package ostrowski.graphics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import ostrowski.graphics.model.ISelectionWatcher;
import ostrowski.graphics.model.Message;
import ostrowski.graphics.model.ObjHex;
import ostrowski.graphics.model.ObjHex.Terrain;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple2;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.Thing;
import ostrowski.graphics.texture.BitmapFont;
import ostrowski.graphics.texture.ButtonImage;
import ostrowski.graphics.texture.Texture;
import ostrowski.graphics.texture.TextureLoader;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;

public class GLView implements Listener
{
   private static float                       DEFAULT_FIELD_OF_VIEW   = 45.0f;
   private static int                         BUTTON_PAN              = 1;      // left button
   //private static int                         BUTTON_SELECT           = 1;    // left button
   private static int                         BUTTON_DRAG             = 3;      // right button
   private static final int                   MIN_ZOOM_POWER          = -5;
   private static final int                   MAX_ZOOM_POWER          = 5;
   private static final int                   MIN_ELEVATION_POWER     = -1;
   private static final int                   MAX_ELEVATION_POWER     = 10;

   public GLCanvas                            _GlCanvas;
   private float                              _yRot                   = 0;
   private float                              _xRot                   = 0;
   private FloatBuffer                        _material;
   private final TextureLoader                _textureLoader          = new TextureLoader();

   private final Semaphore                    _lock_models            = new Semaphore("GLView_models", AnimationControllerSemaphore.CLASS_GLVIEW_MODELS);
   private final Semaphore                    _lock_messages          = new Semaphore("GLView_messages", AnimationControllerSemaphore.CLASS_GLVIEW_MESSAGES);
   private final ArrayList<TexturedObject>    _models                 = new ArrayList<>();
   private final ArrayList<Message>           _messages               = new ArrayList<>();
   private final ArrayList<ISelectionWatcher> _selectionWatchers      = new ArrayList<>();

   private int                                _zoomPower              = 0;
   private int                                _elevationPower         = 4;
   private float                              _fieldOfViewYangle      = DEFAULT_FIELD_OF_VIEW;
   private final float                        _zNear                  = 0.5f;
   private final float                        _zFar                   = 3500.0f;
   private final float                        _heightScale            = 60f;
   public Tuple3                              _cameraPosition         = new Tuple3(0f, getHeightAtCurrentElevationPower(), -120f);
   public Tuple3                              _minOccupiedPosition    = new Tuple3(0f, 0f, 0f);
   public Tuple3                              _maxOccupiedPosition    = new Tuple3(0f, 0f, 0f);

   public BitmapFont                          _font;
   private ButtonImage                        _zoomButtons;
   private ButtonImage                        _elevationButtons;
   private int                                _width;
   private int                                _height;
   private float                              _aspectRatio;
   private int                                _zoomLeft               = 100;
   private int                                _zoomTop                = 100;
   private int                                _elevationLeft;
   private int                                _elevationTop;
   private boolean                            _allowPan               = true;
   private boolean                            _allowDrag              = true;
   private int                                _buttonDown             = 0;
   private Tuple3                             _mouseDownPosWorldCoordinates;
   private Tuple3                             _initialMouseDownCameraPosition;
   private Tuple2                             _mouseDownPosScreenCoordinates;
   private Tuple2                             _initialMouseDownPosScreenCoordinates;
   private TexturedObject                     _texturedObjectClickedUpon;
   private double                             _texturedObjectClickedUponAngleFromCenter;
   private double                             _texturedObjectClickedUponNormalizedDistanceFromCenter;
   public Texture                             _terrainTexture         = null;
   public Texture                             _terrainTextureSelected = null;
   private final ArrayList<TexturedObject>    _selectedObjects        = new ArrayList<>();
   private boolean                            _watchMouseMove;

   public GLView(Composite parent, boolean withControls) {
      _material = BufferUtils.createFloatBuffer(4);

      GLData data = new GLData();
      data.doubleBuffer = true;
      _GlCanvas = new GLCanvas(parent, SWT.NONE, data);
      _GlCanvas.addListener(SWT.Resize, this);
      //_GlCanvas.addListener(SWT.Paint, this);
      _GlCanvas.addListener(SWT.MouseDown, this);
      _GlCanvas.addListener(SWT.MouseUp, this);
      _GlCanvas.addListener(SWT.MouseMove, this);
      _GlCanvas.addListener(SWT.MouseDoubleClick, this);
      _GlCanvas.addListener(SWT.MouseExit, this);
      _GlCanvas.addListener(SWT.MouseVerticalWheel, this);

      if (!useAsCurrentCanvas()) {
         return;
      }
      defineLight(1f);
      defineMaterial();

      // run through some based OpenGL capability settings. Textures
      // enabled, back face culling enabled, depth testing is on,
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDepthFunc(GL11.GL_LEQUAL);
      GL11.glShadeModel(GL11.GL_SMOOTH);

      // define the properties for the perspective of the scene
      resizeCanvas();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);

      GL11.glClearColor(.3f, .5f, .8f, 1.0f); // sets the background clear where nothing is drawn
      GL11.glClearDepth(1.0);
      GL11.glLineWidth(2);

      if (withControls) {
         try {
            _terrainTexture = _textureLoader.getTexture("res/texture_terrain.png");
            _terrainTextureSelected = _textureLoader.getTexture("res/texture_terrain_selected.png");

            Texture fontTexture = _textureLoader.getTexture("res/font.png");
            _font = new BitmapFont(fontTexture, 32 /*characterWidth*/, 32/*characterHeight*/);

            Texture controlsTexture = _textureLoader.getTexture("res/viewControls.png");
            _zoomButtons = new ButtonImage(controlsTexture, new org.lwjgl.util.Rectangle(0, 0, 123, 49));
            _elevationButtons = new ButtonImage(controlsTexture, new org.lwjgl.util.Rectangle(0, 50, 49, 91));
         } catch (IOException e) {
         }
      }
      // This ensures that all normals are normalized before used in computations.
      // It may slow processing down, since it requires extra computations,
      // but since this isn't a very demanding application, the speed reduction shouldn't be noticable.
      GL11.glEnable(GL11.GL_NORMALIZE);

      incrementCameraAngle(0f, -10f);
   }

   public void allowPan(boolean allow) {
      _allowPan = allow;
   }

   public void allowDrag(boolean allow) {
      _allowDrag = allow;
   }

   public void addDefaultMap() {
      for (int x = 0; x < 20; x++) {
         for (int y = 0; y < 20; y++) {
            if ((x % 2) != (y % 2)) {
               Terrain terrain = Terrain.GRASS;
               if (x > 11) {
                  terrain = Terrain.WATER;
               }
               else if (y > 12) {
                  terrain = Terrain.DIRT;
               }
               addHex(x, y, 0, terrain, 1.0f /*opacity*/, (x * 20) + y, null /*label*/);
            }
         }
      }
      setMapExtents(20, 20);
   }

   static float boundsRadius = 24f; // 4-foot diameter hex
   static float edgeRadius = boundsRadius - 0.5f;
   static float rad32 = (float) ((boundsRadius * Math.sqrt(3)) / 2);
   static float rad15 = 1.5f * boundsRadius;

   public Tuple3 getHexLocation(int x, int y, int z) {
      return new Tuple3(rad15 * x , z, rad32 * y);
   }
   public TexturedObject addHex(int x, int y, int z, Terrain terrain, float opacity,
                                long uniqueNumericKey, String label) {
      Tuple3 center = getHexLocation(x , y, z);

      TexturedObject texturedObj = new TexturedObject(_terrainTexture, _terrainTextureSelected, false/*invertNormals*/);
      texturedObj._relatedObject = "x:" + x + ", y:" + y;
      texturedObj._opacity = opacity;
      ObjHex data = new ObjHex(center, edgeRadius, boundsRadius, terrain, uniqueNumericKey);
      if (label != null) {
         Message message = new Message();
         message._text = label;
         message._centerText = true;

         data.setMessage(message);
      }
      texturedObj.addObject(new ObjModel(data, this));
      addModel(texturedObj);
      return texturedObj;
   }

   public void clearModels() {
      synchronized (_models) {
         _lock_models.check();
         _models.clear();
      }
   }

   public boolean removeObject(TexturedObject obj) {
      synchronized (_models) {
         _lock_models.check();
         return _models.remove(obj);
      }
   }

   //   public TexturedObject getObjectByRelatedObject(Object obj) {
   //      for (TexturedObject model : _models) {
   //         if (model._relatedObject == obj)
   //            return model;
   //      }
   //      return null;
   //   }
   public Canvas getCanvas() {
      return _GlCanvas;
   }

   public void setCameraAngle(float x, float y) {
      _xRot = x;
      _yRot = y;
      if (_zoomPower == 0) {
         return;
      }
      zoom(0, Display.getCurrent());
   }

   public void incrementCameraAngle(float x, float y) {
      _xRot += x;
      _yRot += y;
      //      Tuple3 dist3 = _cameraPosition.subtract(_cameraPosition);
      //      double dist = Math.sqrt(dist3.getX() * dist3.getX() + dist3.getY() * dist3.getY() + dist3.getZ() * dist3.getZ());
      //      double flatRadius = Math.sin(Math.toRadians(_yRot)) * dist;
      //      float Z = -(float) (Math.cos(Math.toRadians(_yRot)) * dist);
      //      float X = (float) (Math.sin(Math.toRadians(_xRot)) * flatRadius);
      //      float Y = (float) (Math.cos(Math.toRadians(_xRot)) * flatRadius);
      //      _cameraPosition = new Tuple3(X, Y, Z);
   }

   public boolean useAsCurrentCanvas() {
      if (_GlCanvas == null) {
         return false;
      }
      String threadName = Thread.currentThread().getName();
      if (!threadName.equals("main")) {
         return false;
      }
      if (_GlCanvas.isDisposed()) {
         return false;
      }
      _GlCanvas.setCurrent();
      try {
         GLContext.useContext(_GlCanvas);
         return true;
      } catch (LWJGLException e) {
         e.printStackTrace();
         return false;
      }
   }

   /**
    * Defint the light setup to view the scene
    */
   public void defineLight(float xScale) {

//      GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT,  BufferUtils.createFloatBuffer(4).put(1).put(1).put(1).put(1).flip());
//      GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE,  BufferUtils.createFloatBuffer(4).put(1).put(1).put(1).put(1).flip());
//      GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, BufferUtils.createFloatBuffer(4).put(10f*xScale).put(10f).put(5f).put(0).flip());// set up the position of the light
//      GL11.glDisable(GL11.GL_LIGHT0);

      { // configure light sources:
         GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT,        BufferUtils.createFloatBuffer(4).put(0.0f).put(0.2f).put(0.2f).put(0.2f).flip()); // Setup The Ambient Light
         GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE,        BufferUtils.createFloatBuffer(4).put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip()); // Setup The Diffuse Light
         GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION,       BufferUtils.createFloatBuffer(4).put(-200.0f).put(800.0f).put(1300.0f * xScale).put(0f).flip()); // Position The Light (w=0 => directional source, not a positional source)
         GL11.glEnable(GL11.GL_LIGHT1);
      }
   }
   public void defineMaterial() {
      // setup the ambient light
      GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, BufferUtils.createFloatBuffer(4).put(0.8f).put(0.8f).put(0.8f).put(0.8f).flip());
      GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, GL11.GL_TRUE);

      GL11.glEnable(GL11.GL_LIGHTING);
      // setup the material:
      GL11.glEnable(GL11.GL_COLOR_MATERIAL);
      GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT_AND_DIFFUSE);
      GL11.glPolygonMode(GL11.GL_FRONT_FACE, GL11.GL_AMBIENT_AND_DIFFUSE);

      _material.put(1).put(1).put(1).put(1);
      _material.flip();
      GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, _material);
      GL11.glMaterial(GL11.GL_BACK, GL11.GL_DIFFUSE, _material);
   }

   public void setCameraPosition(float x, float y, float z) {
      _cameraPosition = new Tuple3(x, y, z);
   }

   public void drawScene(Display display) {

      if (!_GlCanvas.isDisposed()) {
         if (!useAsCurrentCanvas()) {
            return;
         }

         ArrayList<Message> messages = new ArrayList<>();

         GL11.glLoadIdentity();

         GL11.glShadeModel(GL11.GL_SMOOTH);


         /******************/
         // Clear the window with current clearing color and clear depthbuffer
         GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

         // Save matrix state
         GL11.glMatrixMode(GL11.GL_MODELVIEW);
         GL11.glLoadIdentity();
         GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
         {
            setupView();
            defineLight(1f);

            // model human(s):
            //humanModel.ModelShape();

            // This actually draws the geometry to the screen
            synchronized (_models) {
               try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_models)) {
                  for (TexturedObject texturedObj : _models) {
                     if (texturedObj instanceof Thing) {
                        if (((Thing)texturedObj)._invertNormals) {
                           GL11.glScalef(-1f, 1f, 1f);
                           defineLight(-1f);
                        }
                     }
                     texturedObj.render(this, messages);
                     if (texturedObj instanceof Thing) {
                        if (((Thing)texturedObj)._invertNormals) {
                           GL11.glScalef(-1f, 1f, 1f);
                           defineLight(1f);
                        }
                     }
                  }
               }
            }
         }
         // Restore transformations from (top).
         GL11.glPopMatrix();

         // Flush drawing commands
         GL11.glFlush();
         /******************/

         enterOrtho();

         if (_zoomButtons != null) {
            _zoomButtons.drawImage(_zoomLeft, _zoomTop);
         }
         if (_elevationButtons != null) {
            _elevationButtons.drawImage(_elevationLeft, _elevationTop);
         }

         if (_font != null) {
            // object-defined messages:
            for (Message message : messages) {
               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               if ((message._text != null) && (message._text.length() > 0) && message._visible) {
                  if (message._zLoc <= 0) {
                     BitmapFont font = _font;
                     font.drawString(message._font, message._colorRGB, message._opacity, message._text,
                                     message._xLoc, _GlCanvas.getBounds().height - message._yLoc, message._centerText/*centerOnCoords*/);
                  }
               }
            }
            // View-defined messages:
            for (Message message : _messages) {
               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               if ((message._text != null) && (message._text.length() > 0) && message._visible) {
                  if (message._zLoc <= 0) {
                     BitmapFont font = _font;
                     font.drawString(message._font, message._colorRGB, message._opacity, message._text,
                                     message._xLoc, _GlCanvas.getBounds().height - message._yLoc, message._centerText/*centerOnCoords*/);
                  }
               }
            }
         }

         // reset the blending
         GL11.glDisable(GL11.GL_BLEND);

         leaveOrtho();

         _GlCanvas.swapBuffers();
      }
   }

   void setupView() {
      GL11.glRotatef(-_yRot, 1.0f, 0.0f, 0.0f); // rotation affects matrix state
      GL11.glRotatef(-_xRot, 0.0f, 1.0f, 0.0f); // do the "camera rotation"

      // position the camera:
      GL11.glTranslatef(_cameraPosition.getX(), _cameraPosition.getY(), _cameraPosition.getZ()); // translate into perspective view
   }

   private void zoom(int zoomInOut, Display display) {
      if (zoomInOut == 0) {
         _zoomPower = 0;
      }
      else {
         _zoomPower += zoomInOut;
         _zoomPower = Math.max(_zoomPower, MIN_ZOOM_POWER);
         _zoomPower = Math.min(_zoomPower, MAX_ZOOM_POWER);
      }
      _fieldOfViewYangle = (float) (DEFAULT_FIELD_OF_VIEW * Math.pow(0.8, _zoomPower));

      useAsCurrentCanvas();
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GLU.gluPerspective(_fieldOfViewYangle, _aspectRatio, _zNear, _zFar);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      drawScene(display);
   }

   @Override
   public void handleEvent(Event event) {
      //long eventHandleStartTime = System.currentTimeMillis();
      try {
         Tuple2 screenLoc = new Tuple2(event.x, event.y);
         if (event.type == SWT.Resize) {
            resizeCanvas();
         }
         else if (event.type == SWT.MouseDown) {
            useAsCurrentCanvas();
            _buttonDown = event.button;
            if (_buttonDown == 1) {
               if ((_zoomButtons != null) && (_zoomButtons.containsPoint(event.x, event.y))) {
                  int buttonRadius = _zoomButtons.getHeight() / 2;
                  int dist2FromZoomOut = getDistSquared(event.x, event.y, (_zoomLeft + buttonRadius), (_zoomTop + buttonRadius));
                  //dist2FromZoomOut *= 1.2;
                  if (dist2FromZoomOut < (buttonRadius * buttonRadius)) {
                     zoom(-1, event.display);
                  }
                  else {
                     int dist2FromZoomIn = getDistSquared(event.x, event.y, ((_zoomLeft + _zoomButtons.getWidth()) - buttonRadius),
                                                          ((_zoomTop + _zoomButtons.getHeight()) - buttonRadius));
                     //dist2FromZoomIn *= 1.2;
                     if (dist2FromZoomIn < (buttonRadius * buttonRadius)) {
                        zoom(1, event.display);
                     }
                     else {
                        int dist2FromZoomReset = getDistSquared(event.x, event.y, (_zoomLeft + (_zoomButtons.getWidth() / 2)),
                                                                (_zoomTop + (_zoomButtons.getHeight() / 2)));
                        if (dist2FromZoomReset < (buttonRadius * buttonRadius)) {
                           zoom(0, event.display);
                        }
                     }
                  }
                  return;
               }
               if ((_elevationButtons != null) && (_elevationButtons.containsPoint(event.x, event.y))) {
                  int buttonRadius = _elevationButtons.getWidth() / 2;
                  int elevationCenterY = _elevationTop + (_elevationButtons.getHeight() / 2);
                  int dist2FromElevationUp = getDistSquared(event.x, event.y, (_elevationLeft + buttonRadius), (_elevationTop + buttonRadius));
                  if ((event.y < elevationCenterY) && (dist2FromElevationUp < (buttonRadius * buttonRadius))) {
                     // raise elevation
                     if (++_elevationPower > MAX_ELEVATION_POWER) {
                        _elevationPower = MAX_ELEVATION_POWER;
                     }
                  }
                  else {
                     int dist2FromElevationDn = getDistSquared(event.x, event.y, ((_elevationLeft + _elevationButtons.getWidth()) - buttonRadius),
                                                               ((_elevationTop + _elevationButtons.getHeight()) - buttonRadius));
                     if ((event.y > elevationCenterY) && (dist2FromElevationDn < (buttonRadius * buttonRadius))) {
                        // lower elevation
                        if (--_elevationPower < MIN_ELEVATION_POWER) {
                           _elevationPower = MIN_ELEVATION_POWER;
                        }
                     }
                  }
                  _cameraPosition = new Tuple3(_cameraPosition.getX(), getHeightAtCurrentElevationPower(), _cameraPosition.getZ());
                  drawScene(event.display);
                  return;
               }

               boolean multiselection = false;
               // TODO: do we handle multiple selection?
               //       if so, watch for the CTRL or SHIFT key, and if down, set multiselection to true
               if (!multiselection) {
                  while (_selectedObjects.size() > 0) {
                     TexturedObject selectedObject = _selectedObjects.remove(0);
                     selectedObject.setSelected(false);
                     // notify any watcher that the selection changed
                     for (ISelectionWatcher watcher : _selectionWatchers) {
                        watcher.ObjectSelected(selectedObject, this, false);
                     }
                  }
               }
            }
            Tuple3 mouseDownWorldCoordinates;
            GL11.glLoadIdentity();
            /******************/
            // Save matrix state
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
            {
               setupView();
               WorldCoordinatesResults results = findWorldCoordinatedForScreenLocation(screenLoc);
               mouseDownWorldCoordinates = results._worldCoordinates;
               _texturedObjectClickedUpon = results._object;
               _texturedObjectClickedUponAngleFromCenter = results._angleFromCenter;
               _texturedObjectClickedUponNormalizedDistanceFromCenter = results._normalizedDistanceFromCenter;

               GL11.glPopMatrix();
            }
            // notify any watcher that the selection changed
            if (_texturedObjectClickedUpon != null) {
               for (ISelectionWatcher watcher : _selectionWatchers) {
                  watcher.onMouseDown(_texturedObjectClickedUpon, event, _texturedObjectClickedUponAngleFromCenter,
                                      _texturedObjectClickedUponNormalizedDistanceFromCenter);
               }
            }

            if (_buttonDown == BUTTON_DRAG) {
               _mouseDownPosWorldCoordinates = mouseDownWorldCoordinates;
               _initialMouseDownCameraPosition = _cameraPosition;
            }
            else if (_buttonDown == BUTTON_PAN) {
               _mouseDownPosScreenCoordinates = screenLoc;
               _initialMouseDownPosScreenCoordinates = screenLoc;
            }
            drawScene(event.display);
         }
         else if (event.type == SWT.MouseUp) {
            if (_initialMouseDownPosScreenCoordinates != null) {
               if (_initialMouseDownPosScreenCoordinates.subtract(screenLoc).magnitude() < 3) {
                  if (_texturedObjectClickedUpon != null) {
                     _selectedObjects.add(_texturedObjectClickedUpon);
                     _texturedObjectClickedUpon.setSelected(true);

                     // notify any watcher that the mouse is up, and of the selected item
                     for (ISelectionWatcher watcher : _selectionWatchers) {
                        watcher.ObjectSelected(_texturedObjectClickedUpon, this, true);
                        watcher.onMouseUp(_texturedObjectClickedUpon, event, _texturedObjectClickedUponAngleFromCenter,
                                          _texturedObjectClickedUponNormalizedDistanceFromCenter);
                     }
                  }
               }
            }
            _buttonDown = 0;
            _mouseDownPosWorldCoordinates = null;
            _mouseDownPosScreenCoordinates = null;
            _initialMouseDownCameraPosition = null;
            _initialMouseDownPosScreenCoordinates = null;
            _texturedObjectClickedUpon = null;
            _texturedObjectClickedUponAngleFromCenter = 0;
            _texturedObjectClickedUponNormalizedDistanceFromCenter = 0;
            drawScene(event.display);

         }
         else if (event.type == SWT.MouseMove) {
            if ((_buttonDown == BUTTON_DRAG) && (_mouseDownPosWorldCoordinates != null) && (_initialMouseDownCameraPosition != null)) {
               if (_allowDrag) {
                  GL11.glLoadIdentity();
                  /******************/
                  // Save matrix state
                  GL11.glMatrixMode(GL11.GL_MODELVIEW);
                  GL11.glLoadIdentity();
                  GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
                  {
                     useAsCurrentCanvas();
                     _cameraPosition = _initialMouseDownCameraPosition;
                     setupView();
                     FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
                     GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);

                     FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
                     GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrix);

                     IntBuffer viewport = BufferUtils.createIntBuffer(16);
                     GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

                     Tuple3 newMouseWorldPos = GetWorldPositionAtScreenLocAndWorldYPos(screenLoc, _mouseDownPosWorldCoordinates.getY(), modelMatrix,
                                                                                       projMatrix, viewport);
                     Tuple3 movementSinceMouseDown = newMouseWorldPos.subtract(_mouseDownPosWorldCoordinates);
                     _cameraPosition = _initialMouseDownCameraPosition.add(movementSinceMouseDown);

                     _cameraPosition = new Tuple3(Math.min(-_minOccupiedPosition.getX(), Math.max(_cameraPosition.getX(), -_maxOccupiedPosition.getZ())),
                                                  _cameraPosition.getY(),
                                                  Math.min(-_minOccupiedPosition.getX(), Math.max(_cameraPosition.getZ(), -_maxOccupiedPosition.getZ()))
                                                  );
                     GL11.glPopMatrix();
                     //System.out.println("camera now at " + _cameraPosition.toString());
                  }
                  // drag complete
                  drawScene(event.display);
                  return;
               }
            }
            else if ((_buttonDown == BUTTON_PAN) && (_mouseDownPosScreenCoordinates != null)) {
               if (_allowPan) {
                  float dx = _mouseDownPosScreenCoordinates.getX() - screenLoc.getX();
                  float dy = _mouseDownPosScreenCoordinates.getY() - screenLoc.getY();
                  _mouseDownPosScreenCoordinates = screenLoc;
                  _yRot -= (_fieldOfViewYangle * dy) / _height;
                  _xRot -= (_fieldOfViewYangle * _aspectRatio * dx) / _width;

                  for (IGLViewListener listener : _listeners) {
                     listener.viewAngleChanged((float)((_xRot  * Math.PI) / 180), (float)((_yRot  * Math.PI) / 180));
                  }
                  GLU.gluPerspective(_fieldOfViewYangle, _aspectRatio, _zNear, _zFar);
                  // pan complete
                  drawScene(event.display);
                  return;
               }
            }
            if ((_buttonDown != 0) || _watchMouseMove) {
               GL11.glLoadIdentity();
               /******************/
               // Save matrix state
               GL11.glMatrixMode(GL11.GL_MODELVIEW);
               GL11.glLoadIdentity();
               GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
               {
                  setupView();
                  WorldCoordinatesResults results = findWorldCoordinatedForScreenLocation(screenLoc);
                  _texturedObjectClickedUpon = results._object;
                  _texturedObjectClickedUponAngleFromCenter = results._angleFromCenter;
                  _texturedObjectClickedUponNormalizedDistanceFromCenter = results._normalizedDistanceFromCenter;

                  GL11.glPopMatrix();
               }
               // notify any watcher that the mouse is up, and of the selected item
               if (_texturedObjectClickedUpon != null) {
                  for (ISelectionWatcher watcher : _selectionWatchers) {
                     if (_buttonDown != 0) {
                        watcher.onMouseDrag(_texturedObjectClickedUpon, event, _texturedObjectClickedUponAngleFromCenter,
                                            _texturedObjectClickedUponNormalizedDistanceFromCenter);
                     }
                     else if (_watchMouseMove) {
                        watcher.onMouseMove(_texturedObjectClickedUpon, event, _texturedObjectClickedUponAngleFromCenter,
                                            _texturedObjectClickedUponNormalizedDistanceFromCenter);
                     }
                  }
               }
            }
            drawScene(event.display);
         }
         else if (event.type == SWT.MouseDoubleClick) {
            return;
         }
         else if (event.type == SWT.MouseExit) {
            return;
         }
         else if (event.type == SWT.MouseVerticalWheel) {
            // record the old zoom level, then do the zoom
            float fieldOfViewYangleBefore = _fieldOfViewYangle;
            zoom((event.count / 3), event.display);
            // Now pan the view so the position under the mouse point wont seem to have moved at all
            if (fieldOfViewYangleBefore != _fieldOfViewYangle) {
               _yRot += ((_fieldOfViewYangle - fieldOfViewYangleBefore) * (event.y - (_height / 2.0))) / _height;
               _xRot += ((_fieldOfViewYangle - fieldOfViewYangleBefore) * (event.x - (_width / 2.0))) / _width;
               // pan complete, redraw the view
               drawScene(event.display);
            }
            return;
         }
         //else if (event.type == SWT.Paint) {
         //   return;
         //}
      } finally {
         //long timeElapsed = System.currentTimeMillis() - eventHandleStartTime;
         //System.out.println("handling of event " + event.toString() + " took " + timeElapsed + "ms.");
      }
   }

   public void setElevationPower(int elevationPower) {
      _elevationPower = Math.min(Math.max(MIN_ELEVATION_POWER, elevationPower), MAX_ELEVATION_POWER);
      _cameraPosition = new Tuple3(_cameraPosition.getX(), getHeightAtCurrentElevationPower(), _cameraPosition.getZ());
   }

   public void setHeightScaleByApproximateHeightInInches(float desiredHeightInInchesIn) {
      float desiredHeightInInches = Math.min(desiredHeightInInchesIn, _zFar);

      for (int scale = 0; scale < MAX_ELEVATION_POWER; scale++) {
         if (-getHeightInInchesForElevationPower(scale) > desiredHeightInInches) {
            setElevationPower(scale);
            return;
         }
      }
      setElevationPower(MAX_ELEVATION_POWER);
   }

   private float getHeightInInchesForElevationPower(int elevationPower) {
      return (float) (-_heightScale * Math.pow(1.5, elevationPower));
   }
   private float getHeightAtCurrentElevationPower() {
      float val = getHeightInInchesForElevationPower(_elevationPower);
      if (val > _zFar) {
         _elevationPower--;
         return getHeightAtCurrentElevationPower();
      }
      return val;
   }

   public void addSelectionWatcher(ISelectionWatcher watcher) {
      _selectionWatchers.add(watcher);
   }

   public boolean removeSelectionWatcher(ISelectionWatcher watcher) {
      return _selectionWatchers.remove(watcher);
   }

   //   private String convertMatrixToString(IntBuffer matrixBuffer) {
   //      int size = matrixBuffer.capacity();
   //      ArrayList<String> list = new ArrayList<>();
   //      for (int i=0 ; i<size ; i++) {
   //         list.add(String.valueOf(matrixBuffer.get(i)));
   //      }
   //      return formatStringMatrix(list);
   //   }
   //   private String convertMatrixToString(FloatBuffer matrixBuffer) {
   //      int size = matrixBuffer.capacity();
   //      ArrayList<String> list = new ArrayList<>();
   //      for (int i=0 ; i<size ; i++) {
   //         list.add(String.valueOf(matrixBuffer.get(i)));
   //      }
   //      return formatStringMatrix(list);
   //   }
   //
   //   private String formatStringMatrix(ArrayList<String> list) {
   //      int maxWidth = 0;
   //      for (String str : list)
   //         if (str.length() > maxWidth)
   //            maxWidth = str.length();
   //
   //      StringBuilder sb = new StringBuilder();
   //      int cols = (int) Math.sqrt(list.size());
   //      int rows = cols;
   //      for (int row=0 ; row < rows ; row++) {
   //         sb.append("\n| ");
   //         for (int col=0 ; col < cols ; col++) {
   //            String str = list.get(row * cols + col);
   //            int padding = maxWidth - str.length();
   //            int prePadding = padding / 2;
   //            int postPadding = padding - prePadding;
   //            while (prePadding-- > 0)
   //               sb.append(' ');
   //            sb.append(str);
   //            while (postPadding-- > 0)
   //               sb.append(' ');
   //            sb.append(' ');
   //         }
   //         sb.append("|");
   //      }
   //      return sb.toString();
   //   }

   private class WorldCoordinatesResults
   {
      public WorldCoordinatesResults() {
      }

      public TexturedObject _object                       = null; // _texturedObjectClickedUpon
      public double         _angleFromCenter              = 0;   // _texturedObjectClickedUponAngleFromCenter
      public double         _normalizedDistanceFromCenter = 0;   // _texturedObjectClickedUponNormalizedDistanceFromCenter
      public Tuple3         _worldCoordinates             = null;
   }

   private WorldCoordinatesResults findWorldCoordinatedForScreenLocation(Tuple2 screenLoc) {
      WorldCoordinatesResults results = new WorldCoordinatesResults();

      FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
      GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);

      FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
      GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrix);

      IntBuffer viewport = BufferUtils.createIntBuffer(16);
      GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

      Tuple2 invertedScreenLoc = new Tuple2(screenLoc.getX(), viewport.get(3) - screenLoc.getY());
      Tuple3 lowestZObjectLocationInScreenCoordinated = null;
      //long startTime = System.currentTimeMillis();

      synchronized (_models) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_models)) {
            for (TexturedObject model : _models) {
               Tuple3 objectLocationInWindowCoords = model.getScreenPosition3dContainingScreenPoint(invertedScreenLoc, modelMatrix, projMatrix, viewport);
               if (objectLocationInWindowCoords != null) {
                  if (objectLocationInWindowCoords.getZ() > 0) {
                     if ((lowestZObjectLocationInScreenCoordinated == null)
                              || (objectLocationInWindowCoords.getZ() < lowestZObjectLocationInScreenCoordinated.getZ())) {
                        lowestZObjectLocationInScreenCoordinated = objectLocationInWindowCoords;
                        results._object = model;
                     }
                  }
               }
            }
         }
      }
      //long duration = System.currentTimeMillis() - startTime;
      //startTime = System.currentTimeMillis();
      //System.out.println("findWorldCoordinatedForScreenLocation point #1 took " + duration + "ms. (" + _models.size() + " models)");

      if (results._object != null) {
         FloatBuffer clickPosInWorldCoordsBuffer = BufferUtils.createFloatBuffer(3);
         GLU.gluUnProject(lowestZObjectLocationInScreenCoordinated.getX(), lowestZObjectLocationInScreenCoordinated.getY(),
                          lowestZObjectLocationInScreenCoordinated.getZ(), modelMatrix, projMatrix, viewport, clickPosInWorldCoordsBuffer);
         Tuple3 clickPosInWorldCoords = new Tuple3(clickPosInWorldCoordsBuffer);
         Tuple3 objCenter = results._object.getObjectCenter();
         Tuple3 delta = objCenter.subtract(clickPosInWorldCoords);
         results._angleFromCenter = Math.atan2(delta.getZ(), delta.getX());
         double distanceFromCenter = Math.sqrt((delta.getX() * delta.getX()) + (delta.getZ() * delta.getZ()));
         Tuple3 objectBoundingDimensions = results._object.getObjectBoundingCubeDimensions();
         double flatObjectRadius = Math.sqrt((objectBoundingDimensions.getX() * objectBoundingDimensions.getX()) + (objectBoundingDimensions.getZ()
                                             * objectBoundingDimensions.getZ()));
         results._normalizedDistanceFromCenter = distanceFromCenter / (flatObjectRadius / 2);
         //duration = System.currentTimeMillis() - startTime;
         //startTime = System.currentTimeMillis();
         //System.out.println("findWorldCoordinatedForScreenLocation point #2 took " + duration + "ms.");
      }
      if (lowestZObjectLocationInScreenCoordinated != null) {
         FloatBuffer objectPosition3d = BufferUtils.createFloatBuffer(3);
         GLU.gluUnProject(lowestZObjectLocationInScreenCoordinated.getX(), lowestZObjectLocationInScreenCoordinated.getY(),
                          lowestZObjectLocationInScreenCoordinated.getZ(), modelMatrix, projMatrix, viewport, objectPosition3d);
         results._worldCoordinates = new Tuple3(objectPosition3d.get(0), objectPosition3d.get(1), objectPosition3d.get(2));
         //duration = System.currentTimeMillis() - startTime;
         //startTime = System.currentTimeMillis();
         //System.out.println("findWorldCoordinatedForScreenLocation point #3 took " + duration + "ms.");
      }
      return results;
   }

   public FloatBuffer GetOGLPos(int x, int y) {
      FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
      GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);

      FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
      GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrix);

      IntBuffer viewport = BufferUtils.createIntBuffer(16);
      GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

      float winX = x;
      float winY = (float) viewport.get(3) - (float) y;
      ByteBuffer winZ = BufferUtils.createByteBuffer(1);
      GL11.glReadPixels(x, (int) winY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, winZ);

      FloatBuffer win_pos = BufferUtils.createFloatBuffer(3);
      GLU.gluUnProject(winX, winY, winZ.get(0), modelMatrix, projMatrix, viewport, win_pos);
      return win_pos;
   }

   public Tuple3 GetWorldPositionAtScreenLocAndWorldYPos(Tuple2 screenPos, float worldPosY, FloatBuffer modelMatrix, FloatBuffer projMatrix, IntBuffer viewport) {
      float winX = screenPos.getX();
      float winY = viewport.get(3) - screenPos.getY();
      FloatBuffer win_pos1 = BufferUtils.createFloatBuffer(3);
      FloatBuffer win_pos2 = BufferUtils.createFloatBuffer(3);
      GLU.gluUnProject(winX, winY, -0.8f, modelMatrix, projMatrix, viewport, win_pos1);
      GLU.gluUnProject(winX, winY, -0.4f, modelMatrix, projMatrix, viewport, win_pos2);

      Tuple3 worldLineEndpoint1 = new Tuple3(win_pos1.get(0), win_pos1.get(1), win_pos1.get(2));
      Tuple3 worldLineEndpoint2 = new Tuple3(win_pos2.get(0), win_pos2.get(1), win_pos2.get(2));
      // the parametric form of the line starts with A=0 at point1, A=1 at point2
      // x = x1 + A(x2-x1)
      // y = y1 + A(y2-y1)
      // z = z1 + A(z2-z1)
      // first, find the A where y = worldPosY.
      // worldPozY = y1 + A(y2-y1)
      // worldPozY - y1 = A(y2-y1)
      // A(y2-y1) = worldPosY - y1
      // A = (worldPosY - y1) / (y2-y1)
      float A = (worldPosY - worldLineEndpoint1.getY()) / (worldLineEndpoint2.getY() - worldLineEndpoint1.getY());
      float worldPosX = worldLineEndpoint1.getX() + (A * (worldLineEndpoint2.getX() - worldLineEndpoint1.getX()));
      float worldPosZ = worldLineEndpoint1.getZ() + (A * (worldLineEndpoint2.getZ() - worldLineEndpoint1.getZ()));
      return new Tuple3(worldPosX, worldPosY, worldPosZ);
   }

   public Tuple3 GetWorldPositionAtScreenLocAndWorldDistance(Tuple2 screenPos, float distanceFromCamera, FloatBuffer modelMatrix, FloatBuffer projMatrix,
                                                             IntBuffer viewport) {
      float winX = screenPos.getX();
      float winY = viewport.get(3) - screenPos.getY();
      FloatBuffer win_pos1 = BufferUtils.createFloatBuffer(3);
      FloatBuffer win_pos2 = BufferUtils.createFloatBuffer(3);
      GLU.gluUnProject(winX, winY, -0.9f, modelMatrix, projMatrix, viewport, win_pos1);
      GLU.gluUnProject(winX, winY, -0.8f, modelMatrix, projMatrix, viewport, win_pos2);

      Tuple3 worldLineEndpoint1 = new Tuple3(win_pos1.get(0), win_pos1.get(1), win_pos1.get(2));
      Tuple3 worldLineEndpoint2 = new Tuple3(win_pos2.get(0), win_pos2.get(1), win_pos2.get(2));
      // the parametric form of the line starts with A=0 at point1, A=1 at point2
      // x = x1 + A(x2-x1)
      // y = y1 + A(y2-y1)
      // z = z1 + A(z2-z1)
      // dist = x*x + y*y + z*z
      // dist = (x1 + Adx) * (x1 + Adx) + (y1 + Ady) * (y1 + Ady) + (z1 + Adz) * (z1 + Adz)
      float dx = (worldLineEndpoint2.getX() - worldLineEndpoint1.getX());
      float dy = (worldLineEndpoint2.getY() - worldLineEndpoint1.getY());
      float dz = (worldLineEndpoint2.getZ() - worldLineEndpoint1.getZ());
      float x1 = worldLineEndpoint1.getX();
      float y1 = worldLineEndpoint1.getY();
      float z1 = worldLineEndpoint1.getZ();
      // solve for A:
      // dist = (x1*x1 + 2*Adx*x1 + Adx*Adx) + (y1*y1 + 2*Ady*y1 + Ady*Ady) + (z1*z1 + 2*Adz*z1 + Adz*Adz)
      // dist = x1*x1 + y1*y1 + z1*z1 + 2*Adx*x1 + 2*Ady*y1 + 2*Adz*z1 + Adx*Adx + Ady*Ady + Adz*Adz
      // 0 = [-dist + x1*x1 + y1*y1 + z1*z1] + A[2*(dx*x1 + dy*y1 + dz*z1)] + A*A*[dx*dx + dy*dy + dz*dz]
      // solve using the quadratic equation:
      // ax*x + bx + c = 0
      // where A = x,  c=[-dist + x1*x1 + y1*y1 + z1*z1],  b = [2*(dx*x1 + dy*y1 + dz*z1)], a = [dx*dx + dy*dy + dz*dz]
      float c = -distanceFromCamera + (x1 * x1) + (y1 * y1) + (z1 * z1);
      float b = 2 * ((dx * x1) + (dy * y1) + (dz * z1));
      float a = (dx * dx) + (dy * dy) + (dz * dz);
      // quadratic equation solution states:
      //
      float A1 = (float) ((-b - Math.sqrt((b * b) - (4 * a * c))) / (2 * a));
      //float A2 = (float) (0 - (-b - Math.sqrt(b * b - 4 * a * c)) / (2 * a));
      float A = A1; // TODO: how do we know which one to use????
      float worldPosX = worldLineEndpoint1.getX() + (A * (worldLineEndpoint2.getX() - worldLineEndpoint1.getX()));
      float worldPosY = worldLineEndpoint1.getY() + (A * (worldLineEndpoint2.getY() - worldLineEndpoint1.getY()));
      float worldPosZ = worldLineEndpoint1.getZ() + (A * (worldLineEndpoint2.getZ() - worldLineEndpoint1.getZ()));
      return new Tuple3(worldPosX, worldPosY, worldPosZ);
   }

   private static int getDistSquared(int x1, int y1, int x2, int y2) {
      return ((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1));
   }

   public void resizeCanvas() {
      Rectangle bounds = _GlCanvas.getBounds();
      _width = bounds.width;
      _height = bounds.height;
      if (_zoomButtons != null) {
         _zoomLeft = _width - _zoomButtons.getWidth() - 10;
         _zoomTop = _height - _zoomButtons.getHeight() - 10;
      }
      if (_elevationButtons != null) {
         _elevationLeft = _width - _elevationButtons.getWidth() - 10;
         _elevationTop = _zoomTop - _elevationButtons.getHeight();
      }
      if (_width == 0) {
         _width = 800;
      }
      if (_height == 0) {
         _height = 600;
      }
      _aspectRatio = ((float) _width) / _height;

      if (!useAsCurrentCanvas()) {
         return;
      }
      GL11.glViewport(0, 0, _width, _height);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GLU.gluPerspective(_fieldOfViewYangle, _aspectRatio, _zNear, _zFar);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
   }

   /**
    * Enter the orthographic mode by first recording the current state,
    * next changing us into orthographic projection.
    */
   public void enterOrtho() {
      // store the current state of the renderer
      GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_ENABLE_BIT);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPushMatrix();

      // now enter orthographic projection
      GL11.glLoadIdentity();
      GL11.glOrtho(0, _width, _height, 0, -1, 1);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
   }

   /**
    * Leave the orthographic mode by restoring the state we store
    * in enterOrtho()
    *
    */
   public void leaveOrtho() {
      // restore the state of the renderer
      GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPopMatrix();
      GL11.glPopAttrib();
   }

   public TextureLoader getTextureLoader() {
      return _textureLoader;
   }

   public Message addString(int fontIndex, String text, int x, int y, int z) {
      Message msg = new Message();
      msg._font = fontIndex;
      msg._text = text;
      msg._xLoc = x;
      msg._yLoc = y;
      msg._zLoc = z;
      _messages.add(msg);
      return msg;
   }

   public void addModel(TexturedObject model) {
      synchronized (_models) {
         _lock_models.check();
         _models.add(model);
      }
   }
   public void setMapExtents(int x, int y) {
      int buffer = 300;
       _maxOccupiedPosition = new Tuple3((rad15 * x) + buffer, 0, (rad32 * y) + buffer);
       _minOccupiedPosition = new Tuple3(-buffer, 0f, -buffer);
   }

   public void addMessage(Message message) {
      synchronized (_messages) {
         _lock_messages.check();
         _messages.add(message);
      }
   }

   public void setWatchMouseMove(boolean watchMouseMove) {
      _watchMouseMove = watchMouseMove;
   }

   ArrayList<IGLViewListener> _listeners = new ArrayList<>();
   public void addViewListener(IGLViewListener listener) {
      _listeners.add(listener);
   }
}

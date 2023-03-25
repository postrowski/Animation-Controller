package ostrowski.graphics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

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
   private static       float DEFAULT_FIELD_OF_VIEW = 45.0f;
   private static       int   BUTTON_PAN            = 1;      // left button
   //private static       int   BUTTON_SELECT         = 1;    // left button
   private static       int   BUTTON_DRAG           = 3;      // right button
   private static final int   MIN_ZOOM_POWER        = -5;
   private static final int   MAX_ZOOM_POWER        = 5;
   private static final int   MIN_ELEVATION_POWER   = -1;
   private static final int   MAX_ELEVATION_POWER   = 10;

   public        GLCanvas      glCanvas;
   private       float         yRot          = 0;
   private       float         xRot          = 0;
   private       FloatBuffer   material;
   private final TextureLoader textureLoader = new TextureLoader();

   private final Semaphore               lock_models       = new Semaphore("GLView_models", AnimationControllerSemaphore.CLASS_GLVIEW_MODELS);
   private final Semaphore               lock_messages     = new Semaphore("GLView_messages", AnimationControllerSemaphore.CLASS_GLVIEW_MESSAGES);
   private final List<TexturedObject>    models            = new ArrayList<>();
   private final List<Message>           messages          = new ArrayList<>();
   private final List<ISelectionWatcher> selectionWatchers = new ArrayList<>();

   private       int    zoomPower           = 0;
   private       int    elevationPower      = 4;
   private       float  fieldOfViewYangle   = DEFAULT_FIELD_OF_VIEW;
   private final float  zNear               = 0.5f;
   private final float  zFar                = 3500.0f;
   private final float  heightScale         = 60f;
   public        Tuple3 cameraPosition      = new Tuple3(0f, getHeightAtCurrentElevationPower(), -120f);
   public        Tuple3 minOccupiedPosition = new Tuple3(0f, 0f, 0f);
   public        Tuple3 maxOccupiedPosition = new Tuple3(0f, 0f, 0f);

   public        BitmapFont           font;
   private       ButtonImage          zoomButtons;
   private       ButtonImage          elevationButtons;
   private       int                  width;
   private       int                  height;
   private       float                aspectRatio;
   private       int                  zoomLeft               = 100;
   private       int                  zoomTop                = 100;
   private       int                  elevationLeft;
   private       int                  elevationTop;
   private       boolean              allowPan               = true;
   private       boolean              allowDrag              = true;
   private       int                  buttonDown             = 0;
   private       Tuple3               mouseDownPosWorldCoordinates;
   private       Tuple3               initialMouseDownCameraPosition;
   private       Tuple2               mouseDownPosScreenCoordinates;
   private       Tuple2               initialMouseDownPosScreenCoordinates;
   private       TexturedObject       texturedObjectClickedUpon;
   private       double               texturedObjectClickedUponAngleFromCenter;
   private       double               texturedObjectClickedUponNormalizedDistanceFromCenter;
   public        Texture              terrainTexture         = null;
   public        Texture              terrainTextureSelected = null;
   private final List<TexturedObject> selectedObjects        = new ArrayList<>();
   private       boolean              watchMouseMove;
   List<IGLViewListener> listeners = new ArrayList<>();

   public GLView(Composite parent, boolean withControls) {
      material = BufferUtils.createFloatBuffer(4);

      GLData data = new GLData();
      data.doubleBuffer = true;
      glCanvas = new GLCanvas(parent, SWT.NONE, data);
      glCanvas.addListener(SWT.Resize, this);
      //_GlCanvas.addListener(SWT.Paint, this);
      glCanvas.addListener(SWT.MouseDown, this);
      glCanvas.addListener(SWT.MouseUp, this);
      glCanvas.addListener(SWT.MouseMove, this);
      glCanvas.addListener(SWT.MouseDoubleClick, this);
      glCanvas.addListener(SWT.MouseExit, this);
      glCanvas.addListener(SWT.MouseVerticalWheel, this);

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
            terrainTexture = textureLoader.getTexture("res/texture_terrain.png");
            terrainTextureSelected = textureLoader.getTexture("res/texture_terrain_selected.png");

            Texture fontTexture = textureLoader.getTexture("res/font.png");
            font = new BitmapFont(fontTexture, 32 /*characterWidth*/, 32/*characterHeight*/);

            Texture controlsTexture = textureLoader.getTexture("res/viewControls.png");
            zoomButtons = new ButtonImage(controlsTexture, new org.lwjgl.util.Rectangle(0, 0, 123, 49));
            elevationButtons = new ButtonImage(controlsTexture, new org.lwjgl.util.Rectangle(0, 50, 49, 91));
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
      allowPan = allow;
   }

   public void allowDrag(boolean allow) {
      allowDrag = allow;
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

      TexturedObject texturedObj = new TexturedObject(terrainTexture, terrainTextureSelected, false/*invertNormals*/);
      texturedObj.relatedObject = "x:" + x + ", y:" + y;
      texturedObj.opacity = opacity;
      ObjHex data = new ObjHex(center, edgeRadius, boundsRadius, terrain, uniqueNumericKey);
      if (label != null) {
         Message message = new Message();
         message.text = label;
         message.centerText = true;

         data.setMessage(message);
      }
      texturedObj.addObject(new ObjModel(data, this));
      addModel(texturedObj);
      return texturedObj;
   }

   public void clearModels() {
      synchronized (models) {
         lock_models.check();
         models.clear();
      }
   }

   public boolean removeObject(TexturedObject obj) {
      synchronized (models) {
         lock_models.check();
         return models.remove(obj);
      }
   }

   //   public TexturedObject getObjectByRelatedObject(Object obj) {
   //      for (TexturedObject model : models) {
   //         if (model.relatedObject == obj)
   //            return model;
   //      }
   //      return null;
   //   }
   public Canvas getCanvas() {
      return glCanvas;
   }

   public void setCameraAngle(float x, float y) {
      xRot = x;
      yRot = y;
      if (zoomPower == 0) {
         return;
      }
      zoom(0, Display.getCurrent());
   }

   public void incrementCameraAngle(float x, float y) {
      xRot += x;
      yRot += y;
      //      Tuple3 dist3 = cameraPosition.subtract.cameraPosition);
      //      double dist = Math.sqrt(dist3.getX() * dist3.getX() + dist3.getY() * dist3.getY() + dist3.getZ() * dist3.getZ());
      //      double flatRadius = Math.sin(Math.toRadians(_yRot)) * dist;
      //      float Z = -(float) (Math.cos(Math.toRadians(_yRot)) * dist);
      //      float X = (float) (Math.sin(Math.toRadians(_xRot)) * flatRadius);
      //      float Y = (float) (Math.cos(Math.toRadians(_xRot)) * flatRadius);
      //      cameraPosition = new Tuple3(X, Y, Z);
   }

   public boolean useAsCurrentCanvas() {
      if (glCanvas == null) {
         return false;
      }
      String threadName = Thread.currentThread().getName();
      if (!threadName.equals("main")) {
         return false;
      }
      if (glCanvas.isDisposed()) {
         return false;
      }
      glCanvas.setCurrent();
      try {
         GLContext.useContext(glCanvas);
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

      material.put(1).put(1).put(1).put(1);
      material.flip();
      GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, material);
      GL11.glMaterial(GL11.GL_BACK, GL11.GL_DIFFUSE, material);
   }

   public void setCameraPosition(float x, float y, float z) {
      cameraPosition = new Tuple3(x, y, z);
   }

   public void drawScene(Display display) {

      if (!glCanvas.isDisposed()) {
         if (!useAsCurrentCanvas()) {
            return;
         }

         List<Message> messages = new ArrayList<>();

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
            synchronized (models) {
               try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_models)) {
                  for (TexturedObject texturedObj : models) {
                     if (texturedObj instanceof Thing) {
                        if (((Thing)texturedObj).invertNormals) {
                           GL11.glScalef(-1f, 1f, 1f);
                           defineLight(-1f);
                        }
                     }
                     texturedObj.render(this, messages);
                     if (texturedObj instanceof Thing) {
                        if (((Thing)texturedObj).invertNormals) {
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

         if (zoomButtons != null) {
            zoomButtons.drawImage(zoomLeft, zoomTop);
         }
         if (elevationButtons != null) {
            elevationButtons.drawImage(elevationLeft, elevationTop);
         }

         if (font != null) {
            // object-defined messages:
            for (Message message : messages) {
               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               if ((message.text != null) && (message.text.length() > 0) && message.visible) {
                  if (message.zLoc <= 0) {
                     BitmapFont font = this.font;
                     font.drawString(message.font, message.colorRGB, message.opacity, message.text,
                                     message.xLoc, glCanvas.getBounds().height - message.yLoc, message.centerText/*centerOnCoords*/);
                  }
               }
            }
            // View-defined messages:
            for (Message message : this.messages) {
               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               if ((message.text != null) && (message.text.length() > 0) && message.visible) {
                  if (message.zLoc <= 0) {
                     BitmapFont font = this.font;
                     font.drawString(message.font, message.colorRGB, message.opacity, message.text,
                                     message.xLoc, glCanvas.getBounds().height - message.yLoc, message.centerText/*centerOnCoords*/);
                  }
               }
            }
         }

         // reset the blending
         GL11.glDisable(GL11.GL_BLEND);

         leaveOrtho();

         glCanvas.swapBuffers();
      }
   }

   void setupView() {
      GL11.glRotatef(-yRot, 1.0f, 0.0f, 0.0f); // rotation affects matrix state
      GL11.glRotatef(-xRot, 0.0f, 1.0f, 0.0f); // do the "camera rotation"

      // position the camera:
      GL11.glTranslatef(cameraPosition.getX(), cameraPosition.getY(), cameraPosition.getZ()); // translate into perspective view
   }

   private void zoom(int zoomInOut, Display display) {
      if (zoomInOut == 0) {
         zoomPower = 0;
      }
      else {
         zoomPower += zoomInOut;
         zoomPower = Math.max(zoomPower, MIN_ZOOM_POWER);
         zoomPower = Math.min(zoomPower, MAX_ZOOM_POWER);
      }
      fieldOfViewYangle = (float) (DEFAULT_FIELD_OF_VIEW * Math.pow(0.8, zoomPower));

      useAsCurrentCanvas();
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GLU.gluPerspective(fieldOfViewYangle, aspectRatio, zNear, zFar);
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
            buttonDown = event.button;
            if (buttonDown == 1) {
               if ((zoomButtons != null) && (zoomButtons.containsPoint(event.x, event.y))) {
                  int buttonRadius = zoomButtons.getHeight() / 2;
                  int dist2FromZoomOut = getDistSquared(event.x, event.y, (zoomLeft + buttonRadius), (zoomTop + buttonRadius));
                  //dist2FromZoomOut *= 1.2;
                  if (dist2FromZoomOut < (buttonRadius * buttonRadius)) {
                     zoom(-1, event.display);
                  }
                  else {
                     int dist2FromZoomIn = getDistSquared(event.x, event.y, ((zoomLeft + zoomButtons.getWidth()) - buttonRadius),
                                                          ((zoomTop + zoomButtons.getHeight()) - buttonRadius));
                     //dist2FromZoomIn *= 1.2;
                     if (dist2FromZoomIn < (buttonRadius * buttonRadius)) {
                        zoom(1, event.display);
                     }
                     else {
                        int dist2FromZoomReset = getDistSquared(event.x, event.y, (zoomLeft + (zoomButtons.getWidth() / 2)),
                                                                (zoomTop + (zoomButtons.getHeight() / 2)));
                        if (dist2FromZoomReset < (buttonRadius * buttonRadius)) {
                           zoom(0, event.display);
                        }
                     }
                  }
                  return;
               }
               if ((elevationButtons != null) && (elevationButtons.containsPoint(event.x, event.y))) {
                  int buttonRadius = elevationButtons.getWidth() / 2;
                  int elevationCenterY = elevationTop + (elevationButtons.getHeight() / 2);
                  int dist2FromElevationUp = getDistSquared(event.x, event.y, (elevationLeft + buttonRadius), (elevationTop + buttonRadius));
                  if ((event.y < elevationCenterY) && (dist2FromElevationUp < (buttonRadius * buttonRadius))) {
                     // raise elevation
                     if (++elevationPower > MAX_ELEVATION_POWER) {
                        elevationPower = MAX_ELEVATION_POWER;
                     }
                  }
                  else {
                     int dist2FromElevationDn = getDistSquared(event.x, event.y, ((elevationLeft + elevationButtons.getWidth()) - buttonRadius),
                                                               ((elevationTop + elevationButtons.getHeight()) - buttonRadius));
                     if ((event.y > elevationCenterY) && (dist2FromElevationDn < (buttonRadius * buttonRadius))) {
                        // lower elevation
                        if (--elevationPower < MIN_ELEVATION_POWER) {
                           elevationPower = MIN_ELEVATION_POWER;
                        }
                     }
                  }
                  cameraPosition = new Tuple3(cameraPosition.getX(), getHeightAtCurrentElevationPower(), cameraPosition.getZ());
                  drawScene(event.display);
                  return;
               }

               boolean multiselection = false;
               // TODO: do we handle multiple selection?
               //       if so, watch for the CTRL or SHIFT key, and if down, set multiselection to true
               if (!multiselection) {
                  while (selectedObjects.size() > 0) {
                     TexturedObject selectedObject = selectedObjects.remove(0);
                     selectedObject.setSelected(false);
                     // notify any watcher that the selection changed
                     for (ISelectionWatcher watcher : selectionWatchers) {
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
               mouseDownWorldCoordinates = results.worldCoordinates;
               texturedObjectClickedUpon = results.object;
               texturedObjectClickedUponAngleFromCenter = results.angleFromCenter;
               texturedObjectClickedUponNormalizedDistanceFromCenter = results.normalizedDistanceFromCenter;

               GL11.glPopMatrix();
            }
            // notify any watcher that the selection changed
            if (texturedObjectClickedUpon != null) {
               for (ISelectionWatcher watcher : selectionWatchers) {
                  watcher.onMouseDown(texturedObjectClickedUpon, event, texturedObjectClickedUponAngleFromCenter,
                                      texturedObjectClickedUponNormalizedDistanceFromCenter);
               }
            }

            if (buttonDown == BUTTON_DRAG) {
               mouseDownPosWorldCoordinates = mouseDownWorldCoordinates;
               initialMouseDownCameraPosition = cameraPosition;
            }
            else if (buttonDown == BUTTON_PAN) {
               mouseDownPosScreenCoordinates = screenLoc;
               initialMouseDownPosScreenCoordinates = screenLoc;
            }
            drawScene(event.display);
         }
         else if (event.type == SWT.MouseUp) {
            if (initialMouseDownPosScreenCoordinates != null) {
               if (initialMouseDownPosScreenCoordinates.subtract(screenLoc).magnitude() < 3) {
                  if (texturedObjectClickedUpon != null) {
                     selectedObjects.add(texturedObjectClickedUpon);
                     texturedObjectClickedUpon.setSelected(true);

                     // notify any watcher that the mouse is up, and of the selected item
                     for (ISelectionWatcher watcher : selectionWatchers) {
                        watcher.ObjectSelected(texturedObjectClickedUpon, this, true);
                        watcher.onMouseUp(texturedObjectClickedUpon, event, texturedObjectClickedUponAngleFromCenter,
                                          texturedObjectClickedUponNormalizedDistanceFromCenter);
                     }
                  }
               }
            }
            buttonDown = 0;
            mouseDownPosWorldCoordinates = null;
            mouseDownPosScreenCoordinates = null;
            initialMouseDownCameraPosition = null;
            initialMouseDownPosScreenCoordinates = null;
            texturedObjectClickedUpon = null;
            texturedObjectClickedUponAngleFromCenter = 0;
            texturedObjectClickedUponNormalizedDistanceFromCenter = 0;
            drawScene(event.display);

         }
         else if (event.type == SWT.MouseMove) {
            if ((buttonDown == BUTTON_DRAG) && (mouseDownPosWorldCoordinates != null) && (initialMouseDownCameraPosition != null)) {
               if (allowDrag) {
                  GL11.glLoadIdentity();
                  /******************/
                  // Save matrix state
                  GL11.glMatrixMode(GL11.GL_MODELVIEW);
                  GL11.glLoadIdentity();
                  GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
                  {
                     useAsCurrentCanvas();
                     cameraPosition = initialMouseDownCameraPosition;
                     setupView();
                     FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
                     GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);

                     FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
                     GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrix);

                     IntBuffer viewport = BufferUtils.createIntBuffer(16);
                     GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

                     Tuple3 newMouseWorldPos = GetWorldPositionAtScreenLocAndWorldYPos(screenLoc, mouseDownPosWorldCoordinates.getY(), modelMatrix,
                                                                                       projMatrix, viewport);
                     Tuple3 movementSinceMouseDown = newMouseWorldPos.subtract(mouseDownPosWorldCoordinates);
                     cameraPosition = initialMouseDownCameraPosition.add(movementSinceMouseDown);

                     cameraPosition = new Tuple3(Math.min(-minOccupiedPosition.getX(), Math.max(cameraPosition.getX(), -maxOccupiedPosition.getZ())),
                                                 cameraPosition.getY(),
                                                 Math.min(-minOccupiedPosition.getX(), Math.max(cameraPosition.getZ(), -maxOccupiedPosition.getZ()))
                                                  );
                     GL11.glPopMatrix();
                     //System.out.println("camera now at " + cameraPosition.toString());
                  }
                  // drag complete
                  drawScene(event.display);
                  return;
               }
            }
            else if ((buttonDown == BUTTON_PAN) && (mouseDownPosScreenCoordinates != null)) {
               if (allowPan) {
                  float dx = mouseDownPosScreenCoordinates.getX() - screenLoc.getX();
                  float dy = mouseDownPosScreenCoordinates.getY() - screenLoc.getY();
                  mouseDownPosScreenCoordinates = screenLoc;
                  yRot -= (fieldOfViewYangle * dy) / height;
                  xRot -= (fieldOfViewYangle * aspectRatio * dx) / width;

                  for (IGLViewListener listener : listeners) {
                     listener.viewAngleChanged((float)((xRot * Math.PI) / 180), (float)((yRot * Math.PI) / 180));
                  }
                  GLU.gluPerspective(fieldOfViewYangle, aspectRatio, zNear, zFar);
                  // pan complete
                  drawScene(event.display);
                  return;
               }
            }
            if ((buttonDown != 0) || watchMouseMove) {
               GL11.glLoadIdentity();
               /******************/
               // Save matrix state
               GL11.glMatrixMode(GL11.GL_MODELVIEW);
               GL11.glLoadIdentity();
               GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
               {
                  setupView();
                  WorldCoordinatesResults results = findWorldCoordinatedForScreenLocation(screenLoc);
                  texturedObjectClickedUpon = results.object;
                  texturedObjectClickedUponAngleFromCenter = results.angleFromCenter;
                  texturedObjectClickedUponNormalizedDistanceFromCenter = results.normalizedDistanceFromCenter;

                  GL11.glPopMatrix();
               }
               // notify any watcher that the mouse is up, and of the selected item
               if (texturedObjectClickedUpon != null) {
                  for (ISelectionWatcher watcher : selectionWatchers) {
                     if (buttonDown != 0) {
                        watcher.onMouseDrag(texturedObjectClickedUpon, event, texturedObjectClickedUponAngleFromCenter,
                                            texturedObjectClickedUponNormalizedDistanceFromCenter);
                     }
                     else if (watchMouseMove) {
                        watcher.onMouseMove(texturedObjectClickedUpon, event, texturedObjectClickedUponAngleFromCenter,
                                            texturedObjectClickedUponNormalizedDistanceFromCenter);
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
            float fieldOfViewYangleBefore = fieldOfViewYangle;
            zoom((event.count / 3), event.display);
            // Now pan the view so the position under the mouse point wont seem to have moved at all
            if (fieldOfViewYangleBefore != fieldOfViewYangle) {
               yRot += ((fieldOfViewYangle - fieldOfViewYangleBefore) * (event.y - (height / 2.0))) / height;
               xRot += ((fieldOfViewYangle - fieldOfViewYangleBefore) * (event.x - (width / 2.0))) / width;
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
      this.elevationPower = Math.min(Math.max(MIN_ELEVATION_POWER, elevationPower), MAX_ELEVATION_POWER);
      cameraPosition = new Tuple3(cameraPosition.getX(), getHeightAtCurrentElevationPower(), cameraPosition.getZ());
   }

   public void setHeightScaleByApproximateHeightInInches(float desiredHeightInInchesIn) {
      float desiredHeightInInches = Math.min(desiredHeightInInchesIn, zFar);

      for (int scale = 0; scale < MAX_ELEVATION_POWER; scale++) {
         if (-getHeightInInchesForElevationPower(scale) > desiredHeightInInches) {
            setElevationPower(scale);
            return;
         }
      }
      setElevationPower(MAX_ELEVATION_POWER);
   }

   private float getHeightInInchesForElevationPower(int elevationPower) {
      return (float) (-heightScale * Math.pow(1.5, elevationPower));
   }
   private float getHeightAtCurrentElevationPower() {
      float val = getHeightInInchesForElevationPower(elevationPower);
      if (val > zFar) {
         elevationPower--;
         return getHeightAtCurrentElevationPower();
      }
      return val;
   }

   public void addSelectionWatcher(ISelectionWatcher watcher) {
      selectionWatchers.add(watcher);
   }

   public boolean removeSelectionWatcher(ISelectionWatcher watcher) {
      return selectionWatchers.remove(watcher);
   }

   //   private String convertMatrixToString(IntBuffer matrixBuffer) {
   //      int size = matrixBuffer.capacity();
   //      List<String> list = new ArrayList<>();
   //      for (int i=0 ; i<size ; i++) {
   //         list.add(String.valueOf(matrixBuffer.get(i)));
   //      }
   //      return formatStringMatrix(list);
   //   }
   //   private String convertMatrixToString(FloatBuffer matrixBuffer) {
   //      int size = matrixBuffer.capacity();
   //      List<String> list = new ArrayList<>();
   //      for (int i=0 ; i<size ; i++) {
   //         list.add(String.valueOf(matrixBuffer.get(i)));
   //      }
   //      return formatStringMatrix(list);
   //   }
   //
   //   private String formatStringMatrix(List<String> list) {
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

      public TexturedObject object                       = null; // texturedObjectClickedUpon
      public double         angleFromCenter              = 0;   // texturedObjectClickedUponAngleFromCenter
      public double         normalizedDistanceFromCenter = 0;   // texturedObjectClickedUponNormalizedDistanceFromCenter
      public Tuple3         worldCoordinates             = null;
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

      synchronized (models) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_models)) {
            for (TexturedObject model : models) {
               Tuple3 objectLocationInWindowCoords = model.getScreenPosition3dContainingScreenPoint(invertedScreenLoc, modelMatrix, projMatrix, viewport);
               if (objectLocationInWindowCoords != null) {
                  if (objectLocationInWindowCoords.getZ() > 0) {
                     if ((lowestZObjectLocationInScreenCoordinated == null)
                              || (objectLocationInWindowCoords.getZ() < lowestZObjectLocationInScreenCoordinated.getZ())) {
                        lowestZObjectLocationInScreenCoordinated = objectLocationInWindowCoords;
                        results.object = model;
                     }
                  }
               }
            }
         }
      }
      //long duration = System.currentTimeMillis() - startTime;
      //startTime = System.currentTimeMillis();
      //System.out.println("findWorldCoordinatedForScreenLocation point #1 took " + duration + "ms. (" + models.size() + " models)");

      if (results.object != null) {
         FloatBuffer clickPosInWorldCoordsBuffer = BufferUtils.createFloatBuffer(3);
         GLU.gluUnProject(lowestZObjectLocationInScreenCoordinated.getX(), lowestZObjectLocationInScreenCoordinated.getY(),
                          lowestZObjectLocationInScreenCoordinated.getZ(), modelMatrix, projMatrix, viewport, clickPosInWorldCoordsBuffer);
         Tuple3 clickPosInWorldCoords = new Tuple3(clickPosInWorldCoordsBuffer);
         Tuple3 objCenter = results.object.getObjectCenter();
         Tuple3 delta = objCenter.subtract(clickPosInWorldCoords);
         results.angleFromCenter = Math.atan2(delta.getZ(), delta.getX());
         double distanceFromCenter = Math.sqrt((delta.getX() * delta.getX()) + (delta.getZ() * delta.getZ()));
         Tuple3 objectBoundingDimensions = results.object.getObjectBoundingCubeDimensions();
         double flatObjectRadius = Math.sqrt((objectBoundingDimensions.getX() * objectBoundingDimensions.getX()) + (objectBoundingDimensions.getZ()
                                             * objectBoundingDimensions.getZ()));
         results.normalizedDistanceFromCenter = distanceFromCenter / (flatObjectRadius / 2);
         //duration = System.currentTimeMillis() - startTime;
         //startTime = System.currentTimeMillis();
         //System.out.println("findWorldCoordinatedForScreenLocation point #2 took " + duration + "ms.");
      }
      if (lowestZObjectLocationInScreenCoordinated != null) {
         FloatBuffer objectPosition3d = BufferUtils.createFloatBuffer(3);
         GLU.gluUnProject(lowestZObjectLocationInScreenCoordinated.getX(), lowestZObjectLocationInScreenCoordinated.getY(),
                          lowestZObjectLocationInScreenCoordinated.getZ(), modelMatrix, projMatrix, viewport, objectPosition3d);
         results.worldCoordinates = new Tuple3(objectPosition3d.get(0), objectPosition3d.get(1), objectPosition3d.get(2));
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
      Rectangle bounds = glCanvas.getBounds();
      width = bounds.width;
      height = bounds.height;
      if (zoomButtons != null) {
         zoomLeft = width - zoomButtons.getWidth() - 10;
         zoomTop = height - zoomButtons.getHeight() - 10;
      }
      if (elevationButtons != null) {
         elevationLeft = width - elevationButtons.getWidth() - 10;
         elevationTop = zoomTop - elevationButtons.getHeight();
      }
      if (width == 0) {
         width = 800;
      }
      if (height == 0) {
         height = 600;
      }
      aspectRatio = ((float) width) / height;

      if (!useAsCurrentCanvas()) {
         return;
      }
      GL11.glViewport(0, 0, width, height);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GLU.gluPerspective(fieldOfViewYangle, aspectRatio, zNear, zFar);
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
      GL11.glOrtho(0, width, height, 0, -1, 1);
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
      return textureLoader;
   }

   public Message addString(int fontIndex, String text, int x, int y, int z) {
      Message msg = new Message();
      msg.font = fontIndex;
      msg.text = text;
      msg.xLoc = x;
      msg.yLoc = y;
      msg.zLoc = z;
      messages.add(msg);
      return msg;
   }

   public void addModel(TexturedObject model) {
      synchronized (models) {
         lock_models.check();
         models.add(model);
      }
   }
   public void setMapExtents(int x, int y) {
      int buffer = 300;
       maxOccupiedPosition = new Tuple3((rad15 * x) + buffer, 0, (rad32 * y) + buffer);
       minOccupiedPosition = new Tuple3(-buffer, 0f, -buffer);
   }

   public void addMessage(Message message) {
      synchronized (messages) {
         lock_messages.check();
         messages.add(message);
      }
   }

   public void setWatchMouseMove(boolean watchMouseMove) {
      this.watchMouseMove = watchMouseMove;
   }

   public void addViewListener(IGLViewListener listener) {
      listeners.add(listener);
   }
}

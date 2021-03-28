package ostrowski.graphics.model;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import ostrowski.graphics.GLView;
import ostrowski.graphics.objects3d.BodyPart;
import ostrowski.graphics.objects3d.HumanBody;
import ostrowski.util.SemaphoreAutoTracker;

/**
 * A OBJ model implementation that renders the data as a display
 * list.
 *
 */
public class ObjModel
{
   /** The identifier of the display list held for this model */
   private int           listID;
   public ObjData        data;
   private final boolean useList  = false;
   public Tuple3         location = new Tuple3(0, 0, 0);

   /**
    * Create a new OBJ model that will render the object data
    * specified in OpenGL
    *
    * @param data The data to be rendered for this model
    */
   public ObjModel(ObjData data, GLView glView) {
      if (useList) {
         // we're going to process the OBJ data and produce a display list
         // that will display the model. First we ask OpenGL to create
         // us a display list
         listID = GL11.glGenLists(1);

         // next we start producing the contents of the list
         GL11.glNewList(listID, GL11.GL_COMPILE);
         List<Message> messages = new ArrayList<>();
         renderWork(data, glView, messages);
         GL11.glEndList();
      }
      else {
         this.data = data;
      }
   }

   protected void renderWork(ObjData data, GLView glView, List<Message> messages) {
      renderData(data);
      if (data instanceof ObjHex) {
         ObjHex hex = (ObjHex) data;
         synchronized (hex.lock_humans) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(hex.lock_humans)) {
               for (HumanBody human : hex.humans) {
                  human.render(glView, messages);
               }
            }
         }
         synchronized (hex.lock_texturedObjects) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(hex.lock_texturedObjects)) {
               for (TexturedObject obj : hex.texturedObjects) {
                  obj.render(glView, messages);
               }
            }
         }
         synchronized (hex.lock_objects) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(hex.lock_objects)) {
               for (ObjData object : hex.objects) {
                  renderData(object);
               }
            }
         }
         Message message = hex.getMessage();
         if (message != null) {
            Tuple3 avePoint = data.getAveragePoint();
            GL11.glPushMatrix();
            {
               GL11.glTranslatef(avePoint.getX(), avePoint.getY(), avePoint.getZ());
               FloatBuffer hexLoc =  BodyPart.projectToWindowLocation();
               int x = (int) hexLoc.get(0);
               int y = (int) hexLoc.get(1);
               int z = (int) hexLoc.get(2);
               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               message.xLoc = x;
               message.yLoc = y;
               message.zLoc = z;
               messages.add(message);
               GL11.glPopMatrix();
            }
         }
      }
   }

   private static void renderData(ObjData data) {
      // cycle through all the faces in the model data
      // rendering a triangle for it
      int currentFaceCountSet = 3;
      GL11.glBegin(GL11.GL_TRIANGLES);
      {
         int faceCount = data.getFaceCount();
         for (int i = 0; i < faceCount; i++) {
            Face face = data.getFace(i);
            if (currentFaceCountSet != face.vertexCount) {
               currentFaceCountSet = face.vertexCount;
               GL11.glEnd();
               if (currentFaceCountSet == 3) {
                  GL11.glBegin(GL11.GL_TRIANGLES);
               }
               else if (currentFaceCountSet == 4) {
                  GL11.glBegin(GL11.GL_QUADS);
               }
               else {
                  throw new RuntimeException("Only triangles and quads are supported");
               }
            }
            for (int v = 0; v < face.vertexCount; v++) {
               // a position, normal and 32 coordinate
               // for each vertex in the face
               Tuple3 vert = face.getVertex(v);
               Tuple3 norm = face.getNormal(v);
               Tuple2 tex = face.getTexCoord(v);

               GL11.glNormal3f(norm.getX(), norm.getY(), norm.getZ());
               GL11.glTexCoord2f(tex.getX(), tex.getY());
               GL11.glVertex3f(vert.getX(), vert.getY(), vert.getZ());
            }
         }
         GL11.glEnd();
      }
   }

   public Tuple3 getScreenPosition3dContainingScreenPoint(Tuple2 invertedScreenLoc, FloatBuffer modelMatrix, FloatBuffer projMatrix, IntBuffer viewport) {
      Tuple3 lowestZCoordinates = null;
      List<Tuple3> winPos3dList = new ArrayList<>();

      float screenRightEdge = viewport.get(2);
      float screenBottomEdge = viewport.get(3);
      float screenLocX = invertedScreenLoc.getX();
      float screenLocY = invertedScreenLoc.getY();
      FloatBuffer win_pos = BufferUtils.createFloatBuffer(3);

      int faceCount = data.getFaceCount();
      for (int i = 0; i < faceCount; i++) {
         boolean pointsToLeftOfScreenLoc = false;
         boolean pointsToRightOfScreenLoc = false;
         boolean pointsToTopOfScreenLoc = false;
         boolean pointsToBottomOfScreenLoc = false;

         boolean pointsToLeftOfScreenEdge = false;
         boolean pointsToRightOfScreenEdge = false;
         boolean pointsToTopOfScreenEdge = false;
         boolean pointsToBottomOfScreenEdge = false;

         Face face = data.getFace(i);
         winPos3dList.clear();
         for (int v = 0; v < face.vertexCount; v++) {
            // a position, normal and 32 coordinate
            // for each vertex in the face
            Tuple3 vert = face.getVertex(v);
            GLU.gluProject(vert.getX(), vert.getY(), vert.getZ(), modelMatrix, projMatrix, viewport, win_pos);
            float x = win_pos.get(0);
            float y = win_pos.get(1);
            float z = win_pos.get(2);
            winPos3dList.add(new Tuple3(x, y, z));

            if (screenLocX >= x) {
               pointsToLeftOfScreenLoc = true;
            }
            else if (screenLocX <= x) {
               pointsToRightOfScreenLoc = true;
            }

            if (screenLocY >= y) {
               pointsToTopOfScreenLoc = true;
            }
            else if (screenLocY <= y) {
               pointsToBottomOfScreenLoc = true;
            }

            if (x < 0) {
               pointsToLeftOfScreenEdge = true;
            }
            else if (x > screenRightEdge) {
               pointsToRightOfScreenEdge = true;
            }

            if (y < 0) {
               pointsToTopOfScreenEdge = true;
            }
            else if (y > screenBottomEdge) {
               pointsToBottomOfScreenEdge = true;
            }
         }
         if (pointsToBottomOfScreenEdge && pointsToTopOfScreenEdge) {
            continue;
         }
         if (pointsToLeftOfScreenEdge && pointsToRightOfScreenEdge) {
            continue;
         }
         if (pointsToLeftOfScreenLoc && pointsToRightOfScreenLoc && pointsToTopOfScreenLoc && pointsToBottomOfScreenLoc) {
            if (triangleContainsPoint(winPos3dList.get(0), winPos3dList.get(1), winPos3dList.get(2), invertedScreenLoc)) {
               float winPosZ = findZcoordOfPointOnPlane(winPos3dList.get(0), winPos3dList.get(1), winPos3dList.get(2), invertedScreenLoc);
               if ((lowestZCoordinates == null) || (winPosZ < lowestZCoordinates.getZ())) {
                  lowestZCoordinates = new Tuple3(screenLocX, screenLocY, winPosZ);
               }
            }
            else if (winPos3dList.size() == 4) {
               if (triangleContainsPoint(winPos3dList.get(2), winPos3dList.get(3), winPos3dList.get(0), invertedScreenLoc)) {
                  float winPosZ = findZcoordOfPointOnPlane(winPos3dList.get(2), winPos3dList.get(3), winPos3dList.get(0), invertedScreenLoc);
                  if ((lowestZCoordinates == null) || (winPosZ < lowestZCoordinates.getZ())) {
                     lowestZCoordinates = new Tuple3(screenLocX, screenLocY, winPosZ);
                  }
               }
            }
         }
      }
      return lowestZCoordinates;
   }

   // see http://local.wasp.uwa.edu.au/~pbourke/geometry/planeeq/
   //A = y1 (z2 - z3) + y2 (z3 - z1) + y3 (z1 - z2)
   //B = z1 (x2 - x3) + z2 (x3 - x1) + z3 (x1 - x2)
   //C = x1 (y2 - y3) + x2 (y3 - y1) + x3 (y1 - y2)
   //- D = x1 (y2 z3 - y3 z2) + x2 (y3 z1 - y1 z3) + x3 (y1 z2 - y2 z1)
   private static float findZcoordOfPointOnPlane(Tuple3 v1, Tuple3 v2, Tuple3 v3, Tuple2 P) {
      float A = (v1.getY() * (v2.getZ() - v3.getZ())) + (v2.getY() * (v3.getZ() - v1.getZ())) + (v3.getY() * (v1.getZ() - v2.getZ()));
      float B = (v1.getZ() * (v2.getX() - v3.getX())) + (v2.getZ() * (v3.getX() - v1.getX())) + (v3.getZ() * (v1.getX() - v2.getX()));
      float C = (v1.getX() * (v2.getY() - v3.getY())) + (v2.getX() * (v3.getY() - v1.getY())) + (v3.getX() * (v1.getY() - v2.getY()));
      float D = (v1.getX() * ((v2.getY() * v3.getZ()) - (v3.getY() * v2.getZ()))) + (v2.getX() * ((v3.getY() * v1.getZ()) - (v1.getY() * v3.getZ()))) + (v3.getX()
                * ((v1.getY() * v2.getZ()) - (v2.getY() * v1.getZ())));
      //Ax + By + Cz = D
      // z = (D - Ax - By) / C
      return (D - (A * P.getX()) - (B * P.getY())) / C;
   }

   // see http://www.blackpawn.com/texts/pointinpoly/default.html for more information on barycentric coordinates
   private static boolean triangleContainsPoint(Tuple3 A, Tuple3 B, Tuple3 C, Tuple2 P) {
      // Compute vectors
      Tuple2 v0 = new Tuple2(C.getX() - A.getX(), C.getY() - A.getY());
      Tuple2 v1 = new Tuple2(B.getX() - A.getX(), B.getY() - A.getY());
      Tuple2 v2 = new Tuple2(P.getX() - A.getX(), P.getY() - A.getY());

      // Compute dot products
      float dot00 = v0.dotProduct(v0);
      float dot01 = v0.dotProduct(v1);
      float dot02 = v0.dotProduct(v2);
      float dot11 = v1.dotProduct(v1);
      float dot12 = v1.dotProduct(v2);

      // Compute barycentric coordinates
      float invDenom = 1 / ((dot00 * dot11) - (dot01 * dot01));
      float u = ((dot11 * dot02) - (dot01 * dot12)) * invDenom;
      if (u <= 0) {
         return false;
      }
      float v = ((dot00 * dot12) - (dot01 * dot02)) * invDenom;
      if (v <= 0) {
         return false;
      }

      // Check if point is in triangle
      return ((u + v) < 1);
   }

   /**
    * Render the OBJ Model
    */
   public void render(GLView glView, List<Message> messages) {
      GL11.glTranslatef(location.getX(), location.getY(), location.getZ());

      if (useList) {
         // since we rendered our display list at construction we
         // can now just call this list causing it to be rendered
         // to the screen
         GL11.glCallList(listID);
      }
      else {
         renderWork(data, glView, messages);
      }
   }

   public void scale(double xScale, double yScale, double zScale) {
      data.scale(xScale, yScale, zScale);
   }

   public void move(double xOffset, double yOffset, double zOffset) {
      data.move(xOffset, yOffset, zOffset);
   }

   public void invertNormals() {
      data.scaleNormals(-1.0, -1.0, -1.0);
      for (Face face : data.faces) {
         face.invertNormal();
      }
   }

   public void reverseRightToLeft() {
      scale(-1.0, 1.0, 1.0);
      data.scaleNormals(-1.0, -1.0, -1.0);
      for (Face face : data.faces) {
         face.invertNormal();
      }
   }

   public Tuple3 getObjectBoundingCubeMinPoints(Tuple3 minIn) {
      if ((data.verts == null) || (data.verts.size() == 0)) {
         return minIn;
      }
      Tuple3 min;
      int i = 0;
      if (minIn == null) {
         min = data.verts.get(0);
         i = 1;
      }
      else {
         min = minIn;
      }
      for (; i < data.verts.size(); i++) {
         Tuple3 vert = data.verts.get(i);
         min = new Tuple3(Math.min(min.getX(), vert.getX()), Math.min(min.getY(), vert.getY()), Math.min(min.getZ(), vert.getZ()));
      }
      return min;
   }

   public Tuple3 getObjectBoundingCubeMaxPoints(Tuple3 maxIn) {
      if ((data.verts == null) || (data.verts.size() == 0)) {
         return maxIn;
      }
      Tuple3 max;
      int i = 0;
      if (maxIn == null) {
         max = data.verts.get(0);
         i = 1;
      }
      else {
         max = maxIn;
      }
      for (; i < data.verts.size(); i++) {
         Tuple3 vert = data.verts.get(i);
         max = new Tuple3(Math.max(max.getX(), vert.getX()), Math.max(max.getY(), vert.getY()), Math.max(max.getZ(), vert.getZ()));
      }
      return max;
   }

   public Tuple3 getObjectCenter() {
      if (data instanceof ObjHex) {
         return ((ObjHex) data).centerLocation();
      }
      return location;
   }
}
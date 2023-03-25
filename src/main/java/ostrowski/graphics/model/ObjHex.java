package ostrowski.graphics.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ostrowski.graphics.GLView;
import ostrowski.graphics.objects3d.HumanBody;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;

/**
 * The data that has been read from the Wavefront .obj file. This is
 * kept separate from the actual rendering with the hope the data
 * might be used for some other rendering engine in the future.
 *
 */
public class ObjHex extends ObjData
{
   public enum Terrain {
      ICE,    WATER_SHALLOW, WATER,       WATER_DEEP,
      GRASS,  BUSH,          BUSH_DENSE,  TREE,
      PAVERS, MARBLE,        ROCK,        RESERVED1,
      DIRT,   GRAVEL,        MUD,         RESERVED2,
   }
   private static int COLS_IN_TERRAIN = 4;
   private static int ROWS_IN_TERRAIN = 4;
   private static float COL_WIDTH = 1.0f / COLS_IN_TERRAIN;
   private static float ROW_HEIGHT = 1.0f / ROWS_IN_TERRAIN;
   private static float CENTER_OFFSET_X = COL_WIDTH / 2f;
   private static float CENTER_OFFSET_Y = ROW_HEIGHT / 2f;
   // establish an outer edge of the pattern which will prevent texture bleeding
   private static float EDGE_OFFSET_FROM_CENTER_X = CENTER_OFFSET_X * .95f;
   private static float EDGE_OFFSET_FROM_CENTER_Y = CENTER_OFFSET_Y * .95f;

   private static double PI_3rd = Math.PI / 3.0;

   // No need to protect access to boundsVerts, its only modified in the ctor
   private final List<Tuple3> boundsVerts = new ArrayList<>();
   long uniqueNumericKey = -1;
   public final Semaphore            lock_humans          = new Semaphore("ObjHex_humans", AnimationControllerSemaphore.CLASS_OBJHEX_HUMANS);
   public final Semaphore            lock_texturedObjects = new Semaphore("ObjHex_texturedObjects", AnimationControllerSemaphore.CLASS_OBJHEX_TEXTUREDOBJECTS);
   public final Semaphore            lock_objects         = new Semaphore("ObjHex_objects", AnimationControllerSemaphore.CLASS_OBJHEX_OBJECTS);
   public final List<HumanBody>      humans               = new ArrayList<>();
   public final List<TexturedObject> texturedObjects      = new ArrayList<>();
   public final List<ObjData>        objects              = new ArrayList<>();
   private      Message              message;

   public ObjHex(Tuple3 center, float edgeRadius, float boundsRadius, Terrain terrain, long uniqueNumericKey) {
      super();
      this.uniqueNumericKey = uniqueNumericKey;
      Tuple3 normal = new Tuple3(0, 0, 1);
      Tuple2 texCenter;
      int row = terrain.ordinal() / COLS_IN_TERRAIN;
      int col = terrain.ordinal() % COLS_IN_TERRAIN;
      texCenter = new Tuple2(CENTER_OFFSET_X + (col * COL_WIDTH),
                             1.0f-(CENTER_OFFSET_Y + (row * ROW_HEIGHT)));
      // get a random initial angle, based upon the x & y position,
      // so that no two hexes have the exact same texture orientation:
      Random rnd = new Random(uniqueNumericKey); rnd.nextFloat();
      double textureAngle = rnd.nextFloat() * Math.PI * 2.0;
      double worldAngle = Math.PI / 2.0;
      List<Tuple2> texCoords = new ArrayList<>();
      for (int i = 0; i < 6; i++) {
         textureAngle += PI_3rd;
         worldAngle += PI_3rd;
         Tuple3 edgeVert = center.add((float)Math.sin(worldAngle) * edgeRadius, 0f,
                                      (float)Math.cos(worldAngle) * edgeRadius);
         Tuple3 boundsVert = center.add((float)Math.sin(worldAngle) * boundsRadius, 0f,
                                        (float)Math.cos(worldAngle) * boundsRadius);
         Tuple2 textureCoord = texCenter.add((float) Math.sin(textureAngle) * EDGE_OFFSET_FROM_CENTER_Y,
                                             (float) Math.cos(textureAngle) * EDGE_OFFSET_FROM_CENTER_X);
         boundsVerts.add(boundsVert);
         verts.add(edgeVert);
         texCoords.add(textureCoord);
      }
      for (int i = 0; i < 6; i++) {
         Face face = new Face(3);
         face.addPoint(center, texCenter, normal);
         face.addPoint(verts.get(i), texCoords.get(i), normal);
         int j = (i + 1) % 6;
         face.addPoint(verts.get(j), texCoords.get(j), normal);
         faces.add(face);
      }
   }
   public void addWall(int pointStart, int pointEnd, float wallHeight, float thickness, boolean hasDoor, boolean doorIsOpen) {
      int vertStart = pointStart / 2;
      int vertEnd   = pointEnd / 2;
      Tuple3 startGround = boundsVerts.get(vertStart);
      Tuple3 endGround = boundsVerts.get(vertEnd);
      if ((pointStart % 2) == 1) {
         int nextStart = (vertStart + 1) % 6;
         Tuple3 nextTuple = boundsVerts.get(nextStart);
         startGround = startGround.add(nextTuple).divide(2f);
      }
      if ((pointEnd % 2) == 1) {
         int nextEnd = (vertEnd + 1) % 6;
         Tuple3 nextTuple = boundsVerts.get(nextEnd);
         endGround = endGround.add(nextTuple).divide(2f);
      }
      Tuple3 startHigh = startGround.add(0, wallHeight, 0);
      Tuple3 endHigh = endGround.add(0, wallHeight, 0);

      Tuple2 textureCoordStartGround = new Tuple2(0.752f, 0.252f);
      Tuple2 textureCoordStartHigh   = new Tuple2(0.752f, 0.498f);
      Tuple2 textureCoordEndGround   = new Tuple2(0.998f, 0.252f);
      Tuple2 textureCoordEndHigh     = new Tuple2(0.998f, 0.498f);

      Tuple3 normalFront = startGround.subtract(startHigh).crossProduct(startGround.subtract(endGround)).normalize();
      //Tuple3 normalBack = normalFront.multiply(-1f);

      Tuple3 thicknessAdj = normalFront.multiply(thickness / 2f);
      Tuple3 startGroundFront = startGround.add(thicknessAdj);
      Tuple3 endGroundFront = endGround.add(thicknessAdj);
      Tuple3 startHighFront = startHigh.add(thicknessAdj);
      Tuple3 endHighFront = endHigh.add(thicknessAdj);
      Tuple3 startGroundBack = startGround.subtract(thicknessAdj);
      Tuple3 endGroundBack = endGround.subtract(thicknessAdj);
      Tuple3 startHighBack = startHigh.subtract(thicknessAdj);
      Tuple3 endHighBack = endHigh.subtract(thicknessAdj);

      verts.add(startGroundFront);
      verts.add(endGroundFront);
      verts.add(startHighFront);
      verts.add(endHighFront);
      verts.add(startGroundBack);
      verts.add(endGroundBack);
      verts.add(startHighBack);
      verts.add(endHighBack);

      double horizontalDistance = endGroundFront.subtract(startGroundFront).magnitude();
      int horizontalTextureRepeatCount = (int) (horizontalDistance / 20) + 1;
      int verticalTextureRepeatCount = (int) (wallHeight / 20) + 1;
      if (hasDoor) {
         int frameWidth = 9;
         Tuple3 doorFrameEndHighFront   = endHighFront.multiply(frameWidth).add(startHighFront).divide(frameWidth+1);
         Tuple3 doorFrameEndGroundFront = endGroundFront.multiply(frameWidth).add(startGroundFront).divide(frameWidth+1);
         Tuple3 doorFrameEndHighBack    = endHighBack.multiply(frameWidth).add(startHighBack).divide(frameWidth+1);
         Tuple3 doorFrameEndGroundBack  = endGroundBack.multiply(frameWidth).add(startGroundBack).divide(frameWidth+1);

         Tuple3 doorFrameStartHighFront   = startHighFront.multiply(frameWidth).add(endHighFront).divide(frameWidth+1);
         Tuple3 doorFrameStartGroundFront = startGroundFront.multiply(frameWidth).add(endGroundFront).divide(frameWidth+1);
         Tuple3 doorFrameStartHighBack    = startHighBack.multiply(frameWidth).add(endHighBack).divide(frameWidth+1);
         Tuple3 doorFrameStartGroundBack  = startGroundBack.multiply(frameWidth).add(endGroundBack).divide(frameWidth+1);


         int doorWidth = 3;
         Tuple3 doorEndHighFront   = doorFrameEndHighFront.multiply(doorWidth).add(doorFrameEndHighBack).divide(doorWidth+1);
         Tuple3 doorEndGroundFront = doorFrameEndGroundFront.multiply(doorWidth).add(doorFrameEndGroundBack).divide(doorWidth+1);
         Tuple3 doorEndHighBack    = doorFrameEndHighBack.multiply(doorWidth).add(doorFrameEndHighFront).divide(doorWidth+1);
         Tuple3 doorEndGroundBack  = doorFrameEndGroundBack.multiply(doorWidth).add(doorFrameEndGroundFront).divide(doorWidth+1);

         Tuple3 doorStartHighFront   = doorFrameStartHighFront.multiply(doorWidth).add(doorFrameStartHighBack).divide(doorWidth+1);
         Tuple3 doorStartGroundFront = doorFrameStartGroundFront.multiply(doorWidth).add(doorFrameStartGroundBack).divide(doorWidth+1);
         Tuple3 doorStartHighBack    = doorFrameStartHighBack.multiply(doorWidth).add(doorFrameStartHighFront).divide(doorWidth+1);
         Tuple3 doorStartGroundBack  = doorFrameStartGroundBack.multiply(doorWidth).add(doorFrameStartGroundFront).divide(doorWidth+1);

         // start door frame
         addCube(startGroundFront, doorFrameStartGroundFront, startHighFront, doorFrameStartHighFront,
                 startGroundBack,  doorFrameStartGroundBack,  startHighBack,  doorFrameStartHighBack,
                 textureCoordStartGround, textureCoordStartHigh, textureCoordEndGround, textureCoordEndHigh,
                 (horizontalTextureRepeatCount / frameWidth)+1, verticalTextureRepeatCount);

         // end door frame
         addCube(doorFrameEndGroundFront, endGroundFront, doorFrameEndHighFront, endHighFront,
                 doorFrameEndGroundBack,  endGroundBack,  doorFrameEndHighBack,  endHighBack,
                 textureCoordStartGround, textureCoordStartHigh, textureCoordEndGround, textureCoordEndHigh,
                 (horizontalTextureRepeatCount / frameWidth)+1, verticalTextureRepeatCount);

         if (!doorIsOpen) {
            Tuple2 doorTextureCoordStartGround = new Tuple2(0.752f, 0.248f);
            Tuple2 doorTextureCoordStartHigh   = new Tuple2(0.752f, 0.002f);
            Tuple2 doorTextureCoordEndGround   = new Tuple2(0.998f, 0.248f);
            Tuple2 doorTextureCoordEndHigh     = new Tuple2(0.998f, 0.002f);

            addCube(doorStartGroundFront, doorEndGroundFront, doorStartHighFront, doorEndHighFront,
                    doorStartGroundBack,  doorEndGroundBack,  doorStartHighBack,  doorEndHighBack,
                    doorTextureCoordStartGround, doorTextureCoordStartHigh, doorTextureCoordEndGround, doorTextureCoordEndHigh,
                    (horizontalTextureRepeatCount / frameWidth)+1, 1/*verticalTextureRepeatCount*/);

         }
      }
      else {
         addCube(startGroundFront, endGroundFront, startHighFront, endHighFront,
                 startGroundBack,  endGroundBack,  startHighBack,  endHighBack,
                 textureCoordStartGround, textureCoordStartHigh, textureCoordEndGround, textureCoordEndHigh,
                 horizontalTextureRepeatCount, verticalTextureRepeatCount);
      }
   }
   private void addCube(Tuple3 startGroundFront, Tuple3 endGroundFront, Tuple3 startHighFront, Tuple3 endHighFront,
                        Tuple3 startGroundBack, Tuple3 endGroundBack, Tuple3 startHighBack, Tuple3 endHighBack,
                        Tuple2 textureCoordStartGround, Tuple2 textureCoordStartHigh, Tuple2 textureCoordEndGround, Tuple2 textureCoordEndHigh,
                        int horizontalTextureRepeatCount, int verticalTextureRepeatCount) {
      // front face
      addRectangle(startGroundFront, startHighFront, endHighFront, endGroundFront,
                   textureCoordStartGround, textureCoordStartHigh, textureCoordEndHigh, textureCoordEndGround,
                   horizontalTextureRepeatCount, verticalTextureRepeatCount);//, normalFront);

      // back face
      addRectangle(endGroundBack, endHighBack, startHighBack, startGroundBack,
                   textureCoordEndGround, textureCoordEndHigh, textureCoordStartHigh, textureCoordStartGround,
                   horizontalTextureRepeatCount, verticalTextureRepeatCount);//, normalBack);

      // start face
      //Tuple3 normalStart = startHighFront.subtract(startGroundFront).crossProduct(startGroundFront.subtract(startGroundBack)).normalize();
      addRectangle(startGroundBack, startHighBack, startHighFront, startGroundFront,
                   textureCoordStartGround, textureCoordStartHigh, textureCoordStartHigh, textureCoordStartGround,
                   horizontalTextureRepeatCount, verticalTextureRepeatCount);//, normalStart);

      // End face
      //Tuple3 normalEnd = normalStart.multiply(-1f);
      addRectangle(endGroundFront, endHighFront, endHighBack, endGroundBack,
                   textureCoordEndGround, textureCoordEndHigh, textureCoordEndHigh, textureCoordEndGround,
                   horizontalTextureRepeatCount, verticalTextureRepeatCount);

      // Top face
      //Tuple3 normalTop = endHighFront.subtract(endHighBack).crossProduct(endHighFront.subtract(startHighBack)).normalize();
      addRectangle(startHighFront, startHighBack, endHighBack, endHighFront,
                   textureCoordStartHigh, textureCoordStartHigh, textureCoordEndHigh, textureCoordEndHigh,
                   horizontalTextureRepeatCount, verticalTextureRepeatCount); //normalTop.multiply(-1f));
   }

   private void addRectangle(Tuple3 point1, Tuple3 point2, Tuple3 point3, Tuple3 point4,
                             Tuple2 texture1, Tuple2 texture2, Tuple2 texture3, Tuple2 texture4,
                             int horizontalTextureRepeatCount, int verticalTextureRepeatCount) {
      if (horizontalTextureRepeatCount > 1) {
         // This assumes 1&2 are on the left, 3&4 are on the right:
         Tuple3 diff1_4 = point4.subtract(point1);
         Tuple3 diff2_3 = point3.subtract(point2);
         Tuple3 delta1_4 = diff1_4.divide(horizontalTextureRepeatCount);
         Tuple3 delta2_3 = diff2_3.divide(horizontalTextureRepeatCount);
         for (int x=0 ; x<horizontalTextureRepeatCount ; x++) {
            Tuple3 point1a = point1.add(delta1_4.multiply(x));
            Tuple3 point2a = point2.add(delta2_3.multiply(x));
            Tuple3 point3a = point2.add(delta2_3.multiply(x+1));
            Tuple3 point4a = point1.add(delta1_4.multiply(x+1));
            addRectangle(point1a, point2a, point3a, point4a,
                         texture1, texture2, texture3, texture4,
                         1/*horizontalTextureRepeatCount*/, verticalTextureRepeatCount);
         }
         return;
      }
      if (verticalTextureRepeatCount > 1) {
         // This assumes 1&4 are on the bottom, 2&3 are on the top:
         Tuple3 diff1_2 = point2.subtract(point1);
         Tuple3 diff4_3 = point3.subtract(point4);
         Tuple3 delta1_2 = diff1_2.divide(verticalTextureRepeatCount);
         Tuple3 delta4_3 = diff4_3.divide(verticalTextureRepeatCount);
         for (int y=0 ; y<verticalTextureRepeatCount ; y++) {
            Tuple3 point1a = point1.add(delta1_2.multiply(y));
            Tuple3 point2a = point1.add(delta1_2.multiply(y+1));
            Tuple3 point3a = point4.add(delta4_3.multiply(y+1));
            Tuple3 point4a = point4.add(delta4_3.multiply(y));
            addRectangle(point1a, point2a, point3a, point4a,
                         texture1, texture2, texture3, texture4,
                         horizontalTextureRepeatCount, 1/*verticalTextureRepeatCount*/);
         }
         return;
      }
      // point data must come in in a clockwise order, as viewed from outside.
      Tuple3 normal = point4.subtract(point1).crossProduct(point1.subtract(point2)).normalize();

      Face face = new Face(3);
      face.addPoint(point1, texture1, normal);
      face.addPoint(point2, texture2, normal);
      face.addPoint(point3, texture3, normal);
      faces.add(face);

      face = new Face(3);
      face.addPoint(point3, texture3, normal);
      face.addPoint(point4, texture4, normal);
      face.addPoint(point1, texture1, normal);
      faces.add(face);
   }

   public void addTree(float trunkDiameter, float height) {
      int faceCount = 9;
      double anglePerFace = (2.0 * Math.PI) / faceCount;
      Tuple3 point1;
      Tuple3 point2;
      Tuple3 point3;
      Tuple3 point4;
      Tuple2 texture1 = new Tuple2(0.748f, 0.498f);
      Tuple2 texture2 = new Tuple2(0.502f, 0.252f);
      Tuple2 texture3 = new Tuple2(0.502f, 0.498f);
      Tuple2 texture4 = new Tuple2(0.748f, 0.252f);
      Tuple3 center = centerLocation();
      for (int i=0 ; i<faceCount ; i++) {
         double angle = i * anglePerFace;
         float x1 = (float) ((Math.sin(angle) * trunkDiameter) / 2.0);
         float y1 = (float) ((Math.cos(angle) * trunkDiameter) / 2.0);
         float x2 = (float) ((Math.sin(angle - anglePerFace) * trunkDiameter) / 2.0);
         float y2 = (float) ((Math.cos(angle - anglePerFace) * trunkDiameter) / 2.0);
         point1 = center.add(x1, 0, y1);
         point2 = point1.add(0, height, 0);
         point4 = center.add(x2, 0, y2);
         point3 = point4.add(0, height, 0);
         addRectangle(point1, point2, point3, point4,
                      texture1, texture2, texture3, texture4,
                      1/*horizontalTextureRepeatCount*/, 3 /*verticalTextureRepeatCount*/);
      }
   }
   public Tuple3 centerLocation() {
      return boundsVerts.get(0).add(boundsVerts.get(3)).divide(2f);
   }

   public void addBush(float bushDiameter, float height, boolean isDense) {
      int faceCount = 7;
      double anglePerFace = (2.0 * Math.PI) / faceCount;
      Tuple3 point1, point2, point3, point4;
      Tuple2 texture1 = isDense ? new Tuple2(0.502f, 0.748f) : new Tuple2(0.252f, 0.748f);
      Tuple2 texture2 = texture1.add( 0.00f,-0.04f);
      Tuple2 texture3 = texture2.add( 0.08f, 0.00f);
      Tuple2 texture4 = texture3.add(-0.00f, 0.04f);
      Tuple3 center = centerLocation();

      List<Float> radiuses = new ArrayList<>();
      radiuses.add((bushDiameter * .75f) / 2.0f);
      radiuses.add(bushDiameter / 2.0f);
      if (height > 25) {
         radiuses.add((bushDiameter * .85f) / 2.0f);
         radiuses.add((bushDiameter * .65f) / 2.0f);
      }
      else {
         radiuses.add((bushDiameter * .75f) / 2.0f);
      }
      radiuses.add(0f);

      for (int y=0 ; y<(radiuses.size()-1) ; y++) {
         double lowerDiameter = radiuses.get(y);
         double upperDiameter = radiuses.get(y+1);
         for (int i=0 ; i<faceCount ; i++) {
            double angle = i * anglePerFace;
            point1 = center.add((float) (Math.sin(angle) * upperDiameter), (((y+1) * height) / (radiuses.size()-1)), (float) (Math.cos(angle) * upperDiameter));
            point2 = center.add((float) (Math.sin(angle) * lowerDiameter), ((y * height) / (radiuses.size()-1)), (float) (Math.cos(angle) * lowerDiameter));
            point3 = center.add((float) (Math.sin(angle + anglePerFace) * lowerDiameter), ((y * height) / (radiuses.size()-1)), (float) (Math.cos(angle + anglePerFace) * lowerDiameter));
            point4 = center.add((float) (Math.sin(angle + anglePerFace) * upperDiameter), (((y+1) * height) / (radiuses.size()-1)), (float) (Math.cos(angle + anglePerFace) * upperDiameter));

//            texture2 = texture1.add(0.249f, 0f);
//            texture3 = texture2.add(0, -0.249f);
//            texture4 = texture3.add(-0.249f, 0f);

            addRectangle(point1, point2, point3, point4,
                         texture1, texture2, texture3, texture4,
                         1/*horizontalTextureRepeatCount*/, 3 /*verticalTextureRepeatCount*/);
         }
      }
   }

   public void addHuman(HumanBody human, int facing) {
      Tuple3 center = centerLocation();
      human.setLocationOffset(center);
      human.setFacing(facing);
      human.setHeightBasedOnLowestLimbPoint();
      synchronized (lock_humans) {
         lock_humans.check();
         humans.add(human);
      }
   }
   public void addTexturedObject(TexturedObject obj) {
      Tuple3 center = centerLocation();
      for (ObjModel model : obj.models) {
         model.move(center.getX(), center.getY(), center.getZ());
      }

      synchronized (lock_texturedObjects) {
         lock_texturedObjects.check();
         texturedObjects.add(obj);
      }
   }
   public void addObject(ObjData obj) {
      Tuple3 center = centerLocation();
      obj.move(center.getX(), center.getY(), center.getZ());
      synchronized (lock_objects) {
         lock_objects.check();
         objects.add(obj);
      }
   }

   // hexIndex is a 1-based index into the texture map for hexes
   public List<Tuple2> getHexPointForTexture(int hexIndexIn, int textureRotation)
   {
      int hexIndex = hexIndexIn-1;
      int col = hexIndex % 8;
      int row = hexIndex / 8;
      // left most hex of 8,8 = (390, 480)
      int offsetX = (int) (col * 55.5);
      int offsetY = (int) (row * 59.5);
      if ((col %2) == 1) {
         offsetY += 32;
      }
      List<Tuple2> results = new ArrayList<>();
      results.add(new Tuple2((20 + offsetX)/512.0f, (512.0f-(38 + offsetY))/512.0f)); // 10 o'clock position
      results.add(new Tuple2((55 + offsetX)/512.0f, (512.0f-(38 + offsetY))/512.0f)); //  2 o'clock position
      results.add(new Tuple2((71 + offsetX)/512.0f, (512.0f-(69 + offsetY))/512.0f)); //  3 o'clock position
      results.add(new Tuple2((55 + offsetX)/512.0f, (512.0f-(98 + offsetY))/512.0f)); //  4 o'clock position
      results.add(new Tuple2((20 + offsetX)/512.0f, (512.0f-(98 + offsetY))/512.0f)); //  7 o'clock position
      results.add(new Tuple2(( 3 + offsetX)/512.0f, (512.0f-(69 + offsetY))/512.0f)); //  9 o'clock position
      results.add(new Tuple2((37 + offsetX)/512.0f, (512.0f-(69 + offsetY))/512.0f)); //  center position

      for (int i=0 ; i<textureRotation ; i++) {
         results.add(results.remove(0));
      }
      return results;
   }
   public void addFloor(GLView view, int height, int color, float opacity) {
      Texture texture;
      try {
         texture = view.getTextureLoader().getTexture("res/texture_hexdecorations.png");
      } catch (IOException e) {
         e.printStackTrace();
         return;
      }
      TexturedObject floor = new TexturedObject(texture, texture, false/*invertNormals*/);
      floor.opacity = opacity;
      List<Tuple2> texturePoints = getHexPointForTexture(color, 0/*textureRotation*/);
      Tuple3 normal = new Tuple3(0, 0, 1);
      ObjData data = new ObjData();
      for (int i = 0; i < 6; i++) {
         int j = (i + 1) % 6;
         Face face = new Face(3);
         Tuple3 vertex = this.centerLocation().add(0, height, 0);
         face.addPoint(vertex, texturePoints.get(6), normal);
         vertex = verts.get(i).add(0, height, 0);
         face.addPoint(vertex, texturePoints.get(i), normal);
         vertex = verts.get(j).add(0, height, 0);
         face.addPoint(vertex, texturePoints.get(j), normal);
         data.faces.add(face);
      }
      ObjModel model = new ObjModel(data, view);
      floor.addObject(model );
      addTexturedObject(floor);
   }

   public void addSolidRock(float rockHeight) {
      Tuple2 textureCoordStartGround = new Tuple2(0.502f, 0.252f);
      Tuple2 textureCoordStartHigh   = new Tuple2(0.502f, 0.498f);
      Tuple2 textureCoordEndGround   = new Tuple2(0.748f, 0.252f);
      Tuple2 textureCoordEndHigh     = new Tuple2(0.748f, 0.498f);
      for (int i=0 ; i<6 ; i++) {
         Tuple3 startGround = boundsVerts.get(i);
         Tuple3 endGround = boundsVerts.get((i + 1) % 6);
         Tuple3 startHigh = startGround.add(0, rockHeight, 0);
         Tuple3 endHigh = endGround.add(0, rockHeight, 0);
         addRectangle(endGround, endHigh, startHigh, startGround,
                      textureCoordEndGround, textureCoordEndHigh, textureCoordStartHigh, textureCoordStartGround,
                      1/*horizontalTextureRepeatCount*/, 1/*verticalTextureRepeatCount*/);
      }
      Tuple3 center = centerLocation().add(0f, rockHeight, 0f);
      int row = Terrain.ROCK.ordinal() / COLS_IN_TERRAIN;
      int col = Terrain.ROCK.ordinal() % COLS_IN_TERRAIN;
      Tuple2 texCenter = new Tuple2(CENTER_OFFSET_X + (col * COL_WIDTH),
                                    1.0f-(CENTER_OFFSET_Y + (row * ROW_HEIGHT)));
      Tuple3 normal = new Tuple3(0, 0, 1);
      List<Tuple2> texCoords = new ArrayList<>();
      Random rnd = new Random(uniqueNumericKey); rnd.nextFloat();
      double textureAngle = rnd.nextFloat() * Math.PI * 2.0;
      for (int i = 0; i < 6; i++) {
         textureAngle += PI_3rd;
         Tuple2 textureCoord = texCenter.add((float) Math.sin(textureAngle) * EDGE_OFFSET_FROM_CENTER_Y,
                                             (float) Math.cos(textureAngle) * EDGE_OFFSET_FROM_CENTER_X);
         texCoords.add(textureCoord);
      }
      for (int i = 0; i < 6; i++) {
         Face face = new Face(3);
         face.addPoint(center, texCenter, normal);
         face.addPoint(boundsVerts.get(i).add(0f, rockHeight, 0f), texCoords.get(i), normal);
         int j = (i + 1) % 6;
         face.addPoint(boundsVerts.get(j).add(0f, rockHeight, 0f), texCoords.get(j), normal);
         faces.add(face);
      }
   }

   /**
    * Create a new set of OBJ data by reading it in from the specified
    * input stream.
    *
    * @param in The input stream from which to read the OBJ data
    * @throws IOException Indicates a failure to read from the stream
    */
   public ObjHex(InputStream in) throws IOException {
      super(in);
   }
   public Message getMessage() {
      return message;
   }
   public void setMessage(Message message) {
      this.message = message;
   }
}

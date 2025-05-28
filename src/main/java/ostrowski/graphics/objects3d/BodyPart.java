package ostrowski.graphics.objects3d;


import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import ostrowski.graphics.GLView;
import ostrowski.graphics.model.ObjLoader;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.Pelvis.Foot;
import ostrowski.graphics.objects3d.Pelvis.Leg;
import ostrowski.graphics.objects3d.Pelvis.LowerLeg;
import ostrowski.graphics.objects3d.Pelvis.Toes;
import ostrowski.graphics.objects3d.Torso.Arm;
import ostrowski.graphics.objects3d.Torso.Fingers;
import ostrowski.graphics.objects3d.Torso.ForeArm;
import ostrowski.graphics.objects3d.Torso.Hand;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;

//import org.lwjgl.opengl.glu.Cylinder;
//import org.lwjgl.opengl.glu.Sphere;
public abstract class BodyPart extends TexturedObject {
   static public HashMap<String, HashMap<Class<? extends BodyPart>, HashMap<String, Object>>>
                         valuesByNameByClassByRace = new HashMap<>();
   public        String  name;
   protected     String  raceName;
   protected     boolean isMale;

   public    GLView glView;
   protected float  lengthFactor;
   protected float  widthFactor;

   Semaphore lock_children = null;
   protected List<TexturedObject> children;

   static {
      HashMap<Class <? extends BodyPart>, HashMap<String, Object>> valuesByNameByClass;
      {
         valuesByNameByClass = new HashMap<>();
         valuesByNameByClassByRace.put("human", valuesByNameByClass);

         HashMap<String, Object> raceValue = new HashMap<>();
         raceValue.put("tiltForward", (float) -120);
         valuesByNameByClass.put(HumanBody.class, raceValue );
         valuesByNameByClass.put(Head.class, getHeadValues(270f/*neckTiltForward*/, 330/*maxNeckTiltForward*/, 225/*minNeckTiltForward*/,
                                                           0f/*neckTiltToLeft*/,45/*maxNeckTiltToLeft*/, -45/*minNeckTiltToLeft*/,
                                                           0f/*neckTwist*/, 90/*maxNeckTwist*/, -90/*minNeckTwist*/,
                                                           6f/*rHead*/, 4f/*neckLength*/, 1.75f/*neckRadius*/, 2f/*neckHeight*/));
         valuesByNameByClass.put(Torso.class, getTorsoValues(0f/*spineTwistToLeft*/, -45/*minSpineTwistToLeft*/, 45/*maxSpineTwistToLeft*/,
                                                             0f/*spineSide*/, -30/*minSpineSide*/, 30/*maxSpineSide*/,
                                                             0f/*twistRightShoulderUp*/, -30/*minTwistRightShoulderUp*/, 30/*maxTwistRightShoulderUp*/,
                                                             0f/*spineBend*/, -30/*minSpineBend*/, 30/*maxSpineBend*/,
                                                             16f/*upperSpineLength*/, 15f/*shoulderWidth*/, 3.5f/*neckOffset*/));
         valuesByNameByClass.put(Pelvis.class, getPelvis(0/*hipTwistLeftUp*/, -30/*minHipTwistLeftUp*/, 30/*maxHipTwistLeftUp*/,
                                                         0/*spinalTwistCC"*/, -45/*minSpinalTwistCC*/, 45/*maxSpinalTwistCC*/,
                                                         8/*lowerSpineLength*/, 8.5f/*pelvisWidth*/, 0/*pelvisDepth*/));

         valuesByNameByClass.put(Arm.class, setValues(13/*length*/,   225,360,90,   0,110,-40,   0,90,-90));
         valuesByNameByClass.put(ForeArm.class, setValues(12,   0,90,-60,   0,0,0,   90,180,0));
         // 0 is arm at right angle to bicept. 90 is arms straight, -60 is hands to shoulder
         // elbow twist (applies to wrist), 180 is palms downward, 0 is twisting outward (palms up)

         valuesByNameByClass.put(Hand.class, setValues(4,   90,165,45,   0,45,-90,   0,0,0));
         // front: 90 is straight, knuckles inline with forearm, 165 is knuckles back to forearm, 45 is hand curling inward
         // side:  0 is straight, knuckles inline with forearm, 45 is thumb pointing at face, -90 is thumb out to front
         // twist: twist occurs at the elbow, not the wrist. This is only here because there is no joint type with front & side only.
         valuesByNameByClass.put(Fingers.class, setValues(4,   -20,120,-20,   0,0,0,   0,0,0));
         // 90 is straight. -20 is hand curling inward, 120 is knuckles back to forearm

         valuesByNameByClass.put(Leg.class, setValues(19,   90,160,-45,   0,60,-10,   0,45,-45));
         valuesByNameByClass.put(LowerLeg.class, setValues(17,   130,240,90,   0,0,0,   0,35,-35));
         // 90 is standing straight legged. 180 is sitting, 240 is heels to butt
         // knee twist -35 is toes apart, +35 is pigeon toed
         valuesByNameByClass.put(Foot.class, setValues(7,   8,70,-20,   0,0,0,   0,0,0));
         // 8 is standing. 70 is toes pointing, -20 is toes toward knees
         valuesByNameByClass.put(Toes.class, setValues(2,   90,235,-270,   0,0,0,   0,0,0));
         // 90 is standing. 135 is toes curled, 30 is toes toward knees
      }


      {
         valuesByNameByClass = new HashMap<>();
         valuesByNameByClassByRace.put("wolf", valuesByNameByClass);
         HashMap<String, Object> raceValue = new HashMap<>();
         raceValue.put("tiltForward", (float) 0);
         valuesByNameByClass.put(HumanBody.class, raceValue );
         valuesByNameByClass.put(Head.class, getHeadValues(270f/*neckTiltForward*/, 330/*maxNeckTiltForward*/, 225/*minNeckTiltForward*/,
                                                           0f/*neckTiltToLeft*/,45/*maxNeckTiltToLeft*/, -45/*minNeckTiltToLeft*/,
                                                           0f/*neckTwist*/, 90/*maxNeckTwist*/, -90/*minNeckTwist*/,
                                                           4f/*rHead*/, 8f/*neckLength*/, 2f/*neckRadius*/, 2f/*neckHeight*/));
         valuesByNameByClass.put(Torso.class, getTorsoValues(0f/*spineTwistToLeft*/, -45/*minSpineTwistToLeft*/, 45/*maxSpineTwistToLeft*/,
                                                             0f/*spineSide*/, -30/*minSpineSide*/, 30/*maxSpineSide*/,
                                                             0f/*twistRightShoulderUp*/, -30/*minTwistRightShoulderUp*/, 30/*maxTwistRightShoulderUp*/,
                                                             0f/*spineBend*/, -30/*minSpineBend*/, 30/*maxSpineBend*/,
                                                             13f/*upperSpineLength*/, 14f/*shoulderWidth*/, 9f/*neckOffset*/));
         valuesByNameByClass.put(Pelvis.class, getPelvis(0/*hipTwistLeftUp*/, -30/*minHipTwistLeftUp*/, 30/*maxHipTwistLeftUp*/,
                                                         0/*spinalTwistCC"*/, -45/*minSpinalTwistCC*/, 45/*maxSpinalTwistCC*/,
                                                         12/*lowerSpineLength*/,9f/*pelvisWidth*/, -1/*pelvisDepth*/));

         valuesByNameByClass.put(Arm.class, setValues(15/*length*/,   180,270,90,   0,30,-30,   0,20,-20));
         valuesByNameByClass.put(ForeArm.class, setValues(8/*length*/,   40,90,-60,   0,0,0,   180,160,200));
         // 0 is arm at right angle to bicept. 90 is arms straight, -60 is hands to shoulder
         // elbow twist (applies to wrist), 180 is palms downward, 0 is twisting outward (palms up)

         valuesByNameByClass.put(Hand.class, setValues(6,   90,110,45,   0,45,-90,   0,0,0));
         // front: 90 is straight, knuckles inline with forearm, 165 is knuckles back to forearm, 45 is hand curling inward
         // side:  0 is straight, knuckles inline with forearm, 45 is thumb pointing at face, -90 is thumb out to front
         // twist: twist occurs at the elbow, not the wrist. This is only here because there is no joint type with front & side only.
         valuesByNameByClass.put(Fingers.class, setValues(4,   90,120,45,   0,0,0,   0,0,0));
         // 90 is straight. -20 is hand curling inward, 120 is knuckles back to forearm

         valuesByNameByClass.put(Leg.class, setValues(13,   320,400,270,   0,60,-10,   0,45,-45));
         valuesByNameByClass.put(LowerLeg.class, setValues(11,   190,240,90,   0,0,0,   0,35,-35));
         // 90 is standing straight legged. 180 is sitting, 240 is heels to butt
         // knee twist -35 is toes apart, +35 is pigeon toed
         valuesByNameByClass.put(Foot.class, setValues(10/*length*/,   8,70,-90,   0,0,0,   0,0,0));
         // 8 is standing. 70 is toes pointing, -20 is toes toward knees
         valuesByNameByClass.put(Toes.class, setValues(5,   70,180,60,   0,0,0,   0,0,0));
         // 90 is standing. 135 is toes curled, 30 is toes toward knees
      }
   }

   protected HashMap<String, Object> getValuesByNameForClass() {
      HashMap<Class <? extends BodyPart>, HashMap<String, Object>> valuesByClass = valuesByNameByClassByRace.get(raceName);
      if (valuesByClass == null) {
         valuesByClass = valuesByNameByClassByRace.get("human");
      }
      if (valuesByClass == null) {
         return null;
      }
      return valuesByClass.get(this.getClass());
   }
   private static HashMap<String, Object> getHeadValues(float neckTiltForward, float maxNeckTiltForward, float minNeckTiltForward,
                                                       float neckTiltToLeft, float maxNeckTiltToLeft, float minNeckTiltToLeft,
                                                       float neckTwist, float maxNeckTwist, float minNeckTwist,
                                                       float rHead, float neckLength, float neckRadius, float neckHeight)
   {
      HashMap<String, Object> valuesByName = new HashMap<>();
      valuesByName.put("neckTiltForward", neckTiltForward);
      valuesByName.put("maxNeckTiltForward", maxNeckTiltForward);
      valuesByName.put("minNeckTiltForward", minNeckTiltForward);
      valuesByName.put("neckTiltToLeft", neckTiltToLeft);
      valuesByName.put("maxNeckTiltToLeft", maxNeckTiltToLeft);
      valuesByName.put("minNeckTiltToLeft", minNeckTiltToLeft);
      valuesByName.put("neckTwist", neckTwist);
      valuesByName.put("maxNeckTwist", maxNeckTwist);
      valuesByName.put("minNeckTwist", minNeckTwist);
      valuesByName.put("rHead", rHead);
      valuesByName.put("neckLength", neckLength);
      valuesByName.put("neckRadius", neckRadius);
      valuesByName.put("neckHeight", neckHeight);
      return valuesByName;
   }


   private static HashMap<String, Object> getPelvis(float hipTwistLeftUp, float minHipTwistLeftUp, float maxHipTwistLeftUp,
                                                   float spinalTwistCC, float minSpinalTwistCC, float maxSpinalTwistCC,
                                                   float lowerSpineLength, float pelvisWidth, float pelvisDepth)
   {
      HashMap<String, Object> valuesByName = new HashMap<>();
      valuesByName.put("hipTwistLeftUp", new RangedValue(hipTwistLeftUp, minHipTwistLeftUp, maxHipTwistLeftUp));
      valuesByName.put("spinalTwistCC", new RangedValue(spinalTwistCC, minSpinalTwistCC, maxSpinalTwistCC));
      valuesByName.put("legOffset", new Tuple3(pelvisWidth/2, pelvisDepth, lowerSpineLength));
      return valuesByName;
   }

   private static HashMap<String, Object> getTorsoValues(float spineTwistToLeft, float minTwistRightShoulderUp, float maxTwistRightShoulderUp,
                                                        float spineSide, float minSpineSide, float maxSpineSide,
                                                        float twistRightShoulderUp, float minSpineTwistToLeft, float maxSpineTwistToLeft,
                                                        float spineBend, float minSpineBend, float maxSpineBend,
                                                        float upperSpineLength, float shoulderWidth, float neckOffset) {
      HashMap<String, Object> valuesByName = new HashMap<>();
      valuesByName.put("spineTwistToLeft", new RangedValue(spineTwistToLeft, minSpineTwistToLeft, maxSpineTwistToLeft));
      valuesByName.put("spineSide", new RangedValue(spineSide, minSpineSide, maxSpineSide));
      valuesByName.put("twistRightShoulderUp", new RangedValue(twistRightShoulderUp, minTwistRightShoulderUp, maxTwistRightShoulderUp));
      valuesByName.put("spineBend", new RangedValue(spineBend, minSpineBend, maxSpineBend));
      valuesByName.put("upperSpineLength", upperSpineLength);
      valuesByName.put("shoulderWidth", shoulderWidth);
      valuesByName.put("neckOffset", neckOffset);
      return valuesByName;
   }
   private static HashMap<String, Object> setValues(int length, int frontRot, int maxFrontRot, int minFrontRot, int sideRot, int maxSideRot, int minSideRot, int twistCW, int maxTwistCW, int minTwistCW) {
      HashMap<String, Object> valuesByName = new HashMap<>();
      valuesByName.put("_length", (float) length);
      valuesByName.put("frontRot", new RangedValue(frontRot, minFrontRot, maxFrontRot));
      valuesByName.put("sideRot", new RangedValue(sideRot, minSideRot, maxSideRot));
      valuesByName.put("twistCW", new RangedValue(twistCW, minTwistCW, maxTwistCW));
      return valuesByName;
   }
   protected float getValueByNameAsFloat(String name) {
      HashMap<String, Object> values = getValuesByNameForClass();
      Object value = values.get(name);
      if ((value == null) || !(value instanceof Float)) {
         return 0;
      }
      return (Float) value;
   }
   protected Tuple3 getValueByNameAsTuple3(String name) {
      HashMap<String, Object> values = getValuesByNameForClass();
      Object value = values.get(name);
      if ((value == null) || !(value instanceof Tuple3)) {
         return new Tuple3(0,0,0);
      }
      return (Tuple3) value;
   }
   protected RangedValue getValueByNameAsRangedValue(String name) {
      HashMap<String, Object> values = getValuesByNameForClass();
      Object value = values.get(name);
      if ((value == null) || !(value instanceof RangedValue)) {
         return new RangedValue(0,0,0);
      }
      return ((RangedValue) value).clone();
   }

   protected abstract Semaphore getChildLock();
   public BodyPart(String name, String modelResourceName, boolean invertNormals,
                   Texture texture, Texture selectedTexture,
                   GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super(texture, selectedTexture, invertNormals);

      lock_children = getChildLock();

      this.glView = glView;
      this.name = name;
      this.lengthFactor = lengthFactor;
      this.widthFactor = widthFactor;
      this.raceName = raceName;
      this.isMale = isMale;

      children = new ArrayList<>();

      if ((modelResourceName != null) && (modelResourceName.length() > 0)) {
         try {
            String raceAdjustedResouceName = modelResourceName;
            if (raceName != null) {
               raceAdjustedResouceName = modelResourceName.substring(0, modelResourceName.length() - 4); // remove the ".obj"
               raceAdjustedResouceName += "_" + raceName.toLowerCase() + ".obj";
               // see if we have a resource we could use. If not, use the original modelResourceName
               try (InputStream in = ObjLoader.class.getClassLoader().getResourceAsStream(raceAdjustedResouceName))
               {
                  if (in == null) {
                     raceAdjustedResouceName = modelResourceName;
                  }
               }
            }
            ObjModel model = ObjLoader.loadObj(raceAdjustedResouceName, glView, lengthFactor, widthFactor);
            if (model != null) {
               //if (invertNormals) {
               //   if (!GLScene.USING_SCALING_FOR_LEFT_SIDE)
               //      model.reverseRightToLeft();
               //}
               addObject(model);

            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   public void addChild(TexturedObject child) {
      synchronized(children) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
            children.add(child);
         }
      }
   }


   @Override
   public String getName() {
      return name;
   }

   public abstract void validateRanges();

   public abstract void setAnglesFromMap(HashMap<String, Float> angleMap);

   public abstract HashMap<String, Float> getAnglesMap();

   public abstract void getParts(List<TexturedObject> parts);

   protected static void drawCylinder(float baseRadius, float topRadius, float height, int slices, int stacks) {
     // Custom Cylinder implementation (assumes topRadius == baseRadius)
     Cylinder cylinder = new Cylinder(baseRadius, height, slices, stacks);
     cylinder.draw();
   }

   protected static void drawSphere(float radius, int slices, int stacks) {
     Sphere sphere = new Sphere(radius, slices, stacks);
     sphere.draw();
   }

    public static FloatBuffer projectToWindowLocation() {
       float objX = 0;
       float objY = 0;
       float objZ = 0;

       try (MemoryStack stack = MemoryStack.stackPush()) {
           FloatBuffer modelBuffer = stack.mallocFloat(16);
           FloatBuffer projBuffer = stack.mallocFloat(16);
           IntBuffer viewport = stack.mallocInt(4);

           GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
           GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projBuffer);
           GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

           Matrix4f model = new Matrix4f();
           Matrix4f proj = new Matrix4f();

           model.set(modelBuffer);
           proj.set(projBuffer);

           // Model + Projection transformation
           Matrix4f mvp = new Matrix4f();
           proj.mul(model, mvp);

           // Transform the origin
           Vector4f in = new Vector4f(objX, objY, objZ, 1.0f);
           Vector4f out = new Vector4f();
           mvp.transform(in, out);

           // Perspective division
           if (out.w != 0.0f) {
               out.x /= out.w;
               out.y /= out.w;
               out.z /= out.w;
           }

           // Map to window coordinates
           float winX = viewport.get(0) + (1 + out.x) * viewport.get(2) / 2;
           float winY = viewport.get(1) + (1 + out.y) * viewport.get(3) / 2;
           float winZ = (1 + out.z) / 2;

           FloatBuffer winPos = stack.mallocFloat(3);
           winPos.put(0, winX);
           winPos.put(1, winY);
           winPos.put(2, winZ);
           return winPos;
       }
   }


    public static FloatBuffer unProjectFromWindowLocation(FloatBuffer windowLocation) {
      float winX = windowLocation.get(0);
      float winY = windowLocation.get(1);
      float winZ = windowLocation.get(2);

      try (MemoryStack stack = MemoryStack.stackPush()) {
          FloatBuffer modelBuffer = stack.mallocFloat(16);
          FloatBuffer projBuffer = stack.mallocFloat(16);
          IntBuffer viewport = stack.mallocInt(4);

          GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
          GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projBuffer);
          GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

          Matrix4f model = new Matrix4f().set(modelBuffer);
          Matrix4f proj = new Matrix4f().set(projBuffer);
          Matrix4f inverse = new Matrix4f();

          proj.mul(model, inverse).invert(); // Invert the combined matrix

          // Normalize window coordinates to [-1, 1]
          float ndcX = (winX - viewport.get(0)) / viewport.get(2) * 2.0f - 1.0f;
          float ndcY = (winY - viewport.get(1)) / viewport.get(3) * 2.0f - 1.0f;
          float ndcZ = 2.0f * winZ - 1.0f;

          Vector4f screenPos = new Vector4f(ndcX, ndcY, ndcZ, 1.0f);
          Vector4f objPos = new Vector4f();
          inverse.transform(screenPos, objPos);

          if (objPos.w != 0.0f) {
              objPos.x /= objPos.w;
              objPos.y /= objPos.w;
              objPos.z /= objPos.w;
          }

          FloatBuffer result = stack.mallocFloat(3);
          result.put(0, objPos.x);
          result.put(1, objPos.y);
          result.put(2, objPos.z);
          return result;
      }
  }}
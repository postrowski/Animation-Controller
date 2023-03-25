package ostrowski.graphics.objects3d;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import ostrowski.graphics.GLView;
import ostrowski.graphics.model.Message;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.Thing.Armor;
import ostrowski.graphics.objects3d.Thing.Shield;
import ostrowski.graphics.objects3d.Thing.Weapon;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;

public class Torso extends BodyPart
{
   RangedValue spineTwistToLeft;    // + = left hip higher than right
   RangedValue spineSide;
   RangedValue twistRightShoulderUp; // one hip more forward than the other
   RangedValue spineBendToFront;
   float       upperSpineLength;
   float       shoulderWidth;
   float       neckOffset;

   Arm  leftArm;
   Arm  rightArm;
   Head head;
   private Thing armor;

   public Torso(Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super("Torso", "res/bodyparts/torso.obj", false/*invertNormals*/, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      leftArm = new Arm(true, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      rightArm = new Arm(false, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      head = new Head(texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);

      spineTwistToLeft = getValueByNameAsRangedValue("spineTwistToLeft");
      spineSide = getValueByNameAsRangedValue("spineSide");
      twistRightShoulderUp = getValueByNameAsRangedValue("twistRightShoulderUp");
      spineBendToFront = getValueByNameAsRangedValue("spineBend");
      upperSpineLength = getValueByNameAsFloat("upperSpineLength");
      shoulderWidth = getValueByNameAsFloat("shoulderWidth");
      neckOffset = getValueByNameAsFloat("neckOffset");

      shoulderWidth *= widthFactor;
      upperSpineLength *= lengthFactor;
      neckOffset *= lengthFactor;
   }


   @Override
   public void render(GLView glView, List<Message> messages) {
      bindToTexture();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glEnable(GL11.GL_NORMALIZE); // see http://www.opengl.org/resources/features/KilgardTechniques/oglpitfall/
      GL11.glPushMatrix();              // save matrix before modeling.
      {
         //GL11.glTranslatef(0.0f, 10.0f, 0.0f);    // base matrix is (0, 10, 0); std rh axis (-z depth)

         // from midpoint on spine
         GL11.glRotatef(spineBendToFront.getValue(), 1f, 0f, 0f);
         GL11.glRotatef(spineSide.getValue(), 0f, 1f, 0f);
         GL11.glRotatef(spineTwistToLeft.getValue(), 0f, 0f, 1.0f);
         for (ObjModel model : models) {
            model.render(glView, messages);
         }
         if (armor != null) {
            armor.render(glView, messages);
         }
         GL11.glTranslatef(0f, 0f, upperSpineLength);
         GL11.glRotatef(twistRightShoulderUp.getValue(), 0f, 1.0f, 0f);
         GL11.glTranslatef(-shoulderWidth / 2, 0f, 0f);
         //GL11.glTranslatef(0f, shoulderOffset, 0f);
         if (rightArm != null) {
            rightArm.render(glView, messages);
         }
         GL11.glTranslatef(shoulderWidth, 0f, 0f);


         GL11.glPushMatrix();
         {
            GL11.glTranslatef(-shoulderWidth / 2, 0f, neckOffset);
            GL11.glRotatef(90, 1f, 0f, 0f);

            // the neck starts higher than the line joining the shoulders
            head.render(glView, messages);
         }

         GL11.glPopMatrix();
         //if (GLScene.USING_SCALING_FOR_LEFT_SIDE)
            GL11.glScalef(-1f, 1f, 1f);
         glView.defineLight(-1f);
         if (leftArm != null) {
            leftArm.render(glView, messages);
         }
         //if (GLScene.USING_SCALING_FOR_LEFT_SIDE)
            GL11.glScalef(-1f, 1f, 1f);

            //glView.defineLight(1f);
      }
      GL11.glPopMatrix();
   }

   @Override
   public void validateRanges() {
      twistRightShoulderUp.validateRange();
      spineBendToFront.validateRange();
      spineSide.validateRange();
      spineTwistToLeft.validateRange();

      if (rightArm != null) {
         rightArm.validateRanges();
      }
      if (leftArm != null) {
         leftArm.validateRanges();
      }
      head.validateRanges();
   }


   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("f")) {
         spineBendToFront.setValue(angleMap.get("f"));
      }
      if (angleMap.containsKey("h")) {
         twistRightShoulderUp.setValue(angleMap.get("h"));
      }
      if (angleMap.containsKey("s")) {
         spineSide.setValue(angleMap.get("s"));
      }
      if (angleMap.containsKey("t")) {
         spineTwistToLeft.setValue(angleMap.get("t"));
      }
      validateRanges();
   }
   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", spineBendToFront.getValue());
      anglesMap.put("h", twistRightShoulderUp.getValue());
      anglesMap.put("s", spineSide.getValue());
      anglesMap.put("t", spineTwistToLeft.getValue());
      return anglesMap;
   }

   @Override
   public void getParts(List<TexturedObject> parts) {
      parts.add(this);
      if (leftArm != null) {
         leftArm.getParts(parts);
      }
      if (rightArm != null) {
         rightArm.getParts(parts);
      }
      head.getParts(parts);
   }

   public Tuple3 getChildLocationIn3DReferenceFrame(boolean rightArm, int depth) {
      Arm arm = rightArm ? this.rightArm : leftArm;
      if (arm == null) {
         return new Tuple3(0, 0, 0);
      }
      Tuple3 computedLoc3;
      if (depth == 0) {
         computedLoc3 = new Tuple3(0, 0, 0);
      }
      else {
         computedLoc3 = arm.getEndPoint(depth - 1);
      }
      computedLoc3 = computedLoc3.add(new Tuple3(-shoulderWidth / 2, 0, 0));
      if (rightArm) {
         computedLoc3 = computedLoc3.rotate(0, twistRightShoulderUp.getValue(), 0);
      }
      else {
         computedLoc3 = computedLoc3.rotate(0, -twistRightShoulderUp.getValue(), 0);
      }
      computedLoc3 = computedLoc3.add(0f, 0f, upperSpineLength);
      if (rightArm) {
         computedLoc3 = computedLoc3.rotate(spineBendToFront.getValue(), spineSide.getValue(), spineTwistToLeft.getValue());
      }
      else {
         computedLoc3 = computedLoc3.rotate(spineBendToFront.getValue(), -spineSide.getValue(), -spineTwistToLeft.getValue());
         computedLoc3.scale(-1f, 1f, 1f);
      }
      return computedLoc3;
   }

   public FloatBuffer getHeadLocationInWindowReferenceFrame() {
      FloatBuffer headLoc2d;
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glEnable(GL11.GL_NORMALIZE); // see http://www.opengl.org/resources/features/KilgardTechniques/oglpitfall/
      GL11.glPushMatrix();                     // save matrix before modelling.
      {
         //GL11.glTranslatef(0.0f, 10.0f, 0.0f);    // base matrix is (0, 10, 0); std rh axis (-z depth)

         // from midpoint on spine
         GL11.glRotatef(spineBendToFront.getValue(), 1f, 0f, 0f);
         GL11.glRotatef(spineSide.getValue(), 0f, 1f, 0f);
         GL11.glRotatef(spineTwistToLeft.getValue(), 0f, 0f, 1.0f);
         GL11.glTranslatef(0f, 0f, upperSpineLength);
         GL11.glRotatef(twistRightShoulderUp.getValue(), 0f, 1.0f, 0f);

         // the neck starts higher than the line joining the shoulders
         headLoc2d = head.getEndpointLocationInWindowReferenceFrame();
         GL11.glPopMatrix();
      }
      return headLoc2d;
   }




   public class Fingers extends BendJoint {
      public Fingers(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
         super( (leftSide ? "LeftFingers" : "RightFingers"), "res/bodyparts/fingers.obj", leftSide,
                texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      }

      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_Fingers_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_FINGERS);
      }
   }

   public class Hand extends BallSocketJoint {
      public Hand(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView,
                  float lengthFactor, float widthFactor, String raceName, boolean isMale) {
         super( (leftSide ? "LeftHand" : "RightHand"), "res/bodyparts/hand.obj", leftSide,
                texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
         addChild(new Fingers(leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale));
         if (!leftSide) {
            addChild(new WeaponPart("rightWeapon", null, false/*invertNormals*/, null/*texture*/, null/*selectedTexture*/, glView, lengthFactor, widthFactor));
         }
      }

      public void setHeldThing(Weapon weapon) {
         removeHeldThings();
         if (weapon == null) {
            return;
         }
         try {
            WeaponPart weaponPart = null;
            synchronized (children) {
               try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
                  for (TexturedObject child : children) {
                     if (child instanceof WeaponPart) {
                        weaponPart = (WeaponPart) child;
                     }
                  }
                  Thing thing = new Thing(weapon, weaponPart, this.glView, invertNormals, lengthFactor, lengthFactor);
                  addChild(thing);
               }
            }
         }
         catch (IOException e) {
            // this happens when we can't find the object for the Thing in the resource directories
         }
      }

      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_Hand_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_HANDS);
      }
   }
   public class ForeArm extends TwistBendJoint {
      public ForeArm(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
         super( (leftSide ? "LeftForeArm" : "RightForeArm"), "res/bodyparts/forearm.obj", leftSide,
                texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
         addChild(new Hand(leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale));
      }
      public void setHeldThing(Thing.Shield shield) {
         removeHeldThings();
         if (shield == null) {
            return;
         }
         try {
            Thing thing = new Thing(shield, this.glView, invertNormals, lengthFactor, lengthFactor);
            thing.facingOffset = new Tuple3(90, 0, -90);
            addChild(thing);
         }
         catch (IOException e) {
            // this happens when we can't find the object for the Thing in the resource directories
         }
      }
      public void setHeldThing(Weapon weapon) {
         synchronized (children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
               for (TexturedObject child : this.children) {
                  if (child instanceof Hand) {
                     ((Hand) child).setHeldThing(weapon);
                     return;
                  }
               }
            }
         }
      }
      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_Forearm_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_FOREARM);
      }
   }


   public class Arm extends BallSocketJoint
   {
      public Arm(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
         super( (leftSide ? "LeftArm" : "RightArm"), "res/bodyparts/arm.obj", leftSide,
               texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
         addChild(new ForeArm(leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale));
      }

      public void setHeldThing(Shield shield) {
         synchronized (children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
               for (TexturedObject child : this.children) {
                  if (child instanceof ForeArm) {
                     ((ForeArm) child).setHeldThing(shield);
                     return;
                  }
               }
            }
         }
      }
      public void setHeldThing(Weapon weapon) {
         synchronized (children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
               for (TexturedObject child : this.children) {
                  if (child instanceof ForeArm) {
                     ((ForeArm) child).setHeldThing(weapon);
                     return;
                  }
               }
            }
         }
      }

      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_Arm_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_ARMS);
      }
   }


   public void setHeldThing(boolean rightHand, Shield shield) {
      if (rightHand) {
         if (rightArm != null) {
            rightArm.setHeldThing(shield);
         }
      }
      else {
         if (leftArm != null) {
            leftArm.setHeldThing(shield);
         }
      }
   }
   public void setHeldThing(boolean rightHand, Weapon weapon) {
      if (rightHand) {
         if (rightArm != null) {
            rightArm.setHeldThing(weapon);
         }
      }
      else {
         if (leftArm != null) {
            leftArm.setHeldThing(weapon);
         }
      }
   }

   public void setArmor(Armor armor) {
      try {
         this.armor = new Thing(armor, this.getClass().getSimpleName(), glView, invertNormals, lengthFactor, widthFactor);
      }
      catch (IOException e) {
         // this happens when we can't find the object for the Thing in the resource directories
         this.armor = null;
      }
      if (leftArm != null) {
         leftArm.setArmor(armor);
      }
      if (rightArm != null) {
         rightArm.setArmor(armor);
      }
      head.setArmor(armor);
   }

   public void removeArm(boolean rightArm) {
      if (rightArm) {
         this.rightArm = null;
      }
      else {
         leftArm = null;
      }
   }

   public TexturedObject getModelHand(boolean rightSide) {
      return new Arm(!rightSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
   }

   public TexturedObject getModelHead() {
      return new Head(texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
   }

   public TexturedObject getModelWing(boolean rightSide) {
      return null;
   }
   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Torso_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_TORSO);
   }
}

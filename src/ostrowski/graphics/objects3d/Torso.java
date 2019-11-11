package ostrowski.graphics.objects3d;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
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
   RangedValue   _spineTwistToLeft;    // + = left hip higher than right
   RangedValue   _spineSide;
   RangedValue   _twistRightShoulderUp; // one hip more forward than the other
   RangedValue   _spineBendToFront;
   float         _upperSpineLength;
   float         _shoulderWidth;
   float         _neckOffset;

   Arm           _leftArm;
   Arm           _rightArm;
   Head          _head;
   private Thing _armor;

   public Torso(Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super("Torso", "res/bodyparts/torso.obj", false/*invertNormals*/, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      _leftArm = new Arm(true, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      _rightArm = new Arm(false, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      _head = new Head(texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);

      _spineTwistToLeft       = getValueByNameAsRangedValue("spineTwistToLeft");
      _spineSide              = getValueByNameAsRangedValue("spineSide");
      _twistRightShoulderUp   = getValueByNameAsRangedValue("twistRightShoulderUp");
      _spineBendToFront       = getValueByNameAsRangedValue("spineBend");
      _upperSpineLength       = getValueByNameAsFloat("upperSpineLength");
      _shoulderWidth          = getValueByNameAsFloat("shoulderWidth");
      _neckOffset             = getValueByNameAsFloat("neckOffset");

      _shoulderWidth    *= widthFactor;
      _upperSpineLength *= lengthFactor;
      _neckOffset       *= lengthFactor;
   }


   @Override
   public void render(GLView glView, ArrayList<Message> messages) {
      bindToTexture();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glEnable(GL11.GL_NORMALIZE); // see http://www.opengl.org/resources/features/KilgardTechniques/oglpitfall/
      GL11.glPushMatrix();              // save matrix before modeling.
      {
         //GL11.glTranslatef(0.0f, 10.0f, 0.0f);    // base matrix is (0, 10, 0); std rh axis (-z depth)

         // from midpoint on spine
         GL11.glRotatef(_spineBendToFront.getValue(), 1f, 0f, 0f);
         GL11.glRotatef(_spineSide.getValue(), 0f, 1f, 0f);
         GL11.glRotatef(_spineTwistToLeft.getValue(), 0f, 0f, 1.0f);
         for (ObjModel model : _models) {
            model.render(glView, messages);
         }
         if (_armor != null) {
            _armor.render(glView, messages);
         }
         GL11.glTranslatef(0f, 0f, _upperSpineLength);
         GL11.glRotatef(_twistRightShoulderUp.getValue(), 0f, 1.0f, 0f);
         GL11.glTranslatef(-_shoulderWidth / 2, 0f, 0f);
         //GL11.glTranslatef(0f, _shoulderOffset, 0f);
         if (_rightArm != null) {
            _rightArm.render(glView, messages);
         }
         GL11.glTranslatef(_shoulderWidth, 0f, 0f);


         GL11.glPushMatrix();
         {
            GL11.glTranslatef(-_shoulderWidth/2, 0f, _neckOffset);
            GL11.glRotatef(90, 1f, 0f, 0f);

            // the neck starts higher than the line joining the shoulders
            _head.render(glView, messages);
         }

         GL11.glPopMatrix();
         //if (GLScene.USING_SCALING_FOR_LEFT_SIDE)
            GL11.glScalef(-1f, 1f, 1f);
         glView.defineLight(-1f);
         if (_leftArm != null) {
            _leftArm.render(glView, messages);
         }
         //if (GLScene.USING_SCALING_FOR_LEFT_SIDE)
            GL11.glScalef(-1f, 1f, 1f);

            //glView.defineLight(1f);
      }
      GL11.glPopMatrix();
   }

   @Override
   public void validateRanges() {
      _twistRightShoulderUp.validateRange();
      _spineBendToFront.validateRange();
      _spineSide.validateRange();
      _spineTwistToLeft.validateRange();

      if (_rightArm != null) {
         _rightArm.validateRanges();
      }
      if (_leftArm != null) {
         _leftArm.validateRanges();
      }
      _head.validateRanges();
   }


   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("f")) {
         _spineBendToFront.setValue(angleMap.get("f"));
      }
      if (angleMap.containsKey("h")) {
         _twistRightShoulderUp.setValue(angleMap.get("h"));
      }
      if (angleMap.containsKey("s")) {
         _spineSide.setValue(angleMap.get("s"));
      }
      if (angleMap.containsKey("t")) {
         _spineTwistToLeft.setValue(angleMap.get("t"));
      }
      validateRanges();
   }
   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", _spineBendToFront.getValue());
      anglesMap.put("h", _twistRightShoulderUp.getValue());
      anglesMap.put("s", _spineSide.getValue());
      anglesMap.put("t", _spineTwistToLeft.getValue());
      return anglesMap;
   }

   @Override
   public void getParts(ArrayList<TexturedObject> parts) {
      parts.add(this);
      if (_leftArm != null) {
         _leftArm.getParts(parts);
      }
      if (_rightArm != null) {
         _rightArm.getParts(parts);
      }
      _head.getParts(parts);
   }

   public Tuple3 getChildLocationIn3DReferenceFrame(boolean rightArm, int depth) {
      Arm arm = rightArm ? _rightArm : _leftArm;
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
      computedLoc3 = computedLoc3.add(new Tuple3(-_shoulderWidth / 2, 0, 0));
      if (rightArm) {
         computedLoc3 = computedLoc3.rotate(0, _twistRightShoulderUp.getValue(), 0);
      }
      else {
         computedLoc3 = computedLoc3.rotate(0, -_twistRightShoulderUp.getValue(), 0);
      }
      computedLoc3 = computedLoc3.add(0f, 0f, _upperSpineLength);
      if (rightArm) {
         computedLoc3 = computedLoc3.rotate(_spineBendToFront.getValue(), _spineSide.getValue(), _spineTwistToLeft.getValue());
      }
      else {
         computedLoc3 = computedLoc3.rotate(_spineBendToFront.getValue(), -_spineSide.getValue(), -_spineTwistToLeft.getValue());
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
         GL11.glRotatef(_spineBendToFront.getValue(), 1f, 0f, 0f);
         GL11.glRotatef(_spineSide.getValue(), 0f, 1f, 0f);
         GL11.glRotatef(_spineTwistToLeft.getValue(), 0f, 0f, 1.0f);
         GL11.glTranslatef(0f, 0f, _upperSpineLength);
         GL11.glRotatef(_twistRightShoulderUp.getValue(), 0f, 1.0f, 0f);

         // the neck starts higher than the line joining the shoulders
         headLoc2d = _head.getEndpointLocationInWindowReferenceFrame();
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
            synchronized (_children) {
               try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_children)) {
                  for (TexturedObject child : _children) {
                     if (child instanceof WeaponPart) {
                        weaponPart = (WeaponPart) child;
                     }
                  }
                  Thing thing = new Thing(weapon, weaponPart, this._glView, _invertNormals, _lengthFactor, _lengthFactor);
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
            Thing thing = new Thing(shield, this._glView, _invertNormals, _lengthFactor, _lengthFactor);
            thing._facingOffset = new Tuple3(90, 0, -90);
            addChild(thing);
         }
         catch (IOException e) {
            // this happens when we can't find the object for the Thing in the resource directories
         }
      }
      public void setHeldThing(Weapon weapon) {
         synchronized (_children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_children)) {
               for (TexturedObject child : this._children) {
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
         synchronized (_children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_children)) {
               for (TexturedObject child : this._children) {
                  if (child instanceof ForeArm) {
                     ((ForeArm) child).setHeldThing(shield);
                     return;
                  }
               }
            }
         }
      }
      public void setHeldThing(Weapon weapon) {
         synchronized (_children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_children)) {
               for (TexturedObject child : this._children) {
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
         if (_rightArm != null) {
            _rightArm.setHeldThing(shield);
         }
      }
      else {
         if (_leftArm != null) {
            _leftArm.setHeldThing(shield);
         }
      }
   }
   public void setHeldThing(boolean rightHand, Weapon weapon) {
      if (rightHand) {
         if (_rightArm != null) {
            _rightArm.setHeldThing(weapon);
         }
      }
      else {
         if (_leftArm != null) {
            _leftArm.setHeldThing(weapon);
         }
      }
   }

   public void setArmor(Armor armor) {
      try {
         _armor = new Thing(armor, this.getClass().getSimpleName(), _glView, _invertNormals, _lengthFactor, _widthFactor);
      }
      catch (IOException e) {
         // this happens when we can't find the object for the Thing in the resource directories
         _armor = null;
      }
      if (_leftArm != null) {
         _leftArm.setArmor(armor);
      }
      if (_rightArm != null) {
         _rightArm.setArmor(armor);
      }
      _head.setArmor(armor);
   }

   public void removeArm(boolean rightArm) {
      if (rightArm) {
         _rightArm = null;
      }
      else {
         _leftArm = null;
      }
   }

   public TexturedObject getModelHand(boolean rightSide) {
      return new Arm(!rightSide, _texture, _selectedTexture, _glView, _lengthFactor, _widthFactor, _raceName, _isMale);
   }

   public TexturedObject getModelHead() {
      return new Head(_texture, _selectedTexture, _glView, _lengthFactor, _widthFactor, _raceName, _isMale);
   }

   public TexturedObject getModelWing(boolean rightSide) {
      return null;
   }
   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Torso_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_TORSO);
   }
}

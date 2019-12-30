package ostrowski.graphics.objects3d;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import ostrowski.graphics.GLView;
import ostrowski.graphics.model.Message;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.Thing.Armor;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;

public class Pelvis extends BodyPart
{
   private RangedValue _hipTwistLeftUp;   // + = left hip higher than right
   private RangedValue _spinalTwistCC;    // one hip more forward than the other
   private Tuple3 _legOffset;

   private Leg    _leftLeg;
   private Leg    _rightLeg;
   private Thing  _armor = null;

   public Pelvis(Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super("Pelvis", "res/bodyparts/pelvis.obj", false/*invertNormals*/, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      _leftLeg = new Leg(true, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      _rightLeg = new Leg(false, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);

      _hipTwistLeftUp = getValueByNameAsRangedValue("hipTwistLeftUp");
      _spinalTwistCC = getValueByNameAsRangedValue("spinalTwistCC");
      _legOffset = getValueByNameAsTuple3("legOffset"); // 8

      _legOffset = new Tuple3(_legOffset.getX() * widthFactor, _legOffset.getY() * widthFactor, _legOffset.getZ() * lengthFactor);
   }

   @Override
   public void render(GLView glView, List<Message> messages) {

      bindToTexture();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glEnable(GL11.GL_NORMALIZE); // see http://www.opengl.org/resources/features/KilgardTechniques/oglpitfall/
      GL11.glPushMatrix(); // save matrix before modeling.
      {
         // from midpoint on spine
         GL11.glRotatef(_hipTwistLeftUp.getValue(), 0f, 1.0f, 0f);
         GL11.glRotatef(_spinalTwistCC.getValue(), 0f, 0f, 1.0f);
         for (ObjModel model : _models) {
            model.render(glView, messages);
         }

         if (_armor != null) {
            _armor.render(glView, messages);
         }
         GL11.glRotatef(180, 1f, 0f, 0f);
         GL11.glTranslatef(_legOffset.getX(), _legOffset.getY(), _legOffset.getZ());
         //if (GLScene.USING_SCALING_FOR_LEFT_SIDE)
         GL11.glScalef(-1f, 1f, 1f);
         glView.defineLight(-1f);
         if (_leftLeg != null) {
            _leftLeg.render(glView, messages);
         }
         //if (GLScene.USING_SCALING_FOR_LEFT_SIDE)
         GL11.glScalef(-1f, 1f, 1f);
         glView.defineLight(1f);
         GL11.glTranslatef(-2 * _legOffset.getX(), 0f, 0f);
         //GL11.glColor3f(1.0f, 0.0f, 0.0f);
         if (_rightLeg != null) {
            _rightLeg.render(glView, messages);
         }
         //GL11.glRotatef(90, 0f, -1f, 0f);
      }
      GL11.glPopMatrix();
      //      Tuple3 toeLoc = getToeLocationIn3DReferenceFrame(true/*rightFoot*/);
      //      GL11.glTranslatef(toeLoc.getX(), toeLoc.getY(), toeLoc.getZ());
      //      drawSphere(5, 6, 6);
      //      GL11.glTranslatef(-toeLoc.getX(), -toeLoc.getY(), -toeLoc.getZ());
   }

   @Override
   public void validateRanges() {
      _hipTwistLeftUp.validateRange();
      _spinalTwistCC.validateRange();
      if (_leftLeg != null) {
         _leftLeg.validateRanges();
      }
      if (_rightLeg != null) {
         _rightLeg.validateRanges();
      }
   }

   public class Toes extends BendJoint
   {
      public Toes(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName,
                  boolean isMale) {
         super((leftSide ? "LeftToes" : "RightToes"), "res/bodyparts/toes.obj", leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor,
               raceName, isMale);
      }

      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_Toes_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_TOES);
      }
   }

   public class Foot extends BendJoint
   {
      public Foot(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName,
                  boolean isMale) {
         super((leftSide ? "LeftFoot" : "RightFoot"), "res/bodyparts/foot.obj", leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor,
               raceName, isMale);
         addChild(new Toes(leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale));
      }

      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_Foot_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_FEET);
      }
   }

   public class LowerLeg extends TwistBendJoint
   {
      public LowerLeg(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName,
                      boolean isMale) {
         super((leftSide ? "LeftLowerLeg" : "RightLowerLeg"), "res/bodyparts/lowerleg.obj", leftSide, texture, selectedTexture, glView, lengthFactor,
               widthFactor, raceName, isMale);
         addChild(new Foot(leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale));
      }

      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_LowerLeg_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_LOWERLEG);
      }
   }

   public class Leg extends BallSocketJoint
   {
      public Leg(boolean leftSide, Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName,
                 boolean isMale) {
         super((leftSide ? "LeftLeg" : "RightLeg"), "res/bodyparts/leg.obj", leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName,
               isMale);
         addChild(new LowerLeg(leftSide, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale)); // next body part
      }

      @Override
      protected Semaphore getChildLock() {
         return new Semaphore("BodyPart_Legs_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_LEGS);
      }
      
   }

   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("h")) _hipTwistLeftUp.setValue(angleMap.get("h"));
      if (angleMap.containsKey("t")) _spinalTwistCC.setValue(angleMap.get("t"));
   }

   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("h", _hipTwistLeftUp.getValue());
      anglesMap.put("t", _spinalTwistCC.getValue());
      return anglesMap;
   }

   @Override
   public void getParts(List<TexturedObject> parts) {
      parts.add(this);
      if (_leftLeg != null) {
         _leftLeg.getParts(parts);
      }
      _rightLeg.getParts(parts);
   }


   /*
   GL11.glRotatef(_hipTwistLeftUp.getValue(), 0f, 1.0f, 0f);
   GL11.glRotatef(_spinalTwistCC.getValue(), 0f, 0f, 1.0f);
   GL11.glRotatef(180, 1f, 0f, 0f);
   GL11.glTranslatef(_legOffset.getX(), _legOffset.getY(), _legOffset.getZ());
   GL11.glScalef(-1f, 1f, 1f);
   _leftLeg.render(glView, messages);
   GL11.glScalef(-1f, 1f, 1f);
   GL11.glTranslatef(-2 * _legOffset.getX(), 0f, 0f);
   _rightLeg.render(glView, messages);
    */
   public Tuple3 getChildLocationIn3DReferenceFrame(boolean rightFoot, int depth) {
      Leg leg = rightFoot ? _rightLeg : _leftLeg;
      Tuple3 computedLoc3;
      
      if (depth == 0) {
         computedLoc3 = new Tuple3(0, 0, 0);
      }
      else {
         computedLoc3 = leg.getEndPoint(depth - 1);
         computedLoc3 = computedLoc3.rotate(180f, 0f, 0f);
      }
      if (!rightFoot) {
         computedLoc3 = computedLoc3.add(-_legOffset.getX(), -_legOffset.getY(), -_legOffset.getZ());
         computedLoc3.scale(-1f, 1f, 1f);
      }
      else {
         computedLoc3 = computedLoc3.add(-_legOffset.getX(), -_legOffset.getY(), -_legOffset.getZ());
      }
      computedLoc3 = computedLoc3.rotate(0, _hipTwistLeftUp.getValue(), _spinalTwistCC.getValue());
      return computedLoc3;
   }

   public FloatBuffer getToeLocationInWindowReferenceFrame(boolean rightFoot) {
      GL11.glPushMatrix();
      try {
         GL11.glRotatef(_hipTwistLeftUp.getValue(), 0f, 1.0f, 0f);
         GL11.glRotatef(_spinalTwistCC.getValue(), 0f, 0f, 1.0f);
         GL11.glTranslatef(_legOffset.getX(), _legOffset.getY(), _legOffset.getZ());
         if (rightFoot) {
            if (_rightLeg == null) return BodyPart.projectToWindowLocation();
            FloatBuffer result = _rightLeg.getEndpointLocationInWindowReferenceFrame(2);
            return result;
         }
         GL11.glTranslatef(-2 * _legOffset.getX(), 0f, 0f);
         //if (GLScene.USING_SCALING_FOR_LEFT_SIDE)
         GL11.glScalef(-1f, 1f, 1f);
         if (_leftLeg == null) return BodyPart.projectToWindowLocation();
         return _leftLeg.getEndpointLocationInWindowReferenceFrame(2);
      } finally {
         GL11.glPopMatrix();
      }
   }

   public void setArmor(Armor armor) {
      try {
         _armor = new Thing(armor, this.getClass().getSimpleName(), _glView, _invertNormals, _lengthFactor, _widthFactor);
      } catch (IOException e) {
         // this happens if the requested armor doesn't have a piece for this bodypart
         _armor = null;
      }
      if (_leftLeg != null) _leftLeg.setArmor(armor);
      if (_rightLeg != null) _rightLeg.setArmor(armor);
   }

   public void removeLeg(boolean rightLeg) {
      if (rightLeg) _rightLeg = null;
      else _leftLeg = null;
   }

   public TexturedObject getModelLeg(boolean rightSide) {
      return new Leg(!rightSide, _texture, _selectedTexture, _glView, _lengthFactor, _widthFactor, _raceName, _isMale);
   }

   public TexturedObject getModelTail() {
      return null;
   }

   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Pelvis_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_PELVIS);
   }

}

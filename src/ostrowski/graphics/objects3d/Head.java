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
import ostrowski.graphics.objects3d.Thing.Armor;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;

public class Head extends BodyPart
{
   float                  _neckTiltForward;  // -90 is straight up, -45 is tilting head forward at 45 degrees
   float                  _neckTiltToLeft;    //
   float                  _neckTwist;    // + is turning head c?w
   float                  _maxNeckTiltForward;
   float                  _minNeckTiltForward;
   float                  _maxNeckTiltToLeft;
   float                  _minNeckTiltToLeft;
   float                  _maxNeckTwist;
   float                  _minNeckTwist;
   float                  _rHead;
   float                  _neckLength;
   float                  _neckRadius;
   float                  _neckHeight;

   private Thing          _armor;
   private TexturedObject _beardObj = null;
   private TexturedObject _hairObj  = null;

   //private ArrayList<ArrayList<Float>> _polygons;
   public Head(Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super("Head", "res/bodyparts/head.obj", false/*invertNormals*/, texture, selectedTexture, glView,
            ((2 * lengthFactor) + widthFactor) / 3f/*lengthFactor*/,
            ((2 * lengthFactor) + widthFactor) / 3f/*widthFactor*/, raceName, isMale);

      _neckTiltForward = getValueByNameAsFloat("neckTiltForward");
      _neckTiltToLeft  = getValueByNameAsFloat("neckTiltToLeft");
      _neckTwist       = getValueByNameAsFloat("neckTwist");
      _rHead           = getValueByNameAsFloat("rHead");
      _neckLength      = getValueByNameAsFloat("neckLength");
      _neckRadius      = getValueByNameAsFloat("neckRadius");
      _neckHeight      = getValueByNameAsFloat("neckHeight");
      _maxNeckTiltForward  = getValueByNameAsFloat("maxNeckTiltForward");
      _minNeckTiltForward  = getValueByNameAsFloat("minNeckTiltForward");
      _maxNeckTiltToLeft   = getValueByNameAsFloat("maxNeckTiltToLeft");
      _minNeckTiltToLeft   = getValueByNameAsFloat("minNeckTiltToLeft");
      _maxNeckTwist        = getValueByNameAsFloat("maxNeckTwist");
      _minNeckTwist        = getValueByNameAsFloat("minNeckTwist");

      _neckLength *= lengthFactor;
      _neckHeight *= lengthFactor;
      _rHead      *= widthFactor;
      _neckRadius *= widthFactor;
      if (glView == null) {
         return;
      }

      // Monster races don't need hair or beards:
      if ((raceName != null ) && (raceName.equalsIgnoreCase("wolf"))) {
         return;
      }

      try {
         // add hair and/or beard:
         Texture hairTexture;
         if (isMale) {
            hairTexture = glView.getTextureLoader().getTexture("res/bodyparts/texture_maleHair.png");
         }
         else {
            hairTexture = glView.getTextureLoader().getTexture("res/bodyparts/texture_maleHair.png"); // femaleHair.png");
         }

         boolean hasHair = true;
         if (raceName != null) {
            if (raceName.equalsIgnoreCase("Cyclops") ||
                raceName.equalsIgnoreCase("Minotaur")) {
               hasHair = false;
            }
         }
         if (hasHair) {
            // add the hair
            _hairObj = new HairObject("hair", "res/bodyparts/malehair1.obj", _invertNormals, hairTexture, hairTexture, glView, _lengthFactor, _widthFactor,
                                      raceName, isMale);
         }
         // add the beard, if needed:
         if ((raceName != null) && raceName.equalsIgnoreCase("Dwarf")) {
            _beardObj = new HairObject("beard", "res/bodyparts/beard1.obj", _invertNormals, hairTexture, hairTexture, glView, _lengthFactor, _widthFactor,
                                       raceName, isMale);
         }
      } catch (IOException e) {
      }
   }

   @Override
   public void validateRanges() {
      if (_neckTwist < _minNeckTwist) {
         _neckTwist = _minNeckTwist; // can't turn too far left
      }
      else if (_neckTwist > _maxNeckTwist)
       {
         _neckTwist = _maxNeckTwist; // or too far right
      }

      if (_neckTiltToLeft < _minNeckTiltToLeft) {
         _neckTiltToLeft = _minNeckTiltToLeft; // can't turn too far left
      }
      else if (_neckTiltToLeft > _maxNeckTiltToLeft)
       {
         _neckTiltToLeft = _maxNeckTiltToLeft; // or too far right
      }

      if (_neckTiltForward < _minNeckTiltForward) {
         _neckTiltForward = _minNeckTiltForward; // can't tilt too far back
      }
      else if (_neckTiltForward > _maxNeckTiltForward)
       {
         _neckTiltForward = _maxNeckTiltForward; // can't tilt head too far forward.
      }
   }

   @Override
   public void render(GLView glView, ArrayList<Message> messages) {
      // neck joint
      GL11.glPushMatrix();
      {
         bindToTexture();
         // rotate about the back center of the neck
         //GL11.glTranslatef(0f, _neckLength * 2 / 3, -_neckHeight);
         GL11.glRotatef(_neckTiltForward, 1.0f, 0.0f, 0.0f);
         GL11.glRotatef(_neckTiltToLeft, 0.0f, 0.0f, 1.0f);
         GL11.glRotatef(_neckTwist, 0.0f, 0f, 1.0f);
         //drawSphere(_neckRadius, _facesCount, _facesCount);
         // restore center of neck translation
         //GL11.glTranslatef(0.0f, -_neckLength * 2 / 3, _neckHeight);
         for (ObjModel model : _models) {
            model.render(glView, messages);
         }

         // If there is armor, don't draw the hair
         if (_armor != null) {
            _armor.render(glView, messages);
         }
         else if (_hairObj != null) {
            _hairObj.render(glView, messages);
         }

         synchronized (_children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_children)) {
               for (TexturedObject child : _children) {
                  child.render(glView, messages);
               }
            }
         }

         if (_beardObj != null) {
            _beardObj.render(glView, messages);
         }

         //GL11.glEnd();
      }
      GL11.glPopMatrix();
      // end neck
   }

   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("t")) {
         _neckTiltForward = angleMap.get("f");
      }
      if (angleMap.containsKey("s")) {
         _neckTiltToLeft = angleMap.get("s");
      }
      if (angleMap.containsKey("t")) {
         _neckTwist = angleMap.get("t");
      }
      validateRanges();
   }

   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", _neckTiltForward);
      anglesMap.put("s", _neckTiltToLeft);
      anglesMap.put("t", _neckTwist);
      return anglesMap;
   }

   @Override
   public void getParts(ArrayList<TexturedObject> parts) {
      parts.add(this);
   }

   public FloatBuffer getEndpointLocationInWindowReferenceFrame() {
      FloatBuffer results;
      GL11.glPushMatrix();
      {
         // rotate about the back center of the neck
         GL11.glTranslatef(0f, _neckLength, -_neckHeight + (_rHead * 4));
         results = projectToWindowLocation();
         GL11.glPopMatrix();
      }
      return results;
   }

   public void setArmor(Armor armor) {
      try {
         _armor = new Thing(armor, this.getClass().getSimpleName(), _glView, _invertNormals, _lengthFactor, _widthFactor);
      } catch (IOException e) {
         // this happens if the requested armor doesn't have a piece for this bodypart
         _armor = null;
      }
   }

   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Head_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_HEAD);
   }

}

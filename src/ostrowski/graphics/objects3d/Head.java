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
import ostrowski.graphics.objects3d.Thing.Armor;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;

public class Head extends BodyPart
{
   float neckTiltForward;  // -90 is straight up, -45 is tilting head forward at 45 degrees
   float neckTiltToLeft;    //
   float neckTwist;    // + is turning head c?w
   float maxNeckTiltForward;
   float minNeckTiltForward;
   float maxNeckTiltToLeft;
   float minNeckTiltToLeft;
   float maxNeckTwist;
   float minNeckTwist;
   float rHead;
   float neckLength;
   float neckRadius;
   float neckHeight;

   private Thing          armor;
   private TexturedObject beardObj = null;
   private TexturedObject hairObj  = null;

   //private List<ArrayList<Float>> polygons;
   public Head(Texture texture, Texture selectedTexture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super("Head", "res/bodyparts/head.obj", false/*invertNormals*/, texture, selectedTexture, glView,
            ((2 * lengthFactor) + widthFactor) / 3f/*lengthFactor*/,
            ((2 * lengthFactor) + widthFactor) / 3f/*widthFactor*/, raceName, isMale);

      neckTiltForward = getValueByNameAsFloat("neckTiltForward");
      neckTiltToLeft = getValueByNameAsFloat("neckTiltToLeft");
      neckTwist = getValueByNameAsFloat("neckTwist");
      rHead = getValueByNameAsFloat("rHead");
      neckLength = getValueByNameAsFloat("neckLength");
      neckRadius = getValueByNameAsFloat("neckRadius");
      neckHeight = getValueByNameAsFloat("neckHeight");
      maxNeckTiltForward = getValueByNameAsFloat("maxNeckTiltForward");
      minNeckTiltForward = getValueByNameAsFloat("minNeckTiltForward");
      maxNeckTiltToLeft = getValueByNameAsFloat("maxNeckTiltToLeft");
      minNeckTiltToLeft = getValueByNameAsFloat("minNeckTiltToLeft");
      maxNeckTwist = getValueByNameAsFloat("maxNeckTwist");
      minNeckTwist = getValueByNameAsFloat("minNeckTwist");

      neckLength *= lengthFactor;
      neckHeight *= lengthFactor;
      rHead *= widthFactor;
      neckRadius *= widthFactor;
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
            hairObj = new HairObject("hair", "res/bodyparts/malehair1.obj", invertNormals, hairTexture, hairTexture, glView, this.lengthFactor, this.widthFactor,
                                     raceName, isMale);
         }
         // add the beard, if needed:
         if ((raceName != null) && raceName.equalsIgnoreCase("Dwarf")) {
            beardObj = new HairObject("beard", "res/bodyparts/beard1.obj", invertNormals, hairTexture, hairTexture, glView, this.lengthFactor, this.widthFactor,
                                      raceName, isMale);
         }
      } catch (IOException e) {
      }
   }

   @Override
   public void validateRanges() {
      if (neckTwist < minNeckTwist) {
         neckTwist = minNeckTwist; // can't turn too far left
      }
      else if (neckTwist > maxNeckTwist)
       {
         neckTwist = maxNeckTwist; // or too far right
      }

      if (neckTiltToLeft < minNeckTiltToLeft) {
         neckTiltToLeft = minNeckTiltToLeft; // can't turn too far left
      }
      else if (neckTiltToLeft > maxNeckTiltToLeft)
       {
         neckTiltToLeft = maxNeckTiltToLeft; // or too far right
      }

      if (neckTiltForward < minNeckTiltForward) {
         neckTiltForward = minNeckTiltForward; // can't tilt too far back
      }
      else if (neckTiltForward > maxNeckTiltForward)
       {
         neckTiltForward = maxNeckTiltForward; // can't tilt head too far forward.
      }
   }

   @Override
   public void render(GLView glView, List<Message> messages) {
      // neck joint
      GL11.glPushMatrix();
      {
         bindToTexture();
         // rotate about the back center of the neck
         //GL11.glTranslatef(0f, neckLength * 2 / 3, -_neckHeight);
         GL11.glRotatef(neckTiltForward, 1.0f, 0.0f, 0.0f);
         GL11.glRotatef(neckTiltToLeft, 0.0f, 0.0f, 1.0f);
         GL11.glRotatef(neckTwist, 0.0f, 0f, 1.0f);
         //drawSphere(neckRadius, facesCount, facesCount);
         // restore center of neck translation
         //GL11.glTranslatef(0.0f, -neckLength * 2 / 3, neckHeight);
         for (ObjModel model : models) {
            model.render(glView, messages);
         }

         // If there is armor, don't draw the hair
         if (armor != null) {
            armor.render(glView, messages);
         }
         else if (hairObj != null) {
            hairObj.render(glView, messages);
         }

         synchronized (children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
               for (TexturedObject child : children) {
                  child.render(glView, messages);
               }
            }
         }

         if (beardObj != null) {
            beardObj.render(glView, messages);
         }

         //GL11.glEnd();
      }
      GL11.glPopMatrix();
      // end neck
   }

   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("t")) {
         neckTiltForward = angleMap.get("f");
      }
      if (angleMap.containsKey("s")) {
         neckTiltToLeft = angleMap.get("s");
      }
      if (angleMap.containsKey("t")) {
         neckTwist = angleMap.get("t");
      }
      validateRanges();
   }

   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", neckTiltForward);
      anglesMap.put("s", neckTiltToLeft);
      anglesMap.put("t", neckTwist);
      return anglesMap;
   }

   @Override
   public void getParts(List<TexturedObject> parts) {
      parts.add(this);
   }

   public FloatBuffer getEndpointLocationInWindowReferenceFrame() {
      FloatBuffer results;
      GL11.glPushMatrix();
      {
         // rotate about the back center of the neck
         GL11.glTranslatef(0f, neckLength, -neckHeight + (rHead * 4));
         results = projectToWindowLocation();
         GL11.glPopMatrix();
      }
      return results;
   }

   public void setArmor(Armor armor) {
      try {
         this.armor = new Thing(armor, this.getClass().getSimpleName(), glView, invertNormals, lengthFactor, widthFactor);
      } catch (IOException e) {
         // this happens if the requested armor doesn't have a piece for this bodypart
         this.armor = null;
      }
   }

   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Head_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_HEAD);
   }

}

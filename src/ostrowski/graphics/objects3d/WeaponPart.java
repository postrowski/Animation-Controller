package ostrowski.graphics.objects3d;

import java.util.ArrayList;
import java.util.HashMap;

import ostrowski.graphics.GLView;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;

public class WeaponPart extends BodyPart
{
   public Float _lengthOffset;
   public Float _frontRot;
   public Float _twistRot;

   public WeaponPart(String name, String modelResourceName, boolean invertNormals,
                     Texture texture, Texture selectedTexture,
                     GLView glView, float lengthFactor, float widthFactor) {
      super(name, modelResourceName, invertNormals, texture, selectedTexture, glView, lengthFactor, widthFactor, null/*raceName*/, true/*isMale*/);
   }

   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", _frontRot);
      anglesMap.put("t", _twistRot);
      anglesMap.put("l", _lengthOffset);
      return anglesMap;
   }

   @Override
   public void getParts(ArrayList<TexturedObject> parts) {
      parts.add(this);
   }

   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("l"))
         _lengthOffset = angleMap.get("l");
      if (angleMap.containsKey("f"))
         _frontRot = angleMap.get("f");
      if (angleMap.containsKey("t"))
         _twistRot = angleMap.get("t");
   }

   @Override
   public void validateRanges() {
   }

   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Weapon_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_WEAPON);
   }

}

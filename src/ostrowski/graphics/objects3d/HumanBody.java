package ostrowski.graphics.objects3d;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.lwjgl.opengl.GL11;

import ostrowski.graphics.AnimationFrame;
import ostrowski.graphics.AnimationSequence;
import ostrowski.graphics.GLView;
import ostrowski.graphics.SequenceLibrary;
import ostrowski.graphics.model.Message;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.Thing.Armor;
import ostrowski.graphics.objects3d.Thing.Shield;
import ostrowski.graphics.objects3d.Thing.Weapon;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.AnimationControllerSemaphore;
import ostrowski.util.Semaphore;

public class HumanBody extends BodyPart
{
   // Cannot start w/ all joint angles at 0;
   // results in no movement...
   private final Pelvis pelvis;
   private final Torso  torso;

   private Tuple3 locationOffset = new Tuple3(0, 0, 0);
   public  Tuple3 positionOffset = new Tuple3(0, 51f, 0);
   public  float  twistOffset    = 0;
   private float  facingOffset   = 0;
   public  float  tiltForward    = 0;
   public  float  tiltSide       = 0;

   HashMap<String, TexturedObject> parts = new HashMap<>();

   private       HashMap<String, HashMap<String, Float>> origPos                = null;
   private       HashMap<String, HashMap<String, Float>> nextPos                = null;
   private final List<AnimationFrame>                    pendingAnimationFrames = new ArrayList<>();
   private       AnimationFrame                          nextFrame              = null;

   private long    origPosTime = -1;
   private long    nextPosTime = -1;
   private boolean repeat      = false;

   private int           animationFrameSequence = 0;
   public  Position      pos;
   public  List<Message> messages               = new ArrayList<>();
   public  int           repeatCount            = 1;

   static String delimiter = "|";

   public enum Position {
      Standing, Sitting, Crouching, Kneeling, LayingOnBack, LayingOnFront
   }

   public HumanBody(Texture texture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super("Body", null/*modelResourceName*/, false/*invertNormals*/, texture, texture, glView, lengthFactor, widthFactor, raceName, isMale);
      pelvis = new Pelvis(texture, texture, glView, lengthFactor, widthFactor, raceName, isMale);
      torso = new Torso(texture, texture, glView, lengthFactor, widthFactor, raceName, isMale);
      List<TexturedObject> parts = new ArrayList<>();
      getParts(parts);
      for (TexturedObject part : parts) {
         this.parts.put(part.getName(), part);
      }
      this.raceName = raceName;
      if (this.raceName != null) {
         HashMap<Class <? extends BodyPart>, HashMap<String, Object>> valuesByClass = valuesByNameByClassByRace.get(this.raceName);
         if (valuesByClass == null) {
            valuesByClass = valuesByNameByClassByRace.get("human");
         }
         HashMap<String, Object> valuesByName = valuesByClass.get(HumanBody.class);
         tiltForward = (Float) valuesByName.get("tiltForward");
      }
      this.parts.put("Body", this);
   }

   public FloatBuffer getHeadLocationInWindowReferenceFrame() {
      FloatBuffer results;
      GL11.glPushMatrix();
      {
         GL11.glTranslatef(locationOffset.getX(), locationOffset.getY(), locationOffset.getZ());
         GL11.glRotatef(facingOffset, 0f, 1f, 0f);
         GL11.glTranslatef(positionOffset.getX(), positionOffset.getY(), positionOffset.getZ());
         GL11.glRotatef(twistOffset, 0f, 1f, 0f);
         GL11.glRotatef(tiltSide, 0f, 0f, 1f);
         GL11.glRotatef(tiltForward, 1f, 0f, 0f);
         results = torso.getHeadLocationInWindowReferenceFrame();
      }
      GL11.glPopMatrix();
      return results;
   }

   FloatBuffer getToeLocation(boolean rightFoot) {
      FloatBuffer toeLocationIn2d = pelvis.getToeLocationInWindowReferenceFrame(rightFoot);
      return BodyPart.unProjectFromWindowLocation(toeLocationIn2d);
   }

   public Tuple3 getToeLocationIn3DReferenceFrame(boolean rightFoot) {
      Tuple3 location = pelvis.getChildLocationIn3DReferenceFrame(rightFoot, 4);
      location = location.add(positionOffset);
      location = location.rotate(0, facingOffset, 0);
      location = location.add(locationOffset);
      location = location.rotate(tiltForward, 0, tiltSide);
      location = location.rotate(0, twistOffset, 0);
      return location;
   }

   @Override
   public void validateRanges() {
      pelvis.validateRanges();
      torso.validateRanges();
   }

   @Override
   public void render(GLView glView, List<Message> messages) {
      // Bind to the texture for our model then render it.
      // This actually draws the geometry to the screen.
      // Must bind the texture BEFORE we call glBegin(...)
      GL11.glPushMatrix();
      {
         if (opacity != 1f) {
            GL11.glColor4f(1f, 1f, 1f, opacity);
         }
         //GL11.glColor3f(1.0f, 1.0f, 1.0f);

         GL11.glTranslatef(locationOffset.getX(), locationOffset.getY(), locationOffset.getZ());
         GL11.glRotatef(facingOffset, 0f, 1f, 0f);
         GL11.glTranslatef(positionOffset.getX(), positionOffset.getY(), positionOffset.getZ());
         GL11.glRotatef(twistOffset, 0f, 1f, 0f);
         GL11.glRotatef(tiltSide, 0f, 0f, 1f);
         GL11.glRotatef(tiltForward, 1f, 0f, 0f);

         // build lower body
         pelvis.render(glView, messages);
         // upper body
         torso.render(glView, messages);
      }
      GL11.glPopMatrix();

      // turn this block on to see all the joints drawn:
      boolean drawJoints = false;
      if (drawJoints) {
         GL11.glColor4f(1f, 1f, 1f, 1f);
         List<Tuple3> jointPoints = setHeightBasedOnLowestLimbPoint();
         for (Tuple3 jointPoint : jointPoints) {
            GL11.glTranslatef(jointPoint.getX(), jointPoint.getY(), jointPoint.getZ());
            BodyPart.drawSphere(2, 8, 8);
            GL11.glTranslatef(-jointPoint.getX(), -jointPoint.getY(), -jointPoint.getZ());
         }
      }

      if (opacity != 1f) {
         GL11.glColor4f(1f, 1f, 1f, 1f);
      }

      // set the location of any messages based on the current head position in 2D
      if ((this.messages != null) && (this.messages.size() > 0)) {
         FloatBuffer headLoc = getHeadLocationInWindowReferenceFrame();
         int x = (int) headLoc.get(0);
         int y = (int) headLoc.get(1);
         int z = (int) headLoc.get(2);
         if (glView.font != null) {
            for (Message message : this.messages) {
               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               message.xLoc = x;
               message.yLoc = y;
               message.zLoc = z;

               if ((message.text != null) && (message.text.length() > 0) && message.visible) {
                  messages.add(message);
               }
            }
         }
      }
   }

   public String getAngles() {
      StringBuilder sb = new StringBuilder();
      boolean first1 = true;
      Set<String> keys = parts.keySet();
      SortedSet<String> sortedKeys = new TreeSet<>(keys);
      for (String name : sortedKeys) {
         if (!first1) {
            sb.append("|");
         }
         TexturedObject child = parts.get(name);
         if (child instanceof BodyPart) {
            BodyPart part = (BodyPart) child;
            sb.append(name).append(';');
            HashMap<String, Float> map = part.getAnglesMap();
            boolean first2 = true;
            for (String key : map.keySet()) {
               if (!first2) {
                  sb.append(',');
               }
               sb.append(key).append(":").append(map.get(key));
               first2 = false;
            }
            first1 = false;
         }
      }
      return sb.toString();
   }

   public void setAngles(String text) {
      HashMap<String, HashMap<String, Float>> map = splitAnglesTextIntoHashMap(text);
      for (String name : map.keySet()) {
         TexturedObject child = parts.get(name);
         if (child instanceof BodyPart) {
            BodyPart part = (BodyPart) child;
            part.setAnglesFromMap(map.get(name));
         }
      }
      validateRanges();
   }

   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("f")) {
         tiltForward = angleMap.get("f");
      }
      if (angleMap.containsKey("s")) {
         tiltSide = angleMap.get("s");
      }
      if (angleMap.containsKey("t")) {
         twistOffset = angleMap.get("t");
      }
      validateRanges();
   }

   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", tiltForward);
      anglesMap.put("s", tiltSide);
      anglesMap.put("t", twistOffset);
      return anglesMap;
   }

   public static HashMap<String, HashMap<String, Float>> splitAnglesTextIntoHashMap(String text) {
      HashMap<String, HashMap<String, Float>> map = new HashMap<>();
      if ((text == null) || (text.length() == 0)) {
         return map;
      }
      StringTokenizer st = new StringTokenizer(text, delimiter);
      while (st.hasMoreElements()) {
         String token = st.nextToken();
         int loc = token.indexOf(';');
         if (loc != -1) {
            String name = token.substring(0, loc);
            HashMap<String, Float> nameValueMap = new HashMap<>();
            StringTokenizer st2 = new StringTokenizer(token.substring(name.length() + 1), ",");
            while (st2.hasMoreElements()) {
               String token2 = st2.nextToken();
               loc = token2.indexOf(':');
               if (loc != -1) {
                  String pairName = token2.substring(0, loc);
                  String pairValue = token2.substring(loc + 1);
                  try {
                     Float value = Float.parseFloat(pairValue);
                     nameValueMap.put(pairName, value);
                  } catch (NumberFormatException e) {
                     pairValue = null;
                  }
               }
            }
            map.put(name, nameValueMap);
         }
      }
      return map;
   }

   public static String combineHashMapIntoAnglesText(HashMap<String, HashMap<String, Float>> map) {
      StringBuilder sb = new StringBuilder();
      for (String key : map.keySet()) {
         HashMap<String, Float> value = map.get(key);
         sb.append(key).append(';');
         for (String subkey : value.keySet()) {
            Float subvalue = value.get(subkey);
            sb.append(subkey);
            sb.append(':');
            sb.append(subvalue);
            sb.append(',');
         }
         sb.setLength(sb.length() - 1);
         sb.append(delimiter);
      }
      return sb.toString();
   }

   public void addAnimationSequence(AnimationSequence seq) {
      if (seq != null) {
         for (int i = 0; i < seq.frames.size(); i++) {
            addAnimationFrame(seq.frames.get(i));
         }
      }
   }

   private void addAnimationFrame(AnimationFrame frame) {
      pendingAnimationFrames.add(frame);
   }

   public void setRepeat(boolean repeat) {
      this.repeat = repeat;
   }

   public boolean advanceAnimation() {
      if ((origPos == null) || (nextPos == null) || (origPosTime == -1) || (nextPosTime == -1)) {
         if (pendingAnimationFrames.size() == 0)
          {
            return false; // no more frames, terminate the animation
         }
      }

      long now = System.currentTimeMillis();
      if (now > nextPosTime) {
         if (pendingAnimationFrames.size() <= 0) {
            origPos = nextPos = null;
            origPosTime = nextPosTime = -1;
            setHeightBasedOnLowestLimbPoint();
            return false; // no more frames, terminate the animation
         }
         AnimationFrame curFrame = nextFrame;
         nextFrame = pendingAnimationFrames.remove(0);
         if (repeat) {
            pendingAnimationFrames.add(nextFrame);
         }

         animationFrameSequence++;
         if (animationFrameSequence >= (pendingAnimationFrames.size() * repeatCount) ) {
            positionOffset = new Tuple3(0, 0, 0);
            animationFrameSequence = 0;
         }

         if (curFrame == null) {
            origPos = splitAnglesTextIntoHashMap(getAngles());
         }
         else {
            origPos = splitAnglesTextIntoHashMap(curFrame.data);
         }
         nextPos = splitAnglesTextIntoHashMap(nextFrame.data);
         origPosTime = System.currentTimeMillis();
         nextPosTime = origPosTime + nextFrame.timeInMilliseconds;
      }

      Tuple3 toeLocBefore = null;
      Tuple3 toeLocAfter = null;
      if (nextFrame.rightFootPlanted) {
         toeLocBefore = getToeLocationIn3DReferenceFrame(true/*rightFoot*/);
         //         coreLocX = 50 + rightToeLocBefore.get(0);
      }
      else if (nextFrame.leftFootPlanted) {
         toeLocBefore = getToeLocationIn3DReferenceFrame(false/*rightFoot*/);
         //         coreLocX = 50 + leftToeLocBefore.get(0);
      }

      long duration = nextPosTime - origPosTime;
      long into = now - origPosTime;
      float percentAlong = ((float) into) / duration;

      for (String name : parts.keySet()) {
         TexturedObject child = parts.get(name);
         if (child instanceof BodyPart) {
            BodyPart part = (BodyPart) child;
            HashMap<String, Float> originalPos = origPos.get(name);
            HashMap<String, Float> nextPos = this.nextPos.get(name);
            // Some BodyParts will not have any movement,
            // and they may not be present in the nextPos HashMap
            if ((originalPos == null) || (nextPos == null)) {
               continue;
            }
            HashMap<String, Float> interpolatedPos = new HashMap<>();
            // Not all components in each BodyParts will moves:
            boolean somethingChanged = false;
            for (String key : nextPos.keySet()) {
               Float orig = originalPos.get(key);
               Float next = nextPos.get(key);
               if ((orig != next) && (orig != null)) {
                  somethingChanged = true;
                  Float current = ((next - orig) * percentAlong) + orig;
                  interpolatedPos.put(key, current);
               }
            }
            if (somethingChanged) {
               part.setAnglesFromMap(interpolatedPos);
            }
         }
      }

      if (nextFrame.rightFootPlanted) {
         toeLocAfter = getToeLocationIn3DReferenceFrame(true/*rightFoot*/);
      }
      else if (nextFrame.leftFootPlanted) {
         toeLocAfter = getToeLocationIn3DReferenceFrame(false/*rightFoot*/);
      }
      if ((toeLocBefore != null) && (toeLocAfter != null)) {
         Tuple3 movement = toeLocAfter.subtract(toeLocBefore);
         if (movement.magnitude() != 0) {
            positionOffset = positionOffset.subtract(movement);
         }
      }
      //else {
         // If we kept a toe in place, we should not need to recompute our current height - at least
         // not until we add elevation
         setHeightBasedOnLowestLimbPoint();
      //}
      return true; // still have more frames, don't terminate the animation
   }

   public void clearAnimations() {
      origPos = nextPos = null;
      origPosTime = nextPosTime = -1;
      pendingAnimationFrames.clear();
      positionOffset = new Tuple3(0, 0, 0);
      setHeightBasedOnLowestLimbPoint();
      animationFrameSequence = 0;
   }

   public BodyPart getBodyPart(String partName) {
      TexturedObject child = parts.get(partName);
      if (child instanceof BodyPart) {
         return (BodyPart) child;
      }
      return null;
   }

   public void setPosition(Position pos) {
      String posKeyFrameName = null;
      switch (pos) {
         case Crouching:     posKeyFrameName = "Crouching"; break;
         case Kneeling:      posKeyFrameName = "Kneeling"; break;
         case LayingOnBack:  posKeyFrameName = "LayingOnBack"; break;
         case LayingOnFront: posKeyFrameName = "LayingOnFront"; break;
         case Sitting:       posKeyFrameName = "Sitting"; break;
         case Standing:      posKeyFrameName = "Ready_Sword_Shield"; break;
      }
      if (setKeyFrame(posKeyFrameName)) {
         this.pos = pos;
      }
   }
   public boolean setKeyFrame(String posKeyFrameName) {
      if (posKeyFrameName == null) {
         return false;
      }
      String angles = SequenceLibrary.keyFrames.get(posKeyFrameName);
      if (angles != null) {
         setAngles(angles);
         setHeightBasedOnLowestLimbPoint();
         return true;
      }
      return false;
   }

   public void setHeldThing(boolean rightHand, Shield shield) {
      torso.setHeldThing(rightHand, shield);
   }

   public void setHeldThing(boolean rightHand, Weapon weapon) {
      torso.setHeldThing(rightHand, weapon);
   }

   public void setHeldThing(boolean rightHand, String heldThingNameIn) {
      if ((heldThingNameIn == null) || (heldThingNameIn.length() == 0)) {
         setHeldThing(rightHand, (Shield) null);
         setHeldThing(rightHand, (Weapon) null);
         return;
      }
      String heldThingName = heldThingNameIn;
      try {
         heldThingName = heldThingName.replace(" ", "");
         heldThingName = heldThingName.replace("-", "");
         heldThingName = heldThingName.replace(",Fine", "");
         Weapon weapon = Weapon.valueOf(heldThingName);
         setHeldThing(rightHand, (Shield) null);
         setHeldThing(rightHand, weapon);
      } catch (Exception e) {
         try {
            heldThingName = heldThingName.replace("Magic", "");
            Shield shield = Shield.valueOf(heldThingName);
            setHeldThing(rightHand, (Weapon) null);
            setHeldThing(rightHand, shield);
         } catch (Exception ex) {
            System.out.println("unable to find object with a name of " + heldThingName);
            ex = null;
         }
      }
   }

   public void setArmor(String armorName) {
      Armor armor = Armor.NoArmor;
      if ((armorName != null) && (armorName.length() > 0))
      {
         try {
            armor = Enum.valueOf(Armor.class, armorName.replace(" ", ""));
         }
         catch (Exception e) {
            if (armorName.equalsIgnoreCase("ClothArmor")) {
               armor = Armor.HeavyCloth;
            }
            else {
               armor = Armor.NoArmor;
            }
         }
         // until all armors are finished,
         // reclassify unfinished armors into one that are complete:
         switch (armor) {
            case Cloth:
            case HeavyCloth:
            case NoArmor:
               armor = Armor.NoArmor;
               break;
            case ChainMail:
            case ElvenChain:
            case HeavyChain:
               armor = Armor.ChainMail;
               break;
            case HeavyLeather:
            case Leather:
            case Samurai:
            case ScaleMail:
               armor = Armor.Leather;
               break;
            case BandedMail:
            case HeavyPlate:
            case LightPlate:
            case Mithril:
            case PlateMail:
               armor = Armor.PlateMail;
         }
      }
      pelvis.setArmor(armor);
      torso.setArmor(armor);
   }

   public void setOpacity(float opacity) {
      this.opacity = opacity;
      //      for (TexturedObject child : parts.values()) {
      //         child.opacity = opacity;
      //      }
   }

   public void removeArm(boolean rightArm) {
      torso.removeArm(rightArm);
   }

   public void removeLeg(boolean rightLeg) {
      pelvis.removeLeg(rightLeg);
   }

   public List<Tuple3> setHeightBasedOnLowestLimbPoint() {
      List<Tuple3> points = new ArrayList<>();
      for (int i = 0; i <= 4; i++) {
         points.add(pelvis.getChildLocationIn3DReferenceFrame(true/*rightFoot*/, i));
         points.add(pelvis.getChildLocationIn3DReferenceFrame(false/*rightFoot*/, i));
         points.add(torso.getChildLocationIn3DReferenceFrame(true/*rightArm*/, i));
         points.add(torso.getChildLocationIn3DReferenceFrame(false/*rightArm*/, i));
      }
      float minY = 0;
      boolean firstItem = true;

      List<Tuple3> adjustedPoints = new ArrayList<>();
      for (Tuple3 point : points) {
         point = point.rotate(tiltForward, 0f, 0f);
         point = point.rotate(0f, 0f, tiltSide);
         point = point.rotate(0f, twistOffset, 0f);

         if (firstItem || (point.getY() < minY)) {
            minY = point.getY();
         }
         firstItem = false;
         adjustedPoints.add(point);
      }
      positionOffset = new Tuple3(positionOffset.getX(), 2 - minY, positionOffset.getZ());
      points.clear();
      for (Tuple3 point : adjustedPoints) {
         point = point.add(positionOffset);
         point = point.rotate(0f, facingOffset, 0f);
         point = point.add(locationOffset);
         points.add(point);
      }
      return points;
   }

   @Override
   public void getParts(List<TexturedObject> parts) {
      pelvis.getParts(parts);
      torso.getParts(parts);
   }

   public TexturedObject getModelHand(boolean rightSide) {
      return torso.getModelHand(rightSide);
   }
   public TexturedObject getModelLeg(boolean rightSide) {
      return pelvis.getModelLeg(rightSide);
   }
   public TexturedObject getModelHead() {
      return torso.getModelHead();
   }
   public TexturedObject getModelTail() {
      return pelvis.getModelTail();
   }
   public TexturedObject getModelWing(boolean rightSide) {
      return torso.getModelWing(rightSide);
   }

   public void setFacing(int facing) {
      facingOffset = (facing * -60f) + 180f;
   }

   public void setLocationOffset(Tuple3 center) {
      locationOffset = new Tuple3(center.getX(), center.getY(), center.getZ());
      positionOffset = new Tuple3(0, 0, 0);
      setHeightBasedOnLowestLimbPoint();
   }

   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Body_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_BODY);
  }
}
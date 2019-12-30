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
   private final Pelvis                                  _pelvis;
   private final Torso                                   _torso;

   private Tuple3                                  _locationOffset         = new Tuple3(0, 0, 0);
   public Tuple3                                   _positionOffset         = new Tuple3(0, 51f, 0);
   public float                                    _twistOffset            = 0;
   private float                                   _facingOffset           = 0;
   public float                                    _tiltForward            = 0;
   public float                                    _tiltSide               = 0;

   HashMap<String, TexturedObject>                 _parts                  = new HashMap<>();

   private HashMap<String, HashMap<String, Float>> _origPos                = null;
   private HashMap<String, HashMap<String, Float>> _nextPos                = null;
   private final List<AnimationFrame>               _pendingAnimationFrames = new ArrayList<>();
   private AnimationFrame                          _nextFrame              = null;

   private long                                    _origPosTime            = -1;
   private long                                    _nextPosTime            = -1;
   private boolean                                 _repeat                 = false;

   private int                                     _animationFrameSequence = 0;
   public  Position                                _pos;

   public enum Position {
      Standing, Sitting, Crouching, Kneeling, LayingOnBack, LayingOnFront
   }

   public HumanBody(Texture texture, GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super("Body", null/*modelResourceName*/, false/*invertNormals*/, texture, texture, glView, lengthFactor, widthFactor, raceName, isMale);
      _pelvis = new Pelvis(texture, texture, glView, lengthFactor, widthFactor, raceName, isMale);
      _torso = new Torso(texture, texture, glView, lengthFactor, widthFactor, raceName, isMale);
      List<TexturedObject> parts = new ArrayList<>();
      getParts(parts);
      for (TexturedObject part : parts) {
         _parts.put(part.getName(), part);
      }
      _raceName = raceName;
      if (_raceName != null) {
         HashMap<Class <? extends BodyPart>, HashMap<String, Object>> valuesByClass = _valuesByNameByClassByRace.get(_raceName);
         if (valuesByClass == null) {
            valuesByClass = _valuesByNameByClassByRace.get("human");
         }
         HashMap<String, Object> valuesByName = valuesByClass.get(HumanBody.class);
         _tiltForward = (Float) valuesByName.get("tiltForward");
      }
      _parts.put("Body", this);
   }

   public FloatBuffer getHeadLocationInWindowReferenceFrame() {
      FloatBuffer results;
      GL11.glPushMatrix();
      {
         GL11.glTranslatef(_locationOffset.getX(), _locationOffset.getY(), _locationOffset.getZ());
         GL11.glRotatef(_facingOffset, 0f, 1f, 0f);
         GL11.glTranslatef(_positionOffset.getX(), _positionOffset.getY(), _positionOffset.getZ());
         GL11.glRotatef(_twistOffset, 0f, 1f, 0f);
         GL11.glRotatef(_tiltSide, 0f, 0f, 1f);
         GL11.glRotatef(_tiltForward, 1f, 0f, 0f);
         results = _torso.getHeadLocationInWindowReferenceFrame();
      }
      GL11.glPopMatrix();
      return results;
   }

   FloatBuffer getToeLocation(boolean rightFoot) {
      FloatBuffer toeLocationIn2d = _pelvis.getToeLocationInWindowReferenceFrame(rightFoot);
      return BodyPart.unProjectFromWindowLocation(toeLocationIn2d);
   }

   public Tuple3 getToeLocationIn3DReferenceFrame(boolean rightFoot) {
      Tuple3 location = _pelvis.getChildLocationIn3DReferenceFrame(rightFoot, 4);
      location = location.add(_positionOffset);
      location = location.rotate(0, _facingOffset, 0);
      location = location.add(_locationOffset);
      location = location.rotate(_tiltForward, 0, _tiltSide);
      location = location.rotate(0, _twistOffset, 0);
      return location;
   }

   @Override
   public void validateRanges() {
      _pelvis.validateRanges();
      _torso.validateRanges();
   }

   @Override
   public void render(GLView glView, List<Message> messages) {
      // Bind to the texture for our model then render it.
      // This actually draws the geometry to the screen.
      // Must bind the texture BEFORE we call glBegin(...)
      GL11.glPushMatrix();
      {
         if (_opacity != 1f) {
            GL11.glColor4f(1f, 1f, 1f, _opacity);
         }
         //GL11.glColor3f(1.0f, 1.0f, 1.0f);

         GL11.glTranslatef(_locationOffset.getX(), _locationOffset.getY(), _locationOffset.getZ());
         GL11.glRotatef(_facingOffset, 0f, 1f, 0f);
         GL11.glTranslatef(_positionOffset.getX(), _positionOffset.getY(), _positionOffset.getZ());
         GL11.glRotatef(_twistOffset, 0f, 1f, 0f);
         GL11.glRotatef(_tiltSide, 0f, 0f, 1f);
         GL11.glRotatef(_tiltForward, 1f, 0f, 0f);

         // build lower body
         _pelvis.render(glView, messages);
         // upper body
         _torso.render(glView, messages);
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

      if (_opacity != 1f) {
         GL11.glColor4f(1f, 1f, 1f, 1f);
      }

      // set the location of any messages based on the current head position in 2D
      if ((_messages != null) && (_messages.size() > 0)) {
         FloatBuffer headLoc = getHeadLocationInWindowReferenceFrame();
         int x = (int) headLoc.get(0);
         int y = (int) headLoc.get(1);
         int z = (int) headLoc.get(2);
         if (glView._font != null) {
            for (Message message : _messages) {
               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               message._xLoc = x;
               message._yLoc = y;
               message._zLoc = z;

               if ((message._text != null) && (message._text.length() > 0) && message._visible) {
                  messages.add(message);
               }
            }
         }
      }
   }

   public List<Message> _messages = new ArrayList<>();
   public int _repeatCount = 1;

   static String             delimiter = "|";

   public String getAngles() {
      StringBuilder sb = new StringBuilder();
      boolean first1 = true;
      Set<String> keys = _parts.keySet();
      SortedSet<String> sortedKeys = new TreeSet<>(keys);
      for (String name : sortedKeys) {
         if (!first1) {
            sb.append("|");
         }
         TexturedObject child = _parts.get(name);
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
         TexturedObject child = _parts.get(name);
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
         _tiltForward = angleMap.get("f");
      }
      if (angleMap.containsKey("s")) {
         _tiltSide = angleMap.get("s");
      }
      if (angleMap.containsKey("t")) {
         _twistOffset = angleMap.get("t");
      }
      validateRanges();
   }

   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", _tiltForward);
      anglesMap.put("s", _tiltSide);
      anglesMap.put("t", _twistOffset);
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
         for (int i = 0; i < seq._frames.size(); i++) {
            addAnimationFrame(seq._frames.get(i));
         }
      }
   }

   private void addAnimationFrame(AnimationFrame frame) {
      _pendingAnimationFrames.add(frame);
   }

   public void setRepeat(boolean repeat) {
      _repeat = repeat;
   }

   public boolean advanceAnimation() {
      if ((_origPos == null) || (_nextPos == null) || (_origPosTime == -1) || (_nextPosTime == -1)) {
         if (_pendingAnimationFrames.size() == 0)
          {
            return false; // no more frames, terminate the animation
         }
      }

      long now = System.currentTimeMillis();
      if (now > _nextPosTime) {
         if (_pendingAnimationFrames.size() <= 0) {
            _origPos = _nextPos = null;
            _origPosTime = _nextPosTime = -1;
            setHeightBasedOnLowestLimbPoint();
            return false; // no more frames, terminate the animation
         }
         AnimationFrame curFrame = _nextFrame;
         _nextFrame = _pendingAnimationFrames.remove(0);
         if (_repeat) {
            _pendingAnimationFrames.add(_nextFrame);
         }

         _animationFrameSequence++;
         if (_animationFrameSequence >= (_pendingAnimationFrames.size() * _repeatCount) ) {
            _positionOffset = new Tuple3(0, 0, 0);
            _animationFrameSequence = 0;
         }

         if (curFrame == null) {
            _origPos = splitAnglesTextIntoHashMap(getAngles());
         }
         else {
            _origPos = splitAnglesTextIntoHashMap(curFrame._data);
         }
         _nextPos = splitAnglesTextIntoHashMap(_nextFrame._data);
         _origPosTime = System.currentTimeMillis();
         _nextPosTime = _origPosTime + _nextFrame._timeInMilliseconds;
      }

      Tuple3 toeLocBefore = null;
      Tuple3 toeLocAfter = null;
      if (_nextFrame._rightFootPlanted) {
         toeLocBefore = getToeLocationIn3DReferenceFrame(true/*rightFoot*/);
         //         _coreLocX = 50 + rightToeLocBefore.get(0);
      }
      else if (_nextFrame._leftFootPlanted) {
         toeLocBefore = getToeLocationIn3DReferenceFrame(false/*rightFoot*/);
         //         _coreLocX = 50 + leftToeLocBefore.get(0);
      }

      long duration = _nextPosTime - _origPosTime;
      long into = now - _origPosTime;
      float percentAlong = ((float) into) / duration;

      for (String name : _parts.keySet()) {
         TexturedObject child = _parts.get(name);
         if (child instanceof BodyPart) {
            BodyPart part = (BodyPart) child;
            HashMap<String, Float> originalPos = _origPos.get(name);
            HashMap<String, Float> nextPos = _nextPos.get(name);
            // Some BodyParts will not have any movement,
            // and they may not be present in the _nextPos HashMap
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

      if (_nextFrame._rightFootPlanted) {
         toeLocAfter = getToeLocationIn3DReferenceFrame(true/*rightFoot*/);
      }
      else if (_nextFrame._leftFootPlanted) {
         toeLocAfter = getToeLocationIn3DReferenceFrame(false/*rightFoot*/);
      }
      if ((toeLocBefore != null) && (toeLocAfter != null)) {
         Tuple3 movement = toeLocAfter.subtract(toeLocBefore);
         if (movement.magnitude() != 0) {
            _positionOffset = _positionOffset.subtract(movement);
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
      _origPos = _nextPos = null;
      _origPosTime = _nextPosTime = -1;
      _pendingAnimationFrames.clear();
      _positionOffset = new Tuple3(0, 0, 0);
      setHeightBasedOnLowestLimbPoint();
      _animationFrameSequence = 0;
   }

   public BodyPart getBodyPart(String partName) {
      TexturedObject child = _parts.get(partName);
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
         _pos = pos;
      }
   }
   public boolean setKeyFrame(String posKeyFrameName) {
      if (posKeyFrameName == null) {
         return false;
      }
      String angles = SequenceLibrary._keyFrames.get(posKeyFrameName);
      if (angles != null) {
         setAngles(angles);
         setHeightBasedOnLowestLimbPoint();
         return true;
      }
      return false;
   }

   public void setHeldThing(boolean rightHand, Shield shield) {
      _torso.setHeldThing(rightHand, shield);
   }

   public void setHeldThing(boolean rightHand, Weapon weapon) {
      _torso.setHeldThing(rightHand, weapon);
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
      _pelvis.setArmor(armor);
      _torso.setArmor(armor);
   }

   public void setOpacity(float opacity) {
      _opacity = opacity;
      //      for (TexturedObject child : _parts.values()) {
      //         child._opacity = opacity;
      //      }
   }

   public void removeArm(boolean rightArm) {
      _torso.removeArm(rightArm);
   }

   public void removeLeg(boolean rightLeg) {
      _pelvis.removeLeg(rightLeg);
   }

   public List<Tuple3> setHeightBasedOnLowestLimbPoint() {
      List<Tuple3> points = new ArrayList<>();
      for (int i = 0; i <= 4; i++) {
         points.add(_pelvis.getChildLocationIn3DReferenceFrame(true/*rightFoot*/, i));
         points.add(_pelvis.getChildLocationIn3DReferenceFrame(false/*rightFoot*/, i));
         points.add(_torso.getChildLocationIn3DReferenceFrame(true/*rightArm*/, i));
         points.add(_torso.getChildLocationIn3DReferenceFrame(false/*rightArm*/, i));
      }
      float minY = 0;
      boolean firstItem = true;

      List<Tuple3> adjustedPoints = new ArrayList<>();
      for (Tuple3 point : points) {
         point = point.rotate(_tiltForward, 0f, 0f);
         point = point.rotate(0f, 0f, _tiltSide);
         point = point.rotate(0f, _twistOffset, 0f);

         if (firstItem || (point.getY() < minY)) {
            minY = point.getY();
         }
         firstItem = false;
         adjustedPoints.add(point);
      }
      _positionOffset = new Tuple3(_positionOffset.getX(), 2-minY, _positionOffset.getZ());
      points.clear();
      for (Tuple3 point : adjustedPoints) {
         point = point.add(_positionOffset);
         point = point.rotate(0f, _facingOffset, 0f);
         point = point.add(_locationOffset);
         points.add(point);
      }
      return points;
   }

   @Override
   public void getParts(List<TexturedObject> parts) {
      _pelvis.getParts(parts);
      _torso.getParts(parts);
   }

   public TexturedObject getModelHand(boolean rightSide) {
      return _torso.getModelHand(rightSide);
   }
   public TexturedObject getModelLeg(boolean rightSide) {
      return _pelvis.getModelLeg(rightSide);
   }
   public TexturedObject getModelHead() {
      return _torso.getModelHead();
   }
   public TexturedObject getModelTail() {
      return _pelvis.getModelTail();
   }
   public TexturedObject getModelWing(boolean rightSide) {
      return _torso.getModelWing(rightSide);
   }

   public void setFacing(int facing) {
      _facingOffset = (facing * -60f) + 180f;
   }

   public void setLocationOffset(Tuple3 center) {
      _locationOffset = new Tuple3(center.getX(), center.getY(), center.getZ());
      _positionOffset = new Tuple3(0, 0, 0);
      setHeightBasedOnLowestLimbPoint();
   }

   @Override
   protected Semaphore getChildLock() {
      return new Semaphore("BodyPart_Body_children", AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_BODY);
  }
}
package ostrowski.graphics;

import java.util.ArrayList;
import java.util.List;

public class AnimationSequence
{
   public String _name;
   public ArrayList<AnimationFrame> _frames = new ArrayList<>();
   public String _beginKeyFrame = "";
   public String _endKeyFrame = "";

   public void loadWalkingSequence() {
      _name = "Walking";
      ArrayList<String> frameData = new ArrayList<>();
      frameData.add("time=250^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:90.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:38.0,t:0.0,s:0.0|RightForeArm;f:80.0,t:90.0,s:0.0|LeftLeg;f:165.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:270.0,t:0.0,s:10.0|RightLowerLeg;f:90.0,t:0.0,s:0.0|RightFoot;f:28.0,t:0.0,s:0.0|LeftArm;f:270.0,t:0.0,s:10.0|RightLeg;f:180.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:150.0,t:0.0,s:0.0^feet=right");
      frameData.add("time=250^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:90.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:18.0,t:0.0,s:0.0|RightForeArm;f:65.0,t:90.0,s:0.0|LeftLeg;f:150.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:260.0,t:0.0,s:10.0|RightLowerLeg;f:90.0,t:0.0,s:0.0|RightFoot;f:18.0,t:0.0,s:0.0|LeftArm;f:280.0,t:0.0,s:10.0|RightLeg;f:195.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:150.0,t:0.0,s:0.0^feet=right");
      frameData.add("time=250^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:90.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:38.0,t:0.0,s:0.0|RightForeArm;f:65.0,t:90.0,s:0.0|LeftLeg;f:145.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:250.0,t:0.0,s:10.0|RightLowerLeg;f:100.0,t:0.0,s:0.0|RightFoot;f:18.0,t:0.0,s:0.0|LeftArm;f:285.0,t:0.0,s:10.0|RightLeg;f:200.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:120.0,t:0.0,s:0.0^feet=right");
      frameData.add("time=150^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:90.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:28.0,t:0.0,s:0.0|RightForeArm;f:75.0,t:90.0,s:0.0|LeftLeg;f:170.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:255.0,t:0.0,s:10.0|RightLowerLeg;f:125.0,t:0.0,s:0.0|RightFoot;f:48.0,t:0.0,s:0.0|LeftArm;f:280.0,t:0.0,s:10.0|RightLeg;f:195.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:100.0,t:0.0,s:0.0^feet=right");
      frameData.add("time=250^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:90.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:28.0,t:0.0,s:0.0|RightForeArm;f:85.0,t:90.0,s:0.0|LeftLeg;f:180.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:270.0,t:0.0,s:10.0|RightLowerLeg;f:150.0,t:0.0,s:0.0|RightFoot;f:28.0,t:0.0,s:0.0|LeftArm;f:270.0,t:0.0,s:10.0|RightLeg;f:175.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:90.0,t:0.0,s:0.0^feet=left");
      frameData.add("time=250^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:65.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:18.0,t:0.0,s:0.0|RightForeArm;f:90.0,t:90.0,s:0.0|LeftLeg;f:195.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:280.0,t:0.0,s:10.0|RightLowerLeg;f:150.0,t:0.0,s:0.0|RightFoot;f:18.0,t:0.0,s:0.0|LeftArm;f:260.0,t:0.0,s:10.0|RightLeg;f:150.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:90.0,t:0.0,s:0.0^feet=left");
      frameData.add("time=250^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:65.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:18.0,t:0.0,s:0.0|RightForeArm;f:90.0,t:90.0,s:0.0|LeftLeg;f:200.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:285.0,t:0.0,s:10.0|RightLowerLeg;f:120.0,t:0.0,s:0.0|RightFoot;f:38.0,t:0.0,s:0.0|LeftArm;f:250.0,t:0.0,s:10.0|RightLeg;f:145.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:100.0,t:0.0,s:0.0^feet=left");
      frameData.add("time=150^data=RightFingers;f:-20.0,t:0.0,s:0.0|LeftFingers;f:-20.0,t:0.0,s:0.0|Head;f:270.0,t:0.0,s:0.0|Core;t:0.0,h:0.0|LeftForeArm;f:75.0,t:90.0,s:0.0|LeftHand;f:90.0,t:0.0,s:0.0|RightToes;f:90.0,t:0.0,s:0.0|LeftFoot;f:48.0,t:0.0,s:0.0|RightForeArm;f:90.0,t:90.0,s:0.0|LeftLeg;f:195.0,t:0.0,s:0.0|RightHand;f:90.0,t:0.0,s:0.0|RightArm;f:280.0,t:0.0,s:10.0|RightLowerLeg;f:100.0,t:0.0,s:0.0|RightFoot;f:28.0,t:0.0,s:0.0|LeftArm;f:255.0,t:0.0,s:10.0|RightLeg;f:170.0,t:0.0,s:0.0|Torso;f:-90.0,t:0.0,s:0.0,h:0.0|LeftToes;f:90.0,t:0.0,s:0.0|LeftLowerLeg;f:125.0,t:0.0,s:0.0^feet=left");

      _frames.clear();
      for (String frameDatum : frameData) {
         addFrame(new AnimationFrame(frameDatum));
      }
   }

   public void addFrame(AnimationFrame frame) {
      _frames.add(frame);
   }

   public ArrayList<String> _positiveAttributes = new ArrayList<>();
   public ArrayList<String> _negativeAttributes = new ArrayList<>();

   public int score(List<String> positiveRequirement, int positiveWeight,
                    List<String> negativeRequirements, int negativeWeight) {
      int score = 0;
      for (String posReq : positiveRequirement) {
         if (_positiveAttributes.contains(posReq)) {
            score += positiveWeight;
         }
      }
      for (String negReq : negativeRequirements) {
         if (_negativeAttributes.contains(negReq)) {
            score -= negativeWeight;
         }
      }
      return score;
   }


}

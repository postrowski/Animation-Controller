package ostrowski.graphics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ostrowski.graphics.model.ObjLoader;
import ostrowski.graphics.objects3d.HumanBody;

public class SequenceLibrary
{
   static ArrayList<AnimationSequence>   _sequences = new ArrayList<>();
   public static HashMap<String, String> _keyFrames = new HashMap<>();

   public static AnimationSequence getAnimationSequenceByName(String race, String name) {
      for (AnimationSequence seq : _sequences) {
         if (seq._name.equalsIgnoreCase(name)) {
            return seq;
         }
      }
      return null;
   }

   static public ArrayList<String> _availableRaces = new ArrayList<>();
   static {
      _availableRaces.add("human");
      _availableRaces.add("minotaur");
      _availableRaces.add("cyclops");
      _availableRaces.add("wolf");
      try (InputStream in = ObjLoader.class.getClassLoader().getResourceAsStream("res/SequenceLibrary.anim")) {
         if (in != null) {
            try (InputStreamReader reader = new InputStreamReader(in)) {
               if (reader != null) {
                  try (BufferedReader infile = new BufferedReader(reader)) {
                     if (infile != null) {
                        loadSequencesFromFile(infile, "human");
                     }
                  }
               }
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public ArrayList<AnimationFrame> getSequence(List<String> positiveRequirements, List<String> negativeRequirements) {
      return null;
   }

   public AnimationSequence getFrame(List<String> positiveRequirements, List<String> negativeRequirements, String race) {
      int bestScore = -1000;
      AnimationSequence bestFrame = null;
      for (AnimationSequence seq : _sequences) {
         int frameScore = seq.score(positiveRequirements, 1, negativeRequirements, -10);
         if (frameScore > bestScore) {
            bestScore = frameScore;
            bestFrame = seq;
         }
      }
      return bestFrame;
   }

   private static void loadSequencesFromFile(BufferedReader input, String racename) {
      String lineData = "";
      AnimationSequence seq = null;
      _sequences.clear();
      //      _animationController.clearSequences();
      //      _humanController.clearKeyFrames();
      try {
         while (true) {
            lineData = input.readLine();
            if (lineData == null) {
               break;
            }
            if (lineData.trim().length() == 0) {
               continue;
            }
            if (lineData.startsWith("sequence:")) {
               if (seq != null) {
                  _sequences.add(seq);
               }
               seq = new AnimationSequence();
               seq._name = lineData.substring("sequence:".length());
            }
            else if (lineData.startsWith("keyStart:")) {
               seq._beginKeyFrame = lineData.substring("keyStart:".length());
            }
            else if (lineData.startsWith("keyEnd:")) {
               seq._endKeyFrame = lineData.substring("keyEnd:".length());
            }
            else if (lineData.startsWith("positiveAssociation:")) {
               seq._positiveAttributes.add(lineData.substring("positiveAssociation:".length()));
            }
            else if (lineData.startsWith("negativeAssociation:")) {
               seq._positiveAttributes.add(lineData.substring("negativeAssociation:".length()));
            }
            else if (lineData.startsWith("keyFrame:")) {
               String keyFrameNameData = lineData.substring("keyFrame:".length());
               int index = keyFrameNameData.indexOf('^');
               if (index != -1) {
                  String name = keyFrameNameData.substring(0, index);
                  String data = keyFrameNameData.substring(index + 1);
                  HumanBody human = new HumanBody(null, null, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, racename, true/*isMale*/);
                  human.setAngles(data);
                  _keyFrames.put(name, human.getAngles());
               }
            }
            else {
               AnimationFrame frame = new AnimationFrame(lineData);
               seq.addFrame(frame);
            }
         }
      } catch (IOException ioException) {
         // file is done
      }
      if (seq != null) {
         _sequences.add(seq);
      }
   }

   public static void saveFile(File fileIn, String racename) {
      File file = fileIn;
      if (file == null) {
         file = new File("C:\\eclipse\\workspaces\\personal\\Animation Controller\\src\\res\\SequenceLibrary.anim");
      }
      if (file.exists()) {
         file.delete();
      }
      try {
         file.createNewFile();
         if (file.exists() && file.canWrite()) {
            try (FileWriter fileWriter = new FileWriter(file)) {

               for (String key : _keyFrames.keySet()) {
                  String keyFrameData = _keyFrames.get(key);
                  fileWriter.append("keyFrame:").append(key).append("^").append(keyFrameData).append("\n");
               }
               for (AnimationSequence sequence : _sequences) {
                  fileWriter.append("\n");
                  fileWriter.append("sequence:").append(sequence._name).append("\n");
                  fileWriter.append("keyStart:").append(sequence._beginKeyFrame).append("\n");
                  fileWriter.append("keyEnd:").append(sequence._endKeyFrame).append("\n");
                  for (String assoc : sequence._positiveAttributes) {
                     fileWriter.append("positiveAssociation:").append(assoc).append("\n");
                  }
                  for (String assoc : sequence._negativeAttributes) {
                     fileWriter.append("negativeAssociation:").append(assoc).append("\n");
                  }
                  for (int i = 0; i < sequence._frames.size(); i++) {
                     fileWriter.append(sequence._frames.get(i).SerializeToString()).append("\n");
                  }
               }
            }
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }
}

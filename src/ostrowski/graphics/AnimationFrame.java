package ostrowski.graphics;

import java.util.StringTokenizer;

public class AnimationFrame
{
   public String  data;
   public long    timeInMilliseconds;
   public boolean leftFootPlanted  = false;
   public boolean rightFootPlanted = false;

   public AnimationFrame() {
   }

   public AnimationFrame(String serializableString) {
      StringTokenizer st = new StringTokenizer(serializableString, "^");
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         StringTokenizer st2 = new StringTokenizer(token, "=");
         if (st2.hasMoreTokens()) {
            String name = st2.nextToken();
            if (st2.hasMoreTokens()) {
               String value = st2.nextToken();
               try {
                  if (name.equalsIgnoreCase("time")) {
                     timeInMilliseconds = Long.parseLong(value);
                  }
                  else if (name.equalsIgnoreCase("data")) {
                     data = value;
                  }
                  else if (name.equalsIgnoreCase("feet")) {
                     if (value.equalsIgnoreCase("both")) {
                        leftFootPlanted = true;
                        rightFootPlanted = true;
                     }
                     else if (value.equalsIgnoreCase("left")) {
                        leftFootPlanted = true;
                     }
                     else if (value.equalsIgnoreCase("right")) {
                        rightFootPlanted = true;
                     }
                  }
               }
               catch (NumberFormatException ex) {
               }
            }
         }
      }
   }

   public AnimationFrame(AnimationFrame frame) {
      data = frame.data;
      leftFootPlanted = frame.leftFootPlanted;
      rightFootPlanted = frame.rightFootPlanted;
      timeInMilliseconds = frame.timeInMilliseconds;
   }

   public String SerializeToString() {
      StringBuilder sb = new StringBuilder();
      sb.append("time=").append(timeInMilliseconds);
      sb.append("^data=").append(data);
      sb.append("^feet=");
      if (leftFootPlanted) {
         if (rightFootPlanted) {
            sb.append("both");
         }
         else {
            sb.append("left");
         }
      }
      else
         if (rightFootPlanted) {
            sb.append("right");
         }
         else {
            sb.append("none");
         }
      return sb.toString();
   }
}

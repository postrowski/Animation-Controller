package ostrowski.graphics;

import java.util.StringTokenizer;

public class AnimationFrame
{
   public String _data;
   public long _timeInMilliseconds;
   public boolean _leftFootPlanted = false;
   public boolean _rightFootPlanted = false;

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
                     _timeInMilliseconds = Long.parseLong(value);
                  }
                  else if (name.equalsIgnoreCase("data")) {
                     _data = value;
                  }
                  else if (name.equalsIgnoreCase("feet")) {
                     if (value.equalsIgnoreCase("both")) {
                        _leftFootPlanted = true;
                        _rightFootPlanted = true;
                     }
                     else if (value.equalsIgnoreCase("left")) {
                        _leftFootPlanted = true;
                     }
                     else if (value.equalsIgnoreCase("right")) {
                        _rightFootPlanted = true;
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
      _data = frame._data;
      _leftFootPlanted = frame._leftFootPlanted;
      _rightFootPlanted = frame._rightFootPlanted;
      _timeInMilliseconds = frame._timeInMilliseconds;
   }

   public String SerializeToString() {
      StringBuilder sb = new StringBuilder();
      sb.append("time=").append(_timeInMilliseconds);
      sb.append("^data=").append(_data);
      sb.append("^feet=");
      if (_leftFootPlanted) {
         if (_rightFootPlanted) {
            sb.append("both");
         }
         else {
            sb.append("left");
         }
      }
      else
         if (_rightFootPlanted) {
            sb.append("right");
         }
         else {
            sb.append("none");
         }
      return sb.toString();
   }
}

package ostrowski.graphics.objects3d;

public class RangedValue implements Cloneable
{
   private float       value;
   private final float minValue;
   private final float maxValue;

   public RangedValue(float initialValue, float minValue, float maxValue) {
      value = initialValue;
      this.minValue = minValue;
      this.maxValue = maxValue;
   }
   public void validateRange() {
      if (value < minValue) {
         value = minValue;
      }
      else if (value > maxValue) {
         value = maxValue;
      }
   }
   public float getValue() {
      return value;
   }
   public void setValue(float value) {
      this.value = value;
      validateRange();
   }

   @Override
   public RangedValue clone() {
      return new RangedValue(value, minValue, maxValue);
   }
}

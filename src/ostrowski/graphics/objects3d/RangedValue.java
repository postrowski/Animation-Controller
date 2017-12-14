package ostrowski.graphics.objects3d;

public class RangedValue implements Cloneable
{
   private float _value;
   private float _minValue;
   private float _maxValue;

   public RangedValue(float initialValue, float minValue, float maxValue) {
      _value = initialValue;
      _minValue = minValue;
      _maxValue = maxValue;
   }
   public void validateRange() {
      if (_value < _minValue) {
         _value = _minValue;
      }
      else if (_value > _maxValue) {
         _value = _maxValue;
      }
   }
   public float getValue() {
      return _value;
   }
   public void setValue(float value) {
      _value = value;
      validateRange();
   }
   
   @Override
   public Object clone() {
      return new RangedValue(_value, _minValue, _maxValue);
   }
}

package ostrowski.graphics.objects3d;

import ostrowski.graphics.GLView;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.Semaphore;


public abstract class TwistBendJoint extends BallSocketJoint
{
   public TwistBendJoint(String name, String modelResourceName, boolean leftSideOfBody,
                         Texture texture, Texture selectedTexture, GLView glView,
                         float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super(name, modelResourceName, leftSideOfBody,
            texture, selectedTexture,
            glView, lengthFactor, widthFactor, raceName, isMale);
   }
   public void setAngles(float front, float twistCW) {
      frontRot.setValue(front);
      this.twistCW.setValue(twistCW);
      validateRanges();
   }
   @Override
   protected abstract Semaphore getChildLock();
}


package ostrowski.graphics.objects3d;

import ostrowski.graphics.GLView;
import ostrowski.graphics.texture.Texture;


public abstract class BendJoint extends TwistBendJoint
{
   public BendJoint(String name, String modelResourceName, boolean leftSideOfBody,
                    Texture texture, Texture selectedTexture,
                    GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super(name, modelResourceName, leftSideOfBody,
            texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
   }

   public void setAngles(float front) {
      _frontRot.setValue(front);
   }
}

package ostrowski.graphics.objects3d;

import java.io.IOException;

import ostrowski.graphics.GLView;
import ostrowski.graphics.model.ObjLoader;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.texture.Texture;

public class HairObject extends TexturedObject
{
   public HairObject(String name, String modelResourceName, boolean invertNormals,
                     Texture texture, Texture selectedTexture,
                     GLView glView, float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super(texture, selectedTexture, invertNormals);
      try {
         ObjModel model = ObjLoader.loadObj(modelResourceName, glView, lengthFactor, widthFactor);
         if (model != null) {
            //if (invertNormals) {
            //   if (!GLScene.USING_SCALING_FOR_LEFT_SIDE)
            //      model.reverseRightToLeft();
            //}
            addObject(model);
         }
      } catch (IOException e) {
      }

   }
}

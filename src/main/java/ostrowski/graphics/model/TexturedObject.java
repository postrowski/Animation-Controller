package ostrowski.graphics.model;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import ostrowski.graphics.GLView;
import ostrowski.graphics.texture.Texture;

public class TexturedObject
{
   public  Object         relatedObject;
   private boolean        selected        = false;
   public  Texture        texture         = null;
   public  Texture        selectedTexture = null;
   public  List<ObjModel> models          = new ArrayList<>();
   public  float          opacity         = 1f;
   public  boolean        invertNormals   = false;

   public TexturedObject() {
   }

   public TexturedObject(Texture texture, Texture selectedTexture, boolean invertNormals) {
      this.texture = texture;
      this.selectedTexture = selectedTexture;
      this.invertNormals = invertNormals;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public void addObject(ObjModel model) {
      if (invertNormals) {
         model.invertNormals();
      }
      if (model != null) models.add(model);
   }

   public void render(GLView glView, List<Message> messages) {
      bindToTexture();
      GL11.glEnable(GL11.GL_BLEND);
      if (opacity != 1f) GL11.glColor4f(1f, 1f, 1f, opacity);
      if (opacity == .65f) opacity = .65f;
      for (ObjModel model : models) {
         model.render(glView, messages);
      }
      if (opacity != 1f) GL11.glColor4f(1f, 1f, 1f, 1f);
      GL11.glDisable(GL11.GL_BLEND);
   }

   protected void bindToTexture() {
      if (selected && (selectedTexture != null))
         selectedTexture.bind();
      else
         texture.bind();
   }

   public Tuple3 getScreenPosition3dContainingScreenPoint(Tuple2 invertedScreenLoc, FloatBuffer modelMatrix, FloatBuffer projMatrix, IntBuffer viewport) {
      Tuple3 lowestZ = null;
      for (ObjModel model : models) {
         Tuple3 z = model.getScreenPosition3dContainingScreenPoint(invertedScreenLoc, modelMatrix, projMatrix, viewport);
         if (z != null) {
            if (z.getZ() > 0) {
               if ((lowestZ == null) || (z.getZ() < lowestZ.getZ())) lowestZ = z;
            }
         }
      }
      return lowestZ;
   }

   public Tuple3 getObjectCenter() {
      Tuple3 averageLoc = new Tuple3(0, 0, 0);
      for (ObjModel model : models) {
         averageLoc = averageLoc.add(model.getObjectCenter());
      }
      return averageLoc.divide(models.size());
   }

   public Tuple3 getObjectBoundingCubeDimensions() {
      Tuple3 min = null;
      Tuple3 max = null;
      for (ObjModel model : models) {
         min = model.getObjectBoundingCubeMinPoints(min);
         max = model.getObjectBoundingCubeMaxPoints(max);
      }
      return max.subtract(min);
   }

   public String getName() {
      return null;
   }
}

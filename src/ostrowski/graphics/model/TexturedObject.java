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
   public Object              _relatedObject;
   private boolean            _selected        = false;
   public Texture             _texture         = null;
   public Texture             _selectedTexture = null;
   public List<ObjModel> _models          = new ArrayList<>();
   public float               _opacity         = 1f;
   public boolean             _invertNormals   = false;

   public TexturedObject() {
   }

   public TexturedObject(Texture texture, Texture selectedTexture, boolean invertNormals) {
      _texture = texture;
      _selectedTexture = selectedTexture;
      _invertNormals = invertNormals;
   }

   public void setSelected(boolean selected) {
      _selected = selected;
   }

   public void addObject(ObjModel model) {
      if (_invertNormals) {
         model.invertNormals();
      }
      if (model != null) _models.add(model);
   }

   public void render(GLView glView, List<Message> messages) {
      bindToTexture();
      GL11.glEnable(GL11.GL_BLEND);
      if (_opacity != 1f) GL11.glColor4f(1f, 1f, 1f, _opacity);
      if (_opacity == .65f) _opacity = .65f;
      for (ObjModel model : _models) {
         model.render(glView, messages);
      }
      if (_opacity != 1f) GL11.glColor4f(1f, 1f, 1f, 1f);
      GL11.glDisable(GL11.GL_BLEND);
   }

   protected void bindToTexture() {
      if (_selected && (_selectedTexture != null))
         _selectedTexture.bind();
      else
         _texture.bind();
   }

   public Tuple3 getScreenPosition3dContainingScreenPoint(Tuple2 invertedScreenLoc, FloatBuffer modelMatrix, FloatBuffer projMatrix, IntBuffer viewport) {
      Tuple3 lowestZ = null;
      for (ObjModel model : _models) {
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
      for (ObjModel model : _models) {
         averageLoc = averageLoc.add(model.getObjectCenter());
      }
      return averageLoc.divide(_models.size());
   }

   public Tuple3 getObjectBoundingCubeDimensions() {
      Tuple3 min = null;
      Tuple3 max = null;
      for (ObjModel model : _models) {
         min = model.getObjectBoundingCubeMinPoints(min);
         max = model.getObjectBoundingCubeMaxPoints(max);
      }
      return max.subtract(min);
   }

   public String getName() {
      return null;
   }
}

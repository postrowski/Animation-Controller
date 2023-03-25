package ostrowski.graphics.texture;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;

/**
 * A simple implementation of fonts in OpenGL. All this actually
 * does is draw a quad textured with the image.
 * 
 * @author Paul Ostrowski
 */
public class ButtonImage
{

   /** The texture containing the font characters */
   private Texture   texture;
   private Rectangle bounds;
   private Rectangle textureBounds;

   /**
    * Create a new font based on specific texture cut up into a specific
    * collection of characters
    * 
    * @param texture The texture containing the characters
    */
   public ButtonImage(Texture texture, Rectangle textureBounds) {
      this.texture = texture;
      this.textureBounds = textureBounds;
   }

   /**
    * Draw an image to the screen as a quad textured with the image.
    * 
    * @param x The upper-left x coordinate to draw the image at (in pixels)
    * @param y The upper-left y coordinate to draw the image at (in pixels)
    */
   public void drawImage(int x, int y) {
      bounds = new Rectangle(x, y, this.getWidth(), this.getHeight());
      // bind the texture so we can render a quad with the image on it
      texture.bind();

      // turn blending on so the image is displayed above the scene
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      // draw a quad to the screen mapped to the right part of the texture
      GL11.glBegin(GL11.GL_QUADS);
      {
         float textureWidth = texture.getImageWidth();
         float textureHeight = texture.getImageHeight();

         float textureLeft = (textureBounds.getX()) / textureWidth;
         float textureBottom = textureHeight - (textureBounds.getY()) / textureHeight;
         float textureRight = (textureBounds.getX() + textureBounds.getWidth()) / textureWidth;
         float textureTop = textureHeight - (textureBounds.getY() + textureBounds.getHeight()) / textureHeight;

         // setup the quad
         GL11.glTexCoord2f(textureLeft, textureBottom);
         GL11.glVertex2i(x, y);

         GL11.glTexCoord2f(textureLeft, textureTop);
         GL11.glVertex2i(x, y + this.getHeight());

         GL11.glTexCoord2f(textureRight, textureTop);
         GL11.glVertex2i(x + this.getWidth(), y + this.getHeight());

         GL11.glTexCoord2f(textureRight, textureBottom);
         GL11.glVertex2i(x + this.getWidth(), y);
      }
      GL11.glEnd();

      // reset the blending
      GL11.glDisable(GL11.GL_BLEND);
   }

   public int getWidth() {
      return textureBounds.getWidth();
   }

   public int getHeight() {
      return textureBounds.getHeight();
   }

   public boolean containsPoint(int x, int y) {
      return (bounds != null) && bounds.contains(x, y);
   }
}

package ostrowski.graphics.texture;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.lwjgl.opengl.GL11;

/**
 * A simple implementation of fonts in OpenGL. All this actually
 * does it draw a bunch of quad's when the characters for the string
 * should be. It then textures those quads with a specially created
 * texture containing all the characters from a font. The texture
 * coordinates are chosen so that the appropriate bit of the texture
 * is shown on each quad for the characters.
 *
 * There's a really good tool for building textures containing fonts
 * called Bitmap Font Builder (http://www.lmnopc.com/bitmapfontbuilder/)
 *
 * The tool actually produces textures containing two textures, one at the
 * bottom and one at the top, which is what this class is based on
 *
 */
public class BitmapFont {
	/** The number of characters across the texture */
	private final int _charactersAcross;

	/** The width of each character in pixels */
	private final int _characterWidth;
	/** The height of each character in pixels */
	private final int _characterHeight;

	/** The width of each character in terms of texture coordinates */
	private final float _characterWidthInTexture;
	/** The height of each character in terms of the texture coordinates */
	private final float _characterHeightInTexture;

	/** The texture containing the font characters */
	private final Texture _texture;

	/**
	 * Create a new font based on specific texture cut up into a specific
	 * collection of characters
	 *
	 * @param texture The texture containing the characters
	 * @param characterWidth The width of the characters on the sheet (in pixels)
	 * @param characterHeight The height of the characters on the sheet (in pixels)
	 */
	public BitmapFont(Texture texture, int characterWidth, int characterHeight) {
		this._texture = texture;
		this._characterWidth = characterWidth;
		this._characterHeight = characterHeight;

		// calculate how much of the texture is taken up with each character
		// by working out the proportion of the texture size that the character
		// size in pixels takes up
		_characterWidthInTexture = texture.getWidth() / (texture.getImageWidth() / characterWidth);
		_characterHeightInTexture = texture.getHeight() / (texture.getImageHeight() / characterHeight);

		// work out the number of characters that fit across the sheet
		_charactersAcross = texture.getImageWidth() / characterWidth;
			}

	/**
	 * Draw a string to the screen as a set of quads textured in the
	 * appropriate way to show the string.
	 *
	 * @param font The index of the font to draw. 0 means the font
	 * at the top, 1 the font at the bottom.
	 * @param text The text to be draw to the screen.
	 * @param x The x coordinate to draw the text at (int pixels)
	 * @param y The y coordinate to draw the text at (in pixels)
	 */
   public void drawString(int font, RGB color, float opacity, String text, int x, int y, boolean centerOnCoords) {
      Rectangle bounds;
      if (centerOnCoords) {
         int newX = x - ((text.length() * (_characterWidth-12)) / 2);
         int newY = y - (_characterHeight / 2);
         bounds = new Rectangle(newX, newY, (text.length() * _characterWidth), _characterHeight);
      }
      else {
         bounds = new Rectangle(x, y, (text.length() * _characterWidth), _characterHeight);
      }
      drawString(font, color, opacity, text, bounds );
   }
	public void drawString(int font, RGB color, float opacity, String text, Rectangle bounds) {
		// bind the font text so we can render quads with the characters on
		_texture.bind();

		// turn blending on so characters are displayed above the scene
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, opacity);
		// cycle through each character drawing a quad to the screen
		// mapped to the right part of the texture
		GL11.glBegin(GL11.GL_QUADS);

		int x = bounds.x;
		int y = bounds.y;
		int characterHeight = bounds.height;
		int characterWidth = bounds.width / text.length();
		int characterStep = characterWidth-12;

		//$GL11.glPushAttrib(GL11.GL_CURRENT_BIT);
		//$GL11.glColor3b((byte)255, (byte)0, (byte)0);
		for (int i=0;i<text.length();i++) {
			// get the index of the character based on the font starting
			// with the space character
			int c = text.charAt(i) - ' ';

			// work out the u,v texture mapping coordinates based on the
			// index of the character and the amount of texture per
			// character
			float u = ((c % _charactersAcross) * _characterWidthInTexture);
			float v = 1 - ((c / _charactersAcross) * _characterHeightInTexture);
			v -= font * 0.5f;

			// setup the quad
			GL11.glTexCoord2f(u, v);
			GL11.glVertex2i(x+(i*characterStep), y);

			GL11.glTexCoord2f(u, v - _characterHeightInTexture);
         GL11.glVertex2i(x+(i*characterStep), y+characterHeight);

			GL11.glTexCoord2f(u + _characterWidthInTexture, v - _characterHeightInTexture);
			GL11.glVertex2i(x+(i*characterStep)+characterWidth, y+characterHeight);

			GL11.glTexCoord2f(u + _characterWidthInTexture, v);
			GL11.glVertex2i(x+(i*characterStep)+characterWidth, y);
		}
      //$GL11.glPopAttrib(); // This sets the colour back to its original value

		GL11.glEnd();
		GL11.glColor4f(1f, 1f, 1f, 1f);

		// reset the blending
		GL11.glDisable(GL11.GL_BLEND);
	}
}

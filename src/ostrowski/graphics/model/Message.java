package ostrowski.graphics.model;

import org.eclipse.swt.graphics.RGB;

public class Message
{
   public int     _font       = 0;
   public int     _xLoc       = 0;
   public int     _yLoc       = 0;
   public int     _zLoc       = 0;
   public boolean _centerText = true;
   public String  _text       = "";
   public boolean _visible    = true;
   public RGB     _colorRGB   = new RGB(255, 255, 255);
   public float   _opacity    = 1f;
}

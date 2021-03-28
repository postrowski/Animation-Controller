package ostrowski.graphics.model;

import org.eclipse.swt.graphics.RGB;

public class Message
{
   public int     font       = 0;
   public int     xLoc       = 0;
   public int     yLoc       = 0;
   public int     zLoc       = 0;
   public boolean centerText = true;
   public String  text       = "";
   public boolean visible    = true;
   public RGB     colorRGB   = new RGB(255, 255, 255);
   public float   opacity    = 1f;
}

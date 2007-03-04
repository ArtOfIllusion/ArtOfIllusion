/* Copyright (C) 1999,2000,2003,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import buoy.widget.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;

/** The RGBColor class is used to represent a color.  This class uses floating point numbers
    (usually between 0 and 1) to store the red, green, and blue components, in contrast to java.awt.Color
    which uses integers. */

public class RGBColor
{
  public float red, green, blue;

  /* The following constants are used for converting to and from the ERGB representation. */

  private static final float ERGB_SCALE = 1.0f/255.0f;
  private static final float INVLOG2 = 1.0f/(float) Math.log(2.0);
  public static final float ERGB_EXP_SCALE[];
  
  static
  {
    // Construct the scale arrays for the exponents.
    
    ERGB_EXP_SCALE = new float [256];
    for (int i = 0; i < 256; i++)
      ERGB_EXP_SCALE[i] = (float) (ERGB_SCALE*FastMath.pow(2.0, i-128));
  }

  /** Construct a new RGBColor object with components 0,0,0. */
  
  public RGBColor()
  {
  }
  
  /** Construct a new RGBColor object with components r,g,b. */

  public RGBColor(float r, float g, float b)
  {
    setRGB(r, g, b);
  }

  /** Construct a new RGBColor object with components r,g,b. */

  public RGBColor(double r, double g, double b)
  {
    setRGB(r, g, b);
  }

  /** Set the red, green, and blue components to the specified values. */

  public final void setRGB(float r, float g, float b)
  {
    red = r;
    green = g;
    blue = b;
  }

  /** Set the red, green, and blue components to the specified values. */

  public final void setRGB(double r, double g, double b)
  {
    red = (float) r;
    green = (float) g;
    blue = (float) b;
  }

  /** Get the value of the red component. */

  public final float getRed()
  {
    return red;
  }

  /** Get the value of the green component. */

  public final float getGreen()
  {
    return green;
  }

  /** Get the value of the blue component. */

  public final float getBlue()
  {
    return blue;
  }
  
  /** Determine whether two colors are identical. */
  
  public boolean equals(Object c)
  {
    if (c instanceof RGBColor)
    {
      RGBColor col = (RGBColor) c;
      return (col.red == red && col.green == green && col.blue == blue);
    }
    return false;
  }

  public int hashCode()
  {
    return Float.floatToIntBits(red+10*green+100*blue);
  }

  /** Create an exact duplicate of this object. */
  
  public final RGBColor duplicate()
  {
    return new RGBColor(red, green, blue);
  }

  /** Make this object identical to another one. */
  
  public final void copy(RGBColor color)
  {
    setRGB(color.red, color.green, color.blue);
  }
  
  /** Get a java.awt.Color object representing this color. */
  
  public final Color getColor()
  {
    float r = red, g = green, b = blue;
    if (r < 0.0f) r = 0.0f;
    if (r > 1.0f) r = 1.0f;
    if (g < 0.0f) g = 0.0f;
    if (g > 1.0f) g = 1.0f;
    if (b < 0.0f) b = 0.0f;
    if (b > 1.0f) b = 1.0f;
    return new Color(r, g, b);
  }

  /** Get a Widget which displays this color. */

  public Widget getSample(int width, int height)
  {
    final CustomWidget w = new CustomWidget();
    w.setPreferredSize(new Dimension(width, height));
    w.setMaximumSize(new Dimension(width, height));
    w.setBackground(getColor());
    BOutline outline = new BOutline(w, BorderFactory.createLoweredBevelBorder()) {
      public void setBackground(Color c)
      {
        w.setBackground(c);
      }
    };
    return outline;
  }
  
  /** Get a representation of this color in the default Java color model. */
  
  public final int getARGB()
  {
    int r, g, b;
    r = (int) (red*255.0);
    g = (int) (green*255.0);
    b = (int) (blue*255.0);
    if (r < 0) r = 0;
    if (r > 255) r = 255;
    if (g < 0) g = 0;
    if (g > 255) g = 255;
    if (b < 0) b = 0;
    if (b > 255) b = 255;
    return 0xFF000000 + (r<<16) + (g<<8) + b;
  }
  
  /** Set the color based on a 32 bit ARGB value (the default Java color model). */
  
  public final void setARGB(int color)
  {
    int r, g, b;
    
    r = (color & 0x00FF0000) >> 16;
    g = (color & 0x0000FF00) >> 8;
    b = color & 0x000000FF;
    setRGB(r/255.0f, g/255.0f, b/255.0f);
  }
  
  /** Add another color to this one. */
  
  public final void add(RGBColor color)
  {
    red += color.red;
    green += color.green;
    blue += color.blue;
  }
  
  /** Subtract another color from this one. */
  
  public final void subtract(RGBColor color)
  {
    red -= color.red;
    green -= color.green;
    blue -= color.blue;
  }
  
  /** Multiply this color by another one. */
  
  public final void multiply(RGBColor color)
  {
    red *= color.red;
    green *= color.green;
    blue *= color.blue;
  }
  
  /** Add the specified values to the components of this color. */
  
  public final void add(float r, float g, float b)
  {
    red += r;
    green += g;
    blue += b;
  }
  
  /** Subtract the specified values from the components of this color. */

  public final void subtract(float r, float g, float b)
  {
    red -= r;
    green -= g;
    blue -= b;
  }
  
  /** Multiply the components of this color by the specified values. */

  public final void multiply(float r, float g, float b)
  {
    red *= r;
    green *= g;
    blue *= b;
  }
  
  /** Scale each component of this color by the specified amount. */
  
  public final void scale(float s)
  {
    red *= s;
    green *= s;
    blue *= s;
  }
  
  /** Scale each component of this color by the specified amount. */
  
  public final void scale(double s)
  {
    float f = (float) s;
    
    red *= f;
    green *= f;
    blue *= f;
  }
  
  /** Clip the components of this color so they lie between 0 and 1. */
  
  public final void clip()
  {
    if (red < 0.0f)
      red = 0.0f;
    if (red > 1.0f)
      red = 1.0f;
    if (green < 0.0f)
      green = 0.0f;
    if (green > 1.0f)
      green = 1.0f;
    if (blue < 0.0f)
      blue = 0.0f;
    if (blue > 1.0f)
      blue = 1.0f;
  }
  
  /** Get the perceptual brightness of this color. */
  
  public final float getBrightness()
  {
    return 0.2125f*red + 0.7154f*green + 0.0721f*blue;
  }
  
  /** Get the maximum value which any of the color components has. */
  
  public final float getMaxComponent()
  {
    float max = (green > red ? green : red);
    if (blue > max)
      max = blue;
    return max;
  }
  
  /** Set this color based on values in the HSV color model.  This routine is 
      based on sample code given in "Computer Graphics: Principles and Practice, 2nd Edition", 
      by Foley, van Dam, Feiner and Hughes, 1997. */
  
  public final void setHSV(float h, float s, float v)
  {
    float f, p, q, t;
    int i;

    if (s == 0.0f)
      {
	setRGB(v, v, v);
	return;
      }
    if (h == 360.0f)
      h = 0.0f;
    else
      h /= 60.0f;
    i = (int) h;
    f = h - (float) i;
    p = v * (1.0f - s);
    q = v * (1.0f - s*f);
    t = v * (1.0f - (s*(1.0f-f)));
    switch (i)
      {
	case 0:
	  setRGB(v, t, p);
	  break;
	case 1:
	  setRGB(q, v, p);
	  break;
	case 2:
	  setRGB(p, v, t);
	  break;
	case 3:
	  setRGB(p, q, v);
	  break;
	case 4:
	  setRGB(t, p, v);
	  break;
	case 5:
	  setRGB(v, p, q);
	  break;
      }
  }
  
  /** Get a representation of this color in the HSV color model.  This routine is 
      based on sample code given in "Computer Graphics: Principles and Practice, 2nd Edition", 
      by Foley, van Dam, Feiner and Hughes, 1997. */

  public final float[] getHSV()
  {
    float max, min, h;
    
    max = Math.max(red, Math.max(blue, green));
    min = Math.min(red, Math.min(blue, green));
    if (max == min)
      return new float [] {0.0f, 0.0f, max};
    if (red == max)
      h = (green-blue) / (max-min);
    else if (green == max)
      h = 2.0f + (blue-red) / (max-min);
    else
      h = 4.0f + (red-green) / (max-min);
    h *= 60.0f;
    if (h < 0.0f)
      h += 360.0f;
    return new float [] {h, (max-min)/max, max};
  }
  
  /** Set this color based on values in the HLS color model.  This routine is 
      based on sample code given in "Computer Graphics: Principles and Practice, 2nd Edition", 
      by Foley, van Dam, Feiner and Hughes, 1997.  (A bug in their HLS to RGB routine has
      been corrected.) */

  public final void setHLS(float h, float l, float s)
  {
    float m1, m2;
    
    m2 = (l<=0.5f) ? (l+l*s) : (l+s-l*s);
    m1 = 2.0f*l - m2;
    if (s == 0.0f)
      setRGB(l, l, l);
    else
      setRGB(value(m1, m2, h+120.0f), value(m1, m2, h), value(m1, m2, h-120.0f));
  }

  private static final float value(float n1, float n2, float hue)
  {
    if (hue > 360.0f)
      hue -= 360.0f;
    else if (hue < 0.0f)
      hue += 360.0f;
    if (hue < 60.0f)
      return n1 + (n2-n1)*hue/60.0f;
    else if (hue < 180.0f)
      return n2;
    else if (hue < 240.0f)
      return n1 + (n2-n1)*(240.0f-hue)/60.0f;
    else
      return n1;
  }

  /** Get a representation of this color in the HLS color model.  This routine is 
      based on sample code given in "Computer Graphics: Principles and Practice, 2nd Edition", 
      by Foley, van Dam, Feiner and Hughes, 1997. */

  public final float[] getHLS()
  {
    float max, min, h, l, delta;
    
    max = Math.max(red, Math.max(blue, green));
    min = Math.min(red, Math.min(blue, green));
    if (max == min)
      return new float [] {0.0f, max, 0.0f};
    l = (max+min)/2.0f;
    delta = max - min;
    if (red == max)
      h = (green-blue) / delta;
    else if (green == max)
      h = 2.0f + (blue-red) / delta;
    else
      h = 4.0f + (red-green) / delta;
    h *= 60.0f;
    if (h < 0.0f)
      h += 360.0f;
    if (l < 0.5f)
      return new float [] {h, l, delta/(max+min)};
    else
      return new float [] {h, l, delta/(2.0f-max-min)};
  }
  
  /** Calculate the ERGB representation of this color.  */

  public final int getERGB()
  {
    float max = red;
    if (green > max)
      max = green;
    if (blue > max)
      max = blue;
    if (max*ERGB_SCALE < ERGB_EXP_SCALE[0])
      return 0;
    int exp = 128+FastMath.ceil(Math.log(max)*INVLOG2);
    float scale = 1.0f/ERGB_EXP_SCALE[exp];
    byte e = (byte) exp, r = (byte) FastMath.round(red*scale), g = (byte) FastMath.round(green*scale), b = (byte) FastMath.round(blue*scale);
    return ((e&0xFF)<<24) + ((r&0xFF)<<16) + ((g&0xFF)<<8) + (b&0xFF);
  }
  
  /** Set this color based on its representation in Greg Ward's ERGB format. */
  
  public final void setERGB(int ergb)
  {
    float scale = ERGB_EXP_SCALE[(ergb>>24)&0xFF];
    setRGB(((ergb>>16)&0xFF)*scale, ((ergb>>8)&0xFF)*scale, (ergb&0xFF)*scale);
  }
  
  /** Set this color based on its representation in Greg Ward's ERGB format. */
  
  public final void setERGB(byte r, byte g, byte b, byte e)
  {
    float scale = ERGB_EXP_SCALE[e&0xFF];
    setRGB((r&0xFF)*scale, (g&0xFF)*scale, (b&0xFF)*scale);
  }

    /*DMT 15 Aug 2001 */ 

  /** Get a representation of this color in the YCrCb color model. */

  public final float[] getYCrCb() 
  { 
      float Y = (0.257f * red) + (0.504f * green) + (0.098f * blue) + 0.0625f; 
      float Cr = (0.439f * red) - (0.368f * green) - (0.071f * blue) + 0.5f; 
      float Cb = +-(0.148f * red) - (0.291f * green) + (0.439f * blue) + 0.5f; 
 
      Y = Math.max (0, Math.min (1, Y)); 
      Cr = Math.max (0, Math.min (1, Cr)); 
      Cb = Math.max (0, Math.min (1, Cb)); 
      return new float [] {Y, Cr, Cb}; 
 
  } 

  /** Set this color based on values in the YCrCb color model. */

  public final void setYCrCb (float Y, float Cr, float Cb)
  {
      Y =  1.164f * (Y - 0.0625f);
      Cr -= 0.5f;
      Cb -= 0.5f;
      red = Y + 1.596f * Cr;
      green = Y - 0.813f * Cr - 0.391f * Cb;
      blue = Y + 2.018f * Cb;

      red = Math.max (0, Math.min (1, red));
      green = Math.max (0, Math.min (1, green));
      blue = Math.max (0, Math.min (1, blue));
 
  } 

  /** Reconstruct an RGBColor based on its serialized representation. */
  
  public RGBColor(DataInputStream in) throws IOException
  {
    red = in.readFloat();
    green = in.readFloat();
    blue = in.readFloat();
  }
  
  /** Serialize this object to an output stream. */

  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeFloat(red);
    out.writeFloat(green);
    out.writeFloat(blue);
  }

  /** Create a string describing the color. */

  public String toString()
  {
    return "RGBColor: " + red + ", " + green + ", " + blue;
  }
}
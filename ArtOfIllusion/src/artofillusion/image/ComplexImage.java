/* Copyright (C) 2003-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import java.awt.*;
import java.awt.image.*;

/** This class stores an image, with optional additional floating point values for each pixel.
    It is intended to be extensible, so that as features are added to the renderers, the amount
    of information available for each pixel can grow. */

public class ComplexImage
{
  private Image img;
  private float pixelData[][];
  private int intImage[];
  private int width, height;
  
  public static final int BLUE = 1;
  public static final int GREEN = 2;
  public static final int RED = 4;
  public static final int ALPHA = 8;
  public static final int DEPTH = 16;
  public static final int OBJECT = 32;
  public static final int NOISE = 64;

  /** Construct a ComplexImage which wraps an Image object. */
  
  public ComplexImage(Image image)
  {
    img = image;
    width = image.getWidth(null);
    height = image.getHeight(null);
    pixelData = new float [7][];
  }

  /** Set the floating point values of a particular component for each pixel.  The length of the value array
      should be equal to the number of pixels in the image, and the values should be ordered by rows. */
  
  public void setComponentValues(int component, float values[])
  {
    pixelData[getComponentIndex(component)] = values;
  }
  
  /** Get the width of the image. */
  
  public int getWidth()
  {
    return width;
  }
  
  /** Get the height of the image. */
  
  public int getHeight()
  {
    return height;
  }
  
  /** Get the Image object. */
  
  public Image getImage()
  {
    return img;
  }
  
  /** Determine whether floating point data is available for a particular component. */
  
  public boolean hasFloatData(int component)
  {
    return (pixelData[getComponentIndex(component)] != null);
  }
  
  /** Get the floating point value of a component for a pixel. */
  
  public float getPixelComponent(int x, int y, int component)
  {
    int index = getComponentIndex(component);
    if (pixelData[index] != null)
      return pixelData[index][x+y*width];
    if (intImage == null)
    {
      try
      {
        PixelGrabber pg = new PixelGrabber(img, 0, 0, -1, -1, true);
        pg.grabPixels();
        intImage = (int []) pg.getPixels();
      }
      catch (InterruptedException ex)
      {
        ex.printStackTrace();
      }
    }
    return ((intImage[x+y*width]>>(index*8))&0xFF)*(1.0f/255.0f);
  }
  
  /** Create a duplicate of this object.  The new ComplexImage will refer to the same Image object as the
      other, but all other fields will be cloned. */
  
  public ComplexImage duplicate()
  {
    ComplexImage ci = new ComplexImage(img);
    ci.intImage = intImage;
    for (int i = 0; i < pixelData.length; i++)
      if (pixelData[i] != null)
      {
        ci.pixelData[i] = new float [pixelData[i].length];
        System.arraycopy(pixelData[i], 0, ci.pixelData[i], 0, pixelData[i].length);
      }
    return ci;
  }
  
  /** Rebuild the Image from the floating point components.  This should be called after the components have
      been modified. */
  
  public void rebuildImage()
  {
    int newimage[] = new int [width*height];
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
      {
        int red = floatToInt(getPixelComponent(i, j, RED));
        int green = floatToInt(getPixelComponent(i, j, GREEN));
        int blue = floatToInt(getPixelComponent(i, j, BLUE));
        int alpha = floatToInt(getPixelComponent(i, j, ALPHA));
        newimage[i+j*width] = (alpha<<24) + (red<<16) + (green<<8) + blue;
      }
    intImage = newimage;
    MemoryImageSource imageSource = new MemoryImageSource(width, height, intImage, 0, width);
    img = Toolkit.getDefaultToolkit().createImage(imageSource);
  }

  /** Given one of the constants for a component, determine the corresponding array index. */

  private int getComponentIndex(int component)
  {
    int index = 0;
    while (((component>>index)&1) == 0)
      index++;
    return index;
  }
  
  /** Convert a floating point value to an appropriate int value.  This is used by rebuildImage(). */
  
  private int floatToInt(float f)
  {
    int i = (int) (f*255.0f);
    if (i < 0)
      i = 0;
    if (i > 255)
      i = 255;
    return i;
  }
}
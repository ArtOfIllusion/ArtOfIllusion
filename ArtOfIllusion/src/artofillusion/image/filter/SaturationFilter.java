/* Copyright (C) 2003-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image.filter;

import artofillusion.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import java.io.*;

/** This is an image filter which adjusts the saturation of an image. */

public class SaturationFilter extends ImageFilter
{
  public SaturationFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("Saturation");
  }
  
  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */
  
  public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    int width = image.getWidth(), height = image.getHeight();
    float saturation = (float) paramValue[0];
    float red[] = new float [width*height];
    float green[] = new float [width*height];
    float blue[] = new float [width*height];
    RGBColor color = new RGBColor();
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
        {
          float r = image.getPixelComponent(i, j, ComplexImage.RED);
          float g = image.getPixelComponent(i, j, ComplexImage.GREEN);
          float b = image.getPixelComponent(i, j, ComplexImage.BLUE);
          color.setRGB(r, g, b);
          float brightness = color.getBrightness();
          r = brightness + (r-brightness)*saturation;
          g = brightness + (g-brightness)*saturation;
          b = brightness + (b-brightness)*saturation;
          if (r < 0.0f) r = 0.0f;
          if (g < 0.0f) g = 0.0f;
          if (b < 0.0f) b = 0.0f;
          red[i+j*width] = r;
          green[i+j*width] = g;
          blue[i+j*width] = b;
        }
    image.setComponentValues(ComplexImage.RED, red);
    image.setComponentValues(ComplexImage.GREEN, green);
    image.setComponentValues(ComplexImage.BLUE, blue);
  }
  
  /** Get a list of parameters which affect the behavior of the filter. */
  
  public TextureParameter [] getParameters()
  {
    return new TextureParameter [] {new TextureParameter(this, getName(), 0.0, Double.MAX_VALUE, 1.0)};
  }

  @Override
  public Property[] getProperties()
  {
    return new Property [] {new Property(getName(), 0.0, Double.MAX_VALUE, 1.0)};
  }

  /** Write a serialized description of this filter to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble((Double) getPropertyValue(0));
  }

  /** Reconstruct this filter from its serialized representation. */
  
  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    setPropertyValue(0, in.readDouble());
  }
}
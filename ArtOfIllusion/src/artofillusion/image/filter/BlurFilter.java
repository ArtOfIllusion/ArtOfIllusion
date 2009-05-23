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

/** This is an image filter which blurs an image. */

public class BlurFilter extends ImageFilter
{
  public BlurFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("Blur");
  }
  
  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */
  
  public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    int radius = (int) (0.5f*(Double) getPropertyValue(0)*image.getHeight());
    if (radius < 1)
      return;
    float mask[] = createMask(radius);
    filterComponent(image, ComplexImage.RED, radius, mask);
    filterComponent(image, ComplexImage.GREEN, radius, mask);
    filterComponent(image, ComplexImage.BLUE, radius, mask);
  }
  
  /** Apply the filter to one component of an image. */
  
  private void filterComponent(ComplexImage image, int component, int radius, float mask[])
  {
    Thread currentThread = Thread.currentThread();
    int maskWidth = 2*radius+1;
    int width = image.getWidth(), height = image.getHeight();
    float blur[] = new float [width*height];
    for (int i = 0; i < width; i++)
    {
      if (currentThread.isInterrupted())
        return;
      for (int j = 0; j < height; j++)
      {
        float value = image.getPixelComponent(i, j, component);
        if (value == 0.0f)
          continue;
        int basex = i-radius, basey = j-radius;
        int xstart = (basex < 0 ? -basex : 0);
        int ystart = (basey < 0 ? -basey : 0);
        int xend = (basex+maskWidth >= width ? width-basex : maskWidth);
        int yend = (basey+maskWidth >= height ? height-basey : maskWidth);
        for (int y = ystart; y < yend; y++)
        {
          int maskBase = y*maskWidth;
          int imageBase = basex+(basey+y)*width;
          for (int x = xstart; x < xend; x++)
            blur[imageBase+x] += mask[maskBase+x]*value;
        }
      }
    }
    image.setComponentValues(component, blur);
  }
  
  /** Build the mask. */
  
  private float [] createMask(int radius)
  {
    int size = 2*radius+1, radius2 = radius*radius;
    float mask[] = new float [size*size];
    for (int i = 0; i < radius; i++)
      for (int j = 0; j < radius; j++)
        {
          int dist2 = i*i+j*j;
          if (dist2 > radius2)
            continue;
          float d = dist2/(float) radius2;
          float value = d*(d-2.0f)+1.0f;
          mask[(radius-i)+(radius-j)*size] = value;
          mask[(radius+i)+(radius-j)*size] = value;
          mask[(radius-i)+(radius+j)*size] = value;
          mask[(radius+i)+(radius+j)*size] = value;
        }
    
    // Normalize the mask.
    
    double sum = 0.0;
    for (int i = 0; i < mask.length; i++)
      sum += mask[i];
    float scale = (float) (1.0/sum);
    for (int i = 0; i < mask.length; i++)
      mask[i] *= scale;
    return mask;
  }
  
  /** Get a list of parameters which affect the behavior of the filter. */
  
  @Override
  public Property[] getProperties()
  {
    return new Property [] {new Property(Translate.text("Radius"), 0.0, 1.0, 0.05)};
  }

  /** Write a serialized description of this filter to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble((Integer) getPropertyValue(0));
  }

  /** Reconstruct this filter from its serialized representation. */
  
  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    setPropertyValue(0, in.readDouble());
  }
}

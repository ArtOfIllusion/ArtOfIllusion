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

/** This is an image filter which adjusts the brightness of an image. */

public class BrightnessFilter extends ImageFilter
{
  public BrightnessFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("Brightness");
  }
  
  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */
  
  public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    filterComponent(image, ComplexImage.RED);
    filterComponent(image, ComplexImage.GREEN);
    filterComponent(image, ComplexImage.BLUE);
  }
  
  /** Apply the filter to one component of an image. */
  
  private void filterComponent(ComplexImage image, int component)
  {
    int width = image.getWidth(), height = image.getHeight();
    float brightness = ((Number) getPropertyValue(0)).floatValue();
    float filtered[] = new float [width*height];
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
        filtered[i+j*width] = image.getPixelComponent(i, j, component)*brightness;
    image.setComponentValues(component, filtered);
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
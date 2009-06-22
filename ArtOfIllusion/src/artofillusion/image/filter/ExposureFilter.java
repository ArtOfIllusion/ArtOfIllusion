/* Copyright (C) 2005-2009 by Peter Eastman

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

/** This is an image filter which compensates for over or under exposed images by applying
    a gamma correction. */

public class ExposureFilter extends ImageFilter
{
  public ExposureFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("Exposure Correction");
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
    double exposure = (Double) getPropertyValue(0);
    double gamma = (exposure < 0.0 ? 1.0/(1.0-exposure) : exposure+1.0);
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
        float hsv[] = color.getHSV();
        hsv[2] = (float) Math.pow(hsv[2], 1.0/gamma);
        color.setHSV(hsv[0], hsv[1], hsv[2]);
        red[i+j*width] = color.getRed();
        green[i+j*width] = color.getGreen();
        blue[i+j*width] = color.getBlue();
      }
    image.setComponentValues(ComplexImage.RED, red);
    image.setComponentValues(ComplexImage.GREEN, green);
    image.setComponentValues(ComplexImage.BLUE, blue);
  }

  @Override
  public Property[] getProperties()
  {
    return new Property [] {new Property(getName(), -5.0, 5.0, 0.0)};
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
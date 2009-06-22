/* Copyright (C) 2009 by Peter Eastman

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

/** This is an image filter which blurs parts of an image to simulate a depth of field effect. */

public class DepthOfFieldFilter extends ImageFilter
{
  public DepthOfFieldFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("depthOfField");
  }

  @Override
  public int getDesiredComponents()
  {
    return ComplexImage.RED+ComplexImage.GREEN+ComplexImage.BLUE+ComplexImage.DEPTH;
  }

  /** Apply the filter to an image.
   *  @param image      the image to filter
   *  @param scene      the Scene which was rendered to create the image
   *  @param camera     the camera from which the Scene was rendered
   *  @param cameraPos  the position of the camera in the scene
   */

  public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    if (!image.hasFloatData(ComplexImage.DEPTH) || camera.getDepthOfField() == 0.0)
      return;
    int radius[] = findBlurRadius(image, camera);
    Thread currentThread = Thread.currentThread();
    int width = image.getWidth(), height = image.getHeight();
    float red[] = new float [width*height];
    float green[] = new float [width*height];
    float blue[] = new float [width*height];
    for (int i = 0; i < width; i++)
    {
      if (currentThread.isInterrupted())
        return;
      for (int j = 0; j < height; j++)
      {
        // Compute the color of this pixel.

        int maxRadius = radius[i+j*width];
        int weight = 0;
        int mstart = Math.max(0, i-maxRadius);
        int mend = Math.min(width-1, i+maxRadius);
        int nstart = Math.max(0, j-maxRadius);
        int nend = Math.min(height-1, j+maxRadius);
        int index = i+j*width;
        for (int m = mstart; m < mend; m++)
          for (int n = nstart; n < nend; n++)
          {
            float r = (float) Math.sqrt((m-i)*(m-i)+(n-j)*(n-j));
            if (r > maxRadius || r > radius[m+n*width])
              continue;
            weight++;
            red[index] += image.getPixelComponent(m, n, ComplexImage.RED);
            green[index] += image.getPixelComponent(m, n, ComplexImage.GREEN);
            blue[index] += image.getPixelComponent(m, n, ComplexImage.BLUE);
          }
        if (weight == 0)
        {
          red[index] = image.getPixelComponent(i, j, ComplexImage.RED);
          green[index] = image.getPixelComponent(i, j, ComplexImage.GREEN);
          blue[index] = image.getPixelComponent(i, j, ComplexImage.BLUE);
        }
        else
        {
          float invWeight = 1.0f/weight;
          red[index] *= invWeight;
          green[index] *= invWeight;
          blue[index] *= invWeight;
        }
      }
    }
    image.setComponentValues(ComplexImage.RED, red);
    image.setComponentValues(ComplexImage.BLUE, blue);
    image.setComponentValues(ComplexImage.GREEN, green);
  }

  /**
   * Find the blur radius for each pixel.
   */

  private int[] findBlurRadius(ComplexImage image, SceneCamera camera)
  {
    int width = image.getWidth();
    int height = image.getHeight();
    double focalDist = camera.getFocalDistance();
    double dofScale = 0.25*height/(camera.getDepthOfField()*camera.getFieldOfView());
    int radius[] = new int[width*height];
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
      {
        float depth = image.getPixelComponent(i, j, ComplexImage.DEPTH);
        if (depth == Float.MAX_VALUE)
          radius[i+j*width] = 0;
        else
          radius[i+j*width] = (int) Math.round(Math.abs(depth-focalDist)*dofScale);
      }
    return radius;
  }

  /** Write a serialized description of this filter to a stream. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
  }

  /** Reconstruct this filter from its serialized representation. */

  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
  }
}

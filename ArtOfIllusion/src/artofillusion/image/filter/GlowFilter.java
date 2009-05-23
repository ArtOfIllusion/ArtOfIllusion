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

/** This is an image filter which adds glow to an image. */

public class GlowFilter extends ImageFilter
{
  public static final String CROSSHAIR = Translate.text("Crosshairs");
  public static final String DIAGONAL = Translate.text("Diagonal");
  public static final String STAR = Translate.text("Star");
  public static final String CIRCLE = Translate.text("Circle");

  private static final String SHAPES[] = new String[] {CROSSHAIR, DIAGONAL, STAR, CIRCLE};

  public GlowFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("Glow");
  }
  
  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */
  
  public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    String shape = (String) getPropertyValue(0);
    if (shape.equals(CIRCLE))
      {
        int radius = (int) ((Double) getPropertyValue(1)*image.getHeight());
        if (radius < 1)
          return;
        float mask[] = createCircularMask(radius);
        filterComponentCircular(image, ComplexImage.RED, radius, mask);
        filterComponentCircular(image, ComplexImage.GREEN, radius, mask);
        filterComponentCircular(image, ComplexImage.BLUE, radius, mask);
      }
    else
      {
        filterComponent(image, ComplexImage.RED);
        filterComponent(image, ComplexImage.GREEN);
        filterComponent(image, ComplexImage.BLUE);
      }
  }
  
  /** Apply the filter to one component of an image. */
  
  private void filterComponent(ComplexImage image, int component)
  {
    Thread currentThread = Thread.currentThread();
    int width = image.getWidth(), height = image.getHeight();
    int radius = (int) ((Double) getPropertyValue(1)*height);
    int diagonalRadius = (int) ((Double) getPropertyValue(1)*height*Math.sqrt(0.5));
    float intensity = ((Number) getPropertyValue(2)).floatValue();
    float glow[] = new float [width*height];
    String shape = (String) getPropertyValue(0);
    for (int i = 0; i < width; i++)
    {
      if (currentThread.isInterrupted())
        return;
      for (int j = 0; j < height; j++)
      {
        float value = image.getPixelComponent(i, j, component);
        glow[i+j*width] += value;
        if (value < 1.0f)
          continue;
        float maxIntensity = intensity*(value-1.0f);
        if (shape.equals(CROSSHAIR) || shape.equals(STAR))
        {
          addGlowArm(glow, i, j, -1, 0, width, height, radius, maxIntensity);
          addGlowArm(glow, i, j, 1, 0, width, height, radius, maxIntensity);
          addGlowArm(glow, i, j, 0, -1, width, height, radius, maxIntensity);
          addGlowArm(glow, i, j, 0, 1, width, height, radius, maxIntensity);
        }
        if (shape.equals(DIAGONAL) || shape.equals(STAR))
        {
          addGlowArm(glow, i, j, -1, -1, width, height, diagonalRadius, maxIntensity);
          addGlowArm(glow, i, j, 1, -1, width, height, diagonalRadius, maxIntensity);
          addGlowArm(glow, i, j, -1, 1, width, height, diagonalRadius, maxIntensity);
          addGlowArm(glow, i, j, 1, 1, width, height, diagonalRadius, maxIntensity);
        }
      }
    }
    image.setComponentValues(component, glow);
  }
  
  /** Add one "arm" to the glow. */
  
  private void addGlowArm(float glow[], int x, int y, int xstep, int ystep, int width, int height, int radius, float intensity)
  {
    float intensityStep = intensity/radius;
    for (int i = 0; i < radius; i++)
      {
        x += xstep;
        y += ystep;
        if (x < 0 || y < 0 || x >= width || y >= height)
          return;
        glow[x+y*width] += intensity;
        intensity -= intensityStep;
      }
  }
  
  /** Build the mask for a circular glow. */
  
  private float [] createCircularMask(int radius)
  {
    int size = 2*radius+1, radius2 = radius*radius;
    float intensity = ((Number) getPropertyValue(2)).floatValue();
    float mask[] = new float [size*size];
    for (int i = 0; i < radius; i++)
      for (int j = 0; j < radius; j++)
        {
          int dist2 = i*i+j*j;
          if (dist2 > radius2)
            continue;
          float d = dist2/(float) radius2;
          float value = intensity*(d*(d-2.0f)+1.0f);
          mask[(radius-i)+(radius-j)*size] = value;
          mask[(radius+i)+(radius-j)*size] = value;
          mask[(radius-i)+(radius+j)*size] = value;
          mask[(radius+i)+(radius+j)*size] = value;
        }
    mask[radius+radius*size] = 0.0f;
    return mask;
  }
  
  /** Apply a circular glow filter to one component of an image. */
  
  private void filterComponentCircular(ComplexImage image, int component, int radius, float mask[])
  {
    Thread currentThread = Thread.currentThread();
    int maskWidth = 2*radius+1;
    int width = image.getWidth(), height = image.getHeight();
    float glow[] = new float [width*height];
    for (int i = 0; i < width; i++)
    {
      if (currentThread.isInterrupted())
        return;
      for (int j = 0; j < height; j++)
      {
        float value = image.getPixelComponent(i, j, component);
        glow[i+j*width] += value;
        if (value < 1.0f)
          continue;
        float maxIntensity = value-1.0f;
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
            glow[imageBase+x] += mask[maskBase+x]*maxIntensity;
        }
      }
    }
    image.setComponentValues(component, glow);
  }

  @Override
  public Property[] getProperties()
  {
    Object shapes[] = new Object[] {CROSSHAIR, DIAGONAL, STAR, CIRCLE};
    return new Property [] {
        new Property(Translate.text("Shape"), shapes, CROSSHAIR),
        new Property(Translate.text("Radius"), 0.0, 1.0, 0.05),
        new Property(Translate.text("Intensity"), 0.0, 1.0, 0.5)};
  }

  /** Write a serialized description of this filter to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    int shape = 0;
    for (int i = 0; i < SHAPES.length; i++)
      if (SHAPES[i].equals(getPropertyValue(0)))
        shape = i;
    out.writeInt(shape);
    out.writeDouble((Double) getPropertyValue(1));
    out.writeDouble((Double) getPropertyValue(2));
  }

  /** Reconstruct this filter from its serialized representation. */
  
  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    setPropertyValue(0, SHAPES[in.readInt()]);
    setPropertyValue(1, in.readDouble());
    setPropertyValue(2, in.readDouble());
  }
}
/* Copyright (C) 2003-2004 by Peter Eastman

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
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** This is an image filter which adds glow to an image. */

public class GlowFilter extends ImageFilter
{
  private int shape;
  public static final int CROSSHAIR = 0;
  public static final int DIAGONAL = 1;
  public static final int STAR = 2;
  public static final int CIRCLE = 3;

  public GlowFilter()
  {
    shape = CROSSHAIR;
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
    if (shape == CIRCLE)
      {
        int radius = (int) (paramValue[0]*image.getHeight());
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
    int radius = (int) (paramValue[0]*height);
    int diagonalRadius = (int) (paramValue[0]*height*Math.sqrt(0.5));
    float intensity = (float) paramValue[1];
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
        float maxIntensity = intensity*(value-1.0f);
        if (shape == CROSSHAIR || shape == STAR)
        {
          addGlowArm(glow, i, j, -1, 0, width, height, radius, maxIntensity);
          addGlowArm(glow, i, j, 1, 0, width, height, radius, maxIntensity);
          addGlowArm(glow, i, j, 0, -1, width, height, radius, maxIntensity);
          addGlowArm(glow, i, j, 0, 1, width, height, radius, maxIntensity);
        }
        if (shape == DIAGONAL || shape == STAR)
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
    float intensity = (float) paramValue[1];
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
  
  /** Get a list of parameters which affect the behavior of the filter. */
  
  public TextureParameter [] getParameters()
  {
    return new TextureParameter [] {new TextureParameter(this, Translate.text("Radius"), 0.0, 1.0, 0.05),
        new TextureParameter(this, Translate.text("Intensity"), 0.0, 1.0, 0.5)};
  }

  /** Write a serialized description of this filter to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(shape);
    out.writeDouble(paramValue[0]);
    out.writeDouble(paramValue[1]);
  }

  /** Reconstruct this filter from its serialized representation. */
  
  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    shape = in.readInt();
    paramValue[0] = in.readDouble();
    paramValue[1] = in.readDouble();
  }

  /** Get a Widget with which the user can specify options for the filter.
   * @param changeCallback*/

  public Widget getConfigPanel(final Runnable changeCallback)
  {
    FormContainer form = new FormContainer(new double [] {0.0, 1.0}, new double [] {1.0, 1.0});
    form.add(super.getConfigPanel(changeCallback), 0, 0, 2, 1);
    form.add(new BLabel(Translate.text("Shape")+": "), 0, 1);
    final BComboBox shapeChoice = new BComboBox(new String [] {
      Translate.text("Crosshairs"),
      Translate.text("Diagonal"),
      Translate.text("Star"),
      Translate.text("Circle"),
    });
    form.add(shapeChoice, 1, 1, new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
    shapeChoice.setSelectedIndex(shape);
    shapeChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        shape = shapeChoice.getSelectedIndex();
        changeCallback.run();
      }
    });
    UIUtilities.applyBackground(form, null);
    return form;
  }
}
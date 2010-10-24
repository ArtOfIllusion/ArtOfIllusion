/* Copyright (C) 2009-2010 by Peter Eastman

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
import java.util.*;

import buoy.widget.*;
import buoy.event.*;

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
    float weight[] = new float [width*height];
    for (int i = 0; i < width; i++)
    {
      if (currentThread.isInterrupted())
        return;
      for (int j = 0; j < height; j++)
      {
        // Blur this pixel based on its distance.

        int pixelRadius = radius[i+j*width];
        int radius2 = pixelRadius*pixelRadius;
        int mstart = Math.max(0, i-pixelRadius);
        int mend = Math.min(width-1, i+pixelRadius);
        int nstart = Math.max(0, j-pixelRadius);
        int nend = Math.min(height-1, j+pixelRadius);
        float scale = (pixelRadius == 0 ? 1.0f : 1.0f/radius2);
        float r = image.getPixelComponent(i, j, ComplexImage.RED);
        float g = image.getPixelComponent(i, j, ComplexImage.GREEN);
        float b = image.getPixelComponent(i, j, ComplexImage.BLUE);
        for (int m = mstart; m <= mend; m++)
          for (int n = nstart; n <= nend; n++)
          {
            int dist2 = (m-i)*(m-i)+(n-j)*(n-j);
            if (dist2 > radius2)
              continue;
            float w = (pixelRadius-(float) Math.sqrt(dist2))*scale;
            int index = m+n*width;
            weight[index] += w;
            red[index] += w*r;
            green[index] += w*g;
            blue[index] += w*b;
          }
      }
    }
    for (int i = 0; i < red.length; i++)
    {
      float invWeight = 1.0f/weight[i];
      red[i] *= invWeight;
      green[i] *= invWeight;
      blue[i] *= invWeight;
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
    boolean useCameraParams = (Boolean) getPropertyValue(0);
    double dof = (useCameraParams ? camera.getDepthOfField() : (Double) getPropertyValue(1));
    double focalDist = (useCameraParams ? camera.getFocalDistance() : (Double) getPropertyValue(2));
    double dofScale = 0.25*height*focalDist/(dof*camera.getFieldOfView());
    int radius[] = new int[width*height];
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
      {
        float depth = image.getPixelComponent(i, j, ComplexImage.DEPTH);
        if (depth == Float.MAX_VALUE)
          radius[i+j*width] = 1;
        else
          radius[i+j*width] = Math.max(1, (int) Math.round(Math.abs(depth-focalDist)*dofScale/depth));
      }
    return radius;
  }

  @Override
  public Property[] getProperties()
  {
    return new Property[] {
        new Property(Translate.text("useParamsFromCamera"), true),
        new Property(Translate.text("depthOfField"), Double.MIN_VALUE, Double.MAX_VALUE, Camera.DEFAULT_DISTANCE_TO_SCREEN/2.0),
        new Property(Translate.text("focalDist"), Double.MIN_VALUE, Double.MAX_VALUE, Camera.DEFAULT_DISTANCE_TO_SCREEN)
    };
  }

  @Override
  public Widget getConfigPanel(Runnable changeCallback)
  {
    Widget panel = super.getConfigPanel(changeCallback);
    final List<Widget> children = UIUtilities.findAllChildren(panel);
    BCheckBox cb = null;
    for (Widget w : children)
      if (w instanceof BCheckBox)
        cb = (BCheckBox) w;
    final BCheckBox checkbox = cb;
    Runnable listener = new Runnable() {
      public void run()
      {

        boolean enable = !checkbox.getState();
        for (Widget w : children)
          if (w != checkbox)
            w.setEnabled(enable);
      }
    };
    checkbox.addEventLink(ValueChangedEvent.class, listener, "run");
    listener.run();
    return panel;
  }

  /** Write a serialized description of this filter to a stream. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(0);
    out.writeBoolean((Boolean) getPropertyValue(0));
    out.writeDouble((Double) getPropertyValue(1));
    out.writeDouble((Double) getPropertyValue(2));
  }

  /** Reconstruct this filter from its serialized representation. */

  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    int version = in.readShort();
    if (version != 0)
      throw new IOException("Unknown version "+version);
    setPropertyValue(0, in.readBoolean());
    setPropertyValue(1, in.readDouble());
    setPropertyValue(2, in.readDouble());
  }
}

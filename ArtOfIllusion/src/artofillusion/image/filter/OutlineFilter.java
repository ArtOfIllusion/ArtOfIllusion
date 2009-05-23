/* Copyright (C) 2004-2009 by Peter Eastman

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

public class OutlineFilter extends ImageFilter
{
  private static final byte NONE = 0;
  private static final byte CENTER = 1;
  private static final byte UP = 2;
  private static final byte DOWN = 3;
  private static final byte LEFT = 4;
  private static final byte RIGHT = 5;
  
  public OutlineFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("Outline");
  }
  
  /** Get a list of all the image components required by this filter. */
  
  public int getDesiredComponents()
  {
    return ComplexImage.RED+ComplexImage.GREEN+ComplexImage.BLUE+ComplexImage.DEPTH;
  }
  
  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */
  
  public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    if (!image.hasFloatData(ComplexImage.DEPTH))
      return;
    double thickness = (Double) getPropertyValue(0);
    if (thickness <= 0.0)
      return;
    float masks[][] = new float [][] {null, createMask(thickness, 0.0, 0.0),
        createMask(thickness, 0.0, -0.5), createMask(thickness, 0.0, 0.5),
        createMask(thickness, -0.5, 0.0), createMask(thickness, 0.5, 0.0)};
    float outline[] = findOutline(image, masks);
    RGBColor color = (RGBColor) getPropertyValue(3);
    applyOutline(image, ComplexImage.RED, outline, color.getRed());
    applyOutline(image, ComplexImage.GREEN, outline, color.getGreen());
    applyOutline(image, ComplexImage.BLUE, outline, color.getBlue());
  }
  
  /** Create a map of the outlines. */
  
  private float [] findOutline(ComplexImage image, float masks[][])
  {
    int width = image.getWidth(), height = image.getHeight();
    byte edgeType[] = new byte [width*height];
    
    // First find the points that lie on edges.
    
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
      {
        float depth = image.getPixelComponent(i, j, ComplexImage.DEPTH);
        if (i > 0 && i < width-1 && isOutline(image.getPixelComponent(i-1, j, ComplexImage.DEPTH), depth, image.getPixelComponent(i+1, j, ComplexImage.DEPTH)))
          edgeType[i+j*width] = CENTER;
        else if (j > 0 && j < height-1 && isOutline(image.getPixelComponent(i, j-1, ComplexImage.DEPTH), depth, image.getPixelComponent(i, j+1, ComplexImage.DEPTH)))
          edgeType[i+j*width] = CENTER;
      }
    
    // Now smooth the outline by finding points on the edge that can be moved a half-pixel
    // in one direction.
    
    for (int i = 1; i < width-1; i++)
      for (int j = 1; j < height-1; j++)
      {
        if (edgeType[i+j*width] == NONE)
          continue;
        int count = 0;
        for (int k = -1; k < 2; k++)
          for (int m = -1; m < 2; m++)
            if (edgeType[i+k+(j+m)*width] != NONE)
              count++;
        if (count != 3)
          continue;
        if (edgeType[i-1+j*width] != NONE)
        {
          if (edgeType[i+1+(j-1)*width] != NONE)
            edgeType[i+j*width] = UP;
          else if (edgeType[i+1+(j+1)*width] != NONE)
            edgeType[i+j*width] = DOWN;
        }
        else if (edgeType[i+1+j*width] != NONE)
        {
          if (edgeType[i-1+(j-1)*width] != NONE)
            edgeType[i+j*width] = UP;
          else if (edgeType[i-1+(j+1)*width] != NONE)
            edgeType[i+j*width] = DOWN;
        }
        else if (edgeType[i+(j-1)*width] != NONE)
        {
          if (edgeType[i-1+(j+1)*width] != NONE)
            edgeType[i+j*width] = LEFT;
          else if (edgeType[i+1+(j+1)*width] != NONE)
            edgeType[i+j*width] = RIGHT;
        }
        else if (edgeType[i+(j+1)*width] != NONE)
        {
          if (edgeType[i-1+(j-1)*width] != NONE)
            edgeType[i+j*width] = LEFT;
          else if (edgeType[i+1+(j-1)*width] != NONE)
            edgeType[i+j*width] = RIGHT;
        }
      }
    
    // Now build the image of the outline.
    
    Thread currentThread = Thread.currentThread();
    float outline[] = new float [width*height];
    int maskWidth[] = new int [masks.length];
    for (int i = 1; i < masks.length; i++)
      maskWidth[i] = (int) Math.sqrt(masks[i].length);
    for (int i = 0; i < width; i++)
    {
      if (currentThread.isInterrupted())
        return outline;
      for (int j = 0; j < height; j++)
      {
        byte type = (edgeType[i+j*width]);
        if (type != NONE)
          drawOutlineSpot(i, j, outline, width, height, masks[type], maskWidth[type], 1.0f);
      }
    }
    return outline;
  }
  
  /** Given the depths at three adjacent pixels, decide whether this is an outline. */
  
  private boolean isOutline(float d1, float d2, float d3)
  {
    double changeCutoff = (Double) getPropertyValue(1);
    double relCutoff = (Double) getPropertyValue(2);
    if (d1 > 1.0e6f)
      d1 = 1.0e6f;
    if (d2 > 1.0e6f)
      d2 = 1.0e6f;
    if (d3 > 1.0e6f)
      d3 = 1.0e6f;
    if (d2-d1 < d2*relCutoff && d2-d3 < d2*relCutoff)
      return false;
    if ((2.0*d2-d1-d3)/d2 > changeCutoff)
      return true;
    return false;
  }
  
  /** Draw a single dot into the image of the outline. */
  
  private void drawOutlineSpot(int i, int j, float outline[], int width, int height, float mask[], int maskWidth, float fraction)
  {
    int radius = (maskWidth-1)/2;
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
        {
          float val = mask[maskBase+x]*fraction;
          if (outline[imageBase+x] < val)
            outline[imageBase+x] = val;
        }
      }
  }
  
  /** Add the outline to one component of the image. */
  
  private void applyOutline(ComplexImage image, int component, float outline[], float color)
  {
    int width = image.getWidth(), height = image.getHeight();
    float pixel[] = new float [width*height];
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
      {
        float fract = outline[j*width+i];
        pixel[j*width+i] = fract*color + (1.0f-fract)*image.getPixelComponent(i, j, component);
      }
    image.setComponentValues(component, pixel);
  }

  /** Build the mask. */
  
  private float [] createMask(double thickness, double xoffset, double yoffset)
  {
    int size = (int) Math.ceil(thickness-0.001);
    if (size%2 == 0)
      size++;
    double radius = 0.5*thickness-0.25;
    double radius2 = radius+0.5;
    int last = size-1, center = last/2;
    float mask[] = new float [size*size];

    for (int i = 0; i < size; i++)
      for (int j = 0; j < size; j++)
      {
        double dx1, dx2, dy1, dy2;
        if (i < center)
        {
          dx1 = center+xoffset-i-0.5;
          dx2 = center+xoffset-i+0.5;
        }
        else if (i == center)
          dx1 = dx2 = Math.abs(xoffset);
        else
        {
          dx1 = i-center-xoffset-0.5;
          dx2 = i-center-xoffset+0.5;
        }
        if (j < center)
        {
          dy1 = center+yoffset-j-0.5;
          dy2 = center+yoffset-j+0.5;
        }
        else if (j == center)
          dy1 = dy2 = Math.abs(yoffset);
        else
        {
          dy1 = j-center-yoffset-0.5;
          dy2 = j-center-yoffset+0.5;
        }
        double dist1 = Math.sqrt(dx1*dx1+dy1*dy1);
        double dist2 = Math.sqrt(dx2*dx2+dy2*dy2);
        float val = 0.5f*(calcMaskValue(radius, dist1, dist2)+calcMaskValue(radius2, dist1, dist2));
        mask[i+j*size] = val;
      }
    return mask;
  }

  private float calcMaskValue(double radius, double dist1, double dist2)
  {
    if (dist1 > radius)
      return 0.0f;
    if (dist2 < radius)
      return 1.0f;
    else
      return (float) ((radius-dist1)/(dist2-dist1));
  }

  @Override
  public Property[] getProperties()
  {
    return new Property[] {
        new Property(Translate.text("Thickness"), 0.0, Double.MAX_VALUE, 3.0),
        new Property(Translate.text("Change Cutoff"), 0.0, Double.MAX_VALUE, 0.01),
        new Property(Translate.text("Distance Cutoff"), 0.0, Double.MAX_VALUE, 0.01),
        new Property(Translate.text("Color"), new RGBColor(0.0, 0.0, 0.0))
    };
  }

  /** Write a serialized description of this filter to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(0);
    out.writeDouble((Double) getPropertyValue(0));
    out.writeDouble((Double) getPropertyValue(1));
    out.writeDouble((Double) getPropertyValue(2));
    RGBColor color = (RGBColor) getPropertyValue(3);
    out.writeDouble(color.getRed());
    out.writeDouble(color.getGreen());
    out.writeDouble(color.getBlue());
  }

  /** Reconstruct this filter from its serialized representation. */
  
  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    int version = in.readShort();
    if (version != 0)
      throw new IOException("Unknown version "+version);
    setPropertyValue(0, in.readDouble());
    setPropertyValue(1, in.readDouble());
    setPropertyValue(2, in.readDouble());
    setPropertyValue(3, new RGBColor(in.readDouble(), in.readDouble(), in.readDouble()));
  }
}

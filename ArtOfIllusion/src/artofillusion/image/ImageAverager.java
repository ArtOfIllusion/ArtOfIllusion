/* This class takes a series of images, and calculates the average image. */

/* Copyright (C) 2001,2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import java.awt.*;
import java.awt.image.*;

public class ImageAverager
{
  int width, height, num;
  float red[], green[], blue[], alpha[];

  public ImageAverager(int width, int height)
  {
    int size = width*height;
    
    this.width = width;
    this.height = height;
    red = new float [size];
    green = new float [size];
    blue = new float [size];
    alpha = new float [size];
  }
  
  /** Add an image. */
  
  public void addImage(Image im)
  {
    addImage(new ComplexImage(im));
  }

  /** Add an image. */
  
  public void addImage(ComplexImage img)
  {
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
        {
          int index = i+j*width;
          red[index] += img.getPixelComponent(i, j, ComplexImage.RED);
          green[index] += img.getPixelComponent(i, j, ComplexImage.GREEN);
          blue[index] += img.getPixelComponent(i, j, ComplexImage.BLUE);
          alpha[index] += img.getPixelComponent(i, j, ComplexImage.ALPHA);
        }
    num++;
  }
  
  /** Get the average image. */
  
  public ComplexImage getAverageImage()
  {
    int data[] = new int [red.length];
    float scale = 1.0f/num;
    
    for (int i = 0; i < data.length; i++)
      {
        red[i] *= scale;
        green[i] *= scale;
        blue[i] *= scale;
        alpha[i] *= scale;
        int a = (int) (alpha[i]*255.0f), r = (int) (red[i]*255.0f), g = (int) (green[i]*255.0f), b = (int) (blue[i]*255.0f);
        if (a > 255)
          a = 255;
        if (r > 255)
          r = 255;
        if (g > 255)
          g = 255;
        if (b > 255)
          b = 255;
        data[i] = (a<<24) + (r<<16) + (g<<8) + b;
      }
    MemoryImageSource source = new MemoryImageSource(width, height, data, 0, width);
    ComplexImage img = new ComplexImage(Toolkit.getDefaultToolkit().createImage(source));
    img.setComponentValues(ComplexImage.RED, red);
    img.setComponentValues(ComplexImage.GREEN, green);
    img.setComponentValues(ComplexImage.BLUE, blue);
    img.setComponentValues(ComplexImage.ALPHA, alpha);
    return img;
  }
  
  /** Prepare to average a new set of images. */
  
  public void clear()
  {
    num = 0;
    for (int i = 0; i < red.length; i++)
      alpha[i] = red[i] = green[i] = blue[i] = 0.0f;
  }
}
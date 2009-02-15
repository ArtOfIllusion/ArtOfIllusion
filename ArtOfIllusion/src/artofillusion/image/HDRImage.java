/* Copyright (C) 2001-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.math.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

/** HDRImage is an ImageMap subclass.  It represents a high dynamic range image stored in
    Greg Ward's RGBE format, as described in "Graphics Gems IV", edited by James Arvo,
    Academic Press, 1994. */

public class HDRImage extends ImageMap
{
  private int width[], height[];
  private byte maps[][][];
  private float average[];
  private double xscale[], yscale[], scale[], scaleMult[];
  private Image preview;

  /** Create an HDRImage from the r, g, b, and e components. */

  public HDRImage(byte r[], byte g[], byte b[], byte e[], int xres, int yres)
  {
    buildMipMaps(r, g, b, e, xres, yres);
    findAverage();
    createPreview();
  }
  
  /** Given the r, g, b, and e arrays for an image, this method builds the full set of mipmaps for it. */
  
  private void buildMipMaps(byte r[], byte g[], byte b[], byte e[], int w, int h)
  {
    byte map0[][] = new byte[][] {r, g, b, e};
    int w1, h1, num, i, j, k, m;
    float wratio, hratio;

    // Determine the size for the first reduced map.  Its width and height must both be
    // powers of 2.
    
    for (w1 = 2; w1 < w; w1 *= 2);
    for (h1 = 2; h1 < h; h1 *= 2);
    w1 /= 2;
    h1 /= 2;
    wratio = w1 / (float) w;
    hratio = h1 / (float) h;
    if (w > 1 && wratio > 0.75)
      {
        w1 /= 2;
        wratio *= 0.5;
      }
    if (h > 1 && hratio > 0.75)
      {
        h1 /= 2;
        hratio *= 0.5;
      }
    
    // Determine the total number of mipmaps we will need, and allocate the arrays.
    
    for (num = 0; (1<<num) < w1 && (1<<num) < h1; num++);
    num += 2;
    if (w == 1 || h == 1)
      num = 1;
    maps = new byte [num][][];
    width = new int [num];
    height = new int [num];
    scale = new double [num];
    xscale = new double [num];
    yscale = new double [num];
    scaleMult = new double [num-1];
    maps[0] = map0;
    width[0] = w;
    height[0] = h;
    scale[0] = 1.0/Math.min(w, h);
    
    // Construct the first reduced map.
    
    if (num > 1)
      {
        width[1] = w1;
        height[1] = h1;
        scale[1] = 1.0/Math.min(w1, h1);
        maps[1] = new byte [4][w1*h1];
        float widthScale = w/(float) w1;
        float heightScale = h/(float) h1;
        for (i = 0; i < 4; i++)
          {
            byte componentMap0[] = maps[0][i];
            byte componentMap1[] = maps[1][i];
            for (j = 0; j < h1; j++)
              {
                int pos1 = j*w1;
                float pos0 = ((int) (j*heightScale))*w;
                for (k = 0; k < w1; k++)
                  {
                    componentMap1[pos1++] = componentMap0[(int) pos0];
                    pos0 += widthScale;
                  }
              }
          }
      }
    
    // Now construct the remaining mipmaps.
    
    RGBColor avg = new RGBColor();
    RGBColor tempColor = new RGBColor();
    for (i = 2; i < num; i++)
      {
        w = width[i] = width[i-1]/2;
        h = height[i] = height[i-1]/2;
        scale[i] = 2.0*scale[i-1];
        maps[i] = new byte [4][w*h];
        for (k = 0; k < w; k++)
          for (m = 0; m < h; m++)
            {
              j = 2*k+4*w*m;
              tempColor.setERGB(maps[i-1][0][j], maps[i-1][1][j], maps[i-1][2][j], maps[i-1][3][j]);
              avg.copy(tempColor);
              j = 2*k+1+4*w*m;
              tempColor.setERGB(maps[i-1][0][j], maps[i-1][1][j], maps[i-1][2][j], maps[i-1][3][j]);
              avg.add(tempColor);
              j = 2*k+2*w*(2*m+1);
              tempColor.setERGB(maps[i-1][0][j], maps[i-1][1][j], maps[i-1][2][j], maps[i-1][3][j]);
              avg.add(tempColor);
              j = 2*k+1+2*w*(2*m+1);
              tempColor.setERGB(maps[i-1][0][j], maps[i-1][1][j], maps[i-1][2][j], maps[i-1][3][j]);
              avg.add(tempColor);
              avg.scale(0.25);
              int ergb = avg.getERGB();
              maps[i][0][k+w*m] = (byte) ((ergb>>16)&0xFF);
              maps[i][1][k+w*m] = (byte) ((ergb>>8)&0xFF);
              maps[i][2][k+w*m] = (byte) (ergb&0xFF);
              maps[i][3][k+w*m] = (byte) ((ergb>>24)&0xFF);
            }
      }
    
    // Precompute multipliers used for doing the mipmapping.
    
    for (i = 0; i < num-1; i++)
      scaleMult[i] = 1.0/(scale[i+1]-scale[i]);

    // Finally, record the scales for all of the maps.
    
    for (i = 0; i < num; i++)
      {
        xscale[i] = (double) width[i];
        yscale[i] = (double) height[i];
      }
  }
    
  /** This method calculates the average value for each component over the entire image. */
  
  private void findAverage()
  {
    byte map[][] = maps[maps.length-1];
    RGBColor avg = new RGBColor();
    RGBColor tempColor = new RGBColor();
    int len = map[0].length;
    
    for (int i = 0; i < len; i++)
      {
        tempColor.setERGB(map[0][i], map[1][i], map[2][i], map[3][i]);
        avg.add(tempColor);
      }
    avg.scale(1.0/len);
    average = new float [] {avg.getRed(), avg.getGreen(), avg.getBlue()};
  }
  
  /** Construct the preview image. */
  
  private void createPreview()
  {
    int w, h;
    
    if (width[0] <= PREVIEW_WIDTH && height[0] <= PREVIEW_HEIGHT)
      {
        w = width[0];
        h = height[0];
      }
    else
      {
        if (width[0] < height[0])
          {
            w = (int) (width[0]*PREVIEW_HEIGHT/(float) height[0]);
            h = PREVIEW_HEIGHT;
          }
        else
          {
            w = PREVIEW_WIDTH;
            h = (int) (height[0]*PREVIEW_WIDTH/(float) width[0]);
          }
      }
    int data[] = new int [w*h];
    float xstep = width[0]/(float) w;
    float ystep = height[0]/(float) h;
    byte map0[][] = maps[0];
    RGBColor tempColor = new RGBColor();
    for (int i = 0; i < h; i++)
      {
        float pos = (int) (i*ystep)*width[0];
        int base = i*w;
        for (int j = 0; j < w; j++)
          {
            int ipos = (int) pos;
            tempColor.setERGB(map0[0][ipos], map0[1][ipos], map0[2][ipos], map0[3][ipos]);
            data[base+j] = tempColor.getARGB();
            pos += xstep;
          }
      }
    MemoryImageSource src = new MemoryImageSource(w, h, data, 0, w);
    preview = Toolkit.getDefaultToolkit().createImage(src);
  }
  
  /** Get the width of the image. */
  
  public int getWidth()
  {
    return width[0];
  }
  
  /** Get the height of the image. */
  
  public int getHeight()
  {
    return height[0];
  }
  
  /** Get the number of components in the image. */
  
  public int getComponentCount()
  {
    return 3;
  }

  /** Get the value of a single component at a particular location in the image.  The components are:
      
      0: Red
      1: Green
      2: Blue
      3: Alpha
      
      The location is specified by x and y, which must lie between 0 and 1.  The value is
      averaged over a region of width (xsize, ysize).  wrapx and wrapy specify whether, for 
      purposes of interpolation, the image should be treated as wrapping around so that 
      opposite edges touch each other. */
  
  public float getComponent(int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    int which;
    float frac;
    double size;
    
    if (component > 2)
      return 1.0f;
    if (xsize*xscale[0] > ysize*yscale[0])
      size = xsize;
    else
      size = ysize;
    y = 1.0-y;
    
    // If size falls outside the range of scales spanned by the mipmaps, just use the first
    // or last one, as appropriate.
    
    if (size <= scale[0])
      return getMapComponent(component, 0, wrapx, wrapy, x, y);
    if (size >= scale[maps.length-1])
      return average[component];
    
    // Determine which mipmaps to use.
    
    for (which = 0; size > scale[which+1]; which++);
    frac = (float) ((size-scale[which]) * scaleMult[which]);
    return (1.0f-frac)*getMapComponent(component, which, wrapx, wrapy, x, y) + frac*getMapComponent(component, which+1, wrapx, wrapy, x, y);
  }

  /** This method is called by getComponent.  It determines the value at the point (x, y)
      of the particular mipmap. */

  private float getMapComponent(int component, int which, boolean wrapx, boolean wrapy, double x, double y)
  {
    int i1, i2, j1, j2;
    int ind1, ind2, ind3, ind4;
    int w = width[which], h = height[which];
    float frac1, frac2, w1, w2, w3, w4;
    byte map[][] = maps[which];
    
    // Determine which elements to interpolate between.

    frac1 = (float) (x*xscale[which]);
    i1 = (int) frac1;
    frac1 -= (float) i1;
    if (i1 >= w-1)
      {
        i1 = w-1;
        i2 = wrapx ? 0 : i1;
      }
    else
      i2 = i1+1;
    frac2 = (float) (y*yscale[which]);
    j1 = (int) frac2;
    frac2 -= (float) j1;
    if (j1 >= h-1)
      {
        j1 = h-1;
        j2 = wrapy ? 0 : j1;
      }
    else
      j2 = j1+1;
    ind1 = i1+j1*w;
    ind2 = i1+j2*w;
    ind3 = i2+j1*w;
    ind4 = i2+j2*w;
    
    // Find the interpolation coefficients.
    
    w1 = (1.0f-frac1)*(1.0f-frac2);
    w2 = (1.0f-frac1)*frac2;
    w3 = frac1*(1.0f-frac2);
    w4 = frac1*frac2;
    
    // Calculate the final value.
    
    float value1 = (map[component][ind1]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind1]&0xFF];
    float value2 = (map[component][ind2]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind2]&0xFF];
    float value3 = (map[component][ind3]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind3]&0xFF];
    float value4 = (map[component][ind4]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind4]&0xFF];
    return (value1*w1 + value2*w2 + value3*w3 + value4*w4);
  }

  /** Get the average value for a particular component, over the entire image. */
  
  public float getAverageComponent(int component)
  {
    if (component >= 3)
      return 0.0f;
    return average[component];
  }

  /** Get the color at a particular location.  The location is specified by x and y, 
      which must lie between 0 and 1.  The color is averaged over a region of width 
      (xsize, ysize).  wrapx and wrapy specify whether, for purposes of interpolation, the 
      image should be treated as wrapping around so that opposite edges touch each other. */

  public void getColor(RGBColor theColor, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    int which;
    float frac;
    double size;
    
    if (xsize*xscale[0] > ysize*yscale[0])
      size = xsize;
    else
      size = ysize;
    y = 1.0-y;

    // If size falls outside the range of scales spanned by the mipmaps, just use the first
    // or last one, as appropriate.
    
    if (size <= scale[0])
      {
        getMapColor(theColor, 0, wrapx, wrapy, x, y);
        return;
      }
    if (size >= scale[maps.length-1])
      {
        getMapColor(theColor, maps.length-1,wrapx, wrapy, x, y);
        return;
      }
    
    // Determine which mipmaps to use.
    
    for (which = 0; size > scale[which+1]; which++);
    frac = (float) ((size-scale[which]) * scaleMult[which]);
    RGBColor tempColor = new RGBColor();
    getMapColor(tempColor, which, wrapx, wrapy, x, y);
    getMapColor(theColor, which+1, wrapx, wrapy, x, y);
    tempColor.scale(1.0f-frac);
    theColor.scale(frac);
    theColor.add(tempColor);
  }

  /** This method is called by getColor.  It determines the color at the point (x, y)
      of the particular mipmap. */

  private void getMapColor(RGBColor theColor, int which, boolean wrapx, boolean wrapy, double x, double y)
  {
    int i1, i2, j1, j2;
    int ind1, ind2, ind3, ind4;
    int w = width[which], h = height[which];
    float frac1, frac2, w1, w2, w3, w4;
    byte map[][] = maps[which];
    
    // Determine which elements to interpolate between.

    frac1 = (float) (x*xscale[which]);
    i1 = (int) frac1;
    frac1 -= (float) i1;
    if (i1 >= w-1)
      {
        i1 = w-1;
        i2 = wrapx ? 0 : i1;
      }
    else
      i2 = i1+1;
    frac2 = (float) (y*yscale[which]);
    j1 = (int) frac2;
    frac2 -= (float) j1;
    if (j1 >= h-1)
      {
        j1 = h-1;
        j2 = wrapy ? 0 : j1;
      }
    else
      j2 = j1+1;
    ind1 = i1+j1*w;
    ind2 = i1+j2*w;
    ind3 = i2+j1*w;
    ind4 = i2+j2*w;
    
    // Find the interpolation coefficients.
    
    w1 = (1.0f-frac1)*(1.0f-frac2);
    w2 = (1.0f-frac1)*frac2;
    w3 = frac1*(1.0f-frac2);
    w4 = frac1*frac2;
    
    // Calculate the final value for each component.
    
    theColor.setERGB(map[0][ind1], map[1][ind1], map[2][ind1], map[3][ind1]);
    theColor.scale(w1);
    RGBColor tempColor = new RGBColor();
    tempColor.setERGB(map[0][ind2], map[1][ind2], map[2][ind2], map[3][ind2]);
    tempColor.scale(w2);
    theColor.add(tempColor);
    tempColor.setERGB(map[0][ind3], map[1][ind3], map[2][ind3], map[3][ind3]);
    tempColor.scale(w3);
    theColor.add(tempColor);
    tempColor.setERGB(map[0][ind4], map[1][ind4], map[2][ind4], map[3][ind4]);
    tempColor.scale(w4);
    theColor.add(tempColor);
  }
  
  /** Get the gradient of a single component at a particular location in the image.  
      The location is specified by x and y, which must lie between 0 and 1.  The value is
      averaged over a region of width (xsize, ysize) before the gradient is calculated.  
      wrapx and wrapy specify whether, for purposes of interpolation, the image should be 
      treated as wrapping around so that opposite edges touch each other. */
  
  public void getGradient(Vec2 grad, int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    int which;
    double frac;
    double size;
    
    if (xsize*xscale[0] > ysize*yscale[0])
      size = xsize;
    else
      size = ysize;
    y = 1.0-y;
    
    // If size falls outside the range of scales spanned by the mipmaps, just use the first
    // or last one, as appropriate.
    
    if (size <= scale[0])
      {
        getMapGradient(grad, component, 0, wrapx, wrapy, x, y);
        return;
      }
    if (size >= scale[maps.length-1])
      {
        grad.set(0.0, 0.0);
        return;
      }
    
    // Determine which mipmaps to use.
    
    for (which = 0; size > scale[which+1]; which++);
    frac = (float) ((size-scale[which]) * scaleMult[which]);
    Vec2 tempVec = new Vec2();
    getMapGradient(grad, component, which, wrapx, wrapy, x, y);
    getMapGradient(tempVec, component, which+1, wrapx, wrapy, x, y);
    grad.scale(1.0-frac);
    tempVec.scale(frac);
    grad.add(tempVec);
    grad.y = -grad.y;
  }

  /** This method is called by getGradient.  It determines the gradient at the point (x, y)
      of the particular mipmap. */

  private void getMapGradient(Vec2 grad, int component, int which, boolean wrapx, boolean wrapy, double x, double y)
  {
    int i1, i2, j1, j2;
    int ind1, ind2, ind3, ind4;
    int w = width[which], h = height[which];
    double frac1, frac2, v1, v2, v3, v4;
    byte map[][] = maps[which];
    
    // Determine which elements to interpolate between.

    frac1 = x*xscale[which];
    i1 = (int) frac1;
    frac1 -= (double) i1;
    if (i1 >= w-1)
      {
        i1 = w-1;
        i2 = wrapx ? 0 : i1;
      }
    else
      i2 = i1+1;
    frac2 = y*yscale[which];
    j1 = (int) frac2;
    frac2 -= (double) j1;
    if (j1 >= h-1)
      {
        j1 = h-1;
        j2 = wrapy ? 0 : j1;
      }
    else
      j2 = j1+1;
    ind1 = i1+j1*w;
    ind2 = i1+j2*w;
    ind3 = i2+j1*w;
    ind4 = i2+j2*w;
    
    // Find the values at the four points.
    
    v1 = (map[component][ind1]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind1]&0xFF];
    v2 = (map[component][ind2]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind2]&0xFF];
    v3 = (map[component][ind3]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind3]&0xFF];
    v4 = (map[component][ind4]&0xFF)*RGBColor.ERGB_EXP_SCALE[map[3][ind4]&0xFF];
    
    // Calculate the final value.
    
    grad.x = ((v3-v1)*(1.0-frac2) + (v4-v2)*frac2) * xscale[which];
    grad.y = ((v2-v1)*(1.0-frac1) + (v4-v3)*frac1) * yscale[which];
  }

  /** Get a scaled down copy of the image, to use for previews.  This Image will be no larger
      (but may be smaller) than PREVIEW_WIDTH by PREVIEW_HEIGHT. */
  
  public Image getPreview()
  {
    return preview;
  }

  /** Reconstruct an image from its serialized representation. */
  
  public HDRImage(DataInputStream in) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("Illegal version for HDRImage");
    int w = in.readInt(), h = in.readInt();
    byte map[][] = new byte [4][];
    for (int i = 0; i < 4; i++)
      {
        map[i] = new byte [w*h];
        in.readFully(map[i]);
      }
    buildMipMaps(map[0], map[1], map[2], map[3], w, h);
    findAverage();
    createPreview();
  }
  
  /** Serialize an image to an output stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeShort(0);
    out.writeInt(width[0]);
    out.writeInt(height[0]);
    for (int i = 0; i < 4; i++)
      out.write(maps[0][i]);
  }
}
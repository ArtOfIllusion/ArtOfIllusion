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
import javax.imageio.*;
import javax.swing.*;

/** MIPMappedImage is an ImageMap subclass.  It stores 8 bits per color component, and uses MIP
mapping to eliminate aliasing. */

public class MIPMappedImage extends ImageMap
{
  private int width[], height[], components;
  private byte maps[][][];
  private float average[];
  private double xscale[], yscale[], scale[], scaleMult[], gradXScale[], gradYScale[];
  private Image preview;

  private static final float SCALE = 1.0f/255.0f;

  /** Construct a MIPMappedImage from an Image object. */

  public MIPMappedImage(Image im) throws InterruptedException
  {
    init(im);
  }

  /** Construct a MIPMappedImage from a GIF, JPEG, or PNG image file. */

  public MIPMappedImage(File file) throws InterruptedException
  {
    Image im = new ImageIcon(file.getAbsolutePath()).getImage();
    maps = new byte [1][][];
    init(im);
  }

  /** Initialize a newly created MIPMappedImage. */

  private void init(Image im) throws InterruptedException
  {
    buildMipMaps(im);
    if (width[0] <= PREVIEW_WIDTH && height[0] <= PREVIEW_HEIGHT)
      preview = im;
    else
      {
        if (width[0] < height[0])
          preview = im.getScaledInstance(-1, PREVIEW_HEIGHT, Image.SCALE_DEFAULT);
        else
          preview = im.getScaledInstance(PREVIEW_WIDTH, -1, Image.SCALE_DEFAULT);
      }
    findAverage();
  }

  /** Given an Image object, this method builds the full set of mipmaps for it. */

  private void buildMipMaps(Image im) throws InterruptedException
  {
    byte map0[][] = findComponentMaps(im);
    int w = im.getWidth(null), h = im.getHeight(null);
    int w1, h1, num, i, j, k, m;
    float wratio, hratio;
    Image im1;

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
    gradXScale = new double [num];
    gradYScale = new double [num];
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
        im1 = im.getScaledInstance(w1, h1, Image.SCALE_DEFAULT);
        maps[1] = findComponentMaps(im1);
      }

    // Now construct the remaining mipmaps.

    for (i = 2; i < num; i++)
      {
        w = width[i] = width[i-1]/2;
        h = height[i] = height[i-1]/2;
        scale[i] = 2.0*scale[i-1];
        maps[i] = new byte [components][w*h];
        for (j = 0; j < components; j++)
          for (k = 0; k < w; k++)
            for (m = 0; m < h; m++)
              maps[i][j][k+w*m] = (byte) ((((int) maps[i-1][j][2*k+4*w*m]&0xFF) + ((int) maps[i-1][j][2*k+1+4*w*m]&0xFF) +
                    ((int) maps[i-1][j][2*k+2*w*(2*m+1)]&0xFF) + ((int) maps[i-1][j][2*k+1+2*w*(2*m+1)]&0xFF)) >> 2);
      }

    // Precompute multipliers used for doing the mipmapping.

    for (i = 0; i < num-1; i++)
      scaleMult[i] = 1.0/(scale[i+1]-scale[i]);

    // Finally, record the scales for all of the maps.

    for (i = 0; i < num; i++)
      {
        xscale[i] = (double) width[i];
        yscale[i] = (double) height[i];
        gradXScale[i] = SCALE*xscale[i];
        gradYScale[i] = SCALE*yscale[i];
      }
  }

  /** This method constructs a set of byte arrays containing the image data for each
      component of the Image object. */

  private byte[][] findComponentMaps(Image im) throws InterruptedException
  {
    PixelGrabber pg = new PixelGrabber(im, 0, 0, -1, -1, true);
    int i, data[];
    byte map[][];

    pg.grabPixels();
    data = (int []) pg.getPixels();
    if (components == 0)
      countComponents(data);
    map = new byte [components][data.length];
    for (i = 0; i < data.length; i++)
      {
        map[0][i] = (byte) ((data[i]>>16) & 0xFF);
        if (components > 1)
          map[1][i] = (byte) ((data[i]>>8) & 0xFF);
        if (components > 2)
          map[2][i] = (byte) (data[i] & 0xFF);
        if (components > 3)
          map[3][i] = (byte) (255-((data[i]>>24) & 0xFF));
      }
    return map;
  }

  /** This method determines the number of independent color components in the image.
      This will be either 1 (monochrome), 3 (RGB), or 4 (ARGB). */

  private void countComponents(int data[])
  {
    components = 1;
    for (int i = 0; i < data.length; i++)
      {
        int j = data[i];
        if ((j & 0xFF000000) != 0xFF000000)
          {
            components = 4;
            return;
          }
        int k = j & 0xFF;
        if ((((j>>8)&0xFF) != k) || (((j>>16)&0xFF) != k))
          {
            components = 3;
          }
      }
  }

  /** This method calculates the average value for each component over the entire image. */

  private void findAverage()
  {
    byte map[][] = maps[maps.length-1];
    long av[] = new long [components];
    int i, j, len = map[0].length;

    for (i = 0; i < len; i++)
      for (j = 0; j < components; j++)
        av[j] += (map[j][i] & 0xFF);
    average = new float [components];
    for (i = 0; i < components; i++)
      average[i] = (av[i]/len)*SCALE;
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
    return components;
  }

  /** Get the value of a single component at a particular location in the image.  The value
      is represented as a float between 0.0 and 1.0.  The components are:

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
      return maps[maps.length-1][component][0]*SCALE;

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
    byte map[] = maps[which][component];

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

    return ((((int) map[ind1]) & 0xFF)*w1 + (((int) map[ind2]) & 0xFF)*w2 +
                    (((int) map[ind3]) & 0xFF)*w3 + (((int) map[ind4]) & 0xFF)*w4)*SCALE;
  }

  /** Get the average value for a particular component, over the entire image. */

  public float getAverageComponent(int component)
  {
    if (component >= components)
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
    float frac1, frac2, w1, w2, w3, w4, red, green, blue;
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

    red = ((((int) map[0][ind1]) & 0xFF)*w1 + (((int) map[0][ind2]) & 0xFF)*w2 +
                    (((int) map[0][ind3]) & 0xFF)*w3 + (((int) map[0][ind4]) & 0xFF)*w4)*SCALE;
    if (components == 1)
      green = blue = red;
    else
      {
        green = ((((int) map[1][ind1]) & 0xFF)*w1 + (((int) map[1][ind2]) & 0xFF)*w2 +
                    (((int) map[1][ind3]) & 0xFF)*w3 + (((int) map[1][ind4]) & 0xFF)*w4)*SCALE;
        blue = ((((int) map[2][ind1]) & 0xFF)*w1 + (((int) map[2][ind2]) & 0xFF)*w2 +
                    (((int) map[2][ind3]) & 0xFF)*w3 + (((int) map[2][ind4]) & 0xFF)*w4)*SCALE;
      }
    theColor.setRGB(red, green, blue);
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
        grad.y = -grad.y;
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
    byte map[] = maps[which][component];

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

    v1 = (double) (((int) map[ind1]) & 0xFF);
    v2 = (double) (((int) map[ind2]) & 0xFF);
    v3 = (double) (((int) map[ind3]) & 0xFF);
    v4 = (double) (((int) map[ind4]) & 0xFF);

    // Calculate the final value.

    grad.x = ((v3-v1)*(1.0-frac2) + (v4-v2)*frac2) * gradXScale[which];
    grad.y = ((v2-v1)*(1.0-frac1) + (v4-v3)*frac1) * gradYScale[which];
  }

  /** Get a scaled down copy of the image, to use for previews.  This Image will be no larger
      (but may be smaller) than PREVIEW_WIDTH by PREVIEW_HEIGHT. */

  public Image getPreview()
  {
    return preview;
  }

  /** Reconstruct an image from its serialized representation. */

  public MIPMappedImage(DataInputStream in) throws IOException, InvalidObjectException
  {
    this(in, in.readShort());
  }

  /** Reconstruct an image from its serialized representation.  This constructor is provided only for
      compatibility with versions of AoI prior to 1.4, which did not write image version information
      to the stream. */

  public MIPMappedImage(DataInputStream in, short version) throws IOException, InvalidObjectException
  {
    if (version < 0 || version > 1)
      throw new InvalidObjectException("Illegal version for MIPMappedImage");
    int w, h;
    Image im;
    if (version == 0)
    {
        w = in.readInt();
        h = in.readInt();
        components = in.readInt();
        byte map[][] = new byte [components][];
        for (int i = 0; i < components; i++)
          {
            map[i] = new byte [w*h];
            in.readFully(map[i]);
          }

        // Create a representation of the image in the default color model.

        int data[] = new int [w*h];
        if (components == 1)
          for (int i = 0; i < data.length; i++)
            data[i] = 0xFF000000 + ((map[0][i]<<16)&0xFF0000) + ((map[0][i]<<8)&0xFF00) + (map[0][i]&0xFF);
        else if (components == 3)
          for (int i = 0; i < data.length; i++)
            data[i] = 0xFF000000 + ((map[0][i]<<16)&0xFF0000) + ((map[1][i]<<8)&0xFF00) + (map[2][i]&0xFF);
        else
          for (int i = 0; i < data.length; i++)
            data[i] = ((map[3][i]<<24)&0xFF000000) + ((map[0][i]<<16)&0xFF0000) + ((map[1][i]<<8)&0xFF00) + (map[2][i]&0xFF);
        MemoryImageSource src = new MemoryImageSource(w, h, data, 0, w);
        im = Toolkit.getDefaultToolkit().createImage(src);
    }
    else
    {
      byte imageData[] = new byte [in.readInt()];
      in.readFully(imageData);
      im = ImageIO.read(new ByteArrayInputStream(imageData));
      w = im.getWidth(null);
      h = im.getHeight(null);
    }

    // Construct the mipmaps, preview image, etc.

    try
      {
        Frame fr = new Frame();
        buildMipMaps(im);
        fr.dispose();
      }
    catch (InterruptedException ex)
      {
        throw(new IOException());
      }
    if (w <= PREVIEW_WIDTH && h <= PREVIEW_HEIGHT)
      preview = im;
    else
      {
        if (w < h)
          preview = im.getScaledInstance(-1, PREVIEW_HEIGHT, Image.SCALE_DEFAULT);
        else
          preview = im.getScaledInstance(PREVIEW_WIDTH, -1, Image.SCALE_DEFAULT);
      }
    findAverage();
  }

  /** Serialize an image to an output stream. */

  public void writeToStream(DataOutputStream out) throws IOException
  {
    // Copy the image data into a BufferedImage.

    int w = getWidth(), h = getHeight();
    BufferedImage bi;
    if (getComponentCount() == 1)
    {
      bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
      for (int i = 0; i < w; i++)
        for (int j = 0; j < h; j++)
        {
          int index = i+j*w;
          bi.setRGB(i, j, 0xFF000000+(maps[0][0][index]&0xFF)*0x010101);
        }
    }
    else if (getComponentCount() == 3)
    {
      bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      for (int i = 0; i < w; i++)
        for (int j = 0; j < h; j++)
        {
          int index = i+j*w;
          bi.setRGB(i, j, 0xFF000000+((maps[0][0][index]&0xFF)<<16)+((maps[0][1][index]&0xFF)<<8)+(maps[0][2][index]&0xFF));
        }
    }
    else
    {
      bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      for (int i = 0; i < w; i++)
        for (int j = 0; j < h; j++)
        {
          int index = i+j*w;
          bi.setRGB(i, j, ((255-(maps[0][3][index]&0xFF))<<24)+((maps[0][0][index]&0xFF)<<16)+((maps[0][1][index]&0xFF)<<8)+(maps[0][2][index]&0xFF));
        }
    }
    out.writeShort(1);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bi, "png", baos);
    out.writeInt(baos.size());
    out.write(baos.toByteArray());
  }
}
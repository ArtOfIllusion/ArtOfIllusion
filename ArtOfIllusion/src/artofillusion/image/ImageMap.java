/* Copyright (C) 2001-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.math.*;
import java.awt.*;
import java.io.*;

/** ImageMap represents an image which can be used for texturing an object.  The number of
components can range from one (monochrome) to four (ARGB).  It also provides a scaled down
Image object which can be used to preview the image.
<p>
This is an abstract class.  Subclasses implement specific ways of storing images. */

public abstract class ImageMap
{
  private int id;
  
  public static final int PREVIEW_WIDTH = 50;
  public static final int PREVIEW_HEIGHT = 50;

  private static int nextID;

  public ImageMap()
  {
    id = nextID++;
  }

  /** Construct an appropriate subclass of ImageMap from an image file. */

  public static ImageMap loadImage(File file) throws Exception
  {
    String name = file.getName().toLowerCase();
    if (name.endsWith(".hdr") || name.endsWith(".pic"))
      {
        try
          {
            return HDRDecoder.createImage(file);
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
          }
      }
    if (name.endsWith(".svg"))
      return new SVGImage(file);
    return new MIPMappedImage(file);
  }
  
  /** Get the width of the image. */
  
  public abstract int getWidth();
  
  /** Get the height of the image. */
  
  public abstract int getHeight();

  /** Get the number of components in the image. */
  
  public abstract int getComponentCount();

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
  
  public abstract float getComponent(int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize);

  /** Get the average value for a particular component, over the entire image. */
  
  public abstract float getAverageComponent(int component);

  /** Get the color at a particular location.  The location is specified by x and y, 
      which must lie between 0 and 1.  The color is averaged over a region of width 
      (xsize, ysize).  wrapx and wrapy specify whether, for purposes of interpolation, the 
      image should be treated as wrapping around so that opposite edges touch each other. */

  public abstract void getColor(RGBColor theColor, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize);
  
  /** Get the gradient of a single component at a particular location in the image.  
      The location is specified by x and y, which must lie between 0 and 1.  The value is
      averaged over a region of width (xsize, ysize) before the gradient is calculated.  
      wrapx and wrapy specify whether, for purposes of interpolation, the image should be 
      treated as wrapping around so that opposite edges touch each other. */
  
  public abstract void getGradient(Vec2 grad, int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize);

  /** Get a scaled down copy of the image, to use for previews.  This Image will be no larger
      (but may be smaller) than PREVIEW_WIDTH by PREVIEW_HEIGHT. */
  
  public abstract Image getPreview();

  /** Get an ID number which is unique (within this session) for this image. */
  
  public int getID()
  {
    return id;
  }

  /** Write out the object's representation to an output stream.  Every ImageMap subclass must also
      define a constructor of the form
      
      public ImageMapSubclass(DataInputStream in) throws IOException, InvalidObjectException
      
      which reconstructs an image from its serialized representation. */

  public abstract void writeToStream(DataOutputStream out) throws IOException;
}
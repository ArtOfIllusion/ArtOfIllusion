/* Copyright (C) 2001-2002 by Peter Eastman
   Modifications copyright (C) 2017 by Petri Ihalainen
   Changes copyright (C) 2020-2022 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.Scene;
import artofillusion.math.*;
import java.awt.*;
import java.io.*;
import java.util.Date;

/** ImageMap represents an image which can be used for texturing an object.  The number of
components can range from one (monochrome) to four (ARGB).  It also provides a scaled down
Image object which can be used to preview the image.
<p>
This is an abstract class.  Subclasses implement specific ways of storing images. */

public abstract class ImageMap
{
  protected String imageName   = new String();
  protected String userCreated = new String();
  protected Date   dateCreated;
  protected String zoneCreated = new String();
  protected String userEdited  = new String();
  protected Date   dateEdited;
  protected String zoneEdited  = new String();

  /** @deprecated */
  @Deprecated
  public static final int PREVIEW_WIDTH = 50;
  /** @deprecated */
  @Deprecated
  public static final int PREVIEW_HEIGHT = 50;
  public static final int PREVIEW_SIZE_DEFAULT = 50;
  public static final int PREVIEW_SIZE_TEMPLATE = 256;

  public static final String IMAGE_TYPE_RGB  = "RGB";
  public static final String IMAGE_TYPE_RGBA = "RGBA";
  public static final String IMAGE_TYPE_GRAY = "GRAY";
  public static final String IMAGE_TYPE_SVG  = "SVG";
  public static final String IMAGE_TYPE_HDR  = "HDR";
  
  private int id;
  private static int nextID;

  public ImageMap()
  {
    id = nextID++;
  }

  /** Construct an appropriate subclass of ImageMap from an image file. */

  public static ImageMap loadImage(File file) throws Exception
  {
    String name = file.getName().toLowerCase();
    if (name.endsWith(".hdr") || name.endsWith(".hdri") || name.endsWith(".pic"))
    {
      try
      {
        ImageMap im = HDRDecoder.createImage(file);
        im.setDataCreated(file);
        return im;
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

  /** Get the aspect ratio as width/height float number. */
  
  public abstract float getAspectRatio();

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

  /** Get a scaled down copy of the image, to use for previews. <p>
      
      If a preview image has been created already thst image will be returned<br>.
      If there is no existing perview image a default size preview image will b returned. 
      This Image will be no larger (but may be smaller) than PREVIEW_WIDTH by PREVIEW_HEIGHT. */
  
  public abstract Image getPreview();

  /** Get a scaled down copy of the image, to use for previews.  This Image will be no larger
      (but may be smaller) than 'size' by 'size'. */
  
  public abstract Image getPreview(int size);

  /** Get an ID number which is unique (within this session) for this image. */
  
  public int getID()
  {
    return id;
  }

  /** Set all creation time metadata */

  protected void setDataCreated(File file)
  {
    String fileName = file.getName();
    imageName = fileName.substring(0, fileName.lastIndexOf('.'));
    
    userCreated = System.getProperty("user.name");
    zoneCreated = System.getProperty("user.timezone");
    dateCreated = new Date();
    
    userEdited = userCreated;
    zoneEdited = zoneCreated;
    dateEdited = (Date)(dateCreated.clone());
  }

  /** Set all last edition time metadata */

  protected void setDataEdited()
  {
    userEdited = System.getProperty("user.name");
    zoneEdited = System.getProperty("user.timezone");
    dateEdited = new Date();
  }

  /** Get the creation date of this image. */

  public Date getDateCreated()
  {
    return dateCreated;
  }

  /** Get the timezone whre this image was created. */

  public String getZoneCreated()
  {
    return zoneCreated;
  }

  /** Get the username who created this image. */

  public String getUserCreated()
  {
    return userCreated;
  }

  /** Get the last editing date of this image. */

  public Date getDateEdited()
  {
    return dateEdited;
  }

  /** Get the timezone where this image was last edited. */

  public String getZoneEdited()
  {
    return zoneEdited;
  }

  /** Get the username who last edited this image. */

  public String getUserEdited()
  {
    return userEdited;
  }

  /** Get the name of the image. */

  public String getName()
  {
    return imageName;
  }

  /** Override this to get the linked file, if any. */

  public File getFile()
  {
    return null;
  }

  /** Override this to get the image type string. <p>
  
      The type may be one of RGB, RGBA, GRAY, SVG, HDR */

  public String getType()
  {
    return new String();
  }

  /** Set the name of the image and update editing time metadata. */

  public void setName(String newName)
  {
    imageName = newName;
    setDataEdited();
  }

  /** Write out the object's representation to an output stream.  Every ImageMap subclass must also
      define a constructor of the form
      
      <pre>public ImageMapSubclass(DataInputStream in) throws IOException, InvalidObjectException</pre>
      
      which reconstructs an image from its serialized representation. */

  public abstract void writeToStream(DataOutputStream out, Scene scene) throws IOException;
}
/* Copyright (C) 2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.math.*;
import artofillusion.*;
import artofillusion.animation.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

/**
 * A ReferenceImage is an object that displays an image for use during modelling.  When drawn
 * in a ViewerCanvas, it is a rectangle with the image mapped to it, but when rendered by a
 * Renderer, it is invisible.
 */

public class ReferenceImage extends Object3D
{
  private double width, height;
  private Image image;

  /**
   * Create a ReferenceImage whose width and height are both 1.0, and with no image set.
   */

  public ReferenceImage()
  {
    width = 1.0;
    height = 1.0;
  }

  public ReferenceImage(Image image)
  {
    this.image = image;
    width = 0.01*image.getWidth(null);
    height = 0.01*image.getHeight(null);
  }

  /**
   * Get the Image displayed by this object.
   */

  public Image getImage()
  {
    return image;
  }

  /**
   * Set the Image displayed by this object.
   */

  public void setImage(Image image)
  {
    this.image = image;
  }


  public boolean canSetTexture()
  {
    return false;
  }

  public Object3D duplicate()
  {
    ReferenceImage ri = new ReferenceImage();
    ri.image = image;
    ri.width = width;
    ri.height = height;
    return ri;
  }

  public void copyObject(Object3D obj)
  {
    ReferenceImage ri = (ReferenceImage) obj;
    image = ri.image;
    width = ri.width;
    height = ri.height;
  }

  public BoundingBox getBounds()
  {
    return new BoundingBox(-0.5*width, 0.5*width, -0.5*height, 0.5*height, 0.0, 0.0);
  }

  public void setSize(double xsize, double ysize, double zsize)
  {
    width = xsize;
    height = ysize;
  }

  public WireframeMesh getWireframeMesh()
  {
    Vec3 vert[] = new Vec3[] {new Vec3(-0.5*width, 0.5*height, 0.0), new Vec3(0.5*width, 0.5*height, 0.0),
        new Vec3(0.5*width, -0.5*height, 0.0), new Vec3(-0.5*width, -0.5*height, 0.0)};
    int from[] = new int[] {0, 1, 2, 3};
    int to[] = new int[] {1, 2, 3, 0};
    return new WireframeMesh(vert, from, to);
  }

  public Keyframe getPoseKeyframe()
  {
    return new NullKeyframe();
  }

  public void applyPoseKeyframe(Keyframe k)
  {
  }

  /**
   * This method is overridden to render the reference image into the ViewerCanvas.
   */

  public void renderObject(ObjectInfo obj, ViewerCanvas canvas, Vec3 viewDir)
  {
    if (image == null)
      super.renderObject(obj, canvas, viewDir);
    else
      canvas.renderImage(image, new Vec3(-0.5*width, -0.5*height, 0.0), new Vec3(0.5*width, -0.5*height, 0.0),
        new Vec3(0.5*width, 0.5*height, 0.0), new Vec3(-0.5*width, 0.5*height, 0.0));
  }

  /**
   * Reconstruct a ReferenceImage that was saved to a file.
   */

  public ReferenceImage(DataInputStream in, Scene theScene) throws IOException
  {
    super(in, theScene);
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    width = in.readDouble();
    height = in.readDouble();
    byte imageData[] = new byte [in.readInt()];
    in.readFully(imageData);
    image = ImageIO.read(new ByteArrayInputStream(imageData));
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);
    out.writeShort(0);
    out.writeDouble(width);
    out.writeDouble(height);

    // Save the image to the file.

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (image instanceof RenderedImage)
      ImageIO.write((RenderedImage) image, "png", baos);
    else
    {
      // We need to copy it into a BufferedImage.

      BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = bi.createGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
      ImageIO.write(bi, "png", baos);
    }
    out.writeInt(baos.size());
    out.write(baos.toByteArray());
  }
}

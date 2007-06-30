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

import java.awt.*;

/**
 * A ReferenceImage is an object that displays an image for use during modelling.  When drawn
 * in a ViewerCanvas, it is a rectangle with the image mapped to it, but when rendered by a
 * Renderer, it is invisible.
 */

public class ReferenceImage extends Object3D
{
  private double width, height;
  private Image image;

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
}

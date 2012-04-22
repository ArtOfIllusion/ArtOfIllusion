/* Copyright (C) 2002-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.math.*;
import artofillusion.object.*;

/** This is a distortion which changes the size of an object. */

public class ScaleDistortion extends Distortion
{
  private double xscale, yscale, zscale;
  private Mat4 preTransform, postTransform;

  public ScaleDistortion(double xscale, double yscale, double zscale, Mat4 preTransform, Mat4 postTransform)
  {
    this.xscale = xscale;
    this.yscale = yscale;
    this.zscale = zscale;
    this.preTransform = preTransform;
    this.postTransform = postTransform;
  }

  /** Determine whether this distortion is identical to another one. */
  
  public boolean isIdenticalTo(Distortion d)
  {
    if (!(d instanceof ScaleDistortion))
      return false;
    ScaleDistortion s = (ScaleDistortion) d;
    if (previous != null && !previous.isIdenticalTo(s.previous))
      return false;
    if (previous == null && s.previous != null)
      return false;
    if (xscale != s.xscale || yscale != s.yscale || zscale != s.zscale)
      return false;
    if (preTransform == s.preTransform && postTransform == s.postTransform)
      return true;
    return (preTransform != null && preTransform.equals(s.preTransform) &&
      postTransform != null && postTransform.equals(s.postTransform));
  }
  
  /** Create a duplicate of this object. */
  
  public Distortion duplicate()
  {
    ScaleDistortion d = new ScaleDistortion(xscale, yscale, zscale, preTransform, postTransform);
    if (previous != null)
      d.previous = previous.duplicate();
    return d;
  }
  
  /** Apply the Distortion, and return a transformed mesh. */

  public Mesh transform(Mesh obj)
  {
    if (previous != null)
      obj = previous.transform(obj);
    Mesh newmesh = (Mesh) obj.duplicate();
    MeshVertex[] vert = newmesh.getVertices();
    Vec3 newvert[] = new Vec3 [vert.length];
    
    for (int i = 0; i < newvert.length; i++)
      {
        newvert[i] = vert[i].r;
        if (preTransform != null)
          preTransform.transform(newvert[i]);
        newvert[i].set(newvert[i].x*xscale, newvert[i].y*yscale, newvert[i].z*zscale);
        if (postTransform != null)
          postTransform.transform(newvert[i]);
      }
    newmesh.setVertexPositions(newvert);
    return newmesh;
  }
}
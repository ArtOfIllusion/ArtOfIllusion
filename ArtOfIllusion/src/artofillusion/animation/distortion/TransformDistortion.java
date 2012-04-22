/* Copyright (C) 2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.math.*;
import artofillusion.object.*;

/** This is a distortion that applies a transformation matrix to each vertex of a mesh. */

public class TransformDistortion extends Distortion
{
  private Mat4 transform;

  public TransformDistortion(Mat4 transform)
  {
    this.transform = transform;
  }

  /** Determine whether this distortion is identical to another one. */

  public boolean isIdenticalTo(Distortion d)
  {
    if (!(d instanceof TransformDistortion))
      return false;
    TransformDistortion t = (TransformDistortion) d;
    if (previous != null && !previous.isIdenticalTo(t.previous))
      return false;
    if (previous == null && t.previous != null)
      return false;
    if (transform == t.transform)
      return true;
    return (transform != null && transform.equals(t.transform));
  }

  /** Create a duplicate of this object. */

  public Distortion duplicate()
  {
    TransformDistortion d = new TransformDistortion(transform);
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
      transform.transform(newvert[i]);
    }
    newmesh.setVertexPositions(newvert);
    return newmesh;
  }
}
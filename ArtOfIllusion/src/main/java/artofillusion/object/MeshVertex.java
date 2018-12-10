/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.math.*;

/** MeshVertex represents a vertex in a mesh object. */

public class MeshVertex
{
  public Vec3 r;
  public int ikJoint;
  public double ikWeight;
  
  public MeshVertex(Vec3 p)
  {
    r = p;
    ikJoint = -1;
    ikWeight = 1.0;
  }
  
  public MeshVertex(MeshVertex v)
  {
    r = new Vec3(v.r);
    ikJoint = v.ikJoint;
    ikWeight = v.ikWeight;
  }
  
  /** Create a new MeshVertex which is a weighted average of two other ones. */
  
  public static MeshVertex blend(MeshVertex v1, MeshVertex v2, double w1, double w2)
  {
    MeshVertex v = new MeshVertex (new Vec3(w1*v1.r.x + w2*v2.r.x,
    	w1*v1.r.y + w2*v2.r.y, w1*v1.r.z + w2*v2.r.z));
    if (v1.ikJoint == v2.ikJoint)
      {
        v.ikJoint = v1.ikJoint;
        v.ikWeight = (w1*v1.ikWeight + w2*v2.ikWeight);
      }
    else if (v2.ikWeight > v1.ikWeight)
      {
        v.ikJoint = v2.ikJoint;
        v.ikWeight = v2.ikWeight;
      }
    else
      {
        v.ikJoint = v1.ikJoint;
        v.ikWeight = v1.ikWeight;
      }
    return v;
  }
}
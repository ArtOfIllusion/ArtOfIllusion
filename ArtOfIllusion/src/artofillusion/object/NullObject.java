/* Copyright (C) 1999-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import java.io.*;

/** NullObject represents an object which has no effect on how the scene is rendered. */

public class NullObject extends Object3D
{
  static BoundingBox bounds;
  static WireframeMesh mesh;

  static
  {
    Vec3 vert[];
    double r = 0.25, i, j, k;
    int ind1, ind2, from[], to[];

    bounds = new BoundingBox(-0.25, 0.25, -0.25, 0.25, -0.25, 0.25);
    vert = new Vec3 [6];
    from = new int [3];
    to = new int [3];
    vert[0] = new Vec3(r, 0.0, 0.0);
    vert[1] = new Vec3(-r, 0.0, 0.0);
    vert[2] = new Vec3(0.0, r, 0.0);
    vert[3] = new Vec3(0.0, -r, 0.0);
    vert[4] = new Vec3(0.0, 0.0, r);
    vert[5] = new Vec3(0.0, 0.0, -r);
    from[0] = 0;
    to[0] = 1;
    from[1] = 2;
    to[1] = 3;
    from[2] = 4;
    to[2] = 5;
    mesh = new WireframeMesh(vert, from, to);
  }
  
  public NullObject()
  {
  }
  
  public Object3D duplicate()
  {
    return new NullObject();
  }
  
  public void copyObject(Object3D obj)
  {
  }

  public BoundingBox getBounds()
  {
    return bounds;
  }

  /* A NullObject is always drawn the same size. */

  public void setSize(double xsize, double ysize, double zsize)
  {
  }

  public boolean canSetTexture()
  {
    return false;
  }
  
  public WireframeMesh getWireframeMesh()
  {
    return mesh;
  }

  public boolean isEditable()
  {
    return false;
  }

  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public NullObject(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(0);
  }
  
  /* Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new NullKeyframe();
  }
  
  /* Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
  }
}
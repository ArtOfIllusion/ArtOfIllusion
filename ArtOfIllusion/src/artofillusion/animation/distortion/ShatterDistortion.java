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
import artofillusion.texture.*;

import java.util.Random;

/** This is a distortion which shatters an object. */

public class ShatterDistortion extends Distortion
{
  private double time, size, speed, randomness, gravity, spin, disappear;
  private int gravityAxis;
  private Mat4 gravityDirTransform;

  public static final int X_AXIS = 0;
  public static final int Y_AXIS = 1;
  public static final int Z_AXIS = 2;
  
  public ShatterDistortion(double time, double size, double speed, double randomness, double gravity, double spin, double disappear, int gravityAxis, Mat4 gravityDirTransform)
  {
    this.time = time;
    this.size = size;
    this.speed = speed;
    this.randomness = randomness;
    this.gravity = gravity;
    this.spin = spin;
    this.disappear = disappear;
    this.gravityAxis = gravityAxis;
    this.gravityDirTransform = gravityDirTransform;
  }

  /** Determine whether this distortion is identical to another one. */
  
  public boolean isIdenticalTo(Distortion d)
  {
    if (!(d instanceof ShatterDistortion))
      return false;
    ShatterDistortion s = (ShatterDistortion) d;
    if (previous != null && !previous.isIdenticalTo(s.previous))
      return false;
    if (previous == null && s.previous != null)
      return false;
    if (time != s.time || size != s.size || speed != s.speed || randomness != s.randomness || gravity != s.gravity || spin != s.spin || disappear != s.disappear || gravityAxis != s.gravityAxis)
      return false;
    if (gravityDirTransform == s.gravityDirTransform)
      return true;
    return (gravityDirTransform != null && gravityDirTransform.equals(s.gravityDirTransform));
  }
  
  /** Create a duplicate of this object. */
  
  public Distortion duplicate()
  {
    ShatterDistortion d = new ShatterDistortion(time, size, speed, randomness, gravity, spin, disappear, gravityAxis, gravityDirTransform);
    if (previous != null)
      d.previous = previous.duplicate();
    return d;
  }
  
  /** Apply the Distortion, and return a transformed mesh. */

  public Mesh transform(Mesh obj)
  {
    if (previous != null)
      obj = previous.transform(obj);
    TriangleMesh mesh = (obj instanceof TriangleMesh ? (TriangleMesh) obj : ((Object3D) obj).convertToTriangleMesh(size));
    mesh = mesh.subdivideToLimit(size);
    TriangleMesh.Vertex vert[] = (TriangleMesh.Vertex []) mesh.getVertices();
    TriangleMesh.Face face[] = mesh.getFaces();
    double dist = speed*time;
    Vec3 gravityDisp, center = new Vec3(), disp = new Vec3();
    Random rand = new FastRandom(0l);
    int seed[] = null;

    // Determine which faces are actually visible.
    
    if (disappear > 0.0)
      {
        boolean visible[] = new boolean [face.length];
        int num = 0;
        for (int i = 0; i < visible.length; i++)
          if (disappear*rand.nextDouble() > time)
            {
              visible[i] = true;
              num++;
            }
        if (num == 0)
          return new TriangleMesh(new Vec3 [] {new Vec3()}, new int [0][0]);
        TriangleMesh.Face shownface[] = new TriangleMesh.Face [num];
        seed = new int [num];
        num = 0;
        for (int i = 0; i < visible.length; i++)
          if (visible[i])
            {
              seed[num] = i;
              shownface[num++] = face[i];
            }
        face = shownface;
      }
    else
      {
        seed = new int [face.length];
        for (int i = 0; i < seed.length; i++)
          seed[i] = i;
      }
    TriangleMesh.Vertex newvert[] = new TriangleMesh.Vertex [face.length*3];
    int newface[][] = new int [face.length][];
    
    // Find the effect of gravity.
    
    if (gravityAxis == X_AXIS)
      gravityDisp = new Vec3(-0.5*gravity*time*time, 0.0, 0.0);
    else if (gravityAxis == Y_AXIS)
      gravityDisp = new Vec3(0.0, -0.5*gravity*time*time, 0.0);
    else
      gravityDisp = new Vec3(0.0, 0.0, -0.5*gravity*time*time);
    if (gravityDirTransform != null)
      gravityDirTransform.transformDirection(gravityDisp);
    
    // Loop through the faces of the subdivided mesh.
    
    double third = 1.0/3.0;
    for (int i = 0; i < face.length; i++)
      {
        rand.setSeed((long) seed[i]);
        newvert[i*3] = mesh.new Vertex(vert[face[i].v1]);
        newvert[i*3+1] = mesh.new Vertex(vert[face[i].v2]);
        newvert[i*3+2] = mesh.new Vertex(vert[face[i].v3]);
        Vec3 v1 = newvert[i*3].r;
        Vec3 v2 = newvert[i*3+1].r;
        Vec3 v3 = newvert[i*3+2].r;
        newface[i] = new int [] {i*3, i*3+1, i*3+2};
        center.set(third*(v1.x+v2.x+v3.x), third*(v1.y+v2.y+v3.y), third*(v1.z+v2.z+v3.z));
        if (spin != 0.0)
          {
            Vec3 spinAxis = new Vec3(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
            spinAxis.normalize();
            Mat4 rotate = Mat4.axisRotation(spinAxis, time*spin*rand.nextDouble());
            v1.subtract(center);
            rotate.transformDirection(v1);
            v1.add(center);
            v2.subtract(center);
            rotate.transformDirection(v2);
            v2.add(center);
            v3.subtract(center);
            rotate.transformDirection(v3);
            v3.add(center);
          }
        disp.set(center.x+randomness*(rand.nextDouble()-0.5), center.y+randomness*(rand.nextDouble()-0.5), center.z+randomness*(rand.nextDouble()-0.5));
        disp.normalize();
        if (randomness != 0.0)
          {
            disp.scale(1.0-randomness);
            disp.x += randomness*(rand.nextDouble()-0.5);
            disp.y += randomness*(rand.nextDouble()-0.5);
            disp.z += randomness*(rand.nextDouble()-0.5);
          }
        disp.scale(dist);
        disp.add(gravityDisp);
        v1.add(disp);
        v2.add(disp);
        v3.add(disp);
      }
    mesh.setShape(newvert, newface);

    // Fix any texture parameters.

    ParameterValue param[] = mesh.getParameterValues();
    for (int i = 0; i < param.length; i++)
    {
      if (param[i] instanceof ConstantParameterValue || param[i] instanceof FaceParameterValue)
        continue;
      double value[][] = new double[face.length][3];
      for (int j = 0; j < face.length; j++)
      {
        value[j][0] = param[i].getValue(j, face[j].v1, face[j].v2, face[j].v3, 1.0, 0.0, 0.0);
        value[j][1] = param[i].getValue(j, face[j].v1, face[j].v2, face[j].v3, 0.0, 1.0, 0.0);
        value[j][2] = param[i].getValue(j, face[j].v1, face[j].v2, face[j].v3, 0.0, 0.0, 1.0);
      }
      param[i] = new FaceVertexParameterValue(value);
    }
    mesh.setParameterValues(param);
    return mesh;
  }
}
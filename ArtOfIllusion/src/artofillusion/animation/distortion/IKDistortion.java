/* Copyright (C) 2003-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;

/** This is a distortion which applies constraints to an object through inverse kinematics. */

public class IKDistortion extends Distortion
{
  private boolean locked[], moving[];
  private Vec3 target[];
  private double weight;
  private Actor actor;

  public IKDistortion(boolean locked[], Vec3 target[], double weight, Actor actor)
  {
    this.locked = locked;
    this.target = target;
    this.weight = weight;
    this.actor = actor;
    moving = new boolean [target.length];
    for (int i = 0; i < moving.length; i++)
      moving[i] = (target[i] != null);
  }

  /** Determine whether this distortion is identical to another one. */
  
  public boolean isIdenticalTo(Distortion d)
  {
    if (!(d instanceof IKDistortion))
      return false;
    IKDistortion s = (IKDistortion) d;
    if (previous != null && !previous.isIdenticalTo(s.previous))
      return false;
    if (previous == null && s.previous != null)
      return false;
    if (actor != s.actor)
      return false;
    if (weight != s.weight)
      return false;
    if (locked.length != s.locked.length)
      return false;
    for (int i = 0; i < locked.length; i++)
    {
      if (locked[i] != s.locked[i] || moving[i] != s.moving[i])
        return false;
      if (target[i] != null && !target[i].equals(s.target[i]))
        return false;
    }
    return true;
  }
  
  /** Create a duplicate of this object. */
  
  public Distortion duplicate()
  {
    IKDistortion d = new IKDistortion(locked, target, weight, actor);
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
    Skeleton skeleton = newmesh.getSkeleton();
    IKSolver ik = new IKSolver(skeleton, locked, moving);
    ik.solve(target, 1500);
    if (weight < 1.0)
    {
      Skeleton targetSkeleton = skeleton.duplicate();
      obj.getSkeleton().blend(skeleton, new Skeleton [] {targetSkeleton}, new double [] {weight});
    }
    if (actor != null)
      actor.shapeMeshFromGestures((Object3D) newmesh);
    else
      Skeleton.adjustMesh(obj, newmesh);
    return newmesh;
  }
}

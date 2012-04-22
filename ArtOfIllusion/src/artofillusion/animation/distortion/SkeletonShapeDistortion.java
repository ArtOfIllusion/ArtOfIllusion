/* Copyright (C) 2004-2012 by Peter Eastman

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

/** This is a distortion which reshapes an object's skeleton. */

public class SkeletonShapeDistortion extends Distortion
{
  private Skeleton skeleton;
  private double weight;
  private Actor actor;

  public SkeletonShapeDistortion(Skeleton skeleton, double weight, Actor actor)
  {
    this.skeleton = skeleton;
    this.weight = weight;
    this.actor = actor;
  }

  /** Determine whether this distortion is identical to another one. */
  
  public boolean isIdenticalTo(Distortion d)
  {
    if (!(d instanceof SkeletonShapeDistortion))
      return false;
    SkeletonShapeDistortion s = (SkeletonShapeDistortion) d;
    if (previous != null && !previous.isIdenticalTo(s.previous))
      return false;
    if (previous == null && s.previous != null)
      return false;
    if (actor != s.actor)
      return false;
    if (weight != s.weight)
      return false;
    if (!skeleton.equals(s.skeleton))
      return false;
    return true;
  }
  
  /** Create a duplicate of this object. */
  
  public Distortion duplicate()
  {
    SkeletonShapeDistortion d = new SkeletonShapeDistortion(skeleton, weight, actor);
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
    Skeleton meshSkeleton = newmesh.getSkeleton();
    obj.getSkeleton().blend(meshSkeleton, new Skeleton [] {skeleton}, new double [] {weight});
    if (actor != null)
      actor.shapeMeshFromGestures((Object3D) newmesh);
    else
      Skeleton.adjustMesh(obj, newmesh);
    return newmesh;
  }
}

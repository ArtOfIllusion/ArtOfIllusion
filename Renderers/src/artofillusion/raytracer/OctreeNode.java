/* Copyright (C) 1999-2015 by Peter Eastman
   Modification copyright (C) 2020 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import java.util.*;

/** This class represents a node in an octree, used for sorting the objects by location in
    the scene.  OctreeNodes can be terminal nodes, in which case they contain a list of 
    objects, or branch nodes, in which case they contain a list of other nodes.
    <p>
    This class is more sophisticated than most octrees.  When it subdivides a node, it does
    not simply cut it in half along each axis.  Instead, it tries to determine the optimal place
    to subdivide along each axis, based on the bounding boxes of the objects within the node. */
   
public class OctreeNode
{
  public OctreeNode parent, child[];
  public RTObject obj[];
  public float minx, maxx, miny, maxy, minz, maxz;
  public float midx, midy, midz;
  private double nodeTol, rayTol;
  
  private static final int CELLS = 64;
  private static final RTObject EMPTY_OBJECT_LIST[] = new RTObject[0];
  private static int leftCount[] = new int [CELLS+2];
  private static int rightCount[] = new int [CELLS+2];

  /** The constructor takes a bounding box, an array of objects, an array of bounding boxes
      of the objects, and a reference to its parent node.  treeDepth is the depth of this node 
      in the octree, and maxDepth is the maximum allowed depth. */
  
  public OctreeNode(float minx, float maxx, float miny, float maxy, float minz, float maxz, RTObject tri[], BoundingBox bb[], OctreeNode parentNode)
  {
    this.minx = minx;
    this.maxx = maxx;
    this.miny = miny;
    this.maxy = maxy;
    this.minz = minz;
    this.maxz = maxz;
    boolean inside[] = new boolean [tri.length];
    int count, i;
    parent = parentNode;

    nodeTol = maxx-minx;
    if (maxy-miny > nodeTol) nodeTol = maxy-miny;
    if (maxz-minz > nodeTol) nodeTol = maxz-minz;
    nodeTol *= Raytracer.TOL;
    rayTol = nodeTol*0.5; // Otherwise completely flat objects might be entirely missed.

    // Find which objects are actually inside this node.

    for (i = 0, count = 0; i < tri.length; i++)
      if (intersects(bb[i]))
        if (tri[i].intersectsNode(this))
        {
          inside[i] = true;
          count++;
        }
    if (count == 0)
    {
      obj = EMPTY_OBJECT_LIST;
      return;
    }
    
    // Build lists of the objects which are inside this node, and their bounding boxes.
    
    obj = new RTObject [count];
    BoundingBox objBounds[] = new BoundingBox [count];
    count = 0;
    for (i = 0; i < tri.length; i++)
      if (inside[i])
      {
        obj[count] = tri[i];
        objBounds[count++] = bb[i];
      }
    subdivide(objBounds);
  }

  /** Determine whether this node should be subdivided.  If so, create the child nodes.  Otherwise, mark it
      as a terminal node. */

  private void subdivide(BoundingBox objBounds[])
  {
    boolean splitx, splity, splitz;

    if (obj.length > 9)
      {
        findMidpoints(objBounds);
        splitx = (midx != maxx);
        splity = (midy != maxy);
        splitz = (midz != maxz);
        if (!(splitx || splity || splitz))
          return;
        child = new OctreeNode[8];
        child[0] = new OctreeNode(minx, midx, miny, midy, minz, midz, obj, objBounds, this);
        if (splitz)
          child[1] = new OctreeNode(minx, midx, miny, midy, midz, maxz, obj, objBounds, this);
        if (splity)
          {
            child[2] = new OctreeNode(minx, midx, midy, maxy, minz, midz, obj, objBounds, this);
            if (splitz)
              child[3] = new OctreeNode(minx, midx, midy, maxy, midz, maxz, obj, objBounds, this);
          }
        if (splitx)
          {
            child[4] = new OctreeNode(midx, maxx, miny, midy, minz, midz, obj, objBounds, this);
            if (splitz)
              child[5] = new OctreeNode(midx, maxx, miny, midy, midz, maxz, obj, objBounds, this);
            if (splity)
              {
                child[6] = new OctreeNode(midx, maxx, midy, maxy, minz, midz, obj, objBounds, this);
                if (splitz)
                  child[7] = new OctreeNode(midx, maxx, midy, maxy, midz, maxz, obj, objBounds, this);
              }
          }
        obj = null;
      }
  }

  /** Build a list of all child nodes. */

  public OctreeNode[] findChildNodes()
  {
    ArrayList<OctreeNode> nodes = new ArrayList<OctreeNode>();
    if (child != null)
      for (int i = 0; i < child.length; i++)
        if (child[i]!= null)
          nodes.add(child[i]);
    return nodes.toArray(new OctreeNode[nodes.size()]);
  }
  
  /** Get the list of objects in this node. */
  
  public RTObject [] getObjects()
  {
    return obj;
  }
  
  /** This method should be called on the root node of the octree.  Given a point, return the 
      terminal node which contains it.  If the point is not inside this node, return null. */
  
  public OctreeNode findNode(Vec3 pos)
  {
    OctreeNode current;

    if (!contains(pos))
      return null;

    current = this;
    while (current.obj == null)
      {
        if (pos.x > current.midx)
          {
            if (pos.y > current.midy)
              {
                if (pos.z > current.midz)
                    current = current.child[7];
                else
                    current = current.child[6];
            }
            else
              {
                if (pos.z > current.midz)
                    current = current.child[5];
                else
                    current = current.child[4];
              }
          }
        else
          {
            if (pos.y > current.midy)
              {
                if (pos.z > current.midz)
                    current = current.child[3];
                else
                    current = current.child[2];
              }
            else
              {
                if (pos.z > current.midz)
                    current = current.child[1];
                else
                    current = current.child[0];
              }
        }
      }
    return current;
  }

  /** Given a ray which passes through this node, find the next node it passes through.  If this is
      the last node it passes through, return null. */
  
  public OctreeNode findNextNode(Ray r)
  {
    if (parent == null)
      return null;

    double maxt = Double.MAX_VALUE;
    double dx = 0.0, dy = 0.0, dz = 0.0;
    Vec3 orig = r.getOrigin(), dir = r.getDirection();

    if (dir.x > rayTol)
    {
      dx = rayTol;
      double t = (maxx-orig.x)/dir.x;
      if (t < maxt)
        maxt = t;
    }
    else if (dir.x < -rayTol)
    {
      dx = -rayTol;
      double t = (minx-orig.x)/dir.x;
      if (t < maxt)
        maxt = t;
    }
    if (dir.y > rayTol)
    {
      dy = rayTol;
      double t = (maxy-orig.y)/dir.y;
      if (t < maxt)
        maxt = t;
    }
    else if (dir.y < -rayTol)
    {
      dy = -rayTol;
      double t = (miny-orig.y)/dir.y;
      if (t < maxt)
        maxt = t;
    }
    if (dir.z > rayTol)
    {
      dz = rayTol;
      double t = (maxz-orig.z)/dir.z;
      if (t < maxt)
        maxt = t;
    }
    else if (dir.z < -rayTol)
    {
      dz = -rayTol;
      double t = (minz-orig.z)/dir.z;
      if (t < maxt)
        maxt = t;
    }

    // Push it just outside this node, then move up the tree to find a node that
    // contains it. If the point is not outside the node, double the margins 
    // and retry until it is.

    OctreeNode current = parent;
    Vec3 nextPos = r.tempVec1;
    int attempt = 0;
    double tolScale;
    do
    {
      tolScale = Math.pow(2.0, attempt);
      nextPos.set(orig.x+dir.x*maxt+dx*tolScale, orig.y+dir.y*maxt+dy*tolScale, orig.z+dir.z*maxt+dz*tolScale);

      while (!current.contains(nextPos))
      {
        current = current.parent;
        if (current == null)
          return null;
      }
      
      // Now move back down the tree until we reach a terminal node.
      
      while (current.obj == null)
      {
        if (nextPos.x > current.midx)
        {
          if (nextPos.y > current.midy)
          {
            if (nextPos.z > current.midz)
              current = current.child[7];
            else
              current = current.child[6];
          }
          else
          {
            if (nextPos.z > current.midz)
              current = current.child[5];
            else
              current = current.child[4];
          }
        }
        else
        {
          if (nextPos.y > current.midy)
          {
            if (nextPos.z > current.midz)
              current = current.child[3];
            else
              current = current.child[2];
          }
          else
          {
            if (nextPos.z > current.midz)
              current = current.child[1];
            else
              current = current.child[0];
          }
        }
      }
      attempt++;
    }while (current == this && attempt < 64); // 64 like in the rice-on-a-chess-board problem.

    return current;
  }

  /** This method should be called on the root node of the octree.  Given a ray whose origin is
      outside the node, find the point where it enters the node, and return the terminal node which
      contains that point.  If the ray never intersects this node, return null. */
  
  public OctreeNode findFirstNode(Ray r)
  {
    double t1, t2, mint = -Double.MAX_VALUE, maxt = Double.MAX_VALUE;
    Vec3 orig = r.getOrigin(), dir = r.getDirection();

    // Find the point where the ray enters this node (if it does at all).

    if (dir.x == 0.0)
      {
        if (orig.x < minx || orig.x > maxx)
          return null;
      }
    else
      {
        t1 = (minx-orig.x)/dir.x;
        t2 = (maxx-orig.x)/dir.x;
        if (t1 < t2)
          {
            if (t1 > mint)
              mint = t1;
            if (t2 < maxt)
              maxt = t2;
          }
        else
          {
            if (t2 > mint)
              mint = t2;
            if (t1 < maxt)
              maxt = t1;
          }
        if (mint > maxt || maxt < 0.0)
          return null;
      }
    if (dir.y == 0.0)
      {
        if (orig.y < miny || orig.y > maxy)
          return null;
      }
    else
      {
        t1 = (miny-orig.y)/dir.y;
        t2 = (maxy-orig.y)/dir.y;
        if (t1 < t2)
          {
            if (t1 > mint)
              mint = t1;
            if (t2 < maxt)
              maxt = t2;
          }
        else
          {
            if (t2 > mint)
              mint = t2;
            if (t1 < maxt)
              maxt = t1;
          }
        if (mint > maxt || maxt < 0.0)
          return null;
      }
    if (dir.z == 0.0)
      {
        if (orig.z < minz || orig.z > maxz)
          return null;
      }
    else
      {
        t1 = (minz-orig.z)/dir.z;
        t2 = (maxz-orig.z)/dir.z;
        if (t1 < t2)
          {
            if (t1 > mint)
              mint = t1;
            if (t2 < maxt)
              maxt = t2;
          }
        else
          {
            if (t2 > mint)
              mint = t2;
            if (t1 < maxt)
              maxt = t1;
          }
        if (mint > maxt || maxt < 0.0)
          return null;
      }

    // Push it just inside this node.

    mint += nodeTol;
    Vec3 nextPos = r.tempVec1;
    nextPos.set(orig.x+dir.x*mint, orig.y+dir.y*mint, orig.z+dir.z*mint);
    
    // Return the terminal node which contains the point.

    return findNode(nextPos);
  }

  /** Analyze the distribution of objects inside this node, and determine the best place at which
      to subdivide it along each axis. */
  
  private void findMidpoints(BoundingBox objBounds[])
  {
    // If the box is much shorter along one axis than the other two, we don't want to subdivide 
    // along that axis, since it would slow down many more rays than it would speed up.

    float xsize = maxx-minx;
    float ysize = maxy-miny;
    float zsize = maxz-minz;

    double cutoff = (xsize > ysize ? xsize : ysize);
    if (zsize > cutoff)
      cutoff = zsize;
    cutoff *= 0.2;
    //if (cutoff < 1.0e-2) // This seems unnecessary. What if the scene is in small scale?
    //  cutoff = 1.0e-2;
    if (xsize > cutoff)
      midx = findAxisMidpoint(objBounds, 0);
    else
      midx = maxx;
    if (ysize > cutoff)
      midy = findAxisMidpoint(objBounds, 1);
    else
      midy = maxy;
    if (zsize > cutoff)
      midz = findAxisMidpoint(objBounds, 2);
    else
      midz = maxz;
  }

  
  private float findAxisMidpoint(BoundingBox objBounds[], int axis)
  {
    for (int i = 0; i < CELLS+2; i++)
      leftCount[i] = rightCount[i] = 0;
    float min = findMinimum(this, axis);
    float max = findMaximum(this, axis);
    float invwidth = CELLS/(max-min);
    for (int i = 0; i < objBounds.length; i++)
    {
      float objmin = findMinimum(objBounds[i], axis);
      float objmax = findMaximum(objBounds[i], axis);
      if (objmin <= min)
        leftCount[0]++;
      else
        leftCount[(int) ((objmin-min)*invwidth)+1]++;
      if (objmax < max)
        rightCount[(int) ((objmax-min)*invwidth)+1]++;
    }
    int numToLeft = 0;
    int numToRight = objBounds.length;
    int minCost = numToRight*(CELLS-16);
    int j = -1;
    for (int i = 0; i < CELLS; i++)
    {
      numToLeft += leftCount[i];
      numToRight -= rightCount[i];
      int cost = numToLeft*i + numToRight*(CELLS-i);
      if (cost < minCost)
      {
        minCost = cost;
        j = i;
      }
    }
    if (j == -1)
      return max;
    
    // We now have a reasonable position for the splitting plane, but we may be able to improve it
    // by making it flush with an object.

    float mid = min + j/invwidth;
    float limit = mid + 1.0f/invwidth;
    boolean found = false;
    for (int i = 0; i < objBounds.length; i++)
    {
      float objmin = findMinimum(objBounds[i], axis);
      if (objmin > mid && objmin < limit)
      {
        limit = objmin;
        found = true;
      }
    }
    if (found)
      return (float) (limit-nodeTol);
    limit = mid - 1.0f/invwidth;
    found = false;
    for (int i = 0; i < objBounds.length; i++)
    {
      float objmax = findMaximum(objBounds[i], axis);
      if (objmax < mid && objmax > limit)
      {
        limit = objmax;
        found = true;
      }
    }
    if (found)
      return (float) (limit+nodeTol);
    return mid;
  }

  /** Get a BoundingBox for this node. */
  
  public BoundingBox getBounds()
  {
    return new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
  }
  
  /** Determine whether the given point lies inside the node. */

  public boolean contains(Vec3 p)
  {
    if (p.x < minx || p.x > maxx || p.y < miny || p.y > maxy || p.z < minz || p.z > maxz)
      return false;
    return true;
  }

  /** Determine whether a bounding box intersects this node. */

  public boolean intersects(BoundingBox b)
  {
    if (minx > b.maxx || maxx < b.minx || miny > b.maxy || maxy < b.miny || minz > b.maxz || maxz < b.minz)
      return false;
    return true;
  }

  private static float findMinimum(OctreeNode box, int axis)
  {
    switch (axis)
    {
      case 0: return box.minx;
      case 1: return box.miny;
      default: return box.minz;
    }
  }
  
  private static float findMaximum(OctreeNode box, int axis)
  {
    switch (axis)
    {
      case 0: return box.maxx;
      case 1: return box.maxy;
      default: return box.maxz;
    }
  }

  private static float findMinimum(BoundingBox box, int axis)
  {
    double val;
    switch (axis)
    {
      case 0:
        val = box.minx;
        break;
      case 1:
        val = box.miny;
        break;
      default:
        val = box.minz;
    }
    return Math.nextAfter((float) val, Float.NEGATIVE_INFINITY);
  }
  
  private static float findMaximum(BoundingBox box, int axis)
  {
    double val;
    switch (axis)
    {
      case 0:
        val = box.maxx;
        break;
      case 1:
        val = box.maxy;
        break;
      default:
        val = box.maxz;
    }
    return Math.nextAfter((float) val, Float.POSITIVE_INFINITY);
  }
}
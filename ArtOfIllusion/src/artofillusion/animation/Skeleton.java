/* Copyright (C) 2001-2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.animation.Joint.DOF;
import artofillusion.math.*;
import artofillusion.object.*;
import java.awt.*;
import java.io.*;

/** This class represents the skeleton of an animated object. */

public class Skeleton
{
  private Joint joint[];
  private int nextID;
  
  private static final int MARKER_WIDTH = 10;
  private static final double BONE_WIDTH = 0.15;
  private static final double WIDEST_POINT = 0.8;
  
  public Skeleton()
  {
    joint = new Joint [0];
    nextID = 1;
  }
  
  /** Create an exact duplicate of this skeleton. */
  
  public Skeleton duplicate()
  {
    Skeleton s = new Skeleton();
    
    s.nextID = nextID;
    s.joint = new Joint [joint.length];
    for (int i = 0; i < joint.length; i++)
      s.joint[i] = joint[i].duplicate();
    for (int i = 0; i < joint.length; i++)
      {
        if (joint[i].parent != null)
          s.joint[i].parent = s.joint[s.findJointIndex(joint[i].parent.id)];
        s.joint[i].children  = new Joint [joint[i].children.length];
        for (int j = 0; j < joint[i].children.length; j++)
          s.joint[i].children[j] = s.joint[s.findJointIndex(joint[i].children[j].id)];
      }
    return s;
  }

  /** Make this skeleton idenical to another one. */
  
  public void copy(Skeleton s)
  {
    nextID = s.nextID;
    joint = new Joint [s.joint.length];
    for (int i = 0; i < joint.length; i++)
      joint[i] = s.joint[i].duplicate();
    for (int i = 0; i < joint.length; i++)
      {
        if (s.joint[i].parent != null)
          joint[i].parent = joint[findJointIndex(s.joint[i].parent.id)];
        joint[i].children  = new Joint [s.joint[i].children.length];
        for (int j = 0; j < joint[i].children.length; j++)
          joint[i].children[j] = joint[findJointIndex(s.joint[i].children[j].id)];
      }
  }

  /** Determine if this skeleton is identical to another one. */
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Skeleton))
      return false;
    Skeleton s = (Skeleton) o;
    if (joint.length != s.joint.length)
      return false;
    for (int i = 0; i < joint.length; i++)
      if (!joint[i].equals(s.joint[i]))
        return false;
    return true;
  }

  /** Add a joint to the skeleton. */
  
  public void addJoint(Joint j, int parentID)
  {
    Joint newjoint[] = new Joint [joint.length+1];
    System.arraycopy(joint, 0, newjoint, 0, joint.length);
    newjoint[joint.length] = j;
    joint = newjoint;
    if (j.id == -1)
      j.id = nextID++;
    int parentIndex = findJointIndex(parentID);
    if (parentIndex == -1)
      {
        j.parent = null;
        return;
      }
    Joint parent = joint[parentIndex];
    j.parent = parent;
    Joint newchildren[] = new Joint [parent.children.length+1];
    System.arraycopy(parent.children, 0, newchildren, 0, parent.children.length);
    newchildren[parent.children.length] = j;
    parent.children = newchildren;
  }
  
  /** Delete a joint from the skeleton.  If it has children, all of them will be deleted
      as well. */

  public void deleteJoint(int id)
  {
    int which = findJointIndex(id);
    Joint j = joint[which], parent = j.parent;
    while (j.children.length > 0)
      deleteJoint(j.children[0].id);
    which = findJointIndex(id);
    Joint newjoint[] = new Joint [joint.length-1];
    for (int i = 0, k = 0; i < joint.length; i++)
      if (i != which)
        newjoint[k++] = joint[i];
    joint = newjoint;
    if (parent == null)
      return;
    Joint newchildren[] = new Joint [parent.children.length-1];
    for (int i = 0, k = 0; i < parent.children.length; i++)
      if (parent.children[i] != j)
        newchildren[k++] = parent.children[i];
    parent.children = newchildren;
  }
  
  /** Add every joint from another skeleton to this one. */
  
  public void addAllJoints(Skeleton s)
  {
    Joint newjoint[] = new Joint [joint.length+s.joint.length];
    System.arraycopy(joint, 0, newjoint, 0, joint.length);
    for (int i = 0; i < s.joint.length; i++)
      newjoint[joint.length+i] = s.joint[i].duplicate();
    for (int i = 0; i < s.joint.length; i++)
    {
      Joint newj = newjoint[joint.length+i];
      Joint oldj = s.joint[i];
      newj.id = nextID++;
      if (oldj.parent != null)
        newj.parent = newjoint[joint.length+s.findJointIndex(oldj.parent.id)];
      newj.children = new Joint [oldj.children.length];
      for (int k = 0; k < newj.children.length; k++)
        newj.children[k] = newjoint[joint.length+s.findJointIndex(oldj.children[k].id)];
    }
    joint = newjoint;
  }
  
  /** Set the parent of a joint. */
  
  public void setJointParent(Joint j, Joint parent)
  {
    if (j.parent != null)
    {
      Joint newchildren[] = new Joint [j.parent.children.length-1];
      for (int i = 0, k = 0; i < j.parent.children.length; i++)
        if (j.parent.children[i] != j)
          newchildren[k++] = j.parent.children[i];
      j.parent.children = newchildren;
    }
    if (parent != null)
    {
      Joint newchildren[] = new Joint [parent.children.length+1];
      System.arraycopy(parent.children, 0, newchildren, 0, parent.children.length);
      newchildren[parent.children.length] = j;
      parent.children = newchildren;
    }
    j.parent = parent;
  }
  
  /** Find the array index for a given joint ID. */
  
  public int findJointIndex(int id)
  {
    int min = 0, max = joint.length-1, current = (min+max)>>1;
    
    if (joint.length == 0)
      return -1;
    while (true)
      {
        if (joint[current].id == id)
          return current;
        if (joint[current].id > id)
          max = current-1;
        else
          min = current+1;
        if (min >= max)
          {
            if (min < joint.length && joint[min].id == id)
              return min;
            return -1;
          }
        current = (min+max)>>1;
      }
  }
  
  /** Get the joint with the specified ID, or null if there is none. */
  
  public Joint getJoint(int id)
  {
    int which = findJointIndex(id);
    if (which < 0 || which >= joint.length)
      return null;
    return joint[which];
  }
  
  /** Get an array of all the joints. */
  
  public Joint [] getJoints()
  {
    Joint j[] = new Joint [joint.length];
    for (int i = 0; i < j.length; i++)
      j[i] = joint[i];
    return j;
  }
  
  /** Get the number of joints in the skeleton. */
  
  public int getNumJoints()
  {
    return joint.length;
  }
  
  /** Get the ID for the next joint to be added. */
  
  public int getNextJointID()
  {
    return nextID;
  }
  
  /** Scale the skeleton by the specified amount along each axis. */
  
  public void scale(double x, double y, double z)
  {
    for (int i = 0; i < joint.length; i++)
      {
        Vec3 pos = joint[i].coords.getOrigin();
        pos.x *= x;
        pos.y *= y;
        pos.z *= z;
        joint[i].coords.setOrigin(pos);
        Vec3 zdir = joint[i].coords.getZDirection();
        Vec3 newzdir = new Vec3(zdir.x*x, zdir.y*y, zdir.z*z);
        double len = newzdir.length();
        if (len > 0.0)
          zdir = newzdir.times(1.0/len);
        Vec3 updir = joint[i].coords.getUpDirection();
        Vec3 newupdir = new Vec3(updir.x*x, updir.y*y, updir.z*z);
        len = newupdir.length();
        if (len > 0.0)
          updir = newupdir.times(1.0/len);
        joint[i].coords.setOrientation(zdir, updir);
      }
    for (int i = 0; i < joint.length; i++)
      {
        if (joint[i].parent == null)
          joint[i].calcAnglesFromCoords(true);
        else
          joint[i].length.pos = joint[i].coords.getOrigin().distance(joint[i].parent.coords.getOrigin());
      }
  }
  
  /** Modify a Skeleton to be a weighted average of an arbitrary list of Skeletons,
      averaged about this one.
      @param average   the Skeleton to modify to be an average of other Skeletons
      @param s         the list of Skeletons to average
      @param weight    the weights for the different Skeletons
  */
  
  public void blend(Skeleton average, Skeleton s[], double weight[])
  {
    for (int i = 0; i < joint.length; i++)
    {
      Joint javg = average.getJoint(joint[i].id);
      Vec3 pos = null, newpos = null;
      double rootangles[] = null;
      if (joint[i].parent == null)
      {
        pos = joint[i].coords.getOrigin();
        newpos = new Vec3(pos);
        rootangles = joint[i].coords.getRotationAngles();
        javg.coords = joint[i].coords.duplicate();
      }
      
      // Find the average DOF values.

      javg.angle1.pos = joint[i].angle1.pos;
      javg.angle2.pos = joint[i].angle2.pos;
      javg.twist.pos = joint[i].twist.pos;
      javg.length.pos = joint[i].length.pos;
      for (int j = 0; j < s.length; j++)
      {
        Joint jother = s[j].getJoint(joint[i].id);
        if (jother == null)
          continue;
        javg.length.pos += weight[j]*findOffset(jother.length, joint[i].length);
        if (joint[i].parent == null)
        {
          newpos.add(jother.coords.getOrigin().minus(pos).times(weight[j]));
          double angles[] = jother.coords.getRotationAngles();
          RotationKeyframe rot = new RotationKeyframe(angles[0]-rootangles[0], angles[1]-rootangles[1], angles[2]-rootangles[2]);
          rot.applyToCoordinates(javg.coords, weight[j], null, null, true, true, true, true);
        }
        else
        {
          javg.angle1.pos += weight[j]*findOffset(jother.angle1, joint[i].angle1);
          javg.angle2.pos += weight[j]*findOffset(jother.angle2, joint[i].angle2);
          javg.twist.pos += weight[j]*findOffset(jother.twist, joint[i].twist);
        }
      }
      if (joint[i].parent == null)
      {
        javg.coords.setOrigin(newpos);
        double angles[] = javg.coords.getRotationAngles();
        javg.angle1.pos = angles[0];
        javg.angle2.pos = angles[1];
        javg.twist.pos = angles[2];
      }
      
      // Give each DOF a chance to clip the value.
      
      javg.angle1.set(javg.angle1.pos);
      javg.angle2.set(javg.angle2.pos);
      javg.twist.set(javg.twist.pos);
      javg.length.set(javg.length.pos);
    }
    
    // Update coordinate systems.
    
    for (int i = 0; i < average.joint.length; i++)
      if (average.joint[i].parent == null)
        average.joint[i].recalcCoords(true);
  }
  
  /** Utility routine for finding the angle offsets. */

  private double findOffset(DOF gesture, DOF defaultPose)
  {
    double diff = gesture.pos-defaultPose.pos;
    if (gesture.loop)
    {
      double range = gesture.max-gesture.min;
      while (diff > gesture.max)
        diff -= range;
      while (diff < gesture.min)
        diff += range;
    }
    return diff;
  }
  
  /** Draw the skeleton onto a canvas. */
  
  public void draw(MeshViewer view, boolean enabled)
  {
    Camera cam = view.getCamera();
    int mode = view.getRenderMode();
    boolean render = (mode != ViewerCanvas.RENDER_WIREFRAME && mode != ViewerCanvas.RENDER_TRANSPARENT && !(view.getCurrentTool() instanceof SkeletonTool));
    Vec2 p[] = new Vec2 [joint.length], v1 = new Vec2(), v2 = new Vec2();
    Point screenVert[] = new Point [joint.length], p1 = new Point(), p2 = new Point();
    double screenZ[] = new double [joint.length];
    Color color = (enabled ? ViewerCanvas.lineColor : ViewerCanvas.disabledColor), col;
    int colInt;
    
    // First calculate the positions of all the joints.

    for (int i = 0; i < joint.length; i++)
      {
        Joint j = joint[i];
        Vec3 pos = j.coords.getOrigin();
        p[i] = cam.getObjectToScreen().timesXY(pos);
        screenVert[i] = new Point((int) p[i].x, (int) p[i].y);
        screenZ[i] = cam.getObjectToView().timesZ(pos);
      }
    
    // Now draw the bones.
    
    col = color;
    Mat4 objToScreen = cam.getObjectToScreen();
    for (int i = 0; i < joint.length; i++)
      {
        Joint j = joint[i], parent = j.parent;
        if (parent == null)
          continue;
        int parentIndex = findJointIndex(parent.id);
        Vec3 zdir = j.coords.getOrigin().minus(parent.coords.getOrigin());
        double length = zdir.length();
        Vec3 xdir = j.coords.getUpDirection().times(BONE_WIDTH*length);
        Vec3 ydir = zdir.cross(xdir);
        ydir.normalize();
        ydir.scale(0.5*BONE_WIDTH*length);
        zdir.scale(WIDEST_POINT);
        Vec3 center = parent.coords.getOrigin().plus(zdir);
        Vec3 cx1 = center.plus(xdir), cx2 = center.minus(xdir);
        Vec3 cy1 = center.plus(ydir), cy2 = center.minus(ydir);
        if (render)
          {
            Vec3 pos[] = new Vec3 [] {
              cx1, cx2, cy1, cy2, joint[i].coords.getOrigin(), joint[parentIndex].coords.getOrigin()
            };
            for (int k = 1; k < pos.length; k++)
              for (int m = 0; m < k; m++)
                view.renderLine(pos[k], pos[m], cam, col);
          }
        else
          {
            Vec2 pos[] = new Vec2 [] {
                objToScreen.timesXY(cx1), objToScreen.timesXY(cx2),
                objToScreen.timesXY(cy1), objToScreen.timesXY(cy2),
                p[i], p[parentIndex]
            };
            for (int k = 1; k < pos.length; k++)
            {
              p1.x = (int) pos[k].x;
              p1.y = (int) pos[k].y;
              for (int m = 0; m < k; m++)
              {
                p2.x = (int) pos[m].x;
                p2.y = (int) pos[m].y;
                view.drawLine(p1, p2, col);
              }
            }
          }
      }

    // Finally draw the markers for all the joints.
    
    int selectedID = view.getSelectedJoint();
    boolean locked[] = view.getLockedJoints();
    for (int i = 0; i < joint.length; i++)
      {
        Joint j = joint[i];
        if (locked[i])
          col = ViewerCanvas.specialHighlightColor;
        else if (j.id == selectedID)
          col = ViewerCanvas.highlightColor;
        else
          col = color;
        if (render)
          {
            v1.x = v2.x = p[i].x;
            v1.y = p[i].y-MARKER_WIDTH;
            v2.y = p[i].y+MARKER_WIDTH;
            view.renderLine(v1, screenZ[i], v2, screenZ[i], cam, col);
            v1.y = v2.y = p[i].y;
            v1.x = p[i].x-MARKER_WIDTH;
            v2.x = p[i].x+MARKER_WIDTH;
            view.renderLine(v1, screenZ[i], v2, screenZ[i], cam, col);
          }
        else
          {
            p1.x = p2.x = screenVert[i].x;
            p1.y = screenVert[i].y-MARKER_WIDTH;
            p2.y = screenVert[i].y+MARKER_WIDTH;
            view.drawLine(p1, p2, col);
            p1.y = p2.y = screenVert[i].y;
            p1.x = screenVert[i].x-MARKER_WIDTH;
            p2.x = screenVert[i].x+MARKER_WIDTH;
            view.drawLine(p1, p2, col);
          }
      }
  }
  
  /** Update a mesh after its skeleton has moved.  oldMesh is the mesh before movement.
      newMesh is a duplicate of it with its skeleton in a different position.  This
      method repositions the vertices of newMesh based on the skeleton. */
  
  public static void adjustMesh(Mesh oldMesh, Mesh newMesh)
  {
    Skeleton s1 = oldMesh.getSkeleton(), s2 = newMesh.getSkeleton();
    MeshVertex v1[] = oldMesh.getVertices(), v2[] = newMesh.getVertices();
    Vec3 v[] = new Vec3 [v2.length];
    Vec3 temp = new Vec3();
    
    for (int i = 0; i < v2.length; i++)
      {
        v[i] = v2[i].r;
        if (v2[i].ikJoint == -1)
          continue;
        Joint j1 = s1.getJoint(v1[i].ikJoint), j2 = s2.getJoint(v2[i].ikJoint);
        if (j1 == null || j2 == null)
          continue;
        double weight = (j2.parent == null ? 1.0 : v2[i].ikWeight);
        v[i].set(v1[i].r);
        j1.coords.toLocal().transform(v[i]);
        j2.coords.fromLocal().transform(v[i]);
        if (weight < 1.0)
          {
            v[i].scale(weight);
            temp.set(v1[i].r);
            j1.parent.coords.toLocal().transform(temp);
            j2.parent.coords.fromLocal().transform(temp);
            temp.scale(1.0-weight);
            v[i].add(temp);
          }
        
        // Adjust the vertex positions to reduce the "squashing" effect around
        // bent joints.
        
        double olddist = v1[i].r.distance2(j1.coords.getOrigin());
        double newdist = v[i].distance2(j2.coords.getOrigin());
        if (olddist > 0.0 && newdist > 0.0)
          {
            v[i].subtract(j2.coords.getOrigin());
            v[i].scale(Math.pow(olddist/newdist, 0.5*v1[i].ikWeight));
            v[i].add(j2.coords.getOrigin());
          }
      }
    newMesh.setVertexPositions(v);
  }

  /** Write a serialized representation of this skeleton to a stream. */

  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeShort(0); // Version number
    out.writeInt(joint.length);
    for (int i = 0; i < joint.length; i++)
      {
        // Write the information about this joint.
        
        Joint j = joint[i];
        out.writeInt(j.id);
        out.writeUTF(j.name);
        j.coords.writeToFile(out);
        j.angle1.writeToStream(out);
        j.angle2.writeToStream(out);
        j.twist.writeToStream(out);
        j.length.writeToStream(out);
        out.writeInt(j.parent == null ? -1 : j.parent.id);
        out.writeInt(j.children.length);
        for (int k = 0; k < j.children.length; k++)
          out.writeInt(j.children[k].id);
      }
  }
  
  /** Reconstruct a skeleton from its serialized representation. */
  
  public Skeleton(DataInputStream in) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    nextID = 1;
    joint = new Joint [in.readInt()];
    int parentID[] = new int [joint.length];
    int childID[][] = new int [joint.length][];
    for (int i = 0; i < joint.length; i++)
      {
        // Read in the information about joints.
        
        int id = in.readInt();
        String name = in.readUTF();
        Joint j = new Joint(new CoordinateSystem(in), null, name);
        joint[i] = j;
        j.id = id;
        j.angle1 = j.new DOF(in);
        j.angle2 = j.new DOF(in);
        j.twist = j.new DOF(in);
        j.length = j.new DOF(in);
        j.length.loop = false;
        parentID[i] = in.readInt();
        childID[i] = new int [in.readInt()];
        for (int k = 0; k < childID[i].length; k++)
          childID[i][k] = in.readInt();
        if (j.id >= nextID)
          nextID = j.id+1;
      }
    
    // Assign the parents and children for each joint.
    
    for (int i = 0; i < joint.length; i++)
      {
        joint[i].parent = getJoint(parentID[i]);
        joint[i].children = new Joint [childID[i].length];
        for (int k = 0; k < childID[i].length; k++)
          joint[i].children[k] = getJoint(childID[i][k]);
      }
  }
}

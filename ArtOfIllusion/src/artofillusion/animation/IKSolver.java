/* Copyright (C) 2001-2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.math.*;
import java.util.*;

/** This class performs inverse kinematics calculations to move skeletons. */

public class IKSolver
{
  private Skeleton skeleton;
  private Joint joint[];
  private int behavior[], downstream[][];
  private boolean forbid[], forbidTwist[];
  private Vec3 originalPos[];
  private int dofIndex[][], jointBase[];
  private int numDOF, numNonFloating;

  private static final int FREE = 0;
  private static final int FIXED = 1;
  private static final int TARGET = 2;
  private static final int FLOATING = 3;
  
  /**
   * Create a new IKSolver for manipulating a skeleton
   *
   * @param s        the skeleton thie IKSolver will affect
   * @param locked   an array of size (# joints in skeleton) specifying which ones are locked in place
   * @param moving   an array of size (# joints in skeleton) specifying which ones are being moved
   */
  
  public IKSolver(Skeleton s, boolean locked[], boolean moving[])
  {
    skeleton = s;
    joint = s.getJoints();
    findDownstreamJoints();
    findJointBehaviors(locked, moving);
    findForbiddenDOF();
    originalPos = new Vec3 [joint.length];
    for (int i = 0; i < originalPos.length; i++)
      originalPos[i] = joint[i].coords.getOrigin();

    // Identify every degree of freedom.
    
    dofIndex = new int [joint.length][];
    for (int i = 0; i < joint.length; i++)
    {
      Joint j = joint[i];
      if (j.parent == null)
      {
        if (!forbid[i])
          dofIndex[i] = new int [] {numDOF++, numDOF++, numDOF++};
      }
      else
      {
        dofIndex[i] = new int [4];
        dofIndex[i][0] = (j.length.fixed || forbid[i] ? -1 : numDOF++);
        dofIndex[i][1] = (j.angle1.fixed || forbid[i] ? -1 : numDOF++);
        dofIndex[i][2] = (j.angle2.fixed || forbid[i] ? -1 : numDOF++);
        dofIndex[i][3] = (j.twist.fixed || forbidTwist[i] ? -1 : numDOF++);
      }
    }
    
    // Find the base indices for all the non-floating joints.
    
    jointBase = new int [joint.length];
    for (int i = 0; i < joint.length; i++)
    {
      if (behavior[i] == FLOATING)
        continue;
      jointBase[i] = numNonFloating*3;
      numNonFloating++;
    }
  }
  
  /**
   * Determine the behavior of every joint.
   *
   * @param locked   an array of size (# joints in skeleton) specifying which ones are locked in place
   * @param moving   an array of size (# joints in skeleton) specifying which ones are being moved
   */
  
  private void findJointBehaviors(boolean locked[], boolean moving[])
  {
    behavior = new int [locked.length];
    for (int i = 0; i < behavior.length; i++)
      behavior[i] = (moving[i] ? TARGET : FIXED);
    for (int i = 0; i < moving.length; i++)
      if (moving[i])
        tagFreeJoints(i, locked);
    boolean isFloating[] = new boolean [joint.length];
    for (int i = 0; i < joint.length; i++)
    {
      // If this joint and everything downstream from it is free, then mark it as floating.
      
      if (behavior[i] != FREE)
        continue;
      int k;
      for (k = 0; k < downstream[i].length; k++)
        if (behavior[downstream[i][k]] != FREE)
          break;
      if (k == downstream[i].length)
      {
        isFloating[i] = true;
        continue;
      }
      
      // Likewise, if everything upstream of it is free, then mark it as floating.
      
      Joint j = joint[i];
      Joint parent = joint[i].parent;
      while (parent != null && behavior[skeleton.findJointIndex(parent.id)] == FREE)
        parent = parent.parent;
      if (parent == null)
        isFloating[i] = true;
    }
    for (int i = 0; i < isFloating.length; i++)
      if (isFloating[i])
        behavior[i] = FLOATING;
  }
  
  /**
   * Recursively mark joints which are free to move.
   *
   * @param index    the index of the joint to start from
   * @param locked   an array of size (# joints in skeleton) specifying which ones are locked in place
   */
  
  private void tagFreeJoints(int index, boolean locked[])
  {
    if (behavior[index] == FIXED)
      behavior[index] = FREE;
    Joint j = joint[index];
    if (j.parent != null)
    {
      int parentIndex = skeleton.findJointIndex(j.parent.id);
      if (behavior[parentIndex] == FIXED && !locked[parentIndex])
        tagFreeJoints(parentIndex, locked);
    }
    for (int i = 0; i < j.children.length; i++)
    {
      int childIndex = skeleton.findJointIndex(j.children[i].id);
      if (behavior[childIndex] == FIXED && !locked[childIndex])
        tagFreeJoints(childIndex, locked);
    }
  }
  
  /**
   * For every joint, make a list of every other joint which is downstream of it.
   */
  
  private void findDownstreamJoints()
  {
    downstream = new int [joint.length][];
    for (int i = 0; i < joint.length; i++)
    {
      Vector v = new Vector();
      addDownstreamJoints(v, joint[i]);
      downstream[i] = new int [v.size()];
      for (int k = 0; k < downstream[i].length; k++)
        downstream[i][k] = skeleton.findJointIndex(((Joint) v.elementAt(k)).id);
    }
  }
  
  /**
   * Recursively find every joint which is downstream from another one.
   */
  
  private void addDownstreamJoints(Vector v, Joint j)
  {
    v.addElement(j);
    for (int k = 0; k < j.children.length; k++)
      addDownstreamJoints(v, j.children[k]);
  }
  
  /**
   * Find any degrees of freedom which are absolutely forbidden to change, since they would necessarily
   * cause locked joints to move.
   */
  
  private void findForbiddenDOF()
  {
    forbid = new boolean [joint.length];
    forbidTwist = new boolean [joint.length];
    for (int i = 0; i < joint.length; i++)
    {
      if (behavior[i] != FIXED)
        continue;
      if (joint[i].parent != null && behavior[skeleton.findJointIndex(joint[i].parent.id)] != FIXED)
        continue;
      
      // This joint and its parent are both fixed.  If its grandparent is also fixed, then its
      // angle1, angle2, and length degrees of freedom should be forbidden.
      
      if (joint[i].parent == null || joint[i].parent.parent == null || behavior[skeleton.findJointIndex(joint[i].parent.parent.id)] == FIXED)
        forbid[i] = true;
      
      // The twist degree of freedom works differently.  It should be forbidden if this joint,
      // its parent, and any of its children are fixed.
      
      for (int j = 0; j < joint[i].children.length; j++)
        if (behavior[skeleton.findJointIndex(joint[i].children[j].id)] == FIXED)
        {
          forbidTwist[i] = true;
          break;
        }
    }
  }

  /**
   * Calculate the force on each degree of freedom.
   *
   * @param target      an array of size (# joints in skeleton) specifying the target of each one (or null)
   * @param prevForce   the force on each degree of freedom in the previous step
   */
  
  private double [] calcForces(Vec3 target[], double prevForce[])
  {
    // Find a scale for each force based on the stiffness and comfort range for each joint.
    
    double dofScale[] = new double [numDOF];
    for (int i = 0; i < joint.length; i++)
    {
      Joint j = joint[i];
      if (j.parent == null)
        continue;
      if (dofIndex[i][0] > -1)
        dofScale[dofIndex[i][0]] = j.length.getForceScale(prevForce[dofIndex[i][0]]);
      if (dofIndex[i][1] > -1)
        dofScale[dofIndex[i][1]] = j.angle1.getForceScale(prevForce[dofIndex[i][1]]);
      if (dofIndex[i][2] > -1)
        dofScale[dofIndex[i][2]] = j.angle2.getForceScale(prevForce[dofIndex[i][2]]);
      if (dofIndex[i][3] > -1)
        dofScale[dofIndex[i][3]] = j.twist.getForceScale(prevForce[dofIndex[i][3]]);
    }

    // Find how every joint position is affected by every degree of freedom.
    
    double matrix[][] = new double [numNonFloating*3][numDOF];
    Vec3 temp = new Vec3();
    for (int i = 0; i < joint.length; i++)
    {
      // Determine the effect of this joint's degrees of freedom on the location of every other joint.
      
      Joint j = joint[i];
      if (j.parent == null)
      {
        // This is a base joint, so apply global translations to it.
        
        if (forbid[i])
          continue;
        if (behavior[i] != FLOATING)
        {
          matrix[jointBase[i]][dofIndex[i][0]] = 1.0;
          matrix[jointBase[i]+1][dofIndex[i][1]] = 1.0;
          matrix[jointBase[i]+2][dofIndex[i][2]] = 1.0;
        }
        for (int k = 0; k < downstream[i].length; k++)
        {
          int index = downstream[i][k];
          if (behavior[index] == FLOATING)
            continue;
          matrix[jointBase[index]][dofIndex[i][0]] = 1.0;
          matrix[jointBase[index]+1][dofIndex[i][1]] = 1.0;
          matrix[jointBase[index]+2][dofIndex[i][2]] = 1.0;
        }
        continue;
      }
      Vec3 zdir = j.coords.getZDirection();
      CoordinateSystem parentCoords = j.parent.coords;
      double c1 = Math.cos(j.angle1.pos*Math.PI/180.0), s1 = Math.sin(j.angle1.pos*Math.PI/180.0);
      double c2 = Math.cos(j.angle2.pos*Math.PI/180.0), s2 = Math.sin(j.angle2.pos*Math.PI/180.0);
      double ct = Math.cos(j.twist.pos*Math.PI/180.0), st = Math.sin(j.twist.pos*Math.PI/180.0);
      for (int k = 0; k < downstream[i].length; k++)
      {
        int index = downstream[i][k];
        if (behavior[index] == FLOATING)
          continue;
        int base = jointBase[index];
        Vec3 r = joint[index].coords.getOrigin().minus(parentCoords.getOrigin());
        r =  parentCoords.toLocal().timesDirection(r);
        r = j.getInverseTransform().timesDirection(r);
        if (!j.length.fixed && !forbid[i] && dofScale[dofIndex[i][0]] > 0.0)
        {
          temp = zdir.times(1.0/dofScale[dofIndex[i][0]]);
          matrix[base][dofIndex[i][0]] = temp.x;
          matrix[base+1][dofIndex[i][0]] = temp.y;
          matrix[base+2][dofIndex[i][0]] = temp.z;
        }
        if (!j.angle1.fixed && !forbid[i] && dofScale[dofIndex[i][1]] > 0.0)
        {
          temp = new Vec3(c1*s2*st*r.x + c1*s2*ct*r.y - s1*s2*r.z,
                -s1*st*r.x - s1*ct*r.y - c1*r.z,
                c1*c2*st*r.x + c1*c2*ct*r.y - s1*c2*r.z);
          parentCoords.fromLocal().transformDirection(temp);
          temp.scale(1.0/dofScale[dofIndex[i][1]]);
          matrix[base][dofIndex[i][1]] = temp.x;
          matrix[base+1][dofIndex[i][1]] = temp.y;
          matrix[base+2][dofIndex[i][1]] = temp.z;
        }
        if (!j.angle2.fixed && !forbid[i] && dofScale[dofIndex[i][2]] > 0.0)
        {
          temp = new Vec3((s1*c2*st-s2*ct)*r.x + (s1*c2*ct+s2*st)*r.y + c1*c2*r.z,
            0.0, -(c2*ct+s1*s2*st)*r.x + (c2*st-s1*s2*ct)*r.y - c1*s2*r.z);
          parentCoords.fromLocal().transformDirection(temp);
          temp.scale(1.0/dofScale[dofIndex[i][2]]);
          matrix[base][dofIndex[i][2]] = temp.x;
          matrix[base+1][dofIndex[i][2]] = temp.y;
          matrix[base+2][dofIndex[i][2]] = temp.z;
        }
        if (!j.twist.fixed && !forbidTwist[i] && dofScale[dofIndex[i][3]] > 0.0)
        {
          temp = new Vec3((s1*s2*ct-c2*st)*r.x - (c2*ct+s1*s2*st)*r.y,
            c1*ct*r.x - c1*st*r.y,
            (s1*c2*ct+s2*st)*r.x + (s2*ct-s1*c2*st)*r.y);
          parentCoords.fromLocal().transformDirection(temp);
          temp.scale(1.0/dofScale[dofIndex[i][3]]);
          matrix[base][dofIndex[i][3]] = temp.x;
          matrix[base+1][dofIndex[i][3]] = temp.y;
          matrix[base+2][dofIndex[i][3]] = temp.z;
        }
      }
    }

    // Construct the right hand side vector and solve the system of equations.
    
    double rhs[] = new double [Math.max(numDOF, numNonFloating*3)];
    for (int i = 0; i < joint.length; i++)
    {
      if (target[i] == null)
        continue;
      Vec3 pos = joint[i].coords.getOrigin();
      rhs[jointBase[i]] = target[i].x-pos.x;
      rhs[jointBase[i]+1] = target[i].y-pos.y;
      rhs[jointBase[i]+2] = target[i].z-pos.z;
    }
    SVD.solve(matrix, rhs, 0.01);

    // Convert angles to degrees and clip forces as necessary.
    
    for (int i = 0; i < joint.length; i++)
    {
      Joint j = joint[i];
      if (j.parent == null)
        continue;
      if (dofIndex[i][0] > -1)
        rhs[dofIndex[i][0]] = j.length.getClippedForce(rhs[dofIndex[i][0]]*dofScale[dofIndex[i][0]]);
      if (dofIndex[i][1] > -1)
        rhs[dofIndex[i][1]] = j.angle1.getClippedForce(rhs[dofIndex[i][1]]*dofScale[dofIndex[i][1]]*180.0/Math.PI);
      if (dofIndex[i][2] > -1)
        rhs[dofIndex[i][2]] = j.angle2.getClippedForce(rhs[dofIndex[i][2]]*dofScale[dofIndex[i][2]]*180.0/Math.PI);
      if (dofIndex[i][3] > -1)
        rhs[dofIndex[i][3]] = j.twist.getClippedForce(rhs[dofIndex[i][3]]*dofScale[dofIndex[i][3]]*180.0/Math.PI);
    }
    return rhs;
  }
  
  /**
   * Perform one step of moving the selected joints toward their target locations.
   *
   * @param force    the force acting on every degree of freedom
   * @param scale    an overall scale on how far to move in this step
   */

   private void step(double force[], double scale)
  {
    // Update the joint angles, applying cutoffs.
    
    for (int i = 0; i < joint.length; i++)
    {
      Joint j = joint[i];
      if (j.parent == null)
        continue;
      if (dofIndex[i][0] > -1)
        j.length.set(j.length.pos + force[dofIndex[i][0]]*scale);
      if (dofIndex[i][1] > -1)
        j.angle1.set(j.angle1.pos + force[dofIndex[i][1]]*scale);
      if (dofIndex[i][2] > -1)
        j.angle2.set(j.angle2.pos + force[dofIndex[i][2]]*scale);
      if (dofIndex[i][3] > -1)
        j.twist.set(j.twist.pos + force[dofIndex[i][3]]*scale);
    }
    
    // Apply global translations to each base joint and update the joint positions.
    
    for (int i = 0; i < joint.length; i++)
      if (joint[i].parent == null)
      {
        if (!forbid[i])
        {
          CoordinateSystem c = joint[i].coords;
          c.setOrigin(c.getOrigin().plus(new Vec3(force[dofIndex[i][0]]*scale, force[dofIndex[i][1]]*scale, force[dofIndex[i][2]]*scale)));
        }
        joint[i].recalcCoords(true);
      }
  }

  /**
   * Solve for the new joint positions, given the target positions.
   *
   * @param target    an array of size [# joints in skeleton] specifying the target of each one (or null)
   * @param maxSteps  the maximum number of iterations to perform
   * @return true if a solution was reached, false if it had not yet converged after maxSteps iterations
   */
  
  public boolean solve(Vec3 target[], int maxSteps)
  {
    Skeleton prevSkeleton = skeleton.duplicate();
    Joint prevJoint[] = prevSkeleton.getJoints();
    Vec3 currentTarget[] = new Vec3 [target.length];

    // Find the maximum distance of any joint from its target.
    
    double maxDist = 0.0;
    for (int i = 0; i < joint.length; i++)
      if (behavior[i] == TARGET)
      {
        double dist = target[i].minus(originalPos[i]).length();
        if (dist > maxDist)
          maxDist = dist;
      }

    // Iterate until the end positions converge.
    
    int count = 0;
    double scale = 0.1;
    double prevForce[] = new double [numDOF];
    while (count < maxSteps && scale*maxDist > 1e-4)
    {
      // Find a target for each joint in this iteration.
      
      for (int i = 0; i < joint.length; i++)
      {
        if (behavior[i] == FIXED)
          currentTarget[i] = originalPos[i];
        else if (behavior[i] == TARGET)
        {
          Vec3 pos = prevJoint[i].coords.getOrigin();
          currentTarget[i] = pos.plus(target[i].minus(pos).times(scale));
        }
      }
      
      // Calculate the force on every degree of freedom.
      
      Skeleton currentSkeleton = prevSkeleton.duplicate();
      joint = currentSkeleton.getJoints();
      double force[] = calcForces(currentTarget, prevForce);
      
      // Determine the correlation between the forces on this iteration and the previous one.
      // If they are highly correlated, increase the size of the step to accelerate convergence.

      double numerator = 0.0, denominator = 0.0;
      for (int i = 0; i < numDOF; i++)
      {
        double prod = force[i]*prevForce[i];
        numerator += prod;
        denominator += (prod > 0.0 ? prod : -prod);
      }
      double stepSize = 1.0+0.5*(denominator > 0.0 ? numerator/denominator : 0.0);
      step(force, stepSize);
      
      // See how far the joints moved.
      
      boolean error = false;
      boolean done = true;
      double fixedTol = 1e-6;
      double targetTol = 1e-8*scale*scale;
      for (int i = 0; i < joint.length; i++)
      {
        double distMoved2 = joint[i].coords.getOrigin().minus(prevJoint[i].coords.getOrigin()).length2();
        if (behavior[i] == FIXED)
        {
          double newDist2 = joint[i].coords.getOrigin().minus(originalPos[i]).length2();
          double oldDist2 = prevJoint[i].coords.getOrigin().minus(originalPos[i]).length2();
          if (newDist2 > fixedTol)
            error = true; // It moved too far.
        }
        if (behavior[i] == TARGET && distMoved2 > targetTol)
        {
          double newDist = joint[i].coords.getOrigin().minus(target[i]).length();
          double oldDist = prevJoint[i].coords.getOrigin().minus(target[i]).length();
          if (newDist > oldDist)
            error = true;
          else if (oldDist-newDist > 0.001*oldDist*scale)
            done = false; // It hasn't converged yet.
        }
      }
      if (error)
        scale *= 0.5;
      else
      {
        prevSkeleton = currentSkeleton;
        prevJoint = joint;
        prevForce = force;
        scale = (scale < 0.5 ? 2.0*scale : 1.0);
      }
      count++;
      if (!error && done)
        break;
    }
    
    // Copy over the results.
    
    skeleton.copy(prevSkeleton);
    return (count < maxSteps);
  }
}

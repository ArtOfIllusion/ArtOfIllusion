/* Copyright (C) 2004-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.texture.*;

/** This abstract class represents a Gesture for a Mesh. */

public abstract class MeshGesture implements Gesture
{
  /** Get the Mesh this Gesture belongs to. */
  
  protected abstract Mesh getMesh();
  
  /** Get the positions of all vertices in this Gesture. */
  
  protected abstract Vec3 [] getVertexPositions();
  
  /** Set the positions of all vertices in this Gesture. */
  
  protected abstract void setVertexPositions(Vec3 pos[]);
  
  /** Return a new keyframe which is the weighted average of an arbitrary list of keyframes,
      averaged about this pose. */

  public Gesture blend(Gesture p[], double weight[])
  {
    MeshGesture average = (MeshGesture) duplicate();
    MeshGesture p2[] = new MeshGesture [p.length];
    for (int i = 0; i < p.length; i++)
      p2[i] = (MeshGesture) p[i];
    blendSkeleton(average, p2, weight);
    blendSurface(average, p2, weight);
    return average;
  }

  /** Modify the skeleton of a Gesture to be a weighted average of an arbitrary list of Gestures,
      averaged about this pose.  This affects only the skeleton, not the vertex positions or
      texture parameters.
      @param average   the Gesture to modify to be an average of other Gestures
      @param p         the list of Gestures to average
      @param weight    the weights for the different Gestures
  */

  public void blendSkeleton(MeshGesture average, MeshGesture p[], double weight[])
  {
    if (getSkeleton() == null)
      return;
    Skeleton s[] = new Skeleton [p.length];
    for (int i = 0; i < s.length; i++)
      s[i] = p[i].getSkeleton();
    getSkeleton().blend(average.getSkeleton(), s, weight);
  }

  /** Modify the mesh surface of a Gesture to be a weighted average of an arbitrary list of Gestures,
      averaged about this pose.  This method only modifies the vertex positions and texture parameters,
      not the skeleton, and all vertex positions are based on the offsets from the joints they are
      bound to.
      @param average   the Gesture to modify to be an average of other Gestures
      @param p         the list of Gestures to average
      @param weight    the weights for the different Gestures
  */
  
  public void blendSurface(MeshGesture average, MeshGesture p[], double weight[])
  {
    Skeleton skeleton = getSkeleton();
    Joint joint[] = (skeleton == null ? null : skeleton.getJoints());
    Joint jt[] = (skeleton == null ? null : average.getSkeleton().getJoints());

    // Initialize the vertex positions.
    
    Vec3 temp = new Vec3();
    Mesh mesh = getMesh();
    MeshVertex vertex[] = mesh.getVertices();
    Vec3 vertPos[] = getVertexPositions();
    Vec3 avgPos[] = average.getVertexPositions();
    if (skeleton != null)
    {
      for (int j = 0; j < vertPos.length; j++)
      {
        MeshVertex v = vertex[j];
        int index = skeleton.findJointIndex(v.ikJoint);
        if (index == -1)
          continue;
        Joint j1 = joint[index], j2 = jt[index];
        double wt = (j2.parent == null ? 1.0 : v.ikWeight);
        avgPos[j].set(vertPos[j]);
        j1.coords.toLocal().transform(avgPos[j]);
        j2.coords.fromLocal().transform(avgPos[j]);
        if (wt < 1.0)
        {
          avgPos[j].scale(wt);
          temp.set(vertPos[j]);
          j1.parent.coords.toLocal().transform(temp);
          j2.parent.coords.fromLocal().transform(temp);
          temp.scale(1.0-wt);
          avgPos[j].add(temp);
        }
      }
    }

    // Now update the vertex positions and parameters.
    
    Vec3 temp1 = new Vec3(), temp2 = new Vec3();
    for (int i = 0; i < weight.length; i++)
    {
      MeshGesture key = (MeshGesture) p[i];
      Vec3 keyPos[] = key.getVertexPositions();
      for (int j = 0; j < vertPos.length; j++)
      {
        MeshVertex v = vertex[j];
        int index = (skeleton == null ? -1 : skeleton.findJointIndex(v.ikJoint));
        if (index == -1)
        {
          // This vertex is not bound to any joint.
          
          avgPos[j].add(keyPos[j].minus(vertPos[j]).times(weight[i]));
        }
        else
        {
          // Add up the offsets in the coordinate systems of the joints.
          
          Joint j1 = joint[index], j2 = jt[index], j3 = key.getSkeleton().getJoint(j1.id);
          double wt = (j1.parent == null ? 1.0 : v.ikWeight);
          temp1.set(keyPos[j]);
          temp2.set(vertPos[j]);
          j3.coords.toLocal().transform(temp1);
          j1.coords.toLocal().transform(temp2);
          temp1.subtract(temp2);
          j2.coords.fromLocal().transformDirection(temp1);
          temp1.scale(wt*weight[i]);
          avgPos[j].add(temp1);
          if (wt < 1.0)
          {
            temp1.set(keyPos[j]);
            temp2.set(vertPos[j]);
            j3.parent.coords.toLocal().transform(temp1);
            j1.parent.coords.toLocal().transform(temp2);
            temp1.subtract(temp2);
            j2.parent.coords.fromLocal().transformDirection(temp1);
            temp1.scale((1.0-wt)*weight[i]);
            avgPos[j].add(temp1);
          }
        }
      }
      TextureParameter params[] = mesh.getParameters();
      if (params != null)
      {
        for (int j = 0; j < params.length; j++)
        {
          ParameterValue val = average.getTextureParameter(params[j]);
          if (val instanceof ConstantParameterValue)
          {
            ConstantParameterValue tv = (ConstantParameterValue) val;
            ConstantParameterValue kv = (ConstantParameterValue) key.getTextureParameter(params[j]);
            tv.setValue(tv.getValue()+weight[i]*(kv.getValue()-tv.getValue()));
          }
          else if (val instanceof VertexParameterValue)
          {
            double tv[] = ((VertexParameterValue) val).getValue();
            double kv[] = ((VertexParameterValue) key.getTextureParameter(params[j])).getValue();
            for (int m = 0; m < tv.length; m++)
              tv[m] += weight[i]*(kv[m]-tv[m]);
            ((VertexParameterValue) val).setValue(tv);
          }
          else if (val instanceof FaceParameterValue)
          {
            double tv[] = ((FaceParameterValue) val).getValue();
            double kv[] = ((FaceParameterValue) key.getTextureParameter(params[j])).getValue();
            for (int m = 0; m < tv.length; m++)
              tv[m] += weight[i]*(kv[m]-tv[m]);
            ((FaceParameterValue) val).setValue(tv);
          }
          else if (val instanceof FaceVertexParameterValue)
          {
            FaceVertexParameterValue tv = (FaceVertexParameterValue) val;
            FaceVertexParameterValue kv = (FaceVertexParameterValue) getTextureParameter(params[j]);
            for (int m = 0; m < tv.getFaceCount(); m++)
              for (int n = 0; n < tv.getFaceVertexCount(m); n++)
                tv.setValue(m, n, tv.getValue(m, n)+weight[i]*(kv.getValue(m, n)-tv.getValue(m, n)));
          }
        }
      }
    }
  }
}

/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.texture.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;

/** The Curve class represents a continuous curve defined by a series of control vertices. 
    It may be either open or closed, and may either interpolate or approximate the control
    vertices.  There is also a smoothness parameter associated with each vertex. */

public class Curve extends Object3D implements Mesh
{
  protected MeshVertex vertex[];
  protected float smoothness[];
  protected boolean closed;
  protected int smoothingMethod;
  protected WireframeMesh cachedWire;
  protected BoundingBox bounds;

  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("menu.smoothingMethod"), new Object[] {
      Translate.text("menu.none"), Translate.text("menu.interpolating"), Translate.text("menu.approximating")
    }, Translate.text("menu.shading")),
    new Property(Translate.text("menu.closedEnds"), true)
  };

  public Curve(Vec3 v[], float smoothness[], int smoothingMethod, boolean isClosed)
  {
    int i;
    
    vertex = new MeshVertex [v.length];
    for (i = 0; i < v.length; i++)
      vertex[i] = new MeshVertex(v[i]);
    this.smoothness = smoothness;
    this.smoothingMethod = smoothingMethod;
    closed = isClosed;
  }

  public Object3D duplicate()
  {
    Vec3 v[] = new Vec3 [vertex.length];
    float s[] = new float [vertex.length];
    
    for (int i = 0; i < vertex.length; i++)
      {
        v[i] = new Vec3(vertex[i].r);
        s[i] = smoothness[i];
      }
    return new Curve(v, s, smoothingMethod, closed);
  }

  public void copyObject(Object3D obj)
  {
    Curve cv = (Curve) obj;
    MeshVertex v[] = cv.getVertices();
    
    vertex = new MeshVertex [v.length];
    smoothness = new float [v.length];
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i] = new MeshVertex(new Vec3(v[i].r));
        smoothness[i] = cv.smoothness[i];
      }
    smoothingMethod = cv.smoothingMethod;
    setClosed(cv.closed);
    clearCachedMesh();
  }

  protected void findBounds()
  {
    double minx, miny, minz, maxx, maxy, maxz;
    Vec3 v, points[];
    int i;
    
    getWireframeMesh();
    points = cachedWire.vert;
    minx = maxx = points[0].x;
    miny = maxy = points[0].y;
    minz = maxz = points[0].z;
    for (i = 1; i < points.length; i++)
      {
        v = points[i];
        if (v.x < minx) minx = v.x;
        if (v.x > maxx) maxx = v.x;
        if (v.y < miny) miny = v.y;
        if (v.y > maxy) maxy = v.y;
        if (v.z < minz) minz = v.z;
        if (v.z > maxz) maxz = v.z;
      }
    bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
  }

  public BoundingBox getBounds()
  {
    if (bounds == null)
      findBounds();
    return bounds;
  }

  public MeshVertex[] getVertices()
  {
    return vertex;
  }
  
  public float[] getSmoothness()
  {
    return smoothness;
  }
  

  /** Get the smoothing method being used for this mesh. */

  public int getSmoothingMethod()
  {
    return smoothingMethod;
  }

  /** Move a single control vertex. */
  
  public void movePoint(int which, Vec3 pos)
  {
    vertex[which].r = pos;
    clearCachedMesh();
  }
  
  /** Get a list of the positions of all vertices which define the mesh. */
  
  public Vec3 [] getVertexPositions()
  {
    Vec3 v[] = new Vec3 [vertex.length];
    for (int i = 0; i < v.length; i++)
      v[i] = new Vec3(vertex[i].r);
    return v;
  }
  
  /** Set new positions for all vertices. */
  
  public void setVertexPositions(Vec3 v[])
  {
    for (int i = 0; i < v.length; i++)
      vertex[i].r = v[i];
    clearCachedMesh();
  }
  
  /** Set the smoothing method. */

  public void setSmoothingMethod(int method)
  {
    smoothingMethod = method;
    clearCachedMesh();
  }
  
  /** Set the smoothness values for all vertices. */
  
  public void setSmoothness(float s[])
  {
    for (int i = 0; i < s.length; i++)
      smoothness[i] = s[i];
    clearCachedMesh();
  }
  
  /** Set both the positions and smoothness values for all points. */

  public void setShape(Vec3 v[], float smoothness[])
  {
    if (v.length != vertex.length)
      vertex = new MeshVertex [v.length];
    for (int i = 0; i < v.length; i++)
      vertex[i] = new MeshVertex(v[i]);
    this.smoothness = smoothness;
    clearCachedMesh();
  }

  public void setClosed(boolean isClosed)
  {
    closed = isClosed;
    clearCachedMesh();
  }

  public boolean isClosed()
  {
    return closed;
  }

  public void setSize(double xsize, double ysize, double zsize)
  {
    Vec3 size = getBounds().getSize();
    double xscale, yscale, zscale;
    
    if (size.x == 0.0)
      xscale = 1.0;
    else
      xscale = xsize / size.x;
    if (size.y == 0.0)
      yscale = 1.0;
    else
      yscale = ysize / size.y;
    if (size.z == 0.0)
      zscale = 1.0;
    else
      zscale = zsize / size.z;
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i].r.x *= xscale;
        vertex[i].r.y *= yscale;
        vertex[i].r.z *= zscale;
      }
    clearCachedMesh();
  }
  
  /** Clear the cached mesh. */
  
  protected void clearCachedMesh()
  {
    cachedWire = null;
    bounds = null;
  }

  public WireframeMesh getWireframeMesh()
  {
    int i, from[], to[];
    Curve subdiv;
    Vec3 vert[];
    
    if (cachedWire != null)
      return cachedWire;
    if (smoothingMethod == NO_SMOOTHING)
      subdiv = this;
    else
      subdiv = subdivideCurve().subdivideCurve();
    vert = new Vec3 [subdiv.vertex.length];
    for (i = 0; i < vert.length; i++)
      vert[i] = subdiv.vertex[i].r;
    if (closed)
      {
        from = new int [vert.length];
        to = new int [vert.length];
        from[vert.length-1] = vert.length-1;
        to[vert.length-1] = 0;
      }
    else
      {
        from = new int [vert.length-1];
        to = new int [vert.length-1];
      }
    for (i = 0; i < vert.length-1; i++)
      {
        from[i] = i;
        to[i] = i+1;
      }
    return (cachedWire = new WireframeMesh(vert, from, to));
  }
  
  /** Return a new Curve object which has been subdivided once to give a finer approximation of the curve shape. */
  
  public Curve subdivideCurve()
  {
    if (vertex.length < 2)
      return (Curve) duplicate();
    if (vertex.length == 2)
      {
        Vec3 newpos[] = new Vec3 [] {new Vec3(vertex[0].r), vertex[0].r.plus(vertex[1].r).times(0.5), new Vec3(vertex[1].r)};
        float news[] = new float [] {smoothness[0], (smoothness[0]+smoothness[1])*0.5f, smoothness[1]};
        return new Curve(newpos, news, smoothingMethod, closed);
      }
    Vec3 v[] = new Vec3 [vertex.length];
    for (int i = 0; i < v.length; i++)
      v[i] = new Vec3(vertex[i].r);
    Vec3 newpos[];
    float news[];
    int i, j;
    if (closed)
      {
        newpos = new Vec3 [v.length*2];
        news = new float [smoothness.length*2];
        if (smoothingMethod == INTERPOLATING)
          {
            newpos[0] = v[0];
            newpos[1] = calcInterpPoint(v, smoothness, v.length-1, 0, 1, 2);
            for (i = 2, j = 1; i < newpos.length; i++)
              {
                if (i%2 == 0)
                  newpos[i] = v[j];
                else
                  {
                    newpos[i] = calcInterpPoint(v, smoothness, j-1, j, (j+1)%v.length, (j+2)%v.length);
                    j++;
                  }
              }
          }
        else
          {
            newpos[0] = calcApproxPoint(v, smoothness, v.length-1, 0, 1);
            for (i = 1; i < v.length-1; i++)
              {
                newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
                newpos[i*2] = calcApproxPoint(v, smoothness, i-1, i, i+1);
              }
            newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
            newpos[i*2] = calcApproxPoint(v, smoothness, i-1, i, 0);
            newpos[i*2+1] = v[0].plus(v[i]).times(0.5);
          }
        for (i = 0; i < smoothness.length; i++)
          {
            news[i*2] = Math.min(smoothness[i]*2.0f, 1.0f);
            news[i*2+1] = 1.0f;
          }
      }
    else
      {
        newpos = new Vec3 [v.length*2-1];
        news = new float [smoothness.length*2-1];
        if (smoothingMethod == INTERPOLATING)
          {
            newpos[0] = v[0];
            newpos[1] = calcInterpPoint(v, smoothness, 0, 0, 1, 2);
            for (i = 2, j = 1; i < newpos.length-2; i++)
              {
                if (i%2 == 0)
                  newpos[i] = v[j];
                else
                  {
                    newpos[i] = calcInterpPoint(v, smoothness, j-1, j, j+1, j+2);
                    j++;
                  }
              }
            newpos[i] = calcInterpPoint(v, smoothness, j-1, j, j+1, j+1);
            newpos[i+1] = v[j+1];
          }
        else
          {
            newpos[0] = v[0];
            for (i = 1; i < v.length-1; i++)
              {
                newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
                newpos[i*2] = calcApproxPoint(v, smoothness, i-1, i, i+1);
              }
            newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
            newpos[i*2] = v[i];
          }
        for (i = 0; i < smoothness.length-1; i++)
          {
            news[i*2] = Math.min(smoothness[i]*2.0f, 1.0f);
            news[i*2+1] = 1.0f;
          }
        news[i*2] = Math.min(smoothness[i]*2.0f, 1.0f);
      }
    return new Curve(newpos, news, smoothingMethod, closed);
  }
  
  /** Return a new Curve object which has been subdivided the specified number of times to give a finer approximation of
      the curve shape. */
  
  public Curve subdivideCurve(int times)
  {
    Curve c = this;
    for (int i = 0; i < times; i++)
      c = c.subdivideCurve();
    return c;
  }

  /** The following two routines are used by subdivideCurve to calculate new point positions
      for interpolating and approximating subdivision.  v is the array of current points, s is
      the array of smoothness values for them, and i, j, k, and m are the indices of the points
      from which the new point will be calculated. */
  
  public static Vec3 calcInterpPoint(Vec3 v[], float s[], int i, int j, int k, int m)
  {
    double w1, w2, w3, w4;
    
    w1 = -0.0625*s[j];
    w2 = 0.5-w1;
    w4 = -0.0625*s[k];
    w3 = 0.5-w4;
    
    return new Vec3 (w1*v[i].x + w2*v[j].x + w3*v[k].x + w4*v[m].x,
                            w1*v[i].y + w2*v[j].y + w3*v[k].y + w4*v[m].y,
                            w1*v[i].z + w2*v[j].z + w3*v[k].z + w4*v[m].z);
  }

  public static Vec3 calcApproxPoint(Vec3 v[], float s[], int i, int j, int k)
  {
    double w1 = 0.125*s[j], w2 = 1.0-2.0*w1;
    
    return new Vec3 (w1*v[i].x + w2*v[j].x + w1*v[k].x,
                            w1*v[i].y + w2*v[j].y + w1*v[k].y,
                            w1*v[i].z + w2*v[j].z + w1*v[k].z);
  }

  public boolean canSetTexture()
  {
    return false;
  }
  
  public int canConvertToTriangleMesh()
  {
    if (closed)
      return EXACTLY;
    return CANT_CONVERT;
  }
  
  public TriangleMesh convertToTriangleMesh(double tol)
  {
    TriangleMesh mesh = triangulateCurve();
    if (mesh != null)
      mesh = TriangleMesh.optimizeMesh(mesh);
    return mesh;
  }

  private TriangleMesh triangulateCurve()
  {
    Vec3 v[] = new Vec3 [vertex.length], size = getBounds().getSize();
    Vec2 v2[] = new Vec2 [vertex.length];
    int i, j, current, count, min;
    int index[] = new int [vertex.length], faces[][] = new int [vertex.length-2][3];
    double dir, dir2;
    boolean inside;

    // Find the largest dimension of the line, and project the vertices onto the other two.

    if (size.x > size.y)
      {
        if (size.y > size.z)
          j = 2;
        else
          j = 1;
      }
    else
      {
        if (size.x > size.z)
          j = 2;
        else
          j = 0;
      }
    for (i = 0; i < vertex.length; i++)
      {
        v[i] = vertex[i].r;
        v2[i] = vertex[i].r.dropAxis(j);
      }

    // Select the vertex to start from.

    min = 0;
    for (i = 1; i < v2.length; i++)
      {
        if (v2[i].x < v2[min].x)
          min = i;
      }
    for (i = 0; i < index.length; i++)
      index[i] = i;
    current = min;
    do
      {
        dir = triangleDirection(v2, index, v2.length, current);
        if (dir == 0.0)
          {
            current = (current+1)%index.length;
            if (current == min)
              return null;  // All of the points lie on a straight line.
          }
      } while (dir == 0.0);

    // Now add the triangles one at a time.

    count = index.length;
    for (i = 0; i < vertex.length-2; i++)
      {
        // Determine whether a triangle centered at the current vertex will face in the
        // correct direction.  If not, or if the triangle will contain another vertex,
        // then try the next vertex.

        j = current;
        do
          {
            dir2 = triangleDirection(v2, index, count, current);
            inside = containsPoints(v2, index, count, current);
            if (dir2*dir < 0.0 || inside)
              {
                current = (current+1)%count;
                if (current == j)
                  return null;  // Cannot triangulate the projected curve.
              }
          } while (dir2*dir < 0.0 || inside);

        // Add the face, and remove the vertex from the list.

        if (current == 0)
          faces[i][0] = index[count-1];
        else
          faces[i][0] = index[current-1];
        faces[i][1] = index[current];
        if (current == count-1)
          faces[i][2] = index[0];
        else
          faces[i][2] = index[current+1];
        for (j = current; j < count-1; j++)
          index[j] = index[j+1];
        count--;
        current = (current+1)%count;
      }
    TriangleMesh mesh = new TriangleMesh(v, faces);
    TriangleMesh.Vertex vert[] = (TriangleMesh.Vertex []) mesh.getVertices();
    for (i = 0; i < vert.length; i++)
      vert[i].smoothness = smoothness[i];
    mesh.setSmoothingMethod(smoothingMethod);
    return mesh;
  }

  /** This is used by the above method.  Given the list of remaining vertices, it finds the
      edges to either size of the specified vertex and returns their cross product.  This tells
      which way a triangle centered at the vertex will face. */

  double triangleDirection(Vec2 v2[], int index[], int count, int which)
  {
    Vec2 va, vb;
    
    if (which == 0)
      va = v2[index[which]].minus(v2[index[count-1]]);
    else
      va = v2[index[which]].minus(v2[index[which-1]]);
    if (which == count-1)
      vb = v2[index[which]].minus(v2[index[0]]);
    else
      vb = v2[index[which]].minus(v2[index[which+1]]);
    return va.cross(vb);
  }

  /** This is used by convertToTriangleMesh.  Given the list of remaining vertices, it
      determines whether a triangle centered at the specified vertex would contain any
      other vertices. */

  boolean containsPoints(Vec2 v2[], int index[], int count, int which)
  {
    Vec2 va, vb, v;
    double a, b, c;
    int i, prev, next;
    
    if (which == 0)
      prev = count-1;
    else
      prev = which-1;
    if (which == count-1)
      next = 0;
    else
      next = which+1;
    va = v2[index[which]].minus(v2[index[prev]]);
    vb = v2[index[which]].minus(v2[index[next]]);
    a = va.cross(vb);
    va.scale(1.0/a);
    vb.scale(1.0/a);
    for (i = 0; i < count; i++)
      if (i != prev && i != which && i != next)
        {
          v = v2[index[i]].minus(v2[index[which]]);
          b = vb.cross(v);
          c = v.cross(va);
          a = 1 - b - c;
          if (a >= 0.0 && a <= 1.0 && b >= 0.0 && b <= 1.0 && c >= 0.0 && c <= 1.0)
            return true;
        }
    return false;
  }

  /** Normal vectors do not make sense for a curve, since it does not define a surface. */
     
  public Vec3 [] getNormals()
  {
    Vec3 norm[] = new Vec3[vertex.length];
    for (int i = 0; i < norm.length; i++)
      norm[i] = Vec3.vz();
    return norm;
  }

  public boolean isEditable()
  {
    return true;
  }

  /** Get the skeleton.  This returns null, since Curves cannot have skeletons. */
  
  public Skeleton getSkeleton()
  {
    return null;
  }
  
  /** Set the skeleton.  This does nothing, since Curves cannot have skeletons. */

  public void setSkeleton(Skeleton s)
  {
  }
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    CurveEditorWindow ed = new CurveEditorWindow(parent, "Curve object '"+ info.getName() +"'", info, cb, true);
    ed.setVisible(true);
  }

  public void editGesture(final EditingWindow parent, ObjectInfo info, Runnable cb, ObjectInfo realObject)
  {
    CurveEditorWindow ed = new CurveEditorWindow(parent, "Gesture '"+ info.getName() +"'", info, cb, false);
    ViewerCanvas views[] = ed.getAllViews();
    for (int i = 0; i < views.length; i++)
      ((MeshViewer) views[i]).setScene(parent.getScene(), realObject);
    ed.setVisible(true);
  }

  /** Get a MeshViewer which can be used for viewing this mesh. */
  
  public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options)
  {
    return new CurveViewer(controller, options);
  }

  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */

  public Curve(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    int i;
    short version = in.readShort();

    if (version != 0)
      throw new InvalidObjectException("");
    vertex = new MeshVertex [in.readInt()];
    smoothness = new float [vertex.length];
    for (i = 0; i < vertex.length; i++)
      {
        vertex[i] = new MeshVertex(new Vec3(in));
        smoothness[i] = in.readFloat();
      }
    closed = in.readBoolean();
    smoothingMethod = in.readInt();
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    int i;

    out.writeShort(0);
    out.writeInt(vertex.length);
    for (i = 0; i < vertex.length; i++)
      {
        vertex[i].r.writeToFile(out);
        out.writeFloat(smoothness[i]);
      }
    out.writeBoolean(closed);
    out.writeInt(smoothingMethod);
  }

  public Property[] getProperties()
  {
    return (Property []) PROPERTIES.clone();
  }

  public Object getPropertyValue(int index)
  {
    if (index == 0)
    {
      if (smoothingMethod == 0)
        return PROPERTIES[0].getAllowedValues()[0];
      else
        return PROPERTIES[0].getAllowedValues()[smoothingMethod-1];
    }
    return Boolean.valueOf(closed);
  }

  public void setPropertyValue(int index, Object value)
  {
    if (index == 0)
    {
      Object values[] = PROPERTIES[0].getAllowedValues();
      for (int i = 0; i < values.length; i++)
        if (values[i].equals(value))
          setSmoothingMethod(i == 0 ? 0 : i+1);
    }
    else
    {
      setClosed(((Boolean) value).booleanValue());
    }
  }

  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new CurveKeyframe(this);
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    CurveKeyframe key = (CurveKeyframe) k;

    for (int i = 0; i < vertex.length; i++)
    {
      vertex[i].r.set(key.vertPos[i]);
      smoothness[i] = key.vertSmoothness[i];
    }
    cachedWire = null;
    bounds = null;
  }

  public boolean canConvertToActor()
  {
    return true;
  }

  /** Curves cannot be keyframed directly, since any change to mesh topology would
      cause all keyframes to become invalid.  Return an actor for this mesh. */

  public Object3D getPosableObject()
  {
    Curve m = (Curve) duplicate();
    return new Actor(m);
  }

  /** This class represents a pose of a Curve. */

  public static class CurveKeyframe extends MeshGesture
  {
    Vec3 vertPos[];
    float vertSmoothness[];
    Curve curve;

    public CurveKeyframe(Curve curve)
    {
      this.curve = curve;
      vertPos = new Vec3 [curve.vertex.length];
      vertSmoothness = new float [curve.vertex.length];
      for (int i = 0; i < vertPos.length; i++)
      {
        vertPos[i] = new Vec3(curve.vertex[i].r);
        vertSmoothness[i] = curve.smoothness[i];
      }
    }

    private CurveKeyframe()
    {
    }

    /** Get the Mesh this Gesture belongs to. */

    protected Mesh getMesh()
    {
      return curve;
    }

    /** Get the positions of all vertices in this Gesture. */

    protected Vec3 [] getVertexPositions()
    {
      return vertPos;
    }

    /** Set the positions of all vertices in this Gesture. */

    protected void setVertexPositions(Vec3 pos[])
    {
      vertPos = pos;
    }

    /** Get the skeleton for this pose (or null if it doesn't have one). */

    public Skeleton getSkeleton()
    {
      return null;
    }

    /** Set the skeleton for this pose. */

    public void setSkeleton(Skeleton s)
    {
    }

    /** Create a duplicate of this keyframe. */

    public Keyframe duplicate()
    {
      return duplicate(curve);
    }

    public Keyframe duplicate(Object owner)
    {
      CurveKeyframe k = new CurveKeyframe();
      if (owner instanceof Curve)
        k.curve = (Curve) owner;
      else
        k.curve = (Curve) ((ObjectInfo) owner).getObject();
      k.vertPos = new Vec3 [vertPos.length];
      k.vertSmoothness = new float [vertSmoothness.length];
      for (int i = 0; i < vertPos.length; i++)
        {
          k.vertPos[i] = new Vec3(vertPos[i]);
          k.vertSmoothness[i] = vertSmoothness[i];
        }
      return k;
    }

    /** Get the list of graphable values for this keyframe. */

    public double [] getGraphValues()
    {
      return new double [0];
    }

    /** Set the list of graphable values for this keyframe. */

    public void setGraphValues(double values[])
    {
    }

    /** These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others.  These methods should never be called, since Curves
       can only be keyframed by converting them to Actors. */

    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      return null;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      return null;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      return null;
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
      super.blendSurface(average, p, weight);
      CurveKeyframe avg = (CurveKeyframe) average;
      for (int i = 0; i < weight.length; i++)
      {
        CurveKeyframe key = (CurveKeyframe) p[i];
        for (int j = 0; j < vertSmoothness.length; j++)
          avg.vertSmoothness[j] += (float) (weight[i]*(key.vertSmoothness[j]-vertSmoothness[j]));
      }

      // Make sure all smoothness values are within legal bounds.

      for (int i = 0; i < vertSmoothness.length; i++)
      {
        if (avg.vertSmoothness[i] < 0.0)
          avg.vertSmoothness[i] = 0.0f;
        if (avg.vertSmoothness[i] > 1.0)
          avg.vertSmoothness[i] = 1.0f;
      }
    }

    /** Determine whether this keyframe is identical to another one. */

    public boolean equals(Keyframe k)
    {
      if (!(k instanceof CurveKeyframe))
        return false;
      CurveKeyframe key = (CurveKeyframe) k;
      for (int i = 0; i < vertPos.length; i++)
      {
        if (!vertPos[i].equals(key.vertPos[i]))
          return false;
        if (vertSmoothness[i] != key.vertSmoothness[i])
          return false;
      }
      return true;
    }

    /** Update the texture parameter values when the texture is changed. */

    public void textureChanged(TextureParameter oldParams[], TextureParameter newParams[])
    {
    }

    /** Get the value of a per-vertex texture parameter. */

    public ParameterValue getTextureParameter(TextureParameter p)
    {
      return null;
    }

    /** Set the value of a per-vertex texture parameter. */

    public void setTextureParameter(TextureParameter p, ParameterValue value)
    {
    }

    /** Write out a representation of this keyframe to a stream. */

    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeShort(0); // version
      out.writeInt(vertPos.length);
      for (int i = 0; i < vertPos.length; i++)
      {
        vertPos[i].writeToFile(out);
        out.writeFloat(vertSmoothness[i]);
      }
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public CurveKeyframe(DataInputStream in, Object parent) throws IOException, InvalidObjectException
    {
      this();
      short version = in.readShort();
      if (version != 0)
        throw new InvalidObjectException("");
      curve = (Curve) parent;
      int numVert = in.readInt();
      vertPos = new Vec3 [numVert];
      vertSmoothness = new float [numVert];
      for (int i = 0; i < numVert; i++)
      {
        vertPos[i] = new Vec3(in);
        vertSmoothness[i] = in.readFloat();
      }
    }
  }
}
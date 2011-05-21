/* Copyright (C) 1999-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;

/** The Cylinder class represents cylinders and cones.  It is specified by the height, the
    radii along the two axes of its base, and the ratio of the top and bottom radii. */

public class Cylinder extends Object3D
{
  private double rx, rz, ratio, height;
  private BoundingBox bounds;
  private RenderingMesh cachedMesh;
  private WireframeMesh cachedWire;

  private static final int SEGMENTS = 16;
  private static double sine[], cosine[];
  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("bottomRadiusX"), 0.0, Double.MAX_VALUE, 1.0),
      new Property(Translate.text("bottomRadiusZ"), 0.0, Double.MAX_VALUE, 1.0),
      new Property(Translate.text("radiusRatio"), 0.0, 1.0, 1.0),
      new Property(Translate.text("Height"), 0.0, Double.MAX_VALUE, 1.0)
  };

  static
  {
    sine = new double [SEGMENTS];
    cosine = new double [SEGMENTS];
    for (int i = 0; i < SEGMENTS; i++)
      {
        sine[i] = Math.sin(i*2.0*Math.PI/SEGMENTS);
        cosine[i] = Math.cos(i*2.0*Math.PI/SEGMENTS);
      }
  }

  public Cylinder(double height, double xradius, double yradius, double ratio)
  {
    this.height = height;
    rx = xradius;
    rz = yradius;
    this.ratio = ratio;
    bounds = new BoundingBox(-rx, rx, -height/2.0, height/2.0, -rz, rz);
  }

  public Object3D duplicate()
  {
    Cylinder obj = new Cylinder(height, rx, rz, ratio);
    obj.copyTextureAndMaterial(this);
    return obj;
  }
  
  public void copyObject(Object3D obj)
  {
    Cylinder c = (Cylinder) obj;
    Vec3 size = c.getBounds().getSize();
    
    setSize(size.x, size.y, size.z);
    ratio = c.ratio;
    copyTextureAndMaterial(obj);
    cachedMesh = null;
    cachedWire = null;
  }

  /** Get the ratio of top/bottom radius. */

  public double getRatio()
  {
    return ratio;
  }

  /** Set the ratio of top/bottom radius. */

  public void setRatio(double ratio)
  {
    this.ratio = ratio;
    cachedMesh = null;
    cachedWire = null;
  }
  
  public BoundingBox getBounds()
  {
    return bounds;
  }

  public void setSize(double xsize, double ysize, double zsize)
  {
    rx = xsize/2.0;
    rz = zsize/2.0;
    height = ysize;
    bounds = new BoundingBox(-rx, rx, -height/2.0, height/2.0, -rz, rz);
    cachedMesh = null;
    cachedWire = null;
  }

  public WireframeMesh getWireframeMesh()
  {
    Vec3 vert[];
    int i, from[], to[];
    double y1, y2;
    
    if (cachedWire != null)
      return cachedWire;
    y1 = -height/2.0;
    y2 = height/2.0;
    if (ratio > 0.0)
      {
        vert = new Vec3 [2*SEGMENTS+2];
        from = new int [5*SEGMENTS];
        to = new int [5*SEGMENTS];
        vert[2*SEGMENTS] = new Vec3(0.0, y1, 0.0);
        vert[2*SEGMENTS+1] = new Vec3(0.0, y2, 0.0);
        for (i = 0; i < SEGMENTS; i++)
          {
            vert[i] = new Vec3(rx*cosine[i], y1, rz *sine[i]);
            vert[i+SEGMENTS] = new Vec3(ratio*rx*cosine[i], y2, ratio*rz *sine[i]);
            from[i] = 2*SEGMENTS;
            to[i] = i;
            from[i+SEGMENTS] = i;
            to[i+SEGMENTS] = (i+1)%SEGMENTS;
            from[i+2*SEGMENTS] = i;
            to[i+2*SEGMENTS] = (i+1)%SEGMENTS+SEGMENTS;
            from[i+3*SEGMENTS] = i+SEGMENTS;
            to[i+3*SEGMENTS] = (i+1)%SEGMENTS+SEGMENTS;
            from[i+4*SEGMENTS] = 2*SEGMENTS+1;
            to[i+4*SEGMENTS] = i+SEGMENTS;
          }
      }
    else
      {
        vert = new Vec3 [SEGMENTS+2];
        from = new int [3*SEGMENTS];
        to = new int [3*SEGMENTS];
        vert[SEGMENTS] = new Vec3(0.0, y1, 0.0);
        vert[SEGMENTS+1] = new Vec3(0.0, y2, 0.0);
        for (i = 0; i < SEGMENTS; i++)
          {
            vert[i] = new Vec3(rx*cosine[i], y1, rz *sine[i]);
            from[i] = SEGMENTS;
            to[i] = i;
            from[i+SEGMENTS] = i;
            to[i+SEGMENTS] = (i+1)%SEGMENTS;
            from[i+2*SEGMENTS] = i;
            to[i+2*SEGMENTS] = SEGMENTS+1;
          }
      }
    return (cachedWire = new WireframeMesh(vert, from, to));
  }

  public int canConvertToTriangleMesh()
  {
    return APPROXIMATELY;
  }
  
  public TriangleMesh convertToTriangleMesh(double tol)
  {
    Vec3 vertices[];
    Vec2 v[] = new Vec2 [4], vtemp[];
    TriangleMesh.Edge edges[];
    TriangleMesh mesh;
    int i, faces[][];
    double angle;
    double y1, y2;
    
    y1 = -height/2.0;
    y2 = height/2.0;

    // Subdivide the edge of the large end.

    v[0] = new Vec2(rx, 0.0);
    v[1] = new Vec2(0.0, -rz);
    v[2] = new Vec2(-rx, 0.0);
    v[3] = new Vec2(0.0, rz);
    while (!withinTolerance(v, tol))
      {
        vtemp = v;
        v = new Vec2 [v.length*2];
        for (i = 0; i < vtemp.length; i++)
          {
            v[i*2] = vtemp[i];
            angle = 2.0*Math.PI*(i+0.5)/vtemp.length;
            v[i*2+1] = new Vec2(rx*Math.cos(angle), -rz *Math.sin(angle));
          }
      }
    
    // Find the list of faces.

    if (ratio == 0.0)
      {
        vertices = new Vec3 [v.length+2];
        faces = new int [v.length*2][];
        vertices[0] = new Vec3(0.0, y1, 0.0);
        vertices[v.length+1] = new Vec3(0.0, y2, 0.0);
        for (i = 0; i < v.length; i++)
          {
            vertices[i+1] = new Vec3(v[i].x, y1, v[i].y);
            faces[i] = new int[] {i+2, i+1, 0};
            faces[i+v.length] = new int[] {v.length+1, i+1, i+2};
          }
        faces[v.length-1][0] -= v.length;
        faces[v.length*2-1][2] -= v.length;
      }
    else
      {
        vertices = new Vec3 [v.length*2+2];
        faces = new int [v.length*4][];
        vertices[0] = new Vec3(0.0, y1, 0.0);
        vertices[v.length*2+1] = new Vec3(0.0, y2, 0.0);
        for (i = 0; i < v.length; i++)
          {
            vertices[i+1] = new Vec3(v[i].x, y1, v[i].y);
            vertices[i+v.length+1] = new Vec3(v[i].x*ratio, y2, v[i].y*ratio);
            faces[i] = new int[] {i+2, i+1, 0};
            faces[i+v.length] = new int[] {i+v.length+1, i+1, i+2};
            faces[i+v.length*2] = new int[] {i+v.length+1, i+2, i+v.length+2};
            faces[i+v.length*3] = new int[] {v.length*2+1, i+v.length+1, i+v.length+2};
          }
        faces[v.length-1][0] -= v.length;
        faces[v.length*2-1][2] -= v.length;
        faces[v.length*3-1][1] -= v.length;
        faces[v.length*3-1][2] -= v.length;
        faces[v.length*4-1][2] -= v.length;
      }
    mesh = new TriangleMesh(vertices, faces);
    edges = mesh.getEdges();
    for (i = 0; i < edges.length; i++)
      if (edges[i].v1 != 0 && edges[i].v2 != 0 && edges[i].v1 <= v.length && edges[i].v2 <= v.length)
        edges[i].smoothness = 0.0f;
    if (ratio != 0.0)
      for (i = 0; i < edges.length; i++)
        if (edges[i].v1 != v.length*2+1 && edges[i].v2 != v.length*2+1 && edges[i].v1 > v.length && edges[i].v2 > v.length)
          edges[i].smoothness = 0.0f;
    mesh.copyTextureAndMaterial(this);
    return mesh;
  }

  boolean withinTolerance(Vec2 v[], double tol)
  {
    Vec2 point, truePoint;
    double angle;

    point = new Vec2(0.0, 0.0);
    truePoint = new Vec2(0.0, 0.0);
    for (int i = 0; i < v.length/2; i++)
      {
        point.x = (v[i].x+v[i+1].x)/2.0;
        point.y = (v[i].y+v[i+1].y)/2.0;
        angle = 2.0*Math.PI*(i+0.5)/v.length;
        truePoint.x = rx*Math.cos(angle);
        truePoint.y = -rz *Math.sin(angle);
        if (truePoint.distance(point) > tol)
          return false;
      }
    return true;
  }

  @Override
  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    Vec3 vert[], norm[];
    Vec2 v[], vtemp[];
    double angle, y1, y2;
    RenderingTriangle tri[];

    if (interactive && cachedMesh != null)
      return cachedMesh;
    y1 = -height/2.0;
    y2 = height/2.0;
    
    // Subdivide the edge of the large end.
    
    v = new Vec2 [] {new Vec2(rx, 0.0), new Vec2(0.0, -rz), new Vec2(-rx, 0.0), new Vec2(0.0, rz)};
    while (!withinTolerance(v, tol))
      {
        vtemp = v;
        v = new Vec2 [v.length*2];
        for (int i = 0; i < vtemp.length; i++)
          {
            v[i*2] = vtemp[i];
            angle = 2.0*Math.PI*(i+0.5)/vtemp.length;
            v[i*2+1] = new Vec2(rx*Math.cos(angle), -rz *Math.sin(angle));
          }
      }
    
    // Build the mesh.
    
    if (ratio == 0.0)
      {
        vert = new Vec3 [v.length+2];
        norm = new Vec3 [v.length+1];
        tri = new RenderingTriangle [v.length*2];
        vert[0] = new Vec3(0.0, y1, 0.0);
        vert[v.length+1] = new Vec3(0.0, y2, 0.0);
        norm[0] = new Vec3(0.0, y1, 0.0);
        for (int i = 0; i < v.length; i++)
          {
            vert[i+1] = new Vec3(v[i].x, y1, v[i].y);
            norm[i+1] = new Vec3(v[i].x/(rx*rx), 1.0/(y2-y1), v[i].y/(rz *rz));
          }
        for (int i = 0; i < v.length-1; i++)
          {
            tri[i] = texMapping.mapTriangle(i+2, i+1, 0, 0, 0, 0, vert);
            tri[i+v.length] = texMapping.mapTriangle(v.length+1, i+1, i+2, i+1, i+1, i+2, vert);
          }
        tri[v.length-1] = texMapping.mapTriangle(1, v.length, 0, 0, 0, 0, vert);
        tri[v.length*2-1] = texMapping.mapTriangle(v.length+1, v.length, 1, v.length, v.length, 1, vert);
      }
    else
      {
        vert = new Vec3 [v.length*2+2];
        norm = new Vec3 [v.length+2];
        tri = new RenderingTriangle [v.length*4];
        vert[0] = new Vec3(0.0, y1, 0.0);
        vert[v.length*2+1] = new Vec3(0.0, y2, 0.0);
        norm[0] = new Vec3(0.0, y1, 0.0);
        norm[v.length+1] = new Vec3(0.0, y2, 0.0);
        for (int i = 0; i < v.length; i++)
          {
            vert[i+1] = new Vec3(v[i].x, y1, v[i].y);
            vert[i+v.length+1] = new Vec3(v[i].x*ratio, y2, v[i].y*ratio);
            norm[i+1] = new Vec3(v[i].x/(rx*rx), (1.0-ratio)/(y2-y1), v[i].y/(rz *rz));
          }
        for (int i = 0; i < v.length-1; i++)
          {
            tri[i] = texMapping.mapTriangle(i+2, i+1, 0, 0, 0, 0, vert);
            tri[i+v.length] = texMapping.mapTriangle(i+v.length+1, i+1, i+2, i+1, i+1, i+2, vert);
            tri[i+v.length*2] = texMapping.mapTriangle(i+v.length+1, i+2, i+v.length+2, i+1, i+2, i+2, vert);
            tri[i+v.length*3] = texMapping.mapTriangle(v.length*2+1, i+v.length+1, i+v.length+2, v.length+1, v.length+1, v.length+1, vert);
          }
        tri[v.length-1] = texMapping.mapTriangle(1, v.length, 0, 0, 0, 0, vert);
        tri[v.length*2-1] = texMapping.mapTriangle(v.length*2, v.length, 1, v.length, v.length, 1, vert);
        tri[v.length*3-1] = texMapping.mapTriangle(v.length*2, 1, v.length+1, v.length, 1, 1, vert);
        tri[v.length*4-1] = texMapping.mapTriangle(v.length*2+1, v.length*2, v.length+1, v.length+1, v.length+1, v.length+1, vert);
      }
    for (int i = 0; i < norm.length; i++)
      norm[i].normalize();
    RenderingMesh mesh = new RenderingMesh(vert, norm, tri, texMapping, matMapping);
    mesh.setParameters(paramValue);
    if (interactive)
      cachedMesh = mesh;
    return mesh;
  }

  @Override
  public void setTexture(Texture tex, TextureMapping mapping)
  {
    super.setTexture(tex, mapping);
    cachedMesh = null;
    cachedWire = null;
  }
  
  @Override
  public void setMaterial(Material mat, MaterialMapping map)
  {
    super.setMaterial(mat, map);
    cachedMesh = null;
  }

  @Override
  public boolean isEditable()
  {
    return true;
  }
  
  @Override
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    ValueField xField = new ValueField(rx, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(rz, ValueField.POSITIVE, 5);
    ValueField heightField = new ValueField(height, ValueField.POSITIVE, 5);
    ValueSlider ratioSlider = new ValueSlider(0.0, 1.0, 100, ratio);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), 
        Translate.text("editCylinderTitle"), new Widget [] {xField, yField, ratioSlider, heightField},
        new String [] {Translate.text("bottomRadiusX"), Translate.text("bottomRadiusZ"), Translate.text("radiusRatio"), Translate.text("Height")});
    if (!dlg.clickedOk())
      return;
    ratio = ratioSlider.getValue();
    setSize(2.0*xField.getValue(), heightField.getValue(), 2.0*yField.getValue());
    cb.run();
  }

  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */

  public Cylinder(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    rx = in.readDouble();
    rz = in.readDouble();
    height = in.readDouble();
    ratio = in.readDouble();
    bounds = new BoundingBox(-rx, rx, -height/2.0, height/2.0, -rz, rz);
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(0);
    out.writeDouble(rx);
    out.writeDouble(rz);
    out.writeDouble(height);
    out.writeDouble(ratio);
  }

  public Property[] getProperties()
  {
    return (Property []) PROPERTIES.clone();
  }

  public Object getPropertyValue(int index)
  {
    switch (index)
    {
      case 0:
        return new Double(rx);
      case 1:
        return new Double(rz);
      case 2:
        return new Double(ratio);
      case 3:
        return new Double(height);
    }
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
    double val = ((Double) value).doubleValue();
    if (index == 0)
      setSize(2.0*val, height, 2.0*rz);
    else if (index == 1)
      setSize(2.0*rx, height, 2.0*val);
    else if (index == 2)
      setRatio(val);
    else if (index == 3)
      setSize(2.0*rx, val, 2.0*rz);
  }

  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new CylinderKeyframe(rx, rz, height, ratio);
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    CylinderKeyframe key = (CylinderKeyframe) k;
    
    ratio = key.ratio;
    setSize(2.0*key.rx, key.height, 2.0*key.ry);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"X Radius", "Z Radius", "Height", "Ratio"},
        new double [] {2.0*rx, 2.0*rz, height, ratio},
        new double [][] {{0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}, 
        {0.0, Double.MAX_VALUE}, {0.0, 1.0}});
  }
  
  /** Return an array containing the names of the graphable values for the keyframes
      returned by getPoseKeyframe(). */
  
  public String [] getPoseValueNames()
  {
    return new String [] {"X Radius", "Z Radius", "Height", "Ratio"};
  }

  /** Get the default list of graphable values for a keyframe returned by getPoseKeyframe(). */
  
  public double [] getDefaultPoseValues()
  {
    return new double [] {2.0*rx, 2.0*rz, height, ratio};
  }
  
  /** Get the allowed range for graphable values for keyframes returned by getPoseKeyframe().
      This returns a 2D array, where elements [n][0] and [n][1] are the minimum and maximum
      allowed values, respectively, for the nth graphable value. */
  
  public double[][] getPoseValueRange()
  {
    return new double [][] {{0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}, 
      {0.0, Double.MAX_VALUE}, {0.0, 1.0}};
  }
  
  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    CylinderKeyframe key = (CylinderKeyframe) k;
    ValueField xField = new ValueField(2.0*key.rx, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(2.0*key.ry, ValueField.POSITIVE, 5);
    ValueField heightField = new ValueField(key.height, ValueField.POSITIVE, 5);
    ValueSlider ratioSlider = new ValueSlider(0.0, 1.0, 100, key.ratio);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), 
        Translate.text("editCylinderTitle"), new Widget [] {xField, yField, ratioSlider, heightField},
        new String [] {Translate.text("bottomRadiusX"), Translate.text("bottomRadiusZ"), Translate.text("radiusRatio"), Translate.text("Height")});
    if (!dlg.clickedOk())
      return;
    key.rx = 0.5*xField.getValue();
    key.ry = 0.5*yField.getValue();
    key.ratio = ratioSlider.getValue();
    key.height = heightField.getValue();
  }
  
  /** Inner class representing a pose for a cylinder. */
  
  public static class CylinderKeyframe implements Keyframe
  {
    public double rx, ry, ratio, height;
    
    public CylinderKeyframe(double rx, double ry, double height, double ratio)
    {
      this.rx = rx;
      this.ry = ry;
      this.height = height;
      this.ratio = ratio;
    }
    
    /** Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return new CylinderKeyframe(rx, ry, height, ratio);
    }

    /** Create a duplicate of this keyframe for a (possibly different) object. */
  
    public Keyframe duplicate(Object owner)
    {
      return new CylinderKeyframe(rx, ry, height, ratio);
    }
  
    /** Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      return new double [] {rx, ry, height, ratio};
    }
  
    /** Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
      rx = values[0];
      ry = values[1];
      height = values[2];
      ratio = values[3];
    }

    /** These methods return a new Keyframe which is a weighted average of this one and one,
        two, or three others. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      CylinderKeyframe k2 = (CylinderKeyframe) o2;

      return new CylinderKeyframe(weight1*rx+weight2*k2.rx, weight1*ry+weight2*k2.ry, 
        weight1*height+weight2*k2.height, weight1*ratio+weight2*k2.ratio);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      CylinderKeyframe k2 = (CylinderKeyframe) o2, k3 = (CylinderKeyframe) o3;

      return new CylinderKeyframe(weight1*rx+weight2*k2.rx+weight3*k3.rx, 
        weight1*ry+weight2*k2.ry+weight3*k3.ry, 
        weight1*height+weight2*k2.height+weight3*k3.height, 
        weight1*ratio+weight2*k2.ratio+weight3*k3.ratio);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      CylinderKeyframe k2 = (CylinderKeyframe) o2, k3 = (CylinderKeyframe) o3, k4 = (CylinderKeyframe) o4;

      return new CylinderKeyframe(weight1*rx+weight2*k2.rx+weight3*k3.rx+weight4*k4.rx, 
        weight1*ry+weight2*k2.ry+weight3*k3.ry+weight4*k4.ry, 
        weight1*height+weight2*k2.height+weight3*k3.height+weight4*k4.height, 
        weight1*ratio+weight2*k2.ratio+weight3*k3.ratio+weight4*k4.ratio);
    }

    /** Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof CylinderKeyframe))
        return false;
      CylinderKeyframe key = (CylinderKeyframe) k;
      return (key.rx == rx && key.ry == ry && key.ratio == ratio && key.height == height);
    }
  
    /** Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeDouble(rx);
      out.writeDouble(ry);
      out.writeDouble(height);
      out.writeDouble(ratio);
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public CylinderKeyframe(DataInputStream in, Object parent) throws IOException
    {
      this(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());
    }
  }
}
/* Copyright (C) 1999-2004 by Peter Eastman

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
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;

/** Contrary to its name, the Cube class actually can represent any rectangular solid.
    The edges do not need to all be the same length. */

public class Cube extends Object3D
{
  double halfx, halfy, halfz;
  BoundingBox bounds;
  RenderingMesh cachedMesh;
  WireframeMesh cachedWire;

  private static final Property PROPERTIES[] = new Property [] {
    new Property("X Size", 0.0, Double.MAX_VALUE, 1.0),
      new Property("Y Size", 0.0, Double.MAX_VALUE, 1.0),
      new Property("Z Size", 0.0, Double.MAX_VALUE, 1.0)
  };

  public Cube(double xsize, double ysize, double zsize)
  {
    halfx = xsize/2.0;
    halfy = ysize/2.0;
    halfz = zsize/2.0;
    bounds = new BoundingBox(-halfx, halfx, -halfy, halfy, -halfz, halfz);
  }

  public Object3D duplicate()
  {
    Cube obj = new Cube(halfx*2.0, halfy*2.0, halfz*2.0);
    obj.copyTextureAndMaterial(this);
    return obj;
  }
  
  public void copyObject(Object3D obj)
  {
    Cube c = (Cube) obj;
    Vec3 size = c.getBounds().getSize();
    
    setSize(size.x, size.y, size.z);
    copyTextureAndMaterial(obj);
    cachedMesh = null;
    cachedWire = null;
  }
  
  public BoundingBox getBounds()
  {
    return bounds;
  }

  public void setSize(double xsize, double ysize, double zsize)
  {
    halfx = xsize/2.0;
    halfy = ysize/2.0;
    halfz = zsize/2.0;
    bounds = new BoundingBox(-halfx, halfx, -halfy, halfy, -halfz, halfz);
    cachedMesh = null;
    cachedWire = null;
  }

  public int canConvertToTriangleMesh()
  {
    return EXACTLY;
  }
  
  public TriangleMesh convertToTriangleMesh(double tol)
  {
    Vec3 v[] = new Vec3 [14];
    TriangleMesh mesh;
    int i, faces[][] = {{1, 0, 12}, {2, 1, 12}, {3, 2, 12}, {0, 3, 12},
    	{1, 2, 9}, {2, 6, 9}, {6, 5, 9}, {5, 1, 9},
    	{0, 1, 8}, {1, 5, 8}, {5, 4, 8}, {4, 0, 8},
    	{3, 0, 11}, {0, 4, 11}, {4, 7, 11}, {7, 3, 11},
    	{4, 5, 13}, {5, 6, 13}, {6, 7, 13}, {7, 4, 13},
    	{2, 3, 10}, {3, 7, 10}, {7, 6, 10}, {6, 2, 10}};

    for (i = 0; i < 14; i++)
      v[i] = new Vec3();
    v[0].x = v[3].x = v[4].x = v[7].x = v[11].x = -halfx;
    v[1].x = v[2].x = v[5].x = v[6].x = v[9].x = halfx;
    v[0].y = v[1].y = v[2].y = v[3].y = v[12].y = -halfy;
    v[4].y = v[5].y = v[6].y = v[7].y = v[13].y = halfy;
    v[2].z = v[3].z = v[6].z = v[7].z = v[10].z = -halfz;
    v[0].z = v[1].z = v[4].z = v[5].z = v[8].z = halfz;
    mesh = new TriangleMesh(v, faces);
    mesh.setSmoothingMethod(TriangleMesh.NO_SMOOTHING);
    mesh.copyTextureAndMaterial(this);
    return mesh;
  }

  public WireframeMesh getWireframeMesh()
  {
    Vec3 vert[];
    int from[], to[];
    
    if (cachedWire != null)
      return cachedWire;
    vert = bounds.getCorners();
    from = new int [] {0, 2, 3, 1, 4, 6, 7, 5, 0, 1, 2, 3};
    to = new int [] {2, 3, 1, 0, 6, 7, 5, 4, 4, 5, 6, 7};
    return (cachedWire = new WireframeMesh(vert, from, to));
  }

  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    Vec3 vert[], norm[];
    RenderingTriangle tri[];

    if (interactive && cachedMesh != null)
      return cachedMesh;
    vert = bounds.getCorners();
    norm = new Vec3 [6];
    tri = new RenderingTriangle [12];
    norm[0] = new Vec3(1.0, 0.0, 0.0);
    norm[1] = new Vec3(-1.0, 0.0, 0.0);
    norm[2] = new Vec3(0.0, 1.0, 0.0);
    norm[3] = new Vec3(0.0, -1.0, 0.0);
    norm[4] = new Vec3(0.0, 0.0, 1.0);
    norm[5] = new Vec3(0.0, 0.0, -1.0);
    tri[0] = texMapping.mapTriangle(0, 4, 5, 3, 3, 3, vert);
    tri[1] = texMapping.mapTriangle(0, 5, 1, 3, 3, 3, vert);
    tri[2] = texMapping.mapTriangle(0, 1, 3, 1, 1, 1, vert);
    tri[3] = texMapping.mapTriangle(0, 3, 2, 1, 1, 1, vert);
    tri[4] = texMapping.mapTriangle(1, 5, 3, 4, 4, 4, vert);
    tri[5] = texMapping.mapTriangle(5, 7, 3, 4, 4, 4, vert);
    tri[6] = texMapping.mapTriangle(0, 2, 4, 5, 5, 5, vert);
    tri[7] = texMapping.mapTriangle(2, 6, 4, 5, 5, 5, vert);
    tri[8] = texMapping.mapTriangle(2, 3, 7, 2, 2, 2, vert);
    tri[9] = texMapping.mapTriangle(2, 7, 6, 2, 2, 2, vert);
    tri[10] = texMapping.mapTriangle(5, 4, 7, 0, 0, 0, vert);
    tri[11] = texMapping.mapTriangle(4, 6, 7, 0, 0, 0, vert);
    RenderingMesh mesh = new RenderingMesh(vert, norm, tri, texMapping, matMapping);
    mesh.setParameters(paramValue);
    if (interactive)
      cachedMesh = mesh;
    return mesh;
  }

  public void setTexture(Texture tex, TextureMapping mapping)
  {
    super.setTexture(tex, mapping);
    cachedMesh = null;
    cachedWire = null;
  }
  
  public boolean isEditable()
  {
    return true;
  }
  
  /* Allow the user to edit the cube's shape. */
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    ValueField xField = new ValueField(2.0*halfx, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(2.0*halfy, ValueField.POSITIVE, 5);
    ValueField zField = new ValueField(2.0*halfz, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editCubeTitle"),
      new Widget [] {xField, yField, zField}, new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    setSize(xField.getValue(), yField.getValue(), zField.getValue());
    cb.run();
  }

  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public Cube(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    halfx = in.readDouble();
    halfy = in.readDouble();
    halfz = in.readDouble();
    bounds = new BoundingBox(-halfx, halfx, -halfy, halfy, -halfz, halfz);
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(0);
    out.writeDouble(halfx);
    out.writeDouble(halfy);
    out.writeDouble(halfz);
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
        return new Double(2.0*halfx);
      case 1:
        return new Double(2.0*halfy);
      case 2:
        return new Double(2.0*halfz);
    }
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
    double val = ((Double) value).doubleValue();
    if (index == 0)
      setSize(val, 2.0*halfy, 2.0*halfz);
    else if (index == 1)
      setSize(2.0*halfx, val, 2.0*halfz);
    else if (index == 2)
      setSize(2.0*halfx, 2.0*halfy, val);
  }

  /* Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new VectorKeyframe(2.0*halfx, 2.0*halfy, 2.0*halfz);
  }
  
  /* Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    VectorKeyframe key = (VectorKeyframe) k;
    
    setSize(key.x, key.y, key.z);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"X Size", "Y Size", "Z Size"},
        new double [] {2.0*halfx, 2.0*halfy, 2.0*halfz}, 
        new double [][] {{0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}});
  }
  
  /* Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    VectorKeyframe key = (VectorKeyframe) k;
    ValueField xField = new ValueField(key.x, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(key.y, ValueField.POSITIVE, 5);
    ValueField zField = new ValueField(key.z, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editCubeTitle"),
      new Widget [] {xField, yField, zField}, new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    key.set(xField.getValue(), yField.getValue(), zField.getValue());
  }
}
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

/** The Sphere class actually can represent any ellipsoid.  It is specified by the radii
    along the three principle axes. */

public class Sphere extends Object3D
{
  private double rx, ry, rz;
  private BoundingBox bounds;
  private RenderingMesh cachedMesh;
  private WireframeMesh cachedWire;

  private static final int SEGMENTS = 16;
  private static double sine[], cosine[];
  private static final Property PROPERTIES[] = new Property [] {
    new Property("X Radius", 0.0, Double.MAX_VALUE, 1.0),
      new Property("Y Radius", 0.0, Double.MAX_VALUE, 1.0),
      new Property("Z Radius", 0.0, Double.MAX_VALUE, 1.0)
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

  public Sphere(double xradius, double yradius, double zradius)
  {
    rx = xradius;
    ry = yradius;
    rz = zradius;
    bounds = new BoundingBox(-rx, rx, -ry, ry, -rz, rz);
  }

  public Object3D duplicate()
  {
    Sphere obj = new Sphere(rx, ry, rz);
    obj.copyTextureAndMaterial(this);
    return obj;
  }
  
  public void copyObject(Object3D obj)
  {
    Sphere s = (Sphere) obj;
    Vec3 size = s.getBounds().getSize();
    
    setSize(size.x, size.y, size.z);
    copyTextureAndMaterial(obj);
    cachedMesh = null;
  }
  
  public BoundingBox getBounds()
  {
    return bounds;
  }

  public void setSize(double xsize, double ysize, double zsize)
  {
    rx = xsize/2.0;
    ry = ysize/2.0;
    rz = zsize/2.0;
    bounds = new BoundingBox(-rx, rx, -ry, ry, -rz, rz);
    cachedMesh = null;
    cachedWire = null;
  }

  public Vec3 getRadii()
  {
    return new Vec3(rx, ry, rz);
  }

  public WireframeMesh getWireframeMesh()
  {
    Vec3 vert[];
    int i, j, from[], to[], numvert, numedge;
    
    if (cachedWire != null)
      return cachedWire;
    numvert = SEGMENTS*(SEGMENTS/2-1)+2;
    numedge = SEGMENTS*(SEGMENTS-1);
    vert = new Vec3 [numvert];
    from = new int [numedge];
    to = new int [numedge];
    vert[numvert-2] = new Vec3(0.0, ry, 0.0);
    vert[numvert-1] = new Vec3(0.0, -ry, 0.0);
    for (i = 0; i < SEGMENTS; i++)
      {
        from[i] = numvert-2;
        to[i] = i;
        for (j = 0; j < SEGMENTS/2-1; j++)
          {
            vert[j*SEGMENTS+i] = new Vec3(rx*sine[j+1]*cosine[i], ry*cosine[j+1], rz*sine[j+1]*sine[i]);
            from[2*(j+1)*SEGMENTS+i] = j*SEGMENTS+i;
            to[2*(j+1)*SEGMENTS+i] = j*SEGMENTS+(i+1)%SEGMENTS;
          }
        for (j = 0; j < SEGMENTS/2-2; j++)
          {
            from[(2*j+1)*SEGMENTS+i] = j*SEGMENTS+i;
            to[(2*j+1)*SEGMENTS+i] = (j+1)*SEGMENTS+i;
          }
        from[(2*j+1)*SEGMENTS+i] = j*SEGMENTS+i;
        to[(2*j+1)*SEGMENTS+i] = numvert-1;
      }
    return (cachedWire = new WireframeMesh(vert, from, to));
  }

  @Override
  public int canConvertToTriangleMesh()
  {
    return APPROXIMATELY;
  }
  
  @Override
  public TriangleMesh convertToTriangleMesh(double tol)
  {
    int i, j, k, m, size, index[][][] = new int [8][][], faces[][];
    Vec3 vertices[];

    vertices = subdivideSphere(index, tol);
    size = index[0].length;
    faces = new int [8*(size-1)*(size-1)][];
    m = 0;
    for (i = 0; i < 8; i++)
      if (i == 0 || i == 2 || i == 5 || i == 7)
        {
          for (j = 1; j < size; j++)
            {
              for (k = 0; k < j-1; k++)
                {
                  faces[m++] = new int [] {index[i][j-1][k], index[i][j][k], index[i][j][k+1]};
                  faces[m++] = new int [] {index[i][j-1][k], index[i][j][k+1], index[i][j-1][k+1]};
                }
              faces[m++] = new int [] {index[i][j-1][k], index[i][j][k], index[i][j][k+1]};
            }
        }
      else
        {
          for (j = 1; j < size; j++)
            {
              for (k = 0; k < j-1; k++)
                {
                  faces[m++] = new int [] {index[i][j-1][k], index[i][j][k+1], index[i][j][k]};
                  faces[m++] = new int [] {index[i][j-1][k], index[i][j-1][k+1], index[i][j][k+1]};
                }
              faces[m++] = new int [] {index[i][j-1][k], index[i][j][k+1], index[i][j][k]};
            }
        }
    TriangleMesh mesh = new TriangleMesh(vertices, faces);
    mesh.copyTextureAndMaterial(this);
    return mesh;
  }

  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    int k, m, size, index[][][];
    RenderingTriangle tri[];
    Vec3 vertices[], norm[];

    if (interactive && cachedMesh != null)
      return cachedMesh;
    index = new int [8][][];
    vertices = subdivideSphere(index, tol);
    norm = findNormals(vertices);
    size = index[0].length;
    tri = new RenderingTriangle [8*(size-1)*(size-1)];
    m = 0;
    for (int i = 0; i < 8; i++)
      if (i == 0 || i == 2 || i == 5 || i == 7)
        {
          for (int j = 1; j < size; j++)
            {
              for (k = 0; k < j-1; k++)
                {
                  tri[m++] = texMapping.mapTriangle(index[i][j-1][k], index[i][j][k], index[i][j][k+1], index[i][j-1][k], index[i][j][k], index[i][j][k+1], vertices);
                  tri[m++] = texMapping.mapTriangle(index[i][j-1][k], index[i][j][k+1], index[i][j-1][k+1], index[i][j-1][k], index[i][j][k+1], index[i][j-1][k+1], vertices);
                }
              tri[m++] = texMapping.mapTriangle(index[i][j-1][k], index[i][j][k], index[i][j][k+1], index[i][j-1][k], index[i][j][k], index[i][j][k+1], vertices);
            }
        }
      else
        {
          for (int j = 1; j < size; j++)
            {
              for (k = 0; k < j-1; k++)
                {
                  tri[m++] = texMapping.mapTriangle(index[i][j-1][k], index[i][j][k+1], index[i][j][k], index[i][j-1][k], index[i][j][k+1], index[i][j][k], vertices);
                  tri[m++] = texMapping.mapTriangle(index[i][j-1][k], index[i][j-1][k+1], index[i][j][k+1], index[i][j-1][k], index[i][j-1][k+1], index[i][j][k+1], vertices);
                }
              tri[m++] = texMapping.mapTriangle(index[i][j-1][k], index[i][j][k+1], index[i][j][k], index[i][j-1][k], index[i][j][k+1], index[i][j][k], vertices);
            }
        }
    RenderingMesh mesh = new RenderingMesh(vertices, norm, tri, texMapping, matMapping);
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
  }

  @Override
  public void setMaterial(Material mat, MaterialMapping map)
  {
    super.setMaterial(mat, map);
    cachedMesh = null;
  }

  /* Find a set of facets which approximate the sphere.  This is used both for converting to
     a triangle mesh, and for generating a rendering mesh. */

  Vec3 [] subdivideSphere(int index[][][], double tol)
  {
    int i, j, k, size = 2;
    Vec3 vert[][], newvert[][], v[];
    double max, error;
    
    vert = new Vec3 [][] {{new Vec3(rx, 0.0, 0.0)}, {new Vec3(0.0, ry, 0.0), new Vec3(0.0, 0.0, rz)}};
    do
      {
        // Find the maximum error in the octant of the sphere.

        max = 0.0;
        for (i = 1; i < size; i++)
          {
            for (j = 0; j < i-1; j++)
              {
                error = faceError(vert[i-1][j], vert[i][j], vert[i][j+1]);
                if (error > max)
                  max = error;
                error = faceError(vert[i-1][j], vert[i-1][j+1], vert[i][j+1]);
                if (error > max)
                  max = error;
              }
            error = faceError(vert[i-1][j], vert[i][j], vert[i][j+1]);
            if (error > max)
              max = error;
            if (max > tol)
              break;
          }
        if (max > tol)
          {
            // Subdivide the octant of the sphere.

            newvert = new Vec3 [2*size-1][];
            for (i = 0; i < 2*size-1; i++)
              newvert[i] = new Vec3 [i+1];
            for (i = 0; i < size; i++)
              for (j = 0; j <= i; j++)
                newvert[2*i][2*j] = vert[i][j];
            for (i = 1; i < size; i++)
              for (j = 0; j < i; j++)
                {
                  newvert[2*i][2*j] = vert[i][j];
                  newvert[2*i-1][2*j] = newVertex(vert[i][j].plus(vert[i-1][j]));
                  newvert[2*i][2*j+1] = newVertex(vert[i][j].plus(vert[i][j+1]));
                  newvert[2*i-1][2*j+1] = newVertex(vert[i-1][j].plus(vert[i][j+1]));
                }
            vert = newvert;
            size = 2*size-1;
          }
      } while (max > tol);
      
    // Build the complete sphere by mirroring the octant.

    v = new Vec3 [4*size*size-8*size+6];
    for (i = 0; i < 8; i++)
      {
        index[i] = new int [size][];
        for (j = 0; j < size; j++)
          index[i][j] = new int [j+1];
      }
    k = 0;
    
    // First copy all the vertices in the interior of the octant.
    
    for (i = 1; i < size-1; i++)
      for (j = 1; j < i; j++)
        {
          v[k] = vert[i][j];
          index[0][i][j] = k++;
          v[k] = new Vec3(-vert[i][j].x, vert[i][j].y, vert[i][j].z);
          index[1][i][j] = k++;
          v[k] = new Vec3(-vert[i][j].x, -vert[i][j].y, vert[i][j].z);
          index[2][i][j] = k++;
          v[k] = new Vec3(vert[i][j].x, -vert[i][j].y, vert[i][j].z);
          index[3][i][j] = k++;
          v[k] = new Vec3(vert[i][j].x, vert[i][j].y, -vert[i][j].z);
          index[4][i][j] = k++;
          v[k] = new Vec3(-vert[i][j].x, vert[i][j].y, -vert[i][j].z);
          index[5][i][j] = k++;
          v[k] = new Vec3(-vert[i][j].x, -vert[i][j].y, -vert[i][j].z);
          index[6][i][j] = k++;
          v[k] = new Vec3(vert[i][j].x, -vert[i][j].y, -vert[i][j].z);
          index[7][i][j] = k++;
        }

    // Copy the vertices on the border between two octants.
    
    for (i = 1; i < size-1; i++)
      {
        v[k] = vert[i][0];
        index[0][i][0] = index[4][i][0] = k++;
        v[k] = new Vec3(-vert[i][0].x, vert[i][0].y, vert[i][0].z);
        index[1][i][0] = index[5][i][0] = k++;
        v[k] = new Vec3(vert[i][0].x, -vert[i][0].y, vert[i][0].z);
        index[3][i][0] = index[7][i][0] = k++;
        v[k] = new Vec3(-vert[i][0].x, -vert[i][0].y, vert[i][0].z);
        index[2][i][0] = index[6][i][0] = k++;
        v[k] = vert[size-1][i];
        index[0][size-1][i] = index[1][size-1][i] = k++;
        v[k] = new Vec3(vert[size-1][i].x, -vert[size-1][i].y, vert[size-1][i].z);
        index[2][size-1][i] = index[3][size-1][i] = k++;
        v[k] = new Vec3(vert[size-1][i].x, vert[size-1][i].y, -vert[size-1][i].z);
        index[4][size-1][i] = index[5][size-1][i] = k++;
        v[k] = new Vec3(vert[size-1][i].x, -vert[size-1][i].y, -vert[size-1][i].z);
        index[6][size-1][i] = index[7][size-1][i] = k++;
        v[k] = vert[i][i];
        index[0][i][i] = index[3][i][i] = k++;
        v[k] = new Vec3(-vert[i][i].x, vert[i][i].y, vert[i][i].z);
        index[1][i][i] = index[2][i][i] = k++;
        v[k] = new Vec3(vert[i][i].x, vert[i][i].y, -vert[i][i].z);
        index[4][i][i] = index[7][i][i] = k++;
        v[k] = new Vec3(-vert[i][i].x, vert[i][i].y, -vert[i][i].z);
        index[5][i][i] = index[6][i][i] = k++;
      }
    
    // Finally, copy the vertices at the intersection of four octants.
    
    v[k] = vert[0][0];
    index[0][0][0] = index[3][0][0] = index[4][0][0] = index[7][0][0] = k++;
    v[k] = new Vec3(-vert[0][0].x, vert[0][0].y, vert[0][0].z);
    index[1][0][0] = index[2][0][0] = index[5][0][0] = index[6][0][0] = k++;
    v[k] = vert[size-1][0];
    index[0][size-1][0] = index[1][size-1][0] = index[4][size-1][0] = index[5][size-1][0] = k++;
    v[k] = new Vec3(vert[size-1][0].x, -vert[size-1][0].y, vert[size-1][0].z);
    index[2][size-1][0] = index[3][size-1][0] = index[6][size-1][0] = index[7][size-1][0] = k++;
    v[k] = vert[size-1][size-1];
    index[0][size-1][size-1] = index[1][size-1][size-1] = index[2][size-1][size-1] = index[3][size-1][size-1] = k++;
    v[k] = new Vec3(vert[size-1][size-1].x, vert[size-1][size-1].y, -vert[size-1][size-1].z);
    index[4][size-1][size-1] = index[5][size-1][size-1] = index[6][size-1][size-1] = index[7][size-1][size-1] = k++;
    return v;
  }
  
  double faceError(Vec3 v1, Vec3 v2, Vec3 v3)
  {
    Vec3 n, v;
    double dist1, dist2, d;
    
    // Find a unit vector midway between the other three.

    v = new Vec3(v1.x+v2.x+v3.x, v1.y+v2.y+v3.y, v1.z+v2.z+v3.z);
    v.normalize();
    
    // Find the distance at which the vector intersects the ellipse.

    dist1 = Math.sqrt(1.0/((v.x*v.x)/(rx*rx)+(v.y*v.y)/(ry*ry)+(v.z*v.z)/(rz*rz)));

    // Find the distance at which the vector intersects the plane of the other three points.

    n = (v2.minus(v1)).cross(v3.minus(v1));
    n.normalize();
    d = v1.dot(n);
    dist2 = d/(v.dot(n));
    return dist1-dist2;
  }
  
  Vec3 newVertex(Vec3 v)
  {
    v.normalize();
    double dist = Math.sqrt(1.0/((v.x*v.x)/(rx*rx)+(v.y*v.y)/(ry*ry)+(v.z*v.z)/(rz*rz)));
    v.scale(dist);
    return v;
  }

  Vec3 [] findNormals(Vec3 v[])
  {
    Vec3 n[] = new Vec3 [v.length];
    for (int i = 0; i < v.length; i++)
      {
        n[i] = new Vec3 (v[i].x/(rx*rx), v[i].y/(ry*ry), v[i].z/(rz*rz));
        n[i].normalize();
      }
    return n;
  }
  
  public boolean isEditable()
  {
    return true;
  }
  
  /* Allow the user to edit the sphere's shape. */
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    ValueField xField = new ValueField(rx, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(ry, ValueField.POSITIVE, 5);
    ValueField zField = new ValueField(rz, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editSphereTitle"),
      new Widget [] {xField, yField, zField}, new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    setSize(2.0*xField.getValue(), 2.0*yField.getValue(), 2.0*zField.getValue());
    cb.run();
  }

  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public Sphere(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    rx = in.readDouble();
    ry = in.readDouble();
    rz = in.readDouble();
    bounds = new BoundingBox(-rx, rx, -ry, ry, -rz, rz);
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(0);
    out.writeDouble(rx);
    out.writeDouble(ry);
    out.writeDouble(rz);
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
        return new Double(ry);
      case 2:
        return new Double(rz);
    }
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
    double val = ((Double) value).doubleValue();
    if (index == 0)
      setSize(2.0*val, 2.0*ry, 2.0*rz);
    else if (index == 1)
      setSize(2.0*rx, 2.0*val, 2.0*rz);
    else if (index == 2)
      setSize(2.0*rx, 2.0*ry, 2.0*val);
  }

  /* Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new VectorKeyframe(rx, ry, rz);
  }
  
  /* Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    VectorKeyframe key = (VectorKeyframe) k;
    
    setSize(2.0*key.x, 2.0*key.y, 2.0*key.z);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"X Radius", "Y Radius", "Z Radius"},
        new double [] {rx, ry, rz}, 
        new double [][] {{0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}});
  }
  
  /* Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    VectorKeyframe key = (VectorKeyframe) k;
    ValueField xField = new ValueField(key.x, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(key.y, ValueField.POSITIVE, 5);
    ValueField zField = new ValueField(key.z, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editSphereTitle"),
      new Widget [] {xField, yField, zField}, new String [] {"X", "Y", "Z"});    
    if (!dlg.clickedOk())
      return;
    key.set(xField.getValue(), yField.getValue(), zField.getValue());
  }
}
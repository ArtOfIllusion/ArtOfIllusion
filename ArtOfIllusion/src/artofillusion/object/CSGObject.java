/* Copyright (C) 2001-2009 by Peter Eastman

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
import java.io.*;
import java.lang.reflect.*;

/** A CSGObject is an Object3D that represents the union, intersection, or difference of 
    two component objects. */

public class CSGObject extends Object3D
{
  ObjectInfo obj1, obj2;
  int operation;
  RenderingMesh cachedMesh;
  WireframeMesh cachedWire;
  BoundingBox bounds;

  public static final int UNION = 0;
  public static final int INTERSECTION = 1;
  public static final int DIFFERENCE12 = 2;
  public static final int DIFFERENCE21 = 3;
  
  /* Create a CSGObject based on two objects and an operation. */
  
  public CSGObject(ObjectInfo o1, ObjectInfo o2, int op)
  {
    obj1 = o1.duplicate();
    obj2 = o2.duplicate();
    obj1.setObject(obj1.getObject().duplicate());
    obj2.setObject(obj2.getObject().duplicate());
    operation = op;
    obj1.setVisible(obj2.visible = true);
  }
  
  /** Create a new object which is an exact duplicate of this one. */
  
  public Object3D duplicate()
  {
    CSGObject obj = new CSGObject(obj1, obj2, operation);
    obj.copyTextureAndMaterial(this);
    return obj;
  }
  
  /** Copy all the properties of another object, to make this one identical to it.  If the
      two objects are of different classes, this will throw a ClassCastException. */
  
  public void copyObject(Object3D obj)
  {
    CSGObject csg = (CSGObject) obj;

    obj1 = csg.obj1.duplicate();
    obj2 = csg.obj2.duplicate();
    obj1.setObject(obj1.object.duplicate());
    obj2.setObject(obj2.object.duplicate());
    operation = csg.operation;
    cachedMesh = csg.cachedMesh;
    cachedWire = csg.cachedWire;
    copyTextureAndMaterial(obj);
    bounds = null;
  }
  
  /** Get the first object. */
  
  public ObjectInfo getObject1()
  {
    return obj1;
  }
  
  /** Get the second object. */
  
  public ObjectInfo getObject2()
  {
    return obj2;
  }
  
  /** Get the boolean operation to be performed. */
  
  public int getOperation()
  {
    return operation;
  }
  
  /** Set the boolean operation to be performed. */
  
  public void setOperation(int op)
  {
    operation = op;
  }
  
  /** Set the component objects. */
  
  public void setComponentObjects(ObjectInfo o1, ObjectInfo o2)
  {
    obj1 = o1;
    obj2 = o2;
    bounds = null;
    cachedMesh = null;
    cachedWire = null;
  }

  /** Center the component objects, and return the vector by which they were displaced. */
  
  public Vec3 centerObjects()
  {
    BoundingBox b1 = obj1.getBounds().transformAndOutset(obj1.getCoords().fromLocal());
    BoundingBox b2 = obj2.getBounds().transformAndOutset(obj2.getCoords().fromLocal());
    BoundingBox b = b1.merge(b2);
    Vec3 center = b.getCenter();
    obj1.getCoords().setOrigin(obj1.getCoords().getOrigin().minus(center));
    obj2.getCoords().setOrigin(obj2.getCoords().getOrigin().minus(center));
    bounds = null;
    cachedMesh = null;
    cachedWire = null;
    return center;
  }

  /** Get a BoundingBox which just encloses the object. */

  public BoundingBox getBounds()
  {
    if (bounds == null)
      findBounds();
    return bounds;
  }

  /** Calculate the (approximate) bounding box for the object. */

  void findBounds()
  {
    double minx, miny, minz, maxx, maxy, maxz;
    Vec3 vert[];
    int i;
    
    if (cachedMesh != null)
      vert = cachedMesh.vert;
    else if (cachedWire != null)
      vert = cachedWire.vert;
    else
      {
        getWireframeMesh();
        vert = cachedWire.vert;
      }

    minx = maxx = vert[0].x;
    miny = maxy = vert[0].y;
    minz = maxz = vert[0].z;
    for (i = 1; i < vert.length; i++)
      {
        if (vert[i].x < minx) minx = vert[i].x;
        if (vert[i].x > maxx) maxx = vert[i].x;
        if (vert[i].y < miny) miny = vert[i].y;
        if (vert[i].y > maxy) maxy = vert[i].y;
        if (vert[i].z < minz) minz = vert[i].z;
        if (vert[i].z > maxz) maxz = vert[i].z;
      }
    bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
  }

  /** Resize the object. */

  public void setSize(double xsize, double ysize, double zsize)
  {
    Vec3 size = bounds.getSize(), objSize;
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
    
    // Adjust the size and position of each component object.
    
    objSize = obj1.getBounds().getSize();
    obj1.getObject().setSize(objSize.x*xscale, objSize.y*yscale, objSize.z*zscale);
    objSize = obj2.getBounds().getSize();
    obj2.getObject().setSize(objSize.x*xscale, objSize.y*yscale, objSize.z*zscale);
    Mat4 m = Mat4.scale(xscale, yscale, zscale);
    obj1.getCoords().transformOrigin(m);
    obj2.getCoords().transformOrigin(m);
    cachedMesh = null;
    cachedWire = null;
    findBounds();
  }

  /** Tells whether the object can be converted to a TriangleMesh. */
  
  public int canConvertToTriangleMesh()
  {
    if (obj1.getObject().canConvertToTriangleMesh() == EXACTLY && obj2.getObject().canConvertToTriangleMesh() == EXACTLY)
      return EXACTLY;
    return APPROXIMATELY;
  }
  
  /** This object is closed if both of its component objects are closed. */
  
  public boolean isClosed()
  {
    return (obj1.getObject().isClosed() && obj2.getObject().isClosed());
  }
  
  /** Create a triangle mesh representing this object. */

  public TriangleMesh convertToTriangleMesh(double tol)
  {
    TriangleMesh mesh1, mesh2;
    
    mesh1 = obj1.getObject().convertToTriangleMesh(tol);
    mesh2 = obj2.getObject().convertToTriangleMesh(tol);
    CSGModeller modeller = new CSGModeller(mesh1, mesh2, obj1.getCoords(), obj2.getCoords());
    TriangleMesh trimesh = modeller.getMesh(operation, getTexture());
    trimesh.copyTextureAndMaterial(this);
    return trimesh;
  }
  
  /** Allow the user to edit this object. */
  
  public boolean isEditable()
  {
    return true;
  }
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    new CSGEditorWindow(parent, info.getName(), this, cb);
  }
    
  /** When setting the texture or material, also set it for each of the component objects. */
     
  public void setTexture(Texture tex, TextureMapping mapping)
  {
    super.setTexture(tex, mapping);
    if (obj1 == null)
      return;
    obj1.getObject().setTexture(tex, mapping);
    obj2.getObject().setTexture(tex, mapping);
  }

  public void setMaterial(Material mat, MaterialMapping mapping)
  {
    super.setMaterial(mat, mapping);
    if (obj1 == null)
      return;
    obj1.getObject().setMaterial(mat, mapping);
    obj2.getObject().setMaterial(mat, mapping);
  }

  /** Get a RenderingMesh for this object. */

  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    if (interactive)
      {
        if (cachedMesh == null)
          cacheMeshes(tol, info);
        return cachedMesh;
      }
    return convertToTriangleMesh(tol).getRenderingMesh(tol, false, info);
  }
  
  /** Get a WireframeMesh for this object. */
  
  public WireframeMesh getWireframeMesh()
  {
    if (cachedWire == null)
      cacheMeshes(ArtOfIllusion.getPreferences().getInteractiveSurfaceError(), null);
    return cachedWire;
  }

  /** Build the preview meshes and save them for later use. */
  
  private void cacheMeshes(double tol, ObjectInfo info)
  {
    TriangleMesh mesh = convertToTriangleMesh(tol);
    cachedMesh = mesh.getRenderingMesh(tol, true, info);
    TriangleMesh.Edge edge[] = mesh.getEdges();
    int to[] = new int [edge.length], from[] = new int [edge.length];
    
    for (int i = 0; i < edge.length; i++)
      {
        to[i] = edge[i].v1;
        from[i] = edge[i].v2;
      }
    cachedWire = new WireframeMesh(cachedMesh.vert, from, to);
  }

  /** Save this object to an output stream. */
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);
    out.writeShort(0);
    out.writeInt(operation);
    obj1.getCoords().writeToFile(out);
    out.writeUTF(obj1.getName());
    out.writeUTF(obj1.getObject().getClass().getName());
    obj1.getObject().writeToFile(out, theScene);
    obj2.getCoords().writeToFile(out);
    out.writeUTF(obj2.getName());
    out.writeUTF(obj2.getObject().getClass().getName());
    obj2.getObject().writeToFile(out, theScene);
  }
  
  public CSGObject(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    operation = in.readInt();
    try
      {
        obj1 = new ObjectInfo(null, new CoordinateSystem(in), in.readUTF());
        Class cls = ArtOfIllusion.getClass(in.readUTF());
        Constructor con = cls.getConstructor(DataInputStream.class, Scene.class);
        obj1.setObject((Object3D) con.newInstance(in, theScene));
        obj2 = new ObjectInfo(null, new CoordinateSystem(in), in.readUTF());
        cls = ArtOfIllusion.getClass(in.readUTF());
        con = cls.getConstructor(DataInputStream.class, Scene.class);
        obj2.setObject((Object3D) con.newInstance(in, theScene));
      }
    catch (InvocationTargetException ex)
      {
        ex.getTargetException().printStackTrace();
        throw new IOException();
      }
    catch (Exception ex)
      {
        ex.printStackTrace();
        throw new IOException();
      }
    obj1.getObject().setTexture(getTexture(), getTextureMapping());
    obj2.getObject().setTexture(getTexture(), getTextureMapping());
    if (getMaterial() != null)
      {
        obj1.getObject().setMaterial(getMaterial(), getMaterialMapping());
        obj2.getObject().setMaterial(getMaterial(), getMaterialMapping());
      }
  }
  
  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new CSGKeyframe(obj1.getObject().getPoseKeyframe(), obj2.getObject().getPoseKeyframe(),
      obj1.getCoords().duplicate(), obj2.getCoords().duplicate());
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    CSGKeyframe key = (CSGKeyframe) k;
    if (key.key1 != null)
      obj1.getObject().applyPoseKeyframe(key.key1);
    if (key.key2 != null)
      obj2.getObject().applyPoseKeyframe(key.key2);
    obj1.getCoords().copyCoords(key.coords1);
    obj2.getCoords().copyCoords(key.coords2);
    cachedMesh = null;
    cachedWire = null;
  }
  
  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, final Keyframe k, final ObjectInfo info)
  {
    final CSGObject copy = (CSGObject) duplicate();
    copy.applyPoseKeyframe(k);
    Runnable onClose = new Runnable() {
      public void run()
      {
        CSGKeyframe original = (CSGKeyframe) k;
        CSGKeyframe edited = (CSGKeyframe) copy.getPoseKeyframe().duplicate(info);
        original.coords1 = edited.coords1;
        original.coords2 = edited.coords2;
        original.key1 = edited.key1;
        original.key2 = edited.key2;
      }
    };
    new CSGEditorWindow(parent, info.getName(), copy, onClose);
  }
  
  /** Inner class representing a pose for a CSGObject. */
  
  public static class CSGKeyframe implements Keyframe
  {
    public Keyframe key1, key2;
    public CoordinateSystem coords1, coords2;
    
    public CSGKeyframe(Keyframe key1, Keyframe key2, CoordinateSystem coords1, CoordinateSystem coords2)
    {
      this.key1 = key1;
      this.key2 = key2;
      this.coords1 = coords1;
      this.coords2 = coords2;
    }
    
    /** Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return new CSGKeyframe(key1.duplicate(), key2.duplicate(), coords1.duplicate(), coords2.duplicate());
    }
    
    /** Create a duplicate of this keyframe for a (possibly different) object. */
  
    public Keyframe duplicate(Object owner)
    {
      CSGObject csg = (CSGObject) ((ObjectInfo) owner).getObject();
      return new CSGKeyframe(key1.duplicate(csg.obj1), key2.duplicate(csg.obj2), coords1.duplicate(), coords2.duplicate());
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

    /* These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      CSGKeyframe k2 = (CSGKeyframe) o2;
      RotationKeyframe rot1, rot2;
      VectorKeyframe orig1, orig2;
      CoordinateSystem c1, c2;
      
      // Find the new coordinate system for the first object.
      
      rot1 = new RotationKeyframe(coords1);
      rot2 = new RotationKeyframe(k2.coords1);
      rot1.setUseQuaternion(true);
      rot2.setUseQuaternion(true);
      orig1 = new VectorKeyframe(coords1.getOrigin());
      orig2 = new VectorKeyframe(k2.coords1.getOrigin());
      c1 = new CoordinateSystem((Vec3) orig1.blend(orig2, weight1, weight2), Vec3.vz(), Vec3.vy());
      ((RotationKeyframe) rot1.blend(rot2, weight1, weight2)).applyToCoordinates(c1, 1.0, null, null, false,
        true, true, true);
      
      // Find the new coordinate system for the second object.
      
      rot1 = new RotationKeyframe(coords2);
      rot2 = new RotationKeyframe(k2.coords2);
      rot1.setUseQuaternion(true);
      rot2.setUseQuaternion(true);
      orig1 = new VectorKeyframe(coords2.getOrigin());
      orig2 = new VectorKeyframe(k2.coords2.getOrigin());
      c2 = new CoordinateSystem((Vec3) orig1.blend(orig2, weight1, weight2), Vec3.vz(), Vec3.vy());
      ((RotationKeyframe) rot1.blend(rot2, weight1, weight2)).applyToCoordinates(c2, 1.0, null, null, false,
        true, true, true);
      
      // Construct the new keyframe.

      return new CSGKeyframe(key1.blend(k2.key1, weight1, weight2), 
        key2.blend(k2.key2, weight1, weight2), c1, c2);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      CSGKeyframe k2 = (CSGKeyframe) o2, k3 = (CSGKeyframe) o3;
      RotationKeyframe rot1, rot2, rot3;
      VectorKeyframe orig1, orig2, orig3;
      CoordinateSystem c1, c2;
      
      // Find the new coordinate system for the first object.
      
      rot1 = new RotationKeyframe(coords1);
      rot2 = new RotationKeyframe(k2.coords1);
      rot3 = new RotationKeyframe(k3.coords1);
      rot1.setUseQuaternion(true);
      rot2.setUseQuaternion(true);
      rot3.setUseQuaternion(true);
      orig1 = new VectorKeyframe(coords1.getOrigin());
      orig2 = new VectorKeyframe(k2.coords1.getOrigin());
      orig3 = new VectorKeyframe(k3.coords1.getOrigin());
      c1 = new CoordinateSystem((Vec3) orig1.blend(orig2, orig3, weight1, weight2, weight3), 
        Vec3.vz(), Vec3.vy());
      ((RotationKeyframe) rot1.blend(rot2, rot3, weight1, weight2, weight3)).applyToCoordinates(c1, 1.0, 
        null, null, false, true, true, true);
      
      // Find the new coordinate system for the second object.
      
      rot1 = new RotationKeyframe(coords2);
      rot2 = new RotationKeyframe(k2.coords2);
      rot3 = new RotationKeyframe(k3.coords2);
      rot1.setUseQuaternion(true);
      rot2.setUseQuaternion(true);
      rot3.setUseQuaternion(true);
      orig1 = new VectorKeyframe(coords2.getOrigin());
      orig2 = new VectorKeyframe(k2.coords2.getOrigin());
      orig3 = new VectorKeyframe(k3.coords2.getOrigin());
      c2 = new CoordinateSystem((Vec3) orig1.blend(orig2, orig3, weight1, weight2, weight3), 
        Vec3.vz(), Vec3.vy());
      ((RotationKeyframe) rot1.blend(rot2, rot3, weight1, weight2, weight3)).applyToCoordinates(c2, 1.0, 
        null, null, false, true, true, true);
      
      // Construct the new keyframe.

      return new CSGKeyframe(key1.blend(k2.key1, k3.key1, weight1, weight2, weight3), 
        key2.blend(k2.key2, k3.key2, weight1, weight2, weight3), c1, c2);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      CSGKeyframe k2 = (CSGKeyframe) o2, k3 = (CSGKeyframe) o3, k4 = (CSGKeyframe) o4;
      RotationKeyframe rot1, rot2, rot3, rot4;
      VectorKeyframe orig1, orig2, orig3, orig4;
      CoordinateSystem c1, c2;
      
      // Find the new coordinate system for the first object.
      
      rot1 = new RotationKeyframe(coords1);
      rot2 = new RotationKeyframe(k2.coords1);
      rot3 = new RotationKeyframe(k3.coords1);
      rot4 = new RotationKeyframe(k4.coords1);
      rot1.setUseQuaternion(true);
      rot2.setUseQuaternion(true);
      rot3.setUseQuaternion(true);
      rot4.setUseQuaternion(true);
      orig1 = new VectorKeyframe(coords1.getOrigin());
      orig2 = new VectorKeyframe(k2.coords1.getOrigin());
      orig3 = new VectorKeyframe(k3.coords1.getOrigin());
      orig4 = new VectorKeyframe(k4.coords1.getOrigin());
      c1 = new CoordinateSystem((Vec3) orig1.blend(orig2, orig3, orig4, weight1, weight2, weight3, weight4), 
        Vec3.vz(), Vec3.vy());
      ((RotationKeyframe) rot1.blend(rot2, rot3, rot4, weight1, weight2, weight3, weight4)).applyToCoordinates(c1, 1.0, 
        null, null, false, true, true, true);
      
      // Find the new coordinate system for the second object.
      
      rot1 = new RotationKeyframe(coords2);
      rot2 = new RotationKeyframe(k2.coords2);
      rot3 = new RotationKeyframe(k3.coords2);
      rot4 = new RotationKeyframe(k4.coords2);
      rot1.setUseQuaternion(true);
      rot2.setUseQuaternion(true);
      rot3.setUseQuaternion(true);
      rot4.setUseQuaternion(true);
      orig1 = new VectorKeyframe(coords2.getOrigin());
      orig2 = new VectorKeyframe(k2.coords2.getOrigin());
      orig3 = new VectorKeyframe(k3.coords2.getOrigin());
      orig4 = new VectorKeyframe(k4.coords2.getOrigin());
      c2 = new CoordinateSystem((Vec3) orig1.blend(orig2, orig3, orig4, weight1, weight2, weight3, weight4), 
        Vec3.vz(), Vec3.vy());
      ((RotationKeyframe) rot1.blend(rot2, rot3, rot3, weight1, weight2, weight3, weight4)).applyToCoordinates(c2, 1.0, 
        null, null, false, true, true, true);
      
      // Construct the new keyframe.

      return new CSGKeyframe(key1.blend(k2.key1, k3.key1, k4.key1, weight1, weight2, 
        weight3, weight4), key2.blend(k2.key2, k3.key2, k4.key2, weight1, weight2, 
        weight3, weight4), c1, c2);
    }

    /** Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof CSGKeyframe))
        return false;
      CSGKeyframe key = (CSGKeyframe) k;
      if (key1 != key.key1 && (key1 == null || key.key1 == null || !key1.equals(key.key1)))
        return false;
      if (key2 != key.key2 && (key2 == null || key.key2 == null || !key2.equals(key.key2)))
        return false;
      if (!coords1.getOrigin().equals(key.coords1.getOrigin()))
        return false;
      if (!coords1.getZDirection().equals(key.coords1.getZDirection()))
        return false;
      if (!coords1.getUpDirection().equals(key.coords1.getUpDirection()))
        return false;
      if (!coords2.getOrigin().equals(key.coords2.getOrigin()))
        return false;
      if (!coords2.getZDirection().equals(key.coords2.getZDirection()))
        return false;
      if (!coords2.getUpDirection().equals(key.coords2.getUpDirection()))
        return false;
      return true;
    }
  
    /** Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeShort(0);
      coords1.writeToFile(out);
      coords2.writeToFile(out);
      out.writeUTF(key1.getClass().getName());
      key1.writeToStream(out);
      out.writeUTF(key2.getClass().getName());
      key2.writeToStream(out);
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public CSGKeyframe(DataInputStream in, Object parent) throws IOException
    {
      short version = in.readShort();
      if (version != 0)
        throw new InvalidObjectException("");
      CSGObject obj = (CSGObject) ((ObjectInfo) parent).object;
      coords1 = new CoordinateSystem(in);
      coords2 = new CoordinateSystem(in);
      try
      {
        Class cl = ArtOfIllusion.getClass(in.readUTF());
        Constructor con = cl.getConstructor(DataInputStream.class, Object.class);
        key1 = (Keyframe) con.newInstance(in, obj.getObject1().getObject());
        cl = ArtOfIllusion.getClass(in.readUTF());
        con = cl.getConstructor(DataInputStream.class, Object.class);
        key2 = (Keyframe) con.newInstance(in, obj.getObject2().getObject());
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        throw new InvalidObjectException("");
      }
    }
  }
}
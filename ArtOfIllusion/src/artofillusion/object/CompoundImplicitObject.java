/* Copyright (C) 2013 by Peter Eastman

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
import artofillusion.ui.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class CompoundImplicitObject extends ImplicitObject
{
  private ArrayList<ImplicitObject> objects;
  private ArrayList<CoordinateSystem> objectCoords;
  private BoundingBox bounds;
  private volatile ArrayList<ArrayList<Integer>> grid;
  private int gridSize;
  private double cutoff;

  public CompoundImplicitObject()
  {
    objects = new ArrayList<ImplicitObject>();
    objectCoords = new ArrayList<CoordinateSystem>();
    cutoff = 1.0;
  }

  public void addObject(ImplicitObject obj, CoordinateSystem coords)
  {
    objects.add(obj);
    objectCoords.add(coords.duplicate());
    objectsChanged();
  }

  public int getNumObjects()
  {
    return objects.size();
  }

  public ImplicitObject getObject(int index)
  {
    return objects.get(index);
  }

  public void setObject(int index, ImplicitObject obj)
  {
    objects.set(index, obj);
    objectsChanged();
  }

  public CoordinateSystem getObjectCoordinates(int index)
  {
    return objectCoords.get(index).duplicate();
  }

  public void setObjectCoordinates(int index, CoordinateSystem coords)
  {
    objectCoords.set(index, coords.duplicate());
    objectsChanged();
  }

  private void objectsChanged()
  {
    bounds = null;
    grid = null;
    clearCachedMesh();
  }

  @Override
  public double getCutoff()
  {
    return cutoff;
  }

  public void setCutoff(double cutoff)
  {
    this.cutoff = cutoff;
  }

  @Override
  public double getFieldValue(double x, double y, double z, double size, double time)
  {
    double value = 0;
    Vec3 pos = new Vec3();
    ArrayList<Integer> indices = findObjectsNearPoint(x, y, z);
    for (int i = 0; i < indices.size(); i++)
    {
      int index = indices.get(i);
      Mat4 toLocal = objectCoords.get(index).toLocal();
      pos.set(x, y, z);
      toLocal.transform(pos);
      ImplicitObject obj = objects.get(index);
      if (obj.getBounds().contains(pos))
        value += obj.getFieldValue(pos.x, pos.y, pos.z, size, time);
    }
    return value;
  }

  @Override
  public void getFieldGradient(double x, double y, double z, double size, double time, Vec3 grad)
  {
    double dx = 0, dy = 0, dz = 0;
    Vec3 pos = new Vec3();
    ArrayList<Integer> indices = findObjectsNearPoint(x, y, z);
    for (int i = 0; i < indices.size(); i++)
    {
      int index = indices.get(i);
      Mat4 toLocal = objectCoords.get(index).toLocal();
      pos.set(x, y, z);
      toLocal.transform(pos);
      ImplicitObject obj = objects.get(index);
      if (obj.getBounds().contains(pos))
      {
        obj.getFieldGradient(pos.x, pos.y, pos.z, size, time, grad);
        dx += grad.x;
        dy += grad.y;
        dz += grad.z;
      }
    }
    grad.set(dx, dy, dz);
  }

  private ArrayList<Integer> findObjectsNearPoint(double x, double y, double z)
  {
    BoundingBox gridBounds = getBounds();
    if (grid == null)
    {
      synchronized (this)
      {
        if (grid == null)
        {
          gridSize = (int) Math.ceil(Math.pow(objects.size(), 1.0/3.0));
          grid = new ArrayList<ArrayList<Integer>>(gridSize*gridSize*gridSize);
          for (int i = 0; i < gridSize*gridSize*gridSize; i++)
            grid.add(null);
          for (int index = 0; index < objects.size(); index++)
          {
            BoundingBox objBounds = objects.get(index).getBounds().transformAndOutset(objectCoords.get(index).fromLocal());
            int minx = (int) Math.floor(gridSize*(objBounds.minx-gridBounds.minx)/(gridBounds.maxx-gridBounds.minx));
            int maxx = (int) Math.ceil(gridSize*(objBounds.maxx-gridBounds.minx)/(gridBounds.maxx-gridBounds.minx));
            int miny = (int) Math.floor(gridSize*(objBounds.miny-gridBounds.miny)/(gridBounds.maxy-gridBounds.miny));
            int maxy = (int) Math.ceil(gridSize*(objBounds.maxy-gridBounds.miny)/(gridBounds.maxy-gridBounds.miny));
            int minz = (int) Math.floor(gridSize*(objBounds.minz-gridBounds.minz)/(gridBounds.maxz-gridBounds.minz));
            int maxz = (int) Math.ceil(gridSize*(objBounds.maxz-gridBounds.minz)/(gridBounds.maxz-gridBounds.minz));
            minx = Math.max(0, Math.min(gridSize-1, minx));
            maxx = Math.max(0, Math.min(gridSize-1, maxx));
            miny = Math.max(0, Math.min(gridSize-1, miny));
            maxy = Math.max(0, Math.min(gridSize-1, maxy));
            minz = Math.max(0, Math.min(gridSize-1, minz));
            maxz = Math.max(0, Math.min(gridSize-1, maxz));
            for (int i = minx; i <= maxx; i++)
              for (int j = miny; j <= maxy; j++)
                for (int k = minz; k <= maxz; k++)
                {
                  int n = k+gridSize*(j+gridSize*i);
                  if (grid.get(n) == null)
                    grid.set(n, new ArrayList<Integer>());
                  grid.get(n).add(index);
                }
          }
        }
      }
      ArrayList<Integer> empty = new ArrayList<Integer>();
      for (int i = 0; i < gridSize*gridSize*gridSize; i++)
        if (grid.get(i) == null)
          grid.set(i, empty);
    }
    int i = (int) (gridSize*(x-gridBounds.minx)/(gridBounds.maxx-gridBounds.minx));
    int j = (int) (gridSize*(y-gridBounds.miny)/(gridBounds.maxy-gridBounds.miny));
    int k = (int) (gridSize*(z-gridBounds.minz)/(gridBounds.maxz-gridBounds.minz));
    i = Math.max(0, Math.min(gridSize-1, i));
    j = Math.max(0, Math.min(gridSize-1, j));
    k = Math.max(0, Math.min(gridSize-1, k));
    return grid.get(k+gridSize*(j+gridSize*i));
  }

  @Override
  public boolean getPreferDirectRendering()
  {
    return true;
  }

  @Override
  public Object3D duplicate()
  {
    CompoundImplicitObject c = new CompoundImplicitObject();
    c.copyObject(this);
    return c;
  }

  @Override
  public void copyObject(Object3D obj)
  {
    CompoundImplicitObject c = (CompoundImplicitObject) obj;
    objects.clear();
    objectCoords.clear();
    for (int i = 0; i < c.getNumObjects(); i++)
      addObject((ImplicitObject) c.getObject(i).duplicate(), c.getObjectCoordinates(i).duplicate());
  }

  @Override
  public synchronized BoundingBox getBounds()
  {
    if (bounds == null)
    {
      if (objects.size() == 0)
        bounds = new BoundingBox(0, 0, 0, 0, 0, 0);
      else
      {
        bounds = objects.get(0).getBounds().transformAndOutset(objectCoords.get(0).fromLocal());
        for (int i = 1; i < objects.size(); i++)
          bounds = bounds.merge(objects.get(i).getBounds().transformAndOutset(objectCoords.get(i).fromLocal()));
      }
    }
    return bounds;
  }

  @Override
  public void setSize(double xsize, double ysize, double zsize)
  {
  }

  /** Allow the user to edit this object. */

  public boolean isEditable()
  {
    return true;
  }

  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    new CompoundImplicitEditorWindow(parent, info.getName(), this, cb);
  }

  @Override
  public Keyframe getPoseKeyframe()
  {
    ArrayList<Keyframe> key = new ArrayList<Keyframe>();
    ArrayList<CoordinateSystem> coords = new ArrayList<CoordinateSystem>();
    for (int i = 0; i < getNumObjects(); i++)
    {
      key.add(getObject(i).getPoseKeyframe());
      coords.add(getObjectCoordinates(i).duplicate());
    }
    return new CompoundImplicitKeyframe(key, coords);
  }

  @Override
  public void applyPoseKeyframe(Keyframe k)
  {
    CompoundImplicitKeyframe key = (CompoundImplicitKeyframe) k;
    for (int i = 0; i < getNumObjects(); i++)
    {
      if (key.key.get(i) != null)
        objects.get(i).applyPoseKeyframe(key.key.get(i));
      objectCoords.get(i).copyCoords(key.coords.get(i));
    }
    objectsChanged();
  }

  @Override
  public void editKeyframe(EditingWindow parent, final Keyframe k, final ObjectInfo info)
  {
    final CompoundImplicitObject copy = (CompoundImplicitObject) duplicate();
    copy.applyPoseKeyframe(k);
    Runnable onClose = new Runnable() {
      public void run()
      {
        CompoundImplicitKeyframe original = (CompoundImplicitKeyframe) k;
        CompoundImplicitKeyframe edited = (CompoundImplicitKeyframe) copy.getPoseKeyframe().duplicate(info);
        original.key = edited.key;
        original.coords = edited.coords;
      }
    };
    new CompoundImplicitEditorWindow(parent, info.getName(), copy, onClose);
  }


  /** Inner class representing a pose for a CompoundImplicitObject. */

  public static class CompoundImplicitKeyframe implements Keyframe
  {
    public ArrayList<Keyframe> key;
    public ArrayList<CoordinateSystem> coords;

    public CompoundImplicitKeyframe(ArrayList<Keyframe> key, ArrayList<CoordinateSystem> coords)
    {
      this.key = key;
      this.coords = coords;
    }

    /** Create a duplicate of this keyframe. */

    public Keyframe duplicate()
    {
      ArrayList<Keyframe> newKey = new ArrayList<Keyframe>();
      ArrayList<CoordinateSystem> newCoords = new ArrayList<CoordinateSystem>();
      for (int i = 0; i < key.size(); i++)
      {
        newKey.add(key.get(i).duplicate());
        newCoords.add(coords.get(i).duplicate());
      }
      return new CompoundImplicitKeyframe(newKey, newCoords);
    }

    /** Create a duplicate of this keyframe for a (possibly different) object. */

    public Keyframe duplicate(Object owner)
    {
      CompoundImplicitObject other = (CompoundImplicitObject) ((ObjectInfo) owner).getObject();
      ArrayList<Keyframe> newKey = new ArrayList<Keyframe>();
      ArrayList<CoordinateSystem> newCoords = new ArrayList<CoordinateSystem>();
      for (int i = 0; i < key.size(); i++)
      {
        newKey.add(key.get(i).duplicate(other.getObject(i)));
        newCoords.add(coords.get(i).duplicate());
      }
      return new CompoundImplicitKeyframe(newKey, newCoords);
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
      CompoundImplicitKeyframe k2 = (CompoundImplicitKeyframe) o2;
      ArrayList<Keyframe> newKey = new ArrayList<Keyframe>();
      ArrayList<CoordinateSystem> newCoords = new ArrayList<CoordinateSystem>();
      for (int i = 0; i < key.size(); i++)
      {
        // Blend the new coordinate systems.

        CoordinateSystem coords1 = coords.get(i);
        CoordinateSystem coords2 = k2.coords.get(i);
        RotationKeyframe rot1 = new RotationKeyframe(coords1);
        RotationKeyframe rot2 = new RotationKeyframe(coords2);
        rot1.setUseQuaternion(true);
        rot2.setUseQuaternion(true);
        VectorKeyframe orig1 = new VectorKeyframe(coords1.getOrigin());
        VectorKeyframe orig2 = new VectorKeyframe(coords2.getOrigin());
        CoordinateSystem c = new CoordinateSystem((Vec3) orig1.blend(orig2, weight1, weight2), Vec3.vz(), Vec3.vy());
        ((RotationKeyframe) rot1.blend(rot2, weight1, weight2)).applyToCoordinates(c, 1.0, null, null, false, true, true, true);
        newCoords.add(c);

        // Blend the object keyframes.

        newKey.add(key.get(i).blend(k2.key.get(i), weight1, weight2));
      }
      return new CompoundImplicitKeyframe(newKey, newCoords);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      CompoundImplicitKeyframe k2 = (CompoundImplicitKeyframe) o2, k3 = (CompoundImplicitKeyframe) o3;
      ArrayList<Keyframe> newKey = new ArrayList<Keyframe>();
      ArrayList<CoordinateSystem> newCoords = new ArrayList<CoordinateSystem>();
      for (int i = 0; i < key.size(); i++)
      {
        // Blend the new coordinate systems.

        CoordinateSystem coords1 = coords.get(i);
        CoordinateSystem coords2 = k2.coords.get(i);
        CoordinateSystem coords3 = k3.coords.get(i);
        RotationKeyframe rot1 = new RotationKeyframe(coords1);
        RotationKeyframe rot2 = new RotationKeyframe(coords2);
        RotationKeyframe rot3 = new RotationKeyframe(coords3);
        rot1.setUseQuaternion(true);
        rot2.setUseQuaternion(true);
        rot3.setUseQuaternion(true);
        VectorKeyframe orig1 = new VectorKeyframe(coords1.getOrigin());
        VectorKeyframe orig2 = new VectorKeyframe(coords2.getOrigin());
        VectorKeyframe orig3 = new VectorKeyframe(coords3.getOrigin());
        CoordinateSystem c = new CoordinateSystem((Vec3) orig1.blend(orig2, orig3, weight1, weight2, weight3), Vec3.vz(), Vec3.vy());
        ((RotationKeyframe) rot1.blend(rot2, rot3, weight1, weight2, weight3)).applyToCoordinates(c, 1.0, null, null, false, true, true, true);
        newCoords.add(c);

        // Blend the object keyframes.

        newKey.add(key.get(i).blend(k2.key.get(i), k3.key.get(i), weight1, weight2, weight3));
      }
      return new CompoundImplicitKeyframe(newKey, newCoords);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      CompoundImplicitKeyframe k2 = (CompoundImplicitKeyframe) o2, k3 = (CompoundImplicitKeyframe) o3, k4 = (CompoundImplicitKeyframe) o4;
      ArrayList<Keyframe> newKey = new ArrayList<Keyframe>();
      ArrayList<CoordinateSystem> newCoords = new ArrayList<CoordinateSystem>();
      for (int i = 0; i < key.size(); i++)
      {
        // Blend the new coordinate systems.

        CoordinateSystem coords1 = coords.get(i);
        CoordinateSystem coords2 = k2.coords.get(i);
        CoordinateSystem coords3 = k3.coords.get(i);
        CoordinateSystem coords4 = k4.coords.get(i);
        RotationKeyframe rot1 = new RotationKeyframe(coords1);
        RotationKeyframe rot2 = new RotationKeyframe(coords2);
        RotationKeyframe rot3 = new RotationKeyframe(coords3);
        RotationKeyframe rot4 = new RotationKeyframe(coords4);
        rot1.setUseQuaternion(true);
        rot2.setUseQuaternion(true);
        rot3.setUseQuaternion(true);
        rot4.setUseQuaternion(true);
        VectorKeyframe orig1 = new VectorKeyframe(coords1.getOrigin());
        VectorKeyframe orig2 = new VectorKeyframe(coords2.getOrigin());
        VectorKeyframe orig3 = new VectorKeyframe(coords3.getOrigin());
        VectorKeyframe orig4 = new VectorKeyframe(coords4.getOrigin());
        CoordinateSystem c = new CoordinateSystem((Vec3) orig1.blend(orig2, orig3, orig4, weight1, weight2, weight3, weight4), Vec3.vz(), Vec3.vy());
        ((RotationKeyframe) rot1.blend(rot2, rot3, rot4, weight1, weight2, weight3, weight4)).applyToCoordinates(c, 1.0, null, null, false, true, true, true);
        newCoords.add(c);

        // Blend the object keyframes.

        newKey.add(key.get(i).blend(k2.key.get(i), k3.key.get(i), k4.key.get(i), weight1, weight2, weight3, weight4));
      }
      return new CompoundImplicitKeyframe(newKey, newCoords);
    }

    /** Determine whether this keyframe is identical to another one. */

    public boolean equals(Keyframe k)
    {
      if (!(k instanceof CompoundImplicitKeyframe))
        return false;
      CompoundImplicitKeyframe other = (CompoundImplicitKeyframe) k;
      for (int i = 0; i < key.size(); i++)
      {
        Keyframe key1 = key.get(i);
        Keyframe key2 = other.key.get(i);
        if (key1 != key2 && (key1 == null || key2 == null || !key1.equals(key2)))
          return false;
        CoordinateSystem coords1 = coords.get(i);
        CoordinateSystem coords2 = other.coords.get(i);
        if (!coords1.getOrigin().equals(coords2.getOrigin()))
          return false;
        if (!coords1.getZDirection().equals(coords2.getZDirection()))
          return false;
        if (!coords1.getUpDirection().equals(coords2.getUpDirection()))
          return false;
      }
      return true;
    }

    /** Write out a representation of this keyframe to a stream. */

    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeShort(0);
      out.writeInt(key.size());
      for (int i = 0; i < key.size(); i++)
      {
        out.writeUTF(key.get(i).getClass().getName());
        key.get(i).writeToStream(out);
        coords.get(i).writeToFile(out);
      }
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public CompoundImplicitKeyframe(DataInputStream in, Object parent) throws IOException
    {
      short version = in.readShort();
      if (version != 0)
        throw new InvalidObjectException("");
      CompoundImplicitObject obj = (CompoundImplicitObject) ((ObjectInfo) parent).object;
      if (in.readInt() != obj.getNumObjects())
        throw new InvalidObjectException("Keyframe contains the wrong number of component objects");
      key = new ArrayList<Keyframe>();
      coords = new ArrayList<CoordinateSystem>();
      try
      {
        for (int i = 0; i < obj.getNumObjects(); i++)
        {
          Class cl = ArtOfIllusion.getClass(in.readUTF());
          Constructor con = cl.getConstructor(DataInputStream.class, Object.class);
          key.add((Keyframe) con.newInstance(in, obj.getObject(i)));
          coords.add(new CoordinateSystem(in));
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        throw new InvalidObjectException(ex.getMessage());
      }
    }
  }
}

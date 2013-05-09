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

  @Override
  public WireframeMesh getWireframeMesh()
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Keyframe getPoseKeyframe()
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void applyPoseKeyframe(Keyframe k)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}

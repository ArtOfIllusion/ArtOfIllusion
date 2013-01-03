/* Copyright (C) 2002-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.distortion.*;
import artofillusion.math.*;
import artofillusion.texture.*;

import java.io.*;
import java.util.*;

/** This abstract class represents an Object3D which is actually composed of
    other objects.  Typically, these objects are procedurally generated,
    such as by a script. */

public abstract class ObjectCollection extends Object3D
{
  protected Vector<ObjectInfo> cachedObjects;
  protected BoundingBox cachedBounds;
  protected double lastTime;
  protected CoordinateSystem lastCoords;
  protected Scene lastScene;
  protected ObjectInfo lastInfo;
  protected boolean usesTime, usesCoords;
  protected Distortion previousDistortion;
  
  public ObjectCollection()
  {
    super();
  }

  public ObjectCollection(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);
  }
  
  /** Get an enumeration of ObjectInfos listing the objects which this object
      is composed of.  This calls the protected method enumerateObjects()
      (which must be provided by subclasses), while applying Distortions and
      caching objects for interactive previews. */
  
  public synchronized Enumeration<ObjectInfo> getObjects(ObjectInfo info, boolean interactive, Scene scene)
  {
    if (interactive && cachedObjects != null && info.getDistortion() == previousDistortion)
      return cachedObjects.elements();
    Vector<ObjectInfo> objectVec = new Vector<ObjectInfo>();
    Enumeration<ObjectInfo> objects = enumerateObjects(info, interactive, scene);
    while (objects.hasMoreElements())
    {
      ObjectInfo childInfo = objects.nextElement();
      applyDistortion(info, childInfo);
      objectVec.add(childInfo);
    }
    if (!interactive)
      return objectVec.elements();
    cachedObjects = objectVec;
    previousDistortion = info.getDistortion();
    return cachedObjects.elements();
  }

  /** Get an enumeration of ObjectInfos listing the objects which this object
      is composed of. */
  
  protected abstract Enumeration<ObjectInfo> enumerateObjects(ObjectInfo info, boolean interactive, Scene scene);

  /** If this object has a distortion applied to it, then apply it to one of its component objects. */

  private void applyDistortion(ObjectInfo thisInfo, ObjectInfo childInfo)
  {
    childInfo.clearDistortion();
    if (thisInfo.isDistorted())
    {
      Distortion d = thisInfo.getDistortion().duplicate();
      Distortion first = d;
      while (first.getPreviousDistortion() != null)
        first = first.getPreviousDistortion();
      first.setPreviousDistortion(new TransformDistortion(childInfo.getCoords().fromLocal()));
      childInfo.setDistortion(d);
      childInfo.addDistortion(new TransformDistortion(childInfo.getCoords().toLocal()));
    }
  }

  /** Get a BoundingBox which just encloses the object. */

  public BoundingBox getBounds()
  {
    if (cachedBounds == null)
    {
      if (lastInfo == null || lastScene == null)
        return new BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      sceneChanged(lastInfo, lastScene);
    }
    return cachedBounds;
  }
  
  /** Since object collections are generally procedurally generated, they may
      depend explicitly on time.  If so, this should be called with the value true. */

  public void setUsesTime(boolean b)
  {
    usesTime = b;
  }
  
  /** Since object collections are generally procedurally generated, they may
      depend explicitly on position.  If so, this should be called with the value true. */

  public void setUsesCoords(boolean b)
  {
    usesCoords = b;
  }

  /** Determine whether the object is closed. */

  public boolean isClosed()
  {
    for (int i = 0; i < cachedObjects.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) cachedObjects.elementAt(i);
        if (!info.getObject().isClosed())
          return false;
      }
    return true;
  }

  /** Assume that a material can be set for the	object collection (though actually
      setting one may or may not have any effect). */

  public boolean canSetMaterial()
  {
    return true;
  }

  /** Get a mesh representing the union of all objects in the collection. */
  
  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    return convertToTriangleMesh(tol).getRenderingMesh(tol, interactive, info);
  }

  /** An object collection is never drawn directly.  Instead, its component objects
      are enumerated and drawn individually. */
  
  public WireframeMesh getWireframeMesh()
  {
    return new WireframeMesh(new Vec3 [0], new int [0], new int [0]);
  }

  /**
   * RenderObject is overridden to render each component object individually.
   */

  public void renderObject(ObjectInfo obj, ViewerCanvas canvas, Vec3 viewDir)
  {
    Camera theCamera = canvas.getCamera();
    Mat4 m = theCamera.getObjectToWorld();
    Enumeration enm = getObjects(obj, true, canvas.getScene());
    while (enm.hasMoreElements())
    {
      ObjectInfo info = (ObjectInfo) enm.nextElement();
      CoordinateSystem coords = info.getCoords().duplicate();
      coords.transformCoordinates(m);
      theCamera.setObjectTransform(coords.fromLocal());
      info.getObject().renderObject(info, canvas, info.getCoords().toLocal().timesDirection(viewDir));
    }
  }

  /** For simplicity, just assume that the object can be converted approximately. */
  
  public int canConvertToTriangleMesh()
  {
    return APPROXIMATELY;
  }
  
  /** Create a triangle mesh which is the union of all the objects in this collection. */

  public TriangleMesh convertToTriangleMesh(double tol)
  {
    ArrayList<Vec3> allVert = new ArrayList<Vec3>();
    ArrayList<Float> vertSmoothness = new ArrayList<Float>();
    ArrayList<int[]> allFace = new ArrayList<int[]>();
    class Edge {
      public int v1, v2;
      public float smoothness;
      public Edge(int v1, int v2, float smoothness)
      {
        this.v1 = v1;
        this.v2 = v2;
        this.smoothness = smoothness;
      }
    }
    ArrayList<Edge> edgeSmoothness = new ArrayList<Edge>();
    int start = 0;
    Enumeration objects = getObjects(new ObjectInfo(this, lastCoords, ""), false, lastScene);
    while (objects.hasMoreElements())
      {
        // Convert the object to a TriangleMesh.

        ObjectInfo obj = (ObjectInfo) objects.nextElement();
        if (obj.getObject().canConvertToTriangleMesh() == CANT_CONVERT)
          continue;
        Mat4 trans = obj.getCoords().fromLocal();
        TriangleMesh tri = (obj.getObject() instanceof TriangleMesh ? (TriangleMesh) obj.getObject().duplicate() : obj.getObject().convertToTriangleMesh(tol));

        // Add its vertices and faces.

        for (MeshVertex vert : tri.getVertices())
        {
          allVert.add(trans.times(vert.r));
          vertSmoothness.add(((TriangleMesh.Vertex) vert).smoothness);
        }
        for (TriangleMesh.Face face : tri.getFaces())
          allFace.add(new int [] {face.v1+start, face.v2+start, face.v3+start});

        // Record edges with non-default smoothness.

        for (TriangleMesh.Edge edge : tri.getEdges())
          if (edge.smoothness != 1.0f)
            edgeSmoothness.add(new Edge(edge.v1+start, edge.v2+start, edge.smoothness));
        start += tri.getVertices().length;
      }
    if (allVert.size() == 0)
      allVert.add(new Vec3());

    // Create the new mesh.

    Vec3 vert[] = new Vec3 [allVert.size()];
    allVert.toArray(vert);
    int face[][] = new int [allFace.size()][];
    allFace.toArray(face);
    TriangleMesh mesh = new TriangleMesh(vert, face);
    mesh.copyTextureAndMaterial(this);
    if (getTexture() == null)
    {
      Texture tex = new UniformTexture();
      mesh.setTexture(tex, tex.getDefaultMapping(mesh));
    }

    // Set vertex and edge smoothness values.

    for (int i = 0; i < vertSmoothness.size(); i++)
      mesh.getVertex(i).smoothness = vertSmoothness.get(i);
    TriangleMesh.Edge edges[] = mesh.getEdges();
    for (Edge edge : edgeSmoothness)
    {
      for (int edgeIndex : mesh.getVertex(edge.v1).getEdges())
        if (edges[edgeIndex].v1 == edge.v2 || edges[edgeIndex].v2 == edge.v2)
          edges[edgeIndex].smoothness = edge.smoothness;
    }
    return mesh;
  }
  
  /** If this object explicitly references time or position, the cached objects and
      bounding box may need to be reevaluated. */

  public void sceneChanged(ObjectInfo info, Scene scene)
  {
    if (cachedBounds == null || (usesTime && lastTime != scene.getTime()) ||
        (usesCoords && !lastCoords.equals(info.getCoords())))
      {
        lastScene = scene;
        lastTime = scene.getTime();
        lastCoords = info.getCoords().duplicate();
        lastInfo = info;
        cachedObjects = null;
        Enumeration objects = getObjects(info, true, scene);
        if (!objects.hasMoreElements())
          cachedBounds = new BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        else
          while (objects.hasMoreElements())
            {
              ObjectInfo obj = (ObjectInfo) objects.nextElement();
              BoundingBox bounds = obj.getBounds();
              bounds = bounds.transformAndOutset(obj.getCoords().fromLocal());
              if (cachedBounds == null)
                cachedBounds = bounds;
              else
                cachedBounds = cachedBounds.merge(bounds);
            }
        info.clearCachedMeshes();
      }
  }
}
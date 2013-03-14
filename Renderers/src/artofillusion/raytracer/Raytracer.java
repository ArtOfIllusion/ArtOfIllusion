/* Copyright (C) 1999-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import java.util.*;
import java.util.List;

/** Raytracer is a Renderer which generates images by raytracing. */

public class Raytracer
{
  protected RTObject sceneObject[];
  protected RTLight light[];
  protected OctreeNode rootNode, cameraNode, lightNode[];
  protected Scene theScene;
  protected Camera camera;
  protected double time, surfaceError = 0.02;
  protected boolean preview, softShadows, adaptive = true, reducedMemory;
  protected ThreadLocal<RaytracerContext> threadContext;
  private List<RTObjectFactory> factories;
  private List<RTObject> objectList;
  private List<RTLight> lightList;

  public static final double TOL = 1e-12;

  /** When a ray is traced to determine what objects it intersects, a RayIntersection object
     is used for returning the results.  To avoid creating excess objects, only one 
     RayIntersection object is created and used for all rays. */

  public static class RayIntersection
  {
    public RTObject first, second;
    public double dist;
    
    public RayIntersection()
    {
    }
  }

  public Raytracer(Camera camera)
  {
    this.camera = camera;
    factories = PluginRegistry.getPlugins(RTObjectFactory.class);
    objectList = Collections.synchronizedList(new ArrayList<RTObject>());
    lightList = Collections.synchronizedList(new ArrayList<RTLight>());
    threadContext = new ThreadLocal<RaytracerContext>() {
      protected RaytracerContext initialValue()
      {
        return new RaytracerContext(Raytracer.this);
      }
    };
  }

  public double getSurfaceError()
  {
    return surfaceError;
  }

  public void setSurfaceError(double surfaceError)
  {
    this.surfaceError = surfaceError;
  }

  public double getTime()
  {
    return time;
  }

  public void setTime(double time)
  {
    this.time = time;
  }

  public boolean isAdaptive()
  {
    return adaptive;
  }

  public void setAdaptive(boolean adaptive)
  {
    this.adaptive = adaptive;
  }

  public boolean isPreview()
  {
    return preview;
  }

  public void setPreview(boolean preview)
  {
    this.preview = preview;
  }

  public boolean isReducedMemory()
  {
    return reducedMemory;
  }

  public void setReducedMemory(boolean reducedMemory)
  {
    this.reducedMemory = reducedMemory;
  }

  public boolean isSoftShadows()
  {
    return softShadows;
  }

  public void setSoftShadows(boolean softShadows)
  {
    this.softShadows = softShadows;
  }

  /** Get the RaytracerContext for the current thread. */

  public RaytracerContext getContext()
  {
    return threadContext.get();
  }

  /** Add a single object to the scene. */

  public void addObject(ObjectInfo info)
  {
    boolean displaced = false;
    double tol;

    // First give plugins a chance to handle the object.

    for (RTObjectFactory factory : factories)
      if (factory.processObject(info, theScene, camera, objectList, lightList))
        return;

    // Handle it in the default way.

    Object3D theObject = info.getObject();
    Mat4 toLocal = info.getCoords().toLocal();
    Mat4 fromLocal = info.getCoords().fromLocal();
    if (theObject instanceof PointLight)
    {
      lightList.add(new RTSphericalLight((PointLight) theObject, info.getCoords(), softShadows));
      return;
    }
    if (theObject instanceof SpotLight)
    {
      lightList.add(new RTSphericalLight((SpotLight) theObject, info.getCoords(), softShadows));
      return;
    }
    if (theObject instanceof DirectionalLight)
    {
      lightList.add(new RTDirectionalLight((DirectionalLight) theObject, info.getCoords(), softShadows));
      return;
    }
    while (theObject instanceof ObjectWrapper)
      theObject = ((ObjectWrapper) theObject).getWrappedObject();
    if (theObject instanceof ObjectCollection)
    {
      Enumeration enm = ((ObjectCollection) theObject).getObjects(info, false, theScene);
      while (enm.hasMoreElements())
      {
        ObjectInfo elem = (ObjectInfo) enm.nextElement();
        if (!elem.isVisible())
          continue;
        ObjectInfo copy = elem.duplicate();
        copy.getCoords().transformCoordinates(fromLocal);
        addObject(copy);
      }
      return;
    }
    Vec3 cameraOrig = camera.getCameraCoordinates().getOrigin();
    double distToScreen = camera.getDistToScreen();
    if (adaptive)
    {
      double dist = info.getBounds().distanceToPoint(toLocal.times(cameraOrig));
      if (dist < distToScreen)
        tol = surfaceError;
      else
        tol = surfaceError*dist/distToScreen;
    }
    else
      tol = surfaceError;
    Texture tex = theObject.getTexture();
    if (tex != null && tex.hasComponent(Texture.DISPLACEMENT_COMPONENT))
    {
      displaced = true;
      if (theObject.canConvertToTriangleMesh() != Object3D.CANT_CONVERT)
      {
        TriangleMesh tm = theObject.convertToTriangleMesh(tol);
        tm.setTexture(tex, theObject.getTextureMapping().duplicate());
        if (theObject.getMaterialMapping() != null)
          tm.setMaterial(theObject.getMaterial(), theObject.getMaterialMapping().duplicate());
        theObject = tm;
      }
    }
    if (!info.isDistorted())
    {
      if (theObject instanceof Sphere)
      {
        Vec3 rad = ((Sphere) theObject).getRadii();
        if (rad.x == rad.y && rad.x == rad.z)
        {
          objectList.add(new RTSphere((Sphere) theObject, fromLocal, toLocal, info.getObject().getAverageParameterValues()));
          return;
        }
        else
        {
          objectList.add(new RTEllipsoid((Sphere) theObject, fromLocal, toLocal, info.getObject().getAverageParameterValues()));
          return;
        }
      }
      else if (theObject instanceof Cylinder)
      {
        objectList.add(new RTCylinder((Cylinder) theObject, fromLocal, toLocal, info.getObject().getAverageParameterValues()));
        return;
      }
      else if (theObject instanceof Cube)
      {
        objectList.add(new RTCube((Cube) theObject, fromLocal, toLocal, info.getObject().getAverageParameterValues()));
        return;
      }
      else if (theObject instanceof ImplicitObject && ((ImplicitObject) theObject).getPreferDirectRendering())
      {
        objectList.add(new RTImplicitObject((ImplicitObject) theObject, fromLocal, toLocal, info.getObject().getAverageParameterValues(), tol));
        return;
      }
    }
    RenderingMesh mesh;
    if (preview)
    {
      mesh = info.getPreviewMesh();
      if (mesh != null)
        mesh = mesh.clone();
    }
    else
      mesh = info.getRenderingMesh(tol);
    if (mesh == null)
      return;
    mesh.transformMesh(fromLocal);
    Vec3 vert[] = mesh.vert;
    RenderingTriangle t[] = mesh.triangle;
    if (displaced)
    {
      Vec3 cameraZDir = camera.getCameraCoordinates().getZDirection();
      double vertTol[] = new double [vert.length];
      if (adaptive)
        for (int i = 0; i < vert.length; i++)
        {
          Vec3 offset = vert[i].minus(cameraOrig);
          double vertDist = offset.length();
          if (offset.dot(cameraZDir) < 0.0)
            vertDist = -vertDist;
          vertTol[i] = (vertDist < distToScreen ? surfaceError : surfaceError*vertDist/distToScreen);
        }
      for (int i = 0; i < t.length; i++)
      {
        RenderingTriangle tri = mesh.triangle[i];
        if (mesh.faceNorm[i].length() < TOL)
          continue;
        if (vert[tri.v1].distance(vert[tri.v2]) < TOL)
          continue;
        if (vert[tri.v1].distance(vert[tri.v3]) < TOL)
          continue;
        if (vert[tri.v2].distance(vert[tri.v3]) < TOL)
          continue;
        double localTol;
        if (adaptive)
        {
          localTol = vertTol[tri.v1];
          if (vertTol[tri.v2] < localTol)
            localTol = vertTol[tri.v2];
          if (vertTol[tri.v3] < localTol)
            localTol = vertTol[tri.v3];
        }
        else
          localTol = tol;
        RTDisplacedTriangle dispTri = new RTDisplacedTriangle(mesh, i, fromLocal, toLocal, localTol, time);
        RTObject dt = dispTri;
        if (!dispTri.isReallyDisplaced())
        {
          if (reducedMemory)
            dt = new RTTriangleLowMemory(mesh, i, fromLocal, toLocal);
          else
            dt = new RTTriangle(mesh, i, fromLocal, toLocal);
        }
        objectList.add(dt);
        if (adaptive && dt instanceof RTDisplacedTriangle)
        {
          double dist = dt.getBounds().distanceToPoint(cameraOrig);
          if (dist < distToScreen)
            ((RTDisplacedTriangle) dt).setTolerance(surfaceError);
          else
            ((RTDisplacedTriangle) dt).setTolerance(surfaceError*dist/distToScreen);
        }
      }
    }
    else
      for (int i = 0; i < t.length; i++)
      {
        RenderingTriangle tri = mesh.triangle[i];
        if (mesh.faceNorm[i].length() < TOL)
          continue;
        if (vert[tri.v1].distance(vert[tri.v2]) < TOL)
          continue;
        if (vert[tri.v1].distance(vert[tri.v3]) < TOL)
          continue;
        if (vert[tri.v2].distance(vert[tri.v3]) < TOL)
          continue;
        if (reducedMemory)
          objectList.add(new RTTriangleLowMemory(mesh, i, fromLocal, toLocal));
        else
          objectList.add(new RTTriangle(mesh, i, fromLocal, toLocal));
      }
  }
  
  /** Build the octree. */
  
  public void finishConstruction()
  {
    sceneObject = objectList.toArray(new RTObject [objectList.size()]);
    for (int i = 0; i < sceneObject.length; i++)
      sceneObject[i].index = i;
    light = lightList.toArray(new RTLight [lightList.size()]);
    objectList = null;
    lightList = null;
    BoundingBox objBounds[] = new BoundingBox [sceneObject.length];
    double minx, maxx, miny, maxy, minz, maxz;
    int i;
    
    // Find the bounding boxes for each object, and for the entire scene.

    minx = miny = minz = Double.MAX_VALUE;
    maxx = maxy = maxz = -Double.MAX_VALUE;
    for (i = 0; i < sceneObject.length; i++)
      {
        objBounds[i] = sceneObject[i].getBounds();
        if (objBounds[i].minx < minx)
          minx = objBounds[i].minx;
        if (objBounds[i].maxx > maxx)
          maxx = objBounds[i].maxx;
        if (objBounds[i].miny < miny)
          miny = objBounds[i].miny;
        if (objBounds[i].maxy > maxy)
          maxy = objBounds[i].maxy;
        if (objBounds[i].minz < minz)
          minz = objBounds[i].minz;
        if (objBounds[i].maxz > maxz)
          maxz = objBounds[i].maxz;
      }
    minx -= TOL;
    miny -= TOL;
    minz -= TOL;
    maxx += TOL;
    maxy += TOL;
    maxz += TOL;
    
    // Create the octree.

    rootNode = new OctreeNode(minx, maxx, miny, maxy, minz, maxz, sceneObject, objBounds, null);

    // Find the nodes which contain the camera and the lights.

    cameraNode = rootNode.findNode(camera.getCameraCoordinates().getOrigin());
    lightNode = new OctreeNode [light.length];
    for (i = 0; i < light.length; i++)
    {
      if (light[i].getLight() instanceof DirectionalLight)
        lightNode[i] = null;
      else
        lightNode[i] = rootNode.findNode(light[i].getCoords().getOrigin());
    }
  }
  
  /**
   * Set fields to null.  This may be called when you are done with the Raytracer and will not use it any more.
   * This is not required, but may help the garbage collector to work more efficiently.
   */

  public void cleanup()
  {
    sceneObject = null;
    light = null;
    rootNode = null;
    cameraNode = null;
    lightNode = null;
    theScene = null;
    camera = null;
  }

  /** Trace a ray, and determine the first object it intersects.  If it is immediately followed
     by a second object, both are returned.  To avoid creating excess objects, the results
     are returned in the global RayIntersection object.  node is the first octree node which
     the ray intersects.  If an intersection is found, the octree node containing the
     intersection point is returned.  Otherwise, the return value is null. */
  
  protected OctreeNode traceRay(Ray r, OctreeNode node)
  {
    RTObject first = null, second = null, obj[];
    double dist, firstDist = Double.MAX_VALUE, secondDist = Double.MAX_VALUE;
    Vec3 intersectionPoint = r.rt.tempVec;
    int i;
    
    while (first == null)
    {
      obj = node.getObjects();
      for (i = obj.length-1; i >= 0; i--)
      {
        SurfaceIntersection intersection = r.findIntersection(obj[i]);
        if (intersection != SurfaceIntersection.NO_INTERSECTION)
        {
          intersection.intersectionPoint(0, intersectionPoint);
          if (node.contains(intersectionPoint))
          {
            dist = intersection.intersectionDist(0);
            if (dist < firstDist)
            {
              secondDist = firstDist;
              second = first;
              firstDist = dist;
              first = obj[i];
            }
            else if (dist < secondDist)
            {
              secondDist = dist;
              second = obj[i];
            }
          }
        }
      }
      if (first == null)
      {
        node = node.findNextNode(r);
        if (node == null)
          return null;
      }
    }
    RayIntersection intersect = r.rt.intersect;
    intersect.first = first;
    intersect.dist = firstDist;
    if (secondDist-firstDist < TOL)
      intersect.second = second;
    else
      intersect.second = null;
    return node;
  }
}
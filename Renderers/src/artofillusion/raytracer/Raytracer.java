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

/**
 * Raytracer implements a raytracing engine.  It compares rays to objects in a scene to identify what they hit.
 * <p/>
 * To use it, first you must define the scene.  Do this by calling {@link #addObject(ObjectInfo)} for each object
 * you want included in the scene.  When all objects have been added, call {@link #finishConstruction()} to analyze
 * the scene and build data structures for use during raytracing.  There are several properties you can set that
 * affect how the objects are interpreted.  For example, {@link #setSurfaceError(double) setSurfaceError()} determines
 * how finely surfaces should be triangulated, and {@link #setAdaptive(boolean) setAdaptive()} determines whether
 * the surface accuracy should vary based on the distance of the object from a camera.
 * <p/>
 * While building the scene, it also records a list of {@link RTLight} objects representing light sources in the scene.
 * You can query the list by calling {@link #getLights()}, but otherwise it is not used.
 * <p/>
 * To trace a ray and see what it hits, call {@link #traceRay(Vec3, Vec3)}.  There is also a second version of the
 * method that is more efficient, but requires more preparation by the caller before it is invoked.
 * <p/>
 * When you are all finished with the Raytracer, it is a good idea to call {@link #cleanup()} to set internal pointers
 * to null.  This is not required, but may help the garbage collector to work more efficiently.
 */

public class Raytracer
{
  private RTObject sceneObject[];
  private RTLight light[];
  private OctreeNode rootNode, cameraNode, lightNode[];
  private Scene scene;
  private Camera camera;
  private double time, surfaceError = 0.02;
  private boolean preview, softShadows, adaptive = true, reducedMemory;
  private ThreadLocal<RaytracerContext> threadContext;
  private List<RTObjectFactory> factories;
  private List<RTObject> objectList;
  private List<RTLight> lightList;

  public static final double TOL = 1e-12;

  /**
   * When a ray is traced to determine what objects it intersects, a RayIntersection object
   * is used for returning the results.  Typically it reports only the first object that was
   * hit, but when when two objects are almost exactly the same distance away, it reports both
   * of them to ensure that neither is missed.
   */

  public static class RayIntersection
  {
    private SurfaceIntersection first, second;
    private double distance;
    
    public RayIntersection()
    {
    }

    /**
     * Get the details of the first intersection.  If no object was hit, this will equal {@link SurfaceIntersection#NO_INTERSECTION}.
     */
    public SurfaceIntersection getFirst()
    {
      return first;
    }

    /**
     * Get the details of the second intersection.  In most cases, this will equal {@link SurfaceIntersection#NO_INTERSECTION}.
     */
    public SurfaceIntersection getSecond()
    {
      return second;
    }

    /**
     * Get the distance to the intersection point.
     */
    public double getDistance()
    {
      return distance;
    }
  }

  /**
   * Create a Raytracer object.
   *
   * @param scene     the Scene which the objects that will be added with {@link #addObject(ObjectInfo)} belong to
   * @param camera    if {@link #isAdaptive()} is true (the default), this Camera is used as the viewpoint for determining
   *                  surface accuracy.  Some objects may also vary their properties based on how they are positioned relative
   *                  to the camera.
   */
  public Raytracer(Scene scene, Camera camera)
  {
    this.scene = scene;
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

  /**
   * Get the error tolerance to use when triangulating objects.  This is the maximum distance any point on the triangulated
   * surface may be from the true surface.  The default value is 0.02.
   */
  public double getSurfaceError()
  {
    return surfaceError;
  }

  /**
   * Set the error tolerance to use when triangulating objects.  This is the maximum distance any point on the triangulated
   * surface may be from the true surface.  Calling this method affects all future calls to {@link #addObject(ObjectInfo) addObject()},
   * but does not affect objects that have already been added.
   */
  public void setSurfaceError(double surfaceError)
  {
    this.surfaceError = surfaceError;
  }

  /**
   * Get whether a decreased surface accuracy should be used for objects that are distant from the camera.  The default value is true.
   */
  public boolean isAdaptive()
  {
    return adaptive;
  }

  /**
   * Set whether a decreased surface accuracy should be used for objects that are distant from the camera.  Calling this method
   * affects all future calls to {@link #addObject(ObjectInfo) addObject()}, but does not affect objects that have already been added.
   */
  public void setAdaptive(boolean adaptive)
  {
    this.adaptive = adaptive;
  }

  /**
   * Get the current time.  This affects objects whose properties vary with time.  The default value is 0.
   */
  public double getTime()
  {
    return time;
  }

  /**
   * Get the current time.  This affects objects whose properties vary with time.  Calling this method affects all future calls
   * to {@link #addObject(ObjectInfo) addObject()}, but does not affect objects that have already been added.
   */
  public void setTime(double time)
  {
    this.time = time;
  }

  /**
   * Get whether the raytracer should use the existing preview meshes for objects, instead of constructing new meshes based
   * on the requested surface accuracy.  The default value is false.
   */
  public boolean getUsePreviewMeshes()
  {
    return preview;
  }

  /**
   * Set whether the raytracer should use the existing preview meshes for objects, instead of constructing new meshes based
   * on the requested surface accuracy.  Calling this method affects all future calls to {@link #addObject(ObjectInfo) addObject()},
   * but does not affect objects that have already been added.
   */
  public void setUsePreviewMeshes(boolean preview)
  {
    this.preview = preview;
  }

  /**
   * Get whether the raytracer should use an alternate representation of triangles that uses less memory but requires more computation
   * to identify ray intersections.  The default value is false.
   */
  public boolean getUseReducedMemory()
  {
    return reducedMemory;
  }

  /**
   * Set whether the raytracer should use an alternate representation of triangles that uses less memory but requires more computation
   * to identify ray intersections.  Calling this method affects all future calls to {@link #addObject(ObjectInfo) addObject()},
   * but does not affect objects that have already been added.
   */
  public void setUseReducedMemory(boolean reducedMemory)
  {
    this.reducedMemory = reducedMemory;
  }

  /**
   * Get whether RTLight objects should be configured to generate soft shadows.  The default value is false.
   */
  public boolean getUseSoftShadows()
  {
    return softShadows;
  }

  /**
   * Set whether RTLight objects should be configured to generate soft shadows.  Calling this method affects all future
   * calls to {@link #addObject(ObjectInfo) addObject()}, but does not affect objects that have already been added.
   */
  public void setUseSoftShadows(boolean softShadows)
  {
    this.softShadows = softShadows;
  }

  /**
   * Get a list of all objects in the scene, as represented by RTObject objects.
   */
  public RTObject[] getObjects()
  {
    return sceneObject;
  }

  /**
   * Get a list of all light sources in the scene, as represented by RTLight objects.
   */
  public RTLight[] getLights()
  {
    return light;
  }

  /** Get the RaytracerContext for the current thread. */

  public RaytracerContext getContext()
  {
    return threadContext.get();
  }

  /**
   * Get the root node of the octree.
   */
  public OctreeNode getRootNode()
  {
    return rootNode;
  }

  /**
   * Get the octree node containing the camera.
   */
  public OctreeNode getCameraNode()
  {
    return cameraNode;
  }

  /**
   * Get a list of the octree nodes containing each light source.
   */
  public OctreeNode[] getLightNodes()
  {
    return lightNode;
  }

  /** Add a single object to the scene. */

  public void addObject(ObjectInfo info)
  {
    if (sceneObject != null)
      throw new IllegalStateException("finishConstruction() has already been called");
    if (objectList == null)
      throw new IllegalStateException("cleanup() has already been called");

    // First give plugins a chance to handle the object.

    for (RTObjectFactory factory : factories)
      if (factory.processObject(info, scene, camera, objectList, lightList))
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
      Enumeration enm = ((ObjectCollection) theObject).getObjects(info, false, scene);
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
    double tol;
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
    boolean displaced = false;
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

  /**
   * This must be called after all objects have been added to the scene and before any calls to {@link #traceRay(Vec3, Vec3) traceRay()}.
   */
  public void finishConstruction()
  {
    if (sceneObject != null)
      throw new IllegalStateException("finishConstruction() has already been called");
    if (objectList == null)
      throw new IllegalStateException("cleanup() has already been called");
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
    objectList = null;
    lightList = null;
    sceneObject = null;
    light = null;
    rootNode = null;
    cameraNode = null;
    lightNode = null;
    scene = null;
    camera = null;
    factories = null;
  }

  /**
   * Trace a ray and determine the first object it hits (or the first two objects, if they are almost exactly the same
   * distance away).
   *
   * @param origin     the origin of the ray
   * @param direction  a unit vector pointing in the ray direction
   * @return a RayIntersection describing what the ray hit
   */
  public RayIntersection traceRay(Vec3 origin, Vec3 direction)
  {
    if (sceneObject == null)
    {
      if (objectList == null)
        throw new IllegalStateException("cleanup() has already been called");
      throw new IllegalStateException("finishConstruction() has not been called");
    }
    RaytracerContext context = getContext();
    Ray r = new Ray(context);
    r.origin.set(origin);
    r.direction.set(direction);
    RayIntersection intersect = new RayIntersection();
    OctreeNode firstNode = rootNode.findFirstNode(r);
    if (firstNode == null)
      intersect.first = intersect.second = SurfaceIntersection.NO_INTERSECTION;
    else
      traceRay(r, firstNode, intersect);
    return intersect;
  }

  /**
   * Trace a ray and determine the first object it hits (or the first two objects, if they are almost exactly the same
   * distance away).  This version of traceRay() is more efficient, but requires more setup work by the caller.
   *
   * @param r         the ray to check for intersections with
   * @param node      the octree node containing the ray origin
   * @param intersect the details of what was hit are returned in this object
   * @return the octree node containing the intersection point, or null if nothing was hit
   */
  public OctreeNode traceRay(Ray r, OctreeNode node, RayIntersection intersect)
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
        {
          intersect.first = SurfaceIntersection.NO_INTERSECTION;
          return null;
        }
      }
    }
    intersect.first = r.rt.lastRayResult[first.index];
    intersect.distance = firstDist;
    if (secondDist-firstDist < TOL)
      intersect.second = r.rt.lastRayResult[second.index];
    else
      intersect.second = SurfaceIntersection.NO_INTERSECTION;
    return node;
  }
}
/* Copyright (C) 2003-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.*;
import artofillusion.util.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;

/** This is a PhotonSource corresponding to an RTEllipsoid or RTSphere. */

public class EllipsoidPhotonSource implements PhotonSource
{
  private double rx, ry, rz, param[];
  private Mat4 fromLocal;
  private TextureMapping texMap;
  private RGBColor color;
  private float lightIntensity;

  /** Create an EllipsoidPhotonSource from a RTEllipsoid.
      @param obj    the object for which to create a photon source
      @param map    the photon map for which this will generate photons
  */
  
  public EllipsoidPhotonSource(RTEllipsoid obj, PhotonMap map)
  {
    rx = obj.rx;
    ry = obj.ry;
    rz = obj.rz;
    fromLocal = obj.fromLocal;
    if (fromLocal == null)
      fromLocal = Mat4.translation(obj.cx, obj.cy, obj.cz);
    param = obj.param;
    texMap = obj.getTextureMapping();
    color = new RGBColor();
    
    // Calculating the surface area of a general ellipsoid is an incredibly difficult problem involving
    // elliptic integrals.  We can estimate it by triangulating the ellipsoid and adding up the areas
    // of the triangles.
    
    ObjectInfo info = new ObjectInfo(obj.theSphere, new CoordinateSystem(), "");
    RenderingMesh mesh = info.getPreviewMesh();
    double area = 0.0;
    for (int i = 0; i < mesh.triangle.length; i++)
      {
        RenderingTriangle tri = mesh.triangle[i];
        Vec3 v1 = mesh.vert[tri.v1], v2 = mesh.vert[tri.v2], v3 = mesh.vert[tri.v3];
        Vec3 e1 = v2.minus(v1);
        Vec3 e2 = v3.minus(v1);
        area += 0.5*e1.cross(e2).length();
      }
    if (texMap.appliesTo() == TextureMapping.FRONT_AND_BACK)
      area *= 2.0;
    
    // Since this method will always underestimate the surface area, apply a correction factor
    // based on how finely it was subdivided.
    
    if (mesh.triangle.length == 8)
      area *= 1.8;
    else if (mesh.triangle.length == 32)
      area *= 1.2;
    else if (mesh.triangle.length == 128)
      area *= 1.05;
    else if (mesh.triangle.length == 512)
      area *= 1.01;

    // Find the average emissive intensity.
    
    TextureSpec spec = map.getWorkspace().surfSpec[0];
    texMap.getTexture().getAverageSpec(spec, map.getRaytracer().getTime(), obj.param);
    color.copy(spec.emissive);
    lightIntensity = 0.5f*(color.getRed()+color.getGreen()+color.getBlue())*(float) area;
  }

  /** Create an EllipsoidPhotonSource from a RTSphere.
      @param obj    the object for which to create a photon source
      @param map    the photon map for which this will generate photons
  */
  
  public EllipsoidPhotonSource(RTSphere obj, PhotonMap map)
  {
    rx = ry = rz = obj.r;
    fromLocal = obj.fromLocal;
    if (fromLocal == null)
      fromLocal = Mat4.translation(obj.cx, obj.cy, obj.cz);
    param = obj.param;
    texMap = obj.getTextureMapping();
    color = new RGBColor();
    double area = 4.0*Math.PI*rx*rx;
    if (texMap.appliesTo() == TextureMapping.FRONT_AND_BACK)
      area *= 2.0;

    // Find the average emissive intensity.
    
    TextureSpec spec = map.getWorkspace().surfSpec[0];
    texMap.getTexture().getAverageSpec(spec, map.getRaytracer().getTime(), obj.param);
    color.copy(spec.emissive);
    lightIntensity = 0.5f*(color.getRed()+color.getGreen()+color.getBlue())*(float) area;
  }

  /** Get the total intensity of light which this object sends into the scene. */

  public double getTotalIntensity()
  {
    return lightIntensity;
  }
  
  /**
   * Generate photons and add them to a map.
   * @param map          the PhotonMap to add the Photons to
   * @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
   * @param threads
   */
  
  public void generatePhotons(PhotonMap map, double intensity, ThreadManager threads)
  {
    Ray r = new Ray(map.getWorkspace().context);
    Vec3 orig = r.getOrigin();
    Vec3 norm = new Vec3();
    double nx = 1.0/rx, ny = 1.0/ry, nz = 1.0/rz;
    double emittedIntensity = 0.0;

    while (emittedIntensity < intensity)
      {
        // Select an origin and direction.
        
        double ctheta = (map.random.nextDouble()-0.5)*2.0;
        double stheta = Math.sqrt(1.0-ctheta*ctheta);
        double phi = map.random.nextDouble()*2.0*Math.PI;
        double sphi = Math.sin(phi), cphi = Math.cos(phi);
        double x = stheta*sphi, y = stheta*cphi, z = ctheta;
        norm.set(nx*x, ny*y, nz*z);
        norm.normalize();
        orig.set(rx*x, ry*y, rz*z);
        emittedIntensity += generateOnePhoton(map, r, norm);
      }
  }
  
  /** Generate a Photon from a point on the sphere.
      @param map       the PhotonMap to add the Photon to
      @param r         a ray whose origin is the point from which to generate the photon (in local coordinates)
      @param norm      the surface normal at the point (in local coordinates)
      @return the intensity of the emitted ray
  */
  
  private float generateOnePhoton(PhotonMap map, Ray r, Vec3 norm)
  {
    RaytracerRenderer rt = map.getRenderer();
    TextureSpec spec = map.getWorkspace().surfSpec[0];
    Vec3 dir = r.getDirection();
    float intensity = 1.0f;
    double dot, absdot;

    do
      {
        dir.set(0.0, 0.0, 0.0);
        map.randomizePoint(dir, 1.0);
        dir.normalize();
        dot = norm.dot(dir);
        absdot = (dot > 0.0 ? dot : -dot);
      } while (absdot < map.random.nextDouble());
    if (!texMap.appliesToFace(dot > 0.0))
      {
        dot = -dot;
        dir.scale(-1.0);
      }

    // Determine the photon color.
    
    texMap.getTextureSpec(dir, spec, dot, rt.smoothScale, rt.time, param);
    color.copy(spec.emissive);
    intensity = color.getRed()+color.getGreen()+color.getBlue();
    if (intensity < 1.0)
      {
        // Use Russian Roulette sampling.
      
        if (intensity < map.random.nextFloat())
          return intensity;
        color.scale(1.0f/intensity);
      }
    
    // Send out the photon.
  
    fromLocal.transform(r.getOrigin());
    fromLocal.transformDirection(dir);
    r.newID();
    map.spawnPhoton(r, color, true);
    return intensity;
  }
}
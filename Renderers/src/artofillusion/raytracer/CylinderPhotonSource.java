/* Copyright (C) 2003-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.util.*;

/** This is a PhotonSource corresponding to an RTCylinder. */

public class CylinderPhotonSource implements PhotonSource
{
  private RTCylinder cylinder;
  private double rx, rz, height, ratio, param[];
  private double bottomArea, topArea, sideArea;
  private Mat4 fromLocal;
  private TextureMapping texMap;
  private RGBColor color;
  private float lightIntensity;

  /** Create an CylinderPhotonSource from a RTCylinder.
      @param obj       the object for which to create a photon source
      @param map    the photon map for which this will generate photons
  */
  
  public CylinderPhotonSource(RTCylinder obj, PhotonMap map)
  {
    cylinder = obj;
    rx = obj.rx;
    rz = obj.rz;
    height = obj.height;
    ratio = Math.sqrt(obj.toprx2/obj.rx2);
    fromLocal = obj.fromLocal;
    if (fromLocal == null && ratio > 1.0)
      {
        rx *= ratio;
        rz *= ratio;
        ratio = 1.0/ratio;
        fromLocal = Mat4.translation(cylinder.cx, cylinder.cy, cylinder.cz).times(Mat4.xrotation(Math.PI));
      }
    param = obj.param;
    texMap = obj.getTextureMapping();
    color = new RGBColor();
    bottomArea = Math.PI*rx*rz;
    topArea = bottomArea*ratio*ratio;
    sideArea = 2.0*Math.PI*Math.sqrt(0.5*(rx*rx+rz*rz))*height*(0.5+0.5*ratio);

    // Find the average emissive intensity.
    
    TextureSpec spec = map.getWorkspace().surfSpec[0];
    texMap.getTexture().getAverageSpec(spec, map.getRaytracer().getTime(), obj.param);
    color.copy(spec.emissive);
    lightIntensity = 0.5f*(color.getRed()+color.getGreen()+color.getBlue())*(float) (bottomArea+topArea+sideArea);
    if (texMap.appliesTo() == TextureMapping.FRONT_AND_BACK)
      lightIntensity *= 2.0f;
  }

  /** Get the total intensity of light which this object sends into the scene. */

  public double getTotalIntensity()
  {
    return lightIntensity;
  }
  
  /**
   * Generate photons and add them to a map.
   *
   * @param map          the PhotonMap to add the Photons to
   * @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
   * @param threads
   */
  
  public void generatePhotons(PhotonMap map, double intensity, ThreadManager threads)
  {
    RenderWorkspace workspace = map.getWorkspace();
    Ray r = new Ray(workspace.context);
    Vec3 orig = r.getOrigin();
    Vec3 norm = new Vec3();
    double prob1 = bottomArea/(bottomArea+topArea+sideArea);
    double prob2 = (bottomArea+topArea)/(bottomArea+topArea+sideArea);
    double halfHeight = 0.5*height;
    double sz = (rx*rx)/(rz*rz);
    double sy = rx*(ratio-1.0)/height;
    double emittedIntensity = 0.0;
    while (emittedIntensity < intensity)
      {
        double p = map.random.nextDouble();
        if (p < prob1)
          {
            // Generate the photon from the bottom.
            
            double x, z;
            do
              {
                x = map.random.nextDouble()-0.5;
                z = map.random.nextDouble()-0.5;
              } while (x*x+z*z > 0.25);
            orig.set(2.0*x*rx, -halfHeight, 2.0*z*rz);
            norm.set(0.0, -1.0, 0.0);
          }
        else if (p < prob2)
          {
            // Generate the photon from the top.
            
            double x, z;
            do
              {
                x = map.random.nextDouble()-0.5;
                z = map.random.nextDouble()-0.5;
              } while (x*x+z*z > 0.25);
            orig.set(2.0*x*rx*ratio, halfHeight, 2.0*z*rz*ratio);
            norm.set(0.0, 1.0, 0.0);
          }
        else
          {
            // Generate the photon from the side.
            
            double h, f;
            do
              {
            	h = map.random.nextDouble();
            	f = 1.0-(1.0-ratio)*h;
              } while (f < map.random.nextDouble());
            double phi = 2.0*Math.PI*map.random.nextDouble();
            double cphi = Math.cos(phi), sphi = Math.sin(phi);
            orig.set(f*rx*cphi, h*height-halfHeight, f*rz*sphi);
            norm.set(orig.x-cylinder.cx, -(rx+sy*(orig.y-cylinder.cy+halfHeight))*sy, (orig.z-cylinder.cz)*sz);
            norm.normalize();
          }
        
        // Select an origin and direction.
        
        emittedIntensity += generateOnePhoton(map, r, workspace, norm);
      }
  }
  
  /** Generate a Photon from a point on the cylinder.
      @param map       the PhotonMap to add the Photon to
      @param r         a ray whose origin is the point from which to generate the photon (in local coordinates)
      @param workspace the current workspace for this thread
      @param norm      the surface normal at the point (in local coordinates)
      @return the intensity of the emitted ray
  */
  
  private float generateOnePhoton(PhotonMap map, Ray r, RenderWorkspace workspace, Vec3 norm)
  {
    RaytracerRenderer renderer = map.getRenderer();
    TextureSpec spec = workspace.surfSpec[0];
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
    
    texMap.getTextureSpec(dir, spec, dot, renderer.smoothScale, renderer.time, param);
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
  
    if (fromLocal != null)
      {
        fromLocal.transform(r.getOrigin());
        fromLocal.transformDirection(dir);
      }
    else
      {
        Vec3 orig = r.getOrigin();
        orig.x += cylinder.cx;
        orig.y += cylinder.cy;
        orig.z += cylinder.cz;
      }
    r.newID();
    map.spawnPhoton(r, color, true);
    return intensity;
  }
}
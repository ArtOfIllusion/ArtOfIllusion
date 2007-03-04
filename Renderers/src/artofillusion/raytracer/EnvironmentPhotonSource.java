/* Copyright (C) 2003-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.texture.*;

/** This is a PhotonSource corresponding to the environment sphere. */

public class EnvironmentPhotonSource implements PhotonSource
{
  private Vec3 center;
  private RGBColor color;
  private TextureMapping envMapping;
  private int envMode;
  private float lightIntensity;
  private double radius;

  /** Create a PointPhotonSource. */
  
  public EnvironmentPhotonSource(Scene scene, BoundingBox sceneBounds)
  {
    // Find the center and radius of the scene.
    
    center = sceneBounds.getCenter();
    Vec3 corner[] = sceneBounds.getCorners();
    double maxDist2 = 0.0;
    for (int i = 0; i < corner.length; i++)
      {
        double dist2 = center.distance2(corner[i]);
        if (dist2 > maxDist2)
          maxDist2 = dist2;
      }
    radius = Math.sqrt(maxDist2);

    // Determine the intensity of the light.

    envMode = scene.getEnvironmentMode();
    color = new RGBColor();
    if (envMode == Scene.ENVIRON_SOLID)
      {
        color.copy(scene.getEnvironmentColor());
      }
    else
      {
        TextureSpec spec = new TextureSpec();
        envMapping = scene.getEnvironmentMapping();
        envMapping.getTexture().getAverageSpec(spec, scene.getTime(), null);
        if (envMode == Scene.ENVIRON_DIFFUSE)
          color.copy(spec.diffuse);
        else
          color.copy(spec.emissive);
      }
    lightIntensity = color.getRed()+color.getGreen()+color.getBlue();
    if (lightIntensity == 0.0f)
      return;
    color.scale(1.0f/lightIntensity);
    lightIntensity *= (float) (4.0*Math.PI*radius*radius);
  }

  /** Get the total intensity of light which this object sends into the scene. */

  public double getTotalIntensity()
  {
    return lightIntensity;
  }
  
  /** Generate photons and add them to a map.
      @param map          the PhotonMap to add the Photons to
      @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
  */
  
  public void generatePhotons(PhotonMap map, double intensity)
  {
    Thread currentThread = Thread.currentThread();
    Raytracer rt = map.getRaytracer();
    TextureSpec spec = map.getContext().surfSpec[0];
    Ray r = new Ray(map.getContext());
    Vec3 orig = r.getOrigin();
    Vec3 dir = r.getDirection();
    double emittedIntensity = 0.0;

    // Send out the photons.
    
    for (int i = 0; emittedIntensity < intensity; i++)
      {
        if (rt.renderThread != currentThread)
          return;

        // Select an origin and direction.
        
        double ctheta = ((i&3)-2+map.random.nextDouble())*0.5;
        double stheta = Math.sqrt(1.0-ctheta*ctheta);
        double phi = (((i>>2)&3)+map.random.nextDouble())*0.5*Math.PI;
        double sphi = Math.sin(phi);
        double cphi = Math.cos(phi);
        orig.set(stheta*sphi, stheta*cphi, ctheta);
        double dot;
        do
          {
            dir.set(0.0, 0.0, 0.0);
            map.randomizePoint(dir, 1.0);
            dir.normalize();
            dot = orig.dot(dir);
            if (dot > 0.0)
              dir.scale(-1.0);
            else
              dot = -dot;
          } while (dot < map.random.nextDouble());
        orig.scale(radius);
        orig.add(center);

        // Determine the photon color.
        
        if (envMode == Scene.ENVIRON_DIFFUSE || envMode == Scene.ENVIRON_EMISSIVE)
          {
            envMapping.getTextureSpec(dir.times(-1.0), spec, 1.0, rt.smoothScale*rt.extraGIEnvSmoothing, rt.time, null);
            if (envMode == Scene.ENVIRON_DIFFUSE)
              color.copy(spec.diffuse);
            else
              color.copy(spec.emissive);
            float sum = color.getRed()+color.getGreen()+color.getBlue();
            emittedIntensity += sum;
            if (sum < 1.0)
              {
                // Use Russian Roulette sampling.
              
                if (sum < map.random.nextFloat())
                  continue;
                color.scale(1.0f/sum);
              }
          }
        else
          emittedIntensity += 1.0;
        
        // Send out the photon.
      
        r.newID();
        map.spawnPhoton(r, color, true);
      }
  }
}
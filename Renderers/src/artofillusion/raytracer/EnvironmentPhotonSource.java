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

  /** Create an EnvironmentPhotonSource. */
  
  public EnvironmentPhotonSource(Scene scene, PhotonMap map)
  {
    // Find the center and radius of the scene.
    
    center = map.getBounds().getCenter();
    Vec3 corner[] = map.getBounds().getCorners();
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
  
  /**
   * Generate photons and add them to a map.
   *
   * @param map          the PhotonMap to add the Photons to
   * @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
   * @param threads      a ThreadManager which may optionally be used to parallelize photon generation
   */

  public void generatePhotons(final PhotonMap map, final double intensity, final ThreadManager threads)
  {
    final Thread currentThread = Thread.currentThread();
    final RaytracerRenderer renderer = map.getRenderer();
    final double emittedIntensity[] = new double[] {0.0};

    // Send out the photons.

    threads.setNumIndices(1024);
    threads.setTask(new ThreadManager.Task()
    {
      public void execute(int index)
      {
        if (renderer.renderThread != currentThread)
          return;

        // Select an origin and direction.

        double ctheta = ((index&3)-2+map.random.nextDouble())*0.5;
        double stheta = Math.sqrt(1.0-ctheta*ctheta);
        double phi = (((index>>2)&3)+map.random.nextDouble())*0.5*Math.PI;
        double sphi = Math.sin(phi);
        double cphi = Math.cos(phi);
        Ray r = new Ray(map.getWorkspace().context);
        Vec3 orig = r.getOrigin();
        Vec3 dir = r.getDirection();
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

        double photonIntensity;
        if (envMode == Scene.ENVIRON_DIFFUSE || envMode == Scene.ENVIRON_EMISSIVE)
          {
            TextureSpec spec = map.getWorkspace().surfSpec[0];
            envMapping.getTextureSpec(dir.times(-1.0), spec, 1.0, renderer.smoothScale*renderer.extraGIEnvSmoothing, renderer.time, null);
            if (envMode == Scene.ENVIRON_DIFFUSE)
              color.copy(spec.diffuse);
            else
              color.copy(spec.emissive);
            photonIntensity = color.getRed()+color.getGreen()+color.getBlue();
            if (photonIntensity < 1.0)
              {
                // Use Russian Roulette sampling.

                if (photonIntensity < map.random.nextFloat())
                  return;
                color.scale(1.0f/photonIntensity);
              }
          }
        else
          photonIntensity = 1.0;
        synchronized (emittedIntensity)
        {
          if (emittedIntensity[0] >= intensity)
          {
            threads.cancel();
            return;
          }
          emittedIntensity[0] += photonIntensity;
        }

        // Send out the photon.

        r.newID();
        map.spawnPhoton(r, color, true);
      }
      public void cleanup()
      {
        map.getWorkspace().cleanup();
      }
    });
    while (emittedIntensity[0] < intensity)
      threads.run();
  }
}
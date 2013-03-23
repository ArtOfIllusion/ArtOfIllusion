/* Copyright (C) 2003-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.util.*;

/** This is a PhotonSource corresponding to a point light. */

public class PointPhotonSource implements PhotonSource
{
  private PointLight light;
  private Vec3 pos;
  private RGBColor color;
  private float lightIntensity;

  /** Create a PointPhotonSource. */
  
  public PointPhotonSource(PointLight light, CoordinateSystem coords, PhotonMap map)
  {
    this.light = light;
    pos = coords.getOrigin();

    // Because the light does not fall off exactly as 1/r^2, the "intensity" varies with distance.
    // Select an average intensity based on the size of the scene.

    Vec3 corner[] = map.getBounds().getCorners();
    double maxDist2 = 0.0;
    for (int i = 0; i < corner.length; i++)
      {
        double dist2 = pos.distance2(corner[i]);
        if (dist2 > maxDist2)
          maxDist2 = dist2;
      }
    color = new RGBColor();
    double radius = Math.sqrt(maxDist2)*0.5;
    light.getLight(color, new Vec3(radius, 0.0, 0.0));
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
  
  public void generatePhotons(final PhotonMap map, double intensity, ThreadManager threads)
  {
    final Thread currentThread = Thread.currentThread();
    final boolean randomizeOrigin = map.getRaytracer().getUseSoftShadows();
    int num = (int) intensity;

    // Send out the photons.  To reduce noise, we use stratified sampling.  Repeatedly find the largest
    // NxN grid whose number of cells is smaller than the number of photons needed, and send out a photon
    // through a random point in each cell.

    while (num > 0)
      {
        final int n = (int) Math.sqrt(num);
        final double du = 2.0/n, dphi = 2.0*Math.PI/n;
        threads.setNumIndices(n*n);
        threads.setTask(new ThreadManager.Task()
        {
          public void execute(int index)
          {
            if (map.getRenderer().renderThread != currentThread)
              return;
            int i = index/n;
            int j = index-(i*n);
            double baseu = -1.0+i*du;
            double basephi = j*dphi;
            Ray r = new Ray(map.getWorkspace().context);
            Vec3 orig = r.getOrigin();
            Vec3 dir = r.getDirection();
            orig.set(pos);
            if (randomizeOrigin)
              map.randomizePoint(orig, light.getRadius());
            double ctheta = baseu+map.random.nextDouble()*du, phi = basephi+map.random.nextDouble()*dphi;
            double stheta = Math.sqrt(1.0-ctheta*ctheta);
            double cphi = Math.cos(phi), sphi = Math.sin(phi);
            dir.set(stheta*sphi, stheta*cphi, ctheta);
            r.newID();
            map.spawnPhoton(r, color, false);
          }
          public void cleanup()
          {
            map.getWorkspace().cleanup();
          }
        });
        threads.run();
        num -= n*n;
      }
  }
}
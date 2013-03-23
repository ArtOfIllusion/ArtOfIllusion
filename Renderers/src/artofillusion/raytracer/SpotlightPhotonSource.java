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

/** This is a PhotonSource corresponding to a spotlight. */

public class SpotlightPhotonSource implements PhotonSource
{
  private SpotLight light;
  private CoordinateSystem coords;
  private RGBColor color;
  private float lightIntensity;
  private double minu;

  /** Create a SpotlightPhotonSource. */
  
  public SpotlightPhotonSource(SpotLight light, CoordinateSystem coords, PhotonMap map)
  {
    this.light = light;
    this.coords = coords;
    double exp = light.getExponent()+1.0;
    minu = Math.pow(light.getAngleCosine(), exp)/exp;

    // Because the light does not fall off exactly as 1/r^2, the "intensity" varies with distance.
    // Select an effective intensity based on the furthest point in the scene from the light.

    Vec3 corner[] = map.getBounds().getCorners();
    Vec3 pos = coords.getOrigin();
    double maxDist2 = 0.0;
    for (int i = 0; i < corner.length; i++)
      {
        double dist2 = pos.distance2(corner[i]);
        if (dist2 > maxDist2)
          maxDist2 = dist2;
      }
    color = new RGBColor();
    double radius = Math.sqrt(maxDist2)*0.5;
    light.getLight(color, new Vec3(0.0, 0.0, radius));
    lightIntensity = color.getRed()+color.getGreen()+color.getBlue();
    if (lightIntensity == 0.0f)
      return;
    color.scale(1.0f/lightIntensity);
    lightIntensity *= (float) ((1.0/exp-minu)*2.0*Math.PI*radius*radius);
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
  
  public void generatePhotons(final PhotonMap map, double intensity, ThreadManager threads)
  {
    final Thread currentThread = Thread.currentThread();
    final Vec3 pos = coords.getOrigin();
    final Vec3 xdir = coords.fromLocal().timesDirection(Vec3.vx());
    final Vec3 ydir = coords.fromLocal().timesDirection(Vec3.vy());
    final Vec3 zdir = coords.getZDirection();
    final double exp = light.getExponent()+1.0, expInv = 1.0/exp;
    final double maxu = 1.0/exp;
    final double usize = maxu-minu;
    final boolean randomizeOrigin = map.getRaytracer().getUseSoftShadows();
    int num = (int) intensity;

    // Send out the photons.  To reduce noise, we use stratified sampling.  Repeatedly find the largest
    // NxN grid whose number of cells is smaller than the number of photons needed, and send out a photon
    // through a random point in each cell.

    while (num > 0)
      {
        final int n = Math.max((int) Math.sqrt(num), 1);
        final double du = usize/n, dv = 2.0*Math.PI/n;
        threads.setNumIndices(n*n);
        threads.setTask(new ThreadManager.Task()
        {
          public void execute(int index)
          {
            if (map.getRenderer().renderThread != currentThread)
              return;
            int i = index/n;
            int j = index-(i*n);
            double baseu = minu+i*du;
            double basev = j+dv;
            Ray r = new Ray(map.getWorkspace().context);
            Vec3 orig = r.getOrigin();
            Vec3 dir = r.getDirection();
            double u = baseu+map.random.nextDouble()*du, v = basev+map.random.nextDouble()*dv;
            double ctheta = Math.pow(u*exp, expInv);
            double stheta = Math.sqrt(1.0-ctheta*ctheta);
            double cphi = Math.cos(v);
            double sphi = Math.sin(v);
            double x = stheta*sphi, y = stheta*cphi, z = ctheta;
            dir.set(x*xdir.x+y*ydir.x+z*zdir.x, x*xdir.y+y*ydir.y+z*zdir.y, x*xdir.z+y*ydir.z+z*zdir.z);
            orig.set(pos);
            if (randomizeOrigin)
              map.randomizePoint(orig, light.getRadius());
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
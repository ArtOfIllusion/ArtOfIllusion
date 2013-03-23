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
import artofillusion.*;

/** This is a PhotonSource corresponding to an RTTriangle. */

public class TrianglePhotonSource implements PhotonSource
{
  private RenderingTriangle tri;
  private RGBColor color;
  private float lightIntensity;

  /** Create an TrianglePhotonSource.
      @param tri    the triangle for which to create a photon source
      @param map    the photon map for which this will generate photons
  */
  
  public TrianglePhotonSource(RenderingTriangle tri, PhotonMap map)
  {
    this.tri = tri;
    
    // Find the size of the triangle.
    
    Vec3 vert1 = tri.theMesh.vert[tri.v1];
    Vec3 vert2 = tri.theMesh.vert[tri.v2];
    Vec3 vert3 = tri.theMesh.vert[tri.v3];
    Vec3 e1 = vert2.minus(vert1);
    Vec3 e2 = vert3.minus(vert1);
    double area = 0.5*e1.cross(e2).length();
    double dist1 = e1.length(), dist2 = e2.length(), dist3 = vert2.distance(vert3);
    double avgSize = (dist1+dist2+dist3)*(1.0/6.0);
    
    // Find the average emissive intensity.
    
    TextureSpec spec = map.getWorkspace().surfSpec[0];
    double third = 1.0/3.0;
    color = new RGBColor();
    tri.getTextureSpec(spec, 1.0, third, third, third, avgSize, map.getRaytracer().getTime());
    color.copy(spec.emissive);
    tri.getTextureSpec(spec, -1.0, third, third, third, avgSize, map.getRaytracer().getTime());
    color.add(spec.emissive);
    lightIntensity = 0.5f*(color.getRed()+color.getGreen()+color.getBlue())*(float) area;
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
  
  public void generatePhotons(PhotonMap map, double intensity, ThreadManager threads)
  {
    RaytracerRenderer rt = map.getRenderer();
    TextureSpec spec = map.getWorkspace().surfSpec[0];
    Ray r = new Ray(map.getWorkspace().context);
    Vec3 vert1 = tri.theMesh.vert[tri.v1];
    Vec3 vert2 = tri.theMesh.vert[tri.v2];
    Vec3 vert3 = tri.theMesh.vert[tri.v3];
    Vec3 orig = r.getOrigin();
    Vec3 dir = r.getDirection();
    Vec3 trueNorm = tri.theMesh.faceNorm[tri.index];
    double emittedIntensity = 0.0;

    // Send out the photons.

    while (emittedIntensity < intensity)
      {
        // Select a direction.
        
        dir.set(0.0, 0.0, 0.0);
        map.randomizePoint(dir, 1.0);
        dir.normalize();
        double dot = trueNorm.dot(dir), absdot = (dot > 0.0 ? dot : -dot);
        if (absdot < map.random.nextDouble())
          continue;

        // Select an origin.

        double u, v, w;
        do
          {
            u = map.random.nextDouble();
            v = map.random.nextDouble();
            w = 1.0-u-v;
          } while (w < 0.0);
        
        // Evaluate the texture at the ray origin.
        
        tri.getTextureSpec(spec, dot, u, v, w, rt.smoothScale, rt.time);
        color.copy(spec.emissive);
        float sum = color.getRed()+color.getGreen()+color.getBlue();
        emittedIntensity += sum;
        if (emittedIntensity > intensity)
          if ((emittedIntensity-intensity)/sum > map.random.nextFloat())
            return;
        if (sum < 1.0f)
          {
            // Use Russian Roulette sampling.
            
            if (sum < map.random.nextFloat())
              continue;
            color.scale(1.0f/sum);
          }
        
        // Send out the photon.
        
        orig.set(u*vert1.x+v*vert2.x+w*vert3.x, u*vert1.y+v*vert2.y+w*vert3.y, u*vert1.z+v*vert2.z+w*vert3.z);
        r.newID();
        map.spawnPhoton(r, color, true);
      }
  }
}
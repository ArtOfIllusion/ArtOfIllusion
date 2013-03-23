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

/** This is a PhotonSource corresponding to an RTDisplacedTriangle. */

public class DisplacedTrianglePhotonSource implements PhotonSource
{
  private RTDisplacedTriangle tri;
  private RGBColor color;
  private float lightIntensity;

  /** Create an DisplacedTrianglePhotonSource.
      @param tri    the triangle for which to create a photon source
      @param map    the photon map for which this will generate photons
  */
  
  public DisplacedTrianglePhotonSource(RTDisplacedTriangle tri, PhotonMap map)
  {
    this.tri = tri;
    
    // Find the size of the triangle.
    
    Vec3 vert1 = tri.tri.theMesh.vert[tri.tri.v1];
    Vec3 vert2 = tri.tri.theMesh.vert[tri.tri.v2];
    Vec3 vert3 = tri.tri.theMesh.vert[tri.tri.v3];
    Vec3 e1 = vert2.minus(vert1);
    Vec3 e2 = vert3.minus(vert1);
    double area = 0.5*e1.cross(e2).length();
    double dist1 = e1.length(), dist2 = e2.length(), dist3 = vert2.distance(vert3);
    double avgSize = (dist1+dist2+dist3)*(1.0/6.0);
    area += avgSize*(tri.maxheight-tri.minheight);  // VERY rough estimate (aka wild guess) of the true surface area
    
    // Find the average emissive intensity.
    
    TextureSpec spec = map.getWorkspace().surfSpec[0];
    double third = 1.0/3.0;
    color = new RGBColor();
    tri.tri.getTextureSpec(spec, 1.0, third, third, third, avgSize, map.getRaytracer().getTime());
    color.copy(spec.emissive);
    tri.tri.getTextureSpec(spec, -1.0, third, third, third, avgSize, map.getRaytracer().getTime());
    color.add(spec.emissive);
    lightIntensity = 0.5f*(color.getRed()+color.getGreen()+color.getBlue())*(float) area;
  }

  /** Get the total intensity of light which this object sends into the scene. */

  public double getTotalIntensity()
  {
    return lightIntensity;
  }
  
  /** Generate photons and add them to a map.
   @param map          the PhotonMap to add the Photons to
    * @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
   * @param threads
  */
  
  public void generatePhotons(PhotonMap map, double intensity, ThreadManager threads)
  {
    RaytracerRenderer renderer = map.getRenderer();
    RenderWorkspace workspace = map.getWorkspace();
    TextureSpec spec = workspace.surfSpec[0];
    Ray r = new Ray(workspace.context);
    Vec3 vert1 = tri.tri.theMesh.vert[tri.tri.v1];
    Vec3 vert2 = tri.tri.theMesh.vert[tri.tri.v2];
    Vec3 vert3 = tri.tri.theMesh.vert[tri.tri.v3];
    Vec3 norm1 = tri.tri.theMesh.norm[tri.tri.n1];
    Vec3 norm2 = tri.tri.theMesh.norm[tri.tri.n2];
    Vec3 norm3 = tri.tri.theMesh.norm[tri.tri.n3];
    Vec3 temp1 = new Vec3(), temp2 = new Vec3(), temp3 = new Vec3(), temp4 = new Vec3();
    Vec3 normal = new Vec3();
    Vec3 orig = r.getOrigin();
    Vec3 dir = r.getDirection();
    double emittedIntensity = 0.0, tol = renderer.surfaceError;

    // Send out the photons.
    
    while (emittedIntensity < intensity)
      {
        // Select an origin.

        double u, v, w;
        do
          {
            u = map.random.nextDouble();
            v = map.random.nextDouble();
            w = 1.0-u-v;
          } while (w < 0.0);
        
        // Determine the position and normal of the surface at that point.
        
        double disp = tri.tri.getDisplacement(u, v, w, tol, renderer.time);
        orig.set(vert1.x+disp*norm1.x, vert1.y+disp*norm1.y, vert1.z+disp*norm1.z);
        double dhdu = (tri.tri.getDisplacement(u+(1e-5), v, w-(1e-5), tol, renderer.time)-disp)*1e5;
        double dhdv = (tri.tri.getDisplacement(u, v+(1e-5), w-(1e-5), tol, renderer.time)-disp)*1e5;
        normal.set(u*norm1.x+v*norm2.x+w*norm3.x, u*norm1.y+v*norm2.y+w*norm3.y, u*norm1.z+v*norm2.z+w*norm3.z);
        normal.normalize();
        temp1.set(vert1.x+disp*norm1.x, vert1.y+disp*norm1.y, vert1.z+disp*norm1.z);
        temp2.set(vert2.x+disp*norm2.x, vert2.y+disp*norm2.y, vert2.z+disp*norm2.z);
        temp3.set(vert3.x+disp*norm3.x, vert3.y+disp*norm3.y, vert3.z+disp*norm3.z);
        temp1.set(temp1.x-temp3.x, temp1.y-temp3.y, temp1.z-temp3.z);
        temp2.set(temp3.x-temp2.x, temp3.y-temp2.y, temp3.z-temp2.z);
        temp3.set(temp1.y*normal.z-temp1.z*normal.y, temp1.z*normal.x-temp1.x*normal.z, temp1.x*normal.y-temp1.y*normal.x);
        temp4.set(temp2.y*normal.z-temp2.z*normal.y, temp2.z*normal.x-temp2.x*normal.z, temp2.x*normal.y-temp2.y*normal.x);
        temp3.scale(-1.0/temp3.dot(temp2));
        temp4.scale(1.0/temp4.dot(temp1));
        temp1.set(dhdu*temp4.x+dhdv*temp3.x, dhdu*temp4.y+dhdv*temp3.y, dhdu*temp4.z+dhdv*temp3.z);
        normal.scale(temp1.dot(normal)+1.0);
        normal.subtract(temp1);
        normal.normalize();

        // Select a direction.
        
        double dot, absdot;
        do
          {
            dir.set(0.0, 0.0, 0.0);
            map.randomizePoint(dir, 1.0);
            dir.normalize();
            dot = normal.dot(dir);
            absdot = (dot > 0.0 ? dot : -dot);
          } while (absdot < map.random.nextDouble());
        
        // Evaluate the texture at the ray origin.
        
        tri.tri.getTextureSpec(spec, dot, u, v, w, renderer.smoothScale, renderer.time);
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
        
        r.newID();
        map.spawnPhoton(r, color, true);
      }
  }
}
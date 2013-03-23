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

/** This is a PhotonSource corresponding to an RTCube. */

public class CubePhotonSource implements PhotonSource
{
  private RTCube cube;
  private double bottomP, topP, leftP, rightP, backP;
  private double param[];
  private Mat4 fromLocal;
  private TextureMapping texMap;
  private RGBColor color;
  private float lightIntensity;

  /** Create a CubePhotonSource from a RTCube.
      @param obj    the object for which to create a photon source
      @param map    the photon map for which this will generate photons
  */

  public CubePhotonSource(RTCube obj, PhotonMap map)
  {
    cube = obj;
    double xsize = cube.maxx-cube.minx;
    double ysize = cube.maxy-cube.miny;
    double zsize = cube.maxz-cube.minz;
    double xyarea = xsize*ysize;
    double xzarea = xsize*zsize;
    double yzarea = ysize*zsize;
    double totalArea = 2.0*(xyarea+xzarea+yzarea);
    double invArea = 1.0/totalArea;
    bottomP = xzarea*invArea;
    topP = bottomP+xzarea*invArea;
    leftP = topP+yzarea*invArea;
    rightP = leftP+yzarea*invArea;
    backP = rightP+xyarea*invArea;
    fromLocal = obj.fromLocal;
    param = obj.param;
    texMap = obj.getTextureMapping();
    color = new RGBColor();

    // Find the average emissive intensity.

    TextureSpec spec = map.getWorkspace().surfSpec[0];
    texMap.getTexture().getAverageSpec(spec, map.getRaytracer().getTime(), obj.param);
    color.copy(spec.emissive);
    lightIntensity = 0.5f*(color.getRed()+color.getGreen()+color.getBlue())*(float) totalArea;
    if (texMap.appliesTo() == TextureMapping.FRONT_AND_BACK)
      lightIntensity *= 2.0f;
  }

  /** Get the total intensity of light which this object sends into the scene. */

  public double getTotalIntensity()
  {
    return lightIntensity;
  }

  /** Generate photons and add them to a map.
   @param map          the PhotonMap to add the Photons to
   @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
   @param threads
  */

  public void generatePhotons(PhotonMap map, double intensity, ThreadManager threads)
  {
    RenderWorkspace workspace = map.getWorkspace();
    Ray r = new Ray(workspace.context);
    Vec3 orig = r.getOrigin();
    Vec3 norm = new Vec3();
    double xsize = cube.maxx-cube.minx;
    double ysize = cube.maxy-cube.miny;
    double zsize = cube.maxz-cube.minz;
    double emittedIntensity = 0.0;
    while (emittedIntensity < intensity)
      {
        // Select an origin and direction.

        double p = map.random.nextDouble();
        double u = map.random.nextDouble()-0.5;
        double v = map.random.nextDouble()-0.5;
        if (p < bottomP)
        {
          orig.set(xsize*u+cube.minx, cube.miny, zsize*v+cube.minz);
          norm.set(0.0, -1.0, 0.0);
        }
        else if (p < topP)
        {
          orig.set(xsize*u+cube.minx, cube.maxy, zsize*v+cube.minz);
          norm.set(0.0, 1.0, 0.0);
        }
        else if (p < leftP)
        {
          orig.set(cube.minx, ysize*u+cube.miny, zsize*v+cube.minz);
          norm.set(-1.0, 0.0, 0.0);
        }
        else if (p < rightP)
        {
          orig.set(cube.maxx, ysize*u+cube.miny, zsize*v+cube.minz);
          norm.set(1.0, 0.0, 0.0);
        }
        else if (p < backP)
        {
          orig.set(xsize*u+cube.minx, ysize*v+cube.miny, cube.minz);
          norm.set(0.0, 0.0, -1.0);
        }
        else
        {
          orig.set(xsize*u+cube.minx, ysize*v+cube.miny, cube.maxz);
          norm.set(0.0, 0.0, 1.0);
        }
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
        orig.x += 0.5*(cube.minx+cube.maxx);
        orig.y += 0.5*(cube.miny+cube.maxy);
        orig.z += 0.5*(cube.minz+cube.maxz);
      }
    r.newID();
    map.spawnPhoton(r, color, true);
    return intensity;
  }
}
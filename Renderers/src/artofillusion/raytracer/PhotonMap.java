/* Copyright (C) 2003-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.material.*;
import artofillusion.texture.*;
import artofillusion.util.*;

import java.util.*;

/** This class is a three dimensional data structure containing the photons in a scene.  The map can
    be searched very efficiently for locating the photons near a particular point and evaluating the
    local illumination
    
    Parts of this class are based on the descriptions and sample code in
    
    Henrick Wann Jensen, "Realistic Image Synthesis Using Photon Mapping", A K Peters, Natick, MA, 2001. */

public class PhotonMap
{
  private Raytracer rt;
  private RaytracerRenderer renderer;
  private ArrayList<Photon> photonList;
  private Photon photon[], workspace[];
  private int numWanted, filter, numEstimate;
  private BoundingBox bounds;
  private Vec3 direction[];
  private boolean includeCaustics, includeDirect, includeIndirect, includeVolume;
  private double lightScale;
  private float cutoffDist2;
  public Random random;
  
  /** Create a new PhotonMap
   * @param totalPhotons        the number of photons which should be stored in this map
   * @param numEstimate         the number of photons to use when estimating the illumination from this map
   * @param includeCaustics     true if this PhotonMap should include photons that represent caustics
   * @param includeDirect       true if this PhotonMap should include photons that represent direct illumination
   * @param includeIndirect     true if this PhotonMap should include photons that represent indirect illumination
   * @param includeVolume
   * @param raytracer           the Raytracer for which this PhotonMap is being generated
   * @param renderer            the renderer for which this PhotonMap is being generated
   * @param bounds              a bounding box enclosing all objects at which photons should be directed
   * @param filter              specifies which type of filter to apply to the photon intensities
   * @param shared              another PhotonMap with which this one may share data structures to save memory (may be null)
   */
  
  public PhotonMap(int totalPhotons, int numEstimate, boolean includeCaustics, boolean includeDirect, boolean includeIndirect, boolean includeVolume, Raytracer raytracer, RaytracerRenderer renderer, BoundingBox bounds, int filter, PhotonMap shared)
  {
    numWanted = totalPhotons;
    this.bounds = bounds;
    this.includeCaustics = includeCaustics;
    this.includeDirect = includeDirect;
    this.includeIndirect = includeIndirect;
    this.includeVolume = includeVolume;
    this.filter = filter;
    this.numEstimate = numEstimate;
    rt = raytracer;
    this.renderer = renderer;
    if (shared != null)
      direction = shared.direction;
    else
      direction = new Vec3 [65536];
    random = new Random(1);
  }
  
  /** Get the Raytracer for which this map holds photons. */
  
  public Raytracer getRaytracer()
  {
    return rt;
  }

  /** Get the renderer for which this map holds photons. */

  public RaytracerRenderer getRenderer()
  {
    return renderer;
  }

  /** Get the RenderWorkspace in which the map is being built. */

  public RenderWorkspace getWorkspace()
  {
    return renderer.getWorkspace();
  }

  /** Get a bounding box enclosing all objects at which photons should be directed. */
  
  public BoundingBox getBounds()
  {
    return bounds;
  }

  /** Get the number of photons to use to estimate the local illumination. */

  public int getNumToEstimate()
  {
    return numEstimate;
  }

  /** Generate photons from all sources until the desired number has been collected. */
  
  public void generatePhotons(PhotonSource source[])
  {
    Thread currentThread = Thread.currentThread();
    double totalIntensity = 0.0, currentIntensity, totalRequested = 0.0;
    double sourceIntensity[] = new double [source.length], totalSourceIntensity = 0.0;
    
    // Determine the total intensity of all sources.
    
    for (int i = 0; i < source.length; i++)
      {
        sourceIntensity[i] = source[i].getTotalIntensity();
        totalSourceIntensity += sourceIntensity[i];
      }
    currentIntensity = 0.1*numWanted;
    
    // Generate photons.
    
    photonList = new ArrayList<Photon>((int) (1.1*numWanted));
    int iteration = 0;
    ThreadManager threads = new ThreadManager();
    try
    {
      while (photonList.size() < numWanted)
      {
        for (int i = 0; i < source.length; i++)
          {
            if (renderer.renderThread != currentThread)
              return;
            source[i].generatePhotons(this, currentIntensity*sourceIntensity[i]/totalSourceIntensity, threads);
            totalRequested += currentIntensity*sourceIntensity[i]/totalSourceIntensity;
          }
        if (photonList.size() >= numWanted*0.9)
          break;
        if (photonList.size() == 0 && currentIntensity > 5.0 && iteration > 2)
          break; // Insignificant numbers of photons will be stored no matter how many we send out.
        totalIntensity += currentIntensity;
        if (photonList.size() < 10)
          currentIntensity *= 10.0;
        else
          currentIntensity = (numWanted-photonList.size())*totalIntensity/photonList.size();
        iteration++;
      }
    }
    finally
    {
      threads.finish();
    }
    lightScale = totalSourceIntensity/totalRequested;
    if (filter == 2)
      lightScale *= 3.0f;
    else if (filter == 1)
      lightScale *= 1.5f;

    // Create the balanced kd-tree.
    
    int numPhotons = photonList.size();
    workspace = photonList.toArray(new Photon [numPhotons]);
    photonList = null;
    photon = new Photon [numPhotons];
    buildTree(0, numPhotons-1, 0);
    workspace = null;
    
    // Select a maximum search radius.  We use two different methods to select cutoffs, one based on photon
    // intensity and one based on density, then keep whichever cutoff is smaller.  First, find the N brightest
    // photons in the map.  The PhotonList can help us to do this.

    PhotonList nearbyPhotons = new PhotonList(numEstimate);
    RGBColor tempColor = new RGBColor();
    nearbyPhotons.init(0.0f);
    for (int i = 0; i < photon.length; i++)
      {
        tempColor.setERGB(photon[i].ergb);
        float intensity = -(tempColor.getRed()+tempColor.getGreen()+tempColor.getBlue());
        if (intensity <= nearbyPhotons.cutoff2)
          nearbyPhotons.addPhoton(photon[i], intensity);
      }
    float red = 0.0f, green = 0.0f, blue = 0.0f;
    for (int i = 0; i < nearbyPhotons.numFound; i++)
      {
        tempColor.setERGB(nearbyPhotons.photon[i].ergb);
        red += tempColor.getRed();
        green += tempColor.getGreen();
        blue += tempColor.getBlue();
      }
    float max = Math.max(Math.max(red, green), blue);
    double cutoff1;
    if (includeVolume)
      cutoff1 = Math.pow(max*lightScale/((4.0/3.0)*Math.PI*0.1), 1.0/3.0);
    else
      cutoff1 = Math.sqrt(max*lightScale/(Math.PI*0.1));
    double volume = (bounds.maxx-bounds.minx)*(bounds.maxy-bounds.miny)*(bounds.maxz-bounds.minz);
    double cutoff2 = Math.pow(0.5*volume*nearbyPhotons.photon.length/photon.length, 1.0/3.0);
    cutoffDist2 = (float) (cutoff1 < cutoff2 ? cutoff1*cutoff1 : cutoff2*cutoff2);
  }
  
  /** Spawn a Photon, and see whether it hits anything in the scene.  If so, add it to the map.
      @param r         the ray along which to spawn the photon
      @param color     the photon color
      @param indirect  specifies whether the photon's source should be treated as indirect illumination
  */
  
  public void spawnPhoton(Ray r, RGBColor color, boolean indirect)
  {
    if (!r.intersects(bounds))
      return;
    OctreeNode node = rt.getRootNode().findNode(r.getOrigin());
    if (node == null)
      node = rt.getRootNode().findFirstNode(r);
    if (node == null)
      return;
    color = color.duplicate();
    RTObject materialObject = renderer.getMaterialAtPoint(getWorkspace(), r.getOrigin(), node);
    if (materialObject == null)
      tracePhoton(r, color, 0, node, SurfaceIntersection.NO_INTERSECTION, null, null, null, null, 0.0, indirect, false);
    else
      tracePhoton(r, color, 0, node, SurfaceIntersection.NO_INTERSECTION, materialObject.getMaterialMapping(), null, materialObject.toLocal(), null, 0.0, indirect, false);
  }
  
  /** Trace a photon through the scene, and record where it is absorbed.
      @param r                  the ray to trace
      @param color              the photon color
      @param treeDepth          the depth of this ray within the ray tree
      @param node               the first octree node which the ray intersects
      @param first              the first object which the ray intersects, or null if this is not known
      @param currentMaterial    the MaterialMapping at the ray's origin (may be null)
      @param prevMaterial       the MaterialMapping the ray was passing through before entering currentMaterial
      @param currentMatTrans    the transform to local coordinates for the current material
      @param prevMatTrans       the transform to local coordinates for the previous material
      @param totalDist          the distance traveled from the viewpoint
      @param diffuse            true if this ray has been diffusely reflected since leaving the eye
      @param caustic            true if this ray has been specularly reflected or refracted since leaving the eye
  */
  
  private void tracePhoton(Ray r, RGBColor color, int treeDepth, OctreeNode node, SurfaceIntersection first, MaterialMapping currentMaterial, MaterialMapping prevMaterial, Mat4 currentMatTrans, Mat4 prevMatTrans, double totalDist, boolean diffuse, boolean caustic)
  {
    SurfaceIntersection second = SurfaceIntersection.NO_INTERSECTION;
    double dist, truedot, n = 1.0, beta = 0.0, d;
    RenderWorkspace workspace = getWorkspace();
    Vec3 intersectionPoint = workspace.pos[treeDepth], norm = workspace.normal[treeDepth], trueNorm = workspace.trueNormal[treeDepth];
    TextureSpec spec = workspace.surfSpec[treeDepth];
    MaterialMapping nextMaterial, oldMaterial;
    Mat4 nextMatTrans, oldMatTrans = null;
    OctreeNode nextNode;

    // Find whether it hits anything.

    SurfaceIntersection intersect = SurfaceIntersection.NO_INTERSECTION;
    if (first != SurfaceIntersection.NO_INTERSECTION)
      intersect = r.findIntersection(first.getObject());
    if (intersect != SurfaceIntersection.NO_INTERSECTION)
    {
      intersect.intersectionPoint(0, intersectionPoint);
      nextNode = rt.getRootNode().findNode(intersectionPoint);
    }
    else
    {
      nextNode = rt.traceRay(r, node, workspace.context.intersect);
      if (nextNode == null)
        return;
      first = workspace.context.intersect.getFirst();
      second = workspace.context.intersect.getSecond();
      intersect = first;
      intersect.intersectionPoint(0, intersectionPoint);
    }
    
    // Get the surface properties at the point of intersection.
    
    dist = intersect.intersectionDist(0);
    totalDist += dist;
    intersect.trueNormal(trueNorm);
    truedot = trueNorm.dot(r.getDirection());
    double texSmoothing = (diffuse ? renderer.smoothScale*renderer.extraGISmoothing : renderer.smoothScale);
    if (truedot > 0.0)
      intersect.intersectionProperties(spec, norm, r.getDirection(), totalDist*texSmoothing*3.0/(2.0+truedot), rt.getTime());
    else
      intersect.intersectionProperties(spec, norm, r.getDirection(), totalDist*texSmoothing*3.0/(2.0-truedot), rt.getTime());

      // Reduce the photon intensity based on the current material or fog.

    if (currentMaterial != null)
    {
      if (includeVolume && currentMaterial.isScattering())
      {
        // See whether a photon gets stored in the material.

        propagateRay(r, nextNode, second, dist, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, color, treeDepth, totalDist, caustic, diffuse);
      }
      else
      {
        RGBColor emissive = new RGBColor(); // This will be ignored.
        workspace.rt.propagateRay(workspace, r, nextNode, dist, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, emissive, color, treeDepth, totalDist);
      }
    }
    else if (renderer.fog)
      color.scale((float) Math.exp(-dist/renderer.fogDist));
    if (color.getRed()+color.getGreen()+color.getBlue() < renderer.minRayIntensity)
      return;  // The photon color is too dim to matter.

    // Decide whether to store a photon here.

    if (!includeVolume)
    {
      if ((includeDirect && treeDepth == 0) ||
          (includeIndirect && diffuse) ||
          (includeCaustics && caustic))
        {
          if (spec.diffuse.getRed()+spec.diffuse.getGreen()+spec.diffuse.getBlue()+
              spec.hilight.getRed()+spec.hilight.getGreen()+spec.hilight.getBlue() > renderer.minRayIntensity)
            addPhoton(intersectionPoint, r.getDirection(), color);
        }
    }

    // Decide whether to spawn reflected and/or transmitted photons.
    
    if (treeDepth == renderer.maxRayDepth-1)
      return;
    boolean spawnSpecular = false, spawnTransmitted = false, spawnDiffuse = false;
    if (includeCaustics || includeVolume)
    {
      if (spec.specular.getRed()+spec.specular.getGreen()+spec.specular.getBlue() > renderer.minRayIntensity)
        spawnSpecular = true;
    }
    if (includeCaustics || includeVolume)
    {
      if (spec.transparent.getRed()+spec.transparent.getGreen()+spec.transparent.getBlue() > renderer.minRayIntensity)
        spawnTransmitted = true;
    }
    if (includeIndirect && !includeVolume)
      {
        if (spec.diffuse.getRed()+spec.diffuse.getGreen()+spec.diffuse.getBlue() > renderer.minRayIntensity)
          spawnDiffuse = true;
      }
    
    // Spawn additional photons.
    
    double dot = norm.dot(r.getDirection());
    RGBColor col = workspace.rayIntensity[treeDepth+1];
    boolean totalReflect = false;
    if (spawnTransmitted)
      {
        // Spawn a transmitted photon.

        col.copy(color);
        col.multiply(spec.transparent);
        workspace.ray[treeDepth+1].getOrigin().set(intersectionPoint);
        Vec3 temp = workspace.ray[treeDepth+1].getDirection();
        RTObject hitObject = first.getObject();
        if (hitObject.getMaterialMapping() == null)
          {
            // Not a solid object, so the bulk material does not change.
            
            temp.set(r.getDirection());
            nextMaterial = currentMaterial;
            nextMatTrans = currentMatTrans;
            oldMaterial = prevMaterial;
            oldMatTrans = prevMatTrans;
          }
        else if (dot < 0.0)
          {
            // Entering an object.

            nextMaterial = hitObject.getMaterialMapping();
            nextMatTrans = hitObject.toLocal();
            oldMaterial = currentMaterial;
            oldMatTrans = currentMatTrans;
            if (currentMaterial == null)
              n = nextMaterial.indexOfRefraction()/1.0;
            else
              n = nextMaterial.indexOfRefraction()/currentMaterial.indexOfRefraction();
            beta = -(dot+Math.sqrt(n*n-1.0+dot*dot));
            temp.set(norm);
            temp.scale(beta);
            temp.add(r.getDirection());
            temp.scale(1.0/n);
          }
        else
          {
            // Exiting an object.

            if (currentMaterial == hitObject.getMaterialMapping())
              {
                nextMaterial = prevMaterial;
                nextMatTrans = prevMatTrans;
                oldMaterial = null;
                if (nextMaterial == null)
                  n = 1.0/currentMaterial.indexOfRefraction();
                else
                  n = nextMaterial.indexOfRefraction()/currentMaterial.indexOfRefraction();
              }
            else
              {
                nextMaterial = currentMaterial;
                nextMatTrans = currentMatTrans;
                if (prevMaterial == hitObject.getMaterialMapping())
                  oldMaterial = null;
                else
                  {
                    oldMaterial = prevMaterial;
                    oldMatTrans = prevMatTrans;
                  }
                n = 1.0;
              }
            beta = dot-Math.sqrt(n*n-1.0+dot*dot);
            temp.set(norm);
            temp.scale(-beta);
            temp.add(r.getDirection());
            temp.scale(1.0/n);
          }
        if (Double.isNaN(beta))
          totalReflect = true;
        else
          {
            d = (truedot > 0.0 ? temp.dot(trueNorm) : -temp.dot(trueNorm));
            if (d < 0.0)
              {
                // Make sure it comes out the correct side.
            
                d += Raytracer.TOL;
                temp.x -= d*trueNorm.x;
                temp.y -= d*trueNorm.y;
                temp.z -= d*trueNorm.z;
                temp.normalize();
              }
            workspace.ray[treeDepth+1].newID();
            if (renderer.gloss)
              randomizeDirection(temp, norm, spec.cloudiness);
            boolean newCaustic = (caustic || n != 1.0);
            tracePhoton(workspace.ray[treeDepth+1], col, treeDepth+1, nextNode, second, nextMaterial, oldMaterial, nextMatTrans, oldMatTrans, totalDist, diffuse, newCaustic);
          }
      }
    if (spawnSpecular || totalReflect)
      {
        // Spawn a reflection ray.

        col.copy(spec.specular);
        if (totalReflect)
          col.add(spec.transparent.getRed(), spec.transparent.getGreen(), spec.transparent.getBlue());
        col.multiply(color);
        Vec3 temp = workspace.ray[treeDepth+1].getDirection();
        temp.set(norm);
        temp.scale(-2.0*dot);
        temp.add(r.getDirection());
        d = (truedot > 0.0 ? temp.dot(trueNorm) : -temp.dot(trueNorm));
        if (d >= 0.0)
          {
            // Make sure it comes out the correct side.
            
            d += Raytracer.TOL;
            temp.x += d*trueNorm.x;
            temp.y += d*trueNorm.y;
            temp.z += d*trueNorm.z;
            temp.normalize();
          }
        workspace.ray[treeDepth+1].getOrigin().set(intersectionPoint);
        workspace.ray[treeDepth+1].newID();
        if (renderer.gloss)
          randomizeDirection(temp, norm, spec.roughness);
        tracePhoton(workspace.ray[treeDepth+1], col, treeDepth+1, nextNode, second, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, totalDist, diffuse, true);
      }
    if (spawnDiffuse)
      {
        // Spawn a diffusely reflected ray.

        col.copy(spec.diffuse);
        col.multiply(color);
        Vec3 temp = workspace.ray[treeDepth+1].getDirection();
        do
          {
            temp.set(0.0, 0.0, 0.0);
            randomizePoint(temp, 1.0);
            temp.normalize();
            d = temp.dot(trueNorm) * (truedot > 0.0 ? 1.0 : -1.0);
          } while (random.nextDouble() > (d < 0.0 ? -d : d));
        if (d > 0.0)
          {
            // Make sure it comes out the correct side.
            
            temp.scale(-1.0);
          }
        workspace.ray[treeDepth+1].getOrigin().set(intersectionPoint);
        workspace.ray[treeDepth+1].newID();
        tracePhoton(workspace.ray[treeDepth+1], col, treeDepth+1, nextNode, second, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, totalDist, true, caustic);
      }
  }

  /** Propagate a ray through a material, determine how much light is removed (due to
      absorption and outscattering) and stored photons in the volume photon map.
      <p>
      On exit, rayIntensity[treeDepth] is reduced by the appropriate factor to account
      for the absorbed light.

      @param r                 the ray being propagated
      @param node              the octree node containing the ray origin
      @param dist              the distance between the ray origin and the endpoint
      @param material          the MaterialMapping through which the ray is being propagated
      @param prevMaterial      the MaterialMapping the ray was passing through before entering material
      @param currentMatTrans   the transform to local coordinates for the current material
      @param prevMatTrans      the transform to local coordinates for the previous material
      @param color             on exit, this is multiplied by the attenuation factor
      @param treeDepth         the current ray tree depth
      @param totalDist         the distance traveled from the viewpoint
      @param caustic            true if this ray has been specularly reflected or refracted since leaving the eye
      @param scattered         true if this ray has already been scattered by the material
   */

  void propagateRay(Ray r, OctreeNode node, SurfaceIntersection first, double dist, MaterialMapping material, MaterialMapping prevMaterial, Mat4 currentMatTrans, Mat4 prevMatTrans, RGBColor color, int treeDepth, double totalDist, boolean caustic, boolean scattered)
  {
    RenderWorkspace workspace = getWorkspace();
    MaterialSpec matSpec = workspace.matSpec;

    // Integrate the material properties by stepping along the ray.

    Vec3 v = workspace.ray[treeDepth+1].origin, origin = r.origin, direction = r.direction;
    double x = 0.0, newx, dx, distToScreen = renderer.theCamera.getDistToScreen(), step;
    double origx, origy, origz, dirx, diry, dirz;

    // Find the ray origin and direction in the object's local coordinates.

    v.set(origin);
    currentMatTrans.transform(v);
    origx = v.x;
    origy = v.y;
    origz = v.z;
    v.set(direction);
    currentMatTrans.transformDirection(v);
    dirx = v.x;
    diry = v.y;
    dirz = v.z;

    // Do the integration.

    step = renderer.stepSize*material.getStepSize();
    do
    {
      // Find the new point along the ray.

      dx = step*(1.5*workspace.context.random.nextDouble());
      if (this.rt.isAdaptive() && totalDist > distToScreen)
        dx *= totalDist/distToScreen;
      newx = x+dx;
      if (newx > dist)
      {
        dx = dist-x;
        x = dist;
      }
      else
        x = newx;
      totalDist += dx;
      v.set(origx+dirx*x, origy+diry*x, origz+dirz*x);

      // Find the material properties at that point.

      material.getMaterialSpec(v, matSpec, dx, this.rt.getTime());
      RGBColor trans = matSpec.transparency;
      RGBColor scat = matSpec.scattering;

      // Update the total emission and transmission.

      float rt, gt, bt;
      if (trans.getRed() == 1.0f)
        rt = 1.0f;
      else
        rt = (float) Math.pow(trans.getRed(), dx);
      if (trans.getGreen() == 1.0f)
        gt = 1.0f;
      else
        gt = (float) Math.pow(trans.getGreen(), dx);
      if (trans.getBlue() == 1.0f)
        bt = 1.0f;
      else
        bt = (float) Math.pow(trans.getBlue(), dx);
      float averageTrans = (rt+gt+bt)/3.0f;
      if (random.nextFloat() < averageTrans)
      {
        // The photon does not interact with the medium here.

        color.multiply(rt/averageTrans, gt/averageTrans, bt/averageTrans);
        continue;
      }
      float scatProb = (scat.getRed()+scat.getGreen()+scat.getBlue())/3.0f;
      if (scatProb > 0.98f)
        scatProb = 0.98f; // Otherwise photons just bounce around forever and never get stored.
      if (random.nextFloat() < scatProb && treeDepth < this.renderer.maxRayDepth-1)
      {
        // The photon is scattered.

        if (treeDepth < this.renderer.maxRayDepth-1)
        {
          RGBColor rayIntensity = workspace.rayIntensity[treeDepth+1];
          rayIntensity.copy(color);
          rayIntensity.multiply(matSpec.scattering);
          rayIntensity.scale(1.0f/scatProb);
          if (rayIntensity.getMaxComponent() > this.renderer.minRayIntensity)
          {
            // Send out a scattered ray.

            v.set(origin.x+direction.x*x, origin.y+direction.y*x, origin.z+direction.z*x);
            while (node != null && !node.contains(v))
            {
              OctreeNode nextNode = node.findNextNode(r);
              node = nextNode;
            }
            if (node == null)
              break;
            double g = matSpec.eccentricity;
            Vec3 newdir = workspace.ray[treeDepth+1].getDirection();
            if (g > 0.01 || g < -0.01)
            {
              // Importance sample the phase function.

              double theta = Math.acos((1.0+g*g-Math.pow((1-g*g)/(1-g+2*g*random.nextDouble()), 2.0))/Math.abs(2*g));
              double phi = 2*Math.PI*random.nextDouble();
              newdir.set(Math.sin(theta)*Math.cos(phi), Math.sin(theta)*Math.sin(phi), Math.cos(theta));
              Mat4 m = Mat4.objectTransform(new Vec3(), direction, Math.abs(direction.y) > 0.9 ? Vec3.vx() : Vec3.vy());
              m.transformDirection(newdir);
            }
            else
            {
              // Pick a uniformly distributed random direction.

              newdir.set(0.0, 0.0, 0.0);
              randomizePoint(newdir, 1.0);
              newdir.normalize();
            }
            tracePhoton(workspace.ray[treeDepth+1], rayIntensity, treeDepth+1, node, first, material, prevMaterial, currentMatTrans, prevMatTrans, totalDist, true, caustic);
          }
        }
        color.setRGB(0.0, 0.0, 0.0);
        break;
      }

      // The photon is absorbed.

      color.scale(1.0/(1.0-scatProb));
      if ((includeDirect || scattered) && color.getMaxComponent() > this.renderer.minRayIntensity)
        addPhoton(v, direction, color);
      color.setRGB(0.0, 0.0, 0.0);
      break;
    } while (x < dist);
  }

  /** Add a Photon to the map. */
  
  private void addPhoton(Vec3 pos, Vec3 dir, RGBColor color)
  {
    Photon p = new Photon(pos, dir, color);
    synchronized (this)
    {
      photonList.add(p);
    }
    if (direction[p.direction&0xFFFF] == null)
    {
      int i = (p.direction>>8) & 0xFF;
      int j = p.direction & 0xFF;
      double phi = i*Math.PI/128, theta = j*Math.PI/256;
      double sphi = Math.sin(phi), cphi = Math.cos(phi);
      double stheta = Math.sin(theta), ctheta = Math.cos(theta);
      direction[p.direction&0xFFFF] = new Vec3(cphi*stheta, ctheta, sphi*stheta);
    }
  }

  /** Add a random displacement to a vector.  The displacements are uniformly distributed
     over the volume of a sphere whose radius is given by size. */

  void randomizePoint(Vec3 pos, double size)
  {
    if (size == 0.0)
      return;

    // Pick a random vector within the unit sphere.

    double x, y, z;
    do
      {
        x = random.nextDouble()-0.5;
        y = random.nextDouble()-0.5;
        z = random.nextDouble()-0.5;
      } while (x*x + y*y + z*z > 0.25);
    pos.x += 2.0*size*x;
    pos.y += 2.0*size*y;
    pos.z += 2.0*size*z;
  }

  /** Given a reflected or transmitted ray, randomly alter its direction to create gloss and
     translucency effects.  dir is a unit vector in the "ideal" reflected or refracted
     direction, which on exit is overwritten with the new direction.  norm is the local
     surface normal, and roughness determines how much the ray direction is altered. */

  void randomizeDirection(Vec3 dir, Vec3 norm, double roughness)
  {
    if (roughness == 0.0)
      return;

    // Pick a random vector within the unit sphere.

    double x, y, z, scale, dot1, dot2;
    do
      {
        x = random.nextDouble()-0.5;
        y = random.nextDouble()-0.5;
        z = random.nextDouble()-0.5;
      } while (x*x + y*y + z*z > 0.25);
    scale = Math.pow(roughness, 1.7)*0.5;
    dot1 = dir.dot(norm);
    dir.x += 2.0*scale*x;
    dir.y += 2.0*scale*y;
    dir.z += 2.0*scale*z;
    dot2 = 2.0*dir.dot(norm);

    // If the ray is on the wrong side of the surface, flip it back.

    if (dot1 < 0.0 && dot2 > 0.0)
      {
        dir.x -= dot2*norm.x;
        dir.y -= dot2*norm.y;
        dir.z -= dot2*norm.z;
      }
    else if (dot1 > 0.0 && dot2 < 0.0)
      {
        dir.x += dot2*norm.x;
        dir.y += dot2*norm.y;
        dir.z += dot2*norm.z;
      }
    dir.normalize();
  }
  
  /** This method is called recursively to build the packed kd-tree of photons from the workspace array.
      @param start      the start of the segment from which to build the tree
      @param end        the end of the segment from which to build the tree
      @param root       the position in the packed array where the root of the tree should go
  */
  
  private void buildTree(int start, int end, int root)
  {
    if (start == end)
      photon[root] = workspace[start];
    if (start >= end)
      return;
  
    // Find a bounding box for the photons in this segment, and decide which axis to split.
    
    float minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, minz = Float.MAX_VALUE;
    float maxx = -Float.MAX_VALUE, maxy = -Float.MAX_VALUE, maxz = -Float.MAX_VALUE;
    for (int i = start; i <= end; i++)
      {
        Photon p = workspace[i];
        if (p.x < minx) minx = p.x;
        if (p.y < miny) miny = p.y;
        if (p.z < minz) minz = p.z;
        if (p.x > maxx) maxx = p.x;
        if (p.y > maxy) maxy = p.y;
        if (p.z > maxz) maxz = p.z;
      }
    float xsize = maxx-minx, ysize = maxy-miny, zsize = maxz-minz;
    int axis;
    if (xsize > ysize && xsize > zsize)
      axis = 0;
    else if (ysize > zsize)
      axis = 1;
    else
      axis = 2;
    
    // Split the photons about the median along this axis.
    
    int size = end-start+1;
    int medianPos = 1;
    while (4*medianPos <= size)
      medianPos += medianPos;
    if (3*medianPos <= size)
      medianPos = 2*medianPos+start-1;
    else
      medianPos = end-medianPos+1;
    medianSplit(start, end, medianPos, axis);

    // Store the median photon, and build the subtrees.
    
    photon[root] = workspace[medianPos];
    photon[root].axis = (short) axis;
    buildTree(start, medianPos-1, 2*root+1);
    buildTree(medianPos+1, end, 2*root+2);
  }
  
  /** This method splits the photons about their median along a particular axis.  When this returns,
      all the photons before medianPos will have values <= the value in medianPos, and all the ones
      after medianPos will have values >= the value in medianPos. */
  
  private void medianSplit(int start, int end, int medianPos, int axis)
  {
    float medianEstimate;
    
    if (start == end)
      return;
    if (end-start == 1)
      {
        if (axisPosition(start, axis) > axisPosition(end, axis))
          swap(start, end);
        return;
      }
    while (start < end)
      {
        // Estimate the median value.
      
        float a = axisPosition(start, axis);
        float b = axisPosition(start+1, axis);
        float c = axisPosition(end, axis);
        if (a > b)
          {
            if (a > c)
              medianEstimate = (b > c ? b : c);
            else
              medianEstimate = a;
          }
        else
          {
            if (b > c)
              medianEstimate = (a > c ? a : c);
            else
              medianEstimate = b;
          }
        
        // Split the photons based on whether they are greater than or less than the median estimate.
        
        int i = start, j = end;
        while (true)
          {
            for (; i < end && axisPosition(i, axis) < medianEstimate; i++);
            for (; axisPosition(j, axis) > medianEstimate; j--);
            if (i >= j)
              break;
            swap(i, j);
            i++;
            j--;
          }
        swap(i, end);
        if (i > medianPos)
          end = i-1;
        if (i <= medianPos)
          start = i;
      }
  }
  
  /** Get the position of a photon along an axis. */
  
  private float axisPosition(int index, int axis)
  {
    switch (axis)
      {
        case 0:
          return workspace[index].x;
        case 1:
          return workspace[index].y;
        default:
          return workspace[index].z;
      }
  }
  
  /** Swap two photons in the workspace array. */
  
  private void swap(int first, int second)
  {
    Photon temp = workspace[first];
    workspace[first] = workspace[second];
    workspace[second] = temp;
  }
  
  /** Determine the surface lighting at a point due to the photons in this map.
      @param pos      the position near which to locate photons
      @param spec     the surface properties at the point being evaluated
      @param normal   the surface normal at the point being evaluated
      @param viewDir  the direction from which the surface is being viewed
      @param front    true if the surface is being viewed from the front
      @param light    the total lighting contribution will be stored in this
      @param pmc      the PhotonMapContext from which this is being invoked
  */
   
  public void getLight(Vec3 pos, TextureSpec spec, Vec3 normal, Vec3 viewDir, boolean front, RGBColor light, PhotonMapContext pmc)
  {
    light.setRGB(0.0f, 0.0f, 0.0f);
    if (photon.length == 0)
      return;
    PhotonList nearbyPhotons = pmc.nearbyPhotons;
    RGBColor tempColor = pmc.tempColor;
    RGBColor tempColor2 = pmc.tempColor2;
    Vec3 tempVec = pmc.tempVec;
    float startCutoff2 = (float) (Math.sqrt(pmc.lastCutoff2)+pos.distance(pmc.lastPos));
    startCutoff2 *= startCutoff2;
    if (startCutoff2 > cutoffDist2)
      startCutoff2 = cutoffDist2;
    nearbyPhotons.init(startCutoff2);
    findPhotons(pos, 0, pmc);
    pmc.lastPos.set(pos);
    pmc.lastCutoff2 = nearbyPhotons.cutoff2;
    if (nearbyPhotons.numFound == 0)
      return;
    float r2inv = 1.0f/nearbyPhotons.cutoff2;
    boolean hilight = false;
    if (spec.hilight.getMaxComponent() > renderer.minRayIntensity)
    {
      hilight = true;
      tempColor2.setRGB(0.0f, 0.0f, 0.0f);
    }
    for (int i = 0; i < nearbyPhotons.numFound; i++)
    {
      Photon p = nearbyPhotons.photon[i];
      Vec3 dir = direction[p.direction&0xFFFF];
      double dot = normal.dot(dir);
      if ((front && dot < -1.0e-10) || (!front && dot > 1.0e-10))
      {
        tempColor.setERGB(p.ergb);
        float x = nearbyPhotons.dist2[i]*r2inv;
        if (filter == 2)
          tempColor.scale(x*(x-2.0f)+1.0f);
        else if (filter == 1)
          tempColor.scale(1.0f-x*x);
        light.add(tempColor);
        if (hilight)
        {
          tempVec.set(dir);
          tempVec.add(viewDir);
          tempVec.normalize();
          double viewDot = (front ? -tempVec.dot(normal) : tempVec.dot(normal));
          if (viewDot > 0.0)
          {
            float scale = (float) FastMath.pow(viewDot, (int) ((1.0-spec.roughness)*128.0)+1);
            tempColor2.add(tempColor.getRed()*scale, tempColor.getGreen()*scale, tempColor.getBlue()*scale);
          }
        }
      }
    }
    light.multiply(spec.diffuse);
    if (hilight)
    {
      tempColor2.multiply(spec.hilight);
      light.add(tempColor2);
    }
    light.scale(lightScale/(Math.PI*nearbyPhotons.cutoff2));
  }

  /** Determine the volume lighting at a point due to the photons in this map.
      @param pos      the position near which to locate photons
      @param spec     the material properties at the point being evaluated
      @param viewDir  the direction from which the material is being viewed
      @param light    the total lighting contribution will be stored in this
      @param pmc      the PhotonMapContext from which this is being invoked
  */

  public void getVolumeLight(Vec3 pos, MaterialSpec spec, Vec3 viewDir, RGBColor light, PhotonMapContext pmc)
  {
    light.setRGB(0.0f, 0.0f, 0.0f);
    if (photon.length == 0)
      return;
    PhotonList nearbyPhotons = pmc.nearbyPhotons;
    RGBColor tempColor = pmc.tempColor;
    float startCutoff2 = pmc.lastCutoff2+(float) pos.distance2(pmc.lastPos);
    if (startCutoff2 > cutoffDist2)
      startCutoff2 = cutoffDist2;
    nearbyPhotons.init(startCutoff2);
    findPhotons(pos, 0, pmc);
    pmc.lastPos.set(pos);
    pmc.lastCutoff2 = nearbyPhotons.cutoff2;
    if (nearbyPhotons.numFound == 0)
      return;
    double eccentricity = spec.eccentricity;
    double ec2 = eccentricity*eccentricity;
    for (int i = 0; i < nearbyPhotons.numFound; i++)
    {
      Photon p = nearbyPhotons.photon[i];
      tempColor.setERGB(p.ergb);
      if (eccentricity != 0.0)
      {
        Vec3 dir = direction[p.direction&0xFFFF];
        double dot = dir.dot(viewDir);
        double fatt = (1.0-ec2)/Math.pow(1.0+ec2+2.0*eccentricity*dot, 1.5);
        tempColor.scale(fatt);
      }
      light.add(tempColor);
    }
    light.scale(lightScale/((4.0/3.0)*Math.PI*Math.pow(nearbyPhotons.cutoff2, 1.5)));
  }

  /** Find the photons nearest to a given point.
      @param pos      the position near which to locate photons
      @param index    the point in the map from which to start searching
      @param pmc      the PhotonMapContext from which this is being invoked
  */
  
  private void findPhotons(Vec3 pos, int index, PhotonMapContext pmc)
  {
    Photon p = photon[index];
    float dx = p.x-(float) pos.x, dy = p.y-(float) pos.y, dz = p.z-(float) pos.z;
    float dist2 = dx*dx + dy*dy + dz*dz;
    float delta;
    switch (p.axis)
      {
        case 0:
          delta = dx;
          break;
        case 1:
          delta = dy;
          break;
        default:
          delta = dz;
      }
    if (delta > 0.0f)
      {
        int child = (index<<1)+1;
        if (child < photon.length)
          {
            findPhotons(pos, child, pmc);
            delta *= delta;
            child++;
            if (child < photon.length && delta < pmc.nearbyPhotons.cutoff2)
              findPhotons(pos, child, pmc);
          }
      }
    else
      {
        int child = (index<<1)+2;
        if (child < photon.length)
          findPhotons(pos, child, pmc);
        delta *= delta;
        child--;
        if (child < photon.length && delta < pmc.nearbyPhotons.cutoff2)
          findPhotons(pos, child, pmc);
      }
    if (dist2 < pmc.nearbyPhotons.cutoff2)
      pmc.nearbyPhotons.addPhoton(p, dist2);
  }

  private void validateTree(int pos)
  {
    int child1 = 2*pos+1, child2 = 2*pos+2;
    if (child1 < photon.length)
      {
        validateLowerBranch(child1, photon[pos].axis, median(pos, photon[pos].axis));
        validateTree(child1);
      }
    if (child2 < photon.length)
      {
        validateUpperBranch(child2, photon[pos].axis, median(pos, photon[pos].axis));
        validateTree(child2);
      }
  }
  
  private void validateLowerBranch(int pos, int axis, float median)
  {
    float value = median(pos, axis);
    if (value > median)
      System.out.println("error!");
    int child1 = 2*pos+1, child2 = 2*pos+2;
    if (child1 < photon.length)
      validateLowerBranch(child1, axis, median);
    if (child2 < photon.length)
      validateLowerBranch(child2, axis, median);
  }

  private void validateUpperBranch(int pos, int axis, float median)
  {
    float value = median(pos, axis);
    if (value < median)
      System.out.println("error!");
    int child1 = 2*pos+1, child2 = 2*pos+2;
    if (child1 < photon.length)
      validateUpperBranch(child1, axis, median);
    if (child2 < photon.length)
      validateUpperBranch(child2, axis, median);
  }

  private float median(int index, int axis)
  {
    switch (axis)
      {
        case 0:
          return photon[index].x;
        case 1:
          return photon[index].y;
        default:
          return photon[index].z;
      }
  }
}
/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.animation.distortion.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;

/** ObjectInfo represents information about an object within a Scene: its position, 
    orientation, name, visibility, etc.  The internal properties (i.e. geometry) of
    the object are defined by the "object" field.
    
    There may be several ObjectInfos in a scene whose "object" fields all reference
    the same Object3D.  In that case, they are live duplicates of each other. */

public class ObjectInfo
{
  public Object3D object;
  public CoordinateSystem coords;
  public String name;
  public boolean visible, selected, parentSelected;
  public ObjectInfo parent, children[];
  public Track tracks[];
  public Keyframe pose;
  public int id;
  private Distortion distortion, prevDistortion;
  private RenderingMesh cachedMesh;
  private WireframeMesh cachedWire;
  private BoundingBox cachedBounds;
  private boolean lastPreviewWasWireframe;

  /** Create a new ObjectInfo. */

  public ObjectInfo(Object3D obj, CoordinateSystem c, String name)
  {
    object = obj;
    coords = c;
    this.name = name;
    visible = true;
    children = new ObjectInfo [0];
    id = -1;
  }
  
  /** Create a new ObjectInfo which is identical to this one.  It will still reference the
      same Object3D object, but all other fields will be cloned. */
  
  public ObjectInfo duplicate()
  {
    return duplicate(object);
  }
  
  /** Create a new ObjectInfo which is identical to this one, but references a new Object3D. */
  
  public ObjectInfo duplicate(Object3D obj)
  {
    ObjectInfo info = new ObjectInfo(obj, coords.duplicate(), name);
    
    info.visible = visible;
    info.id = id;
    if (tracks != null)
      {
        info.tracks = new Track [tracks.length];
        for (int i = 0; i < tracks.length; i++)
          info.tracks[i] = tracks[i].duplicate(info);
      }
    if (distortion != null)
      info.distortion = distortion.duplicate();
    return info;
  }
  
  /** Given an array of ObjectInfos, duplicate all of them (including the objects they
      point to), keeping parent-child relationships intact. */
  
  public static ObjectInfo [] duplicateAll(ObjectInfo info[])
  {
    ObjectInfo newobj[] = new ObjectInfo [info.length];
    for (int i = 0; i < newobj.length; i++)
      newobj[i] = info[i].duplicate(info[i].object.duplicate());
    for (int i = 0; i < info.length; i++)
      for (int k = info[i].children.length-1; k >= 0; k--)
	{
	  int j;
	  for (j = 0; j < info.length && info[j] != info[i].children[k]; j++);
	  if (j < info.length)
	    newobj[i].addChild(newobj[j], 0);
	}
    return newobj;
  }

  /** Make this ObjectInfo identical to another one.  Both ObjectInfos will reference the
      same Object3D object, but all other fields will be cloned. */
  
  public void copyInfo(ObjectInfo info)
  {
    object = info.object;
    coords.copyCoords(info.coords);
    name = info.name.toString();
    visible = info.visible;
    id = info.id;
    cachedMesh = info.cachedMesh;
    cachedWire = info.cachedWire;
    cachedBounds = info.cachedBounds;
    if (info.tracks == null)
      tracks = null;
    else
      {
        tracks = new Track [info.tracks.length];
        for (int i = 0; i < tracks.length; i++)
          tracks[i] = info.tracks[i].duplicate(this);
      }
    if (info.distortion != null)
      distortion = info.distortion.duplicate();
    if (info.prevDistortion != null)
      prevDistortion = info.prevDistortion.duplicate();
  }
  
  /** Add a child to this object. */
  
  public void addChild(ObjectInfo info, int position)
  {
    ObjectInfo newChildren[] = new ObjectInfo [children.length+1];
    int i;
    
    for (i = 0; i < position; i++)
      newChildren[i] = children[i];
    newChildren[position] = info;
    for (; i < children.length; i++)
      newChildren[i+1] = children[i];
    children = newChildren;
    info.parent = this;
  }
  
  /** Remove a child from this object. */
  
  public void removeChild(ObjectInfo info)
  {
    for (int i = 0; i < children.length; i++)
      if (children[i] == info)
        {
          removeChild(i);
          return;
        }
  }

  /** Remove a child from this object. */
  
  public void removeChild(int which)
  {
    ObjectInfo newChildren[] = new ObjectInfo [children.length-1];
    int i;
    
    children[which].parent = null;
    for (i = 0; i < which; i++)
      newChildren[i] = children[i];
    for (i++; i < children.length; i++)
      newChildren[i-1] = children[i];
    children = newChildren;
  }

  /** Add a track to this object. */
  
  public void addTrack(Track tr, int position)
  {
    if (tracks == null)
      {
        tracks = new Track [] {tr};
        return;
      }
    Track newTracks[] = new Track [tracks.length+1];
    int i;
    
    for (i = 0; i < position; i++)
      newTracks[i] = tracks[i];
    newTracks[position] = tr;
    for (; i < tracks.length; i++)
      newTracks[i+1] = tracks[i];
    tracks = newTracks;
  }
  
  /** Remove a track from this object. */
  
  public void removeTrack(Track tr)
  {
    for (int i = 0; i < tracks.length; i++)
      if (tracks[i] == tr)
        {
          removeTrack(i);
          return;
        }
  }

  /** Remove a track from this object. */

  public void removeTrack(int which)
  {
    Track newTracks[] = new Track [tracks.length-1];
    int i;
    
    for (i = 0; i < which; i++)
      newTracks[i] = tracks[i];
    for (i++; i < tracks.length; i++)
      newTracks[i-1] = tracks[i];
    tracks = newTracks;
  }
  
  /** Set the texture and texture mapping for this object. */
  
  public void setTexture(Texture tex, TextureMapping map)
  {
    object.setTexture(tex, map);
    clearCachedMeshes();
    
    // Update any texture tracks.
    
    if (tracks != null)
      for (int i = 0; i < tracks.length; i++)
        if (tracks[i] instanceof TextureTrack)
          ((TextureTrack) tracks[i]).parametersChanged();
  }
  
  /** Set the material and material mapping for this object. */
  
  public void setMaterial(Material mat, MaterialMapping map)
  {
    object.setMaterial(mat, map);
  }
  
  /** Remove any Distortions from the object. */
  
  public void clearDistortion()
  {
    distortion = null;
  }

  /** Add a Distortion to apply to the object.  Any other Distortions which
      have previously been added will be applied before this one. */
  
  public void addDistortion(Distortion d)
  {
    d.setPreviousDistortion(distortion);
    distortion = d;
  }

  /** Returns true if a Distortion has been applied to this object. */
  
  public boolean isDistorted()
  {
    return (distortion != null);
  }

  /** See if the Distortion has changed, and clear the cached meshes if it has. */
  
  private void checkDistortionChanged()
  {
    if ((prevDistortion == distortion) ||
        (distortion != null && distortion.isIdenticalTo(prevDistortion)))
      return;
    prevDistortion = distortion;
    clearCachedMeshes();
  }
  
  /** Get a new object which has had the distortion applied to it.  If there is no distortion,
      this simply returns the original object. */
  
  public Object3D getDistortedObject(double tol)
  {
    if (distortion == null)
      return object;
    Object3D obj = object;
    while (obj instanceof ObjectWrapper)
      obj = ((ObjectWrapper) obj).getWrappedObject();
    if (!(obj instanceof Mesh) && object.canConvertToTriangleMesh() != Object3D.CANT_CONVERT)
      obj = obj.convertToTriangleMesh(tol);
    if (obj instanceof Mesh)
      obj = (Object3D) distortion.transform((Mesh) obj);
    return obj;
  }
  
  /** Get a rendering mesh for this object. */
  
  public RenderingMesh getRenderingMesh(double tol)
  {
    return getDistortedObject(tol).getRenderingMesh(tol, false, this);
  }
  
  /** Get a rendering mesh for interactive previews. */
  
  public RenderingMesh getPreviewMesh()
  {
    checkDistortionChanged();
    if (cachedMesh == null)
      {
        if (pose != null && !pose.equals(object.getPoseKeyframe()))
          object.applyPoseKeyframe(pose);
        double tol = ModellingApp.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cachedMesh = obj.getRenderingMesh(tol, true, this);
        if (cachedBounds == null)
          cachedBounds = obj.getBounds();
      }
    lastPreviewWasWireframe = false;
    return cachedMesh;
  }
  
  /** Get a wireframe mesh for interactive previews. */
  
  public WireframeMesh getWireframePreview()
  {
    checkDistortionChanged();
    if (cachedWire == null)
      {
        if (pose != null && !pose.equals(object.getPoseKeyframe()))
          object.applyPoseKeyframe(pose);
        double tol = ModellingApp.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cachedWire = obj.getWireframeMesh();
        if (cachedBounds == null)
          cachedBounds = obj.getBounds();
      }
    lastPreviewWasWireframe = true;
    return cachedWire;
  }
  
  /** Get a bounding box for the object.  The bounding box is defined in the object's local coordinate system. */
  
  public BoundingBox getBounds()
  {
    checkDistortionChanged();
    if (cachedBounds == null)
      {
        if (pose != null && !pose.equals(object.getPoseKeyframe()))
          object.applyPoseKeyframe(pose);
        double tol = ModellingApp.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cachedBounds = obj.getBounds();
        Object3D realObject = object;
        while (realObject instanceof ObjectWrapper)
          realObject = ((ObjectWrapper) realObject).getWrappedObject();
        if (!(realObject instanceof ObjectCollection))
        {
          if (lastPreviewWasWireframe && cachedWire == null)
            cachedWire = obj.getWireframeMesh();
          else if (!lastPreviewWasWireframe && cachedMesh == null)
            cachedMesh = obj.getRenderingMesh(tol, true, this);
        }
      }
    return cachedBounds;
  }
  
  /** Clear the cached preview meshes.  This should be called whenever the object is changed. */
  
  public void clearCachedMeshes()
  {
    cachedMesh = null;
    cachedWire = null;
    cachedBounds = null;
  }

  /** Get the skeleton for this object, or null if it does not have one. */
  
  public Skeleton getSkeleton()
  {
    return object.getSkeleton();
  }
}
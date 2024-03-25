/* Copyright (C) 1999-2013 by Peter Eastman
   Changes copyright (C) 2024 by Maksim Khramov

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

import java.lang.ref.*;
import java.util.*;
import java.util.stream.IntStream;

/** ObjectInfo represents information about an object within a Scene: its position, 
    orientation, name, visibility, etc.  The internal properties (i.e. geometry) of
    the object are defined by the "object" property.
    <p>
    There may be several ObjectInfos in a scene which all reference
    the same Object3D.  In that case, they are live duplicates of each other. */

public class ObjectInfo
{
  public Object3D object;
  public CoordinateSystem coords;
  public String name;
  public boolean visible, selected, parentSelected;
  public ObjectInfo parent, children[];
  private List<Track> tracks = new ArrayList<>();
  public Keyframe pose;
  public int id;
  private boolean locked;
  private Distortion distortion, prevDistortion;
  private SoftReference<RenderingMesh> cachedMesh;
  private SoftReference<WireframeMesh> cachedWire;
  private BoundingBox cachedBounds;
  private boolean lastPreviewWasWireframe;

  /** Create a new ObjectInfo. */

  public ObjectInfo(Object3D obj, CoordinateSystem c, String name)
  {
    setObject(obj);
    setCoords(c);
    this.setName(name);
    setVisible(true);
    children = new ObjectInfo [0];
    setId(-1);
  }
  
  /** Create a new ObjectInfo which is identical to this one.  It will still reference the
      same Object3D object, but all other fields will be cloned. */
  
  public ObjectInfo duplicate()
  {
    return duplicate(getObject());
  }
  
  /** Create a new ObjectInfo which is identical to this one, but references a new Object3D. */
  
  public ObjectInfo duplicate(Object3D obj)
  {
    ObjectInfo info = new ObjectInfo(obj, getCoords().duplicate(), getName());
    
    info.setVisible(isVisible());
    info.setLocked(isLocked());
    info.setId(id);

    IntStream.range(0, tracks.size()).forEach(index -> info.addTrack(tracks.get(index).duplicate(info), index));

    if (distortion != null)
      info.distortion = distortion.duplicate();
    return info;
  }
  
  /** Given an array of ObjectInfos, duplicate all of them (including the objects they
      point to), keeping parent-child relationships intact. */
  
  public static ObjectInfo[] duplicateAll(ObjectInfo[] info)
  {
    ObjectInfo[] newObj = new ObjectInfo [info.length];
    Map<ObjectInfo, ObjectInfo> objectMap = new HashMap<ObjectInfo, ObjectInfo>();
    for (int i = 0; i < newObj.length; i++)
    {
      newObj[i] = info[i].duplicate(info[i].getObject().duplicate());
      objectMap.put(info[i], newObj[i]);
    }
    for (int i = 0; i < info.length; i++)
      for (int k = info[i].getChildren().length-1; k >= 0; k--)
        {
          int j;
          for (j = 0; j < info.length && info[j] != info[i].getChildren()[k]; j++);
          if (j < info.length)
            newObj[i].addChild(newObj[j], 0);
        }
    for (ObjectInfo item: newObj) item.tracks.forEach(track -> track.updateObjectReferences(objectMap));
    return newObj;
  }

  /** Make this ObjectInfo identical to another one.  Both ObjectInfos will reference the
      same Object3D object, but all other fields will be cloned. */
  
  public void copyInfo(ObjectInfo info)
  {
    setObject(info.getObject());
    getCoords().copyCoords(info.getCoords());
    setName(info.name);
    setVisible(info.visible);
    setLocked(info.locked);
    setId(info.id);
    cachedMesh = info.cachedMesh;
    cachedWire = info.cachedWire;
    cachedBounds = info.cachedBounds;

    tracks.clear();
    List<Track> st = info.tracks;
    IntStream.range(0, st.size()).forEach(index -> this.addTrack(st.get(index).duplicate(this), index));

    if (info.distortion == null) 
      distortion = null;
    else 
      distortion = info.distortion.duplicate();
    if (info.prevDistortion == null) 
      prevDistortion = null;
    else 
      prevDistortion = info.prevDistortion.duplicate();
  }
  
  /** Add a child to this object. */
  
  public void addChild(ObjectInfo info, int position)
  {
    ObjectInfo newChildren[] = new ObjectInfo [getChildren().length+1];
    int i;
    
    for (i = 0; i < position; i++)
      newChildren[i] = getChildren()[i];
    newChildren[position] = info;
    for (; i < getChildren().length; i++)
      newChildren[i+1] = getChildren()[i];
    children = newChildren;
    info.setParent(this);
  }
  
  /** Remove a child from this object. */
  
  public void removeChild(ObjectInfo info)
  {
    for (int i = 0; i < getChildren().length; i++)
      if (getChildren()[i] == info)
        {
          removeChild(i);
          return;
        }
  }

  /** Remove a child from this object. */
  
  public void removeChild(int which)
  {
    ObjectInfo newChildren[] = new ObjectInfo [getChildren().length-1];
    int i;
    
    getChildren()[which].setParent(null);
    for (i = 0; i < which; i++)
      newChildren[i] = getChildren()[i];
    for (i++; i < getChildren().length; i++)
      newChildren[i-1] = getChildren()[i];
    children = newChildren;
  }

  /** Add a track to this object. */
  
  public void addTrack(Track tr, int position)
  {
    tracks.add(position, tr);
  }
  
  /** Remove a track from this object. */
  
  public void removeTrack(Track tr)
  {
    tracks.remove(tr);
  }

  /** Remove a track from this object. */

  public void removeTrack(int which)
  {
    tracks.remove(which);
  }
  
  /** Set the texture and texture mapping for this object. */
  
  public void setTexture(Texture tex, TextureMapping map)
  {
    getObject().setTexture(tex, map);
    clearCachedMeshes();
    
    // Update any texture tracks.
    
    if (getTracks() != null)
      for (int i = 0; i < getTracks().length; i++)
        if (getTracks()[i] instanceof TextureTrack)
          ((TextureTrack) getTracks()[i]).parametersChanged();
  }
  
  /** Set the material and material mapping for this object. */
  
  public void setMaterial(Material mat, MaterialMapping map)
  {
    getObject().setMaterial(mat, map);
    clearCachedMeshes();
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

  /** Get the current Distortion applied to this object. */

  public Distortion getDistortion()
  {
    return distortion;
  }

  /** Returns true if a Distortion has been applied to this object. */
  
  public boolean isDistorted()
  {
    return (distortion != null);
  }

  /** Set the current Distortion applied to this object.  Any previously applied Distortion is discarded. */

  public void setDistortion(Distortion d)
  {
    distortion = d;
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
      return getObject();
    Object3D obj = getObject();
    while (obj instanceof ObjectWrapper)
      obj = ((ObjectWrapper) obj).getWrappedObject();
    if (!(obj instanceof Mesh) && getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT)
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
    RenderingMesh cached = null;
    if (cachedMesh != null)
      cached = cachedMesh.get();
    if (cached == null)
      {
        if (getPose() != null && !getPose().equals(getObject().getPoseKeyframe()))
          getObject().applyPoseKeyframe(getPose());
        double tol = ArtOfIllusion.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cached = obj.getRenderingMesh(tol, true, this);
        cachedMesh = new SoftReference<RenderingMesh>(cached);
        if (cachedBounds == null)
          cachedBounds = obj.getBounds();
      }
    lastPreviewWasWireframe = false;
    return cached;
  }
  
  /** Get a wireframe mesh for interactive previews. */
  
  public WireframeMesh getWireframePreview()
  {
    checkDistortionChanged();
    WireframeMesh cached = null;
    if (cachedWire != null)
      cached = cachedWire.get();
    if (cached == null)
      {
        if (getPose() != null && !getPose().equals(getObject().getPoseKeyframe()))
          getObject().applyPoseKeyframe(getPose());
        double tol = ArtOfIllusion.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cached = obj.getWireframeMesh();
        cachedWire = new SoftReference<WireframeMesh>(cached);
        if (cachedBounds == null)
          cachedBounds = obj.getBounds();
      }
    lastPreviewWasWireframe = true;
    return cached;
  }
  
  /** Get a bounding box for the object.  The bounding box is defined in the object's local coordinate system. */
  
  public BoundingBox getBounds()
  {
    checkDistortionChanged();
    if (cachedBounds == null)
      {
        if (getPose() != null && !getPose().equals(getObject().getPoseKeyframe()))
          getObject().applyPoseKeyframe(getPose());
        double tol = ArtOfIllusion.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cachedBounds = obj.getBounds();
        Object3D realObject = getObject();
        while (realObject instanceof ObjectWrapper)
          realObject = ((ObjectWrapper) realObject).getWrappedObject();
        if (!(realObject instanceof ObjectCollection))
        {
          if (lastPreviewWasWireframe && cachedWire == null)
            cachedWire = new SoftReference<WireframeMesh>(obj.getWireframeMesh());
          else if (!lastPreviewWasWireframe && cachedMesh == null)
            cachedMesh = new SoftReference<RenderingMesh>(obj.getRenderingMesh(tol, true, this));
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
    return getObject().getSkeleton();
  }

  /** Get the Object3D defining the geometry for this ObjectInfo. */

  public Object3D getObject()
  {
    return object;
  }

  /** Set the Object3D defining the geometry for this ObjectInfo. */

  public void setObject(Object3D object)
  {
    this.object = object;
  }

  /** Get the CoordinateSystem for this object. */

  public CoordinateSystem getCoords()
  {
    return coords;
  }

  /** Set the CoordinateSystem for this object. */

  public void setCoords(CoordinateSystem coords)
  {
    this.coords = coords;
  }

  /** Get the name of this object. */

  public String getName()
  {
    return name;
  }

  /** Set the name of this object. */

  public void setName(String name)
  {
    this.name = name;
  }

  /** Get whether this object is visible. */

  public boolean isVisible()
  {
    return visible;
  }

  /** Set whether this object is visible. */

  public void setVisible(boolean visible)
  {
    this.visible = visible;
  }

  /** Get whether this object is locked. */

  public boolean isLocked()
  {
    return locked;
  }

  /** Set whether this object is locked. */

  public void setLocked(boolean locked)
  {
    this.locked = locked;
  }

  /** Get this object's parent, or null if it is a top level object. */

  public ObjectInfo getParent()
  {
    return parent;
  }

  /** Set this object's parent. */

  public void setParent(ObjectInfo parent)
  {
    this.parent = parent;
  }

  /** Get the current pose for this object (may be null). */

  public Keyframe getPose()
  {
    return pose;
  }

  /** Set the current pose for this object (may be null). */

  public void setPose(Keyframe pose)
  {
    this.pose = pose;
  }

  /** Get this object's ID. */

  public int getId()
  {
    return id;
  }

  /** Set this object's ID. */

  public void setId(int id)
  {
    this.id = id;
  }

  /** Get the list of children for this object. */

  public ObjectInfo[] getChildren()
  {
    return children;
  }

  /** Get the list of Tracks for this object. */

  public Track[] getTracks()
  {
    return tracks.toArray(new Track[0]);
  }

  /** Set the list of Tracks for this object. */

  public void setTracks(Track[] tracks)
  {
    this.tracks = new ArrayList<>(Arrays.asList(tracks));
  }
}

/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import java.io.*;

/** This class represents a reference either to an object, or to a joint within an
    object. */

public class ObjectRef
{
  private ObjectInfo object;
  private int objectID, jointID;
  private Scene theScene;
  
  /** Create a "null" reference which does not refer to any object. */
  
  public ObjectRef()
  {
    objectID = jointID = -1;
  }
  
  /** Create a reference to an existing object. */
  
  public ObjectRef(ObjectInfo info)
  {
    object = info;
    objectID = info.getId();
    jointID = -1;
  }
  
  /** Create a reference to a joint within an existing object. */
  
  public ObjectRef(ObjectInfo info, Joint j)
  {
    object = info;
    objectID = info.getId();
    jointID = j.id;
  }

  /** Create a reference to an object/joint which may not have been loaded yet. */
  
  public ObjectRef(int objectID, int jointID, Scene sc)
  {
    this.objectID = objectID;
    this.jointID = jointID;
    theScene = sc;
  }
  
  /** Two ObjectRefs are equals if they refer to the same object or joint. */
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof ObjectRef))
      return false;
    ObjectRef ref = (ObjectRef) obj;
    return (ref.objectID == objectID && ref.jointID == jointID);
  }
  
  /** Get the object this reference refers to. */
  
  public ObjectInfo getObject()
  {
    if (object == null)
    {
      if (theScene == null)
        return null;
      for (int i = theScene.getNumObjects()-1; i >= 0; i--)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.getId() != objectID)
          continue;
        object = info;
        break;
      }
    }
    return object;
  }
  
  /** Get the joint this reference refers to, or null if it does not refer to a joint. */
  
  public Joint getJoint()
  {
    if (jointID == -1)
      return null;
    ObjectInfo info = getObject();
    if (info == null)
      return null;
    Skeleton s = info.getObject().getSkeleton();
    if (s != null)
      return s.getJoint(jointID);
    return null;
  }
  
  /** Get the coordinate system for the object/joint this refers to. */
  
  public CoordinateSystem getCoords()
  {
    ObjectInfo info = getObject();
    if (info == null)
      return null;
    if (jointID == -1)
      return info.getCoords();
    Skeleton s = info.getObject().getSkeleton();
    if (s != null)
    {
      Skeleton ds = info.getDistortedObject(ArtOfIllusion.getPreferences().getInteractiveSurfaceError()).getSkeleton();
      if (ds != null)
        s = ds;
    }
    if (s == null)
      return info.getCoords();
    Joint j = s.getJoint(jointID);
    if (j == null)
      return info.getCoords();
    CoordinateSystem coords = j.coords.duplicate();
    coords.transformCoordinates(info.getCoords().fromLocal());
    return coords;
  }

  /** Create an exact duplicate of this object reference. */
  
  public ObjectRef duplicate()
  {
    ObjectRef ref = new ObjectRef();
    ref.objectID = objectID;
    ref.jointID = jointID;
    ref.object = object;
    ref.theScene = theScene;
    return ref;
  }
  
  /** Make this ObjectRef identical to another one. */
  
  public void copy(ObjectRef ref)
  {
    objectID = ref.objectID;
    jointID = ref.jointID;
    object = ref.object;
    theScene = ref.theScene;
  }

  /** Write a serialized representation of this object reference to a stream. */

  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeInt(objectID);
    out.writeInt(jointID);
  }

  /** Construct an object reference from its serialized representation. */
  
  public ObjectRef(DataInputStream in, Scene theScene) throws IOException
  {
    objectID = in.readInt();
    jointID = in.readInt();
    this.theScene = theScene;
  }
  
  /** Get a text string describing the object and joint. */
  
  public String toString()
  {
    ObjectInfo info = getObject();
    if (info == null)
      return "(no object selected)";
    Joint j = getJoint();
    if (j == null)
      return info.getName();
    return info.getName() +" ("+j.name+")";
  }
}
/* Copyright (C) 2001-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.object.*;
import java.io.*;
import java.util.*;

/** This is an abstract class representing an aspect of the scene which changes with time.
    Tracks are typically defined either by a Timecourse or a Procedure. */

public abstract class Track
{
  protected String name;
  protected boolean enabled = true, quantized = true;
  
  protected Track()
  {
  }
  
  public Track(String name)
  {
    this.name = name;
  }
  
  /** Get the name of the track. */
  
  public String getName()
  {
    return name;
  }
  
  /** Set the name of the track. */
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  /** Returns whether the track is currently enabled. */
  
  public boolean isEnabled()
  {
    return enabled;
  }
  
  /** Enable or disable the track. */
  
  public void setEnabled(boolean enable)
  {
    enabled = enable;
  }
  
  /** Returns whether timepoints for the track must lie exactly on a frame. */
  
  public boolean isQuantized()
  {
    return quantized;
  }
  
  /** Set whether timepoints for the track must lie exactly on a frame. */
  
  public void setQuantized(boolean quantize)
  {
    quantized = quantize;
  }
  
  /** Get the names of all graphable values for this track. */
  
  public String [] getValueNames()
  {
    return new String [0];
  }
  
  /** Get the list of graphable values for a particular keyframe. */
  
  public double [] getGraphValues(Keyframe key)
  {
    return new double [0];
  }
  
  /** Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    return new double [0];
  }
  
  /** Get the allowed range for graphable values.  This returns a 2D array, where elements
      [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
      the nth graphable value. */
  
  public double[][] getValueRange()
  {
    return new double [0][0];
  }

  /** This method should present a window in which the user can edit the track. */
  
  public abstract void edit(LayoutWindow win);
  
  /** This method should present a window in which the user can edit the specified keyframe. */
  
  public void editKeyframe(LayoutWindow win, int which)
  {
  }
  
  /** This method should modify whatever aspects of the scene are governed by this track,
      so that they correspond to their values at the specified time. */
  
  public abstract void apply(double time);
  
  /** Create a duplicate of this track (possibly for another object and/or parent track). */
  
  public abstract Track duplicate(Object parent);
  
  /** Make this track identical to another one. */
  
  public abstract void copy(Track tr);
  
  /** Get a list of all keyframe times for this track. */
  
  public abstract double [] getKeyTimes();
  
  /** Get the timecourse describing this track, or null if it is not described by
      a timecourse. */
  
  public Timecourse getTimecourse()
  {
    return null;
  }
  
  /** Get the smoothing method for this track. */
  
  public int getSmoothingMethod()
  {
    return Timecourse.DISCONTINUOUS;
  }

  /** Set a keyframe at the specified time. */
  
  public void setKeyframe(double time, Keyframe k, Smoothness s)
  {
  }
  
  /** Set a keyframe at the specified time, based on the current state of the Scene. */
  
  public Keyframe setKeyframe(double time, Scene sc)
  {
    return null;
  }
  
  /** Set a keyframe at the specified time, based on the current state of the Scene,
      if and only if the Scene does not match the current state of the track.  Return
      the new keyframe, or null if none was set. */
  
  public Keyframe setKeyframeIfModified(double time, Scene sc)
  {
    return null;
  }
  
  /** Move a keyframe to a new time, and return its new position in the list. */
  
  public abstract int moveKeyframe(int which, double time);
  
  /** Delete the specified keyframe. */
  
  public abstract void deleteKeyframe(int which);
  
  /** A null track is one which has no affect on the scene.  This usually means that no
      keyframes have been added to it. */
  
  public abstract boolean isNullTrack();
  
  /** Get any child tracks of this track. */
  
  public Track [] getSubtracks()
  {
    return new Track [0];
  }
  
  /** Determine whether this track can be added as a child of an object. */
  
  public boolean canAcceptAsParent(Object obj)
  {
    return false;
  }
  
  /** Get the parent object of this track. */
  
  public Object getParent()
  {
    return null;
  }
  
  /** Set the parent object of this track. */
  
  public void setParent(Object obj)
  {
  }
  
  /** Get an array of any objects which this track depends on (and which therefore must
      be updated before this track is applied). */ 
  
  public ObjectInfo [] getDependencies()
  {
    return new ObjectInfo [0];
  }
  
  /** Delete all references to the specified object from this track.  This is used when an
      object is deleted from the scene. */
  
  public void deleteDependencies(ObjectInfo obj)
  {
  }

  /**
   * Update any references to objects this track depends on.  Any reference to an object found as a key
   * in the map should be replaced with the corresponding object.  This is used, for example, when copying
   * and pasting objects between scenes.
   */

  public void updateObjectReferences(Map<ObjectInfo, ObjectInfo> objectMap)
  {
  }
  
  /** Write a serialized representation of this track to a stream. */
  
  public abstract void writeToStream(DataOutputStream out, Scene scene) throws IOException;
  
  /** Initialize this tracked based on its serialized representation as written by writeToStream(). */
  
  public abstract void initFromStream(DataInputStream in, Scene scene) throws IOException, InvalidObjectException;
}
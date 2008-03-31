/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.animation.distortion.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;
import java.lang.reflect.*;

/** This is a Track which controls the pose of an object. */

public class PoseTrack extends Track
{
  private ObjectInfo info;
  private Timecourse tc;
  private int smoothingMethod;
  private boolean relative;
  private Track subtracks[];
  private WeightTrack theWeight;
  private String valueName[];
  private double defaultValue[], valueRange[][];
  
  public PoseTrack(ObjectInfo info)
  {
    super("Pose");
    this.info = info;
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
    smoothingMethod = Timecourse.INTERPOLATING;
    theWeight = new WeightTrack(this);
    subtracks = new Track [] {theWeight};
    info.getObject().configurePoseTrack(this);
  }
  
  /** Modify the pose of the object. */
  
  public void apply(double time)
  {
    Keyframe pose = tc.evaluate(time, smoothingMethod);

    if (pose != null)
    {
      if (info.isDistorted())
      {
        Actor actor = Actor.getActor(info.getObject());
        info.addDistortion(new PoseDistortion(theWeight.getWeight(time), pose, actor, relative));
      }
      else if (info.getPose() == null)
        info.setPose(pose);
      else
      {
        double weight = theWeight.getWeight(time);
        if (relative)
          info.setPose(info.pose.blend(pose, 1.0, weight));
        else
          info.setPose(info.pose.blend(pose, 1.0-weight, weight));
      }
    }
    for (int i = 0; i < subtracks.length; i++)
      subtracks[i].apply(time);
  }
  
  /** Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    PoseTrack t = new PoseTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.smoothingMethod = smoothingMethod;
    t.tc = tc.duplicate((ObjectInfo) obj);
    t.relative = relative;
    t.theWeight = (WeightTrack) theWeight.duplicate(t);
    t.subtracks = new Track [] {theWeight};
    return t;
  }
  
  /** Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    PoseTrack t = (PoseTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    smoothingMethod = t.smoothingMethod;
    tc = t.tc.duplicate(info);
    relative = t.relative;
    theWeight = (WeightTrack) t.theWeight.duplicate(this);
    subtracks = new Track [] {theWeight};
  }
  
  /** Get a list of all keyframe times for this track. */
  
  public double [] getKeyTimes()
  {
    return tc.getTimes();
  }

  /** Get the timecourse describing this track. */
  
  public Timecourse getTimecourse()
  {
    return tc;
  }
  
  /** Set a keyframe at the specified time. */
  
  public void setKeyframe(double time, Keyframe k, Smoothness s)
  {
    tc.addTimepoint(k, time, s);
  }
  
  /** Set a keyframe at the specified time, based on the current state of the Scene. */
  
  public Keyframe setKeyframe(double time, Scene sc)
  {
    Keyframe pose = info.getObject().getPoseKeyframe();
    
    tc.addTimepoint(pose, time, new Smoothness());
    return pose;
  }

  /** Set a keyframe at the specified time, based on the current state of the Scene,
      if and only if the Scene does not match the current state of the track.  Return
      the new Keyframe, or null if none was set. */
  
  public Keyframe setKeyframeIfModified(double time, Scene sc)
  {
    for (int i = 0; i < subtracks.length; i++)
      subtracks[i].setKeyframeIfModified(time, sc);
    if (tc.getTimes().length == 0)
      return setKeyframe(time, sc);
    Keyframe pose1 = tc.evaluate(time, smoothingMethod);
    Keyframe pose2 = info.getPose() == null ? info.getObject().getPoseKeyframe() : info.getPose();
    if (!pose1.equals(pose2))
      return setKeyframe(time, sc);
    return null;
  }

  /** Move a keyframe to a new time, and return its new position in the list. */
  
  public int moveKeyframe(int which, double time)
  {
    return tc.moveTimepoint(which, time);
  }
  
  /** Delete the specified keyframe. */
  
  public void deleteKeyframe(int which)
  {
    tc.removeTimepoint(which);
  }
  
  /** This track is null if it has no keyframes. */
  
  public boolean isNullTrack()
  {
    return (tc.getTimes().length == 0);
  }
  
  /** This has a single child track. */
  
  public Track [] getSubtracks()
  {
    return subtracks;
  }

  /** Determine whether this track can be added as a child of an object. */
  
  public boolean canAcceptAsParent(Object obj)
  {
    return (obj instanceof ObjectInfo && ((ObjectInfo) obj).getObject() == info.getObject());
  }
  
  /** Get the parent object of this track. */
  
  public Object getParent()
  {
    return info;
  }
  
  /** Set the parent object of this track. */
  
  public void setParent(Object obj)
  {
    info = (ObjectInfo) obj;
  }
  
  /** Get the smoothing method for this track. */
  
  public int getSmoothingMethod()
  {
    return smoothingMethod;
  }
  
  /** Set the smoothing method for this track. */
  
  public void setSmoothingMethod(int method)
  {
    smoothingMethod = method;
  }
  
  /** Determine whether this track is in absolute or relative mode. */
  
  public boolean isRelative()
  {
    return relative;
  }
  
  /** Set whether this track is in absolute or relative mode. */
  
  public void setRelative(boolean rel)
  {
    relative = rel;
  }
  
  /** Get the names of all graphable values for this track. */
  
  public String [] getValueNames()
  {
    return valueName;
  }

  /** Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    return defaultValue;
  }
  
  /** Get the allowed range for graphable values.  This returns a 2D array, where elements
     [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
     the nth graphable value. */
  
  public double[][] getValueRange()
  {
    return valueRange;
  }
  
  /** Set the list of graphable values for this track.  Usually, this will only be called
      by its parent object.
      @param names     the names of the graphable values
      @param defaults  the default values of the graphable values
      @param ranges    specifies the allowed range of each graphable values.  elements [n][0]
                       and [n][1] are the minimum and maximum allowed values, respectively, for
                       the nth graphable value.
  */
  
  public void setGraphableValues(String names[], double defaults[], double ranges[][])
  {
    valueName = names;
    defaultValue = defaults;
    valueRange = ranges;
  }
  
  /** Set the list of subtracks (other than the weight track) for this track.  Usually, this will only
      be called by its parent object. */
  
  public void setSubtracks(Track extraSubtracks[])
  {
    subtracks = new Track [extraSubtracks.length+1];
    subtracks[0] = theWeight;
    System.arraycopy(extraSubtracks, 0, subtracks, 1, extraSubtracks.length);
  }

  /** Write a serialized representation of this track to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene sc) throws IOException
  {
    double t[] = tc.getTimes();
    Smoothness s[] = tc.getSmoothness();
    Keyframe v[] = tc.getValues();

    out.writeShort(2); // Version number
    out.writeUTF(name);
    out.writeBoolean(enabled);
    out.writeInt(smoothingMethod);
    out.writeBoolean(relative);
    out.writeInt(t.length);
    if (t.length > 0)
      out.writeUTF(v[0].getClass().getName());
    for (int i = 0; i < t.length; i++)
      {
        out.writeDouble(t[i]);
        v[i].writeToStream(out); 
        s[i].writeToStream(out); 
      }
    for (int i = 0; i < subtracks.length; i++)
      subtracks[i].writeToStream(out, sc);
  }
  
  /** Initialize this tracked based on its serialized representation as written by writeToStream(). */
  
  public void initFromStream(DataInputStream in, Scene scene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    if (version < 0 || version > 2)
      throw new InvalidObjectException("");
    name = in.readUTF();
    enabled = in.readBoolean();
    smoothingMethod = in.readInt();
    relative = (version > 0 ? in.readBoolean() : false);
    int keys = in.readInt();
    double t[] = new double [keys];
    Smoothness s[] = new Smoothness [keys];
    Keyframe v[] = new Keyframe [keys];
    if (keys > 0)
      {
        try
          {
            Class cl = ArtOfIllusion.getClass(in.readUTF());
            Constructor con = cl.getConstructor(new Class [] {DataInputStream.class, Object.class});
            for (int i = 0; i < keys; i++)
              {
                t[i] = in.readDouble();
                v[i] = (Keyframe) con.newInstance(new Object [] {in, info});
                s[i] = new Smoothness(in);
              }
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new InvalidObjectException("");
          }
      }
    tc = new Timecourse(v, t, s);
    info.getObject().configurePoseTrack(this);
    if (version == 0)
      theWeight.initFromStream(in, scene);
    else
      for (int i = 0; i < subtracks.length; i++)
        subtracks[i].initFromStream(in, scene);
  }

  /** Present a window in which the user can edit the specified keyframe. */
  
  public void editKeyframe(LayoutWindow win, int which)
  {
    info.getObject().editKeyframe(win, tc.getValues()[which], info);
  }

  /** This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    BTextField nameField = new BTextField(PoseTrack.this.getName());
    BComboBox smoothChoice = new BComboBox(new String [] {
      Translate.text("Discontinuous"),
      Translate.text("Linear"),
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    smoothChoice.setSelectedIndex(smoothingMethod);
    BComboBox modeChoice = new BComboBox(new String [] {
      Translate.text("Absolute"),
      Translate.text("Relative")
    });
    modeChoice.setSelectedIndex(relative ? 1 : 0);
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("poseTrackTitle"), new Widget []
        {nameField, smoothChoice, modeChoice}, new String [] {Translate.text("trackName"), Translate.text("SmoothingMethod"), Translate.text("trackMode")});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(info)}));
    this.setName(nameField.getText());
    smoothingMethod = smoothChoice.getSelectedIndex();
    relative = (modeChoice.getSelectedIndex() == 1);
  }
}
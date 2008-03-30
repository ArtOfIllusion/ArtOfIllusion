/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;

/** This is a Track which controls whether an object is visible. */

public class VisibilityTrack extends Track
{
  ObjectInfo info;
  Timecourse tc;
    
  public VisibilityTrack(ObjectInfo info)
  {
    super("Visibility");
    this.info = info;
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
  }
  
  /* Modify the position of the object. */
  
  public void apply(double time)
  {
    BooleanKeyframe v = (BooleanKeyframe) tc.evaluate(time, tc.LINEAR);

    if (v == null)
      return;
    info.setVisible(v.val);
  }
  
  /* Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    VisibilityTrack t = new VisibilityTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.tc = tc.duplicate((ObjectInfo) obj);
    return t;
  }
  
  /* Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    VisibilityTrack t = (VisibilityTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    tc = t.tc.duplicate(info);
  }
  
  /* Get a list of all keyframe times for this track. */
  
  public double [] getKeyTimes()
  {
    return tc.getTimes();
  }
  
  /* Get the timecourse describing this track. */
  
  public Timecourse getTimecourse()
  {
    return tc;
  }
  
  /* Set a keyframe at the specified time. */
  
  public void setKeyframe(double time, Keyframe k, Smoothness s)
  {
    tc.addTimepoint(k, time, s);
  }
  
  /* Set a keyframe at the specified time, based on the current state of the Scene. */
  
  public Keyframe setKeyframe(double time, Scene sc)
  {
    Keyframe k = new BooleanKeyframe(info.isVisible());
    tc.addTimepoint(k, time, new Smoothness());
    return k;
  }

  /** Set a keyframe at the specified time, based on the current state of the Scene,
      if and only if the Scene does not match the current state of the track.  Return
      the new Keyframe, or null if none was set. */
  
  public Keyframe setKeyframeIfModified(double time, Scene sc)
  {
    if (tc.getTimes().length == 0)
      return setKeyframe(time, sc);
    BooleanKeyframe v = (BooleanKeyframe) tc.evaluate(time, tc.LINEAR);
    if (v.val == info.isVisible())
      return null;
    return setKeyframe(time, sc);
  }

  /* Move a keyframe to a new time, and return its new position in the list. */
  
  public int moveKeyframe(int which, double time)
  {
    return tc.moveTimepoint(which, time);
  }
  
  /* Delete the specified keyframe. */
  
  public void deleteKeyframe(int which)
  {
    tc.removeTimepoint(which);
  }
  
  /* This track is null if it has no keyframes. */
  
  public boolean isNullTrack()
  {
    return (tc.getTimes().length == 0);
  }

  /* Determine whether this track can be added as a child of an object. */
  
  public boolean canAcceptAsParent(Object obj)
  {
    return (obj instanceof ObjectInfo);
  }
  
  /* Get the parent object of this track. */
  
  public Object getParent()
  {
    return info;
  }
  
  /* Set the parent object of this track. */
  
  public void setParent(Object obj)
  {
    info = (ObjectInfo) obj;
  }
  
  /* Get the names of all graphable values for this track. */
  
  public String [] getValueNames()
  {
    return new String [] {"Visible"};
  }

  /* Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    return new double [] {1.0};
  }
  
  /* Get the allowed range for graphable values.  This returns a 2D array, where elements
     [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
     the nth graphable value. */
  
  public double[][] getValueRange()
  {
    return new double [][] {{0.0, 1.0}};
  }

  /* Write a serialized representation of this track to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene scene) throws IOException
  {
    double t[] = tc.getTimes();
    Keyframe v[] = tc.getValues();

    out.writeShort(0); // Version number
    out.writeUTF(name);
    out.writeBoolean(enabled);
    out.writeInt(t.length);
    for (int i = 0; i < t.length; i++)
      {
        out.writeDouble(t[i]);
        v[i].writeToStream(out);
      }
  }
  
  /** Initialize this tracked based on its serialized representation as written by writeToStream(). */
  
  public void initFromStream(DataInputStream in, Scene scene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    name = in.readUTF();
    enabled = in.readBoolean();
    int keys = in.readInt();
    double t[] = new double [keys];
    Smoothness s[] = new Smoothness [keys];
    Keyframe v[] = new Keyframe [keys];
    for (int i = 0; i < keys; i++)
      {
        t[i] = in.readDouble();
        v[i] = new BooleanKeyframe(in, info);
        s[i] = new Smoothness();
      }
    tc = new Timecourse(v, t, s);
  }

  /* Present a window in which the user can edit the specified keyframe. */
  
  public void editKeyframe(LayoutWindow win, int which)
  {
    BooleanKeyframe key = (BooleanKeyframe) tc.getValues()[which];
    double time = tc.getTimes()[which];
    BCheckBox visibleBox = new BCheckBox(Translate.text("Visible"), key.val);
    ValueField timeField = new ValueField(time, ValueField.NONE, 5);
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("editKeyframe"), new Widget []
      {visibleBox, timeField}, new String [] {null, Translate.text("Time")});

    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(info)}));
    key.val = visibleBox.getState();
    moveKeyframe(which, timeField.getValue());
  }

  /* This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    BTextField nameField = new BTextField(VisibilityTrack.this.getName());
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("visibilityTrackTitle"), new Widget []
        {nameField}, new String [] {Translate.text("trackName")});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(info)}));
    this.setName(nameField.getText());
  }
}
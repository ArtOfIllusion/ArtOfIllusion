/* Copyright (C) 2002-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** This is a Track which applies a TwistDistortion to an object. */

public class TwistTrack extends Track
{
  ObjectInfo info;
  Timecourse tc;
  int axis;
  int smoothingMethod;
  WeightTrack theWeight;
  boolean worldCoords, forward;
  
  public TwistTrack(ObjectInfo info)
  {
    super("Twist");
    this.info = info;
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
    smoothingMethod = Timecourse.INTERPOLATING;
    theWeight = new WeightTrack(this);
    axis = TwistDistortion.Y_AXIS;
    forward = true;
    worldCoords = false;
  }
  
  /** Modify the scale of the object. */
  
  @Override
  public void apply(double time)
  {
    ScalarKeyframe angle = (ScalarKeyframe) tc.evaluate(time, smoothingMethod);
    if (angle == null || angle.val == 0.0)
      return;
    double weight = theWeight.getWeight(time);
    if (weight == 0.0)
      return;
    if (worldCoords)
      info.addDistortion(new TwistDistortion(axis, angle.val*weight, forward, info.getCoords().fromLocal(), info.getCoords().toLocal()));
    else
      info.addDistortion(new TwistDistortion(axis, angle.val*weight, forward, null, null));
  }
  
  /** Create a duplicate of this track. */
  
  @Override
  public Track duplicate(Object obj)
  {
    TwistTrack t = new TwistTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.axis = axis;
    t.forward = forward;
    t.smoothingMethod = smoothingMethod;
    t.worldCoords = worldCoords;
    t.tc = tc.duplicate((ObjectInfo) obj);
    t.theWeight = (WeightTrack) theWeight.duplicate(t);
    return t;
  }
  
  /** Make this track identical to another one. */
  
  @Override
  public void copy(Track tr)
  {
    TwistTrack t = (TwistTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    axis = t.axis;
    forward = t.forward;
    smoothingMethod = t.smoothingMethod;
    worldCoords = t.worldCoords;
    tc = t.tc.duplicate(info);
    theWeight = (WeightTrack) t.theWeight.duplicate(this);
  }
  
  /** Get a list of all keyframe times for this track. */
  
  @Override
  public double [] getKeyTimes()
  {
    return tc.getTimes();
  }

  /** Get the timecourse describing this track. */
  
  @Override
  public Timecourse getTimecourse()
  {
    return tc;
  }
  
  /** Set a keyframe at the specified time. */
  
  @Override
  public void setKeyframe(double time, Keyframe k, Smoothness s)
  {
    tc.addTimepoint(k, time, s);
  }
  
  /** Set a keyframe at the specified time, based on the current state of the Scene. */
  
  @Override
  public Keyframe setKeyframe(double time, Scene sc)
  {
    Keyframe k = tc.evaluate(time, smoothingMethod);
    if (k == null)
      k = new ScalarKeyframe(0.0);
    else
      k = k.duplicate();
    tc.addTimepoint(k, time, new Smoothness());
    return k;
  }

  /** Move a keyframe to a new time, and return its new position in the list. */
  
  @Override
  public int moveKeyframe(int which, double time)
  {
    return tc.moveTimepoint(which, time);
  }
  
  /** Delete the specified keyframe. */
  
  @Override
  public void deleteKeyframe(int which)
  {
    tc.removeTimepoint(which);
  }
  
  /** This track is null if it has no keyframes. */
  
  @Override
  public boolean isNullTrack()
  {
    return (tc.getTimes().length == 0);
  }
  
  /** This has a single child track. */
  
  @Override
  public Track [] getSubtracks()
  {
    return new Track [] {theWeight};
  }

  /** Determine whether this track can be added as a child of an object. */
  
  @Override
  public boolean canAcceptAsParent(Object obj)
  {
    return (obj instanceof ObjectInfo);
  }
  
  /** Get the parent object of this track. */
  
  @Override
  public Object getParent()
  {
    return info;
  }
  
  /** Set the parent object of this track. */
  
  @Override
  public void setParent(Object obj)
  {
    info = (ObjectInfo) obj;
  }
  
  /** Get the smoothing method for this track. */
  
  @Override
  public int getSmoothingMethod()
  {
    return smoothingMethod;
  }
  
  /** Set the smoothing method for this track. */
  
  public void setSmoothingMethod(int method)
  {
    smoothingMethod = method;
  }
  
  /** Get the names of all graphable values for this track. */
  
  @Override
  public String [] getValueNames()
  {
    return new String [] {"Twist Angle"};
  }

  /** Get the default list of graphable values (for a track which has no keyframes). */
  
  @Override
  public double [] getDefaultGraphValues()
  {
    return new double [] {0.0};
  }
  
  /** Get the allowed range for graphable values.  This returns a 2D array, where elements
      [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
      the nth graphable value. */
  
  @Override
  public double[][] getValueRange()
  {
    return new double [][] {{-Double.MAX_VALUE, Double.MAX_VALUE}};
  }
  
  /** Get an array of any objects which this track depends on (and which therefore must
      be updated before this track is applied). */ 
  
  @Override
  public ObjectInfo [] getDependencies()
  {
     return new ObjectInfo [0];
  }
  
  /** Delete all references to the specified object from this track.  This is used when an
      object is deleted from the scene. */
  
  @Override
  public void deleteDependencies(ObjectInfo obj)
  {
  }

  /** Write a serialized representation of this track to a stream. */
  
  @Override
  public void writeToStream(DataOutputStream out, Scene scene) throws IOException
  {
    double t[] = tc.getTimes();
    Smoothness s[] = tc.getSmoothness();
    Keyframe v[] = tc.getValues();

    out.writeShort(0); // Version number
    out.writeUTF(name);
    out.writeBoolean(enabled);
    out.writeInt(smoothingMethod);
    out.writeInt(axis);
    out.writeBoolean(forward);
    out.writeInt(t.length);
    for (int i = 0; i < t.length; i++)
      {
        out.writeDouble(t[i]);
        ((ScalarKeyframe) v[i]).writeToStream(out); 
        s[i].writeToStream(out); 
      }
    theWeight.writeToStream(out, scene);
  }
  
  /** Initialize this tracked based on its serialized representation as written by writeToStream(). */
  
  @Override
  public void initFromStream(DataInputStream in, Scene scene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    name = in.readUTF();
    enabled = in.readBoolean();
    smoothingMethod = in.readInt();
    axis = in.readInt();
    forward = in.readBoolean();
    int keys = in.readInt();
    double t[] = new double [keys];
    Smoothness s[] = new Smoothness [keys];
    Keyframe v[] = new Keyframe [keys];
    for (int i = 0; i < keys; i++)
      {
        t[i] = in.readDouble();
        v[i] = new ScalarKeyframe(in, info);
        s[i] = new Smoothness(in);
      }
    tc = new Timecourse(v, t, s);
    theWeight.initFromStream(in, scene);
  }

  /** Present a window in which the user can edit the specified keyframe. */
  
  @Override
  public void editKeyframe(LayoutWindow win, int which)
  {
    ScalarKeyframe key = (ScalarKeyframe) tc.getValues()[which];
    Smoothness s = tc.getSmoothness()[which];
    double time = tc.getTimes()[which];
    ValueField angleField = new ValueField(key.val, ValueField.NONE, 5);
    ValueField timeField = new ValueField(time, ValueField.NONE, 5);
    ValueSlider s1Slider = new ValueSlider(0.0, 1.0, 100, s.getLeftSmoothness());
    final ValueSlider s2Slider = new ValueSlider(0.0, 1.0, 100, s.getRightSmoothness());
    final BCheckBox sameBox = new BCheckBox(Translate.text("separateSmoothness"), !s.isForceSame());

    sameBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        s2Slider.setEnabled(sameBox.getState());
      }
    });
    s2Slider.setEnabled(sameBox.getState());
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("editKeyframe"), new Widget []
        {angleField, timeField, sameBox, new BLabel(Translate.text("Smoothness")+':'), s1Slider, s2Slider},
        new String [] {Translate.text("twistAngle"), Translate.text("Time"), null, null, "("+Translate.text("left")+")", "("+Translate.text("right")+")"});    
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(info)}));
    key.val = angleField.getValue();
    if (sameBox.getState())
      s.setSmoothness(s1Slider.getValue(), s2Slider.getValue());
    else
      s.setSmoothness(s1Slider.getValue());
    moveKeyframe(which, timeField.getValue());
  }

  /** This method presents a window in which the user can edit the track. */
  
  @Override
  public void edit(LayoutWindow win)
  {
    final BComboBox smoothChoice, axisChoice, coordsChoice;
    BCheckBox reverseBox = new BCheckBox(Translate.text("reverseTwistDirection"), !forward);
    BTextField nameField = new BTextField(getName());
    smoothChoice = new BComboBox(new String [] {
      Translate.text("Discontinuous"),
      Translate.text("Linear"),
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    smoothChoice.setSelectedIndex(smoothingMethod);
    axisChoice = new BComboBox(new String [] {"X", "Y", "Z"});
    axisChoice.setSelectedIndex(axis);
    coordsChoice = new BComboBox(new String [] {
      Translate.text("Local"),
      Translate.text("World")
    });
    coordsChoice.setSelectedIndex(worldCoords ? 1 : 0);
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("twistTrackTitle"), new Widget []
        {nameField, smoothChoice, axisChoice, reverseBox, coordsChoice}, new String []
        {Translate.text("trackName"), Translate.text("SmoothingMethod"), Translate.text("twistAxis"), "", Translate.text("CoordinateSystem")});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()}));
    this.setName(nameField.getText());
    smoothingMethod = smoothChoice.getSelectedIndex();
    axis = axisChoice.getSelectedIndex();
    forward = !reverseBox.getState();
    worldCoords = (coordsChoice.getSelectedIndex() == 1);
  }
}
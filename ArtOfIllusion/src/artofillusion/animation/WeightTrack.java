/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** This is a Track which controls the weight given to another track. */

public class WeightTrack extends Track
{
  Track parent;
  Timecourse tc;
  int smoothingMethod;
  
  public WeightTrack(Track parent)
  {
    super("Weight");
    this.parent = parent;
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
    smoothingMethod = Timecourse.LINEAR;
  }

  /* Get the weight at a particular time. */
  
  public double getWeight(double time)
  {
    if (!enabled)
      return 1.0;
    ScalarKeyframe w = (ScalarKeyframe) tc.evaluate(time, smoothingMethod);
    if (w == null)
      return 1.0;
    if (w.val > 1.0)
      return 1.0;
    if (w.val < 0.0)
      return 0.0;
    return w.val;
  }

  /* Weight tracks do not directly modify the scene. */
  
  public void apply(double time)
  {
  }
  
  /* Create a duplicate of this track. */
  
  public Track duplicate(Object parent)
  {
    WeightTrack t = new WeightTrack((Track) parent);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.smoothingMethod = smoothingMethod;
    t.tc = tc.duplicate(null);
    return t;
  }
  
  /* Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    WeightTrack t = (WeightTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    smoothingMethod = t.smoothingMethod;
    tc = t.tc.duplicate(null);
  }
  
  /* Get the timecourse for this track. */
  
  public Timecourse getTimecourse()
  {
    return tc;
  }
  
  /* Set the timecourse for this track. */
  
  public void setTimecourse(Timecourse t)
  {
    tc = t;
  }
  
  /* Get the smoothing method for this track. */
  
  public int getSmoothingMethod()
  {
    return smoothingMethod;
  }
  
  /* Set the smoothing method for this track. */
  
  public void setSmoothingMethod(int method)
  {
    smoothingMethod = method;
  }
  
  /* Get a list of all keyframe times for this track. */
  
  public double [] getKeyTimes()
  {
    return tc.getTimes();
  }

  /* Set a keyframe at the specified time. */
  
  public void setKeyframe(double time, Keyframe k, Smoothness s)
  {
    tc.addTimepoint(k, time, s);
  }  
  
  /* Set a keyframe at the specified time, based on the current state of the Scene. */
  
  public Keyframe setKeyframe(double time, Scene sc)
  {
    Keyframe k = new ScalarKeyframe(1.0);
    tc.addTimepoint(k, time, new Smoothness());
    return k;
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

  /* Weight tracks never directly affect the scene. */
  
  public boolean isNullTrack()
  {
    return true;
  }
  
  /* Get the parent object of this track. */
  
  public Object getParent()
  {
    return parent;
  }
  
  /* Get the names of all graphable values for this track. */
  
  public String [] getValueNames()
  {
    return new String [] {"Weight"};
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
    Smoothness s[] = tc.getSmoothness();
    Keyframe v[] = tc.getValues();

    out.writeShort(0); // Version number
    out.writeUTF(name);
    out.writeBoolean(enabled);
    out.writeInt(smoothingMethod);
    out.writeInt(t.length);
    for (int i = 0; i < t.length; i++)
      {
        out.writeDouble(t[i]);
        v[i].writeToStream(out);
        s[i].writeToStream(out); 
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
    smoothingMethod = in.readInt();
    int keys = in.readInt();
    double t[] = new double [keys];
    Smoothness s[] = new Smoothness [keys];
    Keyframe v[] = new Keyframe [keys];
    for (int i = 0; i < keys; i++)
      {
        t[i] = in.readDouble();
        v[i] = new ScalarKeyframe(in, parent);
        s[i] = new Smoothness(in);
      }
    tc = new Timecourse(v, t, s);
  }

  /* Present a window in which the user can edit the specified keyframe. */
  
  public void editKeyframe(LayoutWindow win, int which)
  {
    ScalarKeyframe key = (ScalarKeyframe) tc.getValues()[which];
    Smoothness s = tc.getSmoothness()[which];
    double time = tc.getTimes()[which];
    ValueSlider weightSlider = new ValueSlider(0.0, 1.0, 100, key.val);
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
        {weightSlider, timeField, sameBox, new BLabel(Translate.text("Smoothness")+':'), s1Slider, s2Slider},
        new String [] {Translate.text("Weight"), Translate.text("Time"), null, null, "("+Translate.text("left")+")", "("+Translate.text("right")+")"});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(parent)}));
    key.val = weightSlider.getValue();
    if (sameBox.getState())
      s.setSmoothness(s1Slider.getValue(), s2Slider.getValue());
    else
      s.setSmoothness(s1Slider.getValue());
    moveKeyframe(which, timeField.getValue());
  }

  /* This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    BTextField nameField = new BTextField(getName());
    BComboBox smoothChoice = new BComboBox(new String [] {
      Translate.text("Discontinuous"),
      Translate.text("Linear"),
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    smoothChoice.setSelectedIndex(smoothingMethod);
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("weightTrackTitle"), new Widget []
        {nameField, smoothChoice}, new String []
        {Translate.text("trackName"), Translate.text("SmoothingMethod")});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(parent)}));
    this.setName(nameField.getText());
    smoothingMethod = smoothChoice.getSelectedIndex();
  }
}
/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.image.filter.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** This is a Track which allows the parameters of an ImageFilter to be keyframed. */

public class FilterParameterTrack extends Track
{
  private Object parent;
  private ImageFilter filter;
  private Timecourse tc;
  private int smoothingMethod;
  
  /** Create a new FilterParameterTrack. */
  
  public FilterParameterTrack(Object parent, ImageFilter filter)
  {
    super(filter.getName());
    this.parent = parent;
    this.filter = filter;
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
    smoothingMethod = Timecourse.INTERPOLATING;
  }
  
  /** Get the filter corresponding to this track. */
  
  public ImageFilter getFilter()
  {
    return filter;
  }
  
  /** Modify the pose of the object. */
  
  public void apply(double time)
  {
    ArrayKeyframe k = (ArrayKeyframe) tc.evaluate(time, smoothingMethod);

    if (k == null)
      return;
    for (int i = 0; i < k.val.length; i++)
      filter.setParameterValue(i, k.val[i]);
  }
  
  /** Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    FilterParameterTrack t = new FilterParameterTrack(obj, filter);
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.smoothingMethod = smoothingMethod;
    t.tc = tc.duplicate(filter);
    return t;
  }
  
  /** Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    FilterParameterTrack t = (FilterParameterTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    smoothingMethod = t.smoothingMethod;
    tc = t.tc.duplicate(filter);
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
    double val[] = filter.getParameterValues();
    double key[] = new double [val.length];
    for (int i = 0; i < val.length; i++)
      key[i] = val[i];
    Keyframe k = new ArrayKeyframe(key);
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
    double val1[] = ((ArrayKeyframe) tc.evaluate(time, smoothingMethod)).val;
    double val2[] = filter.getParameterValues();
    for (int i = 0; i < val1.length; i++)
      if (val1[i] != val2[i])
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
  
  /** This has no child tracks. */
  
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
    return parent;
  }
  
  /** Set the parent object of this track. */
  
  public void setParent(Object obj)
  {
    parent = obj;
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
  
  /** Get the names of all graphable values for this track. */
  
  public String [] getValueNames()
  {
    TextureParameter param[] = filter.getParameters();
    String name[] = new String [param.length];
    for (int i = 0; i < param.length; i++)
      name[i] = param[i].name;
    return name;
  }

  /** Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    return filter.getParameterValues();
  }
  
  /** Get the allowed range for graphable values.  This returns a 2D array, where elements
      [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
      the nth graphable value. */
  
  public double[][] getValueRange()
  {
    TextureParameter param[] = filter.getParameters();
    double range[][] = new double [param.length][];
    for (int i = 0; i < param.length; i++)
      range[i] = new double [] {param[i].minVal, param[i].maxVal};
    return range;
  }

  /** Write a serialized representation of this track to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene sc) throws IOException
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
        v[i] = new ArrayKeyframe(in, filter);
        s[i] = new Smoothness(in);
      }
    tc = new Timecourse(v, t, s);
  }
  
  /** Present a window in which the user can edit the specified keyframe. */
  
  public void editKeyframe(LayoutWindow win, int which)
  {
    ArrayKeyframe key = (ArrayKeyframe) tc.getValues()[which];
    Smoothness s = tc.getSmoothness()[which];
    double time = tc.getTimes()[which];
    ValueField timeField = new ValueField(time, ValueField.NONE, 5);
    ValueSlider s1Slider = new ValueSlider(0.0, 1.0, 100, s.getLeftSmoothness());
    final ValueSlider s2Slider = new ValueSlider(0.0, 1.0, 100, s.getRightSmoothness());
    final BCheckBox sameBox = new BCheckBox(Translate.text("separateSmoothness"), !s.isForceSame());
    TextureParameter parameter[] = filter.getParameters();
    Widget widget[] = new Widget [parameter.length+5];
    String label[] = new String [parameter.length+5];

    for (int i = 0; i < parameter.length; i++)
    {
      widget[i] = parameter[i].getEditingWidget(key.val[i]);
      label[i] = parameter[i].name;
    }
    sameBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        s2Slider.setEnabled(sameBox.getState());
      }
    });
    s2Slider.setEnabled(sameBox.getState());
    int n = parameter.length;
    widget[n] = timeField;
    widget[n+1] = sameBox;
    widget[n+2] = new BLabel(Translate.text("Smoothness")+':');
    widget[n+3] = s1Slider;
    widget[n+4] = s2Slider;
    label[n] = Translate.text("Time");
    label[n+3] = "("+Translate.text("left")+")";
    label[n+4] = "("+Translate.text("right")+")";
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("editKeyframe"), widget, label);
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(filter)}));
    for (int i = 0; i < parameter.length; i++)
      {
        if (widget[i] instanceof ValueSlider)
          key.val[i] = ((ValueSlider) widget[i]).getValue();
        else
          key.val[i] = ((ValueField) widget[i]).getValue();
      }
    if (sameBox.getState())
      s.setSmoothness(s1Slider.getValue(), s2Slider.getValue());
    else
      s.setSmoothness(s1Slider.getValue());
    moveKeyframe(which, timeField.getValue());
  }

  /** This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    BTextField nameField = new BTextField(FilterParameterTrack.this.getName());
    BComboBox smoothChoice = new BComboBox(new String [] {
      Translate.text("Discontinuous"),
      Translate.text("Linear"),
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    smoothChoice.setSelectedIndex(smoothingMethod);
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("filterTrackTitle"), new Widget []
        {nameField, smoothChoice}, new String [] {Translate.text("trackName"), Translate.text("SmoothingMethod")});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(filter)}));
    this.setName(nameField.getText());
    smoothingMethod = smoothChoice.getSelectedIndex();
  }
}

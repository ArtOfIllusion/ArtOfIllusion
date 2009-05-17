/* Copyright (C) 2003-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.image.filter.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;

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
    int index = 0;
    Property[] properties = filter.getProperties();
    for (int i = 0; i < properties.length; i++)
    {
      if (properties[i].getType() == Property.DOUBLE)
        filter.setPropertyValue(i, k.val[index++]);
      else if (properties[i].getType() == Property.COLOR)
        filter.setPropertyValue(i, new RGBColor(k.val[index++], k.val[index++], k.val[index++]));
    }
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
    double key[] = getCurrentValues();
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
    double val2[] = getCurrentValues();
    for (int i = 0; i < val1.length; i++)
      if (val1[i] != val2[i])
        return setKeyframe(time, sc);
    return null;
  }

  /** Look up the current values from the filter. */

  private double[] getCurrentValues()
  {
    ArrayList<Double> values = new ArrayList<Double>();
    Property properties[] = filter.getProperties();
    for (int i = 0; i < properties.length; i++)
    {
      if (properties[i].getType() == Property.DOUBLE)
        values.add((Double) filter.getPropertyValue(i));
      else if (properties[i].getType() == Property.COLOR)
      {
        RGBColor color = (RGBColor) filter.getPropertyValue(i);
        values.add(Double.valueOf(color.getRed()));
        values.add(Double.valueOf(color.getGreen()));
        values.add(Double.valueOf(color.getBlue()));
      }
    }
    double val[] = new double[values.size()];
    for (int i = 0; i < values.size(); i++)
      val[i] = values.get(i);
    return val;
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
    ArrayList<String> names = new ArrayList<String>();
    for (Property property : filter.getProperties())
    {
      if (property.getType() == Property.DOUBLE)
        names.add(property.getName());
      else if (property.getType() == Property.COLOR)
      {
        names.add(property.getName()+" ("+Translate.text("Red")+")");
        names.add(property.getName()+" ("+Translate.text("Green")+")");
        names.add(property.getName()+" ("+Translate.text("Blue")+")");
      }
    }
    return names.toArray(new String[names.size()]);
  }

  /** Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    return getCurrentValues();
  }
  
  /** Get the allowed range for graphable values.  This returns a 2D array, where elements
      [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
      the nth graphable value. */
  
  public double[][] getValueRange()
  {
    ArrayList<double[]> ranges = new ArrayList<double[]>();
    for (Property property : filter.getProperties())
    {
      if (property.getType() == Property.DOUBLE)
        ranges.add(new double[] {property.getMinimum(), property.getMaximum()});
      else if (property.getType() == Property.COLOR)
      {
        ranges.add(new double[] {0.0, Double.MAX_VALUE});
        ranges.add(new double[] {0.0, Double.MAX_VALUE});
        ranges.add(new double[] {0.0, Double.MAX_VALUE});
      }
    }
    return ranges.toArray(new double[ranges.size()][]);
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
    Property properties[] = filter.getProperties();
    ArrayList<PropertyEditor> editors = new ArrayList<PropertyEditor>();

    int index = 0;
    for (Property property : properties)
    {
      if (property.getType() == Property.DOUBLE)
        editors.add(new PropertyEditor(property, key.val[index++]));
      else if (property.getType() == Property.COLOR)
        editors.add(new PropertyEditor(property, new RGBColor(key.val[index++], key.val[index++], key.val[index++])));
    }
    Widget widget[] = new Widget [editors.size()+5];
    String label[] = new String [editors.size()+5];
    for (int i = 0; i < editors.size(); i++)
    {
      widget[i] = editors.get(i).getWidget();
      label[i] = editors.get(i).getLabel();
    }
    sameBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        s2Slider.setEnabled(sameBox.getState());
      }
    });
    s2Slider.setEnabled(sameBox.getState());
    int n = editors.size();
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
    index = 0;
    for (PropertyEditor editor : editors)
    {
      if (editor.getProperty().getType() == Property.DOUBLE)
        key.val[index++] = (Double) editor.getValue();
      else
      {
        RGBColor color = (RGBColor) editor.getValue();
        key.val[index++] = color.getRed();
        key.val[index++] = color.getGreen();
        key.val[index++] = color.getBlue();
      }
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

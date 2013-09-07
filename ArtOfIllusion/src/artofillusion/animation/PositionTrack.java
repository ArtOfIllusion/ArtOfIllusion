/* Copyright (C) 2001-2013 by Peter Eastman

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
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;

/** This is a Track which controls the position of an object. */

public class PositionTrack extends Track
{
  ObjectInfo info;
  Timecourse tc;
  int smoothingMethod, mode, relCoords, joint;
  ObjectRef relObject;
  WeightTrack theWeight;
  boolean enablex, enabley, enablez;
  
  public static final int ABSOLUTE = 0;
  public static final int RELATIVE = 1;

  public static final int WORLD = 0;
  public static final int PARENT = 1;
  public static final int OBJECT = 2;
  public static final int LOCAL = 3;
  
  public PositionTrack(ObjectInfo info)
  {
    this(info, "Position", true, true, true);
  }
  
  public PositionTrack(ObjectInfo info, String name, boolean affectX, boolean affectY, boolean affectZ)
  {
    super(name);
    this.info = info;
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
    smoothingMethod = Timecourse.INTERPOLATING;
    mode = ABSOLUTE;
    relCoords = PARENT;
    relObject = new ObjectRef();
    theWeight = new WeightTrack(this);
    enablex = affectX;
    enabley = affectY;
    enablez = affectZ;
    joint = -1;
  }
  
  /** Modify the position of the object. */
  
  public void apply(double time)
  {
    Vec3 pos = (VectorKeyframe) tc.evaluate(time, smoothingMethod);
    double weight = theWeight.getWeight(time);

    if (pos == null)
      return;
    Vec3 v = info.getCoords().getOrigin();
    if (mode == ABSOLUTE)
      {
        double w = 1.0-weight;
        if (enablex)
          v.x *= w;
        if (enabley)
          v.y *= w;
        if (enablez)
          v.z *= w;
      }
    if (mode == ABSOLUTE && relCoords == PARENT)
      {
        if (info.getParent() != null)
          pos = info.getParent().getCoords().fromLocal().times(pos);
      }
    else if (mode == ABSOLUTE && relCoords == OBJECT)
      {
        CoordinateSystem coords = relObject.getCoords();
        if (coords != null)
          pos = coords.fromLocal().times(pos);
      }
    else if (mode == RELATIVE && relCoords == PARENT)
      {
        if (info.getParent() != null)
          pos = info.getParent().getCoords().fromLocal().timesDirection(pos);
      }
    else if (mode == RELATIVE && relCoords == OBJECT)
      {
        CoordinateSystem coords = relObject.getCoords();
        if (coords != null)
          pos = coords.fromLocal().timesDirection(pos);
      }
    else if (mode == RELATIVE && relCoords == LOCAL)
      pos = info.getCoords().fromLocal().timesDirection(pos);
    if (joint > -1 && mode == ABSOLUTE)
      {
        Joint j = info.getSkeleton().getJoint(joint);
        if (j != null)
          {
            if (info.getPose() != null && !info.getPose().equals(info.getObject().getPoseKeyframe()))
              {
                info.getObject().applyPoseKeyframe(info.getPose());
                j = info.getSkeleton().getJoint(joint);
              }
            pos = pos.minus(new ObjectRef(info, j).getCoords().getOrigin());
            pos.add(info.getCoords().getOrigin());
          }
      }
    if (enablex)
      v.x += pos.x*weight;
    if (enabley)
      v.y += pos.y*weight;
    if (enablez)
      v.z += pos.z*weight;
    info.getCoords().setOrigin(v);
  }
  
  /** Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    PositionTrack t = new PositionTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.mode = mode;
    t.relCoords = relCoords;
    t.smoothingMethod = smoothingMethod;
    t.tc = tc.duplicate((ObjectInfo) obj);
    t.relObject = relObject.duplicate();
    t.theWeight = (WeightTrack) theWeight.duplicate(t);
    t.enablex = enablex;
    t.enabley = enabley;
    t.enablez = enablez;
    t.joint = joint;
    return t;
  }
  
  /** Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    PositionTrack t = (PositionTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    mode = t.mode;
    relCoords = t.relCoords;
    smoothingMethod = t.smoothingMethod;
    tc = t.tc.duplicate(info);
    relObject = t.relObject.duplicate();
    theWeight = (WeightTrack) t.theWeight.duplicate(this);
    enablex = t.enablex;
    enabley = t.enabley;
    enablez = t.enablez;
    joint = t.joint;
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
    Vec3 pos = info.getCoords().getOrigin();

    if (joint > -1 && mode == ABSOLUTE)
      {
        Joint j = info.getSkeleton().getJoint(joint);
        if (j != null)
          pos = new ObjectRef(info, j).getCoords().getOrigin();
      }
    if (relCoords == PARENT)
      {
        if (info.getParent() != null)
          pos = info.getParent().getCoords().toLocal().times(pos);
      }
    else if (relCoords == OBJECT)
      {
        CoordinateSystem coords = relObject.getCoords();
        if (coords != null)
          pos = coords.toLocal().times(pos);
      }
    Keyframe k = new VectorKeyframe(pos);
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
    VectorKeyframe pos = (VectorKeyframe) tc.evaluate(time, smoothingMethod);
    Vec3 current = info.getCoords().getOrigin();

    if (joint > -1 && mode == ABSOLUTE)
      {
        Joint j = info.getSkeleton().getJoint(joint);
        if (j != null)
          current = new ObjectRef(info, j).getCoords().getOrigin();
      }
    if (relCoords == PARENT)
      {
        if (info.getParent() != null)
          current = info.getParent().getCoords().toLocal().times(current);
      }
    else if (relCoords == OBJECT)
      {
        CoordinateSystem coords = relObject.getCoords();
        if (coords != null)
          current = coords.toLocal().times(current);
      }
    if ((enablex && Math.abs(pos.x-current.x) > 1e-10) ||
        (enabley && Math.abs(pos.y-current.y) > 1e-10) ||
        (enablez && Math.abs(pos.z-current.z) > 1e-10))
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
  
  /** Determine whether this track affects the X coordinate. */
  
  public boolean affectsX()
  {
    return enablex;
  }
  
  /** Determine whether this track affects the Y coordinate. */
  
  public boolean affectsY()
  {
    return enabley;
  }
  
  /** Determine whether this track affects the Z coordinate. */
  
  public boolean affectsZ()
  {
    return enablez;
  }
  
  /** This has a single child track. */
  
  public Track [] getSubtracks()
  {
    return new Track [] {theWeight};
  }

  /** Determine whether this track can be added as a child of an object. */
  
  public boolean canAcceptAsParent(Object obj)
  {
    return (obj instanceof ObjectInfo);
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
    return (mode == RELATIVE);
  }
  
  /** Set whether this track is in absolute or relative mode. */
  
  public void setRelative(boolean rel)
  {
    mode = (rel ? RELATIVE : ABSOLUTE);
  }
  
  /** Get the coordinate system of this track (WORLD, PARENT, OBJECT, or LOCAL). */
  
  public int getCoordinateSystem()
  {
    return relCoords;
  }
  
  /** Set the coordinate system of this track (WORLD, PARENT, OBJECT, or LOCAL). */
  
  public void setCoordinateSystem(int system)
  {
    relCoords = system;
  }
  
  /** Get the object reference for the parent coordinate system.  The return
      value is undefined if getCoordinateSystem() does not return OBJECT. */
  
  public ObjectRef getCoordsObject()
  {
    return relObject;
  }
  
  /** Set the object reference for the parent coordinate system.  This causes
      the coordinate system to be set to OBJECT. */
  
  public void setCoordsObject(ObjectRef obj)
  {
    relObject = obj;
    relCoords = OBJECT;
  }
  
  /** Get the ID of the joint this track applies to, or -1 if it applies to the
      object origin. */
  
  public int getApplyToJoint()
  {
    return joint;
  }
  
  /** Set the ID of the joint this track applies to.  Specify -1 if it should
      apply to the object origin. */
  
  public void setApplyToJoint(int jointID)
  {
    joint = jointID;
  }
  
  /** Get the names of all graphable values for this track. */
  
  public String [] getValueNames()
  {
    return new String [] {"X", "Y", "Z"};
  }

  /** Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    Vec3 pos = info.getCoords().getOrigin();
    return new double [] {pos.x, pos.y, pos.z};
  }
  
  /** Get the allowed range for graphable values.  This returns a 2D array, where elements
      [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
      the nth graphable value. */
  
  public double[][] getValueRange()
  {
    double range[][] = new double [3][2];
    for (int i = 0; i < range.length; i++)
      {
        range[i][0] = -Double.MAX_VALUE;
        range[i][1] = Double.MAX_VALUE;
      }
    return range;
  }
  
  /** Get an array of any objects which this track depends on (and which therefore must
      be updated before this track is applied). */ 
  
  public ObjectInfo [] getDependencies()
  {
    if (relCoords == OBJECT)
    {
      ObjectInfo relInfo = relObject.getObject();
      if (relInfo != null)
        return new ObjectInfo [] {relInfo};
    }
    else if (relCoords == PARENT && info.getParent() != null)
      return new ObjectInfo [] {info.getParent()};
    return new ObjectInfo [0];
  }
  
  /** Delete all references to the specified object from this track.  This is used when an
      object is deleted from the scene. */
  
  public void deleteDependencies(ObjectInfo obj)
  {
    if (relObject.getObject() == obj)
      relObject = new ObjectRef();
  }

  public void updateObjectReferences(Map<ObjectInfo, ObjectInfo> objectMap)
  {
    if (objectMap.containsKey(relObject.getObject()))
    {
      ObjectInfo newObject = objectMap.get(relObject.getObject());
      if (relObject.getJoint() == null)
        relObject = new ObjectRef(newObject);
      else
        relObject = new ObjectRef(newObject, newObject.getSkeleton().getJoint(relObject.getJoint().id));
    }
  }

  /** Write a serialized representation of this track to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene scene) throws IOException
  {
    double t[] = tc.getTimes();
    Smoothness s[] = tc.getSmoothness();
    Keyframe v[] = tc.getValues();

    out.writeShort(1); // Version number
    out.writeUTF(name);
    out.writeBoolean(enabled);
    out.writeInt(smoothingMethod);
    out.writeInt(mode);
    out.writeInt(relCoords);
    out.writeInt(joint);
    out.writeBoolean(enablex);
    out.writeBoolean(enabley);
    out.writeBoolean(enablez);
    out.writeInt(t.length);
    for (int i = 0; i < t.length; i++)
      {
        out.writeDouble(t[i]);
        ((VectorKeyframe) v[i]).writeToFile(out); 
        s[i].writeToStream(out); 
      }
    if (relCoords == OBJECT)
      relObject.writeToStream(out);
    theWeight.writeToStream(out, scene);
  }
  
  /** Initialize this tracked based on its serialized representation as written by writeToStream(). */
  
  public void initFromStream(DataInputStream in, Scene scene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    name = in.readUTF();
    enabled = in.readBoolean();
    smoothingMethod = in.readInt();
    mode = in.readInt();
    relCoords = in.readInt();
    joint = (version == 0 ? -1 : in.readInt());
    enablex = in.readBoolean();
    enabley = in.readBoolean();
    enablez = in.readBoolean();
    int keys = in.readInt();
    double t[] = new double [keys];
    Smoothness s[] = new Smoothness [keys];
    Keyframe v[] = new Keyframe [keys];
    for (int i = 0; i < keys; i++)
      {
        t[i] = in.readDouble();
        v[i] = new VectorKeyframe(new Vec3(in));
        s[i] = new Smoothness(in);
      }
    tc = new Timecourse(v, t, s);
    if (relCoords == OBJECT)
      relObject = new ObjectRef(in, scene);
    else
      relObject = new ObjectRef();
    theWeight.initFromStream(in, scene);
  }

  /** Present a window in which the user can edit the specified keyframe. */
  
  public void editKeyframe(LayoutWindow win, int which)
  {
    VectorKeyframe key = (VectorKeyframe) tc.getValues()[which];
    Smoothness s = tc.getSmoothness()[which];
    double time = tc.getTimes()[which];
    ValueField xField = new ValueField(key.x, ValueField.NONE, 5);
    ValueField yField = new ValueField(key.y, ValueField.NONE, 5);
    ValueField zField = new ValueField(key.z, ValueField.NONE, 5);
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
        {xField, yField, zField, timeField, sameBox, new BLabel(Translate.text("Smoothness")+':'), s1Slider, s2Slider},
        new String [] {"X", "Y", "Z", Translate.text("Time"), null, null, "("+Translate.text("left")+")", "("+Translate.text("right")+")"});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(info)}));
    key.x = xField.getValue();
    key.y = yField.getValue();
    key.z = zField.getValue();
    if (sameBox.getState())
      s.setSmoothness(s1Slider.getValue(), s2Slider.getValue());
    else
      s.setSmoothness(s1Slider.getValue());
    moveKeyframe(which, timeField.getValue());
  }

  /** This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    Skeleton s = info.getSkeleton();
    Joint j[] = (s == null ? null : s.getJoints());
    BTextField nameField = new BTextField(getName());
    BComboBox smoothChoice = new BComboBox(new String [] {
      Translate.text("Discontinuous"),
      Translate.text("Linear"),
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    smoothChoice.setSelectedIndex(smoothingMethod);
    final BComboBox modeChoice = new BComboBox(new String [] {
      Translate.text("Absolute"),
      Translate.text("Relative")
    });
    modeChoice.setSelectedIndex(mode);
    final BComboBox coordsChoice = new BComboBox(new String [] {
      Translate.text("World"),
      Translate.text("Parent"),
      Translate.text("OtherObject")
    });
    if (mode == 1)
      coordsChoice.add(Translate.text("Local"));
    coordsChoice.setSelectedIndex(relCoords);
    BComboBox jointChoice = new BComboBox();
    jointChoice.add(Translate.text("objectOrigin"));
    if (j != null)
    {
      for (int i = 0; i < j.length; i++)
        jointChoice.add(j[i].name);
      for (int i = 0; i < j.length; i++)
        if (j[i].id == joint)
          jointChoice.setSelectedIndex(i+1);
    }
    final ObjectRefSelector objSelector = new ObjectRefSelector(relObject, win, Translate.text("positionRelativeTo"), info);
    objSelector.setEnabled(coordsChoice.getSelectedIndex() == 2);
    modeChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        int sel = modeChoice.getSelectedIndex();
        if (sel == 0 && coordsChoice.getItemCount() == 4)
          coordsChoice.remove(3);
        if (sel == 1 && coordsChoice.getItemCount() == 3)
          coordsChoice.add(Translate.text("Local"));
        objSelector.setEnabled(coordsChoice.getSelectedIndex() == 2);
      }
    });
    coordsChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        objSelector.setEnabled(coordsChoice.getSelectedIndex() == 2);
      }
    });
    RowContainer row = new RowContainer();
    BCheckBox xbox, ybox, zbox;
    row.add(xbox = new BCheckBox("X", enablex));
    row.add(ybox = new BCheckBox("Y", enabley));
    row.add(zbox = new BCheckBox("Z", enablez));
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("positionTrackTitle"), new Widget []
        {nameField, smoothChoice, modeChoice, jointChoice, coordsChoice, objSelector, row}, new String []
        {Translate.text("trackName"), Translate.text("SmoothingMethod"), Translate.text("trackMode"), Translate.text("applyTo"), Translate.text("CoordinateSystem"), "", Translate.text("trackAffects")});
    if (!dlg.clickedOk())
      return;
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()}));
    this.setName(nameField.getText());
    smoothingMethod = smoothChoice.getSelectedIndex();
    mode = modeChoice.getSelectedIndex();
    relCoords = coordsChoice.getSelectedIndex();
    relObject = objSelector.getSelection();
    if (jointChoice.getSelectedIndex() == 0)
      joint = -1;
    else
      joint = j[jointChoice.getSelectedIndex()-1].id;
    enablex = xbox.getState();
    enabley = ybox.getState();
    enablez = zbox.getState();
  }
}
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

/** This is a Track which controls the rotation of an object. */

public class RotationTrack extends Track
{
  private ObjectInfo info;
  private boolean quaternion;
  private Timecourse tc;
  private int smoothingMethod, mode, relCoords, joint;
  private ObjectRef relObject;
  private WeightTrack theWeight;
  private boolean enablex, enabley, enablez;
  
  public static final int ABSOLUTE = 0;
  public static final int RELATIVE = 1;

  public static final int WORLD = 0;
  public static final int PARENT = 1;
  public static final int OBJECT = 2;
  public static final int LOCAL = 3;
  
  public RotationTrack(ObjectInfo info)
  {
    this(info, "Rotation", true, true, true, true);
  }
  
  public RotationTrack(ObjectInfo info, String name, boolean useQuaternion, boolean affectX, boolean affectY, boolean affectZ)
  {
    super(name);
    this.info = info;
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
    tc.setSubdivideAdaptively(!useQuaternion);
    smoothingMethod = Timecourse.INTERPOLATING;
    mode = ABSOLUTE;
    relCoords = PARENT;
    relObject = new ObjectRef();
    theWeight = new WeightTrack(this);
    quaternion = useQuaternion;
    enablex = affectX;
    enabley = affectY;
    enablez = affectZ;
    joint = -1;
  }
  
  /** Modify the rotation of the object. */
  
  public void apply(double time)
  {
    RotationKeyframe rot = (RotationKeyframe) tc.evaluate(time, smoothingMethod);
    double weight = theWeight.getWeight(time);

    if (rot == null)
      return;
    Mat4 pre = null, post = null;
    if (relCoords == PARENT && info.getParent() != null)
      {
        pre = info.getParent().getCoords().toLocal();
        post = info.getParent().getCoords().fromLocal();
      }
    else if (relCoords == OBJECT)
      {
        CoordinateSystem coords = relObject.getCoords();
        if (coords != null)
          {
            pre = coords.toLocal();
            post = coords.fromLocal();
          }
      }
    else if (mode == RELATIVE && relCoords == LOCAL)
      {
        pre = info.getCoords().fromLocal();
        post = info.getCoords().toLocal();
      }
    rot.applyToCoordinates(info.getCoords(), weight, pre, post, (mode == RELATIVE), enablex, enabley, enablez);
    Joint j = (joint > -1 ? info.getSkeleton().getJoint(joint) : null);
    if (j != null && mode == ABSOLUTE)
      {
        if (info.getPose() != null && !info.getPose().equals(info.getObject().getPoseKeyframe()))
          {
            info.getObject().applyPoseKeyframe(info.getPose());
            j = info.getSkeleton().getJoint(joint);
          }
        Mat4 m = info.getCoords().fromLocal().times(j.coords.toLocal().times(info.getCoords().toLocal()));
        info.getCoords().transformAxes(m);
      }
  }
  
  /** Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    RotationTrack t = new RotationTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.mode = mode;
    t.relCoords = relCoords;
    t.smoothingMethod = smoothingMethod;
    t.quaternion = quaternion;
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
    RotationTrack t = (RotationTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    mode = t.mode;
    relCoords = t.relCoords;
    smoothingMethod = t.smoothingMethod;
    quaternion = t.quaternion;
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
    ((RotationKeyframe) k).setUseQuaternion(quaternion);
    tc.addTimepoint(k, time, s);
  }
  
  /** Set a keyframe at the specified time, based on the current state of the Scene. */
  
  public Keyframe setKeyframe(double time, Scene sc)
  {
    RotationKeyframe r = null;
    CoordinateSystem c = null;
    
    if (joint > -1 && mode == ABSOLUTE)
      {
        Joint j = info.getSkeleton().getJoint(joint);
        if (j != null)
          c = new ObjectRef(info, j).getCoords().duplicate();
      }
    if (c == null)
      c = info.getCoords().duplicate();
    if (relCoords == PARENT && info.getParent() != null)
      {
        c.transformAxes(info.getParent().getCoords().toLocal());
        r = new RotationKeyframe(c);
      }
    else if (relCoords == OBJECT)
      {
        CoordinateSystem coords = relObject.getCoords();
        if (coords != null)
          {
            c.transformAxes(coords.toLocal());
            r = new RotationKeyframe(c);
          }
      }
    if (r == null)
      r = new RotationKeyframe(c);
    r.setUseQuaternion(quaternion);
    tc.addTimepoint(r, time, new Smoothness());
    return r;
  }

  /** Set a keyframe at the specified time, based on the current state of the Scene,
      if and only if the Scene does not match the current state of the track.  Return
      the new Keyframe, or null if none was set. */
  
  public Keyframe setKeyframeIfModified(double time, Scene sc)
  {
    if (tc.getTimes().length == 0)
      return setKeyframe(time, sc);
    RotationKeyframe rot = (RotationKeyframe) tc.evaluate(time, smoothingMethod);
    RotationKeyframe current = null;
    Joint j = (joint > -1 && mode == ABSOLUTE ? info.getSkeleton().getJoint(joint) : null);
    CoordinateSystem c = (j == null ? info.getCoords() : new ObjectRef(info, j).getCoords());

    if (relCoords == PARENT && info.getParent() != null)
      {
        c = c.duplicate();
        c.transformAxes(info.getParent().getCoords().toLocal());
      }
    else if (relCoords == OBJECT)
      {
        CoordinateSystem coords = relObject.getCoords();
        if (coords != null)
          {
            c = c.duplicate();
            c.transformAxes(coords.toLocal());
          }
      }
    current = new RotationKeyframe(c);
    if (quaternion)
      {
        double q1[] = rot.getQuaternion(), q2[] = current.getQuaternion();
        double dot = q1[0]*q2[0] + q1[1]*q2[1] + q1[2]*q2[2] + q1[3]*q2[3];
        if (1.0-dot < 1e-10)
          return null;
      }
    else if ((!enablex || Math.abs(rot.x-current.x) < 1e-10) &&
        (!enabley || Math.abs(rot.y-current.y) < 1e-10) &&
        (!enablez || Math.abs(rot.z-current.z) < 1e-10))
      return null;
    return setKeyframe(time, sc);
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
    return enablex || quaternion;
  }
  
  /** Determine whether this track affects the Y coordinate. */
  
  public boolean affectsY()
  {
    return enabley || quaternion;
  }
  
  /** Determine whether this track affects the Z coordinate. */
  
  public boolean affectsZ()
  {
    return enablez || quaternion;
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
  
  /** Determine whether quaternion interpolation should be used. */
  
  public boolean getUseQuaternion()
  {
    return quaternion;
  }
  
  /** Set whether quaternion interpolation should be used. */
  
  public void setUseQuaternion(boolean use)
  {
    Keyframe val[] = tc.getValues();

    quaternion = use;
    tc.setSubdivideAdaptively(!quaternion);
    for (int i = 0; i < val.length; i++)
      {
        RotationKeyframe v = (RotationKeyframe) val[i];
        v.setUseQuaternion(use);
      }
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
    return new String [] {"X Angle", "Y Angle", "Z Angle"};
  }

  /** Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    return info.getCoords().getRotationAngles();
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
    out.writeBoolean(quaternion);
    out.writeBoolean(enablex);
    out.writeBoolean(enabley);
    out.writeBoolean(enablez);
    out.writeInt(t.length);
    for (int i = 0; i < t.length; i++)
      {
        RotationKeyframe k = (RotationKeyframe) v[i];
        out.writeDouble(t[i]);
        k.writeToStream(out);
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
    quaternion = in.readBoolean();
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
        RotationKeyframe k = new RotationKeyframe(in, info);
        k.setUseQuaternion(quaternion);
        v[i] = k;
        s[i] = new Smoothness(in);
      }
    tc = new Timecourse(v, t, s);
    tc.setSubdivideAdaptively(!quaternion);
    if (relCoords == OBJECT)
      relObject = new ObjectRef(in, scene);
    else
      relObject = new ObjectRef();
    theWeight.initFromStream(in, scene);
  }
  
  /** Present a window in which the user can edit the specified keyframe. */
  
  public void editKeyframe(LayoutWindow win, int which)
  {
    RotationKeyframe key = (RotationKeyframe) tc.getValues()[which];
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
    key.set(xField.getValue(), yField.getValue(), zField.getValue());
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
    final BCheckBox isoBox = new BCheckBox(Translate.text("isotropicRotations"), quaternion);
    RowContainer row = new RowContainer();
    final BCheckBox xbox, ybox, zbox;
    row.add(xbox = new BCheckBox("X", enablex));
    row.add(ybox = new BCheckBox("Y", enabley));
    row.add(zbox = new BCheckBox("Z", enablez));
    isoBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        xbox.setEnabled(!isoBox.getState());
        ybox.setEnabled(!isoBox.getState());
        zbox.setEnabled(!isoBox.getState());
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(win, Translate.text("rotationTrackTitle"), new Widget []
        {nameField, smoothChoice, modeChoice, jointChoice, coordsChoice, objSelector, isoBox, row}, new String []
        {Translate.text("trackName"), Translate.text("SmoothingMethod"), Translate.text("trackMode"), Translate.text("applyTo"), Translate.text("CoordinateSystem"), "", null, Translate.text("trackAffects")});
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
    setUseQuaternion(isoBox.getState());
    enablex = xbox.getState();
    enabley = ybox.getState();
    enablez = zbox.getState();
  }
}
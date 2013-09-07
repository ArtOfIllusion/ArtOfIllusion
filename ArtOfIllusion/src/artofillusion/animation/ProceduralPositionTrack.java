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
import artofillusion.procedural.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;

/** This is a Track which uses a procedure to control the position of an object. */

public class ProceduralPositionTrack extends Track implements ProcedureOwner
{
  ObjectInfo info;
  Procedure proc;
  Timecourse tc;
  TextureParameter parameter[];
  int smoothingMethod, mode, relCoords, joint;
  ObjectRef relObject;
  WeightTrack theWeight;
  
  public static final int ABSOLUTE = 0;
  public static final int RELATIVE = 1;

  public static final int WORLD = 0;
  public static final int PARENT = 1;
  public static final int OBJECT = 2;
  public static final int LOCAL = 3;
  
  public ProceduralPositionTrack(ObjectInfo info)
  {
    super("Position (procedural)");
    this.info = info;
    proc = new Procedure(new OutputModule [] {
      new OutputModule("X", "0", 0.0, null, IOPort.NUMBER),
      new OutputModule("Y", "0", 0.0, null, IOPort.NUMBER),
      new OutputModule("Z", "0", 0.0, null, IOPort.NUMBER)});
    parameter = new TextureParameter [0];
    tc = new Timecourse(new Keyframe [0], new double [0], new Smoothness [0]);
    smoothingMethod = Timecourse.INTERPOLATING;
    mode = ABSOLUTE;
    relCoords = WORLD;
    relObject = new ObjectRef();
    theWeight = new WeightTrack(this);
    joint = -1;
  }
  
  /* Modify the position of the object. */
  
  public void apply(double time)
  {
    PointInfo point = new PointInfo();
    Vec3 v = info.getCoords().getOrigin(), jointDelta = null;
    OutputModule output[] = proc.getOutputModules();

    point.x = v.x;
    point.y = v.y;
    point.z = v.z;
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
            jointDelta = new ObjectRef(info, j).getCoords().getOrigin().minus(info.getCoords().getOrigin());
            point.x += jointDelta.x;
            point.y += jointDelta.y;
            point.z += jointDelta.z;
          }
      }
    point.t = time;
    ArrayKeyframe params = (ArrayKeyframe) tc.evaluate(time, smoothingMethod);
    if (params != null)
      point.param = params.val;
    proc.initForPoint(point);
    Vec3 pos = new Vec3(output[0].getAverageValue(0, 0.0), output[1].getAverageValue(0, 0.0), output[2].getAverageValue(0, 0.0));
    double weight = theWeight.getWeight(time);
    if (mode == ABSOLUTE)
      {
        double w = 1.0-weight;
        v.x *= w;
        v.y *= w;
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
    if (jointDelta != null)
      pos.subtract(jointDelta);
    v.x += pos.x*weight;
    v.y += pos.y*weight;
    v.z += pos.z*weight;
    info.getCoords().setOrigin(v);
  }
  
  /* Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    ProceduralPositionTrack t = new ProceduralPositionTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.proc.copy(proc);
    t.mode = mode;
    t.relCoords = relCoords;
    t.smoothingMethod = smoothingMethod;
    t.tc = tc.duplicate((ObjectInfo) obj);
    t.relObject = relObject.duplicate();
    t.theWeight = (WeightTrack) theWeight.duplicate(t);
    t.joint = joint;
    return t;
  }
  
  /* Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    ProceduralPositionTrack t = (ProceduralPositionTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    proc.copy(t.proc);
    mode = t.mode;
    relCoords = t.relCoords;
    smoothingMethod = t.smoothingMethod;
    tc = t.tc.duplicate(info);
    relObject = t.relObject.duplicate();
    theWeight = (WeightTrack) t.theWeight.duplicate(this);
    joint = t.joint;
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
    if (parameter.length == 0)
      return null; // There are no parameters to keyframe.
    ArrayKeyframe params = (ArrayKeyframe) tc.evaluate(time, smoothingMethod);
    double p[];
    if (params == null)
      p = getDefaultGraphValues();
    else
      {
        p = new double [params.val.length];
        System.arraycopy(params.val, 0, p, 0, p.length);
      }
    Keyframe k = new ArrayKeyframe(p);
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
  
  /* Procedural tracks are never null. */
  
  public boolean isNullTrack()
  {
    return false;
  }
    
  /* This has a single child track. */
  
  public Track [] getSubtracks()
  {
    return new Track [] {theWeight};
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
  
  /* Get the names of all graphable values for this track. */
  
  public String [] getValueNames()
  {
    String names[] = new String [parameter.length];
    for (int i = 0; i < names.length; i++)
      names[i] = parameter[i].name;
    return names;
  }

  /* Get the default list of graphable values (for a track which has no keyframes). */
  
  public double [] getDefaultGraphValues()
  {
    double val[] = new double [parameter.length];
    for (int i = 0; i < val.length; i++)
      val[i] = parameter[i].defaultVal;
    return val;
  }
  
  /* Get the allowed range for graphable values.  This returns a 2D array, where elements
     [n][0] and [n][1] are the minimum and maximum allowed values, respectively, for
     the nth graphable value. */
  
  public double[][] getValueRange()
  {
    double range[][] = new double [parameter.length][2];
    for (int i = 0; i < range.length; i++)
      {
        range[i][0] = parameter[i].minVal;
        range[i][1] = parameter[i].maxVal;
      }
    return range;
  }
  
  /* Get an array of any objects which this track depends on (and which therefore must
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
  
  /* Delete all references to the specified object from this track.  This is used when an
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

  /** Find all the parameters for the procedure. */

  private TextureParameter[] findParameters()
  {
    Module module[] = proc.getModules();
    int count = 0;
    
    for (int i = 0; i < module.length; i++)
      if (module[i] instanceof ParameterModule)
        count++;
    TextureParameter params[] = new TextureParameter [count];
    count = 0;
    for (int i = 0; i < module.length; i++)
      if (module[i] instanceof ParameterModule)
        {
          params[count] = ((ParameterModule) module[i]).getParameter(this);
          ((ParameterModule) module[i]).setIndex(count++);
        }
    return params;
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
    proc.writeToStream(out, scene);
    out.writeInt(smoothingMethod);
    out.writeInt(mode);
    out.writeInt(relCoords);
    out.writeInt(joint);
    out.writeInt(t.length);
    for (int i = 0; i < t.length; i++)
      {
        out.writeDouble(t[i]);
        ((ArrayKeyframe) v[i]).writeToStream(out); 
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
    proc.readFromStream(in, scene);
    smoothingMethod = in.readInt();
    mode = in.readInt();
    relCoords = in.readInt();
    joint = (version == 0 ? -1 : in.readInt());
    int keys = in.readInt();
    double t[] = new double [keys];
    Smoothness s[] = new Smoothness [keys];
    Keyframe v[] = new Keyframe [keys];
    for (int i = 0; i < keys; i++)
      {
        t[i] = in.readDouble();
        v[i] = new ArrayKeyframe(in, this);
        s[i] = new Smoothness(in);
      }
    tc = new Timecourse(v, t, s);
    if (relCoords == OBJECT)
      relObject = new ObjectRef(in, scene);
    else
      relObject = new ObjectRef();
    theWeight.initFromStream(in, scene);
    parameter = findParameters();
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
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_TRACK, new Object [] {this, duplicate(info)}));
    for (int i = 0; i < parameter.length; i++)
    {
      if (widget[i] instanceof ValueField)
        key.val[i] = ((ValueField) widget[i]).getValue();
      else
        key.val[i] = ((ValueSlider) widget[i]).getValue();
    }
    if (sameBox.getState())
      s.setSmoothness(s1Slider.getValue(), s2Slider.getValue());
    else
      s.setSmoothness(s1Slider.getValue());
    moveKeyframe(which, timeField.getValue());
  }

  /* This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    ProcedureEditor editor = new ProcedureEditor(proc, this, win.getScene());
    editor.setEditingWindow(win);
  }

  /** Get the title of the procedure's editing window. */
  
  public String getWindowTitle()
  {
    return Translate.text("procPosTrackTitle");
  }
  
  /** Create an object which displays a preview of the procedure. */
  
  public Object getPreview(ProcedureEditor editor)
  {
    return null;
  }
  
  /** Update the display of the preview. */
  
  public void updatePreview(Object preview)
  {
  }
  
  /** Dispose of the preview object when the editor is closed. */
  
  public void disposePreview(Object preview)
  {
  }
  
  /** Determine whether the procedure may contain View Angle modules. */
  
  public boolean allowViewAngle()
  {
    return false;
  }
  
  /** Determine whether the procedure may contain Parameter modules. */
  
  public boolean allowParameters()
  {
    return true;
  }
  
  /** Determine whether the procedure may be renamed. */
  
  public boolean canEditName()
  {
    return true;
  }
  
  /** This is called when the user clicks OK in the procedure editor. */
  
  public void acceptEdits(ProcedureEditor editor)
  {
    EditingWindow win = editor.getEditingWindow();
    win.setUndoRecord(new UndoRecord(win, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()}));
    TextureParameter newparams[] = findParameters();
    int index[] = new int [newparams.length];
    for (int i = 0; i < newparams.length; i++)
      {
        index[i] = -1;
        for (int j = 0; j < parameter.length; j++)
          if (parameter[j].equals(newparams[i]))
            index[i] = j;
      }
    parameter = newparams;
    Keyframe key[] = tc.getValues();
    for (int i = 0; i < key.length; i++)
      {
        double newval[] = new double [parameter.length];
        for (int j = 0; j < newval.length; j++)
          {
            if (index[j] > -1)
              newval[j] = ((ArrayKeyframe) key[i]).val[index[j]];
            else
              newval[j] = parameter[j].defaultVal;
          }
        ((ArrayKeyframe) key[i]).val = newval;
      }
    ((LayoutWindow) win).getScore().finishEditingTrack(this);
  }
  
  /** Display the Properties dialog. */
  
  public void editProperties(ProcedureEditor editor)
  {
    Skeleton s = info.getSkeleton();
    Joint j[] = (s == null ? null : s.getJoints());
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
    final BComboBox coordsChoice = new BComboBox(new String [] {
      Translate.text("World"),
      Translate.text("Parent"),
      Translate.text("OtherObject")
    });
    if (mode == 1)
      coordsChoice.add(Translate.text("Local"));
    coordsChoice.setSelectedIndex(relCoords);
    final ObjectRefSelector objSelector = new ObjectRefSelector(relObject, (LayoutWindow) editor.getEditingWindow(), Translate.text("positionRelativeTo"), info);
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
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("positionTrackTitle"), new Widget []
        {smoothChoice, modeChoice, jointChoice, coordsChoice, objSelector}, new String []
        {Translate.text("paramSmoothingMethod"), Translate.text("trackMode"), Translate.text("applyTo"), Translate.text("CoordinateSystem"), ""});
    if (!dlg.clickedOk())
      return;
    editor.saveState(false);
    smoothingMethod = smoothChoice.getSelectedIndex();
    mode = modeChoice.getSelectedIndex();
    relCoords = coordsChoice.getSelectedIndex();
    relObject = objSelector.getSelection();
    if (jointChoice.getSelectedIndex() == 0)
      joint = -1;
    else
      joint = j[jointChoice.getSelectedIndex()-1].id;
  }
}
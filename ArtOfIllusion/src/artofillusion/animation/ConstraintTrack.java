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

/** This is a Track which places constraints on the position or orientation of an object. */

public class ConstraintTrack extends Track
{
  ObjectInfo info;
  int xType, yType, zType, orientType, orientMode;
  ObjectRef faceToward;
  Vec3 pos, orient;
  WeightTrack theWeight;
  
  private static final int NONE = 0;
  private static final int LESS_THAN = 1;
  private static final int EQUAL_TO = 2;
  private static final int GREATER_THAN = 3;
  private static final int FORCE_X = 1;
  private static final int FORCE_Y = 2;
  private static final int FORCE_Z = 3;

  private static final int PARALLEL = 0;
  private static final int PERPENDICULAR = 1;
  private static final int FACES_OBJECT = 2;
  
  public ConstraintTrack(ObjectInfo info)
  {
    super("Constraint");
    this.info = info;
    theWeight = new WeightTrack(this);
    xType = yType = zType = orientType = NONE;
    orientMode = PARALLEL;
    faceToward = new ObjectRef();
    pos = new Vec3();
    orient = Vec3.vx();
  }

  /** This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    new Editor(win);
  }
  
  /** Modify the position of the object. */
  
  public void apply(double time)
  {
    double weight = theWeight.getWeight(time);

    Vec3 v = info.getCoords().getOrigin();
    v.x = findCoordinate(v.x, pos.x, xType, weight);
    v.y = findCoordinate(v.y, pos.y, yType, weight);
    v.z = findCoordinate(v.z, pos.z, zType, weight);
    if (orientType != NONE)
      {
        Vec3 zdir = info.getCoords().getZDirection();
        Vec3 ydir = info.getCoords().getUpDirection();
        Vec3 xdir = ydir.cross(zdir);
        Vec3 oldy = new Vec3(ydir);
        Vec3 oldz = new Vec3(zdir);
        if (orientType == FORCE_X)
          adjustAxes(xdir, ydir, zdir);
        else if (orientType == FORCE_Y)
          adjustAxes(ydir, zdir, xdir);
        else
          adjustAxes(zdir, xdir, ydir);
        if (weight < 1.0)
        {
          double w1 = 1.0-weight;
          ydir.set(weight*ydir.x+w1*oldy.x, weight*ydir.y+w1*oldy.y, weight*ydir.z+w1*oldy.z);
          zdir.set(weight*zdir.x+w1*oldz.x, weight*zdir.y+w1*oldz.y, weight*zdir.z+w1*oldz.z);
        }
        info.getCoords().setOrientation(zdir, ydir);
      }
    info.getCoords().setOrigin(v);
  }
  
  /** Find the new value for a particular coordinate, based on the current value and the
     constraint type. */
  
  private double findCoordinate(double current, double con, int type, double weight)
  {
    if (type == NONE)
      return current;
    if (type == GREATER_THAN && current > con)
      con = current;
    else if (type == LESS_THAN && current < con)
      con = current;
    return (1.0-weight)*current + weight*con;
  }
  
  /** Given three axes, adjust them so that the first one satisfies the specified constraint. */
  
  private void adjustAxes(Vec3 xdir, Vec3 ydir, Vec3 zdir)
  {
    Vec3 v;
    
    if (orientMode == FACES_OBJECT)
      {
        CoordinateSystem target = faceToward.getCoords();
        if (target == null)
          return;
        v = target.getOrigin().minus(info.getCoords().getOrigin());
      }
    else
      v = new Vec3(orient);
    double len = v.length(), dot;
    if (len == 0.0)
      return;
    v.scale(1.0/len);
    if (orientMode == PERPENDICULAR)
      {
        dot = xdir.dot(v);
        xdir.subtract(v.times(dot));
        xdir.normalize();
      }
    else
      xdir.set(v);
    ydir.set(zdir.cross(xdir));
    zdir.set(xdir.cross(ydir));
    zdir.normalize();
    ydir.normalize();
  }
  
  /** Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    ConstraintTrack t = new ConstraintTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.xType = xType;
    t.yType = yType;
    t.zType = zType;
    t.orientType = orientType;
    t.orientMode = orientMode;
    t.pos = new Vec3(pos);
    t.orient = new Vec3(orient);
    t.faceToward = faceToward.duplicate();
    t.theWeight = (WeightTrack) theWeight.duplicate(t);
    return t;
  }
  
  /** Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    ConstraintTrack t = (ConstraintTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    xType = t.xType;
    yType = t.yType;
    zType = t.zType;
    orientType = t.orientType;
    orientMode = t.orientMode;
    pos = new Vec3(t.pos);
    orient = new Vec3(t.orient);
    faceToward.copy(t.faceToward);
    theWeight = (WeightTrack) t.theWeight.duplicate(t);
  }
  
  /** Get a list of all keyframe times for this track. */
  
  public double [] getKeyTimes()
  {
    return new double [0];
  }

  /** Move a keyframe to a new time, and return its new position in the list. */
  
  public int moveKeyframe(int which, double time)
  {
    return -1;
  }
  
  /** Delete the specified keyframe. */
  
  public void deleteKeyframe(int which)
  {
  }
  
  /** Constraint tracks are never null. */
  
  public boolean isNullTrack()
  {
    return false;
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
  
  /** Get an array of any objects which this track depends on (and which therefore must
     be updated before this track is applied). */ 
  
  public ObjectInfo [] getDependencies()
  {
    if (orientType != NONE && orientMode == FACES_OBJECT)
      {
        ObjectInfo info = faceToward.getObject();
        if (info != null)
          return new ObjectInfo [] {info};
      }
     return new ObjectInfo [0];
  }
  
  /** Delete all references to the specified object from this track.  This is used when an
     object is deleted from the scene. */
  
  public void deleteDependencies(ObjectInfo obj)
  {
    if (faceToward.getObject() == obj)
      faceToward = new ObjectRef();
  }

  public void updateObjectReferences(Map<ObjectInfo, ObjectInfo> objectMap)
  {
    if (objectMap.containsKey(faceToward.getObject()))
    {
      ObjectInfo newObject = objectMap.get(faceToward.getObject());
      if (faceToward.getJoint() == null)
        faceToward = new ObjectRef(newObject);
      else
        faceToward = new ObjectRef(newObject, newObject.getSkeleton().getJoint(faceToward.getJoint().id));
    }
  }

  /** Write a serialized representation of this track to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene scene) throws IOException
  {
    out.writeShort(0); // Version number
    out.writeUTF(name);
    out.writeBoolean(enabled);
    out.writeInt(xType);
    out.writeInt(yType);
    out.writeInt(zType);
    out.writeInt(orientType);
    out.writeInt(orientMode);
    pos.writeToFile(out);
    orient.writeToFile(out);
    if (orientType != NONE && orientMode == FACES_OBJECT)
      faceToward.writeToStream(out);
    theWeight.writeToStream(out, scene);
  }
  
  /** Initialize this tracked based on its serialized representation as written by writeToStream(). */
  
  public void initFromStream(DataInputStream in, Scene scene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    
    if (version != 0)
      throw new InvalidObjectException("");
    name = in.readUTF();
    enabled = in.readBoolean();
    xType = in.readInt();
    yType = in.readInt();
    zType = in.readInt();
    orientType = in.readInt();
    orientMode = in.readInt();
    pos = new Vec3(in);
    orient = new Vec3(in);
    if (orientType != NONE && orientMode == FACES_OBJECT)
      faceToward = new ObjectRef(in, scene);
    else
      faceToward = new ObjectRef();
    theWeight.initFromStream(in, scene);
  }

  /** Inner class for editing constraint tracks. */
  
  private class Editor extends BDialog
  {
    LayoutWindow window;
    BComboBox xChoice, yChoice, zChoice, orientChoice, orientModeChoice;
    ValueField xField, yField, zField, orientXField, orientYField, orientZField;
    OverlayContainer orientPanel;
    ObjectRefSelector objSelector;
    BTextField nameField;
    
    public Editor(LayoutWindow win)
    {
      super(win, Translate.text("constraintTrackTitle"), true);
      window = win;
      
      // Layout the dialog.
      
      FormContainer content = new FormContainer(3, 7);
      setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
      content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, null, null));
      content.add(Translate.label("trackName"), 0, 0);
      content.add(nameField = new BTextField(ConstraintTrack.this.getName()), 1, 0, 2, 1);
      String constraint = Translate.text("constraint");
      String constraintOpt[] = new String [] {
          Translate.text("none"),
          Translate.text("lessThan"),
          Translate.text("equalTo"),
          Translate.text("greaterThan")
      };
      content.add(new BLabel("X "+constraint), 0, 1);
      content.add(new BLabel("Y "+constraint), 0, 2);
      content.add(new BLabel("Z "+constraint), 0, 3);
      content.add(xChoice = new BComboBox(constraintOpt), 1, 1);
      content.add(yChoice = new BComboBox(constraintOpt), 1, 2);
      content.add(zChoice = new BComboBox(constraintOpt), 1, 3);
      xChoice.setSelectedIndex(xType);
      yChoice.setSelectedIndex(yType);
      zChoice.setSelectedIndex(zType);
      xChoice.addEventLink(ValueChangedEvent.class, this, "updateComponents");
      yChoice.addEventLink(ValueChangedEvent.class, this, "updateComponents");
      zChoice.addEventLink(ValueChangedEvent.class, this, "updateComponents");
      content.add(xField = new ValueField(pos.x, ValueField.NONE, 5), 2, 1);
      content.add(yField = new ValueField(pos.y, ValueField.NONE, 5), 2, 2);
      content.add(zField = new ValueField(pos.z, ValueField.NONE, 5), 2, 3);
      content.add(Translate.label("Orientation"), 0, 4);
      orientChoice = new BComboBox(new String [] {
          Translate.text("none"),
          Translate.text("xAxis"),
          Translate.text("yAxis"),
          Translate.text("zAxis")
      });
      content.add(orientChoice, 1, 4);
      orientChoice.setSelectedIndex(orientType);
      orientChoice.addEventLink(ValueChangedEvent.class, this, "updateComponents");
      orientModeChoice = new BComboBox(new String [] {
          Translate.text("parallelTo"),
          Translate.text("perpTo"),
          Translate.text("facesToward")
      });
      content.add(orientModeChoice, 2, 4);
      orientModeChoice.setSelectedIndex(orientMode);
      orientModeChoice.addEventLink(ValueChangedEvent.class, this, "updateComponents");
      orientPanel = new OverlayContainer();
      RowContainer xyzRow = new RowContainer();
      orientPanel.add(xyzRow);
      xyzRow.add(new BLabel("X"));
      xyzRow.add(orientXField = new ValueField(orient.x, ValueField.NONE, 5));
      xyzRow.add(new BLabel("Y"));
      xyzRow.add(orientYField = new ValueField(orient.y, ValueField.NONE, 5));
      xyzRow.add(new BLabel("Z"));
      xyzRow.add(orientZField = new ValueField(orient.z, ValueField.NONE, 5));
      objSelector = new ObjectRefSelector(faceToward, win, Translate.text("axisFacesToward"), info);
      orientPanel.add(objSelector);
      content.add(orientPanel, 0, 5, 3, 1, new LayoutInfo());
      RowContainer buttons = new RowContainer();
      buttons.add(Translate.button("ok", this, "doOk"));
      buttons.add(Translate.button("cancel", this, "dispose"));
      content.add(buttons, 0, 6, 3, 1, new LayoutInfo());
      for (Widget w : UIUtilities.findAllChildren(this))
        w.addEventLink(KeyPressedEvent.class, this, "keyPressed");
      pack();
      UIUtilities.centerDialog(this, win);
      updateComponents();
      setVisible(true);
    }
    
    private void doOk()
    {
      window.setUndoRecord(new UndoRecord(window, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()}));
      ConstraintTrack.this.setName(nameField.getText());
      xType = xChoice.getSelectedIndex();
      yType = yChoice.getSelectedIndex();
      zType = zChoice.getSelectedIndex();
      orientType = orientChoice.getSelectedIndex();
      orientMode = orientModeChoice.getSelectedIndex();
      pos.set(xField.getValue(), yField.getValue(), zField.getValue());
      orient.set(orientXField.getValue(), orientYField.getValue(), orientZField.getValue());
      faceToward = objSelector.getSelection();
      window.getScore().repaintAll();
      dispose();
    }
    
    /** Make sure that the appropriate components are enabled. */
    
    private void updateComponents()
    {
      xField.setEnabled(xChoice.getSelectedIndex() > 0);
      yField.setEnabled(yChoice.getSelectedIndex() > 0);
      zField.setEnabled(zChoice.getSelectedIndex() > 0);
      boolean orientEnabled = (orientChoice.getSelectedIndex() > 0);
      orientXField.setEnabled(orientEnabled);
      orientYField.setEnabled(orientEnabled);
      orientZField.setEnabled(orientEnabled);
      objSelector.setEnabled(orientEnabled);
      orientModeChoice.setEnabled(orientEnabled);
      orientPanel.setVisibleChild(orientModeChoice.getSelectedIndex() == FACES_OBJECT ? 0 : 1);
    }

    private void keyPressed(KeyPressedEvent ev)
    {
      if (ev.getKeyCode() == KeyPressedEvent.VK_ESCAPE)
          dispose();
    }
  }
}
/* Copyright (C) 2003-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.animation.distortion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;

/** This is a Track which modifies the shape of an object using inverse kinematics. */

public class IKTrack extends Track
{
  private ObjectInfo info;
  private Vector constraints;
  private boolean useGestures;
  private WeightTrack theWeight;
  
  public IKTrack(ObjectInfo info)
  {
    super("Inverse Kinematics");
    this.info = info;
    theWeight = new WeightTrack(this);
    constraints = new Vector();
    useGestures = true;
  }
  
  /** Get whether to reshape the mesh based on its gestures. */
  
  public boolean getUseGestures()
  {
    return useGestures;
  }
  
  /** Set whether to reshape the mesh based on its gestures. */
  
  public void setUseGestures(boolean use)
  {
    useGestures = use;
  }

  /** This method presents a window in which the user can edit the track. */
  
  public void edit(LayoutWindow win)
  {
    if (info.getSkeleton() == null)
      new BStandardDialog("", Translate.text("ikNotApplyWarning"), BStandardDialog.INFORMATION).showMessageDialog(win);
    else
      new Editor(win);
  }
  
  /** Modify the position of the object. */
  
  public void apply(double time)
  {
    double weight = theWeight.getWeight(time);
    Skeleton skeleton = info.getSkeleton();
    if (skeleton == null)
      return;
    Joint joint[] = skeleton.getJoints();
    if (joint.length == 0)
      return;
    boolean locked[] = new boolean [joint.length];
    Vec3 target[] = new Vec3 [joint.length];
    Mat4 toLocal = info.getCoords().toLocal();
    
    for (int i = 0; i < constraints.size(); i++)
    {
      Constraint c = (Constraint) constraints.elementAt(i);
      int index = skeleton.findJointIndex(c.jointID);
      if (index == -1)
        continue;
      if (c.target == null)
        locked[index] = true;
      else
      {
        CoordinateSystem coords = c.target.getCoords();
        if (coords == null)
          locked[index] = true;
        else
          target[index] = toLocal.times(coords.getOrigin());
      }
    }
    Actor actor = Actor.getActor(info.getObject());
    info.addDistortion(new IKDistortion(locked, target, weight, actor));
  }
  
  /** Create a duplicate of this track. */
  
  public Track duplicate(Object obj)
  {
    IKTrack t = new IKTrack((ObjectInfo) obj);
    
    t.name = name;
    t.enabled = enabled;
    t.quantized = quantized;
    t.constraints = new Vector();
    for (int i = 0; i < constraints.size(); i++)
    {
      Constraint c = (Constraint) constraints.elementAt(i);
      t.constraints.addElement(c.duplicate());
    }
    t.theWeight = (WeightTrack) theWeight.duplicate(t);
    return t;
  }
  
  /** Make this track identical to another one. */
  
  public void copy(Track tr)
  {
    IKTrack t = (IKTrack) tr;
    
    name = t.name;
    enabled = t.enabled;
    quantized = t.quantized;
    constraints = new Vector();
    for (int i = 0; i < t.constraints.size(); i++)
    {
      Constraint c = (Constraint) t.constraints.elementAt(i);
      constraints.addElement(c.duplicate());
    }
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
  
  /** This track is null if it has no targets. */
  
  public boolean isNullTrack()
  {
    for (int i = 0; i < constraints.size(); i++)
      if (((Constraint) constraints.elementAt(i)).target != null)
        return false;
    return true;
  }
  
  /** This has a single child track. */
  
  public Track [] getSubtracks()
  {
    return new Track [] {theWeight};
  }

  /** Determine whether this track can be added as a child of an object. */
  
  public boolean canAcceptAsParent(Object obj)
  {
    return (obj instanceof ObjectInfo && ((ObjectInfo) obj).getObject() instanceof Mesh);
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
    Vector v = new Vector();
    for (int i = 0; i < constraints.size(); i++)
    {
      Constraint c = (Constraint) constraints.elementAt(i);
      if (c.target != null)
        v.addElement(c.target.getObject());
    }
    ObjectInfo ref[] = new ObjectInfo [v.size()];
    v.copyInto(ref);
    return ref;
  }
  
  /** Delete all references to the specified object from this track.  This is used when an
      object is deleted from the scene. */
  
  public void deleteDependencies(ObjectInfo obj)
  {
    for (int i = constraints.size()-1; i >= 0; i--)
    {
      Constraint c = (Constraint) constraints.elementAt(i);
      if (c.target != null && c.target.getObject() == obj)
        constraints.removeElementAt(i);
    }
  }

  public void updateObjectReferences(Map<ObjectInfo, ObjectInfo> objectMap)
  {
    for (int i = constraints.size()-1; i >= 0; i--)
    {
      Constraint c = (Constraint) constraints.elementAt(i);
      if (c.target != null && objectMap.containsKey(c.target.getObject()))
      {
        ObjectInfo newObject = objectMap.get(c.target.getObject());
        if (c.target.getJoint() == null)
          c.target = new ObjectRef(newObject);
        else
          c.target = new ObjectRef(newObject, newObject.getSkeleton().getJoint(c.target.getJoint().id));
      }
    }
  }

  /** Write a serialized representation of this track to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene scene) throws IOException
  {
    out.writeShort(1); // Version number
    out.writeUTF(name);
    out.writeBoolean(enabled);
    out.writeBoolean(useGestures);
    out.writeInt(constraints.size());
    for (int i = 0; i < constraints.size(); i++)
    {
      Constraint c = (Constraint) constraints.elementAt(i);
      out.writeInt(c.jointID);
      out.writeBoolean(c.target != null);
      if (c.target != null)
        c.target.writeToStream(out);
    }
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
    if (version > 0)
      useGestures = in.readBoolean();
    int numConstraints = in.readInt();
    constraints = new Vector();
    for (int i = 0; i < numConstraints; i++)
    {
      Constraint c = new Constraint(in.readInt(), in.readBoolean() ? new ObjectRef(in, scene) : null);
      constraints.addElement(c);
    }
    theWeight.initFromStream(in, scene);
  }
  
  /** This inner class represents a single constraint. */
  
  private class Constraint
  {
    public int jointID;
    public ObjectRef target;
    
    public Constraint(int id, ObjectRef t)
    {
      jointID = id;
      target = t;
    }
    
    public Constraint duplicate()
    {
      if (target == null)
        return new Constraint(jointID, null);
      return new Constraint(jointID, target.duplicate());
    }
  }

  /** Inner class for editing constraint tracks. */
  
  private class Editor extends BDialog
  {
    LayoutWindow window;
    BList constraintList;
    BTextField nameField;
    BCheckBox gesturesBox;
    Vector tempConstraints;
    int tempJointID[];
    ObjectRef tempTarget[];
    BButton editButton, deleteButton;
    
    public Editor(LayoutWindow win)
    {
      super(win, Translate.text("ikTrackTitle"), true);
      window = win;
      tempConstraints = new Vector();
      for (int i = 0; i < constraints.size(); i++)
        tempConstraints.addElement(((Constraint) constraints.elementAt(i)).duplicate());
      
      // Layout the dialog.
      
      FormContainer content = new FormContainer(new double [] {0.0, 1.0}, new double [] {0.0, 1.0, 0.0, 0.0});
      setContent(content);
      content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
      content.add(Translate.label("trackName"), 0, 0);
      content.add(Translate.label("constraints"), 0, 1);
      content.add(nameField = new BTextField(IKTrack.this.getName(), 30), 1, 0);
      content.add(UIUtilities.createScrollingList(constraintList = new BList()), 1, 1);
      constraintList.setPreferredVisibleRows(5);
      constraintList.addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
      constraintList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
      buildConstraintList();
      content.add(gesturesBox = new BCheckBox(Translate.text("useGesturesToShapeMesh"), useGestures), 0, 2, 2, 1);
      gesturesBox.setEnabled(Actor.getActor(info.getObject()) != null);
      RowContainer buttons = new RowContainer();
      content.add(buttons, 0, 3, 2, 1, new LayoutInfo());
      buttons.add(Translate.button("add", "...", this, "doAdd"));
      buttons.add(editButton = Translate.button("edit", "...", this, "doEdit"));
      buttons.add(deleteButton = Translate.button("delete", this, "doDelete"));
      buttons.add(Translate.button("ok", this, "doOk"));
      buttons.add(Translate.button("cancel", this, "dispose"));
      pack();
      UIUtilities.centerDialog(this, win);
      editButton.setEnabled(false);
      deleteButton.setEnabled(false);
      setVisible(true);
    }
    
    /** Fill in the list of constraints. */
    
    private void buildConstraintList()
    {
      Skeleton skeleton = info.getSkeleton();
      constraintList.removeAll();
      if (tempConstraints.size() == 0)
      {
        constraintList.add("(No Constraints)");
        constraintList.setEnabled(false);
        return;
      }
      constraintList.setEnabled(true);
      for (int i = 0; i < tempConstraints.size(); i++)
      {
        Constraint c = (Constraint) tempConstraints.elementAt(i);
        Joint j = skeleton.getJoint(c.jointID);
        if (c.target == null)
          constraintList.add(Translate.text("jointIsLocked", j.name));
        else
          constraintList.add(Translate.text("jointFollowsTarget", j.name, c.target.toString()));
      }
    }
    
    private void doAdd()
    {
      Constraint c = new Constraint(-1, null);
      if (editConstraint(c))
      {
        tempConstraints.addElement(c);
        buildConstraintList();
      }
    }
    
    private void doEdit()
    {
      if (constraintList.getSelectedIndex() > -1)
      {
        Constraint c = (Constraint) tempConstraints.elementAt(constraintList.getSelectedIndex());
        if (editConstraint(c))
          buildConstraintList();
      }
    }
    
    private void doDelete()
    {
      if (constraintList.getSelectedIndex() > -1)
        tempConstraints.removeElementAt(constraintList.getSelectedIndex());
      buildConstraintList();
    }
    
    private void doOk()
    {
      window.setUndoRecord(new UndoRecord(window, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()}));
      IKTrack.this.setName(nameField.getText());
      IKTrack.this.setUseGestures(gesturesBox.getState());
      constraints = tempConstraints;
      window.getScore().repaintAll();
      dispose();
    }

    private void mouseClicked(MouseClickedEvent ev)
    {
      if (ev.getClickCount() == 2)
        doEdit();
    }
    
    /** Update the components when a selection changes. */
    
    private void selectionChanged()
    {
      editButton.setEnabled(constraintList.getSelectedIndex() > -1);
      deleteButton.setEnabled(constraintList.getSelectedIndex() > -1);
    }
    
    /** Display a window for editing a single constraint. */
    
    private boolean editConstraint(Constraint c)
    {
      Skeleton skeleton = info.getSkeleton();
      Joint joint[] = skeleton.getJoints();
      BList jointList = new BList();
      for (int i = 0; i < joint.length; i++)
        jointList.add(joint[i].name);
      if (skeleton.findJointIndex(c.jointID) > -1)
        jointList.setSelected(skeleton.findJointIndex(c.jointID), true);
      jointList.setPreferredVisibleRows(8);
      RadioButtonGroup group = new RadioButtonGroup();
      BRadioButton lockedBox = new BRadioButton(Translate.text("locked"), c.target == null, group);
      final BRadioButton targetBox = new BRadioButton(Translate.text("followsTarget"), c.target != null, group);
      RowContainer targetRow = new RowContainer();
      targetRow.add(lockedBox);
      targetRow.add(targetBox);
      ObjectRef tempRef = (c.target == null ? new ObjectRef() : c.target);
      final ObjectRefSelector selector = new ObjectRefSelector(tempRef, window, Translate.text("targetForJoint"), info);
      Object listener = new Object() {
        private void processEvent(ValueChangedEvent ev)
        {
          selector.setEnabled(ev.getWidget() == targetBox);
        }
      };
      lockedBox.addEventLink(ValueChangedEvent.class, listener);
      targetBox.addEventLink(ValueChangedEvent.class, listener);
      selector.setEnabled(c.target != null);
      ComponentsDialog dlg = new ComponentsDialog(window, Translate.text("editConstraint"), new Widget []
          {UIUtilities.createScrollingList(jointList), targetRow, selector}, new String [] {Translate.text("applyConstraintTo"), Translate.text("constraintType"),
          Translate.text("targetForJoint")});
      if (!dlg.clickedOk() || jointList.getSelectedIndex() == -1)
        return false;
      if (jointList.getSelectedIndex() > -1)
        c.jointID = joint[jointList.getSelectedIndex()].id;
      if (lockedBox.getState())
        c.target = null;
      else
        c.target = selector.getSelection();
      return true;
    }
  }
}

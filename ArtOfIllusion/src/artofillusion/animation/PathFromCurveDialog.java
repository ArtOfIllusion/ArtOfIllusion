/* Copyright (C) 2001-2011 by Peter Eastman

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
import java.awt.*;
import java.text.*;
import java.util.Vector;

/** PathFromCurveDialog is a dialog box for describing how to set an animation path from
    a Curve object. */

public class PathFromCurveDialog extends BDialog
{
  private LayoutWindow window;
  private Scene theScene;
  private BList objList, curveList;
  private Vector objects, curves;
  private BCheckBox orientBox;
  private BComboBox spacingChoice;
  private ValueField startTimeField, endTimeField, startSpeedField, endSpeedField, accelField; 
  private BLabel speedLabel, lengthLabel; 
  private BButton okButton, cancelButton;
  private Vec3 subdiv[];
  private double curveLength;

  public PathFromCurveDialog(LayoutWindow win, Object sel[])
  {
    super(win, Translate.text("pathFromCurveTitle"), true);
    window = win;
    theScene = window.getScene();

    // Find the object and curves.
    
    objects = new Vector();
    curves = new Vector();
    objList = new BList();
    curveList = new BList();
    for (int i = 0; i < sel.length; i++)
    {
      ObjectInfo info = (ObjectInfo) sel[i];
      if (info.getObject() instanceof Curve && !(info.getObject() instanceof Tube))
      {
        curves.addElement(info);
        curveList.add(info.getName());
      }
      else
      {
        objects.addElement(info);
        objList.add(info.getName());
      }
    }
    objList.setMultipleSelectionEnabled(false);
    curveList.setMultipleSelectionEnabled(false);
    objList.setSelected(0, true);
    curveList.setSelected(0, true);
    objList.addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
    curveList.addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
    
    // Layout the dialog.

    FormContainer content = new FormContainer(4, 5);
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(2, 2, 2, 2), null));
    content.add(Translate.label("setPathOf"), 0, 0);
    content.add(UIUtilities.createScrollingList(objList), 1, 0);
    content.add(Translate.label("fromCurve"), 2, 0);
    content.add(UIUtilities.createScrollingList(curveList), 3, 0);
    content.add(orientBox = new BCheckBox(Translate.text("orientFollowsCurve"), true), 0, 1, 4, 1);
    orientBox.addEventLink(ValueChangedEvent.class, this, "selectionChanged");
    RowContainer keyRow = new RowContainer();
    content.add(keyRow, 0, 2, 4, 1);
    keyRow.add(Translate.label("keyframeSpacing"));
    keyRow.add(spacingChoice = new BComboBox(new String [] {
      Translate.text("uniformSpacing"),
      Translate.text("constantSpeed"),
      Translate.text("constantAccel")
    }));
    spacingChoice.setSelectedIndex(1);
    spacingChoice.addEventLink(ValueChangedEvent.class, this, "selectionChanged");
    FormContainer fieldPanel = new FormContainer(2, 6);
    content.add(fieldPanel, 0, 3, 4, 1);
    LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
    fieldPanel.add(lengthLabel = Translate.label("curveLength"), 0, 0, leftLayout);
    fieldPanel.add(new BLabel(Translate.text("StartTime")+":"), 0, 1, leftLayout);
    fieldPanel.add(new BLabel(Translate.text("EndTime")+":"), 0, 2, leftLayout);
    fieldPanel.add(speedLabel = Translate.label("initialSpeed"), 0, 3, leftLayout);
    fieldPanel.add(Translate.label("finalSpeed"), 0, 4, leftLayout);
    fieldPanel.add(Translate.label("acceleration"), 0, 5, leftLayout);
    fieldPanel.add(lengthLabel = new BLabel(), 1, 0, rightLayout);
    fieldPanel.add(startTimeField = new ValueField(0.0, ValueField.NONE, 5), 1, 1, rightLayout);
    fieldPanel.add(endTimeField = new ValueField(1.0, ValueField.NONE, 5), 1, 2, rightLayout);
    fieldPanel.add(startSpeedField = new ValueField(1.0, ValueField.NONE, 5), 1, 3, rightLayout);
    fieldPanel.add(endSpeedField = new ValueField(1.0, ValueField.NONE, 5), 1, 4, rightLayout);
    fieldPanel.add(accelField = new ValueField(0.0, ValueField.NONE, 5), 1, 5, rightLayout);
    startTimeField.addEventLink(ValueChangedEvent.class, this, "adjustTextFields");
    endTimeField.addEventLink(ValueChangedEvent.class, this, "adjustTextFields");
    startSpeedField.addEventLink(ValueChangedEvent.class, this, "adjustTextFields");
    endSpeedField.addEventLink(ValueChangedEvent.class, this, "adjustTextFields");
    accelField.addEventLink(ValueChangedEvent.class, this, "adjustTextFields");
    endTimeField.setValue(curveLength);
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 4, 4, 1);
    buttons.add(okButton = Translate.button("ok", this, "doOk"));
    buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
    
    // Show the dialog.
    
    pack();
    updateComponents();
    UIUtilities.centerDialog(this, win);
    setVisible(true);
  }

  private void selectionChanged(WidgetEvent ev)
  {
    if (ev.getWidget() == curveList || ev.getWidget() == spacingChoice)
      adjustTextFields(new ValueChangedEvent(startTimeField));
    updateComponents();
    if (ev.getWidget() == spacingChoice)
      startTimeField.requestFocus();
  }
  
  private void doOk()
  {
    addTracks();
    dispose();
  }
  
  /** Update which components are visible and enabled. */
  
  private void updateComponents()
  {
    int spacing = spacingChoice.getSelectedIndex();
    speedLabel.setText(Translate.text(spacing == 2 ? "initialSpeed" : "speed"));
    startSpeedField.setEnabled(spacing != 0);
    endSpeedField.setEnabled(spacing == 2);
    accelField.setEnabled(spacing == 2);
    okButton.setEnabled(objList.getSelectedIndex() > -1 && curveList.getSelectedIndex() > -1 &&
        (spacing == 0 || startTimeField.getValue() != endTimeField.getValue()));
    if (curveList.getSelectedIndex() > -1)
    {
      ObjectInfo info = (ObjectInfo) curves.elementAt(curveList.getSelectedIndex());
      Curve cv = (Curve) info.getObject();
      MeshVertex vert[] = cv.getVertices();
      Vec3 v[] = new Vec3 [vert.length];
      Mat4 trans = info.getCoords().fromLocal();
      for (int i = 0; i < v.length; i++)
        v[i] = trans.times(vert[i].r);
      subdiv = new Curve(v, cv.getSmoothness(), cv.getSmoothingMethod(), cv.isClosed()).subdivideCurve(4).getVertexPositions();
      curveLength = 0.0;
      for (int i = 1; i < subdiv.length; i++)
        curveLength += subdiv[i].distance(subdiv[i-1]);
      if (cv.isClosed())
        curveLength += subdiv[subdiv.length-1].distance(subdiv[0]);
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMaximumFractionDigits(3);
      lengthLabel.setText(nf.format(curveLength));
      layoutChildren();
    }
  }
    
  /** When one value is edited, recalculate the others. */
  
  private void adjustTextFields(ValueChangedEvent ev)
  {
    Widget src = ev.getWidget();
    int spacing = spacingChoice.getSelectedIndex();
    double startTime = startTimeField.getValue(), endTime = endTimeField.getValue();
    double startSpeed = startSpeedField.getValue(), endSpeed = endSpeedField.getValue();
    double accel = accelField.getValue(), time = endTime-startTime;

    if (startTime == endTime)
    {
      okButton.setEnabled(false);
      return;
    }
    if (spacing == 0)
      return;
    if (spacing == 1)
    {
      if (src == startSpeedField)
        endTimeField.setValue(startTime+curveLength/startSpeed);
      else
        startSpeedField.setValue(curveLength/(endTime-startTime));
      okButton.setEnabled(true);
      return;
    }
    if (src == accelField)
    {
      if (accel == 0.0)
      {
        if (endSpeed == 0.0)
          endSpeed = startSpeed;
        else
          startSpeed = endSpeed;
        if (startSpeed == 0.0)
        {
          okButton.setEnabled(false);
          return;
        }
        time = curveLength/startSpeed;
      }
      else
      {
        time = (-startSpeed+Math.sqrt(startSpeed*startSpeed+2.0*accel*curveLength))/accel;
        endSpeed = startSpeed + time*accel;
      }
      endTimeField.setValue(startTime+time);
      startSpeedField.setValue(startSpeed);
      endSpeedField.setValue(endSpeed);
      okButton.setEnabled(true);
      return;
    }
    if (startSpeed == 0.0 && endSpeed == 0.0)
    {
      okButton.setEnabled(false);
      return;
    }
    if (src == startSpeedField || src == endSpeedField)
    {
      accel = (endSpeed*endSpeed-startSpeed*startSpeed)/(2.0*curveLength);
      if (accel == 0.0)
        endTime = startTime + curveLength/startSpeed;
      else
        endTime = startTime + (endSpeed-startSpeed)/accel;
      accelField.setValue(accel);
      endTimeField.setValue(endTime);
    }
    else
    {
      accel = 2.0*(curveLength-startSpeed*time)/(time*time);
      endSpeed = startSpeed + accel*time;
      accelField.setValue(accel);
      endSpeedField.setValue(endSpeed);
    }
    okButton.setEnabled(true);
  }
  
  /** Find the keyframes for the new tracks. */
  
  private void addTracks()
  {
    ObjectInfo info = (ObjectInfo) curves.elementAt(curveList.getSelectedIndex());
    Mat4 trans = info.getCoords().fromLocal();
    Curve cv = (Curve) info.getObject();
    MeshVertex vert[] = cv.getVertices();
    int n = (cv.isClosed() ? vert.length+1 : vert.length);
    double dist[] = new double [n], time[] = new double [n];
    String curveName = info.getName();
    
    // Find the distance along the curve of each keyframe.
    
    int k = (cv.getSmoothingMethod() == Mesh.NO_SMOOTHING ? 1 : 16);
    for (int i = 1; i < vert.length; i++)
      {
        dist[i] = dist[i-1];
        for (int j = 0; j < k; j++)
          dist[i] += subdiv[(i-1)*k+j].distance(subdiv[(i-1)*k+1+j]);
      }
    if (cv.isClosed())
      dist[n-1] = curveLength;
    
    // Find the time of each keyframe.
    
    int spacing = spacingChoice.getSelectedIndex(), fps = theScene.getFramesPerSecond();
    double startTime = startTimeField.getValue(), endTime = endTimeField.getValue();
    double startSpeed = startSpeedField.getValue(), endSpeed = endSpeedField.getValue();
    double accel = accelField.getValue(), totalTime = endTime-startTime;
    for (int i = 0; i < n; i++)
      {
        if (spacing == 0)
          time[i] = startTime + (i*totalTime)/(n-1);
        else if (spacing == 1 || accel == 0.0)
          time[i] = startTime + dist[i]/startSpeed;
        else
          time[i] = startTime + (-startSpeed+Math.sqrt(startSpeed*startSpeed+2.0*accel*dist[i]))/accel;
        time[i] = Math.round(time[i]*fps)/(double) fps;
      }
    
    // Create the position track.
    
    info = (ObjectInfo) objects.elementAt(objList.getSelectedIndex());
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.SET_TRACK_LIST, new Object [] {info, info.getTracks()}));
    float smoothness[] = cv.getSmoothness();
    PositionTrack tr = new PositionTrack(info);
    tr.setName(curveName+" Position");
    for (int i = 0; i < vert.length; i++)
      tr.setKeyframe(time[i], new VectorKeyframe(trans.times(vert[i].r)), new Smoothness(smoothness[i]));
    if (n > vert.length)
      tr.setKeyframe(time[vert.length], new VectorKeyframe(trans.times(vert[0].r)), new Smoothness(smoothness[0]));
    if (cv.getSmoothingMethod() == Mesh.NO_SMOOTHING)
      tr.setSmoothingMethod(Timecourse.LINEAR);
    else if (cv.getSmoothingMethod() == Mesh.INTERPOLATING)
      tr.setSmoothingMethod(Timecourse.INTERPOLATING);
    else
      tr.setSmoothingMethod(Timecourse.APPROXIMATING);
    info.addTrack(tr, 0);
    
    // Create the rotation track.
    
    if (orientBox.getState())
      {
	RotationTrack tr2 = new RotationTrack(info);
        RotationKeyframe lastKey, nextKey;
	Vec3 zdir, updir;

        tr2.setName(curveName+" Rotation");
        if (k != 1 && cv.isClosed())
          zdir = subdiv[1].minus(subdiv[subdiv.length-1]);
        else
          zdir = subdiv[1].minus(subdiv[0]);
        zdir.normalize();
        updir = Vec3.vy();
        double dot = zdir.dot(updir);
	if (Math.abs(dot) > 0.99)
	  {
	    updir = Vec3.vx();
	    dot = zdir.dot(updir);
	  }
	updir.subtract(zdir.times(dot));
	updir.normalize();
        CoordinateSystem coords = new CoordinateSystem(new Vec3(), zdir, updir);
	tr2.setKeyframe(time[0], lastKey = new RotationKeyframe(coords), new Smoothness(smoothness[0]));
	
	double d = 0.0, t;
	int interval = (cv.getSmoothingMethod() == Mesh.NO_SMOOTHING ? 1 : 8);
	for (int i = 1; i < subdiv.length; i++)
	  {
	    if (i == subdiv.length-1)
	      {
	        if (!cv.isClosed())
	          zdir = subdiv[subdiv.length-1].minus(subdiv[subdiv.length-2]);
	        else if (k == 1)
	          zdir = subdiv[0].minus(subdiv[subdiv.length-1]);
	        else
	          zdir = subdiv[0].minus(subdiv[subdiv.length-2]);
	      }
	    else if (k == 1)
	      zdir = subdiv[i+1].minus(subdiv[i]);
	    else
	      zdir = subdiv[i+1].minus(subdiv[i-1]);
	    zdir.normalize();
	    updir = updir.minus(zdir.times(zdir.dot(updir)));
	    updir.normalize();
	    d += subdiv[i-1].distance(subdiv[i]);
	    if (i % interval == 0)
	      {
		coords.setOrientation(zdir, updir);
		nextKey = new RotationKeyframe(coords);
		if (nextKey.x-lastKey.x > 180.0)
		  nextKey.x -= 360.0;
		if (nextKey.x-lastKey.x < -180.0)
		  nextKey.x += 360.0;
		if (nextKey.y-lastKey.y > 180.0)
		  nextKey.y -= 360.0;
		if (nextKey.y-lastKey.y < -180.0)
		  nextKey.y += 360.0;
		if (nextKey.z-lastKey.z > 180.0)
		  nextKey.z -= 360.0;
		if (nextKey.z-lastKey.z < -180.0)
		  nextKey.z += 360.0;
		if (spacing == 0)
		  t = startTime + (i*totalTime)/(subdiv.length-1);
		else if (spacing == 1 || accel == 0.0)
		  t = startTime + d/startSpeed;
		else
		  t = startTime + (-startSpeed+Math.sqrt(startSpeed*startSpeed+2.0*accel*d))/accel;
		t = Math.round(t*fps)/(double) fps;
		validateKeyframe(lastKey, nextKey);
		if (k == 1 || (i%k == 0 && smoothness[i/k] == 0.0))
		  tr2.setKeyframe(t-1.0/fps, new RotationKeyframe(lastKey.x, lastKey.y, lastKey.z), new Smoothness());
		tr2.setKeyframe(t, nextKey,  new Smoothness());
		lastKey = nextKey;
	      }
	  }
	if (cv.getSmoothingMethod() == Mesh.NO_SMOOTHING)
	  tr2.setSmoothingMethod(Timecourse.LINEAR);
	else if (cv.getSmoothingMethod() == Mesh.INTERPOLATING)
	  tr2.setSmoothingMethod(Timecourse.INTERPOLATING);
	else
	  tr2.setSmoothingMethod(Timecourse.APPROXIMATING);
	tr2.setUseQuaternion(true);
	info.addTrack(tr2, 1);
      }
    startTime = Math.round(startTime*fps)/(double) fps;
    window.getScore().rebuildList();
    window.setTime(startTime);
  }
  
  /* Given two successive keyframes, make sure that they will interpolate correctly (such that
     the y axis remains in a plane)), and modify the second one if necessary. */
  
  private void validateKeyframe(RotationKeyframe r1, RotationKeyframe r2)
  {
    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    RotationKeyframe r3 = (RotationKeyframe) r1.blend(r2, 0.75, 0.25);
    Vec3 u1, u2, u3, z1, z2, z3, mid = new Vec3();
    boolean upcheck, zcheck;
    
    coords.setOrientation(r1.x, r1.y, r1.z);
    u1 = new Vec3(coords.getUpDirection());
    z1 = new Vec3(coords.getZDirection());
    coords.setOrientation(r2.x, r2.y, r2.z);
    u2 = new Vec3(coords.getUpDirection());
    z2 = new Vec3(coords.getZDirection());
    coords.setOrientation(r3.x, r3.y, r3.z);
    u3 = new Vec3(coords.getUpDirection());
    z3 = new Vec3(coords.getZDirection());
    upcheck = (u1.dot(u3) < 0.0 || u2.dot(u3) < 0.0);
    zcheck = (z1.dot(z3) < 0.0 || z2.dot(z3) < 0.0);
    mid.set(u1.x+u2.x, u1.y+u2.y, u1.z+u2.z);
    mid.normalize();
    upcheck = (mid.dot(u3) < 0.2);
    mid.set(z1.x+z2.x, z1.y+z2.y, z1.z+z2.z);
    mid.normalize();
    zcheck = (mid.dot(z3) < 0.2);
    if (upcheck || zcheck)
      {
        if (r2.z < r1.z)
          r2.z += 360.0;
        else
          r2.z -= 360.0;
      }
  }
}

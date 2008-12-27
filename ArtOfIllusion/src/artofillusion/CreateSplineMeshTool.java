/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;

/** CreateSplineMeshTool is an EditingTool used for creating SplineMesh objects. */

public class CreateSplineMeshTool extends EditingTool
{
  static int counter = 1;
  static final int FLAT = 0;
  static final int CYLINDER = 1;
  static final int TORUS = 2;

  boolean shiftDown;
  Point clickPoint;
  int usize = 5, vsize = 5, shape = FLAT, smoothing = Mesh.APPROXIMATING;
  double thickness = 0.5;

  public CreateSplineMeshTool(LayoutWindow fr)
  {
    super(fr);
    initButton("splineMesh");
  }

  public void activate()
  {
    super.activate();
    setHelpText();
  }

  private void setHelpText()
  {
    String shapeDesc, smoothingDesc;
    if (shape == FLAT)
      shapeDesc = "flat";
    else if (shape == CYLINDER)
      shapeDesc = "cylindrical";
    else
      shapeDesc = "toroidal";
    if (smoothing == Mesh.INTERPOLATING)
      smoothingDesc = "interpolating";
    else
      smoothingDesc = "approximating";
    theWindow.setHelpText(Translate.text("createSplineMeshTool.helpText",
      new Object [] {Integer.toString(usize), Integer.toString(vsize), 
      Translate.text("createSplineMeshTool."+shapeDesc).toLowerCase(), Translate.text("menu."+smoothingDesc).toLowerCase()}));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("createSplineMeshTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint = e.getPoint();
    shiftDown = e.isShiftDown();
    ((SceneViewer) view).beginDraggingBox(clickPoint, shiftDown);
  }
  
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    Scene theScene = ((LayoutWindow) theWindow).getScene();
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    Vec3 v1, v2, v3, orig, xdir, ydir, zdir;
    double xsize, ysize;
    int i;
    
    if (shiftDown)
      {
	if (Math.abs(dragPoint.x-clickPoint.x) > Math.abs(dragPoint.y-clickPoint.y))
	  {
	    if (dragPoint.y < clickPoint.y)
	      dragPoint.y = clickPoint.y - Math.abs(dragPoint.x-clickPoint.x);
	    else
	      dragPoint.y = clickPoint.y + Math.abs(dragPoint.x-clickPoint.x);
          }
        else
          {
            if (dragPoint.x < clickPoint.x)
              dragPoint.x = clickPoint.x - Math.abs(dragPoint.y-clickPoint.y);
            else
              dragPoint.x = clickPoint.x + Math.abs(dragPoint.y-clickPoint.y);
          }
      }
    if (dragPoint.x == clickPoint.x || dragPoint.y == clickPoint.y)
      {
        ((SceneViewer) view).repaint();
        return;
      }
    v1 = cam.convertScreenToWorld(clickPoint, Camera.DEFAULT_DISTANCE_TO_SCREEN);
    v2 = cam.convertScreenToWorld(new Point(dragPoint.x, clickPoint.y), Camera.DEFAULT_DISTANCE_TO_SCREEN);
    v3 = cam.convertScreenToWorld(dragPoint, Camera.DEFAULT_DISTANCE_TO_SCREEN);
    orig = v1.plus(v3).times(0.5);
    if (dragPoint.x < clickPoint.x)
      xdir = v1.minus(v2);
    else
      xdir = v2.minus(v1);
    if (dragPoint.y < clickPoint.y)
      ydir = v3.minus(v2);
    else
      ydir = v2.minus(v3);
    xsize = xdir.length();
    ysize = ydir.length();
    xdir = xdir.times(1.0/xsize);
    ydir = ydir.times(1.0/ysize);
    zdir = xdir.cross(ydir);
    Vec3 v[][] = getMeshPoints(xsize, ysize);
    float usmoothness[] = new float [usize], vsmoothness[] = new float [vsize];
    for (i = 0; i < usize; i++)
      usmoothness[i] = 1.0f;
    for (i = 0; i < vsize; i++)
      vsmoothness[i] = 1.0f;
    SplineMesh obj = new SplineMesh(v, usmoothness, vsmoothness, smoothing, shape != FLAT, shape == TORUS);
    ObjectInfo info = new ObjectInfo(obj, new CoordinateSystem(orig, zdir, ydir), "Spline Mesh "+(counter++));
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    UndoRecord undo = new UndoRecord(theWindow, false);
    int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
    ((LayoutWindow) theWindow).addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    theWindow.setUndoRecord(undo);
    ((LayoutWindow) theWindow).setSelection(theScene.getNumObjects()-1);
    theWindow.updateImage();
  }

  private Vec3 [][] getMeshPoints(double xsize, double ysize)
  {
    Vec3 v[][] = new Vec3 [usize][vsize];
    int i, j;
    
    if (shape == FLAT)
      {
        double xmin = -xsize*0.5, ymin = -ysize*0.5;
        double uscale = 1.0/(usize-1), vscale = 1.0/(vsize-1);

        for (i = 0; i < usize; i++)
          for (j = 0; j < vsize; j++)
            v[i][j] = new Vec3(xsize*i*uscale+xmin, ysize*j*vscale+ymin, 0.0);
      }
    else if (shape == CYLINDER)
      {
        double rad = xsize*0.5, ymin = -ysize*0.5;
        double uscale = 2.0*Math.PI/usize, vscale = 1.0/(vsize-1);

        for (i = 0; i < usize; i++)
          for (j = 0; j < vsize; j++)
            v[i][j] = new Vec3(rad*Math.sin(uscale*i), ysize*j*vscale+ymin, rad*Math.cos(uscale*i));
      }
    else
      {
        double rad = Math.min(xsize, ysize)*0.25*thickness;
        double radx = xsize*0.5 - rad, rady = ysize*0.5 - rad;
        double uscale = 2.0*Math.PI/usize, vscale = 2.0*Math.PI/vsize;
        Vec3 vr = new Vec3(), vc = new Vec3();

        for (i = 0; i < usize; i++)
          {
            vc.set(radx*Math.cos(uscale*i), rady*Math.sin(uscale*i), 0.0);
            vr.set(rad*Math.cos(uscale*i), rad*Math.sin(uscale*i), 0.0);
            for (j = 0; j < vsize; j++)
              v[i][j] = new Vec3(vc.x+vr.x*Math.cos(vscale*j), vc.y+vr.y*Math.cos(vscale*j), rad*Math.sin(vscale*j));
          }
      }
    return v;
  }

  public void iconDoubleClicked()
  {
    final ValueSlider thicknessSlider = new ValueSlider(0.0, 1.0, 100, thickness);
    int i, minu, minv;
    
    thicknessSlider.setEnabled(shape == TORUS);
    ValueField usizeField = new ValueField((double) usize, ValueField.POSITIVE+ValueField.INTEGER);
    ValueField vsizeField = new ValueField((double) vsize, ValueField.POSITIVE+ValueField.INTEGER);
    BComboBox smoothingChoice = new BComboBox(new String [] {
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    if (smoothing == Mesh.INTERPOLATING)
      smoothingChoice.setSelectedIndex(0);
    else
      smoothingChoice.setSelectedIndex(1);
    final BComboBox shapeChoice = new BComboBox(new String [] {
      Translate.text("Flat"),
      Translate.text("Cylinder"),
      Translate.text("Torus")
    });
    if (shape == FLAT)
      shapeChoice.setSelectedIndex(0);
    else if (shape == CYLINDER)
      shapeChoice.setSelectedIndex(1);
    else
      shapeChoice.setSelectedIndex(2);
    shapeChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        thicknessSlider.setEnabled(shapeChoice.getSelectedIndex() == 2);
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("selectMeshSizeShape"), 
                new Widget [] {usizeField, vsizeField, shapeChoice, smoothingChoice, thicknessSlider},
                new String [] {Translate.text("uSize"), Translate.text("vSize"), Translate.text("Shape"), Translate.text("Smoothing Method"), Translate.text("Thickness")});
    if (!dlg.clickedOk())
      return;
    minu = shapeChoice.getSelectedIndex() == 0 ? 2 : 3;
    minv = shapeChoice.getSelectedIndex() == 2 ? 3 : 2;
    if (usizeField.getValue() < minu)
    {
      new BStandardDialog("", Translate.text("uSizeTooSmall", Integer.toString(minu)), BStandardDialog.ERROR).showMessageDialog(theFrame);
      return;
    }
    if (vsizeField.getValue() < minv)
    {
      new BStandardDialog("", Translate.text("vSizeTooSmall", Integer.toString(minv)), BStandardDialog.ERROR).showMessageDialog(theFrame);
      return;
    }
    usize = (int) usizeField.getValue();
    vsize = (int) vsizeField.getValue();
    i = smoothingChoice.getSelectedIndex();
    if (i == 0)
      smoothing = Mesh.INTERPOLATING;
    else
      smoothing = Mesh.APPROXIMATING;
    i = shapeChoice.getSelectedIndex();
    if (i == 0)
      shape = FLAT;
    else if (i == 1)
      shape = CYLINDER;
    else
      shape = TORUS;
    thickness = thicknessSlider.getValue();
    setHelpText();
  }
}
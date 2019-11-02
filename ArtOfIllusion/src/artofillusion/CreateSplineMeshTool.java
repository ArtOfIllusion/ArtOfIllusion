/* Copyright (C) 1999-2008 by Peter Eastman
   Changes copyright (C) 2019 by Petri Ihalainen

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

  private boolean shiftDown, equilateral, centered;
  private Point clickPoint;
  private ObjectInfo info;
  private SplineMesh mesh;
  private Vec3 ydir, zdir;
  private int usize = 5, vsize = 5, shape = FLAT, smoothing = Mesh.APPROXIMATING;
  private int[] usizefor = new int[] {5, 8, 8};
  private int[] vsizefor = new int[] {5, 5, 8};
  private double thickness = 0.5;

  public CreateSplineMeshTool(LayoutWindow fr)
  {
    super(fr);
    initButton("splineMesh");
  }

  @Override
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

  @Override
  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  @Override
  public String getToolTipText()
  {
    return Translate.text("createSplineMeshTool.tipText");
  }

  @Override
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint  = e.getPoint();
    equilateral = e.isShiftDown();
    centered    = e.isControlDown();
    ydir = Vec3.vy();
    zdir = Vec3.vz();
  }

  @Override
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    ydir.set(cam.getCameraCoordinates().getUpDirection());
    zdir.set(cam.getCameraCoordinates().getZDirection());
    zdir.scale(-1.0);

    if (info == null)
    {
      // Create the initial mesh, if the mouse has moved enough. The limit is there to reduce 
      // the probability of accidentally creating zero size objects.

      if (Math.abs(dragPoint.x-clickPoint.x) + Math.abs(dragPoint.y-clickPoint.y) > 3)
      {
        Scene theScene = ((LayoutWindow) theWindow).getScene();
        Vec3 v[][] = getMeshPoints(1.0, 1.0);
        float usmoothness[] = new float [usize], vsmoothness[] = new float [vsize];
        int i;
        for (i = 0; i < usize; i++)
          usmoothness[i] = 1.0f;
        for (i = 0; i < vsize; i++)
          vsmoothness[i] = 1.0f;
        mesh = new SplineMesh(v, usmoothness, vsmoothness, smoothing, shape != FLAT, shape == TORUS);
        info = new ObjectInfo(mesh, new CoordinateSystem(), "Spline Mesh "+(counter++));
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
        UndoRecord undo = new UndoRecord(theWindow, false);
        int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
        ((LayoutWindow) theWindow).addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        theWindow.setUndoRecord(undo);
        ((LayoutWindow) theWindow).setSelection(theScene.getNumObjects()-1);
      }
      else
        return;
    }

    // Determine the size and position for the sphere.

    Vec3 v1, v2, v3, orig;
    double xsize, ysize, zsize;

    if (equilateral)
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
    v1 = cam.convertScreenToWorld(clickPoint, view.getDistToPlane());
    v2 = cam.convertScreenToWorld(new Point(dragPoint.x, clickPoint.y), view.getDistToPlane());
    v3 = cam.convertScreenToWorld(dragPoint, view.getDistToPlane());

    if (centered)
    {
      orig  = v1;
      xsize = v2.minus(v1).length()*2.0; 
      ysize = v2.minus(v3).length()*2.0;
    }
    else
    {
      orig  = v1.plus(v3).times(0.5);
      xsize = v2.minus(v1).length(); 
      ysize = v2.minus(v3).length();
    }
    mesh.setVertexPositions(getMeshPoints(xsize, ysize));

    info.getCoords().setOrigin(orig);
    info.getCoords().setOrientation(zdir, ydir);
    info.clearCachedMeshes();
    theWindow.setModified();
    theWindow.updateImage();
  }

  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    mesh = null;
    info = null;
    System.gc();
  }

  private Vec3[][] getMeshPoints(double xsize, double ysize)
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

  @Override
  public void iconDoubleClicked()
  {
    final ValueSlider thicknessSlider = new ValueSlider(0.0, 1.0, 100, thickness);
    int i, minu, minv;

    thicknessSlider.setEnabled(shape == TORUS);
    final ValueField usizeField = new ValueField((double) usizefor[shape], ValueField.POSITIVE+ValueField.INTEGER);
    final ValueField vsizeField = new ValueField((double) vsizefor[shape], ValueField.POSITIVE+ValueField.INTEGER);
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
        usizeField.setValue(usizefor[shapeChoice.getSelectedIndex()]);
        vsizeField.setValue(vsizefor[shapeChoice.getSelectedIndex()]);
      }
    });
    usizeField.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        usizefor[shapeChoice.getSelectedIndex()] = (int)usizeField.getValue();
      }
    });

    vsizeField.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        vsizefor[shapeChoice.getSelectedIndex()] = (int)vsizeField.getValue();
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("selectMeshSizeShape"),
                           new Widget [] {shapeChoice, 
                                          usizeField, 
                                          vsizeField, 
                                          smoothingChoice, 
                                          thicknessSlider},
                           new String [] {Translate.text("Shape"), 
                                          Translate.text("uSize"), 
                                          Translate.text("vSize"), 
                                          Translate.text("Smoothing Method"), 
                                          Translate.text("Thickness")});
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
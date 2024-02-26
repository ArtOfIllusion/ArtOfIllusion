/* Copyright (C) 1999-2008 by Peter Eastman
   Changes copyright (C) 2019 by Petri Ihalainen
   Changes copyright (C) 2020-2024 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.Arrays;

/** CreateSplineMeshTool is an EditingTool used for creating SplineMesh objects. */

public class CreateSplineMeshTool extends EditingTool
{
  static int counter = 1;
  static final int FLAT = 0;
  static final int CYLINDER = 1;
  static final int TORUS = 2;

  private boolean equilateral;
  private boolean centered;
  private Point clickPoint;
  private ObjectInfo info;
  private SplineMesh mesh;
  private Vec3 yDir;
  private Vec3 zDir;
  private int uSize = 5;
  private int vSize = 5;
  private int shape = FLAT;
  private int smoothing = Mesh.APPROXIMATING;
  private final int[] usizefor = new int[] {5, 8, 8};
  private final int[] vsizefor = new int[] {5, 5, 8};
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
    String shapeDesc;
    String smoothingDesc;
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

    String helpText = Translate.text("createSplineMeshTool.helpText", uSize, vSize,
            Translate.text("createSplineMeshTool." + shapeDesc).toLowerCase(), Translate.text("menu." + smoothingDesc).toLowerCase());
    theWindow.setHelpText(helpText);
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
    yDir = Vec3.vy();
    zDir = Vec3.vz();
  }

  @Override
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    yDir.set(cam.getCameraCoordinates().getUpDirection());
    zDir.set(cam.getCameraCoordinates().getZDirection());
    zDir.scale(-1.0);

    if (info == null)
    {
      // Create the initial mesh, if the mouse has moved enough. The limit is there to reduce 
      // the probability of accidentally creating zero size objects.

      if (Math.abs(dragPoint.x - clickPoint.x) + Math.abs(dragPoint.y - clickPoint.y) > 3)
      {
        Scene theScene = ((LayoutWindow) theWindow).getScene();
        Vec3[][] v = getMeshPoints(1.0, 1.0);
        float[] uSmoothness = new float [uSize];
        float[] vSmoothness = new float [vSize];

        Arrays.fill(uSmoothness, 1.0f);
        Arrays.fill(vSmoothness, 1.0f);

        mesh = new SplineMesh(v, uSmoothness, vSmoothness, smoothing, shape != FLAT, shape == TORUS);
        info = new ObjectInfo(mesh, new CoordinateSystem(), "Spline Mesh " + (counter++));

        UndoRecord undo = new UndoRecord(theWindow, false);
        int[] sel = ((LayoutWindow) theWindow).getSelectedIndices();
        ((LayoutWindow) theWindow).addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, sel);
        theWindow.setUndoRecord(undo);
        ((LayoutWindow) theWindow).setSelection(theScene.getNumObjects()-1);
      }
      else
        return;
    }

    // Determine the size and position for the SplineMesh.

    Vec3 v1;
    Vec3 v2;
    Vec3 v3;
    Vec3 orig;

    double xSize;
    double ySize;

    if (equilateral)
    {
      if (Math.abs(dragPoint.x - clickPoint.x) > Math.abs(dragPoint.y - clickPoint.y))
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
      xSize = v2.minus(v1).length() * 2.0;
      ySize = v2.minus(v3).length() * 2.0;
    }
    else
    {
      orig  = v1.plus(v3).times(0.5);
      xSize = v2.minus(v1).length();
      ySize = v2.minus(v3).length();
    }
    mesh.setVertexPositions(getMeshPoints(xSize, ySize));

    info.getCoords().setOrigin(orig);
    info.getCoords().setOrientation(zDir, yDir);
    info.clearCachedMeshes();
    theWindow.setModified();

    theWindow.updateImage();
  }

  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    mesh = null;
    info = null;
  }

  private Vec3[][] getMeshPoints(double xSize, double ySize)
  {
    Vec3[][] v = new Vec3 [uSize][vSize];


    if (shape == FLAT)
    {
      double xMin = -xSize * 0.5;
      double yMin = -ySize * 0.5;
      double uScale = 1.0/(uSize -1);
      double vScale = 1.0/(vSize -1);

      for (int i = 0; i < uSize; i++)
        for (int j = 0; j < vSize; j++)
          v[i][j] = new Vec3(xSize * i * uScale + xMin, ySize * j * vScale + yMin, 0.0);
    }
    else if (shape == CYLINDER)
    {
      double rad = xSize * 0.5;
      double yMin = -ySize * 0.5;
      double uScale = 2.0 * Math.PI / uSize;
      double vScale = 1.0 / (vSize - 1);

      for (int i = 0; i < uSize; i++)
        for (int j = 0; j < vSize; j++)
          v[i][j] = new Vec3(rad * Math.sin(uScale * i), ySize * j * vScale + yMin, rad * Math.cos(uScale * i));
    }
    else
    {
      double rad = Math.min(xSize, ySize) * 0.25 * thickness;
      double radX = xSize * 0.5 - rad;
      double radY = ySize * 0.5 - rad;
      double uScale = 2.0 * Math.PI / uSize;
      double vScale = 2.0 * Math.PI / vSize;
      Vec3 vr = new Vec3();
      Vec3 vc = new Vec3();

      for (int i = 0; i < uSize; i++)
      {
        double uScaleI = uScale * i;
        vc.set(radX * Math.cos(uScaleI), radY * Math.sin(uScaleI), 0.0);
        vr.set(rad * Math.cos(uScaleI), rad * Math.sin(uScaleI), 0.0);
        for (int j = 0; j < vSize; j++) {
            double vScaleJ = vScale * j;
            v[i][j] = new Vec3(vc.x + vr.x * Math.cos(vScaleJ), vc.y + vr.y * Math.cos(vScaleJ), rad * Math.sin(vScaleJ));
        }
      }
    }
    return v;
  }

  @Override
  public void iconDoubleClicked()
  {

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
    final ValueField uSizeField = new ValueField((double) usizefor[shape], ValueField.POSITIVE + ValueField.INTEGER);
    final ValueField vSizeField = new ValueField((double) vsizefor[shape], ValueField.POSITIVE + ValueField.INTEGER);
    BComboBox smoothingChoice = new BComboBox(new String [] {
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    if (smoothing == Mesh.INTERPOLATING)
      smoothingChoice.setSelectedIndex(0);
    else
      smoothingChoice.setSelectedIndex(1);
    final ValueSlider thicknessSlider = new ValueSlider(0.0, 1.0, 100, thickness);
    thicknessSlider.setEnabled(shape == TORUS);

    shapeChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        thicknessSlider.setEnabled(shapeChoice.getSelectedIndex() == 2);
        uSizeField.setValue(usizefor[shapeChoice.getSelectedIndex()]);
        vSizeField.setValue(vsizefor[shapeChoice.getSelectedIndex()]);
      }
    });
    uSizeField.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        usizefor[shapeChoice.getSelectedIndex()] = (int)uSizeField.getValue();
      }
    });
    vSizeField.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        vsizefor[shapeChoice.getSelectedIndex()] = (int)vSizeField.getValue();
      }
    });

    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("selectMeshSizeShape"),
                           new Widget [] {shapeChoice, 
                                          uSizeField,
                                          vSizeField,
                                          smoothingChoice, 
                                          thicknessSlider},
                           new String [] {Translate.text("Shape"), 
                                          Translate.text("uSize"), 
                                          Translate.text("vSize"), 
                                          Translate.text("Smoothing Method"), 
                                          Translate.text("Thickness")});
    if (!dlg.clickedOk())
      return;

    int minU = shapeChoice.getSelectedIndex() == 0 ? 2 : 3;
    int minV = shapeChoice.getSelectedIndex() == 2 ? 3 : 2;
    if (uSizeField.getValue() < minU)
    {
      new BStandardDialog("", Translate.text("uSizeTooSmall", minU), BStandardDialog.ERROR).showMessageDialog(theFrame);
      return;
    }
    if (vSizeField.getValue() < minV)
    {
      new BStandardDialog("", Translate.text("vSizeTooSmall", minV), BStandardDialog.ERROR).showMessageDialog(theFrame);
      return;
    }
    uSize = (int) uSizeField.getValue();
    vSize = (int) vSizeField.getValue();

    if (smoothingChoice.getSelectedIndex() == 0)
      smoothing = Mesh.INTERPOLATING;
    else
      smoothing = Mesh.APPROXIMATING;

    switch (shapeChoice.getSelectedIndex())
    {
      case 2:
        shape = TORUS;
        break;
      case 1:
        shape = CYLINDER;
        break;
      default:
        shape = FLAT; // The most simple case is the default
    }
    thickness = thicknessSlider.getValue();
    setHelpText();
  }
}

/* Copyright (C) 1999-2008 by Peter Eastman
   Modification, Copyright (C) 2020 Petri Ihalainen
   Changes copyright (C) 2020-2022 by Maksim Khramov

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

/** CreatePolygonTool is an EditingTool used for creating polygon shaped Line objects. */

public class CreatePolygonTool extends EditingTool
{
  private static int counter = 1, sides = 3, shape = Curve.NO_SMOOTHING;
  private boolean drawFilled = false;
  private boolean dragging, equilateral, centered;
  private ViewerCanvas workingView;
  private Point clickPoint;
  private double sine[], cosine[], minsine, maxsine, mincosine, maxcosine;
  private BRadioButton openButton, filledButton;
  private ObjectInfo objInfo;
  private Vec3 xdir, ydir, zdir;

  public CreatePolygonTool(LayoutWindow fr)
  {
    super(fr);
    initButton("polygon");
    tabulateSines();
  }

  @Override
  public void activate()
  {
    super.activate();
    setHelpText();
  }

  private void setHelpText()
  {
    String type;
    if (shape == Curve.NO_SMOOTHING)
      type = "polygon";
    else if (shape == Curve.INTERPOLATING)
      type = "interpolatingCurve";
    else
      type = "approximatingCurve";
    theWindow.setHelpText(Translate.text("createPolygonTool.helpText", Integer.toString(sides), Translate.text("createPolygonTool."+type)));
  }

  @Override
  public String getToolTipText()
  {
    return Translate.text("createPolygonTool.tipText");
  }

  private void tabulateSines()
  {
    sine = new double [sides];
    cosine = new double [sides];
    minsine = mincosine = 1.0;
    maxsine = maxcosine = -1.0;
    for (int i = 0; i < sides; i++)
    {
      sine[i]   = Math.sin((i+0.5)*2.0*Math.PI/sides);
      sine[i]   = 1e-10*Math.round(sine[i]*1e10);
      cosine[i] = Math.cos((i+0.5)*2.0*Math.PI/sides);
      cosine[i] = 1e-10*Math.round(cosine[i]*1e10);
      minsine   = Math.min(minsine, sine[i]);
      maxsine   = Math.max(maxsine, sine[i]);
      mincosine = Math.min(mincosine, cosine[i]);
      maxcosine = Math.max(maxcosine, cosine[i]);
    }
  }

  @Override
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    dragging = false;
    workingView = view;
    clickPoint  = e.getPoint();
    equilateral = e.isShiftDown();
    centered    = e.isControlDown();
    ydir = Vec3.vy();
    zdir = Vec3.vz();
  }

  @Override
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (objInfo == null)
    {
      // Create the initial object, if the mouse has moved enough. The limit is there to reduce 
      // the probability of accidentally creating zero size objects.

      Point dragPoint = e.getPoint();
      if (Math.abs(dragPoint.x-clickPoint.x) + Math.abs(dragPoint.y-clickPoint.y) > 3)
      {
        dragging = true;
        Scene theScene = ((LayoutWindow) theWindow).getScene();
        Object3D object = createObject();
        objInfo = new ObjectInfo(object, new CoordinateSystem(), "Polygon "+(counter++));
        objInfo.addTrack(new PositionTrack(objInfo), 0);
        objInfo.addTrack(new RotationTrack(objInfo), 1);
        UndoRecord undo = new UndoRecord(theWindow, false);
        int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
        ((LayoutWindow) theWindow).addObject(objInfo, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, sel);
        theWindow.setUndoRecord(undo);
        ((LayoutWindow) theWindow).setSelection(theScene.getNumObjects()-1);
      }
      else
        return;
    }
    update3D(e, view);
    objInfo.clearCachedMeshes();
    theWindow.updateImage();
  }

  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    dragging = false;
    objInfo = null;
    view.repaint();
    theWindow.setModified();
  }

  private Object3D createObject()
  {
    Vec3[] vertex;
    if (drawFilled)
    {
      vertex = new Vec3 [sides+1];
      vertex[vertex.length-1] = new Vec3();
    }
    else
      vertex = new Vec3 [sides];
    for (int i = 0; i < sides; i++)
      vertex[i] = new Vec3(sine[i], cosine[i], 0.0);

    Object3D object;
    if (drawFilled)
    {
      int faces[][] = new int [sides][];
      faces[0] = new int [] {sides-1, 0, sides};
      for (int i = 1; i < sides; i++)
        faces[i] = new int [] {i-1, i, sides};
      object = new TriangleMesh(vertex, faces);
      ((TriangleMesh) object).setSmoothingMethod(shape);
    }
    else
    {
      float[] s = new float [sides];
      for (int i = 0; i < vertex.length; i++)
        s[i] = 1.0f;
      object = new Curve(vertex, s, shape, true);
    }
    return object;
  }

  private void update3D(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    ydir.set(cam.getCameraCoordinates().getUpDirection());
    zdir.set(cam.getCameraCoordinates().getZDirection());
    zdir.scale(-1.0);
    xdir = ydir.cross(zdir);

    double w, h, xscale, yscale;

    Vec3 click3D = cam.convertScreenToWorld(clickPoint, view.getDistToPlane());
    Vec3 drag3D = cam.convertScreenToWorld(dragPoint, view.getDistToPlane());
    w = drag3D.minus(click3D).dot(xdir);
    h = drag3D.minus(click3D).dot(ydir);

    // Update the size of the polygon

    if (centered)
    {
      xscale = Math.abs(w/(maxsine-minsine)*2.0);
      if (h > 0)
        yscale = Math.abs(h/(mincosine));
      else
        yscale = Math.abs(h/(maxcosine));
    }
    else
    {
      xscale = Math.abs(w/(maxsine-minsine));
      yscale = Math.abs(h/(maxcosine-mincosine));
    }
    if (equilateral)
      xscale = yscale = Math.max(xscale, yscale);

    Vec3[] vertex = ((Mesh)(objInfo.object)).getVertexPositions();
    for (int i = 0; i < sides; i++)
    {
      vertex[i].x = sine[i]*xscale;
      vertex[i].y = -cosine[i]*yscale;
    }
    ((Mesh)objInfo.object).setVertexPositions(vertex);

    // Update coordinate center and orientation

    Vec3 orig;
    if (centered)
      orig = click3D;
    else
    {
      double dx, dy;
      if (w < 0)
        dx = minsine*xscale;
      else
        dx = maxsine*xscale;
      if (h < 0)
        dy = mincosine*yscale;
      else
        dy = maxcosine*yscale;
      orig = click3D.plus(xdir.times(dx)).plus(ydir.times(dy));
    }
    objInfo.getCoords().setOrigin(orig);
    objInfo.getCoords().setOrientation(zdir, ydir);
  }

  private Shape getDraggedShape(ViewerCanvas view)
  {
    Mat4 toScreen = view.getCamera().getWorldToScreen();
    Mat4 toWorld  = objInfo.getCoords().fromLocal();
    Polygon dragged = new Polygon();
    Vec2 onScreen;
    Vec3[] vertex = ((Mesh)(objInfo.object)).getVertexPositions();
    for(int i = 0; i < sides; i++)
    {
      onScreen = toScreen.times(toWorld).timesXY(vertex[i]);
      dragged.addPoint((int)onScreen.x, (int)onScreen.y);
    }
    return dragged;
  }

  @Override
  public void drawOverlay(ViewerCanvas view)
  {
    if (view != workingView || !dragging)
      return;
    view.drawShape(getDraggedShape(view), view.disabledColor);
  }

  @Override
  public void iconDoubleClicked()
  {
    int i;
    ValueField sidesField = new ValueField((double) sides, ValueField.NONNEGATIVE+ValueField.INTEGER);
    BComboBox shapeChoice = new BComboBox(new String [] {
      Translate.text("Angled"),
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    RadioButtonGroup fillRadio = new RadioButtonGroup();
    openButton = new BRadioButton(Translate.text("Open") +  " (" + Translate.text("Curve") + ")", !drawFilled, fillRadio);
    filledButton = new BRadioButton(Translate.text("Filled") +  " (" + Translate.text("triangleMesh") + ")", drawFilled, fillRadio);
    if (shape == Curve.NO_SMOOTHING)
      shapeChoice.setSelectedIndex(0);
    else if (shape == Curve.INTERPOLATING)
      shapeChoice.setSelectedIndex(1);
    else
      shapeChoice.setSelectedIndex(2);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("definePolygon"),
                           new Widget [] {sidesField, shapeChoice, openButton, filledButton},
                           new String [] {Translate.text("Sides"), Translate.text("Shape"), "Interior", " "});
    if (!dlg.clickedOk())
      return;
    i = (int) sidesField.getValue();
    if (i < 3)
    {
      new BStandardDialog("", Translate.text("threeSidesRequired"), BStandardDialog.ERROR).showMessageDialog(theFrame);
      return;
    }
    sides = i;
    i = shapeChoice.getSelectedIndex();
    if (i == 0)
      shape = Curve.NO_SMOOTHING;
    else if (i == 1)
      shape = Curve.INTERPOLATING;
    else
      shape = Curve.APPROXIMATING;
    drawFilled = filledButton.getState();
    tabulateSines();
    setHelpText();
  }
}

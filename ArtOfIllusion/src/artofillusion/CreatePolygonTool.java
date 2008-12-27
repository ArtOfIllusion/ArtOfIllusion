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

/** CreatePolygonTool is an EditingTool used for creating polygon shaped Line objects. */

public class CreatePolygonTool extends EditingTool
{
  private static int counter = 1, sides = 3, shape = 0;
  private Point clickPoint;
  private Vec2 points[];
  private double centerx, centery;
  private double sine[], cosine[], minsine, maxsine, mincosine, maxcosine;

  public CreatePolygonTool(LayoutWindow fr)
  {
    super(fr);
    initButton("polygon");
    tabulateSines();
  }

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

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("createPolygonTool.tipText");
  }

  void tabulateSines()
  {
    int i;

    sine = new double [sides];
    cosine = new double [sides];
    minsine = mincosine = 1.0;
    maxsine = maxcosine = -1.0;
    for (i = 0; i < sides; i++)
      {
        sine[i] = Math.sin((i+0.5)*2.0*Math.PI/sides);
        sine[i] = 1e-10*Math.round(sine[i]*1e10);
        cosine[i] = Math.cos((i+0.5)*2.0*Math.PI/sides);
        cosine[i] = 1e-10*Math.round(cosine[i]*1e10);
        if (sine[i] < minsine)
          minsine = sine[i];
        if (sine[i] > maxsine)
          maxsine = sine[i];
        if (cosine[i] < mincosine)
          mincosine = cosine[i];
        if (cosine[i] > maxcosine)
          maxcosine = cosine[i];
      }
  }
  
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint = e.getPoint();
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    findPoints(e.getPoint(), e.isShiftDown());
    int x[] = new int [points.length], y[] = new int [points.length];
    for (int i = 0; i < points.length; i++)
    {
      x[i] = (int) points[i].x;
      y[i] = (int) points[i].y;
    }
    view.drawDraggedShape(new Polygon(x, y, x.length));
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    Vec3 vertex[], orig, ydir, zdir, temp;
    CoordinateSystem coords;
    double scale;
    float s[] = null;
    Object3D obj;

    findPoints(dragPoint, e.isShiftDown());
    if (e.isControlDown())
      {
        vertex = new Vec3 [points.length+1];
        vertex[points.length] = new Vec3();
      }
    else
      {
        vertex = new Vec3 [points.length];
        s = new float [points.length];
        for (int i = 0; i < points.length; i++)
          s[i] = 1.0f;
      }
    orig = cam.convertScreenToWorld(new Point((int) centerx, (int) centery), Camera.DEFAULT_DISTANCE_TO_SCREEN, false);
    temp = cam.convertScreenToWorld(new Point(1+(int) centerx, (int) centery), Camera.DEFAULT_DISTANCE_TO_SCREEN, false);
    scale = temp.minus(orig).length();
    for (int i = 0; i < points.length; i++)
      vertex[i] = new Vec3(scale*(points[i].x-centerx), -scale*(points[i].y-centery), 0.0);

    // Find the object's coordinate system.
    
    ydir = cam.getViewToWorld().timesDirection(Vec3.vy());
    zdir = cam.getViewToWorld().timesDirection(new Vec3(0.0, 0.0, -1.0));
    coords = new CoordinateSystem(orig, zdir, ydir);
   
    if (e.isControlDown())
      {
        int faces[][] = new int [sides][];
        faces[0] = new int [] {sides-1, 0, sides};
        for (int i = 1; i < sides; i++)
          faces[i] = new int [] {i-1, i, sides};
//        vertex[vertex.length-1] = new Vec3(ce)
        obj = new TriangleMesh(vertex, faces);
        ((TriangleMesh) obj).setSmoothingMethod(shape);
      }
    else
      obj = new Curve(vertex, s, shape, true);
    ObjectInfo info = new ObjectInfo(obj, coords, "Polygon "+(counter++));
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    UndoRecord undo = new UndoRecord(theWindow, false);
    int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
    ((LayoutWindow) theWindow).addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    theWindow.setUndoRecord(undo);
    ((LayoutWindow) theWindow).setSelection(((LayoutWindow) theWindow).getScene().getNumObjects()-1);
    points = null;
    theWindow.updateImage();
  }
  
  void findPoints(Point dragPoint, boolean shiftDown)
  {
    double xscale, yscale;
    int w, h;

    w = dragPoint.x-clickPoint.x;
    h = dragPoint.y-clickPoint.y;
    xscale = Math.abs(w/(maxsine-minsine));
    yscale = Math.abs(h/(maxcosine-mincosine));
    if (shiftDown)
      xscale = yscale = Math.min(xscale, yscale);
    if (w > 0)
      centerx = clickPoint.x - minsine*xscale;
    else
      centerx = clickPoint.x - maxsine*xscale;
    if (h > 0)
      centery = clickPoint.y - mincosine*yscale;
    else
      centery = clickPoint.y - maxcosine*yscale;
    if (points == null || points.length != sides)
      points = new Vec2 [sides];
    for (int i = 0; i < sides; i++)
      points[i] = new Vec2(centerx+sine[i]*xscale, centery+cosine[i]*yscale);
  }

  public void iconDoubleClicked()
  {
    int i;
    
    ValueField sidesField = new ValueField((double) sides, ValueField.NONNEGATIVE+ValueField.INTEGER);
    BComboBox shapeChoice = new BComboBox(new String [] {
      Translate.text("Angled"),
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    if (shape == Curve.NO_SMOOTHING)
      shapeChoice.setSelectedIndex(0);
    else if (shape == Curve.INTERPOLATING)
      shapeChoice.setSelectedIndex(1);
    else
      shapeChoice.setSelectedIndex(2);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("enterNumSides"), 
                new Widget [] {sidesField, shapeChoice},
                new String [] {Translate.text("Sides"), Translate.text("Shape")});
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
    tabulateSines();
    setHelpText();
  }
}
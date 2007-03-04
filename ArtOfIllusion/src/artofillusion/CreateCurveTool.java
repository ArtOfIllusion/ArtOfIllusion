/* Copyright (C) 1999-2006 by Peter Eastman

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
import java.awt.*;
import java.awt.geom.*;
import java.util.Vector;

/** CreateCurveTool is an EditingTool used for creating Curve objects. */

public class CreateCurveTool extends EditingTool
{
  Image icon, selectedIcon;
  static int counter = 1;
  private Vector clickPoint, smoothness;
  private int smoothing;
  private Curve theCurve;
  private CoordinateSystem coords;

  public static final int HANDLE_SIZE = 3;

  public CreateCurveTool(EditingWindow fr, int smoothingMethod)
  {
    super(fr);
    if (smoothingMethod == Curve.INTERPOLATING)
      {
        icon = loadImage("interpCurve.gif");
        selectedIcon = loadImage("selected/interpCurve.gif");
      }
    else
      {
        icon = loadImage("approxCurve.gif");
        selectedIcon = loadImage("selected/approxCurve.gif");
      }
    smoothing = smoothingMethod;
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("createCurveTool.helpText"));
  }

  public void deactivate()
  {
    addToScene();
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public Image getIcon()
  {
    return icon;
  }

  public Image getSelectedIcon()
  {
    return selectedIcon;
  }

  public String getToolTipText()
  {
    if (smoothing == Curve.INTERPOLATING)
      return Translate.text("createCurveTool.tipText.interpolating");
    else
      return Translate.text("createCurveTool.tipText.approximating");
  }

  public boolean hilightSelection()
  {
    return (clickPoint == null);
  }
  
  public void drawOverlay(ViewerCanvas view)
  {
    Camera cam = view.getCamera();

    if (clickPoint == null)
      return;
    if (theCurve != null)
    {
      Mat4 trans = cam.getWorldToScreen().times(coords.fromLocal());
      WireframeMesh mesh = theCurve.getWireframeMesh();
      Point p[] = new Point [mesh.vert.length];
      for (int i = 0; i < p.length; i++)
      {
        Vec2 v = trans.timesXY(mesh.vert[i]);
        p[i] = new Point((int) v.x, (int) v.y);
      }
      for (int i = 0; i < mesh.from.length; i++)
        view.drawLine(p[mesh.from[i]], p[mesh.to[i]], ViewerCanvas.lineColor);
    }
    for (int i = 0; i < clickPoint.size(); i++)
      {
        Vec3 pos = (Vec3) clickPoint.lastElement();
        Vec2 screenPos = view.getCamera().getWorldToScreen().timesXY(pos);
        view.drawBox((int) screenPos.x-HANDLE_SIZE/2, (int) screenPos.y-HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, ViewerCanvas.handleColor);
      }
  }
  
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (clickPoint == null)
    {
      clickPoint = new Vector();
      smoothness = new Vector();
      view.repaint();
    }
    else
    {
      Vec3 pos = (Vec3) clickPoint.lastElement();
      Vec2 screenPos = view.getCamera().getWorldToScreen().timesXY(pos);
      view.drawDraggedShape(new Line2D.Float(new Point2D.Double(screenPos.x, screenPos.y), e.getPoint()));
    }
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (clickPoint.size() == 0)
      return;
    Point dragPoint = e.getPoint();
    Vec3 pos = (Vec3) clickPoint.lastElement();
    Vec2 screenPos = view.getCamera().getWorldToScreen().timesXY(pos);
    view.drawDraggedShape(new Line2D.Float(new Point2D.Double(screenPos.x, screenPos.y), dragPoint));
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    Vec3 vertex[], orig, ydir, zdir;
    float s[];

    if (e.getClickCount() != 2)
      {
        clickPoint.addElement(cam.convertScreenToWorld(dragPoint, ModellingApp.DIST_TO_SCREEN));
        smoothness.addElement(new Float(e.isShiftDown() ? 0.0f : 1.0f));
      }
    if (clickPoint.size() > 1)
      {
        // Create a new line object.  First, find all the points in world coordinates.
            
        vertex = new Vec3 [clickPoint.size()];
        s = new float [clickPoint.size()];
        orig = new Vec3();
        for (int i = 0; i < vertex.length; i++)
          {
            vertex[i] = (Vec3) clickPoint.elementAt(i);
            s[i] = ((Float) smoothness.elementAt(i)).floatValue();
            orig = orig.plus(vertex[i]);
          }
        orig = orig.times(1.0/vertex.length);

        // Find the object's coordinate system.

        ydir = cam.getViewToWorld().timesDirection(Vec3.vy());
        zdir = cam.getViewToWorld().timesDirection(new Vec3(0.0, 0.0, -1.0));
        coords = new CoordinateSystem(orig, zdir, ydir);
            
        // Transform all of the vertices into the object's coordinate system.
            
        for (int i = 0; i < vertex.length; i++)
          {
            vertex[i] = coords.toLocal().times(vertex[i]);
          }
        theCurve = new Curve(vertex, s, smoothing, false);
        if (e.getClickCount() == 2)
          {
            theCurve.setClosed(e.isControlDown());
            addToScene();
            return;
          }
        cam.setObjectTransform(coords.fromLocal());
      }
    theWindow.updateImage();
  }
  
  /** When the user presses Enter, add the curve to the scene. */
  
  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    if (e.getKeyCode() == KeyPressedEvent.VK_ENTER && theCurve != null)
      {
        theCurve.setClosed(e.isControlDown());
        addToScene();
        e.consume();
      }
  }

  /** Add the curve to the scene. */
  
  private void addToScene()
  {
    boolean addCurve = (theCurve != null);
    if (addCurve)
      {
        ObjectInfo info = new ObjectInfo(theCurve, coords, "Curve "+(counter++));
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
        UndoRecord undo = new UndoRecord(theWindow, false);
        int sel[] = theWindow.getScene().getSelection();
        ((LayoutWindow) theWindow).addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        theWindow.setUndoRecord(undo);
        ((LayoutWindow) theWindow).setSelection(theWindow.getScene().getNumObjects()-1);
      }
    clickPoint = null;
    smoothness = null;
    theCurve = null;
    coords = null;
    if (addCurve)
      theWindow.updateImage();
  }
}
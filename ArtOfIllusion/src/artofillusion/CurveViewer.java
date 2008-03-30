/* Copyright (C) 1999-2006 by Peter Eastman

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

/** The CurveViewer class is a component which displays a Curve object and 
    allows the user to edit it. */

public class CurveViewer extends MeshViewer
{
  boolean draggingSelectionBox, dragging;
  int deselect;

  public CurveViewer(MeshEditController window, RowContainer p)
  {
    super(window, p);
  }

  protected void drawObject()
  {
    if (!showMesh)
      return;
    MeshVertex v[] = ((Mesh) getController().getObject().getObject()).getVertices();
    boolean selected[] = controller.getSelection();
    WireframeMesh wireframe = getController().getObject().getObject().getWireframeMesh();
    for (int i = 0; i < wireframe.from.length; i++)
      renderLine(wireframe.vert[wireframe.from[i]], wireframe.vert[wireframe.to[i]], theCamera, lineColor);
    for (int i = 0; i < v.length; i++)
      if (!selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
        {
          Vec2 p = theCamera.getObjectToScreen().timesXY(v[i].r);
          double z = theCamera.getObjectToView().timesZ(v[i].r);
          renderBox(((int) p.x) - HANDLE_SIZE/2, ((int) p.y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, lineColor);
        }
    Color col = (currentTool.hilightSelection() ? highlightColor : lineColor);
    for (int i = 0; i < v.length; i++)
      if (selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
        {
          Vec2 p = theCamera.getObjectToScreen().timesXY(v[i].r);
          double z = theCamera.getObjectToView().timesZ(v[i].r);
          renderBox(((int) p.x) - HANDLE_SIZE/2, ((int) p.y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, col);
        }
  }

  /** When the user presses the mouse, forward events to the current tool as appropriate.
      If this is a vertex based tool, allow them to select or deselect vertices. */

  protected void mousePressed(WidgetMouseEvent e)
  {
    int i, j, x, y;
    double z, nearest;
    MeshVertex v[] = ((Curve) getController().getObject().getObject()).getVertices();
    Vec2 pos;
    Point p;

    requestFocus();
    sentClick = false;
    deselect = -1;
    dragging = false;
    clickPoint = e.getPoint();
    
    // Determine which tool is active.
    
    if (metaTool != null && e.isMetaDown())
      activeTool = metaTool;
    else if (altTool != null && e.isAltDown())
      activeTool = altTool;
    else
      activeTool = currentTool;

    // If the current tool wants all clicks, just forward the event and return.

    if ((activeTool.whichClicks() & EditingTool.ALL_CLICKS) != 0)
      {
        activeTool.mousePressed(e, this);
        dragging = true;
        sentClick = true;
      }
    boolean allowSelectionChange = activeTool.allowSelectionChanges();
    boolean wantHandleClicks = ((activeTool.whichClicks() & EditingTool.HANDLE_CLICKS) != 0);
    if (!allowSelectionChange && !wantHandleClicks)
      return;

    // See whether the click was on a currently selected vertex.
    
    p = e.getPoint();
    boolean selected[] = controller.getSelection();
    for (i = 0; i < v.length; i++)
      if (selected[i])
        {
          pos = theCamera.getObjectToScreen().timesXY(v[i].r);
          x = (int) pos.x;
          y = (int) pos.y;
          if (x >= p.x-HANDLE_SIZE/2 && x <= p.x+HANDLE_SIZE/2 && y >= p.y-HANDLE_SIZE/2 && y <= p.y+HANDLE_SIZE/2)
            break;
        }
    if (i < v.length)
      {
        // The click was on a selected vertex.  If it was a shift-click, the user may want
        // to deselect it, so set a flag.  Forward the event to the current tool.
        
        if (e.isShiftDown() && allowSelectionChange)
          deselect = i;
        if (wantHandleClicks)
        {
          activeTool.mousePressedOnHandle(e, this, 0, i);
          sentClick = true;
        }
        return;
      }

    // The click was not on a selected vertex.  See whether it was on an unselected one.
    // If so, select it and send an event to the current tool.
    
    j = -1;
    nearest = Double.MAX_VALUE;
    for (i = 0; i < v.length; i++)
      {
        pos = theCamera.getObjectToScreen().timesXY(v[i].r);
        x = (int) pos.x;
        y = (int) pos.y;
        if (x >= p.x-HANDLE_SIZE/2 && x <= p.x+HANDLE_SIZE/2 && y >= p.y-HANDLE_SIZE/2 && y <= p.y+HANDLE_SIZE/2)
          {
            z = theCamera.getObjectToView().timesZ(v[i].r);
            if (z > 0.0 && z < nearest)
              {
                nearest = z;
                j = i;
              }
          }
      }
    if (j > -1)
      {
        if (allowSelectionChange)
        {
          boolean oldSelection[] = (boolean []) selected.clone();
          if (!e.isShiftDown())
            for (i = 0; i < selected.length; i++)
              selected[i] = false;
          selected[j] = true;
          ((CurveEditorWindow) controller).findSelectionDistance();
          currentTool.getWindow().setUndoRecord(new UndoRecord(currentTool.getWindow(), false, UndoRecord.SET_MESH_SELECTION, new Object [] {controller, new Integer(controller.getSelectionMode()), oldSelection}));
          controller.setSelection(selected);
          activeTool.getWindow().updateMenus();
        }
        if (e.isShiftDown())
          repaint();
        else if (wantHandleClicks)
        {
          activeTool.mousePressedOnHandle(e, this, 0, j);
          sentClick = true;
        }
        return;
      }
    
    // The click was not on a handle.  Start dragging a selection box.

    if (allowSelectionChange)
    {
      draggingSelectionBox = true;
      beginDraggingSelection(p, false);
    }
  }

  protected void mouseDragged(WidgetMouseEvent e)
  {
    if (!dragging)
      {
        Point p = e.getPoint();
        if (Math.abs(p.x-clickPoint.x) < 2 && Math.abs(p.y-clickPoint.y) < 2)
          return;
      }
    dragging = true;
    deselect = -1;
    super.mouseDragged(e);
  }

  protected void mouseReleased(WidgetMouseEvent e)
  {
    int i, x, y;
    MeshVertex v[] = ((Curve) getController().getObject().getObject()).getVertices();
    Vec2 pos;

    moveToGrid(e);
    endDraggingSelection();
    boolean selected[] = controller.getSelection();
    boolean oldSelection[] = (boolean []) selected.clone();
    if (draggingSelectionBox && !e.isShiftDown() && !e.isControlDown())
      for (i = 0; i < selected.length; i++)
        selected[i] = false;

    // If the user was dragging a selection box, then select or deselect anything 
    // it intersects.
    
    if (selectBounds != null)
      {
        boolean newsel = !e.isControlDown();
        for (i = 0; i < v.length; i++)
          {
            pos = theCamera.getObjectToScreen().timesXY(v[i].r);
            x = (int) pos.x;
            y = (int) pos.y;
            if (selectionRegionContains(new Point(x, y)))
              selected[i] = newsel;
          }
      }
    draggingBox = draggingSelectionBox = false;

    // Send the event to the current tool, if appropriate.

    if (sentClick)
      {
        if (!dragging)
          {
            Point p = e.getPoint();
            e.translatePoint(clickPoint.x-p.x, clickPoint.y-p.y);
          }
        activeTool.mouseReleased(e, this);
      }

    // If the user shift-clicked a selected point and released the mouse without dragging,
    // then deselect the point.

    if (deselect > -1)
      selected[deselect] = false;
    ((CurveEditorWindow) controller).findSelectionDistance();
    for (int k = 0; k < selected.length; k++)
      if (selected[k] != oldSelection[k])
      {
        currentTool.getWindow().setUndoRecord(new UndoRecord(currentTool.getWindow(), false, UndoRecord.SET_MESH_SELECTION, new Object [] {controller, new Integer(controller.getSelectionMode()), oldSelection}));
        break;
      }
    controller.setSelection(selected);
    activeTool.getWindow().updateMenus();
  }
}

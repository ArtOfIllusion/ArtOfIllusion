/* Copyright (C) 1999-2009 by Peter Eastman

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
import artofillusion.view.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;

/** The SplineMeshViewer class is a component which displays a SplineMesh object and 
    allow the user to edit it. */

public class SplineMeshViewer extends MeshViewer
{
  private int deselect;
  private Point screenVert[];
  private double screenZ[];
  private boolean draggingSelectionBox, dragging;
  boolean visible[];

  static final RGBColor surfaceRGB = new RGBColor(0.8f, 0.8f, 1.0f);
  static final Color surfaceColor = new Color(0.8f, 0.8f, 1.0f);

  public SplineMeshViewer(MeshEditController window, RowContainer p)
  {
    super(window, p);
    SplineMesh mesh = (SplineMesh) window.getObject().getObject();
    visible = new boolean [mesh.getVertices().length];
  }

  @Override
  public void updateImage()
  {
    SplineMesh mesh = (SplineMesh) getController().getObject().getObject();
    MeshVertex v[] = mesh.getVertices();

    // Calculate the screen coordinates of every vertex.

    screenVert = new Point [v.length];
    screenZ = new double [v.length];
    double clipDist = theCamera.getClipDistance();
    for (int i = 0; i < v.length; i++)
      {
        Vec2 p = theCamera.getObjectToScreen().timesXY(v[i].r);
        screenVert[i] = new Point((int) p.x, (int) p.y);
        screenZ[i] = theCamera.getObjectToView().timesZ(v[i].r);
        visible[i] = (screenZ[i] > clipDist);
      }
    super.updateImage();
  }

  protected void drawObject()
  {
    SplineMesh mesh = (SplineMesh) getController().getObject().getObject();

    // Draw the object surface.

    drawSurface();
    
    // Displace the camera slightly, so edges and points will show through the surface.
    
    Vec3 displace = new Vec3(0.0, 0.0, -0.01);
    theCamera.getViewToWorld().transformDirection(displace);
    getDisplayCoordinates().toLocal().transformDirection(displace);
    Mat4 oldTransform = theCamera.getObjectToWorld();
    theCamera.setObjectTransform(oldTransform.times(Mat4.translation(displace.x, displace.y, displace.z)));
    
    // Draw the points, edges, and skeleton.
    
    Color meshColor, selectedColor;
    if (currentTool instanceof SkeletonTool)
      {
        meshColor = disabledColor;
        selectedColor = new Color(255, 127, 255);
      }
    else
      {
        meshColor = lineColor;
        selectedColor = highlightColor;
        if (showSkeleton && mesh.getSkeleton() != null)
          mesh.getSkeleton().draw(this, false);
      }
    if (controller.getSelectionMode() == SplineMeshEditorWindow.POINT_MODE)
      {
        drawEdges(disabledColor, disabledColor);
        drawVertices(meshColor, currentTool.hilightSelection() ? selectedColor : meshColor);
      }
    else
      drawEdges(meshColor, currentTool.hilightSelection() ? selectedColor : meshColor);
    if (currentTool instanceof SkeletonTool)
      if (showSkeleton && mesh.getSkeleton() != null)
        mesh.getSkeleton().draw(this, true);
  }

  /** Draw the surface of the object. */
  
  private void drawSurface()
  {
    if (!showSurface)
      return;
    ObjectInfo objInfo = controller.getObject();
    Vec3 viewDir = getDisplayCoordinates().toLocal().timesDirection(theCamera.getViewToWorld().timesDirection(Vec3.vz()));
    if (renderMode == RENDER_WIREFRAME)
      renderWireframe(objInfo.getWireframePreview(), theCamera, surfaceColor);
    else if (renderMode == RENDER_TRANSPARENT)
      renderMeshTransparent(objInfo.getPreviewMesh(), new ConstantVertexShader(surfaceRGB), theCamera, viewDir, null);
    else
    {
      RenderingMesh mesh = objInfo.getPreviewMesh();
      VertexShader shader;
      if (renderMode == RENDER_FLAT)
        shader = new FlatVertexShader(mesh, surfaceRGB, viewDir);
      else if (surfaceColoringParameter != null)
      {
        shader = null;
        TextureParameter params[] = objInfo.getObject().getParameters();
        for (int i = 0; i < params.length; i++)
          if (params[i].equals(surfaceColoringParameter))
          {
            shader = new ParameterVertexShader(mesh, mesh.param[i], lowValueColor, highValueColor, surfaceColoringParameter.minVal, surfaceColoringParameter.maxVal, viewDir);
            break;
          }
      }
      else if (renderMode == RENDER_SMOOTH)
        shader = new SmoothVertexShader(mesh, surfaceRGB, viewDir);
      else
        shader = new TexturedVertexShader(mesh, objInfo.getObject(), 0.0, viewDir).optimize();
      renderMesh(mesh, shader, theCamera, objInfo.getObject().isClosed(), null);
    }
  }
  
  /** Draw the vertices of the control mesh. */
  
  private void drawVertices(Color unselectedColor, Color selectedColor)
  {
    if (!showMesh)
      return;
    SplineMesh mesh = (SplineMesh) getController().getObject().getObject();
    MeshVertex v[] = mesh.getVertices();

    // First, draw any unselected portions of the object.

    ArrayList<Rectangle> boxes = new ArrayList<Rectangle>();
    ArrayList<Double> depths = new ArrayList<Double>();
    boolean selected[] = controller.getSelection();
    for (int i = 0; i < v.length; i++)
      if (!selected[i] && visible[i])
      {
        boxes.add(new Rectangle(screenVert[i].x-HANDLE_SIZE/2, screenVert[i].y-HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE));
        depths.add(screenZ[i]-0.02);
      }
    renderBoxes(boxes, depths, unselectedColor);

    // Now draw the selected portions.

    boxes.clear();
    depths.clear();
    for (int i = 0; i < v.length; i++)
      if (selected[i] && visible[i])
      {
        boxes.add(new Rectangle(screenVert[i].x-HANDLE_SIZE/2, screenVert[i].y-HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE));
        depths.add(screenZ[i]-0.02);
      }
    renderBoxes(boxes, depths, selectedColor);
  }
  
  /** Draw the edges of the control mesh. */
  
  private void drawEdges(Color unselectedColor, Color selectedColor)
  {
    if (!showMesh)
      return;

    // First, draw any unselected portions of the object.

    SplineMesh mesh = (SplineMesh) getController().getObject().getObject();
    MeshVertex v[] = mesh.getVertices();
    int i, j, usize = mesh.getUSize(), vsize = mesh.getVSize();
    boolean uclosed = mesh.isUClosed(), vclosed = mesh.isVClosed();
    boolean selected[] = controller.getSelection();
    if (controller.getSelectionMode() == SplineMeshEditorWindow.POINT_MODE)
      {
        for (i = 0; i < usize; i++)
          {
            for (j = 0; j < vsize-1; j++)
              renderLine(v[i+j*usize].r, v[i+(j+1)*usize].r, theCamera, unselectedColor);
            if (vclosed)
              renderLine(v[i+j*usize].r, v[i].r, theCamera, unselectedColor);
          }
        for (j = 0; j < vsize; j++)
          {
            for (i = 0; i < usize-1; i++)
              renderLine(v[i+j*usize].r, v[i+1+j*usize].r, theCamera, unselectedColor);
            if (uclosed)
              renderLine(v[i+j*usize].r, v[j*usize].r, theCamera, unselectedColor);
          }
      }
    else
      {
        for (i = 0; i < usize; i++)
          if (!selected[i])
            {
              for (j = 0; j < vsize-1; j++)
                renderLine(v[i+j*usize].r, v[i+(j+1)*usize].r, theCamera, unselectedColor);
              if (vclosed)
                renderLine(v[i+j*usize].r, v[i].r, theCamera, unselectedColor);
            }
        for (j = 0; j < vsize; j++)
          if (!selected[j+usize])
            {
              for (i = 0; i < usize-1; i++)
                renderLine(v[i+j*usize].r, v[i+1+j*usize].r, theCamera, unselectedColor);
              if (uclosed)
                renderLine(v[i+j*usize].r, v[j*usize].r, theCamera, unselectedColor);
            }
      }

    // Now draw the selected portions.

    if (controller.getSelectionMode() == SplineMeshEditorWindow.POINT_MODE)
      return;
    for (i = 0; i < usize; i++)
      if (selected[i])
        {
          for (j = 0; j < vsize-1; j++)
            renderLine(v[i+j*usize].r, v[i+(j+1)*usize].r, theCamera, selectedColor);
          if (vclosed)
            renderLine(v[i+j*usize].r, v[i].r, theCamera, selectedColor);
        }
    for (j = 0; j < vsize; j++)
      if (selected[j+usize])
        {
          for (i = 0; i < usize-1; i++)
            renderLine(v[i+j*usize].r, v[i+1+j*usize].r, theCamera, selectedColor);
          if (uclosed)
            renderLine(v[i+j*usize].r, v[j*usize].r, theCamera, selectedColor);
        }
  }

  /** When the user presses the mouse, forward events to the current tool as appropriate.
      If this is a vertex based tool, allow them to select or deselect vertices. */

  protected void mousePressed(WidgetMouseEvent e)
  {
    SplineMesh mesh = (SplineMesh) getController().getObject().getObject();
    int usize = mesh.getUSize(), vsize = mesh.getVSize();
    int i, j, k, dist, closest;
    Point pos = e.getPoint();

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

    // Determine what the click was on.
    
    i = findClickTarget(e.getPoint());

    // If the click was not on an object, start dragging a selection box.
    
    if (i == -1)
      {
        if (allowSelectionChange)
        {
          draggingSelectionBox = true;
          beginDraggingSelection(pos, false);
        }
        return;
      }
    
    // If we are in curve selection mode, find the nearest vertex of the clicked curve, 
    // so that it can be passed to editing tools.
    
    if (controller.getSelectionMode() == MeshEditController.EDGE_MODE)
      {
        j = 0;
        closest = Integer.MAX_VALUE;
        if (i < usize)
          {
            for (k = 0; k < vsize; k++)
              {
                dist = Math.abs(pos.x-screenVert[i+usize*k].x) + Math.abs(pos.y-screenVert[i+usize*k].y);
                if (dist < closest)
                  j = i+usize*k;
              }
          }
        else
          {
            for (k = 0; k < usize; k++)
              {
                dist = Math.abs(pos.x-screenVert[k+usize*(i-usize)].x) + Math.abs(pos.y-screenVert[k+usize*(i-usize)].y);
                if (dist < closest)
                  j = k+usize*(i-usize);
              }
          }
      }
    else
      j = i;

    // If the click was on a selected object, forward it to the current tool.  If it was a
    // shift-click, the user may want to deselect it, so set a flag.
    
    boolean selected[] = controller.getSelection();
    if (selected[i])
      {
        if (e.isShiftDown() && allowSelectionChange)
          deselect = i;
        if (wantHandleClicks)
        {
          activeTool.mousePressedOnHandle(e, this, 0, j);
          sentClick = true;
        }
        return;
      }
    if (!allowSelectionChange)
      return;

    // The click was on an unselected object.  Select it and send an event to the current tool.
    
    boolean oldSelection[] = selected.clone();
    if (!e.isShiftDown())
      for (k = 0; k < selected.length; k++)
        selected[k] = false;
    selected[i] = true;
    currentTool.getWindow().setUndoRecord(new UndoRecord(currentTool.getWindow(), false, UndoRecord.SET_MESH_SELECTION, new Object [] {controller, controller.getSelectionMode(), oldSelection}));
    controller.setSelection(selected);
    currentTool.getWindow().updateMenus();
    if (!e.isShiftDown() && wantHandleClicks)
    {
      activeTool.mousePressedOnHandle(e, this, 0, j);
      sentClick = true;
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
    SplineMesh mesh = (SplineMesh) getController().getObject().getObject();
    int usize = mesh.getUSize(), vsize = mesh.getVSize();
    int i, j;

    moveToGrid(e);
    endDraggingSelection();
    boolean selected[] = controller.getSelection();
    boolean oldSelection[] = selected.clone();
    if (draggingSelectionBox && !e.isShiftDown() && !e.isControlDown())
      for (i = 0; i < selected.length; i++)
        selected[i] = false;

    // If the user was dragging a selection box, then select or deselect anything 
    // it intersects.
    
    if (selectBounds != null)
      {
        boolean newsel = !e.isControlDown();
        if (controller.getSelectionMode() == SplineMeshEditorWindow.POINT_MODE)
          {
            for (i = 0; i < selected.length; i++)
              if (selectionRegionContains(screenVert[i]))
                selected[i] = newsel;
          }
        else
          {
            for (i = 0; i < usize; i++)
              {
                for (j = 0; j < vsize && selectionRegionContains(screenVert[i+j*usize]); j++);
                if (j == vsize)
                  selected[i] = newsel;
              }
            for (i = 0; i < vsize; i++)
              {
                for (j = 0; j < usize && selectionRegionContains(screenVert[j+i*usize]); j++);
                if (j == usize)
                  selected[i+usize] = newsel;
              }
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
    for (int k = 0; k < selected.length; k++)
      if (selected[k] != oldSelection[k])
      {
        currentTool.getWindow().setUndoRecord(new UndoRecord(currentTool.getWindow(), false, UndoRecord.SET_MESH_SELECTION, new Object [] {controller, controller.getSelectionMode(), oldSelection}));
        break;
      }
    controller.setSelection(selected);
    currentTool.getWindow().updateMenus();
  }
  
  /** Determine which vertex or curve (depending on the current selection mode) the
      mouse was clicked on.  If the click was on top of multiple objects, priority is given
      to ones which are currently selected, and then to ones which are in front.  If the
      click is not over any object, -1 is returned. */
  
  int findClickTarget(Point pos)
  {
    SplineMesh mesh = (SplineMesh) getController().getObject().getObject();
    MeshVertex vt[] = mesh.getVertices();
    double z, closestz = Double.MAX_VALUE;
    boolean sel = false;
    Point v1;
    int i, j, which = -1;
    boolean uclosed = mesh.isUClosed(), vclosed = mesh.isVClosed();
    boolean selected[] = controller.getSelection();
    boolean priorityToSelected = (getRenderMode() == RENDER_WIREFRAME || getRenderMode() == RENDER_TRANSPARENT);
    int usize = mesh.getUSize(), vsize = mesh.getVSize();
    
    if (controller.getSelectionMode() == SplineMeshEditorWindow.POINT_MODE)
      {
        for (i = 0; i < vt.length; i++)
          {
            if (!visible[i])
              continue;
            if (sel && !selected[i] && priorityToSelected)
              continue;
            v1 = screenVert[i];
            if (pos.x < v1.x-HANDLE_SIZE/2 || pos.x > v1.x+HANDLE_SIZE/2 ||
                pos.y < v1.y-HANDLE_SIZE/2 || pos.y > v1.y+HANDLE_SIZE/2)
              continue;
            z = theCamera.getObjectToView().timesZ(vt[i].r);
            if (z < closestz || (!sel && selected[i] && priorityToSelected))
              {
                which = i;
                closestz = z;
                sel = selected[i];
              }
          }
      }
    else
      {
        for (i = 0; i < usize; i++)
          {
            if (sel && !selected[i] && priorityToSelected)
              continue;
            for (j = 1; j < vsize; j++)
              {
                z = lineClickDepth(pos, vt, i+(j-1)*usize, i+j*usize);
                if (z < closestz || (z < Double.MAX_VALUE && !sel && selected[i] && priorityToSelected))
                  {
                    which = i;
                    closestz = z;
                    sel = selected[i];
                  }
              }
            if (vclosed)
              {
                z = lineClickDepth(pos, vt, i+(j-1)*usize, i);
                if (z < closestz || (z < Double.MAX_VALUE && !sel && selected[i] && priorityToSelected))
                  {
                    which = i;
                    closestz = z;
                    sel = selected[i];
                  }
              }
          }
        for (i = 0; i < vsize; i++)
          {
            if (sel && !selected[i+usize] && priorityToSelected)
              continue;
            for (j = 1; j < usize; j++)
              {
                z = lineClickDepth(pos, vt, j-1+i*usize, j+i*usize);
                if (z < closestz || (z < Double.MAX_VALUE && !sel && selected[i] && priorityToSelected))
                  {
                    which = i+usize;
                    closestz = z;
                    sel = selected[i+usize];
                  }
              }
            if (uclosed)
              {
                z = lineClickDepth(pos, vt, j-1+i*usize, i*usize);
                if (z < closestz || (z < Double.MAX_VALUE && !sel && selected[i] && priorityToSelected))
                  {
                    which = i+usize;
                    closestz = z;
                    sel = selected[i+usize];
                  }
              }
          }
      }
    return which;
  }
  
  /** Given a click position and the endpoints of a line, this method determines whether the
      click was on top of the line.  If so, it returns the z coordinate of the line at the
      point where it was clicked.  Otherwise, it returns Double.MAX_VALUE. */
  
  private double lineClickDepth(Point pos, MeshVertex vt[], int p1, int p2)
  {
    Point v1, v2;
    double u, v, w;
    
    if (!visible[p1] || !visible[p2])
      return Double.MAX_VALUE;
    v1 = screenVert[p1];
    v2 = screenVert[p2];
    if ((pos.x < v1.x-HANDLE_SIZE/2 && pos.x < v2.x-HANDLE_SIZE/2) || 
            (pos.x > v1.x+HANDLE_SIZE/2 && pos.x > v2.x+HANDLE_SIZE/2) || 
            (pos.y < v1.y-HANDLE_SIZE/2 && pos.y < v2.y-HANDLE_SIZE/2) || 
            (pos.y > v1.y+HANDLE_SIZE/2 && pos.y > v2.y+HANDLE_SIZE/2))
      return Double.MAX_VALUE;
    
    // Determine the distance of the click point from the line.
    
    if (Math.abs(v1.x-v2.x) > Math.abs(v1.y-v2.y))
      {
        if (v2.x > v1.x)
          {
            v = ((double) pos.x-v1.x)/(v2.x-v1.x);
            u = 1.0-v;
          }
        else
          {
            u = ((double) pos.x-v2.x)/(v1.x-v2.x);
            v = 1.0-u;
          }
        w = u*v1.y + v*v2.y - pos.y;
      }
    else
      {
        if (v2.y > v1.y)
          {
            v = ((double) pos.y-v1.y)/(v2.y-v1.y);
            u = 1.0-v;
          }
        else
          {
            u = ((double) pos.y-v2.y)/(v1.y-v2.y);
            v = 1.0-u;
          }
        w = u*v1.x + v*v2.x - pos.x;
      }
    if (Math.abs(w) > HANDLE_SIZE/2)
      return Double.MAX_VALUE;
    return u*theCamera.getObjectToView().timesZ(vt[p1].r) + v*theCamera.getObjectToView().timesZ(vt[p2].r);
  }
}

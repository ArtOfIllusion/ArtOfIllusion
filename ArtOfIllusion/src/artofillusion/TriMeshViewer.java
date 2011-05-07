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
import artofillusion.object.TriangleMesh.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import artofillusion.view.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;

/** The TriMeshViewer class is a component which displays a TriangleMesh object and 
    allow the user to edit it. */

public class TriMeshViewer extends MeshViewer
{
  private boolean draggingSelectionBox, dragging;
  private int deselect;
  private Point screenVert[];
  private double screenZ[];
  private Vec2 screenVec2[];
  boolean visible[];

  public TriMeshViewer(MeshEditController window, RowContainer p)
  {
    super(window, p);
    TriangleMesh mesh = (TriangleMesh) window.getObject().getObject();
    visible = new boolean [mesh.getVertices().length];
  }

  @Override
  public void updateImage()
  {
    TriangleMesh mesh = (TriangleMesh) getController().getObject().getObject();
    MeshVertex v[] = mesh.getVertices();
    RenderingMesh previewMesh = getController().getObject().getPreviewMesh();
    boolean project = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).getProjectOntoSurface() : false);

    // Calculate the screen coordinates of every vertex.

    screenVert = new Point [v.length];
    screenZ = new double [v.length];
    if (visible.length != v.length)
      visible = new boolean [v.length];
    screenVec2 = new Vec2 [v.length];
    double clipDist = theCamera.getClipDistance();
    boolean hideVert[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).hideVert : new boolean [v.length]);
    for (int i = 0; i < v.length; i++)
      {
        Vec3 pos = (project ? previewMesh.vert[i] : v[i].r);
        screenVec2[i] = theCamera.getObjectToScreen().timesXY(pos);
        screenVert[i] = new Point((int) screenVec2[i].x, (int) screenVec2[i].y);
        screenZ[i] = theCamera.getObjectToView().timesZ(pos);
        visible[i] = (!hideVert[i] && screenZ[i] > clipDist);
      }
    super.updateImage();
  }

  protected void drawObject()
  {
    TriangleMesh mesh = (TriangleMesh) getController().getObject().getObject();

    // Now draw the object.

    drawSurface();
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
    if (controller.getSelectionMode() == MeshEditController.POINT_MODE)
      {
        drawEdges(screenVec2, disabledColor, disabledColor);
        drawVertices(meshColor, currentTool.hilightSelection() ? selectedColor : meshColor);
      }
    else
      drawEdges(screenVec2, meshColor, currentTool.hilightSelection() ? selectedColor : meshColor);
    if (currentTool instanceof SkeletonTool)
      if (showSkeleton && mesh.getSkeleton() != null)
        mesh.getSkeleton().draw(this, true);
  }

  /** Draw the surface of the object. */
  
  private void drawSurface()
  {
    if (!showSurface)
      return;
    boolean hide[] = null;
    int faceIndex[] = null;
    ObjectInfo objInfo = controller.getObject();
    if (controller instanceof TriMeshEditorWindow && ((TriMeshEditorWindow) controller).getFaceIndexParameter() != null)
    {
      RenderingMesh mesh = objInfo.getPreviewMesh();
      TextureParameter faceIndexParameter = ((TriMeshEditorWindow) controller).getFaceIndexParameter();
      double param[] = null;
      for (int i = 0; i < mesh.param.length; i++)
        if (objInfo.getObject().getParameters()[i] == faceIndexParameter)
          param = ((FaceParameterValue) mesh.param[i]).getValue();
      faceIndex = new int [param.length];
      for (int i = 0; i < faceIndex.length; i++)
        faceIndex[i] = (int) param[i];
      boolean hideFace[] = ((TriMeshEditorWindow) controller).hideFace;
      if (hideFace != null)
      {
        hide = new boolean [param.length];
        for (int i = 0; i < hide.length; i++)
          hide[i] = hideFace[faceIndex[i]];
      }
    }
    Vec3 viewDir = getDisplayCoordinates().toLocal().timesDirection(theCamera.getViewToWorld().timesDirection(Vec3.vz()));
    if (renderMode == RENDER_WIREFRAME)
      renderWireframe(objInfo.getWireframePreview(), theCamera, surfaceColor);
    else if (renderMode == RENDER_TRANSPARENT)
    {
      VertexShader shader = new ConstantVertexShader(transparentColor);
      if (faceIndex != null && controller.getSelectionMode() == MeshEditController.FACE_MODE)
        shader = new SelectionVertexShader(new RGBColor(1.0, 0.4, 1.0), shader, faceIndex, controller.getSelection());
      renderMeshTransparent(objInfo.getPreviewMesh(), shader, theCamera, viewDir, hide);
    }
    else
    {
      RenderingMesh mesh = objInfo.getPreviewMesh();
      VertexShader shader;
      if (renderMode == RENDER_FLAT)
        shader = new FlatVertexShader(mesh, surfaceRGBColor, viewDir);
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
        shader = new SmoothVertexShader(mesh, surfaceRGBColor, viewDir);
      else
        shader = new TexturedVertexShader(mesh, objInfo.getObject(), 0.0, viewDir).optimize();
      if (faceIndex != null && controller.getSelectionMode() == MeshEditController.FACE_MODE)
        shader = new SelectionVertexShader(new RGBColor(1.0, 0.4, 1.0), shader, faceIndex, controller.getSelection());
      renderMesh(mesh, shader, theCamera, objInfo.getObject().isClosed(), hide);
    }
  }
  
  /** Draw the vertices of the control mesh. */
  
  private void drawVertices(Color unselectedColor, Color selectedColor)
  {
    if (!showMesh)
      return;
    MeshVertex v[] = ((Mesh) getController().getObject().getObject()).getVertices();

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
  
  private void drawEdges(Vec2 p[], Color unselectedColor, Color selectedColor)
  {
    if (!showMesh)
      return;
    Edge e[] = ((TriangleMesh) getController().getObject().getObject()).getEdges();

    // Determine which edges are selected.

    int selectMode = controller.getSelectionMode();
    boolean hideEdge[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).hideEdge : new boolean [e.length]);
    boolean selected[] = controller.getSelection();
    boolean isSelected[];
    if (selectMode == MeshEditController.POINT_MODE)
      isSelected = new boolean [e.length];
    else if (selectMode == MeshEditController.EDGE_MODE)
      isSelected = selected;
    else
    {
      isSelected = new boolean [e.length];
      for (int i = 0; i < e.length; i++)
        isSelected[i] = (selected[e[i].f1] || (e[i].f2 > -1 && selected[e[i].f2]));
    }
    int projectedEdge[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).findProjectedEdges() : null);
    if (projectedEdge == null)
    {
      // Draw the edges of the control mesh.

      for (int j = 0; j < 2; j++)
      {
        // Draw unselected edges on the first pass, selected edges on the second pass.

        boolean showSelected = (j == 1);
        Color color = (showSelected ? selectedColor : unselectedColor);
        for (int i = 0; i < e.length; i++)
          if (showSelected == isSelected[i] && !hideEdge[i] && visible[e[i].v1] && visible[e[i].v2])
            renderLine(p[e[i].v1], screenZ[e[i].v1]-0.01, p[e[i].v2], screenZ[e[i].v2]-0.01, theCamera, color);
      }
    }
    else
    {
      // Draw the edges of the subdivided mesh that correspond to edges of the control mesh.

      TriangleMesh divMesh = ((TriMeshEditorWindow) controller).getSubdividedMesh();
      MeshVertex divVert[] = divMesh.getVertices();
      Edge divEdge[] = divMesh.getEdges();
      double divScreenZ[] = new double [divVert.length];
      Vec2 divPos[] = new Vec2 [divVert.length];
      for (int i = 0; i < divVert.length; i++)
      {
        divPos[i] = theCamera.getObjectToScreen().timesXY(divVert[i].r);
        divScreenZ[i] = theCamera.getObjectToView().timesZ(divVert[i].r);
      }
      for (int j = 0; j < 2; j++)
      {
        // Draw unselected edges on the first pass, selected edges on the second pass.

        boolean showSelected = (j == 1);
        Color color = (showSelected ? selectedColor : unselectedColor);
        for (int i = 0; i < projectedEdge.length; i++)
        {
          int index = projectedEdge[i];
          if (index > -1 && showSelected == isSelected[index] && !hideEdge[index] && visible[e[index].v1] && visible[e[index].v2])
            renderLine(divPos[divEdge[i].v1], divScreenZ[divEdge[i].v1]-0.01, divPos[divEdge[i].v2], divScreenZ[divEdge[i].v2]-0.01, theCamera, color);
        }
      }
    }
  }

  /** When the user presses the mouse, forward events to the current tool as appropriate.
      If this is a vertex based tool, allow them to select or deselect vertices. */

  protected void mousePressed(WidgetMouseEvent e)
  {
    TriangleMesh mesh = (TriangleMesh) getController().getObject().getObject();
    Edge ed[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    int i, j, k;

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

    // If the current tool wants all clicks, just forward the event.

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
    
    i = findClickTarget(e.getPoint(), null);

    // If the click was not on an object, start dragging a selection box.
    
    if (i == -1)
    {
      if (allowSelectionChange)
      {
        draggingSelectionBox = true;
        beginDraggingSelection(e.getPoint(), false);
      }
      return;
    }
    
    // If we are in edge or face selection mode, find a vertex of the clicked edge or face, 
    // so that it can be passed to editing tools.
    
    if (controller.getSelectionMode() == MeshEditController.EDGE_MODE)
      {
        if (visible[ed[i].v1])
          j = ed[i].v1;
        else
          j = ed[i].v2;
      }
    else if (controller.getSelectionMode() == MeshEditController.FACE_MODE)
      {
        if (visible[f[i].v1])
          j = f[i].v1;
        else if (visible[f[i].v2])
          j = f[i].v2;
        else
          j = f[i].v3;
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
          activeTool.mousePressedOnHandle(e, this, 0, j);
        sentClick = true;
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
    boolean hideEdge[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).hideEdge : new boolean [ed.length]);
    if (controller.getSelectionMode() == MeshEditController.FACE_MODE)
      {
        if (hideEdge[f[i].e1])
          selected[ed[f[i].e1].f1] = selected[ed[f[i].e1].f2] = true;
        if (hideEdge[f[i].e2])
          selected[ed[f[i].e2].f1] = selected[ed[f[i].e2].f2] = true;
        if (hideEdge[f[i].e3])
          selected[ed[f[i].e3].f1] = selected[ed[f[i].e3].f2] = true;
      }
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
    TriangleMesh mesh = (TriangleMesh) getController().getObject().getObject();
    Edge ed[] = mesh.getEdges();
    Face fc[] = mesh.getFaces();

    moveToGrid(e);
    endDraggingSelection();
    boolean selected[] = controller.getSelection();
    boolean oldSelection[] = selected.clone();
    if (draggingSelectionBox && !e.isShiftDown() && !e.isControlDown())
      for (int i = 0; i < selected.length; i++)
        selected[i] = false;

    // If the user was dragging a selection box, then select or deselect anything 
    // it intersects.
    
    boolean hideVert[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).hideVert : new boolean [mesh.getVertices().length]);
    boolean hideEdge[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).hideEdge : new boolean [ed.length]);
    boolean hideFace[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).hideFace : new boolean [fc.length]);
    boolean tolerant = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).tolerant : false);
    if (selectBounds != null)
    {
      boolean newsel = !e.isControlDown();
      if (controller.getSelectionMode() == MeshEditController.POINT_MODE)
      {
        for (int i = 0; i < selected.length; i++)
          if (!hideVert[i] && selectionRegionContains(screenVert[i]))
            selected[i] = newsel;
      }
      else if (controller.getSelectionMode() == MeshEditController.EDGE_MODE)
      {
        if (tolerant)
        {
          for (int i = 0; i < selected.length; i++)
            if (!hideEdge[i] && (selectionRegionIntersects(screenVert[ed[i].v1], screenVert[ed[i].v2])))
              selected[i] = newsel;
        }
        else
        {
          for (int i = 0; i < selected.length; i++)
            if (!hideEdge[i] && (selectionRegionContains(screenVert[ed[i].v1]) && selectionRegionContains(screenVert[ed[i].v2])))
              selected[i] = newsel;
        }
      }
      else
      {
        if (tolerant)
        {
          for (int i = 0; i < selected.length; i++)
            if (hideFace == null || !hideFace[i])
              if (selectionRegionIntersects(screenVert[fc[i].v1], screenVert[fc[i].v2]) ||
                  selectionRegionIntersects(screenVert[fc[i].v2], screenVert[fc[i].v3]) ||
                  selectionRegionIntersects(screenVert[fc[i].v3], screenVert[fc[i].v1]))
                selected[i] = newsel;
        }
        else
        {
          for (int i = 0; i < selected.length; i++)
            if (hideFace == null || !hideFace[i])
              if (selectionRegionContains(screenVert[fc[i].v1]) && selectionRegionContains(screenVert[fc[i].v2]) && selectionRegionContains(screenVert[fc[i].v3]))
                selected[i] = newsel;
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
    {
      selected[deselect] = false;
      if (controller.getSelectionMode() == MeshEditController.FACE_MODE)
      {
        Face f = fc[deselect];
        if (hideEdge[f.e1])
          selected[ed[f.e1].f1] = selected[ed[f.e1].f2] = false;
        if (hideEdge[f.e2])
          selected[ed[f.e2].f1] = selected[ed[f.e2].f2] = false;
        if (hideEdge[f.e3])
          selected[ed[f.e3].f1] = selected[ed[f.e3].f2] = false;
      }
    }
    for (int k = 0; k < selected.length; k++)
      if (selected[k] != oldSelection[k])
      {
        currentTool.getWindow().setUndoRecord(new UndoRecord(currentTool.getWindow(), false, UndoRecord.SET_MESH_SELECTION, new Object [] {controller, controller.getSelectionMode(), oldSelection}));
        break;
      }
    controller.setSelection(selected);
    currentTool.getWindow().updateMenus();
  }
  
  /** Determine which vertex, edge, or face (depending on the current selection mode) the
      mouse was clicked on.  If the click was on top of multiple objects, priority is given
      to ones which are currently selected, and then to ones which are in front.  If the
      click is not over any object, -1 is returned. */
  
  public int findClickTarget(Point pos, Vec3 uvw)
  {
    double u, v, w, z, closestz = Double.MAX_VALUE;
    boolean sel = false;
    int which = -1;
    
    boolean selected[] = controller.getSelection();
    boolean priorityToSelected = (getRenderMode() == RENDER_WIREFRAME || getRenderMode() == RENDER_TRANSPARENT);
    if (controller.getSelectionMode() == MeshEditController.POINT_MODE)
    {
      TriangleMesh mesh = (TriangleMesh) getController().getObject().getObject();
      Vertex vt[] = (Vertex []) mesh.getVertices();
      for (int i = 0; i < vt.length; i++)
      {
        if (!visible[i])
          continue;
        if (sel && !selected[i] && priorityToSelected)
          continue;
        Point v1 = screenVert[i];
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
    else if (controller.getSelectionMode() == MeshEditController.EDGE_MODE)
    {
      TriangleMesh mesh = (TriangleMesh) getController().getObject().getObject();
      Vertex vt[];
      Edge ed[], origEd[] = mesh.getEdges();
      int projectedEdge[] = null;
      if (controller instanceof TriMeshEditorWindow)
        projectedEdge = ((TriMeshEditorWindow) controller).findProjectedEdges();
      if (projectedEdge != null)
        mesh = ((TriMeshEditorWindow) controller).getSubdividedMesh();
      vt = (Vertex []) mesh.getVertices();
      ed = mesh.getEdges();
      boolean hideEdge[] = (controller instanceof TriMeshEditorWindow ? ((TriMeshEditorWindow) controller).hideEdge : new boolean [ed.length]);
      for (int i = 0; i < ed.length; i++)
      {
        Point v1, v2;
        if (projectedEdge == null)
        {
          if (!visible[ed[i].v1] || !visible[ed[i].v2])
            continue;
          if (hideEdge[i])
            continue;
          if (sel && !selected[i] && priorityToSelected)
            continue;
          v1 = screenVert[ed[i].v1];
          v2 = screenVert[ed[i].v2];
        }
        else
        {
          int orig = projectedEdge[i];
          if (orig == -1)
            continue;
          if (!visible[origEd[orig].v1] || !visible[origEd[orig].v2])
            continue;
          if (hideEdge[orig])
            continue;
          if (sel && !selected[orig] && priorityToSelected)
            continue;
          Vec2 screen1 = theCamera.getObjectToScreen().timesXY(vt[ed[i].v1].r);
          Vec2 screen2 = theCamera.getObjectToScreen().timesXY(vt[ed[i].v2].r);
          v1 = new Point((int) screen1.x, (int) screen1.y);
          v2 = new Point((int) screen2.x, (int) screen2.y);
        }
        if ((pos.x < v1.x-HANDLE_SIZE/2 && pos.x < v2.x-HANDLE_SIZE/2) ||
                (pos.x > v1.x+HANDLE_SIZE/2 && pos.x > v2.x+HANDLE_SIZE/2) ||
                (pos.y < v1.y-HANDLE_SIZE/2 && pos.y < v2.y-HANDLE_SIZE/2) ||
                (pos.y > v1.y+HANDLE_SIZE/2 && pos.y > v2.y+HANDLE_SIZE/2))
          continue;

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
          continue;
        int index = (projectedEdge == null ? i : projectedEdge[i]);
        z = u*theCamera.getObjectToView().timesZ(vt[ed[i].v1].r) +
                v*theCamera.getObjectToView().timesZ(vt[ed[i].v2].r);
        if (z < closestz || (!sel && selected[index] && priorityToSelected))
        {
          which = index;
          closestz = z;
          sel = selected[which];
          if (uvw != null)
            uvw.set(u, v, w);
        }
      }
    }
    else
    {
      TriangleMesh mesh = null;
      if (controller instanceof TriMeshEditorWindow)
        mesh = ((TriMeshEditorWindow) controller).getSubdividedMesh();
      if (mesh == null)
        mesh = (TriangleMesh) getController().getObject().getObject();
      Vertex vt[] = (Vertex []) mesh.getVertices();
      Face fc[] = mesh.getFaces();
      Face origFc[] = ((TriangleMesh) getController().getObject().getObject()).getFaces();
      double param[] = null;
      boolean hideFace[] = null;
      if (controller instanceof TriMeshEditorWindow)
      {
        TriMeshEditorWindow win = (TriMeshEditorWindow) controller;
        if (win.getFaceIndexParameter() != null)
          param = ((FaceParameterValue) mesh.getParameterValue(win.getFaceIndexParameter())).getValue();
        hideFace = win.hideFace;
      }
      for (int i = 0; i < fc.length; i++)
      {
        int index = (param == null ? i : (int) param[i]);
        if (hideFace != null && hideFace[index])
          continue;
        if (!visible[origFc[index].v1] || !visible[origFc[index].v2] || !visible[origFc[index].v3])
          continue;
        if (sel && !selected[index] && priorityToSelected)
          continue;
        Vec2 screen1 = theCamera.getObjectToScreen().timesXY(vt[fc[i].v1].r);
        Vec2 screen2 = theCamera.getObjectToScreen().timesXY(vt[fc[i].v2].r);
        Vec2 screen3 = theCamera.getObjectToScreen().timesXY(vt[fc[i].v3].r);
        Point v1 = new Point((int) screen1.x, (int) screen1.y);
        Point v2 = new Point((int) screen2.x, (int) screen2.y);
        Point v3 = new Point((int) screen3.x, (int) screen3.y);
        if ((pos.x < v1.x-HANDLE_SIZE/2 && pos.x < v2.x-HANDLE_SIZE/2 && pos.x < v3.x-HANDLE_SIZE/2) ||
                (pos.x > v1.x+HANDLE_SIZE/2 && pos.x > v2.x+HANDLE_SIZE/2 && pos.x > v3.x+HANDLE_SIZE/2) ||
                (pos.y < v1.y-HANDLE_SIZE/2 && pos.y < v2.y-HANDLE_SIZE/2 && pos.y < v3.y-HANDLE_SIZE/2) ||
                (pos.y > v1.y+HANDLE_SIZE/2 && pos.y > v2.y+HANDLE_SIZE/2 && pos.y > v3.y+HANDLE_SIZE/2))
          continue;

        // Determine whether the click point was inside the triangle.

        double e1x = v1.x-v2.x;
        double e1y = v1.y-v2.y;
        double e2x = v1.x-v3.x;
        double e2y = v1.y-v3.y;
        double denom = 1.0/(e1x*e2y-e1y*e2x);
        e1x *= denom;
        e1y *= denom;
        e2x *= denom;
        e2y *= denom;
        double vx = pos.x - v1.x;
        double vy = pos.y - v1.y;
        v = e2x*vy - e2y*vx;
        if (v < 0.0 || v > 1.0)
          continue;
        w = vx*e1y - vy*e1x;
        if (w < 0.0 || w > 1.0)
          continue;
        u = 1.0-v-w;
        if (u < 0.0 || u > 1.0)
          continue;
        z = u*theCamera.getObjectToView().timesZ(vt[fc[i].v1].r) +
            v*theCamera.getObjectToView().timesZ(vt[fc[i].v2].r) +
                w*theCamera.getObjectToView().timesZ(vt[fc[i].v3].r);
        if (z < closestz || (!sel && selected[index] && priorityToSelected))
        {
          which = index;
          closestz = z;
          sel = selected[index];
          if (uvw != null)
            uvw.set(u, v, w);
        }
      }
    }
    return which;
  }
}

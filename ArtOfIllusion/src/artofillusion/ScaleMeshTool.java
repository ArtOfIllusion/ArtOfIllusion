/* Copyright (C) 1999-2007 by Peter Eastman

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
import artofillusion.ui.NinePointManipulator.*;
import buoy.event.*;
import java.awt.*;

/** ScaleMeshTool is an EditingTool used for scaling the vertices of TriangleMesh objects. */

public class ScaleMeshTool extends MeshEditingTool
{
  private boolean dragInProgress, scaleX, scaleY, scaleAll;
  private double clickX, clickY, centerX, centerY;
  private Vec3 scaleCenter, baseVertPos[];
  private UndoRecord undo;
  private final NinePointManipulator manipulator;
  
  public static final int HANDLE_SIZE = 5;

  public ScaleMeshTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("scalePoints");
    manipulator = new NinePointManipulator(new Image[] {
      NinePointManipulator.ARROWS_NW_SE, NinePointManipulator.ARROWS_N_S, NinePointManipulator.ARROWS_NE_SW,
      NinePointManipulator.ARROWS_E_W, null, NinePointManipulator.ARROWS_E_W,
      NinePointManipulator.ARROWS_NE_SW, NinePointManipulator.ARROWS_N_S, NinePointManipulator.ARROWS_NW_SE});
    manipulator.addEventLink(HandlePressedEvent.class, this, "handlePressed");
    manipulator.addEventLink(HandleDraggedEvent.class, this, "handleDragged");
    manipulator.addEventLink(HandleReleasedEvent.class, this, "handleReleased");
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return !dragInProgress;
  }

  public String getToolTipText()
  {
    return Translate.text("scaleMeshTool.tipText");
  }

  public void drawOverlay(ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (!dragInProgress)
    {
      if (selectionBounds != null)
      {
        manipulator.draw(view, selectionBounds);
        theWindow.setHelpText(Translate.text("scaleMeshTool.helpText"));
      }
      else
        theWindow.setHelpText(Translate.text("scaleMeshTool.errorText"));
    }
  }
  
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    dragInProgress = false;
    if (selectionBounds != null)
      dragInProgress = manipulator.mousePressed(e, view, selectionBounds);
  }

  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    manipulator.mouseDragged(e, view);
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    manipulator.mouseReleased(e, view);
  }

  protected void handlePressed(HandlePressedEvent ev)
  {
    Point clickPoint = ev.getMouseEvent().getPoint();
    clickX = (double) clickPoint.x;
    clickY = (double) clickPoint.y;
    BoundingBox bounds = ev.getSelectionBounds();
    Rectangle r = ev.getScreenBounds();
    HandlePosition handle = ev.getHandle();

    // Figure out how to scale the selection based on which handle the mouse was presed on.

    scaleCenter = new Vec3(0.0, 0.0, (bounds.minz+bounds.maxz)/2.0);
    if (handle.isWest())
      {
        scaleX = true;
        centerX = (double) (r.x + r.width);
        scaleCenter.x = bounds.minx;
      }
    else if (handle.isEast())
      {
        scaleX = true;
        centerX = (double) r.x;
        scaleCenter.x = bounds.maxx;
      }
    else
      {
        scaleX = false;
        scaleCenter.x = (bounds.minx+bounds.maxx);
      }
    if (handle.isNorth())
      {
        scaleY = true;
        centerY = (double) (r.y + r.height);
        scaleCenter.y = bounds.miny;
      }
    else if (handle.isSouth())
      {
        scaleY = true;
        centerY = (double) r.y;
        scaleCenter.y = bounds.maxy;
      }
    else
      {
        scaleY = false;
        scaleCenter.y = (bounds.miny+bounds.maxy);
      }
    if (ev.getMouseEvent().isControlDown())
      {
        centerX = (double) (r.x + (r.width/2));
        centerY = (double) (r.y + (r.height/2));
        scaleCenter.x = (bounds.minx+bounds.maxx)/2.0;
        scaleCenter.y = (bounds.miny+bounds.maxy)/2.0;
      }
    scaleAll = ev.getMouseEvent().isShiftDown();
    Mesh mesh = (Mesh) controller.getObject().getObject();
    baseVertPos = mesh.getVertexPositions();
  }
  
  protected void handleDragged(HandleDraggedEvent ev)
  {
    if (undo == null)
    {
      Mesh mesh = (Mesh) controller.getObject().getObject();
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    }
    Vec3 scale = scaleMesh(ev);
    theWindow.updateImage();
    if (scaleAll)
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Double.toString(Math.round(scale.x*1e5)/1e5)));
    else if (scaleX && !scaleY)
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Double.toString(Math.round(scale.x*1e5)/1e5)));
    else if (scaleY && !scaleX)
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Double.toString(Math.round(scale.y*1e5)/1e5)));
    else
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Math.round(scale.x*1e5)/1e5+", "+Math.round(scale.y*1e5)/1e5));
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    if (undo != null)
      theWindow.setUndoRecord(undo);
    scaleMesh(ev);
    theWindow.updateImage();
    undo = null;
    baseVertPos = null;
    dragInProgress = false;
  }

  /**
   * Apply the scaling operation to the mesh.
   */

  private Vec3 scaleMesh(HandleEvent ev)
  {
    Point dragPoint = ev.getMouseEvent().getPoint();
    Mesh mesh = (Mesh) controller.getObject().getObject();
    double size, xscale, yscale, zscale;

    // Figure out how much to scale by.

    if (scaleAll)
      xscale = yscale = 0.0;
    else
      xscale = yscale = 1.0;
    if (scaleX)
      {
        size = dragPoint.x-centerX;
        xscale = size/(clickX-centerX);
        if (xscale <= 0)
          xscale = Math.abs(1.0/(clickX-centerX));
      }
    if (scaleY)
      {
        size = dragPoint.y-centerY;
        yscale = size/(clickY-centerY);
        if (yscale <= 0)
          yscale = Math.abs(1.0/(clickY-centerY));
      }
    if (scaleAll)
      xscale = yscale = zscale = Math.max(xscale, yscale);
    else
      zscale = 1.0;

    // Modify the vertex positions.

    Vec3 v[] = findScaledPositions(baseVertPos, xscale, yscale, zscale, (MeshViewer) ev.getView());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    return new Vec3(xscale, yscale, zscale);
  }

  /**
   * Find the new positions of the vertices after scaling.
   */

  private Vec3 [] findScaledPositions(Vec3 vert[], double xscale, double yscale, double zscale, MeshViewer view)
  {
    Vec3 v[] = new Vec3 [vert.length];
    int selected[] = controller.getSelectionDistance();
    Camera cam = view.getCamera();
    Mat4 m;
    int i;
    
    // Find the transformation matrix.
    
    m = cam.getObjectToView();
    m = Mat4.translation(-scaleCenter.x, -scaleCenter.y, -scaleCenter.z).times(m);
    m = Mat4.scale(xscale, yscale, zscale).times(m);
    m = Mat4.translation(scaleCenter.x, scaleCenter.y, scaleCenter.z).times(m);
    m = cam.getViewToWorld().times(m);
    m = view.getDisplayCoordinates().toLocal().times(m);
    
    // Determine the deltas.
    
    for (i = 0; i < vert.length; i++)
      {
        if (selected[i] == 0)
          v[i] = m.times(vert[i]).minus(vert[i]);
        else
          v[i] = new Vec3();
      }
    if (theFrame instanceof MeshEditorWindow)
      ((MeshEditorWindow) theFrame).adjustDeltas(v);
    for (i = 0; i < vert.length; i++)
      v[i].add(vert[i]);
    return v;
  }
}
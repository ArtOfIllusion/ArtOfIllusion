/* Copyright (C) 2003-2013 by Peter Eastman

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
import buoy.widget.*;
import java.awt.*;

/** BevelExtrudeTool is an EditingTool used for beveling and extruding TriangleMesh objects. */

public class BevelExtrudeTool extends MeshEditingTool
{
  private boolean dragInProgress, separateFaces;
  private TriangleMesh origMesh;
  private TriMeshBeveler beveler;
  private Point clickPoint;
  private double width, height;
  private UndoRecord undo;
  private final NinePointManipulator manipulator;

  public BevelExtrudeTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("bevel");
    manipulator = new NinePointManipulator(new Image[] {null, null, null, null, NinePointManipulator.ARROWS_ALL, null, null, null, null});
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
    return Translate.text("bevelExtrudeTool.tipText");
  }
  
  public void drawOverlay(ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (!dragInProgress)
    {
      if (selectionBounds != null)
      {
        manipulator.draw(view, selectionBounds);
        theWindow.setHelpText(Translate.text("bevelExtrudeTool.helpText"));
      }
      else
        theWindow.setHelpText(Translate.text("bevelExtrudeTool.errorText"));
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
    boolean selected[] = controller.getSelection();
    TriangleMesh mesh = (TriangleMesh) controller.getObject().getObject();
    origMesh = (TriangleMesh) mesh.duplicate();
    int selectMode = controller.getSelectionMode();
    int mode;
    if (selectMode == MeshEditController.POINT_MODE)
      mode = TriMeshBeveler.BEVEL_VERTICES;
    else if (selectMode == MeshEditController.EDGE_MODE)
      mode = TriMeshBeveler.BEVEL_EDGES;
    else
      mode = (separateFaces ? TriMeshBeveler.BEVEL_FACES : TriMeshBeveler.BEVEL_FACE_GROUPS);
    beveler = new TriMeshBeveler(origMesh, selected, mode);
    clickPoint = ev.getMouseEvent().getPoint();
  }
  
  protected void handleDragged(HandleDraggedEvent ev)
  {
    TriangleMesh mesh = (TriangleMesh) controller.getObject().getObject();
    Camera cam = ev.getView().getCamera();
    Point dragPoint = ev.getMouseEvent().getPoint();
    
    // Determine the bevel width and extrude height.
    
    Vec3 dragVec = cam.convertScreenToWorld(dragPoint, cam.getDistToScreen()).minus(cam.convertScreenToWorld(clickPoint, cam.getDistToScreen()));
    width = 0.5*dragVec.x;
    height = dragVec.y;
    if (controller.getSelectionMode() == MeshEditController.FACE_MODE)
    {
      if (ev.getMouseEvent().isShiftDown())
      {
        if (Math.abs(width) > Math.abs(height))
          height = 0.0;
        else
          width = 0.0;
      }
    }
    else
    {
      if (ev.getMouseEvent().isShiftDown())
        height = 0.0;
      if (width < 0.0)
        width = 0.0;
    }
    
    // Update the mesh and redisplay.

    if (undo == null)
    {
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_OBJECT, new Object [] {mesh, origMesh});
      undo.addCommand(UndoRecord.SET_MESH_SELECTION, new Object [] {controller, controller.getSelectionMode(), controller.getSelection().clone()});
    }
    mesh.copyObject(beveler.bevelMesh(height, width));
    controller.setMesh(mesh);
    controller.setSelection(beveler.getNewSelection());
    theWindow.setHelpText(Translate.text("bevelExtrudeTool.dragText", width, height));
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    if (width != 0.0 || height != 0.0)
    {
      TriangleMesh mesh = (TriangleMesh) controller.getObject().getObject();
      if (undo != null)
        theWindow.setUndoRecord(undo);
      controller.objectChanged();
    }
    theWindow.updateImage();
    dragInProgress = false;
    undo = null;
  }

  public void iconDoubleClicked()
  {
    BComboBox c = new BComboBox(new String [] {
      Translate.text("selectionAsWhole"),
      Translate.text("individualFaces")
    });
    c.setSelectedIndex(separateFaces ? 1 : 0);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("applyExtrudeTo"),
        new Widget [] {c}, new String [] {null});
    if (dlg.clickedOk())
      separateFaces = (c.getSelectedIndex() == 1);
  }
}

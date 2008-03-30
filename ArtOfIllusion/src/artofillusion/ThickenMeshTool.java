/* Copyright (C) 2001-2007 by Peter Eastman

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
import java.text.*;

/** ThickenMeshTool is an EditingTool used for making pieces of TriangleMeshes thicker
    or thinner. */

public class ThickenMeshTool extends MeshEditingTool
{
  private boolean dragInProgress;
  private Point clickPoint;
  private Vec3 norm[], baseVertPos[];
  private UndoRecord undo;
  private final NinePointManipulator manipulator;

  public ThickenMeshTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("outsetPoints");
    manipulator = new NinePointManipulator(new Image[] {null, null, null, null, NinePointManipulator.ARROWS_N_S, null, null, null, null});
    manipulator.addEventLink(HandlePressedEvent.class, this, "handlePressed");
    manipulator.addEventLink(HandleDraggedEvent.class, this, "handleDragged");
    manipulator.addEventLink(HandleReleasedEvent.class, this, "handleReleased");
  }

  public boolean allowSelectionChanges()
  {
    return !dragInProgress;
  }

  public String getToolTipText()
  {
    return Translate.text("thickenMeshTool.tipText");
  }

  public void drawOverlay(ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (!dragInProgress)
    {
      if (selectionBounds != null)
      {
        manipulator.draw(view, selectionBounds);
        theWindow.setHelpText(Translate.text("thickenMeshTool.helpText"));
      }
      else
        theWindow.setHelpText(Translate.text("thickenMeshTool.errorText"));
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
    Mesh mesh = (Mesh) controller.getObject().getObject();
    clickPoint = ev.getMouseEvent().getPoint();
    norm = mesh.getNormals();
    baseVertPos = mesh.getVertexPositions();
  }
  
  protected void handleDragged(HandleDraggedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = ev.getMouseEvent().getPoint();

    if (undo == null)
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    double distance = 0.01*(clickPoint.y - dragPoint.y);
    Vec3 v[] = findNewPositions(baseVertPos, distance);
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
    NumberFormat format = NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(2);
    if (distance < 0.0)
      theWindow.setHelpText(Translate.text("thickenMeshTool.dragText.inward", format.format(-distance)));
    else
      theWindow.setHelpText(Translate.text("thickenMeshTool.dragText.outward", format.format(distance)));
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = ev.getMouseEvent().getPoint();

    if (undo != null)
      {
        theWindow.setUndoRecord(undo);
        double distance = 0.01*(clickPoint.y - dragPoint.y);
        Vec3 v[] = findNewPositions(baseVertPos, distance);
        mesh.setVertexPositions(v);
      }
    controller.objectChanged();
    theWindow.updateImage();
    norm = null;
    undo = null;
    baseVertPos = null;
    dragInProgress = false;
  }
  
  /* Find the new positions of the vertices . */

  private Vec3 [] findNewPositions(Vec3 vert[], double distance)
  {
    Vec3 v[] = new Vec3 [vert.length];
    int selected[] = controller.getSelectionDistance();
    
    for (int i = 0; i < v.length; i++)
      {
        if (selected[i] == 0)
          v[i] = norm[i].times(distance);
        else
          v[i] = new Vec3();
      }
    ((MeshEditorWindow) theFrame).adjustDeltas(v);
    for (int i = 0; i < vert.length; i++)
      v[i].add(vert[i]);
    return v;
  }
}
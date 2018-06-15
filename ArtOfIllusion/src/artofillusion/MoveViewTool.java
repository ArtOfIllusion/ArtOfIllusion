/* Copyright (C) 1999-2007 by Peter Eastman
   Changes copyright (C) 2016-2017 by Petri Ihalainen
   Changes copyright (C) 2017 by Maksim Khramov

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
import artofillusion.texture.UVMappingWindow;
import buoy.event.*;
import java.awt.*;

/** MoveViewTool is an EditingTool used for moving the viewpoint. */
@EditingTool.ButtonImage("moveView")
@EditingTool.Tooltip("moveViewTool.tipText")
@EditingTool.ActivatedToolText("moveViewTool.helpText")
public class MoveViewTool extends EditingTool
{
  private Point clickPoint;
  private Mat4 viewToWorld;
  private Vec3 clickPos, oldRotCenter, oldCamPos;
  private boolean controlDown;
  private CoordinateSystem oldCoords;
  private double oldScale, oldDist;
  private int selectedNavigation;

  public MoveViewTool(EditingWindow fr)
  {
    super(fr);
  }
  
  @Override
  public boolean hilightSelection()
  {
      return true;
  }

  @Override
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();

	selectedNavigation = view.getNavigationMode();
    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    clickPos = cam.convertScreenToWorld(clickPoint, view.getDistToPlane());
    oldCoords = cam.getCameraCoordinates().duplicate();
    oldCamPos = oldCoords.getOrigin();
    oldRotCenter = new Vec3(view.getRotationCenter());
    oldScale = view.getScale();
    oldDist = view.getDistToPlane(); // distToPlane needs to be kept up to date
	view.setRotationCenter(oldCoords.getOrigin().plus(oldCoords.getZDirection().times(oldDist)));
	
	if (theWindow != null && theWindow.getToolPalette().getSelectedTool() == this && 
	    !e.isAltDown() && !e.isMetaDown()){
		if (view.getNavigationMode() > 3)
			view.setNavigationMode(0);
		else if (view.getNavigationMode() > 1)
			view.setNavigationMode(view.getNavigationMode()-2, true);
	}
	view.mouseDown = true;
	view.moving = true;
  }

	@Override
	public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
	{
		// If the tool is selected in the tool palette
		if (theWindow != null && theWindow.getToolPalette().getSelectedTool() == this && !e.isAltDown() && !e.isMetaDown())
			dragMoveModel(e, view);
		else
		{
			switch (view.getNavigationMode()) {
				case ViewerCanvas.NAVIGATE_MODEL_SPACE:
				case ViewerCanvas.NAVIGATE_MODEL_LANDSCAPE:
					dragMoveModel(e, view);
					break;
				case ViewerCanvas.NAVIGATE_TRAVEL_SPACE:
				case ViewerCanvas.NAVIGATE_TRAVEL_LANDSCAPE:
					dragMoveTravel(e, view);
					break;
				default:
					break;
			}
		}
		setAuxGraphs(view);
		repaintAllViews(view);
		view.viewChanged(false);	
	}

	/* The view must be set to Perspective for travel modes! */
	private void dragMoveTravel(WidgetMouseEvent e, ViewerCanvas view)
	{
		Camera cam = view.getCamera();
		
		// We compare the move to the moment when the mouse button was pressed
		CoordinateSystem coords = oldCoords.duplicate();
		Point dragPoint = e.getPoint();
		int dx, dy;
		
		dx = dragPoint.x-clickPoint.x;
		dy = dragPoint.y-clickPoint.y;

		if (controlDown) // forward move!
		{ 	
			Vec3 hDir;
			if (view.getNavigationMode() == 3)
			{
				hDir = new Vec3(coords.getZDirection().x, 0.0, coords.getZDirection().z);
				hDir.normalize();
			}
			else
				hDir = coords.getZDirection();
				
			Vec3 newPos = oldCamPos.plus(hDir.times(-dy*0.04*oldDist/cam.getDistToScreen()));
			coords.setOrigin(newPos);
			
			cam.setCameraCoordinates(coords);
			view.setRotationCenter(newPos.plus(coords.getZDirection().times(oldDist)));
		}
		else // Move up-down-right-left
		{
			if (e.isShiftDown()) // Shift down move just up or down.
			{
				if (Math.abs(dx) > Math.abs(dy))
					dy = 0;
				else
					dx = 0;
			}
			Vec3 vDir;
			if (view.getNavigationMode() == 3)
				vDir = new Vec3(0,1,0);
			else
				vDir = coords.getUpDirection();

			// Horizontal move
			Vec3 hMove = cam.findDragVector(clickPos, dx, 0.0);
			Mat4 m = Mat4.translation(-hMove.x, 0.0, -hMove.z);
			coords.transformOrigin(m);

			// Vertical move
			Vec3 newPos = coords.getOrigin().plus(vDir.times(dy*0.01*view.getDistToPlane()/cam.getDistToScreen()));
			coords.setOrigin(newPos);
			
			cam.setCameraCoordinates(coords);
			view.setRotationCenter(newPos.plus(coords.getZDirection().times(view.getDistToPlane())));
		}
	}
	
	private void dragMoveModel(WidgetMouseEvent e, ViewerCanvas view)
	{	
		Camera cam = view.getCamera();
		Point dragPoint = e.getPoint();
		int dx, dy;
		
		dx = dragPoint.x-clickPoint.x;
		dy = dragPoint.y-clickPoint.y;

		if (controlDown) // zoom!
		{ 	
			if (view.isPerspective())
			{
				CoordinateSystem coords = view.getCamera().getCameraCoordinates();
				double newDist = oldDist*Math.pow(1.0/1.01, (double)dy);
				Vec3 newPos = view.getRotationCenter().plus(coords.getZDirection().times(-newDist));
				coords.setOrigin(newPos);
				view.getCamera().setCameraCoordinates(coords);
				view.setDistToPlane(newDist);
			}
			else
			{
				double newScale = oldScale*(Math.pow(1.01,(double)dy));
				view.setScale(newScale);
			}
		}
		else // Move up-down-right-left
		{
			if (e.isShiftDown()) // Shift down move just up or down.
			{
				if (Math.abs(dx) > Math.abs(dy))
					dy = 0;
				else
					dx = 0;
			}
			Vec3 move = cam.findDragVector(clickPos, dx, dy); // Check findDragVector()!

			// Scaling the move from Camera to Scene
			if (view.isPerspective())
				move = move.times(oldDist/view.getDistToPlane());
			
			Mat4 m = Mat4.translation(-move.x, -move.y, -move.z);
			CoordinateSystem newCoords = oldCoords.duplicate();			
			newCoords.transformOrigin(m);
			cam.setCameraCoordinates(newCoords);
			view.setRotationCenter(newCoords.getOrigin().plus(newCoords.getZDirection().times(oldDist)));
		}
	}

  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
	view.mouseDown = false;
	view.moving = false;
	view.setNavigationMode(selectedNavigation);
    if (theWindow != null)
      {
        ObjectInfo bound = view.getBoundCamera();
        if (bound != null)
        {
          // This view corresponds to an actual camera in the scene.  Create an undo record, and move any children of
          // the camera.
		  bound.getCoords().copyCoords(view.getCamera().getCameraCoordinates());
          UndoRecord undo = new UndoRecord(theWindow, false, UndoRecord.COPY_COORDS, new Object [] {bound.getCoords(), oldCoords});
          moveChildren(bound, bound.getCoords().fromLocal().times(oldCoords.toLocal()), undo);
          theWindow.setUndoRecord(undo);
        }
        theWindow.updateImage();
      }
	wipeAuxGraphs();
	view.viewChanged(false);
  }

  /** This is called recursively to move any children of a bound camera. */
  private void moveChildren(ObjectInfo parent, Mat4 transform, UndoRecord undo)
  {
    for (int i = 0; i < parent.getChildren().length; i++)
    {
      CoordinateSystem coords = parent.getChildren()[i].getCoords();
      CoordinateSystem oldCoords = coords.duplicate();
      coords.transformCoordinates(transform);
      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {coords, oldCoords});
      moveChildren(parent.getChildren()[i], transform, undo);
    }  
  }

  private void repaintAllViews(ViewerCanvas view)
  {
    if (theWindow == null || theWindow instanceof UVMappingWindow)
	  view.repaint();
    else
	  for (ViewerCanvas v : theWindow.getAllViews())
	  	v.repaint();
  }

  private void setAuxGraphs(ViewerCanvas view)
  {

	if (theWindow != null)
	  for (ViewerCanvas v : theWindow.getAllViews())
        if (v != view)
	      v.auxGraphs.set(view, true);
  }
  
  private void wipeAuxGraphs()
  {
    if (theWindow != null)
	  for (ViewerCanvas v : theWindow.getAllViews())
		v.auxGraphs.wipe();
  }
  
}

/* Copyright (C) 1999-2012 by Peter Eastman
   Changes Copyright (C) 2106 by Petri Ihalainen

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
import java.awt.*;

/** RotateViewTool is an EditingTool for rotating the viewpoint around the origin. */

public class RotateViewTool extends EditingTool
{
  private static final double DRAG_SCALE = 0.01;

  private Point clickPoint;
  private Mat4 viewToWorld;
  private boolean controlDown;
  private CoordinateSystem oldCoords;
  private Vec3 rotationCenter;
  private double angle, distToRot;
 
  public RotateViewTool(EditingWindow fr)
  {
    super(fr);
    initButton("rotateView");
  }

  @Override
  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("rotateViewTool.helpText"));
  }

  @Override
  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  @Override
  public boolean hilightSelection()
  {
      return true;
  }

  @Override
  public String getToolTipText()
  {
    return Translate.text("rotateViewTool.tipText");
  }

  @Override
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    oldCoords = cam.getCameraCoordinates().duplicate();
	
	// Make sure that the rotation Cneter is on Camera Z-axis.
	// Some plugins like PolyMesh can have them separated
	view.setRotationCenter(oldCoords.getOrigin().plus(oldCoords.getZDirection().times(view.getDistToPlane())));
	
    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    viewToWorld = cam.getViewToWorld();
    rotationCenter = view.getRotationCenter();
	distToRot = oldCoords.getOrigin().minus(rotationCenter).length();
  }
 
	@Override
  	public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
	{	
		switch (view.getNavigationMode()) {
			case ViewerCanvas.NAVIGATE_MODEL_SPACE:
				dragRotateSpace(e, view);
				break;
			case ViewerCanvas.NAVIGATE_MODEL_LANDSCAPE:
				dragRotateLandscape(e, view);
				break;
			case ViewerCanvas.NAVIGATE_TRAVEL_SPACE:
			case ViewerCanvas.NAVIGATE_TRAVEL_LANDSCAPE:
			default:
				break;
		}
		
	  if (view.getBoundCamera() != null)
	  {	
		repaintAllViews();
		// The plan was to move the children of the camera along in all views
		// Something not right with it so far
		//ObjectInfo bound = view.getBoundCamera();
        //moveChildrenLive(bound, bound.getCoords().fromLocal().times(oldCoords.toLocal()));
	  }
	}

	private void dragRotateSpace(WidgetMouseEvent e, ViewerCanvas view)
	{
		// This is modified from AoI 2.7
		
		Point dragPoint = e.getPoint();
		CoordinateSystem c = oldCoords.duplicate();
		int dx, dy;
		Vec3 axis;

		dx = dragPoint.x-clickPoint.x;
		dy = dragPoint.y-clickPoint.y;

		if (controlDown)
		{
			axis = viewToWorld.timesDirection(Vec3.vz());
			angle = dx * DRAG_SCALE;
		}
		else if (e.isShiftDown())
		{
			if (Math.abs(dx) > Math.abs(dy))
			{
				axis = viewToWorld.timesDirection(Vec3.vy());
				angle = dx * DRAG_SCALE;
			}
			else
			{
				axis = viewToWorld.timesDirection(Vec3.vx());
				angle = -dy * DRAG_SCALE;
			}
		}
		else
		{
			axis = new Vec3(-dy*DRAG_SCALE, dx*DRAG_SCALE, 0.0);
			angle = axis.length();
			axis = axis.times(1.0/angle);
			axis = viewToWorld.timesDirection(axis);
		}
		
		if (angle != 0.0)
		{
			c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
			c.transformCoordinates(Mat4.axisRotation(axis, -angle));
			c.transformCoordinates(Mat4.translation(rotationCenter.x, rotationCenter.y, rotationCenter.z));
			view.getCamera().setCameraCoordinates(c);			
		}
		
		view.viewChanged(false);	
		view.repaint();
	}

	private void dragRotateLandscape(WidgetMouseEvent e, ViewerCanvas view)
	{
		//This is modified from AoI 3.0
		
		Vec3 vertical = new Vec3(0.0,1.0,0.0); 
		
		Point dragPoint = e.getPoint();
		int dx = dragPoint.x-clickPoint.x;
		int dy = dragPoint.y-clickPoint.y;
		Mat4 rotation;
		// Tilting disabled for the time being
		//if (controlDown)
		//	rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vz()), -dx*DRAG_SCALE);
		//else
		if (e.isShiftDown())
		{
			if (Math.abs(dx) > Math.abs(dy))
				rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE);
			else{
				double dragAngleFw = dy*DRAG_SCALE;
				if (dragAngleFw > Math.PI) dragAngleFw = Math.PI; // These may hep a bit but not all the way
				if (dragAngleFw < -Math.PI) dragAngleFw = -Math.PI;
				rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dragAngleFw);
			}
		}
		else
		{
			double dragAngleFw = dy*DRAG_SCALE;
			if (dragAngleFw > Math.PI) dragAngleFw = Math.PI;
			if (dragAngleFw < -Math.PI) dragAngleFw = -Math.PI;
			rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dragAngleFw);
			//System.out.println(dy + " " + dy*DRAG_SCALE);
			rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE).times(rotation);
		}
		if (!rotation.equals(Mat4.identity()))
		{
			CoordinateSystem c = oldCoords.duplicate();
			c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
			c.transformCoordinates(rotation);

			// Prevent tilting forward or back more than 90 degrees.
			// almost works
			if (c.getUpDirection().y < 0.0)
			{
				//System.out.println("CLIP");
				Vec3 upD = new Vec3(c.getUpDirection().x,0.0,c.getUpDirection().z); 
				upD.normalize();
				Vec3 zD = new Vec3();
				if (c.getZDirection().y < 0.0) 
					zD.y = -1.0;
				else
					zD.y = 1.0;
				Vec3 cp = rotationCenter.plus(zD.times(-distToRot));
				c.setOrientation(zD,upD);
				c.setOrigin(cp);
			}
			else

			c.transformCoordinates(Mat4.translation(rotationCenter.x, rotationCenter.y, rotationCenter.z));
			view.getCamera().setCameraCoordinates(c);
		}
		view.viewChanged(false);
		view.repaint();
	}

  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    mouseDragged(e, view);
    Point dragPoint = e.getPoint();
    if ((dragPoint.x != clickPoint.x || dragPoint.y != clickPoint.y) && view.getBoundCamera() == null)
      view.setOrientation(ViewerCanvas.VIEW_OTHER);
    if (theWindow != null)
      {
        ObjectInfo bound = view.getBoundCamera();
        if (bound != null)
          {
            // This view corresponds to an actual camera in the scene.  Create an undo record, and move any children of
            // the camera.
            UndoRecord undo = new UndoRecord(theWindow, false, UndoRecord.COPY_COORDS, new Object [] {bound.getCoords(), oldCoords});
            moveChildren(bound, bound.getCoords().fromLocal().times(oldCoords.toLocal()), undo);
            theWindow.setUndoRecord(undo);
          }
        theWindow.updateImage();
      }
	if (dragPoint.x == clickPoint.x && dragPoint.y == clickPoint.y)
	  view.centerToPoint(dragPoint);
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
  
  /** This is called recursively during mouseDragged to move any children of a bound camera. */
  private void moveChildrenLive(ObjectInfo parent, Mat4 transform)
  {
    for (int i = 0; i < parent.getChildren().length; i++)
    {
      CoordinateSystem coords = parent.getChildren()[i].getCoords();
      CoordinateSystem oldCoords = coords.duplicate();
	  coords.transformCoordinates(transform);
      moveChildrenLive(parent.getChildren()[i], transform);
    }
	repaintAllViews();
  }

  /** This is used when a SceneCamera moves in the scene */
  private void repaintAllViews()
  {
	ViewerCanvas[] views = theWindow.getAllViews();
	for (ViewerCanvas v : views){
	  v.viewChanged(false);
      v.repaint();
    }
  }
}
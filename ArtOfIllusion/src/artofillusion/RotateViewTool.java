/* Copyright (C) 1999-2012 by Peter Eastman
   Changes copyright (C) 2016-2019 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import static artofillusion.ViewerCanvas.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import static artofillusion.ui.UIUtilities.*;
import artofillusion.view.*;
import artofillusion.texture.UVMappingWindow;
import buoy.event.*;
import java.awt.*;
import java.util.ArrayList;

/** RotateViewTool is an EditingTool for rotating the viewpoint around the origin. */

public class RotateViewTool extends EditingTool
{
  private static final double DRAG_SCALE = 0.01;

  private Point clickPoint;
  private Mat4 viewToWorld;
  private boolean controlDown;
  private CoordinateSystem oldCoords;
  private Vec3 rotationCenter;
  private double angle, distToRot, xAngle, yAngle, r, fwMax, fwMin;
  private Camera camera;
  private int selectedNavigation;
 
  private Point viewCenter, p0, p1;
 
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
    camera = view.getCamera();
	
	selectedNavigation = view.getNavigationMode();
    oldCoords = camera.getCameraCoordinates().duplicate();
	
	// Make sure that the rotation Center is on Camera Z-axis.
	// Some plugins like PolyMesh can have them separated
	view.setRotationCenter(oldCoords.getOrigin().plus(oldCoords.getZDirection().times(view.getDistToPlane())));
	
    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    viewToWorld = camera.getViewToWorld();
    rotationCenter = view.getRotationCenter();
	distToRot = oldCoords.getOrigin().minus(rotationCenter).length();
	
	if (theWindow != null
           && theWindow.getToolPalette().getSelectedTool() == this
           && mouseButtonOne(e))
	{
		if (view.getNavigationMode() > 3)
			view.setNavigationMode(NAVIGATE_MODEL_SPACE);
		else if (view.getNavigationMode() > 1)
			view.setNavigationMode(view.getNavigationMode()-2, true);
	}
	view.mouseDown = true;
	view.rotating = true;
  }
 
	@Override
  	public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
	{
		if (e.getPoint() != clickPoint && view.getBoundCamera() == null) // This is needed even if the mouse has not been dragged yet.
			view.setOrientation(VIEW_OTHER);
			
		if (theWindow != null
                   && theWindow.getToolPalette().getSelectedTool() == this
                   && mouseButtonOne(e))
		{
			if (view.getNavigationMode() == NAVIGATE_MODEL_SPACE)
				dragRotateSpace(e, view);
			else if (view.getNavigationMode() == 1)
			{
				Vec3 zD = oldCoords.getZDirection();
				fwMax = Math.PI*0.5+Math.asin(zD.y);
				fwMin = -Math.PI*0.5+Math.asin(zD.y);
				dragRotateLandscape(e, view);
			}
		}
		else
		{
			switch (view.getNavigationMode()) 
			{
				case NAVIGATE_MODEL_SPACE:
					dragRotateSpace(e, view);
					break;
				case NAVIGATE_MODEL_LANDSCAPE:
					Vec3 zD = oldCoords.getZDirection();
					fwMax = Math.PI*0.5+Math.asin(zD.y);
					fwMin = -Math.PI*0.5+Math.asin(zD.y);
					dragRotateLandscape(e, view);
					break;
				case NAVIGATE_TRAVEL_SPACE:
					dragRotateTravelSpace(e, view);
					break;
				case NAVIGATE_TRAVEL_LANDSCAPE:
					dragRotateTravelLandscape(e, view);
					break;
				default:
					break;
			}
		}
		view.repaint();
		view.viewChanged(false);
	}
	
	private void dragRotateTravelSpace(WidgetMouseEvent e, ViewerCanvas view)
	{
		Point dragPoint = e.getPoint();
		Vec3 axis, location;
		CoordinateSystem c = oldCoords.duplicate();
		location = c.getOrigin();
		int dx, dy;

		dx = dragPoint.x-clickPoint.x;
		dy = dragPoint.y-clickPoint.y;

		if (mouseButtonTwo(e) && controlDown)
		{
			view.tilting = true;
			tilt(e, view, clickPoint);
			return;
		}
		else if (controlDown && e.isShiftDown())
		{
			rotateSpace(e, view, clickPoint);
			return;
		}
		else if (!controlDown && e.isShiftDown())
		{
			if (Math.abs(dx) > Math.abs(dy))
			{
				axis = viewToWorld.timesDirection(Vec3.vy());
				angle = dx * DRAG_SCALE / view.getCamera().getDistToScreen();
			}
			else
			{
				axis = viewToWorld.timesDirection(Vec3.vx());
				angle = -dy * DRAG_SCALE / view.getCamera().getDistToScreen();
			}
		}
		else
		{
			axis = new Vec3(-dy*DRAG_SCALE, dx*DRAG_SCALE, 0.0);
			angle = axis.length() / view.getCamera().getDistToScreen();
			axis.normalize(); //  = axis.times(1.0/angle);
			axis = viewToWorld.timesDirection(axis);
		}
		if (angle != 0)
		{
			c.transformCoordinates(Mat4.translation(-location.x, -location.y, -location.z));
			c.transformCoordinates(Mat4.axisRotation(axis, angle));
			c.transformCoordinates(Mat4.translation(location.x, location.y, location.z));
			view.getCamera().setCameraCoordinates(c);
			Vec3 cc = c.getOrigin();
			view.setRotationCenter(cc.plus(c.getZDirection().times(view.getDistToPlane())));
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

		if (mouseButtonTwo(e) && controlDown)
		{
			view.tilting = true;
			tilt(e, view, clickPoint);
			return;
		}
		else if (controlDown && e.isShiftDown())
		{
			panSpace(e, view, clickPoint);
			return;
		}
		else if (!controlDown && e.isShiftDown())
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
		if (!controlDown && e.isShiftDown())
		{
			if (Math.abs(dx) > Math.abs(dy))
				rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE);
			else{
				double dragAngleFw = dy*DRAG_SCALE;
				if (dragAngleFw > fwMax) dragAngleFw = fwMax; // These may hep a bit but not all the way
				if (dragAngleFw < fwMin) dragAngleFw = fwMin;
				rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dragAngleFw);
			}
		}
		else if (controlDown && e.isShiftDown())
		{
			panLandscape(e, view, clickPoint, vertical);
			return;
		}
		else
		{
			// Prevent tilting forward or back more than 90 degrees
			double dragAngleFw = dy*DRAG_SCALE;
			if (dragAngleFw > fwMax) dragAngleFw = fwMax; // These may hep a bit but not all the way
			if (dragAngleFw < fwMin) dragAngleFw = fwMin;
			rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dragAngleFw);
			rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE).times(rotation);
		}
		if (!rotation.equals(Mat4.identity()))
		{
			CoordinateSystem c = oldCoords.duplicate();
			c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
			c.transformCoordinates(rotation);
			c.transformCoordinates(Mat4.translation(rotationCenter.x, rotationCenter.y, rotationCenter.z));
			view.getCamera().setCameraCoordinates(c);
		}
	}
	private void dragRotateTravelLandscape(WidgetMouseEvent e, ViewerCanvas view)
	{
		//This is modified from AoI 3.0
		Vec3 vertical = new Vec3(0.0,1.0,0.0); 
		
		Point dragPoint = e.getPoint();
		int dx = dragPoint.x-clickPoint.x;
		int dy = dragPoint.y-clickPoint.y;
		Mat4 rotation;
		double dts = view.getCamera().getDistToScreen();
		// Tilting disabled for the time being
		//if (controlDown)
		//	rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vz()), -dx*DRAG_SCALE);
		//else
		if (!controlDown && e.isShiftDown())
		{
			if (Math.abs(dx) > Math.abs(dy))
				rotation = Mat4.axisRotation(vertical, dx*DRAG_SCALE/distToRot);
			else{
				double dragAngleFw = -dy*DRAG_SCALE/distToRot;
				if (dragAngleFw > Math.PI) dragAngleFw = Math.PI; // These may hep a bit but not all the way
				if (dragAngleFw < -Math.PI) dragAngleFw = -Math.PI;
				rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dragAngleFw);
			}
		}
		else if (controlDown && e.isShiftDown())
		{
			rotateLandscape(e, view, clickPoint, vertical);
			return;
		}
		else
		{
			if (view.getBoundCamera() != null){
				int yp = view.getBounds().height/2;
				double fa = Math.PI/2.0 - ((SceneCamera)view.getBoundCamera().getObject()).getFieldOfView()/2.0/180.0*Math.PI;
				
				// dts is equivalent to the "distToScreen" parameter on SceneCameras.
				dts = Math.tan(fa)*yp/100;
			}
			rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), -dy*DRAG_SCALE/dts);
			rotation = Mat4.axisRotation(vertical, dx*DRAG_SCALE/dts).times(rotation);
		}
		if (!rotation.equals(Mat4.identity()))
		{
			CoordinateSystem c = oldCoords.duplicate();
			Vec3 cc = c.getOrigin();
			c.transformCoordinates(Mat4.translation(-cc.x, -cc.y, -cc.z));
			c.transformCoordinates(rotation);

			// Prevent tilting forward or back more than 90 degrees.
			// almost works
			if (c.getUpDirection().y < 0.0)
			{
				Vec3 upD = new Vec3(c.getUpDirection().x,0.0,c.getUpDirection().z); 
				upD.normalize();
				Vec3 zD = new Vec3();
				if (c.getZDirection().y < 0.0) 
					zD.y = -1.0;
				else
					zD.y = 1.0;
				c.setOrientation(zD,upD);
			}
			else
				c.transformCoordinates(Mat4.translation(cc.x, cc.y, cc.z));

			view.getCamera().setCameraCoordinates(c);
			view.setRotationCenter(cc.plus(c.getZDirection().times(view.getDistToPlane())));
		}
	}

  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
	view.mouseDown = false;
	view.tilting = false;
	view.rotating = false;
	view.setNavigationMode(selectedNavigation);

    Point dragPoint = e.getPoint();
    if (theWindow != null)
    {
		ObjectInfo bound = view.getBoundCamera();
        if (bound != null)
        {
            // This view corresponds to an actual camera in the scene.  Create an undo record, and move any children of
            // the camera.
			bound.getCoords().copyCoords(view.getCamera().getCameraCoordinates()); // for precise action.
            UndoRecord undo = new UndoRecord(theWindow, false, UndoRecord.COPY_COORDS, new Object [] {bound.getCoords(), oldCoords});
            moveChildren(bound, bound.getCoords().fromLocal().times(oldCoords.toLocal()), undo);
            theWindow.setUndoRecord(undo);
        }
        theWindow.updateImage();
    }
	
	// If the mouse was not dragged then center to the given point
	// This shouls be directly in the ViewerCanvas but it had a side-effect.
	if (dragPoint.x == clickPoint.x && dragPoint.y == clickPoint.y)
	    view.centerToPoint(dragPoint);
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
  
  private void tilt(WidgetMouseEvent e, ViewerCanvas view, Point clickPoint)
  {
	int d = Math.min(view.getBounds().width, view.getBounds().height);
	r = d*0.45;
	int cx = view.getBounds().width/2;
	int cy = view.getBounds().height/2;
	viewCenter = new Point(cx, cy);
	
	double aClick = Math.atan2(clickPoint.y-cy, clickPoint.x-cx);
	p0 = new Point((int)(r*Math.cos(aClick)+cx), (int)(r*Math.sin(aClick))+cy);
	
	Point dragPoint = e.getPoint();
	double aDrag = Math.atan2(dragPoint.y-cy, dragPoint.x-cx);
	p1 = new Point((int)(r*Math.cos(aDrag))+cx, (int)(r*Math.sin(aDrag))+cy);
	
	Vec3 axis = viewToWorld.timesDirection(Vec3.vz());
	
	angle = aDrag-aClick;
	CoordinateSystem c = oldCoords.duplicate();
	c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
	c.transformCoordinates(Mat4.axisRotation(axis, -angle));
	c.transformCoordinates(Mat4.translation(rotationCenter.x, rotationCenter.y, rotationCenter.z));
	view.getCamera().setCameraCoordinates(c);
  }

  private void panSpace(WidgetMouseEvent e, ViewerCanvas view, Point clickPoint)
  {
	Point dragPoint = e.getPoint();
	int dx = dragPoint.x-clickPoint.x;
	int dy = dragPoint.y-clickPoint.y;
	Vec3 axis = new Vec3(-dy*DRAG_SCALE, dx*DRAG_SCALE, 0.0);
	double angle = axis.length()/view.getCamera().getDistToScreen();
	axis.normalize();
	axis = viewToWorld.timesDirection(axis);

	CoordinateSystem c = oldCoords.duplicate();
	Vec3 cc = c.getOrigin();
	c.transformCoordinates(Mat4.translation(-cc.x, -cc.y, -cc.z));
	c.transformCoordinates(Mat4.axisRotation(axis, angle));
	c.transformCoordinates(Mat4.translation(cc.x, cc.y, cc.z));
	
	view.getCamera().setCameraCoordinates(c);
	view.setRotationCenter(cc.plus(c.getZDirection().times(view.getDistToPlane())));
  }
  
  private void panLandscape(WidgetMouseEvent e, ViewerCanvas view, Point clickPoint, Vec3 vertical)
  {
	Point dragPoint = e.getPoint();
	int dx = dragPoint.x-clickPoint.x;
	int dy = dragPoint.y-clickPoint.y;
	double dts = camera.getDistToScreen();
	
	if (view.getBoundCamera() != null){
		int yp = view.getBounds().height/2;
		double fa = Math.PI/2.0 - ((SceneCamera)view.getBoundCamera().getObject()).getFieldOfView()/2.0/180.0*Math.PI;
		dts = Math.tan(fa)*yp/100;
	}
	
	Mat4 rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), -dy*DRAG_SCALE/dts);
	rotation = Mat4.axisRotation(vertical, dx*DRAG_SCALE/dts).times(rotation);
	
	if (!rotation.equals(Mat4.identity()))
	{
		CoordinateSystem c = oldCoords.duplicate();
		Vec3 cc = c.getOrigin();
		c.transformCoordinates(Mat4.translation(-cc.x, -cc.y, -cc.z));
		c.transformCoordinates(rotation);
	
		// Prevent tilting forward or back more than 90 degrees.
		// almost works
		if (c.getUpDirection().y < 0.0)
		{
			Vec3 upD = new Vec3(c.getUpDirection().x,0.0,c.getUpDirection().z); 
			upD.normalize();
			Vec3 zD = new Vec3();
			if (c.getZDirection().y < 0.0) 
				zD.y = -1.0;
			else
				zD.y = 1.0;
			Vec3 cp = cc.plus(zD.times(-distToRot));
			c.setOrientation(zD,upD);
			c.setOrigin(cp);
		}
		else
			c.transformCoordinates(Mat4.translation(cc.x, cc.y, cc.z));

		camera.setCameraCoordinates(c);
		view.setRotationCenter(cc.plus(c.getZDirection().times(view.getDistToPlane())));
	}
  }
 
  private void rotateSpace(WidgetMouseEvent e, ViewerCanvas view, Point clickPoint)
  {
	Point dragPoint = e.getPoint();
	int dx = dragPoint.x-clickPoint.x;
	int dy = dragPoint.y-clickPoint.y;
	
	Vec3 axis = new Vec3(-dy*DRAG_SCALE, dx*DRAG_SCALE, 0.0);
	double angle = -axis.length();
	axis.normalize();
	axis = viewToWorld.timesDirection(axis);

	CoordinateSystem c = oldCoords.duplicate();
	c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
	c.transformCoordinates(Mat4.axisRotation(axis, angle));
	c.transformCoordinates(Mat4.translation(rotationCenter.x, rotationCenter.y, rotationCenter.z));
	
	view.getCamera().setCameraCoordinates(c);
  }
  
  private void rotateLandscape(WidgetMouseEvent e, ViewerCanvas view, Point clickPoint, Vec3 vertical)
  {
	Point dragPoint = e.getPoint();
	int dx = dragPoint.x-clickPoint.x;
	int dy = dragPoint.y-clickPoint.y;

	Mat4 rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dy*DRAG_SCALE);
	rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE).times(rotation);
    
	if (!rotation.equals(Mat4.identity()))
	{
		CoordinateSystem c = oldCoords.duplicate();
		c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
		c.transformCoordinates(rotation);
    
		// Prevent tilting forward or back more than 90 degrees.
		// almost works
		if (c.getUpDirection().y < 0.0)
		{
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
  }

  @Override
  public void drawOverlay(ViewerCanvas view)
  {
    if (theWindow != null && view.tilting)
	{
	  for (int i=0; i<4; i++)
		view.drawLine(viewCenter, Math.PI/2.0*i+angle, 0.0, r, view.teal);
	  
	  view.drawLine(viewCenter, -Math.PI/2.0, r*0.1, r, view.red);
	  view.drawLine(viewCenter, Math.PI/2.0, r*0.1, r, view.red);
	  view.drawLine(viewCenter, Math.PI, r*0.1, r, view.blue);
	  view.drawLine(viewCenter, 0.0, r*0.1, r, view.blue);
	  
	  // draw dial lines
	  for (int i=0; i<24; i++)
		view.drawLine(viewCenter, Math.PI/12.0*i+angle, r/.45*.4, r, view.ghost);
		
	  view.drawCircle(viewCenter, r, 48, view.ghost);
      view.drawCircle(viewCenter, r/.45*.4, 48, view.teal);
	}
  }
}

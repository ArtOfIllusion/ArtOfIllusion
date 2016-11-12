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
  private double angle, distToRot, xAngle, yAngle, r;
 
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
    Camera cam = view.getCamera();
    oldCoords = cam.getCameraCoordinates().duplicate();
	
	// Make sure that the rotation Center is on Camera Z-axis.
	// Some plugins like PolyMesh can have them separated
	view.setRotationCenter(oldCoords.getOrigin().plus(oldCoords.getZDirection().times(view.getDistToPlane())));
	
    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    viewToWorld = cam.getViewToWorld();
    rotationCenter = view.getRotationCenter();
	distToRot = oldCoords.getOrigin().minus(rotationCenter).length();
	view.mouseDown = true;
  }
 
	@Override
  	public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
	{
		if (e.getPoint() != clickPoint && view.getBoundCamera() == null) // This is needed even if the mouse has not been dragged yet.
			view.setOrientation(ViewerCanvas.VIEW_OTHER);
		switch (view.getNavigationMode()) 
		{
			case ViewerCanvas.NAVIGATE_MODEL_SPACE:
				dragRotateSpace(e, view);
				break;
			case ViewerCanvas.NAVIGATE_MODEL_LANDSCAPE:
				dragRotateLandscape(e, view);
				break;
			case ViewerCanvas.NAVIGATE_TRAVEL_SPACE:
				dragRotateTravelSpace(e, view);
				break;
			case ViewerCanvas.NAVIGATE_TRAVEL_LANDSCAPE:
				dragRotateTravelLandscape(e, view);
				break;
			default:
				break;
		}
		view.viewChanged(false);	
		if (view.getBoundCamera() != null)
			repaintAllViews();
		else
			view.repaint();
		setExtGraphs(view);
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

		if (controlDown && ! e.isShiftDown())
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
		else if (e.isShiftDown() && ! controlDown)
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

		if (controlDown && ! e.isShiftDown())
		{
			view.tilting = true;
			tilt(e, view, clickPoint);
			return;
		}
		else if (controlDown && e.isShiftDown()){
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
		if (e.isShiftDown() && !controlDown)
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
		if (e.isShiftDown() && controlDown)
		{
			panLandscape(e, view, clickPoint, vertical);
			return;
		}
		else
		{
			double dragAngleFw = dy*DRAG_SCALE;
			if (dragAngleFw > Math.PI) dragAngleFw = Math.PI;
			if (dragAngleFw < -Math.PI) dragAngleFw = -Math.PI;
			rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dragAngleFw);
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
	private void dragRotateTravelLandscape(WidgetMouseEvent e, ViewerCanvas view)
	{
		//This is modified from AoI 3.0
		view.setOrientation(ViewerCanvas.VIEW_OTHER);
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
		if (e.isShiftDown() && !controlDown)
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
		if (e.isShiftDown() && controlDown)
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
    //mouseDragged(e, view); // I wonder why this extra one was there. Seemed to cause somethign unexpected.
	view.mouseDown = false;
	view.tilting = false;

    Point dragPoint = e.getPoint();
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
	
	// If the mouse was not dragged then center to the given point
	if (dragPoint.x == clickPoint.x && dragPoint.y == clickPoint.y)
	    view.centerToPoint(dragPoint);
	  
	wipeExtGraphs();
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
  
  private void repaintAllViews()
  {
	for (ViewerCanvas v : theWindow.getAllViews())
		v.repaint();
  }

  public void setExtGraphs(ViewerCanvas view)
  {
	for (ViewerCanvas v : theWindow.getAllViews()){
      if (v == view){
		v.extRC = null;
		v.extCC = null;
		v.extC0 = null;
		v.extC1 = null;
		v.extC2 = null;
		v.extC3 = null;
	  }
	  else{
	    v.extRC = new Vec3(view.getRotationCenter());	
	    v.extCC = new Vec3(view.getCamera().getCameraCoordinates().getOrigin().plus(view.getCamera().getCameraCoordinates().getZDirection().times(0.0001)));
		v.extC0 = view.getCamera().convertScreenToWorld(new Point(0, 0), view.getDistToPlane());
		v.extC1 = view.getCamera().convertScreenToWorld(new Point(view.getBounds().width, 0), view.getDistToPlane());
		v.extC2 = view.getCamera().convertScreenToWorld(new Point(0, view.getBounds().height), view.getDistToPlane());
		v.extC3 = view.getCamera().convertScreenToWorld(new Point(view.getBounds().width, view.getBounds().height), view.getDistToPlane());
		v.repaint();
	  }
    }
  }
  public void wipeExtGraphs()
  {
	for (ViewerCanvas v : theWindow.getAllViews()){
		v.extRC = null;
		v.extCC = null;
		v.extC0 = null;
		v.extC1 = null;
		v.extC2 = null;
		v.extC3 = null;
		v.repaint();
    }
  }

  private void tilt(WidgetMouseEvent e, ViewerCanvas view, Point clickPoint)
  {
	int d = Math.min(view.getBounds().width, view.getBounds().height);
	r = d*0.4;
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
	Camera cam = view.getCamera();
	double dts = cam.getDistToScreen();
	
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

		cam.setCameraCoordinates(c);
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

		//double dragAngleFw = dy*DRAG_SCALE;
		//if (dragAngleFw > Math.PI) dragAngleFw = Math.PI;
		//if (dragAngleFw < -Math.PI) dragAngleFw = -Math.PI;
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
	//view.drawLine(new Point(100,100), new Point(200, 0), Color.RED);
    if (view.tilting){
 	  //view.drawLine(viewCenter, p1, view.red);
	  //view.drawLine(viewCenter, new Point(viewCenter.x, viewCenter.y+(int)r), view.gray);
	  //view.drawLine(viewCenter, new Point(viewCenter.x, viewCenter.y-(int)r), view.gray);
	  //view.drawLine(viewCenter, p0, view.blue);
	  for (int i=0; i<4; i++)
		view.drawLine(viewCenter, Math.PI/2.0*i, 0.0, r, view.gray);
	  for (int i=0; i<24; i++)
		view.drawLine(viewCenter, Math.PI/12.0*i, r*0.8, r, view.gray);
	  view.drawLine(viewCenter, -Math.PI/2.0+angle, r*0.1, r, view.red);
	  view.drawLine(viewCenter, Math.PI/2.0+angle, r*0.1, r, view.red);
	  view.drawLine(viewCenter, Math.PI+angle, r*0.1, r, view.blue);
	  view.drawLine(viewCenter, angle, r*0.1, r, view.blue);
	  
	  view.drawCircle(viewCenter, r, 60, view.gray);
      view.drawCircle(viewCenter, r*.8, 60, view.gray);
	  
	}
  }
}
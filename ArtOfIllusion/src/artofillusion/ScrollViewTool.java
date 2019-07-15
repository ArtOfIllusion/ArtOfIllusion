/* Copyright (C) 2017-2019 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
*/

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import artofillusion.texture.UVMappingWindow;
import buoy.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

/** ScrollViewTool is a tool to handle mouse scroll wheel events in scene and object views. 
    It moves the viewpoint in view z-directi and/or in some cases changes view orientation. */

public class ScrollViewTool
{
	private EditingWindow window;
	private ViewerCanvas view;
	private Camera camera;
	private double distToPlane;
	private double scrollRadius, scrollBlend, scrollBlendX, scrollBlendY;
    private int navigationMode, scrollSteps, startOrientation;
    private Vec3 startZ, startUp;
	private Rectangle bounds;
	private Point mousePoint;
	private CoordinateSystem startCoords;
	private ObjectInfo boundCamera;

	public ScrollViewTool(EditingWindow ew)
	{
		window = ew;
		scrollTimer.setCoalesce(false);
	}

	protected void mouseScrolled(MouseScrolledEvent e, ViewerCanvas v)
	{
	    scrollSteps = v.scrollBuffer;
        v.scrollBuffer = 0;
        v.mouseMoving = false;
		view = v;
		view.scrolling = true;
		distToPlane = view.getDistToPlane();
		navigationMode = view.getNavigationMode();
		bounds = view.getBounds();
		camera = view.getCamera();
		boundCamera = view.getBoundCamera();
        if (!scrollTimer.isRunning())
            startCoords = camera.getCameraCoordinates().duplicate();
		
		// Make sure that the rotation Center is on Camera Z-axis.
		// After a SceneCamera is read from a file, that may not be the case.
        // Any bound should have a 'distToPlane' that should be saved with the object.

		CoordinateSystem coords = camera.getCameraCoordinates();
        startZ  = new Vec3(coords.getZDirection());
        startUp = new Vec3(coords.getUpDirection());
		view.setRotationCenter(coords.getOrigin().plus(startZ.times(distToPlane)));
		mousePoint = view.mousePoint = e.getPoint();
		scrollTimer.restart(); // The timer takes case of the graphics and updating the children of a camera object

		switch (navigationMode) 
		{
			case ViewerCanvas.NAVIGATE_MODEL_SPACE:
			case ViewerCanvas.NAVIGATE_MODEL_LANDSCAPE:
				scrollMoveModel(e);
				break;
			case ViewerCanvas.NAVIGATE_TRAVEL_SPACE:
			case ViewerCanvas.NAVIGATE_TRAVEL_LANDSCAPE:
				scrollMoveTravel(e);
				break;
			default:
				break;
		}
		if (boundCamera != null)
			boundCamera.getCoords().copyCoords(view.getCamera().getCameraCoordinates());
		view.frustumShape.update();
		if (window != null &&
		   (ArtOfIllusion.getPreferences().getDrawActiveFrustum() || 
		   (ArtOfIllusion.getPreferences().getDrawCameraFrustum() && view.getBoundCamera() != null)))
			window.updateImage();
		else
			view.repaint();
		view.viewChanged(false);
	}

	private void scrollMoveModel(MouseScrolledEvent e)
	{
		int amount = e.getWheelRotation();
		if (!e.isAltDown())
			amount *= 10;
		if (ArtOfIllusion.getPreferences().getReverseZooming())
			amount *= -1;
		if (view.isPerspective())
			distToPlane = distToPlane*Math.pow(1.01, amount);
		else
		{
			view.setScale(view.getScale()*Math.pow(1.0/1.01, amount));
			distToPlane = camera.getDistToScreen()*100.0/view.getScale();
			camera.setScreenParamsParallel(view.getScale(), bounds.width, bounds.height);
		}
		CoordinateSystem coords = camera.getCameraCoordinates();
		Vec3 newPos = view.getRotationCenter().plus(coords.getZDirection().times(-distToPlane));
		coords.setOrigin(newPos);
		camera.setCameraCoordinates(coords);
		view.setDistToPlane(distToPlane);
	}

	private void scrollMoveTravel(MouseScrolledEvent e)
	{	
		int amount = e.getWheelRotation();
		if (!e.isAltDown())
			amount *= 10;
		if (ArtOfIllusion.getPreferences().getReverseZooming())
			amount *= -1;
		
		Point scrollPoint = e.getPoint();
		int cx = bounds.width/2;
		int cy = bounds.height/2;
		int d = Math.min(bounds.width, bounds.height);
		
		Vec3 axis, oldPos, newPos;
		double angle, deltaZ, deltaY;
		CoordinateSystem coords = camera.getCameraCoordinates().duplicate();
		
		if (navigationMode == ViewerCanvas.NAVIGATE_TRAVEL_SPACE)
		{
			int rx = scrollPoint.x - cx;
			int ry = scrollPoint.y - cy;
			scrollRadius = Math.sqrt(rx*rx + ry*ry);
			if (scrollRadius < 0.1*d) scrollRadius = 0.1*d;
			if (scrollRadius > 0.4*d) scrollRadius = 0.4*d;
			scrollBlend = (scrollRadius-0.1*d)/(0.3*d);
			
			axis = new Vec3(-ry, rx, 0.0);
			axis.normalize();
			axis = camera.getViewToWorld().timesDirection(axis);
			angle = scrollRadius*scrollBlend*amount*0.00002;
			
			deltaZ = -distToPlane*0.01*amount*(1.0-scrollBlend);
			deltaY = 0.0;
		
			// Calculate the turn
			Vec3 location = coords.getOrigin();
			coords.transformCoordinates(Mat4.translation(-location.x, -location.y, -location.z));
			coords.transformCoordinates(Mat4.axisRotation(axis, angle));
			coords.transformCoordinates(Mat4.translation(location.x, location.y, location.z));
		
			// Calculate the move
			oldPos = new Vec3(coords.getOrigin());
			newPos = oldPos.plus(coords.getZDirection().times(deltaZ));
			coords.setOrigin(newPos);
		}
		
		else if (navigationMode == ViewerCanvas.NAVIGATE_TRAVEL_LANDSCAPE)
		{
			double scrollX = scrollPoint.x - cx;
			if (Math.abs(scrollX) < 0.1*d) scrollX = 0.1*d*Math.signum(scrollX);
			if (Math.abs(scrollX) > 0.4*d) scrollX = 0.4*d*Math.signum(scrollX);
			double scrollBlendX = (Math.abs(scrollX)-0.1*d)/(0.3*d);
			
			double scrollY = scrollPoint.y - cy;
			if (Math.abs(scrollY) < 0.1*d) scrollY = 0.1*(double)d*Math.signum(scrollY);
			if (Math.abs(scrollY) > 0.4*d) scrollY = 0.4*(double)d*Math.signum(scrollY);
			double scrollBlendY = (Math.abs(scrollY)-0.1*d)/(0.3*d);
		
			axis = new Vec3(0,1,0);
			angle = scrollX*scrollBlendX*amount*0.00002;
			
			deltaZ = -distToPlane*0.01*amount*(1.0-Math.max(scrollBlendX, scrollBlendY));
			deltaY = distToPlane*0.002*amount*(scrollBlendY)*Math.signum(scrollY);
		
			// Calculate the turn
			Vec3 location = coords.getOrigin();
			coords.transformCoordinates(Mat4.translation(-location.x, -location.y, -location.z));
			coords.transformCoordinates(Mat4.axisRotation(axis, angle));
			coords.transformCoordinates(Mat4.translation(location.x, location.y, location.z));
		
			// Calculate the move
			Vec3 hDir, vDir;
			vDir = new Vec3 (0,1,0);
			if (coords.getZDirection().z < 0.0)
				hDir = new Vec3 (coords.getZDirection().plus(coords.getUpDirection()));
			else
				hDir = new Vec3 (coords.getZDirection().minus(coords.getUpDirection()));
			hDir.y = 0.0;
			hDir.normalize();
		
			oldPos = new Vec3(coords.getOrigin());
			newPos = oldPos.plus(hDir.times(deltaZ));
			newPos = newPos.plus(vDir.times(deltaY));
			coords.setOrigin(newPos);
		}
		else 
			return;
		
		if(boundCamera == null)
            if(!coords.getZDirection().equals(startZ) || !coords.getUpDirection().equals(startUp))
                view.setOrientation(ViewerCanvas.VIEW_OTHER);
		camera.setCameraCoordinates(coords);
		view.setRotationCenter(newPos.plus(coords.getZDirection().times(distToPlane)));
	}

	public void mouseStoppedScrolling()
	{
        if (window != null && boundCamera != null)
        {
            UndoRecord undo = new UndoRecord(window, false, UndoRecord.COPY_COORDS, new Object [] {boundCamera.getCoords(), startCoords});
            moveCameraChildren(boundCamera, boundCamera.getCoords().fromLocal().times(startCoords.toLocal()), undo);
            window.setUndoRecord(undo);
        }
        window.updateImage();
        view.viewChanged(false);
	}

	private Timer scrollTimer = new Timer(500, new ActionListener() 
	{
		public void actionPerformed(ActionEvent e) 
		{
			scrollTimer.stop();
			view.scrolling = false;
			view.mousePoint = null;
			mouseStoppedScrolling();
		}
	});

	/** 
	    This is called recursively to move any children of a bound camera. 
	*/
    private void moveCameraChildren(ObjectInfo parent, Mat4 transform, UndoRecord undo)
	{	
		for (int i = 0; i < parent.getChildren().length; i++)
		{
			CoordinateSystem coords = parent.getChildren()[i].getCoords();
            CoordinateSystem previousCoords = coords.duplicate();
			coords.transformCoordinates(transform);
            undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {coords, previousCoords});
            moveCameraChildren(parent.getChildren()[i], transform, undo);
		}  
	}
}

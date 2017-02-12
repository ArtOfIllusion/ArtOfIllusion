/* Copyright (C) 2017 by Petri Ihalainen

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
import buoy.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

public class ScrollViewTool
{
	private EditingWindow window;
	private MouseScrolledEvent event;
	private ViewerCanvas view;
	private Camera camera;
	private double distToPlane;
	private double scrollRadius, scrollBlend, scrollBlendX, scrollBlendY; // for graphics
	private int navigationMode;
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
		event = e;
		view = v;
		view.scrolling = true;
		distToPlane = view.getDistToPlane();
		navigationMode = view.getNavigationMode();
		bounds = view.getBounds();
		camera = view.getCamera();
		boundCamera = view.getBoundCamera();
		if (boundCamera != null)
			startCoords = boundCamera.getCoords().duplicate();
		
		mousePoint = view.mousePoint = e.getPoint();
		scrollTimer.restart(); // The timer takes case of teh graphics and updating the children of a camera object

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
		
		if (boundCamera != null && window != null) // wonder why the window is here...
		{
			boundCamera.setCoords(camera.getCameraCoordinates().duplicate());
			moveCameraChildren(boundCamera, boundCamera.getCoords().fromLocal().times(startCoords.toLocal()));

		}
		setAuxGraphs(view);
		repaintAllViews();
		view.viewChanged(false);
	}
		
	private void scrollMoveModel(MouseScrolledEvent e)
	{
		int amount = e.getWheelRotation();
		if (!e.isAltDown())
			amount *= 10;
		if (ArtOfIllusion.getPreferences().getReverseZooming())
			amount *= -1;
		if (camera.isPerspective())
		{
			CoordinateSystem coords = camera.getCameraCoordinates();
			double oldDist = distToPlane;
			//double newDist = oldDist*Math.pow(1.0/1.01, amount); // This woud reverse the action
			double newDist = oldDist*Math.pow(1.01, amount);
			Vec3 oldPos = new Vec3(coords.getOrigin());
			Vec3 newPos = view.getRotationCenter().plus(coords.getZDirection().times(-newDist));
			coords.setOrigin(newPos);
			camera.setCameraCoordinates(coords);
			view.setDistToPlane(newDist);
			distToPlane = newDist; // for ext graphs
		}
		else
		{
			view.setScale(view.getScale()*Math.pow(1.0/1.01, amount));
		}
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
			
			if (scrollBlend < 0.5)
				view.blendColorR = blendColor(view.green, view.yellow, scrollBlend*2.0);
			else
				view.blendColorR = blendColor(view.yellow, view.red, scrollBlend*2.0-1.0);
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
			
			if (scrollBlendX < 0.5)
				view.blendColorX = blendColor(view.green, view.yellow, scrollBlendX*2.0);
			else
				view.blendColorX = blendColor(view.yellow, view.red, scrollBlendX*2.0-1.0);
			
			if (scrollBlendY < 0.5)
				view.blendColorY = blendColor(view.green, view.yellow, scrollBlendY*2.0);
			else
				view.blendColorY = blendColor(view.yellow, view.red, scrollBlendY*2.0-1.0);
		}
		else 
			return;
		
		camera.setCameraCoordinates(coords);
		view.setRotationCenter(newPos.plus(coords.getZDirection().times(distToPlane)));
	}

	public void mouseStoppedScrolling()
	{
		// This should set an undorecord if a camera moved
		wipeExtGraphs();
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

	private Color  blendColor(Color color0, Color color1, double blend)
	{
		int R = (int)(color0.getRed()*(1.0-blend) + color1.getRed()*blend);
		int G = (int)(color0.getGreen()*(1.0-blend) + color1.getGreen()*blend);
		int B = (int)(color0.getBlue()*(1.0-blend) + color1.getBlue()*blend);
		
		return new Color(R, G, B);
	}

	/** 
	    This is called recursively to move any children of a bound camera. 
		This does not set an undo record.
	*/
	private void moveCameraChildren(ObjectInfo parent, Mat4 transform)
	{	
		for (int i = 0; i < parent.getChildren().length; i++)
		{
			CoordinateSystem coords = parent.getChildren()[i].getCoords();
			coords.transformCoordinates(transform);
			moveCameraChildren(parent.getChildren()[i], transform);
		}  
	}

	/** This is used when a SceneCamera moves in the scene */
	private void repaintAllViews()
	{
		ViewerCanvas[] views = window.getAllViews();
			for (ViewerCanvas v : views)
				v.repaint();
	}

	public void setExtGraphs(ViewerCanvas view)
	{
		for (ViewerCanvas v : window.getAllViews())
			if (v != view)
				v.extGraphs.set(view, true);
	}

	public void wipeExtGraphs()
	{
		for (ViewerCanvas v : window.getAllViews())
			v.extGraphs.wipe();
	}

	/** Maybe some day? */
	public void drawOverlay()
	{}
}

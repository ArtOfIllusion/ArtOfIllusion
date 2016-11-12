/* Copyright (C) 2016 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.*;
import artofillusion.ui.*;
import artofillusion.math.*;
import artofillusion.object.ObjectInfo;
import javax.swing.Timer;
import java.awt.event.*;
import java.awt.*;

/**
	ViewAnimation is the animation engine, that is used to produce eg. smooth swithcing between 
	view orientations. It sets the animation to happen within a maximum duration so that shorter 
	transitions happen at slower speed than larger ones, but still take less time to perform.
	
	ViewAnimation checks from user preferences if the user wants to use animations or not.
	
	The coalesce setting should take care, that even with heavier scenes on slower hardware the 
	animation does not consume more time than the setting allows. It just uses fewer frames.
	
	ViewAnimation also takes care of sending a ViewChanged event and repainting the 
	view after the animation is done. So the calling method won't have to.
*/

public class ViewAnimation
{	
	EditingWindow theWindow;
	boolean animate = ArtOfIllusion.getPreferences().getUseViewAnimations();
	double maxDuration = ArtOfIllusion.getPreferences().getMaxAnimationDuration(); // Seconds
	double displayFrq =  ArtOfIllusion.getPreferences().getAnimationFrameRate();  //Hz
	double interval = 1.0/displayFrq; //
	int timerInterval =(int)(interval*1000);

	int steps = 0, step = 0, firstStep = 1;

	CoordinateSystem startCoords, endCoords, aniCoords;
	Vec3 endRotationCenter, rotStart, rotAni, aniZ, aniOrigin;
	ViewerCanvas view;
	Camera cam;
	double[] startAngles, endAngles;
	double startDist, endDist, aniDist, distanceFactor, moveDist; 
	double startWeightLin, endWeightLin, startWeightExp, endWeightExp;
	double angleX, angleY, angleZ, angleMax;
	double startScale, endScale, scalingFactor;
	double timeRot, timeScale, timeDist, timeMove, timeAni;
	double rotSlope = 1.0, moveSlope = 1.5, scaleSlope = .1, distSlope = .1;
	int endOrientation;
	long msStart, msEnd, ms1st=0, msLast, msLatest;

	public ViewAnimation(EditingWindow win)
	{
		theWindow = win;
	}
	
	/* The timer that keeps launcing animation 'frames' */

	private Timer timer = new Timer(timerInterval, new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{	
				timer.setCoalesce(false);
				if (step > steps)
					endAnimation();
				else 
					animationStep();
			}
		});

	/** 
	 * Start the animation sequence 
	 */
	public void start(ViewerCanvas view, CoordinateSystem endCoords, Vec3 endRotationCenter, double endScale, int endOrientation)
	{
		checkPreferences(); // This only works for the 'animate'
		this.view = view;
		this.endCoords = endCoords;
		this.endRotationCenter = endRotationCenter;		
		this.endScale = endScale;
		this.endOrientation = endOrientation;
		cam = view.getCamera();
		//startOrientation = view.getOrientation();
		
		if (! animate){
			endAnimation(); // Go directly to the last frame
			return;
		}

		startCoords = cam.getCameraCoordinates().duplicate();
		aniCoords = startCoords.duplicate();
		rotStart = view.getRotationCenter(); // get from view
		startScale = view.getScale(); // get from view
		
		startAngles = startCoords.getRotationAngles();
		endAngles = endCoords.getRotationAngles();
		startDist = (startCoords.getOrigin().minus(rotStart).length());
		endDist  = (endCoords.getOrigin().minus(endRotationCenter).length());

		if (noMove()){
			endAnimation(); // did not help...
			return;
		}

		aniDist = startDist;
		moveDist = (endRotationCenter.minus(rotStart).length());
		step = firstStep;

		// CHECKING ROTATIONS
		//========================
		// These are here because the logic in angles of Bottom view differs from Top and Front
		// Helps in most cases to make the turn cleaner.
	
		if (startAngles[0] == 90.0 && startAngles[1] == 0.0 && startAngles[2] == 180.0)
		{
			startAngles[1] = 180.0;
			startAngles[2] = 0.0;
		}
		if (endAngles[0] == 90.0 && endAngles[1] == 0.0 && endAngles[2] == 180.0)
		{
			endAngles[1] = 180.0;
			endAngles[2] = 0.0;
		}

		//-------------
		if (endAngles[0]-startAngles[0] < -180.0) endAngles[0] += 360.0; 
		if (endAngles[1]-startAngles[1] < -180.0) endAngles[1] += 360.0; 
		if (endAngles[2]-startAngles[2] < -180.0) endAngles[2] += 360.0; 

		if (endAngles[0]-startAngles[0] > 180.0) endAngles[0] -= 360.0; 
		if (endAngles[1]-startAngles[1] > 180.0) endAngles[1] -= 360.0; 
		if (endAngles[2]-startAngles[2] > 180.0) endAngles[2] -= 360.0; 
		
		// Find the largest rotation to define the needed time
		angleMax = Math.abs(endAngles[0]-startAngles[0]);
		if (Math.abs(endAngles[1]-startAngles[1]) > angleMax) angleMax = Math.abs(endAngles[1]-startAngles[1]);
		if (Math.abs(endAngles[2]-startAngles[2]) > angleMax) angleMax = Math.abs(endAngles[2]-startAngles[2]);
		
		timeRot = maxDuration*((1.0-1.0/(1.0+angleMax/180.0*rotSlope)));

		timeScale = 0.0;
		if (endScale > startScale)
			timeScale = maxDuration*(1.0-startScale/(startScale+scaleSlope*(endScale-startScale)));
		if (startScale > endScale)
			timeScale = maxDuration*(1.0-endScale/(endScale+scaleSlope*(startScale-endScale)));


		timeDist = 0.0;
		if (endDist > startDist)
			timeDist = maxDuration*(1.0-startDist/(startDist+distSlope*(endDist-startDist)));
		if (startDist > endDist)
			timeDist = maxDuration*(1.0-endDist/(endDist+distSlope*(startDist-endDist)));
			
		timeMove = 0.0;
		if (view.isPerspective())
		{
			double pixS = moveDist*2000/startDist;
			double pixE = moveDist*2000/endDist;
			double pixA = pixS+pixE; // Take both to account. "Average" happens in the next equation.

			timeMove = maxDuration*(1.0-1.0/(1.0+pixA/3200*moveSlope));
		}
		else
		{
			double pixS = moveDist*startScale;
			double pixE = moveDist*endScale;
			double pixA = pixS+pixE; // Take both to account. "Average" happens in the next equation.

			timeMove = maxDuration*(1.0-1.0/(1.0+pixA/3200*moveSlope));
		}

		// Finding maximum time of the candidates
		timeAni = 0.0;
		
		if (timeAni < timeRot) timeAni = timeRot;
		if (timeAni < timeScale) timeAni = timeScale;
		if (timeAni < timeDist) timeAni = timeDist;
		if (timeRot == 0.0)
			if (timeAni < timeMove) timeAni = timeMove;
				
		if (timeAni == 0.0) // zero for time = nothing moves & division by zero next --> Blank view.
		{
			endAnimation();
			return;
		}
		
		steps = (int)(timeAni/interval);		
		scalingFactor = Math.pow((endScale/startScale),(1.0/steps));
		distanceFactor = Math.pow((endDist/startDist),(1.0/steps));

		if (endOrientation != view.getOrientation())
		{	
			view.setOrientation(ViewerCanvas.VIEW_OTHER); // in case the move is interrupted
			view.viewChanged(false);
		}
		
		// Now we  know all we need to know to launch the animation sequence.
		// Restart because the previous move could still be running.
		timer.restart();
	}

	/* Play one step of the animation */

	private void animationStep()
	{	
		startWeightLin = (double)(steps-step)/(double)steps;
		endWeightLin = (double)step/(double)steps;
		
		if(view.isPerspective())
		{
			if (distanceFactor == 1.0 || steps <= 1)
			{
				startWeightExp = startWeightLin;
				endWeightExp = endWeightLin;
			}
			else
			{
				startWeightExp =(Math.pow(1.0/distanceFactor,(double)(steps-step))-1.0)/(1.0/Math.pow(distanceFactor,(double)steps)-1.0);
				endWeightExp = 1.0 - startWeightExp;
			}
		}
		else
		{
			if (scalingFactor == 1.0 || steps <= 1)
			{
				startWeightExp = startWeightLin;
				endWeightExp = endWeightLin;
			}
			else
			{
				startWeightExp =(Math.pow(scalingFactor,(double)(steps-step))-1.0)/(Math.pow(scalingFactor,(double)steps)-1.0);
				endWeightExp = 1.0 - startWeightExp;
			}
		}
		
		aniDist = aniDist*distanceFactor;
		
		rotAni = rotStart.times(startWeightExp).plus(endRotationCenter.times(endWeightExp));
		
		angleX = startAngles[0]*startWeightLin + endAngles[0]*endWeightLin;
		angleY = startAngles[1]*startWeightLin + endAngles[1]*endWeightLin;
		angleZ = startAngles[2]*startWeightLin + endAngles[2]*endWeightLin;
		
		aniCoords.setOrientation(angleX, angleY, angleZ);
		aniZ = aniCoords.getZDirection();
		aniZ.normalize();
		aniOrigin = rotAni.plus(aniZ.times(-aniDist));
		
		aniCoords.setOrigin(aniOrigin);
		cam.setCameraCoordinates(aniCoords);
		view.setScale(view.getScale()*scalingFactor);
		view.repaint();
		setExtGraphs();
		step++;
	}

	/* When all the steps of an animation have been played */

	private void endAnimation()
	{
		timer.stop();
		cam.setCameraCoordinates(endCoords);
		view.setScale(endScale);
		view.setRotationCenter(endRotationCenter);
		view.setDistToPlane(endCoords.getOrigin().minus(endRotationCenter).length()); // It seemed to work without this too... But not with SceneCamera
		wipeExtGraphs();
		view.finishAnimation(endOrientation);
		view.viewChanged(false);
	}

	/* Check if there is anything that should move */

	private boolean noMove()
	{
		if (! rotStart.equals(endRotationCenter)) return false;
		if (! startCoords.getOrigin().equals(endCoords.getOrigin())) return false;
		if (startAngles[0] != endAngles[0]) return false;
		if (startAngles[1] != endAngles[1]) return false;
		if (startAngles[2] != endAngles[2]) return false;
		if (startScale != endScale) return false;
		if (view.getOrientation() != endOrientation) return false;
		return true;
	}

	private void checkPreferences()
	{
		// This only works for the boolean to take effect immediately
		// Don't know why?
		animate = ArtOfIllusion.getPreferences().getUseViewAnimations();
		
		//maxDuration = ArtOfIllusion.getPreferences().getMaxAnimationDuration(); // Seconds
		//displayFrq =  ArtOfIllusion.getPreferences().getAnimationFrameRate();  //Hz
		//interval = maxDuration/displayFrq; //
		//timerInterval =(int)(interval*900); // to milliseconds but make it 10% ahead of time
	}
	/*
	// Not used
	public void stop()
	{
		timer.stop();
		step=0;
	}
	*/
  private void setExtGraphs()
  {
	if (theWindow == null)
		return;
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
	    v.extCC = new Vec3(view.getCamera().getCameraCoordinates().getOrigin());
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
}

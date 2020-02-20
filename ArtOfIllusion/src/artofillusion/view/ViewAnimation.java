/* Copyright (C) 2016-2019 by Petri Ihalainen
   Changes copyright (C) 2020 by Maksim Khramov

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
  view after the animation is done, so the calling method won't have to.
*/

public class ViewAnimation
{
  EditingWindow window;
  boolean animate = ArtOfIllusion.getPreferences().getUseViewAnimations();
  double maxDuration = ArtOfIllusion.getPreferences().getMaxAnimationDuration(); // Seconds
  double displayFrq =  ArtOfIllusion.getPreferences().getAnimationFrameRate();  //Hz
  double interval = 1.0/displayFrq; //
  int timerInterval =(int)(interval*1000);
  int steps = 0, step = 1;

  CoordinateSystem startCoords, endCoords, aniCoords;
  Vec3 endRotationCenter, rotStart, rotAni, aniZ, aniOrigin;
  ViewerCanvas view;
  Camera camera;
  ObjectInfo boundCamera;
  double[] startAngles, endAngles;
  double   startDist, endDist, aniDist, distanceFactor, moveDist;
  double   startWeightLin, endWeightLin, startWeightExp, endWeightExp;
  double   angleX, angleY, angleZ, angleMax;
  double   startScale, endScale, scalingFactor, startAngle, endAngle, angleStep, aniAngle;
  double   timeRot, timeScale, timeDist, timeMove, timeAni;
  double   endDistToScreen, refDistToScreen, refDistToPlane, refTangent;
  double   rotSlope = 1.5, moveSlope = 1.5, scaleSlope = .1, distSlope = .1,  perspSlope = 3.0;;
  int      endOrientation, endNavigation;
  long     msStart, msEnd, ms1st=0, msLast, msLatest;
  boolean  endPerspective,changingPerspective, animatingMove, endShowGrid;
  int      viewH, viewW;

  /** A new view animation engine. Each view need's it's own.*/

  public ViewAnimation(EditingWindow win, ViewerCanvas v)
  {
    window = win;
    view = v;
    viewH = view.getBounds().height;
    viewW = view.getBounds().width;
    timer.setCoalesce(false);
  }

  /** Check if the engine is working on an animation, that includes perspetive change */

  public boolean changingPerspective()
  {
    return changingPerspective;
  }

  /** Check if the engine is at an animation, that moves the view camera or changes scale */

  public boolean animatingMove()
  {
    return animatingMove;
  }

  /** The timer that keeps launcing animation 'frames' */

  private Timer timer = new Timer(timerInterval, new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      if (step >= steps)
        endAnimation();
      else
        if (changingPerspective)
          perspectiveStep();
        else
          animationStep();
    }
  });

  /** Start animation of perspective change.
      Camera position will not eventually chnage but the camera will travel
      from/to infinity to produce the perspectve change effect. */

  public void start(boolean nextPerspective)
  {
    if (changingPerspective)
      return;

    camera = view.getCamera();
    endPerspective = nextPerspective;
    endRotationCenter = view.getRotationCenter();
    endOrientation = view.getOrientation();
    endNavigation = view.getNavigationMode();
    endDistToScreen = camera.getDistToScreen();
    refDistToScreen = camera.getDistToScreen();
    refDistToPlane = view.getDistToPlane();
    endCoords = camera.getCameraCoordinates().duplicate();
    if(nextPerspective)
      endScale = 100.0;
    else
      endScale = 100.0*refDistToScreen/refDistToPlane;
    checkPreferences(); // This only works for the 'animate'
    if (! animate)
    {
      endAnimation(); // Go directly to the last frame
      return;
    }

    endShowGrid = view.getShowGrid();
    view.setShowGrid(false);

    this.refDistToPlane = refDistToPlane;
    startCoords = camera.getCameraCoordinates().duplicate();
    aniCoords =  camera.getCameraCoordinates().duplicate();

    double sinComp = view.getBounds().height/2.0/100.0;
    double cosComp = refDistToScreen;
    double halfViewAngle = Math.atan2(sinComp, cosComp);
    double timePersp = 0.0;
    refTangent = (cosComp/sinComp);

    if (nextPerspective)
    {
      startAngle = Math.PI*0.5;
      endAngle =   Math.PI*0.5-halfViewAngle;
    }
    else
    {
      endAngle =   Math.PI*0.5;
      startAngle = Math.PI*0.5-halfViewAngle;
    }

    if (endAngle == startAngle)
      return;
    else
      timePersp = maxDuration*Math.pow((halfViewAngle/Math.PI/2.0), 1.0/perspSlope); // root curve

    steps = (int)(timePersp/interval);
    angleStep = (endAngle - startAngle)/steps;

    step = 1;
    changingPerspective = true;
    boundCamera = view.getBoundCamera();
    timer.restart();
  }

  private void perspectiveStep()
  {
    aniAngle = startAngle + step*angleStep;
    distanceFactor = Math.tan(aniAngle)/refTangent;

    aniDist = refDistToPlane*distanceFactor;
    aniOrigin = endRotationCenter.plus(aniCoords.getZDirection().times(-aniDist));
    aniCoords.setOrigin(aniOrigin);
    camera.setCameraCoordinates(aniCoords);
    view.setDistToPlane(aniDist);
    camera.setDistToScreen(refDistToScreen*distanceFactor);

    // This has to be the last thing before repaint.
    // The view will react to it immediately.

    if (step == 1)
      view.preparePerspectiveAnimation();

    view.repaint();
    step++;
  }

  /**
   * Start the 'linear' animation sequence
   */

  public void start(CoordinateSystem endCoords, Vec3 endRotationCenter, double endScale, int endOrientation, int nextNavigation)
  {
    if (changingPerspective) return;

    this.endCoords = endCoords;
    this.endRotationCenter = endRotationCenter;
    this.endScale = endScale;
    this.endOrientation = endOrientation;
    if (nextNavigation > 1)
      this.endPerspective = true;
    else
      this.endPerspective = view.isPerspectiveSwitch();
    this.endNavigation = nextNavigation;
    this.endShowGrid = view.getShowGrid();
    camera = view.getCamera();
    endDistToScreen = camera.getDistToScreen();

    checkPreferences(); // This only works for the 'animate'
        if (! animate)
        {
      endAnimation(); // Go directly to the last frame
      return;
    }

    // If an animationis redireceted, let's use the original starting point

    if(! animatingMove)
      startCoords = camera.getCameraCoordinates().duplicate();

    aniCoords = camera.getCameraCoordinates().duplicate();
    rotStart = view.getRotationCenter(); // get from view
    startScale = view.getScale(); // get from view
    startAngles = startCoords.getRotationAngles();
    endAngles = endCoords.getRotationAngles();
    startDist = (startCoords.getOrigin().minus(rotStart).length());
    endDist  = (endCoords.getOrigin().minus(endRotationCenter).length());

    if (noMove()){
      endAnimation();
      return;
    }

    aniDist = startDist;
    moveDist = (endRotationCenter.minus(rotStart).length());
    step = 1;

    // CHECKING ROTATIONS
    //========================
    // These are here because the logic in angles of Bottom view differs from Top and Front
    // Helps in most cases to make the turn cleaner but does not always find the shortest
    // possible rotation.

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

    boundCamera = view.getBoundCamera();

    // Now we  know all we need to know to launch the animation sequence.
    // Restart because the previous move could still be running.

    animatingMove = true;
    timer.restart();
  }

  /* Play one step of the animation */

  private void animationStep()
  {
    startWeightLin = (double)(steps-step)/(double)steps;
    endWeightLin = (double)step/(double)steps;

    if(view.isPerspective())
    {
      if (distanceFactor == 1.0 || steps <= 1){
        startWeightExp = startWeightLin;
        endWeightExp = endWeightLin;
      }
      else{
        startWeightExp =(Math.pow(1.0/distanceFactor,(double)(steps-step))-1.0)/(1.0/Math.pow(distanceFactor,(double)steps)-1.0);
        endWeightExp = 1.0 - startWeightExp;
      }
    }
    else
    {
      if (scalingFactor == 1.0 || steps <= 1){
        startWeightExp = startWeightLin;
        endWeightExp = endWeightLin;
      }
      else{
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
    camera.setCameraCoordinates(aniCoords);
    view.setScale(view.getScale()*scalingFactor);
    view.repaint();
    step++;
  }

  /* When all the steps of an animation have been played */

  private void endAnimation()
  {
    timer.stop();
    camera.setCameraCoordinates(endCoords);
    camera.setDistToScreen(endDistToScreen);
    view.setScale(endScale);
    view.setRotationCenter(endRotationCenter);
    view.setDistToPlane(endCoords.getOrigin().minus(endRotationCenter).length()); // It seemed to work without this too... But not with SceneCamera
    view.setShowGrid(endShowGrid);
    view.finishAnimation(endOrientation, endPerspective, endNavigation); // using set-methods for these would loop back to animation
        if (boundCamera != null)
            updateBoundCamera();
        else
        {
      view.viewChanged(false);
      view.repaint();
    }
    changingPerspective = false;
    animatingMove = false;
    }

  /**
   * Check if there is anything that should move.
   * This is for the <b>non-perspectivechanging</b> animations
   */

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

  /* Check how the user has configured the animation engine  */

  private void checkPreferences()
  {
    // This only works for the 'animate' boolean to take effect immediately
    // Don't know why?

    animate = ArtOfIllusion.getPreferences().getUseViewAnimations();
  }

  private void updateBoundCamera()
  {
    if (window != null)
    {
      if (boundCamera != null)
      {
        boundCamera.getCoords().copyCoords(view.getCamera().getCameraCoordinates());
        UndoRecord undo = new UndoRecord(window, false, UndoRecord.COPY_COORDS, boundCamera.getCoords(), startCoords);
        moveChildren(boundCamera, boundCamera.getCoords().fromLocal().times(startCoords.toLocal()), undo);
        window.setUndoRecord(undo);
      }
      window.updateImage();
    }
    view.viewChanged(false);
  }

  private void moveChildren(ObjectInfo parent, Mat4 transform, UndoRecord undo)
  {
    for (int i = 0; i < parent.getChildren().length; i++)
    {
      CoordinateSystem coords = parent.getChildren()[i].getCoords();
      CoordinateSystem oldCoords = coords.duplicate();
      coords.transformCoordinates(transform);
      undo.addCommand(UndoRecord.COPY_COORDS, coords, oldCoords);
      moveChildren(parent.getChildren()[i], transform, undo);
    }
  }
}

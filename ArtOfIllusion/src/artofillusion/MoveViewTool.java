/* Copyright (C) 1999-2007 by Peter Eastman
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

/** MoveViewTool is an EditingTool used for moving the viewpoint. */

public class MoveViewTool extends EditingTool
{
  private Point clickPoint;
  private Mat4 viewToWorld;
  private Vec3 clickPos, oldRotCenter, oldCamPos;
  private boolean controlDown;
  private CoordinateSystem oldCoords;
  private double oldScale, oldDist;

  public MoveViewTool(EditingWindow fr)
  {
    super(fr);
    initButton("moveView");
  }

  @Override
  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("moveViewTool.helpText"));
  }

  @Override
  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  @Override
  public boolean hilightSelection()
  {
/*    if (theWindow instanceof LayoutWindow)
      return false;
    else*/
      return true;
  }

  @Override
  public String getToolTipText()
  {
    return Translate.text("moveViewTool.tipText");
  }

  @Override
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();

    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    clickPos = cam.convertScreenToWorld(clickPoint, cam.getDistToScreen());
    oldCoords = cam.getCameraCoordinates().duplicate();
	oldCamPos = oldCoords.getOrigin();
	oldRotCenter = new Vec3(view.getRotationCenter());
    oldScale = view.getScale();
	//oldDist = oldRotCenter.minus(oldCamPos).length();
	oldDist = view.getDistToPlane(); // distToPlane needs to be kept up to date
  }

  @Override
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
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
	if (view.getBoundCamera() != null)
	{	
		repaintAllViews();
	}
  }

	private void dragMoveTravel(WidgetMouseEvent e, ViewerCanvas view){}
	
	private void dragMoveModel(WidgetMouseEvent e, ViewerCanvas view)
	{
		//
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
		else // Move up down-right-left
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
				move = move.times(oldDist/cam.getDistToScreen());
			
			Mat4 m = Mat4.translation(-move.x, -move.y, -move.z);
			CoordinateSystem newCoords = oldCoords.duplicate();			
			newCoords.transformOrigin(m);
			cam.setCameraCoordinates(newCoords);
			view.setRotationCenter(newCoords.getOrigin().plus(newCoords.getZDirection().times(oldDist)));
		}
		view.viewChanged(false);
		view.repaint();
	}

  @Override
  public void mouseScrolled(MouseScrolledEvent e, ViewerCanvas view)
  {
      switch (view.getNavigationMode()) {
	  case ViewerCanvas.NAVIGATE_MODEL_SPACE:
	  case ViewerCanvas.NAVIGATE_MODEL_LANDSCAPE:
        scrollMoveModel(e, view);
		break;
	  case ViewerCanvas.NAVIGATE_TRAVEL_SPACE:
	  case ViewerCanvas.NAVIGATE_TRAVEL_LANDSCAPE:
        scrollMoveTravel(e, view);
		break;
	  default:
	    break;
	  }
  }
  
  private void scrollMoveModel(MouseScrolledEvent e, ViewerCanvas view)
  {
    int amount = e.getWheelRotation();
    if (!e.isAltDown())
      amount *= 10;
    if (ArtOfIllusion.getPreferences().getReverseZooming())
      amount *= -1;
    if (view.getCamera().isPerspective())
    {
      CoordinateSystem coords = view.getCamera().getCameraCoordinates();
	  CoordinateSystem oldCoords = coords.duplicate();
	  double oldDist = view.getDistToPlane();
	  double newDist = oldDist*Math.pow(1.0/1.01, amount);
	  //double newDist = oldDist*Math.pow(1.01, amount);
	  Vec3 oldPos = new Vec3(coords.getOrigin());
	  Vec3 newPos = view.getRotationCenter().plus(coords.getZDirection().times(-newDist));
	  coords.setOrigin(newPos);
      view.getCamera().setCameraCoordinates(coords);
	  view.setDistToPlane(newDist);
	  
	  if (view.getBoundCamera() != null)
	  {	
		//repaintAllViews();
		ObjectInfo bound = view.getBoundCamera();
        moveChildrenLive(bound, bound.getCoords().fromLocal().times(oldCoords.toLocal()), null);
	  }
      view.viewChanged(false);
      view.repaint();
    }
    else
    {
     view.setScale(view.getScale()*Math.pow(1.0/1.01, amount));
     //view.setScale(view.getScale()*Math.pow(1.01, amount));
    }
  }
  
  private void scrollMoveTravel(MouseScrolledEvent e, ViewerCanvas view)
  {	
	//Vec3 delta = coords.getZDirection().times(-0.1*amount); //??
	//coords.setOrigin(coords.getOrigin().plus(delta));
  }
  
  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    mouseDragged(e, view);
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
  
  /** This is called by mouseDragged and Mouse Scrolled */
  
  private void moveViewZ()
  {
  }
  
  /** This is called recursively during mouseDragged to move any children of a bound camera. */

  private void moveChildrenLive(ObjectInfo parent, Mat4 transform, UndoRecord undo)
  {
    for (int i = 0; i < parent.getChildren().length; i++)
    {
      CoordinateSystem coords = parent.getChildren()[i].getCoords();
      CoordinateSystem oldCoords = coords.duplicate();
	  coords.transformCoordinates(transform);
      moveChildrenLive(parent.getChildren()[i], transform, undo);
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
/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.text.*;

/** SkeletonTool is an EditingTool used for manipulating the skeletons of objects. */

public class SkeletonTool extends EditingTool
{
  private static final int CLICK_TOL = 6;
  private static final int HANDLE_SIZE = 4;
  
  private static final int NO_HANDLES = 0;
  private static final int UNLOCKED_HANDLES = 1;
  private static final int ALL_HANDLES = 2;

  private static int whichHandles = UNLOCKED_HANDLES;
  private Point clickPoint;
  private Vec3 clickPos, handlePos[];
  private boolean hideHandle[], invertDragDir;
  private int clickedHandle;
  private IKSolver ik;
  private Mesh oldMesh, mesh;
  private UndoRecord undo;
  private boolean allowCreating;
  private String helpText;
    
  public SkeletonTool(MeshEditorWindow fr, boolean allowCreating)
  {
    super(fr);
    this.allowCreating = allowCreating;
    initButton("skeleton");
    clickedHandle = -1;
    helpText = Translate.text(allowCreating ? "skeletonTool.helpText" : "skeletonTool.helpTextNoCreate");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(helpText);
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("skeletonTool.tipText");
  }

  /** Find the positions of all the degree-of-freedom handles. */
  
  private void findHandlePositions(Mat4 objToScreen, Joint j, ViewerCanvas view)
  {
    if (j.parent == null || whichHandles == NO_HANDLES)
      hideHandle = new boolean [] {true, true, true, true};
    else if (whichHandles == UNLOCKED_HANDLES)
      hideHandle = new boolean [] {j.angle1.fixed, j.angle2.fixed, j.length.fixed, j.twist.fixed};
    else
      hideHandle = new boolean [4];
    double scale = 70.0/view.getScale();
    handlePos = new Vec3 [] {new Vec3(0.0, scale, 0.0), new Vec3(scale, 0.0, 0.0),
        new Vec3(0.0, 0.0, scale), new Vec3(-scale*0.6, -scale*0.6, scale*0.4)};
    Vec3 jointPos = new Vec3(j.coords.getOrigin());
    objToScreen.transform(jointPos);
    int jointX = (int) jointPos.x;
    int jointY = (int) jointPos.y;
    for (int i = 0; i < hideHandle.length; i++)
    {
      if (hideHandle[i])
        continue;
      j.coords.fromLocal().transformDirection(handlePos[i]);
      handlePos[i].add(j.coords.getOrigin());
      objToScreen.transform(handlePos[i]);
      int x = (int) handlePos[i].x;
      int y = (int) handlePos[i].y;
      if (x == jointX && y == jointY)
        hideHandle[i] = true;
    }
  }
  
  public void drawOverlay(ViewerCanvas view)
  {
    if (ik != null)
      return;
    MeshViewer mv = (MeshViewer) view;
    int selected = mv.getSelectedJoint();
    mesh = (oldMesh == null ? (Mesh) mv.getController().getObject().getObject() : oldMesh);
    Skeleton s = mesh.getSkeleton();
    Joint j = s.getJoint(selected);
    if (j == null)
      return;
    Camera cam = mv.getCamera();
    CoordinateSystem objCoords = mv.getDisplayCoordinates();
    Mat4 objToScreen = cam.getWorldToScreen().times(objCoords.fromLocal());
    
    if (clickedHandle == -1)
    {
      // Draw the handles for each degree of freedom.
      
      findHandlePositions(objToScreen, j, view);
      Vec3 jointPos = new Vec3(j.coords.getOrigin());
      objToScreen.transform(jointPos);
      int jointX = (int) jointPos.x;
      int jointY = (int) jointPos.y;
      for (int i = 0; i < hideHandle.length; i++)
      {
        if (hideHandle[i])
          continue;
        int x = (int) handlePos[i].x;
        int y = (int) handlePos[i].y;
        view.drawLine(new Point(jointX, jointY), new Point(x, y), Color.darkGray);
        view.drawBox(x-HANDLE_SIZE/2, y-HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, ViewerCanvas.handleColor);
      }
    }
    else if (clickedHandle < 2)
    {
      // Draw a circle to show the DOF being moved.
      
      double len = j.length.pos;
      Mat4 m1, m2;
      if (clickedHandle == 0)
      {
        m1 = Mat4.zrotation(j.twist.pos*Math.PI/180.0);
        m2 = j.parent.coords.fromLocal().times(Mat4.yrotation(j.angle2.pos*Math.PI/180.0));
      }
      else
      {
        m1 = Mat4.xrotation(j.angle1.pos*Math.PI/180.0).times(Mat4.zrotation(j.twist.pos*Math.PI/180.0));
        m2 = j.parent.coords.fromLocal();
      }
      m2 = objToScreen.times(m2);
      Point pos[] = new Point [64];
      for (int i = 0; i < pos.length; i++)
      {
        double angle = i*2.0*Math.PI/pos.length;
        Vec3 p = new Vec3(0.0, 0.0, len);
        p = m1.timesDirection(p);
        if (clickedHandle == 0)
          p = m2.times(Mat4.xrotation(angle).timesDirection(p));
        else
          p = m2.times(Mat4.yrotation(angle).timesDirection(p));
        pos[i] = new Point((int) p.x, (int) p.y);
      }
      for (int i = 1; i < pos.length; i++)
        view.drawLine(new Point(pos[i-1].x, pos[i-1].y), new Point(pos[i].x, pos[i].y), Color.darkGray);
      view.drawLine(new Point(pos[0].x, pos[0].y), new Point(pos[pos.length-1].x, pos[pos.length-1].y), Color.darkGray);
    }
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    MeshViewer mv = (MeshViewer) view;
    ViewerCanvas allViews[] = ((MeshEditorWindow) theWindow).getAllViews();
    mesh = (Mesh) mv.getController().getObject().getObject();
    Skeleton s = mesh.getSkeleton();
    Joint selectedJoint = s.getJoint(mv.getSelectedJoint());
    Camera cam = mv.getCamera();
    CoordinateSystem objCoords = mv.getDisplayCoordinates();
    Mat4 objToScreen = cam.getWorldToScreen().times(objCoords.fromLocal());
    
    clickPoint = e.getPoint();
    if (e.isControlDown() && allowCreating)
      {
        // Create a new joint.
        
        undo = new UndoRecord(theWindow, false, UndoRecord.COPY_OBJECT, new Object [] {mesh, mesh.duplicate()});
        Joint j, parent = s.getJoint(mv.getSelectedJoint());
        if (parent == null)
          {
            double distToJoint = cam.getDistToScreen();
            clickPos = cam.convertScreenToWorld(clickPoint, distToJoint);
            objCoords.toLocal().transform(clickPos);
            j = new Joint(new CoordinateSystem(clickPos, Vec3.vz(), Vec3.vy()), null, "Root "+s.getNextJointID());
            j.angle1.fixed = j.angle2.fixed = true;
            s.addJoint(j, -1);
          }
        else
          {
            double distToJoint = cam.getWorldToView().timesZ(parent.coords.getOrigin());
            clickPos = cam.convertScreenToWorld(clickPoint, distToJoint);
            objCoords.toLocal().transform(clickPos);
            Vec3 zdir = clickPos.minus(parent.coords.getOrigin());
            zdir.normalize();
            Vec3 ydir = cam.getCameraCoordinates().getZDirection().cross(zdir);
            ydir.normalize(); 
            j = new Joint(new CoordinateSystem(clickPos, zdir, ydir), parent, "Bone "+s.getNextJointID());
            s.addJoint(j, parent.id);
          }
        for (int k = 0; k < allViews.length; k++)
          ((MeshViewer) allViews[k]).setSelectedJoint(j.id);
        boolean moving[] = new boolean [s.getNumJoints()];
        moving[s.findJointIndex(j.id)] = true;
        ik = new IKSolver(s, mv.getLockedJoints(), moving);
        theWindow.updateImage();
        theWindow.updateMenus();
        oldMesh = (Mesh) mesh.duplicate();
        return;
      }
    
    // See whether a handle was clicked.
    
    if (selectedJoint != null)
    {
      findHandlePositions(objToScreen, selectedJoint, view);
      for (int i = 0; i < hideHandle.length; i++)
      {
        if (!hideHandle[i] && Math.abs(handlePos[i].x-clickPoint.x) <= 0.5*CLICK_TOL && Math.abs(handlePos[i].y-clickPoint.y) <= 0.5*CLICK_TOL)
        {
          clickedHandle = i;
          oldMesh = (Mesh) mesh.duplicate();
          Vec2 jointPos = objToScreen.timesXY(selectedJoint.coords.getOrigin());
          invertDragDir = (handlePos[i].x < jointPos.x);
          theWindow.updateImage();
          return;
        }
      }
    }
    
    // Determine which joint was clicked on.
    
    Joint joint[] = s.getJoints();
    int i;
    for (i = 0; i < joint.length; i++)
      {
        Vec2 pos = objToScreen.timesXY(joint[i].coords.getOrigin());
        if ((clickPoint.x > pos.x-CLICK_TOL) && 
            (clickPoint.x < pos.x+CLICK_TOL) &&
            (clickPoint.y > pos.y-CLICK_TOL) && 
            (clickPoint.y < pos.y+CLICK_TOL))
          break;
      }
    if (i == joint.length)
      {
        // No joint was clicked on, so deselect the selected joint.
        
        for (int j = 0; j < allViews.length; j++)
          ((MeshViewer) allViews[j]).setSelectedJoint(-1);
        theWindow.updateImage();
        theWindow.updateMenus();
        return;
      }
    if (e.isShiftDown())
      {
        // Toggle whether this joint is locked.
        
        for (int j = 0; j < allViews.length; j++)
        {
          MeshViewer v = (MeshViewer) allViews[j];
          if (v.isJointLocked(joint[i].id))
            v.unlockJoint(joint[i].id);
          else
            v.lockJoint(joint[i].id);
        }
        theWindow.updateImage();
        return;
      }
    
    // Make it the selected joint, and prepare to drag it.
    
    for (int j = 0; j < allViews.length; j++)
      ((MeshViewer) allViews[j]).setSelectedJoint(joint[i].id);
    clickPos = joint[i].coords.getOrigin();
    theWindow.updateImage();
    theWindow.updateMenus();
    boolean moving[] = new boolean [s.getNumJoints()];
    moving[s.findJointIndex(joint[i].id)] = true;
    ik = new IKSolver(s, mv.getLockedJoints(), moving);
    oldMesh = (Mesh) mesh.duplicate();
  }
  
  public void mouseDragged(final WidgetMouseEvent e, ViewerCanvas view)
  {
    final MeshViewer mv = (MeshViewer) view;
    final CoordinateSystem objCoords = mv.getDisplayCoordinates();
    final Camera cam = mv.getCamera();
    final Mesh mesh = (Mesh) mv.getController().getObject().getObject();
    final Skeleton s = mesh.getSkeleton();
    final Joint selectedJoint = s.getJoint(mv.getSelectedJoint());

    if (clickedHandle > -1)
    {
      // Adjust a single degree of freedom.
      
      if (undo == null)
        undo = new UndoRecord(getWindow(), false, UndoRecord.COPY_OBJECT, new Object [] {mesh, mesh.duplicate()});
      double dist = clickPoint.x-e.getPoint().x;
      if (invertDragDir)
        dist = -dist;
      Joint origJoint = oldMesh.getSkeleton().getJoint(mv.getSelectedJoint());
      Joint.DOF dof = null;
      Joint.DOF origDof = null;
      String name = null;
      switch (clickedHandle)
      {
        case 0:
          dof = selectedJoint.angle1;
          origDof = origJoint.angle1;
          dist *= 1.8/Math.PI;
          name = "X Bend";
          break;
        case 1:
          dof = selectedJoint.angle2;
          origDof = origJoint.angle2;
          dist *= -1.8/Math.PI;
          name = "Y Bend";
          break;
        case 2:
          dof = selectedJoint.length;
          origDof = origJoint.length;
          dist /= -view.getScale();
          name = "Length";
          break;
        case 3:
          dof = selectedJoint.twist;
          origDof = origJoint.twist;
          dist *= 1.8/Math.PI;
          name = "Twist";
          break;
      }
      dof.set(origDof.pos+dist);
      selectedJoint.recalcCoords(true);
      if (!mv.getSkeletonDetached())
        adjustMesh(mesh);
      mv.getController().objectChanged();
      theWindow.updateImage();
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMaximumFractionDigits(3);
      theWindow.setHelpText(name+": "+nf.format(dof.pos));
      return;
    }

    if (ik == null)
      return;
    boolean converged = false;
    Vec3 target[] = new Vec3 [s.getNumJoints()];
    int jointIndex = s.findJointIndex(selectedJoint.id);
    ActionProcessor process = view.getActionProcessor();
    do
    {
      Vec3 jointPos = objCoords.fromLocal().times(selectedJoint.coords.getOrigin());
      double distToJoint = cam.getWorldToView().timesZ(jointPos);
      Vec3 goal = cam.convertScreenToWorld(e.getPoint(), distToJoint);
      objCoords.toLocal().transform(goal);

      if (undo == null)
        undo = new UndoRecord(SkeletonTool.this.getWindow(), false, UndoRecord.COPY_OBJECT, new Object [] {mesh, mesh.duplicate()});
      target[jointIndex] = goal;
      converged = ik.solve(target, 100);
      if (!mv.getSkeletonDetached())
        adjustMesh(mesh);
      ((ObjectViewer) view).getController().objectChanged();
      theWindow.updateImage();
    } while (!converged && (process == null || (!process.hasEvent() && !process.hasBeenStopped())));
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (undo == null && e.getClickCount() == 2 && !e.isShiftDown() && !e.isControlDown())
      ((MeshEditorWindow) theWindow).editJointCommand(); // They double-clicked without dragging.
    ik = null;
    mesh = oldMesh = null;
    clickedHandle = -1;
    if (undo != null)
      theWindow.setUndoRecord(undo);
    undo = null;
    theWindow.setHelpText(helpText);
  }
  
  public void iconDoubleClicked()
  {
    BComboBox dofChoice = new BComboBox(new String [] {
      Translate.text("noDOF"),
      Translate.text("unlockedDOF"),
      Translate.text("allDOF"),
    });
    dofChoice.setSelectedIndex(whichHandles);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("skeletonToolTitle"), 
                new Widget [] {dofChoice}, new String [] {null});
    if (!dlg.clickedOk())
      return;
    whichHandles = dofChoice.getSelectedIndex();
    theWindow.updateImage();
  }
  
  /** Adjust the mesh after the skeleton has changed. */
  
  protected void adjustMesh(Mesh newMesh)
  {
    Skeleton.adjustMesh(oldMesh, newMesh);
  }
}
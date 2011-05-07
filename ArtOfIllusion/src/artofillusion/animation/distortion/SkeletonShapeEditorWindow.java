/* Copyright (C) 2004-2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;

/** The SkeletonShapeEditorWindow class represents the window for editing SkeletonShapeKeyframes. */

public class SkeletonShapeEditorWindow extends MeshEditorWindow implements MeshEditController
{
  private SkeletonShapeTrack track;
  private SkeletonShapeKeyframe keyframe;
  private int keyIndex;
  private double keyTime;
  private Smoothness keySmoothness;
  private BMenu editMenu, skeletonMenu;
  private BMenuItem skeletonMenuItem[];
  private boolean selected[];
  private Runnable onClose;

  public SkeletonShapeEditorWindow(EditingWindow parent, String title, SkeletonShapeTrack track, int keyIndex, Runnable onClose)
  {
    super(parent, title, new ObjectInfo((Object3D) getEditMesh(((ObjectInfo) track.getParent()).getObject()), new CoordinateSystem(), ""));
    this.track = track;
    this.keyIndex = keyIndex;
    this.onClose = onClose;
    Timecourse tc = track.getTimecourse();
    keyframe = (SkeletonShapeKeyframe) tc.getValues()[keyIndex];
    keySmoothness = tc.getSmoothness()[keyIndex].duplicate();
    keyTime = tc.getTimes()[keyIndex];
    ObjectInfo obj = (ObjectInfo) track.getParent();
    selected = new boolean [oldMesh.getVertices().length];

    FormContainer content = new FormContainer(new double [] {0, 1}, new double [] {1, 0, 0});
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    content.add(helpText = new BLabel(), 0, 1, 2, 1);
    content.add(viewsContainer, 1, 0);
    RowContainer buttons = new RowContainer();
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "doCancel"));
    content.add(buttons, 0, 2, 2, 1, new LayoutInfo());
    content.add(tools = new ToolPalette(1, 3), 0, 0, new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.NONE, null, null));
    EditingTool metaTool, altTool;
    tools.addTool(defaultTool = new SkeletonTool(this, false) {
      protected void adjustMesh(Mesh newMesh)
      {
        adjustMeshForSkeleton();
      }
    });
    tools.addTool(metaTool = new MoveViewTool(this));
    tools.addTool(altTool = new RotateViewTool(this));
    tools.selectTool(defaultTool);
    for (int i = 0; i < theView.length; i++)
    {
      MeshViewer view = (MeshViewer) theView[i];
      view.setMetaTool(metaTool);
      view.setAltTool(altTool);
      view.setScene(parent.getScene(), obj);
    }
    createEditMenu();
    createSkeletonMenu();
    createViewMenu();
    recursivelyAddListeners(this);
    UIUtilities.applyDefaultFont(content);
    UIUtilities.applyDefaultBackground(content);
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension windowDim = new Dimension((screenBounds.width*3)/4, (screenBounds.height*3)/4);
    setBounds(new Rectangle((screenBounds.width-windowDim.width)/2, (screenBounds.height-windowDim.height)/2, windowDim.width, windowDim.height));
    tools.requestFocus();
    updateMenus();
    objInfo.getObject().getSkeleton().copy(keyframe.getSkeleton());
    adjustMeshForSkeleton();
  }
  
  private static Mesh getEditMesh(Object3D object)
  {
    while (object instanceof ObjectWrapper)
      object = ((ObjectWrapper) object).getWrappedObject();
    if (object instanceof Mesh)
      return (Mesh) object;
    return object.convertToTriangleMesh(Double.MAX_VALUE);
  }

  void createEditMenu()
  {
    editMenu = Translate.menu("edit");
    menubar.add(editMenu);
    editMenu.add(undoItem = Translate.menuItem("undo", this, "undoCommand"));
    editMenu.add(redoItem = Translate.menuItem("redo", this, "redoCommand"));
    editMenu.addSeparator();
    editMenu.add(Translate.menuItem("properties", this, "editProperties"));
  }
  
  void createSkeletonMenu()
  {
    BMenuItem item;
    skeletonMenu = Translate.menu("skeleton");
    menubar.add(skeletonMenu);
    skeletonMenuItem = new BMenuItem [1];
    skeletonMenu.add(skeletonMenuItem[0] = Translate.menuItem("editBone", this, "editJointCommand"));
    skeletonMenu.add(Translate.menuItem("resetToDefaultPose", this, "resetCommand"));
    skeletonMenu.add(item = Translate.menuItem("createPoseFromGestures", this, "createFromGesturesCommand"));
    Object3D obj = ((ObjectInfo) track.getParent()).getObject();
    item.setEnabled(Actor.getActor(obj) != null);
  }

  /* EditingWindow methods. */

  public void updateMenus()
  {
    super.updateMenus();
    MeshViewer view = (MeshViewer) theView[currentView];
    Skeleton s = keyframe.getSkeleton();
    Joint selJoint = s.getJoint(view.getSelectedJoint());
    skeletonMenuItem[0].setEnabled(selJoint != null);
  }
  
  /** Get the object being edited in this window. */
  
  public ObjectInfo getObject()
  {
    return objInfo;
  }
  
  /** Set the object being edited in this window. */
  
  public void setObject(Object3D obj)
  {
    objInfo.setObject(obj);
    objInfo.clearCachedMeshes();
  }
  
  public void setMesh(Mesh mesh)
  {
    setObject((Object3D) mesh);
  }
  
  /** Get the selection mode. */
  
  public int getSelectionMode()
  {
    return POINT_MODE;
  }
  
  /** This is ignored, since this window only supports one selection mode. */
  
  public void setSelectionMode(int mode)
  {
  }
  
  /** Get an array of flags specifying which vertices are selected. */
  
  public boolean [] getSelection()
  {
    return selected;
  }
  
  /** Set which vertices are selected. */
  
  public void setSelection(boolean selected[])
  {
    this.selected = selected;
    for (ViewerCanvas view : theView)
      view.repaint();
  }
  
  /** Selection distance is not used for anything in this window, so this simply returns
      0 for selected vertices and -1 for unselected vertices. */

  public int[] getSelectionDistance()
  {
    int dist[] = new int [selected.length];
    for (int i = 0; i < selected.length; i++)
      if (!selected[i])
        dist[i] = -1;
    return dist;
  }

  
  protected void doOk()
  {
    parentWindow.setUndoRecord(new UndoRecord(parentWindow, false, UndoRecord.COPY_TRACK, new Object [] {track, track.duplicate(track.getParent())}));
    keyframe.getSkeleton().copy(getObject().getSkeleton());
    track.moveKeyframe(keyIndex, keyTime);
    track.getTimecourse().getSmoothness()[keyIndex] = keySmoothness;
    oldMesh = null;
    dispose();
    if (onClose != null)
      onClose.run();
  }
  
  protected void doCancel()
  {
    oldMesh = null;
    dispose();
  }
  
  /** Adjust the mesh after the skeleton moves. */
  
  private void adjustMeshForSkeleton()
  {
    Object3D obj = ((ObjectInfo) track.getParent()).getObject();
    Actor actor = Actor.getActor(obj);
    Mesh mesh = (Mesh) getObject().getObject();
    if (track.getUseGestures() && actor != null)
      actor.shapeMeshFromGestures((Object3D) mesh);
    else
      Skeleton.adjustMesh(oldMesh, mesh);
  }
  
  /** Reset the skeleton to its default pose. */
  
  protected void resetCommand()
  {
    Object3D obj = ((ObjectInfo) track.getParent()).getObject();
    Actor actor = Actor.getActor(obj);
    Skeleton defaultSkeleton;
    if (actor != null)
      defaultSkeleton = actor.getGesture(0).getSkeleton();
    else
      defaultSkeleton = obj.getSkeleton();
    Object3D editObj = getObject().getObject();
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {editObj, editObj.duplicate()}));
    editObj.getSkeleton().copy(defaultSkeleton);
    adjustMeshForSkeleton();
    objectChanged();
    updateImage();
  }
  
  /** Create a skeleton shape by blending gestures. */
  
  protected void createFromGesturesCommand()
  {
    ObjectInfo info = (ObjectInfo) track.getParent();
    final Actor actor = Actor.getActor(info.getObject());
    final Actor.ActorKeyframe key = new Actor.ActorKeyframe();
    new ActorEditorWindow(this, info, actor, key, new Runnable() {
      public void run()
      {
        Skeleton newSkeleton = ((Gesture) key.createObjectKeyframe(actor)).getSkeleton();
        Object3D editObj = getObject().getObject();
        setUndoRecord(new UndoRecord(SkeletonShapeEditorWindow.this, false, UndoRecord.COPY_OBJECT, new Object [] {editObj, editObj.duplicate()}));
        editObj.getSkeleton().copy(newSkeleton);
        adjustMeshForSkeleton();
        objectChanged();
        updateImage();
      }
    });
  }
  
  /** Allow the user to edit keyframe properties. */
  
  protected void editProperties()
  {
    ValueField timeField = new ValueField(keyTime, ValueField.NONE, 5);
    ValueSlider s1Slider = new ValueSlider(0.0, 1.0, 100, keySmoothness.getLeftSmoothness());
    final ValueSlider s2Slider = new ValueSlider(0.0, 1.0, 100, keySmoothness.getRightSmoothness());
    final BCheckBox sameBox = new BCheckBox(Translate.text("separateSmoothness"), !keySmoothness.isForceSame());
    
    sameBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        s2Slider.setEnabled(sameBox.getState());
      }
    });
    s2Slider.setEnabled(sameBox.getState());
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("editKeyframe"), new Widget [] {
      timeField, sameBox, new BLabel(Translate.text("Smoothness")+':'), s1Slider, s2Slider},
      new String [] {Translate.text("Time"), null, null, "("+Translate.text("left")+")",
      "("+Translate.text("right")+")"});
    if (!dlg.clickedOk())
      return;
    if (sameBox.getState())
      keySmoothness.setSmoothness(s1Slider.getValue(), s2Slider.getValue());
    else
      keySmoothness.setSmoothness(s1Slider.getValue());
    keyTime = timeField.getValue();
  }

  /** Given a list of deltas which will be added to the selected vertices, calculate the
      corresponding deltas for the unselected vertices according to the mesh tension. */
  
  public void adjustDeltas(Vec3 delta[])
  {
  }

  /** This method does nothing, since it is not permitted to modify the mesh topology. */

  public void deleteCommand()
  {
  }
}

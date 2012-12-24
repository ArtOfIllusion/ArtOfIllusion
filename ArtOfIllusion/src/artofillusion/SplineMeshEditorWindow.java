/* Copyright (C) 1999-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;

/** The SplineMeshEditorWindow class represents the window for editing SplineMesh objects. */

public class SplineMeshEditorWindow extends MeshEditorWindow implements EditingWindow
{
  private ToolPalette modes;
  private BMenu editMenu, meshMenu, skeletonMenu;
  private BMenuItem editMenuItem[], meshMenuItem[], skeletonMenuItem[];
  private BCheckBoxMenuItem smoothItem[], closedItem[];
  private Runnable onClose;
  private int selectionDistance[], maxDistance, selectMode, lastSelectedJoint;
  private boolean topology;
  boolean selected[];
  private TextureParameter jointWeightParam;

  public SplineMeshEditorWindow(EditingWindow parent, String title, ObjectInfo obj, Runnable onClose, boolean allowTopology)
  {
    super(parent, title, obj);
    this.onClose = onClose;
    topology = allowTopology;

    FormContainer content = new FormContainer(new double [] {0, 1}, new double [] {1, 0, 0});
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    content.add(helpText = new BLabel(), 0, 1, 2, 1);
    content.add(viewsContainer, 1, 0);
    RowContainer buttons = new RowContainer();
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "doCancel"));
    content.add(buttons, 0, 2, 2, 1, new LayoutInfo());
    FormContainer toolsContainer = new FormContainer(new double [] {1}, new double [] {1, 0});
    toolsContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.BOTH));
    content.add(toolsContainer, 0, 0);
    toolsContainer.add(tools = new ToolPalette(1, 9), 0, 0);
    EditingTool metaTool, altTool, compoundTool;
    tools.addTool(defaultTool = new ReshapeMeshTool(this, this));
    tools.addTool(new ScaleMeshTool(this, this));
    tools.addTool(new RotateMeshTool(this, this, false));
    tools.addTool(new SkewMeshTool(this, this));
    tools.addTool(new TaperMeshTool(this, this));
    tools.addTool(new ThickenMeshTool(this, this));
    tools.addTool(compoundTool = new MoveScaleRotateMeshTool(this, this));
    if (ArtOfIllusion.getPreferences().getUseCompoundMeshTool())
      defaultTool = compoundTool;
    tools.addTool(new SkeletonTool(this, true));
    tools.addTool(metaTool = new MoveViewTool(this));
    tools.addTool(altTool = new RotateViewTool(this));
    tools.setDefaultTool(defaultTool);
    tools.selectTool(defaultTool);
    for (int i = 0; i < theView.length; i++)
    {
      MeshViewer view = (MeshViewer) theView[i];
      view.setMetaTool(metaTool);
      view.setAltTool(altTool);
      view.setScene(parent.getScene(), obj);
      view.setFreehandSelection(lastFreehand);
    }
    toolsContainer.add(modes = new ToolPalette(1, 2), 0, 1);
    modes.addTool(new GenericTool(this, "point", Translate.text("pointSelectionModeTool.tipText")));
    modes.addTool(new GenericTool(this, "curve", Translate.text("curveSelectionModeTool.tipText")));
    setSelectionMode(modes.getSelection());
    createEditMenu();
    createMeshMenu((SplineMesh) obj.getObject());
    createSkeletonMenu((SplineMesh) obj.getObject());
    createViewMenu();
    recursivelyAddListeners(this);
    UIUtilities.applyDefaultFont(content);
    UIUtilities.applyDefaultBackground(content);
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension windowDim = new Dimension((screenBounds.width*3)/4, (screenBounds.height*3)/4);
    setBounds(new Rectangle((screenBounds.width-windowDim.width)/2, (screenBounds.height-windowDim.height)/2, windowDim.width, windowDim.height));
    tools.requestFocus();
    selected = new boolean [((Mesh) objInfo.getObject()).getVertices().length];
    findSelectionDistance();
    addExtraParameters();
    updateMenus();
  }

  void createEditMenu()
  {
    editMenu = Translate.menu("edit");
    menubar.add(editMenu);
    editMenuItem = new BMenuItem [3];
    editMenu.add(undoItem = Translate.menuItem("undo", this, "undoCommand"));
    editMenu.add(redoItem = Translate.menuItem("redo", this, "redoCommand"));
    editMenu.add(Translate.menuItem("selectAll", this, "selectAllCommand"));
    editMenu.add(editMenuItem[0] = Translate.menuItem("extendSelection", this, "extendSelectionCommand"));
    editMenu.add(editMenuItem[1] = Translate.menuItem("invertSelection", this, "invertSelectionCommand"));
    editMenu.add(editMenuItem[2] = Translate.checkboxMenuItem("freehandSelection", this, "freehandModeChanged", false));
    editMenu.addSeparator();
    editMenu.add(Translate.menuItem("meshTension", this, "setTensionCommand"));
  }

  void createMeshMenu(SplineMesh obj)
  {
    BMenu smoothMenu, closedMenu;
    
    meshMenu = Translate.menu("mesh");
    menubar.add(meshMenu);
    meshMenuItem = new BMenuItem [8];
    meshMenuItem[0] = Translate.menuItem("deleteCurves", this, "deleteCommand");
    if (topology)
      meshMenu.add(meshMenuItem[0]);
    meshMenuItem[1] = Translate.menuItem("subdivide", this, "subdivideCommand");
    if (topology)
      meshMenu.add(meshMenuItem[1]);
    meshMenu.add(meshMenuItem[2] = Translate.menuItem("editPoints", this, "setPointsCommand"));
    meshMenu.add(meshMenuItem[3] = Translate.menuItem("transformPoints", this, "transformPointsCommand"));
    meshMenu.add(meshMenuItem[4] = Translate.menuItem("randomize", this, "randomizeCommand"));
    meshMenu.add(meshMenuItem[5] = Translate.menuItem("parameters", this, "setParametersCommand"));
    meshMenu.add(Translate.menuItem("centerMesh", this, "centerCommand"));
    meshMenu.add(meshMenuItem[6] = Translate.menuItem("extractCurve", this, "extractCurveCommand"));
    meshMenu.addSeparator();
    meshMenu.add(meshMenuItem[7] = Translate.menuItem("smoothness", this, "setSmoothnessCommand"));
    meshMenu.add(smoothMenu = Translate.menu("smoothingMethod"));
    smoothItem = new BCheckBoxMenuItem [2];
    smoothMenu.add(smoothItem[0] = Translate.checkboxMenuItem("interpolating", this, "smoothingChanged", obj.getSmoothingMethod() == TriangleMesh.INTERPOLATING));
    smoothMenu.add(smoothItem[1] = Translate.checkboxMenuItem("approximating", this, "smoothingChanged", obj.getSmoothingMethod() == TriangleMesh.APPROXIMATING));
    closedMenu = Translate.menu("closed");
    if (topology)
      meshMenu.add(closedMenu);
    closedItem = new BCheckBoxMenuItem [4];
    closedMenu.add(closedItem[0] = Translate.checkboxMenuItem("udirection", this, "closedTypeChanged", (obj.isUClosed() && !obj.isVClosed())));
    closedMenu.add(closedItem[1] = Translate.checkboxMenuItem("vdirection", this, "closedTypeChanged", (!obj.isUClosed() && obj.isVClosed())));
    closedMenu.add(closedItem[2] = Translate.checkboxMenuItem("both", this, "closedTypeChanged", (obj.isUClosed() && obj.isVClosed())));
    closedMenu.add(closedItem[3] = Translate.checkboxMenuItem("neither", this, "closedTypeChanged", (!obj.isUClosed() && !obj.isVClosed())));
    if (topology)
      meshMenu.add(Translate.menuItem("invertNormals", this, "reverseNormalsCommand"));
  }

  void createSkeletonMenu(SplineMesh obj)
  {
    skeletonMenu = Translate.menu("skeleton");
    menubar.add(skeletonMenu);
    skeletonMenuItem = new BMenuItem [7];
    skeletonMenu.add(skeletonMenuItem[0] = Translate.menuItem("editBone", this, "editJointCommand"));
    skeletonMenu.add(skeletonMenuItem[1] = Translate.menuItem("deleteBone", this, "deleteJointCommand"));
    skeletonMenu.add(skeletonMenuItem[2] = Translate.menuItem("setParentBone", this, "setJointParentCommand"));
    skeletonMenu.add(skeletonMenuItem[3] = Translate.menuItem("importSkeleton", this, "importSkeletonCommand"));
    skeletonMenu.addSeparator();
    skeletonMenu.add(skeletonMenuItem[4] = Translate.menuItem("bindSkeleton", this, "bindSkeletonCommand"));
    skeletonMenu.add(skeletonMenuItem[5] = Translate.menuItem("unbindSkeleton", this, "unbindSkeletonCommand"));
    skeletonMenu.add(skeletonMenuItem[6] = Translate.checkboxMenuItem("detachSkeleton", this, "skeletonDetachedChanged", false));
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
  
  /** When the selection mode changes, do our best to convert the old selection to the new mode. */
  
  public void setSelectionMode(int mode)
  {
    SplineMesh mesh = (SplineMesh) getObject().getObject();
    MeshVertex vert[] = mesh.getVertices();
    int i, j, usize = mesh.getUSize(), vsize = mesh.getVSize();
    boolean newSel[];
    
    if (mode == selectMode)
      return;
    if (mode == POINT_MODE)
    {
      newSel = new boolean [vert.length];
      for (i = 0; i < usize; i++)
        if (selected[i])
          for (j = 0; j < vsize; j++)
            newSel[i+j*usize] = true;
      for (i = 0; i < vsize; i++)
        if (selected[i+usize])
          for (j = 0; j < usize; j++)
            newSel[j+i*usize] = true;
    }
    else if (mode == EDGE_MODE)
    {
      newSel = new boolean [usize+vsize];
      for (i = 0; i < newSel.length; newSel[i++] = true);
      for (i = 0; i < usize; i++)
        for (j = 0; j < vsize; j++)
          if (!selected[i+j*usize])
            newSel[i] = newSel[j+usize] = false;
    }
    else
      return;
    selectMode = mode;
    setSelection(newSel);
    if (modes.getSelection() != mode)
      modes.selectTool(modes.getTool(mode));
  }
  
  public int getSelectionMode()
  {
    return selectMode;
  }
  
  /** Get an array of flags telling which parts of the mesh are currently selected.  Depending
      on the current selection mode, these flags may correspond to vertices or curves. */
  
  public boolean[] getSelection()
  {
    return selected;
  }
  
  public void setSelection(boolean sel[])
  {
    selected = sel;
    findSelectionDistance();
    updateMenus();
    for (ViewerCanvas view : theView)
      view.repaint();
    repaint();
  }

  public int[] getSelectionDistance()
  {
    if (maxDistance != getTensionDistance())
      findSelectionDistance();
    return selectionDistance;
  }

  /** Calculate the distance (in edges) between each vertex and the nearest selected vertex. */

  void findSelectionDistance()
  {
    SplineMesh mesh = (SplineMesh) getObject().getObject();
    int i, j, k, usize = mesh.getUSize(), vsize = mesh.getVSize();
    boolean uclosed = mesh.isUClosed(), vclosed = mesh.isVClosed();
    int dist[] = new int [mesh.getVertices().length];
    
    maxDistance = getTensionDistance();
    
    // First, set each distance to 0 or -1, depending on whether that vertex is part of the
    // current selection.
    
    if (selectMode == POINT_MODE)
      for (i = 0; i < dist.length; i++)
        dist[i] = selected[i] ? 0 : -1;
    else
      {
        for (i = 0; i < usize; i++)
          for (j = 0; j < vsize; j++)
            dist[i+j*usize] = (selected[i] || selected[j+usize]) ? 0 : -1;
      }

    // Now extend this outward up to maxDistance.

    for (i = 0; i < maxDistance; i++)
      for (j = 0; j < usize; j++)
        for (k = 0; k < vsize; k++)
          if (dist[j+k*usize] == -1)
            {
              if (j == 0)
                {
                  if (uclosed && dist[usize-1+k*usize] == i)
                    dist[j+k*usize] = i+1;
                }
              else
                if (dist[j-1+k*usize] == i)
                  dist[j+k*usize] = i+1;
              if (j == usize-1)
                {
                  if (uclosed && dist[k*usize] == i)
                    dist[j+k*usize] = i+1;
                }
              else
                if (dist[j+1+k*usize] == i)
                  dist[j+k*usize] = i+1;
              if (k == 0)
                {
                  if (vclosed && dist[j+(vsize-1)*usize] == i)
                    dist[j+k*usize] = i+1;
                }
              else
                if (dist[j+(k-1)*usize] == i)
                  dist[j+k*usize] = i+1;
              if (k == vsize-1)
                {
                  if (vclosed && dist[j] == i)
                    dist[j+k*usize] = i+1;
                }
              else
                if (dist[j+(k+1)*usize] == i)
                  dist[j+k*usize] = i+1;
            }
    selectionDistance = dist;
  }
  
  public void setMesh(Mesh mesh)
  {
    SplineMesh obj = (SplineMesh) mesh;

    setObject(obj);
    for (int i = 0; i < theView.length; i++)
    {
      if (selectMode == POINT_MODE && selected.length != obj.getVertices().length)
        selected = new boolean [obj.getVertices().length];
      if (selectMode == EDGE_MODE && selected.length != obj.getUSize()+obj.getVSize())
        selected = new boolean [obj.getUSize()+obj.getVSize()];
      ((SplineMeshViewer) theView[i]).visible = new boolean [obj.getVertices().length];
    }
    updateJointWeightParam();
    findSelectionDistance();
    currentTool.getWindow().updateMenus();
  }

  /* EditingWindow methods. */

  public void setTool(EditingTool tool)
  {
    if (tool instanceof GenericTool)
    {
      if (selectMode == modes.getSelection())
        return;
      if (undoItem != null)
        setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(selectMode), selected}));
      setSelectionMode(modes.getSelection());
      theView[currentView].getCurrentTool().activate();
    }
    else
    {
      for (int i = 0; i < theView.length; i++)
        theView[i].setTool(tool);
      currentTool = tool;
    }
  }

  public void updateImage()
  {
    if (lastSelectedJoint != ((MeshViewer) theView[currentView]).getSelectedJoint())
      updateJointWeightParam();
    super.updateImage();
  }

  public void updateMenus()
  {
    super.updateMenus();
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    boolean curvemode = (selectMode == EDGE_MODE);
    MeshViewer view = (MeshViewer) theView[currentView];
    int count = 0;
    
    for (int i = 0; i < selected.length; i++)
      if (selected[i])
        count++;
    if (count > 0)
    {
      editMenuItem[0].setEnabled(true);
      meshMenuItem[0].setEnabled(curvemode);
      meshMenuItem[1].setEnabled(curvemode);
      meshMenuItem[2].setEnabled(true);
      meshMenuItem[3].setEnabled(true);
      meshMenuItem[4].setEnabled(true);
      meshMenuItem[5].setEnabled(true);
      meshMenuItem[6].setEnabled(curvemode && count == 1);
      meshMenuItem[7].setEnabled(curvemode);
    }
    else
    {
      editMenuItem[0].setEnabled(false);
      for (int i = 0; i < meshMenuItem.length; i++)
        meshMenuItem[i].setEnabled(false);
    }
    Skeleton s = theMesh.getSkeleton();
    Joint selJoint = s.getJoint(view.getSelectedJoint());
    skeletonMenuItem[0].setEnabled(selJoint != null);
    skeletonMenuItem[1].setEnabled(selJoint != null && selJoint.children.length == 0);
    skeletonMenuItem[2].setEnabled(selJoint != null);
    skeletonMenuItem[4].setEnabled(count > 0);
    skeletonMenuItem[5].setEnabled(selJoint != null);
  }

  /** Add an extra texture parameter to the mesh which will be used for keeping track of
      joint weights. */

  private void addExtraParameters()
  {
    if (jointWeightParam != null)
      return;
    jointWeightParam = new TextureParameter(this, "Joint Weight", 0.0, 1.0, 0.0);
    SplineMesh mesh = (SplineMesh) getObject().getObject();
    TextureParameter params[] = mesh.getParameters();
    TextureParameter newparams[] = new TextureParameter [params.length+1];
    ParameterValue values[] = mesh.getParameterValues();
    ParameterValue newvalues[] = new ParameterValue [values.length+1];
    for (int i = 0; i < params.length; i++)
    {
      newparams[i] = params[i];
      newvalues[i] = values[i];
    }
    newparams[params.length] = jointWeightParam;
    newvalues[values.length] = new VertexParameterValue(mesh, jointWeightParam);
    mesh.setParameters(newparams);
    mesh.setParameterValues(newvalues);
    getObject().clearCachedMeshes();
    updateJointWeightParam();
  }

  /** Remove the extra texture parameter from the mesh which was used for keeping track of
      joint weights. */

  public void removeExtraParameters()
  {
    if (jointWeightParam == null)
      return;
    jointWeightParam = null;
    SplineMesh mesh = (SplineMesh) getObject().getObject();
    TextureParameter params[] = mesh.getParameters();
    TextureParameter newparams[] = new TextureParameter [params.length-1];
    ParameterValue values[] = mesh.getParameterValues();
    ParameterValue newvalues[] = new ParameterValue [values.length-1];
    for (int i = 0; i < newparams.length; i++)
    {
      newparams[i] = params[i];
      newvalues[i] = values[i];
    }
    mesh.setParameters(newparams);
    mesh.setParameterValues(newvalues);
    getObject().clearCachedMeshes();
  }

  /** Update the parameter which records weights for the currently selected joint. */

  private void updateJointWeightParam()
  {
    MeshVertex vert[] = ((SplineMesh) getObject().getObject()).getVertices();
    double jointWeight[] = new double [vert.length];
    int selJointId = ((MeshViewer) theView[currentView]).getSelectedJoint();
    Joint selJoint = getObject().getSkeleton().getJoint(selJointId);
    for (int i = 0; i < jointWeight.length; i++)
    {
      Joint vertJoint = getObject().getSkeleton().getJoint(vert[i].ikJoint);
      if (selJoint == null)
        jointWeight[i] = 0.0;
      else if (vert[i].ikJoint == selJointId)
        jointWeight[i] = (selJoint.parent == null ? 1.0 : vert[i].ikWeight);
      else if (vertJoint != null && vertJoint.parent == selJoint)
        jointWeight[i] = 1.0-vert[i].ikWeight;
      else
        jointWeight[i] = 0.0;
    }
    VertexParameterValue value = (VertexParameterValue) getObject().getObject().getParameterValue(jointWeightParam);
    value.setValue(jointWeight);
    getObject().getObject().setParameterValues(getObject().getObject().getParameterValues());
    lastSelectedJoint = selJointId;
    objInfo.clearCachedMeshes();
  }

  /** Get the extra texture parameter which was added to the mesh to keep track of
      joint weighting. */

  public TextureParameter getJointWeightParam()
  {
    return jointWeightParam;
  }

  protected void doOk()
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    if (((SplineMesh) oldMesh).getMaterial() != null)
    {
      if (!theMesh.isClosed())
      {
        String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
        BStandardDialog dlg = new BStandardDialog("", UIUtilities.breakString(Translate.text("surfaceNoLongerClosed")), BStandardDialog.WARNING);
        int choice = dlg.showOptionDialog(this, options, options[0]);
        if (choice == 1)
          return;
        theMesh.setMaterial(null, null);
      }
      else
        theMesh.setMaterial(((SplineMesh) oldMesh).getMaterial(), ((SplineMesh) oldMesh).getMaterialMapping());
    }
    removeExtraParameters();
    oldMesh.copyObject(theMesh);
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

  private void freehandModeChanged()
  {
    setFreehand((((BCheckBoxMenuItem) editMenuItem[2]).getState()));
  }
  
  private void smoothingChanged(CommandEvent ev)
  {
    Object source = ev.getWidget();
    for (int i = 0; i < smoothItem.length; i++)
      if (source == smoothItem[i])
        setSmoothingMethod(i+2);
  }
  
  private void closedTypeChanged(CommandEvent ev)
  {
    Object source = ev.getWidget();
    for (int i = 0; i < closedItem.length; i++)
      if (source == closedItem[i])
        setClosed(i);
  }
  
  private void skeletonDetachedChanged()
  {
    for (int i = 0; i < theView.length; i++)
      ((SplineMeshViewer) theView[i]).setSkeletonDetached(((BCheckBoxMenuItem) skeletonMenuItem[6]).getState());
  }

  /** This is overridden to update jointWeightParam after weights are changed. */

  @Override
  public void bindSkeletonCommand()
  {
    super.bindSkeletonCommand();
    updateJointWeightParam();
    updateImage();
  }

  /** This is overridden to update jointWeightParam after weights are changed. */

  @Override
  public void unbindSkeletonCommand()
  {
    super.unbindSkeletonCommand();
    updateJointWeightParam();
    updateImage();
  }

  /** This is overridden to update jointWeightParam after weights are changed. */

  @Override
  public void setPointsCommand()
  {
    super.setPointsCommand();
    updateJointWeightParam();
    updateImage();
  }

  /** Select the entire mesh. */
  
  public void selectAllCommand()
  {
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(selectMode), selected.clone()}));
    for (int i = 0; i < selected.length; i++)
      selected[i] = true;
    setSelection(selected);
  }

  /** Extend the selection outward by one edge. */

  public void extendSelectionCommand()
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(selectMode), selected.clone()}));
    if (selectMode == POINT_MODE)
    {
      int oldDist = tensionDistance;
      tensionDistance = 1;
      int dist[] = getSelectionDistance();
      boolean selected[] = new boolean [dist.length];
      tensionDistance = oldDist;
      for (int i = 0; i < dist.length; i++)
        selected[i] = (dist[i] == 0 || dist[i] == 1);
      setSelection(selected);
    }
    else
    {
      boolean oldSelection[] = selected;
      boolean newSelection[] = new boolean [oldSelection.length];
      int usize = theMesh.getUSize(), vsize = theMesh.getVSize();
      for (int i = 0; i < usize-1; i++)
        if (oldSelection[i] || oldSelection[i+1])
          newSelection[i] = newSelection[i+1] = true;
      if (theMesh.isUClosed() && (oldSelection[0] || oldSelection[usize-1]))
        newSelection[0] = newSelection[usize-1] = true;
      for (int i = 0; i < vsize-1; i++)
        if (oldSelection[usize+i] || oldSelection[usize+i+1])
          newSelection[usize+i] = newSelection[usize+i+1] = true;
      if (theMesh.isVClosed() && (oldSelection[usize] || oldSelection[usize+vsize-1]))
        newSelection[usize] = newSelection[usize+vsize-1] = true;
      setSelection(newSelection);
    }
  }
  
  /** Invert the current selection. */
  
  public void invertSelectionCommand()
  {
    boolean newSel[] = new boolean [selected.length];
    for (int i = 0; i < newSel.length; i++)
      newSel[i] = !selected[i];
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(selectMode), selected}));
    setSelection(newSel);
  }

  /** Delete the current selection. */
  
  public void deleteCommand()
  {
    if (!topology)
      return;
    int i, j, k, m, unum = 0, vnum = 0;
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    float us[] = theMesh.getUSmoothness(), vs[] = theMesh.getVSmoothness(), newus[], newvs[];
    MeshVertex vt[] = theMesh.getVertices(), v[][];
    int usize = theMesh.getUSize(), vsize = theMesh.getVSize();

    if (selectMode != EDGE_MODE)
      return;
    for (i = 0; i < usize; i++)
      if (selected[i])
        unum++;
    for (i = 0; i < vsize; i++)
      if (selected[i+usize])
        vnum++;
    if (unum == 0 && vnum == 0)
      return;
    if (usize-unum < 2 || vsize-vnum < 2)
    {
      new BStandardDialog("", Translate.text("curveNeeds2Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    if ((theMesh.isUClosed() && usize-unum < 3) || (theMesh.isVClosed() && vsize-vnum < 3))
    {
      new BStandardDialog("", Translate.text("curveNeeds3Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    v = new MeshVertex [usize-unum][vsize-vnum];
    newus = new float [usize-unum];
    newvs = new float [vsize-vnum];
    for (i = 0, j = 0; i < usize; i++)
    {
      if (!selected[i])
      {
        for (k = 0, m = 0; k < vsize; k++)
        {
          if (!selected[k+usize])
          {
            newvs[m] = vs[k];
            v[j][m++] = vt[i+k*usize];
          }
        }
        newus[j++] = us[i];
      }
    }
    theMesh.setShape(v, newus, newvs);
    setMesh(theMesh);
    updateImage();
  }
  
  void subdivideCommand()
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    MeshVertex vt[] = theMesh.getVertices();
    float us[] = theMesh.getUSmoothness(), vs[] = theMesh.getVSmoothness(), newus[], newvs[];
    boolean newsel[], splitu[], splitv[];
    MeshVertex v[][], newv[][];
    double param[][][], newparam[][][];
    int i, j, usplitcount = 0, vsplitcount = 0;
    int usize = theMesh.getUSize(), vsize = theMesh.getVSize();
    int numParam = (theMesh.getParameters() == null ? 0 : theMesh.getParameters().length);
    
    if (selectMode != EDGE_MODE)
      return;
    for (i = 0; !selected[i] && i < selected.length; i++);
    if (i == selected.length)
      return;

    // Determine which parts need to be subdivided.
    
    if (theMesh.isUClosed())
      splitu = new boolean [usize];
    else
      splitu = new boolean [usize-1];
    for (i = 0; i < splitu.length; i++)
      if (selected[i] && selected[(i+1)%usize])
      {
        splitu[i] = true;
        usplitcount++;
      }
    if (theMesh.isVClosed())
      splitv = new boolean [vsize];
    else
      splitv = new boolean [vsize-1];
    for (i = 0; i < splitv.length; i++)
      if (selected[i+usize] && selected[(i+1)%vsize+usize])
      {
        splitv[i] = true;
        vsplitcount++;
      }
    newus = new float [usize+usplitcount];
    newvs = new float [vsize+vsplitcount];
    newsel = new boolean [selected.length+usplitcount+vsplitcount];
    
    // Do the subdivision along the u direction.

    v = new MeshVertex [vsize][usize];
    for (i = 0; i < usize; i++)
      for (j = 0; j < vsize; j++)
        v[j][i] = vt[i+j*usize];
    newv = new MeshVertex [vsize][usize+usplitcount];
    param = new double [vsize][usize][numParam];
    for (int k = 0; k < numParam; k++)
      if (theMesh.getParameterValues()[k] instanceof VertexParameterValue)
      {
        double val[] = ((VertexParameterValue) theMesh.getParameterValues()[k]).getValue();
        for (i = 0; i < usize; i++)
          for (j = 0; j < vsize; j++)
            param[j][i][k] = val[i+usize*j];
      }
    newparam = new double [vsize][usize+usplitcount][numParam];
    splitOneAxis(v, newv, us, newus, splitu, param, newparam, theMesh.isUClosed());

    // Do the subdivision along the v direction.

    v = new MeshVertex [usize+usplitcount][vsize];
    for (i = 0; i < v.length; i++)
      for (j = 0; j < v[i].length; j++)
        v[i][j] = newv[j][i];
    newv = new MeshVertex [usize+usplitcount][vsize+vsplitcount];
    param = new double [usize+usplitcount][vsize][numParam];
    for (i = 0; i < param.length; i++)
      for (j = 0; j < param[i].length; j++)
        for (int k = 0; k < param[i][j].length; k++)
          param[i][j][k] = newparam[j][i][k];
    newparam = new double [usize+usplitcount][vsize+vsplitcount][numParam];
    splitOneAxis(v, newv, vs, newvs, splitv, param, newparam, theMesh.isVClosed());
    
    // Determine the new selection, and update the mesh.
    
    for (i = 0, j = 0; i < usize; i++)
    {
      if (selected[i])
        newsel[j] = true;
      if (i < splitu.length && splitu[i])
        newsel[++j] = true;
      j++;
    }
    for (i = 0, j = 0; i < vsize; i++)
    {
      if (selected[i+usize])
        newsel[j+usize+usplitcount] = true;
      if (i < splitv.length && splitv[i])
        newsel[++j+usize+usplitcount] = true;
      j++;
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    theMesh.setShape(newv, newus, newvs);
    for (int k = 0; k < numParam; k++)
      if (theMesh.getParameterValues()[k] instanceof VertexParameterValue)
      {
        double val[] = new double [newus.length*newvs.length];
        for (i = 0; i < newus.length; i++)
          for (j = 0; j < newvs.length; j++)
            val[i+newus.length*j] = newparam[i][j][k];
        theMesh.setParameterValue(theMesh.getParameters()[k], new VertexParameterValue(val));
      }
    setMesh(theMesh);
    setSelection(newsel);
  }

  // Perform the subdivision along one axis of the mesh.

  private void splitOneAxis(MeshVertex v[][], MeshVertex newv[][], float s[], float news[], boolean split[], double param[][][], double newparam[][][], boolean closed)
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    int method = theMesh.getSmoothingMethod();
    int numParam = param[0][0].length;
    double paramTemp[] = new double [numParam];
    int i, j, k, p1, p3, p4;
        
    for (i = 0, j = 0; i < split.length; i++)
    {
      p1 = i-1;
      if (p1 < 0)
      {
        if (closed)
          p1 = v[0].length-1;
        else
          p1 = 0;
      }
      if (i < v[0].length-1)
        p3 = i+1;
      else
      {
        if (closed)
          p3 = 0;
        else
          p3 = v[0].length-1;
      }
      if ((split[i] || split[p1]) && method == Mesh.APPROXIMATING)
        for (k = 0; k < v.length; k++)
        {
          newv[k][j] = SplineMesh.calcApproxPoint(v[k], s, param[k], paramTemp, p1, i, p3);
          for (int m = 0; m < numParam; m++)
            newparam[k][j][m] = paramTemp[m];
        }
      else
        for (k = 0; k < v.length; k++)
        {
          newv[k][j] = v[k][i];
          for (int m = 0; m < numParam; m++)
            newparam[k][j][m] = param[k][i][m];
        }
      if (split[i] || split[p1])
        news[j] = Math.min(s[i]*2.0f, 1.0f);
      else
        news[j] = s[i];
      if (!split[i])
      {
        j++;
        continue;
      }
      if (method == Mesh.NO_SMOOTHING)
        for (k = 0; k < v.length; k++)
        {
          newv[k][j+1] = MeshVertex.blend(v[k][i], v[k][p3], 0.5, 0.5);
          for (int m = 0; m < numParam; m++)
            newparam[k][j+1][m] = 0.5*(param[k][i][m]+param[k][p3][m]);
        }
      else if (method == Mesh.INTERPOLATING)
      {
        if (i < v[0].length-2)
          p4 = i+2;
        else
        {
          if (closed)
            p4 = (i+2)%v[0].length;
          else
            p4 = v[0].length-1;
        }
        for (k = 0; k < v.length; k++)
        {
          newv[k][j+1] = SplineMesh.calcInterpPoint(v[k], s, param[k], paramTemp, p1, i, p3, p4);
          for (int m = 0; m < numParam; m++)
            newparam[k][j+1][m] = paramTemp[m];
        }
      }
      else
        for (k = 0; k < v.length; k++)
        {
          newv[k][j+1] = MeshVertex.blend(v[k][i], v[k][p3], 0.5, 0.5);
          for (int m = 0; m < numParam; m++)
            newparam[k][j+1][m] = 0.5*(param[k][i][m]+param[k][p3][m]);
        }
      news[j+1] = 1.0f;
      j += 2;
    }
    if (!closed)
    {
      for (k = 0; k < v.length; k++)
      {
        newv[k][0] = v[k][0];
        newv[k][j] = v[k][i];
        for (int m = 0; m < numParam; m++)
        {
          newparam[k][0][m] = param[k][0][m];
          newparam[k][j][m] = param[k][i][m];
        }
      }
      news[j] = s[i];
    }
  }

  void extractCurveCommand()
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    MeshVertex vt[] = theMesh.getVertices();
    int usize = theMesh.getUSize(), vsize = theMesh.getVSize();
    int i, which;

    if (selectMode != EDGE_MODE)
      return;
    for (which = 0; which < selected.length && !selected[which]; which++);
    if (which == selected.length)
      return;    
    
    // Now find the sequence of vertices.
    
    boolean closed;
    Vec3 v[];
    float smoothness[];
    if (which < usize)
    {
      closed = theMesh.isVClosed();
      v = new Vec3 [vsize];
    }
    else
    {
      closed = theMesh.isUClosed();
      v = new Vec3 [usize];
    }
    smoothness = new float [v.length];
    float usmoothness[] = theMesh.getUSmoothness();
    float vsmoothness[] = theMesh.getVSmoothness();
    for (i = 0; i < v.length; i++)
    {
      if (which < usize)
      {
        v[i] = vt[which+i*usize].r;
        smoothness[i] = vsmoothness[i];
      }
      else
      {
        v[i] = vt[(which-usize)*usize+i].r;
        smoothness[i] = usmoothness[i];
      }
    }
    Curve cv = new Curve(v, smoothness, theMesh.getSmoothingMethod(), closed);
    Widget parent = parentWindow.getFrame();
    while (parent != null && !(parent instanceof LayoutWindow))
      parent = parent.getParent();
    if (parent != null)
    {
      String name = new BStandardDialog("", Translate.text("extractedCurveName"), BStandardDialog.QUESTION).showInputDialog(this, null, "Extracted Curve");
      if (name != null)
      {
        ((LayoutWindow) parent).addObject(cv, ((MeshViewer) theView[currentView]).thisObjectInScene.getCoords().duplicate(), name, null);
        ((LayoutWindow) parent).updateImage();
      }
    }
  }

  void setSmoothnessCommand()
  {
    final SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    SplineMesh oldMesh = (SplineMesh) theMesh.duplicate();
    final float usmoothness[] = theMesh.getUSmoothness(), vsmoothness[] = theMesh.getVSmoothness();
    float value;
    int i;
    
    if (selectMode != EDGE_MODE)
      return;
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i == selected.length)
      return;
    if (i < theMesh.getUSize())
      value = usmoothness[i];
    else
      value = vsmoothness[i-theMesh.getUSize()];
    value = 0.001f * (Math.round(value*1000.0f));
    final ValueSlider smoothness = new ValueSlider(0.0, 1.0, 100, (double) value);
    smoothness.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        float s = (float) smoothness.getValue();
        for (int i = 0; i < selected.length; i++)
          if (selected[i])
          {
            if (i < theMesh.getUSize())
              usmoothness[i] = s;
            else
              vsmoothness[i-theMesh.getUSize()] = s;
          }
        theMesh.setSmoothness(usmoothness, vsmoothness);
        objectChanged();
        updateImage();
      }
    } );
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("setCurveSmoothness"), 
        new Widget [] {smoothness}, new String [] {Translate.text("Smoothness")});
    if (dlg.clickedOk())
      setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, oldMesh}));
    else
    {
      theMesh.copyObject(oldMesh);
      objectChanged();
      updateImage();
    }
  }

  void reverseNormalsCommand()
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    theMesh.reverseOrientation();
    objectChanged();
    updateImage();
  }

  void setSmoothingMethod(int method)
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    for (int i = 0; i < smoothItem.length; i++)
      smoothItem[i].setState(false);
    smoothItem[method-2].setState(true);
    theMesh.setSmoothingMethod(method);    
    objectChanged();
    updateImage();
  }

  void setClosed(int item)
  {
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    for (int i = 0; i < closedItem.length; i++)
      closedItem[i].setState(i == item);
    theMesh.setClosed((item == 0 || item == 2), (item == 1 || item == 2));
    setMesh(theMesh);
    updateImage();
  }

  /* Given a list of deltas which will be added to the selected vertices, calculate the
     corresponding deltas for the unselected vertices according to the mesh tension. */
  
  public void adjustDeltas(Vec3 delta[])
  {
    int dist[] = getSelectionDistance(), count[] = new int [delta.length];
    SplineMesh theMesh = (SplineMesh) objInfo.getObject();
    int maxDistance = getTensionDistance(), usize = theMesh.getUSize(), vsize = theMesh.getVSize();
    double tension = getMeshTension(), scale[] = new double [maxDistance+1];

    
    for (int i = 0; i < delta.length; i++)
      if (dist[i] != 0)
        delta[i].set(0.0, 0.0, 0.0);
    for (int i = 0; i < maxDistance; i++)
    {
      for (int j = 0; j < count.length; j++)
        count[j] = 0;
      for (int j = 0; j < usize; j++)
        for (int k = 0; k < vsize; k++)
          if (dist[j+k*usize] == i)
          {
            if (j == 0)
            {
              if (theMesh.isUClosed() && dist[usize-1+k*usize] == i+1)
              {
                count[usize-1+k*usize]++;
                delta[usize-1+k*usize].add(delta[j+k*usize]);
              }
            }
            else
              if (dist[j-1+k*usize] == i+1)
              {
                count[j-1+k*usize]++;
                delta[j-1+k*usize].add(delta[j+k*usize]);
              }
            if (j == usize-1)
            {
              if (theMesh.isUClosed() && dist[k*usize] == i+1)
              {
                count[k*usize]++;
                delta[k*usize].add(delta[j+k*usize]);
              }
            }
            else
              if (dist[j+1+k*usize] == i+1)
              {
                count[j+1+k*usize]++;
                delta[j+1+k*usize].add(delta[j+k*usize]);
              }
            if (k == 0)
            {
              if (theMesh.isVClosed() && dist[j+(vsize-1)*usize] == i+1)
              {
                count[j+(vsize-1)*usize]++;
                delta[j+(vsize-1)*usize].add(delta[j+k*usize]);
              }
            }
            else
              if (dist[j+(k-1)*usize] == i+1)
              {
                count[j+(k-1)*usize]++;
                delta[j+(k-1)*usize].add(delta[j+k*usize]);
              }
            if (k == vsize-1)
            {
              if (theMesh.isVClosed() && dist[j] == i+1)
              {
                dist[j+k*usize] = i+1;
                count[j]++;
                delta[j].add(delta[j+k*usize]);
              }
            }
            else
              if (dist[j+(k+1)*usize] == i+1)
              {
                count[j+(k+1)*usize]++;
                delta[j+(k+1)*usize].add(delta[j+k*usize]);
              }
          }
      for (int j = 0; j < count.length; j++)
        if (count[j] > 1)
          delta[j].scale(1.0/count[j]);
    }
    for (int i = 0; i < scale.length; i++)
      scale[i] = Math.pow((maxDistance-i+1.0)/(maxDistance+1.0), tension);
    for (int i = 0; i < delta.length; i++)
      if (dist[i] > 0)
        delta[i].scale(scale[dist[i]]);
  }
}
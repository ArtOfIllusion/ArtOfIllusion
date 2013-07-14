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
import artofillusion.object.TriangleMesh.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;
import java.util.*;

/** The TriMeshEditorWindow class represents the window for editing TriangleMesh objects. */

public class TriMeshEditorWindow extends MeshEditorWindow implements EditingWindow
{
  private ToolPalette modes;
  private BMenuItem editMenuItem[], selectMenuItem[], meshMenuItem[], skeletonMenuItem[];
  private BCheckBoxMenuItem smoothItem[];
  private Runnable onClose;
  private TriangleMesh mesh, divMesh;
  private RenderingMesh lastPreview;
  private boolean topology, projectOntoSurface;
  boolean hideVert[], hideFace[], hideEdge[], selected[], showQuads, tolerant;
  private int selectionDistance[], maxDistance, selectMode, boundary[][], projectedEdge[];
  private int lastSelectedJoint;
  private TextureParameter faceIndexParam, jointWeightParam;

  protected static boolean lastProjectOntoSurface, lastTolerant, lastShowQuads;

  public TriMeshEditorWindow(EditingWindow parent, String title, ObjectInfo obj, Runnable onClose, boolean allowTopology)
  {
    super(parent, title, obj);
    mesh = (TriangleMesh) objInfo.getObject();
    hideVert = new boolean [mesh.getVertices().length];
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
    toolsContainer.add(tools = new ToolPalette(1, allowTopology ? 11 : 9), 0, 0);
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
    if (allowTopology)
    {
      tools.addTool(new BevelExtrudeTool(this, this));
      tools.addTool(new CreateVertexTool(this, this));
    }
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
    tolerant = lastTolerant;
    projectOntoSurface = lastProjectOntoSurface;
    showQuads = lastShowQuads;
    toolsContainer.add(modes = new ToolPalette(1, 3), 0, 1);
    modes.addTool(new GenericTool(this, "point", Translate.text("pointSelectionModeTool.tipText")));
    modes.addTool(new GenericTool(this, "edge", Translate.text("edgeSelectionModeTool.tipText")));
    modes.addTool(new GenericTool(this, "face", Translate.text("faceSelectionModeTool.tipText")));
    setSelectionMode(modes.getSelection());
    createEditMenu((TriangleMesh) obj.getObject());
    createMeshMenu((TriangleMesh) obj.getObject());
    createSkeletonMenu((TriangleMesh) obj.getObject());
    createViewMenu();
    recursivelyAddListeners(this);
    UIUtilities.applyDefaultFont(content);
    UIUtilities.applyDefaultBackground(content);
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension windowDim = new Dimension((screenBounds.width*3)/4, (screenBounds.height*3)/4);
    setBounds(new Rectangle((screenBounds.width-windowDim.width)/2, (screenBounds.height-windowDim.height)/2, windowDim.width, windowDim.height));
    tools.requestFocus();
    selected = new boolean [mesh.getVertices().length];
    findQuads();
    findSelectionDistance();
    addExtraParameters();
    updateMenus();
  }

  void createEditMenu(TriangleMesh obj)
  {
    BMenu editMenu = Translate.menu("edit");
    menubar.add(editMenu);
    editMenuItem = new BMenuItem [7];
    editMenu.add(undoItem = Translate.menuItem("undo", this, "undoCommand"));
    editMenu.add(redoItem = Translate.menuItem("redo", this, "redoCommand"));
    editMenu.addSeparator();
    editMenuItem[0] = Translate.menuItem("clear", this, "deleteCommand");
    if (topology)
    {
      editMenu.add(editMenuItem[0]);
      editMenu.addSeparator();
    }
    editMenu.add(Translate.menuItem("selectAll", this, "selectAllCommand"));
    editMenu.add(editMenuItem[1] = Translate.menuItem("extendSelection", this, "extendSelectionCommand"));
    editMenu.add(Translate.menuItem("invertSelection", this, "invertSelectionCommand"));
    BMenu selectSpecialMenu = Translate.menu("selectSpecial");
    selectMenuItem = new BMenuItem[4];
    selectSpecialMenu.add(selectMenuItem[0] = Translate.menuItem("selectBoundary", this, "selectObjectBoundaryCommand"));
    selectSpecialMenu.add(selectMenuItem[1] = Translate.menuItem("selectSelectionBoundary", this, "selectSelectionBoundaryCommand"));
    selectSpecialMenu.add(selectMenuItem[2] = Translate.menuItem("selectEdgeLoop", this, "selectEdgeLoopCommand"));
    selectSpecialMenu.add(selectMenuItem[3] = Translate.menuItem("selectEdgeStrip", this, "selectEdgeStripCommand"));
    editMenu.add(selectSpecialMenu);
    editMenu.addSeparator();
    editMenu.add(editMenuItem[2] = Translate.checkboxMenuItem("tolerantSelection", this, "tolerantModeChanged", lastTolerant));
    editMenu.add(editMenuItem[3] = Translate.checkboxMenuItem("freehandSelection", this, "freehandModeChanged", lastFreehand));
    editMenu.add(editMenuItem[4] = Translate.checkboxMenuItem("displayAsQuads", this, "quadModeChanged", lastShowQuads));
    editMenu.add(editMenuItem[5] = Translate.checkboxMenuItem("projectOntoSurface", this, "projectModeChanged", lastProjectOntoSurface));
    editMenu.addSeparator();
    editMenu.add(editMenuItem[6] = Translate.menuItem("hideSelection", this, "hideSelectionCommand"));
    editMenu.add(Translate.menuItem("showAll", this, "showAllCommand"));
    editMenu.addSeparator();
    editMenu.add(Translate.menuItem("meshTension", this, "setTensionCommand"));
  }

  void createMeshMenu(TriangleMesh obj)
  {
    BMenu smoothMenu;
    
    BMenu meshMenu = Translate.menu("mesh");
    menubar.add(meshMenu);
    meshMenuItem = new BMenuItem [12];
    meshMenuItem[0] = Translate.menuItem("subdivideEdges", this, "subdivideCommand");
    if (topology)
      meshMenu.add(meshMenuItem[0]);
    meshMenuItem[1] = Translate.menuItem("simplify", this, "simplifyCommand");
    if (topology)
      meshMenu.add(meshMenuItem[1]);
    meshMenu.add(meshMenuItem[2] = Translate.menuItem("editPoints", this, "setPointsCommand"));
    meshMenu.add(meshMenuItem[3] = Translate.menuItem("transformPoints", this, "transformPointsCommand"));
    meshMenu.add(meshMenuItem[4] = Translate.menuItem("randomize", this, "randomizeCommand"));
    meshMenuItem[5] = Translate.menuItem("bevel", this, "bevelCommand");
    if (topology)
      meshMenu.add(meshMenuItem[5]);
    meshMenu.add(meshMenuItem[6] = Translate.menuItem("parameters", this, "setParametersCommand"));
    if (topology)
      meshMenu.add(Translate.menuItem("optimize", this, "optimizeCommand"));
    meshMenu.add(Translate.menuItem("centerMesh", this, "centerCommand"));
    meshMenu.addSeparator();
    meshMenuItem[7] = Translate.menuItem("closeBoundary", this, "closeBoundaryCommand");
    if (topology)
      meshMenu.add(meshMenuItem[7]);
    meshMenuItem[8] = Translate.menuItem("joinBoundaries", this, "joinBoundariesCommand");
    if (topology)
      meshMenu.add(meshMenuItem[8]);
    meshMenu.add(meshMenuItem[9] = Translate.menuItem("extractFaces", this, "extractFacesCommand"));
    meshMenu.add(meshMenuItem[10] = Translate.menuItem("extractCurve", this, "extractCurveCommand"));
    meshMenu.addSeparator();
    meshMenu.add(meshMenuItem[11] = Translate.menuItem("smoothness", this, "setSmoothnessCommand"));
    meshMenu.add(smoothMenu = Translate.menu("smoothingMethod"));
    smoothItem = new BCheckBoxMenuItem [4];
    smoothMenu.add(smoothItem[0] = Translate.checkboxMenuItem("none", this, "smoothingChanged", obj.getSmoothingMethod() == TriangleMesh.NO_SMOOTHING));
    smoothMenu.add(smoothItem[1] = Translate.checkboxMenuItem("shading", this, "smoothingChanged", obj.getSmoothingMethod() == TriangleMesh.SMOOTH_SHADING));
    smoothMenu.add(smoothItem[2] = Translate.checkboxMenuItem("interpolating", this, "smoothingChanged", obj.getSmoothingMethod() == TriangleMesh.INTERPOLATING));
    smoothMenu.add(smoothItem[3] = Translate.checkboxMenuItem("approximating", this, "smoothingChanged", obj.getSmoothingMethod() == TriangleMesh.APPROXIMATING));
    if (topology)
      meshMenu.add(Translate.menuItem("invertNormals", this, "reverseNormalsCommand"));
  }

  void createSkeletonMenu(TriangleMesh obj)
  {
    BMenu skeletonMenu = Translate.menu("skeleton");
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

  /** Load all the preferences into memory. */

  protected void loadPreferences()
  {
    super.loadPreferences();
    lastTolerant = preferences.getBoolean("tolerantSelection", lastTolerant);
    lastShowQuads = preferences.getBoolean("displayAsQuads", lastShowQuads);
    lastProjectOntoSurface = preferences.getBoolean("projectOntoSurface", lastProjectOntoSurface);
  }

  /** Save user settings that should be persistent between sessions. */

  protected void savePreferences()
  {
    super.savePreferences();
    preferences.putBoolean("tolerantSelection", lastTolerant);
    preferences.putBoolean("displayAsQuads", lastShowQuads);
    preferences.putBoolean("projectOntoSurface", lastProjectOntoSurface);
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
    TriangleMesh obj = (TriangleMesh) mesh;
    setObject(obj);
    this.mesh = obj;
    hideVert = new boolean [mesh.getVertices().length];
    for (int i = 0; i < theView.length; i++)
    {
      if (getSelectionMode() == POINT_MODE && selected.length != obj.getVertices().length)
        {
          selected = new boolean [obj.getVertices().length];
          ((TriMeshViewer) theView[i]).visible = new boolean [obj.getVertices().length];
        }
      if (getSelectionMode() == EDGE_MODE && selected.length != obj.getEdges().length)
        {
          selected = new boolean [obj.getEdges().length];
          ((TriMeshViewer) theView[i]).visible = new boolean [obj.getEdges().length];
        }
      if (getSelectionMode() == FACE_MODE && selected.length != obj.getFaces().length)
        {
          selected = new boolean [obj.getFaces().length];
          ((TriMeshViewer) theView[i]).visible = new boolean [obj.getFaces().length];
        }
    }
    if (hideFace != null)
    {
      boolean oldHideFace[] = hideFace;
      FaceParameterValue val = (FaceParameterValue) getObject().getObject().getParameterValue(faceIndexParam);
      double param[] = val.getValue();
      hideFace = new boolean [obj.getFaces().length];
      for (int i = 0; i < param.length; i++)
      {
        int index = (int) param[i];
        if (index < oldHideFace.length)
          hideFace[i] = oldHideFace[index];
      }
    }
    setHiddenFaces(hideFace);
    updateJointWeightParam();
    findSelectionDistance();
    boundary = null;
    currentTool.getWindow().updateMenus();
  }
  
  /** When the object changes, we need to rebuild the quad display. */
  
  public void objectChanged()
  {
    super.objectChanged();
    findQuads();
  }

  /* EditingWindow methods. */

  public void setTool(EditingTool tool)
  {
    if (tool instanceof GenericTool)
    {
      if (selectMode == modes.getSelection())
        return;
      if (undoItem != null)
        setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected}));
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
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    MeshViewer view = (MeshViewer) theView[currentView];
    boolean any = false;
    int i;
    
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i < selected.length)
    {
      any = true;
      editMenuItem[0].setEnabled(true);
      editMenuItem[1].setEnabled(true);
      editMenuItem[6].setEnabled(true);
      for (i = 0; i < 12; i++)
        meshMenuItem[i].setEnabled(true);
      if (selectMode == EDGE_MODE)
      {
        meshMenuItem[0].setText(Translate.text("menu.subdivideEdges"));
        int boundaryList[][] = findSelectedBoundaries();
        meshMenuItem[6].setEnabled(false);
        meshMenuItem[7].setEnabled(boundaryList.length > 0);
        meshMenuItem[8].setEnabled(boundaryList.length == 2);
        meshMenuItem[9].setEnabled(false);
      }
      else if (selectMode == FACE_MODE)
      {
        meshMenuItem[0].setText(Translate.text("menu.subdivideFaces"));
        meshMenuItem[7].setEnabled(false);
        meshMenuItem[8].setEnabled(false);
        meshMenuItem[10].setEnabled(false);
        meshMenuItem[11].setEnabled(false);
      }
      else
      {
        meshMenuItem[0].setEnabled(false);
        meshMenuItem[7].setEnabled(false);
        meshMenuItem[8].setEnabled(false);
        meshMenuItem[9].setEnabled(false);
        meshMenuItem[10].setEnabled(false);
      }
      meshMenuItem[1].setText(Translate.text("menu.simplify"));
    }
    else    
    {
      editMenuItem[0].setEnabled(false);
      editMenuItem[1].setEnabled(false);
      editMenuItem[6].setEnabled(false);
      meshMenuItem[0].setEnabled(false);
      for (i = 2; i < 12; i++)
        meshMenuItem[i].setEnabled(false);
      meshMenuItem[1].setText(Translate.text("menu.simplifyMesh"));
    }
    editMenuItem[5].setEnabled(theMesh.getSmoothingMethod() == TriangleMesh.APPROXIMATING || theMesh.getSmoothingMethod() == TriangleMesh.INTERPOLATING);
    selectMenuItem[0].setEnabled(!objInfo.getObject().isClosed());
    selectMenuItem[1].setEnabled(any);
    selectMenuItem[2].setEnabled(any && selectMode == EDGE_MODE);
    selectMenuItem[3].setEnabled(any && selectMode == EDGE_MODE);
    Skeleton s = theMesh.getSkeleton();
    Joint selJoint = s.getJoint(view.getSelectedJoint());
    skeletonMenuItem[0].setEnabled(selJoint != null);
    skeletonMenuItem[1].setEnabled(selJoint != null && selJoint.children.length == 0);
    skeletonMenuItem[2].setEnabled(selJoint != null);
    skeletonMenuItem[4].setEnabled(any);
    skeletonMenuItem[5].setEnabled(selJoint != null);
  }

  /** Get which faces are hidden.  This may be null, which means that all faces are visible. */
  
  public boolean [] getHiddenFaces()
  {
    return hideFace;
  }
  
  /** Set which faces are hidden.  Pass null to show all faces. */
  
  public void setHiddenFaces(boolean hidden[])
  {
    hideFace = hidden;
    hideVert = new boolean [mesh.getVertices().length];
    if (hideFace != null)
    {
      for (int i = 0; i < hideVert.length; i++)
        hideVert[i] = true;
      TriangleMesh.Face face[] = mesh.getFaces();
      for (int i = 0; i < face.length; i++)
        if (!hideFace[i])
          hideVert[face[i].v1] = hideVert[face[i].v2] = hideVert[face[i].v3] = false;
    }
    else
    {
      for (int i = 0; i < hideVert.length; i++)
        hideVert[i] = false;
    }
    FaceParameterValue val = (FaceParameterValue) objInfo.getObject().getParameterValue(faceIndexParam);
    double param[] = val.getValue();
    for (int i = 0; i < param.length; i++)
      param[i] = i;
    val.setValue(param);
    objInfo.getObject().setParameterValue(faceIndexParam, val);
    objInfo.clearCachedMeshes();
    findQuads();
    updateImage();
  }
  
  /** Add extra texture parameters to the mesh which will be used for keeping track of face
      and vertex indices. */
  
  private void addExtraParameters()
  {
    if (faceIndexParam != null)
      return;
    faceIndexParam = new TextureParameter(this, "Face Index", 0.0, Double.MAX_VALUE, 0.0);
    jointWeightParam = new TextureParameter(this, "Joint Weight", 0.0, 1.0, 0.0);
    TriangleMesh mesh = (TriangleMesh) getObject().getObject();
    TextureParameter params[] = mesh.getParameters();
    TextureParameter newparams[] = new TextureParameter [params.length+2];
    ParameterValue values[] = mesh.getParameterValues();
    ParameterValue newvalues[] = new ParameterValue [values.length+2];
    for (int i = 0; i < params.length; i++)
    {
      newparams[i] = params[i];
      newvalues[i] = values[i];
    }
    newparams[params.length] = faceIndexParam;
    newvalues[values.length] = new FaceParameterValue(mesh, faceIndexParam);
    double faceIndex[] = new double [mesh.getFaces().length];
    for (int i = 0; i < faceIndex.length; i++)
      faceIndex[i] = i;
    ((FaceParameterValue) newvalues[values.length]).setValue(faceIndex);
    newparams[params.length+1] = jointWeightParam;
    newvalues[values.length+1] = new VertexParameterValue(mesh, jointWeightParam);
    mesh.setParameters(newparams);
    mesh.setParameterValues(newvalues);
    getObject().clearCachedMeshes();
    updateJointWeightParam();
  }

  /** Remove the extra texture parameters from the mesh which were used for keeping track of
      face and vertex indices. */
  
  public void removeExtraParameters()
  {
    if (faceIndexParam == null)
      return;
    faceIndexParam = null;
    jointWeightParam = null;
    TriangleMesh mesh = (TriangleMesh) getObject().getObject();
    TextureParameter params[] = mesh.getParameters();
    TextureParameter newparams[] = new TextureParameter [params.length-2];
    ParameterValue values[] = mesh.getParameterValues();
    ParameterValue newvalues[] = new ParameterValue [values.length-2];
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
    MeshVertex vert[] = mesh.getVertices();
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
      face indices in the editor. */

  public TextureParameter getFaceIndexParameter()
  {
    return faceIndexParam;
  }

  /** Get the extra texture parameter which was added to the mesh to keep track of
      joint weighting. */

  public TextureParameter getJointWeightParam()
  {
    return jointWeightParam;
  }

  /** Get whether the control mesh is displayed projected onto the surface. */

  public boolean getProjectOntoSurface()
  {
    return projectOntoSurface;
  }

  /** Set whether the control mesh is displayed projected onto the surface. */

  public void setProjectOntoSurface(boolean project)
  {
    lastProjectOntoSurface = projectOntoSurface = project;
    savePreferences();
  }

  /** Determine which edge of the control mesh corresponds to each edge of the subdivided mesh.  If the control
      mesh is not being projected onto the surface, this returns null. */

  int [] findProjectedEdges()
  {
    // See if we actually want to project the control mesh.

    if (!getProjectOntoSurface() || (mesh.getSmoothingMethod() != TriangleMesh.APPROXIMATING && mesh.getSmoothingMethod() != TriangleMesh.INTERPOLATING))
    {
      lastPreview = null;
      return null;
    }

    // See whether we need to rebuild to list of projected edges.

    RenderingMesh preview = getObject().getPreviewMesh();
    if (preview == lastPreview)
      return projectedEdge; // The mesh hasn't changed.
    lastPreview = preview;

    // We need an actual TriangleMesh, not just a RenderingMesh.

    divMesh = mesh.convertToTriangleMesh(ArtOfIllusion.getPreferences().getInteractiveSurfaceError());
    Edge divEdge[] = divMesh.getEdges();
    double param[] = ((FaceParameterValue) divMesh.getParameterValue(getFaceIndexParameter())).getValue();
    projectedEdge = new int [divEdge.length];

    // Loop over the edges and try to figure out each one.

    Edge e[] = mesh.getEdges();
    Face face[] = mesh.getFaces();
    int specialEdge[] = null, numSpecial = 0;
    for (int i = 0; i < divEdge.length; i++)
    {
      projectedEdge[i] = -1;
      int f1 = (int) param[divEdge[i].f1];
      int f2 = (divEdge[i].f2 == -1 ? -1 : (int) param[divEdge[i].f2]);
      if (f1 == f2)
        continue; // This edge doesn't correspond to an edge of the control mesh.
      if (f2 != -1)
      {
        // This edge corresponds to an internal edge of the control mesh.

        projectedEdge[i] = face[f1].getSharedFace(face[f2]);
        continue;
      }

      // This edge corresponds to a boundary edge of the control mesh.  We need to figure out
      // which one.  In most cases, its face will only have one boundary edge, so it's easy
      // to tell.

      int boundaryCount = 0;
      Face f = face[f1];
      if (e[f.e1].f2 == -1)
      {
        projectedEdge[i] = f.e1;
        boundaryCount++;
      }
      if (e[f.e2].f2 == -1)
      {
        projectedEdge[i] = f.e2;
        boundaryCount++;
      }
      if (e[f.e3].f2 == -1)
      {
        projectedEdge[i] = f.e3;
        boundaryCount++;
      }
      if (boundaryCount == 1)
        continue; // The face only has one boundary edge.

      // The face has more than one boundary edge, so it takes some work to figure out which one this is.
      // For the moment, simply record it.

      if (specialEdge == null)
        specialEdge = new int [divEdge.length];
      specialEdge[numSpecial++] = i;
      projectedEdge[i] = -1;
    }
    if (numSpecial > 0)
    {
      // There were some boundary edges that couldn't be determined by either of the easy methods.  We need
      // to trace along them to figure out which vertices they connect.

      int numOriginalVert = mesh.getVertices().length;
      MeshVertex divVert[] = divMesh.getVertices();
      for (int i = 0; i < numSpecial; i++)
      {
        if (projectedEdge[specialEdge[i]] > -1)
          continue;

        // Find one of the two original vertices.

        Edge thisEdge = divEdge[specialEdge[i]];
        int v1, currentVert;
        if (thisEdge.v1 < numOriginalVert)
        {
          v1 = thisEdge.v1;
          currentVert = thisEdge.v2;
        }
        else if (thisEdge.v2 < numOriginalVert)
        {
          v1 = thisEdge.v2;
          currentVert = thisEdge.v1;
        }
        else
          continue;

        // Now trace along the edges to find the other one.

        ArrayList<Integer> sequential = new ArrayList<Integer>();
        int currentEdge = specialEdge[i];
        int v2 = -1;
        while (true)
        {
          sequential.add(currentEdge);
          if (thisEdge.v1 != v1 && thisEdge.v1 < numOriginalVert)
          {
            v2 = thisEdge.v1;
            break;
          }
          else if (thisEdge.v2 != v1 && thisEdge.v2 < numOriginalVert)
          {
            v2 = thisEdge.v2;
            break;
          }

          // Find the next edge.

          int vertEdges[] = ((Vertex) divVert[currentVert]).getEdges();
          if (vertEdges[0] != currentEdge)
            currentEdge = vertEdges[0];
          else
            currentEdge = vertEdges[vertEdges.length-1];
          thisEdge = divEdge[currentEdge];
          currentVert = (thisEdge.v1 == currentVert ? thisEdge.v2 : thisEdge.v1);
        }

        // Find the edge of the original mesh that connects these two vertices.

        int vertEdges[] = ((Vertex) mesh.getVertices()[v1]).getEdges();
        Edge first = e[vertEdges[0]];
        int originalEdge = (first.v1 == v2 || first.v2 == v2 ? vertEdges[0] : vertEdges[vertEdges.length-1]);

        // Record this for all of the subdivided edges.

        for (int j = 0; j < sequential.size(); j++)
          projectedEdge[sequential.get(j)] = originalEdge;
      }
    }
    return projectedEdge;
  }

  /** Get the subdivided mesh which represents the surface.  If the control mesh is not being projected
      onto the surface, this returns null. */

  TriangleMesh getSubdividedMesh()
  {
    return divMesh;
  }

  protected void doOk()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    if (((TriangleMesh) oldMesh).getMaterial() != null)
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
        theMesh.setMaterial(((TriangleMesh) oldMesh).getMaterial(), ((TriangleMesh) oldMesh).getMaterialMapping());
    }
    removeExtraParameters();
    oldMesh.copyObject(theMesh);
    oldMesh = null;
    dispose();
    if (onClose != null)
      onClose.run();
    parentWindow.updateImage();
    parentWindow.updateMenus();
  }
  
  /** Determine whether we are in tolerant selection mode. */
  
  public boolean isTolerant()
  {
    return tolerant;
  }
  
  /** Set whether to use tolerant selection mode. */
  
  public void setTolerant(boolean tol)
  {
    lastTolerant = tolerant = tol;
    savePreferences();
  }
  
  /** Determine whether the mesh is being displayed as quads. */
  
  public boolean isQuadMode()
  {
    return showQuads;
  }
  
  /** Set whether to display the mesh as quads. */
  
  public void setQuadMode(boolean quads)
  {
    lastShowQuads = showQuads = quads;
    findQuads();
    findSelectionDistance();
    savePreferences();
  }
  
  /** Determine whether a particular edge is hidden to simulate a quad. */
  
  public boolean isEdgeHidden(int which)
  {
    return hideEdge[which];
  }
  
  /** Find edges which should be hidden to make the object seem to be made of quads. */
  
  private void findQuads()
  {
    TriangleMesh mesh = (TriangleMesh) getObject().getObject();
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    if (hideEdge == null || hideEdge.length != e.length)
      hideEdge = new boolean [e.length];
    if (hideFace == null)
      for (int i = 0; i < e.length; i++)
        hideEdge[i] = false;
    else
      for (int i = 0; i < e.length; i++)
        hideEdge[i] = (hideFace[e[i].f1] && (e[i].f2 == -1 || hideFace[e[i].f2]));
    if (!showQuads)
      return;
    
    // An edge is a candidate for hiding if the two faces it borders are in the same plane.
    
    boolean candidate[] = new boolean [e.length];
    Vec3 norm[] = new Vec3 [f.length];
    for (int i = 0; i < f.length; i++)
      {
        Face fc = f[i];
        norm[i] = v[fc.v2].r.minus(v[fc.v1].r).cross(v[fc.v3].r.minus(v[fc.v1].r));
        double length = norm[i].length();
        if (length > 0.0)
          norm[i].scale(1.0/length);
      }
    for (int i = 0; i < e.length; i++)
      candidate[i] = (e[i].f2 != -1 && norm[e[i].f1].dot(norm[e[i].f2]) > 0.99);
    
    // Give every candidate edge a score for how close the adjoining faces are to forming
    // a rectangle.
    
    class EdgeScore implements Comparable
      {
        public int edge;
        public double score;
        
        public EdgeScore(int edge, double score)
        {
          this.edge = edge;
          this.score = score;
        }
        
        public int compareTo(Object o)
        {
          double diff = score-((EdgeScore) o).score;
          if (diff < 0.0)
            return -1;
          if (diff > 0.0)
            return 1;
          return 0;
        }
      }
    Vector<EdgeScore> scoreVec = new Vector<EdgeScore>(e.length);
    Vec3 temp0 = new Vec3(), temp1 = new Vec3(), temp2 = new Vec3();
    for (int i = 0; i < e.length; i++)
      {
        if (!candidate[i])
          continue;
        
        // Find the four vertices.
        
        Edge ed = e[i];
        int v1 = ed.v1, v2 = ed.v2, v3, v4;
        Face fc = f[ed.f1];
        if (fc.v1 != v1 && fc.v1 != v2)
          v3 = fc.v1;
        else if (fc.v2 != v1 && fc.v2 != v2)
          v3 = fc.v2;
        else
          v3 = fc.v3;
        fc = f[ed.f2];
        if (fc.v1 != v1 && fc.v1 != v2)
          v4 = fc.v1;
        else if (fc.v2 != v1 && fc.v2 != v2)
          v4 = fc.v2;
        else
          v4 = fc.v3;
        
        // Find the angles formed by them.
        
        temp0.set(v[v1].r.minus(v[v2].r));
        temp0.normalize();
        temp1.set(v[v1].r.minus(v[v3].r));
        temp1.normalize();
        temp2.set(v[v1].r.minus(v[v4].r));
        temp2.normalize();
        if (Math.acos(temp0.dot(temp1))+Math.acos(temp0.dot(temp2)) > Math.PI)
          continue;
        double dot = temp1.dot(temp2);
        double score = (dot > 0.0 ? dot : -dot);
        temp1.set(v[v2].r.minus(v[v3].r));
        temp1.normalize();
        temp2.set(v[v2].r.minus(v[v4].r));
        temp2.normalize();
        if (Math.acos(-temp0.dot(temp1))+Math.acos(-temp0.dot(temp2)) > Math.PI)
          continue;
        dot = temp1.dot(temp2);
        score += (dot > 0.0 ? dot : -dot);
        scoreVec.addElement(new EdgeScore(i, score));
      }
    if (scoreVec.size() == 0)
      return;
    
    // Sort them.
    
    EdgeScore score[] = new EdgeScore [scoreVec.size()];
    scoreVec.copyInto(score);
    Arrays.sort(score);
    
    // Mark which edges to hide.
    
    boolean hasHiddenEdge[] = new boolean [f.length];
    for (int i = 0; i < score.length; i++)
      {
        Edge ed = e[score[i].edge];
        if (hasHiddenEdge[ed.f1] || hasHiddenEdge[ed.f2])
          continue;
        hideEdge[score[i].edge] = true;
        hasHiddenEdge[ed.f1] = hasHiddenEdge[ed.f2] = true;
      }
  }

  /** When the selection mode changes, do our best to convert the old selection to the 
      new mode. */
  
  public void setSelectionMode(int mode)
  {
    TriangleMesh mesh = (TriangleMesh) getObject().getObject();
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    boolean newSel[];
    int i;
    
    if (mode == selectMode)
      return;
    if (mode == POINT_MODE)
    {
      newSel = new boolean [v.length];
      if (selectMode == EDGE_MODE)
      {
        for (i = 0; i < e.length; i++)
          if (selected[i])
            newSel[e[i].v1] = newSel[e[i].v2] = true;
      }
      else
      {
        for (i = 0; i < f.length; i++)
          if (selected[i])
            newSel[f[i].v1] = newSel[f[i].v2] = newSel[f[i].v3] = true;
      }
    }
    else if (mode == EDGE_MODE)
    {
      newSel = new boolean [e.length];
      if (selectMode == POINT_MODE)
      {
        if (tolerant)
          for (i = 0; i < e.length; i++)
            newSel[i] = (!hideEdge[i] && (selected[e[i].v1] || selected[e[i].v2]));
        else
          for (i = 0; i < e.length; i++)
            newSel[i] = (!hideEdge[i] && (selected[e[i].v1] && selected[e[i].v2]));
      }
      else
      {
        for (i = 0; i < f.length; i++)
          if (selected[i])
            newSel[f[i].e1] = newSel[f[i].e2] = newSel[f[i].e3] = true;
      }
    }
    else
    {
      newSel = new boolean [f.length];
      if (selectMode == POINT_MODE)
      {
        if (tolerant)
          for (i = 0; i < f.length; i++)
            newSel[i] = (selected[f[i].v1] || selected[f[i].v2] || selected[f[i].v3]);
        else
          for (i = 0; i < f.length; i++)
            newSel[i] = (selected[f[i].v1] && selected[f[i].v2] && selected[f[i].v3]);
      }
      else
      {
        for (i = 0; i < f.length; i++)
          newSel[i] = (selected[f[i].e1] && selected[f[i].e2] && selected[f[i].e3]);
      }
    }
    selectMode = mode;
    setSelection(newSel);
    if (modes.getSelection() != mode)
      modes.selectTool(modes.getTool(mode));
  }
  
  public int getSelectionMode()
  {
    return selectMode;
  }
  
  public void setSelection(boolean sel[])
  {
    selected = sel;
    findSelectionDistance();
    updateMenus();
    for (ViewerCanvas view : theView)
      view.repaint();
  }
  
  /** Get an array of flags telling which parts of the mesh are currently selected.  Depending
      on the current selection mode, these flags may correspond to vertices, edges, or faces. */
  
  public boolean[] getSelection()
  {
    return selected;
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
    int i, j;
    TriangleMesh mesh = (TriangleMesh) getObject().getObject();
    int dist[] = new int [mesh.getVertices().length];
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    
    maxDistance = getTensionDistance();
    
    // First, set each distance to 0 or -1, depending on whether that vertex is part of the
    // current selection.
    
    if (selectMode == POINT_MODE)
      for (i = 0; i < dist.length; i++)
        dist[i] = selected[i] ? 0 : -1;
    else if (selectMode == EDGE_MODE)
      {
        for (i = 0; i < dist.length; i++)
          dist[i] = -1;
        for (i = 0; i < selected.length; i++)
          if (selected[i])
            dist[e[i].v1] = dist[e[i].v2] = 0;
      }
    else
      {
        for (i = 0; i < dist.length; i++)
          dist[i] = -1;
        for (i = 0; i < selected.length; i++)
          if (selected[i])
            dist[f[i].v1] = dist[f[i].v2] = dist[f[i].v3] = 0;
      }

    // Now extend this outward up to maxDistance.

    for (i = 0; i < maxDistance; i++)
      for (j = 0; j < e.length; j++)
        {
          if (hideEdge[j])
            continue;
          if (dist[e[j].v1] == -1 && dist[e[j].v2] == i)
            dist[e[j].v1] = i+1;
          else if (dist[e[j].v2] == -1 && dist[e[j].v1] == i)
            dist[e[j].v2] = i+1;
        }
    selectionDistance = dist;
  }
  
  protected void doCancel()
  {
    oldMesh = null;
    dispose();
  }
  
  private void tolerantModeChanged()
  {
    setTolerant(((BCheckBoxMenuItem) editMenuItem[2]).getState());
  }

  private void freehandModeChanged()
  {
    setFreehand(((BCheckBoxMenuItem) editMenuItem[3]).getState());
  }
  
  private void quadModeChanged()
  {
    setQuadMode(((BCheckBoxMenuItem) editMenuItem[4]).getState());
    updateImage();
  }

  private void projectModeChanged()
  {
    setProjectOntoSurface(((BCheckBoxMenuItem) editMenuItem[5]).getState());
    updateImage();
  }

  private void smoothingChanged(CommandEvent ev)
  {
    Object source = ev.getWidget();
    for (int i = 0; i < smoothItem.length; i++)
      if (source == smoothItem[i])
        setSmoothingMethod(i);
  }

  private void skeletonDetachedChanged()
  {
    for (int i = 0; i < theView.length; i++)
      ((TriMeshViewer) theView[i]).setSkeletonDetached(((BCheckBoxMenuItem) skeletonMenuItem[6]).getState());
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

  public void setPointsCommand()
  {
    super.setPointsCommand();
    updateJointWeightParam();
    updateImage();
  }

  /** Select the entire mesh. */
  
  public void selectAllCommand()
  {
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected.clone()}));
    for (int i = 0; i < selected.length; i++)
      selected[i] = true;
    if (selectMode == EDGE_MODE)
      for (int i = 0; i < selected.length; i++)
        if (isEdgeHidden(i))
          selected[i] = false;
    setSelection(selected);
  }
  
  /** Hide the selected part of the mesh. */
  
  public void hideSelectionCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    boolean hide[] = new boolean [theMesh.getFaces().length];
    if (selectMode == FACE_MODE)
      System.arraycopy(selected, 0, hide, 0, selected.length);
    else if (selectMode == EDGE_MODE)
    {
      TriangleMesh.Edge edge[] = theMesh.getEdges();
      for (int i = 0; i < selected.length; i++)
        if (selected[i])
          hide[edge[i].f1] = hide[edge[i].f2] = true;
    }
    else
    {
      TriangleMesh.Face face[] = theMesh.getFaces();
      for (int i = 0; i < face.length; i++)
        hide[i] = (selected[face[i].v1] || selected[face[i].v2] || selected[face[i].v3]);
    }
    boolean wasHidden[] = hideFace;
    if (wasHidden != null)
      for (int i = 0; i < wasHidden.length; i++)
        if (wasHidden[i])
          hide[i] = true;
    setHiddenFaces(hide);
    for (int i = 0; i < selected.length; i++)
      selected[i] = false;
    setSelection(selected);
  }
  
  /** Show all faces of the mesh. */
  
  public void showAllCommand()
  {
    setHiddenFaces(null);
  }
  
  /** Select the edges which form the boundary of the mesh. */
  
  public void selectObjectBoundaryCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    TriangleMesh.Edge edge[] = theMesh.getEdges();
    boolean newSel[] = new boolean [edge.length];
    
    for (int i = 0; i < newSel.length; i++)
      newSel[i] = (edge[i].f2 == -1);
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected}));
    setSelectionMode(EDGE_MODE);
    setSelection(newSel);
  }
  
  /** Select the edges which form the boundary of the current selection. */
  
  public void selectSelectionBoundaryCommand()
  {
    boolean newSel[] = TriMeshSelectionUtilities.findSelectionBoundary(mesh, selectMode, selected);
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected}));
    setSelectionMode(EDGE_MODE);
    setSelection(newSel);
  }
  
  /** Invert the current selection. */
  
  public void invertSelectionCommand()
  {
    boolean newSel[] = new boolean [selected.length];
    for (int i = 0; i < newSel.length; i++)
      newSel[i] = !selected[i];
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected}));
    setSelection(newSel);
  }
  
  /** Select an edge loop from every edge which is currently selected. */
  
  public void selectEdgeLoopCommand()
  {
    boolean newSel[] = TriMeshSelectionUtilities.findEdgeLoops(mesh, selected);
    if (newSel == null)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("cannotFindEdgeLoop")), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected}));
    setSelection(newSel);
  }
  
  /** Select an edge strip from every edge which is currently selected. */
  
  public void selectEdgeStripCommand()
  {
    boolean newSel[] = TriMeshSelectionUtilities.findEdgeStrips(mesh, selected);
    if (newSel == null)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("cannotFindEdgeStrip")), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected}));
    setSelection(newSel);
  }

  /** Extend the selection outward by one edge. */

  public void extendSelectionCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    int dist[] = getSelectionDistance();
    boolean selectedVert[] = new boolean [dist.length];
    TriangleMesh.Edge edge[] = theMesh.getEdges();
    
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected.clone()}));
    for (int i = 0; i < edge.length; i++)
      if ((dist[edge[i].v1] == 0 || dist[edge[i].v2] == 0) && !isEdgeHidden(i))
        selectedVert[edge[i].v1] = selectedVert[edge[i].v2] = true;
    if (selectMode == POINT_MODE)
      setSelection(selectedVert);
    else if (selectMode == EDGE_MODE)
    {
      for (int i = 0; i < edge.length; i++)
        selected[i] = (selectedVert[edge[i].v1] && selectedVert[edge[i].v2]);
      setSelection(selected);
    }
    else
    {
      TriangleMesh.Face face[] = theMesh.getFaces();
      for (int i = 0; i < face.length; i++)
        selected[i] = (selectedVert[face[i].v1] && selectedVert[face[i].v2] && selectedVert[face[i].v3]);
      setSelection(selected);
    }
  }
  
  /** Delete the selected points, edges, or faces from the mesh. */
  
  public void deleteCommand()
  {
    if (!topology)
      return;
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    Vertex vert[] = (Vertex []) theMesh.getVertices();
    Edge edge[] = theMesh.getEdges();
    Face face[] = theMesh.getFaces();
    boolean deleteVert[] = new boolean [vert.length];
    boolean deleteFace[] = new boolean [face.length];

    // Determine which parts of the mesh to delete.
    
    if (selectMode == POINT_MODE)
    {
      for (int i = 0; i < deleteVert.length; i++)
        deleteVert[i] = selected[i];
      for (int i = 0; i < deleteFace.length; i++)
        deleteFace[i] = (deleteVert[face[i].v1] || deleteVert[face[i].v2] || deleteVert[face[i].v3]);
    }
    else if (selectMode == EDGE_MODE)
    {
      for (int i = 0; i < deleteFace.length; i++)
        deleteFace[i] = (selected[face[i].e1] || selected[face[i].e2] || selected[face[i].e3]);
      for (int i = 0; i < deleteVert.length; i++)
        deleteVert[i] = true;
      for (int i = 0; i < deleteFace.length; i++)
        if (!deleteFace[i])
          deleteVert[face[i].v1] = deleteVert[face[i].v2] = deleteVert[face[i].v3] = false;
    }
    else
    {
      for (int i = 0; i < deleteFace.length; i++)
        deleteFace[i] = selected[i];
      for (int i = 0; i < deleteVert.length; i++)
        deleteVert[i] = true;
      for (int i = 0; i < deleteFace.length; i++)
        if (!deleteFace[i])
          deleteVert[face[i].v1] = deleteVert[face[i].v2] = deleteVert[face[i].v3] = false;
    }
    
    // Make sure this will still be a valid object.
    
    for (int i = 0; i < vert.length; i++)
    {
      int e[] = vert[i].getEdges();
      int f, fprev = edge[e[0]].f1, breaks = 0;
      for (int j = 1; j < e.length; j++)
      {
        f = (edge[e[j]].f1 == fprev ? edge[e[j]].f2 : edge[e[j]].f1);
        if (f == -1)
          break;
        if (!deleteFace[fprev] && deleteFace[f])
          breaks++;
        fprev = f;
      }
      if (!deleteFace[fprev] && (edge[e[0]].f2 == -1 || deleteFace[edge[e[0]].f1]))
        breaks++;
      int vertFaceCount[] = new int [vert.length];
      for (int j = 0; j < face.length; j++)
        if (!deleteFace[j])
        {
          vertFaceCount[face[j].v1]++;
          vertFaceCount[face[j].v2]++;
          vertFaceCount[face[j].v3]++;
        }
      boolean strayVert = false;
      for (int j = 0; j < vertFaceCount.length; j++)
        if (!deleteVert[j] && vertFaceCount[j] == 0)
          strayVert = true;
      if (breaks > 1 || strayVert)
      {
        new BStandardDialog("", UIUtilities.breakString(Translate.text("illegalDelete")), BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
    }
    
    // Find the new lists of vertices and faces.
    
    int newVertCount = 0, newFaceCount = 0;
    int newVertIndex[] = new int [vert.length];
    for (int i = 0; i < deleteVert.length; i++)
    {
      newVertIndex[i] = -1;
      if (!deleteVert[i])
        newVertCount++;
    }
    for (int i = 0; i < deleteFace.length; i++)
      if (!deleteFace[i])
        newFaceCount++;
    Vertex v[] = new Vertex [newVertCount];
    int f[][] = new int [newFaceCount][];
    newVertCount = 0;
    for (int i = 0; i < vert.length; i++)
      if (!deleteVert[i])
      {
        newVertIndex[i] = newVertCount;
        v[newVertCount++] = vert[i];
      }
    newFaceCount = 0;
    for (int i = 0; i < face.length; i++)
      if (!deleteFace[i])
        f[newFaceCount++] = new int [] {newVertIndex[face[i].v1], newVertIndex[face[i].v2], newVertIndex[face[i].v3]};

    // Update the texture parameters.
    
    ParameterValue oldParamVal[] = theMesh.getParameterValues();
    ParameterValue newParamVal[] = new ParameterValue [oldParamVal.length];
    for (int i = 0; i < oldParamVal.length; i++)
    {
      if (oldParamVal[i] instanceof VertexParameterValue)
      {
        double oldval[] = ((VertexParameterValue) oldParamVal[i]).getValue();
        double newval[] = new double [newVertCount];
        for (int j = 0, k = 0; j < oldval.length; j++)
          if (!deleteVert[j])
            newval[k++] = oldval[j];
        newParamVal[i] = new VertexParameterValue(newval);
      }
      else if (oldParamVal[i] instanceof FaceParameterValue)
      {
        double oldval[] = ((FaceParameterValue) oldParamVal[i]).getValue();
        double newval[] = new double [newFaceCount];
        for (int j = 0, k = 0; j < oldval.length; j++)
          if (!deleteFace[j])
            newval[k++] = oldval[j];
        newParamVal[i] = new FaceParameterValue(newval);
      }
      else if (oldParamVal[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) oldParamVal[i];
        double newval[][] = new double [newFaceCount][3];
        for (int j = 0, k = 0; j < fvpv.getFaceCount(); j++)
          if (!deleteFace[j])
          {
            newval[k][0] = fvpv.getValue(j, 0);
            newval[k][1] = fvpv.getValue(j, 1);
            newval[k][2] = fvpv.getValue(j, 2);
            k++;
          }
        newParamVal[i] = new FaceVertexParameterValue(newval);
      }
      else
        newParamVal[i] = oldParamVal[i].duplicate();
    }
    
    // Construct the new mesh.
    
    TriangleMesh newmesh = new TriangleMesh(v, f);
    Edge newedge[] = newmesh.getEdges();
    newmesh.getSkeleton().copy(theMesh.getSkeleton());
    newmesh.copyTextureAndMaterial(theMesh);
    newmesh.setSmoothingMethod(theMesh.getSmoothingMethod());
    newmesh.setParameterValues(newParamVal);
    
    // Copy over the smoothness values for edges.
    
    for (int i = 0; i < edge.length; i++)
    {
      int r1 = newVertIndex[edge[i].v1];
      int r2 = newVertIndex[edge[i].v2];
      for (int j = 0; j < newedge.length; j++)
        if ((r1 == newedge[j].v1 && r2 == newedge[j].v2) || (r1 == newedge[j].v2 && r2 == newedge[j].v1))
          newedge[j].smoothness = edge[i].smoothness;
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {newmesh, theMesh}));
    setMesh(newmesh);
    updateImage();
  }
  
  /** Subdivide selected edges or faces of the mesh. */
  
  public void subdivideCommand()
  {
    int i, j;
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject(), newmesh;
    boolean newselection[];
    Edge edges[];
    Face faces[];
        
    if (selectMode != EDGE_MODE && selectMode != FACE_MODE)
      return;
    for (i = 0; !selected[i] && i < selected.length; i++);
    if (i == selected.length)
      return;
    
    if (selectMode == EDGE_MODE)
    {
      // Subdivide selected edges, using the appropriate method.

      i = theMesh.getVertices().length;
      if (theMesh.getSmoothingMethod() == TriangleMesh.APPROXIMATING)
        newmesh = TriangleMesh.subdivideLoop(theMesh, selected, Double.MAX_VALUE);
      else if (theMesh.getSmoothingMethod() == TriangleMesh.INTERPOLATING)
        newmesh = TriangleMesh.subdivideButterfly(theMesh, selected, Double.MAX_VALUE);
      else
        newmesh = TriangleMesh.subdivideLinear(theMesh, selected);
      setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {newmesh, theMesh}));
      setMesh(newmesh);

      // Update the selection.

      edges = newmesh.getEdges();
      newselection = new boolean [edges.length];
      for (j = 0; j < edges.length; j++)
        newselection[j] = (edges[j].v1 >= i || edges[j].v2 >= i);
      setSelection(newselection);
    }
    else
    {
      // Subdivide selected faces.

      i = theMesh.getVertices().length;
      newmesh = TriangleMesh.subdivideFaces(theMesh, selected);
      setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {newmesh, theMesh}));
      setMesh(newmesh);

      // Update the selection.

      faces = newmesh.getFaces();
      newselection = new boolean [faces.length];
      for (j = 0; j < faces.length; j++)
        newselection[j] = (faces[j].v1 >= i || faces[j].v2 >= i || faces[j].v3 >= i);
      setSelection(newselection);
    }
  }
  
  public void simplifyCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    boolean selection[] = selected;
    ValueField errorField = new ValueField(0.01, ValueField.NONNEGATIVE);
    int i;

    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("simplifyMeshTitle"),
            new Widget[] {errorField}, new String[] {Translate.text("maxSurfaceError")});
    if (!dlg.clickedOk())
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));

    // If we are not in Edge selection mode, convert the selection to edges.

    if (selectMode == POINT_MODE)
    {
      Edge e[] = theMesh.getEdges();
      boolean newSel[] = new boolean [e.length];
      for (i = 0; i < e.length; i++)
        newSel[i] = (selection[e[i].v1] && selection[e[i].v2]);
      selection = newSel;
    }
    if (selectMode == FACE_MODE)
    {
      Edge e[] = theMesh.getEdges();
      boolean newSel[] = new boolean [e.length];
      for (i = 0; i < e.length; i++)
        newSel[i] = (selection[e[i].f1] || (e[i].f2 > -1 && selection[e[i].f2]));
      selection = newSel;
    }
    
    // If no edges are selected, then simplify the entire mesh.

    for (i = 0; i < selection.length && !selection[i]; i++);
    if (i == selection.length)
    {
      selection = new boolean [selection.length];
      for (i = 0; i < selection.length; i++)
        selection[i] = true;
    }
    
    // Generate the simplified mesh.
    
    new TriMeshSimplifier(theMesh, selection, errorField.getValue(), this);
    setMesh(theMesh);
    updateImage();
  }
  
  public void optimizeCommand()
  {
    BStandardDialog dlg = new BStandardDialog("", UIUtilities.breakString(Translate.text("optimizeMeshTitle")), BStandardDialog.QUESTION);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    if (dlg.showOptionDialog(this, options, options[0]) == 1)
      return;
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    theMesh.copyObject(TriangleMesh.optimizeMesh(theMesh));
    setMesh(theMesh);
    for (int i = 0; i < selected.length; i++)
      selected[i] = false;
    setSelection(selected);
    updateImage();
  }

  public void bevelCommand()
  {
    final TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    final ValueSelector heightField = new ValueSelector(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01);
    final ValueSelector widthField = new ValueSelector(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01);
    final ObjectPreviewCanvas preview = new ObjectPreviewCanvas(new ObjectInfo(theMesh, new CoordinateSystem(), ""));
    final int bevelMode[];
    if (selectMode == FACE_MODE)
      bevelMode = new int [] {TriMeshBeveler.BEVEL_FACE_GROUPS, TriMeshBeveler.BEVEL_FACES};
    else if (selectMode == POINT_MODE)
      bevelMode = new int [] {TriMeshBeveler.BEVEL_VERTICES, TriMeshBeveler.BEVEL_VERTICES};
    else
      bevelMode = new int [] {TriMeshBeveler.BEVEL_EDGES, TriMeshBeveler.BEVEL_EDGES};
    final BComboBox applyChoice = new BComboBox(new String [] {
      Translate.text("selectionAsWhole"),
      Translate.text("individualFaces")
    });
    applyChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        TriMeshBeveler beveler = new TriMeshBeveler(theMesh, selected, bevelMode[applyChoice.getSelectedIndex()]);
        double height = heightField.getValue();
        double width = widthField.getValue();
        preview.setObject(beveler.bevelMesh(height, width));
        preview.repaint();
      }
    });
    Object listener = new Object() {
      void processEvent()
      {
        TriMeshBeveler beveler = new TriMeshBeveler(theMesh, selected, bevelMode[applyChoice.getSelectedIndex()]);
        double height = heightField.getValue();
        double width = widthField.getValue();
        preview.setObject(beveler.bevelMesh(height, width));
        preview.repaint();
      }
    };
    heightField.addEventLink(ValueChangedEvent.class, listener);
    widthField.addEventLink(ValueChangedEvent.class, listener);
    preview.setPreferredSize(new Dimension(200, 200));
    ComponentsDialog dlg;
    if (selectMode == FACE_MODE)
      dlg = new ComponentsDialog(this, Translate.text("bevelFacesTitle"),
          new Widget[] {heightField, widthField, applyChoice, preview}, 
          new String [] {Translate.text("extrudeHeight"), Translate.text("bevelWidth"), Translate.text("applyTo"), ""});
    else if (selectMode == POINT_MODE)
      dlg = new ComponentsDialog(this, Translate.text("bevelPointsTitle"),
          new Widget[] {heightField, widthField, preview}, 
          new String [] {Translate.text("extrudeHeight"), Translate.text("bevelWidth"), ""});
    else
      dlg = new ComponentsDialog(this, Translate.text("bevelEdgesTitle"),
          new Widget[] {heightField, widthField, preview}, 
          new String [] {Translate.text("extrudeHeight"), Translate.text("bevelWidth"), ""});
    if (!dlg.clickedOk())
      return;
    double height = heightField.getValue();
    double width = widthField.getValue();
    UndoRecord undo = new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()});
    undo.addCommand(UndoRecord.SET_MESH_SELECTION, new Object [] {this, selectMode, selected});
    setUndoRecord(undo);

    // Generate the new mesh.
    
    TriMeshBeveler beveler = new TriMeshBeveler(theMesh, selected, bevelMode[applyChoice.getSelectedIndex()]);
    theMesh.copyObject(beveler.bevelMesh(height, width));
    setMesh(theMesh);
    setSelection(beveler.getNewSelection());
  }

  /**
   * Given the ordered list of edges for a selected boundary segment, determine whether they form
   * a closed boundary.
   */
  
  private boolean isBoundaryClosed(int edges[])
  {
    if (edges.length < 3)
      return false;
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    Edge ed[] = theMesh.getEdges();
    Edge first = ed[edges[0]];
    Edge last = ed[edges[edges.length-1]];
    return (first.v1 == last.v1 || first.v1 == last.v2 || first.v2 == last.v1 || first.v2 == last.v2);
  }

  /** Get arrays of the indices of all edges which form boundaries that have been selected. */
  
  private int [][] findSelectedBoundaries()
  {
    if (getSelectionMode() != EDGE_MODE)
      return new int [0][0];
    if (boundary == null)
      boundary = ((TriangleMesh) getObject().getObject()).findBoundaryEdges();
    Vector<int[]> all = new Vector<int[]>();
    for (int i = 0; i < boundary.length; i++)
    {
      // Add one "selected boundary" for every continuous run of selected edges.
      
      int start;
      for (start = boundary[i].length-1; start > 0 && selected[boundary[i][start]]; start--);
      Vector<Integer> current = null;
      int j = start;
      do
      {
        boolean isSelected = selected[boundary[i][j]];
        if (isSelected)
        {
          if (current == null)
            current = new Vector<Integer>();
          current.addElement(boundary[i][j]);
        }
        if (++j == boundary[i].length)
          j = 0;
        if ((!isSelected || j == start) && current != null)
        {
          int edgeList[] = new int [current.size()];
          for (int k = 0; k < edgeList.length; k++)
            edgeList[k] = current.elementAt(k);
          all.addElement(edgeList);
          current = null;
        }
      } while (j != start);
    }
    int index[][] = new int [all.size()][];
    all.copyInto(index);
    return index;
  }
  
  public void closeBoundaryCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    Vertex vt[] = (Vertex []) theMesh.getVertices();
    Edge ed[] = theMesh.getEdges();
    Face fc[] = theMesh.getFaces();
    int boundaryList[][] = findSelectedBoundaries();  
    Vec3 newvert[] = new Vec3 [vt.length+boundaryList.length];
    int newface[][], count = 0;
    
    // First, add all of the current vertices and faces to the lists.
    
    for (int i = 0; i < vt.length; i++)
      newvert[i] = vt[i].r;
    for (int i = 0; i < boundaryList.length; i++)
      count += boundaryList[i].length;
    newface = new int [fc.length+count][];
    for (int i = 0; i < fc.length; i++)
      newface[i] = new int [] {fc[i].v1, fc[i].v2, fc[i].v3};

    // For each boundary, add new faces to close it.

    int faceIndex = fc.length;
    int vertIndex = vt.length;
    for (int i = 0; i < boundaryList.length; i++)
    {
      if (boundaryList[i].length < 2)
        continue;
      Edge ed0 = ed[boundaryList[i][0]];
      Edge ed1 = ed[boundaryList[i][1]];
      boolean closed = isBoundaryClosed(boundaryList[i]);
      if (boundaryList[i].length == 2 || (boundaryList[i].length == 3 && closed))
      {
        // Add a single new face spanning the two edges.

        Face f = fc[ed0.f1];
        int thirdVert = (ed0.v1 == ed1.v1 || ed0.v2 == ed1.v1 ? ed1.v2 : ed1.v1);
        if ((f.v1 == ed0.v1 && f.v2 == ed0.v2) || (f.v2 == ed0.v1 && f.v3 == ed0.v2) || (f.v3 == ed0.v1 && f.v1 == ed0.v2))
          newface[faceIndex++] = new int [] {ed0.v2, ed0.v1, thirdVert};
        else
          newface[faceIndex++] = new int [] {ed0.v1, ed0.v2, thirdVert};
      }
      else
      {
        // Add a single vertex in the center, and faces surrounding it.

        Vec3 center = new Vec3();
        int j = (ed0.v1 == ed1.v1 || ed0.v1 == ed1.v2 ? ed0.v2 : ed0.v1);
        for (int k = 0; k < boundaryList[i].length; k++)
        {
          center.add(vt[j].r);
          Edge e = ed[boundaryList[i][k]];
          j = (e.v1 == j ? e.v2 : e.v1);
        }
        if (closed)
          center.scale(1.0/boundaryList[i].length);
        else
        {
          center.add(vt[j].r);
          center.scale(1.0/(boundaryList[i].length+1));
        }
        newvert[vertIndex] = center;
        for (int k = 0; k < boundaryList[i].length; k++)
        {
          Edge e = ed[boundaryList[i][k]];
          Face f = fc[e.f1];
          if ((f.v1 == e.v1 && f.v2 == e.v2) || (f.v2 == e.v1 && f.v3 == e.v2) || (f.v3 == e.v1 && f.v1 == e.v2))
            newface[faceIndex++] = new int [] {e.v2, e.v1, vertIndex};
          else
            newface[faceIndex++] = new int [] {e.v1, e.v2, vertIndex};
        }
        vertIndex++;
      }
    }

    // Remove empty elements from the end of the arrays.

    if (faceIndex < newface.length)
    {
      int newface2[][] = new int[faceIndex][];
      System.arraycopy(newface, 0, newface2, 0, faceIndex);
      newface = newface2;
    }
    if (vertIndex < newvert.length)
    {
      Vec3 newvert2[] = new Vec3[vertIndex];
      System.arraycopy(newvert, 0, newvert2, 0, vertIndex);
      newvert = newvert2;
    }
    
    // Create the new mesh.
    
    TriangleMesh newmesh = new TriangleMesh(newvert, newface);
    Vertex newvt[] = (Vertex []) newmesh.getVertices();
    Edge newed[] = newmesh.getEdges();
    newmesh.copyTextureAndMaterial(theMesh);
    newmesh.setSmoothingMethod(theMesh.getSmoothingMethod());

    // Update the texture parameters.
    
    TextureParameter param[] = theMesh.getParameters();
    ParameterValue oldParamVal[] = theMesh.getParameterValues();
    ParameterValue newParamVal[] = new ParameterValue [oldParamVal.length];
    for (int i = 0; i < oldParamVal.length; i++)
    {
      if (oldParamVal[i] instanceof VertexParameterValue)
      {
        double oldval[] = ((VertexParameterValue) oldParamVal[i]).getValue();
        double newval[] = new double [newvert.length];
        for (int j = 0; j < oldval.length; j++)
          newval[j] = oldval[j];
        for (int j = oldval.length; j < newval.length; j++)
          newval[j] = param[i].defaultVal;
        newParamVal[i] = new VertexParameterValue(newval);
      }
      else if (oldParamVal[i] instanceof FaceParameterValue)
      {
        double oldval[] = ((FaceParameterValue) oldParamVal[i]).getValue();
        double newval[] = new double [newface.length];
        for (int j = 0; j < oldval.length; j++)
          newval[j] = oldval[j];
        for (int j = oldval.length; j < newval.length; j++)
          newval[j] = param[i].defaultVal;
        newParamVal[i] = new FaceParameterValue(newval);
      }
      else if (oldParamVal[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) oldParamVal[i];
        double newval[][] = new double [newface.length][3];
        for (int j = 0; j < 3; j++)
        {
          for (int k = 0; k < fvpv.getFaceCount(); k++)
            newval[k][j] = fvpv.getValue(k, j);
          for (int k = fvpv.getFaceCount(); k < newface.length; k++)
            newval[k][j] = param[i].defaultVal;
        }
        newParamVal[i] = new FaceVertexParameterValue(newval);
      }
      else
        newParamVal[i] = oldParamVal[i].duplicate();
    }
    newmesh.setParameterValues(newParamVal);
    
    // Copy over the smoothness values.
    
    for (int i = 0; i < vt.length; i++)
      newvt[i].smoothness = vt[i].smoothness;
    for (int i = 0; i < newed.length; i++)
    {
      if (newed[i].v1 >= vt.length || newed[i].v2 >= vt.length)
        continue;
      for (int j = 0; j < ed.length; j++)
        if ((newed[i].v1 == ed[j].v1 && newed[i].v2 == ed[j].v2) || (newed[i].v1 == ed[j].v2 && newed[i].v2 == ed[j].v1))
          newed[i].smoothness = ed[j].smoothness;
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {newmesh, theMesh}));
    setMesh(newmesh);
    updateImage();
  }

  public void joinBoundariesCommand()
  {
    final int boundaryList[][] = findSelectedBoundaries();  
    if (boundaryList.length != 2)
      return;
    final int boundaryVert[][] = new int [2][];
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    Vertex vt[] = (Vertex []) theMesh.getVertices();
    Edge ed[] = theMesh.getEdges();
    boolean closed = isBoundaryClosed(boundaryList[0]);
    if (closed != isBoundaryClosed(boundaryList[1]))
    {
      new BStandardDialog("", Translate.text("cannotJoinOpenAndClosed"), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }

    // Construct a list of the vertices on each boundary;

    for (int j = 0; j < 2; j++)
    {
      boundaryVert[j] = new int [boundaryList[j].length + (closed ? 0 : 1)];
      if (boundaryList[j].length == 1 || ed[boundaryList[j][0]].v1 == ed[boundaryList[j][1]].v1 || ed[boundaryList[j][0]].v1 == ed[boundaryList[j][1]].v2)
        boundaryVert[j][0] = ed[boundaryList[j][0]].v2;
      else
        boundaryVert[j][0] = ed[boundaryList[j][0]].v1;
      for (int i = 0; i < boundaryVert[j].length-1; i++)
      {
        Edge e = ed[boundaryList[j][i]];
        boundaryVert[j][i+1] = (e.v1 == boundaryVert[j][i] ? e.v2 : e.v1);
      }
    }

    // Consider all possible ways of joining the two boundaries, and select the one which
    // gives the lowest mean squared distance between connected points as the initial guess.
    
    double offset = 0.0;
    boolean reverse = false;
    int maxsteps = (boundaryList[0].length > boundaryList[1].length ? boundaryList[0].length : boundaryList[1].length);
    if (closed)
    {
      double step0 = boundaryList[0].length/((double) maxsteps);
      double step1 = (reverse ? -1.0 : 1.0)*boundaryList[1].length/((double) maxsteps);
      double mindist = Double.MAX_VALUE;
      for (int i = 0; i < maxsteps-1; i++)
      {
        double dist = 0.0;
        for (int j = 0; j < maxsteps; j++)
        {
          double p0 = j*step0, p1 = (i+j)*step1;
          int i0 = ((int) Math.round(p0)+boundaryList[0].length) % boundaryList[0].length;
          int i1 = ((int) Math.round(p1)+boundaryList[1].length) % boundaryList[1].length;
          dist += vt[boundaryVert[0][i0]].r.distance2(vt[boundaryVert[1][i1]].r);
        }
        if (dist < mindist)
        {
          mindist = dist;
          offset = i*step1;
          reverse = false;
        }
        dist = 0.0;
        for (int j = 0; j < maxsteps; j++)
        {
          double p0 = j*step0, p1 = (i-j)*step1;
          int i0 = ((int) Math.round(p0)+boundaryList[0].length) % boundaryList[0].length;
          int i1 = ((int) Math.round(p1)+boundaryList[1].length) % boundaryList[1].length;
          dist += vt[boundaryVert[0][i0]].r.distance2(vt[boundaryVert[1][i1]].r);
        }
        if (dist < mindist)
        {
          mindist = dist;
          offset = i*step1;
          reverse = true;
        }
      }
    }
    else
    {
      double fdist = 0.0, rdist = 0.0;
      double step0 = boundaryList[0].length/((double) maxsteps);
      double step1 = boundaryList[1].length/((double) maxsteps);
      int revStart = boundaryVert[1].length-1;
      for (int i = 0; i < maxsteps; i++)
      {
        int i0 = (int) Math.round(i*step0);
        int i1 = (int) Math.round(i*step1);
        fdist += vt[boundaryVert[0][i0]].r.distance2(vt[boundaryVert[1][i1]].r);
        rdist += vt[boundaryVert[0][i0]].r.distance2(vt[boundaryVert[1][revStart-i1]].r);
      }
      reverse = (fdist > rdist);
    }
    
    // Create a dialog allowing the user to adjust the parameters.
    
    final ValueSlider offsetSlider = new ValueSlider(-0.5*boundaryList[1].length, 0.5*boundaryList[1].length, 2*maxsteps, 0.0);
    final BCheckBox reverseBox = new BCheckBox(Translate.text("reverseDirection"), false);
    final ObjectPreviewCanvas preview = new ObjectPreviewCanvas(new ObjectInfo(doJoinBoundaries(boundaryList, 
      boundaryVert, offset, reverse), new CoordinateSystem(), ""));
    final double baseOffset = offset;
    final boolean baseReverse = reverse;
    FormContainer content = new FormContainer(new double [] {1.0}, new double [] {0.0, 0.0, 1.0});
    if (closed)
    {
      RowContainer row = new RowContainer();
      row.add(new BLabel(Translate.text("Offset")+":"));
      row.add(offsetSlider);
      content.add(row, 0, 0);
    }
    content.add(reverseBox, 0, 1);
    content.add(preview, 0, 2, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    preview.setPreferredSize(new Dimension(200, 200));
    preview.setRenderMode(ViewerCanvas.RENDER_FLAT);
    Object listener = new Object() {
      void processEvent()
      {
        preview.setObject(doJoinBoundaries(boundaryList, boundaryVert, baseOffset+offsetSlider.getValue(), baseReverse^reverseBox.getState()));
        preview.repaint();
      }
    };
    offsetSlider.addEventLink(ValueChangedEvent.class, listener);
    reverseBox.addEventLink(ValueChangedEvent.class, listener);
    PanelDialog dlg = new PanelDialog(this, Translate.text("joinBoundardiesTitle"), content);
    if (!dlg.clickedOk())
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {preview.getObject().getObject(), theMesh}));
    setMesh((Mesh) preview.getObject().getObject());
    updateImage();
  }

  /* This method does the actual work of joining together two boundary curves. */
  
  TriangleMesh doJoinBoundaries(int boundary[][], int boundaryVert[][], double offset, boolean reverse)
  {
    int maxsteps = (boundary[0].length > boundary[1].length ? boundary[0].length : boundary[1].length);
    double step0 = boundary[0].length/((double) maxsteps);
    double step1 = (reverse ? -1.0 : 1.0)*boundary[1].length/((double) maxsteps);
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    Vertex vt[] = (Vertex []) theMesh.getVertices();
    Edge ed[] = theMesh.getEdges();
    Face fc[] = theMesh.getFaces();
    Vec3 newvert[] = new Vec3 [vt.length];
    int newface[][] = new int [fc.length+boundary[0].length+boundary[1].length][];

    // First copy over the old vertices and faces.
    
    for (int i = 0; i < vt.length; i++)
      newvert[i] = vt[i].r;
    for (int i = 0; i < fc.length; i++)
      newface[i] = new int [] {fc[i].v1, fc[i].v2, fc[i].v3};
    int count = fc.length;
    
    // Go around the boundaries and add new faces.
    
    if (isBoundaryClosed(boundary[0]))
    {
      // We are connecting two closed boundaries.
      
      double p0 = 0.0, p1 = offset;
      int i0prev = 0, i1prev = ((int) Math.round(p1)+boundary[1].length) % boundary[1].length;
      for (int i = 1; i <= maxsteps; i++)
      {
        p0 += step0;
        p1 += step1;
        while (p1 < 0.0)
          p1 += boundary[1].length;
        int i0 = ((int) Math.round(p0)) % boundary[0].length;
        int i1 = ((int) Math.round(p1)) % boundary[1].length;
        if (i0 != i0prev)
        {
          Edge e = ed[boundary[0][step0 > 0.0 ? i0prev : i0]];
          Face f = fc[e.f1];
          int v1 = boundaryVert[0][i0prev], v2 = boundaryVert[0][i0];
          if ((f.v1 == v1 && f.v2 == v2) || (f.v2 == v1 && f.v3 == v2) || (f.v3 == v1 && f.v1 == v2))
            newface[count++] = new int [] {v2, v1, boundaryVert[1][i1prev]};
          else
            newface[count++] = new int [] {v1, v2, boundaryVert[1][i1prev]};
        }
        if (i1 != i1prev)
        {
          Edge e = ed[boundary[1][step1 > 0.0 ? i1prev : i1]];
          Face f = fc[e.f1];
          int v1 = boundaryVert[1][i1prev], v2 = boundaryVert[1][i1];
          if ((f.v1 == v1 && f.v2 == v2) || (f.v2 == v1 && f.v3 == v2) || (f.v3 == v1 && f.v1 == v2))
            newface[count++] = new int [] {v2, v1, boundaryVert[0][i0]};
          else
            newface[count++] = new int [] {v1, v2, boundaryVert[0][i0]};
        }
        i0prev = i0;
        i1prev = i1;
      }
    }
    else
    {
      // We are connecting two open boundaries.
      
      double p0 = 0.0, p1 = (reverse ? boundary[1].length : 0.0);
      int i0prev = 0, i1prev = (int) Math.round(p1);
      while (count < newface.length)
      {
        p0 += step0;
        p1 += step1;
        int i0 = (int) Math.round(p0);
        int i1 = (int) Math.round(p1);
        if (i0 < 0)
          i0 = 0;
        if (i1 < 0)
          i1 = 0;
        if (i0 > boundary[0].length)
          i0 = boundary[0].length;
        if (i1 > boundary[1].length)
          i1 = boundary[1].length;
        if (i0 != i0prev)
        {
          Edge e = ed[boundary[0][i0prev < i0 ? i0prev : i0]];
          Face f = fc[e.f1];
          int v1 = boundaryVert[0][i0prev], v2 = boundaryVert[0][i0];
          if ((f.v1 == v1 && f.v2 == v2) || (f.v2 == v1 && f.v3 == v2) || (f.v3 == v1 && f.v1 == v2))
            newface[count++] = new int [] {v2, v1, boundaryVert[1][i1prev]};
          else
            newface[count++] = new int [] {v1, v2, boundaryVert[1][i1prev]};
        }
        if (i1 != i1prev)
        {
          Edge e = ed[boundary[1][i1prev < i1 ? i1prev : i1]];
          Face f = fc[e.f1];
          int v1 = boundaryVert[1][i1prev], v2 = boundaryVert[1][i1];
          if ((f.v1 == v1 && f.v2 == v2) || (f.v2 == v1 && f.v3 == v2) || (f.v3 == v1 && f.v1 == v2))
            newface[count++] = new int [] {v2, v1, boundaryVert[0][i0]};
          else
            newface[count++] = new int [] {v1, v2, boundaryVert[0][i0]};
        }
        i0prev = i0;
        i1prev = i1;
      }
    }
    
    // Create the new mesh.
    
    TriangleMesh newmesh = new TriangleMesh(newvert, newface);
    Vertex newvt[] = (Vertex []) newmesh.getVertices();
    Edge newed[] = newmesh.getEdges();
    newmesh.copyTextureAndMaterial(theMesh);
    newmesh.setSmoothingMethod(theMesh.getSmoothingMethod());

    // Update the texture parameters.
    
    TextureParameter param[] = theMesh.getParameters();
    ParameterValue oldParamVal[] = theMesh.getParameterValues();
    ParameterValue newParamVal[] = new ParameterValue [oldParamVal.length];
    for (int i = 0; i < oldParamVal.length; i++)
    {
      if (oldParamVal[i] instanceof FaceParameterValue)
      {
        double oldval[] = ((FaceParameterValue) oldParamVal[i]).getValue();
        double newval[] = new double [newface.length];
        for (int j = 0; j < oldval.length; j++)
          newval[j] = oldval[j];
        if (param[i] == getFaceIndexParameter()) // The parameter added by the editor window to record face indices
          for (int j = oldval.length; j < newval.length; j++)
            newval[j] = j;
        else
          for (int j = oldval.length; j < newval.length; j++)
            newval[j] = param[i].defaultVal;
        newParamVal[i] = new FaceParameterValue(newval);
      }
      else if (oldParamVal[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) oldParamVal[i];
        double newval[][] = new double [newface.length][3];
        for (int j = 0; j < 3; j++)
        {
          for (int k = 0; k < fvpv.getFaceCount(); k++)
            newval[k][j] = fvpv.getValue(k, j);
          for (int k = fvpv.getFaceCount(); k < newface.length; k++)
            newval[k][j] = param[i].defaultVal;
        }
        newParamVal[i] = new FaceVertexParameterValue(newval);
      }
      else
        newParamVal[i] = oldParamVal[i].duplicate();
    }
    newmesh.setParameterValues(newParamVal);
    
    // Copy over the smoothness values.
    
    for (int i = 0; i < vt.length; i++)
      newvt[i].smoothness = vt[i].smoothness;
    for (int i = 0; i < newed.length; i++)
    {
      if (newed[i].v1 >= vt.length || newed[i].v2 >= vt.length)
        continue;
      for (int j = 0; j < ed.length; j++)
        if ((newed[i].v1 == ed[j].v1 && newed[i].v2 == ed[j].v2) || (newed[i].v1 == ed[j].v2 && newed[i].v2 == ed[j].v1))
          newed[i].smoothness = ed[j].smoothness;
    }
    return newmesh;
  }

  public void extractFacesCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    Vertex vt[] = (Vertex []) theMesh.getVertices();
    Face fc[] = theMesh.getFaces();
    Vector<Integer> faces = new Vector<Integer>();
    TreeSet<Integer> vertices = new TreeSet<Integer>();

    if (selectMode != FACE_MODE)
      return;
    
    // Find the selected faces and make a new mesh out of them.
    
    for (int i = 0; i < selected.length; i++)
      if (selected[i])
        faces.addElement(i);
    if (faces.size() == 0)
      return;
    for (Integer face : faces)
    {
      vertices.add(fc[face].v1);
      vertices.add(fc[face].v2);
      vertices.add(fc[face].v3);
    }
    int vertIndex[] = new int[vt.length];
    Arrays.fill(vertIndex, -1);
    Vec3 v[] = new Vec3[vertices.size()];
    int count = 0;
    for (Integer vertex : vertices)
    {
      v[count] = new Vec3(vt[vertex].r);
      vertIndex[vertex] = count++;
    }
    int f[][] = new int[faces.size()][];
    count = 0;
    for (Integer face : faces)
      f[count++] = new int[] {vertIndex[fc[face].v1], vertIndex[fc[face].v2], vertIndex[fc[face].v3]};
    TriangleMesh mesh = new TriangleMesh(v, f);
    mesh.copyTextureAndMaterial(theMesh);
    mesh.setSmoothingMethod(theMesh.getSmoothingMethod());

    // Verify that it is a valid mesh.

    int vertexEdgeCount[] = new int[vertices.size()];
    for (int i = 0; i < mesh.getEdges().length; i++)
    {
      Edge edge = mesh.getEdges()[i];
      if (edge.f2 == -1)
      {
        vertexEdgeCount[edge.v1]++;
        vertexEdgeCount[edge.v2]++;
      }
    }
    for (int i = 0; i < vertexEdgeCount.length; i++)
    {
      if (vertexEdgeCount[i] != 0 && vertexEdgeCount[i] != 2)
      {
        new BStandardDialog("", Translate.text("illegalExtract"), BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
    }

    // Copy over smoothness values.

    for (Integer vertex : vertices)
        ((TriangleMesh.Vertex) mesh.getVertices()[vertIndex[vertex]]).smoothness = vt[vertex].smoothness;
    for (int i = 0; i < faces.size(); i++)
    {
      Face face = fc[faces.get(i)];
      Face newFace = mesh.getFaces()[i];
      mesh.getEdges()[newFace.e1].smoothness = theMesh.getEdges()[face.e1].smoothness;
      mesh.getEdges()[newFace.e2].smoothness = theMesh.getEdges()[face.e2].smoothness;
      mesh.getEdges()[newFace.e3].smoothness = theMesh.getEdges()[face.e3].smoothness;
    }

    // Copy over parameter values.

    ParameterValue oldValues[] = theMesh.getParameterValues();
    ParameterValue newValues[] = mesh.getParameterValues();
    for (int i = 0; i < oldValues.length; i++)
    {
      if (oldValues[i] instanceof FaceParameterValue)
      {
        FaceParameterValue old = (FaceParameterValue) oldValues[i];
        double val[] = new double[faces.size()];
        for (int j = 0; j < val.length; j++)
          val[j] = old.getValue()[faces.get(j)];
        newValues[i] = new FaceParameterValue(val);
      }
      else if (oldValues[i] instanceof VertexParameterValue)
      {
        VertexParameterValue old = (VertexParameterValue) oldValues[i];
        double val[] = new double[vertices.size()];
        for (int j = 0; j < vertIndex.length; j++)
          if (vertIndex[j] != -1)
            val[vertIndex[j]] = old.getValue()[j ];
        newValues[i] = new VertexParameterValue(val);
      }
      else if (oldValues[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue old = (FaceVertexParameterValue) oldValues[i];
        double val[][] = new double[faces.size()][];
        for (int j = 0; j < val.length; j++)
        {
          int faceIndex = faces.get(j);
          val[j] = new double[] {old.getValue(faceIndex, 0), old.getValue(faceIndex, 1), old.getValue(faceIndex, 2)};
        }
        newValues[i] = new FaceVertexParameterValue(val);
      }
    }

    // Add it to the scene.

    Widget parent = (Widget) parentWindow;
    while (parent != null && !(parent instanceof LayoutWindow))
      parent = parent.getParent();
    if (parent != null)
    {
      String name = new BStandardDialog("", Translate.text("extractedMeshName"), BStandardDialog.QUESTION).showInputDialog(this, null, "Extracted Mesh");
      if (name != null)
      {
        ((LayoutWindow) parent).addObject(mesh, ((MeshViewer) theView[currentView]).thisObjectInScene.getCoords().duplicate(), name, null);
        ((LayoutWindow) parent).updateImage();
      }
    }
  }

  public void extractCurveCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    Vertex vt[] = (Vertex []) theMesh.getVertices();
    Edge ed[] = theMesh.getEdges();
    Vector<Edge> edges = new Vector<Edge>();
    int i;

    if (selectMode != EDGE_MODE)
      return;

    // Find the select edges, and try to chain them together.

    for (i = 0; i < selected.length; i++)
      if (selected[i])
        edges.addElement(ed[i]);
    if (edges.size() == 0)
      return;
    Edge first = edges.elementAt(0), last = first;
    Vector<Edge> ordered = new Vector<Edge>();
    ordered.addElement(first);
    edges.removeElementAt(0);
    while (edges.size() > 0)
    {
      for (i = 0; i < edges.size(); i++)
      {
        Edge e = edges.elementAt(i);
        if (e.v1 == first.v1 || e.v2 == first.v1 || e.v1 == first.v2 || e.v2 == first.v2)
        {
          ordered.insertElementAt(e, 0);
          first = e;
          break;
        }
        if (e.v1 == last.v1 || e.v2 == last.v1 || e.v1 == last.v2 || e.v2 == last.v2)
        {
          ordered.addElement(e);
          last = e;
          break;
        }
      }
      if (i == edges.size())
      {
        new BStandardDialog("", Translate.text("edgesNotContinuous"), BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
      edges.removeElementAt(i);
    }

    // Now find the sequence of vertices.

    boolean closed = (ordered.size() > 2 && (last.v1 == first.v1 || last.v2 == first.v1 || last.v1 == first.v2 || last.v2 == first.v2));
    Vec3 v[] = new Vec3 [closed ? ordered.size() : ordered.size()+1];
    float smoothness[] = new float [v.length];
    Edge second = (ordered.size() == 1 ? first : ordered.elementAt(1));
    int prev;
    if (first.v1 == second.v1 || first.v1 == second.v2)
      prev = first.v2;
    else
      prev = first.v1;
    for (i = 0; i < ordered.size(); i++)
    {
      Edge e = ordered.elementAt(i);
      v[i] = new Vec3(vt[prev].r);
      smoothness[i] = vt[prev].smoothness;
      prev = (e.v1 == prev ? e.v2 : e.v1);
    }
    if (!closed)
    {
      v[i] = new Vec3(vt[prev].r);
      smoothness[i] = vt[prev].smoothness;
    }
    int smoothingMethod = theMesh.getSmoothingMethod();
    if (smoothingMethod == Mesh.SMOOTH_SHADING)
      smoothingMethod = Mesh.NO_SMOOTHING;
    Curve cv = new Curve(v, smoothness, smoothingMethod, closed);
    Widget parent = (Widget) parentWindow;
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

  public void setSmoothnessCommand()
  {
    final TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    TriangleMesh oldMesh = (TriangleMesh) theMesh.duplicate();
    final Vertex vt[] = (Vertex []) theMesh.getVertices();
    final Edge ed[] = theMesh.getEdges();
    final boolean pointmode = (selectMode == POINT_MODE);
    final ActionProcessor processor = new ActionProcessor();
    float value;
    final ValueSlider smoothness;
    int i;
    
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i == selected.length)
      return;
    if (pointmode)
      value = vt[i].smoothness;
    else
      value = ed[i].smoothness;
    value = 0.001f * (Math.round(value*1000.0f));
    smoothness = new ValueSlider(0.0, 1.0, 100, (double) value);
    smoothness.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        processor.addEvent(new Runnable() {
          public void run()
          {
            float s = (float) smoothness.getValue();
            for (int i = 0; i < selected.length; i++)
              if (selected[i])
              {
                if (pointmode)
                  vt[i].smoothness = s;
                else
                  ed[i].smoothness = s;
              }
            theMesh.setSmoothingMethod(theMesh.getSmoothingMethod());
            objectChanged();
            updateImage();
          }
        } );
      }
    } );
    ComponentsDialog dlg = new ComponentsDialog(this,
        Translate.text(pointmode ? "setPointSmoothness" : "setEdgeSmoothness"),
        new Widget [] {smoothness}, new String [] {Translate.text("Smoothness")});
    processor.stopProcessing();
    if (dlg.clickedOk())
      setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, oldMesh}));
    else
    {
      theMesh.copyObject(oldMesh);
      objectChanged();
      updateImage();
    }
  }

  public void reverseNormalsCommand()
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    theMesh.reverseNormals();
    objectChanged();
    updateImage();
  }

  void setSmoothingMethod(int method)
  {
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    for (int i = 0; i < smoothItem.length; i++)
      smoothItem[i].setState(false);
    smoothItem[method].setState(true);
    theMesh.setSmoothingMethod(method);
    objectChanged();
    updateMenus();
    updateImage();
  }
  
  /** Given a list of deltas which will be added to the selected vertices, calculate the
      corresponding deltas for the unselected vertices according to the mesh tension. */
  
  public void adjustDeltas(Vec3 delta[])
  {
    int dist[] = getSelectionDistance(), count[] = new int [delta.length];
    TriangleMesh theMesh = (TriangleMesh) objInfo.getObject();
    TriangleMesh.Edge edge[] = theMesh.getEdges();
    int maxDistance = getTensionDistance();
    double tension = getMeshTension(), scale[] = new double [maxDistance+1];
    
    for (int i = 0; i < delta.length; i++)
      if (dist[i] != 0)
        delta[i].set(0.0, 0.0, 0.0);
    for (int i = 0; i < maxDistance; i++)
    {
      for (int j = 0; j < count.length; j++)
        count[j] = 0;
      for (int j = 0; j < edge.length; j++)
      {
        if (dist[edge[j].v1] == i && dist[edge[j].v2] == i+1)
        {
          count[edge[j].v2]++;
          delta[edge[j].v2].add(delta[edge[j].v1]);
        }
        else if (dist[edge[j].v2] == i && dist[edge[j].v1] == i+1)
        {
          count[edge[j].v1]++;
          delta[edge[j].v1].add(delta[edge[j].v2]);
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
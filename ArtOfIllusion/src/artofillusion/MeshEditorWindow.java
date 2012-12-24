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
import java.util.*;

/** The MeshEditorWindow class represents the window for editing Mesh objects.  This is an
    abstract class, with subclasses for various types of objects. */

public abstract class MeshEditorWindow extends ObjectEditorWindow implements MeshEditController, EditingWindow
{
  protected Mesh oldMesh;
  protected BMenu viewMenu, colorSurfaceMenu;
  protected BMenuItem undoItem, redoItem, templateItem, axesItem, splitViewItem;
  protected BCheckBoxMenuItem displayItem[], coordsItem[], showItem[], colorSurfaceItem[];
  protected int meshTension, tensionDistance;

  protected static int lastMeshTension = 2;
  protected static int lastTensionDistance = 0;
  protected static final double[] tensionArray = {5.0, 3.0, 2.0, 1.0, 0.5};
  protected static byte lastRenderMode[] = new byte [] {ViewerCanvas.RENDER_TRANSPARENT, ViewerCanvas.RENDER_TRANSPARENT, ViewerCanvas.RENDER_TRANSPARENT, ViewerCanvas.RENDER_TRANSPARENT};
  protected static boolean lastShowMesh[] = new boolean [] {true, true, true, true};
  protected static boolean lastShowSurface[] = new boolean [] {true, true, true, true};
  protected static boolean lastShowSkeleton[] = new boolean [] {true, true, true, true};
  protected static boolean lastShowScene[] = new boolean [] {false, false, false, false};
  protected static boolean lastFreehand, lastUseWorldCoords;

  public MeshEditorWindow(EditingWindow parent, String title, ObjectInfo obj)
  {
    super(parent, title, obj);
    initialize();
    oldMesh = (Mesh) obj.getObject();
    meshTension = lastMeshTension;
    tensionDistance = lastTensionDistance;
  }

  /**
   * Save the display mode when the window is closed.
   */

  public void dispose()
  {
    for (int i = 0; i < theView.length; i++)
      lastRenderMode[i] = (byte) theView[i].getRenderMode();
    savePreferences();
    super.dispose();
  }

  /**
   * This is invoked to create each of the ViewerCanvases in the window.
   * @param index      the index of the canvas to create (from 0 to 3)
   * @param controls   the contain to which the canvas should add its controls
   */

  protected ViewerCanvas createViewerCanvas(int index, RowContainer controls)
  {
    MeshViewer view = ((Mesh) objInfo.getObject()).createMeshViewer(this, controls);
    view.setRenderMode(lastRenderMode[index]);
    view.setMeshVisible(lastShowMesh[index]);
    view.setSurfaceVisible(lastShowSurface[index]);
    view.setSkeletonVisible(lastShowSkeleton[index]);
    view.setSceneVisible(lastShowScene[index]);
    view.setUseWorldCoords(lastUseWorldCoords);
    return view;
  }

  protected void createViewMenu()
  {
    BMenu displayMenu, coordsMenu;

    viewMenu = Translate.menu("view");
    menubar.add(viewMenu);
    viewMenu.add(displayMenu = Translate.menu("displayMode"));
    displayItem = new BCheckBoxMenuItem [6];
    MeshViewer view = (MeshViewer) theView[currentView];
    displayMenu.add(displayItem[0] = Translate.checkboxMenuItem("wireframeDisplay", this, "displayModeChanged", view.getRenderMode() == ViewerCanvas.RENDER_WIREFRAME));
    displayMenu.add(displayItem[1] = Translate.checkboxMenuItem("shadedDisplay", this, "displayModeChanged", view.getRenderMode() == ViewerCanvas.RENDER_FLAT));
    displayMenu.add(displayItem[2] = Translate.checkboxMenuItem("smoothDisplay", this, "displayModeChanged", view.getRenderMode() == ViewerCanvas.RENDER_SMOOTH));
    displayMenu.add(displayItem[3] = Translate.checkboxMenuItem("texturedDisplay", this, "displayModeChanged", view.getRenderMode() == ViewerCanvas.RENDER_TEXTURED));
    displayMenu.add(displayItem[4] = Translate.checkboxMenuItem("transparentDisplay", this, "displayModeChanged", view.getRenderMode() == ViewerCanvas.RENDER_TRANSPARENT));
    displayMenu.add(displayItem[5] = Translate.checkboxMenuItem("renderedDisplay", this, "displayModeChanged", view.getRenderMode() == ViewerCanvas.RENDER_RENDERED));
    if (getObject().getObject().canSetTexture())
      viewMenu.add(colorSurfaceMenu = createColorSurfaceMenu());
    viewMenu.add(createShowMenu());
    viewMenu.add(coordsMenu = Translate.menu("coordinateSystem"));
    coordsItem = new BCheckBoxMenuItem [2];
    coordsMenu.add(coordsItem[0] = Translate.checkboxMenuItem("localCoords", this, "coordinateSystemChanged", !view.getUseWorldCoords()));
    coordsMenu.add(coordsItem[1] = Translate.checkboxMenuItem("sceneCoords", this, "coordinateSystemChanged", view.getUseWorldCoords()));
    viewMenu.add(splitViewItem = Translate.menuItem(numViewsShown == 1 ? "fourViews" : "oneView", this, "toggleViewsCommand"));
    viewMenu.add(Translate.menuItem("grid", this, "setGridCommand"));
    viewMenu.add(axesItem = Translate.menuItem(view.getShowAxes() ? "hideCoordinateAxes" : "showCoordinateAxes", this, "showAxesCommand"));
    viewMenu.add(templateItem = Translate.menuItem("showTemplate", this, "showTemplateCommand"));
    viewMenu.add(Translate.menuItem("setTemplate", this, "setTemplateCommand"));
  }

  protected BMenu createShowMenu()
  {
    BMenu menu = Translate.menu("show");
    showItem = new BCheckBoxMenuItem [4];
    MeshViewer view = (MeshViewer) theView[currentView];
    menu.add(showItem[0] = Translate.checkboxMenuItem("controlMesh", this, "shownItemChanged", view.getMeshVisible()));
    menu.add(showItem[1] = Translate.checkboxMenuItem("surface", this, "shownItemChanged", view.getSurfaceVisible()));
    menu.add(showItem[2] = Translate.checkboxMenuItem("skeleton", this, "shownItemChanged", view.getSkeletonVisible()));
    menu.add(showItem[3] = Translate.checkboxMenuItem("entireScene", this, "shownItemChanged", view.getSceneVisible()));
    return menu;
  }

  protected BMenu createColorSurfaceMenu()
  {
    BMenu menu = Translate.menu("colorSurfaceBy");
    TextureParameter params[] = getObject().getObject().getParameters();
    int numParams = (params == null ? 0 : params.length);
    colorSurfaceItem = new BCheckBoxMenuItem [numParams+2];
    MeshViewer view = (MeshViewer) theView[currentView];
    menu.add(colorSurfaceItem[0] = Translate.checkboxMenuItem("default", this, "surfaceColoringChanged", view.getSurfaceTextureParameter() == null));
    menu.add(colorSurfaceItem[1] = Translate.checkboxMenuItem("boneWeight", this, "surfaceColoringChanged", view.getSurfaceTextureParameter() == getJointWeightParam()));
    BMenu paramMenu = Translate.menu("parameter");
    menu.add(paramMenu);
    for (int i = 0; i < numParams; i++)
    {
      paramMenu.add(colorSurfaceItem[i+2] = new BCheckBoxMenuItem(params[i].name, view.getSurfaceTextureParameter() == params[i]));
      colorSurfaceItem[i+2].addEventLink(CommandEvent.class, this, "surfaceColoringChanged");
    }
    return menu;
  }

  /** Load all the preferences into memory. */

  protected void loadPreferences()
  {
    super.loadPreferences();
    lastRenderMode = preferences.getByteArray("displayMode", lastRenderMode);
    lastFreehand = preferences.getBoolean("freehandSelection", lastFreehand);
    lastShowMesh = loadBooleanArrayPreference("showControlMesh", lastShowMesh);
    lastShowScene = loadBooleanArrayPreference("showScene", lastShowScene);
    lastShowSkeleton = loadBooleanArrayPreference("showSkeleton", lastShowSkeleton);
    lastShowSurface = loadBooleanArrayPreference("showSurface", lastShowSurface);
    lastUseWorldCoords = preferences.getBoolean("useSceneCoords", lastUseWorldCoords);
    lastMeshTension = preferences.getInt("meshTension", lastMeshTension);
    lastTensionDistance = preferences.getInt("meshTensionDistance", lastTensionDistance);
  }

  /** Save user settings that should be persistent between sessions. */

  protected void savePreferences()
  {
    super.savePreferences();
    preferences.putByteArray("displayMode", lastRenderMode);
    preferences.putBoolean("freehandSelection", lastFreehand);
    saveBooleanArrayPreference("showControlMesh", lastShowMesh);
    saveBooleanArrayPreference("showScene", lastShowScene);
    saveBooleanArrayPreference("showSkeleton", lastShowSkeleton);
    saveBooleanArrayPreference("showSurface", lastShowSurface);
    preferences.putBoolean("useSceneCoords", lastUseWorldCoords);
    preferences.putInt("meshTension", lastMeshTension);
    preferences.putInt("meshTensionDistance", lastTensionDistance);
  }

  /** Set the Mesh object for this viewer. */

  public abstract void setMesh(Mesh mesh);

  /** Get an array of flags specifying which parts of the mesh are selected.  Depending on the selection mode,
      this may correspond to vertices, faces, edges, etc. */

  public abstract boolean[] getSelection();

  /** Set an array of flags specifying which parts of the mesh are selected.  Depending on the selection mode,
      this may correspond to vertices, faces, edges, etc. */

  public abstract void setSelection(boolean selected[]);

  /** Get the distance of each vertex from a selected vertex.  This is 0 for a selected
      vertex, 1 for a vertex adjacent to a selected one, etc., up to a specified maximum
      distance.  For vertices more than the maximum distance for a selected one, it is -1. */

  public abstract int[] getSelectionDistance();

  /** This should be called whenever the object has changed. */

  public void objectChanged()
  {
    getObject().clearCachedMeshes();
    for (ViewerCanvas view : theView)
      view.viewChanged(false);
  }

  /* EditingWindow methods. */

  public Scene getScene()
  {
    return ((MeshViewer) theView[currentView]).getScene();
  }

  public void undoCommand()
  {
    super.undoCommand();
    setMesh((Mesh) getObject().getObject());
    for (ViewerCanvas view : theView)
      view.viewChanged(false);
  }

  public void redoCommand()
  {
    super.redoCommand();
    setMesh((Mesh) getObject().getObject());
    for (ViewerCanvas view : theView)
      view.viewChanged(false);
  }

  public void updateMenus()
  {
    MeshViewer view = (MeshViewer) theView[currentView];
    undoItem.setEnabled(undoStack.canUndo());
    redoItem.setEnabled(undoStack.canRedo());
    templateItem.setEnabled(view.getTemplateImage() != null);
    templateItem.setText(view.getTemplateShown() ? Translate.text("menu.hideTemplate") : Translate.text("menu.showTemplate"));
    splitViewItem.setText(numViewsShown == 1 ? Translate.text("menu.fourViews") : Translate.text("menu.oneView"));
    axesItem.setText(Translate.text(view.getShowAxes() ? "menu.hideCoordinateAxes" : "menu.showCoordinateAxes"));
    displayItem[0].setState(view.getRenderMode() == ViewerCanvas.RENDER_WIREFRAME);
    displayItem[1].setState(view.getRenderMode() == ViewerCanvas.RENDER_FLAT);
    displayItem[2].setState(view.getRenderMode() == ViewerCanvas.RENDER_SMOOTH);
    displayItem[3].setState(view.getRenderMode() == ViewerCanvas.RENDER_TEXTURED);
    displayItem[4].setState(view.getRenderMode() == ViewerCanvas.RENDER_TRANSPARENT);
    displayItem[5].setState(view.getRenderMode() == ViewerCanvas.RENDER_RENDERED);
    if (showItem[0] != null)
      showItem[0].setState(view.getMeshVisible());
    if (showItem[1] != null)
      showItem[1].setState(view.getSurfaceVisible());
    if (showItem[2] != null)
      showItem[2].setState(view.getSkeletonVisible());
    if (showItem[3] != null)
      showItem[3].setState(view.getSceneVisible());
    if (colorSurfaceMenu != null)
      colorSurfaceMenu.setEnabled(view.getRenderMode() == ViewerCanvas.RENDER_SMOOTH || view.getRenderMode() == ViewerCanvas.RENDER_TEXTURED);
    if (colorSurfaceItem != null)
    {
      colorSurfaceItem[0].setState(view.getSurfaceTextureParameter() == null);
      colorSurfaceItem[1].setState(view.getSurfaceTextureParameter() == getJointWeightParam());
      TextureParameter params[] = getObject().getObject().getParameters();
      for (int i = 2; i < colorSurfaceItem.length; i++)
        colorSurfaceItem[i].setState(view.getSurfaceTextureParameter() == params[i-2]);
    }
  }

  private void displayModeChanged(WidgetEvent ev)
  {
    Widget source = ev.getWidget();
    for (int i = 0; i < displayItem.length; i++)
      displayItem[i].setState(source == displayItem[i]);
    if (source == displayItem[0])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_WIREFRAME);
    else if (source == displayItem[1])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_FLAT);
    else if (source == displayItem[2])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_SMOOTH);
    else if (source == displayItem[3])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_TEXTURED);
    else if (source == displayItem[4])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_TRANSPARENT);
    else if (source == displayItem[5])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_RENDERED);
    savePreferences();
    updateMenus();
  }

  private void coordinateSystemChanged(WidgetEvent ev)
  {
    Widget source = ev.getWidget();
    for (int i = 0; i < coordsItem.length; i++)
      coordsItem[i].setState(source == coordsItem[i]);
    lastUseWorldCoords = source == coordsItem[1];
    for (int i = 0; i < theView.length; i++)
      ((MeshViewer) theView[i]).setUseWorldCoords(lastUseWorldCoords);
    savePreferences();
    updateImage();
  }

  private void shownItemChanged(WidgetEvent ev)
  {
    Widget source = ev.getWidget();
    MeshViewer view = (MeshViewer) theView[currentView];
    if (source == showItem[0])
      view.setMeshVisible(lastShowMesh[currentView] = showItem[0].getState());
    else if (source == showItem[1])
      view.setSurfaceVisible(lastShowSurface[currentView] = showItem[1].getState());
    else if (source == showItem[2])
      view.setSkeletonVisible(lastShowSkeleton[currentView] = showItem[2].getState());
    else if (source == showItem[3])
      view.setSceneVisible(lastShowScene[currentView] = showItem[3].getState());
    savePreferences();
    updateImage();
  }

  private void surfaceColoringChanged(WidgetEvent ev)
  {
    Widget source = ev.getWidget();
    MeshViewer view = (MeshViewer) theView[currentView];
    for (int i = 0; i < colorSurfaceItem.length; i++)
      if (source == colorSurfaceItem[i])
      {
        if (i == 0)
          view.setSurfaceTextureParameter(null);
        else if (i == 1)
          view.setSurfaceTextureParameter(getJointWeightParam());
        else
          view.setSurfaceTextureParameter(getObject().getObject().getParameters()[i-2]);
      }
    savePreferences();
    updateImage();
    updateMenus();
  }

  /** Determine whether to use freehand selection mode. */

  public boolean isFreehand()
  {
    return ((MeshViewer) theView[0]).getFreehandSelection();
  }

  /** Set whether to use freehand selection mode. */

  public void setFreehand(boolean freehand)
  {
    lastFreehand = freehand;
    for (int i = 0; i < theView.length; i++)
      ((MeshViewer) theView[i]).setFreehandSelection(freehand);
    savePreferences();
  }

  /** Allow the user to enter new coordinates for one or more vertices. */

  public void setPointsCommand()
  {
    int i, j = 0, num = 0;
    final Mesh theMesh = (Mesh) getObject().getObject();
    Mesh oldMesh = (Mesh) theMesh.duplicate();
    Skeleton s = theMesh.getSkeleton();
    Joint jt[] = null;
    final int selectDist[] = getSelectionDistance();
    final MeshViewer view = (MeshViewer) theView[currentView];
    final CoordinateSystem coords = view.thisObjectInScene.getCoords();
    final MeshVertex vert[] = theMesh.getVertices();
    final Vec3 points[] = new Vec3 [vert.length];
    final ValueField xField, yField, zField;
    ValueSlider weightSlider = null;
    BComboBox jointChoice = null;
    double weight = -1.0;
    int joint = -2;
    String title;
    ComponentsDialog dlg;

    for (i = 0; i < selectDist.length; i++)
      if (selectDist[i] == 0)
      {
        num++;
        j = i;
        if (weight == -1.0)
          weight = vert[i].ikWeight;
        else if (vert[i].ikWeight != weight)
          weight = Double.NaN;
        if (joint == -2)
          joint = vert[i].ikJoint;
        else if (vert[i].ikJoint != joint)
          joint = -3;
      }
    if (num == 0)
      return;
    if (num == 1)
    {
      Vec3 pos = vert[j].r;
      if (view.getUseWorldCoords() && coords != null)
        pos = coords.fromLocal().times(pos);
      xField = new ValueField(pos.x, ValueField.NONE, 5);
      yField = new ValueField(pos.y, ValueField.NONE, 5);
      zField = new ValueField(pos.z, ValueField.NONE, 5);
      title = Translate.text("editVertSingle");
    }
    else
    {
      xField = new ValueField(Double.NaN, ValueField.NONE, 5);
      yField = new ValueField(Double.NaN, ValueField.NONE, 5);
      zField = new ValueField(Double.NaN, ValueField.NONE, 5);
      title = Translate.text("editVertMultiple");
    }
    Object listener = new Object() {
      void processEvent()
      {
        for (int i = 0; i < selectDist.length; i++)
        {
          points[i] = vert[i].r;
          if (selectDist[i] == 0)
          {
            if (view.getUseWorldCoords() && coords != null)
              coords.fromLocal().transform(points[i]);
            if (!Double.isNaN(xField.getValue()))
              points[i].x = xField.getValue();
            if (!Double.isNaN(yField.getValue()))
              points[i].y = yField.getValue();
            if (!Double.isNaN(zField.getValue()))
              points[i].z = zField.getValue();
            if (view.getUseWorldCoords() && coords != null)
              coords.toLocal().transform(points[i]);
          }
        }
        theMesh.setVertexPositions(points);
        setMesh(theMesh);
        updateImage();
      }
    };
    xField.addEventLink(ValueChangedEvent.class, listener);
    yField.addEventLink(ValueChangedEvent.class, listener);
    zField.addEventLink(ValueChangedEvent.class, listener);
    if (s == null)
      dlg = new ComponentsDialog(this, title, new Widget [] {xField, yField, zField},
        new String [] {"X", "Y", "Z"});
    else
    {
      weightSlider = new ValueSlider(0.0, 1.0, 100, weight);
      jointChoice = new BComboBox();
      jointChoice.add(Translate.text("none"));
      jt = s.getJoints();
      for (i = 0; i < jt.length; i++)
        jointChoice.add(jt[i].name);
      if (joint == -3)
        jointChoice.add("");
      jointChoice.setSelectedIndex(0);
      for (i = 0; i < jt.length; i++)
        if (jt[i].id == joint)
          jointChoice.setSelectedIndex(i+1);
      if (joint == -3)
        jointChoice.setSelectedIndex(jt.length+1);
      dlg = new ComponentsDialog(this, title, new Widget [] {xField, yField, zField, jointChoice, weightSlider},
        new String [] {"X", "Y", "Z", Translate.text("ikBone"), Translate.text("ikWeight")});
    }
    if (dlg.clickedOk())
    {
      for (i = 0; i < selectDist.length; i++)
        if (selectDist[i] == 0)
        {
          if (weightSlider != null && !Double.isNaN(weightSlider.getValue()))
            vert[i].ikWeight = weightSlider.getValue();
          if (jointChoice != null)
          {
            if (jointChoice.getSelectedIndex() == 0)
              vert[i].ikJoint = -1;
            else if (jointChoice.getSelectedIndex() <= jt.length)
              vert[i].ikJoint = jt[jointChoice.getSelectedIndex()-1].id;
          }
        }
      setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, oldMesh}));
    }
    else
    {
      theMesh.copyObject((Object3D) oldMesh);
      setMesh(theMesh);
      updateImage();
    }
  }

  /** Allow the user to transform one or more vertices. */

  public void transformPointsCommand()
  {
    int i, j;
    Mesh theMesh = (Mesh) getObject().getObject();
    int selectDist[] = getSelectionDistance();
    MeshVertex vert[] = theMesh.getVertices();
    Vec3 points[] = new Vec3 [vert.length], center;
    MeshViewer view = (MeshViewer) theView[currentView];
    CoordinateSystem coords = view.thisObjectInScene.getCoords();

    for (i = 0; i < selectDist.length && selectDist[i] == -1; i++);
    if (i == selectDist.length)
      return;

    // Create the dialog.

    FormContainer content = new FormContainer(4, 5);
    LayoutInfo eastLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    content.add(Translate.label("Move"), 0, 1, eastLayout);
    content.add(Translate.label("Rotate"), 0, 2, eastLayout);
    content.add(Translate.label("Scale"), 0, 3, eastLayout);
    content.add(new BLabel("X"), 1, 0);
    content.add(new BLabel("Y"), 2, 0);
    content.add(new BLabel("Z"), 3, 0);
    ValueField fields[] = new ValueField[9];
    for (i = 0; i < 9; i++)
      content.add(fields[i] = new ValueField(Double.NaN, ValueField.NONE), (i%3)+1, (i/3)+1);
    RowContainer row = new RowContainer();
    content.add(row, 0, 4, 4, 1);
    row.add(Translate.label("transformAround"));
    BComboBox centerChoice = new BComboBox(new String [] {
      Translate.text("centerOfSelection"),
      Translate.text("objectOrigin")
    });
    row.add(centerChoice);
    PanelDialog dlg = new PanelDialog(this, Translate.text("transformPoints"), content);
    if (!dlg.clickedOk())
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {theMesh, theMesh.getVertexPositions()}));
    double val[] = new double [9];
    for (i = 0; i < val.length; i++)
    {
      val[i] = fields[i].getValue();
      if (Double.isNaN(val[i]))
        val[i] = (i < 6 ? 0.0 : 1.0);
    }
    Mat4 m = Mat4.translation(val[0], val[1], val[2]);
    m = m.times(Mat4.xrotation(val[3]*Math.PI/180.0));
    m = m.times(Mat4.yrotation(val[4]*Math.PI/180.0));
    m = m.times(Mat4.zrotation(val[5]*Math.PI/180.0));
    m = m.times(Mat4.scale(val[6], val[7], val[8]));
    if (view.getUseWorldCoords() && coords != null)
      m = coords.toLocal().times(m).times(coords.fromLocal());
    if (centerChoice.getSelectedIndex() == 0)
    {
      center = new Vec3();
      for (i = j = 0; i < selectDist.length; i++)
        if (selectDist[i] == 0)
        {
          center.add(vert[i].r);
          j++;
        }
      center.scale(1.0/j);
      m = Mat4.translation(center.x, center.y, center.z).times(m).times(Mat4.translation(-center.x, -center.y, -center.z));
    }
    for (i = 0; i < selectDist.length; i++)
    {
      points[i] = vert[i].r;
      if (selectDist[i] == 0)
        points[i] = m.times(points[i]);
    }
    theMesh.setVertexPositions(points);
    setMesh(theMesh);
    updateImage();
  }

  /** Displace selected vertices by a random amount. */

  public void randomizeCommand()
  {
    int i;
    Mesh theMesh = (Mesh) getObject().getObject();
    int selectDist[] = getSelectionDistance();
    MeshViewer view = (MeshViewer) theView[currentView];
    CoordinateSystem coords = view.thisObjectInScene.getCoords();
    MeshVertex vert[] = theMesh.getVertices();
    Vec3 points[] = new Vec3 [vert.length];
    ValueField xfield, yfield, zfield;

    for (i = 0; i < selectDist.length && selectDist[i] == -1; i++);
    if (i == selectDist.length)
      return;
    xfield = new ValueField(0.0, ValueField.NONE);
    yfield = new ValueField(0.0, ValueField.NONE);
    zfield = new ValueField(0.0, ValueField.NONE);
    ComponentsDialog dlg = new ComponentsDialog(this, "Maximum random displacement:", new Widget []
            {xfield, yfield, zfield}, new String[] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {theMesh, theMesh.getVertexPositions()}));
    for (i = 0; i < selectDist.length; i++)
    {
      points[i] = vert[i].r;
      if (selectDist[i] == 0)
      {
        if (view.getUseWorldCoords() && coords != null)
          coords.fromLocal().transform(points[i]);
        points[i].x += (1.0-2.0*Math.random())*xfield.getValue();
        points[i].y += (1.0-2.0*Math.random())*yfield.getValue();
        points[i].z += (1.0-2.0*Math.random())*zfield.getValue();
        if (view.getUseWorldCoords() && coords != null)
          coords.toLocal().transform(points[i]);
      }
    }
    theMesh.setVertexPositions(points);
    setMesh(theMesh);
    updateImage();
  }

  /* Center the mesh. */

  public void centerCommand()
  {
    Mesh theMesh = (Mesh) getObject().getObject();
    MeshVertex vert[] = theMesh.getVertices();
    MeshViewer view = (MeshViewer) theView[currentView];
    CoordinateSystem coords = view.thisObjectInScene.getCoords();
    Vec3 center = theMesh.getBounds().getCenter(), points[] = new Vec3 [vert.length];

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {theMesh, theMesh.getVertexPositions()}));
    if (view.getUseWorldCoords() && coords != null)
    {
      coords.fromLocal().transform(center);
      coords.toLocal().transformDirection(center);
    }
    for (int i = 0; i < vert.length; i++)
      points[i] = vert[i].r.minus(center);
    theMesh.setVertexPositions(points);
    Skeleton skeleton = theMesh.getSkeleton();
    if (skeleton != null)
    {
      Joint joint[] = skeleton.getJoints();
      for (int i = 0; i < joint.length; i++)
        joint[i].coords.setOrigin(joint[i].coords.getOrigin().minus(center));
    }
    setMesh(theMesh);
    updateImage();
  }

  /** Allow the user to set the mesh tension. */

  public void setTensionCommand()
  {
    ValueField distanceField = new ValueField((double) tensionDistance, ValueField.NONNEGATIVE+ValueField.INTEGER);
    BComboBox tensionChoice = new BComboBox(new String [] {
      Translate.text("VeryLow"),
      Translate.text("Low"),
      Translate.text("Medium"),
      Translate.text("High"),
      Translate.text("VeryHigh")
    });
    tensionChoice.setSelectedIndex(meshTension);
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("setTensionTitle"),
        new Widget [] {distanceField, tensionChoice},
        new String [] {Translate.text("maxDistance"), Translate.text("Tension")});
    if (!dlg.clickedOk())
      return;
    lastTensionDistance = tensionDistance = (int) distanceField.getValue();
    lastMeshTension = meshTension = tensionChoice.getSelectedIndex();
    savePreferences();
  }

  public double getMeshTension()
  {
    return tensionArray[meshTension];
  }

  public int getTensionDistance()
  {
    return tensionDistance;
  }

  /** Get the extra texture parameter which was added to the mesh to keep track of
      face indices in the editor.  By default this returns null.  Subclasses which use
      an extra parameter to keep track of face indices should override it. */

  public TextureParameter getFaceIndexParameter()
  {
    return null;
  }

  /** Get the extra texture parameter which was added to the mesh to keep track of
      joint weighting.  By default this returns null.  Subclasses which use
      an extra parameter to keep track of joint weights should override it. */

  public TextureParameter getJointWeightParam()
  {
    return null;
  }

  /** Determine whether a TextureParameter was added to the mesh by the editor */

  public boolean isExtraParameter(TextureParameter param)
  {
    return (param == getFaceIndexParameter() || param == getJointWeightParam());
  }

  /** Allow the user to set the texture parameters for selected vertices. */

  public void setParametersCommand()
  {
    if (getSelectionMode() == POINT_MODE)
      setVertexParametersCommand();
    else if (getSelectionMode() == FACE_MODE)
      setFaceParametersCommand();
  }

  protected void setVertexParametersCommand()
  {
    ObjectInfo info = getObject();
    Mesh theMesh = (Mesh) info.getObject();
    final MeshVertex vert[] = theMesh.getVertices();
    TextureParameter param[] = info.getObject().getParameters();
    final ParameterValue paramValue[] = info.getObject().getParameterValues();
    int i, j, k, paramIndex[] = null;
    final int selectDist[] = getSelectionDistance();
    double value[];

    for (j = 0; j < selectDist.length && selectDist[j] != 0; j++);
    if (j == selectDist.length)
      return;
    if (param != null)
    {
      // Find the list of per-vertex parameters.

      int num = 0;
      for (i = 0; i < param.length; i++)
        if (paramValue[i] instanceof VertexParameterValue)
          if (!isExtraParameter(param[i]))
            num++;
      paramIndex = new int [num];
      for (i = 0, k = 0; k < param.length; k++)
        if (paramValue[k] instanceof VertexParameterValue)
          if (!isExtraParameter(param[k]))
            paramIndex[i++] = k;
    }
    if (paramIndex == null || paramIndex.length == 0)
    {
      new BStandardDialog("", Translate.text("noPerVertexParams"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    value = new double [paramIndex.length];
    for (i = 0; i < paramIndex.length; i++)
    {
      double currentVal[] = ((VertexParameterValue) paramValue[paramIndex[i]]).getValue();
      value[i] = currentVal[j];
      for (k = j; k < selectDist.length; k++)
        if (selectDist[k] == 0 && currentVal[k] != value[i])
          value[i] = Double.NaN;
    }

    // Define an inner class used for resetting parameters that represent texture coordinates.

    class ResetButton extends BButton
    {
      VertexParameterValue xparamVal, yparamVal, zparamVal;
      double xvalList[], yvalList[], zvalList[];
      ValueField xfield, yfield, zfield;

      public ResetButton()
      {
        super(Translate.text("Reset"));
        addEventLink(CommandEvent.class, this);
      }

      public void addParam(int index, int type, ValueField field)
      {
        if (type == TextureParameter.X_COORDINATE)
        {
          xparamVal = (VertexParameterValue) paramValue[index];
          xvalList = xparamVal.getValue();
          xfield = field;
        }
        else if (type == TextureParameter.Y_COORDINATE)
        {
          yparamVal = (VertexParameterValue) paramValue[index];
          yvalList = yparamVal.getValue();
          yfield = field;
        }
        else if (type == TextureParameter.Z_COORDINATE)
        {
          zparamVal = (VertexParameterValue) paramValue[index];
          zvalList = zparamVal.getValue();
          zfield = field;
        }
      }

      private void processEvent()
      {
        BStandardDialog dlg = new BStandardDialog("", Translate.text("resetCoordsToPos"), BStandardDialog.QUESTION);
        String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
        int choice = dlg.showOptionDialog(this, options, options[0]);
        if (choice == 1)
          return;
        double xval = Double.NaN, yval = Double.NaN, zval = Double.NaN;
        for (int ind = 0; ind < selectDist.length; ind++)
          if (selectDist[ind] == 0)
          {
            // Reset the texture coordinates for this vertex.

            if (xparamVal != null)
            {
              xvalList[ind] = vert[ind].r.x;
              if (Double.isNaN(xval))
              {
                xval = vert[ind].r.x;
                xfield.setValue(xval);
              }
              else if (xval != vert[ind].r.x)
                xfield.setValue(Double.NaN);
            }
            if (yparamVal != null)
            {
              yvalList[ind] = vert[ind].r.y;
              if (Double.isNaN(yval))
              {
                yval = vert[ind].r.y;
                yfield.setValue(yval);
              }
              else if (yval != vert[ind].r.y)
                yfield.setValue(Double.NaN);
            }
            if (zparamVal != null)
            {
              zvalList[ind] = vert[ind].r.z;
              if (Double.isNaN(zval))
              {
                zval = vert[ind].r.z;
                zfield.setValue(zval);
              }
              else if (zval != vert[ind].r.z)
                zfield.setValue(Double.NaN);
            }
          }
        if (xparamVal != null)
          xparamVal.setValue(xvalList);
        if (yparamVal != null)
          yparamVal.setValue(yvalList);
        if (zparamVal != null)
          zparamVal.setValue(zvalList);
      }
    }

    // Build the panel for editing the values.

    Widget editWidget[] = new Widget [paramIndex.length];
    ColumnContainer content = new ColumnContainer();
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
    LayoutInfo indent1 = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 10, 0, 0), null);
    LayoutInfo indent2 = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 20, 0, 0), null);
    RowContainer coordsPanel = null;
    ResetButton reset = null;
    if (info.getObject().getTexture() instanceof LayeredTexture)
    {
      // This is a layered texture, so we want to group the parameters by layer.

      LayeredMapping map = (LayeredMapping) info.getObject().getTextureMapping();
      Texture layer[] = map.getLayers();
      for (k = 0; k < layer.length; k++)
      {
        coordsPanel = null;
        content.add(new BLabel(Translate.text("layerLabel", Integer.toString(k+1), layer[k].getName())));
        TextureParameter layerParam[] = map.getLayerParameters(k);
        boolean any = false;
        for (i = 0; i < paramIndex.length; i++)
        {
          // Determine whether this parameter is actually part of this layer.

          int m;
          TextureParameter pm = param[paramIndex[i]];
          for (m = 0; m < layerParam.length; m++)
            if (layerParam[m].equals(pm))
              break;
          if (m == layerParam.length)
            continue;
          any = true;

          // It is, so add it.

          editWidget[i] = pm.getEditingWidget(value[i]);
          if (pm.type == TextureParameter.NORMAL_PARAMETER)
          {
            RowContainer row = new RowContainer();
            row.add(new BLabel(pm.name));
            row.add(editWidget[i]);
            content.add(row, indent1);
            if (coordsPanel != null)
              coordsPanel.add(reset);
            coordsPanel = null;
            reset = null;
          }
          else if (coordsPanel == null)
          {
            coordsPanel = new RowContainer();
            content.add(Translate.label("texMappingCoords"), indent1);
            content.add(coordsPanel, indent2);
            coordsPanel.add(new BLabel(pm.name));
            coordsPanel.add(editWidget[i]);
            reset = new ResetButton();
            reset.addParam(paramIndex[i], pm.type, (ValueField) editWidget[i]);
          }
          else
          {
            coordsPanel.add(new BLabel(pm.name));
            coordsPanel.add(editWidget[i]);
            reset.addParam(paramIndex[i], pm.type, (ValueField) editWidget[i]);
          }
        }
        if (coordsPanel != null)
          coordsPanel.add(reset);
        if (!any)
          content.add(Translate.label("noLayerPerVertexParams"), indent1);
      }
    }
    else
    {
      // This is a simple texture, so just list off all the parameters.

      content.add(new BLabel(Translate.text("Texture")+": "+ info.getObject().getTexture().getName()));
      for (i = 0; i < paramIndex.length; i++)
      {
        TextureParameter pm = param[paramIndex[i]];
        editWidget[i] = pm.getEditingWidget(value[i]);
        if (pm.type == TextureParameter.NORMAL_PARAMETER)
        {
          RowContainer row = new RowContainer();
          row.add(new BLabel(pm.name));
          row.add(editWidget[i]);
          content.add(row, indent1);
          if (coordsPanel != null)
            coordsPanel.add(reset);
          coordsPanel = null;
          coordsPanel = null;
        }
        else if (coordsPanel == null)
        {
          coordsPanel = new RowContainer();
          content.add(Translate.label("texMappingCoords"), indent1);
          content.add(coordsPanel, indent2);
          coordsPanel.add(new BLabel(pm.name));
          coordsPanel.add(editWidget[i]);
          reset = new ResetButton();
          reset.addParam(paramIndex[i], pm.type, (ValueField) editWidget[i]);
        }
        else
        {
          coordsPanel.add(new BLabel(pm.name));
          coordsPanel.add(editWidget[i]);
          reset.addParam(paramIndex[i], pm.type, (ValueField) editWidget[i]);
        }
      }
      if (coordsPanel != null)
        coordsPanel.add(reset);
    }
    PanelDialog dlg = new PanelDialog(this, Translate.text("texParamsForSelectedPoints"), content);
    if (!dlg.clickedOk())
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    for (j = 0; j < editWidget.length; j++)
    {
      double d;
      if (editWidget[j] instanceof ValueField)
        d = ((ValueField) editWidget[j]).getValue();
      else
        d = ((ValueSlider) editWidget[j]).getValue();
      if (!Double.isNaN(d))
      {
        double val[] = ((VertexParameterValue) paramValue[paramIndex[j]]).getValue();
        for (i = 0; i < selectDist.length; i++)
          if (selectDist[i] == 0)
            val[i] = d;
        ((VertexParameterValue) paramValue[paramIndex[j]]).setValue(val);
      }
    }
    info.getObject().setParameterValues(paramValue);
    info.clearCachedMeshes();
    updateImage();
  }

  /** Allow the user to set the texture parameters for selected vertices or faces. */

  protected void setFaceParametersCommand()
  {
    FacetedMesh theMesh = (FacetedMesh) objInfo.getObject();
    TextureParameter param[] = objInfo.getObject().getParameters();
    final ParameterValue paramValue[] = objInfo.getObject().getParameterValues();
    boolean selected[] = getSelection();
    int i, j, k, paramIndex[] = null;
    double value[][];

    for (j = 0; j < selected.length && !selected[j]; j++);
    if (j == selected.length)
      return;
    if (param != null)
    {
      // Find the list of per-face and per-face/per-vertex parameters.

      int num = 0;
      for (i = 0; i < param.length; i++)
        if (paramValue[i] instanceof FaceParameterValue || paramValue[i] instanceof FaceVertexParameterValue)
          if (!isExtraParameter(param[i]))
            num++;
      paramIndex = new int [num];
      for (i = 0, k = 0; k < param.length; k++)
        if (paramValue[k] instanceof FaceParameterValue || paramValue[k] instanceof FaceVertexParameterValue)
          if (!isExtraParameter(param[k]))
            paramIndex[i++] = k;
    }
    if (paramIndex == null || paramIndex.length == 0)
    {
      new BStandardDialog("", Translate.text("noPerFaceParams"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    value = new double [paramIndex.length][];
    for (i = 0; i < paramIndex.length; i++)
    {
      if (paramValue[paramIndex[i]] instanceof FaceParameterValue)
      {
        double currentVal[] = ((FaceParameterValue) paramValue[paramIndex[i]]).getValue();
        double commonVal = currentVal[j];
        for (k = j; k < selected.length; k++)
          if (selected[k] && currentVal[k] != commonVal)
            commonVal = Double.NaN;
        value[i] = new double [] {commonVal};
      }
      else
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) paramValue[paramIndex[i]];
        double commonVal[] = new double [] {fvpv.getValue(j, 0), fvpv.getValue(j, 1), fvpv.getValue(j, 2)};
        for (k = j; k < selected.length; k++)
          if (selected[k])
          {
            if (fvpv.getValue(k, 0) != commonVal[0])
              commonVal[0] = Double.NaN;
            if (fvpv.getValue(k, 1) != commonVal[1])
              commonVal[1] = Double.NaN;
            if (fvpv.getValue(k, 2) != commonVal[2])
              commonVal[2] = Double.NaN;
          }
        value[i] = commonVal;
      }
    }

    // Build the panel for editing the values.

    Widget editWidget[][] = new Widget [paramIndex.length][3];
    LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 10, 0, 5), null);
    FormContainer content;
    if (objInfo.getObject().getTexture() instanceof LayeredTexture)
    {
      // This is a layered texture, so we want to group the parameters by layer.

      LayeredMapping map = (LayeredMapping) objInfo.getObject().getTextureMapping();
      Texture layer[] = map.getLayers();
      content = new FormContainer(2, paramIndex.length*3+layer.length);
      content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
      int line = 0;
      for (k = 0; k < layer.length; k++)
      {
        content.add(new BLabel(Translate.text("layerLabel", Integer.toString(k+1), layer[k].getName())), 0, line++, 2, 1);
        TextureParameter layerParam[] = map.getLayerParameters(k);
        boolean any = false;
        for (i = 0; i < paramIndex.length; i++)
        {
          // Determine whether this parameter is actually part of this layer.

          int m;
          TextureParameter pm = param[paramIndex[i]];
          for (m = 0; m < layerParam.length; m++)
            if (layerParam[m].equals(pm))
              break;
          if (m == layerParam.length)
            continue;
          any = true;

          // It is, so add it.

          for (m = 0; m < value[i].length; m++)
          {
            editWidget[i][m] = pm.getEditingWidget(value[i][m]);
            content.add(new BLabel(m == 0 ? pm.name : ""), 0, line, leftLayout);
            content.add(editWidget[i][m], 1, line++);
          }
        }
        if (!any)
          content.add(Translate.label("noLayerPerFaceParams"), 0, line++, 2, 1, new LayoutInfo());
      }
    }
    else
    {
      // This is a simple texture, so just list off all the parameters.

      content = new FormContainer(2, paramIndex.length*3+1);
      content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
      content.add(new BLabel(Translate.text("Texture")+": "+ objInfo.getObject().getTexture().getName()), 0, 0);
      int line = 1;
      for (i = 0; i < paramIndex.length; i++)
      {
        TextureParameter pm = param[paramIndex[i]];

        for (int m = 0; m < value[i].length; m++)
        {
          editWidget[i][m] = pm.getEditingWidget(value[i][m]);
          content.add(new BLabel(m == 0 ? pm.name : ""), 0, line, leftLayout);
          content.add(editWidget[i][m], 1, line++);
        }
      }
    }
    PanelDialog dlg = new PanelDialog(this, Translate.text("texParamsForSelectedFaces"), content);
    if (!dlg.clickedOk())
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    for (j = 0; j < editWidget.length; j++)
    {
      if (paramValue[paramIndex[j]] instanceof FaceParameterValue)
      {
        double d;
        if (editWidget[j][0] instanceof ValueField)
          d = ((ValueField) editWidget[j][0]).getValue();
        else
          d = ((ValueSlider) editWidget[j][0]).getValue();
        if (!Double.isNaN(d))
        {
          double val[] = ((FaceParameterValue) paramValue[paramIndex[j]]).getValue();
          for (i = 0; i < selected.length; i++)
            if (selected[i])
              val[i] = d;
          ((FaceParameterValue) paramValue[paramIndex[j]]).setValue(val);
        }
      }
      else
      {
        double d[];
        if (editWidget[j][0] instanceof ValueField)
          d = new double [] {((ValueField) editWidget[j][0]).getValue(), ((ValueField) editWidget[j][1]).getValue(), ((ValueField) editWidget[j][2]).getValue()};
        else
          d = new double [] {((ValueSlider) editWidget[j][0]).getValue(), ((ValueSlider) editWidget[j][1]).getValue(), ((ValueSlider) editWidget[j][2]).getValue()};
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) paramValue[paramIndex[j]];
        for (i = 0; i < selected.length; i++)
          if (selected[i])
            for (int m = 0; m < 3; m++)
              if (!Double.isNaN(d[m]))
                fvpv.setValue(i, m, d[m]);
      }
    }
    ((Object3D) theMesh).setParameterValues(paramValue);
    objInfo.clearCachedMeshes();
    updateImage();
  }

  /** Delete any parts of the mesh which are currently selected. */

  public abstract void deleteCommand();

  /** Delete the select joint from the skeleton. */

  public void deleteJointCommand()
  {
    Mesh theMesh = (Mesh) getObject().getObject();
    Skeleton s = theMesh.getSkeleton();

    if (s == null)
      return;
    MeshViewer view = (MeshViewer) theView[currentView];
    Joint j = s.getJoint(view.getSelectedJoint());
    if (j == null)
      return;
    String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
    BStandardDialog dlg = new BStandardDialog("", Translate.text(j.children.length == 0 ? "deleteBone" : "deleteBoneAndChildren", j.name), BStandardDialog.QUESTION);
    if (dlg.showOptionDialog(this, options, options[1]) == 1)
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_SKELETON, new Object [] {theMesh.getSkeleton(), theMesh.getSkeleton().duplicate()}));
    s.deleteJoint(view.getSelectedJoint());
    for (int i = 0; i < theView.length; i++)
      ((MeshViewer) theView[i]).setSelectedJoint(j.parent == null ? -1 : j.parent.id);
    updateImage();
    updateMenus();
  }

  /** Allow the user to set the parent of the selected joint. */

  public void setJointParentCommand()
  {
    Mesh theMesh = (Mesh) getObject().getObject();
    Skeleton s = theMesh.getSkeleton();

    if (s == null)
      return;
    Joint j = s.getJoint(((MeshViewer) theView[currentView]).getSelectedJoint());
    if (j == null)
      return;

    // Make a list of all joints which are possibilities to be the parent.

    Joint joint[] = s.getJoints();
    boolean isChild[] = new boolean [joint.length];
    markChildJoints(s, j, isChild);
    Vector<Joint> options = new Vector<Joint>();
    for (int i = 0; i < isChild.length; i++)
      if (!isChild[i])
        options.addElement(joint[i]);

    // Display a window for the user to select the parent joint.

    BList ls = new BList();
    ls.setMultipleSelectionEnabled(false);
    ls.add("("+Translate.text("None")+")");
    ls.setSelected(0, true);
    for (int i = 0; i < options.size(); i++)
    {
      ls.add(options.elementAt(i).name);
      if (options.elementAt(i) == j.parent)
        ls.setSelected(i+1, true);
    }
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("selectParentBone", j.name),
        new Widget [] {UIUtilities.createScrollingList(ls)}, new String [] {null});
    if (!dlg.clickedOk() || ls.getSelectedIndex() == -1)
      return;

    // Set the parent.

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_SKELETON, new Object [] {theMesh.getSkeleton(), theMesh.getSkeleton().duplicate()}));
    if (ls.getSelectedIndex() == 0)
      s.setJointParent(j, null);
    else
      s.setJointParent(j, options.elementAt(ls.getSelectedIndex()-1));

    // Adjust the coordinate system.

    if (j.parent != null)
    {
      Vec3 oldZdir = j.coords.getZDirection();
      Vec3 oldYdir = j.coords.getUpDirection();
      Vec3 oldXdir = oldYdir.cross(oldZdir);
      Vec3 xdir, ydir, zdir;
      zdir = j.coords.getOrigin().minus(j.parent.coords.getOrigin());
      j.length.pos = zdir.length();
      zdir.normalize();
      if (Math.abs(oldXdir.dot(zdir)) < Math.abs(oldYdir.dot(zdir)))
      {
        xdir = oldXdir.minus(zdir.times(oldXdir.dot(zdir)));
        xdir.normalize();
        ydir = zdir.cross(xdir);
      }
      else
      {
        ydir = oldYdir.minus(zdir.times(oldYdir.dot(zdir)));
        ydir.normalize();
        xdir = ydir.cross(zdir);
      }
      j.coords.setOrientation(zdir, ydir);
      j.calcAnglesFromCoords(false);
      for (int i = 0; i < j.children.length; i++)
        j.children[i].calcAnglesFromCoords(false);
    }
    else
      j.calcAnglesFromCoords(false);
    updateImage();
    updateMenus();
  }

  /** This is called by setJointParentCommand().  It identifies joints which are children of the
      specified one. */

  private void markChildJoints(Skeleton s, Joint j, boolean isChild[])
  {
    isChild[s.findJointIndex(j.id)] = true;
    for (int i = 0; i < j.children.length; i++)
      markChildJoints(s, j.children[i], isChild);
  }

  /** Allow the user to edit the selected joint. */

  public void editJointCommand()
  {
    Mesh theMesh = (Mesh) getObject().getObject();
    Skeleton s = theMesh.getSkeleton();

    if (s == null)
      return;
    Joint j = s.getJoint(((MeshViewer) theView[currentView]).getSelectedJoint());
    if (j == null)
      return;
    new JointEditorDialog(this, j.id);
    updateImage();
    updateMenus();
  }

  /** Present a window for binding the selected vertices to the skeleton. */

  public void bindSkeletonCommand()
  {
    Mesh theMesh = (Mesh) getObject().getObject();
    Skeleton s = theMesh.getSkeleton();
    if (s == null)
      return;

    // Find the selected vertices.

    int i, j, selected[] = getSelectionDistance();
    for (j = 0; j < selected.length && selected[j] != 0; j++);
    if (j == selected.length)
      return;

    // Prompt the user.

    ValueSlider blendSlider = new ValueSlider(0.0, 1.0, 100, 0.5);
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("bindPointsToSkeleton"),
      new Widget [] {blendSlider}, new String [] {Translate.text("ikWeightBlending")});
    if (!dlg.clickedOk())
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    double blend = blendSlider.getValue();

    // Find the position and axis vectors for each joint.

    Joint joint[] = s.getJoints();
    Vec3 axis[] = new Vec3 [joint.length];
    for (i = 0; i < joint.length; i++)
    {
      if (joint[i].parent == null || joint[i].length.pos == 0.0)
        continue;
      axis[i] = joint[i].coords.getZDirection();
      axis[i] = axis[i].times(1.0/axis[i].length());
    }

    // Loop over vertices and decide which joint to bind each one to.

    MeshVertex vert[] = theMesh.getVertices();
    double dist[] = new double [joint.length];
    for (i = 0; i < selected.length; i++)
    {
      if (selected[i] != 0)
        continue;
      int nearest = -1;

      // Find which bone it is nearest to.

      for (j = 0; j < joint.length; j++)
      {
        dist[j] = distToBone(vert[i].r, joint[j], axis[j]);
        if (nearest == -1 || dist[j] < dist[nearest])
          nearest = j;
      }
      if (nearest == -1)
        continue;

      // Find the secondary bone.

      int second = -1;
      if (joint[nearest].parent != null)
        second = s.findJointIndex(joint[nearest].parent.id);
      for (j = 0; j < joint[nearest].children.length; j++)
      {
        int k = s.findJointIndex(joint[nearest].children[j].id);
        if (k != -1 && (second == -1 || dist[k] < dist[second]))
          second = k;
      }

      // Select the binding parameters.

      if (second == -1)
      {
        vert[i].ikJoint = joint[nearest].id;
        vert[i].ikWeight = 1.0;
      }
      else if (joint[nearest].parent != null && joint[second].id == joint[nearest].parent.id)
      {
        vert[i].ikJoint = joint[nearest].id;
        double ratio = dist[nearest]/dist[second];
        if (ratio <= 1.0-blend)
          vert[i].ikWeight = 1.0;
        else
          vert[i].ikWeight = 0.5+0.5*(1.0-ratio)/blend;
      }
      else
      {
        double ratio = dist[nearest]/dist[second];
        if (ratio <= 1.0-blend)
          {
            vert[i].ikJoint = joint[nearest].id;
            vert[i].ikWeight = 1.0;
          }
        else
          {
            vert[i].ikJoint = joint[second].id;
            vert[i].ikWeight = 0.5-0.5*(1.0-ratio)/blend;
          }
      }
      vert[i].ikWeight = 0.001*Math.round(vert[i].ikWeight*1000.0);
    }
  }

  /** Detach points from the selected bone. */

  public void unbindSkeletonCommand()
  {
    Mesh theMesh = (Mesh) getObject().getObject();
    Skeleton s = theMesh.getSkeleton();
    if (s == null)
      return;
    Joint j = s.getJoint(((MeshViewer) theView[currentView]).getSelectedJoint());
    if (j == null)
      return;

    // Get confirmation from the user.

    BStandardDialog dlg = new BStandardDialog("", Translate.text("unbindPointsFromBone"), BStandardDialog.QUESTION);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    if (dlg.showOptionDialog(this, options, options[0]) == 1)
      return;

    // Detach the vertices.

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, theMesh.duplicate()}));
    for (MeshVertex vert : theMesh.getVertices())
      if (vert.ikJoint == j.id)
      {
        vert.ikJoint = -1;
        vert.ikWeight = 1.0;
      }
  }

  /* Calculate the distance between a vertex and a bone. */

  private double distToBone(Vec3 v, Joint j, Vec3 axis)
  {
    Vec3 end = j.coords.getOrigin();
    if (axis == null)
      return end.distance(v);
    Vec3 base = j.parent.coords.getOrigin();
    Vec3 diff = v.minus(base);
    double dot = diff.dot(axis);
    if (dot < 0.0)
      return base.distance(v);
    if (dot > j.length.pos)
      return end.distance(v);
    diff.subtract(axis.times(dot));
    return diff.length();
  }

  /** Display a window for importing a skeleton from another object. */

  protected void importSkeletonCommand()
  {
    final TreeList tree = new TreeList(this);
    tree.setPreferredSize(new Dimension(130, 100));
    tree.setAllowMultiple(false);
    tree.setUpdateEnabled(false);
    Scene theScene = getScene();
    class TreeElem extends ObjectTreeElement
    {
      public TreeElem(ObjectInfo info, TreeElement parent, TreeList tree)
      {
        super(info, parent, tree, false);
        selectable = (info != ((MeshViewer) theView[currentView]).thisObjectInScene && info.getSkeleton() != null);
        for (int i = 0; i < info.getChildren().length; i++)
          children.addElement(new TreeElem(info.getChildren()[i], this, tree));
      }
      public boolean isGray()
      {
        return !selectable;
      }
      public boolean canAcceptAsParent(TreeElement el)
      {
        return false;
      }
    };
    for (int i = 0; i < theScene.getNumObjects(); i++)
    {
      ObjectInfo info = theScene.getObject(i);
      if (info.getParent() == null)
        tree.addElement(new TreeElem(info, null, tree));
    }
    tree.setUpdateEnabled(true);
    tree.setBackground(Color.white);
    BScrollPane sp = new BScrollPane(tree, BScrollPane.SCROLLBAR_ALWAYS, BScrollPane.SCROLLBAR_ALWAYS);
    sp.getVerticalScrollBar().setUnitIncrement(10);
    sp.setForceWidth(true);
    sp.setForceHeight(true);
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("selectImportSkeleton"), new Widget [] {sp}, new String [] {null});
    if (!dlg.clickedOk() || tree.getSelectedObjects().length == 0)
      return;
    Mesh theMesh = (Mesh) getObject().getObject();
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_SKELETON, new Object [] {theMesh.getSkeleton(), theMesh.getSkeleton().duplicate()}));
    ObjectInfo info = (ObjectInfo) tree.getSelectedObjects()[0];
    theMesh.getSkeleton().addAllJoints(info.getObject().getSkeleton());
    updateImage();
    updateMenus();
  }

  /** Render a preview of the mesh. */

  public void renderPreviewCommand()
  {
    ((MeshViewer) theView[currentView]).previewObject();
  }

  /** Given a list of deltas which will be added to the selected vertices, calculate the
      corresponding deltas for the unselected vertices according to the mesh tension. */

  public abstract void adjustDeltas(Vec3 delta[]);
}
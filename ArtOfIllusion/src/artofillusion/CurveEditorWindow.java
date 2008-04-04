/* Copyright (C) 1999-2008 by Peter Eastman

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
import buoy.widget.*;

import java.awt.*;

/** The CurveEditorWindow class represents the window for editing Curve objects. */

public class CurveEditorWindow extends MeshEditorWindow implements EditingWindow
{
  protected BMenu editMenu, meshMenu, smoothMenu;
  protected BMenuItem editMenuItem[], meshMenuItem[];
  protected BCheckBoxMenuItem smoothItem[];
  protected Runnable onClose;
  private int selectionDistance[], maxDistance;
  private boolean topology;
  boolean selected[];

  public CurveEditorWindow(EditingWindow parent, String title, ObjectInfo obj, Runnable onClose, boolean allowTopology)
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
    content.add(tools = new ToolPalette(1, 7), 0, 0, new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.NONE, null, null));
    EditingTool metaTool, altTool, compoundTool;
    tools.addTool(defaultTool = new ReshapeMeshTool(this, this));
    tools.addTool(new ScaleMeshTool(this, this));
    tools.addTool(new RotateMeshTool(this, this, false));
    tools.addTool(new SkewMeshTool(this, this));
    tools.addTool(new TaperMeshTool(this, this));
    tools.addTool(compoundTool = new MoveScaleRotateMeshTool(this, this));
    if (ArtOfIllusion.getPreferences().getUseCompoundMeshTool())
      defaultTool = compoundTool;
    tools.addTool(metaTool = new MoveViewTool(this));
    tools.addTool(altTool = new RotateViewTool(this));
    tools.setDefaultTool(defaultTool);
    tools.selectTool(defaultTool);
    for (int i = 0; i < theView.length; i++)
    {
      MeshViewer view = (MeshViewer) theView[i];
      view.setMetaTool(metaTool);
      view.setAltTool(altTool);
      view.setMeshVisible(true);
      view.setScene(parent.getScene(), obj);
    }
    createEditMenu();
    createMeshMenu((Curve) obj.getObject());
    createViewMenu();
    recursivelyAddListeners(this);
    UIUtilities.applyDefaultFont(content);
    UIUtilities.applyDefaultBackground(content);
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension windowDim = new Dimension((screenBounds.width*3)/4, (screenBounds.height*3)/4);
    setBounds(new Rectangle((screenBounds.width-windowDim.width)/2, (screenBounds.height-windowDim.height)/2, windowDim.width, windowDim.height));
    tools.requestFocus();
    selected = new boolean [((Curve) obj.getObject()).getVertices().length];
    findSelectionDistance();
    updateMenus();
  }

  /** This constructor is here to let TubeEditorWindow subclass CurveEditorWindow. */

  protected CurveEditorWindow(EditingWindow parent, String title, ObjectInfo obj)
  {
    super(parent, title, obj);
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
    editMenu.add(Translate.menuItem("curveTension", this, "setTensionCommand"));
  }

  void createMeshMenu(Curve obj)
  {
    meshMenu = Translate.menu("curve");
    menubar.add(meshMenu);
    meshMenuItem = new BMenuItem [7];
    meshMenuItem[0] = Translate.menuItem("deletePoints", this, "deleteCommand");
    if (topology)
      meshMenu.add(meshMenuItem[0]);
    meshMenuItem[1] = Translate.menuItem("subdivide", this, "subdivideCommand");
    if (topology)
      meshMenu.add(meshMenuItem[1]);
    meshMenu.add(meshMenuItem[2] = Translate.menuItem("editPoints", this, "setPointsCommand"));
    meshMenu.add(meshMenuItem[3] = Translate.menuItem("transformPoints", this, "transformPointsCommand"));
    meshMenu.add(meshMenuItem[4] = Translate.menuItem("randomize", this, "randomizeCommand"));
    meshMenu.add(Translate.menuItem("centerCurve", this, "centerCommand"));
    meshMenu.addSeparator();
    meshMenu.add(meshMenuItem[5] = Translate.menuItem("smoothness", this, "setSmoothnessCommand"));
    meshMenu.add(smoothMenu = Translate.menu("smoothingMethod"));
    smoothItem = new BCheckBoxMenuItem [3];
    smoothMenu.add(smoothItem[0] = Translate.checkboxMenuItem("none", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.NO_SMOOTHING));
    smoothMenu.add(smoothItem[1] = Translate.checkboxMenuItem("interpolating", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.INTERPOLATING));
    smoothMenu.add(smoothItem[2] = Translate.checkboxMenuItem("approximating", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.APPROXIMATING));
    meshMenu.add(meshMenuItem[6] = Translate.menuItem("closedEnds", this, "toggleClosedCommand"));
    if (obj.isClosed())
      meshMenuItem[6].setText(Translate.text("menu.openEnds"));
  }

  protected BMenu createShowMenu()
  {
    BMenu menu = Translate.menu("show");
    showItem = new BCheckBoxMenuItem [4];
    menu.add(showItem[0] = Translate.checkboxMenuItem("curve", this, "shownItemChanged", true));
    menu.add(showItem[3] = Translate.checkboxMenuItem("entireScene", this, "shownItemChanged", ((MeshViewer) theView[currentView]).getSceneVisible()));
    return menu;
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
    Curve obj = (Curve) mesh;
    setObject(obj);
    if (selected.length != obj.getVertices().length)
      selected = new boolean [obj.getVertices().length];
    findSelectionDistance();
    currentTool.getWindow().updateMenus();
  }
  
  /** Get an array of flags telling which vertices are currently selected. */
  
  public boolean[] getSelection()
  {
    return selected;
  }
  
  public void setSelection(boolean sel[])
  {
    selected = sel;
    findSelectionDistance();
    updateMenus();
    updateImage();
  }

  public int[] getSelectionDistance()
  {
    if (maxDistance != getTensionDistance())
      findSelectionDistance();
    return selectionDistance;
  }
  
  /** The return value has no meaning, since there is only one selection mode in this window. */
  
  public int getSelectionMode()
  {
    return 0;
  }
  
  /** This is ignored, since there is only one selection mode in this window. */
  
  public void setSelectionMode(int mode)
  {
  }

  /** Calculate the distance (in edges) between each vertex and the nearest selected vertex. */

  void findSelectionDistance()
  {
    Curve theCurve = (Curve) getObject().getObject();
    int i, j, dist[] = new int [theCurve.getVertices().length];
    
    maxDistance = getTensionDistance();
    
    // First, set each distance to 0 or -1, depending on whether that vertex is part of the
    // current selection.
    
    for (i = 0; i < dist.length; i++)
      dist[i] = selected[i] ? 0 : -1;

    // Now extend this outward up to maxDistance.

    for (i = 0; i < maxDistance; i++)
      {
        for (j = 0; j < dist.length-1; j++)
          if (dist[j] == -1 && dist[j+1] == i)
            dist[j] = i+1;
        for (j = 1; j < dist.length; j++)
          if (dist[j] == -1 && dist[j-1] == i)
            dist[j] = i+1;
        if (theCurve.isClosed())
          {
            if (dist[0] == -1 && dist[dist.length-1] == i)
              dist[0] = i+1;
            if (dist[0] == i && dist[dist.length-1] == -1)
              dist[dist.length-1] = i+1;
          }
      }
    selectionDistance = dist;
  }

  /* EditingWindow methods. */

  public void updateMenus()
  {
    super.updateMenus();
    int i;
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i < selected.length)
    {
      editMenuItem[0].setEnabled(true);
      for (i = 0; i < 6; i++)
        meshMenuItem[i].setEnabled(true);
    }
    else
    {
      editMenuItem[0].setEnabled(false);
      for (i = 0; i < 6; i++)
        meshMenuItem[i].setEnabled(false);
    }
  }
  
  protected void doOk()
  {
    Curve theMesh = (Curve) objInfo.getObject();
    oldMesh.copyObject(theMesh);
    oldMesh = null;
    dispose();
    onClose.run();
  }
  
  protected void doCancel()
  {
    oldMesh = null;
    dispose();
  }
  
  protected void freehandModeChanged()
  {
    for (int i = 0; i < theView.length; i++)
      ((CurveViewer) theView[i]).setFreehandSelection(((BCheckBoxMenuItem) editMenuItem[2]).getState());
  }
  
  private void smoothingChanged(CommandEvent ev)
  {
    Widget source = ev.getWidget();
    if (source == smoothItem[0])
      setSmoothingMethod(Mesh.NO_SMOOTHING);
    else if (source == smoothItem[1])
      setSmoothingMethod(Mesh.INTERPOLATING);
    else if (source == smoothItem[2])
      setSmoothingMethod(Mesh.APPROXIMATING);
  }

  /** Select the entire curve. */
  
  public void selectAllCommand()
  {
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(0), selected.clone()}));
    for (int i = 0; i < selected.length; i++)
      selected[i] = true;
    setSelection(selected);
  }

  /** Extend the selection outward by one edge. */

  public void extendSelectionCommand()
  {
    int oldDist = tensionDistance;
    tensionDistance = 1;
    int dist[] = getSelectionDistance();
    boolean newSel[] = new boolean [dist.length];
    tensionDistance = oldDist;
    
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(0), selected.clone()}));
    for (int i = 0; i < dist.length; i++)
      newSel[i] = (dist[i] == 0 || dist[i] == 1);
    setSelection(newSel);
  }
  
  /** Invert the current selection. */
  
  public void invertSelectionCommand()
  {
    boolean newSel[] = new boolean [selected.length];
    for (int i = 0; i < newSel.length; i++)
      newSel[i] = !selected[i];
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(0), selected}));
    setSelection(newSel);
  }

  public void deleteCommand()
  {
    if (!topology)
      return;
    int i, j, num = 0;
    Curve theCurve = (Curve) objInfo.getObject();
    boolean newsel[];
    MeshVertex vt[] = theCurve.getVertices();
    float s[] = theCurve.getSmoothness(), news[];
    Vec3 v[];

    for (i = 0; i < selected.length; i++)
      if (selected[i])
        num++;
    if (num == 0)
      return;
    if (!theCurve.isClosed() && selected.length-num < 2)
      {
        new BStandardDialog("", Translate.text("curveNeeds2Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
        return;
      }
    if (theCurve.isClosed() && selected.length-num < 3)
      {
        new BStandardDialog("", Translate.text("curveNeeds3Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
        return;
      }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    v = new Vec3 [vt.length-num];
    news = new float [vt.length-num];
    newsel = new boolean [vt.length-num];
    for (i = 0, j = 0; i < vt.length; i++)
    {
      if (!selected[i])
      {
        newsel[j] = selected[i];
        news[j] = s[i];
        v[j++] = vt[i].r;
      }
    }
    theCurve.setShape(v, news);
    setSelection(newsel);
  }

  public void subdivideCommand()
  {
    Curve theCurve = (Curve) objInfo.getObject();
    MeshVertex vt[] = theCurve.getVertices();
    float s[] = theCurve.getSmoothness(), news[];
    boolean newsel[], split[];
    Vec3 v[], newpos[];
    int i, j, p1, p3, p4, splitcount = 0, method = theCurve.getSmoothingMethod();
    
    v = new Vec3 [vt.length];
    for (i = 0; i < vt.length; i++)
      v[i] = vt[i].r;
    
    // Determine which parts need to be subdivided.
    
    if (theCurve.isClosed())
      split = new boolean [vt.length];
    else
      split = new boolean [vt.length-1];
    for (i = 0; i < split.length; i++)
      if (selected[i] && selected[(i+1)%selected.length])
      {
        split[i] = true;
        splitcount++;
      }
    newpos = new Vec3 [vt.length+splitcount];
    news = new float [vt.length+splitcount];
    newsel = new boolean [vt.length+splitcount];
    
    // Do the subdivision.

    for (i = 0, j = 0; i < split.length; i++)
    {
      newsel[j] = selected[i];
      p1 = i-1;
      if (p1 < 0)
      {
        if (theCurve.isClosed())
          p1 = v.length-1;
        else
          p1 = 0;
      }
      if (i < v.length-1)
        p3 = i+1;
      else
      {
        if (theCurve.isClosed())
          p3 = 0;
        else
          p3 = v.length-1;
      }
      if (selected[i] && method == Mesh.APPROXIMATING)
        newpos[j] = Curve.calcApproxPoint(v, s, p1, i, p3);
      else
        newpos[j] = vt[i].r;
      if (selected[i])
        news[j] = Math.min(s[i]*2.0f, 1.0f);
      else
        news[j] = s[i];
      if (!split[i])
      {
        j++;
        continue;
      }
      if (method == Mesh.NO_SMOOTHING)
        newpos[j+1] = v[i].plus(v[p3]).times(0.5);
      else if (method == Mesh.INTERPOLATING)
      {
        if (i < v.length-2)
          p4 = i+2;
        else
        {
          if (theCurve.isClosed())
            p4 = (i+2)%v.length;
          else
            p4 = v.length-1;
        }
        newpos[j+1] = Curve.calcInterpPoint(v, s, p1, i, p3, p4);
      }
      else
        newpos[j+1] = v[i].plus(v[p3]).times(0.5);
      news[j+1] = 1.0f;
      newsel[j+1] = true;
      j += 2;
    }
    if (!theCurve.isClosed())
    {
      newpos[0] = v[0];
      newpos[j] = v[i];
      news[j] = s[i];
      newsel[j] = selected[i];
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    theCurve.setShape(newpos, news);
    setSelection(newsel);
  }

  public void setSmoothnessCommand()
  {
    final Curve theCurve = (Curve) objInfo.getObject();
    Curve oldCurve = (Curve) theCurve.duplicate();
    final MeshVertex vt[] = theCurve.getVertices();
    final float s[] = theCurve.getSmoothness();
    float value;
    final ValueSlider smoothness;
    int i;
    
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i == selected.length)
      return;
    value = 0.001f * (Math.round(s[i]*1000.0f));
    smoothness = new ValueSlider(0.0, 1.0, 100, (double) value);
    smoothness.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        float sm = (float) smoothness.getValue();
        float news[] = new float [vt.length];
        for (int i = 0; i < selected.length; i++)
          news[i] = selected[i] ? sm : s[i];
        theCurve.setSmoothness(news);
        objectChanged();
        for (int i = 0; i < theView.length; i++)
          theView[i].repaint();
      }
    } );
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("setPointSmoothness"), new Widget [] {smoothness},
            new String [] {Translate.text("Smoothness")});
    if (dlg.clickedOk())
      setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, oldCurve}));
    else
    {
      theCurve.copyObject(oldCurve);
      objectChanged();
      for (int j = 0; j < theView.length; j++)
        theView[j].repaint();
    }
  }

  void setSmoothingMethod(int method)
  {
    Curve theCurve = (Curve) objInfo.getObject();

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    for (int i = 0; i < smoothItem.length; i++)
      smoothItem[i].setState(false);
    if (method == Mesh.NO_SMOOTHING)
      smoothItem[0].setState(true);
    else if (method == Mesh.INTERPOLATING)
      smoothItem[1].setState(true);
    else
      smoothItem[2].setState(true);
    theCurve.setSmoothingMethod(method);    
    objectChanged();
    for (int i = 0; i < theView.length; i++)
      theView[i].repaint();
  }

  public void toggleClosedCommand()
  {
    Curve theCurve = (Curve) objInfo.getObject();

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    if (theCurve.isClosed())
    {
      theCurve.setClosed(false);
      meshMenuItem[6].setText(Translate.text("menu.closedEnds"));
    }
    else
    {
      theCurve.setClosed(true);
      meshMenuItem[6].setText(Translate.text("menu.openEnds"));
    }
    setMesh(theCurve);
    for (int i = 0; i < theView.length; i++)
      theView[i].repaint();
  }

  /** Given a list of deltas which will be added to the selected vertices, calculate the
      corresponding deltas for the unselected vertices according to the mesh tension. */
  
  public void adjustDeltas(Vec3 delta[])
  {
    int dist[] = getSelectionDistance(), count[] = new int [delta.length];
    Curve theCurve = (Curve) objInfo.getObject();
    int maxDistance = getTensionDistance();
    double tension = getMeshTension(), scale[] = new double [maxDistance+1];

    for (int i = 0; i < delta.length; i++)
      if (dist[i] != 0)
        delta[i].set(0.0, 0.0, 0.0);
    for (int i = 0; i < maxDistance; i++)
    {
      for (int j = 0; j < count.length; j++)
        count[j] = 0;
      for (int j = 0; j < dist.length-1; j++)
      {
        if (dist[j] == i && dist[j+1] == i+1)
        {
          count[j+1]++;
          delta[j+1].add(delta[j]);
        }
        else if (dist[j+1] == i && dist[j] == i+1)
        {
          count[j]++;
          delta[j].add(delta[j+1]);
        }
      }
      if (theCurve.isClosed())
      {
        if (dist[0] == i && dist[dist.length-1] == i+1)
        {
          count[dist.length-1]++;
          delta[dist.length-1].add(delta[0]);
        }
        else if (dist[dist.length-1] == i && dist[0] == i+1)
        {
          count[0]++;
          delta[0].add(delta[dist.length-1]);
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
/* Copyright (C) 2002-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;

/** The TubeEditorWindow class represents the window for editing Tube objects. */

public class TubeEditorWindow extends CurveEditorWindow
{
  private BCheckBoxMenuItem endsItem[];
  private boolean topology;

  public TubeEditorWindow(EditingWindow parent, String title, ObjectInfo obj, Runnable onClose, boolean allowTopology)
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
      view.setScene(parent.getScene(), obj);
    }
    createEditMenu();
    createMeshMenu((Tube) obj.getObject());
    createViewMenu();
    recursivelyAddListeners(this);
    UIUtilities.applyDefaultFont(content);
    UIUtilities.applyDefaultBackground(content);
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension windowDim = new Dimension((screenBounds.width*3)/4, (screenBounds.height*3)/4);
    setBounds(new Rectangle((screenBounds.width-windowDim.width)/2, (screenBounds.height-windowDim.height)/2, windowDim.width, windowDim.height));
    tools.requestFocus();
    selected = new boolean [((Tube) obj.getObject()).getVertices().length];
    findSelectionDistance();
    updateMenus();
  }

  void createMeshMenu(Tube obj)
  {
    meshMenu = Translate.menu("tube");
    menubar.add(meshMenu);
    meshMenuItem = new BMenuItem [8];
    meshMenuItem[0] = Translate.menuItem("deletePoints", this, "deleteCommand");
    if (topology)
      meshMenu.add(meshMenuItem[0]);
    meshMenuItem[1] = Translate.menuItem("subdivide", this, "subdivideCommand");
    if (topology)
      meshMenu.add(meshMenuItem[1]);
    meshMenu.add(meshMenuItem[2] = Translate.menuItem("editPoints", this, "setPointsCommand"));
    meshMenu.add(meshMenuItem[3] = Translate.menuItem("transformPoints", this, "transformPointsCommand"));
    meshMenu.add(meshMenuItem[4] = Translate.menuItem("randomize", this, "randomizeCommand"));
    meshMenu.add(meshMenuItem[5] = Translate.menuItem("parameters", this, "setParametersCommand"));
    meshMenu.add(meshMenuItem[6] = Translate.menuItem("thickness", this, "setThicknessCommand"));
    meshMenu.add(Translate.menuItem("centerTube", this, "centerCommand"));
    meshMenu.addSeparator();
    meshMenu.add(meshMenuItem[7] = Translate.menuItem("smoothness", this, "setSmoothnessCommand"));
    meshMenu.add(smoothMenu = Translate.menu("smoothingMethod"));
    smoothItem = new BCheckBoxMenuItem [3];
    smoothMenu.add(smoothItem[0] = Translate.checkboxMenuItem("none", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.NO_SMOOTHING));
    smoothMenu.add(smoothItem[1] = Translate.checkboxMenuItem("interpolating", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.INTERPOLATING));
    smoothMenu.add(smoothItem[2] = Translate.checkboxMenuItem("approximating", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.APPROXIMATING));
    endsItem = new BCheckBoxMenuItem [3];
    BMenu endsMenu = Translate.menu("endsStyle");
    meshMenu.add(endsMenu);
    endsMenu.add(endsItem[0] = Translate.checkboxMenuItem("openEnds", this, "endsStyleChanged", obj.getEndsStyle() == Tube.OPEN_ENDS));
    endsMenu.add(endsItem[1] = Translate.checkboxMenuItem("closedEnds", this, "endsStyleChanged", obj.getEndsStyle() == Tube.CLOSED_ENDS));
    endsMenu.add(endsItem[2] = Translate.checkboxMenuItem("flatEnds", this, "endsStyleChanged", obj.getEndsStyle() == Tube.FLAT_ENDS));
  }
  
  protected BMenu createShowMenu()
  {
    BMenu menu = Translate.menu("show");
    MeshViewer view = (MeshViewer) theView[currentView];
    showItem = new BCheckBoxMenuItem [4];
    menu.add(showItem[0] = Translate.checkboxMenuItem("curve", this, "shownItemChanged", view.getMeshVisible()));
    menu.add(showItem[1] = Translate.checkboxMenuItem("surface", this, "shownItemChanged", view.getSurfaceVisible()));
    menu.add(showItem[3] = Translate.checkboxMenuItem("entireScene", this, "shownItemChanged", view.getSceneVisible()));
    return menu;
  }

  public void updateMenus()
  {
    super.updateMenus();
    int i;
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i < selected.length)
    {
      editMenuItem[0].setEnabled(true);
      for (i = 0; i < meshMenuItem.length; i++)
        meshMenuItem[i].setEnabled(true);
    }
    else
    {
      editMenuItem[0].setEnabled(false);
      for (i = 0; i < meshMenuItem.length; i++)
        meshMenuItem[i].setEnabled(false);
    }
  }
  
  private void endsStyleChanged(WidgetEvent ev)
  {
    Widget source = ev.getWidget();
    for (int i = 0; i < endsItem.length; i++)
      if (source == endsItem[i])
      {
        for (int j = 0; j < endsItem.length; j++)
          endsItem[j].setState(false);
        endsItem[i].setState(true);
        ((Tube) objInfo.getObject()).setEndsStyle(i);
        objectChanged();
        updateImage();
      }
  }

  protected void doOk()
  {
    Tube theMesh = (Tube) objInfo.getObject();
    if (((Tube) oldMesh).getMaterial() != null)
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
        theMesh.setMaterial(((Tube) oldMesh).getMaterial(), ((Tube) oldMesh).getMaterialMapping());
    }
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

  /** Delete the selected vertices. */

  public void deleteCommand()
  {
    if (!topology)
      return;
    int i, j, num = 0;
    Tube theTube = (Tube) objInfo.getObject();
    boolean newsel[];
    MeshVertex vt[] = theTube.getVertices(), newv[];
    double t[] = theTube.getThickness(), newt[];
    float s[] = theTube.getSmoothness(), news[];

    for (i = 0; i < selected.length; i++)
      if (selected[i])
	num++;
    if (num == 0)
      return;
    if (theTube.getEndsStyle() != Tube.CLOSED_ENDS && selected.length-num < 2)
    {
      new BStandardDialog("", Translate.text("tubeNeeds2Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    if (theTube.getEndsStyle() == Tube.CLOSED_ENDS && selected.length-num < 3)
    {
      new BStandardDialog("", Translate.text("tubeNeeds3Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theTube, theTube.duplicate()}));
    newv = new MeshVertex [vt.length-num];
    newt = new double [vt.length-num];
    news = new float [vt.length-num];
    newsel = new boolean [vt.length-num];
    for (i = 0, j = 0; i < vt.length; i++)
    {
      if (!selected[i])
      {
        newsel[j] = selected[i];
        newt[j] = t[i];
        news[j] = s[i];
        newv[j++] = vt[i];
      }
    }
    theTube.setShape(newv, news, newt);
    setSelection(newsel);
    objectChanged();
    updateImage();
  }

  /** Subdivide the tube between the selected vertices. */

  public void subdivideCommand()
  {
    Tube theTube = (Tube) objInfo.getObject();
    MeshVertex vt[] = theTube.getVertices(), newpos[];
    float s[] = theTube.getSmoothness(), news[];
    double t[] = theTube.getThickness(), newt[];
    int numParam = (theTube.getParameters() == null ? 0 : theTube.getParameters().length);
    double param[][], newparam[][], paramTemp[] = new double [numParam];
    boolean newsel[], split[];
    int i, j, p1, p3, p4, splitcount = 0, method = theTube.getSmoothingMethod();
    
    // Record parameter values.
    
    param = new double [vt.length][numParam];
    ParameterValue paramValue[] = theTube.getParameterValues();
    for (i = 0; i < numParam; i++)
    {
      if (paramValue[i] instanceof VertexParameterValue)
      {
        double val[] = ((VertexParameterValue) paramValue[i]).getValue();
        for (j = 0; j < val.length; j++)
          param[j][i] = val[j];
      }
    }
    
    // Determine which parts need to be subdivided.
    
    if (theTube.getEndsStyle() == Tube.CLOSED_ENDS)
      split = new boolean [vt.length];
    else
      split = new boolean [vt.length-1];
    for (i = 0; i < split.length; i++)
      if (selected[i] && selected[(i+1)%selected.length])
      {
        split[i] = true;
        splitcount++;
      }
    newpos = new MeshVertex [vt.length+splitcount];
    news = new float [vt.length+splitcount];
    newt = new double [vt.length+splitcount];
    newparam = new double [vt.length+splitcount][numParam];
    newsel = new boolean [vt.length+splitcount];
    
    // Do the subdivision.

    for (i = 0, j = 0; i < split.length; i++)
    {
      newsel[j] = selected[i];
      p1 = i-1;
      if (p1 < 0)
      {
        if (theTube.getEndsStyle() == Tube.CLOSED_ENDS)
          p1 = vt.length-1;
        else
          p1 = 0;
      }
      if (i < vt.length-1)
        p3 = i+1;
      else
      {
        if (theTube.getEndsStyle() == Tube.CLOSED_ENDS)
          p3 = 0;
        else
          p3 = vt.length-1;
      }
      if (selected[i] && method == Mesh.APPROXIMATING)
      {
        newpos[j] = SplineMesh.calcApproxPoint(vt, s, param, paramTemp, p1, i, p3);
        newt[j] = Tube.calcApproxThickness(t, s, p1, i, p3);
        for (int k = 0; k < numParam; k++)
          newparam[j][k] = paramTemp[k];
      }
      else
      {
        newpos[j] = vt[i];
        newt[j] = t[i];
        newparam[j] = param[i];
      }
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
      {
        newpos[j+1] = MeshVertex.blend(vt[i], vt[p3], 0.5, 0.5);
        for (int k = 0; k < numParam; k++)
          newparam[j+1][k] = 0.5*(param[i][k]+param[p3][k]);
      }
      else if (method == Mesh.INTERPOLATING)
      {
        if (i < vt.length-2)
          p4 = i+2;
        else
        {
          if (theTube.getEndsStyle() == Tube.CLOSED_ENDS)
            p4 = (i+2)%vt.length;
          else
            p4 = vt.length-1;
        }
        newpos[j+1] = SplineMesh.calcInterpPoint(vt, s, param, paramTemp, p1, i, p3, p4);
        newt[j+1] = Tube.calcInterpThickness(t, s, p1, i, p3, p4);
        for (int k = 0; k < numParam; k++)
          newparam[j+1][k] = paramTemp[k];
      }
      else
      {
        newpos[j+1] = MeshVertex.blend(vt[i], vt[p3], 0.5, 0.5);
        newt[j+1] = 0.5*(t[i]+t[p3]);
        for (int k = 0; k < numParam; k++)
          newparam[j+1][k] = 0.5*(param[i][k]+param[p3][k]);
      }
      news[j+1] = 1.0f;
      newsel[j+1] = true;
      j += 2;
    }
    if (theTube.getEndsStyle() != Tube.CLOSED_ENDS)
    {
      newpos[0] = vt[0];
      newpos[j] = vt[i];
      newt[0] = t[0];
      newt[j] = t[i];
      news[j] = s[i];
      newparam[0] = param[0];
      newparam[j] = param[i];
      newsel[j] = selected[i];
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theTube, theTube.duplicate()}));
    theTube.setShape(newpos, news, newt);
    for (i = 0; i < numParam; i++)
    {
      if (paramValue[i] instanceof VertexParameterValue)
      {
        double val[] = new double [newpos.length];
        for (j = 0; j < val.length; j++)
          val[j] = newparam[j][i];
        paramValue[i] = new VertexParameterValue(val);
      }
    }
    theTube.setParameterValues(paramValue);
    setSelection(newsel);
    objectChanged();
    updateImage();
  }
  
  /** Allow the user to set the thickness for selected vertices. */
  
  public void setThicknessCommand()
  {
    Tube theTube = (Tube) objInfo.getObject();
    double thickness[] = theTube.getThickness(), initial = -1;
    int selectDist[] = getSelectionDistance();

    for (int i = 0; i < selectDist.length; i++)
      if (selectDist[i] == 0)
      {
        if (initial == -1)
          initial = thickness[i];
        else if (initial != thickness[i])
          initial = Double.NaN;
      }
    if (initial == -1)
      return;  // No selected vertices.
    ValueField thicknessField = new ValueField(initial, ValueField.NONNEGATIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("setThicknessTitle"),
      new Widget [] {thicknessField}, new String [] {Translate.text("Thickness")});
    if (!dlg.clickedOk() || Double.isNaN(thicknessField.getValue()))
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theTube, theTube.duplicate()}));
    for (int i = 0; i < selectDist.length; i++)
      if (selectDist[i] == 0)
        thickness[i] = thicknessField.getValue();
    theTube.setThickness(thickness);
    objectChanged();
    updateImage();
  }
}
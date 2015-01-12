/* Copyright (C) 1999-2015 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.material.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;
import java.lang.reflect.*;
import java.util.List;

/** This class implements the dialog box which is used to choose textures for objects. 
    It presents a list of all available textures from which the user can select one.
    If only one object is being editing, it also allows the user to edit the texture mapping
    for that object. */

public class ObjectTextureDialog extends BDialog implements ListChangeListener
{
  private LayoutWindow window;
  private Scene scene;
  private ObjectInfo obj[], editObj;
  private BTabbedPane tabs;
  private BList texList, matList, layerList;
  private BButton texMapButton, matMapButton, addLayerButton, deleteLayerButton, moveUpButton, moveDownButton, editTexturesButton;
  private BComboBox newTextureChoice, newMaterialChoice;
  private BorderContainer content, texturesTab, materialsTab;
  private FormContainer texTitlePanel, texListPanel, matListPanel, layerPanel, paramsPanel;
  private MaterialPreviewer preview;
  private BComboBox typeChoice, blendChoice, paramTypeChoice[];
  private ValueSelector paramValueWidget[];
  private int fieldParamIndex[];
  private BScrollPane paramsScroller;
  private Runnable callback;
  private Texture oldTexture;
  private TextureMapping oldTexMapping;
  Material oldMaterial;
  MaterialMapping oldMatMapping;
  private LayeredTexture layeredTex;
  private LayeredMapping layeredMap;
  private ActionProcessor renderProcessor;
  
  private static final int CONSTANT_PARAM = 0;
  private static final int VERTEX_PARAM = 1;
  private static final int FACE_PARAM = 2;
  private static final int FACE_VERTEX_PARAM = 3;

  private static final int PREVIEW_SIZE = 300;
  
  private static final String PARAM_TYPE_NAME[] = new String [] {
      Translate.text("Object"),
      Translate.text("Vertex"),
      Translate.text("Face"),
      Translate.text("Face-Vertex")
    };


  public ObjectTextureDialog(LayoutWindow parent, ObjectInfo objects[])
  {
    this(parent, objects, true, true);
  }

  public ObjectTextureDialog(LayoutWindow parent, ObjectInfo objects[], boolean includeTextures, boolean includeMaterials)
  {
    super(parent, Translate.text("objectTextureTitle"), false);
    
    window = parent;
    scene = parent.getScene();
    obj = objects;
    renderProcessor = new ActionProcessor();
    editObj = obj[0].duplicate();
    editObj.setObject(editObj.object.duplicate());
    oldTexture = editObj.getObject().getTexture();
    oldTexMapping = editObj.getObject().getTextureMapping();
    if (oldTexture instanceof LayeredTexture)
    {
      layeredMap = (LayeredMapping) oldTexMapping;
      layeredTex = (LayeredTexture) oldTexture;;
    }
    else
    {
      layeredTex = new LayeredTexture(editObj.getObject());
      layeredMap = (LayeredMapping) layeredTex.getDefaultMapping(editObj.getObject());
    }
    oldMaterial =  editObj.getObject().getMaterial();
    if (oldMaterial == null)
      oldMatMapping = null;
    else
      oldMatMapping =  editObj.getObject().getMaterialMapping().duplicate();
    tabs = new BTabbedPane();
    texturesTab = new BorderContainer();
    if (includeTextures)
      tabs.add(texturesTab, Translate.text("Texture"));
    materialsTab = new BorderContainer();
    if (includeMaterials)
      tabs.add(materialsTab, Translate.text("Material"));

    // Add the title and combo box at the top.
    
    content = new BorderContainer();
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.add(tabs, BorderContainer.CENTER);
    texTitlePanel = new FormContainer(1, 2);
    String title;
    if (obj.length == 1)
      title = Translate.text("chooseTextureForSingle", obj[0].getName());
    else
      title = Translate.text("chooseTextureForMultiple");
    texTitlePanel.add(new BLabel(title), 0, 0);
    RowContainer typeRow = new RowContainer();
    typeRow.add(Translate.label("Type"));
    typeRow.add(typeChoice = new BComboBox(new String [] {
      Translate.text("simpleTexture"),
      Translate.text("layeredTexture")
    }));
    typeChoice.setSelectedIndex((oldTexture instanceof LayeredTexture) ? 1 : 0);
    typeChoice.addEventLink(ValueChangedEvent.class, this, "typeChanged");
    if (obj.length == 1)
      texTitlePanel.add(typeRow, 0, 1);

    // Create the list of textures and the buttons under it.

    texList = new BList();
    texList.setMultipleSelectionEnabled(false);
    texList.addEventLink(SelectionChangedEvent.class, this, "textureSelectionChanged");
    texList.addEventLink(MouseClickedEvent.class, this, "textureClicked");
    texListPanel = new FormContainer(new double [] {1.0}, new double [] {1.0, 0.0});
    texListPanel.add(UIUtilities.createScrollingList(texList), 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    RowContainer texButtonRow = new RowContainer();
    texButtonRow.add(texMapButton = Translate.button("editMapping", this, "doEditTextureMapping"));
    texButtonRow.add(newTextureChoice = new BComboBox());
    newTextureChoice.add(Translate.text("button.newTexture"));
    java.util.List<Texture> textureTypes = PluginRegistry.getPlugins(Texture.class);
    for (Texture texture : textureTypes)
    {
      try
      {
        Method mtd = texture.getClass().getMethod("getTypeName", null);
        newTextureChoice.add(mtd.invoke(null, null));
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    newTextureChoice.addEventLink(ValueChangedEvent.class, this, "doNewTexture");
    texListPanel.add(texButtonRow, 0, 1);

    // Create the list of materials and the buttons under it.

    matList = new BList();
    matList.setMultipleSelectionEnabled(false);
    matList.addEventLink(SelectionChangedEvent.class, this, "materialSelectionChanged");
    matList.addEventLink(MouseClickedEvent.class, this, "materialClicked");
    matListPanel = new FormContainer(new double [] {1.0}, new double [] {0.0, 1.0, 0.0});
    if (obj.length == 1)
      title = Translate.text("chooseMaterialForSingle", obj[0].getName());
    else
      title = Translate.text("chooseMaterialForMultiple");
    matListPanel.add(new BLabel(title), 0, 0);
    matListPanel.add(UIUtilities.createScrollingList(matList), 0, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    RowContainer matButtonRow = new RowContainer();
    matButtonRow.add(matMapButton = Translate.button("editMapping", this, "doEditMaterialMapping"));
    matButtonRow.add(newMaterialChoice = new BComboBox());
    newMaterialChoice.add(Translate.text("button.newMaterial"));
    java.util.List<Material> materialTypes = PluginRegistry.getPlugins(Material.class);
    for (Material material : materialTypes)
    {
      try
      {
        Method mtd = material.getClass().getMethod("getTypeName", null);
        newMaterialChoice.add(mtd.invoke(null, null));
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    newMaterialChoice.addEventLink(ValueChangedEvent.class, this, "doNewMaterial");
    matListPanel.add(matButtonRow, 0, 2);
    materialsTab.add(matListPanel, BorderContainer.CENTER);

    // Create the section of the window for layered textures.
    
    layerPanel = new FormContainer(new double [] {1.0, 1.0}, new double [] {0.0, 0.0, 0.0, 0.0, 1.0});
    layerPanel.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null));
    layerPanel.add(addLayerButton = Translate.button("add", " >>", this, "doAddLayer"), 0, 0);
    layerPanel.add(deleteLayerButton = Translate.button("delete", this, "doDeleteLayer"), 0, 1);
    layerPanel.add(moveUpButton = Translate.button("moveUp", this, "doMoveLayerUp"), 0, 2);
    layerPanel.add(moveDownButton = Translate.button("moveDown", this, "doMoveLayerDown"), 0, 3);
    layerList = new BList() {
      public Dimension getPreferredSize()
      {
        return new Dimension(texList.getPreferredSize().width, super.getPreferredSize().height);
      }
    };
    layerList.setMultipleSelectionEnabled(false);
    layerList.addEventLink(SelectionChangedEvent.class, this, "textureSelectionChanged");
    for (int i = 0; i < layeredMap.getLayers().length; i++)
      layerList.add((layeredMap.getLayers())[i].getName());
    layerPanel.add(UIUtilities.createScrollingList(layerList), 1, 0, 1, 4, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    RowContainer blendRow = new RowContainer();
    layerPanel.add(blendRow, 0, 4, 2, 1);
    blendRow.add(Translate.label("blendingMode"));
    blendChoice = new BComboBox(new String [] {
      Translate.text("blend"),
      Translate.text("overlay"),
      Translate.text("overlayBumpsAdd"),
    });
    blendChoice.addEventLink(ValueChangedEvent.class, this, "blendTypeChanged");
    blendRow.add(blendChoice);
   
    // Create the material previewer.
    
    if (oldTexture instanceof LayeredTexture)
    {
      preview = new MaterialPreviewer(layeredTex, editObj.getObject().getMaterial(), PREVIEW_SIZE, PREVIEW_SIZE);
      preview.setTexture(layeredTex, layeredMap);
    }
    else
    {
      preview = new MaterialPreviewer(oldTexture, editObj.getObject().getMaterial(), PREVIEW_SIZE, PREVIEW_SIZE);
      preview.setTexture(oldTexture, oldTexMapping);
    }
    preview.setMaximumSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
    preview.setMaterial(editObj.getObject().getMaterial(), editObj.getObject().getMaterialMapping());
    updatePreviewParameterValues();
    content.add(preview, BorderContainer.EAST);

    // Add the buttons at the bottom.

    RowContainer buttons = new RowContainer();
    content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
    buttons.add(editTexturesButton = Translate.button("texturesAndMaterials", this, "doEditTextures"));
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "doCancel"));

    // Create the parameters panel.
    
    paramsPanel = new FormContainer(1, 2);
    paramsPanel.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    paramsPanel.add(Translate.label("textureParameters"), 0, 0);
    paramsPanel.add(BOutline.createBevelBorder(paramsScroller = new BScrollPane(), false), 0, 1);
    paramsScroller.setPreferredViewSize(new Dimension(300, 80));
    paramsScroller.getVerticalScrollBar().setUnitIncrement(10);
    buildParamList();

    // Show the dialog.

    buildLists();
    if (oldTexture instanceof LayeredTexture)
      layoutLayered();
    else
      layoutSimple();
    pack();
    setResizable(false);
    addEventLink(WindowClosingEvent.class, this, "dispose");
    UIUtilities.centerDialog(this, parent);
    updateComponents();
    scene.addTextureListener(this);
    scene.addMaterialListener(this);
    setVisible(true);
  }

  /**
   * Set the values of texture parameters on the preview object.
   */

  private void updatePreviewParameterValues()
  {
    double paramAvgVal[] = editObj.getObject().getAverageParameterValues();
    ParameterValue paramValue[] = new ParameterValue [paramAvgVal.length];
    for (int i = 0; i < paramValue.length; i++)
      paramValue[i] = new ConstantParameterValue(paramAvgVal[i]);
    preview.getObject().getObject().setParameterValues(paramValue);
  }

  public void dispose()
  {
    scene.removeTextureListener(this);
    scene.removeMaterialListener(this);
    renderProcessor.stopProcessing();
    super.dispose();
  }
  
  /** Set a callback to be executed when the window is closed. */
  
  public void setCallback(Runnable cb)
  {
    callback = cb;
  }
  
  /** Layout the content panel for a simple texture. */
  
  private void layoutSimple()
  {
    texturesTab.remove(BorderContainer.CENTER);
    FormContainer center = new FormContainer(2, 3);
    center.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    center.add(texTitlePanel, 0, 0, 2, 1);
    center.add(texListPanel, 0, 1);
    center.add(paramsPanel, 0, 2, 2, 1);
    texturesTab.add(center, BorderContainer.CENTER);
  }
  
  /** Layout the content panel for a layered texture. */
  
  private void layoutLayered()
  {
    texturesTab.remove(BorderContainer.CENTER);
    FormContainer center = new FormContainer(new double [] {1.0, 1.0, 0.0}, new double [] {0.0, 1.0, 1.0});
    center.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    center.add(texTitlePanel, 0, 0, 3, 1);
    center.add(texListPanel, 0, 1);
    center.add(layerPanel, 1, 1, 2, 1);
    center.add(paramsPanel, 0, 2, 2, 1);
    texturesTab.add(center, BorderContainer.CENTER);
  }
  
  /** Build the lists of all available textures and materials. */
  
  private void buildLists()
  {
    texList.removeAll();
    for (int i = 0; i < scene.getNumTextures(); i++)
    {
      texList.add((scene.getTexture(i)).getName());
      if (editObj.getObject().getTexture() == scene.getTexture(i))
        texList.setSelected(i, true);
    }
    matList.removeAll();
    matList.add(Translate.text("none"));
    boolean foundMaterial = false;
    for (int i = 0; i < scene.getNumMaterials(); i++)
    {
      matList.add(scene.getMaterial(i).getName());
      if (editObj.getObject().getMaterial() == scene.getMaterial(i))
      {
        matList.setSelected(i+1, true);
        foundMaterial = true;
      }
    }
    if (!foundMaterial)
      matList.setSelected(0, true);
  }
  
  /** Build the list of texture parameters. */
  
  private void buildParamList()
  {
    // Find a list of all parameters, both for the entire texture and for the selected layer.
    
    TextureParameter params[];
    Object3D obj = editObj.getObject();
    while (obj instanceof ObjectWrapper)
      obj = ((ObjectWrapper) obj).getWrappedObject();
    if (obj.getTexture() instanceof LayeredTexture)
    {
      int index = layerList.getSelectedIndex();
      if (index == -1)
        params = new TextureParameter [0];
      else
        params = ((LayeredMapping) obj.getTextureMapping()).getLayerParameters(index);
    }
    else
      params = obj.getParameters();
    paramTypeChoice = new BComboBox [params.length];
    paramValueWidget = new ValueSelector [params.length];
    fieldParamIndex = new int [params.length];
    FormContainer paramsContainer = new FormContainer(3, params.length);
    paramsContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
    LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE);
    final TextureParameter texParam[] = obj.getParameters();
    final ParameterValue paramValue[] = obj.getParameterValues();
    for (int i = 0; i < params.length; i++)
    {
      if (params[i].type != TextureParameter.NORMAL_PARAMETER)
        continue;
      boolean perObject = true;
      double val = params[i].defaultVal;
      int j;
      for (j = 0; j < texParam.length; j++)
        if (params[i].equals(texParam[j]))
        {
          val = paramValue[j].getAverageValue();
          perObject = (paramValue[j] instanceof ConstantParameterValue);
          break;
        }
      final int whichParam = j, whichField = i;
      fieldParamIndex[i] = j;
      paramsContainer.add(new BLabel(params[i].name), 0, i, labelLayout);
      paramsContainer.add(paramTypeChoice[i] = new BComboBox(), 1, i);
      paramTypeChoice[i].add(PARAM_TYPE_NAME[0]);
      if (obj instanceof Mesh)
        paramTypeChoice[i].add(PARAM_TYPE_NAME[1]);
      if (obj instanceof FacetedMesh)
        paramTypeChoice[i].add(PARAM_TYPE_NAME[2]);
      if (obj instanceof FacetedMesh)
        paramTypeChoice[i].add(PARAM_TYPE_NAME[3]);
      if (whichParam < paramValue.length)
        paramTypeChoice[i].setSelectedValue(PARAM_TYPE_NAME[parameterTypeCode(paramValue[whichParam].getClass())]);
      paramTypeChoice[i].addEventLink(ValueChangedEvent.class, this, "paramTypeChanged");
      paramsContainer.add(paramValueWidget[i] = new ValueSelector(val, params[i].minVal, params[i].maxVal, 0.005), 2, i);
      paramValueWidget[i].setEnabled(perObject);
      paramValueWidget[i].addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
          if (whichParam >= paramValue.length)
            return;
          double value = paramValueWidget[whichField].getValue();
          ParameterValue val = new ConstantParameterValue(value);
          editObj.getObject().setParameterValue(texParam[whichParam], val);
          preview.getObject().getObject().setParameterValue(texParam[whichParam], val);
          renderPreview();
        }
      });
    }
    paramsScroller.setContent(paramsContainer);
    UIUtilities.applyBackground(paramsContainer, null);
    paramsScroller.layoutChildren();
  }
  
  private int parameterTypeCode(Class type)
  {
    if (type == VertexParameterValue.class)
      return VERTEX_PARAM;
    if (type == FaceParameterValue.class)
      return FACE_PARAM;
    if (type == FaceVertexParameterValue.class)
      return FACE_VERTEX_PARAM;
    return CONSTANT_PARAM;
  }
  
  private void textureClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() == 2)
    {
      int which = texList.getSelectedIndex();
      Texture tex = scene.getTexture(which);
      tex.edit(window, scene);
      scene.changeTexture(which);
      renderPreview();
    }
  }

  private void materialClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() == 2)
    {
      int which = matList.getSelectedIndex()-1;
      if (which > -1)
      {
        Material mat = scene.getMaterial(which);
        mat.edit(window, scene);
        scene.changeMaterial(which);
        preview.render();
      }
    }
  }

  private void doEditTextureMapping()
  {
    int index = layerList.getSelectedIndex();
    new TextureMappingDialog(window, editObj.getObject(), index);
    preview.cancelRendering();
    editObj.setTexture(editObj.getObject().getTexture(), editObj.getObject().getTextureMapping());
    preview.setTexture(editObj.getObject().getTexture(), editObj.getObject().getTextureMapping());
    updatePreviewParameterValues();
    renderPreview();
  }

  private void doEditMaterialMapping()
  {
    new MaterialMappingDialog(window, editObj.getObject());
    preview.setMaterial(editObj.getObject().getMaterial(), editObj.getObject().getMaterialMapping());
    preview.render();
  }

  private void doNewTexture()
  {
    int index = newTextureChoice.getSelectedIndex();
    if (index == 0)
      return;
    newTextureChoice.setSelectedIndex(0);
    List<Texture> textureTypes = PluginRegistry.getPlugins(Texture.class);
    try
    {
      Texture tex = textureTypes.get(index-1).getClass().newInstance();
      int j = 0;
      String name = "";
      do
      {
        j++;
        name = "Untitled "+j;
      } while (scene.getTexture(name) != null);
      tex.setName(name);
      scene.addTexture(tex);
      tex.edit(window, scene);
      boolean layered = (typeChoice.getSelectedIndex() == 1);
      if (!layered)
      {
        texList.setSelected(scene.indexOf(tex), true);
        textureSelectionChanged(new SelectionChangedEvent(texList));
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
  
  private void doNewMaterial()
  {
    int index = newMaterialChoice.getSelectedIndex();
    if (index == 0)
      return;
    newMaterialChoice.setSelectedIndex(0);
    List<Material> materialTypes = PluginRegistry.getPlugins(Material.class);
    try
    {
      Material mat = materialTypes.get(index-1).getClass().newInstance();
      int j = 0;
      String name = "";
      do
      {
        j++;
        name = "Untitled "+j;
      } while (scene.getMaterial(name) != null);
      mat.setName(name);
      scene.addMaterial(mat);
      mat.edit(window, scene);
      matList.setSelected(scene.indexOf(mat), true);
      materialSelectionChanged();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  private void doEditTextures()
  {
    scene.showTexturesDialog(window);
  }
  
  private void doAddLayer()
  {
    preview.cancelRendering();
    Texture tex = scene.getTexture(texList.getSelectedIndex());
    layeredMap.addLayer(0, tex, tex.getDefaultMapping(editObj.getObject()), LayeredMapping.BLEND);
    layerList.add(0, tex.getName());
    layerList.setSelected(0, true);
    resetParameters();
    updateComponents();
  }
  
  private void doDeleteLayer()
  {
    int index = layerList.getSelectedIndex();
    preview.cancelRendering();
    layeredMap.deleteLayer(index);
    layerList.remove(index);
    resetParameters();
    updateComponents();
  }
  
  private void doMoveLayerUp()
  {
    int index = layerList.getSelectedIndex();
    preview.cancelRendering();
    String label = (String) layerList.getItem(index);
    layeredMap.moveLayer(index, index-1);
    layerList.remove(index);
    layerList.add(index-1, label);
    layerList.setSelected(index-1, true);
    resetParameters();
    updateComponents();
  }

  private void doMoveLayerDown()
  {
    int index = layerList.getSelectedIndex();
    preview.cancelRendering();
    String label = (String) layerList.getItem(index);
    layeredMap.moveLayer(index, index+1);
    layerList.remove(index);
    layerList.add(index+1, label);
    layerList.setSelected(index+1, true);
    resetParameters();
    updateComponents();
  }
  
  private void doOk()
  {
    TextureParameter param[] = editObj.getObject().getParameters();
    ParameterValue paramValue[] = editObj.getObject().getParameterValues();
    UndoRecord undo = new UndoRecord(window, false);
    for (int i = 0; i < obj.length; i++)
    {
      undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {obj[i].getObject(), obj[i].getObject().duplicate()});
      if (editObj.getObject().getTexture() instanceof LayeredTexture)
      {
        LayeredMapping m = (LayeredMapping) editObj.getObject().getTextureMapping().duplicate(obj[i].getObject(), editObj.getObject().getTexture());
        obj[i].setTexture(new LayeredTexture(m), m);
      }
      else
        obj[i].setTexture(editObj.getObject().getTexture(), editObj.getObject().getTextureMapping().duplicate());
      for (int j = 0; j < param.length; j++)
        obj[i].getObject().setParameterValue(param[j], paramValue[j].duplicate());
    }
    if (editObj.getObject().getMaterial() == null)
      for (int i = 1; i < obj.length; i++)
        obj[i].getObject().setMaterial(null, null);
    else
      for (int i = 1; i < obj.length; i++)
        if (obj[i].getObject().canSetMaterial())
          obj[i].getObject().setMaterial(editObj.getObject().getMaterial(), editObj.getObject().getMaterialMapping().duplicate());
    obj[0].getObject().copyObject(editObj.getObject());
    window.setUndoRecord(undo);
    window.updateImage();
    window.getScore().tracksModified(false);
    if (callback != null)
      callback.run();
    dispose();
  }
  
  private void doCancel()
  {
    dispose();
    if (callback != null)
      callback.run();
  }
  
  private void paramTypeChanged(ValueChangedEvent ev)
  {
    Widget src = ev.getWidget();
    for (int i = 0; i < paramTypeChoice.length; i++)
      if (src == paramTypeChoice[i])
      {
        // Change whether the parameter is set globally, per-vertex, per-face, etc.
        
        String type = (String) paramTypeChoice[i].getSelectedValue();
        int index = fieldParamIndex[i];
        if (paramValueWidget[i] != null)
          paramValueWidget[i].setEnabled(type == PARAM_TYPE_NAME[CONSTANT_PARAM]);
        TextureParameter param = editObj.getObject().getParameters()[index];
        Object3D realObject = editObj.getObject();
        while (realObject instanceof ObjectWrapper)
          realObject = ((ObjectWrapper) realObject).getWrappedObject();
        if (type == PARAM_TYPE_NAME[CONSTANT_PARAM])
          editObj.getObject().setParameterValue(param, new ConstantParameterValue(editObj.getObject().getParameterValues()[index].getAverageValue()));
        else if (type == PARAM_TYPE_NAME[VERTEX_PARAM])
          editObj.getObject().setParameterValue(param, new VertexParameterValue((Mesh) realObject, param));
        else if (type == PARAM_TYPE_NAME[FACE_PARAM])
          editObj.getObject().setParameterValue(param, new FaceParameterValue((FacetedMesh) realObject, param));
        else if (type == PARAM_TYPE_NAME[FACE_VERTEX_PARAM])
          editObj.getObject().setParameterValue(param, new FaceVertexParameterValue((FacetedMesh) realObject, param));
        renderPreview();
        return;
      }
  }
  
  private void typeChanged()
  {
    boolean layered = (typeChoice.getSelectedIndex() == 1);
    if (!layered)
    {
      texList.setSelected(0, true);
      Texture tex = scene.getDefaultTexture();
      editObj.setTexture(tex, tex.getDefaultMapping(editObj.getObject()));
      updateComponents();
      layoutSimple();
      pack();
      UIUtilities.centerDialog(this, window);
      resetParameters();
    }
    else
    {
      preview.cancelRendering();
      if (layeredMap.getNumLayers() == 0)
      {
        // Give the layered texture a single layer identical to the current simple texture.

        Texture tex = editObj.getObject().getTexture();
        TextureMapping map = editObj.getObject().getTextureMapping();
        layeredMap.addLayer(0, tex, map.duplicate(editObj.getObject(), tex), LayeredMapping.BLEND);
        layerList.add(0, tex.getName());
        layerList.setSelected(0, true);
        ParameterValue values[] = editObj.getObject().getParameterValues();
        editObj.setTexture(layeredTex, layeredMap);
        TextureParameter params[] = layeredMap.getLayerParameters(0);
        for (int i = 0; i < values.length; i++)
          editObj.getObject().setParameterValue(params[i+1], values[i]);
      }
      else
        editObj.setTexture(layeredTex, layeredMap);
      preview.setTexture(layeredTex, layeredMap);
      updatePreviewParameterValues();
      updateComponents();
      layoutLayered();
      pack();
      UIUtilities.centerDialog(this, window);
      resetParameters();
      return;
    }
  }
  
  private void blendTypeChanged()
  {
    layeredMap.setLayerMode(layerList.getSelectedIndex(), blendChoice.getSelectedIndex());
    renderPreview();
  }
  
  private void textureSelectionChanged(SelectionChangedEvent ev)
  {
    boolean layered = (typeChoice.getSelectedIndex() == 1);
    boolean anyselection;
    if (layered)
      anyselection = (layerList.getSelectedIndex() > -1);
    else
      anyselection = (texList.getSelectedIndex() > -1);
    if (!anyselection)
    {
      updateComponents();
      return;
    }
    preview.cancelRendering();
    if (ev.getWidget() != layerList)
    {
      Texture tex;
      if (layered)
        tex = layeredMap.getLayer(layerList.getSelectedIndex());
      else
        tex = scene.getTexture(texList.getSelectedIndex());
      if (!layered)
      {
        if (tex == oldTexture)
          editObj.setTexture(tex, oldTexMapping.duplicate());
        else
          editObj.setTexture(tex, tex.getDefaultMapping(editObj.getObject()));
        preview.setTexture(tex, editObj.getObject().getTextureMapping());
      }
    }
    updateComponents();
    renderPreview();
  }

  private void materialSelectionChanged()
  {
    if (!editObj.getObject().canSetMaterial())
      return;
    if (matList.getSelectedIndex() < 0)
      matList.setSelected(0, true);
    if (matList.getSelectedIndex() == 0)
    {
      matMapButton.setEnabled(false);
      editObj.getObject().setMaterial(null, null);
      preview.setMaterial(null, null);
      preview.render();
      return;
    }
    Material mat = scene.getMaterial(matList.getSelectedIndex()-1);
    if (mat == oldMaterial)
      editObj.getObject().setMaterial(mat, oldMatMapping.duplicate());
    else
      editObj.getObject().setMaterial(mat, mat.getDefaultMapping(editObj.getObject()));
    matMapButton.setEnabled(!(mat instanceof UniformMaterial));
    preview.setMaterial(mat, editObj.getObject().getMaterialMapping());
    preview.render();
  }

  /* Utility routine.  This should be called whenever the list of texture parameters
     might have changed.  It ensures that all parameter lists are correctly updated,
     then re-renders the preview. */
  
  private void resetParameters()
  {
    preview.cancelRendering();
    editObj.setTexture(editObj.getObject().getTexture(), editObj.getObject().getTextureMapping());
    preview.getObject().setTexture(preview.getObject().getObject().getTexture(), preview.getObject().getObject().getTextureMapping());
    renderPreview();
  }

  // Update the status of various components.
  
  private void updateComponents()
  {
    boolean layered = (typeChoice.getSelectedIndex() == 1);
    boolean anyselection = (texList.getSelectedIndex() > -1);

    buildParamList();
    if (layered)
    {
      addLayerButton.setEnabled(anyselection);
      int index = layerList.getSelectedIndex();
      if (index == -1)
      {
        texMapButton.setEnabled(false);
        deleteLayerButton.setEnabled(false);
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);
        blendChoice.setEnabled(false);
        return;
      }
      texMapButton.setEnabled(true);
      deleteLayerButton.setEnabled(true);
      moveUpButton.setEnabled(index > 0);
      moveDownButton.setEnabled(index < layeredMap.getLayers().length-1);
      blendChoice.setEnabled(true);
      blendChoice.setSelectedIndex(layeredMap.getLayerMode(index));
    }
    else if (anyselection)
      texMapButton.setEnabled(true);
    else
      texMapButton.setEnabled(false);
  }
  
  /* ListChangeListener methods. */
  
  public void itemAdded(int index, Object obj)
  {
    if (obj instanceof Texture)
    {
      Texture tex = (Texture) obj;
      texList.add(index, tex.getName());
    }
    else
    {
      Material mat = (Material) obj;
      matList.add(index+1, mat.getName());
    }
  }
  
  public void itemRemoved(int index, Object obj)
  {
    if (obj instanceof Texture)
    {
      Texture tex = (Texture) obj;
      texList.remove(index);
      preview.cancelRendering();
      if (editObj.getObject().getTextureMapping() instanceof LayeredMapping)
      {
        Texture layers[] = layeredMap.getLayers();
        for (int i = layers.length-1; i >= 0; i--)
          if (layers[i] == tex)
          {
            layeredMap.deleteLayer(i);
            layerList.remove(i);
          }
        renderPreview();
        updateComponents();
      }
      else if (editObj.getObject().getTexture() == tex)
      {
        editObj.setTexture(scene.getDefaultTexture(), scene.getDefaultTexture().getDefaultMapping(editObj.getObject()));
        preview.setTexture(editObj.getObject().getTexture(), editObj.getObject().getTextureMapping());
        renderPreview();
        updateComponents();
      }
    }
    else
    {
      Material mat = (Material) obj;
      matList.remove(index+1);
      if (editObj.getObject().getMaterial() == mat)
      {
        editObj.getObject().setMaterial(null, null);
        preview.setMaterial(editObj.getObject().getMaterial(), editObj.getObject().getMaterialMapping());
        preview.render();
        matMapButton.setEnabled(false);
      }
    }
  }
  
  public void itemChanged(int index, Object obj)
  {
    if (obj instanceof Texture)
    {
      Texture tex = (Texture) obj;
      texList.replace(index, tex.getName());
    }
    else
    {
      Material mat = (Material) obj;
      matList.replace(index+1, mat.getName());
    }
  }
  
  /**
   * Rerender the preview image after the texture has changed.
   */
  
  private void renderPreview()
  {
    renderProcessor.addEvent(new Runnable()
    {
      public void run()
      {
        preview.render();
      }
    });
  }
}
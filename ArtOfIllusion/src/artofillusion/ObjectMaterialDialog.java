/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.material.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;

/** This class implements the dialog box which is used to choose materials for objects. 
    It presents a list of all available materials from which the user can select one.
    If only one object is being editing, it also allows the user to edit the material mapping
    for that object. */

public class ObjectMaterialDialog extends BDialog implements ListChangeListener
{
  LayoutWindow window;
  Scene scene;
  ObjectInfo obj[];
  Object3D firstObj;
  BList matList;
  BButton mapButton;
  MaterialPreviewer preview;
  Material oldMaterial;
  MaterialMapping oldMapping;

  public ObjectMaterialDialog(LayoutWindow parent, ObjectInfo objects[])
  {
    super(parent, Translate.text("objectMaterialTitle"), false);
    
    window = parent;
    scene = parent.getScene();
    obj = objects;
    firstObj = obj[0].getObject();
    oldMaterial = firstObj.getMaterial();
    if (oldMaterial == null)
      oldMapping = null;
    else
      oldMapping = firstObj.getMaterialMapping().duplicate();

    // Add the various components to the dialog.
    
    FormContainer content = new FormContainer(3, 4);
    setContent(content);
    String title;
    if (obj.length == 1)
      title = Translate.text("chooseMaterialForSingle", obj[0].getName());
    else
      title = Translate.text("chooseMaterialForMultiple");
    content.add(new BLabel(title), 0, 0, 3, 1);
    matList = new BList();
    matList.setMultipleSelectionEnabled(false);
    buildList();
    matList.addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
    matList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    content.add(UIUtilities.createScrollingList(matList), 0, 1, 2, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    LayoutInfo buttonLayout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(2, 2, 2, 2), null);
    content.add(Translate.button("newMaterial", this, "doNewMaterial"), 0, 2, buttonLayout);
    content.add(Translate.button("materials", this, "doEditMaterials"), 1, 2, buttonLayout);
    content.add(preview = new MaterialPreviewer(firstObj.getTexture(), oldMaterial, 160, 160), 2, 1, 1, 2);
    preview.setTexture(firstObj.getTexture(), firstObj.getTextureMapping());
    preview.setMaterial(oldMaterial, oldMapping);
    preview.render();

    // Add the buttons at the bottom.

    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 3, 3, 1);
    buttons.add(mapButton = Translate.button("editMapping", this, "doEditMapping"));
    mapButton.setEnabled(!(oldMaterial instanceof UniformMaterial) && oldMaterial != null);
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "doCancel"));

    // Show the dialog.

    pack();
    setResizable(false);
    addEventLink(WindowClosingEvent.class, this, "dispose");
    UIUtilities.centerDialog(this, parent.getFrame());
    scene.addMaterialListener(this);
    setVisible(true);
  }

  public void dispose()
  {
    scene.removeMaterialListener(this);
    super.dispose();
  }
  
  private void buildList()
  {
    matList.removeAll();
    matList.add("None");
    matList.setSelected(0, true);
    for (int i = 0; i < scene.getNumMaterials(); i++)
    {
      matList.add(scene.getMaterial(i).getName());
      if (firstObj.getMaterial() == scene.getMaterial(i))
        matList.setSelected(i+1, true);
    }
  }
  
  private void mouseClicked(MouseClickedEvent ev)
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
  
  private void doOk()
  {
    UndoRecord undo = new UndoRecord(window, false);
    for (int i = 0; i < obj.length; i++)
      undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {obj[i].getObject(), obj[i].getObject().duplicate()});
    if (firstObj.getMaterial() == null)
      for (int i = 1; i < obj.length; i++)
        obj[i].getObject().setMaterial(null, null);
    else
      for (int i = 1; i < obj.length; i++)
        obj[i].getObject().setMaterial(firstObj.getMaterial(), firstObj.getMaterialMapping().duplicate());
    window.setUndoRecord(undo);
    dispose();
  }
  
  private void doCancel()
  {
    firstObj.setMaterial(oldMaterial, oldMapping);
    dispose();
  }
  
  private void doNewMaterial()
  {
    Material mat = MaterialsDialog.showNewMaterialWindow(this, scene);
    matList.setSelected(scene.indexOf(mat), true);
    selectionChanged();
  }
  
  private void doEditMaterials()
  {
    scene.showMaterialsDialog(window);
    buildList();
    preview.render();
  }
  
  private void doEditMapping()
  {
    new MaterialMappingDialog(window, firstObj);
    preview.setMaterial(firstObj.getMaterial(), firstObj.getMaterialMapping());
    preview.render();
  }

  private void selectionChanged()
  {
    if (matList.getSelectedIndex() < 0)
      matList.setSelected(0, true);
    if (matList.getSelectedIndex() == 0)
    {
      mapButton.setEnabled(false);
      firstObj.setMaterial(null, null);
      preview.setMaterial(null, null);
      preview.render();
      return;
    }
    Material mat = scene.getMaterial(matList.getSelectedIndex()-1);
    if (mat == oldMaterial)
      firstObj.setMaterial(mat, oldMapping.duplicate());
    else
      firstObj.setMaterial(mat, mat.getDefaultMapping(firstObj));
    mapButton.setEnabled(!(mat instanceof UniformMaterial));
    preview.setMaterial(mat, firstObj.getMaterialMapping());
    preview.render();
  }
  
  /* ListChangeListener methods. */
  
  public void itemAdded(int index, Object obj)
  {
    Material mat = (Material) obj;
    matList.add(index+1, mat.getName());
  }
  
  public void itemRemoved(int index, Object obj)
  {
    Material mat = (Material) obj;
    
    matList.remove(index+1);
    if (firstObj.getMaterial() == mat)
    {
      firstObj.setMaterial(null, null);
      preview.setMaterial(firstObj.getMaterial(), firstObj.getMaterialMapping());
      preview.render();
      mapButton.setEnabled(false);
    }
  }
  
  public void itemChanged(int index, Object obj)
  {
    Material mat = (Material) obj;
    
    matList.replace(index+1, mat.getName());
  }
}
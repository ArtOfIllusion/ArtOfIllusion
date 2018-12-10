/* Copyright (C) 2000-2004 by Peter Eastman
   Changes copyright (C) 2017-2018 by Maksim Khramov

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
import java.awt.Insets;
import java.lang.reflect.*;
import java.util.*;

/** This class implements the dialog box which is used to choose material mappings for objects.
    It presents a list of all mappings which can be used with the current object and material,
    and allows the user to select one. */

public class MaterialMappingDialog extends BDialog
{
  private Object3D obj;
  private List<MaterialMapping> mappings;
  private BComboBox mapChoice;
  private MaterialPreviewer preview;
  private MaterialMapping map, oldMapping;
  private Widget editingPanel;

  /** Create a dialog for editing the material mapping for a particular object. */

  public MaterialMappingDialog(BFrame parent, Object3D obj)
  {
    super(parent, "Material Mapping", true);

    this.obj = obj;
    map = obj.getMaterialMapping();
    oldMapping = map.duplicate();

    // Make a list of all material mappings which can be used for this object and material.

    mappings = new ArrayList<>();
    Material mat = obj.getMaterial();
    
    for (MaterialMapping mapping: PluginRegistry.getPlugins(MaterialMapping.class))
    {
      if(mapping.legalMapping(obj, mat)) mappings.add(mapping);
    }

    // Add the various components to the dialog.

    FormContainer content = new FormContainer(new double [] {1}, new double [] {1, 0, 0, 0});
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.add(preview = new MaterialPreviewer(obj.getTexture(), obj.getMaterial(), obj.duplicate(), 160, 160), 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, new Insets(0, 50, 0, 50), null));
    preview.setMaterial(obj.getMaterial(), obj.getMaterialMapping());
    RowContainer choiceRow = new RowContainer();
    content.add(choiceRow, 0, 1);
    choiceRow.add(new BLabel(Translate.text("Mapping")+":"));
    choiceRow.add(mapChoice = new BComboBox());
    for (int i = 0; i < mappings.size(); i++)
    {
      MaterialMapping cmap = mappings.get(i);
      mapChoice.add(cmap.getName());
      if (cmap.getClass() == map.getClass())
      {
        mapChoice.setSelectedIndex(i);
      }
    }
    mapChoice.addEventLink(ValueChangedEvent.class, this, "mappingChanged");
    content.add(editingPanel = map.getEditingPanel(obj, preview), 0, 2);

    // Add the buttons at the bottom.

    RowContainer row = new RowContainer();
    content.add(row, 0, 3);
    row.add(Translate.button("ok", this, "dispose"));
    row.add(Translate.button("cancel", this, "doCancel"));

    // Show the dialog.

    pack();
    UIUtilities.centerDialog(this, parent);
    setVisible(true);
  }

  private void doCancel()
  {
    setMapping(oldMapping);
    dispose();
  }

  private void mappingChanged()
  {
    try
    {
      MaterialMapping selection = mappings.get(mapChoice.getSelectedIndex());
      if (selection.getClass() == map.getClass())
      {
        return;
      }
      Constructor<?> con = selection.getClass().getConstructor(Material.class);
      Material mat =  obj.getMaterial();
      setMapping((MaterialMapping) con.newInstance(new Object [] {mat}));
      FormContainer content = (FormContainer) getContent();
      content.remove(editingPanel);
      content.add(editingPanel = map.getEditingPanel(obj, preview), 0, 2, 2, 1);
      pack();
      preview.render();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  /** Set the mapping for the object being edited. */

  private void setMapping(MaterialMapping newmap)
  {
    map = newmap;
    obj.setMaterial(obj.getMaterial(), newmap);
    preview.setMaterial(obj.getMaterial(), newmap);
    preview.render();
  }
}

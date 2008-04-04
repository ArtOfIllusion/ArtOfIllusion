/* Copyright (C) 2000,2002-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

/** This class implements the dialog box which is used to choose texture mappings for objects. 
    It presents a list of all mappings which can be used with the current object and material,
    and allows the user to select one. */

public class TextureMappingDialog extends BDialog
{
  private BFrame fr;
  private FormContainer content;
  private Object3D origObj, editObj;
  private Vector mappings;
  private BComboBox mapChoice;
  private MaterialPreviewer preview;
  private TextureMapping map, oldMapping;
  private Widget editingPanel;
  private boolean layered;
  private int layer;

  /** Create a dialog for editing the texture mapping for a particular object.  If the object
      has a layered texture, then layer is the layer number to edit.  Otherwise, layer is
      ignored. */

  public TextureMappingDialog(BFrame parent, Object3D obj, int layer)
  {
    super(parent, "Texture Mapping", true);
    
    fr = parent;
    editObj = obj.duplicate();
    origObj = obj;
    this.layer = layer;
    map = editObj.getTextureMapping();
    layered = (map instanceof LayeredMapping);
    if (layered)
      map = ((LayeredMapping) map).getLayerMapping(layer);
    oldMapping = map.duplicate();
    
    // Make a list of all texture mappings which can be used for this object and texture.
    
    mappings = new Vector();
    List<TextureMapping> allMappings = PluginRegistry.getPlugins(TextureMapping.class);
    for (int i = 0; i < allMappings.size(); i++)
    {
      try
      {
        Method mtd = allMappings.get(i).getClass().getMethod("legalMapping", Object3D.class, Texture.class);
        Texture tex = layered ? ((LayeredMapping) editObj.getTextureMapping()).getLayer(layer) 
            : editObj.getTexture();
        Boolean result = (Boolean) mtd.invoke(null, editObj, tex);
        if (result.booleanValue())
          mappings.addElement(allMappings.get(i).getClass());
      }
      catch (Exception ex)
      {
      }
    }

    // Add the various components to the dialog.
    
    content = new FormContainer(new double [] {1}, new double [] {1, 0, 0, 0});
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    Object3D previewObj = editObj;
    while (previewObj instanceof ObjectWrapper)
      previewObj = ((ObjectWrapper) previewObj).getWrappedObject();
    previewObj = previewObj.duplicate();
    Texture tex = (layered ? map.getTexture() : obj.getTexture());
    content.add(preview = new MaterialPreviewer(tex, obj.getMaterial(), obj.duplicate(), 160, 160), 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, new Insets(0, 50, 0, 50), null));
    setPreviewMapping(map);
    RowContainer choiceRow = new RowContainer();
    content.add(choiceRow, 0, 1);
    choiceRow.add(new BLabel(Translate.text("Mapping")+":"));
    choiceRow.add(mapChoice = new BComboBox());
    for (int i = 0; i < mappings.size(); i++)
    {
      try
      {
        Method mtd = ((Class) mappings.elementAt(i)).getMethod("getName", null);
        mapChoice.add((String) mtd.invoke(null, null));
        if (mappings.elementAt(i) == map.getClass())
          mapChoice.setSelectedIndex(i);
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    mapChoice.addEventLink(ValueChangedEvent.class, this, "mappingChanged");
    content.add(editingPanel = map.getEditingPanel(editObj, preview), 0, 2);

    // Add the buttons at the bottom.

    RowContainer row = new RowContainer();
    content.add(row, 0, 3);
    row.add(Translate.button("ok", this, "doOk"));
    row.add(Translate.button("cancel", this, "dispose"));

    // Show the dialog.

    pack();
    UIUtilities.centerDialog(this, parent);
    setVisible(true);
  }
  
  private void doOk()
  {
    editObj.setTexture(editObj.getTexture(), editObj.getTextureMapping());
    origObj.copyTextureAndMaterial(editObj);
    dispose();
  }

  private void mappingChanged()
  {
    try
    {
      Class cls = (Class) mappings.elementAt(mapChoice.getSelectedIndex());
      if (cls == map.getClass())
        return;
      Constructor con = cls.getConstructor(new Class [] {Object3D.class, Texture.class});
      Texture tex = layered ? ((LayeredMapping) editObj.getTextureMapping()).getLayer(layer) 
          : editObj.getTexture();
      setMapping((TextureMapping) con.newInstance(new Object [] {editObj, tex}));
      content.remove(editingPanel);
      content.add(editingPanel = map.getEditingPanel(editObj, preview), 0, 2);
      pack();
      preview.render();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
  
  /** Set the mapping for the object being edited. */
  
  private void setMapping(TextureMapping newmap)
  {
    map = newmap;
    Vec2 uv[] = null, uvf[][] = null;
    if (newmap instanceof UVMapping)
    {
      // Record the current texture coordinates of each vertex.
      
      Mapping2D oldmap;
      if (layered)
        oldmap = (Mapping2D) ((LayeredMapping) editObj.getTextureMapping()).getLayerMapping(layer);
      else
        oldmap = (Mapping2D) editObj.getTextureMapping();
      Object3D innerObj = editObj;
      while (innerObj instanceof ObjectWrapper)
        innerObj = ((ObjectWrapper) innerObj).getWrappedObject();
      Mesh m = (Mesh) innerObj;
      if (m instanceof FacetedMesh && oldmap instanceof UVMapping && ((UVMapping) oldmap).isPerFaceVertex((FacetedMesh) m))
        uvf = ((UVMapping) oldmap).findFaceTextureCoordinates((FacetedMesh) m);
      else
        uv = oldmap.findTextureCoordinates(m);
    }
    if (layered)
    {
      LayeredMapping lm = (LayeredMapping) editObj.getTextureMapping();
      lm.setLayerMapping(layer, newmap);
      editObj.setTexture(lm.getTexture(), lm);
    }
    else
    {
      editObj.setTexture(editObj.getTexture(), newmap);
      setPreviewMapping(newmap);
    }
    if (uv != null)
    {
      // Set the texture coordinates at each vertex.
      
      ((UVMapping) newmap).setTextureCoordinates(editObj, uv);
      setPreviewMapping(newmap);
    }
    if (uvf != null)
    {
      // Set the texture coordinates at each face-vertex.
      
      ((UVMapping) newmap).setFaceTextureCoordinates(editObj, uvf);
      setPreviewMapping(newmap);
    }
    preview.render();
  }
  
  /** Set the texture mapping for the preview and, if necessary, copy over
      texture coordinates. */
  
  public void setPreviewMapping(TextureMapping newmap)
  {
    Texture tex = (layered ? map.getTexture() : editObj.getTexture());
    preview.setTexture(tex, newmap);
    Object3D mesh = editObj;
    while (mesh instanceof ObjectWrapper)
      mesh = ((ObjectWrapper) mesh).getWrappedObject();
    if (!(mesh instanceof Mesh))
      return;
    TextureParameter param[] = mesh.getParameters();
    ParameterValue val[] = mesh.getParameterValues();
    Object3D previewObj = preview.getObject().getObject();
    for (int i = 0; i < param.length; i++)
      previewObj.setParameterValue(param[i], val[i]);
    preview.render();
  }
}
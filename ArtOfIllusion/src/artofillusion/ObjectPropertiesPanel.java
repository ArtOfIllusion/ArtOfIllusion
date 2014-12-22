/* Copyright (C) 2006-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import buoy.widget.*;
import buoy.event.*;
import artofillusion.ui.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.material.*;

import javax.swing.*;
import java.lang.reflect.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * This is a panel which displays information about the currently selected objects, and allows them
 * to be edited.
 */

public class ObjectPropertiesPanel extends ColumnContainer
{
  private LayoutWindow window;
  private BTextField nameField;
  private ValueField xPosField, yPosField, zPosField, xRotField, yRotField, zRotField;
  private BComboBox textureChoice, materialChoice;
  private PropertyEditor propEditor[];
  private ObjectInfo objects[];
  private Property properties[];
  private Object3D previousObjects[];
  private boolean ignoreNextChange;
  private Widget lastEventSource;
  private ActionProcessor paramChangeProcessor;

  public ObjectPropertiesPanel(LayoutWindow window)
  {
    this.window = window;
    nameField = new BTextField();
    xPosField = new ValueField(0.0, ValueField.NONE, 1);
    yPosField = new ValueField(0.0, ValueField.NONE, 1);
    zPosField = new ValueField(0.0, ValueField.NONE, 1);
    xRotField = new ValueField(0.0, ValueField.NONE, 1);
    yRotField = new ValueField(0.0, ValueField.NONE, 1);
    zRotField = new ValueField(0.0, ValueField.NONE, 1);
    textureChoice = new BComboBox();
    materialChoice = new BComboBox();
    paramChangeProcessor = new ActionProcessor();
    rebuildContents();
    window.addEventLink(SceneChangedEvent.class, this, "rebuildContents");
    nameField.addEventLink(FocusLostEvent.class, this, "nameChanged");
    nameField.addEventLink(KeyPressedEvent.class, this, "nameChanged");
    xPosField.addEventLink(ValueChangedEvent.class, this, "coordinatesChanged");
    yPosField.addEventLink(ValueChangedEvent.class, this, "coordinatesChanged");
    zPosField.addEventLink(ValueChangedEvent.class, this, "coordinatesChanged");
    xRotField.addEventLink(ValueChangedEvent.class, this, "coordinatesChanged");
    yRotField.addEventLink(ValueChangedEvent.class, this, "coordinatesChanged");
    zRotField.addEventLink(ValueChangedEvent.class, this, "coordinatesChanged");
    textureChoice.addEventLink(ValueChangedEvent.class, this, "textureChanged");
    materialChoice.addEventLink(ValueChangedEvent.class, this, "materialChanged");
    ListChangeListener listener = new ListChangeListener()
    {
      public void itemAdded(int index, Object obj)
      {
        rebuildContents();
      }
      public void itemRemoved(int index, Object obj)
      {
        rebuildContents();
      }
      public void itemChanged(int index, Object obj)
      {
        rebuildContents();
      }
    };
    window.getScene().addTextureListener(listener);
    window.getScene().addMaterialListener(listener);
  }

  /**
   * Rebuild the contents of the panel.
   */

  protected void rebuildContents()
  {
    if (ignoreNextChange)
    {
      ignoreNextChange = false;
      return;
    }
    if (lastEventSource == nameField)
      nameChanged(new FocusLostEvent(nameField, false)); // Commit name changes.

    // Find the selected objects.

    Scene scene = window.getScene();
    int sel[] = window.getSelectedIndices();
    objects = new ObjectInfo [sel.length];
    for (int i = 0; i < sel.length; i++)
      objects[i] = scene.getObject(sel[i]);
    boolean objectsChanged = (previousObjects == null || objects.length != previousObjects.length);
    for (int i = 0; i < objects.length && !objectsChanged; i++)
      objectsChanged |= (objects[i].getObject() != previousObjects[i]);
    if (objectsChanged)
    {
      previousObjects = new Object3D[objects.length];
      for (int i = 0; i < objects.length; i++)
        previousObjects[i] = objects[i].getObject();
    }

    // If nothing is selected, just place a message in the panel.

    if (objects.length == 0)
    {
      if (objectsChanged)
      {
        removeAll();
        add(Translate.label("noObjectsSelected"));
        UIUtilities.applyDefaultBackground(this);
        UIUtilities.applyDefaultFont(this);
        if (getParent() != null)
          getParent().layoutChildren();
        repaint();
      }
      return;
    }

    // Set the name.

    if (objects.length == 1)
      nameField.setText(objects[0].getName());

    // Set the position and orientation.

    Vec3 origin = objects[0].getCoords().getOrigin();
    xPosField.setValue(origin.x);
    yPosField.setValue(origin.y);
    zPosField.setValue(origin.z);
    double angles[] = objects[0].getCoords().getRotationAngles();
    xRotField.setValue(angles[0]);
    yRotField.setValue(angles[1]);
    zRotField.setValue(angles[2]);
    for (int i = 1; i < objects.length; i++)
    {
      origin = objects[i].getCoords().getOrigin();
      checkFieldValue(xPosField, origin.x);
      checkFieldValue(yPosField, origin.y);
      checkFieldValue(zPosField, origin.z);
      angles = objects[i].getCoords().getRotationAngles();
      checkFieldValue(xRotField, angles[0]);
      checkFieldValue(yRotField, angles[1]);
      checkFieldValue(zRotField, angles[2]);
    }

    // Set the texture.

    Texture tex = objects[0].getObject().getTexture();
    boolean canSetTexture = objects[0].getObject().canSetTexture();
    boolean sameTexture = true;
    for (int i = 1; i < objects.length; i++)
    {
      Texture thisTex = objects[i].getObject().getTexture();
      if (thisTex != tex)
        sameTexture = false;
      canSetTexture &= objects[i].getObject().canSetTexture();
    }
    if (canSetTexture)
    {
      Vector<String> names = new Vector<String>();
      int selected = -1;
      for (int i = 0; i < scene.getNumTextures(); i++)
      {
        names.add(scene.getTexture(i).getName());
        if (scene.getTexture(i) == tex)
          selected = i;
      }
      if (!sameTexture)
      {
        selected = names.size();
        names.add("");
      }
      else if (tex instanceof LayeredTexture)
      {
        selected = names.size();
        names.add(Translate.text("layeredTexture"));
      }
      List<Texture> textureTypes = PluginRegistry.getPlugins(Texture.class);
      for (Texture texture : textureTypes)
      {
        try
        {
          Method mtd = texture.getClass().getMethod("getTypeName", null);
          names.add(Translate.text("newTextureOfType", mtd.invoke(null, null)));
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
      textureChoice.setModel(new DefaultComboBoxModel(names));
      textureChoice.setSelectedIndex(selected);
    }

    // Set the material.

    Material mat = objects[0].getObject().getMaterial();
    boolean canSetMaterial = objects[0].getObject().canSetMaterial();
    boolean sameMaterial = true;
    for (int i = 1; i < objects.length; i++)
    {
      Material thisMat = objects[i].getObject().getMaterial();
      if (thisMat != mat)
        sameMaterial = false;
      canSetMaterial &= objects[i].getObject().canSetMaterial();
    }
    if (canSetMaterial)
    {
      Vector<String> names = new Vector<String>();
      int selected = -1;
      for (int i = 0; i < scene.getNumMaterials(); i++)
      {
        names.add(scene.getMaterial(i).getName());
        if (scene.getMaterial(i) == mat)
          selected = i;
      }
      if (!sameMaterial)
      {
        selected = names.size();
        names.add("");
      }
      else if (mat == null)
        selected = names.size();
      names.add(Translate.text("none"));
      List<Material> materialTypes = PluginRegistry.getPlugins(Material.class);
      for (Material material : materialTypes)
      {
        try
        {
          Method mtd = material.getClass().getMethod("getTypeName", null);
          names.add(Translate.text("newMaterialOfType", mtd.invoke(null, null)));
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
      materialChoice.setModel(new DefaultComboBoxModel(names));
      materialChoice.setSelectedIndex(selected);
    }

    // See whether the list of properties has changed.

    Property oldProperties[] = properties;
    findProperties();
    boolean propertiesChanged = (oldProperties == null || properties.length != oldProperties.length);
    for (int i = 0; i < properties.length && !propertiesChanged; i++)
      propertiesChanged = !properties[i].equals(oldProperties[i]);

    // Rebuild the panel contents.

    if (!objectsChanged && !propertiesChanged)
    {
      showParameterValues();
      return;
    }
    lastEventSource = null;
    removeAll();
    LayoutInfo fillLayout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null);
    if (objects.length == 1)
    {
      add(Translate.label("Name"));
      add(nameField, fillLayout);
    }
    add(Translate.label("Position"));
    FormContainer positions = new FormContainer(new double [] {0, 1, 0, 1, 0, 1}, new double [1]);
    positions.add(new BLabel("X"), 0, 0);
    positions.add(xPosField, 1, 0, fillLayout);
    positions.add(new BLabel(" Y"), 2, 0);
    positions.add(yPosField, 3, 0, fillLayout);
    positions.add(new BLabel(" Z"), 4, 0);
    positions.add(zPosField, 5, 0, fillLayout);
    add(positions, fillLayout);
    add(Translate.label("Orientation"));
    FormContainer orientation = new FormContainer(new double [] {0, 1, 0, 1, 0, 1}, new double [1]);
    orientation.add(new BLabel("X"), 0, 0);
    orientation.add(xRotField, 1, 0, fillLayout);
    orientation.add(new BLabel(" Y"), 2, 0);
    orientation.add(yRotField, 3, 0, fillLayout);
    orientation.add(new BLabel(" Z"), 4, 0);
    orientation.add(zRotField, 5, 0, fillLayout);
    add(orientation, fillLayout);
    if (canSetTexture)
    {
      add(Translate.label("Texture"));
      add(textureChoice, fillLayout);
    }
    if (canSetMaterial)
    {
      add(Translate.label("Material"));
      add(materialChoice, fillLayout);
    }

    // Build widgets for object parameters.

    LayoutInfo centerLayout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(2, 2, 2, 2), null);
    propEditor = new PropertyEditor[properties.length];
    for (int i = 0; i < propEditor.length; i++)
    {
      propEditor[i] = new PropertyEditor(properties[i], null);
      if (propEditor[i].getLabel() != null)
        add(new BLabel(propEditor[i].getLabel()));
      Widget widget = propEditor[i].getWidget();
      widget.addEventLink(ValueChangedEvent.class, this, "parameterChanged");
      if (widget instanceof ValueSelector || widget instanceof BCheckBox || widget instanceof ValueField)
        add(widget, centerLayout);
      else
        add(widget, fillLayout);
    }
    showParameterValues();

    // Layout and display the panel.

    UIUtilities.applyDefaultBackground(this);
    UIUtilities.applyDefaultFont(this);
    if (getParent() != null)
      getParent().layoutChildren();
    repaint();
  }

  private void checkFieldValue(ValueField field, double value)
  {
    if (field.getValue() != value)
      field.setValue(Double.NaN);
  }

  /**
   * Find the list of object properties to display.
   */

  private void findProperties()
  {
    properties = objects[0].getObject().getProperties();
    for (int i = 1; i < objects.length; i++)
    {
      Property otherProperty[] = objects[i].getObject().getProperties();
      boolean same = (properties.length == otherProperty.length);
      for (int j = 0; j < properties.length && same; j++)
        if (!properties[j].equals(otherProperty[j]))
          same = false;
      if (!same)
      {
        properties = new Property[0];
        return;
      }
    }
  }

  /**
   * Update all the ValueSelectors to show the current values of object parameters.
   */

  private void showParameterValues()
  {
    if (propEditor == null)
      return;
    Object values[] = new Object [propEditor.length];
    for (int i = 0; i < values.length; i++)
      values[i] = objects[0].getObject().getPropertyValue(i);
    for (int i = 1; i < objects.length; i++)
    {
      for (int j = 0; j < values.length; j++)
        if (values[j] != null && !values[j].equals(objects[i].getObject().getPropertyValue(j)))
          values[j] = null;
    }
    for (int i = 0; i < propEditor.length; i++)
    {
      propEditor[i].setValue(values[i]);
    }
  }

  /**
   * This is called when the value in any of the position or orientiation fields is changed.
   */

  private void coordinatesChanged(ValueChangedEvent ev)
  {
    UndoRecord undo = null;
    if (ev.getWidget() != lastEventSource)
    {
      ignoreNextChange = true;
      undo = new UndoRecord(window, false);
      for (int i = 0; i < objects.length; i++)
        undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {objects[i].getCoords(), objects[i].getCoords().duplicate()});
    }
    for (int i = 0; i < objects.length; i++)
    {
      CoordinateSystem coords = objects[i].getCoords();
      Vec3 origin = coords.getOrigin();
      origin.x = getNewValue(origin.x, xPosField.getValue());
      origin.y = getNewValue(origin.y, yPosField.getValue());
      origin.z = getNewValue(origin.z, zPosField.getValue());
      coords.setOrigin(origin);
      double angles[] = coords.getRotationAngles();
      angles[0] = getNewValue(angles[0], xRotField.getValue());
      angles[1] = getNewValue(angles[1], yRotField.getValue());
      angles[2] = getNewValue(angles[2], zRotField.getValue());
      coords.setOrientation(angles[0], angles[1], angles[2]);
    }
    window.getScene().applyTracksAfterModification(Arrays.asList(objects));
    lastEventSource = ev.getWidget();
    if (undo != null)
      window.setUndoRecord(undo);
    else
      window.setModified();
    window.updateImage();
  }

  private double getNewValue(double oldval, double newval)
  {
    return (Double.isNaN(newval) ? oldval : newval);
  }

  /**
   * This is called when an event occurs that might result in the name being changed.
   */

  private void nameChanged(WidgetEvent ev)
  {
    lastEventSource = ev.getWidget();
    if (ev instanceof KeyPressedEvent && ((KeyPressedEvent) ev).getKeyCode() != KeyEvent.VK_ENTER)
      return;
    if (objects.length == 0 || objects[0].getName().equals(nameField.getText()))
      return;
    int which = window.getScene().indexOf(objects[0]);
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.RENAME_OBJECT, new Object [] {new Integer(which), objects[0].getName()}));
    window.setObjectName(which, nameField.getText());
    if (ev instanceof KeyPressedEvent)
      window.getView().requestFocus(); // This is where they'll probably expect it to go
  }

  /**
   * This is called when the texture is changed.
   */

  private void textureChanged()
  {
    int index = textureChoice.getSelectedIndex();
    Scene scene = window.getScene();
    Texture tex = null;
    if (index < scene.getNumTextures())
      tex = scene.getTexture(index);
    else
    {
      List<Texture> textureTypes = PluginRegistry.getPlugins(Texture.class);
      if (index < scene.getNumTextures()+textureTypes.size())
      {
        try
        {
          tex = textureTypes.get(index-scene.getNumTextures()).getClass().newInstance();
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
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
    }
    if (tex != null)
    {
      UndoRecord undo = new UndoRecord(window, false);
      for (int i = 0; i < objects.length; i++)
        if (objects[i].getObject().getTexture() != tex)
        {
          undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {objects[i].getObject(), objects[i].getObject().duplicate()});
          objects[i].setTexture(tex, tex.getDefaultMapping(objects[i].getObject()));
        }
      window.setUndoRecord(undo);
      window.updateImage();
      window.getScore().tracksModified(false);
    }
  }

  /**
   * This is called when the material is changed.
   */

  private void materialChanged()
  {
    int index = materialChoice.getSelectedIndex();
    Scene scene = window.getScene();
    Material mat = null;
    boolean noMaterial = (index == scene.getNumMaterials());
    if (index < scene.getNumMaterials())
    {
      mat = scene.getMaterial(index);
    }
    else if (index > scene.getNumMaterials())
    {
      List<Material> materialTypes = PluginRegistry.getPlugins(Material.class);
      try
      {
        mat = materialTypes.get(index-scene.getNumMaterials()-1).getClass().newInstance();
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
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    if (noMaterial || mat != null)
    {
      UndoRecord undo = new UndoRecord(window, false);
      for (int i = 0; i < objects.length; i++)
        if (objects[i].getObject().getMaterial() != mat)
        {
          undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {objects[i].getObject(), objects[i].getObject().duplicate()});
          objects[i].setMaterial(mat, noMaterial ? null : mat.getDefaultMapping(objects[i].getObject()));
        }
      window.setUndoRecord(undo);
      window.updateImage();
      window.getScore().tracksModified(false);
    }
  }

  /**
   * This is called when an object parameter is changed.
   */

  private void parameterChanged(final ValueChangedEvent ev)
  {
    Runnable r = new Runnable() {
      public void run()
      {
        processParameterChange(ev);
      }
    };
    if (ev.isInProgress())
      paramChangeProcessor.addEvent(r);
    else
      EventQueue.invokeLater(r); // Ensure that it won't be discarded.
  }


  private void processParameterChange(ValueChangedEvent ev)
  {
    UndoRecord undo = null;
    if (ev.getWidget() != lastEventSource)
    {
      ignoreNextChange = true;
      undo = new UndoRecord(window, false);
      for (int i = 0; i < objects.length; i++)
        undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {objects[i].getObject(), objects[i].getObject().duplicate()});
    }
    boolean changed = false;
    for (int i = 0; i < objects.length; i++)
    {
      for (int j = 0; j < propEditor.length; j++)
        if (propEditor[j].getWidget() == ev.getSource())
        {
          Object value = propEditor[j].getValue();
          if (!objects[i].getObject().getPropertyValue(j).equals(value))
          {
            objects[i].getObject().setPropertyValue(j, value);
            changed = true;
          }
        }
      window.getScene().objectModified(objects[i].getObject());
    }
    lastEventSource = ev.getWidget();
    if (undo != null && changed)
      window.setUndoRecord(undo);
    else if (!ev.isInProgress() && changed)
      window.setModified();
    window.updateImage();
    window.getScore().tracksModified(false);
  }

  /**
   * Always use a reasonable size, regardless of the current selection.
   */

  public Dimension getPreferredSize()
  {
    Dimension pref = super.getPreferredSize();
    return new Dimension(Math.max(pref.width, 180), Math.max(pref.height, 200));
  }

  /**
   * Allow the panel to be completely hidden.
   */

  public Dimension getMinimumSize()
  {
    return new Dimension();
  }
}

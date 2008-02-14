/* Copyright (C) 2006 by Peter Eastman

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
import java.util.*;
import java.awt.*;
import java.awt.event.*;

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
  private Widget propSelector[];
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
    int sel[] = scene.getSelection();
    objects = new ObjectInfo [sel.length];
    for (int i = 0; i < sel.length; i++)
      objects[i] = scene.getObject(sel[i]);
    boolean objectsChanged = (previousObjects == null || objects.length != previousObjects.length);
    for (int i = 0; i < objects.length && !objectsChanged; i++)
      objectsChanged |= (objects[i].object != previousObjects[i]);
    if (objectsChanged)
    {
      previousObjects = new Object3D[objects.length];
      for (int i = 0; i < objects.length; i++)
        previousObjects[i] = objects[i].object;
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
        layoutChildren();
        repaint();
      }
      return;
    }

    // Set the name.

    if (objects.length == 1)
      nameField.setText(objects[0].name);

    // Set the position and orientation.

    Vec3 origin = objects[0].coords.getOrigin();
    xPosField.setValue(origin.x);
    yPosField.setValue(origin.y);
    zPosField.setValue(origin.z);
    double angles[] = objects[0].coords.getRotationAngles();
    xRotField.setValue(angles[0]);
    yRotField.setValue(angles[1]);
    zRotField.setValue(angles[2]);
    for (int i = 1; i < objects.length; i++)
    {
      origin = objects[i].coords.getOrigin();
      checkFieldValue(xPosField, origin.x);
      checkFieldValue(yPosField, origin.y);
      checkFieldValue(zPosField, origin.z);
      angles = objects[i].coords.getRotationAngles();
      checkFieldValue(xRotField, angles[0]);
      checkFieldValue(yRotField, angles[1]);
      checkFieldValue(zRotField, angles[2]);
    }

    // Set the texture.

    Texture tex = objects[0].object.getTexture();
    boolean canSetTexture = objects[0].object.canSetTexture();
    boolean sameTexture = true;
    for (int i = 1; i < objects.length; i++)
    {
      Texture thisTex = objects[i].object.getTexture();
      if (thisTex != tex)
        sameTexture = false;
      canSetTexture &= objects[i].object.canSetTexture();
    }
    if (canSetTexture)
    {
      Vector names = new Vector();
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
      names.add(Translate.text("button.newTexture"));
      textureChoice.setModel(new DefaultComboBoxModel(names));
      textureChoice.setSelectedIndex(selected);
    }

    // Set the material.

    Material mat = objects[0].object.getMaterial();
    boolean canSetMaterial = objects[0].object.canSetMaterial();
    boolean sameMaterial = true;
    for (int i = 1; i < objects.length; i++)
    {
      Material thisMat = objects[i].object.getMaterial();
      if (thisMat != mat)
        sameMaterial = false;
      canSetMaterial &= objects[i].object.canSetMaterial();
    }
    if (canSetMaterial)
    {
      Vector names = new Vector();
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
      names.add(Translate.text("button.newMaterial"));
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
    propSelector = new Widget[properties.length];
    for (int i = 0; i < propSelector.length; i++)
    {
      if (properties[i].getType() != Property.BOOLEAN)
        add(new BLabel(properties[i].getName()));
      if (properties[i].getType() == Property.DOUBLE)
        propSelector[i] = new ValueSelector(0.0, properties[i].getMinimum(), properties[i].getMaximum(), 0.005);
      else if (properties[i].getType() == Property.INTEGER)
      {
        propSelector[i] = new ValueField(0.0, ValueField.INTEGER);
        final Property prop = properties[i];
        ((ValueField) propSelector[i]).setValueChecker(new ValueChecker() {
          public boolean isValid(double val)
          {
            return (val >= prop.getMinimum() && val <= prop.getMaximum());
          }
        });
      }
      else if (properties[i].getType() == Property.BOOLEAN)
        propSelector[i] = new BCheckBox(properties[i].getName(), false);
      else if (properties[i].getType() == Property.STRING)
        propSelector[i] = new BTextField(15);
      else if (properties[i].getType() == Property.COLOR)
        propSelector[i] = new ColorSelector(properties[i].getName());
      else if (properties[i].getType() == Property.ENUMERATION)
        propSelector[i] = new BComboBox(properties[i].getAllowedValues());
      if (propSelector[i] instanceof BTextField)
      {
        propSelector[i].addEventLink(FocusLostEvent.class, this, "stringParameterChanged");
        propSelector[i].addEventLink(KeyPressedEvent.class, this, "stringParameterChanged");
      }
      else
        propSelector[i].addEventLink(ValueChangedEvent.class, this, "parameterChanged");
      if (propSelector[i] instanceof ValueSelector || propSelector[i] instanceof BCheckBox || propSelector[i] instanceof ValueField)
        add(propSelector[i], centerLayout);
      else
        add(propSelector[i], fillLayout);
    }
    showParameterValues();

    // Layout and display the panel.

    UIUtilities.applyDefaultBackground(this);
    UIUtilities.applyDefaultFont(this);
    layoutChildren();
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
    properties = objects[0].object.getProperties();
    for (int i = 1; i < objects.length; i++)
    {
      Property otherProperty[] = objects[i].object.getProperties();
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
    if (propSelector == null)
      return;
    Object values[] = new Object [propSelector.length];
    for (int i = 0; i < values.length; i++)
      values[i] = objects[0].object.getPropertyValue(i);
    for (int i = 1; i < objects.length; i++)
    {
      for (int j = 0; j < values.length; j++)
        if (values[j] != null && !values[j].equals(objects[i].object.getPropertyValue(j)))
          values[j] = null;
    }
    for (int i = 0; i < propSelector.length; i++)
    {
      if (propSelector[i] instanceof ValueSelector)
        ((ValueSelector) propSelector[i]).setValue(values[i] == null ? Double.NaN : ((Double) values[i]).doubleValue());
      else if (propSelector[i] instanceof ValueField)
        ((ValueField) propSelector[i]).setValue(values[i] == null ? Double.NaN : ((Integer) values[i]).intValue());
      else if (propSelector[i] instanceof BCheckBox)
        ((BCheckBox) propSelector[i]).setState(((Boolean) values[i]).booleanValue());
      else if (propSelector[i] instanceof BTextField)
        ((BTextField) propSelector[i]).setText((String) values[i]);
      else if (propSelector[i] instanceof ColorSelector)
        ((ColorSelector) propSelector[i]).setColor((RGBColor) values[i]);
      else if (propSelector[i] instanceof BComboBox)
        ((BComboBox) propSelector[i]).setSelectedValue(values[i]);
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
        undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {objects[i].coords, objects[i].coords.duplicate()});
    }
    for (int i = 0; i < objects.length; i++)
    {
      CoordinateSystem coords = objects[i].coords;
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
    if (objects.length == 0 || objects[0].name.equals(nameField.getText()))
      return;
    int which = window.getScene().indexOf(objects[0]);
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.RENAME_OBJECT, new Object [] {new Integer(which), objects[0].name}));
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
    else if (index == textureChoice.getItemCount()-1)
    {
      tex = TexturesDialog.showNewTextureWindow(window, scene);
      if (tex == null)
        rebuildContents(); // They pressed Cancel.
    }
    if (tex != null)
    {
      UndoRecord undo = new UndoRecord(window, false);
      for (int i = 0; i < objects.length; i++)
        if (objects[i].object.getTexture() != tex)
        {
          undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {objects[i].object, objects[i].object.duplicate()});
          objects[i].setTexture(tex, tex.getDefaultMapping(objects[i].object));
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
    boolean noMaterial = (index == materialChoice.getItemCount()-2);
    if (index < scene.getNumMaterials())
      mat = scene.getMaterial(index);
    else if (index == materialChoice.getItemCount()-1)
    {
      mat = MaterialsDialog.showNewMaterialWindow(window, scene);
      if (mat == null)
        rebuildContents(); // They pressed Cancel.
    }
    if (noMaterial || mat != null)
    {
      UndoRecord undo = new UndoRecord(window, false);
      for (int i = 0; i < objects.length; i++)
        if (objects[i].object.getMaterial() != mat)
        {
          undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {objects[i].object, objects[i].object.duplicate()});
          objects[i].setMaterial(mat, noMaterial ? null : mat.getDefaultMapping(objects[i].object));
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

  /**
   * This is used for events generated by BTextFields for editing String properties.  These
   * need to be handled differently, since the property value should only change when editing
   * is complete.
   */

  private void stringParameterChanged(WidgetEvent ev)
  {
    if (ev instanceof FocusLostEvent || (ev instanceof KeyPressedEvent && ((KeyPressedEvent) ev).getKeyCode() == KeyPressedEvent.VK_ENTER))
      parameterChanged(new ValueChangedEvent(ev.getWidget(), false));
  }

  private void processParameterChange(ValueChangedEvent ev)
  {
    UndoRecord undo = null;
    if (ev.getWidget() != lastEventSource)
    {
      ignoreNextChange = true;
      undo = new UndoRecord(window, false);
      for (int i = 0; i < objects.length; i++)
        undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {objects[i].object, objects[i].object.duplicate()});
    }
    boolean changed = false;
    for (int i = 0; i < objects.length; i++)
    {
      for (int j = 0; j < propSelector.length; j++)
        if (propSelector[j] == ev.getSource())
        {
          Object value = null;
          if (propSelector[j] instanceof ValueSelector)
            value = new Double(((ValueSelector) propSelector[j]).getValue());
          else if (propSelector[j] instanceof ValueField)
            value = new Integer((int) ((ValueField) propSelector[j]).getValue());
          else if (propSelector[j] instanceof BCheckBox)
            value = Boolean.valueOf(((BCheckBox) propSelector[j]).getState());
          else if (propSelector[j] instanceof BTextField)
            value = ((BTextField) propSelector[j]).getText();
          else if (propSelector[j] instanceof ColorSelector)
            value = ((ColorSelector) propSelector[j]).getColor();
          else if (propSelector[j] instanceof BComboBox)
            value = ((BComboBox) propSelector[j]).getSelectedValue();
          if (!objects[i].object.getPropertyValue(j).equals(value))
          {
            objects[i].object.setPropertyValue(j, value);
            changed = true;
          }
        }
      window.getScene().objectModified(objects[i].object);
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

  /**
   * A selector Widget for setting a color.
   */

  private class ColorSelector extends CustomWidget
  {
    private String title;
    private RGBColor color;
    ColorSelector(String title)
    {
      this.title = title;
      color = new RGBColor();
      setPreferredSize(new Dimension(30, 15));
      setMaximumSize(new Dimension(30, 15));
      setBackground(color.getColor());
      ((JComponent) getComponent()).setBorder(BorderFactory.createLoweredBevelBorder());
      addEventLink(MouseClickedEvent.class, this);
    }

    public RGBColor getColor()
    {
      return color.duplicate();
    }

    public void setColor(RGBColor color)
    {
      this.color = color.duplicate();
      setBackground(color.getColor());
    }

    private void processEvent()
    {
      RGBColor oldColor = color.duplicate();
      new ColorChooser(window, title, color);
      if (!color.equals(oldColor))
      {
        setBackground(color.getColor());
        dispatchEvent(new ValueChangedEvent(this));
      }
    }
  }
}

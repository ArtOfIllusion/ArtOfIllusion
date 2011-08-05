/* Copyright (C) 2000-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a Module which outputs a per-vertex texture parameter. */

public class ParameterModule extends Module
{
  double minVal, maxVal, defaultVal;
  int index, id;
  PointInfo point;

  public ParameterModule(Point position)
  {
    super(Translate.text("menu.parameterModule"), new IOPort [] {}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})}, 
      position);
    minVal = 0.0f;
    maxVal = 1.0f;
    defaultVal = 0.0f;
    id = TextureParameter.getUniqueID();
  }
  
  /** Get the name of the parameter. */
  
  public String getParameterName()
  {
    return name;
  }
  
  /** Set the name of the parameter. */
  
  public void setParameterName(String name)
  {
    this.name = name;
  }
  
  /** Get the minimum value for the parameter. */
  
  public double getMinimum()
  {
    return minVal;
  }
  
  /** Set the minimum value for the parameter. */
  
  public void setMinimum(double val)
  {
    minVal = val;
  }
  
  /** Get the maximum value for the parameter. */
  
  public double getMaximum()
  {
    return maxVal;
  }
  
  /** Set the maximum value for the parameter. */
  
  public void setMaximum(double val)
  {
    maxVal = val;
  }
  
  /** Get the default value for the parameter. */
  
  public double getDefaultValue()
  {
    return defaultVal;
  }
  
  /** Set the default value for the parameter. */
  
  public void setDefaultValue(double val)
  {
    defaultVal = val;
  }

  /* Cache the PointInfo object to have access to the coordinates later on. */

  public void init(PointInfo p)
  {
    point = p;
  }

  /* This module outputs the value of the parameter. */
  
  public double getAverageValue(int which, double blur)
  {
    if (point.param == null || point.param.length <= which)
      return defaultVal;
    return point.param[index];
  }
  
  /* Set the index of this parameter. */
  
  public void setIndex(int index)
  {
    this.index = index;
  }
  
  /* Get the texture parameter corresponding to this module. */
  
  public TextureParameter getParameter(Object owner)
  {
    TextureParameter param = new TextureParameter(owner, name, minVal, maxVal, defaultVal);
    param.setID(id);
    return param;
  }

  /* Allow the user to set the parameters. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    BTextField nameField = new BTextField(name);
    ValueField minField = new ValueField(minVal, ValueField.NONE);
    ValueField maxField = new ValueField(maxVal, ValueField.NONE);
    final ValueField defaultField = new ValueField(defaultVal, ValueField.NONE);
    defaultField.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        defaultVal = defaultField.getValue();
        editor.updatePreview();
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("selectParameterProperties"),
      new Widget [] {nameField, minField, maxField, defaultField},
      new String [] {Translate.text("Name"), Translate.text("Minimum"), Translate.text("Maximum"), Translate.text("Default")});
    if (!dlg.clickedOk())
      return false;
    name = nameField.getText();
    minVal = minField.getValue();
    maxVal = maxField.getValue();
    defaultVal = defaultField.getValue();
    if (minVal > maxVal)
      {
        new BStandardDialog("", Translate.text("minimumAboveMaxError"), BStandardDialog.ERROR).showMessageDialog(editor.getParentFrame());
        return edit(editor, theScene);
      }
    if (minVal > defaultVal || maxVal < defaultVal)
      {
        new BStandardDialog("", Translate.text("defaultOutOfRangeError"), BStandardDialog.ERROR).showMessageDialog(editor.getParentFrame());
        return edit(editor, theScene);
      }
    layout();
    return true;
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    ParameterModule mod = new ParameterModule(new Point(bounds.x, bounds.y));
    
    mod.name = name;
    mod.minVal = minVal;
    mod.maxVal = maxVal;
    mod.defaultVal = defaultVal;
    mod.index = index;
    mod.id = id;
    mod.layout();
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeUTF(name);
    out.writeDouble(minVal);
    out.writeDouble(maxVal);
    out.writeDouble(defaultVal);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    name = in.readUTF();
    minVal = in.readDouble();
    maxVal = in.readDouble();
    defaultVal = in.readDouble();
    layout();
  }
}

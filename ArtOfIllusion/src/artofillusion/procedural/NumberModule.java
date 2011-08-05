/* Copyright (C) 2000-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a Module which outputs a number. */

public class NumberModule extends Module
{
  private double value;
  
  public NumberModule(Point position)
  {
    super("0.0", new IOPort [] {}, new IOPort [] {
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})}, 
      position);
  }
  
  public NumberModule(Point position, double v) 
  { 
    super(Double.toString(v), new IOPort [] {}, new IOPort [] { 
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})},  
      position); 
    value = v; 
  }
  
  /** Get the value. */
  
  public double getValue()
  {
    return value;
  }
  
  /** Set the value. */
  
  public void setValue(double v)
  {
    value = v;
  }

  /** Allow the user to set a new value. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final ValueField field = new ValueField(value, ValueField.NONE);
    field.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        value = field.getValue();
        editor.updatePreview();
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("selectValue"), new Widget [] {field},
      new String [] {null});
    if (!dlg.clickedOk())
      return false;
    value = field.getValue();
    name = Double.toString(value);
    layout();
    return true;
  }

  /** This module simply outputs the value. */
  
  public double getAverageValue(int which, double blur)
  {
    return value;
  }

  public void getValueGradient(Vec3 grad, double blur)
  {
    grad.set(0.0, 0.0, 0.0);
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    NumberModule mod = new NumberModule(new Point(bounds.x, bounds.y));
    
    mod.value = value;
    mod.name = ""+value;
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble(value);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    value = in.readDouble();
    name = ""+value;
    layout();
  }
}

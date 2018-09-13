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

/** This is a Module which scales and shifts its input value. */

public class ScaleShiftModule extends ProceduralModule
{
  private double scale, shift;

  public ScaleShiftModule(Point position)
  {
    super("\u00D7 1.0 + 0.0", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(0)"})},
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})},
      position);
    scale = 1.0;
    shift = 0.0;
  }

  /** Get the scale value. */

  public double getScale()
  {
    return scale;
  }

  /** Set the scale value. */

  public void setScale(double val)
  {
    scale = val;
  }

  /** Get the shift value. */

  public double getShift()
  {
    return shift;
  }

  /** Set the shift value. */

  public void setShift(double val)
  {
    shift = val;
  }

  /* Calculate the output value. */

  @Override
  public double getAverageValue(int which, double blur)
  {
    if (linkFrom[0] == null)
      return shift;
    return linkFrom[0].getAverageValue(linkFromIndex[0], blur)*scale + shift;
  }

  /* Calculate the output error. */

  @Override
  public double getValueError(int which, double blur)
  {
    if (linkFrom[0] == null)
      return 0.0;
    return linkFrom[0].getValueError(linkFromIndex[0], blur)*Math.abs(scale);
  }

  /* The gradient is the sum of the two gradients. */

  @Override
  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (linkFrom[0] == null)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
    else
      linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
    grad.set(grad.x*scale+shift, grad.y*scale+shift, grad.z*scale+shift);
  }

  /* Allow the user to set the parameters. */

  @Override
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final ValueField scaleField = new ValueField(scale, ValueField.NONE, 5);
    final ValueField shiftField = new ValueField(shift, ValueField.NONE, 5);
    Object listener = new Object() {
      void processEvent()
      {
        scale = scaleField.getValue();
        shift = shiftField.getValue();
        editor.updatePreview();
      }
    };
    scaleField.addEventLink(ValueChangedEvent.class, listener);
    shiftField.addEventLink(ValueChangedEvent.class, listener);
    RowContainer row = new RowContainer();
    row.add(new BLabel(Translate.text("scaleShiftEquation")));
    row.add(scaleField);
    row.add(new BLabel(" + "));
    row.add(shiftField);
    PanelDialog dlg = new PanelDialog(editor.getParentFrame(), Translate.text("selectScaleShiftProperties"), row);
    if (!dlg.clickedOk())
      return false;
    scale = scaleField.getValue();
    shift = shiftField.getValue();
    name = "\u00D7 "+scale+" + "+shift;
    layout();
    return true;
  }

  /* Create a duplicate of this module. */

  @Override
  public Module duplicate()
  {
    ScaleShiftModule mod = new ScaleShiftModule(new Point(bounds.x, bounds.y));

    mod.scale = scale;
    mod.shift = shift;
    mod.name = name;
    return mod;
  }

  /* Write out the parameters. */

  @Override
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble(scale);
    out.writeDouble(shift);
  }

  /* Read in the parameters. */

  @Override
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    scale = in.readDouble();
    shift = in.readDouble();
    name = "\u00D7 "+scale+" + "+shift;
    layout();
  }
}

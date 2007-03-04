/* Copyright (C) 2000,2004 by Peter Eastman

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
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a Module which clips its input to a fixed range. */

public class ClipModule extends Module
{
  double min, max;
    
  public ClipModule(Point position)
  {
    super(Translate.text("menu.clipModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})}, 
      position);
    min = 0.0;
    max = 1.0;
  }
  
  /** Get the minimum clipping value. */
  
  public double getMinimum()
  {
    return min;
  }
  
  /** Set the minimum clipping value. */
  
  public void setMinimum(double m)
  {
    min = m;
  }
  
  /** Get the maximum clipping value. */
  
  public double getMaximum()
  {
    return max;
  }
  
  /** Set the maximum clipping value. */
  
  public void setMaximum(double m)
  {
    max = m;
  }

  /* Clip the input value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (linkFrom[0] == null)
      return 0.0;
    double v = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    if (v < min)
      return min;
    if (v > max)
      return max;
    return v;
  }

  /* Calculate the error. */
  
  public double getValueError(int which, double blur)
  {
    if (linkFrom[0] == null)
      return 0.0;
    double v = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    if (v < min || v > max)
      return 0.0;
    return linkFrom[0].getValueError(linkFromIndex[0], blur);
  }

  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (linkFrom[0] == null)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
    double v = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    if (v < min || v > max)
      grad.set(0.0, 0.0, 0.0);
    else
      linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
  }
  
  /* Allow the user to set the parameters. */
  
  public boolean edit(BFrame fr, Scene theScene)
  {
    ValueField minField = new ValueField(min, ValueField.NONE);
    ValueField maxField = new ValueField(max, ValueField.NONE);
    ComponentsDialog dlg = new ComponentsDialog(fr, Translate.text("selectClipRange"), 
      new Widget [] {minField, maxField},
      new String [] {Translate.text("Minimum"), Translate.text("Maximum")});
    if (!dlg.clickedOk())
      return false;
    min = minField.getValue();
    max = maxField.getValue();
    if (min > max)
    {
      new BStandardDialog("", Translate.text("minimumAboveMaxError"), BStandardDialog.INFORMATION).showMessageDialog(fr);
      edit(fr, theScene);
    }
    return true;
  }

    /* Create a duplicate of this module. */

  public Module duplicate()
  {
    ClipModule mod = new ClipModule(new Point(bounds.x, bounds.y));
    mod.min = min;
    mod.max = max;
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble(min);
    out.writeDouble(max);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    min = in.readDouble();
    max = in.readDouble();
  }
}

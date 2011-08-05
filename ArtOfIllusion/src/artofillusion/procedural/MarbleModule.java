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

/** This is a Module which generates a marble pattern. */

public class MarbleModule extends Module
{
  boolean valueOk, gradOk;
  int octaves;
  double value, error, amplitude, spacing, lastBlur;
  Vec3 gradient, tempVec;
  PointInfo point;
  
  public MarbleModule(Point position)
  {
    super(Translate.text("menu.marbleModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Noise", "(0.5)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})}, 
      position);
    octaves = 4;
    amplitude = 5.0;
    spacing = 1.0;
    gradient = new Vec3();
    tempVec = new Vec3();
  }
  
  /** Get the number of octaves. */
  
  public int getOctaves()
  {
    return octaves;
  }
  
  /** Set the number of octaves. */
  
  public void setOctaves(int o)
  {
    octaves = o;
  }
  
  /** Get the amplitude. */
  
  public double getAmplitude()
  {
    return amplitude;
  }
  
  /** Set the amplitude. */
  
  public void setAmplitude(double a)
  {
    amplitude = a;
  }
  
  /** Get the spacing. */
  
  public double getSpacing()
  {
    return spacing;
  }
  
  /** Set the spacing. */
  
  public void setSpacing(double s)
  {
    spacing = s;
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    point = p;
    valueOk = gradOk = false;
  }

  /* Calculate the value, error, and gradient all at once, since calculating just the value
     is almost as much work as calculating all three. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    double x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);
    double persistence = (linkFrom[3] == null) ? 0.5 : linkFrom[3].getAverageValue(linkFromIndex[3], blur);
    double xsize = (linkFrom[0] == null) ? 0.5*point.xsize+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double ysize = (linkFrom[1] == null) ? 0.5*point.ysize+blur : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double zsize = (linkFrom[2] == null) ? 0.5*point.zsize+blur : linkFrom[2].getValueError(linkFromIndex[2], blur);
    double amp = 0.5*amplitude, scale = 1.0, d;
    double cutoff = 0.5/Math.max(Math.max(xsize, ysize), zsize);

    // First calculate the turbulence function.
    
    value = 0.0;
    error = 0.0;
    gradient.set(0.0, 0.0, 0.0);
    for (int i = 0; i < octaves && cutoff > scale; i++)
      {
        d = amp*Noise.value(x*scale+123.456, y*scale+123.456, z*scale+123.456);
        Noise.calcGradient(tempVec, x*scale+123.456, y*scale+123.456, z*scale+123.456);
        if (d > 0.0)
          tempVec.scale(amp*scale);
        else
          tempVec.scale(-amp*scale);
        error += Math.abs(xsize*tempVec.x) + Math.abs(ysize*tempVec.y) + Math.abs(zsize*tempVec.z);
        if (scale > 0.5*cutoff)
        {
          d *= 2.0*(1.0-scale/cutoff);
          tempVec.scale(2.0*(1.0-scale/cutoff));
        }
        value += Math.abs(d);
        gradient.add(tempVec);
        amp *= persistence;
        scale *= 2.0;
      }
    
    // Now use that to calculate the marble function.
    
    scale = 2.0*Math.PI/spacing;
    if (linkFrom[0] == null)
      tempVec.set(1.0, 0.0, 0.0);
    else
      linkFrom[0].getValueGradient(linkFromIndex[0], tempVec, blur);
    gradient.set(scale*tempVec.x+gradient.x, scale*tempVec.y+gradient.y, scale*tempVec.z+gradient.z);
    gradient.scale(0.5*Math.cos(x*scale+value));
    if (error == 0.0)
      value = 0.5 + 0.5*Math.sin(x*scale+value);
    else
      value = 0.5 + 0.25*(Math.sin(x*scale+value-error) - Math.sin(x*scale+value+error)) / error;
    error = Math.abs(xsize*gradient.x) + Math.abs(ysize*gradient.y) + Math.abs(zsize*gradient.z);
    if (error > 0.5)
      error = 0.5;
    valueOk = true;
    lastBlur = blur;
    return value;
  }

  /* The error is calculated at the same time as the value. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    return error;
  }

  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (gradOk && blur == lastBlur)
      {
	grad.set(gradient);
	return;
      }
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    double dx = gradient.x, dy = gradient.y, dz = gradient.z;
    if (dx != 0.0)
      {
        if (linkFrom[0] == null)
          gradient.set(dx, 0.0, 0.0);
        else
          {
            linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
            gradient.x = dx*grad.x;
            gradient.y = dx*grad.y;
            gradient.z = dx*grad.z;
          }
      }
    else
      gradient.set(0.0, 0.0, 0.0);
    if (dy != 0.0)
      {
        if (linkFrom[1] == null)
          gradient.y += dy;
        else
          {
            linkFrom[1].getValueGradient(linkFromIndex[1], grad, blur);
            gradient.x += dy*grad.x;
            gradient.y += dy*grad.y;
            gradient.z += dy*grad.z;
          }
      }
    if (dz != 0.0)
      {
        if (linkFrom[2] == null)
          gradient.z += dz;
        else
          {
            linkFrom[2].getValueGradient(linkFromIndex[2], grad, blur);
            gradient.x += dz*grad.x;
            gradient.y += dz*grad.y;
            gradient.z += dz*grad.z;
          }
      }
    gradOk = true;
    grad.set(gradient);
  }
  
  /** Allow the user to set the parameters. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final ValueField octavesField = new ValueField((double) octaves, ValueField.POSITIVE+ValueField.INTEGER);
    final ValueField ampField = new ValueField(amplitude, ValueField.NONE);
    final ValueField spacingField = new ValueField(spacing, ValueField.POSITIVE);
    Object listener = new Object() {
      void processEvent()
      {
        octaves = (int) octavesField.getValue();
        amplitude = ampField.getValue();
        spacing = spacingField.getValue();
        editor.updatePreview();
      }
    };
    octavesField.addEventLink(ValueChangedEvent.class, listener);
    ampField.addEventLink(ValueChangedEvent.class, listener);
    spacingField.addEventLink(ValueChangedEvent.class, listener);
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("selectMarbleProperties"),
      new Widget [] {ampField, spacingField, octavesField},
      new String [] {Translate.text("Noise Amplitude"), Translate.text("Band Spacing"), Translate.text("Octaves")});
    if (!dlg.clickedOk())
      return false;
    return true;
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    MarbleModule mod = new MarbleModule(new Point(bounds.x, bounds.y));
    
    mod.octaves = octaves;
    mod.amplitude = amplitude;
    mod.spacing = spacing;
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(octaves);
    out.writeDouble(amplitude);
    out.writeDouble(spacing);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    octaves = in.readInt();
    amplitude = in.readDouble();
    spacing = in.readDouble();
  }
}

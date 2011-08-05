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

/** This is a Module which generates fractal noise based on Perlin's noise function. */

public class NoiseModule extends Module
{
  boolean valueOk, errorOk, gradOk;
  int octaves;
  double value, error, amplitude, lastBlur;
  Vec3 gradient, tempVec;
  PointInfo point;
  
  public NoiseModule(Point position)
  {
    super(Translate.text("menu.noiseModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Noise", "(0.5)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})}, 
      position);
    octaves = 4;
    amplitude = 1.0;
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

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    point = p;
    valueOk = errorOk = gradOk = false;
  }

  /* Calculate the noise function. */
  
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
    double amp = 0.5*amplitude, scale = 1.0;
    double cutoff = 0.5/Math.max(Math.max(xsize, ysize), zsize);
    
    value = 0.0;
    for (int i = 0; i < octaves && cutoff > scale; i++)
      {
        if (scale > 0.5*cutoff)
          amp *= 2.0*(1.0-scale/cutoff);
        value += amp*Noise.value(x*scale+123.456, y*scale+123.456, z*scale+123.456);
        amp *= persistence;
        scale *= 2.0;
      }
    value = 0.5*value + 0.5;
    valueOk = true;
    lastBlur = blur;
    return value;
  }

  /* Estimate the error from the derivative of the function. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (errorOk)
      return error;
    double x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);
    double persistence = (linkFrom[3] == null) ? 0.5 : linkFrom[3].getAverageValue(linkFromIndex[3], blur);
    double xsize = (linkFrom[0] == null) ? 0.5*point.xsize+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double ysize = (linkFrom[1] == null) ? 0.5*point.ysize+blur : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double zsize = (linkFrom[2] == null) ? 0.5*point.zsize+blur : linkFrom[2].getValueError(linkFromIndex[2], blur);
    double amp = 0.5*amplitude, scale = 1.0;
    double cutoff = 0.5/Math.max(Math.max(xsize, ysize), zsize);
    int i;
    
    error = 0.0;
    gradient.set(0.0, 0.0, 0.0);
    for (i = 0; i < octaves && cutoff > scale; i++)
      {
        Noise.calcGradient(tempVec, x*scale+123.456, y*scale+123.456, z*scale+123.456);
        tempVec.scale(amp*scale);
        error += Math.abs(xsize*tempVec.x) + Math.abs(ysize*tempVec.y) + Math.abs(zsize*tempVec.z);
        if (scale > 0.5*cutoff)
          tempVec.scale(2.0*(1.0-scale/cutoff));
        gradient.add(tempVec);
        amp *= persistence;
        scale *= 2.0;
      }
    for (; i < octaves; i++)
      {
        error += amp;
        amp *= persistence;
        scale *= 2.0;
      }
    error *= 0.5;
    gradient.scale(0.5);
    errorOk = true;
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
    if (!errorOk || blur != lastBlur)
      getValueError(which, blur);
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
  
  /* Allow the user to set the parameters. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final ValueField octavesField = new ValueField((double) octaves, ValueField.POSITIVE+ValueField.INTEGER);
    final ValueField ampField = new ValueField(amplitude, ValueField.NONE);
    Object listener = new Object() {
      void processEvent()
      {
        octaves = (int) octavesField.getValue();
        amplitude = ampField.getValue();
        editor.updatePreview();
      }
    };
    octavesField.addEventLink(ValueChangedEvent.class, listener);
    ampField.addEventLink(ValueChangedEvent.class, listener);
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("selectNoiseProperties"),
      new Widget [] {ampField, octavesField}, new String [] {Translate.text("Amplitude"), Translate.text("Octaves")});
    if (!dlg.clickedOk())
      return false;
    octaves = (int) octavesField.getValue();
    amplitude = ampField.getValue();
    return true;
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    NoiseModule mod = new NoiseModule(new Point(bounds.x, bounds.y));
    
    mod.octaves = octaves;
    mod.amplitude = amplitude;
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(octaves);
    out.writeDouble(amplitude);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    octaves = in.readInt();
    amplitude = in.readDouble();
  }
}

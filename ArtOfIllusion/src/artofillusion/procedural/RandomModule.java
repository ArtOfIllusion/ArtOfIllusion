/* Copyright (C) 2002-2011 by Peter Eastman

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
import java.util.*;

/** This is a Module which outputs a random function of its input. */

public class RandomModule extends ProceduralModule
{
  boolean repeat, valueOk, errorOk, gradOk;
  int lastBase, octaves;
  double a1, a2, a3;
  double value, error, deriv, amplitude, lastBlur;
  Random random;
  Vec3 gradient;
  PointInfo point;

  public RandomModule(Point position)
  {
    super(Translate.text("menu.randomModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(time)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Noise", "(0.5)"})},
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})},
      position);
    octaves = 3;
    amplitude = 1.0;
    lastBase = Integer.MAX_VALUE;
    gradient = new Vec3();
    random = new FastRandom(0);
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

  @Override
  public void init(PointInfo p)
  {
    valueOk = errorOk = gradOk = false;
    point = p;
  }

  /** Find the polynomial coefficients from a specified base. */

  private void calcCoefficients(int base)
  {
    if (base == lastBase)
      return;
    lastBase = base;
    random.setSeed(base);
    random.nextDouble();
    a1 = random.nextDouble();
    random.setSeed(base+1);
    random.nextDouble();
    double m2 = random.nextDouble();
    a2 = -2.0*a1-m2;
    a3 = a1+m2;
  }

  /** Calculate the noise function corresponding to a given input value, where
      fract is the fractional part of the input, and calcCoefficients() has
      already been called with the integer part. */

  private double calcNoise(double fract)
  {
    return fract*(a1 + fract*(a2 + fract*a3));
  }

  /** Calculate the derivative of the noise function corresponding to a given
      input value, where fract is the fractional part of the input, and
      calcCoefficients() has already been called with the integer part. */

  private double calcNoiseDeriv(double fract)
  {
    return (a1 + fract*(2.0*a2 + fract*3.0*a3));
  }

  /** Calculate the integral of the noise function from the start of the
      current unit interval, where fract is the fractional part of the input,
      and calcCoefficients() has already been called with the integer part. */

  private double calcNoiseIntegral(double fract)
  {
    return fract*fract*(0.5*a1 + fract*((1.0/3.0)*a2 + fract*0.25*a3));
  }

  /** Calculate the integral of the noise function over a unit interval,
      where calcCoefficients() has already been called with the starting
      point. */

  private double calcNoiseUnitIntegral()
  {
    return (0.5*a1 + ((1.0/3.0)*a2 + 0.25*a3));
  }

  /* Calculate the output value. */

  @Override
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    double x = (linkFrom[0] == null) ? point.t : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double persistence = (linkFrom[1] == null) ? 0.5 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double xsize = (linkFrom[0] == null) ? blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double amp = amplitude, scale = 1.0;
    double cutoff = 0.5/xsize;

    value = 0.0;
    for (int i = 0; i < octaves && cutoff > scale; i++)
      {
        if (xsize == 0.0)
          {
            double x1 = x*scale+123.456;
            int base = FastMath.floor(x1);
            calcCoefficients(base);
            value += amp*calcNoise(x1-base);
          }
        else
          {
            double x1 = (x-xsize)*scale+123.456;
            double x2 = (x+xsize)*scale+123.456;
            int base1 = FastMath.floor(x1);
            int base2 = FastMath.floor(x2);
            calcCoefficients(base1);
            double integral = -calcNoiseIntegral(x1-base1);
            while (base1 < base2)
              {
                integral += calcNoiseUnitIntegral();
                calcCoefficients(++base1);
              }
            integral += calcNoiseIntegral(x2-base2);
            value += 0.5*amp*integral/xsize;
          }
        amp *= persistence;
        scale *= 2.0;
      }
    value += 0.5;
    valueOk = true;
    lastBlur = blur;
    return value;
  }

  /* Calculate the error. */

  @Override
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (errorOk)
      return error;
    double x = (linkFrom[0] == null) ? point.t : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double persistence = (linkFrom[1] == null) ? 0.5 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double xsize = (linkFrom[0] == null) ? blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double amp = amplitude, scale = 1.0;
    double cutoff = 0.5/xsize;
    int i;

    deriv = 0.0;
    error = 0.0;
    for (i = 0; i < octaves && cutoff > scale; i++)
      {
        double x1 = x*scale+123.456;
        int base = FastMath.floor(x1);
        calcCoefficients(base);
        deriv += amp*calcNoiseDeriv(x1-base);
        amp *= persistence;
        scale *= 2.0;
      }
    error = xsize*deriv;
    for (; i < octaves; i++)
      {
        error += amp;
        amp *= persistence;
        scale *= 2.0;
      }
    errorOk = true;
    return error;
  }

  /* Calculate the gradient. */

  @Override
  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!errorOk || blur != lastBlur)
      getValueError(which, blur);
    if (gradOk && blur == lastBlur)
      {
	grad.set(gradient);
	return;
      }
    if (linkFrom[0] == null)
      {
	gradient.set(0.0, 0.0, 0.0);
	grad.set(0.0, 0.0, 0.0);
	gradOk = true;
	return;
      }
    lastBlur = blur;
    gradOk = true;
    linkFrom[0].getValueGradient(linkFromIndex[0], gradient, blur);
    gradient.scale(deriv);
    grad.set(gradient);
  }

  /* Create a duplicate of this module. */

  @Override
  public Module duplicate()
  {
    RandomModule mod = new RandomModule(new Point(bounds.x, bounds.y));

    mod.octaves = octaves;
    mod.amplitude = amplitude;
    return mod;
  }

  /* Write out the parameters. */

  @Override
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(octaves);
    out.writeDouble(amplitude);
  }

  /* Read in the parameters. */

  @Override
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    octaves = in.readInt();
    amplitude = in.readDouble();
  }

  /* Allow the user to set the parameters. */

  @Override
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
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("selectRandomProperties"), new Widget [] {ampField, octavesField},
      new String [] {Translate.text("Amplitude"), Translate.text("Octaves")});
    if (!dlg.clickedOk())
      return false;
    octaves = (int) octavesField.getValue();
    amplitude = ampField.getValue();
    return true;
  }
}

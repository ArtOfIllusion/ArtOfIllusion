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

/** This is a Module which randomly displaces the coordinate system. */

public class JitterModule extends Module
{
  boolean valueOk;
  Vec3 v, tempVec;
  double xamp, yamp, zamp, xscale, yscale, zscale, invxscale, invyscale, invzscale, lastBlur;
  PointInfo point;
  
  public JitterModule(Point position)
  {
    super(Translate.text("menu.jitterModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"X"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Y"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Z"})}, 
      position);
    xamp = yamp = zamp = 0.1;
    xscale = yscale = zscale = invxscale = invyscale = invzscale = 1.0;
    v = new Vec3();
    tempVec = new Vec3();
  }
  
  /** Get the X scale. */
  
  public double getXScale()
  {
    return xscale;
  }
  
  /** Set the X scale. */
  
  public void setXScale(double scale)
  {
    xscale = scale;
    invxscale = 1.0/scale;
  }
  
  /** Get the Y scale. */
  
  public double getYScale()
  {
    return yscale;
  }
  
  /** Set the Y scale. */
  
  public void setYScale(double scale)
  {
    yscale = scale;
    invyscale = 1.0/scale;
  }
  
  /** Get the Z scale. */
  
  public double getZScale()
  {
    return zscale;
  }
  
  /** Set the Z scale. */
  
  public void setZScale(double scale)
  {
    zscale = scale;
    invzscale = 1.0/scale;
  }
  
  /** Get the X amplitude. */
  
  public double getXAmplitude()
  {
    return xamp;
  }
  
  /** Set the X amplitude. */
  
  public void setXAmplitude(double amp)
  {
    xamp = amp;
  }
  
  /** Get the Y amplitude. */
  
  public double getYAmplitude()
  {
    return yamp;
  }
  
  /** Set the Y amplitude. */
  
  public void setYAmplitude(double amp)
  {
    yamp = amp;
  }
  
  /** Get the Z amplitude. */
  
  public double getZAmplitude()
  {
    return zamp;
  }
  
  /** Set the Z amplitude. */
  
  public void setZAmplitude(double amp)
  {
    zamp = amp;
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    point = p;
    valueOk = false;
  }

  /* Calculate the average value of an output. */

  public double getAverageValue(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      {
        v.x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
        v.y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
        v.z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);
        Noise.calcVector(tempVec, invxscale*v.x, invyscale*v.y, invzscale*v.z);
        v.x += xamp*tempVec.x;
        v.y += yamp*tempVec.y;
        v.z += zamp*tempVec.z;
        valueOk = true;
        lastBlur = blur;
      }
    if (which == 0)
      return v.x;
    else if (which == 1)
      return v.y;
    else
      return v.z;
  }
  
  /* The error is unaffected by this module. */
  
  public double getValueError(int which, double blur)
  {
    if (linkFrom[which] != null)
      return linkFrom[which].getValueError(linkFromIndex[which], blur);
    if (which == 0)
      return point.xsize*0.5+blur;
    else if (which == 1)
      return point.ysize*0.5+blur;
    else
      return point.zsize*0.5+blur;
  }
  
  /* The gradient is unaffected by this module. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (linkFrom[which] != null)
      linkFrom[which].getValueGradient(linkFromIndex[which], grad, blur);
    else if (which == 0)
      grad.set(1.0, 0.0, 0.0);
    else if (which == 1)
      grad.set(0.0, 1.0, 0.0);
    else
      grad.set(0.0, 0.0, 1.0);
  }
  
  /* Allow the user to set the parameters. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final ValueField amp1 = new ValueField(xamp, ValueField.NONE, 4);
    final ValueField amp2 = new ValueField(yamp, ValueField.NONE, 4);
    final ValueField amp3 = new ValueField(zamp, ValueField.NONE, 4);
    final ValueField scale1 = new ValueField(xscale, ValueField.POSITIVE, 4);
    final ValueField scale2 = new ValueField(yscale, ValueField.POSITIVE, 4);
    final ValueField scale3 = new ValueField(zscale, ValueField.POSITIVE, 4);
    Object listener = new Object() {
      void processEvent()
      {
        xamp = amp1.getValue();
        yamp = amp2.getValue();
        zamp = amp3.getValue();
        xscale = scale1.getValue();
        yscale = scale2.getValue();
        zscale = scale3.getValue();
        invxscale = 1.0/xscale;
        invyscale = 1.0/yscale;
        invzscale = 1.0/zscale;
        editor.updatePreview();
      }
    };
    amp1.addEventLink(ValueChangedEvent.class, listener);
    amp2.addEventLink(ValueChangedEvent.class, listener);
    amp3.addEventLink(ValueChangedEvent.class, listener);
    scale1.addEventLink(ValueChangedEvent.class, listener);
    scale2.addEventLink(ValueChangedEvent.class, listener);
    scale3.addEventLink(ValueChangedEvent.class, listener);
    FormContainer p = new FormContainer(4, 3);
    p.add(new BLabel(), 0, 0);
    p.add(new BLabel("X"), 1, 0);
    p.add(new BLabel("Y"), 2, 0);
    p.add(new BLabel("Z"), 3, 0);
    p.add(Translate.label("Amplitude"), 0, 1);
    p.add(amp1, 1, 1);
    p.add(amp2, 2, 1);
    p.add(amp3, 3, 1);
    p.add(Translate.label("Scale"), 0, 2);
    p.add(scale1, 1, 2);
    p.add(scale2, 2, 2);
    p.add(scale3, 3, 2);
    PanelDialog dlg = new PanelDialog(editor.getParentFrame(), Translate.text("selectJitterParameters"), p);
    if (!dlg.clickedOk())
      return false;
    return true;
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    JitterModule mod = new JitterModule(new Point(bounds.x, bounds.y));
    
    mod.xamp = xamp;
    mod.yamp = yamp;
    mod.zamp = zamp;
    mod.xscale = xscale;
    mod.yscale = yscale;
    mod.zscale = zscale;
    mod.invxscale = invxscale;
    mod.invyscale = invyscale;
    mod.invzscale = invzscale;
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble(xamp);
    out.writeDouble(yamp);
    out.writeDouble(zamp);
    out.writeDouble(xscale);
    out.writeDouble(yscale);
    out.writeDouble(zscale);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    xamp = in.readDouble();
    yamp = in.readDouble();
    zamp = in.readDouble();
    xscale = in.readDouble();
    yscale = in.readDouble();
    zscale = in.readDouble();
    invxscale = 1.0/xscale;
    invyscale = 1.0/yscale;
    invzscale = 1.0/zscale;
  }
}

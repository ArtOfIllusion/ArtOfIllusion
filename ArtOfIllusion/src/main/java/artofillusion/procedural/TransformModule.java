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
import java.awt.*;
import java.io.*;

/** This is a Module which performs a linear 3D coordinate transform. */

public class TransformModule extends ProceduralModule
{
  boolean valueOk, errorOk, gradOk;
  CoordinateSystem coords;
  Vec3 grad1, grad2, grad3, v, tempVec;
  Mat4 trans, gradTrans;
  double xscale, yscale, zscale, lastBlur;
  PointInfo point;

  public TransformModule(Point position)
  {
    super(Translate.text("menu.linearModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"})},
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"X"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Y"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Z"})},
      position);
    coords = new CoordinateSystem(new Vec3(0.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
    xscale = yscale = zscale = 1.0;
    updateTransforms();
    grad1 = new Vec3();
    grad2 = new Vec3();
    grad3 = new Vec3();
    v = new Vec3();
    tempVec = new Vec3();
  }

  /** Get the coordinate system which defines the rotations and translations. */

  public CoordinateSystem getCoordinates()
  {
    return coords;
  }

  /** Set the coordinate system which defines the rotations and translations. */

  public void setCoordinates(CoordinateSystem coords)
  {
    this.coords = coords;
    updateTransforms();
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
    updateTransforms();
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
    updateTransforms();
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
    updateTransforms();
  }

  /* Calculate the transform matrices. */

  private void updateTransforms()
  {
    trans = Mat4.scale(xscale, yscale, zscale).times(coords.fromLocal());
    gradTrans = coords.toLocal().times(Mat4.scale(xscale, yscale, zscale));
  }

  /* New point, so the value will need to be recalculated. */

  @Override
  public void init(PointInfo p)
  {
    point = p;
    valueOk = errorOk = gradOk = false;
  }

  /* Calculate the average value of an output. */

  @Override
  public double getAverageValue(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      {
        v.x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
        v.y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
        v.z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);
        trans.transform(v);
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

  /* Calculate the error of an output. */

  @Override
  public double getValueError(int which, double blur)
  {
    if (!errorOk || blur != lastBlur)
      {
        tempVec.x = (linkFrom[0] == null) ? point.xsize*0.5+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
        tempVec.y = (linkFrom[1] == null) ? point.ysize*0.5+blur : linkFrom[1].getValueError(linkFromIndex[1], blur);
        tempVec.z = (linkFrom[2] == null) ? point.zsize*0.5+blur : linkFrom[2].getValueError(linkFromIndex[2], blur);
        trans.transformDirection(tempVec);
        tempVec.x = Math.abs(tempVec.x);
        tempVec.y = Math.abs(tempVec.y);
        tempVec.z = Math.abs(tempVec.z);
        errorOk = true;
        lastBlur = blur;
      }
    if (which == 0)
      return tempVec.x;
    else if (which == 1)
      return tempVec.y;
    else
      return tempVec.z;
  }

  /* Calculate the gradient of an output. */

  @Override
  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!gradOk || blur != lastBlur)
      {
        if (linkFrom[0] == null)
          {
            grad1.set(trans.m11, 0.0, 0.0);
            grad2.set(trans.m21, 0.0, 0.0);
            grad3.set(trans.m31, 0.0, 0.0);
          }
        else
          {
            linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
            grad1.x = trans.m11*grad.x;
            grad1.y = trans.m11*grad.y;
            grad1.z = trans.m11*grad.z;
            grad2.x = trans.m21*grad.x;
            grad2.y = trans.m21*grad.y;
            grad2.z = trans.m21*grad.z;
            grad3.x = trans.m31*grad.x;
            grad3.y = trans.m31*grad.y;
            grad3.z = trans.m31*grad.z;
          }
        if (linkFrom[1] == null)
          {
            grad1.y += trans.m12;
            grad2.y += trans.m22;
            grad3.y += trans.m32;
          }
        else
          {
            linkFrom[1].getValueGradient(linkFromIndex[1], grad, blur);
            grad1.x += trans.m12*grad.x;
            grad1.y += trans.m12*grad.y;
            grad1.z += trans.m12*grad.z;
            grad2.x += trans.m22*grad.x;
            grad2.y += trans.m22*grad.y;
            grad2.z += trans.m22*grad.z;
            grad3.x += trans.m32*grad.x;
            grad3.y += trans.m32*grad.y;
            grad3.z += trans.m32*grad.z;
          }
        if (linkFrom[2] == null)
          {
            grad1.z += trans.m13;
            grad2.z += trans.m23;
            grad3.z += trans.m33;
          }
        else
          {
            linkFrom[2].getValueGradient(linkFromIndex[2], grad, blur);
            grad1.x += trans.m13*grad.x;
            grad1.y += trans.m13*grad.y;
            grad1.z += trans.m13*grad.z;
            grad2.x += trans.m23*grad.x;
            grad2.y += trans.m23*grad.y;
            grad2.z += trans.m23*grad.z;
            grad3.x += trans.m33*grad.x;
            grad3.y += trans.m33*grad.y;
            grad3.z += trans.m33*grad.z;
          }
        gradOk = true;
        lastBlur = blur;
      }
    if (which == 0)
      grad.set(grad1);
    else if (which == 1)
      grad.set(grad2);
    else
      grad.set(grad3);
  }

  /* Allow the user to set the parameters. */

  @Override
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final Vec3 orig = coords.getOrigin();
    final double angles[] = coords.getRotationAngles();
    final TransformDialog dlg = new TransformDialog(editor.getParentFrame(), Translate.text("selectTransformProperties"),
        new double [] {orig.x, orig.y, orig.z, angles[0], angles[1], angles[2],
        xscale, yscale, zscale}, true, false, false);
    dlg.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        double values[] = dlg.getValues();
        if (!Double.isNaN(values[0]))
          orig.x = values[0];
        if (!Double.isNaN(values[1]))
          orig.y = values[1];
        if (!Double.isNaN(values[2]))
          orig.z = values[2];
        if (!Double.isNaN(values[3]))
          angles[0] = values[3];
        if (!Double.isNaN(values[4]))
          angles[1] = values[4];
        if (!Double.isNaN(values[5]))
          angles[2] = values[5];
        if (!Double.isNaN(values[6]))
          xscale = values[6];
        if (!Double.isNaN(values[7]))
          yscale = values[7];
        if (!Double.isNaN(values[8]))
          zscale = values[8];
        coords.setOrigin(orig);
        coords.setOrientation(angles[0], angles[1], angles[2]);
        updateTransforms();
        editor.updatePreview();
      }
    });
    dlg.setVisible(true);
    return dlg.clickedOk();
  }

  /* Create a duplicate of this module. */

  @Override
  public Module duplicate()
  {
    TransformModule mod = new TransformModule(new Point(bounds.x, bounds.y));

    mod.coords = coords.duplicate();
    mod.xscale = xscale;
    mod.yscale = yscale;
    mod.zscale = zscale;
    mod.updateTransforms();
    return mod;
  }

  /* Write out the parameters. */

  @Override
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    coords.writeToFile(out);
    out.writeDouble(xscale);
    out.writeDouble(yscale);
    out.writeDouble(zscale);
  }

  /* Read in the parameters. */

  @Override
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    coords = new CoordinateSystem(in);
    xscale = in.readDouble();
    yscale = in.readDouble();
    zscale = in.readDouble();
    updateTransforms();
  }
}

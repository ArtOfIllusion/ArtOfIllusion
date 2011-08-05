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
import java.util.*;

/** This is a Module which generates a pattern based on Steven Worley's cellular texture
    basis function. */

public class CellsModule extends Module
{
  private boolean valueOk[], gradOk[], used2ThisTime, used2LastTime;
  private double value[], value1[], error, cell, lastBlur;
  private Vec3 gradient[], gradient1[], tempVec;
  private int id[], id1[];
  private PointInfo point;
  private Cells cells;
  private Random random;
  
  public CellsModule(Point position)
  {
    super(Translate.text("menu.cellsModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Cell"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Distance 1"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Distance 2"})}, 
      position);
    gradient = new Vec3 [] {new Vec3(), new Vec3()};
    gradient1 = new Vec3 [] {gradient[0]};
    value = new double [2];
    value1 = new double [1];
    id = new int [2];
    id1 = new int [1];
    valueOk = new boolean [3];
    gradOk = new boolean [3];
    tempVec = new Vec3();
    cells = new Cells();
    random = new FastRandom(0);
  }
  
  /** Get the metric to use for the cells function.  This is one of the constants defined in
      the Cells class. */
  
  public int getMetric()
  {
    return cells.getMetric();
  }

  /** Set the metric to use for the cells function.  This should be one of the constants defined in
      the Cells class. */
  
  public void setMetric(int m)
  {
    cells.setMetric(m);
  }

  /** New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    if (valueOk[0])
      used2LastTime = used2ThisTime;
    point = p;
    valueOk[0] = valueOk[1] = valueOk[2] = gradOk[1] = gradOk[2] = false;
    used2ThisTime = false;
  }
  
  /** Calculate the function. */
  
  private void calcValues(int which, double blur)
  {
    double xsize = (linkFrom[0] == null) ? 0.5*point.xsize+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double ysize = (linkFrom[1] == null) ? 0.5*point.ysize+blur : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double zsize = (linkFrom[2] == null) ? 0.5*point.zsize+blur : linkFrom[2].getValueError(linkFromIndex[2], blur);

    tempVec.x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    tempVec.y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    tempVec.z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);    
    
    // It's faster to only calculate input 1.  If outputs 0 and 2 weren't
    // needed for the last point, guess that it won't be needed this time either.
    
    if (which == 0 || which == 2 || used2LastTime)
      {
        cells.calcFunctions(tempVec, value, gradient, id);
        valueOk[0] = valueOk[1] = valueOk[2] = true;
      }
    else
      {
        cells.calcFunctions(tempVec, value1, gradient1, id1);
        value[0] = value1[0];
        id[0] = id1[0];
        gradient[0].set(gradient1[0]);
        valueOk[1] = true;
      }
    gradOk[1] = gradOk[2] = false;
    error = Math.max(Math.max(xsize, ysize), zsize);
    random.setSeed(id[0]);
    random.nextDouble();
    cell = random.nextDouble();
    lastBlur = blur;
    if (which == 0 || which == 2)
      used2ThisTime = true;
  }
  
  /** Calculate the average value of an output. */

  public double getAverageValue(int which, double blur)
  {
    if (!valueOk[which] || blur != lastBlur)
      calcValues(which, blur);
    if (which > 0)
      return value[which-1];
    double diff = value[1]-value[0];
    if (diff >= error)
      return cell;
    random.setSeed(id[1]);
    random.nextDouble();
    double cell2 = random.nextDouble();
    double weight = 0.5+0.5*diff/error;
    return weight*cell+(1.0-weight)*cell2;
  }
  
  /** Calculate the error of an output. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk[which] || blur != lastBlur)
      calcValues(which, blur);
    if (which == 0)
      {
        double diff = value[1]-value[0];
        if (diff >= error)
          return 0.0;
        random.setSeed(id[1]);
        random.nextDouble();
        double cell2 = random.nextDouble();
        return 0.5*Math.abs(cell-cell2);
      }
    return error;
  }
  
  /** Calculate the gradient of an output. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (which == 0)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
    if (!valueOk[which] || blur != lastBlur)
      calcValues(which, blur);
    if (gradOk[which])
      {
        grad.set(gradient[which-1]);
        return;
      }
    Vec3 g = gradient[which-1];
    double dx = g.x, dy = g.y, dz = g.z;
    if (dx != 0.0)
      {
        if (linkFrom[0] == null)
          g.set(dx, 0.0, 0.0);
        else
          {
            linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
            g.x = dx*grad.x;
            g.y = dx*grad.y;
            g.z = dx*grad.z;
          }
      }
    else
      g.set(0.0, 0.0, 0.0);
    if (dy != 0.0)
      {
        if (linkFrom[1] == null)
          g.y += dy;
        else
          {
            linkFrom[1].getValueGradient(linkFromIndex[1], grad, blur);
            g.x += dy*grad.x;
            g.y += dy*grad.y;
            g.z += dy*grad.z;
          }
      }
    if (dz != 0.0)
      {
        if (linkFrom[2] == null)
          g.z += dz;
        else
          {
            linkFrom[2].getValueGradient(linkFromIndex[2], grad, blur);
            g.x += dz*grad.x;
            g.y += dz*grad.y;
            g.z += dz*grad.z;
          }
      }
    gradOk[which] = true;
    grad.set(g);
  }
  
  /** Allow the user to set the parameters. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final RadioButtonGroup metricGroup = new RadioButtonGroup();
    int metric = cells.getMetric();
    final BRadioButton euclidBox = new BRadioButton("Euclidean", metric == Cells.EUCLIDEAN, metricGroup);
    final BRadioButton cityBox = new BRadioButton("City Block", metric == Cells.CITY_BLOCK, metricGroup);
    final BRadioButton chessBox = new BRadioButton("Chess Board", metric == Cells.CHESS_BOARD, metricGroup);
    metricGroup.addEventLink(SelectionChangedEvent.class, new Object() {
      void processEvent()
      {
        if (metricGroup.getSelection() == euclidBox)
          cells.setMetric(Cells.EUCLIDEAN);
        else if (metricGroup.getSelection() == cityBox)
          cells.setMetric(Cells.CITY_BLOCK);
        else if (metricGroup.getSelection() == chessBox)
          cells.setMetric(Cells.CHESS_BOARD);
        editor.updatePreview();
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), "Select which distance metric to use:",
      new Widget [] {euclidBox, cityBox, chessBox}, new String [] {"", "", ""});
    if (!dlg.clickedOk())
      return false;
    if (metricGroup.getSelection() == euclidBox)
      cells.setMetric(Cells.EUCLIDEAN);
    else if (metricGroup.getSelection() == cityBox)
      cells.setMetric(Cells.CITY_BLOCK);
    else if (metricGroup.getSelection() == chessBox)
      cells.setMetric(Cells.CHESS_BOARD);
    return true;
  }

  /** Create a duplicate of this module. */

  public Module duplicate()
  {
    CellsModule module = new CellsModule(new Point(bounds.x, bounds.y));
    module.setMetric(cells.getMetric());
    return module;
  }

  /** Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(cells.getMetric());
  }
  
  /** Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    cells.setMetric(in.readInt());
  }
}

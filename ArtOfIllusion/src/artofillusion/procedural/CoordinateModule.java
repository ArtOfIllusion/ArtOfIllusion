/* Copyright (C) 2000 by Peter Eastman

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
import java.awt.*;
import java.io.*;

/** This is a Module which outputs a coordinate (x, y, z, or t). */

public class CoordinateModule extends Module
{
  int coordinate;
  PointInfo point;
  
  public static final int X = 0;
  public static final int Y = 1;
  public static final int Z = 2;
  public static final int T = 3;
  public static final String COORD_NAME[] = new String [] {"X", "Y", "Z", Translate.text("Time")};

  public CoordinateModule(Point position)
  {
    this(position, X);
  }

  public CoordinateModule(Point position, int coordinate)
  {
    super(COORD_NAME[coordinate], new IOPort [] {}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {COORD_NAME[coordinate]})}, 
      position);
    this.coordinate = coordinate;
  }

  /* Set the coordinate which this module outputs. */
  
  public void setCoordinate(int coordinate)
  {
    this.coordinate = coordinate;
    name = COORD_NAME[coordinate];
    output[0].setDescription(new String [] {COORD_NAME[coordinate]});
    layout();
  }

  /* Cache the PointInfo object to have access to the coordinates later on. */

  public void init(PointInfo p)
  {
    point = p;
  }

  /* This module outputs the value of the specified coordinate. */
  
  public double getAverageValue(int which, double blur)
  {
    switch (coordinate)
    {
      case X:
	return point.x;
      case Y:
	return point.y;
      case Z:
	return point.z;
      default:
        return point.t;
    }
  }

  /* Return the error in the specified coordinate. */

  public double getValueError(int which, double blur)
  {
    switch (coordinate)
    {
      case X:
	return 0.5*point.xsize+blur;
      case Y:
	return 0.5*point.ysize+blur;
      case Z:
	return 0.5*point.zsize+blur;
      default:
        return 0.0;
    }
  }

  /* The gradient is simply linear in the appropriate coordinate. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    switch (coordinate)
    {
      case X:
	grad.set(1.0, 0.0, 0.0);
	break;
      case Y:
	grad.set(0.0, 1.0, 0.0);
	break;
      case Z:
	grad.set(0.0, 0.0, 1.0);
	break;
      default:
	grad.set(0.0, 0.0, 0.0);
    }
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    CoordinateModule mod = new CoordinateModule(new Point(bounds.x, bounds.y), coordinate);
    
    mod.layout();
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(coordinate);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    coordinate = in.readInt();
    name = COORD_NAME[coordinate];
    output[0].setDescription(new String [] {COORD_NAME[coordinate]});
    layout();
  }
}

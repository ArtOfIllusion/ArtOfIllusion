/* Copyright (C) 2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import junit.framework.*;
import artofillusion.procedural.*;

import java.awt.*;

public class TestCompareModule extends TestCase
{
  public void testCompare()
  {
    CompareModule module = new CompareModule(new Point());
    module.linkFrom[0] = new CoordinateModule(new Point(), CoordinateModule.X);
    module.linkFrom[1] = new CoordinateModule(new Point(), CoordinateModule.Y);

    // Test exact comparisons.
    
    checkValue(module, 0.0, 0.0, 0.0, 0.0, 0.0);
    checkValue(module, 0.0, 1.0, 0.0, 0.0, 0.0);
    checkValue(module, 1.0, 0.0, 0.0, 0.0, 1.0);
    checkValue(module, 1.0, 1.0, 0.0, 0.0, 0.0);

    // Test comparisons where the difference is greater than the error.

    checkValue(module, 0.0, 1.0, 0.1, 0.1, 0.0);
    checkValue(module, 1.0, 0.0, 0.1, 0.1, 1.0);

    // Test having the difference in the error range.

    checkValue(module, 0.0, 1.0, 1.5, 1.5, 0.1);
    checkValue(module, 1.0, 0.0, 1.5, 1.5, 0.9);
    checkValue(module, 0.0, 0.0, 1.5, 1.5, 0.5);
  }

  private void checkValue(Module module, double x, double y, double xsize, double ysize, double expected)
  {
    PointInfo info = new PointInfo();
    info.x = x;
    info.xsize = xsize;
    info.y = y;
    info.ysize = ysize;
    module.init(info);
    module.linkFrom[0].init(info);
    module.linkFrom[1].init(info);
    assertEquals(expected, module.getAverageValue(0, 0.0), 1e-8);
  }
}

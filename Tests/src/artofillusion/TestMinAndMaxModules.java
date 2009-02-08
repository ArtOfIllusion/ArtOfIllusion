/* Copyright (C) 2009 by Peter Eastman

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
import java.util.*;

public class TestMinAndMaxModules extends TestCase
{
  public void testMinAndMax()
  {
    MinModule min = new MinModule(new Point());
    min.linkFrom[0] = new CoordinateModule(new Point(), CoordinateModule.X);
    min.linkFrom[1] = new CoordinateModule(new Point(), CoordinateModule.Y);
    MaxModule max = new MaxModule(new Point());
    max.linkFrom[0] = new CoordinateModule(new Point(), CoordinateModule.X);
    max.linkFrom[1] = new CoordinateModule(new Point(), CoordinateModule.Y);

    // Test cases where the values are well separated.

    checkValue(min, 0.0, 5.0, 1.0, 2.0, 0.0, 1.0, 1.0);
    checkValue(max, 0.0, 5.0, 1.0, 2.0, 5.0, 2.0, 2.0);
    checkValue(min, 1.0, 0.0, 0.1, 0.2, 0.0, 0.2, 0.2);
    checkValue(max, 1.0, 0.0, 0.1, 0.2, 1.0, 0.1, 0.1);

    // Test cases where they are equal.

    checkValue(min, 2.0, 2.0, 0.2, 0.1, 2.0, 0.1, 0.1);
    checkValue(max, 2.0, 2.0, 0.2, 0.1, 2.0, 0.1, 0.1);
    checkValue(min, 2.0, 2.0, 0.1, 0.2, 2.0, 0.1, 0.1);
    checkValue(max, 2.0, 2.0, 0.1, 0.2, 2.0, 0.1, 0.1);

    // Now try lots of randomly chosen values.

    Random r = new Random(0);
    for (int i = 0; i < 100; i++)
    {
      double x = r.nextDouble();
      double y = r.nextDouble();
      double xerror = r.nextDouble();
      double yerror = r.nextDouble();
      if (x < y)
      {
        checkValue(min, x, y, xerror, yerror, x, 0.0, xerror);
        checkValue(max, x, y, xerror, yerror, y, 0.0, yerror);
      }
      else
      {
        checkValue(min, x, y, xerror, yerror, y, 0.0, yerror);
        checkValue(max, x, y, xerror, yerror, x, 0.0, xerror);
      }
    }
  }

  private void checkValue(Module module, double x, double y, double xsize, double ysize, double expectedValue, double minError, double maxError)
  {
    PointInfo info = new PointInfo();
    info.x = x;
    info.xsize = xsize;
    info.y = y;
    info.ysize = ysize;
    module.init(info);
    module.linkFrom[0].init(info);
    module.linkFrom[1].init(info);
    assertEquals(expectedValue, module.getAverageValue(0, 0.0), 0.0);
    assert(module.getValueError(0, 0.0) >= minError);
    assert(module.getValueError(0, 0.0) <= maxError);
  }
}

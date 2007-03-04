/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import junit.framework.*;
import artofillusion.math.*;

public class TestRGBColor extends TestCase
{
  /**
   * Test converting to and from ERGB format.
   */

  public void testERGB()
  {
    RGBColor c1 = new RGBColor(), c2 = new RGBColor();
    c1.setRGB(0.0f, 0.0f, 0.0f);
    c2.setERGB(c1.getERGB());
    assertColorsEquals(c1, c2, 0.0f);
    c1.setRGB(1.0f, 1.0f, 1.0f);
    c2.setERGB(c1.getERGB());
    assertColorsEquals(c1, c2, 0.0f);
    for (int i = 0; i < 1000; i++)
    {
      c1.setRGB(Math.random(), Math.random(), Math.random());
      c1.scale(10.0*Math.random());
      c2.setERGB(c1.getERGB());
      assertColorsEquals(c1, c2, c1.getMaxComponent()/128);
    }
  }

  private void assertColorsEquals(RGBColor c1, RGBColor c2, float tol)
  {
    assertEquals(c1.getRed(), c2.getRed(), tol);
    assertEquals(c1.getGreen(), c2.getGreen(), tol);
    assertEquals(c1.getBlue(), c2.getBlue(), tol);
  }
}

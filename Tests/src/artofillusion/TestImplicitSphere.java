/* Copyright (C) 2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import junit.framework.*;

public class TestImplicitSphere extends TestCase
{
  private void validateSphere(ImplicitSphere sphere)
  {
    double radius = sphere.getRadius();
    double influence = Math.max(radius, sphere.getInfluenceRadius());
    Vec3 grad = new Vec3();
    for (int i = 0; i < 10; i++)
    {
      double x = 2*radius*(Math.random()-0.5);
      double y = 2*radius*(Math.random()-0.5);
      double z = 2*radius*(Math.random()-0.5);

      // Check the value.

      double value = sphere.getFieldValue(x, y, z, 0, 0);
      double r = Math.sqrt(x*x+y*y+z*z);
      if (r < radius)
        assertTrue(value > 1);
      else if (r > radius)
        assertTrue(value < 1);
      if (r > influence)
        assertTrue(value == 0);

      // Check the gradient.

      sphere.getFieldGradient(x, y, z, 0, 0, grad);
      double step = radius*1e-3;
      double vx1 = sphere.getFieldValue(x-step, y, z, 0, 0);
      double vx2 = sphere.getFieldValue(x+step, y, z, 0, 0);
      double vy1 = sphere.getFieldValue(x, y-step, z, 0, 0);
      double vy2 = sphere.getFieldValue(x, y+step, z, 0, 0);
      double vz1 = sphere.getFieldValue(x, y, z-step, 0, 0);
      double vz2 = sphere.getFieldValue(x, y, z+step, 0, 0);
      assertEquals(0.5*(vx2-vx1)/step, grad.x, 1e-2*value);
      assertEquals(0.5*(vy2-vy1)/step, grad.y, 1e-2*value);
      assertEquals(0.5*(vz2-vz1)/step, grad.z, 1e-2*value);
    }
  }

  public void testSphere()
  {
    for (int i = 0; i < 20; i++)
    {
      double r1 = 0.1+Math.random();
      double r2 = 0.1+Math.random();
      ImplicitSphere sphere = new ImplicitSphere(Math.min(r1, r2), Math.max(r1, r2));
      validateSphere(sphere);
    }
  }
}

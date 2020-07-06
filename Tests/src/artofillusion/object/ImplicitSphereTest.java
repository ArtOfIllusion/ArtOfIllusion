/* Copyright (C) 2013 by Peter Eastman
   Changes copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;
import artofillusion.math.*;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImplicitSphereTest
{ 

  private static ImplicitSphere[] testSpheres = new ImplicitSphere[20];
  private static Vec3[] samplePoints = new Vec3[100];

  @BeforeClass
  public static void setup_spheres_and_test_points()
  {
    for (int sphere = 0; sphere < testSpheres.length; sphere++)
    {
      double r1 = 0.1 + Math.random();
      double r2 = 0.1 + Math.random();
      testSpheres[sphere] = new ImplicitSphere(Math.min(r1, r2),
                                               Math.max(r1, r2));
    }

    for (int point = 0; point < samplePoints.length; point++)
    {
      /* Factor 2.05 is to cover the entire possible size range of
       * test spheres (-1.1 to 1.1) Unlike original code, we're not
       * scaling the test points to a specific sphere.
       */
      samplePoints[point] = new Vec3(2.05*(Math.random() - 0.5),
                                     2.05*(Math.random() - 0.5),
                                     2.05*(Math.random() - 0.5));
    }
  }

  private void validateSphere(ImplicitSphere sphere)
  {
    double radius = sphere.getRadius();
    double influence = Math.max(radius, sphere.getInfluenceRadius());
    Vec3 grad = new Vec3();
    for (Vec3 point: samplePoints)
    {

      // Check the value.

      double value = sphere.getFieldValue(point.x,
                                          point.y,
					  point.z, 0, 0);
      double r = point.length();
      if (r < radius)
        assertTrue(value > 1);
      else if (r > radius)
        assertTrue(value < 1);
      if (r > influence)
        assertTrue(value == 0);

      // Check the gradient.

      sphere.getFieldGradient(point.x, point.y, point.z, 0, 0, grad);
      double step = radius*1e-3;
      double vx1 = sphere.getFieldValue(point.x-step, point.y, point.z, 0, 0);
      double vx2 = sphere.getFieldValue(point.x+step, point.y, point.z, 0, 0);
      double vy1 = sphere.getFieldValue(point.x, point.y-step, point.z, 0, 0);
      double vy2 = sphere.getFieldValue(point.x, point.y+step, point.z, 0, 0);
      double vz1 = sphere.getFieldValue(point.x, point.y, point.z-step, 0, 0);
      double vz2 = sphere.getFieldValue(point.x, point.y, point.z+step, 0, 0);
      assertEquals(0.5*(vx2-vx1)/step, grad.x, 1e-2*value);
      assertEquals(0.5*(vy2-vy1)/step, grad.y, 1e-2*value);
      assertEquals(0.5*(vz2-vz1)/step, grad.z, 1e-2*value);
    }
  }

  @Test
  public void testSphere()
  {
    for (ImplicitSphere sphere: testSpheres)
    {
      validateSphere(sphere);
    }
  }
}

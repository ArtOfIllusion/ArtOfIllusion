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

  @Test
  public void point_inside_radius_is_greater_than_1()
  {
    for (ImplicitSphere sphere: testSpheres)
    {
      for (Vec3 point: samplePoints)
      {
        if (point.length() < sphere.getRadius())
           assertTrue(representTestPair(point, sphere),
                      sphere.getFieldValue(point.x, point.y, point.z, 0, 0) > 1);
      }
    }
  }

  @Test
  public void point_outside_influence_radius_is_0()
  {
    for (ImplicitSphere sphere: testSpheres)
    {
      for (Vec3 point: samplePoints)
      {
        if (point.length() > sphere.getInfluenceRadius())
        {
          assertTrue(representTestPair(point, sphere),
                     sphere.getFieldValue(point.x, point.y, point.z, 0, 0) == 0);
        }
      }
    }
  }

  @Test
  public void point_between_radius_and_influence_is_between_0_and_1()
  {
    for (ImplicitSphere sphere: testSpheres)
    {
      for (Vec3 point: samplePoints)
      {
        if (point.length() <= sphere.getInfluenceRadius()
            && point.length() >= sphere.getRadius())
        {
           double value = sphere.getFieldValue(point.x, point.y, point.z, 0, 0);
           assertTrue(representTestPair(point, sphere) + "\nValue:" + value,
                     1 > value && value > 0);
        }
      }
    }
  }

  @Test
  public void gradient_estimate_within_delta()
  {
    Vec3 grad = new Vec3();
    for (ImplicitSphere sphere: testSpheres)
    {
      for (Vec3 point: samplePoints)
      {
        double radius = sphere.getRadius();
        double value = sphere.getFieldValue(point.x, point.y, point.z, 0, 0);
        sphere.getFieldGradient(point.x, point.y, point.z, 0, 0, grad);
        double step = radius*1e-4;
        double vx1 = sphere.getFieldValue(point.x-step, point.y, point.z, 0, 0);
        double vx2 = sphere.getFieldValue(point.x+step, point.y, point.z, 0, 0);
        double vy1 = sphere.getFieldValue(point.x, point.y-step, point.z, 0, 0);
        double vy2 = sphere.getFieldValue(point.x, point.y+step, point.z, 0, 0);
        double vz1 = sphere.getFieldValue(point.x, point.y, point.z-step, 0, 0);
        double vz2 = sphere.getFieldValue(point.x, point.y, point.z+step, 0, 0);
        double gradient = (vx2-vx1)/step;
	assertEquals("X-grad" + representTestPair(point, sphere),
                     0.5*gradient, grad.x, 1e-5*Math.abs(grad.x));
        gradient = (vy2-vy1)/step;
	assertEquals("Y-grad" + representTestPair(point, sphere),
                     0.5*gradient, grad.y, 1e-5*Math.abs(grad.y));
        gradient = (vz2-vz1)/step;
	assertEquals("Z-grad" + representTestPair(point, sphere),
                     0.5*gradient, grad.z, 1e-5*Math.abs(grad.z));
      }
    }
  }

  private String representTestPair(Vec3 point, ImplicitSphere sphere)
  {
    return "\nTest Point:\n" +
	   "\nX:" + point.x +
	   "\nY:" + point.y +
	   "\nZ:" + point.z +
	   "\nLength:" + point.length() +
           "\n\nImplicit Sphere:\n" +
	   "\nRadius:" + sphere.getRadius() +
	   "\nInfluence:" + sphere.getInfluenceRadius();
  }
}

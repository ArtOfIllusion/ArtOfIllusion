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

import java.util.*;

public class ImplicitSphereTest
{ 

  private static int testSpheres = 20;
  private static int samplePoints = 100;
  private static List<TestPair> pairs = new ArrayList<TestPair>();

  private static class TestPair  //It was this, or abuse AbstractMap.SimpleEntry
  {
    public final ImplicitSphere sphere;
    public final Vec3 point;

    public TestPair(ImplicitSphere sp, Vec3 pnt)
    {
      sphere = sp;
      point = pnt;
    }
  }

  @BeforeClass
  public static void setup_spheres_and_test_points()
  {
    for (int sphere = 0; sphere < testSpheres; sphere++)
    {
      double r1 = 0.1 + Math.random();
      double r2 = 0.1 + Math.random();
      ImplicitSphere currentSphere = new ImplicitSphere(Math.min(r1, r2),
                                                        Math.max(r1, r2));

      for (int point = 0; point < samplePoints; point++)
      {
        /* Factor 2.05 is to cover the entire possible size range of
         * test spheres (-1.1 to 1.1) Unlike original code, we're not
         * scaling the test points to a specific sphere.
         */
        Vec3 currentPoint = new Vec3(2.05*(Math.random() - 0.5),
                                       2.05*(Math.random() - 0.5),
				       2.05*(Math.random() - 0.5));

	pairs.add(new TestPair(currentSphere, currentPoint));
      }
    }
  }

  @Test
  public void point_inside_radius_is_greater_than_1()
  {
    pairs.stream()
         .filter(p -> p.point.length() < p.sphere.getRadius())
         .forEach(p ->
           assertTrue(printPair(p),
                      p.sphere.getFieldValue(p.point.x, p.point.y,
                                             p.point.z, 0, 0) > 1));
  }

  @Test
  public void point_outside_influence_radius_is_0()
  {
    pairs.stream()
         .filter(p -> p.point.length() > p.sphere.getInfluenceRadius())
         .forEach(p ->
           assertTrue(printPair(p),
                      p.sphere.getFieldValue(p.point.x, p.point.y,
                                             p.point.z, 0, 0) == 0));
  }

  @Test
  public void point_between_radius_and_influence_is_between_0_and_1()
  {
    pairs.stream()
         .filter(p -> p.point.length() <= p.sphere.getInfluenceRadius()
                      && p.point.length() >= p.sphere.getRadius())
         .forEach(p ->	 
           {
             double value = p.sphere.getFieldValue(p.point.x, p.point.y,
                                                   p.point.z, 0, 0);
             assertTrue(printPair(p) + "\nValue:" + value, 1 > value && value > 0);
           }); 
  }

  @Test
  public void gradient_estimate_within_delta()
  {
    Vec3 grad = new Vec3();
    pairs.stream()
         .forEach(p ->
      {
        double radius = p.sphere.getRadius();
        double value = p.sphere.getFieldValue(p.point.x, p.point.y, p.point.z, 0, 0);
        p.sphere.getFieldGradient(p.point.x, p.point.y, p.point.z, 0, 0, grad);
        double step = radius*1e-4;
        double vx1 = p.sphere.getFieldValue(p.point.x-step, p.point.y, p.point.z, 0, 0);
        double vx2 = p.sphere.getFieldValue(p.point.x+step, p.point.y, p.point.z, 0, 0);
        double vy1 = p.sphere.getFieldValue(p.point.x, p.point.y-step, p.point.z, 0, 0);
        double vy2 = p.sphere.getFieldValue(p.point.x, p.point.y+step, p.point.z, 0, 0);
        double vz1 = p.sphere.getFieldValue(p.point.x, p.point.y, p.point.z-step, 0, 0);
        double vz2 = p.sphere.getFieldValue(p.point.x, p.point.y, p.point.z+step, 0, 0);
        double gradient = (vx2-vx1)/step;
	assertEquals("X-grad" + printPair(p),
                     0.5*gradient, grad.x, 1e-5*Math.abs(grad.x));
        gradient = (vy2-vy1)/step;
	assertEquals("Y-grad" + printPair(p),
                     0.5*gradient, grad.y, 1e-5*Math.abs(grad.y));
        gradient = (vz2-vz1)/step;
	assertEquals("Z-grad" + printPair(p),
                     0.5*gradient, grad.z, 1e-5*Math.abs(grad.z));
      });
  }

  private String printPair(TestPair pair)
  {
    return "\nTest Point:\n" +
	   "\nX:" + pair.point.x +
	   "\nY:" + pair.point.y +
	   "\nZ:" + pair.point.z +
	   "\nLength:" + pair.point.length() +
           "\n\nImplicit Sphere:\n" +
	   "\nRadius:" + pair.sphere.getRadius() +
	   "\nInfluence:" + pair.sphere.getInfluenceRadius();
  }
}

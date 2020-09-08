/* Copyright (C) 2013 by Peter Eastman
   Changes copyright (C) 2017 by Maksim Khramov
   Changes/refactor (C) 2020 by Lucas Stanek

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
import static java.lang.Math.*;

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
      double r1 = 0.1 + random();
      double r2 = 0.1 + random();
      ImplicitSphere currentSphere = new ImplicitSphere(min(r1, r2),
                                                        max(r1, r2));

      for (int point = 0; point < samplePoints; point++)
      {
        /* Factor 2.05 is to cover the entire possible size range of
         * test spheres (-1.1 to 1.1) 
         */
        Vec3 currentPoint = new Vec3(2.05*(random() - 0.5),
                                     2.05*(random() - 0.5),
                                     2.05*(random() - 0.5));

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
             assertTrue(printPair(p) + "\nValue:" + value,
                        1 > value && value > 0);
           }); 
  }

  @Test
  public void gradient_estimate_within_delta()
  {
    pairs.stream()
         .filter(p -> abs(p.point.length() - p.sphere.getInfluenceRadius())
                      > p.sphere.getRadius() * 1e-4)
         .forEach(p ->
           {
             Vec3 grad = new Vec3();
             p.sphere.getFieldGradient(p.point.x, p.point.y, p.point.z,
                                       0, 0, grad);
             Vec3 estGrad = estimateGradient(p);
             assertEquals("X-grad" + printPair(p),
                          0.5 * estGrad.x, grad.x, 1e-4 * abs(grad.x));
             assertEquals("Y-grad" + printPair(p),
                          0.5 * estGrad.y, grad.y, 1e-4 * abs(grad.y));
             assertEquals("Z-grad" + printPair(p),
                          0.5 * estGrad.z, grad.z, 1e-4 * abs(grad.z));
            });
  }

  /** Discontinuities near the edge of influence radius make estimating
   * the gradient difficult. For these, we just give up and make sure
   * the returned gradients are in the correct octant
   */
  @Test
  public void gradient_estimate_at_influence_edge()
  {
    pairs.stream()
         .filter(p -> abs(p.point.length() - p.sphere.getInfluenceRadius())
                      <= p.sphere.getRadius() * 1e-4)
         .forEach(p ->
           {
             Vec3 grad = new Vec3();
             p.sphere.getFieldGradient(p.point.x, p.point.y, p.point.z,
                                       0, 0, grad);
             Vec3 estGrad = estimateGradient(p);
             assertEquals("X-grad" + printPair(p),
                          signum(0.5 * estGrad.x), signum(grad.x), .01);
             assertEquals("Y-grad" + printPair(p),
                          signum(0.5 * estGrad.y), signum(grad.y), .01);
             assertEquals("Z-grad" + printPair(p),
                          signum(0.5 * estGrad.z), signum(grad.z), .01);
           });  
  }

  private Vec3 estimateGradient(TestPair pair)
  {
    ImplicitSphere sphere = pair.sphere;
    Vec3 point = pair.point;
    double step = sphere.getRadius() * 1e-4;
    double vx1 = sphere.getFieldValue(point.x-step, point.y, point.z, 0, 0);
    double vx2 = sphere.getFieldValue(point.x+step, point.y, point.z, 0, 0);
    double vy1 = sphere.getFieldValue(point.x, point.y-step, point.z, 0, 0);
    double vy2 = sphere.getFieldValue(point.x, point.y+step, point.z, 0, 0);
    double vz1 = sphere.getFieldValue(point.x, point.y, point.z-step, 0, 0);
    double vz2 = sphere.getFieldValue(point.x, point.y, point.z+step, 0, 0);
    return new Vec3((vx2-vx1)/step, (vy2-vy1)/step, (vz2-vz1)/step);
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

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
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImplicitSphereTest
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

  @Test
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
  
  @Test(expected = ClassCastException.class)
  public void testCopyImplicitSphereFromBadObject()
  {
    ImplicitSphere me = new ImplicitSphere(1, 1);
    Sphere bad = new Sphere(1,1,1);
    me.copyObject(bad);
  }
  
  
  @Test
  public void testCopyImplicitSphere()
  {
    ImplicitSphere me = new ImplicitSphere(1, 1);
    ImplicitSphere other = new ImplicitSphere(2,3);
    
    me.copyObject(other);
  }
  
  
  @Test
  public void testImplicitSphereBounds1()
  {
    ImplicitSphere sphere = new ImplicitSphere(1, 1);
    BoundingBox bb = sphere.getBounds();

    Assert.assertEquals(-1.0f, bb.minx, 0);
    Assert.assertEquals(-1.0f, bb.miny, 0);
    Assert.assertEquals(-1.0f, bb.minz, 0);
    Assert.assertEquals(1.0f, bb.maxx, 0);
    Assert.assertEquals(1.0f, bb.maxy, 0);
    Assert.assertEquals(1.0f, bb.maxz, 0);
  }
  
  @Test
  public void testImplicitSphereBounds2()
  {
    ImplicitSphere sphere = new ImplicitSphere(1, 2);
    BoundingBox bb = sphere.getBounds();

    Assert.assertEquals(-2.0f, bb.minx, 0);
    Assert.assertEquals(-2.0f, bb.miny, 0);
    Assert.assertEquals(-2.0f, bb.minz, 0);
    Assert.assertEquals(2.0f, bb.maxx, 0);
    Assert.assertEquals(2.0f, bb.maxy, 0);
    Assert.assertEquals(2.0f, bb.maxz, 0);
  }
  
}

/* Copyright (C) 2016 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author makiam
 */
public class CoordinateSystemEqualityTest
{
  
  public CoordinateSystemEqualityTest()
  {
  }
  
  @Before
  public void setUp()
  {
  }
  
  @Test
  public void testCSAreEquals() 
  {
    CoordinateSystem cs1 = new CoordinateSystem();
    CoordinateSystem cs2 = new CoordinateSystem();
    
    assertTrue(cs1.equals(cs2));
  }
  
  @Test
  public void testCustomCSAreEquals() 
  {
    Vec3 vector = new Vec3();
    CoordinateSystem cs1 = new CoordinateSystem(vector,vector,vector);
    CoordinateSystem cs2 = new CoordinateSystem(vector,vector,vector);
    
    assertTrue(cs1.equals(cs2));
  }
  
  @Test
  public void testCustomCSAreEquals2() 
  {
    Vec3 vector = new Vec3(1.0, 1.0, 1.0);
    CoordinateSystem cs1 = new CoordinateSystem(vector,vector,vector);
    CoordinateSystem cs2 = new CoordinateSystem(vector,vector,vector);
    
    assertTrue(cs1.equals(cs2));
  }
  
  @Test
  public void testCSNotEqualsByOrig() 
  {
    CoordinateSystem cs1 = new CoordinateSystem();
    CoordinateSystem cs2 = new CoordinateSystem();
    cs2.orig.x += 1.0;
    assertFalse(cs1.equals(cs2));    
  }
  
  @Test
  public void testCSNotEqualsByZDir() 
  {
    CoordinateSystem cs1 = new CoordinateSystem();
    CoordinateSystem cs2 = new CoordinateSystem();
    cs2.zdir.x += 1.0;
    assertFalse(cs1.equals(cs2));    
  }
  
  @Test
  public void testCSNotEqualsByUpDir() 
  {
    CoordinateSystem cs1 = new CoordinateSystem();
    CoordinateSystem cs2 = new CoordinateSystem();
    cs2.updir.x += 1.0;
    assertFalse(cs1.equals(cs2));    
  } 
}

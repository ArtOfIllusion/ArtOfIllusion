/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

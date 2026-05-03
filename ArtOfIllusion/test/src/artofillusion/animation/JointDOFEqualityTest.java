/* Copyright (C) 2016 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.math.CoordinateSystem;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author makiam
 * 
 */
public class JointDOFEqualityTest
{
  private Joint source;
  public JointDOFEqualityTest()
  {
  }
  
  @Before
  public void setUp()
  {
    CoordinateSystem cs = new CoordinateSystem();
    source = new Joint(cs,null,"Origin1");    
  }

  
  @Test
  public void testDOFAreEquals() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    
    assertTrue(sourceDof.equals(targetDof));
  }
  
  @Test
  public void testDOFAreNotEqualsByFixed() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.fixed = !targetDof.fixed;
    
    assertFalse(sourceDof.equals(targetDof));
  }

  @Test
  public void testDOFAreNotEqualsByComfort() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.comfort = !targetDof.comfort;
    
    assertFalse(sourceDof.equals(targetDof));
  }

  @Test
  public void testDOFAreNotEqualsByLoop() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.loop = !targetDof.loop;
    
    assertFalse(sourceDof.equals(targetDof));
  }
  
  @Test
  public void testDOFAreNotEqualsByMin() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.min = targetDof.min - 1.0;
    
    assertFalse(sourceDof.equals(targetDof));
  }

  @Test
  public void testDOFAreNotEqualsByMinComfort() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.minComfort = targetDof.minComfort - 1.0;
    
    assertFalse(sourceDof.equals(targetDof));
  }
  
  @Test
  public void testDOFAreNotEqualsByMax() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.max = targetDof.max + 1.0;
    
    assertFalse(sourceDof.equals(targetDof));
  }
  
  @Test
  public void testDOFAreNotEqualsByMaxComfort() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.maxComfort = targetDof.maxComfort + 1.0;
    
    assertFalse(sourceDof.equals(targetDof));
  }  

  @Test
  public void testDOFAreNotEqualsByStiffness() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.stiffness = targetDof.stiffness + 1.0;
    
    assertFalse(sourceDof.equals(targetDof));
  }  
  
  @Test
  public void testDOFAreNotEqualsByPos() 
  {
    Joint.DOF sourceDof = source.angle1;
    Joint.DOF targetDof = sourceDof.duplicate();
    targetDof.pos = targetDof.pos + 1.0;
    
    assertFalse(sourceDof.equals(targetDof));
  }  
}

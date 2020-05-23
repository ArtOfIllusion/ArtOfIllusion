/* Copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class ProceduralMaterial3DTest
{

    private ProceduralMaterial3D material;
    
    @org.junit.Before
    public void prepareTest()
    {
      material = new ProceduralMaterial3D();
    }
    
    @Test
    public void testGetTrackAllowViewAngle(){
      Assert.assertFalse(material.allowViewAngle());
      
    }
    
    @Test
    public void testGetTrackAllowParameters(){
      Assert.assertFalse(material.allowParameters());
      
    }
    
    @Test
    public void testGetTrackAllowChangeName(){
      Assert.assertTrue(material.canEditName());
      
    }   
}

/* Copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class ProceduralTexture2DTest
{
    private ProceduralTexture2D texture;
    
    @org.junit.Before
    public void prepareTest()
    {
      texture = new ProceduralTexture2D();
    }
    
    @Test
    public void testGetTrackAllowViewAngle(){
      Assert.assertTrue(texture.allowViewAngle());
      
    }
    
    @Test
    public void testGetTrackAllowParameters(){
      Assert.assertTrue(texture.allowParameters());
      
    }
    
    @Test
    public void testGetTrackAllowChangeName(){
      Assert.assertTrue(texture.canEditName());
      
    }  
}

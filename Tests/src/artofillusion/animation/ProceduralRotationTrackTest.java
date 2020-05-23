/* Copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.math.CoordinateSystem;
import artofillusion.object.Cube;
import artofillusion.object.ObjectInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class ProceduralRotationTrackTest
{
    private final ObjectInfo trackTarget = new ObjectInfo(new Cube(1, 1, 1), new CoordinateSystem(), "TestTarget");
    private ProceduralRotationTrack track;
    
    @org.junit.Before
    public void prepareTest()
    {
      track = new ProceduralRotationTrack(trackTarget);
    }
    
    @Test
    public void testGetTrackPreview(){
      Assert.assertNull(track.getPreview(null));
    }
    
    @Test
    public void testGetTrackAllowViewAngle(){
      Assert.assertFalse(track.allowViewAngle());
      
    }
    
    @Test
    public void testGetTrackAllowParameters(){
      Assert.assertTrue(track.allowParameters());
      
    }
    
    @Test
    public void testGetTrackAllowChangeName(){
      Assert.assertTrue(track.canEditName());
      
    } 
}

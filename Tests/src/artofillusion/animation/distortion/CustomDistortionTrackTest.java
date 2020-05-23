/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.animation.distortion;

import artofillusion.math.CoordinateSystem;
import artofillusion.object.Cube;
import artofillusion.object.ObjectInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class CustomDistortionTrackTest
{
    private final ObjectInfo trackTarget = new ObjectInfo(new Cube(1, 1, 1), new CoordinateSystem(), "TestTarget");
    private CustomDistortionTrack track;
    
    @org.junit.Before
    public void prepareTest()
    {
      track = new CustomDistortionTrack(trackTarget);
    }
    
    @Test
    public void testGetTrackPreview(){
      Assert.assertNull(track.getPreview(null));
    }
    
    @Test
    public void testGetTrackAllowViewAngle(){
      Assert.assertTrue(track.allowViewAngle());
      
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

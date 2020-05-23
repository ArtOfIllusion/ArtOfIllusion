/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

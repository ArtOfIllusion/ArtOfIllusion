/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

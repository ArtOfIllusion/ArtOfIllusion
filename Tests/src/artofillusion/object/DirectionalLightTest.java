/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.Scene;
import artofillusion.math.RGBColor;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class DirectionalLightTest {
    
    @Test
    public void testCreateDirectinalLight()
    {
        RGBColor color = new RGBColor();
        DirectionalLight light = new DirectionalLight(color, 0);
        Assert.assertNotNull(light);
        Assert.assertEquals(light.getColor(), color);
        Assert.assertEquals(light.getIntensity(), 0, 0);
        Assert.assertEquals(light.getRadius(), 1.0, 0);
        Assert.assertEquals(Light.TYPE_NORMAL, light.getType());
        Assert.assertEquals(light.getDecayRate(), 0.5f, 0);
        
    }
    
    @Test
    public void testCreateDirectinalLight2()
    {
        RGBColor color = new RGBColor();
        DirectionalLight light = new DirectionalLight(color, 0, 5.0);
        Assert.assertNotNull(light);
        Assert.assertEquals(light.getColor(), color);
        Assert.assertEquals(light.getIntensity(), 0, 0);
        Assert.assertEquals(light.getRadius(), 5.0, 0);
        Assert.assertEquals(Light.TYPE_NORMAL, light.getType());
        Assert.assertEquals(light.getDecayRate(), 0.5f, 0);
        
    }
    
    @Test(expected = InvalidObjectException.class)
    public void testCreateDirectionalLightFromStreamBadVersion0() throws IOException
    {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object3D version
        wrap.putInt(-1); // No matertial
        wrap.putInt(0);  // Default texture
        
        wrap.putShort((short)-1);  // Version
        DirectionalLight light = new DirectionalLight(new DataInputStream(new ByteArrayInputStream(wrap.array())), scene);
        
    }

    @Test(expected = InvalidObjectException.class)
    public void testCreateDirectionalLightFromStreamBadVersion1() throws IOException
    {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object3D version
        wrap.putInt(-1); // No matertial
        wrap.putInt(0);  // Default texture
        
        wrap.putShort((short)2);  // Version
        {
            // RGB Color
            wrap.putFloat(0);
            wrap.putFloat(0.5f);
            wrap.putFloat(1f);
        }
        DirectionalLight light = new DirectionalLight(new DataInputStream(new ByteArrayInputStream(wrap.array())), scene);
        
    }
    
    
    @Test(expected = InvalidObjectException.class)
    public void testCreateDirectionalLightFromStreamGoodVersion0() throws IOException
    {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object3D version
        wrap.putInt(-1); // No matertial
        wrap.putInt(0);  // Default texture
        
        wrap.putShort((short)0);  // Version
        {
            // RGB Color
            wrap.putFloat(0);
            wrap.putFloat(0.5f);
            wrap.putFloat(1f);
        }
        wrap.putFloat(0.75f); //Intensity
        
        DirectionalLight light = new DirectionalLight(new DataInputStream(new ByteArrayInputStream(wrap.array())), scene);
        
    }
    
    
    @Test
    public void testDLCopy()
    {
        RGBColor color = new RGBColor(0, 0.5, 1);
        DirectionalLight source = new DirectionalLight(color, 3, 5.0);
        source.setType(Light.TYPE_AMBIENT);
        source.setDecayRate(0.8f);
        RGBColor targetColor = new RGBColor();
        DirectionalLight target = new DirectionalLight(targetColor, 0);
        
        target.copyObject(source);
       
        Assert.assertEquals(target.getColor(), color);
        Assert.assertEquals(target.getIntensity(), 3, 0);
        Assert.assertEquals(target.getRadius(), 5.0, 0);
        Assert.assertEquals(Light.TYPE_AMBIENT, target.getType());
        Assert.assertEquals(target.getDecayRate(), 0.8f, 0);
        
    }
    
    
    /*
    Testcase fails as some original light parameters is not copied to target
    */
    @Test
    public void testDLDuplicate()
    {
        RGBColor color = new RGBColor(0, 0.5, 1);
        DirectionalLight source = new DirectionalLight(color, 3, 5.0);
        source.setType(Light.TYPE_AMBIENT);
        source.setDecayRate(0.8f);
        
        DirectionalLight target = (DirectionalLight)source.duplicate();
       
        Assert.assertEquals(target.getColor(), color);
        Assert.assertEquals(target.getIntensity(), 3, 0);
        Assert.assertEquals(target.getRadius(), 5.0, 0);
        Assert.assertEquals(Light.TYPE_AMBIENT, target.getType());
        Assert.assertEquals(target.getDecayRate(), 0.8f, 0);
        
    }
}

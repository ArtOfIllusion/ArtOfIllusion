/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.math.RGBColor;
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
}

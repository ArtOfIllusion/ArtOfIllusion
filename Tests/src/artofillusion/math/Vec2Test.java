/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class Vec2Test  {
    
    @Test
    public  void testVec2Constructor0()
    {
        Vec2 test = new Vec2();
        Assert.assertEquals(0d, test.x, 0);
        Assert.assertEquals(0d, test.y, 0);
    }
    
    @Test
    public  void testVec2Constructor1()
    {
        Vec2 test = new Vec2(1.0, 1.0);
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(1.0, test.y, 0);
    }
    
    @Test
    public  void testVec2Constructor2()
    {
        Vec2 source = new Vec2(1.0, 1.0);
        Vec2 test = new Vec2(source);
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(1.0, test.y, 0);
    }
    
    @Test
    public void testVec2Constructor3() throws IOException
    {
        
        byte[] bytes = new byte[16];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putDouble(1.0);
        wrap.putDouble(2.0);
        
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        Vec2 test = new Vec2(new DataInputStream(targetStream));
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(2.0, test.y, 0);
    }
    
    
    @Test
    public void testCreateXVect2()
    {
        Vec2 test = Vec2.vx();
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(0.0, test.y, 0);
    }
    
    @Test
    public void testCreateYVect2()
    {
        Vec2 test = Vec2.vy();
        Assert.assertEquals(0.0, test.x, 0);
        Assert.assertEquals(1.0, test.y, 0);
    }
    
    @Test
    public void testVector2set()
    {
        Vec2 test = new Vec2();
        test.set(1.0, 2.0);
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(2.0, test.y, 0);
    }

    @Test
    public void testVector2times0()
    {
        Vec2 test = new Vec2();
        
        test = test.times(2.0);
        
        Assert.assertEquals(0.0, test.x, 0);
        Assert.assertEquals(0.0, test.y, 0);
    }
    
    @Test
    public void testVector2scale0()
    {
        Vec2 test = new Vec2();
        
        test.scale(2.0);
        
        Assert.assertEquals(0.0, test.x, 0);
        Assert.assertEquals(0.0, test.y, 0);
    }
    
    @Test
    public void testVector2times1()
    {
        Vec2 test = new Vec2();
        test.set(1.0, 2.0);
        test = test.times(0);
        
        Assert.assertEquals(0.0, test.x, 0);
        Assert.assertEquals(0.0, test.y, 0);
    }
    
    @Test
    public void testVector2scale1()
    {
        Vec2 test = new Vec2();
        test.set(1.0, 2.0);
        test.scale(0);
        
        Assert.assertEquals(0.0, test.x, 0);
        Assert.assertEquals(0.0, test.y, 0);
    }
    
    @Test
    public void testVector2times2()
    {
        Vec2 test = new Vec2();
        test.set(1.0, 2.0);
        test = test.times(2);
        
        Assert.assertEquals(2.0, test.x, 0);
        Assert.assertEquals(4.0, test.y, 0);
    }
    
    @Test
    public void testVector2scale2()
    {
        Vec2 test = new Vec2();
        test.set(1.0, 2.0);
        test.scale(2);
        
        Assert.assertEquals(2.0, test.x, 0);
        Assert.assertEquals(4.0, test.y, 0);
    }
    
}

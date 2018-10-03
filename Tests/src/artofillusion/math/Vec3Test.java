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
public class Vec3Test {
    @Test
    public  void testVec3Constructor0()
    {
        Vec3 test = new Vec3();
        Assert.assertEquals(0d, test.x, 0);
        Assert.assertEquals(0d, test.y, 0);
        Assert.assertEquals(0d, test.z, 0);
    }
    
    @Test
    public  void testVec3Constructor1()
    {
        Vec3 test = new Vec3(1.0, 2.0, 3.0);
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(2.0, test.y, 0);
        Assert.assertEquals(3.0, test.z, 0);
    }
    
    @Test
    public  void testVec3Constructor2()
    {
        Vec3 source = new Vec3(1.0, 2.0, 3.0);
        Vec3 test = new Vec3(source);
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(2.0, test.y, 0);
        Assert.assertEquals(3.0, test.z, 0);
    }
    
    @Test
    public void testVec3Constructor3() throws IOException
    {
        
        byte[] bytes = new byte[24];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putDouble(1.0);
        wrap.putDouble(2.0);
        wrap.putDouble(3.0);
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        Vec3 test = new Vec3(new DataInputStream(targetStream));
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(2.0, test.y, 0);
        Assert.assertEquals(2.0, test.y, 0);
    }

    @Test
    public void testCreateXVect2()
    {
        Vec3 test = Vec3.vx();
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(0.0, test.y, 0);
        Assert.assertEquals(0.0, test.z, 0);
    }
    
    @Test
    public void testCreateYVect2()
    {
        Vec3 test = Vec3.vy();
        Assert.assertEquals(0.0, test.x, 0);
        Assert.assertEquals(1.0, test.y, 0);
        Assert.assertEquals(0.0, test.z, 0);
    }
    
    @Test
    public void testCreateZVect2()
    {
        Vec3 test = Vec3.vz();
        Assert.assertEquals(0.0, test.x, 0);
        Assert.assertEquals(0.0, test.y, 0);
        Assert.assertEquals(1.0, test.z, 0);
    }
    
    @Test
    public void testVector3set()
    {
        Vec3 test = new Vec3();
        test.set(1.0, 2.0, 3.0);
        Assert.assertEquals(1.0, test.x, 0);
        Assert.assertEquals(2.0, test.y, 0);
        Assert.assertEquals(3.0, test.z, 0);
    }
}

/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package artofillusion.math;

import artofillusion.test.util.StreamUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author maksim.khramov
 */
public class CoordinateSystemTest {

    @Test
    public void testCreateCS() {
        Vec3 vec = new Vec3();
        vec.set(1, 2.0, 3.0);
        
        CoordinateSystem test = new CoordinateSystem(vec, 0, 0, 0);
        Assert.assertEquals(1.0, test.orig.x, 0);
        Assert.assertEquals(2.0, test.orig.y, 0);
        Assert.assertEquals(3.0, test.orig.z, 0);
        
    }
    
    
    @Test
    public void testCreateCSFromStream() throws IOException
    {        
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putDouble(1.0);
        wrap.putDouble(2.0);
        wrap.putDouble(3.0);
        
        wrap.putDouble(0.0);
        wrap.putDouble(45.0);
        wrap.putDouble(90.0);
        
        CoordinateSystem test = new CoordinateSystem(StreamUtil.stream(wrap));
        
        Assert.assertEquals(1.0, test.orig.x, 0);
        Assert.assertEquals(2.0, test.orig.y, 0);
        Assert.assertEquals(3.0, test.orig.z, 0);
        
        Assert.assertEquals(0, test.xrot, 0);
        Assert.assertEquals(45, test.yrot, 0);
        Assert.assertEquals(90, test.zrot, 0);
        
    }
}

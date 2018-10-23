/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.Scene;
import artofillusion.math.Vec3;
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
public class CurveTest {
    
    @Test
    public void testCreateCurve()
    {
        Curve curve = new Curve(new Vec3[0], new float[0], Mesh.APPROXIMATING, true);
        Assert.assertNotNull(curve);
        Assert.assertNotNull(curve.getVertices());
        Assert.assertEquals(0, curve.getVertices().length);
        Assert.assertEquals(0, curve.getSmoothness().length);
        Assert.assertEquals(Mesh.APPROXIMATING, curve.getSmoothingMethod());
        
    }
    
    @Test
    public void testCreateCurve2()
    {
        Curve curve = new Curve(new Vec3[] {new Vec3(), new Vec3()}, new float[] {0f, 1f}, Mesh.APPROXIMATING, false);
        Assert.assertNotNull(curve);
        Assert.assertNotNull(curve.getVertices());
        Assert.assertEquals(2, curve.getVertices().length);
        Assert.assertEquals(2, curve.getSmoothness().length);
        Assert.assertEquals(Mesh.APPROXIMATING, curve.getSmoothingMethod());
        Assert.assertFalse(curve.isClosed());
        
    }
    
    @Test
    public void testCurveDuplicate()
    {
        Curve source = new Curve(new Vec3[] {new Vec3(), new Vec3()}, new float[] {0f, 1f}, Mesh.APPROXIMATING, false);        
        Curve curve = (Curve)source.duplicate();
        
        Assert.assertNotEquals(source, curve);
        Assert.assertNotNull(curve);
        Assert.assertNotNull(curve.getVertices());
        Assert.assertEquals(2, curve.getVertices().length);
        Assert.assertEquals(2, curve.getSmoothness().length);
        Assert.assertEquals(Mesh.APPROXIMATING, curve.getSmoothingMethod());
        Assert.assertFalse(curve.isClosed());
        
    }
    
    @Test
    public void testCurveCopy()
    {
        Curve source = new Curve(new Vec3[] {new Vec3(), new Vec3()}, new float[] {0f, 1f}, Mesh.APPROXIMATING, false);        
        Curve curve = new Curve(new Vec3[0], new float[0], Mesh.APPROXIMATING, true);
        curve.copyObject(source);
        
        Assert.assertNotEquals(source, curve);
        Assert.assertNotNull(curve);
        Assert.assertNotNull(curve.getVertices());
        Assert.assertEquals(2, curve.getVertices().length);
        Assert.assertEquals(2, curve.getSmoothness().length);
        Assert.assertEquals(Mesh.APPROXIMATING, curve.getSmoothingMethod());
        Assert.assertFalse(curve.isClosed());
        
    }
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testLoadCurveBadObjectVersion1() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)-1);
        
        new Curve(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
    }
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testLoadCurveBadObjectVersion2() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)2);
        
        new Curve(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
    }
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testLoadCurveBadObjectVersion3() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1);
        wrap.putShort((short)1); // Read version again !!!!
        new Curve(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
    }
    
    @Test
    public void testLoadCurve() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)0);
        wrap.putShort((short)0); // Read version again !!!!
        wrap.putInt(0); // Vertex count
        
        wrap.put((byte)0);  // Closed curve - false
        wrap.putInt(Mesh.INTERPOLATING);
        
        Curve curve = new Curve(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
        Assert.assertNotNull(curve);
        Assert.assertEquals(0, curve.getVertices().length);
        Assert.assertEquals(false, curve.isClosed());
        Assert.assertEquals(Mesh.INTERPOLATING, curve.getSmoothingMethod());
    }
}

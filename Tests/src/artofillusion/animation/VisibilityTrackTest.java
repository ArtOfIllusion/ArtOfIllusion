/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package artofillusion.animation;

import artofillusion.Scene;
import artofillusion.math.CoordinateSystem;
import artofillusion.object.Cube;
import artofillusion.object.Object3D;
import artofillusion.object.ObjectInfo;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author maksim.khramov
 */
public class VisibilityTrackTest {
    
    @Test
    public void testCreateVisibilityTrack() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        VisibilityTrack vt = new VisibilityTrack(oi);
        Assert.assertNotNull(vt);
        Assert.assertEquals(oi, vt.getParent());
    }
    
    @Test(expected = InvalidObjectException.class)
    public void testLoadTrackBadVersion() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Track Version
        
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        VisibilityTrack vt = new VisibilityTrack(oi);
        vt.initFromStream(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
    }
    
}
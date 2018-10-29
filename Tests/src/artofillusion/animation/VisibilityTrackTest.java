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
import artofillusion.object.NullObject;
import artofillusion.object.Object3D;
import artofillusion.object.ObjectInfo;
import artofillusion.test.util.StreamUtil;
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
    public void testCreateVisibilityTrack()
    {
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
        vt.initFromStream(StreamUtil.stream(wrap), (Scene)null);
    }
    
    @Test
    public void testLoadVisibilityTrackFromStreamNoKeys() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)0); // Track Version
        {
            String trackName = "Visibility Track";
            wrap.putShort(Integer.valueOf(trackName.length()).shortValue());
            wrap.put(trackName.getBytes());
        }
        wrap.put((byte)0);  // Enabled - false
        wrap.putInt(0); // No keys
        
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        VisibilityTrack vt = new VisibilityTrack(oi);
        vt.initFromStream(StreamUtil.stream(wrap), (Scene)null);
        
        Assert.assertEquals("Visibility Track", vt.getName());
        Assert.assertEquals(oi, vt.getParent());
        Assert.assertFalse(vt.isEnabled());
        
        Assert.assertEquals(0, vt.getTimecourse().getValues().length);
    }
    
    @Test
    public void testLoadVisibilityTrackFromStreamWithKeys() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)0); // Track Version
        {
            String trackName = "Visibility Track";
            wrap.putShort(Integer.valueOf(trackName.length()).shortValue());
            wrap.put(trackName.getBytes());
        }
        wrap.put((byte)0);  // Enabled - false
        wrap.putInt(2); // 2 keys
        
        { // Key 1
            wrap.putDouble(0);
            wrap.put((byte)0);             
        }
        { // Key 2
            wrap.putDouble(1);
            wrap.put((byte)1);             
        }
        
        
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        VisibilityTrack vt = new VisibilityTrack(oi);
        vt.initFromStream(StreamUtil.stream(wrap), (Scene)null);
        
        Assert.assertEquals("Visibility Track", vt.getName());
        Assert.assertEquals(oi, vt.getParent());
        Assert.assertFalse(vt.isEnabled());
        
        Timecourse tc = vt.getTimecourse();
        Assert.assertEquals(2, tc.getValues().length);
        Assert.assertEquals(2, tc.getTimes().length);
        Assert.assertEquals(2, tc.getSmoothness().length);
        
        
    }
    
    @Test
    public void testDuplicateVisibilityTrack()
    {
        ObjectInfo oi = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Test null");
        VisibilityTrack vt = new VisibilityTrack(oi);
        Track dup = vt.duplicate(oi);
        
        Assert.assertNotNull(dup);
        Assert.assertTrue(dup instanceof VisibilityTrack);
        
        Assert.assertNotEquals(vt, dup);
        Assert.assertEquals("Visibility", dup.getName());
        Assert.assertEquals(oi, dup.getParent());
        Assert.assertTrue(dup.isEnabled());
        
        Assert.assertEquals(vt.getTimecourse().getValues().length, dup.getTimecourse().getValues().length);
    }
    
    @Test
    public void testCopyVisibilityTrack()
    {
        ObjectInfo oi = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Test null");
        VisibilityTrack source = new VisibilityTrack(oi);
        source.setName("Source");
        source.setEnabled(false);
        source.setQuantized(false);
        source.getTimecourse().addTimepoint(new BooleanKeyframe(true), 5, new Smoothness());
        source.getTimecourse().addTimepoint(new BooleanKeyframe(false), 10, new Smoothness());
        
        VisibilityTrack target = new VisibilityTrack(oi);
        target.copy(source);
        
        Assert.assertEquals("Source", target.getName());
        Assert.assertFalse(target.isEnabled());
        Assert.assertFalse(target.isQuantized());
    
        Assert.assertEquals(source.getTimecourse().getValues().length, target.getTimecourse().getValues().length);
    }
    
}

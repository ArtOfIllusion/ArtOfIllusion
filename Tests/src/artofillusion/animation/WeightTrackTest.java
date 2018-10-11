/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package artofillusion.animation;

import artofillusion.LayoutWindow;
import artofillusion.Scene;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class WeightTrackTest {

    private static DummyTrack parent;
    
    @BeforeClass
    public static void setUpClass()
    {
        parent = new DummyTrack();
    }
  
    @Test
    public void testCreateWeightTrack()
    {
        
        WeightTrack weight = new WeightTrack(parent);
        
        Assert.assertNotNull(weight);
        Assert.assertEquals("Weight", weight.getName());
        Assert.assertEquals(parent, weight.parent);
        Assert.assertTrue(weight.isEnabled());
    }
    
    @Test
    public void testGetWeightForDisabledTrack()
    {
        WeightTrack weight = new WeightTrack(parent);
        weight.setEnabled(false);
        Assert.assertEquals(1.0, weight.getWeight(0), 0);
    }
    
    @Test(expected = InvalidObjectException.class)
    public void testLoadFromStreamTrackBadVersion() throws IOException
    {
        byte[] bytes = new byte[12];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)1); // Track Version
            
        Track track = new WeightTrack(parent);
        track.initFromStream(new DataInputStream(new ByteArrayInputStream(bytes)), (Scene)null);
    }
    
    
    @Test
    public void testLoadFromStreamTrack() throws IOException
    {
        byte[] bytes = new byte[120];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)0); // Track Version

        String trackName = "Weight";
        wrap.putShort(Integer.valueOf(trackName.length()).shortValue());
        wrap.put(trackName.getBytes());
        
        wrap.put((byte)1);  // Is Enabled
        wrap.putInt(Timecourse.LINEAR);
        
        wrap.putInt(1); // KeysCount
        {
            wrap.putDouble(0); // Time;
            wrap.putDouble(1); // Scalar Keyframe data;
            // Smoothness data
            {
                wrap.putDouble(0);
                wrap.putDouble(1);            
            }

        }
        
        Track track = new WeightTrack(parent);
        track.initFromStream(new DataInputStream(new ByteArrayInputStream(bytes)), (Scene)null);
        
        Assert.assertTrue(track.isEnabled());
        Assert.assertEquals(Timecourse.LINEAR, track.getSmoothingMethod());
        Assert.assertEquals(1, track.getKeyTimes().length);
    }
    
    
    public static class DummyTrack extends Track
    {

        @Override
        public void edit(LayoutWindow win) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void apply(double time) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Track duplicate(Object parent) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void copy(Track tr) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public double[] getKeyTimes() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int moveKeyframe(int which, double time) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void deleteKeyframe(int which) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isNullTrack() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void writeToStream(DataOutputStream out, Scene scene) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void initFromStream(DataInputStream in, Scene scene) throws IOException, InvalidObjectException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

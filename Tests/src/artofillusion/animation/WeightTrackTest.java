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
import artofillusion.math.CoordinateSystem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import org.junit.Assert;
import org.junit.Before;
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

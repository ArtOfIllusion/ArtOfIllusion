/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author MaksK
 */
public class TimecourseTest {
    
    @Test
    public void testCreateTimecourse() {
        Timecourse tc = new Timecourse(new Keyframe[0], new double[0], new Smoothness[0]);
        Assert.assertNotNull(tc);
        Assert.assertEquals(0, tc.getTimes().length);
        Assert.assertEquals(0, tc.getValues().length);
        Assert.assertEquals(0, tc.getSmoothness().length);
    }
    
    @Test
    public void testSubdivideTimecourseNotEnoughLength()
    {
        Timecourse tc = new Timecourse(new Keyframe[0], new double[0], new Smoothness[0]);
        Timecourse subdiv = tc.subdivide(Timecourse.DISCONTINUOUS);
        
        Assert.assertNotNull(tc);    
        Assert.assertEquals(tc, subdiv);
    }
    
    @Test
    public void  testTimecourseAddAndReplaceCurrentValue()
    {
        Timecourse tc = new Timecourse(new BooleanKeyframe[] { new BooleanKeyframe(true) } , new double[] {5}, new Smoothness[] {new Smoothness()});        
        tc.addTimepoint(new BooleanKeyframe(false), 5, new Smoothness());
        
        Assert.assertEquals(1, tc.getValues().length);
        Assert.assertEquals(1, tc.getTimes().length);
        Assert.assertEquals(1, tc.getSmoothness().length);
        Assert.assertEquals(5, tc.getTimes()[0], 0);

    }
    
    @Test
    public void  testTimecourseAddAsNewtValue()
    {
        Timecourse tc = new Timecourse(new BooleanKeyframe[] { new BooleanKeyframe(true) } , new double[] {5}, new Smoothness[] {new Smoothness()});        
        tc.addTimepoint(new BooleanKeyframe(false), 10, new Smoothness());
        
        Assert.assertEquals(2, tc.getValues().length);
        Assert.assertEquals(2, tc.getTimes().length);
        Assert.assertEquals(2, tc.getSmoothness().length);
        Assert.assertEquals(5, tc.getTimes()[0], 0);
        Assert.assertEquals(10, tc.getTimes()[1], 0);

    }
    
    @Test
    public void testClearAllFromTimecourse()
    {
        Timecourse tc = new Timecourse(new BooleanKeyframe[] { new BooleanKeyframe(true) } , new double[] {5}, new Smoothness[] {new Smoothness()});
        tc.addTimepoint(new BooleanKeyframe(false), 10, new Smoothness());
        tc.removeAllTimepoints();
        
        Assert.assertEquals(0, tc.getValues().length);
        Assert.assertEquals(0, tc.getTimes().length);
        Assert.assertEquals(0, tc.getSmoothness().length);        
    }
    
    @Test
    public void testDuplicateEmptyTimecourse()
    {
        Timecourse tc = new Timecourse(new Keyframe[0], new double[0], new Smoothness[0]);
        tc.setSubdivideAdaptively(false);
        
        Timecourse dup = tc.duplicate(null);
        
        Assert.assertNotNull(dup);
        Assert.assertNotEquals(dup, tc);
        Assert.assertFalse(dup.getSubdivideAdaptively());
        
        Assert.assertEquals(0, dup.getTimes().length);
        Assert.assertEquals(0, dup.getValues().length);
        Assert.assertEquals(0, dup.getSmoothness().length);
    }
    
    @Test
    public void testDuplicateTimecourse()
    {
        Timecourse tc = new Timecourse(new Keyframe[0], new double[0], new Smoothness[0]);
        tc.addTimepoint(new BooleanKeyframe(true), 5, new Smoothness());
        tc.addTimepoint(new BooleanKeyframe(false), 10, new Smoothness());
        
        tc.setSubdivideAdaptively(false);
        
        Timecourse dup = tc.duplicate(null);
        
        Assert.assertNotNull(dup);
        Assert.assertNotEquals(dup, tc);
        Assert.assertFalse(dup.getSubdivideAdaptively());
        
        Assert.assertEquals(2, dup.getTimes().length);
        Assert.assertEquals(2, dup.getValues().length);
        Assert.assertEquals(2, dup.getSmoothness().length);
    }
}

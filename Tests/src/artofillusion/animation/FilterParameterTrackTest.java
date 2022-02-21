/* Copyright (C) 1999-2009 by Peter Eastman
   Modifications Copyright 2016 by Petri Ihalainen
   Changes copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.Scene;
import artofillusion.image.ComplexImage;
import artofillusion.image.filter.ImageFilter;
import artofillusion.math.CoordinateSystem;
import artofillusion.object.SceneCamera;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;



/**
 *
 * @author MaksK
 */
public class FilterParameterTrackTest
{
    @Test
    public void testDuplucateFilterParameterTrack()
    {
      SceneCamera camera1 = new SceneCamera();
      SceneCamera camera2 = new SceneCamera();
      FilterParameterTrack source = new FilterParameterTrack(camera1, new DummyFilter());
      source.setEnabled(false);
      source.setQuantized(false);
      
      source.setSmoothingMethod(Timecourse.APPROXIMATING);
      
      FilterParameterTrack target = (FilterParameterTrack)source.duplicate(camera2);
      
      Assert.assertEquals(source.isEnabled(), target.isEnabled());
      Assert.assertEquals(source.isQuantized(), target.isQuantized());
      
    }
    
    
    private class DummyFilter extends ImageFilter {

    @Override
    public String getName()
    {
      return "Dummy Filter";
    }

    @Override
    public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
    {
      
    }

    @Override
    public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
    {
      
    }

    @Override
    public void initFromStream(DataInputStream in, Scene theScene) throws IOException
    {
      
    }
      
    }
}

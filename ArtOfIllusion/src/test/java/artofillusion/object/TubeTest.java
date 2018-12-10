/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.math.Vec3;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class TubeTest {
    
    @Test
    public void testCreateTubeFromCurve()
    {
        Tube tube = new Tube(new Curve(new Vec3[] {new Vec3(), new Vec3()}, new float[] {0f, 1f}, Mesh.APPROXIMATING, false), new double[] {0f, 1f}, Tube.CLOSED_ENDS);
        Assert.assertNotNull(tube);
        
    }

    
}

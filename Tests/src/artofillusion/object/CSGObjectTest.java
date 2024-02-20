/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.math.CoordinateSystem;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class CSGObjectTest {

    @Test
    public void testCreateCSGObject()
    {

        ObjectInfo cube = new ObjectInfo(new Cube(1,2,3), new CoordinateSystem(), "cube");
        ObjectInfo sphere = new ObjectInfo(new Sphere(1,2,3), new CoordinateSystem(), "sphere");
        CSGObject cso = new CSGObject(cube, sphere, CSGObject.UNION);
        
        Assert.assertNotNull(cso);
    }
    
    @Test(expected = ClassCastException.class)
    public void testCopyObjectFromBadSource()
    {
        ObjectInfo cube = new ObjectInfo(new Cube(1,2,3), new CoordinateSystem(), "cube");
        ObjectInfo sphere = new ObjectInfo(new Sphere(1,2,3), new CoordinateSystem(), "sphere");
        CSGObject cso = new CSGObject(cube, sphere, CSGObject.UNION);
        
        cso.copyObject(new Cube(1,2,3));
    }
}

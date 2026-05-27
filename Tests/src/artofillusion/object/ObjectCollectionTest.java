/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.Scene;
import artofillusion.animation.Keyframe;
import artofillusion.math.CoordinateSystem;
import artofillusion.math.Vec3;
import java.util.Enumeration;
import java.util.Vector;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class ObjectCollectionTest {

    @Test
    public void testEmptyCollectionObjectIsClosed() {
        ObjectCollection oc = new CustomObjectCollection();
        Assert.assertNotNull(oc);
        Assert.assertTrue(oc.isClosed());
    }
    
    @Test
    public void testClosedSingleItemCollectionObjectIsClosed()
    {
        ObjectCollection oc = new CustomObjectCollection();
        oc.cachedObjects.add(new ObjectInfo(new Cube(1,1,1), new CoordinateSystem(), "Cube"));
        Assert.assertNotNull(oc);
        Assert.assertTrue(oc.isClosed());
    }
    
    @Test
    public void testOpenedSingleItemCollectionObjectIsNotClosed()
    {
        ObjectCollection oc = new CustomObjectCollection();
        Curve curve = new Curve(new Vec3[0], new float[0], Mesh.APPROXIMATING, false);        
        oc.cachedObjects.add(new ObjectInfo(curve, new CoordinateSystem(), "Curve"));
        Assert.assertNotNull(oc);
        Assert.assertFalse(oc.isClosed());
        
    }

    @Test
    public void testObjectCollectionIsClosed()
    {
        ObjectCollection oc = new CustomObjectCollection();
        
        oc.cachedObjects.add(new ObjectInfo(new Cube(1,1,1), new CoordinateSystem(),"Cube1"));
        oc.cachedObjects.add(new ObjectInfo(new Cube(1,1,1), new CoordinateSystem(),"Cube2"));
        
        Assert.assertNotNull(oc);
        Assert.assertTrue(oc.isClosed());
    }
    
    @Test
    public void testObjectCollectionIsNotClosed()
    {
        ObjectCollection oc = new CustomObjectCollection();
        Curve curve = new Curve(new Vec3[0], new float[0], Mesh.APPROXIMATING, false);
        oc.cachedObjects.add(new ObjectInfo(new Cube(1,1,1), new CoordinateSystem(),"Cube"));
        oc.cachedObjects.add(new ObjectInfo(curve, new CoordinateSystem(), "Curve"));
        Assert.assertNotNull(oc);
        Assert.assertFalse(oc.isClosed());        
    }
    
    
    public class CustomObjectCollection extends ObjectCollection
    {
        public CustomObjectCollection()
        {
            super();
            cachedObjects = new Vector<>();
        }
        
        @Override
        protected Enumeration<ObjectInfo> enumerateObjects(ObjectInfo info, boolean interactive, Scene scene) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object3D duplicate() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void copyObject(Object3D obj) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setSize(double xsize, double ysize, double zsize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe getPoseKeyframe() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void applyPoseKeyframe(Keyframe k) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

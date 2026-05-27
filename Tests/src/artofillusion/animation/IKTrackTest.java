/* Copyright (C) 2024 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.math.CoordinateSystem;
import artofillusion.object.Cube;
import artofillusion.object.NullObject;
import artofillusion.object.Object3D;
import artofillusion.object.ObjectInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class IKTrackTest {


    @Test
    public void createTrack() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);
        Assert.assertNotNull(it);
        Assert.assertEquals(oi, it.getParent());
        Assert.assertEquals("Inverse Kinematics", it.getName());
        Assert.assertTrue(it.isEnabled());
        Assert.assertTrue(it.isQuantized());
        Assert.assertTrue(it.getUseGestures());

        Assert.assertNotNull(it.getConstraints());
        Assert.assertEquals(0, it.getConstraints().size());
    }
    
    @Test
    public void testDuplicateTrackNoConstraints() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);
        it.setName("Test Name");
        it.setEnabled(false);
        it.setQuantized(false);
        it.setUseGestures(false);
        it.addConstraint(0, new ObjectRef());
        it.addConstraint(1, new ObjectRef());
        it.addConstraint(20, null);

        IKTrack duplicate = (IKTrack)it.duplicate(oi);
        Assert.assertNotNull(duplicate);
        Assert.assertEquals(oi, duplicate.getParent());
        Assert.assertFalse(duplicate.isEnabled());
        Assert.assertFalse(duplicate.isQuantized());

        Assert.assertEquals("Test Name", duplicate.getName());

        List<IKTrack.Constraint> constraints = duplicate.getConstraints();
        Assert.assertNotNull(constraints);
        Assert.assertEquals(it.getConstraints().size(), constraints.size());
        Assert.assertTrue(duplicate.getUseGestures());
    }

    @Test
    public void testCopyTrack() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi1 = new ObjectInfo(obj, new CoordinateSystem(), "Cube1");
        ObjectInfo oi2 = new ObjectInfo(obj, new CoordinateSystem(), "Cube2");

        IKTrack it = new IKTrack(oi1);
        it.setName("Inverse Kinematics");
        it.setEnabled(false);
        it.setQuantized(false);
        it.setUseGestures(false);

        IKTrack source = new IKTrack(oi2);

        source.addConstraint(0, new ObjectRef());
        source.addConstraint(1, new ObjectRef());
        source.addConstraint(20, null);
        
        it.copy(source);
        Assert.assertEquals(oi1, it.getParent());
        Assert.assertTrue(it.isEnabled());
        Assert.assertTrue(it.isQuantized());

        Assert.assertEquals(it.getConstraints().size(), source.getConstraints().size());        
        
        Assert.assertFalse(it.getUseGestures());
    }
    
    @Test
    public void testEmptyTrackIsNull() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);
        Assert.assertTrue(it.isNullTrack());
    }
    
    @Test
    public void testNotEmptyTrackIsNull() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);

        it.addConstraint(0, null);

        Assert.assertTrue(it.isNullTrack());
    }

    @Test
    public void testNotEmptyTrackIsNull2() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);

        it.addConstraint(0, null);
        it.addConstraint(1, null);

        Assert.assertTrue(it.isNullTrack());
    }

    @Test
    public void testNotEmptyTrackIsNotNull() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);

        ObjectRef ref = new ObjectRef();
        it.addConstraint(0, ref);
        it.addConstraint(1, null);
        Assert.assertFalse(it.isNullTrack());
    }

    @Test
    public void testNotEmptyTrackIsNotNull2() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);

        ObjectRef ref = new ObjectRef();
        it.addConstraint(0, ref);
        it.addConstraint(1, ref);
        Assert.assertFalse(it.isNullTrack());
    }

    @Test
    public void testGetTrackWeights() {
        Object3D obj = new Cube(1, 1, 1);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);
        ObjectRef ref = new ObjectRef();
        it.addConstraint(0, ref);
        it.addConstraint(1, ref);

        Track[] subTracks = it.getSubtracks();
        Assert.assertEquals(1, subTracks.length);
        Assert.assertTrue(subTracks[0] instanceof WeightTrack);
    }

    @Test
    public void testTrackCanBeAddedAsChild() {
        Object3D obj = new Cube(1, 1, 1);
        obj = obj.convertToTriangleMesh(0.1f);
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Cube");
        IKTrack it = new IKTrack(oi);

        Assert.assertTrue(it.canAcceptAsParent(oi));
    }

    @Test
    public void testTrackCannotBeAddedAsChild0() {
        Object3D obj = new NullObject();
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Null");
        IKTrack it = new IKTrack(oi);

        Assert.assertFalse(it.canAcceptAsParent("oi"));
    }

    @Test
    public void testTrackCannotBeAddedAsChild1() {
        Object3D obj = new NullObject();
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Null");
        IKTrack it = new IKTrack(oi);

        Assert.assertFalse(it.canAcceptAsParent(oi));
    }

    @Test
    public void testGetEmptyTrackDependencies1() {
        Object3D obj = new NullObject();
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Null");
        IKTrack it = new IKTrack(oi);

        ObjectInfo[] dependencies = it.getDependencies();
        Assert.assertEquals(0, dependencies.length);
    }

    @Test
    public void testGetEmptyTrackDependencies2() {
        Object3D obj = new NullObject();
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Null");
        IKTrack it = new IKTrack(oi);
        it.addConstraint(0, null);
        it.addConstraint(1, null);
        it.addConstraint(2, null);

        ObjectInfo[] dependencies = it.getDependencies();
        Assert.assertEquals(0, dependencies.length);
    }

    @Test
    public void testGetNonEmptyDependencies() {
        Object3D obj = new NullObject();
        ObjectInfo oi = new ObjectInfo(obj, new CoordinateSystem(), "Null");
        IKTrack it = new IKTrack(oi);
        it.addConstraint(0, null);
        it.addConstraint(1, null);

        Object3D null2 = new NullObject();
        ObjectInfo null2oi = new ObjectInfo(null2, new CoordinateSystem(), "Null");

        it.addConstraint(2, new ObjectRef(null2oi));

        ObjectInfo[] dependencies = it.getDependencies();
        Assert.assertEquals(1, dependencies.length);
        Assert.assertEquals(null2oi, dependencies[0]);
    }
}

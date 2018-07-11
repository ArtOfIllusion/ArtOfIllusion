/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.math.Vec3;
import artofillusion.object.Curve;
import artofillusion.object.Mesh;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class ActorTest {


    @Test
    public void testCreateActor() {
        Vec3[] cp = new Vec3[2];
        cp[0] = new Vec3();
        cp[1] = Vec3.vz();
        float[] smoothness = new float[2];
        smoothness[0] = 0;
        smoothness[1] = 0;
        
        Curve curve = new Curve(cp, smoothness, Mesh.NO_SMOOTHING, true);
        Actor actor = new Actor(curve);
        
        Assert.assertEquals(1, actor.getNumGestures());
    }
    
    @Test
    public void testActorAddGesture() {
        Vec3[] cp = new Vec3[2];
        cp[0] = new Vec3();
        cp[1] = Vec3.vz();
        float[] smoothness = new float[2];
        smoothness[0] = 0;
        smoothness[1] = 0;
        
        Curve curve = new Curve(cp, smoothness, Mesh.NO_SMOOTHING, true);
        Actor actor = new Actor(curve);
        
        actor.addGesture((Gesture)curve.getPoseKeyframe(), "Gesture Two");
        
        Assert.assertEquals(2, actor.getNumGestures());        
        Assert.assertEquals(2, actor.gesture.length);
        Assert.assertEquals(2, actor.gestureName.length);
        Assert.assertEquals(2, actor.gestureID.length);
    }
    
    
    @Test
    public void testActorKeyframe0() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {  
        
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        Assert.assertEquals(0, acf.getNumGestures());
    }

    @Test
    public void testActorKeyframe1() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {  
        int[] ids = new int[1];
        ids[0] = 0;
 
        double weights[] = new double[1];
        weights[0] = 1.0d;
        
        
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe(ids, weights);
        Assert.assertEquals(1, acf.getNumGestures());
    }
    
    @Test
    public void testActorKeyframe2() {  
        int[] ids = new int[2];
        ids[0] = 0;
        ids[1] = 1;
        double weights[] = new double[2];
        weights[0] = 1.0d;
        weights[1] = 2.0d;
        
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe(ids, weights);
        Assert.assertEquals(2, acf.getNumGestures());
    }
    
    @Test
    public void testAddGesture() {
        int[] ids = new int[2];
        ids[0] = 0;
        ids[1] = 1;
        double weights[] = new double[2];
        weights[0] = 1.0d;
        weights[1] = 2.0d;
        
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe(ids, weights);
        acf.addGesture(2, 3.0d);
        Assert.assertEquals(3, acf.getNumGestures());
    }
    
    @Test
    public void testDeleteLastGesture() {
        int[] ids = new int[2];
        ids[0] = 0;
        ids[1] = 1;
        double weights[] = new double[2];
        weights[0] = 1.0d;
        weights[1] = 2.0d;
        
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe(ids, weights);
        acf.addGesture(2, 3.0d);
        
        acf.deleteGesture(acf.getNumGestures()-1);
        Assert.assertEquals(2, acf.getNumGestures());
       
    }
    
    @Test
    public void testDeleteSingleGesture() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);
        acf.deleteGesture(0);
        
        Assert.assertEquals(0, acf.getNumGestures());
        Assert.assertEquals(0, acf.id.length);
        Assert.assertEquals(0, acf.weight.length);
    }
    
    
    @Test(expected = NegativeArraySizeException.class)
    public void testDeleteMissingGesture() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.deleteGesture(0);
    }
    
    @Test(expected = NegativeArraySizeException.class)
    public void testDeleteMissingGestureBig() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.deleteGesture(10);
    }

    @Test
    public void testGetGestureData() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);
        Assert.assertEquals(2, acf.getGestureID(0));
        Assert.assertEquals(3.0d, acf.getGestureWeight(0), 0.01);
    }
    
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetGestureMissedData() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);
        Assert.assertEquals(2, acf.getGestureID(10));
        Assert.assertEquals(3.0d, acf.getGestureWeight(10), 0.01);
    }
    
    
    @Test
    public void testDuplicateActorKeyFrame() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);

        Actor.ActorKeyframe dup = (Actor.ActorKeyframe)acf.duplicate();
        Assert.assertNotSame(dup, acf);
        Assert.assertEquals(1, dup.getNumGestures());
        Assert.assertEquals(1, dup.id.length);
        Assert.assertEquals(1, dup.weight.length);
        
    }
    
    @Test
    public void testDuplicateActorKeyFrameЩерук() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);

        Actor.ActorKeyframe dup = (Actor.ActorKeyframe)acf.duplicate(acf);
        Assert.assertNotSame(dup, acf);
        Assert.assertEquals(1, dup.getNumGestures());
        Assert.assertEquals(1, dup.id.length);
        Assert.assertEquals(1, dup.weight.length);
        
    }
    
    
    @Test
    public void testCopyActorKeyframe() {
        int[] ids = new int[2];
        ids[0] = 0;
        ids[1] = 1;
        double weights[] = new double[2];
        weights[0] = 1.0d;
        weights[1] = 2.0d;
        
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe(ids, weights);
        acf.addGesture(2, 3.0d);

        Actor.ActorKeyframe copy = new Actor.ActorKeyframe();
        copy.copy(acf);
        
        Assert.assertNotSame(copy, acf);
        Assert.assertEquals(acf.getNumGestures(), copy.getNumGestures());
        Assert.assertEquals(acf.id.length, copy.id.length);
        Assert.assertEquals(acf.weight.length, copy.weight.length);
    }
    
    @Test
    public void testActorZeroGesturesAddWeightsToTableZeroGestures() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        Map table = new HashMap();
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("addWeightsToTable", Actor.ActorKeyframe.class, Map.class, double.class);
        method.setAccessible(true);
        method.invoke(null, acf, table, 1.0d);

    }
    
    @Test
    public void testActorSingleGestureAddWeightsToTableZeroGestures() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);
        Map map = new HashMap();
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("addWeightsToTable", Actor.ActorKeyframe.class, Map.class, double.class);
        method.setAccessible(true);
        method.invoke(null, acf, map, 1.0d);

        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(2));
        Assert.assertEquals(3.0d, (Double)map.get(2), 0d);
    }
    
    @Test
    public void testActorSingleGestureAddWeightsToTableSingleGesture() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);
        Map map = new HashMap();
        map.put(2, 3.5d);
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("addWeightsToTable", Actor.ActorKeyframe.class, Map.class, double.class);
        method.setAccessible(true);
        method.invoke(null, acf, map, 1.5d);

        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(2));
        Assert.assertEquals(8.0d, (Double)map.get(2), 0d);
    }
    
    @Test
    public void testActorSingleGestureAddWeightsToTableSingleGestureNotMatch() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        acf.addGesture(2, 3.0d);
        Map map = new HashMap();
        map.put(3, 5d);
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("addWeightsToTable", Actor.ActorKeyframe.class, Map.class, double.class);
        method.setAccessible(true);
        method.invoke(null, acf, map, 1.5d);

        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey(2));
        Assert.assertTrue(map.containsKey(3));
        Assert.assertEquals(4.5d, (Double)map.get(2), 0d);
        Assert.assertEquals(5.0d, (Double)map.get(3), 0d);
    }
    
    @Test
    public void testActorKeyFrameGetGraphValues() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        double[] result = acf.getGraphValues();
        Assert.assertNotNull(result);
    }
    
    @Test
    public void testActorKeyFrameSetGraphValues() {
        Actor.ActorKeyframe acf = new Actor.ActorKeyframe();
        double weights[] = new double[2];
        weights[0] = 1.0d;
        weights[1] = 2.0d;
        
        acf.setGraphValues(weights);
        
    }
    
    @Test
    public void testCreateActorFromEmptyTable() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {

        Map<Integer,Double> map = new HashMap<>();
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("getKeyframeFromTable", Map.class);
        method.setAccessible(true);
        Actor.ActorKeyframe target = (Actor.ActorKeyframe)method.invoke(null, map);
        
        Assert.assertNotNull(target);
        Assert.assertEquals(0, target.id.length);
        Assert.assertEquals(0, target.weight.length);
        
    }

    @Test
    public void testCreateActorFromAllZeroTable() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {

        Map<Integer,Double> map = new HashMap<>();
        map.put(1, 0d);
        map.put(2, 0d);
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("getKeyframeFromTable", Map.class);
        method.setAccessible(true);
        Actor.ActorKeyframe target = (Actor.ActorKeyframe)method.invoke(null, map);
        
        Assert.assertNotNull(target);
        Assert.assertEquals(0, target.id.length);
        Assert.assertEquals(0, target.weight.length);
        
    }
    
    @Test
    public void testCreateActorFromTableOfTwo() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Map<Integer,Double> map = new HashMap<>();
        map.put(1, 50d);
        map.put(2, 10d);
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("getKeyframeFromTable", Map.class);
        method.setAccessible(true);
        Actor.ActorKeyframe target = (Actor.ActorKeyframe)method.invoke(null, map);
        
        Assert.assertNotNull(target);
        Assert.assertEquals(2, target.id.length);
        Assert.assertEquals(2, target.weight.length);        
    }

    @Test
    public void testCreateActorFromTableOfThree() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Map<Integer,Double> map = new HashMap<>();
        map.put(1, 50d);
        map.put(2, 10d);
        map.put(3, 5d);
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("getKeyframeFromTable", Map.class);
        method.setAccessible(true);
        Actor.ActorKeyframe target = (Actor.ActorKeyframe)method.invoke(null, map);
        
        Assert.assertNotNull(target);
        Assert.assertEquals(3, target.id.length);
        Assert.assertEquals(3, target.weight.length);        
    }
    
    @Test
    public void testCreateActorFromSparseTableOfFour() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Map<Integer,Double> map = new HashMap<>();
        map.put(1, 10d);
        map.put(2, 0d);
        map.put(3, 5d);
        map.put(4, 0d);
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("getKeyframeFromTable", Map.class);
        method.setAccessible(true);
        Actor.ActorKeyframe target = (Actor.ActorKeyframe)method.invoke(null, map);
        
        Assert.assertNotNull(target);
        Assert.assertEquals(2, target.id.length);
        Assert.assertEquals(2, target.weight.length);        
    }
}

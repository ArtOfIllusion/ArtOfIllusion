/*
 * Copyright 2018 Veeam Software.
 * 
 * Created by Maksim Khramov
 * Date: Jul 6, 2018.
 */
package artofillusion.animation;

import artofillusion.math.Vec3;
import artofillusion.object.Curve;
import artofillusion.object.Mesh;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    public void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        Vec3[] cp = new Vec3[2];
        cp[0] = new Vec3();
        cp[1] = Vec3.vz();
        float[] smoothness = new float[2];
        smoothness[0] = 0;
        smoothness[1] = 0;
        
        Curve curve = new Curve(cp, smoothness, Mesh.NO_SMOOTHING, true);
        Actor actor = new Actor(curve);
        
        
        Method method = Actor.ActorKeyframe.class.getDeclaredMethod("addWeightsToTable", Actor.ActorKeyframe.class, Map.class, double.class);
        method.setAccessible(true);
        actor.getPoseKeyframe().blend(null, 0, 0);
        
        

    }
    
}

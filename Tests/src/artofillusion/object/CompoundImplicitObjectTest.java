/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.object;

import artofillusion.animation.Keyframe;
import artofillusion.math.BoundingBox;
import artofillusion.math.CoordinateSystem;
import artofillusion.math.Vec3;
import org.junit.Assert;
import org.junit.Test;



/**
 *
 * @author maksim.khramov
 */
public class CompoundImplicitObjectTest {
    
    @Test
    public void testGetInitialCutoff()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      Assert.assertEquals(1.0, cio.getCutoff(), 0);
    }
    
    
    @Test
    public void testCompoundImplicitObjectIsEditable()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      Assert.assertTrue(cio.isEditable());
    }
    
    @Test
    public void testGetEmptyObjectSize() {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      Assert.assertEquals(0, cio.getNumObjects());
    }
    
    @Test
    public void testEmptyCompoundBounds()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      BoundingBox bb = cio.getBounds();
      Assert.assertEquals(0f, bb.minx, 0);
      Assert.assertEquals(0f, bb.miny, 0);
      Assert.assertEquals(0f, bb.minz, 0);
      Assert.assertEquals(0f, bb.maxx, 0);
      Assert.assertEquals(0f, bb.maxy, 0);
      Assert.assertEquals(0f, bb.maxz, 0);
    }
    
    
    @Test
    public void testCompoundBoundsSingle()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      
      cio.addObject(new ImplicitSphere(1.0, 1.0), new CoordinateSystem());
      BoundingBox bb = cio.getBounds();
      
      Assert.assertEquals(-1.0f, bb.minx, 0);
      Assert.assertEquals(-1.0f, bb.miny, 0);
      Assert.assertEquals(-1.0f, bb.minz, 0);
      Assert.assertEquals(1.0f, bb.maxx, 0);
      Assert.assertEquals(1.0f, bb.maxy, 0);
      Assert.assertEquals(1.0f, bb.maxz, 0);
    }
    
    
    @Test
    public void testCompoundBoundsDouble()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      
      cio.addObject(new ImplicitSphere(1.0, 1.0), new CoordinateSystem());
      cio.addObject(new ImplicitSphere(1.0, 2.0), new CoordinateSystem());
      
      BoundingBox bb = cio.getBounds();
      
      Assert.assertEquals(-2.0f, bb.minx, 0);
      Assert.assertEquals(-2.0f, bb.miny, 0);
      Assert.assertEquals(-2.0f, bb.minz, 0);
      Assert.assertEquals(2.0f, bb.maxx, 0);
      Assert.assertEquals(2.0f, bb.maxy, 0);
      Assert.assertEquals(2.0f, bb.maxz, 0);
    }
    
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetObjectFromEmptyCompoundNegative() 
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      cio.getObject(0);
    }
    
    
   
    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetObjectToEmptyCompundNegative() 
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      cio.setObject(0, new ImplicitSphere(1.0, 1.0));
    }
    
    
    @Test
    public void testSetObjectToCompound()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      ImplicitSphere sphereOriginal = new ImplicitSphere(1.0, 1.0);
      cio.addObject(sphereOriginal, new CoordinateSystem());
      
      ImplicitSphere sphereReplacement = new ImplicitSphere(1.0, 1.0);
      
      cio.setObject(0, sphereReplacement);
      ImplicitSphere test = (ImplicitSphere)cio.getObject(0);
      
      Assert.assertEquals(test, sphereReplacement);
      
    }
    
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetObjectCoordinatesForEmptyCompound()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      cio.setObjectCoordinates(0, new CoordinateSystem());
    }
    
    
    
    @Test
    public void testSetObjectCoordinatesForCompound()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      cio.addObject(new ImplicitSphere(1.0, 1.0), new CoordinateSystem());
      cio.setObjectCoordinates(0, new CoordinateSystem(Vec3.vx(), Vec3.vy(), Vec3.vz()));
      
    }
    
    
    @Test
    public void testAddCompound()
    {
      CompoundImplicitObject cio = new CompoundImplicitObject();
      cio.addObject(new ImplicitSphere(1.0, 1.0), new CoordinateSystem());
      Assert.assertEquals(1, cio.getNumObjects());
    }
    
    @Test
    public void testDuplicateEmptyCompound()
    {
      CompoundImplicitObject source = new CompoundImplicitObject();
      CompoundImplicitObject target = (CompoundImplicitObject)source.duplicate();
      
      Assert.assertNotEquals(target, source);
      Assert.assertEquals(0, target.getNumObjects());
    }
    
    @Test
    public void testDuplicateCompound()
    {
      CompoundImplicitObject source = new CompoundImplicitObject();
      ImplicitObject sourceImplicit = new ImplicitSphere(1.0, 1.0);
      CoordinateSystem ccs = new CoordinateSystem();
      
      source.addObject(sourceImplicit, ccs);
      
      CompoundImplicitObject target = (CompoundImplicitObject)source.duplicate();
      
      Assert.assertNotEquals(target, source);
      Assert.assertEquals(1, target.getNumObjects());
      Assert.assertNotEquals(sourceImplicit, target.getObject(0));
      CoordinateSystem tcs =  target.getObjectCoordinates(0);      
      
      Assert.assertEquals(ccs, tcs);
      Assert.assertNotSame(ccs, tcs);
      
    }
    
    @Test
    public void testGetPoseKeyFrameFromEmptyCompound()
    {
      CompoundImplicitObject source = new CompoundImplicitObject();
      
      Keyframe keyframe = source.getPoseKeyframe();
      Assert.assertNotNull(keyframe);
      Assert.assertTrue(keyframe instanceof CompoundImplicitObject.CompoundImplicitKeyframe);
      
      Assert.assertTrue(((CompoundImplicitObject.CompoundImplicitKeyframe)keyframe).key.isEmpty());
    }
    
    

    @Test
    public void testGetPoseKeyFrameFromCompound()
    {
      CompoundImplicitObject source = new CompoundImplicitObject();
      ImplicitObject sourceImplicit = new ImplicitSphere(1.0, 1.0);
      CoordinateSystem ccs = new CoordinateSystem();
      
      source.addObject(sourceImplicit, ccs);
      
      Keyframe keyframe = source.getPoseKeyframe();
      Assert.assertNotNull(keyframe);
      Assert.assertTrue(keyframe instanceof CompoundImplicitObject.CompoundImplicitKeyframe);
      
      Assert.assertFalse(((CompoundImplicitObject.CompoundImplicitKeyframe)keyframe).key.isEmpty());
    }
    
}

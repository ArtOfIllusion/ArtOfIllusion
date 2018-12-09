/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.Scene;
import artofillusion.image.ComplexImage;
import artofillusion.image.filter.ImageFilter;
import artofillusion.math.CoordinateSystem;
import artofillusion.test.util.StreamUtil;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Assert;

import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class SceneCameraTest {
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void loadSceneCameraBadVersion1() throws IOException
    {
        Scene scene = new Scene();        
        ByteBuffer wrap = ByteBuffer.allocate(2);
        wrap.putShort((short)-1); // Object Version
        
        
        
        new SceneCamera(StreamUtil.stream(wrap), scene);
    }
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void loadSceneCameraBadVersion1ReadVesionAgain() throws IOException
    {
        Scene scene = new Scene();
        
        ByteBuffer wrap = ByteBuffer.allocate(4);
        wrap.putShort((short)1); // Object Version
        wrap.putShort((short)-1); // Object Version read AGAIN !!!
        
        
        new SceneCamera(StreamUtil.stream(wrap), scene);
    }
    
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void loadSceneCameraBadVersion2() throws IOException
    {
        Scene scene = new Scene();

        ByteBuffer wrap = ByteBuffer.allocate(2);
        wrap.putShort((short)2); // Object Version
        
        
        new SceneCamera(StreamUtil.stream(wrap), scene);
    }

    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void loadSceneCameraBadVersion2ReadVesionAgain() throws IOException
    {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(4);
        wrap.putShort((short)1); // Object Version
        wrap.putShort((short)3); // Object Version read AGAIN !!!
        
        new SceneCamera(StreamUtil.stream(wrap), scene);
    }
    
    @Test
    public void testLoadSceneCameraVersion0() throws IOException
    {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object Version
        wrap.putShort((short)0); // Object Version read AGAIN !!!
        
        wrap.putDouble(90); // FOV
        wrap.putDouble(500); // DOF
        wrap.putDouble(1000); //Focal distance
        
        wrap.putInt(0); // Camera filters count
        
        SceneCamera sc = new SceneCamera(StreamUtil.stream(wrap), scene);
        Assert.assertNotNull(sc);
        Assert.assertEquals(90, sc.getFieldOfView(), 0);
        Assert.assertEquals(500, sc.getDepthOfField(), 0);
        Assert.assertEquals(1000, sc.getFocalDistance(), 0);
        Assert.assertTrue(sc.isPerspective());
        
        Assert.assertEquals(0, sc.getImageFilters().length);
        
    }
    
    @Test
    public void testLoadSceneCameraVersion2() throws IOException
    {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object Version
        wrap.putShort((short)2); // Object Version read AGAIN !!!
        
        wrap.putDouble(90); // FOV
        wrap.putDouble(500); // DOF
        wrap.putDouble(1000); //Focal distance
        
        wrap.put((byte)1);  // Perspective camera. Boolean treats as byte 
        
        wrap.putInt(0); // Camera filters count

        SceneCamera sc = new SceneCamera(StreamUtil.stream(wrap), scene);
        Assert.assertNotNull(sc);
        Assert.assertEquals(90, sc.getFieldOfView(), 0);
        Assert.assertEquals(500, sc.getDepthOfField(), 0);
        Assert.assertEquals(1000, sc.getFocalDistance(), 0);
        Assert.assertTrue(sc.isPerspective());
        
        Assert.assertEquals(0, sc.getImageFilters().length);
        
    }
    
    @Test
    public void testLoadSceneCameraVersion2NoPersp() throws IOException
    {
        Scene scene = new Scene();

        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object Version
        wrap.putShort((short)2); // Object Version read AGAIN !!!
        
        wrap.putDouble(90); // FOV
        wrap.putDouble(500); // DOF
        wrap.putDouble(1000); //Focal distance
        
        wrap.put((byte)0);  // Non Perspective camera. Boolean treats as byte 
        
        wrap.putInt(0); // Camera filters count
        
        SceneCamera sc = new SceneCamera(StreamUtil.stream(wrap), scene);
        Assert.assertNotNull(sc);
        Assert.assertEquals(90, sc.getFieldOfView(), 0);
        Assert.assertEquals(500, sc.getDepthOfField(), 0);
        Assert.assertEquals(1000, sc.getFocalDistance(), 0);
        Assert.assertTrue(!sc.isPerspective());
        
        Assert.assertEquals(0, sc.getImageFilters().length);
        
    }
    
    @Test(expected = IOException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testLoadCameraWithBadFilter() throws IOException
    {
        Scene scene = new Scene();

        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object Version
        wrap.putShort((short)2); // Object Version read AGAIN !!!
        
        wrap.putDouble(90); // FOV
        wrap.putDouble(500); // DOF
        wrap.putDouble(1000); //Focal distance
        
        wrap.put((byte)0);  // Non Perspective camera. Boolean treats as byte 
        
        wrap.putInt(1); // Camera filters count
        
        String className = "dummy.dummy.UnknownFilterClass";
        
        wrap.putShort(Integer.valueOf(className.length()).shortValue());
        wrap.put(className.getBytes());

        new SceneCamera(StreamUtil.stream(wrap), scene);
                
    }
    
    @Test
    public void testLoadCameraWithGoodFilter() throws IOException
    {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Object Version
        wrap.putShort((short)2); // Object Version read AGAIN !!!
        
        wrap.putDouble(90); // FOV
        wrap.putDouble(500); // DOF
        wrap.putDouble(1000); //Focal distance
        
        wrap.put((byte)0);  // Non Perspective camera. Boolean treats as byte 
        
        wrap.putInt(1); // Camera filters count
        
        String className = DummyImageFilter.class.getTypeName();
        
        wrap.putShort(Integer.valueOf(className.length()).shortValue());
        wrap.put(className.getBytes());

        SceneCamera sc = new SceneCamera(StreamUtil.stream(wrap), scene);
        Assert.assertNotNull(sc);
        
        Assert.assertEquals(1, sc.getImageFilters().length);                
    }
    
    @Test
    public void testSceneCameraDuplicate()
    {
        SceneCamera sc = new SceneCamera();
        sc.setDistToPlane(300);
        sc.setDepthOfField(500);
        sc.setFieldOfView(90);
        sc.setPerspective(false);
        sc.setImageFilters(new ImageFilter[] {new DummyImageFilter()});
        
        SceneCamera clone = sc.duplicate();
        
        Assert.assertNotNull(clone);
        Assert.assertEquals(sc.getDistToPlane(), clone.getDistToPlane(), 0);
        Assert.assertEquals(sc.getDepthOfField(), clone.getDepthOfField(), 0);
        Assert.assertEquals(sc.getFieldOfView(), clone.getFieldOfView(), 0);
        Assert.assertEquals(sc.isPerspective(), clone.isPerspective());
        
        Assert.assertEquals(sc.getImageFilters().length, clone.getImageFilters().length);
    }
    
    @Test
    public void testSceneCameraCopyObject()
    {
        SceneCamera sc = new SceneCamera();
        sc.setDistToPlane(300);
        sc.setDepthOfField(500);
        sc.setFieldOfView(90);
        sc.setPerspective(false);
        sc.setImageFilters(new ImageFilter[] {new DummyImageFilter()});
        
        SceneCamera clone = new SceneCamera();
        clone.copyObject(sc);
        

        Assert.assertEquals(sc.getDistToPlane(), clone.getDistToPlane(), 0);
        Assert.assertEquals(sc.getDepthOfField(), clone.getDepthOfField(), 0);
        Assert.assertEquals(sc.getFieldOfView(), clone.getFieldOfView(), 0);
        Assert.assertEquals(sc.isPerspective(), clone.isPerspective());
        
        Assert.assertEquals(sc.getImageFilters().length, clone.getImageFilters().length); 
    }
    
    public static class DummyImageFilter extends ImageFilter 
    {

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void writeToStream(DataOutputStream out, Scene theScene) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void initFromStream(DataInputStream in, Scene theScene) throws IOException {            
        }
        
    }
}

/* Copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package artofillusion.object;

import artofillusion.Camera;
import artofillusion.Scene;
import artofillusion.animation.FilterParameterTrack;
import artofillusion.animation.PoseTrack;
import artofillusion.animation.WeightTrack;
import artofillusion.image.ComplexImage;
import artofillusion.image.filter.ImageFilter;
import artofillusion.math.BoundingBox;
import artofillusion.math.CoordinateSystem;
import artofillusion.test.util.StreamUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class SceneCameraTest
{
  
  @Test
  public void testGetCameraBounds() {
    SceneCamera camera = new SceneCamera();
    BoundingBox cBox = camera.getBounds();
    Assert.assertEquals(-0.25, cBox.minx, 0);
    Assert.assertEquals(0.25, cBox.maxx, 0);
    
    Assert.assertEquals(-0.15, cBox.miny, 0);
    Assert.assertEquals(0.20, cBox.maxy, 0);
    
    Assert.assertEquals(-0.20, cBox.minz, 0);
    Assert.assertEquals(0.20, cBox.maxz, 0);
  }
  
  
  @Test
  public void testCreateSceneCamera() {
    
    
    SceneCamera camera = new SceneCamera();
    ObjectInfo camObj = new ObjectInfo(camera, new CoordinateSystem(), "Camera");
    Assert.assertTrue(camera.isPerspective());
    Assert.assertEquals(camera, camObj.getObject());
    Assert.assertNull(camObj.getTracks());
    
  }
  
  @Test
  public void testGetEmptySceneCameraFilters() {
    SceneCamera camera = new SceneCamera();
    Assert.assertTrue(camera.isPerspective());
    Assert.assertNotNull(camera.getImageFilters());
    Assert.assertEquals(0, camera.getImageFilters().length);
  }
 
  
  @Test
  public void testSetAndGetImageFiltersForCamera() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter()});
    ImageFilter[] filters = camera.getImageFilters();
    Assert.assertNotNull(filters);
    Assert.assertEquals(1,filters.length);
  }
  
  
  @Test
  public void testDuplicateCameraWithFilters() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter(), new DummyImageFilter()});
    
    SceneCamera duplicate = camera.duplicate();
    Assert.assertEquals(2, duplicate.getImageFilters().length);
    
    for(ImageFilter nf: duplicate.getImageFilters()) Assert.assertNotNull(nf);
  }
  
  
  @Test(expected = ClassCastException.class)
  public void testCopyCameraFromBadObject() {
    SceneCamera camera = new SceneCamera();
    camera.copyObject(new NullObject());
  }
  
  
  @Test
  public void testCopyCameraSimple() {
    SceneCamera camera = new SceneCamera();
    SceneCamera source = new SceneCamera();
    
    source.setPerspective(false); // By default Camera is Perspeсtive
    source.setFieldOfView(45);    // Default 30;
    
    source.setDistToPlane(123);
    source.setDepthOfField(111);
    source.setFocalDistance(55);
    
    
    camera.copyObject(source);
    
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(123, camera.getDistToPlane(), 0);
    Assert.assertFalse(camera.isPerspective());
    Assert.assertEquals(111f, camera.getDepthOfField(), 0);
    Assert.assertEquals(55f, camera.getFocalDistance(), 0);
    Assert.assertEquals(0, camera.getImageFilters().length);
    
  }
  
  
  @Test
  public void testCopyCameraWithFilters() {
    SceneCamera camera = new SceneCamera();
    SceneCamera source = new SceneCamera();
    
    source.setPerspective(false); // By default Camera is Perspeсtive
    source.setFieldOfView(45);    // Default 30;
    
    source.setDistToPlane(123);
    source.setDepthOfField(111);
    source.setFocalDistance(55);
    source.setImageFilters(new ImageFilter[] {new DummyImageFilter(), new DummyImageFilter2()});
    
    camera.copyObject(source);
    
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(123, camera.getDistToPlane(), 0);
    Assert.assertFalse(camera.isPerspective());
    Assert.assertEquals(111f, camera.getDepthOfField(), 0);
    Assert.assertEquals(55f, camera.getFocalDistance(), 0);
    Assert.assertEquals(2, camera.getImageFilters().length);
    
  }

  
  @Test
  public void testConfigurePoseTrackForCameraNoFilters() {
    SceneCamera camera = new SceneCamera();
    
    
    ObjectInfo info = new ObjectInfo(camera, new CoordinateSystem(), "Camera");
    PoseTrack pTrack = new PoseTrack(info);
    
    Assert.assertEquals(1, pTrack.getSubtracks().length);
    Assert.assertTrue(pTrack.getSubtracks()[0] instanceof WeightTrack);
    
    
    System.out.println(Arrays.toString(pTrack.getValueNames()));
  }
  
  
  @Test
  public void testConfigurePoseTrackForCameraSingleFilter() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter()});
    
    ObjectInfo info = new ObjectInfo(camera, new CoordinateSystem(), "Camera");
    PoseTrack pTrack = new PoseTrack(info);
    
    Assert.assertEquals(2, pTrack.getSubtracks().length);
    Assert.assertTrue(pTrack.getSubtracks()[0] instanceof WeightTrack);
    Assert.assertTrue(pTrack.getSubtracks()[1] instanceof FilterParameterTrack);
    
    System.out.println(Arrays.toString(pTrack.getValueNames()));
  }
  

  @Test
  public void testConfigurePoseTrackForCameraDoubleFilters() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter(), new DummyImageFilter()});
    
    ObjectInfo info = new ObjectInfo(camera, new CoordinateSystem(), "Camera");
    PoseTrack pTrack = new PoseTrack(info);
    
    Assert.assertEquals(3, pTrack.getSubtracks().length);
    Assert.assertTrue(pTrack.getSubtracks()[0] instanceof WeightTrack);
    Assert.assertTrue(pTrack.getSubtracks()[1] instanceof FilterParameterTrack);
    
    System.out.println(Arrays.toString(pTrack.getValueNames()));
  }

  @Test
  public void testGetCameraDesiredComponents() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter()});

    Assert.assertEquals(ComplexImage.RED, camera.getComponentsForFilters());
  }

  @Test
  public void testGetCameraDesiredComponents2() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter(), new DummyImageFilter2()});

    Assert.assertEquals(ComplexImage.RED + ComplexImage.ALPHA, camera.getComponentsForFilters());
  }
  
  
  @Test
  public void testGetCameraExtraDesiredComponents() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter()});
    camera.setExtraRequiredComponents(ComplexImage.ALPHA);
    Assert.assertEquals(ComplexImage.RED + ComplexImage.ALPHA, camera.getComponentsForFilters());
  }

  @Test
  public void testGetCameraExtraDesiredComponents2() {
    SceneCamera camera = new SceneCamera();
    camera.setImageFilters(new ImageFilter[] {new DummyImageFilter(), new DummyImageFilter2()});
    camera.setExtraRequiredComponents(ComplexImage.ALPHA);
    Assert.assertEquals(ComplexImage.RED + ComplexImage.ALPHA, camera.getComponentsForFilters());
  }
  
  @Test
  public void testFilterDuplicate() {
    DummyImageFilter filter = new DummyImageFilter();
    ImageFilter duplicate = filter.duplicate();
    Assert.assertNotNull(duplicate);
    
  }
  
  
  
  @Test(expected = InvalidObjectException.class)
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  public void testCreateSceneCameraFromStreamBadObject3dVersion() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(4);
    wrap.putShort((short)2); // Base Object3d version
    
    
    new SceneCamera(StreamUtil.stream(wrap), null);
  }
  
  
  @Test(expected = InvalidObjectException.class)
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  public void testCreateSceneCameraFromStreamBadCameraVersion() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(4);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)4); // Scene Camera object version
    
    new SceneCamera(StreamUtil.stream(wrap), null);
  }
  
  @Test(expected = InvalidObjectException.class)
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  public void testCreateSceneCameraFromStreamBadCameraNegativeVersion() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(4);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)-1); // Scene Camera object version
    
    new SceneCamera(StreamUtil.stream(wrap), null);
  }
  

  @Test
  public void testCreateSceneCameraFromStreamVersion0NoCameraFilters() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(100);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)0); // Scene Camera object version
    
    wrap.putDouble(45); // fov
    wrap.putDouble(100); // depthOfFieled
    wrap.putDouble(50); // focalDistance;

    SceneCamera camera = new SceneCamera(StreamUtil.stream(wrap), null);
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(Camera.DEFAULT_DISTANCE_TO_SCREEN, camera.getDistToPlane(), 0);
    Assert.assertTrue(camera.isPerspective());
    Assert.assertEquals(100f, camera.getDepthOfField(), 0);
    Assert.assertEquals(50f, camera.getFocalDistance(), 0);
    Assert.assertEquals(0, camera.getImageFilters().length);
  }


  @Test
  public void testCreateSceneCameraFromStreamVersion2() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(100);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)2); // Scene Camera object version
    
    wrap.putDouble(45); // fov
    wrap.putDouble(100); // depthOfFieled
    wrap.putDouble(50); // focalDistance;
    wrap.put((byte)1);  // Perspective Boolean treats as byte 
    wrap.putInt(0); // No camera filters
    
    SceneCamera camera = new SceneCamera(StreamUtil.stream(wrap), null);
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(Camera.DEFAULT_DISTANCE_TO_SCREEN, camera.getDistToPlane(), 0);
    Assert.assertTrue(camera.isPerspective());
    Assert.assertEquals(100f, camera.getDepthOfField(), 0);
    Assert.assertEquals(50f, camera.getFocalDistance(), 0);
    Assert.assertEquals(0, camera.getImageFilters().length);
  }
  
  @Test
  public void testCreateSceneCameraFromStreamVersion3() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(100);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)3); // Scene Camera object version
    wrap.putDouble(123); // distToPlane
    wrap.putDouble(45); // fov
    wrap.putDouble(100); // depthOfFieled
    wrap.putDouble(50); // focalDistance;
    wrap.put((byte)1);  // Perspective Boolean treats as byte 
    wrap.putInt(0); // No camera filters
    
    SceneCamera camera = new SceneCamera(StreamUtil.stream(wrap), null);
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(123, camera.getDistToPlane(), 0);
    Assert.assertTrue(camera.isPerspective());
    Assert.assertEquals(100f, camera.getDepthOfField(), 0);
    Assert.assertEquals(50f, camera.getFocalDistance(), 0);
    Assert.assertEquals(0, camera.getImageFilters().length);
  }  
  
  
  
  @Test(expected = IOException.class)
  public void testCreateSceneCameraFromStreamBadFilter() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(100);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)3); // Scene Camera object version
    wrap.putDouble(123); // distToPlane
    wrap.putDouble(45); // fov
    wrap.putDouble(100); // depthOfFieled
    wrap.putDouble(50); // focalDistance;
    wrap.put((byte)1);  // Perspective Boolean treats as byte 
    wrap.putInt(1); // No camera filters
    
    String filterName = "UnknownFilterClass";
    byte[] className = filterName.getBytes("UTF-8");
    Integer  cnl = className.length;
    wrap.putShort(cnl.shortValue());
    wrap.put(className);
    
    SceneCamera camera = new SceneCamera(StreamUtil.stream(wrap), null);
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(123, camera.getDistToPlane(), 0);
    Assert.assertTrue(camera.isPerspective());
    Assert.assertEquals(100f, camera.getDepthOfField(), 0);
    Assert.assertEquals(50f, camera.getFocalDistance(), 0);
    Assert.assertEquals(0, camera.getImageFilters().length);
  }  
  
  
  
  @Test
  public void testCreateSceneCameraFromStreamSingleGoodFilter() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(200);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)3); // Scene Camera object version
    wrap.putDouble(123); // distToPlane
    wrap.putDouble(45); // fov
    wrap.putDouble(100); // depthOfFieled
    wrap.putDouble(50); // focalDistance;
    wrap.put((byte)1);  // Perspective Boolean treats as byte 
    wrap.putInt(1); //One Camera filter
    
    
    String filterName = DummyImageFilter.class.getName();
    
    byte[] className = filterName.getBytes("UTF-8");
    Integer  cnl = className.length;
    wrap.putShort(cnl.shortValue());
    wrap.put(className);
    
    
    
    SceneCamera camera = new SceneCamera(StreamUtil.stream(wrap), null);
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(123, camera.getDistToPlane(), 0);
    Assert.assertTrue(camera.isPerspective());
    Assert.assertEquals(100f, camera.getDepthOfField(), 0);
    Assert.assertEquals(50f, camera.getFocalDistance(), 0);
    Assert.assertEquals(1, camera.getImageFilters().length);
    Assert.assertTrue(camera.getImageFilters()[0] instanceof DummyImageFilter);
  }  
  
  @Test
  public void testCreateSceneCameraFromStreamDoubleGoodFilter() throws IOException {
    ByteBuffer wrap = ByteBuffer.allocate(200);
    wrap.putShort((short)1); // Base Object3d version
    wrap.putShort((short)3); // Scene Camera object version
    wrap.putDouble(123); // distToPlane
    wrap.putDouble(45); // fov
    wrap.putDouble(100); // depthOfFieled
    wrap.putDouble(50); // focalDistance;
    wrap.put((byte)1);  // Perspective Boolean treats as byte 
    wrap.putInt(2); //One Camera filter
    
    
    String filterName = DummyImageFilter.class.getName();
    
    byte[] className = filterName.getBytes("UTF-8");
    Integer  cnl = className.length;
    wrap.putShort(cnl.shortValue());
    wrap.put(className);
    
    filterName = DummyImageFilter2.class.getName();    
    className = filterName.getBytes("UTF-8");
    cnl = className.length;
    wrap.putShort(cnl.shortValue());
    wrap.put(className);
    
    
    
    SceneCamera camera = new SceneCamera(StreamUtil.stream(wrap), null);
    Assert.assertEquals(45f, camera.getFieldOfView(), 0);
    Assert.assertEquals(123, camera.getDistToPlane(), 0);
    Assert.assertTrue(camera.isPerspective());
    Assert.assertEquals(100f, camera.getDepthOfField(), 0);
    Assert.assertEquals(50f, camera.getFocalDistance(), 0);
    Assert.assertEquals(2, camera.getImageFilters().length);
    Assert.assertTrue(camera.getImageFilters()[0] instanceof DummyImageFilter);
    Assert.assertTrue(camera.getImageFilters()[1] instanceof DummyImageFilter2);
  }  
  
  
  public static class DummyImageFilter extends ImageFilter {
    
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

    @Override
    public int getDesiredComponents()
    {
      return ComplexImage.RED;
    }
  }
  
  public static class DummyImageFilter2 extends ImageFilter {

    @Override
    public String getName()
    {
        return "Dummy Filter Other";
    }

    @Override
    public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void initFromStream(DataInputStream in, Scene theScene) throws IOException
    {
    }
    
    @Override
    public int getDesiredComponents()
    {
      return ComplexImage.ALPHA;
    }

  }
}

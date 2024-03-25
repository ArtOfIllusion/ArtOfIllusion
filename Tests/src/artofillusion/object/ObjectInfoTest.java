/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.RenderingMesh;
import artofillusion.animation.PoseTrack;
import artofillusion.animation.PositionTrack;
import artofillusion.animation.RotationTrack;
import artofillusion.animation.Skeleton;
import artofillusion.animation.TextureTrack;
import artofillusion.animation.Track;
import artofillusion.animation.distortion.Distortion;
import artofillusion.material.Material;
import artofillusion.material.MaterialMapping;
import artofillusion.material.UniformMaterial;
import artofillusion.math.CoordinateSystem;
import artofillusion.math.Vec3;
import artofillusion.texture.Texture;
import artofillusion.texture.TextureMapping;
import artofillusion.texture.UniformTexture;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author MaksK
 */
public class ObjectInfoTest
{
  

  /**
   * Test to create new ObjectInfo object with all defaults
   */
  @Test
  public void testCreateNewObjectInfo()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    assertNotNull(test);
    
    assertEquals("Test", test.getName());
    assertEquals(-1, test.getId());
    assertTrue(test.isVisible());
    assertFalse(test.isLocked());
    assertNull(test.getPose());
    
    assertNull(test.getParent());
    assertNotNull(test.getChildren());
    assertEquals(0, test.getChildren().length);
    
    assertNull(test.getTracks());
    assertNull(test.getDistortion());
    
  }
  
  /**
   * Test to check that parent object is set and get properly
   */
  @Test
  public void testSetObjectInfoParent()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Null");
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    
    test.setParent(parent);
    assertNotNull(test.getParent());
    assertEquals(parent, test.getParent());
    
  }
  
  /**
   * Test to add some tracks
   */
  @Test
  public void testAddTrack()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.addTrack(new PositionTrack(test), 0);
    test.addTrack(new RotationTrack(test), 1);
    
    assertNotNull(test.getTracks());
    assertEquals(2, test.getTracks().length);
    
    assertTrue(test.getTracks()[0] instanceof PositionTrack);
    assertTrue(test.getTracks()[1] instanceof RotationTrack);
  }

  /**
   *
   */
  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testAddTrackToGivenErrorPos()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.addTrack(new PositionTrack(test), 5);

  }
  
  /**
   *
   */
  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testAddTrackToGivenPosInExistList()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.addTrack(new RotationTrack(test), 1);
    
    test.addTrack(new PositionTrack(test), 5);
    
  }
  
  /**
   * Test check added track can be found and removed 
   */
  @Test
  public void testRemoveTrackByTrack()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    Track pTrack = new PositionTrack(test);
    Track rTrack = new RotationTrack(test);
    
    test.addTrack(pTrack, 0);
    test.addTrack(rTrack, 1);
    
    test.removeTrack(pTrack);
    
    assertNotNull(test.getTracks());
    assertEquals(1, test.getTracks().length);
    assertTrue(test.getTracks()[0] instanceof RotationTrack);
  }
  
  /**
   * Test to remove track by position 
   */
  @Test
  public void testRemoveTrackByPositionFirst()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    Track pTrack = new PositionTrack(test);
    Track rTrack = new RotationTrack(test);
    
    test.addTrack(pTrack, 0);
    test.addTrack(rTrack, 1);
    
    test.removeTrack(0);
    
    assertNotNull(test.getTracks());
    assertEquals(1, test.getTracks().length);
    assertTrue(test.getTracks()[0] instanceof RotationTrack);
  }
  
  /**
   * Test to remove track by position 
   */
  @Test
  public void testRemoveTrackByPositionLast()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    Track pTrack = new PositionTrack(test);
    Track rTrack = new RotationTrack(test);
    
    test.addTrack(pTrack, 0);
    test.addTrack(rTrack, 1);
    
    test.removeTrack(1);
    
    assertNotNull(test.getTracks());
    assertEquals(1, test.getTracks().length);
    assertTrue(test.getTracks()[0] instanceof PositionTrack);
  }
  
  /**
   * Test objectInfo duplicate
   */
  @Test
  public void testDuplicate()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.setVisible(false);
    test.setLocked(true);
    
    ObjectInfo duplicate = test.duplicate();
   
    assertNotNull(duplicate);
    assertFalse(duplicate.isVisible());
    assertTrue(duplicate.isLocked());
    assertNotEquals(duplicate, test);
    assertEquals(duplicate.getObject(), test.getObject());
  }
  
  /**
   * Test objectInfo replace geometry and duplicate
   */
  @Test
  public void testDuplicateWithNewGeometry()
  {
    ObjectInfo source = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    source.setVisible(false);
    source.setLocked(true);
    
    Object3D newObj = new Sphere(1d, 1d, 1d);
    
    ObjectInfo duplicate = source.duplicate(newObj);
    assertNotNull(duplicate);
    assertFalse(duplicate.isVisible());
    assertTrue(duplicate.isLocked());
    assertNotEquals(duplicate, source);
    assertNotEquals(duplicate.getObject(), source.getObject());
    assertTrue(duplicate.getObject() instanceof Sphere);
    
  }
  
  /**
   * Test objectInfo replace geometry and duplicate with existed tracks data
   */
  @Test
  public void testDuplicateWithNewGeometryAndTracks()
  {
    ObjectInfo source = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    source.setVisible(false);
    source.setLocked(true);

    source.addTrack(new PoseTrack(source), 0);
    source.addTrack(new PositionTrack(source), 1);
    source.addTrack(new RotationTrack(source),2);
    
    Object3D newObj = new Sphere(1d, 1d, 1d);
    
    ObjectInfo duplicate = source.duplicate(newObj);
    assertNotNull(duplicate);
    assertFalse(duplicate.isVisible());
    assertTrue(duplicate.isLocked());
    
    assertNotEquals(duplicate, source);
    assertNotEquals(duplicate.getObject(), source.getObject());
    assertTrue(duplicate.getObject() instanceof Sphere);
    
    assertEquals(3, duplicate.getTracks().length);
    assertEquals(duplicate, duplicate.getTracks()[0].getParent());
  }
  
  /**
   * Test objectInfo duplicate with existed tracks
   */
  @Test
  public void testDuplicateWithTracks() {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.setVisible(false);
    test.setLocked(true);
    
    test.addTrack(new PositionTrack(test), 0);
    test.addTrack(new RotationTrack(test), 1);
    
    ObjectInfo duplicate = test.duplicate();
   
    assertNotNull(duplicate);
    assertFalse(duplicate.isVisible());
    assertTrue(duplicate.isLocked());
    assertNotEquals(duplicate, test);
    assertEquals(duplicate.getObject(), test.getObject());
    
    assertNotNull(duplicate.getTracks());
    assertEquals(2, duplicate.getTracks().length);
    
    assertTrue(duplicate.getTracks()[0] instanceof PositionTrack);
    assertTrue(duplicate.getTracks()[1] instanceof RotationTrack);
    
    assertEquals(duplicate, duplicate.getTracks()[0].getParent());
    assertNull(duplicate.getDistortion());

  }
  
  /**
   * Test objectInfo duplicate with distortion data
   */
  @Test
  public void testDuplicateWithDistortion()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.setDistortion(new DistortionImpl());
    
    ObjectInfo duplicate = test.duplicate();
    assertNotNull(duplicate.getDistortion());
    
  }
  
  /**
   * Test objectInfo to add child objects at the beginning of list
   */  
  @Test
  public void testAddChildFirst()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    ObjectInfo childTwo = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Sphere");
    parent.addChild(childOne, 0);
    parent.addChild(childTwo, 0);
    
    assertEquals(2, parent.getChildren().length);
    assertEquals(childTwo, parent.getChildren()[0]);
    assertEquals(childOne, parent.getChildren()[1]);
  }
  
  /**
   * Test objectInfo to add child objects at the end of list
   */    
  @Test
  public void testAddChildTwo()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    ObjectInfo childTwo = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Sphere");
    parent.addChild(childOne, 0);
    parent.addChild(childTwo, 1);
    
    assertEquals(2, parent.getChildren().length);
    assertEquals(childOne, parent.getChildren()[0]);
    assertEquals(childTwo, parent.getChildren()[1]);
    
  }
  
  /**
   * Test objectInfo to remove given child from list 
   */  
  @Test
  public void testRemoveChildByChild()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    ObjectInfo childTwo = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Sphere");
    
    parent.addChild(childOne, 0);
    parent.addChild(childTwo, 1);
    
    parent.removeChild(childOne);

    assertEquals(1, parent.getChildren().length);
    assertEquals(childTwo, parent.getChildren()[0]);
    Assert.assertNull(childOne.parent);
  }
  
  /**
   * Test objectInfo to remove non existed child from list 
   */  
  @Test
  public void testRemoveChildByMissedChild()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    ObjectInfo childTwo = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Sphere");
    
    parent.addChild(childOne, 0);
    
    
    parent.removeChild(childTwo);

    assertEquals(1, parent.getChildren().length);
    assertEquals(childOne, parent.getChildren()[0]);
    
  }
  
  /**
   * Test objectInfo to remove null child from list 
   */  
  @Test
  public void testRemoveChildByNullChild()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    
    parent.addChild(childOne, 0);
    
    
    parent.removeChild(null);

    assertEquals(1, parent.getChildren().length);
    assertEquals(childOne, parent.getChildren()[0]);
    
  }
  
  /**
   * Test objectInfo to remove child from list by position
   */   
  @Test
  public void testRemoveChildByPositionOne()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    ObjectInfo childTwo = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Sphere");
    
    parent.addChild(childOne, 0);
    parent.addChild(childTwo, 1);
    
    parent.removeChild(1);

    assertEquals(1, parent.getChildren().length);
    assertEquals(childOne, parent.getChildren()[0]);
    Assert.assertNull(childTwo.parent);
    
  }
  
  /**
   * Test objectInfo to remove child from list by position
   */   
  @Test
  public void testRemoveChildByPositionTwo()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    ObjectInfo childTwo = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Sphere");
    
    parent.addChild(childOne, 0);
    parent.addChild(childTwo, 1);
    
    parent.removeChild(0);

    assertEquals(1, parent.getChildren().length);
    assertEquals(childTwo, parent.getChildren()[0]);
    Assert.assertNull(childOne.parent);
    
  }
  
  /**
   * Test objectInfo copy data from other objectInfo and points to same geometry
   */
  @Test
  public void testCopyInfo()
  {
    Object3D sourceGeometry = new Cube(1d,1d,1d);
    CoordinateSystem sourceCoords = new CoordinateSystem(Vec3.vx(), Vec3.vy(), Vec3.vz());
    
    ObjectInfo source = new ObjectInfo(sourceGeometry, sourceCoords, "Source");
    source.setVisible(false);
    source.setLocked(true);
    source.setId(100);
    ObjectInfo target = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Target");
    
    target.copyInfo(source);
    
    assertEquals(100, target.getId());
    assertEquals("Source", target.getName());
    assertEquals(sourceGeometry, target.getObject());
    assertEquals(sourceCoords, target.getCoords());
    
    assertTrue(target.isLocked());
    assertFalse(target.isVisible());
    
  }
  
  /**
   * Test objectInfo copy data from other objectInfo and points to same geometry
   * Checks that source empty tracks overwrite existed one
   */  
  public void testCopyInfoWithEmptyTracksOverExisted()
  {
    ObjectInfo source = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Source");
    
    ObjectInfo target = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Target");
    target.addTrack(new PositionTrack(target), 0);
    target.addTrack(new RotationTrack(target), 1);
    
    target.copyInfo(source);
    assertNull(target.getTracks());
    
    
  }
  
  /**
   * Test objectInfo copy data from other objectInfo and points to same geometry
   * Checks that source tracks overwrite existed one
   */  
  @Test  
  public void testCopyInfoWithTracksOverExisted()
  {
    ObjectInfo source = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Source");
    source.addTrack(new TextureTrackImpl((source)), 0);
    
    ObjectInfo target = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Target");
    target.addTrack(new PositionTrack(target), 0);
    target.addTrack(new RotationTrack(target), 1);
    
    target.copyInfo(source);
    assertNotNull(target.getTracks());
    assertEquals(1, target.getTracks().length);
    Track testT = target.getTracks()[0];
    assertTrue(testT instanceof TextureTrack);
    assertEquals(target, target.getTracks()[0].getParent());
  }
  
  /**
   * Test objectInfo copy data from other objectInfo and points to same geometry
   * Checks that source distortion copied
   */ 
  @Test
  public void testCopyInfoWithDistortionOverEmpty()
  {
    ObjectInfo source = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Source");
    Distortion dist = new DistortionImpl();
    source.setDistortion(dist);
    
    ObjectInfo target = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Target");
    target.copyInfo(source);
    assertNotNull(target.getDistortion());
    assertTrue(target.getDistortion() instanceof DistortionImpl);
    
  }
  
  /**
   * Test objectInfo copy data from other objectInfo and points to same geometry
   * Checks that empty distortion data overwrites existed one
   */ 
  @Test
  public void testCopyInfoWithNullDistortionOverExisted()
  {
    ObjectInfo source = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Source");    
    ObjectInfo target = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Target");
    
    target.setDistortion(new DistortionImpl());
    
    target.copyInfo(source);
    assertNull(target.getDistortion());
    
  }
  
  /**
   * Test checks new distortion sets for object
   */
  @Test
  public void testSetDistortion()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.setDistortion(new DistortionImpl());
    
    
    Distortion cd = test.getDistortion();
    assertNotNull(cd);
    assertNull(cd.getPreviousDistortion());
    
  }
  
  /**
   * Test checks new distortion adds to non existed one
   */
  @Test
  public void testAddDistortionToNull()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.addDistortion(new DistortionImpl());
    Distortion cd = test.getDistortion();
    assertNotNull(cd);
    assertNull(cd.getPreviousDistortion());
    
  }

  /**
   * Test checks new distortion adds to already existed one
   */
  @Test
  public void testAddDistortionToExisted()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    Distortion dist1 = new DistortionImpl();
    Distortion dist2 = new DistortionImpl();
    
    test.addDistortion(dist1);
    test.addDistortion(dist2);
    
    Distortion cd = test.getDistortion();
    assertNotNull(cd);
    assertNotNull(cd.getPreviousDistortion());
    
  }  
  
  /**
   * Test clears non existed distortion 
   */
  @Test
  public void testClearNullDistortion()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    test.clearDistortion();
    
    assertNull(test.getDistortion());
    assertFalse(test.isDistorted());
  }
  
  /**
   * Test clears existed distortion from object
   */
  @Test
  public void testClearNotNullDistortion()
  {
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    Distortion dist = new DistortionImpl();
            
    test.setDistortion(dist);
    assertNull(dist.getPreviousDistortion());
    
    test.clearDistortion();
    
    assertNull(test.getDistortion());
    assertFalse(test.isDistorted());
    
  }
  
  /**
   * Test sets and checks material add
   */
  @Test
  public void setObjectInfoMaterial()
  {
    Object3D cube = new Cube(1d,1d,1d);
    ObjectInfo test = new ObjectInfo(cube, new CoordinateSystem(), "Test");
    Material mat = new UniformMaterial();
    MaterialMapping map = mat.getDefaultMapping(cube);
    test.setMaterial(mat, map);
    
    Object3D res = test.getObject();
    assertNotNull(res.getMaterial());
    assertNotNull(res.getMaterialMapping());
    assertEquals(mat, res.getMaterial());
    assertEquals(map, res.getMaterialMapping());
  }
  
  /**
   * Test to get skeleton data
   */
  @Test
  public void testGetCubeSkeleton()
  {
    Object3D cube = new Cube(1d,1d,1d);
    ObjectInfo test = new ObjectInfo(cube, new CoordinateSystem(), "Test");
    
    assertNull(test.getSkeleton());
  }
  
  /**
   * Test to get skeleton data from mesh object
   */
  @Test
  public void testGetMeshSkeleton()
  {
    Object3D meshCube = new Cube(1d,1d,1d);
    TriangleMesh mesh = meshCube.convertToTriangleMesh(0.1); 
    
    
    ObjectInfo test = new ObjectInfo(meshCube, new CoordinateSystem(), "Test");
    test.setObject(mesh);
    
    mesh = (TriangleMesh)test.getObject();
    Skeleton sc = new Skeleton();
    
    mesh.setSkeleton(sc);
    assertNotNull(test.getSkeleton());
    assertEquals(sc, test.getSkeleton());
    
  }
  
  /**
   * Test sets texture to object and checks that existed texture tracks changes its parameters
   */
  @Test
  public void testSetTextureWithTextureTracks()
  {
    Object3D cube = new Cube(1d,1d,1d);
    ObjectInfo test = new ObjectInfo(cube, new CoordinateSystem(), "Test");
    
    TextureTrackImpl tt = new TextureTrackImpl(test);
    
    test.addTrack(tt, 0);
    test.addTrack(new PositionTrack(test), 1);
    
    Texture tex = new UniformTexture();
    TextureMapping map = tex.getDefaultMapping(cube);
    
    test.setTexture(tex, map);
    
    assertEquals(1, tt.getParameterChangedEventFireCount());
    assertEquals(tex, test.getObject().getTexture());
    assertEquals(map, test.getObject().getTextureMapping());
    
  }
  /**
   * Test check that getDistortedObject without distortion returns unchanged object
   */  
  @Test
  public void testGetDistortedObjectNoDistortion()
  {
    Object3D cube = new Cube(1d,1d,1d);
    ObjectInfo test = new ObjectInfo(cube, new CoordinateSystem(), "Test");

    Object3D distorted = test.getDistortedObject(0.1d);
    assertNotNull(distorted);
    assertEquals(test.getObject(), distorted);
    
  }
  /**
   * Test check that getDistortedObject without distortion returns unchanged object
   * Distorted object is wrapperd with ObjectWrapper
   */  
  @Test
  public void testGetDistortedObjectFromWrapperNoDistortion()
  {
    Object3D cube = new Cube(1d,1d,1d);
    ObjectWrapperImpl wrapper = new ObjectWrapperImpl(cube);
    ObjectInfo test = new ObjectInfo(wrapper, new CoordinateSystem(), "Test");

    Object3D distorted = test.getDistortedObject(0.1d);
    assertNotNull(distorted);
    assertEquals(test.getObject(), distorted);
    
  }  
  
  @Test(expected = NullPointerException.class)
  public void testGetRenderingMeshWithUnsetTexture()
  {
    Object3D cube = new Cube(1d,1d,1d);
    ObjectInfo test = new ObjectInfo(cube, new CoordinateSystem(), "Test");
    
    assertNotNull(test.getRenderingMesh(0.1d));
    assertTrue(test.getRenderingMesh(0.1d) instanceof RenderingMesh);
  }
  
  @Test
  public void testGetRenderingMesh()
  {
    Object3D cube = new Cube(1d,1d,1d);
    ObjectInfo test = new ObjectInfo(cube, new CoordinateSystem(), "Test");
    Texture tex = new UniformTexture();
    TextureMapping map = tex.getDefaultMapping(cube);
    
    test.setTexture(tex, map);
    
    assertNotNull(test.getRenderingMesh(0.1d));
    assertTrue(test.getRenderingMesh(0.1d) instanceof RenderingMesh);
  }
  

  @Test
  public void testDuplicateAll()
  {
    ObjectInfo parent = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Parent");
    ObjectInfo childOne = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Cube");
    ObjectInfo childTwo = new ObjectInfo(new Sphere(1d,1d,1d), new CoordinateSystem(), "Sphere");
    parent.addChild(childOne, 0);
    parent.addChild(childTwo, 0);
    ObjectInfo test = new ObjectInfo(new Cube(1d,1d,1d), new CoordinateSystem(), "Test");
    ObjectInfo deep = new ObjectInfo(new NullObject(), new CoordinateSystem(), "Null");
    ObjectInfo wrapper = new ObjectInfo(new ObjectWrapperImpl(new Cylinder(5, 1, 2, 3)), new CoordinateSystem(), "Wrapper");
    deep.addChild(wrapper, 0);
    
    test.addTrack(new PositionTrack(test), 0);
    test.addTrack(new RotationTrack(test), 1);
    
    ObjectInfo[] source = new ObjectInfo[] {parent,test,deep};    
    ObjectInfo[] result = ObjectInfo.duplicateAll(source);
    
    assertNotNull(result);
    assertEquals(3, result.length);
    assertEquals(2, result[1].getTracks().length);
  }
  
  private static class TextureTrackImpl extends TextureTrack
  {
    private int parameterChangedEventFireCount = 0;
    
    public TextureTrackImpl(ObjectInfo info)
    {
      super(info);
    }

    @Override
    public void parametersChanged()
    {     
      parameterChangedEventFireCount++;
    }

    public int getParameterChangedEventFireCount()
    {
      return parameterChangedEventFireCount;
    }
    
    
  }
  private static class DistortionImpl extends Distortion
  {

    public DistortionImpl()
    {
    }

    @Override
    public boolean isIdenticalTo(Distortion d)
    {
        return false;
    }

    @Override
    public Distortion duplicate()
    {
        return new DistortionImpl();
    }

    @Override
    public Mesh transform(Mesh obj)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
  
  private static class ObjectWrapperImpl extends ObjectWrapper
  {

    public ObjectWrapperImpl(Object3D target) {
      this.theObject = target;
    }
    
    @Override
    public Object3D duplicate()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void copyObject(Object3D obj)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSize(double xsize, double ysize, double zsize)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
  }
  

}

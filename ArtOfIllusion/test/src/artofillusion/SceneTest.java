/* Copyright (C) 2016 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.CoordinateSystem;
import artofillusion.math.RGBColor;
import artofillusion.math.Vec3;
import artofillusion.object.NullObject;
import artofillusion.object.ObjectInfo;
import artofillusion.object.SceneCamera;
import artofillusion.object.Sphere;
import artofillusion.object.SpotLight;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author MaksK
 */
public class SceneTest {

    public SceneTest() 
    {
    }

    @Test
    public void testGetSceneHasNoCamerasList() 
    {
      Scene scene = new Scene();
      List<ObjectInfo> cameras = scene.getCameras();
      assertNotNull(cameras);
      assertTrue(cameras.isEmpty());
    }
    
    @Test
    public void testGetSceneHasSingleCameraOnly()
    {
      Scene scene = new Scene();
      CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
      ObjectInfo info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");

      scene.addObject(info, null);
      
      List<ObjectInfo> cameras = scene.getCameras();
      assertNotNull(cameras);
      assertEquals(1, cameras.size());
      assertTrue(cameras.get(0).getObject() instanceof SceneCamera );
    }
    
    @Test
    public void testGetSceneHasCamerasOnly()
    {
      Scene scene = new Scene();
      CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
      ObjectInfo info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");

      scene.addObject(info, null);
      
      info = new ObjectInfo(new SceneCamera(), coords, "Camera 2");

      scene.addObject(info, null);
      
      List<ObjectInfo> cameras = scene.getCameras();
      assertNotNull(cameras);
      assertEquals(2, cameras.size());
      for( ObjectInfo cameraObj: cameras)
      {
        assertTrue(cameraObj.getObject() instanceof SceneCamera );
      }
      
    }
    
    @Test
    public void testSceneHasCameraAndOther()
    {
      Scene scene = new Scene();
      
      CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
      ObjectInfo info = new ObjectInfo(new NullObject(), coords, "Null Object");

      scene.addObject(info, null);
      info = new ObjectInfo(new Sphere(1.0, 1.0, 1.0), coords, "Sphere 1");
      scene.addObject(info, null);
      
      info = new ObjectInfo(new SpotLight(new RGBColor(), 1.9f, 90.0, 5.0, 5.0), coords, "SpotLight 1");
      scene.addObject(info, null);      
      
      info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");
      scene.addObject(info, null);
      
      assertTrue(scene.getNumObjects() == 4);
      List<ObjectInfo> cameras = scene.getCameras();
      assertNotNull(cameras);
      assertEquals(1, cameras.size());
      assertTrue(cameras.get(0).getObject() instanceof SceneCamera ); 
      
    }
    

}

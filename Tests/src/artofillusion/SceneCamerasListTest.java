/* Copyright (C) 2016 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.PositionTrack;
import artofillusion.animation.RotationTrack;
import artofillusion.math.CoordinateSystem;
import artofillusion.math.Vec3;
import artofillusion.object.ObjectInfo;
import artofillusion.object.SceneCamera;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author MaksK
 */
public class SceneCamerasListTest {

    public SceneCamerasListTest() 
    {
    }


    

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void testGetSceneCameras() 
    {
      Scene scene = new Scene();
      CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
      ObjectInfo info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");
      info.addTrack(new PositionTrack(info), 0);
      info.addTrack(new RotationTrack(info), 1);
      scene.addObject(info, null);
      
      List<ObjectInfo> cameras = scene.getCameras();
      assertNotNull(cameras);
      assertEquals(1, cameras.size());
    }
}

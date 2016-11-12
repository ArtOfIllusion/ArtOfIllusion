/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

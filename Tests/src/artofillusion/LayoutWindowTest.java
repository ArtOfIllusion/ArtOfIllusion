/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.animation.distortion.*;

import artofillusion.math.CoordinateSystem;
import artofillusion.math.Vec3;
import artofillusion.object.Curve;
import artofillusion.object.ObjectInfo;
import artofillusion.object.Sphere;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.*;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.netbeans.jemmy.Bundle;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;

/**
 *
 * @author MaksK
 */
public class LayoutWindowTest
{

  private JMenuBarOperator appMainMenu;
  private JFrameOperator appFrame;

  private Scene scene;
  private LayoutWindow layout;

  private static final Bundle bundle = new Bundle();

  @Rule
  public TestName name = new TestName();

  @Rule
  public TestWatcher watcher = new TestWatcher()
  {
    @Override
    protected void succeeded(Description description)
    {
      super.succeeded(description);
      System.out.println(" (Pass)");
    }
    
    @Override
    protected void failed(Throwable e, Description description)
    {
      super.failed(e, description);
      System.out.println(" (Fail)");
      org.netbeans.jemmy.util.PNGEncoder.captureScreen(name.getMethodName()+".png");
    }
  };

  @BeforeClass
  public static void setupClass() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, URISyntaxException, IOException
  {

    Locale.setDefault(Locale.ENGLISH);
    new ClassReference("artofillusion.ArtOfIllusion").startApplication();
    bundle.load(ArtOfIllusion.class.getClassLoader().getResourceAsStream("artofillusion.properties"));
    JemmyProperties.setCurrentOutput(TestOut.getNullOutput());
  }

  @Before
  public void setUp()
  {
    appFrame = new JFrameOperator("Untitled");
    appMainMenu = new JMenuBarOperator(appFrame);
    appMainMenu.closeSubmenus();
    
    layout = (LayoutWindow) ArtOfIllusion.getWindows()[0];
    layout.updateImage();
    layout.updateMenus();
    scene = layout.getScene();

    System.out.print("Executing Test Name: " + name.getMethodName());
  }

  @After
  public void done()
  {
    int scc = scene.getNumObjects();
    for (int i = 2; i < scc; i++)
    {
      layout.removeObject(2, null);
    }
    layout.updateImage();
    layout.updateMenus();
    
    try {
      Thread.sleep(1000);
    } catch(InterruptedException ie) {
      
    }
  }

  @Test
  public void testInvokePreferencesCommand()
  {
    appMainMenu.pushMenuNoBlock("Edit|Preferences...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, bundle.getResource("prefsTitle"));

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

  @Test
  public void testInvokeImagesCommand()
  {
    appMainMenu.pushMenuNoBlock("Scene|Images...");
    JDialogOperator dialog = new JDialogOperator("Images");

    JButtonOperator ok = new JButtonOperator(dialog, "OK");
    ok.clickMouse();
  }

  @Test
  public void testInvokeRenderSceneCommand()
  {
    appMainMenu.pushMenuNoBlock("Scene|Render Scene...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Rendering Options");

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

  @Test
  public void testInvokePreviewAnimationCommand()
  {

    appMainMenu.pushMenuNoBlock("Animation|Preview Animation");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Render Wireframe Preview");

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

//<editor-fold defaultstate="collapsed" desc="Test bulk keyframe commands">
  @Test
  public void testInvokeMoveKeyFramesCommand()
  {
    appMainMenu.pushMenuNoBlock("Animation|Bulk Edit Keyframes|Move...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Move Keyframes");

    JButtonOperator cancel = new JButtonOperator(dialog, "Cancel");
    cancel.clickMouse();
  }

  @Test
  public void testInvokeCopyKeyFramesCommand()
  {
    appMainMenu.pushMenuNoBlock("Animation|Bulk Edit Keyframes|Copy...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Copy Keyframes");
    JButtonOperator cancel = new JButtonOperator(dialog, "Cancel");
    cancel.clickMouse();
  }

  @Test
  public void testInvokeRescaleKeyFramesCommand()
  {
    appMainMenu.pushMenuNoBlock("Animation|Bulk Edit Keyframes|Rescale...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Rescale Keyframes");
    JButtonOperator cancel = new JButtonOperator(dialog, "Cancel");
    cancel.clickMouse();
  }

  @Test
  public void testInvokeDeleteKeyFramesCommand()
  {
    appMainMenu.pushMenuNoBlock("Animation|Bulk Edit Keyframes|Delete...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Delete Keyframes");
    JButtonOperator cancel = new JButtonOperator(dialog, "Cancel");
    cancel.clickMouse();
  }
//</editor-fold>

  @Test
  public void testInvokeFrameForwardCommand()
  {
    double timeStep = 1.0 / scene.getFramesPerSecond();
    double sceneTime = scene.getTime();

    appMainMenu.pushMenu("Animation|Forward One Frame");

    assertEquals(scene.getTime(), sceneTime + timeStep, timeStep * 0.01);
  }

  @Test
  public void testInvokeFrameBackwardCommand()
  {
    double timeStep = 1.0 / scene.getFramesPerSecond();
    double sceneTime = scene.getTime();

    appMainMenu.pushMenu("Animation|Back One Frame");
    assertEquals(scene.getTime(), sceneTime - timeStep, timeStep * 0.01);
  }

  @Test
  public void testInvokeJumpToTimeCommand()
  {
    appMainMenu.pushMenuNoBlock("Animation|Jump To Time...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Jump To Time");

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

//<editor-fold defaultstate="collapsed" desc="Test Add Track commands">
  @Test
  public void testAddTrackPositionXYZOneCommand()
  {
    executeTrackMenu("Position|XYZ (One Track)", PositionTrack.class, 3);
  }

  @Test
  public void testAddTrackPositionXYZThreeCommand()
  {
    executeTrackMenu("Position|XYZ (Three Tracks)", PositionTrack.class, 5);
  }

  @Test
  public void testAddTrackPositionXYZProceduralCommand()
  {
    executeTrackMenu("Position|Procedural", ProceduralPositionTrack.class, 3);
  }

  @Test
  public void testAddTrackRotationXYZOneCommand()
  {
    executeTrackMenu("Rotation|XYZ (One Track)", RotationTrack.class, 3);
  }

  @Test
  public void testAddTrackRotationXYZThreeCommand()
  {
    executeTrackMenu("Rotation|XYZ (Three Tracks)", RotationTrack.class, 5);
  }

  @Test
  public void testAddTrackRotationXYZProceduralCommand()
  {
    executeTrackMenu("Rotation|Procedural", ProceduralRotationTrack.class, 3);
  }

  @Test
  public void testAddTrackRotationXYZQuaternionCommand()
  {
    executeTrackMenu("Rotation|Quaternion", RotationTrack.class, 3);
  }

  @Test
  public void testAddPoseTrackCommand()
  {
    executeTrackMenu("Pose", PoseTrack.class, 3);
  }

  @Test
  public void testAddBendTrackCommand()
  {
    executeTrackMenu("Distortion|Bend", BendTrack.class, 3);
  }

  @Test
  public void testAddCustomTrackCommand()
  {
    executeTrackMenu("Distortion|Custom", CustomDistortionTrack.class, 3);
  }

  @Test
  public void testAddScaleTrackCommand()
  {
    executeTrackMenu("Distortion|Scale", ScaleTrack.class, 3);
  }

  @Test
  public void testAddShatterTrackCommand()
  {
    executeTrackMenu("Distortion|Shatter", ShatterTrack.class, 3);
  }

  @Test
  public void testAddTwistTrackCommand()
  {
    executeTrackMenu("Distortion|Twist", TwistTrack.class, 3);
  }

  @Test
  public void testAddIKTrackCommand()
  {
    executeTrackMenu("Distortion|Inverse Kinematics", IKTrack.class, 3);
  }

  @Test
  public void testAddSkeletonShapeTrackCommand()
  {
    executeTrackMenu("Distortion|Skeleton Shape", SkeletonShapeTrack.class, 3);
  }

  @Test
  public void testAddConstraintTrackCommand()
  {
    executeTrackMenu("Constraint", ConstraintTrack.class, 3);
  }

  @Test
  public void testAddVisibilityTrackCommand()
  {
    executeTrackMenu("Visibility", VisibilityTrack.class, 3);
  }

  @Test
  public void testAddTextureParameterTrackCommand()
  {
    executeTrackMenu("Texture Parameter", TextureTrack.class, 3);
  }

  private void executeTrackMenu(String path, Class clazz, int count)
  {
    
    JMenuItemOperator oto = appMainMenu.showMenuItem("Animation|" + bundle.getResource("menu.addTrack"));
    assertFalse(oto.isEnabled());

    ObjectInfo test = new ObjectInfo(new Sphere(1d, 1d, 1d), new CoordinateSystem(), "Test-" + System.currentTimeMillis());

    layout.addObject(test, null);
    layout.setSelection(2);

    appMainMenu.pushMenu("Animation|Add Track To Selected Objects|" + path);
    test = layout.getScene().getObject(2);

    assertEquals(count, test.getTracks().length);
    Track[] tracks = test.getTracks();
    assertTrue(clazz.isInstance(tracks[0]));
  }
//</editor-fold>

  @Test
  public void invokePathFromCurveCommand()
  {
    ObjectInfo test = new ObjectInfo(new Sphere(1d, 1d, 1d), new CoordinateSystem(), "Test-" + System.currentTimeMillis());
    Vec3[] points = new Vec3[]
    {
      new Vec3(-1.8, 1.2, 0), new Vec3(1.8, -1.2, 0), new Vec3(1.8, -1.2, 0)
    };
    Curve curve = new Curve(points, new float[]
    {
      1.0f, 1.0f, 1.0f
    }, 3, false);

    ObjectInfo path = new ObjectInfo(curve, new CoordinateSystem(), "Curve-" + System.currentTimeMillis());

    layout.addObject(test, null);
    layout.addObject(path, null);
    layout.setSelection(new int[]
    {
      2, 3
    });

    appMainMenu.pushMenuNoBlock("Animation|Set Path From Curve...");

    JDialogOperator dialog = new JDialogOperator(appFrame, "Set Path From Curve");

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();

  }

  @Test
  public void invokeCreateScriptedObjectCommand()
  {
    appMainMenu.pushMenuNoBlock("Tools|Create Scripted Object...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "New Scripted Object");

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

  @Test
  public void invokeExecuteScriptWindowCommand()
  {
    appMainMenu.pushMenuNoBlock("Tools|Edit Script...");
    JFrameOperator scw = new JFrameOperator(1);

    JButtonOperator cancel = new JButtonOperator(scw, "Close");
    cancel.clickMouse();

  }

  @Test
  public void testInvokeShowTexturesDialogCommand()
  {
    appMainMenu.pushMenuNoBlock("Scene|Textures And Materials...");
    JDialogOperator dialog = new JDialogOperator(appFrame, "Textures and Materials");

    JButtonOperator cancel = new JButtonOperator(dialog, "Close");
    cancel.clickMouse();

  }

  @Test
  public void testInvokeShowEnvironmentDialog()
  {
    appMainMenu.pushMenuNoBlock("Scene|Environment...");
    JDialogOperator dialog = new JDialogOperator(appFrame);

    JLabelOperator label = new JLabelOperator(dialog, "Select Environment Properties:");

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();

  }

  @Test
  public void testInvokeLayoutbjectCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Object Layout...");
    assertFalse(oto.isEnabled());

  }

  @Test
  public void testInvokeLayoutForSingleObjectCommand()
  {
    String objectName = "Test-" + System.currentTimeMillis();
    ObjectInfo test = new ObjectInfo(new Sphere(1d, 1d, 1d), new CoordinateSystem(), objectName);
    layout.addObject(test, null);
    layout.setSelection(2);

    appMainMenu.pushMenuNoBlock("Object|Object Layout...");
    JDialogOperator dialog = new JDialogOperator(appFrame, MessageFormat.format(bundle.getResource("objectLayoutTitle"), objectName));

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

  @Test
  public void testInvokeTransformObjectCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Transform Object...");
    assertFalse(oto.isEnabled());

  }

  @Test
  public void testInvokeTransformForSingleObjectCommand()
  {
    String objectName = "Test-" + System.currentTimeMillis();
    ObjectInfo test = new ObjectInfo(new Sphere(1d, 1d, 1d), new CoordinateSystem(), objectName);
    layout.addObject(test, null);
    layout.setSelection(2);

    appMainMenu.pushMenuNoBlock("Object|Transform Object...");
    JDialogOperator dialog = new JDialogOperator(appFrame, MessageFormat.format(bundle.getResource("transformObjectTitle"), objectName));

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

  @Test
  public void testInvokeTransformForMultipleObjectsCommand()
  {
    String objectName = "Test-" + System.currentTimeMillis();
    ObjectInfo test = new ObjectInfo(new Sphere(1d, 1d, 1d), new CoordinateSystem(), objectName);
    layout.addObject(test, null);
    objectName = "Test-" + System.currentTimeMillis();
    test = new ObjectInfo(new Sphere(1d, 1d, 1d), new CoordinateSystem(), objectName);
    layout.addObject(test, null);

    layout.setSelection(new int[]
    {
      2, 3
    });

    appMainMenu.pushMenuNoBlock("Object|Transform Object...");
    JDialogOperator dialog = new JDialogOperator(appFrame, MessageFormat.format(bundle.getResource("transformObjectTitleMultiple"), objectName));

    JButtonOperator cancel = new JButtonOperator(dialog, bundle.getResource("button.cancel"));
    cancel.clickMouse();
  }

  @Test
  public void testInvokeHideSelectionCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Hide Selection");
    assertFalse(oto.isEnabled());

  }

  @Test
  public void testInvokeShowSelectionCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Show Selection");
    assertFalse(oto.isEnabled());

  }

  @Test
  public void testInvokeShowSelectionAllCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Show All");
    assertTrue(oto.isEnabled());

  }

  @Test
  public void testInvokeLockSelectionCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Lock Selection");
    assertFalse(oto.isEnabled());

  }

  @Test
  public void testInvokeUnlockSelectionCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Unlock Selection");
    assertFalse(oto.isEnabled());

  }

  @Test
  public void testInvokeUnlockSelectionAllCommandNoSelection()
  {
    JMenuItemOperator oto = appMainMenu.showMenuItem("Object|Unlock All");
    assertTrue(oto.isEnabled());

  }

}

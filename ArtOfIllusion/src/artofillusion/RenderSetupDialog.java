/* Copyright (C) 1999-2015 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.image.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/** This class implements the dialog box in which the user can select a renderer, and 
    specify options on how a scene should be rendered. */

public class RenderSetupDialog
{
  private BFrame parent;
  private List<Renderer> renderers;
  private ObjectInfo cameras[];
  private Scene theScene;
  private BComboBox rendChoice, camChoice;
  private BRadioButton singleBox, movieBox;
  private RadioButtonGroup movieGroup;
  private ValueField widthField, heightField, startField, endField, fpsField, subimagesField;
  private Widget configPanel;
  private BorderContainer content;
  
  static Renderer currentRenderer;
  static int currentCamera = 0, width = 640, height = 480;
  static int fps = 30, subimages = 1;
  static double startTime = 0.0, endTime = 1.0;
  static boolean movie;

  public RenderSetupDialog(BFrame parent, Scene theScene)
  {
    this.parent = parent;
    this.theScene = theScene;
    renderers = PluginRegistry.getPlugins(Renderer.class);
    if (currentRenderer == null)
      currentRenderer = ArtOfIllusion.getPreferences().getDefaultRenderer();
    
    // Find all the cameras in the scene.
    
    ObjectInfo obj;
    int i, count;
    
    for (i = 0, count = 0; i < theScene.getNumObjects(); i++)
    {
      obj = theScene.getObject(i);
      if (obj.getObject() instanceof SceneCamera)
        count++;
    }
    if (count == 0)
    {
      new BStandardDialog("", Translate.text("noCameraError"), BStandardDialog.ERROR).showMessageDialog(parent);
      return;
    }
    if (count <= currentCamera)
      currentCamera = 0;
    cameras = new ObjectInfo [count];
    for (i = 0, count = 0; i < theScene.getNumObjects(); i++)
    {
      obj = theScene.getObject(i);
      if (obj.getObject() instanceof SceneCamera)
        cameras[count++] = obj;
    }
    showDialog();
  }
  
  private void showDialog()
  {
    content = new BorderContainer();

    // Create the first panel, which general options.

    FormContainer top = new FormContainer(4, 5);
    top.setDefaultLayout(new LayoutInfo(LayoutInfo.EAST, LayoutInfo.HORIZONTAL, new Insets(0, 0, 0, 5), null));
    LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    top.add(new BLabel(Translate.text("Width")+":"), 0, 0, labelLayout);
    top.add(new BLabel(Translate.text("Height")+":"), 0, 1, labelLayout);
    top.add(new BLabel(Translate.text("Render")+":"), 0, 2, labelLayout);
    top.add(new BLabel(Translate.text("StartTime")+":"), 0, 3, labelLayout);
    top.add(new BLabel(Translate.text("EndTime")+":"), 0, 4, labelLayout);
    top.add(widthField = new ValueField((double) width, ValueField.POSITIVE+ValueField.INTEGER), 1, 0);
    top.add(heightField = new ValueField((double) height, ValueField.POSITIVE+ValueField.INTEGER), 1, 1);
    movieGroup = new RadioButtonGroup();
    movieGroup.addEventLink(SelectionChangedEvent.class, this, "enableMovieComponents");
    top.add(singleBox = new BRadioButton("Single Image", !movie, movieGroup), 1, 2);
    top.add(startField = new ValueField(startTime, ValueField.NONE), 1, 3);
    top.add(endField = new ValueField(endTime, ValueField.NONE), 1, 4);
    top.add(new BLabel(Translate.text("Renderer")+":"), 2, 0, labelLayout);
    top.add(new BLabel(Translate.text("Camera")+":"), 2, 1, labelLayout);
    top.add(movieBox = new BRadioButton("Movie", movie, movieGroup), 2, 2);
    top.add(new BLabel(Translate.text("FramesPerSec")+":"), 2, 3, labelLayout);
    top.add(new BLabel(Translate.text("ImagesPerFrame")+":"), 2, 4, labelLayout);
    top.add(rendChoice = new BComboBox(), 3, 0);
    for (Renderer renderer : renderers)
      rendChoice.add(renderer.getName());
    rendChoice.setSelectedValue(currentRenderer.getName());
    rendChoice.addEventLink(ValueChangedEvent.class, this, "rendererChanged");
    top.add(camChoice = new BComboBox(), 3, 1);
    for (ObjectInfo camera : cameras)
      camChoice.add(camera.getName());
    camChoice.setSelectedIndex(currentCamera);
    top.add(fpsField = new ValueField(fps, ValueField.POSITIVE+ValueField.INTEGER), 3, 3);
    top.add(subimagesField = new ValueField(subimages, ValueField.POSITIVE+ValueField.INTEGER), 3, 4);
    enableMovieComponents();
    content.add(top, BorderContainer.NORTH);
    
    // Add the panel containing renderer-specific options.

    loadRenderSettings(theScene);
    content.add(currentRenderer.getConfigPanel(), BorderContainer.CENTER, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    enableMovieComponents();
    PanelDialog dlg = new PanelDialog(parent, Translate.text("renderTitle"), content);
    content.remove(BorderContainer.CENTER);
    if (dlg.clickedOk())
      doRender();
  }
  
  private void doRender()
  {
    width = (int) widthField.getValue();
    height = (int) heightField.getValue();
    movie = movieBox.getState();
    startTime = startField.getValue();
    endTime = endField.getValue();
    fps = (int) fpsField.getValue();
    subimages = (int) subimagesField.getValue();
    currentCamera = camChoice.getSelectedIndex();
    if (currentRenderer.recordConfiguration())
    {
      theScene.setMetadata(currentRenderer.getClass().getName()+" settings", currentRenderer.getConfiguration());
      Camera cam = new Camera();
      SceneCamera sc = (SceneCamera) cameras[currentCamera].getObject();
      cam.setCameraCoordinates(cameras[currentCamera].getCoords().duplicate());
      cam.setScreenTransform(sc.getScreenTransform(width, height), width, height);
      if (movie)
      {
        int startFrameNumber = (int) Math.round(startTime*fps)+1;
        try
        {
          ImageSaver saver = new ImageSaver(parent, width, height, fps, startFrameNumber);
          if (!saver.clickedOk())
            return;
          new RenderingDialog(parent, currentRenderer, theScene,
            cam, cameras[currentCamera], startTime, endTime, fps, subimages, saver);
        }
        catch (IOException ex)
        {
          new BStandardDialog("", Translate.text("errorSavingFile", ex.getMessage() == null ? "" : ex.getMessage()), BStandardDialog.ERROR).showMessageDialog(parent);
        }
      }
      else
        new RenderingDialog(parent, currentRenderer, theScene, cam, cameras[currentCamera]);
    }
  }
  
  /** Render a still image based on the current settings. */
  
  public static void renderImmediately(BFrame parent, Scene theScene)
  {
    // Find the camera to render from.
    
    ArrayList<ObjectInfo> cameras = new ArrayList<ObjectInfo>();
    for (int i = 0; i < theScene.getNumObjects(); i++)
      if (theScene.getObject(i).getObject() instanceof SceneCamera)
        cameras.add(theScene.getObject(i));
    if (cameras.size() == 0)
    {
      new BStandardDialog("", Translate.text("noCameraError"), BStandardDialog.ERROR).showMessageDialog(parent);
      return;
    }
    if (cameras.size() <= currentCamera)
      currentCamera = 0;
    
    // Render the image.
    
    if (currentRenderer == null)
      currentRenderer = ArtOfIllusion.getPreferences().getDefaultRenderer();
    currentRenderer.getConfigPanel();
    currentRenderer.recordConfiguration();
    loadRenderSettings(theScene);
    Camera cam = new Camera();
    ObjectInfo cameraInfo = cameras.get(currentCamera);
    SceneCamera sc = (SceneCamera) cameraInfo.getObject();
    cam.setCameraCoordinates(cameraInfo.getCoords().duplicate());
    cam.setScreenTransform(sc.getScreenTransform(width, height), width, height);
    new RenderingDialog(parent, currentRenderer, theScene, cam, cameraInfo);
  }
    
  private void rendererChanged()
  {
    content.remove(BorderContainer.CENTER);
    currentRenderer = renderers.get(rendChoice.getSelectedIndex());
    loadRenderSettings(theScene);
    content.add(currentRenderer.getConfigPanel(), BorderContainer.CENTER, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    UIUtilities.findWindow(content).pack();
  }
  
  private void enableMovieComponents()
  {
    boolean enable = movieBox.getState();
    
    startField.setEnabled(enable);
    endField.setEnabled(enable);
    fpsField.setEnabled(enable);
    subimagesField.setEnabled(enable);
  }

  /**
   * See if the scene contains saved settings for the current renderer and load them.
   */
  private static void loadRenderSettings(Scene scene)
  {
    try
    {
      Object settings = scene.getMetadata(currentRenderer.getClass().getName()+" settings");
      if (settings instanceof Map)
      {
        Map<String, Object> savedSettings = (Map<String, Object>) settings;
        for (Map.Entry<String, Object> entry : savedSettings.entrySet())
          currentRenderer.setConfiguration(entry.getKey(), entry.getValue());
      }
    }
    catch (ClassCastException ex)
    {
      // Unexpected objects in the map.  Just ignore.
    }
  }
}
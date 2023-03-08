/* Copyright (C) 1999-2015 by Peter Eastman
   Changes copyright (C) 2016 by Maksim Khramov
   Changes copyright (C) 2020 by Petri Ihalainen

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
  private final BFrame parent;
  private static final List<Renderer> renderers = PluginRegistry.getPlugins(Renderer.class);
  private final List<ObjectInfo> cameras;
  private final Scene theScene;
  private Map<String, Object> rendererConfiguration;
  private Renderer lastConfiguredRenderer;
  private BComboBox rendChoice, camChoice;
  private BRadioButton movieBox;
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
    
    if (currentRenderer == null)
      currentRenderer = ArtOfIllusion.getPreferences().getDefaultRenderer();

    // Find all the cameras in the scene.

    cameras = theScene.getCameras();
    if (cameras.isEmpty())
    {
      new BStandardDialog("", Translate.text("noCameraError"), BStandardDialog.ERROR).showMessageDialog(parent);
      return;
    }
    if(cameras.size() <= currentCamera)
      currentCamera = 0;

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
    top.add(new BRadioButton(Translate.text("SingleImage"), !movie, movieGroup), 1, 2);
    top.add(startField = new ValueField(startTime, ValueField.NONE), 1, 3);
    top.add(endField = new ValueField(endTime, ValueField.NONE), 1, 4);
    top.add(new BLabel(Translate.text("Renderer")+":"), 2, 0, labelLayout);
    top.add(new BLabel(Translate.text("Camera")+":"), 2, 1, labelLayout);
    top.add(movieBox = new BRadioButton(Translate.text("Movie"), movie, movieGroup), 2, 2);
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

    // Load the last dialog settings from metadata

    loadDialogSettings(theScene);
    copyConfigurationToUI();

    // Add the panel containing renderer-specific options.

    loadRenderSettings(theScene);
    content.add(currentRenderer.getConfigPanel(), BorderContainer.CENTER, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    enableMovieComponents();
    lastConfiguredRenderer = currentRenderer;
    rendererConfiguration  = currentRenderer.getConfiguration();
    PanelDialog dlg = new PanelDialog(parent, Translate.text("renderTitle"), content);
    if (dlg.clickedOk())
      doRender();
  }

  private void doRender()
  {
    checkModified();
    recordConfiguration();
    theScene.setMetadata("RenderSetupDialog settings", getConfiguration());
    if (currentRenderer.recordConfiguration())
    {
      checkRendererModified();
      theScene.setMetadata(currentRenderer.getClass().getName()+" settings", currentRenderer.getConfiguration());
      ObjectInfo cameraInfo = cameras.get(currentCamera);
      Camera cam = new Camera();
      SceneCamera sc = (SceneCamera) cameraInfo.getObject();
      cam.setCameraCoordinates(cameraInfo.getCoords().duplicate());
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
            cam, cameraInfo, startTime, endTime, fps, subimages, saver);
        }
        catch (IOException ex)
        {
          new BStandardDialog("", Translate.text("errorSavingFile", ex.getMessage() == null ? "" : ex.getMessage()), BStandardDialog.ERROR).showMessageDialog(parent);
        }
      }
      else
        new RenderingDialog(parent, currentRenderer, theScene, cam, cameraInfo);
    }
  }

  /** Render a still image based on the current settings. */

  public static void renderImmediately(BFrame parent, Scene theScene)
  {
    // Load the last used settings if available

    loadDialogSettings(theScene);
    if (currentRenderer == null)
    {
      currentRenderer = ArtOfIllusion.getPreferences().getDefaultRenderer();
      currentRenderer.getConfigPanel();
      currentRenderer.recordConfiguration();
    }
    loadRenderSettings(theScene);

    // Find the camera to render from or return if none are present.

    List<ObjectInfo> cameras = theScene.getCameras();
    if (cameras.isEmpty())
    {
      new BStandardDialog("", Translate.text("noCameraError"), BStandardDialog.ERROR).showMessageDialog(parent);
      return;
    }
    if (cameras.size() <= currentCamera)
      currentCamera = 0;

    // Render the image.

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
    Check if any of the valaues on the UI differ from the last recorded ones. 
    If so set LayutWindow know.*/

  private void checkModified()
  {
    if (width  != (int) widthField.getValue() ||
        height != (int) heightField.getValue() ||
        movie  != movieBox.getState() ||
        startTime != startField.getValue() ||
        endTime != endField.getValue() ||
        fps != (int) fpsField.getValue() ||
        subimages != (int) subimagesField.getValue() ||
        currentCamera != camChoice.getSelectedIndex() ||
        lastConfiguredRenderer != currentRenderer)
      ((LayoutWindow)parent).setModified();
  }

  private void recordConfiguration()
  {
    width = (int) widthField.getValue();
    height = (int) heightField.getValue();
    movie = movieBox.getState();
    startTime = startField.getValue();
    endTime = endField.getValue();
    fps = (int) fpsField.getValue();
    subimages = (int) subimagesField.getValue();
    currentCamera = camChoice.getSelectedIndex();
  }

  private Map<String, Object> getConfiguration()
  {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("rendererName", currentRenderer.getName());
    map.put("currentCamera", currentCamera);
    map.put("width", width);
    map.put("height", height);
    map.put("fps", fps);
    map.put("subimages", subimages);
    map.put("startTime", startTime);
    map.put("endTime", endTime);
    map.put("movie", movie); 
    return map;
  }

  private void copyConfigurationToUI()
  {
    rendChoice.setSelectedValue(currentRenderer.getName());
    camChoice.setSelectedIndex(currentCamera);
    movieBox.setState(movie);
    widthField.setValue(width);
    heightField.setValue(height);
    startField.setValue(startTime);
    endField.setValue(endTime);
    fpsField.setValue(fps);
    subimagesField.setValue(subimages);
  }

  /** See if the scene contains saved settings for the dialog and load them. */

  private static void loadDialogSettings(Scene scene)
  {
    try
    {
      Object settings = scene.getMetadata("RenderSetupDialog settings");
      if (settings instanceof Map)
      {
        Map<String, Object> savedSettings = (Map<String, Object>) settings;
        for (Map.Entry<String, Object> entry : savedSettings.entrySet())
        {
          String rendererName;
          switch (entry.getKey())
          {
            case "rendererName" : 
              rendererName = (String)entry.getValue();
              for (Renderer r : renderers)
                if (r.getName().equals(rendererName))
                  currentRenderer = r;
              break;
            case "currentCamera": 
              currentCamera = (Integer)entry.getValue(); 
              break;
            case "width": 
              width = (Integer)entry.getValue(); 
              break;
            case "height": 
              height = (Integer)entry.getValue(); 
              break;
            case "fps": 
              fps = (Integer)entry.getValue(); 
              break;
            case "subimages": 
              subimages = (Integer)entry.getValue(); 
              break;
            case "startTime": 
              startTime = ((Number) entry.getValue()).doubleValue(); 
              break;
            case "endTime": 
              endTime = ((Number) entry.getValue()).doubleValue(); 
              break;
            case "movie": 
              movie = (Boolean)entry.getValue(); 
              break;
          }
        }
      }
    }
    catch (ClassCastException ex)
    {
    }
  }

  /** See if the scene contains saved settings for the current renderer and load them. */

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

  /** Compare the last recorded renderer setting to those that were recorded at dialog open.
      If changes are found, mark the scene modified. */

  private void checkRendererModified()
  {
    if (lastConfiguredRenderer != currentRenderer)
    {
      ((LayoutWindow)parent).setModified();
      return;
    }
    boolean modified = false;
    Object recValue, curValue;
    String recKey;
    for (Map.Entry<String, Object> recordedEntry : rendererConfiguration.entrySet())
    {
      recKey   = recordedEntry.getKey();
      recValue = recordedEntry.getValue();
      for (Map.Entry<String, Object> currentEntry : currentRenderer.getConfiguration().entrySet())
      {
        curValue = currentEntry.getValue();
        if (recKey.equals(currentEntry.getKey()))
        {
          if (recValue instanceof Boolean)
            if ((boolean)recValue != (boolean)curValue)
              modified = true;
          if (recValue instanceof Integer)
            if ((int)recValue != (int)curValue)
              modified = true;
          if (recValue instanceof Number && !(recValue instanceof Integer)) // 'else if' did not work right
            if (((Number)recValue).doubleValue() != ((Number)curValue).doubleValue())
              modified = true;
          if (recValue instanceof String) // No Strings in built-in renderers, but who knows...?
            if (!((String)recValue).equals((String)curValue))
              modified = true;
        }
      }
    }
    if (modified)
      ((LayoutWindow)parent).setModified();
    return;
  }
}

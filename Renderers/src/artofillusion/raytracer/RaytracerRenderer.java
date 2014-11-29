/* Copyright (C) 1999-2014 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.*;
import artofillusion.util.*;
import artofillusion.image.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

/** RaytracerRenderer is a Renderer which generates images by raytracing. */

public class RaytracerRenderer implements Renderer, Runnable
{
  protected Raytracer raytracer;
  protected BTabbedPane configPanel;
  protected BCheckBox depthBox, glossBox, shadowBox, causticsBox, transparentBox, adaptiveBox, rouletteBox, reducedMemoryBox;
  protected BComboBox aliasChoice, maxRaysChoice, minRaysChoice, giModeChoice, scatterModeChoice, diffuseRaysChoice, glossRaysChoice, shadowRaysChoice;
  protected ValueField errorField, rayDepthField, rayCutoffField, smoothField, stepSizeField;
  protected ValueField extraGIField, extraGIEnvField;
  protected ValueField globalPhotonsField, globalNeighborPhotonsField, causticsPhotonsField, causticsNeighborPhotonsField, volumePhotonsField, volumeNeighborPhotonsField;
  protected int pixel[], width, height, rtWidth, rtHeight, maxRayDepth = 8, minRays = 4, maxRays = 16, diffuseRays, glossRays, shadowRays, antialiasLevel;
  protected Scene theScene;
  protected Camera theCamera;
  protected SceneCamera sceneCamera;
  protected RenderListener listener;
  protected BufferedImage img;
  protected volatile Thread renderThread;
  protected RGBColor ambColor, envColor, fogColor;
  protected double envParamValue[];
  protected TextureMapping envMapping;
  protected int envMode;
  protected double time, fogDist, surfaceError = 0.02, stepSize = 1.0;
  protected double smoothing = 1.0, smoothScale, extraGISmoothing = 10.0, extraGIEnvSmoothing = 100.0;
  protected int giMode = GI_NONE, scatterMode = SCATTER_SINGLE, globalPhotons = 10000, globalNeighborPhotons = 200, causticsPhotons = 10000, causticsNeighborPhotons = 100, volumePhotons = 10000, volumeNeighborPhotons = 100;
  protected float minRayIntensity = 0.01f, floatImage[][], depthImage[], errorImage[], objectImage[];
  protected boolean fog, depth = false, gloss = false, softShadows = false, caustics = false, transparentBackground = false, adaptive = true, roulette = false, reducedMemory = false;
  protected boolean needCopyToUI = true, isPreview;
  protected PhotonMap globalMap, causticsMap, volumeMap;
  protected BoundingBox materialBounds;
  protected ThreadLocal<RenderWorkspace> threadWorkspace;

  public static final int GI_NONE = 0;
  public static final int GI_AMBIENT_OCCLUSION = 1;
  public static final int GI_MONTE_CARLO = 2;
  public static final int GI_PHOTON = 3;
  public static final int GI_HYBRID = 4;

  public static final int SCATTER_SINGLE = 0;
  public static final int SCATTER_PHOTONS = 1;
  public static final int SCATTER_BOTH = 2;

  public static final float COLOR_THRESH_ABS = 1.0f/128.0f;
  public static final float COLOR_THRESH_REL = 1.0f/32.0f;

  public static final int distrib1[] = {0, 3, 1, 2, 1, 2, 0, 3, 2, 0, 3, 1, 3, 1, 2, 0};
  public static final int distrib2[] = {0, 1, 2, 3, 3, 0, 1, 2, 1, 2, 3, 0, 0, 1, 2, 3};

  /** When a shadow ray is traced to determine whether a light is blocked, MaterialIntersection
   objects are used to keep track of where the ray enters or exits materials. */

  public static class MaterialIntersection
  {
    public MaterialMapping mat;
    public Mat4 toLocal;
    public double dist;
    public boolean entered;
    public OctreeNode node;

    public MaterialIntersection()
    {
    }
  }

  public RaytracerRenderer()
  {
    threadWorkspace = new ThreadLocal<RenderWorkspace>() {
      protected RenderWorkspace initialValue()
      {
        return new RenderWorkspace(RaytracerRenderer.this, raytracer.getContext());
      }
    };
  }

  /** Get the Workspace for the current thread. */

  public RenderWorkspace getWorkspace()
  {
    return threadWorkspace.get();
  }

  /** Methods from the Renderer interface. */

  public String getName()
  {
    return "Raytracer";
  }

  public synchronized void renderScene(Scene theScene, Camera theCamera, RenderListener rl, SceneCamera sceneCamera)
  {
    if (renderThread != null && renderThread.isAlive())
    {
      // A render is currently in progress, so cancel it.

      Thread oldRenderThread = renderThread;
      renderThread = null;
      try
      {
        oldRenderThread.join();
      }
      catch (InterruptedException ex)
      {
        // Ignore.
      }
    }
    raytracer = new Raytracer(theScene, theCamera);
    raytracer.setSurfaceError(surfaceError);
    raytracer.setTime(time);
    raytracer.setAdaptive(adaptive);
    raytracer.setUsePreviewMeshes(isPreview);
    raytracer.setUseReducedMemory(reducedMemory);
    raytracer.setUseSoftShadows(softShadows);
    Dimension dim = theCamera.getSize();

    listener = rl;
    this.theScene = theScene;
    this.theCamera = theCamera.duplicate();
    if (sceneCamera == null)
    {
      sceneCamera = new SceneCamera();
      sceneCamera.setDepthOfField(0.0);
      sceneCamera.setFocalDistance(theCamera.getDistToScreen());
    }
    else
      sceneCamera = sceneCamera.duplicate();
    this.sceneCamera = sceneCamera;
    time = theScene.getTime();
    width = dim.width;
    height = dim.height;
    renderThread = new Thread(this, "Raytracer main thread");
    renderThread.setPriority(Thread.NORM_PRIORITY);
    renderThread.start();
  }

  public synchronized void cancelRendering(Scene sc)
  {
    Thread t = renderThread;

    if (theScene != sc)
      return;
    renderThread = null;
    if (t == null)
      return;
    try
    {
      while (t.isAlive())
      {
        Thread.sleep(100);
      }
    }
    catch (InterruptedException ex)
    {
      // Ignore.
    }
    RenderListener rl = listener;
    listener = null;
    if (rl != null)
      rl.renderingCanceled();
    finish();
  }

  public Widget getConfigPanel()
  {
    if (configPanel == null)
    {
      // General options panel.

      ColumnContainer generalPanel = new ColumnContainer();
      FormContainer choicesPanel = new FormContainer(2, 4);
      generalPanel.add(choicesPanel);
      LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
      LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
      choicesPanel.add(Translate.label("surfaceAccuracy"), 0, 0, leftLayout);
      choicesPanel.add(new BLabel(Translate.text("Antialiasing")+":"), 0, 1, leftLayout);
      choicesPanel.add(Translate.label("minRaysPixel"), 0, 2, leftLayout);
      choicesPanel.add(Translate.label("maxRaysPixel"), 0, 3, leftLayout);
      choicesPanel.add(errorField = new ValueField(surfaceError, ValueField.POSITIVE, 6), 1, 0, rightLayout);
      choicesPanel.add(aliasChoice = new BComboBox(new String [] {
          Translate.text("none"),
          Translate.text("Medium"),
          Translate.text("Maximum")
      }), 1, 1, rightLayout);
      choicesPanel.add(minRaysChoice = new BComboBox(), 1, 2, rightLayout);
      choicesPanel.add(maxRaysChoice = new BComboBox(), 1, 3, rightLayout);
      for (int i = 4; i <= 1024; i *= 2)
      {
        minRaysChoice.add(Integer.toString(i));
        maxRaysChoice.add(Integer.toString(i));
      }
      ColumnContainer boxes = new ColumnContainer();
      generalPanel.add(boxes);
      boxes.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
      boxes.add(depthBox = new BCheckBox(Translate.text("depthOfField"), depth));
      boxes.add(glossBox = new BCheckBox(Translate.text("glossTranslucency"), gloss));
      RowContainer row;
      LayoutInfo indent = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 30, 0, 0), null);
      boxes.add(row = new RowContainer());
      row.add(Translate.label("raysToSample"), indent);
      row.add(glossRaysChoice = new BComboBox());
      boxes.add(shadowBox = new BCheckBox(Translate.text("softShadows"), softShadows));
      boxes.add(row = new RowContainer());
      row.add(Translate.label("raysToSample"), indent);
      row.add(shadowRaysChoice = new BComboBox());
      boxes.add(transparentBox = new BCheckBox(Translate.text("transparentBackground"), transparentBackground));
      glossRaysChoice.add("1");
      shadowRaysChoice.add("1");
      for (int i = 4; i <= 64; i *= 2)
      {
        glossRaysChoice.add(Integer.toString(i));
        shadowRaysChoice.add(Integer.toString(i));
      }
      glossBox.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
          UIUtilities.setEnabled(glossRaysChoice.getParent(), aliasChoice.getSelectedIndex() > 0 && glossBox.getState());
        }
      });
      shadowBox.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
          UIUtilities.setEnabled(shadowRaysChoice.getParent(), aliasChoice.getSelectedIndex() > 0 && shadowBox.getState());
        }
      });

      // Illumination options panel.

      giModeChoice = new BComboBox(new String [] {
          Translate.text("none"),
          Translate.text("ambientOcclusion"),
          Translate.text("monteCarlo"),
          Translate.text("photonMappingDirect"),
          Translate.text("photonMappingFinalGather")
      });
      scatterModeChoice = new BComboBox(new String [] {
          Translate.text("singleScattering"),
          Translate.text("photonMapping"),
          Translate.text("Both")
      });
      diffuseRaysChoice = new BComboBox();
      diffuseRaysChoice.add("1");
      for (int i = 4; i <= 64; i *= 2)
        diffuseRaysChoice.add(Integer.toString(i));
      globalPhotonsField = new ValueField(globalPhotons, ValueField.POSITIVE+ValueField.INTEGER, 7);
      globalNeighborPhotonsField = new ValueField(globalNeighborPhotons, ValueField.POSITIVE+ValueField.INTEGER, 4);
      causticsPhotonsField = new ValueField(causticsPhotons, ValueField.POSITIVE+ValueField.INTEGER, 7);
      causticsNeighborPhotonsField = new ValueField(causticsNeighborPhotons, ValueField.POSITIVE+ValueField.INTEGER, 4);
      volumePhotonsField = new ValueField(volumePhotons, ValueField.POSITIVE+ValueField.INTEGER, 7);
      volumeNeighborPhotonsField = new ValueField(volumeNeighborPhotons, ValueField.POSITIVE+ValueField.INTEGER, 4);
      causticsBox = new BCheckBox(Translate.text("useCausticsMap"), caustics);
      ColumnContainer illuminationPanel = new ColumnContainer();
      LayoutInfo indent0 = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
      LayoutInfo indent1 = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 20, 0, 0), null);
      illuminationPanel.add(row = new RowContainer(), indent0);
      row.add(Translate.label("globalIllumination"));
      row.add(giModeChoice);
      illuminationPanel.add(row = new RowContainer(), indent1);
      row.add(Translate.label("raysToSampleEnvironment"));
      row.add(diffuseRaysChoice);
      illuminationPanel.add(row = new RowContainer(), indent1);
      row.add(Translate.label("totalPhotons"));
      row.add(globalPhotonsField);
      row.add(Translate.label("numToEstimateLight"));
      row.add(globalNeighborPhotonsField);
      illuminationPanel.add(causticsBox, indent0);
      illuminationPanel.add(row = new RowContainer(), indent1);
      row.add(Translate.label("totalPhotons"));
      row.add(causticsPhotonsField);
      row.add(Translate.label("numToEstimateLight"));
      row.add(causticsNeighborPhotonsField);
      illuminationPanel.add(row = new RowContainer(), indent0);
      row.add(Translate.label("materialScattering"));
      row.add(scatterModeChoice);
      illuminationPanel.add(row = new RowContainer(), indent1);
      row.add(Translate.label("totalPhotons"));
      row.add(volumePhotonsField);
      row.add(Translate.label("numToEstimateLight"));
      row.add(volumeNeighborPhotonsField);
      causticsBox.dispatchEvent(new ValueChangedEvent(causticsBox));

      // Advanced options panel.

      rayDepthField = new ValueField(maxRayDepth, ValueField.POSITIVE+ValueField.INTEGER);
      rayCutoffField = new ValueField(minRayIntensity, ValueField.NONNEGATIVE);
      smoothField = new ValueField(smoothing, ValueField.NONNEGATIVE);
      extraGIField = new ValueField(extraGISmoothing, ValueField.POSITIVE);
      extraGIEnvField = new ValueField(extraGIEnvSmoothing, ValueField.POSITIVE);
      stepSizeField = new ValueField(stepSize, ValueField.POSITIVE);
      adaptiveBox = new BCheckBox(Translate.text("reduceAccuracyForDistant"), adaptive);
      rouletteBox = new BCheckBox(Translate.text("russianRoulette"), roulette);
      reducedMemoryBox = new BCheckBox(Translate.text("useLessMemory"), reducedMemory);
      FormContainer advancedPanel = new FormContainer(2, 8);
      advancedPanel.add(Translate.label("maxRayTreeDepth"), 0, 0, leftLayout);
      advancedPanel.add(Translate.label("minRayIntensity"), 0, 1, leftLayout);
      advancedPanel.add(Translate.label("matStepSize"), 0, 3, leftLayout);
      advancedPanel.add(Translate.label("texSmoothing"), 0, 4, leftLayout);
      advancedPanel.add(rayDepthField, 1, 0, rightLayout);
      advancedPanel.add(rayCutoffField, 1, 1, rightLayout);
      advancedPanel.add(stepSizeField, 1, 3, rightLayout);
      advancedPanel.add(smoothField, 1, 4, rightLayout);
      advancedPanel.add(Translate.label("extraGISmoothing"), 0, 5, 2, 1);
      advancedPanel.add(row = new RowContainer(), 0, 6, 2, 1);
      row.add(new BLabel(Translate.text("Textures")+":"));
      row.add(extraGIField);
      row.add(new BLabel(Translate.text("environment")+":"));
      row.add(extraGIEnvField);
      boxes = new ColumnContainer();
      advancedPanel.add(boxes, 0, 7, 2, 1);
      boxes.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
      boxes.add(adaptiveBox);
      boxes.add(reducedMemoryBox);
      boxes.add(rouletteBox);

      // Create the tabbed pane.

      configPanel = new BTabbedPane();
      configPanel.add(generalPanel, Translate.text("general"));
      configPanel.add(illuminationPanel, Translate.text("illumination"));
      configPanel.add(advancedPanel, Translate.text("advanced"));

      // Set up listeners for components.

      Object raysListener = new Object() {
        void processEvent(WidgetEvent ev)
        {
          boolean multi = (aliasChoice.getSelectedIndex() > 0);

          depthBox.setEnabled(multi);
          glossBox.setEnabled(multi);
          shadowBox.setEnabled(multi);
          minRaysChoice.setEnabled(multi);
          maxRaysChoice.setEnabled(multi);
          UIUtilities.setEnabled(glossRaysChoice.getParent(), multi && glossBox.getState());
          UIUtilities.setEnabled(shadowRaysChoice.getParent(), multi && shadowBox.getState());
          if (minRaysChoice.getSelectedIndex() > maxRaysChoice.getSelectedIndex())
          {
            if (ev.getWidget() == maxRaysChoice)
              minRaysChoice.setSelectedIndex(maxRaysChoice.getSelectedIndex());
            else
              maxRaysChoice.setSelectedIndex(minRaysChoice.getSelectedIndex());
          }
        }
      };
      aliasChoice.addEventLink(ValueChangedEvent.class, raysListener);
      minRaysChoice.addEventLink(ValueChangedEvent.class, raysListener);
      maxRaysChoice.addEventLink(ValueChangedEvent.class, raysListener);
      aliasChoice.dispatchEvent(new ValueChangedEvent(aliasChoice));
      Object illumListener = new Object() {
        void processEvent()
        {
          int mode = giModeChoice.getSelectedIndex();
          UIUtilities.setEnabled(diffuseRaysChoice.getParent(), mode == GI_MONTE_CARLO || mode == GI_HYBRID || mode == GI_AMBIENT_OCCLUSION);
          UIUtilities.setEnabled(globalPhotonsField.getParent(), mode == GI_PHOTON || mode == GI_HYBRID);
          UIUtilities.setEnabled(causticsPhotonsField.getParent(), causticsBox.getState());
          UIUtilities.setEnabled(volumePhotonsField.getParent(), scatterModeChoice.getSelectedIndex() > 0);
        }
      };
      giModeChoice.addEventLink(ValueChangedEvent.class, illumListener);
      causticsBox.addEventLink(ValueChangedEvent.class, illumListener);
      scatterModeChoice.addEventLink(ValueChangedEvent.class, illumListener);
    }
    if (needCopyToUI)
      copyConfigurationToUI();
    return configPanel;
  }

  /** Copy the current configuration to the user interface. */

  protected void copyConfigurationToUI()
  {
    needCopyToUI = false;
    if (configPanel == null)
      getConfigPanel();
    rayDepthField.setValue(maxRayDepth);
    rayCutoffField.setValue(minRayIntensity);
    stepSizeField.setValue(stepSize);
    smoothField.setValue(smoothing);
    extraGIField.setValue(extraGISmoothing);
    extraGIEnvField.setValue(extraGIEnvSmoothing);
    adaptiveBox.setState(adaptive);
    rouletteBox.setState(roulette);
    errorField.setValue(surfaceError);
    aliasChoice.setSelectedIndex(antialiasLevel);
    depthBox.setState(depth);
    glossBox.setState(gloss);
    glossRaysChoice.setSelectedValue(Integer.toString(glossRays));
    shadowBox.setState(softShadows);
    shadowRaysChoice.setSelectedValue(Integer.toString(shadowRays));
    minRaysChoice.setSelectedValue(Integer.toString(minRays));
    maxRaysChoice.setSelectedValue(Integer.toString(maxRays));
    reducedMemoryBox.setState(reducedMemory);
    giModeChoice.setSelectedIndex(giMode);
    diffuseRaysChoice.setSelectedValue(Integer.toString(diffuseRays));
    globalPhotonsField.setValue(globalPhotons);
    globalNeighborPhotonsField.setValue(globalNeighborPhotons);
    causticsBox.setState(caustics);
    causticsPhotonsField.setValue(causticsPhotons);
    causticsNeighborPhotonsField.setValue(causticsNeighborPhotons);
    scatterModeChoice.setSelectedIndex(scatterMode);
    volumePhotonsField.setValue(volumePhotons);
    volumeNeighborPhotonsField.setValue(volumeNeighborPhotons);
    transparentBox.setState(transparentBackground);

    // Generate events to force appropriate components to be enabled or disabled.

    aliasChoice.dispatchEvent(new ValueChangedEvent(aliasChoice));
  }

  public boolean recordConfiguration()
  {
    maxRayDepth = (int) rayDepthField.getValue();
    minRayIntensity = (float) rayCutoffField.getValue();
    stepSize = stepSizeField.getValue();
    smoothing = smoothField.getValue();
    extraGISmoothing = extraGIField.getValue();
    extraGIEnvSmoothing = extraGIEnvField.getValue();
    adaptive = adaptiveBox.getState();
    roulette = rouletteBox.getState();
    surfaceError = errorField.getValue();
    antialiasLevel = aliasChoice.getSelectedIndex();
    depth = depthBox.getState();
    gloss = glossBox.getState();
    glossRays = Integer.parseInt((String) glossRaysChoice.getSelectedValue());
    softShadows = shadowBox.getState();
    shadowRays = Integer.parseInt((String) shadowRaysChoice.getSelectedValue());
    minRays = Integer.parseInt((String) minRaysChoice.getSelectedValue());
    maxRays = Integer.parseInt((String) maxRaysChoice.getSelectedValue());
    transparentBackground = transparentBox.getState();
    giMode = giModeChoice.getSelectedIndex();
    diffuseRays = Integer.parseInt((String) diffuseRaysChoice.getSelectedValue());
    globalPhotons = (int) globalPhotonsField.getValue();
    globalNeighborPhotons = (int) globalNeighborPhotonsField.getValue();
    caustics = causticsBox.getState();
    causticsPhotons = (int) causticsPhotonsField.getValue();
    causticsNeighborPhotons = (int) causticsNeighborPhotonsField.getValue();
    scatterMode = scatterModeChoice.getSelectedIndex();
    volumePhotons = (int) volumePhotonsField.getValue();
    volumeNeighborPhotons = (int) volumeNeighborPhotonsField.getValue();
    reducedMemory = reducedMemoryBox.getState();
    isPreview = false;
    return true;
  }

  public Map<String, Object> getConfiguration()
  {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("maxRayDepth", maxRayDepth);
    map.put("minRayIntensity", minRayIntensity);
    map.put("materialStepSize", stepSize);
    map.put("textureSmoothing", smoothing);
    map.put("extraGISmoothing", extraGISmoothing);
    map.put("extraGIEnvSmoothing", extraGIEnvSmoothing);
    map.put("reduceAccuracyForDistant", adaptive);
    map.put("russianRouletteSampling", roulette);
    map.put("useLessMemory", reducedMemory);
    map.put("maxSurfaceError", surfaceError);
    map.put("antialiasing", antialiasLevel);
    map.put("depthOfField", depth);
    map.put("gloss", gloss);
    map.put("raysToSampleGloss", glossRays);
    map.put("softShadows", softShadows);
    map.put("raysToSampleShadows", shadowRays);
    map.put("minRaysPerPixel", minRays);
    map.put("maxRaysPerPixel", maxRays);
    map.put("transparentBackground", transparentBackground);
    map.put("globalIlluminationMode", giMode);
    map.put("raysToSampleEnvironment", diffuseRays);
    map.put("globalIlluminationPhotons", globalPhotons);
    map.put("globalIlluminationPhotonsInEstimate", globalNeighborPhotons);
    map.put("caustics", caustics);
    map.put("causticsPhotons", causticsPhotons);
    map.put("causticsPhotonsInEstimate", causticsNeighborPhotons);
    map.put("scatteringMode", scatterMode);
    map.put("scatteringPhotons", volumePhotons);
    map.put("scatteringPhotonsInEstimate", volumeNeighborPhotons);
    return map;
  }

  public void setConfiguration(String property, Object value)
  {
    needCopyToUI = true;
    isPreview = false;
    if ("maxRayDepth".equals(property))
      maxRayDepth = (Integer) value;
    else if ("minRayIntensity".equals(property))
      minRayIntensity = ((Number) value).floatValue();
    else if ("materialStepSize".equals(property))
      stepSize = ((Number) value).doubleValue();
    else if ("textureSmoothing".equals(property))
      smoothing = ((Number) value).doubleValue();
    else if ("extraGISmoothing".equals(property))
      extraGISmoothing = ((Number) value).doubleValue();
    else if ("extraGIEnvSmoothing".equals(property))
      extraGIEnvSmoothing = ((Number) value).doubleValue();
    else if ("reduceAccuracyForDistant".equals(property))
      adaptive = (Boolean) value;
    else if ("russianRouletteSampling".equals(property))
      roulette = (Boolean) value;
    else if ("useLessMemory".equals(property))
      reducedMemory = (Boolean) value;
    else if ("maxSurfaceError".equals(property))
      surfaceError = ((Number) value).doubleValue();
    else if ("antialiasing".equals(property))
      antialiasLevel = (Integer) value;
    else if ("depthOfField".equals(property))
      depth = (Boolean) value;
    else if ("gloss".equals(property))
      gloss = (Boolean) value;
    else if ("raysToSampleGloss".equals(property))
      glossRays = (Integer) value;
    else if ("softShadows".equals(property))
      softShadows = (Boolean) value;
    else if ("raysToSampleShadows".equals(property))
      shadowRays = (Integer) value;
    else if ("minRaysPerPixel".equals(property))
      minRays = (Integer) value;
    else if ("maxRaysPerPixel".equals(property))
      maxRays = (Integer) value;
    else if ("transparentBackground".equals(property))
      transparentBackground = (Boolean) value;
    else if ("globalIlluminationMode".equals(property))
      giMode = (Integer) value;
    else if ("raysToSampleEnvironment".equals(property))
      diffuseRays = (Integer) value;
    else if ("globalIlluminationPhotons".equals(property))
      globalPhotons = (Integer) value;
    else if ("globalIlluminationPhotonsInEstimate".equals(property))
      globalNeighborPhotons = (Integer) value;
    else if ("caustics".equals(property))
      caustics = (Boolean) value;
    else if ("causticsPhotons".equals(property))
      causticsPhotons = (Integer) value;
    else if ("causticsPhotonsInEstimate".equals(property))
      causticsNeighborPhotons = (Integer) value;
    else if ("scatteringMode".equals(property))
      scatterMode = (Integer) value;
    else if ("scatteringPhotons".equals(property))
      volumePhotons = (Integer) value;
    else if ("scatteringPhotonsInEstimate".equals(property))
      volumeNeighborPhotons = (Integer) value;
  }

  public void configurePreview()
  {
    if (needCopyToUI)
      copyConfigurationToUI();
    maxRayDepth = 6;
    minRayIntensity = 0.02f;
    antialiasLevel = 0;
    depth = gloss = softShadows = transparentBackground = false;
    minRays = 4;
    maxRays = 4;
    antialiasLevel = 2;
    stepSize = 1.0;
    smoothing = 1.0;
    extraGISmoothing = 10.0;
    extraGIEnvSmoothing = 100.0;
    adaptive = true;
    reducedMemory = false;
    roulette = false;
    surfaceError = ArtOfIllusion.getPreferences().getInteractiveSurfaceError();
    giMode = GI_NONE;
    scatterMode = SCATTER_SINGLE;
    caustics = false;
    isPreview = true;
  }

  /** Construct the list of RTObjects and lights in the scene. */

  protected void buildScene()
  {
    final Thread mainThread = Thread.currentThread();

    ThreadManager threads = new ThreadManager(theScene.getNumObjects(), new ThreadManager.Task()
    {
      public void execute(int index)
      {
        if (renderThread != mainThread)
          return;
        ObjectInfo info = theScene.getObject(index);
        if (info.isVisible())
          raytracer.addObject(info);
      }
      public void cleanup()
      {
      }
    });
    threads.run();
    threads.finish();
    raytracer.finishConstruction();
    for (RTObject obj : raytracer.getObjects())
    {
      if (obj.getMaterialMapping() != null)
      {
        if (materialBounds == null)
          materialBounds = new BoundingBox(obj.getBounds());
        else
          materialBounds.extend(obj.getBounds());
      }
    }
    ambColor = theScene.getAmbientColor();
    envColor = theScene.getEnvironmentColor();
    envMapping = theScene.getEnvironmentMapping();
    envMode = theScene.getEnvironmentMode();
    fogColor = theScene.getFogColor();
    fog = theScene.getFogState();
    fogDist = theScene.getFogDistance();
    ParameterValue envParam[] = theScene.getEnvironmentParameterValues();
    envParamValue = new double [envParam.length];
    for (int i = 0; i < envParamValue.length; i++)
      envParamValue[i] = envParam[i].getAverageValue();
  }

  /** Build the photon maps. */

  protected void buildPhotonMap()
  {
    if (giMode != GI_PHOTON && giMode != GI_HYBRID && !caustics && scatterMode != SCATTER_PHOTONS && scatterMode != SCATTER_BOTH)
      return;
    PhotonMap shared = null;
    if (giMode == GI_PHOTON)
    {
      listener.statusChanged("Building Global Photon Map");
      globalMap = shared = new PhotonMap(globalPhotons, globalNeighborPhotons, false, false, true, false, raytracer, this, raytracer.getRootNode(), 1, null);
      generatePhotons(globalMap);
    }
    else if (giMode == GI_HYBRID)
    {
      listener.statusChanged("Building Global Photon Map");
      globalMap = shared = new PhotonMap(globalPhotons, globalNeighborPhotons, true, true, true, false, raytracer, this, raytracer.getRootNode(), 0, null);
      generatePhotons(globalMap);
    }
    if (caustics)
    {
      // Find a bounding box around all objects that can generate caustics.

      BoundingBox bounds = null;
      for (RTObject obj : raytracer.getObjects())
      {
        Texture tex = obj.getTextureMapping().getTexture();
        MaterialMapping mm = obj.getMaterialMapping();
        if (tex.hasComponent(Texture.SPECULAR_COLOR_COMPONENT) || (tex.hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT) && mm != null && mm.getMaterial().indexOfRefraction() != 1.0))
        {
          if (bounds == null)
            bounds = obj.getBounds();
          else
            bounds = bounds.merge(obj.getBounds());
        }
      }
      if (bounds == null)
        bounds = new BoundingBox(0, 0, 0, 0, 0, 0);
      listener.statusChanged("Building Caustics Photon Map");
      causticsMap = shared = new PhotonMap(causticsPhotons, causticsNeighborPhotons, true, false, false, false, raytracer, this, bounds, 2, shared);
      generatePhotons(causticsMap);
    }
    if (scatterMode == SCATTER_PHOTONS || scatterMode == SCATTER_BOTH)
    {
      // Find a bounding box around all objects with scattering materials.

      BoundingBox bounds = null;
      for (RTObject obj : raytracer.getObjects())
      {
        Texture tex = obj.getTextureMapping().getTexture();
        MaterialMapping mm = obj.getMaterialMapping();
        if (tex.hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT) && mm != null && mm.getMaterial().isScattering())
        {
          if (bounds == null)
            bounds = obj.getBounds();
          else
            bounds = bounds.merge(obj.getBounds());
        }
      }
      if (bounds == null)
        bounds = new BoundingBox(0, 0, 0, 0, 0, 0);
      listener.statusChanged("Building Volume Photon Map");
      volumeMap = new PhotonMap(volumePhotons, volumeNeighborPhotons, false, scatterMode == SCATTER_PHOTONS, true, true, raytracer, this, bounds, 0, shared);
      generatePhotons(volumeMap);
    }
  }

  /** Find all the photon sources in the scene, and generate the photons in a PhotonMap. */

  protected void generatePhotons(PhotonMap map)
  {
    List<PhotonSourceFactory> factories = PluginRegistry.getPlugins(PhotonSourceFactory.class);
    ArrayList<PhotonSource> sources = new ArrayList<PhotonSource>();
    for (RTLight lt : raytracer.getLights())
    {
      // First give plugins a chance to handle it.

      boolean processed = false;
      for (PhotonSourceFactory factory : factories)
        if (factory.processLight(lt, map, sources))
        {
          processed = true;
          break;
        }
      if (processed)
        continue;

      // Process it in the default way.

      if (lt.getLight() instanceof DirectionalLight)
        sources.add(new DirectionalPhotonSource((DirectionalLight) lt.getLight(), lt.getCoords(), map));
      else if (lt.getLight() instanceof PointLight)
        sources.add(new PointPhotonSource((PointLight) lt.getLight(), lt.getCoords(), map));
      else if (lt.getLight() instanceof SpotLight)
        sources.add(new SpotlightPhotonSource((SpotLight) lt.getLight(), lt.getCoords(), map));
    }
    ArrayList<PhotonSource> objectSources = new ArrayList<PhotonSource>();
    for (RTObject obj : raytracer.getObjects())
    {
      // First give plugins a chance to handle it.

      boolean processed = false;
      for (PhotonSourceFactory factory : factories)
        if (factory.processObject(obj, map, sources))
        {
          processed = true;
          break;
        }
      if (processed)
        continue;

      // Process it in the default way.

      if (!obj.getTextureMapping().getTexture().hasComponent(Texture.EMISSIVE_COLOR_COMPONENT))
        continue;
      PhotonSource src;
      if (obj instanceof RTTriangle)
        src = new TrianglePhotonSource(((RTTriangle) obj).tri, map);
      else if (obj instanceof RTTriangleLowMemory)
        src = new TrianglePhotonSource(((RTTriangleLowMemory) obj).tri, map);
      else if (obj instanceof RTDisplacedTriangle)
        src = new DisplacedTrianglePhotonSource((RTDisplacedTriangle) obj, map);
      else if (obj instanceof RTEllipsoid)
        src = new EllipsoidPhotonSource((RTEllipsoid) obj, map);
      else if (obj instanceof RTSphere)
        src = new EllipsoidPhotonSource((RTSphere) obj, map);
      else if (obj instanceof RTCylinder)
        src = new CylinderPhotonSource((RTCylinder) obj, map);
      else if (obj instanceof RTCube)
        src = new CubePhotonSource((RTCube) obj, map);
      else
        continue;
      if (src.getTotalIntensity() > 0.0)
        objectSources.add(src);
    }
    if (objectSources.size() > 0)
      sources.add(new CompoundPhotonSource(objectSources.toArray(new PhotonSource[objectSources.size()])));
    sources.add(new EnvironmentPhotonSource(theScene, map));
    PhotonSource src[] = sources.toArray(new PhotonSource [sources.size()]);
    map.generatePhotons(src);
  }

  /** Main method in which the image is rendered. */

  public void run()
  {
    long updateTime = System.currentTimeMillis();
    final Thread thisThread = Thread.currentThread();
    if (renderThread != thisThread)
      return;
    if (width == 0 || height == 0)
    {
      finish();
      return;
    }

    img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
    pixel = ((DataBufferInt) ((BufferedImage) img).getRaster().getDataBuffer()).getData();
    int requiredComponents = sceneCamera.getComponentsForFilters();
    floatImage = new float [4][width*height];
    if ((requiredComponents&ComplexImage.DEPTH) != 0)
      depthImage = new float [width*height];
    if ((requiredComponents&ComplexImage.NOISE) != 0)
      errorImage = new float [width*height];
    if ((requiredComponents&ComplexImage.OBJECT) != 0)
      objectImage = new float [width*height];
    listener.statusChanged(Translate.text("Processing Scene"));
    buildScene();
    if (renderThread != thisThread)
      return;
    buildPhotonMap();
    listener.statusChanged(Translate.text("Rendering"));
    for (int i = 0; i < pixel.length; i++)
      pixel[i] = 0;
    int maxRaysInUse = maxRays;
    int minRaysInUse = minRays;
    if (antialiasLevel == 0)
      minRaysInUse = maxRaysInUse = 1;
    smoothScale = smoothing*2.0*Math.tan(sceneCamera.getFieldOfView()*Math.PI/360.0)/height;

    // Rendering is done in two phases.  In the first phase, we send one ray per pixel, ordered so that a
    // rough image appears quickly then progressively becomes more detailed.

    if (maxRaysInUse == 1)
    {
      rtWidth = width;
      rtHeight = height;
    }
    else
    {
      rtWidth = 2*width+2;
      rtHeight = 2*height+2;
      smoothScale *= 0.5;
    }

    final int finalMinRays = minRaysInUse;
    final int currentScale[] = new int [1];
    final int currentWidth[] = new int [1];
    final boolean isFirstPass[] = new boolean[] {true};
    ThreadManager threads = new ThreadManager(width, new ThreadManager.Task() {
      public void execute(int index)
      {
        if (renderThread != thisThread)
          return;
        int row = index/currentWidth[0];
        int col = index-row*currentWidth[0];
        if (!isFirstPass[0] && row%2 == 0 && col%2 == 0)
          return;
        int subsample = (finalMinRays > 1 ? 2 : 1);
        RenderWorkspace workspace = getWorkspace();
        PixelInfo pixel = workspace.tempPixel;
        pixel.clear();
        pixel.depth = (float) spawnEyeRay(workspace, col*subsample*currentScale[0], row*subsample*currentScale[0], 4, finalMinRays);
        pixel.object = (workspace.firstObjectHit == null ? 0.0f : Float.intBitsToFloat(workspace.firstObjectHit.getObject().hashCode()));
        pixel.add(workspace.color[0], (float) workspace.transparency[0]);
        recordPixel(col*currentScale[0], row*currentScale[0], currentScale[0], pixel);
      }
      public void cleanup()
      {
        getWorkspace().cleanup();
      }
    });
    for (currentScale[0] = 1<<(int)(Math.log(width/32)/Math.log(2.0)); currentScale[0] >= 1; currentScale[0] /= 2)
    {
      currentWidth[0] = (int) Math.ceil((double) width/currentScale[0]);
      int currentHeight = (int) Math.ceil((double) height/currentScale[0]);
      threads.setNumIndices(currentWidth[0]*currentHeight);
      threads.run();
      isFirstPass[0] = false;
      if (renderThread != thisThread)
      {
        threads.finish();
        return;
      }
      long currentTime = System.currentTimeMillis();
      if (currentTime-updateTime > 250 || currentScale[0] == 1 && currentTime-updateTime > 150)
      {
        listener.imageUpdated(img);
        updateTime = System.currentTimeMillis();
      }
    }
    threads.finish();

    // At this point, we have sent one ray/pixel.  If that's all they requested, we can just
    // return now.  Otherwise, go on to phase 2 where we send out more rays to improve the
    // image quality.

    if (maxRaysInUse == 1)
    {
      finish();
      return;
    }

    // We need to adaptively decide how many rays to use for each pixel.  To save memory,
    // we only deal with six rows at a time.  Begin by sending minRays for each pixel.  If
    // the results are not sufficiently converged for a given pixel, double the number of
    // rays for that pixel, and every adjacent pixel.  Repeat until everything converges,
    // or we reach maxRays.

    PixelInfo tempPixel = new PixelInfo();
    RGBColor tempColor = new RGBColor();
    final PixelInfo pix[][] = new PixelInfo [6][rtWidth];
    for (int i = 0; i < pix.length; i++)
      for (int j = 0; j < pix[i].length; j++)
        pix[i][j] = new PixelInfo();
    loadRow(pix[0], 0, tempColor);
    loadRow(pix[2], 1, tempColor);
    loadRow(pix[4], 2, tempColor);
    int minPerSubpixel = minRaysInUse/4, maxPerSubpixel = maxRaysInUse/4;
    final int currentRow[] = new int [1];
    final int currentCount[] = new int [1];
    threads = new ThreadManager(rtWidth, new ThreadManager.Task() {
      public void execute(int index)
      {
        RenderWorkspace workspace = getWorkspace();
        PixelInfo tempPixel = workspace.tempPixel;
        for (int m = 0; m < 6; m++)
        {
          PixelInfo thisPixel = pix[m][index];
          thisPixel.converged = true;
          if (thisPixel.needsMore)
          {
            tempPixel.clear();
            int baseNum = (m&1)*8+(index&1)*4;
            int numNeeded = currentCount[0]-thisPixel.raysSent;
            for (int k = thisPixel.raysSent; k < currentCount[0]; k++)
            {
              float dist = (float) spawnEyeRay(workspace, index, 2*currentRow[0]+m, baseNum+k, numNeeded);
              if (k < currentCount[0]/2)
              {
                thisPixel.add(workspace.color[0], (float) workspace.transparency[0]);
                if (dist < thisPixel.depth)
                {
                  thisPixel.depth = dist;
                  thisPixel.object = (workspace.firstObjectHit == null ? 0.0f : Float.intBitsToFloat(workspace.firstObjectHit.getObject().hashCode()));
                }
              }
              else
              {
                tempPixel.add(workspace.color[0], (float) workspace.transparency[0]);
                if (dist < tempPixel.depth)
                {
                  tempPixel.depth = dist;
                  tempPixel.object = (workspace.firstObjectHit == null ? 0.0f : Float.intBitsToFloat(workspace.firstObjectHit.getObject().hashCode()));
                }
              }
            }
            if (currentCount[0] > 1)
              thisPixel.converged = thisPixel.matches(tempPixel, COLOR_THRESH_ABS, COLOR_THRESH_REL);
            thisPixel.add(tempPixel);
          }
        }
      }
      public void cleanup()
      {
        getWorkspace().cleanup();
      }
    });

    for (currentRow[0] = 0; currentRow[0] < height-1; currentRow[0]++)
    {
      // Keep refining the pixels in the current set of six rows until they converge, or
      // we reach maxRays.

      boolean done = false;
      for (currentCount[0] = minPerSubpixel; currentCount[0] <= maxPerSubpixel && !done; currentCount[0] *= 2)
      {
        // Send out more rays through any pixels which are marked as needing it.

        threads.run();
        if (renderThread != thisThread)
        {
          threads.finish();
          return;
        }

        // If we have only sent out one ray per subpixel, we cannot yet judge the convergence of
        // each one.  Instead, compare each pixel to its neighbors and use that to decide where
        // we need more.

        if (currentCount[0] == 1)
          for (int m = 0; m < 5; m++)
            for (int j = 0; j < rtWidth-1; j++)
            {
              if (!pix[m][j].matches(pix[m+1][j], COLOR_THRESH_ABS, COLOR_THRESH_REL))
                pix[m][j].converged = pix[m+1][j].converged = false;
              if (!pix[m][j].matches(pix[m][j+1], COLOR_THRESH_ABS, COLOR_THRESH_REL))
                pix[m][j].converged = pix[m][j+1].converged = false;
            }

        // If a pixel has not yet converged, mark that pixel and all of its neighbors
        // to get more rays.

        for (int m = 0; m < 6; m++)
          for (int j = 0; j < rtWidth; j++)
            pix[m][j].needsMore = false;
        done = true;
        for (int m = 0; m < 6; m++)
          for (int j = 0; j < rtWidth; j++)
            if (!pix[m][j].converged)
            {
              done = false;
              pix[m][j].needsMore = true;
              if (m > 0)
                pix[m-1][j].needsMore = true;
              if (m < 5)
                pix[m+1][j].needsMore = true;
              if (j > 0)
                pix[m][j-1].needsMore = true;
              if (j < rtWidth-1)
                pix[m][j+1].needsMore = true;
            }
      }

      // Copy the colors into the image, and update the image if enough time has elapsed.

      recordRow(pix, tempPixel, currentRow[0]);
      if (System.currentTimeMillis()-updateTime > 5000)
      {
        listener.imageUpdated(img);
        updateTime = System.currentTimeMillis();
      }

      // Rotate the temporary pixel buffer by two rows.

      PixelInfo temp1[] = pix[0], temp2[] = pix[1];
      for (int j = 0; j < 4; j++)
        pix[j] = pix[j+2];
      pix[4] = temp1;
      pix[5] = temp2;
      for (int j = 0; j < rtWidth; j++)
        pix[5][j].clear();
      loadRow(pix[4], currentRow[0]+2, tempColor);
    }

    // Copy the final row of pixels into the image.

    recordRow(pix, tempPixel, height-1);

    // All done.  Send the final image.

    threads.finish();
    finish();
  }

  /** Load a row of pixels from the image. */

  protected void loadRow(PixelInfo pix[], int y, RGBColor temp)
  {
    for (PixelInfo p : pix)
      p.clear();
    if (y >= height)
      return;
    for (int i = 0; i < width; i++)
    {
      int x = i*2+1;
      int index = i+y*width;
      temp.setRGB(floatImage[0][index], floatImage[1][index], floatImage[2][index]);
      pix[x].add(temp, 1.0f-floatImage[3][index]);
      if (depthImage != null)
        pix[x].depth = depthImage[index];
      if (objectImage != null)
        pix[x].object = objectImage[index];
    }
  }

  /** Record a row of pixels into the image. */

  protected void recordRow(PixelInfo pix[][], PixelInfo tempPixel, int row)
  {
    for (int i = 0; i < width; i++)
    {
      int x = i*2+1;
      tempPixel.copy(pix[1][x]);
      tempPixel.add(pix[1][x+1]);
      tempPixel.add(pix[2][x]);
      tempPixel.add(pix[2][x+1]);
      if (antialiasLevel == 2)
      {
        tempPixel.add(tempPixel);
        tempPixel.add(pix[0][x]);
        tempPixel.add(pix[0][x+1]);
        tempPixel.add(pix[3][x]);
        tempPixel.add(pix[3][x+1]);
        tempPixel.add(pix[1][x-1]);
        tempPixel.add(pix[2][x-1]);
        tempPixel.add(pix[1][x+2]);
        tempPixel.add(pix[2][x+2]);
      }
      recordPixel(i, row, 1, tempPixel);
      if (errorImage != null)
      {
        // If we only have one ray/subpixel, we need to estimate standard deviation from the differences between subpixels.

        if (pix[1][x].raysSent+pix[1][x+1].raysSent+pix[2][x].raysSent+pix[2][x+1].raysSent == 4)
        {
          float ninvTotal = 1.0f/tempPixel.raysSent;
          PixelInfo p1 = pix[1][x];
          PixelInfo p2 = pix[1][x+1];
          PixelInfo p3 = pix[2][x];
          PixelInfo p4 = pix[2][x+1];
          float ninv1 = 1.0f/p1.raysSent;
          float ninv2 = 1.0f/p2.raysSent;
          float ninv3 = 1.0f/p3.raysSent;
          float ninv4 = 1.0f/p4.raysSent;
          float r = tempPixel.red*ninvTotal;
          float g = tempPixel.green*ninvTotal;
          float b = tempPixel.blue*ninvTotal;
          errorImage[i+row*width] =
              ((p1.red*ninv1-r)*(p1.red*ninv1-r) + (p1.green*ninv1-g)*(p1.green*ninv1-g) + (p1.blue*ninv1-b)*(p1.blue*ninv1-b) +
                  (p2.red*ninv2-r)*(p2.red*ninv2-r) + (p2.green*ninv2-g)*(p2.green*ninv2-g) + (p2.blue*ninv2-b)*(p2.blue*ninv2-b) +
                  (p3.red*ninv3-r)*(p2.red*ninv3-r) + (p3.green*ninv3-g)*(p3.green*ninv3-g) + (p3.blue*ninv3-b)*(p3.blue*ninv3-b) +
                  (p4.red*ninv4-r)*(p3.red*ninv4-r) + (p4.green*ninv4-g)*(p4.green*ninv4-g) + (p4.blue*ninv4-b)*(p4.blue*ninv4-b))/12.0f;
        }
        else
        {
          int degreesOfFreedom = (antialiasLevel == 2 ? tempPixel.raysSent/2 : tempPixel.raysSent);
          errorImage[i+row*width] = (tempPixel.getRedVariance()+tempPixel.getGreenVariance()+tempPixel.getBlueVariance())/(3.0f*degreesOfFreedom);
        }
      }
    }
  }

  /** Record a single pixel into the image. */

  protected void recordPixel(int x, int y, int size, PixelInfo pix)
  {
    int index = x+y*width;
    if (size == 1)
      pixel[index] = pix.calcARGB();
    else
    {
      int rows = Math.min(size, width-x);
      int cols = Math.min(size, height-y);
      int argb = pix.calcARGB();
      for (int i = 0; i < rows; i++)
        for (int j = 0; j < cols; j++)
          pixel[(x+i)+(y+j)*width] = argb;
    }
    float ninv = 1.0f/pix.raysSent;
    floatImage[0][index] = pix.red*ninv;
    floatImage[1][index] = pix.green*ninv;
    floatImage[2][index] = pix.blue*ninv;
    floatImage[3][index] = 1.0f-pix.transparency*ninv;
    if (depthImage != null)
      depthImage[index] = pix.depth;
    if (objectImage != null)
      objectImage[index] = pix.object;
  }

  /** This routine is called when rendering is finished.  It sets variables to null and
   runs a garbage collection. */

  protected void finish()
  {
    theScene = null;
    theCamera = null;
    envMapping = null;
    renderThread = null;
    globalMap = null;
    causticsMap = null;
    volumeMap = null;
    RenderListener rl = listener;
    ComplexImage im =  null;
    Image image = img;
    if (image != null)
    {
      im = new ComplexImage(image);
      if (rl != null)
      {
        im.setComponentValues(ComplexImage.RED, floatImage[0]);
        im.setComponentValues(ComplexImage.GREEN, floatImage[1]);
        im.setComponentValues(ComplexImage.BLUE, floatImage[2]);
        im.setComponentValues(ComplexImage.ALPHA, floatImage[3]);
        if (depthImage != null)
          im.setComponentValues(ComplexImage.DEPTH, depthImage);
        if (objectImage != null)
          im.setComponentValues(ComplexImage.OBJECT, objectImage);
        if (errorImage != null)
          im.setComponentValues(ComplexImage.NOISE, errorImage);
        listener = null;
      }
    }
    img = null;
    pixel = null;
    floatImage = null;
    depthImage = null;
    errorImage = null;
    objectImage = null;
    raytracer.cleanup();
    raytracer = null;
    System.gc();
    if (rl != null && im != null)
      rl.imageComplete(im);
  }

  /** This routine sends out a new ray, starting from the viewpoint and passing through
   pixel (i, j).  Number indicates which ray this is within the pixel, and is used for
   distribution ray tracing.  The light color is returned in color[0], and the
   transparency in transparency[0]. */

  protected double spawnEyeRay(RenderWorkspace workspace, int i, int j, int number, int outOf)
  {
    Ray ray = workspace.ray[0];
    Vec3 orig = ray.getOrigin(), dir = ray.getDirection();
    double h = i-rtWidth*0.5+0.5, v = j-rtHeight*0.5+0.5;
    Random random = workspace.context.random;

    if (antialiasLevel > 0)
    {
      int rows = FastMath.ceil(Math.sqrt(outOf));
      int cols = outOf/rows;
      int num = number%outOf;
      int row = num/cols;
      int col = num-row*cols;
      h += (col+random.nextDouble())/cols-0.5;
      v += (row+random.nextDouble())/rows-0.5;
    }
    double dof1 = 0.0, dof2 = 0.0;
    if (depth)
    {
      dof1 = 0.25*(random.nextDouble()+distrib1[number&15]);
      dof2 = 0.25*(random.nextDouble()+distrib2[number&15]);
    }
    sceneCamera.getRayFromCamera(h/rtHeight, v/rtHeight, dof1, dof2, orig, dir);
    theCamera.getCameraCoordinates().fromLocal().transform(orig);
    theCamera.getCameraCoordinates().fromLocal().transformDirection(dir);
    ray.newID();
    workspace.rayIntensity[0].setRGB(1.0f, 1.0f, 1.0f);
    workspace.firstObjectHit = null;
    double distScale = dir.dot(theCamera.getCameraCoordinates().getZDirection());
    OctreeNode node = raytracer.getCameraNode();
    if (node == null)
      node = raytracer.getRootNode().findFirstNode(ray);
    if (node == null)
    {
      RGBColor color = workspace.color[0];
      TextureSpec surfSpec = workspace.surfSpec[0];
      if (transparentBackground)
      {
        workspace.transparency[0] = 1.0;
        color.setRGB(0.0f, 0.0f, 0.0f);
        return Float.MAX_VALUE;
      }
      if (envMode == Scene.ENVIRON_SOLID)
      {
        color.copy(envColor);
        return Float.MAX_VALUE;
      }
      envMapping.getTextureSpec(ray.direction, surfSpec, 1.0, smoothScale, time, envParamValue);
      if (envMode == Scene.ENVIRON_DIFFUSE)
        color.copy(surfSpec.diffuse);
      else
        color.copy(surfSpec.emissive);
      return Float.MAX_VALUE;
    }
    if (!workspace.materialAtCameraIsFixed)
    {
      workspace.materialAtCamera = getMaterialAtPoint(workspace, orig, node);
      workspace.materialAtCameraIsFixed = !depth;
    }
    if (workspace.materialAtCamera == null)
      return distScale*spawnRay(workspace, 0, node, SurfaceIntersection.NO_INTERSECTION, null, null, null, null, number, 0.0, true, false);
    return distScale*spawnRay(workspace, 0, node, SurfaceIntersection.NO_INTERSECTION, workspace.materialAtCamera.getMaterialMapping(), null, workspace.materialAtCamera.toLocal(), null, number, 0.0, true, false);
  }

  /** Determine what material is present at a particular point in the scene.

   @param workspace      contains information for the thread currently being executed
   @param pos     the point at which to determine the material
   @param node    the octree node containing the point
   @return the object with a material which the point is inside, or null if it is not inside any material.
   */

  protected RTObject getMaterialAtPoint(RenderWorkspace workspace, Vec3 pos, OctreeNode node)
  {
    // Many points can be excluded immediately.

    if (materialBounds == null || !materialBounds.contains(pos))
      return null;

    // Create a ray pointing away from the point.

    Ray r = workspace.ray[maxRayDepth];
    r.origin.set(pos);
    double len2 = pos.length2();
    if (len2 > 1e-5)
    {
      r.direction.set(pos);
      r.direction.scale(1.0/Math.sqrt(len2));
    }
    else
      r.direction.set(0.0, 0.0, 1.0);
    r.newID();

    // Trace the ray and watch for it to exit a material.

    int matCount = 0;
    MaterialIntersection matChange[] = workspace.matChange;
    Vec3 trueNorm = workspace.trueNormal[0];
    SurfaceIntersection first, next = SurfaceIntersection.NO_INTERSECTION;
    Raytracer.RayIntersection intersect = workspace.context.intersect;
    while (true)
    {
      if (next == SurfaceIntersection.NO_INTERSECTION)
      {
        node = raytracer.traceRay(r, node, intersect);
        if (node == null)
          return null;
        first = intersect.getFirst();
        next = intersect.getSecond();
      }
      else
      {
        first = next;
        next = SurfaceIntersection.NO_INTERSECTION;
      }
      MaterialMapping mat = first.getObject().getMaterialMapping();
      if (mat != null)
      {
        first.trueNormal(trueNorm);
        double angle = -trueNorm.dot(r.getDirection());
        boolean entered = (angle > 0.0);
        if (entered)
        {
          if (matCount == matChange.length)
          {
            workspace.increaseMaterialChangeLength();
            matChange = workspace.matChange;
          }
          matChange[matCount++].mat = mat;
        }
        else if (matCount > 0 && matChange[matCount-1].mat == mat)
          matCount--;
        else
          return first.getObject();
      }
      if (next == SurfaceIntersection.NO_INTERSECTION)
      {
        first.intersectionPoint(0, r.getOrigin());
        r.newID();
      }
    }
  }

  /** This routine is called recursively to spawn new rays.  It traces the ray, spawning
   still other rays as necessary, and returns the total light incident on the ray origin
   from the specified direction.  The appropriate Ray object should be set up before
   calling this method, and the light color is returned in the appropriate RGBColor
   object.

   @param workspace          contains information for the thread currently being executed
   @param treeDepth          the depth of this ray within the ray tree
   @param node               the first octree node which the ray intersects
   @param first              the first object which the ray intersects, or null if this is not known
   @param currentMaterial    the MaterialMapping at the ray's origin (may be null)
   @param prevMaterial       the MaterialMapping the ray was passing through before entering currentMaterial
   @param currentMatTrans    the transform to local coordinates for the current material
   @param prevMatTrans       the transform to local coordinates for the previous material
   @param rayNumber          the number of the ray within the pixel (for distribution ray tracing)
   @param totalDist          the distance traveled from the viewpoint
   @param transmitted        true if this ray has only been transmitted (not reflected) since leaving the eye
   @param diffuse            true if this ray has been diffusely reflected since leaving the eye
   @return the distance to the first object hit by the ray
   */

  protected double spawnRay(RenderWorkspace workspace, int treeDepth, OctreeNode node, SurfaceIntersection first, MaterialMapping currentMaterial, MaterialMapping prevMaterial, Mat4 currentMatTrans, Mat4 prevMatTrans, int rayNumber, double totalDist, boolean transmitted, boolean diffuse)
  {
    SurfaceIntersection second = SurfaceIntersection.NO_INTERSECTION;
    double dist, dot, truedot, n, beta = 0.0, d;
    Vec3 intersectionPoint = workspace.pos[treeDepth], norm = workspace.normal[treeDepth], trueNorm = workspace.trueNormal[treeDepth], temp;
    boolean totalReflect = false;
    Ray r = workspace.ray[treeDepth];
    TextureSpec spec = workspace.surfSpec[treeDepth];
    MaterialMapping nextMaterial, oldMaterial;
    Mat4 nextMatTrans, oldMatTrans = null;
    RGBColor color = workspace.color[treeDepth], rayIntensity = workspace.rayIntensity[treeDepth];
    OctreeNode nextNode;

    // Find the intersection between the ray and the first object it hits.

    workspace.transparency[treeDepth] = 0.0;
    SurfaceIntersection intersection = SurfaceIntersection.NO_INTERSECTION;
    if (first != SurfaceIntersection.NO_INTERSECTION)
    {
      intersection = r.findIntersection(first.getObject());
      if (intersection == SurfaceIntersection.NO_INTERSECTION)
      {
        // If the intersection is very close to the ray origin, findIntersection() may have
        // ignored it.  Move back a tiny bit.

        Ray r2 = workspace.ray[treeDepth+1];
        r2.origin.set(r.origin);
        r2.direction.set(r.direction);
        r2.origin.x -= Raytracer.TOL*r.direction.x;
        r2.origin.y -= Raytracer.TOL*r.direction.y;
        r2.origin.z -= Raytracer.TOL*r.direction.z;
        intersection = r2.findIntersection(first.getObject());
      }
    }
    if (intersection != SurfaceIntersection.NO_INTERSECTION)
    {
      intersection.intersectionPoint(0, intersectionPoint);
      nextNode = raytracer.getRootNode().findNode(intersectionPoint);
    }
    else
    {
      Raytracer.RayIntersection intersect = workspace.context.intersect;
      nextNode = raytracer.traceRay(r, node, intersect);
      if (nextNode == null)
      {
        if (transmitted && transparentBackground)
        {
          color.setRGB(0.0f, 0.0f, 0.0f);
          workspace.transparency[treeDepth] = Math.min(Math.min(rayIntensity.getRed(), rayIntensity.getGreen()), rayIntensity.getBlue());
          return Float.MAX_VALUE;
        }
        if (envMode == Scene.ENVIRON_SOLID)
        {
          color.copy(envColor);
          color.multiply(rayIntensity);
          return Float.MAX_VALUE;
        }
        double envSmoothing = (diffuse ? smoothScale*extraGIEnvSmoothing : smoothScale);
        envMapping.getTextureSpec(r.direction, spec, 1.0, smoothing*envSmoothing, time, envParamValue);
        if (envMode == Scene.ENVIRON_DIFFUSE)
          color.copy(spec.diffuse);
        else
          color.copy(spec.emissive);
        color.multiply(rayIntensity);
        return Float.MAX_VALUE;
      }
      first = intersect.getFirst();
      second = intersect.getSecond();
      intersection = first;
      intersection.intersectionPoint(0, intersectionPoint);
    }
    if (treeDepth == 0)
      workspace.firstObjectHit = first.getObject();
    dist = intersection.intersectionDist(0);
    totalDist += dist;
    intersection.trueNormal(trueNorm);
    truedot = trueNorm.dot(r.getDirection());
    double texSmoothing = (diffuse ? smoothScale*extraGISmoothing : smoothScale);
    if (truedot > 0.0)
      intersection.intersectionProperties(spec, norm, r.getDirection(), totalDist*texSmoothing*3.0/(2.0+truedot), time);
    else
      intersection.intersectionProperties(spec, norm, r.getDirection(), totalDist*texSmoothing*3.0/(2.0-truedot), time);

    // Get the direct lighting contribution, and adjust the ray intensity based on the 
    // material it is passing through.

    getDirectLight(workspace, intersectionPoint, norm, (truedot<0.0), r.getDirection(), treeDepth, nextNode, rayNumber, totalDist, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, diffuse);
    if (currentMaterial != null)
    {
      propagateRay(workspace, r, node, dist, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, workspace.tempColor, rayIntensity, treeDepth, totalDist);
      color.multiply(rayIntensity);
      color.add(workspace.tempColor);
    }
    else if (fog)
    {
      float fract = (float) Math.exp(-dist/fogDist);
      color.scale(fract);
      workspace.tempColor.copy(fogColor);
      workspace.tempColor.scale(1.0f-fract);
      color.add(workspace.tempColor);
      color.multiply(rayIntensity);
      rayIntensity.scale(fract);
    }
    else
      color.multiply(rayIntensity);

    // Determine which types of rays to spawn.

    if (treeDepth == maxRayDepth-1)
      return dist;
    if (giMode == GI_AMBIENT_OCCLUSION && diffuse)
      return dist; // This ray was sampling ambient occlusion, so don't send out any more rays.
    boolean spawnSpecular = false, spawnTransmitted = false, spawnDiffuse = false;
    float specularScale = 1.0f, transmittedScale = 1.0f, diffuseScale = 1.0f;
    Random random = workspace.context.random;
    if (roulette)
    {
      // Russian Roulette sampling is enabled: randomly decide whether to spawn a
      // ray of each type.

      float prob = (rayIntensity.getRed()*spec.specular.getRed() +
          rayIntensity.getGreen()*spec.specular.getGreen() +
          rayIntensity.getBlue()*spec.specular.getBlue())/3.0f;
      if (prob > random.nextFloat())
      {
        spawnSpecular = true;
        specularScale = 1.0f/prob;
      }
      prob = (rayIntensity.getRed()*spec.transparent.getRed() +
          rayIntensity.getGreen()*spec.transparent.getGreen() +
          rayIntensity.getBlue()*spec.transparent.getBlue())/3.0f;
      if (prob > random.nextFloat())
      {
        spawnTransmitted = true;
        transmittedScale = 1.0f/prob;
      }
      if (giMode == GI_MONTE_CARLO || giMode == GI_AMBIENT_OCCLUSION || (giMode == GI_HYBRID && !diffuse))
      {
        prob = (rayIntensity.getRed()*spec.diffuse.getRed() +
            rayIntensity.getGreen()*spec.diffuse.getGreen() +
            rayIntensity.getBlue()*spec.diffuse.getBlue())/3.0f;
        if (prob > random.nextFloat())
        {
          spawnDiffuse = true;
          diffuseScale = 1.0f/prob;
        }
      }
    }
    else
    {
      // Russian Roulette sampling is disabled.  Always spawn rays whenever appropriate.

      spawnSpecular = (rayIntensity.getRed()*spec.specular.getRed() > minRayIntensity ||
          rayIntensity.getGreen()*spec.specular.getGreen() > minRayIntensity ||
          rayIntensity.getBlue()*spec.specular.getBlue() > minRayIntensity);
      spawnTransmitted = (rayIntensity.getRed()*spec.transparent.getRed() > minRayIntensity ||
          rayIntensity.getGreen()*spec.transparent.getGreen() > minRayIntensity ||
          rayIntensity.getBlue()*spec.transparent.getBlue() > minRayIntensity);
      if (giMode == GI_MONTE_CARLO || giMode == GI_AMBIENT_OCCLUSION || (giMode == GI_HYBRID && !diffuse))
        spawnDiffuse = (rayIntensity.getRed()*spec.diffuse.getRed() > minRayIntensity ||
            rayIntensity.getGreen()*spec.diffuse.getGreen() > minRayIntensity ||
            rayIntensity.getBlue()*spec.diffuse.getBlue() > minRayIntensity);
    }

    // Now spawn the rays.

    dot = norm.dot(r.getDirection());
    RGBColor col = workspace.rayIntensity[treeDepth+1];
    if (spawnTransmitted)
    {
      // Spawn a transmitted ray.

      col.copy(rayIntensity);
      col.multiply(spec.transparent);
      col.scale(transmittedScale);
      workspace.ray[treeDepth+1].getOrigin().set(intersectionPoint);
      temp = workspace.ray[treeDepth].tempVec1;
      RTObject hitObject = first.getObject();
      if (hitObject.getMaterialMapping() == null)
      {
        // Not a solid object, so the bulk material does not change.

        temp.set(r.getDirection());
        nextMaterial = currentMaterial;
        nextMatTrans = currentMatTrans;
        oldMaterial = prevMaterial;
        oldMatTrans = prevMatTrans;
      }
      else if (truedot < 0.0)
      {
        // Entering an object.

        nextMaterial = hitObject.getMaterialMapping();
        nextMatTrans = hitObject.toLocal();
        oldMaterial = currentMaterial;
        oldMatTrans = currentMatTrans;
        if (currentMaterial == null)
          n = nextMaterial.indexOfRefraction();
        else
          n = nextMaterial.indexOfRefraction()/currentMaterial.indexOfRefraction();
        beta = -(dot+Math.sqrt(n*n-1.0+dot*dot));
        temp.set(norm);
        temp.scale(beta);
        temp.add(r.getDirection());
        temp.scale(1.0/n);
      }
      else
      {
        // Exiting an object.

        if (currentMaterial == hitObject.getMaterialMapping())
        {
          nextMaterial = prevMaterial;
          nextMatTrans = prevMatTrans;
          oldMaterial = null;
          if (nextMaterial == null)
            n = 1.0/currentMaterial.indexOfRefraction();
          else
            n = nextMaterial.indexOfRefraction()/currentMaterial.indexOfRefraction();
        }
        else
        {
          nextMaterial = currentMaterial;
          nextMatTrans = currentMatTrans;
          if (prevMaterial == hitObject.getMaterialMapping())
            oldMaterial = null;
          else
          {
            oldMaterial = prevMaterial;
            oldMatTrans = prevMatTrans;
          }
          n = 1.0;
        }
        beta = dot-Math.sqrt(n*n-1.0+dot*dot);
        temp.set(norm);
        temp.scale(-beta);
        temp.add(r.getDirection());
        temp.scale(1.0/n);
      }
      if (Double.isNaN(beta))
        totalReflect = true;
      else
      {
        d = (truedot > 0.0 ? temp.dot(trueNorm) : -temp.dot(trueNorm));
        if (d < 0.0)
        {
          // Make sure it comes out the correct side.

          d += Raytracer.TOL;
          temp.x -= d*trueNorm.x;
          temp.y -= d*trueNorm.y;
          temp.z -= d*trueNorm.z;
          temp.normalize();
        }
        workspace.ray[treeDepth+1].newID();
        int numRays = (gloss && spec.cloudiness != 0.0 ? glossRays : 1);
        if (transmitted && transparentBackground)
          workspace.transparency[treeDepth] = 0.0;
        for (int i = 0; i < numRays; i++)
        {
          workspace.ray[treeDepth+1].getDirection().set(temp);
          if (gloss)
            randomizeDirection(workspace.ray[treeDepth+1].getDirection(), norm, random, spec.cloudiness, rayNumber+treeDepth+1);
          spawnRay(workspace, treeDepth+1, nextNode, second, nextMaterial, oldMaterial, nextMatTrans, oldMatTrans, rayNumber, totalDist, transmitted, diffuse);
          workspace.color[treeDepth+1].scale(1.0/numRays);
          color.add(workspace.color[treeDepth+1]);
          if (transmitted && transparentBackground)
            workspace.transparency[treeDepth] += workspace.transparency[treeDepth+1]/numRays;
        }
      }
    }
    if (spawnSpecular || totalReflect)
    {
      // Spawn a reflection ray.

      col.copy(spec.specular);
      col.scale(specularScale);
      if (totalReflect)
        col.add(spec.transparent.getRed()*transmittedScale, spec.transparent.getGreen()*transmittedScale, spec.transparent.getBlue()*transmittedScale);
      col.multiply(rayIntensity);
      temp = workspace.ray[treeDepth].tempVec1;
      temp.set(norm);
      temp.scale(-2.0*dot);
      temp.add(r.getDirection());
      d = (truedot > 0.0 ? temp.dot(trueNorm) : -temp.dot(trueNorm));
      if (d >= 0.0)
      {
        // Make sure it comes out the correct side.

        d += Raytracer.TOL;
        temp.x += d*trueNorm.x;
        temp.y += d*trueNorm.y;
        temp.z += d*trueNorm.z;
        temp.normalize();
      }
      workspace.ray[treeDepth+1].getOrigin().set(intersectionPoint);
      workspace.ray[treeDepth+1].newID();
      int numRays = (gloss && spec.roughness != 0.0 ? glossRays : 1);
      for (int i = 0; i < numRays; i++)
      {
        workspace.ray[treeDepth+1].getDirection().set(temp);
        if (gloss)
          randomizeDirection(workspace.ray[treeDepth+1].getDirection(), norm, random, spec.roughness, rayNumber+treeDepth+1+i);
        spawnRay(workspace, treeDepth+1, nextNode, SurfaceIntersection.NO_INTERSECTION, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, rayNumber, totalDist, false, diffuse);
        workspace.color[treeDepth+1].scale(1.0/numRays);
        color.add(workspace.color[treeDepth+1]);
      }
    }
    if (spawnDiffuse)
    {
      // Spawn a diffusely reflected ray.

      int numRays = (diffuse ? 1 : diffuseRays);
      col.copy(spec.diffuse);
      col.multiply(rayIntensity);
      col.scale(diffuseScale);
      temp = workspace.ray[treeDepth+1].getDirection();
      for (int i = 0; i < numRays; i++)
      {
        do
        {
          temp.set(0.0, 0.0, 0.0);
          randomizePoint(temp, random, 1.0, rayNumber+treeDepth+1+i);
          temp.normalize();
          d = temp.dot(norm) * (dot > 0.0 ? 1.0 : -1.0);
        } while (random.nextDouble() > (d < 0.0 ? -d : d));
        if (temp.dot(trueNorm) * (truedot > 0.0 ? 1.0 : -1.0) > 0.0)
        {
          // Make sure it comes out the correct side.

          temp.scale(-1.0);
        }
        workspace.ray[treeDepth+1].getOrigin().set(intersectionPoint);
        workspace.ray[treeDepth+1].newID();
        spawnRay(workspace, treeDepth+1, nextNode, SurfaceIntersection.NO_INTERSECTION, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans, rayNumber, totalDist, false, true);
        workspace.color[treeDepth+1].scale(1.0f/numRays);
        color.add(workspace.color[treeDepth+1]);
      }
    }
    return dist;
  }

  /** Find the direct lighting contribution to the surface color.  The surface properties for the given point
   should be in surfSpec[treeDepth], and the resulting color is returned in color[treeDepth].

   @param workspace          contains information for the thread currently being executed
   @param pos                the point for which light is being calculated
   @param normal             the local surface normal
   @param front              true if the surface is being viewed from the front
   @param viewDir            the direction from which the surface is being viewed
   @param treeDepth          the current ray tree depth
   @param node               the octree node containing pos
   @param rayNumber          the number of the ray within the pixel (for distribution ray tracing)
   @param totalDist          the distance traveled from the viewpoint
   @param currentMaterial    the MaterialMapping at the point (may be null)
   @param prevMaterial       the MaterialMapping the ray was passing through before entering currentMaterial
   @param currentMatTrans    the transform to local coordinates for the current material
   @param prevMatTrans       the transform to local coordinates for the previous material
   @param diffuse            true if this ray has been diffusely reflected since leaving the eye
   */

  protected void getDirectLight(RenderWorkspace workspace, Vec3 pos, Vec3 normal, boolean front, Vec3 viewDir, int treeDepth, OctreeNode node, int rayNumber, double totalDist, MaterialMapping currentMaterial, MaterialMapping prevMaterial, Mat4 currentMatTrans, Mat4 prevMatTrans, boolean diffuse)
  {
    int i;
    RGBColor lightColor = workspace.color[treeDepth+1], finalColor = workspace.color[treeDepth];
    TextureSpec spec = workspace.surfSpec[treeDepth];
    Vec3 dir;
    Ray r = workspace.ray[treeDepth+1];
    double sign, distToLight, dot;
    boolean hilight;
    Light lt;

    // Start with the ambient and emissive contributions.

    finalColor.copy(ambColor);
    finalColor.multiply(spec.diffuse);
    finalColor.add(spec.emissive);

    // If this ray was sampling ambient occlusion, we can stop now.

    if (giMode == GI_AMBIENT_OCCLUSION && diffuse)
      return;

    // If appropriate, get the light from photon maps.

    if (giMode == GI_HYBRID && diffuse)
    {
      workspace.globalMap.getLight(pos, spec, normal, viewDir, front, lightColor);
      finalColor.add(lightColor);
      return;
    }
    if (giMode == GI_PHOTON)
    {
      workspace.globalMap.getLight(pos, spec, normal, viewDir, front, lightColor);
      finalColor.add(lightColor);
    }
    if (caustics)
    {
      workspace.causticsMap.getLight(pos, spec, normal, viewDir, front, lightColor);
      finalColor.add(lightColor);
    }

    // Now loop over the list of lights.

    dir = r.getDirection();
    sign = front ? 1.0 : -1.0;
    hilight = (spec.hilight.getRed() != 0.0 || spec.hilight.getGreen() != 0.0 || spec.hilight.getBlue() != 0.0);
    for (i = raytracer.getLights().length-1; i >= 0; i--)
    {
      RTLight light = raytracer.getLights()[i];
      int numRays = (light.getSoftShadows() ? shadowRays : 1);
      for (int j = 0; j < numRays; j++)
      {
        lt = light.getLight();
        distToLight = light.findRayToLight(pos, r, this, rayNumber+treeDepth+1+j);
        r.newID();

        // Now scan through the list of objects, and see if the light is blocked.

        if (lt.getType() == Light.TYPE_AMBIENT)
          dot = 1.0;
        else
          dot = sign*dir.dot(normal);
        if (dot > 0.0)
        {
          lt.getLight(lightColor, light.getCoords().toLocal().times(pos));
          if (Math.abs(lightColor.getRed()*(spec.diffuse.getRed()*dot+spec.hilight.getRed())) < minRayIntensity &&
              Math.abs(lightColor.getGreen()*(spec.diffuse.getGreen()*dot+spec.hilight.getGreen())) < minRayIntensity &&
              Math.abs(lightColor.getBlue()*(spec.diffuse.getBlue()*dot+spec.hilight.getBlue())) < minRayIntensity)
            continue;
          if (lt.getType() == Light.TYPE_AMBIENT || lt.getType() == Light.TYPE_SHADOWLESS || traceLightRay(workspace, r, treeDepth+1, node, raytracer.getLightNodes()[i], distToLight, totalDist, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans))
          {
            RGBColor tempColor = workspace.tempColor;
            tempColor.copy(lightColor);
            tempColor.multiply(spec.diffuse);
            tempColor.scale(dot/numRays);
            finalColor.add(tempColor);
            if (hilight)
            {
              dir.subtract(viewDir);
              dir.normalize();
              dot = sign*dir.dot(normal);
              if (dot > 0.0)
              {
                tempColor.copy(lightColor);
                tempColor.multiply(spec.hilight);
                tempColor.scale(FastMath.pow(dot, (int) ((1.0-spec.roughness)*128.0)+1)/numRays);
                finalColor.add(tempColor);
              }
            }
          }
        }
      }
    }
  }

  /** Trace a ray to a light source, and determine which objects it intersects.  If the ray
   is completely blocked, such that no light from the light source reaches the ray origin,
   return false.  Otherwise, return true, and reduce the intensity of color[treeDepth] to
   give the amount of light which reaches the ray origin.  Arguments are:

   @param workspace          contains information for the thread currently being executed
   @param r                  the ray to trace
   @param treeDepth          the current ray tree depth
   @param node               the octree node containing the ray origin
   @param endNode            the node containing the Light, or null if the light is outside the octree
   @param distToLight        the distance from the ray origin to the light
   @param totalDist          the distance traveled from the viewpoint
   @param currentMaterial    the MaterialMapping at the ray's origin (may be null)
   @param prevMaterial       the MaterialMapping the ray was passing through before entering currentMaterial
   @param currentMatTrans    the transform to local coordinates for the current material
   @param prevMatTrans       the transform to local coordinates for the previous material */

  protected boolean traceLightRay(RenderWorkspace workspace, Ray r, int treeDepth, OctreeNode node, OctreeNode endNode, double distToLight, double totalDist, MaterialMapping currentMaterial, MaterialMapping prevMaterial, Mat4 currentMatTrans, Mat4 prevMatTrans)
  {
    RGBColor lightColor = workspace.color[treeDepth], transColor = workspace.surfSpec[treeDepth].transparent;
    Vec3 intersectionPoint = workspace.pos[maxRayDepth], trueNorm = workspace.trueNormal[maxRayDepth];
    MaterialIntersection matChange[] = workspace.matChange;
    int i, j, matCount = 0;

    do
    {
      RTObject obj[] = node.getObjects();
      for (i = obj.length-1; i >= 0; i--)
      {
        SurfaceIntersection intersection = r.findIntersection(obj[i]);
        if (intersection != SurfaceIntersection.NO_INTERSECTION)
          for (j = 0; ; j++)
          {
            intersection.intersectionPoint(j, intersectionPoint);
            if (node.contains(intersectionPoint))
            {
              double dist = intersection.intersectionDist(j);
              if (dist < distToLight)
              {
                intersection.trueNormal(trueNorm);
                double angle = -trueNorm.dot(r.getDirection());
                intersection.intersectionTransparency(j, transColor, angle, (totalDist+dist)*smoothScale, time);
                lightColor.multiply(transColor);
                if (lightColor.getRed() < minRayIntensity && lightColor.getGreen() < minRayIntensity && lightColor.getBlue() < minRayIntensity)
                  return false;
                MaterialMapping mat = obj[i].getMaterialMapping();
                if (mat != null && mat.castsShadows())
                {
                  if (matCount == matChange.length)
                  {
                    workspace.increaseMaterialChangeLength();
                    matChange = workspace.matChange;
                  }
                  matChange[matCount].mat = mat;
                  matChange[matCount].toLocal = obj[i].toLocal();
                  matChange[matCount].dist = dist;
                  matChange[matCount].node = node;
                  matChange[matCount].entered = (angle > 0.0)^(j%2==1);
                  matCount++;
                }
              }
            }
            if (j >= intersection.numIntersections()-1)
              break;
          }
      }
      if (node == endNode)
        break;
      node = node.findNextNode(r);
    } while (node != null);
    if (currentMaterial == null && matCount == 0)
      return true;

    // The ray passes through one or more Materials, so attenuate it accordingly.

    sortMaterialList(matChange, matCount);
    if (matCount == matChange.length)
    {
      workspace.increaseMaterialChangeLength();
      matChange = workspace.matChange;
    }
    matChange[matCount++].dist = distToLight;
    double dist = 0.0;
    for (i = 0; ; i++)
    {
      if (currentMaterial != null && currentMaterial.castsShadows())
      {
        propagateLightRay(workspace, r, dist, matChange[i].dist, currentMaterial, lightColor, currentMatTrans, totalDist);
        if (lightColor.getRed() < minRayIntensity && lightColor.getGreen() < minRayIntensity && lightColor.getBlue() < minRayIntensity)
          return false;
      }
      if (i == matCount-1)
        break;
      double n1 = (currentMaterial == null ? 1.0 : currentMaterial.indexOfRefraction());
      if (matChange[i].entered)
      {
        if (matChange[i].mat != currentMaterial)
        {
          prevMaterial = currentMaterial;
          prevMatTrans = currentMatTrans;
          currentMaterial = matChange[i].mat;
          currentMatTrans = matChange[i].toLocal;
        }
      }
      else if (matChange[i].mat == currentMaterial)
      {
        currentMaterial = prevMaterial;
        currentMatTrans = prevMatTrans;
        prevMaterial = null;
      }
      else if (matChange[i].mat == prevMaterial)
        prevMaterial = null;
      if (caustics)
      {
        double n2 = (currentMaterial == null ? 1.0 : currentMaterial.indexOfRefraction());
        if (n1 != n2)
          return false;
      }
      node = matChange[i].node;
      dist = matChange[i].dist;
    }
    return true;
  }

  /** Propagate a ray through a material, and determine how much light is removed (due to
   absorption and outscattering) and added (due to emission and inscattering).
   <p>
   On exit, color[treeDepth] is set equal to the emitted color, and rayIntensity[treeDepth]
   is reduced by the appropriate factor to account for the absorbed light.

   @param workspace         contains information for the thread currently being executed
   @param r                 the ray being propagated
   @param node              the octree node containing the ray origin
   @param dist              the distance between the ray origin and the endpoint
   @param material          the MaterialMapping through which the ray is being propagated
   @param prevMaterial      the MaterialMapping the ray was passing through before entering material
   @param currentMatTrans   the transform to local coordinates for the current material
   @param prevMatTrans      the transform to local coordinates for the previous material
   @param emitted           on exit, this contains the light emitted from the material
   @param filter            on exit, this is multiplied by the attenuation factor
   @param treeDepth         the current ray tree depth
   @param totalDist         the distance traveled from the viewpoint
   */

  protected void propagateRay(RenderWorkspace workspace, Ray r, OctreeNode node, double dist, MaterialMapping material, MaterialMapping prevMaterial, Mat4 currentMatTrans, Mat4 prevMatTrans, RGBColor emitted, RGBColor filter, int treeDepth, double totalDist)
  {
    boolean scattering = material.isScattering();
    MaterialSpec matSpec = workspace.matSpec;
    float re, ge, be, rs, gs, bs;
    float rf = filter.getRed(), gf = filter.getGreen(), bf = filter.getBlue();

    if (material instanceof UniformMaterialMapping && !scattering)
    {
      // The effects of the material can be computed exactly.

      material.getMaterialSpec(r.origin, matSpec, 0.0, time);
      RGBColor trans = matSpec.transparency, blend = matSpec.color;
      float d = (float) dist;

      if (trans.getRed() == 1.0f)
        rs = 1.0f;
      else
        rs = (float) Math.pow(trans.getRed(), d);
      if (trans.getGreen() == 1.0f)
        gs = 1.0f;
      else
        gs = (float) Math.pow(trans.getGreen(), d);
      if (trans.getBlue() == 1.0f)
        bs = 1.0f;
      else
        bs = (float) Math.pow(trans.getBlue(), d);
      re = blend.getRed()*rf*(1.0f-rs);
      ge = blend.getGreen()*gf*(1.0f-gs);
      be = blend.getBlue()*bf*(1.0f-bs);
      rf *= rs;
      gf *= gs;
      bf *= bs;
    }
    else
    {
      // Integrate the material properties by stepping along the ray.

      Vec3 v = workspace.ray[treeDepth+1].origin, origin = r.origin, direction = r.direction;
      double x = 0.0, newx, dx, distToScreen = theCamera.getDistToScreen(), step;
      double origx, origy, origz, dirx, diry, dirz;

      // Find the ray origin and direction in the object's local coordinates.

      v.set(origin);
      currentMatTrans.transform(v);
      origx = v.x;
      origy = v.y;
      origz = v.z;
      v.set(direction);
      currentMatTrans.transformDirection(v);
      dirx = v.x;
      diry = v.y;
      dirz = v.z;

      // Do the integration.

      re = ge = be = 0.0f;
      step = stepSize*material.getStepSize();
      do
      {
        // Find the new point along the ray.

        dx = step*(1.5*workspace.context.random.nextDouble());
        if (adaptive && totalDist > distToScreen)
          dx *= totalDist/distToScreen;
        newx = x+dx;
        if (newx > dist)
        {
          dx = dist-x;
          x = dist;
        }
        else
          x = newx;
        totalDist += dx;
        v.set(origx+dirx*x, origy+diry*x, origz+dirz*x);

        // Find the material properties at that point.

        material.getMaterialSpec(v, matSpec, dx, time);
        RGBColor trans = matSpec.transparency, blend = matSpec.color;

        // Update the total emission and transmission.

        if (trans.getRed() == 1.0f)
          rs = 1.0f;
        else
          rs = (float) Math.pow(trans.getRed(), dx);
        if (trans.getGreen() == 1.0f)
          gs = 1.0f;
        else
          gs = (float) Math.pow(trans.getGreen(), dx);
        if (trans.getBlue() == 1.0f)
          bs = 1.0f;
        else
          bs = (float) Math.pow(trans.getBlue(), dx);
        re += blend.getRed()*rf*(1.0f-rs);
        ge += blend.getGreen()*gf*(1.0f-gs);
        be += blend.getBlue()*bf*(1.0f-bs);
        if (scattering)
        {
          RGBColor rayIntensity = workspace.rayIntensity[treeDepth+1];
          rayIntensity.setRGB(rf, gf, bf);
          rayIntensity.multiply(matSpec.scattering);
          if (rayIntensity.getRed() > minRayIntensity ||
              rayIntensity.getGreen() > minRayIntensity ||
              rayIntensity.getBlue() > minRayIntensity)
          {
            if (scatterMode == SCATTER_SINGLE || scatterMode == SCATTER_BOTH)
            {
              v.set(origin.x+direction.x*x, origin.y+direction.y*x, origin.z+direction.z*x);
              while (node != null && !node.contains(v))
                node = node.findNextNode(r);
              if (node == null)
                break;
              getScatteredLight(workspace, treeDepth+1, node, matSpec.eccentricity, totalDist, material, prevMaterial, currentMatTrans, prevMatTrans);
              RGBColor color = workspace.color[treeDepth+1];
              re += color.getRed()*(1.0f-rs);
              ge += color.getGreen()*(1.0f-gs);
              be += color.getBlue()*(1.0f-bs);
            }
            if (workspace.volumeMap != null)
            {
              RGBColor color = workspace.color[treeDepth+1];
              workspace.volumeMap.getVolumeLight(v, matSpec, r.getDirection(), color);
              color.multiply(rayIntensity);
              re += color.getRed()*(1.0f-rs);
              ge += color.getGreen()*(1.0f-gs);
              be += color.getBlue()*(1.0f-bs);
            }
          }
        }
        rf *= rs;
        gf *= gs;
        bf *= bs;
        if (rf < minRayIntensity && gf < minRayIntensity && bf < minRayIntensity)
        {
          // Everything beyond this point makes an insignificant contribution, so
          // just stop now.

          rf = gf = bf = 0.0f;
          break;
        }
      } while (x < dist);
    }

    // Set the output colors and return.

    emitted.setRGB(re, ge, be);
    filter.setRGB(rf, gf, bf);
  }

  /** Propagate a light ray through a Material, and determine how much light is removed.

   @param workspace  contains information for the thread currently being executed
   @param r          the ray being traced
   @param startDist  the distance along the ray at which to start propagating
   @param endDist    the distance along the ray at which to stop propagating
   @param material   the MaterialMapping through which the ray is passing
   @param filter     on exit, this is multiplied by the attenuation factor
   @param toLocal    the transformation from world coordinates to the material's local coordinates.
   @param totalDist  the distance traveled from the viewpoint */

  protected void propagateLightRay(RenderWorkspace workspace, Ray r, double startDist, double endDist, MaterialMapping material, RGBColor filter, Mat4 toLocal, double totalDist)
  {
    float rf = filter.getRed(), gf = filter.getGreen(), bf = filter.getBlue();
    MaterialSpec matSpec = workspace.matSpec;

    if (material instanceof UniformMaterialMapping)
    {
      // The effects of the material can be computed exactly.

      material.getMaterialSpec(r.origin, matSpec, 0.0, time);
      RGBColor trans = matSpec.transparency;
      float d = (float) (endDist-startDist);

      if (trans.getRed() != 1.0f)
        rf *= (float) Math.pow(trans.getRed(), d);
      if (trans.getGreen() != 1.0f)
        gf *= (float) Math.pow(trans.getGreen(), d);
      if (trans.getBlue() != 1.0f)
        bf *= (float) Math.pow(trans.getBlue(), d);
    }
    else
    {
      // Integrate the material properties by stepping along the ray.

      Vec3 v = workspace.ray[maxRayDepth].origin;
      double x = startDist, newx, dx, distToScreen = theCamera.getDistToScreen(), step;
      double origx, origy, origz, dirx, diry, dirz;

      // Find the ray origin and direction in the object's local coordinates.

      v.set(r.origin);
      toLocal.transform(v);
      origx = v.x;
      origy = v.y;
      origz = v.z;
      v.set(r.direction);
      toLocal.transformDirection(v);
      dirx = v.x;
      diry = v.y;
      dirz = v.z;

      // Do the integration.

      step = stepSize*material.getStepSize();
      do
      {
        // Find the new point along the ray.

        dx = step*(1.5*workspace.context.random.nextDouble());
        if (adaptive && totalDist > distToScreen)
          dx *= totalDist/distToScreen;
        newx = x+dx;
        if (newx > endDist)
        {
          dx = endDist-x;
          x = endDist;
        }
        else
          x = newx;
        totalDist += dx;
        v.set(origx+dirx*x, origy+diry*x, origz+dirz*x);

        // Find the material properties at that point.

        material.getMaterialSpec(v, matSpec, dx, time);
        RGBColor trans = matSpec.transparency;

        // Update the total emission and transmission.

        if (trans.getRed() != 1.0f)
          rf *= (float) Math.pow(trans.getRed(), dx);
        if (trans.getGreen() != 1.0f)
          gf *= (float) Math.pow(trans.getGreen(), dx);
        if (trans.getBlue() != 1.0f)
          bf *= (float) Math.pow(trans.getBlue(), dx);
        if (rf < minRayIntensity && gf < minRayIntensity && bf < minRayIntensity)
        {
          // Everything beyond this point makes an insignificant contribution, so
          // just stop now.

          rf = gf = bf = 0.0f;
          break;
        }
      } while (x < endDist);
    }

    // Set the output colors and return.

    filter.setRGB(rf, gf, bf);
  }

  /** Find the light being scattered by a point in scattering material.
   The surface properties for the given point should be in surfSpec[treeDepth-1], and
   the resulting color is returned in color[treeDepth-1].

   @param workspace                contains information for the thread currently being executed
   @param treeDepth         the current ray tree depth
   @param node              the octree node containing the point
   @param eccentricity      the eccentricity of the material
   @param totalDist         the distance traveled from the viewpoint
   @param currentMaterial   the MaterialMapping through which the ray is being propagated
   @param prevMaterial      the MaterialMapping the ray was passing through before entering material
   @param currentMatTrans   the transform to local coordinates for the current material
   @param prevMatTrans      the transform to local coordinates for the previous material
   */

  protected void getScatteredLight(RenderWorkspace workspace, int treeDepth, OctreeNode node, double eccentricity, double totalDist, MaterialMapping currentMaterial, MaterialMapping prevMaterial, Mat4 currentMatTrans, Mat4 prevMatTrans)
  {
    int i;
    RGBColor filter = workspace.rayIntensity[treeDepth], lightColor = workspace.color[treeDepth];
    Ray r = workspace.ray[treeDepth];
    Vec3 dir, pos = r.origin, viewDir = workspace.ray[treeDepth-1].direction;
    double distToLight, fatt, dot;
    double ec2 = eccentricity*eccentricity;
    Light lt;

    workspace.tempColor2.setRGB(0.0f, 0.0f, 0.0f);
    dir = r.getDirection();
    for (i = raytracer.getLights().length-1; i >= 0; i--)
    {
      RTLight light = raytracer.getLights()[i];
      lt = light.getLight();
      distToLight = light.findRayToLight(pos, r, this, -1);
      r.newID();

      // Now scan through the list of objects, and see if the light is blocked.

      lt.getLight(lightColor, light.getCoords().toLocal().times(pos));
      lightColor.multiply(filter);
      if (eccentricity != 0.0 && lt.getType() != Light.TYPE_AMBIENT)
      {
        dot = dir.dot(viewDir);
        fatt = (1.0-ec2)/Math.pow(1.0+ec2-2.0*eccentricity*dot, 1.5);
        lightColor.scale(fatt);
      }
      if (lightColor.getRed() < minRayIntensity && lightColor.getGreen() < minRayIntensity &&
          lightColor.getBlue() < minRayIntensity)
        continue;
      if (lt.getType() == Light.TYPE_AMBIENT || lt.getType() == Light.TYPE_SHADOWLESS || traceLightRay(workspace, r, treeDepth, node, raytracer.getLightNodes()[i], distToLight, totalDist, currentMaterial, prevMaterial, currentMatTrans, prevMatTrans))
        workspace.tempColor2.add(lightColor);
    }
    workspace.color[treeDepth].copy(workspace.tempColor2);
  }

  /** Add a random displacement to a vector.  The displacements are uniformly distributed
   over the volume of a sphere whose radius is given by size.  number is used for
   distributing the displacements evenly. */

  public void randomizePoint(Vec3 pos, Random random, double size, int number)
  {
    double x, y, z;
    int d;

    if (size == 0.0)
      return;

    // Pick a random vector within an octant of the unit sphere.

    do
    {
      x = random.nextDouble();
      y = random.nextDouble();
      z = random.nextDouble();
    } while (x*x + y*y + z*z > 1.0);
    x *= size;
    y *= size;
    z *= size;

    // Decide which octant of the sphere to use for this ray.

    d = distrib1[number&15];
    if (d < 2)
      x *= -1.0;
    if (d == 1 || d == 2)
      y *= -1.0;
    if ((distrib2[number&15]&1) == 0)
      z *= -1.0;
    pos.x += x;
    pos.y += y;
    pos.z += z;
  }

  /** Given a reflected or transmitted ray, randomly alter its direction to create gloss and
   translucency effects.  dir is a unit vector in the "ideal" reflected or refracted
   direction, which on exit is overwritten with the new direction.  norm is the local
   surface normal, roughness determines how much the ray direction is altered, and number
   is used for distributing rays evenly. */

  public void randomizeDirection(Vec3 dir, Vec3 norm, Random random, double roughness, int number)
  {
    double x, y, z, scale, dot1, dot2;
    int d;

    if (roughness <= 0.0)
      return;

    // Pick a random vector within an octant of the unit sphere.

    do
    {
      x = random.nextDouble();
      y = random.nextDouble();
      z = random.nextDouble();
    } while (x*x + y*y + z*z > 1.0);
    scale = Math.pow(roughness, 1.7)*0.5;
    x *= scale;
    y *= scale;
    z *= scale;

    // Decide which octant of the sphere to use for this ray.

    d = distrib1[number&15];
    if (d < 2)
      x *= -1.0;
    if (d == 1 || d == 2)
      y *= -1.0;
    if ((distrib2[number&15]&1) == 0)
      z *= -1.0;
    dot1 = dir.dot(norm);
    dir.x += x;
    dir.y += y;
    dir.z += z;
    dot2 = 2.0*dir.dot(norm);

    // If the ray is on the wrong side of the surface, flip it back.

    if (dot1 < 0.0 && dot2 > 0.0)
    {
      dir.x -= dot2*norm.x;
      dir.y -= dot2*norm.y;
      dir.z -= dot2*norm.z;
    }
    else if (dot1 > 0.0 && dot2 < 0.0)
    {
      dir.x += dot2*norm.x;
      dir.y += dot2*norm.y;
      dir.z += dot2*norm.z;
    }
    dir.normalize();
  }

  /** Sort the list of MaterialIntersection objects by position along a shadow ray. 
   This is done with a simple insertion sort.  Because the list tends to be very
   short, and is in close to the correct order to begin with, this will generally
   be very fast. */

  protected void sortMaterialList(MaterialIntersection matChange[], int count)
  {
    for (int i = 1; i < count; i++)
      for (int j = i; j > 0 && matChange[j].dist < matChange[j-1].dist; j--)
      {
        MaterialIntersection temp = matChange[j-1];
        matChange[j-1] = matChange[j];
        matChange[j] = temp;
      }
  }
}
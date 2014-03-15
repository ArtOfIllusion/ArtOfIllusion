/* Copyright (C) 2001-2014 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raster;

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

/** Raster is a Renderer which generates images with a scanline algorithm. */

public class Raster implements Renderer, Runnable
{
  private ObjectInfo light[];
  private BTabbedPane configPanel;
  private BCheckBox transparentBox, adaptiveBox, hideBackfaceBox, hdrBox;
  private BComboBox shadeChoice, aliasChoice, sampleChoice;
  private ValueField errorField, smoothField;
  private int imagePixel[], width, height, envMode, imageWidth, imageHeight;
  private int shadingMode = PHONG, samplesPerPixel = 1, subsample = 1;
  private Fragment fragment[];
  private long updateTime;
  private Scene theScene;
  private Camera theCamera;
  private RenderListener listener;
  private BufferedImage img;
  private Thread renderThread;
  private RGBColor ambColor, envColor, fogColor;
  private TextureMapping envMapping;
  private ThreadLocal threadRasterContext, threadCompositingContext;
  private RowLock lock[];
  private double envParamValue[];
  private double time, smoothing = 1.0, smoothScale, focalDist, surfaceError = 0.02, fogDist;
  private boolean fog, transparentBackground = false, adaptive = true, hideBackfaces = true, generateHDR = false, positionNeeded, depthNeeded, needCopyToUI = true;
  private boolean isPreview;

  public static final int GOURAUD = 0;
  public static final int HYBRID = 1;
  public static final int PHONG = 2;

  public static final double TOL = 1e-12;
  public static final float INTENSITY_CUTOFF = 0.005f;

  public static final Fragment BACKGROUND_FRAGMENT = new OpaqueFragment(0, Float.MAX_VALUE);
  private static final int WHITE_ERGB = new RGBColor(1.0f, 1.0f, 1.0f).getERGB();

  public Raster()
  {
    threadRasterContext = new ThreadLocal() {
      protected Object initialValue()
      {
        return new RasterContext(theCamera, width);
      }
    };
    threadCompositingContext = new ThreadLocal() {
      protected Object initialValue()
      {
        return new CompositingContext(theCamera);
      }
    };
  }

  /* Methods from the Renderer interface. */

  public String getName()
  {
    return "Raster";
  }

  public synchronized void renderScene(Scene theScene, Camera camera, RenderListener rl, SceneCamera sceneCamera)
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
    Dimension dim = camera.getSize();
    if (dim.width == 0 || dim.height == 0)
      return;

    listener = rl;
    this.theScene = theScene;
    theCamera = camera.duplicate();
    if (sceneCamera == null)
    {
      sceneCamera = new SceneCamera();
      sceneCamera.setDepthOfField(0.0);
      sceneCamera.setFocalDistance(theCamera.getDistToScreen());
    }
    focalDist = sceneCamera.getFocalDistance();
    depthNeeded = ((sceneCamera.getComponentsForFilters()&ComplexImage.DEPTH) != 0);
    time = theScene.getTime();
    if (imagePixel == null || imageWidth != dim.width || imageHeight != dim.height)
    {
      imageWidth = dim.width;
      imageHeight = dim.height;
      img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB_PRE);
      imagePixel = ((DataBufferInt) ((BufferedImage) img).getRaster().getDataBuffer()).getData();
    }
    width = imageWidth*samplesPerPixel;
    height = imageHeight*samplesPerPixel;
    theCamera.setScreenTransform(sceneCamera.getScreenTransform(width, height), width, height);
    renderThread = new Thread(this, "Raster Renderer Main Thread");
    renderThread.start();
  }

  public synchronized void cancelRendering(Scene sc)
  {
    Thread t = renderThread;
    RenderListener rl = listener;

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
    finish(null);
    rl.renderingCanceled();
  }

  public Widget getConfigPanel()
  {
    if (configPanel == null)
    {
      // General options panel.

      FormContainer generalPanel = new FormContainer(3, 4);
      LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
      LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
      generalPanel.add(Translate.label("surfaceAccuracy"), 0, 0, leftLayout);
      generalPanel.add(Translate.label("shadingMethod"), 0, 1, leftLayout);
      generalPanel.add(Translate.label("supersampling"), 0, 2, leftLayout);
      generalPanel.add(errorField = new ValueField(surfaceError, ValueField.POSITIVE, 6), 1, 0, rightLayout);
      generalPanel.add(shadeChoice = new BComboBox(new String[]{
          Translate.text("gouraud"),
          Translate.text("hybrid"),
          Translate.text("phong")
      }), 1, 1, rightLayout);
      generalPanel.add(aliasChoice = new BComboBox(new String[]{
          Translate.text("none"),
          Translate.text("Edges"),
          Translate.text("Everything")
      }), 1, 2, rightLayout);
      generalPanel.add(sampleChoice = new BComboBox(new String[]{"2x2", "3x3"}), 2, 2, rightLayout);
      sampleChoice.setEnabled(false);
      generalPanel.add(transparentBox = new BCheckBox(Translate.text("transparentBackground"), transparentBackground), 0, 3, 3, 1);
      aliasChoice.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
          sampleChoice.setEnabled(aliasChoice.getSelectedIndex() > 0);
        }
      });

      // Advanced options panel.

      FormContainer advancedPanel = new FormContainer(new double [] {0.0, 1.0}, new double [4]);
      advancedPanel.add(Translate.label("texSmoothing"), 0, 0, leftLayout);
      advancedPanel.add(smoothField = new ValueField(smoothing, ValueField.NONNEGATIVE), 1, 0, rightLayout);
      advancedPanel.add(adaptiveBox = new BCheckBox(Translate.text("reduceAccuracyForDistant"), adaptive), 0, 1, 2, 1, rightLayout);
      advancedPanel.add(hideBackfaceBox = new BCheckBox(Translate.text("eliminateBackfaces"), hideBackfaces), 0, 2, 2, 1, rightLayout);
      advancedPanel.add(hdrBox = new BCheckBox(Translate.text("generateHDR"), generateHDR), 0, 3, 2, 1, rightLayout);

      // Create the tabbed pane.

      configPanel = new BTabbedPane();
      configPanel.add(generalPanel, Translate.text("general"));
      configPanel.add(advancedPanel, Translate.text("advanced"));
    }
    if (needCopyToUI)
      copyConfigurationToUI();
    return configPanel;
  }

  /** Copy the current configuration to the user interface. */

  private void copyConfigurationToUI()
  {
    needCopyToUI = false;
    if (configPanel == null)
      getConfigPanel();
    smoothField.setValue(smoothing);
    adaptiveBox.setState(adaptive);
    hideBackfaceBox.setState(hideBackfaces);
    hdrBox.setState(generateHDR);
    errorField.setValue(surfaceError);
    shadeChoice.setSelectedIndex(shadingMode);
    transparentBox.setState(transparentBackground);
    if (samplesPerPixel == 1)
    {
      aliasChoice.setSelectedIndex(0);
    }
    else if (subsample == 1)
    {
      aliasChoice.setSelectedIndex(2);
      sampleChoice.setSelectedIndex(samplesPerPixel-2);
    }
    else
    {
      aliasChoice.setSelectedIndex(1);
      sampleChoice.setSelectedIndex(samplesPerPixel-2);
    }
    sampleChoice.setEnabled(aliasChoice.getSelectedIndex() > 0);
  }

  public boolean recordConfiguration()
  {
    smoothing = smoothField.getValue();
    adaptive = adaptiveBox.getState();
    hideBackfaces = hideBackfaceBox.getState();
    generateHDR = hdrBox.getState();
    surfaceError = errorField.getValue();
    shadingMode = shadeChoice.getSelectedIndex();
    transparentBackground = transparentBox.getState();
    if (aliasChoice.getSelectedIndex() == 0)
      samplesPerPixel = subsample = 1;
    else if (aliasChoice.getSelectedIndex() == 1)
      samplesPerPixel = subsample = sampleChoice.getSelectedIndex()+2;
    else
      {
        samplesPerPixel = sampleChoice.getSelectedIndex()+2;
        subsample = 1;
      }
    isPreview = false;
    return true;
  }


  public Map<String, Object> getConfiguration()
  {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("textureSmoothing", smoothing);
    map.put("reduceAccuracyForDistant", adaptive);
    map.put("hideBackfaces", hideBackfaces);
    map.put("highDynamicRange", generateHDR);
    map.put("maxSurfaceError", surfaceError);
    map.put("shadingMethod", shadingMode);
    map.put("transparentBackground", transparentBackground);
    int antialiasLevel = 0;
    if (samplesPerPixel == 2)
      antialiasLevel = subsample;
    else if (samplesPerPixel == 3)
      antialiasLevel = (subsample == 1 ? 3 : 4);
    map.put("antialiasing", antialiasLevel);
    return map;
  }

  public void setConfiguration(String property, Object value)
  {
    needCopyToUI = true;
    isPreview = false;
    if ("textureSmoothing".equals(property))
      smoothing = ((Number) value).doubleValue();
    else if ("reduceAccuracyForDistant".equals(property))
      adaptive = (Boolean) value;
    else if ("hideBackfaces".equals(property))
      hideBackfaces = (Boolean) value;
    else if ("highDynamicRange".equals(property))
      generateHDR = (Boolean) value;
    else if ("maxSurfaceError".equals(property))
      surfaceError = ((Number) value).doubleValue();
    else if ("shadingMethod".equals(property))
      shadingMode = (Integer) value;
    else if ("transparentBackground".equals(property))
      transparentBackground = (Boolean) value;
    else if ("antialiasing".equals(property))
    {
      int antialiasLevel = (Integer) value;
      switch (antialiasLevel)
      {
        case 0:
          samplesPerPixel = subsample = 1;
          break;
        case 1:
          samplesPerPixel = 2;
          subsample = 1;
          break;
        case 2:
          samplesPerPixel = 2;
          subsample = 2;
          break;
        case 3:
          samplesPerPixel = 3;
          subsample = 1;
          break;
        case 4:
          samplesPerPixel = 3;
          subsample = 3;
          break;
      }
    }
  }

  public void configurePreview()
  {
    if (needCopyToUI)
      copyConfigurationToUI();
    transparentBackground = false;
    smoothing = 1.0;
    adaptive = hideBackfaces = true;
    generateHDR = false;
    surfaceError = ArtOfIllusion.getPreferences().getInteractiveSurfaceError();
    shadingMode = HYBRID;
    samplesPerPixel = subsample = 1;
    isPreview = true;
  }

  /** Find all the light sources in the scene. */

  void findLights()
  {
    Vector<ObjectInfo> lt = new Vector<ObjectInfo>();
    int i;

    positionNeeded = false;
    for (i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.getObject() instanceof Light && info.isVisible())
          lt.addElement(info);
      }
    light = new ObjectInfo [lt.size()];
    for (i = 0; i < light.length; i++)
      {
        light[i] = lt.elementAt(i);
        if (!(light[i].getObject() instanceof DirectionalLight))
          positionNeeded = true;
      }
  }

  /** Main method in which the image is rendered. */

  public void run()
  {
    final Thread thisThread = Thread.currentThread();
    if (renderThread != thisThread)
      return;
    fragment = new Fragment [width*height];
    Arrays.fill(fragment, BACKGROUND_FRAGMENT);
    lock = new RowLock[height];
    for (int i = 0; i < lock.length; i++)
      lock[i] = new RowLock();
    updateTime = System.currentTimeMillis();

    // Record information about the scene.

    findLights();
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

    // Determine information about the viewpoint.

    final Vec3 viewdir = theCamera.getViewToWorld().timesDirection(Vec3.vz());
    Point p = new Point(width/2, height/2);
    final Vec3 orig = theCamera.getCameraCoordinates().getOrigin();
    Vec3 center = theCamera.convertScreenToWorld(p, focalDist);
    p.x++;
    Vec3 hvec = theCamera.convertScreenToWorld(p, focalDist).minus(center);
    p.x--;
    p.y++;
    Vec3 vvec = theCamera.convertScreenToWorld(p, focalDist).minus(center);
    p.y--;
    smoothScale = smoothing*hvec.length()/focalDist;

    // Render the objects.

    final ObjectInfo sortedObjects[] = sortObjects();
    ThreadManager threads = new ThreadManager(sortedObjects.length, new ThreadManager.Task() {
      public void execute(int index)
      {
        RasterContext context = (RasterContext) threadRasterContext.get();
        ObjectInfo obj = sortedObjects[index];
        context.camera.setObjectTransform(obj.getCoords().fromLocal());
        renderObject(obj, orig, viewdir, obj.getCoords().toLocal(), context, thisThread);
        if (thisThread != renderThread)
          return;
        if (System.currentTimeMillis()-updateTime > 5000)
          updateImage();
      }
      public void cleanup()
      {
        ((RasterContext) threadRasterContext.get()).cleanup();
      }
    });
    threads.run();
    threads.finish();
    finish(createFinalImage(center, orig, hvec, vvec));
  }

  /**
   * Sort the objects in the scene into the most efficient order for rendering.
   */

  private ObjectInfo[] sortObjects()
  {
    class SortRecord implements Comparable
    {
      public ObjectInfo object;
      public double depth;
      public boolean isTransparent;

      SortRecord(ObjectInfo object)
      {
        this.object = object;
        depth = theCamera.getObjectToView().times(object.getBounds().getCenter()).z;
        if (object.getObject().getTexture() != null)
          isTransparent = (object.getObject().getTexture().hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT));
      }

      public int compareTo(Object o)
      {
        SortRecord other = (SortRecord) o;
        if (isTransparent == other.isTransparent)
        {
          // Order by depth.

          if (depth < other.depth)
            return -1;
          if (depth == other.depth)
            return 0;
          return 1;
        }

        // Put transparent objects last.

        if (isTransparent)
          return 1;
        return -1;
      }
    }
    ArrayList<SortRecord> objects = new ArrayList<SortRecord>();
    for (int i = 0; i < theScene.getNumObjects(); i++)
    {
      ObjectInfo obj = theScene.getObject(i);
      theCamera.setObjectTransform(obj.getCoords().fromLocal());
      objects.add(new SortRecord(obj));
    }
    Collections.sort(objects);
    ObjectInfo result[] = new ObjectInfo[objects.size()];
    for (int i = 0; i < result.length; i++)
      result[i] = objects.get(i).object;
    return result;
  }

  /** Update the image being displayed. */

  private synchronized void updateImage()
  {
    if (System.currentTimeMillis()-updateTime < 5000)
      return;
    RGBColor frontColor = new RGBColor();
      for (int i1 = 0, i2 = 0; i1 < imageHeight; i1++, i2 += samplesPerPixel)
        for (int j1 = 0, j2 = 0; j1 < imageWidth; j1++, j2 += samplesPerPixel)
        {
          fragment[i2*width+j2].getAdditiveColor(frontColor);
          imagePixel[i1*imageWidth+j1] = frontColor.getARGB();
        }
    listener.imageUpdated(img);
    updateTime = System.currentTimeMillis();
  }

  /** Update the image being displayed during compositing. */

  private synchronized void updateFinalImage()
  {
    if (System.currentTimeMillis()-updateTime < 5000)
      return;
    listener.imageUpdated(img);
    updateTime = System.currentTimeMillis();
  }

  /** Create the final version of the image. */

  private ComplexImage createFinalImage(final Vec3 center, final Vec3 orig, final Vec3 hvec, final Vec3 vvec)
  {
    final Thread thisThread = Thread.currentThread();
    if (renderThread != thisThread)
      return null;
    final int n = samplesPerPixel*samplesPerPixel;
    final float hdrImage[][] = (generateHDR ? new float[3][imageWidth*imageHeight] : null);
    ThreadManager threads = new ThreadManager(imageHeight, new ThreadManager.Task() {
      public void execute(int i1)
      {
        CompositingContext context = (CompositingContext) threadCompositingContext.get();
        Vec3 dir = context.tempVec[1];
        RGBColor totalColor = context.totalColor;
        RGBColor totalTransparency = context.totalTransparency;
        RGBColor addColor = context.addColor;
        RGBColor multColor = context.multColor;
        RGBColor subpixelColor = context.subpixelColor;
        RGBColor subpixelMult = context.subpixelMult;
        ArrayList<ObjectMaterialInfo> materialStack = context.materialStack;
        TextureSpec surfSpec = context.surfSpec;
        int i2 = i1*samplesPerPixel;
        for (int j1 = 0, j2 = 0; j1 < imageWidth; j1++, j2 += samplesPerPixel)
        {
          totalColor.setRGB(0.0f, 0.0f, 0.0f);
          totalTransparency.setRGB(0.0f, 0.0f, 0.0f);
          for (int k = 0; k < samplesPerPixel; k++)
          {
            int base = width*(i2+k)+j2;
            for (int m = 0; m < samplesPerPixel; m++)
            {
              // Find the overall color of this subpixel.

              subpixelColor.setRGB(0.0f, 0.0f, 0.0f);
              subpixelMult.setRGB(1.0f, 1.0f, 1.0f);
              Fragment f = fragment[base+m];
              float lastDepth = 0;
              while (true)
              {
                // Factor in materials.

                ObjectMaterialInfo fragmentMaterial = f.getMaterialMapping();
                ObjectMaterialInfo currentMaterial = null;
                if (materialStack.size() > 0)
                  currentMaterial = materialStack.get(materialStack.size()-1);
                adjustColorsForMaterial(currentMaterial, j2+m, i2+k, lastDepth, f.getDepth(), addColor, context.multColor, context);
                addColor.multiply(subpixelMult);
                subpixelColor.add(addColor);
                subpixelMult.multiply(multColor);
                if (fragmentMaterial != null)
                {
                  if (f.isEntering())
                    materialStack.add(fragmentMaterial);
                  else
                    materialStack.remove(fragmentMaterial);
                }
                lastDepth = f.getDepth();

                // If we've reached the end, factor in the background.

                if (f == BACKGROUND_FRAGMENT)
                {
                  if (transparentBackground)
                  {
                    // Just make it invisible.

                    addColor.setRGB(0.0f, 0.0f, 0.0f);
                  }
                  else if (envMode == Scene.ENVIRON_SOLID)
                    addColor.copy(envColor);
                  else
                  {
                    // Find the background color.

                    double h = j2+k-width/2.0, v = i2+m-height/2.0;
                    dir.x = center.x + h*hvec.x + v*vvec.x;
                    dir.y = center.y + h*hvec.y + v*vvec.y;
                    dir.z = center.z + h*hvec.z + v*vvec.z;
                    dir.subtract(orig);
                    dir.normalize();
                    envMapping.getTextureSpec(dir, surfSpec, 1.0, smoothScale, time, envParamValue);
                    if (envMode == Scene.ENVIRON_DIFFUSE)
                      addColor.copy(surfSpec.diffuse);
                    else
                      addColor.copy(surfSpec.emissive);
                  }
                }
                else
                  f.getAdditiveColor(addColor);

                // Factor in the fragment color.

                addColor.multiply(subpixelMult);
                subpixelColor.add(addColor);
                if (f.isOpaque())
                {
                  if (f != BACKGROUND_FRAGMENT || !transparentBackground)
                    subpixelMult.setRGB(0.0f, 0.0f, 0.0f);
                  break;
                }
                f.getMultiplicativeColor(multColor);
                subpixelMult.multiply(multColor);
                f = f.getNextFragment();
              }
              totalColor.add(subpixelColor);
              totalTransparency.add(subpixelMult);
              materialStack.clear();
            }
          }
          totalColor.scale(1.0f/n);
          totalTransparency.scale(1.0f/n);
          imagePixel[i1*imageWidth+j1] = calcARGB(totalColor, totalTransparency);
          if (generateHDR)
          {
            hdrImage[0][i1*imageWidth+j1] = totalColor.getRed();
            hdrImage[1][i1*imageWidth+j1] = totalColor.getGreen();
            hdrImage[2][i1*imageWidth+j1] = totalColor.getBlue();
          }
        }
        if (renderThread != thisThread)
          return;
        if (System.currentTimeMillis()-updateTime > 5000)
          updateFinalImage();
      }
      public void cleanup()
      {
        ((CompositingContext) threadCompositingContext.get()).cleanup();
      }
    });
    threads.run();
    threads.finish();

    // Create the ComplexImage.

    ComplexImage image = new ComplexImage(img);
    if (generateHDR)
    {
      image.setComponentValues(ComplexImage.RED, hdrImage[0]);
      image.setComponentValues(ComplexImage.GREEN, hdrImage[1]);
      image.setComponentValues(ComplexImage.BLUE, hdrImage[2]);
    }
    if (depthNeeded)
    {
      float imageZbuffer[] = new float [imageWidth*imageHeight];
      for (int i1 = 0, i2 = 0; i1 < imageHeight; i1++, i2 += samplesPerPixel)
        for (int j1 = 0, j2 = 0; j1 < imageWidth; j1++, j2 += samplesPerPixel)
        {
          float minDepth = Float.MAX_VALUE;
          for (int k = 0; k < samplesPerPixel; k++)
          {
            int base = width*(i2+k)+j2;
            for (int m = 0; m < samplesPerPixel; m++)
            {
              float z = fragment[base+m].getDepth();
              if (z < minDepth)
                minDepth = z;
            }
          }
          imageZbuffer[i1*imageWidth+j1] = minDepth;
        }
      image.setComponentValues(ComplexImage.DEPTH, imageZbuffer);
    }
    return image;
  }

  /**
   * Adjust the additive and multiplicative colors for a pixel based on the material it is
   * passing through.
   */

  private void adjustColorsForMaterial(ObjectMaterialInfo material, int x, int y, float startDepth, float endDepth, RGBColor addColor, RGBColor multColor, CompositingContext context)
  {
    if (material == null)
    {
      // It is passing through empty space, so taking fog into account.

      if (fog)
      {
        float fract1 = (float) Math.exp((startDepth-endDepth)/fogDist), fract2 = 1.0f-fract1;
        multColor.setRGB(fract1, fract1, fract1);
        addColor.setRGB(fract1*addColor.getRed() + fract2*fogColor.getRed(),
          fract1*addColor.getGreen() + fract2*fogColor.getGreen(),
          fract1*addColor.getBlue() + fract2*fogColor.getBlue());
      }
      else
      {
        addColor.setRGB(0.0f, 0.0f, 0.0f);
        multColor.setRGB(1.0f, 1.0f, 1.0f);
      }
      return;
    }
    if (material.getMapping() instanceof UniformMaterialMapping)
    {
      // A uniform material, so we can calculate the effect exactly.

      material.getMapping().getMaterialSpec(context.tempVec[0], context.matSpec, 0.0, time);
      RGBColor trans = context.matSpec.transparency, blend = context.matSpec.color;
      double dist = endDepth-startDepth;
      float rs = (float) Math.pow(trans.getRed(), dist);
      float gs = (float) Math.pow(trans.getGreen(), dist);
      float bs = (float) Math.pow(trans.getBlue(), dist);
      multColor.setRGB(rs, gs, bs);
      addColor.setRGB(rs*addColor.getRed() + (1.0f-rs)*blend.getRed(),
        gs*addColor.getGreen() + (1.0f-gs)*blend.getGreen(),
        bs*addColor.getBlue() + (1.0f-bs)*blend.getBlue());
      return;
    }

    // Step through the material and add up the contribution at each point.

    Vec2 imagePos = new Vec2(x, y);
    Vec3 startPoint = context.camera.convertScreenToWorld(imagePos, startDepth, false);
    Vec3 endPoint = context.camera.convertScreenToWorld(imagePos, endDepth, false);
    double distToPoint = context.camera.getCameraCoordinates().getOrigin().distance(startPoint);
    material.getToLocal().transform(startPoint);
    material.getToLocal().transform(endPoint);
    double dist = startPoint.distance(endPoint);
    double stepSize = material.getMapping().getStepSize();
    double distToScreen = context.camera.getDistToScreen();
    if (distToPoint > distToScreen)
      stepSize *= distToPoint/distToScreen;
    int steps = FastMath.ceil(dist/stepSize);
    stepSize = dist/steps;
    multColor.setRGB(1.0f, 1.0f, 1.0f);
    addColor.setRGB(0.0f, 0.0f, 0.0f);
    for (int i = 0; i < steps; i++)
    {
      double fract2 = (0.5+i)/steps, fract1 = 1.0-fract2;
      context.tempVec[0].set(fract1*startPoint.x+fract2*endPoint.x,
          fract1*startPoint.y+fract2*endPoint.y,
          fract1*startPoint.z+fract2*endPoint.z);
      material.getMapping().getMaterialSpec(context.tempVec[0], context.matSpec, stepSize, time);
      RGBColor trans = context.matSpec.transparency, blend = context.matSpec.color;
      float rs = (float) Math.pow(trans.getRed(), stepSize);
      float gs = (float) Math.pow(trans.getGreen(), stepSize);
      float bs = (float) Math.pow(trans.getBlue(), stepSize);
      multColor.multiply(rs, gs, bs);
      addColor.setRGB(multColor.getRed()*addColor.getRed() + (1.0f-multColor.getRed())*blend.getRed(),
        multColor.getGreen()*addColor.getGreen() + (1.0f-multColor.getGreen())*blend.getGreen(),
        multColor.getBlue()*addColor.getBlue() + (1.0f-multColor.getBlue())*blend.getBlue());
      if (multColor.getMaxComponent() < INTENSITY_CUTOFF)
      {
        // Nothing beyond this point is visible, so stop now.

        multColor.setRGB(0.0f, 0.0f, 0.0f);
        return;
      }
    }
  }

  /** This routine is called when rendering is finished. */

  private void finish(ComplexImage finalImage)
  {
    light = null;
    theScene = null;
    theCamera = null;
    envMapping = null;
    img = null;
    imagePixel = null;
    fragment = null;
    RenderListener rl = listener;
    listener = null;
    renderThread = null;
    if (rl != null && finalImage != null)
      rl.imageComplete(finalImage);
  }

  /** Given an RGBColor and a transparency value, calculate the ARGB value. */

  private int calcARGB(RGBColor color, RGBColor transparency)
  {
    double t = (transparency.getRed()+transparency.getGreen()+transparency.getBlue())/3.0;
    if (!transparentBackground || t <= 0.0)
      return color.getARGB();
    if (t >= 1.0)
      return 0;
    double scale = 255.0/(1.0-t);
    int a, r, g, b;
    a = (int) (255.0*(1.0-t));
    r = (int) (color.getRed()*scale);
    g = (int) (color.getGreen()*scale);
    b = (int) (color.getBlue()*scale);
    if (r < 0) r = 0;
    if (r > 255) r = 255;
    if (g < 0) g = 0;
    if (g > 255) g = 255;
    if (b < 0) b = 0;
    if (b > 255) b = 255;
    return (a<<24) + (r<<16) + (g<<8) + b;
  }

  /** Render a single object into the scene.  viewdir is the direction from 
     which the object is being viewed in world coordinates. */

  private void renderObject(ObjectInfo obj, Vec3 orig, Vec3 viewdir, Mat4 toLocal, RasterContext context, Thread mainThread)
  {
    RenderingMesh mesh;
    Object3D theObject;
    double tol;
    int i;

    if (mainThread != renderThread)
      return;
    if (!obj.isVisible())
      return;
    theObject = obj.getObject();
    if (context.camera.visibility(obj.getBounds()) == Camera.NOT_VISIBLE)
      return;
    while (theObject instanceof ObjectWrapper)
      theObject = ((ObjectWrapper) theObject).getWrappedObject();
    if (theObject instanceof ObjectCollection)
      {
        Enumeration objects = ((ObjectCollection) theObject).getObjects(obj, false, theScene);
        Mat4 fromLocal = context.camera.getObjectToWorld();
        while (objects.hasMoreElements())
          {
            ObjectInfo elem = (ObjectInfo) objects.nextElement();
            CoordinateSystem coords = elem.getCoords().duplicate();
            coords.transformCoordinates(fromLocal);
            context.camera.setObjectTransform(coords.fromLocal());
            renderObject(elem, orig, viewdir, coords.toLocal(), context, mainThread);
          }
        return;
      }
    if (adaptive)
      {
        double dist = obj.getBounds().distanceToPoint(toLocal.times(orig));
        double distToScreen = context.camera.getDistToScreen();
        if (dist < distToScreen)
          tol = surfaceError;
        else
          tol = surfaceError*dist/distToScreen;
      }
    else
      tol = surfaceError;
    mesh = (isPreview ? obj.getPreviewMesh() : obj.getRenderingMesh(tol));
    if (mesh == null)
      return;
    if (mainThread != renderThread)
      return;
    viewdir = toLocal.timesDirection(viewdir);
    if (context.lightPosition == null)
    {
      context.lightPosition = new Vec3 [light.length];
      context.lightDirection = new Vec3 [light.length];
    }
    for (i = light.length-1; i >= 0; i--)
    {
      context.lightPosition[i] = toLocal.times(light[i].getCoords().getOrigin());
      if (!(light[i].getObject() instanceof PointLight))
        context.lightDirection[i] = toLocal.timesDirection(light[i].getCoords().getZDirection());
    }
    boolean bumpMap = theObject.getTexture().hasComponent(Texture.BUMP_COMPONENT);
    boolean cullBackfaces = (hideBackfaces && theObject.isClosed() && !theObject.getTexture().hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT));
    ObjectMaterialInfo material = null;
    if (theObject.getMaterialMapping() != null)
      material = new ObjectMaterialInfo(theObject.getMaterialMapping(), toLocal);
    if (theObject.getTexture().hasComponent(Texture.DISPLACEMENT_COMPONENT))
      renderMeshDisplaced(mesh, viewdir, tol, cullBackfaces, bumpMap, material, context);
    else if (shadingMode == GOURAUD)
      renderMeshGouraud(mesh, viewdir, cullBackfaces, material, context);
    else if (shadingMode == HYBRID && !bumpMap)
      renderMeshHybrid(mesh, viewdir, cullBackfaces, material, context);
    else
      renderMeshPhong(mesh, viewdir, cullBackfaces, bumpMap, material, context);
  }

  /** Calculate the lighting model at a point on a surface.  If diffuse, specular, or highlight
     is null, that component will not be calculated. */

  private void calcLight(Vec3 pos, Vec3 norm, Vec3 viewdir, Vec3 faceNorm, double roughness, RGBColor diffuse, RGBColor specular, RGBColor highlight, RasterContext context)
  {
    Vec3 reflectDir = context.tempVec[0], lightDir = context.tempVec[1];
    double viewDot = viewdir.dot(norm), faceDot = viewdir.dot(faceNorm);
    RGBColor outputColor = context.tempColor[0];

    if (diffuse != null)
      diffuse.copy(ambColor);
    if (highlight != null)
      highlight.setRGB(0.0f, 0.0f, 0.0f);
    if (specular != null)
      {
        if (envMode == Scene.ENVIRON_SOLID)
          specular.copy(envColor);
        else
          {
            // Find the reflection direction and add in the environment color.

            reflectDir.set(norm);
            reflectDir.scale(-2.0*viewDot);
            reflectDir.add(viewdir);
            context.camera.getViewToWorld().transformDirection(reflectDir);
            envMapping.getTextureSpec(reflectDir, context.surfSpec2, 1.0, smoothScale, time, envParamValue);
            if (envMode == Scene.ENVIRON_DIFFUSE)
              specular.copy(context.surfSpec2.diffuse);
            else
              specular.copy(context.surfSpec2.emissive);
          }
      }

    // Prevent artifacts where the triangle is facing toward the viewer, but the local
    // interpolated normal is facing away.

    if (viewDot < 0.0 && faceDot > 0.0)
      viewDot = TOL;
    else if (viewDot > 0.0 && faceDot < 0.0)
      viewDot = -TOL;

    // Loop over the lights and add in each one.

    for (int i = light.length-1; i >= 0; i--)
      {
        Light lt = (Light) light[i].getObject();
        Vec3 lightPos = context.lightPosition[i];
        double distToLight, lightDot;
        if (lt instanceof PointLight)
          {
            lightDir.set(pos);
            lightDir.subtract(lightPos);
            distToLight = lightDir.length();
            lightDir.scale(1.0/distToLight);
          }
        else if (lt instanceof SpotLight)
          {
            lightDir.set(pos);
            lightDir.subtract(lightPos);
            distToLight = lightDir.length();
            lightDir.scale(1.0/distToLight);
          }
        else if (lt instanceof DirectionalLight)
          lightDir.set(context.lightDirection[i]);
        lt.getLight(outputColor, light[i].getCoords().toLocal().times(pos));
        if (lt.getType() == Light.TYPE_AMBIENT)
          {
            if (diffuse != null)
              diffuse.add(outputColor.getRed(), outputColor.getGreen(), outputColor.getBlue());
            continue;
          }
        lightDot = lightDir.dot(norm);
        if ((lightDot >= 0.0 && viewDot <= 0.0) || (lightDot <= 0.0 && viewDot >= 0.0))
          continue;
        if (diffuse != null)
          {
            float dot = (float) (lightDot < 0.0 ? -lightDot : lightDot);
            diffuse.add(outputColor.getRed()*dot, outputColor.getGreen()*dot, outputColor.getBlue()*dot);
          }
        if (highlight != null)
          {
            lightDir.add(viewdir);
            lightDir.normalize();
            double dot = lightDir.dot(norm);
            dot = (dot < 0.0 ? -dot : dot);
            outputColor.scale(FastMath.pow(dot, (int) ((1.0-roughness)*128.0)+1));
            highlight.add(outputColor);
          }
      }
  }

  /**
   * Create a Fragment object.
   *
   * @param addColor   the additive color in ERGB format
   * @param multColor  the multiplicative color in ERGB format
   * @param depth      the depth of the fragment
   * @param material   a description of the material for the object being rendered
   * @param isBackface true if this triangle faces away from the camera
   */

  private Fragment createFragment(int addColor, int multColor, float depth, ObjectMaterialInfo material, boolean isBackface)
  {
    if (multColor == 0)
    {
      // It is fully opaque.

      return new OpaqueFragment(addColor, depth);
    }
    else if (addColor == 0 && multColor == WHITE_ERGB && material == null)
      return null; // This is a fully transparent fragment, so we can just discard it.
    else
    {
      if (material == null)
        return new TransparentFragment(addColor, multColor, depth, BACKGROUND_FRAGMENT);
      return new MaterialFragment(addColor, multColor, depth, BACKGROUND_FRAGMENT, material, !isBackface);
    }
  }

  /**
   * Record a row of Fragment objects into the buffer.
   *
   * @param row      the index of the row
   * @param xstart   the starting position along the row
   * @param xend     the ending position along the row
   * @param context  the RasterContext from which to copy the Fragments
   */

  private void recordRow(int row, int xstart, int xend, RasterContext context)
  {
    Fragment source[] = context.fragment;
    int indexBase = row*width;

    synchronized (lock[row])
    {
      for (int x = xstart; x < xend; x++)
      {
        Fragment f = source[x];
        if (f == null)
          continue;
        int index = indexBase+x;
        Fragment current = fragment[index];
        if (f.getDepth() < current.getDepth())
          fragment[index] = f.insertNextFragment(current);
        else
          fragment[index] = current.insertNextFragment(f);
      }
    }
  }

  /** Clip a triangle to the region in front of the z clipping plane. */

  private Vec3 [] clipTriangle(Vec3 v1, Vec3 v2, Vec3 v3, float z1, float z2, float z3, float newz[], double newu[], double newv[], RasterContext context)
  {
    double clip = context.camera.getClipDistance();
    boolean c1 = z1 < clip, c2 = z2 < clip, c3 = z3 < clip;
    Vec3 u1, u2, u3, u4;
    int clipCount = 0;

    if (c1) clipCount++;
    if (c2) clipCount++;
    if (c3) clipCount++;
    if (clipCount == 2)
      {
        // Two vertices need to be clipped.

        if (!c1)
          {
            u1 = v1;
            newz[0] = z1;
            newu[0] = 1.0;
            newv[0] = 0.0;
            double f2 = (z1-clip)/(z1-z2), f1 = 1.0-f2;
            u2 = new Vec3(f1*v1.x+f2*v2.x, f1*v1.y+f2*v2.y, f1*v1.z+f2*v2.z);
            newz[1] = (float) (f1*z1 + f2*z2);
            newu[1] = f1;
            newv[1] = f2;
            f2 = (z1-clip)/(z1-z3);
            f1 = 1.0-f2;
            u3 = new Vec3(f1*v1.x+f2*v3.x, f1*v1.y+f2*v3.y, f1*v1.z+f2*v3.z);
            newz[2] = (float) (f1*z1 + f2*z3);
            newu[2] = f1;
            newv[2] = 0.0;
          }
        else if (!c2)
          {
            u2 = v2;
            newz[1] = z2;
            newu[1] = 0.0;
            newv[1] = 1.0;
            double f2 = (z2-clip)/(z2-z3), f1 = 1.0-f2;
            u3 = new Vec3(f1*v2.x+f2*v3.x, f1*v2.y+f2*v3.y, f1*v2.z+f2*v3.z);
            newz[2] = (float) (f1*z2 + f2*z3);
            newu[2] = 0.0;
            newv[2] = f1;
            f2 = (z2-clip)/(z2-z1);
            f1 = 1.0-f2;
            u1 = new Vec3(f1*v2.x+f2*v1.x, f1*v2.y+f2*v1.y, f1*v2.z+f2*v1.z);
            newz[0] = (float) (f1*z2 + f2*z1);
            newu[0] = f2;
            newv[0] = f1;
          }
        else
          {
            u3 = v3;
            newz[2] = z3;
            newu[2] = 0.0;
            newv[2] = 0.0;
            double f2 = (z3-clip)/(z3-z1), f1 = 1.0-f2;
            u1 = new Vec3(f1*v3.x+f2*v1.x, f1*v3.y+f2*v1.y, f1*v3.z+f2*v1.z);
            newz[0] = (float) (f1*z3 + f2*z1);
            newu[0] = f2;
            newv[0] = 0.0;
            f2 = (z3-clip)/(z3-z2);
            f1 = 1.0-f2;
            u2 = new Vec3(f1*v3.x+f2*v2.x, f1*v3.y+f2*v2.y, f1*v3.z+f2*v2.z);
            newz[1] = (float) (f1*z3 + f2*z2);
            newu[1] = 0.0;
            newv[1] = f2;
          }
        return new Vec3 [] {u1, u2, u3};
      }

    // Only one vertex needs to be clipped, resulting in a quad.

    if (c1)
      {
        u1 = v2;
        newz[0] = z2;
        newu[0] = 0.0;
        newv[0] = 1.0;
        u2 = v3;
        newz[1] = z3;
        newu[1] = 0.0;
        newv[1] = 0.0;
        double f1 = (z2-clip)/(z2-z1), f2 = 1.0-f1;
        u3 = new Vec3(f1*v1.x+f2*v2.x, f1*v1.y+f2*v2.y, f1*v1.z+f2*v2.z);
        newz[2] = (float) (f1*z1 + f2*z2);
        newu[2] = f1;
        newv[2] = f2;
        f1 = (z3-clip)/(z3-z1);
        f2 = 1.0-f1;
        u4 = new Vec3(f1*v1.x+f2*v3.x, f1*v1.y+f2*v3.y, f1*v1.z+f2*v3.z);
        newz[3] = (float) (f1*z1 + f2*z3);
        newu[3] = f1;
        newv[3] = 0.0;
      }
    else if (c2)
      {
        u1 = v3;
        newz[0] = z3;
        newu[0] = 0.0;
        newv[0] = 0.0;
        u2 = v1;
        newz[1] = z1;
        newu[1] = 1.0;
        newv[1] = 0.0;
        double f1 = (z3-clip)/(z3-z2), f2 = 1.0-f1;
        u3 = new Vec3(f1*v2.x+f2*v3.x, f1*v2.y+f2*v3.y, f1*v2.z+f2*v3.z);
        newz[2] = (float) (f1*z2 + f2*z3);
        newu[2] = 0.0;
        newv[2] = f1;
        f1 = (z1-clip)/(z1-z2);
        f2 = 1.0-f1;
        u4 = new Vec3(f1*v2.x+f2*v1.x, f1*v2.y+f2*v1.y, f1*v2.z+f2*v1.z);
        newz[3] = (float) (f1*z2 + f2*z1);
        newu[3] = f2;
        newv[3] = f1;
      }
    else
      {
        u1 = v1;
        newz[0] = z1;
        newu[0] = 1.0;
        newv[0] = 0.0;
        u2 = v2;
        newz[1] = z2;
        newu[1] = 0.0;
        newv[1] = 1.0;
        double f1 = (z1-clip)/(z1-z3), f2 = 1.0-f1;
        u3 = new Vec3(f1*v3.x+f2*v1.x, f1*v3.y+f2*v1.y, f1*v3.z+f2*v1.z);
        newz[2] = (float) (f1*z3 + f2*z1);
        newu[2] = f2;
        newv[2] = 0.0;
        f1 = (z2-clip)/(z2-z3);
        f2 = 1.0-f1;
        u4 = new Vec3(f1*v3.x+f2*v2.x, f1*v3.y+f2*v2.y, f1*v3.z+f2*v2.z);
        newz[3] = (float) (f1*z3 + f2*z2);
        newu[3] = 0.0;
        newv[3] = f2;
      }
    return new Vec3 [] {u1, u2, u3, u4};
  }

  /** Render a triangle mesh with Gouraud shading. */

  private void renderMeshGouraud(RenderingMesh mesh, Vec3 viewdir, boolean cullBackfaces, ObjectMaterialInfo material, RasterContext context)
  {
    Vec3 vert[] = mesh.vert, norm[] = mesh.norm;
    Vec2 pos[] = new Vec2 [vert.length];
    float z[] = new float [vert.length], clip = (float) context.camera.getClipDistance(), clipz[] = new float [4];
    double clipu[] = new double [4], clipv[] = new double [4];
    double distToScreen = context.camera.getDistToScreen(), tol = smoothScale;
    RGBColor diffuse[] = new RGBColor [4], specular[] = new RGBColor [4], highlight[] = new RGBColor [4];
    Mat4 toView = context.camera.getObjectToView(), toScreen = context.camera.getObjectToScreen();
    RenderingTriangle tri;
    int i, v1, v2, v3, n1, n2, n3;
    boolean backface;

    for (i = 0; i < 4; i++)
      {
        diffuse[i] = new RGBColor();
        specular[i] = new RGBColor();
        highlight[i] = new RGBColor();
      }
    for (i = vert.length-1; i >= 0; i--)
      {
        pos[i] = toScreen.timesXY(vert[i]);
        z[i] = (float) toView.timesZ(vert[i]);
      }
    for (i = mesh.triangle.length-1; i >= 0; i--)
      {
        tri = mesh.triangle[i];
        v1 = tri.v1;
        v2 = tri.v2;
        v3 = tri.v3;
        n1 = tri.n1;
        n2 = tri.n2;
        n3 = tri.n3;
        if (z[v1] < clip && z[v2] < clip && z[v3] < clip)
          continue;
        backface = ((pos[v2].x-pos[v1].x)*(pos[v3].y-pos[v1].y) - (pos[v2].y-pos[v1].y)*(pos[v3].x-pos[v1].x) > 0.0);
        double viewdot = viewdir.dot(mesh.faceNorm[i]);
        if (z[v1] < clip || z[v2] < clip || z[v3] < clip)
          {
            Vec3 clipPos[] = clipTriangle(vert[v1], vert[v2], vert[v3], z[v1], z[v2], z[v3], clipz, clipu, clipv, context);
            Vec2 clipPos2D[] = new Vec2 [clipPos.length];
            for (int j = clipPos.length-1; j >= 0; j--)
              {
                clipPos2D[j] = toScreen.timesXY(clipPos[j]);
                double u = clipu[j], v = clipv[j], w = 1.0-u-v;
                tri.getTextureSpec(context.surfSpec, viewdot, u, v, 1.0-u-v, tol, time);
                context.tempVec[2].set(norm[n1].x*u + norm[n2].x*v + norm[n3].x*w, norm[n1].y*u + norm[n2].y*v + norm[n3].y*w, norm[n1].z*u + norm[n2].z*v + norm[n3].z*w);
                context.tempVec[2].normalize();
                calcLight(clipPos[j], context.tempVec[2], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[j], specular[j], highlight[j], context);
                specular[j].add(highlight[j]);
              }
            renderTriangleGouraud(clipPos2D[0], clipz[0], clipu[0], clipv[0], diffuse[0], specular[0],
                clipPos2D[1], clipz[1], clipu[1], clipv[1], diffuse[1], specular[1],
                clipPos2D[2], clipz[2], clipu[2], clipv[2], diffuse[2], specular[2],
                tri, clip, viewdot, backface, material, context);
            if (clipPos.length == 4)
              renderTriangleGouraud(clipPos2D[1], clipz[1], clipu[1], clipv[1], diffuse[1], specular[1],
                clipPos2D[2], clipz[2], clipu[2], clipv[2], diffuse[2], specular[2],
                clipPos2D[3], clipz[3], clipu[3], clipv[3], diffuse[3], specular[3],
                tri, clip, viewdot, backface, material, context);
          }
        else
          {
            if (cullBackfaces && backface)
              continue;
            if (z[v1] > distToScreen)
              tol = smoothScale*z[v1];
            tri.getTextureSpec(context.surfSpec, viewdot, 1.0, 0.0, 0.0, tol, time);
            calcLight(vert[v1], norm[n1], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[0], specular[0], highlight[0], context);
            specular[0].add(highlight[0]);
            if (z[v2] > distToScreen)
              tol = smoothScale*z[v2];
            tri.getTextureSpec(context.surfSpec, viewdot, 0.0, 1.0, 0.0, tol, time);
            calcLight(vert[v2], norm[n2], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[1], specular[1], highlight[1], context);
            specular[1].add(highlight[1]);
            if (z[v3] > distToScreen)
              tol = smoothScale*z[v3];
            tri.getTextureSpec(context.surfSpec, viewdot, 0.0, 0.0, 1.0, tol, time);
            calcLight(vert[v3], norm[n3], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[2], specular[2], highlight[2], context);
            specular[2].add(highlight[2]);
            renderTriangleGouraud(pos[v1], z[v1], 1.0, 0.0, diffuse[0], specular[0],
                pos[v2], z[v2], 0.0, 1.0, diffuse[1], specular[1],
                pos[v3], z[v3], 0.0, 0.0, diffuse[2], specular[2],
                tri, clip, viewdot, backface, material, context);
          }
      }
  }

  /** Render a triangle with Gouraud shading. */

  private void renderTriangleGouraud(Vec2 pos1, float zf1, double uf1, double vf1, RGBColor diffuse1, RGBColor specular1,
                                     Vec2 pos2, float zf2, double uf2, double vf2, RGBColor diffuse2, RGBColor specular2,
                                     Vec2 pos3, float zf3, double uf3, double vf3, RGBColor diffuse3, RGBColor specular3,
                                     RenderingTriangle tri, double clip, double viewdot, boolean isBackface, ObjectMaterialInfo material, RasterContext context)
  {
    double x1, x2, x3, y1, y2, y3;
    double dx1, dx2, dy1, dy2, mx1, mx2;
    double xstart, xend;
    float z1, z2, z3, dz1, dz2, mz1, mz2, zstart, zend, z, zl, dz;
    double u1, u2, u3, v1, v2, v3, du1, du2, dv1, dv2, mu1, mu2, mv1, mv2;
    double ustart, uend, vstart, vend, u, v, ul, vl, wl, du, dv;
    RGBColor dif1, dif2, dif3, spec1, spec2, spec3;
    float ddifred1, ddifred2, ddifgreen1, ddifgreen2, ddifblue1, ddifblue2;
    float mdifred1, mdifred2, mdifgreen1, mdifgreen2, mdifblue1, mdifblue2;
    float dspecred1, dspecred2, dspecgreen1, dspecgreen2, dspecblue1, dspecblue2;
    float mspecred1, mspecred2, mspecgreen1, mspecgreen2, mspecblue1, mspecblue2;
    float difredstart, difredend, difgreenstart, difgreenend, difbluestart, difblueend;
    float specredstart, specredend, specgreenstart, specgreenend, specbluestart, specblueend;
    float difred, difgreen, difblue, ddifred, ddifgreen, ddifblue;
    float specred, specgreen, specblue, dspecred, dspecgreen, dspecblue;
    float denom;
    int left, right, i, index, yend, y, lastAddColor = 0, lastMultColor = 0;
    boolean doSubsample = (subsample > 1), repeat;
    TextureSpec surfSpec = context.surfSpec;

    // Order the three vertices by y coordinate.

    if (pos1.y <= pos2.y && pos1.y <= pos3.y)
      {
        x1 = pos1.x;
        y1 = pos1.y;
        z1 = zf1;
        u1 = uf1;
        v1 = vf1;
        dif1 = diffuse1;
        spec1 = specular1;
        if (pos2.y < pos3.y)
          {
            x2 = pos2.x;
            y2 = pos2.y;
            z2 = zf2;
            u2 = uf2;
            v2 = vf2;
            dif2 = diffuse2;
            spec2 = specular2;
            x3 = pos3.x;
            y3 = pos3.y;
            z3 = zf3;
            u3 = uf3;
            v3 = vf3;
            dif3 = diffuse3;
            spec3 = specular3;
          }
        else
          {
            x2 = pos3.x;
            y2 = pos3.y;
            z2 = zf3;
            u2 = uf3;
            v2 = vf3;
            dif2 = diffuse3;
            spec2 = specular3;
            x3 = pos2.x;
            y3 = pos2.y;
            z3 = zf2;
            u3 = uf2;
            v3 = vf2;
            dif3 = diffuse2;
            spec3 = specular2;
          }
      }
    else if (pos2.y <= pos1.y && pos2.y <= pos3.y)
      {
        x1 = pos2.x;
        y1 = pos2.y;
        z1 = zf2;
        u1 = uf2;
        v1 = vf2;
        dif1 = diffuse2;
        spec1 = specular2;
        if (pos1.y < pos3.y)
          {
            x2 = pos1.x;
            y2 = pos1.y;
            z2 = zf1;
            u2 = uf1;
            v2 = vf1;
            dif2 = diffuse1;
            spec2 = specular1;
            x3 = pos3.x;
            y3 = pos3.y;
            z3 = zf3;
            u3 = uf3;
            v3 = vf3;
            dif3 = diffuse3;
            spec3 = specular3;
          }
        else
          {
            x2 = pos3.x;
            y2 = pos3.y;
            z2 = zf3;
            u2 = uf3;
            v2 = vf3;
            dif2 = diffuse3;
            spec2 = specular3;
            x3 = pos1.x;
            y3 = pos1.y;
            z3 = zf1;
            u3 = uf1;
            v3 = vf1;
            dif3 = diffuse1;
            spec3 = specular1;
          }
      }
    else
      {
        x1 = pos3.x;
        y1 = pos3.y;
        z1 = zf3;
        u1 = uf3;
        v1 = vf3;
        dif1 = diffuse3;
        spec1 = specular3;
        if (pos1.y < pos2.y)
          {
            x2 = pos1.x;
            y2 = pos1.y;
            z2 = zf1;
            u2 = uf1;
            v2 = vf1;
            dif2 = diffuse1;
            spec2 = specular1;
            x3 = pos2.x;
            y3 = pos2.y;
            z3 = zf2;
            u3 = uf2;
            v3 = vf2;
            dif3 = diffuse2;
            spec3 = specular2;
          }
        else
          {
            x2 = pos2.x;
            y2 = pos2.y;
            z2 = zf2;
            u2 = uf2;
            v2 = vf2;
            dif2 = diffuse2;
            spec2 = specular2;
            x3 = pos1.x;
            y3 = pos1.y;
            z3 = zf1;
            u3 = uf1;
            v3 = vf1;
            dif3 = diffuse1;
            spec3 = specular1;
          }
      }

    // Round the coordinates to the nearest pixel to avoid errors during rasterization.

    x1 = FastMath.round(x1);
    y1 = FastMath.round(y1);
    x2 = FastMath.round(x2);
    y2 = FastMath.round(y2);
    x3 = FastMath.round(x3);
    y3 = FastMath.round(y3);

    // Calculate intermediate variables.

    z1 = 1.0f/z1;
    u1 *= z1;
    v1 *= z1;
    z2 = 1.0f/z2;
    u2 *= z2;
    v2 *= z2;
    z3 = 1.0f/z3;
    u3 *= z3;
    v3 *= z3;
    dx1 = x3-x1;
    dy1 = y3-y1;
    dz1 = z3-z1;
    if (dy1 == 0)
      return;
    du1 = u3-u1;
    dv1 = v3-v1;
    ddifred1 = dif3.getRed()-dif1.getRed();
    ddifgreen1 = dif3.getGreen()-dif1.getGreen();
    ddifblue1 = dif3.getBlue()-dif1.getBlue();
    dspecred1 = spec3.getRed()-spec1.getRed();
    dspecgreen1 = spec3.getGreen()-spec1.getGreen();
    dspecblue1 = spec3.getBlue()-spec1.getBlue();
    dx2 = x2-x1;
    dy2 = y2-y1;
    dz2 = z2-z1;
    du2 = u2-u1;
    dv2 = v2-v1;
    ddifred2 = dif2.getRed()-dif1.getRed();
    ddifgreen2 = dif2.getGreen()-dif1.getGreen();
    ddifblue2 = dif2.getBlue()-dif1.getBlue();
    dspecred2 = spec2.getRed()-spec1.getRed();
    dspecgreen2 = spec2.getGreen()-spec1.getGreen();
    dspecblue2 = spec2.getBlue()-spec1.getBlue();
    denom = (float) (1.0/dy1);
    mx1 = dx1*denom;
    mz1 = dz1*denom;
    mu1 = du1*denom;
    mv1 = dv1*denom;
    mdifred1 = ddifred1*denom;
    mdifgreen1 = ddifgreen1*denom;
    mdifblue1 = ddifblue1*denom;
    mspecred1 = dspecred1*denom;
    mspecgreen1 = dspecgreen1*denom;
    mspecblue1 = dspecblue1*denom;
    xstart = xend = x1;
    zstart = zend = z1;
    ustart = uend = u1;
    vstart = vend = v1;
    difredstart = difredend = dif1.getRed();
    difgreenstart = difgreenend = dif1.getGreen();
    difbluestart = difblueend = dif1.getBlue();
    specredstart = specredend = spec1.getRed();
    specgreenstart = specgreenend = spec1.getGreen();
    specbluestart = specblueend = spec1.getBlue();
    y = FastMath.round(y1);
    if (dy2 > 0.0)
      {
        denom = (float) (1.0/dy2);
        mx2 = dx2*denom;
        mz2 = dz2*denom;
        mu2 = du2*denom;
        mv2 = dv2*denom;
        mdifred2 = ddifred2*denom;
        mdifgreen2 = ddifgreen2*denom;
        mdifblue2 = ddifblue2*denom;
        mspecred2 = dspecred2*denom;
        mspecgreen2 = dspecgreen2*denom;
        mspecblue2 = dspecblue2*denom;
        if (y2 < 0)
          {
            xstart += mx1*dy2;
            xend += mx2*dy2;
            zstart += mz1*dy2;
            zend += mz2*dy2;
            ustart += mu1*dy2;
            uend += mu2*dy2;
            vstart += mv1*dy2;
            vend += mv2*dy2;
            difredstart += mdifred1*dy2;
            difredend += mdifred2*dy2;
            difgreenstart += mdifgreen1*dy2;
            difgreenend += mdifgreen2*dy2;
            difbluestart += mdifblue1*dy2;
            difblueend += mdifblue2*dy2;
            specredstart += mspecred1*dy2;
            specredend += mspecred2*dy2;
            specgreenstart += mspecgreen1*dy2;
            specgreenend += mspecgreen2*dy2;
            specbluestart += mspecblue1*dy2;
            specblueend += mspecblue2*dy2;
            y = FastMath.round(y2);
          }
        else if (y < 0)
          {
            xstart -= mx1*y;
            xend -= mx2*y;
            zstart -= mz1*y;
            zend -= mz2*y;
            ustart -= mu1*y;
            uend -= mu2*y;
            vstart -= mv1*y;
            vend -= mv2*y;
            difredstart -= mdifred1*y;
            difredend -= mdifred2*y;
            difgreenstart -= mdifgreen1*y;
            difgreenend -= mdifgreen2*y;
            difbluestart -= mdifblue1*y;
            difblueend -= mdifblue2*y;
            specredstart -= mspecred1*y;
            specredend -= mspecred2*y;
            specgreenstart -= mspecgreen1*y;
            specgreenend -= mspecgreen2*y;
            specbluestart -= mspecblue1*y;
            specblueend -= mspecblue2*y;
            y = 0;
          }
        yend = FastMath.round(y2);
        if (yend > height)
          yend = height;
        index = y*width;

        // Rasterize the top half of the triangle,

        while (y < yend)
          {
            if (xstart < xend)
              {
                left = FastMath.round(xstart);
                right = FastMath.round(xend);
                z = zstart;
                dz = zend-zstart;
                u = ustart;
                du = uend-ustart;
                v = vstart;
                dv = vend-vstart;
                difred = difredstart;
                ddifred = difredend-difredstart;
                difgreen = difgreenstart;
                ddifgreen = difgreenend-difgreenstart;
                difblue = difbluestart;
                ddifblue = difblueend-difbluestart;
                specred = specredstart;
                dspecred = specredend-specredstart;
                specgreen = specgreenstart;
                dspecgreen = specgreenend-specgreenstart;
                specblue = specbluestart;
                dspecblue = specblueend-specbluestart;
              }
            else
              {
                left = FastMath.round(xend);
                right = FastMath.round(xstart);
                z = zend;
                dz = zstart-zend;
                u = uend;
                du = ustart-uend;
                v = vend;
                dv = vstart-vend;
                difred = difredend;
                ddifred = difredstart-difredend;
                difgreen = difgreenend;
                ddifgreen = difgreenstart-difgreenend;
                difblue = difblueend;
                ddifblue = difbluestart-difblueend;
                specred = specredend;
                dspecred = specredstart-specredend;
                specgreen = specgreenend;
                dspecgreen = specgreenstart-specgreenend;
                specblue = specblueend;
                dspecblue = specbluestart-specblueend;
              }
            if (left != right)
              {
                if (xend == xstart)
                  denom = 1.0f;
                else if (xend > xstart)
                  denom = (float) (1.0/(xend-xstart));
                else
                  denom = (float) (1.0/(xstart-xend));
                dz *= denom;
                du *= denom;
                dv *= denom;
                ddifred *= denom;
                ddifgreen *= denom;
                ddifblue *= denom;
                dspecred *= denom;
                dspecgreen *= denom;
                dspecblue *= denom;
                if (left < 0)
                {
                  z -= dz*left;
                  u -= du*left;
                  v -= dv*left;
                  difred -= ddifred*left;
                  difgreen -= ddifgreen*left;
                  difblue -= ddifblue*left;
                  specred -= dspecred*left;
                  specgreen -= dspecgreen*left;
                  specblue -= dspecblue*left;
                  left = 0;
                }
                if (right > width)
                  right = width;
                repeat = false;
                for (i = left; i < right; i++)
                  {
                    zl = 1.0f/z;
                    if (zl < fragment[index+i].getOpaqueDepth() && zl > clip)
                      {
                        if (!repeat || (i%subsample == 0))
                          {
                            ul = u*zl;
                            vl = v*zl;
                            wl = 1.0-ul-vl;
                            tri.getTextureSpec(surfSpec, viewdot, ul, vl, wl, smoothScale*z, time);
                            context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*difred + surfSpec.hilight.getRed()*specred + surfSpec.emissive.getRed(),
                              surfSpec.diffuse.getGreen()*difgreen + surfSpec.hilight.getGreen()*specgreen + surfSpec.emissive.getGreen(),
                              surfSpec.diffuse.getBlue()*difblue + surfSpec.hilight.getBlue()*specblue + surfSpec.emissive.getBlue());
                            lastAddColor = context.tempColor[0].getERGB();
                            lastMultColor = surfSpec.transparent.getERGB();
                          }
                        context.fragment[i] = createFragment(lastAddColor, lastMultColor, zl, material, isBackface);
                        repeat = doSubsample;
                      }
                    else
                    {
                      context.fragment[i] = null;
                      repeat = false;
                    }
                    z += dz;
                    u += du;
                    v += dv;
                    difred += ddifred;
                    difgreen += ddifgreen;
                    difblue += ddifblue;
                    specred += dspecred;
                    specgreen += dspecgreen;
                    specblue += dspecblue;
                  }
                recordRow(y, left, right, context);
              }
            xstart += mx1;
            zstart += mz1;
            ustart += mu1;
            vstart += mv1;
            difredstart += mdifred1;
            difgreenstart += mdifgreen1;
            difbluestart += mdifblue1;
            specredstart += mspecred1;
            specgreenstart += mspecgreen1;
            specbluestart += mspecblue1;
            xend += mx2;
            zend += mz2;
            uend += mu2;
            vend += mv2;
            difredend += mdifred2;
            difgreenend += mdifgreen2;
            difblueend += mdifblue2;
            specredend += mspecred2;
            specgreenend += mspecgreen2;
            specblueend += mspecblue2;
            index += width;
            y++;
          }
      }

    // Calculate intermediate variables for the bottom half of the triangle.

    dx2 = x3-x2;
    dy2 = y3-y2;
    dz2 = z3-z2;
    du2 = u3-u2;
    dv2 = v3-v2;
    ddifred2 = dif3.getRed()-dif2.getRed();
    ddifgreen2 = dif3.getGreen()-dif2.getGreen();
    ddifblue2 = dif3.getBlue()-dif2.getBlue();
    dspecred2 = spec3.getRed()-spec2.getRed();
    dspecgreen2 = spec3.getGreen()-spec2.getGreen();
    dspecblue2 = spec3.getBlue()-spec2.getBlue();
    if (dy2 > 0.0)
      {
        denom = (float) (1.0/dy2);
        mx2 = dx2*denom;
        mz2 = dz2*denom;
        mu2 = du2*denom;
        mv2 = dv2*denom;
        mdifred2 = ddifred2*denom;
        mdifgreen2 = ddifgreen2*denom;
        mdifblue2 = ddifblue2*denom;
        mspecred2 = dspecred2*denom;
        mspecgreen2 = dspecgreen2*denom;
        mspecblue2 = dspecblue2*denom;
        xend = x2;
        zend = z2;
        uend = u2;
        vend = v2;
        difredend = dif2.getRed();
        difgreenend = dif2.getGreen();
        difblueend = dif2.getBlue();
        specredend = spec2.getRed();
        specgreenend = spec2.getGreen();
        specblueend = spec2.getBlue();
        if (y < 0)
          {
            xstart -= mx1*y;
            xend -= mx2*y;
            zstart -= mz1*y;
            zend -= mz2*y;
            ustart -= mu1*y;
            uend -= mu2*y;
            vstart -= mv1*y;
            vend -= mv2*y;
            difredstart -= mdifred1*y;
            difredend -= mdifred2*y;
            difgreenstart -= mdifgreen1*y;
            difgreenend -= mdifgreen2*y;
            difbluestart -= mdifblue1*y;
            difblueend -= mdifblue2*y;
            specredstart -= mspecred1*y;
            specredend -= mspecred2*y;
            specgreenstart -= mspecgreen1*y;
            specgreenend -= mspecgreen2*y;
            specbluestart -= mspecblue1*y;
            specblueend -= mspecblue2*y;
            y = 0;
          }
        yend = FastMath.round(y3 < height ? y3 : height);
        index = y*width;

        // Rasterize the bottom half of the triangle,

        while (y < yend)
          {
            if (xstart < xend)
              {
                left = FastMath.round(xstart);
                right = FastMath.round(xend);
                z = zstart;
                dz = zend-zstart;
                u = ustart;
                du = uend-ustart;
                v = vstart;
                dv = vend-vstart;
                difred = difredstart;
                ddifred = difredend-difredstart;
                difgreen = difgreenstart;
                ddifgreen = difgreenend-difgreenstart;
                difblue = difbluestart;
                ddifblue = difblueend-difbluestart;
                specred = specredstart;
                dspecred = specredend-specredstart;
                specgreen = specgreenstart;
                dspecgreen = specgreenend-specgreenstart;
                specblue = specbluestart;
                dspecblue = specblueend-specbluestart;
              }
            else
              {
                left = FastMath.round(xend);
                right = FastMath.round(xstart);
                z = zend;
                dz = zstart-zend;
                u = uend;
                du = ustart-uend;
                v = vend;
                dv = vstart-vend;
                difred = difredend;
                ddifred = difredstart-difredend;
                difgreen = difgreenend;
                ddifgreen = difgreenstart-difgreenend;
                difblue = difblueend;
                ddifblue = difbluestart-difblueend;
                specred = specredend;
                dspecred = specredstart-specredend;
                specgreen = specgreenend;
                dspecgreen = specgreenstart-specgreenend;
                specblue = specblueend;
                dspecblue = specbluestart-specblueend;
              }
            if (left != right)
              {
                if (xend == xstart)
                  denom = 1.0f;
                else if (xend > xstart)
                  denom = (float) (1.0/(xend-xstart));
                else
                  denom = (float) (1.0/(xstart-xend));
                dz *= denom;
                du *= denom;
                dv *= denom;
                ddifred *= denom;
                ddifgreen *= denom;
                ddifblue *= denom;
                dspecred *= denom;
                dspecgreen *= denom;
                dspecblue *= denom;
                if (left < 0)
                {
                  z -= dz*left;
                  u -= du*left;
                  v -= dv*left;
                  difred -= ddifred*left;
                  difgreen -= ddifgreen*left;
                  difblue -= ddifblue*left;
                  specred -= dspecred*left;
                  specgreen -= dspecgreen*left;
                  specblue -= dspecblue*left;
                  left = 0;
                }
                if (right > width)
                  right = width;
                repeat = false;
                for (i = left; i < right; i++)
                  {
                    zl = 1.0f/z;
                    if (zl < fragment[index+i].getOpaqueDepth() && zl > clip)
                      {
                        if (!repeat || (i%subsample == 0))
                          {
                            ul = u*zl;
                            vl = v*zl;
                            wl = 1.0-ul-vl;
                            tri.getTextureSpec(surfSpec, viewdot, ul, vl, wl, smoothScale*z, time);
                            context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*difred + surfSpec.hilight.getRed()*specred + surfSpec.emissive.getRed(),
                              surfSpec.diffuse.getGreen()*difgreen + surfSpec.hilight.getGreen()*specgreen + surfSpec.emissive.getGreen(),
                              surfSpec.diffuse.getBlue()*difblue + surfSpec.hilight.getBlue()*specblue + surfSpec.emissive.getBlue());
                            lastAddColor = context.tempColor[0].getERGB();
                            lastMultColor = surfSpec.transparent.getERGB();
                          }
                        context.fragment[i] = createFragment(lastAddColor, lastMultColor, zl, material, isBackface);
                        repeat = doSubsample;
                      }
                    else
                    {
                      context.fragment[i] = null;
                      repeat = false;
                    }
                    z += dz;
                    u += du;
                    v += dv;
                    difred += ddifred;
                    difgreen += ddifgreen;
                    difblue += ddifblue;
                    specred += dspecred;
                    specgreen += dspecgreen;
                    specblue += dspecblue;
                  }
                recordRow(y, left, right, context);
              }
            xstart += mx1;
            zstart += mz1;
            ustart += mu1;
            vstart += mv1;
            difredstart += mdifred1;
            difgreenstart += mdifgreen1;
            difbluestart += mdifblue1;
            specredstart += mspecred1;
            specgreenstart += mspecgreen1;
            specbluestart += mspecblue1;
            xend += mx2;
            zend += mz2;
            uend += mu2;
            vend += mv2;
            difredend += mdifred2;
            difgreenend += mdifgreen2;
            difblueend += mdifblue2;
            specredend += mspecred2;
            specgreenend += mspecgreen2;
            specblueend += mspecblue2;
            index += width;
            y++;
          }
      }
  }

  /** Render a triangle mesh with hybrid Gouraud/Phong shading. */

  private void renderMeshHybrid(RenderingMesh mesh, Vec3 viewdir, boolean cullBackfaces, ObjectMaterialInfo material, RasterContext context)
  {
    Vec3 vert[] = mesh.vert, norm[] = mesh.norm, clipNorm[] = new Vec3 [4];
    Vec2 pos[] = new Vec2 [vert.length];
    float z[] = new float [vert.length], clip = (float) context.camera.getClipDistance(), clipz[] = new float [4];
    double clipu[] = new double [4], clipv[] = new double [4];
    double distToScreen = context.camera.getDistToScreen(), tol = smoothScale;
    RGBColor diffuse[] = new RGBColor [4];
    Mat4 toView = context.camera.getObjectToView(), toScreen = context.camera.getObjectToScreen();
    RenderingTriangle tri;
    int i, v1, v2, v3, n1, n2, n3;
    boolean backface;

    for (i = 0; i < 4; i++)
      {
        diffuse[i] = new RGBColor();
        clipNorm[i] = new Vec3();
      }
    for (i = vert.length-1; i >= 0; i--)
      {
        pos[i] = toScreen.timesXY(vert[i]);
        z[i] = (float) toView.timesZ(vert[i]);
      }
    for (i = mesh.triangle.length-1; i >= 0; i--)
      {
        tri = mesh.triangle[i];
        v1 = tri.v1;
        v2 = tri.v2;
        v3 = tri.v3;
        n1 = tri.n1;
        n2 = tri.n2;
        n3 = tri.n3;
        if (z[v1] < clip && z[v2] < clip && z[v3] < clip)
          continue;
        backface = ((pos[v2].x-pos[v1].x)*(pos[v3].y-pos[v1].y) - (pos[v2].y-pos[v1].y)*(pos[v3].x-pos[v1].x) > 0.0);
        double viewdot = viewdir.dot(mesh.faceNorm[i]);
        if (z[v1] < clip || z[v2] < clip || z[v3] < clip)
          {
            Vec3 clipPos[] = clipTriangle(vert[v1], vert[v2], vert[v3], z[v1], z[v2], z[v3], clipz, clipu, clipv, context);
            Vec2 clipPos2D[] = new Vec2 [clipPos.length];
            for (int j = clipPos.length-1; j >= 0; j--)
              {
                clipPos2D[j] = toScreen.timesXY(clipPos[j]);
                double u = clipu[j], v = clipv[j], w = 1.0-u-v;
                tri.getTextureSpec(context.surfSpec, viewdot, u, v, 1.0-u-v, tol, time);
                clipNorm[j].set(norm[n1].x*u + norm[n2].x*v + norm[n3].x*w, norm[n1].y*u + norm[n2].y*v + norm[n3].y*w, norm[n1].z*u + norm[n2].z*v + norm[n3].z*w);
                clipNorm[j].normalize();
                calcLight(clipPos[j], context.tempVec[2], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[j], null, null, context);
              }
            renderTriangleHybrid(clipPos2D[0], clipz[0], clipPos[0], clipNorm[0], clipu[0], clipv[0], diffuse[0],
                clipPos2D[1], clipz[1], clipPos[1], clipNorm[1], clipu[1], clipv[1], diffuse[1],
                clipPos2D[2], clipz[2], clipPos[2], clipNorm[2], clipu[2], clipv[2], diffuse[2],
                tri, viewdir, mesh.faceNorm[i], clip, viewdot, backface, material, context);
            if (clipPos.length == 4)
              renderTriangleHybrid(clipPos2D[1], clipz[1], clipPos[1], clipNorm[1], clipu[1], clipv[1], diffuse[1],
                clipPos2D[2], clipz[2], clipPos[2], clipNorm[2], clipu[2], clipv[2], diffuse[2],
                clipPos2D[3], clipz[3], clipPos[3], clipNorm[3], clipu[3], clipv[3], diffuse[3],
                tri, viewdir, mesh.faceNorm[i], clip, viewdot, backface, material, context);
          }
        else
          {
            if (cullBackfaces && backface)
              continue;
            if (z[v1] > distToScreen)
              tol = smoothScale*z[v1];
            tri.getTextureSpec(context.surfSpec, viewdot, 1.0, 0.0, 0.0, tol, time);
            calcLight(vert[v1], norm[n1], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[0], null, null, context);
            if (z[v2] > distToScreen)
              tol = smoothScale*z[v2];
            tri.getTextureSpec(context.surfSpec, viewdot, 0.0, 1.0, 0.0, tol, time);
            calcLight(vert[v2], norm[n2], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[1], null, null, context);
            if (z[v3] > distToScreen)
              tol = smoothScale*z[v3];
            tri.getTextureSpec(context.surfSpec, viewdot, 0.0, 0.0, 1.0, tol, time);
            calcLight(vert[v3], norm[n3], viewdir, mesh.faceNorm[i], context.surfSpec.roughness, diffuse[2], null, null, context);
            renderTriangleHybrid(pos[v1], z[v1], vert[v1], norm[n1], 1.0, 0.0, diffuse[0],
                pos[v2], z[v2], vert[v2], norm[n2], 0.0, 1.0, diffuse[1],
                pos[v3], z[v3], vert[v3], norm[n3], 0.0, 0.0, diffuse[2],
                tri, viewdir, mesh.faceNorm[i], clip, viewdot, backface, material, context);
          }
      }
  }

  /** Render a triangle with hybrid Gouraud/Phong shading. */

  private void renderTriangleHybrid(Vec2 pos1, float zf1, Vec3 vert1, Vec3 normf1, double uf1, double vf1, RGBColor diffuse1,
                                    Vec2 pos2, float zf2, Vec3 vert2, Vec3 normf2, double uf2, double vf2, RGBColor diffuse2,
                                    Vec2 pos3, float zf3, Vec3 vert3, Vec3 normf3, double uf3, double vf3, RGBColor diffuse3,
                                    RenderingTriangle tri, Vec3 viewdir, Vec3 faceNorm, double clip, double viewdot, boolean isBackface, ObjectMaterialInfo material, RasterContext context)
  {
    double x1, x2, x3, y1, y2, y3;
    double dx1, dx2, dy1, dy2, mx1, mx2;
    double xstart, xend;
    float z1, z2, z3, dz1, dz2, mz1, mz2, zstart, zend, z, zl, dz;
    double u1, u2, u3, v1, v2, v3, du1, du2, dv1, dv2, mu1, mu2, mv1, mv2;
    double ustart, uend, vstart, vend, u, v, ul, vl, wl, du, dv;
    RGBColor dif1, dif2, dif3, specular = context.tempColor[1], highlight = context.tempColor[2];
    Vec3 norm1, norm2, norm3;
    float ddifred1, ddifred2, ddifgreen1, ddifgreen2, ddifblue1, ddifblue2;
    float mdifred1, mdifred2, mdifgreen1, mdifgreen2, mdifblue1, mdifblue2;
    double dnormx1, dnormx2, dnormy1, dnormy2, dnormz1, dnormz2;
    double mnormx1, mnormx2, mnormy1, mnormy2, mnormz1, mnormz2;
    float difredstart, difredend, difgreenstart, difgreenend, difbluestart, difblueend;
    double normxstart, normxend, normystart, normyend, normzstart, normzend;
    float difred, difgreen, difblue, ddifred, ddifgreen, ddifblue;
    double normx, normy, normz, dnormx, dnormy, dnormz;
    float denom;
    int left, right, i, index, yend, y, lastAddColor = 0, lastMultColor = 0;
    boolean doSubsample = (subsample > 1), repeat;
    TextureSpec surfSpec = context.surfSpec;

    // Order the three vertices by y coordinate.

    if (pos1.y <= pos2.y && pos1.y <= pos3.y)
      {
        x1 = pos1.x;
        y1 = pos1.y;
        z1 = zf1;
        u1 = uf1;
        v1 = vf1;
        dif1 = diffuse1;
        norm1 = normf1;
        if (pos2.y < pos3.y)
          {
            x2 = pos2.x;
            y2 = pos2.y;
            z2 = zf2;
            u2 = uf2;
            v2 = vf2;
            dif2 = diffuse2;
            norm2 = normf2;
            x3 = pos3.x;
            y3 = pos3.y;
            z3 = zf3;
            u3 = uf3;
            v3 = vf3;
            dif3 = diffuse3;
            norm3 = normf3;
          }
        else
          {
            x2 = pos3.x;
            y2 = pos3.y;
            z2 = zf3;
            u2 = uf3;
            v2 = vf3;
            dif2 = diffuse3;
            norm2 = normf3;
            x3 = pos2.x;
            y3 = pos2.y;
            z3 = zf2;
            u3 = uf2;
            v3 = vf2;
            dif3 = diffuse2;
            norm3 = normf2;
          }
      }
    else if (pos2.y <= pos1.y && pos2.y <= pos3.y)
      {
        x1 = pos2.x;
        y1 = pos2.y;
        z1 = zf2;
        u1 = uf2;
        v1 = vf2;
        dif1 = diffuse2;
        norm1 = normf2;
        if (pos1.y < pos3.y)
          {
            x2 = pos1.x;
            y2 = pos1.y;
            z2 = zf1;
            u2 = uf1;
            v2 = vf1;
            dif2 = diffuse1;
            norm2 = normf1;
            x3 = pos3.x;
            y3 = pos3.y;
            z3 = zf3;
            u3 = uf3;
            v3 = vf3;
            dif3 = diffuse3;
            norm3 = normf3;
          }
        else
          {
            x2 = pos3.x;
            y2 = pos3.y;
            z2 = zf3;
            u2 = uf3;
            v2 = vf3;
            dif2 = diffuse3;
            norm2 = normf3;
            x3 = pos1.x;
            y3 = pos1.y;
            z3 = zf1;
            u3 = uf1;
            v3 = vf1;
            dif3 = diffuse1;
            norm3 = normf1;
          }
      }
    else
      {
        x1 = pos3.x;
        y1 = pos3.y;
        z1 = zf3;
        u1 = uf3;
        v1 = vf3;
        dif1 = diffuse3;
        norm1 = normf3;
        if (pos1.y < pos2.y)
          {
            x2 = pos1.x;
            y2 = pos1.y;
            z2 = zf1;
            u2 = uf1;
            v2 = vf1;
            dif2 = diffuse1;
            norm2 = normf1;
            x3 = pos2.x;
            y3 = pos2.y;
            z3 = zf2;
            u3 = uf2;
            v3 = vf2;
            dif3 = diffuse2;
            norm3 = normf2;
          }
        else
          {
            x2 = pos2.x;
            y2 = pos2.y;
            z2 = zf2;
            u2 = uf2;
            v2 = vf2;
            dif2 = diffuse2;
            norm2 = normf2;
            x3 = pos1.x;
            y3 = pos1.y;
            z3 = zf1;
            u3 = uf1;
            v3 = vf1;
            dif3 = diffuse1;
            norm3 = normf1;
          }
      }

    // Round the coordinates to the nearest pixel to avoid errors during rasterization.

    x1 = FastMath.round(x1);
    y1 = FastMath.round(y1);
    x2 = FastMath.round(x2);
    y2 = FastMath.round(y2);
    x3 = FastMath.round(x3);
    y3 = FastMath.round(y3);

    // Calculate intermediate variables.

    z1 = 1.0f/z1;
    u1 *= z1;
    v1 *= z1;
    z2 = 1.0f/z2;
    u2 *= z2;
    v2 *= z2;
    z3 = 1.0f/z3;
    u3 *= z3;
    v3 *= z3;
    dx1 = x3-x1;
    dy1 = y3-y1;
    dz1 = z3-z1;
    if (dy1 == 0)
      return;
    du1 = u3-u1;
    dv1 = v3-v1;
    ddifred1 = dif3.getRed()-dif1.getRed();
    ddifgreen1 = dif3.getGreen()-dif1.getGreen();
    ddifblue1 = dif3.getBlue()-dif1.getBlue();
    dnormx1 = norm3.x-norm1.x;
    dnormy1 = norm3.y-norm1.y;
    dnormz1 = norm3.z-norm1.z;
    dx2 = x2-x1;
    dy2 = y2-y1;
    dz2 = z2-z1;
    du2 = u2-u1;
    dv2 = v2-v1;
    ddifred2 = dif2.getRed()-dif1.getRed();
    ddifgreen2 = dif2.getGreen()-dif1.getGreen();
    ddifblue2 = dif2.getBlue()-dif1.getBlue();
    dnormx2 = norm2.x-norm1.x;
    dnormy2 = norm2.y-norm1.y;
    dnormz2 = norm2.z-norm1.z;
    denom = (float) (1.0/dy1);
    mx1 = dx1*denom;
    mz1 = dz1*denom;
    mu1 = du1*denom;
    mv1 = dv1*denom;
    mdifred1 = ddifred1*denom;
    mdifgreen1 = ddifgreen1*denom;
    mdifblue1 = ddifblue1*denom;
    mnormx1 = dnormx1*denom;
    mnormy1 = dnormy1*denom;
    mnormz1 = dnormz1*denom;
    xstart = xend = x1;
    zstart = zend = z1;
    ustart = uend = u1;
    vstart = vend = v1;
    difredstart = difredend = dif1.getRed();
    difgreenstart = difgreenend = dif1.getGreen();
    difbluestart = difblueend = dif1.getBlue();
    normxstart = normxend = norm1.x;
    normystart = normyend = norm1.y;
    normzstart = normzend = norm1.z;
    y = FastMath.round(y1);
    if (dy2 > 0.0)
      {
        denom = (float) (1.0/dy2);
        mx2 = dx2*denom;
        mz2 = dz2*denom;
        mu2 = du2*denom;
        mv2 = dv2*denom;
        mdifred2 = ddifred2*denom;
        mdifgreen2 = ddifgreen2*denom;
        mdifblue2 = ddifblue2*denom;
        mnormx2 = dnormx2*denom;
        mnormy2 = dnormy2*denom;
        mnormz2 = dnormz2*denom;
        if (y2 < 0)
          {
            xstart += mx1*dy2;
            xend += mx2*dy2;
            zstart += mz1*dy2;
            zend += mz2*dy2;
            ustart += mu1*dy2;
            uend += mu2*dy2;
            vstart += mv1*dy2;
            vend += mv2*dy2;
            difredstart += mdifred1*dy2;
            difredend += mdifred2*dy2;
            difgreenstart += mdifgreen1*dy2;
            difgreenend += mdifgreen2*dy2;
            difbluestart += mdifblue1*dy2;
            difblueend += mdifblue2*dy2;
            normxstart += mnormx1*dy2;
            normxend += mnormx2*dy2;
            normystart += mnormy1*dy2;
            normyend += mnormy2*dy2;
            normzstart += mnormz1*dy2;
            normzend += mnormz2*dy2;
            y = FastMath.round(y2);
          }
        else if (y < 0)
          {
            xstart -= mx1*y;
            xend -= mx2*y;
            zstart -= mz1*y;
            zend -= mz2*y;
            ustart -= mu1*y;
            uend -= mu2*y;
            vstart -= mv1*y;
            vend -= mv2*y;
            difredstart -= mdifred1*y;
            difredend -= mdifred2*y;
            difgreenstart -= mdifgreen1*y;
            difgreenend -= mdifgreen2*y;
            difbluestart -= mdifblue1*y;
            difblueend -= mdifblue2*y;
            normxstart -= mnormx1*y;
            normxend -= mnormx2*y;
            normystart -= mnormy1*y;
            normyend -= mnormy2*y;
            normzstart -= mnormz1*y;
            normzend -= mnormz2*y;
            y = 0;
          }
        yend = FastMath.round(y2);
        if (yend > height)
          yend = height;
        index = y*width;

        // Rasterize the top half of the triangle,

        while (y < yend)
          {
            if (xstart < xend)
              {
                left = FastMath.round(xstart);
                right = FastMath.round(xend);
                z = zstart;
                dz = zend-zstart;
                u = ustart;
                du = uend-ustart;
                v = vstart;
                dv = vend-vstart;
                difred = difredstart;
                ddifred = difredend-difredstart;
                difgreen = difgreenstart;
                ddifgreen = difgreenend-difgreenstart;
                difblue = difbluestart;
                ddifblue = difblueend-difbluestart;
                normx = normxstart;
                dnormx = normxend-normxstart;
                normy = normystart;
                dnormy = normyend-normystart;
                normz = normzstart;
                dnormz = normzend-normzstart;
              }
            else
              {
                left = FastMath.round(xend);
                right = FastMath.round(xstart);
                z = zend;
                dz = zstart-zend;
                u = uend;
                du = ustart-uend;
                v = vend;
                dv = vstart-vend;
                difred = difredend;
                ddifred = difredstart-difredend;
                difgreen = difgreenend;
                ddifgreen = difgreenstart-difgreenend;
                difblue = difblueend;
                ddifblue = difbluestart-difblueend;
                normx = normxend;
                dnormx = normxstart-normxend;
                normy = normyend;
                dnormy = normystart-normyend;
                normz = normzend;
                dnormz = normzstart-normzend;
              }
            if (left != right)
              {
                if (xend == xstart)
                  denom = 1.0f;
                else if (xend > xstart)
                  denom = (float) (1.0/(xend-xstart));
                else
                  denom = (float) (1.0/(xstart-xend));
                dz *= denom;
                du *= denom;
                dv *= denom;
                ddifred *= denom;
                ddifgreen *= denom;
                ddifblue *= denom;
                dnormx *= denom;
                dnormy *= denom;
                dnormz *= denom;
                if (left < 0)
                {
                  z -= dz*left;
                  u -= du*left;
                  v -= dv*left;
                  difred -= ddifred*left;
                  difgreen -= ddifgreen*left;
                  difblue -= ddifblue*left;
                  normx -= dnormx*left;
                  normy -= dnormy*left;
                  normz -= dnormz*left;
                  left = 0;
                }
                if (right > width)
                  right = width;
                repeat = false;
                for (i = left; i < right; i++)
                  {
                    zl = 1.0f/z;
                    if (zl < fragment[index+i].getOpaqueDepth() && zl > clip)
                      {
                        if (!repeat || (i%subsample == 0))
                          {
                            ul = u*zl;
                            vl = v*zl;
                            wl = 1.0-ul-vl;
                            tri.getTextureSpec(surfSpec, viewdot, ul, vl, wl, smoothScale*z, time);
                            if (surfSpec.hilight.getRed() == 0.0f && surfSpec.hilight.getGreen() == 0.0f && surfSpec.hilight.getBlue() == 0.0f &&
                                surfSpec.specular.getRed() == 0.0f && surfSpec.specular.getGreen() == 0.0f && surfSpec.specular.getBlue() == 0.0f)
                              context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*difred + surfSpec.emissive.getRed(),
                                surfSpec.diffuse.getGreen()*difgreen + surfSpec.emissive.getGreen(),
                                surfSpec.diffuse.getBlue()*difblue + surfSpec.emissive.getBlue());
                            else
                              {
                                if (positionNeeded)
                                  context.tempVec[2].set(ul*vert1.x+vl*vert2.x+wl*vert3.x, ul*vert1.y+vl*vert2.y+wl*vert3.y, ul*vert1.z+vl*vert2.z+wl*vert3.z);
                                context.tempVec[3].set(normx, normy, normz);
                                context.tempVec[3].normalize();
                                calcLight(context.tempVec[2], context.tempVec[3], viewdir, faceNorm, surfSpec.roughness, null, specular, highlight, context);
                                context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*difred + surfSpec.hilight.getRed()*highlight.getRed() + surfSpec.specular.getRed()*specular.getRed() + surfSpec.emissive.getRed(),
                                  surfSpec.diffuse.getGreen()*difgreen + surfSpec.hilight.getGreen()*highlight.getGreen() + surfSpec.specular.getGreen()*specular.getGreen() + surfSpec.emissive.getGreen(),
                                  surfSpec.diffuse.getBlue()*difblue + surfSpec.hilight.getBlue()*highlight.getBlue() + surfSpec.specular.getBlue()*specular.getBlue() + surfSpec.emissive.getBlue());
                              }
                            lastAddColor = context.tempColor[0].getERGB();
                            lastMultColor = surfSpec.transparent.getERGB();
                          }
                        context.fragment[i] = createFragment(lastAddColor, lastMultColor, zl, material, isBackface);
                        repeat = doSubsample;
                      }
                    else
                    {
                      context.fragment[i] = null;
                      repeat = false;
                    }
                    z += dz;
                    u += du;
                    v += dv;
                    difred += ddifred;
                    difgreen += ddifgreen;
                    difblue += ddifblue;
                    normx += dnormx;
                    normy += dnormy;
                    normz += dnormz;
                  }
                recordRow(y, left, right, context);
              }
            xstart += mx1;
            zstart += mz1;
            ustart += mu1;
            vstart += mv1;
            difredstart += mdifred1;
            difgreenstart += mdifgreen1;
            difbluestart += mdifblue1;
            normxstart += mnormx1;
            normystart += mnormy1;
            normzstart += mnormz1;
            xend += mx2;
            zend += mz2;
            uend += mu2;
            vend += mv2;
            difredend += mdifred2;
            difgreenend += mdifgreen2;
            difblueend += mdifblue2;
            normxend += mnormx2;
            normyend += mnormy2;
            normzend += mnormz2;
            index += width;
            y++;
          }
      }

    // Calculate intermediate variables for the bottom half of the triangle.

    dx2 = x3-x2;
    dy2 = y3-y2;
    dz2 = z3-z2;
    du2 = u3-u2;
    dv2 = v3-v2;
    ddifred2 = dif3.getRed()-dif2.getRed();
    ddifgreen2 = dif3.getGreen()-dif2.getGreen();
    ddifblue2 = dif3.getBlue()-dif2.getBlue();
    dnormx2 = norm3.x-norm2.x;
    dnormy2 = norm3.y-norm2.y;
    dnormz2 = norm3.z-norm2.z;
    if (dy2 > 0.0)
      {
        denom = (float) (1.0/dy2);
        mx2 = dx2*denom;
        mz2 = dz2*denom;
        mu2 = du2*denom;
        mv2 = dv2*denom;
        mdifred2 = ddifred2*denom;
        mdifgreen2 = ddifgreen2*denom;
        mdifblue2 = ddifblue2*denom;
        mnormx2 = dnormx2*denom;
        mnormy2 = dnormy2*denom;
        mnormz2 = dnormz2*denom;
        xend = x2;
        zend = z2;
        uend = u2;
        vend = v2;
        difredend = dif2.getRed();
        difgreenend = dif2.getGreen();
        difblueend = dif2.getBlue();
        normxend = norm2.x;
        normyend = norm2.y;
        normzend = norm2.z;
        if (y < 0)
          {
            xstart -= mx1*y;
            xend -= mx2*y;
            zstart -= mz1*y;
            zend -= mz2*y;
            ustart -= mu1*y;
            uend -= mu2*y;
            vstart -= mv1*y;
            vend -= mv2*y;
            difredstart -= mdifred1*y;
            difredend -= mdifred2*y;
            difgreenstart -= mdifgreen1*y;
            difgreenend -= mdifgreen2*y;
            difbluestart -= mdifblue1*y;
            difblueend -= mdifblue2*y;
            normxstart -= mnormx1*y;
            normxend -= mnormx2*y;
            normystart -= mnormy1*y;
            normyend -= mnormy2*y;
            normzstart -= mnormz1*y;
            normzend -= mnormz2*y;
            y = 0;
          }
        yend = FastMath.round(y3 < height ? y3 : height);
        index = y*width;

        // Rasterize the bottom half of the triangle,

        while (y < yend)
          {
            if (xstart < xend)
              {
                left = FastMath.round(xstart);
                right = FastMath.round(xend);
                z = zstart;
                dz = zend-zstart;
                u = ustart;
                du = uend-ustart;
                v = vstart;
                dv = vend-vstart;
                difred = difredstart;
                ddifred = difredend-difredstart;
                difgreen = difgreenstart;
                ddifgreen = difgreenend-difgreenstart;
                difblue = difbluestart;
                ddifblue = difblueend-difbluestart;
                normx = normxstart;
                dnormx = normxend-normxstart;
                normy = normystart;
                dnormy = normyend-normystart;
                normz = normzstart;
                dnormz = normzend-normzstart;
              }
            else
              {
                left = FastMath.round(xend);
                right = FastMath.round(xstart);
                z = zend;
                dz = zstart-zend;
                u = uend;
                du = ustart-uend;
                v = vend;
                dv = vstart-vend;
                difred = difredend;
                ddifred = difredstart-difredend;
                difgreen = difgreenend;
                ddifgreen = difgreenstart-difgreenend;
                difblue = difblueend;
                ddifblue = difbluestart-difblueend;
                normx = normxend;
                dnormx = normxstart-normxend;
                normy = normyend;
                dnormy = normystart-normyend;
                normz = normzend;
                dnormz = normzstart-normzend;
              }
            if (left != right)
              {
                if (xend == xstart)
                  denom = 1.0f;
                else if (xend > xstart)
                  denom = (float) (1.0/(xend-xstart));
                else
                  denom = (float) (1.0/(xstart-xend));
                dz *= denom;
                du *= denom;
                dv *= denom;
                ddifred *= denom;
                ddifgreen *= denom;
                ddifblue *= denom;
                dnormx *= denom;
                dnormy *= denom;
                dnormz *= denom;
                if (left < 0)
                {
                  z -= dz*left;
                  u -= du*left;
                  v -= dv*left;
                  difred -= ddifred*left;
                  difgreen -= ddifgreen*left;
                  difblue -= ddifblue*left;
                  normx -= dnormx*left;
                  normy -= dnormy*left;
                  normz -= dnormz*left;
                  left = 0;
                }
                if (right > width)
                  right = width;
                repeat = false;
                for (i = left; i < right; i++)
                  {
                    zl = 1.0f/z;
                    if (zl < fragment[index+i].getOpaqueDepth() && zl > clip)
                      {
                        if (!repeat || (i%subsample == 0))
                          {
                            ul = u*zl;
                            vl = v*zl;
                            wl = 1.0-ul-vl;
                            tri.getTextureSpec(surfSpec, viewdot, ul, vl, wl, smoothScale*z, time);
                            if (surfSpec.hilight.getRed() == 0.0f && surfSpec.hilight.getGreen() == 0.0f && surfSpec.hilight.getBlue() == 0.0f &&
                                surfSpec.specular.getRed() == 0.0f && surfSpec.specular.getGreen() == 0.0f && surfSpec.specular.getBlue() == 0.0f)
                              context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*difred + surfSpec.emissive.getRed(),
                                surfSpec.diffuse.getGreen()*difgreen + surfSpec.emissive.getGreen(),
                                surfSpec.diffuse.getBlue()*difblue + surfSpec.emissive.getBlue());
                            else
                              {
                                if (positionNeeded)
                                  context.tempVec[2].set(ul*vert1.x+vl*vert2.x+wl*vert3.x, ul*vert1.y+vl*vert2.y+wl*vert3.y, ul*vert1.z+vl*vert2.z+wl*vert3.z);
                                context.tempVec[3].set(normx, normy, normz);
                                context.tempVec[3].normalize();
                                calcLight(context.tempVec[2], context.tempVec[3], viewdir, faceNorm, surfSpec.roughness, null, specular, highlight, context);
                                context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*difred + surfSpec.hilight.getRed()*highlight.getRed() + surfSpec.specular.getRed()*specular.getRed() + surfSpec.emissive.getRed(),
                                  surfSpec.diffuse.getGreen()*difgreen + surfSpec.hilight.getGreen()*highlight.getGreen() + surfSpec.specular.getGreen()*specular.getGreen() + surfSpec.emissive.getGreen(),
                                  surfSpec.diffuse.getBlue()*difblue + surfSpec.hilight.getBlue()*highlight.getBlue() + surfSpec.specular.getBlue()*specular.getBlue() + surfSpec.emissive.getBlue());
                              }
                            lastAddColor = context.tempColor[0].getERGB();
                            lastMultColor = surfSpec.transparent.getERGB();
                          }
                        context.fragment[i] = createFragment(lastAddColor, lastMultColor, zl, material, isBackface);
                        repeat = doSubsample;
                      }
                    else
                    {
                      context.fragment[i] = null;
                      repeat = false;
                    }
                    z += dz;
                    u += du;
                    v += dv;
                    difred += ddifred;
                    difgreen += ddifgreen;
                    difblue += ddifblue;
                    normx += dnormx;
                    normy += dnormy;
                    normz += dnormz;
                  }
                recordRow(y, left, right, context);
              }
            xstart += mx1;
            zstart += mz1;
            ustart += mu1;
            vstart += mv1;
            difredstart += mdifred1;
            difgreenstart += mdifgreen1;
            difbluestart += mdifblue1;
            normxstart += mnormx1;
            normystart += mnormy1;
            normzstart += mnormz1;
            xend += mx2;
            zend += mz2;
            uend += mu2;
            vend += mv2;
            difredend += mdifred2;
            difgreenend += mdifgreen2;
            difblueend += mdifblue2;
            normxend += mnormx2;
            normyend += mnormy2;
            normzend += mnormz2;
            index += width;
            y++;
          }
      }
  }

  /** Render a triangle mesh with Phong shading. */

  private void renderMeshPhong(RenderingMesh mesh, Vec3 viewdir, boolean cullBackfaces, boolean bumpMap, ObjectMaterialInfo material, RasterContext context)
  {
    Vec3 vert[] = mesh.vert, norm[] = mesh.norm, clipNorm[] = new Vec3 [4];
    Vec2 pos[] = new Vec2 [vert.length];
    float z[] = new float [vert.length], clip = (float) context.camera.getClipDistance(), clipz[] = new float [4];
    double clipu[] = new double [4], clipv[] = new double [4];
    Mat4 toView = context.camera.getObjectToView(), toScreen = context.camera.getObjectToScreen();
    RenderingTriangle tri;
    int i, v1, v2, v3, n1, n2, n3;
    boolean backface;

    for (i = 0; i < 4; i++)
      clipNorm[i] = new Vec3();
    for (i = vert.length-1; i >= 0; i--)
      {
        pos[i] = toScreen.timesXY(vert[i]);
        z[i] = (float) toView.timesZ(vert[i]);
      }
    for (i = mesh.triangle.length-1; i >= 0; i--)
      {
        tri = mesh.triangle[i];
        v1 = tri.v1;
        v2 = tri.v2;
        v3 = tri.v3;
        n1 = tri.n1;
        n2 = tri.n2;
        n3 = tri.n3;
        if (z[v1] < clip && z[v2] < clip && z[v3] < clip)
          continue;
        backface = ((pos[v2].x-pos[v1].x)*(pos[v3].y-pos[v1].y) - (pos[v2].y-pos[v1].y)*(pos[v3].x-pos[v1].x) > 0.0);
        if (z[v1] < clip || z[v2] < clip || z[v3] < clip)
          {
            Vec3 clipPos[] = clipTriangle(vert[v1], vert[v2], vert[v3], z[v1], z[v2], z[v3], clipz, clipu, clipv, context);
            Vec2 clipPos2D[] = new Vec2 [clipPos.length];
            for (int j = clipPos.length-1; j >= 0; j--)
              {
                clipPos2D[j] = toScreen.timesXY(clipPos[j]);
                double u = clipu[j], v = clipv[j], w = 1.0-u-v;
                clipNorm[j].set(norm[n1].x*u + norm[n2].x*v + norm[n3].x*w, norm[n1].y*u + norm[n2].y*v + norm[n3].y*w, norm[n1].z*u + norm[n2].z*v + norm[n3].z*w);
                clipNorm[j].normalize();
              }
            renderTrianglePhong(clipPos2D[0], clipz[0], clipPos[0], clipNorm[0], clipu[0], clipv[0],
                clipPos2D[1], clipz[1], clipPos[1], clipNorm[1], clipu[1], clipv[1],
                clipPos2D[2], clipz[2], clipPos[2], clipNorm[2], clipu[2], clipv[2],
                tri, viewdir, mesh.faceNorm[i], clip, bumpMap, backface, material, context);
            if (clipPos.length == 4)
              renderTrianglePhong(clipPos2D[1], clipz[1], clipPos[1], clipNorm[1], clipu[1], clipv[1],
                clipPos2D[2], clipz[2], clipPos[2], clipNorm[2], clipu[2], clipv[2],
                clipPos2D[3], clipz[3], clipPos[3], clipNorm[3], clipu[3], clipv[3],
                tri, viewdir, mesh.faceNorm[i], clip, bumpMap, backface, material, context);
          }
        else
          {
            if (cullBackfaces && backface)
              continue;
            renderTrianglePhong(pos[v1], z[v1], vert[v1], norm[n1], 1.0, 0.0,
                pos[v2], z[v2], vert[v2], norm[n2], 0.0, 1.0,
                pos[v3], z[v3], vert[v3], norm[n3], 0.0, 0.0,
                tri, viewdir, mesh.faceNorm[i], clip, bumpMap, backface, material, context);
          }
      }
  }

  /** Render a triangle with Phong shading. */

  private void renderTrianglePhong(Vec2 pos1, float zf1, Vec3 vert1, Vec3 normf1, double uf1, double vf1,
                                   Vec2 pos2, float zf2, Vec3 vert2, Vec3 normf2, double uf2, double vf2,
                                   Vec2 pos3, float zf3, Vec3 vert3, Vec3 normf3, double uf3, double vf3,
                                   RenderingTriangle tri, Vec3 viewdir, Vec3 faceNorm, double clip, boolean bumpMap, boolean isBackface, ObjectMaterialInfo material, RasterContext context)
  {
    double x1, x2, x3, y1, y2, y3;
    double dx1, dx2, dy1, dy2, mx1, mx2;
    double xstart, xend;
    float z1, z2, z3, dz1, dz2, mz1, mz2, zstart, zend, z, zl, dz;
    double u1, u2, u3, v1, v2, v3, du1, du2, dv1, dv2, mu1, mu2, mv1, mv2;
    double ustart, uend, vstart, vend, u, v, ul, vl, wl, du, dv;
    RGBColor diffuse = context.tempColor[1], specular = context.tempColor[2], highlight = context.tempColor[3];
    Vec3 norm1, norm2, norm3, normal = context.tempVec[3];
    double dnormx1, dnormx2, dnormy1, dnormy2, dnormz1, dnormz2;
    double mnormx1, mnormx2, mnormy1, mnormy2, mnormz1, mnormz2;
    double normxstart, normxend, normystart, normyend, normzstart, normzend;
    double normx, normy, normz, dnormx, dnormy, dnormz;
    float denom;
    int left, right, i, index, yend, y, lastAddColor = 0, lastMultColor = 0;
    boolean doSubsample = (subsample > 1), repeat;
    TextureSpec surfSpec = context.surfSpec;

    // Order the three vertices by y coordinate.

    if (pos1.y <= pos2.y && pos1.y <= pos3.y)
      {
        x1 = pos1.x;
        y1 = pos1.y;
        z1 = zf1;
        u1 = uf1;
        v1 = vf1;
        norm1 = normf1;
        if (pos2.y < pos3.y)
          {
            x2 = pos2.x;
            y2 = pos2.y;
            z2 = zf2;
            u2 = uf2;
            v2 = vf2;
            norm2 = normf2;
            x3 = pos3.x;
            y3 = pos3.y;
            z3 = zf3;
            u3 = uf3;
            v3 = vf3;
            norm3 = normf3;
          }
        else
          {
            x2 = pos3.x;
            y2 = pos3.y;
            z2 = zf3;
            u2 = uf3;
            v2 = vf3;
            norm2 = normf3;
            x3 = pos2.x;
            y3 = pos2.y;
            z3 = zf2;
            u3 = uf2;
            v3 = vf2;
            norm3 = normf2;
          }
      }
    else if (pos2.y <= pos1.y && pos2.y <= pos3.y)
      {
        x1 = pos2.x;
        y1 = pos2.y;
        z1 = zf2;
        u1 = uf2;
        v1 = vf2;
        norm1 = normf2;
        if (pos1.y < pos3.y)
          {
            x2 = pos1.x;
            y2 = pos1.y;
            z2 = zf1;
            u2 = uf1;
            v2 = vf1;
            norm2 = normf1;
            x3 = pos3.x;
            y3 = pos3.y;
            z3 = zf3;
            u3 = uf3;
            v3 = vf3;
            norm3 = normf3;
          }
        else
          {
            x2 = pos3.x;
            y2 = pos3.y;
            z2 = zf3;
            u2 = uf3;
            v2 = vf3;
            norm2 = normf3;
            x3 = pos1.x;
            y3 = pos1.y;
            z3 = zf1;
            u3 = uf1;
            v3 = vf1;
            norm3 = normf1;
          }
      }
    else
      {
        x1 = pos3.x;
        y1 = pos3.y;
        z1 = zf3;
        u1 = uf3;
        v1 = vf3;
        norm1 = normf3;
        if (pos1.y < pos2.y)
          {
            x2 = pos1.x;
            y2 = pos1.y;
            z2 = zf1;
            u2 = uf1;
            v2 = vf1;
            norm2 = normf1;
            x3 = pos2.x;
            y3 = pos2.y;
            z3 = zf2;
            u3 = uf2;
            v3 = vf2;
            norm3 = normf2;
          }
        else
          {
            x2 = pos2.x;
            y2 = pos2.y;
            z2 = zf2;
            u2 = uf2;
            v2 = vf2;
            norm2 = normf2;
            x3 = pos1.x;
            y3 = pos1.y;
            z3 = zf1;
            u3 = uf1;
            v3 = vf1;
            norm3 = normf1;
          }
      }

    // Round the coordinates to the nearest pixel to avoid errors during rasterization.

    x1 = FastMath.round(x1);
    y1 = FastMath.round(y1);
    x2 = FastMath.round(x2);
    y2 = FastMath.round(y2);
    x3 = FastMath.round(x3);
    y3 = FastMath.round(y3);

    // Calculate intermediate variables.

    z1 = 1.0f/z1;
    u1 *= z1;
    v1 *= z1;
    z2 = 1.0f/z2;
    u2 *= z2;
    v2 *= z2;
    z3 = 1.0f/z3;
    u3 *= z3;
    v3 *= z3;
    dx1 = x3-x1;
    dy1 = y3-y1;
    dz1 = z3-z1;
    if (dy1 == 0)
      return;
    du1 = u3-u1;
    dv1 = v3-v1;
    dnormx1 = norm3.x-norm1.x;
    dnormy1 = norm3.y-norm1.y;
    dnormz1 = norm3.z-norm1.z;
    dx2 = x2-x1;
    dy2 = y2-y1;
    dz2 = z2-z1;
    du2 = u2-u1;
    dv2 = v2-v1;
    dnormx2 = norm2.x-norm1.x;
    dnormy2 = norm2.y-norm1.y;
    dnormz2 = norm2.z-norm1.z;
    denom = (float) (1.0/dy1);
    mx1 = dx1*denom;
    mz1 = dz1*denom;
    mu1 = du1*denom;
    mv1 = dv1*denom;
    mnormx1 = dnormx1*denom;
    mnormy1 = dnormy1*denom;
    mnormz1 = dnormz1*denom;
    xstart = xend = x1;
    zstart = zend = z1;
    ustart = uend = u1;
    vstart = vend = v1;
    normxstart = normxend = norm1.x;
    normystart = normyend = norm1.y;
    normzstart = normzend = norm1.z;
    y = FastMath.round(y1);
    if (dy2 > 0.0)
      {
        denom = (float) (1.0/dy2);
        mx2 = dx2*denom;
        mz2 = dz2*denom;
        mu2 = du2*denom;
        mv2 = dv2*denom;
        mnormx2 = dnormx2*denom;
        mnormy2 = dnormy2*denom;
        mnormz2 = dnormz2*denom;
        if (y2 < 0)
          {
            xstart += mx1*dy2;
            xend += mx2*dy2;
            zstart += mz1*dy2;
            zend += mz2*dy2;
            ustart += mu1*dy2;
            uend += mu2*dy2;
            vstart += mv1*dy2;
            vend += mv2*dy2;
            normxstart += mnormx1*dy2;
            normxend += mnormx2*dy2;
            normystart += mnormy1*dy2;
            normyend += mnormy2*dy2;
            normzstart += mnormz1*dy2;
            normzend += mnormz2*dy2;
            y = FastMath.round(y2);
          }
        else if (y < 0)
          {
            xstart -= mx1*y;
            xend -= mx2*y;
            zstart -= mz1*y;
            zend -= mz2*y;
            ustart -= mu1*y;
            uend -= mu2*y;
            vstart -= mv1*y;
            vend -= mv2*y;
            normxstart -= mnormx1*y;
            normxend -= mnormx2*y;
            normystart -= mnormy1*y;
            normyend -= mnormy2*y;
            normzstart -= mnormz1*y;
            normzend -= mnormz2*y;
            y = 0;
          }
        yend = FastMath.round(y2);
        if (yend > height)
          yend = height;
        index = y*width;

        // Rasterize the top half of the triangle,

        while (y < yend)
          {
            if (xstart < xend)
              {
                left = FastMath.round(xstart);
                right = FastMath.round(xend);
                z = zstart;
                dz = zend-zstart;
                u = ustart;
                du = uend-ustart;
                v = vstart;
                dv = vend-vstart;
                normx = normxstart;
                dnormx = normxend-normxstart;
                normy = normystart;
                dnormy = normyend-normystart;
                normz = normzstart;
                dnormz = normzend-normzstart;
              }
            else
              {
                left = FastMath.round(xend);
                right = FastMath.round(xstart);
                z = zend;
                dz = zstart-zend;
                u = uend;
                du = ustart-uend;
                v = vend;
                dv = vstart-vend;
                normx = normxend;
                dnormx = normxstart-normxend;
                normy = normyend;
                dnormy = normystart-normyend;
                normz = normzend;
                dnormz = normzstart-normzend;
              }
            if (left != right)
              {
                if (xend == xstart)
                  denom = 1.0f;
                else if (xend > xstart)
                  denom = (float) (1.0/(xend-xstart));
                else
                  denom = (float) (1.0/(xstart-xend));
                dz *= denom;
                du *= denom;
                dv *= denom;
                dnormx *= denom;
                dnormy *= denom;
                dnormz *= denom;
                if (left < 0)
                {
                  z -= dz*left;
                  u -= du*left;
                  v -= dv*left;
                  normx -= dnormx*left;
                  normy -= dnormy*left;
                  normz -= dnormz*left;
                  left = 0;
                }
                if (right > width)
                  right = width;
                repeat = false;
                for (i = left; i < right; i++)
                  {
                    zl = 1.0f/z;
                    if (zl < fragment[index+i].getOpaqueDepth() && zl > clip)
                      {
                        if (!repeat || (i%subsample == 0))
                          {
                            ul = u*zl;
                            vl = v*zl;
                            wl = 1.0-ul-vl;
                            if (positionNeeded)
                              context.tempVec[2].set(ul*vert1.x+vl*vert2.x+wl*vert3.x, ul*vert1.y+vl*vert2.y+wl*vert3.y, ul*vert1.z+vl*vert2.z+wl*vert3.z);
                            normal.set(normx, normy, normz);
                            normal.normalize();
                            tri.getTextureSpec(surfSpec, viewdir.dot(normal), ul, vl, wl, smoothScale*z, time);
                            if (bumpMap)
                              {
                                normal.scale(surfSpec.bumpGrad.dot(normal)+1.0);
                                normal.subtract(surfSpec.bumpGrad);
                                normal.normalize();
                              }
                            if (surfSpec.hilight.getRed() == 0.0f && surfSpec.hilight.getGreen() == 0.0f && surfSpec.hilight.getBlue() == 0.0f &&
                                surfSpec.specular.getRed() == 0.0f && surfSpec.specular.getGreen() == 0.0f && surfSpec.specular.getBlue() == 0.0f)
                              {
                                calcLight(context.tempVec[2], normal, viewdir, faceNorm, surfSpec.roughness, diffuse, null, null, context);
                                context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*diffuse.getRed() + surfSpec.emissive.getRed(),
                                  surfSpec.diffuse.getGreen()*diffuse.getGreen() + surfSpec.emissive.getGreen(),
                                  surfSpec.diffuse.getBlue()*diffuse.getBlue() + surfSpec.emissive.getBlue());
                              }
                            else
                              {
                                calcLight(context.tempVec[2], normal, viewdir, faceNorm, surfSpec.roughness, diffuse, specular, highlight, context);
                                context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*diffuse.getRed() + surfSpec.hilight.getRed()*highlight.getRed() + surfSpec.specular.getRed()*specular.getRed() + surfSpec.emissive.getRed(),
                                  surfSpec.diffuse.getGreen()*diffuse.getGreen() + surfSpec.hilight.getGreen()*highlight.getGreen() + surfSpec.specular.getGreen()*specular.getGreen() + surfSpec.emissive.getGreen(),
                                  surfSpec.diffuse.getBlue()*diffuse.getBlue() + surfSpec.hilight.getBlue()*highlight.getBlue() + surfSpec.specular.getBlue()*specular.getBlue() + surfSpec.emissive.getBlue());
                              }
                            lastAddColor = context.tempColor[0].getERGB();
                            lastMultColor = surfSpec.transparent.getERGB();
                          }
                        context.fragment[i] = createFragment(lastAddColor, lastMultColor, zl, material, isBackface);
                        repeat = doSubsample;
                      }
                    else
                    {
                      context.fragment[i] = null;
                      repeat = false;
                    }
                    z += dz;
                    u += du;
                    v += dv;
                    normx += dnormx;
                    normy += dnormy;
                    normz += dnormz;
                  }
                recordRow(y, left, right, context);
              }
            xstart += mx1;
            zstart += mz1;
            ustart += mu1;
            vstart += mv1;
            normxstart += mnormx1;
            normystart += mnormy1;
            normzstart += mnormz1;
            xend += mx2;
            zend += mz2;
            uend += mu2;
            vend += mv2;
            normxend += mnormx2;
            normyend += mnormy2;
            normzend += mnormz2;
            index += width;
            y++;
          }
      }

    // Calculate intermediate variables for the bottom half of the triangle.

    dx2 = x3-x2;
    dy2 = y3-y2;
    dz2 = z3-z2;
    du2 = u3-u2;
    dv2 = v3-v2;
    dnormx2 = norm3.x-norm2.x;
    dnormy2 = norm3.y-norm2.y;
    dnormz2 = norm3.z-norm2.z;
    if (dy2 > 0.0)
      {
        denom = (float) (1.0/dy2);
        mx2 = dx2*denom;
        mz2 = dz2*denom;
        mu2 = du2*denom;
        mv2 = dv2*denom;
        mnormx2 = dnormx2*denom;
        mnormy2 = dnormy2*denom;
        mnormz2 = dnormz2*denom;
        xend = x2;
        zend = z2;
        uend = u2;
        vend = v2;
        normxend = norm2.x;
        normyend = norm2.y;
        normzend = norm2.z;
        if (y < 0)
          {
            xstart -= mx1*y;
            xend -= mx2*y;
            zstart -= mz1*y;
            zend -= mz2*y;
            ustart -= mu1*y;
            uend -= mu2*y;
            vstart -= mv1*y;
            vend -= mv2*y;
            normxstart -= mnormx1*y;
            normxend -= mnormx2*y;
            normystart -= mnormy1*y;
            normyend -= mnormy2*y;
            normzstart -= mnormz1*y;
            normzend -= mnormz2*y;
            y = 0;
          }
        yend = FastMath.round(y3 < height ? y3 : height);
        index = y*width;

        // Rasterize the bottom half of the triangle,

        while (y < yend)
          {
            if (xstart < xend)
              {
                left = FastMath.round(xstart);
                right = FastMath.round(xend);
                z = zstart;
                dz = zend-zstart;
                u = ustart;
                du = uend-ustart;
                v = vstart;
                dv = vend-vstart;
                normx = normxstart;
                dnormx = normxend-normxstart;
                normy = normystart;
                dnormy = normyend-normystart;
                normz = normzstart;
                dnormz = normzend-normzstart;
              }
            else
              {
                left = FastMath.round(xend);
                right = FastMath.round(xstart);
                z = zend;
                dz = zstart-zend;
                u = uend;
                du = ustart-uend;
                v = vend;
                dv = vstart-vend;
                normx = normxend;
                dnormx = normxstart-normxend;
                normy = normyend;
                dnormy = normystart-normyend;
                normz = normzend;
                dnormz = normzstart-normzend;
              }
            if (left != right)
              {
                if (xend == xstart)
                  denom = 1.0f;
                else if (xend > xstart)
                  denom = (float) (1.0/(xend-xstart));
                else
                  denom = (float) (1.0/(xstart-xend));
                dz *= denom;
                du *= denom;
                dv *= denom;
                dnormx *= denom;
                dnormy *= denom;
                dnormz *= denom;
                if (left < 0)
                {
                  z -= dz*left;
                  u -= du*left;
                  v -= dv*left;
                  normx -= dnormx*left;
                  normy -= dnormy*left;
                  normz -= dnormz*left;
                  left = 0;
                }
                if (right > width)
                  right = width;
                repeat = false;
                for (i = left; i < right; i++)
                  {
                    zl = 1.0f/z;
                    if (zl < fragment[index+i].getOpaqueDepth() && zl > clip)
                      {
                        if (!repeat || (i%subsample == 0))
                          {
                            ul = u*zl;
                            vl = v*zl;
                            wl = 1.0-ul-vl;
                            if (positionNeeded)
                              context.tempVec[2].set(ul*vert1.x+vl*vert2.x+wl*vert3.x, ul*vert1.y+vl*vert2.y+wl*vert3.y, ul*vert1.z+vl*vert2.z+wl*vert3.z);
                            normal.set(normx, normy, normz);
                            normal.normalize();
                            tri.getTextureSpec(surfSpec, viewdir.dot(normal), ul, vl, wl, smoothScale*z, time);
                            if (bumpMap)
                              {
                                normal.scale(surfSpec.bumpGrad.dot(normal)+1.0);
                                normal.subtract(surfSpec.bumpGrad);
                                normal.normalize();
                              }
                            if (surfSpec.hilight.getRed() == 0.0f && surfSpec.hilight.getGreen() == 0.0f && surfSpec.hilight.getBlue() == 0.0f &&
                                surfSpec.specular.getRed() == 0.0f && surfSpec.specular.getGreen() == 0.0f && surfSpec.specular.getBlue() == 0.0f)
                              {
                                calcLight(context.tempVec[2], normal, viewdir, faceNorm, surfSpec.roughness, diffuse, null, null, context);
                                context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*diffuse.getRed() + surfSpec.emissive.getRed(),
                                  surfSpec.diffuse.getGreen()*diffuse.getGreen() + surfSpec.emissive.getGreen(),
                                  surfSpec.diffuse.getBlue()*diffuse.getBlue() + surfSpec.emissive.getBlue());
                              }
                            else
                              {
                                calcLight(context.tempVec[2], normal, viewdir, faceNorm, surfSpec.roughness, diffuse, specular, highlight, context);
                                context.tempColor[0].setRGB(surfSpec.diffuse.getRed()*diffuse.getRed() + surfSpec.hilight.getRed()*highlight.getRed() + surfSpec.specular.getRed()*specular.getRed() + surfSpec.emissive.getRed(),
                                  surfSpec.diffuse.getGreen()*diffuse.getGreen() + surfSpec.hilight.getGreen()*highlight.getGreen() + surfSpec.specular.getGreen()*specular.getGreen() + surfSpec.emissive.getGreen(),
                                  surfSpec.diffuse.getBlue()*diffuse.getBlue() + surfSpec.hilight.getBlue()*highlight.getBlue() + surfSpec.specular.getBlue()*specular.getBlue() + surfSpec.emissive.getBlue());
                              }
                            lastAddColor = context.tempColor[0].getERGB();
                            lastMultColor = surfSpec.transparent.getERGB();
                          }
                        context.fragment[i] = createFragment(lastAddColor, lastMultColor, zl, material, isBackface);
                        repeat = doSubsample;
                      }
                    else
                    {
                      context.fragment[i] = null;
                      repeat = false;
                    }
                    z += dz;
                    u += du;
                    v += dv;
                    normx += dnormx;
                    normy += dnormy;
                    normz += dnormz;
                  }
                recordRow(y, left, right, context);
              }
            xstart += mx1;
            zstart += mz1;
            ustart += mu1;
            vstart += mv1;
            normxstart += mnormx1;
            normystart += mnormy1;
            normzstart += mnormz1;
            xend += mx2;
            zend += mz2;
            uend += mu2;
            vend += mv2;
            normxend += mnormx2;
            normyend += mnormy2;
            normzend += mnormz2;
            index += width;
            y++;
          }
      }
  }

  /** Render a displacement mapped triangle mesh by recursively subdividing the triangles
     until they are sufficiently small. */

  private void renderMeshDisplaced(RenderingMesh mesh, Vec3 viewdir, double tol, boolean cullBackfaces, boolean bumpMap, ObjectMaterialInfo material, RasterContext context)
  {
    Vec3 vert[] = mesh.vert, norm[] = mesh.norm;
    Mat4 toView = context.camera.getObjectToView(), toScreen = context.camera.getObjectToScreen();
    int v1, v2, v3, n1, n2, n3;
    double dist1, dist2, dist3;
    RenderingTriangle tri;

    for (int i = mesh.triangle.length-1; i >= 0; i--)
      {
        tri = mesh.triangle[i];
        v1 = tri.v1;
        v2 = tri.v2;
        v3 = tri.v3;
        n1 = tri.n1;
        n2 = tri.n2;
        n3 = tri.n3;
        dist1 = vert[v1].distance(vert[v2]);
        dist2 = vert[v2].distance(vert[v3]);
        dist3 = vert[v3].distance(vert[v1]);

        // Calculate the gradient vectors for u and v.

        context.tempVec[0].set(vert[v1].x-vert[v3].x, vert[v1].y-vert[v3].y, vert[v1].z-vert[v3].z);
        context.tempVec[1].set(vert[v3].x-vert[v2].x, vert[v3].y-vert[v2].y, vert[v3].z-vert[v2].z);
        Vec3 vgrad = context.tempVec[0].cross(mesh.faceNorm[i]);
        Vec3 ugrad = context.tempVec[1].cross(mesh.faceNorm[i]);
        vgrad.scale(-1.0/vgrad.dot(context.tempVec[1]));
        ugrad.scale(1.0/ugrad.dot(context.tempVec[0]));
        DisplacedVertex dv1 = new DisplacedVertex(tri, vert[v1], norm[n1], 1.0, 0.0, toView, toScreen, context);
        DisplacedVertex dv2 = new DisplacedVertex(tri, vert[v2], norm[n2], 0.0, 1.0, toView, toScreen, context);
        DisplacedVertex dv3 = new DisplacedVertex(tri, vert[v3], norm[n3], 0.0, 0.0, toView, toScreen, context);
        renderDisplacedTriangle(tri, dv1, dist1, dv2, dist2, dv3, dist3, viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
      }
  }

  /** Render a displacement mapeed triangle by recursively subdividing it. */

  private void renderDisplacedTriangle(RenderingTriangle tri, DisplacedVertex dv1,
                                       double dist1, DisplacedVertex dv2, double dist2, DisplacedVertex dv3, double dist3,
                                       Vec3 viewdir, Vec3 ugrad, Vec3 vgrad, double tol, boolean cullBackfaces, boolean bumpMap,
                                       ObjectMaterialInfo material, RasterContext context)
  {
    Mat4 toView = context.camera.getObjectToView(), toScreen = context.camera.getObjectToScreen();
    DisplacedVertex midv1 = null, midv2 = null, midv3 = null;
    double halfdist1 = 0, halfdist2 = 0, halfdist3 = 0;
    boolean split1 = dist1 > tol, split2 = dist2 > tol, split3 = dist3 > tol;
    int shading = (bumpMap ? PHONG : shadingMode), count = 0;

    if (split1)
      {
        midv1 = new DisplacedVertex(tri, new Vec3(0.5*(dv1.vert.x+dv2.vert.x), 0.5*(dv1.vert.y+dv2.vert.y), 0.5*(dv1.vert.z+dv2.vert.z)),
          new Vec3(0.5*(dv1.norm.x+dv2.norm.x), 0.5*(dv1.norm.y+dv2.norm.y), 0.5*(dv1.norm.z+dv2.norm.z)),
          0.5*(dv1.u+dv2.u), 0.5*(dv1.v+dv2.v), toView, toScreen, context);
        halfdist1 = 0.5*dist1;
        count++;
      }
    if (split2)
      {
        midv2 = new DisplacedVertex(tri, new Vec3(0.5*(dv2.vert.x+dv3.vert.x), 0.5*(dv2.vert.y+dv3.vert.y), 0.5*(dv2.vert.z+dv3.vert.z)),
          new Vec3(0.5*(dv2.norm.x+dv3.norm.x), 0.5*(dv2.norm.y+dv3.norm.y), 0.5*(dv2.norm.z+dv3.norm.z)),
          0.5*(dv2.u+dv3.u), 0.5*(dv2.v+dv3.v), toView, toScreen, context);
        halfdist2 = 0.5*dist2;
        count++;
      }
    if (split3)
      {
        midv3 = new DisplacedVertex(tri, new Vec3(0.5*(dv3.vert.x+dv1.vert.x), 0.5*(dv3.vert.y+dv1.vert.y), 0.5*(dv3.vert.z+dv1.vert.z)),
          new Vec3(0.5*(dv3.norm.x+dv1.norm.x), 0.5*(dv3.norm.y+dv1.norm.y), 0.5*(dv3.norm.z+dv1.norm.z)),
          0.5*(dv3.u+dv1.u), 0.5*(dv3.v+dv1.v), toView, toScreen, context);
        halfdist3 = 0.5*dist3;
        count++;
      }

    // If any side is still too large, subdivide the triangle further.

    if (count == 1)
      {
        // Split it into two triangles.

        if (split1)
          {
            double d = dv3.vert.distance(midv1.vert);
            renderDisplacedTriangle(tri, dv1, halfdist1, midv1, d, dv3, dist3,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, midv1, halfdist1, dv2, dist2, dv3, d,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
          }
        else if (split2)
          {
            double d = dv1.vert.distance(midv2.vert);
            renderDisplacedTriangle(tri, dv2, halfdist2, midv2, d, dv1, dist1,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, midv2, halfdist2, dv3, dist3, dv1, d,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
          }
        else
          {
            double d = dv1.vert.distance(midv3.vert);
            renderDisplacedTriangle(tri, dv3, halfdist3, midv3, d, dv2, dist2,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, midv3, halfdist3, dv1, dist1, dv2, d,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
          }
        return;
      }
    if (count == 2)
      {
        // Split it into three triangles.

        if (!split1)
          {
            double d1 = midv2.vert.distance(dv1.vert), d2 = midv2.vert.distance(midv3.vert);
            renderDisplacedTriangle(tri, dv1, dist1, dv2, halfdist2, midv2, d1,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, dv1, d1, midv2, d2, midv3, halfdist3,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, dv3, halfdist3, midv3, d2, midv2, halfdist2,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
          }
        else if (!split2)
          {
            double d1 = midv3.vert.distance(dv2.vert), d2 = midv3.vert.distance(midv1.vert);
            renderDisplacedTriangle(tri, dv2, dist2, dv3, halfdist3, midv3, d1,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, dv2, d1, midv3, d2, midv1, halfdist1,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, dv1, halfdist1, midv1, d2, midv3, halfdist3,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
          }
        else
          {
            double d1 = midv1.vert.distance(dv3.vert), d2 = midv1.vert.distance(midv2.vert);
            renderDisplacedTriangle(tri, dv3, dist3, dv1, halfdist1, midv1, d1,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, dv3, d1, midv1, d2, midv2, halfdist2,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
            renderDisplacedTriangle(tri, dv2, halfdist2, midv2, d2, midv1, halfdist1,
                viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
          }
        return;
      }
    if (count == 3)
      {
        // Split it into four triangles.

        double d1 = midv1.vert.distance(midv2.vert), d2 = midv2.vert.distance(midv3.vert), d3 = midv3.vert.distance(midv1.vert);
        renderDisplacedTriangle(tri, dv1, halfdist1, midv1, d3, midv3, halfdist3,
            viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
        renderDisplacedTriangle(tri, dv2, halfdist2, midv2, d1, midv1, halfdist1,
            viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
        renderDisplacedTriangle(tri, dv3, halfdist3, midv3, d2, midv2, halfdist2,
            viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
        renderDisplacedTriangle(tri, midv1, d1, midv2, d2, midv3, d3,
            viewdir, ugrad, vgrad, tol, cullBackfaces, bumpMap, material, context);
        return;
      }

    // The triangle is small enough that it does not need to be split any more, so render it.

    float clip = (float) context.camera.getClipDistance();
    if (dv1.z < clip && dv2.z < clip && dv3.z < clip)
      return;
    if (dv1.z <= 0.0f || dv2.z < 0.0f || dv3.z < 0.0f)
      return;
    boolean backface = ((dv2.pos.x-dv1.pos.x)*(dv3.pos.y-dv1.pos.y) - (dv2.pos.y-dv1.pos.y)*(dv3.pos.x-dv1.pos.x) > 0.0);
    if (cullBackfaces && backface)
      return;
    if (dv1.dispnorm == null)
      dv1.prepareToRender(tri, viewdir, ugrad, vgrad, shading, context);
    if (dv2.dispnorm == null)
      dv2.prepareToRender(tri, viewdir, ugrad, vgrad, shading, context);
    if (dv3.dispnorm == null)
      dv3.prepareToRender(tri, viewdir, ugrad, vgrad, shading, context);
    Vec3 closestNorm;
    if (dv1.z < dv2.z && dv1.z < dv3.z)
      closestNorm = dv1.dispnorm;
    else if (dv2.z < dv1.z && dv2.z < dv3.z)
      closestNorm = dv2.dispnorm;
    else
      closestNorm = dv3.dispnorm;
    if (shading == GOURAUD)
      renderTriangleGouraud(dv1.pos, dv1.z, dv1.u, dv1.v, dv1.diffuse, dv1.specular,
        dv2.pos, dv2.z, dv2.u, dv2.v, dv2.diffuse, dv2.specular,
        dv3.pos, dv3.z, dv3.u, dv3.v, dv3.diffuse, dv3.specular,
        tri, clip, viewdir.dot(closestNorm), backface, material, context);
    else if (shading == HYBRID)
      renderTriangleHybrid(dv1.pos, dv1.z, dv1.dispvert, dv1.dispnorm, dv1.u, dv1.v, dv1.diffuse,
        dv2.pos, dv2.z, dv2.dispvert, dv2.dispnorm, dv2.u, dv2.v, dv2.diffuse,
        dv3.pos, dv3.z, dv3.dispvert, dv3.dispnorm, dv3.u, dv3.v, dv3.diffuse,
        tri, viewdir, closestNorm, clip, viewdir.dot(closestNorm), backface, material, context);
    else
      renderTrianglePhong(dv1.pos, dv1.z, dv1.dispvert, dv1.dispnorm, dv1.u, dv1.v,
        dv2.pos, dv2.z, dv2.dispvert, dv2.dispnorm, dv2.u, dv2.v,
        dv3.pos, dv3.z, dv3.dispvert, dv3.dispnorm, dv3.u, dv3.v,
        tri, viewdir, closestNorm, clip, bumpMap, backface, material, context);
  }

  /** This is an inner class for keeping track of information about vertices when 
     doing displacement mapping. */

  private class DisplacedVertex
  {
    public Vec3 vert, norm, dispvert, dispnorm;
    public Vec2 pos;
    public double u, v, disp, tol;
    public float z, basez;
    public RGBColor diffuse, specular, highlight;

    public DisplacedVertex(RenderingTriangle tri, Vec3 vert, Vec3 norm, double u, double v,
                           Mat4 toView, Mat4 toScreen, RasterContext context)
    {
      this.vert = vert;
      this.norm = norm;
      this.u = u;
      this.v = v;
      basez = (float) toView.timesZ(vert);
      tol = (basez > context.camera.getDistToScreen()) ? smoothScale*basez : smoothScale;
      disp = tri.getDisplacement(u, v, 1.0-u-v, tol, 0.0);
      dispvert = new Vec3(vert.x+disp*norm.x, vert.y+disp*norm.y, vert.z+disp*norm.z);
      z = (float) toView.timesZ(dispvert);
      pos = toScreen.timesXY(dispvert);
    }

    /** Calculate all the properties which are necessary for rendering this point. */

    public final void prepareToRender(RenderingTriangle tri, Vec3 viewdir, Vec3 ugrad, Vec3 vgrad, int shading, RasterContext context)
    {
      // Find the derivatives of the displacement map, and use them to find the 
      // local normal vector.

      double w = 1.0-u-v;
      double dhdu = (tri.getDisplacement(u+(1e-5), v, w-(1e-5), tol, 0.0)-disp)*1e5;
      double dhdv = (tri.getDisplacement(u, v+(1e-5), w-(1e-5), tol, 0.0)-disp)*1e5;
      dispnorm = new Vec3(norm);
      context.tempVec[0].set(dhdu*ugrad.x+dhdv*vgrad.x, dhdu*ugrad.y+dhdv*vgrad.y, dhdu*ugrad.z+dhdv*vgrad.z);
      dispnorm.scale(context.tempVec[0].dot(dispnorm)+1.0);
      dispnorm.subtract(context.tempVec[0]);
      dispnorm.normalize();

      // Find the screen position and lighting.

      tol = (z > context.camera.getDistToScreen()) ? smoothScale*z : smoothScale;
      if (shading == GOURAUD)
      {
        specular = new RGBColor();
        highlight = new RGBColor();
      }
      if (shading != PHONG)
        {
          diffuse = new RGBColor();
          tri.getTextureSpec(context.surfSpec, viewdir.dot(dispnorm), u, v, w, tol, time);
          calcLight(dispvert, dispnorm, viewdir, dispnorm, context.surfSpec.roughness, diffuse, specular, highlight, context);
          if (specular != null)
            specular.add(highlight);
        }
    }
  }

  /**
   * This class is used for the lock objects on individual rows.
   */

  private static class RowLock
  {
  }
}
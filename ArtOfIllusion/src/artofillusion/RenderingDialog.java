/* Copyright (C) 1999-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.image.*;
import artofillusion.image.filter.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This class implements the dialog box in which the user can watch a scene being rendered. */

public class RenderingDialog extends BDialog implements RenderListener
{
  private CustomWidget canvas;
  private Image previewImage;
  private ComplexImage originalImage, filteredImage;
  private Renderer renderer;
  private Scene theScene;
  private ObjectInfo sceneCamera;
  private SceneCamera cameraForFilters;
  private Camera theCamera;
  private double start, end, originalTime;
  private ImageSaver imgsaver;
  private ImageAverager imgaverager;
  private BButton closeButton, saveButton, filterButton;
  private BLabel label1, label2;
  private BFrame parent;
  private int w, h, fps, subimages, currentFrame, currentSubimage, totalFrames;
  private long startTime;
  private boolean done, hasModifiedFilters;

  /** Render a single frame. */

  public RenderingDialog(BFrame parent, Renderer rend, Scene sc, Camera cam, ObjectInfo sceneCamera)
  {
    super(parent, true);
    this.parent = parent;
    this.sceneCamera = sceneCamera;
    renderer = rend;
    theScene = sc;
    layoutDialog(parent, cam);
    startTime = System.currentTimeMillis();
    SceneCamera scm = (SceneCamera) sceneCamera.getObject();
    rend.renderScene(sc, cam, this, scm);
    setVisible(true);
  }
  
  /** Render an animation. */
  
  public RenderingDialog(BFrame parent, Renderer rend, Scene sc, Camera cam, ObjectInfo sceneCamera,
    double start, double end, int fps, int subimages, ImageSaver imgsaver)
  {
    super(parent, true);
    this.parent = parent;
    renderer = rend;
    theScene = sc;
    theCamera = cam;
    this.sceneCamera = sceneCamera;
    this.start = start;
    this.end = end;
    this.fps = fps;
    this.subimages = subimages;
    this.imgsaver = imgsaver;
    originalTime = theScene.getTime();
    totalFrames = (int) Math.ceil((end-start)*fps);
    if (totalFrames <= 0)
      totalFrames = 1;
    if (subimages > 1)
      imgaverager = new ImageAverager(theCamera.getSize().width, theCamera.getSize().height);
    layoutDialog(parent, cam);
    startTime = System.currentTimeMillis();
    sc.setTime(start);
    theCamera.setCameraCoordinates(sceneCamera.getCoords().duplicate());
    SceneCamera scm = (SceneCamera) sceneCamera.getObject();
    cam.setScreenTransform(scm.getScreenTransform(w, h), w, h);
    rend.renderScene(sc, theCamera, this, scm);
    setVisible(true);
  }

  private void layoutDialog(BFrame parent, Camera cam)
  {
    Dimension dim = cam.getSize();
    w = dim.width;
    h = dim.height;
    setFont(parent.getFont());
    FormContainer content = new FormContainer(new double [] {1, 0, 0}, new double [] {0, 0, 1});
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null));
    content.add(label1 = new BLabel(Translate.text("Rendering", "...")), 0, 0);
    content.add(label2 = new BLabel(Translate.text("elapsedTime", "0:00")), 0, 1);
    content.add(closeButton = Translate.button("cancel", this, "doCancel"), 2, 0);
    content.add(saveButton = Translate.button("save", this, "doSave"), 2, 1);
    content.add(filterButton = Translate.button("filter", this, "doFilter"), 1, 1);
    closeButton.setFocusable(false);
    saveButton.setVisible(false);
    filterButton.setVisible(false);
    canvas = new CustomWidget();
    canvas.setPreferredSize(new Dimension(w, h));
    canvas.addEventLink(RepaintEvent.class, this, "paintCanvas");
    BScrollPane sp = new BScrollPane(canvas);
    content.add(sp, 0, 2, 3, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    pack();
    UIUtilities.centerDialog(this, parent);
    UIUtilities.fitWindowToScreen(this);
    addEventLink(WindowClosingEvent.class, this, "doCancel");
  }
  
  private void doCancel()
  {
    done = true;
    renderer.cancelRendering(theScene);
    if (imgsaver != null)
    {
      theScene.setTime(originalTime);
      try
      {
        imgsaver.lastMovieImage(); // Ken: soft abort; file should be readable.
      }
      catch (IOException ex)
      {
        new BStandardDialog("", Translate.text("errorSavingFile", ex.getMessage() == null ? "" : ex.getMessage()), BStandardDialog.ERROR).showMessageDialog(parent);
      }
    }
    dispose();
    if (hasModifiedFilters)
    {
      String values[] = new String[] {Translate.text("button.save"), Translate.text("button.dontSave")};
      int choice = new BStandardDialog("", Translate.text("saveModifiedFilters", sceneCamera.getName()), BStandardDialog.QUESTION).showOptionDialog(parent, values, values[0]);
      if (choice == 0)
        ((SceneCamera) sceneCamera.getObject()).setImageFilters(cameraForFilters.getImageFilters());
    }
  }
  
  private void doSave()
  {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    ImageSaver saver = new ImageSaver(parent);
    if (saver.clickedOk())
    {
      try
      {
        saver.saveImage(filteredImage);
      }
      catch (IOException ex)
      {
        new BStandardDialog("", Translate.text("errorSavingFile", ex.getMessage() == null ? "" : ex.getMessage()), BStandardDialog.ERROR).showMessageDialog(parent);
      }
    }
    setCursor(Cursor.getDefaultCursor());
    toFront();
  }

  private void doFilter()
  {
    final CameraFilterDialog.FiltersPanel filtersPanel = new CameraFilterDialog.FiltersPanel(cameraForFilters, new Runnable()
    {
      public void run()
      {
      }
    });
    final BDialog dlg = new BDialog(this, Translate.text("Filters"), true);
    final ImageFilter originalFilters[] = cameraForFilters.getImageFilters();
    final boolean hasAppliedFilters[] = new boolean[1];
    RowContainer buttonPanel = new RowContainer();
    buttonPanel.add(Translate.button("apply", new Object() {
      void processEvent()
      {
        hasAppliedFilters[0] = true;
        cameraForFilters.setImageFilters(filtersPanel.getFilters().toArray(new ImageFilter[0]));
        applyFilters(false);
        verifyFilters(dlg);
      }
    }, "processEvent"));
    buttonPanel.add(Translate.button("ok", new Object() {
      void processEvent()
      {
        cameraForFilters.setImageFilters(filtersPanel.getFilters().toArray(new ImageFilter[0]));
        applyFilters(false);
        verifyFilters(dlg);
        hasModifiedFilters = true;
        dlg.dispose();
      }
    }, "processEvent"));
    buttonPanel.add(Translate.button("cancel", new Object() {
      void processEvent()
      {
        if (hasAppliedFilters[0])
        {
          cameraForFilters.setImageFilters(originalFilters);
          applyFilters(false);
        }
        dlg.dispose();
      }
    }, "processEvent"));
    BorderContainer content = new BorderContainer();
    content.add(filtersPanel, BorderContainer.CENTER);
    content.add(buttonPanel, BorderContainer.SOUTH, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE));
    dlg.setContent(content);
    dlg.pack();
    dlg.setVisible(true);
  }
  
  private void paintCanvas(RepaintEvent ev)
  {
    if (previewImage != null)
      ev.getGraphics().drawImage(previewImage, 0, 0, null);
  }

  
  /** Update the label of how much time has elapsed. */
  
  private void updateTimeLabel()
  {
    int sec = (int) ((System.currentTimeMillis()-startTime)/1000);
    int hour = sec/3600;
    sec -= 3600*hour;
    int min = sec/60;
    sec -= 60*min;
    if (hour == 0)
      label2.setText(Translate.text("elapsedTime", min+":"+(sec<10?"0":"")+sec));
    else
      label2.setText(Translate.text("elapsedTime", hour+":"+(min<10?"0":"")+min+":"+(sec<10?"0":"")+sec));
  }

  /** Make sure all filters can be applied, and show a warning message if now. */

  private void verifyFilters(WindowWidget parent)
  {
    ImageFilter[] filters = cameraForFilters.getImageFilters();
    for (int i = 0; i < filters.length; i++)
    {
      int required = filters[i].getDesiredComponents();
      while (required != 0)
      {
        int component = required - (required&(required-1));
        if (!originalImage.hasFloatData(component))
        {
          new BStandardDialog("", UIUtilities.breakString(Translate.text("missingComponentsForFilters")), BStandardDialog.WARNING).showMessageDialog(parent);
          return;
        }
        required -= component;
      }
    }
  }

  /** Apply the filters to the image. */

  private void applyFilters(boolean updateLabel)
  {
    filteredImage = null;
    if (cameraForFilters.getImageFilters().length > 0)
    {
      filteredImage = originalImage.duplicate();
      if (updateLabel)
        statusChanged(Translate.text("applyingFilters"));
      cameraForFilters.applyImageFilters(filteredImage, theScene, sceneCamera.getCoords());
    }
    else
      filteredImage = originalImage;
    previewImage = filteredImage.getImage();
    canvas.repaint();
  }
  
  /** Called when more pixels are available for the current image. */
  
  public void imageUpdated(Image image)
  {
    previewImage = image;
    canvas.repaint();
    updateTimeLabel();
  }
  
  /** Called when the status changes. */
  
  public void statusChanged(String status)
  {
    if (imgsaver != null)
    {
      String current = Integer.toString(currentFrame+1);
      String total = Integer.toString(totalFrames);
      if (subimages > 1)
        label1.setText(Translate.text("renderSubimageLabel", new String [] {current, total, Integer.toString(currentSubimage+1), status}));
      else
        label1.setText(Translate.text("renderFrameLabel", new String [] {current, total, status}));
    }
    else
      label1.setText(status+"...");
    updateTimeLabel();
  }

  
  /** Called when rendering is finished. */
  
  public void imageComplete(ComplexImage image)
  {
    cameraForFilters = ((SceneCamera) sceneCamera.getObject()).duplicate();
    originalImage = image;
    previewImage = image.getImage();
    canvas.repaint();
    applyFilters(true);
    try
    {
      EventQueue.invokeAndWait(new Runnable() {
        public void run()
        {
          try
          {
            if (imgsaver != null)
              nextFrame();
            if (currentFrame == totalFrames)
            {
              done = true;
              if (imgsaver != null)
                imgsaver.lastMovieImage();
              label1.setText(Translate.text("doneRendering"));
              closeButton.setText(Translate.text("button.close"));
              saveButton.setVisible(imgsaver == null);
              filterButton.setVisible(imgsaver == null);
              ((WidgetContainer) getContent()).layoutChildren();
            }
            updateTimeLabel();
          }
          catch (final IOException ex)
          {
            EventQueue.invokeLater(new Runnable() {
              public void run()
              {
                new BStandardDialog("", Translate.text("errorSavingFile", ex.getMessage() == null ? "" : ex.getMessage()), BStandardDialog.ERROR).showMessageDialog(parent);
                doCancel();
              }
            });
          }
        }
      });
    }
    catch (Exception ex)
    {
      // This generally means the thread has been interrupted, which we can just ignore.

      ex.printStackTrace();
    }
  }
  
  /** Called when rendering is cancelled. */
  
  public void renderingCanceled()
  {
    dispose();
  }
  
  /** Save the image which has just finished rendering, and begin the next one. */
  
  private void nextFrame() throws IOException
  {
    if (done)
      return;
    statusChanged(Translate.text("Saving"));
    if (imgaverager != null)
    {
      imgaverager.addImage(filteredImage);
      currentSubimage++;
      if (currentSubimage == subimages)
      {
        imgsaver.saveImage(imgaverager.getAverageImage());
        imgaverager.clear();
        currentSubimage = 0;
        currentFrame++;
        if (currentFrame == totalFrames)
          return;
      }
    }
    else
    {
      imgsaver.saveImage(filteredImage);
      currentFrame++;
    }
    theScene.setTime(start+(currentFrame*subimages+currentSubimage)/(double) (fps*subimages));
    theCamera.setCameraCoordinates(sceneCamera.getCoords().duplicate());
    SceneCamera scm = (SceneCamera) sceneCamera.getObject();
    theCamera.setScreenTransform(scm.getScreenTransform(w, h), w, h);
    renderer.renderScene(theScene, theCamera, this, scm);
    statusChanged(Translate.text("Rendering"));
  }
}
/* Copyright (C) 1999-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import artofillusion.view.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

/** ViewerCanvas is the abstract superclass of all components which display objects, and allow
    the user to manipulate them with EditingTools. */

public abstract class ViewerCanvas extends CustomWidget
{
  protected Camera theCamera;
  protected ObjectInfo boundCamera;
  protected EditingTool currentTool, activeTool, metaTool, altTool;
  protected PopupMenuManager popupManager;
  protected int renderMode, gridSubdivisions, orientation;
  protected double gridSpacing, scale;
  protected boolean perspective, hideBackfaces, showGrid, snapToGrid, drawFocus, showTemplate, showAxes;
  protected ActionProcessor mouseProcessor;
  protected Image templateImage, renderedImage;
  protected CanvasDrawer drawer;
  protected Dimension prefSize;
  protected Map<ViewerControl,Widget> controlMap;
  protected Vec3 rotationCenter;

  protected final ViewChangedEvent viewChangedEvent;
  
  private static boolean openGLAvailable;
  private static List<ViewerControl> controls = new ArrayList<ViewerControl>();
  
  static
  {
    try
    {
      Class.forName("javax.media.opengl.GLCanvas");
      openGLAvailable = true;
    }
    catch (Throwable t)
    {
      System.out.println("Error loading GLCanvas class: "+t);
      System.out.println("java.library.path: "+System.getProperty("java.library.path"));
    }
  }

  public static Color backgroundColor;
  public static Color lineColor;
  public static Color handleColor;
  public static Color highlightColor;
  public static Color specialHighlightColor;
  public static Color disabledColor;
  public static Color surfaceColor;
  public static RGBColor surfaceRGBColor;
  public static RGBColor transparentColor;
  public static RGBColor lowValueColor;
  public static RGBColor highValueColor;

  public static final int RENDER_WIREFRAME = 0;
  public static final int RENDER_FLAT = 1;
  public static final int RENDER_SMOOTH = 2;
  public static final int RENDER_TEXTURED = 3;
  public static final int RENDER_TRANSPARENT = 4;
  public static final int RENDER_RENDERED = 5;

  public static final int VIEW_FRONT = 0;
  public static final int VIEW_BACK = 1;
  public static final int VIEW_LEFT = 2;
  public static final int VIEW_RIGHT = 3;
  public static final int VIEW_TOP = 4;
  public static final int VIEW_BOTTOM = 5;
  public static final int VIEW_OTHER = Integer.MAX_VALUE;

  public ViewerCanvas()
  {
    this(ArtOfIllusion.getPreferences().getUseOpenGL() && openGLAvailable);
  }

  public ViewerCanvas(boolean useOpenGL)
  {
    CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    viewChangedEvent = new ViewChangedEvent(this);
    controlMap = new HashMap<ViewerControl,Widget>();
    theCamera = new Camera();
    theCamera.setCameraCoordinates(coords);
    setBackground(backgroundColor);
    if (useOpenGL)
    {
      try
      {
        drawer = new GLCanvasDrawer(this);
        component = ((GLCanvasDrawer) drawer).getGLCanvas();
      }
      catch (Throwable t)
      {
        System.out.println("Error creating GLCanvasDrawer: "+t);
        openGLAvailable = false;
      }
    }
    if (drawer == null)
      drawer = new SoftwareCanvasDrawer(this);
    setFocusable(true);
    prefSize = new Dimension(0, 0);
    addEventLink(MousePressedEvent.class, this, "processMousePressed");
    addEventLink(MouseReleasedEvent.class, this, "processMouseReleased");
    addEventLink(MouseDraggedEvent.class, this, "processMouseDragged");
    addEventLink(MouseMovedEvent.class, this, "processMouseDragged"); // Workaround for Mac OS X bug
    addEventLink(MouseScrolledEvent.class, this, "processMouseScrolled");
    addEventLink(MouseClickedEvent.class, this, "showPopupIfNeeded");
    getComponent().addComponentListener(new ComponentListener()
    {
      public void componentResized(ComponentEvent componentEvent)
      {
        viewChanged(false);
      }
      public void componentMoved(ComponentEvent componentEvent)
      {
      }
      public void componentShown(ComponentEvent componentEvent)
      {
      }
      public void componentHidden(ComponentEvent componentEvent)
      {
      }
    });
    orientation = 0;
    perspective = false;
    scale = 100.0;
  }
  
  /** Get the CanvasDrawer which is rendering the image for this canvas. */
  
  public CanvasDrawer getCanvasDrawer()
  {
    return drawer;
  }

  /** Build the menus at the top.  Subclasses of ViewerCanvas can call this method if they want to
      use the standard set of menus. */

  protected void buildChoices(RowContainer row)
  {
    for (int i = 0; i < controls.size(); i++)
    {
      Widget w = controls.get(i).createWidget(this);
      if (w != null)
      {
        row.add(w);
        controlMap.put(controls.get(i), w);
      }
    }
    viewChanged(false);
  }
  
  private void processMousePressed(WidgetMouseEvent ev)
  {
    if (mouseProcessor != null)
      mouseProcessor.stopProcessing();
    mousePressed(ev);
    mouseProcessor = new ActionProcessor();
  }
  
  private void processMouseDragged(final WidgetMouseEvent ev)
  {
    if (mouseProcessor != null)
      mouseProcessor.addEvent(new Runnable() {
        public void run()
        {
          mouseDragged(ev);
        }
      });
  }
  
  private void processMouseReleased(WidgetMouseEvent ev)
  {
    if (mouseProcessor != null)
    {
      mouseProcessor.stopProcessing();
      mouseProcessor = null;
      mouseReleased(ev);
    }
  }

  protected void processMouseScrolled(MouseScrolledEvent ev)
  {
    int amount = ev.getWheelRotation();
    if (!ev.isAltDown())
      amount *= 10;
    if (ArtOfIllusion.getPreferences().getReverseZooming())
      amount *= -1;
    if (isPerspective())
    {
      CoordinateSystem coords = theCamera.getCameraCoordinates();
      Vec3 delta = coords.getZDirection().times(-0.1*amount);
      coords.setOrigin(coords.getOrigin().plus(delta));
      theCamera.setCameraCoordinates(coords);
      viewChanged(false);
      repaint();
    }
    else
    {
      setScale(getScale()*Math.pow(0.99, amount));
    }
  }
  
  /** Subclasses should override this to handle events. */
  
  protected void mousePressed(WidgetMouseEvent ev)
  {
  }
  
  /** Subclasses should override this to handle events. */

  protected void mouseDragged(WidgetMouseEvent ev)
  {
  }
  
  /** Subclasses should override this to handle events. */

  protected void mouseReleased(WidgetMouseEvent ev)
  {
  }
  
  /** This needs to be overridden, since the component may not be a JComponent. */
  
  public void setPreferredSize(Dimension size)
  {
    prefSize = new Dimension(size);
  }
  
  public Dimension getPreferredSize()
  {
    return new Dimension(prefSize);
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(0, 0);
  }

  /** Get the ActionProcessor which is currently in use for processing mouse events (may be null). */
  
  public ActionProcessor getActionProcessor()
  {
    return mouseProcessor;
  }

  public Camera getCamera()
  {
    return theCamera;
  }

  /**
   * Get the Scene being displayed in this canvas.  The default implementation returns null.
   * Subclasses may override it to return an appropriate Scene.
   */

  public Scene getScene()
  {
    return null;
  }

  /** Set the currently selected tool. */

  public void setTool(EditingTool tool)
  {
    currentTool = tool;
    repaint();
  }
  
  /** Get the currently selected tool. */
  
  public EditingTool getCurrentTool()
  {
    return currentTool;
  }

  /** Set the tool which should be active when the meta key is pressed. */
  
  public void setMetaTool(EditingTool tool)
  {
    metaTool = tool;
  }

  /** Set the tool which should be active when the alt key is pressed. */
  
  public void setAltTool(EditingTool tool)
  {
    altTool = tool;
  }
  
  /** Set whether to display perspective or parallel mode. */

  public void setPerspective(boolean perspective)
  {
    this.perspective = perspective;
    viewChanged(false);
    repaint();
  }
  
  /** Determine whether the view is currently is perspective mode. */
  
  public boolean isPerspective()
  {
    if (boundCamera != null && boundCamera.getObject() instanceof SceneCamera)
      return ((SceneCamera) boundCamera.getObject()).isPerspective();
    return perspective;
  }
  
  /** Get the current scale factor for the view. */
  
  public double getScale()
  {
    if (isPerspective())
      return 100.0;
    return scale;
  }
  
  /** Set the scale factor for the view. */
  
  public void setScale(double scale)
  {
    if (scale > 0.0)
      this.scale = scale;
    viewChanged(false);
    repaint();
  }
  
  /** Get whether a focus ring should be drawn around this component. */
  
  public boolean getDrawFocus()
  {
    return drawFocus;
  }
  
  /** Set whether a focus ring should be drawn around this component. */
  
  public void setDrawFocus(boolean draw)
  {
    drawFocus = draw;
  }
  
  /** Determine whether the coordinate axes are currently showing. */
  
  public boolean getShowAxes()
  {
    return showAxes;
  }
  
  /** Set whether the coordinate axes should be displayed. */
  
  public void setShowAxes(boolean show)
  {
    showAxes = show;
    viewChanged(false);
  }
  
  /** Determine whether the template image is currently showing. */
  
  public boolean getTemplateShown()
  {
    return showTemplate;
  }
  
  /** Set whether the template image should be displayed. */
  
  public void setShowTemplate(boolean show)
  {
    showTemplate = show;
    viewChanged(false);
  }
  
  /** Get the template image. */
  
  public Image getTemplateImage()
  {
    return templateImage;
  }
  
  /** Set the template image. */
  
  public void setTemplateImage(Image im)
  {
    templateImage = im;
    drawer.setTemplateImage(im);
    viewChanged(false);
  }
  
  /** Set the template image based on an image file. */
  
  public void setTemplateImage(File f) throws InterruptedException
  {
    Image im = Toolkit.getDefaultToolkit().getImage(f.getAbsolutePath());
    MediaTracker mt = new MediaTracker(getComponent());
    mt.addImage(im, 0);
    mt.waitForID(0);
    if (mt.isErrorID(0))
      throw (new InterruptedException());
    setTemplateImage(im);
  }

  /**
   * Get the location around which the view should be rotated.  This may be null, in which case the value
   * returned by {@link #getDefaultRotationCenter()} will be used instead.
   */

  public Vec3 getRotationCenter()
  {
    return rotationCenter;
  }

  /**
   * Set the location around which the view should be rotated.  This may be null, in which case the value
   * returned by {@link #getDefaultRotationCenter()} will be used instead.
   */

  public void setRotationCenter(Vec3 rotationCenter)
  {
    this.rotationCenter = rotationCenter;
  }

  /**
   * Get the default location around which the view should be rotated.  This value will be used if
   * {@link #getRotationCenter()} returns null.
   */

  public Vec3 getDefaultRotationCenter()
  {
    CoordinateSystem coords = theCamera.getCameraCoordinates();
    double distToCenter = -coords.getZDirection().dot(coords.getOrigin());
    return coords.getOrigin().plus(coords.getZDirection().times(distToCenter));
  }

  /** Set the PopupMenuManager for this canvas. */

  public void setPopupMenuManager(PopupMenuManager manager)
  {
    popupManager = manager;
  }
  
  /** Display the popup menu when an appropriate event occurs. */
  
  protected void showPopupIfNeeded(WidgetMouseEvent ev)
  {
    if (popupManager != null && ev instanceof MouseClickedEvent && ev.getButton() == WidgetMouseEvent.BUTTON3 && ev.getClickCount() == 1)
    {
      Point pos = ev.getPoint();
      popupManager.showPopupMenu(this, pos.x, pos.y);
    }
  }

  public void adjustCamera(boolean perspective)
  {
    Rectangle bounds = getBounds();
    double scale = getScale();

    if (boundCamera != null && boundCamera.getObject() instanceof SceneCamera)
      theCamera.setScreenTransform(((SceneCamera) boundCamera.getObject()).getScreenTransform(bounds.width, bounds.height), bounds.width, bounds.height);
    else if (perspective)
      theCamera.setScreenParams(0, scale, bounds.width, bounds.height);
    else
      theCamera.setScreenParamsParallel(scale, bounds.width, bounds.height);  
  }

  /** Get the SceneCamera (if any) which is bound to this view. */

  public ObjectInfo getBoundCamera()
  {
    return boundCamera;
  }

  /** Set the SceneCamera which is bound to this view (may be null). */
  
  public void setBoundCamera(ObjectInfo boundCamera)
  {
    this.boundCamera = boundCamera;
  }

  /** Set the grid parameters for this view. */

  public void setGrid(double spacing, int subdivisions, boolean show, boolean snap)
  {
    gridSpacing = spacing;
    gridSubdivisions = subdivisions;
    showGrid = show;
    snapToGrid = snap;
    if (snap)
      theCamera.setGrid(spacing/subdivisions);
    else
      theCamera.setGrid(0.0);
    viewChanged(false);
  }

  /** Get whether the grid is shown. */

  public boolean getShowGrid()
  {
    return showGrid;
  }

  /** Get whether Snap To Grid is enabled. */

  public boolean getSnapToGrid()
  {
    return snapToGrid;
  }

  /** Get the grid spacing. */

  public double getGridSpacing()
  {
    return gridSpacing;
  }

  /** Get the number of "snap to" subdivisions between grid lines. */

  public int getSnapToSubdivisions()
  {
    return gridSubdivisions;
  }

  /** Adjust the camera position and magnification so that the specified box
      fills the view.  This has no effect if there is a camera point to this
      view. */
  
  public void frameBox(BoundingBox bb)
  {
    if (boundCamera != null)
      return;
    
    // Move the camera so that it points at the center of the box, and is well outside it. 
       
    Rectangle bounds = getBounds();
    if (isPerspective())
      theCamera.setScreenParams(0, 100.0, bounds.width, bounds.height);
    else
      theCamera.setScreenParamsParallel(100.0, bounds.width, bounds.height);  
    double startDist = Camera.DEFAULT_DISTANCE_TO_SCREEN+Math.max(Math.max(bb.maxx-bb.minx, bb.maxy-bb.miny), bb.maxz-bb.minz);
    CoordinateSystem coords = theCamera.getCameraCoordinates();
    Vec3 boxCenter = bb.getCenter();
    coords.setOrigin(boxCenter.minus(coords.getZDirection().times(startDist)));
    theCamera.setCameraCoordinates(coords);
    
    // Now adjust the magnification or camera position to make the box fill
    // the view.
    
    theCamera.setObjectTransform(Mat4.identity());
    Rectangle screenBounds = theCamera.findScreenBounds(bb);
    double scalex = bounds.width/(double) screenBounds.width;
    double scaley = bounds.height/(double) screenBounds.height;
    double minScale = (scalex < scaley ? scalex : scaley);
    if (isPerspective())
    {
      // Perspective mode, so adjust the camera position.
      
      coords.setOrigin(boxCenter.minus(coords.getZDirection().times(1.1*startDist/minScale)));
      setScale(100.0);
    }
    else
    {
      // Parallel mode, so adjust the magnification.
      
      setScale(minScale*100.0);
    }
    theCamera.setCameraCoordinates(coords);

  }
  
  /** This should be called by the CanvasDrawer just before rendering an image.  It sets up the camera correctly. */
  
  public void prepareCameraForRendering()
  {
    if (boundCamera != null)
      boundCamera.getCoords().copyCoords(theCamera.getCameraCoordinates());
    adjustCamera(isPerspective());
  }
  
  /** Estimate the range of depth values that the camera will need to render.  This need not be exact,
      but should err on the side of returning bounds that are slightly too large.
      @return the two element array {minDepth, maxDepth}
   */
  
  public abstract double[] estimateDepthRange();

  /**
   * This is called when the content of the view has changed.
   *
   * @param selectionOnly   if true, the only change to the view is what is currently selected
   */

  public void viewChanged(boolean selectionOnly)
  {
    dispatchEvent(viewChangedEvent);
  }

  /** Subclasses should override this to draw the contents of the canvas, but should begin
      by calling super.updateImage() to display the grid. */

  public synchronized void updateImage()
  {
    Rectangle bounds = getBounds();
    
    if (bounds.height <= 0)
      return;
    
    // Draw the grid, if necessary.
    
    if (showGrid)
    {
      float scale1 = 0.75f/255.0f;
      float scale2 = 0.25f/255.0f;
      Color majorColor = new Color(lineColor.getRed()*scale1+backgroundColor.getRed()*scale2,
          lineColor.getGreen()*scale1+backgroundColor.getGreen()*scale2,
          lineColor.getBlue()*scale1+backgroundColor.getBlue()*scale2);
      Color minorColor = new Color(lineColor.getRed()*scale2 +backgroundColor.getRed()*scale1,
          lineColor.getGreen()*scale2 +backgroundColor.getGreen()*scale1,
          lineColor.getBlue()*scale2 +backgroundColor.getBlue()*scale1);
      if (!isPerspective())
      {
        // Parallel mode, so draw a flat grid.
        
        Vec2 v1 = theCamera.getViewToScreen().timesXY(new Vec3());
        Vec2 v2 = theCamera.getViewToScreen().timesXY(new Vec3(gridSpacing, gridSpacing, 1.0));
        Vec2 v3 = theCamera.getWorldToScreen().timesXY(new Vec3());
        Vec3 horizDir = theCamera.getViewToWorld().timesDirection(Vec3.vx());
        Vec3 vertDir = theCamera.getViewToWorld().timesDirection(Vec3.vy());
        double horizSign, vertSign;
        if (Math.abs(horizDir.x) >= Math.abs(horizDir.y) && Math.abs(horizDir.x) >= Math.abs(horizDir.z))
          horizSign = (horizDir.x > 0 ? -1 : 1);
        else if (Math.abs(horizDir.y) >= Math.abs(horizDir.x) && Math.abs(horizDir.y) >= Math.abs(horizDir.z))
          horizSign = (horizDir.y > 0 ? -1 : 1);
        else
          horizSign = (horizDir.z > 0 ? -1 : 1);
        if (Math.abs(vertDir.x) >= Math.abs(vertDir.y) && Math.abs(vertDir.x) >= Math.abs(vertDir.z))
          vertSign = (vertDir.x > 0 ? -1 : 1);
        else if (Math.abs(vertDir.y) >= Math.abs(vertDir.x) && Math.abs(vertDir.y) >= Math.abs(vertDir.z))
          vertSign = (vertDir.y > 0 ? -1 : 1);
        else
          vertSign = (vertDir.z > 0 ? -1 : 1);
        int decimals = 2;
        if (Math.abs(gridSpacing-Math.round(gridSpacing)) < 1e-5)
          decimals = 0;
        else if (Math.abs(10.0*gridSpacing-Math.round(10.0*gridSpacing)) < 1e-5)
          decimals = 1;
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);
        FontMetrics fm = getComponent().getFontMetrics(getComponent().getFont());
        int ascent = fm.getMaxAscent();
        double space = Math.abs(v1.x-v2.x);
        int origin = (int) v3.x;
        double pos = Math.IEEEremainder(v3.x, space);
        if (pos < 0.0)
          pos += space;
        int numberInterval = 1;
        if (space < 10)
          numberInterval = 5;
        else if (space < 25)
          numberInterval = 2;
        while ((int) pos < bounds.width)
        {
          int x = (int) pos;
          drawVRule(x, (x == origin ? majorColor : minorColor));
          int lineIndex = (int) Math.round((pos-v3.x)/space);
          if (lineIndex%numberInterval == 0)
            drawString(format.format(lineIndex*gridSpacing*horizSign), x+3, ascent+3, lineColor);
          pos += space;
        }
        space = Math.abs(v1.y-v2.y);
        origin = (int) v3.y;
        pos = Math.IEEEremainder(v3.y, space);
        if (pos < 0.0)
          pos += space;
        while ((int) pos < bounds.height)
        {
          int y = (int) pos;
          drawHRule(y, (y == origin ? majorColor : minorColor));
          int lineIndex = (int) Math.round((pos-v3.y)/space);
          if (lineIndex%numberInterval == 0)
          drawString(format.format(lineIndex*gridSpacing*vertSign), 3, y+ascent+3, lineColor);
          pos += space;
        }
      }
      else
      {
        // Perspective mode, so draw a ground plane.
        
        theCamera.setObjectTransform(Mat4.identity());
        int size = (int) Math.max(10, 10/gridSpacing);
        for (int i = -size; i <= size; i++)
        {
          Vec3 end1 = new Vec3(i*gridSpacing, 0.0, -size*gridSpacing);
          Vec3 end2 = new Vec3(i*gridSpacing, 0.0, size*gridSpacing);
          renderLine(end1, end2, theCamera, i == 0 ? majorColor : minorColor);
        }
        for (int i = -size; i <= size; i++)
        {
          Vec3 end1 = new Vec3(-size*gridSpacing, 0.0, i*gridSpacing);
          Vec3 end2 = new Vec3(size*gridSpacing, 0.0, i*gridSpacing);
          renderLine(end1, end2, theCamera, i == 0 ? majorColor : minorColor);
        }
      }
    }
  }
  
  /** Draw the coordinate axes into the view. */
  
  protected void drawCoordinateAxes()
  {
    // Select a size for the coordinate axes.
    
    Rectangle bounds = getBounds();
    int axisLength = 50;
    if (axisLength*5 > bounds.width)
      axisLength = bounds.width/5;
    if (axisLength*5 > bounds.height)
      axisLength = bounds.height/5;
    double len = axisLength/getScale();
    
    // Calculate the screen positions of the axis ends.
    
    Vec2 offset = new Vec2(0.5*bounds.width-axisLength-15, 0.5*bounds.height-axisLength-15);
    CoordinateSystem cameraCoords = theCamera.getCameraCoordinates();
    Vec3 center = cameraCoords.getOrigin().plus(cameraCoords.getZDirection().times(theCamera.getDistToScreen()));
    Vec3 xpos = center.plus(new Vec3(len, 0.0, 0.0));
    Vec3 ypos = center.plus(new Vec3(0.0, len, 0.0));
    Vec3 zpos = center.plus(new Vec3(0.0, 0.0, len));
    Vec2 screenCenter = theCamera.getWorldToScreen().timesXY(center).plus(offset);
    Vec2 screenX = theCamera.getWorldToScreen().timesXY(xpos).plus(offset);
    Vec2 screenY = theCamera.getWorldToScreen().timesXY(ypos).plus(offset);
    Vec2 screenZ = theCamera.getWorldToScreen().timesXY(zpos).plus(offset);
    
    // Draw the axes.
    
    Point centerPoint = new Point((int) Math.round(screenCenter.x), (int) Math.round(screenCenter.y));
    drawLine(centerPoint, new Point((int) screenX.x, (int) screenX.y), lineColor);
    drawLine(centerPoint, new Point((int) screenY.x, (int) screenY.y), lineColor);
    drawLine(centerPoint, new Point((int) screenZ.x, (int) screenZ.y), lineColor);
    
    // Draw the labels.
    
    if (screenX.minus(screenCenter).length() > 2.0)
    {
      Vec2 dir = screenX.minus(screenCenter);
      Vec2 labelPos = screenX.plus(dir.times(8.0/dir.length()));
      int x = (int) labelPos.x;
      int y = (int) labelPos.y;
      drawLine(new Point(x-4, y-4), new Point(x+4, y+4), lineColor);
      drawLine(new Point(x-4, y+4), new Point(x+4, y-4), lineColor);
    }
    if (screenY.minus(screenCenter).length() > 2.0)
    {
      Vec2 dir = screenY.minus(screenCenter);
      Vec2 labelPos = screenY.plus(dir.times(8.0/dir.length()));
      int x = (int) labelPos.x;
      int y = (int) labelPos.y;
      drawLine(new Point(x-4, y-4), new Point(x, y), lineColor);
      drawLine(new Point(x+4, y-4), new Point(x, y), lineColor);
      drawLine(new Point(x, y), new Point(x, y+4), lineColor);
    }
    if (screenZ.minus(screenCenter).length() > 2.0)
    {
      Vec2 dir = screenZ.minus(screenCenter);
      Vec2 labelPos = screenZ.plus(dir.times(8.0/dir.length()));
      int x = (int) labelPos.x;
      int y = (int) labelPos.y;
      drawLine(new Point(x-4, y-4), new Point(x+4, y-4), lineColor);
      drawLine(new Point(x+4, y-4), new Point(x-4, y+4), lineColor);
      drawLine(new Point(x-4, y+4), new Point(x+4, y+4), lineColor);
    }
  }
  
  public int getRenderMode()
  {
    return renderMode;
  }
  
  public void setRenderMode(int mode)
  {
    if (mode == RENDER_RENDERED && currentTool != null)
    {
      for (ViewerCanvas view : currentTool.getWindow().getAllViews())
        if (view != this && view.getRenderMode() == RENDER_RENDERED)
        {
          new BStandardDialog("", Translate.text("renderedModeMultipleViews"), BStandardDialog.ERROR).showMessageDialog(UIUtilities.findWindow(this));
          return;
        }
    }
    renderMode = mode;
    renderedImage = null;
    viewChanged(false);
    repaint();
  }
  
  /** Adjust the coordinates of a mouse event to move it to the nearest
      grid location. */
  
  void moveToGrid(WidgetMouseEvent e)
  {
    Point pos = e.getPoint();
    Vec3 v;
    Vec2 v2;
    
    if (!snapToGrid || isPerspective())
      return;
    v = theCamera.convertScreenToWorld(pos, theCamera.getDistToScreen());
    v2 = theCamera.getWorldToScreen().timesXY(v);
    e.translatePoint((int) v2.x - pos.x, (int) v2.y - pos.y);
  }

  /**
   * Get the current orientation mode.
   */

  public int getOrientation()
  {
    return orientation;
  }

  /** Set the view orientation to any of the values shown in the choice menu. */
  
  public void setOrientation(int which)
  {
    orientation = which;
    if (which > 5 && which != VIEW_OTHER)
      return;
    CoordinateSystem coords = theCamera.getCameraCoordinates();
    double dist = theCamera.getDistToScreen();
    Vec3 center = coords.getOrigin().plus(coords.getZDirection().times(dist));

    if (which == 0)             // Front
    {
      center.z += dist;
      coords = new CoordinateSystem(center, new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    }
    else if (which == 1)        // Back
    {
      center.z -= dist;
      coords = new CoordinateSystem(center, Vec3.vz(), Vec3.vy());
    }
    else if (which == 2)        // Left
    {
      center.x -= dist;
      coords = new CoordinateSystem(center, Vec3.vx(), Vec3.vy());
    }
    else if (which == 3)        // Right
    {
      center.x += dist;
      coords = new CoordinateSystem(center, new Vec3(-1.0, 0.0, 0.0), Vec3.vy());
    }
    else if (which == 4)        // Top
    {
      center.y += dist;
      coords = new CoordinateSystem(center, new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 0.0, -1.0));
    }
    else if (which == 5)        // Bottom
    {
      center.y -= dist;
      coords = new CoordinateSystem(center, Vec3.vy(), Vec3.vz());
    }
    theCamera.setCameraCoordinates(coords);
    viewChanged(false);
    repaint();
  }
  
  /** If there is a camera bound to this view, copy the coordinates from it. */
  
  public void copyOrientationFromCamera()
  {
    if (boundCamera == null)
      return;
    CoordinateSystem coords = theCamera.getCameraCoordinates();
    coords.copyCoords(boundCamera.getCoords());
    theCamera.setCameraCoordinates(coords);
  }
  
  /** Show feedback to the user in response to a mouse drag, by drawing a Shape over the
      image.  This method should only ever be called from the event dispatch thread. */
  
  public void drawDraggedShape(Shape shape)
  {
    drawer.drawDraggedShape(shape);
  }
  
  /** Draw a border around the rendered image. */
  
  public void drawBorder()
  {
    drawer.drawBorder();
  }

  /** Draw a horizontal line across the rendered image.  The parameters are the y coordinate
      of the line and the line color. */
  
  public void drawHRule(int y, Color color)
  {
    drawer.drawHRule(y, color);
  }

  /** Draw a vertical line across the rendered image.  The parameters are the x coordinate
      of the line and the line color. */
  
  public void drawVRule(int x, Color color)
  {
    drawer.drawVRule(x, color);
  }

  /** Draw a filled box in the rendered image. */
  
  public void drawBox(int x, int y, int width, int height, Color color)
  {
    drawer.drawBox(x, y, width, height, color);
  }

  /** Draw a set of filled boxes in the rendered image. */

  public void drawBoxes(java.util.List<Rectangle> box, Color color)
  {
    if (box.size() > 0)
      drawer.drawBoxes(box, color);
  }

  /** Render a filled box at a specified depth in the rendered image. */
  
  public void renderBox(int x, int y, int width, int height, double depth, Color color)
  {
    drawer.renderBox(x, y, width, height, depth, color);
  }

  /** Render a set of filled boxes at specified depths in the rendered image. */

  public void renderBoxes(java.util.List<Rectangle> box, java.util.List<Double>depth, Color color)
  {
    if (box.size() > 0)
      drawer.renderBoxes(box, depth, color);
  }

  /** Draw a line into the rendered image. */
  
  public void drawLine(Point p1, Point p2, Color color)
  {
    drawer.drawLine(p1, p2, color);
  }
  
  /** Render a line into the image.
      @param p1     the first endpoint of the line
      @param p2     the second endpoint of the line
      @param cam    the camera from which to draw the line
      @param color  the line color
  */
  
  public void renderLine(Vec3 p1, Vec3 p2, Camera cam, Color color)
  {
    drawer.renderLine(p1, p2, cam, color);
  }

  /** Render a line into the image.
      @param p1     the first endpoint of the line, in screen coordinates
      @param zf1    the z coordinate of the first endpoint, in view coordinates
      @param p2     the second endpoint of the line, in screen coordinates
      @param zf2    the z coordinate of the second endpoint, in view coordinates
      @param cam    the camera from which to draw the line
      @param color  the line color
  */
  
  public void renderLine(Vec2 p1, double zf1, Vec2 p2, double zf2, Camera cam, Color color)
  {
    drawer.renderLine(p1, zf1, p2, zf2, cam, color);
  }
  
  /** Render a wireframe object. */
  
  public void renderWireframe(WireframeMesh mesh, Camera cam, Color color)
  {
    drawer.renderWireframe(mesh, cam, color);
  }

  /** Render an object with flat shading in subtractive (transparent) mode. */
  
  public void renderMeshTransparent(RenderingMesh mesh, VertexShader shader, Camera cam, Vec3 viewDir, boolean hideFace[])
  {
    drawer.renderMeshTransparent(mesh, shader, cam, viewDir, hideFace);
  }
  
  /** Render a mesh to the canvas. */
  
  public void renderMesh(RenderingMesh mesh, VertexShader shader, Camera cam, boolean closed, boolean hideFace[])
  {
    drawer.renderMesh(mesh, shader, cam, closed, hideFace);
  }

  /** Draw a piece of text onto the canvas. */

  public void drawString(String text, int x, int y, Color color)
  {
    drawer.drawString(text, x, y, color);
  }

  /** Draw an image onto the canvas. */

  public void drawImage(Image image, int x, int y)
  {
    drawer.drawImage(image, x, y);
  }

  /**
   * Render an image onto the canvas.  The image is drawn as a planar rectangle in 3D space,
   * which you specify by its four corners in clockwise order, starting from the top left.
   *
   * @param image  the image to render
   * @param p1     the coordinates of the first corner of the image
   * @param p2     the coordinates of the second corner of the image
   * @param p3     the coordinates of the third corner of the image
   * @param p4     the coordinates of the fourth corner of the image
   */

  public void renderImage(Image image, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4)
  {
    drawer.renderImage(image, p1, p2, p3, p4, theCamera);
  }

  /** Draw the outline of a Shape into the canvas. */

  public void drawShape(Shape shape, Color color)
  {
    drawer.drawShape(shape, color);
  }

  /** Draw a filled Shape onto the canvas. */

  public void fillShape(Shape shape, Color color)
  {
    drawer.fillShape(shape, color);
  }

  /** Determine whether OpenGL rendering is available. */
  
  public static boolean isOpenGLAvailable()
  {
    return openGLAvailable;
  }

  /**
   * Get the list of ViewerControls which will be added to each new ViewerCanvas.
   */

  public static List getViewerControls()
  {
    return Collections.unmodifiableList(controls);
  }

  /**
   * Add a new ViewerControl that will be added to each new ViewerCanvas.
   *
   * @param control     the ViewerControl to add
   */

  public static void addViewerControl(ViewerControl control)
  {
    controls.add(control);
  }

  /**
   * Add a new ViewerControl that will be added to each new ViewerCanvas.
   *
   * @param index       the position (from left to right) at which the new control should be added
   * @param control     the ViewerControl to add
   */

  public static void addViewerControl(int index, ViewerControl control)
  {
    controls.add(index, control);
  }

  /**
   * Remove a ViewerControl from the list of ones to be added to each new ViewerCanvas.
   *
   * @param control     the ViewerControl to remove
   */

  public static void removeViewerControl(ViewerControl control)
  {
    controls.remove(control);
  }

  /**
   * Get a Map whose keys are the defined ViewerControls, and whose values are the corresponding
   * Widgets for this canvas.
   */

  public Map getViewerControlWidgets()
  {
    return Collections.unmodifiableMap(controlMap);
  }
}
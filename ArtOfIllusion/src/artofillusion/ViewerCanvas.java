/* Copyright (C) 1999-2011 by Peter Eastman
   Changes Copyrignt (C) 2016-2020 Petri Ihalainen
   Changes copyright (C) 2016-2022 by Maksim Khramov

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
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.Timer;

/** ViewerCanvas is the abstract superclass of all components which display objects, and allow
    the user to manipulate them with EditingTools. */

public abstract class ViewerCanvas extends CustomWidget
{
  protected Camera theCamera;
  protected ObjectInfo boundCamera;
  protected EditingTool currentTool, activeTool, metaTool, altTool;
  protected ScrollViewTool scrollTool;
  protected PopupMenuManager popupManager;
  protected int renderMode, gridSubdivisions, orientation, navigation, scrollBuffer;
  protected double gridSpacing, scale, distToPlane, scrollRadius, scrollX, scrollY, scrollBlend, scrollBlendX, scrollBlendY;
  protected boolean perspective, perspectiveSwitch, hideBackfaces, showGrid, snapToGrid, drawFocus, showTemplate, showAxes;
  protected boolean lastModelPerspective;
  protected ActionProcessor mouseProcessor;
  protected Image templateImage, renderedImage;
  protected CanvasDrawer drawer;
  protected Dimension prefSize;
  protected Map<ViewerControl,Widget> controlMap;
  protected Vec3 rotationCenter;
  protected ViewAnimation animation;
  protected ClickedPointFinder finder;

  protected final ViewChangedEvent viewChangedEvent;

  public static Color gray, red, green, blue, CUE, cueActive, cueIdle, BEAM, beam;
  public FrustumShape frustumShape;
  public Point mousePoint;
  public boolean perspectiveControlEnabled = true;
  public boolean navigationTravelEnabled = true;
  public int lastSetNavigation = 0; // To get the mode right during animation preview
  public boolean showViewCone = true;

  private static boolean openGLAvailable;
  private static List<ViewerControl> controls = new ArrayList<ViewerControl>();
  private CoordinateAxes coordinateAxes;
  
  static
  {
    try
    {
      Class.forName("com.jogamp.opengl.awt.GLCanvas");
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

  public static final int NAVIGATE_MODEL_SPACE = 0;
  public static final int NAVIGATE_MODEL_LANDSCAPE = 1;
  public static final int NAVIGATE_TRAVEL_SPACE = 2;
  public static final int NAVIGATE_TRAVEL_LANDSCAPE = 3;

  public static final double MINIMUM_ZOOM_DISTANCE = 1.0E-12; // Closer than this numerical errors start to show

  public boolean mouseDown, mouseMoving, tilting, moving, rotating, scrolling, dragging;

  /** Create a new ViewerCanvas */
  public ViewerCanvas()
  {
    this(ArtOfIllusion.getPreferences().getUseOpenGL() && openGLAvailable);
  }

  /** Create a new ViewerCanvas */
  public ViewerCanvas(boolean useOpenGL)
  {
    CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    viewChangedEvent = new ViewChangedEvent(this);
    controlMap = new HashMap<ViewerControl,Widget>();
    theCamera = new Camera();
    theCamera.setCameraCoordinates(coords);
    finder = new ClickedPointFinder();
    frustumShape = new FrustumShape();
    coordinateAxes = new CoordinateAxes();
    setBackground(backgroundColor);
    setRotationCenter(new Vec3());
    setDistToPlane(Camera.DEFAULT_DISTANCE_TO_SCREEN);
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
    addEventLink(MouseMovedEvent.class, this, "processMouseMoved");
    addEventLink(MouseExitedEvent.class, this, "mouseExited");
    addEventLink(MouseScrolledEvent.class, this, "processMouseScrolled");
    addEventLink(MouseClickedEvent.class, this, "showPopupIfNeeded");
    getComponent().addComponentListener(new ComponentListener()
    {
      @Override
      public void componentResized(ComponentEvent componentEvent)
      {
        viewChanged(false);
      }
      @Override
      public void componentMoved(ComponentEvent componentEvent)
      {
      }
      @Override
      public void componentShown(ComponentEvent componentEvent)
      {
      }
      @Override
      public void componentHidden(ComponentEvent componentEvent)
      {
      }
    });
    orientation = 0;
    perspective = false;
    scale = 100.0;
    setCueColors();
    mouseMoveTimer.setCoalesce(false);
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
        @Override
        public void run()
        {
          mouseDragged(ev);
        }
      });
  }

  private void processMouseMoved(final MouseMovedEvent ev)
  {
    if (mouseProcessor != null)
      mouseProcessor.stopProcessing();
    mouseProcessor = new ActionProcessor();
    mouseProcessor.addEvent(new Runnable() {
      @Override
      public void run()
      {
        mouseMoved(ev);
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

  /**
    Processing scrollwheel events here. Subclasses may override.
  */
  protected void processMouseScrolled(MouseScrolledEvent e)
  {
    if (scrollTool == null)
      return;

    if (mouseProcessor != null)
      mouseProcessor.stopProcessing();

    mouseProcessor = new ActionProcessor();
    if (e.isAltDown())
        scrollBuffer += e.getWheelRotation();
    else
        scrollBuffer += e.getWheelRotation()*10;
    final ViewerCanvas viewToProcess = this;
    final MouseScrolledEvent scrollEvent = e;
    mouseProcessor.addEvent(new Runnable() {
      @Override
      public void run()
      {
        scrollTool.mouseScrolled(scrollEvent, viewToProcess);
      }
    });
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
  protected void mouseMoved(MouseMovedEvent ev)
  {
  }

  /** Subclasses should override this to handle events. */

  protected void mouseReleased(WidgetMouseEvent ev)
  {
  }

  /** Subclasses may override but should call super first */

  protected void mouseExited(WidgetMouseEvent ev)
  {
    // Mouse may exit the view while scroll wheel is rotating
    // and the state would be left incorrect, allowing unintended
    // drawing of frustumShape.

    scrolling = false;
  }

  /** This needs to be overridden, since the component may not be a JComponent. */

  @Override
  public void setPreferredSize(Dimension size)
  {
    prefSize = new Dimension(size);
  }

  @Override
  public Dimension getPreferredSize()
  {
    return new Dimension(prefSize);
  }

  @Override
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

  /**
    For object editors this tool is set in the abstract ObjectEditorWindow,
    whereas the alt- and meta tools are set in each sub(-sub-sub) class of it.
    This way it gets inherited to plugin tools too (like PME).
  */

  public void setScrollTool(ScrollViewTool tool)
  {
    scrollTool = tool;
  }

  /** Set the animation engine */

  public void setViewAnimation(ViewAnimation ani)
  {
    animation = ani;
  }

  /** Get the animation engine */

  public ViewAnimation getViewAnimation()
  {
    return animation;
  }

  /**
    Set whether to display perspective or parallel mode. <p>

    When the mode changes the view scale and camera distance from
    drawing plane are recalculated so that the perceived scale on
    the drawing plane does not change.<p>

    For animated perspective changes the view has to be in perspective during the animation and
    the perspective-parameter is turned false at finishAnimation() if needed. The perspectiveSwitch-
    parameter tells the users last perspehtive selection during animation.
   */

  public void setPerspective(boolean nextPerspective)
  {
    // Can't not go parallel in travel modes
    if (navigation == NAVIGATE_TRAVEL_SPACE || navigation == NAVIGATE_TRAVEL_LANDSCAPE)
      return;

    // don't recalculate if not necessary
    if (perspectiveSwitch == nextPerspective){
      return;
    }

    // if the view is not up yet
    if (getBounds().height == 0 || getBounds().width == 0 || theCamera == null){
      perspective = perspectiveSwitch = nextPerspective;
      return;
    }

    // SceneCamera has logic of it's own, so just jump there!
    if (boundCamera != null && boundCamera.getObject() instanceof SceneCamera){
      perspective = perspectiveSwitch = nextPerspective;
      return;
    }

    if (animation.animatingMove() || animation.changingPerspective())
      return;

    perspectiveSwitch = nextPerspective;
    animation.start(nextPerspective);
  }

  /**
    This is needed when animated perspective change parallel to perspective begins.
    The scale and perspective parameters can not be accessed directly form the
    animation engine and they take immediade effect on the camera.
   */

  public void preparePerspectiveAnimation()
  {
    scale = 100.0;
    perspective = true;
  }

  /**
    ViewAnimation calls this when the animation is finished, so the orientation menu
    will be up to date and the perspective is set to it's value without
    launching a new animation sequence.
  */

  public void finishAnimation(int which, boolean persp, int navi)
  {
    orientation = which;
    perspective = persp;
    navigation = navi;
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

  /** Check what the perespective was set to last */
  public boolean isPerspectiveSwitch()
  {
    return perspectiveSwitch;
  }

  /** Get the current scale factor for the view. */

  public double getScale()
  {
    return scale;
  }

  /** Set the scale factor for the view. */

  public void setScale(double scale)
  {
    if (scale > 0.0)
      this.scale = scale;
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
    Get the location around which the view should be rotated.  This may be null, in which case the value
    returned by {@link #getDefaultRotationCenter()} will be used instead.
   */

  public Vec3 getRotationCenter()
  {
    return rotationCenter;
  }

  /**
    Set the location around which the view should be rotated. This may be null, in which case the value
    returned by {@link #getDefaultRotationCenter()} will be used instead.
   */

  public void setRotationCenter(Vec3 rotationCenter)
  {
    this.rotationCenter = rotationCenter;
  }

  /**
    This method will be called if {@link #getRotationCenter()} returns null.

    It should be made sure that the rotation center is always known and never null, but if
    null is returned this recreates it and returns the new value
   */

  public Vec3 getDefaultRotationCenter()
  {
    CoordinateSystem coords = theCamera.getCameraCoordinates();
    double distToCenter = -coords.getZDirection().dot(coords.getOrigin());
    setRotationCenter(coords.getOrigin().plus(coords.getZDirection().times(distToCenter)));
    return getRotationCenter();
  }

  /**
    Set the distance from drawing plane to view camera. <p>

    Depending on the view action this may be used to recalculate
    the position of the camera or the rotationCenter. Conversely, it may be
    recalculated from the rotationCenter and camera position.
   */
  public void setDistToPlane(double dist)
  {
    distToPlane = dist;

    if (boundCamera != null)
      if (boundCamera.getObject() instanceof SceneCamera)
        ((SceneCamera)boundCamera.getObject()).setDistToPlane(dist);
      else if (boundCamera.getObject() instanceof SpotLight)
        ((SpotLight)boundCamera.getObject()).setDistToPlane(dist);
      else if (boundCamera.getObject() instanceof DirectionalLight)
        ((DirectionalLight)boundCamera.getObject()).setDistToPlane(dist);
  }

  /** Get the distance from camera to drawing plane */

  public double getDistToPlane()
  {
    if (boundCamera != null)
        if (boundCamera.getObject() instanceof SceneCamera)
            return ((SceneCamera)boundCamera.getObject()).getDistToPlane();
        else if (boundCamera.getObject() instanceof SpotLight)
            return ((SpotLight)boundCamera.getObject()).getDistToPlane();
        else if (boundCamera.getObject() instanceof DirectionalLight)
            return ((DirectionalLight)boundCamera.getObject()).getDistToPlane();
    return distToPlane;
  }

  /** Get the currently used navigation mode */
  public int getNavigationMode()
  {
    return navigation;
  }

  /**
    Set navigation mode

    @param nextNavigation may be one of
      NAVIGATE_MODEL_SPACE = 0;
      NAVIGATE_MODEL_LANDSCAPE = 1;
      NAVIGATE_TRAVEL_SPACE = 2;
      NAVIGATE_TRAVEL_LANDSCAPE = 3;

    Setting the value higher than 3 will have MoveViewTool and RotateViewTool ignore mouse commands.
    This may be helpful for plug-in added navigation modes.

    If the view is in tilted orientation and then set to 'landscape' the tilt angle will be reset
    and the view set to y = up.
   */

  public void setNavigationMode(int nextNavigation)
  {
    if (nextNavigation == navigation)
      return;

    if (nextNavigation < 2)
      setNavigationMode(nextNavigation, perspectiveSwitch);
    else
      setNavigationMode(nextNavigation, true);
  }

  /** Set navigation mode and perspective */

  public void setNavigationMode(int nextNavigation, boolean nextPerspective)
  {
    // If not changing, do nothing
    if (nextNavigation == navigation)
      return;

    // If the view is not up yet, just set the parameters
    if (getBounds().height == 0 || getBounds().width == 0 || theCamera == null)
    {
      navigation = nextNavigation;
      if (navigation > 1)
        perspective = true;
      return;
    }

    // Change perspective without animation. This is a bit blunt but softer
    // options would need a more complex set of conditions.

    if (nextNavigation < 2)
      perspective = nextPerspective;
    else
      perspective = true;
    if (perspective)
      scale = 100;
    else
      scale = 100.0*theCamera.getDistToScreen()/distToPlane;

    // If changing from a 3D-mode to a landscape mode,
    // animate the turning of y-direction up.

    if ((navigation == 0 || navigation == 2) && (nextNavigation == 1 || nextNavigation == 3))
    {
      CoordinateSystem coords = theCamera.getCameraCoordinates().duplicate();
      Vec3 z  = coords.getZDirection();
      Vec3 up = coords.getUpDirection();

      // If camera z-axis is aligned with world y-axis the orientation is good for
      // landscape modes.

      if (Math.abs(z.y) == 1.0)
      {
        // When z.y == 1.0, z.x and z.x may still have a value in the
        // magnitude of 1E-15.

        z.x = z.z = 0.0;
        up.y = 0.0;
        if (up.length() == 0.0) // Just a precaution. Should never happen.
          up.z = 1.0;
        up.normalize();
        coords.setOrientation(z, up);
        coords.setOrigin(rotationCenter.minus(z.times(distToPlane)));
        theCamera.setCameraCoordinates(coords);
        navigation = nextNavigation;
        viewChanged(false);
        repaint();
      }
      else
      {
        // Start with up = vertical direction and turn it prpendicular to z

        up.x = up.z = 0.0;
        up.y = 1.0;
        up.subtract(z.times(up.dot(z)));
        up.normalize();
        coords.setOrientation(z, up);
        animation.start(coords, rotationCenter, scale, orientation, nextNavigation);
      }
    }
    else
    {
      navigation = nextNavigation;
      viewChanged(false);
    }
    repaint();
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

  /** Matching the camera with the cirrent state of the view */ // I guess?

  public void adjustCamera(boolean perspective)
  {
    Rectangle bounds = getBounds();
    double scale = getScale();

    if (boundCamera != null && boundCamera.getObject() instanceof SceneCamera){
      theCamera.setScreenTransform(((SceneCamera) boundCamera.getObject()).getScreenTransform(bounds.width, bounds.height), bounds.width, bounds.height);
    }
    else if (perspective)
      theCamera.setScreenParams(scale, bounds.width, bounds.height);
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

  /** Set grid sown or not */
  public void setShowGrid(boolean show)
  {
    showGrid = show;
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

  /** 
      Sub classes that can handle a partially hidden mesh should override this.
   */

  public boolean[] getHiddenRenderingTriangles()
  {
    return null;
  }

  /**
      @deprecated <p>
      Use {@link #fitToObjects} instead.<p>

      Adjust the camera position and magnification so that the specified box
      fills the view.  This has no effect if there is a camera bound to this
      view.
  */

  @Deprecated
  public void frameBox(BoundingBox bb)
  {
    if (boundCamera != null)
      return;

    // Move the camera so that it points at the center of the box, and is well outside it.

    Rectangle bounds = getBounds();
    if (isPerspective())
      theCamera.setScreenParams(100.0, bounds.width, bounds.height);
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
    minScale = Math.min(minScale, theCamera.getDistToScreen()*100.0/MINIMUM_ZOOM_DISTANCE);
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

  /**
    Fit view to the selected MeshVertices OR the entire mesh.<p>

    @param selection If set to true, fits to selected vertices else
    fit to the entire mesh.
   */

  public void fitToVertices(MeshEditorWindow w, boolean selection)
  {
    BoundingBox b = boundsOfSelection(w, selection);
    if (b == null)
      return;

    Vec3 newCenter = new Vec3(b.getCenter());
    CoordinateSystem newCoords = theCamera.getCameraCoordinates().duplicate();
    int d = Math.min(getBounds().width, getBounds().height);
    Vec3 size = b.getSize();
    double diag = Math.sqrt(size.x*size.x+size.y*size.y+size.z*size.z);
    double newDistToPlane = 100*theCamera.getDistToScreen() / (double)d / 0.9 * diag;

    // The calling I know of have limits of their own but this method is available for others.

    newDistToPlane = Math.max(newDistToPlane, MINIMUM_ZOOM_DISTANCE);
    newCoords.setOrigin(newCenter.plus(newCoords.getZDirection().times(-newDistToPlane-(b.maxz-b.minz) * 0.5)));
    double newScale;
    if (perspective)
      newScale = 100.0;
    else
      newScale = 100.0*theCamera.getDistToScreen()/newDistToPlane;
    animation.start(newCoords, newCenter, newScale, orientation, navigation);
  }

  /** Sub classes that can handle bones needs to override this */

  public void fitToBone(ObjectInfo info)
  {
  }

  /** Create a bounding box for  selected vertices */

  public BoundingBox boundsOfSelection(MeshEditorWindow w, boolean selection)
  {
    Mesh mesh = (Mesh) w.getObject().getObject();
    MeshVertex vert[] = mesh.getVertices();
    int selected[] = w.getSelectionDistance();
    double minx, miny, minz, maxx, maxy, maxz;
    boolean anything = false;
    minx = miny = minz = Double.MAX_VALUE;
    maxx = maxy = maxz = -Double.MAX_VALUE;
    Mat4 t = ((ObjectViewer)w.getView()).getDisplayCoordinates().fromLocal();

    for (int i = 0; i < vert.length; i++)
    {
      if (selected[i] == 0 || !selection)
      {
        anything = true;
        Vec3 v = t.times(vert[i].r);
        if (v.x < minx) minx = v.x;
        if (v.x > maxx) maxx = v.x;
        if (v.y < miny) miny = v.y;
        if (v.y > maxy) maxy = v.y;
        if (v.z < minz) minz = v.z;
        if (v.z > maxz) maxz = v.z;
      }
    }
    if (anything)
    {
      // This is in case you have selected only on vertex.
      // The size of it would be zero --> ViewerCavas would go  blank
      // The zero size should be handled in the calling method

      if (maxx-minx < 0.001) {maxx += 0.0005; minx -= 0.0005;}
      if (maxy-miny < 0.001) {maxy += 0.0005; miny -= 0.0005;}
      if (maxz-minz < 0.001) {maxz += 0.0005; minz -= 0.0005;}

      return new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
    }
    else
      return null;
  }

  /**
     Fit view to the given set of objects.<p>
     Each object is given the space of a sphere, that would just fits tt's bounding box.
   */

  public void fitToObjects(Collection<ObjectInfo> objects)
  {
    if (objects.size() == 0)
      return;

    CoordinateSystem newCoords;
    BoundingBox b;
    double br, z; // box radius, view z coordinate
    Vec3 bc;      // box center
    Vec3 cx, cy, cz, newCenter;
    cz = theCamera.getCameraCoordinates().getZDirection();
    cy = theCamera.getCameraCoordinates().getUpDirection();
    cx = cz.cross(cy);
    Vec2 p;  // x-y-point in view space
    Mat4 toScene, worldToView, viewToWorld;
    worldToView = theCamera.getWorldToView();
    viewToWorld = theCamera.getViewToWorld();
    int w = getBounds().width;
    int h = getBounds().height;
    int d = Math.min(w, h);

    double minx, miny, minz, maxx, maxy, maxz;
    minx = miny = minz = Double.POSITIVE_INFINITY;
    maxx = maxy = maxz = Double.NEGATIVE_INFINITY;

    for (ObjectInfo info : objects)
    {
      b = info.getBounds();
      bc = b.getCenter();
      br = Math.sqrt((b.maxx-b.minx)*(b.maxx-b.minx)+(b.maxy-b.miny)*(b.maxy-b.miny)+(b.maxz-b.minz)*(b.maxz-b.minz)) / 2.0;
      toScene = info.getCoords().fromLocal();
      toScene.transform(bc);

      p = worldToView.timesXY(bc.plus(cx.times(-br))); // In view space x is 'backwards'.
      maxx = Math.max(maxx, p.x);
      p = worldToView.timesXY(bc.plus(cx.times(br)));
      minx = Math.min(minx, p.x);

      p = worldToView.timesXY(bc.plus(cy.times(br)));
      maxy = Math.max(maxy, p.y);
      p = worldToView.timesXY(bc.plus(cy.times(-br)));
      miny = Math.min(miny, p.y);

      z = worldToView.timesZ(bc.plus(cz.times(br)));
      maxz= Math.max(maxz, z);
      z = worldToView.timesZ(bc.plus(cz.times(-br)));
      minz = Math.min(minz, z);
    }

    newCenter = new Vec3((minx+maxx)*0.5, (miny+maxy)*0.5, (minz+maxz)*0.5);
    viewToWorld.transform(newCenter);

    double projectionDist;
    if (boundCamera != null && boundCamera.getObject() instanceof SceneCamera)
    {
      double compAngle = (Math.PI - Math.toRadians(((SceneCamera)boundCamera.getObject()).getFieldOfView()))/2;
      projectionDist = Math.tan(compAngle)*getBounds().height/2/100;
    }
    else
      projectionDist = theCamera.getDistToScreen();

    double newDistToPlane = 100*projectionDist/(double)d/0.9*Math.max(maxx-minx, maxy-miny)+(maxz-minz)*0.5;
    newDistToPlane = Math.max(newDistToPlane, MINIMUM_ZOOM_DISTANCE); // Attempt to zoom to zero size blanks the view.
    double newScale;
    if (perspective)
      newScale = 100.0;
    else
      newScale = 100.0*projectionDist/newDistToPlane;
    newCoords = theCamera.getCameraCoordinates().duplicate();
    newCoords.setOrigin(newCenter.plus(newCoords.getZDirection().times(-newDistToPlane)));

    animation.start(newCoords, newCenter, newScale, orientation, navigation);
  }

  /** 
     Turn the view to match the closest main coordinate directions, 
     which maybe positive or negative.
   */
  public void alignWithClosestAxis()
  {
    CoordinateSystem coords = theCamera.getCameraCoordinates().duplicate();
    int newOrientation = ViewerCanvas.VIEW_OTHER;
    Vec3 zDir = coords.getZDirection();
    Vec3 upDir = coords.getUpDirection();
    Vec3 center;

    Vec3 zNew  =  new Vec3(0,0,zDir.z);
    if (Math.abs(zDir.x) > Math.abs(zDir.z)) zNew  =  new Vec3(zDir.x,0,0);
    if (Math.abs(zDir.y) > Math.abs(zDir.z) && Math.abs(zDir.y) > Math.abs(zDir.x)) zNew  =  new Vec3(0,zDir.y,0);

    Vec3 upNew  =  new Vec3(0,upDir.y,0);
    if (Math.abs(upDir.z) > Math.abs(upDir.y)) upNew  =  new Vec3(0,0,upDir.z);
    if (Math.abs(upDir.x) > Math.abs(upDir.y) && Math.abs(upDir.x) > Math.abs(upDir.z)) upNew  =  new Vec3(upDir.x,0,0);

    zNew.normalize();
    upNew.normalize();

    // Sometimes the new Up- and Z-directions end up the same or opposite.
    // Then keep the one that was closer to what was found and find the next closest to the other one.

    if (zNew.equals(upNew) || zNew.equals(upNew.times(-1)))
    {
      if (Math.max(Math.max(Math.abs(zDir.x),Math.abs(zDir.y)),Math.abs(zDir.z)) < Math.max(Math.max(Math.abs(upDir.x),Math.abs(upDir.y)),Math.abs(upDir.z)))
        zNew = findNextAxis(zDir, upNew);
      else
        upNew = findNextAxis(upDir, zNew);
    }
    if (zNew.equals(upNew) || zNew.equals(upNew.times(-1))) // A safety measure. Should not be needed
      return;

    center = rotationCenter.plus(zNew.times(-distToPlane));
    coords.setOrientation(zNew,upNew);
    coords.setOrigin(center);

    if (theCamera == null)
    {
      if (zNew.z == -1 && upNew.y ==  1) newOrientation = 0; // Front
      if (zNew.z ==  1 && upNew.y ==  1) newOrientation = 1; // Back
      if (zNew.x ==  1 && upNew.y ==  1) newOrientation = 2; // Left
      if (zNew.x == -1 && upNew.y ==  1) newOrientation = 3; // Right
      if (zNew.y == -1 && upNew.z == -1) newOrientation = 4; // Top
      if (zNew.y ==  1 && upNew.z ==  1) newOrientation = 5; // Bottom
    }
    else
      newOrientation = orientation;

    animation.start(coords, rotationCenter, scale, newOrientation, navigation);
  }

  private Vec3 findNextAxis(Vec3 dir, Vec3 fixed)
  {
    double maxD = Math.max(Math.max(Math.abs(dir.x),Math.abs(dir.y)),Math.abs(dir.z));
    double minD = Math.min(Math.min(Math.abs(dir.x),Math.abs(dir.y)),Math.abs(dir.z));
    Vec3 nextDir;

    if (Math.abs(dir.x) != maxD && Math.abs(dir.x) != minD) nextDir = new Vec3(dir.x,0,0);
    else if (Math.abs(dir.y) != maxD && Math.abs(dir.y) != minD) nextDir = new Vec3(0,dir.y,0);
    else nextDir = new Vec3(0,0,dir.z);
    nextDir.normalize();

    // There still is the possibility that the directions are same
    // try in a different order

    if (nextDir.equals(fixed) || nextDir.equals(fixed.times(-1)))
      if (Math.abs(dir.z) != maxD && Math.abs(dir.z) != minD) nextDir = new Vec3(0,0,dir.z); // already z ?
      else if (Math.abs(dir.y) != maxD && Math.abs(dir.y) != minD) nextDir = new Vec3(0,0,dir.y); // already checked
      else nextDir = new Vec3(0,0,dir.x); // this would be definitely different.
    nextDir.normalize();

    return nextDir;
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
    // Animation tracks may tilt the camera and the y-up navigation modes may not be
    // appropriate. This check tries to use the user's last selection if possible -- Otherwise selects the
    // correcponding 3D-mode

    if (boundCamera != null && (lastSetNavigation == 1 || lastSetNavigation == 3))
      if (theCamera.getCameraCoordinates().getRotationAngles()[2] == 0.0) // The rotation angles of the boundCamera are checked elsewhere
        navigation = lastSetNavigation;
      else
        navigation = lastSetNavigation-1;

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

    if (showGrid && !animation.animatingMove() && !animation.changingPerspective())
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
    coordinateAxes.draw();
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

  /**
     Adjust the coordinates of a mouse event to move it to the nearest
     grid location.
   */

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
    Get the current orientation mode.
   */

  public int getOrientation()
  {
    return orientation;
  }

  /**
     Launch an orientation change procedure to turn the orientation into any of the
     presets shown in the drop-down menu.

     This method calls the ViewAnimation to perform the turn, if needed.
   */

  public void setOrientation(int which)
  {
    if (orientation == which || which > 5)
      return;
    CoordinateSystem coords = new CoordinateSystem();
    Vec3 center = new Vec3(getRotationCenter());
    if (which == 0)             // Front
    {
      center.z += distToPlane;
      coords = new CoordinateSystem(center, new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    }
    else if (which == 1)        // Back
    {
      center.z -= distToPlane;
      coords = new CoordinateSystem(center, Vec3.vz(), Vec3.vy());
    }
    else if (which == 2)        // Left
    {
      center.x -= distToPlane;
      coords = new CoordinateSystem(center, Vec3.vx(), Vec3.vy());
    }
    else if (which == 3)        // Right
    {
      center.x += distToPlane;
      coords = new CoordinateSystem(center, new Vec3(-1.0, 0.0, 0.0), Vec3.vy());
    }
    else if (which == 4)        // Top
    {
      center.y += distToPlane;
      coords = new CoordinateSystem(center, new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 0.0, -1.0));
    }
    else if (which == 5)        // Bottom
    {
      center.y -= distToPlane;
      coords = new CoordinateSystem(center, Vec3.vy(), Vec3.vz());
    }
    animation.start(coords, rotationCenter, scale, which, navigation);
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

  /**
    Center the view to a point in the model space. No need to
    override by subclasses. Local coordinates are handled
    by the ClickedPointFinder, 'finder'.
   */

  public void centerToPoint(Point pointOnView)
  {
    Vec3 pointInSpace = finder.newPoint(this, pointOnView);
    CoordinateSystem coords = theCamera.getCameraCoordinates().duplicate();
    Vec3 cz = coords.getZDirection();
    if (perspective)
      distToPlane = coords.getOrigin().minus(pointInSpace).length();
    Vec3 cp = pointInSpace.plus(cz.times(-distToPlane));
    coords.setOrigin(cp);

    if(animation != null) animation.start(coords, pointInSpace, scale, orientation, navigation);
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

  /** Get the centermost point of the screen */

  public Point getViewCenter()
  {
    int x = getBounds().width;
    int y = getBounds().height;
    int cx = x/2;
    int cy = y/2;
    return (new Point(cx, cy));
  }

  private void drawNavigationCues()
  {
    if (tilting || moving || rotating) return;
    if (! ArtOfIllusion.getPreferences().getShowTravelCuesOnIdle() &&
    (! ArtOfIllusion.getPreferences().getShowTravelCuesScrolling() || ! scrolling))
      return;

    int d = Math.min(getBounds().width, getBounds().height);
    Point viewCenter = getViewCenter();
    Color cueColor;

    if (scrolling){
      cueColor = cueActive;
    }
    else{
      cueColor = cueIdle;
    }
    if (navigation == 2){
      drawCircle(viewCenter, d*0.1, 30, cueColor);
      drawCircle(viewCenter, d*0.4, 60, cueColor);
    }
    if (navigation == 3){
      drawSquare(viewCenter, d*0.1, cueColor);
      drawSquare(viewCenter, d*0.4, cueColor);
    }
  }

  protected Timer mouseMoveTimer = new Timer(500, new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      mouseMoving = false;
      mouseMoveTimer.stop();
      mousePoint = null;
      repaint();
    }
  });

  /* Draw a point in the modelling space on the screen */

  private void renderPoint(Vec3 p, Color c, int radius)
  {
    Vec2 ps = getCamera().getWorldToScreen().timesXY(p);
    drawLine(new Point((int)ps.x, (int)ps.y), new Point((int)ps.x+radius+1, (int)ps.y), c);
    drawLine(new Point((int)ps.x, (int)ps.y), new Point((int)ps.x-radius, (int)ps.y), c);
    drawLine(new Point((int)ps.x, (int)ps.y), new Point((int)ps.x, (int)ps.y+radius+1), c);
    drawLine(new Point((int)ps.x, (int)ps.y), new Point((int)ps.x, (int)ps.y-radius), c);
  }

  /**
  *  Draw a line on the screen as segment of a radius from a center point.
  *  Angle ar radii and r0 and r1 as screen (pixel) coordinate scale.
  */

  public void drawLine(Point center, double angle, double r0, double r1, Color color)
  {
    Point p0, p1;
    p0 = new Point(center.x + (int)(Math.cos(angle)*r0), center.y + (int)(Math.sin(angle)*r0));
    p1 = new Point(center.x + (int)(Math.cos(angle)*r1), center.y + (int)(Math.sin(angle)*r1));

    drawLine(p0, p1, color);
  }

  /** Draw a circle on the screen out of small pieces of lines*/

  public void drawCircle(Point center, double radius, int segments, Color color)
  {
    Point p0, p1;

    for (int i = 0; i < segments; i++)
    {
      double a0 = 2*Math.PI/segments*i;
      double x0 = Math.cos(a0)*radius;
      double y0 = Math.sin(a0)*radius;
      double a1 = 2*Math.PI/segments*(i+1);
      double x1 = Math.cos(a1)*radius;
      double y1 = Math.sin(a1)*radius;

      p0 = new Point(center.x + (int)x0, center.y + (int)y0);
      p1 = new Point(center.x + (int)x1, center.y + (int)y1);

      drawLine(p0 ,p1, color);
    }
  }

  /** draw a suare on the screen */

  public void drawSquare(Point center, double distance, Color color)
  {
    Point p0, p1;
    double x0 = center.x-(int)(distance);
    double y0 = center.y-(int)(distance);
    double x1 = center.x+(int)(distance);
    double y1 = center.y+(int)(distance);
    p0 = new Point((int)x0, (int)y0);
    p1 = new Point((int)x1, (int)y0);
    drawLine(p0 ,p1, color);
    p0 = new Point((int)x0, (int)y1);
    p1 = new Point((int)x1, (int)y1);
    drawLine(p0 ,p1, color);
    p0 = new Point((int)x0, (int)y0);
    p1 = new Point((int)x0, (int)y1);
    drawLine(p0 ,p1, color);
    p0 = new Point((int)x1, (int)y0);
    p1 = new Point((int)x1, (int)y1);
    drawLine(p0 ,p1, color);
  }

  /* These colors are used on the navigation cue graphics */

  private void setCueColors()
  {
    CUE       = new Color(0, 127, 255);
    BEAM      = new Color(159, 255, 0);
    cueIdle   = new Color(CUE.getRed()  *1/4+backgroundColor.getRed()  *3/4,
              CUE.getGreen()*1/4+backgroundColor.getGreen()*3/4,
              CUE.getBlue() *1/4+backgroundColor.getBlue() *3/4);
    cueActive = new Color(CUE.getRed()  *1/2+backgroundColor.getRed()  *1/2,
              CUE.getGreen()*1/2+backgroundColor.getGreen()*1/2,
              CUE.getBlue() *1/2+backgroundColor.getBlue() *1/2);
    gray      = new Color(Color.GRAY.getRed()  *1/2+backgroundColor.getRed()  *1/2,
              Color.GRAY.getGreen()*1/2+backgroundColor.getGreen()*1/2,
              Color.GRAY.getBlue() *1/2+backgroundColor.getBlue() *1/2);
    red       = new Color(Color.RED.getRed()   *3/4+backgroundColor.getRed()  *1/4,
              Color.RED.getGreen() *3/4+backgroundColor.getGreen()*1/4,
              Color.RED.getBlue()  *3/4+backgroundColor.getBlue() *1/4);
    green     = new Color(Color.GREEN.getRed()   *3/4+backgroundColor.getRed()  *1/4,
              Color.GREEN.getGreen() *3/4+backgroundColor.getGreen()*1/4,
              Color.GREEN.getBlue()  *3/4+backgroundColor.getBlue() *1/4);
    blue      = new Color(Color.BLUE.getRed()  *3/4+backgroundColor.getRed()  *1/4,
              Color.BLUE.getGreen()*3/4+backgroundColor.getGreen()*1/4,
              Color.BLUE.getBlue() *3/4+backgroundColor.getBlue() *1/4);
    beam      = new Color(BEAM.getRed()  *2/4+backgroundColor.getRed()  *2/4,
              BEAM.getGreen()*2/4+backgroundColor.getGreen()*2/4,
              BEAM.getBlue() *2/4+backgroundColor.getBlue() *2/4);
  }

  /*
  * Blend between two colors.
  * The 'blend' is the balance of the color in range 0.0 - 1.0 from color0 to color1
  */

  private Color blendColor(Color color0, Color color1, double blend)
  {
    int R = (int)(color0.getRed()*(1.0-blend) + color1.getRed()*blend);
    int G = (int)(color0.getGreen()*(1.0-blend) + color1.getGreen()*blend);
    int B = (int)(color0.getBlue()*(1.0-blend) + color1.getBlue()*blend);

    return new Color(R, G, B);
  }

  /** Draw informative graphics on the screen */

  public void drawOverlay(ViewerCanvas viewToDrawOn)
  {
    setCueColors(); // This should be set at preferences change
    if (viewToDrawOn == this)
      drawNavigationCues();
    else
      frustumShape.draw(viewToDrawOn);
  }

  /**
    FrustumShape is the visulization of the 'drawingplane' and the position 
    of view camera. The ViewerCanvas parameter 'distToPlane' defines the 
    distance between the drawing plane and the theCamera.
  */

  public class FrustumShape
  {
    private Vec3[] corners = new Vec3[4];;
    private Vec3 viewingPoint3D;
    private Vec2 vector2D; // used in conversions 3D to screen 2D
    private Color CUEBACK, cueBack, eyeBright, eyeDim;
    private Color planeColor, shapeColor, centerColor, eyeColor;

    private FrustumShape()
    {
    }

    private void setPlaneColors()
    {
      CUEBACK   = new Color(255, 127, 0);
      cueBack   = new Color(CUEBACK.getRed()  *1/2+backgroundColor.getRed()  *1/2,
                            CUEBACK.getGreen()*1/2+backgroundColor.getGreen()*1/2,
                            CUEBACK.getBlue() *1/2+backgroundColor.getBlue() *1/2);
      eyeBright = Color.MAGENTA;
      eyeDim    = new Color(eyeBright.getRed()  *1/2+backgroundColor.getRed()  *1/2,
                            eyeBright.getGreen()*1/2+backgroundColor.getGreen()*1/2,
                            eyeBright.getBlue() *1/2+backgroundColor.getBlue() *1/2);
    }

    void update()
    {
      Rectangle b = getBounds();

      Vec3 camZ = theCamera.getCameraCoordinates().getZDirection();
      double ds = theCamera.getDistToScreen();
      corners[0] = theCamera.convertScreenToWorld(new Point(0, 0), distToPlane, false);
      corners[1] = theCamera.convertScreenToWorld(new Point(b.width, 0), distToPlane, false);
      corners[2] = theCamera.convertScreenToWorld(new Point(b.width, b.height), distToPlane, false);
      corners[3] = theCamera.convertScreenToWorld(new Point(0, b.height), distToPlane, false);
      viewingPoint3D = theCamera.getCameraCoordinates().getOrigin();
    }

    /** The direction in the scene from the user's eye to the view. */

    public Vec3 getViewingDirection()
    {
      return new Vec3(theCamera.getCameraCoordinates().getZDirection());
    }

    /** Corners of this drawing plane in scene coordinates. */

    public Vec3[] getCorners()
    {
      return corners;
    }

    public double size()
    {
      return corners[2].minus(corners[0]).length();
    }

    public boolean smallEnough(ViewerCanvas toDrawOn, double limit)
    {
      return (size() <= toDrawOn.frustumShape.size()*limit);
    }

    /**
      Draw a 2D projection of this plane on the given ViewerCancvas
    */

    public void draw(ViewerCanvas viewToDrawOn)
    {
      if (! ArtOfIllusion.getPreferences().getDrawActiveFrustum() &&
         (! ArtOfIllusion.getPreferences().getDrawCameraFrustum() || boundCamera == null))
        return;
      if (! moving && ! rotating && ! scrolling)
        return;

      viewToDrawOn.frustumShape.update();
      update();
      if (! smallEnough(viewToDrawOn, 1.25))
        return;

      Point[] cornerPoints = new Point[4];
      Point viewPoint, rotPoint, tipPoint;
      setCueColors();
      setPlaneColors();

      Vec3 viewZ = viewToDrawOn.getCamera().getCameraCoordinates().getZDirection();
      double viewCos = viewZ.dot(getViewingDirection());
      if (viewCos < 0.0)
      {
        centerColor = CUEBACK;
        planeColor  = cueBack;
        eyeColor    = eyeDim;
      }
      else
      {
        centerColor = CUE;
        planeColor  = cueActive;
        eyeColor    = eyeBright;
      }

      // This makes the color of the edges of the shape fade when it is
      // pointed toward or away from the camera of view it is drawn on.

      double viewCos2;
      if (viewCos < 0.0)
        viewCos2 = Math.abs(viewCos*0.67);
      else
        viewCos2 = Math.pow(viewCos, 10.0);

      shapeColor = new Color((int)(planeColor.getRed()  *(1-viewCos2) + backgroundColor.getRed()  *viewCos2),
                             (int)(planeColor.getGreen()*(1-viewCos2) + backgroundColor.getGreen()*viewCos2),
                             (int)(planeColor.getBlue() *(1-viewCos2) + backgroundColor.getBlue() *viewCos2));

      Mat4 worldToScreen = viewToDrawOn.getCamera().getWorldToScreen();

      for (int c = 0; c < cornerPoints.length; c++)
      {
        vector2D = worldToScreen.timesXY(corners[c]);
        cornerPoints[c] = new Point((int)Math.round(vector2D.x), (int)Math.round(vector2D.y));
      }

      vector2D = worldToScreen.timesXY(rotationCenter);
      rotPoint = new Point((int)Math.round(vector2D.x), (int)Math.round(vector2D.y));
      vector2D = worldToScreen.timesXY(viewingPoint3D);
      viewPoint = new Point((int)Math.round(vector2D.x), (int)Math.round(vector2D.y));

      // Darw a flat shape when the view is in orthographic mode

      if (isPerspective())
        tipPoint = viewPoint;
      else
        tipPoint = rotPoint;

      if (boundCamera != null && !(boundCamera.getObject() instanceof SceneCamera))
        viewToDrawOn.drawLine(rotPoint, viewPoint, beam);

      for (int c = 0; c < 4; c++)
      {
        // Could add 3D-feel by first drawing a faint shape as overlay and then
        // redrawing by rendeLine() with a thicker color. Unfortunately the depth map of
        // SWDrawer is inaccurate and the GL-drawer has a shift of one or two pixels in the
        // interpretation of drawing coordinates

        if (getViewingDirection().dot(viewToDrawOn.getCamera().getCameraCoordinates().getZDirection()) < 0.985)
        {
          viewToDrawOn.drawLine(cornerPoints[c], tipPoint, shapeColor);
          drawMarker(viewToDrawOn, viewPoint, 7, eyeColor, true);
        }
        viewToDrawOn.drawLine(cornerPoints[c], cornerPoints[(c+1)%4], planeColor);
      }
      drawMarker(viewToDrawOn, rotPoint, 7, centerColor, false);
    }

    /**
      Draw a marker on the screen.<p>

      The drawing has to happen pixel by pixel because the canvasdrawers
      handle 'drawLine()' with a different logic.<p>

      A marker is a small cross, drawn either upright or diagonally.
    */

    private void drawMarker(ViewerCanvas v, Point p, int size, Color c, boolean diag)
    {
      int s = size/2;
      v.drawLine(p, new Point(p.x+1, p.y), c);
      if (diag)
        for (int i = 1; i <= s; i++)
        {
          v.drawLine(new Point(p.x+i, p.y+i), new Point(p.x+i+1, p.y+i), c);
          v.drawLine(new Point(p.x-i, p.y+i), new Point(p.x-i+1, p.y+i), c);
          v.drawLine(new Point(p.x+i, p.y-i), new Point(p.x+i+1, p.y-i), c);
          v.drawLine(new Point(p.x-i, p.y-i), new Point(p.x-i+1, p.y-i), c);
        }
      else
        for (int i = 1; i <= s; i++)
        {
          v.drawLine(new Point(p.x+i, p.y), new Point(p.x+i+1, p.y), c);
          v.drawLine(new Point(p.x-i, p.y), new Point(p.x-i+1, p.y), c);
          v.drawLine(new Point(p.x, p.y+i), new Point(p.x+1, p.y+i), c);
          v.drawLine(new Point(p.x, p.y-i), new Point(p.x+1, p.y-i), c);
        }
    }
  }

  /** 
    The coordinate axes (or more precisely 'coordinate directions indicator') as an inner 
    class to keep it's variables away from the rest of the ViewerCanvas.
  */

  private class CoordinateAxes
  {
    private static final int coordinateAxisSize = 30;
    private BufferedImage image;
    private Graphics2D graphics;
    private int axisLength, imageSize, labelD;
    private Vec2 label2D, axis2D;
    private Color axisColor,lastBGColor, lastLColor;
    private Point imagePos, labelPos, imageCenter;
    private Rectangle bounds;
    private CoordinateSystem cameraCoords;
    private Mat4 rotate;
    private int axis, i, labelPoint[][][], dc[];
    private final Vec3 axis3D[] = new Vec3[]{Vec3.vx(), Vec3.vy(), Vec3.vz()};
    private final Vec3 origin = new Vec3();

    CoordinateAxes()
    {
      setDimensions();
      setColors();
    }

    void draw()
    {
      if (axisLength != coordinateAxisSize || bounds.x != getBounds().width || bounds.y != getBounds().height)
        setDimensions();
      if (lastBGColor.getRGB() != backgroundColor.getRGB() || lastLColor.getRGB() != lineColor.getRGB())
        setColors();
      cameraCoords = theCamera.getCameraCoordinates();
      rotate = Mat4.viewTransform(origin, cameraCoords.getZDirection(), cameraCoords.getUpDirection().times(-1));
      graphics.clearRect(0,0,imageSize-1,imageSize-1);
      graphics.setColor(axisColor);
      for(axis = 0; axis < 3; axis++)
      {
        axis2D = rotate.timesXY(axis3D[axis]).times(axisLength);
        label2D = rotate.timesXY(axis3D[axis]).times(axisLength+labelD*2);
        graphics.drawLine(imageCenter.x, imageCenter.y, point(axis2D, imageCenter).x,  point(axis2D, imageCenter).y);
        if (label2D.minus(axis2D).length() > (double)labelD/4.0)
        {
          labelPos = point(label2D, imageCenter);
          for (i = 0; i < labelPoint[axis].length; i++)
          {
            dc = drawingCoords(labelPoint[axis][i], labelPos);
            graphics.drawLine(dc[0], dc[1], dc[2], dc[3]);
          }
        }
      }
      drawImage(image, imagePos.x, imagePos.y);
    }

    private void setDimensions()
    {
      bounds = getBounds();
      axisLength = Math.min(coordinateAxisSize, Math.min(bounds.width/6, bounds.height/6));
      labelD = (axisLength < 45) ? 6 : 8; // Indexing. The size is 7 or 9 pixels.
      imageSize = (axisLength+(labelD+1)*3+1)*2+1; // Make sure it's odd, so the center is really drawn in the middle
      imageCenter = new Point(imageSize/2, imageSize/2);
      imagePos = new Point(bounds.width-imageSize, bounds.height-imageSize);

      // The instrucuions to draw x, y and z
      labelPoint = new int[3][][];
      labelPoint[0] = new int[][]{{0,0,labelD,labelD},{0,labelD,labelD,0}};
      labelPoint[1] = new int[][]{{0,0,labelD/2,labelD/2},{labelD/2,labelD/2,labelD,0},{labelD/2,labelD/2,labelD/2,labelD}};
      labelPoint[2] = new int[][]{{0,0,labelD,0},{0,labelD,labelD,0},{0,labelD,labelD,labelD}};

      image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
      graphics = image.createGraphics();
      graphics.setBackground(new Color(0,0,0,0));
    }

    private void setColors()
    {
      axisColor = new Color(backgroundColor.getRed()*3/8 + lineColor.getRed()*5/8, 
                            backgroundColor.getGreen()*3/8 + lineColor.getGreen()*5/8, 
                            backgroundColor.getBlue()*3/8 + lineColor.getBlue()*5/8,
                            255);
      lastBGColor = backgroundColor;
      lastLColor = lineColor;
    }

    private Point point(Vec2 v2, Point p)
    {
      return new Point ((int)v2.x+p.x, (int)v2.y+p.y);
    }

    private int[] drawingCoords(int[] a, Point p)
    {
      return new int[]{a[0]+p.x-labelD/2, a[1]+p.y-labelD/2, a[2]+p.x-labelD/2, a[3]+p.y-labelD/2};
    }
  }
}

/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.event.*;

import java.awt.*;
import java.io.*;

import artofillusion.math.*;
import artofillusion.*;

import javax.imageio.*;

/**
 * This class displays a set of handles around a selection in a ViewerCanvas.  It processes
 * mouse clicks on them, and translates them into higher level events which are dispatched for
 * further processing, most often by an EditingTool.  As the name suggests, this manipulator can
 * display up to nine handles, corresponding to the eight compass points plus center.
 */

public class NinePointManipulator extends EventSource implements Manipulator
{
  private final Image images[];
  private Rectangle screenBounds;
  private BoundingBox selectionBounds;
  private HandlePosition handle;
  private int imageWidth, imageHeight;

  public static final Image ARROWS_N_S = loadImage("arrows_N_S.gif");
  public static final Image ARROWS_E_W = loadImage("arrows_E_W.gif");
  public static final Image ARROWS_NW_SE = loadImage("arrows_NW_SE.gif");
  public static final Image ARROWS_NE_SW = loadImage("arrows_NE_SW.gif");
  public static final Image ARROWS_N_W = loadImage("arrows_N_W.gif");
  public static final Image ARROWS_N_E = loadImage("arrows_N_E.gif");
  public static final Image ARROWS_S_W = loadImage("arrows_S_W.gif");
  public static final Image ARROWS_S_E = loadImage("arrows_S_E.gif");
  public static final Image ARROWS_ALL = loadImage("arrows_all.gif");

  public static final Image ROTATE_TOP = loadImage("rotate_top.gif");
  public static final Image ROTATE_BOTTOM = loadImage("rotate_bottom.gif");
  public static final Image ROTATE_LEFT = loadImage("rotate_left.gif");
  public static final Image ROTATE_RIGHT = loadImage("rotate_right.gif");
  public static final Image ROTATE_TOPLEFT = loadImage("rotate_topleft.gif");
  public static final Image ROTATE_TOPRIGHT = loadImage("rotate_topright.gif");
  public static final Image ROTATE_BOTTOMLEFT = loadImage("rotate_bottomleft.gif");
  public static final Image ROTATE_BOTTOMRIGHT = loadImage("rotate_bottomright.gif");

  public static final HandlePosition NW = new HandlePosition(0, 0);
  public static final HandlePosition N = new HandlePosition(1, 0);
  public static final HandlePosition NE = new HandlePosition(2, 0);
  public static final HandlePosition W = new HandlePosition(0, 1);
  public static final HandlePosition CENTER = new HandlePosition(1, 1);
  public static final HandlePosition E = new HandlePosition(2, 1);
  public static final HandlePosition SW = new HandlePosition(0, 2);
  public static final HandlePosition S = new HandlePosition(1, 2);
  public static final HandlePosition SE = new HandlePosition(2, 2);

  private static final HandlePosition ALL_HANDLES[] = new HandlePosition[] {NW, N, NE, W, CENTER, E, SW, S, SE};
  private static final int MARGIN = 4;

  private static Image loadImage(String name)
  {
    try
    {
      return ImageIO.read(NinePointManipulator.class.getResource("/artofillusion/ui/manipulatorIcons/"+name));
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    return null;
  }

  /**
   * Create a new NinePointManipulator.
   *
   * @param images     an array of images to use for the nine handles in the following order:
   *                   NW, N, NE, W, CENTER, E, SW, S, SE.  If an element is null, there will
   *                   not be any handle at the corresponding position.  All images should be
   *                   of the same size.
   */

  public NinePointManipulator(Image images[])
  {
    this.images = (Image[]) images.clone();
    for (int i = 0; i < images.length; i++)
      if (images[i] != null)
      {
        imageWidth = images[i].getWidth(null);
        imageHeight = images[i].getHeight(null);
      }
  }

  /**
   * Draw the handles onto a ViewerCanvas.
   *
   * @param view             the canvas onto which to draw the handles
   * @param selectionBounds  a BoundingBox enclosing whatever is selected in the canvas
   */

  public void draw(ViewerCanvas view, BoundingBox selectionBounds)
  {
    Rectangle r = findScreenBounds(selectionBounds, view.getCamera());
    if (r == null)
      return;
    int x[] = new int[] {r.x, r.x+(r.width-imageWidth)/2, r.x+r.width-imageWidth};
    int y[] = new int[] {r.y, r.y+(r.height-imageHeight)/2, r.y+r.height-imageHeight};
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
      {
        int index = j*3+i;
        if (images[index] != null)
          view.drawImage(images[index], x[i], y[j]);
      }
  }

  /**
   * This should be invoked when the mouse is pressed.  It determines whether the mouse was over
   * a handle, and if so, generates a HandlePressedEvent.
   *
   * @param ev               the event which has occurred
   * @param view             the ViewerCanvas in which the event occurred
   * @param selectionBounds  a BoundingBox enclosing whatever is selected in the canvas
   * @return true if the mouse was pressed on a handle, false otherwise
   */

  public boolean mousePressed(WidgetMouseEvent ev, ViewerCanvas view, BoundingBox selectionBounds)
  {
    handle = null;
    Rectangle r = findScreenBounds(selectionBounds, view.getCamera());
    Point pos = ev.getPoint();
    if (r == null || !r.contains(pos))
      return false;
    int x = -1, y = -1;
    if (pos.x < r.x+imageWidth)
      x = 0;
    else if (pos.x > r.x+(r.width-imageWidth)/2 && pos.x < r.x+(r.width+imageWidth)/2)
      x = 1;
    else if (pos.x > r.x+r.width-imageWidth)
      x = 2;
    if (pos.y < r.y+imageWidth)
      y = 0;
    else if (pos.y > r.y+(r.height-imageHeight)/2 && pos.y < r.y+(r.height+imageHeight)/2)
      y = 1;
    else if (pos.y > r.y+r.height-imageHeight)
      y = 2;
    if (x == -1 || y == -1 || images[x+y*3] == null)
      return false;
    screenBounds = r;
    this.selectionBounds = selectionBounds;
    handle = ALL_HANDLES[y*3+x];
    dispatchEvent(new HandlePressedEvent(view, handle, screenBounds, selectionBounds, ev));
    return true;
  }

  /**
   * This should be invoked when the mouse is dragged.  If the drag began with the mouse over
   * a handle, this generates a HandleDraggedEvent.
   *
   * @param ev      the event which has occurred
   * @param view    the ViewerCanvas in which the event occurred
   */

  public void mouseDragged(WidgetMouseEvent ev, ViewerCanvas view)
  {
    if (handle != null)
      dispatchEvent(new HandleDraggedEvent(view, handle, screenBounds, selectionBounds, ev));
  }

  /**
   * This should be invoked when the mouse is released.  If the drag began with the mouse over
   * a handle, this generates a HandleReleasedEvent.
   *
   * @param ev      the event which has occurred
   * @param view    the ViewerCanvas in which the event occurred
   */

  public void mouseReleased(WidgetMouseEvent ev, ViewerCanvas view)
  {
    if (handle != null)
      dispatchEvent(new HandleReleasedEvent(view, handle, screenBounds, selectionBounds, ev));
    handle = null;
  }

  /**
   * Given a bounding box in view coordinates, find the corresponding rectangle in
   * screen coordinates.
   */

  private Rectangle findScreenBounds(BoundingBox b, Camera cam)
  {
    Mat4 m = cam.getObjectToWorld();
    cam.setObjectTransform(cam.getViewToWorld());
    Rectangle r = cam.findScreenBounds(b);
    cam.setObjectTransform(m);
    if (r != null)
      r.setBounds(r.x-imageWidth-MARGIN, r.y-imageHeight-MARGIN, r.width+2*imageWidth+2*MARGIN, r.height+2*imageHeight+2*MARGIN);
    return r;
  }

  /**
   * Instances of this class represent the nine handle positions.
   */

  public static class HandlePosition
  {
    private int x, y;

    private HandlePosition(int x, int y)
    {
      this.x = x;
      this.y = y;
    }

    /**
     * Return true if this is the NW, W, or SW position.
     */

    public boolean isWest()
    {
      return (x == 0);
    }

    /**
     * Return true if this is the NE, E, or SE position.
     */

    public boolean isEast()
    {
      return (x == 2);
    }

    /**
     * Return true if this is the NW, N, or NE position.
     */

    public boolean isNorth()
    {
      return (y == 0);
    }

    /**
     * Return true if this is the SW, S, or SE position.
     */

    public boolean isSouth()
    {
      return (y == 2);
    }
  }

  /**
   * This is the superclass of the various events generated by the manipulator.
   */

  public class HandleEvent
  {
    private ViewerCanvas view;
    private HandlePosition handle;
    private Rectangle screenBounds;
    private BoundingBox selectionBounds;
    private WidgetMouseEvent event;

    private HandleEvent(ViewerCanvas view, HandlePosition handle, Rectangle screenBounds, BoundingBox selectionBounds, WidgetMouseEvent event)
    {
      this.view = view;
      this.handle = handle;
      this.screenBounds = screenBounds;
      this.selectionBounds = selectionBounds;
      this.event = event;
    }

    /**
     * Get the ViewerCanvas in which this event occurred.
     */

    public ViewerCanvas getView()
    {
      return view;
    }

    /**
     * Get the handle being manipulated.
     */

    public HandlePosition getHandle()
    {
      return handle;
    }

    /**
     * Get the bounding box of the manipulator on screen at the time the mouse was first clicked.
     */

    public Rectangle getScreenBounds()
    {
      return new Rectangle(screenBounds);
    }

    /**
     * Get the bounding box in view coordinates of the selection at the time the mouse was first clicked.
     */

    public BoundingBox getSelectionBounds()
    {
      return new BoundingBox(selectionBounds);
    }

    /**
     * Get the original mouse event responsible for this event being generated.
     */

    public WidgetMouseEvent getMouseEvent()
    {
      return event;
    }

    /**
     * Get the manipulator which generated this event.
     */

    public NinePointManipulator getManipulator()
    {
      return NinePointManipulator.this;
    }
  }

  /**
   * This is the event class generated when the user clicks on a handle.
   */

  public class HandlePressedEvent extends HandleEvent
  {
    private HandlePressedEvent(ViewerCanvas view, HandlePosition handle, Rectangle screenBounds, BoundingBox selectionBounds, WidgetMouseEvent event)
    {
      super(view, handle, screenBounds, selectionBounds, event);
    }
  }

  /**
   * This is the event class generated when the user drags on a handle.
   */

  public class HandleDraggedEvent extends HandleEvent
  {
    private HandleDraggedEvent(ViewerCanvas view, HandlePosition handle, Rectangle screenBounds, BoundingBox selectionBounds, WidgetMouseEvent event)
    {
      super(view, handle, screenBounds, selectionBounds, event);
    }
  }

  /**
   * This is the event class generated when the user releases on a handle.
   */

  public class HandleReleasedEvent extends HandleEvent
  {
    private HandleReleasedEvent(ViewerCanvas view, HandlePosition handle, Rectangle screenBounds, BoundingBox selectionBounds, WidgetMouseEvent event)
    {
      super(view, handle, screenBounds, selectionBounds, event);
    }
  }
}

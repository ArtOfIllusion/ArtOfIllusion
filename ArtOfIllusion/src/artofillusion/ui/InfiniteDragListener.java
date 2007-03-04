/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.event.*;
import buoy.widget.*;

import javax.swing.*;
import java.awt.*;

/**
 * This class implements "infinite" mouse drags, which are not restricted by the boundaries
 * of the screen.  It is useful when you want to use mouse drags to control something other than
 * the cursor position.  It accomplishes this by repeatedly moving the cursor back to the original
 * click position, while generating its own MouseDraggedEvents and MouseReleasedEvents as if the
 * cursor were freely moving over an unbounded screen.
 */

public class InfiniteDragListener extends EventSource
{
  private final Widget source;
  private Robot robot;
  private Point startPoint, startScreenPoint;
  private int dx, dy;
  private boolean enabled;

  /**
   * Create an InfiniteDragListener.
   *
   * @param source    the Widget on which the user may click to start an infinite drag
   */

  public InfiniteDragListener(Widget source)
  {
    this.source = source;
    enabled = true;
    source.addEventLink(MousePressedEvent.class, this, "mousePressed");
    source.addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
    source.addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
  }

  /**
   * Set whether the InfiniteDragListener is enabled.  When disabled, it does not generate any
   * events or affect the mouse position.
   */

  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  /**
   * Get whether the InfiniteDragListener is enabled.  When disabled, it does not generate any
   * events or affect the mouse position.
   */

  public boolean isEnabled()
  {
    return enabled;
  }

  private void mousePressed(MousePressedEvent ev)
  {
    if (!enabled)
      return;
    startPoint = ev.getPoint();
    startScreenPoint = new Point(startPoint);
    SwingUtilities.convertPointToScreen(startScreenPoint, source.getComponent());
    dx = 0;
    dy = 0;
    try
    {
      robot = new Robot(source.getComponent().getGraphicsConfiguration().getDevice());
    }
    catch (Exception ex)
    {
      robot = null;
    }
    dispatchEvent(ev);
  }

  private void mouseDragged(MouseDraggedEvent ev)
  {
    if (!enabled)
      return;
    dispatchEvent(new MouseDraggedEvent(source, ev.getWhen(), ev.getModifiersEx(), ev.getX()+dx, ev.getY()+dy));
    if ((Math.abs(ev.getX()-startPoint.x) > 5 || Math.abs(ev.getY()-startPoint.y) > 5) && robot != null)
    {
      dx += ev.getX()-startPoint.x;
      dy += ev.getY()-startPoint.y;
      robot.mouseMove(startScreenPoint.x, startScreenPoint.y);
    }
  }

  private void mouseReleased(MouseReleasedEvent ev)
  {
    robot = null;
    if (!enabled)
      return;
    dispatchEvent(new MouseReleasedEvent(source, ev.getWhen(), ev.getModifiersEx(), ev.getX()+dx, ev.getY()+dy, ev.getClickCount(), ev.isPopupTrigger(), ev.getButton()));
  }
}

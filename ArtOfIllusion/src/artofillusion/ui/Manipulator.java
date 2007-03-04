/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.*;
import artofillusion.math.*;
import buoy.event.*;

/**
 * A Manipulator is a class which presents a user interface in a {@link ViewerCanvas}.  It is
 * typically used by an {@link EditingTool}.
 */

public interface Manipulator
{
  /**
   * Draw the manipulator's user interface into the canvas.
   *
   * @param view      the canvas this manipulator is displayed in
   * @param selectionBounds   a bounding box containing everything that is selected in the canvas
   */

  void draw(ViewerCanvas view, BoundingBox selectionBounds);

  /**
   * Respond to mouse presses in the canvas.
   *
   * @param ev        the event which has occurred
   * @param view      the canvas this manipulator is displayed in
   * @param selectionBounds   a bounding box containing everything that is selected in the canvas
   * @return true if the manipulator has handled the event, false otherwise
   */

  boolean mousePressed(WidgetMouseEvent ev, ViewerCanvas view, BoundingBox selectionBounds);

  /**
   * Respond to mouse drags in the canvas.
   *
   * @param ev         the event which has occurred
   * @param view      the canvas this manipulator is displayed in
   */

  void mouseDragged(WidgetMouseEvent ev, ViewerCanvas view);

  /**
   * Respond to mouse releases in the canvas.
   *
   * @param ev        the event which has occurred
   * @param view      the canvas this manipulator is displayed in
   */

  void mouseReleased(WidgetMouseEvent ev, ViewerCanvas view);
}

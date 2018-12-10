/* Copyright (C) 2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.*;
import buoy.widget.*;

/**
 * A ViewerControl defines a Widget that is added to the toolbar at the top of each
 * ViewerCanvas.  After creating a ViewerControl, invoke {@link ViewerCanvas#addViewerControl(ViewerControl)}
 * to register it.  Every time a new ViewerCanvas is then created,
 * {@link #createWidget(artofillusion.ViewerCanvas) createWidget()} will be invoked
 * to create a control for that canvas.
 */

public interface ViewerControl
{
  /**
   * This is invoked each time a new ViewerCanvas is created.  It should create a Widget
   * which is then added to the canvas' toolbar.
   * <p>
   * It is permitted for this method to return null.  This allows a ViewerControl to only
   * add Widgets to particular types of ViewerCanvases.
   */

  Widget createWidget(ViewerCanvas view);

  /**
   * Get the name of this ViewerControl, as it should be displayed in the user interface.
   */

  String getName();
}

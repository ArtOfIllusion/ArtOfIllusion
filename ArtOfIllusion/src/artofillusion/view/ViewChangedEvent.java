/* Copyright (C) 2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.*;

/**
 * A ViewChangedEvent is dispatched by a ViewerCanvas to indicate that some element of the
 * view settings has changed.  This includes the camera position or orientation, the zoom
 * level, the projection mode, etc.
 */

public class ViewChangedEvent
{
  private ViewerCanvas source;

  /**
   * Create a ViewChangedEvent.
   */

  public ViewChangedEvent(ViewerCanvas source)
  {
    this.source = source;
  }

  /**
   * Get the ViewerCanvas which generated this event.
   */

  public ViewerCanvas getSource()
  {
    return source;
  }
}

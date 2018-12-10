/* Copyright (C) 2006-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.ui.*;

/**
 * A SceneChangedEvent is dispatched by an EditingWindow to indicate that some element of the
 * scene has changed.  This includes all aspects of the scene, including the list of objects,
 * properties of individual objects, textures, the list of currently selected objects, etc.
 */

public class SceneChangedEvent
{
  private EditingWindow window;

  public SceneChangedEvent(EditingWindow window)
  {
    this.window = window;
  }

  /**
   * Get the LayoutWindow containing the scene which was changed.
   */

  public EditingWindow getWindow()
  {
    return window;
  }
}

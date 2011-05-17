/* Copyright (C) 1999-2001,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.*;
import buoy.widget.*;

/** EditingWindow represents a window used for editing an object or scene. */

public interface EditingWindow
{
  /** Get the ToolPalette for this window. */

  public ToolPalette getToolPalette();

  /** Set the currently selected EditingTool. */
  
  public void setTool(EditingTool tool);
  
  /** Set the text to display at the bottom of the window. */
  
  public void setHelpText(String text);
  
  /** Get the BFrame for this EditingWindow: either the EditingWindow itself if it is a
      BFrame, or its parent if it is a BDialog. */
  
  public BFrame getFrame();

  /** Update the image displayed in this window. */

  public void updateImage();

  /** Update which menus are enabled. */

  public void updateMenus();
  
  /** Set the current UndoRecord for this EditingWindow. */
  
  public void setUndoRecord(UndoRecord command);

  /** Register that the scene or object contained in the window has been modified. */
  
  public void setModified();
  
  /** Get the Scene which is being edited in this window.  If it is not a window for
      editing a scene, this should return null. */

  public Scene getScene();
  
  /** Get the ViewerCanvas in which editing is taking place.  This may return null
      if there is no ViewerCanvas. */
  
  public ViewerCanvas getView();

  /** Get all ViewerCanvases contained in this window.  This may return null
      if there is no ViewerCanvas. */

  public ViewerCanvas[] getAllViews();

  /** Confirm whether this window should be closed (possibly by displaying a message to the
      user), and then close it.  If the closing is canceled, this should return false. */

  public boolean confirmClose();
}
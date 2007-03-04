/* Copyright (C) 2002-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.widget.*;

/** This interface represents an object which creates and manages a popup menu for
    some other component. */

public interface PopupMenuManager
{
  /** Display the PopupMenu. */
  
  public void showPopupMenu(Widget w, int x, int y);
}
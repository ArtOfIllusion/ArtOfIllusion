/* Copyright (C) 2001,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.widget.*;
import java.awt.*;

/* This is a Widget which acts as a spacer.  It displays a blank background, and copies
   its width and height from specified other Widgets. */

public class Spacer extends CustomWidget
{
  private Widget vertical, horizontal;
  
  public Spacer(Widget copyHoriz, Widget copyVert)
  {
    horizontal = copyHoriz;
    vertical = copyVert;
  }
  
  public Dimension getPreferredSize()
  {
    Dimension v = vertical.getPreferredSize(), h = horizontal.getPreferredSize();
    
    return new Dimension(h.width, v.height);
  }
}

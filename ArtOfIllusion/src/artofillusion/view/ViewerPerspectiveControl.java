/* Copyright (C) 2007-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.object.*;
import buoy.widget.*;
import buoy.event.*;
import artofillusion.*;
import artofillusion.ui.*;

/**
 * This is a ViewerControl for setting whether a view uses perspective mode.
 */

public class ViewerPerspectiveControl implements ViewerControl
{
  public Widget createWidget(final ViewerCanvas view)
  {
    final BComboBox perspectiveChoice = new BComboBox(new String [] {
      Translate.text("Perspective"),
      Translate.text("Parallel")
    });
    perspectiveChoice.setSelectedIndex(1);
    view.addEventLink(ViewChangedEvent.class, new Object() {
      void processEvent()
      {
        if (view.getBoundCamera() != null && view.getBoundCamera().getObject() instanceof SceneCamera)
          perspectiveChoice.setEnabled(false);
        else if (view.getRenderMode() == ViewerCanvas.RENDER_RENDERED)
          perspectiveChoice.setEnabled(false);
        else
        {
          perspectiveChoice.setEnabled(true);
          perspectiveChoice.setSelectedIndex(view.isPerspective() ? 0 : 1);
        }
      }
    });
    perspectiveChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        boolean perspective = (perspectiveChoice.getSelectedIndex() == 0);
        if (view.isPerspective() != perspective)
          view.setPerspective(perspective);
      }
    });
    return perspectiveChoice;
  }

  public String getName()
  {
    return Translate.text("Perspective");
  }
}

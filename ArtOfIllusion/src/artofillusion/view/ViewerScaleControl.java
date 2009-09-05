/* Copyright (C) 2007-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import buoy.widget.*;
import buoy.event.*;
import artofillusion.*;
import artofillusion.ui.*;

/**
 * This is a ViewerControl for adjusting the scale of the view.
 */

public class ViewerScaleControl implements ViewerControl
{
  public Widget createWidget(final ViewerCanvas view)
  {
    final ValueField scaleField = new ValueField(100.0, ValueField.POSITIVE, 5);
    scaleField.setText("100");
    scaleField.setMinDecimalPlaces(1);
    view.addEventLink(ViewChangedEvent.class, new Object() {
      void processEvent()
      {
        if (view.isPerspective() || view.getBoundCamera() != null)
          scaleField.setEnabled(false);
        else
        {
          scaleField.setEnabled(true);
          if (view.getScale() != scaleField.getValue())
            scaleField.setValue(view.getScale());
        }
      }
    });
    scaleField.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        view.setScale(scaleField.getValue());
      }
    });
    return scaleField;
  }


  public String getName()
  {
    return Translate.text("Magnification");
  }
}

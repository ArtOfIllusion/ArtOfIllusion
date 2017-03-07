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
import artofillusion.math.*;

/**
 * This is a ViewerControl for adjusting the scale of the view.
 */

public class ViewerScaleControl implements ViewerControl
{
  @Override
  public Widget createWidget(final ViewerCanvas view)
  {
    final ValueField scaleField = new ValueField(100.0, ValueField.POSITIVE, 5);
    scaleField.setText("100");
    scaleField.setMinDecimalPlaces(1);
    view.addEventLink(ViewChangedEvent.class, new Object() {
      void processEvent()
      {
        if (view.isPerspective() || view.getBoundCamera() != null)
		{
          //scaleField.setEnabled(false);
		  scaleField.setValue(view.getDistToPlane());
		}
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
  		// This should be animated, but can not at the moment:
		// The animation starts at first digit and as the animaton starts 
		// the value begins to change, before you have finished writing.

		if (! view.isPerspective())
		{
          view.setScale(scaleField.getValue());
		  view.repaint(); 
		}
		else
		{
			view.setDistToPlane(scaleField.getValue());
			if (view.getNavigationMode() == 0 || view.getNavigationMode() == 1)
			{
				CoordinateSystem coords = view.getCamera().getCameraCoordinates().duplicate();
				Vec3 rc = view.getRotationCenter();
				Vec3 cc = rc.plus(coords.getZDirection().times(-view.getDistToPlane()));
				coords.setOrigin(cc);
				view.getCamera().setCameraCoordinates(coords);
				view.repaint();
				
				// view.getViewAnimation().start(view, coords, rc, view.getScale(), view.getOrientation());
			}
			else
			{
				CoordinateSystem coords = view.getCamera().getCameraCoordinates().duplicate();
				Vec3 rc = coords.getOrigin().plus(coords.getZDirection().times(view.getDistToPlane()));
				view.setRotationCenter(rc);
				view.repaint();
			}
		}
      }
    });
    return scaleField;
  }

  @Override
  public String getName()
  {
    return Translate.text("Magnification");
  }
}

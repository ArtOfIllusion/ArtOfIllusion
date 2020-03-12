/* Copyright (C) 2007-2009 by Peter Eastman
   Modifications copyright (C) 2017-2020 Petri Ihalainen

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
  private ViewerCanvas view;
  private ValueField scaleField;

  @Override
  public Widget createWidget(final ViewerCanvas view)
  {
    this.view = view;
    final ValueField scaleField = new ValueField(100.0, ValueField.POSITIVE, 5);
    this.scaleField = scaleField;
    scaleField.setText("100");
    scaleField.setMinDecimalPlaces(1);
    scaleField.setValueChecker(new ZoomChecker(view));
    view.addEventLink(ViewChangedEvent.class, new Object() {
      void processEvent()
      {
        if (view.isPerspective() || view.getBoundCamera() != null)
        {
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
        if (!scaleField.isValid(scaleField.getValue()))
          return;

        if (! view.isPerspective())
        { 
          view.getViewAnimation().start(view.getCamera().getCameraCoordinates(), view.getRotationCenter(), 
                                        scaleField.getValue(), view.getOrientation(), view.getNavigationMode() );
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
            view.getViewAnimation().start(coords, rc, view.getScale(), view.getOrientation(), view.getNavigationMode());
          }
          else
          {
            CoordinateSystem coords = view.getCamera().getCameraCoordinates().duplicate();
            Vec3 rc = coords.getOrigin().plus(coords.getZDirection().times(view.getDistToPlane()));
            view.getViewAnimation().start(coords, rc, view.getScale(), view.getOrientation(), view.getNavigationMode());
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

  private class ZoomChecker implements ValueChecker
  {
    private ViewerCanvas view;

    ZoomChecker(ViewerCanvas view)
    {
      this.view = view;
    }

    @Override
    public boolean isValid(double value)
    {
      if (view.isPerspective())
        return value >= ViewerCanvas.MINIMUM_ZOOM_DISTANCE;
      return view.getCamera().getDistToScreen()*100/value >= ViewerCanvas.MINIMUM_ZOOM_DISTANCE;
    }
  }
}

/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.widget.*;
import buoy.event.*;
import buoy.xml.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

/**
 * This class is used for selecting a numeric value within a (possibly unbounded) range.  The user
 * may edit the value either by typing into a text field, or by clicking and dragging on an
 * "adjuster" Widget.  This class is similar to JSlider, but it can be used when the permitted values
 * are not restricted to a finite range.
 */

public class ValueSelector extends RowContainer
{
  private final ValueField field;
  private final CustomWidget adjuster;
  private double minimum, maximum, increment;
  private int lastX;
  private final InfiniteDragListener drag;

  private static Cursor BLANK_CURSOR;
  private static Icon DIAL_IMAGE;
  private static Icon KNOB_IMAGE;

  static
  {
    try
    {
      Image image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
      BLANK_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(), "");
      DIAL_IMAGE = new IconResource("artofillusion/Icons/dial.png");
      KNOB_IMAGE = new IconResource("artofillusion/Icons/dialKnob.png");
    }
    catch (Exception ex)
    {
    }
  }

  /**
   * Create a ValueSelector.
   *
   * @param value       the initial value
   * @param min         the minimum legal value.  Specify Double.NEGATIVE_INFINITY if the range
   *                    is not bounded from below.
   * @param max         the maximum legal value.  Specify Double.POSITIVE_INFINITY if the range
   *                    is not bounded from above.
   * @param increment   the amount by which the value should change for each pixel the mouse is
   *                    moved after clicking on the adjustor Widget
   */

  public ValueSelector(double value, double min, double max, double increment)
  {
    minimum = min;
    maximum = max;
    this.increment = increment;
    add(field = new ValueField(value, ValueField.NONE));
    field.setValueChecker(new ValueChecker()
    {
      public boolean isValid(double val)
      {
        return (val >= minimum && val <= maximum);
      }
    });
    field.addEventLink(ValueChangedEvent.class, this, "dispatchValueChanged");
    adjuster = new CustomWidget();
    adjuster.addEventLink(RepaintEvent.class, this, "paintDial");
    adjuster.setPreferredSize(new Dimension(DIAL_IMAGE.getIconWidth(), DIAL_IMAGE.getIconHeight()));
    add(adjuster);
    drag = new InfiniteDragListener(adjuster);
    drag.addEventLink(MousePressedEvent.class, this, "mousePressed");
    drag.addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
    drag.addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
  }

  /**
   * Get the current value.
   */

  public double getValue()
  {
    return field.getValue();
  }

  /**
   * Set the current value.
   */

  public void setValue(double value)
  {
    field.setValue(value);
    adjuster.repaint();
  }

  /** Get the minimum allowed value. */

  public double getMinimumValue()
  {
    return minimum;
  }

  /** Set the minimum allowed value. */

  public void setMinimumValue(double min)
  {
    this.minimum = min;
  }

  /** Get the maximum allowed value. */

  public double getMaximumValue()
  {
    return maximum;
  }

  /** Set the maximum allowed value. */

  public void setMaximumValue(double max)
  {
    this.maximum = max;
  }

  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    field.setEnabled(enabled);
    drag.setEnabled(enabled);

  }

  private void mousePressed(MousePressedEvent ev)
  {
    lastX = ev.getX();
    setCursor(BLANK_CURSOR);
  }

  private void mouseDragged(MouseDraggedEvent ev)
  {
    if (Double.isNaN(field.getValue()))
      return;
    double scale = (ev.isAltDown() ? 10.0 : 1.0);
    double value = field.getValue()+scale*increment*(ev.getX()-lastX);
    if (value < minimum)
      value = minimum;
    if (value > maximum)
      value = maximum;
    field.setValue(value);
    lastX = ev.getX();
    dispatchValueChanged(ev);
    adjuster.repaint();
  }

  private void mouseReleased(MouseReleasedEvent ev)
  {
    setCursor(null);
    if (!Double.isNaN(field.getValue()))
      dispatchValueChanged(ev);
  }

  /**
   * Dispatch a ValueChangedEvent.
   */

  private void dispatchValueChanged(WidgetEvent originalEvent)
  {
    if (originalEvent.getWidget() == field)
      adjuster.repaint();
    dispatchEvent(new ValueChangedEvent(this, originalEvent instanceof MouseDraggedEvent));
  }

  /**
   * Paint the adjuster widget.
   */

  private void paintDial(RepaintEvent ev)
  {
    DIAL_IMAGE.paintIcon(adjuster.getComponent(), ev.getGraphics(), 0, 0);
    if (!Double.isNaN(field.getValue()) && isEnabled())
    {
      double angle = 0.005*Math.PI*field.getValue()/increment;
      int x = 8+(int) Math.round(4*Math.sin(angle));
      int y = 8-(int) Math.round(4*Math.cos(angle));
      KNOB_IMAGE.paintIcon(adjuster.getComponent(), ev.getGraphics(), x, y);
    }
  }
}

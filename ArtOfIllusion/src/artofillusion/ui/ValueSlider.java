/* Copyright (C) 1999-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/** A ValueSlider contains a BTextField and a BSlider which are together used for choosing
    a value.  Editing either one causes the other to change automatically.  If an illegal
    value is entered into the BTextField, the text turns red to indicate this. */

public class ValueSlider extends WidgetContainer
{
  BTextField field;
  BSlider slider;
  double value, min, max;
  int increments;
  boolean forceInt;
  
  public ValueSlider(double min, double max, int increments, double value)
  {
    component = new JPanel(null);
    this.value = value;
    this.min = min;
    this.max = max;
    this.increments = increments;
    field = new BTextField(5);
    slider = new BSlider((int) ((value-min)*increments/(max-min)), 0, increments+1, BSlider.HORIZONTAL);
    setText();
    field.addEventLink(ValueChangedEvent.class, this, "textChanged");
    slider.addEventLink(ValueChangedEvent.class, this, "sliderChanged");
    slider.setEnabled(!Double.isNaN(value));
    ((JPanel) component).add(field.getComponent());
    ((JPanel) component).add(slider.getComponent());
    setAsParent(field);
    setAsParent(slider);
  } 

  public void setForceInteger(boolean force)
  {
    forceInt = force;
  }

  private void sliderChanged(ValueChangedEvent ev)
  {
    double newvalue = slider.getValue()*(max-min)/increments + min;
    if (newvalue < min)
      newvalue = min;
    if (newvalue > max)
      newvalue = max;
    if (newvalue == value)
      return;
    value = newvalue;
    setText();
    field.getComponent().setForeground(Color.black);
    dispatchEvent(new ValueChangedEvent(this, ev.isInProgress()));
  }
  
  public void textChanged(ValueChangedEvent ev)
  {
    double val;
    
    try
    {
      val = new Double(field.getText()).doubleValue();
    }
    catch (NumberFormatException ex)
    {
      field.getComponent().setForeground(Color.red);
      return;
    }
    if (val < min || val > max)
      field.getComponent().setForeground(Color.red);
    else if (forceInt && val != Math.floor(val))
      field.getComponent().setForeground(Color.red);
    else
    {
      value = val;
      field.getComponent().setForeground(Color.black);
      slider.setValue((int) ((value-min)*increments/(max-min)));
      slider.setEnabled(true);
      dispatchEvent(new ValueChangedEvent(this, ev.isInProgress()));
    }
  }
  
  public double getValue()
  {
    return value;
  }
  
  public void setValue(double val)
  {
    value = val;
    slider.setValue((int) ((val-min)*increments/(max-min)));
    setText();
  }

  /** Get the minimum allowed value. */

  public double getMinimumValue()
  {
    return min;
  }

  /** Set the minimum allowed value. */

  public void setMinimumValue(double min)
  {
    this.min = min;
  }

  /** Get the maximum allowed value. */

  public double getMaximumValue()
  {
    return max;
  }

  /** Set the maximum allowed value. */

  public void setMaximumValue(double max)
  {
    this.max = max;
  }

  /** Set the contents of the text field based on the value. */
  
  private void setText()
  {
    String text;
    if (Double.isNaN(value))
      text = "";
    else if (value == 0.0 || value == -0.0)
      text = "0.0";
    else
    {
      // Make sure at least three significant digits are visible.
      
      int digits = (int) Math.floor(Math.log(Math.abs(value))/Math.log(10.0));
      double scale = Math.pow(10.0, digits < 0 ? 2-digits : 3);
      text = Double.toString(Math.round(value*scale)/scale);
    }
    if (!text.equals(field.getText()))
      field.setText(text);
  }

  public void setEnabled(boolean enabled)
  {
    field.setEnabled(enabled);
    slider.setEnabled(enabled && !Double.isNaN(value));
    super.setEnabled(enabled);
  }
    
  /**
   * Get the number of children in this container.
   */
  
  public int getChildCount()
  {
    return 2;
  }

  /**
   * Get an Iterator listing all child Widgets.
   */
  
  public Collection getChildren()
  {
    ArrayList children = new ArrayList(2);
    children.add(field);
    children.add(slider);
    return children;
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public void layoutChildren()
  {
    Dimension size = component.getSize();
    Dimension textPref = field.getPreferredSize();
    Dimension scrollPref = slider.getPreferredSize();
    field.getComponent().setBounds(new Rectangle(0, (size.height-textPref.height)/2, textPref.width, textPref.height));
    int scrollx = textPref.width+5;
    slider.getComponent().setBounds(new Rectangle(scrollx, (size.height-scrollPref.height)/2, size.width-scrollx, scrollPref.height));
  }
  
  /**
   * Do not allow children to be removed.
   */
  
  public synchronized void remove(Widget widget)
  {
  }
  
  /**
   * Do not allow children to be removed.
   */
  
  public synchronized void removeAll()
  {
  }
  
  /**
   * Get the smallest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget smaller than its minimum size.
   */
  
  public Dimension getMinimumSize()
  {
    Dimension dim1 = field.getMinimumSize();
    Dimension dim2 = slider.getMinimumSize();
    return new Dimension(dim1.width+dim2.width+5, Math.max(dim1.height, dim2.height));
  }

  /**
   * Get the preferred size at which this Widget will look best.  When a WidgetContainer lays out
   * its contents, it will attempt to make this Widget as close as possible to its preferred size.
   */
  
  public Dimension getPreferredSize()
  {
    Dimension dim1 = field.getPreferredSize();
    Dimension dim2 = slider.getPreferredSize();
    return new Dimension(dim1.width+dim2.width+5/*+increments*/, Math.max(dim1.height, dim2.height));
  }
}

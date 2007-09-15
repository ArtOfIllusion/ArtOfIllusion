/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.widget.*;
import java.awt.*;
import javax.swing.*;

/** A ValueField is a BTextField used for entering a numerical value.  Constraints can
    be specified for the value, for example, that it must be positive.  If an illegal
    value is entered into the text field, the text turns red to indicate this. */

public class ValueField extends BTextField
{
  private double value = Double.NaN;
  private int constraints, decimalPlaces = 3;
  private boolean validEventsOnly = true;
  private ValueChecker check;
  
  public static final int NONE = 0;
  public static final int NONNEGATIVE = 1;
  public static final int NONZERO = 2;
  public static final int POSITIVE = 3;
  public static final int INTEGER = 4;
  
  public ValueField(double value, int constraints)
  {
    this(value, constraints, 5);
  }

  public ValueField(float value, int constraints)
  {
    this((double) value, constraints, 5);
  }

  public ValueField(float value, int constraints, int columns)
  {
    this((double) value, constraints, columns);
  }

  public ValueField(double value, int constraints, int columns)
  {
    super(columns);
    this.constraints = constraints;
    setValue(value);
  }
  
  /** Set a ValueChecker to be used for determining whether the value is valid.  The value
      must satisfy both the ValueChecker *and* any other constraints to be considered valid. */
  
  public void setValueChecker(ValueChecker vc)
  {
    check = vc;
  }
  
  /** Get the ValueChecker for this field.  Returns null if none has been set. */
  
  public ValueChecker getValueChecker()
  {
    return check;
  }

  /** Determine whether the text entered in the field is a valid number. */
  
  public boolean isTextValid()
  {
    double val = value;
    
    try
      {
        if ((constraints & INTEGER) != 0)
          val = (double) Integer.parseInt(getText());
        else
          val = new Double(getText()).doubleValue();
      }
    catch (NumberFormatException ex)
      {
        return false;
      }
    return isValid(val);
  }
  
  /** Determine whether a particular value is valid for this field. */
  
  public boolean isValid(double val)
  {
    if (val < 0.0 && ((constraints & NONNEGATIVE) != 0))
      return false;
    else if (val == 0.0 && ((constraints & NONZERO) != 0))
      return false;
    else if (check != null && !check.isValid(val))
      return false;
    return true;
  }
  
  /** Recheck the current value to see if it is valid, and set the text color accordingly. */
  
  public void checkIfValid()
  {
    setTextColor(isValid(value));
  }
  
  /** Set the text color appropriately for whether the text is valid. */
  
  private void setTextColor(boolean valid)
  {
    ((JTextField) getComponent()).setForeground(valid ? Color.black : Color.red);
  }

  protected void textChanged()
  {
    double val = value, oldVal = value;
    
    try
    {
      if ((constraints & INTEGER) != 0)
        val = (double) Integer.parseInt(getText());
      else
        val = new Double(getText()).doubleValue();
    }
    catch (NumberFormatException ex)
    {
      setTextColor(false);
      return;
    }
    boolean suppress;
    if (!isTextValid())
    {
      setTextColor(false);
      suppress = validEventsOnly;
    }
    else
    {
      value = val;
      setTextColor(true);
      suppress = (val == oldVal);
    }
    try
    {
      if (suppress)
        suppressEvents++;
      super.textChanged();
    }
    finally
    {
      if (suppress)
        suppressEvents--;
    }
  }
  
  /** Get the current value in this field. */

  public double getValue()
  {
    return value;
  }
  
  /** Set the value in this field. */
  
  public void setValue(double val)
  {
    if (val == value)
      return;
    String text = convertNumberToString(val);
    if (text.equals(convertNumberToString(value)))
      return;
    value = val;
    setText(text);
  }

  /** Get the text representation of a number. */

  private String convertNumberToString(double val)
  {
    if (Double.isNaN(val))
      return "";
    else if ((constraints & INTEGER) != 0)
      return Integer.toString((int) val);
    else if (val == 0.0 || val == -0.0)
      return "0.0";
    else
      {
        // Make sure at least three significant digits are visible.

        int digits = (int) Math.floor(Math.log(Math.abs(val))/Math.log(10.0));
        double scale = Math.pow(10.0, digits < 0 ? decimalPlaces-1-digits : decimalPlaces);
        return Double.toString(Math.round(val*scale)/scale);
      }
  }

  /** Set the minimum number of decimal places to display. */
  
  public void setMinDecimalPlaces(int decimals)
  {
    decimalPlaces = decimals;
  }
  
  /** Set whether this field should send out all ValueChangedEvents, or only those which result
      in valid entries (the default). */
  
  public void sendValidEventsOnly(boolean validOnly)
  {
    validEventsOnly = validOnly;
  }
}

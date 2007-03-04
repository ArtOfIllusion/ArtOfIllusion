/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;

/**
 * This class defines an arbitrary property of an object.  It specifies the property name,
 * the type of property (numeric, text, boolean, etc.), and the set of allowed values.
 */

public class Property
{
  private String name;
  private PropertyType type;
  private double min, max;
  private Object allowedValues[], defaultValue;

  /**
   * A property whose values are represented by Double objects.
   */
  public static final PropertyType DOUBLE = new PropertyType();
  /**
   * A property whose values are represented by Integer objects.
   */
  public static final PropertyType INTEGER = new PropertyType();
  /**
   * A property whose values are represented by Boolean objects.
   */
  public static final PropertyType BOOLEAN = new PropertyType();
  /**
   * A property whose values are represented by String objects.
   */
  public static final PropertyType STRING = new PropertyType();
  /**
   * A property whose values are represented by RGBColor objects.
   */
  public static final PropertyType COLOR = new PropertyType();
  /**
   * A property whose values must be one of a fixed set of allowed values.
   */
  public static final PropertyType ENUMERATION = new PropertyType();

  /**
   * Create a Double valued property.
   */

  public Property(String name, double min, double max, double defaultValue)
  {
    this.name = name;
    this.min = min;
    this.max = max;
    this.defaultValue = new Double(defaultValue);
    type = DOUBLE;
  }

  /**
   * Create an Integer valued property.
   */

  public Property(String name, int min, int max, int defaultValue)
  {
    this.name = name;
    this.min = min;
    this.max = max;
    this.defaultValue = new Integer(defaultValue);
    type = INTEGER;
  }

  /**
   * Create a Boolean valued property.
   */

  public Property(String name, boolean defaultValue)
  {
    this.name = name;
    this.defaultValue = Boolean.valueOf(defaultValue);
    type = BOOLEAN;
  }

  /**
   * Create a String valued property.
   */

  public Property(String name, String defaultValue)
  {
    this.name = name;
    this.defaultValue = defaultValue;
    type = STRING;
  }

  /**
   * Create an RGBColor valued property.
   */

  public Property(String name, RGBColor defaultValue)
  {
    this.name = name;
    this.defaultValue = defaultValue.duplicate();
    type = COLOR;
  }

  /**
   * Create an enumerated property.
   */

  public Property(String name, Object allowedValues[], Object defaultValue)
  {
    this.name = name;
    this.allowedValues = (Object []) allowedValues.clone();
    this.defaultValue = defaultValue;
    type = ENUMERATION;
  }

  /**
   * Get the name of this property.
   */

  public String getName()
  {
    return name;
  }

  /**
   * Get the type of this property.
   */

  public PropertyType getType()
  {
    return type;
  }

  /**
   * Get the default value for this property.
   */

  public Object getDefaultValue()
  {
    return defaultValue;
  }

  /**
   * Get the minimum allowed value.  The return value is undefined and meaningless if this property
   * has a type other than DOUBLE or INTEGER.
   */

  public double getMinimum()
  {
    return min;
  }

  /**
   * Get the maximum allowed value.  The return value is undefined and meaningless if this property
   * has a type other than DOUBLE or INTEGER.
   */

  public double getMaximum()
  {
    return max;
  }

  /**
   * Get the list of allowed values.  The return value is undefined and meaningless if this property
   * has a type other than ENUMERATION.
   */

  public Object[] getAllowedValues()
  {
    return (Object []) allowedValues.clone();
  }

  /**
   * Determine whether an object represents a legal value for this property.
   */

  public boolean isLegalValue(Object value)
  {
    if (type == DOUBLE)
    {
      if (value instanceof Double)
      {
        double val = ((Double) value).doubleValue();
        return (val >= min && val <= max);
      }
    }
    else if (type == INTEGER)
    {
      if (value instanceof Integer)
      {
        int val = ((Integer) value).intValue();
        return (val >= min && val <= max);
      }
    }
    else if (type == BOOLEAN)
      return (value instanceof Boolean);
    else if (type == STRING)
      return (value instanceof String);
    else if (type == COLOR)
      return (value instanceof RGBColor);
    else if (type == ENUMERATION)
    {
      for (int i = 0; i < allowedValues.length; i++)
        if (allowedValues[i] == value)
          return true;
    }
    return false;
  }

  public boolean equals(Object obj)
  {
    if (!(obj instanceof Property))
      return false;
    Property prop = (Property) obj;
    if (prop.type != type || !prop.name.equals(name))
      return false;
    if (type == BOOLEAN || type == STRING || type == COLOR)
      return true;
    if (type == DOUBLE || type == INTEGER)
      return (prop.min == min && prop.max == max);
    if (prop.allowedValues.length != allowedValues.length)
      return false;
    for (int i = 0; i < allowedValues.length; i++)
      if (!prop.allowedValues[i].equals(allowedValues[i]))
        return false;
    return true;
  }

  public int hashCode()
  {
    return name.hashCode()^type.hashCode();
  }


  /**
   * Instances of this class represent specific types of properties.
   */

  public static class PropertyType
  {
    private PropertyType()
    {
    }
  }
}

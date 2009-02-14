/* Copyright (C) 2000-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;

/** This class describes a parameter which affects the appearance of an object, and
    can vary over the object's surface.  Every parameter is described by a name, the Texture
    which it belongs to, and the min, max, and default values. */

public class TextureParameter
{
  public final Object owner;
  public final String name;
  public final double minVal, maxVal, defaultVal;
  public int identifier, type;
  
  public static final int NORMAL_PARAMETER = 0;
  public static final int X_COORDINATE = 1;
  public static final int Y_COORDINATE = 2;
  public static final int Z_COORDINATE = 3;
  
  private static int nextID = 0;
  
  public TextureParameter(Object owner, String name, double minVal, double maxVal, double defaultVal)
  {
    this.owner = owner;
    this.name = name;
    this.minVal = minVal;
    this.maxVal = maxVal;
    this.defaultVal = defaultVal;
    identifier = -1;
    type = NORMAL_PARAMETER;
  }
  
  public TextureParameter duplicate()
  {
    return duplicate(owner);
  }

  /**
   * Create a TextureParameter which is a duplicate of an existing one, but with a different owner.
   */

  public TextureParameter duplicate(Object owner)
  {
    TextureParameter tp = new TextureParameter(owner, name, minVal, maxVal, defaultVal);
    tp.identifier = identifier;
    tp.type = type;
    return tp;
  }

  public boolean equals(Object o)
  {
    if (!(o instanceof TextureParameter))
      return false;
    TextureParameter param = (TextureParameter) o;
    boolean sameOwner = false;
    if (param.owner == owner)
      sameOwner = true;
    else if (param.owner instanceof Texture && owner instanceof Texture &&
        ((Texture) param.owner).getID() == ((Texture) owner).getID())
      sameOwner = true;
    else if (owner instanceof TextureMapping && owner.getClass() == param.owner.getClass())
      sameOwner = true;
    if (!sameOwner)
      return false;
    if (identifier == -1 || param.identifier == -1)
      return (param.name.equals(name));
    return (identifier == param.identifier);
  }


  public int hashCode()
  {
    if (identifier == -1)
      return name.hashCode();
    return identifier;
  }

  /** Assign a new ID number to this parameter. */
  
  public void assignNewID()
  {
    identifier = getUniqueID();
  }
  
  /** Get a unique ID number which can be assigned to a parameter. */
  
  public static synchronized int getUniqueID()
  {
    return (nextID++);
  }

  /** Set the ID number for this parameter.  (Use with extreme caution!) */
  
  public void setID(int newid)
  {
    identifier = newid;
  }
  
  /** Get a Widget which can be used to select a value for this parameter.  This means
      either a ValueSlider (if the parameter has a finite range), or a ValueField with
      appropriate constraints set (if the range is unbounded on either end). */
  
  public Widget getEditingWidget(double currentValue)
  {
    if (minVal == -Double.MAX_VALUE || maxVal == Double.MAX_VALUE)
    {
      ValueField field = new ValueField(currentValue, ValueField.NONE);
      field.setValueChecker(new ValueChecker() {
        public boolean isValid(double val)
        {
          return (val >= minVal && val <= maxVal);
        }
      } );
      return field;
    }
    return new ValueSlider(minVal, maxVal, 100, currentValue);
  }
}
/* Copyright (C) 2000-2012 by Peter Eastman
   Changes copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import java.awt.*;
import java.util.Arrays;

/** This is the graphical representation of an input or output port on a module. */

public class IOPort
{
  int x, y, valueType, type, location;
  String description[];
  Rectangle bounds;
  Module module;
  
  public static final int INPUT = 0;
  public static final int OUTPUT = 1;
  
  public static final int NUMBER = 0;
  public static final int COLOR = 1;
  
  public static final int TOP = 0;
  public static final int BOTTOM = 1;
  public static final int LEFT = 2;
  public static final int RIGHT = 3;
  public static final int SIZE = 5;
  
  public IOPort(int valueType, int type, int location, String... description)
  {
    this.valueType = valueType;
    this.type = type;
    this.location = location;
    this.description = description;
  }
  
  /** Get the port's screen position. */

  public Point getPosition()
  {
    return new Point(x, y);
  }

  /** Set the port's screen position. */
  
  public void setPosition(int x, int y)
  {
    this.x = x;
    this.y = y;
    if (location == TOP)
      bounds = new Rectangle(x-SIZE, y-1, 2*SIZE, SIZE+2);
    else if (location == BOTTOM)
      bounds = new Rectangle(x-SIZE, y-SIZE-1, 2*SIZE, SIZE+2);
    if (location == LEFT)
      bounds = new Rectangle(x-1, y-SIZE, SIZE+2, 2*SIZE);
    else if (location == RIGHT)
      bounds = new Rectangle(x-SIZE-1, y-SIZE, SIZE+2, 2*SIZE);
  }
  
  /** Get the type of value for this port. */
  
  public int getValueType()
  {
    return valueType;
  }
  
  /** Get the type of port this is (input or output). */
  
  public int getType()
  {
    return type;
  }
  
  /** Get the location of this port (top, bottom, left, or right). */
  
  public int getLocation()
  {
    return location;
  }
  
  /** Get the module this port belongs to. */
  
  public Module getModule()
  {
    return module;
  }
  
  /** Set the module this port belongs to. */
  
  public void setModule(Module mod)
  {
    module = mod;
  }

  /** Get the index of this port in its Module's list of input or output ports. */

  public int getIndex()
  {
    return (type == INPUT) ? module.getInputIndex(this) : module.getOutputIndex(this);
  }

  /** Determine whether a point on the screen is inside this port. */
  
  public boolean contains(Point p)
  {
    return bounds.contains(p);
  }
  
  /** Get the description of this port. */
  
  public String[] getDescription()
  {
    return description;
  }
  
  /** Set the description of this port. */
  
  public void setDescription(String... desc)
  {
    description = desc;
  }
  
  /** Draw the port. */
  
  public void draw(Graphics g)
  {
    if (valueType == NUMBER)
      g.setColor(Color.BLACK);
    else
      g.setColor(Color.BLUE);
    
    switch (location) {
      case TOP:
        g.fillPolygon(new int[] {x+SIZE, x-SIZE, x}, new int[] {y, y, y+SIZE}, 3);
        break;
      case BOTTOM:
        g.fillPolygon(new int[] {x+SIZE, x-SIZE, x}, new int[] {y, y, y-SIZE}, 3);
        break;
      case LEFT:
        g.fillPolygon(new int[] {x, x, x+SIZE}, new int[] {y+SIZE, y-SIZE, y}, 3);
        break;
     case RIGHT:
        g.fillPolygon(new int[] {x-SIZE, x-SIZE, x}, new int[] {y+SIZE, y-SIZE, y}, 3);
        break;
     default:
        break;
      }
  }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IOPort{valueType=").append(valueType == COLOR ? "Color" : "Number");
        sb.append(", type=").append(type == INPUT ? "Input" : "Output");
        sb.append(", description=").append(Arrays.toString(description));
        sb.append('}');
        return sb.toString();
    }
  
  
}


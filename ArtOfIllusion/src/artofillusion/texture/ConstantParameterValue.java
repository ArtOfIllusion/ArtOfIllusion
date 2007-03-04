/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import java.io.*;

/** This class defines a scalar parameter which is constant over the surface of a mesh. */

public class ConstantParameterValue implements ParameterValue
{
  private double value;
  
  /** Create a new ConstantParameterValue object. */
  
  public ConstantParameterValue(double val)
  {
    value = val;
  }
  
  /** Get the constant parameter value. */
  
  public double getValue()
  {
    return value;
  }
  
  /** Set the constant parameter value. */
  
  public void setValue(double val)
  {
    value = val;
  }
  
  /** Get the value of the parameter at a particular point in a particular triangle. */
  
  public double getValue(int tri, int v1, int v2, int v3, double u, double v, double w)
  {
    return value;
  }
  
  /** Get the average value of the parameter over the entire surface. */
  
  public double getAverageValue()
  {
    return value;
  }
  
  /** Create a duplicate of this object. */
  
  public ParameterValue duplicate()
  {
    return new ConstantParameterValue(value);
  }
  
  /** Determine whether this object represents the same value as another one. */
  
  public boolean equals(Object o)
  {
    if (!(o instanceof ConstantParameterValue))
      return false;
    return (((ConstantParameterValue) o).value == value);
  }
  
  /** Write out a serialized representation of this object to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeDouble(value);
  }
  
  /** Reconstruct a serialized object. */
  
  public ConstantParameterValue(DataInputStream in) throws IOException
  {
    value = in.readDouble();
  }
}

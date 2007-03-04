/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import java.io.*;

/** This interface represents a class that defines the value of a scalar parameter over
    the surface of a mesh. */

public interface ParameterValue
{
  /** Get the value of the parameter at a particular point in a particular triangle. */
  
  public double getValue(int tri, int v1, int v2, int v3, double u, double v, double w);
  
  /** Get the average value of the parameter over the entire surface. */
  
  public double getAverageValue();
  
  /** Create a duplicate of this object. */
  
  public ParameterValue duplicate();
  
  /** Write out a serialized representation of this object to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException;
}

/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raster;

import artofillusion.material.*;
import artofillusion.math.*;

/**
 * This class stores information about the material for an object.
 */

public class ObjectMaterialInfo
{
  private MaterialMapping mapping;
  private Mat4 toLocal;

  public ObjectMaterialInfo(MaterialMapping mapping, Mat4 toLocal)
  {
    this.mapping = mapping;
    this.toLocal = toLocal;
  }

  public MaterialMapping getMapping()
  {
    return mapping;
  }

  public Mat4 getToLocal()
  {
    return toLocal;
  }
}

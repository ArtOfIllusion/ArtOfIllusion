/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import artofillusion.object.*;

/** Material3D represents a Material whose properties are defined in 3D. */

public abstract class Material3D extends Material
{  
  /** Get the properties at point (x, y, z) at time t.  More precisely, the properties 
      returned should represent an average over a region of width (xsize, ysize, zsize), 
      which is centered at (x, y, z). */

  public abstract void getMaterialSpec(MaterialSpec spec, double x, double y, double z, double xsize, double ysize, double zsize, double t);

  /** The default mapping is a LinearMaterialMapping. */
  
  public MaterialMapping getDefaultMapping(Object3D obj)
  {
    return new LinearMaterialMapping(obj, this);
  }
}
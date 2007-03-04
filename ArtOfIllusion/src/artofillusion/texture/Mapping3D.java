/* Mapping3D is an abstract class describing a linear mapping between 3D texture coordinates
   and 3D space. */

/* Copyright (C) 2000-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.object.*;

public abstract class Mapping3D extends TextureMapping
{
  Texture3D texture;
  
  public Mapping3D(Texture theTexture)
  {
    texture = (Texture3D) theTexture;
  }

  public Texture getTexture()
  {
    return texture;
  }

  public static boolean legalMapping(Object3D obj, Texture tex)
  {
    return (tex instanceof Texture3D);
  }
}
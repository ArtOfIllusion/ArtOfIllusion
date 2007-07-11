/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.object.*;

/** Mapping3D is an abstract class describing a linear mapping between 3D texture coordinates
    and 3D space. */

public abstract class Mapping3D extends TextureMapping
{
  Object3D object;
  Texture3D texture;
  
  public Mapping3D(Object3D theObject, Texture theTexture)
  {
    object = theObject;
    texture = (Texture3D) theTexture;
  }

  public Texture getTexture()
  {
    return texture;
  }

  public Object3D getObject()
  {
    return object;
  }

  public static boolean legalMapping(Object3D obj, Texture tex)
  {
    return (tex instanceof Texture3D);
  }
}
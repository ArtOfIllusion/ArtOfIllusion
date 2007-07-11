/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.math.*;
import artofillusion.object.*;

/** Mapping2D is an abstract class describing a linear mapping between 2D texture coordinates
    and 3D space. */

public abstract class Mapping2D extends TextureMapping
{
  Object3D object;
  Texture2D texture;
  
  public Mapping2D(Object3D theObject, Texture theTexture)
  {
    object = theObject;
    texture = (Texture2D) theTexture;
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
    return (tex instanceof Texture2D);
  }
  
  /** Given a Mesh to which this mapping has been applied, return the texture coordinates at
      each vertex. */
  
  public abstract Vec2 [] findTextureCoordinates(Mesh mesh);
}
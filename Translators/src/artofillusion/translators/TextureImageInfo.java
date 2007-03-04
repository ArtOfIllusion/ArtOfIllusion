/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.translators;

import artofillusion.texture.*;

/** This class is used by TextureImageExporter for storing information about texture images. */

public class TextureImageInfo
{
  Texture texture;
  String name;
  String diffuseFilename, specularFilename, hilightFilename, transparentFilename, emissiveFilename, bumpFilename;
  double minu, minv, maxu, maxv;
  double paramValue[];
  
  public TextureImageInfo(Texture tex, double param[])
  {
    texture = tex;
    paramValue = param;
    minu = minv = Double.MAX_VALUE;
    maxu = maxv = -Double.MAX_VALUE;
  }
}
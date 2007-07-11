/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import junit.framework.*;
import artofillusion.texture.*;
import artofillusion.object.*;

public class TestLayeredTexture extends TestCase
{
  public void testHasComponent()
  {
    // Create a layered texture.

    Object3D obj = new Sphere(1.0, 1.0, 1.0);
    LayeredTexture tex = new LayeredTexture(obj);
    LayeredMapping map = new LayeredMapping(obj, tex);
    tex.setMapping(map);
    UniformTexture t1 = new UniformTexture();
    UniformTexture t2 = new UniformTexture();
    map.addLayer(t2);
    map.addLayer(t1);
    map.setLayerMode(0, LayeredMapping.BLEND);
    map.setLayerMode(1, LayeredMapping.BLEND);

    // Check a few components.

    assertTrue(tex.hasComponent(Texture.DIFFUSE_COLOR_COMPONENT));
    assertFalse(tex.hasComponent(Texture.SPECULAR_COLOR_COMPONENT));
    t2.specularity = 0.2f;
    assertTrue(tex.hasComponent(Texture.SPECULAR_COLOR_COMPONENT));

    // Check transparency, which has more complex rules than other components.

    assertFalse(tex.hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT));
    t1.transparency = 0.5f;
    assertTrue(tex.hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT));
    map.setLayerMode(0, LayeredMapping.OVERLAY_ADD_BUMPS);
    assertFalse(tex.hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT));
    t2.transparency = 0.9f;
    assertTrue(tex.hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT));
    map.setLayerMode(1, LayeredMapping.OVERLAY_ADD_BUMPS);
    assertTrue(tex.hasComponent(Texture.TRANSPARENT_COLOR_COMPONENT));
  }
}

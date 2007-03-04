/* Copyright (C) 2001,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raster;

import artofillusion.*;
import artofillusion.image.*;
import artofillusion.object.*;
import buoy.widget.*;
import java.util.*;

/** This is a wrapper around the Raster renderer, so that the whole class does not need to
    be loaded at startup. */

public class RasterRenderer implements Renderer
{
  Raster raster;
  
  public RasterRenderer()
  {
    raster = new Raster();
  }

  public String getName()
  {
    return "Raster";
  }

  public void renderScene(Scene theScene, Camera theCamera, RenderListener rl, SceneCamera sceneCamera)
  {
    raster.renderScene(theScene, theCamera, rl, sceneCamera);
  }

  public void cancelRendering(Scene sc)
  {
    raster.cancelRendering(sc);
  }

  public Widget getConfigPanel()
  {
    return raster.getConfigPanel();
  }

  public boolean recordConfiguration()
  {
    return raster.recordConfiguration();
  }

  public void configurePreview()
  {
    raster.configurePreview();
  }
  
  public Map getConfiguration()
  {
    return raster.getConfiguration();
  }

  public void setConfiguration(String property, Object value)
  {
    raster.setConfiguration(property, value);
  }
}
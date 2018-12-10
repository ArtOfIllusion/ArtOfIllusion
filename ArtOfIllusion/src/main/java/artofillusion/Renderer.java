/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.object.*;
import buoy.widget.*;
import java.util.*;

/** The Renderer interface defines the methods for rendering a scene.  Call renderScene() to
    render an image, and cancelRendering() to stop a render that is in progress.
    <p>
    Renderers typically have configuration options that affect the rendering process.  You should
    always configure a Renderer before calling renderScene().  This can be done in any of
    three different ways.
    <p>
    First and most simply, you can call configurePreview().  This configures the renderer in a
    way which is appropriate for quick previews.  It will attempt to find a balance between
    speed and image quality, but err on the side of speed when necessary.
    <p>
    Second, each Renderer can create a user interface that allows the configuration to be
    edited interactively.  Call getConfigPanel() to get a Widget for configuring the renderer.
    When the user is done setting options, call recordConfiguration() to record them.
    <p>
    Finally, you can query and set rendering options directly with getConfiguration() and
    setConfiguration().  Rendering options are defined as key:value pairs.  Each key is
    always a String.  The value may be a String, Integer, Float, Double, or Boolean,
    depending on the specific option.
    */

public interface Renderer
{
  /** Get the name of the renderer.*/

  public String getName();

  /** Begin rendering a scene.  If depthOfField is set to 0, then depth of field effect 
      will not be used.  Some renderers may not support this effect, in which case  
      depthOfField and focalDist will be ignored. */

  public void renderScene(Scene theScene, Camera theCamera, RenderListener listener, SceneCamera sceneCamera);

  /** Cancel a rendering which is in progress. */
  
  public void cancelRendering(Scene theScene);

  /** Get a Widget in which the user can specify options about how the scene should be rendered. */ 

  public Widget getConfigPanel();

  /** Record the values which the user has entered into the configuration panel.  If all
      values are valid, return true.  Otherwise, return false. */

  public boolean recordConfiguration();

  /** Configure the renderer in a way which is appropriate for rendering previews.  This should
      try to find a reasonable balance between speed and image quality. */

  public void configurePreview();
  
  /** Get a Map containing all current configuration options for the renderer. */

  public Map<String, Object> getConfiguration();
  
  /** Set the value of a configuration option for the renderer. */

  public void setConfiguration(String property, Object value);
}
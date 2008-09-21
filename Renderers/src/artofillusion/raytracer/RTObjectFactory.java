/* Copyright (C) 2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.object.*;
import artofillusion.*;

import java.util.*;

/**
 * This interface defines a category of plugins that can generate RTObjects and Lights from
 * objects in the scene.  As the raytracer is processing objects, it invokes each registered
 * RTObjectFactory to process each object.  It stops as soon as one of them indicates that
 * it has successfully processed the object.  If no factory can process the object, the
 * raytracer generates RTObjects and Lights itself in the default way.
 */

public interface RTObjectFactory
{
  /**
   * This method will be invoked for each object in the scene.  If this factory can process it,
   * this method should generate an appropriate set of RTObjects and Lights from it and return
   * true to indicate that the object has been processed.
   *
   * @param obj        the object to process
   * @param scene      the Scene which is being rendered
   * @param camera     the Camera from which the scene is being rendered
   * @param rtobjects  RTObjects representing the object should be added to this Collection
   * @param lights     RTLights representing the object should be added to this collection
   * @return true if the object has been successfully processed, false otherwise.  Returning true
   * will prevent any further RTObjects or Lights from being generated for the object.
   */
  boolean processObject(ObjectInfo obj, Scene scene, Camera camera, Collection<RTObject> rtobjects, Collection<RTLight> lights);
}

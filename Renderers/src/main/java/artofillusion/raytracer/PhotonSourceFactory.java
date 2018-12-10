/* Copyright (C) 2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import java.util.*;

/**
 * This interface defines a category of plugins that can generate PhotonSources from
 * objects in the scene.  As the raytracer is processing RTObjects and Lights, it invokes
 * each registered PhotonSourcetFactory to process each one.  It stops as soon as one
 * of them indicates that it has successfully processed the object.  If no factory can process
 * the object, the raytracer generates PhotonSources itself in the default way.
 */

public interface PhotonSourceFactory
{
  /**
   * This method will be invoked for each RTObject in the scene.  If this factory can process it,
   * this method should generate an appropriate set of PhotonSources from it and return
   * true to indicate that the object has been processed.
   *
   * @param obj        the object to process
   * @param map        the PhotonMap which is being created
   * @param sources    PhotonSources representing the object should be added to this Collection
   * @return true if the object has been successfully processed, false otherwise.  Returning true
   * will prevent any further PhotonSources from being generated for the object.
   */
  boolean processObject(RTObject obj, PhotonMap map, Collection<PhotonSource> sources);

  /**
   * This method will be invoked for each Light in the scene.  If this factory can process it,
   * this method should generate an appropriate set of PhotonSources from it and return
   * true to indicate that the object has been processed.
   *
   * @param light      the Light to process
   * @param map        the PhotonMap which is being created
   * @param sources    PhotonSources representing the light should be added to this Collection
   * @return true if the light has been successfully processed, false otherwise.  Returning true
   * will prevent any further PhotonSources from being generated for the light.
   */
  boolean processLight(RTLight light, PhotonMap map, Collection<PhotonSource> sources);
}

/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.util.*;

/** This interface defines an object which can generate photons. */

public interface PhotonSource
{
  /** Get the total intensity of light which this object sends into the scene. */

  public double getTotalIntensity();
  
  /**
   * Generate photons and add them to a map.
   *
   * @param map          the PhotonMap to add the Photons to
   * @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
   * @param threads      a ThreadManager which may optionally be used to parallelize photon generation
   */
  
  public void generatePhotons(PhotonMap map, double intensity, ThreadManager threads);
}
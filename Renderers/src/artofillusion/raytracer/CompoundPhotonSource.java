/* Copyright (C) 2007-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.util.*;

/**
 * This class is a PhotonSource that simply represents a collection of other PhotonSources.
 * It is used to parallelize the generation of photons from sources that are not themselves
 * parallelized.
 */

public class CompoundPhotonSource implements PhotonSource
{
  private PhotonSource source[];
  private double sourceIntensity[];
  private double totalSourceIntensity;

  public CompoundPhotonSource(PhotonSource source[])
  {
    this.source = source;
    sourceIntensity = new double[source.length];
    totalSourceIntensity = 0.0;
    for (int i = 0; i < source.length; i++)
    {
      sourceIntensity[i] = source[i].getTotalIntensity();
      totalSourceIntensity += sourceIntensity[i];
    }
  }

  public double getTotalIntensity()
  {
    return totalSourceIntensity;
  }

  public void generatePhotons(final PhotonMap map, final double intensity, ThreadManager threads)
  {
    final Thread currentThread = Thread.currentThread();
    threads.setNumIndices(source.length);
    threads.setTask(new ThreadManager.Task()
    {
      public void execute(int index)
      {
        if (map.getRenderer().renderThread != currentThread)
          return;
        source[index].generatePhotons(map, intensity*sourceIntensity[index]/totalSourceIntensity, null);
      }
      public void cleanup()
      {
        map.getWorkspace().cleanup();
      }
    });
    threads.run();
  }
}

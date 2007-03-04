/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

/** This class stores a list of photons which are close to a point in space, and calculates the incident
    light intensity from them.
    
    Parts of this class are based on the descriptions and sample code in
    
    Henrick Wann Jensen, "Realistic Image Synthesis Using Photon Mapping", A K Peters, Natick, MA, 2001. */

public class PhotonList
{
  public Photon photon[];
  public float dist2[], cutoff2;
  public int numFound;
  
  /** Create a new PhotonList.
      @param size      the number of photons to use for calculating the local light intensity
  */
  
  public PhotonList(int size)
  {
    photon = new Photon [size];
    dist2 = new float [size];
  }
  
  /** Initialize the list for beginning a new search.
      @param cutoff2     the maximum squared distance a photon may be from the point of interest
  */
  
  public final void init(float cutoff2)
  {
    this.cutoff2 = cutoff2;
    numFound = 0;
  }
  
  /** Add a photon to the list. */
  
  public final void addPhoton(Photon p, float sqDist)
  {
    if (numFound < photon.length)
      {
        // Add the photon to the list.
        
        photon[numFound] = p;
        dist2[numFound++] = sqDist;
        if (numFound == photon.length)
          buildHeap();
        return;
      }
    
    // Add the photon to the heap.
    
    int pos = 0;
    while (true)
      {
        int child = (pos<<1)+1;
        if (child >= dist2.length)
          break;
        int child2 = child+1;
        if (child2 < dist2.length && dist2[child] < dist2[child2])
          child = child2;
        if (dist2[child] <= sqDist)
          break;
        photon[pos] = photon[child];
        dist2[pos] = dist2[child];
        pos = child;
      }
    photon[pos] = p;
    dist2[pos] = sqDist;
    cutoff2 = dist2[0];
  }
  
  /** Reorder the photons to form a max heap.  This means that for every photon i, dist2[i]
      is greater than or equal to dist2[2*i+1] and dist2[2*i+2]. */
  
  private void buildHeap()
  {
    int half = (dist2.length>>1)-1;
    for (int i = half; i >= 0; i--)
      {
        int parent = i;
        Photon p = photon[i];
        float d = dist2[i];
        while (parent <= half)
          {
            int j = (parent<<1)+1;
            if (j+1 < dist2.length && dist2[j] < dist2[j+1])
              j++;
            if (d >= dist2[j])
              break;
            dist2[parent] = dist2[j];
            photon[parent] = photon[j];
            parent = j;
          }
        dist2[parent] = d;
        photon[parent] = p;
      }
  }
}
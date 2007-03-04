/* Copyright (C) 2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import java.util.*;

/**
 * This is a faster replacement for java.util.Random.  It produces identical results.  Unlike
 * java.util.Random, this class is not thread safe, so a FastRandom object should never be
 * accessed simultaneously from multiple threads.
 */

public class FastRandom extends Random
{
  private long currentSeed;

  // The following constants are declared by java.util.Random, but because they are private,
  // we need to repeat them here.

  private static final long MULTIPLIER = 0x5DEECE66DL;
  private static final long ADDEND = 0xBL;
  private static final long MASK = ((1L<<48)-1);

  /**
   * Create a new random number generator.
   *
   * @param seed     the initial seed value
   */

  public FastRandom(long seed)
  {
    super(seed);
  }

  /**
   * Set the seed value.
   */

  public void setSeed(long seed)
  {
    super.setSeed(seed);
    currentSeed = (seed^MULTIPLIER)&MASK;
  }

  /**
   * This is overridden to update the seed in a much faster (but not threadsafe) way.
   */

  protected int next(int bits)
  {
    currentSeed = (currentSeed*MULTIPLIER+ADDEND)&MASK;
    return (int) (currentSeed >>> (48-bits));
  }
}

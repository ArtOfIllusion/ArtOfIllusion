/* Copyright (C) 2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

/**
 * A ResourcePool maintains a set of reusable objects that can be checked out when needed.
 * This is used to avoid unnecessary object creation in time critical routines.  This class
 * is not thread safe, so a ResourcePool should not be shared between threads.  Also, there
 * is no way to return a single object to the pool.  Once you are done with all objects that
 * have been taken from the pool, call reset() to allow all of them to be reused.
 */

public class ResourcePool
{
  private Class objectClass;
  private Object pool[];
  private int next;

  /**
   * Create a new ResourcePool.
   *
   * @param objectClass     the type of object this pool should contain.  The class must have
   *                        a constructor which takes no arguments.
   */

  public ResourcePool(Class objectClass)
  {
    this.objectClass = objectClass;
    pool = new Object [0];
  }

  private Object createObject()
  {
    try
    {
      return objectClass.newInstance();
    }
    catch (Exception ex)
    {
      // Ignore now.  There's nothing we can do about it, and it's just as easy to handle the
      // NullPointerException in getObject().

      return null;
    }
  }

  /**
   * Get an object from the pool.
   */

  public Object getObject()
  {
    if (next == pool.length)
    {
      Object newPool[] = new Object [pool.length*2+1];
      for (int i = 0; i < pool.length; i++)
        newPool[i] = pool[i];
      for (int i = pool.length; i < newPool.length; i++)
        newPool[i] = createObject();
      pool = newPool;
    }
    return pool[next++];
  }

  /**
   * Reset the pool.  This should be called when you are finished using all objects that have been
   * taken from the pool.
   */

  public void reset()
  {
    next = 0;
  }
}

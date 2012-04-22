/* Copyright (C) 2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.script;

import artofillusion.*;
import artofillusion.object.*;
import java.util.*;

/* This class enumerates the objects defined by a ScriptedObject. */

public class ScriptedObjectEnumeration implements Enumeration<ObjectInfo>
{
  private ObjectInfo next;
  private boolean complete;
  
  ScriptedObjectEnumeration(ObjectInfo obj, boolean interactive, Scene sc)
  {
    new ScriptedObjectController(obj, this, interactive, sc);
  }
  
  /** This is called by the ScriptedObjectController every time a new object is created. */
  
  public synchronized void addObject(ObjectInfo info)
  {
    while (next != null)
      {
        try
          {
            wait();
          }
        catch (InterruptedException ex)
          {
          }
      }
    next = info;
    notify();
  }
  
  /** This is called by the ScriptedObjectController once execution is complete. */
  
  public synchronized void executionComplete()
  {
    complete = true;
    notify();
  }
  
  /** Determine whether there are more objects to enumerate. */
  
  public synchronized boolean hasMoreElements()
  {
    while (next == null && !complete)
      {
        try
          {
            wait();
          }
        catch (InterruptedException ex)
          {
          }
      }
    return (next != null);
  }
  
  /** Get the next ObjectInfo, or null if there are no more. */
  
  public synchronized ObjectInfo nextElement()
  {
    while (next == null && !complete)
      {
        try
          {
            wait();
          }
        catch (InterruptedException ex)
          {
          }
      }
    ObjectInfo nextElem = next;
    next = null;
    notify();
    return nextElem;
  }
}
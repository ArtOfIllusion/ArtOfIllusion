/* Copyright (C) 2002-2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.script;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;

/** This class mediates interactions between an ObjectScript and the rest of
    the program. */

public class ScriptedObjectController
{
  private ObjectInfo info;
  private ScriptedObject object;
  private ScriptedObjectEnumeration enumeration;
  private Scene scene;
  private boolean preview;
  
  /** Create a new ScriptedObjectController and execute its script. */
  
  ScriptedObjectController(ObjectInfo obj, ScriptedObjectEnumeration objectEnum, boolean interactive, Scene sc)
  {
    info = obj;
    Object3D innerObject = obj.getObject();
    while (innerObject instanceof ObjectWrapper)
      innerObject = ((ObjectWrapper) innerObject).getWrappedObject();
    object = (ScriptedObject) innerObject;
    enumeration = objectEnum;
    preview = interactive;
    scene = sc;
    object.setUsesTime(false);
    object.setUsesCoords(false);
    new Thread() {
      public void run()
      {
        try
          {
            ObjectScript script = object.getObjectScript();
            script.execute(ScriptedObjectController.this);
            enumeration.executionComplete();
          }
        catch (Exception ex)
          {
            enumeration.executionComplete();
            ScriptRunner.displayError(object.getLanguage(), ex);
          }
      }
    }.start();
  }
  
  /** Get the coordinate system which defines the scripted object's position in the scene. */
  
  public final CoordinateSystem getCoordinates()
  {
    object.setUsesCoords(true);
    return info.getCoords();
  }
  
  /** Get the current time. */
  
  public final double getTime()
  {
    object.setUsesTime(true);
    return scene.getTime();
  }
  
  /** Get the scene this object is part of. */
  
  public final Scene getScene()
  {
    object.setUsesTime(true);
    return scene;
  }
  
  /** Determine whether the script is currently being executed to create an interactive preview. */
  
  public final boolean isPreview()
  {
    return preview;
  }
  
  /** Get the value of a parameter. */
  
  public final double getParameter(String name) throws IllegalArgumentException
  {
    for (int i = object.getNumParameters()-1; i >= 0; i--)
      if (object.getParameterName(i).equals(name))
        return object.getParameterValue(i);
    throw new IllegalArgumentException("Unknown parameter '"+name+"'");
  }

  /** Add an object to the scripted object. */
  
  public final void addObject(ObjectInfo info)
  {
    info.tracks = new Track [0];
    if (info.getObject().canSetTexture() && info.getObject().getTextureMapping() == null)
      info.setTexture(object.getTexture(), object.getTextureMapping());
    if (info.getObject().canSetMaterial() && info.getObject().getMaterialMapping() == null)
      info.setMaterial(object.getMaterial(), object.getMaterialMapping());
    enumeration.addObject(info);
  }
  
  /** Add an object to the scripted object. */
  
  public final void addObject(Object3D obj, CoordinateSystem coords)
  {
    addObject(new ObjectInfo(obj, coords, ""));
  }
}
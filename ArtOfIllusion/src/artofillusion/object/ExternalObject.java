/* Copyright (C) 2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.Vector;

/** ExternalObject is an Object3D that is stored in a separate file. */

public class ExternalObject extends ObjectWrapper
{
  private File externalFile;
  private String objectName;
  private String loadingError;
  
  /** Create an ExternalObject from a file.
      @param file    the scene file containing the object
      @param name    the name of the object to load
  */
  
  public ExternalObject(File file, String name)
  {
    externalFile = file;
    objectName = name;
    theObject = new NullObject();
  }
  
  /** This constructor is used internally. */
  
  private ExternalObject()
  {
    theObject = new NullObject();
  }
  
  /** Get the name of the object in the external scene. */
  
  public String getExternalObjectName()
  {
    return objectName;
  }
  
  /** Set the name of the object in the external scene. */
  
  public void setExternalObjectName(String name)
  {
    objectName = name;
  }
  
  /** Get the path to the external scene file. */
  
  public File getExternalSceneFile()
  {
    return externalFile;
  }
  
  /** Set the path to the external scene file. */
  
  public void setExternalSceneFile(File file)
  {
    externalFile = file;
  }
  
  /** Get an error message which describes why the object could not be loaded, or null
      if it was loaded successfully. */
  
  public String getLoadingError()
  {
    return loadingError;
  }
  
  /** Reload the external object from its file. */
  
  public void reloadObject()
  {
    theObject = new NullObject();
    loadingError = null;
    try
    {
      if (!externalFile.isFile())
      {
        loadingError = Translate.text("externalObject.sceneNotFound", externalFile.getAbsolutePath());
        return;
      }
      Scene scene = new Scene(externalFile, true);
      boolean found = false;
      for (int i = 0; i < scene.getNumObjects(); i++)
      {
        ObjectInfo info = scene.getObject(i);
        if (!info.name.equals(objectName))
          continue;
        if (found)
        {
          loadingError = Translate.text("externalObject.multipleObjectsFound", externalFile.getAbsolutePath(), objectName);
          return;
        }
        theObject = info.object;
        found = true;
      }
      if (!found)
        loadingError = Translate.text("externalObject.objectNotFound", externalFile.getAbsolutePath(), objectName);
    }
    catch (Exception ex)
    {
      // If anything goes wrong, use a null object and return an error message.
      
      ex.printStackTrace();
      loadingError = ex.getMessage();
    }
  }
  
  /** Create a new object which is an exact duplicate of this one. */
  
  public Object3D duplicate()
  {
    ExternalObject obj = new ExternalObject();
    obj.externalFile = externalFile;
    obj.objectName = objectName;
    obj.theObject = theObject;
    return obj;
  }
  
  /** Copy all the properties of another object, to make this one identical to it.  If the
      two objects are of different classes, this will throw a ClassCastException. */
  
  public void copyObject(Object3D obj)
  {
    ExternalObject eo = (ExternalObject) obj;
    externalFile = eo.externalFile;
    objectName = eo.objectName;
    theObject = eo.theObject;
  }

  /** ExternalObjects cannot be resized, since they are entirely defined by a separate file. */

  public void setSize(double xsize, double ysize, double zsize)
  {
  }
  
  /** If the object can be edited by the user, isEditable() should be overridden to return true.
      edit() should then create a window and allow the user to edit the object. */
  
  public boolean isEditable()
  {
    return true;
  }
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    new ExternalObjectEditingWindow(parent, this, info);
  }

  /** This method tells whether textures can be assigned to the object.  Objects for which
      it makes no sense to assign a texture (curves, lights, etc.) should override this
      method to return false. */
  
  public boolean canSetTexture()
  {
    return false;
  }
  
  /** This method tells whether materials can be assigned to the object.  The default
      implementation will give the correct result for most objects, but subclasses
      can override this if necessary. */
  
  public boolean canSetMaterial()
  {
    return false;
  }
  
  /** Determine whether the user should be allowed to convert this object to an Actor. */
  
  public boolean canConvertToActor()
  {
    return false;
  }

  /** The following method writes the object's data to an output stream. */
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);
    out.writeShort(0);
    out.writeUTF(externalFile.getAbsolutePath());
    out.writeUTF(findRelativePath(theScene));
    out.writeUTF(objectName);
  }
  
  /** Find the relative path from the scene file containing this object to the external scene. */
  
  private String findRelativePath(Scene theScene)
  {
    String scenePath = null, externalPath = null;
    try
    {
      scenePath = new File(theScene.getDirectory()).getCanonicalPath();
      externalPath = externalFile.getCanonicalPath();
    }
    catch (IOException ex)
    {
      // We couldn't get the canonical name for one of the files.
      
      return "";
    }
    
    // Break each path into pieces, and find how much they share in common.
    
    String splitExpr = File.separator;
    if ("\\".equals(splitExpr))
      splitExpr = "\\\\";
    String scenePathParts[] = scenePath.split(splitExpr);
    String externalPathParts[] = externalPath.split(splitExpr);
    int numCommon;
    for (numCommon = 0; numCommon < scenePathParts.length && numCommon < externalPathParts.length && scenePathParts[numCommon].equals(externalPathParts[numCommon]); numCommon++);
    StringBuffer relPath = new StringBuffer();
    for (int i = numCommon; i < scenePathParts.length; i++)
      relPath.append(".."+File.separator);
    for (int i = numCommon; i < externalPathParts.length; i++)
    {
      if (i > numCommon)
        relPath.append(File.separator);
      relPath.append(externalPathParts[i]);
    }
    return relPath.toString();
  }
  
  /** Recreate an ExternalObject by reading in the serialized representation written by writeToFile(). */
  
  public ExternalObject(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("Unknown version: "+version);
    externalFile = new File(in.readUTF());
    String relPath = in.readUTF();
    if (!externalFile.isFile())
    {
      File f = new File(theScene.getDirectory(), relPath);
      if (f.isFile())
        externalFile = f;
    }
    objectName = in.readUTF();
    reloadObject();
  }
}

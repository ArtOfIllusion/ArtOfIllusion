/* Copyright (C) 2002-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.script;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import java.io.*;
import java.util.*;
import java.awt.*;

/** This class represents an Object3D whose properties are defined by a script. */

public class ScriptedObject extends ObjectCollection
{
  private String script;
  private String language;
  private ObjectScript parsedScript;
  private String paramName[];
  private double paramValue[];
  
  public ScriptedObject(String scriptText, String language)
  {
    script = scriptText;
    this.language = language;
    paramName = new String [0];
    paramValue = new double [0];
  }

  public ScriptedObject(String scriptText)
  {
    this(scriptText, ScriptRunner.LANGUAGES[0]);
  }

  /** Get the script which defines this object. */
  
  public String getScript()
  {
    return script;
  }

  /** Set the script which defines this object. */
  
  public void setScript(String scriptText)
  {
    script = scriptText;
    parsedScript = null;
    cachedObjects = null;
    cachedBounds = null;
  }

  /** Get the language the script is written in. */

  public String getLanguage()
  {
    return language;
  }

  /** Set the language the script is written in. */

  public void setLanguage(String language)
  {
    this.language = language;
    parsedScript = null;
    cachedObjects = null;
    cachedBounds = null;
  }

  /** Get the parsed form of the script. */
  
  public ObjectScript getObjectScript()
  {
    if (parsedScript == null)
      {
        try
          {
            parsedScript = ScriptRunner.parseObjectScript(language, script);
          }
        catch (final Exception ex)
          {
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                ScriptRunner.displayError(language, ex);
              }
            });
            parsedScript = new ObjectScript() {
              public void execute(ScriptedObjectController script)
              {
              }
            };
          }
      }
    return parsedScript;
  }
  
  /** Get the number of parameters for this object. */
  
  public final int getNumParameters()
  {
    return paramName.length;
  }
  
  /** Get the name of the i'th parameter. */
  
  public final String getParameterName(int i)
  {
    return paramName[i];
  }
    
  /** Get the value of the i'th parameter. */
  
  public final double getParameterValue(int i)
  {
    return paramValue[i];
  }
  
  /** Set the name of the i'th parameter. */
  
  public void setParameterName(int i, String name)
  {
    paramName[i] = name;
  }
  
  /** Set the value of the i'th parameter. */
  
  public void setParameterValue(int i, double value)
  {
    paramValue[i] = value;
  }
  
  /** Set a new list of parameters. */
  
  public void setParameters(String names[], double values[])
  {
    paramName = names;
    paramValue = values;
  }
  
  /** Get an enumeration of ObjectInfos listing the objects which this object
      is composed of. */
  
  protected Enumeration<ObjectInfo> enumerateObjects(ObjectInfo info, boolean interactive, Scene scene)
  {
    return new ScriptedObjectEnumeration(info, interactive, scene);
  }

  /** Create a new object which is an exact duplicate of this one. */
  
  public Object3D duplicate()
  {
    ScriptedObject so = new ScriptedObject(script, language);
    so.paramName = new String [paramName.length];
    System.arraycopy(paramName, 0, so.paramName, 0, paramName.length);
    so.paramValue = new double [paramValue.length];
    System.arraycopy(paramValue, 0, so.paramValue, 0, paramValue.length);
    so.copyTextureAndMaterial(this);
    return so;
  }
  
  /** Copy all the properties of another object, to make this one identical to it.  If the
     two objects are of different classes, this will throw a ClassCastException. */
  
  public void copyObject(Object3D obj)
  {
    ScriptedObject so = (ScriptedObject) obj;
    script = so.script;
    language = so.language;
    paramName = new String [so.paramName.length];
    System.arraycopy(so.paramName, 0, paramName, 0, paramName.length);
    paramValue = new double [so.paramValue.length];
    System.arraycopy(so.paramValue, 0, paramValue, 0, paramValue.length);
    parsedScript = null;
    copyTextureAndMaterial(obj);
  }

  /** setSize() has no effect, since the geometry of the object is set by the script. */

  public void setSize(double xsize, double ysize, double zsize)
  {
  }

  /** Get a list of editable properties defined by this object. */

  public Property[] getProperties()
  {
    Property prop[] = new Property[paramName.length];
    for (int i = 0; i < prop.length; i++)
      prop[i] = new Property(paramName[i], -Double.MAX_VALUE, Double.MAX_VALUE, 0.0);
    return prop;
  }

  /** Get the value of one of this object's editable properties.
      @param index     the index of the property to get
   */

  public Object getPropertyValue(int index)
  {
    return paramValue[index];
  }

  /** Set the value of one of this object's editable properties.
      @param index     the index of the property to set
      @param value     the value to set for the property
   */

  public void setPropertyValue(int index, Object value)
  {
    paramValue[index] = (Double) value;
    cachedObjects = null;
    cachedBounds = null;
  }

  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new ScriptedObjectKeyframe(this, paramName, paramValue);
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    if (paramName.length == 0)
      return;
    ScriptedObjectKeyframe key = (ScriptedObjectKeyframe) k;
    for (int i = 0; i < paramName.length; i++)
      {
        Double d = key.valueTable.get(paramName[i]);
        if (d == null)
          paramValue[i] = 0.0;
        else
          paramValue[i] = d;
      }
    cachedObjects = null;
    cachedBounds = null;
  }

  /** Return an array containing the names of the graphable values for the keyframes
      returned by getPoseKeyframe(). */
  
  public String [] getPoseValueNames()
  {
    return paramName;
  }

  /** Get the default list of graphable values for a keyframe returned by getPoseKeyframe(). */
  
  public double [] getDefaultPoseValues()
  {
    return paramValue;
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    String name[] = new String [paramValue.length];
    double value[] = new double [paramValue.length];
    double range[][] = new double [paramValue.length][];
    for (int i = 0; i < paramValue.length; i++)
    {
      name[i] = paramName[i];
      value[i] = paramValue[i];
      range[i] = new double [] {-Double.MAX_VALUE, Double.MAX_VALUE};
    }
    track.setGraphableValues(name, value, range);
  }
  
  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    ScriptedObjectKeyframe key = (ScriptedObjectKeyframe) k;
    ValueField valField[] = new ValueField [paramName.length];
    
    for (int i = 0; i < paramName.length; i++)
      {
        Double d = (Double) key.valueTable.get(paramName[i]);
        if (d == null)
          valField[i] = new ValueField(0.0, ValueField.NONE, 5);
        else
          valField[i] = new ValueField(d, ValueField.NONE, 5);
      }
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editScriptedObjTitle"),
        valField, paramName);
    if (!dlg.clickedOk())
      return;
    for (int i = 0; i < paramName.length; i++)
      key.valueTable.put(paramName[i], valField[i].getValue());
  }

  public boolean isEditable()
  {
    return true;
  }
  
  /** Allow the user to edit the script. */
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    new ScriptedObjectEditorWindow(parent, info, cb);
  }

  /** This constructor reconstructs a ScriptedObject from an input stream. */

  public ScriptedObject(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    script = in.readUTF();
    if (version > 0)
      language = in.readUTF();
    short numParams = in.readShort();
    paramName = new String [numParams];
    paramValue = new double [numParams];
    for (int i = 0; i < numParams; i++)
      {
        paramName[i] = in.readUTF();
        paramValue[i] = in.readDouble();
      }
  }

  /** Write a serialized representation of this object to an output stream. */

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(1);
    out.writeUTF(script);
    out.writeUTF(language);
    out.writeShort(paramName.length);
    for (int i = 0; i < paramName.length; i++)
      {
        out.writeUTF(paramName[i]);
        out.writeDouble(paramValue[i]);
      }
  }
  
  /** Inner class representing a pose for a scripted object. */
  
  public static class ScriptedObjectKeyframe implements Keyframe
  {
    ScriptedObject script;
    public Hashtable<String, Double> valueTable;
    
    public ScriptedObjectKeyframe(ScriptedObject object, String names[], double values[])
    {
      script = object;
      valueTable = new Hashtable<String, Double>();
      for (int i = 0; i < names.length; i++)
        valueTable.put(names[i], values[i]);
    }
    
    /* Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return duplicate(script);
    }

    /* Create a duplicate of this keyframe for a (possibly different) object. */
  
    public Keyframe duplicate(Object owner)
    {
      ScriptedObjectKeyframe key = new ScriptedObjectKeyframe((ScriptedObject) ((ObjectInfo) owner).getObject(), new String [0], new double [0]);
      key.valueTable = (Hashtable<String, Double>) valueTable.clone();
      return key;
    }
  
    /* Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      String names[] = script.paramName;
      double values[] = new double [names.length];
      for (int i = 0; i < names.length; i++)
        {
          Double d = (Double) valueTable.get(names[i]);
          if (d == null)
            values[i] = script.paramValue[i];
          else
            values[i] = d;
        }
      return values;
    }
  
    /* Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
      String names[] = script.paramName;
      for (int i = 0; i < names.length; i++)
        valueTable.put(names[i], values[i]);
    }

    /* These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      return blend(new Keyframe [] {this, o2},
        new double [] {weight1, weight2});
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      return blend(new Keyframe [] {this, o2, o3},
        new double [] {weight1, weight2, weight3});
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      return blend(new Keyframe [] {this, o2, o3, o4},
        new double [] {weight1, weight2, weight3, weight4});
    }
    
    /* Blend an arbitrary list of keyframes. */
    
    private Keyframe blend(Keyframe k[], double weight[])
    {
      String names[] = script.paramName;
      double values[] = new double [names.length];
      for (int i = 0; i < names.length; i++)
        {
          double d = 0.0;
          for (int j = 0; j < k.length; j++)
            {
              ScriptedObjectKeyframe key = (ScriptedObjectKeyframe) k[j];
              Double v = (Double) key.valueTable.get(names[i]);
              if (v != null)
                d += weight[j]*v;
            }
          values[i] = d;
        }
      return new ScriptedObjectKeyframe(script, names, values);
    }

    /* Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof ScriptedObjectKeyframe))
        return false;
      ScriptedObjectKeyframe key = (ScriptedObjectKeyframe) k;
      for (String name : script.paramName)
        {
          Double d1 = (Double) valueTable.get(name);
          Double d2 = (Double) key.valueTable.get(name);
          if (d1 == null && d2 == null)
            continue;
          if (d1 == null || d2 == null)
            return false;
          double diff = d1-d2;
          if (diff > 1e-12 || diff < -1e12)
            return false;
        }
      return true;
    }
  
    /* Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeShort(0);
      for (double value : getGraphValues())
        out.writeDouble(value);
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public ScriptedObjectKeyframe(DataInputStream in, Object parent) throws IOException
    {
      short version = in.readShort();
      if (version != 0)
        throw new InvalidObjectException("");
      script = (ScriptedObject) ((ObjectInfo) parent).getObject();
      double values[] = new double [script.paramName.length];
      for (int i = 0; i < values.length; i++)
        values[i] = in.readDouble();
      valueTable = new Hashtable<String, Double>();
      for (int i = 0; i < values.length; i++)
        valueTable.put(script.paramName[i], values[i]);
    }
  }
}

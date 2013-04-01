/* Copyright (C) 2002-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.script;

import artofillusion.*;
import artofillusion.util.*;
import buoy.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** This class is used for executing scripts. */

public class ScriptRunner
{
  public static final String LANGUAGES[] = {"Groovy", "BeanShell"};
  private static SearchlistClassLoader parentLoader;
  private static PrintStream output;
  private static final HashMap<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
  private static final String IMPORTS[] = {"artofillusion.*", "artofillusion.image.*", "artofillusion.material.*",
      "artofillusion.math.*", "artofillusion.object.*", "artofillusion.script.*", "artofillusion.texture.*",
      "artofillusion.ui.*", "buoy.event.*", "buoy.widget.*"};

  /** Get the ScriptEngine for running scripts written in a particular language. */
  
  public static ScriptEngine getScriptEngine(String language)
  {
    if (!engines.containsKey(language))
      {
        if (parentLoader == null)
        {
          parentLoader = new SearchlistClassLoader(ScriptRunner.class.getClassLoader());
          for (ClassLoader plugin : PluginRegistry.getPluginClassLoaders())
            parentLoader.add(plugin);
        }
        ScriptEngine engine;
        if (language.equals("Groovy"))
          engine = new GroovyScriptEngine(parentLoader);
        else if (language.equals("BeanShell"))
          engine = new BeanshellScriptEngine(parentLoader);
        else
          throw new IllegalArgumentException("Unknown name for scripting language: "+language);
        engines.put(language, engine);
        try
          {
            for (String packageName : IMPORTS)
              engine.addImport(packageName);
          }
        catch (Exception e)
          {
            e.printStackTrace();
          }
        output = new PrintStream(new ScriptOutputWindow());
        engine.setOutput(output);
      }
    return engines.get(language);
  }
  
  /** Execute a script. */
  
  public static void executeScript(String language, String script, Map<String, Object> variables)
  {
    try
      {
        getScriptEngine(language).executeScript(script, variables);
      }
    catch (ScriptException e)
      {
        System.out.println("Error in line "+e.getLineNumber()+": "+e.getMessage());
      }
  }
  
  /** Parse a Tool script. */
  
  public static ToolScript parseToolScript(String language, String script) throws Exception
  {
    return getScriptEngine(language).createToolScript(script);
  }
  
  /** Parse an Object script. */
  
  public static ObjectScript parseObjectScript(String language, String script) throws Exception
  {
    return getScriptEngine(language).createObjectScript(script);
  }

  /** Display a dialog showing an exception thrown by a script.  This returns the line number
      in which the error occurred, or -1 if it could not be determined. */

  public static int displayError(String language, Exception ex)
  {
    if (ex instanceof UndeclaredThrowableException)
      ex = (Exception) ((UndeclaredThrowableException) ex).getUndeclaredThrowable();
    String head = "An error occurred while executing the script:";
    String message = null, errorText = null, column = null;
    int line = -1;
    try
    {
      if (ex instanceof ScriptException)
      {
        ScriptException t = (ScriptException) ex;
        message = t.getMessage();
        if (t.getLineNumber() > -1)
          line = t.getLineNumber();
        ex.printStackTrace(output);
      }
      else
        {
          message = ex.getMessage();
          ex.printStackTrace(output);
        }
      if (message == null || message.length() == 0)
        message = ex.toString();
    }
    catch (Exception ex2)
    {
      ex2.printStackTrace();
    }
    ArrayList<String> v = new ArrayList<String>();
    v.add(head);
    if (message != null)
    {
      if (!message.contains("Inline eval of"))
        v.add(message);
      else
        {
          int i = message.lastIndexOf("> Encountered");
          if (i > -1)
            {
              int j = message.lastIndexOf(", column");
              if (j > i)
                column = (message.substring(j));
            }
        }
    }
    if (line > -1 && errorText != null)
      {
        if (column == null)
          v.add("Encountered \""+errorText+"\" at line "+line+".");
        else
          v.add("Encountered \""+errorText+"\" at line "+line+column);
      }
    String msg[] = v.toArray(new String [v.size()]);
    new BStandardDialog("Error", msg, BStandardDialog.ERROR).showMessageDialog(null);
    return line;
  }

  /** Given the name of a file, determine what language it contains based on the extension. */

  public static String getLanguageForFilename(String filename)
  {
    for (String language : LANGUAGES)
      if (filename.endsWith("."+getScriptEngine(language).getFilenameExtension()))
        return language;
    throw new IllegalArgumentException("Filename \""+filename+"\" does not match any recognized scripting language.");
  }

  /** Return the standard filename extension to use for a language. */

  public static String getFilenameExtension(String language)
  {
    return getScriptEngine(language).getFilenameExtension();
  }
}

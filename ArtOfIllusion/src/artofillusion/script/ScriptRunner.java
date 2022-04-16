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
  private static SearchlistClassLoader parentLoader;
  private static PrintStream output;
  // This is a cache of script engine instances
  private static final HashMap<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
  private static final String IMPORTS[] = {"artofillusion.*", "artofillusion.image.*", "artofillusion.material.*",
      "artofillusion.math.*", "artofillusion.object.*", "artofillusion.script.*", "artofillusion.texture.*",
      "artofillusion.ui.*", "buoy.event.*", "buoy.widget.*"};
  public static final String UNKNOWN_LANGUAGE = "?";
  
  public enum Language {
      BEANSHELL ("BeanShell", "bsh", BeanshellScriptEngine.class), 
      GROOVY ("Groovy", "groovy", GroovyScriptEngine.class);
      public final String name; 
      public final String fileNameExtension;
      public final Class <? extends ScriptEngine> engineClass;
      Language (String name, String extension, Class <? extends ScriptEngine> engineClass) {
          this.name = name;
          this.fileNameExtension = extension;
          this.engineClass = engineClass;
      }
  }

  private static final String [] languageNames;
  
  static {
      List <String> names = new ArrayList <String> ();
      for (Language l : Language.values()) {
          names.add(l.name);
      }
      languageNames = (String[]) names.toArray(new String [0]);
  }
  
    public static String[] getLanguageNames() {
        return languageNames;
    }
  
  
  /** Get the ScriptEngine for running scripts written in a particular language. */
  
  public static ScriptEngine getScriptEngine(String language) throws ScriptException
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
        Class <? extends ScriptEngine> languageEngine = null;
        for (Language implementedLanguage : Language.values()) {
            if (implementedLanguage.name.equals (language)) languageEngine = implementedLanguage.engineClass;
        }
        if (languageEngine == null)
            throw new IllegalArgumentException("Unknown name for scripting language: "+language);
        try
        {
            engine = languageEngine.getConstructor (ClassLoader.class).newInstance(parentLoader);
        } catch (Exception ex)
        {
            throw new ScriptException ("Could not create a script engine of class " + languageEngine, -1, ex);
        } 
        engines.put(language, engine);
        try
          {
            for (String packageName : IMPORTS)
              engine.addImport(packageName);
          }
        catch (Exception e)
          {
            throw new ScriptException ("Could not import the required packages (" + IMPORTS.toString() + ")", -1, e);
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

  /** Given the name of a file, determine what language it contains based on the fileNameExtension. 
   @return {@link #UNKNOWN_LANGUAGE} if the language is not recognized
   */
  public static String getLanguageForFilename(String filename)
  {
      for (Language knownLanguage : Language.values())
        if (filename.endsWith("." + knownLanguage.fileNameExtension))
            return knownLanguage.name;
      return UNKNOWN_LANGUAGE;
  }

  /** Return the standard filename fileNameExtension to use for a language. 
   @return {@link #UNKNOWN_LANGUAGE} if the language is not recognized
   */

  public static String getFilenameExtension(String language)
  {
      for (Language knownLanguage : Language.values())
        if (knownLanguage.name.equals(language))
            return knownLanguage.fileNameExtension;
      return UNKNOWN_LANGUAGE;
  }
}

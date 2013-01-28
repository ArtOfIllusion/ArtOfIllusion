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
import bsh.*;
import buoy.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.script.*;

/** This class is used for executing scripts. */

public class ScriptRunner
{
  public static final String LANGUAGES[] = {"Groovy", "BeanShell"};
  private static ScriptEngineManager manager;
  private static final HashMap<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
  private static final String IMPORTS = "import artofillusion.*;"+
                                        "import artofillusion.image.*;"+
                                        "import artofillusion.material.*;"+
                                        "import artofillusion.math.*;"+
                                        "import artofillusion.object.*;"+
                                        "import artofillusion.script.*;"+
                                        "import artofillusion.texture.*;"+
                                        "import artofillusion.ui.*;"+
                                        "import buoy.event.*;"+
                                        "import buoy.widget.*;";

  /** Get the ScriptEngine for running scripts written in a particular language. */
  
  public static ScriptEngine getScriptEngine(String language)
  {
    if (!engines.containsKey(language))
      {
        if (manager == null)
        {
          SearchlistClassLoader loader = new SearchlistClassLoader(ScriptRunner.class.getClassLoader());
          for (ClassLoader plugin : PluginRegistry.getPluginClassLoaders())
            loader.add(plugin);
          manager = new ScriptEngineManager(loader);
        }
        ScriptEngine engine = null;
        for (ScriptEngineFactory factory : manager.getEngineFactories())
          if (factory.getLanguageName().equals(language))
            engine = factory.getScriptEngine();
        if (engine == null)
          throw new IllegalArgumentException("Unknown name for scripting language: "+language);
        engines.put(language, engine);
        try
          {
            engine.eval(IMPORTS);
          }
        catch (ScriptException e)
          {
            e.printStackTrace();
          }
        Writer out = new OutputStreamWriter(new ScriptOutputWindow());
        engine.getContext().setWriter(out);
        engine.getContext().setErrorWriter(out);
      }
    return engines.get(language);
  }
  
  /** Execute a script. */
  
  public static Object executeScript(String language, String script)
  {
    try
      {
        return getScriptEngine(language).eval(script);
      }
    catch (ScriptException e)
      {
        System.out.println("Error in line "+e.getLineNumber()+": "+e.getMessage());
      }
    return null;
  }
  
  /** Parse a Tool script. */
  
  public static ToolScript parseToolScript(String language, String script) throws Exception
  {
    if (language.equals("BeanShell"))
    {
      String prefix = "return new ToolScript() {void execute(LayoutWindow window) {\n";
      String suffix = "\n;}}";
      return (ToolScript) getScriptEngine(language).eval(prefix+script+suffix);
    }
    Compilable engine = (Compilable) getScriptEngine(language);
    return new CompiledToolScript(engine.compile(IMPORTS+script));
  }
  
  /** Parse an Object script. */
  
  public static ObjectScript parseObjectScript(String language, String script) throws Exception
  {
    if (language.equals("BeanShell"))
    {
      String prefix = "return new ObjectScript() {void execute(ScriptedObjectController script) {\n";
      String suffix = "\n;}}";
      return (ObjectScript) getScriptEngine(language).eval(prefix+script+suffix);
    }
    Compilable engine = (Compilable) getScriptEngine(language);
    return new CompiledObjectScript(engine.compile(IMPORTS+script));
  }

  /** Display a dialog showing an exception thrown by a script.  This returns the line number
      in which the error occurred, or -1 if it could not be determined. */

  public static int displayError(String language, Exception ex, int lineOffset)
  {
    if (ex instanceof UndeclaredThrowableException)
      ex = (Exception) ((UndeclaredThrowableException) ex).getUndeclaredThrowable();
    String head = "An error occurred while executing the script:";
    String message = null, errorText = null, column = null;
    int line = -1;
    try
    {
      if (ex instanceof TargetError)
        {
          TargetError t = (TargetError) ex;
          message = t.getMessage();
          errorText = t.getErrorText();
          if (t.getErrorLineNumber() > -1)
            line = t.getErrorLineNumber()-lineOffset;
          t.getTarget().printStackTrace(new PrintWriter(getScriptEngine(language).getContext().getWriter()));
        }
      else if (ex instanceof EvalError)
        {
          EvalError e = (EvalError) ex;
          head = "An error occurred while parsing the script:";
          message = e.getMessage();
          errorText = e.getErrorText();
          if (e.getErrorLineNumber() > -1)
            line = e.getErrorLineNumber()-lineOffset;
          e.printStackTrace(new PrintWriter(getScriptEngine(language).getContext().getWriter()));
        }
      else if (ex instanceof ScriptException)
      {
        ScriptException t = (ScriptException) ex;
        message = t.getMessage();
        if (t.getLineNumber() > -1)
          line = t.getLineNumber()-lineOffset;
        ex.printStackTrace(new PrintWriter(getScriptEngine(language).getContext().getWriter()));
      }
      else
        {
          message = ex.getMessage();
          ex.printStackTrace(new PrintWriter(getScriptEngine(language).getContext().getWriter()));
        }
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
    {
      for (String extension : getScriptEngine(language).getFactory().getExtensions())
      {
        if (filename.endsWith("."+extension))
          return language;
      }
    }
    throw new IllegalArgumentException("Filename \""+filename+"\" does not match any recognized scripting language.");
  }

  /** Return the standard filename extension to use for a language. */

  public static String getFilenameExtension(String language)
  {
    return getScriptEngine(language).getFactory().getExtensions().get(0);
  }

  /** Inner class used to represent a compiled ToolScript. */

  private static class CompiledToolScript implements ToolScript
  {
    private CompiledScript script;

    public CompiledToolScript(CompiledScript script)
    {
      this.script = script;
    }

    public void execute(LayoutWindow window) throws ScriptException
    {
      Bindings bindings = new SimpleBindings();
      bindings.put("window", window);
      script.getEngine().setBindings(bindings, ScriptContext.ENGINE_SCOPE);
      script.eval();
    }
  }

  /** Inner class used to represent a compiled ObjectScript. */

  private static class CompiledObjectScript implements ObjectScript
  {
    private CompiledScript script;

    public CompiledObjectScript(CompiledScript script)
    {
      this.script = script;
    }

    public void execute(ScriptedObjectController controller) throws ScriptException
    {
      Bindings bindings = new SimpleBindings();
      bindings.put("script", controller);
      script.getEngine().setBindings(bindings, ScriptContext.ENGINE_SCOPE);
      script.eval();
    }
  }
}

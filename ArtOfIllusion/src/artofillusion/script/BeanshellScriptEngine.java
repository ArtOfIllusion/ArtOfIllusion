package artofillusion.script;

/* Copyright (C) 2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


import java.io.*;
import java.util.*;

import bsh.*;

/**
 * This ScriptEngine implements the BeanShell scripting language.
 */
public class BeanshellScriptEngine implements ScriptEngine
{
  Interpreter interp;

  public BeanshellScriptEngine(ClassLoader parent)
  {
    interp = new Interpreter();
    interp.setClassLoader(parent);
  }

  public String getName()
  {
    return "BeanShell";
  }

  public String getFilenameExtension()
  {
    return "bsh";
  }

  public void setOutput(PrintStream out)
  {
    interp.setOut(out);
    interp.setErr(out);
  }

  public void addImport(String packageOrClass) throws Exception
  {
    interp.eval("import "+packageOrClass);
  }

  public void executeScript(String script, Map<String, Object> variables) throws ScriptException
  {
    try
    {
      for (Map.Entry<String, Object> entry : variables.entrySet())
        interp.set(entry.getKey(), entry.getValue());
      interp.eval(script);
    }
    catch (EvalError e)
    {
      throw new ScriptException(e.getErrorText(), e.getErrorLineNumber(), e);
    }
  }

  public ToolScript createToolScript(String script) throws ScriptException
  {
    String prefix = "return new ToolScript() {void execute(LayoutWindow window) {\n";
    String suffix = "\n;}}";
    try
    {
      return (ToolScript) interp.eval(prefix+script+suffix);
    }
    catch (EvalError e)
    {
      throw new ScriptException(e.getMessage(), e.getErrorLineNumber()-1, e);
    }
  }

  public ObjectScript createObjectScript(String script) throws ScriptException
  {
    String prefix = "return new ObjectScript() {void execute(ScriptedObjectController script) {\n";
    String suffix = "\n;}}";
    try
    {
      return (ObjectScript) interp.eval(prefix+script+suffix);
    }
    catch (EvalError e)
    {
      throw new ScriptException(e.getMessage(), e.getErrorLineNumber()-1, e);
    }
  }
}

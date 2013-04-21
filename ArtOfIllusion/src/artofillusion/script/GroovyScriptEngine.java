package artofillusion.script;

/* Copyright (C) 2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


import artofillusion.*;
import groovy.lang.*;
import org.codehaus.groovy.control.*;

import java.io.*;
import java.util.*;

/**
 * This ScriptEngine implements the Groovy scripting language.
 */
public class GroovyScriptEngine implements ScriptEngine
{
  private CompilerConfiguration config;
  private GroovyShell shell;
  private StringBuilder imports;
  private int numImports = 0;

  public GroovyScriptEngine(ClassLoader parent)
  {
    config = new CompilerConfiguration();
    shell = new GroovyShell(parent, new Binding(), config);
    imports = new StringBuilder();
  }

  public String getName()
  {
    return "Groovy";
  }

  public String getFilenameExtension()
  {
    return "groovy";
  }

  public void setOutput(PrintStream out)
  {
    config.setOutput(new PrintWriter(out, true));
  }

  public void addImport(String packageOrClass) throws Exception
  {
    imports.append("import ").append(packageOrClass).append(";\n");
    numImports++;
  }

  public void executeScript(String script, Map<String, Object> variables) throws ScriptException
  {
    try
    {
      for (Map.Entry<String, Object> entry : variables.entrySet())
        shell.setVariable(entry.getKey(), entry.getValue());
      shell.evaluate(imports.toString()+script);
    }
    catch (CompilationFailedException e)
    {
      throw new ScriptException(e.getMessage(), -1);
    }
  }

  public ToolScript createToolScript(String script) throws ScriptException
  {
    try
    {
      return new CompiledToolScript(shell.parse(imports.toString()+script));
    }
    catch (CompilationFailedException e)
    {
      throw new ScriptException(e.getMessage(), -1);
    }
  }

  public ObjectScript createObjectScript(String script) throws ScriptException
  {
    try
    {
      return new CompiledObjectScript(shell.parse(imports.toString()+script));
    }
    catch (CompilationFailedException e)
    {
      throw new ScriptException(e.getMessage(), -1, e);
    }
  }

  /** Inner class used to represent a compiled ToolScript. */

  private class CompiledToolScript implements ToolScript
  {
    private Script script;

    public CompiledToolScript(Script script)
    {
      this.script = script;
      script.setProperty("out", config.getOutput());
    }

    public void execute(LayoutWindow window) throws ScriptException
    {
      script.setProperty("window", window);
      try
      {
        script.run();
      }
      catch (Exception ex)
      {
        int line = -1;
        for (StackTraceElement element : ex.getStackTrace())
        {
          if (element.getClassName().equals(script.getClass().getName())) {
            line = element.getLineNumber()-numImports;
            break;
          }
        }
        throw new ScriptException(ex.getMessage(), line, ex);
      }
    }
  }

  /** Inner class used to represent a compiled ObjectScript. */

  private class CompiledObjectScript implements ObjectScript
  {
    private Script script;

    public CompiledObjectScript(Script script)
    {
      this.script = script;
      script.setProperty("out", config.getOutput());
    }

    public void execute(ScriptedObjectController controller) throws ScriptException
    {
      script.setProperty("script", controller);
      script.run();
    }
  }
}

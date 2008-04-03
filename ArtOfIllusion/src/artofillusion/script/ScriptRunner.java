/* Copyright (C) 2002-2007 by Peter Eastman

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

/** This class is used for executing scripts. */

public class ScriptRunner
{
  static Interpreter interp;
  
  /** Get the interpreter for running scripts.  This ensures that all scripts
      are run with the same interpreter, so they will all share the same
      global namespace. */
  
  public static Interpreter getInterpreter()
  {
    if (interp == null)
      {
        interp = new Interpreter();
        try
          {
            interp.eval("import artofillusion.*");
            interp.eval("import artofillusion.image.*");
            interp.eval("import artofillusion.material.*");
            interp.eval("import artofillusion.math.*");
            interp.eval("import artofillusion.object.*");
            interp.eval("import artofillusion.script.*");
            interp.eval("import artofillusion.texture.*");
            interp.eval("import artofillusion.ui.*");
            interp.eval("import buoy.event.*");
            interp.eval("import buoy.widget.*");
          }
        catch (EvalError e)
          {
            e.printStackTrace();
          }
        PrintStream out = new PrintStream(new ScriptOutputWindow());
        interp.setOut(out);
        interp.setErr(out);
        SearchlistClassLoader loader = new SearchlistClassLoader(ScriptRunner.class.getClassLoader());
        List plugins = PluginRegistry.getPluginClassLoaders();
        for (int i = 0; i < plugins.size(); i++)
        loader.add((ClassLoader) plugins.get(i));
        interp.setClassLoader(loader);
      }
    return interp;
  }
  
  /** Execute a script. */
  
  public static Object executeScript(String script)
  {
    try
      {
        return getInterpreter().eval(script);
      }
    catch (EvalError e)
      {
        System.out.println("Error in line "+e.getErrorLineNumber()+": "+e.getErrorText());
      }
    return null;
  }
  
  /** Parse a Tool script. */
  
  public static ToolScript parseToolScript(String script) throws Exception
  {
    String prefix = "return new ToolScript() {void execute(LayoutWindow window) {\n";
    String suffix = "\n;}}";
    return (ToolScript) getInterpreter().eval(prefix+script+suffix);
  }
  
  /** Parse an Object script. */
  
  public static ObjectScript parseObjectScript(String script) throws Exception
  {
    String prefix = "return new ObjectScript() {void execute(ScriptedObjectController script) {\n";
    String suffix = "\n;}}";
    return (ObjectScript) getInterpreter().eval(prefix+script+suffix);
  }

  /** Display a dialog showing an exception thrown by a script.  This returns the line number
      in which the error occurred, or -1 if it could not be determined. */

  public static int displayError(Exception ex, int lineOffset)
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
          t.getTarget().printStackTrace(interp.getOut());
        }
      else if (ex instanceof EvalError)
        {
          EvalError e = (EvalError) ex;
          head = "An error occurred while parsing the script:";
          message = e.getMessage();
          errorText = e.getErrorText();
          if (e.getErrorLineNumber() > -1)
            line = e.getErrorLineNumber()-lineOffset;
          e.printStackTrace(interp.getOut());
        }
      else
        {
          message = ex.getMessage();
          ex.printStackTrace(interp.getOut());
        }
    }
    catch (Exception ex2)
    {
      ex2.printStackTrace();
    }
    Vector v = new Vector();
    v.addElement(head);
    if (message != null)
    {
      if (message.indexOf("Inline eval of") == -1)
        v.addElement(message);
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
          v.addElement("Encountered \""+errorText+"\" at line "+line+".");
        else
          v.addElement("Encountered \""+errorText+"\" at line "+line+column);
      }
    String msg[] = new String [v.size()];
    v.copyInto(msg);
    new BStandardDialog("Error", msg, BStandardDialog.ERROR).showMessageDialog(null);
    return line;
  }
}

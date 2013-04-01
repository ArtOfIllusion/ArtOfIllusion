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

/**
 * This interface represents an engine that can compile and execute scripts.
 */
public interface ScriptEngine
{
  /**
   * Get the name of the scripting language this engine implements.
   */
  String getName();

  /**
   * Get the filename extension used to identify files written in this engine's language.
   */
  String getFilenameExtension();

  /**
   * Set a stream to which script output should be directed.
   */
  void setOutput(PrintStream out);

  /**
   * Add a package or class that should be automatically imported in every script.
   */
  void addImport(String packageOrClass) throws Exception;

  /**
   * Execute a script.
   *
   * @param script     the source code of the script
   * @param variables  a Map defining values for a set of variables that should be defined in the script
   */
  void executeScript(String script, Map<String, Object> variables) throws ScriptException;

  /**
   * Compile a script that can be executed as a tool script.
   *
   * @param script     the source code of the script
   */
  ToolScript createToolScript(String script) throws ScriptException;

  /**
   * Compile a script that can be executed as an object script.
   *
   * @param script     the source code of the script
   */
  ObjectScript createObjectScript(String script) throws ScriptException;
}

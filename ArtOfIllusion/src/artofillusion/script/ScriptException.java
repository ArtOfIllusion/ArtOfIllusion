package artofillusion.script;

/* Copyright (C) 2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


/**
 * An exception thrown by the ScriptEngine interface.
 */
public class ScriptException extends Exception
{
  private int lineNumber;

  public ScriptException(String message, int lineNumber)
  {
    super(message);
    this.lineNumber = lineNumber;
  }

  public ScriptException(String message, int lineNumber, Throwable cause)
  {
    super(message, cause);
    this.lineNumber = lineNumber;
  }

  /**
   * Get the line in the script at which the error occurred, or -1 if a specific line could not be identified.
   */
  public int getLineNumber()
  {
    return lineNumber;
  }
}

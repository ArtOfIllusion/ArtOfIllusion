/* Copyright (C) 2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

/** This interface defines an object which "owns" a procedure, and helps
    define the user interface for editing it. */

public interface ProcedureOwner
{
  /** Get the title of the procedure's editing window. */
  
  public String getWindowTitle();
  
  /** Create an object which displays a preview of the procedure. */
  
  public Object getPreview(ProcedureEditor editor);
  
  /** Update the display of the preview. */
  
  public void updatePreview(Object preview);
  
  /** Dispose of the preview object when the editor is closed. */
  
  public void disposePreview(Object preview);
  
  /** Determine whether the procedure may contain Parameter modules. */
  
  public boolean allowParameters();
  
  /** Determine whether the procedure may contain View Angle modules. */
  
  public boolean allowViewAngle();
  
  /** Determine whether the procedure may be renamed. */
  
  public boolean canEditName();
  
  /** Get the name of the procedure. */
  
  public String getName();
  
  /** Set the name of the procedure. */
  
  public void setName(String name);
  
  /** This is called when the user clicks OK in the procedure editor. */
  
  public void acceptEdits(ProcedureEditor editor);
  
  /** Display the Properties dialog. */
  
  public void editProperties(ProcedureEditor editor);
}
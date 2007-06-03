/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

/** GenericTool is an EditingTool which performs no operations.  It is generally used simply
    as a button, for allowing the user to select various options.  The constructor takes
    the name of an image file, which is used as the tool's icon. */

public class GenericTool extends EditingTool
{
  private String tipText;

  public GenericTool(EditingWindow fr, String imageName, String tipText)
  {
    super(fr);
    initButton(imageName);
    this.tipText = tipText;
  }

  public void activate()
  {
    super.activate();
  }

  public int whichClicks()
  {
    return HANDLE_CLICKS;
  }

  public String getToolTipText()
  {
    return tipText;
  }
}
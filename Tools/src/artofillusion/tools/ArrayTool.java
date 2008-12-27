/* Copyright 2001-2004 by Rick van der Meiden and Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
*/

package artofillusion.tools;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.util.*;

/**
The array tool creates an array of copies of an object.
@author Rick van der Meiden
*/

public class ArrayTool implements ModellingTool
{
  /** instance this tool,load it in memory */

  public ArrayTool()
  {
  }
  
  /** Get the text that appear as the menu item.*/
  public String getName()
  {
        return Translate.text("menu.array");
  }

  /** See whether an appropriate object is selected and either display an error
     message, or bring up the array tool window. */
  public void commandSelected(LayoutWindow window)
  {
        if (window.getSelectedIndices().length < 1)
            new BStandardDialog("", "You must select one or more objects to create an array from.", BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
        else
            new ArrayDialog(window);
  }

}

/* Copyright (C) 2002-2004 by Norbert Krieg and Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.translators;

import artofillusion.*;
import buoy.widget.*;

/** POVTranslator is a Translator which (imports and) exports Povray files. */

public class POVTranslator implements Translator
{
  public String getName()
  {
    return "Povray V3.5 (.pov)";
  }

  public boolean canImport()
  {
    return false;
  }
  
  public boolean canExport()
  {
    return true;
  }
  
  public void importFile(BFrame parent)
  {
  }
  
  public void exportFile(BFrame parent, Scene theScene)
  {
      POVExporter.exportFile(parent,theScene);
  }
}

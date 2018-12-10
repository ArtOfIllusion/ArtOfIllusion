/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.translators;

import artofillusion.*;
import buoy.widget.*;

/** VRMLTranslator is a Translator which exports (and will eventually import) VRML files. */

public class VRMLTranslator implements Translator
{
  @Override
  public String getName()
  {
    return "VRML";
  }

  @Override
  public boolean canImport()
  {
    return false;
  }
  
  @Override
  public boolean canExport()
  {
    return true;
  }
  
  @Override
  public void importFile(BFrame parent)
  {
  }
  
  @Override
  public void exportFile(BFrame parent, Scene theScene)
  {
    VRMLExporter.exportFile(parent, theScene);
  }
}

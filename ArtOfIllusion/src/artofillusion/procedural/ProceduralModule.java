/* Copyright (C) 2000-2011 by Peter Eastman
   Changes copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import java.awt.*;

/**
 * This represents a module in a procedure. This is an abstract class, whose
 * subclasses represent specific kinds of modules.
 */

public class ProceduralModule extends artofillusion.procedural.Module
{
  public ProceduralModule(String name, IOPort input[], IOPort output[], Point position)
  {
    super(name, input, output, position);
  }
}

/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.util;

import java.io.File;

/**
 *
 * @author MaksK
 */
public final class FileUtils
{
  private static final String FIX_SEPARATOR=".";
  
  private FileUtils()
  {    
  }
  
  public static String getNameNoExtension(File file)
  {
    String fileName = file.getName();
    int pointPos = fileName.lastIndexOf(FIX_SEPARATOR);
    return (pointPos == -1) ? fileName : fileName.substring(0, pointPos);    
  }
  
}

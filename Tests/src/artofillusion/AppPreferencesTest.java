/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import java.io.File;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class AppPreferencesTest
{
  
  public AppPreferencesTest()
  {
  }

  // TODO add test methods here.
  // The methods must be annotated with annotation @Test. For example:
  //
  @Test
  public void hello() {
    File dir = ApplicationPreferences.getPreferencesDirectory();
    Path path = ApplicationPreferences.getPreferencesPath();
    
    Assert.assertEquals(path, dir.toPath());
  }
}

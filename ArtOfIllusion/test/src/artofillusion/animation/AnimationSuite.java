/* Copyright (C) 2016 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author makiam
 * 
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
{
  artofillusion.animation.JointEqualityTest.class, artofillusion.animation.JointDOFEqualityTest.class
})
public class AnimationSuite
{

  @Before
  public void setUp() throws Exception
  {
  }
  
}

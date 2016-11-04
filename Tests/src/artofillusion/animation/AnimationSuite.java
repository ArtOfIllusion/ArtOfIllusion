/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.procedural;

import java.awt.Point;
import org.junit.Assert;

import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class ModuleTest
{
  @Test
  public void testModuleDuplicate() {
    Module source = new ColorScaleModule(new Point(128, 64));
    Module target = source.duplicate();
    
    Assert.assertTrue(target instanceof ColorScaleModule);
    Assert.assertNotNull(target);
    Assert.assertEquals(128, target.getBounds().x);
    Assert.assertEquals(64, target.getBounds().y);
    
  }
}

/* Copyright (C) 2006 by Peter Eastman
   Changes copyright (C) 2017-2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assert;

public class RGBColorTest
{
  /**
   * Test converting to and from ERGB format.
   */

  @Test
  public void testLoadRGBColorFromStream() throws IOException
  {
    byte[] bytes = new byte[12];
    ByteBuffer wrap = ByteBuffer.wrap(bytes);
    wrap.putFloat(0);
    wrap.putFloat(0.5f);
    wrap.putFloat(1.0f);
    
    RGBColor color = new RGBColor(new DataInputStream(new ByteArrayInputStream(bytes)));
    Assert.assertEquals(0, color.red, 0);
    Assert.assertEquals(0.5, color.green, 0);
    Assert.assertEquals(1, color.blue, 0);
  }
  
  
  @Test
  public void testERGB()
  {
    RGBColor c1 = new RGBColor(), c2 = new RGBColor();
    c1.setRGB(0.0f, 0.0f, 0.0f);
    c2.setERGB(c1.getERGB());
    assertColorsEquals(c1, c2, 0.0f);
    
    c1.setRGB(1.0f, 1.0f, 1.0f);
    c2.setERGB(c1.getERGB());
    assertColorsEquals(c1, c2, 0.0f);
    
    for (int i = 0; i < 1000; i++)
    {
      c1.setRGB(Math.random(), Math.random(), Math.random());
      c1.scale(10.0*Math.random());
      c2.setERGB(c1.getERGB());
      assertColorsEquals(c1, c2, c1.getMaxComponent()/128);
    }
  }

  private void assertColorsEquals(RGBColor c1, RGBColor c2, float tol)
  {
    assertEquals(c1.getRed(), c2.getRed(), tol);
    assertEquals(c1.getGreen(), c2.getGreen(), tol);
    assertEquals(c1.getBlue(), c2.getBlue(), tol);
  }
}

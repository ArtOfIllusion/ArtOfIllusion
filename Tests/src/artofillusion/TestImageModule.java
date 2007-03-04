/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import junit.framework.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.procedural.*;
import artofillusion.ui.*;

import java.awt.image.*;
import java.awt.*;
import java.util.*;

public class TestImageModule extends TestCase
{
  private static final int SIZE = 100;

  protected void setUp() throws Exception
  {
    Translate.setLocale(Locale.US);
  }

  /**
   * Test values and gradients in RGB mode.
   */

  public void testRGB() throws InterruptedException
  {
    testColorModel(ImageModule.RGB_MODEL);
  }

  /**
   * Test values and gradients in HSV mode.
   */

  public void testHSV() throws InterruptedException
  {
    testColorModel(ImageModule.HSV_MODEL);
  }

  /**
   * Test values and gradients in HLS mode.
   */

  public void testHLS() throws InterruptedException
  {
    testColorModel(ImageModule.HLS_MODEL);
  }

  private void testColorModel(int colorModel) throws InterruptedException
  {
    ImageModule module = buildModule(colorModel);
    PointInfo info = new PointInfo();
    info.xsize = info.ysize = info.zsize = 2.0/SIZE;
    info.z = 0.0;
    Vec3 grad1 = new Vec3(), grad2 = new Vec3(), grad3 = new Vec3();
    for (int i = 0; i < 1000; i++)
    {
      info.x = Math.random()*0.98;
      info.y = Math.random()*0.98;
      module.init(info);

      // Get the component values, reconstruct the color, and see if it correct.

      double v1 = module.getAverageValue(1, 0.0);
      double v2 = module.getAverageValue(2, 0.0);
      double v3 = module.getAverageValue(3, 0.0);
      compareColors(createColor(v1, v2, v3, colorModel), createColor(Math.cos(info.x), Math.sin(info.y), info.x*info.y, colorModel), 0.02);

      // Get the comopnent gradients, use them to predict the color a short distance away, and see if it is correct.

      module.getValueGradient(1, grad1, 0.0);
      module.getValueGradient(2, grad2, 0.0);
      module.getValueGradient(3, grad3, 0.0);
      info.x += 0.01;
      info.y += 0.01;
      module.init(info);
      compareColors(createColor(v1+grad1.x*0.01+grad1.y*0.01, v2+grad2.x*0.01+grad2.y*0.01, v3+grad3.x*0.01+grad3.y*0.01, colorModel),
          createColor(module.getAverageValue(1, 0.0), module.getAverageValue(2, 0.0), module.getAverageValue(3, 0.0), colorModel), 0.04);
    }
  }

  /**
   * Create a color based on its component values in a specified color model.
   */

  private RGBColor createColor(double component1, double component2, double component3, int colorModel)
  {
    if (colorModel == ImageModule.RGB_MODEL)
      return new RGBColor(component1, component2, component3);
    RGBColor color = new RGBColor();
    if (colorModel == ImageModule.HSV_MODEL)
      color.setHSV((float) component1, (float) component2, (float) component3);
    else
      color.setHLS((float) component1, (float) component2, (float) component3);
    return color;
  }

  /**
   * Compare two colors and make sure they are sufficiently close.
   */

  private void compareColors(RGBColor color1, RGBColor color2, double tolerance)
  {
    assertEquals(color1.getRed(), color2.getRed(), tolerance);
    assertEquals(color1.getGreen(), color2.getGreen(), tolerance);
    assertEquals(color1.getBlue(), color2.getBlue(), tolerance);
  }

  /**
   * Build a test image.
   */

  private ImageModule buildModule(int colorModel) throws InterruptedException
  {
    BufferedImage im = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    int pixel[] = ((DataBufferInt) ((BufferedImage) im).getRaster().getDataBuffer()).getData();
    for (int i = 0; i < SIZE; i++)
      for (int j = 0; j < SIZE; j++)
      {
        float x = i/(float) SIZE;
        float y = 1.0f-(j/(float) SIZE);
        RGBColor color = createColor(Math.cos(x), Math.sin(y), x*y, colorModel);
        pixel[i+SIZE*j] = color.getARGB();
      }
    ImageMap map = new MIPMappedImage(im);
    ImageModule module = new ImageModule(new Point());
    module.setMap(map);
    module.setColorModel(colorModel);
    module.setTileX(false);
    module.setTileY(false);
    return module;
  }
}

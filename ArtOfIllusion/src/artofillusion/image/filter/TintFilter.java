/* Copyright (C) 2003-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image.filter;

import artofillusion.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** This is an image filter which multiplies an image by a color. */

public class TintFilter extends ImageFilter
{
  public TintFilter()
  {
  }

  /** Get the name of this filter.*/

  public String getName()
  {
    return Translate.text("Tint");
  }
  
  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */
  
  public void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    filterComponent(image, ComplexImage.RED, (float) paramValue[0]);
    filterComponent(image, ComplexImage.GREEN, (float) paramValue[1]);
    filterComponent(image, ComplexImage.BLUE, (float) paramValue[2]);
  }
  
  /** Apply the filter to one component of an image. */
  
  private void filterComponent(ComplexImage image, int component, float scale)
  {
    int width = image.getWidth(), height = image.getHeight();
    float filtered[] = new float [width*height];
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
        filtered[i+j*width] = image.getPixelComponent(i, j, component)*scale;
    image.setComponentValues(component, filtered);
  }
  
  /** Get a list of parameters which affect the behavior of the filter. */
  
  public TextureParameter [] getParameters()
  {
    return new TextureParameter [] {
        new TextureParameter(this, Translate.text("Red"), 0.0, Double.MAX_VALUE, 1.0),
        new TextureParameter(this, Translate.text("Green"), 0.0, Double.MAX_VALUE, 1.0),
        new TextureParameter(this, Translate.text("Blue"), 0.0, Double.MAX_VALUE, 1.0),
    };
  }

  /** Write a serialized description of this filter to a stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    for (int i = 0; i < paramValue.length; i++)
      out.writeDouble(paramValue[i]);
  }

  /** Reconstruct this filter from its serialized representation. */
  
  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    for (int i = 0; i < paramValue.length; i++)
      paramValue[i] = in.readDouble();
  }

  /** Get a Widget with which the user can specify options for the filter.
   * @param changeCallback*/

  public Widget getConfigPanel(final Runnable changeCallback)
  {
    final RGBColor color = new RGBColor(paramValue[0], paramValue[1], paramValue[2]);
    final Widget sample = color.getSample(40, 30);
    RowContainer row = new RowContainer();
    row.add(new BLabel(Translate.text("Color")+": "));
    row.add(sample);
    sample.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(UIUtilities.findFrame(sample), Translate.text("chooseTintColor"), color);
        paramValue[0] = color.getRed();
        paramValue[1] = color.getGreen();
        paramValue[2] = color.getBlue();
        sample.setBackground(color.getColor());
        changeCallback.run();
      }
    });
    UIUtilities.applyBackground(row, null);
    sample.setBackground(color.getColor());
    return row;
  }
}

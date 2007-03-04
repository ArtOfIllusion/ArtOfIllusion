/* Copyright (C) 2000-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.util.Vector;

/** ImageOrColor represents a color which can potentially vary with position.  It provides a 
    Widget with which the user can edit it.  They can choose either a single color, or an 
    ImageMap which is then multiplied by a user specified color. */

public class ImageOrColor
{
  private ImageMap map;
  private RGBColor color;
  
  public ImageOrColor(RGBColor theColor)
  {
    color = theColor.duplicate();
  }
  
  public ImageOrColor(RGBColor scaleColor, ImageMap theMap)
  {
    color = scaleColor.duplicate();
    map = theMap;
  }
  
  /** Get the image, or null if it is a single color. */
  
  public ImageMap getImage()
  {
    return map;
  }

  /** Get the color by which the image is multiplied. */
  
  public RGBColor getColor()
  {
    return color;
  }  
  
  /** Given a texture coordinate (x and y each between 0 and 1), return the average color of
      a region of width (xsize, ysize) centered at that location.  wrapx and wrapy specify 
      whether, for purposes of interpolation, the image should be treated as wrapping around 
      so that opposite edges touch each other. */
  
  public void getColor(RGBColor theColor, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    if (map == null)
      theColor.copy(color);
    else
      {
	map.getColor(theColor, wrapx, wrapy, x, y, xsize, ysize);
	theColor.multiply(color);
      }
  }

  /** Return the average color over the entire image. */
  
  public void getAverageColor(RGBColor theColor)
  {
    if (map == null)
      theColor.copy(color);
    else
      {
	theColor.setRGB(map.getAverageComponent(0), map.getAverageComponent(1), map.getAverageComponent(2));
	theColor.multiply(color);
      }
  }
  
  /** Create a duplicate of this object. */
  
  public ImageOrColor duplicate()
  {
    ImageOrColor copy = new ImageOrColor(color);
    
    copy.map = map;
    return copy;
  }
  
  /** Make this object identical to another one. */
  
  public void copy(ImageOrColor obj)
  {
    map = obj.map;
    color.copy(obj.color);
  }

  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */
  
  public ImageOrColor(DataInputStream in, Scene theScene) throws IOException
  {
    int index = in.readInt();
    
    if (index > -1)
      map = theScene.getImage(index);
    color = new RGBColor(in);
  }
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    if (map == null)
      out.writeInt(-1);
    else
      out.writeInt(theScene.indexOf(map));
    color.writeToFile(out);
  }

  /** Get a Widget with which the user can edit the color.  This Widget will send out
      ValueChangedEvents whenever the color changes.
      @param parent    a parent BFrame which can be used for displaying dialogs
      @param theScene  the Scene from which to get images
  */
  
  public Widget getEditingPanel(final BFrame parent, final Scene theScene)
  {
    final RowContainer row = new RowContainer();
    final CustomWidget preview = new CustomWidget();
    preview.setPreferredSize(new Dimension(ImageMap.PREVIEW_WIDTH, ImageMap.PREVIEW_HEIGHT));
    preview.addEventLink(RepaintEvent.class, new Object() {
      void processEvent(RepaintEvent ev)
      {
        Graphics2D g = ev.getGraphics();
        if (map != null)
          g.drawImage(map.getPreview(), 0, 0, null);
        else
          g.drawRect(1, 1, ImageMap.PREVIEW_WIDTH-2, ImageMap.PREVIEW_HEIGHT-2);
      }
    });
    preview.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent(MouseClickedEvent ev)
      {
        ImagesDialog dlg = new ImagesDialog(parent, theScene, map);
        map = dlg.getSelection();
        preview.repaint();
        row.dispatchEvent(new ValueChangedEvent(row));
      }
    });
    final Widget colorPatch = color.getSample(ImageMap.PREVIEW_WIDTH, ImageMap.PREVIEW_HEIGHT);
    colorPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent(MouseClickedEvent ev)
      {
        new ColorChooser(parent, Translate.text("Color"), color);
        colorPatch.setBackground(color.getColor());
        colorPatch.repaint();
        row.dispatchEvent(new ValueChangedEvent(row));
      }
    });
    row.add(preview);
    row.add(colorPatch);
    return row;
  }
}
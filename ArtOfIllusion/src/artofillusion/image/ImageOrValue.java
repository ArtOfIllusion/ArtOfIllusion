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

/** ImageOrValue represents a float value between 0 and 1, which can potentially vary with
    position.  It provides a Widget with which the user can edit it.  They can choose either
    a single value, or a component of an ImageMap which can be multiplied by a user specified
    value. */

public class ImageOrValue
{
  private ImageMap map;
  private float value;
  private int component;
  
  public ImageOrValue(float val)
  {
    value = val;
  }
  
  public ImageOrValue(float scale, ImageMap theMap, int whichComponent)
  {
    value = scale;
    map = theMap;
    component = whichComponent;
  }
  
  /** Get the image, or null if it is a single value. */
  
  public ImageMap getImage()
  {
    return map;
  }
  
  /** Get which component of the image to use. */
  
  public int getComponent()
  {
    return component;
  }
  
  /** Get the value by which the image is scaled. */
  
  public float getValue()
  {
    return value;
  }
  
  /** Given a texture coordinate (x and y each between 0 and 1), return the average value 
      of a region of width (xsize, ysize) centered at that location.  wrapx and wrapy specify
      whether, for purposes of interpolation, the image should be treated as wrapping around 
      so that opposite edges touch each other. */
  
  public float getValue(boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    if (map == null)
      return value;
    else
      return map.getComponent(component, wrapx, wrapy, x, y, xsize, ysize)*value;
  }

  /** Given a texture coordinate, get the gradient of the image. */

  public void getGradient(Vec2 grad, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    if (map == null)
      grad.set(0.0, 0.0);
    else
      {
        map.getGradient(grad, component, wrapx, wrapy, x, y, xsize, ysize);
        grad.scale(value);
      }
  }

  /** Return the average value over the entire image. */
  
  public float getAverageValue()
  {
    if (map == null)
      return value;
    else
      return map.getAverageComponent(component)*value;
  }
  
  /** Create a duplicate of this object. */
  
  public ImageOrValue duplicate()
  {
    ImageOrValue copy = new ImageOrValue(value);
    
    copy.map = map;
    copy.component = component;
    return copy;
  }
  
  /** Make this object identical to another one. */
  
  public void copy(ImageOrValue obj)
  {
    value = obj.value;
    map = obj.map;
    component = obj.component;
  }

  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */
  
  public ImageOrValue(DataInputStream in, Scene theScene) throws IOException
  {
    int index = in.readInt();
    
    if (index > -1)
      map = theScene.getImage(index);
    value = in.readFloat();
    component = in.readInt();
  }
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    if (map == null)
      out.writeInt(-1);
    else
      out.writeInt(theScene.indexOf(map));
    out.writeFloat(value);
    out.writeInt(component);
  }

  /** Get a Widget with which the user can edit the value.  This Widget will send out
      ValueChangedEvents whenever the value changes.
      @param parent    a parent BFrame which can be used for displaying dialogs
      @param theScene  the Scene from which to get images
  */
  
  public Widget getEditingPanel(BFrame parent, Scene theScene)
  {
    return new EditingPanel(parent, theScene);
  }
  
  private class EditingPanel extends FormContainer
  {
    BFrame fr;
    Scene sc;
    CustomWidget preview;
    BComboBox componentChoice;
    BLabel componentLabel, valueLabel;
    ValueSlider slider;

    public EditingPanel(BFrame parent, Scene theScene)
    {
      super(new double [] {0.0, 1.0, 0.0}, new double [] {1.0, 1.0});
      fr = parent;
      sc = theScene;
      preview = new CustomWidget();
      preview.setPreferredSize(new Dimension(ImageMap.PREVIEW_WIDTH, ImageMap.PREVIEW_HEIGHT));
      preview.addEventLink(RepaintEvent.class, this, "paint");
      preview.addEventLink(MouseClickedEvent.class, this, "previewClicked");
      componentChoice = new BComboBox(new String [] {
        Translate.text("Red"),
        Translate.text("Green"),
        Translate.text("Blue")
      });
      componentChoice.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      componentLabel = new BLabel(Translate.text("Component")+":");
      valueLabel = new BLabel();
      slider = new ValueSlider(0.0, 1.0, 100, value);
      slider.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      add(preview, 0, 0, 1, 2);
      add(valueLabel, 1, 0);
      add(componentLabel, 2, 0);
      add(slider, 1, 1);
      add(componentChoice, 2, 1);
      updateComponents();
    }
    
    private void updateComponents()
    {
      dispatchEvent(new ValueChangedEvent(this));
      if (map == null)
        valueLabel.setText(Translate.text("Value")+":");
      else
        valueLabel.setText(Translate.text("Scale")+":");
      if (map != null && map.getComponentCount() > componentChoice.getItemCount())
        componentChoice.add(Translate.text("Mask"));
      else if (componentChoice.getItemCount() == 4)
        componentChoice.remove(3);
      if (map == null || map.getComponentCount() == 1)
      {
        componentChoice.setEnabled(false);
        ImageOrValue.this.component = 0;
        return;
      }
      componentChoice.setEnabled(true);
      if (ImageOrValue.this.component >= componentChoice.getItemCount())
        ImageOrValue.this.component = 0;
      componentChoice.setSelectedIndex(ImageOrValue.this.component);
    }
    
    private void paint(RepaintEvent ev)
    {
      Graphics2D g = ev.getGraphics();
      if (map != null)
        g.drawImage(map.getPreview(), 0, 0, null);
      else
        g.drawRect(1, 1, ImageMap.PREVIEW_WIDTH-2, ImageMap.PREVIEW_HEIGHT-2);
    }
    
    private void previewClicked()
    {
      ImagesDialog dlg = new ImagesDialog(fr, sc, map);
      map = dlg.getSelection();
      updateComponents();
      preview.repaint();
    }

    private void valueChanged()
    {
      value = (float) slider.getValue();
      ImageOrValue.this.component = componentChoice.getSelectedIndex();
      dispatchEvent(new ValueChangedEvent(this));
    }
  }
}
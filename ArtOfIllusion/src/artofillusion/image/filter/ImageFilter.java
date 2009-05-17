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
import java.awt.*;
import java.io.*;

/** This class defines an object which can filter rendered images.  It is an abstract class, whose subclasses implement
    specific types of filters.  An ImageFilter may present a user interface for configuring filtering options,
    and also may have a list of keyframeable parameters. */

public abstract class ImageFilter
{
  protected double paramValue[];
  
  /** Every ImageFilter subclass must provide a constructor which takes no arguments. */
  
  public ImageFilter()
  {
    TextureParameter param[] = getParameters();
    paramValue = new double [param.length];
    for (int i = 0; i < param.length; i++)
      paramValue[i] = param[i].defaultVal;
  }

  /** Get the name of this filter. */
  
  public abstract String getName();
  
  /** Get a list of all the image components required by this filter.  This should be a sum of the constants
      defined in ComplexImage.  The renderer will attempt to provide all requested components, but some renderers
      may not support all components.  The filter should therefore be prepared for the possibility that some
      components may be null (aside from the basic red, green, blue, and alpha components, which are always
      available). */
  
  public int getDesiredComponents()
  {
    // The default implementation requests red, green, and blue.  Subclasses with other needs should override this.
    
    return ComplexImage.RED+ComplexImage.GREEN+ComplexImage.BLUE;
  }
  
  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */
  
  public abstract void filterImage(ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos);
  
  /** Create an exact duplicate of this filter. */
  
  public ImageFilter duplicate()
  {
    try
      {
        ImageFilter f = (ImageFilter) getClass().newInstance();
        f.copy(this);
        return f;
      }
    catch (Exception ex)
      {
        ex.printStackTrace();
      }
    return null;
  }
  
  /** Given another ImageFilter (of the same class as this one), make this one identical to it. */
  
  public void copy(ImageFilter f)
  {
    // Subclasses should override this if necessary to copy any additional information.
    
    paramValue = new double [f.paramValue.length];
    for (int i = 0; i < paramValue.length; i++)
      paramValue[i] = f.paramValue[i];
  }
  
  /** Get a list of parameters which affect the behavior of the filter. */
  
  public TextureParameter [] getParameters()
  {
    // Subclasses which have parameters should override this.
    
    return new TextureParameter [0];
  }
  
  /** Get the list of parameter values. */
  
  public double [] getParameterValues()
  {
    double val[] = new double [paramValue.length];
    System.arraycopy(paramValue, 0, val, 0, val.length);
    return val;
  }
  
  /** Set the value of a parameter. */
  
  public void setParameterValue(int which, double value)
  {
    paramValue[which] = value;
  }

  /** Write a serialized description of this filter to a stream. */
  
  public abstract void writeToStream(DataOutputStream out, Scene theScene) throws IOException;

  /** Reconstruct this filter from its serialized representation. */
  
  public abstract void initFromStream(DataInputStream in, Scene theScene) throws IOException;

  /** Get a Widget with which the user can specify options for the filter.
   *
   * @param changeCallback    a Runnable which should be invoked whenever the filter's configuration
   *                          changes, so the containing window can update its preview
   */

  public Widget getConfigPanel(final Runnable changeCallback)
  {
    // The default implementation simply allows the parameters to be edited.  Subclasses may override
    // this to provide more complex configuration panels.
    
    final TextureParameter param[] = getParameters();
    FormContainer form = new FormContainer(2, param.length);
    
    // Create the labels.
    
    LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    for (int i = 0; i < param.length; i++)
      form.add(new BLabel(param[i].name+": "), 0, i, leftLayout);
    
    // Define an inner class to act as a listener on a ValueField.
    
    class FieldListener
    {
      private int which;
      private ValueField field;
      
      public FieldListener(int which, ValueField field)
      {
        this.which = which;
        this.field = field;
      }
      
      void processEvent()
      {
        setParameterValue(which, field.getValue());
        changeCallback.run();
      }
    }
    
    // Define an inner class to act as a listener on a ValueSlider.
    
    class SliderListener
    {
      private int which;
      private ValueSlider slider;
      
      public SliderListener(int which, ValueSlider slider)
      {
        this.which = which;
        this.slider = slider;
      }
      
      void processEvent()
      {
        setParameterValue(which, slider.getValue());
        changeCallback.run();
      }
    }
    
    // Create the editing components.
    
    LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
    for (int i = 0; i < param.length; i++)
    {
      Widget w = param[i].getEditingWidget(paramValue[i]);
      if (w instanceof ValueField)
        w.addEventLink(ValueChangedEvent.class, new FieldListener(i, (ValueField) w));
      else
        w.addEventLink(ValueChangedEvent.class, new SliderListener(i, (ValueSlider) w));
      form.add(w, 1, i, rightLayout);
    }
    UIUtilities.applyBackground(form, null);
    return form;
  }
}
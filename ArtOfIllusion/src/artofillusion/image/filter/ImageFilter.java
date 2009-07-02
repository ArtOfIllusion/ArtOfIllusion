/* Copyright (C) 2003-2009 by Peter Eastman

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

import javax.swing.border.*;
import java.awt.*;
import java.io.*;

/** This class defines an object which can filter rendered images.  It is an abstract class, whose subclasses implement
    specific types of filters.  An ImageFilter may present a user interface for configuring filtering options,
    and also may have a list of keyframeable parameters. */

public abstract class ImageFilter
{
  @Deprecated
  protected double paramValue[];
  private Object propertyValue[];
  
  /** Every ImageFilter subclass must provide a constructor which takes no arguments. */
  
  public ImageFilter()
  {
    Property[] properties = getProperties();
    propertyValue = new Object[properties.length];
    for (int i = 0; i < properties.length; i++)
      propertyValue[i] = properties[i].getDefaultValue();

    // This is for backward compatibility with ImageFilters that use the deprecated
    // TextureParameter interface.

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
    propertyValue = new Object [f.propertyValue.length];
    for (int i = 0; i < propertyValue.length; i++)
      propertyValue[i] = f.propertyValue[i];

  }
  
  /** This method is deprecated.  Call getProperties() instead. */
  
  @Deprecated
  public TextureParameter [] getParameters()
  {
    return new TextureParameter [0];
  }
  
  /** This method is deprecated.  Call getPropertyValue() instead. */

  @Deprecated
  public double [] getParameterValues()
  {
    double val[] = new double [paramValue.length];
    System.arraycopy(paramValue, 0, val, 0, val.length);
    return val;
  }
  
  /** This method is deprecated.  Call setPropertyValue() instead. */

  @Deprecated
  public void setParameterValue(int index, double value)
  {
    paramValue[index] = value;
    propertyValue[index] = value;
  }

  /**
   * Get a list of Properties which affect the behavior of the filter.
   */

  public Property[] getProperties()
  {
    // The default implementation is for backward compatibility with ImageFilters that
    // use the deprecated TextureParameter interface.  Subclasses should override it to
    // return the actual list of Properties for the filter.

    TextureParameter[] parameters = getParameters();
    Property properties[] = new Property[parameters.length];
    for (int i = 0; i < parameters.length; i++)
      properties[i] = new Property(parameters[i].name, parameters[i].minVal, parameters[i].maxVal, parameters[i].defaultVal);
    return properties;
  }

  /**
   * Get the value of a Property.
   *
   * @param index    the index of the Property to get
   */

  public Object getPropertyValue(int index)
  {
    if (index < paramValue.length)
      return paramValue[index];
    return propertyValue[index];
  }

  /**
   * Set the value of a Property.
   *
   * @param index    the index of the Property to set
   * @param value    the value of the Property
   */

  public void setPropertyValue(int index, Object value)
  {
    propertyValue[index] = value;

    // This is for backward compatibility with ImageFilters that use the deprecated
    // TextureParameter interface.

    if (value instanceof Number && paramValue.length > index)
      paramValue[index] = ((Number) value).doubleValue();
  }

  /** Write a serialized description of this filter to a stream. */
  
  public abstract void writeToStream(DataOutputStream out, Scene theScene) throws IOException;

  /** Reconstruct this filter from its serialized representation. */
  
  public abstract void initFromStream(DataInputStream in, Scene theScene) throws IOException;

  /**
   * Get a Widget with which the user can specify options for the filter.
   *
   * @param changeCallback    a Runnable which should be invoked whenever the filter's configuration
   *                          changes, so the containing window can update its preview
   */

  public Widget getConfigPanel(final Runnable changeCallback)
  {
    // The default implementation simply allows the Properties to be edited.  Subclasses may override
    // this to provide more complex configuration panels.
    
    final Property properties[] = getProperties();
    FormContainer form = new FormContainer(2, properties.length);
    
    // Define an inner class to act as a listener on a PropertyEditors.
    
    class EditorListener
    {
      private int which;
      private PropertyEditor editor;

      public EditorListener(int which, PropertyEditor editor)
      {
        this.which = which;
        this.editor = editor;
      }
      
      void processEvent()
      {
        setPropertyValue(which, editor.getValue());
        changeCallback.run();
      }
    }

    // Create the editing components.
    
    LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
    for (int i = 0; i < properties.length; i++)
    {
      PropertyEditor editor = new PropertyEditor(properties[i], getPropertyValue(i));
      if (editor.getLabel() != null)
        form.add(new BLabel(editor.getLabel()+": "), 0, i, leftLayout);
      Widget w = editor.getWidget();
      w.addEventLink(ValueChangedEvent.class, new EditorListener(i, editor));
      form.add(w, 1, i, rightLayout);
    }
    UIUtilities.applyBackground(form, null);
    BOutline border = new BOutline(form, new EmptyBorder(3, 3, 3, 3));
    border.setBackground(null);
    return border;
  }
}
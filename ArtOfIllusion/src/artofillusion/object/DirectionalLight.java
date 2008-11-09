/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** DirectionalLight represents a distant light source which emits light in one direction
    from outside the scene. */

public class DirectionalLight extends Light
{
  private double radius;

  static BoundingBox bounds;
  static WireframeMesh mesh;
  static final int SEGMENTS = 8;
  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("lightColor"), new RGBColor(1.0, 1.0, 1.0)),
    new Property(Translate.text("Intensity"), -Double.MAX_VALUE, Double.MAX_VALUE, 1.0),
    new Property(Translate.text("AngularRadius"), 0.0, 45.0, 1.0),
    new Property(Translate.text("lightType"), new String[] {Translate.text("normalLight"), Translate.text("shadowlessLight"), Translate.text("ambientLight")}, Translate.text("normalLight"))
  };

  static {
    double sine[] = new double [SEGMENTS];
    double cosine[] = new double [SEGMENTS];
    Vec3 vert[];
    int i, from[], to[];

    bounds = new BoundingBox(-0.15, 0.15, -0.15, 0.15, -0.15, 0.25);
    for (i = 0; i < SEGMENTS; i++)
      {
        sine[i] = Math.sin(i*2.0*Math.PI/SEGMENTS);
        cosine[i] = Math.cos(i*2.0*Math.PI/SEGMENTS);
      }
    vert = new Vec3 [SEGMENTS*4];
    from = new int [SEGMENTS*4];
    to = new int [SEGMENTS*4];
    for (i = 0; i < SEGMENTS; i++)
      {
        vert[i] = new Vec3(0.15*cosine[i], 0.15*sine[i], -0.15);
        vert[i+SEGMENTS] = new Vec3(0.15*cosine[i], 0.15*sine[i], 0.0);
        vert[i+2*SEGMENTS] = new Vec3(0.15*cosine[i], 0.15*sine[i], 0.05);
        vert[i+3*SEGMENTS] = new Vec3(0.15*cosine[i], 0.15*sine[i], 0.25);
        from[i] = i;
        to[i] = (i+1)%SEGMENTS;
        from[i+SEGMENTS] = i;
        to[i+SEGMENTS] = i+SEGMENTS;
        from[i+2*SEGMENTS] = i+SEGMENTS;
        to[i+2*SEGMENTS] = (i+1)%SEGMENTS+SEGMENTS;
        from[i+3*SEGMENTS] = i+2*SEGMENTS;
        to[i+3*SEGMENTS] = i+3*SEGMENTS;
      }
    mesh = new WireframeMesh(vert, from, to);
  }
  
  public DirectionalLight(RGBColor theColor, float theIntensity)
  {
    this(theColor, theIntensity, 1.0);
  }

  public DirectionalLight(RGBColor theColor, float theIntensity, double theRadius)
  {
    setParameters(theColor.duplicate(), theIntensity, TYPE_NORMAL, 0.5f);
    setRadius(theRadius);
  }

  public Object3D duplicate()
  {
    return new DirectionalLight(color, intensity, radius);
  }
  
  public void copyObject(Object3D obj)
  {
    DirectionalLight lt = (DirectionalLight) obj;

    setParameters(lt.color.duplicate(), lt.intensity, lt.type, lt.decayRate);
    setRadius(lt.getRadius());
  }

  public BoundingBox getBounds()
  {
    return bounds;
  }

  /** A DirectionalLight has no size.  Hence, calls to setSize() are ignored. */

  public void setSize(double xsize, double ysize, double zsize)
  {
  }

  /**
   * Get the angular radius (in degrees) over which light is emitted.
   */

  public double getRadius()
  {
    return radius;
  }

  /**
   * Set the angular radius (in degrees) over which light is emitted.
   */

  public void setRadius(double r)
  {
    radius = r;
  }

  /**
   * Directional lights are not attenuated with distance, since the light source is far
   * outside the scene.
   */

  public void getLight(RGBColor light, Vec3 position)
  {
    light.copy(color);
    light.scale(intensity);
  }
  
  public boolean canSetTexture()
  {
    return false;
  }
  
  public WireframeMesh getWireframeMesh()
  {
    return mesh;
  }

  public boolean isEditable()
  {
    return true;
  }
  
  
  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public DirectionalLight(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version < 0 || version > 2)
      throw new InvalidObjectException("");
    setParameters(new RGBColor(in), in.readFloat(), version == 0 ? TYPE_NORMAL : in.readShort(), 0.0f);
    setRadius(version > 1 ? in.readDouble() : 0.0);
    bounds = new BoundingBox(-0.15, 0.15, -0.15, 0.15, -0.15, 0.25);
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(2);
    color.writeToFile(out);
    out.writeFloat(intensity);
    out.writeShort(type);
    out.writeDouble(radius);
  }

  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    final Widget patch = color.getSample(50, 30);
    ValueField intensityField = new ValueField(intensity, ValueField.NONE);
    ValueSelector radiusField = new ValueSelector(radius, 0.0, 45.0, 0.1);
    BComboBox typeChoice = new BComboBox(new String[] {Translate.text("normalLight"), Translate.text("shadowlessLight"), Translate.text("ambientLight")});
    typeChoice.setSelectedIndex(type);
    RGBColor oldColor = color.duplicate();
    final BFrame parentFrame = parent.getFrame();
    
    patch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(parentFrame, Translate.text("lightColor"), color);
        patch.setBackground(color.getColor());
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(parentFrame, Translate.text("editDirectionalLightTitle"), 
        new Widget [] {patch, intensityField, radiusField, typeChoice}, new String [] {Translate.text("Color"), Translate.text("Intensity"), Translate.text("AngularRadius"), Translate.text("lightType")});
    if (!dlg.clickedOk())
    {
      color.copy(oldColor);
      return;
    }
    setParameters(color, (float) intensityField.getValue(), typeChoice.getSelectedIndex(), decayRate);
    setRadius(radiusField.getValue());
    cb.run();
  }

  public Property[] getProperties()
  {
    return (Property []) PROPERTIES.clone();
  }

  public Object getPropertyValue(int index)
  {
    switch (index)
    {
      case 0:
        return color.duplicate();
      case 1:
        return new Double(intensity);
      case 2:
        return new Double(radius);
      case 3:
        return PROPERTIES[index].getAllowedValues()[type];
    }
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
    if (index == 0)
      color = ((RGBColor) value).duplicate();
    else if (index == 1)
      intensity = ((Double) value).floatValue();
    else if (index == 2)
      radius = ((Double) value).doubleValue();
    else if (index == 3)
    {
      Object values[] = PROPERTIES[index].getAllowedValues();
      for (int i = 0; i < values.length; i++)
        if (values[i].equals(value))
          type = i;
    }
  }

  /* Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new DirectionalLightKeyframe(color, intensity, radius);
  }
  
  /* Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    DirectionalLightKeyframe key = (DirectionalLightKeyframe) k;
    
    setParameters(key.color.duplicate(), key.intensity, type, 0.5f);
    setRadius(key.radius);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"Intensity", "AngularRadius"},
        new double [] {intensity, radius},
        new double [][] {{-Double.MAX_VALUE, Double.MAX_VALUE}, {0.0, 45.0}});
  }

  /* Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    final DirectionalLightKeyframe key = (DirectionalLightKeyframe) k;
    final Widget patch = key.color.getSample(50, 30);
    ValueField intensityField = new ValueField(key.intensity, ValueField.NONE);
    ValueSelector radiusField = new ValueSelector(key.radius, 0.0, 45.0, 0.1);
    RGBColor oldColor = key.color.duplicate();
    final BFrame parentFrame = parent.getFrame();
    
    patch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(parentFrame, Translate.text("lightColor"), key.color);
        patch.setBackground(key.color.getColor());
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(parentFrame, Translate.text("editDirectionalLightTitle"), 
        new Widget [] {patch, intensityField, radiusField},
        new String [] {Translate.text("Color"), Translate.text("Intensity"), Translate.text("AngularRadius")});
    if (!dlg.clickedOk())
    {
      key.color.copy(oldColor);
      return;
    }
    key.intensity = (float) intensityField.getValue();
    key.radius = radiusField.getValue();
  }
  
  /* Inner class representing a pose for a directional light. */
  
  public static class DirectionalLightKeyframe implements Keyframe
  {
    public RGBColor color;
    public float intensity;
    public double radius;
    
    public DirectionalLightKeyframe(RGBColor color, float intensity, double radius)
    {
      this.color = color.duplicate();
      this.intensity = intensity;
      this.radius = radius;
    }
    
    /* Create a duplicate of this keyframe.. */
  
    public Keyframe duplicate()
    {
      return new DirectionalLightKeyframe(color, intensity, radius);
    }
    
    /* Create a duplicate of this keyframe for a (possibly different) object. */
  
    public Keyframe duplicate(Object owner)
    {
      return duplicate();
    }
  
    /* Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      return new double [] {intensity, radius};
    }
  
    /* Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
      intensity = (float) values[0];
      radius = values[1];
    }

    /* These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      DirectionalLightKeyframe k2 = (DirectionalLightKeyframe) o2;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue());
      return new DirectionalLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity), weight1*radius+weight2*k2.radius);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      DirectionalLightKeyframe k2 = (DirectionalLightKeyframe) o2, k3 = (DirectionalLightKeyframe) o3;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed()+weight3*k3.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen()+weight3*k3.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue()+weight3*k3.color.getBlue());
      return new DirectionalLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity+weight3*k3.intensity), weight1*radius+weight2*k2.radius+weight3*k3.radius);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      DirectionalLightKeyframe k2 = (DirectionalLightKeyframe) o2, k3 = (DirectionalLightKeyframe) o3, k4 = (DirectionalLightKeyframe) o4;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed()+weight3*k3.color.getRed()+weight4*k4.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen()+weight3*k3.color.getGreen()+weight4*k4.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue()+weight3*k3.color.getBlue()+weight4*k4.color.getBlue());
      return new DirectionalLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity+weight3*k3.intensity+weight4*k4.intensity), weight1*radius+weight2*k2.radius+weight3*k3.radius+weight4*k4.radius);
    }

    /* Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof DirectionalLightKeyframe))
        return false;
      DirectionalLightKeyframe key = (DirectionalLightKeyframe) k;
      return (key.color.equals(color) && key.intensity == intensity && key.radius == radius);
    }
  
    /* Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      color.writeToFile(out);
      out.writeFloat(intensity);
      out.writeFloat((float) radius);
    }

    /* Reconstructs the keyframe from its serialized representation. */

    public DirectionalLightKeyframe(DataInputStream in, Object parent) throws IOException
    {
      this(new RGBColor(in), in.readFloat(), in.readFloat());
    }
  }
}
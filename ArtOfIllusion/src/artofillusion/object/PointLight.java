/* Copyright (C) 1999-2007 by Peter Eastman

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

/** PointLight represents a light source which emits light equally in all directions. */

public class PointLight extends Light
{
  double radius;
  
  static BoundingBox bounds;
  static WireframeMesh mesh;
  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("lightColor"), new RGBColor(1.0, 1.0, 1.0)),
    new Property(Translate.text("Intensity"), -Double.MAX_VALUE, Double.MAX_VALUE, 1.0),
    new Property(Translate.text("decayRate"), 0.0, Double.MAX_VALUE, 0.25),
    new Property(Translate.text("Radius"), 0.0, Double.MAX_VALUE, 0.1),
    new Property(Translate.text("lightType"), new String[] {Translate.text("normalLight"), Translate.text("shadowlessLight"), Translate.text("ambientLight")}, Translate.text("normalLight"))
  };

  static
  {
    Vec3 vert[];
    double r1 = 0.05, r2 = 0.25, i, j, k;
    int ind1, ind2, from[], to[];

    bounds = new BoundingBox(-0.25, 0.25, -0.25, 0.25, -0.25, 0.25);
    vert = new Vec3 [28];
    from = new int [14];
    to = new int [14];
    vert[0] = new Vec3(r1, 0.0, 0.0);
    vert[1] = new Vec3(r2, 0.0, 0.0);
    from[0] = 0;
    to[0] = 1;
    vert[2] = new Vec3(-r1, 0.0, 0.0);
    vert[3] = new Vec3(-r2, 0.0, 0.0);
    from[1] = 2;
    to[1] = 3;
    vert[4] = new Vec3(0.0, r1, 0.0);
    vert[5] = new Vec3(0.0, r2, 0.0);
    from[2] = 4;
    to[2] = 5;
    vert[6] = new Vec3(0.0, -r1, 0.0);
    vert[7] = new Vec3(0.0, -r2, 0.0);
    from[3] = 6;
    to[3] = 7;
    vert[8] = new Vec3(0.0, 0.0, r1);
    vert[9] = new Vec3(0.0, 0.0, r2);
    from[4] = 8;
    to[4] = 9;
    vert[10] = new Vec3(0.0, 0.0, -r1);
    vert[11] = new Vec3(0.0, 0.0, -r2);
    from[5] = 10;
    to[5] = 11;

    r1 *= 0.57735; // 1/sqrt(3).
    r2 *= 0.57735;
    ind1 = 12;
    ind2 = 6;
    for (i = -1.0; i < 2.0; i += 2.0)
      for (j = -1.0; j < 2.0; j += 2.0)
	for (k = -1.0; k < 2.0; k += 2.0)
	  {
	    vert[ind1++] = new Vec3(r1*i, r1*j, r1*k);
	    vert[ind1++] = new Vec3(r2*i, r2*j, r2*k);
	    from[ind2] = ind1-2;
	    to[ind2++] = ind1-1;
	  }
    mesh = new WireframeMesh(vert, from, to);
  }
  
  public PointLight(RGBColor theColor, float theIntensity, double theRadius)
  {
    this(theColor, theIntensity, theRadius, TYPE_NORMAL, 0.25f);
  }
  
  public PointLight(RGBColor theColor, float theIntensity, double theRadius, int type, float decay)
  {
    setParameters(theColor.duplicate(), theIntensity, type, decay);
    setRadius(theRadius);
    bounds = new BoundingBox(-0.25, 0.25, -0.25, 0.25, -0.25, 0.25);
  }
  
  public double getRadius()
  {
    return radius;
  }
  
  public void setRadius(double r)
  {
    radius = r;
  }

  /**
   * Get the attenuated light at a given position relative to the light source.
   */

  public void getLight(RGBColor light, Vec3 position)
  {
    double d = position.length()*decayRate;
    light.copy(color);
    light.scale(intensity/(1.0f+d+d*d));
  }

  public Object3D duplicate()
  {
    return new PointLight(color, intensity, radius, type, decayRate);
  }
  
  public void copyObject(Object3D obj)
  {
    PointLight lt = (PointLight) obj;

    setParameters(lt.color.duplicate(), lt.intensity, lt.type, lt.decayRate);
    setRadius(lt.radius);
  }

  public BoundingBox getBounds()
  {
    return bounds;
  }

  /** A PointLight is always drawn the same size, which has no connection to the properties
     of the light. */

  public void setSize(double xsize, double ysize, double zsize)
  {
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
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    final Widget patch = color.getSample(50, 30);
    ValueField intensityField = new ValueField(intensity, ValueField.NONE);
    ValueField radiusField = new ValueField(radius, ValueField.NONNEGATIVE);
    ValueField decayField = new ValueField(decayRate, ValueField.NONNEGATIVE);
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
    ComponentsDialog dlg = new ComponentsDialog(parentFrame, Translate.text("editPointLightTitle"), 
	new Widget [] {patch, intensityField, radiusField, decayField, typeChoice},
	new String [] {Translate.text("Color"), Translate.text("Intensity"), Translate.text("Radius"), Translate.text("decayRate"), Translate.text("lightType")});
    if (!dlg.clickedOk())
    {
      color.copy(oldColor);
      return;
    }
    setParameters(color, (float) intensityField.getValue(), typeChoice.getSelectedIndex(),
        (float) decayField.getValue());
    setRadius(radiusField.getValue());
    cb.run();
  }

  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */

  public PointLight(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    setParameters(new RGBColor(in), in.readFloat(), version == 0 ? (in.readBoolean() ? TYPE_AMBIENT : TYPE_NORMAL) : in.readShort(), in.readFloat());
    setRadius(in.readDouble());
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(1);
    color.writeToFile(out);
    out.writeFloat(intensity);
    out.writeShort(type);
    out.writeFloat(decayRate);
    out.writeDouble(radius);
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
        return new Double(decayRate);
      case 3:
        return new Double(radius);
      case 4:
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
      decayRate = ((Double) value).floatValue();
    else if (index == 3)
      radius = ((Double) value).doubleValue();
    else if (index == 4)
    {
      Object values[] = PROPERTIES[index].getAllowedValues();
      for (int i = 0; i < values.length; i++)
        if (values[i].equals(value))
          type = i;
    }
  }

  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new PointLightKeyframe(color, intensity, decayRate, radius);
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    PointLightKeyframe key = (PointLightKeyframe) k;
    
    setParameters(key.color.duplicate(), key.intensity, type, key.decayRate);
    setRadius(key.radius);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"Intensity", "Decay Rate", "Radius"},
        new double [] {intensity, decayRate, radius}, 
        new double [][] {{-Double.MAX_VALUE, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}});
  }
  
  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    final PointLightKeyframe key = (PointLightKeyframe) k;
    final Widget patch = key.color.getSample(50, 30);
    ValueField intensityField = new ValueField(key.intensity, ValueField.NONE);
    ValueField radiusField = new ValueField(key.radius, ValueField.NONNEGATIVE);
    ValueField decayField = new ValueField(key.decayRate, ValueField.NONNEGATIVE);
    RGBColor oldColor = key.color.duplicate();
    final BFrame parentFrame = parent.getFrame();
    
    patch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(parentFrame, Translate.text("lightColor"), key.color);
        patch.setBackground(key.color.getColor());
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(parentFrame, Translate.text("editPointLightTitle"), 
	new Widget [] {patch, intensityField, radiusField, decayField}, 
	new String [] {Translate.text("Color"), Translate.text("Intensity"), Translate.text("Radius"), Translate.text("decayRate")});
    if (!dlg.clickedOk())
    {
      key.color.copy(oldColor);
      return;
    }
    key.intensity = (float) intensityField.getValue(); 
    key.decayRate = (float) decayField.getValue();
    key.radius = radiusField.getValue();
  }
  
  /** Inner class representing a pose for a cylinder. */
  
  public static class PointLightKeyframe implements Keyframe
  {
    public RGBColor color;
    public float intensity, decayRate;
    public double radius;
    
    public PointLightKeyframe(RGBColor color, float intensity, float decayRate, double radius)
    {
      this.color = color.duplicate();
      this.intensity = intensity;
      this.decayRate = decayRate;
      this.radius = radius;
    }
    
    /** Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return new PointLightKeyframe(color, intensity, decayRate, radius);
    }
    
    /** Create a duplicate of this keyframe for a (possibly different) object. */
  
    public Keyframe duplicate(Object owner)
    {
      return duplicate();
    }
  
    /** Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      return new double [] {intensity, decayRate, radius};
    }
  
    /** Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
      intensity = (float) values[0];
      decayRate = (float) values[1];
      radius = values[2];
    }

    /** These methods return a new Keyframe which is a weighted average of this one and one,
        two, or three others. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      PointLightKeyframe k2 = (PointLightKeyframe) o2;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue());
      return new PointLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity), 
        (float) (weight1*decayRate+weight2*k2.decayRate), 
        weight1*radius+weight2*k2.radius);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      PointLightKeyframe k2 = (PointLightKeyframe) o2, k3 = (PointLightKeyframe) o3;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed()+weight3*k3.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen()+weight3*k3.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue()+weight3*k3.color.getBlue());
      return new PointLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity+weight3*k3.intensity), 
        (float) (weight1*decayRate+weight2*k2.decayRate+weight3*k3.decayRate), 
        weight1*radius+weight2*k2.radius+weight3*k3.radius);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      PointLightKeyframe k2 = (PointLightKeyframe) o2, k3 = (PointLightKeyframe) o3, k4 = (PointLightKeyframe) o4;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed()+weight3*k3.color.getRed()+weight4*k4.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen()+weight3*k3.color.getGreen()+weight4*k4.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue()+weight3*k3.color.getBlue()+weight4*k4.color.getBlue());
      return new PointLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity+weight3*k3.intensity+weight4*k4.intensity), 
        (float) (weight1*decayRate+weight2*k2.decayRate+weight3*k3.decayRate+weight4*k4.decayRate), 
        weight1*radius+weight2*k2.radius+weight3*k3.radius+weight4*k4.radius);
    }

    /** Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof PointLightKeyframe))
        return false;
      PointLightKeyframe key = (PointLightKeyframe) k;
      return (key.color.equals(color) && key.intensity == intensity && key.decayRate == decayRate && key.radius == radius);
    }
  
    /** Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      color.writeToFile(out);
      out.writeFloat(intensity);
      out.writeFloat(decayRate);
      out.writeDouble(radius);
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public PointLightKeyframe(DataInputStream in, Object parent) throws IOException
    {
      this(new RGBColor(in), in.readFloat(), in.readFloat(), in.readDouble());
    }
  }
}
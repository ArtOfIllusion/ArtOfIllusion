/* Copyright (C) 2000-2007 by Peter Eastman

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
import java.awt.*;
import java.awt.image.*;
import java.io.*;

/** SpotLight represents a light source which emits a cone of light in a specified direction. */

public class SpotLight extends Light 
{
  double radius, angle, falloff, cosangle, exponent;
  
  static BoundingBox bounds;
  static WireframeMesh mesh;
  static final int SEGMENTS = 8;
  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("lightColor"), new RGBColor(1.0, 1.0, 1.0)),
    new Property(Translate.text("Intensity"), -Double.MAX_VALUE, Double.MAX_VALUE, 1.0),
    new Property(Translate.text("coneAngle"), 0.0, 180.0, 30.0),
    new Property(Translate.text("falloffRate"), 0.0, 1.0, 0.0),
    new Property(Translate.text("decayRate"), 0.0, Double.MAX_VALUE, 0.25),
    new Property(Translate.text("Radius"), 0.0, Double.MAX_VALUE, 0.1),
    new Property(Translate.text("lightType"), new String[] {Translate.text("normalLight"), Translate.text("shadowlessLight"), Translate.text("ambientLight")}, Translate.text("normalLight"))
  };

  static {
    double sine[] = new double [SEGMENTS];
    double cosine[] = new double [SEGMENTS];
    Vec3 vert[];
    int i, from[], to[];

    bounds = new BoundingBox(-0.2, 0.2, -0.2, 0.2, -0.2, 0.2);
    for (i = 0; i < SEGMENTS; i++)
      {
        sine[i] = Math.sin(i*2.0*Math.PI/SEGMENTS);
        cosine[i] = Math.cos(i*2.0*Math.PI/SEGMENTS);
      }
    vert = new Vec3 [SEGMENTS*3+1];
    from = new int [SEGMENTS*3];
    to = new int [SEGMENTS*3];
    vert[SEGMENTS*3] = new Vec3(0.0, 0.0, -0.2);
    for (i = 0; i < SEGMENTS; i++)
      {
        vert[i] = new Vec3(0.075*cosine[i], 0.075*sine[i], -0.05);
        vert[i+SEGMENTS] = new Vec3(0.1*cosine[i], 0.1*sine[i], 0.0);
        vert[i+2*SEGMENTS] = new Vec3(0.2*cosine[i], 0.2*sine[i], 0.2);
        from[i] = SEGMENTS*3;
        to[i] = i;
        from[i+SEGMENTS] = i;
        to[i+SEGMENTS] = (i+1)%SEGMENTS;
        from[i+2*SEGMENTS] = i+SEGMENTS;
        to[i+2*SEGMENTS] = i+2*SEGMENTS;
      }
    mesh = new WireframeMesh(vert, from, to);
  }

  public SpotLight(RGBColor theColor, float theIntensity, double theAngle, double falloffRate, double theRadius)
  {
    this(theColor, theIntensity, theAngle, falloffRate, theRadius, TYPE_NORMAL, 0.25f);
  }
  
  public SpotLight(RGBColor theColor, float theIntensity, double theAngle, double falloffRate, double theRadius, int type, float decay)
  {
    setParameters(theColor.duplicate(), theIntensity, type, decay);
    setRadius(theRadius);
    setAngle(theAngle);
    setFalloff(falloffRate);
  }
  
  public double getRadius()
  {
    return radius;
  }
  
  public void setRadius(double r)
  {
    radius = r;
  }
  
  public double getAngle()
  {
    return angle;
  }
  
  public void setAngle(double a)
  {
    angle = a;
    cosangle = Math.cos(angle*Math.PI/360.0);
  }
  
  public double getFalloff()
  {
    return falloff;
  }
  
  public void setFalloff(double f)
  {
    falloff = f;
    exponent = f*f*128.0;
  }
  
  public double getAngleCosine()
  {
    return cosangle;
  }
  
  public double getExponent()
  {
    return exponent;
  }
  
  /**
   * Get the attenuated light at a given position relative to the light source.
   */

  public void getLight(RGBColor light, Vec3 position)
  {
    double distance = position.length();
    double fatt = position.z/distance;
    if (fatt < cosangle)
      light.setRGB(0.0f, 0.0f, 0.0f);
    else
    {
      double d = distance*decayRate;
      light.copy(color);
      double scale = intensity/(1.0f+d+d*d);
      if (exponent > 0.0)
        scale *= Math.pow(fatt, exponent);
      light.scale(scale);
    }
  }

  public Object3D duplicate()
  {
    return new SpotLight(color, intensity, angle, falloff, radius, type, decayRate);
  }
  
  public void copyObject(Object3D obj)
  {
    SpotLight lt = (SpotLight) obj;

    setParameters(lt.color.duplicate(), lt.intensity, lt.type, lt.decayRate);
    setRadius(lt.radius);
    setAngle(lt.angle);
    setFalloff(lt.falloff);
  }

  public BoundingBox getBounds()
  {
    return bounds;
  }

  /* A SpotLight is always drawn the same size, which has no connection to the properties
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
  
  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public SpotLight(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    setParameters(new RGBColor(in), in.readFloat(), version == 0 ? (in.readBoolean() ? TYPE_AMBIENT : TYPE_NORMAL) : in.readShort(), in.readFloat());
    setRadius(in.readDouble());
    setAngle(in.readDouble());
    setFalloff(in.readDouble());
    bounds = new BoundingBox(-0.2, 0.2, -0.2, 0.2, -0.2, 0.2);
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
    out.writeDouble(angle);
    out.writeDouble(falloff);
  }

  public void edit(EditingWindow parent, ObjectInfo info, final Runnable cb)
  {
    final Widget patch = color.getSample(50, 30);
    final ValueField intensityField = new ValueField(intensity, ValueField.NONE);
    final ValueField radiusField = new ValueField(radius, ValueField.NONNEGATIVE);
    final ValueField decayField = new ValueField(decayRate, ValueField.NONNEGATIVE);
    final ValueSlider angleSlider = new ValueSlider(0.0, 180.0, 180, angle);
    final ValueSlider falloffSlider = new ValueSlider(0.0, 1.0, 100, falloff);
    final BComboBox typeChoice = new BComboBox(new String[] {Translate.text("normalLight"), Translate.text("shadowlessLight"), Translate.text("ambientLight")});
    typeChoice.setSelectedIndex(type);
    final Preview preview = new Preview(100);
    final RGBColor oldColor = color.duplicate();
    final BFrame parentFrame = parent.getFrame();
    final BDialog dlg = new BDialog(parentFrame, "", true);
    FormContainer content = new FormContainer(3, 9);

    dlg.setContent(content);
    LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    LayoutInfo widgetLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE);
    content.add(Translate.label("editSpotLightTitle"), 0, 0, 3, 1);
    content.add(Translate.label("coneAngle"), 0, 1, labelLayout);
    content.add(Translate.label("falloffRate"), 0, 2, labelLayout);
    content.add(Translate.label("Color"), 0, 3, labelLayout);
    content.add(Translate.label("Intensity"), 0, 4, labelLayout);
    content.add(Translate.label("Radius"), 0, 5, labelLayout);
    content.add(Translate.label("decayRate"), 0, 6, labelLayout);
    content.add(Translate.label("lightType"), 0, 7, labelLayout);
    content.add(angleSlider, 1, 1, widgetLayout);
    content.add(falloffSlider, 1, 2, widgetLayout);
    content.add(patch, 1, 3, widgetLayout);
    content.add(intensityField, 1, 4, widgetLayout);
    content.add(radiusField, 1, 5, widgetLayout);
    content.add(decayField, 1, 6, widgetLayout);
    content.add(typeChoice, 1, 7, widgetLayout);
    content.add(preview, 2, 1, 1, 7);
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 8, 3, 1);
    BButton okButton = Translate.button("ok", new Object() {
      void processEvent()
      {
        setParameters(color, (float) intensityField.getValue(), typeChoice.getSelectedIndex(),
            (float) decayField.getValue());
        setRadius(radiusField.getValue());
        setAngle(angleSlider.getValue());
        setFalloff(falloffSlider.getValue());
        dlg.dispose();
        cb.run();
      }
    }, "processEvent");
    buttons.add(okButton);
    BButton cancelButton = Translate.button("cancel", new Object() {
      void processEvent()
      {
        color.copy(oldColor);
        dlg.dispose();
      }
    }, "processEvent");
    buttons.add(cancelButton);
    patch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(parentFrame, Translate.text("lightColor"), color);
        patch.setBackground(color.getColor());
        preview.updateImage(angleSlider.getValue(), falloffSlider.getValue());
      }
    });
    Object listener = new Object() {
      void processEvent()
      {
        preview.updateImage(angleSlider.getValue(), falloffSlider.getValue());
      }
    };
    angleSlider.addEventLink(ValueChangedEvent.class, listener);
    falloffSlider.addEventLink(ValueChangedEvent.class, listener);
    dlg.pack();
    UIUtilities.centerDialog(dlg, parentFrame);
    dlg.setVisible(true);
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
        return new Double(angle);
      case 3:
        return new Double(falloff);
      case 4:
        return new Double(decayRate);
      case 5:
        return new Double(radius);
      case 6:
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
      setAngle(((Double) value).floatValue());
    else if (index == 3)
      setFalloff(((Double) value).floatValue());
    else if (index == 4)
      decayRate = ((Double) value).floatValue();
    else if (index == 5)
      radius = ((Double) value).doubleValue();
    else if (index == 6)
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
    return new SpotLightKeyframe(color, intensity, decayRate, radius, angle, falloff);
  }
  
  /* Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    SpotLightKeyframe key = (SpotLightKeyframe) k;
    
    setParameters(key.color.duplicate(), key.intensity, type, key.decayRate);
    setRadius(key.radius);
    setAngle(key.angle);
    setFalloff(key.falloff);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"Intensity", "Decay Rate", "Radius", "Cone Angle", "Falloff Rate"},
        new double [] {intensity, decayRate, radius, angle, falloff}, 
        new double [][] {{-Double.MAX_VALUE, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE},
        {0.0, Double.MAX_VALUE}, {0.0, 180.0}, {0.0, 1.0}});
  }
  
  /* Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    final SpotLightKeyframe key = (SpotLightKeyframe) k;
    final Widget patch = key.color.getSample(50, 30);
    final ValueField intensityField = new ValueField(key.intensity, ValueField.NONE);
    final ValueField radiusField = new ValueField(key.radius, ValueField.NONNEGATIVE);
    final ValueField decayField = new ValueField(key.decayRate, ValueField.NONNEGATIVE);
    final ValueSlider angleSlider = new ValueSlider(0.0, 180.0, 180, key.angle);
    final ValueSlider falloffSlider = new ValueSlider(0.0, 1.0, 100, key.falloff);
    final Preview preview = new Preview(100);
    final RGBColor oldColor = key.color.duplicate();
    final BFrame parentFrame = parent.getFrame();
    final BDialog dlg = new BDialog(parentFrame, "", true);
    FormContainer content = new FormContainer(3, 8);

    dlg.setContent(content);
    LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    content.add(Translate.label("editSpotLightTitle"), 0, 0, 3, 1);
    content.add(Translate.label("coneAngle"), 0, 1, labelLayout);
    content.add(Translate.label("falloffRate"), 0, 2, labelLayout);
    content.add(Translate.label("Color"), 0, 3, labelLayout);
    content.add(Translate.label("Intensity"), 0, 4, labelLayout);
    content.add(Translate.label("Radius"), 0, 5, labelLayout);
    content.add(Translate.label("decayRate"), 0, 6, labelLayout);
    content.add(angleSlider, 1, 1);
    content.add(falloffSlider, 1, 2);
    content.add(patch, 1, 3);
    content.add(intensityField, 1, 4);
    content.add(radiusField, 1, 5);
    content.add(decayField, 1, 6);
    content.add(preview, 2, 1, 1, 6);
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 7, 3, 1);
    BButton okButton = Translate.button("ok", new Object() {
      void processEvent()
      {
        key.intensity = (float) intensityField.getValue();
        key.decayRate = (float) decayField.getValue();
        key.radius = radiusField.getValue();
        key.angle = angleSlider.getValue();
        key.falloff = falloffSlider.getValue();
        dlg.dispose();
      }
    }, "processEvent");
    buttons.add(okButton);
    BButton cancelButton = Translate.button("cancel", new Object() {
      void processEvent()
      {
        key.color.copy(oldColor);
        dlg.dispose();
      }
    }, "processEvent");
    buttons.add(cancelButton);
    patch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(parentFrame, Translate.text("lightColor"), key.color);
        patch.setBackground(key.color.getColor());
        preview.updateImage(angleSlider.getValue(), falloffSlider.getValue());
      }
    });
    Object listener = new Object() {
      void processEvent()
      {
        preview.updateImage(angleSlider.getValue(), falloffSlider.getValue());
      }
    };
    angleSlider.addEventLink(ValueChangedEvent.class, listener);
    falloffSlider.addEventLink(ValueChangedEvent.class, listener);
    dlg.pack();
    UIUtilities.centerDialog(dlg, parentFrame);
    dlg.setVisible(true);
  }

  /* Inner class representing a pose for a cylinder. */
  
  public static class SpotLightKeyframe implements Keyframe
  {
    public RGBColor color;
    public float intensity, decayRate;
    public double radius, angle, falloff;
    
    public SpotLightKeyframe(RGBColor color, float intensity, float decayRate, double radius, double angle, double falloff)
    {
      this.color = color.duplicate();
      this.intensity = intensity;
      this.decayRate = decayRate;
      this.radius = radius;
      this.angle = angle;
      this.falloff = falloff;
    }
    
    /* Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return new SpotLightKeyframe(color, intensity, decayRate, radius, angle, falloff);
    }
    
    /* Create a duplicate of this keyframe for a (possibly different) object. */
  
    public Keyframe duplicate(Object owner)
    {
      return duplicate();
    }
  
    /* Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      return new double [] {intensity, decayRate, radius, angle, falloff};
    }
  
    /* Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
      intensity = (float) values[0];
      decayRate = (float) values[1];
      radius = values[2];
      angle = values[3];
      falloff = values[4];
    }

    /* These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      SpotLightKeyframe k2 = (SpotLightKeyframe) o2;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue());
      return new SpotLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity), 
        (float) (weight1*decayRate+weight2*k2.decayRate), weight1*radius+weight2*k2.radius, 
        weight1*angle+weight2*k2.angle, weight1*falloff+weight2*k2.falloff);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      SpotLightKeyframe k2 = (SpotLightKeyframe) o2, k3 = (SpotLightKeyframe) o3;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed()+weight3*k3.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen()+weight3*k3.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue()+weight3*k3.color.getBlue());
      return new SpotLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity+weight3*k3.intensity), 
        (float) (weight1*decayRate+weight2*k2.decayRate+weight3*k3.decayRate), 
        weight1*radius+weight2*k2.radius+weight3*k3.radius,
        weight1*angle+weight2*k2.angle+weight3*k3.angle,
        weight1*falloff+weight2*k2.falloff+weight3*k3.falloff);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      SpotLightKeyframe k2 = (SpotLightKeyframe) o2, k3 = (SpotLightKeyframe) o3, k4 = (SpotLightKeyframe) o4;
      RGBColor c = new RGBColor(weight1*color.getRed()+weight2*k2.color.getRed()+weight3*k3.color.getRed()+weight4*k4.color.getRed(),
        weight1*color.getGreen()+weight2*k2.color.getGreen()+weight3*k3.color.getGreen()+weight4*k4.color.getGreen(),
        weight1*color.getBlue()+weight2*k2.color.getBlue()+weight3*k3.color.getBlue()+weight4*k4.color.getBlue());
      return new SpotLightKeyframe(c, (float) (weight1*intensity+weight2*k2.intensity+weight3*k3.intensity+weight4*k4.intensity), 
        (float) (weight1*decayRate+weight2*k2.decayRate+weight3*k3.decayRate+weight4*k4.decayRate), 
        weight1*radius+weight2*k2.radius+weight3*k3.radius+weight4*k4.radius,
        weight1*angle+weight2*k2.angle+weight3*k3.angle+weight4*k4.angle,
        weight1*falloff+weight2*k2.falloff+weight3*k3.falloff+weight4*k4.falloff);
    }

    /* Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof SpotLightKeyframe))
        return false;
      SpotLightKeyframe key = (SpotLightKeyframe) k;
      return (key.color.equals(color) && key.intensity == intensity && key.decayRate == decayRate && key.radius == radius && key.angle == angle && key.falloff == falloff);
    }
  
    /* Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      color.writeToFile(out);
      out.writeFloat(intensity);
      out.writeFloat(decayRate);
      out.writeDouble(radius);
      out.writeDouble(angle);
      out.writeDouble(falloff);
    }

    /* Reconstructs the keyframe from its serialized representation. */

    public SpotLightKeyframe(DataInputStream in, Object parent) throws IOException
    {
      this(new RGBColor(in), in.readFloat(), in.readFloat(), in.readDouble(), in.readDouble(), in.readDouble());
    }
  }
  
  /* Preview is a Widget which displays a preview of the light distribution created by the
     spotlight. */
  
  private class Preview extends CustomWidget
  {
    MemoryImageSource imageSource;
    int pixel[], size;
    Image img;
    
    public Preview(int size)
    {
      this.size = size;
      setPreferredSize(new Dimension(size, size));
      pixel = new int [size*size];
      imageSource = new MemoryImageSource(size, size, pixel, 0, size);
      imageSource.setAnimated(true);
      img = Toolkit.getDefaultToolkit().createImage(imageSource);
      addEventLink(RepaintEvent.class, this, "paint");
      updateImage(angle, falloff);
    }
    
    public synchronized void updateImage(double ang, double fall)
    {
      int i, j, first;
      double center = size/2.0, tn = Math.tan(ang*Math.PI/360.0);
      double ex = fall*fall*128.0, cs;
      RGBColor col = new RGBColor(0.0f, 0.0f, 0.0f);
      
      for (i = 0; i < size; i++)
      {
        first = (int) Math.abs((i-center)/tn);
        if (first > size)
          first = size;
        for (j = 0; j < first; j++)
          pixel[i*size+j] = 0xFF000000;
        for (j = first; j < size; j++)
        {
          cs = j/Math.sqrt(j*j+(i-center)*(i-center));
          col.copy(color);
          col.scale(Math.pow(cs, ex));
          pixel[i*size+j] = col.getARGB();
        }
      }
      imageSource.newPixels();
      repaint();
    }
    
    private void paint(RepaintEvent ev)
    {
      ev.getGraphics().drawImage(img, 0, 0, getComponent());
    }
  }
}
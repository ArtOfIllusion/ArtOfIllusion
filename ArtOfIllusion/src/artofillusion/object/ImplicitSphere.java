/* Copyright (C) 2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;

import java.io.*;

/**
 * This is a spherical implicit object (i.e. a metaball).  It is characterized by two numbers: a radius, which is the
 * radius of the sphere it creates in isolation, and an "influence radius", which is the distance it extends outward
 * before the implicit function becomes zero.  The influence radius <i>must</i> by larger than the radius, or the behavior
 * becomes undefined.  This class is generally not useful on its own, but collections of ImplicitSpheres
 * are useful for many sorts of effects where balls should smoothly merge together when they come close.
 */
public class ImplicitSphere extends ImplicitObject
{
  private double radius, influenceRadius, scale;
  private BoundingBox bounds;
  private RenderingMesh cachedMesh;
  private WireframeMesh cachedWire;
  private static final Property PROPERTIES[] = new Property [] {
      new Property(Translate.text("radius"), 0.0, Double.MAX_VALUE, 1.0),
      new Property(Translate.text("influenceRadius"), 0.0, Double.MAX_VALUE, 1.0)
  };

  public ImplicitSphere(double radius, double influenceRadius)
  {
    this.radius = radius;
    this.influenceRadius = influenceRadius;
    sizeChanged();
  }

  public double getRadius()
  {
    return radius;
  }

  public void setRadius(double radius)
  {
    this.radius = radius;
    sizeChanged();
  }

  public double getInfluenceRadius()
  {
    return influenceRadius;
  }

  public void setInfluenceRadius(double influenceRadius)
  {
    this.influenceRadius = influenceRadius;
    sizeChanged();
  }

  private void sizeChanged()
  {
    double f = influenceRadius/radius-1;
    scale = 1/(f*f);
    bounds = new BoundingBox(-influenceRadius, influenceRadius, -influenceRadius, influenceRadius, -influenceRadius, influenceRadius);
    cachedWire = null;
    cachedMesh = null;
  }

  @Override
  public double getCutoff()
  {
    return 1;
  }

  @Override
  public boolean getPreferDirectRendering()
  {
    return true;
  }

  @Override
  public double getFieldValue(double x, double y, double z, double size, double time)
  {
    double r = Math.sqrt(x*x+y*y+z*z);
    if (r >= influenceRadius)
      return 0;
    double f = influenceRadius/r-1;
    return scale*f*f;
  }

  @Override
  public void getFieldGradient(double x, double y, double z, double size, double time, Vec3 grad)
  {
    double r = Math.sqrt(x*x+y*y+z*z);
    if (r >= influenceRadius)
      grad.set(0, 0, 0);
    else
    {
      double f = influenceRadius/r-1;
      double c = -scale*2*f*influenceRadius/(r*r*r);
      grad.set(c*x, c*y, c*z);
    }
  }

  @Override
  public void applyPoseKeyframe(Keyframe k)
  {
    ImplicitSphereKeyframe key = (ImplicitSphereKeyframe) k;
    radius = key.radius;
    influenceRadius = key.influenceRadius;
    sizeChanged();
  }

  /** This will be called whenever a new pose track is created for this object.  It allows
   the object to configure the track by setting its graphable values, subtracks, etc. */

  @Override
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {Translate.text("radius"), Translate.text("influenceRadius")},
        new double [] {radius, influenceRadius},
        new double [][] {{0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}});
  }

  /** Return an array containing the names of the graphable values for the keyframes
   returned by getPoseKeyframe(). */

  public String [] getPoseValueNames()
  {
    return new String [] {Translate.text("radius"), Translate.text("influenceRadius")};
  }

  /** Get the default list of graphable values for a keyframe returned by getPoseKeyframe(). */

  public double [] getDefaultPoseValues()
  {
    return new double [] {radius, influenceRadius};
  }

  @Override
  public Object3D duplicate()
  {
    return new ImplicitSphere(radius, influenceRadius);
  }

  @Override
  public void copyObject(Object3D obj)
  {
    ImplicitSphere sphere = (ImplicitSphere) obj;
    radius = sphere.getRadius();
    influenceRadius = sphere.getInfluenceRadius();
    sizeChanged();
  }

  @Override
  public BoundingBox getBounds()
  {
    return bounds;
  }

  @Override
  public void setSize(double xsize, double ysize, double zsize)
  {
  }

  @Override
  public WireframeMesh getWireframeMesh()
  {
    if (cachedWire == null)
      cachedWire = new Sphere(radius, radius, radius).getWireframeMesh();
    return cachedWire;
  }

  @Override
  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    if (cachedMesh == null)
    {
      Sphere sphere = new Sphere(radius, radius, radius);
      sphere.setTexture(getTexture(), getTextureMapping());
      cachedMesh = sphere.getRenderingMesh(tol, interactive, info);
    }
    return cachedMesh;
  }

  @Override
  public Keyframe getPoseKeyframe()
  {
    return new ImplicitSphereKeyframe(radius, influenceRadius);
  }

  @Override
  public Property[] getProperties()
  {
    return (Property []) PROPERTIES.clone();
  }

  @Override
  public Object getPropertyValue(int index)
  {
    switch (index)
    {
      case 0:
        return radius;
      case 1:
        return influenceRadius;
    }
    return null;
  }

  @Override
  public void setPropertyValue(int index, Object value)
  {
    double val = (Double) value;
    if (index == 0)
      setRadius(val);
    else if (index == 1)
      setInfluenceRadius(val);
  }

  @Override
  public void setTexture(Texture tex, TextureMapping mapping)
  {
    super.setTexture(tex, mapping);
    cachedMesh = null;
  }

  @Override
  public void setMaterial(Material mat, MaterialMapping map)
  {
    super.setMaterial(mat, map);
    cachedMesh = null;
  }

  @Override
  public boolean isEditable()
  {
    return true;
  }

  @Override
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    ValueField radiusField = new ValueField(radius, ValueField.POSITIVE, 5);
    ValueField influenceField = new ValueField(influenceRadius, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(),
        Translate.text("editImplicitSphereTitle"), new Widget[] {radiusField, influenceField},
        new String [] {Translate.text("radius"), Translate.text("influenceRadius")});
    if (!dlg.clickedOk())
      return;
    radius = radiusField.getValue();
    influenceRadius = influenceField.getValue();
    sizeChanged();
    cb.run();
  }

  /** The following two methods are used for reading and writing files.  The first is a
   constructor which reads the necessary data from an input stream.  The other writes
   the object's representation to an output stream. */

  public ImplicitSphere(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    radius = in.readDouble();
    influenceRadius = in.readDouble();
    sizeChanged();
  }

  @Override
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(0);
    out.writeDouble(radius);
    out.writeDouble(influenceRadius);
  }

  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */

  @Override
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    ImplicitSphereKeyframe key = (ImplicitSphereKeyframe) k;
    ValueField radiusField = new ValueField(key.radius, ValueField.POSITIVE, 5);
    ValueField influenceField = new ValueField(key.influenceRadius, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(),
        Translate.text("editImplicitSphereTitle"), new Widget[] {radiusField, influenceField},
        new String [] {Translate.text("radius"), Translate.text("influenceRadius")});
    if (!dlg.clickedOk())
      return;
    key.radius = radiusField.getValue();
    key.influenceRadius = influenceField.getValue();
  }

  /** Inner class representing a pose for an ImplicitSphere. */

  public static class ImplicitSphereKeyframe implements Keyframe
  {
    public double radius, influenceRadius;

    public ImplicitSphereKeyframe(double radius, double influenceRadius)
    {
      this.radius = radius;
      this.influenceRadius = influenceRadius;
    }

    /** Create a duplicate of this keyframe. */

    public Keyframe duplicate()
    {
      return new ImplicitSphereKeyframe(radius, influenceRadius);
    }

    /** Create a duplicate of this keyframe for a (possibly different) object. */

    public Keyframe duplicate(Object owner)
    {
      return new ImplicitSphereKeyframe(radius, influenceRadius);
    }

    /** Get the list of graphable values for this keyframe. */

    public double [] getGraphValues()
    {
      return new double [] {radius, influenceRadius};
    }

    /** Set the list of graphable values for this keyframe. */

    public void setGraphValues(double values[])
    {
      radius = values[0];
      influenceRadius = values[1];
    }

    /** These methods return a new Keyframe which is a weighted average of this one and one,
     two, or three others. */

    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      ImplicitSphereKeyframe k2 = (ImplicitSphereKeyframe) o2;

      return new ImplicitSphereKeyframe(weight1*radius+weight2*k2.radius, weight1*influenceRadius+weight2*k2.influenceRadius);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      ImplicitSphereKeyframe k2 = (ImplicitSphereKeyframe) o2, k3 = (ImplicitSphereKeyframe) o3;

      return new ImplicitSphereKeyframe(weight1*radius+weight2*k2.radius+weight3*k3.radius,
          weight1*influenceRadius+weight2*k2.influenceRadius+weight3*k3.influenceRadius);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      ImplicitSphereKeyframe k2 = (ImplicitSphereKeyframe) o2, k3 = (ImplicitSphereKeyframe) o3, k4 = (ImplicitSphereKeyframe) o4;

      return new ImplicitSphereKeyframe(weight1*radius+weight2*k2.radius+weight3*k3.radius+weight4*k4.radius,
          weight1*influenceRadius+weight2*k2.influenceRadius+weight3*k3.influenceRadius+weight4*k4.influenceRadius);
    }

    /** Determine whether this keyframe is identical to another one. */

    public boolean equals(Keyframe k)
    {
      if (!(k instanceof ImplicitSphereKeyframe))
        return false;
      ImplicitSphereKeyframe key = (ImplicitSphereKeyframe) k;
      return (key.radius == radius && key.influenceRadius == influenceRadius);
    }

    /** Write out a representation of this keyframe to a stream. */

    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeDouble(radius);
      out.writeDouble(influenceRadius);
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public ImplicitSphereKeyframe(DataInputStream in, Object parent) throws IOException
    {
      this(in.readDouble(), in.readDouble());
    }
  }
}

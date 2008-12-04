/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.texture.*;
import artofillusion.procedural.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.awt.*;
import java.util.*;

/** This is a PointLight whose emitted light is calculated by a Procedure. */

public class ProceduralPointLight extends PointLight
{
  private Procedure procedure;
  private ThreadLocal renderingProc;
  private double currentTime;
  private TextureParameter parameters[];
  private double parameterValues[];

  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("Radius"), 0.0, 45.0, 1.0),
    new Property(Translate.text("lightType"), new String[] {Translate.text("normalLight"), Translate.text("shadowlessLight"), Translate.text("ambientLight")}, Translate.text("normalLight"))
  };

  public ProceduralPointLight(double theRadius)
  {
    super(new RGBColor(), 1.0f, theRadius);
    procedure = createProcedure();
    findParameters();
    initThreadLocal();
  }

  /**
   * Create a Procedure object for this light.
   */

  private Procedure createProcedure()
  {
    return new Procedure(new OutputModule [] {
        new OutputModule("Color", "White", 0.0, new RGBColor(1.0, 1.0, 1.0), IOPort.COLOR),
        new OutputModule("Intensity", "1/r\u00B2", 1.0, null, IOPort.NUMBER)});
  }

  /**
   * Reinitialize the ThreadLocal that holds copies of the Procedure during rendering.
   */

  private void initThreadLocal()
  {
    renderingProc = new ThreadLocal() {
      protected Object initialValue()
      {
        Procedure localProc = createProcedure();
        localProc.copy(procedure);
        return localProc;
      }
    };
  }

  /**
   * Find all parameters defined by the procedure.
   */

  private void findParameters()
  {
    Module module[] = procedure.getModules();
    int count = 0;
    for (int i = 0; i < module.length; i++)
      if (module[i] instanceof ParameterModule)
        count++;
    TextureParameter newParameters[] = new TextureParameter[count];
    double newValues[] = new double[count];
    count = 0;
    for (int i = 0; i < module.length; i++)
      if (module[i] instanceof ParameterModule)
        {
          newParameters[count] = ((ParameterModule) module[i]).getParameter(this);
          newValues[count] = newParameters[count].defaultVal;
          if (parameters != null)
          {
            for (int j = 0; j < parameters.length; j++)
              if (newParameters[count].equals(parameters[j]))
                newValues[count] = parameterValues[j];
          }
          ((ParameterModule) module[i]).setIndex(count++);
        }
    parameters = newParameters;
    parameterValues = newValues;
  }


  public Object3D duplicate()
  {
    ProceduralPointLight light = new ProceduralPointLight(getRadius());
    light.copyObject(this);
    return light;
  }

  public void copyObject(Object3D obj)
  {
    ProceduralPointLight lt = (ProceduralPointLight) obj;
    setRadius(lt.getRadius());
    procedure.copy(lt.procedure);
  }

  public void sceneChanged(ObjectInfo info, Scene scene)
  {
    currentTime = scene.getTime();
  }

  /**
   * Evaluate the Procedure to determine the light color at a point.
   */

  public void getLight(RGBColor light, Vec3 position)
  {
    PointInfo point = new PointInfo();
    point.x = position.x;
    point.y = position.y;
    point.z = position.z;
    point.t = currentTime;
    point.param = parameterValues;
    Procedure pr = (Procedure) renderingProc.get();
    pr.initForPoint(point);
    OutputModule output[] = pr.getOutputModules();
    output[0].getColor(0, light, 0.0);
    double intensity;
    if (output[1].inputConnected(0))
      intensity = output[1].getAverageValue(0, 0.0);
    else
      intensity = 1.0/position.length2();
    light.scale(intensity);
  }

  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public ProceduralPointLight(DataInputStream in, Scene theScene) throws IOException
  {
    super(in, theScene);
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    procedure = createProcedure();
    procedure.readFromStream(in, theScene);
    bounds = new BoundingBox(-0.15, 0.15, -0.15, 0.15, -0.15, 0.25);
    findParameters();
    initThreadLocal();
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);
    out.writeShort(0);
    procedure.writeToStream(out, theScene);
  }

  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    ProcedureEditor editor = new ProcedureEditor(procedure, new LightProcedureOwner(info, cb), parent.getScene());
    editor.setEditingWindow(parent);
  }

  public Property[] getProperties()
  {
    Property properties[] = new Property[parameters.length+2];
    for (int i = 0; i < parameters.length; i++)
      properties[i] = new Property(parameters[i].name, parameters[i].minVal, parameters[i].maxVal, parameters[i].defaultVal);
    properties[properties.length-2] = PROPERTIES[0];
    properties[properties.length-1] = PROPERTIES[1];
    return properties;
  }

  public Object getPropertyValue(int index)
  {
    if (index < parameterValues.length)
      return parameterValues[index];
    switch (index-parameterValues.length)
    {
      case 0:
        return new Double(getRadius());
      case 1:
        return PROPERTIES[1].getAllowedValues()[type];
    }
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
    if (index < parameterValues.length)
      parameterValues[index] = (Double) value;
    else if (index == parameterValues.length)
      setRadius((Double) value);
    else if (index == parameterValues.length+1)
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
    return new ProceduralLightKeyframe(this);
  }

  /* Modify this object based on a pose keyframe. */

  public void applyPoseKeyframe(Keyframe k)
  {
    ProceduralLightKeyframe key = (ProceduralLightKeyframe) k;
    setRadius(key.radius);
    for (int i = 0; i < parameters.length; i++)
    {
      if (key.paramValues.containsKey(parameters[i]))
        parameterValues[i] = key.paramValues.get(parameters[i]);
      else
        parameterValues[i] = parameters[i].defaultVal;
    }
  }

  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */

  public void configurePoseTrack(PoseTrack track)
  {
    String names[] = new String[parameters.length+1];
    double defaults[] = new double[parameters.length+1];
    double ranges[][] = new double[parameters.length+1][];
    for (int i = 0; i < parameters.length; i++)
    {
      TextureParameter param = parameters[i];
      names[i] = param.name;
      defaults[i] = param.defaultVal;
      ranges[i] = new double[] {param.minVal, param.maxVal};
    }
    names[parameters.length] = Translate.text("Radius");
    defaults[parameters.length] = getRadius();
    ranges[parameters.length] = new double[] {0.0, 45.0};
    track.setGraphableValues(names, defaults, ranges);
  }

  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */

  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    final ProceduralLightKeyframe key = (ProceduralLightKeyframe) k;
    ValueSelector fields[] = new ValueSelector[parameters.length+1];
    String names[] = new String[parameters.length+1];
    for (int i = 0; i < parameters.length; i++) {
      TextureParameter param = parameters[i];
      double value = key.paramValues.containsKey(param) ? key.paramValues.get(param) : param.defaultVal;
      double range = param.maxVal-param.minVal;
      if (range == 0.0 || Double.isInfinite(range))
        range = 1.0;
      fields[i] = new ValueSelector(value, param.minVal, param.maxVal, range*0.01);
      names[i] = param.name;
    }
    fields[fields.length-1] = new ValueSelector(key.radius, 0.0, 45.0, 0.1);;
    names[names.length-1] = Translate.text("Radius");
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editPointLightTitle"), fields, names);
    if (!dlg.clickedOk())
      return;
    for (int i = 0; i < parameters.length; i++)
      key.paramValues.put(parameters[i], fields[i].getValue());
    key.radius = fields[fields.length-1].getValue();
  }

  /** Inner class representing a pose for a point light. */

  public static class ProceduralLightKeyframe implements Keyframe
  {
    private final ProceduralPointLight light;
    public HashMap<TextureParameter, Double> paramValues;
    public double radius;

    public ProceduralLightKeyframe(ProceduralPointLight light)
    {
      this.light = light;
      paramValues = new HashMap<TextureParameter, Double>();
      for (int i = 0; i < light.parameters.length; i++)
        paramValues.put(light.parameters[i], light.parameterValues[i]);
      radius = light.getRadius();
    }

    /* Create a duplicate of this keyframe.. */

    public Keyframe duplicate()
    {
      return duplicate(light);
    }

    /* Create a duplicate of this keyframe for a (possibly different) object. */

    public Keyframe duplicate(Object owner)
    {
      ProceduralLightKeyframe key = new ProceduralLightKeyframe((ProceduralPointLight) ((ObjectInfo) owner).getObject());
      key.paramValues.clear();
      for (Map.Entry<TextureParameter, Double> entry : paramValues.entrySet())
        key.paramValues.put(entry.getKey(), entry.getValue());
      key.radius = radius;
      return key;
    }

    /* Get the list of graphable values for this keyframe. */

    public double [] getGraphValues()
    {
      double values[] = new double[light.parameters.length+1];
      for (int i = 0; i < light.parameters.length; i++) {
        TextureParameter param = light.parameters[i];
        values[i] = (paramValues.containsKey(param) ? paramValues.get(param) : param.defaultVal);
      }
      values[values.length-1] = radius;
      return values;
    }

    /* Set the list of graphable values for this keyframe. */

    public void setGraphValues(double values[])
    {
      paramValues.clear();
      for (int i = 0; i < light.parameters.length; i++)
        paramValues.put(light.parameters[i], values[i]);
      radius = values[values.length-1];
    }

    /* These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others. */

    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      ProceduralLightKeyframe k2 = (ProceduralLightKeyframe) o2;
      ProceduralLightKeyframe key = new ProceduralLightKeyframe(light);
      key.radius = weight1*radius+weight2*k2.radius;
      for (TextureParameter param : light.parameters)
      {
        double val1 = paramValues.containsKey(param) ? paramValues.get(param) : param.defaultVal;
        double val2 = k2.paramValues.containsKey(param) ? k2.paramValues.get(param) : param.defaultVal;
        key.paramValues.put(param, weight1*val1+weight2*val2);
      }
      return key;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      ProceduralLightKeyframe k2 = (ProceduralLightKeyframe) o2, k3 = (ProceduralLightKeyframe) o3;
      ProceduralLightKeyframe key = new ProceduralLightKeyframe(light);
      key.radius = weight1*radius+weight2*k2.radius+weight3*k3.radius;
      for (TextureParameter param : light.parameters)
      {
        double val1 = paramValues.containsKey(param) ? paramValues.get(param) : param.defaultVal;
        double val2 = k2.paramValues.containsKey(param) ? k2.paramValues.get(param) : param.defaultVal;
        double val3 = k3.paramValues.containsKey(param) ? k3.paramValues.get(param) : param.defaultVal;
        key.paramValues.put(param, weight1*val1+weight2*val2+weight3*val3);
      }
      return key;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      ProceduralLightKeyframe k2 = (ProceduralLightKeyframe) o2, k3 = (ProceduralLightKeyframe) o3, k4 = (ProceduralLightKeyframe) o4;
      ProceduralLightKeyframe key = new ProceduralLightKeyframe(light);
      key.radius = weight1*radius+weight2*k2.radius+weight3*k3.radius+weight4*k4.radius;
      for (TextureParameter param : light.parameters)
      {
        double val1 = paramValues.containsKey(param) ? paramValues.get(param) : param.defaultVal;
        double val2 = k2.paramValues.containsKey(param) ? k2.paramValues.get(param) : param.defaultVal;
        double val3 = k3.paramValues.containsKey(param) ? k3.paramValues.get(param) : param.defaultVal;
        double val4 = k4.paramValues.containsKey(param) ? k4.paramValues.get(param) : param.defaultVal;
        key.paramValues.put(param, weight1*val1+weight2*val2+weight3*val3+weight4*val4);
      }
      return key;
    }

    /* Determine whether this keyframe is identical to another one. */

    public boolean equals(Keyframe k)
    {
      if (!(k instanceof ProceduralLightKeyframe))
        return false;
      ProceduralLightKeyframe key = (ProceduralLightKeyframe) k;
      if (key.radius != radius)
        return false;
      for (TextureParameter param : light.parameters)
      {
        double val1 = paramValues.containsKey(param) ? paramValues.get(param) : param.defaultVal;
        double val2 = key.paramValues.containsKey(param) ? key.paramValues.get(param) : param.defaultVal;
        if (val1 != val2)
        return false;
      }
      return true;
    }

    /* Write out a representation of this keyframe to a stream. */

    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeDouble(radius);
      for (TextureParameter param : light.parameters)
      {
        double val = paramValues.containsKey(param) ? paramValues.get(param) : param.defaultVal;
        out.writeDouble(val);
      }
    }

    /* Reconstructs the keyframe from its serialized representation. */

    public ProceduralLightKeyframe(DataInputStream in, Object parent) throws IOException
    {
      this((ProceduralPointLight) ((ObjectInfo) parent).getObject());
      radius = in.readDouble();
      for (TextureParameter param : light.parameters)
        paramValues.put(param, in.readDouble());
    }
  }

  private class LightProcedureOwner implements ProcedureOwner
  {
    private ObjectInfo info;
    private Runnable callback;

    public LightProcedureOwner(ObjectInfo info, Runnable callback)
    {
      this.info = info;
      this.callback = callback;
    }

    public String getWindowTitle()
    {
      return Translate.text("editProceduralPointLightTitle");
    }

    public Object getPreview(ProcedureEditor editor)
    {
      BDialog dlg = new BDialog(editor.getParentFrame(), "Preview", false);
      BorderContainer content = new BorderContainer();
      final MaterialPreviewer preview = new MaterialPreviewer(new UniformTexture(), null, 200, 160);
      Scene scene = preview.getScene();
      for (int i = 0; i < scene.getNumObjects(); i++)
      {
        ObjectInfo info = scene.getObject(i);
        if (info.getObject() instanceof DirectionalLight)
        {
          info.setObject(ProceduralPointLight.this);
          info.getCoords().setOrigin(new Vec3(1.0, 0.8, 2.0));
        }
      }
      content.add(preview, BorderContainer.CENTER);
      RowContainer row = new RowContainer();
      content.add(row, BorderContainer.SOUTH, new LayoutInfo());
      row.add(Translate.label("Time", ":"));
      final ValueSelector value = new ValueSelector(0.0, -Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
      final ActionProcessor processor = new ActionProcessor();
      row.add(value);
      value.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
          processor.addEvent(new Runnable()
          {
            public void run()
            {
              preview.getScene().setTime(value.getValue());
              preview.render();
            }
          });
        }
      });
      dlg.setContent(content);
      dlg.pack();
      Rectangle parentBounds = editor.getParentFrame().getBounds();
      Rectangle location = dlg.getBounds();
      location.y = parentBounds.y;
      location.x = parentBounds.x+parentBounds.width;
      dlg.setBounds(location);
      dlg.setVisible(true);
      return preview;
    }

    public void updatePreview(Object preview)
    {
      findParameters();
      initThreadLocal();
      ((MaterialPreviewer) preview).render();
    }

    public void disposePreview(Object preview)
    {
      UIUtilities.findWindow((MaterialPreviewer) preview).dispose();
    }

    public boolean allowParameters()
    {
      return true;
    }

    public boolean allowViewAngle()
    {
      return false;
    }

    public boolean canEditName()
    {
      return false;
    }

    public String getName()
    {
      return info.getName();
    }

    public void setName(String name)
    {
    }

    public void acceptEdits(ProcedureEditor editor)
    {
      findParameters();
      initThreadLocal();
      callback.run();
    }

    public void editProperties(ProcedureEditor editor)
    {
      ValueSelector radiusField = new ValueSelector(getRadius(), 0.0, 45.0, 0.1);
      BComboBox typeChoice = new BComboBox(new String[] {Translate.text("normalLight"), Translate.text("shadowlessLight"), Translate.text("ambientLight")});
      typeChoice.setSelectedIndex(type);
      final BFrame parentFrame = editor.getParentFrame();

      ComponentsDialog dlg = new ComponentsDialog(parentFrame, Translate.text("Properties"),
          new Widget [] {radiusField, typeChoice}, new String [] {Translate.text("Radius"), Translate.text("lightType")});
      if (!dlg.clickedOk())
        return;
      setParameters(getColor(), getIntensity(), typeChoice.getSelectedIndex(), getDecayRate());
      setRadius(radiusField.getValue());
    }
  }
}
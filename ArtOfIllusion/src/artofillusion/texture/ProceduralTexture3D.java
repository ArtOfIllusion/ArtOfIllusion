/* Copyright (C) 2000-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.procedural.*;
import artofillusion.ui.*;
import buoy.widget.*;
import buoy.event.*;

import java.awt.*;
import java.io.*;

/** This is a Texture3D which uses a Procedure to calculate its properties. */

public class ProceduralTexture3D extends Texture3D implements ProcedureOwner
{  
  private Procedure proc;
  private double antialiasing;
  private ThreadLocal renderingProc;

  public ProceduralTexture3D()
  {
    proc = createProcedure();
    antialiasing = 1.0;
    initThreadLocal();
  }

  /**
   * Create a Procedure object for this texture.
   */

  private Procedure createProcedure()
  {
    return new Procedure(new OutputModule [] {
      new OutputModule(Translate.text("Diffuse"), Translate.text("white"), 0.0, new RGBColor(1.0f, 1.0f, 1.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Specular"), Translate.text("white"), 0.0, new RGBColor(1.0f, 1.0f, 1.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Transparent"), Translate.text("white"), 0.0, new RGBColor(1.0f, 1.0f, 1.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Emissive"), Translate.text("black"), 0.0, new RGBColor(0.0f, 0.0f, 0.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Transparency"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Specularity"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Shininess"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Roughness"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Cloudiness"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("BumpHeight"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Displacement"), "0", 0.0, null, IOPort.NUMBER)
    });
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
        localProc.copy(proc);
        return localProc;
      }
    };
  }

  public static String getTypeName()
  {
    return "Procedural 3D";
  }

  public void getAverageSpec(TextureSpec spec, double time, double param[])
  {
    getTextureSpec(spec, 0.0, 0.0, 0.0, 1e3, 1e3, 1e3, 1.0, time, param);
  }

  public void getTextureSpec(TextureSpec spec, double x, double y, double z, double xsize, double ysize, double zsize, double angle, double t, double param[])
  {
    Procedure pr = (Procedure) renderingProc.get();
    OutputModule output[] = pr.getOutputModules();
    PointInfo info = new PointInfo();
    info.x = x;
    info.y = y;
    info.z = z;
    info.xsize = xsize*antialiasing;
    info.ysize = ysize*antialiasing;
    info.zsize = zsize*antialiasing;
    info.viewangle = angle;
    info.t = t;
    info.param = param;
    pr.initForPoint(info);
    double transparency = output[4].getAverageValue(0, 0.0);
    double specularity = output[5].getAverageValue(0, 0.0);
    double shininess = output[6].getAverageValue(0, 0.0);
    if (transparency < 0.0)
      transparency = 0.0;
    if (transparency > 1.0)
      transparency = 1.0;
    if (specularity < 0.0)
      specularity = 0.0;
    if (specularity > 1.0)
      specularity = 1.0;
    if (shininess < 0.0)
      shininess = 0.0;
    if (shininess > 1.0)
      shininess = 1.0;
    output[0].getColor(0, spec.diffuse, 0.0);
    output[1].getColor(0, spec.specular, 0.0);
    output[2].getColor(0, spec.transparent, 0.0);
    output[3].getColor(0, spec.emissive, 0.0);
    spec.hilight.copy(spec.specular);
    spec.diffuse.scale((1.0f-transparency)*(1.0f-specularity));
    spec.specular.scale((1.0f-transparency)*specularity);
    spec.hilight.scale((1.0f-transparency)*shininess);
    spec.transparent.scale(transparency);
    spec.roughness = output[7].getAverageValue(0, 0.0);
    spec.cloudiness = output[8].getAverageValue(0, 0.0);
    if (spec.roughness < 0.0)
      spec.roughness = 0.0;
    if (spec.roughness > 1.0)
      spec.roughness = 1.0;
    if (spec.cloudiness < 0.0)
      spec.cloudiness = 0.0;
    if (spec.cloudiness > 1.0)
      spec.cloudiness = 1.0;
    output[9].getValueGradient(0, spec.bumpGrad, 0.0);
    spec.bumpGrad.scale(0.04);
  }
  
  public void getTransparency(RGBColor trans, double x, double y, double z, double xsize, double ysize, double zsize, double angle, double t, double param[])
  {
    Procedure pr = (Procedure) renderingProc.get();
    OutputModule output[] = pr.getOutputModules();
    PointInfo info = new PointInfo();
    info.x = x;
    info.y = y;
    info.z = z;
    info.xsize = xsize*antialiasing;
    info.ysize = ysize*antialiasing;
    info.zsize = zsize*antialiasing;
    info.viewangle = angle;
    info.t = t;
    info.param = param;
    pr.initForPoint(info);
    double transparency = output[4].getAverageValue(0, 0.0);
    if (transparency < 0.0)
      transparency = 0.0;
    if (transparency > 1.0)
      transparency = 1.0;
    output[2].getColor(0, trans, 0.0);
    trans.scale(transparency);
  }

  /** Get the procedure used by this texture. */
  
  public Procedure getProcedure()
  {
    return proc;
  }

  /** Determine whether this Texture uses the specified image. */

  public boolean usesImage(ImageMap image)
  {
    Module modules[] = proc.getModules();

    for (int i = 0; i < modules.length; i++)
      if (modules[i] instanceof ImageModule && ((ImageModule) modules[i]).getMap() == image)
        return true;
    return false;
  }

  public double getDisplacement(double x, double y, double z, double xsize, double ysize, double zsize, double t, double param[])
  {
    Procedure pr = (Procedure) renderingProc.get();
    OutputModule output[] = pr.getOutputModules();
    PointInfo info = new PointInfo();
    info.x = x;
    info.y = y;
    info.z = z;
    info.xsize = xsize*antialiasing;
    info.ysize = ysize*antialiasing;
    info.zsize = zsize*antialiasing;
    info.viewangle = 1.0;
    info.t = t;
    info.param = param;
    pr.initForPoint(info);
    return output[10].getAverageValue(0, 0.0);
  }
  
  /** Get the list of parameters for this texture. */
  
  public TextureParameter[] getParameters()
  {
    Module module[] = proc.getModules();
    int count = 0;
    
    for (int i = 0; i < module.length; i++)
      if (module[i] instanceof ParameterModule)
        count++;
    TextureParameter params[] = new TextureParameter [count];
    count = 0;
    for (int i = 0; i < module.length; i++)
      if (module[i] instanceof ParameterModule)
        {
          params[count] = ((ParameterModule) module[i]).getParameter(this);
          ((ParameterModule) module[i]).setIndex(count++);
        }
    return params;
  }

  public Texture duplicate()
  {
    ProceduralTexture3D tex = new ProceduralTexture3D();
    
    tex.proc.copy(proc);
    tex.setName(getName());
    tex.antialiasing = antialiasing;
    return tex;
  }
  
  /** Determine whether this texture has a non-zero value anywhere for a particular component.
      @param component    the texture component to check for (one of the *_COMPONENT constants)
  */
  
  public boolean hasComponent(int component)
  {
    OutputModule output[] = proc.getOutputModules();
    switch (component)
      {
        case DIFFUSE_COLOR_COMPONENT:
          return true;
        case SPECULAR_COLOR_COMPONENT:
          return output[5].inputConnected(0);
        case TRANSPARENT_COLOR_COMPONENT:
          return output[4].inputConnected(0);
        case HILIGHT_COLOR_COMPONENT:
          return output[6].inputConnected(0);
        case EMISSIVE_COLOR_COMPONENT:
          return output[3].inputConnected(0);
        case BUMP_COMPONENT:
          return output[9].inputConnected(0);
        case DISPLACEMENT_COMPONENT:
          return output[10].inputConnected(0);
      }
    return false;
  }

  public void edit(BFrame fr, Scene sc)
  {
    new ProcedureEditor(proc, this, sc);
  }

  public ProceduralTexture3D(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    setName(in.readUTF());
    antialiasing = in.readDouble();
    proc = createProcedure();
    proc.readFromStream(in, theScene);
    if (version == 0)
    {
      // Reassign the inputs to the output modules, since Shininess was add
      // in version 1.
      
      OutputModule output[] = proc.getOutputModules();
      Module input[] = new Module [output.length];
      int index[] = new int [output.length];
      for (int i = 0; i < output.length; i++)
        {
          int j = (i < 6 ? i : i-1);
          input[i] = output[j].linkFrom[0];
          index[i] = output[j].linkFromIndex[0];
        }
      Link link[] = proc.getLinks();
      for (int i = 0; i < link.length; i++)
        if (link[i].to.getModule() instanceof OutputModule)
          {
            proc.deleteLink(i--);
            link = proc.getLinks();
          }
      for (int i = 0; i < input.length; i++)
        if (input[i] != null)
          {
            IOPort to  = output[i].getInputPorts()[0];
            IOPort from = input[i].getOutputPorts()[index[i]];
            proc.addLink(new Link(from, to));
          }
    }
    initThreadLocal();
  }
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(1);
    out.writeUTF(getName());
    out.writeDouble(antialiasing);
    proc.writeToStream(out, theScene);
  }

  /** Get the title of the procedure's editing window. */
  
  public String getWindowTitle()
  {
    return "Procedural 3D Texture";
  }
  
  /** Create an object which displays a preview of the procedure. */
  
  public Object getPreview(ProcedureEditor editor)
  {
    final BDialog dlg = new BDialog(editor.getParentFrame(), "Preview", false);
    BorderContainer content = new BorderContainer();
    final MaterialPreviewer preview = new MaterialPreviewer(this, null, 200, 160);
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
  
  /** Update the display of the preview. */
  
  public void updatePreview(Object preview)
  {
    initThreadLocal();
    ((MaterialPreviewer) preview).render();
  }
  
  /** Dispose of the preview object when the editor is closed. */
  
  public void disposePreview(Object preview)
  {
    UIUtilities.findWindow((MaterialPreviewer) preview).dispose();
  }
  
  /** Determine whether the procedure may contain View Angle modules. */
  
  public boolean allowViewAngle()
  {
    return true;
  }
  
  /** Determine whether the procedure may contain Parameter modules. */
  
  public boolean allowParameters()
  {
    return true;
  }
  
  /** Determine whether the procedure may be renamed. */
  
  public boolean canEditName()
  {
    return true;
  }
  
  /** This is called when the user clicks OK in the procedure editor. */
  
  public void acceptEdits(ProcedureEditor editor)
  {
    initThreadLocal();
    int i = editor.getScene().indexOf(this);
    if (i > -1)
      editor.getScene().changeTexture(i);
  }
  
  /** Display the Properties dialog. */
  
  public void editProperties(ProcedureEditor editor)
  {
    ValueField aliasField = new ValueField(antialiasing, ValueField.POSITIVE);
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("editTextureTitle"), 
      new Widget [] {aliasField},
      new String [] {Translate.text("Antialiasing")});
    if (!dlg.clickedOk())
      return;
    editor.saveState(false);
    antialiasing = aliasField.getValue();
    editor.updatePreview();
  }
}

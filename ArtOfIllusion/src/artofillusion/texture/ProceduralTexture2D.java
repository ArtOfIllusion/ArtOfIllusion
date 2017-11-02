/* Copyright (C) 2000-2008 by Peter Eastman
   Changes copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.procedural.ProceduralTextureCommon;
import artofillusion.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.procedural.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;

/** This is a Texture2D which uses a Procedure to calculate its properties. */

public class ProceduralTexture2D extends Texture2D implements ProcedureOwner
{
  private Procedure proc;
  private double antialiasing;
  private ThreadLocal<Procedure> renderingProc;

  public ProceduralTexture2D()
  {
    proc = ProceduralTextureCommon.createTextureProcedure();
    antialiasing = 1.0;
    initThreadLocal();
  }

  /**
   * Reinitialize the ThreadLocal that holds copies of the Procedure during rendering.
   */

  private void initThreadLocal()
  {
    renderingProc = new ThreadLocal<Procedure>() {
      @Override
      protected Procedure initialValue()
      {
        Procedure localProc = ProceduralTextureCommon.createTextureProcedure();
        localProc.copy(proc);
        return localProc;
      }
    };
  }

  public static String getTypeName()
  {
    return "Procedural 2D";
  }

  @Override
  public void getAverageSpec(TextureSpec spec, double time, double param[])
  {
    getTextureSpec(spec, 0.0, 0.0, 1e3, 1e3, 1.0, time, param);
  }

  @Override
  public void getTextureSpec(TextureSpec spec, double x, double y, double xsize, double ysize, double angle, double t, double param[])
  {
    Procedure pr = (Procedure) renderingProc.get();
    OutputModule output[] = pr.getOutputModules();
    PointInfo info = new PointInfo();
    info.x = x;
    info.y = y;
    info.z = 0.0;
    info.xsize = xsize*antialiasing;
    info.ysize = ysize*antialiasing;
    info.zsize = 0.0;
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

  @Override
  public void getTransparency(RGBColor trans, double x, double y, double xsize, double ysize, double angle, double t, double param[])
  {
    Procedure pr = (Procedure) renderingProc.get();
    OutputModule output[] = pr.getOutputModules();
    PointInfo info = new PointInfo();
    info.x = x;
    info.y = y;
    info.z = 0.0;
    info.xsize = xsize*antialiasing;
    info.ysize = ysize*antialiasing;
    info.zsize = 0.0;
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

  @Override
  public boolean usesImage(ImageMap image)
  {
    return Procedure.procedureUsesImage(proc, image);
  }

  @Override
  public double getDisplacement(double x, double y, double xsize, double ysize, double t, double param[])
  {
    Procedure pr = (Procedure) renderingProc.get();
    OutputModule output[] = pr.getOutputModules();
    PointInfo info = new PointInfo();
    info.x = x;
    info.y = y;
    info.z = 0.0;
    info.xsize = xsize*antialiasing;
    info.ysize = ysize*antialiasing;
    info.zsize = 0.0;
    info.viewangle = 1.0;
    info.t = t;
    info.param = param;
    pr.initForPoint(info);
    return output[10].getAverageValue(0, 0.0);
  }

  @Override
  public Texture duplicate()
  {
    ProceduralTexture2D tex = new ProceduralTexture2D();

    tex.proc.copy(proc);
    tex.setName(getName());
    tex.antialiasing = antialiasing;
    return tex;
  }

  /** Get the list of parameters for this texture. */

  @Override
  public TextureParameter[] getParameters()
  {
    return Procedure.getTextureParameters(proc, this);
  }

  /** Determine whether this texture has a non-zero value anywhere for a particular component.
      @param component    the texture component to check for (one of the *_COMPONENT constants)
  */

  @Override
  public boolean hasComponent(int component)
  {
    return Procedure.hasTextureComponent(proc, component);
  }

  @Override
  public void edit(BFrame fr, Scene sc)
  {
    new ProcedureEditor(proc, this, sc);
  }

  public ProceduralTexture2D(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();

    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    name = in.readUTF();
    antialiasing = in.readDouble();
    proc = ProceduralTextureCommon.createTextureProcedure();
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

  @Override
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(1);
    out.writeUTF(getName());
    out.writeDouble(antialiasing);
    proc.writeToStream(out, theScene);
  }

  /** Get the title of the procedure's editing window. */

  @Override
  public String getWindowTitle()
  {
    return "Procedural 2D Texture";
  }

  /** Create an object which displays a preview of the procedure. */

  @Override
  public Object getPreview(ProcedureEditor editor)
  {
    return ProceduralTextureCommon.getPreview(editor, this);
  }

  /** Update the display of the preview. */

  @Override
  public void updatePreview(Object preview)
  {
    initThreadLocal();
    ((MaterialPreviewer) preview).render();
  }

  /** Dispose of the preview object when the editor is closed. */

  @Override
  public void disposePreview(Object preview)
  {
    UIUtilities.findWindow((MaterialPreviewer) preview).dispose();
  }

    /** Determine whether the procedure may contain Parameter modules. */

  @Override
  public boolean allowParameters()
  {
    return true;
  }
  
  /** Determine whether the procedure may contain View Angle modules. */

  @Override
  public boolean allowViewAngle()
  {
    return true;
  }
  
  /** Determine whether the procedure may be renamed. */

  @Override
  public boolean canEditName()
  {
    return true;
  }

  /** This is called when the user clicks OK in the procedure editor. */

  @Override
  public void acceptEdits(ProcedureEditor editor)
  {
    initThreadLocal();
    int i = editor.getScene().indexOf(this);
    if (i > -1)
      editor.getScene().changeTexture(i);
  }

  /** Display the Properties dialog. */

  @Override
  public void editProperties(ProcedureEditor editor)
  {
    ValueField aliasField = new ValueField(antialiasing, ValueField.POSITIVE);
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("editTextureTitle"),
      new Widget [] {aliasField},
      new String [] {Translate.text("Antialiasing")});
    if (dlg.clickedOk())
    {
    editor.saveState(false);
    antialiasing = aliasField.getValue();

    }
  }
}

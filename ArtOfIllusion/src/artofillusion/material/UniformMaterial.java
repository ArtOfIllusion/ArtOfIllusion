/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** UniformMaterial represents a material whose properties do not vary with position. */

public class UniformMaterial extends Material
{
  RGBColor transparencyColor, matColor, scatteringColor;
  RGBColor trueTrans, trueScat;
  float density, scattering, transparency;
  double eccentricity;
  boolean shadows;

  public UniformMaterial()
  {
    // Generate the default color.

    transparencyColor = new RGBColor(1.0f, 1.0f, 1.0f);
    matColor = new RGBColor(0.0f, 0.0f, 0.0f);
    scatteringColor = new RGBColor(0.5f, 0.5f, 0.5f);
    trueTrans = new RGBColor(0.0f, 0.0f, 0.0f);
    trueScat = new RGBColor(0.0f, 0.0f, 0.0f);
    density = 1.0f;
    scattering = 0.0f;
    transparency = 0.5f;
    eccentricity = 0.0;
    name = "";
    refraction = 1.0;
    shadows = true;
    recalcColors();
  }

  /* Recalculate the true transparency and scattering colors. */

  private void recalcColors()
  {
    trueTrans.setRGB((float) Math.pow(transparencyColor.red*transparency, density),
        (float) Math.pow(transparencyColor.green*transparency, density),
        (float) Math.pow(transparencyColor.blue*transparency, density));
    trueScat.copy(scatteringColor);
    trueScat.scale(density*scattering);
  }

  public static String getTypeName()
  {
    return "Uniform";
  }

  public boolean isScattering()
  {
    return (density > 0.0 && scattering > 0.0);
  }
  
  public boolean castsShadows()
  {
    return shadows;
  }

  public void getMaterialSpec(MaterialSpec spec)
  {
    spec.transparency.copy(trueTrans);
    spec.color.copy(matColor);
    if (scattering > 0.0)
    {
      spec.scattering.copy(trueScat);
      spec.eccentricity = eccentricity;
    }
  }
  
  /* The only MaterialMapping which can be used for a UniformMaterial is a UniformMapping. */
  
  public MaterialMapping getDefaultMapping(Object3D obj)
  {
    return new UniformMaterialMapping(obj, this);
  }

  /* Create a duplicate of the material. */
  
  public Material duplicate()
  {
    UniformMaterial m = new UniformMaterial();
    
    m.name = name;
    m.transparencyColor.copy(transparencyColor);
    m.matColor.copy(matColor);
    m.scatteringColor.copy(scatteringColor);
    m.refraction = refraction;
    m.density = density;
    m.scattering = scattering;
    m.transparency = transparency;
    m.eccentricity = eccentricity;
    m.shadows = shadows;
    m.recalcColors();
    return m;
  }
  
  /* Allow the user to interactively edit the material. */
  
  public void edit(final BFrame fr, Scene sc)
  {
    final UniformMaterial newMaterial = (UniformMaterial) duplicate();
    BTextField nameField = new BTextField(name);
    final ValueField refractField = new ValueField(refraction, ValueField.POSITIVE);
    final ValueSlider densitySlider = new ValueSlider(0.0, 1.0, 100, (double) density);
    final ValueSlider scatSlider = new ValueSlider(0.0, 1.0, 100, (double) scattering);
    final ValueSlider transSlider = new ValueSlider(0.0, 1.0, 100, (double) transparency);
    final ValueSlider eccSlider = new ValueSlider(-1.0, 1.0, 100, eccentricity);
    final Widget transPatch = transparencyColor.getSample(50, 30);
    final Widget colorPatch = matColor.getSample(50, 30);
    final Widget scatPatch = scatteringColor.getSample(50, 30);
    final BCheckBox shadowBox = new BCheckBox(Translate.text("CastsShadows"), shadows);
    final MaterialPreviewer preview = new MaterialPreviewer(null, newMaterial, 200, 160);
    final ActionProcessor process = new ActionProcessor();
    final Runnable renderCallback = new Runnable() {
      public void run()
      {
        preview.render();
      }
    };
    transPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(fr, Translate.text("Transparency"), newMaterial.transparencyColor);
        transPatch.setBackground(newMaterial.transparencyColor.getColor());
        newMaterial.recalcColors();
        process.addEvent(renderCallback);
      }
    });
    colorPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(fr, Translate.text("MaterialColor"), newMaterial.matColor);
        colorPatch.setBackground(newMaterial.matColor.getColor());
        newMaterial.recalcColors();
        process.addEvent(renderCallback);
      }
    });
    scatPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(fr, Translate.text("ScatteringColor"), newMaterial.scatteringColor);
        scatPatch.setBackground(newMaterial.scatteringColor.getColor());
        newMaterial.recalcColors();
        process.addEvent(renderCallback);
      }
    });
    Object valueListener = new Object() {
      void processEvent()
      {
        newMaterial.density = (float) densitySlider.getValue();
        newMaterial.scattering = (float) scatSlider.getValue();
        newMaterial.transparency = (float) transSlider.getValue();
        newMaterial.eccentricity = (float) eccSlider.getValue();
        newMaterial.refraction = refractField.getValue();
        newMaterial.shadows = shadowBox.getState();
        newMaterial.recalcColors();
        process.addEvent(renderCallback);
      }
    };
    refractField.addEventLink(ValueChangedEvent.class, valueListener);
    densitySlider.addEventLink(ValueChangedEvent.class, valueListener);
    scatSlider.addEventLink(ValueChangedEvent.class, valueListener);
    transSlider.addEventLink(ValueChangedEvent.class, valueListener);
    eccSlider.addEventLink(ValueChangedEvent.class, valueListener);
    shadowBox.addEventLink(ValueChangedEvent.class, valueListener);
    ComponentsDialog dlg = new ComponentsDialog(fr, "", new Widget [] {
        preview, nameField, colorPatch, transPatch, transSlider, densitySlider, scatSlider, scatPatch,
        eccSlider, refractField, shadowBox}, new String [] {null, Translate.text("Name"),
        Translate.text("EmissiveColor"), Translate.text("TransparentColor"), Translate.text("Transparency"), Translate.text("Density"),
        Translate.text("Scattering"), Translate.text("ScatteringColor"), Translate.text("Eccentricity"),
        Translate.text("IndexOfRefraction"), null});
    process.stopProcessing();
    if (!dlg.clickedOk())
      return;
    refraction = refractField.getValue();
    transparency = (float) transSlider.getValue();
    density = (float) densitySlider.getValue();
    scattering = (float) scatSlider.getValue();
    UniformMaterial.this.name = nameField.getText();
    transparencyColor.copy(newMaterial.transparencyColor);
    matColor.copy(newMaterial.matColor);
    scatteringColor.copy(newMaterial.scatteringColor);
    eccentricity = newMaterial.eccentricity;
    shadows = newMaterial.shadows;
    recalcColors();
    int index = sc.indexOf(this);
    if (index > -1)
      sc.changeMaterial(index);
  }
  
  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */
  
  public UniformMaterial(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    name = in.readUTF();
    refraction = in.readDouble();
    transparencyColor = new RGBColor(in);
    matColor = new RGBColor(in);
    scatteringColor = new RGBColor(in);
    density = in.readFloat();
    scattering = in.readFloat();
    transparency = (version == 0 ? 1.0f : in.readFloat());
    eccentricity = in.readDouble();
    shadows = in.readBoolean();
    trueTrans = new RGBColor(0.0f, 0.0f, 0.0f);
    trueScat = new RGBColor(0.0f, 0.0f, 0.0f);
    recalcColors();
  }
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(1);
    out.writeUTF(name);
    out.writeDouble(refraction);
    transparencyColor.writeToFile(out);
    matColor.writeToFile(out);
    scatteringColor.writeToFile(out);
    out.writeFloat(density);
    out.writeFloat(scattering);
    out.writeFloat(transparency);
    out.writeDouble(eccentricity);
    out.writeBoolean(shadows);
  }
}
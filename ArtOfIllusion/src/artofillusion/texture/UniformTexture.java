/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** UniformMaterial represents a material whose properties do not vary with position. */

public class UniformTexture extends Texture
{
  public RGBColor diffuseColor, specularColor, transparentColor, emissiveColor;
  public double roughness, cloudiness;
  public float transparency, specularity, shininess;
  
  public UniformTexture()
  {
    // Generate the default color.

    diffuseColor = new RGBColor(1.0f, 1.0f, 1.0f);
    specularColor = new RGBColor(1.0f, 1.0f, 1.0f);
    transparentColor = new RGBColor(1.0f, 1.0f, 1.0f);
    emissiveColor = new RGBColor(0.0f, 0.0f, 0.0f);
    transparency = 0.0f;
    specularity = 0.0f;
    shininess = 0.0f;
    roughness = 0.2;
    cloudiness = 0.0;
    name = "";
  }
  
  /** Create a texture which is completely invisible. */

  public static UniformTexture invisibleTexture()
  {
    UniformTexture tex = new UniformTexture();

    tex.diffuseColor.setRGB(0.0f, 0.0f, 0.0f);
    tex.specularColor.setRGB(0.0f, 0.0f, 0.0f);
    tex.transparency = 1.0f;
    return tex;
  }

  public static String getTypeName()
  {
    return "Uniform";
  }

  public void getTextureSpec(TextureSpec spec)
  {
    float frac = (1.0f-transparency)*(1.0f-specularity);
    
    spec.diffuse.setRGB(diffuseColor.red*frac, diffuseColor.green*frac, diffuseColor.blue*frac);
    frac = (1.0f-transparency)*specularity;
    spec.specular.setRGB(specularColor.red*frac, specularColor.green*frac, specularColor.blue*frac);
    frac = (1.0f-transparency)*shininess;
    spec.hilight.setRGB(specularColor.red*frac, specularColor.green*frac, specularColor.blue*frac);
    spec.transparent.setRGB(transparentColor.red*transparency, transparentColor.green*transparency, transparentColor.blue*transparency);
    spec.emissive.copy(emissiveColor);
    spec.roughness = roughness;
    spec.cloudiness = cloudiness;
    spec.bumpGrad.set(0.0, 0.0, 0.0);
  }
  
  public void getTransparency(RGBColor trans)
  {
    trans.setRGB(transparentColor.red*transparency, transparentColor.green*transparency, transparentColor.blue*transparency);
  }
  
  public void getAverageSpec(TextureSpec spec, double time, double param[])
  {
    getTextureSpec(spec);
  }
  
  /** The only TextureMapping which can be used for a UniformTexture is a UniformMapping. */
  
  public TextureMapping getDefaultMapping(Object3D object)
  {
    return new UniformMapping(object, this);
  }

  /** Create a duplicate of the texture. */
  
  public Texture duplicate()
  {
    UniformTexture m = new UniformTexture();
    
    m.name = name;
    m.diffuseColor.copy(diffuseColor);
    m.specularColor.copy(specularColor);
    m.transparentColor.copy(transparentColor);
    m.emissiveColor.copy(emissiveColor);
    m.transparency = transparency;
    m.specularity = specularity;
    m.roughness = roughness;
    m.cloudiness = cloudiness;
    m.shininess = shininess;
    return m;
  }

  /** Determine whether this texture has a non-zero value anywhere for a particular component.
      @param component    the texture component to check for (one of the *_COMPONENT constants)
  */
  
  public boolean hasComponent(int component)
  {
    switch (component)
      {
        case DIFFUSE_COLOR_COMPONENT:
          return (transparency < 1.0f && specularity < 1.0f && diffuseColor.getMaxComponent() > 0.0);
        case SPECULAR_COLOR_COMPONENT:
          return (transparency < 1.0f && specularity > 0.0f && specularColor.getMaxComponent() > 0.0);
        case TRANSPARENT_COLOR_COMPONENT:
          return (transparency > 0.0f && transparentColor.getMaxComponent() > 0.0);
        case HILIGHT_COLOR_COMPONENT:
          return (transparency < 1.0f && shininess > 0.0f && specularColor.getMaxComponent() > 0.0);
        case EMISSIVE_COLOR_COMPONENT:
          return (emissiveColor.getMaxComponent() > 0.0);
        default:
          return false;
      }
  }
  
  /** Allow the user to interactively edit the material. */
  
  public void edit(final BFrame fr, Scene sc)
  {
    BTextField nameField = new BTextField(name, 15);
    final ValueSlider transSlider = new ValueSlider(0.0, 1.0, 100, (double) transparency);
    final ValueSlider specSlider = new ValueSlider(0.0, 1.0, 100, (double) specularity);
    final ValueSlider shinSlider = new ValueSlider(0.0, 1.0, 100, shininess);
    final ValueSlider roughSlider = new ValueSlider(0.0, 1.0, 100, roughness);
    final ValueSlider clearSlider = new ValueSlider(0.0, 1.0, 100, cloudiness);
    final Widget diffPatch = diffuseColor.getSample(50, 30);
    final Widget specPatch = specularColor.getSample(50, 30);
    final Widget transPatch = transparentColor.getSample(50, 30);
    final Widget emissPatch = emissiveColor.getSample(50, 30);
    final UniformTexture newTexture = (UniformTexture) duplicate();
    final MaterialPreviewer preview = new MaterialPreviewer(newTexture, null, 200, 160);
    final ActionProcessor process = new ActionProcessor();
    final Runnable renderCallback = new Runnable() {
      public void run()
      {
        preview.render();
      }
    };
    diffPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(fr, Translate.text("DiffuseColor"), newTexture.diffuseColor);
        diffPatch.setBackground(newTexture.diffuseColor.getColor());
        process.addEvent(renderCallback);
      }
    });
    specPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(fr, Translate.text("SpecularColor"), newTexture.specularColor);
        specPatch.setBackground(newTexture.specularColor.getColor());
        process.addEvent(renderCallback);
      }
    });
    transPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(fr, Translate.text("TransparentColor"), newTexture.transparentColor);
        transPatch.setBackground(newTexture.transparentColor.getColor());
        process.addEvent(renderCallback);
      }
    });
    emissPatch.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        new ColorChooser(fr, Translate.text("EmissiveColor"), newTexture.emissiveColor);
        emissPatch.setBackground(newTexture.emissiveColor.getColor());
        process.addEvent(renderCallback);
      }
    });
    Object valueListener = new Object() {
      void processEvent()
      {
        newTexture.transparency = (float) transSlider.getValue();
        newTexture.specularity = (float) specSlider.getValue();
        newTexture.shininess = (float) shinSlider.getValue();
        newTexture.roughness = roughSlider.getValue();
        newTexture.cloudiness = clearSlider.getValue();
        process.addEvent(renderCallback);
      }
    };
    transSlider.addEventLink(ValueChangedEvent.class, valueListener);
    specSlider.addEventLink(ValueChangedEvent.class, valueListener);
    shinSlider.addEventLink(ValueChangedEvent.class, valueListener);
    roughSlider.addEventLink(ValueChangedEvent.class, valueListener);
    clearSlider.addEventLink(ValueChangedEvent.class, valueListener);
    ComponentsDialog dlg = new ComponentsDialog(fr, "", new Widget [] {
        preview, nameField, diffPatch, specPatch, transPatch, emissPatch, transSlider, specSlider,
        shinSlider, roughSlider, clearSlider}, new String [] {null, Translate.text("Name"),
        Translate.text("DiffuseColor"), Translate.text("SpecularColor"), Translate.text("TransparentColor"),
        Translate.text("EmissiveColor"), Translate.text("Transparency"), Translate.text("Specularity"),
        Translate.text("Shininess"), Translate.text("Roughness"), Translate.text("Cloudiness")});
    process.stopProcessing();
    if (!dlg.clickedOk())
      return;
    transparency = (float) transSlider.getValue();
    specularity = (float) specSlider.getValue();
    shininess = (float) shinSlider.getValue();
    roughness = roughSlider.getValue();
    cloudiness = clearSlider.getValue();
    UniformTexture.this.name = nameField.getText();
    diffuseColor.copy(newTexture.diffuseColor);
    specularColor.copy(newTexture.specularColor);
    transparentColor.copy(newTexture.transparentColor);
    emissiveColor.copy(newTexture.emissiveColor);
    int index = sc.indexOf(this);
    if (index > -1)
      sc.changeTexture(index);
  }
  
  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */
  
  public UniformTexture(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    name = in.readUTF();
    diffuseColor = new RGBColor(in);
    specularColor = new RGBColor(in);
    transparentColor = new RGBColor(in);
    emissiveColor = new RGBColor(in);
    roughness = in.readDouble();
    cloudiness = in.readDouble();
    transparency = in.readFloat();
    specularity = in.readFloat();
    if (version > 0)
      shininess = in.readFloat();
    else
      shininess = specularity;
  }
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(1);
    out.writeUTF(name);
    diffuseColor.writeToFile(out);
    specularColor.writeToFile(out);
    transparentColor.writeToFile(out);
    emissiveColor.writeToFile(out);
    out.writeDouble(roughness);
    out.writeDouble(cloudiness);
    out.writeFloat(transparency);
    out.writeFloat(specularity);
    out.writeFloat(shininess);
  }
}
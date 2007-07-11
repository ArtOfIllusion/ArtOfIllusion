/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;

/** UniformMapping is the TextureMapping for UniformTextures. */

public class UniformMapping extends TextureMapping
{
  Object3D object;
  UniformTexture texture;
  
  public UniformMapping(Object3D theObject, Texture theTexture)
  {
    object = theObject;
    texture = (UniformTexture) theTexture;
  }

  public Texture getTexture()
  {
    return texture;
  }

  public Object3D getObject()
  {
    return object;
  }

  public RenderingTriangle mapTriangle(int v1, int v2, int v3, int n1, int n2, int n3, Vec3 vert[])
  {
    return new UniformTriangle(v1, v2, v3, n1, n2, n3);
  }
  
  public void getTextureSpec(Vec3 pos, TextureSpec spec, double angle, double size, double t, double param[])
  {
    if (!appliesToFace(angle > 0.0))
      {
        spec.diffuse.setRGB(0.0f, 0.0f, 0.0f);
        spec.specular.setRGB(0.0f, 0.0f, 0.0f);
        spec.transparent.setRGB(1.0f, 1.0f, 1.0f);
        spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
        spec.roughness = spec.cloudiness = 0.0;
        spec.bumpGrad.set(0.0, 0.0, 0.0);
        return;
      }
    texture.getTextureSpec(spec);
  }

  public void getTransparency(Vec3 pos, RGBColor trans, double angle, double size, double t, double param[])
  {
    if (!appliesToFace(angle > 0.0))
      {
        trans.setRGB(1.0f, 1.0f, 1.0f);
        return;
      }
    texture.getTransparency(trans);
  }

  public double getDisplacement(Vec3 pos, double size, double t, double param[])
  {
    return 0.0;
  }

  public static boolean legalMapping(Object3D obj, Texture tex)
  {
    return (tex instanceof UniformTexture);
  }

  public TextureMapping duplicate()
  {
    UniformMapping map = new UniformMapping(object, texture);
    map.setAppliesTo(appliesTo());
    return map;
  }
  
  public TextureMapping duplicate(Object3D obj, Texture tex)
  {
    UniformMapping map = new UniformMapping(obj, tex);
    map.setAppliesTo(appliesTo());
    return map;
  }
  
  public void copy(TextureMapping map)
  {
    setAppliesTo(map.appliesTo());
  }

  public Widget getEditingPanel(Object3D obj, final MaterialPreviewer preview)
  {
    RowContainer row = new RowContainer();
    final BComboBox c = new BComboBox(new String [] {
      Translate.text("frontAndBackFaces"),
      Translate.text("frontFacesOnly"),
      Translate.text("backFacesOnly")
    });
    row.add(new BLabel(Translate.text("applyTo")+":"));
    row.add(c);
    c.setSelectedIndex(appliesTo());
    c.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        setAppliesTo((short) c.getSelectedIndex());
        preview.setTexture(getTexture(), UniformMapping.this);
        preview.render();
      }
    });
    return row;
  }
  
  public UniformMapping(DataInputStream in, Object3D theObject, Texture theTexture) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    if (version == 1)
      setAppliesTo(in.readShort());
    object = theObject;
    texture = (UniformTexture) theTexture;
  }
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeShort(1);
    out.writeShort(appliesTo());
  }
}
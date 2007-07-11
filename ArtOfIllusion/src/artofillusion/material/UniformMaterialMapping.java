/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import buoy.widget.*;
import java.io.*;

/** UniformMaterialMapping is the MaterialMapping for UniformMaterials. */

public class UniformMaterialMapping extends MaterialMapping
{
  public UniformMaterialMapping(Object3D theObject, Material theMaterial)
  {
    super(theObject, theMaterial);
  }
  
  public double getStepSize()
  {
    return material.getStepSize();
  }

  public void getMaterialSpec(Vec3 pos, MaterialSpec spec, double size, double t)
  {
    ((UniformMaterial) material).getMaterialSpec(spec);
  }

  public static boolean legalMapping(Object3D obj, Material mat)
  {
    return (mat instanceof UniformMaterial);
  }

  public MaterialMapping duplicate()
  {
    return new UniformMaterialMapping(object, material);
  }
  
  public MaterialMapping duplicate(Object3D obj, Material mat)
  {
    return new UniformMaterialMapping(obj, mat);
  }
  
  public void copy(MaterialMapping map)
  {
    material = map.material;
  }

  public Widget getEditingPanel(Object3D obj, MaterialPreviewer preview)
  {
    return new CustomWidget();
  }
  
  public UniformMaterialMapping(DataInputStream in, Object3D theObject, Material theMaterial) throws IOException, InvalidObjectException
  {
    super(theObject, theMaterial);
    
    short version = in.readShort();
    
    if (version != 0)
      throw new InvalidObjectException("");
    material = theMaterial;
  }
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeShort(0);
  }
}
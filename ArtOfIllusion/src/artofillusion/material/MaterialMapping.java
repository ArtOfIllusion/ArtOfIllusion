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

/** A MaterialMapping describes the mapping of a Material's coordinates to the local coordinate
    system of an object.
   
    This is an abstract class.  Its subclasses describe various types of
    mappings which are appropriate for various types of objects and materials. */

public abstract class MaterialMapping
{
  Object3D object;
  Material material;
  
  protected MaterialMapping(Object3D obj, Material mat)
  {
    object = obj;
    material = mat;
  }
  
  /** Every subclass of MaterialMapping must define a constructor which takes a Material
      and an Object3D as its arguments:
      <p>
      public MappingSubclass(Object3D theObject, Material theMaterial)
      <p>
      In addition, every subclass must include a constructor with the signature
      <p>
      public MappingSubclass(DataInputStream in, Object3D theObject, Material theMaterial) throws IOException, InvalidObjectException
      <p>
      which reconstructs the mapping by reading its data from an input stream.  The following
      method writes the object's data to an output stream. */
  
  public abstract void writeToFile(DataOutputStream out) throws IOException;
     
  /** Get the name of this type of mapping.  Subclasses should override this method to return
      an appropriate name. */
  
  public static String getName()
  {
    return "";
  }

  /** Get the index of refraction for this mapping's Material. */  

  public double indexOfRefraction()
  {
    return material.indexOfRefraction();
  }
  
  /** Get the step size to use for integrating the material. */
  
  public abstract double getStepSize();
  
  /** Return true if this mapping's Material has internal scattering. */
  
  public boolean isScattering()
  {
    return material.isScattering();
  }
  
  /** Return true if this mapping's Material casts shadows. */
  
  public boolean castsShadows()
  {
    return material.castsShadows();
  }

  /** Get the Material which is being mapped. */
  
  public Material getMaterial()
  {
    return material;
  }

  /** Get the object to which the material is applied. */

  public Object3D getObject()
  {
    return object;
  }

  /** Given a point inside the object for which this mapping is being used, find the
      corresponding material properties.  The properties should be averaged over a region of 
      width size. */
  
  public abstract void getMaterialSpec(Vec3 pos, MaterialSpec spec, double size, double t);
  
  /** Create a new MaterialMapping which is identical to this one. */
  
  public abstract MaterialMapping duplicate();
  
  /** Create a new MaterialMapping which is identical to this one, but for a
      different object and Material. */
  
  public abstract MaterialMapping duplicate(Object3D obj, Material mat);
  
  /** Make this mapping identical to another one. */
  
  public abstract void copy(MaterialMapping map);
  
  /** This method should return a Widget in which the user can edit the mapping.  The
      parameters are the object whose mapping is being edited, and a MaterialPreviewer
      which should be rendered whenever one of the mapping's parameters changes. */

  public abstract Widget getEditingPanel(Object3D obj, MaterialPreviewer preview);
}
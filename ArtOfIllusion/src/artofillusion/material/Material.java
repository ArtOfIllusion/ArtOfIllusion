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
import artofillusion.image.*;
import buoy.widget.*;
import java.io.*;

/** A Material represents a description of the bulk physical properties of an object:
    internal color and transparency, index of refraction, etc.  This is distinct from the
    surface properties, which are described by a Texture object. */

public abstract class Material
{
  protected String name;
  protected double refraction = 1.0;
  protected int id = nextID++;
  
  private static int nextID;

  /** Get the name of this type of material.  Subclasses should override this method to return
     an appropriate name. */
  
  public static String getTypeName()
  {
    return "";
  }

  /** Get the name of the material. */

  public String getName()
  {
    return name;
  }

  /** Change the name of the material. */

  public void setName(String name)
  {
    this.name = name;
  }

  /** Get the index of refraction. */  

  public double indexOfRefraction()
  {
    return refraction;
  }
  
  /** Set the index of refraction. */
  
  public void setIndexOfRefraction(double n)
  {
    refraction = n;
  }
  
  /** Get the step size to be used for integrating this material. */
  
  public double getStepSize()
  {
    return 0.1;
  }
  
  /** Return true if this Material makes use of the specified ImageMap.  Materials which
     use ImageMaps should override this method. */
  
  public boolean usesImage(ImageMap image)
  {
    return false;
  }
  
  /** Return true if this material has internal scattering. */
  
  public abstract boolean isScattering();
  
  /** Return true if this material should cast shadows. */
  
  public abstract boolean castsShadows();

  /** Get an ID number which is unique (within this session) for this material. */
  
  public int getID()
  {
    return id;
  }
  
  /** Assign a new ID number to this material, to reflect the fact that it has changed. */
  
  public void assignNewID()
  {
    id = nextID++;
  }
  
  /** Set the ID number for this material.  (Use with extreme caution!) */
  
  public void setID(int newid)
  {
    id = newid;
  }

  /** Get a default MaterialMapping for the material. */
  
  public abstract MaterialMapping getDefaultMapping(Object3D obj);

  /** Create a duplicate of the material. */
  
  public abstract Material duplicate();
  
  /** Allow the user to interactively edit the material.  fr is a Frame which can be used as a
      parent for Dialogs, and sc is the Scene which this Material is part of. */
  
  public abstract void edit(BFrame fr, Scene sc);

  /** The following method writes the material's data to an output stream.  In addition to this
      method, every Material must include a constructor with the signature
  
      public Classname(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
     
      which reconstructs the material by reading its data from an input stream. */
  
  public abstract void writeToFile(DataOutputStream out, Scene theScene) throws IOException;
}
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
import artofillusion.image.*;
import buoy.widget.*;
import java.io.*;

/** A Texture represents a description of the surface properties of an object: color, 
    transparency, displacement, etc.  This is distinct from the interior bulk properties,
    which are described by a Material object. */

public abstract class Texture
{
  protected String name;
  protected int id = nextID++;

  private static int nextID;
  
  public static final int DIFFUSE_COLOR_COMPONENT = 0;
  public static final int SPECULAR_COLOR_COMPONENT = 1;
  public static final int TRANSPARENT_COLOR_COMPONENT = 2;
  public static final int HILIGHT_COLOR_COMPONENT = 3;
  public static final int EMISSIVE_COLOR_COMPONENT = 4;
  public static final int BUMP_COMPONENT = 5;
  public static final int DISPLACEMENT_COMPONENT = 6;

  /** Get the name of this type of texture.  Subclasses should override this method to return
     an appropriate name. */
  
  public static String getTypeName()
  {
    return "";
  }

  /** Get the name of the texture. */

  public String getName()
  {
    return name;
  }

  /** Change the name of the texture. */

  public void setName(String name)
  {
    this.name = name;
  }
  
  /** Determine whether this texture has a non-zero value anywhere for a particular component.
      @param component    the texture component to check for (one of the *_COMPONENT constants)
  */
  
  public abstract boolean hasComponent(int component);
  
  /** Get the list of parameters for this texture. */
  
  public TextureParameter[] getParameters()
  {
    return new TextureParameter [0];
  }
  
  /** Return true if this Texture makes use of the specified ImageMap.  Textures which
     use ImageMaps should override this method. */
  
  public boolean usesImage(ImageMap image)
  {
    return false;
  }

  /** Get an ID number which is unique (within this session) for this texture. */
  
  public int getID()
  {
    return id;
  }
  
  /** Assign a new ID number to this texture, to reflect the fact that it has changed. */
  
  public void assignNewID()
  {
    id = nextID++;
  }

  /** Set the ID number for this texture.  (Use with extreme caution!) */
  
  public void setID(int newid)
  {
    id = newid;
  }
  
  /** Get a TextureSpec which represents the average surface properties of this texture.  It
     will be used by Renderers and Translators which do not support the given texture type.
     This need not be an exact mathematical average, but should give a reasonable representation
     of the overall surface properties. */

  public abstract void getAverageSpec(TextureSpec spec, double time, double param[]);

  /** Get a default TextureMapping for the texture. */
  
  public abstract TextureMapping getDefaultMapping(Object3D object);

  /** Create a duplicate of the texture. */
  
  public abstract Texture duplicate();
  
  /** Allow the user to interactively edit the texture.  fr is a BFrame which can be used as a
      parent for Dialogs, and sc is the Scene which this Texture is part of. */
  
  public abstract void edit(BFrame fr, Scene sc);

  /** The following method writes the texture's data to an output stream.  In addition to this
      method, every Texture must include a constructor with the signature
  
      public Classname(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
     
      which reconstructs the texture by reading its data from an input stream. */
  
  public abstract void writeToFile(DataOutputStream out, Scene theScene) throws IOException;
}
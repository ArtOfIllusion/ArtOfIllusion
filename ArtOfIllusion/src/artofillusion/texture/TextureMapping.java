/* Copyright (C) 1999-2007 by Peter Eastman

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
import buoy.widget.*;
import java.io.*;

/** A TextureMapping describes the mapping of a Texture's texture coordinates to points on 
   the surface of an object.  It operates in two ways.  First, it generates RenderingTriangles
   whic contain the necessary information to be rendered.  Second, since some renderers can
   directly render certain objects without ever breaking them into triangles, it must be able
   to directly calculate the material properties for a point in space.
   <p>
   This is an abstract class.  Its subclasses describe various types of
   mappings which are appropriate for various types of objects and materials. */

public abstract class TextureMapping
{
  private boolean twoSided = true;
  private boolean applyToFront = true;
  
  public static final short FRONT_AND_BACK = 0;
  public static final short FRONT_ONLY = 1;
  public static final short BACK_ONLY = 2;
  
  /** Every subclass of TextureMapping must define a constructor which takes a Texture
      and an Object3D as its arguments:
      <p>
      public MappingSubclass(Object3D obj, Texture texture)
      <p>
      In addition, every subclass must include a method of the form
      <p>
      public static boolean legalMapping(Object3D obj, Texture texture)
      <p>
      which returns true if the mapping can be used with the specified object and Texture.  
      Finally, every subclass must include a constructor with the signature
      <p>
      public MappingSubclass(DataInputStream in, Object3D obj, Texture texture) throws IOException, InvalidObjectException
      <p>
      which reconstructs the mapping by reading its data from an input stream.  The following
      method writes the object's data to an output stream. */
  
  public abstract void writeToFile(DataOutputStream out) throws IOException;
     
  /** Get the Texture associated with this TextureMapping. */
  
  public abstract Texture getTexture();

  /** Get the object this mapping is applied to. */

  public abstract Object3D getObject();

  /** Get the name of this type of mapping.  Subclasses should override this method to return
      an appropriate name. */
  
  public static String getName()
  {
    return "";
  }
  
  /** Given the vertices to be mapped and their normal vectors, generate a RenderingTriangle.
      Most subclasses will override this method.  However, some mappings which require more 
      information than just the vertex coordinates may instead define new methods which 
      replace this one. */

  public RenderingTriangle mapTriangle(int v1, int v2, int v3, int n1, int n2, int n3, Vec3 vert[])
  {
    return null;
  }
  
  /** This method is called once the texture parameters for the vertices of a triangle
      are known.  If necessary, subclasses can override this to take appropriate action. */
  
  public void setParameters(RenderingTriangle tri, double p1[], double p2[], double p3[], RenderingMesh mesh)
  {
  }
  
  /** Given a point on the surface for which this mapping is being used, find the 
      corresponding surface properties.  The properties should be averaged over a region 
      around the point.
      @param pos      the point at which to evaluate the texture
      @param spec     the surface properties will be stored in this
      @param angle    the dot product of the view direction with the surface normal
      @param size     the width of the region over which to average the surface properties
      @param t        the time at which to evaluate the surface properties
      @param param    the texture parameter values at the point
  */
  
  public abstract void getTextureSpec(Vec3 pos, TextureSpec spec, double angle, double size, double t, double param[]);
  
  /** Same as above, except only return the transparent color.  This can save time in cases
      where only the transparency is required, for example, when tracing shadow rays. */

  public abstract void getTransparency(Vec3 pos, RGBColor trans, double angle, double size, double t, double param[]);
  
  /** Given a point on the surface, evaluate the displacement function. All parameters
      are the same as for getTextureSpec(). */

  public abstract double getDisplacement(Vec3 pos, double size, double t, double param[]);

  /** Create a new TextureMapping which is identical to this one. */
  
  public abstract TextureMapping duplicate();
  
  /** Create a new TextureMapping which is identical to this one, but for a
      different object and texture. */
  
  public abstract TextureMapping duplicate(Object3D obj, Texture tex);
  
  /** Make this mapping identical to another one. */
  
  public abstract void copy(TextureMapping map);
  
  /** Get the list of texture parameters associated with this mapping and its texture.
      Subclasses that define their own parameters should override this to add them to
      the list. */
  
  public TextureParameter [] getParameters()
  {
    return getTexture().getParameters();
  }
  
  /** Determine whether this texture is applied to front faces, back faces,
      or both. */
  
  public short appliesTo()
  {
    if (twoSided)
      return FRONT_AND_BACK;
    return (applyToFront ? FRONT_ONLY : BACK_ONLY);
  }

  /** Determine whether this texture is applied to front (or back) faces. */
  
  public final boolean appliesToFace(boolean front)
  {
    return (twoSided || front == applyToFront);
  }

  /** Set whether this texture should apply to front faces, back faces, or
      both. */
  
  public void setAppliesTo(short whichFaces)
  {
    if (whichFaces == FRONT_AND_BACK)
      twoSided = true;
    else
    {
      twoSided = false;
      applyToFront = (whichFaces == FRONT_ONLY);
    }
  }
  
  /** This method should return a Widget in which the user can edit the mapping.  The
      parameters are the object whose mapping is being edited, and a MaterialPreviewer
      which should be rendered whenever one of the mapping's parameters changes. */

  public abstract Widget getEditingPanel(Object3D obj, MaterialPreviewer preview);
}
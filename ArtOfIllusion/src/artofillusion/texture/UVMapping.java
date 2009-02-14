/* Copyright (C) 2002-2009 by Peter Eastman

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

/** UVMapping is a Mapping2D which allows the user to specify the texture
    coordinates of each vertex by hand. */

public class UVMapping extends Mapping2D
{
  int numTextureParams;
  TextureParameter uparam, vparam;

  public UVMapping(Object3D theObject, Texture theTexture)
  {
    super(theObject, theTexture);
  }

  public static String getName()
  {
    return "UV";
  }

  /**
   * Get the TextureParameter which stores the U texture coordinate.
   */

  public TextureParameter getUParameter()
  {
    if (uparam == null)
      getParameters();
    return uparam;
  }

  /**
   * Get the TextureParameter which stores the V texture coordinate.
   */

  public TextureParameter getVParameter()
  {
    if (vparam == null)
      getParameters();
    return vparam;
  }

  public static boolean legalMapping(Object3D obj, Texture tex)
  {
    while (obj instanceof ObjectWrapper)
      obj = ((ObjectWrapper) obj).getWrappedObject();
    return (tex instanceof Texture2D && obj instanceof Mesh);
  }

  /** Create a UV mapped triangle. */

  public RenderingTriangle mapTriangle(int v1, int v2, int v3, int n1, int n2, int n3, Vec3 vert[])
  {
    return new UVMappedTriangle(v1, v2, v3, n1, n2, n3);
  }

  /** This method is called once the texture parameters for the vertices of a triangle
      are known. */
  
  public void setParameters(RenderingTriangle tri, double p1[], double p2[], double p3[], RenderingMesh mesh)
  {
    UVMappedTriangle uv = (UVMappedTriangle) tri;
    uv.setTextureCoordinates((float) p1[numTextureParams], (float) p1[numTextureParams+1],
        (float) p2[numTextureParams], (float) p2[numTextureParams+1],
        (float) p3[numTextureParams], (float) p3[numTextureParams+1],
        mesh.vert[uv.v1], mesh.vert[uv.v2], mesh.vert[uv.v3]);
  }
  
  /** This method should not generally be called.  The mapping is undefined without knowing
      the texture coordinates for a particular triangle. */

  public void getTextureSpec(Vec3 pos, TextureSpec spec, double angle, double size, double time, double param[])
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
    texture.getTextureSpec(spec, pos.x, pos.y, size, size, angle, time, param);
    if (texture.hasComponent(Texture.BUMP_COMPONENT))
      {
        double s = spec.bumpGrad.x;
        double t = spec.bumpGrad.y;
        spec.bumpGrad.set(s, t, 0.0);
      }
  }

  /** This method should not generally be called.  The mapping is undefined without knowing
      the texture coordinates for a particular triangle. */

  public void getTransparency(Vec3 pos, RGBColor trans, double angle, double size, double time, double param[])
  {
    if (!appliesToFace(angle > 0.0))
      trans.setRGB(1.0f, 1.0f, 1.0f);
    else
      texture.getTransparency(trans, pos.x, pos.y, size, size, angle, time, param);
  }

  /** This method should not generally be called.  The mapping is undefined without knowing
      the texture coordinates for a particular triangle. */

  public double getDisplacement(Vec3 pos, double size, double time, double param[])
  {
    return texture.getDisplacement(pos.x, pos.y, size, size, time, param);
  }

  /** Given a Mesh to which this mapping has been applied, return the texture coordinates at
      each vertex. */
  
  public Vec2 [] findTextureCoordinates(Mesh mesh)
  {
    TextureParameter param[] = mesh.getParameters();
    ParameterValue values[] = mesh.getParameterValues();
    if (mesh instanceof FacetedMesh && isPerFaceVertex((FacetedMesh) mesh))
    {
      // This is a per-face-vertex mapping, so we need to simplify it down to per-vertex.

      FacetedMesh fm = (FacetedMesh) mesh;
      FaceVertexParameterValue uval = null, vval = null;
      for (int i = 0; i < param.length; i++)
        {
          if (param[i].equals(uparam))
            uval = (FaceVertexParameterValue) values[i];
          else if (param[i].equals(vparam))
            vval = (FaceVertexParameterValue) values[i];
        }
      Vec2 uv[] = new Vec2 [mesh.getVertices().length];
      for (int i = 0; i < fm.getFaceCount(); i++)
        for (int j = 0; j < fm.getFaceVertexCount(i); j++)
          uv[fm.getFaceVertexIndex(i, j)] = new Vec2(uval.getValue(i, j), vval.getValue(i, j));
      return uv;
    }
    double uval[] = null, vval[] = null;
    for (int i = 0; i < param.length; i++)
      {
        if (param[i].equals(uparam))
          uval = ((VertexParameterValue) values[i]).getValue();
        else if (param[i].equals(vparam))
          vval = ((VertexParameterValue) values[i]).getValue();
      }
    Vec2 uv[] = new Vec2 [uval.length];
    for (int i = 0; i < uv.length; i++)
      uv[i] = new Vec2(uval[i], vval[i]);
    return uv;
  }

  /** Given a FacetedMesh to which this mapping has been applied, return the texture coordinates at
      each vertex of each face.  The return value is an array of size [# faces][# vertices/face]
      containing the face-vertex texture coordinates. */
  
  public Vec2 [][] findFaceTextureCoordinates(FacetedMesh mesh)
  {
    int faces = mesh.getFaceCount();
    TextureParameter param[] = mesh.getParameters();
    ParameterValue values[] = mesh.getParameterValues();
    FaceVertexParameterValue uval = null, vval = null;
    for (int i = 0; i < param.length; i++)
      {
        if (param[i].equals(uparam))
          uval = (FaceVertexParameterValue) values[i];
        else if (param[i].equals(vparam))
          vval = (FaceVertexParameterValue) values[i];
      }
    Vec2 uv[][] = new Vec2[faces][];
    for (int i = 0; i < faces; i++)
    {
      uv[i] = new Vec2[mesh.getFaceVertexCount(i)];
      for (int j = 0; j < uv[i].length; j++)
        uv[i][j] = new Vec2(uval.getValue(i, j), vval.getValue(i, j));
    }
    return uv;
  }

  /**
   * Given an object to which this mapping has been applied and the desired texture coordinates
   * at each vertex, set the texture parameters accordingly.
   */
  
  public void setTextureCoordinates(Object3D obj, Vec2 uv[])
  {
    setTextureCoordinates(obj, uv, uparam, vparam);
  }

  /**
   * Given an object to which this mapping has been applied and the desired texture coordinates
   * at each vertex, set the texture parameters accordingly.
   * <p>
   * In most cases, you can call {@link #setTextureCoordinates(artofillusion.object.Object3D, artofillusion.math.Vec2[])}
   * instead, since the mapping already knows what parameters correspond to the U and V
   * coordinates.  This version is necessary when this mapping is part of a {@link LayeredMapping},
   * since the LayeredMapping will have created new parameters that must be used in place of the original ones.
   */

  public void setTextureCoordinates(Object3D obj, Vec2 uv[], TextureParameter uParameter, TextureParameter vParameter)
  {
    double uval[] = new double [uv.length], vval[] = new double [uv.length];
    for (int i = 0; i < uv.length; i++)
    {
      uval[i] = uv[i].x;
      vval[i] = uv[i].y;
    }
    obj.setParameterValue(uParameter, new VertexParameterValue(uval));
    obj.setParameterValue(vParameter, new VertexParameterValue(vval));
  }

  /**
   * Given a triangle mesh to which this mapping has been applied and the desired texture coordinates
   * at each vertex, set the texture parameters accordingly.  uv is an array of size [# faces][# vertices/face]
   * containing the face-vertex texture coordinates.
   */
  
  public void setFaceTextureCoordinates(Object3D obj, Vec2 uv[][])
  {
    setFaceTextureCoordinates(obj, uv, uparam, vparam);
  }

  /**
   * Given a triangle mesh to which this mapping has been applied and the desired texture coordinates
   * at each vertex, set the texture parameters accordingly.  uv is an array of size [# faces][# vertices/face]
   * containing the face-vertex texture coordinates.
   * <p>
   * In most cases, you can call {@link #setFaceTextureCoordinates(artofillusion.object.Object3D, artofillusion.math.Vec2[][])}
   * instead, since the mapping already knows what parameters correspond to the U and V
   * coordinates.  This version is necessary when this mapping is part of a {@link LayeredMapping},
   * since the LayeredMapping will have created new parameters that must be used in place of the original ones.
   */

  public void setFaceTextureCoordinates(Object3D obj, Vec2 uv[][], TextureParameter uParameter, TextureParameter vParameter)
  {
    while (obj instanceof ObjectWrapper)
      obj = ((ObjectWrapper) obj).getWrappedObject();
    FacetedMesh mesh = (FacetedMesh) obj;
    int faces = mesh.getFaceCount();
    double uval[][] = new double[faces][], vval[][] = new double[faces][];
    for (int i = 0; i < faces; i++)
    {
      uval[i] = new double[uv[i].length];
      vval[i] = new double[uv[i].length];
      for (int j = 0; j < uval[i].length; j++)
      {
        uval[i][j] = uv[i][j].x;
        vval[i][j] = uv[i][j].y;
      }
    }
    obj.setParameterValue(uparam, new FaceVertexParameterValue(uval));
    obj.setParameterValue(vparam, new FaceVertexParameterValue(vval));
  }

  /** Given a faceted mesh to which this mapping has been applied, determined whether the mapping
      is per-face-vertex. */
  
  public boolean isPerFaceVertex(FacetedMesh mesh)
  {
    TextureParameter param[] = mesh.getParameters();
    for (int i = 0; i < param.length; i++)
      if (param[i].equals(uparam) || param[i].equals(vparam))
        return (mesh.getParameterValues()[i] instanceof FaceVertexParameterValue);
    return false;
  }

  public TextureMapping duplicate()
  {
    return duplicate(object, texture);
  }

  public TextureMapping duplicate(Object3D obj, Texture tex)
  {
    UVMapping map = new UVMapping(obj, tex);
    
    map.numTextureParams = numTextureParams;
    map.uparam = uparam;
    map.vparam = vparam;
    return map;
  }
  
  public void copy(TextureMapping mapping)
  {
    UVMapping map = (UVMapping) mapping; 
    
    numTextureParams = map.numTextureParams;
    uparam = map.uparam;
    vparam = map.vparam;
  }

  /* Get the list of texture parameters associated with this mapping and its texture.
     That includes the texture's parameters, and parameters for the texture coordinates. */
  
  public TextureParameter [] getParameters()
  {
    TextureParameter tp[] = getTexture().getParameters();
    numTextureParams = tp.length;
    TextureParameter p[] = new TextureParameter [numTextureParams+2];
    System.arraycopy(tp, 0, p, 0, numTextureParams);
    if (uparam == null)
      {
        uparam = new TextureParameter(this, "U", -Double.MAX_VALUE, Double.MAX_VALUE, 0.0);
        vparam = new TextureParameter(this, "V", -Double.MAX_VALUE, Double.MAX_VALUE, 0.0);
        uparam.type = TextureParameter.X_COORDINATE;
        vparam.type = TextureParameter.Y_COORDINATE;
      }
    p[numTextureParams] = uparam;
    p[numTextureParams+1] = vparam;
    return p;
  }

  public Widget getEditingPanel(Object3D obj, MaterialPreviewer preview)
  {
    return new Editor(obj, preview);
  }
  
  public UVMapping(DataInputStream in, Object3D theObject, Texture theTexture) throws IOException, InvalidObjectException
  {
    super(theObject, theTexture);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    setAppliesTo(in.readShort());
  }
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeShort(0);
    out.writeShort(appliesTo());
  }
  
  /* Editor is an inner class for editing the mapping. */

  class Editor extends FormContainer
  {
    BComboBox applyToChoice;
    Object3D theObject;
    MaterialPreviewer preview;

    public Editor(Object3D obj, MaterialPreviewer preview)
    {
      super(1, 2);
      theObject = obj;
      this.preview = preview;
      
      // Add the various components to the Panel.
      
      add(Translate.button("editUVCoords", this, "doEdit"), 0, 0);
      RowContainer applyRow = new RowContainer();
      applyRow.add(new BLabel(Translate.text("applyTo")+":"));
      applyRow.add(applyToChoice = new BComboBox(new String [] {
        Translate.text("frontAndBackFaces"),
        Translate.text("frontFacesOnly"),
        Translate.text("backFacesOnly")
      }));
      add(applyRow, 0, 1);
      applyToChoice.setSelectedIndex(appliesTo());
      applyToChoice.addEventLink(ValueChangedEvent.class, this, "applyToChanged");
    }

    private void doEdit()
    {
      new UVMappingWindow((BDialog) UIUtilities.findWindow(this), theObject, UVMapping.this);
      Widget parent = getParent();
      while (!(parent instanceof TextureMappingDialog) && parent != null)
        parent = parent.getParent();
      if (parent != null)
        ((TextureMappingDialog) parent).setPreviewMapping(UVMapping.this);
      preview.render();
    }

    private void applyToChanged()
    {
      setAppliesTo((short) applyToChoice.getSelectedIndex());
      preview.setTexture(getTexture(), UVMapping.this);
      preview.render();
    }
  }
}
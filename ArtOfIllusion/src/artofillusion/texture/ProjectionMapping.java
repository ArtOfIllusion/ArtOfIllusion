/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** ProjectionMapping is a Mapping2D which projects the texture along a specified direction. */

public class ProjectionMapping extends Mapping2D
{
  CoordinateSystem coords;
  double ax, bx, cx, dx, ay, by, cy, dy;
  double xscale, yscale, matScaleX, matScaleY;
  boolean coordsFromParams;
  int numTextureParams;
  TextureParameter xparam, yparam, zparam;

  public ProjectionMapping(Object3D theObject, Texture theTexture)
  {
    super(theObject, theTexture);
    coords = new CoordinateSystem(new Vec3(), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
    xscale = yscale = 1.0;
    dx = dy = -0.5;
    findCoefficients();
  }

  public static String getName()
  {
    return "Projection";
  }

  /* Calculate the mapping coefficients. */
  
  private void findCoefficients()
  {
    Vec3 zdir = coords.getZDirection(), ydir = coords.getUpDirection();
    Vec3 xdir = ydir.cross(zdir);
    ax = xdir.x/xscale;
    bx = xdir.y/xscale;
    cx = xdir.z/xscale;
    ay = ydir.x/yscale;
    by = ydir.y/yscale;
    cy = ydir.z/yscale;
    matScaleX = Math.abs(1.0/xscale);
    matScaleY = Math.abs(1.0/yscale);
  }
  
  /** Get a vector whose components contain the center position for the mapping. */
  
  public Vec2 getCenter()
  {
    return new Vec2(dx, dy);
  }
  
  /** Set the center position for the mapping. */
  
  public void setCenter(Vec2 center)
  {
    dx = center.x;
    dy = center.y;
    findCoefficients();
  }
  
  /** Get a vector whose components contain the scale factors for the mapping. */
  
  public Vec2 getScale()
  {
    return new Vec2(xscale, yscale);
  }
  
  /** Set the scale factors for the mapping. */
  
  public void setScale(Vec2 scale)
  {
    xscale = scale.x;
    yscale = scale.y;
    findCoefficients();
  }
  
  /** Get a vector whose components contain the rotation angles for the mapping. */
  
  public Vec3 getRotations()
  {
    double angles[] = coords.getRotationAngles();
    return new Vec3(angles[0], angles[1], angles[2]);
  }
  
  /** Set the rotation angles for the mapping. */
  
  public void setRotations(Vec3 angles)
  {
    coords.setOrientation(angles.x, angles.y, angles.z);
    findCoefficients();
  }
  
  /** Determine whether this texture is bound to the surface (texture coordinates are determined by parameters,
      not by position). */
  
  public boolean isBoundToSurface()
  {
    return coordsFromParams;
  }

  /** Set whether this texture is bound to the surface (texture coordinates are determined by parameters,
      not by position). */
  
  public void setBoundToSurface(boolean bound)
  {
    coordsFromParams = bound;
  }

  /* Create a rendering triangle with this mapping. */

  public RenderingTriangle mapTriangle(int v1, int v2, int v3, int n1, int n2, int n3, Vec3 vert[])
  {
    Vec3 c1 = vert[v1], c2 = vert[v2], c3 = vert[v3];
    
    if (coordsFromParams)
      return new UVMappedTriangle(v1, v2, v3, n1, n2, n3);
    else
      return new Linear2DTriangle(v1, v2, v3, n1, n2, n3, c1.x*ax+c1.y*bx+c1.z*cx-dx, c1.x*ay+c1.y*by+c1.z*cy-dy, 
          c2.x*ax+c2.y*bx+c2.z*cx-dx, c2.x*ay+c2.y*by+c2.z*cy-dy,
          c3.x*ax+c3.y*bx+c3.z*cx-dx, c3.x*ay+c3.y*by+c3.z*cy-dy);
  }

  /** This method is called once the texture parameters for the vertices of a triangle
      are known. */
  
  public void setParameters(RenderingTriangle tri, double p1[], double p2[], double p3[], RenderingMesh mesh)
  {
    if (!(tri instanceof UVMappedTriangle))
      return;
    UVMappedTriangle uv = (UVMappedTriangle) tri;
    double x1 = p1[numTextureParams];
    double y1 = p1[numTextureParams+1];
    double z1 = p1[numTextureParams+2];
    double x2 = p2[numTextureParams];
    double y2 = p2[numTextureParams+1];
    double z2 = p2[numTextureParams+2];
    double x3 = p3[numTextureParams];
    double y3 = p3[numTextureParams+1];
    double z3 = p3[numTextureParams+2];
    uv.setTextureCoordinates((float) (x1*ax+y1*bx+z1*cx-dx), (float) (x1*ay+y1*by+z1*cy-dy),
        (float) (x2*ax+y2*bx+z2*cx-dx), (float) (x2*ay+y2*by+z2*cy-dy),
        (float) (x3*ax+y3*bx+z3*cx-dx), (float) (x3*ay+y3*by+z3*cy-dy),
        mesh.vert[uv.v1], mesh.vert[uv.v2], mesh.vert[uv.v3]);
  }

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
    if (coordsFromParams && param.length > numTextureParams && param[numTextureParams] != Double.MAX_VALUE)
      {
        double x = param[numTextureParams];
        double y = param[numTextureParams+1];
        double z = param[numTextureParams+2];
        texture.getTextureSpec(spec, x*ax+y*bx+z*cx-dx, x*ay+y*by+z*cy-dy, size*matScaleX, size*matScaleY, angle, time, param);
      }
    else
      texture.getTextureSpec(spec, pos.x*ax+pos.y*bx+pos.z*cx-dx, pos.x*ay+pos.y*by+pos.z*cy-dy, size*matScaleX, size*matScaleY, angle, time, param);
    if (texture.hasComponent(Texture.BUMP_COMPONENT))
      {
        double s = spec.bumpGrad.x;
        double t = spec.bumpGrad.y;
        spec.bumpGrad.set(s*ax+t*ay, s*bx+t*by, s*cx+t*cy);
      }
  }

  public void getTransparency(Vec3 pos, RGBColor trans, double angle, double size, double time, double param[])
  {
    if (!appliesToFace(angle > 0.0))
      {
        trans.setRGB(1.0f, 1.0f, 1.0f);
        return;
      }
    if (coordsFromParams && param.length > numTextureParams && param[numTextureParams] != Double.MAX_VALUE)
      {
        double x = param[numTextureParams];
        double y = param[numTextureParams+1];
        double z = param[numTextureParams+2];
        texture.getTransparency(trans, x*ax+y*bx+z*cx-dx, x*ay+y*by+z*cy-dy, size*matScaleX, size*matScaleY, angle, time, param);
      }
    else
      texture.getTransparency(trans, pos.x*ax+pos.y*bx+pos.z*cx-dx, pos.x*ay+pos.y*by+pos.z*cy-dy, size*matScaleX, size*matScaleY, angle, time, param);
  }

  public double getDisplacement(Vec3 pos, double size, double time, double param[])
  {
    if (coordsFromParams && param.length > numTextureParams && param[numTextureParams] != Double.MAX_VALUE)
      {
        double x = param[numTextureParams];
        double y = param[numTextureParams+1];
        double z = param[numTextureParams+2];
        return texture.getDisplacement(x*ax+y*bx+z*cx-dx, x*ay+y*by+z*cy-dy, size*matScaleX, size*matScaleY, time, param);
      }
      return texture.getDisplacement(pos.x*ax+pos.y*bx+pos.z*cx-dx, pos.x*ay+pos.y*by+pos.z*cy-dy, size*matScaleX, size*matScaleY, time, param);
  }

  /** Given a Mesh to which this mapping has been applied, return the texture coordinates at
      each vertex. */
  
  public Vec2 [] findTextureCoordinates(Mesh mesh)
  {
    TextureParameter param[] = mesh.getParameters();
    ParameterValue paramValue[] = mesh.getParameterValues();
    VertexParameterValue xval = null, yval = null, zval = null;
    if (coordsFromParams)
      for (int i = 0; i < param.length; i++)
        {
          if (param[i].equals(xparam))
            xval = (VertexParameterValue) paramValue[i];
          else if (param[i].equals(yparam))
            yval = (VertexParameterValue) paramValue[i];
          else if (param[i].equals(zparam))
            zval = (VertexParameterValue) paramValue[i];
        }
    MeshVertex vert[] = mesh.getVertices();
    Vec2 uv[] = new Vec2 [vert.length];
    for (int i = 0; i < vert.length; i++)
      {
        double x, y, z;
        if (xval == null)
          {
            x = vert[i].r.x;
            y = vert[i].r.y;
            z = vert[i].r.z;
          }
        else
          {
            x = xval.getValue()[i];
            y = yval.getValue()[i];
            z = zval.getValue()[i];
          }
        uv[i] = new Vec2(x*ax+y*bx+z*cx-dx, x*ay+y*by+z*cy-dy);
      }
    return uv;
  }

  public TextureMapping duplicate()
  {
    return duplicate(object, texture);
  }

  public TextureMapping duplicate(Object3D obj, Texture tex)
  {
    ProjectionMapping map = new ProjectionMapping(obj, tex);
    
    map.coords = coords.duplicate();
    map.dx = dx;
    map.dy = dy;
    map.xscale = xscale;
    map.yscale = yscale;
    map.findCoefficients();
    map.coordsFromParams = coordsFromParams;
    map.numTextureParams = numTextureParams;
    map.setAppliesTo(appliesTo());
    map.xparam = xparam;
    map.yparam = yparam;
    map.zparam = zparam;
    return map;
  }
  
  public void copy(TextureMapping mapping)
  {
    ProjectionMapping map = (ProjectionMapping) mapping; 
    
    coords = map.coords.duplicate();
    dx = map.dx;
    dy = map.dy;
    xscale = map.xscale;
    yscale = map.yscale;
    findCoefficients();
    coordsFromParams = map.coordsFromParams;
    numTextureParams = map.numTextureParams;
    setAppliesTo(map.appliesTo());
    xparam = map.xparam;
    yparam = map.yparam;
    zparam = map.zparam;
  }

  /** Get the list of texture parameters associated with this mapping and its texture.
      That includes the texture's parameters, and possibly parameters for the texture
      coordinates. */
  
  public TextureParameter [] getParameters()
  {
    if (!coordsFromParams)
      return getTexture().getParameters();
    TextureParameter tp[] = getTexture().getParameters();
    numTextureParams = tp.length;
    TextureParameter p[] = new TextureParameter [numTextureParams+3];
    System.arraycopy(tp, 0, p, 0, numTextureParams);
    if (xparam == null)
      {
        xparam = new TextureParameter(this, "X", -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        yparam = new TextureParameter(this, "Y", -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        zparam = new TextureParameter(this, "Z", -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        xparam.type = TextureParameter.X_COORDINATE;
        yparam.type = TextureParameter.Y_COORDINATE;
        zparam.type = TextureParameter.Z_COORDINATE;
        xparam.assignNewID();
        yparam.assignNewID();
        zparam.assignNewID();
      }
    p[numTextureParams] = xparam;
    p[numTextureParams+1] = yparam;
    p[numTextureParams+2] = zparam;
    return p;
  }

  public Widget getEditingPanel(Object3D obj, MaterialPreviewer preview)
  {
    return new Editor(obj, preview);
  }
  
  public ProjectionMapping(DataInputStream in, Object3D theObject, Texture theTexture) throws IOException, InvalidObjectException
  {
    super(theObject, theTexture);

    short version = in.readShort();
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    coords = new CoordinateSystem(in);
    dx = in.readDouble();
    dy = in.readDouble();
    xscale = in.readDouble();
    yscale = in.readDouble();
    findCoefficients();
    coordsFromParams = in.readBoolean();
    if (version == 1)
      setAppliesTo(in.readShort());
  }
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeShort(1);
    coords.writeToFile(out);
    out.writeDouble(dx);
    out.writeDouble(dy);
    out.writeDouble(xscale);
    out.writeDouble(yscale);
    out.writeBoolean(coordsFromParams);
    out.writeShort(appliesTo());
  }
  
  /* Editor is an inner class for editing the mapping. */

  class Editor extends FormContainer
  {
    ValueField xrotField, yrotField, zrotField, xscaleField, yscaleField, xtransField, ytransField;
    BCheckBox coordsFromParamsBox;
    BComboBox applyToChoice;
    Object3D theObject;
    MaterialPreviewer preview;

    public Editor(Object3D obj, MaterialPreviewer preview)
    {
      super(6, 8);
      theObject = obj;
      this.preview = preview;
      
      // Add the various components to the Panel.
      
      setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
      add(new BLabel(Translate.text("Scale")+":"), 0, 0, 6, 1);
      add(new BLabel("X"), 0, 1);
      add(xscaleField = new ValueField(xscale, ValueField.NONZERO, 5), 1, 1);
      add(new BLabel("Y"), 2, 1);
      add(yscaleField = new ValueField(yscale, ValueField.NONZERO, 5), 3, 1);
      add(new BLabel(Translate.text("Center")+":"), 0, 2, 6, 1);
      add(new BLabel("X"), 0, 3);
      add(xtransField = new ValueField(dx, ValueField.NONE, 5), 1, 3);
      add(new BLabel("Y"), 2, 3);
      add(ytransField = new ValueField(dy, ValueField.NONE, 5), 3, 3);
      double angles[] = coords.getRotationAngles();
      add(new BLabel(Translate.text("Rotation")+":"), 0, 4, 6, 1);
      add(new BLabel("X"), 0, 5);
      add(xrotField = new ValueField(angles[0], ValueField.NONE, 5), 1, 5);
      add(new BLabel("Y"), 2, 5);
      add(yrotField = new ValueField(angles[1], ValueField.NONE, 5), 3, 5);
      add(new BLabel("Z"), 4, 5);
      add(zrotField = new ValueField(angles[2], ValueField.NONE, 5), 5, 5);
      RowContainer applyRow = new RowContainer();
      applyRow.add(new BLabel(Translate.text("applyTo")+":"));
      applyRow.add(applyToChoice = new BComboBox(new String [] {
        Translate.text("frontAndBackFaces"),
        Translate.text("frontFacesOnly"),
        Translate.text("backFacesOnly")
      }));
      add(applyRow, 0, 6, 6, 1);
      applyToChoice.setSelectedIndex(appliesTo());
      add(coordsFromParamsBox = new BCheckBox(Translate.text("bindTexToSurface"), coordsFromParams), 0, 7, 6, 1);
      coordsFromParamsBox.setEnabled(theObject instanceof Mesh || theObject instanceof Actor);
      xscaleField.addEventLink(ValueChangedEvent.class, this);
      yscaleField.addEventLink(ValueChangedEvent.class, this);
      xtransField.addEventLink(ValueChangedEvent.class, this);
      ytransField.addEventLink(ValueChangedEvent.class, this);
      xrotField.addEventLink(ValueChangedEvent.class, this);
      yrotField.addEventLink(ValueChangedEvent.class, this);
      zrotField.addEventLink(ValueChangedEvent.class, this);
      coordsFromParamsBox.addEventLink(ValueChangedEvent.class, this);
      applyToChoice.addEventLink(ValueChangedEvent.class, this);
    }

    private void processEvent()
    {
      xscale = xscaleField.getValue();
      yscale = yscaleField.getValue();
      dx = xtransField.getValue();
      dy = ytransField.getValue();
      coords.setOrientation(xrotField.getValue(), yrotField.getValue(), zrotField.getValue());
      findCoefficients();
      coordsFromParams = coordsFromParamsBox.getState();
      setAppliesTo((short) applyToChoice.getSelectedIndex());
      preview.setTexture(getTexture(), ProjectionMapping.this);
      preview.render();
    }
  }
}
/* Copyright (C) 2000-2009 by Peter Eastman

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
import java.lang.reflect.*;
import java.util.*;

/** LayeredMapping is the TextureMapping corresponding to LayeredTextures.  It allows
    multiple textures to be layered on top of each other.  Most of the actual work is done
    by this class, rather than LayeredTexture. */

public class LayeredMapping extends TextureMapping
{
  Object3D theObject;
  LayeredTexture theTexture;
  Texture texture[];
  TextureMapping mapping[];
  int blendMode[], fractParamIndex[], paramStartIndex[], numParams[], fractParamID[], maxParams;

  public static final int BLEND = 0;
  public static final int OVERLAY_BLEND_BUMPS = 1;
  public static final int OVERLAY_ADD_BUMPS = 2;

  public LayeredMapping(Object3D obj, Texture tex)
  {
    theObject = obj;
    theTexture = (LayeredTexture) tex;
    texture = new Texture [0];
    mapping = new TextureMapping [0];
    blendMode = new int [0];
    fractParamID = new int [0];
  }
  
  /** Get the number of layers. */
  
  public int getNumLayers()
  {
    return texture.length;
  }
  
  /** Get the list of layers for this texture. */
  
  public Texture [] getLayers()
  {
    return texture;
  }
  
  /** Get a particular layer. */
  
  public Texture getLayer(int which)
  {
    return texture[which];
  }
  
  /** Set a particular layer. */
  
  public void setLayer(int which, Texture tex)
  {
    texture[which] = tex;
  }
  
  /** Get the mapping for a particular layer. */
  
  public TextureMapping getLayerMapping(int which)
  {
    return mapping[which];
  }
  
  /** Set the mapping for a particular layer. */
  
  public void setLayerMapping(int which, TextureMapping map)
  {
    mapping[which] = map;
  }
  
  /** Get the blend mode for a particular layer. */
  
  public int getLayerMode(int which)
  {
    return blendMode[which];
  }
  
  
  /** Set the blend mode for a particular layer. */
  
  public void setLayerMode(int which, int mode)
  {
    blendMode[which] = mode;
  }
    
  /** Get the list of texture parameters. */
  
  public TextureParameter[] getParameters()
  {
    Vector<TextureParameter> param = new Vector<TextureParameter>();
    TextureParameter p[];
    int i, j;
    
    // There are two types of parameters: those corresponding to the blending fraction for
    // a layer, and those which belong to layer.  We recalculate the list every time this
    // method is called, since there is no way of knowning when a layer texture might have
    // been edited such that its list of parameters has changed.
    
    fractParamIndex = new int [texture.length];
    paramStartIndex = new int [texture.length];
    numParams = new int [texture.length];
    maxParams = 0;
    for (i = 0; i < texture.length; i++)
    {
      fractParamIndex[i] = param.size();
      TextureParameter fractParam = new TextureParameter(this, texture[i].getName()+" fraction", 0.0f, 1.0f, 1.0f);
      fractParam.setID(fractParamID[i]);
      param.addElement(fractParam);
      p = mapping[i].getParameters();
      if (p != null)
      {
        numParams[i] = p.length;
        paramStartIndex[i] = param.size();
        for (j = 0; j < p.length; j++)
        {
          param.addElement(p[j].duplicate());
          if (p[j].identifier != -1)
            param.lastElement().setID(System.identityHashCode(mapping[i])+p[j].identifier*1025);
        }
        if (p.length > maxParams)
          maxParams = p.length;
      }
    }
    p = new TextureParameter [param.size()];
    for (i = 0; i < p.length; i++)
      p[i] = param.elementAt(i);
    return p;
  }

  /** Get the list of texture parameters for a particular layer. */
  
  public TextureParameter [] getLayerParameters(int which)
  {
    TextureParameter p[] = getParameters();
    TextureParameter param[] = new TextureParameter [numParams[which]+1];
    param[0] = p[fractParamIndex[which]];
    for (int i = 1; i < param.length; i++)
      param[i] = p[i+paramStartIndex[which]-1];
    return param;
  }

  /** Get the parameter which specifies the blending fraction for a layer. */

  public TextureParameter getLayerBlendingParameter(int layer)
  {
    return getParameters()[fractParamIndex[layer]];
  }

  /**
   * Get the parameter corresponding to a parameter of one of the layer textures or mappings.
   * <p>
   * A LayeredMapping creates a new TextureParameter corresponding to each parameter of its
   * component Textures and TextureMappings.  This is necessary because a single Texture
   * might be used for multiple layers, so there must be multiple parameters corresponding to
   * each parameter of that Texture.
   *
   * @param parameter a parameter defined by a layer's Texture or TextureMapping
   * @param layer     the layer for which to get the parameter
   * @return the parameter of this LayeredMapping corresponding to the specified parameter,
   * or null if the specified parameter does not belong to the Texture or TextureMapping of
   * the specified layer
   */

  public TextureParameter getParameterForLayer(TextureParameter parameter, int layer)
  {
    TextureParameter originalParams[] = mapping[layer].getParameters();
    TextureParameter newParams[] = getLayerParameters(layer);
    for (int i = 0; i < originalParams.length; i++)
      if (parameter.equals(originalParams[i]))
        return newParams[i+1];
    return null;
  }

  /**
   * Add a layer to the texture.
   *
   * @deprecated Use {@link #addLayer(int, Texture, TextureMapping, int)} instead.
   */

  public void addLayer(Texture tex)
  {
    Texture newtexture[] = new Texture [texture.length+1];
    TextureMapping newmapping[] = new TextureMapping [texture.length+1];
    int newblendMode[] = new int [texture.length+1];
    int newFractParamID[] = new int [texture.length+1];
    int i;
    
    newtexture[0] = tex;
    newmapping[0] = tex.getDefaultMapping(theObject);
    newblendMode[0] = BLEND;
    newFractParamID[0] = TextureParameter.getUniqueID();
    for (i = 0; i < texture.length; i++)
      {
        newtexture[i+1] = texture[i];
        newmapping[i+1] = mapping[i];
        newblendMode[i+1] = blendMode[i];
        newFractParamID[i+1] = fractParamID[i];
      }
    texture = newtexture;
    mapping = newmapping;
    blendMode = newblendMode;
    fractParamID = newFractParamID;
  }

  /**
   * Add a layer to the texture.
   *
   * @param index  the position at which the new layer should be added, where layer 0
   *               is the topmost layer (the one visible over all others)
   * @param tex    the Texture of the new layer
   * @param map    the TextureMapping of the new layer
   * @param mode   the blending mode of the new layer ({@link #BLEND}, {@link #OVERLAY_BLEND_BUMPS},
   *               or {@link #OVERLAY_ADD_BUMPS})
   */

  public void addLayer(int index, Texture tex, TextureMapping map, int mode)
  {
    Texture newtexture[] = new Texture [texture.length+1];
    TextureMapping newmapping[] = new TextureMapping [texture.length+1];
    int newblendMode[] = new int [texture.length+1];
    int newFractParamID[] = new int [texture.length+1];

    newtexture[index] = tex;
    newmapping[index] = map;
    newblendMode[index] = mode;
    newFractParamID[index] = TextureParameter.getUniqueID();
    for (int i = 0; i < texture.length; i++)
      {
        int j = (i < index ? i : i+1);
        newtexture[j] = texture[i];
        newmapping[j] = mapping[i];
        newblendMode[j] = blendMode[i];
        newFractParamID[j] = fractParamID[i];
      }
    texture = newtexture;
    mapping = newmapping;
    blendMode = newblendMode;
    fractParamID = newFractParamID;
  }

  /** Delete a layer from the texture. */

  public void deleteLayer(int which)
  {
    Texture newtexture[] = new Texture [texture.length-1];
    TextureMapping newmapping[] = new TextureMapping [texture.length-1];
    int newblendMode[] = new int [texture.length-1];
    int newFractParamID[] = new int [texture.length-1];
    int i, j;
    
    for (i = j = 0; i < texture.length; i++)
      if (i != which)
        {
          newtexture[j] = texture[i];
          newmapping[j] = mapping[i];
          newblendMode[j] = blendMode[i];
          newFractParamID[j] = fractParamID[i];
          j++;
        }
    texture = newtexture;
    mapping = newmapping;
    blendMode = newblendMode;
    fractParamID = newFractParamID;
  }
  
  /** Move a layer to a new position. */
  
  public void moveLayer(int which, int pos)
  {
    Texture newtexture[] = new Texture [texture.length];
    TextureMapping newmapping[] = new TextureMapping [texture.length];
    int newblendMode[] = new int [texture.length];
    int newFractParamID[] = new int [texture.length];
    int i, j;
    
    for (i = j = 0; i < newtexture.length; i++)
      {
        if (j == which)
          j++;
        if (i == pos)
          {
            newtexture[i] = texture[which];
            newmapping[i] = mapping[which];
            newblendMode[i] = blendMode[which];
            newFractParamID[i] = fractParamID[which];
          }
        else
          {
            newtexture[i] = texture[j];
            newmapping[i] = mapping[j];
            newblendMode[i] = blendMode[j];
            newFractParamID[i] = fractParamID[j];
            j++;
          }
      }
    texture = newtexture;
    mapping = newmapping;
    blendMode = newblendMode;
    fractParamID = newFractParamID;
  }

  /** Loading and saving of layered mappings works a bit differently from other mappings, 
     since it needs to refer other textures in the scene. */

  public void readFromFile(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    int numTextures = in.readInt();

    if (version != 0)
      throw new InvalidObjectException("");    
    texture = new Texture [numTextures];
    mapping = new TextureMapping [numTextures];
    blendMode = new int [numTextures];
    fractParamID = new int [numTextures];
    for (int i = 0; i < texture.length; i++)
      {
        texture[i] = theScene.getTexture(in.readInt());
        blendMode[i] = in.readInt();
        fractParamID[i] = TextureParameter.getUniqueID();
        try
          {
            Class mapClass = ArtOfIllusion.getClass(in.readUTF());
            Constructor con = mapClass.getConstructor(DataInputStream.class, Object3D.class, Texture.class);
            mapping[i] = (TextureMapping) con.newInstance(in, theObject, texture[i]);
          }
        catch (Exception ex)
          {
            throw new IOException();
          }
      }
  }
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(0);
    out.writeInt(texture.length);
    for (int i = 0; i < texture.length; i++)
      {
        out.writeInt(theScene.indexOf(texture[i]));
        out.writeInt(blendMode[i]);
        out.writeUTF(mapping[i].getClass().getName());
        mapping[i].writeToFile(out);
      }
  }

  /** This form of writeToFile() is never used, and should never be called. */

  public void writeToFile(DataOutputStream out) throws IOException
  {
    throw new IllegalStateException();
  }

  public static String getName()
  {
    return "Layered";
  }

  public RenderingTriangle mapTriangle(int v1, int v2, int v3, int n1, int n2, int n3, Vec3 vert[])
  {
    return new LayeredTriangle(v1, v2, v3, n1, n2, n3, vert[v1].x, vert[v1].y, vert[v1].z, 
        vert[v2].x, vert[v2].y, vert[v2].z, vert[v3].x, vert[v3].y, vert[v3].z, this, theTexture, vert);
  }
  
  /** This method is called once the texture parameters for the vertices of a triangle
      are known. */
  
  public void setParameters(RenderingTriangle tri, double p1[], double p2[], double p3[], RenderingMesh mesh)
  {
    LayeredTriangle lt = (LayeredTriangle) tri;
    
    for (int i = 0; i < lt.layerTriangle.length; i++)
      if (lt.layerTriangle[i] != null && numParams[i] > 0)
        {
          double t1[] = new double [numParams[i]];
          double t2[] = new double [numParams[i]];
          double t3[] = new double [numParams[i]];
          for (int j = 0; j < numParams[i]; j++)
            {
              t1[j] = p1[paramStartIndex[i]+j];
              t2[j] = p2[paramStartIndex[i]+j];
              t3[j] = p3[paramStartIndex[i]+j];
            }
          mapping[i].setParameters(lt.layerTriangle[i], t1, t2, t3, mesh);
        }
  }
  
  /** Determine the surface properties by adding up the properties of all of the layers. */
  
  public void getTextureSpec(Vec3 pos, TextureSpec spec, double angle, double size, double t, double param[])
  {
    float rt = 1.0f, gt = 1.0f, bt = 1.0f;
    double ft = 1.0;
    boolean front = (angle > 0.0);
    spec.diffuse.setRGB(0.0f, 0.0f, 0.0f);
    spec.specular.setRGB(0.0f, 0.0f, 0.0f);
    spec.hilight.setRGB(0.0f, 0.0f, 0.0f);
    spec.transparent.setRGB(1.0f, 1.0f, 1.0f);
    spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
    spec.roughness = spec.cloudiness = 0.0;
    spec.bumpGrad.set(0.0, 0.0, 0.0);
    TextureSpec tempSpec = new TextureSpec();
    double paramTemp[] = new double [maxParams];
    for (int i = 0; i < texture.length; i++)
    {
      if (!mapping[i].appliesToFace(front))
        continue;
      double f = param[fractParamIndex[i]];
      if (numParams[i] > 0)
        for (int j = 0; j < numParams[i]; j++)
          paramTemp[j] = param[paramStartIndex[i]+j];
      mapping[i].getTextureSpec(pos, tempSpec, angle, size, t, paramTemp);
      float r = rt*(float) f;
      float g = gt*(float) f;
      float b = bt*(float) f;
      spec.diffuse.add(r*tempSpec.diffuse.red, g*tempSpec.diffuse.green, b*tempSpec.diffuse.blue);
      spec.specular.add(r*tempSpec.specular.red, g*tempSpec.specular.green, b*tempSpec.specular.blue);
          spec.hilight.add(r*tempSpec.hilight.red, g*tempSpec.hilight.green, b*tempSpec.hilight.blue);
      spec.emissive.add(r*tempSpec.emissive.red, g*tempSpec.emissive.green, b*tempSpec.emissive.blue);
      if (blendMode[i] == BLEND)
      {
        spec.transparent.subtract(r*(1.0f-tempSpec.transparent.red), g*(1.0f-tempSpec.transparent.green), b*(1.0f-tempSpec.transparent.blue));
        f = ft*f;
      }
      else
      {
        r *= (1.0f-tempSpec.transparent.red);
        g *= (1.0f-tempSpec.transparent.green);
        b *= (1.0f-tempSpec.transparent.blue);
            spec.transparent.subtract(r, g, b);
        f = Math.max(Math.max(r, g), b);
      }
      spec.roughness += f*tempSpec.roughness;
      spec.cloudiness += f*tempSpec.cloudiness;
      if (blendMode[i] == OVERLAY_ADD_BUMPS)
        tempSpec.bumpGrad.scale(param[fractParamIndex[i]]);
      else
        tempSpec.bumpGrad.scale(f);
      spec.bumpGrad.add(tempSpec.bumpGrad);
      rt -= r;
      gt -= g;
      bt -= b;
      ft -= f;
      if (rt <= 0.0f && gt <= 0.0f && bt <= 0.0f)
        return;
    }
  }
  
  /** Estimate the average surface properties by adding up the average properties of all 
     of the layers. */
  
  public void getAverageSpec(TextureSpec spec, double time, double param[])
  {
    float rt = 1.0f, gt = 1.0f, bt = 1.0f;
    double ft = 1.0;
    spec.diffuse.setRGB(0.0f, 0.0f, 0.0f);
    spec.specular.setRGB(0.0f, 0.0f, 0.0f);
    spec.hilight.setRGB(0.0f, 0.0f, 0.0f);
    spec.transparent.setRGB(1.0f, 1.0f, 1.0f);
    spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
    spec.roughness = spec.cloudiness = 0.0;
    spec.bumpGrad.set(0.0, 0.0, 0.0);
    TextureSpec tempSpec = new TextureSpec();
    double paramTemp[] = new double [maxParams];
    for (int i = 0; i < texture.length; i++)
    {
      double f = param[fractParamIndex[i]];
      if (numParams[i] > 0)
        for (int j = 0; j < numParams[i]; j++)
          paramTemp[j] = param[paramStartIndex[i]+j];
      texture[i].getAverageSpec(tempSpec, time, paramTemp);
      float r = rt*(float) f;
      float g = gt*(float) f;
      float b = bt*(float) f;
      spec.diffuse.add(r*tempSpec.diffuse.red, g*tempSpec.diffuse.green, b*tempSpec.diffuse.blue);
          spec.specular.add(r*tempSpec.specular.red, g*tempSpec.specular.green, b*tempSpec.specular.blue);
          spec.hilight.add(r*tempSpec.hilight.red, g*tempSpec.hilight.green, b*tempSpec.hilight.blue);
      spec.emissive.add(r*tempSpec.emissive.red, g*tempSpec.emissive.green, b*tempSpec.emissive.blue);
      if (blendMode[i] == BLEND)
      {
        spec.transparent.subtract(r*(1.0f-tempSpec.transparent.red), g*(1.0f-tempSpec.transparent.green), b*(1.0f-tempSpec.transparent.blue));
        f = ft*f;
      }
      else
      {
        r *= (1.0f-tempSpec.transparent.red);
        g *= (1.0f-tempSpec.transparent.green);
        b *= (1.0f-tempSpec.transparent.blue);
            spec.transparent.subtract(r, g, b);
        f = Math.max(Math.max(r, g), b);
      }
      spec.roughness += ft*tempSpec.roughness;
      spec.cloudiness += ft*tempSpec.cloudiness;
      if (blendMode[i] == OVERLAY_ADD_BUMPS)
        tempSpec.bumpGrad.scale(param[fractParamIndex[i]]);
      else
        tempSpec.bumpGrad.scale(f);
      spec.bumpGrad.add(tempSpec.bumpGrad);
      rt -= r;
      gt -= g;
      bt -= b;
      ft -= f;
      if (rt <= 0.0f && gt <= 0.0f && bt <= 0.0f)
        return;
    }
  }

  /** Determine the transparency by adding up all of the layers. */

  public void getTransparency(Vec3 pos, RGBColor trans, double angle, double size, double t, double param[])
  {
    float rt = 1.0f, gt = 1.0f, bt = 1.0f;
    RGBColor tempColor = new RGBColor();
    boolean front = (angle > 0.0);
    double paramTemp[] = new double [maxParams];
    trans.setRGB(1.0f, 1.0f, 1.0f);
    for (int i = 0; i < texture.length; i++)
    {
      if (!mapping[i].appliesToFace(front))
        continue;
      double f = param[fractParamIndex[i]];
      if (numParams[i] > 0)
        for (int j = 0; j < numParams[i]; j++)
          paramTemp[j] = param[paramStartIndex[i]+j];
      mapping[i].getTransparency(pos, tempColor, angle, size, t, paramTemp);
      float r = rt*(float) f;
      float g = gt*(float) f;
      float b = bt*(float) f;
      if (blendMode[i] == BLEND)
        trans.subtract(r*(1.0f-tempColor.red), g*(1.0f-tempColor.green), b*(1.0f-tempColor.blue));
      else
      {
        r *= (1.0f-tempColor.red);
        g *= (1.0f-tempColor.green);
        b *= (1.0f-tempColor.blue);
        trans.subtract(r, g, b);
      }
      rt -= r;
      gt -= g;
      bt -= b;
      if (rt <= 0.0f && gt <= 0.0f && bt <= 0.0f)
        return;
    }
  }
  
  /** Determine the displacement height by adding up all of the layers. */

  public double getDisplacement(Vec3 pos, double size, double t, double param[])
  {
    double ft = 1.0, height = 0.0, temp;
    RGBColor tempColor = new RGBColor();
    double paramTemp[] = new double [maxParams];
    for (int i = 0; i < texture.length; i++)
    {
      double f = param[fractParamIndex[i]];
      if (numParams[i] > 0)
        for (int j = 0; j < numParams[i]; j++)
          paramTemp[j] = param[paramStartIndex[i]+j];
      temp = mapping[i].getDisplacement(pos, size, t, paramTemp);
      f *= ft;
      if (blendMode[i] == OVERLAY_BLEND_BUMPS)
      {
        mapping[i].getTransparency(pos, tempColor, 1.0, size, t, paramTemp);
        float min = tempColor.red;
        if (min > tempColor.green)
          min = tempColor.green;
        if (min > tempColor.blue)
          min = tempColor.blue;
        f *= ft*(1.0f-min);
      }
      if (blendMode[i] != LayeredMapping.OVERLAY_ADD_BUMPS)
        ft -= f;
      height += temp*f;
      if (ft <= 0.0)
        return height;
    }
    return height;
  }

  /** Get the LayeredTexture object this mapping is associated with */
  
  public Texture getTexture()
  {
    return theTexture;
  }

  public Object3D getObject()
  {
    return theObject;
  }

  /** Create a new TextureMapping which is identical to this one. */
  
  public TextureMapping duplicate()
  {
    return duplicate(theObject, theTexture);
  }
  
  /** Create a new TextureMapping which is identical to this one, but for a
     different Texture. */
  
  public TextureMapping duplicate(Object3D obj, Texture tex)
  {
    LayeredMapping map = new LayeredMapping(obj, null);
    int layers = texture.length;

    map.theTexture = new LayeredTexture(map);
    map.texture = new Texture [layers];
    map.mapping = new TextureMapping [layers];
    map.blendMode = new int [layers];
    map.fractParamID = new int [layers];
    for (int i = 0; i < layers; i++)
      {
        map.texture[i] = texture[i];
        map.mapping[i] = mapping[i].duplicate();
        map.blendMode[i] = blendMode[i];
        map.fractParamID[i] = fractParamID[i];
      }
    map.getParameters();
    return map;
  }
  
  /** Make this mapping identical to another one. */
  
  public void copy(TextureMapping theMap)
  {
    LayeredMapping map = (LayeredMapping) theMap;
    int layers = map.texture.length;

    texture = new Texture [layers];
    mapping = new TextureMapping [layers];
    blendMode = new int [layers];
    fractParamID = new int [layers];
    for (int i = 0; i < layers; i++)
      {
        texture[i] = map.texture[i];
        mapping[i] = map.mapping[i].duplicate();
        blendMode[i] = map.blendMode[i];
        fractParamID[i] = map.fractParamID[i];
      }
    getParameters();
  }
  
  /** There is no editing panel for layered mappings, since this is handled directly by the
      object texture dialog. */

  public Widget getEditingPanel(Object3D obj, MaterialPreviewer preview)
  {
    return null;
  }
}
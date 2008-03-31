/* Copyright (C) 2002,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.translators;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;

/** OBJImporter implements the importing of OBJ files. */

public class OBJImporter
{
  public static void importFile(BFrame parent)
  {
    BFileChooser bfc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("importOBJ"));
    if (ArtOfIllusion.getCurrentDirectory() != null)
      bfc.setDirectory(new File(ArtOfIllusion.getCurrentDirectory()));
    if (!bfc.showDialog(parent))
      return;
    File f = bfc.getSelectedFile();
    ArtOfIllusion.setCurrentDirectory(bfc.getDirectory().getAbsolutePath());
    String objName = f.getName();
    if (objName.lastIndexOf('.') > 0)
      objName = objName.substring(0, objName.lastIndexOf('.'));
    
    // Create a scene to add objects to.
    
    Scene theScene = new Scene();
    CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    ObjectInfo info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    theScene.addObject(info, null);
    info = new ObjectInfo(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 0.8f), coords.duplicate(), "Light 1");
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    theScene.addObject(info, null);

    // Open the file and read the contents.
    
    Hashtable groupTable = new Hashtable(), textureTable = new Hashtable();
    Vector vertex = new Vector(), normal = new Vector(), texture = new Vector(), face[] = new Vector [] {new Vector()};
    groupTable.put("default", face[0]);
    int lineno = 0, smoothingGroup = -1;
    String currentTexture = null;
    VertexInfo vertIndex[] = new VertexInfo [3];
    double val[] = new double [3];
    double min[] = new double [] {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
    double max[] = new double [] {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
    String s;
    BufferedReader in = null;
    try
      {
        in = new BufferedReader(new FileReader(f));
        while ((s = in.readLine()) != null)
          {
            lineno++;
            if (s.startsWith("#"))
              continue;
            if (s.endsWith("\\"))
            {
              String s2;
              while (s.endsWith("\\") && (s2 = in.readLine()) != null)
                s = s.substring(0, s.length()-1)+s2;
            }
            String fields[] = breakLine(s);
            if (fields.length == 0)
              continue;
            if ("v".equals(fields[0]) && fields.length == 4)
              {
                // Read in a vertex.
                
                for (int i = 0; i < 3; i++)
                  {
                    try
                      {
                        val[i] = new Double(fields[i+1]).doubleValue();
                        if (val[i] < min[i])
                          min[i] = val[i];
                        if (val[i] > max[i])
                          max[i] = val[i];
                      }
                    catch (NumberFormatException ex)
                      {
                        throw new Exception("Illegal value '"+fields[i+1]+
                          "' found in line "+lineno+".");
                      }
                  }
                vertex.addElement(new Vec3(val[0], val[1], val[2]));
              }
            else if ("vn".equals(fields[0]) && fields.length == 4)
              {
                // Read in a vertex normal.
                
                for (int i = 0; i < 3; i++)
                  {
                    try
                      {
                        val[i] = new Double(fields[i+1]).doubleValue();
                      }
                    catch (NumberFormatException ex)
                      {
                        throw new Exception("Illegal value '"+fields[i+1]+
                          "' found in line "+lineno+".");
                      }
                  }
                normal.addElement(new Vec3(val[0], val[1], val[2]));
              }
            else if ("vt".equals(fields[0]) && fields.length > 1)
              {
                // Read in a texture vertex.
                
                for (int i = 0; i < 3; i++)
                  {
                    try
                      {
                        if (i < fields.length-1)
                          val[i] = new Double(fields[i+1]).doubleValue();
                        else
                          val[i] = 0.0;
                      }
                    catch (NumberFormatException ex)
                      {
                        throw new Exception("Illegal value '"+fields[i+1]+
                          "' found in line "+lineno+".");
                      }
                  }
                texture.addElement(new Vec3(val[0], val[1], val[2]));
              }
            else if ("f".equals(fields[0]))
              {
                if (vertIndex.length != fields.length-1)
                  vertIndex = new VertexInfo [fields.length-1];
                for (int i = 0; i < vertIndex.length; i++)
                  vertIndex[i] = parseVertexSpec(fields[i+1], vertex, texture, normal, lineno);
                for (int i = 0; i < face.length; i++)
                  {
                    if (fields.length == 4)
                      {
                        // Add a triangular face.
                        
                        face[i].addElement(new FaceInfo(vertIndex[0], vertIndex[1], vertIndex[2], smoothingGroup, currentTexture));
                      }
                    else if (fields.length == 5)
                      {
                        // Add a 4 sided face.
                        
                        face[i].addElement(new FaceInfo(vertIndex[0], vertIndex[1], vertIndex[2], smoothingGroup, currentTexture));
                        face[i].addElement(new FaceInfo(vertIndex[2], vertIndex[3], vertIndex[0], smoothingGroup, currentTexture));
                      }
                    else if (fields.length > 5)
                      {
                        // Add a face with more than 4 sides.
                        
                        int step, start;
                        for (step = 1; 2*step < vertIndex.length; step *= 2)
                          {
                            for (start = 0; start+2*step < vertIndex.length; start += 2*step)
                              face[i].addElement(new FaceInfo(vertIndex[start], vertIndex[start+step], vertIndex[start+2*step], smoothingGroup, currentTexture));
                            if (start+step < vertIndex.length)
                              face[i].addElement(new FaceInfo(vertIndex[start], vertIndex[start+step], vertIndex[0], smoothingGroup, currentTexture));
                          }
                      }
                  }
              }
            else if ("s".equals(fields[0]))
              {
                // Set the smoothing group.
                
                if (fields.length == 1 || "off".equalsIgnoreCase(fields[1]))
                  {
                    smoothingGroup = 0;
                    continue;
                  }
                try
                  {
                    smoothingGroup = Integer.parseInt(fields[1]);
                  }
                catch (NumberFormatException ex)
                  {
                    throw new Exception("Illegal value '"+fields[1]+
                      "' found in line "+lineno+".");
                  }
              }
            else if ("g".equals(fields[0]))
              {
                // Set the current group or groups.
                
                face = new Vector [fields.length-1];
                for (int i = 0; i < face.length; i++)
                  {
                    face[i] = (Vector) groupTable.get(fields[i+1]);
                    if (face[i] == null)
                      {
                        face[i] = new Vector();
                        groupTable.put(fields[i+1], face[i]);
                      }
                  }
              }
            else if ("usemtl".equals(fields[0]) && fields.length > 1)
              {
                // Set the current texture.
                
                currentTexture = fields[1];
              }
            else if ("mtllib".equals(fields[0]))
              {
                // Load one or more texture libraries.
                
                for (int i = 1; i < fields.length; i++)
                  parseTextures(fields[i], bfc.getDirectory(), textureTable);
              }
          }

        // If no mtl file was specified, but there is one with the same name is the obj file,
        // go ahead and load it.

        if (textureTable.size() == 0)
        {
          File defaultMtl = new File(bfc.getDirectory(), objName+".mtl");
          if (defaultMtl.isFile())
            parseTextures(objName+".mtl", bfc.getDirectory(), textureTable);
        }
        
        // If necessary, rescale the vertices to make the object an appropriate size.
        
        double maxSize = Math.max(Math.max(max[0]-min[0], max[1]-min[1]), max[2]-min[2]);
        double scale = Math.pow(10.0, -Math.floor(Math.log(maxSize)/Math.log(10.0)));
        for (int i = 0; i < vertex.size(); i++)
          ((Vec3) vertex.elementAt(i)).scale(scale);
        
        // Create a triangle mesh for each group.
        
        Enumeration keys = groupTable.keys();
        Hashtable realizedTextures = new Hashtable();
        Hashtable imageMaps = new Hashtable();
        while (keys.hasMoreElements())
          {
            String group = (String) keys.nextElement();
            Vector groupFaces = (Vector) groupTable.get(group);
            if (groupFaces.size() == 0)
              continue;
            
            // Find which vertices are used by faces in this group.
            
            int realIndex[] = new int [vertex.size()];
            for (int i = 0; i < realIndex.length; i++)
              realIndex[i] = -1;
            int fc[][] = new int [groupFaces.size()][], numVert = 0;
            for (int i = 0; i < fc.length; i++)
              {
                FaceInfo fi = (FaceInfo) groupFaces.elementAt(i);
                for (int j = 0; j < 3; j++)
                  if (realIndex[fi.getVertex(j).vert] == -1)
                    realIndex[fi.getVertex(j).vert] = numVert++;
                fc[i] = new int [] {realIndex[fi.v1.vert], realIndex[fi.v2.vert], realIndex[fi.v3.vert]};
              }
            
            // Build the list of vertices and center them.
            
            Vec3 vert[] = new Vec3 [numVert], center = new Vec3();
            for (int i = 0; i < realIndex.length; i++)
              if (realIndex[i] > -1)
                {
                  vert[realIndex[i]] = (Vec3) vertex.elementAt(i);
                  center.add(vert[realIndex[i]]);
                }
            center.scale(1.0/vert.length);
            for (int i = 0; i < vert.length; i++)
              vert[i] = vert[i].minus(center);
            coords = new CoordinateSystem(center, Vec3.vz(), Vec3.vy());
            info = new ObjectInfo(new TriangleMesh(vert, fc), coords, ("default".equals(group) ? objName : group));
            info.addTrack(new PositionTrack(info), 0);
            info.addTrack(new RotationTrack(info), 1);
            
            // Find the smoothness values for the edges.
            
            TriangleMesh.Edge edge[] = ((TriangleMesh) info.getObject()).getEdges();
            for (int i = 0; i < edge.length; i++)
              {
                if (edge[i].f2 == -1)
                  continue;
                FaceInfo f1 = (FaceInfo) groupFaces.elementAt(edge[i].f1);
                FaceInfo f2 = (FaceInfo) groupFaces.elementAt(edge[i].f2);
                if (f1.smoothingGroup == 0 || f1.smoothingGroup != f2.smoothingGroup)
                  {
                    // They are in different smoothing groups.

                    edge[i].smoothness = 0.0f;
                    continue;
                  }
                
                // Find matching vertices and compare their normals.
                
                for (int j = 0; j < 3; j++)
                  for (int k = 0; k < 3; k++)
                    if (f1.getVertex(j).vert == f2.getVertex(k).vert)
                      {
                        int n1 = f1.getVertex(j).norm;
                        int n2 = f2.getVertex(k).norm;
                        if (n1 != n2 && ((Vec3) normal.elementAt(n1)).distance((Vec3) normal.elementAt(n2)) > 1e-10)
                          edge[i].smoothness = 0.0f;
                        break;
                      }
              }
            
            // Set the texture.  For the moment, assume a single texture per group.  In the future, this could possibly
            // be improved to deal correctly with per-face textures.
            
            String texName = ((FaceInfo) groupFaces.elementAt(0)).texture;
            if (texName != null && textureTable.get(texName) != null)
              {
                Texture tex = (Texture) realizedTextures.get(texName);
                if (tex == null)
                  {
                    tex = createTexture((TextureInfo) textureTable.get(texName), theScene, bfc.getDirectory(), imageMaps, parent);
                    realizedTextures.put(texName, tex);
                  }
                if (tex instanceof Texture2D)
                  {
                    // Set the UV coordinates.
                    
                    UVMapping map = new UVMapping(info.getObject(), tex);
                    info.setTexture(tex, map);
                    Vec2 uv[] = new Vec2 [numVert];
                    boolean needPerFace = false;
                    for (int j = 0; j < groupFaces.size() && !needPerFace; j++)
                      {
                        FaceInfo fi = (FaceInfo) groupFaces.elementAt(j);
                        for (int k = 0; k < 3; k++)
                          {
                            VertexInfo vi = fi.getVertex(k);
                            Vec3 texCoords = (Vec3) (vi.tex < texture.size() ? texture.elementAt(vi.tex) : vertex.elementAt(vi.vert));
                            Vec2 tc = new Vec2(texCoords.x, texCoords.y);
                            if (uv[realIndex[vi.vert]] != null && !uv[realIndex[vi.vert]].equals(tc))
                              needPerFace = true;
                            uv[realIndex[vi.vert]] = tc;
                          }
                      }
                    if (needPerFace)
                    {
                      // Different faces have different texture coordinates for the same vertex,
                      // so we need to use per-face-vertex coordinates.
                      
                      Vec2 uvf[][] = new Vec2 [groupFaces.size()][3];
                      for (int j = 0; j < groupFaces.size(); j++)
                      {
                        FaceInfo fi = (FaceInfo) groupFaces.elementAt(j);
                        for (int k = 0; k < 3; k++)
                          {
                            VertexInfo vi = fi.getVertex(k);
                            Vec3 texCoords = (Vec3) (vi.tex < texture.size() ? texture.elementAt(vi.tex) : vertex.elementAt(vi.vert));
                            uvf[j][k] = new Vec2(texCoords.x, texCoords.y);
                          }
                      }
                      map.setFaceTextureCoordinates((TriangleMesh) info.getObject(), uvf);
                    }
                    else
                      map.setTextureCoordinates(info.getObject(), uv);
                  }
                else
                  info.setTexture(tex, tex.getDefaultMapping(info.getObject()));
              }
            theScene.addObject(info, null);
          }
      }
    catch (Exception ex)
      {
        try
          {
            in.close();
          }
        catch (Exception ex2)
          {
          }
        new BStandardDialog("", new String [] {Translate.text("errorLoadingFile"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(parent);
        return;
      }
    ArtOfIllusion.newWindow(theScene);
    return;
  }
  
  /** Separate a line into pieces divided by whitespace. */
  
  private static String [] breakLine(String line)
  {
    StringTokenizer st = new StringTokenizer(line);
    Vector v = new Vector();
    
    while (st.hasMoreTokens())
      v.addElement(st.nextToken());
    String result[] = new String [v.size()];
    v.copyInto(result);
    return result;
  }
  
  /** Parse the specification for a vertex and return the index of the vertex
      to use. */
  
  private static VertexInfo parseVertexSpec(String spec, Vector vertex, Vector texture, Vector normal, int lineno) throws Exception
  {
    VertexInfo info = new VertexInfo();
    StringTokenizer st = new StringTokenizer(spec, "/", true);
    info.tex = info.norm = Integer.MAX_VALUE;
    int i = 0;
    while (st.hasMoreTokens())
      {
        String value = st.nextToken();
        if ("/".equals(value))
          {
            i++;
            continue;
          }
        try
          {
            int index = Integer.parseInt(value);
            int total = 0;
            if (i == 0)
              total = vertex.size();
            else if (i == 1)
              total = texture.size();
            else
              total = normal.size();
            if (index < 0)
              index += total;
            else
              index--;
            if (i == 0)
              info.vert = index;
            else if (i == 1)
              info.tex = index;
            else
              info.norm = index;
          }
        catch (NumberFormatException ex)
          {
            throw new Exception("Illegal value '"+spec+"' found in line "+lineno+".");
          }
      }
    if (info.tex == Integer.MAX_VALUE)
      info.tex = info.vert;
    if (info.norm == Integer.MAX_VALUE)
      info.norm = info.vert;
    return info;
  }
  
  /** Parse the contents of a .mtl file and add TextureInfo object to a hashtable. */
  
  private static void parseTextures(String file, File baseDir, Hashtable textures) throws Exception
  {
    File f = new File(baseDir, file);
    if (!f.isFile())
      f = new File(file);
    if (!f.isFile())
    {
      new BStandardDialog("Error Importing File", "Cannot locate material file '"+file+"'.", BStandardDialog.ERROR).showMessageDialog(null);
      return;
    }
    BufferedReader in = new BufferedReader(new FileReader(f));
    String line;
    TextureInfo currentTexture = null;
    while ((line = in.readLine()) != null)
      {
        try
          {
            if (line.startsWith("#"))
              continue;
            String fields[] = breakLine(line);
            if (fields.length == 0)
              continue;
            if ("newmtl".equals(fields[0]))
              {
                // This is the start of a new texture.
                
                currentTexture = null;
                if (fields.length == 1 || textures.get(fields[1]) != null)
                  continue;
                currentTexture = new TextureInfo();
                currentTexture.name = fields[1];
                textures.put(fields[1], currentTexture);
              }
            if (currentTexture == null || fields.length < 2)
              continue;
            if ("Kd".equals(fields[0]))
              currentTexture.diffuse = parseColor(fields);
            else if ("Ka".equals(fields[0]))
              currentTexture.ambient = parseColor(fields);
            else if ("Ks".equals(fields[0]))
              currentTexture.specular = parseColor(fields);
            else if ("d".equals(fields[0]) || "Tr".equals(fields[0]))
              currentTexture.transparency = 1.0-(new Double(fields[1]).doubleValue());
            else if ("Ns".equals(fields[0]))
              currentTexture.shininess = new Double(fields[1]).doubleValue();
            else if ("map_Kd".equals(fields[0]))
              currentTexture.diffuseMap = fields[1];
            else if ("map_Ka".equals(fields[0]))
              currentTexture.ambientMap = fields[1];
            else if ("map_Ks".equals(fields[0]))
              currentTexture.specularMap = fields[1];
            else if ("map_d".equals(fields[0]))
              currentTexture.transparentMap = fields[1];
            else if ("map_Bump".equals(fields[0]))
              currentTexture.bumpMap = fields[1];
          }
        catch (Exception ex)
          {
            in.close();
            throw new Exception("Illegal line '"+line+"' found in file '"+file+"'.");
          }
      }
    in.close();
  }
  
  /** Create a texture from a TextureInfo and add it to the scene. */
  
  private static Texture createTexture(TextureInfo info, Scene scene, File baseDir, Hashtable imageMaps, BFrame parent) throws Exception
  {
    info.resolveColors();
    ImageMap diffuseMap = loadMap(info.diffuseMap, scene, baseDir, imageMaps, parent);
    ImageMap specularMap = loadMap(info.specularMap, scene, baseDir, imageMaps, parent);
    ImageMap transparentMap = loadMap(info.transparentMap, scene, baseDir, imageMaps, parent);
    ImageMap bumpMap = loadMap(info.bumpMap, scene, baseDir, imageMaps, parent);
    RGBColor transparentColor =  new RGBColor(info.transparency, info.transparency, info.transparency);
    if (diffuseMap == null && specularMap == null && transparentMap == null && bumpMap == null)
      {
        // Create a uniform texture.
        
        UniformTexture tex = new UniformTexture();
        tex.diffuseColor = info.diffuse.duplicate();
        tex.specularColor = info.specular.duplicate();
        tex.transparentColor = transparentColor;
        tex.shininess = (float) info.specularity;
        tex.specularity = 0.0f;
        tex.roughness = info.roughness;
        tex.setName(info.name);
        scene.addTexture(tex);
        return tex;
      }
    else
      {
        // Create an image mapped texture.

        ImageMapTexture tex = new ImageMapTexture();
        tex.diffuseColor = (diffuseMap == null ? new ImageOrColor(info.diffuse) : new ImageOrColor(info.diffuse, diffuseMap));
        tex.specularColor = (specularMap == null ? new ImageOrColor(info.specular) : new ImageOrColor(info.specular, specularMap));
        tex.transparentColor = (transparentMap == null ? new ImageOrColor(transparentColor) : new ImageOrColor(transparentColor, transparentMap));
        if (bumpMap != null)
          tex.bump = new ImageOrValue(1.0f, bumpMap, 0);
        tex.shininess = new ImageOrValue((float) info.specularity);
        tex.specularity = new ImageOrValue(0.0f);
        tex.roughness = new ImageOrValue((float) info.roughness);
        tex.tileX = tex.tileY = true;
        tex.mirrorX = tex.mirrorY = false;
        tex.setName(info.name);
        scene.addTexture(tex);
        return tex;
      }
  }
  
  /** Return the image map corresponding to the specified filename, and add it to the scene. */
  
  private static ImageMap loadMap(String name, Scene scene, File baseDir, Hashtable imageMaps, BFrame parent) throws Exception
  {
    if (name == null)
      return null;
    ImageMap map = (ImageMap) imageMaps.get(name);
    if (map != null)
      return map;
    File f = new File(baseDir, name);
    if (!f.isFile())
      f = new File(name);
    if (!f.isFile())
      throw new Exception("Cannot locate image map file '"+name+"'.");
    try
      {
        map = ImageMap.loadImage(f);
      }
    catch (InterruptedException ex)
      {
        throw new Exception("Unable to load image map file '"+f.getAbsolutePath()+"'.");
      }
    scene.addImage(map);
    imageMaps.put(name, map);
    return map;
  }
  
  /** Parse the specification for a color. */
  
  private static RGBColor parseColor(String fields[]) throws NumberFormatException
  {
    if (fields.length < 4)
      return null;
    return new RGBColor(new Double(fields[1]).doubleValue(),
        new Double(fields[2]).doubleValue(),
        new Double(fields[3]).doubleValue());
  }
  
  /** Inner class for storing information about a vertex of a face. */
  
  private static class VertexInfo
  {
    public int vert, norm, tex;
  }
  
  /** Inner class for storing information about a face. */
  
  private static class FaceInfo
  {
    public VertexInfo v1, v2, v3;
    public int smoothingGroup;
    public String texture;
    
    public FaceInfo(VertexInfo v1, VertexInfo v2, VertexInfo v3, int smoothingGroup, String texture)
    {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.smoothingGroup = smoothingGroup;
      this.texture = texture;
    }
    
    public VertexInfo getVertex(int which)
    {
      switch (which)
        {
          case 0: return v1;
          case 1: return v2;
          default: return v3;
        }
    }
  }
  
  /** Inner class for storing information about a texture in a .mtl file. */
  
  private static class TextureInfo
  {
    public String name;
    public RGBColor ambient, diffuse, specular;
    public double shininess, transparency, specularity, roughness;
    public String ambientMap, diffuseMap, specularMap, transparentMap, bumpMap;
    
    /** This should be called once, after the TextureInfo is created but before it is actually used.  It converts from the
        representation used by .obj files to the one used by Art of Illusion. */
        
    public void resolveColors()
    {
      if (diffuse == null)
        diffuse = new RGBColor(0.0, 0.0, 0.0);
      if (ambient == null)
        ambient = new RGBColor(0.0, 0.0, 0.0);
      if (specular == null)
        specular = new RGBColor(0.0, 0.0, 0.0);
      else
        specularity = 1.0;
      diffuse.scale(1.0-transparency);
      specular.scale(1.0-transparency);
      roughness = 1.0-(shininess-1.0)/128.0;
      if (roughness > 1.0)
        roughness = 1.0;
      checkColorRange(ambient);
      checkColorRange(diffuse);
      checkColorRange(specular);
    }
    
    /** Make sure that the components of a color are all between 0 and 1. */
    
    private void checkColorRange(RGBColor c)
    {
      float r = c.getRed(), g = c.getGreen(), b = c.getBlue();
      if (r < 0.0f) r = 0.0f;
      if (r > 1.0f) r = 1.0f;
      if (g < 0.0f) g = 0.0f;
      if (g > 1.0f) g = 1.0f;
      if (b < 0.0f) b = 0.0f;
      if (b > 1.0f) b = 1.0f;
      c.setRGB(r, g, b);
    }
  }
}
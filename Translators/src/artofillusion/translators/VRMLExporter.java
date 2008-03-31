/* Copyright (C) 1999-2007 by Peter Eastman
   Some parts copyright (C) 2005 by Nik Trevallyn-Jones

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.translators;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/** VRMLExporter contains the actual routines for exporting VRML files. */

public class VRMLExporter
{
    public static final String matchId = " \"'#,.\\[]{}" +
        "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u000e" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019" +
        "\u001a\u001b\u001c\u001d\u001e\u001f\u007f";

    public static final String replace = "_______()()";
    public static final String illegalFirst = "+-0123456789";

  public static void exportFile(BFrame parent, Scene theScene)
  {
    // Display a dialog box with options on how to export the scene.

    ValueField errorField = new ValueField(0.05, ValueField.POSITIVE);
    final ValueField widthField = new ValueField(200.0, ValueField.INTEGER+ValueField.POSITIVE);
    final ValueField heightField = new ValueField(200.0, ValueField.INTEGER+ValueField.POSITIVE);
    final ValueSlider qualitySlider = new ValueSlider(0.0, 1.0, 100, 0.5);
    final BCheckBox texBox = new BCheckBox(Translate.text("createImageFilesForTextures"), false);
    BCheckBox compressBox = new BCheckBox(Translate.text("compressOutputFile"), true);
    BCheckBox smoothBox = new BCheckBox(Translate.text("subdivideSmoothMeshes"), true);
    BComboBox exportChoice = new BComboBox(new String [] {
      Translate.text("exportWholeScene"),
      Translate.text("selectedObjectsOnly")
    });
    texBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        widthField.setEnabled(texBox.getState());
        heightField.setEnabled(texBox.getState());
        qualitySlider.setEnabled(texBox.getState());
      }
    });
    texBox.dispatchEvent(new ValueChangedEvent(texBox));
    ComponentsDialog dlg;
    if (theScene.getSelection().length > 0)
      dlg = new ComponentsDialog(parent, Translate.text("exportToVRML"),
          new Widget [] {exportChoice, errorField, compressBox, smoothBox, texBox, Translate.label("imageSizeForTextures"), widthField, heightField, qualitySlider},
          new String [] {null, Translate.text("maxSurfaceError"), null, null, null, null, Translate.text("Width"), Translate.text("Height"), Translate.text("imageQuality")});
    else
      dlg = new ComponentsDialog(parent, Translate.text("exportToVRML"),
          new Widget [] {errorField, compressBox, smoothBox, texBox, Translate.label("imageSizeForTextures"), widthField, heightField, qualitySlider},
          new String [] {Translate.text("maxSurfaceError"), null, null, null, null, Translate.text("Width"), Translate.text("Height"), Translate.text("imageQuality")});
    if (!dlg.clickedOk())
      return;

    // Ask the user to select the output file.

    BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("exportToVRML"));
    if (compressBox.getState())
      fc.setSelectedFile(new File("Untitled.wrz"));
    else
      fc.setSelectedFile(new File("Untitled.wrl"));
    if (ArtOfIllusion.getCurrentDirectory() != null)
      fc.setDirectory(new File(ArtOfIllusion.getCurrentDirectory()));
    if (!fc.showDialog(parent))
      return;
    File dir = fc.getDirectory();
    File f = fc.getSelectedFile();
    String name = f.getName();
    String baseName = (name.endsWith(".wrl") || name.endsWith(".wrz") ? name.substring(0, name.length()-4) : name);
    ArtOfIllusion.setCurrentDirectory(dir.getAbsolutePath());

    // Create the output files.

    try
    {
      TextureImageExporter textureExporter = null;
      if (texBox.getState())
      {
        textureExporter = new TextureImageExporter(dir, baseName, (int) (100*qualitySlider.getValue()),
            TextureImageExporter.DIFFUSE, (int) widthField.getValue(), (int) heightField.getValue());
        boolean wholeScene = (exportChoice.getSelectedIndex() == 0);
        for (int i = 0; i < theScene.getNumObjects(); i++)
        {
          ObjectInfo info = theScene.getObject(i);
          if (!wholeScene && !info.selected)
            continue;
          textureExporter.addObject(info);
        }
        textureExporter.saveImages();
      }
      OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
      if (compressBox.getState())
        out = new GZIPOutputStream(out);
      writeScene(theScene, out, exportChoice.getSelectedIndex() == 0, errorField.getValue(), smoothBox.getState(), textureExporter);
      out.close();
    }
    catch (Exception ex)
      {
        System.out.println("VRMLExporter.exportFile: " + ex);
        ex.printStackTrace(System.out);
        new BStandardDialog("", new String [] {Translate.text("errorExportingScene"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(parent);
      }
  }

  /** Write out the scene in VRML format to the specified OutputStream.  The other parameters
      correspond to the options in the dialog box displayed by exportFile(). */

  private static void writeScene(Scene theScene, OutputStream os, boolean wholeScene, double tol, boolean smooth, TextureImageExporter textureExporter)
  {
    PrintWriter out = new PrintWriter(os);
    int i, selected[] = theScene.getSelection();
    RGBColor color;

    // Write the header information.

    write("#VRML V2.0 utf8", out, 0);
    write("#Produced by Art of Illusion, " + (new Date()).toString(), out, 0);

    // If we are exporting the whole scene, then write the environment information.

    if (wholeScene)
      {
        // Turn off the headlight.

        write("NavigationInfo {", out, 0);
        write("headlight FALSE", out, 1);
        write("}", out, 0);

        // Set the background color.

        color = theScene.getEnvironmentColor();
        write("Background {", out, 0);
        write("skyColor "+color.getRed()+" "+color.getGreen()+" "+color.getBlue(), out, 1);
        write("}", out, 0);

        // Set the ambient light.

        color = theScene.getAmbientColor();
        write("PointLight {", out, 0);
        write("color "+color.getRed()+" "+color.getGreen()+" "+color.getBlue(), out, 1);
        write("intensity 0", out, 1);
        write("ambientIntensity 1", out, 1);
        write("radius 1e15", out, 1);
        write("}", out, 0);

        // Add fog, if appropriate.

        if (theScene.getFogState())
          {
            color = theScene.getFogColor();
            write("Fog {", out, 0);
            write("color "+color.getRed()+" "+color.getGreen()+" "+color.getBlue(), out, 1);
            write("fogType \"EXPONENTIAL\"", out, 1);
            write("visibilityRange "+2.0*theScene.getFogDistance(), out, 1);
            write("}", out, 0);
          }
      }

    // Write the objects in the scene.

    if (wholeScene)
      for (i = 0; i < theScene.getNumObjects(); i++)
        writeObject(theScene.getObject(i), null, out, tol, smooth, 0, theScene, textureExporter);
    else
      for (i = 0; i < selected.length; i++)
        writeObject(theScene.getObject(selected[i]), null, out, tol, smooth, 0, theScene, textureExporter);
    out.flush();
  }

  /** Write a single line to the PrintWriter, indented by the specified number of spaces. */

  private static void write(String str, PrintWriter out, int indent)
  {
    for (int i = 0; i < indent; i++)
      out.print(" ");
    out.print(str);
    out.print("\r\n");
  }

  /**
   * Write a single object to the PrintWriter.
   *
   * @param info the ObjectInfo representing the object to write
   * @param parent the parent ObjectInfo, if <code>info</code> is a child,
   *        otherwise <i>null</i>.
   * @param out the PrintWriter to write to
   * @param tol the tolerance to use when generating object meshes
   * @param smooth specifies whether to smooth triangle meshes
   * @param indent the number of spaces to place at the beginning of each line
   * @param theScene the Scene being exported
   * @param textureExporter the exporter to use for textures
   */

  private static void writeObject(ObjectInfo info, ObjectInfo parent, PrintWriter out, double tol, boolean smooth, int indent, Scene theScene, TextureImageExporter textureExporter)
  {
    if (info.getParent() != null && info.getParent() != parent)
      return; // someone else's child - skip

    CoordinateSystem coords = info.getCoords();
    Object3D obj = info.getObject();
    Vec3 orig = coords.getOrigin(), size = info.getBounds().getSize(), axis = new Vec3(0.0, 0.0, 0.0);
    double rot[] = new double [4], ratio = 0.0;
    double pos[] = new double [3], scale[] = new double [3];
    String name = translate(info.getName(), 0, 1, matchId, replace) +
        translate(info.getName(), 1, -1, matchId, replace);
    if (name.length() > 0 && illegalFirst.indexOf(name.charAt(0)) > 0)
      name = '_'+name;

    if (obj instanceof SceneCamera)
      {
        coords = coords.duplicate();
        coords.setOrientation(info.getCoords().getZDirection().times(-1.0), info.getCoords().getUpDirection().times(1.0));
      }
    rot[3] = coords.getAxisAngleRotation(axis);

    // Limit the values to six decimal places.

    pos[0] = Math.round(orig.x*1e6)/1e6;
    pos[1] = Math.round(orig.y*1e6)/1e6;
    pos[2] = Math.round(orig.z*1e6)/1e6;
    scale[0] = Math.round(size.x*1e6)/1e6;
    scale[1] = Math.round(size.y*1e6)/1e6;
    scale[2] = Math.round(size.z*1e6)/1e6;
    rot[0] = Math.round(axis.x*1e6)/1e6;
    rot[1] = Math.round(axis.y*1e6)/1e6;
    rot[2] = Math.round(axis.z*1e6)/1e6;
    rot[3] = Math.round(rot[3]*1e6)/1e6;

    if (obj instanceof DirectionalLight)
      {
        RGBColor color = ((Light) obj).getColor();
        boolean ambient = (((Light) obj).getType() == Light.TYPE_AMBIENT);
        Vec3 dir = coords.getZDirection();
        write("DirectionalLight {", out, indent);
        write("direction "+dir.x+" "+dir.y+" "+dir.z, out, indent+1);
        write("color "+color.getRed()+" "+color.getGreen()+" "+color.getBlue(), out, indent+1);
        if (ambient)
          {
            write("ambientIntensity "+((Light) obj).getIntensity(), out, indent+1);
            write("intensity 0", out, indent+1);
          }
        else
          write("intensity "+((Light) obj).getIntensity(), out, indent+1);
        write("}", out, indent);
        return;
      }
    if (obj instanceof SpotLight)
      {
        RGBColor color = ((Light) obj).getColor();
        float decay = ((Light) obj).getDecayRate();
        boolean ambient = (((Light) obj).getType() == Light.TYPE_AMBIENT);
        Vec3 dir = coords.getZDirection();
        double inner = Math.acos(Math.pow(0.9, 1.0/((SpotLight) obj).getExponent()));
        double outer = Math.acos(Math.pow(0.1, 1.0/((SpotLight) obj).getExponent()));
        double cutoff = ((SpotLight) obj).getAngle()/2.0;
        if (cutoff < outer)
          outer = cutoff;
        write("SpotLight {", out, indent);
        write("location "+pos[0]+" "+pos[1]+" "+pos[2], out, indent+1);
        write("direction "+dir.x+" "+dir.y+" "+dir.z, out, indent+1);
        write("cutOffAngle "+outer, out, indent+1);
        if (inner < outer)
          write("beamWidth "+inner, out, indent+1);
        write("color "+color.getRed()+" "+color.getGreen()+" "+color.getBlue(), out, indent+1);
        if (ambient)
          {
            write("ambientIntensity "+((Light) obj).getIntensity(), out, indent+1);
            write("intensity 0", out, indent+1);
          }
        else
          write("intensity "+((Light) obj).getIntensity(), out, indent+1);
        write("attenuation 1 "+decay+" "+(decay*decay), out, indent+1);
        write("}", out, indent);
        return;
      }
    if (obj instanceof PointLight)
      {
        RGBColor color = ((Light) obj).getColor();
        float decay = ((Light) obj).getDecayRate();
        boolean ambient = (((Light) obj).getType() == Light.TYPE_AMBIENT);
        write("PointLight {", out, indent);
        write("location "+pos[0]+" "+pos[1]+" "+pos[2], out, indent+1);
        write("color "+color.getRed()+" "+color.getGreen()+" "+color.getBlue(), out, indent+1);
        if (ambient)
          {
            write("ambientIntensity "+((Light) obj).getIntensity(), out, indent+1);
            write("intensity 0", out, indent+1);
          }
        else
          write("intensity "+((Light) obj).getIntensity(), out, indent+1);
        write("attenuation 1 "+decay+" "+(decay*decay), out, indent+1);
        write("}", out, indent);
        return;
      }
    if (obj instanceof SceneCamera)
      {
        write("Viewpoint {", out, indent);
        write("position "+pos[0]+" "+pos[1]+" "+pos[2], out, indent+1);
        write("orientation "+rot[0]+" "+rot[1]+" "+rot[2]+" "+rot[3], out, indent+1);
        write("fieldOfView "+((SceneCamera) obj).getFieldOfView()*Math.PI/180.0, out, indent+1);
        write("description "+"\""+ info.getName() +"\"", out, indent+1);
        write("}", out, indent);
        return;
      }

    if (info.getChildren() != null && info.getChildren().length > 0) {
        // a group node
        write("DEF " + name + " Group {", out, indent++);
        write("children [", out, indent++);
    }

    // This object will be represented by a Shape node.  First, create a Transform node.

    write("DEF " + name + " Transform {", out, indent);
    write("translation "+pos[0]+" "+pos[1]+" "+pos[2], out, indent+1);
    write("rotation "+rot[0]+" "+rot[1]+" "+rot[2]+" "+rot[3], out, indent+1);
    if (obj instanceof Cylinder)
      ratio = ((Cylinder) obj).getRatio();
    if (obj instanceof Sphere || (obj instanceof Cylinder && (ratio == 0.0 || ratio == 1.0)))
      write("scale "+scale[0]/2.0+" "+scale[1]/2.0+" "+scale[2]/2.0, out, indent+1);
    write("children [", out, indent+1);

    // Create an appropriate Shape node.

    TextureImageInfo ti = (textureExporter == null ? null : textureExporter.getTextureInfo(obj.getTexture()));
    boolean hasTexture = (ti != null && ti.diffuseFilename != null);
    if (obj instanceof Cube && !hasTexture)
      {
        write("Shape {", out, indent+2);
        writeTexture(info, out, indent+3, theScene, textureExporter);
        write("geometry DEF " + name + " Box {", out, indent+3);
        write("size "+scale[0]+" "+scale[1]+" "+scale[2], out, indent+4);
        write("}", out, indent+3);
        write("}", out, indent+2);
      }
    else if (obj instanceof Cylinder && ratio == 1.0 && !hasTexture)
      {
        write("Shape {", out, indent+2);
        writeTexture(info, out, indent+3, theScene, textureExporter);
        write("geometry DEF " + name + " Cylinder {}", out, indent+3);
        write("}", out, indent+2);
      }
    else if (obj instanceof Cylinder && ratio == 0.0 && !hasTexture)
      {
        write("Shape {", out, indent+2);
        writeTexture(info, out, indent+3, theScene, textureExporter);
        write("geometry DEF " + name + " Cone {}", out, indent+3);
        write("}", out, indent+2);
      }
    else if (obj instanceof Sphere && !hasTexture)
      {
        write("Shape {", out, indent+2);
        writeTexture(info, out, indent+3, theScene, textureExporter);
        write("geometry DEF " + name + " Sphere {", out, indent+3);
        write("}", out, indent+3);
        write("}", out, indent+2);
      }
    else if (obj instanceof Curve && !obj.canSetTexture())
      {
        WireframeMesh mesh = obj.getWireframeMesh();
        if (mesh != null)
          {
            Vec3 vert[] = mesh.vert;

            write("Shape {", out, indent+2);
            write("geometry DEF " + name + " IndexedLineSet {", out, indent+3);
            write("coord Coordinate { point [", out, indent+4);
            for (int i = 0; i < vert.length; i++)
              {
                pos[0] = Math.round(vert[i].x*1e6)/1e6;
                pos[1] = Math.round(vert[i].y*1e6)/1e6;
                pos[2] = Math.round(vert[i].z*1e6)/1e6;
                write(pos[0]+" "+pos[1]+" "+pos[2]+",", out, indent+5);
              }
            write("] }", out, indent+4);
            write("coordIndex [", out, indent+4);
            for (int i = 0; i < vert.length-1; i++)
              write(i+",", out, indent+5);
            if (obj.isClosed())
              write((vert.length-1)+", 0, -1", out, indent+5);
            else
              write((vert.length-1)+", -1", out, indent+5);
            write("]", out, indent+4);
            write("}", out, indent+3);
            write("}", out, indent+2);
          }
      }
    else if (obj instanceof FacetedMesh && !smooth)
      {
        writeMesh((FacetedMesh) obj, info, out, indent+2, theScene, textureExporter, false);
      }
    else if (obj instanceof ObjectCollection)
      {
        Enumeration e = ((ObjectCollection) obj).getObjects(info, false, theScene);
        while (e.hasMoreElements())
          writeObject((ObjectInfo) e.nextElement(), info, out, tol, smooth, indent+2, theScene, textureExporter);
      }
    else
      {
        // All other objects are represented as IndexedFaceSets.

        TriangleMesh mesh = info.getObject().convertToTriangleMesh(tol);
        if (mesh != null)
          writeMesh(mesh, info, out, indent+2, theScene, textureExporter, true);
      }

    if (info.getChildren() != null && info.getChildren().length > 0)
      {
        write("]", out, indent+1);
        write("}", out, indent);

        indent -= 2;

        int max = info.getChildren().length;
        for (int i = 0; i < max; i++)
            writeObject(info.getChildren()[i], info, out, tol, smooth, indent+2, theScene, textureExporter);
      }

    write("]", out, indent+1);
    write("}", out, indent);
  }

  /** Write out an IndexedFaceSet node describing a mesh. */

  private static void writeMesh(FacetedMesh mesh, ObjectInfo info, PrintWriter out, int indent, Scene theScene, TextureImageExporter textureExporter, boolean includeNormals)
  {
    MeshVertex vert[] = mesh.getVertices();
    double pos[] = new double [3];
    String name = translate(info.getName(), 0, 1, matchId, replace) +
        translate(info.getName(), 1, -1, matchId, replace);
    if (name.length() > 0 && illegalFirst.indexOf(name.charAt(0)) > 0)
      name = '_'+name;

    write("Shape {", out, indent);
    writeTexture(info, out, indent+1, theScene, textureExporter);
    write("geometry DEF " + name + " IndexedFaceSet {", out, indent+1);
    if (info.getObject().isClosed())
      write("solid TRUE", out, indent+2);
    else
      write("solid FALSE", out, indent+2);
    write("coord Coordinate { point [", out, indent+2);
    for (int i = 0; i < vert.length; i++)
      {
        pos[0] = Math.round(vert[i].r.x*1e6)/1e6;
        pos[1] = Math.round(vert[i].r.y*1e6)/1e6;
        pos[2] = Math.round(vert[i].r.z*1e6)/1e6;
        write(pos[0]+" "+pos[1]+" "+pos[2]+",", out, indent+3);
      }
    write("] }", out, indent+2);
    write("coordIndex [", out, indent+2);
    for (int i = 0; i < mesh.getFaceCount(); i++)
    {
      StringBuffer buf = new StringBuffer();
      for (int j = 0; j < mesh.getFaceVertexCount(i); j++)
      {
        if (j > 0)
          buf.append(", ");
        buf.append(mesh.getFaceVertexIndex(i, j));
      }
      buf.append(", -1,");
      write(buf.toString(), out, indent+3);
    }
    write("]", out, indent+2);
    if (includeNormals)
      {
        Vec3 norm[] = mesh.getNormals();
        write("normal Normal { vector [", out, indent+2);
        for (int i = 0; i < norm.length; i++)
          {
            if (norm[i] == null)
              write("1 0 0,", out, indent+3);
            else
              {
                pos[0] = Math.round(norm[i].x*1e6)/1e6;
                pos[1] = Math.round(norm[i].y*1e6)/1e6;
                pos[2] = Math.round(norm[i].z*1e6)/1e6;
                write(pos[0]+" "+pos[1]+" "+pos[2]+",", out, indent+3);
              }
          }
        write("] }", out, indent+2);
        write("normalIndex [", out, indent+2);
        for (int i = 0; i < mesh.getFaceCount(); i++)
        {
          StringBuffer buf = new StringBuffer();
          for (int j = 0; j < mesh.getFaceVertexCount(i); j++)
          {
            if (j > 0)
              buf.append(", ");
            buf.append(mesh.getFaceVertexIndex(i, j));
          }
          buf.append(", -1,");
          write(buf.toString(), out, indent+3);
        }
        write("]", out, indent+2);
      }
    TextureImageInfo ti = (textureExporter == null ? null : textureExporter.getTextureInfo(((Object3D) mesh).getTexture()));
    if (ti != null && ((Object3D) mesh).getTextureMapping() instanceof UVMapping && ((UVMapping) ((Object3D) mesh).getTextureMapping()).isPerFaceVertex(mesh))
    {
      // A per-face-vertex texture mapping.

      Vec2 coords[][] = ((UVMapping) ((Object3D) mesh).getTextureMapping()).findFaceTextureCoordinates(mesh);
      double uscale = (ti.maxu == ti.minu ? 1.0 : 1.0/(ti.maxu-ti.minu));
      double vscale = (ti.maxv == ti.minv ? 1.0 : 1.0/(ti.maxv-ti.minv));
      write("texCoord TextureCoordinate { point [", out, indent+2);
      for (int j = 0; j < coords.length; j++)
        for (int k = 0; k < coords[j].length; k++)
        {
          pos[0] = (coords[j][k].x-ti.minu)*uscale;
          pos[1] = (coords[j][k].y-ti.minv)*vscale;
          pos[0] = Math.round(pos[0]*1e6)/1e6;
          pos[1] = Math.round(pos[1]*1e6)/1e6;
          write(pos[0]+" "+pos[1]+",", out, indent+3);
        }
      write("] }", out, indent+2);
      write("texCoordIndex [", out, indent+2);
      int texVertIndex = 0;
      for (int i = 0; i < mesh.getFaceCount(); i++)
      {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < mesh.getFaceVertexCount(i); j++)
        {
          if (j > 0)
            buf.append(", ");
          buf.append(texVertIndex++);
        }
        buf.append(", -1,");
        write(buf.toString(), out, indent+3);
      }
      write("]", out, indent+2);
    }
    else if (ti != null && ((Object3D) mesh).getTextureMapping() instanceof Mapping2D)
    {
      // A per-vertex texture mapping.

      Vec2 coords[] = ((Mapping2D) ((Object3D) mesh).getTextureMapping()).findTextureCoordinates(mesh);
      double uscale = (ti.maxu == ti.minu ? 1.0 : 1.0/(ti.maxu-ti.minu));
      double vscale = (ti.maxv == ti.minv ? 1.0 : 1.0/(ti.maxv-ti.minv));
      write("texCoord TextureCoordinate { point [", out, indent+2);
      for (int i = 0; i < coords.length; i++)
        {
          pos[0] = (coords[i].x-ti.minu)*uscale;
          pos[1] = (coords[i].y-ti.minv)*vscale;
          pos[0] = Math.round(pos[0]*1e6)/1e6;
          pos[1] = Math.round(pos[1]*1e6)/1e6;
          write(pos[0]+" "+pos[1]+",", out, indent+3);
        }
      write("] }", out, indent+2);
      write("texCoordIndex [", out, indent+2);
      for (int i = 0; i < mesh.getFaceCount(); i++)
      {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < mesh.getFaceVertexCount(i); j++)
        {
          if (j > 0)
            buf.append(", ");
          buf.append(mesh.getFaceVertexIndex(i, j));
        }
        buf.append(", -1,");
        write(buf.toString(), out, indent+3);
      }
      write("]", out, indent+2);
    }
    write("}", out, indent+1);
    write("}", out, indent);
  }

  /** Write out an Appearance node describing a Texture. */

  private static void writeTexture(ObjectInfo info, PrintWriter out, int indent, Scene theScene, TextureImageExporter textureExporter)
  {
    Texture tex = info.getObject().getTexture();
    TextureSpec spec;

    if (tex == null)
      return;
    TextureImageInfo ti = (textureExporter == null ? null : textureExporter.getTextureInfo(tex));
    boolean hasMap = (ti != null && ti.diffuseFilename != null);
    spec = new TextureSpec();
    tex.getAverageSpec(spec, theScene.getTime(), info.getObject().getAverageParameterValues());
    write("appearance Appearance {", out, indent);
    write("material Material {", out, indent+1);
    if (hasMap)
      write("diffuseColor 1 1 1", out, indent+2);
    else
      write("diffuseColor "+spec.diffuse.getRed()+" "+spec.diffuse.getGreen()+" "+spec.diffuse.getBlue(), out, indent+2);
    write("emissiveColor "+spec.emissive.getRed()+" "+spec.emissive.getGreen()+" "+spec.emissive.getBlue(), out, indent+2);
    write("specularColor "+spec.specular.getRed()+" "+spec.specular.getGreen()+" "+spec.specular.getBlue(), out, indent+2);
    write("shininess "+(1.0-spec.roughness), out, indent+2);
    write("transparency "+Math.max(spec.transparent.getRed(), Math.max(spec.transparent.getGreen(), spec.transparent.getBlue())), out, indent+2);
    write("ambientIntensity 1", out, indent+2);
    write("}", out, indent+1);
    if (hasMap)
      {
        write("texture ImageTexture {", out, indent+1);
        write("url \""+ti.diffuseFilename+"\"", out, indent+2);
        write("}", out, indent+1);
      }
    write("}", out, indent);
  }
    /**
     *  Translates chars in a string, from 'match' to 'replace'.
     *
     *<br> For each char in str:
     *<ul>
     *<li>If the char is in 'match' then
     *   <ul>
     *   <li>if there is a char in the corresponding position in 'replace',
     *          then it replaces the char (ie, the char is translated).
     *   <li>if there is no corresponding char in 'replace' then nothing is
     *		copied (ie, the char is deleted).
     *   </ul>
     *<br>
     *<li>if the char is <em>not</em> in 'match' then
     *   <ul>
     *   <li>if 'replace' is longer than 'match' and the char matches one of
     *		the extra chars in 'replace', then the char is copied.
     *   <li>Otherwise the char is deleted.
     *   </ul>
     *</ul>
     *
     *  @param str - incoming String
     *
     *  @param first - the first char in <i>string</i> to translate
     *
     *  @param count - the number of chars to translate
     *
     *  @param match - the list of chars to match in string.
     *
     *  @param replace - the list of chars to replace matched chars with.
     *
     *  @return the translated String
     *
     *  Examples:
     *  match  = "({[+=-"
     *  replace = ")}]"
     *
     *  will convert ( to ); { to }; [ to ]; delete all +=- and copy all else
     *
     *  match  = 'A'
     *  replace = ''
     *  copy all but A
     *
     *  match  = ''
     *  replace = 'A'
     *  delete all but A
     *
     *  match  = 'A'
     *  replace = 'A'
     *  copy all (including A)
     *
     *  match  = ''
     *  replace = ''
     *  delete all.
     *
     *  If this all seems arbitrary, then the following explanation may help:
     *
     *  Any char in 'match' with no corresponding char in 'replace' is
     *  specified for deletion. Conversely, any char in 'replace' with no
     *  corresponding char in 'match' is specified for copying.
     *
     *  If 'match' is longer than 'replace', then this specifies conditional
     *  deletion (those chars in 'match' with no corresponding char in
     *  'replace'), and hence any chars not matched in 'match' are copied
     *  transparently.
     *
     *  So if 'replace' is empty, and 'match' is non empty, then the chars in
     *  'match' are deleted, and everything else is copied.
     *
     *  Conversely, if 'replace' is longer than 'match', then this specifies
     *  conditional copying (those chars in 'replace' with no corresponding
     *  char in 'match'), and hence any chars not matched in 'match' and not
     *  in the extra chars in 'replace' are deleted.
     *
     *  So if 'match' is empty, and 'replace' is non-empty, then 'replace'
     *  specifies chars to be copied, and everything else is deleted.
     *
     *  If 'match' and 'replace' are idenical non-null strings, then the effect
     *  is to copy everything, without change.
     *  (each char in 'match' is translated to itself, and all others are
     *  copied).
     *
     *  If 'match' and 'replace' are both null strings, then the effect is to
     *  delete all characters.
     *  (null 'match' means no translations, and null 'replace' means no
     *  copying).
     */
    private static String translate(String str, int first, int count,
				   String match, String replace)
    {
	if (count < 0) count = str.length() - first;
	else count = Math.min(count, str.length());

	StringBuffer b = new StringBuffer(count);

	int pos = 0;
	char c = 0;

	if (match == null) match = "";
	if (replace == null) replace = "";

	boolean copy = (match.length() != 0 &&
			match.length() >= replace.length());

	// loop over the input string
	int max = first + count;
	for (int x = first; x < max; x++) {
	    c = str.charAt(x);
	    pos = match.indexOf(c);

	    // if found c in 'match'
	    if (pos >= 0) {
		// translate
		if (pos < replace.length()) b.append(replace.charAt(pos));
	    }

	    // copy
	    else if (copy || replace.indexOf(c) >= match.length()) b.append(c);
	}

	return b.toString();
    }
}

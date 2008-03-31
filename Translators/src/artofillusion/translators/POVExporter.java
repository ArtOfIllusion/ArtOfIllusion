/* Copyright (C) 2002-2007 by Norbert Krieg and Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.translators;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;

/** POVExporter contains the actual routines for exporting POV files. */

public class POVExporter
{
    static boolean DEBUG=false;
    static int DIRECTIONAL_LIGHT_DISTANCE=10000;   // for directional light simulating
    static int MAXIMAL_LIGHT_FADE_DISTANCE=10000;  // = decay rate is set to zero
    static int TIGHTNESS=10;  // = decay rate for the spotlight
    static char CHAR_REPLACE='_';
    static final String TEXTURE_NAME_PREFIX="AOI";  //  prefix of the texture declaration for povray files
    static final char REPLACEMENT_CHARACTER='_';   // replacement for non allowed characters in names
    static final String FIX_SEPARATOR=".";   // The separator in file names of suffices and prefices

    static String exportFileName="Untitled";

    public static void exportFile(BFrame parent, Scene theScene)
    {
	// Display a dialog box with options on how to export the scene.
	
	ValueField errorField = new ValueField(0.05, ValueField.POSITIVE);
	BCheckBox smoothBox = new BCheckBox("Smooth Meshes", true);
	BCheckBox includeBox = new BCheckBox("Produce Extra Includefile for Povray textures", true);
        BComboBox exportChoice = new BComboBox(new String [] {
          Translate.text("exportWholeScene"),
          Translate.text("selectedObjectsOnly")
        });
	ComponentsDialog dlg;
	if (theScene.getSelection().length > 0)
	    dlg = new ComponentsDialog(parent, Translate.text("exportToPOV"), 
				       new Widget [] {exportChoice, Translate.label("maxSurfaceError"), errorField, includeBox, smoothBox}, 
				       new String [] {"", "", "", "", ""});
	else
	    dlg = new ComponentsDialog(parent, Translate.text("exportToPOV"), 
				       new Widget [] {Translate.label("maxSurfaceError"), errorField, includeBox, smoothBox}, 
				       new String [] {"", "", "", ""});
	if (!dlg.clickedOk())
	    return;
	
	// Ask the user to select the output file.

        BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("exportToPOV"));
	String suffix=".pov", suffix2=".inc";
	fc.setSelectedFile(new File(exportFileName+suffix));
	if (ArtOfIllusion.getCurrentDirectory() != null)
	    fc.setDirectory(new File(ArtOfIllusion.getCurrentDirectory()));
	// Get the result
	if (!fc.showDialog(parent))
	    return;
        File path = fc.getDirectory();
	int endpos=fc.getSelectedFile().getName().indexOf(FIX_SEPARATOR);
	if (endpos==-1) endpos=fc.getSelectedFile().getName().length();
	exportFileName=fc.getSelectedFile().getName().substring(0,endpos);
	ArtOfIllusion.setCurrentDirectory(fc.getDirectory().getAbsolutePath());
	
	// Check whether file/s exist/s
	File f=null;
	f=new File(path, exportFileName+suffix);
	if (f.exists()) {
	    //overwrite dialog
            String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
            int choice = new BStandardDialog("", Translate.text("overwriteFile", exportFileName+suffix), BStandardDialog.QUESTION).showOptionDialog(parent, options, options[1]);
	    if (choice==1)
              return;
	}
	// Check whether to produce an include file or not
	boolean bIncludeFile=includeBox.getState();
	if (bIncludeFile) {
	    f=new File(path, exportFileName+suffix2);
	    if (f.exists()) {
		// overwrite dialog, if not then write a single pov file and inform the user
                String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
                int choice = new BStandardDialog("", Translate.text("overwriteFile", exportFileName+suffix2), BStandardDialog.QUESTION).showOptionDialog(parent, options, options[1]);
                if (choice==1)
		    bIncludeFile=false; // Write to the povray file
	    }
	}

	// Create the output file.
	try
	    {
		FileOutputStream fos = new FileOutputStream(new File(path, exportFileName+suffix));
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		FileOutputStream fos2=null;
		BufferedOutputStream bos2=null;
		if (bIncludeFile) {
		    fos2=new FileOutputStream(new File(path, exportFileName+suffix2));
		    bos2=new BufferedOutputStream(fos2);
		}
		writeScene(theScene, bos,exportChoice.getSelectedIndex() == 0, errorField.getValue(), smoothBox.getState(),bos2,exportFileName+suffix2);
		bos.close();
		if (bos2!=null) bos2.close();
		fos.close();
		if (fos2!=null) fos2.close();
	    }
	catch (IOException ex)
	    {
              new BStandardDialog("", new String [] {Translate.text("errorExportingScene"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(parent);
	    }
    }
    
    /* Write out the scene in POV format to the specified OutputStream.  The other parameters
       correspond to the options in the dialog box displayed by exportFile(). */
    
    static void writeScene(Scene theScene, OutputStream os, boolean wholeScene, double tol, boolean smooth, OutputStream os2, String includeFileName)
    {
	PrintWriter out = new PrintWriter(os);
	PrintWriter out2 = out;
	boolean bIncludeFile=false;
	if (os2!=null) {
	    out2=new PrintWriter(os2);
	    bIncludeFile=true;
	}
	
	// Write the header information.
	
	write("// POV V3.5", out, 0);
	write("// Produced by Art of Illusion (Peter Eastman) "+ArtOfIllusion.getVersion(), out, 0);
	write("//   and POVExporter plugin (Norbert Krieg) version 1.11", out, 0);
	write("//   creation date: "+new Date().toString(),out,0);
	write("",out,0);
	if (bIncludeFile) {
	    write("// POV V3.5 (include file)",out2,0);
	    write("// Produced by Art of Illusion (Peter Eastman) "+ArtOfIllusion.getVersion(), out2, 0);
	    write("//   and POVExporter plugin (Norbert Krieg) version 1.11", out2, 0);
	    write("//   creation date: "+new Date().toString(),out2,0);
	    write("",out2,0);
	}
	
	// Language spezific settings
	write("#version 3.5;",out,0);
	write("",out,0);
	
	// Global settings
	// only if writeing a whole scene
	if (wholeScene) {
	    write("// Global settings",out,0);
	    // Background color
	    RGBColor color = theScene.getEnvironmentColor();
	    write("background { color rgb <"+color.getRed()+","+color.getGreen()+","+color.getBlue()+"> }", out, 0);
	    
	    // global settings for povray
	    write("global_settings {",out,0);
	    
	    // ambient color
	    color=theScene.getAmbientColor();
	    write("ambient_light <"+color.getRed()+","+color.getGreen()+","+color.getBlue()+">",out,1);
	    
	    // global settings for povray end
	    write("}",out,0);
	    
	    // fog if present
	    if (theScene.getFogState()) {
		color=theScene.getFogColor();
		write("fog {",out,0);
		write("color <"+color.getRed()+","+color.getGreen()+","+color.getBlue()+">",out,1);
		write("distance "+theScene.getFogDistance(),out,1);
		write("}",out,0);
	    }
	}
	write("",out,0);
	
	// Write declarations, camera, lights and objects into file
	int selected[] = theScene.getSelection();
	int maxIndex=0;
	// writing the declarations
	write("// texture declarations",out2,0);
	if (wholeScene) {
	    maxIndex=theScene.getNumObjects();
	    for (int i = 0; i < maxIndex; i++) writeTexture(theScene,theScene.getObject(i),out2,1);
	} else {
	    maxIndex=selected.length;
	    for (int i = 0; i < maxIndex; i++) writeTexture(theScene,theScene.getObject(selected[i]),out2,1);
	}
	out2.flush();
	// write a language directive to include the texture declarations
	if (bIncludeFile) {
	    write("#include \""+includeFileName+"\"",out,0);
	}
	// writing the objects
	write("",out,0);
	if (wholeScene) {
	    maxIndex=theScene.getNumObjects();
	    for (int i = 0; i < maxIndex; i++) writeObjects(theScene,theScene.getObject(i),out,smooth,tol);
	} else {
	    maxIndex=selected.length;
	    for (int i = 0; i < maxIndex; i++) writeObjects(theScene,theScene.getObject(selected[i]),out,smooth,tol);
	}
	
	out.flush();
    }
    
    /* Write a single line to the PrintWriter, indented by the specified number of tabs. */
    
    static void write(String str, PrintWriter out, int indent)
    {
	for (int i = 0; i < indent; i++)
	    out.print("\t");
	out.print(str);
	out.println();
    }

    static String getVec3String(Vec3 vec) {
        if (vec == null)
          return "<1,0,0>";
	double x=Math.round(vec.x*1e6)/1e6;
	double y=Math.round(vec.y*1e6)/1e6;
	double z=Math.round(vec.z*1e6)/1e6;
	return "<"+x+","+y+","+z+">";
    }
    
    static private String cleanName (String oldName) {
	StringBuffer workString=new StringBuffer(oldName.trim());
	for (int i=0;i<workString.length();i++) {
	    char c=workString.charAt(i);
	    if (! java.lang.Character.isLetterOrDigit(c)) {
		workString.setCharAt(i,CHAR_REPLACE);
	    }
	}
	return workString.toString();
    }
    
    /* Write out a series of rotations corresponding to a coordinate system. */
    
    private static void writeRotation(CoordinateSystem coords, PrintWriter out, int indent)
    {
      double [] rot=coords.getRotationAngles();
      write("rotate <0,0,"+(-rot[2])+">",out,indent);
      write("rotate <"+(-rot[0])+",0,0>",out,indent);
      write("rotate <0,"+(-rot[1])+",0>",out,indent);
    }

    /* Write out an Appearance node describing a Texture. */
    
    static void writeTexture(Scene theScene, ObjectInfo obj, PrintWriter out,int indent)
    {
	// ObjectInfo obj=theScene.getObject(index);
	Texture tex = obj.getObject().getTexture();
	if (tex == null) return;
	TextureSpec spec;
	
	spec = new TextureSpec();
	tex.getAverageSpec(spec, theScene.getTime(), obj.getObject().getAverageParameterValues());
	
	String texName=cleanName(tex.getName());
	if (tex.getID()==1) texName=cleanName(obj.getName());
	if (DEBUG) {
	    System.err.println("Texture "+tex.getName()+":\t"+tex.getID());
	}
	
	write("#declare "+TEXTURE_NAME_PREFIX+texName+" = ",out,0);
	write("texture {",out,indent);
	write("pigment {",out,indent+1);
	write("color <"+spec.diffuse.getRed()+","+spec.diffuse.getGreen()+","+spec.diffuse.getBlue()+">",out,indent+2);
	write("}",out,indent+1);
	write("finish {",out,indent+1);
	write("ambient "+Math.max(spec.emissive.getRed(), Math.max(spec.emissive.getGreen(), spec.emissive.getBlue())), out, indent+2);
	write("specular "+Math.max(spec.specular.getRed(), Math.max(spec.specular.getGreen(), spec.specular.getBlue())), out, indent+2);
	write("roughness "+spec.roughness,out,indent+2);
	write("}",out,indent+1);
	write("}",out,indent);
	
	/*
	  write("emissiveColor "+spec.emissive.getRed()+" "+spec.emissive.getGreen()+" "+spec.emissive.getBlue(), out, indent+2);
	  write("specularColor "+spec.specular.getRed()+" "+spec.specular.getGreen()+" "+spec.specular.getBlue(), out, indent+2);
	  write("shininess "+(1.0-spec.roughness), out, indent+2);
	  write("transparency "+Math.max(spec.transparent.getRed(), Math.max(spec.transparent.getGreen(), spec.transparent.getBlue())), out, indent+2);
	  write("ambientIntensity 1", out, indent+2);
	  write("}", out, indent+1);
	  write("}", out, indent);
	*/
    }
    
    static void writeObjects(Scene theScene, ObjectInfo obj, PrintWriter out,boolean smooth,double tolerance) {
    // Camera setting
    // (not very good because of extra loop count - but for readability purposes of POV file better)
	if (!obj.isVisible()) return;
	if (obj.getObject() instanceof SceneCamera) {
	    write("// Camera settings",out,0);
	    ObjectInfo info = (ObjectInfo)obj;
	    CoordinateSystem coords = info.getCoords().duplicate();
	    // nötig ???? obwohl KoSys linkshändig????
	    //coords.setOrientation(coords.getZDirection().times(-1.0), coords.getUpDirection().times(1.0));
	    Vec3 orig=coords.getOrigin();
	    String location="location "+getVec3String(orig);
            Vec3 zdir = coords.getZDirection();
            Vec3 updir = coords.getUpDirection();
            Vec3 rightdir = zdir.cross(updir).times(1.33);
	    String direction="direction <"+zdir.x+","+zdir.y+","+zdir.z+">";
	    String up="up <"+updir.x+","+updir.y+","+updir.z+">";
	    String right="right <"+rightdir.x+","+rightdir.y+","+rightdir.z+">";
	    String angle="angle "+(((SceneCamera) obj.getObject()).getFieldOfView()*1.33);
	    write("camera {",out,0);
	    write("perspective",out,1);
	    write(location,out,1);
	    write(direction,out,1);
	    write(up,out,1);
	    write(right,out,1);
	    write(angle,out,1);
	    write("}",out,0);
	    if (DEBUG) {
		System.err.println("Camera:");
		System.err.println("// Standort:\t"+orig.toString());
		System.err.println("// Blickrichtung:\t"+coords.getZDirection().toString());
		System.err.println("// Up-Richtung:\t"+coords.getUpDirection().toString());
		System.err.println("// Right (for aspect ratio):\t"+coords.getUpDirection().cross(coords.getZDirection()).times(1.33).toString());
	    }
	    write("",out,0);
	}
	// Light setting
	// in extra loop for having it at the beginning of the povray file
	// Directional Light
	// in POVRAY it is a very far away point light
	// i.e. 10000 units should be in most cases far enough
	else if (obj.getObject() instanceof DirectionalLight) {
	    write("// Directional light (simulated as far away point light distance="+DIRECTIONAL_LIGHT_DISTANCE+"units)",out,0);
	    DirectionalLight light=(DirectionalLight) obj.getObject();
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    CoordinateSystem coords = info.getCoords();
	    Vec3 zdir=coords.getZDirection();
	    float intensity=light.getIntensity();
	    RGBColor color=light.getColor().duplicate();
	    color.scale(intensity);
	    zdir.normalize(); // to get a defined distance
	    zdir.scale(-DIRECTIONAL_LIGHT_DISTANCE);
	    write("// "+ info.getName(),out,0);
	    write("light_source {",out,0);
	    write(getVec3String(zdir),out,1);
	    write("color <"+color.getRed()+","+color.getGreen()+","+color.getBlue()+">",out,1);
	    write("parallel",out,1);
	    write("point_at "+getVec3String(new Vec3(0,0,0)),out,1);
	    write("}",out,0);
	    write("",out,0);
	    if (DEBUG) {
		System.err.println("// Directional Light source");
		System.err.println("// --> point light:");
		System.err.println("// Position:\t"+zdir.toString());
		System.err.println("// Color:\t"+color.toString());
	    }
	}
	else if (obj.getObject() instanceof PointLight) {
	    write("// Point light",out,0);
	    write("// decay is different than in Art Of Illusion",out,0);
	    write("// the point light source has actually no \"radius\"",out,0);
	    PointLight light=(PointLight) obj.getObject();
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    CoordinateSystem coords = info.getCoords();
	    Vec3 orig=coords.getOrigin();
	    float intensity=light.getIntensity();
	    RGBColor color=light.getColor().duplicate();
	    color.scale(intensity);
	    //double radius=light.getRadius();
	    float dr=light.getDecayRate();
	    double fadeDistance=MAXIMAL_LIGHT_FADE_DISTANCE;
	    if (dr!=0.0) fadeDistance=0.5/(new Float(dr).doubleValue());
	    double fadePower=2.0; 
	    if (fadeDistance>=5.0) fadePower=1.0;  // only linear instead of squared
	    write("// "+ info.getName(),out,0);
	    write("light_source {",out,0);
	    write(getVec3String(orig),out,1);
	    write("color <"+color.getRed()+","+color.getGreen()+","+color.getBlue()+">",out,1);
	    if (light.getType() != Light.TYPE_NORMAL) write("shadowless",out,1);
	    write("fade_distance "+fadeDistance,out,1);
	    write("fade_power "+fadePower,out,1);
	    write("}",out,0);
	    write("",out,0);
	    if (DEBUG) {
		System.err.println("// Point Light source");
		System.err.println("// --> point light:");
		System.err.println("// Position:\t"+orig.toString());
		System.err.println("// FadeDistance:\t"+fadeDistance);
		System.err.println("// FadePower:\t"+fadePower);
	    }
	}
	else if (obj.getObject() instanceof SpotLight) {
	    write("// Spot light",out,0);
	    write("// the spot light source has actually no \"radius\"",out,0);
	    SpotLight light=(SpotLight) obj.getObject();
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    CoordinateSystem coords = info.getCoords();
	    Vec3 zdir=coords.getZDirection();
	    Vec3 orig=coords.getOrigin();
	    // get the point to which a spotlight points at (orig + zdir)
	    Vec3 pointat=orig.plus(zdir);
	    // Adjust Color
	    RGBColor color=light.getColor().duplicate();
	    float intensity=light.getIntensity();
	    color.scale(intensity);
	    // calculating the radius and fallof angle for POVRAY
	    // not all possibilities of povray's spotlight used
	    double radius=light.getAngle()/2.0*(1.0-light.getFalloff());
	    double falloff=light.getAngle()/2.0;
	    // calculate the decay rate
	    float dr=light.getDecayRate();
	    double fadeDistance=MAXIMAL_LIGHT_FADE_DISTANCE;
	    if (dr!=0.0) fadeDistance=0.5/(new Float(dr).doubleValue());
	    double fadePower=2.0;  
	    if (fadeDistance>=5.0) fadePower=1.0;  // only linear instead of squared
	    write("// "+ info.getName(),out,0);
	    write("light_source {",out,0);
	    write(getVec3String(orig),out,1);
	    write("color <"+color.getRed()+","+color.getGreen()+","+color.getBlue()+">",out,1);
	    write("spotlight",out,1);
	    write("point_at "+getVec3String(pointat),out,1);
	    write("radius "+radius,out,1);
	    write("falloff "+falloff,out,1);
	    write("tightness "+TIGHTNESS,out,1); // simulates the AoI's behavior
	    if (light.getType() != Light.TYPE_NORMAL) write("shadowless",out,1);
	    write("fade_distance "+fadeDistance,out,1);
	    write("fade_power "+fadePower,out,1);
	    write("}",out,0);
	    write("",out,0);
	    if (DEBUG) {
		System.err.println("// Spotlight source");
		System.err.println("// --> spot light:");
		System.err.println("// Position:\t"+orig.toString());
		System.err.println("// Point at:\t"+pointat.toString());
		System.err.println("// Color:\t"+color.toString());
		System.err.println("// Radius:\t"+radius);
		System.err.println("// Falloff:\t"+falloff);
		System.err.println("// Tightness:\t"+TIGHTNESS);
		if (light.getType() != Light.TYPE_NORMAL) System.err.println("shadowless");
		System.err.println("// FadeDistance:\t"+fadeDistance);
		System.err.println("// FadePower:\t"+fadePower);
	    }
	}
	// Cube setting
	else if (obj.getObject() instanceof Cube) {
	    write("// Cube settings",out,0);
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    CoordinateSystem coords = info.getCoords();
	    Vec3 size = info.getBounds().getSize(), orig=coords.getOrigin();
	    String texName=cleanName(info.getObject().getTexture().getName());
	    if (info.getObject().getTexture().getID()==1) texName=cleanName(info.getName());
	    size.scale(0.5);
	    write("// "+ info.getName(),out,0);
	    write("box {",out,0);
	    write(getVec3String(new Vec3(-1,-1,-1))+", "+getVec3String(new Vec3(1,1,1)),out,1);
	    write("scale "+getVec3String(size),out,1);
	    writeRotation(coords,out,1);
	    write("translate "+getVec3String(orig),out,1);
	    write("texture { "+TEXTURE_NAME_PREFIX+texName+" }",out,1);
	    write("}",out,0);
	    write("",out,0);
	    if (DEBUG) {
		System.err.println("Box:");
		System.err.println("Mittelpunkt:"+orig.toString());
		System.err.println("Größe:"+size.times(2.0).toString());
		System.err.println("Eckpunkte:\t(lu): "+getVec3String(orig.minus(size)));
		System.err.println("          \t(ro): "+getVec3String(orig.plus(size)));
		System.err.println();
	    }
	}
	//  sphere and ellipse setting
	// (only for testing purposes in an extra loop)
	else if (obj.getObject() instanceof Sphere) {
	    write("// Sphere settings",out,0);
	    Sphere sphere=(Sphere) obj.getObject();
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    CoordinateSystem coords = info.getCoords();
	    //coords.setOrientation(coords.getZDirection().times(-1.0), coords.getUpDirection().times(1.0));
	    Vec3 size = sphere.getRadii(), orig=coords.getOrigin();
	    String texName=cleanName(info.getObject().getTexture().getName());
	    if (info.getObject().getTexture().getID()==1) texName=cleanName(info.getName());
	    write("// "+ info.getName(),out,0);
	    write("sphere {",out,0);
	    write("<0,0,0>, 1",out,1);  // standard sphere
	    write("scale "+getVec3String(size),out,1);
	    writeRotation(coords,out,1);
	    write("translate "+getVec3String(orig),out,1);
	    write("texture { "+TEXTURE_NAME_PREFIX+texName+" }",out,1);
	    write("}",out,0);
	    write("",out,0);
	    if (DEBUG) {
                double rot[] = coords.getRotationAngles();
		System.err.println("Sphere:");
		System.err.println("// Mittelpunkt:\t"+orig.toString());
		System.err.println("// Größe:\t"+size.times(2.0).toString());
		System.err.println("// Rotation (x-axis):\t"+rot[0]);
		System.err.println("//          (y-axis):\t"+rot[1]);
		System.err.println("//          (z-axis):\t"+rot[2]);
		System.err.println();
	    }
	}
	// cylinder setting
	// will be transkripted to a cone object
	// (only for testing purposes in an extra loop)
	else if (obj.getObject() instanceof Cylinder) {
	    write("// Cylinder settings",out,0);
	    Cylinder cyl=(Cylinder) obj.getObject();
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    CoordinateSystem coords = info.getCoords();
	    Vec3 orig=coords.getOrigin();
	    double ratio=cyl.getRatio();
	    Vec3 size=cyl.getBounds().getSize();
	    String texName=cleanName(info.getObject().getTexture().getName());
	    if (info.getObject().getTexture().getID()==1) texName=cleanName(info.getName());
	    size.scale(0.5); 
	    write("// "+ info.getName(),out,0);
	    write("cone {",out,0);
	    write("<0,-1,0>, 1, <0,1,0>, "+ratio,out,1);  // standard sphere
	    write("scale "+getVec3String(size),out,1);
	    writeRotation(coords,out,1);
	    write("translate "+getVec3String(orig),out,1);
	    write("texture { "+TEXTURE_NAME_PREFIX+texName+" }",out,1);
	    write("}",out,0);
	    write("",out,0);
	    if (DEBUG) {
                double rot[] = coords.getRotationAngles();
		System.err.println("Cone:");
		System.err.println("// Mittelpunkt:\t"+orig.toString());
		System.err.println("// Größe:\t"+size.times(2.0).toString());
		System.err.println("// Rotation (x-axis):\t"+rot[0]);
		System.err.println("//          (y-axis):\t"+rot[1]);
		System.err.println("//          (z-axis):\t"+rot[2]);
		System.err.println();
	    }
	}
	// Mesh for not smoothed triangle meshes
	// (only for testing purposes in an extra loop)
	else if ((obj.getObject() instanceof TriangleMesh) && (!smooth)) {
	    write("// Triangle Mesh (mesh2) not smoothed",out,0);
	    TriangleMesh trimesh=(TriangleMesh) obj.getObject();
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    CoordinateSystem coords = info.getCoords();
	    Vec3 orig=coords.getOrigin();
	    String texName=cleanName(info.getObject().getTexture().getName());
	    if (info.getObject().getTexture().getID()==1) texName=cleanName(info.getName());
	    
	    MeshVertex vert[] = trimesh.getVertices();
	    TriangleMesh.Face face[] = (trimesh.getFaces());  
	    
	    write("// "+ info.getName(),out,0);
	    write("mesh2 {",out,0);
	    write("vertex_vectors {",out,1);
	    write(vert.length+",",out,2);
	    for (int j = 0; j < vert.length; j++)
		{
		    if (j!=vert.length-1) {
			write(getVec3String(vert[j].r)+",",out,2);
		    }
		    else {
			write(getVec3String(vert[j].r),out,2);
		    }
		}
	    write("}",out,1);
	    write("face_indices {",out,1);
	    write(face.length+",",out,2);
	    for (int j = 0; j < face.length; j++)
		{
		    if (j!=face.length-1) { 
			write("<"+face[j].v1+","+face[j].v2+","+face[j].v3+"> ,",out,2); 
		    }
		    else {
			write("<"+face[j].v1+","+face[j].v2+","+face[j].v3+">",out,2);
		    }
		}
	    write("}",out,1);
	    write("texture { "+TEXTURE_NAME_PREFIX+texName+" }",out,1);
	    writeRotation(coords,out,1);
	    write("translate "+getVec3String(orig),out,1);
	    write("}",out,0);
	    write("",out,0);
	    if (DEBUG) {
		System.err.println("Mesh");
		System.err.println("Name:\t"+ info.getName());
		System.err.println("Mittelpunkt:\t"+orig.toString());
		System.err.println();
	    }
	}
	// if it is a grouping object
	else if ((obj.getObject()) instanceof ObjectCollection)    {
	    Enumeration objects = ((ObjectCollection) obj.getObject()).getObjects(obj, false, theScene);
	    while (objects.hasMoreElements())
		writeObjects(theScene,(ObjectInfo)objects.nextElement(),out,smooth,tolerance);
	}
	// any other object
	else { /* if smoothed mesh or any other object */
	    write("// smoothed object",out,0);
	    ObjectInfo info=(ObjectInfo)obj.duplicate();
	    RenderingMesh mesh = info.getRenderingMesh(tolerance);
	    write("// "+ info.getName(),out,0);
	    if (mesh != null) {  // only if you can render the mesh
		Vec3 vert[] = mesh.vert, norm[] = mesh.norm;
		RenderingTriangle face[] = mesh.triangle;
		
		CoordinateSystem coords = info.getCoords();
		Vec3 orig=coords.getOrigin();
		String texName=cleanName(info.getObject().getTexture().getName());
		if (info.getObject().getTexture().getID()==1) texName=cleanName(info.getName());
		
		write("mesh2 {",out,0);
		write("vertex_vectors {",out,1);
		write(vert.length+",",out,2);
		for (int j = 0; j < vert.length; j++)
		    {
			if (j!=vert.length-1) {
			    write(getVec3String(vert[j])+",",out,2);
			}
			else {
			    write(getVec3String(vert[j]),out,2);
			}
		    }
		write("}",out,1);
		write("normal_vectors {",out,1);
		write(norm.length+",",out,2);
		for (int j = 0; j < norm.length; j++)
		    {
			if (j!=norm.length-1) {
			    write(getVec3String(norm[j])+",",out,2);
			}
			else {
			    write(getVec3String(norm[j]),out,2);
			}
		    }
		write("}",out,1);
		write("face_indices {",out,1);
		write(face.length+",",out,2);
		for (int j = 0; j < face.length; j++)
		    {
			if (j!=face.length-1) { 
			    write("<"+face[j].v1+","+face[j].v2+","+face[j].v3+"> ,",out,2); 
			}
			else {
			    write("<"+face[j].v1+","+face[j].v2+","+face[j].v3+">",out,2);
			}
		    }
		write("}",out,1);
		write("normal_indices {",out,1);
		write(face.length+",",out,2);
		for (int j = 0; j < face.length; j++)
		    {
			if (j!=face.length-1) { 
			    write("<"+face[j].n1+","+face[j].n2+","+face[j].n3+"> ,",out,2);
			}
			else {
			    write("<"+face[j].n1+","+face[j].n2+","+face[j].n3+">",out,2);
			}
		    }
		write("}",out,1);
		
		write("texture { "+TEXTURE_NAME_PREFIX+texName+" }",out,1);
                writeRotation(coords,out,1);
                write("translate "+getVec3String(orig),out,1);
		write("}",out,0);
		write("",out,0);
		if (DEBUG) {
		    System.err.println("Mesh");
		    System.err.println("Name:\t"+ info.getName());
		    System.err.println("Mittelpunkt:\t"+orig.toString());
		    System.err.println();
		}
	    }
	    write("",out,0);
	}
    } // Klasse writeObject ende
}

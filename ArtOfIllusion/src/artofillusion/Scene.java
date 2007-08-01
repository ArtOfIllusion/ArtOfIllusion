/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.image.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

/** The Scene class describes a collection of objects, arranged relative to each other to
    form a scene, as well as the available textures and materials, environment options, etc. */

public class Scene
{
  private Vector objects, materials, textures, images, selection;
  private Vector textureListeners, materialListeners;
  private RGBColor ambientColor, environColor, fogColor;
  private Texture environTexture;
  private TextureMapping environMapping;
  private int gridSubdivisions, environMode, framesPerSecond, nextID;
  private double fogDist, gridSpacing, time;
  private boolean fog, showGrid, snapToGrid, errorsLoading;
  private String name, directory;
  private TexturesDialog texDlg;
  private MaterialsDialog matDlg;
  private ParameterValue environParamValue[];
  private StringBuffer loadingErrors;
  
  public static final int HANDLE_SIZE = 4;
  public static final int ENVIRON_SOLID = 0;
  public static final int ENVIRON_DIFFUSE = 1;
  public static final int ENVIRON_EMISSIVE = 2;

  public Scene()
  {
    UniformTexture defTex = new UniformTexture();
    
    objects = new Vector();
    materials = new Vector();
    textures = new Vector();
    images = new Vector();
    selection = new Vector();
    textureListeners = new Vector();
    materialListeners = new Vector();
    defTex.setName("Default Texture");
    textures.addElement(defTex);
    ambientColor = new RGBColor(0.3f, 0.3f, 0.3f);
    environColor = new RGBColor(0.0f, 0.0f, 0.0f);
    environTexture = defTex;
    environMapping = defTex.getDefaultMapping(new Sphere(1.0, 1.0, 1.0));
    environParamValue = new ParameterValue [0];
    environMode = ENVIRON_SOLID;
    fogColor = new RGBColor(0.3f, 0.3f, 0.3f);
    fogDist = 20.0;
    fog = false;
    framesPerSecond = 30;
    nextID = 1;

    // Grids are off by default.
    
    showGrid = snapToGrid = false;
    gridSpacing = 1.0;
    gridSubdivisions = 10;
  }

  /** Get the name of this scene. */
  
  public String getName()
  {
    return name;
  }

  /** Set the name of this scene. */

  public void setName(String newName)
  {
    name = newName;
  }
  
  /** Get the directory on disk in which this scene is saved. */

  public String getDirectory()
  {
    return directory;
  }

  /** Set the directory on disk in which this scene is saved. */

  public void setDirectory(String newDir)
  {
    directory = newDir;
  }
  
  /** Get the current time. */
  
  public double getTime()
  {
    return time;
  }
  
  /** Set the current time. */
  
  public void setTime(double t)
  {
    time = t;
    boolean changed[] = new boolean [objects.size()];
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        if (info.parent == null)
          applyTracksToObject(info, changed, i);
      }
  }
  
  /** Modify an object (and all of its children) based on its tracks at the current time. */
  
  public void applyTracksToObject(ObjectInfo info)
  {
    applyTracksToObject(info, null, 0);
  }

  private void applyTracksToObject(ObjectInfo info, boolean changed[], int index)
  {
    if (changed != null)
      {
        if (changed[index])
          {
            // This object has already been updated.
            
            info.object.sceneChanged(info, this);
            return;
          }
        changed[index] = true;
      }
      
    // Determine whether this object possesses a Position or Rotation track, and update any
    // tracks it is dependent on.
    
    boolean hasPos = false, hasRot = false, hasPose = false;
    for (int j = 0; j < info.tracks.length; j++)
      {
        if (info.tracks[j].isNullTrack() || !info.tracks[j].isEnabled())
          continue;
        if (changed != null)
          {
            ObjectInfo depends[] = info.tracks[j].getDependencies();
            for (int i = 0; i < depends.length; i++)
              {
                int k = objects.indexOf(depends[i]);
                if (k > -1 && !changed[k])
                  applyTracksToObject(depends[i], changed, k);
              }
          }
        if (info.tracks[j] instanceof PositionTrack)
          hasPos = true;
        else if (info.tracks[j] instanceof RotationTrack)
          hasRot = true;
        else if (info.tracks[j] instanceof PoseTrack)
          hasPose = true;
      }
    if (hasPos)
      {
        Vec3 orig = info.coords.getOrigin();
        orig.set(0.0, 0.0, 0.0);
        info.coords.setOrigin(orig);
      }
    if (hasRot)
      info.coords.setOrientation(0.0, 0.0, 0.0);
    if (hasPose)
      info.clearCachedMeshes();
    info.pose = null;
    
    // Apply the tracks.
    
    info.clearDistortion();
    for (int j = info.tracks.length-1; j >= 0; j--)
      if (info.tracks[j].isEnabled())
        info.tracks[j].apply(time);
    if (info.pose != null)
      info.object.applyPoseKeyframe(info.pose);
    info.object.sceneChanged(info, this);
    
    // Now call this method recursively on any child objects.
    
    for (int i = 0; i < info.children.length; i++)
      applyTracksToObject(info.children[i]);
  }
  
  /** Get the number of frames per second. */
  
  public int getFramesPerSecond()
  {
    return framesPerSecond;
  }
  
  /** Set the number of frames per second. */
  
  public void setFramesPerSecond(int n)
  {
    framesPerSecond = n;
  }
  
  /** Get the scene's ambient light color. */
  
  public RGBColor getAmbientColor()
  {
    return ambientColor;
  }
  
  /** Set the scene's ambient light color. */

  public void setAmbientColor(RGBColor color)
  {
    ambientColor = color;
  }

  /** Get the Scene's environment mapping mode.  This will be either ENVIRON_SOLID, ENVIRON_DIFFUSE, or
      ENVIRON_EMISSIVE. */

  public int getEnvironmentMode()
  {
    return environMode;
  }
  
  /** Set the Scene's environment mapping mode.  This should be either ENVIRON_SOLID, ENVIRON_DIFFUSE, or
      ENVIRON_EMISSIVE. */

  public void setEnvironmentMode(int mode)
  {
    environMode = mode;
  }
  
  /** Get the texture being used as an environment mapping. */

  public Texture getEnvironmentTexture()
  {
    return environTexture;
  }
  
  /** Set the texture being used as an environment mapping. */

  public void setEnvironmentTexture(Texture tex)
  {
    environTexture = tex;
  }

  /** Get the TextureMapping being used to map the environment map texture to the environment sphere. */

  public TextureMapping getEnvironmentMapping()
  {
    return environMapping;
  }
  
  /** Set the TextureMapping to use for mapping the environment map texture to the environment sphere. */

  public void setEnvironmentMapping(TextureMapping map)
  {
    environMapping = map;
  }
  
  /** Get the parameter values used for the environment map. */
  
  public ParameterValue [] getEnvironmentParameterValues()
  {
    return environParamValue;
  }
  
  /** Set the parameter values used for the environment map. */
  
  public void setEnvironmentParameterValues(ParameterValue value[])
  {
    environParamValue = value;
  }

  /** Get the environment color. */

  public RGBColor getEnvironmentColor()
  {
    return environColor;
  }
  
  /** Set the environment color. */

  public void setEnvironmentColor(RGBColor color)
  {
    environColor = color;
  }
  
  /** Get the fog color. */

  public RGBColor getFogColor()
  {
    return fogColor;
  }
  
  /** Set the fog color. */

  public void setFogColor(RGBColor color)
  {
    fogColor = color;
  }

  /** Determine whether fog is enabled. */
  
  public boolean getFogState()
  {
    return fog;
  }
  
  /** Get the length constant for exponential fog. */

  public double getFogDistance()
  {
    return fogDist;
  }
  
  /** Set the state of fog in the scene.
      @param state    sets whether fog is enabled
      @param dist     the length constant for exponential fog.
  */

  public void setFog(boolean state, double dist)
  {
    fog = state;
    fogDist = dist;
  }
  
  /** Get whether the grid is displayed. */
  
  public boolean getShowGrid()
  {
    return showGrid;
  }
  
  /** Set whether the grid is displayed. */
  
  public void setShowGrid(boolean show)
  {
    showGrid = show;
  }
  
  /** Get whether snap-to-grid is enabled. */
  
  public boolean getSnapToGrid()
  {
    return snapToGrid;
  }
  
  /** Set whether snap-to-grid is enabled. */
  
  public void setSnapToGrid(boolean snap)
  {
    snapToGrid = snap;
  }
  
  /** Get the grid spacing. */
  
  public double getGridSpacing()
  {
    return gridSpacing;
  }
  
  /** Set the grid spacing. */
  
  public void setGridSpacing(double spacing)
  {
    gridSpacing = spacing;
  }
  
  /** Get the number of grid snap-to subdivisions. */
  
  public int getGridSubdivisions()
  {
    return gridSubdivisions;
  }

  /** Set the number of grid snap-to subdivisions. */
  
  public void setGridSubdivisions(int subdivisions)
  {
    gridSubdivisions = subdivisions;
  }

  /** Add a new object to the scene.  If undo is not null, appropriate commands will be
      added to it to undo this operation. */

  public void addObject(Object3D obj, CoordinateSystem coords, String name, UndoRecord undo)
  {
    addObject(new ObjectInfo(obj, coords, name), undo);
    updateSelectionInfo();
  }
  
  /** Add a new object to the scene.  If undo is not null, appropriate commands will be
      added to it to undo this operation. */

  public void addObject(ObjectInfo info, UndoRecord undo)
  {
    addObject(info, objects.size(), undo);
    updateSelectionInfo();
  }
  
  /** Add a new object to the scene in the specified position.  If undo is not null, 
      appropriate commands will be added to it to undo this operation. */

  public void addObject(ObjectInfo info, int index, UndoRecord undo)
  {
    info.id = nextID++;
    if (info.tracks == null)
      {
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
      }
    if (info.object.canSetTexture() && info.object.getTextureMapping() == null)
      info.setTexture(getDefaultTexture(), getDefaultTexture().getDefaultMapping(info.object));
    info.object.sceneChanged(info, this);
    objects.insertElementAt(info, index);
    if (undo != null)
      undo.addCommandAtBeginning(UndoRecord.DELETE_OBJECT, new Object [] {new Integer(index)});
    updateSelectionInfo();
  }
  
  /** Delete an object from the scene.  If undo is not null, appropriate commands will be
      added to it to undo this operation. */

  public void removeObject(int which, UndoRecord undo)
  {
    ObjectInfo info = (ObjectInfo) objects.elementAt(which);
    objects.removeElementAt(which);
    if (undo != null)
      undo.addCommandAtBeginning(UndoRecord.ADD_OBJECT, new Object [] {info, new Integer(which)});
    if (info.parent != null)
      {
        int j;
        for (j = 0; info.parent.children[j] != info; j++);
        if (undo != null)
          undo.addCommandAtBeginning(UndoRecord.ADD_TO_GROUP, new Object [] {info.parent, info, new Integer(j)});
        info.parent.removeChild(j);
      }
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = (ObjectInfo) objects.elementAt(i);
        for (int j = 0; j < obj.tracks.length; j++)
          {
            Track tr = obj.tracks[j];
            ObjectInfo depends[] = tr.getDependencies();
            for (int k = 0; k < depends.length; k++)
              if (depends[k] == info)
                {
                  if (undo != null)
                    undo.addCommandAtBeginning(UndoRecord.COPY_TRACK, new Object [] {tr, tr.duplicate(tr.getParent())});
                  obj.tracks[j].deleteDependencies(info);
                }
          }
      }
    clearSelection();
  }
  
  /** Add a new Material to the scene. */
  
  public void addMaterial(Material mat)
  {
    materials.addElement(mat);
    for (int i = 0; i < materialListeners.size(); i++)
      ((ListChangeListener) materialListeners.elementAt(i)).itemAdded(materials.size()-1, mat);
  }
  
  /** Remove a Material from the scene. */
  
  public void removeMaterial(int which)
  {
    Material mat = (Material) materials.elementAt(which);

    materials.removeElementAt(which);
    for (int i = 0; i < materialListeners.size(); i++)
      ((ListChangeListener) materialListeners.elementAt(i)).itemRemoved(which, mat);
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = (ObjectInfo) objects.elementAt(i);
        if (obj.object.getMaterial() == mat)
          obj.setMaterial(null, null);
      }
  }
  
  /** Add a new Texture to the scene. */

  public void addTexture(Texture tex)
  {
    textures.addElement(tex);
    for (int i = 0; i < textureListeners.size(); i++)
      ((ListChangeListener) textureListeners.elementAt(i)).itemAdded(textures.size()-1, tex);
  }

  /** Remove a Texture from the scene. */
  
  public void removeTexture(int which)
  {
    Texture tex = (Texture) textures.elementAt(which);

    textures.removeElementAt(which);
    for (int i = 0; i < textureListeners.size(); i++)
      ((ListChangeListener) textureListeners.elementAt(i)).itemRemoved(which, tex);
    if (textures.size() == 0)
      {
        UniformTexture defTex = new UniformTexture();
        defTex.setName("Default Texture");
        textures.addElement(defTex);
        for (int i = 0; i < textureListeners.size(); i++)
          ((ListChangeListener) textureListeners.elementAt(i)).itemAdded(0, defTex);
      }
    Texture def = (Texture) textures.elementAt(0);
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = (ObjectInfo) objects.elementAt(i);
        if (obj.object.getTexture() == tex)
          obj.setTexture(def, def.getDefaultMapping(obj.object));
      }
  }
  
  /** This method should be called after a Material has been edited.  It notifies 
      any objects using the Material that it has changed. */
  
  public void changeMaterial(int which)
  {
    Material mat = (Material) materials.elementAt(which);
    Object3D obj;

    for (int i = 0; i < objects.size(); i++)
      {
        obj = ((ObjectInfo) objects.elementAt(i)).object;
        if (obj.getMaterial() == mat)
          obj.setMaterial(mat, obj.getMaterialMapping());
      }
    for (int i = 0; i < materialListeners.size(); i++)
      ((ListChangeListener) materialListeners.elementAt(i)).itemChanged(which, mat);
  }
  
  /** This method should be called after a Texture has been edited.  It notifies 
      any objects using the Texture that it has changed. */

  public void changeTexture(int which)
  {
    Texture tex = (Texture) textures.elementAt(which);

    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = (ObjectInfo) objects.elementAt(i);
        if (obj.object.getTexture() == tex)
          obj.setTexture(tex, obj.object.getTextureMapping());
      }
    for (int i = 0; i < textureListeners.size(); i++)
      ((ListChangeListener) textureListeners.elementAt(i)).itemChanged(which, tex);
  }
  
  /** Add an object which wants to be notified when the list of Materials in the Scene changes. */
  
  public void addMaterialListener(ListChangeListener ls)
  {
    materialListeners.addElement(ls);
  }
  
  /** Remove an object from the set to be notified when the list of Materials changes. */
  
  public void removeMaterialListener(ListChangeListener ls)
  {
    materialListeners.removeElement(ls);
  }
  
  /** Add an object which wants to be notified when the list of Textures in the Scene changes. */

  public void addTextureListener(ListChangeListener ls)
  {
    textureListeners.addElement(ls);
  }
  
  /** Remove an object from the set to be notified when the list of Textures changes. */

  public void removeTextureListener(ListChangeListener ls)
  {
    textureListeners.removeElement(ls);
  }
  
  /** Show the dialog for editing textures. */
  
  public void showTexturesDialog(BFrame parent)
  {
    if (texDlg == null)
      texDlg = new TexturesDialog(parent, this);
    else
    {
      Rectangle r = texDlg.getBounds();
      texDlg.dispose();
      texDlg = new TexturesDialog(parent, this);
      texDlg.setBounds(r);
    }
    texDlg.setVisible(true);
  }
  
  /** Show the dialog for editing materials. */
  
  public void showMaterialsDialog(BFrame parent)
  {
    if (matDlg == null)
      matDlg = new MaterialsDialog(parent, this);
    else
    {
      Rectangle r = matDlg.getBounds();
      matDlg.dispose();
      matDlg = new MaterialsDialog(parent, this);
      matDlg.setBounds(r);
    }
    matDlg.setVisible(true);
  }
  
  /** Add an image map to the scene. */
  
  public void addImage(ImageMap im)
  {
    images.addElement(im);
  }
  
  /** Remove an image map from the scene. */

  public boolean removeImage(int which)
  {
    ImageMap image = (ImageMap) images.elementAt(which);

    for (int i = 0; i < textures.size(); i++)
      if (((Texture) textures.elementAt(i)).usesImage(image))
        return false;
    for (int i = 0; i < materials.size(); i++)
      if (((Material) materials.elementAt(i)).usesImage(image))
        return false;
    images.removeElementAt(which);
    return true;
  }
  
  /** Replace every instance of one object in the scene with another one.  If undo is not
      null, commands will be added to it to undo this operation. */
  
  public void replaceObject(Object3D original, Object3D replaceWith, UndoRecord undo)
  {
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        if (info.object != original)
          continue;
        if (undo != null)
          undo.addCommand(UndoRecord.SET_OBJECT, new Object [] {info, original});
        info.object = replaceWith;
        info.clearCachedMeshes();
      }
  }
  
  /** This should be called whenever an object changes.  It clears any cached meshes for
      any instances of the object. */
  
  public void objectModified(Object3D obj)
  {
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        if (info.object == obj)
          {
            info.clearCachedMeshes();
            info.pose = null;
          }
      }
  }
  
  /** Set one object to be selected, deselecting all other objects. */

  public void setSelection(int which)
  {
    clearSelection();
    addToSelection(which);
    updateSelectionInfo();
  }

  /** Set a list of objects to be selected, deselecting all other objects. */
  
  public void setSelection(int which[])
  {
    clearSelection();
    for (int i = 0; i < which.length; i++)
      addToSelection(which[i]);
    updateSelectionInfo();
  }
  
  /** Add an object to the list of selected objects. */
  
  public void addToSelection(int which)
  {
    ObjectInfo info = (ObjectInfo) objects.elementAt(which);
    if (!info.selected)
      selection.addElement(new Integer(which));
    info.selected = true;
    updateSelectionInfo();
  }
  
  /** Deselect all objects. */
  
  public void clearSelection()
  {
    selection.removeAllElements();
    for (int i = 0; i < objects.size(); i++)
      ((ObjectInfo) objects.elementAt(i)).selected = false;
    updateSelectionInfo();
  }
  
  /** Deselect a particular object. */
  
  public void removeFromSelection(int which)
  {
    ObjectInfo info = (ObjectInfo) objects.elementAt(which);
    selection.removeElement(new Integer(which));
    info.selected = false;
    updateSelectionInfo();
  }
  
  /** Calculate the list of which objects are children of selected objects. */
  
  private void updateSelectionInfo()
  {
    for (int i = objects.size()-1; i >= 0; i--)
      ((ObjectInfo) objects.elementAt(i)).parentSelected = false;
    for (int i = objects.size()-1; i >= 0; i--)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        ObjectInfo parent = info.parent;
        while (parent != null)
          {
            if (parent.selected || parent.parentSelected)
              {
                info.parentSelected = true;
                break;
              }
            parent = parent.parent;
          }
      }
  }
  
  /** Get the number of objects in this scene. */
  
  public int getNumObjects()
  {
    return objects.size();
  }
  
  /** Get the i'th object. */
  
  public ObjectInfo getObject(int i)
  {
    return (ObjectInfo) objects.elementAt(i);
  }
    
  /** Get the object with the specified name, or null if there is none.  If
      more than one object has the same name, this will return the first one. */
  
  public ObjectInfo getObject(String name)
  {
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        if (info.name.equals(name))
          return info;
      }
    return null;
  }

  /** Get the index of the specified object. */
  
  public int indexOf(ObjectInfo info)
  {
    return objects.indexOf(info);
  }
  
  /** Get the number of textures in this scene. */
  
  public int getNumTextures()
  {
    return textures.size();
  }
  
  /** Get the index of the specified texture. */
  
  public int indexOf(Texture tex)
  {
    return textures.indexOf(tex);
  }
  
  /** Get the i'th texture. */
  
  public Texture getTexture(int i)
  {
    return (Texture) textures.elementAt(i);
  }
  
  /** Get the texture with the specified name, or null if there is none.  If
      more than one texture has the same name, this will return the first one. */
  
  public Texture getTexture(String name)
  {
    for (int i = 0; i < textures.size(); i++)
      {
        Texture tex = (Texture) textures.elementAt(i);
        if (tex.getName().equals(name))
          return tex;
      }
    return null;
  }
  
  /** Get the number of materials in this scene. */
  
  public int getNumMaterials()
  {
    return materials.size();
  }
  
  /** Get the i'th material. */
  
  public Material getMaterial(int i)
  {
    return (Material) materials.elementAt(i);
  }
  
  /** Get the material with the specified name, or null if there is none.  If
      more than one material has the same name, this will return the first one. */
  
  public Material getMaterial(String name)
  {
    for (int i = 0; i < materials.size(); i++)
      {
        Material mat = (Material) materials.elementAt(i);
        if (mat.getName().equals(name))
          return mat;
      }
    return null;
  }
  
  /** Get the index of the specified material. */
  
  public int indexOf(Material mat)
  {
    return materials.indexOf(mat);
  }

  /** Get the number of image maps in this scene. */
  
  public int getNumImages()
  {
    return images.size();
  }
  
  /** Get the i'th image map. */
  
  public ImageMap getImage(int i)
  {
    return (ImageMap) images.elementAt(i);
  }
  
  /** Get the index of the specified image map. */
  
  public int indexOf(ImageMap im)
  {
    return images.indexOf(im);
  }
  
  /** Get the default Texture for newly created objects. */
  
  public Texture getDefaultTexture()
  {
    return (Texture) textures.elementAt(0);
  }
    
  /** Get a list of the indices of all selected objects. */
  
  public int [] getSelection()
  {
    int sel[] = new int [selection.size()];

    for (int i = 0; i < sel.length; i++)
      sel[i] = ((Integer) selection.elementAt(i)).intValue();
    return sel;
  }
  
  /** Get the indices of all objects which are either selected, or are children of
      selected objects. */
  
  public int [] getSelectionWithChildren()
  {
    int count = 0;
    for (int i = objects.size()-1; i >= 0; i--)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        if (info.selected || info.parentSelected)
          count++;
      }
    int sel[] = new int [count];
    count = 0;
    for (int i = objects.size()-1; i >= 0; i--)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        if (info.selected || info.parentSelected)
          sel[count++] = i;
      }
    return sel;
  }
  
  /** Return true if any errors occurred while loading the scene.  The scene is still valid
      and usable, but some objects in it were not loaded correctly. */
  
  public boolean errorsOccurredInLoading()
  {
    return errorsLoading;
  }

  /** Get a description of any errors which occurred while loading the scene. */

  public String getLoadingErrors()
  {
    return (loadingErrors == null ? "" : loadingErrors.toString());
  }

  /** The following constructor is used for reading files.  If fullScene is false, only the
      Textures and Materials are read. */
  
  public Scene(File f, boolean fullScene) throws IOException, InvalidObjectException
  {
    DataInputStream in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(f))));
    initFromStream(in, fullScene);
    in.close();
    setName(f.getName());
    setDirectory(f.getParent());
  }

  /** The following constructor is used for reading from arbitrary input streams.  If fullScene
      is false, only the Textures and Materials are read. */
  
  public Scene(DataInputStream in, boolean fullScene) throws IOException, InvalidObjectException
  {
    initFromStream(in, fullScene);
  }
  
  /** Initialize the scene based on information read from an input stream. */
  
  private void initFromStream(DataInputStream in, boolean fullScene) throws IOException, InvalidObjectException
  {
    int i, j, count;
    short version = in.readShort();
    Hashtable table;
    Class cls;
    Constructor con;

    if (version < 0 || version > 3)
      throw new InvalidObjectException("");
    loadingErrors = new StringBuffer();
    ambientColor = new RGBColor(in);
    fogColor = new RGBColor(in);
    fog = in.readBoolean();
    fogDist = in.readDouble();
    showGrid = in.readBoolean();
    snapToGrid = in.readBoolean();
    gridSpacing = in.readDouble();
    gridSubdivisions = in.readInt();
    framesPerSecond = in.readInt();
    nextID = 1;
    
    // Read the image maps.
    
    count = in.readInt();
    images = new Vector(count);
    for (i = 0; i < count; i++)
      {
        if (version == 0)
          {
            images.addElement(new MIPMappedImage(in, (short) 0));
            continue;
          }
        String classname = in.readUTF();
        try
          {
            cls = ModellingApp.getClass(classname);
            if (cls == null)
              throw new IOException("Unknown class: "+classname);
            con = cls.getConstructor(new Class [] {DataInputStream.class});
            images.addElement(con.newInstance(new Object [] {in}));
          }
        catch (Exception ex)
          {
            throw new IOException("Error loading image: "+ex.getMessage());
          }
      }
    
    // Read the materials.
    
    count = in.readInt();
    materials = new Vector(count);
    for (i = 0; i < count; i++)
      {
        try
          {
            String classname = in.readUTF();
            int len = in.readInt();
            byte bytes[] = new byte [len];
            in.readFully(bytes);
            cls = ModellingApp.getClass(classname);
            try
              {
                if (cls == null)
                  throw new IOException("Unknown class: "+classname);
                con = cls.getConstructor(new Class [] {DataInputStream.class, Scene.class});
                materials.addElement(con.newInstance(new Object [] {new DataInputStream(new ByteArrayInputStream(bytes)), this}));
              }
            catch (Exception ex)
              {
                ex.printStackTrace();
                if (ex instanceof ClassNotFoundException)
                  loadingErrors.append(Translate.text("errorFindingClass", classname)).append('\n');
                else
                  loadingErrors.append(Translate.text("errorInstantiatingClass", classname)).append('\n');
                UniformMaterial m = new UniformMaterial();
                m.setName("<unreadable>");
                materials.addElement(m);
                errorsLoading = true;
              }
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new IOException();
          }
      }
      
    // Read the textures.
    
    count = in.readInt();
    textures = new Vector(count);
    for (i = 0; i < count; i++)
      {
        try
          {
            String classname = in.readUTF();
            int len = in.readInt();
            byte bytes[] = new byte [len];
            in.readFully(bytes);
            cls = ModellingApp.getClass(classname);
            try
              {
                if (cls == null)
                  throw new IOException("Unknown class: "+classname);
                con = cls.getConstructor(new Class [] {DataInputStream.class, Scene.class});
                textures.addElement(con.newInstance(new Object [] {new DataInputStream(new ByteArrayInputStream(bytes)), this}));
              }
            catch (Exception ex)
              {
                ex.printStackTrace();
                if (ex instanceof ClassNotFoundException)
                  loadingErrors.append(Translate.text("errorFindingClass", classname)).append('\n');
                else
                  loadingErrors.append(Translate.text("errorInstantiatingClass", classname)).append('\n');
                UniformTexture t = new UniformTexture();
                t.setName("<unreadable>");
                textures.addElement(t);
                errorsLoading = true;
              }
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new IOException();
          }
      }
  
    // Read the objects.
    
    count = in.readInt();
    objects = new Vector(count);
    table = new Hashtable(count);
    for (i = 0; i < count; i++)
      objects.addElement(readObjectFromFile(in, table, version));
    selection = new Vector();
    
    // Read the list of children for each object.
    
    for (i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        int num = in.readInt();
        for (j = 0; j < num; j++)
          {
            ObjectInfo child = (ObjectInfo) objects.elementAt(in.readInt());
            info.addChild(child, j);
          }
      }
    
    // Read in the environment mapping information.
    
    environMode = (int) in.readShort();
    if (environMode == ENVIRON_SOLID)
      {
        environColor = new RGBColor(in);
        environTexture = (Texture) textures.elementAt(0);
        environMapping = environTexture.getDefaultMapping(new Sphere(1.0, 1.0, 1.0));
        environParamValue = new ParameterValue [0];
      }
    else
      {
        int texIndex = in.readInt();
        if (texIndex == -1)
          {
            // This is a layered texture.

            Object3D sphere = new Sphere(1.0, 1.0, 1.0);
            environTexture = new LayeredTexture(sphere);
            String mapClassName = in.readUTF();
            if (!LayeredMapping.class.getName().equals(mapClassName))
              throw new InvalidObjectException("");
            environMapping = environTexture.getDefaultMapping(sphere);
            ((LayeredMapping) environMapping).readFromFile(in, this);
          }
        else
          {
            environTexture = ((Texture) textures.elementAt(texIndex));
            try
              {
                Class mapClass = ModellingApp.getClass(in.readUTF());
                con = mapClass.getConstructor(new Class [] {DataInputStream.class, Texture.class});
                environMapping = (TextureMapping) con.newInstance(new Object [] {in, environTexture});
              }
            catch (Exception ex)
              {
                throw new IOException();
              }
          }
        environColor = new RGBColor(0.0f, 0.0f, 0.0f);
        environParamValue = new ParameterValue [environMapping.getParameters().length];
        if (version > 2)
          for (i = 0; i < environParamValue.length; i++)
            environParamValue[i] = Object3D.readParameterValue(in);
      }
    textureListeners = new Vector();
    materialListeners = new Vector();
    setTime(0.0);
  }
  
  private ObjectInfo readObjectFromFile(DataInputStream in, Hashtable table, int version) throws IOException, InvalidObjectException
  {
    ObjectInfo info = new ObjectInfo(null, new CoordinateSystem(in), in.readUTF());
    Class cls;
    Constructor con;
    Object3D obj;
    Object key;
    
    info.id = in.readInt();
    if (info.id >= nextID)
      nextID = info.id+1;
    info.visible = in.readBoolean();
    key = new Integer(in.readInt());
    obj = (Object3D) table.get(key);
    if (obj == null)
      {
        try
          {
            String classname = in.readUTF();
            int len = in.readInt();
            byte bytes[] = new byte [len];
            in.readFully(bytes);
            try
              {
                cls = ModellingApp.getClass(classname);
                con = cls.getConstructor(new Class [] {DataInputStream.class, Scene.class});
                obj = (Object3D) con.newInstance(new Object [] {new DataInputStream(new ByteArrayInputStream(bytes)), this});
              }
            catch (Exception ex)
              {
                if (ex instanceof InvocationTargetException)
                  ((InvocationTargetException) ex).getTargetException().printStackTrace();
                else
                  ex.printStackTrace();
                if (ex instanceof ClassNotFoundException)
                  loadingErrors.append(info.name).append(": ").append(Translate.text("errorFindingClass", classname)).append('\n');
                else
                  loadingErrors.append(info.name).append(": ").append(Translate.text("errorInstantiatingClass", classname)).append('\n');
                obj = new NullObject();
                info.name = "<unreadable> "+info.name;
                errorsLoading = true;
              }
            table.put(key, obj);
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new IOException();
          }
      }
    info.object = obj;
    
    if (version < 2 && obj.getTexture() != null)
      {
        // Initialize the texture parameters.
        
        TextureParameter texParam[] = obj.getTextureMapping().getParameters();
        ParameterValue paramValue[] = obj.getParameterValues();
        double val[] = new double [paramValue.length];
        boolean perVertex[] = new boolean [paramValue.length];
        for (int i = 0; i < val.length; i++)
          val[i] = in.readDouble();
        for (int i = 0; i < perVertex.length; i++)
          perVertex[i] = in.readBoolean();
        for (int i = 0; i < paramValue.length; i++)
          if (paramValue[i] == null)
          {
            if (perVertex[i])
              paramValue[i] = new VertexParameterValue((Mesh) obj, texParam[i]);
            else
              paramValue[i] = new ConstantParameterValue(val[i]);
          }
        obj.setParameterValues(paramValue);
      }
    
    // Read the tracks for this object.
    
    int tracks = in.readInt();
    try
      {
        for (int i = 0; i < tracks; i++)
          {
            cls = ModellingApp.getClass(in.readUTF());
            con = cls.getConstructor(new Class [] {ObjectInfo.class});
            Track tr = (Track) con.newInstance(new Object [] {info});
            tr.initFromStream(in, this);
            info.addTrack(tr, i);
          }
        if (info.tracks == null)
          info.tracks = new Track [0];
      }
    catch (Exception ex)
      {
        ex.printStackTrace();
        throw new IOException();
      }
    return info;
  }

  /** Save the Scene to a file. */
  
  public void writeToFile(File f) throws IOException
  {
    int mode = (ModellingApp.getPreferences().getKeepBackupFiles() ? SafeFileOutputStream.OVERWRITE+SafeFileOutputStream.KEEP_BACKUP : SafeFileOutputStream.OVERWRITE);
    SafeFileOutputStream safeOut = new SafeFileOutputStream(f, mode);
    DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(safeOut)));
    writeToStream(out);
    out.close();
  }

  /** Write the Scene's representation to an output stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    Material mat;
    Texture tex;
    int i, j, index = 0;
    Hashtable table = new Hashtable(objects.size());
    
    out.writeShort(3);
    ambientColor.writeToFile(out);
    fogColor.writeToFile(out);
    out.writeBoolean(fog);
    out.writeDouble(fogDist);
    out.writeBoolean(showGrid);
    out.writeBoolean(snapToGrid);
    out.writeDouble(gridSpacing);
    out.writeInt(gridSubdivisions);
    out.writeInt(framesPerSecond);

    // Save the image maps.
    
    out.writeInt(images.size());
    for (i = 0; i < images.size(); i++)
      {
        ImageMap img = (ImageMap) images.elementAt(i);
        out.writeUTF(img.getClass().getName());
        img.writeToStream(out);
      }

    // Save the materials.

    out.writeInt(materials.size());
    for (i = 0; i < materials.size(); i++)
      {
        mat = (Material) materials.elementAt(i);
        out.writeUTF(mat.getClass().getName());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        mat.writeToFile(new DataOutputStream(bos), this);
        byte bytes[] = bos.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes, 0, bytes.length);
      }
    
    // Save the textures.

    out.writeInt(textures.size());
    for (i = 0; i < textures.size(); i++)
      {
        tex = (Texture) textures.elementAt(i);
        out.writeUTF(tex.getClass().getName());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        tex.writeToFile(new DataOutputStream(bos), this);
        byte bytes[] = bos.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes, 0, bytes.length);
      }

    // Save the objects.
    
    out.writeInt(objects.size());
    for (i = 0; i < objects.size(); i++)
      index = writeObjectToFile(out, (ObjectInfo) objects.elementAt(i), table, index);
    
    // Record the children of each object.  The format of this will be changed in the
    // next version.
    
    for (i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) objects.elementAt(i);
        out.writeInt(info.children.length);
        for (j = 0; j < info.children.length; j++)
          out.writeInt(objects.indexOf(info.children[j]));
      }
    
    // Save the environment mapping information.

    out.writeShort((short) environMode);
    if (environMode == ENVIRON_SOLID)
      environColor.writeToFile(out);
    else
      {
        out.writeInt(textures.lastIndexOf(environTexture));
        out.writeUTF(environMapping.getClass().getName());
        if (environMapping instanceof LayeredMapping)
          ((LayeredMapping) environMapping).writeToFile(out, this);
        else
          environMapping.writeToFile(out);
        for (i = 0; i < environParamValue.length; i++)
        {
          out.writeUTF(environParamValue[i].getClass().getName());
          environParamValue[i].writeToStream(out);
        }
      }
  }
  
  /** Write the information about a single object to a file. */
  
  private int writeObjectToFile(DataOutputStream out, ObjectInfo info, Hashtable table, int index) throws IOException
  {
    Integer key;

    info.coords.writeToFile(out);
    out.writeUTF(info.name);
    out.writeInt(info.id);
    out.writeBoolean(info.visible);
    key = (Integer) table.get(info.object);
    if (key == null)
      {
        out.writeInt(index);
        out.writeUTF(info.object.getClass().getName());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        info.object.writeToFile(new DataOutputStream(bos), this);
        byte bytes[] = bos.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes, 0, bytes.length);
        key = new Integer(index++);
        table.put(info.object, key);
      }
    else
      out.writeInt(key.intValue());
    
    // Write the tracks for this object.
    
    out.writeInt(info.tracks.length);
    for (int i = 0; i < info.tracks.length; i++)
      {
        out.writeUTF(info.tracks[i].getClass().getName());
        info.tracks[i].writeToStream(out, this);
      }
    return index;
  }
}
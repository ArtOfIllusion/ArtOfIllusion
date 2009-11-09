/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.view.*;
import artofillusion.animation.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;
import java.lang.reflect.*;

/** Object3D is the abstract superclass of any object which can be placed into a Scene. */

public abstract class Object3D
{
  protected Texture theTexture;
  protected Material theMaterial;
  protected TextureMapping texMapping;
  protected MaterialMapping matMapping;
  protected TextureParameter texParam[];
  protected ParameterValue paramValue[];

  public static final int CANT_CONVERT = 0;
  public static final int EXACTLY = 1;
  public static final int APPROXIMATELY = 2;

  public Object3D()
  {
  }
  
  /** Create a new object which is an exact duplicate of this one. */
  
  public abstract Object3D duplicate();
  
  /** Copy all the properties of another object, to make this one identical to it.  If the
      two objects are of different classes, this will throw a ClassCastException. */
  
  public abstract void copyObject(Object3D obj);

  /** Get a BoundingBox which just encloses the object. */

  public abstract BoundingBox getBounds();

  /** Resize the object.  This should be interpreted such that, if setSize() is followed
      by a call to getBounds(), the dimensions of the BoundingBox will exactly match the
      dimensions specified in setSize(). */

  public abstract void setSize(double xsize, double ysize, double zsize);

  /** Tells whether the object is closed.  For curves, this means it has no endpoints.  For
      surface, it means the surface has no boundary. */

  public boolean isClosed()
  {
    return true;
  }
  
  /** Tells whether the object can be converted to a TriangleMesh.  It should return one
      of the following values:
      
      CANT_CONVERT: The object cannot be converted to a TriangleMesh.
      EXACTLY: The object can be represented exactly by a TriangleMesh.
      APPROXIMATELY: The object can be converted to a TriangleMesh.  However, the resulting
      mesh will not be exactly the same shape as the original object.
      
      If a class overrides this method, it must also override convertToTriangleMesh(). */

  public int canConvertToTriangleMesh()
  {
    return CANT_CONVERT;
  }
  
  /** Return a TriangleMesh which reproduces the shape of this object.  If
      canConvertToTriangleMesh() returned APPROXIMATELY, this method should return a
      TriangleMesh which reproduces the object to within the specified tolerance.  That is,
      no point on the mesh should be further than tol from the corresponding point on the
      original surface.  If canConvertToTriangleMesh() returned EXACTLY, then tol should
      be ignored.  If canConvertToTriangleMesh() return CANT_CONVERT, this method returns null. */

  public TriangleMesh convertToTriangleMesh(double tol)
  {
    return null;
  }
  
  /** This will be called whenever this object is moved, or the time changes.  Most objects
      will do nothing here, and do not need to override this.  It is available for those
      cases where an object's internal properties depend explicitly on time or on the
      object's position within the scene. */
  
  public void sceneChanged(ObjectInfo info, Scene scene)
  {
  }
  
  /** If the object can be edited by the user, isEditable() should be overridden to return true.
      edit() should then create a window and allow the user to edit the object. */
  
  public boolean isEditable()
  {
    return false;
  }
  
  /** Display a window in which the user can edit this object.
      @param parent   the window from which this command is being invoked
      @param info     the ObjectInfo corresponding to this object
      @param cb       a callback which will be executed when editing is complete.  If the user
                      cancels the operation, it will not be called.
  */
  
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
  }
  
  /** Edit an object which represents a gesture for an Actor object.  realObject
      specifies the object in the scene which this is a gesture for. */

  public void editGesture(EditingWindow parent, ObjectInfo info, Runnable cb, ObjectInfo realObject)
  {
    edit(parent, info, cb);
  }

  /** This method tells whether textures can be assigned to the object.  Objects for which
      it makes no sense to assign a texture (curves, lights, etc.) should override this
      method to return false. */
  
  public boolean canSetTexture()
  {
    return true;
  }
  
  /** This method tells whether materials can be assigned to the object.  The default
      implementation will give the correct result for most objects, but subclasses
      can override this if necessary. */
  
  public boolean canSetMaterial()
  {
    return (canSetTexture() && isClosed());
  }
  
  /** Set the Texture and TextureMapping for this object. */

  public void setTexture(Texture tex, TextureMapping map)
  {
    theTexture = tex;
    texMapping = map;
    if (map instanceof LayeredMapping)
      theTexture = ((LayeredMapping) map).getTexture();
    
    // Update the texture parameters.
    
    if (map == null)
      {
        setParameters(new TextureParameter [0]);
        setParameterValues(new ParameterValue [0]);
      }
    else
      {
        TextureParameter oldParam[] = getParameters();
        ParameterValue oldValue[] = getParameterValues();
        TextureParameter newParam[] = map.getParameters();
        ParameterValue newVal[] = new ParameterValue [newParam.length];
        for (int i = 0; i < newParam.length; i++)
          {
            if (oldParam != null)
              for (int j = 0; j < oldParam.length; j++)
                if (newParam[i].equals(oldParam[j]))
                  {
                    newVal[i] = oldValue[j];
                    break;
                  }
            if (newVal[i] == null)
            {
              if (newParam[i].type == TextureParameter.NORMAL_PARAMETER || !(this instanceof Mesh))
                newVal[i] = new ConstantParameterValue(newParam[i].defaultVal);
              else
                newVal[i] = new VertexParameterValue((Mesh) this, newParam[i]);
            }
          }
        setParameters(newParam);
        setParameterValues(newVal);
      }
  }
  
  /** Get this object's Texture. */
  
  public Texture getTexture()
  {
    return theTexture;
  }
  
  /** Get this object's TextureMapping. */
  
  public TextureMapping getTextureMapping()
  {
    return texMapping;
  }
  
  /** Set the Material and MaterialMapping for this object.  Pass null for both arguments to
      specify that the object does not have a Material. */
  
  public void setMaterial(Material mat, MaterialMapping map)
  {
    theMaterial = mat;
    matMapping = map;
  }
  
  /** Get this object's Material. */
  
  public Material getMaterial()
  {
    return theMaterial;
  }
  
  /** Get this object's MaterialMapping. */
  
  public MaterialMapping getMaterialMapping()
  {
    return matMapping;
  }
  
  /** Get the list of texture parameters for this object. */
  
  public TextureParameter [] getParameters()
  {
    return texParam;
  }
  
  /** Set the list of texture parameters for this object. */
  
  public void setParameters(TextureParameter param[])
  {
    texParam = param;
  }
  
  /** Get the list of objects defining the values of texture parameters. */
  
  public ParameterValue [] getParameterValues()
  {
    return paramValue;
  }
  
  /** Get the average value of each texture parameter. */
  
  public double [] getAverageParameterValues()
  {
    if (paramValue == null)
      return new double [0];
    double d[] = new double [paramValue.length];
    for (int i = 0; i < d.length; i++)
      d[i] = paramValue[i].getAverageValue();
    return d;
  }
  
  /** Set the list of objects defining the values of texture parameters. */
  
  public void setParameterValues(ParameterValue val[])
  {
    paramValue = val;
  }
  
  /** Get the object defining the value of a particular texture parameter.  If the parameter is not
      defined for this object, this returns null. */
  
  public ParameterValue getParameterValue(TextureParameter param)
  {
    for (int i = 0; i < texParam.length; i++)
      if (texParam[i].equals(param))
        return paramValue[i];
    return null;
  }
  
  /** Set the object defining the value of a particular texture parameter. */
  
  public void setParameterValue(TextureParameter param, ParameterValue val)
  {
    for (int i = 0; i < texParam.length; i++)
      if (texParam[i].equals(param))
      {
        paramValue[i] = val;
        return;
      }
  }

  /** Copy all texture and material information from another object to this one.  This method
      is intended to be called by subclasses' implementations of duplicate() and copyObject(). */

  public void copyTextureAndMaterial(Object3D obj)
  {
    if (obj.getTextureMapping() != null)
      setTexture(obj.getTexture(), obj.getTextureMapping().duplicate(this, obj.getTexture()));
    if (obj.getMaterialMapping() != null)
      setMaterial(obj.getMaterial(), obj.getMaterialMapping().duplicate(this, obj.getMaterial()));
    else
      setMaterial(null, null);
    TextureParameter objParam[] = obj.getParameters();
    if (objParam != null)
    {
      TextureParameter thisParam[] = new TextureParameter [objParam.length];
      ParameterValue objValue[] = obj.getParameterValues();
      ParameterValue thisValue[] = new ParameterValue [objValue.length];
      for (int i = 0; i < thisValue.length; i++)
      {
        thisParam[i] = (i < texParam.length ? texParam[i] : objParam[i]);
        thisValue[i] = objValue[i].duplicate();
      }
      setParameters(thisParam);
      setParameterValues(thisValue);
    }
  }
  
  /** Get the skeleton for this object, or null if it does not have one. */
  
  public Skeleton getSkeleton()
  {
    return null;
  }

  /** Objects which can be rendered as part of a scene should override this method to return
      a RenderingMesh which describes the appearance of the object.  All points on the 
      RenderingMesh should be within a distance tol of the true surface.  The interactive flag
      tells whether the resulting Mesh will be rendered in interactive mode.  When interactive
      is set to true, the RenderingMesh should be cached for future use, so that it may be
      rendered repeatedly without needing to be regenerated.
      <p>
      The ObjectInfo contains additional information which may affect how the object is
      rendered, such as it location in the scene, texture parameters, etc.
      <p>
      Objects which cannot be rendered directly (lights, cameras, curves, etc.) do not need
      to override this method. */

  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    return null;
  }
  
  /** Every object should override this method to return a WireframeMesh.  This will be used
      for drawing the object in wireframe mode, and also for drawing "nonrenderable" objects
      in other rendering modes. */
  
  public abstract WireframeMesh getWireframeMesh();

  /**
   * Render this object into a ViewerCanvas.  The default implementation is sufficient for most
   * objects, but subclasses may override this to customize how they are displayed.
   *
   * @param obj      the ObjectInfo for this object
   * @param canvas   the canvas in which to render this object
   * @param viewDir  the direction from which this object is being viewed
   */

  public void renderObject(ObjectInfo obj, ViewerCanvas canvas, Vec3 viewDir)
  {
    if (!obj.isVisible())
      return;
    Camera theCamera = canvas.getCamera();
    if (theCamera.visibility(obj.getBounds()) == Camera.NOT_VISIBLE)
      return;
    int renderMode = canvas.getRenderMode();
    if (renderMode == ViewerCanvas.RENDER_WIREFRAME)
    {
      canvas.renderWireframe(obj.getWireframePreview(), theCamera, ViewerCanvas.lineColor);
      return;
    }
    RenderingMesh mesh = obj.getPreviewMesh();
    if (mesh != null)
    {
      VertexShader shader;
      if (renderMode == ViewerCanvas.RENDER_TRANSPARENT)
      {
        shader = new ConstantVertexShader(ViewerCanvas.transparentColor);
        canvas.renderMeshTransparent(mesh, shader, theCamera, obj.getCoords().toLocal().timesDirection(viewDir), null);
      }
      else
      {
        double time = 0.0;
        if (canvas.getScene() != null)
          time = canvas.getScene().getTime();
        if (renderMode == ViewerCanvas.RENDER_FLAT)
          shader = new FlatVertexShader(mesh, obj.getObject(), time, obj.getCoords().toLocal().timesDirection(viewDir));
        else if (renderMode == ViewerCanvas.RENDER_SMOOTH)
          shader = new SmoothVertexShader(mesh, obj.getObject(), time, obj.getCoords().toLocal().timesDirection(viewDir));
        else
          shader = new TexturedVertexShader(mesh, obj.getObject(), time, obj.getCoords().toLocal().timesDirection(viewDir)).optimize();
        canvas.renderMesh(mesh, shader, theCamera, obj.getObject().isClosed(), null);
      }
    }
    else
      canvas.renderWireframe(obj.getWireframePreview(), theCamera, ViewerCanvas.lineColor);
  }

  /** The following method writes the object's data to an output stream.  Subclasses should
      override this method, but also call super.writeToFile() to save information about
      materials, etc.  In addition to this method, every Object3D must include a constructor 
      with the signature

      public Classname(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
      
      which reconstructs the object by reading its data from an input stream.  This 
      constructor, similarly, should call the overridden constructor to read information
      about materials, etc. */
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(1);
    if (canSetTexture())
      {
        if (theMaterial == null)
          out.writeInt(-1);
        else
          {
            out.writeInt(theScene.indexOf(theMaterial));
            out.writeUTF(matMapping.getClass().getName());
            matMapping.writeToFile(out);
          }
        if (theTexture instanceof LayeredTexture)
          {
            out.writeInt(-1);
            ((LayeredMapping) texMapping).writeToFile(out, theScene);
          }
        else
          {
            out.writeInt(theScene.indexOf(theTexture));
            out.writeUTF(texMapping.getClass().getName());
            texMapping.writeToFile(out);
          }
        for (int i = 0; i < paramValue.length; i++)
        {
          out.writeUTF(paramValue[i].getClass().getName());
          paramValue[i].writeToStream(out);
        }
      }
  }
  
  public Object3D(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    int i;

    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    if (!canSetTexture())
      return;
    i = in.readInt();
    if (i > -1)
      {
        try
          {
            Class mapClass = ArtOfIllusion.getClass(in.readUTF());
            Constructor con = mapClass.getConstructor(new Class [] {DataInputStream.class, Object3D.class, Material.class});
            theMaterial = theScene.getMaterial(i);
            setMaterial(theMaterial, (MaterialMapping) con.newInstance(new Object [] {in, this, theMaterial}));
          }
        catch (Exception ex)
          {
            throw new IOException(ex.getMessage());
          }
      }
    i = in.readInt();
    if (i > -1)
      {
        try
          {
            Class mapClass = ArtOfIllusion.getClass(in.readUTF());
            Constructor con = mapClass.getConstructor(new Class [] {DataInputStream.class, Object3D.class, Texture.class});
            theTexture = theScene.getTexture(i);
            setTexture(theTexture, (TextureMapping) con.newInstance(new Object [] {in, this, theTexture}));
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
          }
      }
    else
      {
        // This is a layered texture.
        
        LayeredTexture tex = new LayeredTexture(this);
        LayeredMapping map = (LayeredMapping) tex.getDefaultMapping(this);
        map.readFromFile(in, theScene);
        setTexture(tex, map);
      }
    paramValue = new ParameterValue [texParam.length];
    if (version > 0)
      for (i = 0; i < paramValue.length; i++)
        paramValue[i] = readParameterValue(in);
    setParameterValues(paramValue);
  }
  
  /** Read in the value of a texture parameter from a stream. */
  
  public static ParameterValue readParameterValue(DataInputStream in) throws IOException
  {
    try
    {
      Class valueClass = ArtOfIllusion.getClass(in.readUTF());
      Constructor con = valueClass.getConstructor(new Class [] {DataInputStream.class});
      return ((ParameterValue) con.newInstance(new Object [] {in}));
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      throw new IOException(ex.getMessage());
    }
  }

  /** Get a list of editable properties defined by this object. */

  public Property[] getProperties()
  {
    return new Property[0];
  }

  /** Get the value of one of this object's editable properties.
      @param index     the index of the property to get
   */

  public Object getPropertyValue(int index)
  {
    return null;
  }

  /** Set the value of one of this object's editable properties.
      @param index     the index of the property to set
      @param value     the value to set for the property
   */

  public void setPropertyValue(int index, Object value)
  {
  }
  
  /** Return a Keyframe which describes the current pose of this object. */
  
  public abstract Keyframe getPoseKeyframe();
  
  /** Modify this object based on a pose keyframe. */
  
  public abstract void applyPoseKeyframe(Keyframe k);
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [0], new double [0], new double [0][2]);
  }
  
  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    new BStandardDialog("", Translate.text("noParamsForKeyframe"), BStandardDialog.INFORMATION).showMessageDialog((Widget) parent);
  }
  
  /** Determine whether the user should be allowed to convert this object to an Actor. */
  
  public boolean canConvertToActor()
  {
    return false;
  }
  
  /** Get a version of this object to which a pose track can be attached.  For most objects,
      this simply returns itself.  Some objects, however, need to be converted to Actors
      to have pose tracks.  If this method returns null, no pose track will be attached. */
  
  public Object3D getPosableObject()
  {
    return this;
  }
}
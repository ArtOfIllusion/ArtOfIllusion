/* Copyright (C) 2004-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import java.io.*;

/** An ObjectWrapper is an Object3D that acts as a wrapper around another Object3D.
    The "wrapped object" is the one which actually defines the geometry for this object. */

public abstract class ObjectWrapper extends Object3D
{
  protected Object3D theObject;
  
  public ObjectWrapper()
  {
    super();
  }
  
  public ObjectWrapper(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);
  }

  /** Get the inner Object3D which is wrapped by this one. */
  
  public Object3D getWrappedObject()
  {
    return theObject;
  }
  
  /** Get a BoundingBox which just encloses the object. */

  public BoundingBox getBounds()
  {
    return theObject.getBounds();
  }

  /** Tells whether the object is closed.  For curves, this means it has no endpoints.  For
      surface, it means the surface has no boundary. */

  public boolean isClosed()
  {
    return theObject.isClosed();
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
    return theObject.canConvertToTriangleMesh();
  }
  
  /** Return a TriangleMesh which reproduces the shape of this object.  If
      canConvertToTriangleMesh() returned APPROXIMATELY, this method should return a
      TriangleMesh which reproduces the object to within the specified tolerance.  That is,
      no point on the mesh should be further than tol from the corresponding point on the
      original surface.  If canConvertToTriangleMesh() returned EXACTLY, then tol should
      be ignored.  If canConvertToTriangleMesh() return CANT_CONVERT, this method returns null. */

  public TriangleMesh convertToTriangleMesh(double tol)
  {
    return theObject.convertToTriangleMesh(tol);
  }
  
  /** This will be called whenever this object is moved, or the time changes.  Most objects
      will do nothing here, and do not need to override this.  It is available for those
      cases where an object's internal properties depend explicitly on time or on the
      object's position within the scene. */
  
  public void sceneChanged(ObjectInfo info, Scene scene)
  {
    theObject.sceneChanged(info, scene);
  }
  
  /** Edit an object which represents a gesture for an Actor object.  realObject
      specifies the object in the scene which this is a gesture for. */

  public void editGesture(EditingWindow parent, ObjectInfo info, Runnable cb, ObjectInfo realObject)
  {
    theObject.editGesture(parent, info, cb, realObject);
  }
    
  /** Get this object's Texture. */
  
  public Texture getTexture()
  {
    return theObject.getTexture();
  }
  
  /** Get this object's TextureMapping. */
  
  public TextureMapping getTextureMapping()
  {
    return theObject.getTextureMapping();
  }
  
  /** Get this object's Material. */
  
  public Material getMaterial()
  {
    return theObject.getMaterial();
  }
  
  /** Get this object's MaterialMapping. */
  
  public MaterialMapping getMaterialMapping()
  {
    return theObject.getMaterialMapping();
  }
  
  /** Get the list of texture parameters for this object. */
  
  public TextureParameter [] getParameters()
  {
    return theObject.getParameters();
  }
  
  /** Set the list of texture parameters for this object. */
  
  public void setParameters(TextureParameter param[])
  {
    theObject.setParameters(param);
  }
  
  /** Get the list of objects defining the values of texture parameters. */
  
  public ParameterValue [] getParameterValues()
  {
    return theObject.getParameterValues();
  }
  
  /** Get the average value of each texture parameter. */
  
  public double [] getAverageParameterValues()
  {
    return theObject.getAverageParameterValues();
  }
  
  /** Set the list of objects defining the values of texture parameters. */
  
  public void setParameterValues(ParameterValue val[])
  {
    theObject.setParameterValues(val);
  }
  
  /** Get the object defining the value of a particular texture parameter.  If the parameter is not
      defined for this object, this returns null. */
  
  public ParameterValue getParameterValue(TextureParameter param)
  {
    return theObject.getParameterValue(param);
  }
  
  /** Set the object defining the value of a particular texture parameter. */
  
  public void setParameterValue(TextureParameter param, ParameterValue val)
  {
    theObject.setParameterValue(param, val);
  }

  /** Get the skeleton for this object, or null if it does not have one. */

  public Skeleton getSkeleton()
  {
    return theObject.getSkeleton();
  }

  /** Objects which can be rendered as part of a scene should override this method to return
      a RenderingMesh which describes the appearance of the object.  All points on the 
      RenderingMesh should be within a distance tol of the true surface.  The interactive flag
      tells whether the resulting Mesh will be rendered in interactive mode.  When interactive
      is set to true, the RenderingMesh should be cached for future use, so that it may be
      rendered repeatedly without needing to be regenerated.
      
      The ObjectInfo contains additional information which may affect how the object is
      rendered, such as it location in the scene, texture parameters, etc.
      
      Objects which cannot be rendered directly (lights, cameras, curves, etc.) do not need
      to override this method. */

  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    return theObject.getRenderingMesh(tol, interactive, info);
  }
  
  /** Every object should override this method to return a WireframeMesh.  This will be used
      for drawing the object in wireframe mode, and also for drawing "nonrenderable" objects
      in other rendering modes. */
  
  public WireframeMesh getWireframeMesh()
  {
    return theObject.getWireframeMesh();
  }

  public void renderObject(ObjectInfo obj, ViewerCanvas canvas, Vec3 viewDir)
  {
    theObject.renderObject(obj, canvas, viewDir);
  }

  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return theObject.getPoseKeyframe();
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    theObject.applyPoseKeyframe(k);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    theObject.configurePoseTrack(track);
  }
  
  /** Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    theObject.editKeyframe(parent, k, info);
  }
}

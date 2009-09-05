/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.image.*;
import artofillusion.image.filter.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** SceneCamera is a type of Object3D.  It represents a camera which the user can position
    within a scene.  It should not be confused with the Camera class. */

public class SceneCamera extends Object3D
{
  private double fov, depthOfField, focalDist;
  private boolean perspective;
  private ImageFilter filter[];
  private int extraComponents;
  
  private static BoundingBox bounds;
  private static WireframeMesh mesh;
  private static final int SEGMENTS = 8;
  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("fieldOfView"), 0.0, 180.0, 30.0),
      new Property(Translate.text("depthOfField"), Double.MIN_VALUE, Double.MAX_VALUE, Camera.DEFAULT_DISTANCE_TO_SCREEN/2.0),
      new Property(Translate.text("focalDist"), Double.MIN_VALUE, Double.MAX_VALUE, Camera.DEFAULT_DISTANCE_TO_SCREEN),
      new Property(Translate.text("Perspective"), true)
  };

  static
  {
    double sine[], cosine[];
    int i, t[], f[], to[], from[], index = 0;
    Vec3 vert[];
    
    bounds = new BoundingBox(-0.25, 0.25, -0.15, 0.20, -0.2, 0.2);
    sine = new double [SEGMENTS];
    cosine = new double [SEGMENTS];
    for (i = 0; i < SEGMENTS; i++)
    {
      sine[i] = Math.sin(i*2.0*Math.PI/SEGMENTS);
      cosine[i] = Math.cos(i*2.0*Math.PI/SEGMENTS);
    }
    vert = new Vec3[24+2*SEGMENTS];
    from = new int [34+3*SEGMENTS];
    to = new int [34+3*SEGMENTS];
    
    // Create the body.
    
    vert[0] = new Vec3(-0.25, -0.15, -0.2);
    vert[1] = new Vec3(0.25, -0.15, -0.2);
    vert[2] = new Vec3(0.25, 0.15, -0.2);
    vert[3] = new Vec3(-0.25, 0.15, -0.2);
    vert[4] = new Vec3(-0.25, -0.15, 0.0);
    vert[5] = new Vec3(0.25, -0.15, 0.0);
    vert[6] = new Vec3(0.25, 0.15, 0.0);
    vert[7] = new Vec3(-0.25, 0.15, 0.0);
    f = new int [] {0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, };
    t = new int [] {1, 2, 3, 0, 5, 6, 7, 4, 4, 5, 6, 7, };
    for (i = 0; i < f.length; i++, index++)
    {
      from[index] = f[i];
      to[index] = t[i];
    }
    
    // Create the shutter.
    
    vert[8] = new Vec3(-0.25, 0.15, -0.15);
    vert[9] = new Vec3(-0.2, 0.15, -0.15);
    vert[10] = new Vec3(-0.2, 0.2, -0.15);
    vert[11] = new Vec3(-0.25, 0.2, -0.15);
    vert[12] = new Vec3(-0.25, 0.15, -0.1);
    vert[13] = new Vec3(-0.2, 0.15, -0.1);
    vert[14] = new Vec3(-0.2, 0.2, -0.1);
    vert[15] = new Vec3(-0.25, 0.2, -0.1);
    for (i = 0; i < f.length; i++, index++)
    {
      from[index] = f[i]+8;
      to[index] = t[i]+8;
    }
    
    // Create the viewfinder.

    vert[16] = new Vec3(-0.2, 0.15, 0.0);
    vert[17] = new Vec3(-0.05, 0.2, 0.0);
    vert[18] = new Vec3(0.05, 0.2, 0.0);
    vert[19] = new Vec3(0.2, 0.15, 0.0);
    vert[20] = new Vec3(0.2, 0.15, -0.2);
    vert[21] = new Vec3(0.05, 0.2, -0.2);
    vert[22] = new Vec3(-0.05, 0.2, -0.2);
    vert[23] = new Vec3(-0.2, 0.15, -0.2);
    f = new int [] {16, 17, 18, 19, 20, 21, 22, 23, 17, 18};
    t = new int [] {17, 18, 19, 20, 21, 22, 23, 16, 22, 21};
    for (i = 0; i < f.length; i++, index++)
    {
      from[index] = f[i];
      to[index] = t[i];
    }
    
    // Create the lens.
    
    for (i = 0; i < SEGMENTS; i++, index++)
    {
      vert[24+i] = new Vec3(0.1*cosine[i], 0.1*sine[i], 0.0);
      vert[24+i+SEGMENTS] = new Vec3(0.1*cosine[i], 0.1*sine[i], 0.2);
      from[index] = 24+i;
      to[index] = 24+(i+1)%SEGMENTS;
      from[index+SEGMENTS] = 24+i;
      to[index+SEGMENTS] = 24+i+SEGMENTS;
      from[index+2*SEGMENTS] = 24+i+SEGMENTS;
      to[index+2*SEGMENTS] = 24+(i+1)%SEGMENTS+SEGMENTS;
    }    
    mesh = new WireframeMesh(vert, from, to);
  }
  
  public SceneCamera()
  {
    fov = 30.0;
    depthOfField = Camera.DEFAULT_DISTANCE_TO_SCREEN/2.0;
    focalDist = Camera.DEFAULT_DISTANCE_TO_SCREEN;
    perspective = true;
    filter = new ImageFilter [0];
  }
  
  public double getFieldOfView()
  {
    return fov;
  }

  public void setFieldOfView(double fieldOfView)
  {
    fov = fieldOfView;
  }

  public double getDepthOfField()
  {
    return depthOfField;
  }

  public void setDepthOfField(double dof)
  {
    depthOfField = dof;
  }

  public double getFocalDistance()
  {
    return focalDist;
  }

  public void setFocalDistance(double dist)
  {
    focalDist = dist;
  }

  public boolean isPerspective()
  {
    return perspective;
  }

  public void setPerspective(boolean perspective)
  {
    this.perspective = perspective;
  }

  /** Get the list of ImageFilters for this camera. */
  
  public ImageFilter [] getImageFilters()
  {
    ImageFilter filt[] = new ImageFilter [filter.length];
    for (int i = 0; i < filter.length; i++)
      filt[i] = filter[i];
    return filt;
  }

  /** Set the list of ImageFilters for this camera. */
  
  public void setImageFilters(ImageFilter filters[])
  {
    filter = filters;
  }

  /**
   * Get a list of additional image components, beyond those required by the camera's filters,
   * which should be included in rendered images.  This is a sum of the constants defined in
   * ComplexImage.
   */

  public int getExtraRequiredComponents()
  {
    return extraComponents;
  }

  /**
   * Set a list of additional image components, beyond those required by the camera's filters,
   * which should be included in rendered images.  This is a sum of the constants defined in
   * ComplexImage.
   */

  public void setExtraRequiredComponents(int components)
  {
    extraComponents = components;
  }

  /**
   * Get a list of all image components that should be included in rendered images.  This includes
   * all those required by this camera's filters, as well as ones specified by
   * setExtraRequiredComponents().  This is a sum of the constants defined in ComplexImage.
   */
  
  public int getComponentsForFilters()
  {
    int components = extraComponents;
    for (int i = 0; i < filter.length; i++)
      components |= filter[i].getDesiredComponents();
    return components;
  }
  
  /** Apply all of this camera's filters to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param coords     the position of this camera in the scene
  */
  
  public void applyImageFilters(ComplexImage image, Scene scene, CoordinateSystem coords)
  {
    for (int i = 0; i < filter.length; i++)
      filter[i].filterImage(image, scene, this, coords);
    image.rebuildImage();
  }

  /**
   * Get the transform which maps between view coordinates and screen coordinates for this camera.
   *
   * @param width    the image width in pixels
   * @param height   the image height in pixels
   */

  public Mat4 getScreenTransform(int width, int height)
  {
    if (perspective)
    {
      double scale = 0.5*height/Math.tan(getFieldOfView()*Math.PI/360.0);
      Mat4 screenTransform = Mat4.scale(-scale, -scale, scale).times(Mat4.perspective(0.0));
      screenTransform = Mat4.translation((double) width/2.0, (double) height/2.0, 0.0).times(screenTransform);
      return screenTransform;
    }
    else
    {
      double scale = 0.5*height/(Math.tan(getFieldOfView()*Math.PI/360.0)*getFocalDistance());
      Mat4 screenTransform = Mat4.scale(-scale, -scale, scale).times(Mat4.identity());
      screenTransform = Mat4.translation((double) width/2.0, (double) height/2.0, 0.0).times(screenTransform);
      return screenTransform;
    }
  }

  /**
   * Compute a ray from the camera location through a point in its field of, represented in the camera's local
   * coordinate system.
   *
   * @param x          the x coordinate of the point in the plane z=1 through which the ray passes
   * @param y          the y coordinate of the point in the plane z=1 through which the ray passes
   * @param dof1       this is used for simulating depth of field.  dof1 and dof2 are independent values uniformly distributed
   *                   between 0 and 1.  Together, they select the point on the camera which should serve as the ray's origin.
   * @param origin     on exit, this contains the ray origin
   * @param direction  on exit, this contains the normalized ray direction
   */

  public void getRayFromCamera(double x, double y, double dof1, double dof2, Vec3 origin, Vec3 direction)
  {
    if (perspective)
    {
      origin.set(0.0, 0.0, 0.0);
      double scale = focalDist*2.0*Math.tan(getFieldOfView()*Math.PI/360.0);
      if (dof1 != 0.0)
      {
        double angle = dof1*2.0*Math.PI;
        double dofScale = 0.01*scale*dof2*focalDist/depthOfField;
        origin.x = dofScale*Math.cos(angle);
        origin.y = dofScale*Math.sin(angle);
      }
      direction.set(-x*scale-origin.x, -y*scale-origin.y, focalDist);
      direction.normalize();
    }
    else
    {
      double scale = focalDist*2.0*Math.tan(getFieldOfView()*Math.PI/360.0);
      origin.set(-scale*x, -scale*y, 0);
      if (dof1 != 0.0)
      {
        double angle = dof1*2.0*Math.PI;
        double dofScale = 0.01*scale*dof2*focalDist/depthOfField;
        double dx = dofScale*Math.cos(angle);
        double dy = dofScale*Math.sin(angle);
        origin.x += dx;
        origin.y += dy;
        direction.set(-dx, -dy, focalDist);
        direction.normalize();
      }
      else
        direction.set(0.0, 0.0, 1.0);
    }
  }

  public SceneCamera duplicate()
  {
    SceneCamera sc = new SceneCamera();
    
    sc.fov = fov;
    sc.depthOfField = depthOfField;
    sc.focalDist = focalDist;
    sc.perspective = perspective;
    sc.filter = new ImageFilter [filter.length];
    for (int i = 0; i < filter.length; i++)
      sc.filter[i] = filter[i].duplicate();
    return sc;
  }
  
  public void copyObject(Object3D obj)
  {
    SceneCamera sc = (SceneCamera) obj;

    fov = sc.fov;
    depthOfField = sc.depthOfField;
    focalDist = sc.focalDist;
    perspective = sc.perspective;
    filter = new ImageFilter [sc.filter.length];
    for (int i = 0; i < filter.length; i++)
      filter[i] = sc.filter[i].duplicate();
  }

  public BoundingBox getBounds()
  {
    return bounds;
  }

  /* A SceneCamera has no size, so calls to setSize() are ignored. */

  public void setSize(double xsize, double ysize, double zsize)
  {
  }

  public boolean canSetTexture()
  {
    return false;
  }
  
  public WireframeMesh getWireframeMesh()
  {
    return mesh;
  }
  
  /** Create a Camera object representing the view through this SceneCamera.
      @param width     the width of the image viewed through the Camera
      @param height    the height of the image viewed through the Camera
      @param coords    the CoordinateSystem of this SceneCamera
      @return an appropriately configured Camera
  */
  
  public Camera createCamera(int width, int height, CoordinateSystem coords)
  {
    Camera cam = new Camera();
    cam.setCameraCoordinates(coords.duplicate());
    cam.setScreenTransform(getScreenTransform(width, height), width, height);
    return cam;
  }
  
  /** This is a utility method which synchronously renders an image of the scene
      from the viewpoint of this camera. */

  public ComplexImage renderScene(Scene theScene, int width, int height, Renderer rend, CoordinateSystem cameraPos)
  {
    Camera cam = createCamera(width, height, cameraPos);
    final ComplexImage theImage[] = new ComplexImage [1];
    RenderListener rl = new RenderListener() {
      public void imageUpdated(Image image)
      {
      }
      public void statusChanged(String status)
      {
      }
      public synchronized void imageComplete(ComplexImage image)
      {
        theImage[0] = image;
        notify();
      }
      public void renderingCanceled()
      {
        notify();
      }
    };
    rend.renderScene(theScene, cam, rl, this);
    synchronized (rl)
    {
      try
      {
        rl.wait();
      }
      catch (InterruptedException ex)
      {
        rend.cancelRendering(theScene);
        return null;
      }
    }
    applyImageFilters(theImage[0], theScene, cameraPos);
    return theImage[0];
  }

  public boolean isEditable()
  {
    return true;
  }

  public void edit(final EditingWindow parent, final ObjectInfo info, Runnable cb)
  {
    final ValueSlider fovSlider = new ValueSlider(0.0, 180.0, 90, fov);
    final ValueField dofField = new ValueField(depthOfField, ValueField.POSITIVE);
    final ValueField fdField = new ValueField(focalDist, ValueField.POSITIVE);
    BCheckBox perspectiveBox = new BCheckBox(Translate.text("Perspective"), perspective);
    BButton filtersButton = Translate.button("filters", new Object() {
      void processEvent()
      {
        SceneCamera temp = SceneCamera.this.duplicate();
        temp.fov = fovSlider.getValue();
        temp.depthOfField = dofField.getValue();
        temp.focalDist = fdField.getValue();
        new CameraFilterDialog(parent, temp, info.getCoords());
        filter = temp.filter;
      }
    }, "processEvent");
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editCameraTitle"),
		new Widget[] {fovSlider, dofField, fdField, perspectiveBox, filtersButton},
		new String[] {Translate.text("fieldOfView"), Translate.text("depthOfField"), Translate.text("focalDist"), null, null});
    if (dlg.clickedOk())
    {
      fov = fovSlider.getValue();
      depthOfField = dofField.getValue();
      focalDist = fdField.getValue();
      perspective = perspectiveBox.getState();
    }
    
    // If there are any Pose tracks for this object, they need to have their subtracks updated
    // to reflect the current list of filters.
    
    Scene sc = parent.getScene();
    for (int i = 0; i < sc.getNumObjects(); i++)
      if (sc.getObject(i).getObject() == this)
      {
        // This ObjectInfo corresponds to this SceneCamera.  Check each of its tracks.
        
        ObjectInfo obj = sc.getObject(i);
        for (int j = 0; j < obj.getTracks().length; j++)
          if (obj.getTracks()[j] instanceof PoseTrack)
          {
            // This is a Pose track, so update its subtracks.
            
            PoseTrack pose = (PoseTrack) obj.getTracks()[j];
            Track old[] = pose.getSubtracks();
            Track newtracks[] = new Track [filter.length];
            for (int k = 0; k < filter.length; k++)
            {
              Track existing = null;
              for (int m = 0; m < old.length && existing == null; m++)
                if (old[m] instanceof FilterParameterTrack && ((FilterParameterTrack) old[m]).getFilter() == filter[k])
                  existing = old[m];
              if (existing == null)
                existing = new FilterParameterTrack(pose, filter[k]);
              newtracks[k] = existing;
            }
            pose.setSubtracks(newtracks);
          }
      }
    if (parent instanceof LayoutWindow)
      ((LayoutWindow) parent).getScore().rebuildList();
    cb.run();
  }

  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public SceneCamera(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version < 0 ||version > 2)
      throw new InvalidObjectException("");
    fov = in.readDouble();
    depthOfField = in.readDouble();
    focalDist = in.readDouble();
    if (version < 2)
      perspective = true;
    else
      perspective = in.readBoolean();
    if (version == 0)
      filter = new ImageFilter [0];
    else
    {
      filter = new ImageFilter [in.readInt()];
      try
      {
        for (int i = 0; i < filter.length; i++)
        {
          Class cls = ArtOfIllusion.getClass(in.readUTF());
          filter[i] = (ImageFilter) cls.newInstance();
          filter[i].initFromStream(in, theScene);
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        throw new IOException();
      }
    }
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(2);
    out.writeDouble(fov);
    out.writeDouble(depthOfField);
    out.writeDouble(focalDist);
    out.writeBoolean(perspective);
    out.writeInt(filter.length);
    for (int i = 0; i < filter.length; i++)
    {
      out.writeUTF(filter[i].getClass().getName());
      filter[i].writeToStream(out, theScene);
    }
  }

  public Property[] getProperties()
  {
    return PROPERTIES.clone();
  }

  public Object getPropertyValue(int index)
  {
    switch (index)
    {
      case 0:
        return fov;
      case 1:
        return depthOfField;
      case 2:
        return focalDist;
      case 3:
        return perspective;
    }
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
    if (index == 0)
      fov = (Double) value;
    else if (index == 1)
      depthOfField = (Double) value;
    else if (index == 2)
      focalDist = (Double) value;
    else if (index == 3)
      perspective = (Boolean) value;
  }

  /* Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new CameraKeyframe(fov, depthOfField, focalDist);
  }
  
  /* Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    CameraKeyframe key = (CameraKeyframe) k;
    
    fov = key.fov;
    depthOfField = key.depthOfField;
    focalDist = key.focalDist;
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"Field of View", "Depth of Field", "Focal Distance"},
        new double [] {fov, depthOfField, focalDist}, 
        new double [][] {{0.0, 180.0}, {0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}});
    FilterParameterTrack subtrack[] = new FilterParameterTrack [filter.length];
    for (int i = 0; i < subtrack.length; i++)
      subtrack[i] = new FilterParameterTrack(track, filter[i]);
    track.setSubtracks(subtrack);
  }

  /* Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    CameraKeyframe key = (CameraKeyframe) k;
    ValueSlider fovSlider = new ValueSlider(0.0, 180.0, 90, key.fov);
    ValueField dofField = new ValueField(key.depthOfField, ValueField.POSITIVE);
    ValueField fdField = new ValueField(key.focalDist, ValueField.POSITIVE);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editCameraTitle"),
		new Widget[] {fovSlider, dofField, fdField},
		new String[] {Translate.text("fieldOfView"), Translate.text("depthOfField"), Translate.text("focalDist")});
    if (!dlg.clickedOk())
      return;
    key.fov = fovSlider.getValue();
    key.depthOfField = dofField.getValue();
    key.focalDist = fdField.getValue();
  }

  /* Inner class representing a pose for a cylinder. */
  
  public static class CameraKeyframe implements Keyframe
  {
    public double fov, depthOfField, focalDist;
    
    public CameraKeyframe(double fov, double depthOfField, double focalDist)
    {
      this.fov = fov;
      this.depthOfField = depthOfField;
      this.focalDist = focalDist;
    }
    
    /* Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return new CameraKeyframe(fov, depthOfField, focalDist);
    }
    
    /* Create a duplicate of this keyframe for a (possibly different) object. */
  
    public Keyframe duplicate(Object owner)
    {
      return new CameraKeyframe(fov, depthOfField, focalDist);
    }
  
    /* Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      return new double [] {fov, depthOfField, focalDist};
    }
  
    /* Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
      fov = values[0];
      depthOfField = values[1];
      focalDist = values[2];
    }

    /* These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      CameraKeyframe k2 = (CameraKeyframe) o2;

      return new CameraKeyframe(weight1*fov+weight2*k2.fov, weight1*depthOfField+weight2*k2.depthOfField, 
        weight1*focalDist+weight2*k2.focalDist);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      CameraKeyframe k2 = (CameraKeyframe) o2, k3 = (CameraKeyframe) o3;

      return new CameraKeyframe(weight1*fov+weight2*k2.fov+weight3*k3.fov, 
        weight1*depthOfField+weight2*k2.depthOfField+weight3*k3.depthOfField, 
        weight1*focalDist+weight2*k2.focalDist+weight3*k3.focalDist);
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      CameraKeyframe k2 = (CameraKeyframe) o2, k3 = (CameraKeyframe) o3, k4 = (CameraKeyframe) o4;

      return new CameraKeyframe(weight1*fov+weight2*k2.fov+weight3*k3.fov+weight4*k4.fov, 
        weight1*depthOfField+weight2*k2.depthOfField+weight3*k3.depthOfField+weight4*k4.depthOfField, 
        weight1*focalDist+weight2*k2.focalDist+weight3*k3.focalDist+weight4*k4.focalDist);
    }

    /* Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof CameraKeyframe))
        return false;
      CameraKeyframe key = (CameraKeyframe) k;
      return (key.fov == fov && key.depthOfField == depthOfField && key.focalDist == focalDist);
    }
  
    /* Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeDouble(fov);
      out.writeDouble(depthOfField);
      out.writeDouble(focalDist);
    }

    /* Reconstructs the keyframe from its serialized representation. */

    public CameraKeyframe(DataInputStream in, Object parent) throws IOException
    {
      this(in.readDouble(), in.readDouble(), in.readDouble());
    }
  }
}
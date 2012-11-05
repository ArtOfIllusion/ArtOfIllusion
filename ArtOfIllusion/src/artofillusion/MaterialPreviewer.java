/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.image.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.event.*;

/** MaterialPreviewer is a component used for renderering previews of Materials.  It displays
    a scene consisting of a Sphere with the desired Material applied to it, a ground plane,
    and a single light.  Optionally, an Object3D may be specified which will then be used
    instead of a Sphere. */

public class MaterialPreviewer extends CustomWidget implements RenderListener
{
  Scene theScene;
  Camera theCamera;
  ObjectInfo info;
  CoordinateSystem objectCoords;
  Image theImage;
  boolean mouseInside, renderInProgress;
  Point clickPoint;
  private Mat4 dragTransform;
  private Object3D shape[];

  public static final int HANDLE_SIZE = 5;
  static final double DRAG_SCALE = Math.PI/360.0;

  /** Create a MaterialPreviewer to display a Texture and/or Material mapped to a sphere.
      Either tex or mat may be null. */

  public MaterialPreviewer(Texture tex, Material mat, int width, int height)
  {
    shape = new Object3D [] {
        new Sphere(1.0, 1.0, 1.0),
        new Cube(2.0, 2.0, 2.0),
        new Cylinder(2.0, 1.0, 1.0, 1.0),
        new Cylinder(2.0, 1.0, 1.0, 0.0)
      };
    ObjectInfo objInfo = new ObjectInfo(shape[0], new CoordinateSystem(), "");
    initObject(tex, mat, objInfo);
    init(objInfo, width, height);
  }
  
  /** Same as above, except you can specify a different object to use instead of a sphere. */

  public MaterialPreviewer(Texture tex, Material mat, Object3D obj, int width, int height)
  {
    ObjectInfo objInfo = new ObjectInfo(obj, new CoordinateSystem(), "");
    initObject(tex, mat, objInfo);
    init(objInfo, width, height);
  }

  /** Create a MaterialPreviewer to display the specified object, with its current texture
      and material. */

  public MaterialPreviewer(ObjectInfo obj, int width, int height)
  {
    init(obj.duplicate(), width, height);
  }
  
  /** Initialize the object's texture and material. */
  
  private void initObject(Texture tex, Material mat, ObjectInfo objInfo)
  {
    if (tex == null)
      tex = UniformTexture.invisibleTexture();
    objInfo.setTexture(tex, tex.getDefaultMapping(objInfo.getObject()));
    if (mat != null)
      objInfo.setMaterial(mat, mat.getDefaultMapping(objInfo.getObject()));
  }
  
  /** Initialize the MaterialPreviewer. */
  
  private void init(ObjectInfo obj, int width, int height)
  {
    BoundingBox bounds = obj.getBounds();
    Vec3 size = bounds.getSize();
    double max = Math.max(size.x, Math.max(size.y, size.z))/2.0;
    double floor = -bounds.getSize().length()/2.0;
    CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, 10.0*max), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    if (max > 10.0)
      max = 10.0;
    Vec3 vert[] = new Vec3 [] {new Vec3(100.0*max, floor, 100.0*max), new Vec3(-100.0*max, floor, 100.0*max), new Vec3(0.0, floor, -100.0*max)};
    int face[][] = {{0, 1, 2}};
    TriangleMesh tri;

    theScene = new Scene();
    theCamera = new Camera();
    theCamera.setCameraCoordinates(coords);
    coords = new CoordinateSystem(new Vec3(), new Vec3(-0.5, -0.4, -1.0), Vec3.vy());
    theScene.addObject(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 0.8f), coords, "", null);
    coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    theScene.addObject(tri = new TriangleMesh(vert, face), coords, "", null);
    Texture tex = theScene.getDefaultTexture();
    tri.setTexture(tex, tex.getDefaultMapping(tri));
    info = obj;
    info.setCoords(new CoordinateSystem());
    objectCoords = info.getCoords();
    theScene.addObject(info, null);
    setPreferredSize(new Dimension(width, height));
    addEventLink(MousePressedEvent.class, this, "mousePressed");
    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
    addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    addEventLink(MouseEnteredEvent.class, this, "mouseEntered");
    addEventLink(MouseExitedEvent.class, this, "mouseExited");
    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
    addEventLink(RepaintEvent.class, this, "paint");

    // Set up other listeners.
    
    getComponent().addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent ev)
      {
        render();
      }
    });
    getComponent().addHierarchyListener(new HierarchyListener() {
      public void hierarchyChanged(HierarchyEvent ev)
      {
        if ((ev.getChangeFlags()&HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
          if (!getComponent().isDisplayable())
          {
            Renderer rend = ArtOfIllusion.getPreferences().getTexturePreviewRenderer();
            if (rend != null)
              rend.cancelRendering(theScene);
          }
      }
    });
    render();
  }
  
  /** Get the object on which the texture and material are being displayed. */
  
  public ObjectInfo getObject()
  {
    return info;
  }

  /** Get the scene being rendererd as the preview. */

  public Scene getScene()
  {
    return theScene;
  }
  
  /* The following methods are used to modify the properties of the object being displayed. */

  public void setTexture(Texture tex, TextureMapping map)
  {
    if (tex == null)
      tex = UniformTexture.invisibleTexture();
    if (map == null)
      map = tex.getDefaultMapping(info.getObject());
    info.setTexture(tex, map);
  }
  
  public void setMaterial(Material mat, MaterialMapping map)
  {
    info.setMaterial(mat, map);
  }

  /** Render the preview. */

  public synchronized void render()
  {
    Renderer rend = ArtOfIllusion.getPreferences().getTexturePreviewRenderer();
    if (rend == null)
      return;
    rend.cancelRendering(theScene);
    Rectangle bounds = getBounds();
    if (bounds.width == 0 || bounds.height == 0)
      return;
    SceneCamera sc = new SceneCamera();
    sc.setFieldOfView(16.0);
    theCamera.setScreenTransform(sc.getScreenTransform(bounds.width, bounds.height), bounds.width, bounds.height);
    rend.configurePreview();
    rend.renderScene(theScene, theCamera, this, sc);
    renderInProgress = true;
    repaint();
  }

  /** Cancel rendering. */
  
  public synchronized void cancelRendering()
  {
    Renderer rend = ArtOfIllusion.getPreferences().getTexturePreviewRenderer();
    if (rend != null)
      rend.cancelRendering(theScene);
  }

  private void paint(RepaintEvent ev)
  {
    Graphics2D g = ev.getGraphics();
    if (theImage != null)
      g.drawImage(theImage, 0, 0, getComponent());
    if (mouseInside)
      drawHilight(g);
    if (renderInProgress)
      {
        Rectangle bounds = getBounds();
        g.setColor(Color.red);
        g.drawRect(0, 0, bounds.width-1, bounds.height-1);
      }
  }

  private void drawHilight(Graphics g)
  {
    Rectangle bounds = getBounds();
    g.setColor(Color.red);
    g.fillRect(0, 0, HANDLE_SIZE, HANDLE_SIZE);
    g.fillRect(bounds.width-HANDLE_SIZE, 0, HANDLE_SIZE, HANDLE_SIZE);
    g.fillRect(0, bounds.height-HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
    g.fillRect(bounds.width-HANDLE_SIZE, bounds.height-HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
  }

  private void drawObject(Graphics g)
  {
    g.setColor(Color.gray);
    Vec3 origin = objectCoords.getOrigin();
    Mat4 m = objectCoords.fromLocal();
    m = Mat4.translation(-origin.x, -origin.y, -origin.z).times(m);
    m = dragTransform.times(m);
    m = Mat4.translation(origin.x, origin.y, origin.z).times(m);
    theCamera.setObjectTransform(m);
    WireframeMesh mesh = info.getObject().getWireframeMesh();
    int from[] = mesh.from, to[] = mesh.to, last = -1;
    Vec3 vert[] = mesh.vert;
    for (int i = 0; i < mesh.from.length; i++)
    {
      if (from[i] == last)
        theCamera.drawClippedLineTo(g, vert[(last=to[i])]);
      else
        theCamera.drawClippedLine(g, vert[from[i]], vert[(last=to[i])]);
    }
  }
  
  /** Rotate the object to show a specific side. */
  
  private void changeView(int view)
  {
    double angles[][] = new double [][] {
        {0.0, 0.0, 0.0}, 
        {0.0, 180.0, 0.0}, 
        {0.0, -90.0, 0.0}, 
        {0.0, 90.0, 0.0}, 
        {-90.0, 0.0, 0.0}, 
        {90.0, 0.0, 0.0}, 
    };
    objectCoords.setOrientation(angles[view][0], angles[view][1], angles[view][2]);
    objectCoords.setOrigin(new Vec3());
    render();
  }
  
  /** Change what object the texture/material is displayed ob. */
  
  private void changeObject(int object)
  {
    shape[object].setTexture(info.getObject().getTexture(), info.getObject().getTextureMapping());
    shape[object].setMaterial(info.getObject().getMaterial(), info.getObject().getMaterialMapping());
    info.setObject(shape[object]);
    info.clearCachedMeshes();
    render();
  }

  /** Called when more pixels are available for the current image. */
  
  public void imageUpdated(Image image)
  {
    theImage = image;
    repaint();
  }

  /** The renderer may call this method periodically during rendering, to give the listener text descriptions
      of the current status of rendering. */
  
  public void statusChanged(String status)
  {
  }

  /** Called when rendering is complete. */
  
  public void imageComplete(ComplexImage image)
  {
    theImage = image.getImage();
    renderInProgress = false;
    repaint();
  }
  
  /** Called when rendering is cancelled. */
  
  public void renderingCanceled()
  {
  }

  private void mouseEntered(MouseEnteredEvent e)
  {
    mouseInside = true;
    Graphics g = getComponent().getGraphics();
    drawHilight(g);
    g.dispose();
  }
  
  private void mouseExited(MouseExitedEvent e)
  {
    mouseInside = false;
    repaint();
  }
  
  private void mousePressed(MousePressedEvent e)
  {
    Graphics g = getComponent().getGraphics();
    clickPoint = e.getPoint();
    Renderer rend = ArtOfIllusion.getPreferences().getTexturePreviewRenderer();
    if (rend != null)
      rend.cancelRendering(theScene);
    dragTransform = Mat4.identity();
    drawObject(g);
    g.dispose();
  }

  private void mouseReleased(MouseReleasedEvent e)
  {
    if (clickPoint == null)
      return;
    Point dragPoint = e.getPoint();
    if (!clickPoint.equals(dragPoint))
    {
      if (e.isMetaDown())
      {
        if (e.isControlDown())
          dragTransform = Mat4.translation(0.0, 0.0, (dragPoint.y-clickPoint.y)*0.01);
        else
          dragTransform = Mat4.translation((dragPoint.x-clickPoint.x)*0.01, (clickPoint.y-dragPoint.y)*0.01, 0.0);
        objectCoords.transformOrigin(dragTransform);
      }
      else
      {
        Vec3 rotAxis = new Vec3((clickPoint.y-dragPoint.y)*DRAG_SCALE, (dragPoint.x-clickPoint.x)*DRAG_SCALE, 0.0);
        double angle = rotAxis.length();
        rotAxis = rotAxis.times(1.0/angle);
        rotAxis = theCamera.getViewToWorld().timesDirection(rotAxis);
        dragTransform = Mat4.axisRotation(rotAxis, angle);
        objectCoords.transformAxes(dragTransform);
      }
    }
    render();
  }

  private void mouseClicked(MouseClickedEvent e)
  {
    if (e.getClickCount() == 2)
      showConfigurationDialog();
  }

  private void mouseDragged(MouseDraggedEvent e)
  {
    if (clickPoint == null)
      return;
    Graphics g = getComponent().getGraphics();
    Point dragPoint = e.getPoint();
    if (e.isMetaDown())
    {
      if (e.isControlDown())
        dragTransform = Mat4.translation(0.0, 0.0, (dragPoint.y-clickPoint.y)*0.01);
      else
        dragTransform = Mat4.translation((dragPoint.x-clickPoint.x)*0.01, (clickPoint.y-dragPoint.y)*0.01, 0.0);
    }
    else
    {
      Vec3 rotAxis = new Vec3((clickPoint.y-dragPoint.y)*DRAG_SCALE, (dragPoint.x-clickPoint.x)*DRAG_SCALE, 0.0);
      double angle = rotAxis.length();
      rotAxis = rotAxis.times(1.0/angle);
      rotAxis = theCamera.getViewToWorld().timesDirection(rotAxis);
      dragTransform = Mat4.axisRotation(rotAxis, angle);
    }
    g.drawImage(theImage, 0, 0, getComponent());
    drawHilight(g);
    drawObject(g);
    g.dispose();
  }

  /**
   * Show a dialog for configuring the view.
   */

  private void showConfigurationDialog()
  {
    BComboBox viewChoice = new BComboBox(new String [] {
      "",
      Translate.text("Front"),
      Translate.text("Back"),
      Translate.text("Left"),
      Translate.text("Right"),
      Translate.text("Top"),
      Translate.text("Bottom")
    });
    BComboBox shapeChoice = new BComboBox(new String [] {
      Translate.text("menu.sphere"),
      Translate.text("menu.cube"),
      Translate.text("menu.cylinder"),
      Translate.text("menu.cone")
    });
    ComponentsDialog dlg;
    if (shape == null)
      dlg = new ComponentsDialog(UIUtilities.findWindow(this), Translate.text("configurePreview"),
          new Widget [] {viewChoice}, new String [] {Translate.text("resetViewTo")});
    else
    {
      for (int i = 0; i < shape.length; i++)
      if (shape[i] == info.getObject())
        shapeChoice.setSelectedIndex(i);
      dlg = new ComponentsDialog(UIUtilities.findWindow(this), Translate.text("configurePreview"),
          new Widget [] {shapeChoice, viewChoice}, new String [] {Translate.text("Shape"), Translate.text("resetViewTo")});
    }
    if (!dlg.clickedOk())
      return;
    if (shape != null && shape[shapeChoice.getSelectedIndex()] != info.getObject())
      changeObject(shapeChoice.getSelectedIndex());
    if (viewChoice.getSelectedIndex() > 0)
      changeView(viewChoice.getSelectedIndex()-1);
  }
}

/* Copyright (C) 2002-2009 by Peter Eastman

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
import java.awt.event.*;

/** UVMappingViewer appears in the UVMappingWindow, and is used for editing the
    UV texture coordinates at each vertex of a mesh. */

public class UVMappingViewer extends MeshViewer
{
  private Texture2D tex;
  private UVMappingWindow window;
  private UVMesh uvmesh;
  private ObjectInfo meshInfo;
  private UVEditController controller;
  private double minu, maxu, minv, maxv, time, param[];
  private int component, sampling, deselect;
  private boolean selected[], dragging, draggingSelectionBox;
  private Point screenVert[];
  private Vec2 coord[];
  private int vertIndex[];
  
  private static final int MARKER_SIZE = 4;
  
  public UVMappingViewer(Texture2D tex, UVMappingWindow window, double minu, double maxu, double minv, double maxv, int component, int sampling, double time, double param[])
  {
    super(window, new RowContainer());
    this.tex = tex;
    this.window = window;
    this.time = time;
    this.param = param;
    uvmesh = new UVMesh();
    meshInfo = new ObjectInfo(uvmesh, new CoordinateSystem(), "");
    controller = new UVEditController();
    screenVert = new Point [0];
    selected = new boolean [0];
    setParameters(minu, maxu, minv, maxv, component, sampling);
    setShowTemplate(true);
    getComponent().addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e)
      {
        createImage();
      }
    });
  }
  
  /** Get the controller for editing the UV mesh. */
  
  public MeshEditController getController()
  {
    return controller;
  }

  /** Set the parameters for what part of the texture to display. */
  
  public void setParameters(double minu, double maxu, double minv, double maxv)
  {
    setParameters(minu, maxu, minv, maxv, component, sampling);
  }
  
  /** Set the parameters for what part of the texture to display. */
  
  public void setParameters(double minu, double maxu, double minv, double maxv, int component, int sampling)
  {
    this.minu = minu;
    this.maxu = maxu;
    this.minv = minv;
    this.maxv = maxv;
    this.component = component;
    this.sampling = sampling;
    adjustCamera();
    createImage();
    calcScreenPositions();
    repaint();
    window.displayRangeChanged();
  }

  /** Recalculate the camera settings. */
  
  private void adjustCamera()
  {
    Rectangle dim = getBounds();
    double uscale = dim.width/(maxu-minu);
    double vscale = dim.height/(maxv-minv);
    theCamera.setScreenParamsParallel(1.0, dim.width, dim.height);
    Mat4 worldToView = Mat4.scale(-uscale, vscale, 1.0).times(Mat4.translation(-minu-0.5*dim.width/uscale, -maxv+0.5*dim.height/vscale, 0.0));
    Mat4 viewToWorld = Mat4.translation(minu+0.5*dim.width/uscale, maxv-0.5*dim.height/vscale, 0.0).times(Mat4.scale(-1.0/uscale, 1.0/vscale, 1.0));
    theCamera.setViewTransform(worldToView, viewToWorld);
  }
  
  /** Recalculate the texture image. */
  
  private void createImage()
  {
    Rectangle dim = getBounds();
    if (dim.width < 1 || dim.height < 1)
      return;
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    double uoffset = 0.5*sampling*(maxu-minu)/dim.width;
    double voffset = 0.5*sampling*(maxv-minv)/dim.height;
    Image theImage = ((Texture2D) tex.duplicate()).createComponentImage(minu+uoffset, maxu+uoffset, minv-voffset, maxv-voffset,
        dim.width/sampling, dim.height/sampling, component, time, param);
    if (sampling > 1)
      theImage = theImage.getScaledInstance(dim.width, dim.height, Image.SCALE_SMOOTH);
    setTemplateImage(theImage);
    adjustCamera();
    setCursor(Cursor.getDefaultCursor());
  }
  
  /** Estimate the range of depth values that the camera will need to render.  This need not be exact,
      but should err on the side of returning bounds that are slightly too large.
      @return the two element array {minDepth, maxDepth}
   */
   
  public double[] estimateDepthRange()
  {
    return new double [] {-1.0, 1.0};
  }

  /** Calculate the screen location of each vertex. */
  
  private void calcScreenPositions()
  {
    Rectangle dim = getBounds();
    double uscale = dim.width/(maxu-minu);
    double vscale = dim.height/(maxv-minv);
    screenVert = new Point [uvmesh.vert.length];
    for (int i = 0; i < uvmesh.vert.length; i++)
      {
        int x = (int) ((uvmesh.vert[i].r.x-minu)*uscale-0.5);
        int y = (int) ((maxv-uvmesh.vert[i].r.y)*vscale-0.5);
        screenVert[i] = new Point(x, y);
      }
  }

  public synchronized void updateImage()
  {
    adjustCamera();
    for (int i = 0; i < screenVert.length; i++)
    {
      int x = screenVert[i].x;
      int y = screenVert[i].y;
      drawBox(x-MARKER_SIZE, y-1, 2*MARKER_SIZE+1, 3, Color.white);
      drawBox(x-1, y-MARKER_SIZE, 3, 2*MARKER_SIZE+1, Color.white);
      drawBox(x-MARKER_SIZE+1, y, 2*MARKER_SIZE-1, 1, selected[i] ? Color.red : Color.black);
      drawBox(x, y-MARKER_SIZE+1, 1, 2*MARKER_SIZE-1, selected[i] ? Color.red : Color.black);
    }
    currentTool.drawOverlay(this);
  }
  
  /** Unused method from object viewer. */

  protected void drawObject()
  {
  }

  /** Get a list of which vertices are selected.  The array length is equal to the
      total number of vertices in the object being edited, not just the ones
      currently displayed in this viewer. */
  
  public boolean [] getSelection()
  {
    boolean sel[] = new boolean [coord.length];
    for (int i = 0; i < selected.length; i++)
      if (selected[i])
        sel[vertIndex[i]] = true;
    return sel;
  }

  /** Set the Mesh object for this viewer. */
  
  public void setMesh(Mesh mesh)
  {
    window.setMesh(mesh);
  }
  
  /** Rebuild the list of vertices to display. */
  
  public void setDisplayedVertices(Vec2 coord[], boolean display[])
  {
    this.coord = coord;
    int count = 0;
    for (int i = 0; i < display.length; i++)
      if (display[i])
        count++;
    vertIndex = new int [count];
    count = 0;
    for (int i = 0; i < display.length; i++)
      if (display[i])
        vertIndex[count++] = i;
    updateVertexPositions(coord);
    selected = new boolean [vertIndex.length];
    window.selectionDistance = null;
    repaint();
  }
  
  /** Update the positions of the displayed vertices. */
  
  public void updateVertexPositions(Vec2 coord[])
  {
    Vec2 shown[] = new Vec2 [vertIndex.length];
    for (int i = 0; i < shown.length; i++)
      shown[i] = coord[vertIndex[i]];
    uvmesh.setVertices(shown);
    calcScreenPositions();
  }
  
  /**
   * Get the minimum U value.
   */
  
  public double getMinU()
  {
    return minu;
  }
  
  /**
   * Get the maximum U value.
   */

   public double getMaxU()
  {
    return maxu;
  }
  
  /**
   * Get the minimum V value.
   */

   public double getMinV()
  {
    return minv;
  }
  
  /**
   * Get the maximum V value.
   */

  public double getMaxV()
  {
    return maxv;
  }

  /**
   * Get whether a mouse drag is currently in progress.
   */

  public boolean isDragInProgress()
  {
    return dragging;
  }

  /** This is called whenever the mesh has changed. */
  
  public void objectChanged()
  {
    calcScreenPositions();
    window.getObject().clearCachedMeshes();
    for (int i = 0; i < vertIndex.length; i++)
      coord[vertIndex[i]] = new Vec2(uvmesh.vert[i].r.x, uvmesh.vert[i].r.y);
    window.setTextureCoords(coord);
    window.updateTextFields();
  }

  /** When the user presses the mouse, forward events to the current tool as appropriate.
      If this is a vertex based tool, allow them to select or deselect vertices. */

  protected void mousePressed(WidgetMouseEvent e)
  {
    sentClick = true;
    deselect = -1;
    dragging = false;
    clickPoint = e.getPoint();
    
    // Determine which tool is active.
    
    if (metaTool != null && e.isMetaDown())
      activeTool = metaTool;
    else if (altTool != null && e.isAltDown())
      activeTool = altTool;
    else
      activeTool = currentTool;

    // If the current tool wants all clicks, just forward the event and return.

    if ((activeTool.whichClicks() & EditingTool.ALL_CLICKS) != 0)
      {
        activeTool.mousePressed(e, this);
        dragging = true;
      }
    boolean allowSelectionChange = activeTool.allowSelectionChanges();
    boolean wantHandleClicks = ((activeTool.whichClicks() & EditingTool.HANDLE_CLICKS) != 0);
    if (!allowSelectionChange && !wantHandleClicks)
      return;

    // Determine what the click was on.
    
    int i = findClickTarget(e.getPoint());

    // If the click was not on an object, start dragging a selection box.
    
    if (i == -1)
      {
        draggingSelectionBox = true;
        beginDraggingSelection(e.getPoint(), false);
        sentClick = false;
        return;
      }

    // If the click was on a selected object, forward it to the current tool.  If it was a
    // shift-click, the user may want to deselect it, so set a flag.
    
    if (selected[i])
      {
        if (e.isShiftDown())
          deselect = i;
        activeTool.mousePressedOnHandle(e, this, 0, i);
        return;
      }
    
    // The click was on an unselected object.  Select it and send an event to the current tool.
    
    if (!e.isShiftDown())
      for (int k = 0; k < selected.length; k++)
        selected[k] = false;
    selected[i] = true;
    window.selectionDistance = null;
    currentTool.getWindow().updateMenus();
    if (e.isShiftDown())
      {
        sentClick = false;
        repaint();
      }
    else
      activeTool.mousePressedOnHandle(e, this, 0, i);
  }

  protected void mouseDragged(WidgetMouseEvent e)
  {
    if (!dragging)
    {
      Point p = e.getPoint();
      if (Math.abs(p.x-clickPoint.x) < 2 && Math.abs(p.y-clickPoint.y) < 2)
        return;
    }
    dragging = true;
    deselect = -1;
    super.mouseDragged(e);
  }

  protected void mouseReleased(WidgetMouseEvent e)
  {
    endDraggingSelection();
    if (draggingSelectionBox && !e.isShiftDown() && !e.isControlDown())
      for (int i = 0; i < selected.length; i++)
        selected[i] = false;

    // If the user was dragging a selection box, then select or deselect anything 
    // it intersects.
    
    if (selectBounds != null)
    {
      boolean newsel = !e.isControlDown();
      for (int i = 0; i < selected.length; i++)
        if (selectionRegionContains(screenVert[i]))
          selected[i] = newsel;
    }
    draggingBox = draggingSelectionBox = false;

    // Send the event to the current tool, if appropriate.

    boolean wasDragging = dragging;
    dragging = false;
    if (sentClick)
    {
      if (!wasDragging)
      {
        Point p = e.getPoint();
        e.translatePoint(clickPoint.x-p.x, clickPoint.y-p.y);
      }
      activeTool.mouseReleased(e, this);
    }

    // If the user shift-clicked a selected point and released the mouse without dragging,
    // then deselect the point.

    if (deselect > -1)
      selected[deselect] = false;
    window.selectionDistance = null;
    currentTool.getWindow().updateMenus();
    repaint();
  }

  protected void processMouseScrolled(MouseScrolledEvent ev)
  {
    int amount = ev.getWheelRotation();
    if (!ev.isAltDown())
      amount *= 10;
    if (ArtOfIllusion.getPreferences().getReverseZooming())
      amount *= -1;
    double factor = Math.pow(1.01, -amount);
    double midu = (minu+maxu)/2;
    double midv = (minv+maxv)/2;
    double newminu = ((minu - midu)/factor) + midu;
    double newmaxu = ((maxu - midu)/factor) + midu;
    double newminv = ((minv - midv)/factor) + midv;
    double newmaxv = ((maxv - midv)/factor) + midv;
    setParameters(newminu, newmaxu, newminv, newmaxv);
  }

  /** Determine which point was clicked on. */

  private int findClickTarget(Point pos)
  {
    MeshVertex v[] = uvmesh.getVertices();

    for (int i = v.length-1; i >= 0; i--)
      {
        int dx = (int) (screenVert[i].x-pos.x);
        if (dx < -MARKER_SIZE || dx > MARKER_SIZE)
          continue;
        int dy = (int) (screenVert[i].y-pos.y);
        if (dy < -MARKER_SIZE || dy > MARKER_SIZE)
          continue;
        return i;
      }
    return -1;
  }

  /** Inner class representing the vertices being displayed. */
  
  private static class UVMesh extends Object3D implements Mesh
  {
    public MeshVertex vert[];
    
    public UVMesh()
    {
      vert = new MeshVertex [0];
    }

    public UVMesh(Vec2 uv[])
    {
      vert = new MeshVertex [uv.length];
      for (int i = 0; i < vert.length; i++)
        vert[i] = new MeshVertex(new Vec3(uv[i].x, uv[i].y, 0.0));
    }

    /** Create a new object which is an exact duplicate of this one. */
  
    public Object3D duplicate()
    {
      Vec2 uv[] = new Vec2 [vert.length];
      for (int i = 0; i < vert.length; i++)
        uv[i] = new Vec2(vert[i].r.x, vert[i].r.y);
      return new UVMesh(uv);
    }
  
    /** Copy all the properties of another object, to make this one identical to it.  If the
        two objects are of different classes, this will throw a ClassCastException. */
    
    public void copyObject(Object3D obj)
    {
      UVMesh mesh = (UVMesh) obj;
      vert = new MeshVertex [mesh.vert.length];
      for (int i = 0; i < mesh.vert.length; i++)
        vert[i] = new MeshVertex(new Vec3(mesh.vert[i].r));
    }

    /** Get the list of vertices which define the mesh. */
  
    public MeshVertex[] getVertices()
    {
      return vert;
    }
  
    /** Get a list of the positions of all vertices which define the mesh. */
    
    public Vec3 [] getVertexPositions()
    {
      Vec3 v[] = new Vec3 [vert.length];
      for (int i = 0; i < v.length; i++)
        v[i] = new Vec3(vert[i].r);
      return v;
    }
  
    /** Set the positions for all the vertices of the mesh. */
  
    public void setVertexPositions(Vec3 v[])
    {
      vert = new MeshVertex [v.length];
      for (int i = 0; i < v.length; i++)
        vert[i] = new MeshVertex(v[i]);
    }
  
    /** Set the positions for all the vertices of the mesh. */
  
    public void setVertices(Vec2 v[])
    {
      vert = new MeshVertex [v.length];
      for (int i = 0; i < v.length; i++)
        vert[i] = new MeshVertex(new Vec3(v[i].x, v[i].y, 0.0));
    }
  
    /** Unused method from Object3D. */
  
    public BoundingBox getBounds()
    {
      return null;
    }
  
    /** Unused method from Object3D. */
  
    public void setSize(double xsize, double ysize, double zsize)
    {
    }
    
    /** Unused method from Object3D. */
    
    public WireframeMesh getWireframeMesh()
    {
      return null;
    }
  
    /** Unused method from Object3D. */
  
    public Keyframe getPoseKeyframe()
    {
      return null;
    }
  
    /** Unused method from Object3D. */
  
    public void applyPoseKeyframe(Keyframe k)
    {
    }
      
    /** Unused method from Mesh. */
      
    public Vec3 [] getNormals()
    {
      return null;
    }
  
    /** Unused method from Mesh. */
    
    public Skeleton getSkeleton()
    {
      return null;
    }
    
    /** Unused method from Mesh. */
  
    public void setSkeleton(Skeleton s)
    {
    }
    
    /** Unused method from Mesh. */
    
    public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options)
    {
      return null;
    }
  }
  
  /** Inner class which is the controller for editing the mesh. */
  
  private class UVEditController implements MeshEditController
  {
    /** Get the object being edited in this window. */
    
    public ObjectInfo getObject()
    {
      return meshInfo;
    }
    
    
    /** Set the mesh being edited. */
    
    public void setMesh(Mesh mesh)
    {
    }
    
    /** This should be called whenever the object has changed. */
    
    public void objectChanged()
    {
      UVMappingViewer.this.objectChanged();
    }
    
    /** The return value has no meaning, since there is only one selection mode in this window. */
        
    public int getSelectionMode()
    {
      return POINT_MODE;
    }
    
    /** This is ignored, since there is only one selection mode in this window. */
        
    public void setSelectionMode(int mode)
    {
    }
    
    /** Get an array of flags specifying which parts of the object are selected. */
    
    public boolean[] getSelection()
    {
      return selected;
    }
    
    /** Set an array of flags specifying which parts of the object are selected.  Depending on the selection mode
        and type of object, this may correspond to vertices, faces, edges, etc. */
    
    public void setSelection(boolean selected[])
    {
      UVMappingViewer.this.selected = selected;
    }

    /** Selection distance is simply 0 if the vertex is selected, and -1 otherwise. */

    public int[] getSelectionDistance()
    {
      int selectionDistance[] = new int [selected.length];
      for (int i = 0; i < selected.length; i++)
        selectionDistance[i] = (selected[i] ? 0 : -1);
      return selectionDistance;
    }

    /** Get the mesh tension level. */

    public double getMeshTension()
    {
      return 1.0;
    }

    /** Get the distance over which mesh tension applies. */

    public int getTensionDistance()
    {
      return 0;
    }
  }
}
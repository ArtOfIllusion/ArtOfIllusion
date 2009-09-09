/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import java.awt.*;

/** The Camera class has two functions.  First, it keeps track of the transformations
    between the various coordinate systems used by the program.  These include:
    <p>
    <ul>
    <li>Object Coordinates are used for defining the shape of individual objects.  Each object
    has its own local coordinate system.</li>
    <li>World Coordinates are used for defining the arrangement of objects to form a scene.</li>
    <li>View Coordinates describe the positions of objects relative to the viewer.
    Specifically, the viewer is considered to be at the origin, and to be looking along the
    positive z axis.</li>
    <li>Screen coordinates correspond to pixels in the image being generated.</li>
    <p>
    Second, the camera class is responsible for drawing various simple shapes.  These include
    straight lines, rectangular boxes, and Bezier curves. 
    <p>
    The Camera class does *not* represent a "camera" which the user creates and positions
    within a scene.  That is represented by the SceneCamera class. */

public class Camera implements Cloneable
{
  private Mat4 objectToWorld, objectToView, objectToScreen, worldToView, worldToScreen;
  private Mat4 viewToScreen, viewToWorld;
  private double viewDist, distToScreen, scale, frontClipPlane, gridSpacing;
  private boolean perspective;
  private int hres, vres;
  private int lastX, lastY;
  private Vec3 lastPoint;
  private CoordinateSystem cameraCoords;

  public static final double DEFAULT_DISTANCE_TO_SCREEN = 20.0;

  public Camera()
  {
    objectToWorld = objectToView = worldToView = viewToWorld = Mat4.identity();
    distToScreen = DEFAULT_DISTANCE_TO_SCREEN;
    setScreenParams(0.0, 100.0, 100, 100);
  }

  /**
   * Set the distance from the camera to the screen.
   *
   * @deprecated use setScreenParams() instead
   */

  public void setDistToScreen(double dist)
  {
    double oldScale = scale/distToScreen;
    
    distToScreen = dist;
    frontClipPlane = dist/20.0;
    if (perspective)
      setScreenParams(viewDist, oldScale, hres, vres);
    else
      setScreenParamsParallel(scale, hres, vres);
  }
  
  public double getDistToScreen()
  {
    return distToScreen;
  }
  
  public double getClipDistance()
  {
    return frontClipPlane;
  }
  
  public void setClipDistance(double distance)
  {
    frontClipPlane = distance;
  }
  
  /** Determine whether the camera is in perspective or parallel projection mode. */
  
  public boolean isPerspective()
  {
    return perspective;
  }
  
  /** Create a duplicate of this camera. */
  
  public Camera duplicate()
  {
    Camera c = null;
    try
    {
      c = (Camera) clone();
    }
    catch (CloneNotSupportedException ex)
    {
    }
    c.cameraCoords = cameraCoords.duplicate();
    return c;
  }

  /**
   * Set the transformation which maps from view coordinates to screen coordinates.
   *
   * @param screenTransform the transformation from view coordinates to screen coordinates
   * @param width           the screen width in pixels
   * @param height          the screen height in pixels
   */

  public void setScreenTransform(Mat4 screenTransform, int width, int height)
  {
    hres = width;
    vres = height;
    viewToScreen = screenTransform;
    worldToScreen = viewToScreen.times(worldToView);
    objectToScreen = worldToScreen.times(objectToWorld);
  }
  
  /**
   * Set the camera to perspective mode with the specified parameters.
   */

  public void setScreenParams(double newViewDist, double newScale, int newHres, int newVres)
  {
    viewDist = newViewDist;
    scale = newScale*distToScreen;
    Mat4 screenTransform = Mat4.scale(-scale, -scale, scale).times(Mat4.perspective(newViewDist));
    screenTransform = Mat4.translation((double) hres/2.0, (double) vres/2.0, 0.0).times(screenTransform);
    setScreenTransform(screenTransform, newHres, newVres);
    frontClipPlane = distToScreen/20.0;
    perspective = true;
  }
  
  /**
   * Set the camera to parallel projection mode with the specified parameters.
   */

  public void setScreenParamsParallel(double newScale, int newHres, int newVres)
  {
    scale = newScale;
    Mat4 screenTransform = Mat4.scale(-scale, -scale, scale);
    screenTransform = Mat4.translation((double) newHres/2.0, (double) newVres/2.0, 0.0).times(screenTransform);
    setScreenTransform(screenTransform, newHres, newVres);
    frontClipPlane = -Double.MAX_VALUE;
    perspective = false;
  }
  
  /**
   * Set the dimension's of the camera's viewport.
   *
   * @deprecated use setScreenTransform(), setScreenParams() or setScreenParamsParallel() instead
   */

  public void setSize(int newHres, int newVres)
  {
    hres = newHres;
    vres = newVres;
    if (perspective)
      viewToScreen = Mat4.perspective(viewDist);
    else
      viewToScreen = Mat4.identity();
    viewToScreen = Mat4.scale(-scale, -scale, scale).times(viewToScreen);
    viewToScreen = Mat4.translation((double) hres/2.0, (double) vres/2.0, 0.0).times(viewToScreen);
    worldToScreen = viewToScreen.times(worldToView);
    objectToScreen = worldToScreen.times(objectToWorld);
  }
  
  /** Get the dimension's of the camera's viewport. */

  public Dimension getSize()
  {
    return new Dimension(hres, vres);
  }

  /** Set the grid spacing. */

  public void setGrid(double spacing)
  {
    gridSpacing = spacing;
  }

  /** Set the camera's coordinate system. */

  public void setCameraCoordinates(CoordinateSystem coords)
  {
    worldToView = coords.toLocal();
    viewToWorld = coords.fromLocal();
    objectToView = worldToView.times(objectToWorld);
    worldToScreen = viewToScreen.times(worldToView);
    objectToScreen = worldToScreen.times(objectToWorld);
    cameraCoords = coords;
  }

  /** Explicitly set the transformation between world and view coordinates. */

  public void setViewTransform(Mat4 worldToView, Mat4 viewToWorld)
  {
    this.worldToView = worldToView;
    this.viewToWorld = viewToWorld;
    objectToView = worldToView.times(objectToWorld);
    worldToScreen = viewToScreen.times(worldToView);
    objectToScreen = worldToScreen.times(objectToWorld);
  }
  
  /** Get the camera's coordinate system. */
  
  public CoordinateSystem getCameraCoordinates()
  {
    return cameraCoords;
  }
  
  /** Set the transformation for converting object coordinates to world coordinates. */

  public void setObjectTransform(Mat4 m)
  {
    objectToWorld = m;
    objectToView = worldToView.times(objectToWorld);
    objectToScreen = worldToScreen.times(objectToWorld);    
  }

  /** The following routines return the various transformation matrices maintained by
      the Camera object. */

  public final Mat4 getObjectToWorld()
  {
    return objectToWorld;
  }

  public final Mat4 getObjectToView()
  {
    return objectToView;
  }

  public final Mat4 getObjectToScreen()
  {
    return objectToScreen;
  }
  
  public final Mat4 getWorldToView()
  {
    return worldToView;
  }

  public final Mat4 getWorldToScreen()
  {
    return worldToScreen;
  }
  
  public final Mat4 getViewToScreen()
  {
    return viewToScreen;
  }
  
  public final Mat4 getViewToWorld()
  {
    return viewToWorld;
  }
  
  /** Given a point in screen coordinates, find the corresponding point in world coordinates
      which is at a specified depth from the viewpoint.  If the grid is turned on, the
      oisuruib us adjusted based on it. */

  public Vec3 convertScreenToWorld(Point p, double depth)
  {
    return convertScreenToWorld(new Vec2(p.x, p.y), depth, true);
  }
  
  /** Given a point in screen coordinates, find the corresponding point in world coordinates
      which is at a specified depth from the viewpoint.  The optional parameter snapToGrid
      specifies whether the position should be adjusted based on the current grid (if the
      grid is turned on). */

  public Vec3 convertScreenToWorld(Point p, double depth, boolean snapToGrid)
  {
    return convertScreenToWorld(new Vec2(p.x, p.y), depth, snapToGrid);
  }

  /** Given a point in screen coordinates, find the corresponding point in world coordinates
      which is at a specified depth from the viewpoint.  The optional parameter snapToGrid
      specifies whether the position should be adjusted based on the current grid (if the
      grid is turned on). */

  public Vec3 convertScreenToWorld(Vec2 p, double depth, boolean snapToGrid)
  {
    Vec2 v1, v2;
    Vec3 v3;
    
    // Construct two points in view coordinates at the specified depth, and convert them
    // to screen coordinates.
    
    v1 = viewToScreen.timesXY(new Vec3(0.0, 0.0, depth));
    v2 = viewToScreen.timesXY(new Vec3(1.0, 1.0, depth));

    // Now solve for the specified point in view coordinates, and convert it to world
    // coordinates.
    
    v3 =  new Vec3((p.x-v1.x)/(v2.x-v1.x), (p.y-v1.y)/(v2.y-v1.y), depth);
    if (snapToGrid && gridSpacing > 0.0)
      {
        v3.x  = Math.floor(v3.x/gridSpacing + 0.5) * gridSpacing;
        v3.y  = Math.floor(v3.y/gridSpacing + 0.5) * gridSpacing;
      }
    return viewToWorld.times(v3);
  }

  /** This is provided for backward compatibility.  The version that takes doubles as
      arguments is preferred. */

  public Vec3 findDragVector(Vec3 p, int dx, int dy)
  {
    return findDragVector(p, (double) dx, (double) dy);
  }

  /** The following routine is used for dragging objects on the screen.  Given a point p
      (specified in world coordinates) which has been dragged by (dx, dy) pixels, find the
      displacement vector in world coordinates. */
  
  public Vec3 findDragVector(Vec3 p, double dx, double dy)
  {
    Vec3 v1, v2;
    Vec2 v3, v4, p1;
    double a, b;
    
    // All motion should be parallel to the x and y axes of view coordinates, so find out
    // what these are in world coordinates.
    
    v1 = viewToWorld.timesDirection(Vec3.vx());
    v2 = viewToWorld.timesDirection(Vec3.vy());
    
    // Now find what they are in screen coordinates.
    
    p1 = worldToScreen.timesXY(p);
    v3 = (worldToScreen.timesXY(p.plus(v1))).minus(p1);
    v4 = (worldToScreen.timesXY(p.plus(v2))).minus(p1);
    
    // Now solve the equations.
    
    b = (v3.x*dy-v3.y*dx)/(v3.x*v4.y-v4.x*v3.y);
    a = (dx-b*v4.x)/v3.x;
    if (gridSpacing > 0.0)
      {
        a = Math.floor(a/gridSpacing + 0.5) * gridSpacing;
        b = Math.floor(b/gridSpacing + 0.5) * gridSpacing;
      }
    return (v1.times(a)).plus(v2.times(b));
  }
  
  /** Given a bounding box (specified in object coordinates), return a rectangle which
      describes the object's position on the screen.  If the object is not visible, 
      return null. */
  
  public Rectangle findScreenBounds(BoundingBox bb)
  {
    Vec3 corner[] = bb.getCorners();
    Vec2 p;
    int i;
    double minx = hres, miny = vres, maxx = -1.0, maxy = -1.0;
    boolean clipped = true;
    double z;
    
    for (i = 0; i < 8; i++)
      {
        p = objectToScreen.timesXY(corner[i]);
        z = objectToView.timesZ(corner[i]);
        if (!perspective || z > frontClipPlane)
          {
            clipped = false;
            if (p.x < minx) minx = p.x;
            if (p.x > maxx) maxx = p.x;
            if (p.y < miny) miny = p.y;
            if (p.y > maxy) maxy = p.y;
          }
        else
          {
            if (p.x < hres/2.0) maxx = hres;
            else
              minx = -1.0;
            if (p.y < vres/2.0) maxy = vres;
            else
              miny = -1.0;
          }
        }
    if (clipped || minx == hres || miny == vres || maxx == -1.0 || maxy == -1.0)
      return null;
    return new Rectangle((int) minx, (int) miny, (int) (Math.ceil(maxx)-minx), (int) (Math.ceil(maxy)-miny));
  }

  public static final int NOT_VISIBLE = 0;
  public static final int NEEDS_CLIPPING = 1;
  public static final int VISIBLE = 2;

  /** Given a bounding box (specified in object coordinates), determine whether the object is
  visible.  It returns one of the following values:
  <ul>
  <li>NOT_VISIBLE: The entire bounding box is offscreen.  The object does not need to be drawn.</li>
  <li>NEEDS_CLIPPING: The object is partly visible, but at least one corner of the box lies
  in front of the clipping plane.  It should be drawn using the clipping drawing routines.</li>
  <li>VISIBLE: The object is entirely in front of the viewer, and can be drawn with the
  faster (non-clipping) drawing routines.</li>
  </ul>
  */

  public int visibility(BoundingBox bb)
  {
    Vec3 corner[] = bb.getCorners();
    Vec2 p;
    boolean offLeft = true, offRight = true, offTop = true, offBottom = true;
    int i, clippedCount = 0;
    
    for (i = 0; i < 8; i++)
      if (perspective && objectToView.timesZ(corner[i]) <= frontClipPlane)
        clippedCount++;
    if (clippedCount == 8)
      return NOT_VISIBLE;
    for (i = 0; i < 8; i++)
      {
        p = objectToScreen.timesXY(corner[i]);
        if (p.x > 0.0) offLeft = false;
        if (p.y > 0.0) offTop = false;
        if (p.x < hres) offRight = false;
        if (p.y < vres) offBottom = false;
        if (!(offLeft | offTop | offRight | offBottom))
          {
          if (clippedCount == 0)
            return VISIBLE;
          else
            return NEEDS_CLIPPING;
          }
      }
    return NOT_VISIBLE;
  }
  
  /** Draw a line between two points (specified in object coordinates). */
  
  public void drawLine(Graphics g, Vec3 from, Vec3 to)
  {
    double w;
    int x, y;
    final Mat4 m = objectToScreen;
    
    w = m.m41*to.x + m.m42*to.y + m.m43*to.z + m.m44;
    lastX = (int) ((m.m11*to.x + m.m12*to.y + m.m13*to.z + m.m14)/w);
    lastY = (int) ((m.m21*to.x + m.m22*to.y + m.m23*to.z + m.m24)/w);
    w = m.m41*from.x + m.m42*from.y + m.m43*from.z + m.m44;
    x = (int) ((m.m11*from.x + m.m12*from.y + m.m13*from.z + m.m14)/w);
    y = (int) ((m.m21*from.x + m.m22*from.y + m.m23*from.z + m.m24)/w);
    g.drawLine(x, y, lastX, lastY);
  }
  
  /** Same as above, except clip the line to the front clipping plane. */

  public void drawClippedLine(Graphics g, Vec3 from, Vec3 to)
  {
    Vec3 p1, p2;
    Vec2 p3, p4;
    double fract;
    
    p1 = objectToView.times(from);
    p2 = objectToView.times(to);
    lastPoint = p2;
    if (perspective)
      {
        if (p1.z <= frontClipPlane && p2.z <= frontClipPlane)
          return;
        if (p1.z < frontClipPlane)
          {
            fract = (frontClipPlane-p1.z)/(p2.z-p1.z);
            p1 = p1.times(1.0-fract).plus(p2.times(fract));
          }
        else if (p2.z < frontClipPlane)
          {
            fract = (frontClipPlane-p2.z)/(p1.z-p2.z);
            p2 = p2.times(1.0-fract).plus(p1.times(fract));
          }
      }
    p3 = viewToScreen.timesXY(p1);
    p4 = viewToScreen.timesXY(p2);
    g.drawLine((int) p3.x, (int) p3.y, (int) p4.x, (int) p4.y);
  }
  
  /** Draw a line from the endpoint of the previous line to a new point. */
  
  public void drawLineTo(Graphics g, Vec3 to)
  {
    double w;
    int x, y;
    final Mat4 m = objectToScreen;
    
    w = m.m41*to.x + m.m42*to.y + m.m43*to.z + m.m44;
    x = (int) ((m.m11*to.x + m.m12*to.y + m.m13*to.z + m.m14)/w);
    y = (int) ((m.m21*to.x + m.m22*to.y + m.m23*to.z + m.m24)/w);
    g.drawLine(lastX, lastY, x, y);
    lastX = x;
    lastY = y;
  }
  
  /** Same as above, except clip the line to the front clipping plane. */

  public void drawClippedLineTo(Graphics g, Vec3 to)
  {
    Vec3 p1, p2;
    Vec2 p3, p4;
    double fract;
    
    p1 = lastPoint;
    p2 = objectToView.times(to);
    lastPoint = p2;
    if (perspective)
      {
        if (p1.z <= frontClipPlane && p2.z <= frontClipPlane)
          return;
        if (p1.z < frontClipPlane)
          {
            fract = (frontClipPlane-p1.z)/(p2.z-p1.z);
            p1 = p1.times(1.0-fract).plus(p2.times(fract));
          }
        else if (p2.z < frontClipPlane)
          {
            fract = (frontClipPlane-p2.z)/(p1.z-p2.z);
            p2 = p2.times(1.0-fract).plus(p1.times(fract));
          }
      }
    p3 = viewToScreen.timesXY(p1);
    p4 = viewToScreen.timesXY(p2);
    g.drawLine((int) p3.x, (int) p3.y, (int) p4.x, (int) p4.y);
  }
  
  /** Draw a bounding box (specified in object coordinates). */
  
  public void drawBox(Graphics g, BoundingBox bb)
  {
    Vec3 corner[] = bb.getCorners();
    Vec2 v;
    int i, x[] = new int [8], y[] = new int [8];
    
    for (i = 0; i < 8; i++)
      {
        v = objectToScreen.timesXY(corner[i]);
        x[i] = (int) v.x;
        y[i] = (int) v.y;
      }
    g.drawLine(x[0], y[0], x[1], y[1]);
    g.drawLine(x[1], y[1], x[3], y[3]);
    g.drawLine(x[3], y[3], x[2], y[2]);
    g.drawLine(x[2], y[2], x[0], y[0]);
    g.drawLine(x[4], y[4], x[5], y[5]);
    g.drawLine(x[5], y[5], x[7], y[7]);
    g.drawLine(x[7], y[7], x[6], y[6]);
    g.drawLine(x[6], y[6], x[4], y[4]);
    g.drawLine(x[0], y[0], x[4], y[4]);
    g.drawLine(x[1], y[1], x[5], y[5]);
    g.drawLine(x[2], y[2], x[6], y[6]);
    g.drawLine(x[3], y[3], x[7], y[7]);
  }

  /** Same as above, except clip all the edges of the box to the front clipping plane. */

  public void drawClippedBox(Graphics g, BoundingBox bb)
  {
    Vec3 corner[] = bb.getCorners();
    
    drawClippedLine(g, corner[0], corner[1]);
    drawClippedLineTo(g, corner[3]);
    drawClippedLineTo(g, corner[2]);
    drawClippedLineTo(g, corner[0]);
    drawClippedLineTo(g, corner[4]);
    drawClippedLineTo(g, corner[5]);
    drawClippedLineTo(g, corner[7]);
    drawClippedLineTo(g, corner[6]);
    drawClippedLineTo(g, corner[4]);
    drawClippedLine(g, corner[1], corner[5]);
    drawClippedLine(g, corner[2], corner[6]);
    drawClippedLine(g, corner[3], corner[7]);
  }
  
  /** Draw a cubic Bezier curve, using the four specified control vertices.  We do this by
      forward differencing.  For speed, we always subdivide the curve into eight segments.
      This should be enough for interactive purposes, and allows the forward differencing
      coefficients to be precalculated. */
     
  public void drawBezier(Graphics g, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4)
  {
    double x1, x2, x3, x4, y1, y2, y3, y4, w1, w2, w3, w4;
    double dx1, dx2, dx3;
    double dy1, dy2, dy3;
    double dw1, dw2, dw3;
    int i, h, v, oldh, oldv;
    Mat4 m = objectToScreen;

    x1 = m.m11*v1.x+m.m12*v1.y+m.m13*v1.z+m.m14;
    x2 = m.m11*v2.x+m.m12*v2.y+m.m13*v2.z+m.m14;
    x3 = m.m11*v3.x+m.m12*v3.y+m.m13*v3.z+m.m14;
    x4 = m.m11*v4.x+m.m12*v4.y+m.m13*v4.z+m.m14;
    y1 = m.m21*v1.x+m.m22*v1.y+m.m23*v1.z+m.m24;
    y2 = m.m21*v2.x+m.m22*v2.y+m.m23*v2.z+m.m24;
    y3 = m.m21*v3.x+m.m22*v3.y+m.m23*v3.z+m.m24;
    y4 = m.m21*v4.x+m.m22*v4.y+m.m23*v4.z+m.m24;
    w1 = m.m41*v1.x+m.m42*v1.y+m.m43*v1.z+m.m44;
    w2 = m.m41*v2.x+m.m42*v2.y+m.m43*v2.z+m.m44;
    w3 = m.m41*v3.x+m.m42*v3.y+m.m43*v3.z+m.m44;
    w4 = m.m41*v4.x+m.m42*v4.y+m.m43*v4.z+m.m44;
    dx1 = -0.330078125*x1 + 0.287109375*x2 + 0.041015625*x3 + 0.001953125*x4;
    dx2 = 0.08203125*x1 - 0.15234375*x2 + 0.05859375*x3 + 0.01171875*x4;
    dx3 = 0.01171875*(x4-x1) + 0.03515625*(x2-x3);
    dy1 = -0.330078125*y1 + 0.287109375*y2 + 0.041015625*y3 + 0.001953125*y4;
    dy2 = 0.08203125*y1 - 0.15234375*y2 + 0.05859375*y3 + 0.01171875*y4;
    dy3 = 0.01171875*(y4-y1) + 0.03515625*(y2-y3);
    dw1 = -0.330078125*w1 + 0.287109375*w2 + 0.041015625*w3 + 0.001953125*w4;
    dw2 = 0.08203125*w1 - 0.15234375*w2 + 0.05859375*w3 + 0.01171875*w4;
    dw3 = 0.01171875*(w4-w1) + 0.03515625*(w2-w3);
    oldh = (int) (x1/w1);
    oldv = (int) (y1/w1);
    for (i = 0; i < 8; i++)
      {
        x1 += dx1;  dx1 += dx2;  dx2 += dx3;
        y1 += dy1;  dy1 += dy2;  dy2 += dy3;
        w1 += dw1;  dw1 += dw2;  dw2 += dw3;
        h = (int) (x1/w1);
        v = (int) (y1/w1);
        g.drawLine(oldh, oldv, h, v);
        oldh = h;
        oldv = v;
      }
  }

  /** Same as above, except clip the curve to the front clipping plane. */

  public void drawClippedBezier(Graphics g, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4)
  {
    Vec3 pos;
    double dx1, dx2, dx3;
    double dy1, dy2, dy3;
    double dz1, dz2, dz3;
    boolean clip1, clip2, clip3, clip4;
    int i;

    clip1 = (perspective && objectToView.timesZ(v1) < frontClipPlane);
    clip2 = (perspective && objectToView.timesZ(v2) < frontClipPlane);
    clip3 = (perspective && objectToView.timesZ(v3) < frontClipPlane);
    clip4 = (perspective && objectToView.timesZ(v4) < frontClipPlane);
    if (clip1 && clip2 && clip3 && clip4)
      return;
    if (!(clip1 || clip2 || clip3 || clip4))
      drawBezier(g, v1, v2, v3, v4);
    dx1 = -0.330078125*v1.x + 0.287109375*v2.x + 0.041015625*v3.x + 0.001953125*v4.x;
    dx2 = 0.08203125*v1.x - 0.15234375*v2.x + 0.05859375*v3.x + 0.01171875*v4.x;
    dx3 = 0.01171875*(v4.x-v1.x) + 0.03515625*(v2.x-v3.x);
    dy1 = -0.330078125*v1.y + 0.287109375*v2.y + 0.041015625*v3.y + 0.001953125*v4.y;
    dy2 = 0.08203125*v1.y - 0.15234375*v2.y + 0.05859375*v3.y + 0.01171875*v4.y;
    dy3 = 0.01171875*(v4.y-v1.y) + 0.03515625*(v2.y-v3.y);
    dz1 = -0.330078125*v1.z + 0.287109375*v2.z + 0.041015625*v3.z + 0.001953125*v4.z;
    dz2 = 0.08203125*v1.z - 0.15234375*v2.z + 0.05859375*v3.z + 0.01171875*v4.z;
    dz3 = 0.01171875*(v4.z-v1.z) + 0.03515625*(v2.z-v3.z);
    pos = new Vec3(v1.x+dx1, v1.y+dy1, v1.z+dz1);
    dx1 += dx2;  dx2 += dx3;
    dy1 += dy2;  dy2 += dy3;
    dz1 += dz2;  dz2 += dz3;
    drawClippedLine(g, v1, pos);
    for (i = 0; i < 7; i++)
      {
        pos.x += dx1;  dx1 += dx2;  dx2 += dx3;
        pos.y += dy1;  dy1 += dy2;  dy2 += dy3;
        pos.z += dz1;  dz1 += dz2;  dz2 += dz3;
        drawClippedLineTo(g, pos);
      }
  }

  /** Draw a cubic Bezier curve, using the four specified control vertices.  We do this by
      recursive subdivision.  We always subdivide three times, which should be enough for
      interactive purposes.  For speed, fixed point arithmetic is used throughout. */
  
  public void drawBezier2(Graphics g, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4)
  {
    int x1, x2, x3, x4, y1, y2, y3, y4, w1, w2, w3, w4;
    Mat4 m = objectToScreen;
    
    x1 = (int) ((m.m11*v1.x+m.m12*v1.y+m.m13*v1.z+m.m14)*65536.0);
    x2 = (int) ((m.m11*v2.x+m.m12*v2.y+m.m13*v2.z+m.m14)*65536.0);
    x3 = (int) ((m.m11*v3.x+m.m12*v3.y+m.m13*v3.z+m.m14)*65536.0);
    x4 = (int) ((m.m11*v4.x+m.m12*v4.y+m.m13*v4.z+m.m14)*65536.0);
    y1 = (int) ((m.m21*v1.x+m.m22*v1.y+m.m23*v1.z+m.m24)*65536.0);
    y2 = (int) ((m.m21*v2.x+m.m22*v2.y+m.m23*v2.z+m.m24)*65536.0);
    y3 = (int) ((m.m21*v3.x+m.m22*v3.y+m.m23*v3.z+m.m24)*65536.0);
    y4 = (int) ((m.m21*v4.x+m.m22*v4.y+m.m23*v4.z+m.m24)*65536.0);
    w1 = (int) ((m.m41*v1.x+m.m42*v1.y+m.m43*v1.z+m.m44)*65536.0);
    w2 = (int) ((m.m41*v2.x+m.m42*v2.y+m.m43*v2.z+m.m44)*65536.0);
    w3 = (int) ((m.m41*v3.x+m.m42*v3.y+m.m43*v3.z+m.m44)*65536.0);
    w4 = (int) ((m.m41*v4.x+m.m42*v4.y+m.m43*v4.z+m.m44)*65536.0);
    divideAndDraw(g, x1, x2, x3, x4, y1, y2, y3, y4, w1, w2, w3, w4, 3);
  }

  void divideAndDraw(Graphics g, int x1, int x2, int x3, int x4, int y1, int y2, int y3, int y4, int w1, int w2, int w3, int w4, int count)
  {
    int xl2, xl3, xl4, xr2, xr3, xh;
    int yl2, yl3, yl4, yr2, yr3, yh;
    int wl2, wl3, wl4, wr2, wr3, wh;
    
    // Subdivide the curve.

    xl2 = (x1+x2)>>1;
    xh = (x2+x3)>>1;
    xl3 = (xl2+xh)>>1;
    xr3 = (x3+x4)>>1;
    xr2 = (xh+xr3)>>1;
    xl4 = (xl3+xr2)>>1;
    yl2 = (y1+y2)>>1;
    yh = (y2+y3)>>1;
    yl3 = (yl2+yh)>>1;
    yr3 = (y3+y4)>>1;
    yr2 = (yh+yr3)>>1;
    yl4 = (yl3+yr2)>>1;
    wl2 = (w1+w2)>>1;
    wh = (w2+w3)>>1;
    wl3 = (wl2+wh)>>1;
    wr3 = (w3+w4)>>1;
    wr2 = (wh+wr3)>>1;
    wl4 = (wl3+wr2)>>1;

    // If we've subdivided enough, draw the curve and return.  Otherwise, subdivide again.

    if (count == 1)
      {
        g.drawLine(x1/w1, y1/w1, xl4/wl4, yl4/wl4);
        g.drawLine(x4/w4, y4/w4, xl4/wl4, yl4/wl4);
      }
    else
      {
        divideAndDraw(g, x1, xl2, xl3, xl4, y1, yl2, yl3, yl4, w1, wl2, wl3, wl4, count-1);
        divideAndDraw(g, xl4, xr2, xr3, x4, yl4, yr2, yr3, y4, wl4, wr2, wr3, w4, count-1);
      }
  }
}
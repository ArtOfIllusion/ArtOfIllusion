/* Copyright (C) 2005-2012 by Peter Eastman

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY 
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.texture.TextureSpec;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.nio.*;
import java.util.*;
import java.lang.ref.*;

import javax.media.opengl.*;

import buoy.event.*;
import com.sun.opengl.util.*;

/** This is a CanvasDrawer which uses OpenGL to render the contents of a ViewerCanvas. */

public class GLCanvasDrawer implements CanvasDrawer
{
  private ViewerCanvas view;
  private GLCanvas canvas;
  private GL gl;
  private Rectangle bounds;
  private Mat4 lastObjectTransform;
  private Color lastColor;
  private double minDepth, maxDepth;
  private boolean depthEnabled, lightingEnabled, cullingEnabled;
  private FloatBuffer vertBuffer, normBuffer;
  private Shape draggedShape;
  private GLImage template;
  private WeakHashMap<Image, GLTexture> textureMap = new WeakHashMap<Image, GLTexture>();
  private ReferenceQueue textureCleanupQueue = new ReferenceQueue();
  private HashSet<TextureReference> textureReferences = new HashSet<TextureReference>();

  private static final float COLOR_SCALE = 1.0f/255.0f;
  private static HashMap<String, SoftReference<GLImage>> textImageMap = new HashMap<String, SoftReference<GLImage>>();
  private static WeakHashMap<Image, SoftReference<GLImage>> imageMap = new WeakHashMap<Image, SoftReference<GLImage>>();
  private static Color lastTextColor;
  private static int imageRenderMode = -1;
  private static boolean useTextureRectangle;

  public GLCanvasDrawer(ViewerCanvas view)
  {
    this.view = view;
    canvas = new GLCanvas();
    canvas.addGLEventListener(new CanvasListener());
  }
  
  /** Get the GLCanvas into which this draws. */
  
  public Component getGLCanvas()
  {
    return canvas;
  }

  /** Set the template image. */
  
  public void setTemplateImage(Image im)
  {
    if (im == null)
    {
      template = null;
      return;
    }
    try
    {
      template = new GLImage(im);
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }
  }
  
  /** Show feedback to the user in response to a mouse drag, by drawing a Shape over the image.
      Unlike the other methods of this class, this method may be called at arbitrary times
      (though always from the event dispatch thread), not during the process of rendering
      the image. */

  public void drawDraggedShape(Shape shape)
  {
    draggedShape = shape;
    canvas.display();
  }

  /** Prepare for rendering from the camera's current viewpoint.*/
  
  private void prepareView3D(Camera camera)
  {
    if (camera.getObjectToView() == lastObjectTransform)
      return;
    lastObjectTransform = camera.getObjectToView();
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    double scale = -1.0/camera.getViewToScreen().m11;
    if (view.isPerspective())
      gl.glFrustum(-0.5*bounds.width*scale, 0.5*bounds.width*scale, -0.5*bounds.height*scale, 0.5*bounds.height*scale, minDepth, maxDepth);
    else
      gl.glOrtho(-0.5*bounds.width*scale, 0.5*bounds.width*scale, -0.5*bounds.height*scale, 0.5*bounds.height*scale, minDepth, maxDepth);
    Mat4 toView = lastObjectTransform;
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadMatrixd(new double [] {
        -toView.m11, toView.m21, -toView.m31, toView.m41,
        -toView.m12, toView.m22, -toView.m32, toView.m42,
        -toView.m13, toView.m23, -toView.m33, toView.m43,
        -toView.m14, toView.m24, -toView.m34, toView.m44
    }, 0);
  }
  
  /** Prepare for drawing 2D primitives. */
  
  private void prepareView2D()
  {
    if (lastObjectTransform == null)
      return;
    lastObjectTransform = null;
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    if (view.isPerspective())
      gl.glFrustum(0.0, bounds.width, 0.0, bounds.height, minDepth, maxDepth);
    else
      gl.glOrtho(0.0, bounds.width, 0.0, bounds.height, minDepth, maxDepth);
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
  }
  
  /** Prepare for drawing with or without depth testing. */
  
  private void prepareDepthTest(boolean depthTest)
  {
    if (depthTest != depthEnabled)
    {
      depthEnabled = depthTest;
      if (depthEnabled)
        gl.glEnable(GL.GL_DEPTH_TEST);
      else
        gl.glDisable(GL.GL_DEPTH_TEST);
    }
  }
  
  /** Prepare for drawing in a solid color. */
  
  private void prepareSolidColor(Color color)
  {
    if (lastColor == color)
      return;
    if (lightingEnabled)
    {
      lightingEnabled = false;
      gl.glDisable(GL.GL_LIGHTING);
    }
    lastColor = color;
    gl.glColor3f(color.getRed()*COLOR_SCALE, color.getGreen()*COLOR_SCALE, color.getBlue()*COLOR_SCALE);
  }
  
  /** Prepare for drawing a shaded surface. */
  
  private void prepareShading(boolean lighting)
  {
    if (lightingEnabled != lighting)
    {
      lightingEnabled = lighting;
      if (lighting)
        gl.glEnable(GL.GL_LIGHTING);
      else
        gl.glDisable(GL.GL_LIGHTING);
    }
    lastColor = null;
  }
  
  /** Prepare for drawing with or without backface culling. */
  
  private void prepareCulling(boolean hideBackfaces)
  {
    if (cullingEnabled == hideBackfaces)
      return;
    cullingEnabled = hideBackfaces;
    if (cullingEnabled)
      gl.glEnable(GL.GL_CULL_FACE);
    else
      gl.glDisable(GL.GL_CULL_FACE);
  }
  
  /** Prepare the buffers used for storing vertex and normal arrays. */
  
  private void prepareBuffers(int requiredSize)
  {
    if (vertBuffer == null || vertBuffer.capacity() < requiredSize)
    {
      vertBuffer = BufferUtil.newFloatBuffer(requiredSize);
      normBuffer = BufferUtil.newFloatBuffer(requiredSize);
      gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertBuffer);
      gl.glNormalPointer(GL.GL_FLOAT, 0, normBuffer);
    }
  }
  
  /** Draw a border around the rendered image. */
  
  public void drawBorder()
  {
    prepareView2D();
    prepareDepthTest(false);
    prepareSolidColor(Color.black);
    double d = -(minDepth+0.001);
    double scale = (view.isPerspective() ? -d/minDepth : 1.0);
    gl.glBegin(GL.GL_LINE_STRIP);
    gl.glVertex3d(0.0f, 0.0f, d);
    gl.glVertex3d((bounds.width-1)*scale, 0.0f, d);
    gl.glVertex3d((bounds.width-1)*scale, (bounds.height-1)*scale, d);
    gl.glVertex3d(0.0f, (bounds.height-1)*scale, d);
    gl.glVertex3d(0.0f, 0.0f, d);
    gl.glEnd();
    if (view.getDrawFocus())
    {
      prepareSolidColor(ViewerCanvas.lineColor);
      gl.glBegin(GL.GL_LINE_STRIP);
      gl.glVertex3d(scale, scale, d);
      gl.glVertex3d((bounds.width-2)*scale, scale, d);
      gl.glVertex3d((bounds.width-2)*scale, (bounds.height-2)*scale, d);
      gl.glVertex3d(scale, (bounds.height-2)*scale, d);
      gl.glVertex3d(scale, scale, d);
      gl.glEnd();
    }
  }

  /** Draw a horizontal line across the rendered image.  The parameters are the y coordinate
      of the line and the line color. */
  
  public void drawHRule(int y, Color color)
  {
    prepareView2D();
    prepareDepthTest(false);
    prepareSolidColor(color);
    double d = -(minDepth+0.001);
    double scale = (view.isPerspective() ? -d/minDepth : 1.0);
    gl.glBegin(GL.GL_LINES);
    gl.glVertex3d(0.0, (bounds.height-y)*scale, d);
    gl.glVertex3d(bounds.width*scale, (bounds.height-y)*scale, d);
    gl.glEnd();
  }

  /** Draw a vertical line across the rendered image.  The parameters are the x coordinate
      of the line and the line color. */
  
  public void drawVRule(int x, Color color)
  {
    prepareView2D();
    prepareDepthTest(false);
    prepareSolidColor(color);
    double d = -(minDepth+0.001);
    double scale = (view.isPerspective() ? -d/minDepth : 1.0);
    gl.glBegin(GL.GL_LINES);
    gl.glVertex3d(x*scale, 0.0, d);
    gl.glVertex3d(x*scale, bounds.height*scale, d);
    gl.glEnd();
  }

  /** Draw a filled box in the rendered image. */
  
  public void drawBox(int x, int y, int width, int height, Color color)
  {
    prepareView2D();
    prepareDepthTest(false);
    prepareSolidColor(color);
    float y1 = bounds.height-y;
    double d = -(minDepth+0.001);
    double scale = (view.isPerspective() ? -d/minDepth : 1.0);
    gl.glBegin(GL.GL_QUADS);
    gl.glVertex3d(x*scale, y1*scale, d);
    gl.glVertex3d(x*scale, (y1-height)*scale, d);
    gl.glVertex3d((x+width)*scale, (y1-height)*scale, d);
    gl.glVertex3d((x+width)*scale, y1*scale, d);
    gl.glEnd();
  }

  /** Draw a set of filled boxes in the rendered image. */

  public void drawBoxes(java.util.List<Rectangle> box, Color color)
  {
    prepareView2D();
    prepareDepthTest(false);
    prepareSolidColor(color);
    prepareBuffers(box.size()*12);
    vertBuffer.clear();
    float d = (float) -(minDepth+0.001);
    for (Rectangle r : box)
    {
      float y1 = bounds.height-r.y;
      float scale = (float) (view.isPerspective() ? -d/minDepth : 1.0);
      vertBuffer.put(r.x*scale);
      vertBuffer.put(y1*scale);
      vertBuffer.put(d);
      vertBuffer.put(r.x*scale);
      vertBuffer.put((y1-r.height)*scale);
      vertBuffer.put(d);
      vertBuffer.put((r.x+r.width)*scale);
      vertBuffer.put((y1-r.height)*scale);
      vertBuffer.put(d);
      vertBuffer.put((r.x+r.width)*scale);
      vertBuffer.put(y1*scale);
      vertBuffer.put(d);
    }
    gl.glDrawArrays(GL.GL_QUADS, 0, box.size()*4);
  }

  /** Render a filled box at a specified depth in the rendered image. */
  
  public void renderBox(int x, int y, int width, int height, double depth, Color color)
  {
    prepareView2D();
    prepareDepthTest(true);
    prepareSolidColor(color);
    float y1 = bounds.height-y;
    float d = (float) -depth;
    gl.glBegin(GL.GL_QUADS);
    float scale = (float) (view.isPerspective() ? depth/minDepth : 1.0);
    gl.glVertex3f(x*scale, y1*scale, d);
    gl.glVertex3f(x*scale, (y1-height)*scale, d);
    gl.glVertex3f((x+width)*scale, (y1-height)*scale, d);
    gl.glVertex3f((x+width)*scale, y1*scale, d);
    gl.glEnd();
  }

  /** Render a set of filled boxes at specified depths in the rendered image. */

  public void renderBoxes(java.util.List<Rectangle> box, java.util.List<Double>depth, Color color)
  {
    prepareView2D();
    prepareDepthTest(true);
    prepareSolidColor(color);
    prepareBuffers(box.size()*12);
    vertBuffer.clear();
    for (int i = 0; i < box.size(); i++)
    {
      Rectangle r = box.get(i);
      float y1 = bounds.height-r.y;
      float d = -depth.get(i).floatValue();
      float scale = (float) (view.isPerspective() ? -d/minDepth : 1.0);
      vertBuffer.put(r.x*scale);
      vertBuffer.put(y1*scale);
      vertBuffer.put(d);
      vertBuffer.put(r.x*scale);
      vertBuffer.put((y1-r.height)*scale);
      vertBuffer.put(d);
      vertBuffer.put((r.x+r.width)*scale);
      vertBuffer.put((y1-r.height)*scale);
      vertBuffer.put(d);
      vertBuffer.put((r.x+r.width)*scale);
      vertBuffer.put(y1*scale);
      vertBuffer.put(d);
    }
    gl.glDrawArrays(GL.GL_QUADS, 0, box.size()*4);
  }

  /** Draw a line into the rendered image. */
  
  public void drawLine(Point p1, Point p2, Color color)
  {
    prepareView2D();
    prepareDepthTest(false);
    prepareSolidColor(color);
    double d = -(minDepth+0.001);
    double scale = (view.isPerspective() ? -d/minDepth : 1.0);
    gl.glBegin(GL.GL_LINES);
    gl.glVertex3d(p1.x*scale, (bounds.height-p1.y)*scale, d);
    gl.glVertex3d(p2.x*scale, (bounds.height-p2.y)*scale, d);
    gl.glEnd();
  }
  
  /** Render a line into the image.
      @param p1     the first endpoint of the line
      @param p2     the second endpoint of the line
      @param cam    the camera from which to draw the line
      @param color  the line color
  */
  
  public void renderLine(Vec3 p1, Vec3 p2, Camera cam, Color color)
  {
    prepareView3D(cam);
    prepareDepthTest(true);
    prepareSolidColor(color);
    gl.glBegin(GL.GL_LINES);
    gl.glVertex3d(p1.x, p1.y, p1.z);
    gl.glVertex3d(p2.x, p2.y, p2.z);
    gl.glEnd();
  }

  /** Render a line into the image.
      @param p1     the first endpoint of the line, in screen coordinates
      @param zf1    the z coordinate of the first endpoint, in view coordinates
      @param p2     the second endpoint of the line, in screen coordinates
      @param zf2    the z coordinate of the second endpoint, in view coordinates
      @param cam    the camera from which to draw the line
      @param color  the line color
  */
  
  public void renderLine(Vec2 p1, double zf1, Vec2 p2, double zf2, Camera cam, Color color)
  {
    prepareView2D();
    prepareDepthTest(true);
    prepareSolidColor(color);
    gl.glBegin(GL.GL_LINES);
    if (view.isPerspective())
    {
      double scale1 = zf1/minDepth;
      double scale2 = zf2/minDepth;
      gl.glVertex3d(p1.x*scale1, (bounds.height-p1.y)*scale1, -zf1);
      gl.glVertex3d(p2.x*scale2, (bounds.height-p2.y)*scale2, -zf2);
    }
    else
    {
      gl.glVertex3d(p1.x, bounds.height-p1.y, -zf1);
      gl.glVertex3d(p2.x, bounds.height-p2.y, -zf2);
    }
    gl.glEnd();
  }
  
  /** Render a wireframe object. */
  
  public void renderWireframe(WireframeMesh mesh, Camera cam, Color color)
  {
    prepareView3D(cam);
    prepareDepthTest(true);
    prepareSolidColor(color);
    prepareBuffers(mesh.vert.length*3);
    vertBuffer.clear();
    for (int i = 0; i < mesh.vert.length; i++)
    {
      Vec3 v = mesh.vert[i];
      vertBuffer.put((float) v.x);
      vertBuffer.put((float) v.y);
      vertBuffer.put((float) v.z);
    }
    int vertexIndices[] = new int [mesh.from.length*2];
    for (int i = 0, j = 0; i < mesh.from.length; i++)
    {
      vertexIndices[j++] = mesh.from[i];
      vertexIndices[j++] = mesh.to[i];
    }
    gl.glDrawElements(GL.GL_LINES, vertexIndices.length, GL.GL_UNSIGNED_INT, IntBuffer.wrap(vertexIndices));
  }

  /** Render an object with flat shading in subtractive (transparent) mode. */
  
  public void renderMeshTransparent(RenderingMesh mesh, VertexShader shader, Camera cam, Vec3 viewDir, boolean hideFace[])
  {
    prepareView3D(cam);
    prepareDepthTest(false);
    prepareCulling(false);
    prepareShading(false);
    boolean invertColor = (ViewerCanvas.backgroundColor.getGreen() > 127);
    if (invertColor)
      gl.glBlendFunc(GL.GL_ZERO, GL.GL_ONE_MINUS_SRC_COLOR);
    else
      gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
    gl.glEnable(GL.GL_BLEND);
    gl.glBegin(GL.GL_TRIANGLES);
    RGBColor faceColor = new RGBColor();
    for (int i = 0; i < mesh.triangle.length; i++)
    {
      if (hideFace != null && hideFace[i])
        continue;
      RenderingTriangle tri = mesh.triangle[i];
      Vec3 vert1 = mesh.vert[tri.v1];
      Vec3 vert2 = mesh.vert[tri.v2];
      Vec3 vert3 = mesh.vert[tri.v3];
      double dot = (float) viewDir.dot(mesh.faceNorm[i]);
      shader.getColor(i, 0, faceColor);
      if (invertColor)
        faceColor.setRGB(1.0f-faceColor.getRed(), 1.0f-faceColor.getGreen(), 1.0f-faceColor.getBlue());
      faceColor.scale(1.0f-0.8f*Math.abs(dot));
      gl.glColor3f(faceColor.getRed(), faceColor.getGreen(), faceColor.getBlue());
      gl.glVertex3d(vert1.x, vert1.y, vert1.z);
      gl.glVertex3d(vert2.x, vert2.y, vert2.z);
      gl.glVertex3d(vert3.x, vert3.y, vert3.z);
    }
    gl.glEnd();
    gl.glDisable(GL.GL_BLEND);
  }
  
  /** Render a mesh to the canvas. */
  
  public void renderMesh(RenderingMesh mesh, VertexShader shader, Camera cam, boolean closed, boolean hideFace[])
  {
    prepareView3D(cam);
    prepareDepthTest(true);
    prepareCulling(closed && hideFace == null);
    boolean uniform = shader.isUniformTexture();
    if (uniform)
    {
      // Set up the material properties to let OpenGL shade the surface.
      
      prepareShading(true);
      TextureSpec spec = new TextureSpec();
      shader.getTextureSpec(spec);
      gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE, FloatBuffer.wrap(new float [] {spec.diffuse.getRed(), spec.diffuse.getGreen(), spec.diffuse.getBlue(), 1.0f}));
      gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, FloatBuffer.wrap(new float [] {spec.hilight.getRed(), spec.hilight.getGreen(), spec.hilight.getBlue(), 1.0f}));
      gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, FloatBuffer.wrap(new float [] {spec.emissive.getRed(), spec.emissive.getGreen(), spec.emissive.getBlue(), 1.0f}));
      gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, (float) ((1.0-spec.roughness)*127.0+1.0));
      
      // Fill in buffers with the vertices and normals.
      
      int vertexIndices[] = null;
      boolean flat = (shader instanceof FlatVertexShader);
      if (hideFace == null && !flat)
        vertexIndices = mesh.getVertexIndices();
      if (vertexIndices != null)
      {
        // This is a fully smoothed mesh, so we can use glDrawElements().
        
        prepareBuffers(mesh.vert.length*3);
        vertBuffer.clear();
        normBuffer.clear();
        for (int i = 0; i < mesh.vert.length; i++)
        {
          Vec3 v = mesh.vert[i];
          vertBuffer.put((float) v.x);
          vertBuffer.put((float) v.y);
          vertBuffer.put((float) v.z);
        }
        for (int i = 0; i < mesh.norm.length; i++)
        {
          Vec3 v = mesh.norm[i];
          normBuffer.put((float) v.x);
          normBuffer.put((float) v.y);
          normBuffer.put((float) v.z);
        }
        gl.glDrawElements(GL.GL_TRIANGLES, vertexIndices.length, GL.GL_UNSIGNED_INT, IntBuffer.wrap(vertexIndices));
      }
      else
      {
        // We need a separate normal for each face-vertex.
        
        prepareBuffers(mesh.triangle.length*9);
        vertBuffer.clear();
        normBuffer.clear();
        int faceCount = 0;
        for (int i = 0; i < mesh.triangle.length; i++)
        {
          if (hideFace != null && hideFace[i])
            continue;
          faceCount++;
          RenderingTriangle tri = mesh.triangle[i];
          Vec3 v = mesh.vert[tri.v1];
          vertBuffer.put((float) v.x);
          vertBuffer.put((float) v.y);
          vertBuffer.put((float) v.z);
          v = mesh.vert[tri.v2];
          vertBuffer.put((float) v.x);
          vertBuffer.put((float) v.y);
          vertBuffer.put((float) v.z);
          v = mesh.vert[tri.v3];
          vertBuffer.put((float) v.x);
          vertBuffer.put((float) v.y);
          vertBuffer.put((float) v.z);
          if (flat)
          {
            // Force flat shading.

            v = mesh.faceNorm[i];
            normBuffer.put((float) v.x);
            normBuffer.put((float) v.y);
            normBuffer.put((float) v.z);
            normBuffer.put((float) v.x);
            normBuffer.put((float) v.y);
            normBuffer.put((float) v.z);
            normBuffer.put((float) v.x);
            normBuffer.put((float) v.y);
            normBuffer.put((float) v.z);
          }
          else
          {
            // Set the normals from the mesh.

            v = mesh.norm[tri.n1];
            normBuffer.put((float) v.x);
            normBuffer.put((float) v.y);
            normBuffer.put((float) v.z);
            v = mesh.norm[tri.n2];
            normBuffer.put((float) v.x);
            normBuffer.put((float) v.y);
            normBuffer.put((float) v.z);
            v = mesh.norm[tri.n3];
            normBuffer.put((float) v.x);
            normBuffer.put((float) v.y);
            normBuffer.put((float) v.z);
          }
        }
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, faceCount*3);
      }
    }
    else
    {
      // We will need to calculate the color of each face/vertex ourselves.
      
      prepareShading(false);
      gl.glBegin(GL.GL_TRIANGLES);
      RGBColor surfaceColor = new RGBColor();
      for (int i = 0; i < mesh.triangle.length; i++)
      {
        if (hideFace != null && hideFace[i])
          continue;
        RenderingTriangle tri = mesh.triangle[i];
        Vec3 vert1 = mesh.vert[tri.v1];
        Vec3 vert2 = mesh.vert[tri.v2];
        Vec3 vert3 = mesh.vert[tri.v3];
        shader.getColor(i, 0, surfaceColor);
        gl.glColor3f(surfaceColor.getRed(), surfaceColor.getGreen(), surfaceColor.getBlue());
        gl.glVertex3d(vert1.x, vert1.y, vert1.z);
        if (shader.isUniformFace(i))
        {
          gl.glVertex3d(vert2.x, vert2.y, vert2.z);
          gl.glVertex3d(vert3.x, vert3.y, vert3.z);
        }
        else
        {
          shader.getColor(i, 1, surfaceColor);
          gl.glColor3f(surfaceColor.getRed(), surfaceColor.getGreen(), surfaceColor.getBlue());
          gl.glVertex3d(vert2.x, vert2.y, vert2.z);
          shader.getColor(i, 2, surfaceColor);
          gl.glColor3f(surfaceColor.getRed(), surfaceColor.getGreen(), surfaceColor.getBlue());
          gl.glVertex3d(vert3.x, vert3.y, vert3.z);
        }
      }
      gl.glEnd();
    }
  }

  /** Draw a piece of text onto the canvas. */

  public void drawString(String text, int x, int y, Color color)
  {
    if (color != lastTextColor)
    {
      textImageMap.clear();
      lastTextColor = color;
    }
    Font font = view.getComponent().getFont();
    FontMetrics fm = view.getComponent().getFontMetrics(font);
    int ascent = fm.getMaxAscent();
    int descent = fm.getMaxDescent();
    SoftReference<GLImage> ref = textImageMap.get(text);
    GLImage image = (ref == null ? null : ref.get());
    if (image == null)
    {
      // Create an image of the text.

      int width = fm.stringWidth(text);
      int height = ascent+descent;
      Image im = view.getComponent().createImage(width, height);
      Graphics g = im.getGraphics();
      g.setColor(ViewerCanvas.backgroundColor);
      g.fillRect(0, 0, width, height);
      g.setColor(color);
      g.setFont(font);
      g.drawString(text, 0, ascent);
      g.dispose();
      try
      {
        image = new GLImage(im);
      }
      catch (InterruptedException ex)
      {
        ex.printStackTrace();
        return;
      }
      textImageMap.put(text, new SoftReference<GLImage>(image));
    }
    drawImage(image, x, y-ascent);
  }

  /** Draw an image onto the canvas. */

  public void drawImage(Image image, int x, int y)
  {
    try
    {
      drawImage(getCachedImage(image, false), x, y);
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }
  }

  /**
   * Render an image onto the canvas.
   *
   * @param image  the image to render
   * @param p1     the coordinates of the first corner of the image
   * @param p2     the coordinates of the second corner of the image
   * @param p3     the coordinates of the third corner of the image
   * @param p4     the coordinates of the fourth corner of the image
   * @param camera the camera from which to draw the image
   */

  public void renderImage(Image image, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, Camera camera)
  {
    GLTexture record;
    try
    {
      record = getCachedTexture(image);
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
      return;
    }
    prepareView3D(camera);
    prepareDepthTest(true);
    prepareCulling(false);
    prepareShading(false);
    double width = (useTextureRectangle ? record.width : 1.0);
    double height = (useTextureRectangle ? record.height : 1.0);
    gl.glEnable(imageRenderMode);
    gl.glBindTexture(imageRenderMode, record.getTextureId());
    gl.glBegin(GL.GL_QUADS);
    gl.glVertex3d(p1.x, p1.y, p1.z);
    gl.glTexCoord2d(width, 0.0);
    gl.glVertex3d(p2.x, p2.y, p2.z);
    gl.glTexCoord2d(width, height);
    gl.glVertex3d(p3.x, p3.y, p3.z);
    gl.glTexCoord2d(0.0, height);
    gl.glVertex3d(p4.x, p4.y, p4.z);
    gl.glTexCoord2d(0.0, 0.0);
    gl.glEnd();
    gl.glDisable(imageRenderMode);
  }

  public void imageChanged(Image image)
  {
    imageMap.remove(image);
  }

  /** Get a GLImage for an Image, attempting to reuse objects for efficiency. */

  private GLImage getCachedImage(Image image, boolean texture) throws InterruptedException
  {
    GLImage record = null;
    SoftReference<GLImage> ref = imageMap.get(image);
    if (ref != null)
      record = ref.get();
    if (record == null)
    {
      Image sourceImage = image;
      if (texture && !useTextureRectangle)
      {
        // Scale the image so its dimensions are powers of 2.

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        int newWidth = (int) Math.pow(2, Math.ceil(Math.log(width)/Math.log(2)));
        int newHeight = (int) Math.pow(2, Math.ceil(Math.log(height)/Math.log(2)));
        if (newWidth != width || newHeight != height)
        {
          sourceImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
          MediaTracker mt = new MediaTracker(canvas);
          mt.addImage(sourceImage, 0);
          mt.waitForID(0);
        }
      }

      // Grab the pixels from the image and cache them.

      record = new GLImage(sourceImage);
      imageMap.put(image, new SoftReference<GLImage>(record));
    }
    return record;
  }

  /** Get a GLTexture for an Image, attempting to reuse objects for efficiency. */

  private GLTexture getCachedTexture(Image image) throws InterruptedException
  {
    GLTexture record = textureMap.get(image);
    if (record == null)
    {
      // Create a texture from the image and cache it.

      record = new GLTexture(image);
      textureMap.put(image, record);
    }
    return record;
  }

  /** Draw an image into the rendered image. */

  private void drawImage(GLImage image, int x, int y)
  {
    prepareView2D();
    prepareDepthTest(false);
    gl.glEnable(GL.GL_ALPHA_TEST);
    double d = -(minDepth+0.001);
    gl.glRasterPos3d(x, Math.max(0, bounds.height-image.height-y), d);
    gl.glDrawPixels(image.width, image.height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, image.data);
    gl.glDisable(GL.GL_ALPHA_TEST);
  }

  /** Draw the outline of a Shape into the canvas. */
  
  public void drawShape(Shape shape, Color color)
  {
    drawShape(shape, color, GL.GL_LINE_STRIP);
  }

  /** Draw a filled Shape onto the canvas. */

  public void fillShape(Shape shape, Color color)
  {
    drawShape(shape, color, GL.GL_POLYGON);
  }

  /** This is called by both drawShape() and fillShape(). */

  public void drawShape(Shape shape, Color color, int mode)
  {
    prepareView2D();
    prepareDepthTest(false);
    prepareSolidColor(color);
    Point lastMove = null;
    PathIterator path = shape.getPathIterator(null);
    float coords[] = new float [6];
    double d = -(minDepth+0.001);
    double scale = (view.isPerspective() ? -d/minDepth : 1.0);
    gl.glBegin(mode);
    while (!path.isDone())
    {
      int type = path.currentSegment(coords);
      if (type == PathIterator.SEG_MOVETO)
      {
        gl.glEnd();
        gl.glBegin(mode);
        lastMove = new Point((int) coords[0], (int) coords[1]);
        gl.glVertex3d(lastMove.x*scale, (bounds.height-lastMove.y)*scale, d);
      }
      else if (type == PathIterator.SEG_LINETO)
        gl.glVertex3d(coords[0]*scale, (bounds.height-coords[1])*scale, d);
      else if (type == PathIterator.SEG_CLOSE)
        gl.glVertex3d(lastMove.x*scale, (bounds.height-lastMove.y)*scale, d);
      path.next();
    }
    gl.glEnd();
  }

  /** This inner class implements the callbacks to perform drawing with Jogl. */
  
  private class CanvasListener implements GLEventListener
  {
    public void init(GLAutoDrawable drawable)
    {
      GL gl = drawable.getGL();
      gl.glShadeModel(GL.GL_SMOOTH);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, FloatBuffer.wrap(new float [] {0.0f, 0.0f, 1.0f, 0.0f}));
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, FloatBuffer.wrap(new float [] {0.8f, 0.8f, 0.8f, 1.0f}));
      gl.glEnable(GL.GL_LIGHT0);
      gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, FloatBuffer.wrap(new float [] {0.1f, 0.1f, 0.1f, 1.0f}));
      gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
      gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
      gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
      gl.glAlphaFunc(GL.GL_GREATER, 0.0f);
      if (imageRenderMode == -1)
      {
        // Determine whether non-power-of-2 textures are supported.

        if (gl.glGetString(GL.GL_EXTENSIONS).indexOf("GL_EXT_texture_rectangle") > -1)
        {
          imageRenderMode = GL.GL_TEXTURE_RECTANGLE_ARB;
          useTextureRectangle = true;
        }
        else
        {
          imageRenderMode = GL.GL_TEXTURE_2D;
          useTextureRectangle = false;
        }
      }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
    }

    public void display(GLAutoDrawable drawable)
    {
      view.prepareCameraForRendering();
      gl = drawable.getGL();
      Color background = ViewerCanvas.backgroundColor;
      gl.glClearColor(background.getRed()/255.0f, background.getGreen()/255.0f, background.getBlue()/255.0f, 0.0f);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT+GL.GL_DEPTH_BUFFER_BIT);
      bounds = canvas.getBounds();
      gl.glEnable(GL.GL_DEPTH_TEST);
      depthEnabled = true;
      lastObjectTransform = Mat4.identity(); // ensure the view will get initialized
      lastColor = new Color(0); // ensure the color/material will get initialized
      double depthRange[] = view.estimateDepthRange();
      if (view.getShowGrid() && view.isPerspective())
      {
        double maxGridSize = 10.0*Math.sqrt(2.0);
        double gridCenterDist = view.getCamera().getWorldToView().times(new Vec3()).length();
        depthRange[0] = Math.min(depthRange[0], gridCenterDist-maxGridSize);
        depthRange[1] = Math.max(depthRange[1], gridCenterDist+maxGridSize);
      }
      if (view.isPerspective())
        minDepth = view.getCamera().getDistToScreen()/20.0;
      else
        minDepth = Math.min(-0.01, depthRange[0]);
      minDepth -= 0.01;
      maxDepth = depthRange[1]+0.01;
      if (view.getTemplateShown())
        drawImage(template, 0, 0);
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
      view.updateImage();
      view.getCurrentTool().drawOverlay(view);
      if (draggedShape != null)
        drawShape(draggedShape, ViewerCanvas.lineColor);
      draggedShape = null;

      // Clean up unused textures.

      Reference ref;
      while ((ref = textureCleanupQueue.poll()) != null)
      {
        gl.glDeleteTextures(1, new int[] {((TextureReference) ref).textureId}, 0);
        textureReferences.remove(ref);
      }

      // Send out a RepaintEvent so anyone who is listening will know the view has been repainted.

      view.dispatchEvent(new RepaintEvent(view, null));
    }

    public void displayChanged(GLAutoDrawable drawable, boolean arg1, boolean arg2)
    {
    }
  }

  /** This inner class represents an image that is ready to be drawn into the GLCanvas. */

  private static class GLImage
  {
    public ByteBuffer data;
    public int width, height;

    /** Create a GLImage from a regular Image object. */

    public GLImage(Image image) throws InterruptedException
    {
      width = image.getWidth(null);
      height = image.getHeight(null);

      // Extract the image data and convert it to the proper format.

      PixelGrabber pg = new PixelGrabber(image, 0, 0, -1, -1, true);
      pg.grabPixels();
      int templatePixel[] = (int []) pg.getPixels();
      byte dataArray[] = new byte [templatePixel.length*4];
      int pos = 0;
      for (int row = 0; row < height; row++)
        for (int col = 0; col < width; col++)
        {
          int argb = templatePixel[width*(height-row-1)+col];
          dataArray[pos++] = (byte) (argb>>16);
          dataArray[pos++] = (byte) (argb>>8);
          dataArray[pos++] = (byte) (argb);
          dataArray[pos++] = (byte) (argb>>24);
        }
      data = ByteBuffer.wrap(dataArray);
    }
  }

  /** This inner class represents an image that is ready to be used as a texture in the GLCanvas. */

  private class GLTexture
  {
    public int width, height;
    private int textureId[];

    /** Create a GLTexture from a regular Image object. */

    public GLTexture(Image image) throws InterruptedException
    {
      GLImage glImage = getCachedImage(image, true);
      width = glImage.width;
      height = glImage.height;
      textureId = new int[1];
      gl.glGenTextures(1, textureId, 0);
      gl.glBindTexture(imageRenderMode, textureId[0]);
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
      gl.glTexParameteri(imageRenderMode, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
      gl.glTexParameteri(imageRenderMode, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
      gl.glTexParameteri(imageRenderMode, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(imageRenderMode, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
      gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
      gl.glTexImage2D(imageRenderMode, 0, GL.GL_RGBA, width, height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, glImage.data);
      textureReferences.add(new TextureReference(this));
    }

    public int getTextureId()
    {
      return textureId[0];
    }
  }

  /** This inner class is used for cleaning up textures once they are no longer needed. */

  private class TextureReference extends PhantomReference<GLTexture>
  {
    private int textureId;

    TextureReference(GLTexture texture)
    {
      super(texture, textureCleanupQueue);
      textureId = texture.getTextureId();
    }
  }
}

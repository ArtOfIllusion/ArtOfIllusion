/* Copyright (C) 2005-2009 by Peter Eastman

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY 
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import java.awt.*;

import artofillusion.Camera;
import artofillusion.RenderingMesh;
import artofillusion.WireframeMesh;
import artofillusion.math.Vec2;
import artofillusion.math.Vec3;

/** This interface defines an object which renders the content of a ViewerCanvas. */

public interface CanvasDrawer
{
  /** Set the template image. */
  
  public void setTemplateImage(Image im);
  
  /** Show feedback to the user in response to a mouse drag, by drawing a Shape over the image.
      Unlike the other methods of this class, this method may be called at arbitrary times
      (though always from the event dispatch thread), not during the process of rendering
      the image. */

  public void drawDraggedShape(Shape shape);

  /** Draw a border around the rendered image. */
  
  public void drawBorder();

  /** Draw a horizontal line across the rendered image.  The parameters are the y coordinate
      of the line and the line color. */
  
  public void drawHRule(int y, Color color);

  /** Draw a vertical line across the rendered image.  The parameters are the x coordinate
      of the line and the line color. */
  
  public void drawVRule(int x, Color color);

  /** Draw a filled box in the rendered image. */
  
  public void drawBox(int x, int y, int width, int height, Color color);

  /** Draw a set of filled boxes in the rendered image. */

  public void drawBoxes(java.util.List<Rectangle> box, Color color);

  /** Render a filled box at a specified depth in the rendered image. */
  
  public void renderBox(int x, int y, int width, int height, double depth, Color color);

  /** Render a set of filled boxes at specified depths in the rendered image. */

  public void renderBoxes(java.util.List<Rectangle> box, java.util.List<Double>depth, Color color);

  /** Draw a line into the rendered image. */
  
  public void drawLine(Point p1, Point p2, Color color);
  
  /** Render a line into the image.
      @param p1     the first endpoint of the line
      @param p2     the second endpoint of the line
      @param cam    the camera from which to draw the line
      @param color  the line color
  */
  
  public void renderLine(Vec3 p1, Vec3 p2, Camera cam, Color color);

  /** Render a line into the image.
      @param p1     the first endpoint of the line, in screen coordinates
      @param zf1    the z coordinate of the first endpoint, in view coordinates
      @param p2     the second endpoint of the line, in screen coordinates
      @param zf2    the z coordinate of the second endpoint, in view coordinates
      @param cam    the camera from which to draw the line
      @param color  the line color
  */
  
  public void renderLine(Vec2 p1, double zf1, Vec2 p2, double zf2, Camera cam, Color color);
  
  /** Render a wireframe object. */
  
  public void renderWireframe(WireframeMesh mesh, Camera cam, Color color);

  /** Render an object with flat shading in subtractive (transparent) mode. */
  
  public void renderMeshTransparent(RenderingMesh mesh, VertexShader shader, Camera cam, Vec3 viewDir, boolean hideFace[]);
  
  /** Render a mesh to the canvas. */
  
  public void renderMesh(RenderingMesh mesh, VertexShader shader, Camera cam, boolean closed, boolean hideFace[]);

  /** Draw a piece of text onto the canvas. */

  public void drawString(String text, int x, int y, Color color);

  /** Draw an image onto the canvas. */

  public void drawImage(Image image, int x, int y);

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

  public void renderImage(Image image, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, Camera camera);

  /** Draw the outline of a Shape into the canvas. */

  public void drawShape(Shape shape, Color color);

  /** Draw a filled Shape onto the canvas. */

  public void fillShape(Shape shape, Color color);

  /**
   * This should be called to indicate that a previously drawn image has changed, and cached information
   * for it needs to be discarded.
   */

  public void imageChanged(Image image);
}

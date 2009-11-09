/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.MeshEditController;
import artofillusion.view.*;
import buoy.widget.*;
import java.awt.*;

/** TubeViewer subclasses CurveViewer to display a Tube that is being edited. */

public class TubeViewer extends CurveViewer
{
  public TubeViewer(MeshEditController window, RowContainer p)
  {
    super(window, p);
  }

  protected void drawObject()
  {
    // First draw the surface.
    
    if (showSurface)
    {
      ObjectInfo objInfo = controller.getObject();
      Vec3 viewDir = getDisplayCoordinates().toLocal().timesDirection(theCamera.getViewToWorld().timesDirection(Vec3.vz()));
      if (renderMode == RENDER_WIREFRAME)
        renderWireframe(objInfo.getWireframePreview(), theCamera, surfaceColor);
      else if (renderMode == RENDER_TRANSPARENT)
        renderMeshTransparent(objInfo.getPreviewMesh(), new ConstantVertexShader(transparentColor), theCamera, viewDir, null);
      else
      {
        RenderingMesh mesh = objInfo.getPreviewMesh();
        VertexShader shader;
        if (renderMode == RENDER_FLAT)
          shader = new FlatVertexShader(mesh, surfaceRGBColor, viewDir);
        else if (renderMode == RENDER_SMOOTH)
          shader = new SmoothVertexShader(mesh, surfaceRGBColor, viewDir);
        else
          shader = new TexturedVertexShader(mesh, objInfo.getObject(), 0.0, viewDir).optimize();
        renderMesh(mesh, shader, theCamera, objInfo.getObject().isClosed(), null);
      }
    }
    
    // Now draw the central curve.

    if (showMesh)
    {
      MeshVertex v[] = ((Mesh) getController().getObject().getObject()).getVertices();
      Vec2 pos[] = new Vec2 [v.length];
      for (int i = 0; i < v.length; i++)
        pos[i] = theCamera.getObjectToScreen().timesXY(v[i].r);
      for (int i = 0; i < v.length-1; i++)
        renderLine(v[i].r, v[i+1].r, theCamera, lineColor);
      if (((Tube) getController().getObject().getObject()).getEndsStyle() == Tube.CLOSED_ENDS)
        renderLine(v[v.length-1].r, v[0].r, theCamera, lineColor);
  
      // Draw the handles for the control points.
  
      boolean selected[] = controller.getSelection();
      for (int i = 0; i < v.length; i++)
        if (!selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
        {
          double z = theCamera.getObjectToView().timesZ(v[i].r);
          renderBox(((int) pos[i].x) - HANDLE_SIZE/2, ((int) pos[i].y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, lineColor);
        }
      Color color = (currentTool.hilightSelection() ? highlightColor : lineColor);
      for (int i = 0; i < v.length; i++)
        if (selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
        {
          double z = theCamera.getObjectToView().timesZ(v[i].r);
          renderBox(((int) pos[i].x) - HANDLE_SIZE/2, ((int) pos[i].y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, color);
        }
    }
  }
}

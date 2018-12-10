/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.*;

/**
 * This is a subclass of EditingTool for tools which edit Mesh objects.  It contains common
 * features used by many tools.
 */

public abstract class MeshEditingTool extends EditingTool
{
  protected MeshEditController controller;

  public MeshEditingTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr);
    this.controller = controller;
  }

  /**
   * This method returns a bounding box for the selected vertices in view coordinates,
   * or null if nothing is selected.
   */

  protected BoundingBox findSelectionBounds(Camera cam)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    MeshVertex vert[] = mesh.getVertices();
    int selected[] = controller.getSelectionDistance();
    double minx, miny, minz, maxx, maxy, maxz;
    boolean anything = false;

    minx = miny = minz = Double.MAX_VALUE;
    maxx = maxy = maxz = -Double.MAX_VALUE;
    for (int i = 0; i < vert.length; i++)
      {
        if (selected[i] == 0)
          {
            anything = true;
            Vec3 v = cam.getObjectToView().times(vert[i].r);
            if (v.x < minx) minx = v.x;
            if (v.x > maxx) maxx = v.x;
            if (v.y < miny) miny = v.y;
            if (v.y > maxy) maxy = v.y;
            if (v.z < minz) minz = v.z;
            if (v.z > maxz) maxz = v.z;
          }
      }
    if (anything)
      return new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
    return null;
  }
}

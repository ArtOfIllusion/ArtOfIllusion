/* Copyright (C) 2001-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tools;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;

/** This dialog box allows the user to specify options for creating lathed objects. */

public class LatheDialog extends BDialog
{
  LayoutWindow window;
  Curve theCurve;
  ObjectInfo curveInfo;
  RadioButtonGroup axisGroup;
  BRadioButton xBox, yBox, zBox, endsBox;
  ValueField radiusField, segmentsField;
  ValueSlider angleSlider;
  BButton okButton, cancelButton;
  ObjectPreviewCanvas preview;
  
  private static int counter = 1;

  public LatheDialog(LayoutWindow window, ObjectInfo curve)
  {
    super(window, "Lathe", true);
    this.window = window;
    theCurve = (Curve) curve.getObject();
    curveInfo = curve;

    // Layout the window.
    
    FormContainer content = new FormContainer(3, 10);
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
    content.add(new BLabel("Select Lathe Axis:"), 0, 0, 2, 1);
    axisGroup = new RadioButtonGroup();
    content.add(xBox = new BRadioButton("X axis", false, axisGroup), 0, 1, 2, 1);
    content.add(yBox = new BRadioButton("Y axis", true, axisGroup), 0, 2, 2, 1);
    content.add(zBox = new BRadioButton("Z axis", false, axisGroup), 0, 3, 2, 1);
    content.add(endsBox = new BRadioButton("Line through endpoints", false, axisGroup), 0, 4, 2, 1);
    axisGroup.addEventLink(SelectionChangedEvent.class, this, "makeObject");
    content.add(new BLabel("Total Rotation Angle:"), 0, 5, 2, 1);
    content.add(angleSlider = new ValueSlider(0.0, 360.0, 180, 360.0), 0, 6, 2, 1);
    angleSlider.addEventLink(ValueChangedEvent.class, this, "makeObject");
    content.add(new BLabel("Radius:"), 0, 7);
    content.add(new BLabel("Segments:"), 0, 8);
    content.add(radiusField = new ValueField(0.0, ValueField.NONE), 1, 7);
    content.add(segmentsField = new ValueField(8.0, ValueField.POSITIVE+ValueField.INTEGER), 1, 8);
    radiusField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    segmentsField.addEventLink(ValueChangedEvent.class, this, "makeObject");

    // Add the preview canvas.
    
    content.add(preview = new ObjectPreviewCanvas(null), 2, 0, 1, 9);
    preview.setPreferredSize(new Dimension(150, 150));
    
    // Add the buttons at the bottom.
    
    RowContainer buttons = new RowContainer();
    buttons.add(okButton = Translate.button("ok", this, "doOk"));
    buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
    content.add(buttons, 0, 9, 3, 1, new LayoutInfo());
    selectDefaults();
    makeObject();
    pack();
    UIUtilities.centerDialog(this, window);
    setVisible(true);
  }
  
  private void doOk()
  {
    CoordinateSystem coords = curveInfo.getCoords().duplicate();
    Vec3 offset = curveInfo.getCoords().fromLocal().times(theCurve.getVertices()[0].r).minus(coords.fromLocal().times(((Mesh) preview.getObject().getObject()).getVertices()[0].r));
    coords.setOrigin(coords.getOrigin().plus(offset));
    window.addObject(preview.getObject().getObject(), coords, "Lathed Object "+(counter++), null);
    window.setSelection(window.getScene().getNumObjects()-1);
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
    window.updateImage();
    dispose();
  }

  // Select default values for the various options.
  
  private void selectDefaults()
  {
    MeshVertex vert[] = theCurve.getVertices();

    if (!theCurve.isClosed() && vert[0].r.distance(vert[vert.length-1].r) > 0.0)
      {
        axisGroup.setSelection(endsBox);
        return;
      }
    endsBox.setEnabled(false);
    double minx = Double.MAX_VALUE;
    for (int i = 0; i < vert.length; i++)
      if (vert[i].r.x < minx)
        minx = vert[i].r.x;
    minx = Math.max(-minx, 0.0);
    radiusField.setValue(Math.ceil(minx));
  }
  
  // Create the extruded object.
  
  private void makeObject()
  {
    int segments = (int) segmentsField.getValue();
    double angle = angleSlider.getValue(), radius = radiusField.getValue();
    int axis;
    if (axisGroup.getSelection() == xBox)
      axis = LatheTool.X_AXIS;
    else if (axisGroup.getSelection() == yBox)
      axis = LatheTool.Y_AXIS;
    else if (axisGroup.getSelection() == zBox)
      axis = LatheTool.Z_AXIS;
    else
      axis = LatheTool.AXIS_THROUGH_ENDS;
    SplineMesh mesh = (SplineMesh) latheCurve(theCurve, axis, segments, angle, radius);
    Texture tex = window.getScene().getDefaultTexture();
    mesh.setTexture(tex, tex.getDefaultMapping(mesh));
    preview.setObject(mesh);
    preview.repaint();
  }

  protected static Mesh latheCurve(Curve theCurve, int latheAxis, int segments, double angle, double latheRadius)
  {
    MeshVertex vert[] = theCurve.getVertices();
    Vec3 axis, radius, center = new Vec3();
    double angleStep = angle*Math.PI/(segments*180.0);
    boolean closed = false;

    if (angle == 360.0)
      closed = true;
    else
      segments++;

    // Determine the rotation axis.

    if (latheAxis == LatheTool.X_AXIS)
      {
        axis = Vec3.vx();
        radius = Vec3.vy();
      }
    else if (latheAxis == LatheTool.Y_AXIS)
      {
        axis = Vec3.vy();
        radius = Vec3.vx();
      }
    else if (latheAxis == LatheTool.Z_AXIS)
      {
        axis = Vec3.vz();
        radius = Vec3.vx();
      }
    else if (latheAxis == LatheTool.AXIS_THROUGH_ENDS)
      {
        axis = vert[0].r.minus(vert[vert.length-1].r);
        axis.normalize();
        center.set(vert[0].r);
        radius = new Vec3();
        for (int i = 0; i < vert.length; i++)
          radius.add(vert[i].r);
        radius.scale(1.0/vert.length);
        radius.subtract(center);
        radius.subtract(axis.times(axis.dot(center)));
        radius.normalize();
      }
    else
      throw new IllegalArgumentException("Illegal value specified for lathe axis");
    center.add(radius.times(-latheRadius));

    // Calculate the vertices of the lathed surface.

    Vec3 v[][] = new Vec3 [segments][vert.length], cm = new Vec3();
    CoordinateSystem coords = new CoordinateSystem(center, axis, radius);
    for (int i = 0; i < segments; i++)
      {
        Mat4 m = coords.fromLocal().times(Mat4.zrotation(i*angleStep)).times(coords.toLocal());
        for (int j = 0; j < vert.length; j++)
          {
            v[i][j] = m.times(vert[j].r);
            cm.add(v[i][j]);
          }
      }

    // Create the arrays of smoothness values.

    float usmooth[] = new float [segments], vsmooth[] = new float [vert.length];
    float s[] = theCurve.getSmoothness();
    for (int i = 0; i < segments; i++)
      usmooth[i] = 1.0f;
    for (int i = 0; i < s.length; i++)
      vsmooth[i] = s[i];
    int smoothMethod = theCurve.getSmoothingMethod();
    if (smoothMethod == Mesh.NO_SMOOTHING)
      {
        for (int i = 0; i < s.length; i++)
          vsmooth[i] = 0.0f;
        smoothMethod = Mesh.APPROXIMATING;
      }
    else
      for (int i = 0; i < s.length; i++)
        vsmooth[i] = s[i];

    // Center it.

    cm.scale(1.0/(segments*vert.length));
    for (int i = 0; i < v.length; i++)
      for (int j = 0; j < v[i].length; j++)
        v[i][j].subtract(cm);
    SplineMesh mesh = new SplineMesh(v, usmooth, vsmooth, smoothMethod, closed, theCurve.isClosed());
    mesh.makeRightSideOut();
    return mesh;
  }
}
/* Copyright (C) 2001-2005 by Peter Eastman

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
import artofillusion.object.TriangleMesh.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;

/** This dialog box allows the user to specify options for creating extruded objects. */

public class ExtrudeDialog extends BDialog
{
  private LayoutWindow window;
  private BComboBox objChoice, pathChoice;
  private RadioButtonGroup pathGroup;
  private BRadioButton pathBox, xBox, yBox, zBox, vectorBox;
  private BCheckBox orientBox;
  private ValueField distField, xField, yField, zField, segField, angleField, tolField;
  private BButton okButton, cancelButton;
  private ObjectPreviewCanvas preview;
  private Vector objects, paths;

  private static int counter = 1;

  public ExtrudeDialog(LayoutWindow window)
  {
    super(window, "Extrude", true);
    this.window = window;
    Scene scene = window.getScene();
    int selection[] = window.getSelectedIndices();
    
    // Identify the objects that can be extruded, and the paths along which they can be
    // extruded.

    objects = new Vector();
    paths = new Vector();
    for (int i = 0; i < selection.length; i++)
    {
      ObjectInfo obj = scene.getObject(selection[i]);
      if (obj.getObject() instanceof Curve)
      {
        objects.addElement(obj);
        paths.addElement(obj);
      }
      else if ((obj.getObject() instanceof TriangleMesh ||
          obj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT) &&
          !obj.getObject().isClosed())
        objects.addElement(obj);
    }
    if (objects.size() == 1)
      paths.removeAllElements();

    // Layout the window.
    
    FormContainer content = new FormContainer(4, 10);
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
    content.add(new BLabel("Object to Extrude:"), 0, 0, 2, 1);
    content.add(objChoice = new BComboBox(), 0, 1, 2, 1);
    for (int i = 0; i < objects.size(); i++)
      objChoice.add(((ObjectInfo) objects.elementAt(i)).getName());
    objChoice.addEventLink(ValueChangedEvent.class, this, "stateChanged");
    content.add(new BLabel("Extrude Direction:"), 0, 2, 2, 1);
    pathGroup = new RadioButtonGroup();
    content.add(xBox = new BRadioButton("X", true, pathGroup), 0, 3);
    content.add(yBox = new BRadioButton("Y", true, pathGroup), 0, 4);
    content.add(zBox = new BRadioButton("Z", true, pathGroup), 0, 5);
    content.add(pathBox = new BRadioButton("Curve", true, pathGroup), 0, 6);
    content.add(vectorBox = new BRadioButton("Vector", true, pathGroup), 0, 7);
    pathBox.setEnabled(paths.size() > 0);
    pathGroup.addEventLink(SelectionChangedEvent.class, this, "stateChanged");
    pathGroup.setSelection(zBox);
    RowContainer distanceRow = new RowContainer();
    content.add(distanceRow, 1, 4);
    distanceRow.add(new BLabel("Distance:"));
    distanceRow.add(distField = new ValueField(1.0, ValueField.POSITIVE, 5));
    distField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    content.add(pathChoice = new BComboBox(), 1, 6);
    for (int i = 0; i < paths.size(); i++)
      pathChoice.add(((ObjectInfo) paths.elementAt(i)).getName());
    pathChoice.addEventLink(ValueChangedEvent.class, this, "stateChanged");
    RowContainer vectorRow = new RowContainer();
    content.add(vectorRow, 1, 7);
    vectorRow.add(new BLabel("X"));
    vectorRow.add(xField = new ValueField(0.0, ValueField.NONE, 4));
    xField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    vectorRow.add(new BLabel("Y"));
    vectorRow.add(yField = new ValueField(0.0, ValueField.NONE, 4));
    yField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    vectorRow.add(new BLabel("Z"));
    vectorRow.add(zField = new ValueField(1.0, ValueField.NONE, 4));
    zField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    content.add(orientBox = new BCheckBox("Orientation Follows Curve", true), 0, 8, 2, 1);
    orientBox.addEventLink(ValueChangedEvent.class, this, "stateChanged");
    content.add(new BLabel("Number of Segments:"), 2, 0);
    content.add(new BLabel("Twist (degrees):"), 2, 1);
    content.add(new BLabel("Surface Accuracy:"), 2, 2);
    content.add(segField = new ValueField(1.0, ValueField.POSITIVE+ValueField.INTEGER, 5), 3, 0);
    content.add(angleField = new ValueField(0.0, ValueField.NONE, 5), 3, 1);
    content.add(tolField = new ValueField(0.1, ValueField.POSITIVE, 5), 3, 2);
    segField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    angleField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    tolField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    if (paths.size() > 0)
      for (int i = 0; i < scene.getNumObjects(); i++)
        if (scene.getObject(i) != paths.elementAt(0))
        {
          objChoice.setSelectedIndex(i);
          break;
        }
    content.add(preview = new ObjectPreviewCanvas((ObjectInfo) objects.elementAt(0)), 2, 3, 2, 6,
        new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 9, 4, 1, new LayoutInfo());
    buttons.add(okButton = Translate.button("ok", this, "doOk"));
    buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
    makeObject();
    pack();
    UIUtilities.centerDialog(this, window);
    updateComponents();
    setVisible(true);
  }
  
  /** Deal with changes to the checkboxes and choices. */
  
  private void stateChanged()
  {
    makeObject();
    updateComponents();
  }
  
  /** Enable or disable components, based on the current selections. */
  
  private void updateComponents()
  {
    distField.setEnabled(xBox.getState() || yBox.getState() || zBox.getState());
    xField.setEnabled(vectorBox.getState());
    yField.setEnabled(vectorBox.getState());
    zField.setEnabled(vectorBox.getState());
    pathChoice.setEnabled(pathBox.getState());
    segField.setEnabled(!pathBox.getState());
    orientBox.setEnabled(pathBox.getState());
    Object3D profile = ((ObjectInfo) objects.elementAt(objChoice.getSelectedIndex())).getObject();
    tolField.setEnabled(!(profile instanceof Curve || profile instanceof TriangleMesh));
    if (pathBox.getState())
      okButton.setEnabled(objects.elementAt(objChoice.getSelectedIndex()) != paths.elementAt(pathChoice.getSelectedIndex()));
    else
      okButton.setEnabled(true);
  }
  
  private void doOk()
  {
    ObjectInfo profile = (ObjectInfo) objects.elementAt(objChoice.getSelectedIndex());
    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    if (profile.getObject() instanceof Mesh)
    {
      Vec3 offset = profile.getCoords().fromLocal().times(((Mesh) profile.getObject()).getVertices()[0].r).minus(coords.fromLocal().times(((Mesh) preview.getObject().getObject()).getVertices()[0].r));
      coords.setOrigin(coords.getOrigin().plus(offset));
    }
    window.addObject(preview.getObject().getObject(), coords, "Extruded Object "+(counter++), null);
    window.setSelection(window.getScene().getNumObjects()-1);
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
    window.updateImage();
    dispose();
  }
  
  // Create the extruded object.
  
  private void makeObject()
  {
    ObjectInfo profile = (ObjectInfo) objects.elementAt(objChoice.getSelectedIndex());
    Curve path;
    CoordinateSystem pathCoords;
    
    if (pathBox.getState())
      {
        ObjectInfo info = (ObjectInfo) paths.elementAt(pathChoice.getSelectedIndex());
        path = (Curve) info.getObject();
        pathCoords = info.getCoords();
      }
    else
      {
        Vec3 dir = new Vec3();
        if (xBox.getState())
          dir.x = distField.getValue();
        else if (yBox.getState())
          dir.y = distField.getValue();
        else if (zBox.getState())
          dir.z = distField.getValue();
        else
          dir.set(xField.getValue(), yField.getValue(), zField.getValue());
        Vec3 v[] = new Vec3 [(int) segField.getValue()+1];
        float smooth[] = new float [v.length];
        for (int i = 0; i < v.length; i++)
          {
            v[i] = new Vec3(dir);
            v[i].scale(i/segField.getValue());
            smooth[i] = 1.0f;
          }
        path = new Curve(v, smooth, Mesh.INTERPOLATING, false);
        pathCoords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
      }
    Object3D obj;
    if (profile.getObject() == path)
      obj = null;
    else if (profile.getObject() instanceof TriangleMesh)
      obj = extrudeMesh((TriangleMesh) profile.getObject(), path, profile.getCoords(), pathCoords, angleField.getValue()*Math.PI/180.0, orientBox.getState());
    else if (profile.getObject() instanceof Curve)
      obj = extrudeCurve((Curve) profile.getObject(), path, profile.getCoords(), pathCoords, angleField.getValue()*Math.PI/180.0, orientBox.getState());
    else
      obj = extrudeMesh(profile.getObject().convertToTriangleMesh(tolField.getValue()), path, profile.getCoords(), pathCoords, angleField.getValue()*Math.PI/180.0, orientBox.getState());
    Texture tex = window.getScene().getDefaultTexture();
    obj.setTexture(tex, tex.getDefaultMapping(obj));
    preview.setObject(obj);
    preview.repaint();
  }
  
  /** Extrude a curve into a spline mesh.
      
      @param profile     the curve to extrude
      @param profCoords  the coordinate system of the profile
      @param dir         the direction and distance along which to extrude it
      @param segments    the number of segments to create
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */
  
  public static Object3D extrudeCurve(Curve profile, CoordinateSystem profCoords, Vec3 dir, int segments, double angle, boolean orient)
  {
    Vec3 v[] = new Vec3 [segments+1];
    float smooth[] = new float [v.length];
    for (int i = 0; i < v.length; i++)
      {
        v[i] = new Vec3(dir);
        v[i].scale(i*segments);
        smooth[i] = 1.0f;
      }
    Curve path = new Curve(v, smooth, Mesh.INTERPOLATING, false);
    return extrudeCurve(profile, path, profCoords, new CoordinateSystem(), angle, orient);
  }
  
  /** Extrude a curve into a spline mesh.
      
      @param profile     the curve to extrude
      @param path        the path along which to extrude it
      @param profCoords  the coordinate system of the profile
      @param pathCoords  the coordinate system of the path
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */
  
  public static Object3D extrudeCurve(Curve profile, Curve path, CoordinateSystem profCoords, CoordinateSystem pathCoords, double angle, boolean orient)
  {
    MeshVertex profVert[] = profile.getVertices(), pathVert[] = path.getVertices();
    Vec3 profv[] = new Vec3 [profVert.length], pathv[] = new Vec3[pathVert.length];
    Vec3 subdiv[], center = new Vec3(), zdir[], updir[], t[], v[][];
    float usmooth[] = new float [pathVert.length], vsmooth[] = new float [profVert.length];
    float profSmooth[] = profile.getSmoothness(), pathSmooth[] = path.getSmoothness();
    CoordinateSystem localCoords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    Mat4 rotate;
    int i, j;
    
    for (i = 0; i < profVert.length; i++)
      profv[i] = profCoords.fromLocal().timesDirection(profVert[i].r);
    for (i = 0; i < pathVert.length; i++)
      pathv[i] = pathCoords.fromLocal().timesDirection(pathVert[i].r);

    // Construct the Minimally Rotating Frame at every point along the path.  First, 
    // subdivide the path and determine its direction at the starting point.
    
    subdiv = new Curve(pathv, pathSmooth, path.getSmoothingMethod(), path.isClosed()).subdivideCurve().getVertexPositions();
    t = new Vec3 [subdiv.length];
    zdir = new Vec3 [subdiv.length];
    updir = new Vec3 [subdiv.length];
    t[0] = subdiv[1].minus(subdiv[0]);
    t[0].normalize();
    zdir[0] = Vec3.vz();
    updir[0] = Vec3.vy();
    
    // Now find two vectors perpendicular to the path, and determine how much they
    // contribute to the z and up directions.
    
    Vec3 dir1, dir2;
    double zfrac1, zfrac2, upfrac1, upfrac2;
    zfrac1 = t[0].dot(zdir[0]);
    zfrac2 = Math.sqrt(1.0-zfrac1*zfrac1);
    dir1 = zdir[0].minus(t[0].times(zfrac1));
    dir1.normalize();
    upfrac1 = t[0].dot(updir[0]);
    upfrac2 = Math.sqrt(1.0-upfrac1*upfrac1);
    dir2 = updir[0].minus(t[0].times(upfrac1));
    dir2.normalize();
    
    // Propagate the vectors along the path.
    
    for (i = 1; i < subdiv.length; i++)
      {
        if (i == subdiv.length-1)
          {
            if (path.isClosed())
              t[i] = subdiv[0].minus(subdiv[subdiv.length-2]);
            else
              t[i] = subdiv[subdiv.length-1].minus(subdiv[subdiv.length-2]);
          }
        else
          t[i] = subdiv[i+1].minus(subdiv[i-1]);
        t[i].normalize();
        if (orient)
          {
            dir1 = dir1.minus(t[i].times(t[i].dot(dir1)));
            dir1.normalize();
            dir2 = dir2.minus(t[i].times(t[i].dot(dir2)));
            dir2.normalize();
            zdir[i] = t[i].times(zfrac1).plus(dir1.times(zfrac2));
            updir[i] = t[i].times(upfrac1).plus(dir2.times(upfrac2));
          }
        else
          {
            zdir[i] = zdir[i-1];
            updir[i] = updir[i-1];
          }
      }
    
    // Set the smoothness values.
    
    if (path.getSmoothingMethod() != Mesh.NO_SMOOTHING)
      for (i = 0; i < usmooth.length; i++)
        usmooth[i] = pathSmooth[i];
    if (profile.getSmoothingMethod() != Mesh.NO_SMOOTHING)
      for (i = 0; i < vsmooth.length; i++)
        vsmooth[i] = profSmooth[i];

    // If one curve is interpolating and the other is approximating, use a subdivided
    // version of the interpolating one to more accurately get its shape (since the final
    // mesh will be approximating).
    
    if (profile.getSmoothingMethod() == Mesh.APPROXIMATING && path.getSmoothingMethod() == Mesh.INTERPOLATING)
      {
        pathv = subdiv;
        usmooth = new float [pathv.length];
        for (i = 0; i < usmooth.length; i++)
          {
            if (i%2 == 0)
              usmooth[i] = Math.min(pathSmooth[i/2]*2.0f, 1.0f);
            else
              usmooth[i] = 1.0f;
          }
      }
    if (profile.getSmoothingMethod() == Mesh.INTERPOLATING && path.getSmoothingMethod() == Mesh.APPROXIMATING)
      {
        profv = new Curve(profv, profSmooth, profile.getSmoothingMethod(), profile.isClosed()).subdivideCurve().getVertexPositions();
        vsmooth = new float [profv.length];
        for (i = 0; i < vsmooth.length; i++)
          {
            if (i%2 == 0)
              vsmooth[i] = Math.min(profSmooth[i/2]*2.0f, 1.0f);
            else
              vsmooth[i] = 1.0f;
          }
      }
    
    // Create the extruded surface.
    
    v = new Vec3 [pathv.length][profv.length];
    for (i = 0; i < pathv.length; i++)
      {
        localCoords.setOrigin(pathv[i]);
        int k = (pathv.length == subdiv.length ? i : 2*i);
        localCoords.setOrientation(zdir[k], updir[k]);
        if (angle != 0.0)
          {
            rotate = Mat4.axisRotation(t[k], i*angle/(pathv.length-1));
            localCoords.transformAxes(rotate);
          }
        for (j = 0; j < profv.length; j++)
          {
            v[i][j] = localCoords.fromLocal().times(profv[j]);
            center.add(v[i][j]);
          }
      }
    
    // Center it.
    
    center.scale(1.0/(profv.length*pathv.length));
    for (i = 0; i < pathv.length; i++)
      for (j = 0; j < profv.length; j++)
        v[i][j].subtract(center);
    SplineMesh mesh = new SplineMesh(v, usmooth, vsmooth, Math.max(profile.getSmoothingMethod(), 
        path.getSmoothingMethod()), path.isClosed(), profile.isClosed());
    mesh.makeRightSideOut();
    return mesh;
  }

  /** Extrude a triangle mesh into a solid object.
      
      @param profile     the TriangleMesh to extrude
      @param profCoords  the coordinate system of the profile
      @param dir         the direction and distance along which to extrude it
      @param segments    the number of segments to create
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */
  
  public static Object3D extrudeMesh(TriangleMesh profile, CoordinateSystem profCoords, Vec3 dir, int segments, double angle, boolean orient)
  {
    Vec3 v[] = new Vec3 [segments+1];
    float smooth[] = new float [v.length];
    for (int i = 0; i < v.length; i++)
      {
        v[i] = new Vec3(dir);
        v[i].scale(i*segments);
        smooth[i] = 1.0f;
      }
    Curve path = new Curve(v, smooth, Mesh.INTERPOLATING, false);
    return extrudeMesh(profile, path, profCoords, new CoordinateSystem(), angle, orient);
  }
  
  /** Extrude a triangle mesh into a solid object.
      
      @param profile     the TriangleMesh to extrude
      @param path        the path along which to extrude it
      @param profCoords  the coordinate system of the profile
      @param pathCoords  the coordinate system of the path
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */
  
  public static Object3D extrudeMesh(TriangleMesh profile, Curve path, CoordinateSystem profCoords, CoordinateSystem pathCoords, double angle, boolean orient)
  {
    Vertex profVert[] = (Vertex [] ) profile.getVertices();
    MeshVertex pathVert[] = path.getVertices();
    Edge profEdge[] = profile.getEdges();
    Face profFace[] = profile.getFaces();
    Vec3 profv[] = new Vec3 [profVert.length], pathv[] = new Vec3[pathVert.length];
    Vec3 subdiv[], center, zdir[], updir[], t[], v[];
    float pathSmooth[] = path.getSmoothness();
    CoordinateSystem localCoords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    Mat4 rotate;
    int numBoundaryEdges = 0, numBoundaryPoints = 0, i, j, k;
    int boundaryEdge[], boundaryPoint[];
    
    for (i = 0; i < profVert.length; i++)
      profv[i] = profCoords.fromLocal().timesDirection(profVert[i].r);
    for (i = 0; i < pathVert.length; i++)
      pathv[i] = pathCoords.fromLocal().timesDirection(pathVert[i].r);
    if (path.getSmoothingMethod() == Mesh.NO_SMOOTHING)
      for (i = 0; i < pathSmooth.length; i++)
        pathSmooth[i] = 0.0f;
    
    // Make a list of the edges and vertices which are on the boundary of the mesh.
    
    boolean onBound[] = new boolean [profv.length];
    for (i = 0; i < profEdge.length; i++)
      if (profEdge[i].f2 == -1)
        {
          numBoundaryEdges++;
          onBound[profEdge[i].v1] = onBound[profEdge[i].v2] = true;
        }
    for (i = 0; i < onBound.length; i++)
      if (onBound[i])
        numBoundaryPoints++;
    boundaryEdge = new int [numBoundaryEdges];
    boundaryPoint = new int [numBoundaryPoints];
    for (i = 0, j = 0; i < profEdge.length; i++)
      if (profEdge[i].f2 == -1)
        boundaryEdge[j++] = i;
    for (i = 0, j = 0; i < onBound.length; i++)
      if (onBound[i])
        boundaryPoint[j++] = i;

    // Find which direction each boundary edge points in.
    
    boolean forward[] = new boolean [boundaryEdge.length];
    int edgeVertIndex[][] = new int [boundaryEdge.length][2];
    for (i = 0; i < boundaryEdge.length; i++)
      {
        Edge ed = profEdge[boundaryEdge[i]];
        Face fc = profFace[ed.f1];
        forward[i] = ((fc.v1 == ed.v1 && fc.v2 == ed.v2) || 
            (fc.v2 == ed.v1 && fc.v3 == ed.v2) || (fc.v3 == ed.v1 && fc.v1 == ed.v2));
        for (j = 0; j < boundaryPoint.length; j++)
          {
            if (boundaryPoint[j] == ed.v1)
              edgeVertIndex[i][0] = j;
            else if (boundaryPoint[j] == ed.v2)
              edgeVertIndex[i][1] = j;
          }
      }

    // Make up a list of the indices for every point on the side of the extruded object.
    
    int index[][];
    if (path.isClosed())
      {
        index = new int [pathv.length+1][boundaryPoint.length];
        for (i = 0; i < boundaryPoint.length; i++)
          {
            for (j = 0; j < pathv.length; j++)
              index[j][i] = j*boundaryPoint.length+i;
            index[j][i] = i;
          }
      }
    else
      {
        index = new int [pathv.length][boundaryPoint.length];
        for (i = 0; i < boundaryPoint.length; i++)
          {
            index[0][i] = boundaryPoint[i];
            index[pathv.length-1][i] = boundaryPoint[i]+profv.length;
            for (j = 1; j < pathv.length-1; j++)
              index[j][i] = (j-1)*boundaryPoint.length+i+2*profv.length;
          }
      }

    // Construct the Minimally Rotating Frame at every point along the path.  First, 
    // subdivide the path and determine its direction at the starting point.
    
    subdiv = new Curve(pathv, pathSmooth, path.getSmoothingMethod(), path.isClosed()).subdivideCurve().getVertexPositions();
    t = new Vec3 [subdiv.length];
    zdir = new Vec3 [subdiv.length];
    updir = new Vec3 [subdiv.length];
    t[0] = subdiv[1].minus(subdiv[0]);
    t[0].normalize();
    zdir[0] = Vec3.vz();
    updir[0] = Vec3.vy();
    
    // Now find two vectors perpendicular to the path, and determine how much they
    // contribute to the z and up directions.
    
    Vec3 dir1, dir2;
    double zfrac1, zfrac2, upfrac1, upfrac2;
    zfrac1 = t[0].dot(zdir[0]);
    zfrac2 = Math.sqrt(1.0-zfrac1*zfrac1);
    dir1 = zdir[0].minus(t[0].times(zfrac1));
    dir1.normalize();
    upfrac1 = t[0].dot(updir[0]);
    upfrac2 = Math.sqrt(1.0-upfrac1*upfrac1);
    dir2 = updir[0].minus(t[0].times(upfrac1));
    dir2.normalize();
    
    // Propagate the vectors along the path.
    
    for (i = 1; i < subdiv.length; i++)
      {
        if (i == subdiv.length-1)
          {
            if (path.isClosed())
              t[i] = subdiv[0].minus(subdiv[subdiv.length-2]);
            else
              t[i] = subdiv[subdiv.length-1].minus(subdiv[subdiv.length-2]);
          }
        else
          t[i] = subdiv[i+1].minus(subdiv[i-1]);
        t[i].normalize();
        if (orient)
          {
            dir1 = dir1.minus(t[i].times(t[i].dot(dir1)));
            dir1.normalize();
            dir2 = dir2.minus(t[i].times(t[i].dot(dir2)));
            dir2.normalize();
            zdir[i] = t[i].times(zfrac1).plus(dir1.times(zfrac2));
            updir[i] = t[i].times(upfrac1).plus(dir2.times(upfrac2));
          }
        else
          {
            zdir[i] = zdir[i-1];
            updir[i] = updir[i-1];
          }
      }
    
    // Create the extruded surface.
    
    if (path.isClosed())
      v = new Vec3 [numBoundaryPoints*pathv.length];
    else
      v = new Vec3 [2*profv.length+numBoundaryPoints*(pathv.length-2)];
    Vector newEdge = new Vector(), newFace = new Vector();
    boolean angled = (profile.getSmoothingMethod() == Mesh.NO_SMOOTHING && path.getSmoothingMethod() != Mesh.NO_SMOOTHING);
    if (!path.isClosed())
      {
        // Add two copies of the profile mesh, to serve as the ends.
        
        localCoords.setOrigin(pathv[0]);
        localCoords.setOrientation(zdir[0], updir[0]);
        for (i = 0; i < profv.length; i++)
          v[i] = localCoords.fromLocal().times(profv[i]);
        k = (pathv.length == subdiv.length ? pathv.length-1 : 2*(pathv.length-1));
        localCoords.setOrigin(pathv[pathv.length-1]);
        localCoords.setOrientation(zdir[k], updir[k]);
        if (angle != 0.0)
          {
            rotate = Mat4.axisRotation(t[k], angle);
            localCoords.transformAxes(rotate);
          }
        for (i = 0; i < profv.length; i++)
          v[i+profv.length] = localCoords.fromLocal().times(profv[i]);
        
        // Add the edges and faces.
        
        for (i = 0; i < profEdge.length; i++)
          {
            float smoothness = profEdge[i].smoothness;
            if (angled || profEdge[i].f2 == -1)
              smoothness = 0.0f;
            newEdge.addElement(new EdgeInfo(profEdge[i].v1, profEdge[i].v2, smoothness));
            newEdge.addElement(new EdgeInfo(profEdge[i].v1+profv.length, profEdge[i].v2+profv.length, smoothness));
          }
        for (i = 0; i < profFace.length; i++)
          {
            Face f = profFace[i];
            newFace.addElement(new int [] {f.v1, f.v2, f.v3});
            newFace.addElement(new int [] {f.v1+profv.length, f.v3+profv.length, f.v2+profv.length});
          }
      }
    for (i = 0; i < pathv.length; i++)
      {
        if (!path.isClosed() && i == pathv.length-1)
          break;
        
        // Add each row of triangles and edges around the side.
        
        for (j = 0; j < boundaryEdge.length; j++)
          {
            int v1, v2;
            if (forward[j])
              {
                v1 = edgeVertIndex[j][0];
                v2 = edgeVertIndex[j][1];
              }
            else
              {
                v1 = edgeVertIndex[j][1];
                v2 = edgeVertIndex[j][0];
              }
            newFace.addElement(new int [] {index[i][v1], index[i+1][v1], index[i+1][v2]});
            newFace.addElement(new int [] {index[i][v2], index[i][v1], index[i+1][v2]});
            EdgeInfo ed1 = new EdgeInfo(index[i][v1], index[i+1][v1], angled ? 0.0f : profVert[boundaryPoint[v1]].smoothness);
            newEdge.addElement(ed1);
            ed1 = new EdgeInfo(index[i][v2], index[i+1][v2], angled ? 0.0f : profVert[boundaryPoint[v2]].smoothness);
            newEdge.addElement(ed1);
            ed1 = new EdgeInfo(index[i][v1], index[i+1][v2], 1.0f);
            newEdge.addElement(ed1);
            if (path.isClosed() || i > 0)
              {
                ed1 = new EdgeInfo(index[i][v1], index[i][v2], pathSmooth[i]);
                newEdge.addElement(ed1);
              }
          }
        
        // Add each row of points around the side.
        
        localCoords.setOrigin(pathv[i]);
        k = (pathv.length == subdiv.length ? i : 2*i);
        localCoords.setOrientation(zdir[k], updir[k]);
        if (angle != 0.0)
          {
            rotate = Mat4.axisRotation(t[k], i*angle/(pathv.length-1));
            localCoords.transformAxes(rotate);
          }
        for (j = 0; j < boundaryPoint.length; j++)
          v[index[i][j]] = localCoords.fromLocal().times(profv[boundaryPoint[j]]);
      }
    
    // Center it.
    
    center = new Vec3();
    for (i = 0; i < v.length; i++)
      center.add(v[i]);
    center.scale(1.0/v.length);
    for (i = 0; i < v.length; i++)
      v[i].subtract(center);
    
    // Build the final object.
    
    int faces[][] = new int [newFace.size()][];
    for (i = 0; i < faces.length; i++)
      faces[i] = (int []) newFace.elementAt(i);
    TriangleMesh mesh = new TriangleMesh(v, faces);
    Edge meshEdge[] = mesh.getEdges();
    for (i = 0; i < newEdge.size(); i++)
      {
        EdgeInfo info = (EdgeInfo) newEdge.elementAt(i);
        if (info.smoothness == 1.0f)
          continue;
        for (j = 0; j < meshEdge.length; j++)
          if ((meshEdge[j].v1 == info.v1 && meshEdge[j].v2 == info.v2) || 
              (meshEdge[j].v1 == info.v2 && meshEdge[j].v2 == info.v1))
            meshEdge[j].smoothness = info.smoothness;
      }
    mesh.setSmoothingMethod(Math.max(profile.getSmoothingMethod(), path.getSmoothingMethod()));
    mesh.makeRightSideOut();
    return mesh;
  }
  
  // Inner class used for storing information about new edges being created.
  
  private static class EdgeInfo
  {
    int v1, v2;
    float smoothness;
    
    public EdgeInfo(int vert1, int vert2, float smooth)
    {
      v1 = vert1;
      v2 = vert2;
      smoothness = smooth;
    }
  }
}
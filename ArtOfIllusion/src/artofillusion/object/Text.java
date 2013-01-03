package artofillusion.object;

/* Copyright (C) 2012 by Peter Eastman and Julio Sangrador-Patón

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;

public class Text extends ObjectCollection
{
  public static enum TextType {Outline, Tube, Surface, Solid};
  private TextType type;
  private String fontName, text;
  private boolean bold, italic;
  private double thickness;
  private int subdivisions;

  private double tolerance = 0.1; // What to do about this?

  public Text()
  {
    type = TextType.Outline;
    text = "Text";
    fontName = "Serif";
    thickness = 0.1;
    subdivisions = 1;
  }

  public String getText()
  {
    return text;
  }

  public void setText(String text)
  {
    this.text = text;
    cachedObjects = null;
    cachedBounds = null;
  }

  public TextType getType()
  {
    return type;
  }

  public void setType(TextType type)
  {
    this.type = type;
    cachedObjects = null;
    cachedBounds = null;
  }

  public boolean getBold()
  {
    return bold;
  }

  public void setBold(boolean bold)
  {
    this.bold = bold;
    cachedObjects = null;
    cachedBounds = null;
  }

  public boolean getItalic()
  {
    return italic;
  }

  public void setItalic(boolean italic)
  {
    this.italic = italic;
    cachedObjects = null;
    cachedBounds = null;
  }

  public String getFontName()
  {
    return fontName;
  }

  public void setFontName(String fontName)
  {
    this.fontName = fontName;
    cachedObjects = null;
    cachedBounds = null;
  }

  public int getSubdivisions()
  {
    return subdivisions;
  }

  public void setSubdivisions(int subdivisions)
  {
    this.subdivisions = subdivisions;
    cachedObjects = null;
    cachedBounds = null;
  }

  public double getThickness()
  {
    return thickness;
  }

  public void setThickness(double thickness)
  {
    this.thickness = thickness;
    cachedObjects = null;
    cachedBounds = null;
  }

  @Override
  protected Enumeration<ObjectInfo> enumerateObjects(ObjectInfo info, boolean interactive, Scene scene)
  {
    ArrayList<ObjectInfo> objects = new ArrayList<ObjectInfo>();
    int style = Font.PLAIN;
    if (bold)
      style |= Font.BOLD;
    if (italic)
      style |= Font.ITALIC;
    Font font = new Font(fontName, style, 1);
    Texture texture = getTexture();
    try
    {
      GlyphVector glyphVector = font.createGlyphVector(new FontRenderContext(null, true, true), text);
      int numGlyphs = glyphVector.getNumGlyphs();

      for (int glyphIndex = 0; glyphIndex < numGlyphs; glyphIndex++) {
        Shape currentOutline = glyphVector.getGlyphOutline(glyphIndex);
        PathIterator pathIterator = currentOutline.getPathIterator(null);
        float segmentCoords[] = new float[6];
        Vec3 pointsRuntimeTypeArray[] = new Vec3[1];
        Vector<Vec3> points = new Vector<Vec3>();
        Vector<Float> smoothnesses = new Vector<Float>();
        boolean firstCurveOfGlyph = true;
        String glyphName = Character.toString(text.charAt(glyphVector.getGlyphCharIndex(glyphIndex)));
        ObjectInfo fullLetterOI = null;
        while(!pathIterator.isDone()){
          switch(pathIterator.currentSegment(segmentCoords)) {
            case PathIterator.SEG_MOVETO:
              points.add(new Vec3((double)segmentCoords[0],-(double)segmentCoords[1],0));
              smoothnesses.add(1.0f);
              break;
            case PathIterator.SEG_LINETO:
              if (smoothnesses.size() > 0)
                smoothnesses.set(smoothnesses.size()-1, 0.0f);
              points.add(new Vec3((double)segmentCoords[0],-(double)segmentCoords[1],0));
              smoothnesses.add(0.0f);
              break;
            case PathIterator.SEG_QUADTO:
              points.add(new Vec3((double)segmentCoords[0],-(double)segmentCoords[1],0));
              smoothnesses.add(1.0f);
              points.add(new Vec3((double)segmentCoords[2],-(double)segmentCoords[3],0));
              smoothnesses.add(1.0f);
              break;
            case PathIterator.SEG_CUBICTO:
              points.add(new Vec3((double)segmentCoords[0],-(double)segmentCoords[1],0));
              smoothnesses.add(1.0f);
              points.add(new Vec3((double)segmentCoords[2],-(double)segmentCoords[3],0));
              smoothnesses.add(1.0f);
              points.add(new Vec3((double)segmentCoords[4],-(double)segmentCoords[5],0));
              smoothnesses.add(1.0f);
              break;
            case PathIterator.SEG_CLOSE:
              // Sometimes the initial point is duplicated at the end,
              // so try to remove the duplicate
              if((points.elementAt(0)).distance(points.elementAt(points.size()-1))<0.0001) {
                points.removeElementAt(points.size()-1);
                smoothnesses.removeElementAt(smoothnesses.size()-1);
              }
              // Convert the vectors into arrays
              float smoothnessesArray[] = new float[smoothnesses.size()];
              for (int i = 0; i < smoothnesses.size(); i++) {
                smoothnessesArray[i] = smoothnesses.elementAt(i);
              }
              Curve theCurve = new Curve(points.toArray(pointsRuntimeTypeArray),smoothnessesArray,Mesh.APPROXIMATING,true);
              ObjectInfo currentGlyphOI = new ObjectInfo(theCurve, new CoordinateSystem(), glyphName);
              currentGlyphOI.setTexture(texture, texture.getDefaultMapping(currentGlyphOI.object));
              if(type == TextType.Surface || type == TextType.Solid) {
                // try to triangulate the curve
                TriangleMesh theMesh = theCurve.subdivideCurve(subdivisions).convertToTriangleMesh(tolerance);
                if (theMesh == null)
                {
                  pathIterator.next();
                  continue;
                }
                currentGlyphOI = new ObjectInfo(theMesh, new CoordinateSystem(), glyphName);
                currentGlyphOI.setTexture(texture, texture.getDefaultMapping(currentGlyphOI.object));
                if(firstCurveOfGlyph) {
                  fullLetterOI = currentGlyphOI;
                }
                else { // More curves, see if they intersect or unite what we already have
                  ObjectInfo meshToTestForIntersection = new ObjectInfo(solidify((TriangleMesh) currentGlyphOI.getObject(), 0.2), currentGlyphOI.coords.duplicate(), glyphName);
                  meshToTestForIntersection.setTexture(texture, texture.getDefaultMapping(meshToTestForIntersection.object)); // for getBounds() to work
                  Vec3 coordsDiff = currentGlyphOI.getBounds().getCenter().minus(meshToTestForIntersection.getBounds().getCenter());
                  meshToTestForIntersection.coords.setOrigin(meshToTestForIntersection.coords.getOrigin().plus(coordsDiff));
                  CSGObject testCSG = new CSGObject(fullLetterOI,meshToTestForIntersection,CSGObject.INTERSECTION);
                  TriangleMesh testCSGMesh = testCSG.convertToTriangleMesh(tolerance);
                  if(testCSGMesh.getEdges().length > 0) { // Intersects
                    BoundingBox bounds1 = fullLetterOI.getBounds();
                    BoundingBox bounds2 = currentGlyphOI.getBounds();
                    boolean firstIsLarger = ((bounds1.maxx-bounds1.minx)*(bounds1.maxy-bounds1.miny) >= (bounds2.maxx-bounds2.minx)*(bounds2.maxy-bounds2.miny));
                    ObjectInfo firstMesh = (firstIsLarger ? fullLetterOI : currentGlyphOI);
                    ObjectInfo secondMesh = (firstIsLarger ? currentGlyphOI : fullLetterOI);
                    ObjectInfo meshToCut = new ObjectInfo(solidify((TriangleMesh) secondMesh.getObject(), 0.2), secondMesh.coords.duplicate(), glyphName);
                    meshToCut.setTexture(texture, texture.getDefaultMapping(meshToCut.object));  // for getBounds() to work
                    coordsDiff = secondMesh.getBounds().getCenter().minus(meshToCut.getBounds().getCenter());
                    meshToCut.coords.setOrigin(meshToCut.coords.getOrigin().plus(coordsDiff));
                    CSGObject aCSG = new CSGObject(firstMesh, meshToCut, CSGObject.DIFFERENCE12);
                    TriangleMesh aCSGMesh = aCSG.convertToTriangleMesh(tolerance);
  //                  fullLetterOI = new ObjectInfo(meshToCut.object,fullLetterOI.coords.duplicate(), glyphName);

                    // Make sure that vertices around the cutout region have the correct smoothness.

                    TriangleMesh cutout = (TriangleMesh) secondMesh.getObject();
                    for (int i = 0; i < cutout.getVertices().length; i++)
                      if (cutout.getVertex(i).smoothness == 0.0f)
                      {
                        for (int j = 0; j < aCSGMesh.getVertices().length; j++)
                          if (cutout.getVertex(i).r.distance2(aCSGMesh.getVertex(j).r) < 1e-10)
                            aCSGMesh.getVertex(j).smoothness = 0.0f;
                      }
                    fullLetterOI = new ObjectInfo(aCSGMesh,fullLetterOI.coords.duplicate(), glyphName);
                    fullLetterOI.setTexture(texture, texture.getDefaultMapping(fullLetterOI.object));
                  }
                  else { // Unites
                    CSGObject aCSG = new CSGObject(fullLetterOI, currentGlyphOI, CSGObject.UNION);
                    TriangleMesh aCSGMesh = aCSG.convertToTriangleMesh(tolerance);
                    fullLetterOI = new ObjectInfo(aCSGMesh,fullLetterOI.coords.duplicate(), glyphName);
                    fullLetterOI.setTexture(texture, texture.getDefaultMapping(fullLetterOI.object));
                  }
                  try { // Optimize the mesh while we are building it
                    fullLetterOI.object = TriangleMesh.optimizeMesh((TriangleMesh) fullLetterOI.object);
                    fullLetterOI.clearCachedMeshes();
                  } catch (Exception e) {e.printStackTrace();} // Should we act? Not sure...
                }
                firstCurveOfGlyph = false;
              }
              else if(type == TextType.Tube) {
                int nVerts = theCurve.getVertices().length;
                double tubeThickness[] = new double[nVerts];
                for(int t = 0; t < nVerts; t++) {
                  tubeThickness[t]=thickness;
                }
                Tube theTube = new Tube(theCurve, tubeThickness, Tube.CLOSED_ENDS);
                theTube.setTexture(texture, texture.getDefaultMapping(theTube));
                objects.add(new ObjectInfo(theTube, new CoordinateSystem(), glyphName));
              }
              else { // User wants only the curves
                objects.add(currentGlyphOI);
              }
              segmentCoords = new float[6];
              pointsRuntimeTypeArray = new Vec3[1];
              points = new Vector<Vec3>();
              smoothnesses = new Vector<Float>();
          } /* Segments loop */
          pathIterator.next();
        } /* Curves loop */
        if (fullLetterOI != null) {
          if(type == TextType.Surface || type == TextType.Solid) {
            try { // Final optimization of the mesh, maybe it is redundant.
              TriangleMesh mesh = (TriangleMesh) fullLetterOI.getObject();
              mesh.setSmoothingMethod(Mesh.APPROXIMATING);
              fullLetterOI.object = TriangleMesh.optimizeMesh(mesh);
              fullLetterOI.clearCachedMeshes();
            }
            catch (Exception e) {e.printStackTrace();} // Again, should we act?
          }
          if(type == TextType.Solid) { // Extrude the shape.
            boolean selection[] = new boolean [((TriangleMesh) fullLetterOI.getObject()).getEdges().length];
            Arrays.fill(selection, true);
            new TriMeshSimplifier((TriangleMesh) fullLetterOI.getObject(), selection, 1e-6, null);
            TriangleMesh mesh = solidify((TriangleMesh) fullLetterOI.getObject(), thickness);
            ObjectInfo extrudedMeshOI = new ObjectInfo(mesh, fullLetterOI.coords.duplicate(), glyphName);
            extrudedMeshOI.setTexture(texture, texture.getDefaultMapping(extrudedMeshOI.object));  // for getBounds() to work
            Vec3 coordsDiff = fullLetterOI.getBounds().getCenter().minus(extrudedMeshOI.getBounds().getCenter());
            extrudedMeshOI.coords.setOrigin(extrudedMeshOI.coords.getOrigin().plus(coordsDiff));
            fullLetterOI = extrudedMeshOI;
  //          boolean selection[] = new boolean [mesh.getEdges().length];
  //          Arrays.fill(selection, true);
  //          new TriMeshSimplifier(mesh, selection, tolerance*0.01, null);
  //          mesh.autosmoothMeshEdges(1.5);
            mesh.setSmoothingMethod(Mesh.APPROXIMATING);
            fullLetterOI.object = mesh = TriangleMesh.optimizeMesh(mesh);
            fullLetterOI.clearCachedMeshes();
          }
          if(type != TextType.Outline && type != TextType.Tube && !firstCurveOfGlyph){ // We had already added them for these
            objects.add(fullLetterOI);
          }
        }

      } /* Glyphs loop */
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return Collections.enumeration(objects);
  }

  private static TriangleMesh solidify(TriangleMesh mesh, double thickness)
  {
    MeshVertex vert[] = mesh.getVertices();
    Vec3 norm[] = mesh.getNormals();

    // Duplicate and outset the vertices.

    TriangleMesh.Vertex newVert[] = new TriangleMesh.Vertex [vert.length*2];
    double offset = 0.5*thickness;
    for (int i = 0; i < norm.length; i++)
      if (norm[i].z != 0.0)
      {
        offset = 0.5*thickness*norm[i].z;
        break;
      }
    for (int i = 0; i < vert.length; i++)
    {
      newVert[i] = mesh.new Vertex((TriangleMesh.Vertex) vert[i]);
      newVert[i+vert.length] = mesh.new Vertex((TriangleMesh.Vertex) vert[i]);
//      newVert[i].r.add(norm[i].times(0.5*thickness));
//      newVert[i+vert.length].r.add(norm[i].times(-0.5*thickness));
      newVert[i].r.z += offset;
      newVert[i+vert.length].r.z -= offset;
    }

    // Count the boundary edges.

    TriangleMesh.Edge edge[] = mesh.getEdges();
    int numBoundary = 0;
    for (TriangleMesh.Edge e : edge)
      if (e.f2 == -1)
        numBoundary++;

    // Duplicate the faces.

    TriangleMesh.Face face[] = mesh.getFaces();
    int newFace[][] = new int [face.length*2+numBoundary*2][3];
    for (int i = 0; i < face.length; i++)
    {
      newFace[i][0] = face[i].v1;
      newFace[i][1] = face[i].v2;
      newFace[i][2] = face[i].v3;
      newFace[face.length+i][0] = face[i].v1+vert.length;
      newFace[face.length+i][1] = face[i].v3+vert.length;
      newFace[face.length+i][2] = face[i].v2+vert.length;
    }

    // Add faces around the boundary.

    for (int i = 0, j = face.length*2; i < edge.length; i++)
    {
      if (edge[i].f2 == -1)
      {
        TriangleMesh.Edge e = edge[i];
        newFace[j][0] = e.v2;
        newFace[j][1] = e.v1;
        newFace[j++][2] = e.v1+vert.length;
        newFace[j][0] = e.v2;
        newFace[j][1] = e.v1+vert.length;
        newFace[j++][2] = e.v2+vert.length;
      }
    }

    // Create the new mesh.

    TriangleMesh newMesh = new TriangleMesh(newVert, newFace);

    // Copy the edge smoothness values over.

    TriangleMesh.Face newf[] = newMesh.getFaces();
    TriangleMesh.Edge newe[] = newMesh.getEdges();
    for (int i = 0; i < face.length; i++)
    {
      newe[newf[i].e1].smoothness = edge[face[i].e1].smoothness;
      newe[newf[i].e2].smoothness = edge[face[i].e2].smoothness;
      newe[newf[i].e3].smoothness = edge[face[i].e3].smoothness;
      newe[newf[i+face.length].e1].smoothness = edge[face[i].e3].smoothness;
      newe[newf[i+face.length].e2].smoothness = edge[face[i].e2].smoothness;
      newe[newf[i+face.length].e3].smoothness = edge[face[i].e1].smoothness;
    }

    // Set the edges around the boundary of the original mesh to have a smoothness of 0, so that
    // we get a sharp corner.

    int firstNewFace = face.length*2;
    for (TriangleMesh.Edge e : newe)
    {
      int j = 0;
      if (e.f1 >= firstNewFace)
        j++;
      if (e.f2 >= firstNewFace)
        j++;
      if (j == 1)
        e.smoothness = 0.0f;
    }

    // Set any edge connecting two non-smooth vertices to all not be smooth.

    for (TriangleMesh.Edge e : newe)
      if (newMesh.getVertex(e.v1).smoothness == 0.0f && newMesh.getVertex(e.v2).smoothness == 0.0f)
        e.smoothness = 0.0f;
    return newMesh;
  }

  @Override
  public boolean isEditable()
  {
    return true;
  }

  @Override
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    RadioButtonGroup meshType = new RadioButtonGroup();
    final BRadioButton meshSpline = new BRadioButton("Silhouette", type == TextType.Outline, meshType);
    final BRadioButton mesh2D = new BRadioButton("2D surface", type == TextType.Surface, meshType);
    final BRadioButton mesh3D = new BRadioButton("3D solid", type == TextType.Solid, meshType);
    final BRadioButton meshTube = new BRadioButton("Tube", type == TextType.Tube, meshType);

    GridContainer kindOfMesh = new GridContainer(2, 2);
    kindOfMesh.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(2, 2, 2, 2), null));
    kindOfMesh.add(meshSpline, 0, 0);
    kindOfMesh.add(mesh2D, 0, 1);
    kindOfMesh.add(mesh3D, 1, 0);
    kindOfMesh.add(meshTube, 1, 1);

    String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    final BList fontsList = new BList(fonts);
    fontsList.setPreferredVisibleRows(10);
    fontsList.setMultipleSelectionEnabled(false);
    int theSelectedIndex = 0;
    for(int y=0;y<fonts.length;y++) {
      if(fonts[y].equals(fontName)) {
        theSelectedIndex = y;
      }
    }
    fontsList.setSelected(theSelectedIndex, true);
    fontsList.scrollToItem(theSelectedIndex);

    final BCheckBox wantsBoldCheck = new BCheckBox("Bold", bold);
    final BCheckBox wantsItalicCheck = new BCheckBox("Italic", italic);
    RowContainer fontStyle = new RowContainer();
    fontStyle.add(wantsBoldCheck);
    fontStyle.add(wantsItalicCheck);

    final BTextField textToShow = new BTextField(text);
    final ValueField toleranceValue = new ValueField(tolerance, ValueField.POSITIVE);
    final ValueField thicknessValue = new ValueField(thickness, ValueField.POSITIVE);
    final ValueField degreeOfSubdivision = new ValueField(subdivisions, ValueField.NONNEGATIVE);

    final ObjectPreviewCanvas preview = new ObjectPreviewCanvas(new ObjectInfo(this.duplicate(), new CoordinateSystem(), ""));
    BoundingBox bounds = preview.getObject().getBounds();
    bounds.outset((bounds.maxx-bounds.minx)/10);
    preview.frameBox(bounds);

    final ActionProcessor actionProcessor = new ActionProcessor();
    Object listener = new Object() {
      void processEvent()
      {
        actionProcessor.addEvent(new Runnable()
        {
          public void run()
          {
            Text text = (Text) preview.getObject().getObject();
            text.setBold(wantsBoldCheck.getState());
            text.setItalic(wantsItalicCheck.getState());
            text.setFontName(fontsList.getSelectedValue().toString());
            text.setText(textToShow.getText());
            text.tolerance = toleranceValue.getValue();
            text.setType(meshSpline.getState()?TextType.Outline:mesh2D.getState()?TextType.Surface:mesh3D.getState()?TextType.Solid:TextType.Tube);
            text.setThickness(thicknessValue.getValue());
            text.setSubdivisions((int) degreeOfSubdivision.getValue());
            preview.objectChanged();
            BoundingBox bounds = preview.getObject().getBounds();
            bounds.outset((bounds.maxx-bounds.minx)/10);
            preview.frameBox(bounds);
            preview.repaint();
          }
        });
      }
    };
    wantsBoldCheck.addEventLink(ValueChangedEvent.class, listener);
    wantsItalicCheck.addEventLink(ValueChangedEvent.class, listener);
    fontsList.addEventLink(SelectionChangedEvent.class, listener);
    textToShow.addEventLink(ValueChangedEvent.class, listener);
    toleranceValue.addEventLink(ValueChangedEvent.class, listener);
    meshType.addEventLink(SelectionChangedEvent.class, listener);
    thicknessValue.addEventLink(ValueChangedEvent.class, listener);
    degreeOfSubdivision.addEventLink(ValueChangedEvent.class, listener);

/* Create and show the dialog */

    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), "Select Parameters for Text Tool",
        new Widget [] {kindOfMesh, UIUtilities.createScrollingList(fontsList), fontStyle, textToShow, toleranceValue, thicknessValue, degreeOfSubdivision, preview},
        new String [] {"Type of Object", "Font", "Style", "Text to Render","Tolerance", "Thickness", "Degree of subdivision", null});
    actionProcessor.stopProcessing();
    if (!dlg.clickedOk()) {
      return;
    }

/* Obtain the user values */

    if (!dlg.clickedOk())
      return;
    copyObject(preview.getObject().getObject());
    cb.run();
  }

  @Override
  public Object3D duplicate()
  {
    Text copy = new Text();
    copy.copyObject(this);
    return copy;
  }

  @Override
  public void copyObject(Object3D obj)
  {
    Text text = (Text) obj;
    setType(text.type);
    setFontName(text.fontName);
    setText(text.text);
    setBold(text.bold);
    setItalic(text.italic);
    setThickness(text.thickness);
    setSubdivisions(text.subdivisions);
    tolerance = text.tolerance;
    copyTextureAndMaterial(obj);
  }

  @Override
  public void setSize(double xsize, double ysize, double zsize)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Keyframe getPoseKeyframe()
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void applyPoseKeyframe(Keyframe k)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}

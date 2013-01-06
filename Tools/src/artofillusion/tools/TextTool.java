package artofillusion.tools;

/* Copyright (C) 2006-2013 by Peter Eastman and Julio Sangrador-Patón

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;

/**
 * TextTool creates meshes or curves that represent text.
 */
public class TextTool implements ModellingTool
{
  public static enum TextType {Outline, Tube, Surface, Solid}

  public TextTool()
  {
  }

  /** Get the text that appear as the menu item.*/

  public String getName()
  {
    return Translate.text("menu.textTool");
  }

  /** Display the dialog. */

  public void commandSelected(LayoutWindow window)
  {
    new TextDialog(window);
  }

  /**
   * Create a set of objects that represent a line of text.
   *
   * @param text         the text string to generate objects for
   * @param fontName     the name of the font to use
   * @param type         the type of objects to create
   * @param bold         whether to use a bold font
   * @param italic       whether to use an italic font
   * @param thickness    if type is Tube, the thickness of the Tube objects to create.  If type is Solid, the thickness of the extruded letters.
   * @param texture      the texture to assign to the created objects (ignored if type is Outline, since then the objects are Curves)
   * @return the objects that were created (one or more for each letter)
   */
  public static ArrayList<ObjectInfo> createText(String text, String fontName, TextType type, boolean bold, boolean italic, double thickness, Texture texture)
  {
    ArrayList<ObjectInfo> objects = new ArrayList<ObjectInfo>();
    int style = Font.PLAIN;
    if (bold)
      style |= Font.BOLD;
    if (italic)
      style |= Font.ITALIC;
    Font font = new Font(fontName, style, 1);
    try
    {
      // Loop over all glyphs (roughly speaking, letters) and create objects for them.

      GlyphVector glyphVector = font.createGlyphVector(new FontRenderContext(null, true, true), text);
      for (int glyphIndex = 0; glyphIndex < glyphVector.getNumGlyphs(); glyphIndex++)
      {
        float segmentCoords[] = new float[6];
        ArrayList<Vec3> points = new ArrayList<Vec3>();
        ArrayList<Float> smoothnesses = new ArrayList<Float>();
        boolean firstCurveOfGlyph = true;
        String glyphName = Character.toString(text.charAt(glyphVector.getGlyphCharIndex(glyphIndex)));
        ObjectInfo fullLetterOI = null;

        // Loop over all segments in the outline of the current glyph and process them.

        Shape currentOutline = glyphVector.getGlyphOutline(glyphIndex);
        PathIterator pathIterator = currentOutline.getPathIterator(null);
        while (!pathIterator.isDone())
        {
          switch(pathIterator.currentSegment(segmentCoords))
          {
            case PathIterator.SEG_MOVETO:
              points.add(new Vec3(segmentCoords[0], -segmentCoords[1], 0));
              smoothnesses.add(1.0f);
              break;
            case PathIterator.SEG_LINETO:
              if (smoothnesses.size() > 0)
                smoothnesses.set(smoothnesses.size()-1, 0.0f);
              points.add(new Vec3(segmentCoords[0], -segmentCoords[1], 0));
              smoothnesses.add(0.0f);
              break;
            case PathIterator.SEG_QUADTO:
              points.add(new Vec3(segmentCoords[0], -segmentCoords[1], 0));
              smoothnesses.add(1.0f);
              points.add(new Vec3(segmentCoords[2], -segmentCoords[3], 0));
              smoothnesses.add(1.0f);
              break;
            case PathIterator.SEG_CUBICTO:
              points.add(new Vec3(segmentCoords[0], -segmentCoords[1], 0));
              smoothnesses.add(1.0f);
              points.add(new Vec3(segmentCoords[2], -segmentCoords[3], 0));
              smoothnesses.add(1.0f);
              points.add(new Vec3(segmentCoords[4], -segmentCoords[5], 0));
              smoothnesses.add(1.0f);
              break;
            case PathIterator.SEG_CLOSE:
              // Sometimes the initial point is duplicated at the end,
              // so try to remove the duplicate
              if ((points.get(0)).distance(points.get(points.size()-1)) < 0.0001)
              {
                points.remove(points.size()-1);
                smoothnesses.remove(smoothnesses.size()-1);
              }
              // Convert the ArrayLists into arrays
              float smoothnessesArray[] = new float[smoothnesses.size()];
              for (int i = 0; i < smoothnesses.size(); i++)
                smoothnessesArray[i] = smoothnesses.get(i);
              Curve theCurve = new Curve(points.toArray(new Vec3[points.size()]), smoothnessesArray, Mesh.APPROXIMATING, true);
              ObjectInfo currentGlyphOI = new ObjectInfo(theCurve, new CoordinateSystem(), glyphName);
              currentGlyphOI.setTexture(texture, texture.getDefaultMapping(currentGlyphOI.object));
              if (type == TextType.Surface || type == TextType.Solid)
              {
                // Try to triangulate the curve.

                Curve subdividedCurve = theCurve.subdivideCurve(1);
                TriangleMesh theMesh = subdividedCurve.convertToTriangleMesh(1);
                if (theMesh == null)
                {
                  // Try subdividing it a second time.

                  subdividedCurve = subdividedCurve.subdivideCurve(1);
                  theMesh = subdividedCurve.convertToTriangleMesh(1);
                  if (theMesh == null)
                  {
                    // Perhaps we can triangulate the original, unsubdivided curve?

                    theMesh = theCurve.convertToTriangleMesh(1);
                    if (theMesh == null)
                    {
                      // Just give up.

                      pathIterator.next();
                      continue;
                    }
                  }
                }
                currentGlyphOI = new ObjectInfo(theMesh, new CoordinateSystem(), glyphName);
                currentGlyphOI.setTexture(texture, texture.getDefaultMapping(currentGlyphOI.object));
                if (firstCurveOfGlyph)
                {
                  fullLetterOI = currentGlyphOI;
                }
                else
                {
                  // More curves, see if they intersect or unite what we already have
                  ObjectInfo meshToTestForIntersection = new ObjectInfo(solidify((TriangleMesh) currentGlyphOI.getObject(), 0.2), currentGlyphOI.coords.duplicate(), glyphName);
                  meshToTestForIntersection.setTexture(texture, texture.getDefaultMapping(meshToTestForIntersection.object)); // for getBounds() to work
                  Vec3 coordsDiff = currentGlyphOI.getBounds().getCenter().minus(meshToTestForIntersection.getBounds().getCenter());
                  meshToTestForIntersection.coords.setOrigin(meshToTestForIntersection.coords.getOrigin().plus(coordsDiff));
                  CSGObject testCSG = new CSGObject(fullLetterOI, meshToTestForIntersection, CSGObject.INTERSECTION);
                  TriangleMesh testCSGMesh = testCSG.convertToTriangleMesh(1);
                  if (testCSGMesh.getEdges().length > 0)
                  {
                    // Intersects
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
                    TriangleMesh aCSGMesh = aCSG.convertToTriangleMesh(1);

                    // Make sure that vertices around the cutout region have the correct smoothness.

                    TriangleMesh cutout = (TriangleMesh) secondMesh.getObject();
                    for (int i = 0; i < cutout.getVertices().length; i++)
                      if (cutout.getVertex(i).smoothness == 0.0f)
                      {
                        for (int j = 0; j < aCSGMesh.getVertices().length; j++)
                          if (cutout.getVertex(i).r.distance2(aCSGMesh.getVertex(j).r) < 1e-10)
                            aCSGMesh.getVertex(j).smoothness = 0.0f;
                      }
                    fullLetterOI = new ObjectInfo(aCSGMesh, fullLetterOI.coords.duplicate(), glyphName);
                    fullLetterOI.setTexture(texture, texture.getDefaultMapping(fullLetterOI.object));
                  }
                  else
                  {
                    // Unites
                    CSGObject aCSG = new CSGObject(fullLetterOI, currentGlyphOI, CSGObject.UNION);
                    TriangleMesh aCSGMesh = aCSG.convertToTriangleMesh(1);
                    fullLetterOI = new ObjectInfo(aCSGMesh, fullLetterOI.coords.duplicate(), glyphName);
                    fullLetterOI.setTexture(texture, texture.getDefaultMapping(fullLetterOI.object));
                  }
                  try
                  {
                    // Optimize the mesh while we are building it
                    fullLetterOI.object = TriangleMesh.optimizeMesh((TriangleMesh) fullLetterOI.object);
                    fullLetterOI.clearCachedMeshes();
                  }
                  catch (Exception e)
                  {
                    e.printStackTrace();
                  }
                }
                firstCurveOfGlyph = false;
              }
              else if (type == TextType.Tube)
              {
                double tubeThickness[] = new double[theCurve.getVertices().length];
                Arrays.fill(tubeThickness, thickness);
                Tube theTube = new Tube(theCurve, tubeThickness, Tube.CLOSED_ENDS);
                theTube.setTexture(texture, texture.getDefaultMapping(theTube));
                objects.add(new ObjectInfo(theTube, new CoordinateSystem(), glyphName));
              }
              else
              {
                // User wants only the curves
                objects.add(currentGlyphOI);
              }
              points = new ArrayList<Vec3>();
              smoothnesses = new ArrayList<Float>();
          } // Segments loop
          pathIterator.next();
        } // Curves loop
        if (fullLetterOI != null)
        {
          if (type == TextType.Surface || type == TextType.Solid)
          {
            boolean selection[] = new boolean [((TriangleMesh) fullLetterOI.getObject()).getEdges().length];
            Arrays.fill(selection, true);
            new TriMeshSimplifier((TriangleMesh) fullLetterOI.getObject(), selection, 1e-6, null);
            try
            {
              // Set any edge connecting two non-smooth vertices to not be smooth.

              TriangleMesh mesh = (TriangleMesh) fullLetterOI.getObject();
              for (TriangleMesh.Edge e : mesh.getEdges())
                if (mesh.getVertex(e.v1).smoothness == 0.0f && mesh.getVertex(e.v2).smoothness == 0.0f)
                  e.smoothness = 0.0f;
              mesh.setSmoothingMethod(Mesh.APPROXIMATING);
              fullLetterOI.object = TriangleMesh.optimizeMesh(mesh);
              fullLetterOI.clearCachedMeshes();
            }
            catch (Exception e)
            {
              e.printStackTrace();
            }
          }
          if (type == TextType.Solid)
          {
            // Extrude the shape.
            TriangleMesh mesh = solidify((TriangleMesh) fullLetterOI.getObject(), thickness);
            ObjectInfo extrudedMeshOI = new ObjectInfo(mesh, fullLetterOI.coords.duplicate(), glyphName);
            extrudedMeshOI.setTexture(texture, texture.getDefaultMapping(extrudedMeshOI.object));  // for getBounds() to work
            Vec3 coordsDiff = fullLetterOI.getBounds().getCenter().minus(extrudedMeshOI.getBounds().getCenter());
            extrudedMeshOI.coords.setOrigin(extrudedMeshOI.coords.getOrigin().plus(coordsDiff));
            fullLetterOI = extrudedMeshOI;
            mesh.setSmoothingMethod(Mesh.APPROXIMATING);
            fullLetterOI.object = mesh = TriangleMesh.optimizeMesh(mesh);
            fullLetterOI.clearCachedMeshes();
          }
          if (type != TextType.Outline && type != TextType.Tube && !firstCurveOfGlyph)
          {
            // We had already added them for these
            objects.add(fullLetterOI);
          }
        }

      } // Glyphs loop
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return objects;
  }

  private static TriangleMesh solidify(TriangleMesh mesh, double thickness)
  {
    MeshVertex vert[] = mesh.getVertices();
    Vec3 norm[] = mesh.getNormals();

    // Duplicate and outset the vertices.

    TriangleMesh.Vertex newVert[] = new TriangleMesh.Vertex [vert.length*2];
    double offset = 0.5*thickness;
    for (Vec3 n : norm)
      if (n.z != 0.0)
      {
        offset = 0.5*thickness*n.z;
        break;
      }
    for (int i = 0; i < vert.length; i++)
    {
      newVert[i] = mesh.new Vertex((TriangleMesh.Vertex) vert[i]);
      newVert[i+vert.length] = mesh.new Vertex((TriangleMesh.Vertex) vert[i]);
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

    // Set any edge connecting two non-smooth vertices to not be smooth.

    for (TriangleMesh.Edge e : newe)
      if (newMesh.getVertex(e.v1).smoothness == 0.0f && newMesh.getVertex(e.v2).smoothness == 0.0f)
        e.smoothness = 0.0f;
    return newMesh;
  }
}

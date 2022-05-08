/* Copyright (C) 2022 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tool.help;


import artofillusion.BevelExtrudeTool;
import artofillusion.CreateCameraTool;
import artofillusion.CreateCubeTool;
import artofillusion.CreateCurveTool;
import artofillusion.CreateCylinderTool;
import artofillusion.CreateLightTool;
import artofillusion.CreatePolygonTool;
import artofillusion.CreateSphereTool;
import artofillusion.CreateSplineMeshTool;
import artofillusion.CreateVertexTool;
import artofillusion.LayoutWindow;
import artofillusion.MeshEditorWindow;
import artofillusion.MoveObjectTool;
import artofillusion.MoveScaleRotateMeshTool;
import artofillusion.MoveScaleRotateObjectTool;
import artofillusion.MoveViewTool;
import artofillusion.ReshapeMeshTool;
import artofillusion.RotateMeshTool;
import artofillusion.RotateObjectTool;
import artofillusion.RotateViewTool;
import artofillusion.ScaleMeshTool;
import artofillusion.ScaleObjectTool;
import artofillusion.Scene;
import artofillusion.SkewMeshTool;
import artofillusion.TaperMeshTool;
import artofillusion.ThickenMeshTool;
import artofillusion.animation.SkeletonTool;
import artofillusion.animation.distortion.SkeletonShapeEditorWindow;
import artofillusion.test.util.RegisterTestResources;
import artofillusion.texture.MoveUVViewTool;
import artofillusion.ui.EditingTool;
import artofillusion.ui.GenericTool;
import buoy.widget.BLabel;
import java.lang.reflect.Field;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.ClassRule;

/**
 *
 * @author MaksK
 */
public class TestToolsHelp {

    private static LayoutWindow layout;
    private static Field helpField;
    
    
    @ClassRule
    public static final RegisterTestResources res = new RegisterTestResources();
    
    @SuppressWarnings("java:S3011")
    @BeforeClass
    public static void setUpClass() throws NoSuchFieldException, IllegalAccessException {        

        layout = new LayoutWindow(new Scene());
        helpField = LayoutWindow.class.getDeclaredField("helpText");
        helpField.setAccessible(true);
    }

    @SuppressWarnings("java:S3011")
    @Before
    public void setUp() throws IllegalAccessException
    {
      BLabel label = (BLabel)helpField.get(layout);
      label.setText(null);
      
      
    }

    @Test
    public void testCreateCameraToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreateCameraTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Click to create a camera.", toolText);
    }

    @Test
    public void testCreateLightToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreateLightTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Click to create a point light, drag for a directional light, control-drag for a spot light.", toolText);
    }

    @Test
    public void testCreateCurveToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreateCurveTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Click to add points, shift-click for a corner.  Double-click or press Enter to finish line, control-double-click to close line.  Double-click icon to set smoothing.", toolText);
    }

    @Test
    public void testMoveViewToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new MoveViewTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to move viewpoint.  Shift-drag to constrain movement, control-drag to zoom.", toolText);
    }

    @Test
    public void testCreateCubeToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreateCubeTool(layout);
        tool.activate();
        BLabel label = (BLabel)helpField.get(layout);
        String toolText = label.getText();
        assertEquals("Drag to create a box.  Shift-drag to create a cube.  Hold Ctrl to expand from center.", toolText);
    }

    @Test
    public void testCreateSphereToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreateSphereTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to create an ellipsoid.  Shift-drag to create a sphere.  Hold Ctrl to expand from center.", toolText);
    }

    @Test
    public void testCreateCylinerToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreateCylinderTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to create a cylinder or cone.  Shift-drag to constrain.  Hold Ctrl to expand from center.  Double-click icon to set shape.", toolText);
    }

    @SuppressWarnings("java:S1192")
    //Expected NPE as no controller passed to Tool
    @Test(expected = NullPointerException.class)
    public void testCreateVertexToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreateVertexTool(layout, null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Expected", toolText);
    }

    @Test
    public void testCreatePolygonToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new CreatePolygonTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to create a 3-sided polygon, shift-drag for a regular polygon, control-drag to expand from center.  Double-click icon to set shape and fill.", toolText);
    }

    @Test
    public void testThickenMeshToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new ThickenMeshTool(layout, null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }

    @Test
    public void testSkewMeshToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new SkewMeshTool(layout, null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }

    @Test
    public void testScaleMeshToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new ScaleMeshTool(layout,null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }

    @Test
    public void testTaperMeshToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new TaperMeshTool(layout,null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }

    @Test
    public void testRotateObjectToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new RotateObjectTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to rotate selected objects.  Drag a handle to constrain rotation.  Double-click icon for options.", toolText);
    }

    @Test
    public void testBevelExtrudeToolWindowHelpText() throws IllegalAccessException
    {
        EditingTool tool = new BevelExtrudeTool(layout, null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }

    @Test
    public void testMoveScaleRotateMeshToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new MoveScaleRotateMeshTool(layout,null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }

    @Test
    public void testCreateSplineMeshToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new CreateSplineMeshTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to create a 5 by 5 flat, approximating spline mesh.  Shift-drag to constrain shape.  Hold Ctrl to expand from center.  Double-click icon to change mesh properties.", toolText);
    }

    @Test
    public void testReshapeMeshToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new ReshapeMeshTool(layout, null);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Select and move points.  Shift adds to selection, Control-drag removes from selection.", toolText);
    }

    @Test
    public void testMoveUVViewToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new MoveUVViewTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to move viewpoint.  Shift-drag to constrain movement, control-drag to zoom.", toolText);
    }

    @Test
    public void testScaleObjectToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new ScaleObjectTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag a handle to resize objects.  Shift-drag preserves shape, control-drag scales around center.  Double-click icon for options.", toolText);
    }

    //Expected NPE as no properly initialized EditorWindow
    @Test(expected = NullPointerException.class)
    public void testSkeletonToolWindowHelpText() throws IllegalAccessException {
        MeshEditorWindow mew = new SkeletonShapeEditorWindow(layout,"SkeletonShape",null,0,null);
        EditingTool tool = new SkeletonTool(mew, true);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Expected", toolText);
    }

    //Expected NPE as no controller passed to Tool
    @Test(expected = NullPointerException.class)
    public void testRotateMeshToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new RotateMeshTool(layout, null, true);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Expected", toolText);
    }

    @Test
    public void testMoveObjectToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new MoveObjectTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to move selected objects.  Shift-drag constrains movement, control-drag moves perpendicular to view.  Double-click icon for options.", toolText);
    }

    @Test
    public void testMoveScaleRotateObjectToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new MoveScaleRotateObjectTool(layout);
        tool.activate();
        
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }

    @Test
    public void testRotateViewToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new RotateViewTool(layout);
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertEquals("Drag to rotate view.  Shift-drag to constrain movement, control-drag to rotate about axis.", toolText);
    }

    @Test
    public void testGenericToolWindowHelpText() throws IllegalAccessException {
        EditingTool tool = new GenericTool(layout, "", "Generic Tool");
        tool.activate();
        String toolText = ((BLabel)helpField.get(layout)).getText();
        assertNull(toolText);
    }
}

/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tool.help;

import artofillusion.ApplicationPreferences;
import artofillusion.ArtOfIllusion;

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
import artofillusion.PluginRegistry;
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
import artofillusion.texture.MoveUVViewTool;
import artofillusion.ui.EditingTool;
import artofillusion.ui.GenericTool;
import artofillusion.ui.ThemeManager;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author MaksK
 */
public class TestToolsHelp {
    
    private static LayoutWindow layout;
    
    public TestToolsHelp() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        PluginRegistry.registerResource("TranslateBundle", "artofillusion", ArtOfIllusion.class.getClassLoader(), "artofillusion", null);
        PluginRegistry.registerResource("UITheme", "default", ArtOfIllusion.class.getClassLoader(), "artofillusion/Icons/defaultTheme.xml", null);
        ThemeManager.initThemes();
        ArtOfIllusion.setPreferences(new ApplicationPreferences());
        layout = new LayoutWindow(new Scene());
    }
    
    @Before
    public void setUp()
    {
        layout.getHelpText().setText(null);
    }
    
    @Test
    public void testCreateCameraToolWindowHelpText()
    {
        EditingTool tool = new CreateCameraTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Click to create a camera.", toolText);
    }
    
    @Test
    public void testCreateLightToolWindowHelpText()
    {
        EditingTool tool = new CreateLightTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Click to create a point light, drag for a directional light, control-drag for a spot light.", toolText);
    }
    
    @Test
    public void testCreateCurveToolWindowHelpText()
    {
        EditingTool tool = new CreateCurveTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Click to add points, shift-click for a corner.  Double-click or press Enter to finish line, control-double-click to close line.  Double-click icon to set smoothing.", toolText);
    }
    
    @Test
    public void testMoveViewToolWindowHelpText()
    {
        EditingTool tool = new MoveViewTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to move viewpoint.  Shift-drag to constrain movement, control-drag to zoom.", toolText);
    }
    
    @Test
    public void testCreateCubeToolWindowHelpText()
    {
        EditingTool tool = new CreateCubeTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to create a box.  Shift-drag to create a cube.", toolText);
    }

    @Test
    public void testCreateSphereToolWindowHelpText()
    {
        EditingTool tool = new CreateSphereTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to create an ellipsoid.  Shift-drag to create a sphere.", toolText);
    }
    
    @Test
    public void testCreateCylinerToolWindowHelpText()
    {
        EditingTool tool = new CreateCylinderTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to create a cylinder or cone.  Shift-drag to constrain.  Double-click icon to set shape.", toolText);
    }
    
    //Expected NPE as no controller passed to Tool
    @Test(expected = NullPointerException.class)
    public void testCreateVertexToolWindowHelpText()
    {
        EditingTool tool = new CreateVertexTool(layout, null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Expected", toolText);
    }
    
    @Test
    public void testCreatePolygonToolWindowHelpText()
    {
        EditingTool tool = new CreatePolygonTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to create a 3-sided polygon, shift-drag for a regular polygon, control-drag for a surface.  Double-click icon to set shape.", toolText);
    }
    
    @Test
    public void testThickenMeshToolWindowHelpText()
    {
        EditingTool tool = new ThickenMeshTool(layout, null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertNull(toolText);
    }
    
    @Test
    public void testSkewMeshToolWindowHelpText()
    {
        EditingTool tool = new SkewMeshTool(layout, null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertNull(toolText);
    }  
    
    @Test
    public void testScaleMeshToolWindowHelpText()
    {
        EditingTool tool = new ScaleMeshTool(layout,null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertNull(toolText);
    }
    
    @Test
    public void testTaperMeshToolWindowHelpText()
    {
        EditingTool tool = new TaperMeshTool(layout,null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertNull(toolText);
    }
    
    @Test
    public void testRotateObjectToolWindowHelpText()
    {
        EditingTool tool = new RotateObjectTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to rotate selected objects.  Drag a handle to constrain rotation.  Double-click icon for options.", toolText);
    }
    
    @Test
    public void testBevelExtrudeToolWindowHelpText()
    {
        EditingTool tool = new BevelExtrudeTool(layout, null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertNull(toolText);
    }
    
    @Test
    public void testMoveScaleRotateMeshToolWindowHelpText() {
        EditingTool tool = new MoveScaleRotateMeshTool(layout,null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertNull(toolText);
    }
    
    @Test
    public void testCreateSplineMeshToolWindowHelpText() {
        EditingTool tool = new CreateSplineMeshTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to create a 5 by 5 flat, approximating spline mesh.  Shift-drag to constrain shape.  Double-click icon to change mesh properties.", toolText);  
    }
    
    @Test
    public void testReshapeMeshToolWindowHelpText() {
        EditingTool tool = new ReshapeMeshTool(layout, null);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Select and move points.  Shift adds to selection, Control-drag removes from selection.", toolText); 
    }
 
    @Test
    public void testMoveUVViewToolWindowHelpText() {
        EditingTool tool = new MoveUVViewTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to move viewpoint.  Shift-drag to constrain movement, control-drag to zoom.", toolText);
    }
    
    @Test
    public void testScaleObjectToolWindowHelpText() {
        EditingTool tool = new ScaleObjectTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag a handle to resize objects.  Shift-drag preserves shape, control-drag scales around center.  Double-click icon for options.", toolText);
    }
    
    //Expected NPE as no properly initialized EditorWindow
    @Test(expected = NullPointerException.class)
    public void testSkeletonToolWindowHelpText() {
        MeshEditorWindow mew = new SkeletonShapeEditorWindow(layout,"SkeletonShape",null,0,null);
        EditingTool tool = new SkeletonTool(mew, true);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Expected", toolText);    
    }
    
    //Expected NPE as no controller passed to Tool
    @Test(expected = NullPointerException.class)
    public void testRotateMeshToolWindowHelpText() {
        EditingTool tool = new RotateMeshTool(layout, null, true);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Expected", toolText);         
    }
    
    @Test
    public void testMoveObjectToolWindowHelpText() {
        EditingTool tool = new MoveObjectTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to move selected objects.  Shift-drag constrains movement, control-drag moves perpendicular to view.  Double-click icon for options.", toolText);
    }
    
    @Test
    public void testMoveScaleRotateObjectToolWindowHelpText() {
        EditingTool tool = new MoveScaleRotateObjectTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertNull(toolText);
    }
    
    @Test
    public void testRotateViewToolWindowHelpText() {
        EditingTool tool = new RotateViewTool(layout);
        tool.activate();
        String toolText = layout.getHelpText().getText();
        assertEquals("Drag to rotate view.  Shift-drag to constrain movement, control-drag to rotate about axis.", toolText);
    }
    
    @Test
    public void testGenericToolWindowHelpText() {
        EditingTool tool = new GenericTool(layout, "", "Generic Tool");
        tool.activate();
        assertNull(layout.getHelpText().getText());
    }
}

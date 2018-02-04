/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.raster;

import artofillusion.ui.Translate;
import artofillusion.ui.ValueField;
import buoy.widget.BCheckBox;
import buoy.widget.BComboBox;
import buoy.widget.BTabbedPane;
import buoy.widget.FormContainer;
import buoy.widget.LayoutInfo;
import buoy.widget.Widget;
import java.awt.Insets;

/**
 *
 * @author MaksK
 */
public class RasterRenderConfigPanelWidget extends BTabbedPane
{
  private static final LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
  private static final LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);

  private final ValueField errorField = new ValueField(0.02, ValueField.POSITIVE, 6);
  
  private BComboBox shadeChoice;
  private BComboBox aliasChoice;
  private BComboBox sampleChoice;
  
  private BCheckBox transparentBox;
  private BCheckBox adaptiveBox;
  private BCheckBox thideBackfaceBox;
  private BCheckBox hdrBox;
  
  private RasterRenderConfigPanelWidget()
  {
    init();
  }
  
  private void init()
  {
    final FormContainer generalPanel = new FormContainer(3, 4);
    generalPanel.add(Translate.label("surfaceAccuracy"), 0, 0, leftLayout);
    generalPanel.add(Translate.label("shadingMethod"), 0, 1, leftLayout);
    generalPanel.add(Translate.label("supersampling"), 0, 2, leftLayout);
    
    generalPanel.add(errorField , 1, 0, rightLayout);
    generalPanel.add(shadeChoice = new BComboBox(new String[]{
        Translate.text("gouraud"),
        Translate.text("hybrid"),
        Translate.text("phong")
    }), 1, 1, rightLayout);
    
    generalPanel.add(aliasChoice = new BComboBox(new String[]{
        Translate.text("none"),
        Translate.text("Edges"),
        Translate.text("Everything")
    }), 1, 2, rightLayout);

    generalPanel.add(sampleChoice = new BComboBox(new String[]{"2x2", "3x3"}), 2, 2, rightLayout);
    sampleChoice.setEnabled(false);
    final FormContainer advancedPanel = new FormContainer(new double [] {0.0, 1.0}, new double [4]);
    
    this.add(generalPanel, Translate.text("general"));
    this.add(advancedPanel, Translate.text("advanced"));
  }
  
  private static RasterRenderConfigPanelWidget instance = null;
  
  public static Widget build()
  {
    if(instance == null)
      instance = new RasterRenderConfigPanelWidget();
    return instance;
  }
}

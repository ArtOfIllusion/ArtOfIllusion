/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.MaterialPreviewer;
import artofillusion.math.RGBColor;
import artofillusion.texture.Texture;
import artofillusion.ui.ActionProcessor;
import artofillusion.ui.Translate;
import artofillusion.ui.ValueSelector;
import buoy.event.ValueChangedEvent;
import buoy.widget.BDialog;
import buoy.widget.BorderContainer;
import buoy.widget.LayoutInfo;
import buoy.widget.RowContainer;
import java.awt.Rectangle;

/**
 *
 * @author MaksK
 */
public class ProceduralTextureCommon
{
  private ProceduralTextureCommon()
  {    
  }
  
  /**
   * Create a Procedure object for texture.
   */

  public static Procedure createTextureProcedure()
  {
    return new Procedure(new OutputModule [] {
      new OutputModule(Translate.text("Diffuse"), Translate.text("white"), 0.0, new RGBColor(1.0f, 1.0f, 1.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Specular"), Translate.text("white"), 0.0, new RGBColor(1.0f, 1.0f, 1.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Transparent"), Translate.text("white"), 0.0, new RGBColor(1.0f, 1.0f, 1.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Emissive"), Translate.text("black"), 0.0, new RGBColor(0.0f, 0.0f, 0.0f), IOPort.COLOR),
      new OutputModule(Translate.text("Transparency"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Specularity"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Shininess"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Roughness"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Cloudiness"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("BumpHeight"), "0", 0.0, null, IOPort.NUMBER),
      new OutputModule(Translate.text("Displacement"), "0", 0.0, null, IOPort.NUMBER)});
  }
  
  
  public static MaterialPreviewer getPreview(ProcedureEditor editor, Texture texture)
  {
    final BDialog dlg = new BDialog(editor.getParentFrame(), "Preview", false);
    BorderContainer content = new BorderContainer();
    final MaterialPreviewer preview = new MaterialPreviewer(texture, null, 200, 160);
    content.add(preview, BorderContainer.CENTER);
    RowContainer row = new RowContainer();
    content.add(row, BorderContainer.SOUTH, new LayoutInfo());
    row.add(Translate.label("Time", ":"));
    final ValueSelector value = new ValueSelector(0.0, -Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
    final ActionProcessor processor = new ActionProcessor();
    row.add(value);
    value.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        processor.addEvent(new Runnable()
        {
          @Override
          public void run()
          {
            preview.getScene().setTime(value.getValue());
            preview.render();
          }
        });
      }
    });
    dlg.setContent(content);
    dlg.pack();
    Rectangle parentBounds = editor.getParentFrame().getBounds();
    Rectangle location = dlg.getBounds();
    location.y = parentBounds.y;
    location.x = parentBounds.x+parentBounds.width;
    dlg.setBounds(location);
    dlg.setVisible(true);
    return preview;
    
  }
}

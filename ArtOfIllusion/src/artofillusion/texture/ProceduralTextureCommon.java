/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.MaterialPreviewer;
import artofillusion.TextureParameter;
import artofillusion.image.ImageMap;
import artofillusion.math.RGBColor;
import artofillusion.procedural.IOPort;
import artofillusion.procedural.ImageModule;
import artofillusion.procedural.Module;
import artofillusion.procedural.OutputModule;
import artofillusion.procedural.ParameterModule;
import artofillusion.procedural.Procedure;
import artofillusion.procedural.ProcedureEditor;
import static artofillusion.texture.Texture.*;
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

  public static Procedure createProcedure()
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
  
  public static TextureParameter[] getTextureParameters(Object texture, Procedure proc)
  {
    Module modules[] = proc.getModules();
    int count = 0;

    for (Module module1 : modules)
    {
      if (module1 instanceof ParameterModule)
      {
        count++;
      }
    }
    
    TextureParameter params[] = new TextureParameter [count];
    count = 0;
    for (Module module : modules)
    {
      if (module instanceof ParameterModule)
      {
        params[count] = ((ParameterModule) module).getParameter(texture);
        ((ParameterModule) module).setIndex(count++);
      }
    }
    return params;
    
  }
  
  /** Determine whether given procedure texture has a non-zero value anywhere for a particular component.
   *  @param proc the Procedure to check 
      @param component    the texture component to check for (one of the Texture *_COMPONENT constants)
  */  
  public static boolean hasTextureComponent(Procedure proc, int component)
  {
    OutputModule output[] = proc.getOutputModules();
    switch (component)
      {
        case DIFFUSE_COLOR_COMPONENT:
          return true;
        case SPECULAR_COLOR_COMPONENT:
          return output[5].inputConnected(0);
        case TRANSPARENT_COLOR_COMPONENT:
          return output[4].inputConnected(0);
        case HILIGHT_COLOR_COMPONENT:
          return output[6].inputConnected(0);
        case EMISSIVE_COLOR_COMPONENT:
          return output[3].inputConnected(0);
        case BUMP_COMPONENT:
          return output[9].inputConnected(0);
        case DISPLACEMENT_COMPONENT:
          return output[10].inputConnected(0);
      }
    return false;
  }
  
  public static boolean procedureUsesImage(Procedure proc, ImageMap image)
  {
    for (Module module: proc.getModules())
      if (module instanceof ImageModule && ((ImageModule) module).getMap() == image)
          return true;
    return false;
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

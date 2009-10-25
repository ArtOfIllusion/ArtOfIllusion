/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;

/** This dialog box allows the user to specify options for CSG objects. */

public class CSGDialog extends BDialog
{
  private CSGObject theObject;
  private CSGModeller modeller;
  private Texture texture;
  private BComboBox opChoice;
  private ObjectPreviewCanvas preview;
  private int operation[];
  private boolean ok;

  public CSGDialog(EditingWindow window, CSGObject obj)
  {
    super(window.getFrame(), true);
    theObject = obj;
    Scene scene = window.getScene();
    texture = scene.getDefaultTexture();
    
    // Layout the window.
    
    BorderContainer content = new BorderContainer();
    setContent(content);
    RowContainer opRow = new RowContainer();
    content.add(opRow, BorderContainer.NORTH);
    opRow.add(new BLabel(Translate.text("Operation")+":"));
    opRow.add(opChoice = new BComboBox());
    int i = 0;
    operation = new int [4];
    if (obj.getObject1().getObject().isClosed() && obj.getObject2().getObject().isClosed())
    {
      opChoice.add(Translate.text("Union"));
      operation[i++] = CSGObject.UNION;
    }
    opChoice.add(Translate.text("Intersection"));
    operation[i++] = CSGObject.INTERSECTION;
    if (obj.getObject2().getObject().isClosed())
    {
      opChoice.add(Translate.text("firstSecond"));
      operation[i++] = CSGObject.DIFFERENCE12;
    }
    if (obj.getObject1().getObject().isClosed())
    {
      opChoice.add(Translate.text("secondFirst"));
      operation[i++] = CSGObject.DIFFERENCE21;
    }
    for (int j = 0; j < i; j++)
      if (obj.getOperation() == operation[j])
        opChoice.setSelectedIndex(j);
    opChoice.addEventLink(ValueChangedEvent.class, this, "makePreview");

    // Add the preview canvas.
    
    content.add(preview = new ObjectPreviewCanvas(null), BorderContainer.CENTER);
    preview.setPreferredSize(new Dimension(200, 200));
    
    // Add the buttons at the bottom.
    
    RowContainer buttons = new RowContainer();
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "dispose"));
    content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
    addEventLink(WindowClosingEvent.class, this, "doOk");
    makePreview();
    pack();
    UIUtilities.centerDialog(this, window.getFrame());
    setVisible(true);
  }
  
  private void doOk()
  {
    theObject.setOperation(operation[opChoice.getSelectedIndex()]);
    ok = true;
    dispose();
  }
  
  // Create a preview object.
  
  private void makePreview()
  {
    if (modeller == null)
    {
      double tol = ArtOfIllusion.getPreferences().getInteractiveSurfaceError();
      TriangleMesh mesh1, mesh2;
  
      mesh1 = theObject.getObject1().getObject().convertToTriangleMesh(tol);
      mesh2 = theObject.getObject2().getObject().convertToTriangleMesh(tol);
      modeller = new CSGModeller(mesh1, mesh2, theObject.getObject1().getCoords(), theObject.getObject2().getCoords());
    }
    TriangleMesh trimesh = modeller.getMesh(operation[opChoice.getSelectedIndex()], texture);
    trimesh.setTexture(texture, texture.getDefaultMapping(trimesh));
    preview.setObject(trimesh);
    preview.repaint();
  }
  
  // Determine whether the user clicked the OK button.
  
  public boolean clickedOk()
  {
    return ok;
  }
}
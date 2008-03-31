/* Copyright (C) 2002-2004 by Peter Eastman

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

/** This dialog box allows the user to specify options for creating a tube. */

public class TubeDialog extends BDialog
{
  LayoutWindow window;
  Curve theCurve;
  ObjectInfo curveInfo;
  Tube theTube;
  BButton okButton, cancelButton;
  ValueField thicknessField;
  BComboBox endsChoice;
  ObjectPreviewCanvas preview;
  
  private static int counter = 1;

  public TubeDialog(LayoutWindow window, ObjectInfo curve)
  {
    super(window, "Tube", true);
    this.window = window;
    curveInfo = curve;
    theCurve = (Curve) curve.getObject();
    Scene scene = window.getScene();
    
    // Layout the window.
    
    FormContainer content = new FormContainer(4, 10);
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
    content.add(new BLabel("Tube Width"), 0, 0);
    content.add(new BLabel("Cap Ends"), 0, 1);
    content.add(thicknessField = new ValueField(0.1, ValueField.POSITIVE, 5), 1, 0);
    thicknessField.addEventLink(ValueChangedEvent.class, this, "makeObject");
    content.add(endsChoice = new BComboBox(new String [] {"Open Ends", "Flat Ends"}), 1, 1);
    endsChoice.setEnabled(!theCurve.isClosed());
    endsChoice.addEventLink(ValueChangedEvent.class, this, "makeObject");
    content.add(preview = new ObjectPreviewCanvas(null), 0, 2, 2, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    preview.setPreferredSize(new Dimension(150, 150));
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 3, 2, 1, new LayoutInfo());
    buttons.add(okButton = Translate.button("ok", this, "doOk"));
    buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
    makeObject();
    pack();
    UIUtilities.centerDialog(this, window);
    setVisible(true);
  }
  
  private void doOk()
  {
    window.addObject(theTube, curveInfo.getCoords().duplicate(), "Tube "+(counter++), null);
    window.setSelection(window.getScene().getNumObjects()-1);
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
    window.updateImage();
    dispose();
  }
  
  // Create the Tube.
  
  private void makeObject()
  {
    MeshVertex vert[] = theCurve.getVertices();
    double thickness[] = new double [vert.length];
    for (int i = 0; i < thickness.length; i++)
      thickness[i] = thicknessField.getValue();
    int endsStyle;
    if (theCurve.isClosed())
      endsStyle = Tube.CLOSED_ENDS;
    else if (endsChoice.getSelectedIndex() == 0)
      endsStyle = Tube.OPEN_ENDS;
    else
      endsStyle = Tube.FLAT_ENDS;
    theTube = new Tube(theCurve, thickness, endsStyle);
    ObjectInfo tubeInfo = new ObjectInfo(theTube, new CoordinateSystem(), "");
    Texture tex = window.getScene().getDefaultTexture();
    tubeInfo.setTexture(tex, tex.getDefaultMapping(theTube));
    preview.setObject(theTube);
    preview.repaint();
  }
}
/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;

/** This class implements the dialog box which is used for the "Transform Points" command.
    It allows the user enter values by which to translate, rotate, and scale the selected
    Points.  It also allows them to specify whether the rotation and scaling should be done
    with respect to the origin of object coordinates, or the center of the selected points. */

public class TransformPointsDialog extends BDialog
{
  private double finalValues[];
  private ValueField fields[];
  private BComboBox centerChoice;

  public TransformPointsDialog(BFrame parent)
  {
    super(parent, Translate.text("transformPoints"), true);
    fields = new ValueField [9];
    layoutDialog();
    pack();
    setResizable(false);
    UIUtilities.centerDialog(this, parent);
    fields[0].requestFocus();
    setVisible(true);
  }

  public double [] getValues()
  {
    return finalValues;
  }
  
  public boolean useSelectionCenter()
  {
    return (centerChoice.getSelectedIndex() == 0);
  }
  
  private void layoutDialog()
  {
    FormContainer content = new FormContainer(4, 6);
    setContent(content);
    LayoutInfo eastLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
    content.add(Translate.label("Move"), 0, 1, eastLayout);
    content.add(Translate.label("Rotate"), 0, 2, eastLayout);
    content.add(Translate.label("Scale"), 0, 3, eastLayout);
    content.add(new BLabel("X"), 1, 0);
    content.add(new BLabel("Y"), 2, 0);
    content.add(new BLabel("Z"), 3, 0);
    for (int i = 0; i < 9; i++)
      content.add(fields[i] = new ValueField(Double.NaN, ValueField.NONE), (i%3)+1, (i/3)+1);
    RowContainer row = new RowContainer();
    content.add(row, 0, 4, 4, 1);
    row.add(Translate.label("transformAround"));
    row.add(centerChoice = new BComboBox(new String [] {
      Translate.text("centerOfSelection"),
      Translate.text("objectOrigin")
    }));
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 5, 4, 1);
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "dispose"));
    addEventLink(WindowClosingEvent.class, this, "dispose");
    addAsListener(this);
  }

  private void doOk()
  {
    finalValues = new double [9];
    for (int i = 0; i < finalValues.length; i++)
    {
      finalValues[i] = fields[i].getValue();
      if (Double.isNaN(finalValues[i]))
        finalValues[i] = (i < 6 ? 0.0 : 1.0);
    }
    dispose();
  }
    
  /** Pressing Return and Escape are equivalent to clicking OK and Cancel. */
    
  private void keyPressed(KeyPressedEvent ev)
  {
    int code = ev.getKeyCode();

    if (code == KeyPressedEvent.VK_ENTER)
      doOk();
    if (code == KeyPressedEvent.VK_ESCAPE)
      dispose();
  }

  /** Add this as a listener to every Widget. */
  
  private void addAsListener(Widget w)
  {
    w.addEventLink(KeyPressedEvent.class, this, "keyPressed");
    if (w instanceof WidgetContainer)
    {
      Iterator iter = ((WidgetContainer) w).getChildren().iterator();
      while (iter.hasNext())
        addAsListener((Widget) iter.next());
    }
  }
}
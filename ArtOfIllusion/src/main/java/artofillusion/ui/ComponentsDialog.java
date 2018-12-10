/* Copyright (C) 1999-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.event.*;
import buoy.widget.*;
import java.awt.*;

/** A ComponentsDialog is a modal dialog containing a line of text, and one or more Widgets
    for the user to edit.  Each Widget has a label next to it.  At the bottom are two
    buttons labeled OK and Cancel. */
   
public class ComponentsDialog extends BDialog
{
  private Widget comp[];
  private boolean ok;
  private Runnable okCallback, cancelCallback;
  private BButton okButton, cancelButton;

  /** Create a modal dialog containing a set of labeled components.
  
      @param parent       the parent of the dialog
      @param prompt       a text string to appear at the top of the dialog
      @param components   the list of components to display
      @param labels       the list of labels for each component
  */

  public ComponentsDialog(WindowWidget parent, String prompt, Widget components[], String labels[])
  {
    this(parent, prompt, components, labels, null, null);
  }
    
  /** Create a non-modal dialog containing a set of labeled components.
  
      @param parent       the parent of the dialog
      @param prompt       a text string to appear at the top of the dialog
      @param components   the list of components to display
      @param labels       the list of labels for each component
      @param onOK         a callback to execute when the user clicks OK
      @param onCancel     a callback to execute when the user clicks Cancel
  */

  public ComponentsDialog(WindowWidget parent, String prompt, Widget components[], String labels[], Runnable onOK, Runnable onCancel)
  {
    super(parent, (onOK == null && onCancel == null));
    comp = components;
    okCallback = onOK;
    cancelCallback = onCancel;
    BorderContainer content = new BorderContainer();
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(10, 10, 10, 10), null));
    content.add(new BLabel(prompt), BorderContainer.NORTH);
    
    // Add the Widgets.
    
    FormContainer center = new FormContainer(new double [] {0.0, 1.0}, new double [components.length]);
    content.add(center, BorderContainer.CENTER);
    for (int i = 0; i < components.length; i++)
    {
      if (labels[i] == null)
        center.add(components[i], 0, i, 2, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(2, 0, 2, 0), null));
      else
      {
        center.add(new BLabel(labels[i]), 0, i, new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(2, 0, 2, 5), null));
        center.add(components[i], 1, i, new LayoutInfo(LayoutInfo.WEST, LayoutInfo.BOTH, new Insets(2, 0, 2, 0), null));
      }
    }
    for (Widget w : UIUtilities.findAllChildren(this))
      w.addEventLink(KeyPressedEvent.class, this, "keyPressed");

    // Add the buttons at the bottom.
    
    RowContainer buttons = new RowContainer();
    content.add(buttons, BorderContainer.SOUTH);
    buttons.add(okButton = Translate.button("ok", this, "buttonPressed"));
    buttons.add(cancelButton = Translate.button("cancel", this, "buttonPressed"));
    okButton.addEventLink(KeyPressedEvent.class, this, "keyPressed");
    cancelButton.addEventLink(KeyPressedEvent.class, this, "keyPressed");
    addEventLink(WindowClosingEvent.class, new Object() {
      void processEvent()
      {
        ok = false;
        closeWindow();
      }
    } );
    setDefaultButton(okButton);
    pack();
    setResizable(false);
    UIUtilities.centerDialog(this, parent);
    setVisible(true);
  }
  
  /** Return true if the user clicked OK, false if they clicked Cancel. */
  
  public boolean clickedOk()
  {
    return ok;
  }
  
  /** Set whether the OK button is enabled. */
  
  public void setOkEnabled(boolean enabled)
  {
    okButton.setEnabled(enabled);
  }

  private void buttonPressed(CommandEvent e)
  {
    String command = e.getActionCommand();

    if (command.equals("cancel"))
      ok = false;
    else
      ok = true;
    closeWindow();
  }
  
  private void closeWindow()
  {
    if (ok && okCallback != null)
      okCallback.run();
    if (!ok && cancelCallback != null)
      cancelCallback.run();
    dispose();
    for (int i = 0; i < comp.length; i++)
      comp[i].removeEventLink(KeyPressedEvent.class, this);
  }
    
  /** Pressing Return and Escape are equivalent to clicking OK and Cancel. */
    
  private void keyPressed(KeyPressedEvent ev)
  {
    int code = ev.getKeyCode();
    if (code == KeyPressedEvent.VK_ESCAPE)
      closeWindow();
  }
}

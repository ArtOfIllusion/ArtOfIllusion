/* Copyright (C) 2000-2006 by Peter Eastman

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
import java.util.*;

/** A PanelDialog is a modal dialog containing a line of text at the top, and a single
    Widget (usually a container with other Widgets).  At the bottom are two buttons labeled OK 
    and Cancel. */
   
public class PanelDialog extends BDialog
{
  private boolean ok;

  /** Create a modal dialog containing a panel.
  
      @param parent       the parent of the dialog
      @param prompt       a text string to appear at the top of the dialog (may be null)
      @param thePanel     the panel to display
  */

  public PanelDialog(WindowWidget parent, String prompt, Widget thePanel)
  {
    super(parent, true);
    BorderContainer content = new BorderContainer();
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(10, 10, 10, 10), null));
    if (prompt != null)
      content.add(new BLabel(prompt), BorderContainer.NORTH);
    content.add(thePanel, BorderContainer.CENTER);
    
    // Add the buttons at the bottom.
    
    RowContainer buttons = new RowContainer();
    content.add(buttons, BorderContainer.SOUTH);
    BButton okButton;
    buttons.add(okButton = Translate.button("ok", this, "buttonPressed"));
    buttons.add(Translate.button("cancel", this, "buttonPressed"));
    addEventLink(WindowClosingEvent.class, new Object() {
      void processEvent()
      {
        ok = false;
        closeWindow();
      }
    } );
    addAsListener(this);
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
    dispose();
    removeAsListener(this);
  }
    
  /** Pressing Return and Escape are equivalent to clicking OK and Cancel. */
    
  private void keyPressed(KeyPressedEvent ev)
  {
    int code = ev.getKeyCode();
    if (code == KeyPressedEvent.VK_ESCAPE)
      closeWindow();
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
  
  /** Remove this as a listener before returning. */
  
  private void removeAsListener(Widget w)
  {
    w.removeEventLink(KeyPressedEvent.class, this);
    if (w instanceof WidgetContainer)
    {
      Iterator iter = ((WidgetContainer) w).getChildren().iterator();
      while (iter.hasNext())
        removeAsListener((Widget) iter.next());
    }
  }
}

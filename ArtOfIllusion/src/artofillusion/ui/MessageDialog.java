/* Copyright (C) 1999,2000,2002,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;

/** A MessageDialog is a modal dialog containing one or more lines of text, and one or more 
    buttons for the user to select from.  Clicking any of the buttons dismisses the dialog.
    <p>
    This class is provided mainly for backward compatibility.  In most cases, it is better to
    use a BStandardDialog, since that will have the correct platform-specific appearance.  This
    class does have two advantages over BStandardDialog, however: it can display an arbitrary
    number of custom buttons, and it automatically breaks text into multiple lines. */

public class MessageDialog extends BDialog
{
  private int pressed, i, number;
  
  public MessageDialog(WindowWidget parent, String message)
  {
    this(parent, UIUtilities.breakString(message));
  }

  public MessageDialog(WindowWidget parent, String message, String[] choices)
  {
    this(parent, UIUtilities.breakString(message), choices);
  }

  public MessageDialog(WindowWidget parent, String[] message)
  {
    this(parent, message, new String[] {Translate.text("button.ok")});
  }

  public MessageDialog(WindowWidget parent, String[] message, String[] choices)
  {
    super(parent, true);
    BorderContainer content = new BorderContainer();
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(10, 10, 10, 10), null));
    RowContainer buttons = new RowContainer();
    content.add(buttons, BorderContainer.SOUTH);
    for (i = 0; i < choices.length; i++)
      {
	BButton btn = new BButton(choices[i]);
	btn.addEventLink(CommandEvent.class, new Object() {
	  int number = i;
	  void processEvent() {pressed = number; dispose();}
        });
	buttons.add(btn);
        btn.addEventLink(KeyPressedEvent.class, this, "keyPressed");
      }
    ColumnContainer msg = new ColumnContainer();
    content.add(msg, BorderContainer.CENTER);
    for (i = 0; i < message.length; i++)
      msg.add(new BLabel(message[i]));
    number = choices.length;
    addEventLink(WindowClosingEvent.class, new Object() {
      void processEvent()
      {
        pressed = number-1;
        dispose();
      }
    } );
    pack();
    setResizable(false);
    UIUtilities.centerDialog(this, parent);
    setVisible(true);
  }
  
  /** Get the index of the button that was pressed. */
  
  public int getChoice()
  {
    return pressed;
  }
    
  /* Pressing Escape is equivalent to clicking the last button. */
    
  private void keyPressed(KeyPressedEvent ev)
  {
    if (ev.getKeyCode() == KeyPressedEvent.VK_ESCAPE)
      pressed = number-1;
    else if (ev.getKeyCode() == KeyPressedEvent.VK_ENTER)
      pressed = 0;
    else
      return;
    dispose();
  }
}


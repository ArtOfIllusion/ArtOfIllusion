/* Copyright (C) 1999-2007 by Peter Eastman

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

/**
 * EditingTool is the superclass of tools for editing objects or scenes.  An EditingTool
 * has an image which appears in a tool palette, allowing the tool to be selected.  When
 * selected, the editing tool responds to events in the scene or object viewer.
 * <p>
 * An EditingTool specifies what types of mouse clicks it wants to receive by the value it
 * returns from its whichClicks() method.  This should be a sum of the contants OBJECT_CLICKS
 * (for mouse clicks on objects), HANDLE_CLICKS (for mouse clicks on handles), and ALL_CLICKS
 * (for all mouse clicks regardless of what they are on).  The exact definition of an "object"
 * or "handle" is not specified.  It is up to the ViewerCanvas generating the events to decide
 * what constitutes an object or handle.
 * <p>
 * An EditingTool may also specify whether the current selection may be changed while that tool
 * is active by the value it returns from its allowSelectionChanges() method.  This method is
 * always called <i>after</i> mousePressed(), allowing the tool to first determine what was clicked
 * on before deciding whether to allow the selection to change in response to the click.
 * <p>
 * More precisely, here is the sequence of actions a ViewerCanvas performs when the mouse is pressed:
 * <ol>
 * <li>If the active EditingTool has requested ALL_CLICKS, its mousePressed() method is invoked.</li>
 * <li>The ViewerCanvas determines what was clicked on.</li>
 * <li>The active EditingTool's allowSelectionChanges() method is invoked and, if it returns true,
 * the selection is updated in response to the click.</li>
 * <li>If the click was on a handle and the active EditingTool has requested HANDLE_CLICKS,
 * its mousePressedOnHandle() method is invoked.</li>
 * <li>Otherwise, if the click was on an object and the active EditingTool has requested OBJECT_CLICKS,
 * its mousePressedOnObject() method is invoked.</li>
 * </ol>
 */

public abstract class EditingTool
{
  protected EditingWindow theWindow;
  protected BFrame theFrame;
  protected ToolButton button;
  
  public EditingTool(EditingWindow win)
  {
    theWindow = win;
    if (win != null)
      theFrame = win.getFrame();
  }
  
  /** Get the EditingWindow to which this tool belongs. */
  
  public EditingWindow getWindow()
  {
    return theWindow;
  }

  /** Get the ToolButton used to represent this tool in a ToolPalette. */

  public ToolButton getButton()
  {
    return button;
  }

  protected void initButton(String name)
  {
    button = ThemeManager.getToolButton(this, name);
  }

  /** Get the tool tip text to display for this tool (or null if it does not have a tool tip). */
  
  public String getToolTipText()
  {
    return null;
  }
  
  public static final int ALL_CLICKS = 1;
  public static final int OBJECT_CLICKS = 2;
  public static final int HANDLE_CLICKS = 4;

  /**
   * Get what types of mouse clicks this tool wants to receive.  This should be a sum of the
   * constants ALL_CLICKS, OBJECT_CLICKS, or HANDLE_CLICKS.
   */
  
  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  /**
   * Get whether the selection may be changed while this tool is active.  The default implementation
   * returns true if whichClicks() requests either OBJECT_CLICKS or HANDLE_CLICKS.
   */

  public boolean allowSelectionChanges()
  {
    int clicks = whichClicks();
    return ((clicks&OBJECT_CLICKS) != 0 || (clicks&HANDLE_CLICKS) != 0);
  }

  /** Get whether the current selection should be hilighted when this tool is active. */
  
  public boolean hilightSelection()
  {
    return true;
  }
  
  /** Draw any graphics that this tool overlays on top of the view. */
  
  public void drawOverlay(ViewerCanvas view)
  {
  }
  
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
  }

  public void mousePressedOnObject(WidgetMouseEvent e, ViewerCanvas view, int obj)
  {
  }

  public void mousePressedOnHandle(WidgetMouseEvent e, ViewerCanvas view, int obj, int handle)
  {
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
  }
  
  public void mouseMoved(WidgetMouseEvent e, ViewerCanvas view)
  {
  }
  
  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
  }

  public void activate()
  {
    theWindow.setTool(this);
    button.setSelected(true);
  }
  
  public void deactivate()
  {
    button.setSelected(false);
  }

  /**
   * Get whether this tool opens a configuration dialog when double-clicked.
   */

  public boolean isEditable()
  {
    return false;
  }


  public void iconDoubleClicked()
  {
  }
}
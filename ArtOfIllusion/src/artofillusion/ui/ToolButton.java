/* Copyright (C) 2007 by François Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.awt.*;

/**
 * A ToolButton provides the user interface for an {@link EditingTool} in a {@link ToolPalette}.
 * This is an abstract class.  Subclasses implement {@link #paint(java.awt.Graphics2D) paint()} to
 * determine the appearance of the button.  {@link DefaultToolButton} is the standard implementation
 * which is used by default, but themes may define their own subclasses to customize the appearance
 * and behavior of buttons.
 * <p>
 * A ToolButton is not a widget.  Most often it is used inside a ToolPalette, which handles events,
 * layout, and so on for the ToolButtons it contains.  If you want to display a ToolButton as an
 * independent widget, you can do that with the {@link ToolButtonWidget} class.
 *
 * @author Francois Guillet
 *
 */
public abstract class ToolButton {

    protected int height;
    protected int width;
    protected int state;
    protected Point position;
    protected Object owner;
    public static final int NORMAL_STATE = 0;
    public static final int SELECTED_STATE = 1;
    public static final int HIGHLIGHTED_STATE = 2;

    /**
     * Constructor for the ToolButton class.
     *
     * @param owner The owner of the button. This allows for specific button behavior depending on the class
     * of the owner.
     */
    public ToolButton(Object owner) {
        this.owner = owner;
        state = NORMAL_STATE;
        position = new Point(0, 0);
    }

    /**
     * Returns the button height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the button width
     */
    public int getWidth() {
        return width;
    }

    /**
     * returns the button size
     */
    public Dimension getSize() {
        return new Dimension(width, height);
    }

    /**
     * Returns the button state. The button can be in normal state, highlighted state
     * or selected state.
     */
    public int getState() {
        return state;
    }

    /**
     * Sets the button as selected. If the button is currently
     * highlighted, selecting it supersedes the highlighted state.
     */
    public void setSelected(boolean selected) {
        if (selected)
            state = SELECTED_STATE;
        else
            state = NORMAL_STATE;
    }

    /**
     * Sets the button as highlighted. Selection precedes over highlighting,
     * so if the button is currently selected, highlighting is ignored.
     */
    public void setHighlighted(boolean highlighted) {
        //selected buttons can't be highlighted
        if (state == SELECTED_STATE)
            return;
        if (highlighted)
            state = HIGHLIGHTED_STATE;
        else
            state = NORMAL_STATE;
    }

    public boolean isSelected() {
        return (state == SELECTED_STATE);
    }

    public boolean isHighlighted() {
        return (state == HIGHLIGHTED_STATE);
    }

    public abstract void paint(Graphics2D g);

    public void setPosition(int x, int y) {
        position = new Point(x, y);
    }

    /**
     * Returns the button position
     */
    public Point getPosition() {
        return position;
    }
}
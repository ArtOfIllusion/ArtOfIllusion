/* Copyright (C) 2007 by François Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.awt.Dimension;

import buoy.event.*;
import buoy.widget.CustomWidget;

/**
 * A ToolBarButton is a simple button that relies on a ToolButton for graphic representation.
 *
 * @author Francois Guillet
 *
 */
public class ToolButtonWidget extends CustomWidget {

	private ToolButton button;

	/**
     * Create a new ToolButtonWidget whose appearance is determined by a ToolButton.
	 */
	public ToolButtonWidget(ToolButton button) {
        this.button = button;
		addEventLink(MousePressedEvent.class, this, "doButtonPressed");
		addEventLink(MouseEnteredEvent.class, this, "doMouseEntered");
		addEventLink(MouseExitedEvent.class, this, "doMouseExited");
		addEventLink(RepaintEvent.class, this, "paint");
	}

	public Dimension getMinimumSize() {
		return button.getSize();
	}

	public Dimension getPreferredSize() {
		return button.getSize();
	}

	public void setSelected(boolean selected) {
		button.setSelected(selected);
		repaint();
	}

	public boolean isSelected() {
		return button.isSelected();
	}

	private void doButtonPressed() {
		button.setSelected(!button.isSelected());
		dispatchEvent(new ValueChangedEvent(this));
		repaint();
	}

	private void doMouseEntered() {
		button.setHighlighted(true);
		repaint();
	}

	private void doMouseExited() {
		button.setHighlighted(false);
		repaint();
	}

	public void paint(RepaintEvent ev) {
		button.paint(ev.getGraphics());
	}
}
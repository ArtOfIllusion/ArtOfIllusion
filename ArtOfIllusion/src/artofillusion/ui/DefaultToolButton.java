/* Copyright (C) 2007 by François Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.awt.*;
import javax.swing.ImageIcon;

/**
 * This ToolButton is the classic button with one icon for standard representation
 * and another one for selected state.
 * 
 * @author Francois Guillet
 *
 */
public class DefaultToolButton extends ToolButton {
    protected ImageIcon icon;
    protected ImageIcon selectedIcon;

    public DefaultToolButton(Object owner, String iconFileName, String selectedIconFilename) {
        super(owner);
        icon = ThemeManager.getIcon(iconFileName);
        selectedIcon = ThemeManager.getIcon(selectedIconFilename);
        height = icon.getIconHeight();
        width = icon.getIconWidth();
    }

    public void paint(Graphics2D g) {
        switch(state) {
        case NORMAL_STATE:
        case HIGHLIGHTED_STATE:
            g.drawImage(icon.getImage(), position.x, position.y, null);
            break;
        case SELECTED_STATE:
            g.drawImage(selectedIcon.getImage(), position.x, position.y, null);
            break;
        }
    }
}
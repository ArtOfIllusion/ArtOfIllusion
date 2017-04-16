/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

/**
 *
 * @author mkhramov
 */
public class ColorSampleComponent extends JPanel {

    private static final Border border = new BevelBorder(BevelBorder.LOWERED);
    
    public ColorSampleComponent() {
        super.setBorder(border);
    }

    @Override
    public void setBorder(Border border) {
        //Suppress change border
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        //super.paintComponent(graphics);
        graphics.setColor(getForeground());
        Insets insets = border.getBorderInsets(this);
        graphics.fillRect(insets.left, insets.top, this.getWidth() - (insets.left + insets.right), this.getHeight() - (insets.bottom + insets.top));
    }
    
}

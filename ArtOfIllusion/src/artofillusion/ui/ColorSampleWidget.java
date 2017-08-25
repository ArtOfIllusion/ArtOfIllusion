/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */
package artofillusion.ui;

import artofillusion.math.RGBColor;
import buoy.widget.Widget;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import static java.awt.event.MouseEvent.BUTTON1;

/**
 *
 * @author mkhramov
 */
public class ColorSampleWidget extends Widget {

    private RGBColor color;
    
    public void setColor(RGBColor color) {
        this.color = color;
        this.component.setForeground(color.getColor());
    }

    public RGBColor getColor() 
    {
        return color;
    }
    
    public ColorSampleWidget(RGBColor color, int width, int height) {
        Dimension size = new Dimension(width, height);
        this.component = new ColorSampleComponent();
        this.color = color.duplicate();
        this.component.setPreferredSize(size);
        this.component.setMaximumSize(size);
        this.component.setForeground(color.getColor());
        
        this.component.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ColorSampleWidget.this.mouseClicked(e);
            }
            
        });
    }
    
    public void mouseClicked(MouseEvent e)
    {
        if(BUTTON1 != e.getButton()) return;
        new ColorChooser(UIUtilities.findFrame(this), Translate.text("ambientColor"), color);
        this.component.setForeground(color.getColor());
    }
    

}

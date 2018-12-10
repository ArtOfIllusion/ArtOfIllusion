/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *
 * @author maksim.khramov
 */
public class Input {
    private Component component = null;
    private String title = null;
    private String defaultValue = null;
    
    private Input() {        
    }
    
    public static Input create() {
        return new Input();
    }
    
    public Input withOwner(Component owner) {
        this.component = owner;
        return this;
    }
    public Input withTitle(String title) {
        this.title = title;
        return this;          
    }
    public Input withDefault(String value) {
        this.defaultValue = value;
        return this;
    }
    
    public String input() {
        return JOptionPane.showInputDialog(component, title, defaultValue, JOptionPane.PLAIN_MESSAGE);
    }
}

/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 *
 * @author maksim.khramov
 */
public final class Messages {
    
    private static final String TITLE = "Art Of Illusion";
    
    private static final Icon icon;
    static {
        icon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("artofillusion/Icons/appIcon.png"));
    }
    
    public static void error(Object message, Component owner) {
        JOptionPane.showMessageDialog(owner, message, TITLE, JOptionPane.ERROR_MESSAGE);
    }
    
    public static void error(Object message) {
        error(message, (Component)null);
    }
    
    public static void warning(Object message, Component owner) {
        JOptionPane.showMessageDialog(owner, message, TITLE, JOptionPane.WARNING_MESSAGE);
    }
    
    public static void warning(Object message) {
      warning(message, (Component)null);
    }
    
    
    public static void information(Object message) {
        information(message, (Component) null);
    }
    
    public static void information(Object message, Component owner) {
        JOptionPane.showMessageDialog(owner, message, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    public static String[] optionsOkCancel() {
        return new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    }
    public static String[] optionsYesNo() {
        return new String [] {Translate.text("Yes"), Translate.text("No")};
    }
    
    public void questionOkCancel() {
        final String options[] = Messages.optionsOkCancel();
    }
}

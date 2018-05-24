/*
 * Copyright 2018.
 * 
 * Created by Maksim Khramov
 * Date: May 23, 2018.
 */
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
    

}

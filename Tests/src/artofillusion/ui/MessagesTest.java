/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.ArtOfIllusion;
import artofillusion.PluginRegistry;
import buoy.widget.BDialog;
import buoy.widget.BStandardDialog;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class MessagesTest {
    
    private static final Logger logger = Logger.getLogger(MessagesTest.class.getName());
    
    @BeforeClass
    public static void setupClass() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
            logger.log(Level.INFO, "Exception at test setup", ex);
        }
        PluginRegistry.registerResource("TranslateBundle", "artofillusion", ArtOfIllusion.class.getClassLoader(), "artofillusion", null);
    }

    @Test
    public void testError() {
        Messages.error("Hello Errors");
    }
    
    @Test
    public void testWarning() {
        Messages.warning("This is warning");
    }
    
    @Test
    public void testInformation() {
      Messages.information("Information message");
    }
    
    @Test
    public void testPlain() {
      JOptionPane.showMessageDialog(null, "Plain message", "", JOptionPane.PLAIN_MESSAGE);
    }
    
    @Test
    public void testPlainStyleDialog() {
        BStandardDialog dlg = new BStandardDialog("Art Of Illusion", Translate.text("savePoseAsGesture"), BStandardDialog.PLAIN);
        String name = dlg.showInputDialog(new BDialog(), null, "New Gesture");
    }
}

/*
 * Copyright 2018 Veeam Software.
 * 
 * Created by Maksim Khramov
 * Date: May 30, 2018.
 */
package artofillusion.ui;

import artofillusion.TitleWindowNB;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class TitleWindowNBTest {

    private static final Logger logger = Logger.getLogger(TitleWindowNBTest.class.getName());
    
    @BeforeClass
    public static void setupClass() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
            logger.log(Level.INFO, "Exception at test setup", ex);
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testShowTitleWindow() throws InterruptedException {

        SwingUtilities.invokeLater(() -> {
            new TitleWindowNB();
        });
        Thread.sleep(2000);
        JOptionPane.showMessageDialog(null, "Switch");
        Thread.sleep(1000);

    }
}

/*
 * Copyright 2018 Veeam Software.
 * 
 * Created by Maksim Khramov
 * Date: May 30, 2018.
 */
package artofillusion.ui;

import artofillusion.TitleWindowNB;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class TitleWindowNBTest {


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

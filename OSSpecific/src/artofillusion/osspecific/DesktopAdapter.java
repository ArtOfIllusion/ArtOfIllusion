/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.osspecific;

import artofillusion.Scene;
import artofillusion.TitleWindow;
import buoy.event.MouseClickedEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 *
 * @author maksim.khramov
 */
public class DesktopAdapter {
    
    protected void handleAbout() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TitleWindow win = new TitleWindow();
                win.addEventLink(MouseClickedEvent.class, win, "dispose");
            }
            
        });
    }
    
    protected void handleOpenFiles(List<File> files) {
        for(File file: files) {
            try {
                artofillusion.ArtOfIllusion.newWindow(new Scene(file, true));
            } catch (IOException ex) {
                System.out.println("Unable to open scene from : " + file + " due: " + ex.getMessage());
            }
        }
    }
}

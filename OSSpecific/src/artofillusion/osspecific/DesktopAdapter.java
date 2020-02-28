/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.osspecific;

import artofillusion.ArtOfIllusion;
import artofillusion.LayoutWindow;
import artofillusion.PreferencesWindow;
import artofillusion.Scene;
import artofillusion.TitleWindow;
import artofillusion.ui.EditingWindow;
import artofillusion.ui.UIUtilities;
import buoy.event.MouseClickedEvent;
import buoy.widget.BFrame;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
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
    
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected void handlePreferences() {
      Window frontWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
      boolean frontIsLayoutWindow = false;
      for (EditingWindow window : ArtOfIllusion.getWindows())
        if (window instanceof LayoutWindow && window.getFrame().getComponent() == frontWindow)
        {
          ((LayoutWindow) window).preferencesCommand();
          frontIsLayoutWindow = true;
          break;
        }
      if (!frontIsLayoutWindow)
      {
        BFrame frame = new BFrame();
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setBounds(screenBounds);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIUtilities.centerWindow(frame);
                new PreferencesWindow(frame);
                frame.dispose();
            }
        });
      }
    }
}

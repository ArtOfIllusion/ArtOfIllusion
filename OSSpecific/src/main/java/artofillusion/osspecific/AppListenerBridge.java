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
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MaksK
 */
public class AppListenerBridge implements com.apple.eawt.ApplicationListener {
    
    private static final Logger LOG = Logger.getLogger(AppListenerBridge.class.getName());
    
    public AppListenerBridge() {
        init();
    }
    
    private void init() {
        Application app = Application.getApplication();
        app.setEnabledAboutMenu(true);
        app.setEnabledPreferencesMenu(true);
        app.addApplicationListener(this);         
    }
    
    @Override
    public void handleAbout(ApplicationEvent event) {
        TitleWindow win = new TitleWindow();
        win.addEventLink(MouseClickedEvent.class, win, "dispose");
        event.setHandled(true);
    }

    @Override
    public void handleOpenApplication(ApplicationEvent event) {
        event.setHandled(true);
    }

    @Override
    public void handleOpenFile(ApplicationEvent event) {
        try {
            ArtOfIllusion.newWindow(new Scene(Paths.get(event.getFilename()).toFile(), true));
        } catch (IOException ex) {
            LOG.log(Level.INFO, "Error opening file", ex);
        }
        event.setHandled(true);
    }

  @Override
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  public void handlePreferences(ApplicationEvent event)
  {
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
      BFrame f = new BFrame();
      Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
      f.setBounds(screenBounds);
      UIUtilities.centerWindow(f);
      new PreferencesWindow(f);
      f.dispose();
    }
    event.setHandled(true);
  }
  

    @Override
    public void handlePrintFile(ApplicationEvent event) {
        event.setHandled(true);
    }

    @Override
    public void handleQuit(ApplicationEvent event) {
        event.setHandled(false);
    }

    @Override
    public void handleReOpenApplication(ApplicationEvent event) {
        event.setHandled(true);
    }
    
}

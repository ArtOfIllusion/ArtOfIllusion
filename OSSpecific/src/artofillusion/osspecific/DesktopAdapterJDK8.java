/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.osspecific;


import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;


/**
 *
 * @author maksim.khramov
 */
public class DesktopAdapterJDK8 extends DesktopAdapter implements AboutHandler, OpenFilesHandler, PreferencesHandler, QuitHandler {

    public DesktopAdapterJDK8() {
        Application app = Application.getApplication();
        app.setEnabledAboutMenu(true);
        app.setAboutHandler(this);
        app.setQuitHandler(this);
        app.setPreferencesHandler(this);
        app.setOpenFileHandler(this);
    }

    @Override
    public void handleAbout(AppEvent.AboutEvent ae) {
        handleAbout();
    }

    @Override
    public void openFiles(AppEvent.OpenFilesEvent event) {        
        handleOpenFiles(event.getFiles());
    }

    @Override
    public void handlePreferences(AppEvent.PreferencesEvent pe) {
        handlePreferences();
    }

    @Override
    public void handleQuitRequestWith(AppEvent.QuitEvent event, QuitResponse response) {
        artofillusion.ArtOfIllusion.quit();
        //need to do this otherwise the user will never be able to quit again
        response.cancelQuit();
    }
    
}

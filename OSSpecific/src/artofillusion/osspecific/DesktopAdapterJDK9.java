/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.osspecific;

import java.awt.Desktop;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;

/**
 *
 * @author maksim.khramov
 */
public class DesktopAdapterJDK9 extends DesktopAdapter implements AboutHandler, OpenFilesHandler, PreferencesHandler, QuitHandler {

    public DesktopAdapterJDK9() {
        Desktop app = Desktop.getDesktop();
        
        app.setAboutHandler(this);
        app.setQuitHandler(this);
        app.setPreferencesHandler(this);
        app.setOpenFileHandler(this);
    }

    @Override
    public void handleAbout(AboutEvent event) {
        handleAbout();
    }

    @Override
    public void openFiles(OpenFilesEvent event) {
        handleOpenFiles(event.getFiles());
    }

    @Override
    public void handlePreferences(PreferencesEvent event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleQuitRequestWith(QuitEvent event, QuitResponse response) {
        artofillusion.ArtOfIllusion.quit();
        //need to do this otherwise the user will never be able to quit again
        response.cancelQuit();
    }
    
}

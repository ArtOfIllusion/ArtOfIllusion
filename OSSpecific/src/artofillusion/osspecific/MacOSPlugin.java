/* Copyright (C) 2002-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.osspecific;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.prefs.*;
import javax.swing.*;

/** This is a plugin to make Art of Illusion behave more like a standard Macintosh 
    application when running under Mac OS X. */

public class MacOSPlugin implements Plugin, InvocationHandler
{
  private boolean usingAppMenu;

  public void processMessage(int message, Object args[])
  {
    if (message == APPLICATION_STARTING)
    {
      String os = ((String) System.getProperties().get("os.name")).toLowerCase();
      if (!os.startsWith("mac os x"))
        return;
      ArtOfIllusion.addWindow(new MacMenuBarWindow());
      UIUtilities.setDefaultFont(new Font("Application", Font.PLAIN, 11));
      UIUtilities.setStandardDialogInsets(3);
      try
      {
        // Use reflection to set up the application menu.
        
        Class appClass = Class.forName("com.apple.eawt.Application");
        Object app = appClass.getMethod("getApplication").invoke(null);
        appClass.getMethod("setEnabledAboutMenu", Boolean.TYPE).invoke(app, Boolean.TRUE);
        appClass.getMethod("setEnabledPreferencesMenu", Boolean.TYPE).invoke(app, Boolean.TRUE);
        Class listenerClass = Class.forName("com.apple.eawt.ApplicationListener");
        Object proxy = Proxy.newProxyInstance(appClass.getClassLoader(), new Class [] {listenerClass}, this);
        appClass.getMethod("addApplicationListener", listenerClass).invoke(app, proxy);
      }
      catch (Exception ex)
      {
        // An error occured trying to set up the application menu, so just stick with the standard
        // Quit and Preferences menu items in the File and Edit menus.
        
        ex.printStackTrace();
      }
      usingAppMenu = true;
    }
    else if (message == SCENE_WINDOW_CREATED)
    {
      final LayoutWindow win = (LayoutWindow) args[0];
      win.addEventLink(SceneChangedEvent.class, new Object() {
        void processEvent()
        {
          updateWindowProperties(win);
        }
      });
      updateWindowProperties(win);
      if (!usingAppMenu)
        return;
      
      // Remove the Quit and Preferences menu items, since we are using the ones in the application
      // menu instead.
      
      removeMenuItem(win, Translate.text("menu.file"), Translate.text("menu.quit"));
      removeMenuItem(win, Translate.text("menu.edit"), Translate.text("menu.preferences"));
    }
    else if (message == SCENE_SAVED)
    {
      LayoutWindow win = (LayoutWindow) args[1];
      updateWindowProperties(win);
      win.getComponent().getRootPane().putClientProperty("Window.documentModified", false);      
    }
  }

  /** Update the Mac OS X specific client properties. */

  private void updateWindowProperties(LayoutWindow win)
  {
    win.getComponent().getRootPane().putClientProperty("Window.documentModified", win.isModified());
    Scene scene = win.getScene();
    if (scene.getName() != null)
    {
      File file = new File(scene.getDirectory(), scene.getName());
      win.getComponent().getRootPane().putClientProperty("Window.documentFile", file);
    }
  }
  
  /** Remove a menu item from a menu in a window.  If it is immediately preceded by a separator,
      also remove that. */
  
  private void removeMenuItem(BFrame frame, String menu, String item)
  {
    BMenuBar bar = frame.getMenuBar();
    for (int i = 0; i < bar.getChildCount(); i++)
    {
      BMenu m = bar.getChild(i);
      if (!m.getText().equals(menu))
        continue;
      for (int j = 0; j < m.getChildCount(); j++)
      {
        MenuWidget w = m.getChild(j);
        if (w instanceof BMenuItem && ((BMenuItem) w).getText().equals(item))
        {
          m.remove((Widget) w);
          if (j > 0 && m.getChild(j-1) instanceof BSeparator)
            m.remove((Widget) m.getChild(j-1));
          return;
        }
      }
      return;
    }
  }
  
  /** Handle ApplicationListener methods. */
  
  public Object invoke(Object proxy, Method method, Object args[])
  {
    boolean handled = true;
    if ("handleAbout".equals(method.getName()))
    {
      TitleWindow win = new TitleWindow();
      win.addEventLink(MouseClickedEvent.class, win, "dispose");
    }
    else if ("handlePreferences".equals(method.getName()))
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
    }
    else if ("handleQuit".equals(method.getName()))
    {
      ArtOfIllusion.quit();
      handled = false;
    }
    else if ("handleOpenFile".equals(method.getName()))
    {
      try
      {
        Method getFilename = args[0].getClass().getMethod("getFilename");
        String path = (String) getFilename.invoke(args[0]);
        ArtOfIllusion.newWindow(new Scene(new File(path), true));
      }
      catch (Exception ex)
      {
        // Nothing we can really do about it...
        
        ex.printStackTrace();
      }
    }
    else
      return null;
    
    // Call setHandled(true) on the event to show we have handled it.
    
    try
    {
      Method setHandled = args[0].getClass().getMethod("setHandled", new Class [] {Boolean.TYPE});
      setHandled.invoke(args[0], handled);
    }
    catch (Exception ex)
    {
      // Nothing we can really do about it...
      
      ex.printStackTrace();
    }
    return null;
  }
  
  /** This is an inner class used to provide a minimal menu bar when all windows are
      closed. */
  
  private class MacMenuBarWindow extends BFrame implements EditingWindow
  {
    public MacMenuBarWindow()
    {
      super();
      ((JFrame) getComponent()).setUndecorated(true);
      setBackground(new Color(0, 0, 0, 0));
      BMenuBar menubar = new BMenuBar();
      setMenuBar(menubar);
      BMenu file = Translate.menu("file");
      menubar.add(file);
      file.add(Translate.menuItem("new", this, "actionPerformed"));
      file.add(Translate.menuItem("open", this, "actionPerformed"));
      final BMenu recentMenu = Translate.menu("openRecent");
      RecentFiles.createMenu(recentMenu);
      file.add(recentMenu);
      Preferences.userNodeForPackage(RecentFiles.class).addPreferenceChangeListener(new PreferenceChangeListener() {
        public void preferenceChange(PreferenceChangeEvent ev)
        {
          RecentFiles.createMenu(recentMenu);
        }
      });
      pack();
      setBounds(new Rectangle(-1000, -1000, 0, 0));
      setVisible(true);
    }

    public ToolPalette getToolPalette()
    {
      return null;
    }

    public void setTool(EditingTool tool)
    {
    }
  
    public void setHelpText(String text)
    {
    }

    public BFrame getFrame()
    {
      return this;
    }

    public void updateImage()
    {
    }

    public void updateMenus()
    {
    }

    public void setUndoRecord(UndoRecord command)
    {
    }

    public void setModified()
    {
    }

    public Scene getScene()
    {
      return null;
    }

    public ViewerCanvas getView()
    {
      return null;
    }

    public ViewerCanvas[] getAllViews()
    {
      return null;
    }

    public boolean confirmClose()
    {
      dispose();
      return true;
    }
    
    private void actionPerformed(CommandEvent ev)
    {
      String command = ev.getActionCommand();
      if (command.equals("new"))
        ArtOfIllusion.newWindow();
      else if (command.equals("open"))
        ArtOfIllusion.openScene(this);
      else if (command.equals("quit"))
        ArtOfIllusion.quit();
    }
  }
}
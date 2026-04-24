/* Copyright (C) 2002-2020 by Peter Eastman
   Changes copyright (C) 2017-2023 by Maksim Khramov

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
import java.awt.desktop.*;
import java.io.*;
import java.util.Locale;
import java.util.prefs.*;

/**
 * This is a plugin to make Art of Illusion behave more like a standard
 * Macintosh application when running under Mac OS X.
 */
public class MacOSPlugin implements Plugin, AboutHandler, QuitHandler, OpenFilesHandler, PreferencesHandler
{

  private static final String OS = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

  private boolean usingAppMenu;

  public void onApplicationStarting()
  {
    if (OS.startsWith("mac"))
    {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      ArtOfIllusion.addWindow(new MacMenuBarWindow());
      UIUtilities.setDefaultFont(new Font("Application", Font.PLAIN, 11));
      UIUtilities.setStandardDialogInsets(3);

      Desktop desktop = Desktop.getDesktop();
      desktop.setAboutHandler(this);
      desktop.setQuitHandler(this);
      desktop.setOpenFileHandler(this);
      desktop.setPreferencesHandler(this);
      usingAppMenu = true;
    }
  }

  public void onSceneWindowCreated(LayoutWindow view)
  {
    view.addEventLink(SceneChangedEvent.class, new Object()
    {
      void processEvent()
      {
        updateWindowProperties(view);
      }
    });
    updateWindowProperties(view);

    if (usingAppMenu)
    {
      // Remove the Quit and Preferences menu items, since we are using the ones in the application
      // menu instead.
      removeMenuItem(view, Translate.text("menu.file"), Translate.text("menu.quit"));
      removeMenuItem(view, Translate.text("menu.edit"), Translate.text("menu.preferences"));
    }
  }

  public void onSceneSaved(File file, LayoutWindow win)
  {
    updateWindowProperties(win);
    win.getComponent().getRootPane().putClientProperty("Window.documentModified", false);
  }

  @Override
  public void processMessage(int message, Object... args)
  {
    switch (message)
    {
      case APPLICATION_STARTING:
        onApplicationStarting();
        break;
      case SCENE_WINDOW_CREATED:
        onSceneWindowCreated((LayoutWindow) args[0]);
        break;
      case SCENE_SAVED:
        onSceneSaved(null, (LayoutWindow) args[1]);
        break;
      default:
        break;
    }
  }

  /** Update the Mac OS X specific client properties. */

  private void updateWindowProperties(LayoutWindow win)
  {
    javax.swing.JRootPane rp = win.getComponent().getRootPane();
    rp.putClientProperty("Window.documentModified", win.isModified());
    Scene scene = win.getScene();
    if (scene.getName() != null)
    {
      File file = new File(scene.getDirectory(), scene.getName());
      rp.putClientProperty("Window.documentFile", file);
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

  @Override
  public void handleAbout(AboutEvent event)
  {
    TitleWindow win = new TitleWindow();
    win.addEventLink(MouseClickedEvent.class, win, "dispose");
  }

  @Override
  public void handleQuitRequestWith(QuitEvent e, QuitResponse response)
  {
    ArtOfIllusion.quit();
    response.cancelQuit();
  }

  @Override
  public void openFiles(OpenFilesEvent event)
  {
    for (File file : event.getFiles())
    {
      try
      {
        ArtOfIllusion.newWindow(new Scene(file, true));
      } catch (IOException ioe)
      {
        // Nothing we can really do about it...
      }
    }
  }

  @Override
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  public void handlePreferences(PreferencesEvent e)
  {
    final Window frontWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    boolean frontIsLayoutWindow = false;
    for (EditingWindow window : ArtOfIllusion.getWindows())
    {
      if (window instanceof LayoutWindow && window.getFrame().getComponent() == frontWindow)
      {
        ((LayoutWindow) window).preferencesCommand();
        frontIsLayoutWindow = true;
        break;
      }
    }
    if (frontIsLayoutWindow)
    {
      return;
    }
    BFrame f = new BFrame();
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    f.setBounds(screenBounds);
    UIUtilities.centerWindow(f);
    new PreferencesWindow(f);
    f.dispose();
  }

  /** This is an inner class used to provide a minimal menu bar when all windows are
      closed. */

  private class MacMenuBarWindow extends BFrame implements EditingWindow
  {
    public MacMenuBarWindow()
    {
      super();
      getComponent().setUndecorated(true);
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
      Preferences.userNodeForPackage(RecentFiles.class).addPreferenceChangeListener((PreferenceChangeEvent ev) ->
      {
        RecentFiles.createMenu(recentMenu);
      });
      pack();
      setBounds(new Rectangle(-1000, -1000, 0, 0));
      setVisible(true);
    }

    @Override
    public ToolPalette getToolPalette()
    {
      return null;
    }

    @Override
    public void setTool(EditingTool tool)
    {
    }

    @Override
    public void setHelpText(String text)
    {
    }

    @Override
    public BFrame getFrame()
    {
      return this;
    }

    @Override
    public void updateImage()
    {
    }

    @Override
    public void updateMenus()
    {
    }

    @Override
    public void setUndoRecord(UndoRecord command)
    {
    }

    @Override
    public void setModified()
    {
    }

    @Override
    public Scene getScene()
    {
      return null;
    }

    @Override
    public ViewerCanvas getView()
    {
      return null;
    }

    @Override
    public ViewerCanvas[] getAllViews()
    {
      return null;
    }

    @Override
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
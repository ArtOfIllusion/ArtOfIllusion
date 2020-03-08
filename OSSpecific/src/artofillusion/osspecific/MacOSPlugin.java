/* Copyright (C) 2002-2011 by Peter Eastman
   Changes copyright (C) 2017-2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.osspecific;

import artofillusion.ArtOfIllusion;
import artofillusion.DefaultPluginImplementation;
import artofillusion.LayoutWindow;
import artofillusion.RecentFiles;
import artofillusion.Scene;
import artofillusion.SceneChangedEvent;
import artofillusion.UndoRecord;
import artofillusion.ViewerCanvas;
import artofillusion.ui.EditingTool;
import artofillusion.ui.EditingWindow;
import artofillusion.ui.ToolPalette;
import artofillusion.ui.Translate;
import artofillusion.ui.UIUtilities;
import buoy.event.CommandEvent;
import buoy.widget.BFrame;
import buoy.widget.BMenu;
import buoy.widget.BMenuBar;
import buoy.widget.BMenuItem;
import buoy.widget.BSeparator;
import buoy.widget.MenuWidget;
import buoy.widget.Widget;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/** This is a plugin to make Art of Illusion behave more like a standard Macintosh
    application when running under Mac OS X. */
public class MacOSPlugin extends DefaultPluginImplementation {
    
    private static final boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

    @Override
    protected void onApplicationStarting() {
        if(!isMac) return;
        ArtOfIllusion.addWindow(new MacMenuBarWindow());
        UIUtilities.setDefaultFont(new Font("Application", Font.PLAIN, 11));
        UIUtilities.setStandardDialogInsets(3);
        try {
            Class.forName("artofillusion.osspecific.DesktopAdapterJDK8").getConstructor().newInstance();
        } catch (NoClassDefFoundError | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex8) {
            System.out.println("Unable to instantiate JDK8 adapter. Try new JDK9 one");
            try {
                Class.forName("artofillusion.osspecific.DesktopAdapterJDK9").getConstructor().newInstance();
            } catch (NoClassDefFoundError | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex9) {
                System.out.println("Unable to instantiate JDK9 adapter.");
            }
        }

    }

    @Override
    protected void onSceneSaved(File file, LayoutWindow view)
    {
        if(isMac)
        {
            updateWindowProperties(view);
            view.getComponent().getRootPane().putClientProperty("Window.documentModified", false);
        }
    }

    @Override
    protected void onSceneWindowCreated(final LayoutWindow view) {
        if(isMac)
        {
            view.addEventLink(SceneChangedEvent.class, new Object() {
                void processEvent() {
                    updateWindowProperties(view);
                }
            });
            updateWindowProperties(view);
            view.getComponent().getRootPane().putClientProperty("Window.documentModified", false);
            // Remove the Quit and Preferences menu items, since we are using the ones in the application
            // menu instead.

            removeMenuItem(view, Translate.text("menu.file"), Translate.text("menu.quit"));
            removeMenuItem(view, Translate.text("menu.edit"), Translate.text("menu.preferences"));
        }
    }

  /** Update the Mac OS X specific client properties. */

  private static void updateWindowProperties(LayoutWindow view)
  {
    view.getComponent().getRootPane().putClientProperty("Window.documentModified", view.isModified());
    Scene scene = view.getScene();
    if(null == scene.getName()) return;

    File file = new File(scene.getDirectory(), scene.getName());
    view.getComponent().getRootPane().putClientProperty("Window.documentFile", file);

  }

  /** Remove a menu item from a menu in a window.  If it is immediately preceded by a separator,
      also remove that. */

  private static void removeMenuItem(BFrame frame, String menu, String item)
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

  /** This is an inner class used to provide a minimal menu bar when all windows are
      closed. */

  private class MacMenuBarWindow extends BFrame implements EditingWindow
  {
    public MacMenuBarWindow()
    {
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
      Preferences.userNodeForPackage(RecentFiles.class).addPreferenceChangeListener(new PreferenceChangeListener() {
        @Override
        public void preferenceChange(PreferenceChangeEvent ev)
        {
          RecentFiles.createMenu(recentMenu);
        }
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

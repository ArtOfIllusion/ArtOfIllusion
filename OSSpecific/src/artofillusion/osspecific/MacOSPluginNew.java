/* Copyright (C) 2002-2011 by Peter Eastman
   Changes copyright (C) 2017-2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */
package artofillusion.osspecific;

import artofillusion.DefaultPluginImplementation;
import artofillusion.LayoutWindow;
import artofillusion.Scene;
import artofillusion.SceneChangedEvent;
import artofillusion.ui.Translate;
import buoy.widget.BFrame;
import buoy.widget.BMenu;
import buoy.widget.BMenuBar;
import buoy.widget.BMenuItem;
import buoy.widget.BSeparator;
import buoy.widget.MenuWidget;
import buoy.widget.Widget;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author maksim.khramov
 */
public class MacOSPluginNew extends DefaultPluginImplementation {

    private static final boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    
    @Override
    protected void onApplicationStarting() {
        if(!isMac) return;
        try {
            Class.forName("artofillusion.osspecific.DesktopAdapterJDK8").getConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex8) {
            System.out.println("Unable to instantiate JDK8 adapter. Try new JDK9 one");
            try {
                Class.forName("artofillusion.osspecific.DesktopAdapterJDK9").getConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex9) {
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
    protected void onSceneWindowCreated(LayoutWindow view) {
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
    
}

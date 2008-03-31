/* Copyright (C) 2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;
import java.util.prefs.*;

/** This class maintains a list of recently accessed scene files, and generates menus allowing
    them to be opened easily. */

public class RecentFiles
{  
  private static final int MAX_RECENT = 10;
  
  /** Given a BMenu, fill it in with a list of items for recent files. */
  
  public static void createMenu(BMenu menu)
  {
    menu.removeAll();
    Preferences pref = Preferences.userNodeForPackage(RecentFiles.class);
    String recent[] = pref.get("recentFiles", "").split(File.pathSeparator);
    for (int i = 0; i < recent.length; i++)
    {
      final File file = new File(recent[i]);
      BMenuItem item = new BMenuItem(file.getName());
      menu.add(item);
      item.addEventLink(CommandEvent.class, new Object() {
        void processEvent(CommandEvent ev)
        {
          ArtOfIllusion.openScene(file, UIUtilities.findFrame(ev.getWidget()));
        }
      });
    }
  }
  
  /** Add a File to the list of recent files. */
  
  public static void addRecentFile(File file)
  {
    // Find the new list of recent files.
    
    String newPath = file.getAbsolutePath();
    Preferences pref = Preferences.userNodeForPackage(RecentFiles.class);
    String recent[] = pref.get("recentFiles", "").split(File.pathSeparator);
    ArrayList newFiles = new ArrayList();
    newFiles.add(newPath);
    for (int i = 0; i < recent.length && newFiles.size() < MAX_RECENT; i++)
      if (!newPath.equals(recent[i]))
        newFiles.add(recent[i]);
    StringBuffer fileList = new StringBuffer();
    for (int i = 0; i < newFiles.size(); i++)
    {
      if (i > 0)
        fileList.append(File.pathSeparator);
      fileList.append(newFiles.get(i));
    }
    pref.put("recentFiles", fileList.toString());
    
    // Rebuild the menus in all open windows.
    
    EditingWindow win[] = ArtOfIllusion.getWindows();
    for (int i = 0; i < win.length; i++)
      if (win[i] instanceof LayoutWindow)
        createMenu(((LayoutWindow) win[i]).recentFilesMenu);
  }
}

/* Copyright (C) 2002-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import java.io.*;
import java.util.*;
import java.util.List;

/** This class keeps track of program-wide user preferences. */

public class ApplicationPreferences
{
  private Properties properties;
  private int defaultDisplayMode, undoLevels;
  private double interactiveTol;
  private boolean keepBackupFiles, useOpenGL, useCompoundMeshTool, reverseZooming;
  private Renderer objectPreviewRenderer, texturePreviewRenderer, defaultRenderer;

  /**
   * Create a new ApplicationPreferences object, loading the preferences from a
   * file in the default location.
   */

  public ApplicationPreferences()
  {
    File f = new File(getPreferencesDirectory(), "aoiprefs");
    if (!f.exists())
    {
      // See if it exists in the old location.

      File f2 = new File(System.getProperty("user.home"), ".aoiprefs");
      if (f2.exists())
        f2.renameTo(f);
    }
    initDefaultPreferences();
    if (!f.exists())
      {
        properties = new Properties();
        Translate.setLocale(Locale.getDefault());
        return;
      }
    try
      {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        loadPreferences(in);
        in.close();
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
      }
  }

  /**
   * Create a new ApplicationPreferences object, loading the preferences from an InputStream.
   */

  public ApplicationPreferences(InputStream in)
  {
    initDefaultPreferences();
    try
    {
      loadPreferences(in);
      in.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

  /** Load the preferences from an InputStream. */

  private void loadPreferences(InputStream in) throws IOException
  {
    properties = new Properties();
    properties.load(in);
    parsePreferences();
  }

  /** Save any changed preferences to disk. */

  public void savePreferences()
  {
    // Copy over preferences that are stored in other classes.

    properties.put("theme", ThemeManager.getSelectedTheme().resource.getId());
    ThemeManager.ColorSet colorSets[] = ThemeManager.getSelectedTheme().getColorSets();
    for (int i = 0; i < colorSets.length; i++)
      if (colorSets[i] == ThemeManager.getSelectedColorSet())
        properties.put("themeColorSet", Integer.toString(i));

    // Write the preferences to a file.

    File f = new File(getPreferencesDirectory(), "aoiprefs");
    try
      {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        properties.store(out, "Art of Illusion Preferences File");
        out.close();
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
      }
  }

  /** Get the directory in which preferences files are saved. */

  public static File getPreferencesDirectory()
  {
    File dir = new File(System.getProperty("user.home"), ".artofillusion");
    if (!dir.exists())
      dir.mkdirs();
    return dir;
  }

  /** Initialize internal variables to reasonable defaults. */

  private void initDefaultPreferences()
  {
    List<Renderer> renderers = PluginRegistry.getPlugins(Renderer.class);
    if (renderers.size() > 0)
      objectPreviewRenderer = texturePreviewRenderer = defaultRenderer = getNamedRenderer("Raytracer");
    defaultDisplayMode = ViewerCanvas.RENDER_SMOOTH;
    interactiveTol = 0.05;
    undoLevels = 6;
    useOpenGL = true;
    keepBackupFiles = false;
    useCompoundMeshTool = false;
    reverseZooming = false;
  }

  /** Parse the properties loaded from the preferences file. */

  private void parsePreferences()
  {
    objectPreviewRenderer = getNamedRenderer(properties.getProperty("objectPreviewRenderer"));
    texturePreviewRenderer = getNamedRenderer(properties.getProperty("texturePreviewRenderer"));
    defaultRenderer = getNamedRenderer(properties.getProperty("defaultRenderer"));
    defaultDisplayMode = parseIntProperty("defaultDisplayMode", defaultDisplayMode);
    interactiveTol = parseDoubleProperty("interactiveSurfaceError", interactiveTol);
    undoLevels = parseIntProperty("undoLevels", undoLevels);
    useOpenGL = parseBooleanProperty("useOpenGL", useOpenGL);
    keepBackupFiles = parseBooleanProperty("keepBackupFiles", keepBackupFiles);
    useCompoundMeshTool = parseBooleanProperty("useCompoundMeshTool", useCompoundMeshTool);
    reverseZooming = parseBooleanProperty("reverseZooming", reverseZooming);
    Translate.setLocale(parseLocaleProperty("language"));
    if (properties.getProperty("theme") == null)
    {
      ThemeManager.setSelectedTheme(ThemeManager.getDefaultTheme());
      ThemeManager.setSelectedColorSet(ThemeManager.getSelectedTheme().getColorSets()[parseIntProperty("colorScheme", 0)]);
    }
    else
    {
      String themeId = properties.getProperty("theme");
      List themes = ThemeManager.getThemes();
      for (int i = 0; i < themes.size(); i++)
      {
        ThemeManager.ThemeInfo theme = (ThemeManager.ThemeInfo) themes.get(i);
        if (theme.resource.getId().equals(themeId))
        {
          ThemeManager.setSelectedTheme(theme);
          int colorSetIndex = parseIntProperty("themeColorSet", 0);
          ThemeManager.ColorSet colorSets[] = theme.getColorSets();
          if (colorSetIndex > -1 && colorSetIndex < colorSets.length)
            ThemeManager.setSelectedColorSet(colorSets[colorSetIndex]);
          break;
        }
      }
    }
  }

  /** Parse an integer valued property. */

  private int parseIntProperty(String name, int defaultVal)
  {
    try
      {
        return Integer.parseInt(properties.getProperty(name));
      }
    catch (Exception ex)
      {
        return defaultVal;
      }
  }

  /** Parse a double valued property. */

  private double parseDoubleProperty(String name, double defaultVal)
  {
    try
      {
        return Double.valueOf(properties.getProperty(name));
      }
    catch (Exception ex)
      {
        return defaultVal;
      }
  }

  /** Parse a boolea valued property. */

  private boolean parseBooleanProperty(String name, boolean defaultVal)
  {
    String prop = properties.getProperty(name);
    if (prop == null)
      return defaultVal;
    return Boolean.valueOf(prop);
  }

  /** Parse a property specifying a locale. */

  private Locale parseLocaleProperty(String name)
  {
    try
      {
        String desc = properties.getProperty(name);
        String language = desc.substring(0, 2);
        String country = desc.substring(3);
        return new Locale(language, country);
      }
    catch (Exception ex)
      {
        return Locale.getDefault();
      }
  }

  /** Look up a renderer by name. */

  private Renderer getNamedRenderer(String name)
  {
    List<Renderer> renderers = PluginRegistry.getPlugins(Renderer.class);
    if (renderers.size() == 0)
      return null;
    for (Renderer r : renderers)
      if (r.getName().equals(name))
        return r;
    return renderers.get(renderers.size()-1);
  }

  /** Get the default renderer. */

  public final Renderer getDefaultRenderer()
  {
    return defaultRenderer;
  }

  /** Set the default renderer. */

  public final void setDefaultRenderer(Renderer rend)
  {
    defaultRenderer = rend;
    properties.put("defaultRenderer", rend.getName());
  }

  /** Get the object preview renderer. */

  public final Renderer getObjectPreviewRenderer()
  {
    return objectPreviewRenderer;
  }

  /** Set the object preview renderer. */

  public final void setObjectPreviewRenderer(Renderer rend)
  {
    objectPreviewRenderer = rend;
    properties.put("objectPreviewRenderer", rend.getName());
  }

  /** Get the texture preview renderer. */

  public final Renderer getTexturePreviewRenderer()
  {
    return texturePreviewRenderer;
  }

  /** Set the texture preview renderer. */

  public final void setTexturePreviewRenderer(Renderer rend)
  {
    texturePreviewRenderer = rend;
    properties.put("texturePreviewRenderer", rend.getName());
  }

  /** Get the default display mode. */

  public final int getDefaultDisplayMode()
  {
    return defaultDisplayMode;
  }

  /** Set the default display mode. */

  public final void setDefaultDisplayMode(int mode)
  {
    defaultDisplayMode = mode;
    properties.put("defaultDisplayMode", Integer.toString(mode));
  }

  /** Get the interactive surface error. */

  public final double getInteractiveSurfaceError()
  {
    return interactiveTol;
  }

  /** Set the interactive surface error. */

  public final void setInteractiveSurfaceError(double tol)
  {
    boolean changed = (interactiveTol != tol);

    interactiveTol = tol;
    properties.put("interactiveSurfaceError", Double.toString(tol));
    if (changed)
      {
        // Clear the cached meshes for objects in all windows.

        EditingWindow windows[] = ArtOfIllusion.getWindows();
        for (EditingWindow w : windows)
          {
            Scene sc = w.getScene();
            if (sc == null)
              continue;
            for (int j = 0; j < sc.getNumObjects(); j++)
              {
                ObjectInfo info = sc.getObject(j);
                Vec3 size = info.getBounds().getSize();
                info.getObject().setSize(size.x, size.y, size.z);
                info.clearCachedMeshes();
              }
            w.updateImage();
          }
      }
  }

  /** Get the locale for displaying text. */

  public final Locale getLocale()
  {
    return Translate.getLocale();
  }

  /** Set the locale for displaying text. */

  public final void setLocale(Locale locale)
  {
    Translate.setLocale(locale);
    properties.put("language", locale.getLanguage()+'_'+locale.getCountry());
  }

  /** Get the number of levels of Undo to support. */

  public final int getUndoLevels()
  {
    return undoLevels;
  }

  /** Set the number of levels of Undo to support. */

  public final void setUndoLevels(int levels)
  {
    undoLevels = levels;
    properties.put("undoLevels", Integer.toString(levels));
  }

  /** Get whether to use OpenGL for interactive rendering. */

  public final boolean getUseOpenGL()
  {
    return useOpenGL;
  }

  /** Set whether to use OpenGL for interactive rendering. */

  public final void setUseOpenGL(boolean use)
  {
    useOpenGL = use;
    properties.put("useOpenGL", Boolean.toString(use));
  }

  /** Get whether to keep backup files. */

  public final boolean getKeepBackupFiles()
  {
    return keepBackupFiles;
  }

  /** Set whether to keep backup files. */

  public final void setKeepBackupFiles(boolean keep)
  {
    keepBackupFiles = keep;
    properties.put("keepBackupFiles", Boolean.toString(keep));
  }

  /** Get whether to use the compound move/scale/rotate tool as the default for mesh editing. */

  public final boolean getUseCompoundMeshTool()
  {
    return useCompoundMeshTool;
  }

  /** Set whether to use the compound move/scale/rotate tool as the default for mesh editing. */

  public final void setUseCompoundMeshTool(boolean use)
  {
    useCompoundMeshTool = use;
    properties.put("useCompoundMeshTool", Boolean.toString(use));
  }

  /** Get whether to reverse the direction of scroll wheel zooming. */

  public final boolean getReverseZooming()
  {
    return reverseZooming;
  }

  /** Set whether to reverse the direction of scroll wheel zooming. */

  public final void setReverseZooming(boolean reverse)
  {
    reverseZooming = reverse;
    properties.put("reverseZooming", Boolean.toString(reverse));
  }
}
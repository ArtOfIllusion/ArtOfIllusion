/* Copyright (C) 2002-2006 by Peter Eastman

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
import java.awt.*;

/** This class keeps track of program-wide user preferences. */

public class ApplicationPreferences
{
  private Properties properties;
  private int defaultDisplayMode, undoLevels, colorScheme;
  private double interactiveTol;
  private boolean keepBackupFiles, useOpenGL, useCompoundMeshTool;
  private Renderer objectPreviewRenderer, texturePreviewRenderer, defaultRenderer;

  private static final Color COLORS[][] = new Color [][] {
    {Color.WHITE, Color.BLACK, Color.RED, Color.MAGENTA, Color.GREEN, Color.GRAY, new Color(0.8f, 0.8f, 1.0f), new Color(0.8f, 0.8f, 1.0f)},
    {new Color(40, 40, 40), Color.WHITE, Color.RED, Color.MAGENTA, Color.GREEN, Color.GRAY, new Color(0.8f, 0.8f, 1.0f), new Color(0.15f, 0.15f, 0.28f)}
  };

  public ApplicationPreferences()
  {
    loadPreferences();
  }

  /** Load the preferences from disk. */

  public void loadPreferences()
  {
    properties = new Properties();
    initDefaultPreferences();
    File f = new File(getPreferencesDirectory(), "aoiprefs");
    if (!f.exists())
    {
      // See if it exists in the old location.

      File f2 = new File(System.getProperty("user.home"), ".aoiprefs");
      if (f2.exists())
        f2.renameTo(f);
    }
    if (!f.exists())
      {
        Translate.setLocale(Locale.getDefault());
        return;
      }
    try
      {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        properties.load(in);
        in.close();
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
      }
    parsePreferences();
  }

  /** Save any changed preferences to disk. */

  public void savePreferences()
  {
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
    Renderer renderers[] = ModellingApp.getRenderers();
    if (renderers.length > 0)
      objectPreviewRenderer = texturePreviewRenderer = defaultRenderer = getNamedRenderer("Raytracer");
    defaultDisplayMode = ViewerCanvas.RENDER_SMOOTH;
    interactiveTol = 0.05;
    undoLevels = 6;
    useOpenGL = true;
    keepBackupFiles = false;
    useCompoundMeshTool = false;
    colorScheme = 0;
    applyColorScheme();
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
    Translate.setLocale(parseLocaleProperty("language"));
    colorScheme = parseIntProperty("colorScheme", colorScheme);
    applyColorScheme();
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
        return new Double(properties.getProperty(name)).doubleValue();
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
    return Boolean.valueOf(prop).booleanValue();
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
    Renderer renderers[] = ModellingApp.getRenderers();

    if (renderers.length == 0)
      return null;
    for (int i = 0; i < renderers.length; i++)
      if (renderers[i].getName().equals(name))
        return renderers[i];
    return renderers[renderers.length-1];
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

        EditingWindow windows[] = ModellingApp.getWindows();
        for (int i = 0; i < windows.length; i++)
          {
            Scene sc = windows[i].getScene();
            if (sc == null)
              continue;
            for (int j = 0; j < sc.getNumObjects(); j++)
              {
                ObjectInfo info = sc.getObject(j);
                Vec3 size = info.getBounds().getSize();
                info.object.setSize(size.x, size.y, size.z);
                info.clearCachedMeshes();
              }
            windows[i].updateImage();
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

  /** Get the selected color scheme. */

  public final int getColorScheme()
  {
    return colorScheme;
  }

  /** Set the selected color scheme. */

  public final void setColorScheme(int scheme)
  {
    colorScheme = scheme;
    properties.put("colorScheme", Integer.toString(scheme));
    applyColorScheme();
  }

  /** Apply the selected color scheme. */

  private void applyColorScheme()
  {
    Color schemeColors[] = COLORS[colorScheme];
    ViewerCanvas.backgroundColor = schemeColors[0];
    ViewerCanvas.lineColor = schemeColors[1];
    ViewerCanvas.handleColor = schemeColors[2];
    ViewerCanvas.highlightColor = schemeColors[3];
    ViewerCanvas.specialHighlightColor = schemeColors[4];
    ViewerCanvas.disabledColor = schemeColors[5];
    ViewerCanvas.surfaceColor = schemeColors[6];
    ViewerCanvas.surfaceRGBColor = new RGBColor(schemeColors[6].getRed()/255.0, schemeColors[6].getGreen()/255.0, schemeColors[6].getBlue()/255.0);
    ViewerCanvas.transparentColor = new RGBColor(schemeColors[7].getRed()/255.0, schemeColors[7].getGreen()/255.0, schemeColors[7].getBlue()/255.0);
  }
}
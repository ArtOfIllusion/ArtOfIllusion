/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.image.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.procedural.*;
import artofillusion.script.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import artofillusion.keystroke.*;
import buoy.widget.*;
import buoy.xml.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
import javax.swing.*;

/** This is the main class for Art of Illusion.  All of its methods and variables are static,
    so no instance of this class ever gets created.  It starts up the application, and 
    maintains global variables. */

public class ModellingApp
{
  public static final String MAJOR_VERSION = "2.4";
  public static final String VERSION = MAJOR_VERSION+".1";
  public static final double DIST_TO_SCREEN = 20.0;
  public static final Color APP_BACKGROUND_COLOR = new Color(228, 228, 243);
  public static final String APP_DIRECTORY, PLUGIN_DIRECTORY;
  public static final String TOOL_SCRIPT_DIRECTORY, OBJECT_SCRIPT_DIRECTORY, STARTUP_SCRIPT_DIRECTORY;
  public static final ImageIcon APP_ICON;
  public static Font defaultFont;
  public static String currentDirectory;
  public static int standardDialogInsets = 0;
  private static ApplicationPreferences preferences;
  private static ObjectInfo clipboardObject[];
  private static Texture clipboardTexture[];
  private static Material clipboardMaterial[];
  private static ImageMap clipboardImage[];
  private static Vector windows = new Vector();
  private static URLClassLoader pluginLoader[];
  private static Vector renderers = new Vector();
  private static Vector translators = new Vector();
  private static Vector modellingTools = new Vector();
  private static Vector textures = new Vector();
  private static Vector materials = new Vector();
  private static Vector textureMappings = new Vector();
  private static Vector materialMappings = new Vector();
  private static Vector plugins = new Vector();
  private static Vector filters = new Vector();
  private static Vector modules = new Vector();
  private static Hashtable classTranslations = new Hashtable();
  private static int numNewWindows = 0;

  static
  {
    // A clever trick for getting the location of the jar file, which David Smiley
    // posted to the Apple java-dev mailing list on April 14, 2002.  It works on
    // most, but not all, platforms, so in case of a problem we fall back to using
    // user.dir.

    String dir = System.getProperty("user.dir");
    try
      {
        URL url = ModellingApp.class.getResource("/artofillusion/ModellingApp.class");
        if (url.toString().startsWith("jar:"))
          {
            String furl = url.getFile();
            furl = furl.substring(0, furl.indexOf('!'));
            dir = new File(new URL(furl).getFile()).getParent();
            if (!new File(dir).exists())
              dir = System.getProperty("user.dir");
          }
      }
      catch (Exception ex)
      {
      }
    
    // Set up the standard directories.
    
    APP_DIRECTORY = dir;
    PLUGIN_DIRECTORY = new File(APP_DIRECTORY, "Plugins").getAbsolutePath();
    File scripts = new File(APP_DIRECTORY, "Scripts");
    TOOL_SCRIPT_DIRECTORY = new File(scripts, "Tools").getAbsolutePath();
    OBJECT_SCRIPT_DIRECTORY = new File(scripts, "Objects").getAbsolutePath();
    STARTUP_SCRIPT_DIRECTORY = new File(scripts, "Startup").getAbsolutePath();
    
    // Load the application's icon.

    ImageIcon icon = new IconResource("artofillusion/Icons/appIcon.png");
    APP_ICON = (icon.getIconWidth() == -1 ? null : icon);

    // Build a table of classes which have moved.
    
    classTranslations.put("artofillusion.tools.CSGObject", "artofillusion.object.CSGObject");
    classTranslations.put("artofillusion.Cube", "artofillusion.object.Cube");
    classTranslations.put("artofillusion.Curve", "artofillusion.object.Curve");
    classTranslations.put("artofillusion.Cylinder", "artofillusion.object.Cylinder");
    classTranslations.put("artofillusion.DirectionalLight", "artofillusion.object.DirectionalLight");
    classTranslations.put("artofillusion.NullObject", "artofillusion.object.NullObject");
    classTranslations.put("artofillusion.PointLight", "artofillusion.object.PointLight");
    classTranslations.put("artofillusion.SceneCamera", "artofillusion.object.SceneCamera");
    classTranslations.put("artofillusion.Sphere", "artofillusion.object.Sphere");
    classTranslations.put("artofillusion.SplineMesh", "artofillusion.object.SplineMesh");
    classTranslations.put("artofillusion.SpotLight", "artofillusion.object.SpotLight");
    classTranslations.put("artofillusion.TriangleMesh", "artofillusion.object.TriangleMesh");
    classTranslations.put("artofillusion.Tube", "artofillusion.object.Tube");
    classTranslations.put("artofillusion.CylindricalMapping", "artofillusion.texture.CylindricalMapping");
    classTranslations.put("artofillusion.ImageMapTexture", "artofillusion.texture.ImageMapTexture");
    classTranslations.put("artofillusion.LayeredMapping", "artofillusion.texture.LayeredMapping");
    classTranslations.put("artofillusion.LayeredTexture", "artofillusion.texture.LayeredTexture");
    classTranslations.put("artofillusion.LinearMapping3D", "artofillusion.texture.LinearMapping3D");
    classTranslations.put("artofillusion.procedural.ProceduralTexture2D", "artofillusion.texture.ProceduralTexture2D");
    classTranslations.put("artofillusion.procedural.ProceduralTexture3D", "artofillusion.texture.ProceduralTexture3D");
    classTranslations.put("artofillusion.ProjectionMapping", "artofillusion.texture.ProjectionMapping");
    classTranslations.put("artofillusion.SphericalMapping", "artofillusion.texture.SphericalMapping");
    classTranslations.put("artofillusion.UniformMapping", "artofillusion.texture.UniformMapping");
    classTranslations.put("artofillusion.UniformTexture", "artofillusion.texture.UniformTexture");
    classTranslations.put("artofillusion.LinearMaterialMapping", "artofillusion.material.LinearMaterialMapping");
    classTranslations.put("artofillusion.procedural.ProceduralMaterial3D", "artofillusion.material.ProceduralMaterial3D");
    classTranslations.put("artofillusion.UniformMaterial", "artofillusion.material.UniformMaterial");
    classTranslations.put("artofillusion.UniformMaterialMapping", "artofillusion.material.UniformMaterialMapping");
    classTranslations.put("artofillusion.tools.tapDesigner.TapDesignerObjectCollection", "artofillusion.tapDesigner.TapDesignerObjectCollection");
    classTranslations.put("artofillusion.tools.tapDesigner.TapTube", "artofillusion.tapDesigner.TapTube");
    classTranslations.put("artofillusion.tools.tapDesigner.TapSplineMesh", "artofillusion.tapDesigner.TapSplineMesh");
    classTranslations.put("artofillusion.tools.tapDesigner.TapObject", "artofillusion.tapDesigner.TapObject");
    classTranslations.put("artofillusion.tools.tapDesigner.TapLeaf", "artofillusion.tapDesigner.TapLeaf");
  }

  public static void main(String args[])
  {
    Translate.setLocale(Locale.getDefault());
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception ex)
    {
    }
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
    try
    {
      // Due to the strange way PopupFactory is implemented, we need to use reflection to make sure
      // we *really* get heavyweight popups from the very start.

      Class popup = PopupFactory.class;
      Field heavyweight = popup.getDeclaredField("HEAVY_WEIGHT_POPUP");
      Method setPopupType = popup.getDeclaredMethod("setPopupType", new Class [] {Integer.TYPE});
      heavyweight.setAccessible(true);
      setPopupType.setAccessible(true);
      setPopupType.invoke(PopupFactory.getSharedInstance(), new Object [] {heavyweight.get(null)});
    }
    catch (Exception ex)
    {
      // Don't worry about it.
    }
    TitleWindow title = new TitleWindow();
    try
    {
      textures.addElement(Class.forName("artofillusion.texture.UniformTexture"));
      textures.addElement(Class.forName("artofillusion.texture.ImageMapTexture"));
      textures.addElement(Class.forName("artofillusion.texture.ProceduralTexture2D"));
      textures.addElement(Class.forName("artofillusion.texture.ProceduralTexture3D"));
      materials.addElement(Class.forName("artofillusion.material.UniformMaterial"));
      materials.addElement(Class.forName("artofillusion.material.ProceduralMaterial3D"));
      textureMappings.addElement(Class.forName("artofillusion.texture.UniformMapping"));
      textureMappings.addElement(Class.forName("artofillusion.texture.ProjectionMapping"));
      textureMappings.addElement(Class.forName("artofillusion.texture.CylindricalMapping"));
      textureMappings.addElement(Class.forName("artofillusion.texture.SphericalMapping"));
      textureMappings.addElement(Class.forName("artofillusion.texture.UVMapping"));
      textureMappings.addElement(Class.forName("artofillusion.texture.LinearMapping3D"));
      materialMappings.addElement(Class.forName("artofillusion.material.LinearMaterialMapping"));
      filters.addElement(Class.forName("artofillusion.image.filter.BrightnessFilter"));
      filters.addElement(Class.forName("artofillusion.image.filter.SaturationFilter"));
      filters.addElement(Class.forName("artofillusion.image.filter.ExposureFilter"));
      filters.addElement(Class.forName("artofillusion.image.filter.TintFilter"));
      filters.addElement(Class.forName("artofillusion.image.filter.BlurFilter"));
      filters.addElement(Class.forName("artofillusion.image.filter.GlowFilter"));
      filters.addElement(Class.forName("artofillusion.image.filter.OutlineFilter"));
    }
    catch (ClassNotFoundException ex)
    {
    }
    if (pluginLoader == null)
      scanPlugins();
    preferences = new ApplicationPreferences();
    KeystrokeManager.loadRecords();
    for (int i = 0; i < plugins.size(); i++)
    {
      try
      {
        ((Plugin) plugins.elementAt(i)).processMessage(Plugin.APPLICATION_STARTING, new Object [0]);
      }
      catch (Throwable tx)
      {
        String name = plugins.elementAt(i).getClass().getName();
        name = name.substring(name.lastIndexOf('.')+1);
        new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginInitError", name)), BStandardDialog.ERROR).showMessageDialog(null);
      }
    }
    for (int i = 0; i < args.length; i++)
    {
      try
      {
        newWindow(new Scene(new File(args[i]), true));
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    runStartupScripts();
    if (numNewWindows == 0)
      newWindow();
    title.dispose();
  }

  /** Get the application preferences object. */
  
  public static ApplicationPreferences getPreferences()
  {
    return preferences;
  }

  /** Get a list of all available Renderers. */

  public static Renderer[] getRenderers()
  {
    Renderer r[] = new Renderer [renderers.size()];
    renderers.copyInto(r);
    return r;
  }
  
  /** Get a list of all installed Plugins. */

  public static Plugin[] getPlugins()
  {
    Plugin p[] = new Plugin [plugins.size()];
    plugins.copyInto(p);
    return p;
  }

  /** Get a list of all available Translators. */

  public static Translator[] getTranslators()
  {
    Translator t[] = new Translator [translators.size()];
    translators.copyInto(t);
    return t;
  }
  
  /** Get a list of all available ModellingTools. */
  
  public static ModellingTool[] getModellingTools()
  {
    ModellingTool t[] = new ModellingTool [modellingTools.size()];
    modellingTools.copyInto(t);
    return t;
  }
  
  /** Get a list of all available Texture classes. */

  public static Class[] getTextureTypes()
  {
    Class t[] = new Class [textures.size()];
    textures.copyInto(t);
    return t;
  }
  
  /** Get a list of all available Material classes. */

  public static Class[] getMaterialTypes()
  {
    Class m[] = new Class [materials.size()];
    materials.copyInto(m);
    return m;
  }
  
  /** Get a list of all available TextureMapping classes. */

  public static Class[] getTextureMappings()
  {
    Class t[] = new Class [textureMappings.size()];
    textureMappings.copyInto(t);
    return t;
  }
  
  /** Get a list of all available MaterialMapping classes. */

  public static Class[] getMaterialMappings()
  {
    Class m[] = new Class [materialMappings.size()];
    materialMappings.copyInto(m);
    return m;
  }
  
  /** Get a list of all available ImageFilter classes. */

  public static Class[] getImageFilters()
  {
    Class f[] = new Class [filters.size()];
    filters.copyInto(f);
    return f;
  }

  /** Get a list of all plugin-defined procedural Module classes. */

  public static Class[] getModules()
  {
    Class m[] = new Class [modules.size()];
    modules.copyInto(m);
    return m;
  }
  
  /** Add a new Renderer the list of available ones. */
  
  public static void registerRenderer(Renderer o)
  {
    renderers.addElement(o);
  }
  
  /** Add a new Translator the list of available ones. */
  
  public static void registerTranslator(Translator o)
  {
    translators.addElement(o);
  }
  
  /** Add a new ModellingTool the list of available ones. */
  
  public static void registerModellingTool(ModellingTool o)
  {
    modellingTools.addElement(o);
  }
  
  /** Add a new Texture the list of available ones. */
  
  public static void registerTexture(Texture o)
  {
    textures.addElement(o.getClass());
  }
  
  /** Add a new Material the list of available ones. */
  
  public static void registerMaterial(Material o)
  {
    materials.addElement(o.getClass());
  }
  
  /** Add a new TextureMapping the list of available ones. */
  
  public static void registerTextureMapping(TextureMapping o)
  {
    textureMappings.addElement(o.getClass());
  }
  
  /** Add a new MaterialMapping the list of available ones. */
  
  public static void registerMaterialMapping(MaterialMapping o)
  {
    materialMappings.addElement(o.getClass());
  }
  
  /** Add a new Plugin the list of available ones. */
  
  public static void registerPlugin(Plugin o)
  {
    plugins.addElement(o);
  }
  
  /** Add a new ImageFilter to the list of available ones. */
  
  public static void registerImageFilter(artofillusion.image.filter.ImageFilter o)
  {
    filters.addElement(o.getClass());
  }
  
  /** Add a new Module to the list of available ones. */
  
  public static void registerModule(Module o)
  {
    modules.addElement(o.getClass());
  }
  
  /** Create a new Scene, and display it in a window. */
  
  public static void newWindow()
  {
    Scene theScene = new Scene();
    CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, ModellingApp.DIST_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    ObjectInfo info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    theScene.addObject(info, null);
    info = new ObjectInfo(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 0.8f), coords.duplicate(), "Light 1");
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    theScene.addObject(info, null);
    newWindow(theScene);
  }
  
  /** Create a new window for editing the specified scene. */
  
  public static void newWindow(final Scene theScene)
  {
    // New windows should always be created on the event thread.
    
    numNewWindows++;
    SwingUtilities.invokeLater(new Runnable() {
      public void run()
      {
        LayoutWindow fr = new LayoutWindow(theScene);
        windows.addElement(fr);
        for (int i = 0; i < plugins.size(); i++)
        {
          try
          {
            ((Plugin) plugins.elementAt(i)).processMessage(Plugin.SCENE_WINDOW_CREATED, new Object [] {fr});
          }
          catch (Throwable tx)
          {
            String name = plugins.elementAt(i).getClass().getName();
            name = name.substring(name.lastIndexOf('.')+1);
            new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginNotifyError", name)), BStandardDialog.ERROR).showMessageDialog(null);
          }
        }
        fr.setVisible(true);
        fr.arrangeDockableWidgets();
        
        // If the user opens a file immediately after running the program, close the empty
        // scene window.
        
        for (int i = windows.size()-2; i >= 0; i--)
          if (windows.elementAt(i) instanceof LayoutWindow)
          {
            LayoutWindow win = (LayoutWindow) windows.elementAt(i);
            if (win.getScene().getName() == null && !win.isModified())
              closeWindow(win);
          }
      }
    });
  }
  
  /** Add a window to the list of open windows. */
  
  public static void addWindow(EditingWindow win)
  {
    windows.addElement(win);
  }
  
  /** Close a window. */
  
  public static void closeWindow(EditingWindow win)
  {
    if (win.confirmClose())
      {
        windows.removeElement(win);
        if (win instanceof LayoutWindow)
          for (int i = 0; i < plugins.size(); i++)
          {
            try
            {
              ((Plugin) plugins.elementAt(i)).processMessage(Plugin.SCENE_WINDOW_CLOSING, new Object [] {win});
            }
            catch (Throwable tx)
            {
              String name = plugins.elementAt(i).getClass().getName();
              name = name.substring(name.lastIndexOf('.')+1);
              new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginNotifyError", name)), BStandardDialog.ERROR).showMessageDialog(null);
            }
          }
      }
    if (windows.size() ==  0)
      quit();
  }
  
  /** Get a list of all open windows. */
  
  public static EditingWindow[] getWindows()
  {
    EditingWindow w[] = new EditingWindow [windows.size()];
    windows.copyInto(w);
    return w;
  }
  
  /** Quit Art of Illusion. */
  
  public static void quit()
  {
    for (int i = windows.size()-1; i >= 0; i--)
    {
      EditingWindow win = (EditingWindow) windows.elementAt(i);
      closeWindow(win);
      if (windows.contains(win))
        return;
    }
    for (int i = 0; i < plugins.size(); i++)
    {
      try
      {
        ((Plugin) plugins.elementAt(i)).processMessage(Plugin.APPLICATION_STOPPING, new Object [0]);
      }
      catch (Throwable tx)
      {
        String name = plugins.elementAt(i).getClass().getName();
        name = name.substring(name.lastIndexOf('.')+1);
        new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginNotifyError", name)), BStandardDialog.ERROR).showMessageDialog(null);
      }
    }
    System.exit(0);
  }
  
  private static void scanPlugins()
  {
    File dir = new File(PLUGIN_DIRECTORY);
    String files[], name;
    ZipFile zf = null;
    ZipEntry ze;
    BufferedReader in;
    Object instance;
    int i, j;
    
    if (!dir.exists())
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("cannotLocatePlugins")), BStandardDialog.ERROR).showMessageDialog(null);
      pluginLoader = new URLClassLoader [0];
      return;
    }
    files = dir.list();
    pluginLoader = new URLClassLoader [files.length];
    for (i = j = 0; i < files.length; i++)
    {
      // Open the zip file.

      File f = new File(PLUGIN_DIRECTORY, files[i]);
      try
      {
        zf = new ZipFile(f);
      }
      catch (IOException ex)
      {
        continue;  // Not a zip file.
      }
      try
      {
        pluginLoader[j++] = new URLClassLoader(new URL [] {f.toURL()});

        // Find the plugin directory file, and record any plugin classes.

        ze = zf.getEntry("plugins");
        if (ze != null)
        {
          in = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));
          name = in.readLine();
          while  (name != null)
          {
            try
            {
              instance = pluginLoader[j-1].loadClass(name).newInstance();
              if (instance instanceof Plugin)
                plugins.addElement(instance);
              if (instance instanceof Renderer)
                renderers.addElement(instance);
              if (instance instanceof Translator)
                translators.addElement(instance);
              if (instance instanceof ModellingTool)
                modellingTools.addElement(instance);
              if (instance instanceof Texture)
                textures.addElement(instance.getClass());
              if (instance instanceof Material)
                materials.addElement(instance.getClass());
              if (instance instanceof TextureMapping)
                textureMappings.addElement(instance.getClass());
              if (instance instanceof MaterialMapping)
                materialMappings.addElement(instance.getClass());
              if (instance instanceof artofillusion.image.filter.ImageFilter)
                filters.addElement(instance.getClass());
              if (instance instanceof Module)
                modules.addElement(instance.getClass());
            }
            catch (ClassNotFoundException ex)
            {
            }
            catch (IllegalAccessException ex)
            {
            }
            catch (InstantiationException ex)
            {
            }
            catch (Throwable tx)
            {
              new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginLoadError", name)), BStandardDialog.ERROR).showMessageDialog(null);
            }
            name = in.readLine();
          }
        }
      }
      catch (IOException ex)
      {
        System.out.println("Error reading plugin file "+files[i]);
      }
    }

    // Shorten the array if some files turned out not to be zip files.

    if (j < files.length)
      {
        URLClassLoader temp[] = new URLClassLoader [j];
        for (i = 0; i < j; i++)
          temp[i] = pluginLoader[i];
        pluginLoader = temp;
      }
  }
  
  /** Execute all startup scripts. */
  
  private static void runStartupScripts()
  {
    String files[] = new File(ModellingApp.STARTUP_SCRIPT_DIRECTORY).list();
    if (files != null)
      for (int i = 0; i < files.length; i++)
        if (files[i].endsWith(".bsh"))
          {
            try
              {
                String script = loadFile(new File(ModellingApp.STARTUP_SCRIPT_DIRECTORY, files[i]));
                ScriptRunner.executeScript(script);
              }
            catch (IOException ex)
              {
                ex.printStackTrace();
              }
          }
  }
  
  /** Get a class specified by name.  This checks both the system classes, and all plugins.
      It also accounts for classes which changed packages in version 1.3. */
  
  public static Class getClass(String name) throws ClassNotFoundException
  {
    try
    {
      return lookupClass(name);
    }
    catch (ClassNotFoundException ex)
    {
      int i = name.indexOf('$');
      if (i == -1)
      {
        String newName = (String) classTranslations.get(name);
        if (newName == null)
          throw ex;
        return lookupClass(newName);
      }
      String newName = (String) classTranslations.get(name.substring(0, i));
      if (newName == null)
        throw ex;
      return lookupClass(newName+name.substring(i));
    }
  }

  private static Class lookupClass(String name) throws ClassNotFoundException
  {
    try
      {
        return Class.forName(name);
      }
    catch (ClassNotFoundException ex)
    {
    }
    for (int i = 0; i < pluginLoader.length; i++)
      {
        try
          {
            return pluginLoader[i].loadClass(name);
          }
        catch (ClassNotFoundException ex)
          {
            if (i == pluginLoader.length-1)
              throw ex;
          }
      }
    return null;
  }
  
  /** This is a utility routine provided for the convenience of other classes, since it is
      such a common operation.  Given a Window, center it in the screen. */
  
  public static void centerWindow(Window win)
  {
    Dimension d1 = Toolkit.getDefaultToolkit().getScreenSize(), d2 = win.getSize();
    int x, y;
    
    x = (d1.width-d2.width)/2;
    y = (d1.height-d2.height)/2;
    if (x < 0)
      x = 0;
    if (y < 0)
      y = 0;
    win.setLocation(x, y);
  }
  
  /** This is a utility routine provided for the convenience of other classes, since it is
      such a common operation.  Given a Dialog, center it relative to its parent. */
  
  public static void centerDialog(Dialog dlg, Window parent)
  {
    Rectangle r = parent.getBounds();
    Dimension d = dlg.getSize();
    int x, y;
    
    x = r.x+(r.width-d.width)/2;
    y = r.y+20;
    if (x < 0)
      x = 0;
    if (y < 0)
      y = 0;
    dlg.setLocation(x, y);
  }
  
  /** This is a utility routine which loads a file from disk. */
  
  public static String loadFile(File f) throws IOException
  {
    BufferedReader in = new BufferedReader(new FileReader(f));
    StringBuffer buf = new StringBuffer();
    int c;
    while ((c = in.read()) != -1)
      buf.append((char) c);
    in.close();
    return buf.toString();
  }
  
  /** Save a scene to a file.  This method returns true if the scene is successfully saved,
      false if an error occurs. */

  public static boolean saveScene(Scene sc, LayoutWindow fr)
  {
    // Create the file.

    try
    {
      File f = new File(sc.getDirectory(), sc.getName());
      sc.writeToFile(f);
      for (int i = 0; i < plugins.size(); i++)
      {
        try
        {
          ((Plugin) plugins.elementAt(i)).processMessage(Plugin.SCENE_SAVED, new Object [] {f, fr});
        }
        catch (Throwable tx)
        {
          String name = plugins.elementAt(i).getClass().getName();
          name = name.substring(name.lastIndexOf('.')+1);
          new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginNotifyError", name)), BStandardDialog.ERROR).showMessageDialog(null);
        }
      }
      RecentFiles.addRecentFile(f);
    }
    catch (IOException ex)
    {
      new BStandardDialog("", new String [] {Translate.text("errorSavingScene"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(fr);
      return false;
    }
    return true;
  }
  
  /** Prompt the user to select a scene file, then open a new window containing it.  The BFrame is used for
      displaying dialogs. */
  
  public static void openScene(BFrame fr)
  {
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("openScene"));
    if (currentDirectory != null)
      fc.setDirectory(new File(currentDirectory));
    if (!fc.showDialog(fr))
      return;
    currentDirectory = fc.getDirectory().getAbsolutePath();
    openScene(fc.getSelectedFile(), fr);
  }
    
  /** Load a scene from a file, and open a new window containing it.  The BFrame is used for
      displaying dialogs. */
  
  public static void openScene(File f, BFrame fr)
  {
    // Open the file and read the scene.
    
    try
    {
      DataInputStream in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(f))));
      Scene sc = new Scene(in, true);
      sc.setName(f.getName());
      sc.setDirectory(f.getParent());
      if (sc.errorsOccurredInLoading())
        new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingScenePart")), BStandardDialog.ERROR).showMessageDialog(fr);
      newWindow(sc);
      in.close();
      RecentFiles.addRecentFile(f);
    }
    catch (InvalidObjectException ex)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingWholeScene")), BStandardDialog.ERROR).showMessageDialog(fr);
    }
    catch (IOException ex)
    {
      new BStandardDialog("", new String [] {Translate.text("errorLoadingFile"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(fr);
    }
  }
  
  /** Copy a list of objects to the clipboard, so they can be pasted into either the same scene or a
      different one. */
  
  public static void copyToClipboard(ObjectInfo obj[], Scene scene)
  {
    // First make a list of all textures used by the objects.
    
    Vector textures = new Vector();
    for (int i = 0; i < obj.length; i++)
      {
        Texture tex = obj[i].object.getTexture();
        if (tex instanceof LayeredTexture)
          {
            LayeredMapping map = (LayeredMapping) obj[i].object.getTextureMapping();
            Texture layer[] = map.getLayers();
            for (int j = 0; j < layer.length; j++)
              {
                Texture dup = layer[j].duplicate();
                dup.setID(layer[j].getID());
                textures.addElement(dup);
                map.setLayer(j, dup);
                map.setLayerMapping(j, map.getLayerMapping(j).duplicate(dup));
              }
          }
        else if (tex != null)
          {
            Texture dup = tex.duplicate();
            dup.setID(tex.getID());
            textures.addElement(dup);
            obj[i].object.setTexture(dup, obj[i].object.getTextureMapping().duplicate(dup));
          }
      }

    // Next make a list of all materials used by the objects.
    
    Vector materials = new Vector();
    for (int i = 0; i < obj.length; i++)
      {
        Material mat = obj[i].object.getMaterial();
        if (mat != null)
          {
            Material dup = mat.duplicate();
            dup.setID(mat.getID());
            materials.addElement(dup);
            obj[i].object.setMaterial(dup, obj[i].object.getMaterialMapping().duplicate(dup));
          }
      }
    
    // Now make a list of all ImageMaps used by any of them.

    Vector images = new Vector();
    for (int i = 0; i < scene.getNumImages(); i++)
      {
        ImageMap map = scene.getImage(i);
        boolean used = false;
        for (int j = 0; j < textures.size() && !used; j++)
          used = ((Texture) textures.elementAt(j)).usesImage(map);
        for (int j = 0; j < materials.size() && !used; j++)
          used = ((Material) materials.elementAt(j)).usesImage(map);
        if (used)
          images.addElement(map);
      }
    
    // Save all of them to the appropriate arrays.
    
    clipboardObject = obj;
    clipboardTexture = new Texture [textures.size()];
    textures.copyInto(clipboardTexture);
    clipboardMaterial = new Material [materials.size()];
    materials.copyInto(clipboardMaterial);
    clipboardImage = new ImageMap [images.size()];
    images.copyInto(clipboardImage);
  }
  
  /** Paste the contents of the clipboard into a window. */
  
  public static void pasteClipboard(LayoutWindow win)
  {
    if (clipboardObject == null)
      return;
    Scene scene = win.getScene();
    UndoRecord undo = new UndoRecord(win, false);
    win.setUndoRecord(undo);
    int sel[] = scene.getSelection();

    // First add any new image maps to the scene.
    
    for (int i = 0; i < clipboardImage.length; i++)
      {
        int j;
        for (j = 0; j < scene.getNumImages() && clipboardImage[i].getID() != scene.getImage(j).getID(); j++);
        if (j == scene.getNumImages())
          scene.addImage(clipboardImage[i]);
      }
    
    // Now add any new textures.
    
    for (int i = 0; i < clipboardTexture.length; i++)
      {
        Texture newtex;
        int j;
        for (j = 0; j < scene.getNumTextures() && clipboardTexture[i].getID() != scene.getTexture(j).getID(); j++);
        if (j == scene.getNumTextures())
          {
            newtex = clipboardTexture[i].duplicate();
            newtex.setID(clipboardTexture[i].getID());
            scene.addTexture(newtex);
          }
        else
          newtex = scene.getTexture(j);
        for (j = 0; j < clipboardObject.length; j++)
          {
            Texture current = clipboardObject[j].object.getTexture();
            if (current == clipboardTexture[i])
              clipboardObject[j].setTexture(newtex, clipboardObject[j].object.getTextureMapping().duplicate(newtex));
            else if (current instanceof LayeredTexture)
              {
                LayeredMapping map = (LayeredMapping) clipboardObject[j].object.getTextureMapping();
                map = (LayeredMapping) map.duplicate();
                clipboardObject[j].setTexture(new LayeredTexture(map), map);
                Texture layer[] = map.getLayers();
                for (int k = 0; k < layer.length; k++)
                  if (layer[k] == clipboardTexture[i])
                    {
                      map.setLayer(k, newtex);
                      map.setLayerMapping(k, map.getLayerMapping(k).duplicate(newtex));
                    }
              }
          }
      }
    
    // Add any new materials.
    
    for (int i = 0; i < clipboardMaterial.length; i++)
      {
        Material newmat;
        int j;
        for (j = 0; j < scene.getNumMaterials() && clipboardMaterial[i].getID() != scene.getMaterial(j).getID(); j++);
        if (j == scene.getNumMaterials())
        {
          newmat = clipboardMaterial[i].duplicate();
          newmat.setID(clipboardMaterial[i].getID());
          scene.addMaterial(newmat);
        }
        else
          newmat = scene.getMaterial(j);
        for (j = 0; j < clipboardObject.length; j++)
          {
            Material current = clipboardObject[j].object.getMaterial();
            if (current == clipboardMaterial[i])
              clipboardObject[j].setMaterial(newmat, clipboardObject[j].object.getMaterialMapping().duplicate(newmat));
          }
      }
    
    // Finally add the objects to the scene.
    
    ObjectInfo obj[] = ObjectInfo.duplicateAll(clipboardObject);
    for (int i = 0; i < obj.length; i++)
      win.addObject(obj[i], undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
  }
  
  /** Get the number of objects on the clipboard. */
  
  public static int getClipboardSize()
  {
    if (clipboardObject == null)
      return 0;
    return clipboardObject.length;
  }
}
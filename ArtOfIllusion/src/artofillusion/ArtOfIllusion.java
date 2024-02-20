/* Copyright (C) 1999-2013 by Peter Eastman
   Changes copyright (C) 2016-2024 by Maksim Khramov
   Changes copyright (C) 2016 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.image.*;
import artofillusion.image.filter.ImageFilter;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.script.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import artofillusion.keystroke.*;
import artofillusion.view.*;
import buoy.widget.*;
import buoy.xml.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;


/** This is the main class for Art of Illusion.  All of its methods and variables are static,
    so no instance of this class ever gets created.  It starts up the application, and
    maintains global variables. */

public class ArtOfIllusion
{
  public static final String APP_DIRECTORY, PLUGIN_DIRECTORY;
  public static final String TOOL_SCRIPT_DIRECTORY, OBJECT_SCRIPT_DIRECTORY, STARTUP_SCRIPT_DIRECTORY;
  public static final ImageIcon APP_ICON;

  private static ApplicationPreferences preferences;

  private static ObjectInfo[] clipboardObject;
  private static Texture[] clipboardTexture;
  private static Material[] clipboardMaterial;
  private static ImageMap[] clipboardImage;

  private static LinkedList<EditingWindow> windows = new LinkedList<EditingWindow>();
  private static final HashMap<String, String> classTranslations = new HashMap<String, String>();
  private static int numNewWindows = 0;

  static
  {
    // A clever trick for getting the location of the jar file, which David Smiley
    // posted to the Apple java-dev mailing list on April 14, 2002.  It works on
    // most, but not all, platforms, so in case of a problem, we fall back to using
    // user.dir.

    String dir = System.getProperty("user.dir");
    try
      {
        URL url = ArtOfIllusion.class.getResource("/artofillusion/ArtOfIllusion.class");
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
    try {        
        Properties translations = new Properties();
        translations.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("mappings.properties"));
        classTranslations.putAll((Map)translations);
    } catch(IOException ioe) {        
    }
    
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

    TitleWindow title = new TitleWindow();
    PluginRegistry.addCategory(Plugin.class);
    PluginRegistry.addCategory(Renderer.class);
    PluginRegistry.addCategory(Translator.class);
    PluginRegistry.addCategory(ModellingTool.class);
    PluginRegistry.addCategory(Texture.class);
    PluginRegistry.addCategory(Material.class);
    PluginRegistry.addCategory(TextureMapping.class);
    PluginRegistry.addCategory(MaterialMapping.class);
    PluginRegistry.addCategory(ImageFilter.class);
    PluginRegistry.addCategory(artofillusion.procedural.Module.class);
    PluginRegistry.registerPlugin(new UniformTexture());
    PluginRegistry.registerPlugin(new ImageMapTexture());
    PluginRegistry.registerPlugin(new ProceduralTexture2D());
    PluginRegistry.registerPlugin(new ProceduralTexture3D());
    PluginRegistry.registerPlugin(new UniformMaterial());
    PluginRegistry.registerPlugin(new ProceduralMaterial3D());
    PluginRegistry.registerPlugin(new UniformMapping(null, null));
    PluginRegistry.registerPlugin(new ProjectionMapping(null, null));
    PluginRegistry.registerPlugin(new CylindricalMapping(null, null));
    PluginRegistry.registerPlugin(new SphericalMapping(null, null));
    PluginRegistry.registerPlugin(new UVMapping(null, null));
    PluginRegistry.registerPlugin(new LinearMapping3D(null, null));
    PluginRegistry.registerPlugin(new LinearMaterialMapping(null, null));
    PluginRegistry.registerResource("TranslateBundle", "artofillusion", ArtOfIllusion.class.getClassLoader(), "artofillusion", null);
    PluginRegistry.registerResource("UITheme", "default", ArtOfIllusion.class.getClassLoader(), "artofillusion/Icons/defaultTheme.xml", null);
    PluginRegistry.scanPlugins();
    ThemeManager.initThemes();
    preferences = new ApplicationPreferences();
    KeystrokeManager.loadRecords();
    ViewerCanvas.addViewerControl(new ViewerOrientationControl());
    ViewerCanvas.addViewerControl(new ViewerPerspectiveControl());
    ViewerCanvas.addViewerControl(new ViewerScaleControl());
    ViewerCanvas.addViewerControl(new ViewerNavigationControl());

    for (Plugin plugin: PluginRegistry.getPlugins(Plugin.class))
    {
      try
      {
        plugin.processMessage(Plugin.APPLICATION_STARTING, new Object [0]);
      }
      catch (Throwable tx)
      {
        tx.printStackTrace();
        new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginInitError", plugin.getClass().getSimpleName())), BStandardDialog.ERROR).showMessageDialog(null);
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

  /** Get the complete version number of Art of Illusion. */

  public static String getVersion()
  {
    return getMajorVersion()+".0";
  }

  /** Get the major part of the version number of Art of Illusion. */

  public static String getMajorVersion()
  {
    return "3.2";
  }


  public static String getBuildInfo()
  {
    return java.util.Objects.toString(ArtOfIllusion.class.getPackage()
	    .getImplementationVersion(), "Missing Build Data!");    
  }
  /** Get the application preferences object. */

  public static ApplicationPreferences getPreferences()
  {
    return preferences;
  }

  /** Create a new Scene, and display it in a window. */

  public static void newWindow()
  {
    Scene theScene = new Scene();
    CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
    ObjectInfo info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");
    theScene.addObject(info, (UndoRecord)null);
    
    info = new ObjectInfo(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 0.8f), coords.duplicate(), "Light 1");
    theScene.addObject(info, (UndoRecord)null);
    
    newWindow(theScene);
  }

  /** Create a new window for editing the specified scene. */
  public static void newWindow(final Scene scene)
  {
    // New windows should always be created on the event thread.
    
    numNewWindows++;
    SwingUtilities.invokeLater(new Runnable() {
      public void run()
      {
        LayoutWindow fr = new LayoutWindow(scene);        
        windows.add(fr);
        fr.setVisible(true);
        fr.arrangeDockableWidgets();

        /* If the user opens a file immediately after running the program,
         * close the empty scene window. Delayed to work around timing bugs
         * when interacting with macOS and GLJPanels.
         */

        SwingWorker autoCloseUnmodified = new SwingWorker<Boolean, Void>()
        {
          @Override
          public Boolean doInBackground()
          {
            try
            {
              Thread.sleep(1000); //500 worked ; 250 failed
            }
            catch (InterruptedException ex)
            {
              System.out.println(ex);
            }

            for (EditingWindow window : windows)
            {
              if (window instanceof LayoutWindow
                  && window != fr  
                  && ((LayoutWindow)window).getScene().getName() == null
                  && ((LayoutWindow)window).isModified() == false
                  ) closeWindow(window);
            }
            return true;
          }

          @Override
          public void done()
          {
            try
            {
              Boolean result = get();
            }
            catch (InterruptedException ignore) {}
            catch (java.util.concurrent.ExecutionException e)
            {
              String why = null;
              Throwable cause = e.getCause();
              if (cause != null)
              {
                why = cause.getMessage();
              } else {
                why = e.getMessage();
              }
              System.err.println("Error: " + why);
            }
          }
        };
        autoCloseUnmodified.execute();
      }
    });    
  }

  /** Add a window to the list of open windows. */

  public static void addWindow(EditingWindow win)
  {
    windows.add(win);
  }

  /** Close a window. */
  public static void closeWindow(EditingWindow win)
  {
    if (win.confirmClose())
    {
      windows.remove(win);
    }
  }

  /** Get a list of all open windows. */

  public static EditingWindow[] getWindows()
  {
    return windows.toArray(new EditingWindow[windows.size()]);
  }

  /** Quit Art of Illusion. */
  public static void quit()
  {
      do
      {
        EditingWindow ew = windows.peekLast();
        if(ew.confirmClose())
        {
            windows.removeLast();
        } else
        {
            return;
        }        
      } while(!windows.isEmpty());
  }

  /** Execute all startup scripts. */
  private static void runStartupScripts()
  {
    String files[] = new File(STARTUP_SCRIPT_DIRECTORY).list();
    if(null == files)
      return;
    HashMap<String, Object> variables = new HashMap<String, Object>();
    
    for (String file : files)
    {
      String language = ScriptRunner.getLanguageForFilename(file);
      if (language != ScriptRunner.UNKNOWN_LANGUAGE)
        {
        try 
          {
            String script = loadFile(new File(STARTUP_SCRIPT_DIRECTORY, file));
            ScriptRunner.executeScript(language, script, variables);
          }
        catch (IOException ex)
          {
            ex.printStackTrace();
          }
        }
      else 
      {
        System.err.println (Translate.text ("unsupportedFileExtension") + " : " + file);
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
        String newName = classTranslations.get(name);
        if (newName == null)
          throw ex;
        return lookupClass(newName);
      }
      String newName = classTranslations.get(name.substring(0, i));
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
    List pluginLoaders = PluginRegistry.getPluginClassLoaders();
    for (int i = 0; i < pluginLoaders.size(); i++)
    {
      try
      {
        return ((ClassLoader) pluginLoaders.get(i)).loadClass(name);
      }
      catch (ClassNotFoundException ex)
      {
        if (i == pluginLoaders.size()-1)
          throw ex;
      }
    }
    return null;
  }

  /** This is a utility routine which loads a file from disk. */

  public static String loadFile(File f) throws IOException
  {
    BufferedReader in = new BufferedReader(new FileReader(f));
    StringBuilder buf = new StringBuilder();
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
      
      for (Plugin plugin: PluginRegistry.getPlugins(Plugin.class))
      {
        try
        {
          plugin.processMessage(Plugin.SCENE_SAVED, new Object [] {f, fr});
        }
        catch (Throwable tx)
        {
          tx.printStackTrace();
          new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginNotifyError", plugin.getClass().getSimpleName())), BStandardDialog.ERROR).showMessageDialog(null);
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
    if (getCurrentDirectory() != null)
      fc.setDirectory(new File(getCurrentDirectory()));
    //fully qualified path, as otherwise conflicts with an AWT class.
    javax.swing.filechooser.FileFilter sceneFilter = new FileNameExtensionFilter(Translate.text("fileFilter.aoi"), "aoi");
    fc.getComponent().setAcceptAllFileFilterUsed(true);
    fc.getComponent().addChoosableFileFilter(sceneFilter);
    Preferences pref = Preferences.userNodeForPackage(ArtOfIllusion.class);
    javax.swing.filechooser.FileFilter filter = pref.getBoolean("FilterSceneFiles", true)? sceneFilter : fc.getComponent().getAcceptAllFileFilter();
    fc.getComponent().setFileFilter(filter);
    if (!fc.showDialog(fr))
      return;
    pref.putBoolean("FilterSceneFiles", fc.getFileFilter() == sceneFilter);
    setCurrentDirectory(fc.getDirectory().getAbsolutePath());
    openScene(fc.getSelectedFile(), fr);
  }

  /** Load a scene from a file, and open a new window containing it.  The BFrame is used for
      displaying dialogs. */

  public static void openScene(File file, BFrame frame)
  {
    // Open the file and read the scene.

    try
    {
      Scene scene = new Scene(file, true);
      List<String> errors = scene.getErrors();
      if (!errors.isEmpty())
      {
        String allErrors = String.join("\n", errors);
        new BStandardDialog("", new Object[] {UIUtilities.breakString(Translate.text("errorLoadingScenePart")), new BScrollPane(new BTextArea(allErrors))}, BStandardDialog.ERROR).showMessageDialog(frame);
    }

      newWindow(scene);
      RecentFiles.addRecentFile(file);
    }
    catch (InvalidObjectException ex)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingWholeScene")), BStandardDialog.ERROR).showMessageDialog(frame);
    }
    catch (IOException ex)
    {
      new BStandardDialog("", new String [] {Translate.text("errorLoadingFile"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(frame);
    }
  }

  /** Copy a list of objects to the clipboard, so they can be pasted into either the same scene or a
      different one. */

  public static void copyToClipboard(ObjectInfo[] obj, Scene scene)
  {
    // First, make a list of all textures used by the objects.

    List<Texture> textures = new ArrayList<>();
    for (ObjectInfo cObj: obj)
      {
        Object3D object = cObj.getObject();
        Texture tex = object.getTexture();
        if (tex instanceof LayeredTexture)
          {
            LayeredMapping map = (LayeredMapping) object.getTextureMapping();
            Texture[] layer = map.getLayers();
            for (int j = 0; j < layer.length; j++)
              {
                Texture dup = layer[j].duplicate();
                dup.setID(layer[j].getID());
                textures.add(dup);
                map.setLayer(j, dup);
                map.setLayerMapping(j, map.getLayerMapping(j).duplicate(object, dup));
              }
          }
        else if (tex != null)
          {
            Texture dup = tex.duplicate();
            dup.setID(tex.getID());
            textures.add(dup);
            object.setTexture(dup, object.getTextureMapping().duplicate(object, dup));
          }
      }

    // Next, make a list of all materials used by the objects.

    List<Material> materials = new ArrayList<>();
    for (ObjectInfo cObj: obj)
      {
        Object3D object = cObj.getObject();
        Material mat = object.getMaterial();
        if (mat != null)
          {
            Material dup = mat.duplicate();
            dup.setID(mat.getID());
            materials.add(dup);
            object.setMaterial(dup, object.getMaterialMapping().duplicate(object, dup));
          }
      }

    // Now make a list of all ImageMaps used by any of them.

    List<ImageMap> images = new ArrayList<>();
    for (ImageMap map: scene.getImages())
      {
        if(textures.stream().anyMatch(texture -> texture.usesImage(map)) || materials.stream().anyMatch(material -> material.usesImage(map)))
        {
          images.add(map);
        }
      }

    // Save all of them to the appropriate arrays.

    clipboardObject = obj;
    clipboardTexture = textures.toArray(new Texture[0]);
    clipboardMaterial = materials.toArray(new Material[0]);
    clipboardImage = images.toArray(new ImageMap[0]);
  }

  /** Paste the contents of the clipboard into a window. */

  public static void pasteClipboard(LayoutWindow win)
  {
    if (clipboardObject == null)
      return;
    Scene scene = win.getScene();
    UndoRecord undo = new UndoRecord(win, false);
    win.setUndoRecord(undo);
    int[] sel = win.getSelectedIndices();

    // First, add any new image maps to the scene.
    for (ImageMap map: clipboardImage)
    {
      if(scene.getImages().stream().anyMatch(image -> image.getID() == map.getID()))  continue;
      scene.addImage(map);
    }

    // Now add any new textures.
    for (Texture match: clipboardTexture)
    {
      Texture newTex = ArtOfIllusion.getSceneTextureOrAdd(scene, match);
      for (ObjectInfo cObj: clipboardObject)
      {
        Object3D object = cObj.getObject();
        Texture current = object.getTexture();
        if (current == null) continue;

        ParameterValue[] newParamValues = copyObjectParameters(object);
        if (current == match)
          cObj.setTexture(newTex, object.getTextureMapping().duplicate(object, newTex));
        else if (current instanceof LayeredTexture)
          {
            LayeredMapping map = (LayeredMapping) object.getTextureMapping();
            map = (LayeredMapping) map.duplicate();
            cObj.setTexture(new LayeredTexture(map), map);
            Texture[] layer = map.getLayers();
            for (int k = 0; k < layer.length; k++)
              if (layer[k] == match)
                {
                  map.setLayer(k, newTex);
                  map.setLayerMapping(k, map.getLayerMapping(k).duplicate(object, newTex));
                }
          }
        object.setParameterValues(newParamValues);

      }
    }

    // Add any new materials.

    for (Material mat:  clipboardMaterial)
    {
      Material newMat = ArtOfIllusion.getSceneMaterialOrAdd(scene, mat);

      for (ObjectInfo cObj: clipboardObject)
      {
        Object3D object = cObj.getObject();
        Material current = object.getMaterial();
        if (current == mat)
          cObj.setMaterial(newMat, object.getMaterialMapping().duplicate(object, newMat));
      }
    }

    // Finally, add the objects to the scene.
    for (ObjectInfo obj: ObjectInfo.duplicateAll(clipboardObject)) win.addObject(obj, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, sel);
  }

  public static Texture getSceneTextureOrAdd(Scene scene, Texture match)
  {
    Optional<Texture> asset = scene.getTextures().stream().filter(texture -> texture.getID() == match.getID()).findFirst();

    return asset.orElseGet(() -> {
      Texture newTex = match.duplicate();
      newTex.setID(match.getID());
      scene.addTexture(newTex);
      return newTex;
    });
  }

  public static Material getSceneMaterialOrAdd(Scene scene, Material match)
  {
    Optional<Material> asset = scene.getMaterials().stream().filter(material -> material.getID() == match.getID()).findFirst();

    return asset.orElseGet(() -> {
      Material newMat = match.duplicate();
      newMat.setID(match.getID());
      scene.addMaterial(newMat);
      return newMat;
    });
  }

  private static ParameterValue[] copyObjectParameters(Object3D object)
  {
    ParameterValue[] oldParamValues = object.getParameterValues();
    ParameterValue[] newParamValues = new ParameterValue[oldParamValues.length];
    for (int k = 0; k < newParamValues.length; k++) newParamValues[k] = oldParamValues[k].duplicate();
    return newParamValues;
  }

  /** Get the number of objects on the clipboard. */

  public static int getClipboardSize()
  {
    return clipboardObject == null ? 0 : clipboardObject.length;
  }

  /** Get the directory in which the user most recently accessed a file. */

  public static String getCurrentDirectory()
  {
    return ModellingApp.currentDirectory;
  }

  /** Set the directory in which the user most recently accessed a file. */

  public static void setCurrentDirectory(String currentDirectory)
  {
    ModellingApp.currentDirectory = currentDirectory;
  }

  public static void showErrors(Map<String, Throwable> errors) {
    java.util.function.Function<Map.Entry<String, Throwable>, String> tmss = (Map.Entry<String, Throwable> t) -> "Plugin: "
            + t + " throw: " + t.getValue().getMessage()
            + " with" + Arrays.toString(t.getValue().getStackTrace());
    List<String> err = errors.entrySet().stream().map(tmss).collect(java.util.stream.Collectors.toList());
    showErrors(err);
  }
  
  public static void showErrors(List<String> errors)
  {
      BTextArea report = new BTextArea(String.join("\n\n", errors));
      JTextArea area = report.getComponent();
      area.setPreferredSize(new java.awt.Dimension(500, 200));
      area.setFont(area.getFont().deriveFont(12f));
      area.setLineWrap(true);
      area.setEditable(false);
      area.setWrapStyleWord(true);      
      SwingUtilities.invokeLater(() -> new BStandardDialog("Art Of Illusion", new Object[] { new BScrollPane(report) }, BStandardDialog.ERROR).showMessageDialog(null));
  }
}

/* Copyright (C) 2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import buoy.widget.*;

import java.io.*;
import java.util.zip.*;
import java.util.*;
import java.net.*;

import artofillusion.ui.*;

public class PluginRegistry
{
  private static final ArrayList pluginLoaders = new ArrayList();
  private static final HashSet categories = new HashSet();
  private static final HashMap categoryClasses = new HashMap();

  /**
   * Scan all files in the Plugins directory, read in their indices, and record all plugins
   * contained in them.
   */

  static void scanPlugins()
  {
    File dir = new File(ModellingApp.PLUGIN_DIRECTORY);
    if (!dir.exists())
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("cannotLocatePlugins")), BStandardDialog.ERROR).showMessageDialog(null);
      return;
    }

    // Scan the plugins directory, and parse the index in every jar file.

    String files[] = dir.list();
    HashSet jars = new HashSet();
    for (int i = 0; i < files.length; i++)
    {
      try
      {
        jars.add(new JarInfo(new File(dir, files[i])));
      }
      catch (IOException ex)
      {
        // Not a zip file.
      }
    }

    // Now build a classloader for each one, registering plugins, categories, and resources.
    // This needs to be done in the proper order to account for dependencies between plugins.

    HashMap nameMap = new HashMap();
    while (jars.size() > 0)
    {
      boolean processedAny = false;
      for (Iterator jarIterator = new ArrayList(jars).iterator(); jarIterator.hasNext(); )
      {
        JarInfo jar = (JarInfo) jarIterator.next();

        // See if we've already processed all other jars it depends on.

        boolean importsOk = true;
        for (Iterator importIterator = jar.imports.iterator(); importsOk && importIterator.hasNext(); )
          importsOk &= nameMap.containsKey(importIterator.next());
        if (importsOk)
        {
          processJar(jar, nameMap);
          processedAny = true;
          jars.remove(jar);
        }
      }
      if (!processedAny)
      {
        System.err.println("*** The following plugins were not loaded because their imports could not be resolved:");
        for (Iterator jarIterator = jars.iterator(); jarIterator.hasNext(); )
          System.out.println(((JarInfo) jarIterator.next()).file.getName());
        System.err.println();
        break;
      }
    }
  }

  /**
   * Process a single jar file in the Plugins directory.
   *
   * @param jar       the jar file being processed
   * @param nameMap   maps plugin names to JarInfo objects
   */

  private static void processJar(JarInfo jar, Map nameMap)
  {
    try
    {
      if (jar.imports.size() == 0)
        jar.loader = new URLClassLoader(new URL [] {jar.file.toURI().toURL()});
      else
      {

      }
      pluginLoaders.add(jar.loader);
      if (jar.name != null && jar.name.length() > 0)
        nameMap.put(jar.name, jar);
      for (int i = 0; i < jar.categories.size(); i++)
        addCategory(jar.loader.loadClass((String) jar.categories.get(i)));
      for (int i = 0; i < jar.classes.size(); i++)
        registerPlugin(jar.loader.loadClass((String) jar.classes.get(i)).newInstance());
    }
    catch (Exception ex)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("pluginLoadError", jar.file.getName())), BStandardDialog.ERROR).showMessageDialog(null);
      System.err.println("*** Exception while initializing plugin "+jar.file.getName()+":");
      ex.printStackTrace();
    }
  }

  /**
   * Get the ClassLoaders for all jar files in the Plugins directory.  There is one ClassLoader
   * for every jar.
   */

  public static List getPluginClassLoaders()
  {
    return new ArrayList(pluginLoaders);
  }

  /**
   * Define a new category of plugins.  A category is specified by a class or interface.  After
   * adding a category, any call to {@link #registerPlugin(Object)} will check the registered object
   * to see if it is an instance of the specified class.  If so, it is added to the list of plugins
   * in that category.
   */

  public static void addCategory(Class category)
  {
    categories.add(category);
  }

  /**
   * Get all categories of plugins that have been defined.
   */

  public static List getCategories()
  {
    return new ArrayList(categories);
  }

  /**
   * Register a new plugin.  The specified object is checked against every defined category of plugins
   * by seeing if it is an instance of the class or interface defining each category.  If so, it is
   * added to the list of plugins in that category.
   */

  public static void registerPlugin(Object plugin)
  {
    for (Iterator categoryIterator = categories.iterator(); categoryIterator.hasNext(); )
    {
      Class category = (Class) categoryIterator.next();
      if (category.isInstance(plugin))
      {
        List instances = (List) categoryClasses.get(category);
        if (instances == null)
        {
          instances = new ArrayList();
          categoryClasses.put(category, instances);
        }
        instances.add(plugin);
      }
    }
  }

  /**
   * Get all registered plugins in a particular category.
   */

  public static List getPlugins(Class category)
  {
    List plugins = (List) categoryClasses.get(category);
    if (plugins == null)
      return new ArrayList();
    return new ArrayList(plugins);
  }

  /**
   * This class is used to store information about the content of a jar file during initialization.
   */

  private static class JarInfo
  {
    File file;
    String name, version;
    ArrayList imports, classes, categories, resources;
    ClassLoader loader;

    JarInfo(File file) throws IOException
    {
      this.file = file;
      imports = new ArrayList();
      classes = new ArrayList();
      categories = new ArrayList();
      resources = new ArrayList();
      ZipFile zf = new ZipFile(file);
      try
      {
        ZipEntry plugins = zf.getEntry("plugins");
        if (plugins != null)
        {
          BufferedReader in = new BufferedReader(new InputStreamReader(zf.getInputStream(plugins)));
          String className = in.readLine();
          while (className != null)
          {
            classes.add(className.trim());
            className = in.readLine();
          }
          return;
        }
        throw new IOException(); // No index found
      }
      finally
      {
        zf.close();
      }
    }
  }
}

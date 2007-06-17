/* Copyright (C) 2007 by Peter Eastman
   Some parts copyright (C) 2006 by Nik Trevallyn-Jones

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
import java.lang.reflect.*;

import artofillusion.ui.*;
import artofillusion.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;

public class PluginRegistry
{
  private static final ArrayList pluginLoaders = new ArrayList();
  private static final HashSet categories = new HashSet();
  private static final HashMap categoryClasses = new HashMap();
  private static final HashMap resources = new HashMap();
  private static final HashMap exports = new HashMap();

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
      catch (Exception ex)
      {
        System.err.println("*** Exception loading plugin file "+files[i]);
        ex.printStackTrace(System.err);
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
        SearchlistClassLoader loader = new SearchlistClassLoader(new URL [] {jar.file.toURI().toURL()});;
        jar.loader = loader;
        for (Iterator importIterator = jar.imports.iterator(); importIterator.hasNext(); )
          loader.add(((JarInfo) nameMap.get(importIterator.next())).loader);
      }
      pluginLoaders.add(jar.loader);
      HashMap classNameMap = new HashMap();
      if (jar.name != null && jar.name.length() > 0)
        nameMap.put(jar.name, jar);
      for (int i = 0; i < jar.categories.size(); i++)
        addCategory(jar.loader.loadClass((String) jar.categories.get(i)));
      for (int i = 0; i < jar.plugins.size(); i++)
      {
        Object plugin = jar.loader.loadClass((String) jar.plugins.get(i)).newInstance();
        registerPlugin(plugin);
        classNameMap.put(jar.plugins.get(i), plugin);
      }
      for (int i = 0; i < jar.exports.size(); i++)
      {
        ExportInfo info = (ExportInfo) jar.exports.get(i);
        info.plugin = classNameMap.get(info.className);
        registerExportedMethod(info);
      }
      for (int i = 0; i < jar.resources.size(); i++)
      {
        ResourceInfo info = (ResourceInfo) jar.resources.get(i);
        registerResource(info.type, info.id, jar.loader, info.name, info.locale);
      }
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
   * Register a new resource.  You can then call {@link #getResource(String, String)} to look up
   * a particular resource, or {@link #getResources(String)} to find all registered resources of
   * a particular type.
   *
   * @param type        the type of resource being registered
   * @param id          the id of this resource
   * @param loader      the ClassLoader with which to load the resource
   * @param name        the fully qualified name of the resource, that should be passed to
   *                    <code>loader.getResource()</code> to load it
   * @param locale      the locale this resource represents (may be null)
   * @throws IllegalArgumentException if there is already a registered resource with the same type, id, and locale
   */

  public static void registerResource(String type, String id, ClassLoader loader, String name, Locale locale) throws IllegalArgumentException
  {
    Map resourcesForType = (Map) resources.get(type);
    if (resourcesForType == null)
    {
      resourcesForType = new HashMap();
      resources.put(type, resourcesForType);
    }
    PluginResource resource = (PluginResource) resourcesForType.get(id);
    if (resource == null)
    {
      resource = new PluginResource(type, id);
      resourcesForType.put(id, resource);
    }
    resource.addResource(name, loader, locale);
  }

  /**
   * Get a list of all type identifiers for which there are PluginResources available.
   */

  public static List getResourceTypes()
  {
    return new ArrayList(resources.keySet());
  }

  /**
   * Get a list of all registered PluginResources of a particular type.
   */

  public static List getResources(String type)
  {
    Map resourcesForType = (Map) resources.get(type);
    if (resourcesForType == null)
      return new ArrayList();
    return new ArrayList(resourcesForType.values());
  }

  /**
   * Get the PluginResource with a particular type and id, or null if there is no such resource.
   */

  public static PluginResource getResource(String type, String id)
  {
    Map resourcesForType = (Map) resources.get(type);
    if (resourcesForType == null)
      return null;
    return (PluginResource) resourcesForType.get(id);
  }

  /**
   * Register a method which may be invoked on a plugin object.  This allows external code to easily
   * use features of a plugin without needing to directly import that plugin or use reflection.
   * Use {@link #getExportedMethodIds()} to get a list of all exported methods that have been
   * registered, and {@link #invokeExportedMethod(String, Object[])} to invoke one.
   *
   * @param plugin     the plugin object on which the method should be invoked
   * @param method     the name of the method to invoke
   * @param id         a unique identifier which may be passed to <code>invokeExportedMethod()</code>
   *                   to identify this method
   */

  public static void registerExportedMethod(Object plugin, String method, String id) throws IllegalArgumentException
  {
    ExportInfo info = new ExportInfo();
    info.plugin = plugin;
    info.method = method;
    info.id = id;
    registerExportedMethod(info);
  }

  private static void registerExportedMethod(ExportInfo export) throws IllegalArgumentException
  {
    if (exports.containsKey(export.id))
      throw new IllegalArgumentException("Multiple exported methods with id="+export.id);
    exports.put(export.id, export);
  }

  /**
   * Get a list of the identifiers of all exported methods which have been registered.
   */

  public static List getExportedMethodIds()
  {
    return new ArrayList(exports.keySet());
  }

  /**
   * Invoke an exported method of a plugin object.
   *
   * @param id       the unique identifier of the method to invoke
   * @param args     the list of arguments to pass to the method
   * @return the value returned by the method after it was invoked
   * @throws NoSuchMethodException if there is no exported method with the specified ID, or if there
   * is no form of the exported method whose arguments are compatible with the specified args array.
   * @throws InvocationTargetException if the method threw an exception when it was invoked.
   */

  public static Object invokeExportedMethod(String id, Object args[]) throws NoSuchMethodException, InvocationTargetException
  {
    ExportInfo info = (ExportInfo) exports.get(id);
    if (info == null)
      throw new NoSuchMethodException("There is no exported method with id="+id);

    // Try to find a method to invoke.

    Method methods[] = info.plugin.getClass().getMethods();
    for (int i = 0; i < methods.length; i++)
    {
      if (!methods[i].getName().equals(info.method))
        continue;
      Class types[] = methods[i].getParameterTypes();
      if (types.length != args.length)
        continue;
      boolean valid = true;
      for (int j = 0; valid && j < types.length; j++)
        valid = (args[j] == null || types[j].isInstance(args[j]));
      if (valid)
      {
        try
        {
          return methods[i].invoke(info.plugin, args);
        }
        catch (IllegalAccessException ex)
        {
          // This should be impossible, since getMethods() only returns public methods.

          throw new InvocationTargetException(ex);
        }
      }
    }
    throw new NoSuchMethodException("No method found which matches the specified name and argument types.");
  }

  /**
   * This class is used to store information about the content of a jar file during initialization.
   */

  private static class JarInfo
  {
    File file;
    String name, version;
    ArrayList imports, plugins, categories, resources, exports;
    ClassLoader loader;

    JarInfo(File file) throws IOException
    {
      this.file = file;
      imports = new ArrayList();
      plugins = new ArrayList();
      categories = new ArrayList();
      resources = new ArrayList();
      exports = new ArrayList();
      ZipFile zf = new ZipFile(file);
      try
      {
        ZipEntry ze = zf.getEntry("extensions.xml");
        if (ze != null)
        {
          InputStream in = new BufferedInputStream(zf.getInputStream(ze));
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          try
          {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            Element extensions = doc.getDocumentElement();
            if (!"extension".equals(extensions.getNodeName()))
              throw new Exception("The root element must be <extension>");
            Node nameNode = extensions.getAttributes().getNamedItem("name");
            if (nameNode != null)
              name = nameNode.getNodeValue();
            NodeList categoryList = doc.getElementsByTagName("category");
            for (int i = 0; i < categoryList.getLength(); i++)
            {
              Node category = categoryList.item(i);
              categories.add(category.getAttributes().getNamedItem("class").getNodeValue());
            }
            NodeList pluginList = doc.getElementsByTagName("plugin");
            for (int i = 0; i < pluginList.getLength(); i++)
            {
              Node plugin = pluginList.item(i);

              // Check for <export> tags inside the <plugin> tag.

              String className = plugin.getAttributes().getNamedItem("class").getNodeValue();
              plugins.add(className);
              NodeList children = plugin.getChildNodes();
              for (int k = 0; k < children.getLength(); k++)
              {
                Node childNode = children.item(k);
                if ("export".equals(childNode.getNodeName()))
                {
                  ExportInfo export = new ExportInfo();
                  export.method = childNode.getAttributes().getNamedItem("method").getNodeValue();
                  export.id = childNode.getAttributes().getNamedItem("id").getNodeValue();
                  export.className = className;
                  exports.add(export);
                }
              }
            }
            NodeList importList = doc.getElementsByTagName("import");
            for (int i = 0; i < importList.getLength(); i++)
            {
              Node importNode = importList.item(i);
              imports.add(importNode.getAttributes().getNamedItem("name").getNodeValue());
            }
            NodeList resourceList = doc.getElementsByTagName("resource");
            for (int i = 0; i < resourceList.getLength(); i++)
            {
              Node resourceNode = resourceList.item(i);
              ResourceInfo resource = new ResourceInfo();
              resource.type = resourceNode.getAttributes().getNamedItem("type").getNodeValue();
              resource.id = resourceNode.getAttributes().getNamedItem("id").getNodeValue();
              resource.name = resourceNode.getAttributes().getNamedItem("name").getNodeValue();
              Node localeNode = resourceNode.getAttributes().getNamedItem("locale");
              if (localeNode != null)
              {
                String[] parts = localeNode.getNodeValue().split("_");
                if (parts.length == 1)
                  resource.locale = new Locale(parts[0]);
                else if (parts.length == 2)
                  resource.locale = new Locale(parts[0], parts[1]);
                else if (parts.length == 3)
                  resource.locale = new Locale(parts[0], parts[1], parts[2]);
              }
              resources.add(resource);
            }
          }
          catch (Exception ex)
          {
            System.err.println("*** Exception while parsing extensions.xml for plugin "+file.getName()+":");
            ex.printStackTrace();
            throw new IOException();
          }
          return;
        }
        ze = zf.getEntry("plugins");
        if (ze != null)
        {
          BufferedReader in = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));
          String className = in.readLine();
          while (className != null)
          {
            plugins.add(className.trim());
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

  /**
   * A PluginResource represents a resource that was loaded from a plugin.  Each PluginResource
   * is identified by a type and an id.  Typically the type indicates the purpose for which a
   * resource is to be used, and the id designates a specific resource of that type.
   * <p>
   * It is also possible for several different localized versions of a resource to be available,
   * possibly provided by different plugins.  A single PluginResource object represents all
   * the different localized resources that share the same type and id.  When you invoke one of
   * the methods to access the resource's contents, the localized version that most closely matches the
   * currently selected locale is used.
   */

  public static class PluginResource
  {
    private String type, id;
    private ArrayList names, loaders, locales;

    private PluginResource(String type, String id)
    {
      this.type = type;
      this.id = id;
      names = new ArrayList();
      loaders = new ArrayList();
      locales = new ArrayList();
    }

    private void addResource(String name, ClassLoader loader, Locale locale) throws IllegalArgumentException
    {
      if (locales.contains(locale))
        throw new IllegalArgumentException("Multiple resource definitions for type="+type+", name="+ id +", locale="+locale);
      names.add(name);
      loaders.add(loader);
      locales.add(locale);
    }

    /**
     * Get the type of this PluginResource.
     */

    public String getType()
    {
      return type;
    }

    /**
     * Get the id of this PluginResource.
     */

    public String getId()
    {
      return id;
    }

    /**
     * Find which localized version of the resource best matches a locale.
     */

    private int findLocalizedVersion(Locale locale)
    {
      int bestMatch = 0, bestMatchedLevels = 0;
      for (int i = 0; i < locales.size(); i++)
      {
        Locale loc = (Locale) locales.get(i);
        int matchedLevels = 0;
        if (loc != null && loc.getLanguage() == locale.getLanguage())
        {
          matchedLevels++;
          if (loc.getCountry() == locale.getCountry())
          {
            matchedLevels++;
            if (loc.getVariant() == locale.getVariant())
              matchedLevels++;
          }
        }
        if (matchedLevels > bestMatchedLevels)
        {
          bestMatch = i;
          bestMatchedLevels = matchedLevels;
        }
      }
      return bestMatch;
    }

    /**
     * Get an InputStream for reading this resource.  If there are multiple localized versions,
     * the version which best matches the currently selected locale is used.
     */

    public InputStream getInputStream()
    {
      int index = findLocalizedVersion(Translate.getLocale());
      return ((ClassLoader) loaders.get(index)).getResourceAsStream((String) names.get(index));
    }

    /**
     * Get a URL for reading this resource.  If there are multiple localized versions,
     * the version which best matches the currently selected locale is used.
     */

    public URL getURL()
    {
      int index = findLocalizedVersion(Translate.getLocale());
      return ((ClassLoader) loaders.get(index)).getResource((String) names.get(index));
    }

    /**
     * Get the fully qualified name of the resource this represents.  If there are multiple localized
     * versions, the version which best matches the currently selected locale is used.
     */

    public String getName()
    {
      int index = findLocalizedVersion(Translate.getLocale());
      return (String) names.get(index);
    }

    /**
     * Get the ClassLoader responsible for loading this resource.  If there are multiple localized
     * versions, the version which best matches the currently selected locale is used.
     */

    public ClassLoader getClassLoader()
    {
      int index = findLocalizedVersion(Translate.getLocale());
      return (ClassLoader) loaders.get(index);
    }
  }

  /**
   * This class is used to store information about an "export" record in an XML file.
   */

  private static class ExportInfo
  {
    String method, id, className;
    Object plugin;
  }

  /**
   * This class is used to store information about a "resource" record in an XML file.
   */

  private static class ResourceInfo
  {
    String type, id, name;
    Locale locale;
    HashMap values = new HashMap();
  }
}

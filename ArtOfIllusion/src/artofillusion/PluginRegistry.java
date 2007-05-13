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
import java.lang.reflect.Proxy;

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
      if (jar.name != null && jar.name.length() > 0)
        nameMap.put(jar.name, jar);
      for (int i = 0; i < jar.categories.size(); i++)
        addCategory(jar.loader.loadClass((String) jar.categories.get(i)));
      for (int i = 0; i < jar.plugins.size(); i++)
        registerPlugin(jar.loader.loadClass((String) jar.plugins.get(i)).newInstance());
      for (int i = 0; i < jar.proxies.size(); i++)
      {
        ProxyInfo info = (ProxyInfo) jar.proxies.get(i);
        Object proxy = Proxy.newProxyInstance(jar.loader, info.getInterfaces(jar.loader), new ProxyHandler(info.target, jar.loader, info.values));
        registerPlugin(proxy);
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
   * This class is used to store information about the content of a jar file during initialization.
   */

  private static class JarInfo
  {
    File file;
    String name, version;
    ArrayList imports, plugins, categories, resources, proxies;
    ClassLoader loader;

    JarInfo(File file) throws IOException
    {
      this.file = file;
      imports = new ArrayList();
      plugins = new ArrayList();
      categories = new ArrayList();
      resources = new ArrayList();
      proxies = new ArrayList();
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
            NodeList valueList = doc.getElementsByTagName("method");
            for (int i = 0; i < pluginList.getLength(); i++)
            {
              Node plugin = pluginList.item(i);

              // See whether a proxy should be created for this plugin.

              ProxyInfo proxy = null;
              NodeList children = plugin.getChildNodes();
              for (int k = 0; k < children.getLength() && proxy == null; k++)
              {
                Node proxyNode = children.item(k);
                if (!"proxy".equals(proxyNode.getNodeName()))
                  continue;
                proxy = new ProxyInfo();
                proxy.target = plugin.getAttributes().getNamedItem("class").getNodeValue();
                proxy.interfaces = proxyNode.getAttributes().getNamedItem("interface").getNodeValue().split(";");
                for (int j = 0; j < valueList.getLength(); j++)
                  if (valueList.item(j).getParentNode() == proxyNode)
                  {
                    NamedNodeMap attributes  = valueList.item(j).getAttributes();
                    proxy.values.put(attributes.getNamedItem("name").getNodeValue(), attributes.getNamedItem("value").getNodeValue());
                  }
                proxies.add(proxy);
              }
              if (proxy == null)
                plugins.add(plugin.getAttributes().getNamedItem("class").getNodeValue());
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
   * This class is used to store information about a "proxy" record in an XML file.
   */

  private static class ProxyInfo
  {
    String target, interfaces[];
    HashMap values = new HashMap();

    Class[] getInterfaces(ClassLoader loader) throws ClassNotFoundException
    {
      Class cls[] = new Class[interfaces.length];
      for (int i = 0; i < cls.length; i++)
        cls[i] = loader.loadClass(interfaces[i]);
      return cls;
    }
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

  /**
   * This is the InvocationHandler for automatically generated proxies.
   */

  private static class ProxyHandler implements InvocationHandler
  {
    protected String type;
    protected ClassLoader loader;
    protected Object target;
    protected Map map;

    public ProxyHandler(String type, ClassLoader loader, Map map)
    {
      this.type = type;
      this.loader = loader;
      this.map = map;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
    {
      String name = method.getName();
      try
      {
        if ((args == null || args.length == 0) && map.containsKey(name))
        {
          String value = (String) map.get(name);
          Class valType = method.getReturnType();

          // If the return type is String, simply return in.

          if (valType == String.class)
            return value;
          if (valType.isPrimitive())
          {
            // Deal with primitive types.

            if (valType == int.class)
              return new Integer(value);
            if (valType == boolean.class)
              return Boolean.valueOf(value);
            if (valType == float.class)
              return new Float(value);
            if (valType == long.class)
              return new Long(value);
            if (valType == double.class)
              return new Double(value);
            if (valType == byte.class)
              return new Byte(value);
            if (valType == char.class)
              return (value == null || value.length() == 0 ? new Character('\0') : new Character(value.charAt(0)));
            if (valType == short.class)
              return new Short(value);
          }

          // See if there is a static valueOf(String) method.

          try
          {
            Method valueOf = valType.getMethod("valueOf", new Class[] {String.class});
            if (Modifier.isStatic(valueOf.getModifiers()))
              return valueOf.invoke(null, new Object[] {value});
          }
          catch (NoSuchMethodException ex)
          {
          }

          // See if there is a constructor that takes a String.

          try
          {
            Constructor ctor = valType.getConstructor(new Class[] {String.class});
            return ctor.newInstance(new Object[] {value});
          }
          catch (NoSuchMethodException ex)
          {
          }

          // Give up and just invoke the method on the target.

          System.out.println("PluginRegistry: unable to create a value of type "+valType.getName()+" for proxy method");
        }
      }
      catch (Exception e)
      {
        System.out.println("PluginRegistry: error invoking proxy method "+name);
      }
      try
      {
        if (target == null)
          target = loader.loadClass(type).newInstance();
        return method.invoke(target, args);
      }
      catch (Exception ex)
      {
        return null;
      }
    }
  }
}

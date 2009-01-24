/* Copyright (C) 2003-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.event.*;
import buoy.widget.*;
import java.text.*;
import java.util.*;

import artofillusion.*;

/**
 * This class provides utilities for localizing text so that it can be translated into
 * different languages.  It does this by loading strings from a resource bundle, and
 * using them to create properly localized widgets.
 * <p>
 * The resource bundle is created from a {@link artofillusion.PluginRegistry.PluginResource PluginResource}
 * of type "TranslateBundle" provided by the {@link artofillusion.PluginRegistry PluginRegistry}.
 * By default it uses the PluginResource with ID "artofillusion" which is built into the application,
 * but you can specify a
 * different one by prefixing its ID to the property name passed to any method of this class.
 * This allows plugins to provide their own ResourceBundles for localizing their strings.  To do
 * this, the plugin should include a set of properties files that define the localized versions
 * of its strings, such as:
 * <p>
 * com/mycompany/myplugin.properties<br>
 * com/mycompany/myplugin_fr.properties
 * com/mycompany/myplugin_es.properties
 * <p>
 * In its extensions.xml file, it then provides a reference to these files:
 * <p>
 * &lt;resource type="TranslateBundle" id="myplugin" name="com.mycompany.myplugin"/&gt;
 * <p>
 * To look up keys from that bundle, prefix the key with the ID specified in the &lt;resource&gt;
 * tag:
 * <p>
 * BLabel instructions = Translate.label("myplugin:instructionsLabel");
 */

public class Translate
{
  private static Locale locale = Locale.getDefault();
  private static Map<String, ResourceBundle> bundles = new HashMap<String, ResourceBundle>();
  
  /** Set the locale to be used for generating text. */
  
  public static void setLocale(Locale l)
  {
    locale = l;
    bundles.clear();
  }
  
  /** Get the locale currently used for generating text. */
  
  public static Locale getLocale()
  {
    return locale;
  }
  
  /** Get a list of the locales for which we have translations. */
  
  public static Locale [] getAvailableLocales()
  {
    return new Locale [] {
      new Locale("af", "ZA"),
      Locale.SIMPLIFIED_CHINESE,
      new Locale("da", "DK"),
      new Locale("nl", "NL"),
      Locale.US,
      new Locale("fi", "FI"),
      Locale.FRENCH,
      Locale.GERMAN,
      Locale.ITALIAN,
      Locale.JAPANESE,
      new Locale("pt", "BR"),
      new Locale("es", "ES"),
      new Locale("sv", "SE"),
      new Locale("vi", "VN")
    };
  }

  /**
   * Look up the value corresponding to a resource key.
   *
   * @param key        the key specified by the user
   * @param prefix     an optional prefix to prepend to the key
   * @param suffix     an optional suffix to append to the key
   */

  private static String getValue(String key, String prefix, String suffix) throws MissingResourceException
  {
    String bundle;
    int colon = key.indexOf(':');
    if (colon == -1)
      bundle = "artofillusion";
    else
    {
      bundle = key.substring(0, colon);
      key = key.substring(colon+1);
    }
    if (prefix != null && suffix != null)
      key = prefix+key+suffix;
    else if (prefix != null)
      key = prefix+key;
    else if (suffix != null)
      key = key+suffix;
    ResourceBundle resources = bundles.get(bundle);
    if (resources == null)
    {
      PluginRegistry.PluginResource plugin = PluginRegistry.getResource("TranslateBundle", bundle);
      if (plugin == null)
        throw new MissingResourceException("No TranslateBundle defined", bundle, key);
      resources = ResourceBundle.getBundle(plugin.getName(), locale, plugin.getClassLoader());
      bundles.put(bundle, resources);
    }
    return resources.getString(key);
  }
  
  /** Get a BMenu whose text is given by the property "menu.(name)". */
  
  public static BMenu menu(String name)
  {
    String title = name;
    try
    {
      title = getValue(name, "menu.", null);
    }
    catch (MissingResourceException ex)
    {
    }
    BMenu menu = new BMenu(title);
    menu.setName(name);
    return menu;
  }
  
  /** Get a BMenuItem whose text is given by the property "menu.(name)".
      If listener is not null, the specified method of it will be added to the BMenuItem as an
      event link for CommandEvents, and the menu item's action command will be set to
      (name).  This also checks for a property called "menu.shortcut.(name)",
      and if it is found, sets the menu shortcut accordingly. */
  
  public static BMenuItem menuItem(String name, Object listener, String method)
  {
    String command = name;
    try
    {
      command = getValue(name, "menu.", null);
    }
    catch (MissingResourceException ex)
    {
    }
    BMenuItem item = new BMenuItem(command);
    item.setActionCommand(name);
    try
    {
      String shortcut = getValue(name, "menu.", ".shortcut");
      if (shortcut.length() > 1 && shortcut.charAt(0) == '^')
        item.setShortcut(new Shortcut(shortcut.charAt(1), Shortcut.DEFAULT_MASK|Shortcut.SHIFT_MASK));
      else if (shortcut.length() > 0)
        item.setShortcut(new Shortcut(shortcut.charAt(0)));
    }
    catch (MissingResourceException ex)
    {
    }
    if (listener != null)
      item.addEventLink(CommandEvent.class, listener, method);
    return item;
  }

  /** Get a BMenuItem whose text is given by the property "menu.(name)".
      If listener is not null, the specified method of it will be added to the BMenuItem as an
      event link for CommandEvents, and the menu item's action command will be set to
      (name).  This form of the method allows you to explicitly specify
      a menu shortcut, rather than using the one given in the properties
      file. */
  
  public static BMenuItem menuItem(String name, Object listener, String method, Shortcut shortcut)
  {
    String command = name;
    try
    {
      command = getValue(name, "menu.", null);
    }
    catch (MissingResourceException ex)
    {
    }
    BMenuItem item = new BMenuItem(command);
    item.setActionCommand(name);
    if (shortcut != null)
      item.setShortcut(shortcut);
    if (listener != null)
      item.addEventLink(CommandEvent.class, listener, method);
    return item;
  }

  /** Get a BCheckBoxMenuItem whose text is given by the property "menu.(name)".
      If listener is not null, the specified method of it will be added to the BCheckboxMenuItem as an
      event link for CommandEvents.  state specifies the initial state of the item. */
  
  public static BCheckBoxMenuItem checkboxMenuItem(String name, Object listener, String method, boolean state)
  {
    String command = name;
    try
    {
      command = getValue(name, "menu.", null);
    }
    catch (MissingResourceException ex)
    {
    }
    BCheckBoxMenuItem item = new BCheckBoxMenuItem(command, state);
    item.setActionCommand(name);
    try
    {
      String shortcut = getValue(name, "menu.", ".shortcut");
      if (shortcut.length() > 1 && shortcut.charAt(0) == '^')
        item.setShortcut(new Shortcut(shortcut.charAt(1), Shortcut.DEFAULT_MASK|Shortcut.SHIFT_MASK));
      else if (shortcut.length() > 0)
        item.setShortcut(new Shortcut(shortcut.charAt(0)));
    }
    catch (MissingResourceException ex)
    {
    }
    if (listener != null)
      item.addEventLink(CommandEvent.class, listener, method);
    return item;
  }

  /** Get a BButton whose text is given by the property "button.(name)".
      If listener is not null, the specified method of it will be added to the BButton as an
      event link for CommandEvents, and the menu item's action command will be set to
      (name). */
  
  public static BButton button(String name, Object listener, String method)
  {
    return button(name, null, listener, method);
  }

  /** Get a BButton whose text is given by the property "button.(name)", with a suffix
      appended to it. If listener is not null, the specified method of it will be added
      to the BButton as an event link for CommandEvents, and the button's action command
      will be set to (name). */
  
  public static BButton button(String name, String suffix, Object listener, String method)
  {
    String command = name;
    try
    {
      command = getValue(name, "button.", null);
    }
    catch (MissingResourceException ex)
    {
    }
    if (suffix != null)
      command += suffix;
    BButton b = new BButton(command);
    b.setActionCommand(name);
    if (listener != null)
      b.addEventLink(CommandEvent.class, listener, method);
    return b;
  }

  /** Get a BLabel whose text is given by the property "name".  If the
      property is not found, this simply uses name. */

  public static BLabel label(String name)
  {
    return label(name, null);
  }

  /** Get a BLabel whose text is given by the property "name", with a suffix appended
      to it.  If the property is not found, this simply uses name. */

  public static BLabel label(String name, String suffix)
  {
    try
    {
      name = getValue(name, null, null);
    }
    catch (MissingResourceException ex)
    {
    }
    if (suffix != null)
      name += suffix;
    return new BLabel(name);
  }

  /** Get the text given by the property "name".  If the property is not
      found, this simply returns name. */

  public static String text(String name)
  {
    try
    {
      return getValue(name, null, null);
    }
    catch (MissingResourceException ex)
    {
      return name;
    }
  }

  /** Get the text given by the property "name".  If the property is not
      found, this simply uses name.  Any occurrance of the pattern "{0}"
      in the text string will be replaced with the string representation
      of arg1. */

  public static String text(String name, Object arg1)
  {
    String pattern = name;
    try
    {
      pattern = getValue(name, null, null);
    }
    catch (MissingResourceException ex)
    {
    }
    return MessageFormat.format(pattern, arg1);
  }

  /** Get the text given by the property "name".  If the property is not
      found, this simply uses name.  Any occurrances of the patterns
      "{0}" and "{1}" in the text string will be replaced with the 
      strings representations of arg1 and arg2, respectively. */

  public static String text(String name, Object arg1, Object arg2)
  {
    String pattern = name;
    try
    {
      pattern = getValue(name, null, null);
    }
    catch (MissingResourceException ex)
    {
    }
    return MessageFormat.format(pattern, arg1, arg2);
  }

  /** Get the text given by the property "name".  If the property is not
      found, this simply uses name.  That string and the args array are
      then passed to MessageFormat.format() so that any variable fields
      can be replaced with the correct values. */

  public static String text(String name, Object args[])
  {
    String pattern = name;
    try
    {
      pattern = getValue(name, null, null);
    }
    catch (MissingResourceException ex)
    {
    }
    return MessageFormat.format(pattern, args);
  }
}

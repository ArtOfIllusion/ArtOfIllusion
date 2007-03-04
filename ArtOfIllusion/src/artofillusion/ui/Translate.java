/* Copyright (C) 2003-2004 by Peter Eastman

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

/** This class provides utilites for localizing text so that it can be
    translated into different languages. */

public class Translate
{
  private static Locale locale = Locale.getDefault();
  private static ResourceBundle resources;
  
  /** Set the locale to be used for generating text. */
  
  public static void setLocale(Locale l)
  {
    locale = l;
    resources = ResourceBundle.getBundle("artofillusion", locale);
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
      new Locale("da", "DA"),
      Locale.US,
      Locale.FRENCH,
      Locale.GERMAN,
      Locale.ITALIAN,
      Locale.JAPANESE,
      new Locale("pt", "BR"),
      new Locale("es", "ES"),
      new Locale("sv", "SE")
};
  }
  
  /** Get a BMenu whose text is given by the property "menu.(name)". */
  
  public static BMenu menu(String name)
  {
    try
    {
      return new BMenu(resources.getString("menu."+name));
    }
    catch (MissingResourceException ex)
    {
      return new BMenu(name);
    }
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
      command = resources.getString("menu."+name);
    }
    catch (MissingResourceException ex)
    {
    }
    BMenuItem item = new BMenuItem(command);
    item.setActionCommand(name);
    try
    {
      String shortcut = resources.getString("menu."+name+".shortcut");
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

  /**  Get a BMenuItem whose text is given by the property "menu.(name)".
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
      command = resources.getString("menu."+name);
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
      command = resources.getString("menu."+name);
    }
    catch (MissingResourceException ex)
    {
    }
    BCheckBoxMenuItem item = new BCheckBoxMenuItem(command, state);
    item.setActionCommand(name);
    try
    {
      String shortcut = resources.getString("menu."+name+".shortcut");
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
      command = resources.getString("button."+name);
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
      name = resources.getString(name);
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
      return resources.getString(name);
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
      pattern = resources.getString(name);
    }
    catch (MissingResourceException ex)
    {
    }
    return MessageFormat.format(pattern, new Object [] {arg1});
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
      pattern = resources.getString(name);
    }
    catch (MissingResourceException ex)
    {
    }
    return MessageFormat.format(pattern, new Object [] {arg1, arg2});
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
      pattern = resources.getString(name);
    }
    catch (MissingResourceException ex)
    {
    }
    return MessageFormat.format(pattern, args);
  }
}

/* Copyright (C) 2006-2013 by Peter Eastman
   Changes copyright (C) 2019 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.keystroke;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * This class contains information about a keyboard shortcut which automates some operation.
 * A keystroke pairs a key description (key code and modifier) with a Beanshell/Groovy script to execute
 * when the key is pressed.
 */

@XmlAccessorType(XmlAccessType.FIELD)
public class KeystrokeRecord
{
  @XmlAttribute private int keyCode;
  @XmlAttribute private int modifiers;

  @XmlAttribute public String name;
  @XmlValue private String script;
  @XmlAttribute private String language;

  public KeystrokeRecord() {
  }
  
  /**
   * Create a new KeystrokeRecord.
   *
   * @param keyCode   the key code (as defined by KeyEvent) for the key which activates this keystroke
   * @param modifiers the modifier keys which must be held down to activate this keystroke
   * @param name      a name to identify this keystroke
   * @param script    a script to execute when the keystroke is activated
   * @param language  the language in which the script is written
   */
  public KeystrokeRecord(int keyCode, int modifiers, String name, String script, String language)
  {
    this.keyCode = keyCode;
    this.modifiers = modifiers;
    this.name = name;
    this.script = script;
    this.language = language;
  }

  /**
   * Create a new KeystrokeRecord.  This constructor assumes the script is written in BeanShell, and exists
   * only for backward compatibility.
   *
   * @param keyCode   the key code (as defined by KeyEvent) for the key which activates this keystroke
   * @param modifiers the modifier keys which must be held down to activate this keystroke
   * @param name      a name to identify this keystroke
   * @param script    a BeanShell script to execute when the keystroke is activated
   */

  public KeystrokeRecord(int keyCode, int modifiers, String name, String script)
  {
    this(keyCode, modifiers, name, script, "BeanShell");
  }

  public int getKeyCode()
  {
    return keyCode;
  }

  public void setKeyCode(int keyCode)
  {
    this.keyCode = keyCode;
  }

  public int getModifiers()
  {
    return modifiers;
  }

  public void setModifiers(int modifiers)
  {
    this.modifiers = modifiers;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getScript()
  {
    return script;
  }

  public void setScript(String script)
  {
    this.script = script;
  }

  public String getLanguage()
  {
    return language;
  }

  public void setLanguage(String language)
  {
    this.language = language;
  }

  /**
   * Create an exact duplicate of this record.
   */

  public KeystrokeRecord duplicate()
  {
    return new KeystrokeRecord(keyCode, modifiers, name, script, language);
  }

    @Override
    public String toString() {
        return "KeystrokeRecord{" + "keyCode=" + keyCode + ", modifiers=" + modifiers + ", name=" + name + ", script=" + script + ", language=" + language + '}';
    }
  
  
}

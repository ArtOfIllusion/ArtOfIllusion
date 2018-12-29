/* Copyright (C) 2006-2013 by Peter Eastman
   Changes copyright (C) 2017-2019 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.keystroke;

import artofillusion.*;
import artofillusion.script.*;
import artofillusion.ui.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class maintains the list of keystrokes, and executes them in response to KeyEvents.
 */

public class KeystrokeManager
{
  private static Unmarshaller um = null;
  private static Marshaller ms = null;
  
  static {
    try
    {
      JAXBContext context = javax.xml.bind.JAXBContext.newInstance(KeystrokeRecords.class);
      um = context.createUnmarshaller();
      ms = context.createMarshaller();
    } catch (JAXBException ex)
    {
      System.out.println("Error creating XML marshaller/unmarshaller: " + ex);
    }

  }
  
  private static final KeystrokeRecords keystrokes = new KeystrokeRecords();
  private static Map<Integer, List<KeystrokeRecord>> keyIndex = new HashMap<Integer, List<KeystrokeRecord>>();

  private static final String KEYSTROKE_FILENAME = "keystrokes.xml";

  /**
   * Get an array of all defined KeystrokeRecords.
   */
  public static KeystrokeRecord[] getAllRecords()
  {
    return keystrokes.items.toArray(new KeystrokeRecord [keystrokes.items.size()]);    
  }

  /**
   * Set the list of all defined KeystrokeRecords, completely replacing the existing ones.
   */
  public static void setAllRecords(KeystrokeRecord allRecords[])
  {
    keystrokes.addAll(allRecords);
    recordModified();
  }

  /**
   * Add a new KeystrokeRecord.
   */
  public static void addRecord(KeystrokeRecord record)
  {
    keystrokes.items.add(record);
    recordModified();
  }

  /**
   * Remove a KeystrokeRecord.
   */
  public static void removeRecord(KeystrokeRecord record)
  {
    keystrokes.items.remove(record);
    recordModified();
  }

  /**
   * This should be called whenever a KeystrokeRecord has been modified.
   */
  public static void recordModified()
  {
    keyIndex = null;
  }

  /**
   * Given a key event, find any matching KeystrokeRecords and execute them.
   *
   * @param event     the KeyEvent which has occurred
   * @param window    the EditingWindow in which the event occurred
   */

  public static void executeKeystrokes(KeyEvent event, EditingWindow window)
  {
    if (keyIndex == null)
    {
      // We need to build an index for quickly looking up KeystrokeRecords.

      keyIndex = new HashMap<Integer, List<KeystrokeRecord>>(keystrokes.items.size());
      for (KeystrokeRecord record : keystrokes.items)
      {
        List<KeystrokeRecord> list = keyIndex.get(record.getKeyCode());
        if (list == null)
        {
          list = new ArrayList<KeystrokeRecord>(1);
          keyIndex.put(record.getKeyCode(), list);
        }
        list.add(record);
      }
    }

    // Get the list of all records with the correct ID.

    List<KeystrokeRecord> list = keyIndex.get(event.getKeyCode());
    if (list == null)
      return;
    
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("window", window);
    for (KeystrokeRecord record : list)
    {
      if (record.getModifiers() == event.getModifiers())
      {
        // Execute it.
        ScriptRunner.executeScript(record.getLanguage(), record.getScript(), variables);
        event.consume();
      }
    }
  }

  /**
   * Locate the file containing keystroke definitions and load them.
   */
  public static void loadRecords()
  {
    try
    {
      Path keys = Paths.get(ApplicationPreferences.getPreferencesDirectory().getPath()).resolve(KEYSTROKE_FILENAME);
      InputStream in;
      if(Files.exists(keys))
        in = new BufferedInputStream(Files.newInputStream(keys));
      else 
        in = KeystrokeManager.class.getResourceAsStream("/" + KEYSTROKE_FILENAME);

      addRecordsFromXML(in);
      in.close();

    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  /**
   * Read an XML file from an InputStream and add all the keystrokes it contains.  For each one,
   * it checks whether there was already an existing keystroke with the same name.  If so, the
   * new keystroke replaces the old one.  If not, the new keystroke is simply added.
   */
  public static void addRecordsFromXML(InputStream in) throws Exception
  {
    // Build a table of existing records.

    HashMap<String, KeystrokeRecord> existing = new HashMap<String, KeystrokeRecord>();
    keystrokes.items.forEach((record) -> { existing.put(record.getName(), record); });

    // Parse the XML and load the records.
    KeystrokeRecords kr = (KeystrokeRecords)um.unmarshal(in);    
    for(KeystrokeRecord record: kr.items)
    {
        if(existing.containsKey(record.getName())) keystrokes.items.remove(existing.get(record.getName()));
        addRecord(record);
    }
  }

  /**
   * Save the list of keystrokes to an XML file.
   */
  public static void saveRecords() throws Exception
  {
      ms.marshal(keystrokes, Paths.get(ApplicationPreferences.getPreferencesDirectory().getPath()).resolve(KEYSTROKE_FILENAME).toFile());
  }
  
    @XmlRootElement(name = "keystrokes")
    public static class KeystrokeRecords {
        @XmlElement(name="keystroke") List<KeystrokeRecord> items = new ArrayList<KeystrokeRecord>();
        
        public void addAll(KeystrokeRecord... elements)
        {
            items.clear();
            Collections.addAll(items, elements);
        }
    }
}

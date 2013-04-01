/* Copyright (C) 2006-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.keystroke;

import artofillusion.ui.*;
import artofillusion.script.*;
import artofillusion.*;

import java.util.*;
import java.awt.event.*;
import java.io.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;
import org.w3c.dom.Node;

/**
 * This class maintains the list of keystrokes, and executes them in response to KeyEvents.
 */

public class KeystrokeManager
{
  private static ArrayList<KeystrokeRecord> records = new ArrayList<KeystrokeRecord>();
  private static HashMap<Integer, ArrayList<KeystrokeRecord>> keyIndex = new HashMap<Integer, ArrayList<KeystrokeRecord>>();

  private static final String KEYSTROKE_FILENAME = "keystrokes.xml";

  /**
   * Get a list of all defined KeystrokeRecords.
   */

  public static KeystrokeRecord[] getAllRecords()
  {
    return records.toArray(new KeystrokeRecord [records.size()]);
  }

  /**
   * Set the list of all defined KeystrokeRecords, completely replacing the existing ones.
   */

  public static void setAllRecords(KeystrokeRecord allRecords[])
  {
    records.clear();
    Collections.addAll(records, allRecords);
    recordModified();
  }

  /**
   * Add a new KeystrokeRecord.
   */

  public static void addRecord(KeystrokeRecord record)
  {
    records.add(record);
    recordModified();
  }

  /**
   * Remove a KeystrokeRecord.
   */

  public static void removeRecord(KeystrokeRecord record)
  {
    records.remove(record);
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

      keyIndex = new HashMap<Integer, ArrayList<KeystrokeRecord>>(records.size());
      for (KeystrokeRecord record : records)
      {
        ArrayList<KeystrokeRecord> list = keyIndex.get(record.getKeyCode());
        if (list == null)
        {
          list = new ArrayList<KeystrokeRecord>(1);
          keyIndex.put(record.getKeyCode(), list);
        }
        list.add(record);
      }
    }

    // Get the list of all records with the correct ID.

    ArrayList<KeystrokeRecord> list = keyIndex.get(event.getKeyCode());
    if (list == null)
      return;
    for (KeystrokeRecord record : list)
    {
      if (record.getModifiers() == event.getModifiers())
      {
        // Execute it.

        HashMap<String, Object> variables = new HashMap<String, Object>();
        variables.put("window", window);
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
      File dir = ApplicationPreferences.getPreferencesDirectory();
      File inputFile = new File(dir, KEYSTROKE_FILENAME);
      InputStream in;
      if (inputFile.exists())
        in = new BufferedInputStream(new FileInputStream(inputFile));
      else
        in = KeystrokeManager.class.getResourceAsStream("/"+KEYSTROKE_FILENAME);
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
    for (KeystrokeRecord record : records)
      existing.put(record.getName(), record);

    // Parse the XML and load the records.

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(in);
    NodeList keystrokes = doc.getElementsByTagName("keystroke");
    for (int i = 0; i < keystrokes.getLength(); i++)
    {
      Node keystroke = keystrokes.item(i);
      String name = keystroke.getAttributes().getNamedItem("name").getNodeValue();
      String language = (keystroke.getAttributes().getNamedItem("language") == null ? "BeanShell" : keystroke.getAttributes().getNamedItem("language").getNodeValue());
      int code = Integer.parseInt(keystroke.getAttributes().getNamedItem("code").getNodeValue());
      int modifiers = Integer.parseInt(keystroke.getAttributes().getNamedItem("modifiers").getNodeValue());
      String script = keystroke.getFirstChild().getNodeValue();
      if (existing.containsKey(name))
        records.remove(existing.get(name));
      addRecord(new KeystrokeRecord(code, modifiers, name, script, language));
    }
  }

  /**
   * Save the list of keystrokes to an XML file.
   */

  public static void saveRecords() throws Exception
  {
    // Construct the XML.

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();
    Element root = doc.createElement("keystrokes");
    doc.appendChild(root);
    for (KeystrokeRecord record : records)
    {
      Element recordElement = doc.createElement("keystroke");
      recordElement.setAttribute("name", record.getName());
      recordElement.setAttribute("language", record.getLanguage());
      recordElement.setAttribute("code", Integer.toString(record.getKeyCode()));
      recordElement.setAttribute("modifiers", Integer.toString(record.getModifiers()));
      Text scriptElement = doc.createTextNode(record.getScript());
      recordElement.appendChild(scriptElement);
      root.appendChild(recordElement);
    }

    // Save it to disk.

    File dir = ApplicationPreferences.getPreferencesDirectory();
    File outFile = new File(dir, KEYSTROKE_FILENAME);
    OutputStream out = new BufferedOutputStream(new SafeFileOutputStream(outFile, SafeFileOutputStream.OVERWRITE));
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(out);
    TransformerFactory transFactory = TransformerFactory.newInstance();
    Transformer transformer = transFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.transform(source, result);
    out.close();
  }
}

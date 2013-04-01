/* Copyright (C) 2002-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.script;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This class presents a user interface for entering scripts to be executed. */

public class ExecuteScriptWindow extends BFrame
{
  private LayoutWindow window;
  private ScriptEditor scriptText;
  private BComboBox languageChoice;
  private String scriptName;

  private static File scriptDir;
  private static String lastScript;
  private static String lastScriptName = "Untitled";
  
  public ExecuteScriptWindow(LayoutWindow win)
  {
    super(lastScriptName);
    scriptName = lastScriptName;
    BorderContainer content = new BorderContainer();
    setContent(content);
    window = win;
    if (scriptDir == null)
      scriptDir = new File(ArtOfIllusion.TOOL_SCRIPT_DIRECTORY);
    scriptText = new ScriptEditor("");
    if (lastScript != null)
      scriptText.setText(lastScript);
    content.add(scriptText.createContainer(), BorderContainer.CENTER);
    languageChoice = new BComboBox(ScriptRunner.LANGUAGES);
    RowContainer languageRow = new RowContainer();
    languageRow.add(Translate.label("language"));
    languageRow.add(languageChoice);
    content.add(languageRow, BorderContainer.NORTH, new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE));
    RowContainer buttons = new RowContainer();
    content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
    buttons.add(Translate.button("executeScript", this, "executeScript"));
    buttons.add(Translate.button("Load", "...", this, "loadScript"));
    buttons.add(Translate.button("Save", "...", this, "saveScript"));
    buttons.add(Translate.button("close", this, "closeWindow"));
    addEventLink(WindowClosingEvent.class, this, "closeWindow");
    scriptText.setCaretPosition(0);
    pack();
    UIUtilities.centerWindow(this);
    scriptText.requestFocus();
    setVisible(true);
  }
  
  private void closeWindow()
  {
    lastScript = scriptText.getText();
    dispose();
  }
  
  /** Prompt the user to load a script. */
  
  private void loadScript()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("selectScriptToLoad"));
    fc.setDirectory(scriptDir);
    fc.showDialog(this);
    if (fc.getSelectedFile() == null)
      return;
    scriptDir = fc.getDirectory();
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    File f = fc.getSelectedFile();
    try
    {
      BufferedReader in = new BufferedReader(new FileReader(f));
      StringBuffer buf = new StringBuffer();
      int c;
      while ((c = in.read()) != -1)
        buf.append((char) c);
      in.close();
      scriptText.setText(buf.toString());
      scriptText.setCaretPosition(0);
    }
    catch (Exception ex)
    {
      new BStandardDialog(null, new String [] {Translate.text("errorReadingScript"),
        ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
    }
    String filename = fc.getSelectedFile().getName();
    try
    {
      languageChoice.setSelectedValue(ScriptRunner.getLanguageForFilename(filename));
    }
    catch (IllegalArgumentException ex)
    {
      languageChoice.setSelectedValue(ScriptRunner.LANGUAGES[0]);
    }
    setScriptNameFromFile(filename);
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
  
  /** Prompt the user to save a script. */
  
  private void saveScript()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("saveScriptToFile"));
    fc.setDirectory(scriptDir);
    fc.setSelectedFile(new File(scriptDir, scriptName+'.'+ScriptRunner.getFilenameExtension((String) languageChoice.getSelectedValue())));
    fc.showDialog(this);
    if (fc.getSelectedFile() == null)
      return;
    scriptDir = fc.getDirectory();
    
    // Write the script to disk.
    
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    File f = fc.getSelectedFile();
    try
    {
      BufferedWriter out = new BufferedWriter(new FileWriter(f));
      out.write(scriptText.getText().toCharArray());
      out.close();
    }
    catch (Exception ex)
    {
      new BStandardDialog(null, new String [] {Translate.text("errorWritingScript"),
        ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
    }
    setScriptNameFromFile(fc.getSelectedFile().getName());

    // Update the Scripts menus in all windows.
    
    EditingWindow allWindows[] = ArtOfIllusion.getWindows();
    for (int i = 0; i < allWindows.length; i++)
      if (allWindows[i] instanceof LayoutWindow)
        ((LayoutWindow) allWindows[i]).rebuildScriptsMenu();
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /** Set the script name based on the name of a file that was loaded or saved. */

  private void setScriptNameFromFile(String filename)
  {
    if (filename.contains("."))
      scriptName = filename.substring(0, filename.lastIndexOf("."));
    else
      scriptName = filename;
    lastScriptName = scriptName;
    setTitle(scriptName);
  }
  
  /** Execute the script. */
  
  private void executeScript()
  {
    String language = (String) languageChoice.getSelectedValue();
    try
    {
      ToolScript script = ScriptRunner.parseToolScript(language, scriptText.getText());
      script.execute(window);
    }
    catch (Exception e)
    {
      int line = ScriptRunner.displayError(language, e);
      if (line > -1)
        {
          // Find the start of the line containing the error.
          
          String text = scriptText.getText();
          int index = 0;
          for (int i = 0; i < line-1; i++)
            {
              int next = text.indexOf('\n', index);
              if (next == -1)
                {
                  index = -1;
                  break;
                }
              index = next+1;
            }
          if (index > -1)
            scriptText.setCaretPosition(index);
          scriptText.requestFocus();
        }
    }
    window.updateImage();
    scriptText.requestFocus();
  }
}

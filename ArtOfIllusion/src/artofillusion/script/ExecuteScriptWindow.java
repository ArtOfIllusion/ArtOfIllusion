/* Copyright (C) 2002-2013 by Peter Eastman
   Changes copyright (C) 2020 by Maksim Khramov

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
import java.io.*;

/** This class presents a user interface for entering scripts to be executed. */

public final class ExecuteScriptWindow extends ScriptWindowBase
{
  private final LayoutWindow window;

  private static File scriptDir;
  private static String lastScript;
  private static String lastScriptName = "Untitled";

  public ExecuteScriptWindow(LayoutWindow win)
  {
    super(lastScriptName);
    scriptName = lastScriptName;

    window = win;
    if (scriptDir == null) scriptDir = new File(ArtOfIllusion.TOOL_SCRIPT_DIRECTORY);
    
    if (lastScript != null) scriptText.setText(lastScript);
    scriptText.setCaretPosition(0);

    getButtons().add(Translate.button("executeScript", this, "executeScript"));
    getButtons().add(Translate.button("Load", "...", this, "loadScript"));
    getButtons().add(Translate.button("Save", "...", this, "saveScript"));
    getButtons().add(Translate.button("close", this, "closeWindow"));
    
    addEventLink(WindowClosingEvent.class, this, "closeWindow");
    
    pack();
    this.getComponent().setLocationRelativeTo(null);
    setVisible(true);
    scriptText.requestFocus();
  }

  @Override
  protected void setScriptFolder(File target)
  {
    scriptDir = target;
  }

  @Override
  protected File getScriptFolder()
  {
    return scriptDir;
  }


  private void closeWindow()
  {
    lastScript = scriptText.getText();
    dispose();
  }


  /** Prompt the user to save a script. */

  @Override
  protected void saveScript()
  {
    super.saveScript();

    // Update the Scripts menus in all windows.
    for (EditingWindow editingWindow : ArtOfIllusion.getWindows())
      if (editingWindow instanceof LayoutWindow)
        ((LayoutWindow) editingWindow).rebuildScriptsMenu();
    
  }

  /** Set the script name based on the name of a file that was loaded or saved. */

  @Override
  protected void setScriptNameFromFile(String filename)
  {
    super.setScriptNameFromFile(filename);
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

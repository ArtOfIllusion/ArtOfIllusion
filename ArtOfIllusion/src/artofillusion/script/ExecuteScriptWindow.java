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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rtextarea.RTextScrollPane;

/** This class presents a user interface for entering scripts to be executed. */

public class ExecuteScriptWindow extends BFrame
{
  private LayoutWindow window;
  private RSyntaxTextArea scriptText;
  private BComboBox languageChoice;
  // TODO Translate this?
  public static final String NEW_SCRIPT_NAME = "Untitled";
  private String scriptPath;

  // TODO should this be static? It's actually the last directory used for scripts, 
  // is there a reason it is shared among all editing windows? 
  private static File scriptDir= new File(ArtOfIllusion.TOOL_SCRIPT_DIRECTORY);
  public static final Set<String> RECENT_SCRIPTS = new LinkedHashSet<String> ();
  private String language;
    private final BButton save;
    private final BButton executeSelected;
    private final BButton executeToCursor;
    private static final int EDITORS_OFFSET = 32;
    private static ArrayList <String> openedScripts = new ArrayList<String> ();
    
    /** This is used to track the "changed" status of the script being edited. */
    private class ScriptKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {
            save.setEnabled(true);        
        }

        @Override
        public void keyPressed(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
        
    }
    
    /**
     * 
     * @param win
     * @param scriptAbsolutePath ExecuteScriptWindow.NEW_SCRIPT_NAME if this is a new script
     * @param scriptLanguage May be null (scriptLanguage unknown) if this is a new script
     */
  public ExecuteScriptWindow(LayoutWindow win, String scriptAbsolutePath, String scriptLanguage)
  {
    super(scriptAbsolutePath);
    setScriptNameFromFile(scriptAbsolutePath);
    language = scriptLanguage;
    scriptPath = scriptAbsolutePath;

    // TODO Add output panel and an error panel ; 
    // TODO make sure the scripts outputs are redirected to those
    BorderContainer content = new BorderContainer();
    setContent(content);
    window = win;
    String editorTextContent = "";
    if (scriptLanguage != null && scriptAbsolutePath.contains(".")) {
        try {
            editorTextContent = ArtOfIllusion.loadFile(new File (scriptAbsolutePath));
        } catch (IOException ex) {
            Logger.getLogger(ExecuteScriptWindow.class.getName()).log(Level.SEVERE, null, ex);
            // TODO FIXME Disable editing and saving since loading has failed
            // TODO display a dialog box error explaining this
        }
    }
    scriptText = new RSyntaxTextArea(editorTextContent, 25, 100);
    scriptText.addKeyListener(new ScriptKeyListener());
    SyntaxScheme scheme = scriptText.getSyntaxScheme();
    Style style = scheme.getStyle(SyntaxScheme.COMMENT_EOL);
    Style newStyle = new Style(style.foreground, style.background, style.font.deriveFont(Font.PLAIN));
    scheme.setStyle(SyntaxScheme.COMMENT_EOL, newStyle);
    scheme.setStyle(SyntaxScheme.COMMENT_MULTILINE, newStyle);

    scriptText.setAnimateBracketMatching(false);
    scriptText.setTabSize(2);
    scriptText.setCodeFoldingEnabled(true);
    content.add(new AWTWidget(new RTextScrollPane(scriptText))
               , BorderContainer.CENTER);
    languageChoice = new BComboBox(ScriptRunner.LANGUAGES);
    BorderContainer tools = new BorderContainer ();
    content.add(tools, BorderContainer.NORTH);
    RowContainer buttons = new RowContainer();
    buttons.add(Translate.button("load", "...", this, "loadScript"));
    buttons.add(Translate.button("saveAs", "...", this, "saveScriptAs"));
    buttons.add(save = Translate.button("save", "", this, "saveScript"));
    save.setEnabled(false);

    tools.add(buttons, BorderContainer.WEST, new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE));

    // another center row for the "execute selected" and verious debugging items
    RowContainer debugTools = new RowContainer();
    debugTools.add(Translate.button("executeScript", this, "executeScript"));
    debugTools.add(executeToCursor = Translate.button("executeToCursor", this, "executeToCursor"));
    debugTools.add(executeSelected = Translate.button("executeSelected", this, "executeSelected"));
    
    tools.add(debugTools, BorderContainer.CENTER, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE));
    
    RowContainer languageRow = new RowContainer();
    languageRow.add(Translate.label("language"));
    languageRow.add(languageChoice);
    if (scriptLanguage != null) {
        languageChoice.setSelectedValue(scriptLanguage);
        languageChoice.setEnabled(false);
    }
    tools.add(languageRow, BorderContainer.EAST, new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE));
    //buttons.add(Translate.button("close", this, "closeWindow"));
    addEventLink(WindowClosingEvent.class, this, "closeWindow");
    languageChoice.addEventLink(ValueChangedEvent.class, this, "updateLanguage");
    scriptText.setCaretPosition(0);
    pack();
    updateLanguage();
    UIUtilities.centerWindow(this);
    // We add an offset to every window so one does not exactly hide the others
    int editorFrameOffset = EDITORS_OFFSET*openedScripts.size();
    setBounds(new Rectangle(this.getBounds().x + editorFrameOffset, this.getBounds().y + editorFrameOffset, 
            this.getBounds().width, this.getBounds().height));
    scriptText.requestFocus();
    setVisible(true);
    updateEditableStatus(NEW_SCRIPT_NAME, scriptAbsolutePath);
  }

    private void updateEditableStatus(String previousScriptAbsoluePath, String scriptAbsolutePath) {
        if (!previousScriptAbsoluePath.equals(scriptAbsolutePath)) {    
            if (openedScripts.contains(scriptAbsolutePath))
            {
                scriptText.setEditable(false);
                scriptText.setEnabled(false);
                scriptText.setBackground(Color.LIGHT_GRAY);
                new BStandardDialog(null, new String [] {Translate.text("alreadyOpenedScript"),
                    "This window is read-only : this script is open in other window(s) " + scriptAbsolutePath}, BStandardDialog.ERROR).showMessageDialog(this);
           }
            else
            {
                scriptText.setEditable(true);
                scriptText.setEnabled(true);
                scriptText.setBackground(Color.WHITE);
            }
            openedScripts.remove(previousScriptAbsoluePath);
            openedScripts.add(scriptAbsolutePath);
        }
    }

  /** Make syntax highlighting match current scripting language */

  private void updateLanguage()
  {
    scriptText.setSyntaxEditingStyle(
        ScriptRunner.LANGUAGES[1].equalsIgnoreCase(language) ?
          SyntaxConstants.SYNTAX_STYLE_GROOVY : SyntaxConstants.SYNTAX_STYLE_JAVA);
  }

  private void closeWindow()
  {
    // TODO Warning message if the script hasn't been saved
    dispose();
    openedScripts.remove(scriptPath);
  }

  /** Prompt the user to load a script. */

  private void loadScript()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("selectScriptToLoad"));
    // Save the current program working directory
    File workingDir = fc.getDirectory();
    fc.setDirectory(scriptDir);
    fc.showDialog(this);
    if (fc.getSelectedFile() != null)
    {
      scriptDir = fc.getDirectory();
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      File f = fc.getSelectedFile();
      try
      {
        scriptText.setText(ArtOfIllusion.loadFile(f));
        updateEditableStatus(scriptPath, fc.getSelectedFile().getAbsolutePath());
        scriptPath = fc.getSelectedFile().getAbsolutePath();
        scriptText.setCaretPosition(0);
        String filename = fc.getSelectedFile().getName();
        String fileLanguage = ScriptRunner.getLanguageForFilename(filename);
        if (fileLanguage != null)
        {
            languageChoice.setSelectedValue(fileLanguage);
            languageChoice.setEnabled(false);
            setScriptNameFromFile(fc.getSelectedFile().getAbsolutePath());
            for (EditingWindow edWindow: ArtOfIllusion.getWindows()) {
                if (edWindow instanceof LayoutWindow)
                {
                    ((LayoutWindow) edWindow).rebuildRecentScriptsMenu();
                }
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            updateLanguage();
            // disable the "Save" button, 
            // to be re-enabled as soon as the text changes
            save.setEnabled(false);
        }
        else
        {
            // TODO translate
            new BStandardDialog(null, new String [] {Translate.text("errorReadingScript"),
              "Unsupported file extension for " + filename}, BStandardDialog.ERROR).showMessageDialog(this);
        }
      }
      catch (Exception ex)
      {
        new BStandardDialog(null, new String [] {Translate.text("errorReadingScript"),
          ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
      }
    }
    // Restore program working directory for other filechoosers
    fc.setDirectory(workingDir);
  }

 /** Prompt the user to save a script. */

  private void saveScriptAs()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("saveScriptToFile"));
    // TODO FIXME Est-ce que ces manipulations de répertoires sont vraiment nécessaires?
    // Save current program working directory
    File workingDir = fc.getDirectory();
    fc.setDirectory(scriptDir);
    if (language == null) 
        language = (String) languageChoice.getSelectedValue();
    fc.setSelectedFile(new File(scriptDir, scriptPath/*+'.'+
            ScriptRunner.getFilenameExtension(language)*/));
    fc.showDialog(this);
    if (fc.getSelectedFile() != null)
    {
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
      // Now we have saved, we can't change the language
      languageChoice.setEnabled(false);
      updateEditableStatus(scriptPath, fc.getSelectedFile().getAbsolutePath());
      scriptPath = fc.getSelectedFile().getAbsolutePath();

      setScriptNameFromFile(fc.getSelectedFile().getAbsolutePath());
      // Update the Scripts menus in all windows.
        for (EditingWindow edWin : ArtOfIllusion.getWindows())
        {
            if (edWin instanceof LayoutWindow)
            {
                ((LayoutWindow) edWin).rebuildScriptsMenu();
            }
        }
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    save.setEnabled(false);
    // Restore program working directory
    fc.setDirectory(workingDir);
 }

  /** Save the current script to its current file path, without user input. 
   */
  private void saveScript()
  {
    // Write the script to disk.
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    File f = new File (scriptPath);
    try
    {
      BufferedWriter out = new BufferedWriter(new FileWriter(f));
      out.write(scriptText.getText().toCharArray());
      out.close();
    }
    catch (Exception ex)
    {
      new BStandardDialog(null, new String [] {Translate.text("errorWritingScript"),
        scriptPath + (ex.getMessage() == null ? "" : ex.getMessage())}, BStandardDialog.ERROR).showMessageDialog(this);
    }
    // Now we have saved, we can't change the language
    languageChoice.setEnabled(false);
    save.setEnabled(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
 }


  /** Set the script name based on the name of a file that was loaded or saved. 
   @param filePath NEW_SCRIPT_NAME or an absolute file path
   */
  private void setScriptNameFromFile(String filePath)
  {
      /*
    if (filePath.contains("."))
      scriptPath = filePath.substring(0, filePath.lastIndexOf("."));
    else
      scriptPath = filePath;
      */
    if (filePath != NEW_SCRIPT_NAME)
        RECENT_SCRIPTS.add(filePath);
    setTitle(filePath);
  }

  private void executeSelected()
  {
    executeText(scriptText.getSelectedText());
    window.updateImage();
    scriptText.requestFocus();
  }
  
  private void executeToCursor() 
  {
    final String substringAfterCaret = scriptText.getText().substring(scriptText.getCaretPosition());
    int charactersUntilEndOfLine = substringAfterCaret.indexOf("\n");
    if (charactersUntilEndOfLine == -1)
        charactersUntilEndOfLine = substringAfterCaret.length();
    final String textToEndOfCaretLine = scriptText.getText().substring(
            0, scriptText.getCaretPosition() + charactersUntilEndOfLine);
    executeText(textToEndOfCaretLine);
    window.updateImage();
    scriptText.requestFocus();
  }
  
          /** Execute the script. */

  private void executeScript()
  {
    executeText(scriptText.getText());
    window.updateImage();
    scriptText.requestFocus();
  }

    public void executeText(final String text) {
        try
        {
            ToolScript script = ScriptRunner.parseToolScript(language, text);
            script.execute(window);
        }
        catch (Exception e)
        {
            int line = ScriptRunner.displayError(language, e);
            if (line > -1)
            {
                // Find the start of the line containing the error.
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
    }
}

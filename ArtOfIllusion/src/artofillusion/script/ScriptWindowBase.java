/* Copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.script;

import artofillusion.ui.Translate;
import buoy.widget.*;
import java.awt.Cursor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 *
 * @author MaksK
 */
class ScriptWindowBase extends BFrame
{
  protected final RSyntaxTextArea scriptText;
  
  protected String scriptName;
  
  protected BComboBox languageChoice;
  private final RowContainer buttons = new RowContainer();
  
  private BMenu editMenu;

  public ScriptWindowBase(String title)
  {
    super(title);
    scriptText = new RSyntaxTextArea("", 25, 80);
    scriptText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
    scriptText.setCodeFoldingEnabled(true);
    
    scriptText.setTabSize(3);

    languageChoice = new BComboBox(ScriptRunner.LANGUAGES);

    setContent(new BorderContainer());
    getContent().add(new AWTWidget(new RTextScrollPane(scriptText)), BorderContainer.CENTER);

    RowContainer languageRow = new RowContainer();
    languageRow.add(Translate.label("language"));
    languageRow.add(languageChoice);
    getContent().add(languageRow, BorderContainer.NORTH, new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE));
    getContent().add(buttons, BorderContainer.SOUTH, new LayoutInfo());

    
    setMenuBar(new BMenuBar());
    createEditorMenu();
  }

  private void createEditorMenu() {
    editMenu = Translate.menu("edit");
    editMenu.getComponent().add(new JMenuItem(RSyntaxTextArea.getAction(RSyntaxTextArea.UNDO_ACTION)));
    editMenu.getComponent().add(new JMenuItem(RSyntaxTextArea.getAction(RSyntaxTextArea.REDO_ACTION)));
    editMenu.getComponent().add(new JMenuItem(RSyntaxTextArea.getAction(RSyntaxTextArea.CUT_ACTION)));
    editMenu.getComponent().add(new JMenuItem(RSyntaxTextArea.getAction(RSyntaxTextArea.COPY_ACTION)));
    editMenu.getComponent().add(new JMenuItem(RSyntaxTextArea.getAction(RSyntaxTextArea.PASTE_ACTION)));
    editMenu.getComponent().add(new JMenuItem(RSyntaxTextArea.getAction(RSyntaxTextArea.DELETE_ACTION)));
    editMenu.getComponent().add(new JMenuItem(RSyntaxTextArea.getAction(RSyntaxTextArea.SELECT_ALL_ACTION)));
    this.getMenuBar().add(editMenu);
  }


  @Override
  public final BorderContainer getContent()
  {
    return (BorderContainer)content;
  }

  protected final RowContainer getButtons() {
    return buttons;
  }


  /** Set the script name based on the name of a file that was loaded or saved.
   * @param filename */

  protected void setScriptNameFromFile(String filename)
  {
    if (filename.contains("."))
      scriptName = filename.substring(0, filename.lastIndexOf("."));
    else
      scriptName = filename;
  }

  protected File getScriptFolder() {
    return null;
  }

  protected void setScriptFolder(File target) {
  }

  private static final FileNameExtensionFilter groovyScriptExtension = new FileNameExtensionFilter("Groovy Script", "groovy");
  private static final FileNameExtensionFilter beanShellScriptExtension = new FileNameExtensionFilter("Bean Shell Script", "bsh");
  /**
   * Prompt the user to load a script.
   */
  protected final void loadScript()
  {

    BFileChooser chooser = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("selectScriptToLoad"));
    
    chooser.getComponent().addChoosableFileFilter(beanShellScriptExtension);
    chooser.getComponent().addChoosableFileFilter(groovyScriptExtension);
    chooser.setFileFilter(beanShellScriptExtension);
    
    chooser.setDirectory(getScriptFolder());
    chooser.showDialog(this);
    if (chooser.getSelectedFile() == null)
      return;
    setScriptFolder(chooser.getDirectory());

    try
    {
      scriptText.setText(Files.lines(chooser.getSelectedFile().toPath()).collect(Collectors.joining(System.lineSeparator())));
      scriptText.setCaretPosition(0);
    }
    catch (IOException ioe)
    {
      String reason = ioe.getMessage() == null ? "" : ioe.getMessage();
      JOptionPane.showMessageDialog(this.component, Translate.text("errorReadingScript") + reason, "Art Of Illusion", JOptionPane.ERROR_MESSAGE);
    }
    String filename = chooser.getSelectedFile().getName();
    try
    {
      languageChoice.setSelectedValue(ScriptRunner.getLanguageForFilename(filename));
    }
    catch (IllegalArgumentException ex)
    {
      languageChoice.setSelectedValue(ScriptRunner.LANGUAGES[0]);
    }
    setScriptNameFromFile(filename);
  }


  /** Prompt the user to save a script. */

  protected void saveScript()
  {
    BFileChooser chooser = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("saveScriptToFile"));
    chooser.setDirectory(getScriptFolder());
    chooser.setSelectedFile(new File(getScriptFolder(), scriptName + '.' + ScriptRunner.getFilenameExtension((String) languageChoice.getSelectedValue())));
    chooser.showDialog(this);
    if (chooser.getSelectedFile() == null)
      return;
    setScriptFolder(chooser.getDirectory());

    // Write the script to disk.
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    try (BufferedWriter out = Files.newBufferedWriter(chooser.getSelectedFile().toPath()))
    {
      out.write(scriptText.getText().toCharArray());
    }
    catch (IOException ioe)
    {
      String reason = ioe.getMessage() == null ? "" : ioe.getMessage();
      JOptionPane.showMessageDialog(this.component, Translate.text("errorWritingScript") + reason, "Art Of Illusion", JOptionPane.ERROR_MESSAGE);
    }
    setScriptNameFromFile(chooser.getSelectedFile().getName());
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }


}

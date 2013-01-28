/* Copyright (C) 2002-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.script;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This class presents a user interface for entering object scripts. */

public class ScriptedObjectEditorWindow extends BFrame
{
  private EditingWindow window;
  private ObjectInfo info;
  private ScriptEditor scriptText;
  private BComboBox languageChoice;
  private String scriptName;
  private Runnable onClose;
  
  private static File scriptDir;
  
  public ScriptedObjectEditorWindow(EditingWindow parent, ObjectInfo obj, Runnable onClose)
  {
    super("Script '"+ obj.getName() +"'");
    window = parent;
    info = obj;
    this.onClose = onClose;
    scriptName = "Untitled";
    if (scriptDir == null)
      scriptDir = new File(ArtOfIllusion.OBJECT_SCRIPT_DIRECTORY);
    BorderContainer content = new BorderContainer();
    setContent(content);
    scriptText = new ScriptEditor(((ScriptedObject) info.getObject()).getScript());
    languageChoice = new BComboBox(ScriptRunner.LANGUAGES);
    languageChoice.setSelectedValue(((ScriptedObject) info.getObject()).getLanguage());
    RowContainer languageRow = new RowContainer();
    languageRow.add(Translate.label("language"));
    languageRow.add(languageChoice);
    content.add(languageRow, BorderContainer.NORTH, new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE));
    content.add(BOutline.createBevelBorder(scriptText.createContainer(), false), BorderContainer.CENTER);
    RowContainer buttons = new RowContainer();
    content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
    buttons.add(Translate.button("ok", this, "commitChanges"));
    buttons.add(Translate.button("Load", "...", this, "loadScript"));
    buttons.add(Translate.button("Save", "...", this, "saveScript"));
    buttons.add(Translate.button("scriptParameters", this, "editParameters"));
    buttons.add(Translate.button("cancel", this, "dispose"));
    addEventLink(WindowClosingEvent.class, this, "commitChanges");
    scriptText.setCaretPosition(0);
    pack();
    UIUtilities.centerWindow(this);
    scriptText.requestFocus();
    setVisible(true);
  }
  
  /** Display a dialog for editing the parameters. */
  
  private void editParameters()
  {
    new ParametersDialog();
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
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /** Set the script name based on the name of a file that was loaded or saved. */

  private void setScriptNameFromFile(String filename)
  {
    if (filename.contains("."))
      scriptName = filename.substring(0, filename.lastIndexOf("."));
    else
      scriptName = filename;
  }

  /** Commit changes to the scripted object. */
  
  private void commitChanges()
  {
    ScriptedObject so = (ScriptedObject) info.getObject();
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    so.setScript(scriptText.getText());
    so.setLanguage(languageChoice.getSelectedValue().toString());
    so.sceneChanged(info, window.getScene());
    if (onClose != null)
      onClose.run();
    window.updateImage();
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    dispose();
  }

  /** This is an inner class for editing the list of parameters on the object. */
  
  private class ParametersDialog extends BDialog
  {
    private ScriptedObject script;
    private BList paramList;
    private BTextField nameField;
    private ValueField valueField;
    private String name[];
    private double value[];
    private int current;
    
    public ParametersDialog()
    {
      super(ScriptedObjectEditorWindow.this, Translate.text("objectParameters"), true);
      script = (ScriptedObject) info.getObject();
      FormContainer content = new FormContainer(new double [] {0.0, 1.0}, new double [] {1.0, 0.0, 0.0, 0.0});
      content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, null, null));
      setContent(content);
      name = new String [script.getNumParameters()];
      value = new double [script.getNumParameters()];
      for (int i = 0; i < name.length; i++)
        {
          name[i] = script.getParameterName(i);
          value[i] = script.getParameterValue(i);
        }
      content.add(UIUtilities.createScrollingList(paramList = new BList()), 0, 0, 2, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
      paramList.setPreferredVisibleRows(5);
      buildParameterList();
      paramList.addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
      content.add(Translate.label("Name"), 0, 1);
      content.add(Translate.label("Value"), 0, 2);
      content.add(nameField = new BTextField(), 1, 1);
      nameField.addEventLink(ValueChangedEvent.class, this, "textChanged");
      nameField.addEventLink(FocusLostEvent.class, this, "focusLost");
      content.add(valueField = new ValueField(0.0, ValueField.NONE), 1, 2);
      valueField.addEventLink(ValueChangedEvent.class, this, "textChanged");
      RowContainer buttons = new RowContainer();
      content.add(buttons, 0, 3, 2, 1, new LayoutInfo());
      buttons.add(Translate.button("add", this, "doAdd"));
      buttons.add(Translate.button("remove", this, "doRemove"));
      buttons.add(Translate.button("ok", this, "doOk"));
      buttons.add(Translate.button("cancel", this, "dispose"));
      setSelectedParameter(name.length == 0 ? -1 : 0);
      pack();
      UIUtilities.centerDialog(this, ScriptedObjectEditorWindow.this);
      setVisible(true);
    }
    
    /** Build the list of parameters. */
    
    private void buildParameterList()
    {
      paramList.removeAll();
      for (int i = 0; i < name.length; i++)
        paramList.add(name[i]);
      if (name.length == 0)
        paramList.add("(no parameters)");
    }
    
    /** Update the components to show the currently selected parameter. */
    
    private void setSelectedParameter(int which)
    {
      if (which != paramList.getSelectedIndex())
      {
        paramList.clearSelection();
        paramList.setSelected(which, true);
      }
      current = which;
      if (which == -1 || which >= name.length)
        {
          nameField.setEnabled(false);
          valueField.setEnabled(false);
        }
      else
        {
          nameField.setEnabled(true);
          valueField.setEnabled(true);
          nameField.setText(name[which]);
          valueField.setValue(value[which]);
        }
    }
    
    /** Deal with changes to the text fields. */
    
    private void textChanged(ValueChangedEvent ev)
    {
      if (current < 0 || current > name.length)
        return;
      if (ev.getWidget() == nameField)
        {
          name[current] = nameField.getText();
//          paramList.replaceItem(name[current], current);
        }
      else
        value[current] = valueField.getValue();
    }
    
    /** When the name field loses focus, update it in the list. */

    private void focusLost()
    {
      paramList.replace(current, name[current]);
    }
    
    /** Deal with selection changes. */
    
    private void selectionChanged()
    {
      setSelectedParameter(paramList.getSelectedIndex());
    }
    
    /** Add a new parameter. */
    
    private void doAdd()
    {
      String newName[] = new String [name.length+1];
      double newValue[] = new double [value.length+1];
      System.arraycopy(name, 0, newName, 0, name.length);
      System.arraycopy(value, 0, newValue, 0, value.length);
      newName[name.length] = "";
      newValue[value.length] = 0.0;
      name = newName;
      value = newValue;
      buildParameterList();
      setSelectedParameter(name.length-1);
      nameField.requestFocus();
    }
    
    /** Remove a parameter. */
    
    private void doRemove()
    {
      int which = paramList.getSelectedIndex();
      String newName[] = new String [name.length-1];
      double newValue[] = new double [value.length-1];
      for (int i = 0, j = 0; i < name.length; i++)
        {
          if (i == which)
            continue;
          newName[j] = name[i];
          newValue[j] = value[i];
          j++;
        }
      name = newName;
      value = newValue;
      buildParameterList();
      setSelectedParameter(-1);
    }
    
    /** Save the changes. */
    
    private void doOk()
    {
      script.setParameters(name, value);
      dispose();
    }
  }
}

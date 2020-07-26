/* Copyright (C) 2006-2013 by Peter Eastman
   Changes copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.keystroke;

import buoy.widget.*;
import buoy.event.*;
import artofillusion.ui.*;
import artofillusion.script.*;

import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * This class presents a user interface for editing a single KeystrokeRecord.  To use it, invoke
 * the static editKeystroke() method.
 */

public class KeystrokeEditor extends BDialog
{
  private final BTextField keyField;
  private final BTextField nameField;
  private final BComboBox languageChoice;

  private final RSyntaxTextArea syntaxTextArea;
  private final BButton okButton;
  private KeystrokeRecord record;

  private static final Set<Integer> reserved = new HashSet<Integer>() {
    {
      add(KeyEvent.VK_LEFT);
      add(KeyEvent.VK_RIGHT);
      add(KeyEvent.VK_UP);
      add(KeyEvent.VK_DOWN);
      add(KeyEvent.VK_ENTER);
      add(KeyEvent.VK_ESCAPE);
      add(KeyEvent.VK_TAB);
      add(KeyEvent.VK_SHIFT);
      add(KeyEvent.VK_ALT);
      add(KeyEvent.VK_CONTROL);
      add(KeyEvent.VK_META);

    }
  };

  /**
   * Display a dialog for editing a KeystrokeRecord.
   *
   * @param record      the record to be edited
   * @param parent      the parent window
   * @return a new KeystrokeRecord representing the edited keystroke, or null if the user
   * clicked Cancel.
   */

  public static KeystrokeRecord showEditorDialog(KeystrokeRecord record, WindowWidget parent)
  {
    KeystrokeEditor editor = new KeystrokeEditor(record, parent);
    editor.setVisible(true);
    return editor.record;
  }

  private KeystrokeEditor(KeystrokeRecord record, WindowWidget parent)
  {
    super(parent, true);
    FormContainer content = new FormContainer(new double [] {0, 1}, new double [] {0, 0, 0, 0, 1, 0});
    setContent(content);
    this.record = record.duplicate();
    keyField = new BTextField(KeystrokePreferencesPanel.getKeyDescription(record.getKeyCode(), record.getModifiers()));
    keyField.setEditable(false);
    keyField.addEventLink(KeyPressedEvent.class, this, "setKey");
    nameField = new BTextField(record.getName());
    languageChoice = new BComboBox(ScriptRunner.LANGUAGES);
    languageChoice.setSelectedValue(record.getLanguage());

    syntaxTextArea = new RSyntaxTextArea(record.getScript(), 25, 100);
    syntaxTextArea.setTabSize(2);
    syntaxTextArea.setCodeFoldingEnabled(true);
    syntaxTextArea.setSyntaxEditingStyle(record.getLanguage().equalsIgnoreCase("groovy") ? SyntaxConstants.SYNTAX_STYLE_GROOVY : SyntaxConstants.SYNTAX_STYLE_JAVA);

    LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE);
    LayoutInfo fillLayout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null);
    content.add(Translate.label("Key"), 0, 0, rightLayout);
    content.add(Translate.label("Name"), 0, 1, rightLayout);
    content.add(Translate.label("language"), 0, 2, rightLayout);
    content.add(keyField, 1, 0, fillLayout);
    content.add(nameField, 1, 1, fillLayout);
    content.add(languageChoice, 1, 2, new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE));
    content.add(Translate.label("Script"), 0, 3, 2, 1, new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
    content.add(new AWTWidget(new RTextScrollPane(syntaxTextArea)), 0, 4, 2, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 5, 2, 1);
    okButton = Translate.button("ok", this, "clickedOk");
    buttons.add(okButton);
    buttons.add(Translate.button("cancel", this, "clickedCancel"));
    enableOkButton();
    pack();
  }

  private void clickedOk()
  {
    record.setName(nameField.getText());
    record.setLanguage(languageChoice.getSelectedValue().toString());
    record.setScript(syntaxTextArea.getText());
    dispose();
  }

  private void clickedCancel()
  {
    record = null;
    dispose();
  }

  private void enableOkButton()
  {
    okButton.setEnabled(record.getKeyCode() != 0);
  }

  private void setKey(KeyPressedEvent event)
  {
    int code = event.getKeyCode();
    if(reserved.contains(code)) return;

    int modifiers = event.getModifiers() & (KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK);
    record.setKeyCode(code);
    record.setModifiers(modifiers);
    keyField.setText(KeystrokePreferencesPanel.getKeyDescription(code, modifiers));
    enableOkButton();
  }
}

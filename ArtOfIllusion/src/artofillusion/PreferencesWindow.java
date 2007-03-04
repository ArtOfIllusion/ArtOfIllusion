/* Copyright (C) 2002-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.ui.*;
import artofillusion.keystroke.*;
import buoy.widget.*;
import java.util.*;
import java.awt.*;

/** This is the window for editing application-wide preferences. */

public class PreferencesWindow
{
  private BComboBox defaultRendChoice, objectRendChoice, texRendChoice, displayChoice, localeChoice, colorChoice, toolChoice;
  private ValueField interactiveTolField, undoField;
  private BCheckBox glBox, backupBox;
  private static int lastTab;

  public PreferencesWindow(BFrame parent)
  {
    BTabbedPane tabs = new BTabbedPane();
    tabs.add(createGeneralPanel(), Translate.text("general"));
    KeystrokePreferencesPanel keystrokePanel = new KeystrokePreferencesPanel();
    tabs.add(keystrokePanel, Translate.text("shortcuts"));
    tabs.setSelectedTab(lastTab);
    boolean done = false;
    while (!done)
    {
      PanelDialog dlg = new PanelDialog(parent, Translate.text("prefsTitle"), tabs);
      lastTab = tabs.getSelectedTab();
      if (!dlg.clickedOk())
        return;
      done = true;
      if (interactiveTolField.getValue() < 0.01)
      {
        String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
        int choice = new BStandardDialog("", Translate.text("lowSurfErrorWarning"), BStandardDialog.WARNING).showOptionDialog(parent, options, options[0]);
        if (choice == 1)
          done = false;
      }
    }
    ApplicationPreferences prefs = ModellingApp.getPreferences();
    Locale languages[] = Translate.getAvailableLocales();
    Renderer renderers[] = ModellingApp.getRenderers();
    if (renderers.length > 0)
    {
      prefs.setDefaultRenderer(renderers[defaultRendChoice.getSelectedIndex()]);
      prefs.setObjectPreviewRenderer(renderers[objectRendChoice.getSelectedIndex()]);
      prefs.setTexturePreviewRenderer(renderers[texRendChoice.getSelectedIndex()]);
    }
    prefs.setDefaultDisplayMode(displayChoice.getSelectedIndex());
    prefs.setColorScheme(colorChoice.getSelectedIndex());
    prefs.setInteractiveSurfaceError(interactiveTolField.getValue());
    prefs.setUndoLevels((int) undoField.getValue());
    if (!prefs.getLocale().equals(languages[localeChoice.getSelectedIndex()]))
      new BStandardDialog("", UIUtilities.breakString(Translate.text("languageChangedWarning")), BStandardDialog.INFORMATION).showMessageDialog(parent);
    if (prefs.getUseOpenGL() != glBox.getState())
      new BStandardDialog("", UIUtilities.breakString(Translate.text("glChangedWarning")), BStandardDialog.INFORMATION).showMessageDialog(parent);
    prefs.setLocale(languages[localeChoice.getSelectedIndex()]);
    prefs.setUseOpenGL(glBox.getState());
    prefs.setKeepBackupFiles(backupBox.getState());
    prefs.setUseCompoundMeshTool(toolChoice.getSelectedIndex() == 1);
    prefs.savePreferences();
    keystrokePanel.saveChanges();
  }

  /** Create a Choice for selecting a renderer. */

  private BComboBox getRendererChoice(Renderer selected)
  {
    Renderer renderers[] = ModellingApp.getRenderers();
    BComboBox c = new BComboBox();

    for (int i = 0; i < renderers.length; i++)
      c.add(renderers[i].getName());
    if (selected != null)
      c.setSelectedValue(selected.getName());
    return c;
  }

  /** Create the general settings panel. */

  private Widget createGeneralPanel()
  {
    // Create the Widgets.

    ApplicationPreferences prefs = ModellingApp.getPreferences();
    defaultRendChoice = getRendererChoice(prefs.getDefaultRenderer());
    objectRendChoice = getRendererChoice(prefs.getObjectPreviewRenderer());
    texRendChoice = getRendererChoice(prefs.getTexturePreviewRenderer());
    interactiveTolField = new ValueField(prefs.getInteractiveSurfaceError(), ValueField.POSITIVE);
    undoField = new ValueField(prefs.getUndoLevels(), ValueField.POSITIVE+ValueField.INTEGER);
    glBox  = new BCheckBox(Translate.text("useOpenGL"), prefs.getUseOpenGL());
    glBox.setEnabled(ViewerCanvas.isOpenGLAvailable());
    backupBox  = new BCheckBox(Translate.text("keepBackupFiles"), prefs.getKeepBackupFiles());
    displayChoice = new BComboBox(new String [] {
      Translate.text("menu.wireframeDisplay"),
      Translate.text("menu.shadedDisplay"),
      Translate.text("menu.smoothDisplay"),
      Translate.text("menu.texturedDisplay")
    });
    displayChoice.setSelectedIndex(prefs.getDefaultDisplayMode());
    colorChoice = new BComboBox(new String [] {
      Translate.text("White"),
      Translate.text("Gray")
    });
    colorChoice.setSelectedIndex(prefs.getColorScheme());
    toolChoice = new BComboBox(new String [] {
      Translate.text("Move"),
      Translate.text("compoundMoveScaleRotate")
    });
    toolChoice.setSelectedIndex(prefs.getUseCompoundMeshTool() ? 1 : 0);
    localeChoice = new BComboBox();
    Locale languages[] = Translate.getAvailableLocales();
    for (int i = 0; i < languages.length; i++)
    {
      localeChoice.add(languages[i].getDisplayName(prefs.getLocale()));
      if (prefs.getLocale().equals(languages[i]))
        localeChoice.setSelectedIndex(i);
    }

    // Layout the panel.

    FormContainer panel = new FormContainer(2, 11);
    panel.setColumnWeight(1, 1.0);
    LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(2, 0, 2, 5), null);
    LayoutInfo widgetLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.BOTH, new Insets(2, 0, 2, 0), null);
    LayoutInfo centerLayout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(2, 0, 2, 0), null);
    panel.add(Translate.label("defaultRenderer"), 0, 0, labelLayout);
    panel.add(Translate.label("objPreviewRenderer"), 0, 1, labelLayout);
    panel.add(Translate.label("texPreviewRenderer"), 0, 2, labelLayout);
    panel.add(Translate.label("defaultDisplayMode"), 0, 3, labelLayout);
    panel.add(Translate.label("backgroundColor"), 0, 4, labelLayout);
    panel.add(Translate.label("defaultMeshEditingTool"), 0, 5, labelLayout);
    panel.add(Translate.label("interactiveSurfError"), 0, 6, labelLayout);
    panel.add(Translate.label("maxUndoLevels"), 0, 7, labelLayout);
    panel.add(Translate.label("language"), 0, 10, labelLayout);
    panel.add(defaultRendChoice, 1, 0, widgetLayout);
    panel.add(objectRendChoice, 1, 1, widgetLayout);
    panel.add(texRendChoice, 1, 2, widgetLayout);
    panel.add(displayChoice, 1, 3, widgetLayout);
    panel.add(colorChoice, 1, 4, widgetLayout);
    panel.add(toolChoice, 1, 5, widgetLayout);
    panel.add(interactiveTolField, 1, 6, widgetLayout);
    panel.add(undoField, 1, 7, widgetLayout);
    panel.add(glBox, 0, 8, 2, 1, centerLayout);
    panel.add(backupBox, 0, 9, 2, 1, centerLayout);
    panel.add(localeChoice, 1, 10, widgetLayout);
    return panel;
  }
}
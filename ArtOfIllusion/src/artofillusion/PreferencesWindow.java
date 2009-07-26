/* Copyright (C) 2002-2009 by Peter Eastman

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
import buoy.event.*;

import java.util.*;
import java.util.List;
import java.awt.*;

/** This is the window for editing application-wide preferences. */

public class PreferencesWindow
{
  private BComboBox defaultRendChoice, objectRendChoice, texRendChoice, localeChoice, themeChoice, colorChoice, toolChoice;
  private ValueField interactiveTolField, undoField;
  private BCheckBox glBox, backupBox, reverseZoomBox;
  private List<ThemeManager.ThemeInfo> themes;
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
    ApplicationPreferences prefs = ArtOfIllusion.getPreferences();
    Locale languages[] = Translate.getAvailableLocales();
    List<Renderer> renderers = PluginRegistry.getPlugins(Renderer.class);
    if (renderers.size() > 0)
    {
      prefs.setDefaultRenderer(renderers.get(defaultRendChoice.getSelectedIndex()));
      prefs.setObjectPreviewRenderer(renderers.get(objectRendChoice.getSelectedIndex()));
      prefs.setTexturePreviewRenderer(renderers.get(texRendChoice.getSelectedIndex()));
    }
    prefs.setInteractiveSurfaceError(interactiveTolField.getValue());
    prefs.setUndoLevels((int) undoField.getValue());
    if (!prefs.getLocale().equals(languages[localeChoice.getSelectedIndex()]))
      new BStandardDialog("", UIUtilities.breakString(Translate.text("languageChangedWarning")), BStandardDialog.INFORMATION).showMessageDialog(parent);
    if (prefs.getUseOpenGL() != glBox.getState())
      new BStandardDialog("", UIUtilities.breakString(Translate.text("glChangedWarning")), BStandardDialog.INFORMATION).showMessageDialog(parent);
    if (!ThemeManager.getSelectedTheme().getName().equals(themeChoice.getSelectedValue()))
      new BStandardDialog("", UIUtilities.breakString(Translate.text("themeChangedWarning")), BStandardDialog.INFORMATION).showMessageDialog(parent);
    prefs.setLocale(languages[localeChoice.getSelectedIndex()]);
    prefs.setUseOpenGL(glBox.getState());
    prefs.setKeepBackupFiles(backupBox.getState());
    prefs.setReverseZooming(reverseZoomBox.getState());
    prefs.setUseCompoundMeshTool(toolChoice.getSelectedIndex() == 1);
    ThemeManager.setSelectedTheme(themes.get(themeChoice.getSelectedIndex()));
    ThemeManager.setSelectedColorSet(ThemeManager.getSelectedTheme().getColorSets()[colorChoice.getSelectedIndex()]);
    prefs.savePreferences();
    keystrokePanel.saveChanges();
  }

  /** Create a Choice for selecting a renderer. */

  private BComboBox getRendererChoice(Renderer selected)
  {
    List<Renderer> renderers = PluginRegistry.getPlugins(Renderer.class);
    BComboBox c = new BComboBox();

    for (Renderer r : renderers)
      c.add(r.getName());
    if (selected != null)
      c.setSelectedValue(selected.getName());
    return c;
  }

  /** Create the general settings panel. */

  private Widget createGeneralPanel()
  {
    // Create the Widgets.

    ApplicationPreferences prefs = ArtOfIllusion.getPreferences();
    defaultRendChoice = getRendererChoice(prefs.getDefaultRenderer());
    objectRendChoice = getRendererChoice(prefs.getObjectPreviewRenderer());
    texRendChoice = getRendererChoice(prefs.getTexturePreviewRenderer());
    interactiveTolField = new ValueField(prefs.getInteractiveSurfaceError(), ValueField.POSITIVE);
    undoField = new ValueField(prefs.getUndoLevels(), ValueField.POSITIVE+ValueField.INTEGER);
    glBox = new BCheckBox(Translate.text("useOpenGL"), prefs.getUseOpenGL());
    glBox.setEnabled(ViewerCanvas.isOpenGLAvailable());
    backupBox = new BCheckBox(Translate.text("keepBackupFiles"), prefs.getKeepBackupFiles());
    reverseZoomBox  = new BCheckBox(Translate.text("reverseScrollWheelZooming"), prefs.getReverseZooming());
    List allThemes = ThemeManager.getThemes();
    themes = new ArrayList<ThemeManager.ThemeInfo>();
    for (int i = 0; i < allThemes.size(); i++)
    {
      ThemeManager.ThemeInfo theme = (ThemeManager.ThemeInfo) allThemes.get(i);
      if (theme.selectable)
        themes.add(theme);
    }
    Collections.sort(themes, new Comparator<ThemeManager.ThemeInfo>()
    {
      public int compare(ThemeManager.ThemeInfo o1, ThemeManager.ThemeInfo o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });
    String themeNames[] = new String[themes.size()];
    for (int i = 0; i < themeNames.length; i++)
      themeNames[i] = themes.get(i).getName();
    themeChoice = new BComboBox(themeNames);
    ThemeManager.ThemeInfo selectedTheme = ThemeManager.getSelectedTheme();
    themeChoice.setSelectedValue(selectedTheme.getName());
    colorChoice = new BComboBox();
    buildColorSetMenu(selectedTheme);
    themeChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        buildColorSetMenu(themes.get(themeChoice.getSelectedIndex()));
      }
    });
    ThemeManager.ColorSet[] colorSets = selectedTheme.getColorSets();
    for (int i = 0; i < colorSets.length; i++)
      if (colorSets[i] == ThemeManager.getSelectedColorSet())
        colorChoice.setSelectedIndex(i);
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

    FormContainer panel = new FormContainer(2, 12);
    panel.setColumnWeight(1, 1.0);
    LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(2, 0, 2, 5), null);
    LayoutInfo widgetLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.BOTH, new Insets(2, 0, 2, 0), null);
    LayoutInfo centerLayout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(2, 0, 2, 0), null);
    panel.add(Translate.label("defaultRenderer"), 0, 0, labelLayout);
    panel.add(Translate.label("objPreviewRenderer"), 0, 1, labelLayout);
    panel.add(Translate.label("texPreviewRenderer"), 0, 2, labelLayout);
    panel.add(Translate.label("selectedTheme"), 0, 3, labelLayout);
    panel.add(Translate.label("themeColorSet"), 0, 4, labelLayout);
    panel.add(Translate.label("defaultMeshEditingTool"), 0, 5, labelLayout);
    panel.add(Translate.label("interactiveSurfError"), 0, 6, labelLayout);
    panel.add(Translate.label("maxUndoLevels"), 0, 7, labelLayout);
    panel.add(Translate.label("language"), 0, 11, labelLayout);
    panel.add(defaultRendChoice, 1, 0, widgetLayout);
    panel.add(objectRendChoice, 1, 1, widgetLayout);
    panel.add(texRendChoice, 1, 2, widgetLayout);
    panel.add(themeChoice, 1, 3, widgetLayout);
    panel.add(colorChoice, 1, 4, widgetLayout);
    panel.add(toolChoice, 1, 5, widgetLayout);
    panel.add(interactiveTolField, 1, 6, widgetLayout);
    panel.add(undoField, 1, 7, widgetLayout);
    panel.add(reverseZoomBox, 0, 8, 2, 1, centerLayout);
    panel.add(glBox, 0, 9, 2, 1, centerLayout);
    panel.add(backupBox, 0, 10, 2, 1, centerLayout);
    panel.add(localeChoice, 1, 11, widgetLayout);
    return panel;
  }

  private void buildColorSetMenu(ThemeManager.ThemeInfo theme)
  {
    ThemeManager.ColorSet colorSets[] = theme.getColorSets();
    String names[] = new String[colorSets.length];
    for (int i = 0; i < names.length; i++)
      names[i] = colorSets[i].getName();
    colorChoice.setContents(names);
  }
}
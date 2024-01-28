/* Copyright (C) 2024 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.preferences;

import artofillusion.ArtOfIllusion;
import artofillusion.ui.ThemeManager;
import artofillusion.ui.Translate;
import artofillusion.ui.UIUtilities;
import buoy.widget.BStandardDialog;
import buoy.widget.Widget;



public class AppearancePreferencesPanel extends buoy.widget.AWTWidget implements PreferencesEditor {

    private final AppearancePreferencesPanelImpl impl;
    
    public AppearancePreferencesPanel() {
        super(new AppearancePreferencesPanelImpl());
        impl = (AppearancePreferencesPanelImpl)this.component;
    }
    
    @Override
    public Widget getPreferencesPanel() {
        return this;
    }

    @Override
    public void savePreferences() {
        artofillusion.ApplicationPreferences preferences = ArtOfIllusion.getPreferences();
        java.util.Locale sl = impl.getSelectedLocale();
        if (!preferences.getLocale().equals(sl)) {
            new BStandardDialog("", UIUtilities.breakString(Translate.text("languageChangedWarning")), BStandardDialog.INFORMATION).showMessageDialog(UIUtilities.findWindow(this));

        }

        if (!ThemeManager.getSelectedTheme().getName().equals(impl.getSelectedThemeName())) {
            new BStandardDialog("", UIUtilities.breakString(Translate.text("themeChangedWarning")), BStandardDialog.INFORMATION).showMessageDialog(UIUtilities.findWindow(this));
        }

        ThemeManager.setSelectedTheme(impl.getSelectedTheme());
        ThemeManager.setSelectedColorSet(ThemeManager.getSelectedTheme().getColorSets()[impl.getSelectedColorSetIndex()]);
        preferences.setLocale(sl);
        preferences.savePreferences();
    }
    
    @Override
    public String getName() {
        return Translate.text("Appearance");
    }
    
}

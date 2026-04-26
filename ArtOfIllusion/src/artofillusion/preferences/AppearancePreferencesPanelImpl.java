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
import artofillusion.ui.ThemeManager.ThemeInfo;
import artofillusion.ui.Translate;


import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 *
 * @author MaksK
 */

public class AppearancePreferencesPanelImpl extends javax.swing.JPanel {
    
    private static final Comparator<ThemeInfo> tc  = Comparator.comparing(ThemeInfo::getName);
    private final List<ThemeInfo> themes = ThemeManager.getThemes().stream().filter(info -> info.selectable).sorted(tc).collect(Collectors.toList());

    private final Locale[] languages = Translate.getAvailableLocales();
    /**
     * Creates new form AppearancePreferencesPanelImpl
     */
    public AppearancePreferencesPanelImpl() {
        initComponents();
    }

    private DefaultComboBoxModel<String> getLocalesModel() {
        DefaultComboBoxModel<String> dcm = new DefaultComboBoxModel<>();
        Locale cloc =  ArtOfIllusion.getPreferences().getLocale();
        for(Locale loc: languages) {
            String localeDisplayName = loc.getDisplayName(loc);
            dcm.addElement(localeDisplayName);
            if(cloc.equals(loc)) dcm.setSelectedItem(localeDisplayName);
        }
        return  dcm;
    } 
    
    private DefaultComboBoxModel<String> getThemesModel() {
        DefaultComboBoxModel<String> dcm = new DefaultComboBoxModel<>();
        String selectedThemeName = ThemeManager.getSelectedTheme().getName();
        themes.forEach(theme -> {
            dcm.addElement(theme.getName());
            if(theme.getName().equals(selectedThemeName)) dcm.setSelectedItem(theme.getName());
        });
        return dcm;
    } 
      
    private DefaultComboBoxModel<String> getColorSetModel(ThemeInfo theme) {
        DefaultComboBoxModel<String> dcm = new DefaultComboBoxModel<>();
        for(ThemeManager.ColorSet cSet:  theme.getColorSets()) {
            dcm.addElement(cSet.getName());
        } 
        return dcm;
    } 
    
    
    public Locale getSelectedLocale() {
        return languages[languageSelector.getSelectedIndex()];
    }

    public String getSelectedThemeName() {
        return themeSelector.getSelectedItem().toString();
    }

    public int getSelectedColorSetIndex() {
        return colorSetSelector.getSelectedIndex();
    } 
    
    public ThemeInfo getSelectedTheme() {
        return themes.get(themeSelector.getSelectedIndex());
    } 

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Layout Code">
    private void initComponents() {

        languageSelector = new javax.swing.JComboBox<>();
        javax.swing.JLabel languageLabel = new javax.swing.JLabel();
        themeSelector = new javax.swing.JComboBox<>();
        javax.swing.JLabel themeLabel = new javax.swing.JLabel();
        colorSetSelector = new javax.swing.JComboBox<>();
        javax.swing.JLabel colorSetLabel = new javax.swing.JLabel();

        languageSelector.setMaximumRowCount(languages.length > 20 ? 16 : languages.length);
        languageSelector.setModel(getLocalesModel());

        languageLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        languageLabel.setLabelFor(languageSelector);
        languageLabel.setText(Translate.text("language"));

        themeSelector.setModel(getThemesModel());
        themeSelector.setSelectedItem(ThemeManager.getSelectedTheme().getName());
        themeSelectorActionPerformed(null);
        themeSelector.addActionListener(event -> themeSelectorActionPerformed(event));

        themeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        themeLabel.setText(Translate.text("selectedTheme"));

        colorSetSelector.setModel(getColorSetModel(ThemeManager.getSelectedTheme()));
        setSelectedColorSet();

        colorSetLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        colorSetLabel.setLabelFor(colorSetSelector);
        colorSetLabel.setText(Translate.text("themeColorSet"));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(languageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                    .addComponent(themeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(colorSetLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(themeSelector, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(languageSelector, 0, 267, Short.MAX_VALUE)
                    .addComponent(colorSetSelector, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(languageSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(languageLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(themeSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(themeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(colorSetSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(colorSetLabel))
                .addContainerGap(204, Short.MAX_VALUE))
        );
    }// </editor-fold>

    private void themeSelectorActionPerformed(java.awt.event.ActionEvent evt) {
        String selectedThemeName = themeSelector.getSelectedItem().toString();
        themes.stream().filter(theme -> theme.getName().equals(selectedThemeName)).findFirst().ifPresent(ft -> {
            colorSetSelector.setModel(getColorSetModel(ft));
        });
    }

    private void setSelectedColorSet() {
        ThemeManager.ColorSet[] colorSets = ThemeManager.getSelectedTheme().getColorSets();
        ThemeManager.ColorSet selectedSet = ThemeManager.getSelectedColorSet();
        for (int i = 0; i < colorSets.length; i++) {
            if (colorSets[i] == selectedSet) {
                colorSetSelector.setSelectedIndex(i);
                break;
            }
        }
    } 
    

    private JComboBox<String> colorSetSelector;
    private JComboBox<String> languageSelector;
    private JComboBox<String> themeSelector;


}

/* Copyright (C) 2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.widget.*;
import buoy.event.*;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.io.*;
import java.util.prefs.Preferences;

/**
 * This is a BFileChooser for selecting image files to load.  It displays a preview
 * of the currently selected image.
 */

public class ImageFileChooser extends BFileChooser
{
  private JLabel preview;

  private static final int PREVIEW_SIZE = 200;
  private static final int INSET = 5;
  private static final Preferences pref = Preferences.userNodeForPackage(ImageFileChooser.class);

  /**
   * Create an ImageFileChooser.
   *
   * @param title     the title displayed on the dialog
   */

  public ImageFileChooser(String title)
  {
    super(BFileChooser.OPEN_FILE, title);
    preview = new JLabel();
    preview.setPreferredSize(new Dimension(PREVIEW_SIZE+2*INSET, PREVIEW_SIZE+2*INSET));
    preview.setHorizontalAlignment(JLabel.CENTER);
    preview.setVerticalAlignment(JLabel.CENTER);
    JFileChooser jfc = getComponent();
    jfc.setAccessory(preview);
    
    // Set up filename filters.
    
    jfc.addChoosableFileFilter(new FileNameExtensionFilter(Translate.text("fileFilter.images"),
                     "jpg", "jpeg", "png", "tif", "tiff", "svg", "svgz", "hdr"));
    jfc.addChoosableFileFilter(new FileNameExtensionFilter(Translate.text("fileFilter.jpeg"), "jpg", "jpeg"));
    jfc.addChoosableFileFilter(new FileNameExtensionFilter(Translate.text("fileFilter.hdr"), "hdr"));
    jfc.addChoosableFileFilter(new FileNameExtensionFilter(Translate.text("fileFilter.png"), "png"));
    jfc.addChoosableFileFilter(new FileNameExtensionFilter(Translate.text("fileFilter.svg"), "svg", "svgz"));
    jfc.addChoosableFileFilter(new FileNameExtensionFilter(Translate.text("fileFilter.tif"), "tif", "tiff"));
    jfc.setAcceptAllFileFilterUsed(true);
    
    // Read the saved filter Preference.

    String preferredType = pref.get("LastType", Translate.text("fileFilter.images"));
    javax.swing.filechooser.FileFilter preferredFilter = null;
    for (javax.swing.filechooser.FileFilter filter : jfc.getChoosableFileFilters())
      if (filter.getDescription().equals(preferredType))
        preferredFilter = filter;

    // Sanity Check - if the value in preferences is not valid, just fall back to no filter.
    
    if (preferredFilter == null)
      preferredFilter = jfc.getAcceptAllFileFilter();
    setFileFilter(preferredFilter);

    addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
    selectionChanged();
  }

  @Override
  public boolean showDialog(Widget parent)
  {
    if (super.showDialog(parent))
    {
      pref.put("LastType", getFileFilter().getDescription());
      return true;
    }
    return false;
  }


  private void selectionChanged()
  {
    File file = null;
    if (isMultipleSelectionEnabled())
    {
      File files[] = getSelectedFiles();
      if (files.length > 1)
      {
        preview.setIcon(null);
        preview.setText(null);
        return;
      }
      if (files.length == 1)
        file = files[0];
    }
    else
      file = getSelectedFile();
    if (file == null || !file.isFile())
    {
      preview.setIcon(null);
      preview.setText(Translate.text("noFileSelected"));
    }
    else
    {
      ImageIcon image = new ImageIcon(file.getAbsolutePath());
      if (image.getImageLoadStatus() != MediaTracker.COMPLETE)
      {
        preview.setIcon(null);
        preview.setText(Translate.text("noPreviewAvailable"));
      }
      else
      {
        preview.setText(null);
        int width = image.getIconWidth();
        int height = image.getIconHeight();
        if (width > PREVIEW_SIZE && width >= height)
          image = new ImageIcon(image.getImage().getScaledInstance(PREVIEW_SIZE, -1, Image.SCALE_DEFAULT));
        else if (height > PREVIEW_SIZE)
          image = new ImageIcon(image.getImage().getScaledInstance(-1, PREVIEW_SIZE, Image.SCALE_DEFAULT));
        preview.setIcon(image);
      }
    }
  }
}

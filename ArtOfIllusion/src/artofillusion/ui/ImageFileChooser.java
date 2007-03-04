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
import java.awt.*;
import java.io.*;

/**
 * This is a BFileChooser for selecting image files to load.  It displays a preview
 * of the currently selected image.
 */

public class ImageFileChooser extends BFileChooser
{
  private JLabel preview;

  private static final int PREVIEW_SIZE = 200;
  private static final int INSET = 5;

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
    ((JFileChooser) getComponent()).setAccessory(preview);
    addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
    selectionChanged();
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

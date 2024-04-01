/* Copyright (C) 2024 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tools;

import artofillusion.object.Object3D;
import artofillusion.object.ReferenceImage;
import artofillusion.ui.ImageFileChooser;
import artofillusion.ui.Translate;
import artofillusion.ui.UIUtilities;
import buoy.widget.BFileChooser;
import buoy.widget.BStandardDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Optional;

public class ReferenceImageFactory implements PrimitiveFactory {

    private String objectName = "";

    @Override
    public String getCategory() {
        return "Other";
    }

    @Override
    public String getName() {
        return Translate.text("menu.referenceImage");
    }

    @Override
    public String getObjectName() {
        return objectName;
    }

    @Override
    public Optional<Object3D> create() {
        BFileChooser fc = new ImageFileChooser(Translate.text("selectReferenceImage"));
        if (!fc.showDialog(null)) return Optional.empty();
        File f = fc.getSelectedFile();
        Image image = new ImageIcon(f.getAbsolutePath()).getImage();

        if (image == null || image.getWidth(null) <= 0 || image.getHeight(null) <= 0)
        {
            new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingImage", f.getName())), BStandardDialog.ERROR).showMessageDialog(null);
            return Optional.empty();
        }
        objectName = f.getName();
        if (objectName.lastIndexOf('.') > -1) objectName = objectName.substring(0, objectName.lastIndexOf('.'));

        return Optional.of(new ReferenceImage(image));
    }

}

/* Copyright (C) 2004-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.ui.*;
import buoy.widget.*;
import buoy.event.*;

import java.awt.*;
import java.util.*;
import javax.swing.*;

/** TitleWindow displays a window containing the title and credits. */

public class TitleWindow extends BWindow
{
  public TitleWindow()
  {
    int imageNumber = new Random(System.currentTimeMillis()).nextInt(8);
    ImageIcon image = new ImageIcon(getClass().getResource("/artofillusion/titleImages/titleImage"+imageNumber+".jpg"));
    String text = "<html><div align=\"center\">"+
        "Art of Illusion v"+ModellingApp.VERSION+
        "<br>Copyright 1999-2007 by Peter Eastman and others"+
        "<br>(See the README file for details.)"+
        "<br>This program may be freely distributed under"+
        "<br>the terms of the accompanying license.</div></html>";
    BLabel label = new BLabel(text, image, BLabel.NORTH, BLabel.SOUTH);
    label.setFont(new Font("Serif", Font.PLAIN, 12));
    BOutline content = BOutline.createLineBorder(new BOutline(label, BorderFactory.createEmptyBorder(0, 0, 5, 0)), Color.BLACK, 1);
    Color background = Color.white;
    if (imageNumber == 4)
      background = new Color(204, 204, 255);
    else if (imageNumber == 6)
      background = new Color(232, 255, 232);
    UIUtilities.applyBackground(content, background);
    setContent(content);
    pack();
    Rectangle bounds = getBounds();
    bounds.height++;
    setBounds(bounds); // Workaround for Windows bug
    UIUtilities.centerWindow(this);
    setVisible(true);
  }
}
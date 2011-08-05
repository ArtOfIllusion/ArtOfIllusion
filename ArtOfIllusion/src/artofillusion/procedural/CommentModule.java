/* Copyright (C) 2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a Module which displays a comment, but otherwise has no effect on the procedure. */

public class CommentModule extends Module
{
  public CommentModule(Point position)
  {
    this(position, "Double-click to set comment");
  }
  
  public CommentModule(Point position, String text) 
  { 
    super(text, new IOPort [] {}, new IOPort [] {}, position); 
  }

  /** Allow the user to edit the comment text. */
  
  public boolean edit(ProcedureEditor editor, Scene theScene)
  {
    BTextArea ta = new BTextArea(name, 10, 40);
    PanelDialog dlg = new PanelDialog(editor.getParentFrame(), Translate.text("editComment"), BOutline.createBevelBorder(new BScrollPane(ta), false));
    if (!dlg.clickedOk())
      return false;
    name = ta.getText();
    layout();
    return true;
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    CommentModule mod = new CommentModule(new Point(bounds.x, bounds.y), name);
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeUTF(name);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    name = in.readUTF();
    layout();
  }
  
  /** Calculate the size on the screen of this module.  */
  
  public void calcSize()
  {
    String lines[] = name.split("\n");
    bounds.width = 0;
    for (int i = 0; i < lines.length; i++)
    {
      int len = defaultMetrics.stringWidth(lines[i]);
      if (len > bounds.width)
        bounds.width = len;
    }
    bounds.width += IOPort.SIZE*4;
    bounds.height = lines.length*(defaultMetrics.getMaxAscent()+defaultMetrics.getMaxDescent())+IOPort.SIZE*4;
  }
  
  /** Draw the contents of the module. */
  
  protected void drawContents(Graphics2D g)
  {
    g.setColor(Color.black);
    g.setFont(defaultFont);
    int lineHeight = defaultMetrics.getMaxAscent()+defaultMetrics.getMaxDescent();
    int offset = defaultMetrics.getAscent();
    String lines[] = name.split("\n");
    for (int i = 0; i < lines.length; i++)
      g.drawString(lines[i], bounds.x+IOPort.SIZE*2, bounds.y+IOPort.SIZE*2+offset+i*lineHeight);
  }
}

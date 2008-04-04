/* Copyright (C) 2001-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** ImagesDialog is a dialog box for editing the list of ImageMaps used in a scene. */

public class ImagesDialog extends BDialog
{
  private Scene theScene;
  private BFrame parent;
  private int selection;
  private BScrollPane sp;
  private ImagesCanvas ic;
  private BButton b[];

  public ImagesDialog(BFrame fr, Scene sc, ImageMap selected)
  {
    super(fr, "Images", true);
    BorderContainer content = new BorderContainer();
    setContent(content);
    parent = fr;
    theScene = sc;
    for (selection = 0; selection < sc.getNumImages() && sc.getImage(selection) != selected; selection++);
    if (selection == sc.getNumImages())
      selection = -1;
    sp = new BScrollPane(BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_ALWAYS);
    content.add(sp, BorderContainer.CENTER);
    sp.setContent(ic = new ImagesCanvas(5));
    RowContainer buttons = new RowContainer();
    content.add(buttons, BorderContainer.SOUTH);
    b = new BButton [4];
    buttons.add(b[0] = Translate.button("load", "...", this, "doLoad"));
    buttons.add(b[1] = Translate.button("delete", "...", this, "doDelete"));
    buttons.add(b[2] = Translate.button("selectNone", this, "doSelectNone"));
    buttons.add(b[3] = Translate.button("ok", this, "dispose"));
    hilightButtons();
    sp.setPreferredViewSize(new Dimension(ic.getGridWidth()*5+4, ic.getGridHeight()*4+4));
    pack();
    setResizable(false);
    addEventLink(WindowClosingEvent.class, this, "dispose");
    ic.imagesChanged();
    ic.scrollToSelection();
    UIUtilities.centerDialog(this, fr);
    setVisible(true);
  }

  public ImageMap getSelection()
  {
    if (selection < 0)
      return null;
    return theScene.getImage(selection);
  }

  private void hilightButtons()
  {
    b[1].setEnabled(selection >= 0);
    b[2].setEnabled(selection >= 0);
  }
  
  private void doLoad()
  {
    BFileChooser fc = new ImageFileChooser(Translate.text("selectImagesToLoad"));
    fc.setMultipleSelectionEnabled(true);
    if (!fc.showDialog(this))
      return;
    File files[] = fc.getSelectedFiles();
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (int i = 0; i < files.length; i++)
    {
      try
      {
        theScene.addImage(ImageMap.loadImage(files[i]));
      }
      catch (Exception ex)
      {
        new BStandardDialog("", Translate.text("errorLoadingImage", files[i].getName()), BStandardDialog.ERROR).showMessageDialog(this);
        ex.printStackTrace();
        setCursor(Cursor.getDefaultCursor());
        return;
      }
    }
    setCursor(Cursor.getDefaultCursor());
    selection = theScene.getNumImages()-1;
    ic.imagesChanged();
    ic.scrollToSelection();
    hilightButtons();
  }
  
  private void doDelete()
  {
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    BStandardDialog dlg = new BStandardDialog(null, Translate.text("deleteSelectedImage"), BStandardDialog.QUESTION);
    if (dlg.showOptionDialog(this, options, options[1]) == 1)
      return;
    boolean success = theScene.removeImage(selection);
    if (!success)
    {
      new BStandardDialog(null, UIUtilities.breakString(Translate.text("imageInUse")), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    selection = -1;
    ic.imagesChanged();
    hilightButtons();
  }
  
  private void doSelectNone()
  {
    selection = -1;
    ic.imagesChanged();
    hilightButtons();
  }

  /** ImagesCanvas is an inner class which displays the loaded images and allows the user
      to select one by clicking on it. */
  
  private class ImagesCanvas extends CustomWidget
  {
    private int w, h, gridw, gridh;

    public ImagesCanvas(int width)
    {
      w = width;
      gridw = ImageMap.PREVIEW_WIDTH + 10;
      gridh = ImageMap.PREVIEW_HEIGHT + 10;
      sp.getVerticalScrollBar().setUnitIncrement(gridh);
      addEventLink(RepaintEvent.class, this, "paint");
      addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    }

    public void imagesChanged()
    {
      h = (theScene.getNumImages()-1)/w + 1;
      setPreferredSize(new Dimension(w*gridw, h*gridh));
      sp.layoutChildren();
      repaint();
    }

    public int getGridWidth()
    {
      return gridw;
    }
    
    public int getGridHeight()
    {
      return gridh;
    }
    
    public void scrollToSelection()
    {
      if (selection < 0)
        return;
      sp.getVerticalScrollBar().setValue((selection/w)*gridh);
    }
    
    private void paint(RepaintEvent ev)
    {
      Graphics2D g = ev.getGraphics();
      for (int i = 0; i < theScene.getNumImages(); i++)
	g.drawImage(theScene.getImage(i).getPreview(), (i%w)*gridw+5, (i/w)*gridh+5, getComponent());
      if (selection >= 0)
	{
	  int x = (selection%w)*gridw, y = (selection/w)*gridh;
	  g.drawRect(x+1, y+1, gridw-2, gridh-2);
	  g.drawRect(x+2, y+2, gridw-4, gridh-4);
	}
    }

    private void mouseClicked(MouseClickedEvent ev)
    {
      Point p = ev.getPoint();
      int i, j;

      i = (p.x/gridw);
      j = (p.y/gridh);
      if (i < 5 && i+j*w < theScene.getNumImages())
	selection = i+j*w;
      else
        selection = -1;
      repaint();
      hilightButtons();
    }
  }
}


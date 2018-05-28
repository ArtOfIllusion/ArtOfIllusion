/* Copyright (C) 2001-2005 by Peter Eastman
   Modifications copyright (C) 2017 by Petri Ihalainen
   Changes copyright (C) 2018 by Maksim Khramov

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
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.Timer;
import static java.lang.Math.*;

/** ImagesDialog is a dialog box for editing the list of ImageMaps used in a scene. */

public class ImagesDialog extends BDialog
{
  private Scene theScene;
  private BFrame parent;
  private int selection, dialogHeight, dialogWidth, frameWidth, cOff=0;
  private BScrollPane sp;
  private ImagesCanvas ic;
  private BButton b[];
  private Color selectedColor;
  private int previewSize = 100, canvasWidth = 5;
  private LayoutInfo fillTight, fillLoose, fillLowRight;
  private ImageMap selectedImage;
  private Timer timer;
  
  public ImagesDialog(BFrame fr, Scene sc, ImageMap selected)
  {
    super(fr, "Images", true);
    selectedImage = selected;
    parent = fr;
    theScene = sc;
    selectedColor = ThemeManager.getSelectedColorSet().viewerHighlight;
    
    BorderContainer content = new BorderContainer();
    ColumnContainer buttonContainer = new ColumnContainer();
    ColumnContainer buttonArea = new ColumnContainer();
    GridContainer buttonGridUp  = new GridContainer(3,2);
    GridContainer buttonGridLow = new GridContainer(3,1);
    
    setContent(content);
    for (selection = 0; selection < sc.getNumImages() && sc.getImage(selection) != selected; selection++);
    if (selection == sc.getNumImages())
      selection = -1;
    sp = new BScrollPane(BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_ALWAYS);
    sp.setContent(ic = new ImagesCanvas(canvasWidth));
    
    content.add(sp, BorderContainer.CENTER);
    content.add(buttonContainer, BorderContainer.SOUTH);
    buttonContainer.add(buttonArea,  new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(0,0,0,0), null));
    buttonArea.add(buttonGridUp, new LayoutInfo(LayoutInfo.SOUTH, LayoutInfo.NONE, new Insets(10,0,0,0), null));
    buttonArea.add(buttonGridLow, new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.NONE, new Insets(0,0,10,0), null));

    b = new BButton[9];
    fillTight = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH);
    fillLoose  = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, new Insets(2,2,2,2), new Dimension(0,0));
    
    buttonGridUp.add(b[0]  = Translate.button("load", "...", this, "doLoad"), 0, 0, fillLoose);
    buttonGridUp.add(b[1]  = Translate.button("link", "...", this, "doLink"),0, 1, fillLoose);
    buttonGridUp.add(b[2]  = Translate.button("details", "...", this, "openDetailsDialog"), 1, 0, fillLoose);
    buttonGridUp.add(b[3]  = Translate.button("refresh", this, "doRefresh"), 1, 1, fillLoose);
    buttonGridUp.add(b[4]  = Translate.button("delete", "...", this, "doDelete"), 2, 0, fillLoose);
    buttonGridUp.add(b[5]  = Translate.button("purge", "...", this, "purge"), 2, 1, fillLoose);
    buttonGridLow.add(b[6] = Translate.button("selectNone", this, "doSelectNone"), 0, 0, fillLoose);
    buttonGridLow.add(b[7] = Translate.button("ok", this, "close"), 1, 0, fillLoose);
    buttonGridLow.add(b[8] = Translate.button("cancel", this, "cancel"), 2, 0, fillLoose); 

    hilightButtons();
    sp.setPreferredViewSize(new Dimension(ic.getGridWidth()*canvasWidth, ic.getGridHeight()*4));
    pack();
    dialogWidth  = getBounds().width;
    dialogHeight = getBounds().height;
    setResizable(true);
    addAsListener(this);
    addEventLink(WindowClosingEvent.class, this, "cancel");
    addEventLink(WindowResizedEvent.class, this, "resize");
    ic.scrollToSelection();
    ic.imagesChanged();
    UIUtilities.centerDialog(this, fr);
    setVisible(true);
  }
  
  /** 
      Create a square chequered grayscale image<p>
      @param size: Size of te image in pixels<br>
      @param sqSize: Size if the squares, the image consists of.<br>
      @param midShade: average value of color (0 - 255)<br>
      @param difference: color +/- difference form the average of the used two colors<br> 
  */
  
  public BufferedImage iconBackground(int size, int sqSize, int midShade, int difference)
  {
    Color bgColor1 = new Color(midShade-difference, midShade-difference, midShade-difference);
    Color bgColor2 = new Color(midShade+difference, midShade+difference, midShade+difference);
    int rgb1 = bgColor1.getRGB();
    int rgb2 = bgColor2.getRGB();
    int sq2 = sqSize*2;
    BufferedImage checkers = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

    for (int x = 0; x < size; x++)
      for (int y = 0; y < size; y++)
      {
        if ((x%sq2 < sqSize && y%sq2 < sqSize) || (x%sq2 >= sqSize && y%sq2 >= sqSize))
          checkers.setRGB(x, y, rgb1);
        else
          checkers.setRGB(x, y, rgb2);
      }
    return checkers;
  }

  public ImageMap getSelection()
  {
    if (selection < 0)
      return null;
    return theScene.getImage(selection);
  }

  private void hilightButtons()
  {
    b[2].setEnabled(selection >= 0); // open details
    boolean exts = false;
    for(int i = 0; i < theScene.getNumImages(); i++)
      if (theScene.getImage(i) instanceof ExternalImage)
        exts = true;
    b[3].setEnabled(exts); // refresh
    b[4].setEnabled(selection >= 0); // delete
    b[5].setEnabled(theScene.getNumImages() > 0); // purge
    b[6].setEnabled(selection >= 0); // select none
  }

  private void doRefresh()
  {
    for(int i = 0; i < theScene.getNumImages(); i++)
    {
      ImageMap imap = theScene.getImage(i);
      if (imap instanceof ExternalImage)
      {
        ((ExternalImage)imap).refreshImage();
      }
    }
    ic.imagesChanged();
    hilightButtons();
 }

  private void doLink()
  {
    BFileChooser fc = new ImageFileChooser(Translate.text("selectImageToLink"));
    fc.setMultipleSelectionEnabled(true);
    if (!fc.showDialog(this))
      return;
    File files[] = fc.getSelectedFiles();
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (File file: files)
    {
      try
      {
        theScene.addImage(new ExternalImage(file, theScene));
      }
      catch (Exception ex)
      {
        setCursor(Cursor.getDefaultCursor());
        Messages.error(Translate.text("errorLoadingImage", file.getName()), this.getComponent());
        ex.printStackTrace();
        return;
      }
    }
    setCursor(Cursor.getDefaultCursor());
    selection = theScene.getNumImages()-1;
    ic.imagesChanged();
    hilightButtons();
    setModified();
 }

  private void doLoad()
  {
    BFileChooser fc = new ImageFileChooser(Translate.text("selectImagesToLoad"));
    fc.setMultipleSelectionEnabled(true);
    if (!fc.showDialog(this))
      return;
    File files[] = fc.getSelectedFiles();
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (File file: files)
    {
      try
      {
        theScene.addImage(ImageMap.loadImage(file));
      }
      catch (Exception ex)
      {
        Messages.error(Translate.text("errorLoadingImage", file.getName()), this.getComponent());
        ex.printStackTrace();
        setCursor(Cursor.getDefaultCursor());
        
        // Return if any of the files can not be loaded. 
        // This should not be the case but currently there is no way of interrupting the load 
        // deliberately otherwise. 

        return; 
      }
    }
    setCursor(Cursor.getDefaultCursor());
    selection = theScene.getNumImages()-1;
    ic.imagesChanged();
    hilightButtons();
    setModified();
  }

  private void doDelete()
  {
    String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
    String name = theScene.getImage(selection).getName();
    if (name.equals(""))
        name = Translate.text("unNamed");
    String question = Translate.text("deleteSelectedImage") + ", \"" + name + "\" ?";
    BStandardDialog dlg = new BStandardDialog(null, question , BStandardDialog.QUESTION);
    if (dlg.showOptionDialog(this, options, options[1]) == 1)
      return;
    boolean success = theScene.removeImage(selection);
    if (!success)
    {
      Messages.error(UIUtilities.breakString(Translate.text("imageInUse")), this.getComponent());
      return;
    }
    selection = -1;
    ic.imagesChanged();
    hilightButtons();
    setModified();
  }

  private void purge()
  {
    new PurgeDialog(true);
    ic.imagesChanged();
    hilightButtons();
  }

  private void doSelectNone()
  {
    selection = -1;
    ic.imagesChanged();
    hilightButtons();
  }

  private void resize(WindowResizedEvent e)
  {
    // To prevent handling of interrupted resize-events 
    // Don't know if it really matters, but there are plenty of those.
    if (dialogWidth == getBounds().width && dialogHeight == getBounds().height)
      return;
    dialogWidth  = getBounds().width;
    dialogHeight = getBounds().height;
    ic.resized();
  }

  private void openDetailsDialog()
  {
    File oldFile = getSelection().getFile();
    new ImageDetailsDialog(parent, theScene, getSelection());
    ic.imagesChanged();
    ic.scrollToSelection();
    if (getSelection().getFile() != oldFile)
      setModified();
  }

  private Image loadIcon(String iconName)
  {
    try 
    {
      return ImageIO.read(ExternalImage.class.getResource("/artofillusion/image/icons/" + iconName));
    }
    catch(IOException e)
    {
        System.out.println(e);
    }
    return null;
  }

  private void close()
  {
    dispose();
    removeAsListener(this);
  }

  private void cancel()
  {
    // returning selection to, what is was at dialog open
    
    for (selection = 0; selection < theScene.getNumImages() && theScene.getImage(selection) != selectedImage; selection++);
    dispose();
    removeAsListener(this);
  }

  /** Set the parent window to modified state */

  private void setModified()
  { 
    if (parent instanceof EditingWindow)
    {
      ((EditingWindow)parent).setModified();
    }
    else
    {
      // If the dialog is accessed through a ProcedureEditor, the scene is already marked modified.
    }
  } 

  /** Pressing Return and Escape are equivalent to clicking OK and Cancel. Arrow keys can be used to select. */

  private void keyPressed(KeyPressedEvent ev)
  {
    int code = ev.getKeyCode();

    if (code == KeyPressedEvent.VK_ESCAPE)
      cancel();

    if (code == KeyPressedEvent.VK_ENTER)
      close();

    if (code == KeyPressedEvent.VK_LEFT)
      if (selection < 0)
        selection = theScene.getNumImages()-1;
      else
        selection = Math.max(selection-1, 0);
    else if (code == KeyPressedEvent.VK_RIGHT)
      selection = Math.min(selection+1, theScene.getNumImages()-1);
    else if (code == KeyPressedEvent.VK_UP)
    {
      if (selection < 0)
        selection = theScene.getNumImages()-1;
      else if (selection > canvasWidth-1)
        selection -= canvasWidth;
    }
    else if (code == KeyPressedEvent.VK_DOWN)
    {
      if (selection < 0)
        selection = 0;
    else
      selection = Math.min(selection+canvasWidth, theScene.getNumImages()-1);
    }
    else;
    ic.imagesChanged();
    hilightButtons();
  }

  /** Add this as a listener to every Widget. */

  private void addAsListener(Widget w)
  {
    w.addEventLink(KeyPressedEvent.class, this, "keyPressed");
    if (w instanceof WidgetContainer)
    {
      Iterator iter = ((WidgetContainer) w).getChildren().iterator();
      while (iter.hasNext())
        addAsListener((Widget) iter.next());
    }
  }

  /** Remove this as a listener before returning. */

  private void removeAsListener(Widget w)
  {
    w.removeEventLink(KeyPressedEvent.class, this);
    if (w instanceof WidgetContainer)
    {
      Iterator iter = ((WidgetContainer) w).getChildren().iterator();
      while (iter.hasNext())
        removeAsListener((Widget) iter.next());
    }
  }

  /** ImagesCanvas is an inner class which displays the loaded images and allows the user
      to select one by clicking on it. */

  private class ImagesCanvas extends CustomWidget
  {
    private int w, h, gridw, gridh, iconSize, lastIconSize = -1, textSize;
    private int step, steps, scrollIncrement, scrollFinalValue;
    private Color textBGColor = new Color(95,95,127,191);
    private Color textColor   = new Color(223,223,127,255);
    private Color canvasColor = new Color(223,223,223);
    private Color frameColor  = new Color(175,175,175);
    private Image linkedIcon, linkBrokenIcon, inUseIcon;
    private Image linkedIconTMP, linkBrokenIconTMP, inUseIconTMP;
    private ImageMap currentImage;
    private JViewport vp = sp.getComponent().getViewport();
    private Font templateFont = new BLabel().getFont();

    public ImagesCanvas(int width)
    {
      w = width; // Number of icons on one row
      gridw = previewSize + 10;
      gridh = previewSize + 10;
      sp.getVerticalScrollBar().setUnitIncrement(gridh/10);
      addEventLink(RepaintEvent.class, this, "paint");
      addEventLink(MouseClickedEvent.class, this, "mouseClicked");
      timer.setCoalesce(false);
    }

    /** The timer that keeps launcing animation 'frames' */
    private Timer timer = new Timer((int)(1f/61f*1000f), new ActionListener() 
    {
      public void actionPerformed(ActionEvent e) 
      {  
          BScrollBar bar = sp.getVerticalScrollBar();
        if ((scrollIncrement < 0 && bar.getValue()+scrollIncrement <= scrollFinalValue) ||
            (scrollIncrement > 0 && bar.getValue()+scrollIncrement >= scrollFinalValue) )
          {
            timer.stop();
              bar.setValue(scrollFinalValue);
          }
        else
          {
            bar.setValue(bar.getValue()+scrollIncrement);
            step++;
            timer.restart();
          }
      }
    });

    public void imagesChanged()
    {
      h = Math.max((theScene.getNumImages()-1)/w + 1, 4); // Number of rows of icons
      setPreferredSize(new Dimension(w*gridw, max(h*gridh, vp.getExtentSize().height)));
      sp.layoutChildren();
      scrollToSelection();
      repaint();
    }
    
    public void resized()
    {
      int vw = sp.getViewSize().width;
      previewSize = min(max(((vw)/5) - 10, ImageMap.PREVIEW_SIZE_DEFAULT), ImageMap.PREVIEW_SIZE_TEMPLATE);
      gridw = previewSize + 10;
      gridh = previewSize + 10; 
      cOff = max(0, (vw - (previewSize+10)*5)/2);
      setPreferredSize(new Dimension(w*gridw, max(h*gridh, vp.getExtentSize().height)));
      sp.getVerticalScrollBar().setUnitIncrement(gridh/10);
      sp.layoutChildren();
      scrollToSelection();
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
      timer.stop();
      if (selection < 0)
        return;
      int selUp   = selection/w*gridh ;
      int selLow = selUp+gridh;
      int vpUp = vp.getViewPosition().y;
      int vpLow = vpUp+vp.getExtentSize().height;
      int move = Math.min(Math.max(0, selLow-vpLow), selUp-vpUp);
      double scrollTime = 1.0*(1.0-1.0/(1.0+Math.abs((double)move)/(double)gridh*0.1));
      double incs = scrollTime*ArtOfIllusion.getPreferences().getAnimationFrameRate();
      scrollIncrement = (int)((double)move/incs);
      scrollFinalValue = sp.getVerticalScrollBar().getValue() + move;
      step = 1;
      timer.restart();
    }

    private void paint(RepaintEvent ev)
    {
      int x, y, head, tail;
      Graphics2D g = ev.getGraphics();
      Font textFont = templateFont.deriveFont((float)(previewSize/40+7));

      textSize = (int)Math.round(textFont.getSize2D());
      iconSize = textSize+21+previewSize/50;
      if (iconSize != lastIconSize)
      {
        inUseIcon      = loadIcon("in_use.png").getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
        linkedIcon     = loadIcon("linked.png").getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
        linkBrokenIcon = loadIcon("link_broken.png").getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
        lastIconSize   = iconSize;
      }
      g.setColor(canvasColor);
      g.fill(new Rectangle(0, vp.getViewPosition().y, vp.getExtentSize().width, vp.getExtentSize().height));

      int scrollY = vp.getViewPosition().y;
      int scrollH = vp.getExtentSize().height;
      BufferedImage bgImage = iconBackground(previewSize, 5, 207, 12);

      // Paint only the ones that are fit the visible part 
      // of the canvas. .. Have to paint one full row more for tha scroll
      head = Math.max((scrollY/gridh*w-1), 0);
      tail = Math.min(theScene.getNumImages(),(scrollY+scrollH)/gridh*w+w+1);
      boolean inUse = false;
      for (int i = head; i < tail; i++)
      {
        x = (i%w)*gridw+cOff;
        y = (i/w)*gridh;
        g.setColor(frameColor);
        g.fillRect(x+1, y+1, gridw-2, gridh-2);
        g.drawImage(bgImage, (i%w)*gridw+5+cOff, (i/w)*gridh+5, getComponent());
        
        currentImage = theScene.getImage(i);
        smoothPaint(g, i);

        x = (i%w)*gridw+cOff;
        y = (i/w)*gridh;

        if (selection == i)
        {
          x = (selection%w)*gridw+cOff;
          y = (selection/w)*gridh;
          g.setColor(selectedColor);
          g.drawRect(x+1, y+1, gridw-3, gridh-3);
          g.drawRect(x+2, y+2, gridw-5, gridh-5);
          g.drawRect(x+3, y+3, gridw-7, gridh-7);
          g.drawRect(x+4, y+4, gridw-9, gridh-9);
        }

        drawName(g, textFont, i);
        if (currentImage instanceof ExternalImage)
          if (((ExternalImage)currentImage).isConnected())
            g.drawImage(linkedIcon,(i%w)*gridw+1+cOff, (i/w)*gridh+9+previewSize-iconSize, getComponent());
          else
            g.drawImage(linkBrokenIcon,(i%w)*gridw+1+cOff, (i/w)*gridh+9+previewSize-iconSize, getComponent());
        for (int t = 0; t < theScene.getNumTextures(); t++)
          if (theScene.getTexture(t).usesImage(currentImage))
            inUse = true;
        if (inUse)
          g.drawImage(inUseIcon,(i%w)*gridw+9+cOff+previewSize-iconSize, (i/w)*gridh+9+previewSize-iconSize, getComponent());
        inUse = false;
      }
    }

    private void smoothPaint(Graphics2D g, int i)
    {
        Image pim = currentImage.getPreview(previewSize);
        if (pim == null)
          return;
        int xOff = (previewSize-pim.getWidth(null))/2;
        int yOff = (previewSize-pim.getHeight(null))/2;
        g.drawImage(pim, (i%w)*gridw+5+xOff+cOff, (i/w)*gridh+5+yOff, getComponent());
    }

    private void drawName(Graphics2D g, Font f, int i)
    {
        String name = theScene.getImage(i).getName();
        if (name.isEmpty())
          name = Translate.text("unNamed");
        
        BufferedImage textStripe = new BufferedImage(previewSize, textSize+5, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt = textStripe.createGraphics();
        gt.setColor(textBGColor);
        gt.fill(new Rectangle(0, 0, previewSize, textSize+5));
        gt.setColor(textColor);
        gt.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        gt.setFont(f);
        gt.drawString(name, iconSize*4/5, textSize+1);
        g.drawImage(textStripe, (i%w)*gridw+5+cOff, (i/w)*gridh+previewSize-textSize-2, null);
    }

    private void mouseClicked(MouseClickedEvent ev)
    {

      Point p = ev.getPoint();
      int i, j;

      i = ((p.x-cOff)/gridw);
      j = (p.y/gridh);
      if (cOff-p.x < 0 &&  i < 5 && i+j*w < theScene.getNumImages())
      {
        selection = i+j*w;
        scrollToSelection();
        // No need to repaint. The partially visible parts have been painted already.
      }
      else
        selection = -1;
      repaint();
      hilightButtons();
      if (ev.getClickCount() > 1)
        openDetailsDialog();
    }
  }

  /** PurgeDialog is the dialog for removing multiple unused images in one sweep */
  private class PurgeDialog extends BDialog
  {
    private ColumnContainer content;
    private BButton purgeButton, okButton, cancelButton, selectAllButton, selectNoneButton;
    private LayoutInfo thumb, text, box;
    private ArrayList<ImageMap> unusedImages;
    private BCheckBox[] removeBox;
    
    PurgeDialog(boolean intent)
    {
      super(parent, "Purge Images", true);
      
      thumb = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(1,15,1,1), null);
      text  = new LayoutInfo(LayoutInfo.CENTER,   LayoutInfo.NONE, new Insets(1,1,1,1),  null);
      box   = new LayoutInfo(LayoutInfo.WEST,   LayoutInfo.NONE, new Insets(1,1,1,15), null);

      content = new ColumnContainer();
      ColumnContainer buttonArea = new ColumnContainer();
      GridContainer buttonsUp = new GridContainer(3,1);
      GridContainer buttonsLow = new GridContainer(2,1);
      
      BLabel header = Translate.label("purgeHeader");
      header.setFont(header.getFont().deriveFont(Font.BOLD));
            
      buttonsUp.add(selectNoneButton = Translate.button("selectNone", this, "selectNone"), 0, 0, fillLoose);
      buttonsUp.add(selectAllButton = Translate.button("selectAll", this, "selectAll"), 1, 0, fillLoose);
      buttonsUp.add(purgeButton = Translate.button("purge", this, "deleteAndReturn"), 2, 0, fillLoose);
      buttonsLow.add(cancelButton = Translate.button("cancel", this, "close"), 1, 0, fillLoose);
      buttonsLow.add(okButton = Translate.button("ok", this, "deleteAndClose"), 0, 0, fillLoose);
      
      content.add(header, new LayoutInfo(LayoutInfo.SOUTH, LayoutInfo.NONE, new Insets(15,5,15,5), null));
      addUnusedImagesTable(intent);
      content.add(buttonArea, new LayoutInfo(LayoutInfo.SOUTH, LayoutInfo.NONE, new Insets(0,0,0,0), null));
      buttonArea.add(buttonsUp,  new LayoutInfo(LayoutInfo.SOUTH, LayoutInfo.NONE, new Insets(10,0,0,0), null));
      buttonArea.add(buttonsLow,  new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.NONE, new Insets(0,0,0,0), null));
      setContent(content);
      pack();
      setResizable(false);
      addAsListener(this);
      addEventLink(WindowClosingEvent.class, this, "close");
      setVisible(true);
    }

    private void addUnusedImagesTable(boolean intent) // intent = to delete or not
    {
      unusedImages = new ArrayList<ImageMap>();
      ImageMap im;
      boolean unused;
      FormContainer unusedTable;
      BScrollPane tableScroller;

      BufferedImage bg, nameTag;
      Image prev;
      Color textBG = new Color(223,223,223);

      for (int i = 0; i < theScene.getNumImages(); i++)
      {
        im = theScene.getImage(i);
        unused = true;
        for (int t = 0; t < theScene.getNumTextures(); t++)
          if (theScene.getTexture(t).usesImage(im))
            unused = false;
        if (unused)
          unusedImages.add(im);
      }
            
      unusedTable = new FormContainer(3,unusedImages.size());
      unusedTable.setColumnWeight(1, 10.0);
      removeBox = new BCheckBox[unusedImages.size()];
      
      int nameTagWidth = 0;
      Font f = new BButton().getFont();
      FontMetrics fm = new BufferedImage(1,1,1).createGraphics().getFontMetrics(f);
      for (int u = 0; u < unusedImages.size(); u++)
        nameTagWidth = fm.stringWidth(unusedImages.get(u).getName());
      nameTagWidth = Math.max(nameTagWidth+20, 200);
      
      if (unusedImages.size() > 0)
      {
        for (int u = 0; u < unusedImages.size(); u++)
        {
          removeBox[u] = new BCheckBox("", intent);
          String imageName = unusedImages.get(u).getName();
          if (imageName.isEmpty())
          imageName = Translate.text("unNamed");
          bg = iconBackground(40,4,207,8);
          prev = unusedImages.get(u).getPreview(40);
          nameTag = new BufferedImage(nameTagWidth, 40, BufferedImage.TYPE_INT_RGB);
          
          Graphics2D gp = bg.createGraphics();
          gp.drawImage(prev, (40-prev.getWidth(null))/2, (40-prev.getHeight(null))/2, getComponent());
          BLabel imageLabel = new BLabel(new ImageIcon(bg));
          
          Graphics2D gn = nameTag.createGraphics();
          gn.setColor(textBG);
          gn.fillRect(0,0,nameTagWidth,40);
          gn.setColor(Color.black);
          gn.setFont(f);
          gn.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
          gn.drawString(imageName, 8, 25);
          BLabel nameLabel  = new BLabel(new ImageIcon(nameTag), BLabel.WEST);
        
          unusedTable.add(imageLabel, 0, u, thumb);
          unusedTable.add(nameLabel, 1, u, text);
          unusedTable.add(removeBox[u], 2, u, box);
        }
        
        int scrollerW = unusedTable.getPreferredSize().width;
        int scrollerH;
        if (unusedImages.size() > 16)
          scrollerH = 42*12;
        else
          scrollerH = 42*unusedImages.size();
        BScrollPane unusedScroller;
        unusedScroller = new BScrollPane(BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_AS_NEEDED);
        unusedScroller.setPreferredViewSize(new Dimension(scrollerW, scrollerH));
        unusedScroller.getVerticalScrollBar().setBlockIncrement(42*1);
        unusedScroller.getVerticalScrollBar().setUnitIncrement(42*1);
        unusedScroller.setContent(unusedTable);
        content.add(unusedScroller);
      }
      else
      {
        String noPurge = Translate.text("allImagesInUse");

        int textWidth = fm.stringWidth(noPurge);
        int tagWidth = Math.max(textWidth+20, 200);
        int tOff = (tagWidth-textWidth)/2;

        nameTag = new BufferedImage(tagWidth, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D gn = nameTag.createGraphics();
        gn.setColor(textBG);
        gn.fillRect(0,0,tagWidth,40);
        gn.setColor(Color.black);
        gn.setFont(f);
        gn.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        gn.drawString(noPurge, tOff, 25);
        BLabel noPurgeLabel  = new BLabel(new ImageIcon(nameTag), BLabel.CENTER);
        content.add(noPurgeLabel);
      }
    }

    private void selectAll()
    {
      for (BCheckBox b: removeBox) b.setState(true);
    }

    private void selectNone()
    {
      for (BCheckBox b: removeBox) b.setState(false);
    }

    private void deleteAndReturn()
    {
      int count = 0;
      for (int r = 0; r < removeBox.length; r++)
        if (removeBox[r].getState())
          count++;
           
      if (count > 0 && confirmRemoval(count))
      {
        deleteSelectedImages();
        ic.imagesChanged();
        dispose();
        new PurgeDialog(false);
      }
    }

    private void deleteAndClose()
    {
      int count = 0;
      for (int r = 0; r < removeBox.length; r++)
        if (removeBox[r].getState())
           count++;
      if (count > 0 && confirmRemoval(count))
      {
        deleteSelectedImages();
        close();
      }
      close();
    }

    private boolean confirmRemoval(int count)
    {
      String title   = Translate.text("confirmTitle");
      String warning = Translate.text("purgeWarningHEAD") + " " + count + " " +
                       Translate.text("purgeWarningTAIL") + "\n" + 
                       Translate.text("purgeConfirmQuestion");

      BStandardDialog confirm = new BStandardDialog(title, warning, BStandardDialog.QUESTION);
      String[] options = new String[]{Translate.text("Yes"), Translate.text("No")};
      return (confirm.showOptionDialog(this, options, options[1]) == 0);
    }

    private void deleteSelectedImages()
    {
      for (int d = 0; d < unusedImages.size(); d++)
        if (removeBox[d].getState())
          for (int i = 0; i < theScene.getNumImages(); i++)
            if (theScene.getImage(i) == unusedImages.get(d))
            {
              theScene.removeImage(i);
              if (selection > i)
                selection--;
              else if (selection == i)
                selection = -1;
            }
      setModified();
    }

    private void close()
    {
      dispose();
      removeAsListener(this);
    }

    /** Pressing Return and Escape are equivalent to clicking OK and Cancel. */

    private void keyPressed(KeyPressedEvent ev)
    {
      int code = ev.getKeyCode();
      if (code == KeyPressedEvent.VK_ESCAPE)
        close();
      if (code == KeyPressedEvent.VK_ENTER)
        deleteAndClose();
    }

    /** Add this as a listener to every Widget. */

    private void addAsListener(Widget w)
    {
      w.addEventLink(KeyPressedEvent.class, this, "keyPressed");
      if (w instanceof WidgetContainer)
      {
        Iterator iter = ((WidgetContainer) w).getChildren().iterator();
        while (iter.hasNext())
          addAsListener((Widget) iter.next());
      }
    }

    /** Remove this as a listener before returning. */
    
    private void removeAsListener(Widget w)
    {
      w.removeEventLink(KeyPressedEvent.class, this);
      if (w instanceof WidgetContainer)
      {
        Iterator iter = ((WidgetContainer) w).getChildren().iterator();
        while (iter.hasNext())
          removeAsListener((Widget) iter.next());
      }
    }
  }
}


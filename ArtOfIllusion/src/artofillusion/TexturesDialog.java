/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/** TexturesDialog is a dialog box for editing the list of Textures used in a scene. */

public class TexturesDialog extends BDialog implements ListChangeListener
{
  private Scene theScene;
  private BFrame parent;
  private BList texList;
  private BButton b[];

  public TexturesDialog(BFrame fr, Scene sc)
  {
    super(fr, Translate.text("texturesTitle"), false);
    parent = fr;
    theScene = sc;
    BorderContainer content = new BorderContainer();
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    ColumnContainer buttons = new ColumnContainer();
    buttons.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, null, null));
    content.add(buttons, BorderContainer.WEST);
    b = new BButton [6];
    buttons.add(b[0] = Translate.button("new", "...", this, "doNew"));
    buttons.add(b[1] = Translate.button("copy", "...", this, "doCopy"));
    buttons.add(b[2] = Translate.button("delete", "...", this, "doDelete"));
    buttons.add(b[3] = Translate.button("import", "...", this, "doImport"));
    buttons.add(b[4] = Translate.button("edit", "...", this, "doEdit"));
    buttons.add(b[5] = Translate.button("close", this, "dispose"));
    texList = new BList();
    texList.setMultipleSelectionEnabled(false);
    content.add(UIUtilities.createScrollingList(texList), BorderContainer.CENTER);
    for (int i = 0; i < theScene.getNumTextures(); i++)
      texList.add(theScene.getTexture(i).getName());
    texList.addEventLink(SelectionChangedEvent.class, this, "hilightButtons");
    texList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    sc.addTextureListener(this);
    addEventLink(WindowClosingEvent.class, this, "dispose");
    hilightButtons();
    pack();
    UIUtilities.centerDialog(this, parent);
  }

  public void dispose()
  {
    theScene.removeTextureListener(this);
    super.dispose();
  }

  private void hilightButtons()
  {
    boolean selection = (texList.getSelectedIndex() != -1);
    
    b[1].setEnabled(selection);
    b[2].setEnabled(selection);
    b[4].setEnabled(selection);
  }
  
  private void mouseClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() == 2 && texList.getSelectedIndex() != -1)
      doEdit();
  }

  private void doNew()
  {
    showNewTextureWindow(this, theScene);
  }
  
  private void doCopy()
  {
    String name = new BStandardDialog("", Translate.text("newTexName"), BStandardDialog.PLAIN).showInputDialog(this, null, "");
    if (name == null)
      return;
    Texture tex = theScene.getTexture(texList.getSelectedIndex()).duplicate();
    tex.setName(name);
    theScene.addTexture(tex);
  }
  
  private void doDelete()
  {
    int selected = texList.getSelectedIndex();
    Texture tex = theScene.getTexture(selected);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    int choice = new BStandardDialog("", Translate.text("deleteTexture", tex.getName()), BStandardDialog.PLAIN).showOptionDialog(this, options, options[1]);
    if (choice == 0)
      theScene.removeTexture(selected);
  }
  
  private void doEdit()
  {
    int selected = texList.getSelectedIndex();
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    Texture tex = theScene.getTexture(selected);
    tex.edit(parent, theScene);
    tex.assignNewID();
    theScene.changeTexture(selected);
    setCursor(Cursor.getDefaultCursor());
  }
  
  /** Add a texture to the list. */
  
  public void itemAdded(int index, Object obj)
  {
    Texture tex = (Texture) obj;

    texList.add(index, tex.getName());
    texList.setSelected(texList.getSelectedIndex(), false);
    texList.setSelected(index, true);
    hilightButtons();
    if (parent instanceof LayoutWindow)
    {
      ((LayoutWindow) parent).setModified();
      ((LayoutWindow) parent).updateImage();
    }
  }
  
  /** Remove a texture from the list. */
  
  public void itemRemoved(int index, Object obj)
  {
    texList.removeAll();
    for (int j = 0; j < theScene.getNumTextures(); j++)
      texList.add(theScene.getTexture(j).getName());
    hilightButtons();
    if (parent instanceof LayoutWindow)
    {
      ((LayoutWindow) parent).setModified();
      ((LayoutWindow) parent).updateImage();
    }
  }
  
  /** Change the name of a texture in the list. */
  
  public void itemChanged(int index, Object obj)
  {
    Texture tex = (Texture) obj;
    
    texList.replace(index, tex.getName());
    texList.setSelected(index, true);
    if (parent instanceof LayoutWindow)
    {
      ((LayoutWindow) parent).setModified();
      ((LayoutWindow) parent).updateImage();
    }
  }
  
  /** Display a window in which the user can create a new texture. */
  
  public static Texture showNewTextureWindow(WindowWidget parent, Scene theScene)
  {
    BTextField nameField = new BTextField();
    BComboBox typeChoice = new BComboBox();
    List<Texture> textureTypes = PluginRegistry.getPlugins(Texture.class);
    java.lang.reflect.Method mtd;
    for (int j = 0; j < textureTypes.size(); j++)
    {
      try
      {
        mtd = textureTypes.get(j).getClass().getMethod("getTypeName", null);
        typeChoice.add((String) mtd.invoke(null, null));
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    int j = 0;
    String name = "";
    do
    {
      j++;
      name = "Untitled "+j;
    } while (theScene.getTexture(name) != null);
    nameField.setText(name);
    nameField.setSelectionStart(0);
    nameField.setSelectionEnd(name.length());
    ComponentsDialog dlg = new ComponentsDialog(parent, Translate.text("newTexNameAndType"),
            new Widget [] {nameField, typeChoice}, new String[] {Translate.text("Name"), Translate.text("Type")});
    if (dlg.clickedOk())
    {
      Widget frame = parent;
      while (!(frame instanceof BFrame))
        frame = frame.getParent();
      try
      {
        Texture tex = textureTypes.get(typeChoice.getSelectedIndex()).getClass().newInstance();
        tex.setName(nameField.getText());
        theScene.addTexture(tex);
        tex.edit((BFrame) frame, theScene);
        return tex;
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    return null;
  }
  
  /** Ask the user to select textures to import from a scene. */
  
  private void doImport()
  {
    // First prompt them to select a file.
    
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("importTextures"));
    if (!fc.showDialog(this))
      return;
    File f = fc.getSelectedFile();
    DataInputStream in = null;
    Scene sc = null;
    try
    {
      in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(f))));
      sc = new Scene(in, false);
    }
    catch (InvalidObjectException ex)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingWholeScene")), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    catch (IOException ex)
    {
      new BStandardDialog("", new String [] {Translate.text("errorLoadingFile"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    finally
    {
      try
      {
        in.close();
      }
      catch (Exception ex)
      {
        new BStandardDialog("", new String [] {Translate.text("errorLoadingFile"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
    }
    if (sc.getNumTextures() == 0)
    {
      new BStandardDialog("", Translate.text("noTexturesError"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    
    // Now create the main dialog.
    
    FormContainer mainPanel = new FormContainer(4, 1);
    mainPanel.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    final BList baseList = new BList(), importList = new BList();
    final BButton addButton = new BButton(Translate.text("Add")+" >>");
    final BButton removeButton = new BButton("<< "+Translate.text("Remove"));
    final Vector baseVec = new Vector(), importVec = new Vector();
    final MaterialPreviewer preview = new MaterialPreviewer(sc.getTexture(0), null, 160, 160);
    baseList.setMultipleSelectionEnabled(false);
    importList.setMultipleSelectionEnabled(false);
    for (int i = 0; i < sc.getNumTextures(); i++)
    {
      baseVec.addElement(sc.getTexture(i));
      baseList.add(sc.getTexture(i).getName());
    }
    Object listListener = new Object() {
      private void processEvent(WidgetEvent ev)
      {
	if (ev.getWidget() == baseList && baseList.getSelectedIndex() > -1)
        {
          if (importList.getSelectedIndex() > -1)
            importList.setSelected(importList.getSelectedIndex(), false);
          addButton.setEnabled(true);
          removeButton.setEnabled(false);
          Texture tex = (Texture) baseVec.elementAt(baseList.getSelectedIndex());
          preview.setTexture(tex, tex.getDefaultMapping(preview.getObject().getObject()));
          preview.render();
        }
	else if (importList.getSelectedIndex() > -1)
        {
          if (baseList.getSelectedIndex() > -1)
            baseList.setSelected(baseList.getSelectedIndex(), false);
          addButton.setEnabled(false);
          removeButton.setEnabled(true);
          Texture tex = (Texture) importVec.elementAt(importList.getSelectedIndex());
          preview.setTexture(tex, tex.getDefaultMapping(preview.getObject().getObject()));
          preview.render();
        }
      }
    };
    baseList.addEventLink(SelectionChangedEvent.class, listListener);
    importList.addEventLink(SelectionChangedEvent.class, listListener);
    baseList.setSelected(0, true);
    addButton.addEventLink(CommandEvent.class, new Object() {
      void processEvent()
      {
        int which = baseList.getSelectedIndex();
        if (which < 0)
          return;
        Texture tex = (Texture) baseVec.elementAt(which);
        baseVec.removeElementAt(which);
        baseList.remove(which);
        importVec.addElement(tex);
        importList.add(tex.getName());
        addButton.setEnabled(false);
      }
    });
    removeButton.addEventLink(CommandEvent.class, new Object() {
      void processEvent()
      {
        int which = importList.getSelectedIndex();
        if (which < 0)
          return;
        Texture tex = (Texture) importVec.elementAt(which);
        importVec.removeElementAt(which);
        importList.remove(which);
        baseVec.addElement(tex);
        baseList.add(tex.getName());
        removeButton.setEnabled(false);
      }
    });
    removeButton.setEnabled(false);
    ColumnContainer buttons = new ColumnContainer();
    buttons.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null));
    buttons.add(addButton);
    buttons.add(removeButton);
    mainPanel.add(UIUtilities.createScrollingList(baseList), 0, 0);
    mainPanel.add(buttons, 1, 0, new LayoutInfo());
    mainPanel.add(UIUtilities.createScrollingList(importList), 2, 0);
    mainPanel.add(preview, 3, 0);
    ((BScrollPane) baseList.getParent()).setPreferredViewSize(new Dimension(150, 200));
    ((BScrollPane) importList.getParent()).setPreferredViewSize(new Dimension(150, 200));
    PanelDialog dlg = new PanelDialog(this, Translate.text("selectTexturesToImport"), mainPanel);
    if (!dlg.clickedOk())
      return;
    
    // Copy the selected textures, along with any images they use.
    
    boolean imageUsed[] = new boolean [sc.getNumImages()];
    for (int i = 0; i < importVec.size(); i++)
    {
      Texture tex = (Texture) importVec.elementAt(i);
      for (int j = 0; j < imageUsed.length; j++)
        if (!imageUsed[j] && tex.usesImage(sc.getImage(j)))
        {
          theScene.addImage(sc.getImage(j));
          imageUsed[j] = true;
        }
      theScene.addTexture(tex);
    }
  }
}

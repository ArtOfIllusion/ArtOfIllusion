/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.material.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/** MaterialsDialog is a dialog box for editing the list of Materials used in a scene. */

public class MaterialsDialog extends BDialog implements ListChangeListener
{
  private Scene theScene;
  private BFrame parent;
  private BList matList;
  private BButton b[];

  public MaterialsDialog(BFrame fr, Scene sc)
  {
    super(fr, Translate.text("materialsTitle"), false);
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
    matList = new BList();
    matList.setMultipleSelectionEnabled(false);
    content.add(UIUtilities.createScrollingList(matList), BorderContainer.CENTER);
    for (int i = 0; i < theScene.getNumMaterials(); i++)
      matList.add(theScene.getMaterial(i).getName());
    matList.addEventLink(SelectionChangedEvent.class, this, "hilightButtons");
    matList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    sc.addMaterialListener(this);
    addEventLink(WindowClosingEvent.class, this, "dispose");
    hilightButtons();
    pack();
    UIUtilities.centerDialog(this, parent);
  }

  public void dispose()
  {
    theScene.removeMaterialListener(this);
    super.dispose();
  }

  void hilightButtons()
  {
    boolean selection = (matList.getSelectedIndex() != -1);
    
    b[1].setEnabled(selection);
    b[2].setEnabled(selection);
    b[4].setEnabled(selection);
  }
  
  private void mouseClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() == 2 && matList.getSelectedIndex() != -1)
      doEdit();
  }

  private void doNew()
  {
    showNewMaterialWindow(this, theScene);
  }
  
  private void doCopy()
  {
    String name = new BStandardDialog("", Translate.text("newTexName"), BStandardDialog.PLAIN).showInputDialog(this, null, "");
    if (name == null)
      return;
    Material tex = theScene.getMaterial(matList.getSelectedIndex()).duplicate();
    tex.setName(name);
    theScene.addMaterial(tex);
  }
  
  private void doDelete()
  {
    int selected = matList.getSelectedIndex();
    Material tex = theScene.getMaterial(selected);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    int choice = new BStandardDialog("", Translate.text("deleteMaterial", tex.getName()), BStandardDialog.PLAIN).showOptionDialog(this, options, options[1]);
    if (choice == 0)
      theScene.removeMaterial(selected);
  }
  
  private void doEdit()
  {
    int selected = matList.getSelectedIndex();
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    Material tex = theScene.getMaterial(selected);
    tex.edit(parent, theScene);
    tex.assignNewID();
    theScene.changeMaterial(selected);
    setCursor(Cursor.getDefaultCursor());
  }
  
  /** Add a material to the list. */
  
  public void itemAdded(int index, Object obj)
  {
    Material mat = (Material) obj;

    matList.add(index, mat.getName());
    matList.setSelected(matList.getSelectedIndex(), false);
    matList.setSelected(index, true);
    hilightButtons();
    if (parent instanceof LayoutWindow)
    {
      ((LayoutWindow) parent).setModified();
      ((LayoutWindow) parent).updateImage();
    }
  }
  
  /** Remove a material from the list. */
  
  public void itemRemoved(int index, Object obj)
  {
    matList.removeAll();
    for (int j = 0; j < theScene.getNumMaterials(); j++)
      matList.add(theScene.getMaterial(j).getName());
    hilightButtons();
    if (parent instanceof LayoutWindow)
    {
      ((LayoutWindow) parent).setModified();
      ((LayoutWindow) parent).updateImage();
    }
  }
  
  /** Change the name of a material in the list. */
  
  public void itemChanged(int index, Object obj)
  {
    Material mat = (Material) obj;
    
    matList.replace(index, mat.getName());
    matList.setSelected(index, true);
    if (parent instanceof LayoutWindow)
    {
      ((LayoutWindow) parent).setModified();
      ((LayoutWindow) parent).updateImage();
    }
  }

  /** Display a window in which the user can create a new material. */
  
  public static Material showNewMaterialWindow(WindowWidget parent, Scene theScene)
  {
    BTextField nameField = new BTextField();
    BComboBox typeChoice = new BComboBox();
    List<Material> materialTypes = PluginRegistry.getPlugins(Material.class);
    java.lang.reflect.Method mtd;
    for (int j = 0; j < materialTypes.size(); j++)
    {
      try
      {
        mtd = materialTypes.get(j).getClass().getMethod("getTypeName", null);
        typeChoice.add((String) mtd.invoke(null, null));
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    int j = 0, k = -1;
    String name = "";
    do
      {
        j++;
        name = "Untitled "+j;
      } while (theScene.getMaterial(name) != null);
    nameField.setText(name);
    nameField.setSelectionStart(0);
    nameField.setSelectionEnd(name.length());
    ComponentsDialog dlg = new ComponentsDialog(parent, Translate.text("newMatNameAndType"),
            new Widget [] {nameField, typeChoice}, new String[] {Translate.text("Name"), Translate.text("Type")});
    if (dlg.clickedOk())
    {
      Widget frame = parent;
      while (!(frame instanceof BFrame))
        frame = frame.getParent();
      try
      {
        Material mat = materialTypes.get(typeChoice.getSelectedIndex()).getClass().newInstance();
        mat.setName(nameField.getText());
        theScene.addMaterial(mat);
        mat.edit((BFrame) frame, theScene);
        return mat;
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    return null;
  }
  
  /** Ask the user to select materials to import from a scene. */
  
  private void doImport()
  {
    // First prompt them to select a file.
    
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("importMaterials"));
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
    if (sc.getNumMaterials() == 0)
    {
      new BStandardDialog("", Translate.text("noMaterialsError"), BStandardDialog.INFORMATION).showMessageDialog(this);
      return;
    }
    
    // Now create the main dialog.
    
    FormContainer mainPanel = new FormContainer(4, 1);
    mainPanel.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    final BList baseList = new BList(), importList = new BList();
    final BButton addButton = new BButton(Translate.text("Add")+" >>");
    final BButton removeButton = new BButton("<< "+Translate.text("Remove"));
    final Vector baseVec = new Vector(), importVec = new Vector();
    final MaterialPreviewer preview = new MaterialPreviewer(null, sc.getMaterial(0), 160, 160);
    baseList.setMultipleSelectionEnabled(false);
    importList.setMultipleSelectionEnabled(false);
    for (int i = 0; i < sc.getNumMaterials(); i++)
    {
      baseVec.addElement(sc.getMaterial(i));
      baseList.add(sc.getMaterial(i).getName());
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
          Material mat = (Material) baseVec.elementAt(baseList.getSelectedIndex());
          preview.setMaterial(mat, mat.getDefaultMapping(preview.getObject().getObject()));
          preview.render();
        }
	else if (importList.getSelectedIndex() > -1)
        {
          if (baseList.getSelectedIndex() > -1)
            baseList.setSelected(baseList.getSelectedIndex(), false);
          addButton.setEnabled(false);
          removeButton.setEnabled(true);
          Material mat = (Material) importVec.elementAt(importList.getSelectedIndex());
          preview.setMaterial(mat, mat.getDefaultMapping(preview.getObject().getObject()));
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
        Material mat = (Material) baseVec.elementAt(which);
        baseVec.removeElementAt(which);
        baseList.remove(which);
        importVec.addElement(mat);
        importList.add(mat.getName());
        addButton.setEnabled(false);
      }
    });
    removeButton.addEventLink(CommandEvent.class, new Object() {
      void processEvent()
      {
        int which = importList.getSelectedIndex();
        if (which < 0)
          return;
        Material mat = (Material) importVec.elementAt(which);
        importVec.removeElementAt(which);
        importList.remove(which);
        baseVec.addElement(mat);
        baseList.add(mat.getName());
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
    PanelDialog dlg = new PanelDialog(this, Translate.text("selectMaterialsToImport"), mainPanel);
    if (!dlg.clickedOk())
      return;
    
    // Copy the selected materials, along with any images they use.
    
    boolean imageUsed[] = new boolean [sc.getNumImages()];
    for (int i = 0; i < importVec.size(); i++)
    {
      Material mat = (Material) importVec.elementAt(i);
      for (int j = 0; j < imageUsed.length; j++)
        if (!imageUsed[j] && mat.usesImage(sc.getImage(j)))
        {
          theScene.addImage(sc.getImage(j));
          imageUsed[j] = true;
        }
      theScene.addMaterial(mat);
    }
  }
}


/* Copyright (C) 2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a window for editing ExternalObjects. */

public class ExternalObjectEditingWindow extends BDialog
{
  EditingWindow parentWindow;
  ExternalObject theObject;
  ObjectInfo info;
  BTextField fileField, nameField;
  
  /** Display a window for editing an ExternalObject.
      @param parent     the parent window
      @param obj        the object to edit
      @param info       the ObjectInfo for the ExternalObject
  */
  
  public ExternalObjectEditingWindow(EditingWindow parent, ExternalObject obj, ObjectInfo info)
  {
    super(parent.getFrame(), info.name, true);
    parentWindow = parent;
    theObject = obj;
    this.info = info;
    FormContainer content = new FormContainer(new double [] {0, 1, 0}, new double [] {1, 1, 1});
    setContent(BOutline.createEmptyBorder(content, ModellingApp.standardDialogInsets));
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null));
    LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(2, 2, 2, 2), null);
    content.add(Translate.label("externalObject.sceneFile"), 0, 0, labelLayout);
    content.add(Translate.label("externalObject.objectName"), 0, 1, labelLayout);
    content.add(fileField = new BTextField(theObject.getExternalSceneFile().getAbsolutePath(), 30), 1, 0);
    content.add(nameField = new BTextField(theObject.getExternalObjectName(), 30), 1, 1);
    content.add(Translate.button("browse", this, "doBrowseFile"), 2, 0);
    content.add(Translate.button("browse", this, "doBrowseObject"), 2, 1);
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 2, 3, 1, new LayoutInfo());
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "dispose"));
    pack();
    UIUtilities.centerDialog(this, parentWindow.getFrame());
    setVisible(true);
  }
  
  /** Allow the user to select a file. */
  
  private void doBrowseFile()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("externalObject.selectScene"));
    File f = theObject.getExternalSceneFile();
    if (f.isFile())
      fc.setSelectedFile(f);
    if (fc.showDialog(this))
      fileField.setText(fc.getSelectedFile().getAbsolutePath());
  }
  
  /** Allow the user to select an object from the file. */
  
  private void doBrowseObject()
  {
    // Load the scene.
    
    File f = theObject.getExternalSceneFile();
    if (!f.isFile())
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("externalObject.sceneNotFound",
          theObject.getExternalSceneFile().getAbsolutePath())), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    Scene sc = null;
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try
    {
      sc = new Scene(f, true);
    }
    catch (InvalidObjectException ex)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingWholeScene")), BStandardDialog.ERROR).showMessageDialog(this);
    }
    catch (IOException ex)
    {
      new BStandardDialog("", new String [] {Translate.text("errorLoadingFile"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
    }
    setCursor(Cursor.getDefaultCursor());
    if (sc == null)
      return;
    
    // Create a tree of all objects in the scene.
    
    TreeList itemTree = new TreeList(parentWindow);
    itemTree.setPreferredSize(new Dimension(130, 100));
    itemTree.setUpdateEnabled(false);
    for (int i = 0; i < sc.getNumObjects(); i++)
    {
      ObjectInfo info = sc.getObject(i);
      if (info.parent == null)
        itemTree.addElement(new ObjectTreeElement(info, itemTree));
    }
    itemTree.setUpdateEnabled(true);
    itemTree.setAllowMultiple(false);
    BScrollPane itemTreeScroller = new BScrollPane(itemTree);
    itemTreeScroller.setForceWidth(true);
    itemTreeScroller.setForceHeight(true);
    itemTreeScroller.getVerticalScrollBar().setUnitIncrement(10);
    ObjectInfo oldSelection = sc.getObject(theObject.getExternalObjectName());
    if (oldSelection != null)
    {
      itemTree.setSelected(oldSelection, true);
      itemTree.expandToShowObject(oldSelection);
    }
    
    // Ask the user to pick an object.
    
    PanelDialog dlg = new PanelDialog(this, Translate.text("externalObject.selectObject"), BOutline.createBevelBorder(itemTreeScroller, false));
    if (dlg.clickedOk())
    {
      Object sel[] = itemTree.getSelectedObjects();
      if (sel.length == 0)
        nameField.setText("");
      else
        nameField.setText(((ObjectInfo) sel[0]).name);
    }
  }
  
  /** Save the changes and reload the object. */
  
  private void doOk()
  {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    theObject.setExternalObjectName(nameField.getText());
    theObject.setExternalSceneFile(new File(fileField.getText()));
    theObject.reloadObject();
    if (theObject.getLoadingError() != null)
      new BStandardDialog("", UIUtilities.breakString(Translate.text("externalObject.loadingError", theObject.getLoadingError())), BStandardDialog.ERROR).showMessageDialog(this);
    info.clearCachedMeshes();
    dispose();
    parentWindow.updateImage();
  }
}


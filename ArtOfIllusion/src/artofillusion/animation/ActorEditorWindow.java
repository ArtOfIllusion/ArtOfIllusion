/* Copyright (C) 2002-2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.animation.Actor.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;

/** The ActorEditorWindow class represents the window for editing Actor objects. */

public class ActorEditorWindow extends BDialog
{
  private Actor theObject, oldObject;
  private ObjectInfo objInfo;
  private EditingWindow theWindow;
  private BList gestureList;
  private BTable currentPoseTable;
  private CurrentPoseTableModel tableModel;
  private BButton addButton, removeButton, saveButton, extractButton;
  private BButton editButton, renameButton, copyButton, deleteButton;
  private BButton okButton, cancelButton;
  private ActionProcessor processor;
  private ActorKeyframe currentPose, key;
  private ObjectPreviewCanvas preview;
  private Runnable onClose;
  private boolean canModifyGestures;

  /** Display a window for editing a pose of an Actor.  If key is null, the
      Actor's current pose will be edited.  Otherwise, the specified keyframe
      will be edited. */

  public ActorEditorWindow(EditingWindow parent, ObjectInfo info, Actor obj, ActorKeyframe key, Runnable cb)
  {
    super(parent.getFrame(), (key == null ? info.getName() : Translate.text("editKeyframeFor", info.getName())), false);
    objInfo = info;
    oldObject = obj;
    theObject = (Actor) obj.duplicate();
    theWindow = parent;
    this.key = key;
    if (key != null)
      theObject.applyPoseKeyframe(key);
    currentPose = (ActorKeyframe) theObject.getPoseKeyframe();
    onClose = cb;
    processor = new ActionProcessor();
    canModifyGestures = (info.getObject() == obj);
    for (int i = currentPose.id.length-1; i >= 0; i--)
    {
      int which = theObject.findPoseIndex(currentPose.id[i]);
      if (which == -1)
        currentPose.deleteGesture(i);
    }

    // Layout the components in the window.

    FormContainer content = new FormContainer(new double [] {0, 0, 1}, new double [] {1, 0, 0});
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));

    // The list of gestures.

    content.add(UIUtilities.createScrollingList(gestureList = new BList()), 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    gestureList.setPreferredVisibleRows(10);
    for (int i = 0; i < theObject.gestureName.length; i++)
      gestureList.add(theObject.gestureName[i]);
    gestureList.setMultipleSelectionEnabled(false);
    gestureList.addEventLink(SelectionChangedEvent.class, this, "updateComponents");
    gestureList.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent(MouseClickedEvent ev)
      {
        if (ev.getClickCount() == 2)
          doEdit();
      }
    });

    // The buttons for editing them.

    FormContainer leftPanel = new FormContainer(new double [] {1}, new double [] {0, 0, 0, 0, 0, 1, 0, 0});
    content.add(leftPanel, 0, 1);
    leftPanel.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null));
    leftPanel.add(editButton = Translate.button("edit", this, "doEdit"), 0, 1);
    leftPanel.add(renameButton = Translate.button("rename", this, "doRename"), 0, 2);
    leftPanel.add(copyButton = Translate.button("duplicate", this, "doDuplicate"), 0, 3);
    leftPanel.add(deleteButton = Translate.button("delete", this, "doDelete"), 0, 4);
    leftPanel.add(saveButton = Translate.button("save", "...", this, "doSave"), 0, 6);
    leftPanel.add(extractButton = Translate.button("extract", "...", this, "doExtract"), 0, 7);

    // The Add and Remove buttons.

    ColumnContainer addRemovePanel = new ColumnContainer();
    content.add(addRemovePanel, 1, 0);
    addRemovePanel.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null));
    addRemovePanel.add(addButton = Translate.button("add", ">>", this, "doAdd"));
    addRemovePanel.add(removeButton = new BButton("<< "+Translate.text("button.remove")));
    removeButton.addEventLink(CommandEvent.class, this, "doRemove");

    // The current pose.

    tableModel = new CurrentPoseTableModel();
    currentPoseTable = new BTable(tableModel);
    currentPoseTable.setMultipleSelectionEnabled(false);
    currentPoseTable.addEventLink(SelectionChangedEvent.class, this, "updateComponents");
    TableColumn column = ((JTable) currentPoseTable.getComponent()).getColumnModel().getColumn(1);
    column.setCellEditor(new ValueEditor());
    ValueRenderer renderer = new ValueRenderer();
    column.setCellRenderer(renderer);
    column.setMinWidth(renderer.getPreferredSize().width);
    column.setMaxWidth(renderer.getPreferredSize().width);
    ((JTable) currentPoseTable.getComponent()).setRowHeight(renderer.getPreferredSize().height);
    BScrollPane tableScrollPane = new BScrollPane(currentPoseTable, BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_ALWAYS);
    tableScrollPane.setPreferredViewSize(new Dimension(250, 5*renderer.getPreferredSize().height));
    content.add(BOutline.createBevelBorder(tableScrollPane, false), 2, 0);

    // The object previewer.

    content.add(preview = new ObjectPreviewCanvas(new ObjectInfo(theObject, new CoordinateSystem(), "")), 1, 1, 2, 1,
        new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    preview.setPreferredSize(new Dimension(300, 300));

    // The buttons at the bottom.

    RowContainer bottomPanel = new RowContainer();
    content.add(bottomPanel, 0, 2, 3, 1, new LayoutInfo());
    bottomPanel.add(okButton = Translate.button("ok", this, "doOk"));
    bottomPanel.add(cancelButton = Translate.button("cancel", this, "dispose"));
    pack();
    UIUtilities.centerDialog(this, parent.getFrame());
    updateComponents();
    setVisible(true);
  }

  /** Add a new gesture to the current pose. */

  private void doAdd()
  {
    currentPose.addGesture(theObject.gestureID[gestureList.getSelectedIndex()], 1.0);
    tableModel.fireTableDataChanged();
    updateComponents();
    updateDisplay();
  }

  /** Remove a gesture from the current pose. */

  private void doRemove()
  {
    currentPose.deleteGesture(currentPoseTable.getSelectedRows()[0]);
    tableModel.fireTableDataChanged();
    updateComponents();
    updateDisplay();
  }

  /** Save the current pose as a new gesture. */

  private void doSave()
  {
    BStandardDialog dlg = new BStandardDialog("", Translate.text("savePoseAsGesture"), BStandardDialog.PLAIN);
    String name = dlg.showInputDialog(this, null, "New Gesture");
    if (name == null)
      return;
    theObject.addGesture((Gesture) theObject.getWrappedObject().getPoseKeyframe(), name);
    gestureList.add(name);
    gestureList.setSelected(gestureList.getItemCount()-1, true);
    updateComponents();
  }

  /** Save the current pose as a new object. */

  private void doExtract()
  {
    BStandardDialog dlg = new BStandardDialog("", Translate.text("savePoseAsObject"), BStandardDialog.PLAIN);
    String name = dlg.showInputDialog(this, null, "Extracted Pose");
    if (name == null)
      return;
    ObjectInfo info = new ObjectInfo(theObject.getWrappedObject().duplicate(),
      new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy()), name);
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    if (theWindow instanceof LayoutWindow)
      ((LayoutWindow) theWindow).addObject(info, null);
    else
      theWindow.getScene().addObject(info, null);
    theWindow.updateImage();
  }

  /** Edit the currently selected gesture. */

  private void doEdit()
  {
    if (canModifyGestures)
      editGesture(gestureList.getSelectedIndex());
  }

  /** Rename a gesture. */

  private void doRename()
  {
    int which = gestureList.getSelectedIndex();
    BStandardDialog dlg = new BStandardDialog("", Translate.text("enterNewNameForGesture"), BStandardDialog.PLAIN);
    String name = dlg.showInputDialog(this, null, theObject.gestureName[which]);
    if (name == null)
      return;
    theObject.gestureName[which] = name;
    gestureList.replace(which, theObject.gestureName[which]);
    gestureList.setSelected(which, true);
  }

  /** Duplicate a gesture. */

  private void doDuplicate()
  {
    int which = gestureList.getSelectedIndex();
    BStandardDialog dlg = new BStandardDialog("", Translate.text("enterNameForNewGesture"), BStandardDialog.PLAIN);
    String name = dlg.showInputDialog(this, null, "Copy of "+theObject.gestureName[which]);
    if (name == null)
      return;
    theObject.addGesture((Gesture) theObject.gesture[which].duplicate(theObject.getWrappedObject()), name);
    gestureList.add(name);
    gestureList.setSelected(gestureList.getItemCount()-1, true);
    updateComponents();
    editGesture(gestureList.getItemCount()-1);
  }

  /** Delete a gesture. */

  private void doDelete()
  {
    int which = gestureList.getSelectedIndex();
    String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
    int choice = new BStandardDialog("", Translate.text("deleteGesture", theObject.gestureName[which]), BStandardDialog.QUESTION).showOptionDialog(this, options, options[0]);
    if (choice== 1)
      return;
    for (int i = 0; i < currentPose.id.length; i++)
      if (currentPose.id[i] == theObject.gestureID[which])
      {
        currentPose.deleteGesture(i);
        i--;
        updateDisplay();
      }
    theObject.deleteGestureWithID(theObject.gestureID[which]);
    gestureList.remove(which);
    tableModel.fireTableDataChanged();
    updateComponents();
  }

  /** Save the edits. */

  private void doOk()
  {
    oldObject.copyObject(theObject);
    if (key != null)
    {
      key.copy(currentPose);
      theWindow.getScene().applyTracksToObject(objInfo);
      theWindow.updateImage();
    }
    dispose();
    if (onClose != null)
      onClose.run();
  }

  /** Display a window to let the user edit a gesture. */

  private void editGesture(int which)
  {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    Object3D editObj = theObject.getWrappedObject().duplicate();
    editObj.applyPoseKeyframe(theObject.gesture[which]);
    EditCallback cb = new EditCallback(editObj, theObject.gestureID[which]);
    ObjectInfo info = objInfo.duplicate(editObj);
    info.setName(theObject.gestureName[which]);
    editObj.editGesture(theWindow, info, cb, objInfo);
    setCursor(Cursor.getDefaultCursor());
  }

  /** Update the status of the buttons and weight field. */

  private void updateComponents()
  {
    int i, id, selected = gestureList.getSelectedIndex();

    if (selected == -1)
    {
      addButton.setEnabled(false);
      editButton.setEnabled(false);
      renameButton.setEnabled(false);
      copyButton.setEnabled(false);
      deleteButton.setEnabled(false);
    }
    else
    {
      editButton.setEnabled(canModifyGestures);
      renameButton.setEnabled(canModifyGestures && selected > 0);
      copyButton.setEnabled(canModifyGestures);
      deleteButton.setEnabled(canModifyGestures && selected > 0);
      id = theObject.gestureID[gestureList.getSelectedIndex()];
      for (i = 0; i < currentPose.id.length && currentPose.id[i] != id; i++);
      addButton.setEnabled(selected > 0 && i == currentPose.id.length);
    }
    int rows[] = currentPoseTable.getSelectedRows();
    removeButton.setEnabled(rows.length > 0);
    saveButton.setEnabled(canModifyGestures);
  }

  /** Reapply the current pose and update the preview. */

  private void updateDisplay()
  {
    theObject.applyPoseKeyframe(currentPose);
    preview.objectChanged();
    preview.repaint();
  }

  /** This is an inner class which is called when a keyframe has been edited. */

  private class EditCallback implements Runnable
  {
    public Object3D editObject;
    public int editID;

    public EditCallback(Object3D obj, int id)
    {
      editObject = obj;
      editID = id;
    }

    public void run()
    {
      int which = theObject.findPoseIndex(editID);
      theObject.getWrappedObject().copyObject(editObject);
      theObject.gesture[which] = (Gesture) theObject.getWrappedObject().getPoseKeyframe();

      // Update the skeletons for all other poses, in case a joint has been added or deleted.

      Skeleton s = theObject.gesture[which].getSkeleton();
      if (s != null)
        for (int i = 0; i < theObject.gesture.length; i++)
        {
          if (i == which)
            continue;
          Skeleton olds = theObject.gesture[i].getSkeleton();
          Skeleton news = s.duplicate();
          Joint joint[] = news.getJoints();
          for (int j = 0; j < joint.length; j++)
          {
            Joint j2 = olds.getJoint(joint[j].id);
            if (j2 != null)
              joint[j].copy(j2);
          }
          theObject.gesture[i].setSkeleton(news);
        }
      updateDisplay();
    }
  }

  /**
   * This is the model for the table describing the current pose.
   */

  private class CurrentPoseTableModel extends AbstractTableModel
  {
    public int getRowCount()
    {
      return currentPose.getNumGestures();
    }

    public int getColumnCount()
    {
      return 2;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      if (columnIndex == 0)
        return theObject.gestureName[theObject.findPoseIndex(currentPose.getGestureID(rowIndex))];
      return new Double(currentPose.getGestureWeight(rowIndex));
    }

    public String getColumnName(int column)
    {
      return (Translate.text(column == 0 ? "Gesture" : "Weight"));
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return (columnIndex == 1);
    }
  }

  /**
   * This is the renderer for values in the Weight column of the table.
   */

  private class ValueRenderer extends BuoyComponent implements TableCellRenderer
  {
    private ValueSelector selector;

    public ValueRenderer()
    {
      super(new ValueSelector(0.0, -Double.MAX_VALUE, Double.MAX_VALUE, 0.005));
      selector = (ValueSelector) getWidget();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      selector.setValue(((Double) value).doubleValue());
      selector.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
      return this;
    }

    public void validate()
    {
      super.validate();
      selector.layoutChildren();
    }
  }

  /**
   * This is the editor for values in the Weight column of the table.
   */

  private class ValueEditor extends AbstractCellEditor implements TableCellEditor
  {
    private ValueSelector selector;
    private BuoyComponent component;
    private int currentRow;

    public ValueEditor()
    {
      selector = new ValueSelector(0.0, -Double.MAX_VALUE, Double.MAX_VALUE, 0.005);
      component = new BuoyComponent(selector) {
        public void validate()
        {
          super.validate();
          selector.layoutChildren();
        }
      };
      selector.setBackground(((JTable) currentPoseTable.getComponent()).getSelectionBackground());
      selector.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
          processor.addEvent(new Runnable() {
            public void run()
            {
              currentPose.weight[currentRow] = selector.getValue();
              updateDisplay();
            }
          });
        }
      });
    }

    public Object getCellEditorValue()
    {
      return new Double(selector.getValue());
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
      selector.setValue(((Double) value).doubleValue());
      currentRow = row;
      return component;
    }
  }
}

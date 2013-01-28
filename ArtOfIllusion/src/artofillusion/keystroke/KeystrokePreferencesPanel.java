/* Copyright (C) 2006-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.keystroke;

import artofillusion.script.*;
import buoy.widget.*;
import buoy.event.*;

import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.text.*;

import artofillusion.ui.*;

/**
 * This class presents a user interface for editing the list of KeystrokeRecords.
 */

public class KeystrokePreferencesPanel extends FormContainer
{
  private ArrayList<KeystrokeRecord> records;
  private BTable table;
  private BButton editButton, addButton, deleteButton;
  private boolean changed;
  private int sortColumn = 1;

  public KeystrokePreferencesPanel()
  {
    super(new double [] {1}, new double [] {1, 0});
    KeystrokeRecord allRecords[] = KeystrokeManager.getAllRecords();
    records = new ArrayList<KeystrokeRecord>(allRecords.length);
    for (int i = 0; i < allRecords.length; i++)
      records.add(allRecords[i]);
    table = new BTable(new KeystrokeTableModel());
    table.setColumnWidth(0, 100);
    table.setColumnWidth(1, 250);
    table.setColumnsReorderable(false);
    table.addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
    table.addEventLink(MouseClickedEvent.class, this, "tableClicked");
    table.getTableHeader().addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent(MouseClickedEvent ev)
      {
        int col = table.findColumn(ev.getPoint());
        if (col > -1)
        {
          sortColumn = col;
          sortRecords();
        }
      }
    });
    BScrollPane scroll = new BScrollPane(table, BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_AS_NEEDED);
    scroll.setPreferredViewSize(new Dimension(350, table.getRowCount() == 0 ? 150 : 15*table.getRowHeight(0)));
    add(scroll, 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    RowContainer buttons = new RowContainer();
    add(buttons, 0, 1, new LayoutInfo());
    buttons.add(editButton = Translate.button("edit", "...", this, "editRecord"));
    buttons.add(addButton = Translate.button("add", "...", this, "addRecord"));
    buttons.add(deleteButton = Translate.button("delete", this, "deleteRecords"));
    selectionChanged();
  }

  /**
   * Save the changes.
   */

  public void saveChanges()
  {
    if (!changed)
      return;
    KeystrokeManager.setAllRecords((KeystrokeRecord []) records.toArray(new KeystrokeRecord [records.size()]));
    try
    {
      KeystrokeManager.saveRecords();
    }
    catch (Exception ex)
    {
      new BStandardDialog("", Translate.text("errorSavingPrefs", ex.getMessage() == null ? "" : ex.getMessage()), BStandardDialog.ERROR).showMessageDialog(this);
    }
  }

  /**
   * This is called when the selection in the table changes.
   */

  private void selectionChanged()
  {
    int count = table.getSelectedRows().length;
    editButton.setEnabled(count == 1);
    deleteButton.setEnabled(count > 0);
  }

  /**
   * This is called when the user clicks on the table.
   */

  private void tableClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() != 2 || ev.getModifiersEx() != 0)
      return;
    editRecord();
  }

  /**
   * Edit the selected record.
   */

  private void editRecord()
  {
    int row = table.getSelectedRows()[0];
    KeystrokeRecord record = (KeystrokeRecord) records.get(row);
    KeystrokeRecord edited = KeystrokeEditor.showEditorDialog(record, UIUtilities.findWindow(this));
    if (edited == null)
      return;
    records.set(row, edited);
    sortRecords();
    changed = true;
  }

  /**
   * Add a new record.
   */

  private void addRecord()
  {
    KeystrokeRecord record = new KeystrokeRecord(0, 0, "", "", ScriptRunner.LANGUAGES[0]);
    KeystrokeRecord edited = KeystrokeEditor.showEditorDialog(record, UIUtilities.findWindow(this));
    if (edited == null)
      return;
    records.add(edited);
    sortRecords();
    changed = true;
  }

  /**
   * Delete the selected records.
   */

  private void deleteRecords()
  {
    int selected[] = table.getSelectedRows();
    Arrays.sort(selected);
    for (int i = 0; i < selected.length; i++)
      records.remove(selected[i]);
    sortRecords();
    changed = true;
  }

  /**
   * Resort the list of records by name.
   */

  private void sortRecords()
  {
    final Comparator stringComparator = Collator.getInstance(Translate.getLocale());
    Collections.sort(records, new Comparator<KeystrokeRecord>()
    {
      public int compare(KeystrokeRecord r1, KeystrokeRecord r2)
      {
        String s1, s2;
        if (sortColumn == 0)
        {
          s1 = getKeyDescription(r1.getKeyCode(), r1.getModifiers());
          s2 = getKeyDescription(r2.getKeyCode(), r2.getModifiers());
        }
        else
        {
          s1 = r1.getName();
          s2 = r2.getName();
        }
        return stringComparator.compare(s1, s2);
      }
    });
    ((KeystrokeTableModel) table.getModel()).fireTableDataChanged();
  }

  /**
   * Get a string describing a keystroke.
   */

  static String getKeyDescription(int code, int modifiers)
  {
    if (code == 0)
      return "";
    String keyDesc = KeyEvent.getKeyText(code);
    if (modifiers != 0)
      keyDesc = KeyEvent.getKeyModifiersText(modifiers)+"+"+keyDesc;
    return keyDesc;
  }

  /**
   * This is the model for the table of keystrokes.
   */

  private class KeystrokeTableModel extends AbstractTableModel
  {
    public int getRowCount()
    {
      return records.size();
    }

    public int getColumnCount()
    {
      return 2;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      KeystrokeRecord record = (KeystrokeRecord) records.get(rowIndex);
      if (columnIndex == 1)
        return record.getName();
      return getKeyDescription(record.getKeyCode(), record.getModifiers());
    }

    public String getColumnName(int column)
    {
      return (column == 1 ? Translate.text("Name") : Translate.text("Key"));
    }
  }
}

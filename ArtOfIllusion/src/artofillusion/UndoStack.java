/* Copyright (C) 1999-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import java.util.*;

/** This class maintains a stack of UndoRecords for a window.  It also automatically
    records the redo records generated when they are executed. */

public class UndoStack
{
  private LinkedList<UndoRecord> undoList, redoList;
  
  public UndoStack()
  {
    undoList = new LinkedList<UndoRecord>();
    redoList = new LinkedList<UndoRecord>();
  }
  
  /**
   * Determine whether there are any undo records available, so that an Undo command
   * could be executed.
   */
  
  public boolean canUndo()
  {
    return (undoList.size() > 0);
  }
  
  /**
   * Determine whether there are any redo records available, so that a Redo command
   * could be executed.
   */
  
  public boolean canRedo()
  {
    return (redoList.size() > 0);
  }
  
  /**
   * Add an UndoRecord to the stack.
   */
  
  public void addRecord(UndoRecord record)
  {
    int levels = ArtOfIllusion.getPreferences().getUndoLevels();
    if (levels < 1)
      levels = 1;
    while (undoList.size() >= levels)
      undoList.removeFirst();
    undoList.add(record);
    redoList.clear();
    record.cacheToDisk();
  }
  
  /**
   * Execute the undo record at the top of the stack.
   */
  
  public void executeUndo()
  {
    if (undoList.size() == 0)
      return;
    UndoRecord record = (UndoRecord) undoList.removeLast();
    redoList.add(record.execute());
  }
  
  /**
   * Execute the redo record at the top of the stack.
   */
  
  public void executeRedo()
  {
    if (redoList.size() == 0)
      return;
    UndoRecord record = (UndoRecord) redoList.removeLast();
    undoList.add(record.execute());
  }
}

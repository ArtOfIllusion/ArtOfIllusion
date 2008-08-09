/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import java.util.Vector;

/** The UndoRecord class records a series of commands, allowing the user to undo a previous
    action. */

public class UndoRecord
{
  Vector<Integer> command;
  Vector<Object[]> data;
  boolean redo;
  EditingWindow theWindow;
  
  public static final int COPY_OBJECT = 0;
  public static final int COPY_COORDS = 1;
  public static final int COPY_OBJECT_INFO = 2;
  public static final int SET_OBJECT = 3;
  public static final int ADD_OBJECT = 4;
  public static final int DELETE_OBJECT = 5;
  public static final int RENAME_OBJECT = 6;
  public static final int ADD_TO_GROUP = 7;
  public static final int REMOVE_FROM_GROUP = 8;
  public static final int SET_GROUP_CONTENTS = 9;
  public static final int SET_TRACK = 10;
  public static final int SET_TRACK_LIST = 11;
  public static final int COPY_TRACK = 12;
  public static final int COPY_VERTEX_POSITIONS = 13;
  public static final int COPY_SKELETON = 14;
  public static final int SET_MESH_SELECTION = 15;
  public static final int SET_SCENE_SELECTION = 16;

  public UndoRecord(EditingWindow win, boolean isRedo)
  {
    theWindow = win;
    redo = isRedo;
    command = new Vector<Integer>();
    data = new Vector<Object[]>();
  }
  
  public UndoRecord(EditingWindow win, boolean isRedo, int theCommand, Object commandData[])
  {
    this(win, isRedo);
    addCommand(theCommand, commandData);
  }
  
  public boolean isRedo()
  {
    return redo;
  }
  
  public void addCommand(int theCommand, Object commandData[])
  {
    command.addElement(theCommand);
    data.addElement(commandData);
  }
  
  public void addCommandAtBeginning(int theCommand, Object commandData[])
  {
    command.insertElementAt(theCommand, 0);
    data.insertElementAt(commandData, 0);
  }
  
  public UndoRecord execute()
  {
    int i, c;
    Object d[];
    UndoRecord redoRecord = new UndoRecord(theWindow, !redo);
    int selection[] = theWindow.getScene().getSelection();
    boolean needRestoreSelection = false;
    
    for (i = 0; i < command.size(); i++)
      {
        c = ((Integer) command.elementAt(i)).intValue();
        d = (Object []) data.elementAt(i);
        switch (c)
        {
          case COPY_OBJECT:
            {
              Object3D obj1 = (Object3D) d[0], obj2 = (Object3D) d[1];
              redoRecord.addCommandAtBeginning(COPY_OBJECT, new Object [] {obj1, obj1.duplicate()});
              obj1.copyObject(obj2);
              if (theWindow.getScene() != null)
                theWindow.getScene().objectModified(obj1);
              break;
            }
          case COPY_COORDS:
            {
              CoordinateSystem coords1 = (CoordinateSystem) d[0], coords2 = (CoordinateSystem) d[1];
              redoRecord.addCommandAtBeginning(COPY_COORDS, new Object [] {coords1, coords1.duplicate()});
              coords1.copyCoords(coords2);
              break;
            }
          case COPY_OBJECT_INFO:
            {
              ObjectInfo info1 = (ObjectInfo) d[0], info2 = (ObjectInfo) d[1];
              redoRecord.addCommandAtBeginning(COPY_OBJECT_INFO, new Object [] {info1, info1.duplicate()});
              info1.copyInfo(info2);
              break;
            }
          case SET_OBJECT:
            {
              ObjectInfo info = (ObjectInfo) d[0];
              Object3D obj = (Object3D) d[1];
              redoRecord.addCommandAtBeginning(SET_OBJECT, new Object [] {info, info.getObject()});
              info.setObject(obj);
              break;
            }
          case ADD_OBJECT:
            {
              ObjectInfo info = (ObjectInfo) d[0];
              LayoutWindow win = (LayoutWindow) theWindow;
              int index = ((Integer) d[1]).intValue();
              win.addObject(info, index, redoRecord);
              if (info.selected)
                win.getScene().addToSelection(index);
              needRestoreSelection = true;
              break;
            }
          case DELETE_OBJECT:
            {
              int which = ((Integer) d[0]).intValue();
              LayoutWindow win = (LayoutWindow) theWindow;
              win.removeObject(which, redoRecord);
              needRestoreSelection = true;
              break;
            }
          case RENAME_OBJECT:
            {
              LayoutWindow win = (LayoutWindow) theWindow;
              int which = ((Integer) d[0]).intValue();
              String oldName = (win.getScene().getObject(which)).getName();
              redoRecord.addCommandAtBeginning(RENAME_OBJECT, new Object [] {d[0], oldName});
              win.setObjectName(which, (String) d[1]);
              break;
            }
          case ADD_TO_GROUP:
            {
              int pos = ((Integer) d[2]).intValue();
              ObjectInfo group = (ObjectInfo) d[0];
              ObjectInfo child = (ObjectInfo) d[1];
              redoRecord.addCommandAtBeginning(REMOVE_FROM_GROUP, new Object [] {group, child});
              group.addChild(child, pos);
              break;
            }
          case REMOVE_FROM_GROUP:
            {
              ObjectInfo group = (ObjectInfo) d[0];
              ObjectInfo child = (ObjectInfo) d[1];
              int pos;
              for (pos = 0; pos < group.getChildren().length && group.getChildren()[pos] != child; pos++);
              if (pos == group.getChildren().length)
                break;
              redoRecord.addCommandAtBeginning(ADD_TO_GROUP, new Object [] {group, child, new Integer(pos)});
              group.removeChild(child);
              break;
            }
          case SET_GROUP_CONTENTS:
            {
              ObjectInfo group = (ObjectInfo) d[0];
              ObjectInfo oldobj[] = group.getChildren();
              ObjectInfo newobj[] = (ObjectInfo []) d[1];
              redoRecord.addCommandAtBeginning(SET_GROUP_CONTENTS, new Object [] {group, oldobj});
              for (int j = 0; j < oldobj.length; j++)
                oldobj[j].setParent(null);
              for (int j = 0; j < newobj.length; j++)
                newobj[j].setParent(group);
              group.children = newobj;
              break;
            }
          case SET_TRACK:
            {
              ObjectInfo info = (ObjectInfo) d[0];
              int which = ((Integer) d[1]).intValue();
              redoRecord.addCommandAtBeginning(SET_TRACK, new Object [] {info, d[1], info.getTracks()[which]});
              info.getTracks()[which] = (Track) d[2];
              break;
            }
          case SET_TRACK_LIST:
            {
              ObjectInfo info = (ObjectInfo) d[0];
              redoRecord.addCommandAtBeginning(SET_TRACK_LIST, new Object [] {info, info.getTracks()});
              info.tracks = (Track []) d[1];
              break;
            }
          case COPY_TRACK:
            {
              Track tr1 = (Track) d[0];
              Track tr2 = (Track) d[1];
              redoRecord.addCommandAtBeginning(COPY_TRACK, new Object [] {tr1, tr1.duplicate(tr1.getParent())});
              tr1.copy(tr2);
              break;
            }
          case COPY_VERTEX_POSITIONS:
            {
              Mesh mesh = (Mesh) d[0];
              Vec3 pos[] = (Vec3 []) d[1];
              redoRecord.addCommandAtBeginning(COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
              mesh.setVertexPositions(pos);
              if (theWindow.getScene() != null)
                theWindow.getScene().objectModified((Object3D) mesh);
              break;
            }
          case COPY_SKELETON:
            {
              Skeleton s1 = (Skeleton) d[0], s2 = (Skeleton) d[1];
              redoRecord.addCommandAtBeginning(COPY_SKELETON, new Object [] {s1, s1.duplicate()});
              s1.copy(s2);
              break;
            }
          case SET_MESH_SELECTION:
            {
              MeshEditController controller = (MeshEditController) d[0];
              int mode = ((Integer) d[1]).intValue();
              boolean selected[] = (boolean []) d[2];
              redoRecord.addCommandAtBeginning(SET_MESH_SELECTION, new Object [] {controller, new Integer(controller.getSelectionMode()), controller.getSelection().clone()});
              controller.setSelectionMode(mode);
              controller.setSelection(selected);
              break;
            }
          case SET_SCENE_SELECTION:
            {
              int selected[] = (int []) d[0];
              needRestoreSelection = true;
              if (theWindow instanceof LayoutWindow)
                ((LayoutWindow) theWindow).setSelection(selected);
              else
                theWindow.getScene().setSelection(selected);
              break;
            }
        }
      }
    if (needRestoreSelection)
      redoRecord.addCommand(SET_SCENE_SELECTION, new Object [] {selection});
    theWindow.setModified();
    return redoRecord;
  }
}
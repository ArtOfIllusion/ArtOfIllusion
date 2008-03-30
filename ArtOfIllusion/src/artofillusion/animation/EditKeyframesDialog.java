/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.text.*;
import java.util.*;

/** EditKeyframesDialog presents a dialog box for doing bulk editing of keyframes.  It is used 
    for the Move, Copy, Rescale, Loop, and Delete commands */

public class EditKeyframesDialog
{
  LayoutWindow window;
  Scene theScene;
  BComboBox tracksChoice;
  ValueField startField, endField, extraField; 
  BButton okButton, cancelButton;
  int operation;
  
  public static final int MOVE = 0;
  public static final int COPY = 1;
  public static final int RESCALE = 2;
  public static final int LOOP = 3;
  public static final int DELETE = 4;

  private static final String opTitle[] = new String [] {
      Translate.text("Move"),
      Translate.text("Copy"),
      Translate.text("Rescale"),
      Translate.text("Loop"),
      Translate.text("Delete")
  };

  public EditKeyframesDialog(LayoutWindow win, int operation)
  {
    window = win;
    this.operation = operation;
    theScene = window.getScene();
    
    // Layout the dialog.

    ColumnContainer content = new ColumnContainer();
    RowContainer row;
    content.add(row = new RowContainer());
    row.add(Translate.label("applyTo"));
    row.add(tracksChoice = new BComboBox(new String [] {
      Translate.text("allTracks"),
      Translate.text("allTracksSelectedObjects"),
      Translate.text("selectedTracks")
    }));
    if (window.getScore().getSelectedTracks().length > 0)
      tracksChoice.setSelectedIndex(2);
    else if (theScene.getSelection().length > 0)
      tracksChoice.setSelectedIndex(1);
    content.add(row = new RowContainer());
    row.add(new BLabel(opTitle[operation]+" "+Translate.text("keyframesBetweenTime")));
    row.add(startField = new ValueField(0.0, ValueField.NONE, 5));
    content.add(row = new RowContainer());
    row.add(Translate.label("andTime"));
    row.add(endField = new ValueField(0.0, ValueField.NONE, 5));
    content.add(row = new RowContainer());
    if (operation == MOVE || operation == COPY)
      row.add(Translate.label("to time"));
    else if (operation == RESCALE)
      row.add(Translate.label("byFactorOf"));
    else if (operation == LOOP)
      row.add(Translate.label("numLoopTimes"));
    if (operation == LOOP)
      extraField = new ValueField(2.0, ValueField.INTEGER+ValueField.POSITIVE, 5);
    else
      extraField = new ValueField(1.0, ValueField.NONE, 5);
    if (operation != DELETE)
      row.add(extraField);
    
    // Show the dialog.
    
    do
    {
      PanelDialog dlg = new PanelDialog(win, opTitle[operation]+" "+Translate.text("Keyframes"), content);
      if (!dlg.clickedOk())
        return;
    } while (!doEdit());
  }
  
  /** Perform the operation.  Returns true if it was successful, false if it was canceled. */
  
  private boolean doEdit()
  {
    int whichTracks = tracksChoice.getSelectedIndex();
    double fps = (double) theScene.getFramesPerSecond();
    Track track[];
    
    // Find which tracks to apply the operation to.
    
    if (whichTracks == 0)
      {
        Vector tracks = new Vector();
        for (int i = 0; i < theScene.getNumObjects(); i++)
          {
            ObjectInfo info = theScene.getObject(i);
            for (int j = 0; j < info.getTracks().length; j++)
              addToVector(info.getTracks()[j], tracks);
          }
        track = new Track [tracks.size()];
        for (int i = 0; i < track.length; i++)
          track[i] = (Track) tracks.elementAt(i);
      }
    else if (whichTracks == 1)
      {
        int sel[] = theScene.getSelection();
        Vector tracks = new Vector();
        for (int i = 0; i < sel.length; i++)
          {
            ObjectInfo info = theScene.getObject(sel[i]);
            for (int j = 0; j < info.getTracks().length; j++)
              addToVector(info.getTracks()[j], tracks);
          }
        track = new Track [tracks.size()];
        for (int i = 0; i < track.length; i++)
          track[i] = (Track) tracks.elementAt(i);
      }
    else
      track = window.getScore().getSelectedTracks();
    
    // Find the range of times affected by the operation.
    
    double start = startField.getValue(), end = endField.getValue();
    double extra = extraField.getValue(), scaleStart = 0.0, scaleFactor = 0.0;
    int numLoops = 0;
    
    if (start > end)
      {
        double temp = start;
        start = end;
        end = temp;
      }
    double targetStart, targetEnd;
    if (operation == MOVE || operation == COPY)
      {
        targetStart = extra;
        targetEnd = targetStart + (end-start);
        if (targetStart > start && targetStart < end)
          targetStart = end;
        if (targetEnd > start && targetEnd < end)
          targetEnd = start;
      }
    else if (operation == RESCALE)
      {
        scaleFactor = extra;
        targetStart = end;
        targetEnd = start + Math.abs(scaleFactor)*(end-start);
        if (scaleFactor < 0.0)
          scaleStart = targetEnd;
        else
          scaleStart = start;
        if (targetEnd < targetStart)
          targetEnd = targetStart;
      }
    else if (operation == LOOP)
      {
        numLoops = (int) extra;
        targetStart = end;
        targetEnd = start + numLoops*(end-start);
      }
    else
      targetStart = targetEnd = start;
    
    // Determine whether there are any existing keyframes in the target range.
    
    boolean any = false;
    for (int i = 0; !any && i < track.length; i++)
      {
        double t[] = track[i].getKeyTimes();
        for (int j = 0; !any && j < t.length; j++)
          if (t[j] > targetStart && t[j] < targetEnd)
            any = true;
      }
    if (any)
      {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(3);
        String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
        int choice = new BStandardDialog("", UIUtilities.breakString(Translate.text("existingKeyframesError", nf.format(targetStart), nf.format(targetEnd))), BStandardDialog.QUESTION).showOptionDialog(window, options, options[0]);
        if (choice == 1)
          return false;
      }
    
    // Prepare an undo record.
    
    UndoRecord undo = new UndoRecord(window, false);
    for (int i = 0; i < track.length; i++)
      {
        Object parent = track[i].getParent();
        Track dup;
        if (parent instanceof ObjectInfo)
          dup = track[i].duplicate((ObjectInfo) parent);
        else
          dup = track[i].duplicate(null);
        undo.addCommand(UndoRecord.COPY_TRACK, new Object [] {track[i], dup});
      }
    
    // Perform the operation.
    
    for (int i = 0; i < track.length; i++)
      {
        // Delete existing keyframes in the target region.
        
        double t[] = track[i].getKeyTimes();
        for (int j = t.length-1; j >= 0; j--)
          if (t[j] > targetStart && t[j] < targetEnd)
            track[i].deleteKeyframe(j);
        
        // Create the new keyframes.
        
        Timecourse tc = track[i].getTimecourse();
        if (tc == null)
          continue;
        Keyframe key[] = tc.getValues();
        Smoothness s[] = tc.getSmoothness();
        t = track[i].getKeyTimes();
        if (operation == MOVE || operation == RESCALE || operation == DELETE)
          for (int j = t.length-1; j >=0 ; j--)
            if (t[j] >= start && t[j] <= end)
              track[i].deleteKeyframe(j);
        if (operation == MOVE || operation == COPY)
          for (int j = 0; j < t.length; j++)
            if (t[j] >= start && t[j] <= end)
              track[i].setKeyframe(roundTime(t[j]-start+extra, fps), key[j].duplicate(), s[j]);
        if (operation == RESCALE)
          for (int j = 0; j < t.length; j++)
            if (t[j] >= start && t[j] <= end)
              track[i].setKeyframe(roundTime(scaleStart+scaleFactor*(t[j]-start), fps), key[j].duplicate(), s[j]);
        if (operation == LOOP)
          for (int j = 0; j < t.length; j++)
            if (t[j] >= start && t[j] <= end)
              for (int k = 1; k < numLoops; k++)
                track[i].setKeyframe(roundTime(t[j]+k*(end-start), fps), key[j].duplicate(), s[j]);
      }
    window.getScore().rebuildList();
    window.setTime(window.getScene().getTime());
    window.setUndoRecord(undo);
    return true;
  }
  
  /* Given a Track, add it and all of its subtracks to the specified vector. */
  
  private void addToVector(Track tr, Vector v)
  {
    Track sub[] = tr.getSubtracks();
    
    v.addElement(tr);
    for (int i = 0; i < sub.length; i++)
      addToVector(sub[i], v);
  }
  
  /* Round a time to the nearest frame. */
  
  private double roundTime(double t, double fps)
  {
    return Math.round(t*fps)/fps;
  }
}

/* Copyright (C) 2001-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.text.*;
import java.util.*;

/** This is the Widget which displays all of the tracks in the score. */

public class TracksPanel extends CustomWidget implements TrackDisplay
{
  LayoutWindow window;
  TreeList theList;
  Score theScore;
  double start, scale, dragKeyTime[];
  int subdivisions, mode, effectiveMode;
  Point lastPos, dragPos;
  boolean draggingBox;
  int yoffset;
  Vector<Marker> markers;
  UndoRecord undo;
  
  private static final Polygon handle;

  private static final int HANDLE_SIZE = 7;

  static
  {
    handle = new Polygon(new int [] {-HANDLE_SIZE/2, 0, HANDLE_SIZE/2, 0},
        new int [] {0, HANDLE_SIZE/2, 0, -HANDLE_SIZE/2, 0}, 4);
  }

  public TracksPanel(LayoutWindow win, TreeList list, Score sc, int subdivisions, double scale)
  {
    window = win;
    theList = list;
    theScore = sc;
    this.subdivisions = subdivisions;
    this.scale = scale;
    markers = new Vector<Marker>();
    setPreferredSize(new Dimension(200, 100));
    addEventLink(MousePressedEvent.class, this, "mousePressed");
    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
    addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    addEventLink(RepaintEvent.class, this, "paint");
  }
  
  /** Set the starting time to display. */
  
  public void setStartTime(double time)
  {
    start = time;
  }
  
  /** Set the number of pixels per unit time. */
  
  public void setScale(double s)
  {
    scale = s;
  }
  
  /** Set the number of subdivisions per unit time. */
  
  public void setSubdivisions(int s)
  {
    subdivisions = s;
  }
  
  /*8 Set the y offset (for vertically scrolling the panel). */
  
  public void setYOffset(int offset)
  {
    yoffset = offset;
  }
  
  /*8 Add a marker to the display. */
  
  public void addMarker(Marker m)
  {
    markers.addElement(m);
  }

  /*8 Set the mode (select-and-move or scroll-and-scale) for this display. */
  
  public void setMode(int m)
  {
    mode = m;
  }

  private void paint(RepaintEvent ev)
  {
    Graphics2D g = ev.getGraphics();
    Object obj[] = theList.getVisibleObjects();
    SelectionInfo selection[] = theScore.getSelectedKeyframes();
    Rectangle dim = getBounds();
    int rowHeight = theList.getRowHeight();
    int x, y, i, j, k;
    
    for (i = 0; i < obj.length; i++)
    {
      if (obj[i] instanceof Track)
        g.setColor(Color.lightGray);
      else
        g.setColor(Color.darkGray);
      y = yoffset+i*rowHeight;
      g.fillRect(0, y, dim.width, rowHeight-2);
      if (!(obj[i] instanceof Track))
        continue;
      Track tr = (Track) obj[i];
      if (tr.getTimecourse() == null)
        continue;
      double t[] = tr.getKeyTimes();
      Keyframe key[] = tr.getTimecourse().getValues();
      g.setColor(Color.black);
      for (j = 0; j < t.length; j++)
      {
        for (k = 0; k < selection.length && selection[k].key != key[j]; k++);
        if (k < selection.length)
          g.setColor(Color.red);
        x = (int) Math.round(scale*(t[j]-start));
        handle.translate(x, y+rowHeight/2);
        g.drawPolygon(handle);
        handle.translate(-x, -y-rowHeight/2);
        if (k < selection.length)
          g.setColor(Color.black);
      }
    }
    
    // Draw the markers.
    
    for (i = 0; i < markers.size(); i++)
    {
      Marker m = markers.elementAt(i);
      g.setColor(m.color);
      x = (int) Math.round(scale*(m.position-start));
      g.drawLine(x, 0, x, dim.height);
    }

    // If a drag is in progress, draw a box.

    if (dragPos != null)
    {
      g.setColor(Color.BLACK);
      g.drawRect(Math.min(lastPos.x, dragPos.x), Math.min(lastPos.y, dragPos.y),
        Math.abs(dragPos.x-lastPos.x), Math.abs(dragPos.y-lastPos.y));
    }
  }
  
  /** Record the times of any selected keyframes. */
  
  private void findInitialKeyTimes()
  {
    SelectionInfo sel[] = theScore.getSelectedKeyframes();
    dragKeyTime = new double [sel.length];
    for (int i = 0; i < sel.length; i++)
      {
	double t[] = sel[i].track.getKeyTimes();
	dragKeyTime[i] = t[sel[i].keyIndex];
      }
  }
  
  private void mousePressed(MousePressedEvent ev)
  {
    lastPos = ev.getPoint();
    int rowHeight = theList.getRowHeight(), y = lastPos.y-yoffset, row = y/rowHeight;
    Object obj[] = theList.getVisibleObjects();
    
    undo = null;
    dragPos = null;
    draggingBox = false;
    effectiveMode = (ev.isMetaDown() ? Score.SCROLL_AND_SCALE : mode);
    if (effectiveMode != Score.SELECT_AND_MOVE)
      return;
    if (row < 0 || row >= obj.length || !(obj[row] instanceof Track))
    {
      if (!ev.isShiftDown())
        theScore.setSelectedKeyframes(new SelectionInfo [0]);
      findInitialKeyTimes();
      draggingBox = true;
      theScore.repaintGraphs();
      return;
    }
    double t[] = ((Track) obj[row]).getKeyTimes();
    for (int i = 0; i < t.length; i++)
    {
      int x = (int) Math.round(scale*(t[i]-start));
      if (x < lastPos.x-HANDLE_SIZE/2 || x > lastPos.x+HANDLE_SIZE/2)
        continue;
      Track tr = (Track) obj[row];
      Keyframe key = tr.getTimecourse().getValues()[i];
      if (ev.isShiftDown())
      {
        if (theScore.isKeyframeSelected(key))
          theScore.removeSelectedKeyframe(key);
        else
          theScore.addSelectedKeyframes(new SelectionInfo [] {new SelectionInfo(tr, key)});
      }
      else if (!theScore.isKeyframeSelected(key))
        theScore.setSelectedKeyframes(new SelectionInfo [] {new SelectionInfo(tr, key)});
      findInitialKeyTimes();
      theScore.repaintGraphs();
      return;
    }
    if (!ev.isShiftDown())
      theScore.setSelectedKeyframes(new SelectionInfo [0]);
    draggingBox = true;
    theScore.repaintAll();
  }
  
  private void mouseDragged(MouseDraggedEvent ev)
  {
    Point pos = ev.getPoint();
    
    if (effectiveMode == Score.SELECT_AND_MOVE)
    {
      if (draggingBox)
      {
        // Drag a box for selecting keyframes.
        
        dragPos = pos;
        repaint();
        return;
      }

      // Drag the selected keyframes.
      
      SelectionInfo sel[] = theScore.getSelectedKeyframes();
      int i, j;
      if (undo == null)
      {
        // Duplicate any tracks with selected keyframes, so we can undo the drag.
        
        undo = new UndoRecord(window, false);
        for (i = 0; i < sel.length; i++)
        {
          Track tr = sel[i].track;
          for (j = 0; j < i && tr != sel[j].track; j++);
          if (j == i)
            undo.addCommand(UndoRecord.COPY_TRACK, new Object [] {tr, tr.duplicate(tr.getParent())});
        }
        window.setUndoRecord(undo);
      }
      double dt = (pos.x-lastPos.x)/scale;
      for (i = 0; i < sel.length; i++)
      {
        // Move each selected keyframe.
        
        int oldindex = sel[i].keyIndex;
        double t = dragKeyTime[i]+dt;
        if (sel[i].track.isQuantized())
          t = Math.round(t*subdivisions)/(double) subdivisions;
        int newindex = sel[i].track.moveKeyframe(oldindex, t);
        
        // If the index of this keyframe within the timecourse has changed, update all
        // the SelectionInfo objects for this track.
        
        if (oldindex != newindex)
        {
          for (j = 0; j < sel.length; j++)
          {
            if (sel[j].keyIndex < oldindex && sel[j].keyIndex > newindex)
              sel[j].keyIndex++;
            else if (sel[j].keyIndex > oldindex && sel[j].keyIndex < newindex)
              sel[j].keyIndex--;
          }
          sel[i].keyIndex = newindex;
        }
      }
      repaint();
      return;
    }
    if (effectiveMode != Score.SCROLL_AND_SCALE)
      return;
    if (ev.isShiftDown())
    {
      // Change the scale of the time axis.
      
      if (pos.x > lastPos.x)
        window.getScore().setScale(scale*Math.pow(1.01, pos.x-lastPos.x));
      else
        window.getScore().setScale(scale/Math.pow(1.01, lastPos.x-pos.x));
      lastPos = pos;
      return;
    }
    
    // Scroll the display.
    
    double dt = (pos.x-lastPos.x)/scale;
    if (lastPos.y != pos.y)
      window.getScore().setScrollPosition(lastPos.y-pos.y-yoffset);
    if (lastPos.x != pos.x)
      window.getScore().setStartTime(start-dt);
    lastPos = pos;
  }

  private void mouseReleased(MouseReleasedEvent ev)
  {
    if (dragPos != null)
    {
      // They were dragging a box, so select any keyframes inside it.  First, find the range
      // of rows intersecting the box.
      
      float rowHeight = (float) theList.getRowHeight();
      int y1 = lastPos.y-yoffset, y2 = dragPos.y-yoffset;
      int row1, row2, x1 = Math.min(lastPos.x, dragPos.x), x2 = Math.max(lastPos.x, dragPos.x);
      dragPos = null;
      if (y1 < y2)
      {
        row1 = Math.round(y1/rowHeight);
        row2 = Math.round(y2/rowHeight);
      }
      else
      {
        row1 = Math.round(y2/rowHeight);
        row2 = Math.round(y1/rowHeight);
      }
      Object obj[] = theList.getVisibleObjects();
      if (row1 < 0)
        row1 = 0;
      if (row2 > obj.length)
        row2 = obj.length;
      Vector<SelectionInfo> v = new Vector<SelectionInfo>();
      for (int row = row1; row < row2; row++)
      {
        if (!(obj[row] instanceof Track))
          continue;
        
        // Find any keyframes of this track inside the box.
        
        Track tr = (Track) obj[row];
        double t[] = tr.getKeyTimes();
        for (int i = 0; i < t.length; i++)
        {
          int x = (int) Math.round(scale*(t[i]-start));
          if (x < x1 || x > x2)
            continue;
          Keyframe key = tr.getTimecourse().getValues()[i];
          v.addElement(new SelectionInfo(tr, key));
        }
      }
      SelectionInfo sel[] = new SelectionInfo [v.size()];
      for (int i = 0; i < sel.length; i++)
        sel[i] = v.elementAt(i);
      theScore.addSelectedKeyframes(sel);
      theScore.repaintGraphs();
    }
  }
  
  private void mouseClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() == 2 && effectiveMode == Score.SELECT_AND_MOVE)
      theScore.editSelectedKeyframe();
  }
}
/* Copyright (C) 2001,2003,2004 by Peter Eastman

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

/** This is a Widget which displays a time axis. */

public class TimeAxis extends CustomWidget
{
  double start, scale, origMarkerPos;
  Score theScore;
  Vector markers;
  int subdivisions;
  Marker draggingMarker;
  Point clickPos;
  ActionProcessor process;
  
  static final int TICK_HEIGHT = 7;
  static final int MARKER_SIZE = 5;
  private static final NumberFormat nf = NumberFormat.getNumberInstance();
  
  /* The arguments to the constructor are the number of ticks to show within a unit 
     interval, and the number of pixels in a unit interval. */
  
  public TimeAxis(int subdivisions, double scale, Score sc)
  {
    this.subdivisions = subdivisions;
    this.scale = scale;
    theScore = sc;
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(2);
    markers = new Vector();
    addEventLink(MousePressedEvent.class, this, "mousePressed");
    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
    addEventLink(RepaintEvent.class, this, "paint");
  }
  
  /** Get the starting time to display. */
  
  public double getStartTime()
  {
    return start;
  }
  
  /** Set the starting time to display. */
  
  public void setStartTime(double time)
  {
    start = time;
  }
  
  /** Get the number of pixels per unit time. */
  
  public double getScale()
  {
    return scale;
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
  
  /** Add a marker to the axis. */
  
  public void addMarker(Marker m)
  {
    markers.addElement(m);
  }
  
  public Dimension getPreferredSize()
  {
    Font f = getFont();
    if (f == null)
      return new Dimension();
    FontMetrics fm = getComponent().getFontMetrics(f);
    return new Dimension(1, fm.getMaxAscent()+fm.getMaxDescent()+TICK_HEIGHT);
  }
  
  private void paint(RepaintEvent ev)
  {
    Graphics2D g = ev.getGraphics();
    FontMetrics fm = getComponent().getFontMetrics(getFont());
    Rectangle dim = getBounds();
    double inc = 1.0/subdivisions;
    int labelPos = fm.getMaxAscent(), tickPos = labelPos+fm.getMaxDescent(), i = 1;

    // Figure out how many ticks to display.

    while (i < subdivisions && inc*scale < 2.0)
    {
      i++;
      if (i*(subdivisions/i) == subdivisions)
        inc = ((double) i)/subdivisions;
    }
    inc = ((double) i)/subdivisions;
    
    // Figure out which ticks to put labels on.
    
    int numLabels = subdivisions/i;
    if (numLabels == 0)
      numLabels = 1;
    boolean label[] = new boolean [numLabels];
    int labelWidth = fm.stringWidth("0000");
    int labelInterval = (int) Math.ceil(labelWidth/(scale*inc));
    if (numLabels > 1)
      while (labelInterval < numLabels && (numLabels/labelInterval)*labelInterval != numLabels)
        labelInterval++;
    for (int j = 0; j< label.length; j += labelInterval)
      label[j] = true;
    
    // Draw the axis.

    double t = Math.ceil(start/inc)*inc;
    int x = (int) Math.round((t-start)*scale), lastLabel = -labelWidth;
    i = (int) Math.round((t-Math.floor(t))/inc);
    while (x < dim.width)
    {
      if (i == 0)
        g.drawLine(x, tickPos, x, tickPos+TICK_HEIGHT);
      else if (label[i])
        g.drawLine(x, tickPos+1, x, tickPos+TICK_HEIGHT);
      else
        g.drawLine(x, tickPos+2, x, tickPos+TICK_HEIGHT);
      if (label[i] && x-lastLabel > labelWidth)
      {
        String s = nf.format(Math.round(t*subdivisions)/(double) subdivisions);
        int w = fm.stringWidth(s);
        g.drawString(s, x-w/2, labelPos);
        lastLabel = x;
      }
      t += inc;
      i = (i+1)%label.length;
      x = (int) Math.round((t-start)*scale);
    }
    
    // Draw any markers.
    
    for (i = 0; i < markers.size(); i++)
    {
      Marker m = (Marker) markers.elementAt(i);
      x = (int) Math.round(scale*(m.position-start));
      g.setColor(m.color);
      g.fillRect(x-MARKER_SIZE/2, tickPos+2, MARKER_SIZE, TICK_HEIGHT-2);
    }
  }
  
  private void mousePressed(MousePressedEvent ev)
  {
    clickPos = ev.getPoint();
    draggingMarker = null;
    process = new ActionProcessor();
    for (int i = 0; i < markers.size(); i++)
      {
	Marker m = (Marker) markers.elementAt(i);
	int x = (int) (scale*(m.position-start));
	if (clickPos.x < x-MARKER_SIZE/2-1 || clickPos.x > x+MARKER_SIZE/2+1)
	  continue;
	draggingMarker = m;
	origMarkerPos = m.position;
      }
    if (draggingMarker == null && markers.size() > 0)
      {
        // Snap the default marker to the click position.
        
	draggingMarker = (Marker) markers.elementAt(0);
        origMarkerPos = clickPos.x/scale+start;
        mouseDragged(ev);
      }
  }
  
  private void mouseDragged(WidgetMouseEvent ev)
  {
    final Point pos = ev.getPoint();
    
    if (draggingMarker == null)
      return;
    
    // Find the new position for the marker that is being dragged.  Since this can 
    // be a somewhat CPU intensive operation, we perform it on a separate thread 
    // to avoid blocking the AWT even thread.

    Runnable c = new Runnable() {
      public void run()
      {
	if (pos.x < 0)
	  pos.x = 0;
	int width = getBounds().width;
	if (pos.x >= width)
	  pos.x = width-1;
	double t = draggingMarker.position;
	t = origMarkerPos + (pos.x-clickPos.x)/scale;
	t = Math.round(t*subdivisions)/(double) subdivisions;
	draggingMarker.position = t;
	theScore.markerMoved(draggingMarker, true);
      }
    };
    process.addEvent(c);
  }

  private void mouseReleased(MouseReleasedEvent ev)
  {
    if (process != null)
      process.stopProcessing();
    process = null;
    if (draggingMarker != null)
      theScore.markerMoved(draggingMarker, false);
  }
}
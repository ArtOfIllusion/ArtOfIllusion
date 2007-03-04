/* Copyright (C) 2001,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import javax.swing.*;

/** This class is attached to a scroll pane.  It causes the scroll pane to automatically
    scroll whenever the mouse is dragged beyond the edge of it. */

public class AutoScroller implements Runnable
{
  protected BScrollPane sp;
  protected Thread scrollThread;
  protected int x, y, xinc, yinc, delay;
  
  public AutoScroller(BScrollPane pane, int xincrement, int yincrement)
  {
    xinc = xincrement;
    yinc = yincrement;
    sp = pane;
    pane.getContent().addEventLink(MousePressedEvent.class, this, "mousePressed");
    pane.getContent().addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
    pane.getContent().addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
    delay = 100;
  }
  
  private void mousePressed(MousePressedEvent ev)
  {
    x = ev.getX();
    y = ev.getY();
    if (scrollThread == null)
    {
      scrollThread = new Thread(this);
      scrollThread.start();
    }
  }

  private void mouseReleased(MouseReleasedEvent ev)
  {
    scrollThread.interrupt();
    scrollThread = null;
  }
  
  private void mouseDragged(MouseDraggedEvent ev)
  {
    x = ev.getX();
    y = ev.getY();
  }

  public void run()
  {
    while (true)
      {
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            Dimension scrollSize = sp.getViewSize();
            Point scrollPos = new Point(sp.getHorizontalScrollBar().getValue(), sp.getVerticalScrollBar().getValue());
            if (x < scrollPos.x || y < scrollPos.y || x > scrollPos.x+scrollSize.width || y > scrollPos.y+scrollSize.height)
              scrollWhileDragging(scrollSize, scrollPos);
          }
        });
        try
          {
            Thread.sleep(delay);
          }
        catch (InterruptedException ex)
          {
            return;
          }
      }
  }
  
  /** This is called repeatedly whenever the mouse is dragged outside the visible bounds
      to scroll the BScrollPane.  If additional things need to be done at this time (such
      redrawing objects being dragged), this can be subclassed. */

  protected void scrollWhileDragging(Dimension scrollSize, Point scrollPos)
  {
    final Point newPos = scrollPos;
    if (x < scrollPos.x)
      newPos.x -= xinc;
    if (y < scrollPos.y)
      newPos.y -= yinc;
    if (x > scrollPos.x+scrollSize.width)
      newPos.x += xinc;
    if (y > scrollPos.y+scrollSize.height)
      newPos.y += yinc;
    sp.getHorizontalScrollBar().setValue(newPos.x);
    sp.getVerticalScrollBar().setValue(newPos.y);
    x += newPos.x - scrollPos.x;
    y += newPos.y - scrollPos.y;
  }
}

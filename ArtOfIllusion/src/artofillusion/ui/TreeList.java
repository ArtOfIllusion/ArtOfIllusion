/* Copyright (C) 2001-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.*;
import buoy.event.*;
import buoy.widget.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/** This is a Widget which displays a hierarchy of objects.  It provides functionality
    for opening and closing parts of the hierarchy, selecting elements, and moving elements
    around. */

public class TreeList extends CustomWidget
{
  private EditingWindow window;
  private Vector<TreeElement> elements, showing, selected;
  private Vector<Integer> indent;
  private int yoffset, rowHeight, dragStart, lastDrag, lastClickRow, lastIndent, maxRowWidth;
  private boolean updateDisabled, moving, origSelected[], insertAbove, okToInsert, allowMultiple;
  private PopupMenuManager popupManager;
  protected UndoRecord undo;
  
  private static final Polygon openHandle, closedHandle, insertHandle;
  private static final int INDENT_WIDTH = 10;
  private static final int HANDLE_WIDTH = 4;
  private static final int HANDLE_HEIGHT = 8;
  private static final int INSERT_WIDTH = 3;
  private static final int INSERT_HEIGHT = 6;

  static
  {
    openHandle = new Polygon(new int [] {-HANDLE_HEIGHT/2, HANDLE_HEIGHT/2, 0},
        new int [] {-HANDLE_WIDTH/2, -HANDLE_WIDTH/2, HANDLE_WIDTH/2}, 3);
    closedHandle = new Polygon(new int [] {-HANDLE_WIDTH/2, HANDLE_WIDTH/2, -HANDLE_WIDTH/2},
        new int [] {-HANDLE_HEIGHT/2, 0, HANDLE_HEIGHT/2}, 3);
    insertHandle = new Polygon(new int [] {-INSERT_WIDTH-2, -2, -INSERT_WIDTH-2},
        new int [] {-INSERT_HEIGHT/2, 0, INSERT_HEIGHT/2}, 3);
  }

  public TreeList(EditingWindow win)
  {
    window = win;
    elements = new Vector<TreeElement>();
    showing = new Vector<TreeElement>();
    indent = new Vector<Integer>();
    selected = new Vector<TreeElement>();
    origSelected = new boolean [0];
    allowMultiple = true;
    lastClickRow = -1;
    Font font = getFont();
    if (font == null)
      font = UIUtilities.getDefaultFont();
    if (font != null)
    {
      FontMetrics fm = getComponent().getFontMetrics(font);
      rowHeight = Math.max(fm.getMaxAscent()+fm.getMaxDescent(), HANDLE_HEIGHT)+3;
    }
    else
      rowHeight = 15;
    buildState();
    addEventLink(MousePressedEvent.class, this, "mousePressed");
    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
    addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    addEventLink(RepaintEvent.class, this, "paint");
  }

  public Dimension getPreferredSize()
  {
    Dimension superPref = super.getPreferredSize();
    return new Dimension(Math.max(superPref.width, maxRowWidth), Math.max(superPref.height, rowHeight*showing.size()));
  }
  
  public Dimension getMinimumSize()
  {
    return new Dimension(maxRowWidth, rowHeight*showing.size());
  }

  /** Set whether this tree allows multiple selections (default is true). */
  
  public void setAllowMultiple(boolean allow)
  {
    allowMultiple = allow;
  }

  /** Temporarily disable updating of the tree.  This is useful when several elements are
      going to be added or removed at once. */
  
  public void setUpdateEnabled(boolean enabled)
  {
    updateDisabled = !enabled;
    if (enabled)
    {
      buildState();
      repaint();
    }
  }

  /** Add an element to the tree. */
  
  public void addElement(TreeElement el)
  {
    elements.addElement(el);
    buildState();
    if (!updateDisabled)
      repaint();
  }

  /** Add an element to the tree. */
  
  public void addElement(TreeElement el, int position)
  {
    elements.insertElementAt(el, position);
    if (!updateDisabled)
    {
      buildState();
      repaint();
    }
  }
  
  /** Find the TreeElement corresponding to an object, or null if there is none. */
  
  public TreeElement findElement(Object obj)
  {
    for (int i = 0; i < elements.size(); i++)
    {
      TreeElement el = (TreeElement) elements.elementAt(i);
      if (el.getObject().equals(obj))
        return el;
      TreeElement subElement = findElement(obj, el);
      if (subElement != null)
        return subElement;
    }
    return null;
  }
  
  private TreeElement findElement(Object obj, TreeElement parent)
  {
    for (int i = 0; i < parent.getNumChildren(); i++)
    {
      TreeElement el = parent.getChild(i);
      if (el.getObject().equals(obj))
        return el;
      TreeElement subElement = findElement(obj, el);
      if (subElement != null)
        return subElement;
    }
    return null;
  }
  
  /** Remove the element from the tree which corresponds to the specified object. */
  
  public void removeObject(Object obj)
  {
    for (int i = elements.size()-1; i >= 0; i--)
    {
      TreeElement el = (TreeElement) elements.elementAt(i);
      if (el.getObject() == obj)
        elements.removeElementAt(i);
      else
        el.removeChild(obj);
    }
    if (!updateDisabled)
    {
      buildState();
      repaint();
    }
  }
  
  /** Remove all elements from the tree. */
  
  public void removeAllElements()
  {
    elements.removeAllElements();
    if (!updateDisabled)
    {
      buildState();
      repaint();
    }
  }
  
  /** Get an array of all the TreeElements in the tree. */
  
  public TreeElement [] getElements()
  {
    Vector<TreeElement> v = new Vector<TreeElement>();
    TreeElement el;

    for (int i = 0; i < elements.size(); i++)
    {
      el = (TreeElement) elements.elementAt(i);
      v.addElement(el);
      addChildrenToVector(el, v);
    }
    TreeElement allEl[] = new TreeElement [v.size()];
    for (int i = 0; i < allEl.length; i++)
      allEl[i] = (TreeElement) v.elementAt(i);
    return allEl;
  }
  
  private void addChildrenToVector(TreeElement el, Vector<TreeElement> v)
  {
    for (int i = 0; i < el.getNumChildren(); i++)
    {
      TreeElement child = el.getChild(i);
      v.addElement(child);
      addChildrenToVector(child, v);
    }
  }
  
  /** Get an array of the objects corresponding to selected TreeElements. */
  
  public Object [] getSelectedObjects()
  {
    Object sel[] = new Object [selected.size()];
    for (int i = 0; i < sel.length; i++)
      sel[i] = ((TreeElement) selected.elementAt(i)).getObject();
    return sel;
  }
  
  /** Deselect all elements in the tree. */
  
  public void deselectAll()
  {
    for (int i = 0; i < elements.size(); i++)
      deselectRecursively((TreeElement) elements.elementAt(i));
    if (!updateDisabled)
    {
      buildState();
      repaint();
    }
  }
  
  private void deselectRecursively(TreeElement el)
  {
    el.setSelected(false);
    for (int i = 0; i < el.getNumChildren(); i++)
      deselectRecursively(el.getChild(i));
  }
  
  /** Get an array of the objects corresponding to visible TreeElements, in the order that
      they appear. */
  
  public Object [] getVisibleObjects()
  {
    Object vis[] = new Object [showing.size()];
    for (int i = 0; i < vis.length; i++)
      vis[i] = ((TreeElement) showing.elementAt(i)).getObject();
    return vis;
  }
  
  /** Get the height (in pixels) of each row in the list. */
  
  public int getRowHeight()
  {
    return rowHeight;
  }
  
  /** Select or deselect the element corresponding to a particular object. */
  
  public void setSelected(Object obj, boolean selected)
  {
    TreeElement el = (obj instanceof TreeElement ? (TreeElement) obj : findElement(obj));
    boolean wasDisabled = updateDisabled;
    updateDisabled = true;
    if (el != null)
    {
      el.setSelected(selected);
      for (int i = 0; i < el.getNumChildren(); i++)
      {
        TreeElement child = el.getChild(i);
        if (child.selectWithParent())
          setSelected(child, selected);
      }
    }
    updateDisabled = wasDisabled;
    if (!updateDisabled)
    {
      buildState();
      repaint();
    }
  }
  
  /** Expand all parents of the specified object to make it visible. */
  
  public void expandToShowObject(Object obj)
  {
    TreeElement el = (obj instanceof TreeElement ? (TreeElement) obj : findElement(obj));
    if (el == null)
      return;
    while ((el = el.getParent()) != null)
      el.setExpanded(true);
    if (!updateDisabled)
    {
      buildState();
      repaint();
    }
  }
  
  /** Start recording an undo record to reverse subsequent actions taken by the TreeList. */
  
  private void recordUndo()
  {
    undo = new UndoRecord(window, false);
  }
  
  /** Finish recording the undo record, and return the completed record. */
  
  private UndoRecord finishRecording()
  {
    UndoRecord rec = undo;
    undo = null;
    return rec;
  }

  /** Build the arrays representing the current state of the tree. */
  
  private void buildState()
  {
    if (updateDisabled)
      return;
    showing.removeAllElements();
    indent.removeAllElements();
    selected.removeAllElements();
    for (int i = 0; i < elements.size(); i++)
    {
      TreeElement el = (TreeElement) elements.elementAt(i);
      showing.addElement(el);
      indent.addElement(0);
      if (el.isSelected())
        selected.addElement(el);
      addChildrenToState(el, 1, el.isExpanded());
    }
    if (origSelected.length != showing.size())
      origSelected = new boolean [showing.size()];
    invalidateSize();
    if (getComponent().isDisplayable())
      getParent().layoutChildren();
    if (selected.size() == 0)
      lastClickRow = -1;
    else if (selected.size() == 1)
      lastClickRow = showing.indexOf(selected.get(0));
  }
  
  private void addChildrenToState(TreeElement el, int currentIndent, boolean expanded)
  {
    for (int i = 0; i < el.getNumChildren(); i++)
    {
      TreeElement child = el.getChild(i);
      if (expanded)
      {
        showing.addElement(child);
        indent.addElement(new Integer(currentIndent));
      }
      if (child.isSelected())
        selected.addElement(child);
      addChildrenToState(child, currentIndent+1, expanded & child.isExpanded());
    }
  }
  
  /** Set the y offset (for vertically scrolling the panel). */
  
  public void setYOffset(int offset)
  {
    yoffset = offset;
  }
  
  /** Paint the tree. */
  
  private void paint(RepaintEvent ev)
  {
    Graphics2D g = ev.getGraphics();
    FontMetrics fm = g.getFontMetrics();
    Rectangle dim = getBounds();
    int y = yoffset;
    
    rowHeight = Math.max(fm.getMaxAscent()+fm.getMaxDescent(), HANDLE_HEIGHT)+3;
    maxRowWidth = 0;
    for (int i = 0; i < showing.size(); i++)
    {
      TreeElement el = (TreeElement) showing.elementAt(i);
      int x = ((Integer) indent.elementAt(i)).intValue()*INDENT_WIDTH;
      if (el.getNumChildren() > 0)
      {
        // Draw the handle to collapse or expand the hierarchy.
        
        g.setColor(Color.black);
        if (el.isExpanded())
        {
          openHandle.translate(x+INDENT_WIDTH/2, y+rowHeight/2);
          g.drawPolygon(openHandle);
          openHandle.translate(-x-INDENT_WIDTH/2, -y-rowHeight/2);
        }
        else
        {
          closedHandle.translate(x+INDENT_WIDTH/2, y+rowHeight/2);
          g.drawPolygon(closedHandle);
          closedHandle.translate(-x-INDENT_WIDTH/2, -y-rowHeight/2);
        }
      }
      
      // Draw the label.
      
      x += INDENT_WIDTH;
      Icon icon = el.getIcon();
      if (icon != null)
      {
        icon.paintIcon(getComponent(), g, x, y);
        x += icon.getIconWidth();
      }
      if (el.isSelected())
      {
        g.setColor(el.isGray() ? Color.gray : Color.black);
        g.fillRect(x, y, dim.width-x, rowHeight-3);
        g.setColor(Color.white);
        g.drawString(el.getLabel(), x+1, y+fm.getMaxAscent());
      }
      else
      {
        g.setColor(el.isGray() ? Color.gray : Color.black);
        g.drawString(el.getLabel(), x+1, y+fm.getMaxAscent());
      }
      y += rowHeight;
      maxRowWidth = Math.max(maxRowWidth, fm.stringWidth(el.getLabel())+x+1);
    }
  }
  
  private void mousePressed(MousePressedEvent ev)
  {
    Point pos = ev.getPoint();
    pos.y -= yoffset;
    int row = pos.y/rowHeight;
    
    moving = false;
    if (row >= showing.size())
    {
      // The click was below the last item in the list.
      
      deselectAll();
      Arrays.fill(origSelected, false);
      buildState();
      dispatchEvent(new SelectionChangedEvent(this));
      return;
    }
    dragStart = lastDrag = row;
    TreeElement el = (TreeElement) showing.elementAt(row);
    int i = pos.x/INDENT_WIDTH;
    int ind = ((Integer) indent.elementAt(row)).intValue();
    if (i == ind && el.getNumChildren() > 0)
    {
      // Expand or collapse this item.
      
      el.setExpanded(!el.isExpanded());
      buildState();
      repaint();
      dispatchEvent(new ElementExpandedEvent(el));
      showPopupIfNeeded(ev);
      return;
    }
    if (i < ind || (el.getParent() != null && el.selectWithParent() && el.getParent().isSelected()))
    {
      showPopupIfNeeded(ev);
      return;
    }
    if (ev.isShiftDown() || ev.isControlDown() || ev.isMetaDown())
    {
      moving = false;
      if (lastClickRow == -1)
        lastClickRow = row;
    }
    else
    {
      moving = true;
      lastClickRow = row;
    }
    okToInsert = false;
    boolean selectionChanged = false;
    if (allowMultiple && (ev.isControlDown() || ev.isMetaDown()) && !ev.isPopupTrigger() && ev.getButton() == 1)
    {
      setSelected(el, !el.isSelected());
      selectionChanged = true;
    }
    else if (allowMultiple && ev.isShiftDown() && lastClickRow > -1)
    {
      int min = Math.min(lastClickRow, row);
      int max = Math.min(Math.max(lastClickRow, row), showing.size()-1);
      updateDisabled = true;
      for (i = 0; i < showing.size(); i++)
      {
        TreeElement elem = (TreeElement) showing.elementAt(i);
        boolean sel = (origSelected[i] || (i >= min && i <= max));
        if (elem.isSelected() != sel)
        {
          setSelected(elem, sel);
          selectionChanged = true;
        }
      }
      updateDisabled = false;
    }
    else if (!el.isSelected())
    {
      deselectAll();
      setSelected(el, true);
      selectionChanged = true;
    }
    for (i = 0; i < origSelected.length; i++)
    {
      el = (TreeElement) showing.elementAt(i);
      origSelected[i] = el.isSelected();
    }
    buildState();
    if (selectionChanged)
      dispatchEvent(new SelectionChangedEvent(this));
    repaint();
    showPopupIfNeeded(ev);
  }
  
  private void mouseDragged(MouseDraggedEvent ev)
  {
    Point pos = ev.getPoint();
    pos.y -= yoffset;
    int row = pos.y/rowHeight, min, max, i;

    if (moving)
    {
      // The selected elements are being dragged.
      
      if (selected.size() == 0)
        return;
      boolean above = pos.y - row*rowHeight < rowHeight/2;
      if (row >= showing.size())
      {
        row = showing.size();
        above = true;
      }
      if (row < 0)
      {
        row = 0;
        above = true;
      }
      if (row == lastDrag && above == insertAbove)
        return;
      Graphics g = getComponent().getGraphics();
      if (okToInsert)
      {
        // Erase the old insertion marker.
    
        g.setColor(getBackground());
        drawInsertionPoint(g, insertAbove ? lastDrag : lastDrag+1, lastIndent);
      }
      
      // Determine whether the selected objects can be inserted here.

      TreeElement parent = null;
      if (row < showing.size())
      {
        TreeElement el = (TreeElement) showing.elementAt(row);
        parent = el;
        lastIndent = ((Integer) indent.elementAt(row)).intValue();
        if (above)
        {
          parent = el.getParent();
          okToInsert = dragTargetOk(parent);
        }
        else
        {
          okToInsert = dragTargetOk(parent);
          if (okToInsert)
            lastIndent++;
          else
          {
            parent = el.getParent();
            okToInsert = dragTargetOk(parent);
          }
        }
      }
      else
        lastIndent = 0;
      okToInsert = true;
      for (i = 0; okToInsert && i < selected.size(); i++)
      {
        TreeElement el = (TreeElement) selected.elementAt(i);
        if (el.getParent() != null && el.getParent().isSelected())
          continue;
        okToInsert &= el.canAcceptAsParent(parent);
      }
      if (okToInsert)
      {
    
        // Draw the new insertion point.

        g.setColor(Color.black);
        drawInsertionPoint(g, above ? row : row+1, lastIndent);
      }
      g.dispose();
      lastDrag = row;
      insertAbove = above;
      return;
    }
    
    if (row == lastDrag || !allowMultiple)
      return;
    lastDrag = row;
    min = Math.max(Math.min(row, dragStart), 0);
    max = Math.min(Math.max(row, dragStart), showing.size()-1);
    updateDisabled = true;
    for (i = 0; i < showing.size(); i++)
    {
      TreeElement el = (TreeElement) showing.elementAt(i);
      boolean sel = (origSelected[i] || (i >= min && i <= max) || 
          (el.getParent() != null && el.getParent().isSelected()));
      if (el.isSelected() != sel)
        setSelected(el, sel);
    }
    updateDisabled = false;
    buildState();
    repaint();
  }

  private void mouseReleased(MouseReleasedEvent ev)
  {
    if (moving)
    {
      if (okToInsert)
      {
        // Move the selected elements to the specified location.
        
        recordUndo();
        updateDisabled = true;
        TreeElement el = null, parent;
        int position = 0;
        
        // First figure out where to insert them.
        
        if (lastDrag < showing.size())
        {
          el = (TreeElement) showing.elementAt(lastDrag);
          parent = el;
          if (insertAbove || !dragTargetOk(el))
          {
            parent = el.getParent();
            if (parent == null)
              for (position = 0; elements.elementAt(position) != el; position++);
            else
              for (position = 0; parent.getChild(position) != el; position++);
            if (!insertAbove)
              position++;
          }
          else
            position = 0;
        }
        else
        {
          parent = null;
          position = elements.size();
        }
        
        // Now remove them from the tree, and insert them at the correct place.
        
        for (int i = 0; i < selected.size(); i++)
        {
          el = (TreeElement) selected.elementAt(i);
          if (el.getParent() != null && el.getParent().isSelected())
          {
            selected.removeElementAt(i);
            i--;
            continue;
          }
          if (el.getParent() == parent)
          {
            int j = showing.indexOf(el);
            if (j < lastDrag || (!insertAbove && j == lastDrag))
              position--;
          }
          removeObject(el.getObject());
        }
        if (position < 0)
          position = 0;
        if (parent == null)
          for (int i = 0; i < selected.size(); i++)
          {
            el = (TreeElement) selected.elementAt(i);
            if (el.getParent() == null || !el.getParent().isSelected())
              addElement(el, position++);
          }
        else
          for (int i = 0; i < selected.size(); i++)
          {
            el = (TreeElement) selected.elementAt(i);
            if (el.getParent() == null || !el.getParent().isSelected())
              parent.addChild(el, position++);
          }
        updateDisabled = false;
        buildState();
        window.setUndoRecord(finishRecording());
        repaint();
        dispatchEvent(new ElementMovedEvent(el));
      }
    }
    else
    {
      // They were selecting a range of objects.
      
      if (lastDrag != dragStart)
        dispatchEvent(new SelectionChangedEvent(this));
    }
    showPopupIfNeeded(ev);
  }
  
  /** Determine whether the selected elements can be added to a particular parent. */
  
  private boolean dragTargetOk(TreeElement parent)
  {
    for (int i = 0; i < selected.size(); i++)
    {
      TreeElement el = (TreeElement) selected.elementAt(i);
      if (el.getParent() != null && el.getParent().isSelected())
        continue;
      if (!el.canAcceptAsParent(parent))
        return false;
    }
    return true;
  }
  
  private void mouseClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() != 2)
      return;
    Point pos = ev.getPoint();
    pos.y -= yoffset;
    int row = pos.y/rowHeight, i = pos.x/INDENT_WIDTH;
    if (row >= showing.size())
      return;
    int ind = ((Integer) indent.elementAt(row)).intValue();
    TreeElement el = (TreeElement) showing.elementAt(row);
    if (i < ind)
      return;
    dispatchEvent(new ElementDoubleClickedEvent(el));
  }

  /** Draw the insertion point to show where dragged items will be moved to. */
  
  private void drawInsertionPoint(Graphics g, int pos, int indent)
  {
    int x = (indent+1)*INDENT_WIDTH, y = pos*rowHeight-2+yoffset;
    Rectangle dim = getBounds();

    insertHandle.translate(x, y);
    g.fillPolygon(insertHandle);
    insertHandle.translate(-x, -y);
    g.drawLine(x-2, y, dim.width, y);
  }
  
  /** Set the PopupMenuManager for this list. */
  
  public void setPopupMenuManager(PopupMenuManager manager)
  {
    popupManager = manager;
  }
  
  /** Display the popup menu when an appropriate event occurs. */
  
  private void showPopupIfNeeded(WidgetMouseEvent ev)
  {
    if (!ev.isPopupTrigger() || popupManager == null)
      return;
    repaint();
    Point pos = ev.getPoint();
    popupManager.showPopupMenu(this, pos.x, pos.y);
  }
  
  /** Inner class which is the superclass of various events generated by tree. */
  
  public class TreeElementEvent implements WidgetEvent
  {
    TreeElement elem;
    
    private TreeElementEvent(TreeElement el)
    {
      elem = el;
    }
    
    public TreeElement getElement()
    {
      return elem;
    }
    
    public Widget getWidget()
    {
      return TreeList.this;
    }
  }
  
  /** Inner class representing an event when one or more elements are moved in the tree. */
  
  public class ElementMovedEvent extends TreeElementEvent
  {
    private ElementMovedEvent(TreeElement el)
    {
      super(el);
    }
  }
  
  /** Inner class representing an event when an element is expanded or collapsed. */
  
  public class ElementExpandedEvent extends TreeElementEvent
  {
    private ElementExpandedEvent(TreeElement el)
    {
      super(el);
    }
  }
  
  /** Inner class representing an event when an element is double-clicked. */
  
  public class ElementDoubleClickedEvent extends TreeElementEvent
  {
    private ElementDoubleClickedEvent(TreeElement el)
    {
      super(el);
    }
  }
}

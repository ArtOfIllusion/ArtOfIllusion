/* Copyright (C) 2001-2012 by Peter Eastman

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
import buoy.event.*;
import buoy.widget.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

/** This is a Widget which displays all the tracks for objects in a scene, and shows
    where their keyframes are. */

public class Score extends BorderContainer implements EditingWindow, PopupMenuManager
{
  LayoutWindow window;
  TreeList theList;
  TimeAxis theAxis;
  Vector<TrackDisplay> graphs;
  BScrollBar scroll;
  ToolPalette viewTools, modeTools;
  BLabel helpText;
  BSplitPane div;
  BPopupMenu popupMenu;
  BMenuItem popupMenuItem[];
  Marker timeMarker;
  SelectionInfo selection[];
  int scrollPos, mode, view;
  double startTime, timeScale;
  int yoffset;
  private boolean hasRepaintedView[], isAnimating;
  private long animateStartClockTime;
  private double animateStartSceneTime;
  private double playbackSpeed;
  private final BButton playButton, rewindButton, endButton;
  private final BSlider speedSlider;
  private final BLabel speedLabel, timeFrameLabel;
  private final ImageIcon playIcon, stopIcon;

  public static final int TRACKS_MODE = 0;
  public static final int SINGLE_GRAPH_MODE = 1;
  public static final int MULTI_GRAPH_MODE = 2;

  public static final int SELECT_AND_MOVE = 0;
  public static final int SCROLL_AND_SCALE = 1;

  private final String MODE_HELP_TEXT[] = new String [] {
      Translate.text("moveKeyframeTool.helpText"),
      Translate.text("moveScoreTool.helpText")};

  private final double SPEEDS[] = {0.2, 0.3, 0.4, 0.5, 0.6, 0.8, 1, 1.5, 2, 2.5, 3, 4, 5};

  public Score(LayoutWindow win)
  {
    window = win;
    playIcon = ThemeManager.getIcon("play");
    stopIcon = ThemeManager.getIcon("stop");
    playButton = new BButton(playIcon);
    rewindButton = new BButton(ThemeManager.getIcon("rewind"));
    endButton = new BButton(ThemeManager.getIcon("forward"));
    playButton.addEventLink(CommandEvent.class, this, "clickedPlay");
    rewindButton.addEventLink(CommandEvent.class, this, "clickedRewind");
    endButton.addEventLink(CommandEvent.class, this, "clickedEnd");
    speedSlider = new BSlider(SPEEDS.length/2, 0, SPEEDS.length-1, BSlider.HORIZONTAL);
    speedSlider.addEventLink(ValueChangedEvent.class, this, "speedChanged");
    speedSlider.setMinorTickSpacing(1);
    speedSlider.setSnapToTicks(true);
    speedSlider.getComponent().setPreferredSize(new Dimension(1, speedSlider.getPreferredSize().height));
    speedLabel = new BLabel();
    speedLabel.setAlignment(BLabel.CENTER);
    timeFrameLabel = new BLabel();
    timeFrameLabel.setAlignment(BLabel.NORTH);
    RowContainer controlButtons = new RowContainer();
    controlButtons.add(rewindButton);
    controlButtons.add(playButton);
    controlButtons.add(endButton);
    FormContainer controlsContainer = new FormContainer(new double[] {1, 0}, new double[] {0, 0, 0, 1});
    controlsContainer.add(controlButtons, 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE));
    controlsContainer.add(speedLabel, 0, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL));
    controlsContainer.add(speedSlider, 0, 2, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL));
    controlsContainer.add(timeFrameLabel, 0, 3, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    controlsContainer.add(new BSeparator(BSeparator.VERTICAL), 1, 0, 1, 4);
    theList = new TreeList(win);
    theList.setPreferredSize(new Dimension(130, 0));
    theList.addEventLink(TreeList.ElementMovedEvent.class, this, "elementsMoved");
    theList.addEventLink(TreeList.ElementExpandedEvent.class, this, "elementOpenedOrClosed");
    theList.addEventLink(TreeList.ElementDoubleClickedEvent.class, this, "elementDoubleClicked");
    theList.addEventLink(SelectionChangedEvent.class, this, "treeSelectionChanged");
    theList.setPopupMenuManager(this);
    selection = new SelectionInfo [0];
    int fps = window.getScene().getFramesPerSecond();
    timeScale = fps*5.0;
    theAxis = new TimeAxis(fps, timeScale, this);
    graphs = new Vector<TrackDisplay>();
    timeMarker = new Marker(window.getScene().getTime(), Translate.text("Time"), Color.green);
    theAxis.addMarker(timeMarker);
    scroll = new BScrollBar(0, 0, 0, 0, BScrollBar.VERTICAL);
    scroll.addEventLink(ValueChangedEvent.class, this, "scrollbarChanged");
    viewTools = new ToolPalette(1, 3);
    modeTools = new ToolPalette(1, 2);
    viewTools.addTool(new GenericTool(this, "trackMode", Translate.text("trackModeTool.tipText")));
    viewTools.addTool(new GenericTool(this, "singleMode", Translate.text("singleGraphModeTool.tipText")));
    viewTools.addTool(new GenericTool(this, "multiMode", Translate.text("multiGraphModeTool.tipText")));
    modeTools.addTool(new GenericTool(this, "moveKey", Translate.text("moveKeyframeTool.tipText")));
    modeTools.addTool(new GenericTool(this, "panTrack", Translate.text("moveScoreTool.tipText")));
    BorderContainer treeContainer = new BorderContainer();
    treeContainer.add(new Spacer(theList, theAxis), BorderContainer.NORTH);
    treeContainer.add(theList, BorderContainer.CENTER);
    div = new BSplitPane(BSplitPane.HORIZONTAL, treeContainer, null);
    div.setResizeWeight(0.0);
    div.resetToPreferredSizes();
    ((JSplitPane) div.getComponent()).setBorder(null);
    layoutGraphs();
    add(div, BorderContainer.CENTER);
    FormContainer rightSide = new FormContainer(new double [] {1.0, 1.0}, new double [] {0.0, 0.0, 1.0});
    rightSide.add(scroll, 0, 0, 1, 3, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.VERTICAL, null, null));
    rightSide.add(viewTools, 1, 0);
    rightSide.add(modeTools, 1, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(0, 5, 0, 0), null));
    add(controlsContainer, BorderContainer.WEST);
    add(rightSide, BorderContainer.EAST);
    add(helpText = new BLabel(MODE_HELP_TEXT[mode]), BorderContainer.SOUTH);
    rebuildList();
    createPopupMenu();
    setFocusable(true);
    setPlaybackSeed(1);
    UIUtilities.applyDefaultBackground(controlsContainer);
    UIUtilities.applyDefaultBackground(rightSide);
  }

  /** Create the popup menu. */
  
  private void createPopupMenu()
  {
    popupMenu = new BPopupMenu();
    popupMenuItem = new BMenuItem [5];
    popupMenu.add(popupMenuItem[0] = Translate.menuItem("editTrack", this, "editSelectedTrack", null));
    popupMenu.add(popupMenuItem[1] = Translate.menuItem("duplicateTracks", this, "duplicateSelectedTracks", null));
    popupMenu.add(popupMenuItem[2] = Translate.menuItem("deleteTracks", this, "deleteSelectedTracks", null));
    popupMenu.add(popupMenuItem[3] = Translate.menuItem("enableTracks", window, "actionPerformed", null));
    popupMenu.add(popupMenuItem[4] = Translate.menuItem("disableTracks", window, "actionPerformed", null));
  }
  
  /** Display the popup menu. */

  public void showPopupMenu(Widget w, int x, int y)
  {
    Track selTrack[] = getSelectedTracks();
    boolean enable = false, disable = false;

    for (int i = 0; i < selTrack.length; i++)
      {
        if (selTrack[i].isEnabled())
          disable = true;
        else
          enable = true;
      }
    popupMenuItem[0].setEnabled(selTrack.length == 1); // Edit Track
    popupMenuItem[1].setEnabled(selTrack.length > 0); // Duplicate Tracks
    popupMenuItem[2].setEnabled(selTrack.length > 0); // Delete Tracks
    popupMenuItem[3].setEnabled(enable); // Enable Tracks
    popupMenuItem[4].setEnabled(disable); // Disable Tracks
    popupMenu.show(w, x, y);
  }
  
  /** Get the popup menu for the score. */
  
  public BPopupMenu getPopupMenu()
  {
    return popupMenu;
  }
  
  /** Allow the score to be fully hidden. */
  
  public Dimension getMinimumSize()
  {
    return new Dimension(0, 0);
  }

  /** Get the currently selected tracks. */
  
  public Track [] getSelectedTracks()
  {
    Object obj[] = theList.getSelectedObjects();
    Track tr[] = new Track [obj.length];
    
    for (int i = 0; i < tr.length; i++)
      tr[i] = (Track) obj[i];
    return tr;
  }
  
  /** Get the currently selected keyframes. */
  
  public SelectionInfo [] getSelectedKeyframes()
  {
    return selection;
  }
  
  /** Set the currently selected keyframes. */
  
  public void setSelectedKeyframes(SelectionInfo sel[])
  {
    selection = sel;
    for (int i = 0; i < graphs.size(); i++)
      ((Widget) graphs.elementAt(i)).repaint();
    window.updateMenus();
  }
  
  /** Add a set of keyframes to the selection. */
  
  public void addSelectedKeyframes(SelectionInfo newsel[])
  {
    Vector v = new Vector();
    int i, j;
    
    for (i = 0; i < selection.length; i++)
      v.addElement(selection[i]);
    for (i = 0; i < newsel.length; i++)
      {
        for (j = 0; j < selection.length; j++)
          if (newsel[i].key == selection[j].key)
            {
              for (int k = 0; k < newsel[i].selected.length; k++)
                selection[j].selected[k] |= newsel[i].selected[k];
              break;
            }
        if (j == selection.length)
          v.addElement(newsel[i]);
      }
    selection = new SelectionInfo [v.size()];
    for (i = 0; i < selection.length; i++)
      selection[i] = (SelectionInfo) v.elementAt(i);
    window.updateMenus();
  }
  
  /** Remove a keyframe from the selection. */
  
  public void removeSelectedKeyframe(Keyframe key)
  {
    Vector v = new Vector();
    
    for (int i = 0; i < selection.length; i++)
      if (selection[i].key != key)
        v.addElement(selection[i]);
    selection = new SelectionInfo [v.size()];
    for (int i = 0; i < selection.length; i++)
      selection[i] = (SelectionInfo) v.elementAt(i);
    window.updateMenus();
  }
  
  /** Determine whether a particular keyframe is selected. */
  
  public boolean isKeyframeSelected(Keyframe k)
  {
    for (int i = 0; i < selection.length; i++)
      if (selection[i].key == k)
        return true;
    return false;
  }

  /** Determine whether the handle for a particular value of a keyframe is selected. */
  
  public boolean isKeyframeSelected(Keyframe k, int value)
  {
    for (int i = 0; i < selection.length; i++)
      if (selection[i].key == k)
        return (selection[i].selected.length > value && selection[i].selected[value]);
    return false;
  }

  /** Rebuild the TreeList, attempting as much as possible to preserve its current state. */
  
  public void rebuildList()
  {
    Scene theScene = window.getScene();
    TreeElement allEl[] = theList.getElements();

    theList.setUpdateEnabled(false);
    theList.removeAllElements();
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.selected)
          {
            TreeElement el = new ObjectTreeElement(info, null, theList, false);
            theList.addElement(el);
            el.setExpanded(true);
          }
      }
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (!info.selected || info.getTracks().length == 0)
          continue;
        ObjectTreeElement el = (ObjectTreeElement) theList.findElement(info);
        el.addTracks();
      }
    for (int i = 0; i < allEl.length; i++)
      {
        TreeElement el = theList.findElement(allEl[i].getObject());
        if (el == null)
          continue;
        el.setExpanded(allEl[i].isExpanded());
        el.setSelected(allEl[i].isSelected());
      }
    allEl = theList.getElements();
    for (int i = 0; i < allEl.length; i++)
      allEl[i].setSelectable(allEl[i] instanceof TrackTreeElement);
    theList.setUpdateEnabled(true);
    selectedTracksChanged();
    updateScrollbar();
    repaintGraphs();
    setScrollPosition(scrollPos);
  }
  
  /** Layout the display in the right side of the Score, based on the current view mode
      and selected tracks. */

  private void layoutGraphs()
  {
    Track tr[] = getSelectedTracks();
    graphs.removeAllElements();
    int divider = div.getDividerLocation();
    if (view == TRACKS_MODE)
    {
      FormContainer graphContainer = new FormContainer(new double [] {1.0}, new double [] {0.0, 1.0});
      TracksPanel theTracks = new TracksPanel(window, theList, this, window.getScene().getFramesPerSecond(), timeScale);
      theTracks.setStartTime(startTime);
      theTracks.addMarker(timeMarker);
      theTracks.setMode(mode);
      graphContainer.add(theAxis, 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(0, 0, 2, 0), null));
      graphContainer.add(theTracks, 0, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
      graphs.addElement(theTracks);
      div.add(graphContainer, 1);
      graphContainer.addEventLink(MouseScrolledEvent.class, this, "mouseScrolled");
    }
    else if (view == SINGLE_GRAPH_MODE || tr.length == 0)
    {
      FormContainer graphContainer = new FormContainer(new double [] {0.0, 1.0}, new double [] {0.0, 1.0});
      TrackGraph gr = new TrackGraph(window, this, theAxis);
      gr.setSubdivisions(window.getScene().getFramesPerSecond());
      gr.setStartTime(startTime);
      gr.setScale(timeScale);
      gr.addMarker(timeMarker);
      gr.setMode(mode);
      gr.setTracks(tr);
      gr.setBackground(Color.white);
      graphContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
      graphContainer.add(theAxis, 1, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(0, 0, 2, 0), null));
      graphContainer.add(gr.getAxis(), 0, 1);
      graphContainer.add(gr, 1, 1);
      graphs.addElement(gr);
      div.add(graphContainer, 1);
      graphContainer.addEventLink(MouseScrolledEvent.class, this, "mouseScrolled");
    }
    else if (view == MULTI_GRAPH_MODE)
    {
      double weight[] = new double [tr.length+1];
      for (int i = 1; i < weight.length; i++)
        weight[i] = 1.0;
      FormContainer graphContainer = new FormContainer(new double [] {0.0, 1.0}, weight);
      graphContainer.add(theAxis, 1, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(0, 0, 2, 0), null));
      for (int i = 0; i < tr.length; i++)
      {
        TrackGraph gr = new TrackGraph(window, this, theAxis);
        gr.setSubdivisions(window.getScene().getFramesPerSecond());
        gr.setStartTime(startTime);
        gr.setScale(timeScale);
        gr.addMarker(timeMarker);
        gr.setMode(mode);
        gr.setTracks(new Track [] {tr[i]});
        gr.setBackground(Color.white);
        LayoutInfo layout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, new Insets(i == 0 ? 0 : 4, 0, 0, 0), null);
        graphContainer.add(gr.getAxis(), 0, i+1, layout);
        graphContainer.add(gr, 1, i+1, layout);
        graphs.addElement(gr);
      }
      div.add(graphContainer, 1);
      graphContainer.addEventLink(MouseScrolledEvent.class, this, "mouseScrolled");
    }
    for (Widget child : div.getChildren())
      UIUtilities.applyDefaultBackground(child);
    UIUtilities.applyDefaultFont(div);
    div.setDividerLocation(divider);
    div.layoutChildren();
    repaintAll();
  }
  
  /** Get the starting time to display. */
  
  public double getStartTime()
  {
    return startTime;
  }
  
  /** Set the starting time to display. */
  
  public void setStartTime(double time)
  {
    theAxis.setStartTime(time);
    for (int i = 0; i < graphs.size(); i++)
      graphs.elementAt(i).setStartTime(time);
    startTime = time;
    repaintGraphs();
  }
  
  /** Get the number of pixels per unit time. */
  
  public double getScale()
  {
    return timeScale;
  }
  
  /** Set the number of pixels per unit time. */
  
  public void setScale(double s)
  {
    theAxis.setScale(s);
    for (int i = 0; i < graphs.size(); i++)
      ((TrackDisplay) graphs.elementAt(i)).setScale(s);
    timeScale = s;
    repaintGraphs();
  }
  
  /** Set the current time. */
  
  public void setTime(double time)
  {
    if (hasRepaintedView != null)
      Arrays.fill(hasRepaintedView, false);
    timeMarker.position = time;
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(3);
    timeFrameLabel.setText(Translate.text("timeFrameLabel", nf.format(time), Integer.toString((int) Math.round(time*window.getScene().getFramesPerSecond()))));
    int graphWidth = ((Widget) graphs.elementAt(0)).getBounds().width;
    if (time < startTime)
      setStartTime(time);
    else if (time > startTime+graphWidth/timeScale)
      setStartTime(time-0.5*graphWidth/timeScale);
    else
      repaintGraphs();
  }

  /** Start animating the display. */

  public void startAnimating()
  {
    if (hasRepaintedView == null)
    {
      // The first time this is called, add a listener to all the views in the window so we can tell when they've all
      // been repainted.

      hasRepaintedView = new boolean[window.getAllViews().length];
      Object listener = new Object() {
        void viewRepainted(RepaintEvent ev)
        {
          boolean allRepainted = true;
          for (int i = 0; i < hasRepaintedView.length; i++)
          {
            if (ev.getWidget() == window.getAllViews()[i])
              hasRepaintedView[i] = true;
            allRepainted &= hasRepaintedView[i];
          }
          if (isAnimating && (!window.getSplitView() || allRepainted))
          {
            // Update the time to show the next frame.

            double elapsedTime = (System.currentTimeMillis()-animateStartClockTime)*0.001;
            double sceneTime = animateStartSceneTime+elapsedTime*playbackSpeed;
            int fps = window.getScene().getFramesPerSecond();
            sceneTime = ((int) (sceneTime*fps))/(double) fps;
            window.setTime(sceneTime);
          }
        }
      };
      for (ViewerCanvas view : window.getAllViews())
        view.addEventLink(RepaintEvent.class, listener, "viewRepainted");
    }
    animateStartSceneTime = window.getScene().getTime();
    animateStartClockTime = System.currentTimeMillis();
    isAnimating = true;
    playButton.setIcon(stopIcon);
    rewindButton.setEnabled(false);
    endButton.setEnabled(false);
    window.setTime(animateStartSceneTime);
  }

  /** Stop animating the display. */

  public void stopAnimating()
  {
    isAnimating = false;
    playButton.setIcon(playIcon);
    rewindButton.setEnabled(true);
    endButton.setEnabled(true);
  }

  /** Get whether the display is currently being animated. */

  public boolean getAnimating()
  {
    return isAnimating;
  }

  /**
   * Get the playback speed.
   */

  public double getPlaybackSpeed()
  {
    return playbackSpeed;
  }

  /**
   * Set the playback speed.
   */

  public void setPlaybackSeed(double speed)
  {
    playbackSpeed = speed;
    int speedIndex;
    for (speedIndex = 0; speedIndex < SPEEDS.length && speed > SPEEDS[speedIndex]; speedIndex++)
      ;
    speedSlider.setValue(speedIndex);
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(1);
    speedLabel.setText(Translate.text("playbackSpeedLabel", nf.format(speed)));
    animateStartSceneTime = window.getScene().getTime();
    animateStartClockTime = System.currentTimeMillis();
  }
  
  /** Respond to the scroll wheel. */
  
  private void mouseScrolled(MouseScrolledEvent ev)
  {
    setStartTime(startTime+ev.getUnitsToScroll()/(double) window.getScene().getFramesPerSecond());
  }
  
  /** Update the menus when the selection changes. */
  
  private void treeSelectionChanged()
  {
    selectedTracksChanged();
    window.updateMenus();
  }
  
  /** This is called whenever there is a change to which tracks are selected.  It updates
      the list of selected keyframes, removing any which are no longer appropriate. */
  
  private void selectedTracksChanged()
  {
    Vector v = new Vector();
    Track sel[] = getSelectedTracks();

    for (int i = 0; i < selection.length; i++)
      for (int j = 0; j < sel.length; j++)
        if (selection[i].track == sel[j])
          {
            v.addElement(selection[i]);
            break;
          }
    selection = new SelectionInfo [v.size()];
    for (int i = 0; i < selection.length; i++)
      selection[i] = (SelectionInfo) v.elementAt(i);
    if (view == SINGLE_GRAPH_MODE)
      {
        ((TrackGraph) graphs.elementAt(0)).setTracks(sel);
        repaintAll();
      }
    if (view == MULTI_GRAPH_MODE)
      layoutGraphs();
  }

  /** This is called whenever a track is modified.  It causes all graphs to be 
      properly updated and, optionally, updates the Scene to reflect any changes
      to selected keyframes. */
  
  public void tracksModified(boolean updateScene)
  {
    for (int i = 0; i < graphs.size(); i++)
      {
        if (graphs.elementAt(i) instanceof TrackGraph)
          ((TrackGraph) graphs.elementAt(i)).tracksModified();
        else
          ((Widget) graphs.elementAt(i)).repaint();
      }
    if (!updateScene)
      return;
    
    // Find the list of tracks with selected keyframes.

    Vector v = new Vector();
    for (int i = 0; i < selection.length; i++)
      if (!v.contains(selection[i].track))
        v.addElement(selection[i].track);

    // Now update them.

    for (int i = 0; i < v.size(); i++)
      {
        Track tr = (Track) v.elementAt(i);
        Object parent = tr.getParent();
        while (parent != null && parent instanceof Track)
          parent = ((Track) parent).getParent();
        if (parent instanceof ObjectInfo)
          window.getScene().applyTracksToObject((ObjectInfo) parent);
      }
    window.updateImage();
  }
  
  /** Repaint all of the graphs. */
  
  public void repaintGraphs()
  {
    for (int i = 0; i < graphs.size(); i++)
      {
        Widget gr = (Widget) graphs.elementAt(i);
        gr.repaint();
        if (gr instanceof TrackGraph)
          ((TrackGraph) gr).getAxis().repaint();
      }
    theAxis.repaint();
  }
  
  /** Repaint all of the child Widgets. */
  
  public void repaintAll()
  {
    repaintGraphs();
    theList.repaint();
  }
  
  private void elementsMoved()
  {
    repaintGraphs();
  }

  private void elementOpenedOrClosed()
  {
    repaintGraphs();
    updateScrollbar();
  }
  
  private void elementDoubleClicked(TreeList.ElementDoubleClickedEvent ev)
  {
    TreeElement el = ev.getElement();
    if (el != null && el.getObject() instanceof Track)
    {
      Track tr = (Track) el.getObject();
      tr.edit(window);
      Object parent = tr.getParent();
      while (parent != null && parent instanceof Track)
        parent = ((Track) parent).getParent();
      if (parent instanceof ObjectInfo)
        window.getScene().applyTracksToObject((ObjectInfo) parent);
      window.updateImage();
      selectedTracksChanged();
      repaintAll();
    }
  }
  
  /** Scroll the list and the tracks together. */
  
  public void setScrollPosition(int pos)
  {
    Rectangle size = theList.getBounds();
    Dimension prefSize = theList.getPreferredSize();

    if (pos > prefSize.height-size.height)
      pos = prefSize.height-size.height;
    if (pos < 0)
      pos = 0;
    if (pos == scrollPos)
      return;
    theList.setYOffset(-pos);
    for (int i = 0; i < graphs.size(); i++)
      graphs.elementAt(i).setYOffset(-pos);
    yoffset = -pos;
    theList.repaint();
    repaintGraphs();
    scrollPos = pos;
    updateScrollbar();
  }
  
  /** Respond to changes on the scrollbar. */
  
  private void scrollbarChanged(ValueChangedEvent ev)
  {
    setScrollPosition(scroll.getValue());
  }
  
  /** Update the bounds of the scrollbar. */
  
  private void updateScrollbar()
  {
    int height = theList.getPreferredSize().height, showing = theList.getBounds().height;
    scroll.setMaximum(height);
    scroll.setExtent(showing);
    scroll.setValue(scrollPos);
    scroll.setUnitIncrement(Math.max(theList.getRowHeight(), 1));
    scroll.setBlockIncrement(Math.max(showing-theList.getRowHeight(), 1));
  }
  
  /** This is called when a time marker has been moved.  If this is an intermediate 
      position in the middle of a drag, then intermediate will be true. */
  
  public void markerMoved(Marker m, boolean intermediate)
  {
    if (/*!intermediate && */m == timeMarker && m.position != window.getScene().getTime())
      window.setTime(m.position);
    else
      repaintGraphs();
  }
  
  /** Make sure the scrollbar gets adjusted when the score is resized. */

  public void layoutChildren()
  {
//    theAxis.setSize(theAxis.getSize().width, theAxis.getPreferredSize().height); // Workaround for layout manager bug.
    super.layoutChildren();
    updateScrollbar();
  }
  
  /** Allow the user to edit the currently selected track. */
  
  public void editSelectedTrack()
  {
    Object sel[] = theList.getSelectedObjects();
    if (sel.length == 1 && sel[0] instanceof Track)
      {
        Track tr = (Track) sel[0];
        tr.edit(window);
        finishEditingTrack(tr);
      }
  }
  
  /** This method should be called when a track is done being edited. */
  
  public void finishEditingTrack(Track tr)
  {
    Object parent = tr.getParent();
    while (parent != null && parent instanceof Track)
      parent = ((Track) parent).getParent();
    if (parent instanceof ObjectInfo)
      window.getScene().applyTracksToObject((ObjectInfo) parent);
    setModified();
    window.updateImage();
    selectedTracksChanged();
    repaintAll();
  }
  
  /** Enable or disable all selected tracks. */
  
  public void setTracksEnabled(boolean enable)
  {
    Object sel[] = theList.getSelectedObjects();
    UndoRecord undo = new UndoRecord(window, false);
    Vector v = new Vector();

    for (int i = 0; i < sel.length; i++)
      if (sel[i] instanceof Track)
        {
          Track tr = (Track) sel[i];
          Object parent = tr.getParent();
          while (parent instanceof Track)
            parent = ((Track) parent).getParent();
          if (parent instanceof ObjectInfo && v.indexOf(parent) == -1)
            {
              v.addElement(parent);
              undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {parent, ((ObjectInfo) parent).duplicate()});
            }
          tr.setEnabled(enable);
        }
    for (int i = 0; i < v.size(); i++)
      window.getScene().applyTracksToObject((ObjectInfo) v.elementAt(i));
    theList.repaint();
    window.setUndoRecord(undo);
    window.updateImage();
    window.updateMenus();
  }

  /** Add a keyframe to each selected track, based on the current state of the scene. */
  
  public void keyframeSelectedTracks()
  {
    Scene theScene = window.getScene();
    Object sel[] = theList.getSelectedObjects();
    double time = theScene.getTime();
    UndoRecord undo = new UndoRecord(window, false);
    Vector newkeys = new Vector();
    
    for (int i = 0; i < sel.length; i++)
      if (sel[i] instanceof Track)
        {
          Track tr = (Track) sel[i];
          if (tr.getParent() instanceof ObjectInfo)
            {
              ObjectInfo info = (ObjectInfo) tr.getParent();
              for (int j = 0; j < info.getTracks().length; j++)
                if (info.getTracks()[j] == tr)
                  undo.addCommand(UndoRecord.SET_TRACK, new Object [] {info, new Integer(j), tr.duplicate(info)});
            }
          Keyframe k = tr.setKeyframe(time, theScene);
          if (k != null)
            newkeys.addElement(new SelectionInfo(tr, k));
        }
    window.setUndoRecord(undo);
    if (newkeys.size() > 0)
      {
        SelectionInfo newsel[] = new SelectionInfo [newkeys.size()];
        newkeys.copyInto(newsel);
        setSelectedKeyframes(newsel);
      }
    selectedTracksChanged();
    repaintGraphs();
    window.updateMenus();
  }
  
  /** Add a keyframe to the tracks of selected objects which have been modified. */
  
  public void keyframeModifiedTracks()
  {
    Scene theScene = window.getScene();
    int sel[] = window.getSelectedIndices();
    double time = theScene.getTime();
    UndoRecord undo = new UndoRecord(window, false);
    Vector newkeys = new Vector();
    
    for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        boolean posx = false, posy = false, posz = false;
        boolean rotx = false, roty = false, rotz = false;
        for (int j = 0; j < info.getTracks().length; j++)
          {
            Track tr = info.getTracks()[j];
            if (!tr.isEnabled())
              continue;
            if (tr instanceof PositionTrack && posx && posy && posz)
              continue;
            if (tr instanceof RotationTrack && rotx && roty && rotz)
              continue;
            undo.addCommand(UndoRecord.SET_TRACK, new Object [] {info, new Integer(j), tr.duplicate(info)});
            Keyframe k = tr.setKeyframeIfModified(time, theScene);
            if (k != null)
              {
                newkeys.addElement(new SelectionInfo(tr, k));
                if (tr instanceof PositionTrack)
                  {
                    PositionTrack pt = (PositionTrack) tr;
                    posx |= pt.affectsX();
                    posy |= pt.affectsY();
                    posz |= pt.affectsZ();
                  }
                if (tr instanceof RotationTrack)
                  {
                    RotationTrack rt = (RotationTrack) tr;
                    rotx |= rt.affectsX();
                    roty |= rt.affectsY();
                    rotz |= rt.affectsZ();
                  }
              }
          }
      }
    window.setUndoRecord(undo);
    if (newkeys.size() > 0)
      {
        SelectionInfo newsel[] = new SelectionInfo [newkeys.size()];
        newkeys.copyInto(newsel);
        setSelectedKeyframes(newsel);
      }
    selectedTracksChanged();
    repaintGraphs();
    window.updateMenus();
  }
  
  /** Duplicate the selected tracks. */
  
  public void duplicateSelectedTracks()
  {
    Object sel[] = theList.getSelectedObjects();
    UndoRecord undo = new UndoRecord(window, false);
    Vector modifiedObj = new Vector(), addedTrack = new Vector();
    
    for (int i = 0; i < sel.length; i++)
      if (sel[i] instanceof Track)
        {
          Track tr = (Track) sel[i];
          if (!(tr.getParent() instanceof ObjectInfo))
            continue;
          ObjectInfo info = (ObjectInfo) tr.getParent();
          if (modifiedObj.indexOf(info) < 0)
            {
              undo.addCommand(UndoRecord.SET_TRACK_LIST, new Object [] {info, info.getTracks()});
              modifiedObj.addElement(info);
            }
          for (int j = 0; j < info.getTracks().length; j++)
            if (info.getTracks()[j] == tr)
              {
                Track newtr = tr.duplicate(info);
                newtr.setName("Copy of "+tr.getName());
                info.addTrack(newtr, j+1);
                addedTrack.addElement(newtr);
              }
        }
    window.setUndoRecord(undo);
    rebuildList();
    for (int i = 0; i < addedTrack.size(); i++)
      {
        TreeElement el = theList.findElement(addedTrack.elementAt(i));
        if (el == null)
          continue;
        el.setSelected(true);
      }
    repaintGraphs();
  }
  
  /** Delete the selected tracks. */
  
  public void deleteSelectedTracks()
  {
    Object sel[] = theList.getSelectedObjects();
    UndoRecord undo = new UndoRecord(window, false);
    Vector modifiedObj = new Vector();
    
    for (int i = 0; i < sel.length; i++)
      if (sel[i] instanceof Track)
        {
          Track tr = (Track) sel[i];
          if (!(tr.getParent() instanceof ObjectInfo))
            continue;
          ObjectInfo info = (ObjectInfo) tr.getParent();
          if (modifiedObj.indexOf(info) < 0)
            {
              undo.addCommand(UndoRecord.SET_TRACK_LIST, new Object [] {info, info.getTracks()});
              modifiedObj.addElement(info);
            }
          info.removeTrack(tr);
        }
    window.setUndoRecord(undo);
    rebuildList();
    repaintGraphs();
  }
  
  /** Select all tracks of selected objects. */
  
  public void selectAllTracks()
  {
    Scene theScene = window.getScene();
    int sel[] = window.getSelectedIndices();
    
    theList.setUpdateEnabled(false);
    for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        TreeElement el = theList.findElement(info);
        if (el != null)
          for (int j = 0; j < el.getNumChildren(); j++)
            theList.setSelected(el.getChild(j), true);
      }
    theList.setUpdateEnabled(true);
    selectedTracksChanged();
    window.updateMenus();
  }
  
  /** Add a track to the specified objects. */
  
  public void addTrack(Object obj[], Class trackClass, Object extraArgs[], boolean deselectOthers)
  {
    Scene theScene = window.getScene();
    UndoRecord undo = new UndoRecord(window, false);
    Vector added = new Vector();
    Object args[];
    if (extraArgs == null)
      args = new Object [1];
    else
      {
        args = new Object [extraArgs.length+1];
        for (int i = 0; i < extraArgs.length; i++)
          args[i+1] = extraArgs[i];
      }
    Constructor con[] = trackClass.getConstructors();
    int which;
    for (which = 0; which < con.length && con[which].getParameterTypes().length != args.length; which++);
    try
      {
        for (int i = 0; i < obj.length; i++)
          if (obj[i] instanceof ObjectInfo)
            {
              ObjectInfo info = (ObjectInfo) obj[i];
              if (trackClass == PoseTrack.class)
                {
                  Object3D posable = info.getObject().getPosableObject();
                  if (posable == null)
                    continue;
                  if (posable != info.getObject())
                    {
                      String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
                      BStandardDialog dlg = new BStandardDialog("", UIUtilities.breakString(Translate.text("mustConvertToActor", info.getName())), BStandardDialog.QUESTION);
                      int choice = dlg.showOptionDialog(window, options, options[0]);
                      if (choice == 1)
                        continue;
                      theScene.replaceObject(info.getObject(), posable, undo);
                    }
                }
              undo.addCommand(UndoRecord.SET_TRACK_LIST, new Object [] {info, info.getTracks()});
              args[0] = info;
              Track newtrack = (Track) con[which].newInstance(args);
              info.addTrack(newtrack, 0);
              added.addElement(newtrack);
            }
      }
    catch (Exception ex)
      {
        ex.printStackTrace();
      }
    window.setUndoRecord(undo);
    if (deselectOthers)
      theList.deselectAll();
    rebuildList();
    for (int i = 0; i < added.size(); i++)
      theList.setSelected(added.elementAt(i), true);
    selectedTracksChanged();
    window.updateMenus();
  }
  
  /** Edit the selected keyframe. */
  
  public void editSelectedKeyframe()
  {
    if (selection.length != 1)
      return;
    selection[0].track.editKeyframe(window, selection[0].keyIndex);
    tracksModified(true);
  }

  /** Delete all selected keyframes. */
  
  public void deleteSelectedKeyframes()
  {
    Hashtable changedTracks = new Hashtable();
    for (int i = 0; i < selection.length; i++)
      {
        Track tr = selection[i].track;
        Keyframe keys[] = tr.getTimecourse().getValues();
        for (int j = 0; j < keys.length; j++)
          if (keys[j] == selection[i].key)
            {
              if (changedTracks.get(tr) == null)
                changedTracks.put(tr, tr.duplicate(tr.getParent()));
              tr.deleteKeyframe(j);
              break;
            }
      }
    selection = new SelectionInfo [0];
    UndoRecord undo = new UndoRecord(window, false);
    Enumeration tracks = changedTracks.keys();
    while (tracks.hasMoreElements())
      {
        Track tr = (Track) tracks.nextElement();
        Object parent = tr.getParent();
        while (parent != null && parent instanceof Track)
          parent = ((Track) parent).getParent();
        if (parent instanceof ObjectInfo)
          window.getScene().applyTracksToObject((ObjectInfo) parent);
        undo.addCommand(UndoRecord.COPY_TRACK, new Object [] {tr, changedTracks.get(tr)});
      }
    window.setUndoRecord(undo);
    window.updateMenus();
    tracksModified(true);
  }

  private void clickedPlay()
  {
    if (isAnimating)
      stopAnimating();
    else
      startAnimating();
  }

  private void clickedRewind()
  {
    // Find the earliest keyframe on any track of any object.

    double minTime = Math.min(0.0, window.getScene().getTime());
    for (ObjectInfo obj : window.getScene().getAllObjects())
    {
      for (Track track : obj.getTracks())
      {
        double keyTimes[] = track.getKeyTimes();
        if (keyTimes.length > 0)
          minTime = Math.min(minTime, keyTimes[0]);
      }
    }
    if (minTime != window.getScene().getTime())
      window.setTime(minTime);
  }

  private void clickedEnd()
  {
    // Find the latest keyframe on any track of any object.

    double maxTime = Math.max(0.0, window.getScene().getTime());
    for (ObjectInfo obj : window.getScene().getAllObjects())
    {
      for (Track track : obj.getTracks())
      {
        double keyTimes[] = track.getKeyTimes();
        if (keyTimes.length > 0)
          maxTime = Math.max(maxTime, keyTimes[keyTimes.length-1]);
      }
    }
    if (maxTime != window.getScene().getTime())
      window.setTime(maxTime);
  }

  private void speedChanged()
  {
    setPlaybackSeed(SPEEDS[speedSlider.getValue()]);
  }

  /** EditingWindow methods.  Most of these either do nothing, or simply call through to 
      the corresponding methods of the LayoutWindow the Score is in. */

  public boolean confirmClose()
  {
    return true;
  }

  public ToolPalette getToolPalette()
  {
    return modeTools;
  }

  public void setTool(EditingTool tool)
  {
    if (view != viewTools.getSelection())
      {
        view = viewTools.getSelection();
        layoutGraphs();
      }
    if (mode != modeTools.getSelection())
      {
        mode = modeTools.getSelection();
        for (int i = 0; i < graphs.size(); i++)
          graphs.elementAt(i).setMode(mode);
        setHelpText(MODE_HELP_TEXT[mode]);
      }
  }
  
  public void setHelpText(String text)
  {
    helpText.setText(text);
  }
  
  public BFrame getFrame()
  {
    return window;
  }

  public void updateImage()
  {
  }

  public void updateMenus()
  {
  }
  
  public void setUndoRecord(UndoRecord command)
  {
    window.setUndoRecord(command);
  }

  public void setModified()
  {
    window.setModified();
  }

  public Scene getScene()
  {
    return window.getScene();
  }
  
  public ViewerCanvas getView()
  {
    return null;
  }

  public ViewerCanvas[] getAllViews()
  {
    return null;
  }
}
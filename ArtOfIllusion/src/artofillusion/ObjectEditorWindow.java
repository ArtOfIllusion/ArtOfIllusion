/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import artofillusion.keystroke.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;

/** The ObjectEditorWindow class represents a window for editing an object.  This is an
    abstract class, with subclasses for various types of objects. */

public abstract class ObjectEditorWindow extends BFrame implements EditingWindow
{
  protected EditingWindow parentWindow;
  protected ObjectInfo objInfo;
  protected ViewerCanvas theView[];
  protected BorderContainer viewPanel[];
  protected FormContainer viewsContainer;
  protected int numViewsShown, currentView;
  protected ToolPalette tools;
  protected EditingTool defaultTool, currentTool;
  protected BLabel helpText;
  protected BMenuBar menubar;
  protected UndoStack undoStack;
  protected Preferences preferences;
  private boolean hasNotifiedPlugins, sceneChangePending;
  private SceneChangedEvent sceneChangedEvent;

  protected static boolean lastShowAxes, lastShowGrid, lastSnapToGrid;
  protected static int lastNumViews = 4, lastGridSubdivisions = 10;
  protected static double lastGridSpacing = 1.0;

  public ObjectEditorWindow(EditingWindow parent, String title, ObjectInfo obj)
  {
    super(title);
    parentWindow = parent;
    sceneChangedEvent = new SceneChangedEvent(this);
    objInfo = obj.duplicate(obj.getObject().duplicate());
    objInfo.getCoords().setOrigin(new Vec3());
    objInfo.getCoords().setOrientation(Vec3.vz(), Vec3.vy());
    objInfo.clearDistortion();
  }

  /**
   * Initialize various objects that will be used by the window.  Subclasses typically invoke this from
   * their constructors.
   */

  protected void initialize()
  {
    undoStack = new UndoStack();
    if (ArtOfIllusion.APP_ICON != null)
      setIcon(ArtOfIllusion.APP_ICON);
    preferences = Preferences.userNodeForPackage(getClass()).node("ObjectEditorWindow");
    loadPreferences();
    addEventLink(WindowClosingEvent.class, new Object() {
      void processEvent()
      {
        BStandardDialog dlg = new BStandardDialog("", Translate.text("saveWindowChanges"), BStandardDialog.QUESTION);
        String options[] = new String [] {
          Translate.text("saveChanges"),
          Translate.text("discardChanges"),
          Translate.text("button.cancel")
        };
        int choice = dlg.showOptionDialog(ObjectEditorWindow.this, options, options[0]);
        if (choice == 0)
          doOk();
        else if (choice == 1)
          doCancel();
      }
    });
    numViewsShown = lastNumViews;
    viewPanel = new BorderContainer [4];
    viewsContainer = new FormContainer(new double [] {1, numViewsShown == 1 ? 0 : 1}, new double [] {1, numViewsShown == 1 ? 0 : 1});
    viewsContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    theView = new ViewerCanvas [4];
    Object listen = new Object() {
      void processEvent(MousePressedEvent ev)
      {
        setCurrentView((ViewerCanvas) ev.getWidget());
      }
    };
    for (int i = 0; i < theView.length; i++)
    {
      viewPanel[i] = new BorderContainer() {
        public Dimension getPreferredSize()
        {
          return new Dimension(0, 0);
        }
        public Dimension getMinimumSize()
        {
          return new Dimension(0, 0);
        }
      };
      RowContainer controls = new RowContainer();
      viewPanel[i].add(controls, BorderContainer.NORTH);
      viewPanel[i].add(theView[i] = createViewerCanvas(i, controls), BorderContainer.CENTER);
      theView[i].setShowAxes(lastShowAxes);
      theView[i].setGrid(lastGridSpacing, lastGridSubdivisions, lastShowGrid, lastSnapToGrid);
      theView[i].addEventLink(MousePressedEvent.class, listen);
    }
    theView[1].setOrientation(2);
    theView[2].setOrientation(4);
    theView[3].setPerspective(true);
    theView[currentView].setDrawFocus(true);
    viewsContainer.add(viewPanel[0], 0, 0);
    viewsContainer.add(viewPanel[1], 1, 0);
    viewsContainer.add(viewPanel[2], 0, 1);
    viewsContainer.add(viewPanel[3], 1, 1);
    menubar = new BMenuBar();
    setMenuBar(menubar);
  }

  /**
   * This is invoked to create each of the ViewerCanvases in the window.
   * @param index      the index of the canvas to create (from 0 to 3)
   * @param controls   the contain to which the canvas should add its controls
   */

  protected abstract ViewerCanvas createViewerCanvas(int index, RowContainer controls);

  /** We need to be notified of key events which take place anywhere in the window.  To make
      sure this happens, we need to add the window as a KeyPressed listener to every component
      contained in it. */

  protected void recursivelyAddListeners(Widget w)
  {
    if (!(w instanceof TextWidget))
      w.addEventLink(KeyPressedEvent.class, this, "keyPressed");
    if (w instanceof WidgetContainer)
      {
        Iterator children = ((WidgetContainer) w).getChildren().iterator();
        while (children.hasNext())
          recursivelyAddListeners((Widget) children.next());
      }
  }

  /** Load all the preferences into memory. */

  protected void loadPreferences()
  {
    lastShowAxes = preferences.getBoolean("showAxes", lastShowAxes);
    lastShowGrid = preferences.getBoolean("showGrid", lastShowGrid);
    lastSnapToGrid = preferences.getBoolean("snapToGrid", lastSnapToGrid);
    lastGridSpacing = preferences.getDouble("gridSpacing", lastGridSpacing);
    lastGridSubdivisions = preferences.getInt("gridSubdivisions", lastGridSubdivisions);
    lastNumViews = preferences.getInt("numViews", lastNumViews);
  }

  /** Save user settings that should be persistent between sessions. */

  protected void savePreferences()
  {
    preferences.putBoolean("showAxes", lastShowAxes);
    preferences.putBoolean("showGrid", lastShowGrid);
    preferences.putBoolean("snapToGrid", lastSnapToGrid);
    preferences.putDouble("gridSpacing", lastGridSpacing);
    preferences.putInt("gridSubdivisions", lastGridSubdivisions);
    preferences.putInt("numViews", lastNumViews);
  }

  /** Utility routine that loads an array of booleans (encoded as bytes) from the preferences. */

  protected boolean [] loadBooleanArrayPreference(String key, boolean def[])
  {
    byte bytes[] = preferences.getByteArray(key, null);
    if (bytes == null)
      return def;
    boolean value[] = new boolean [bytes.length];
    for (int i = 0; i < value.length; i++)
      value[i] = (bytes[i] != 0);
    return value;
  }

  /** Utility routine that saves an array of booleans to the preferences. */

  protected void saveBooleanArrayPreference(String key, boolean value[])
  {
    byte bytes[] = new byte [value.length];
    for (int i = 0; i < value.length; i++)
      bytes[i] = (byte) (value[i] ? 1 : 0);
    preferences.putByteArray(key, bytes);
  }

  /* EditingWindow methods. */

  public void setTool(EditingTool tool)
  {
    for (int i = 0; i < theView.length; i++)
      theView[i].setTool(tool);
    currentTool = tool;
  }

  public boolean confirmClose()
  {
    return true;
  }

  public void setHelpText(String text)
  {
    helpText.setText(text);
  }

  public BFrame getFrame()
  {
    return this;
  }

  public void updateImage()
  {
    for (ViewerCanvas view : theView)
      view.repaint();
  }

  public void setUndoRecord(UndoRecord command)
  {
    undoStack.addRecord(command);
    setModified();
    updateMenus();
  }

  public void setModified()
  {
    dispatchSceneChangedEvent();
  }

  /** Cause a SceneChangedEvent to be dispatched to this window's listeners. */

  private void dispatchSceneChangedEvent()
  {
    if (sceneChangePending)
      return; // There's already a Runnable on the event queue waiting to dispatch a SceneChangedEvent.
    sceneChangePending = true;
    EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        sceneChangePending = false;
        dispatchEvent(sceneChangedEvent);
      }
    });
  }

  protected void keyPressed(KeyPressedEvent e)
  {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    if (manager.getFocusedWindow() != getComponent() || manager.getFocusOwner() instanceof TextComponent)
      return;
    tools.getSelectedTool().keyPressed(e, theView[currentView]);
    if (!e.isConsumed())
      KeystrokeManager.executeKeystrokes(e, this);
  }

  /** Get the currently selected ViewerCanvas for this window. */

  public ViewerCanvas getView()
  {
    return theView[currentView];
  }

  /**
   * Set which ViewerCanvas has focus.
   *
   * @param view  the ViewerCanvas which should become the currently focused view.  If this
   * is not one of the views belonging to this window, this method does nothing.
   */

  public void setCurrentView(ViewerCanvas view)
  {
    for (int i = 0; i < theView.length; i++)
      if (currentView != i && view == theView[i])
      {
        theView[currentView].setDrawFocus(false);
        theView[i].setDrawFocus(true);
        currentView = i;
        updateMenus();
        updateImage();
      }
  }

  /** Get the ToolPalette for this window. */

  public ToolPalette getToolPalette()
  {
    return tools;
  }

  /** Get all ViewerCanvases in this window. */

  public ViewerCanvas[] getAllViews()
  {
    return new ViewerCanvas [] {theView[0], theView[1], theView[2], theView[3]};
  }

  /** Toggle whether the coordinate axes are shown. */

  public void showAxesCommand()
  {
    boolean wasShown = theView[currentView].getShowAxes();
    for (int i = 0; i < theView.length; i++)
      theView[i].setShowAxes(!wasShown);
    lastShowAxes = !wasShown;
    savePreferences();
    updateMenus();
    updateImage();
  }

  /** Toggle whether the template is shown. */

  public void showTemplateCommand()
  {
    boolean wasShown = theView[currentView].getTemplateShown();
    theView[currentView].setShowTemplate(!wasShown);
    updateMenus();
    updateImage();
  }

  /** Toggle whether there are one or four views shown. */

  public void toggleViewsCommand()
  {
    if (numViewsShown == 4)
    {
      numViewsShown = 1;
      viewsContainer.setColumnWeight(0, (currentView == 0 || currentView == 2) ? 1 : 0);
      viewsContainer.setColumnWeight(1, (currentView == 1 || currentView == 3) ? 1 : 0);
      viewsContainer.setRowWeight(0, (currentView == 0 || currentView == 1) ? 1 : 0);
      viewsContainer.setRowWeight(1, (currentView == 2 || currentView == 3) ? 1 : 0);
    }
    else
    {
      numViewsShown = 4;
      viewsContainer.setColumnWeight(0, 1);
      viewsContainer.setColumnWeight(1, 1);
      viewsContainer.setRowWeight(0, 1);
      viewsContainer.setRowWeight(1, 1);
    }
    viewsContainer.layoutChildren();
    lastNumViews = numViewsShown;
    savePreferences();
    updateMenus();
    updateImage();
    viewPanel[currentView].requestFocus();
  }

  /** Allow the user to set the template image. */

  public void setTemplateCommand()
  {
    BFileChooser fc = new ImageFileChooser(Translate.text("selectTemplateImage"));
    if (!fc.showDialog(this))
      return;
    try
    {
      theView[currentView].setTemplateImage(fc.getSelectedFile());
    }
    catch (InterruptedException ex)
    {
      new BStandardDialog("", Translate.text("errorLoadingImage", fc.getSelectedFile().getName()), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    theView[currentView].setShowTemplate(true);
    updateMenus();
    updateImage();
  }

  /** Set the grid options for the current window. */

  public void setGridCommand()
  {
    ValueField spaceField = new ValueField(theView[currentView].gridSpacing, ValueField.POSITIVE);
    ValueField divField = new ValueField(theView[currentView].gridSubdivisions, ValueField.POSITIVE+ValueField.INTEGER);
    BCheckBox showBox = new BCheckBox(Translate.text("showGrid"), theView[currentView].showGrid);
    BCheckBox snapBox = new BCheckBox(Translate.text("snapToGrid"), theView[currentView].snapToGrid);
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("gridTitle"),
        new Widget [] {spaceField, divField, showBox, snapBox},
        new String [] {Translate.text("gridSpacing"), Translate.text("snapToSubdivisions"), null, null});
    if (!dlg.clickedOk())
      return;
    lastGridSpacing = spaceField.getValue();
    lastGridSubdivisions = (int) divField.getValue();
    lastShowGrid = showBox.getState();
    lastSnapToGrid = snapBox.getState();
    for (int i = 0; i < theView.length; i++)
      theView[i].setGrid(lastGridSpacing, lastGridSubdivisions, lastShowGrid, lastSnapToGrid);
    savePreferences();
    updateImage();
  }

  /** Undo the most recent action. */

  public void undoCommand()
  {
    undoStack.executeUndo();
    updateImage();
    updateMenus();
  }

  /** Redo the last action that was undone. */

  public void redoCommand()
  {
    undoStack.executeRedo();
    updateImage();
    updateMenus();
  }

  /** This will be called when the user clicks the OK button. */

  protected abstract void doOk();

  /** This will be called when the user clicks the Cancel button. */

  protected abstract void doCancel();

  /**
   * This is overridden to notify all plugins when the window is shown for the first time.
   */

  public void setVisible(boolean visible)
  {
    if (visible && !hasNotifiedPlugins)
    {
      hasNotifiedPlugins = true;
      List<Plugin> plugins = PluginRegistry.getPlugins(Plugin.class);
      for (int i = 0; i < plugins.size(); i++)
        plugins.get(i).processMessage(Plugin.OBJECT_WINDOW_CREATED, new Object[] {this});
    }
    super.setVisible(visible);
  }

  /**
   * This is overridden to notify all plugins when the window is closed.
   */

  public void dispose()
  {
    super.dispose();
    List<Plugin> plugins = PluginRegistry.getPlugins(Plugin.class);
    for (int i = 0; i < plugins.size(); i++)
      plugins.get(i).processMessage(Plugin.OBJECT_WINDOW_CLOSING, new Object[] {this});
  }
}
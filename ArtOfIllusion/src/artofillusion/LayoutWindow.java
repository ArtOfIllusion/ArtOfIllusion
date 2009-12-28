/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.animation.distortion.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.script.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import artofillusion.keystroke.*;
import buoy.event.*;
import buoy.widget.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.prefs.*;
import java.util.*;
import java.util.List;

import buoyx.docking.*;

import javax.swing.text.*;
import javax.swing.*;

/** The LayoutWindow class represents the main window for creating and laying out scenes. */

public class LayoutWindow extends BFrame implements EditingWindow, PopupMenuManager
{
  SceneViewer theView[];
  BorderContainer viewPanel[];
  FormContainer viewsContainer;
  FormContainer centerContainer;
  private DockingContainer dock[];
  BSplitPane div1, div2;
  BScrollPane itemTreeScroller;
  Score theScore;
  ToolPalette tools;
  BLabel helpText, timeFrameLabel;
  TreeList itemTree;
  Scene theScene;
  BMenuBar menubar;
  BMenu fileMenu, recentFilesMenu, editMenu, objectMenu, createMenu, toolsMenu, scriptMenu;
  BMenu animationMenu, editKeyframeMenu,sceneMenu;
  BMenu addTrackMenu, positionTrackMenu, rotationTrackMenu, distortionMenu;
  BMenuItem fileMenuItem[], editMenuItem[], objectMenuItem[], toolsMenuItem[];
  BMenuItem animationMenuItem[], sceneMenuItem[], popupMenuItem[];
  BCheckBoxMenuItem displayItem[];
  BPopupMenu popupMenu;
  UndoStack undoStack;
  int numViewsShown, currentView;
  private boolean modified, sceneChangePending;
  private KeyEventPostProcessor keyEventHandler;
  private SceneChangedEvent sceneChangedEvent;
  private List<ModellingTool> modellingTools;
  protected Preferences preferences;

  /** Create a new LayoutWindow for editing a Scene.  Usually, you will not use this constructor directly.
      Instead, call ModellingApp.newWindow(Scene s). */

  public LayoutWindow(Scene s)
  {
    super(s.getName() == null ? "Untitled" : s.getName());
    theScene = s;
    helpText = new BLabel();
    theScore = new Score(this);
    undoStack = new UndoStack();
    sceneChangedEvent = new SceneChangedEvent(this);
    createItemList();

    // Create the four SceneViewer panels.

    theView = new SceneViewer [4];
    viewPanel = new BorderContainer [4];
    RowContainer row;
    Object listen = new Object() {
      void processEvent(MousePressedEvent ev)
      {
        setCurrentView((ViewerCanvas) ev.getWidget());
      }
    };
    Object keyListener = new Object() {
      public void processEvent(KeyPressedEvent ev)
      {
        handleKeyEvent(ev);
      }
    };
    for (int i = 0; i < 4; i++)
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
      viewPanel[i].add(row = new RowContainer(), BorderContainer.NORTH);
      viewPanel[i].add(theView[i] = new SceneViewer(theScene, row, this), BorderContainer.CENTER);
      theView[i].setGrid(theScene.getGridSpacing(), theScene.getGridSubdivisions(), theScene.getShowGrid(), theScene.getSnapToGrid());
      theView[i].addEventLink(MousePressedEvent.class, listen);
      theView[i].addEventLink(KeyPressedEvent.class, keyListener);
      theView[i].setPopupMenuManager(this);
    }
    theView[1].setOrientation(2);
    theView[2].setOrientation(4);
    theView[3].setOrientation(6);
    theView[3].setPerspective(true);
    theView[currentView].setDrawFocus(true);
    viewsContainer = new FormContainer(new double [] {1, 1}, new double [] {1, 1});
    viewsContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    viewsContainer.add(viewPanel[0], 0, 0);
    viewsContainer.add(viewPanel[1], 1, 0);
    viewsContainer.add(viewPanel[2], 0, 1);
    viewsContainer.add(viewPanel[3], 1, 1);
    centerContainer = new FormContainer(new double [] {0.0, 1.0}, new double [] {0.0, 1.0, 0.0, 0.0});
    centerContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    centerContainer.add(viewsContainer, 1, 0, 1, 3);
    centerContainer.add(helpText, 0, 3, 2, 1);
    dock = new DockingContainer [4];
    dock[0] = new DockingContainer(centerContainer, BTabbedPane.LEFT);
    dock[1] = new DockingContainer(dock[0], BTabbedPane.RIGHT);
    dock[2] = new DockingContainer(dock[1], BTabbedPane.BOTTOM);
    dock[3] = new DockingContainer(dock[2], BTabbedPane.TOP);
    setContent(dock[3]);
    for (int i = 0; i < dock.length; i++)
    {
      dock[i].setHideSingleTab(true);
      dock[i].addEventLink(DockingEvent.class, this, "dockableWidgetMoved");
      BSplitPane split = dock[i].getSplitPane();
      split.setContinuousLayout(true);
      split.setOneTouchExpandable(true);
      BTabbedPane.TabPosition pos = dock[i].getTabPosition();
      split.setResizeWeight(pos == BTabbedPane.TOP || pos == BTabbedPane.LEFT ? 1.0 : 0.0);
      split.addEventLink(ValueChangedEvent.class, this, "updateMenus");
      split.addEventLink(ValueChangedEvent.class, this, "updateMenus");
    }
    ObjectPropertiesPanel propertiesPanel = new ObjectPropertiesPanel(this);
    BScrollPane propertiesScroller = new BScrollPane(propertiesPanel, BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_AS_NEEDED);
    propertiesScroller.getVerticalScrollBar().setUnitIncrement(10);
    propertiesScroller.setBackground(ThemeManager.getAppBackgroundColor());
    getDockingContainer(BTabbedPane.RIGHT).addDockableWidget(new DefaultDockableWidget(itemTreeScroller, Translate.text("Objects")));
    getDockingContainer(BTabbedPane.RIGHT).addDockableWidget(new DefaultDockableWidget(propertiesScroller, Translate.text("Properties")), 0, 1);
    getDockingContainer(BTabbedPane.BOTTOM).addDockableWidget(new DefaultDockableWidget(theScore, Translate.text("Score")));

    // Build the tool palette.

    tools = new ToolPalette(2, 7);
    EditingTool metaTool, altTool, defaultTool, compoundTool;
    tools.addTool(defaultTool = new MoveObjectTool(this));
    tools.addTool(new RotateObjectTool(this));
    tools.addTool(new ScaleObjectTool(this));
    tools.addTool(compoundTool = new MoveScaleRotateObjectTool(this));
    tools.addTool(new CreateCubeTool(this));
    tools.addTool(new CreateSphereTool(this));
    tools.addTool(new CreateCylinderTool(this));
    tools.addTool(new CreateSplineMeshTool(this));
    tools.addTool(new CreatePolygonTool(this));
    tools.addTool(new CreateCurveTool(this));
    tools.addTool(new CreateCameraTool(this));
    tools.addTool(new CreateLightTool(this));
    tools.addTool(metaTool = new MoveViewTool(this));
    tools.addTool(altTool = new RotateViewTool(this));
    if (ArtOfIllusion.getPreferences().getUseCompoundMeshTool())
      defaultTool = compoundTool;
    tools.setDefaultTool(defaultTool);
    tools.selectTool(defaultTool);
    for (int i = 0; i < theView.length; i++)
    {
      theView[i].setMetaTool(metaTool);
      theView[i].setAltTool(altTool);
    }

    // Fill in the left hand panel.

    centerContainer.add(tools, 0, 0);
    centerContainer.add(timeFrameLabel = new BLabel(Translate.text("timeFrameLabel", "0.0", "0"), BLabel.CENTER), 0, 2, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));

    // Build the menubar.

    menubar = new BMenuBar();
    setMenuBar(menubar);
    createFileMenu();
    createEditMenu();
    createSceneMenu();
    createObjectMenu();
    createAnimationMenu();
    createToolsMenu();
    createPopupMenu();
    preferences = Preferences.userNodeForPackage(getClass()).node("LayoutWindow");
    loadPreferences();
    numViewsShown = (numViewsShown == 1 ? 4 : 1);
    toggleViewsCommand();
    keyEventHandler = new KeyEventPostProcessor()
    {
      public boolean postProcessKeyEvent(KeyEvent e)
      {
        if (e.getID() != KeyEvent.KEY_PRESSED || e.isConsumed())
          return false;
        KeyPressedEvent press = new KeyPressedEvent(LayoutWindow.this, e.getWhen(), e.getModifiersEx(), e.getKeyCode());
        handleKeyEvent(press);
        return (press.isConsumed());
      }
    };
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(keyEventHandler);
    addEventLink(WindowActivatedEvent.class, this, "updateMenus");
    addEventLink(WindowClosingEvent.class, new Object() {
      void processEvent()
      {
        ArtOfIllusion.closeWindow(LayoutWindow.this);
      }
    });
    itemTree.setPopupMenuManager(this);
    UIUtilities.applyDefaultFont(getContent());
    UIUtilities.applyDefaultBackground(centerContainer);
    itemTreeScroller.setBackground(Color.white);
    if (ArtOfIllusion.APP_ICON != null)
      setIcon(ArtOfIllusion.APP_ICON);
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    setBounds(screenBounds);
    tools.requestFocus();
  }

  /** Load all the preferences into memory. */

  protected void loadPreferences()
  {
    boolean lastShowAxes = preferences.getBoolean("showAxes", false);
    numViewsShown = preferences.getInt("numViews", 4);
    byte lastRenderMode[] = preferences.getByteArray("displayMode", new byte[] {ViewerCanvas.RENDER_SMOOTH, ViewerCanvas.RENDER_SMOOTH, ViewerCanvas.RENDER_SMOOTH, ViewerCanvas.RENDER_SMOOTH});
    for (int i = 0; i < theView.length; i++)
    {
      theView[i].setShowAxes(lastShowAxes);
      theView[i].setRenderMode((int) lastRenderMode[i]);
    }
  }

  /** Save user settings that should be persistent between sessions. */

  protected void savePreferences()
  {
    preferences.putBoolean("showAxes", theView[currentView].getShowAxes());
    preferences.putInt("numViews", numViewsShown);
    preferences.putByteArray("displayMode", new byte[] {(byte) theView[0].getRenderMode(), (byte) theView[1].getRenderMode(), (byte) theView[2].getRenderMode(), (byte) theView[3].getRenderMode()});
  }

  private void handleKeyEvent(KeyPressedEvent e)
  {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    if (manager.getFocusedWindow() != getComponent() || manager.getFocusOwner() instanceof JTextComponent)
      return;
    tools.getSelectedTool().keyPressed(e, theView[currentView]);
    if (!e.isConsumed())
      KeystrokeManager.executeKeystrokes(e, this);
  }

  /** Create the TreeList containing all the objects in the scene. */

  private void createItemList()
  {
    itemTree = new TreeList(this);
    itemTree.setPreferredSize(new Dimension(130, 300));
    itemTree.addEventLink(TreeList.ElementMovedEvent.class, theScore, "rebuildList");
    itemTree.addEventLink(TreeList.ElementDoubleClickedEvent.class, this, "editObjectCommand");
    itemTree.setUpdateEnabled(false);
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.getParent() == null)
          itemTree.addElement(new ObjectTreeElement(info, itemTree));
      }
    itemTree.setUpdateEnabled(true);
    itemTreeScroller = new BScrollPane(itemTree) {
      public Dimension getMinimumSize()
      {
        return new Dimension(0, 0);
      }
    };
    itemTreeScroller.setForceWidth(true);
    itemTreeScroller.setForceHeight(true);
    itemTreeScroller.getVerticalScrollBar().setUnitIncrement(10);
    itemTree.addEventLink(SelectionChangedEvent.class, this, "treeSelectionChanged");
    new AutoScroller(itemTreeScroller, 0, 10);
  }

  /** Rebuild the TreeList of objects, attempting as much as possible to preserve its
      current state. */

  public void rebuildItemList()
  {
    boolean expanded[] = new boolean [theScene.getNumObjects()], selected[] = new boolean [theScene.getNumObjects()];

    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        TreeElement el = itemTree.findElement(info);
        if (el == null)
          continue;
        expanded[i] = el.isExpanded();
        selected[i] = el.isSelected();
      }
    itemTree.setUpdateEnabled(false);
    itemTree.removeAllElements();
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.getParent() == null)
          itemTree.addElement(new ObjectTreeElement(info, itemTree));
      }
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        TreeElement el = itemTree.findElement(info);
        if (el == null)
          continue;
        el.setExpanded(expanded[i]);
        el.setSelected(selected[i]);
      }
    itemTree.setUpdateEnabled(true);
    theScore.rebuildList();
  }

  /** This is called whenever the user moves a DockableWidget.  It saves the current configuration
      to the preferences. */

  private void dockableWidgetMoved()
  {
    StringBuffer config = new StringBuffer();
    for (int i = 0; i < dock.length; i++)
    {
      for (int j = 0; j < dock[i].getTabCount(); j++)
      {
        for (int k = 0; k < dock[i].getTabChildCount(j); k++)
        {
          DockableWidget w = dock[i].getChild(j, k);
          config.append(w.getContent().getClass().getName());
          config.append('\t');
          config.append(w.getLabel());
          config.append('\n');
        }
        config.append('\n');
      }
      config.append("-\n");
    }
    Preferences prefs = Preferences.userNodeForPackage(getClass()).node("LayoutWindow");
    prefs.put("dockingConfiguration", config.toString());
  }

  /** This is called when the window is first created.  It attempts to arrange the DockableWidgets
      however they were last arranged by the user. */

  void arrangeDockableWidgets()
  {
    // Look up how they were last arranged.

    Preferences prefs = Preferences.userNodeForPackage(getClass()).node("LayoutWindow");
    String config = prefs.get("dockingConfiguration", null);
    if (config == null)
      return;

    // Make a table of all DockableWidgets.

    HashMap<String, DockableWidget> widgets = new HashMap<String, DockableWidget>();
    for (int i = 0; i < dock.length; i++)
    {
      for (Widget next : dock[i].getChildren())
      {
        if (next instanceof DockableWidget)
        {
          DockableWidget w = (DockableWidget) next;
          widgets.put(w.getContent().getClass().getName()+'\t'+w.getLabel(), w);
        }
      }
    }

    // Rearrange them.

    String lines[] = config.split("\n");
    int container = 0, tab = 0, index = 0;
    for (int i = 0; i < lines.length; i++)
    {
      if (lines[i].length() == 0)
      {
        tab++;
        index = 0;
      }
      else if ("-".equals(lines[i]))
      {
        container++;
        tab = 0;
        index = 0;
      }
      else
      {
        DockableWidget w = widgets.get(lines[i]);
        if (w != null)
        {
          dock[container].addDockableWidget(w, tab, index++);
          widgets.remove(lines[i]);
        }
      }
    }
    setScoreVisible(false);
  }

  private void createFileMenu()
  {
    BMenuItem item;
    BMenu importMenu, exportMenu;
    List<Translator> trans = PluginRegistry.getPlugins(Translator.class);

    fileMenu = Translate.menu("file");
    menubar.add(fileMenu);
    importMenu = Translate.menu("import");
    exportMenu = Translate.menu("export");
    fileMenuItem = new BMenuItem [1];
    fileMenu.add(Translate.menuItem("new", this, "actionPerformed"));
    fileMenu.add(Translate.menuItem("open", this, "actionPerformed"));
    fileMenu.add(recentFilesMenu = Translate.menu("openRecent"));
    RecentFiles.createMenu(recentFilesMenu);
    fileMenu.add(Translate.menuItem("close", this, "actionPerformed"));
    fileMenu.addSeparator();
    Collections.sort(trans, new Comparator<Translator>() {
      public int compare(Translator o1, Translator o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });
    for (int i = 0; i < trans.size(); i++)
      {
        if (trans.get(i).canImport())
          {
            importMenu.add(item = new BMenuItem(trans.get(i).getName()));
            item.setActionCommand("import");
            item.addEventLink(CommandEvent.class, this, "actionPerformed");
          }
        if (trans.get(i).canExport())
          {
            exportMenu.add(item = new BMenuItem(trans.get(i).getName()));
            item.setActionCommand("export");
            item.addEventLink(CommandEvent.class, this, "actionPerformed");
          }
      }
    if (importMenu.getChildCount() > 0)
      fileMenu.add(importMenu);
    if (exportMenu.getChildCount() > 0)
      fileMenu.add(exportMenu);
    fileMenu.add(Translate.menuItem("linkExternal", this, "linkExternalCommand"));
    fileMenu.addSeparator();
    fileMenu.add(fileMenuItem[0] = Translate.menuItem("save", this, "saveCommand"));
    fileMenu.add(Translate.menuItem("saveas", this, "saveAsCommand"));
    fileMenu.addSeparator();
    fileMenu.add(Translate.menuItem("quit", this, "actionPerformed"));
  }

  private void createEditMenu()
  {
    editMenu = Translate.menu("edit");
    menubar.add(editMenu);
    editMenuItem = new BMenuItem [9];
    editMenu.add(editMenuItem[0] = Translate.menuItem("undo", this, "undoCommand"));
    editMenu.add(editMenuItem[1] = Translate.menuItem("redo", this, "redoCommand"));
    editMenu.addSeparator();
    editMenu.add(editMenuItem[2] = Translate.menuItem("cut", this, "cutCommand"));
    editMenu.add(editMenuItem[3] = Translate.menuItem("copy", this, "copyCommand"));
    editMenu.add(editMenuItem[4] = Translate.menuItem("paste", this, "pasteCommand"));
    editMenu.add(editMenuItem[5] = Translate.menuItem("clear", this, "clearCommand"));
    editMenu.add(editMenuItem[6] = Translate.menuItem("selectChildren", this, "actionPerformed"));
    editMenu.add(Translate.menuItem("selectAll", this, "selectAllCommand"));
    editMenu.addSeparator();
    editMenu.add(editMenuItem[7] = Translate.menuItem("duplicate", this, "duplicateCommand"));
    editMenu.add(editMenuItem[8] = Translate.menuItem("sever", this, "severCommand"));
    editMenu.addSeparator();
    editMenu.add(Translate.menuItem("preferences", this, "actionPerformed"));
  }

  private void createObjectMenu()
  {
    objectMenu = Translate.menu("object");
    menubar.add(objectMenu);
    objectMenuItem = new BMenuItem [13];
    objectMenu.add(objectMenuItem[0] = Translate.menuItem("editObject", this, "editObjectCommand"));
    objectMenu.add(objectMenuItem[1] = Translate.menuItem("objectLayout", this, "objectLayoutCommand"));
    objectMenu.add(objectMenuItem[2] = Translate.menuItem("transformObject", this, "transformObjectCommand"));
    objectMenu.add(objectMenuItem[3] = Translate.menuItem("alignObjects", this, "alignObjectsCommand"));
    objectMenu.add(objectMenuItem[4] = Translate.menuItem("setTexture", this, "setTextureCommand"));
    objectMenu.add(objectMenuItem[5] = Translate.menuItem("setMaterial", this, "setMaterialCommand"));
    objectMenu.add(objectMenuItem[6] = Translate.menuItem("renameObject", this, "renameObjectCommand"));
    objectMenu.add(objectMenuItem[7] = Translate.menuItem("convertToTriangle", this, "convertToTriangleCommand"));
    objectMenu.add(objectMenuItem[8] = Translate.menuItem("convertToActor", this, "convertToActorCommand"));
    objectMenu.addSeparator();
    objectMenu.add(objectMenuItem[9] = Translate.menuItem("hideSelection", this, "actionPerformed"));
    objectMenu.add(objectMenuItem[10] = Translate.menuItem("showSelection", this, "actionPerformed"));
    objectMenu.add(Translate.menuItem("showAll", this, "actionPerformed"));
    objectMenu.addSeparator();
    objectMenu.add(objectMenuItem[11] = Translate.menuItem("lockSelection", this, "actionPerformed"));
    objectMenu.add(objectMenuItem[12] = Translate.menuItem("unlockSelection", this, "actionPerformed"));
    objectMenu.add(Translate.menuItem("unlockAll", this, "actionPerformed"));
    objectMenu.addSeparator();
    objectMenu.add(createMenu = Translate.menu("createPrimitive"));
    createMenu.add(Translate.menuItem("cube", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("sphere", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("cylinder", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("cone", this, "createObjectCommand"));
    createMenu.addSeparator();
    createMenu.add(Translate.menuItem("pointLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("directionalLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("spotLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("proceduralPointLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("proceduralDirectionalLight", this, "createObjectCommand"));
    createMenu.addSeparator();
    createMenu.add(Translate.menuItem("camera", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("referenceImage", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("null", this, "createObjectCommand"));
  }

  private void createToolsMenu()
  {
    modellingTools = PluginRegistry.getPlugins(ModellingTool.class);
    Collections.sort(modellingTools, new Comparator<ModellingTool>() {
      public int compare(ModellingTool o1, ModellingTool o2)
      {
        return (o1.getName().compareTo(o2.getName()));
      }
    });
    toolsMenu = Translate.menu("tools");
    menubar.add(toolsMenu);
    toolsMenuItem = new BMenuItem [modellingTools.size()];
    for (int i = 0; i < modellingTools.size(); i++)
      {
        BMenuItem item = new BMenuItem(modellingTools.get(i).getName());
        toolsMenu.add(item);
        item.setActionCommand("modellingTool");
        item.addEventLink(CommandEvent.class, this, "modellingToolCommand");
        toolsMenuItem[i] = item;
      }
    toolsMenu.addSeparator();
    toolsMenu.add(Translate.menuItem("createScriptObject", this, "createScriptObjectCommand"));
    toolsMenu.add(Translate.menuItem("editScript", this, "actionPerformed"));
    toolsMenu.add(scriptMenu = Translate.menu("scripts"));
    rebuildScriptsMenu();
  }

  /** Rebuild the list of tool scripts in the Tools menu.  This should be called whenever a
      script has been added to or deleted from the Scripts/Tools directory on disk. */

  public void rebuildScriptsMenu()
  {
    scriptMenu.removeAll();
    addScriptsToMenu(scriptMenu, new File(ArtOfIllusion.TOOL_SCRIPT_DIRECTORY));
  }

  private void addScriptsToMenu(BMenu menu, File dir)
  {
    String files[] = dir.list();
    if (files == null)
      return;
    Arrays.sort(files, Collator.getInstance(Translate.getLocale()));
    for (String file : files)
    {
      File f = new File(dir, file);
      if (f.isDirectory())
      {
        BMenu m = new BMenu(file);
        menu.add(m);
        addScriptsToMenu(m, f);
      }
      else if (file.endsWith(".bsh") && file.length() > 4)
      {
        BMenuItem item = new BMenuItem(file.substring(0, file.length()-4));
        item.setActionCommand(f.getAbsolutePath());
        item.addEventLink(CommandEvent.class, this, "executeScriptCommand");
        menu.add(item);
      }
    }
  }

  private void createAnimationMenu()
  {
    animationMenu = Translate.menu("animation");
    menubar.add(animationMenu);
    animationMenuItem = new BMenuItem [13];
    animationMenu.add(addTrackMenu = Translate.menu("addTrack"));
    addTrackMenu.add(positionTrackMenu = Translate.menu("positionTrack"));
    positionTrackMenu.add(Translate.menuItem("xyzOneTrack", this, "actionPerformed"));
    positionTrackMenu.add(Translate.menuItem("xyzThreeTracks", this, "actionPerformed"));
    positionTrackMenu.add(Translate.menuItem("proceduralTrack", this, "actionPerformed"));
    addTrackMenu.add(rotationTrackMenu = Translate.menu("rotationTrack"));
    rotationTrackMenu.add(Translate.menuItem("xyzOneTrack", this, "actionPerformed"));
    rotationTrackMenu.add(Translate.menuItem("xyzThreeTracks", this, "actionPerformed"));
    rotationTrackMenu.add(Translate.menuItem("quaternionTrack", this, "actionPerformed"));
    rotationTrackMenu.add(Translate.menuItem("proceduralTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("poseTrack", this, "actionPerformed"));
    addTrackMenu.add(distortionMenu = Translate.menu("distortionTrack"));
    distortionMenu.add(Translate.menuItem("bendDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("customDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("scaleDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("shatterDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("twistDistortion", this, "actionPerformed"));
    distortionMenu.addSeparator();
    distortionMenu.add(Translate.menuItem("IKTrack", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("skeletonShapeTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("constraintTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("visibilityTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("textureTrack", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[0] = Translate.menuItem("editTrack", theScore, "editSelectedTrack"));
    animationMenu.add(animationMenuItem[1] = Translate.menuItem("duplicateTracks", theScore, "duplicateSelectedTracks"));
    animationMenu.add(animationMenuItem[2] = Translate.menuItem("deleteTracks", theScore, "deleteSelectedTracks"));
    animationMenu.add(animationMenuItem[3] = Translate.menuItem("selectAllTracks", theScore, "selectAllTracks"));
    animationMenu.add(animationMenuItem[4] = Translate.menuItem("enableTracks", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[5] = Translate.menuItem("disableTracks", this, "actionPerformed"));
    animationMenu.addSeparator();
    animationMenu.add(animationMenuItem[6] = Translate.menuItem("keyframe", theScore, "keyframeSelectedTracks"));
    animationMenu.add(animationMenuItem[7] = Translate.menuItem("keyframeModified", theScore, "keyframeModifiedTracks"));
    animationMenu.add(animationMenuItem[8] = Translate.menuItem("editKeyframe", theScore, "editSelectedKeyframe"));
    animationMenu.add(animationMenuItem[9] = Translate.menuItem("deleteSelectedKeyframes", theScore, "deleteSelectedKeyframes"));
    animationMenu.add(editKeyframeMenu = Translate.menu("bulkEditKeyframes"));
    editKeyframeMenu.add(Translate.menuItem("moveKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("copyKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("rescaleKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("loopKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("deleteKeyframes", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[10] = Translate.menuItem("pathFromCurve", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[11] = Translate.menuItem("bindToParent", this, "bindToParentCommand"));
    animationMenu.addSeparator();
    animationMenu.add(animationMenuItem[12] = Translate.menuItem("showScore", this, "actionPerformed"));
    animationMenu.add(Translate.menuItem("previewAnimation", this, "actionPerformed"));
    animationMenu.addSeparator();
    animationMenu.add(Translate.menuItem("forwardFrame", this, "actionPerformed"));
    animationMenu.add(Translate.menuItem("backFrame", this, "actionPerformed"));
    animationMenu.add(Translate.menuItem("jumpToTime", this, "jumpToTimeCommand"));
  }

  private void createSceneMenu()
  {
    BMenu displayMenu;

    sceneMenu = Translate.menu("scene");
    menubar.add(sceneMenu);
    sceneMenuItem = new BMenuItem [5];
    sceneMenu.add(Translate.menuItem("renderScene", this, "renderCommand"));
    sceneMenu.add(Translate.menuItem("renderImmediately", this, "actionPerformed"));
    sceneMenu.addSeparator();
    sceneMenu.add(displayMenu = Translate.menu("displayMode"));
    displayItem = new BCheckBoxMenuItem [5];
    displayMenu.add(displayItem[0] = Translate.checkboxMenuItem("wireframeDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_WIREFRAME));
    displayMenu.add(displayItem[1] = Translate.checkboxMenuItem("shadedDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_FLAT));
    displayMenu.add(displayItem[2] = Translate.checkboxMenuItem("smoothDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_SMOOTH));
    displayMenu.add(displayItem[3] = Translate.checkboxMenuItem("texturedDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_TEXTURED));
    displayMenu.add(displayItem[4] = Translate.checkboxMenuItem("transparentDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_TEXTURED));
    sceneMenu.add(sceneMenuItem[0] = Translate.menuItem("fourViews", this, "toggleViewsCommand"));
    sceneMenu.add(sceneMenuItem[1] = Translate.menuItem("hideObjectList", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("grid", this, "setGridCommand"));
    sceneMenu.add(sceneMenuItem[2] = Translate.menuItem("showCoordinateAxes", this, "actionPerformed"));
    sceneMenu.add(sceneMenuItem[3] = Translate.menuItem("showTemplate", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("setTemplate", this, "setTemplateCommand"));
    sceneMenu.addSeparator();
    sceneMenu.add(sceneMenuItem[4] = Translate.menuItem("frameSelection", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("frameScene", this, "actionPerformed"));
    sceneMenu.addSeparator();
    sceneMenu.add(Translate.menuItem("textures", this, "texturesCommand"));
    sceneMenu.add(Translate.menuItem("materials", this, "materialsCommand"));
    sceneMenu.add(Translate.menuItem("images", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("environment", this, "environmentCommand"));
  }

  /** Create the popup menu. */

  private void createPopupMenu()
  {
    popupMenu = new BPopupMenu();
    popupMenuItem = new BMenuItem [15];
    popupMenu.add(popupMenuItem[0] = Translate.menuItem("editObject", this, "editObjectCommand", null));
    popupMenu.add(popupMenuItem[1] = Translate.menuItem("objectLayout", this, "objectLayoutCommand", null));
    popupMenu.add(popupMenuItem[2] = Translate.menuItem("setTexture", this, "setTextureCommand", null));
    popupMenu.add(popupMenuItem[3] = Translate.menuItem("setMaterial", this, "setMaterialCommand", null));
    popupMenu.add(popupMenuItem[4] = Translate.menuItem("renameObject", this, "renameObjectCommand", null));
    popupMenu.add(popupMenuItem[5] = Translate.menuItem("convertToTriangle", this, "convertToTriangleCommand", null));
    popupMenu.add(popupMenuItem[6] = Translate.menuItem("selectChildren", this, "actionPerformed", null));
    popupMenu.addSeparator();
    popupMenu.add(popupMenuItem[7] = Translate.menuItem("hideSelection", this, "actionPerformed", null));
    popupMenu.add(popupMenuItem[8] = Translate.menuItem("showSelection", this, "actionPerformed", null));
    popupMenu.addSeparator();
    popupMenu.add(popupMenuItem[9] = Translate.menuItem("lockSelection", this, "actionPerformed"));
    popupMenu.add(popupMenuItem[10] = Translate.menuItem("unlockSelection", this, "actionPerformed"));
    popupMenu.addSeparator();
    popupMenu.add(popupMenuItem[11] = Translate.menuItem("cut", this, "cutCommand", null));
    popupMenu.add(popupMenuItem[12] = Translate.menuItem("copy", this, "copyCommand", null));
    popupMenu.add(popupMenuItem[13] = Translate.menuItem("paste", this, "pasteCommand", null));
    popupMenu.add(popupMenuItem[14] = Translate.menuItem("clear", this, "clearCommand", null));
  }

  /** Display the popup menu. */

  public void showPopupMenu(Widget w, int x, int y)
  {
    Object sel[] = itemTree.getSelectedObjects();
    boolean canConvert, canSetMaterial, canSetTexture, canHide, canShow, canLock, canUnlock, hasChildren;
    ObjectInfo info;
    Object3D obj;

    canConvert = canSetMaterial = canSetTexture = (sel.length > 0);
    canHide = canShow = canLock = canUnlock = hasChildren = false;
    for (int i = 0; i < sel.length; i++)
      {
        info = (ObjectInfo) sel[i];
        obj = info.getObject();
        if (obj.canConvertToTriangleMesh() == Object3D.CANT_CONVERT)
          canConvert = false;
        if (!obj.canSetTexture())
          canSetTexture = false;
        if (!obj.canSetMaterial())
          canSetMaterial = false;
        if (info.getChildren().length > 0)
          hasChildren = true;
        if (info.isVisible())
          canHide = true;
        else
          canShow = true;
        if (info.isLocked())
          canUnlock = true;
        else
          canLock = true;
      }
    if (sel.length == 0)
      {
        for (int i = 0; i < popupMenuItem.length; i++)
          popupMenuItem[i].setEnabled(false);
      }
    else
      {
        obj = ((ObjectInfo) sel[0]).getObject();
        popupMenuItem[0].setEnabled(sel.length == 1 && obj.isEditable()); // Edit Object
        popupMenuItem[1].setEnabled(true); // Object Layout
        popupMenuItem[2].setEnabled(canSetTexture); // Set Texture
        popupMenuItem[3].setEnabled(canSetMaterial); // Set Material
        popupMenuItem[4].setEnabled(sel.length == 1); // Rename Object
        popupMenuItem[5].setEnabled(canConvert); // Convert to Triangle Mesh
        popupMenuItem[6].setEnabled(sel.length == 1 && hasChildren); // Select Children
        popupMenuItem[7].setEnabled(canHide); // Hide Selection
        popupMenuItem[8].setEnabled(canShow); // Show Selection
        popupMenuItem[9].setEnabled(canLock); // Lock Selection
        popupMenuItem[10].setEnabled(canUnlock); // Unlock Selection
        popupMenuItem[11].setEnabled(sel.length > 0); // Cut
        popupMenuItem[12].setEnabled(sel.length > 0); // Copy
        popupMenuItem[14].setEnabled(sel.length > 0); // Clear
      }
    popupMenuItem[13].setEnabled(ArtOfIllusion.getClipboardSize() > 0); // Paste
    popupMenu.show(w, x, y);
  }

  /** Get the File menu. */

  public BMenu getFileMenu()
  {
    return fileMenu;
  }

  /** Get the Edit menu. */

  public BMenu getEditMenu()
  {
    return editMenu;
  }

  /** Get the Scene menu. */

  public BMenu getSceneMenu()
  {
    return sceneMenu;
  }

  /** Get the Object menu. */

  public BMenu getObjectMenu()
  {
    return objectMenu;
  }

  /** Get the Animation menu. */

  public BMenu getAnimationMenu()
  {
    return animationMenu;
  }

  /** Get the Tools menu. */

  public BMenu getToolsMenu()
  {
    return toolsMenu;
  }

  /** Get the popup menu. */

  public BPopupMenu getPopupMenu()
  {
    return popupMenu;
  }

  /** Get the DockingContainer which holds DockableWidgets on one side of the window. */

  public DockingContainer getDockingContainer(BTabbedPane.TabPosition position)
  {
    for (int i = 0; i < dock.length; i++)
      if (dock[i].getTabPosition() == position)
        return dock[i];
    return null; // should be impossible
  }

  /** Set the wait cursor on everything in this window. */

  public void setWaitCursor()
  {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  /** Remove the wait cursor from everything in this window. */

  public void clearWaitCursor()
  {
    setCursor(Cursor.getDefaultCursor());
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(100, 100);
  }

  /* EditingWindow methods. */

  /** This method is called to close the window.  If the Scene has been modified, it first
      gives the user a chance to save the Scene, or to cancel.  If the user cancels it, the
      method returns false.  Otherwise, it closes the window and returns true. */

  public boolean confirmClose()
  {
    if (modified)
    {
      String name = theScene.getName();
      if (name == null)
        name = "Untitled";
      BStandardDialog dlg = new BStandardDialog("", Translate.text("checkSaveChanges", name), BStandardDialog.QUESTION);
      String options[] = new String [] {Translate.text("button.save"), Translate.text("button.dontSave"), Translate.text("button.cancel")};
      int choice = dlg.showOptionDialog(this, options, options[0]);
      if (choice == 0)
      {
        saveCommand();
        if (modified)
          return false;
      }
      if (choice == 2)
        return false;
    }
    dispose();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(keyEventHandler);
    return true;
  }

  /** Set the selected EditingTool for this window. */

  public void setTool(EditingTool tool)
  {
    for (int i = 0; i < theView.length; i++)
      theView[i].setTool(tool);
  }

  /** Set the help text displayed at the bottom of the window. */

  public void setHelpText(String text)
  {
    helpText.setText(text);
  }

  /** Get the Frame corresponding to this window.  (Because LayoutWindow is a Frame,
      it simply returns itself.) */

  public BFrame getFrame()
  {
    return this;
  }

  /** Update the images displayed in all of the viewport. */

  public void updateImage()
  {
    if (numViewsShown == 1)
    {
      theView[currentView].copyOrientationFromCamera();
      theView[currentView].repaint();
    }
    else
      for (int i = 0; i < numViewsShown; i++)
      {
        theView[i].copyOrientationFromCamera();
        theView[i].repaint();
      }
  }

  /** Update the state of all menu items. */

  public void updateMenus()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int numSelObjects = sel.length;
    Track selTrack[] = theScore.getSelectedTracks();
    int numSelTracks = selTrack.length;
    int numSelKeyframes = theScore.getSelectedKeyframes().length;
    ViewerCanvas view = theView[currentView];
    boolean canConvert, canSetMaterial, canSetTexture;
    boolean curve, noncurve, enable, disable, hasChildren, hasParent;
    ObjectInfo info;
    Object3D obj;
    int i;

    canConvert = canSetMaterial = canSetTexture = (numSelObjects > 0);
    curve = noncurve = enable = disable = hasChildren = hasParent = false;
    for (i = 0; i < numSelObjects; i++)
    {
      info = (ObjectInfo) sel[i];
      obj = info.getObject();
      if (obj instanceof Curve)
        curve = true;
      else
        noncurve = true;
      if (obj.canConvertToTriangleMesh() == Object3D.CANT_CONVERT)
        canConvert = false;
      if (!obj.canSetTexture())
        canSetTexture = false;
      if (!obj.canSetMaterial())
        canSetMaterial = false;
      if (info.getChildren().length > 0)
        hasChildren = true;
      if (info.getParent() != null)
        hasParent = true;
    }
    for (i = 0; i < numSelTracks; i++)
    {
      if (selTrack[i].isEnabled())
        disable = true;
      else
        enable = true;
    }

    fileMenuItem[0].setEnabled(modified);
    editMenuItem[0].setEnabled(undoStack.canUndo()); // Undo
    editMenuItem[1].setEnabled(undoStack.canRedo()); // Redo
    editMenuItem[2].setEnabled(numSelObjects > 0); // Cut
    editMenuItem[3].setEnabled(numSelObjects > 0); // Copy
    editMenuItem[4].setEnabled(ArtOfIllusion.getClipboardSize() > 0); // Paste
    editMenuItem[5].setEnabled(numSelObjects > 0); // Clear
    editMenuItem[6].setEnabled(hasChildren); // Select Children
    editMenuItem[7].setEnabled(numSelObjects > 0); // Make Live Duplicates
    editMenuItem[8].setEnabled(numSelObjects > 0); // Sever Duplicates
    if (numSelObjects == 0)
    {
      for (i = 0; i < objectMenuItem.length; i++)
        objectMenuItem[i].setEnabled(false);
    }
    else
    {
      obj = ((ObjectInfo) sel[0]).getObject();
      objectMenuItem[0].setEnabled(numSelObjects == 1 && obj.isEditable()); // Edit Object
      objectMenuItem[1].setEnabled(true); // Object Layout
      objectMenuItem[2].setEnabled(true); // Transform Object
      objectMenuItem[3].setEnabled(numSelObjects > 0); // Align Objects
      objectMenuItem[4].setEnabled(canSetTexture); // Set Texture
      objectMenuItem[5].setEnabled(canSetMaterial); // Set Material
      objectMenuItem[6].setEnabled(sel.length == 1); // Rename Object
      objectMenuItem[7].setEnabled(canConvert && sel.length == 1); // Convert to Triangle Mesh
      objectMenuItem[8].setEnabled(sel.length == 1 && ((ObjectInfo) sel[0]).getObject().canConvertToActor()); // Convert to Actor
      objectMenuItem[9].setEnabled(true); // Hide Selection
      objectMenuItem[10].setEnabled(true); // Show Selection
      objectMenuItem[11].setEnabled(true); // Lock Selection
      objectMenuItem[12].setEnabled(true); // Unlock Selection
    }
    animationMenuItem[0].setEnabled(numSelTracks == 1); // Edit Track
    animationMenuItem[1].setEnabled(numSelTracks > 0); // Duplicate Tracks
    animationMenuItem[2].setEnabled(numSelTracks > 0); // Delete Tracks
    animationMenuItem[3].setEnabled(numSelObjects > 0); // Select All Tracks
    animationMenuItem[4].setEnabled(enable); // Enable Tracks
    animationMenuItem[5].setEnabled(disable); // Disable Tracks
    animationMenuItem[6].setEnabled(numSelTracks > 0); // Keyframe Selected Tracks
    animationMenuItem[7].setEnabled(numSelObjects > 0); // Keyframe Modified Tracks
    animationMenuItem[8].setEnabled(numSelKeyframes == 1); // Edit Keyframe
    animationMenuItem[9].setEnabled(numSelKeyframes > 0); // Delete Selected Keyframes
    animationMenuItem[10].setEnabled(curve && noncurve); // Set Path From Curve
    animationMenuItem[11].setEnabled(hasParent); // Bind to Parent Skeleton
    animationMenuItem[12].setText(Translate.text(theScore.getBounds().height == 0 || theScore.getBounds().width == 0 ? "menu.showScore" : "menu.hideScore"));
    addTrackMenu.setEnabled(numSelObjects > 0);
    distortionMenu.setEnabled(sel.length > 0);
    sceneMenuItem[1].setText(Translate.text(itemTreeScroller.getBounds().width == 0 || itemTreeScroller.getBounds().height == 0 ? "menu.showObjectList" : "menu.hideObjectList"));
    sceneMenuItem[2].setText(Translate.text(view.getShowAxes() ? "menu.hideCoordinateAxes" : "menu.showCoordinateAxes"));
    sceneMenuItem[3].setEnabled(view.getTemplateImage() != null); // Show template
    sceneMenuItem[3].setText(Translate.text(view.getTemplateShown() ? "menu.hideTemplate" : "menu.showTemplate"));
    sceneMenuItem[4].setEnabled(sel.length > 0); // Frame Selection With Camera
    displayItem[0].setState(view.getRenderMode() == ViewerCanvas.RENDER_WIREFRAME);
    displayItem[1].setState(view.getRenderMode() == ViewerCanvas.RENDER_FLAT);
    displayItem[2].setState(view.getRenderMode() == ViewerCanvas.RENDER_SMOOTH);
    displayItem[3].setState(view.getRenderMode() == ViewerCanvas.RENDER_TEXTURED);
  }

  /** Set the UndoRecord which will be executed if the user chooses Undo from the Edit menu. */

  public void setUndoRecord(UndoRecord command)
  {
    undoStack.addRecord(command);
    setModified();
    updateMenus();
  }

  /** Set whether the scene has been modified since it was last saved. */

  public void setModified()
  {
    modified = true;
    dispatchSceneChangedEvent();
  }

  /** Determine whether the scene has been modified since it was last saved. */

  public boolean isModified()
  {
    return modified;
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

  /** Add a new object to the scene.  If undo is not null, 
      appropriate commands will be added to it to undo this operation. */

  public void addObject(Object3D obj, CoordinateSystem coords, String name, UndoRecord undo)
  {
    addObject(new ObjectInfo(obj, coords, name), undo);
  }

  /** Add a new object to the scene.  If undo is not null, 
      appropriate commands will be added to it to undo this operation. */

  public void addObject(ObjectInfo info, UndoRecord undo)
  {
    theScene.addObject(info, undo);
    itemTree.addElement(new ObjectTreeElement(info, itemTree));
    for (int i = 0; i < theView.length ; i++)
      theView[i].rebuildCameraList();
    theScore.rebuildList();
  }

  /** Add a new object to the scene.  If undo is not null, 
      appropriate commands will be added to it to undo this operation. */

  public void addObject(ObjectInfo info, int index, UndoRecord undo)
  {
    theScene.addObject(info, index, undo);
    itemTree.addElement(new ObjectTreeElement(info, itemTree), index);
    for (int i = 0; i < theView.length ; i++)
      theView[i].rebuildCameraList();
    theScore.rebuildList();
  }

  /** Remove an object from the scene.  If undo is not null, 
      appropriate commands will be added to it to undo this operation. */

  public void removeObject(int which, UndoRecord undo)
  {
    ObjectInfo info = theScene.getObject(which);
    ObjectInfo parent = info.getParent();
    int childIndex = -1;
    if (parent != null)
      for (int i = 0; i < parent.getChildren().length; i++)
        if (parent.getChildren()[i] == info)
          childIndex = i;
    itemTree.removeObject(info);
    if (childIndex > -1 && info.getParent() == null)
      undo.addCommandAtBeginning(UndoRecord.ADD_TO_GROUP, new Object [] {parent, info, new Integer(childIndex)});
    theScene.removeObject(which, undo);
    for (int i = 0; i < theView.length ; i++)
      theView[i].rebuildCameraList();
    theScore.rebuildList();
  }

  /** Set the name of an object in the scene. */

  public void setObjectName(int which, String name)
  {
    theScene.getObject(which).setName(name);
    itemTree.repaint();
    for (int i = 0; i < theView.length ; i++)
      theView[i].rebuildCameraList();
    theScore.rebuildList();
  }

  /** Set the time which is currently being displayed. */

  public void setTime(double time)
  {
    theScene.setTime(time);
    theScore.setTime(time);
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(3);
    timeFrameLabel.setText(Translate.text("timeFrameLabel", nf.format(time),
        Integer.toString((int) Math.round(time*theScene.getFramesPerSecond()))));
    theScore.repaint();
    itemTree.repaint();
    updateImage();
    dispatchSceneChangedEvent();
  }

  /** Get the Scene associated with this window. */

  public Scene getScene()
  {
    return theScene;
  }

  /** Get the ViewerCanvas which currently has focus. */

  public ViewerCanvas getView()
  {
    return theView[currentView];
  }
  
  /** Get all ViewerCanvases contained in this window. */

  public ViewerCanvas[] getAllViews()
  {
    return (ViewerCanvas[]) theView.clone();
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
        displayItem[0].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_WIREFRAME);
        displayItem[1].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_FLAT);
        displayItem[2].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_SMOOTH);
        displayItem[3].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_TEXTURED);
        currentView = i;
        updateImage();
        updateMenus();
      }
  }

  /** Get the Score for this window. */

  public Score getScore()
  {
    return theScore;
  }

  /** Get the ToolPalette for this window. */

  public ToolPalette getToolPalette()
  {
    return tools;
  }

  /** Set whether a DockableWidget contained in this window is visible. */

  private void setDockableWidgetVisible(DockableWidget widget, boolean visible)
  {
    DockingContainer parent = (DockingContainer) widget.getParent();
    BTabbedPane.TabPosition pos = parent.getTabPosition();
    BSplitPane split = parent.getSplitPane();
    if (visible)
      split.resetToPreferredSizes();
    else
      split.setDividerLocation(pos == BTabbedPane.TOP || pos == BTabbedPane.LEFT ? 0.0 : 1.0);
    updateMenus();
  }

  /** Set whether the object list should be displayed. */

  public void setObjectListVisible(boolean visible)
  {
    setDockableWidgetVisible((DockableWidget) itemTreeScroller.getParent(), visible);
  }

  /** Set whether the score should be displayed. */

  public void setScoreVisible(boolean visible)
  {
    setDockableWidgetVisible((DockableWidget) theScore.getParent(), visible);
  }

  /** Set whether the window is split into four views. */

  public void setSplitView(boolean split)
  {
    if ((numViewsShown == 1) == split)
      toggleViewsCommand();
  }

  /** This is called when the selection in the object tree changes. */

  private void treeSelectionChanged()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int which[] = new int [sel.length];

    for (int i = 0; i < sel.length; i++)
      which[i] = theScene.indexOf((ObjectInfo) sel[i]);
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
    setSelection(which);
    updateImage();
  }

  private void displayModeCommand(CommandEvent ev)
  {
    Widget source = ev.getWidget();
    if (source == displayItem[0])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_WIREFRAME);
    else if (source == displayItem[1])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_FLAT);
    else if (source == displayItem[2])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_SMOOTH);
    else if (source == displayItem[3])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_TEXTURED);
    else if (source == displayItem[4])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_TRANSPARENT);
    for (int i = 0; i < displayItem.length; i++)
      displayItem[i].setState(source == displayItem[i]);
    savePreferences();
  }

  /** Get a list of the indices of all selected objects. */

  public int[] getSelectedIndices()
  {
    return theScene.getSelection();
  }

  /** Get a collection of all selected objects. */

  public Collection<ObjectInfo> getSelectedObjects()
  {
    ArrayList<ObjectInfo> objects = new ArrayList<ObjectInfo>();
    for (int index : theScene.getSelection())
      objects.add(theScene.getObject(index));
    return objects;
  }

  /** Determine whether an object is selected. */

  public boolean isObjectSelected(ObjectInfo info)
  {
    return info.selected;
  }

  /** Determine whether an object is selected. */

  public boolean isObjectSelected(int index)
  {
    return theScene.getObject(index).selected;
  }

  /** Get the indices of all objects which are either selected, or are children of
      selected objects. */

  public int[] getSelectionWithChildren()
  {
    return theScene.getSelectionWithChildren();
  }

  /** Set a single object in the scene to be selected. */

  public void setSelection(int which)
  {
    itemTree.setUpdateEnabled(false);
    clearSelection();
    theScene.setSelection(which);
    itemTree.setSelected(theScene.getObject(which), true);
    itemTree.setUpdateEnabled(true);
    theScore.rebuildList();
    updateMenus();
  }

  /** Set the list of objects in the scene which should be selected. */

  public void setSelection(int which[])
  {
    itemTree.setUpdateEnabled(false);
    clearSelection();
    theScene.setSelection(which);
    for (int i = 0; i < which.length; i++)
      itemTree.setSelected(theScene.getObject(which[i]), true);
    itemTree.setUpdateEnabled(true);
    theScore.rebuildList();
    updateMenus();
  }

  /** Set an object to be selected. */

  public void addToSelection(int which)
  {
    theScene.addToSelection(which);
    itemTree.setSelected(theScene.getObject(which), true);
    theScore.rebuildList();
    updateMenus();
  }

  /** Deselect all objects. */

  public void clearSelection()
  {
    theScene.clearSelection();
    itemTree.deselectAll();
    theScore.rebuildList();
    updateMenus();
  }

  /** Deselect a single object. */

  public void removeFromSelection(int which)
  {
    theScene.removeFromSelection(which);
    itemTree.setSelected(theScene.getObject(which), false);
    theScore.rebuildList();
    updateMenus();
  }

  private void actionPerformed(CommandEvent e)
  {
    String command = e.getActionCommand();
    Widget src = e.getWidget();
    Widget menu = (src instanceof MenuWidget ? src.getParent() : null);

    setWaitCursor();
    if (menu == fileMenu)
      {
        savePreferences();
        if (command.equals("new"))
          ArtOfIllusion.newWindow();
        else if (command.equals("open"))
          ArtOfIllusion.openScene(this);
        else if (command.equals("close"))
          ArtOfIllusion.closeWindow(this);
        else if (command.equals("quit"))
          ArtOfIllusion.quit();
      }
    else if (command.equals("import"))
     importCommand(((BMenuItem) e.getWidget()).getText());
    else if (command.equals("export"))
      exportCommand(((BMenuItem) e.getWidget()).getText());
    else if (menu == editMenu)
      {
        if (command.equals("selectChildren"))
          {
            setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
            setSelection(getSelectionWithChildren());
            updateImage();
          }
        else if (command.equals("preferences"))
          new PreferencesWindow(this);
      }
    else if (menu == objectMenu)
      {
        if (command.equals("hideSelection"))
          setObjectVisibility(false, true);
        else if (command.equals("showSelection"))
          setObjectVisibility(true, true);
        else if (command.equals("showAll"))
          setObjectVisibility(true, false);
        else if (command.equals("lockSelection"))
          setObjectsLocked(true, true);
        else if (command.equals("unlockSelection"))
          setObjectsLocked(false, true);
        else if (command.equals("unlockAll"))
          setObjectsLocked(false, false);
      }
    else if (menu == toolsMenu)
      {
        if (command.equals("editScript"))
          new ExecuteScriptWindow(this);
      }
    else if (menu == animationMenu || menu == theScore.getPopupMenu())
      {
        if (command.equals("showScore"))
          setScoreVisible(theScore.getBounds().height == 0 || theScore.getBounds().width == 0);
        else if (command.equals("previewAnimation"))
          new AnimationPreviewer(this);
        else if (command.equals("forwardFrame"))
          {
            double t = theScene.getTime() + 1.0/theScene.getFramesPerSecond();
            setTime(t);
          }
        else if (command.equals("backFrame"))
          {
            double t = theScene.getTime() - 1.0/theScene.getFramesPerSecond();
            setTime(t);
          }
        else if (command.equals("enableTracks"))
          theScore.setTracksEnabled(true);
        else if (command.equals("disableTracks"))
          theScore.setTracksEnabled(false);
        else if (command.equals("pathFromCurve"))
          new PathFromCurveDialog(this, itemTree.getSelectedObjects());
        else if (command.equals("bindToParent"))
          bindToParentCommand();
      }
    else if (menu == editKeyframeMenu)
      {
        if (command.equals("moveKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.MOVE);
        else if (command.equals("copyKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.COPY);
        else if (command.equals("rescaleKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.RESCALE);
        else if (command.equals("loopKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.LOOP);
        else if (command.equals("deleteKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.DELETE);
      }
    else if (menu == addTrackMenu)
      {
        if (command.equals("poseTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), PoseTrack.class, null, true);
        else if (command.equals("constraintTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), ConstraintTrack.class, null, true);
        else if (command.equals("visibilityTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), VisibilityTrack.class, null, true);
        else if (command.equals("textureTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), TextureTrack.class, null, true);
      }
    else if (menu == positionTrackMenu)
      {
        if (command.equals("xyzOneTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, null, true);
        else if (command.equals("xyzThreeTracks"))
          {
            theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, new Object [] {"Z Position", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE}, true);
            theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, new Object [] {"Y Position", Boolean.FALSE, Boolean.TRUE, Boolean.FALSE}, false);
            theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, new Object [] {"X Position", Boolean.TRUE, Boolean.FALSE, Boolean.FALSE}, false);
          }
        else if (command.equals("proceduralTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), ProceduralPositionTrack.class, null, true);
      }
    else if (menu == rotationTrackMenu)
      {
        if (command.equals("xyzOneTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Rotation", Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE}, true);
        else if (command.equals("xyzThreeTracks"))
          {
            theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Z Rotation", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE}, true);
            theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Y Rotation", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE}, false);
            theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"X Rotation", Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE}, false);
          }
        else if (command.equals("quaternionTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Rotation", Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE}, true);
        else if (command.equals("proceduralTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), ProceduralRotationTrack.class, null, true);
      }
    else if (menu == distortionMenu)
      {
        if (command.equals("bendDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), BendTrack.class, null, true);
        else if (command.equals("customDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), CustomDistortionTrack.class, null, true);
        else if (command.equals("scaleDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), ScaleTrack.class, null, true);
        else if (command.equals("shatterDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), ShatterTrack.class, null, true);
        else if (command.equals("twistDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), TwistTrack.class, null, true);
        else if (command.equals("IKTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), IKTrack.class, null, true);
        else if (command.equals("skeletonShapeTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), SkeletonShapeTrack.class, null, true);
      }
    else if (menu == sceneMenu)
      {
        if (command.equals("renderScene"))
          new RenderSetupDialog(this, theScene);
        else if (command.equals("renderImmediately"))
          RenderSetupDialog.renderImmediately(this, theScene);
        else if (command.equals("hideObjectList"))
          setObjectListVisible(itemTreeScroller.getBounds().width == 0 || itemTreeScroller.getBounds().height == 0);
        else if (command.equals("showCoordinateAxes"))
          {
            boolean wasShown = theView[currentView].getShowAxes();
            for (int i = 0; i < theView.length; i++)
              theView[i].setShowAxes(!wasShown);
            savePreferences();
            updateImage();
            updateMenus();
          }
        else if (command.equals("showTemplate"))
          {
            boolean wasShown = theView[currentView].getTemplateShown();
            theView[currentView].setShowTemplate(!wasShown);
            updateImage();
            updateMenus();
          }
        else if (command.equals("frameSelection"))
          frameWithCameraCommand(true);
        else if (command.equals("frameScene"))
          frameWithCameraCommand(false);
        else if (command.equals("images"))
          new ImagesDialog(this, theScene, null);
      }
    else if (menu == popupMenu)
      {
        if (command.equals("selectChildren"))
          {
            setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
            setSelection(getSelectionWithChildren());
            updateImage();
          }
        else if (command.equals("hideSelection"))
          setObjectVisibility(false, true);
        else if (command.equals("showSelection"))
          setObjectVisibility(true, true);
        else if (command.equals("lockSelection"))
          setObjectsLocked(true, true);
        else if (command.equals("unlockSelection"))
          setObjectsLocked(false, true);
      }
    clearWaitCursor();
  }

  void importCommand(String format)
  {
    List<Translator> trans = PluginRegistry.getPlugins(Translator.class);
    for (int i = 0; i < trans.size(); i++)
      if (trans.get(i).canImport() && format.equals(trans.get(i).getName()))
        {
          trans.get(i).importFile(this);
          return;
        }
  }

  void exportCommand(String format)
  {
    List<Translator> trans = PluginRegistry.getPlugins(Translator.class);
    for (int i = 0; i < trans.size(); i++)
      if (trans.get(i).canExport() && format.equals(trans.get(i).getName()))
        {
          trans.get(i).exportFile(this, theScene);
          return;
        }
  }

  public void linkExternalCommand()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("externalObject.selectScene"));
    if (!fc.showDialog(this))
      return;
    ExternalObject obj = new ExternalObject(fc.getSelectedFile(), "");
    ObjectInfo info = new ObjectInfo(obj, new CoordinateSystem(), "External Object");
    if (obj.getTexture() == null)
      obj.setTexture(getScene().getDefaultTexture(), getScene().getDefaultTexture().getDefaultMapping(obj));
    UndoRecord undo = new UndoRecord(this, false);
    int sel[] = getSelectedIndices();
    addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    setUndoRecord(undo);
    setSelection(theScene.getNumObjects()-1);
    editObjectCommand();
  }

  void modellingToolCommand(CommandEvent ev)
  {
    Widget item = ev.getWidget();
    for (int i = 0; i < toolsMenuItem.length; i++)
      if (toolsMenuItem[i] == item)
        modellingTools.get(i).commandSelected(this);
  }

  public void saveCommand()
  {
    if (theScene.getName() == null)
      saveAsCommand();
    else
      modified = !ArtOfIllusion.saveScene(theScene, this);
  }

  public void saveAsCommand()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("saveScene"));
    if (theScene.getName() == null)
      fc.setSelectedFile(new File("Untitled.aoi"));
    else
      fc.setSelectedFile(new File(theScene.getName()));
    if (theScene.getDirectory() != null)
      fc.setDirectory(new File(theScene.getDirectory()));
    else if (ArtOfIllusion.getCurrentDirectory() != null)
      fc.setDirectory(new File(ArtOfIllusion.getCurrentDirectory()));
    if (!fc.showDialog(this))
      return;
    String name = fc.getSelectedFile().getName();
    if (!name.toLowerCase().endsWith(".aoi"))
      name = name+".aoi";
    File file = new File(fc.getDirectory(), name);
    if (file.isFile())
    {
      String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
      int choice = new BStandardDialog("", Translate.text("overwriteFile", name), BStandardDialog.QUESTION).showOptionDialog(this, options, options[1]);
      if (choice == 1)
        return;
    }
    theScene.setName(name);
    theScene.setDirectory(fc.getDirectory().getAbsolutePath());
    setTitle(name);
    modified = !ArtOfIllusion.saveScene(theScene, this);
  }

  public void undoCommand()
  {
    undoStack.executeUndo();
    rebuildItemList();
    updateImage();
    updateMenus();
  }

  public void redoCommand()
  {
    undoStack.executeRedo();
    rebuildItemList();
    updateImage();
    updateMenus();
  }

  public void cutCommand()
  {
    copyCommand();
    clearCommand();
  }

  public void copyCommand()
  {
    int sel[] = getSelectionWithChildren();
    if (sel.length == 0)
      return;
    ObjectInfo copy[] = new ObjectInfo [sel.length];
    for (int i = 0; i < sel.length; i++)
      copy[i] = theScene.getObject(sel[i]);
    copy = ObjectInfo.duplicateAll(copy);
    ArtOfIllusion.copyToClipboard(copy, theScene);
    updateMenus();
  }

  public void pasteCommand()
  {
    int which[] = new int [ArtOfIllusion.getClipboardSize()], num = theScene.getNumObjects();
    for (int i = 0; i < which.length; i++)
      which[i] = num+i;
    ArtOfIllusion.pasteClipboard(this);
    setSelection(which);
    rebuildItemList();
    updateImage();
  }

  public void clearCommand()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int selIndex[] = getSelectedIndices();
    boolean any;
    int i;

    if (sel.length == 0)
      return;
    clearSelection();
    UndoRecord undo = new UndoRecord(this, false);

    // First remove any selected objects.

    for (i = sel.length-1; i >= 0; i--)
      {
        ObjectInfo info = (ObjectInfo) sel[i];
        int index = theScene.indexOf(info);
        removeObject(index, undo);
      }

    // Now remove any objects whose parents were just deleted.

    do
    {
      any = false;
      for (i = 0; i < theScene.getNumObjects(); i++)
        {
          ObjectInfo info = theScene.getObject(i);
          if (info.getParent() != null && theScene.indexOf(info.getParent()) == -1)
            {
              removeObject(i, undo);
              i--;
              any = true;
            }
        }
    } while (any);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {selIndex});
    setUndoRecord(undo);
    updateMenus();
    updateImage();
  }

  public void selectAllCommand()
  {
    int i, which[] = new int [theScene.getNumObjects()];

    for (i = 0; i < which.length; i++)
      which[i] = i;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
    setSelection(which);
    updateImage();
  }

  public void duplicateCommand()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int i, which[] = new int [sel.length], num = theScene.getNumObjects();

    UndoRecord undo = new UndoRecord(this, false);
    int selected[] = getSelectedIndices();
    for (i = 0; i < sel.length; i++)
      {
        addObject(((ObjectInfo) sel[i]).duplicate(), undo);
        which[i] = num + i;
      }
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {selected});
    setSelection(which);
    setUndoRecord(undo);
    updateImage();
  }

  public void severCommand()
  {
    Object sel[] = itemTree.getSelectedObjects();
    ObjectInfo info;
    int i;

    UndoRecord undo = new UndoRecord(this, false);
    for (i = 0; i < sel.length; i++)
      {
        info = (ObjectInfo) sel[i];
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setObject(info.object.duplicate());
      }
    setUndoRecord(undo);
  }

  public void editObjectCommand()
  {
    int sel[] = getSelectedIndices();
    final Object3D obj;

    if (sel.length != 1)
      return;
    obj = theScene.getObject(sel[0]).getObject();
    if (obj.isEditable())
      {
        final UndoRecord undo = new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {obj, obj.duplicate()});
        obj.edit(this, theScene.getObject(sel[0]), new Runnable() {
          public void run()
          {
            setUndoRecord(undo);
            theScene.objectModified(obj);
            updateImage();
            updateMenus();
          }
        } );
      }
  }

  public void objectLayoutCommand()
  {
    int i, sel[] = getSelectedIndices();
    TransformDialog dlg;
    ObjectInfo obj[] = new ObjectInfo [sel.length];
    Vec3 orig, size;
    double angles[], values[];

    if (sel.length == 0)
      return;
    UndoRecord undo = new UndoRecord(this, false);
    for (i = 0; i < sel.length; i++)
      {
        obj[i] = theScene.getObject(sel[i]);
        undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {obj[i].getObject(), obj[i].getObject().duplicate()});
        undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {obj[i].getCoords(), obj[i].getCoords().duplicate()});
      }
    if (sel.length == 1)
    {
      orig = obj[0].getCoords().getOrigin();
      angles = obj[0].getCoords().getRotationAngles();
      size = obj[0].getObject().getBounds().getSize();
      dlg = new TransformDialog(this, Translate.text("objectLayoutTitle", theScene.getObject(sel[0]).getName()),
          new double [] {orig.x, orig.y, orig.z, angles[0], angles[1], angles[2],
          size.x, size.y, size.z}, false, false);
      if (!dlg.clickedOk())
        return;
      values = dlg.getValues();
      if (!Double.isNaN(values[0]))
        orig.x = values[0];
      if (!Double.isNaN(values[1]))
        orig.y = values[1];
      if (!Double.isNaN(values[2]))
        orig.z = values[2];
      if (!Double.isNaN(values[3]))
        angles[0] = values[3];
      if (!Double.isNaN(values[4]))
        angles[1] = values[4];
      if (!Double.isNaN(values[5]))
        angles[2] = values[5];
      if (!Double.isNaN(values[6]))
        size.x = values[6];
      if (!Double.isNaN(values[7]))
        size.y = values[7];
      if (!Double.isNaN(values[8]))
        size.z = values[8];
      obj[0].getCoords().setOrigin(orig);
      obj[0].getCoords().setOrientation(angles[0], angles[1], angles[2]);
      obj[0].getObject().setSize(size.x, size.y, size.z);
      theScene.objectModified(obj[0].getObject());
      obj[0].getObject().sceneChanged(obj[0], theScene);
      theScene.applyTracksAfterModification(Collections.singleton(obj[0]));
    }
    else
    {
      dlg = new TransformDialog(this, Translate.text("objectLayoutTitleMultiple"), false, false);
      if (!dlg.clickedOk())
        return;
      values = dlg.getValues();
      for (i = 0; i < sel.length; i++)
      {
        orig = obj[i].getCoords().getOrigin();
        angles = obj[i].getCoords().getRotationAngles();
        size = obj[i].getObject().getBounds().getSize();
        if (!Double.isNaN(values[0]))
          orig.x = values[0];
        if (!Double.isNaN(values[1]))
          orig.y = values[1];
        if (!Double.isNaN(values[2]))
          orig.z = values[2];
        if (!Double.isNaN(values[3]))
          angles[0] = values[3];
        if (!Double.isNaN(values[4]))
          angles[1] = values[4];
        if (!Double.isNaN(values[5]))
          angles[2] = values[5];
        if (!Double.isNaN(values[6]))
          size.x = values[6];
        if (!Double.isNaN(values[7]))
          size.y = values[7];
        if (!Double.isNaN(values[8]))
          size.z = values[8];
        obj[i].getCoords().setOrigin(orig);
        obj[i].getCoords().setOrientation(angles[0], angles[1], angles[2]);
        obj[i].getObject().setSize(size.x, size.y, size.z);
      }
      ArrayList<ObjectInfo> modified = new ArrayList<ObjectInfo>();
      for (int index : sel)
        modified.add(theScene.getObject(index));
      theScene.applyTracksAfterModification(modified);
    }
    setUndoRecord(undo);
    updateImage();
  }

  public void transformObjectCommand()
  {
    int i, sel[] = getSelectedIndices();
    TransformDialog dlg;
    ObjectInfo info;
    Object3D obj;
    CoordinateSystem coords;
    Vec3 orig, size, center;
    double values[];
    Mat4 m;

    if (sel.length == 0)
      return;
    if (sel.length == 1)
      dlg = new TransformDialog(this, Translate.text("transformObjectTitle", theScene.getObject(sel[0]).getName()),
                new double [] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0}, true, true);
    else
      dlg = new TransformDialog(this, Translate.text("transformObjectTitleMultiple"),
                new double [] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0}, true, true);
    if (!dlg.clickedOk())
      return;
    values = dlg.getValues();

    // Find the center of all selected objects.

    BoundingBox bounds = null;
    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      if (bounds == null)
        bounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
      else
        bounds = bounds.merge(info.getBounds().transformAndOutset(info.getCoords().fromLocal()));
    }
    center = bounds.getCenter();
    if (dlg.applyToChildren())
      sel = getSelectionWithChildren();

    // Determine the rotation matrix.

    m = Mat4.identity();
    if (!Double.isNaN(values[3]))
      m = m.times(Mat4.xrotation(values[3]*Math.PI/180.0));
    if (!Double.isNaN(values[4]))
      m = m.times(Mat4.yrotation(values[4]*Math.PI/180.0));
    if (!Double.isNaN(values[5]))
      m = m.times(Mat4.zrotation(values[5]*Math.PI/180.0));
    UndoRecord undo = new UndoRecord(this, false);
    HashSet<Object3D> scaledObjects = new HashSet<Object3D>();
    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      obj = info.getObject();
      coords = info.getCoords();
      if (!scaledObjects.contains(obj))
        undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {obj, obj.duplicate()});
      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {coords, coords.duplicate()});
      orig = coords.getOrigin();
      size = obj.getBounds().getSize();
      if (!Double.isNaN(values[0]))
        orig.x += values[0];
      if (!Double.isNaN(values[1]))
        orig.y += values[1];
      if (!Double.isNaN(values[2]))
        orig.z += values[2];
      if (!Double.isNaN(values[6]))
        size.x *= values[6];
      if (!Double.isNaN(values[7]))
        size.y *= values[7];
      if (!Double.isNaN(values[8]))
        size.z *= values[8];
      if (dlg.useSelectionCenter())
      {
        Vec3 neworig = orig.minus(center);
        if (!Double.isNaN(values[6]))
          neworig.x *= values[6];
        if (!Double.isNaN(values[7]))
          neworig.y *= values[7];
        if (!Double.isNaN(values[8]))
          neworig.z *= values[8];
        coords.setOrigin(neworig);
        coords.transformCoordinates(m);
        coords.setOrigin(coords.getOrigin().plus(center));
      }
      else
      {
        coords.setOrigin(orig);
        coords.transformAxes(m);
      }
      if (!scaledObjects.contains(obj))
      {
        obj.setSize(size.x, size.y, size.z);
        scaledObjects.add(obj);
      }
    }
    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      theScene.objectModified(info.getObject());
    }
    ArrayList<ObjectInfo> modified = new ArrayList<ObjectInfo>();
    for (int index : sel)
      modified.add(theScene.getObject(index));
    theScene.applyTracksAfterModification(modified);
    setUndoRecord(undo);
    updateImage();
  }

  public void alignObjectsCommand()
  {
    int i, sel[] = getSelectedIndices();
    ComponentsDialog dlg;
    ObjectInfo info;
    CoordinateSystem coords;
    Vec3 alignTo, orig, center;
    BComboBox xchoice, ychoice, zchoice;
    RowContainer px = new RowContainer(), py = new RowContainer(), pz = new RowContainer();
    ValueField vfx, vfy, vfz;
    BoundingBox bounds;

    if (sel.length == 0)
      return;
    px.add(xchoice = new BComboBox(new String [] {
      Translate.text("doNotAlign"),
      Translate.text("Right"),
      Translate.text("Center"),
      Translate.text("Left"),
      Translate.text("Origin")
    }));
    px.add(Translate.label("alignTo"));
    px.add(vfx = new ValueField(Double.NaN, ValueField.NONE, 5));
    py.add(ychoice = new BComboBox(new String [] {
      Translate.text("doNotAlign"),
      Translate.text("Top"),
      Translate.text("Center"),
      Translate.text("Bottom"),
      Translate.text("Origin")
    }));
    py.add(Translate.label("alignTo"));
    py.add(vfy = new ValueField(Double.NaN, ValueField.NONE, 5));
    pz.add(zchoice = new BComboBox(new String [] {
      Translate.text("doNotAlign"),
      Translate.text("Front"),
      Translate.text("Center"),
      Translate.text("Back"),
      Translate.text("Origin")
    }));
    pz.add(Translate.label("alignTo"));
    pz.add(vfz = new ValueField(Double.NaN, ValueField.NONE, 5));
    dlg = new ComponentsDialog(this, Translate.text("alignObjectsTitle"),
                new Widget [] {px, py, pz}, new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    UndoRecord undo = new UndoRecord(this, false);

    // Determine the position to align the objects to.

    alignTo = new Vec3();
    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      coords = info.getCoords();
      bounds = info.getBounds();
      bounds = bounds.transformAndOutset(coords.fromLocal());
      center = bounds.getCenter();
      orig = coords.getOrigin();
      if (!Double.isNaN(vfx.getValue()))
        alignTo.x += vfx.getValue();
      else if (xchoice.getSelectedIndex() == 1)
        alignTo.x += bounds.maxx;
      else if (xchoice.getSelectedIndex() == 2)
        alignTo.x += center.x;
      else if (xchoice.getSelectedIndex() == 3)
        alignTo.x += bounds.minx;
      else if (xchoice.getSelectedIndex() == 4)
        alignTo.x += orig.x;
      if (!Double.isNaN(vfy.getValue()))
        alignTo.y += vfy.getValue();
      else if (ychoice.getSelectedIndex() == 1)
        alignTo.y += bounds.maxy;
      else if (ychoice.getSelectedIndex() == 2)
        alignTo.y += center.y;
      else if (ychoice.getSelectedIndex() == 3)
        alignTo.y += bounds.miny;
      else if (ychoice.getSelectedIndex() == 4)
        alignTo.y += orig.y;
      if (!Double.isNaN(vfz.getValue()))
        alignTo.z += vfz.getValue();
      else if (zchoice.getSelectedIndex() == 1)
        alignTo.z += bounds.maxz;
      else if (zchoice.getSelectedIndex() == 2)
        alignTo.z += center.z;
      else if (zchoice.getSelectedIndex() == 3)
        alignTo.z += bounds.minz;
      else if (zchoice.getSelectedIndex() == 4)
        alignTo.z += orig.z;
    }
    alignTo.scale(1.0/sel.length);

    // Now transform all of the objects.

    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      coords = info.getCoords();
      bounds = info.getBounds();
      bounds = bounds.transformAndOutset(coords.fromLocal());
      center = bounds.getCenter();
      orig = coords.getOrigin();
      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {coords, coords.duplicate()});
      if (xchoice.getSelectedIndex() == 1)
        orig.x += alignTo.x-bounds.maxx;
      else if (xchoice.getSelectedIndex() == 2)
        orig.x += alignTo.x-center.x;
      else if (xchoice.getSelectedIndex() == 3)
        orig.x += alignTo.x-bounds.minx;
      else if (xchoice.getSelectedIndex() == 4)
        orig.x += alignTo.x-orig.x;
      if (ychoice.getSelectedIndex() == 1)
        orig.y += alignTo.y-bounds.maxy;
      else if (ychoice.getSelectedIndex() == 2)
        orig.y += alignTo.y-center.y;
      else if (ychoice.getSelectedIndex() == 3)
        orig.y += alignTo.y-bounds.miny;
      else if (ychoice.getSelectedIndex() == 4)
        orig.y += alignTo.y-orig.y;
      if (zchoice.getSelectedIndex() == 1)
        orig.z += alignTo.z-bounds.maxz;
      else if (zchoice.getSelectedIndex() == 2)
        orig.z += alignTo.z-center.z;
      else if (zchoice.getSelectedIndex() == 3)
        orig.z += alignTo.z-bounds.minz;
      else if (zchoice.getSelectedIndex() == 4)
        orig.z += alignTo.z-orig.z;
      coords.setOrigin(orig);
    }
    ArrayList<ObjectInfo> modified = new ArrayList<ObjectInfo>();
    for (int index : sel)
      modified.add(theScene.getObject(index));
    theScene.applyTracksAfterModification(modified);
    setUndoRecord(undo);
    updateImage();
  }

  public void setTextureCommand()
  {
    int sel[] = getSelectedIndices(), i, count = 0;
    ObjectInfo obj[];

    for (i = 0; i < sel.length; i++)
      if (theScene.getObject(sel[i]).getObject().canSetTexture())
        count++;
    if (count == 0)
      return;
    obj = new ObjectInfo [count];
    for (i = 0; i < sel.length; i++)
      if (theScene.getObject(sel[i]).getObject().canSetTexture())
        obj[i] = theScene.getObject(sel[i]);
    new ObjectTextureDialog(this, obj);
    for (i = 0; i < sel.length; i++)
      theScene.objectModified(theScene.getObject(sel[i]).getObject());
    modified = true;
    updateImage();
  }

  public void setMaterialCommand()
  {
    int sel[] = getSelectedIndices(), i, count = 0;
    ObjectInfo obj[], info;

    for (i = 0; i < sel.length; i++)
      {
        info = theScene.getObject(sel[i]);
        if (info.getObject().canSetMaterial())
          count++;
      }
    if (count == 0)
      return;
    obj = new ObjectInfo [count];
    for (i = 0; i < sel.length; i++)
      {
        info = theScene.getObject(sel[i]);
        if (info.getObject().canSetMaterial())
          obj[i] = info;
      }
    new ObjectMaterialDialog(this, obj);
    modified = true;
    updateImage();
  }

  public void renameObjectCommand()
  {
    int sel[] = getSelectedIndices();
    ObjectInfo info;

    if (sel.length != 1)
      return;
    info = theScene.getObject(sel[0]);
    BStandardDialog dlg = new BStandardDialog("", Translate.text("renameObjectTitle"), BStandardDialog.PLAIN);
    String val = dlg.showInputDialog(this, null, info.getName());
    if (val == null)
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.RENAME_OBJECT, new Object [] {new Integer(sel[0]), info.getName()}));
    setObjectName(sel[0], val);
  }

  public void convertToTriangleCommand()
  {
    int sel[] = getSelectedIndices();
    Object3D obj, mesh;
    ObjectInfo info;

    if (sel.length != 1)
      return;
    info = theScene.getObject(sel[0]);
    obj = info.getObject();
    if (obj.canConvertToTriangleMesh() == Object3D.CANT_CONVERT)
      return;

    // If the object has a Pose track, all Pose keyframes will need to be deleted.

    boolean confirmed = false, hasPose = false;
    for (int i = 0; i < info.getTracks().length; i++)
      if (info.getTracks()[i] instanceof PoseTrack)
      {
        hasPose = true;
        if (!confirmed && !info.getTracks()[i].isNullTrack())
        {
          BStandardDialog dlg = new BStandardDialog("", Translate.text("convertLosesPosesWarning", info.getName()), BStandardDialog.QUESTION);
          String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
          if (dlg.showOptionDialog(this, options, options[0]) == 1)
            return;
          confirmed = true;
        }
        if (info.getTracks()[i].getTimecourse() != null)
          info.getTracks()[i].getTimecourse().removeAllTimepoints();
        info.setPose(null);
      }
    if (confirmed)
      theScore.repaintAll();
    UndoRecord undo = new UndoRecord(this, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
    if (obj.canConvertToTriangleMesh() == Object3D.EXACTLY)
    {
      if (!confirmed)
      {
        BStandardDialog dlg = new BStandardDialog("", Translate.text("confirmConvertToTriangle", info.getName()), BStandardDialog.QUESTION);
        String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
        if (dlg.showOptionDialog(this, options, options[0]) == 1)
          return;
      }
      mesh = obj.convertToTriangleMesh(0.0);
    }
    else
    {
      ValueField errorField = new ValueField(0.1, ValueField.POSITIVE);
      ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("selectToleranceForMesh"),
          new Widget [] {errorField}, new String [] {Translate.text("maxError")});
      if (!dlg.clickedOk())
        return;
      mesh = obj.convertToTriangleMesh(errorField.getValue());
    }
    if (mesh == null)
    {
      new BStandardDialog("", Translate.text("cannotTriangulate"), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    if (hasPose)
      mesh = mesh.getPosableObject();
    if (mesh.getTexture() == null)
    {
      Texture tex = theScene.getDefaultTexture();
      mesh.setTexture(tex, tex.getDefaultMapping(mesh));
    }
    theScene.replaceObject(obj, mesh, undo);
    setUndoRecord(undo);
    updateImage();
    updateMenus();
  }

  public void convertToActorCommand()
  {
    int sel[] = getSelectedIndices();
    Object3D obj;
    ObjectInfo info;

    if (sel.length != 1)
      return;
    info = theScene.getObject(sel[0]);
    obj = info.getObject();
    Object3D posable = obj.getPosableObject();
    if (posable == null)
      return;
    BStandardDialog dlg = new BStandardDialog("", UIUtilities.breakString(Translate.text("confirmConvertToActor", info.getName())), BStandardDialog.QUESTION);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    if (dlg.showOptionDialog(this, options, options[0]) == 1)
      return;
    UndoRecord undo = new UndoRecord(this, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
    theScene.replaceObject(obj, posable, undo);
    setUndoRecord(undo);
    updateImage();
    updateMenus();
  }

  private void setObjectVisibility(boolean visible, boolean selectionOnly)
  {
    UndoRecord undo = new UndoRecord(this, false);
    if (selectionOnly)
    {
      int sel[] = getSelectedIndices();
      for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setVisible(visible);
      }
    }
    else
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setVisible(visible);
      }
    setUndoRecord(undo);
    updateImage();
    itemTree.repaint();
  }

  private void setObjectsLocked(boolean locked, boolean selectionOnly)
  {
    UndoRecord undo = new UndoRecord(this, false);
    if (selectionOnly)
    {
      int sel[] = getSelectedIndices();
      for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setLocked(locked);
      }
    }
    else
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setLocked(locked);
      }
    setUndoRecord(undo);
    updateImage();
    itemTree.repaint();
  }


  void createObjectCommand(CommandEvent ev)
  {
    String type = ev.getActionCommand();
    Object3D obj;
    String name;

    if ("cube".equals(type))
    {
      obj = new Cube(1.0, 1.0, 1.0);
      name = "Cube "+(CreateCubeTool.counter++);
    }
    else if ("sphere".equals(type))
    {
      obj = new Sphere(0.5, 0.5, 0.5);
      name = "Sphere "+(CreateSphereTool.counter++);
    }
    else if ("cylinder".equals(type))
    {
      obj = new Cylinder(1.0, 0.5, 0.5, 1.0);
      name = "Cylinder "+(CreateCylinderTool.counter++);
    }
    else if ("cone".equals(type))
    {
      obj = new Cylinder(1.0, 0.5, 0.5, 0.0);
      name = "Cone "+(CreateCylinderTool.counter++);
    }
    else if ("pointLight".equals(type))
    {
      obj = new PointLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f, 0.1);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("directionalLight".equals(type))
    {
      obj = new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("spotLight".equals(type))
    {
      obj = new SpotLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f, 20.0, 0.0, 0.1);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("proceduralPointLight".equals(type))
    {
      obj = new ProceduralPointLight(0.1);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("proceduralDirectionalLight".equals(type))
    {
      obj = new ProceduralDirectionalLight(1.0);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("camera".equals(type))
    {
      obj = new SceneCamera();
      name = "Camera "+(CreateCameraTool.counter++);
    }
    else if ("referenceImage".equals(type))
    {
      BFileChooser fc = new ImageFileChooser(Translate.text("selectReferenceImage"));
      if (!fc.showDialog(this))
        return;
      File f = fc.getSelectedFile();
      Image image = new ImageIcon(f.getAbsolutePath()).getImage();
      if (image == null || image.getWidth(null) <= 0 || image.getHeight(null) <= 0)
      {
        new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingImage", f.getName())), BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
      obj = new ReferenceImage(image);
      name = f.getName();
      if (name.lastIndexOf('.') > -1)
        name = name.substring(0, name.lastIndexOf('.'));
    }
    else
    {
      obj = new NullObject();
      name = "Null";
    }
    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    ObjectInfo info = new ObjectInfo(obj, coords, name);
    if (obj.canSetTexture())
      info.setTexture(theScene.getDefaultTexture(), theScene.getDefaultTexture().getDefaultMapping(obj));
    Vec3 orig = coords.getOrigin();
    double angles[] = coords.getRotationAngles();
    Vec3 size = info.getBounds().getSize();
    TransformDialog dlg = new TransformDialog(this, Translate.text("objectLayoutTitle", name),
        new double [] {orig.x, orig.y, orig.z, angles[0], angles[1], angles[2],
        size.x, size.y, size.z}, false, false);
    if (!dlg.clickedOk())
      return;
    double values[] = dlg.getValues();
    if (!Double.isNaN(values[0]))
      orig.x = values[0];
    if (!Double.isNaN(values[1]))
      orig.y = values[1];
    if (!Double.isNaN(values[2]))
      orig.z = values[2];
    if (!Double.isNaN(values[3]))
      angles[0] = values[3];
    if (!Double.isNaN(values[4]))
      angles[1] = values[4];
    if (!Double.isNaN(values[5]))
      angles[2] = values[5];
    if (!Double.isNaN(values[6]))
      size.x = values[6];
    if (!Double.isNaN(values[7]))
      size.y = values[7];
    if (!Double.isNaN(values[8]))
      size.z = values[8];
    coords.setOrigin(orig);
    coords.setOrientation(angles[0], angles[1], angles[2]);
    obj.setSize(size.x, size.y, size.z);
    info.clearCachedMeshes();
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    UndoRecord undo = new UndoRecord(this, false);
    int sel[] = getSelectedIndices();
    addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    setSelection(theScene.getNumObjects()-1);
    setUndoRecord(undo);
    updateImage();
  }

  public void createScriptObjectCommand()
  {
    // Prompt the user to select a name and, optionally, a predefined script.

    BTextField nameField = new BTextField(Translate.text("Script"));
    BComboBox scriptChoice = new BComboBox();
    scriptChoice.add(Translate.text("newScript"));
    String files[] = new File(ArtOfIllusion.OBJECT_SCRIPT_DIRECTORY).list();
    if (files != null)
      for (int i = 0; i < files.length; i++)
        if (files[i].endsWith(".bsh") && files[i].length() > 4)
          scriptChoice.add(files[i].substring(0, files[i].length()-4));
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("newScriptedObject"),
      new Widget [] {nameField, scriptChoice}, new String [] {Translate.text("Name"), Translate.text("Script")});
    if (!dlg.clickedOk())
      return;

    // If they are using a predefined script, load it.

    String scriptText = "";
    if (scriptChoice.getSelectedIndex() > 0)
    {
      try
      {
        String scriptName = scriptChoice.getSelectedValue()+".bsh";
        File f = new File(ArtOfIllusion.OBJECT_SCRIPT_DIRECTORY, scriptName);
        scriptText = ArtOfIllusion.loadFile(f);
      }
      catch (IOException ex)
      {
        new BStandardDialog("", new String [] {Translate.text("errorReadingScript"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
    }
    ScriptedObject obj = new ScriptedObject("");
    ObjectInfo info = new ObjectInfo(obj, new CoordinateSystem(), nameField.getText());
    UndoRecord undo = new UndoRecord(this, false);
    int sel[] = getSelectedIndices();
    addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    setSelection(theScene.getNumObjects()-1);
    setUndoRecord(undo);
    updateImage();
    obj.setScript(scriptText);
    editObjectCommand();
  }

  public void jumpToTimeCommand()
  {
    ValueField timeField = new ValueField(theScene.getTime(), ValueField.NONE);
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("jumpToTimeTitle"),
      new Widget [] {timeField}, new String [] {Translate.text("Time")});

    if (!dlg.clickedOk())
      return;
    double t = timeField.getValue();
    double fps = theScene.getFramesPerSecond();
    t = Math.round(t*fps)/(double) fps;
    setTime(t);
  }

  public void bindToParentCommand()
  {
    BStandardDialog dlg = new BStandardDialog("", UIUtilities.breakString(Translate.text("confirmBindParent")), BStandardDialog.QUESTION);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    if (dlg.showOptionDialog(this, options, options[0]) == 1)
      return;
    int sel[] = getSelectedIndices();

    UndoRecord undo = new UndoRecord(this, false);
    for (int i = 0; i < sel.length; i++)
    {
      ObjectInfo info = theScene.getObject(sel[i]);
      if (info.getParent() == null)
        continue;
      Skeleton s = info.getParent().getSkeleton();
      ObjectRef relObj = new ObjectRef(info.getParent());
      if (s != null)
      {
        double nearest = Double.MAX_VALUE;
        Joint jt[] = s.getJoints();
        Vec3 pos = info.getCoords().getOrigin();
        for (int j = 0; j < jt.length; j++)
        {
          ObjectRef r = new ObjectRef(info.getParent(), jt[j]);
          double dist = r.getCoords().getOrigin().distance2(pos);
          if (dist < nearest)
          {
            relObj = r;
            nearest = dist;
          }
        }
      }
      undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
      PositionTrack pt = new PositionTrack(info);
      pt.setCoordsObject(relObj);
      info.addTrack(pt, 0);
      pt.setKeyframe(theScene.getTime(), theScene);
      RotationTrack rt = new RotationTrack(info);
      rt.setCoordsObject(relObj);
      info.addTrack(rt, 1);
      rt.setKeyframe(theScene.getTime(), theScene);
    }
    setUndoRecord(undo);
    theScore.rebuildList();
    theScore.repaint();
  }

  public void renderCommand()
  {
    new RenderSetupDialog(this, theScene);
  }

  public void toggleViewsCommand()
  {
    if (numViewsShown == 4)
    {
      numViewsShown = 1;
      viewsContainer.setColumnWeight(0, (currentView == 0 || currentView == 2) ? 1 : 0);
      viewsContainer.setColumnWeight(1, (currentView == 1 || currentView == 3) ? 1 : 0);
      viewsContainer.setRowWeight(0, (currentView == 0 || currentView == 1) ? 1 : 0);
      viewsContainer.setRowWeight(1, (currentView == 2 || currentView == 3) ? 1 : 0);
      sceneMenuItem[0].setText(Translate.text("menu.fourViews"));
    }
    else
    {
      numViewsShown = 4;
      viewsContainer.setColumnWeight(0, 1);
      viewsContainer.setColumnWeight(1, 1);
      viewsContainer.setRowWeight(0, 1);
      viewsContainer.setRowWeight(1, 1);
      sceneMenuItem[0].setText(Translate.text("menu.oneView"));
    }
    viewsContainer.layoutChildren();
    savePreferences();
    updateImage();
    viewPanel[currentView].requestFocus();
  }

  public void setTemplateCommand()
  {
    BFileChooser fc = new ImageFileChooser(Translate.text("selectTemplateImage"));
    if (!fc.showDialog(this))
      return;
    File f = fc.getSelectedFile();
    try
    {
      theView[currentView].setTemplateImage(f);
    }
    catch (InterruptedException ex)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingImage", f.getName())), BStandardDialog.ERROR).showMessageDialog(this);
    }
    theView[currentView].setShowTemplate(true);
    updateImage();
    updateMenus();
  }

  public void setGridCommand()
  {
    ValueField spaceField = new ValueField(theScene.getGridSpacing(), ValueField.POSITIVE);
    ValueField divField = new ValueField(theScene.getGridSubdivisions(), ValueField.POSITIVE+ValueField.INTEGER);
    BCheckBox showBox = new BCheckBox(Translate.text("showGrid"), theScene.getShowGrid());
    BCheckBox snapBox = new BCheckBox(Translate.text("snapToGrid"), theScene.getSnapToGrid());
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("gridTitle"),
                new Widget [] {spaceField, divField, showBox, snapBox},
                new String [] {Translate.text("gridSpacing"), Translate.text("snapToSubdivisions"), null, null});
    if (!dlg.clickedOk())
      return;
    theScene.setGridSpacing(spaceField.getValue());
    theScene.setGridSubdivisions((int) divField.getValue());
    theScene.setShowGrid(showBox.getState());
    theScene.setSnapToGrid(snapBox.getState());
    for (int i = 0; i < theView.length; i++)
      theView[i].setGrid(theScene.getGridSpacing(), theScene.getGridSubdivisions(), theScene.getShowGrid(), theScene.getSnapToGrid());
    updateImage();
  }

  public void frameWithCameraCommand(boolean selectionOnly)
  {
    int sel[] = getSelectionWithChildren();
    BoundingBox bb = null;

    if (selectionOnly)
      for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        BoundingBox bounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
        if (bb == null)
          bb = bounds;
        else
          bb = bb.merge(bounds);
      }
    else
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        BoundingBox bounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
        if (bb == null)
          bb = bounds;
        else
          bb = bb.merge(bounds);
      }
    if (bb == null)
      return;
    if (numViewsShown == 1)
      theView[currentView].frameBox(bb);
    else
      for (int i = 0; i < theView.length; i++)
        theView[i].frameBox(bb);
    updateImage();
  }

  public void texturesCommand()
  {
    theScene.showTexturesDialog(this);
  }

  public void materialsCommand()
  {
    theScene.showMaterialsDialog(this);
  }

  public void environmentCommand()
  {
    final RGBColor ambColor = theScene.getAmbientColor(), envColor = theScene.getEnvironmentColor(), fogColor = theScene.getFogColor();
    final RGBColor oldAmbColor = ambColor.duplicate(), oldEnvColor = envColor.duplicate(), oldFogColor = fogColor.duplicate();
    final Widget ambPatch = ambColor.getSample(50, 30), envPatch = envColor.getSample(50, 30), fogPatch = fogColor.getSample(50, 30);
    final BCheckBox fogBox = new BCheckBox("Environment Fog", theScene.getFogState());
    final ValueField fogField = new ValueField(theScene.getFogDistance(), ValueField.POSITIVE);
    final OverlayContainer envPanel = new OverlayContainer();
    final BComboBox envChoice;
    final BButton envButton = new BButton(Translate.text("Choose")+":");
    final BLabel envLabel = new BLabel();
    final Sphere envSphere = new Sphere(1.0, 1.0, 1.0);
    final ObjectInfo envInfo = new ObjectInfo(envSphere, new CoordinateSystem(), "Environment");

    envChoice = new BComboBox(new String [] {
      Translate.text("solidColor"),
      Translate.text("textureDiffuse"),
      Translate.text("textureEmissive")
    });
    envChoice.setSelectedIndex(theScene.getEnvironmentMode());
    RowContainer row = new RowContainer();
    row.add(envButton);
    row.add(envLabel);
    envPanel.add(envPatch, 0);
    envPanel.add(row, 1);
    if (theScene.getEnvironmentMode() == Scene.ENVIRON_SOLID)
      envPanel.setVisibleChild(0);
    else
      envPanel.setVisibleChild(1);
    envInfo.setTexture(theScene.getEnvironmentTexture(), theScene.getEnvironmentMapping());
    envSphere.setParameterValues(theScene.getEnvironmentParameterValues());
    envLabel.setText(envSphere.getTexture().getName());
    envChoice.addEventLink(ValueChangedEvent.class, new Object()
    {
      void processEvent()
      {
        if (envChoice.getSelectedIndex() == Scene.ENVIRON_SOLID)
          envPanel.setVisibleChild(0);
        else
          envPanel.setVisibleChild(1);
        envPanel.getParent().layoutChildren();
      }
    });
    final Runnable envTextureCallback = new Runnable() {
      public void run()
      {
        envLabel.setText(envSphere.getTexture().getName());
        envPanel.getParent().layoutChildren();
      }
    };
    envButton.addEventLink(CommandEvent.class, new Object()
    {
      void processEvent()
      {
        envPanel.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ObjectTextureDialog otd = new ObjectTextureDialog(LayoutWindow.this, new ObjectInfo [] {envInfo});
        otd.setCallback(envTextureCallback);
        envPanel.getParent().setCursor(Cursor.getDefaultCursor());
      }
    });
    ambPatch.addEventLink(MouseClickedEvent.class, new Object()
    {
      void processEvent()
      {
        new ColorChooser(LayoutWindow.this, Translate.text("ambientColor"), ambColor);
        ambPatch.setBackground(ambColor.getColor());
        ambPatch.repaint();
      }
    });
    envPatch.addEventLink(MouseClickedEvent.class, new Object()
    {
      void processEvent()
      {
        new ColorChooser(LayoutWindow.this, Translate.text("environmentColor"), envColor);
        envPatch.setBackground(envColor.getColor());
        envPatch.repaint();
      }
    });
    fogPatch.addEventLink(MouseClickedEvent.class, new Object()
    {
      void processEvent()
      {
        new ColorChooser(LayoutWindow.this, Translate.text("fogColor"), fogColor);
        fogPatch.setBackground(fogColor.getColor());
        fogPatch.repaint();
      }
    });
    Runnable okCallback = new Runnable() {
      public void run()
      {
        theScene.setFog(fogBox.getState(), fogField.getValue());
        theScene.setEnvironmentMode(envChoice.getSelectedIndex());
        theScene.setEnvironmentTexture(envSphere.getTexture());
        theScene.setEnvironmentMapping(envSphere.getTextureMapping());
        theScene.setEnvironmentParameterValues(envSphere.getParameterValues());
        setModified();
      }
    };
    Runnable cancelCallback = new Runnable() {
      public void run()
      {
        ambColor.copy(oldAmbColor);
        envColor.copy(oldEnvColor);
        fogColor.copy(oldFogColor);
      }
    };
    new ComponentsDialog(LayoutWindow.this, Translate.text("environmentTitle"),
        new Widget [] {ambPatch, envChoice, envPanel, fogBox, fogPatch, fogField},
        new String [] {Translate.text("ambientColor"), Translate.text("environment"), "", "", Translate.text("fogColor"), Translate.text("fogDistance")},
        okCallback, cancelCallback);
  }

  private void executeScriptCommand(CommandEvent ev)
  {
    executeScript(new File(ev.getActionCommand()));
  }

  /** Execute the tool script contained in a file, passing a reference to this window in its "window" variable. */

  public void executeScript(File f)
  {
    // Read the script from the file.

    String scriptText = null;
    try
    {
      scriptText = ArtOfIllusion.loadFile(f);
    }
    catch (IOException ex)
    {
      new BStandardDialog("", new String [] {Translate.text("errorReadingScript"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    try
    {
      ToolScript script = ScriptRunner.parseToolScript(scriptText);
      script.execute(this);
    }
    catch (Exception e)
    {
      ScriptRunner.displayError(e, 1);
    }
    updateImage();
    dispatchSceneChangedEvent(); // To be safe, since we can't rely on scripts to set undo records or call setModified().
  }
}
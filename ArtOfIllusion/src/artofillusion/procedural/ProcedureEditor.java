/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;

/** This is the editor for editing procedures.  It subclasses CustomWidget, but you should never
    add it to any Container.  Instead, it will automatically create a BFrame and add itself
    to that. */

public class ProcedureEditor extends CustomWidget
{
  private BFrame parent;
  private Procedure proc;
  private ProcedureOwner owner;
  private Scene theScene;
  private EditingWindow win;
  private Dimension size;
  private BMenuItem undoItem, cutItem, copyItem, pasteItem, clearItem;
  private BTextField nameField;
  private boolean selectedModule[], selectedLink[], draggingLink, draggingModule, draggingBox, undoIsRedo;
  private Point clickPos, lastPos;
  private InfoBox inputInfo, outputInfo;
  private IOPort dragFromPort, dragToPort;
  private BScrollPane scroll;
  private Object preview;
  private ByteArrayOutputStream undoBuffer, cancelBuffer;

  private final Color darkLinkColor = Color.darkGray;
  private final Color blueLinkColor = new Color(40, 40, 255);
  private final Color selectedLinkColor = new Color(255, 50, 50);
  private final Color outputBackgroundColor = new Color(210, 210, 240);
  private final static float BEZIER_HARDNESS = 0.5f; //increase hardness to a have a more pronouced shape
  private final static Stroke normal = new BasicStroke();
  private final static Stroke bold = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
  
  private static ClipboardSelection clipboard;

  public ProcedureEditor(Procedure proc, ProcedureOwner owner, Scene scene)
  {
    super();
    this.proc = proc;
    this.owner = owner;
    theScene = scene;
    selectedModule = new boolean [proc.getModules().length];
    selectedLink = new boolean [proc.getLinks().length];
    inputInfo = new InfoBox();
    outputInfo = new InfoBox();
    undoBuffer = new ByteArrayOutputStream();
    cancelBuffer = new ByteArrayOutputStream();
    parent = new BFrame(owner.getWindowTitle());
    BorderContainer content = new BorderContainer();
    parent.setContent(content);
    content.add(scroll = new BScrollPane(this), BorderContainer.CENTER);
    scroll.setPreferredViewSize(new Dimension(600, 600));
    new AutoScroller(scroll, 5, 5);
    size = new Dimension(1000, 1000);
    setBackground(Color.white);
    addEventLink(KeyPressedEvent.class, this, "keyPressed");
    addEventLink(MousePressedEvent.class, this, "mousePressed");
    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
    addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
    addEventLink(RepaintEvent.class, this, "paint");

    // Save the current state of the procedure so that editing can be canceled.

    DataOutputStream out = new DataOutputStream(cancelBuffer);
    try
    {
      proc.writeToStream(out, theScene);
      out.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    // Create the buttons at the top of the window.
    
    FormContainer top = new FormContainer(new double [] {0.0, 0.0, 1.0, 0.0, 0.0}, new double [] {1.0});
    top.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(0, 0, 0, 5), null));
    top.add(Translate.button("properties", this, "actionPerformed"), 0, 0);
    if (owner.canEditName())
    {
      top.add(new BLabel(Translate.text("Name")+':'), 1, 0);
      top.add(nameField = new BTextField(owner.getName()), 2, 0);
    }
    top.add(Translate.button("ok", this, "doOk"), 3, 0);
    top.add(Translate.button("cancel", this, "actionPerformed"), 4, 0);
    parent.addEventLink(WindowClosingEvent.class, this, "doOk");
    content.add(top, BorderContainer.NORTH);
    
    // Let each output module calculate its preferred width, then set all of them to be
    // as wide as the widest one.
    
    OutputModule output[] = proc.getOutputModules();
    int i, widest = 0;
    for (i = 0; i < output.length; i++)
      {
        output[i].calcSize();
        if (output[i].getBounds().width > widest)
          widest = output[i].getBounds().width;
      }
    int x = size.width-widest, y = 15;
    for (i = 0; i < output.length; i++)
      {
        output[i].setWidth(widest);
        output[i].setPosition(x - 15, y);
        y += output[i].getBounds().height+15;
      }
    
    // Add the menu bar.
    
    BMenuBar mb = new BMenuBar();
    parent.setMenuBar(mb);
    mb.add(getEditMenu());
    mb.add(getInsertMenu());
    updateMenus();
    
    // Display the window.
    
    parent.pack();
    scroll.getHorizontalScrollBar().setBlockIncrement(100);
    scroll.getVerticalScrollBar().setBlockIncrement(100);
    scroll.getHorizontalScrollBar().setUnitIncrement(10);
    scroll.getVerticalScrollBar().setUnitIncrement(10);
    parent.setVisible(true);
    scroll.getHorizontalScrollBar().setValue(getBounds().width-scroll.getViewSize().width);
    preview = owner.getPreview(this);
  }

  /** Create the Edit menu. */
  
  private BMenu getEditMenu()
  {
    BMenu editMenu = Translate.menu("edit");    
    editMenu.add(undoItem = Translate.menuItem("undo", this, "actionPerformed"));
    undoItem.setEnabled(false);
    editMenu.addSeparator();
    editMenu.add(cutItem = Translate.menuItem("cut", this, "actionPerformed"));
    editMenu.add(copyItem = Translate.menuItem("copy", this, "actionPerformed"));
    editMenu.add(pasteItem = Translate.menuItem("paste", this, "actionPerformed"));
    editMenu.add(clearItem = Translate.menuItem("clear", this, "actionPerformed"));
    editMenu.addSeparator();
    editMenu.add(Translate.menuItem("properties", this, "actionPerformed"));
    return editMenu;
  }

  /** Create the Insert menu. */
  
  private BMenu getInsertMenu()
  {
    BMenu insertMenu = Translate.menu("insert"), subMenu;
    
    insertMenu.add(subMenu = Translate.menu("values"));
    subMenu.add(Translate.menuItem("numberModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("colorModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("xModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("yModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("zModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("timeModule", this, "actionPerformed"));
    if (owner.allowViewAngle())
      subMenu.add(Translate.menuItem("viewAngleModule", this, "actionPerformed"));
    if (owner.allowParameters())
      subMenu.add(Translate.menuItem("parameterModule", this, "actionPerformed"));
    subMenu.addSeparator();
    subMenu.add(Translate.menuItem("commentModule", this, "actionPerformed"));

    insertMenu.add(subMenu = Translate.menu("functions"));
    subMenu.add(Translate.menuItem("expressionModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("customFunctionModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("scaleShiftModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("addModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("subtractModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("multiplyModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("divideModule", this, "actionPerformed"));
    subMenu.addSeparator();
    subMenu.add(Translate.menuItem("absModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("blurModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("clipModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("greaterThanModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("minModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("maxModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("interpolateModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("modModule", this, "actionPerformed"));
    subMenu.addSeparator();
    subMenu.add(Translate.menuItem("sineModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("cosineModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("sqrtModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("expModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("logModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("powModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("biasModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("gainModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("randomModule", this, "actionPerformed"));
    
    insertMenu.add(subMenu = Translate.menu("colorFunctions"));
    subMenu.add(Translate.menuItem("customColorFunctionModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("blendModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("addColorModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("subtractColorModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("multiplyColorModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("lighterModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("darkerModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("scaleColorModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("RGBModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("HSVModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("HLSModule", this, "actionPerformed"));

    insertMenu.add(subMenu = Translate.menu("transforms"));
    subMenu.add(Translate.menuItem("linearModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("polarModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("sphericalModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("jitterModule", this, "actionPerformed"));

    insertMenu.add(subMenu = Translate.menu("patterns"));
    subMenu.add(Translate.menuItem("noiseModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("turbulenceModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("gridModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("cellsModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("marbleModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("woodModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("checkerModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("bricksModule", this, "actionPerformed"));
    subMenu.add(Translate.menuItem("imageModule", this, "actionPerformed"));

    List<Module> plugins = PluginRegistry.getPlugins(Module.class);
    if (plugins.size() > 0)
    {
      insertMenu.add(subMenu = Translate.menu("plugins"));
      for (int i = 0; i < plugins.size(); i++)
      {
        try
        {
          BMenuItem mi = new BMenuItem((plugins.get(i).getClass().newInstance()).getName());
          mi.setActionCommand(plugins.get(i).getClass().getName());
          mi.addEventLink(CommandEvent.class, this, "actionPerformed");
          subMenu.add(mi);
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
    }
    return insertMenu;
  }
  
  /** Get the editor's parent Frame. */
  
  public BFrame getParentFrame()
  {
    return parent;
  }
  
  /** Get the scene the procedure is part of. */
  
  public Scene getScene()
  {
    return theScene;
  }
  
  /** Set the editing window which owns the scene the procedure is part of. */
  
  public void setEditingWindow(EditingWindow window)
  {
    win = window;
  }
  
  /** Get the editing window which owns the scene the procedure is part of. */
  
  public EditingWindow getEditingWindow()
  {
    return win;
  }
  
  public Dimension getPreferredSize()
  {
    return size;
  }
  
  private void paint(RepaintEvent ev)
  {
    paint(ev.getGraphics());
  }
  
  private void paint(Graphics2D g)
  {
    OutputModule output[] = proc.getOutputModules();
    Module module[] = proc.getModules();
    Link link[] = proc.getLinks();
    int divider = output[0].getBounds().x-5;

    // Draw the line marking off the output modules.
    
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(outputBackgroundColor);
    g.fillRoundRect(divider, 5, size.width - divider - 10, size.height - 10, 8, 8);
    
    // Draw the output modules.

    for (int i = 0; i < output.length; i++)
      output[i].draw(g, false);
    
    // Draw the modules.
    
    for (int i = 0; i < module.length; i++)
      module[i].draw(g, selectedModule[i]);
    
    // Draw the unselected links.
    
    g.setStroke(bold);
    for (int i = 0; i < link.length; i++)
      if (!selectedLink[i])
        {
          g.setColor(link[i].from.getValueType() == IOPort.NUMBER ? darkLinkColor : blueLinkColor);
          g.draw(createBezierCurve(link[i]));
        }
    
    // Draw the selected links.

    g.setColor(selectedLinkColor);
    for (int i = 0; i < link.length; i++)
      if (selectedLink[i])
        g.draw(createBezierCurve(link[i]));
    g.setStroke(normal);
    
    // If we are in the middle of dragging something, draw the thing being dragged.
    
    if (draggingLink)
      {
        boolean isInput = (dragFromPort.getType() == IOPort.INPUT);
        if (isInput || dragToPort != null)
          inputInfo.draw(g);
        if (!isInput || dragToPort != null)
          outputInfo.draw(g);
      }
    g.setXORMode(Color.WHITE);
    if (draggingBox && lastPos != null)
      {
        Rectangle rect;
        rect = getRectangle(clickPos, lastPos);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
      }
    if (draggingLink && lastPos != null)
      {
        if (dragToPort == null)
          g.drawLine(clickPos.x, clickPos.y, lastPos.x, lastPos.y);
        else
          {
            Point pos = dragToPort.getPosition();
            g.drawLine(clickPos.x, clickPos.y, pos.x, pos.y);
          }
      }
    if (draggingModule && lastPos != null)
      {
        int dx = lastPos.x-clickPos.x, dy = lastPos.y-clickPos.y;
        for (int i = 0; i < selectedModule.length; i++)
          if (selectedModule[i])
            {
              Rectangle rect = module[i].getBounds();
              g.drawRect(rect.x+dx, rect.y+dy, rect.width, rect.height);
            }
      }
  }

  private Shape createBezierCurve(Link link)
  {
    int x1 = link.from.getPosition().x;
    int y1 = link.from.getPosition().y;
    int x2 = link.to.getPosition().x;
    int y2 = link.to.getPosition().y;
    float ctrlx1, ctrly1, ctrlx2, ctrly2;
    if (link.from.getLocation() == IOPort.LEFT || link.from.getLocation() == IOPort.RIGHT)
    {
      ctrlx1 = (x2-x1)*BEZIER_HARDNESS + x1;
      ctrly1 = y1;
    }
    else
    {
      ctrlx1 = x1;
      ctrly1 = (y2-y1)*BEZIER_HARDNESS + y1;
    }
    if (link.to.getLocation() == IOPort.LEFT || link.to.getLocation() == IOPort.RIGHT)
    {
      ctrlx2 = (1 - BEZIER_HARDNESS)*(x2-x1) + x1;
      ctrly2 = y2;
    }
    else
    {
      ctrlx2 = x2;
      ctrly2 = (1 - BEZIER_HARDNESS)*(y2-y1) + y1;
    }
    return new CubicCurve2D.Float(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
  }

  /** Update the items in the Edit menu whenever the selection changes. */
  
  private void updateMenus()
  {
    boolean anyModule = false, anyLink = false;
    for (int i = 0; i < selectedModule.length; i++)
      if (selectedModule[i])
        anyModule = true;
    for (int i = 0; i < selectedLink.length; i++)
      if (selectedLink[i])
        anyLink = true;
    cutItem.setEnabled(anyModule);
    copyItem.setEnabled(anyModule);
    pasteItem.setEnabled(clipboard != null);
    clearItem.setEnabled(anyModule || anyLink);
  }
  
  /** Respond to menu items. */
  
  private void actionPerformed(CommandEvent e)
  {
    String command = e.getActionCommand();
    Point p = new Point(scroll.getHorizontalScrollBar().getValue(), scroll.getVerticalScrollBar().getValue());
    Rectangle bounds = scroll.getBounds();

    p.x += (int) (0.5*bounds.width*Math.random());
    p.y += (int) (0.5*bounds.height*Math.random());
    if (command.equals("cancel"))
      {
        undoBuffer = cancelBuffer;
        undo();
        owner.disposePreview(preview);
        parent.dispose();
        return;
      }
    if (command.equals("undo"))
      {
        undo();
        return;
      }
    if (command.equals("cut"))
      {
        clipboard = new ClipboardSelection(proc, selectedModule, selectedLink);
        deleteSelection();
        updateMenus();
        return;
      }
    if (command.equals("copy"))
      {
        clipboard = new ClipboardSelection(proc, selectedModule, selectedLink);
        updateMenus();
        return;
      }
    if (command.equals("paste") && clipboard != null)
      {
        saveState(false);
        clipboard.paste(this);
        repaint();
        return;
      }
    if (command.equals("clear"))
      {
        deleteSelection();
        return;
      }
    if (command.equals("properties"))
    {
      owner.editProperties(this);
      updatePreview();
      return;
    }
    saveState(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    if (command.equals("numberModule"))
      addModule(new NumberModule(p));
    else if (command.equals("colorModule"))
      addModule(new ColorModule(p));
    else if (command.equals("xModule"))
      addModule(new CoordinateModule(p, CoordinateModule.X));
    else if (command.equals("yModule"))
      addModule(new CoordinateModule(p, CoordinateModule.Y));
    else if (command.equals("zModule"))
      addModule(new CoordinateModule(p, CoordinateModule.Z));
    else if (command.equals("timeModule"))
      addModule(new CoordinateModule(p, CoordinateModule.T));
    else if (command.equals("viewAngleModule"))
      addModule(new ViewAngleModule(p));
    else if (command.equals("parameterModule"))
      addModule(new ParameterModule(p));
    else if (command.equals("commentModule"))
      addModule(new CommentModule(p));
    else if (command.equals("addModule"))
      addModule(new SumModule(p));
    else if (command.equals("subtractModule"))
      addModule(new DifferenceModule(p));
    else if (command.equals("multiplyModule"))
      addModule(new ProductModule(p));
    else if (command.equals("divideModule"))
      addModule(new RatioModule(p));
    else if (command.equals("scaleShiftModule"))
      addModule(new ScaleShiftModule(p));
    else if (command.equals("interpolateModule"))
      addModule(new InterpModule(p));
    else if (command.equals("greaterThanModule"))
      addModule(new CompareModule(p));
    else if (command.equals("minModule"))
      addModule(new MinModule(p));
    else if (command.equals("maxModule"))
      addModule(new MaxModule(p));
    else if (command.equals("modModule"))
      addModule(new ModModule(p));
    else if (command.equals("absModule"))
      addModule(new AbsModule(p));
    else if (command.equals("clipModule"))
      addModule(new ClipModule(p));
    else if (command.equals("sineModule"))
      addModule(new SineModule(p));
    else if (command.equals("cosineModule"))
      addModule(new CosineModule(p));
    else if (command.equals("sqrtModule"))
      addModule(new SqrtModule(p));
    else if (command.equals("expModule"))
      addModule(new ExpModule(p));
    else if (command.equals("logModule"))
      addModule(new LogModule(p));
    else if (command.equals("powModule"))
      addModule(new PowerModule(p));
    else if (command.equals("biasModule"))
      addModule(new BiasModule(p));
    else if (command.equals("gainModule"))
      addModule(new GainModule(p));
    else if (command.equals("randomModule"))
      addModule(new RandomModule(p));
    else if (command.equals("blurModule"))
      addModule(new BlurModule(p));
    else if (command.equals("customFunctionModule"))
      addModule(new FunctionModule(p));
    else if (command.equals("expressionModule"))
      addModule(new ExprModule(p));
    else if (command.equals("linearModule"))
      addModule(new TransformModule(p));
    else if (command.equals("polarModule"))
      addModule(new PolarModule(p));
    else if (command.equals("sphericalModule"))
      addModule(new SphericalModule(p));
    else if (command.equals("jitterModule"))
      addModule(new JitterModule(p));
    else if (command.equals("addColorModule"))
      addModule(new ColorSumModule(p));
    else if (command.equals("subtractColorModule"))
      addModule(new ColorDifferenceModule(p));
    else if (command.equals("multiplyColorModule"))
      addModule(new ColorProductModule(p));
    else if (command.equals("lighterModule"))
      addModule(new ColorLightenModule(p));
    else if (command.equals("darkerModule"))
      addModule(new ColorDarkenModule(p));
    else if (command.equals("scaleColorModule"))
      addModule(new ColorScaleModule(p));
    else if (command.equals("blendModule"))
      addModule(new BlendModule(p));
    else if (command.equals("customColorFunctionModule"))
      addModule(new SpectrumModule(p));
    else if (command.equals("RGBModule"))
      addModule(new RGBModule(p));
    else if (command.equals("HSVModule"))
      addModule(new HSVModule(p));
    else if (command.equals("HLSModule"))
      addModule(new HLSModule(p));
    else if (command.equals("noiseModule"))
      addModule(new NoiseModule(p));
    else if (command.equals("turbulenceModule"))
      addModule(new TurbulenceModule(p));
    else if (command.equals("marbleModule"))
      addModule(new MarbleModule(p));
    else if (command.equals("woodModule"))
      addModule(new WoodModule(p));
    else if (command.equals("gridModule"))
      addModule(new GridModule(p));
    else if (command.equals("cellsModule"))
      addModule(new CellsModule(p));
    else if (command.equals("checkerModule"))
      addModule(new CheckerModule(p));
    else if (command.equals("bricksModule"))
      addModule(new BrickModule(p));
    else if (command.equals("imageModule"))
      addModule(new ImageModule(p));
    else
    {
      try
      {
        Class cl = ArtOfIllusion.getClass(command);
        Module mod = (Module) cl.newInstance();
        mod.setPosition(p.x, p.y);
        addModule(mod);
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    setCursor(Cursor.getDefaultCursor());
    repaint();
  }

  /** Save changes and close the window. */

  private void doOk()
  {
      if (owner.canEditName())
        owner.setName(nameField.getText());
      owner.acceptEdits(this);
      owner.disposePreview(preview);
      parent.dispose();
  }
  
  /** Add a module to the procedure. */
  
  private void addModule(Module mod)
  {
    selectedModule = new boolean [selectedModule.length+1];
    selectedModule[selectedModule.length-1] = true;
    for (int i = 0; i < selectedLink.length; i++)
      selectedLink[i] = false;
    proc.addModule(mod);
    updateMenus();
  }
  
  /** Add a link between two ports in the procedure. */
  
  private void addLink(IOPort port1, IOPort port2)
  {
    IOPort from, to;
    
    selectedLink = new boolean [selectedLink.length+1];
    selectedLink[selectedLink.length-1] = true;
    for (int i = 0; i < selectedModule.length; i++)
      selectedModule[i] = false;
    if (port1.getType() == IOPort.OUTPUT)
      {
        from  = port1;
        to = port2;
      }
    else
      {
        from  = port2;
        to = port1;
      }
    proc.addLink(new Link(from, to));
    if (proc.checkFeedback())
      {
        proc.deleteLink(proc.getLinks().length-1);
        selectedLink = new boolean [proc.getLinks().length];
        new BStandardDialog(null, new String [] {"The link you have selected cannot be created,",
          "as it would result in a feedback loop."}, BStandardDialog.ERROR).showMessageDialog(parent);
      }
    updateMenus();
  }
  
  /** Record the current state of the procedure, so that it can be undone. */
  
  public void saveState(boolean redo)
  {
    undoBuffer.reset();
    DataOutputStream out = new DataOutputStream(undoBuffer);
    try
      {
        proc.writeToStream(out, theScene);
        out.close();
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
      }
    undoIsRedo = redo & !undoIsRedo;
    undoItem.setText(undoIsRedo ? "Redo" : "Undo");
    undoItem.setEnabled(true);
  }
  
  /** Undo the last action. */
  
  private void undo()
  {
    byte buffer[] = undoBuffer.toByteArray();
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
    
    saveState(true);
    try
      {
        proc.readFromStream(in, theScene);
        in.close();
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
      }
    selectedModule = new boolean [proc.getModules().length];
    selectedLink = new boolean [proc.getLinks().length];
    repaint();
    updatePreview();
    updateMenus();
  }
  
  /** Update the preview. */
  
  public void updatePreview()
  {
    owner.updatePreview(preview);
  }
    
  /** The canvas needs to be able to accept focus. */
  
  public boolean isFocusTraversable()
  {
    return true;
  }
  
  /** Respond to mouse clicks. */
  
  private void mouseClicked(MouseClickedEvent e)
  {
    Point pos = e.getPoint();
    if (e.getClickCount() == 2)
      {
        // See if the click was on a module.  If so, call its edit() method.
        
        Module module[] = proc.getModules();
        for (int i = 0; i < module.length; i++)
          if (module[i].getBounds().contains(pos))
            {
              saveState(false);
              if (module[i].edit(parent, theScene))
                {
                  repaint();
                  updatePreview();
                }
              return;
            }
      }
  }

  /** Respond to mouse presses. */

  private void mousePressed(MousePressedEvent e)
  {
    OutputModule output[] = proc.getOutputModules();
    Module module[] = proc.getModules();
    Link link[] = proc.getLinks();
    IOPort port;
    int i, j;
    
    requestFocus();
    clickPos = e.getPoint();
    lastPos = null;

    // First see if the mouse was pressed on a port.
    
    for (i = 0; i < module.length; i++)
      {
        port = module[i].getClickedPort(clickPos);
        if (port != null)
          {
            startDragLink(port);
            return;
          }
      }
    for (i = 0; i < output.length; i++)
      {
        port = output[i].getClickedPort(clickPos);
        if (port != null)
          {
            startDragLink(port);
            return;
          }
      }
    
    // See if the mouse was pressed on a selected module.
    
    for (i = module.length-1; i >= 0; i--)
      if (selectedModule[i] && module[i].getBounds().contains(clickPos))
        {
          draggingModule = true;
          repaint();
          return;
        }

    // See if the mouse was pressed on an unselected module.
    
    for (i = module.length-1; i >= 0; i--)
      if (!selectedModule[i] && module[i].getBounds().contains(clickPos))
        {
          draggingModule = true;
          if (!e.isShiftDown())
          {
            Arrays.fill(selectedModule, false);
            Arrays.fill(selectedLink, false);
          }
          selectedModule[i] = true;
          repaint();
          updateMenus();
          return;
        }

    // See if the mouse was pressed on a link.
    
    for (i = 0; i < link.length; i++)
      {
        int tol = 2;
        if (!createBezierCurve(link[i]).intersects(new Rectangle(clickPos.x - tol, clickPos.y - tol, 2*tol, 2*tol)))
          continue;
        if (!e.isShiftDown())
        {
          Arrays.fill(selectedModule, false);
          Arrays.fill(selectedLink, false);
        }
        selectedLink[i] = true;
        repaint();
        updateMenus();
        return;
      }

    // Erase the selection if the shift key is not held down.
    
    if (!e.isShiftDown())
    {
      Arrays.fill(selectedModule, false);
      Arrays.fill(selectedLink, false);
    }
    draggingBox = true;
    repaint();
    updateMenus();
  }
  
  private void mouseReleased(MouseReleasedEvent e)
  {
    Point pos = e.getPoint();
    
    if (draggingLink)
      {
        draggingLink = false;
        if (dragToPort != null)
          {
            saveState(false);
            addLink(dragFromPort, dragToPort);
            updatePreview();
          }
        repaint();
        return;
      }
    if (draggingBox)
      {
        if (lastPos == null)
          {
            draggingBox = false;
            repaint();
            return;
          }
        Rectangle rect = getRectangle(clickPos, lastPos);
        Module module[] = proc.getModules();
        for (int i = 0; i < selectedModule.length; i++)
          if (module[i].getBounds().intersects(rect))
            selectedModule[i] = true;
        draggingBox = false;
        repaint();
        updateMenus();
        return;
      }
    
    // Move any selected modules.
    
    if (draggingModule)
    {
      saveState(false);
      draggingModule = false;
      int dx = pos.x-clickPos.x, dy = pos.y-clickPos.y;
      Module module[] = proc.getModules();
      for (int i = 0; i < selectedModule.length; i++)
        if (selectedModule[i])
          {
            Rectangle rect = module[i].getBounds();
            module[i].setPosition(rect.x+dx, rect.y+dy);
          }
    }
    repaint();
  }
  
  /** Deal with mouse drags. */
  
  private void mouseDragged(MouseDraggedEvent e)
  {
    Point pos = e.getPoint();
    
    if (draggingBox)
      {
        // A selection box is being dragged.

        Graphics g = getComponent().getGraphics();
        Rectangle rect;
        g.setColor(Color.black);
        g.setXORMode(Color.white);
        if (lastPos != null)
          {
            rect = getRectangle(clickPos, lastPos);
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
          }
        lastPos = pos;
        rect = getRectangle(clickPos, lastPos);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
        g.dispose();
        return;
      }
    if (draggingLink)
      {
        // A link is being dragged.  If the mouse was previously dragged to another port,
        // and is still over it, simply return.
        
        if (dragToPort != null && dragToPort.contains(pos))
          return;
        
        // If the mouse has been moved off a port, redraw everything.  Otherwise, just
        // erase the previous line.
        
        Graphics2D g = (Graphics2D) getComponent().getGraphics();
        boolean isInput = (dragFromPort.getType() == IOPort.INPUT);
        if (dragToPort != null)
          {
            dragToPort = null;
            paint(g);
          }
        g.setColor(Color.black);
        g.setXORMode(Color.white);
        if (lastPos != null)
          g.drawLine(clickPos.x, clickPos.y, lastPos.x, lastPos.y);
        
        // See whether the mouse is now over a port.
        
        OutputModule output[] = proc.getOutputModules();
        Module module[] = proc.getModules();
        for (int i = 0; i < module.length; i++)
          {
            IOPort port[] = isInput ? module[i].getOutputPorts() : module[i].getInputPorts();
            for (int j = 0; j < port.length; j++)
              if (isInput || !module[i].inputConnected(j))
                if (port[j].getValueType() == dragFromPort.getValueType() && port[j].contains(pos))
                  dragToPort = port[j];
            if (dragToPort != null)
              break;
          }
        if (!isInput)
          for (int i = 0; i < output.length; i++)
            {
              IOPort port[] = output[i].getInputPorts();
              for (int j = 0; j < port.length; j++)
                if (!output[i].inputConnected(j))
                  if (port[j].getValueType() == dragFromPort.getValueType() && port[j].contains(pos))
                    dragToPort = port[j];
              if (dragToPort != null)
                break;
            }

        // If the mouse is now over a port, we need to show its info box.
        
        if (dragToPort != null)
          {
            InfoBox info = isInput ? outputInfo : inputInfo;
            Rectangle rect = info.getBounds();
            pos = dragToPort.getPosition();
            if (isInput)
              info.setPosition(pos.x-rect.width-10, pos.y-rect.height/2);
            else
              info.setPosition(pos.x+10, pos.y-rect.height/2);
            info.setText(dragToPort.getDescription());
            repaint();
          }
        else
          g.drawLine(clickPos.x, clickPos.y, pos.x, pos.y);
        g.dispose();
        lastPos = pos;
        return;
      }
    
    // Move any selected modules.
    
    if (!draggingModule)
      return;
    int dx = pos.x-clickPos.x, dy = pos.y-clickPos.y, lastdx = 0, lastdy = 0;
    Module module[] = proc.getModules();
    Graphics g = getComponent().getGraphics();
    if (lastPos != null)
      {
        lastdx = lastPos.x-clickPos.x;
        lastdy = lastPos.y-clickPos.y;
      }
    g.setColor(Color.black);
    g.setXORMode(Color.white);
    for (int i = 0; i < selectedModule.length; i++)
      if (selectedModule[i])
        {
          Rectangle rect = module[i].getBounds();
          if (lastPos != null)
            g.drawRect(rect.x+lastdx, rect.y+lastdy, rect.width, rect.height);
          g.drawRect(rect.x+dx, rect.y+dy, rect.width, rect.height);
        }
    g.dispose();
    lastPos = pos;
  }
    
  /** Start dragging a link. */
  
  private void startDragLink(IOPort port)
  {
    boolean isInput = (port.getType() == IOPort.INPUT);
    InfoBox info = isInput ? inputInfo : outputInfo;
    Point pos = port.getPosition();

    draggingLink = true;
    draggingModule = false;
    dragFromPort = port;
    dragToPort = null;
    clickPos = port.getPosition();
    info.setText(port.getDescription());
    Rectangle rect = info.getBounds();
    if (isInput)
      info.setPosition(pos.x+10, pos.y-rect.height/2);
    else
      info.setPosition(pos.x-rect.width-10, pos.y-rect.height/2);
    Graphics g = getComponent().getGraphics();
    info.draw(g);
    g.dispose();
    
    // If this is an input port which is already connected to something, don't actually
    // drag a link.
    
    if (isInput)
      {
        Module module = port.getModule();
        IOPort inputs[] = module.getInputPorts();
        for (int i = 0; i < inputs.length; i++)
          if (inputs[i] == port && module.inputConnected(i))
            draggingLink = false;
      }
  }
  
  /** Respond to key presses. */
  
  private void keyPressed(KeyPressedEvent e)
  {
    int key = e.getKeyCode();

    if (key != KeyPressedEvent.VK_BACK_SPACE && key != KeyPressedEvent.VK_DELETE)
      return;
    deleteSelection();
  }
  
  /** Delete any selected modules and links. */
  
  private void deleteSelection()
  {
    saveState(false);
    
    // First select any links which are connected to selected modules, since they will also need to be deleted.
    
    Module module[] = proc.getModules();
    Link link[] = proc.getLinks();
    for (int i = 0; i < selectedLink.length; i++)
      for (int j = 0; j < selectedModule.length; j++)
        if ((module[j] == link[i].from.getModule() && selectedModule[j]) || 
            (module[j] == link[i].to.getModule() && selectedModule[j]))
        selectedLink[i] = true;
    
    // Delete any selected links.
    
    for (int i = selectedLink.length-1; i >= 0; i--)
      if (selectedLink[i])
        proc.deleteLink(i);
    
    // Now delete any selected modules.

    for (int i = selectedModule.length-1; i >= 0; i--)
      if (selectedModule[i])
        proc.deleteModule(i);
    selectedModule = new boolean [proc.getModules().length];
    selectedLink = new boolean [proc.getLinks().length];
    updatePreview();
    repaint();
    updateMenus();
  }
  
  /** Utility function to create a Rectangle from two Points. */
  
  public Rectangle getRectangle(Point p1, Point p2)
  {
    int x, y, width, height;
    
    x = Math.min(p1.x, p2.x);
    y = Math.min(p1.y, p2.y);
    width = Math.abs(p1.x-p2.x);
    height = Math.abs(p1.y-p2.y);
    return new Rectangle(x, y, width, height);
  }
  
  /** Inner class representing a set of Modules and Links on the clipboard. */
  
  private static class ClipboardSelection
  {
    Module module[];
    Link link[];
    
    /** Create a new ClipboardSelection representing the current selection. */
    
    public ClipboardSelection(Procedure proc, boolean selectedModule[], boolean selectedLink[])
    {
      // Determine which modules and links to copy.
      
      Vector mod = new Vector(), ln = new Vector();
      Module allModules[] = proc.getModules();
      Link allLinks[] = proc.getLinks();
      for (int i = 0; i < selectedModule.length; i++)
        if (selectedModule[i])
          mod.addElement(allModules[i]);
      for (int i = 0; i < selectedLink.length; i++)
        if (mod.indexOf(allLinks[i].from.getModule()) > -1 && mod.indexOf(allLinks[i].to.getModule()) > -1)
          ln.addElement(allLinks[i]);
      
      // Duplicate them and build the arrays.
      
      module = new Module [mod.size()];
      link = new Link [ln.size()];
      for (int i = 0; i < module.length; i++)
        module[i] = ((Module) mod.elementAt(i)).duplicate();
      for (int i = 0; i < link.length; i++)
        {
          Link thisLink = (Link) ln.elementAt(i);
          int from = mod.indexOf(thisLink.from.getModule());
          int to = mod.indexOf(thisLink.to.getModule());
          int fromPort = thisLink.getFromPortIndex();
          int toPort = thisLink.getToPortIndex();
          link[i] = new Link(module[from].getOutputPorts()[fromPort], module[to].getInputPorts()[toPort]);
        }
    }
    
    /** Paste the clipboard selection into the procedure. */
    
    public void paste(ProcedureEditor editor)
    {
      int numModules = editor.selectedModule.length, numLinks = editor.selectedLink.length;
      Module realMod[] = new Module [module.length];
      
      // Add the modules.
      
      for (int i = 0; i < module.length; i++)
        {
          if (module[i] instanceof ParameterModule && !editor.owner.allowParameters())
            continue;
          realMod[i] = module[i].duplicate();
          if (realMod[i] instanceof ImageModule && editor.theScene.indexOf(((ImageModule) realMod[i]).getMap()) == -1)
            ((ImageModule) realMod[i]).setMap(null);
          Rectangle bounds = realMod[i].getBounds();
          realMod[i].setPosition(bounds.x-30, bounds.y+30);
          editor.addModule(realMod[i]);
        }
      
      // Add the links.
      
      for (int i = 0; i < link.length; i++)
        {
          int from, to;
          for (from = 0; module[from] != link[i].from.getModule(); from++);
          for (to = 0; module[to] != link[i].to.getModule(); to++);
          if (realMod[from] == null || realMod[to] == null)
            continue;
          int fromPort = link[i].getFromPortIndex();
          int toPort = link[i].getToPortIndex();
          editor.addLink(realMod[from].getOutputPorts()[fromPort], realMod[to].getInputPorts()[toPort]);
        }
      
      // Select everything that was just pasted.
      
      for (int i = 0; i < editor.selectedModule.length; i++)
        editor.selectedModule[i] = (i >= numModules);
      for (int i = 0; i < editor.selectedLink.length; i++)
        editor.selectedLink[i] = (i >= numLinks);
      editor.updateMenus();
    }
  }
}

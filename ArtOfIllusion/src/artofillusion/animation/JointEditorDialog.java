/* Copyright (C) 2002-2004 by Peter Eastman
   Changes copyright (C) 2020 by Maksim Khramov
   Changes Copyright (C) 2020 Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.ui.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.animation.Joint.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;

/** This is a dialog box for editing joints in a skeleton. */

public class JointEditorDialog extends BDialog
{
  private MeshEditorWindow window;
  private Mesh theMesh;
  private Object3D oldMesh;
  private Skeleton skeleton;
  private Joint joint;
  private BTextField nameField;
  private DOFPanel ang1Panel, ang2Panel, twistPanel, lengthPanel;
  private BButton okButton, cancelButton;
  private boolean disableUpdating;

  public JointEditorDialog(MeshEditorWindow win, int jointID)
  {
    super(win, "Edit Bone", true);
    window = win;
    theMesh = (Mesh) window.getObject().getObject();
    oldMesh = ((Object3D) theMesh).duplicate();
    skeleton = theMesh.getSkeleton();
    joint = skeleton.getJoint(jointID);

    // Layout the dialog.

    BorderContainer content = new BorderContainer();
    setContent(content);
    RowContainer nameRow = new RowContainer();
    nameRow.add(Translate.label("Name"));
    nameRow.add(nameField = new BTextField(joint.name, 20));
    content.add(nameRow, BorderContainer.NORTH, new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(5, 5, 5, 5), null));

    FormContainer center = new FormContainer(4, 2);
    center.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(5, 5, 5, 5), null));
    center.add(makeBorder(ang1Panel = new DOFPanel("X Bend", -180.0, 180.0, joint.angle1)), 0, 0);
    center.add(ang1Panel.getGraph(), 1, 0);
    center.add(makeBorder(ang2Panel = new DOFPanel("Y Bend", -180.0, 180.0, joint.angle2)), 2, 0);
    center.add(ang2Panel.getGraph(), 3, 0);
    center.add(makeBorder(twistPanel = new DOFPanel("Twist", -180.0, 180.0, joint.twist)), 0, 1);
    center.add(twistPanel.getGraph(), 1, 1);
    center.add(makeBorder(lengthPanel = new DOFPanel("Length", 0.0, Double.MAX_VALUE, joint.length)), 2, 1);
    content.add(center, BorderContainer.CENTER);

    if (joint.parent == null)
      lengthPanel.setEnabled(false);
    RowContainer buttons = new RowContainer();
    buttons.add(okButton = Translate.button("ok", this, "doOk"));
    buttons.add(cancelButton = Translate.button("cancel", this, "doCancel"));
    content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
    setResizable(false);
    addEventLink(WindowClosingEvent.class, this, "doCancel");
    pack();
    setVisible(true);
  }

  private Widget makeBorder(Widget w)
  {
    return BOutline.createBevelBorder(BOutline.createEmptyBorder(w, 4), false);
  }

  private void doOk()
  {
    valueChanged();
    joint.name = nameField.getText();
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.COPY_OBJECT, theMesh, oldMesh));
    dispose();
  }

  private void doCancel()
  {
    ((Object3D) theMesh).copyObject(oldMesh);
    window.objectChanged();
    window.updateImage();
    dispose();
  }

  /** This method is called when any of the values in the ValueFields is changed. */

  private void valueChanged()
  {
    boolean ok = (ang1Panel.isValid() && ang2Panel.isValid() && twistPanel.isValid() && lengthPanel.isValid());
    okButton.setEnabled(ok);
    if (!ok/* || disableUpdating*/)
      return;
    ang1Panel.recordValues();
    ang2Panel.recordValues();
    twistPanel.recordValues();
    lengthPanel.recordValues();
    joint.recalcCoords(true);
    if (!((MeshViewer) window.getView()).getSkeletonDetached())
      Skeleton.adjustMesh((Mesh) oldMesh, theMesh);
    window.objectChanged();
    window.updateImage();
  }

  /** Inner class representing a panel for editing a single degree of freedom. */

  //private class DOFPanel extends FormContainer
  private class DOFPanel extends ColumnContainer
  {
    public ValueField valField, minField, maxField, minComfortField, maxComfortField;
    public ValueSlider stiffnessSlider;
    public BCheckBox fixedBox, rangeBox, comfortBox;
    public double min, max;
    public DOF dof;
    private DOFGraph graph;

    public DOFPanel(String name, double min, double max, DOF dof)
    {
      //super(3, 7);
      super();
      this.min = min;
      this.max = max;
      this.dof = dof;

      // Create the components for the panel.

      valField = new ValueField(dof.pos, ValueField.NONE, 6);
      minField = new ValueField(dof.min, ValueField.NONE, 4);
      maxField = new ValueField(dof.max == Double.MAX_VALUE ? Double.NaN : dof.max, ValueField.NONE, 4);
      minComfortField = new ValueField(dof.minComfort, ValueField.NONE, 4);
      maxComfortField = new ValueField(dof.maxComfort == Double.MAX_VALUE ? Double.NaN : dof.maxComfort, ValueField.NONE, 4);
      valField.setValueChecker(new ValueChecker() {
        @Override
        public boolean isValid(double val)
        {
          double lower = (rangeBox.getState() ? minField.getValue() : DOFPanel.this.min);
          double upper = (rangeBox.getState() ? maxField.getValue() : DOFPanel.this.max);
          return (val >= lower && val <= upper);
        }
      } );
      minField.setValueChecker(new ValueChecker() {
        @Override
        public boolean isValid(double val)
        {
          if (!rangeBox.getState())
            return true;
          if (comfortBox.getState() && val > minComfortField.getValue())
            return false;
          return (val <= valField.getValue() && val >= DOFPanel.this.min);
        }
      } );
      maxField.setValueChecker(new ValueChecker() {
        @Override
        public boolean isValid(double val)
        {
          if (!rangeBox.getState())
            return true;
          if (comfortBox.getState() && val < maxComfortField.getValue())
            return false;
          return (val >= valField.getValue() && val <= DOFPanel.this.max);
        }
      } );
      minComfortField.setValueChecker(new ValueChecker() {
        @Override
        public boolean isValid(double val)
        {
          if (!comfortBox.getState() || !rangeBox.getState())
            return true;
          double lower = minField.getValue();
          double upper = Math.min(maxField.getValue(), maxComfortField.getValue());
          return (val >= lower && val <= upper);
        }
      } );
      maxComfortField.setValueChecker(new ValueChecker() {
        @Override
        public boolean isValid(double val)
        {
          if (!comfortBox.getState() || !rangeBox.getState())
            return true;
          double lower = Math.max(minField.getValue(), minComfortField.getValue());
          double upper = maxField.getValue();
          return (val >= lower && val <= upper);
        }
      } );
      Object tl = new Object() {
        void processEvent()
        {
          valField.checkIfValid();
          minField.checkIfValid();
          maxField.checkIfValid();
          minComfortField.checkIfValid();
          maxComfortField.checkIfValid();
          valueChanged();
          if (graph != null)
            graph.repaint();
        }
      };
      valField.addEventLink(ValueChangedEvent.class, tl);
      minField.addEventLink(ValueChangedEvent.class, tl);
      maxField.addEventLink(ValueChangedEvent.class, tl);
      minComfortField.addEventLink(ValueChangedEvent.class, tl);
      maxComfortField.addEventLink(ValueChangedEvent.class, tl);
      valField.sendValidEventsOnly(false);
      minField.sendValidEventsOnly(false);
      maxField.sendValidEventsOnly(false);
      minComfortField.sendValidEventsOnly(false);
      maxComfortField.sendValidEventsOnly(false);
      stiffnessSlider = new ValueSlider(0.0, 1.0, 100, dof.stiffness); 
        ((BSlider)((ArrayList)stiffnessSlider.getChildren()).get(1)).getComponent().setPreferredSize(new Dimension(100, 20));
        ((BTextField)((ArrayList)stiffnessSlider.getChildren()).get(0)).setColumns(3);
      fixedBox = new BCheckBox(Translate.text("Lock"), dof.fixed);
      rangeBox = new BCheckBox(Translate.text("restrictTotalRange"), dof.min > min || dof.max < max || dof.comfort);
      comfortBox = new BCheckBox(Translate.text("restrictComfortRange"), dof.comfort);
      fixedBox.addEventLink(ValueChangedEvent.class, this, "checkboxChanged");
      rangeBox.addEventLink(ValueChangedEvent.class, this, "checkboxChanged");
      comfortBox.addEventLink(ValueChangedEvent.class, this, "checkboxChanged");

      // Build the UI sub components ...

      LayoutInfo nogap = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 0), null);
      setDefaultLayout(nogap);
      RowContainer[] row = new RowContainer[4];
      for (int i = 0; i < row.length; i++)
        row[i] = new RowContainer();
      ColumnContainer rangeBlock = new ColumnContainer();
      ColumnContainer comfortBlock = new ColumnContainer();
      ColumnContainer stiffBlock = new ColumnContainer();
      RowContainer rangeRow = new RowContainer();
      RowContainer comfortRow = new RowContainer();

      // ... fill them ...
  
      LayoutInfo rowgap = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 4, 0, 4), null);
      LayoutInfo rowfirst = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 4), null);
      LayoutInfo rowfirstontop = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 4, 4, 4), null);
      LayoutInfo boxgap = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 0), null);
      LayoutInfo underboxgap = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(-2, 0, 0, 0), null);
      LayoutInfo blockgap = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(4, 0, 4, 0), null);

      rangeRow.add(minField, rowfirst);
      rangeRow.add(Translate.label("to"), rowgap);
      rangeRow.add(maxField, rowgap);
      rangeBlock.add(rangeBox, boxgap);
      rangeBlock.add(rangeRow, rowgap);

      comfortRow.add(minComfortField, rowfirst);
      comfortRow.add(Translate.label("to"), rowgap);
      comfortRow.add(maxComfortField, rowgap);
      comfortBlock.add(comfortBox, boxgap);
      comfortBlock.add(comfortRow, rowgap);

      stiffBlock.add(Translate.label("Stiffness"), rowfirstontop);
      stiffBlock.add(stiffnessSlider, rowgap);

      // Make sure, that all the name labels are the same size. The text is not translated.
      // The size of the label affects the size of each panel.

      BLabel nameLabel = new BLabel("X-Xxxx");
      nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
      Dimension nameSize = nameLabel.getPreferredSize();
      nameLabel.setText(name);
      nameLabel.getComponent().setPreferredSize(nameSize);

      // ... and add to the panel.

      row[0].add(nameLabel, rowgap);
      row[0].add(valField, rowgap);
      row[0].add(fixedBox, boxgap);
      row[1].add(rangeBlock, blockgap);
      row[2].add(comfortBlock, blockgap);
      row[3].add(stiffBlock, blockgap);

      for (RowContainer r : row)
        add(r);
      updateComponents();
    }

    /* Update the states of the various components. */

    public void updateComponents()
    {
      boolean fixed = fixedBox.getState();
      boolean range = (!fixed && rangeBox.getState());
      boolean comfort = (range && comfortBox.getState());
      valField.setEnabled(!fixed);
      minField.setEnabled(range);
      maxField.setEnabled(range);
      minComfortField.setEnabled(comfort);
      maxComfortField.setEnabled(comfort);
      valField.setEditable(!fixed);
      minField.setEditable(range);
      maxField.setEditable(range);
      minComfortField.setEditable(comfort);
      maxComfortField.setEditable(comfort);
      stiffnessSlider.setEnabled(!fixed);
      rangeBox.setEnabled(!fixed);
      comfortBox.setEnabled(range);
    }

    /* Respond to clicks on the checkboxes. */

    private void checkboxChanged(WidgetEvent ev)
    {
      if (ev.getWidget() == comfortBox && comfortBox.getState())
      {
        if (minComfortField.getValue() < minField.getValue())
          minComfortField.setValue(minField.getValue());
        if (maxComfortField.getValue() > maxField.getValue())
          maxComfortField.setValue(maxField.getValue());
      }
      updateComponents();
      valField.checkIfValid();
      minField.checkIfValid();
      maxField.checkIfValid();
      minComfortField.checkIfValid();
      maxComfortField.checkIfValid();
      if (graph != null)
        graph.repaint();
      valueChanged();
    }

    /** Determine whether the values entered in the fields are all valid. */

    public boolean isValid()
    {
      if (fixedBox.getState())
        return true;
      return (valField.isValid(valField.getValue()) && minField.isValid(minField.getValue()) && maxField.isValid(maxField.getValue()) &&
        minComfortField.isValid(minComfortField.getValue()) && maxComfortField.isValid(maxComfortField.getValue()));
    }

    /** Copy the entered values into the DOF object. */

    public void recordValues()
    {
      dof.fixed = fixedBox.getState();
      boolean range = (!dof.fixed && rangeBox.getState());
      dof.pos = valField.getValue();
      if (range)
      {
        dof.min = minField.getValue();
        dof.max = maxField.getValue();
        if (Double.isNaN(dof.max))
          dof.max = Double.MAX_VALUE;
      }
      else
      {
        dof.min = min;
        dof.max = max;
      }
      dof.minComfort = minComfortField.getValue();
      dof.maxComfort = maxComfortField.getValue();
      if (Double.isNaN(dof.maxComfort))
        dof.maxComfort = Double.MAX_VALUE;
      dof.stiffness = stiffnessSlider.getValue();
      dof.comfort = (range && comfortBox.getState());
      dof.loop = ((max-min == 360.0) && (!range || (dof.max-dof.min == 360.0)));
    }

    /** Set the enabled state of every component in this panel. */

    @Override
    public void setEnabled(boolean enabled)
    {
      super.setEnabled(enabled);
      Iterator child = getChildren().iterator();
      while (child.hasNext())
        ((Widget) child.next()).setEnabled(enabled);
    }

    /** Get a graph showing the values for this panel. */

    public DOFGraph getGraph()
    {
      if (graph == null)
        graph = new DOFGraph(this);
      return graph;
    }
  }

  /** Inner class which draws the circular diagram representing the range of motion for
      a degree of freedom. */

  private class DOFGraph extends CustomWidget
  {
    private DOFPanel panel;
    private boolean dragging0, dragging1, dragging2, dragging3, dragging4;
    private double lastAngle;
    private ActionProcessor process;

    private static final double DIAL_R1    = 55.0;
    private static final int    GRAPH_SIZE = (int)(DIAL_R1*2 + 1);
    private static final double HANDLE_R   = 6.0;
    private static final double ARM_R1   = DIAL_R1-HANDLE_R-1.0;
    private static final double ARM_R2   = DIAL_R1-HANDLE_R*2-2.0;
    private static final double ARM_R3   = DIAL_R1-HANDLE_R*3-3.0;

    private Color restColor1 = new Color(223,  31,  63, 127);
    private Color restColor2 = new Color(223,  31,  63, 255);
    private Color comfColor1 = new Color(255, 199,  63, 127);
    private Color comfColor2 = new Color(255, 199,  63, 255);
    private Color valuColor1 = new Color( 31,  99, 159, 255);
    private Color valuColor2 = new Color( 47,  63, 127);
    private Color valuColor3 = new Color(175, 175, 175);
    private Color dialActive = Color.white;
    private Color dialIdle   = new Color(223,  223, 223);
    private Color dialMark   = Color.gray;

    private Point centerPoint;
    private BufferedImage image;
    private Graphics2D g2;
    private AffineTransform atr_init, atr_draw;
    private Ellipse2D.Double dial, trace;
    private Shape[] marker = new Shape[24];

    public DOFGraph(DOFPanel dp)
    {
      panel = dp;
      addEventLink(MousePressedEvent.class, this, "mousePressed");
      addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
      addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
      addEventLink(RepaintEvent.class, this, "paint");
      setPreferredSize(new Dimension(GRAPH_SIZE, GRAPH_SIZE));
      image = new BufferedImage(GRAPH_SIZE, GRAPH_SIZE, BufferedImage.TYPE_INT_ARGB);
      g2 = image.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      atr_init = new AffineTransform(g2.getTransform());
      atr_draw = new AffineTransform(g2.getTransform());
      atr_draw.scale(1.0, -1.0);
      atr_draw.translate((double)GRAPH_SIZE*0.5, (double)GRAPH_SIZE*-0.5);
      atr_draw.quadrantRotate(1);
      dial = new Ellipse2D.Double(-DIAL_R1, -DIAL_R1, DIAL_R1*2.0, DIAL_R1*2.0);
      trace = new Ellipse2D.Double(-ARM_R1, -ARM_R1, ARM_R1*2.0, ARM_R1*2.0);

      Vec2 arm1, arm2;
      double r0;
      for (int i = 0; i < 24; i++)
      {
        if ((i*15)%45 == 0)
          r0 = ARM_R3;
        else
          r0 = ARM_R1;
        arm1 = getValueVector(i*15, r0);
        arm2 = getValueVector(i*15, DIAL_R1);
        marker[i] = new Line2D.Double(arm1.x, arm1.y, arm2.x, arm2.y);
      }
      centerPoint = new Point(GRAPH_SIZE/2, GRAPH_SIZE/2); // Calculated deliberately as odd int/2;
    }

    private void mousePressed(MousePressedEvent ev)
    {
      Vec2 mouseVector, valueVector1, valueVector2;
      process = new ActionProcessor();
      lastAngle = 0.0;

      // pointer

      if (panel.fixedBox.getState())
        return;
      mouseVector = getMouseVector(ev.getPoint());
      valueVector1 = getValueVector(panel.valField.getValue(), ARM_R1);
      if (onHandle(valueVector1, mouseVector, HANDLE_R))
      {
        dragging0 = true;
        disableUpdating = true;
        return;
      }

      // restriceted range 

      double minLimit = panel.minField.getValue();
      double maxLimit = panel.maxField.getValue();
      double mouseAngle = getMouseAngleDeg(ev.getPoint());

      if (!panel.rangeBox.getState())
        return;
      valueVector1 = getValueVector(minLimit, ARM_R2);
      valueVector2 = getValueVector(maxLimit, ARM_R2);
      dragging1 = onHandle(valueVector1, mouseVector, HANDLE_R);
      dragging2 = onHandle(valueVector2, mouseVector, HANDLE_R);

      if (dragging1 && dragging2)
      {
        if (maxLimit-minLimit > 180)
          if (Math.abs(maxLimit-mouseAngle) < Math.abs(minLimit-mouseAngle))
            dragging1 = false;
          else
            dragging2 = false;
        else
          if (mouseAngle < maxLimit)
            dragging2 = false;
          else
            dragging1 = false;
      }
      if (dragging1 && minLimit == -180 && mouseAngle > 140)
        lastAngle = mouseAngle-360;
      if (dragging2 && maxLimit == 180 && mouseAngle < -140)
        lastAngle = mouseAngle+360;

      if (dragging1 || dragging2)
      {
        disableUpdating = true;
        return;
      }

      // comfort zones

      minLimit = panel.minComfortField.getValue();
      maxLimit = panel.maxComfortField.getValue();

      if (!panel.comfortBox.getState())
        return;
      valueVector1 = getValueVector(minLimit, ARM_R3);
      valueVector2 = getValueVector(maxLimit, ARM_R3);
      dragging3 = onHandle(valueVector1, mouseVector, HANDLE_R);
      dragging4 = onHandle(valueVector2, mouseVector, HANDLE_R);

      if (dragging3 && dragging4)
      {
        if (maxLimit-minLimit > 180)
          if (Math.abs(maxLimit-mouseAngle) < Math.abs(minLimit-mouseAngle))
            dragging3 = false;
          else
            dragging4 = false;
        else
          if (mouseAngle < maxLimit)
            dragging4 = false;
          else
            dragging3 = false;
      }
      if (dragging3 && minLimit == -180 && mouseAngle > 140)
        lastAngle = mouseAngle-360;
      if (dragging4 && maxLimit == 180 && mouseAngle < -140)
        lastAngle = mouseAngle+360;

      if (dragging3 || dragging4)
          disableUpdating = true;
      return;
    }

    private void mouseReleased(MouseReleasedEvent ev)
    {
      if (process != null)
        process.stopProcessing();
      process = null;
      dragging0 = dragging1 = dragging2 = dragging3 = dragging4 = false;
      disableUpdating = false;
    }

    private void mouseDragged(final MouseDraggedEvent ev)
    {
      process.addEvent(new Runnable() {
        @Override
        public void run()
        {
          dealWithDrag(ev);
        }
      });
    }

    /* Deal with a mouse dragged event. */

    private void dealWithDrag(MouseDraggedEvent ev)
    {
      if (!(dragging0 || dragging1 || dragging2 || dragging3 || dragging4))
        return;
      Point pos = ev.getPoint();

      double mouseAngle  = getMouseAngleDeg(ev.getPoint());      
      double angle = Math.rint(mouseAngle);

      if (panel.rangeBox.getState())
      {
        if (lastAngle-angle > 320)
          angle += 360.0;
        else if (angle-lastAngle > 320)
          angle -= 360.0;
      }

      double val = panel.dof.pos;
      boolean range = panel.rangeBox.getState();
      double minv = (range ? panel.dof.min : panel.min);
      double maxv = (range ? panel.dof.max : panel.max);
      double minComfort = panel.dof.minComfort;
      double maxComfort = panel.dof.maxComfort;
      if (!dragging1 && angle < minv)
        angle = minv;
      if (!dragging2 && angle > maxv)
        angle = maxv;
      if ((dragging1 && angle > val) || (dragging2 && angle < val))
        angle = val;
      if (dragging1 && angle < -180)
        angle = -180;
      if (dragging2 && angle > 180)
        angle = 180;  
      if (dragging3 && angle > maxComfort)
        angle = maxComfort;
      if (dragging4 && angle < minComfort)
        angle = minComfort;
      if (panel.dof.comfort)
      {
        if (dragging1 && angle > minComfort)
          angle = minComfort;
        if (dragging2 && angle < maxComfort)
          angle = maxComfort;
      }
      if (dragging0)
        panel.valField.setValue(angle);
      else if (dragging1)
        panel.minField.setValue(angle);
      else if (dragging2)
        panel.maxField.setValue(angle);
      else if (dragging3)
        panel.minComfortField.setValue(angle);
      else if (dragging4)
        panel.maxComfortField.setValue(angle);
      lastAngle = angle;
      panel.recordValues();
      repaint();
      valueChanged();
    }

    private void paint(RepaintEvent ev)
    {
      Vec2 arm;
      Graphics2D g = (Graphics2D)ev.getGraphics();
      
      g2.setTransform(atr_init);    
      g2.setBackground(new Color (0, 0, 0, 0));
      g2.clearRect(0, 0, GRAPH_SIZE, GRAPH_SIZE);
      g2.setTransform(atr_draw);    

      if (panel.fixedBox.getState())
      {
        // The angle is fixed, so just draw a line where it is.

        g2.setColor(dialIdle);
        g2.fill(dial);
        arm = getValueVector(panel.valField.getValue(), DIAL_R1);
        g2.setColor(valuColor3);
        g2.draw(new Line2D.Double(0, 0, arm.x, arm.y));
        g2.draw(dial);
        g.drawImage(image, 0, 0, null);
        return;
      }

      // Draw the background

      g2.setColor(dialActive);
      g2.fill(dial);
      g2.setColor(new Color(200, 200, 230));
      g2.draw(trace);
      for (Shape m : marker)
        g2.draw(m);

      int min = (int) (panel.rangeBox.getState() ? panel.minField.getValue() : panel.min);
      int max = (int) (panel.rangeBox.getState() ? panel.maxField.getValue() : panel.max);

      // Draw the restricted sector

      g2.setColor(restColor1);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.fill(new Arc2D.Double(-DIAL_R1, -DIAL_R1, DIAL_R1*2.0, DIAL_R1*2.0, min, -360+max-min, Arc2D.PIE));
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);

      // Draw the comfot zones

      if (panel.rangeBox.getState() && panel.comfortBox.getState())
      {
        int minComfort = (int) panel.minComfortField.getValue();
        int maxComfort = (int) panel.maxComfortField.getValue();
        g2.setColor(comfColor1);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
          g2.fill(new Arc2D.Double(-DIAL_R1, -DIAL_R1, DIAL_R1*2.0, DIAL_R1*2.0, minComfort, -minComfort+min, Arc2D.PIE));
          g2.fill(new Arc2D.Double(-DIAL_R1, -DIAL_R1, DIAL_R1*2.0, DIAL_R1*2.0, max, -max+maxComfort, Arc2D.PIE));
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        min = minComfort;
        max = maxComfort;
      }

      // Draw a line marking the current position, and handles for any draggable points.

      if (panel.rangeBox.getState())
      {
        // Restriction line
        g2.setColor(restColor1);
        arm = getValueVector(180, DIAL_R1);
        g2.draw(new Line2D.Double(0, 0, arm.x, arm.y));

        // comfort handles
        if (panel.comfortBox.getState())
        {
          g2.setColor(comfColor2);
          g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            arm = getValueVector(panel.minComfortField.getValue(), ARM_R3);
            g2.fill(new Ellipse2D.Double(arm.x-HANDLE_R, arm.y-HANDLE_R, HANDLE_R*2.0, HANDLE_R*2.0));
            arm = getValueVector(panel.maxComfortField.getValue(), ARM_R3);
            g2.fill(new Ellipse2D.Double(arm.x-HANDLE_R, arm.y-HANDLE_R, HANDLE_R*2.0, HANDLE_R*2.0));
          g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        }
        // restriction handles
        g2.setColor(restColor2);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
          arm = getValueVector(panel.minField.getValue(), ARM_R2);
          g2.fill(new Ellipse2D.Double(arm.x-HANDLE_R, arm.y-HANDLE_R, HANDLE_R*2.0, HANDLE_R*2.0));
          arm = getValueVector(panel.maxField.getValue(), ARM_R2);
          g2.fill(new Ellipse2D.Double(arm.x-HANDLE_R, arm.y-HANDLE_R, HANDLE_R*2.0, HANDLE_R*2.0));
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
      }
      
      // Draw the angle pointer
      
      arm = getValueVector(panel.valField.getValue(), ARM_R1);
      g2.setColor(valuColor1);
      g2.draw(new Line2D.Double(0, 0, arm.x, arm.y));
      arm = getValueVector(panel.valField.getValue(), ARM_R1);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(valuColor1);
        g2.fill(new Ellipse2D.Double(arm.x-HANDLE_R, arm.y-HANDLE_R, HANDLE_R*2.0, HANDLE_R*2.0));
        g2.fill(new Ellipse2D.Double(-2.5, -2.5, +5, +5));
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
      
      // Draw the edge of the dial
      
      g2.setColor(dialMark);
      g2.draw(dial);
      g.drawImage(image, 0, 0, null);
    }

    private Vec2 getValueVector(double degrees, double radius)
    {
      return new Vec2(Math.cos(Math.toRadians(degrees))*radius, -Math.sin(Math.toRadians(degrees))*radius);
    }

    private boolean onHandle(Vec2 handle, Vec2 mouse, double r)
    {
      return handle.distance(mouse) <= r;
    }

    private Vec2 getMouseVector(Point mousePoint)
    {
      return new Vec2(-mousePoint.y+centerPoint.y, -mousePoint.x+centerPoint.x);
    }

    private double getMouseAngleDeg(Point mousePoint)
    {
      return Math.toDegrees(Math.atan2(mousePoint.x-centerPoint.x, centerPoint.y-mousePoint.y));
    }
  }
}

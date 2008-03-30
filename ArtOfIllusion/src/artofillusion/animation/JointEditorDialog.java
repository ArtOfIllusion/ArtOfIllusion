/* Copyright (C) 2002-2004 by Peter Eastman

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
import artofillusion.animation.Joint.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
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
    content.add(nameRow, BorderContainer.NORTH, new LayoutInfo());
    FormContainer center = new FormContainer(4, 2);
    center.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(5, 5, 5, 5), null));
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
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.COPY_OBJECT, new Object [] {theMesh, oldMesh}));
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
  
  private class DOFPanel extends FormContainer
  {
    public ValueField valField, minField, maxField, minComfortField, maxComfortField;
    public ValueSlider stiffnessSlider;
    public BCheckBox fixedBox, rangeBox, comfortBox;
    public double min, max;
    public DOF dof;
    private DOFGraph graph;
    
    public DOFPanel(String name, double min, double max, DOF dof)
    {
      super(3, 7);
      this.min = min;
      this.max = max;
      this.dof = dof;

      // Create the components for the panel.
      
      valField = new ValueField(dof.pos, ValueField.NONE, 5);
      minField = new ValueField(dof.min, ValueField.NONE, 5);
      maxField = new ValueField(dof.max == Double.MAX_VALUE ? Double.NaN : dof.max, ValueField.NONE, 5);
      minComfortField = new ValueField(dof.minComfort, ValueField.NONE, 5);
      maxComfortField = new ValueField(dof.maxComfort == Double.MAX_VALUE ? Double.NaN : dof.maxComfort, ValueField.NONE, 5);
      valField.setValueChecker(new ValueChecker() {
        public boolean isValid(double val)
        {
          double lower = (rangeBox.getState() ? minField.getValue() : DOFPanel.this.min);
          double upper = (rangeBox.getState() ? maxField.getValue() : DOFPanel.this.max);
          return (val >= lower && val <= upper);
        }
      } );
      minField.setValueChecker(new ValueChecker() {
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
      stiffnessSlider = new ValueSlider(0.0, 1.0, 50, dof.stiffness);
      fixedBox = new BCheckBox(Translate.text("Lock"), dof.fixed);
      rangeBox = new BCheckBox(Translate.text("restrictTotalRange"), dof.min > min || dof.max < max || dof.comfort);
      comfortBox = new BCheckBox(Translate.text("restrictComfortRange"), dof.comfort);
      fixedBox.addEventLink(ValueChangedEvent.class, this, "checkboxChanged");
      rangeBox.addEventLink(ValueChangedEvent.class, this, "checkboxChanged");
      comfortBox.addEventLink(ValueChangedEvent.class, this, "checkboxChanged");
      
      // Lay them out.
      
      add(new BLabel(name), 0, 0);
      add(valField, 1, 0);
      add(fixedBox, 2, 0);
      add(rangeBox, 0, 1, 3, 1);
      add(minField, 0, 2);
      add(Translate.label("to"), 1, 2);
      add(maxField, 2, 2);
      add(comfortBox, 0, 3, 3, 1);
      add(minComfortField, 0, 4);
      add(Translate.label("to"), 1, 4);
      add(maxComfortField, 2, 4);
      add(new BLabel("Stiffness"), 0, 5, 3, 1);
      add(stiffnessSlider, 0, 6, 3, 1);
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
    private int dragging;
    private double lastAngle;
    private ActionProcessor process;
    
    private static final int SIZE = 64;
    private static final int RADIUS1 = 64;
    private static final int RADIUS2 = 58;
    private static final int RADIUS3 = 52;
    private static final int HANDLE_SIZE = 6;
    private static final int OFFSET1 = (SIZE-RADIUS1+HANDLE_SIZE)/2;
    private static final int OFFSET2 = (SIZE-RADIUS2+HANDLE_SIZE)/2;
    private static final int OFFSET3 = (SIZE-RADIUS3+HANDLE_SIZE)/2;
    
    public DOFGraph(DOFPanel dp)
    {
      panel = dp;
      dragging = -1;
      addEventLink(MousePressedEvent.class, this, "mousePressed");
      addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
      addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
      addEventLink(RepaintEvent.class, this, "paint");
      setPreferredSize(new Dimension(SIZE+2*OFFSET1, SIZE+2*OFFSET1));
    }
    
    private void mousePressed(MousePressedEvent ev)
    {
      process = new ActionProcessor();
      lastAngle = 0.0;
      if (panel.fixedBox.getState())
        return;
      Point c = ev.getPoint();
      Point v = getAnglePosition(panel.valField.getValue(), RADIUS1);
      if (clicked(c, v))
        {
          dragging = 0;
          disableUpdating = true;
          return;
        }
      if (!panel.rangeBox.getState())
        return;
      v = getAnglePosition(panel.maxField.getValue(), RADIUS2);
      if (clicked(c, v))
        dragging = 2;
      v = getAnglePosition(panel.minField.getValue(), RADIUS2);
      if (clicked(c, v))
        dragging = 1;
      if (dragging > -1)
        disableUpdating = true;
      if (!panel.comfortBox.getState())
        return;
      v = getAnglePosition(panel.maxComfortField.getValue(), RADIUS3);
      if (clicked(c, v))
        dragging = 4;
      v = getAnglePosition(panel.minComfortField.getValue(), RADIUS3);
      if (clicked(c, v))
        dragging = 3;
      if (dragging > -1)
        disableUpdating = true;
    }
    
    private void mouseReleased(MouseReleasedEvent ev)
    {
      if (process != null)
        process.stopProcessing();
      process = null;
      dragging = -1;
      disableUpdating = false;
    }
    
    private void mouseDragged(final MouseDraggedEvent ev)
    {
      process.addEvent(new Runnable() {
        public void run()
        {
          dealWithDrag(ev);
        }
      });
    }
    
    /* Deal with a mouse dragged event. */
    
    private void dealWithDrag(MouseDraggedEvent ev)
    {
      if (dragging == -1)
        return;
      Point pos = ev.getPoint();
      double angle = Math.atan2(pos.x-OFFSET1-SIZE/2, OFFSET1+SIZE/2-pos.y)*180.0/Math.PI;
      angle = Math.rint(angle);
      if (lastAngle-angle > 270.0)
        angle += 360.0;
      else if (angle-lastAngle > 270.0)
        angle -= 360.0;
      double val = panel.dof.pos;
      boolean range = panel.rangeBox.getState();
      double minv = (range ? panel.dof.min : panel.min);
      double maxv = (range ? panel.dof.max : panel.max);
      double minComfort = panel.dof.minComfort;
      double maxComfort = panel.dof.maxComfort;
      if (dragging != 1 && angle < minv)
        angle = minv;
      if (dragging != 2 && angle > maxv)
        angle = maxv;
      if ((dragging == 1 && angle > val) || (dragging == 2 && angle < val))
        angle = val;
      if (dragging == 3 && angle > maxComfort)
        angle = maxComfort;
      if (dragging == 4 && angle < minComfort)
        angle = minComfort;
      if (panel.dof.comfort)
      {
        if (dragging == 1 && angle > minComfort)
          angle = minComfort;
        if (dragging == 2 && angle < maxComfort)
          angle = maxComfort;
      }
      if (dragging == 0)
        panel.valField.setValue(angle);
      else if (dragging == 1)
        panel.minField.setValue(angle);
      else if (dragging == 2)
        panel.maxField.setValue(angle);
      else if (dragging == 3)
        panel.minComfortField.setValue(angle);
      else if (dragging == 4)
        panel.maxComfortField.setValue(angle);
      lastAngle = angle;
      panel.recordValues();
      repaint();
      valueChanged();
    }
    
    private void paint(RepaintEvent ev)
    {
      Graphics g = ev.getGraphics();
      if (panel.fixedBox.getState())
      {
        // The angle is fixed, so just draw a line where it is.
        
        g.setColor(Color.lightGray);
        g.fillOval(0, 0, SIZE+OFFSET1, SIZE+OFFSET1);
        g.setColor(Color.black);
        Point p = getAnglePosition(panel.valField.getValue(), RADIUS1);
        g.drawLine(SIZE/2+OFFSET1, SIZE/2+OFFSET1, p.x, p.y);
        return;
      }
      
      // Draw the various arcs.
      
      g.setColor(Color.white);
      g.fillOval(OFFSET1, OFFSET1, RADIUS1, RADIUS1);
      int min = (int) (panel.rangeBox.getState() ? panel.minField.getValue() : panel.min);
      int max = (int) (panel.rangeBox.getState() ? panel.maxField.getValue() : panel.max);
      g.setColor(Color.black);
      g.fillArc(OFFSET2, OFFSET2, RADIUS2, RADIUS2, 90-min, 360-max+min);
      if (panel.rangeBox.getState() && panel.comfortBox.getState())
      {
        g.setColor(Color.lightGray);
        int minComfort = (int) panel.minComfortField.getValue();
        int maxComfort = (int) panel.maxComfortField.getValue();
        g.fillArc(OFFSET3, OFFSET3, RADIUS3, RADIUS3, 90-minComfort, minComfort-min);
        g.fillArc(OFFSET3, OFFSET3, RADIUS3, RADIUS3, 90-max, max-maxComfort);
        min = minComfort;
        max = maxComfort;
        g.setColor(Color.black);
      }
      g.drawOval(OFFSET1, OFFSET1, RADIUS1, RADIUS1);
      
      // Draw a line marking the current position, and handles for any draggable points.
      
      Point p = getAnglePosition(panel.valField.getValue(), RADIUS1);
      g.drawLine(SIZE/2+OFFSET1, SIZE/2+OFFSET1, p.x, p.y);
      g.fillRect(p.x-OFFSET1, p.y-OFFSET1, HANDLE_SIZE, HANDLE_SIZE);
      if (panel.rangeBox.getState())
      {
        p = getAnglePosition(panel.minField.getValue(), RADIUS2);
        g.fillRect(p.x-OFFSET1, p.y-OFFSET1, HANDLE_SIZE, HANDLE_SIZE);
        p = getAnglePosition(panel.maxField.getValue(), RADIUS2);
        g.fillRect(p.x-OFFSET1, p.y-OFFSET1, HANDLE_SIZE, HANDLE_SIZE);
      }
      if (panel.rangeBox.getState() && panel.comfortBox.getState())
      {
        p = getAnglePosition(panel.minComfortField.getValue(), RADIUS3);
        g.fillRect(p.x-OFFSET1, p.y-OFFSET1, HANDLE_SIZE, HANDLE_SIZE);
        p = getAnglePosition(panel.maxComfortField.getValue(), RADIUS3);
        g.fillRect(p.x-OFFSET1, p.y-OFFSET1, HANDLE_SIZE, HANDLE_SIZE);
      }
    }
    
    /* Find the point on the circumference corresponding to a particular angle. */
    
    private Point getAnglePosition(double angle, int radius)
    {
      return new Point((int) (0.5*(RADIUS1+radius*Math.sin(angle*Math.PI/180.0)))+OFFSET1, 
        (int) (0.5*(RADIUS1-radius*Math.cos(angle*Math.PI/180.0)))+OFFSET1);
    }
    
    /* Return true if the second point is within the required distance of the first point. */
    
    public boolean clicked(Point clickPos, Point targetPos)
    {
      return (clickPos.x >= targetPos.x-OFFSET1 && clickPos.x <= targetPos.x+OFFSET1 && 
        clickPos.y >= targetPos.y-OFFSET1 && clickPos.y <= targetPos.y+OFFSET1);
    }
  }
}

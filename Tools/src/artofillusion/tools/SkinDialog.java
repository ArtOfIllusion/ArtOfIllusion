/* Copyright (C) 2001-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tools;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.Vector;

/** This dialog box allows the user to specify options for creating skinned objects. */

public class SkinDialog extends BDialog
{
  private LayoutWindow window;
  private BList curveList;
  private BCheckBox reverseBox;
  private BButton upButton, downButton, okButton, cancelButton;
  private ObjectPreviewCanvas preview;
  private ObjectInfo curve[];
  private boolean reverse[];
  private Vec3 centerOffset;

  private static int counter = 1;

  public SkinDialog(LayoutWindow window, Vector curves)
  {
    super(window, "Skin", true);
    this.window = window;
    curve = new ObjectInfo [curves.size()];
    reverse = new boolean [curves.size()];

    // Layout the window.
    
    FormContainer content = new FormContainer(3, 2);
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    content.add(UIUtilities.createScrollingList(curveList = new BList()), 0, 0);
    for (int i = 0; i < curves.size(); i++)
    {
      curve[i] = (ObjectInfo) curves.elementAt(i);
      curveList.add(curve[i].getName());
    }
    curveList.setMultipleSelectionEnabled(false);
    curveList.setSelected(0, true);
    curveList.addEventLink(SelectionChangedEvent.class, this, "selectionChanged");
    ColumnContainer col = new ColumnContainer();
    content.add(col, 1, 0);
    col.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets(2, 2, 2, 2), null));
    col.add(upButton = new BButton("Move Up"));
    upButton.addEventLink(CommandEvent.class, this, "doMoveUp");
    col.add(downButton = new BButton("Move Down"));
    downButton.addEventLink(CommandEvent.class, this, "doMoveDown");
    col.add(reverseBox = new BCheckBox("Reverse Direction", false));
    reverseBox.addEventLink(ValueChangedEvent.class, this, "selectionChanged");
    content.add(preview = new ObjectPreviewCanvas(null), 2, 0);
    preview.setPreferredSize(new Dimension(150, 150));
    RowContainer buttons = new RowContainer();
    content.add(buttons, 0, 1, 3, 1, new LayoutInfo());
    buttons.add(okButton = Translate.button("ok", this, "doOk"));
    buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
    makeObject();
    pack();
    UIUtilities.centerDialog(this, window);
    updateComponents();
    setVisible(true);
  }
  
  private void selectionChanged(WidgetEvent e)
  {
    if (e.getWidget() == reverseBox)
      reverse[curveList.getSelectedIndex()] = reverseBox.getState();
    makeObject();
    updateComponents();
  }
  
  // Enable or disable components, based on the current selections.
  
  private void updateComponents()
  {
    int which = curveList.getSelectedIndex();
    
    upButton.setEnabled(which > 0);
    downButton.setEnabled(which < curve.length-1);
    reverseBox.setState(which > -1 && reverse[which]);
  }
  
  private void doOk()
  {
    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    coords.setOrigin(coords.getOrigin().plus(centerOffset));
    window.addObject(preview.getObject().getObject(), coords, "Skinned Object "+(counter++), null);
    window.setSelection(window.getScene().getNumObjects()-1);
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
    window.updateImage();
    dispose();
  }
  
  private void doMoveUp()
  {
    int which = curveList.getSelectedIndex();
    ObjectInfo swap1 = curve[which];
    curve[which] = curve[which-1];
    curve[which-1] = swap1;
    boolean swap2 = reverse[which];
    reverse[which] = reverse[which-1];
    reverse[which-1] = swap2;
    Object swap3 = curveList.getItem(which);
    curveList.remove(which);
    curveList.add(which-1, swap3);
    curveList.setSelected(which-1, true);
    makeObject();
    updateComponents();
  }
  
  private void doMoveDown()
  {
    int which = curveList.getSelectedIndex();
    ObjectInfo swap1 = curve[which];
    curve[which] = curve[which+1];
    curve[which+1] = swap1;
    boolean swap2 = reverse[which];
    reverse[which] = reverse[which+1];
    reverse[which+1] = swap2;
    Object swap3 = curveList.getItem(which);
    curveList.remove(which);
    curveList.add(which+1, swap3);
    curveList.setSelected(which+1, true);
    makeObject();
    updateComponents();
  }
  
  // Create the skinned object.
  
  private void makeObject()
  {
    Vec3 v[][] = new Vec3 [curve.length][], center = new Vec3();
    float us[] = new float [curve.length], vs[] = new float [((Curve) curve[0].getObject()).getVertices().length];
    int smoothMethod = Mesh.INTERPOLATING;
    boolean closed = false;
    
    for (int i = 0; i < curve.length; i++)
      {
        Curve cv = (Curve) curve[i].getObject();
        MeshVertex vert[] = cv.getVertices();
        v[i] = new Vec3 [vert.length];
        float smooth[] = cv.getSmoothness();
        if (cv.getSmoothingMethod() > smoothMethod)
          smoothMethod = cv.getSmoothingMethod();
        closed |= cv.isClosed();
        for (int j = 0; j < vert.length; j++)
          {
            int k = (reverse[i] ? vert.length-j-1 : j);
            v[i][j] = curve[i].getCoords().fromLocal().times(vert[k].r);
            center.add(v[i][j]);
            if (cv.getSmoothingMethod() != Mesh.NO_SMOOTHING)
              vs[j] += smooth[k];
          }
        us[i] = 1.0f;
      }
    for (int i = 0; i < vs.length; i++)
      vs[i] /= curve.length;
    
    // Center it.
    
    center.scale(1.0/(v.length*v[0].length));
    for (int i = 0; i < v.length; i++)
      for (int j = 0; j < v[i].length; j++)
        v[i][j].subtract(center);
    centerOffset = center;
    SplineMesh mesh = new SplineMesh(v, us, vs, smoothMethod, false, closed);
    Texture tex = window.getScene().getDefaultTexture();
    mesh.setTexture(tex, tex.getDefaultMapping(mesh));
    mesh.makeRightSideOut();
    preview.setObject(mesh);
    preview.repaint();
  }
}
/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.*;
import artofillusion.math.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;
import java.util.prefs.*;

/** ColorChooser is a BDialog in which the user can edit an RGBColor object.  It allows the
    color to be specified using the RGB, HSV, or HLS color models. */

public class ColorChooser extends BDialog
{
  private RGBColor oldColor, newColor;
  private ValueSlider slider1, slider2, slider3;
  private BLabel label1, label2, label3;
  private Widget oldColorPatch, newColorPatch;
  private BComboBox modeC, rangeC;
  
  private static final int RECENT_COLOR_COUNT = 15;
  private static ArrayList<RGBColor> recentColors;
  private static int mode;
  private static int rangeMode;

  static
  {
    try
    {
      mode = Preferences.userNodeForPackage(ColorChooser.class).getInt("defaultColorModel", 1);
      rangeMode = Preferences.userNodeForPackage(ColorChooser.class).getInt("defaultColorRange", 0);
    }
    catch (Exception ex)
    {
      mode = 1;
      rangeMode = 0;
    }
    recentColors = new ArrayList<RGBColor>();
    for (int i = 0; i < RECENT_COLOR_COUNT; i++)
      recentColors.add(new RGBColor(1.0, 1.0, 1.0));
  }
  
  public ColorChooser(BFrame parent, String title, RGBColor c)
  {
    super(parent, title, true);
    BorderContainer content = new BorderContainer();
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    oldColor = c;
    newColor = c.duplicate();

    // Add the buttons at the bottom.

    RowContainer buttons = new RowContainer();
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "dispose"));
    content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());

    // Add the main panel.

    FormContainer center = new FormContainer(new double [] {0, 1, 0}, new double [] {1, 1, 1, 1, 1, 1, 1, 1, 1});
    center.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.HORIZONTAL, null, null));
    content.add(center, BorderContainer.CENTER);
    center.add(label1 = Translate.label("Red"), 0, 0, 2, 1);
    center.add(label2 = Translate.label("Green"), 0, 2, 2, 1);
    center.add(label3 = Translate.label("Blue"), 0, 4, 2, 1);
    LayoutInfo sliderLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.HORIZONTAL, new Insets(0, 0, 0, 5), null);
    double componentMax = (rangeMode == 0 ? 1.0 : 255.0);
    center.add(slider1 = new ValueSlider(0.0, componentMax, 100, (double) newColor.getRed()), 0, 1, 2, 1, sliderLayout);
    center.add(slider2 = new ValueSlider(0.0, componentMax, 100, (double) newColor.getGreen()), 0, 3, 2, 1, sliderLayout);
    center.add(slider3 = new ValueSlider(0.0, componentMax, 100, (double) newColor.getBlue()), 0, 5, 2, 1, sliderLayout);
    slider1.addEventLink(ValueChangedEvent.class, this, "valueChanged");
    slider2.addEventLink(ValueChangedEvent.class, this, "valueChanged");
    slider3.addEventLink(ValueChangedEvent.class, this, "valueChanged");
    RowContainer choicesRow = new RowContainer();
    center.add(choicesRow, 0, 6, 3, 1);
    choicesRow.add(Translate.label("colorModel"));
    choicesRow.add(modeC = new BComboBox(new String [] {"RGB", "HSV", "HLS"}), new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 8), null));
    choicesRow.add(Translate.label("colorComponentRange"));
    modeC.addEventLink(ValueChangedEvent.class, this, "modeChanged");
    modeC.setSelectedIndex(mode);
    choicesRow.add(rangeC = new BComboBox(new String [] {"0 to 1", "0 to 255"}), new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
    rangeC.addEventLink(ValueChangedEvent.class, this, "rangeChanged");
    rangeC.setSelectedIndex(rangeMode);
    LayoutInfo patchLayout = new LayoutInfo();
    center.add(Translate.label("originalColor"), 2, 0, patchLayout);
    center.add(oldColorPatch = oldColor.getSample(50, 30), 2, 1, patchLayout);
    center.add(Translate.label("newColor"), 2, 2, patchLayout);
    center.add(newColorPatch = newColor.getSample(50, 30), 2, 3, patchLayout);
    center.add(Translate.label("recentColors"), 0, 7, 3, 1);
    RowContainer recentColorRow = new RowContainer();
    center.add(recentColorRow, 0, 8, 3, 1);
    for (int i = 0; i < recentColors.size(); i++)
    {
      final RGBColor color = ((RGBColor) recentColors.get(i));
      Widget sample = color.getSample(16, 16);
      recentColorRow.add(sample);
      sample.addEventLink(MousePressedEvent.class, new Object() {
        void processEvent()
        {
          setColor(color);
        }
      });
    }
    addAsListener(this);
    modeChanged();
    pack();
    setResizable(false);
    UIUtilities.centerDialog(this, parent);
    setVisible(true);
  }

  /** Add this as a key listener to every component. */
  
  private void addAsListener(Widget w)
  {
    w.addEventLink(KeyPressedEvent.class, this, "keyPressed");
    if (w instanceof WidgetContainer)
      {
        for (Widget child : ((WidgetContainer) w).getChildren())
          addAsListener(child);
      }
  }

  private void doOk()
  {
    oldColor.setRGB(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
    for (int i = 0; i < recentColors.size(); i++)
      if (recentColors.get(i).equals(newColor))
      {
        recentColors.remove(i);
        break;
      }
    if (recentColors.size() == RECENT_COLOR_COUNT)
      recentColors.remove(recentColors.size()-1);
    recentColors.add(0, newColor.duplicate());
    dispose();
  }

  private void valueChanged()
  {
    float values[] = new float [] {(float) slider1.getValue(), (float) slider2.getValue(), (float) slider3.getValue()};
    if (rangeMode == 1)
    {
      values[0] /= 255.0;
      values[1] /= 255.0;
      values[2] /= 255.0;
    }
    if (mode == 0)
      newColor.setRGB(values[0], values[1], values[2]);
    else if (mode == 1)
      newColor.setHSV(values[0]*360.0f, values[1], values[2]);
    else
      newColor.setHLS(values[0]*360.0f, values[1], values[2]);
    newColorPatch.setBackground(newColor.getColor());
    newColorPatch.repaint();
  }

  private void setColor(RGBColor color)
  {
    newColor.copy(color);
    newColorPatch.setBackground(newColor.getColor());
    newColorPatch.repaint();
    modeChanged();
  }

  /** Pressing Return and Escape are equivalent to clicking OK and Cancel. */
    
  private void keyPressed(KeyPressedEvent ev)
  {
    int code = ev.getKeyCode();

    if (code != KeyPressedEvent.VK_ENTER && code != KeyPressedEvent.VK_ESCAPE)
      return;
    if (code == KeyPressedEvent.VK_ENTER && ev.getWidget() instanceof BButton)
      return;
    if (code == KeyPressedEvent.VK_ENTER)
      oldColor.setRGB(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
    dispose();
  }
  
  private void modeChanged()
  {
    float values[];
    mode = modeC.getSelectedIndex();
    if (mode == 0)
      {
        label1.setText(Translate.text("Red"));
        label2.setText(Translate.text("Green"));
        label3.setText(Translate.text("Blue"));
        values = new float [] {newColor.getRed(), newColor.getGreen(), newColor.getBlue()};
      }
    else if (mode == 1)
      {
        label1.setText(Translate.text("Hue"));
        label2.setText(Translate.text("Saturation"));
        label3.setText(Translate.text("Value"));
        values = newColor.getHSV();
        values[0] /= 360.0;
      }
    else
      {
        label1.setText(Translate.text("Hue"));
        label2.setText(Translate.text("Lightness"));
        label3.setText(Translate.text("Saturation"));
        values = newColor.getHLS();
        values[0] /= 360.0;
      }
    double scale = (rangeMode == 0 ? 1.0 : 255.0);
    slider1.setValue(scale*values[0]);
    slider2.setValue(scale*values[1]);
    slider3.setValue(scale*values[2]);
    Preferences.userNodeForPackage(ColorChooser.class).putInt("defaultColorModel", mode);
  }

  private void rangeChanged()
  {
    rangeMode = rangeC.getSelectedIndex();
    double componentMax = (rangeMode == 0 ? 1.0 : 255.0);
    slider1.setMaximumValue(componentMax);
    slider2.setMaximumValue(componentMax);
    slider3.setMaximumValue(componentMax);
    Preferences.userNodeForPackage(ColorChooser.class).putInt("defaultColorRange", rangeMode);
    modeChanged();
  }
}

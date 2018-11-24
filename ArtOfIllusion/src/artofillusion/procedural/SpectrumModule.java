/* Copyright (C) 2000-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a Module which implements an arbitrary mapping of numbers to colors. */

public class SpectrumModule extends ProceduralModule
{
  RGBColor color[], outputColor;
  float a1[][], b1[][], c1[][];
  double index[], lastBlur;
  boolean repeat, colorOk;

  public SpectrumModule(Point position)
  {
    super("Spectrum", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Index", "(0)"})},
      new IOPort [] {new IOPort(IOPort.COLOR, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Color"})},
      position);
    color = new RGBColor [] {new RGBColor(0.0f, 0.0f, 0.0f), new RGBColor(1.0f, 1.0f, 1.0f)};
    index = new double [] {0.0, 1.0};
    outputColor = new RGBColor(0.0f, 0.0f, 0.0f);
    calcCoefficients();
  }

  /** Get the list of colors in the table. */

  public RGBColor [] getColors()
  {
    return color;
  }

  /** Get the list of input values corresponding to the colors in the table. */

  public double [] getColorPositions()
  {
    return index;
  }

  /** Set the color table
      @param color     the list of colors
      @param position  the list of input values corresponding to the colors.  These must be between
                       0 and 1, and be in increasing order
  */

  public void setColors(RGBColor color[], double position[])
  {
    this.color = color;
    index = position;
    calcCoefficients();
  }

  /** Get whether the colors should repeat outside the range [0,1]. */

  public boolean getRepeat()
  {
    return repeat;
  }

  /** Set whether the colors should repeat outside the range [0,1]. */

  public void setRepeat(boolean repeat)
  {
    this.repeat = repeat;
  }

  /* New point, so the color will need to be recalculated. */

  @Override
  public void init(PointInfo p)
  {
    colorOk = false;
  }

  /* Calculate coefficients which will be needed for integrating the color table. */

  private void calcCoefficients()
  {
    int i;

    a1 = new float [color.length][3];
    b1 = new float [color.length][3];
    c1 = new float [color.length][3];

    for (i = 0; i < color.length-1; i++)
      {
        float d = (float) (index[i+1]-index[i]);
        if (d == 0.0f)
          a1[i][0] = a1[i][1] = a1[i][2] = b1[i][0] = b1[i][1] = b1[i][2] = 0.0f;
        else
          {
            a1[i][0] = 0.5f*(color[i+1].getRed()-color[i].getRed())/d;
            a1[i][1] = 0.5f*(color[i+1].getGreen()-color[i].getGreen())/d;
            a1[i][2] = 0.5f*(color[i+1].getBlue()-color[i].getBlue())/d;
            b1[i][0] = color[i].getRed()-2.0f*a1[i][0]*((float) index[i]);
            b1[i][1] = color[i].getGreen()-2.0f*a1[i][1]*((float) index[i]);
            b1[i][2] = color[i].getBlue()-2.0f*a1[i][2]*((float) index[i]);
            c1[i+1][0] = (float) (c1[i][0] + index[i+1]*(a1[i][0]*index[i+1]+b1[i][0]) - index[i]*(a1[i][0]*index[i]+b1[i][0]));
            c1[i+1][1] = (float) (c1[i][1] + index[i+1]*(a1[i][1]*index[i+1]+b1[i][1]) - index[i]*(a1[i][1]*index[i]+b1[i][1]));
            c1[i+1][2] = (float) (c1[i][2] + index[i+1]*(a1[i][2]*index[i+1]+b1[i][2]) - index[i]*(a1[i][2]*index[i]+b1[i][2]));
          }
      }
    if (!repeat)
      {
        b1[i][0] = color[i].getRed();
        b1[i][1] = color[i].getGreen();
        b1[i][2] = color[i].getBlue();
      }
    for (i = 1; i < color.length; i++)
      {
        c1[i][0] -= (float) (index[i]*(a1[i][0]*index[i]+b1[i][0]));
        c1[i][1] -= (float) (index[i]*(a1[i][1]*index[i]+b1[i][1]));
        c1[i][2] -= (float) (index[i]*(a1[i][2]*index[i]+b1[i][2]));
      }
   }

  /* Calculate the color corresponding to a given input value. */

  private void calcColor(double value, RGBColor c)
  {
    if (value <= 0.0 || value >= 1.0)
      {
        if (repeat)
          value -= FastMath.floor(value);
        else if (value <= 0.0)
          {
            c.copy(color[0]);
            return;
          }
        else
          {
            c.copy(color[color.length-1]);
            return;
          }
      }
      int i, i1, i2;
      for (i = 1; i < index.length && value > index[i]; i++);
      i1 = i-1;
      i2 = i;
      float d = (float) (index[i2]-index[i1]);
      float fract = d > 0.0f ? (float) (value-index[i1])/d : 0.0f;
      float fract2 = 1.0f-fract;
      c.setRGB(fract*color[i2].getRed() + fract2*color[i1].getRed(),
          fract*color[i2].getGreen() + fract2*color[i1].getGreen(),
          fract*color[i2].getBlue() + fract2*color[i1].getBlue());
  }

  /* Calculate the integral of the color at a point. */

  private void integrateColor(double value, RGBColor c)
  {
    float vi, vf;
    int i;

    if (!repeat)
      {
        if (value <= 0.0)
          {
            c.setRGB((float) value*b1[0][0], (float) value*b1[0][1], (float) value*b1[0][2]);
            return;
          }
        if (value >= 1.0)
          {
            i = color.length-1;
            c.setRGB(c1[i][0]+(float) value*b1[i][0], c1[i][1]+(float) value*b1[i][1], c1[i][2]+(float) value*b1[i][2]);
            return;
          }
        vf = (float) value;
        c.setRGB(0.0f, 0.0f, 0.0f);
      }
    else
      {
        vi = (float) FastMath.floor(value);
        vf = (float) value-vi;
        c.setRGB(vi*c1[color.length-1][0], vi*c1[color.length-1][1], vi*c1[color.length-1][2]);
      }
    if (vf == 0.0)
      return;
    for (i = 1; i < index.length && vf > index[i]; i++);
    i--;
    c.add(c1[i][0]+vf*(vf*a1[i][0]+b1[i][0]), c1[i][1]+vf*(vf*a1[i][1]+b1[i][1]), c1[i][2]+vf*(vf*a1[i][2]+b1[i][2]));
  }

  /* Calculate the output color. */

  @Override
  public void getColor(int which, RGBColor c, double blur)
  {
    if (colorOk && blur == lastBlur)
      {
        c.copy(outputColor);
        return;
      }
    colorOk = true;
    lastBlur = blur;
    double value = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double error = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
    if (error == 0.0)
      calcColor(value, c);
    else
      {
        integrateColor(value+0.5*error, c);
        integrateColor(value-0.5*error, outputColor);
        c.subtract(outputColor);
        c.scale(1.0/error);
        outputColor.copy(c);
      }
  }

  /* Create a duplicate of this module. */

  @Override
  public Module duplicate()
  {
    SpectrumModule mod = new SpectrumModule(new Point(bounds.x, bounds.y));

    mod.repeat = repeat;
    mod.color = new RGBColor [color.length];
    mod.index = new double [index.length];
    for (int i = 0; i < color.length; i++)
      {
        mod.color[i] = color[i].duplicate();
        mod.index[i] = index[i];
      }
    mod.calcCoefficients();
    return mod;
  }

  /* Write out the parameters. */

  @Override
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(color.length);
    for (int i = 0; i < color.length; i++)
      {
        out.writeDouble(index[i]);
        color[i].writeToFile(out);
      }
    out.writeBoolean(repeat);
  }

  /* Read in the parameters. */

  @Override
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    int num = in.readInt();
    color = new RGBColor [num];
    index = new double [num];
    for (int i = 0; i < color.length; i++)
      {
        index[i] = in.readDouble();
        color[i] = new RGBColor(in);
      }
    repeat = in.readBoolean();
    calcCoefficients();
  }

  /* Calculate the size of the module. */

  @Override
  public void calcSize()
  {
    bounds.width = 40+IOPort.SIZE*2;
    bounds.height = 20+IOPort.SIZE*2;
  }

  @Override
  protected void drawContents(Graphics2D g)
  {
    int x1 = bounds.x+IOPort.SIZE, y1 = bounds.y+IOPort.SIZE;
    RGBColor temp = new RGBColor(0.0f, 0.0f, 0.0f);

    for (int i = 0; i < 40; i++)
      {
        calcColor(i/40.0, temp);
        g.setColor(temp.getColor());
        g.drawLine(x1+i, y1, x1+i, y1+20);
      }
  }

  /* Allow the user to set the parameters. */

  @Override
  public boolean edit(ProcedureEditor editor, Scene theScene)
  {
    EditingDialog dlg = new EditingDialog(editor);
    calcCoefficients();
    return dlg.clickedOk;
  }

  /** Inner class used for editing the list of colors. */

  private class EditingDialog extends BDialog
  {
    ProcedureEditor editor;
    CustomWidget canvas;
    ValueField indexField;
    Widget preview;
    BCheckBox repeatBox;
    BButton deleteButton;
    Point clickPoint, handlePos[];
    int selected, rows = 1;
    boolean clickedOk;

    static final int HANDLE_SIZE = 5;
    static final int INSET = 3;

    public EditingDialog(ProcedureEditor editor)
    {
      super(editor.getParentFrame(), "Function", true);
      this.editor = editor;
      FormContainer content = new FormContainer(1, 5);
      setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
      content.add(Translate.label("functionModuleInstructions"), 0, 0);
      content.add(canvas = new CustomWidget(), 0, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
      canvas.setPreferredSize(new Dimension (200, 30+rows*HANDLE_SIZE));
      canvas.addEventLink(RepaintEvent.class, this, "paintCanvas");
      canvas.addEventLink(KeyPressedEvent.class, this, "keyPressed");
      canvas.addEventLink(MousePressedEvent.class, this, "mousePressed");
      canvas.addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
      canvas.addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
      RowContainer row = new RowContainer();
      content.add(row, 0, 2);
      row.add(new BLabel(Translate.text("Value")+":"));
      row.add(indexField = new ValueField(Double.NaN, ValueField.NONE));
      indexField.setValueChecker(new ValueChecker() {
        @Override
        public boolean isValid(double val)
        {
          return (val >= 0.0 && val <= 1.0);
        }
      });
      indexField.addEventLink(ValueChangedEvent.class, this, "indexChanged");
      row.add(new BLabel(Translate.text("Color")+":"));
      row.add(preview = color[0].getSample(30, 20));
      preview.addEventLink(MouseClickedEvent.class, this, "selectColor");
      row.add(Translate.button("add", this, "doAdd"));
      row.add(deleteButton = Translate.button("delete", this, "doDelete"));
      content.add(repeatBox = new BCheckBox(Translate.text("functionIsPeriodic"), repeat), 0, 3);
      repeatBox.addEventLink(ValueChangedEvent.class, this, "repeatChanged");
      RowContainer buttons = new RowContainer();
      content.add(buttons, 0, 4);
      buttons.add(Translate.button("ok", this, "doOk"));
      buttons.add(Translate.button("cancel", this, "doCancel"));
      adjustComponents();
      handlePos = new Point [index.length];
      for (int i = 0; i < index.length; i++)
        handlePos[i] = new Point(0, 0);
      pack();
      UIUtilities.centerDialog(this, editor.getParentFrame());
      setVisible(true);
    }

    /* Adjust the various components in the window based on the currently selected point. */

    private void adjustComponents()
    {
      indexField.setText(Double.toString(index[selected]));
      preview.setBackground(color[selected].getColor());
      preview.repaint();
      boolean movable = (selected > 0 && selected < index.length-1);
      indexField.setEnabled(movable);
      deleteButton.setEnabled(movable);
    }

    /* Calculate the heights of all the handles. */

    private void positionHandles()
    {
      Rectangle bounds = canvas.getBounds();
      int oldrows = rows;

      bounds.x += INSET;
      bounds.width -= 2*INSET;
      for (int i = 0; i < index.length; i++)
        handlePos[i].x = (int) (bounds.x+index[i]*bounds.width);
      if (clickPoint != null)
        return;
      rows = 1;
      for (int i = 1; i < index.length; i++)
        {
          int row = 0;
          boolean overlap = true;
          while (overlap)
            {
              overlap = false;
              int j;
              for (j = 0; j < i && (row != handlePos[j].y || handlePos[i].x-handlePos[j].x > HANDLE_SIZE); j++);
              if (j < i)
                {
                  row++;
                  overlap = true;
                }
            }
          handlePos[i].y = row;
          if (row >= rows)
            rows = row+1;
        }
      if (oldrows != rows)
        pack();
    }

    /* Draw the canvas. */

    private void paintCanvas(RepaintEvent ev)
    {
      Graphics2D g = ev.getGraphics();
      Rectangle bounds = canvas.getBounds();
      bounds.x += INSET;
      bounds.width -= 2*INSET;
      RGBColor temp = new RGBColor(0.0f, 0.0f, 0.0f);
      double scale = 1.0/(bounds.width);
      for (int i = 0; i < bounds.width; i++)
      {
        calcColor(i*scale, temp);
        g.setColor(temp.getColor());
        g.drawLine(bounds.x+i, 0, bounds.x+i, 30);
      }
      g.setColor(getBackground());
      g.fillRect(0, 30, bounds.width, bounds.height);
      g.setColor(Color.black);
      positionHandles();
      for (int i = 0; i < index.length; i++)
      {
        if (i == selected)
          g.setColor(Color.red);
        int x = handlePos[i].x, y = 30+handlePos[i].y*HANDLE_SIZE;
        g.fillPolygon(new int [] {x, x+HANDLE_SIZE, x-HANDLE_SIZE}, new int [] {y, y+HANDLE_SIZE, y+HANDLE_SIZE}, 3);
        g.setColor(Color.black);
        g.drawLine(x, y, x, 30);
      }
    }

    /* Add a new handle at the specified position. */

    private void addHandle(double where)
    {
      double newindex[] = new double [index.length+1];
      RGBColor newcolor[] = new RGBColor [color.length+1];
      int i;

      for (i = 0; i < index.length && index[i] < where; i++)
      {
        newindex[i] = index[i];
        newcolor[i] = color[i];
      }
      newindex[i] = where;
      newcolor[i] = new RGBColor(1.0f, 1.0f, 1.0f);
      calcColor(newindex[i], newcolor[i]);
      selected = i;
      for (; i < index.length; i++)
      {
        newindex[i+1] = index[i];
        newcolor[i+1] = color[i];
      }
      index = newindex;
      color = newcolor;
      handlePos = new Point [index.length+1];
      for (i = 0; i < handlePos.length; i++)
        handlePos[i] = new Point(0, 0);
      calcCoefficients();
      adjustComponents();
      canvas.repaint();
      editor.updatePreview();
    }

    /* Delete the currently selected handle. */

    private void doDelete()
    {
      if (selected == 0 || selected == index.length-1)
        return;
      double newindex[] = new double [index.length-1];
      RGBColor newcolor[] = new RGBColor [color.length-1];
      int i;

      for (i = 0; i < index.length-1; i++)
        {
          if (i < selected)
            {
              newindex[i] = index[i];
              newcolor[i] = color[i];
            }
          else
            {
              newindex[i] = index[i+1];
              newcolor[i] = color[i+1];
            }
        }
      selected = 0;
      index = newindex;
      color = newcolor;
      handlePos = new Point [index.length];
      for (i = 0; i < handlePos.length; i++)
        handlePos[i] = new Point(0, 0);
      calcCoefficients();
      adjustComponents();
      canvas.repaint();
      editor.updatePreview();
    }

    private void doAdd()
    {
      addHandle(0.5);
    }

    private void doOk()
    {
      clickedOk = true;
      dispose();
    }

    private void doCancel()
    {
      dispose();
    }

    /* Respond to keypresses. */

    private void keyPressed(KeyPressedEvent ev)
    {
      if (ev.getKeyCode() == KeyPressedEvent.VK_ENTER)
        doOk();
      else if (ev.getKeyCode() == KeyPressedEvent.VK_ESCAPE)
        doCancel();
      if (ev.getWidget() != canvas)
        return;
      if (ev.getKeyCode() == KeyPressedEvent.VK_BACK_SPACE || ev.getKeyCode() == KeyPressedEvent.VK_DELETE)
        doDelete();
    }

    /** Allow the user to set the color for the selected handle. */

    private void selectColor()
    {
      new ColorChooser((BFrame) this.getParent(), Translate.text("selectColor"), color[selected]);
      preview.setBackground(color[selected].getColor());
      canvas.repaint();
      preview.repaint();
      editor.updatePreview();
    }

    /** Respond to mouse presses on the canvas. */

    private void mousePressed(MousePressedEvent ev)
    {
      clickPoint = ev.getPoint();
      canvas.requestFocus();
      if (ev.isControlDown())
      {
        Rectangle bounds = canvas.getBounds();
        double ind = clickPoint.x/(bounds.width-1.0);
        addHandle(0.001*((int) (ind*1000.0)));
        return;
      }
      for (int i = 0; i < handlePos.length; i++)
      {
        int x = handlePos[i].x, y = 30+handlePos[i].y*HANDLE_SIZE;
        if (clickPoint.x >= x-HANDLE_SIZE && clickPoint.x <= x+HANDLE_SIZE &&
            clickPoint.y >= y && clickPoint.y <= y+HANDLE_SIZE)
        {
          selected = i;
          adjustComponents();
          canvas.repaint();
          return;
        }
      }
      clickPoint = null;
    }

    /* Move the current selected handle. */

    private void mouseDragged(MouseDraggedEvent ev)
    {
      if (clickPoint == null || selected == 0 || selected == index.length-1)
        return;
      Rectangle bounds = canvas.getBounds();
      Point pos = ev.getPoint();

      handlePos[selected].x = pos.x;
      double ind = ((double) pos.x)/(bounds.width-1.0);
      if (ind < 0.0)
        ind = 0.0;
      if (ind > 1.0)
        ind = 1.0;
      index[selected] = 0.001*((int) (1000.0*ind));
      while (index[selected] < index[selected-1])
      {
        double temp = index[selected];
        index[selected] = index[selected-1];
        index[selected-1] = temp;
        RGBColor tempcolor = color[selected];
        color[selected] = color[selected-1];
        color[selected-1] = tempcolor;
        Point tempPos = handlePos[selected];
        handlePos[selected] = handlePos[selected-1];
        handlePos[selected-1] = tempPos;
        selected--;
      }
      while (index[selected] > index[selected+1])
      {
        double temp = index[selected];
        index[selected] = index[selected+1];
        index[selected+1] = temp;
        RGBColor tempcolor = color[selected];
        color[selected] = color[selected+1];
        color[selected+1] = tempcolor;
        Point tempPos = handlePos[selected];
        handlePos[selected] = handlePos[selected+1];
        handlePos[selected+1] = tempPos;
        selected++;
      }
      adjustComponents();
      canvas.repaint();
    }

    /* Reposition the handles when the user finished dragging one. */

    private void mouseReleased(MouseReleasedEvent ev)
    {
      clickPoint = null;
      calcCoefficients();
      canvas.repaint();
      editor.updatePreview();
    }

    private void indexChanged()
    {
      index[selected] = indexField.getValue();
      calcCoefficients();
      canvas.repaint();
      editor.updatePreview();
    }

    private void repeatChanged()
    {
      repeat = repeatBox.getState();
      editor.updatePreview();
    }
  }
}

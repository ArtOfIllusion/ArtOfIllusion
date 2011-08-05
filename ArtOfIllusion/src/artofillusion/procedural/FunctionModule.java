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
import java.text.*;

/** This is a Module which outputs a user defined function of its input, y = f(x).  It is defined
    by a set of points (x,y).  For other values of x, the output is determined by interpolation. */

public class FunctionModule extends Module
{
  private boolean repeat, valueOk, errorOk, gradOk;
  private double x[], y[], value, error, deriv, lastBlur;
  private double a0[], a1[], a2[], a3[], b[];
  private short shape;
  private Vec3 gradient;
  
  public static final short LINEAR = 0;
  public static final short SMOOTH_INTERPOLATE = 1;

  public FunctionModule(Point position)
  {
    super("", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(0"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})}, 
      position);
    x = new double [] {0.0, 1.0};
    y = new double [] {0.0, 1.0};
    shape = LINEAR;
    calcCoefficients();
    gradient = new Vec3();
  }
  
  /** Get the list of x (input) values. */
  
  public double [] getX()
  {
    return x;
  }
  
  /** Get the list of y (output) values. */
  
  public double [] getY()
  {
    return y;
  }
  
  /** Set the lists of (x,y) pairs that define the function.
      @param x  the list of x values
      @param y  the list of y values.  These must be in the range [0,1], and be in increasing order
  */
  
  public void setFunction(double x[], double y[])
  {
    this.x = x;
    this.y = y;
    calcCoefficients();
  }

  /** Get whether the function should repeat outside the range [0,1]. */
  
  public boolean getRepeat()
  {
    return repeat;
  }
  
  /** Set whether the function should repeat outside the range [0,1]. */
  
  public void setRepeat(boolean repeat)
  {
    this.repeat = repeat;
    calcCoefficients();
  }
  
  /** Get the method for interpolating between the set values (LINEAR or SMOOTH_INTERPOLATE). */
  
  public short getMethod()
  {
    return shape;
  }
  
  /** Set the method for interpolating between the set values (LINEAR or SMOOTH_INTERPOLATE). */
  
  public void setMethod(short method)
  {
    shape = method;
    calcCoefficients();
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = errorOk = gradOk = false;
  }
    
  /* Calculate the output corresponding to a given input value. */
  
  private double calcValue(double value)
  {
    if (value <= 0.0 || value >= 1.0)
      {
        if (repeat)
          value -= FastMath.floor(value);
        else if (value <= 0.0)
          return y[0];
        else
          return y[y.length-1];
      }
    int i;
    for (i = 1; i < x.length && value > x[i]; i++);
    i--;
    if (shape == SMOOTH_INTERPOLATE)
      return a0[i] + value*(2.0*a1[i] + value*(3.0*a2[i] + value*4.0*a3[i]));
    else
      return a0[i] + value*2.0*a1[i];
  }
  
  /* Calculate the integral of the function at a given point. */
  
  private double integral(double valueIn)
  {
    double vi, vf, result;
    int i;
    
    if (repeat)
      {
        vi = FastMath.floor(valueIn);
        vf = valueIn-vi;
        result = vi*b[b.length-1];
      }
    else
      {
        if (valueIn <= 0.0)
          return valueIn*y[0];
        if (valueIn >= 1.0)
          return b[b.length-1]+(valueIn-1.0)*y[y.length-1];
        vf = (float) valueIn;
        result = 0.0;
      }
    if (vf == 0.0)
      return result;
    for (i = 1; i < x.length && vf > x[i]; i++);
    i--;
    if (shape == SMOOTH_INTERPOLATE)
      result += b[i]+vf*(a0[i]+vf*(a1[i]+vf*(a2[i]+vf*a3[i])));
    else
      result += b[i]+vf*(a0[i]+vf*(a1[i]));
    return result;
  }

  /* Calculate the coefficients for evaluating the curves. */
    
  private void calcCoefficients()
  {
    a0 = new double [x.length-1];
    a1 = new double [x.length-1];
    a2 = new double [x.length-1];
    a3 = new double [x.length-1];
    b = new double [x.length];
    if (shape == LINEAR)
      {
        for (int i = 0; i < a0.length; i++)
          {
            double dx = x[i+1]-x[i];
            if (dx == 0.0)
              continue;
            a1[i] = (y[i+1]-y[i])/dx;
            a0[i] = y[i]-a1[i]*x[i];
            a1[i] *= 0.5;
            b[i+1] = b[i] + x[i+1]*(a0[i]+x[i+1]*a1[i]) - x[i]*(a0[i]+x[i]*a1[i]);
          }
        for (int i = 1; i < b.length-1; i++)
          b[i] -= x[i]*(a0[i]+x[i]*a1[i]);
        return;
      }
    double m[][] = new double [4][4], a[] = new double [4], deriv[] = new double [x.length];
    for (int i = 1; i < x.length-1; i++)
      if (x[i-1] != x[i+1])
        deriv[i] = (y[i+1]-y[i-1])/(x[i+1]-x[i]);
    if (repeat)
      deriv[0] = deriv[x.length-1] = (y[1]-y[y.length-2])/(1.0+x[1]-x[x.length-2]);
    for (int i = 0; i < a0.length; i++)
      {
        m[0][0] = 0.0;
        m[0][1] = 1.0;
        m[0][2] = 2.0*x[i];
        m[0][3] = 3.0*x[i]*x[i];
        a[0] = deriv[i];
        m[1][0] = 1.0;
        m[1][1] = x[i];
        m[1][2] = x[i]*x[i];
        m[1][3] = x[i]*x[i]*x[i];
        a[1] = y[i];
        m[2][0] = 1.0;
        m[2][1] = x[i+1];
        m[2][2] = x[i+1]*x[i+1];
        m[2][3] = x[i+1]*x[i+1]*x[i+1];
        a[2] = y[i+1];
        m[3][0] = 0.0;
        m[3][1] = 1.0;
        m[3][2] = 2.0*x[i+1];
        m[3][3] = 3.0*x[i+1]*x[i+1];
        a[3] = deriv[i+1];
        SVD.solve(m, a);
        a0[i] = a[0];
        a1[i] = 0.5*a[1];
        a2[i] = a[2]/3.0;
        a3[i] = 0.25*a[3];
        b[i+1] = b[i] + x[i+1]*(a0[i]+x[i+1]*(a1[i]+x[i+1]*(a2[i]+x[i+1]*a3[i]))) - x[i]*(a0[i]+x[i]*(a1[i]+x[i]*(a2[i]+x[i]*a3[i])));
      }
    for (int i = 1; i < b.length-1; i++)
      b[i] -= x[i]*(a0[i]+x[i]*(a1[i]+x[i]*(a2[i]+x[i]*a3[i])));
  }

  /* Calculate the output value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    lastBlur = blur;
    valueOk = true;
    double valueIn = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double errorIn = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
    if (errorIn == 0.0)
      {
        value = calcValue(valueIn);
        error = 0.0;
        errorOk = true;
        return value;
      }
    value = (integral(valueIn+errorIn)-integral(valueIn-errorIn))/(2.0*errorIn);
    return value;
  }
  
  /* Calculate the error. */
  
  public double getValueError(int which, double blur)
  {
    if (errorOk && blur == lastBlur)
      return error;
    lastBlur = blur;
    errorOk = true;
    if (linkFrom[0] == null)
      {
        gradient.set(0.0, 0.0, 0.0);
        error = 0.0;
        gradOk = true;
        return 0.0;
      }
    double valueIn = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    if (valueIn <= 0.0 || valueIn >= 1.0)
      {
        if (!repeat)
          {
            gradient.set(0.0, 0.0, 0.0);
            error = 0.0;
            gradOk = true;
            return 0.0;
          }
        valueIn -= FastMath.floor(valueIn);
      }
    int i;
    for (i = 1; i < x.length && valueIn > x[i]; i++);
    i--;
    error = linkFrom[0].getValueError(linkFromIndex[0], blur);
    if (shape == SMOOTH_INTERPOLATE)
      deriv = 2.0*a1[i] + valueIn*(6.0*a2[i] + valueIn*12.0*a3[i]);
    else
      deriv = 2.0*a1[i];
    error *= Math.abs(deriv);
    return error;
  }

  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!errorOk || blur != lastBlur)
      getValueError(which, blur);
    if (gradOk && blur == lastBlur)
      {
        grad.set(gradient);
        return;
      }
    if (linkFrom[0] == null)
      {
        gradient.set(0.0, 0.0, 0.0);
        grad.set(0.0, 0.0, 0.0);
        gradOk = true;
        return;
      }
    lastBlur = blur;
    gradOk = true;
    linkFrom[0].getValueGradient(linkFromIndex[0], gradient, blur);
    gradient.scale(deriv);
    grad.set(gradient);
  }
  
  /* Calculate the size of the module. */
  
  public void calcSize()
  {
    bounds.width = 40+IOPort.SIZE*2;
    bounds.height = 25+IOPort.SIZE*2;
  }

  protected void drawContents(Graphics2D g)
  {
    Rectangle r = new Rectangle(bounds.x+IOPort.SIZE, bounds.y+IOPort.SIZE, bounds.width-2*IOPort.SIZE, bounds.height-2*IOPort.SIZE);
    double miny = Double.MAX_VALUE, maxy = -Double.MAX_VALUE;

    for (int i = 0; i < y.length; i++)
      {
        if (y[i] < miny)
          miny = y[i];
        if (y[i] > maxy)
          maxy = y[i];
      }
    g.setColor(Color.white);
    g.fillRect(r.x, r.y, r.width, r.height);
    g.setColor(Color.black);
    if (shape == SMOOTH_INTERPOLATE)
      {
        int lastx = (int) (r.x+x[0]*r.width);
        int lasty = (int) (r.y+(maxy-y[0])*r.height/(maxy-miny));
        for (int i = 0; i < x.length-1; i++)
          {
            double dx = x[i+1]-x[i];
            int nextx = 0, nexty = 0;
            if (dx == 0.0)
              {
                nextx = (int) (r.x+x[i+1]*r.width);
                nexty = (int) (r.y+(maxy-y[i+1])*r.height/(maxy-miny));
              }
            else
              for (int j = 1; j < 8; j++)
                {
                  double xf = x[i]+j*0.125*dx, yf = calcValue(xf);
                  nextx = (int) (r.x+xf*r.width);
                  nexty = (int) (r.y+(maxy-yf)*r.height/(maxy-miny));
                  g.drawLine(lastx, lasty, nextx, nexty);
                  lastx = nextx;
                  lasty = nexty;
                }
            g.drawLine(lastx, lasty, nextx, nexty);
            lastx = nextx;
            lasty = nexty;
          }
        return;
      }
    for (int i = 0; i < x.length-1; i++)
      {
        int x1 = (int) (r.x+x[i]*r.width);
        int y1 = (int) (r.y+(maxy-y[i])*r.height/(maxy-miny));
        int x2 = (int) (r.x+x[i+1]*r.width);
        int y2 = (int) (r.y+(maxy-y[i+1])*r.height/(maxy-miny));
        g.drawLine(x1, y1, x2, y2);
      }
  }

  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    FunctionModule mod = new FunctionModule(new Point(bounds.x, bounds.y));
    
    mod.repeat = repeat;
    mod.shape = shape;
    mod.x = new double [x.length];
    mod.y = new double [y.length];
    for (int i = 0; i < x.length; i++)
      {
        mod.x[i] = x[i];
        mod.y[i] = y[i];
      }
    mod.calcCoefficients();
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(x.length);
    for (int i = 0; i < x.length; i++)
      {
        out.writeDouble(x[i]);
        out.writeDouble(y[i]);
      }
    out.writeBoolean(repeat);
    out.writeShort(shape);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    int num = in.readInt();
    x = new double [num];
    y = new double [num];
    for (int i = 0; i < x.length; i++)
      {
        x[i] = in.readDouble();
        y[i] = in.readDouble();
      }
    repeat = in.readBoolean();
    shape = in.readShort();
    calcCoefficients();
  }
  
  /* Allow the user to set the parameters. */
  
  public boolean edit(ProcedureEditor editor, Scene theScene)
  {
    EditingDialog dlg = new EditingDialog(editor);
    return dlg.clickedOk;
  }
  
  /* Inner class used for editing the list of colors. */
  
  private class EditingDialog extends BDialog
  {
    ProcedureEditor editor;
    CustomWidget canvas;
    ValueField xField, yField;
    BCheckBox repeatBox, smoothBox;
    BButton deleteButton;
    Point clickPoint, handlePos[];
    Rectangle graphBounds;
    FontMetrics fm;
    NumberFormat hFormat, vFormat;
    int selected;
    boolean clickedOk, fixRange;
    double miny, maxy, labelstep;
    
    static final int HANDLE_SIZE = 5;
    
    public EditingDialog(ProcedureEditor editor)
    {
      super(editor.getParentFrame(), "Function", true);
      this.editor = editor;
      FormContainer content = new FormContainer(1, 5);
      setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
      content.add(Translate.label("functionModuleInstructions"), 0, 0);
      content.add(canvas = new CustomWidget(), 0, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
      canvas.setPreferredSize(new Dimension(200, 150));
      canvas.setBackground(Color.white);
      canvas.addEventLink(RepaintEvent.class, this, "paintCanvas");
      canvas.addEventLink(KeyPressedEvent.class, this, "keyPressed");
      canvas.addEventLink(MousePressedEvent.class, this, "mousePressed");
      canvas.addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
      canvas.addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
      graphBounds = new Rectangle();
      RowContainer row = new RowContainer();
      content.add(row, 0, 2);
      row.add(new BLabel("X:"));
      row.add(xField = new ValueField(Double.NaN, ValueField.NONE));
      xField.setValueChecker(new ValueChecker() {
        public boolean isValid(double val)
        {
          return (val >= 0.0 && val <= 1.0);
        }
      });
      row.add(new BLabel("Y:"));
      row.add(yField = new ValueField(Double.NaN, ValueField.NONE));
      xField.addEventLink(ValueChangedEvent.class, this, "textChanged");
      yField.addEventLink(ValueChangedEvent.class, this, "textChanged");
      row.add(Translate.button("add", this, "doAdd"));
      row.add(deleteButton = Translate.button("delete", this, "doDelete"));
      RowContainer checkboxRow = new RowContainer();
      content.add(checkboxRow, 0, 3);
      checkboxRow.add(repeatBox = new BCheckBox(Translate.text("functionIsPeriodic"), repeat));
      checkboxRow.add(smoothBox = new BCheckBox(Translate.text("smoothCurve"), shape == SMOOTH_INTERPOLATE));
      repeatBox.addEventLink(ValueChangedEvent.class, this, "functionChanged");
      smoothBox.addEventLink(ValueChangedEvent.class, this, "functionChanged");
      RowContainer buttons = new RowContainer();
      content.add(buttons, 0, 4);
      buttons.add(Translate.button("ok", this, "doOk"));
      buttons.add(Translate.button("cancel", this, "dispose"));
      hFormat = NumberFormat.getInstance();
      vFormat = NumberFormat.getInstance();
      hFormat.setMaximumFractionDigits(1);
      findRange();
      adjustComponents();
      handlePos = new Point [x.length];
      for (int i = 0; i < x.length; i++)
        {
          x[i] = FunctionModule.this.x[i];
          y[i] = FunctionModule.this.y[i];
          handlePos[i] = new Point(0, 0);
        }
      pack();
      UIUtilities.centerDialog(this, editor.getParentFrame());
      fm = canvas.getComponent().getFontMetrics(canvas.getFont());
      setVisible(true);
    }
    
    /* Adjust the various components in the window based on the currently selected point. */
    
    private void adjustComponents()
    {
      xField.setValue(x[selected]);
      yField.setValue(y[selected]);
      boolean movable = (selected > 0 && selected < x.length-1);
      xField.setEnabled(movable);
      deleteButton.setEnabled(movable);
    }
    
    /* Determine the range of y values and the labels to use on the y axis. */
    
    private void findRange()
    {
      if (fixRange)
        return;
      miny = Double.MAX_VALUE;
      maxy = -Double.MAX_VALUE;
      for (int i = 0; i < y.length; i++)
        {
          if (y[i] < miny)
            miny = y[i];
          if (y[i] > maxy)
            maxy = y[i];
        }
      if (miny == maxy)
        {
          miny = FastMath.floor(miny);
          maxy = miny+1.0;
        }
      int decimals = (int) FastMath.floor(Math.log(maxy-miny)/Math.log(10.0));
      labelstep = FastMath.pow(10.0, decimals);
      vFormat.setMaximumFractionDigits(decimals < 0 ? -decimals : 1);
    }

    /* Calculate the position of all the handles. */
    
    private void positionHandles(Rectangle r)
    {
      for (int i = 0; i < x.length; i++)
        {
          handlePos[i].x = (int) (r.x+x[i]*r.width);
          handlePos[i].y = (int) (r.y+(maxy-y[i])*r.height/(maxy-miny));
        }
    }
    
    /* Paint the axes on the graph, and calculate the bounds of the graph. */
    
    private void paintAxes(Graphics2D g)
    {
      int maxWidth = 0, fontHeight = fm.getHeight();
      Rectangle bounds = canvas.getBounds();
      double pos = labelstep*Math.ceil(miny/labelstep);
      String label;
      
      graphBounds.y = HANDLE_SIZE/2;
      graphBounds.height = bounds.height-HANDLE_SIZE-fontHeight-5;
      g.setColor(Color.black);
      while (pos <= maxy)
      {
        label = vFormat.format(pos);
        int w = fm.stringWidth(label);
        if (w > maxWidth)
          maxWidth = w;
        g.drawString(label, 0, graphBounds.y+((int) ((maxy-pos)*graphBounds.height/(maxy-miny)))+fontHeight/2);
        pos += labelstep;
      }
      graphBounds.x = maxWidth+5;
      graphBounds.width = bounds.width-maxWidth-5-HANDLE_SIZE/2;
      pos = labelstep*Math.ceil(miny/labelstep);
      while (pos <= maxy)
      {
        int v = graphBounds.y+((int) ((maxy-pos)*graphBounds.height/(maxy-miny)));
        g.drawLine(graphBounds.x-3, v, graphBounds.x, v);
        pos += labelstep;
      }
      for (int i = 0; i < 10; i++)
      {
        label = hFormat.format(0.1*i);
        int h = graphBounds.x+(i*graphBounds.width)/10;
        g.drawLine(h, graphBounds.y+graphBounds.height, h, graphBounds.y+graphBounds.height+3);
        g.drawString(label, h-fm.stringWidth(label)/2, bounds.height);
      }
      g.drawLine(graphBounds.x, 0, graphBounds.x, graphBounds.y+graphBounds.height);
      g.drawLine(graphBounds.x, graphBounds.y+graphBounds.height, graphBounds.x+graphBounds.width, graphBounds.y+graphBounds.height);
      positionHandles(graphBounds);
    }
    
    /* Draw the canvas. */
    
    private void paintCanvas(RepaintEvent ev)
    {
      Graphics2D g = ev.getGraphics();
      paintAxes(g);
      g.setColor(Color.black);
      if (smoothBox.getState())
        {
          int lastx = handlePos[0].x, lasty = handlePos[0].y;
          for (int i = 0; i < handlePos.length-1; i++)
            {
              double dx = x[i+1]-x[i];
              if (dx == 0.0)
                {
                  g.drawLine(lastx, lasty, handlePos[i+1].x, handlePos[i+1].y);
                  lastx = handlePos[i+1].x;
                  lasty = handlePos[i+1].y;
                  continue;
                }
              for (int j = 1; j < 8; j++)
                {
                  double xf = x[i]+j*0.125*dx, yf = calcValue(xf);
                  int nextx = (int) (graphBounds.x+xf*graphBounds.width);
                  int nexty = (int) (graphBounds.y+(maxy-yf)*graphBounds.height/(maxy-miny));
                  g.drawLine(lastx, lasty, nextx, nexty);
                  lastx = nextx;
                  lasty = nexty;
                }
              g.drawLine(lastx, lasty, handlePos[i+1].x, handlePos[i+1].y);
              lastx = handlePos[i+1].x;
              lasty = handlePos[i+1].y;
            }
        }
      else
        for (int i = 0; i < handlePos.length-1; i++)
          g.drawLine(handlePos[i].x, handlePos[i].y, handlePos[i+1].x, handlePos[i+1].y);
      for (int i = 0; i < handlePos.length; i++)
        {
          if (selected == i)
            g.setColor(Color.red);
          else
            g.setColor(Color.black);
          g.fillRect(handlePos[i].x-HANDLE_SIZE/2, handlePos[i].y-HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        }
    }
    
    /* Add a new handle at the specified position. */
    
    private void addHandle(double where, double val)
    {
      double newx[] = new double [x.length+1], newy[] = new double [y.length+1];
      int i;

      for (i = 0; i < x.length && x[i] < where; i++)
        {
          newx[i] = x[i];
          newy[i] = y[i];
        }
      newx[i] = where;
      newy[i] = val;
      selected = i;
      for (; i < x.length; i++)
        {
          newx[i+1] = x[i];
          newy[i+1] = y[i];
        }
      x = newx;
      y = newy;
      handlePos = new Point [x.length];
      for (i = 0; i < handlePos.length; i++)
        handlePos[i] = new Point(0, 0);
      calcCoefficients();
      adjustComponents();
      findRange();
      positionHandles(graphBounds);
      canvas.repaint();
      editor.updatePreview();
    }
    
    /* Delete the currently selected handle. */

    private void doDelete()
    {
      if (selected == 0 || selected == x.length-1)
        return;
      double newx[] = new double [x.length-1], newy[] = new double [y.length-1];
      int i;

      for (i = 0; i < x.length-1; i++)
        {
          if (i < selected)
            {
              newx[i] = x[i];
              newy[i] = y[i];
            }
          else
            {
              newx[i] = x[i+1];
              newy[i] = y[i+1];
            }
        }
      selected = 0;
      x = newx;
      y = newy;
      handlePos = new Point [x.length];
      for (i = 0; i < handlePos.length; i++)
        handlePos[i] = new Point(0, 0);
      calcCoefficients();
      adjustComponents();
      findRange();
      positionHandles(graphBounds);
      canvas.repaint();
      editor.updatePreview();
    }
    
    private void doAdd()
    {
      addHandle(0.5, calcValue(0.5));
    }
    
    private void doOk()
    {
      clickedOk = true;
      dispose();
    }
    
    /* Respond to keypresses. */
    
    private void keyPressed(KeyPressedEvent ev)
    {
      if (ev.getKeyCode() == KeyPressedEvent.VK_ENTER)
        doOk();
      if (ev.getKeyCode() == KeyPressedEvent.VK_ESCAPE)
        dispose();
      if (ev.getSource() != canvas)
        return;
      if (ev.getKeyCode() == KeyPressedEvent.VK_BACK_SPACE || ev.getKeyCode() == KeyPressedEvent.VK_DELETE)
        doDelete();
    }
    
    /* Deal with mouse clicks on the canvas. */
    
    private void mousePressed(MousePressedEvent ev)
    {
      fixRange = true;
      clickPoint = ev.getPoint();
      canvas.requestFocus();
      if (ev.isControlDown())
      {
        double h = (clickPoint.x-graphBounds.x)/(graphBounds.width-1.0);
        double v = (graphBounds.height-clickPoint.y+graphBounds.y)/(graphBounds.height-1.0);
        v = v*(maxy-miny)+miny;
        addHandle(0.001*((int) (1000.0*h)), 0.001*((int) (1000.0*v)));
        return;
      }
      for (int i = 0; i < handlePos.length; i++)
      {
        int xh = handlePos[i].x, yh = handlePos[i].y;
        if (clickPoint.x >= xh-HANDLE_SIZE/2 && clickPoint.x <= xh+HANDLE_SIZE/2 && 
            clickPoint.y >= yh-HANDLE_SIZE/2 && clickPoint.y <= yh+HANDLE_SIZE/2)
        {
          selected = i;
          adjustComponents();
          canvas.repaint();
          return;
        }
      }
      clickPoint = null;
    }
    
    /* Move the currently selected handle. */
    
    private void mouseDragged(MouseDraggedEvent ev)
    {
      if (clickPoint == null)
        return;
      Point pos = ev.getPoint();
      
      handlePos[selected].x = pos.x;
      double newx = ((double) pos.x-graphBounds.x)/(graphBounds.width-1.0);
      double newy = ((double) (graphBounds.height-pos.y+graphBounds.y))/(graphBounds.height-1.0);
      newy = newy*(maxy-miny)+miny;
      if (newx < 0.0)
        newx = 0.0;
      if (newx > 1.0)
        newx = 1.0;
      if (newy < miny)
        newy = miny;
      if (newy > maxy)
        newy = maxy;
      y[selected] = 0.001*((int) (1000.0*newy));
      if (selected == 0 || selected == x.length-1)
      {
        calcCoefficients();
        adjustComponents();
        canvas.repaint();
        return;
      }
      x[selected] = 0.001*((int) (1000.0*newx));
      while (x[selected] < x[selected-1])
      {
        double temp = x[selected];
        x[selected] = x[selected-1];
        x[selected-1] = temp;
        temp = y[selected];
        y[selected] = y[selected-1];
        y[selected-1] = temp;
        Point tempPos = handlePos[selected];
        handlePos[selected] = handlePos[selected-1];
        handlePos[selected-1] = tempPos;
        selected--;
      }
      while (x[selected] > x[selected+1])
      {
        double temp = x[selected];
        x[selected] = x[selected+1];
        x[selected+1] = temp;
        temp = y[selected];
        y[selected] = y[selected+1];
        y[selected+1] = temp;
        Point tempPos = handlePos[selected];
        handlePos[selected] = handlePos[selected+1];
        handlePos[selected+1] = tempPos;
        selected++;
      }
      calcCoefficients();
      adjustComponents();
      canvas.repaint();
    }
    
    /* Reposition the handles when the user finished dragging one. */
    
    private void mouseReleased(MouseReleasedEvent ev)
    {
      clickPoint = null;
      calcCoefficients();
      fixRange = false;
      findRange();
      positionHandles(graphBounds);
      canvas.repaint();
      editor.updatePreview();
    }
    
    private void textChanged()
    {
      x[selected] = xField.getValue();
      y[selected] = yField.getValue();
      while (selected > 0 && x[selected] < x[selected-1])
      {
        double temp = x[selected];
        x[selected] = x[selected-1];
        x[selected-1] = temp;
        temp = y[selected];
        y[selected] = y[selected-1];
        y[selected-1] = temp;
        Point tempPos = handlePos[selected];
        handlePos[selected] = handlePos[selected-1];
        handlePos[selected-1] = tempPos;
        selected--;
      }
      while (selected < x.length-1 && x[selected] > x[selected+1])
      {
        double temp = x[selected];
        x[selected] = x[selected+1];
        x[selected+1] = temp;
        temp = y[selected];
        y[selected] = y[selected+1];
        y[selected+1] = temp;
        Point tempPos = handlePos[selected];
        handlePos[selected] = handlePos[selected+1];
        handlePos[selected+1] = tempPos;
        selected++;
      }
      functionChanged();
    }
    
    private void functionChanged()
    {
      repeat = repeatBox.getState();
      shape = smoothBox.getState() ? SMOOTH_INTERPOLATE : LINEAR;
      calcCoefficients();
      if (!fixRange)
      {
        findRange();
        positionHandles(graphBounds);
      }
      canvas.repaint();
      editor.updatePreview();
    }
  }
}

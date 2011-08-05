/* Copyright (C) 2000-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;

/** This is a Module which outputs an image. */

public class ImageModule extends Module
{
  private ImageMap map;
  private boolean tilex, tiley, mirrorx, mirrory, wrapx, wrapy;
  private boolean pointOk, colorOk, valueOk[], gradOk[], outside;
  private double xscale, yscale, xinv, yinv, componentValue[];
  private double x, y, xsize, ysize, lastBlur;
  private int maxComponent, colorModel;
  private PointInfo point;
  private RGBColor color, tempColor;
  private Vec2 tempGrad;
  private Vec3 gradient[];

  public static final int RGB_MODEL = 0;
  public static final int HSV_MODEL = 1;
  public static final int HLS_MODEL = 2;

  public ImageModule(Point position)
  {
    super("("+Translate.text("menu.imageModule")+")", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"})}, 
      new IOPort [] {new IOPort(IOPort.COLOR, IOPort.OUTPUT, IOPort.RIGHT, new String [] {Translate.text("Color")}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {Translate.text("Red")}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {Translate.text("Green")}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {Translate.text("Blue")}), 
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {Translate.text("Mask")})}, 
      position);
    xscale = yscale = xinv = yinv = 1.0;
    tilex = tiley = true;
    colorModel = RGB_MODEL;
    color = new RGBColor(0.0f, 0.0f, 0.0f);
    tempColor = new RGBColor();
    tempGrad = new Vec2();
    gradient = new Vec3 [] {new Vec3(), new Vec3(), new Vec3(), new Vec3()};
    componentValue = new double [4];
    valueOk = new boolean [4];
    gradOk = new boolean [4];
  }
  
  /** Get the image map used by this module. */
  
  public ImageMap getMap()
  {
    return map;
  }
  
  /** Set the image map used by this module. */
  
  public void setMap(ImageMap map)
  {
    this.map = map;
    maxComponent = (map == null ? 0 : map.getComponentCount()-1);
  }
  
  /** Get the X scale. */
  
  public double getXScale()
  {
    return xscale;
  }
  
  /** Set the X scale. */
  
  public void setXScale(double scale)
  {
    xscale = scale;
    xinv = 1.0/scale;
  }
  
  /** Get the Y scale. */
  
  public double getYScale()
  {
    return yscale;
  }
  
  /** Set the Y scale. */
  
  public void setYScale(double scale)
  {
    yscale = scale;
    yinv = 1.0/scale;
  }
  
  /** Get whether the image is tiled in the X direction. */
  
  public boolean getTileX()
  {
    return tilex;
  }
  
  /** Set whether the image is tiled in the X direction. */
  
  public void setTileX(boolean b)
  {
    tilex = b;
  }
  
  /** Get whether the image is tiled in the Y direction. */
  
  public boolean getTileY()
  {
    return tiley;
  }
  
  /** Set whether the image is tiled in the Y direction. */
  
  public void setTileY(boolean b)
  {
    tiley = b;
  }

  /** Get whether the image is mirrored in the X direction. */
  
  public boolean getMirrorX()
  {
    return mirrorx;
  }
  
  /** Set whether the image is mirrored in the X direction. */
  
  public void setMirrorX(boolean b)
  {
    mirrorx = b;
  }
  
  /** Get whether the image is mirrored in the Y direction. */
  
  public boolean getMirrorY()
  {
    return mirrory;
  }
  
  /** Set whether the image is mirrored in the Y direction. */
  
  public void setMirrorY(boolean b)
  {
    mirrory = b;
  }

  /** Get the color model to output (RGB, HSV, or HLS). */

  public int getColorModel()
  {
    return colorModel;
  }

  /** Get the color model to output (RGB, HSV, or HLS). */

  public void setColorModel(int model)
  {
    colorModel = model;
  }

  /** New point, so the color will need to be recalculated. */

  public void init(PointInfo p)
  {
    point = p;
    pointOk = colorOk = false;
    valueOk[0] = valueOk[1] = valueOk[2] = valueOk[3] = false;
    gradOk[0] = gradOk[1] = gradOk[2] = gradOk[3] = false;
  }

  /** Find the point at which the image is being evaluated. */
  
  private void findPoint(double blur)
  {
    pointOk = true;
    colorOk = valueOk[0] = valueOk[1] = valueOk[2] = valueOk[3] = false;
    x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    x *= xinv;
    y *= yinv;
    outside = (!tilex && (x < 0.0 || x > 1.0)) || (!tiley && (y < 0.0 || y > 1.0));
    if (outside)
      return;
    if (mirrorx)
    {
      double f = FastMath.floor(x);
      if ((((int) f)&1) == 0)
        x = 1.0+f-x;
      else
        x = x-f;
    }
    else
      x = x-FastMath.floor(x);
    if (mirrory)
    {
      double f = FastMath.floor(y);
      if ((((int) f)&1) == 0)
        y = 1.0+f-y;
      else
        y = y-f;
    }
    else
      y = y-FastMath.floor(y);
    xsize = (linkFrom[0] == null) ? 0.5*point.xsize+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    ysize = (linkFrom[1] == null) ? 0.5*point.ysize+blur : linkFrom[1].getValueError(linkFromIndex[1], blur);
    xsize *= xinv;
    ysize *= yinv;
    wrapx = tilex && !mirrorx;
    wrapy = tiley && !mirrory;
  }

  /** Calculate the color. */
  
  public void getColor(int which, RGBColor c, double blur)
  {
    if (colorOk && blur == lastBlur)
    {
      c.copy(color);
      return;
    }
    if (map == null)
    {
      color.setRGB(0.0f, 0.0f, 0.0f);
      c.copy(color);
      return;
    }
    if (!pointOk || blur != lastBlur)
      findPoint(blur);
    colorOk = true;
    lastBlur = blur;
    if (outside)
    {
      // The point is outside the map.

      color.setRGB(0.0f, 0.0f, 0.0f);
      c.copy(color);
      return;
    }
    map.getColor(color, wrapx, wrapy, x, y, xsize, ysize);
    c.copy(color);
  }
  
  /** Get the value of one of the components. */
  
  public double getAverageValue(int which, double blur)
  {
    int component = which-1;
    if (component > maxComponent)
    {
      if (component == 3)
        return 0.0;
      component = 0;
    }
    if (valueOk[component] && blur == lastBlur)
      return componentValue[component];
    if (map == null)
      return 0.0;
    if (!pointOk || blur != lastBlur)
      findPoint(blur);
    if (outside)
      return 0.0;
    if (colorModel == RGB_MODEL || component == 3)
    {
      valueOk[component] = true;
      componentValue[component] = map.getComponent(component, wrapx, wrapy, x, y, xsize, ysize);
    }
    else
    {
      colorOk = true;
      valueOk[0] = valueOk[1] = valueOk[2] = true;
      lastBlur = blur;
      map.getColor(color, wrapx, wrapy, x, y, xsize, ysize);
      float components[] = (colorModel == HSV_MODEL ? color.getHSV() : color.getHLS());
      componentValue[0] = components[0]/360.0;
      componentValue[1] = components[1];
      componentValue[2] = components[2];
    }
    return componentValue[component];
  }

  /** Get the gradient of one of the components. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    int component = which-1;
    if (component > maxComponent)
    {
      if (component == 3)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
      component = 0;
    }
    if (gradOk[component] && blur == lastBlur)
    {
      grad.set(gradient[component]);
      return;
    }
    if (map == null)
    {
      grad.set(0.0, 0.0, 0.0);
      return;
    }
    if (!pointOk || blur != lastBlur)
      findPoint(blur);
    if (outside)
    {
      grad.set(0.0, 0.0, 0.0);
      return;
    }
    if (colorModel == RGB_MODEL || component == 3)
      map.getGradient(tempGrad, component, wrapx, wrapy, x, y, xsize, ysize);
    else
    {
      double value = getAverageValue(which, blur);
      if (x >= 1.0)
        tempGrad.x = 0.0;
      else
      {
        double dx = xsize;
        if (x+dx > 1.0)
          dx = 1.0-x;
        map.getColor(tempColor, wrapx, wrapy, x+dx, y, xsize, ysize);
        float components[] = (colorModel == HSV_MODEL ? tempColor.getHSV() : tempColor.getHLS());
        components[0] /= 360.0;
        tempGrad.x = (components[component]-value)/dx;
      }
      if (y >= 1.0)
        tempGrad.y = 0.0;
      else
      {
        double dy = ysize;
        if (y+dy > 1.0)
          dy = 1.0-y;
        map.getColor(tempColor, wrapx, wrapy, x, y+dy, xsize, ysize);
        float components[] = (colorModel == HSV_MODEL ? tempColor.getHSV() : tempColor.getHLS());
        components[0] /= 360.0;
        tempGrad.y = -(components[component]-value)/dy;
      }
    }
    calcGradient(component, grad, blur);
    grad.set(gradient[component]);
  }

  /**
   * Calculate the gradient of a component.  This assumes that tempGrad has already been
   * set to the appropriate gradient of the image.
   */

  private void calcGradient(int component, Vec3 grad, double blur)
  {
    double dx = tempGrad.x*xinv, dy = tempGrad.y*yinv;
    Vec3 g = gradient[component];
    if (dx != 0.0)
    {
      if (linkFrom[0] == null)
        g.set(dx, 0.0, 0.0);
      else
      {
        linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
        g.x = dx*grad.x;
        g.y = dx*grad.y;
        g.z = dx*grad.z;
      }
    }
    else
      g.set(0.0, 0.0, 0.0);
    if (dy != 0.0)
    {
      if (linkFrom[1] == null)
        g.y += dy;
      else
      {
        linkFrom[1].getValueGradient(linkFromIndex[1], grad, blur);
        g.x += dy*grad.x;
        g.y += dy*grad.y;
        g.z += dy*grad.z;
      }
    }
    gradOk[component] = true;
  }

  public void calcSize()
  {
    bounds.width = ImageMap.PREVIEW_WIDTH+IOPort.SIZE*2;
    bounds.height = ImageMap.PREVIEW_HEIGHT+IOPort.SIZE*2;
    if (output.length*IOPort.SIZE*3 > bounds.height)
      bounds.height = output.length*IOPort.SIZE*3;
  }

  protected void drawContents(Graphics2D g)
  {
    if (map == null)
      {
        super.drawContents(g);
        return;
      }
    g.drawImage(map.getPreview(), bounds.x+bounds.width/2-ImageMap.PREVIEW_WIDTH/2, bounds.y+bounds.height/2-ImageMap.PREVIEW_HEIGHT/2, null);
  }
  
  /** Create a duplicate of this module. */
  
  public Module duplicate()
  {
    ImageModule mod = new ImageModule(new Point(bounds.x, bounds.y));
    
    mod.map = map;
    mod.xscale = xscale;
    mod.yscale = yscale;
    mod.xinv = xinv;
    mod.yinv = yinv;
    mod.color.copy(color);
    mod.tilex = tilex;
    mod.tiley = tiley;
    mod.mirrorx = mirrorx;
    mod.mirrory = mirrory;
    mod.wrapx = wrapx;
    mod.wrapy = wrapy;
    mod.maxComponent = maxComponent;
    mod.colorModel = colorModel;
    return mod;
  }

  /** Set the outputs based on what color model is selected. */

  private void setupOutputs()
  {
    if (colorModel == RGB_MODEL)
    {
      output[1].setDescription(new String [] {Translate.text("Red")});
      output[2].setDescription(new String [] {Translate.text("Green")});
      output[3].setDescription(new String [] {Translate.text("Blue")});
    }
    else if (colorModel == HSV_MODEL)
    {
      output[1].setDescription(new String [] {Translate.text("Hue")});
      output[2].setDescription(new String [] {Translate.text("Saturation")});
      output[3].setDescription(new String [] {Translate.text("Value")});
    }
    else if (colorModel == HLS_MODEL)
    {
      output[1].setDescription(new String [] {Translate.text("Hue")});
      output[2].setDescription(new String [] {Translate.text("Lightness")});
      output[3].setDescription(new String [] {Translate.text("Saturation")});
    }
  }
  
  /** Allow the user to set a new value. */
  
  public boolean edit(final ProcedureEditor editor, final Scene theScene)
  {
    ImageMap oldMap = map;
    final ValueField xField = new ValueField(xscale, ValueField.NONE, 10);
    final ValueField yField = new ValueField(yscale, ValueField.NONE, 10);
    final BCheckBox tilexBox = new BCheckBox("X", tilex);
    final BCheckBox tileyBox = new BCheckBox("Y", tiley);
    final BCheckBox mirrorxBox = new BCheckBox("X", mirrorx);
    final BCheckBox mirroryBox = new BCheckBox("Y", mirrory);
    final BComboBox modelChoice = new BComboBox(new String[] {"RGB", "HSV", "HLS"});
    Object listener = new Object() {
      void processEvent()
      {
        xscale = xField.getValue();
        yscale = yField.getValue();
        xinv = 1.0/xscale;
        yinv = 1.0/yscale;
        tilex = tilexBox.getState();
        tiley = tileyBox.getState();
        mirrorx = mirrorxBox.getState();
        mirrory = mirroryBox.getState();
        colorModel = modelChoice.getSelectedIndex();
        if (map == null)
          maxComponent = 0;
        else if (colorModel == RGB_MODEL)
          maxComponent = map.getComponentCount()-1;
        else if (map.getComponentCount() > 3)
          maxComponent = 3;
        else
          maxComponent = 2;
        editor.updatePreview();
      }
    };
    xField.addEventLink(ValueChangedEvent.class, listener);
    yField.addEventLink(ValueChangedEvent.class, listener);
    tilexBox.addEventLink(ValueChangedEvent.class, listener);
    tileyBox.addEventLink(ValueChangedEvent.class, listener);
    mirrorxBox.addEventLink(ValueChangedEvent.class, listener);
    mirroryBox.addEventLink(ValueChangedEvent.class, listener);
    modelChoice.addEventLink(ValueChangedEvent.class, listener);
    modelChoice.setSelectedIndex(colorModel);
    final BLabel preview = new BLabel() {
      public Dimension getPreferredSize()
      {
        return new Dimension(ImageMap.PREVIEW_WIDTH, ImageMap.PREVIEW_HEIGHT);
      }
    };
    if (map != null)
      preview.setIcon(new ImageIcon(map.getPreview()));
    preview.setAlignment(BLabel.CENTER);
    BOutline outline = new BOutline(preview, BorderFactory.createLineBorder(Color.black)) {
      public Dimension getMaximumSize()
      {
        return new Dimension(ImageMap.PREVIEW_WIDTH+2, ImageMap.PREVIEW_HEIGHT+2);
      }
    };
    preview.addEventLink(MouseClickedEvent.class, new Object() {
      void processEvent()
      {
        ImagesDialog dlg = new ImagesDialog(editor.getParentFrame(), theScene, map);
        if (dlg.getSelection() != map && dlg.getSelection() != null)
          {
            int w = dlg.getSelection().getWidth();
            int h = dlg.getSelection().getHeight();
            if (w > h)
              {
                xField.setValue(1.0);
                yField.setValue(((double) h)/w);
              }
            else
              {
                xField.setValue(((double) w)/h);
                yField.setValue(1.0);
              }
          }
        map = dlg.getSelection();
        if (map == null)
          maxComponent = 0;
        else if (colorModel == RGB_MODEL)
          maxComponent = map.getComponentCount()-1;
        else if (map.getComponentCount() > 3)
          maxComponent = 3;
        else
          maxComponent = 2;
        preview.setIcon(map == null ? null : new ImageIcon(map.getPreview()));
        editor.updatePreview();
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), "Click to Set Image:",
      new Widget [] {outline, xField, yField, tilexBox, tileyBox, mirrorxBox, mirroryBox, modelChoice},
      new String [] {null, "X Size", "Y Size", "Tile", "", "Mirror", "", "Outputs"});
    if (!dlg.clickedOk())
      return false;
    setupOutputs();
    return true;
  }

  /** Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeInt(-2);
    if (map == null)
      out.writeInt(-1);
    else
      out.writeInt(theScene.indexOf(map));
    out.writeDouble(xscale);
    out.writeDouble(yscale);
    out.writeBoolean(tilex);
    out.writeBoolean(tiley);
    out.writeBoolean(mirrorx);
    out.writeBoolean(mirrory);
    out.writeInt(colorModel);
  }
  
  /** Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    int version = in.readInt();
    if (version < -2)
      throw new InvalidObjectException("");
    int index = (version > -2 ? version : in.readInt());
    
    if (index > -1)
      map = theScene.getImage(index);
    else
      map = null;
    xscale = in.readDouble();
    yscale = in.readDouble();
    xinv = 1.0/xscale;
    yinv = 1.0/yscale;
    tilex = in.readBoolean();
    tiley = in.readBoolean();
    mirrorx = in.readBoolean();
    mirrory = in.readBoolean();
    colorModel = (version == -2 ? in.readInt() : RGB_MODEL);
    if (map == null)
      maxComponent = 0;
    else if (colorModel == RGB_MODEL)
      maxComponent = map.getComponentCount()-1;
    else if (map.getComponentCount() > 3)
      maxComponent = 3;
    else
      maxComponent = 2;
    setupOutputs();
  }
}

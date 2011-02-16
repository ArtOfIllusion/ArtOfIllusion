/* Copyright (C) 2001-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.image.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** ImageMapTexture represents a texture whose properties are defined by images. */

public class ImageMapTexture extends Texture2D
{
  public ImageOrColor diffuseColor, specularColor, transparentColor, emissiveColor;
  public ImageOrValue roughness, cloudiness, transparency, specularity, shininess, bump, displacement;
  public boolean tileX, tileY, mirrorX, mirrorY;

  public ImageMapTexture()
  {
    RGBColor white = new RGBColor(1.0f, 1.0f, 1.0f);
    
    // Generate the default texture.

    diffuseColor = new ImageOrColor(white);
    specularColor = new ImageOrColor(white);
    transparentColor = new ImageOrColor(white);
    emissiveColor = new ImageOrColor(new RGBColor(0.0f, 0.0f, 0.0f));
    transparency = new ImageOrValue(0.0f);
    specularity = new ImageOrValue(0.0f);
    shininess = new ImageOrValue(0.0f);
    roughness = new ImageOrValue(0.2f);
    cloudiness = new ImageOrValue(0.0f);
    bump = new ImageOrValue(0.0f);
    displacement = new ImageOrValue(0.0f);
    tileX = tileY = true;
    name = "";
  }

  public static String getTypeName()
  {
    return "Image Mapped";
  }

  public void getTextureSpec(TextureSpec spec, double x, double y, double xsize, double ysize, double angle, double t, double param[])
  {
    float transVal, specVal;
    boolean wrapx, wrapy;
    double f;
    
    if ((!tileX && (x < 0.0 || x > 1.0)) || (!tileY && (y < 0.0 || y > 1.0)))
      {
        // The point falls outside the image, so set the surfaceSpec to be transparent.

        spec.diffuse.setRGB(0.0f, 0.0f, 0.0f);
        spec.specular.setRGB(0.0f, 0.0f, 0.0f);
        spec.hilight.setRGB(0.0f, 0.0f, 0.0f);
        spec.transparent.setRGB(1.0f, 1.0f, 1.0f);
        spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
        spec.roughness = spec.cloudiness = 1.0;
        spec.bumpGrad.set(0.0, 0.0, 0.0);
        return;
      }
    if (mirrorX)
      {
        f = FastMath.floor(x);
        if ((((int) f)&1) == 0)
          x = 1.0+f-x;
        else
          x = x-f;
      }
    else
      x = x-FastMath.floor(x);
    if (mirrorY)
      {
        f = FastMath.floor(y);
        if ((((int) f)&1) == 0)
          y = 1.0+f-y;
        else
          y = y-f;
      }
    else
      y = y-FastMath.floor(y);
    wrapx = tileX && !mirrorX;
    wrapy = tileY && !mirrorY;
    transVal = transparency.getValue(wrapx, wrapy, x, y, xsize, ysize);
    specVal = specularity.getValue(wrapx, wrapy, x, y, xsize, ysize);    
    diffuseColor.getColor(spec.diffuse, wrapx, wrapy, x, y, xsize, ysize);
    spec.diffuse.scale((1.0f-transVal)*(1.0f-specVal));
    specularColor.getColor(spec.specular, wrapx, wrapy, x, y, xsize, ysize);
    spec.hilight.copy(spec.specular);
    spec.specular.scale((1.0f-transVal)*specVal);
    spec.hilight.scale((1.0f-transVal)*shininess.getValue(wrapx, wrapy, x, y, xsize, ysize));
    transparentColor.getColor(spec.transparent, wrapx, wrapy, x, y, xsize, ysize);
    spec.transparent.scale(transVal);
    emissiveColor.getColor(spec.emissive, wrapx, wrapy, x, y, xsize, ysize);
    spec.roughness = roughness.getValue(wrapx, wrapy, x, y, xsize, ysize);
    spec.cloudiness = cloudiness.getValue(wrapx, wrapy, x, y, xsize, ysize);
    Vec2 grad = new Vec2();
    bump.getGradient(grad, wrapx, wrapy, x, y, xsize, ysize);
    spec.bumpGrad.set(grad.x*0.04, grad.y*0.04, 0.0);
  }
  
  public void getTransparency(RGBColor trans, double x, double y, double xsize, double ysize, double angle, double t, double param[])
  {
    float transVal;
    boolean wrapx, wrapy;
    double f;
    
    if ((!tileX && (x < 0.0 || x > 1.0)) || (!tileY && (y < 0.0 || y > 1.0)))
      {
        // The point falls outside the image, so the texture is completely transparent.

        trans.setRGB(1.0f, 1.0f, 1.0f);
        return;
      }
    if (mirrorX)
      {
        f = FastMath.floor(x);
        if ((((int) f)&1) == 0)
          x = 1.0+f-x;
        else
          x = x-f;
      }
    else
      x = x-FastMath.floor(x);
    if (mirrorY)
      {
        f = FastMath.floor(y);
        if ((((int) f)&1) == 0)
          y = 1.0+f-y;
        else
          y = y-f;
      }
    else
      y = y-FastMath.floor(y);
    wrapx = tileX && !mirrorX;
    wrapy = tileY && !mirrorY;
    transVal = transparency.getValue(wrapx, wrapy, x, y, xsize, ysize);
    transparentColor.getColor(trans, wrapx, wrapy, x, y, xsize, ysize);
    trans.scale(transVal);
  }

  public double getDisplacement(double x, double y, double xsize, double ysize, double t, double param[])
  {
    boolean wrapx, wrapy;
    double f;
    
    if ((!tileX && (x < 0.0 || x > 1.0)) || (!tileY && (y < 0.0 || y > 1.0)))
      {
        // The point falls outside the image.

        return 0.0;
      }
    if (mirrorX)
      {
        f = FastMath.floor(x);
        if ((((int) f)&1) == 0)
          x = 1.0+f-x;
        else
          x = x-f;
      }
    else
      x = x-FastMath.floor(x);
    if (mirrorY)
      {
        f = FastMath.floor(y);
        if ((((int) f)&1) == 0)
          y = 1.0+f-y;
        else
          y = y-f;
      }
    else
      y = y-FastMath.floor(y);
    wrapx = tileX && !mirrorX;
    wrapy = tileY && !mirrorY;
    return (double) displacement.getValue(wrapx, wrapy, x, y, xsize, ysize);
  }

  public void getAverageSpec(TextureSpec spec, double time, double param[])
  {
    float transVal = transparency.getAverageValue(), specVal = specularity.getAverageValue();
    
    diffuseColor.getAverageColor(spec.diffuse);
    spec.diffuse.scale((1.0f-transVal)*(1.0f-specVal));
    specularColor.getAverageColor(spec.specular);
    spec.hilight.copy(spec.specular);
    spec.specular.scale((1.0f-transVal)*specVal);
    spec.hilight.scale((1.0f-transVal)*shininess.getAverageValue());
    transparentColor.getAverageColor(spec.transparent);
    spec.transparent.scale(transVal);
    emissiveColor.getAverageColor(spec.emissive);
    spec.roughness = roughness.getAverageValue();
    spec.cloudiness = cloudiness.getAverageValue();
    spec.bumpGrad.set(0.0, 0.0, 0.0);
  }
  
  /** Determine whether this Texture uses the specified image. */

  public boolean usesImage(ImageMap image)
  {
    return (diffuseColor.getImage() == image || specularColor.getImage() == image || 
      transparentColor.getImage() == image || emissiveColor.getImage() == image || 
      roughness.getImage() == image || cloudiness.getImage() == image || 
      transparency.getImage() == image || specularity.getImage() == image || 
      shininess.getImage() == image || bump.getImage() == image ||
      displacement.getImage() == image);
  }

  /** Create a duplicate of the texture. */
  
  public Texture duplicate()
  {
    ImageMapTexture m = new ImageMapTexture();
    
    m.name = name;
    m.diffuseColor.copy(diffuseColor);
    m.specularColor.copy(specularColor);
    m.transparentColor.copy(transparentColor);
    m.emissiveColor.copy(emissiveColor);
    m.transparency = transparency.duplicate();
    m.specularity = specularity.duplicate();
    m.shininess = shininess.duplicate();
    m.roughness = roughness.duplicate();
    m.cloudiness = cloudiness.duplicate();
    m.bump = bump.duplicate();
    m.displacement = displacement.duplicate();
    m.tileX = tileX;
    m.tileY = tileY;
    m.mirrorX = mirrorX;
    m.mirrorY = mirrorY;
    return m;
  }

  /** Determine whether this texture has a non-zero value anywhere for a particular component.
      @param component    the texture component to check for (one of the *_COMPONENT constants)
  */
  
  public boolean hasComponent(int component)
  {
    switch (component)
      {
        case DIFFUSE_COLOR_COMPONENT:
          return ((transparency.getValue() < 1.0f || transparency.getImage() != null) &&
                  (specularity.getValue() < 1.0f || specularity.getImage() != null) &&
                  diffuseColor.getColor().getMaxComponent() > 0.0);
        case SPECULAR_COLOR_COMPONENT:
          return ((transparency.getValue() < 1.0f || transparency.getImage() != null) &&
                  specularity.getValue() > 0.0f && specularColor.getColor().getMaxComponent() > 0.0);
        case TRANSPARENT_COLOR_COMPONENT:
          if (!(tileX && tileY))
            return true;
          return (transparency.getValue() > 0.0f && transparentColor.getColor().getMaxComponent() > 0.0);
        case HILIGHT_COLOR_COMPONENT:
          return ((transparency.getValue() < 1.0f || transparency.getImage() != null) &&
                  shininess.getValue() > 0.0f && specularColor.getColor().getMaxComponent() > 0.0);
        case EMISSIVE_COLOR_COMPONENT:
          return (emissiveColor.getColor().getMaxComponent() > 0.0);
        case BUMP_COMPONENT:
          return (bump.getImage() != null);
        case DISPLACEMENT_COMPONENT:
          return (displacement.getValue() != 0.0f);
      }
    return false;
  }
  
  /** Allow the user to interactively edit the texture. */
  
  public void edit(BFrame fr, Scene sc)
  {
    new Editor(fr, sc);
  }
  
  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */
  
  public ImageMapTexture(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    name = in.readUTF();
    diffuseColor = new ImageOrColor(in, theScene);
    specularColor = new ImageOrColor(in, theScene);
    transparentColor = new ImageOrColor(in, theScene);
    emissiveColor = new ImageOrColor(in, theScene);
    roughness = new ImageOrValue(in, theScene);
    cloudiness = new ImageOrValue(in, theScene);
    transparency = new ImageOrValue(in, theScene);
    specularity = new ImageOrValue(in, theScene);
    if (version == 0)
      shininess = specularity.duplicate();
    else
      shininess = new ImageOrValue(in, theScene);
    bump = new ImageOrValue(in, theScene);
    displacement = new ImageOrValue(in, theScene);
    tileX = in.readBoolean();
    tileY = in.readBoolean();
    mirrorX = in.readBoolean();
    mirrorY = in.readBoolean();
  }
  
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(1);
    out.writeUTF(name);
    diffuseColor.writeToFile(out, theScene);
    specularColor.writeToFile(out, theScene);
    transparentColor.writeToFile(out, theScene);
    emissiveColor.writeToFile(out, theScene);
    roughness.writeToFile(out, theScene);
    cloudiness.writeToFile(out, theScene);
    transparency.writeToFile(out, theScene);
    specularity.writeToFile(out, theScene);
    shininess.writeToFile(out, theScene);
    bump.writeToFile(out, theScene);
    displacement.writeToFile(out, theScene);
    out.writeBoolean(tileX);
    out.writeBoolean(tileY);
    out.writeBoolean(mirrorX);
    out.writeBoolean(mirrorY);
  }
  
  /** A member class which represents a dialog box for editing the texture. */
  
  private class Editor extends BDialog
  {
    BTextField nameField;
    Widget transPanel, specPanel, shinPanel, roughPanel, cloudPanel, bumpPanel, displacePanel;
    Widget diffColorPanel, specColorPanel, transColorPanel, emissColorPanel;
    BCheckBox tileXBox, tileYBox, mirrorXBox, mirrorYBox;
    MaterialPreviewer preview;
    ActionProcessor renderProcessor;
    ImageMapTexture newTexture;
    BFrame parent;
    Scene scene;
    
    public Editor(BFrame fr, Scene sc)
    {
      super(fr, true);
      parent = fr;
      scene = sc;
      newTexture = (ImageMapTexture) duplicate();
      BorderContainer content = new BorderContainer();
      setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));

      // Add the buttons at the bottom.

      RowContainer buttons = new RowContainer();
      buttons.add(Translate.button("ok", this, "doOk"));
      buttons.add(Translate.button("cancel", this, "dispose"));
      content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
      
      // Add the left panel.

      FormContainer left = new FormContainer(2, 6);
      left.add(preview = new MaterialPreviewer(newTexture, null, 200, 160), 0, 0, 2, 1);
      LayoutInfo leftGapLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
      left.add(Translate.label("Name"), 0, 1);
      left.add(Translate.label("DiffuseColor"), 0, 2, leftGapLayout);
      left.add(Translate.label("SpecularColor"), 0, 3, leftGapLayout);
      left.add(Translate.label("TransparentColor"), 0, 4, leftGapLayout);
      left.add(Translate.label("EmissiveColor"), 0, 5, leftGapLayout);
      LayoutInfo justLeftLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
      left.add(nameField = new BTextField(ImageMapTexture.this.name, 15), 1, 1, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, null, null));
      left.add(diffColorPanel = newTexture.diffuseColor.getEditingPanel(parent, sc), 1, 2, justLeftLayout);
      left.add(specColorPanel = newTexture.specularColor.getEditingPanel(parent, sc), 1, 3, justLeftLayout);
      left.add(transColorPanel = newTexture.transparentColor.getEditingPanel(parent, sc), 1, 4, justLeftLayout);
      left.add(emissColorPanel = newTexture.emissiveColor.getEditingPanel(parent, sc), 1, 5, justLeftLayout);
      content.add(left, BorderContainer.WEST);

      // Add the right panel.

      FormContainer right = new FormContainer(2, 8);
      GridContainer boxes = new GridContainer(2, 2);
      right.add(boxes, 0, 0, 2, 1);
      boxes.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
      boxes.add(tileXBox = new BCheckBox("Tile Horizontally", tileX), 0, 0);
      boxes.add(mirrorXBox = new BCheckBox("Mirror Horizontally", mirrorX), 0, 1);
      boxes.add(tileYBox = new BCheckBox("Tile Vertically", tileY), 1, 0);
      boxes.add(mirrorYBox = new BCheckBox("Mirror Vertically", mirrorY), 1, 1);
      right.add(Translate.label("Transparency"), 0, 1, leftGapLayout);
      right.add(Translate.label("Specularity"), 0, 2, leftGapLayout);
      right.add(Translate.label("Shininess"), 0, 3, leftGapLayout);
      right.add(Translate.label("Roughness"), 0, 4, leftGapLayout);
      right.add(Translate.label("Cloudiness"), 0, 5, leftGapLayout);
      right.add(Translate.label("BumpHeight"), 0, 6, leftGapLayout);
      right.add(Translate.label("Displacement"), 0, 7, leftGapLayout);
      right.add(transPanel = newTexture.transparency.getEditingPanel(parent, sc), 1, 1);
      right.add(specPanel = newTexture.specularity.getEditingPanel(parent, sc), 1, 2);
      right.add(shinPanel = newTexture.shininess.getEditingPanel(parent, sc), 1, 3);
      right.add(roughPanel = newTexture.roughness.getEditingPanel(parent, sc), 1, 4);
      right.add(cloudPanel = newTexture.cloudiness.getEditingPanel(parent, sc), 1, 5);
      right.add(bumpPanel = newTexture.bump.getEditingPanel(parent, sc), 1, 6);
      right.add(displacePanel = newTexture.displacement.getEditingPanel(parent, sc), 1, 7);
      content.add(right, BorderContainer.EAST);
      diffColorPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      specColorPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      transColorPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      emissColorPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      transPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      specPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      shinPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      roughPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      cloudPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      bumpPanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      displacePanel.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      tileXBox.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      tileYBox.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      mirrorXBox.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      mirrorYBox.addEventLink(ValueChangedEvent.class, this, "valueChanged");
      renderProcessor = new ActionProcessor();
      pack();
      setResizable(false);
      UIUtilities.centerDialog(this, fr);
      setVisible(true);
    }

    private void doOk()
    {
      transparency.copy(newTexture.transparency);
      specularity.copy(newTexture.specularity);
      shininess.copy(newTexture.shininess);
      roughness.copy(newTexture.roughness);
      cloudiness.copy(newTexture.cloudiness);
      bump.copy(newTexture.bump);
      displacement.copy(newTexture.displacement);
      ImageMapTexture.this.name = nameField.getText();
      diffuseColor.copy(newTexture.diffuseColor);
      specularColor.copy(newTexture.specularColor);
      transparentColor.copy(newTexture.transparentColor);
      emissiveColor.copy(newTexture.emissiveColor);
      tileX = newTexture.tileX;
      tileY = newTexture.tileY;
      mirrorX = newTexture.mirrorX;
      mirrorY = newTexture.mirrorY;
      int index = scene.indexOf(ImageMapTexture.this);
      if (index > -1)
        scene.changeTexture(index);
      dispose();
    }
    
    public void dispose()
    {
      renderProcessor.stopProcessing();
      super.dispose();
    }

    private void valueChanged()
    {
      newTexture.tileX = tileXBox.getState();
      newTexture.tileY = tileYBox.getState();
      newTexture.mirrorX = mirrorXBox.getState();
      newTexture.mirrorY = mirrorYBox.getState();
      renderProcessor.addEvent(new Runnable() {
        public void run()
        {
          preview.render();
        }
      });
    }
  }
}
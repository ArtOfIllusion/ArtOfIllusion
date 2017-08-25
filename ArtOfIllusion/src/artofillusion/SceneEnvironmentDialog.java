/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.CoordinateSystem;
import artofillusion.object.ObjectInfo;
import artofillusion.object.Sphere;
import artofillusion.ui.ColorSampleWidget;
import artofillusion.ui.Translate;
import artofillusion.ui.ValueField;
import buoy.event.CommandEvent;
import buoy.event.ValueChangedEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
import buoy.widget.BCheckBox;
import buoy.widget.BComboBox;
import buoy.widget.BDialog;
import buoy.widget.BLabel;
import buoy.widget.BorderContainer;
import buoy.widget.FormContainer;
import buoy.widget.LayoutInfo;
import buoy.widget.OverlayContainer;
import buoy.widget.RowContainer;
import buoy.widget.WindowWidget;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 *
 * @author mkhramov
 */
public class SceneEnvironmentDialog extends BDialog implements ActionListener, Runnable
{
  
  private static final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
  
  private static final LayoutInfo labelLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(2, 0, 2, 5), null);
  private static final LayoutInfo widgetLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.BOTH, new Insets(2, 0, 2, 0), null);
  
  private final BComboBox environmentType = new BComboBox();
  
  private final BButton cancelButton = new BButton(Translate.text("button.cancel"));
  private final BButton okButton = new BButton(Translate.text("button.ok"));
  private final BButton envButton = new BButton(Translate.text("Choose") + ":");
  
  private final BLabel ambientColorLabel = new BLabel(Translate.text("ambientColor"));
  private final BLabel environmentLabel = new BLabel(Translate.text("environment"));
  private final BLabel envTextureLabel = new BLabel("");
  
  private final BCheckBox fogBox = new BCheckBox("Environment Fog", false);
  
  private final BLabel fogColorLabel = new BLabel(Translate.text("fogColor"));
  
  private final BLabel fogDistanceLabel = new BLabel(Translate.text("fogDistance"));
  private final ValueField fogField = new ValueField(0, ValueField.POSITIVE);
  
  final OverlayContainer envPanel = new OverlayContainer();
  
  private final Scene scene;
  private final WindowWidget parent;
  
  private ColorSampleWidget ambientColor;
  private ColorSampleWidget envColor;
  private ColorSampleWidget fogColor;
  
  final Sphere envSphere = new Sphere(1.0, 1.0, 1.0);
  final ObjectInfo envInfo = new ObjectInfo(envSphere, new CoordinateSystem(), "Environment");
  
  public SceneEnvironmentDialog(WindowWidget frame, Scene scene)
  {
    super(frame, Translate.text("environmentTitle"), false); 
    this.parent = frame;
    this.scene = scene;  
    initGUI();
  }
  
  private void initGUI()
  {
    this.getComponent().setIconImage(ArtOfIllusion.APP_ICON.getImage());
    this.getComponent().getRootPane().registerKeyboardAction(this, escape, JComponent.WHEN_IN_FOCUSED_WINDOW);
    
    BorderContainer content = new BorderContainer();
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(10, 10, 10, 10), null));
    
    FormContainer center = new FormContainer(new double [] {0.0, 1.0}, new double [6]);
    content.add(center, BorderContainer.CENTER);
    
    ambientColor = new ColorSampleWidget(scene.getAmbientColor(), 50, 30);
    envColor = new ColorSampleWidget(scene.getEnvironmentColor(), 50, 30);
    fogColor = new ColorSampleWidget(scene.getFogColor(), 50, 30);
    
    center.add(ambientColorLabel, 0, 0, labelLayout);
    center.add(ambientColor, 1, 0, widgetLayout);
    
    environmentType.add(Translate.text("solidColor"));
    environmentType.add(Translate.text("textureDiffuse"));
    environmentType.add(Translate.text("textureEmissive"));
    environmentType.setSelectedIndex(scene.getEnvironmentMode());
    
    center.add(environmentLabel, 0, 1, labelLayout);
    center.add(environmentType, 1, 1, widgetLayout);
    

    RowContainer row = new RowContainer();
    row.add(envButton);
    row.add(envTextureLabel);

    envPanel.add(envColor,0);
    envPanel.add(row,1);    
    
    envInfo.setTexture(scene.getEnvironmentTexture(), scene.getEnvironmentMapping());
    envSphere.setParameterValues(scene.getEnvironmentParameterValues());
    envTextureLabel.setText(envSphere.getTexture().getName());
    
    if (Scene.ENVIRON_SOLID == scene.getEnvironmentMode())
      envPanel.setVisibleChild(0);
    else
      envPanel.setVisibleChild(1);
    
    center.add(new BLabel(""), 0, 2, labelLayout);
    center.add(envPanel, 1, 2, widgetLayout);
    
    fogBox.setState(scene.getFogState());
    center.add(new BLabel(""), 0, 3, labelLayout);
    center.add(fogBox, 1, 3, widgetLayout);    

    center.add(fogColorLabel, 0, 4, labelLayout);
    center.add(fogColor, 1, 4, widgetLayout);

    fogField.setValue(scene.getFogDistance());
    center.add(fogDistanceLabel, 0, 5, labelLayout);
    center.add(fogField, 1, 5, widgetLayout);
    
    this.addEventLink(WindowClosingEvent.class, this, "onDispose");
    okButton.addEventLink(CommandEvent.class, this, "onOK");
    cancelButton.addEventLink(CommandEvent.class, this, "onCancel");
    environmentType.addEventLink(ValueChangedEvent.class, this, "onChoice");
    envButton.addEventLink(CommandEvent.class, this, "onTextureSelect");
    
    RowContainer buttons = new RowContainer();
    
    buttons.add(okButton);
    buttons.add(cancelButton);
    
    content.add(buttons,BorderContainer.SOUTH);
    setDefaultButton(cancelButton);
    pack();
    setResizable(false);
    ((JDialog)this.getComponent()).setLocationRelativeTo(null);
    setVisible(true);
    

    
  }
  
  public static void show(WindowWidget frame, Scene scene)
  {
    new SceneEnvironmentDialog(frame, scene);
  }
  
  public void onCancel(CommandEvent event)
  {
    getComponent().dispose();
  }
  
  public void onOK(CommandEvent event)
  {
    scene.setAmbientColor(this.ambientColor.getColor());
    scene.setEnvironmentColor(this.envColor.getColor());
    scene.setFogColor(this.fogColor.getColor());
    scene.setFog(fogBox.getState(), fogField.getValue());
    scene.setEnvironmentMode(environmentType.getSelectedIndex());
    scene.setEnvironmentTexture(envSphere.getTexture());
    scene.setEnvironmentMapping(envSphere.getTextureMapping());
    scene.setEnvironmentParameterValues(envSphere.getParameterValues());
    getComponent().dispose();
    ((LayoutWindow)parent).setModified();
  }
  
  public void onTextureSelect(CommandEvent event)
  {
    ObjectTextureDialog otd = new ObjectTextureDialog((LayoutWindow)parent, new ObjectInfo[] {this.envInfo});
    otd.setCallback(this);
    
  }
  public void onDispose()
  {
      getComponent().dispose();
  }
  
  public void onChoice(ValueChangedEvent event)
  {
      if(Scene.ENVIRON_SOLID == environmentType.getSelectedIndex())
      {
        envPanel.setVisibleChild(0);
      } else
      {
        envPanel.setVisibleChild(1);
      }
      
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    getComponent().dispose();
  }

  @Override
  public void run()
  {
    String newTex = envSphere.getTexture().getName();
    envTextureLabel.setText(newTex);
  }
  
}

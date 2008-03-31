/* Copyright 2001-2004 by Rick van der Meiden and Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
*/


package artofillusion.tools;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;

/**
This dialog box allows the user to specify options for creating array objects.
@author Rick van der Meiden
*/

public class ArrayDialog extends BDialog
{
  private LayoutWindow window;
  private ArraySpec spec;

  private Vector curvesVector;
  private BButton okButton, cancelButton;
  private BLabel linearCopiesLabel, stepXLabel, stepYLabel, stepZLabel;
  private BRadioButton curveCopiesBox, curveStepBox, linearBox, curveBox;
  private BCheckBox intervalXBox, intervalYBox, intervalZBox;
  private BCheckBox orientationBox, useOrientationBox, useOriginBox;
  private BCheckBox duplicateBox, groupBox, liveBox, deepBox;
  private BComboBox curveChoice;
  private ValueField linearCopiesField, stepXField, stepYField, stepZField;
  private ValueField curveCopiesField, curveStepField;
  private RadioButtonGroup methodGroup;
  private RadioButtonGroup modeGroup;
  
  public ArrayDialog(LayoutWindow window)
  {
    super(window, "Array", true);
    this.window = window;

    // set defaults from scene
    spec = new ArraySpec(window);

    // get available curves
    curvesVector = new Vector(10,10);
    for (int i=0; i<window.getScene().getNumObjects();i++)
    {
        ObjectInfo info = window.getScene().getObject(i);
        if (info.getObject() instanceof Curve)
                curvesVector.addElement(info);
    }
 
    // layout dialog
    methodGroup = new RadioButtonGroup();
    modeGroup = new RadioButtonGroup();
    ColumnContainer content = new ColumnContainer();
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(0, 0, 15, 0), null));
    content.add(new BLabel("Create Array of Selected Objects:"), new LayoutInfo());
    content.add(createLinearPanel());
    content.add(createCurvePanel());
    content.add(createOptionsPanel());
    content.add(createFinishPanel());

   // don't allow user to use nil curve 
   if (curvesVector.size() <= 0)
        curveBox.setEnabled(false);

    // update spec
    updateSpec();

    pack();
    UIUtilities.centerDialog(this, window);
    setVisible(true);
  }

  private Widget createLinearPanel()
  {
    FormContainer panel = new FormContainer(4, 4);
    panel.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
    panel.add(linearBox = new BRadioButton("Linear:", (spec.method==ArraySpec.METHOD_LINEAR), methodGroup), 0, 0);
    linearBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(linearCopiesLabel = new BLabel("Number of Copies"), 1, 0);
    panel.add(linearCopiesField = new ValueField(spec.linearCopies, ValueField.POSITIVE+ValueField.INTEGER, 4), 2, 0);
    linearCopiesField.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(stepXLabel = new BLabel("Step X:"), 1, 1);
    panel.add(stepXField = new ValueField(spec.stepX, ValueField.NONE, 4), 2, 1);
    stepXField.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(intervalXBox = new BCheckBox("Times X Size", spec.intervalX), 3, 1);
    intervalXBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(stepYLabel = new BLabel("Step Y:"), 1, 2);
    panel.add(stepYField = new ValueField(spec.stepY, ValueField.NONE,4), 2, 2);
    stepYField.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(intervalYBox = new BCheckBox("Times Y Size", spec.intervalY), 3, 2);
    intervalYBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(stepZLabel = new BLabel("Step Z:"), 1, 3);
    panel.add(stepZField = new ValueField(spec.stepZ, ValueField.NONE,4), 2, 3);
    stepZField.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(intervalZBox = new BCheckBox("Times Z Size", spec.intervalZ), 3, 3);
    intervalZBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    return panel;
  }

  private Widget createCurvePanel()
  {
    FormContainer panel = new FormContainer(3, 6);
    panel.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
    panel.add(curveBox = new BRadioButton("From Curve", (spec.method==ArraySpec.METHOD_CURVE), methodGroup), 0, 0);
    curveBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(curveChoice = new BComboBox(), 1, 0);
    // put names of possible curves in choice
    
    for (int k=0; k<curvesVector.size(); k++)
    {
        ObjectInfo info = (ObjectInfo)(curvesVector.elementAt(k));
        curveChoice.add(info.getName());
    }
    
    if (spec.curve != null)
        curveChoice.setSelectedValue(spec.curve.getName());
    curveChoice.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(curveCopiesBox = new BRadioButton("Number Of Copies", spec.curveMode == spec.MODE_COPIES, modeGroup), 1, 1);
    curveCopiesBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(curveCopiesField = new ValueField(spec.curveCopies, ValueField.POSITIVE+ValueField.INTEGER,4), 2, 1);
    curveCopiesField.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(curveStepBox = new BRadioButton("Step Size:", spec.curveMode == spec.MODE_STEP, modeGroup), 1, 2);
    curveStepBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(curveStepField = new ValueField(spec.curveStep, ValueField.POSITIVE,4), 2, 2);
    curveStepField.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(orientationBox = new BCheckBox("Orientation Follows Curve", spec.orientation), 1, 3);
    orientationBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(useOriginBox = new BCheckBox("Use Original Position", !spec.ignoreOrigin), 1, 4);
    useOriginBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(useOrientationBox = new BCheckBox("Use Original Orientation", !spec.ignoreOrientation), 1, 5);
    useOrientationBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    return panel;
  }

  private Widget createOptionsPanel()
  {
    FormContainer panel = new FormContainer(2, 2);
    panel.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
    panel.add(deepBox = new BCheckBox("Include Children", spec.deep), 0, 0);
    deepBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(groupBox = new BCheckBox("Group", spec.group), 0, 1);
    groupBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(duplicateBox = new BCheckBox("Skip First Copy", !spec.dupFirst), 1, 0);
    duplicateBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    panel.add(liveBox = new BCheckBox("Live Duplicates", spec.live), 1, 1);
    liveBox.addEventLink(ValueChangedEvent.class, this, "updateSpec");
    return panel;
  }

  private Widget createFinishPanel()
  {
    RowContainer panel = new RowContainer();
    panel.add(okButton = Translate.button("ok", this, "doOk"));
    panel.add(cancelButton = Translate.button("cancel", this, "dispose"));
    return panel;
  }

  private void doOk()
  {
    updateSpec();
    spec.createArray();
    window.rebuildItemList();
    window.updateImage();
    dispose();
  }

  // Update ArraySpec data
  private void updateSpec()
  {
        // get values

        if (linearBox.getState() == true)
                spec.method = spec.METHOD_LINEAR;
        if (curveBox.getState() == true)
                spec.method = spec.METHOD_CURVE;
        spec.linearCopies = (int)linearCopiesField.getValue();
        spec.stepX = stepXField.getValue();
        spec.stepY = stepYField.getValue();
        spec.stepZ = stepZField.getValue();
        spec.intervalX = intervalXBox.getState();
        spec.intervalY = intervalYBox.getState();
        spec.intervalZ = intervalZBox.getState();
        if (curvesVector.size() > 0)
                spec.curve = (ObjectInfo)(curvesVector.elementAt(curveChoice.getSelectedIndex()));

        if (curveCopiesBox.getState() == true)
                spec.curveMode = spec.MODE_COPIES;
        if (curveStepBox.getState() == true)
                spec.curveMode = spec.MODE_STEP;
        spec.curveStep = curveStepField.getValue();
        spec.curveCopies = (int)curveCopiesField.getValue();
        spec.orientation = orientationBox.getState();
        spec.ignoreOrientation = !useOrientationBox.getState();
        spec.ignoreOrigin = !useOriginBox.getState();
        spec.dupFirst = !duplicateBox.getState();
        spec.group = groupBox.getState();
        spec.live = liveBox.getState();
        spec.deep = deepBox.getState();


        // update enabled/disabled status
        linearCopiesField.setEnabled(spec.method == spec.METHOD_LINEAR);
        stepXField.setEnabled(spec.method == spec.METHOD_LINEAR);
        stepYField.setEnabled(spec.method == spec.METHOD_LINEAR);
        stepZField.setEnabled(spec.method == spec.METHOD_LINEAR);
        linearCopiesLabel.setEnabled(spec.method == spec.METHOD_LINEAR);
        stepXLabel.setEnabled(spec.method == spec.METHOD_LINEAR);
        stepYLabel.setEnabled(spec.method == spec.METHOD_LINEAR);
        stepZLabel.setEnabled(spec.method == spec.METHOD_LINEAR);
        intervalXBox.setEnabled(spec.method == spec.METHOD_LINEAR);
        intervalYBox.setEnabled(spec.method == spec.METHOD_LINEAR);
        intervalZBox.setEnabled(spec.method == spec.METHOD_LINEAR);

        curveChoice.setEnabled(spec.method == spec.METHOD_CURVE);
        curveCopiesField.setEnabled(spec.method == spec.METHOD_CURVE & spec.curveMode == spec.MODE_COPIES);
        curveCopiesBox.setEnabled(spec.method == spec.METHOD_CURVE);
        curveStepField.setEnabled(spec.method == spec.METHOD_CURVE & spec.curveMode == spec.MODE_STEP);
        curveStepBox.setEnabled(spec.method == spec.METHOD_CURVE);
        orientationBox.setEnabled(spec.method == spec.METHOD_CURVE);
        useOriginBox.setEnabled(spec.method == spec.METHOD_CURVE);
        useOrientationBox.setEnabled(spec.method == spec.METHOD_CURVE);

        // duplicateBox.setEnabled(true);
        // groupBox.setEnabled(true);
        // liveBox.setEnabled(true);

  }


}

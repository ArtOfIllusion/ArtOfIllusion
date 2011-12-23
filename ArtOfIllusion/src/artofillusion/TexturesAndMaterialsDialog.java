/* Copyright (C) 2011 by Helge Hansen and Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.image.*;
import artofillusion.material.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

public class TexturesAndMaterialsDialog extends BDialog
{
  Scene theScene;
  EditingWindow parentFrame;
  BTree libraryList;
  File libraryFile;
  Scene selectedScene;
  Texture selectedTexture;
  Material selectedMaterial;
  SceneTreeNode selectedSceneNode;
  int insertLocation;
  BButton duplicateButton, deleteButton, editButton;
  BButton loadLibButton, saveLibButton, deleteLibButton, newFileButton, includeFileButton, closeButton;
  BComboBox typeChoice;
  BRadioButton showTexturesButton, showMaterialsButton, showBothButton;
  List<Texture> textureTypes = PluginRegistry.getPlugins(Texture.class);
  List<Material> materialTypes = PluginRegistry.getPlugins(Material.class);
  MaterialPreviewer matPre;
  BLabel matInfo;
  File mainFolder;
  private boolean showTextures, showMaterials;
  private final ArrayList<Object> rootNodes;

  private static DataFlavor TextureFlavor = new DataFlavor(Texture.class, "Texture");
  private static DataFlavor MaterialFlavor = new DataFlavor(Material.class, "Material");

  ListChangeListener listListener = new ListChangeListener()
  {
    public void itemAdded(int index, java.lang.Object obj)
    {
      ((SceneTreeModel) libraryList.getModel()).rebuildScenes(null);
    }

    public void itemChanged(int index, java.lang.Object obj)
    {
      ((SceneTreeModel) libraryList.getModel()).rebuildScenes(null);
    }

    public void itemRemoved(int index, java.lang.Object obj)
    {
      ((SceneTreeModel) libraryList.getModel()).rebuildScenes(null);
    }
  };

  public TexturesAndMaterialsDialog(EditingWindow frame, Scene aScene)
  {

    super(frame.getFrame(), Translate.text("texturesTitle"), false);

    parentFrame = frame;
    theScene = aScene;

    theScene.addMaterialListener(listListener);
    theScene.addTextureListener(listListener);

    mainFolder = new File(ArtOfIllusion.APP_DIRECTORY, "Textures and Materials");

    BorderContainer content = new BorderContainer();
    setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));

    // list:

    libraryList = new BTree();

    libraryList.setMultipleSelectionEnabled(false);
    libraryList.addEventLink(SelectionChangedEvent.class, this, "doSelectionChanged");
    libraryList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    libraryList.getComponent().setDragEnabled(true);
    libraryList.getComponent().setDropMode(DropMode.ON);
    libraryList.getComponent().setTransferHandler(new DragHandler());
    libraryList.setRootNodeShown(false);

    BScrollPane listWrapper = new BScrollPane(libraryList, BScrollPane.SCROLLBAR_AS_NEEDED, BScrollPane.SCROLLBAR_AS_NEEDED);
    listWrapper.setBackground(libraryList.getBackground());
    listWrapper.setForceWidth(true);
    listWrapper.setPreferredViewSize(new Dimension(250, 250));

    // Radio buttons for filtering the tree

    FormContainer leftPanel = new FormContainer(new double[] {1}, new double[] {1, 0, 0, 0});
    leftPanel.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE));
    leftPanel.add(listWrapper, 0, 0, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH));
    RadioButtonGroup group = new RadioButtonGroup();
    leftPanel.add(showTexturesButton = new BRadioButton(Translate.text("showTextures"), false, group), 0, 1);
    leftPanel.add(showMaterialsButton = new BRadioButton(Translate.text("showMaterials"), false, group), 0, 2);
    leftPanel.add(showBothButton = new BRadioButton(Translate.text("showBoth"), true, group), 0, 3);
    group.addEventLink(SelectionChangedEvent.class, this, "filterChanged");
    content.add(leftPanel, BorderContainer.WEST);

    // preview:

    BorderContainer matBox = new BorderContainer();

    Texture tx0 = theScene.getTexture(0); // initial texture
    matPre = new MaterialPreviewer(tx0, null, 300, 300); // size to be determined
    matBox.add(matPre, BorderContainer.CENTER); // preview must be in the center part to be resizeable

    ColumnContainer infoBox = new ColumnContainer();

    matInfo = new BLabel();
    BOutline matBorder = BOutline.createEmptyBorder(matInfo, 3); // a little space around the text
    infoBox.add(matBorder);
    infoBox.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.BOTH));

    matBox.add(infoBox, BorderContainer.SOUTH);

    content.add(matBox, BorderContainer.CENTER);

    // buttons:
    ColumnContainer buttons = new ColumnContainer();
    buttons.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, null, null));
    content.add(buttons, BorderContainer.EAST);

    buttons.add(new BLabel(Translate.text("sceneFunctions"), BLabel.CENTER));

    typeChoice = new BComboBox();
    typeChoice.add(Translate.text("button.new")+"...");

    java.lang.reflect.Method mtd;

    for (Texture tex : textureTypes)
    {
      try
      {
        mtd = tex.getClass().getMethod("getTypeName", null);
        typeChoice.add((String) mtd.invoke(null, null)+" texture");
      }
      catch (Exception ex)
      {
      }
    }
    for (Material mat : materialTypes)
    {
      try
      {
        mtd = mat.getClass().getMethod("getTypeName", null);
        typeChoice.add((String) mtd.invoke(null, null)+" material");
      }
      catch (Exception ex)
      {
      }
    }
    typeChoice.addEventLink(ValueChangedEvent.class, this, "doNew");

    buttons.add(typeChoice);

    buttons.add(duplicateButton = Translate.button("duplicate", "...", this, "doCopy"));
    buttons.add(deleteButton = Translate.button("delete", "...", this, "doDelete"));
    buttons.add(editButton = Translate.button("edit", "...", this, "doEdit"));

    buttons.add(new BSeparator());

    buttons.add(new BLabel(Translate.text("libraryFunctions"), BLabel.CENTER));
    buttons.add(loadLibButton = Translate.button("loadFromLibrary", this, "doLoadFromLibrary"));
    buttons.add(saveLibButton = Translate.button("saveToLibrary", this, "doSaveToLibrary"));
    buttons.add(deleteLibButton = Translate.button("deleteFromLibrary", this, "doDeleteFromLibrary"));
    buttons.add(newFileButton = Translate.button("newLibraryFile", this, "doNewLib"));
    buttons.add(includeFileButton = Translate.button("showExternalFile", this, "doIncludeLib"));

    buttons.add(new BSeparator());

    buttons.add(closeButton = Translate.button("close", this, "dispose"));

    hilightButtons();

    addEventLink(WindowClosingEvent.class, this, "dispose");
    rootNodes = new ArrayList<Object>();
    showTextures = true;
    showMaterials = true;
    rootNodes.add(new SceneTreeNode(null, theScene));
    for (File file : mainFolder.listFiles())
    {
      if (file.isDirectory())
        rootNodes.add(new FolderTreeNode(file));
      else if (file.getName().endsWith(".aoi"))
        rootNodes.add(new SceneTreeNode(file));
    }
    libraryList.setModel(new SceneTreeModel());
    setSelection(libraryList.getRootNode(), theScene, theScene.getDefaultTexture());
    pack();
    UIUtilities.centerDialog(this, parentFrame.getFrame());
    setVisible(true);
  }

  private String getTypeName(Object item)
  {
    String typeName = "";
    try
    {
      Method mtd = item.getClass().getMethod("getTypeName", null);
      typeName = (String) mtd.invoke(null, null);
    }
    catch (Exception ex)
    {
    }
    return typeName;
  }

  public void doSelectionChanged()
  {
    TreePath selection = libraryList.getSelectedNode();
    Texture oldTexture = selectedTexture;
    Material oldMaterial = selectedMaterial;
    selectedTexture = null;
    selectedMaterial = null;
    insertLocation = -1;
    if (selection != null && libraryList.isLeafNode(selection))
    {
      TreePath parentNode = libraryList.getParentNode(selection);
      SceneTreeNode sceneNode = (SceneTreeNode) parentNode.getLastPathComponent();
      try
      {
        selectedSceneNode = sceneNode;
        selectedScene = sceneNode.getScene();
        libraryFile = sceneNode.file;
        Object node = selection.getLastPathComponent();
        if (node instanceof TextureTreeNode)
        {
          selectedTexture = selectedScene.getTexture(((TextureTreeNode) node).index);
          if (selectedTexture != oldTexture)
          {
            matPre.setTexture(selectedTexture, selectedTexture.getDefaultMapping(matPre.getObject().getObject()));
            matPre.setMaterial(null, null);
            matPre.render();
            setInfoText(Translate.text("textureName")+" "+selectedTexture.getName(), Translate.text("textureType")+" "+getTypeName(selectedTexture));
          }
        }
        else
        {
          selectedMaterial = selectedScene.getMaterial(((MaterialTreeNode) node).index);
          if (selectedMaterial != oldMaterial)
          {
            Texture tex = UniformTexture.invisibleTexture();
            matPre.setTexture(tex, tex.getDefaultMapping(matPre.getObject().getObject()));
            matPre.setMaterial(selectedMaterial, selectedMaterial.getDefaultMapping(matPre.getObject().getObject()));
            matPre.render();
            setInfoText(Translate.text("materialName")+" "+selectedMaterial.getName(), Translate.text("materialType")+" "+getTypeName(selectedMaterial));
          }
        }
      }
      catch (IOException ex)
      {
        new BStandardDialog("", Translate.text("errorLoadingFile")+": "+ex.getLocalizedMessage(), BStandardDialog.ERROR).showMessageDialog(this);
      }
    }
    if (selectedTexture == null && selectedMaterial == null)
    {
      Texture tex = UniformTexture.invisibleTexture();
      matPre.setTexture(tex, tex.getDefaultMapping(matPre.getObject().getObject()));
      matPre.setMaterial(null, null);
      matPre.render();
      setInfoText(Translate.text("noSelection"), "&nbsp;");
    }

    hilightButtons();
  }

  private boolean setSelection(TreePath node, Scene scene, Object object)
  {
    Object value = node.getLastPathComponent();
    if (value instanceof FolderTreeNode && !libraryList.isNodeExpanded(node))
      return false;
    TreeModel model = libraryList.getModel();
    int numChildren = model.getChildCount(value);
    if (value instanceof SceneTreeNode)
    {
      SceneTreeNode stn = (SceneTreeNode) value;
      if (stn.scene == null || stn.scene.get() != scene)
        return false;
      for (int i = 0; i < numChildren; i++)
      {
        Object child = model.getChild(value, i);
        if ((child instanceof TextureTreeNode && scene.getTexture(((TextureTreeNode) child).index) == object) ||
          (child instanceof MaterialTreeNode && scene.getMaterial(((MaterialTreeNode) child).index) == object))
        {
          libraryList.setNodeSelected(node.pathByAddingChild(child), true);
          doSelectionChanged();
          return true;
        }
      }
      return false;
    }
    for (int i = 0; i < numChildren; i++)
    {
      if (setSelection(node.pathByAddingChild(model.getChild(value, i)), scene, object))
        return true;
    }
    return false;
  }

  public void hilightButtons()
  {
    if (selectedTexture == null && selectedMaterial == null)
    {
      duplicateButton.setEnabled(false);
      deleteButton.setEnabled(false);
      editButton.setEnabled(false);
      loadLibButton.setEnabled(false);
      saveLibButton.setEnabled(false);
      deleteLibButton.setEnabled(false);
    }
    else
    {
      hiLight(selectedScene == theScene);
    }
  }

  private void hiLight(boolean h)
  {
    duplicateButton.setEnabled(h);
    deleteButton.setEnabled(h);
    editButton.setEnabled(h);
    loadLibButton.setEnabled(!h);
    saveLibButton.setEnabled(h);
    deleteLibButton.setEnabled(!h);
  }

  public void mouseClicked(MouseClickedEvent ev)
  {
    if (ev.getClickCount() == 2)
    {
      doEdit();
    }
    else if (ev.getClickCount() == 1)
    {
      doSelectionChanged();
    }
  }

  private void filterChanged(SelectionChangedEvent ev)
  {
    if (ev.getWidget() == showTexturesButton)
    {
      showTextures = true;
      showMaterials = false;
    }
    else if (ev.getWidget() == showMaterialsButton)
    {
      showTextures = false;
      showMaterials = true;
    }
    else
    {
      showTextures = true;
      showMaterials = true;
    }
    ((SceneTreeModel) libraryList.getModel()).resetFilter();
  }

  public void doNew()
  {
    int newType = typeChoice.getSelectedIndex()-1;
    if (newType >= 0)
    {
      if (newType >= textureTypes.size())
      {
        // A new material

        int j = 0;
        String name = "";
        do
        {
          j++;
          name = "Untitled "+j;
        } while (theScene.getMaterial(name) != null);
        try
        {
          Material mat = materialTypes.get(newType-textureTypes.size()).getClass().newInstance();
          mat.setName(name);
          theScene.addMaterial(mat);
          mat.edit(parentFrame.getFrame(), theScene);
        }
        catch (Exception ex)
        {
        }
        parentFrame.setModified();
        selectLastCurrentMaterial();
      }
      else
      {
        // A new texture
        
        int j = 0;
        String name = "";
        do
        {
          j++;
          name = "Untitled "+j;
        } while (theScene.getTexture(name) != null);
        try
        {
          Texture tex = textureTypes.get(newType).getClass().newInstance();
          tex.setName(name);
          theScene.addTexture(tex);
          tex.edit(parentFrame.getFrame(), theScene);
        }
        catch (Exception ex)
        {
        }
        parentFrame.setModified();
        selectLastCurrentTexture();
      }
      typeChoice.setSelectedIndex(0);
    }
  }

  public void doCopy()
  {
    if (selectedTexture != null)
    {
      String name = new BStandardDialog("", Translate.text("newTexName"), BStandardDialog.PLAIN).showInputDialog(this, null, "");
      if (name == null)
        return;
      Texture tex = selectedTexture.duplicate();
      tex.setName(name);
      theScene.addTexture(tex);
      parentFrame.setModified();
      selectLastCurrentTexture();
    }
    else if (selectedMaterial != null)
    {
      String name = new BStandardDialog("", Translate.text("newMatName"), BStandardDialog.PLAIN).showInputDialog(this, null, "");
      if (name == null)
        return;
      Material mat = selectedMaterial.duplicate();
      mat.setName(name);
      theScene.addMaterial(mat);
      parentFrame.setModified();
      selectLastCurrentMaterial();
    }
  }

  public void doDelete()
  {
    if (selectedTexture != null)
    {
      String[] options = new String[]{Translate.text("button.ok"), Translate.text("button.cancel")};
      int choice = new BStandardDialog("", Translate.text("deleteTexture", selectedTexture.getName()), BStandardDialog.PLAIN).showOptionDialog(this, options, options[1]);
      if (choice == 0)
      {
        theScene.removeTexture(theScene.indexOf(selectedTexture));
        parentFrame.setModified();
        setSelection(libraryList.getRootNode(), theScene, theScene.getDefaultTexture());
      }
    }
    else if (selectedMaterial != null)
    {
      String[] options = new String[]{Translate.text("button.ok"), Translate.text("button.cancel")};
      int choice = new BStandardDialog("", Translate.text("deleteMaterial", selectedMaterial.getName()), BStandardDialog.PLAIN).showOptionDialog(this, options, options[1]);
      if (choice == 0)
      {
        theScene.removeMaterial(theScene.indexOf(selectedMaterial));
        parentFrame.setModified();
        setSelection(libraryList.getRootNode(), theScene, theScene.getDefaultTexture());
      }
    }
  }

  public void doEdit()
  {
    if (selectedScene != theScene)
      return;
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    if (selectedTexture != null)
    {
      Texture tex = selectedTexture;
      tex.edit(parentFrame.getFrame(), theScene);
      tex.assignNewID();
      theScene.changeTexture(theScene.indexOf(tex));
      parentFrame.setModified();
    }
    else if (selectedMaterial != null)
    {
      Material mat = selectedMaterial;
      mat.edit(parentFrame.getFrame(), theScene);
      mat.assignNewID();
      theScene.changeMaterial(theScene.indexOf(mat));
      parentFrame.setModified();
    }
    setCursor(Cursor.getDefaultCursor());
  }

  // --

  public void doLoadFromLibrary()
  {
    if (selectedTexture != null)
    {
      Texture newTexture = selectedTexture.duplicate();
      theScene.addTexture(newTexture, insertLocation == -1 ? theScene.getNumTextures() : insertLocation);
      parentFrame.setModified();
      for (int i = 0; i < selectedScene.getNumImages(); i++)
      {
        ImageMap image = selectedScene.getImage(i);
        if (selectedTexture.usesImage(image))
        {
          theScene.addImage(image);
        }
      }
      parentFrame.updateImage();
      setSelection(libraryList.getRootNode(), theScene, newTexture);
    }
    else if (selectedMaterial != null)
    {
      Material newMaterial = selectedMaterial.duplicate();
      theScene.addMaterial(newMaterial, insertLocation == -1 ? theScene.getNumMaterials() : insertLocation);
      parentFrame.setModified();
      for (int i = 0; i < selectedScene.getNumImages(); i++)
      {
        ImageMap image = selectedScene.getImage(i);
        if (selectedMaterial.usesImage(image))
        {
          theScene.addImage(image);
        }
      }
      parentFrame.updateImage();
      setSelection(libraryList.getRootNode(), theScene, newMaterial);
    }
    hilightButtons();
  }

  public void doSaveToLibrary()
  {
    String itemText;
    if (selectedTexture != null)
    {
      itemText = "selectSceneToSaveTexture";
    }
    else
    {
      itemText = "selectSceneToSaveMaterial";
    }
    if (selectedTexture != null || selectedMaterial != null)
    {
      BFileChooser fcOut = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text(itemText), mainFolder);
      if (fcOut.showDialog(this))
        saveToFile(fcOut.getSelectedFile());
    }
  }

  private void saveToFile(File saveFile)
  {
    if (saveFile.exists())
    {
      try
      {
        Scene saveScene = new Scene(saveFile, true);
        if (selectedTexture != null)
        {
          Texture newTexture = selectedTexture.duplicate();
          saveScene.addTexture(newTexture, insertLocation == -1 ? saveScene.getNumTextures() : insertLocation);
          for (int i = 0; i < selectedScene.getNumImages(); i++)
          {
            ImageMap image = selectedScene.getImage(i);
            if (selectedTexture.usesImage(image))
              saveScene.addImage(image);
          }
          saveScene.writeToFile(saveFile);
        }
        else if (selectedMaterial != null)
        {
          Material newMaterial = selectedMaterial.duplicate();
          saveScene.addMaterial(newMaterial, insertLocation == -1 ? saveScene.getNumMaterials() : insertLocation);
          for (int i = 0; i < selectedScene.getNumImages(); i++)
          {
            ImageMap image = selectedScene.getImage(i);
            if (selectedMaterial.usesImage(image))
              saveScene.addImage(image);
          }
          saveScene.writeToFile(saveFile);
        }
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
    }
    ((SceneTreeModel) libraryList.getModel()).rebuildScenes(saveFile);
  }

  public void doDeleteFromLibrary()
  {
    if (selectedScene == null || selectedScene == theScene)
      return;
    try
    {
      if (selectedTexture != null)
      {
        String[] options = new String[]{Translate.text("button.ok"), Translate.text("button.cancel")};
        int choice = new BStandardDialog("", Translate.text("deleteTexture", selectedTexture.getName()), BStandardDialog.PLAIN).showOptionDialog(this, options, options[1]);
        if (choice == 0)
        {
          int texIndex = selectedScene.indexOf(selectedTexture);
          selectedScene.removeTexture(texIndex);
          selectedScene.writeToFile(libraryFile);
          ((SceneTreeModel) libraryList.getModel()).rebuildScenes(libraryFile);
          setSelection(libraryList.getRootNode(), theScene, theScene.getDefaultTexture());
        }
      }
      else if (selectedMaterial != null)
      {
        String[] options = new String[]{Translate.text("button.ok"), Translate.text("button.cancel")};
        int choice = new BStandardDialog("", Translate.text("deleteMaterial", selectedMaterial.getName()), BStandardDialog.PLAIN).showOptionDialog(this, options, options[1]);
        if (choice == 0)
        {
          int matIndex = selectedScene.indexOf(selectedMaterial);
          selectedScene.removeMaterial(matIndex);
          selectedScene.writeToFile(libraryFile);
          ((SceneTreeModel) libraryList.getModel()).rebuildScenes(libraryFile);
          setSelection(libraryList.getRootNode(), theScene, theScene.getDefaultTexture());
        }
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

  public void doNewLib()
  {
    BFileChooser fcNew = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("selectNewLibraryName"), mainFolder);
    if (fcNew.showDialog(this))
    {
      File saveFile = fcNew.getSelectedFile();
      if (!saveFile.exists())
      {
        try
        {
          new Scene().writeToFile(saveFile);
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
        }
        ((SceneTreeModel) libraryList.getModel()).rebuildLibrary();
      }
      else
      {
        BStandardDialog d = new BStandardDialog("", Translate.text("fileAlreadyExists"), BStandardDialog.ERROR);
        d.showMessageDialog(this);
      }
    }
  }

  public void doIncludeLib()
  {
    BFileChooser fcInc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("selectExternalFile"));
    if (fcInc.showDialog(this))
    {
      File inputFile = fcInc.getSelectedFile();
      if (inputFile.exists())
      {
        ((SceneTreeModel) libraryList.getModel()).addScene(inputFile);
      }
    }
  }

  public void dispose()
  {
    theScene.removeMaterialListener(listListener);
    theScene.removeTextureListener(listListener);
    super.dispose();
  }

  private void setInfoText(String line1, String line2)
  {
    String s = "<html><p>"+line1+"</p><p>"+line2+"</p></html>";
    matInfo.setText(s);
  }

  private void selectLastCurrentTexture()
  {
    TreePath r = libraryList.getRootNode();
    TreePath current = libraryList.getChildNode(r, 0);
    libraryList.setNodeExpanded(current, true);
    int lastIndex = libraryList.getChildNodeCount(current)-theScene.getNumMaterials()-1;
    libraryList.setNodeSelected(libraryList.getChildNode(current, lastIndex), true);
    doSelectionChanged();
  }

  private void selectLastCurrentMaterial()
  {
    TreePath r = libraryList.getRootNode();
    TreePath current = libraryList.getChildNode(r, 0);
    libraryList.setNodeExpanded(current, true);
    int lastIndex = libraryList.getChildNodeCount(current)-1;
    libraryList.setNodeSelected(libraryList.getChildNode(current, lastIndex), true);
    doSelectionChanged();
  }

  private class TextureTreeNode
  {
    int index;
    String name;
    SceneTreeNode scene;

    TextureTreeNode(SceneTreeNode scene, int index) throws IOException
    {
      this.scene = scene;
      this.index = index;
      name = scene.getScene().getTexture(index).getName();
    }

    @Override
    public String toString()
    {
      return name;
    }
  }

  private class MaterialTreeNode
  {
    int index;
    String name;
    SceneTreeNode scene;

    MaterialTreeNode(SceneTreeNode scene, int index) throws IOException
    {
      this.scene = scene;
      this.index = index;
      name = scene.getScene().getMaterial(index).getName();
    }

    @Override
    public String toString()
    {
      return name;
    }
  }

  private class SceneTreeNode
  {
    ArrayList<TextureTreeNode> textures;
    ArrayList<MaterialTreeNode> materials;
    SoftReference<Scene> scene;
    final File file;

    SceneTreeNode(File file)
    {
      this.file = file;
    }

    SceneTreeNode(File file, Scene scene)
    {
      this.file = file;
      this.scene = new SoftReference<Scene>(scene);
    }

    void ensureChildrenValid()
    {
      if (textures == null)
      {
        try
        {
          Scene theScene = getScene();
          textures = new ArrayList<TextureTreeNode>();
          for (int i = 0; i < theScene.getNumTextures(); i++)
            textures.add(new TextureTreeNode(this, i));
          materials = new ArrayList<MaterialTreeNode>();
          for (int i = 0; i < theScene.getNumMaterials(); i++)
            materials.add(new MaterialTreeNode(this, i));
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
        }
      }
    }

    Scene getScene() throws IOException
    {
      if (scene != null)
      {
        Scene theScene = scene.get();
        if (theScene != null)
          return theScene;
      }
      Scene theScene = new Scene(file, true);
      scene = new SoftReference<Scene>(theScene);
      return theScene;
    }

    @Override
    public String toString()
    {
      if (file == null)
        return Translate.text("currentScene");
      return file.getName().substring(0, file.getName().length()-4);
    }
  }

  private class FolderTreeNode
  {
    final File file;
    ArrayList<Object> children;

    FolderTreeNode(File file)
    {
      this.file = file;
    }

    ArrayList<Object> getChildren()
    {
      if (children == null)
      {
        children = new ArrayList<Object>();
        for (File f : file.listFiles())
        {
          if (f.isDirectory())
            children.add(new FolderTreeNode(f));
          else if (f.getName().endsWith(".aoi"))
            children.add(new SceneTreeNode(f));
        }
      }
      return children;
    }

    @Override
    public String toString()
    {
      return file.getName();
    }
  }

  private class SceneTreeModel implements TreeModel
  {
    private ArrayList<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
    private DefaultMutableTreeNode root = new DefaultMutableTreeNode();

    public void addTreeModelListener(TreeModelListener listener)
    {
      listeners.add(listener);
    }

    public void removeTreeModelListener(TreeModelListener listener)
    {
      listeners.remove(listener);
    }

    public Object getRoot()
    {
      return root;
    }

    public Object getChild(Object o, int i)
    {
      if (o == root)
        return rootNodes.get(i);
      if (o instanceof FolderTreeNode)
        return ((FolderTreeNode) o).getChildren().get(i);
      SceneTreeNode node = (SceneTreeNode) o;
      node.ensureChildrenValid();
      if (showTextures)
      {
        if (i < node.textures.size())
          return node.textures.get(i);
        i -= node.textures.size();
      }
      return node.materials.get(i);
    }

    public int getChildCount(Object o)
    {
      if (o == root)
        return rootNodes.size();
      if (o instanceof FolderTreeNode)
        return ((FolderTreeNode) o).getChildren().size();
      if (!(o instanceof SceneTreeNode))
        return 0;
      SceneTreeNode node = (SceneTreeNode) o;
      node.ensureChildrenValid();
      int count = 0;
      if (showTextures)
        count += node.textures.size();
      if (showMaterials)
        count += node.materials.size();
      return count;
    }

    public boolean isLeaf(Object o)
    {
      return !(o == root || o instanceof FolderTreeNode || o instanceof SceneTreeNode);
    }

    public void valueForPathChanged(TreePath treePath, Object o)
    {
    }

    public int getIndexOfChild(Object o, Object o1)
    {
      if (o == root)
        return rootNodes.indexOf(o1);
      if (o instanceof FolderTreeNode)
        return ((FolderTreeNode) o).getChildren().indexOf(o1);
      SceneTreeNode node = (SceneTreeNode) o;
      node.ensureChildrenValid();
      int texIndex = node.textures.indexOf(o1);
      if (texIndex > -1)
        return texIndex;
      int matIndex = node.materials.indexOf(o1);
      if (matIndex > -1)
        return matIndex+(showTextures ? node.textures.size() : 0);;
      return -1;
    }

    void rebuildNode(Object node, File file)
    {
      if (node instanceof SceneTreeNode)
      {
        SceneTreeNode sct = (SceneTreeNode) node;
        if (file == null || file.equals(sct.file))
        {
          sct.textures = null;
          sct.materials = null;
          if (sct.file != null)
            sct.scene = null;
        }
        return;
      }
      if (node instanceof FolderTreeNode && ((FolderTreeNode) node).children == null)
        return;
      int numChildren = getChildCount(node);
      for (int i = 0; i < numChildren; i++)
        rebuildNode(getChild(node, i), file);
    }

    void rebuildScenes(final File file)
    {
      updateTree(new Runnable()
      {
        public void run()
        {
          rebuildNode(root, file);
        }
      });
    }

    void rebuildLibrary()
    {
      updateTree(new Runnable()
      {
        public void run()
        {
          ((FolderTreeNode) rootNodes.get(1)).children = null;
        }
      });
    }

    void resetFilter()
    {
      updateTree(null);
    }

    void addScene(final File file)
    {
      updateTree(new Runnable()
      {
        public void run()
        {
          rootNodes.add(new SceneTreeNode(file));
        }
      });
    }

    void updateTree(Runnable updater)
    {
      List<TreePath> expanded = Collections.list(libraryList.getComponent().getExpandedDescendants(libraryList.getRootNode()));
      if (updater != null)
        updater.run();
      Object selection = (selectedTexture == null ? selectedMaterial : selectedTexture);
      TreeModelEvent ev = new TreeModelEvent(this, new TreePath(root));
      for (TreeModelListener listener : listeners)
        listener.treeStructureChanged(ev);
      for (TreePath path : expanded)
        libraryList.setNodeExpanded(path, true);
      if (selection != null)
        setSelection(new TreePath(root), selectedScene, selection);
    }
  }

  private class DragHandler extends TransferHandler
  {
    @Override
    public int getSourceActions(JComponent jComponent)
    {
      return COPY;
    }

    @Override
    public boolean canImport(TransferSupport transferSupport)
    {
      SceneTreeNode sceneNode = findDropLocation(transferSupport);
      if (sceneNode == null)
        return false;
      if (sceneNode == selectedSceneNode)
      {
        int current = -1;
        try
        {
          if (selectedTexture != null)
            current = selectedSceneNode.getScene().indexOf(selectedTexture);
          else if (selectedMaterial != null)
            current = selectedSceneNode.getScene().indexOf(selectedMaterial);
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
          return false;
        }
        if (insertLocation == -1 || insertLocation == current)
          return false;
      }
      transferSupport.setShowDropLocation(true);
      return true;
    }

    @Override
    protected Transferable createTransferable(JComponent jComponent)
    {
      if (selectedTexture != null)
        return new DragTransferable(selectedTexture);
      if (selectedMaterial != null)
        return new DragTransferable(selectedMaterial);
      return null;
    }

    @Override
    public boolean importData(TransferSupport transferSupport)
    {
      SceneTreeNode sceneNode = findDropLocation(transferSupport);
      if (sceneNode == null)
        return false;
      try
      {
        Scene destScene = sceneNode.getScene();
        if (sceneNode == selectedSceneNode)
        {
          Scene saveScene = (destScene == theScene ? theScene : new Scene(sceneNode.file, true));
          if (selectedTexture != null)
            saveScene.reorderTexture(destScene.indexOf(selectedTexture), insertLocation);
          else if (selectedMaterial != null)
            saveScene.reorderMaterial(destScene.indexOf(selectedMaterial), insertLocation);
          if (destScene != theScene)
            saveScene.writeToFile(sceneNode.file);
          ((SceneTreeModel) libraryList.getModel()).rebuildScenes(null);
        }
        else if (destScene == theScene)
          doLoadFromLibrary();
        else
          saveToFile(sceneNode.file);
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
      return true;
    }

    private SceneTreeNode findDropLocation(TransferSupport transferSupport)
    {
      TreePath location = libraryList.findNode(transferSupport.getDropLocation().getDropPoint());
      if (location == null)
        return null;
      insertLocation = -1;
      if (selectedTexture != null && location.getLastPathComponent() instanceof TextureTreeNode)
        insertLocation = ((TextureTreeNode) location.getLastPathComponent()).index;
      if (selectedMaterial != null && location.getLastPathComponent() instanceof MaterialTreeNode)
        insertLocation = ((MaterialTreeNode) location.getLastPathComponent()).index;
      for (Object node : location.getPath())
        if (node instanceof SceneTreeNode)
          return (SceneTreeNode) node;
      return null;
    }
  }

  private class DragTransferable implements Transferable
  {
    private Object data;
    private DataFlavor flavors[];

    public DragTransferable(Object data)
    {
      this.data = data;
      flavors = new DataFlavor[] {DataFlavor.stringFlavor, data instanceof Texture ? TextureFlavor : MaterialFlavor};
    }

    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException
    {
      if (dataFlavor == DataFlavor.stringFlavor)
        return data.toString();
      return data;
    }

    public DataFlavor[] getTransferDataFlavors()
    {
      return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor dataFlavor)
    {
      for (DataFlavor flavor : flavors)
        if (flavor == dataFlavor)
          return true;
      return false;
    }
  }
}

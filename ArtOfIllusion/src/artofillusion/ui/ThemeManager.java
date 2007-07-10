/* Copyright (C) 2007 by Franois Guillet
   Some parts copyright 2007 by Peter Eastman

 This program is free software; you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import artofillusion.*;
import artofillusion.math.RGBColor;

/**
 * This class holds GUI customization information. Customization consists of
 * various colors used in AoI GUI as well as the look and feel of some GUI
 * elements (eg buttons). In this respect, the theme manager is thus a factory of GUI elements.
 *
 * @author Franois Guillet
 *
 */
public class ThemeManager {


    /**
     * This class hold all the colors used by a theme. A theme can propose several color sets.
     *
     * @author Francois Guillet
     *
     */
    public static class ColorSet {
        public final Color appBackground;
        public final Color paletteBackground;
        public final Color viewerBackground;
        public final Color viewerLine;
        public final Color viewerHandle;
        public final Color viewerHighlight;
        public final Color viewerSpecialHighlight;
        public final Color viewerDisabled;
        public final Color viewerSurface;
        public final Color viewerTransparent;
        public final Color dockableBarColor1;
        public final Color dockableBarColor2;
        public final Color dockableTitleColor;
        public final Color textColor;
        private final String name;

        private ColorSet(Node node)
        {
          name = getAttribute(node, "name");
          NodeList list = node.getChildNodes();
          node = getNodeFromNodeList(list, "applicationbackground");
          appBackground = getColorFromNode(node);
          node = getNodeFromNodeList(list, "palettebackground");
          paletteBackground = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewerbackground");
          viewerBackground = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewerline");
          viewerLine = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewerhandle");
          viewerHandle = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewerhighlight");
          viewerHighlight = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewerspecialhighlight");
          viewerSpecialHighlight = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewerdisabled");
          viewerDisabled = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewersurface");
          viewerSurface = getColorFromNode(node);
          node = getNodeFromNodeList(list, "viewertransparent");
          viewerTransparent = getColorFromNode(node);
          node = getNodeFromNodeList(list, "dockablebarcolor1");
          dockableBarColor1 = getColorFromNode(node);
          node = getNodeFromNodeList(list, "dockablebarcolor2");
          dockableBarColor2 = getColorFromNode(node);
          node = getNodeFromNodeList(list, "dockabletitlecolor");
          dockableTitleColor = getColorFromNode(node);
          node = getNodeFromNodeList(list, "textcolor");
          textColor = getColorFromNode(node);
        }

      public String getName()
      {
        return Translate.text(name);
      }
    }

    /**
     * This class stores information about a theme. This can be general purpose information such as
     * theme content and author, or very specific information such as the button styling for
     * this theme.
     *
     * @author Francois Guillet
     *
     */
    public static class ThemeInfo
    {
      private final String name;
      public final String author;
      public final String description;
      public final Class buttonClass;
      //this is the button style parameters for this theme.
      //Relevant XML node is passed onto the button class so it can parse
      //it and deliver button style parameters the theme will give the buttons
      //whenever it is selected.
      public final Object buttonProperties;
      //button margin is the space around each button
      public final int buttonMargin;
      //palette margin is the space around the buttons
      public final int paletteMargin;
      //the theme colorsets
      private final ColorSet[] colorSets;
      public final boolean classicToolBarButtons;
      public final PluginRegistry.PluginResource resource;
      public final String pathRoot;
      public final boolean selectable;

      private ThemeInfo(PluginRegistry.PluginResource resource) throws IOException, SAXException, ParserConfigurationException
      {
        InputStream is = resource.getInputStream();
        DocumentBuilder builder;
        builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.parse( is );
        is.close();
        Node rootNode = document.getDocumentElement();
        NodeList themeNodeList = rootNode.getChildNodes();
        this.resource = resource;
        String root = resource.getName();
        if (root.lastIndexOf('/') > -1)
          root = root.substring(0, root.lastIndexOf('/')+1);
        else
          root = "";
        pathRoot = root;
        String s;
        Node node = getNodeFromNodeList(themeNodeList, "name");
        name = (node != null ? node.getFirstChild().getNodeValue() : "");
        node = getNodeFromNodeList(themeNodeList, "author");
        author = (node != null ? node.getFirstChild().getNodeValue() : "");
        node = getNodeFromNodeList(themeNodeList, "description");
        description = (node != null ? node.getFirstChild().getNodeValue() : "");
        node = getNodeFromNodeList(themeNodeList, "selectable");
        selectable = (node != null ? Boolean.valueOf(node.getFirstChild().getNodeValue()).booleanValue() : true);
        node = getNodeFromNodeList(themeNodeList, "button");
        if (node != null) {
            String className = getAttribute(node, "class");
            Object properties = null;
            Class cls = DefaultToolButton.class;
            try {
              cls = resource.getClassLoader().loadClass(className);
              Method m = cls.getMethod("readPropertiesFromXMLNode", new Class[] { Node.class } );
                properties = m.invoke(className, new Object[] { node } );
            } catch (NoSuchMethodException ex) {
            } catch (Exception e) {
                e.printStackTrace();
            }
            buttonClass = cls;
            buttonProperties = properties;
            s = getAttribute(node, "useintoolbars");
            classicToolBarButtons = (s != null ? !Boolean.valueOf(s).booleanValue() : false);
        }
        else
        {
          buttonClass = DefaultToolButton.class;
          buttonProperties = null;
          classicToolBarButtons = false;
        }
        node = getNodeFromNodeList(themeNodeList, "palettemargin");
        paletteMargin = getIntegerValueFromNode(node);
        node = getNodeFromNodeList(themeNodeList, "buttonmargin");
        buttonMargin = getIntegerValueFromNode(node);
        //color sets
        int count = 0;
        for (int i = 0; i < themeNodeList.getLength(); i++) {
            node = themeNodeList.item(i);
            if (node.getNodeName() == "colorset") {
                count++;
            }
        }
        colorSets = new ColorSet[count];
        count = 0;
        for (int i = 0; i < themeNodeList.getLength(); i++) {
            node = themeNodeList.item(i);
            if (node.getNodeName() == "colorset")
                colorSets[count++] = new ColorSet(node);
        }
      }

      public String getName()
      {
        return Translate.text(name);
      }

      public ColorSet[] getColorSets()
      {
        return (ColorSet[]) colorSets.clone();
      }
    }

    private static ThemeInfo selectedTheme, defaultTheme;
    private static ColorSet selectedColorSet;
    private static ThemeInfo[] themeList;
    private static Map themeIdMap;
    private static DocumentBuilderFactory documentBuilderFactory; //XML parsing

    /**
     * Get the currently selected theme.
     */

    public static ThemeInfo getSelectedTheme()
    {
      return selectedTheme;
    }

    /**
     * Set the currently selected theme.
     */

    public static void setSelectedTheme(ThemeInfo theme) {
        selectedTheme = theme;
        setSelectedColorSet(theme.colorSets[0]);
        applyButtonProperties();
    }

    /**
     * Get the currently selected color set.
     */

    public static ColorSet getSelectedColorSet()
    {
      return selectedColorSet;
    }

    /**
     * Set the currently selected color set.
     */

    public static void setSelectedColorSet(ColorSet colorSet)
    {
      selectedColorSet = colorSet;
      applyThemeColors();
    }

    /**
     * Get a list of all available themes.
     */

    public static List getThemes()
    {
      return Collections.unmodifiableList(Arrays.asList(themeList));
    }

    /**
     * Get the default theme.
     */

    public static ThemeInfo getDefaultTheme()
    {
      return defaultTheme;
    }

    private static void applyThemeColors() {
        ColorSet set = selectedColorSet;
        ViewerCanvas.backgroundColor = new Color(set.viewerBackground.getRed(),
                set.viewerBackground.getGreen(),
                set.viewerBackground.getBlue());
        ViewerCanvas.lineColor = new Color(set.viewerLine.getRed(),
                set.viewerLine.getGreen(),
                set.viewerLine.getBlue());
        ViewerCanvas.handleColor = new Color(set.viewerHandle.getRed(),
                set.viewerHandle.getGreen(),
                set.viewerHandle.getBlue());
        ViewerCanvas.highlightColor = new Color(set.viewerHighlight.getRed(),
                set.viewerHighlight.getGreen(),
                set.viewerHighlight.getBlue());
        ViewerCanvas.specialHighlightColor = new Color(set.viewerSpecialHighlight.getRed(),
                set.viewerSpecialHighlight.getGreen(),
                set.viewerSpecialHighlight.getBlue());
        ViewerCanvas.disabledColor = new Color(set.viewerDisabled.getRed(),
                set.viewerDisabled.getGreen(),
                set.viewerDisabled.getBlue());
        Color viewerSurface = new Color(set.viewerSurface.getRed(),
                set.viewerSurface.getGreen(),
                set.viewerSurface.getBlue());
        Color viewerTransparent = new Color(set.viewerTransparent.getRed(),
                set.viewerTransparent.getGreen(),
                set.viewerTransparent.getBlue());
        ViewerCanvas.surfaceColor = viewerSurface;
        ViewerCanvas.surfaceRGBColor = new RGBColor(viewerSurface.getRed()/255.0, viewerSurface.getGreen()/255.0, viewerSurface.getBlue()/255.0);
        ViewerCanvas.transparentColor = new RGBColor(viewerTransparent.getRed()/255.0, viewerTransparent.getGreen()/255.0, viewerTransparent.getBlue()/255.0);
    }

    private static void applyButtonProperties() {
        if (selectedTheme.buttonProperties != null) {
            Class buttonClass = selectedTheme.buttonClass;
            try {
                Method m = buttonClass.getMethod("setProperties", new Class[] { Object.class } );
                m.invoke(buttonClass, new Object[] { selectedTheme.buttonProperties } );
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

  private static URL getIconURL(String name)
    {
      ThemeInfo source = selectedTheme;
      ThemeInfo defaultSource = defaultTheme;
      int colon = name.indexOf(':');
      if (colon > -1)
      {
        defaultSource = (ThemeInfo) themeIdMap.get(name.substring(0, colon));
        name = name.substring(colon+1);
      }
      URL url = null;
      url = source.resource.getClassLoader().getResource(source.pathRoot+name+".png");
      if (url == null)
        url = source.resource.getClassLoader().getResource(source.pathRoot+name+".gif");
      if (url == null && defaultSource != null)
        url = defaultSource.resource.getClassLoader().getResource(defaultSource.pathRoot+name+".png");
      if (url == null && defaultSource != null)
        url = defaultSource.resource.getClassLoader().getResource(defaultSource.pathRoot+name+".gif");
      return url;
    }

    /**
     * Creates a ToolButton according to the current theme
     * @param owner The button owner
     * @param iconName The name of the icon to display on the button, without extension
     * @param selectedIconName The name of the icon to display when the button is selected, without extension
     * @return
     */
    public static ToolButton getToolButton(Object owner, String iconName, String selectedIconName) {
        Class buttonClass = selectedTheme.buttonClass;
        Constructor contructor;
        try {
            contructor = buttonClass.getConstructor(new Class[] { Object.class, String.class, String.class } );
            return (ToolButton) contructor.newInstance( new Object[] { owner, iconName, selectedIconName } );
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Given an icon file name, this method returns the icon according to the
     * currently selected theme. If no such icon is available within the
     * current theme, the icon is looked for in the default theme.
     * This method will first look for a .gif file, then for a.png one.
     *
     * @param iconName The file name of the icon, without extension.
     */
    public static ImageIcon getIcon(String iconName)
    {
      URL url = getIconURL(iconName);
      if (url == null)
        return null;
      return new ImageIcon(url);
    }

    /**
     * Returns the background color of the application (not to be mistaken for the view background)
     */
    public static Color getAppBackgroundColor() {
        return selectedColorSet.appBackground;
    }

    /**
     * Returns the tool palette background color
     */
    public static Color getPaletteBackgroundColor() {
        return selectedColorSet.paletteBackground;
    }

    /**
     * Returns the first color of the dockable widgets title bar gradient painting
     */
    public static Color getDockableBarColor1() {
        return selectedColorSet.dockableBarColor1;
    }

    /**
     * Returns the second color of the dockable widgets title bar gradient painting
     */
    public static Color getDockableBarColor2() {
        return selectedColorSet.dockableBarColor2;
    }

    /**
     * Returns the text color of the dockable widgets title bar text
     */
    public static Color getDockableTitleColor() {
        return selectedColorSet.dockableTitleColor;
    }

    /**
     * Returns the color of the text to use for widgets.
     * Can also be used as foreground color.
     */
    public static Color getTextColor() {
        return selectedColorSet.textColor;
    }

    /**
     * This is invoked during startup to initialize the list of installed themes.
     */

    public static void initThemes()
    {
      if (themeList != null)
        throw new IllegalStateException("The themes have already been initialized.");
      themeIdMap = new HashMap();
      documentBuilderFactory = DocumentBuilderFactory.newInstance();
      List resources = PluginRegistry.getResources("UITheme");
      ArrayList list = new ArrayList();
      for (int i = 0; i < resources.size(); i++)
      {
        try
        {
          ThemeInfo themeInfo = new ThemeInfo((PluginRegistry.PluginResource) resources.get(i));
          list.add(themeInfo);
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
      themeList = (ThemeInfo[]) list.toArray(new ThemeInfo[list.size()]);
      for (int i = 0; i < themeList.length; i++)
        themeIdMap.put(themeList[i].resource.getId(), themeList[i]);
      defaultTheme = themeList[0];
      setSelectedTheme(themeList[0]);
    }

    private static int getIntegerValueFromNode(Node node) {
        if (node != null) {
            String s = getAttribute(node, "value");
            if (s != null) {
                return Integer.parseInt(s);
            }
        }
        return 0;
    }

    private static Color getColorFromNode(Node node) {
        if (node == null) {
            return new Color(0, 0, 0);
        }
        String s = getAttribute(node, "R");
        int r = 0;
        if (s != null) {
            r = Integer.valueOf(s).intValue();
        }
        int g = 0;
        s = getAttribute(node, "G");
        if (s != null) {
            g = Integer.valueOf(s).intValue();
        }
        int b = 0;
        s = getAttribute(node, "B");
        if (s != null) {
            b = Integer.valueOf(s).intValue();
        }
        return new Color(r, g, b);
    }
    /**
     * Returns the palette margin to use for tool palette display
     */
    public static int getPaletteMargin() {
        return selectedTheme.paletteMargin;
    }

    /**
     * Returns the button margin to use for tool palette display
     */
    public static int getButtonMargin() {
        return selectedTheme.buttonMargin;
    }

    /**
     *  Utility for parsing XML Documents: gets a named node from a node list. Returns null if the node does not
     *  exist.
     *
     *@param  nl        The node list
     *@param  nodeName  The node name
     *@return           The node named nodeName
     */
    private static Node getNodeFromNodeList( NodeList nl, String nodeName )
    {
        for ( int i = 0; i < nl.getLength(); ++i )
        {
            Node n = nl.item( i );
            if ( n.getNodeName().equals( nodeName ) )
            {
                return n;
            }
        }
        return null;
    }

    /**
     *  Utility for parsing XML Documents: gets a named attribute value from a node
     *
     *@param  name  The attribute name
     *@param  node  Description of the Parameter
     *@return       The attribute value
     */
    private static String getAttribute( Node node, String name )
    {
        NamedNodeMap nm = node.getAttributes();
        if ( nm == null )
            return null;
        Node nn = nm.getNamedItem( name );
        if ( nn == null )
            return null;
        return nn.getNodeValue();
    }
}
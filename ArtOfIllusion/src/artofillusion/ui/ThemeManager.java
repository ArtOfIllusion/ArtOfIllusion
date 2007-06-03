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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
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
	public class ColorSet {
		Color appBackground;
		Color paletteBackground;
		Color viewerBackground;
	    Color viewerLine;
	    Color viewerHandle;
	    Color viewerHighlight;
	    Color viewerSpecialHighlight;
	    Color viewerDisabled;
	    Color viewerSurface;
	    Color viewerTransparent;
	    Color dockableBarColor1;
	    Color dockableBarColor2;
	    Color dockableTitleColor;
	    Color textColor;
	    String name;
	}

	/**
	 * This class stores information about a theme. This can be general purpose information such as
	 * theme content and author, or very specific information such as the button styling for
	 * this theme.
	 *
	 * @author Francois Guillet
	 *
	 */
	public class ThemeInfo {
		String name;
		String author;
		String description;
		//the name of the button class for this theme.
		String buttonClass;
		//this is the button style parameters for this theme.
		//Relevant XML node is passed onto the button class so it can parse
		//it and deliver button style parameters the theme will give the buttons
		//whenever it is selected.
		Object buttonProperties;
		//button margin is the space around each button
		int buttonMargin;
		//palette margin is the space around the buttons
		int paletteMargin;
		//the currently selected color set for this theme
		int set;
		//the theme colorsets
		ColorSet[] colorSets;
		boolean classicToolBarButtons = true;
		//the file holding the theme contents
		File file;
        PluginRegistry.PluginResource resource;
        String pathRoot;
    }

	private static ThemeManager themeManager = null;
	private static ArrayList buttonClasses;
    private ThemeInfo selectedTheme, defaultTheme;
    int selected;
	private ThemeInfo[] themeList;
    private Map themeNameMap;
	private boolean customize; //true if custom settings override theme settings
	private Class customButton; //custom button class
	private int paletteMargin, buttonMargin; //custom margin settings
	private Color appBackground; //custom colors
	private Color paletteBackground;
	private Color viewerBackground;
	private Color viewerLine;
	private Color viewerHandle;
	private Color viewerHighlight;
	private Color viewerSpecialHighlight;
	private Color viewerDisabled;
	private Color viewerSurface;
	private Color viewerTransparent;
    private Color dockableBarColor1;
    private Color dockableBarColor2;
    private Color dockableTitleColor;
    private Color textColor;
    private DocumentBuilderFactory documentBuilderFactory; //XML parsing
	private boolean toolbarIsClassic;

	private ThemeManager() {
		super();
		buttonClasses = new ArrayList();
        themeNameMap = new HashMap();
        //loading button classes.
		//they could also be loaded from plugins/themes if need be
			buttonClasses.add(DefaultToolButton.class);
		documentBuilderFactory = DocumentBuilderFactory.newInstance();
		initThemes();
        defaultTheme = themeList[0];
        customize = false;
		toolbarIsClassic = true;
		setSelectedTheme(themeList[0]);
	}

    public ThemeInfo getSelectedTheme()
    {
      return selectedTheme;
    }

    public void setSelectedTheme(ThemeInfo theme) {
        selectedTheme = theme;
        customButton = (Class) buttonClasses.get(getThemeButton());
        paletteMargin = selectedTheme.paletteMargin;
        buttonMargin = selectedTheme.buttonMargin;
        toolbarIsClassic = selectedTheme.classicToolBarButtons;
        applyThemeColors();
        applyButtonProperties();
	}

    public List getThemes()
    {
      return Arrays.asList(themeList);
    }

    private void applyThemeColors() {
		ColorSet set = selectedTheme.colorSets[selectedTheme.set];
		appBackground = new Color(set.appBackground.getRed(),
				set.appBackground.getGreen(),
				set.appBackground.getBlue());
		paletteBackground = new Color(set.paletteBackground.getRed(),
				set.paletteBackground.getGreen(),
				set.paletteBackground.getBlue());
		viewerBackground = new Color(set.viewerBackground.getRed(),
				set.viewerBackground.getGreen(),
				set.viewerBackground.getBlue());
		viewerLine = new Color(set.viewerLine.getRed(),
				set.viewerLine.getGreen(),
				set.viewerLine.getBlue());
		viewerHandle = new Color(set.viewerHandle.getRed(),
				set.viewerHandle.getGreen(),
				set.viewerHandle.getBlue());
		viewerHighlight = new Color(set.viewerHighlight.getRed(),
				set.viewerHighlight.getGreen(),
				set.viewerHighlight.getBlue());
		viewerSpecialHighlight = new Color(set.viewerSpecialHighlight.getRed(),
				set.viewerSpecialHighlight.getGreen(),
				set.viewerSpecialHighlight.getBlue());
		viewerDisabled = new Color(set.viewerDisabled.getRed(),
				set.viewerDisabled.getGreen(),
				set.viewerDisabled.getBlue());
		viewerSurface = new Color(set.viewerSurface.getRed(),
				set.viewerSurface.getGreen(),
				set.viewerSurface.getBlue());
		viewerTransparent = new Color(set.viewerTransparent.getRed(),
				set.viewerTransparent.getGreen(),
				set.viewerTransparent.getBlue());
		ViewerCanvas.backgroundColor = viewerBackground;
	    ViewerCanvas.lineColor = viewerLine;
	    ViewerCanvas.handleColor = viewerHandle;
	    ViewerCanvas.highlightColor = viewerHighlight;
	    ViewerCanvas.specialHighlightColor = viewerSpecialHighlight;
	    ViewerCanvas.disabledColor = viewerDisabled;
	    ViewerCanvas.surfaceColor = viewerSurface;
	    ViewerCanvas.surfaceRGBColor = new RGBColor(viewerSurface.getRed()/255.0, viewerSurface.getGreen()/255.0, viewerSurface.getBlue()/255.0);
	    ViewerCanvas.transparentColor = new RGBColor(viewerTransparent.getRed()/255.0, viewerTransparent.getGreen()/255.0, viewerTransparent.getBlue()/255.0);
	    dockableBarColor1 = set.dockableBarColor1;
	    dockableBarColor2 = set.dockableBarColor2;
	    dockableTitleColor = set.dockableTitleColor;
	    textColor = set.textColor;
	}

	private void applyButtonProperties() {
		if (selectedTheme.buttonProperties != null) {
	    	Class buttonClass = (Class)buttonClasses.get(getThemeButton());
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

	public static ThemeManager getThemeManager() {
		if (themeManager == null) {
			themeManager = new ThemeManager();
		}
		return themeManager;
	}

	private URL getIconURL(String name)
    {
      ThemeInfo source = selectedTheme;
      ThemeInfo defaultSource = defaultTheme;
      int colon = name.indexOf(':');
      if (colon > -1)
      {
        defaultSource = (ThemeInfo) themeNameMap.get(name.substring(0, colon));
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
	 * Creates a palette button according to the current theme
	 * @param owner The button owner
	 * @param iconName The name of the icon to display on the button, without extension
	 * @return
	 */
	public ToolButton getPaletteButton(Object owner, String iconName) {
		Class buttonClass;
		if (customize) {
			buttonClass = customButton;
		} else {
			buttonClass = (Class)buttonClasses.get(getThemeButton());
		}
		Constructor contructor;
		try {
			contructor = buttonClass.getConstructor(new Class[] { Object.class, String.class } );
			return (ToolButton) contructor.newInstance( new Object[] { owner, iconName } );
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
	 * Creates a toolbar button according to the current theme
	 * @param owner The button owner
	 * @param iconName The name of the icon to display on the button, without extension
	 * @return
	 */
	public ToolButton getToolBarButton(Object owner, String iconName) {
		boolean classic = selectedTheme.classicToolBarButtons;
		if (customize) {
			classic = toolbarIsClassic;
		}
		if (classic) {
			return new DefaultToolButton(owner, iconName);
		} else {
			return getPaletteButton(owner, iconName);
		}
	}

	/**
	 * Given an icon file name, this method returns the icon according to the
	 * currently selected theme. If no such icon is available within the
	 * current theme, the icon is looked for in the default theme.
	 * This method will first look for a .gif file, then for a.png one.
	 *
	 * @param iconName The file name of the icon, without extension.
	 */
	public ImageIcon getIcon(String iconName)
    {
      URL url = getIconURL(iconName);
      if (url == null)
        return null;
      return new ImageIcon(url);
	}

	/**
	 * Returns the background color of the application (not to be mistaken for the view background)
	 */
	public Color getAppBackgroundColor() {
		Color color;
		if (customize) {
			color = appBackground;
		} else {
			color = selectedTheme.colorSets[selectedTheme.set].appBackground;
		}
		return color;
	}

	/**
	 * Returns the tool palette background color
	 */
	public Color getPaletteBackgroundColor() {
		Color color;
		if (customize) {
			color = paletteBackground;
		} else {
			color = selectedTheme.colorSets[selectedTheme.set].paletteBackground;
		}
		return color;
	}

	/**
	 * Returns the first color of the dockable widgets title bar gradient painting
	 */
	public Color getDockableBarColor1() {
		Color color;
		if (customize) {
			color = dockableBarColor1;
		} else {
			color = selectedTheme.colorSets[selectedTheme.set].dockableBarColor1;
		}
		return color;
	}

	/**
	 * Returns the second color of the dockable widgets title bar gradient painting
	 */
	public Color getDockableBarColor2() {
		Color color;
		if (customize) {
			color = dockableBarColor2;
		} else {
			color = selectedTheme.colorSets[selectedTheme.set].dockableBarColor2;
		}
		return color;
	}

	/**
	 * Returns the text color of the dockable widgets title bar text
	 */
	public Color getDockableTitleColor() {
		Color color;
		if (customize) {
			color = dockableTitleColor;
		} else {
			color = selectedTheme.colorSets[selectedTheme.set].dockableTitleColor;
		}
		return color;
	}

	/**
	 * Returns the color of the text to use for widgets.
	 * Can also be used as foreground color.
	 */
	public Color getTextColor() {
		Color color;
		if (customize) {
			color = textColor;
		} else {
			color = selectedTheme.colorSets[selectedTheme.set].textColor;
		}
		return color;
	}

    public void initThemes()
    {
      List resources = PluginRegistry.getResources("UITheme");
      ArrayList list = new ArrayList();
      ThemeInfo themeInfo = null;
      for (int i = 0; i < resources.size(); i++)
      {
        themeInfo = loadThemeInfo((PluginRegistry.PluginResource) resources.get(i));
        list.add(themeInfo);
      }
      themeList = (ThemeInfo[]) list.toArray(new ThemeInfo[list.size()]);
      for (int i = 0; i < themeList.length; i++)
        themeNameMap.put(themeList[i].name, themeList[i]);
    }

	private ThemeInfo loadThemeInfo(PluginRegistry.PluginResource resource) {
		ThemeInfo info = null;
		try {
			InputStream is = resource.getInputStream();
			DocumentBuilder builder;
			builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse( is );
            is.close();
            Node rootNode = document.getDocumentElement();
            NodeList themeNodeList = rootNode.getChildNodes();
            info = new ThemeInfo();
            info.resource = resource;
            info.pathRoot = info.resource.getName();
            if (info.pathRoot.lastIndexOf('/') > -1)
              info.pathRoot = info.pathRoot.substring(0, info.pathRoot.lastIndexOf('/')+1);
            else
              info.pathRoot = "";
            String s;
            Node node = getNodeFromNodeList(themeNodeList, "name");
            if (node != null) {
            	info.name = node.getFirstChild().getNodeValue();
            }
            node = getNodeFromNodeList(themeNodeList, "author");
            if (node != null) {
            	info.author = node.getFirstChild().getNodeValue();
            }
            node = getNodeFromNodeList(themeNodeList, "description");
            if (node != null) {
            	info.description = node.getFirstChild().getNodeValue();
            }
            node = getNodeFromNodeList(themeNodeList, "button");
            if (node != null) {
            	info.buttonClass = getAttribute(node, "class");
            	Class buttonClass = (Class)buttonClasses.get(getThemeButton(info.buttonClass));
            	try {
					Method m = buttonClass.getMethod("readPropertiesFromXMLNode", new Class[] { Node.class } );
					info.buttonProperties = m.invoke(buttonClass, new Object[] { node } );
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
				s = getAttribute(node, "useintoolbars");
				if (s != null) {
					info.classicToolBarButtons = ! Boolean.parseBoolean(s);
				}
            }
            node = getNodeFromNodeList(themeNodeList, "palettemargin");
            info.paletteMargin = getIntegerValueFromNode(node);
            node = getNodeFromNodeList(themeNodeList, "buttonmargin");
            info.buttonMargin = getIntegerValueFromNode(node);
            //color sets
            int count = 0;
            for (int i = 0; i < themeNodeList.getLength(); i++) {
            	node = themeNodeList.item(i);
            	if (node.getNodeName() == "colorset") {
            		count++;
            	}
            }
            info.colorSets = new ColorSet[count];
            count = 0;
            NodeList list;
            for (int i = 0; i < themeNodeList.getLength(); i++) {
            	node = themeNodeList.item(i);
            	if (node.getNodeName() == "colorset") {
            		info.colorSets[count] = new ColorSet();
            		info.colorSets[count].name = getAttribute(node, "name");
            		list = node.getChildNodes();
            		node = getNodeFromNodeList(list, "applicationbackground");
                    info.colorSets[count].appBackground = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "palettebackground");
                    info.colorSets[count].paletteBackground = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewerbackground");
                    info.colorSets[count].viewerBackground = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewerline");
                    info.colorSets[count].viewerLine = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewerhandle");
                    info.colorSets[count].viewerHandle = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewerhighlight");
                    info.colorSets[count].viewerHighlight = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewerspecialhighlight");
                    info.colorSets[count].viewerSpecialHighlight = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewerdisabled");
                    info.colorSets[count].viewerDisabled = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewersurface");
                    info.colorSets[count].viewerSurface = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "viewertransparent");
                    info.colorSets[count].viewerTransparent = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "dockablebarcolor1");
                    info.colorSets[count].dockableBarColor1 = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "dockablebarcolor2");
                    info.colorSets[count].dockableBarColor2 = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "dockabletitlecolor");
                    info.colorSets[count].dockableTitleColor = getColorFromNode(node);
                    node = getNodeFromNodeList(list, "textcolor");
                    info.colorSets[count].textColor = getColorFromNode(node);
                    count++;
            	}
            }
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return info;
	}

	public static int getIntegerValueFromNode(Node node) {
		if (node != null) {
        	String s = getAttribute(node, "value");
        	if (s != null) {
        		return Integer.parseInt(s);
        	}
        }
		return 0;
	}

	public static Color getColorFromNode(Node node) {
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
	 * Returns the button class index of the given string
	 */
	private int getThemeButton(String button) {
		for (int i = 0; i < buttonClasses.size(); i++) {
			if (((Class)buttonClasses.get(i)).getName().contains(button)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the button class index of the selected theme
	 */
	private int getThemeButton() {
		return getThemeButton(selectedTheme.buttonClass);
	}

	/**
	 * Returns the palette margin to use for tool palette display
	 */
	public int getPaletteMargin() {
		int margin;
		if (customize) {
			margin = paletteMargin;
		} else {
			margin = selectedTheme.paletteMargin;
		}
		return margin;
	}

	/**
	 * Returns the button margin to use for tool palette display
	 */
	public int getButtonMargin() {
		int margin;
		if (customize) {
			margin = buttonMargin;
		} else {
			margin = selectedTheme.buttonMargin;
		}
		return margin;
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
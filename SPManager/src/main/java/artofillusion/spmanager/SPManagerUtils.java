/*
 *  This class implements utilities
 */
/*
 *  Copyright (C) 2003 by Francois Guillet
 *  This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 2 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */
package artofillusion.spmanager;

import java.awt.*;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import java.io.*;
import java.util.zip.*;
import artofillusion.*;
import artofillusion.ui.*;

/**
 *  Description of the Class
 *
 *@author     pims
 *@created    6 juillet 2004
 */
public class SPManagerUtils
{
    public static DocumentBuilderFactory factory;
    public static DocumentBuilder builder;

    static {
        factory = DocumentBuilderFactory.newInstance();
        try
        {
            builder = factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     *  Description of the Method
     *
     *@param  intArray  Description of the Parameter
     *@return           Description of the Return Value
     */
    public static int[] increaseIntArray( int[] intArray )
    {
        if ( intArray == null )
        {
            int[] tmpArray = new int[1];
            return tmpArray;
        }
        else
        {
            int[] tmpArray = new int[intArray.length + 1];
            System.arraycopy( intArray, 0, tmpArray, 0, intArray.length );
            return tmpArray;
        }
    }


    /**
     *  Description of the Method
     *
     *@param  intArray  Description of the Parameter
     *@param  element   Description of the Parameter
     *@return           Description of the Return Value
     */
    public static int[] deleteIntArrayElement( int[] intArray, int element )
    {
        if ( intArray == null )
            return null;
        else if ( intArray.length == 1 )
            return null;
        else
        {
            int[] tmpArray = new int[intArray.length - 1];
            if ( element > 0 )
                System.arraycopy( intArray, 0, tmpArray, 0, element );
            if ( element < intArray.length - 1 )
                System.arraycopy( intArray, element + 1, tmpArray, element, tmpArray.length - element );
            int i;
            return tmpArray;
        }
    }


    /**
     *  Sets the dialogLocation attribute of the SPManagerUtils class
     *
     *@param  frame        The new dialogLocation value
     *@param  parentFrame  The new dialogLocation value
     */
    public static void setDialogLocation( JFrame frame, JFrame parentFrame )
    {
        Point location = new Point();
        location.x = parentFrame.getLocation().x + parentFrame.getWidth() / 2 - frame.getWidth() / 2;
        location.y = parentFrame.getLocation().y + parentFrame.getHeight() / 2 - frame.getHeight() / 2;
        if ( location.x < 0 )
            location.x = 0;
        if ( location.y < 0 )
            location.y = 0;
        frame.setLocation( location );
    }


    /**
     *  Description of the Method
     */
    public static void updateAllAoIWindows()
    {
	try {
	    EditingWindow allWindows[] = ArtOfIllusion.getWindows();
	    for ( int i = 0; i < allWindows.length; i++ )
		if ( allWindows[i] instanceof LayoutWindow )
		    ( (LayoutWindow) allWindows[i] ).rebuildScriptsMenu();
	} catch (Throwable t) {}
    }


    /**
     *  Gets the jarFileContent attribute of the SPManagerUtils class
     *
     *@param  filename  Description of the Parameter
     *@return           The jarFileContent value
     */
    public static byte[] getJarFileContent( String filename )
    {
        File dir = new File( SPManagerPlugin.PLUGIN_DIRECTORY );
        if ( dir.exists() )
        {
            String[] files = dir.list();
            for ( int i = 0; i < files.length; i++ )
                if ( files[i].startsWith( "SPManager" ) )
                {
                    ZipFile zf = null;
                    try
                    {
                        zf = new ZipFile( new File( dir, files[i] ) );
                    }
                    catch ( IOException ex )
                    {
                        continue;
                        // Not a zip file.
                    }
                    if ( zf != null )
                    {
                        ZipEntry ze = zf.getEntry( filename );
                        if ( ze != null )
                        {
                            int size = (int) ze.getSize();
                            byte data[] = new byte[size];
                            try
                            {
                                BufferedInputStream in = new BufferedInputStream( zf.getInputStream( ze ) );
                                for ( int j = 0; j < size; j++ )
                                    data[j] = (byte) in.read();
                                in.close();
                            }
                            catch ( IOException ex )
                            {
                                System.out.println( "IOException in getJarFileContent" );
                            }
                            return data;
                        }
                        else
                            System.out.println( "File " + filename + " not found within zip file" );
                    }
                }
        }
        else
        {
            System.out.println( "Dir does not exist" );
        }
        return null;
    }


    /**
     *  Gets the jarFileContent attribute of the SPManagerUtils class
     *
     *@param  jarPath   Description of the Parameter
     *@param  filename  Description of the Parameter
     *@return           The jarFileContent value
     */
    public static byte[] getJarFileContent( String jarPath, String filename )
    {
        ZipFile zf = null;
        try
        {
            zf = new ZipFile( new File( jarPath ) );
        }
        catch ( IOException ex )
        {
            return null;
            // Not a zip file.
        }
        if ( zf != null )
        {
            ZipEntry ze = zf.getEntry( filename );
            if ( ze != null )
            {
                int size = (int) ze.getSize();
                byte data[] = new byte[size];
                try
                {
                    BufferedInputStream in = new BufferedInputStream( zf.getInputStream( ze ) );
                    for ( int j = 0; j < size; j++ )
                        data[j] = (byte) in.read();
                    in.close();
                }
                catch ( IOException ex )
                {
                    System.out.println( "IOException in getJarFileContent" );
                }
                return data;
            }
            else
                System.out.println( "File " + filename + " not found within zip file" );
        }
        return null;
    }

    /**
     *  Gets a named node from a node list. Returns null if the node does not
     *  exist.
     *
     *@param  nl        The node list
     *@param  nodeName  The node name
     *@return           The node named nodeName
     */
    public static Node getNodeFromNodeList( NodeList nl, String nodeName,
														  int index )
    {
        for ( int i = 0; i < nl.getLength(); ++i )
        {
            Node n = nl.item( i );
            if ( n.getNodeName().equals( nodeName ) )
            {
                if (index-- == 0) return n;
            }
        }
        return null;
    }

    /**
     *  Gets a named attribute value from a node
     *
     *@param  name  The attribute name
     *@param  node  Description of the Parameter
     *@return       The attribute value
     */
    public static String getAttribute( Node node, String name )
    {
        NamedNodeMap nm = node.getAttributes();
        if ( nm == null )
            return null;
        Node nn = nm.getNamedItem( name );
        if ( nn == null )
            return null;
        return nn.getNodeValue();
    }


    /**
     *  Gets a value from a named child node
     *
     *@param  name  The child node name
     *@param  node  The node
     *@return       The attribute value
     */
    public static String getNodeValue( Node node, String name,
													String defaultVal, int index)
    {
        NodeList nl = node.getChildNodes();
        if ( nl.getLength() == 0 ) return defaultVal;

        Node n = getNodeFromNodeList( nl, name, index );
        if ( n == null ) return defaultVal;

	n = n.getChildNodes().item(0);
	if (n == null) return defaultVal;

        String value = n.getNodeValue();
        if (value == null) value = defaultVal;

        return value;
    }

    /**
     *  method to parse digits into an int
     */
    public static int parseInt(String val, int start, int max)
	throws NumberFormatException
    {
	if (val == null || val.length() <= start) return 0;

	if (max < 0 || max > val.length()) max = val.length();

	char c;

	// throw exception if the value cannot be a number
	c = val.charAt(start);
	if (! (Character.isDigit(c) || "+-".indexOf(c) >= 0))
	    throw new NumberFormatException(val);

	int result = 0;
	for (int i = start; i < max; i++) {
	    c = val.charAt(i);
	    if (Character.isDigit(c))
		result = (result*10) + Character.digit(c, 10);
	    else break;
	}

	return result;
    }

    /**
     *  parse a double value from a String.
     *
     *  The string is parsed from the first (zeroeth) char up to the first
     *  char which is not valid in a double representation
     *  (ie digit, '.', 'e', 'E', '+', or '-').
     */
    public static double parseDouble(String val)
	throws NumberFormatException
    {
	if (val == null || val.length() == 0) return 0;

	char c;

	// throw exception if the value cannot be a number
	c = val.charAt(0);
	if (! (Character.isDigit(c) || "+-.".indexOf(c) >= 0))
	    throw new NumberFormatException(val);

	long result = 0;
	long frac = 0;
	int exp = 0;
	boolean mantissa = true;
	int sign = 1, esign = 1;

	int max = val.length();
	for (int i = 0; i < max; i++) {
	    c = val.charAt(i);

	    if (result == 0 && (c == '-' || c == '+'))
		sign = (c == '-' ? -1 : 1);
	    else if (frac == 0 && c == '.')
		frac = 1;
	    else if (mantissa == true && (c == 'e' || c == 'E'))
		mantissa = false;
	    else if (Character.isDigit(c)) {
		if (mantissa) {
		    result = (result*10) + Character.digit(c, 10);

		    if (frac > 0) frac *= 10;
		    System.out.println("test: c=" + c + "; frac=" + frac);
		}
		else {
		    if (exp == 0 && (c == '-' || c == '+'))
			esign = (c == '-' ? -1 : 1);
		    else
			exp = (exp*10) + Character.digit(c, 10);
		}
	    }
	    else
		break;
	}

	if (frac == 0) frac = 1;

	System.out.println("parseDouble: esign:" + esign + "; exp:" + exp
			   + "; sign:" + sign + "; result:" + result
			   + "; frac:" + frac);

	return (Math.pow(10.0, esign * exp) * sign * result)/frac;
    }

    /**
     *  parse a version string into a numeric value.
     *
     *  Each component is parsed as an int and scaled into a 3-digit
     *  column.
     *<br>Eg 1.2 is parsed into 1002; 1.20 is parsed into 1020; and 1.20.3
     *  is parsed into 1020003
     *
     */
    public static long parseVersion(String val)
	throws NumberFormatException
    {
	long result = parseInt(val, 0, -1);

	int pos = 0;
	while ((pos = val.indexOf('.', pos)) >= 0) {
	    result = (result*1000) + parseInt(val, pos+1, -1);
	    pos++;
	}

	return result;
    }
}


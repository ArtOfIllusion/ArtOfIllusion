/*
 *  Copyright (C) 2003 by Francois Guillet
 *  Copyright (C) 2003 by Peter Eastman for original Translate.java code
 *  This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 2 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */
package artofillusion.spmanager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import buoy.event.*;
import buoy.widget.*;
//import artofillusion.ui.*;
import artofillusion.*;
import java.io.*;

/**
 *  This class extends AoI Translate Class so that i) the spmanager properties
 *  file is used instead of AoI properties file ii) buoy objects are returned
 *
 *@author     pims
 *@created    27 mai 2004
 */

public class SPMTranslate
{
    private static Locale locale = Locale.getDefault();
    private static ResourceBundle resources;


    /**
     *  Set the locale to be used for generating text.
     *
     *@param  l  The new locale value
     */

    public static void setLocale( Locale l )
    {
        locale = l;
/*
        File dir = new File( ModellingApp.PLUGIN_DIRECTORY );
        if ( dir.exists() )
        {
            String[] files = dir.list();
            for ( int i = 0; i < files.length; i++ )
                if ( files[i].startsWith( "SPManager" ) )
                {
                    ZipFile zf = null;
                    try
                    {
                        zf = new ZipFile( new File( ModellingApp.PLUGIN_DIRECTORY, files[i] ) );
                    }
                    catch ( IOException ex )
                    {
                        continue;
                        // Not a zip file.
                    }
                    if ( zf != null )
                    {
                        JarClassLoader jcl = new JarClassLoader( zf );
                        resources = ResourceBundle.getBundle( "spmanager", locale, jcl );
                    }
                }
        }
        else
        {
            System.out.println( "Dir does not exist" );
        }*/
        resources = ResourceBundle.getBundle("spmanager", locale);
    }


    /**
     *  Get the locale currently used for generating text.
     *
     *@return    The locale value
     */

    public static Locale getLocale()
    {
        return locale;
    }


    /**
     *  Get a Menu whose text is given by the property "menu.(name)".
     *
     *@param  name  Description of the Parameter
     *@return       Description of the Return Value
     */

    public static BMenu bMenu( String name )
    {
        try
        {
            return new BMenu( resources.getString( "menu." + name ) );
        }
        catch ( MissingResourceException ex )
        {
            return new BMenu( name );
        }
    }


    /**
     *  Get a BMenuItem whose text is given by the property "menu.(name)". This
     *  checks for a property called "menu.shortcut.(name)", and if it is found,
     *  sets the menu shortcut accordingly. No event link is added.
     *
     *@param  name  Description of the Parameter
     *@return       Description of the Return Value
     */

    public static BMenuItem bMenuItem( String name )
    {
        return bMenuItem( name, null, null, null );
    }


    /**
     *  Description of the Method
     *
     *@param  name    Description of the Parameter
     *@param  target  Description of the Parameter
     *@param  method  Description of the Parameter
     *@return         Description of the Return Value
     */
    public static BMenuItem bMenuItem( String name, java.lang.Object target, String method )
    {
        return bMenuItem( name, CommandEvent.class, target, method );
    }


    /**
     *  Description of the Method
     *
     *@param  name       Description of the Parameter
     *@param  eventType  Description of the Parameter
     *@param  target     Description of the Parameter
     *@param  method     Description of the Parameter
     *@return            Description of the Return Value
     */
    public static BMenuItem bMenuItem( String name, java.lang.Class eventType, java.lang.Object target, String method )
    {
        String command = name;
        BMenuItem item = null;
        try
        {
            command = resources.getString( "menu." + name );
            String shortcut = resources.getString( "menu." + name + ".shortcut" );
            if ( shortcut.length() > 1 && shortcut.charAt( 0 ) == '^' )
                item = new BMenuItem( command, new Shortcut( shortcut.charAt( 1 ), Shortcut.SHIFT_MASK | Shortcut.DEFAULT_MASK ) );
            else if ( shortcut.length() > 0 )
                item = new BMenuItem( command, new Shortcut( shortcut.charAt( 0 ) ) );
        }
        catch ( MissingResourceException ex )
        {
            item = new BMenuItem( command );
        }
        if ( eventType != null )
            item.addEventLink( eventType, target, method );
        return item;
    }


    /**
     *  Get a BCheckboxMenuItem whose text is given by the property
     *  "checkbox.(name)". State specifies the initial state of the item.
     *
     *@param  name   Description of the Parameter
     *@param  state  Description of the Parameter
     *@return        Description of the Return Value
     */

    public static BCheckBoxMenuItem bCheckBoxMenuItem( String name, boolean state )
    {
        String command = name;
        try
        {
            command = resources.getString( "menu." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        BCheckBoxMenuItem item = new BCheckBoxMenuItem( command, state );
        return item;
    }


    /**
     *  Get a BButton whose text is given by the property "button.(name)".
     *
     *@param  name  Description of the Parameter
     *@return       Description of the Return Value
     */

    public static BButton bButton( String name )
    {
        return bButton( name, null, null, null );
    }


    /**
     *  Description of the Method
     *
     *@param  name    Description of the Parameter
     *@param  target  Description of the Parameter
     *@param  method  Description of the Parameter
     *@return         Description of the Return Value
     */
    public static BButton bButton( String name, java.lang.Object target, String method )
    {
        return bButton( name, CommandEvent.class, target, method );
    }


    /**
     *  Description of the Method
     *
     *@param  name       Description of the Parameter
     *@param  eventType  Description of the Parameter
     *@param  target     Description of the Parameter
     *@param  method     Description of the Parameter
     *@return            Description of the Return Value
     */
    public static BButton bButton( String name, java.lang.Class eventType, Object target, String method )
    {
        String command = name;
        try
        {
            command = resources.getString( "button." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        BButton button = new BButton( command );
        if ( eventType != null )
            button.addEventLink( eventType, target, method );
        return button;
    }


    /**
     *  Get a BRadioButton whose text is given by the property "radio.(name)".
     *  group is the RadioButtonGroup the radio button created button will
     *  belong to.
     *
     *@param  name   Description of the Parameter
     *@param  state  Description of the Parameter
     *@param  group  Description of the Parameter
     *@return        Description of the Return Value
     */

    public static BRadioButton bRadioButton( String name, boolean state, RadioButtonGroup group )
    {
        return bRadioButton( name, state, group, null, null );
    }


    /**
     *  Description of the Method
     *
     *@param  name    Description of the Parameter
     *@param  state   Description of the Parameter
     *@param  group   Description of the Parameter
     *@param  target  Description of the Parameter
     *@param  method  Description of the Parameter
     *@return         Description of the Return Value
     */
    public static BRadioButton bRadioButton( String name, boolean state, RadioButtonGroup group, Object target, String method )
    {
        String command = name;
        try
        {
            command = resources.getString( "radio." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        BRadioButton radio = new BRadioButton( command, state, group );
        //radio.setText(command);
        //radio.setState(state);
        if ( target != null )
            group.addEventLink( SelectionChangedEvent.class, target, method );
        return radio;
    }


    /**
     *  These functions are for compatibility with the AWT preview window
     *
     *@param  name      Description of the Parameter
     *@param  listener  Description of the Parameter
     *@return           Description of the Return Value
     */

    public static Button button( String name, ActionListener listener )
    {
        return button( name, null, listener );
    }


    /**
     *  These functions are for compatibility with the AWT preview window
     *
     *@param  name      Description of the Parameter
     *@param  suffix    Description of the Parameter
     *@param  listener  Description of the Parameter
     *@return           Description of the Return Value
     */
    public static Button button( String name, String suffix, ActionListener listener )
    {
        String command = name;
        try
        {
            command = resources.getString( "button." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        if ( suffix != null )
            command += suffix;
        Button b = new Button( command );
        if ( listener != null )
        {
            b.setActionCommand( name );
            b.addActionListener( listener );
        }
        return b;
    }


    /**
     *  returns a BCheckBox whose label is given by the property
     *  "checkbox.name". state is the initial state of the checkbox
     *
     *@param  name   Description of the Parameter
     *@param  state  Description of the Parameter
     *@return        Description of the Return Value
     */
    public static BCheckBox bCheckBox( String name, boolean state )
    {
        return bCheckBox( name, state, null, null );
    }


    /**
     *  Description of the Method
     *
     *@param  name    Description of the Parameter
     *@param  state   Description of the Parameter
     *@param  target  Description of the Parameter
     *@param  method  Description of the Parameter
     *@return         Description of the Return Value
     */
    public static BCheckBox bCheckBox( String name, boolean state, Object target, String method )
    {
        String command = name;
        try
        {
            command = resources.getString( "checkbox." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        BCheckBox b = new BCheckBox( command, state );
        if ( target != null )
            b.addEventLink( ValueChangedEvent.class, target, method );
        return b;
    }


    /**
     *  Get a Label whose text is given by the property "label.name". If the
     *  property is not found, this simply uses name.
     *
     *@param  name  Description of the Parameter
     *@return       Description of the Return Value
     */
    public static BLabel bLabel( String name )
    {
        return bLabel( name, null );
    }


    /**
     *  Description of the Method
     *
     *@param  name  Description of the Parameter
     *@param  args  Description of the Parameter
     *@return       Description of the Return Value
     */
    public static BLabel bLabel( String name, Object[] args )
    {
        try
        {
            name = resources.getString( "label." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        if ( args != null )
        {
            return new BLabel( MessageFormat.format( name, args ) );
        }
        return new BLabel( name );
    }


    /**
     *  Get the text given by the property "name". If the property is not found,
     *  this simply returns name.
     *
     *@param  name  Description of the Parameter
     *@return       Description of the Return Value
     */

    public static String text( String name )
    {
        try
        {
            return resources.getString( "text." + name );
        }
        catch ( MissingResourceException ex )
        {
            return name;
        }
    }


    /**
     *  Get the text given by the property "name". If the property is not found,
     *  this simply uses name. Any occurrance of the pattern "{0}" in the text
     *  string will be replaced with the string representation of arg1.
     *
     *@param  name  Description of the Parameter
     *@param  arg1  Description of the Parameter
     *@return       Description of the Return Value
     */

    public static String text( String name, Object arg1 )
    {
        String pattern = name;
        try
        {
            pattern = resources.getString( "text." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        return MessageFormat.format( pattern, new Object[]{arg1} );
    }


    /**
     *  Get the text given by the property "name". If the property is not found,
     *  this simply uses name. Any occurrances of the patterns "{0}" and "{1}"
     *  in the text string will be replaced with the strings representations of
     *  arg1 and arg2, respectively.
     *
     *@param  name  Description of the Parameter
     *@param  arg1  Description of the Parameter
     *@param  arg2  Description of the Parameter
     *@return       Description of the Return Value
     */

    public static String text( String name, Object arg1, Object arg2 )
    {
        String pattern = name;
        try
        {
            pattern = resources.getString( "text." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        return MessageFormat.format( pattern, new Object[]{arg1, arg2} );
    }


    /**
     *  Get the text given by the property "name". If the property is not found,
     *  this simply uses name. That string and the args array are then passed to
     *  MessageFormat.format() so that any variable fields can be replaced with
     *  the correct values.
     *
     *@param  name  Description of the Parameter
     *@param  args  Description of the Parameter
     *@return       Description of the Return Value
     */

    public static String text( String name, Object args[] )
    {
        String pattern = name;
        try
        {
            pattern = resources.getString( "text." + name );
        }
        catch ( MissingResourceException ex )
        {
        }
        return MessageFormat.format( pattern, args );
    }
}


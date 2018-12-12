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

import artofillusion.ui.Translate;
import buoy.event.*;
import buoy.widget.*;
import java.text.*;
import java.util.*;


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
        resources = ResourceBundle.getBundle("spmanager", locale);
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
    public static BLabel bLabel(String name)
    {
        return bLabel(name, null);
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
        return Translate.text("spmanager:" + "text." + name);
    }


    /**
     *  Get the text given by the property "name". If the property is not found,
     *  this simply uses name. Any occurrence of the pattern "{0}" in the text
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


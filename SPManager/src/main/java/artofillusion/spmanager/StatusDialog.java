/*
 *  Copyright 2004 Francois Guillet
 *  This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 2 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */
package artofillusion.spmanager;

import artofillusion.ui.UIUtilities;

import java.awt.*;
import buoy.widget.*;
import buoy.event.*;

/**
 *  Description of the Class
 *
 *@author     pims
 *@created    20 mars 2004
 */
public class StatusDialog extends BDialog
{
    protected BLabel label;
    protected BProgressBar progressBar;


    /**
     *  Constructor for the StatusDialog object
     */
    public StatusDialog(BFrame parent)
    {
        super( parent, SPMTranslate.text( "remoteStatus" ), false );
        ColumnContainer cc = new ColumnContainer();
        LayoutInfo layout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 10, 10, 10, 10 ), new Dimension( 0, 0 ) );
        cc.add( label = SPMTranslate.bLabel( "status" ), layout );
        label.setText( SPMTranslate.text( "scanningPlugins" ) );
        layout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets( 10, 10, 10, 10 ), new Dimension( 0, 0 ) );
        cc.add( progressBar = new BProgressBar( 0, 100 ), layout );
        progressBar.setShowProgressText( true );
        setContent( cc );
        pack();
        centerAndSizeWindow();
        setVisible( true );
        layoutChildren();
        addEventLink( WindowClosingEvent.class, this, "doClose" );
    }



    /**
     *  Sets the text attribute of the StatusDialog object
     *
     *@param  text  The new text value
     */
    public void setText( String text )
    {
        label.setText( text );
        pack();
        //layoutChildren();
    }


    /**
     *  Sets the progressText attribute of the StatusDialog object
     *
     *@param  text  The new progressText value
     */
    public void setProgressText( String text )
    {
        progressBar.setProgressText( text );
    }


    /**
     *  Sets the barValue attribute of the StatusDialog object
     *
     *@param  i  The new barValue value
     */
    public void setBarValue( int i )
    {
	if (i < 0) setIdle(true);
	else setIdle(false);

        progressBar.setValue( i );
    }


    /**
     *  Gets the barValue attribute of the StatusDialog object
     *
     *@return    The barValue value
     */
    public int getBarValue()
    {
        return progressBar.getValue();
    }

    /**
     *
     */
    public void setIdle(boolean flag)
    { progressBar.setIndeterminate(flag); }

    /**
     *  Description of the Method
     */
    private void centerAndSizeWindow()
    {
	UIUtilities.centerDialog(this, (WindowWidget) getParent());

	/*
        Dimension d1 = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension d2 = getComponent().getSize();
        int x;
        int y;

        d2.width = d2.width * 2;
        x = ( d1.width - d2.width ) / 2;
        y = ( d1.height - d2.height ) / 2;
        if ( x < 0 )
        {
            x = 0;
        }
        if ( y < 0 )
        {
            y = 0;
        }
        setBounds( new Rectangle( x, y, d2.width, d2.height + 2 ) );
	*/
    }


    /**
     *  Description of the Method
     */
    protected void doClose()
    {
        setVisible( false );
	dispose();
    }
}


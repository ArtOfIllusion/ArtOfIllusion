
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

import java.awt.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.net.*;
import buoy.widget.*;
import buoy.event.*;

import java.util.*;

/**
 *  Description of the Class
 *
 *@author     François Guillet
 *@created    March, 13 2004
 */
public class SPMSetupFrame extends BDialog
{
    private SPManagerFrame frame;
    private BComboBox repositoriesCB;
    private ColumnContainer filterContainer;
    private BButton okButton, cancelButton, addButton, removeButton;
    private BTextField repEntry, proxyHostEntry, proxyPortEntry, usernameEntry;
    private BPasswordField passwordEntry;
    private BLabel proxyHostLabel, proxyPortLabel, usernameLabel, passwordLabel;
    private BCheckBox useProxyCB;
    private BCheckBox useCacheCB;
    private SPMParameters parameters;
    private String[] rep;

    protected static final String[] EMPTY_STRING_ARRAY = new String[0];
    
    /**
     *  Constructor for the SPMSetupFrame object
     *
     *@param  fr  Description of the Parameter
     */
    public SPMSetupFrame( SPManagerFrame fr )
    {

        super( fr, true );
        frame = fr;
        setTitle( SPMTranslate.text( "SPManagerSetup" ) );
        //JDialog dialog = (JDialog) getComponent();
        //dialog.setUndecorated( true );
        //dialog.getRootPane().setWindowDecorationStyle( JRootPane.FRAME );
        parameters = frame.getParameters();
        addEventLink( WindowClosingEvent.class, this, "doCancel" );

        ColumnContainer cc = new ColumnContainer();
        LayoutInfo topLayout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 5, 3, 3, 3 ), new Dimension( 0, 0 ) );
        cc.add( SPMTranslate.bLabel( "chooseRepository" ), topLayout );
        LayoutInfo layout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 3, 3, 5, 3 ), new Dimension( 0, 0 ) );
        cc.add( repositoriesCB = new BComboBox( (Object[]) ( rep = parameters.getRepositories() ) ), layout );
        repositoriesCB.addEventLink( ValueChangedEvent.class, this, "doRepositoriesCBChanged" );
        repositoriesCB.setSelectedIndex( parameters.getCurrentRepositoryIndex() );

	// NTJ: populate filters
	filterContainer = new ColumnContainer();

	HashMap filters = parameters.getFilters();

	if (filters.size() > 0) {
	    String[] keys = (String[]) filters.keySet().toArray(EMPTY_STRING_ARRAY);
	    Arrays.sort(keys);
	    
	    String filtName, filtVal, filtType;
	    int i, j;
	    RowContainer line=null;
	    //RadioButtonGroup group;
	    BComboBox sel=null;
	    LayoutInfo right = new LayoutInfo(LayoutInfo.EAST,LayoutInfo.NONE);
	    LayoutInfo left = new LayoutInfo(LayoutInfo.WEST,LayoutInfo.NONE);
	    for (i = 0; i < keys.length; i++) {
		filtName = keys[i];
		filtVal = (String) filters.get(filtName);

		System.out.println("filter: " + filtName + "=" + filtVal);

		line = new RowContainer();
		sel = new BComboBox();
		//group = new RadioButtonGroup();

		line.add(new BLabel(filtName));
		line.add(sel);

		for (j = 0; j < SPMParameters.LAST_FILTER; j++) {
		    filtType = SPMParameters.FILTER_NAMES[j];

		    sel.add(filtType);
		    if (filtVal.equals(filtType))
			sel.setSelectedValue(filtType);
		    /*
		    line.add(new BRadioButton(filtType,
					      filtVal.equals(filtType),
					      group)
			     );
		    */
		}

		filterContainer.add(line, right);
	    }

	    BScrollPane sp = new BScrollPane(filterContainer);
	    sp.setVerticalScrollbarPolicy(BScrollPane.SCROLLBAR_AS_NEEDED);
	    Dimension dim = new Dimension();
	    dim.setSize(filterContainer.getPreferredSize().getWidth(),
			line.getPreferredSize().getHeight()
			*Math.min(filters.size(), 5));
	    
	    sp.setPreferredViewSize(dim);

	    BOutline bo = new BOutline(sp, BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), SPMTranslate.text("filters")));

	    cc.add(bo, layout);
	}

        useCacheCB =  SPMTranslate.bCheckBox( "useCache", parameters.getUseCache(), this, "doUseCacheCB" );
        cc.add( useCacheCB, new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 2, 3, 2, 3 ), new Dimension( 0, 0 ) ));

        LayoutInfo buttonLayout = new LayoutInfo( LayoutInfo.WEST, LayoutInfo.BOTH, new Insets( 2, 0, 2, 0 ), new Dimension( 0, 0 ) );
        LayoutInfo rcLayout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 4, 3, 4, 3 ), new Dimension( 0, 0 ) );

        FormContainer fm = new FormContainer( 2, 5 );
        LayoutInfo formLayout = new LayoutInfo( LayoutInfo.WEST, LayoutInfo.NONE, new Insets( 2, 4, 2, 4 ), new Dimension( 0, 0 ) );

        useProxyCB = SPMTranslate.bCheckBox( "useProxy", parameters.useProxy(), this, "doUseProxyCB" );
        fm.add( useProxyCB, 0, 0, formLayout );

        fm.add( proxyHostLabel = SPMTranslate.bLabel( "proxyHost" ), 0, 1, formLayout );
        proxyHostEntry = new BTextField( parameters.getProxyHost(), 25 );
        fm.add( proxyHostEntry, 1, 1, formLayout );

        fm.add( proxyPortLabel = SPMTranslate.bLabel( "proxyPort" ), 0, 2, formLayout );
        proxyPortEntry = new BTextField( parameters.getProxyPort(), 25 );
        fm.add( proxyPortEntry, 1, 2, formLayout );

        fm.add( usernameLabel = SPMTranslate.bLabel( "username" ), 0, 3, formLayout );
        usernameEntry = new BTextField( parameters.getUsername(), 15 );
        fm.add( usernameEntry, 1, 3, formLayout );

        fm.add( passwordLabel = SPMTranslate.bLabel( "password" ), 0, 4, formLayout );
        passwordEntry = new BPasswordField( parameters.getPassword(), 15 );
        fm.add( passwordEntry, 1, 4, formLayout );
        formLayout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 0, 0, 0, 0 ), new Dimension( 0, 0 ) );
        cc.add( fm, rcLayout );

        if ( !useProxyCB.getState() )
        {
            proxyHostEntry.setEnabled( false );
            proxyPortEntry.setEnabled( false );
            usernameEntry.setEnabled( false );
            passwordEntry.setEnabled( false );
            proxyHostLabel.setEnabled( false );
            proxyPortLabel.setEnabled( false );
            usernameLabel.setEnabled( false );
            passwordLabel.setEnabled( false );
        }

        RowContainer buttons = new RowContainer();
        buttons.add( okButton = SPMTranslate.bButton( "ok", this, "doOK" ) );
        buttons.add( cancelButton = SPMTranslate.bButton( "cancel", this, "doCancel" ) );
        cc.add( buttons, new LayoutInfo() );
        setContent( cc );
        addEventLink( WindowClosingEvent.class, this, "doCancel" );
        pack();
        ( (JDialog) getComponent() ).setLocationRelativeTo( frame.getComponent() );
        /*
         *  addButton.setEnabled( false );
         *  if ( rep.length <= 1 )
         *  {
         *  removeButton.setEnabled( false );
         *  }
         */
        parameters.setChanged( false );
        setVisible( true );
    }

    /**
     *  Proxy checkbox selected
     */
    private void doUseProxyCB()
    {
        if ( !useProxyCB.getState() )
        {
            proxyHostEntry.setEnabled( false );
            proxyPortEntry.setEnabled( false );
            usernameEntry.setEnabled( false );
            passwordEntry.setEnabled( false );
            proxyHostLabel.setEnabled( false );
            proxyPortLabel.setEnabled( false );
            usernameLabel.setEnabled( false );
            passwordLabel.setEnabled( false );
        }
        else
        {
            proxyHostEntry.setEnabled( true );
            proxyPortEntry.setEnabled( true );
            usernameEntry.setEnabled( true );
            passwordEntry.setEnabled( true );
            proxyHostLabel.setEnabled( true );
            proxyPortLabel.setEnabled( true );
            usernameLabel.setEnabled( true );
            passwordLabel.setEnabled( true );
        }
    }


    /**
     *  Add a new repository to the list
     */
    private void doAdd()
    {
        int i;

        String[] newRep = new String[rep.length + 1];
        for ( i = 0; i < rep.length; ++i )
        {
            newRep[i] = rep[i];
        }
        newRep[i] = repEntry.getText();
        rep = newRep;
        repositoriesCB.setContents( (Object[]) newRep );
        repositoriesCB.setSelectedIndex( i );
        if ( rep.length > 1 )
        {
            removeButton.setEnabled( true );
        }
    }


    /**
     *  Remove a repository from the list
     */
    private void doRemove()
    {
        String[] newRep = new String[rep.length - 1];
        int j = 0;
        int removed = 0;
        String s = repEntry.getText();
        for ( int i = 0; i < rep.length && j < newRep.length; ++i )
        {
            if ( !s.equals( rep[i] ) )
            {
                newRep[j] = rep[i];
                ++j;
            }
            else
            {
                removed = i;
            }
        }
        rep = newRep;
        if ( removed >= rep.length )
        {
            --removed;
        }
        repositoriesCB.setContents( (Object[]) rep );
        repositoriesCB.setSelectedIndex( removed );
        if ( rep.length <= 1 )
        {
            removeButton.setEnabled( false );
        }
    }


    /**
     *  OK button selected
     */
    private void doOK()
    {
        parameters.setURLs( rep, repositoriesCB.getSelectedIndex() );
        parameters.setProxyParameters( useProxyCB.getState(),
                proxyHostEntry.getText(),
                proxyPortEntry.getText(),
                usernameEntry.getText(),
                passwordEntry.getText() );

	HashMap filters = parameters.getFilters();
	filters.clear();
	RowContainer line;
	//RadioButtonGroup group;
	BComboBox sel;
	String filtName, filtVal;
	for (int i = 0; i < filterContainer.getChildCount(); i++) {
	    line = (RowContainer) filterContainer.getChild(i);

	    filtName = ((BLabel) line.getChild(0)).getText();
	    /*
	    group = ((BRadioButton) line.getChild(1)).getGroup();

	    if (group.getSelection() != null)
		filtVal = ((BRadioButton) group.getSelection()).getText();
	    else filtVal = SPMParameters.FILTER_NAMES[SPMParameters.DEFAULT];
	    */
	    sel = (BComboBox) line.getChild(1);
	    if (sel.getSelectedIndex() >= 0)
		filtVal = sel.getSelectedValue().toString();
	    else
		filtVal = SPMParameters.FILTER_NAMES[SPMParameters.DEFAULT];

	    filters.put(filtName, filtVal);
	}

        parameters.setChanged( true );
        dispose();
    }


    /**
     *  Cancel button selected
     */
    private void doCancel()
    {
        dispose();
    }

     /**
     *  Use cache checkbox selected
     */
    private void doUseCacheCB()
    {
        parameters.setUseCache( useCacheCB.getState() );
    }


    /**
     *  Description of the Method
     */
    private void doRepositoriesCBChanged()
    {
        //repEntry.setText( (String) repositoriesCB.getSelectedValue() );
	parameters.setCurrentRepository( repositoriesCB.getSelectedIndex() );
    }


    /**
     *  Description of the Method
     */
    private void doRepEntryChanged()
    {
        try
        {
            String text = repEntry.getText();
            new URL( text );
            ( (JTextField) repEntry.getComponent() ).setForeground( Color.black );
            String s[] = parameters.getRepositories();
            addButton.setEnabled( true );
            removeButton.setEnabled( false );
            for ( int i = 0; i < s.length; ++i )
            {
                if ( s[i].equals( text ) )
                {
                    addButton.setEnabled( false );
                    removeButton.setEnabled( true );
                }
            }
        }
        catch ( MalformedURLException e )
        {
            ( (JTextField) repEntry.getComponent() ).setForeground( Color.red );
            addButton.setEnabled( false );
            removeButton.setEnabled( false );
        }
    }
}


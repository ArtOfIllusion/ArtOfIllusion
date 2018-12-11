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
import javax.swing.tree.*;
import buoy.widget.*;
import buoy.event.*;
//import artofillusion.ModellingApp;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 *  Description of the Class
 *
 *@author     pims
 *@created    20 mars 2004
 */
public class ManageSplitPane extends SPMSplitPane
{
    private BButton deleteButton;
    private BButton deleteAllButton;


    /**
     *  Constructor for the ManageSplitPane object
     */
    public ManageSplitPane()
    {
        super( "installedScriptsPlugins" );
        acceptsFileSelection = false;
        //initialise button
        LayoutInfo layout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 0, 0, 0, 0 ), new Dimension( 0, 0 ) );
        //buttonRow.add( deleteAllButton = SPMTranslate.bButton( "deleteAllSelected", this, "doDeleteAll" ), layout );
        //deleteAllButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Delete16.gif" ) ) );
        buttonRow.add( deleteButton = SPMTranslate.bButton( "deleteFile", this, "doDelete" ), layout );
        deleteButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Delete16.gif" ) ) );
        deleteButton.setText( SPMTranslate.text( "deleteScript" ) );
        deleteButton.setEnabled( false );
        fs = new LocalSPMFileSystem();
        updateTree();
    }


    /**
     *  Description of the Method
     */
    protected void updateTree()
    {
        /*
         *  {
         *  updateTree(true);
         *  }
         *  protected void updateTree(boolean force)
         */
        //update the file system
        fs.initialize();

        //get the scripts
        getPlugins();
        getToolScripts();
        getObjectScripts();
        getStartupScripts();
    }


    /**
     *  Gets the plugins attribute of the ManageSplitPane object
     */
    private void getPlugins()
    {
        getFiles( pluginsPath, fs.getPlugins() );
    }


    /**
     *  Gets the toolScripts attribute of the ManageSplitPane object
     */
    private void getToolScripts()
    {
        getFiles( toolScriptsPath, fs.getToolScripts() );
    }


    /**
     *  Gets the objectScripts attribute of the ManageSplitPane object
     */
    private void getObjectScripts()
    {
        getFiles( objectScriptsPath, fs.getObjectScripts() );
    }


    /**
     *  Gets the startupScripts attribute of the ManageSplitPane object
     */
    private void getStartupScripts()
    {
        getFiles( startupScriptsPath, fs.getStartupScripts() );
    }


    /**
     *  Gets the files attribute of the ManageSplitPane object
     *
     *@param  addTo  Description of the Parameter
     *@param  infos  Description of the Parameter
     */
    private void getFiles( TreePath addTo, Vector infos )
    {
        DefaultMutableTreeNode tn;
        SPMObjectInfo info;

        for ( int i = 0; i < infos.size(); i++ )
        {
            info = (SPMObjectInfo) infos.elementAt( i );
            tn = new DefaultMutableTreeNode( info.getName() );
            tn.setAllowsChildren( false );
            tn.setUserObject( info );
            tree.addNode( addTo, tn );
            //System.out.println( "added " + info.getName() + " to " + addTo );
        }

	// NTJ: set reference counts
	for (int i = 0; i < infos.size(); i++) {
            info = (SPMObjectInfo) infos.elementAt( i );
	    //System.out.println("SPManager: file=" + info.getName());

	    Collection externals = info.getExternals();
	    String extName, extType;
	    SPMObjectInfo ext;
	    if (externals != null) {
		//for (int j = 0; j < externals.size(); j++) {
		for (Iterator iter = externals.iterator(); iter.hasNext(); ) {
		    //extName = (String) externals.get(j);
		    extName = (String) iter.next();

		    if (extName.endsWith("= required")) {
			extType = extName.substring(extName.indexOf(':')+1,
						    extName.indexOf('=')).trim();
			extName = extName.substring(0, extName.indexOf(':'));

			//System.out.println("getFiles: extName=" + extName + "<<");
			ext = getInfo(extName, (TreePath)pathMap.get(extType));
			if (ext != null) ext.refcount++;
		    }
		}
	    }
	}
    }


    /**
     *  Description of the Method
     */
    private void doDelete()
    {
        SPMObjectInfo info = getSelectedNodeInfo();

	if (info.refcount > 0) {
	    JOptionPane.showMessageDialog( (JFrame) SPManagerFrame.getInstance().getComponent(), SPMTranslate.text("cannotDeleteRequired"), SPMTranslate.text("Delete", info.fileName), JOptionPane.ERROR_MESSAGE);

	    return;
	}

        int r = JOptionPane.showConfirmDialog( (JFrame) SPManagerFrame.getInstance().getComponent(), SPMTranslate.text( "permanentlyDelete", info.fileName ),
                SPMTranslate.text( "warning" ), JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION );
        if ( r == JOptionPane.YES_OPTION )
            deleteFile( info );
    }


    /**
     *  Constructor for the deleteFile object
     *
     *@param  info  Description of the Parameter
     */
    private void deleteFile( SPMObjectInfo info )
    {
        if ( info != null )
        {

            File file = new File( info.fileName );
            if ( !file.exists() )
            {
                System.out.println( "SPManager :" );
                System.out.println( "Delete: no such file or directory: " + file.getAbsolutePath() );
            }
            if ( !file.canWrite() )
            {
                System.out.println( "SPManager :" );
                System.out.println( "Delete: write protected: " + file.getAbsolutePath() );
            }
            if ( !file.delete() )
            {

                new BStandardDialog( SPMTranslate.text( "error" ), SPMTranslate.text( "cannotDeleteFile", info.fileName ), BStandardDialog.ERROR ).showMessageDialog( SPManagerFrame.getInstance() );
                System.out.println( "SPManager :" );
                System.out.println( "File cannot be deleted: " + file.getAbsolutePath() );
                return;
            }
            if ( info.fileName.lastIndexOf( "Plugins" ) != -1 )
                modified = true;
            else
                SPManagerUtils.updateAllAoIWindows();
            info.setSelected( false );
            if ( info.files != null )
            {
                for ( int i = 0; i < info.files.length; ++i )
                {
                    file = new File( info.getAddFileName( i ) );
                    file.delete();
                }
            }

	    Collection externals = info.getExternals();
	    String extName, extType;
	    SPMObjectInfo ext;
	    if (externals != null) {
		//for (int j = 0; j < externals.size(); j++) {
		for (Iterator iter = externals.iterator(); iter.hasNext(); ) {
		    //extName = (String) externals.get(j);
		    extName = (String) iter.next();

		    if (extName.endsWith("= required")) {
			extType = extName.substring(extName.indexOf(':')+1,
						    extName.indexOf('=')).trim();

			extName = extName.substring(0, extName.indexOf(':'));

			//System.out.println("deleteFile: extName=" + extName + "<<");
			ext = getInfo(extName, (TreePath)pathMap.get(extType));
			if (ext != null) ext.refcount--;
		    }
		}
	    }
	    
            fs.initialize();
            tree.removeNode( tree.getSelectedNode() );
            voidSelection();

        }
        for ( int i = 0; i < splitPaneList.size(); ++i )
        {
            if ( splitPaneList.elementAt( i ) != this )
            {
                ( (SPMSplitPane) splitPaneList.elementAt( i ) ).doUpdate();
            }
        }

    }


    /**
     *  Description of the Method
     */
    public void doDeleteAll()
    {
        int r = JOptionPane.showConfirmDialog( (JFrame) SPManagerFrame.getInstance().getComponent(), SPMTranslate.text( "permanentlyDeleteAll" ),
                SPMTranslate.text( "warning" ), JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION );
        if ( r == JOptionPane.YES_OPTION )
        {
            deleteAllSelected( toolScriptsPath );
            deleteAllSelected( objectScriptsPath );
            deleteAllSelected( startupScriptsPath );
            voidSelection();
        }
    }


    /**
     *  Description of the Method
     *
     *@param  path  Description of the Parameter
     */
    private void deleteAllSelected( TreePath path )
    {
        int count = tree.getChildNodeCount( path );
        if ( count > 0 )
        {
            for ( int j = count - 1; j >= 0; --j )
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getChildNode( path, j ).getLastPathComponent();
                SPMObjectInfo nodeInfo = (SPMObjectInfo) node.getUserObject();
                if ( nodeInfo.isSelected() )
                {
                    deleteFile( nodeInfo );
                    tree.removeNode( tree.getChildNode( path, j ) );
                }
            }
        }
    }


    /**
     *  Description of the Method
     *
     *@param  deletable  Description of the Parameter
     */
    public void scriptSelection( boolean deletable )
    {
        deleteButton.setText( SPMTranslate.text( "deleteScript" ) );
        deleteButton.setEnabled( deletable );
        super.scriptSelection( deletable );
    }


    /**
     *  Description of the Method
     *
     *@param  deletable  Description of the Parameter
     */
    public void pluginSelection( boolean deletable )
    {
        deleteButton.setText( SPMTranslate.text( "deletePlugin" ) );
        deleteButton.setEnabled( deletable );
        super.pluginSelection( deletable );
    }


    /**
     *  Description of the Method
     */
    public void voidSelection()
    {
        deleteButton.setEnabled( false );
        super.voidSelection();
    }
}


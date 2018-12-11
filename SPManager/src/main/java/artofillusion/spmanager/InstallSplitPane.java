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

import artofillusion.*;
import artofillusion.ui.Translate;
import artofillusion.ui.UIUtilities;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import buoy.widget.*;
import buoy.event.*;
//import artofillusion.ModellingApp;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;
import java.text.*;


/**
 *  Description of the Class
 *
 *@author     pims
 *@created    20 mars 2004
 */
public class InstallSplitPane extends SPMSplitPane
{
    private URL repository;

    private BButton installAllButton, installSingleButton, selectAllButton;
    private BCheckBox selectCB;
    private boolean setup = false;
    private boolean unknownHost = false;
    private StatusDialog status;
    private long downloadedLength = 0;
    private long lengthToDownload;
    private boolean isDownloading;
    private SPMObjectInfo installNodeInfo;

    protected ArrayList errors=null;

    /**
     *  Constructor for the InstallSplitPane object
     *
     *@param  workMode    Description of the Parameter
     *@param  repository  Description of the Parameter
     */
    public InstallSplitPane( short workMode, URL repository )
    {
	super( ( workMode == INSTALL ? "installScriptsPlugins" : "updateScriptsPlugins" ), workMode );
	fs = new HttpSPMFileSystem( repository );
	initialize();
    }


    /**
     *  Constructor for the InstallSplitPane object
     *
     *@param  workMode  Description of the Parameter
     *@param  fs        Description of the Parameter
     */
    public InstallSplitPane( short workMode, SPMFileSystem fs )
    {
	super( ( workMode == INSTALL ? "installScriptsPlugins" : "updateScriptsPlugins" ), workMode );
	this.fs = fs;
	initialize();
    }


    /**
     *  Description of the Method
     */
    private void initialize()
    {
	//initialise buttons
	LayoutInfo layout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 0, 5, 0, 5 ), new Dimension( 0, 0 ) );
	if ( workMode == UPDATE )
	{
	    buttonRow.add( installAllButton = SPMTranslate.bButton( "updateAllSelected", this, "doInstallAll" ), layout );
	    installAllButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Refresh16.gif" ) ) );
	    //buttonRow.add( installSingleButton = SPMTranslate.bButton( "updateSingle", this, "doInstallSingle" ), layout );
	    //installSingleButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Refresh16.gif" ) ) );
	    buttonRow.add( selectAllButton = SPMTranslate.bButton( "selectAll", this, "doSelectAll" ), layout );
	    //selectAllButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Refresh16.gif" ) ) );
	}
	else
	{
	    buttonRow.add( installAllButton = SPMTranslate.bButton( "installAllSelected", this, "doInstallAll" ), layout );
	    installAllButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Import16.gif" ) ) );
	    //buttonRow.add( installSingleButton = SPMTranslate.bButton( "installSingle", this, "doInstallSingle" ), layout );
	    //installSingleButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Import16.gif" ) ) );
	    buttonRow.add( selectAllButton = SPMTranslate.bButton( "selectAll", this, "doSelectAll" ), layout );
	    //selectAllButton.setIcon( new ImageIcon( getClass().getResource( "/artofillusion/spmanager/icons/Import16.gif" ) ) );
	}
	buttonRow.add( selectCB = SPMTranslate.bCheckBox( "selected", false, this, "doSelectCB" ) );
	//installSingleButton.setEnabled( false );
	selectCB.setEnabled( false );
	updateTree();
	modified = false;
    }


    /**
     *  Description of the Method
     */
    protected void updateTree()
    {
	if ( setup )
	{
	    //get the scripts
	    fs.getRemoteInfo(
		    new Runnable()
		    {
			public void run()
			{
			    doCallbackUpdate();
			}
		    } );

	}
    }


    /**
     *  Description of the Method
     */
    public void doCallbackUpdate()
    {
	getPlugins();
	getToolScripts();
	getObjectScripts();
	getStartupScripts();
	setup = true;
	( (DefaultTreeModel) tree.getModel() ).reload();
	SPManagerFrame.getInstance().checkForUpdatedMe();
    }


    /**
     *  Pane setup
     */
    public void doSetup()
    {
	if ( !setup )
	{
	    //get the scripts
	    fs.getRemoteInfo(
		    new Runnable()
		    {
			public void run()
			{
			    doCallbackUpdate();
			}
		    } );
	}
    }


    /**
     *  Gets the plugins attribute of the InstallSplitPane object
     */
    private void getPlugins()
    {
	getFiles( pluginsPath, fs.getPlugins(), getManager().fs.getPlugins() );
    }


    /**
     *  Gets the toolScripts attribute of the InstallSplitPane object
     */
    private void getToolScripts()
    {
	getFiles( toolScriptsPath, fs.getToolScripts(), getManager().fs.getToolScripts() );
    }


    /**
     *  Gets the objectScripts attribute of the InstallSplitPane object
     */
    private void getObjectScripts()
    {
	getFiles( objectScriptsPath, fs.getObjectScripts(), getManager().fs.getObjectScripts() );
    }


    /**
     *  Gets the startupScripts attribute of the InstallSplitPane object
     */
    private void getStartupScripts()
    {
	getFiles( startupScriptsPath, fs.getStartupScripts(), getManager().fs.getStartupScripts() );
    }


    /**
     *  Gets the files attribute of the InstallSplitPane object
     *
     *@param  addTo              Description of the Parameter
     *@param  infos              Description of the Parameter
     *@param  managerInfoVector  Description of the Parameter
     */
    private void getFiles( TreePath addTo, Vector infos, Vector managerInfoVector )
    {
	DefaultMutableTreeNode tn;
	SPMObjectInfo info;
	SPMObjectInfo managerInfo;
	boolean eligible;
	TreeMap map = new TreeMap();

	for ( int i = 0; i < infos.size(); i++ )
	{
	    info = (SPMObjectInfo) infos.elementAt( i );

	    if (info.restriction >= SPMParameters.HIDE) continue;

	    {
		//check if file candidate for update or install
		eligible = ( workMode == INSTALL );
		managerInfo = null;
		String name = info.getName();
		for ( int j = 0; j < managerInfoVector.size(); ++j )
		{
		    if ( ( (SPMObjectInfo) managerInfoVector.elementAt( j ) ).getName().equals( name ) )
		    {
			eligible = ( workMode == UPDATE );
			if ( eligible )
			{
			    //check if valid update

			    managerInfo = (SPMObjectInfo) managerInfoVector.elementAt( j );
			    System.out.println( info.getName() );
			    System.out.println( "major distant local :" + info.getMajor() + " " + managerInfo.getMajor() );
			    System.out.println( "minor distant local :" + info.getMinor() + " " + managerInfo.getMinor() );
			    System.out.println( "beta distant local :" + info.isBeta() + " " + managerInfo.isBeta() );
			    System.out.println( "beta distant local :" + info.getBeta() + " " + managerInfo.getBeta() );
			    if ( info.getMajor() < managerInfo.getMajor() )
			    {
				eligible = false;
			    }
			    else if ( info.getMajor() == managerInfo.getMajor() )
			    {
				if ( info.getMinor() < managerInfo.getMinor() )
				{
				    eligible = false;
				}
				else if ( info.getMinor() == managerInfo.getMinor() )
				{
				    if (managerInfo.isBeta()) {
					if (info.isBeta() && info.getBeta()
						<= managerInfo.getBeta())
					{
					    eligible = false;
					}
				    }
				    else
				    {
					eligible = false;
				    }
				}
			    }
			}
		    }
		}
		if ( eligible ) {
		    map.put( info.getName(), info);
		}
	    }
	}
	Collection col = map.values();
	if ( ! col.isEmpty() )
	{
	    for( Iterator iter = col.iterator(); iter.hasNext(); )
	    {
		info = (SPMObjectInfo) iter.next();
		tn = new DefaultMutableTreeNode( info.getName() );
		tn.setAllowsChildren( false );
		tn.setUserObject( info );
		tree.addNode( addTo, tn );
	    }
	}
    }


    /**
     *  Description of the Method
     */
    public void doInstallAll()
    {
	if ( !isDownloading )
	{
	    isDownloading = true;
	    lengthToDownload = 0;
	    downloadedLength = 0;
	    lengthToDownload = getInstallLength( pluginsPath );
	    lengthToDownload += getInstallLength( toolScriptsPath );
	    lengthToDownload += getInstallLength( objectScriptsPath );
	    lengthToDownload += getInstallLength( startupScriptsPath );
	    if ( lengthToDownload > 0 )
	    {
		status = new StatusDialog(SPManagerPlugin.getFrame());
		(
			new Thread()
			{
			    public void run()
			    {
				installAllSelected( pluginsPath );
				installAllSelected( toolScriptsPath );
				installAllSelected( objectScriptsPath );
				installAllSelected( startupScriptsPath );

				try {
				    SwingUtilities.invokeAndWait(new Runnable() {
					public void run()
					{
					    voidSelection();
					    getManager().doUpdate();
					    isDownloading = false;
					    status.dispose();
					    status = null;
					    SPManagerUtils.updateAllAoIWindows();
					}
				    });
				} catch (Exception e) {
				    System.out.println("install error: " + e);
				}
			    }
			}).start();
	    }
	}
    }


    /**
     *  Gets the installLength attribute of the InstallSplitPane object
     *
     *@param  path  Description of the Parameter
     *@return       The installLength value
     */
    private long getInstallLength( TreePath path )
    {
	long length = 0;
	int count = tree.getChildNodeCount( path );
	if ( count > 0 )
	{
	    for ( int j = count - 1; j >= 0; --j )
	    {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getChildNode( path, j ).getLastPathComponent();
		SPMObjectInfo nodeInfo = (SPMObjectInfo) node.getUserObject();
		if ( nodeInfo.isSelected() )
		{
		    length += nodeInfo.getTotalLength();
		}
	    }
	}
	return length;
    }


    /**
     *  Description of the Method
     *
     *@param  path  Description of the Parameter
     */
    private void installAllSelected( TreePath path )
    {
	int count = tree.getChildNodeCount( path );
	if ( count > 0 )
	{
	    if (errors == null) errors = new ArrayList(128);
	    else errors.clear();

	    boolean ignoreErrs = false;
	    int err = 0, cut1, cut2;

	    download:
		for ( int j = count - 1; j >= 0; --j )
		{
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getChildNode( path, j ).getLastPathComponent();
		    SPMObjectInfo nodeInfo = (SPMObjectInfo) node.getUserObject();
		    if ( nodeInfo.isSelected() )
		    {
			// confirm before proceeding?
				if (nodeInfo.restriction == SPMParameters.CONFIRM) {
				    String reason = nodeInfo.getComments();
				    cut1 = reason.indexOf("**");
				    cut2 = reason.indexOf("**", cut1+2);
				    if (cut1 >= 0) {
					if (cut2 <= cut1+2) cut2 = reason.length();
					reason = reason.substring(cut1+2, cut2) + "\n";
				    }
				    else
					reason = "\n";

				    reason = SPMTranslate.text("markedConfirm", nodeInfo.getName()) +
				    reason + SPMTranslate.text("Confirm");
				    if (new BStandardDialog("SPManager", UIUtilities.breakString(reason),
					    BStandardDialog.QUESTION)
				    .showOptionDialog(null, SPManagerFrame.YES_NO, SPManagerFrame.YES_NO[1]) == 1)
				    {
					errors.add(SPMTranslate.text("Cancelled",
						nodeInfo.getName()));

					continue;
				    }
				}

				installFile( nodeInfo );
				tree.removeNode( tree.getChildNode( path, j ) );

				if (errors.size() > err && ignoreErrs == false) {
				    BLabel messg = SPMTranslate.bLabel("errMsg");

				    BStandardDialog dlg = new
				    BStandardDialog("SPManager", messg,
					    BStandardDialog.WARNING);

				    switch (dlg.showOptionDialog(null, SPManagerFrame.CONTINUE_IGNORE,
					    SPMTranslate.text("Continue"))) {

					    case 1:
						break download;

					    case 2:
						ignoreErrs = true;
				    }
				}

				err = errors.size();
		    }
		}

	    if (errors.size() > 0) showErrors(errors);
	}
    }


    /**
     *  Description of the Method
     */
    public void doInstallSingle()
    {
	if (errors == null) errors = new ArrayList(128);
	else errors.clear();

	installNodeInfo = getSelectedNodeInfo();
	if ( installNodeInfo == null )
	{
	    return;
	}

	// confirm before proceeding?
	if (installNodeInfo.restriction == SPMParameters.CONFIRM) {
	    String reason = installNodeInfo.getComments();
	    int cut1 = reason.indexOf("**");
	    int cut2 = reason.indexOf("**", cut1+2);
	    if (cut1 >= 0) {
		if (cut2 <= cut1+2) cut2 = reason.length();
		reason = reason.substring(cut1+2, cut2) + "\n";
	    }
	    else
		reason = "\n";

	    reason = SPMTranslate.text("markedConfirm", installNodeInfo.getName()) +
	    reason + SPMTranslate.text("Confirm");
	    if (new BStandardDialog("SPManager", UIUtilities.breakString(reason),
		    BStandardDialog.QUESTION)
	    .showOptionDialog(null, SPManagerFrame.YES_NO, SPManagerFrame.YES_NO[1]) == 1)
	    {
		errors.add(SPMTranslate.text("Cancelled",
			installNodeInfo.getName()));

		showErrors(errors);
		return;
	    }

	}

	if ( !isDownloading )
	{
	    isDownloading = true;
	    lengthToDownload = installNodeInfo.getTotalLength();

	    downloadedLength = 0;
	    if ( lengthToDownload > 0 )
	    {
		if (errors == null) errors = new ArrayList(128);
		else errors.clear();

		status = new StatusDialog(SPManagerPlugin.getFrame());
		(
			new Thread()
			{
			    public void run()
			    {
				installFile( installNodeInfo );

				if (errors.size() > 0) showErrors(errors);

				//status.dispose();
				//SPManagerPlugin.restart();
				try {
				    SwingUtilities.invokeAndWait(new Runnable() {
					public void run()
					{
					    /*  NTJ - replaced by restart()  */
					    tree.removeNode(tree.getSelectedNode());
					    voidSelection();
					    getManager().doUpdate();
					    isDownloading = false;
					    status.dispose();
					    status = null;
					    SPManagerUtils.updateAllAoIWindows();
					}
				    });
				} catch (Exception e) {
				    System.out.println("install error: " + e);
				}
			    }
			}).start();
	    }
	}

	if (errors.size() > 0) showErrors(errors);
    }


    /**
     *  Description of the Method
     *
     *@param  nodeInfo  Description of the Parameter
     */
    public void installFile( SPMObjectInfo nodeInfo )
    {
	HashMap transaction = new HashMap(32);

	if (errors == null) errors = new ArrayList(16);
	int errCount = errors.size();

	File file = null;
	switch ( fs.getInfoType( nodeInfo ) )
	{
	case SPMFileSystem.PLUGIN_TYPE:
	    file = new File(SPManagerPlugin.PLUGIN_DIRECTORY, nodeInfo.getName() + ".jar");
	    modified = true;
	    break;
	case SPMFileSystem.TOOL_SCRIPT_TYPE:
	    file = new File(SPManagerPlugin.TOOL_SCRIPT_DIRECTORY, nodeInfo.getName() + ".bsh");
	    break;
	case SPMFileSystem.OBJECT_SCRIPT_TYPE:
	    file = new File(SPManagerPlugin.OBJECT_SCRIPT_DIRECTORY, nodeInfo.getName() + ".bsh");
	    break;
	case SPMFileSystem.STARTUP_SCRIPT_TYPE:
	    file = new File(SPManagerPlugin.STARTUP_SCRIPT_DIRECTORY, nodeInfo.getName() + ".bsh");
	    break;
	}

	File folder = new File(SPManagerPlugin.TEMP_DIR,
		file.getParentFile().getName());

	System.out.println("folder=" + folder.getAbsolutePath());

	if (!folder.exists() && !folder.mkdirs()) {
	    errors.add(SPMTranslate.text("error") + "cannot open/create " +
		    folder.getAbsolutePath());

	    System.out.println("cannot open/create " +
		    folder.getAbsolutePath());
	}

	File update = new File(folder, file.getName() + ".upd");

	System.out.println("downloading to " + update.getAbsolutePath());

	if (status == null)
	    status = new StatusDialog(SPManagerPlugin.getFrame());
	
	status.setText( SPMTranslate.text( "downloading", nodeInfo.getName() ) );

	if ( fs.getInfoType( nodeInfo ) == SPMFileSystem.PLUGIN_TYPE )
	{
	    downloadedLength += HttpSPMFileSystem.downloadRemoteBinaryFile( nodeInfo.httpFile, update.getAbsolutePath(), nodeInfo.length, status, lengthToDownload, downloadedLength, errors );
	}
	else
	{
	    downloadedLength += HttpSPMFileSystem.downloadRemoteBinaryFile( nodeInfo.httpFile, update.getAbsolutePath(), nodeInfo.length, status, lengthToDownload, downloadedLength, errors );
	}

	transaction.put(file, update);

	if ( nodeInfo.files != null )
	{
	    String dest;
	    int sep;
	    for ( int j = 0; j < nodeInfo.files.length; ++j )
	    {
		dest = (String) nodeInfo.destination.get(j);
		sep = dest.indexOf('/');

		// build the destination path
		if (sep == 0) {
		    file = new File(SPManagerPlugin.APP_DIRECTORY, dest.substring(1));
		}
		else if (dest.startsWith("$")) {
		    try {
			file = new File(SPManagerPlugin.class.getField(dest.substring(1, sep)).get(null).toString(), dest.substring(sep+1));
		    } catch (Exception e) {
			System.out.println("SPManager: cannot resolve: " + dest);
			errors.add("Cannot resolve: " + dest);
		    }
		}
		else {
		    switch ( fs.getInfoType( nodeInfo ) )
		    {
		    case SPMFileSystem.PLUGIN_TYPE:
			file = new File(SPManagerPlugin.PLUGIN_DIRECTORY, dest);
			break;
		    case SPMFileSystem.TOOL_SCRIPT_TYPE:
			file = new File(SPManagerPlugin.TOOL_SCRIPT_DIRECTORY, dest);
			break;
		    case SPMFileSystem.OBJECT_SCRIPT_TYPE:
			file = new File(SPManagerPlugin.OBJECT_SCRIPT_DIRECTORY, dest);
			break;
		    case SPMFileSystem.STARTUP_SCRIPT_TYPE:
			file = new File(SPManagerPlugin.STARTUP_SCRIPT_DIRECTORY, dest);
			break;
		    }
		}

		// now add the actual file name
		//file = new File(file, nodeInfo.files[j]);  NTJ: now added in SPMObjectInfo when XML is parsed

		folder = new File(SPManagerPlugin.TEMP_DIR,
			file.getParentFile().getName());

		if (!folder.exists() && !folder.mkdirs()) {
		    errors.add(SPMTranslate.text("error") +
			    "cannot open/create " +
			    folder.getAbsolutePath());

		    System.out.println("cannot open/create " +
			    folder.getAbsolutePath());
		}

		update = new File(folder, file.getName() + ".upd");

		System.out.println("downloading to " +
			update.getAbsolutePath());

		if ( status != null )
		{
		    status.setText( SPMTranslate.text( "downloading", nodeInfo.files[j] ) );
		}
		//value = status.getBarValue();
		URL addFileURL = nodeInfo.getAddFileURL( j );
		System.out.println("downloading from " + addFileURL.toString());
		downloadedLength += HttpSPMFileSystem.downloadRemoteBinaryFile( addFileURL, update.getAbsolutePath(), nodeInfo.fileSizes[j], status, lengthToDownload, downloadedLength, errors );

		transaction.put(file, update);
	    }
	}

	File orig;
	Map.Entry entry;
	Iterator iter = transaction.entrySet().iterator();
	while (iter.hasNext()) {
	    try {

		entry = (Map.Entry) iter.next();

		orig = (File) entry.getKey();
		update = (File) entry.getValue();

		// if there are errors, just clean up...
		if (errors.size() > errCount) {

		    if (!update.delete()) {
			System.out.println("SPManager: tx abort: " +
				" update file not deleted: " +
				update.getAbsolutePath());

			// NTJ Happens normally on some Wincrap boxes, so don't
			// display an error
			//errors.add("couldn't delete " +
			//   update.getAbsolutePath());

			// make file zero-length
			RandomAccessFile raf =
			    new RandomAccessFile(update, "rw");
			raf.setLength(0);
			raf.close();
		    }

		    continue;
		}

		folder = file.getParentFile();
		if (!folder.exists() && !folder.mkdirs()) {
		    throw new RuntimeException("cannot open/create " +
			    folder.getAbsolutePath());
		}

		// now delete the original, and rename the new file
		if (orig.exists()) orig.delete();

		System.out.println("copying file to " +
			orig.getAbsolutePath());

		if (!update.renameTo(orig)) {

		    System.out.println("SPManager: old-style copy...");
		    if (copyFile(update, orig)) {

			// make sure update file really was deleted
			if (!update.delete()) {
			    System.out.println("SPManager:" +
				    " update file not deleted: " +
				    update.getAbsolutePath());

			    // NTJ Happens normally on some Wincrap boxes, so don't
			    // display an error
			    //errors.add("couldn't delete " +
			    //   update.getAbsolutePath());

			    // make file zero-length
			    RandomAccessFile raf =
				new RandomAccessFile(update, "rw");
			    raf.setLength(0);
			    raf.close();
			}
		    }
		    else {
			System.out.println("SPManager.cleanup: " +
				"could not copy " + file.getPath());

			errors.add("couldn't copy " + file.getName());
		    }
		}
	    } catch (Exception e) {
		errors.add(SPMTranslate.text("error") +
			"(" + file.getName() + ")" + e);
	    }
	}

	//fs.deleteInfo(nodeInfo);
    }


    /**
     *  copy one file to another
     */
    protected static boolean copyFile(File in, File out)
    {
	InputStream is = null;
	OutputStream os = null;
	try {
	    is = new BufferedInputStream(new FileInputStream(in));
	    os = new BufferedOutputStream(new FileOutputStream(out));

	    int b;
	    while ((b = is.read()) >= 0) os.write((byte) b);
	}
	catch (Exception e) {
	    return false;
	}
	finally {
	    try {
		os.flush();
		os.close();
		is.close();
	    } catch (Exception e) {}
	}

	return true;
    }

    /**
     *  Description of the Method
     *
     *@param  path  Description of the Parameter
     */
    private void selectAllInfos( TreePath path )
    {
	SPMObjectInfo info;
	int count = tree.getChildNodeCount( path );
	for ( int j = 0; j < count; ++j )
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getChildNode( path, j ).getLastPathComponent();
	    SPMObjectInfo nodeInfo = (SPMObjectInfo) node.getUserObject();
	    nodeInfo.setSelected( true );

	    if (extMap != null) extMap.clear();
	    selectExternals(nodeInfo);
	}
    }


    /**
     *  Description of the Method
     */
    public void doSelectAll()
    {
	selectAllInfos( pluginsPath );
	selectAllInfos( toolScriptsPath );
	selectAllInfos( objectScriptsPath );
	selectAllInfos( startupScriptsPath );
	SPMObjectInfo nodeInfo = getSelectedNodeInfo();
	if ( nodeInfo != null )
	{
	    selectCB.setState( true );
	}
	repaint();
    }


    /**
     *  Description of the Method
     */
    public void doSelectCB()
    {
	SPMObjectInfo nodeInfo = getSelectedNodeInfo();
	if ( nodeInfo != null )
	{
	    nodeInfo.setSelected( selectCB.getState() );

	    if (extMap != null) extMap.clear();
	    selectExternals(nodeInfo);
	}
	repaint();
    }


    /**
     *  Description of the Method
     *
     *@param  info  Description of the Parameter
     */
    protected void notifyObjectInfoSelection( SPMObjectInfo info )
    {
	selectCB.removeEventLink( ValueChangedEvent.class, this );
	selectCB.setState( info.isSelected() );
	selectCB.addEventLink( ValueChangedEvent.class, this, "doSelectCB" );

	super.notifyObjectInfoSelection(info);
    }


    /**
     *  Description of the Method
     *
     *@param  deletable  Description of the Parameter
     */
    public void scriptSelection( boolean deletable )
    {
	//Button.setEnabled( true );
	selectCB.setEnabled( true );
	SPMObjectInfo nodeInfo = getSelectedNodeInfo();
	if ( nodeInfo != null )
	{
	    selectCB.setState( nodeInfo.isSelected() );

	    // disable select and install controls if item is restricted
	    if (nodeInfo.restriction >= SPMParameters.DISABLE) {
		selectCB.setEnabled(false);
		//installSingleButton.setEnabled(false);
	    }

	    // disable select control if item has references
	    if (nodeInfo.refcount > 0) selectCB.setEnabled(false);

	    // disable install single if script has dependents
	    Collection externals = nodeInfo.getExternals();
	    //if (externals != null && externals.size() > 0)
	    //installSingleButton.setEnabled(false);
	}
	super.scriptSelection( deletable );
    }


    /**
     *  Description of the Method
     *
     *@param  deletable  Description of the Parameter
     */
    public void pluginSelection( boolean deletable )
    {
	//installSingleButton.setEnabled( true );
	selectCB.setEnabled( true );
	SPMObjectInfo nodeInfo = getSelectedNodeInfo();
	if ( nodeInfo != null )
	{
	    selectCB.setState( nodeInfo.isSelected() );

	    //System.out.println("externals =" +
	    //	       (nodeInfo.getExternals() != null
	    //		? nodeInfo.getExternals().size()
	    //		: 0));

	    // disable select and install controls if item is restricted
	    if (nodeInfo.restriction >= SPMParameters.DISABLE) {
		selectCB.setEnabled(false);
		//installSingleButton.setEnabled(false);
	    }

	    // disable select control if item has references
	    if (nodeInfo.refcount > 0) selectCB.setEnabled(false);

	    // disable install single if plugin has dependents
	    Collection externals = nodeInfo.getExternals();
	    //if (externals != null && externals.size() > 0)
	    //installSingleButton.setEnabled(false);
	}
	super.pluginSelection( deletable );
    }


    /**
     *  Description of the Method
     */
    public void voidSelection()
    {
	//installSingleButton.setEnabled( false );
	selectCB.setEnabled( false );
	super.voidSelection();
    }


    /**
     *  Sets the repository attribute of the InstallSplitPane object
     *
     *@param  newRep  The new repository value
     */
    public void setRepository( URL newRep )
    {
	repository = newRep;
	fs = new HttpSPMFileSystem( newRep );
	doSetup();
    }

    public void showErrors()
    { if (errors != null && errors.size() > 0) showErrors(errors); }
    
    /**
     *  show errors in a panel
     */
    public static void showErrors(ArrayList errs)
    {
	BTextArea txt = new BTextArea(5, 45);
	txt.setEditable(false);
	txt.setWrapStyle(BTextArea.WRAP_WORD);

	for (int i = 0; i < errs.size(); i++)
	    txt.append(errs.get(i) + "\n");

	BScrollPane detail =
	    new BScrollPane(txt, BScrollPane.SCROLLBAR_NEVER,
		    BScrollPane.SCROLLBAR_AS_NEEDED);

	BLabel messg = SPMTranslate.bLabel("errMsg");

	new BStandardDialog("SPManager",
		new Widget[] { messg, detail },
		BStandardDialog.WARNING)
	.showMessageDialog(null);
    }

}


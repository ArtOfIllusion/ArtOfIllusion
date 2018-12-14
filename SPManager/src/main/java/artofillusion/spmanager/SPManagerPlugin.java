
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
import artofillusion.ui.*;
import artofillusion.util.SearchlistClassLoader;
import buoy.event.*;
import buoy.widget.*;
import java.awt.Window;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

/**
 *  The Plugin corresponding to the SPManager
 *
 *@author     Francois Guillet
 *@created    20 march 2004
 */
public class SPManagerPlugin implements Plugin
{
    private static final Logger logger = Logger.getLogger(SPManagerPlugin.class.getName());
    
    public static String AOI_VERSION;
    public static String TEMP_DIR;
    public static String APP_DIRECTORY;
    public static String PLUGIN_DIRECTORY;
    public static String TOOL_SCRIPT_DIRECTORY;
    public static String OBJECT_SCRIPT_DIRECTORY;
    public static String STARTUP_SCRIPT_DIRECTORY;

    public static final int DOWNLOAD = -1;

    private static SPManagerFrame spmFrame;

    public SPManagerPlugin() {
        AOI_VERSION = ArtOfIllusion.getMajorVersion();
    }
    
    /**
     *  Description of the Method
     *
     *@param  message  Description of the Parameter
     *@param  args     Description of the Parameter
     */
    @Override
    public void processMessage( int message, Object args[] )
    {

	switch (message) {
	case Plugin.APPLICATION_STARTING:
	    SPMTranslate.setLocale(ArtOfIllusion.getPreferences().getLocale());

	    APP_DIRECTORY = ArtOfIllusion.APP_DIRECTORY;
	    PLUGIN_DIRECTORY = ArtOfIllusion.PLUGIN_DIRECTORY;
	    TOOL_SCRIPT_DIRECTORY = ArtOfIllusion.TOOL_SCRIPT_DIRECTORY;
	    OBJECT_SCRIPT_DIRECTORY = ArtOfIllusion.OBJECT_SCRIPT_DIRECTORY;
	    STARTUP_SCRIPT_DIRECTORY = ArtOfIllusion.STARTUP_SCRIPT_DIRECTORY;

	    System.out.println("SPManager starting...");

	    // get details of plugin classloaders
	    int i;
	    URL urlList[];

	    ClassLoader ldr = null;
	    URLClassLoader urlldr = null;
	    SearchlistClassLoader searchldr = null;
	    Object obj;

	    java.util.List list = PluginRegistry.getPluginClassLoaders();
	    HashMap loaders = new HashMap(list.size());
	    for (i = 0; i < list.size(); i++) {
		obj = list.get(i);
		if (obj instanceof URLClassLoader)
		    urlList = ((URLClassLoader) obj).getURLs();
		else
		    urlList = ((SearchlistClassLoader) obj).getURLs();

		if (urlList.length > 0)
		    loaders.put(urlList[0], obj);
	    }

	    Method addUrl = null;

	    try {
		addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		addUrl.setAccessible(true);
	    } catch (Exception e) {
		System.out.println("Error getting addURL method: " + e);
	    }

	    // get details of all local plugins
	    SPMObjectInfo info;
	    StringBuffer errs = null;

	    File files[], urlfile;
	    URL url;
	    Map.Entry entry;
	    String key[], value;
	    File plugdir = new File(PLUGIN_DIRECTORY);
	    if (plugdir.exists()) {
		files = plugdir.listFiles();
		for (i = 0; i < files.length; i++) {
		    info = new SPMObjectInfo(files[i].getAbsolutePath());

		    if (info.invalid) {
			if (errs == null) errs = new StringBuffer(1024);
			if (errs.length() > 0) errs.append('\n');
			errs.append(SPMTranslate.text("pluginFailure",
				info.getName()));
		    }

		    if (info.actions != null && info.actions.size() > 0) {

			try {
			    url = files[i].toURI().toURL();
			} catch (Exception e) {
			    continue;
			}

			//System.out.println("SPM: url=" + url);

			// get the classloader for the current plugin
			obj = loaders.get(url);

			if (obj == null) {
			    System.out.println("SPManager: could not find classloader: " + files[i].getPath());
			    continue;
			}

			// cast or convert it to a SearchlistClassLoader
			if (obj instanceof SearchlistClassLoader) {
			    searchldr = (SearchlistClassLoader) obj;
			    //System.out.println("loader is a srchloader");
			}
			else {
			    urlldr = (URLClassLoader) obj;
			}

			// ok, now perform the actions
			for (Iterator iter = info.actions.entrySet().iterator();
			iter.hasNext(); ) {

			    entry = (Map.Entry) iter.next();
			    key = entry.getKey().toString().split(":");

			    //System.out.println("SPM: action=" +
			    //	       entry.getValue().toString());

			    try {
				if (key[0].startsWith("/"))
				    urlfile = new File(APP_DIRECTORY, key[0].substring(1));
				else
				    urlfile = new File(plugdir, key[0]);
				
				url = urlfile.toURI().toURL();
			    } catch (Exception e) {
				System.out.println("Error making url: " + e);
				continue;
			    }

			    System.out.println("SPM: adding path: " + url);

			    value = entry.getValue().toString();
                            
			    if ("classpath".equalsIgnoreCase(value)) {
				if (searchldr != null)
				    searchldr.add(url);
				else if (addUrl != null) {
				    try {
					//urlarg[0] = url;
					//addUrl.invoke(urlldr, urlarg);        // non-varargs call (1.4)
					addUrl.invoke(urlldr, url);		// varargs call (1.5)
				    } catch (Exception e) {
					System.out.println("Error invoking: "
						+ e);
				    }
				}
				else System.out.println("Could not add path" +
					url);				    
			    }
			    else if ("import".equalsIgnoreCase(value)) {
				ldr = (ClassLoader) loaders.get(url);
				
				if (key.length == 1) {
				    if (obj != null) searchldr.add(ldr);
				    else System.out.println("SPM: could not find"
					    + " loader for: " + url);
				}
			    }
			}
		    }
		}

		if (errs != null) {
		    BTextArea txt = new BTextArea(5, 45);
		    txt.setEditable(false);

		    txt.append(errs.toString());

		    BScrollPane detail =
			new BScrollPane(txt, BScrollPane.SCROLLBAR_NEVER,
				BScrollPane.SCROLLBAR_AS_NEEDED);

		    BLabel messg = SPMTranslate.bLabel("loadError");

		    new BStandardDialog("SPManager initialise", new Widget[] { messg, detail }, BStandardDialog.ERROR).showMessageDialog(null);
		}
	    }
	    else System.out.println("SPManager: could not find plugin dir: " + PLUGIN_DIRECTORY);


	    init();
	    break;

	case Plugin.SCENE_WINDOW_CREATED:
	{
	    LayoutWindow layout = (LayoutWindow) args[0];
	    
	    BMenu toolsMenu = layout.getToolsMenu();
	    toolsMenu.addSeparator();
            BMenuItem menuItem = new BMenuItem(Translate.text("spmanager:menu.SPManager"));
            menuItem.addEventLink(CommandEvent.class, this, "doMenu");
	    

	    toolsMenu.add( menuItem );
	}
	break;

	case APPLICATION_STOPPING:
	    
	    break;
	    
	case DOWNLOAD:
	{
	    System.out.println("DOWNLOAD...");

	    BFrame frame = (BFrame) args[0];
	    URL from = (URL) args[1];
	    URL to = (URL) (args.length > 2 ? args[2] : null);

	    download(frame, from, to);
	}
	break;

	default:
	    //System.out.println("SPManagerPlugin: ignoringmessage: " + message);
	}
    }

    /**
     *  initialise the plugin
     */
    public void init()
    {

	ArrayList err = new ArrayList(8);

	System.out.println("SPManager: java temp dir is " + System.getProperty("java.io.tmpdir"));

	// try system TEMP directory
	File temp = new File(System.getProperty("java.io.tmpdir"));

	// try 'temp' in AOI installation directory
	if (! ((temp.exists() && temp.isDirectory()) || temp.mkdir())) {
	    System.out.println("SPManager: could not open/create temp dir: " +
		    temp.getAbsolutePath());

	    temp = new File(APP_DIRECTORY, "temp");
	}

	// try 'SPtemp' in user's home directory
	if (! ((temp.exists() && temp.isDirectory()) || temp.mkdir())) {
	    System.out.println("Cannot create temp folder: " +
		    temp.getAbsolutePath());

	    temp = new File(System.getProperty("user.dir"), "SPMtemp");
	}

	if (! ((temp.exists() && temp.isDirectory()) || temp.mkdir())) {
	    err.add("Cannot create temp folder: " +
		    temp.getAbsolutePath());
	}

	if (!temp.canWrite())
	    err.add("Write permission denied to temp folder: " +
		    temp.getAbsolutePath());

	// create temporary private sub-tree
	String path;
	File t = null;
	try {
	    t = File.createTempFile("spmanager-temp-"+ System.getProperty("user.name") + "-", ".lck", temp);
	    t.deleteOnExit();
	    path= t.getName();
	    path = path.substring(0, path.length()- ".lck".length());
	} catch (Exception e) {
	    // failed to create temp file, use fallback naming algorithm
	    System.out.println("SPManager: could not create temp file: " + t.getAbsolutePath() + e);

	    path = System.getProperty("user.name") + "-" + String.valueOf(System.currentTimeMillis());
	}
	
	temp = new File(temp, path);

	if (! ((temp.exists() && temp.isDirectory()) || temp.mkdir())) {
	    err.add("Cannot create temp folder: " +
		    temp.getAbsolutePath());
	}

	if (!temp.canWrite())
	    err.add("Write permission denied to temp folder: " + temp.getAbsolutePath());

	TEMP_DIR = temp.getAbsolutePath();

	System.out.println("SPManager: temp dir set to: " + temp.getAbsolutePath());

	// make sure all temp directories are created
	File subfolder = new File(PLUGIN_DIRECTORY);
	temp = new File(TEMP_DIR, subfolder.getName());
	if (!temp.exists() && !temp.mkdirs())
	    err.add("Cannot create temp plugin folder: " +
		    temp.getAbsolutePath());

	subfolder = new File(TOOL_SCRIPT_DIRECTORY);
	temp = new File(TEMP_DIR, subfolder.getName());
	if (!temp.exists() && !temp.mkdirs())
	    err.add("Cannot create temp script folder: " +
		    temp.getAbsolutePath());

	subfolder = new File(OBJECT_SCRIPT_DIRECTORY);
	temp = new File(TEMP_DIR, subfolder.getName());
	if (!temp.exists() && !temp.mkdirs())
	    err.add("Cannot create temp script folder: " +
		    temp.getAbsolutePath());

	subfolder = new File(STARTUP_SCRIPT_DIRECTORY);
	temp = new File(TEMP_DIR, subfolder.getName());
	if (!temp.exists() && !temp.mkdirs ())
	    err.add("Cannot create temp script folder: " +
		    temp.getAbsolutePath());

	// make sure all live directories exist
	temp = new File(PLUGIN_DIRECTORY);
	if (!temp.exists() && !temp.mkdir())
	    err.add("Cannot create missing plugin folder: " +
		    temp.getAbsolutePath());

	temp = new File(TOOL_SCRIPT_DIRECTORY);
	if (!temp.exists() && !temp.mkdirs())
	    err.add("Cannot create missing script folder: " +
		    temp.getAbsolutePath());

	temp = new File(OBJECT_SCRIPT_DIRECTORY);
	if (!temp.exists() && !temp.mkdirs())
	    err.add("Cannot create missing script folder: " +
		    temp.getAbsolutePath());

	temp = new File(STARTUP_SCRIPT_DIRECTORY);
	if (!temp.exists() && !temp.mkdirs ())
	    err.add("Cannot create missing script folder: " +
		    temp.getAbsolutePath());


	if (err.size() > 0) {
	    BTextArea txt = new BTextArea(5, 45);
	    txt.setEditable(false);

	    for (int i = 0; i < err.size(); i++)
		txt.append(err.get(i) + "\n");

	    BScrollPane detail = new BScrollPane(txt, BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_AS_NEEDED);
	    BLabel messg = SPMTranslate.bLabel("errMsg");
	    new BStandardDialog("SPManager initialise", new Widget[] { messg, detail }, BStandardDialog.WARNING) .showMessageDialog(null);
	}
    }

    public void registerResource(String type, String id, ClassLoader loader, String baseName, Locale locale)
    {
	String suffix = "";

	if (locale.getLanguage().length() > 0)
	    suffix += "_" + locale.getLanguage();
	if (locale.getCountry().length() > 0)
	    suffix += "_" + locale.getCountry();
	if (locale.getVariant().length() > 0)
	    suffix += "_" + locale.getVariant();

	URL url;
	int cut;

	for (int i = 0; i < 3; i++) {
	    try {
		url = loader.getResource(baseName + suffix + ".properties");

		if (url != null) {
		    PluginRegistry.registerResource(type, id, loader, url.getPath(), locale);
		    break;
		}
	    } catch (Exception e) {}

	    // can we remove part of the suffix?
	    cut = suffix.lastIndexOf('_');
	    if (cut > 0) suffix = suffix.substring(0, cut);
	    else break;
	}
    }

    public void download(BFrame frame, URL from)
    { download(frame, from, null); }

    /**
     *  download (and install if possible) the specified file(set).
     */
    public void download(BFrame frame, URL from, URL to)
    {
	final BFrame context = frame;
	final URL url = from;
	final URL toUrl = to;

	final StatusDialog status = new StatusDialog(context) {
	    SPMObjectInfo info;
	    BLabel size;
	    BButton okbtn;
	    String filename, name;
	    ColumnContainer col;
	    RowContainer buttons;
	    BTextField savePath;
	    Thread worker;

            @Override
	    public void setVisible(boolean vis)
	    {
		if (!vis) {
		    super.setVisible(vis);
		    return;
		}

		col = (ColumnContainer) getContent();

		filename = url.getFile();
		int cut = filename.lastIndexOf('/');
		if (cut > 0 && cut < filename.length())
		    filename = filename.substring(cut+1);

		cut = filename.lastIndexOf('?');
		if (cut > 0)
		    filename = filename.substring(0, cut);

		name = new String(filename);
		cut = name.lastIndexOf('.');
		if (cut > 0) name = name.substring(0, cut);

		setText(name);
		setProgressText(SPMTranslate.text("clickStart"));

		okbtn = SPMTranslate.bButton("start", this, "ok");

		buttons = new RowContainer();
		buttons.add(okbtn);
		buttons.add(SPMTranslate.bButton("cancel", this, "close"));

		okbtn.setActionCommand("ok");

		col.add(buttons);
		pack();
		super.setVisible(true);
	    }

	    public void close()
	    {
		if (worker != null) worker.interrupt();
		doClose();
	    }

	    public void ok(CommandEvent ev)
	    {
		String cmd = ev.getActionCommand();
		okbtn.setEnabled(false);

		final BButton okbut = okbtn;

		final StatusDialog stat = this;

		if (cmd.equals("ok")) {

		    setProgressText(SPMTranslate.text("contacting"));
		    setIdle(true);

		    System.out.println("DOWNLOAD: creating ObjectInfo..."+
			    url.toString());
		    worker = new Thread() {
                        @Override
			public void run()
			{
			    info = new SPMObjectInfo(url);
			    if (info.length == 0) {
				System.out.println("DOWNLOAD: no info");
				info.length = info.getRemoteFileSize(url.toString());
			    }


			    // get destination if needed
			    if (info.name == null
				    || info.name.length() == 0) {

				System.out.println("need path...");

				RowContainer row = new RowContainer();
				row.add(SPMTranslate.bLabel("savePath"));
				savePath = new BTextField("", 25);
				savePath.addEventLink(ValueChangedEvent.class, this, "savePath");
				row.add(savePath);
				row.add(SPMTranslate.bButton("browse",
					this,
				"browse"));
				col.remove(buttons);
				col.add(row);
				col.add(buttons);
				pack();
			    }

			    long total = info.getTotalLength();

			    String sz = (total > 1000000
				    ? " " +
					    (total/1000000)
					    + " MB"
					    : total > 1000
					    ? " " + (total/1000)
						    + " kB"
						    : (total > 0)
						    ? " " + total
							    + " bytes"
							    : "");

			    setText(info.getName() + " " + sz);
			    setIdle(false);
			    setProgressText(SPMTranslate.text("ready"));

			    okbut.setActionCommand("install");
			    okbut.setText(SPMTranslate.text("install"));
			    okbut.setEnabled(true);
			    pack();
			}

			public void savePath()
			{
			    String val = savePath.getText();
			    okbtn.setEnabled(val != null && val.length() > 0);
			}

			public void browse()
			{
			    BFileChooser fc = new
			    BFileChooser(BFileChooser.SAVE_FILE,
				    Translate.text("savePath"));

			    File file = null;
			    String path = null;
			    String fname = savePath.getText();

			    if (fname == null || fname.length() == 0) {
				path = System.getProperty("user.home");
				fname = filename;
			    }

			    if (fname != null) {
				if (path != null)
				    file = new File(path, fname);
				else
				    file = new File(fname);

				fc.setDirectory(file.getParentFile());
				fc.setSelectedFile(file);
			    }
			    else fc.setDirectory(new File(path));

			    if (fc.showDialog(context)) {
				savePath.setText(fc.getSelectedFile().getAbsolutePath());
			    }
			}

		    };

		    worker.start();

		}
		else if (cmd.equals("install")) {
		    System.out.println("DOWNLOAD: downloading " +
			    url.toString());

		    setText(SPMTranslate.text("downloading", info.getName()));
		    pack();

		    final ArrayList errs = new ArrayList();
		    worker = new Thread() {
                        @Override
			public void run()
			{
			    long total = info.getTotalLength();

			    // full save path
			    String path = null;
			    if (savePath != null)
				path = savePath.getText();

			    else if (info.name != null)
				path = ArtOfIllusion.PLUGIN_DIRECTORY + File.separatorChar + info.name + ".jar";


			    if (path == null || path.length() == 0) {
				System.out.println("DOWNLOAD: no save location");
				new BStandardDialog("SPManager", SPMTranslate.text("noSaveLocation"), BStandardDialog.ERROR).showMessageDialog(null);

				doClose();
			    }

			    System.out.println("DOWNLOAD: downloading file...");
			    if (total > 0)
				setBarValue(total > 0 ? 0 : -1);

			    long dl = HttpSPMFileSystem
			    .downloadRemoteBinaryFile(url, path, info.length, stat, total, 0, errs);

			    for (int i = 0; info.files != null && i < info.files.length; i++) {
				if (worker.interrupted()) {
				    doClose();
				    return;
				}

				name = PLUGIN_DIRECTORY + File.separatorChar + info.destination.get(i) + info.files[i];

				setText(SPMTranslate.text("downloading", info.files[i]));
				pack();

				dl += HttpSPMFileSystem
				.downloadRemoteBinaryFile(info.getAddFileURL(i), name, info.fileSizes[i], stat, total, dl, errs);
			    }

			    if (errs != null && errs.size() > 0)
				InstallSplitPane.showErrors(errs);
			    else
				new BStandardDialog("SPManager", SPMTranslate.text("modified"), BStandardDialog.ERROR).showMessageDialog(null);

			    System.out.println("DOWNLOAD: done");
			    doClose();
			}
		    };

		    worker.start();
		}
		else System.out.println("?? cmd=" + cmd);
	    }
	};
    }

    /**
     *  Description of the Method
     */
    public void doMenu()
    {
	// NTJ: don't implement yet... (Nov 2006)
	//if (!Translate.getLocale().equals(Locale.getDefault())) {
	//}

	if ( spmFrame == null )
	    spmFrame = new SPManagerFrame();
	( (Window) spmFrame.getComponent() ).toFront();
	//spmFrame.layoutChildren();
	( (Window) spmFrame.getComponent() ).show();
	//spmFrame.printBounds( spmFrame );
    }


    /**
     *  restart the plugin
     */
    public static void restart()
    {
	SPManagerFrame old = spmFrame;

	spmFrame = new SPManagerFrame();
	((Window) spmFrame.getComponent()).toFront();
	((Window) spmFrame.getComponent()).show();

	old.dispose();
    }

    /**
     *  close the frame
     */
    public void close()
    {
        if(null == spmFrame) return;

        spmFrame.setVisible(false);
        spmFrame.dispose();
    }


    /**
     *  Gets the name attribute of the SPManagerPlugin object
     *
     *@return    The name value
     */
    public String getName()
    {
	return "Script and Plugin Manager";
    }


    /**
     *  Gets the frame attribute of the SPManagerPlugin class
     *
     *@return    The frame value
     */
    public static SPManagerFrame getFrame()
    {
	return spmFrame;
    }


}


/*  PostInstall.java  */

package artofillusion.spmanager.postinstall;

/*
 * PostInstall: perform post-install cleanup
 *
 * Copyright (C) 2006 Nik Trevallyn-Jones, Sydney Australia
 *
 * Author: Nik Trevallyn-Jones, nik777@users.sourceforge.net
 * $Id: Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * with this program. If not, the license is available from the
 * GNU project, at http://www.gnu.org.
 */

import artofillusion.*;
import artofillusion.ui.*;

import buoy.widget.*;

import java.io.*;
import java.util.*;

/**
 *  AOI plugin to clean up after plugin installation/upgrade
 */

public class PostInstall implements Plugin
{
    protected ArrayList ok, err;
    protected static File tempDir = null;

    public void processMessage(int msg, Object[] args)
    {
	int i, k;

	switch (msg) {
	case Plugin.APPLICATION_STARTING:
	    try {

		// find the SPManager temp dir
		if (tempDir == null)
		    tempDir = new File(System.getProperty("java.io.tmpdir"));

		if (!tempDir.exists())
		    tempDir = new File(ArtOfIllusion.APP_DIRECTORY, "temp");

		if (!tempDir.exists())
		    tempDir = new File(System.getProperty("user.dir"), "SPMtemp");

		if (!tempDir.exists()) {
		    System.out.println("PostInstall: No TEMP dir found");
				       		    
		    tempDir = null;
		    return;
		}

		// get the correct sub-tree
		String prefix = "spmanager-temp-"
		    + System.getProperty("user.name") + "-";
		File lockfile;
		String[] sub = tempDir.list();
		for (i = 0; i < sub.length; i++) {
		    if (sub[i].startsWith(prefix) && (!sub[i].endsWith(".lck"))) {
			lockfile = new File(tempDir, sub[i] + ".lck");
			
			// no lock-file means not active
			if (!lockfile.exists()) {
			    tempDir = new File(tempDir, sub[i]);
			    break;
			}
		    }
		}

		// if no sub-tree found, exit now
		if (!tempDir.getName().startsWith("spmanager-temp-")) {
		    System.out.println("PostInstall: no TEMP sub-tree found");
		    
		    tempDir = null;
		    return;
		}
		
		System.out.println("PostInstall: tempDir is " +
			tempDir.getAbsolutePath());
		
		ok = new ArrayList(128);
		err = new ArrayList(128);

		cleanup(ArtOfIllusion.PLUGIN_DIRECTORY, ok, err);
		cleanup(ArtOfIllusion.TOOL_SCRIPT_DIRECTORY, ok, err);
		cleanup(ArtOfIllusion.OBJECT_SCRIPT_DIRECTORY, ok, err);
		cleanup(ArtOfIllusion.STARTUP_SCRIPT_DIRECTORY, ok, err);
	    } catch (Exception e) {
		System.out.println("PostInstall: exception raised - aborting: "
				   + e.toString());

		err.add("Exception raised - aborting: " + e.toString());
	    }
	    finally {
		
		if (tempDir == null) return;
		
		// delete the temp tree
		try {
		    File tmp;
		    String[] sub = tempDir.list();
		    ArrayList list = new ArrayList(sub.length);
		    for (i = 0; i < sub.length; i++)
			list.add(tempDir + File.separator + sub[i]);

		    for (i = 0; i < list.size(); i++) {
			tmp = new File((String) list.get(i));

			// make sure we empty all sub-directories first
			if (tmp.isDirectory()) {
			    sub = tmp.list();
			    if (sub.length > 0) {
				System.out.println("PostInstall: descending into "
					+ tmp.getAbsolutePath());

				for (k = 0; k < sub.length; k++) {
				    list.add(i, tmp.getAbsolutePath() +
					    File.separator + sub[k]);
				    
				    //System.out.println("PI: added: "
					//    	+ tmp.getAbsolutePath() + " : " + sub[k]);
				}
				
				// continue processing from this element
				i--;
				continue;
			    }
			}

			System.out.println("PostInstall: deleting " + 
				tmp.getAbsolutePath());

			tmp.delete();
		    }
		}
		catch (Exception e) {
		    System.out.println("PostInstall: error: " + e);
		    e.printStackTrace();
		}
		
		System.out.println("PostInstall: deleting "
			+ tempDir.getAbsolutePath());
		
		tempDir.delete();
	    }
	    break;

	case Plugin.SCENE_WINDOW_CREATED:
	    try {
		if (err != null && err.size() > 0) {
		    BTextArea txt = new BTextArea(5, 45);
		    txt.setEditable(false);

		    for (i = 0; i < err.size(); i++)
			txt.append(err.get(i) + "\n");

		    BScrollPane detail =
			new BScrollPane(txt, BScrollPane.SCROLLBAR_NEVER,
					BScrollPane.SCROLLBAR_AS_NEEDED);

		    BLabel messg = Translate.label("postinstall.errMsg");

		    new BStandardDialog("PostInstall",
					new Widget[] { messg, detail },
					BStandardDialog.WARNING)
			.showMessageDialog(null);
		    
		}

		if (ok != null && ok.size() > 0) {

		    BTextArea txt = new BTextArea(5, 45);
		    txt.setEditable(false);
		    //txt.setText("PostInstall:\n");

		    for (i = 0; i < ok.size(); i++)
			txt.append(ok.get(i).toString() + "\n");

		    BScrollPane detail =
			new BScrollPane(txt, BScrollPane.SCROLLBAR_NEVER,
					BScrollPane.SCROLLBAR_AS_NEEDED);
		    
		    BLabel messg = Translate.label("postinstall:okMsg");

		    BLabel restart = Translate.label("postinstall:restartMsg");

		    new BStandardDialog("PostInstall: ", new Widget[] {
					    messg, restart, detail
					},
					BStandardDialog.INFORMATION)
			.showMessageDialog(null);
		}
	    }
	    finally {
		ok = null;
		err = null;
	    }
	}
    }

    /**
     *  cleanup any incomplete file downloads in the specified directory
     *
     *  @param path - the String representation of the pathname
     */
    public static void cleanup(String path, ArrayList ok, ArrayList err)
    {
	File from, to;
	File plugin, update;
	String fname;
	int count = 0;

	to = new File(path);
	from = new File(tempDir, to.getName());

	if (!from.exists()) {
	    System.out.println("PostInstall: FROM path does not exist: " +
			       from.getAbsolutePath());
	    return;
	}

	if (!to.exists()) {
	    System.out.println("PostInstall: TO path does not exist: " +
			       to.getAbsolutePath());
	    return;
	}

	String[] files = from.list();
	if (files == null || files.length == 0) return;

	//System.out.println("PostInstall: cleaning up " + path);

	// iterate all filenames in the directory
	for (int i = 0; i < files.length; i++) {

	    // only process filename that look like an update file
	    if (files[i].endsWith(".upd")) {

		update = new File(from, files[i]);
				
		// if the file is zero-length, jut try to delete it
		if (update.length() == 0) {
		    if (update.delete()) {
			System.out.println("PostInstall: " +
					   "deleted zero-length file: " +
					   update.getAbsolutePath());
		    }
		    else {
			System.out.println("PostInstall.cleanup: " +
					   "Could not delete: " +
					   update.getAbsolutePath());

			err.add("could not delete " +
				update.getAbsolutePath());
		    }

		    continue;	// skip to next file
		}


		fname = files[i].substring(0,
					   files[i].length()-".upd".length());

		plugin = new File(to, fname);

		//System.out.println("Checking: " + files[i] + " -> " + fname);

		// if the corresponding plugin also exists then fix it
		if (plugin.exists()) plugin.delete();

		if (update.renameTo(plugin)) {
		    System.out.println("PostInstall.cleanup: "+
				       "Updated " + fname);

		    ok.add("Updated " + fname);
		}
		else {
		    InputStream is = null;
		    OutputStream os = null;
		    try {
			is = new BufferedInputStream(new FileInputStream(update));
			os = new BufferedOutputStream(new FileOutputStream(plugin));

			int b;
			while ((b = is.read()) >= 0) os.write((byte) b);

			System.out.println("PostInstall.cleanup: "+
					   "Updated " + fname);

			ok.add("Updated (copied) " + fname);
		    }
		    catch (Exception e) {
			System.out.println("PostInstall.cleanup: "+
					   "**Error updating " + fname);

			err.add("couldn't rename or copy " + fname);
		    }
		    finally {
			try {
			    os.flush();
			    os.close();
			    is.close();
			} catch (Exception e) {}

			if (!update.delete()) {
			    System.out.println("PostInstall.cleanup: " +
					       "**Error: Could not delete: " +
					       update.getAbsolutePath());

			    err.add("couldn't delete " +
				    update.getAbsolutePath());
			    
			    // set file to zero-length
			    try {
				RandomAccessFile raf =
				    new RandomAccessFile(update, "rw");

				raf.setLength(0);
				raf.close();				
			    } catch (Exception e) {
				System.out.println("PostInstall: "
					+ Translate.text("postinstall:truncateMsg",
						update.getAbsolutePath()));
			    }
			}
		    }
		}
	    }
	}
    }
 
}

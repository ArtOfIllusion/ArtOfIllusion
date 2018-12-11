/* Copyright 2004 Francois Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
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

public class LocalSPMFileSystem extends SPMFileSystem
{   
    public LocalSPMFileSystem()
    {
        super();
    }
    
    public void initialize()
    {
        super.initialize();
        scanPlugins();
        scanToolScripts();
        scanObjectScripts();
        scanStartupScripts();
        initialized = true;
    }
    
    
    private void scanPlugins()
    {
        scanFiles(SPManagerPlugin.PLUGIN_DIRECTORY, pluginsInfo, ".jar");
    }
    
    private void scanToolScripts()
    {
        scanFiles(SPManagerPlugin.TOOL_SCRIPT_DIRECTORY, toolInfo, ".bsh");
    }
    
    private void scanObjectScripts()
    {
        scanFiles(SPManagerPlugin.OBJECT_SCRIPT_DIRECTORY, objectInfo, ".bsh");
    }
    
    private void scanStartupScripts()
    {
        scanFiles(SPManagerPlugin.STARTUP_SCRIPT_DIRECTORY, startupInfo, ".bsh");
    }
    
    private void scanFiles(String directory, Vector infoVector, String suffix)
    {
        SPMObjectInfo info;
        
        File dir = new File(directory);
        if (dir.exists())
        {
            String[] files = dir.list();
            if (files.length > 0) Arrays.sort(files);
            for (int i = 0; i < files.length; i++)
                if (files[i].endsWith(suffix))
                {   info = new SPMObjectInfo(directory+File.separatorChar+files[i]);
                    infoVector.add(info);
                }
        }
    }

}

/* Copyright 2004 Francois Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
*/

package artofillusion.spmanager;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalSPMFileSystem extends SPMFileSystem
{   
    private static final Logger logger = Logger.getLogger(LocalSPMFileSystem.class.getName());
    
    public LocalSPMFileSystem()
    {
        super();
    }
    
    @Override
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
    
    private void scanFiles(String directory, List<SPMObjectInfo> infoList, String suffix)
    {

        Predicate<Path> pp = (Path t) -> t.getFileName().toString().endsWith(suffix);
        Function<Path, SPMObjectInfo> pts = (Path t) -> new SPMObjectInfo(t.toString());
        try {
            Files.walk(Paths.get(directory)).sorted().filter(pp).map(pts).forEach((item) -> {infoList.add(item);});
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }        
    }
    


}

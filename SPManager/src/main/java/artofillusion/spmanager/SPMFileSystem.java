/* Copyright 2004 Francois Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
*/

package artofillusion.spmanager;

import java.util.ArrayList;
import java.util.List;

public class SPMFileSystem
{   
    protected List<SPMObjectInfo> pluginsInfo;
    protected List<SPMObjectInfo> toolInfo;
    protected List<SPMObjectInfo> objectInfo;
    protected List<SPMObjectInfo> startupInfo;
    
    protected boolean initialized;
    
    public final static short PLUGIN_TYPE = 0;
    public final static short TOOL_SCRIPT_TYPE = 1;
    public final static short OBJECT_SCRIPT_TYPE = 2;
    public final static short STARTUP_SCRIPT_TYPE = 3;
    
    public SPMFileSystem()
    {
        pluginsInfo = new ArrayList<>();
        toolInfo = new ArrayList<>();
        objectInfo = new ArrayList<>();
        startupInfo = new ArrayList<>();
        initialized = false;
    }
    
    public short getInfoType(SPMObjectInfo info)
    {
        for(SPMObjectInfo item: pluginsInfo) {
            if(info == item) return PLUGIN_TYPE;
        }
        for(SPMObjectInfo item: toolInfo) {
            if(info == item) return TOOL_SCRIPT_TYPE;
        }
        for(SPMObjectInfo item: objectInfo) {
            if(info == item) return OBJECT_SCRIPT_TYPE;
        }
        for(SPMObjectInfo item: startupInfo) {
            if(info == item) return STARTUP_SCRIPT_TYPE;
        }
        return PLUGIN_TYPE;
    }
    
    public void deleteInfo(SPMObjectInfo info)
    {
        pluginsInfo.remove(info);
        toolInfo.remove(info);
        objectInfo.remove(info);
        startupInfo.remove(info);
    }
    
    public void downloadRemoteFile(SPMObjectInfo nodeInfo, String fileName)
    {
        System.out.println("Download request for a non able SPMFileSystem");
    }
    
    public void initialize()
    {
        pluginsInfo.clear();
        toolInfo.clear();
        objectInfo.clear();
        startupInfo.clear();
    }
    
    public List<SPMObjectInfo> getPlugins()
    {
        if (!initialized) initialize();
        return pluginsInfo;
    }
    
    public List<SPMObjectInfo> getToolScripts()
    {
        if (!initialized) initialize();
        return toolInfo;
    }
    
    public List<SPMObjectInfo> getObjectScripts()
    {
        if (!initialized) initialize();
        return objectInfo;
    }
    
    public List<SPMObjectInfo> getStartupScripts()
    {
        if (!initialized) initialize();
        return startupInfo;
    }
    
    public void getRemoteInfo(Runnable cb)
    {
        if (!initialized) initialize();
    }
}

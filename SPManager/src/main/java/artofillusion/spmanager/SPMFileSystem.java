/* Copyright 2004 Francois Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
*/

package artofillusion.spmanager;

import java.util.Vector;
import artofillusion.*;

public class SPMFileSystem
{   
    protected Vector pluginsInfo, toolInfo, objectInfo, startupInfo;
    protected boolean initialized;
    
    public final static short PLUGIN_TYPE = 0;
    public final static short TOOL_SCRIPT_TYPE = 1;
    public final static short OBJECT_SCRIPT_TYPE = 2;
    public final static short STARTUP_SCRIPT_TYPE = 3;
    
    public SPMFileSystem()
    {
        pluginsInfo = new Vector();
        toolInfo = new Vector();
        objectInfo = new Vector();
        startupInfo = new Vector();
        initialized = false;
    }
    
    public short getInfoType(SPMObjectInfo info)
    {
        for (int i = 0; i < pluginsInfo.size(); ++i)
            if (info == pluginsInfo.elementAt(i)) return PLUGIN_TYPE;
        for (int i = 0; i < toolInfo.size(); ++i)
            if (info == toolInfo.elementAt(i)) return TOOL_SCRIPT_TYPE;
        for (int i = 0; i < objectInfo.size(); ++i)
            if (info == objectInfo.elementAt(i)) return OBJECT_SCRIPT_TYPE;
        for (int i = 0; i < startupInfo.size(); ++i)
            if (info == startupInfo.elementAt(i)) return STARTUP_SCRIPT_TYPE;
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
    
    public Vector getPlugins()
    {
        if (!initialized) initialize();
        return pluginsInfo;
    }
    
    public Vector getToolScripts()
    {
        if (!initialized) initialize();
        return toolInfo;
    }
    
    public Vector getObjectScripts()
    {
        if (!initialized) initialize();
        return objectInfo;
    }
    
    public Vector getStartupScripts()
    {
        if (!initialized) initialize();
        return startupInfo;
    }
    
    public void getRemoteInfo(Runnable cb)
    {
        if (!initialized) initialize();
    }
}

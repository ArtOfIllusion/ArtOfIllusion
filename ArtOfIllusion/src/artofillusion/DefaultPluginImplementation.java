/* 
   Copyright (C) 2019 by Maksim Khramov

 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details. */
package artofillusion;

import java.io.File;

/**
 *
 * @author maksim.khramov
 */

 public class DefaultPluginImplementation implements Plugin {
     
    protected void onApplicationStarting() {
    }
    
    protected void onApplicationStopping() {
    }
    
    protected void onSceneWindowCreated(LayoutWindow view) {
    }
   
    protected void onSceneSaved(File file, LayoutWindow view) {
    }

    protected void onSceneClosing(LayoutWindow layoutWindow) {        
    }
    
    protected void onObjectWindowCreated(ObjectEditorWindow objectEditorWindow) {        
    }

    protected void onObjectWindowClosing(ObjectEditorWindow objectEditorWindow) {        
    }
    
    @Override
    public void processMessage(int message, Object... args) {
        switch(message) {
            case Plugin.APPLICATION_STARTING: {
                onApplicationStarting();
                break;
            }
            case Plugin.APPLICATION_STOPPING: {
                onApplicationStopping();
                break;
            }
            
            case Plugin.SCENE_WINDOW_CREATED: {                
                onSceneWindowCreated((LayoutWindow)args[0]);
                break;
            }
            case Plugin.SCENE_WINDOW_CLOSING: {
                onSceneClosing((LayoutWindow)args[0]);
                break;
            }
            
            case Plugin.OBJECT_WINDOW_CREATED: {
                onObjectWindowCreated((ObjectEditorWindow)args[0]);
                break;
            }
            
            case Plugin.OBJECT_WINDOW_CLOSING: {
                onObjectWindowClosing((ObjectEditorWindow)args[0]);
                break;
            }            
            
            case Plugin.SCENE_SAVED: {
                onSceneSaved((File)args[0], (LayoutWindow)args[1]);
                break;
            }
            
            
            default: {
                
            }
        }
    }






    
}

/*
 *  Copyright 2022 by Maksim Khramov
 *  This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 2 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package artofillusion.test.mocks;


import artofillusion.LayoutWindow;
import artofillusion.ModellingTool;
import artofillusion.Plugin;

/**
 *
 * @author MaksK
 */
public class DummyPluginAndTool implements Plugin, ModellingTool {

    @Override
    public void processMessage(int message, Object[] args)
    {
    }
    @Override
    public String getName() {
        return "Dummy Tool Plugin";
    }

    @Override
    public void commandSelected(LayoutWindow window) {
        
    }
    
}

/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import artofillusion.MaterialPreviewer;
import artofillusion.math.Vec3;
import artofillusion.object.Object3D;
import buoy.widget.Widget;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class MaterialMappingTest {
    
    @Test
    public void createMaterialMapping()
    {
        
    }
    
    public static class DummyMapping extends MaterialMapping
    {
        public DummyMapping(Object3D obj, Material mat)
        {
            super(obj, mat);
        }
        
        public DummyMapping(DataInputStream in, Object3D obj, Material mat)
        {
            this(obj, mat);
        }
        
        @Override
        public void writeToFile(DataOutputStream out) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public double getStepSize() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getMaterialSpec(Vec3 pos, MaterialSpec spec, double size, double t) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public MaterialMapping duplicate() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public MaterialMapping duplicate(Object3D obj, Material mat) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void copy(MaterialMapping map) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Widget getEditingPanel(Object3D obj, MaterialPreviewer preview) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

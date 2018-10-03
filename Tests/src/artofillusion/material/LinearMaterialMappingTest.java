/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import artofillusion.Scene;
import artofillusion.object.Cube;
import buoy.widget.BFrame;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author maksim.khramov
 */
@SuppressWarnings("ResultOfObjectAllocationIgnored")
public class LinearMaterialMappingTest{

    @Test
    public void testCreateLMM()
    {
        Cube cube = new Cube(1, 1, 1);
        Material3D mat = new DummyMaterial();
        LinearMaterialMapping lmm = new LinearMaterialMapping(cube, mat);
        
        Assert.assertEquals(mat, lmm.getMaterial());
        Assert.assertEquals("Linear", LinearMaterialMapping.getName());
    }
    
    @Test(expected = InvalidObjectException.class)
    public void testCreateLMMFromStreamBadVersion1() throws IOException
    {
        Cube cube = new Cube(1, 1, 1);
        Material3D mat = new DummyMaterial();
        
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)-1);
        
        new LinearMaterialMapping(new DataInputStream(new ByteArrayInputStream(bytes)), cube, mat);
        
    }
    
    
    @Test(expected = InvalidObjectException.class)
    public void testCreateLMMFromStreamBadVersion2() throws IOException
    {
        Cube cube = new Cube(1, 1, 1);
        Material3D mat = new DummyMaterial();
        
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)2);
        
        new LinearMaterialMapping(new DataInputStream(new ByteArrayInputStream(bytes)), cube, mat);

        //        
        wrap.putDouble(1.0);
        wrap.putDouble(2.0);
        wrap.putDouble(3.0);
        
        wrap.putDouble(0.0);
        wrap.putDouble(45.0);
        wrap.putDouble(90.0);
    }
    
    @Test
    public void testCreateLMMFromStreamVersion0() throws IOException
    {
        Cube cube = new Cube(1, 1, 1);
        Material3D mat = new DummyMaterial();
        
        byte[] bytes = new byte[200];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        
        //  Version
        wrap.putShort((short)0);

        //  Coordinate system data      
        wrap.putDouble(1.0);
        wrap.putDouble(2.0);
        wrap.putDouble(3.0);
        
        wrap.putDouble(0.0);
        wrap.putDouble(45.0);
        wrap.putDouble(90.0);
        
        // dx,dy,dz
        wrap.putDouble(4.0);
        wrap.putDouble(5.0);
        wrap.putDouble(6.0);
        
        // x, y, z scales
        wrap.putDouble(0.1);
        wrap.putDouble(0.5);
        wrap.putDouble(3.5);
        
        LinearMaterialMapping lmm = new LinearMaterialMapping(new DataInputStream(new ByteArrayInputStream(bytes)), cube, mat);
        
        Assert.assertEquals(mat, lmm.getMaterial());
        Assert.assertEquals(false, lmm.isScaledToObject());

        
        Assert.assertEquals(1.0, lmm.coords.getOrigin().x, 0);
        Assert.assertEquals(2.0, lmm.coords.getOrigin().y, 0);
        Assert.assertEquals(3.0, lmm.coords.getOrigin().z, 0);
        
        Assert.assertEquals(0.1, lmm.xscale, 0);
        Assert.assertEquals(0.5, lmm.yscale, 0);
        Assert.assertEquals(3.5, lmm.zscale, 0);
    }  
    
    @Test
    public void testCreateLMMFromStreamVersion1() throws IOException
    {
        Cube cube = new Cube(1, 1, 1);
        Material3D mat = new DummyMaterial();
        
        byte[] bytes = new byte[200];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        
        //  Version
        wrap.putShort((short)1);

        //  Coordinate system data      
        wrap.putDouble(1.0);
        wrap.putDouble(2.0);
        wrap.putDouble(3.0);
        
        wrap.putDouble(0.0);
        wrap.putDouble(45.0);
        wrap.putDouble(90.0);
        
        // dx,dy,dz
        wrap.putDouble(4.0);
        wrap.putDouble(5.0);
        wrap.putDouble(6.0);
        
        // x, y, z scales
        wrap.putDouble(0.1);
        wrap.putDouble(0.5);
        wrap.putDouble(3.5);

        // scale to object
        wrap.put((byte)1);
        
        LinearMaterialMapping lmm = new LinearMaterialMapping(new DataInputStream(new ByteArrayInputStream(bytes)), cube, mat);
        
        Assert.assertEquals(mat, lmm.getMaterial());
        Assert.assertEquals(true, lmm.isScaledToObject());

        Assert.assertEquals(1.0, lmm.coords.getOrigin().x, 0);
        Assert.assertEquals(2.0, lmm.coords.getOrigin().y, 0);
        Assert.assertEquals(3.0, lmm.coords.getOrigin().z, 0);
        
        Assert.assertEquals(0.1, lmm.xscale, 0);
        Assert.assertEquals(0.5, lmm.yscale, 0);
        Assert.assertEquals(3.5, lmm.zscale, 0);
    }
    
    @Test
    public void testCreateLMMFromStreamVersion1Unscaled() throws IOException
    {
        Cube cube = new Cube(1, 1, 1);
        Material3D mat = new DummyMaterial();
        
        byte[] bytes = new byte[200];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        
        //  Version
        wrap.putShort((short)1);

        //  Coordinate system data      
        wrap.putDouble(1.0);
        wrap.putDouble(2.0);
        wrap.putDouble(3.0);
        
        wrap.putDouble(0.0);
        wrap.putDouble(45.0);
        wrap.putDouble(90.0);
        
        // dx,dy,dz
        wrap.putDouble(4.0);
        wrap.putDouble(5.0);
        wrap.putDouble(6.0);
        
        // x, y, z scales
        wrap.putDouble(0.1);
        wrap.putDouble(0.5);
        wrap.putDouble(3.5);

        // scale to object
        wrap.put((byte)0);
        
        LinearMaterialMapping lmm = new LinearMaterialMapping(new DataInputStream(new ByteArrayInputStream(bytes)), cube, mat);
        
        Assert.assertEquals(mat, lmm.getMaterial());
        Assert.assertEquals(false, lmm.isScaledToObject());
        
        Assert.assertEquals(1.0, lmm.coords.getOrigin().x, 0);
        Assert.assertEquals(2.0, lmm.coords.getOrigin().y, 0);
        Assert.assertEquals(3.0, lmm.coords.getOrigin().z, 0);
        
        Assert.assertEquals(0.1, lmm.xscale, 0);
        Assert.assertEquals(0.5, lmm.yscale, 0);
        Assert.assertEquals(3.5, lmm.zscale, 0);

    }
    
    private class DummyMaterial extends Material3D            
    {

        @Override
        public void getMaterialSpec(MaterialSpec spec, double x, double y, double z, double xsize, double ysize, double zsize, double t) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isScattering() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean castsShadows() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Material duplicate() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void edit(BFrame fr, Scene sc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void writeToFile(DataOutputStream out, Scene theScene) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.Scene;
import artofillusion.WireframeMesh;
import artofillusion.animation.Keyframe;
import artofillusion.math.BoundingBox;
import artofillusion.texture.LayeredTexture;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class Object3DTest {
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testAttemptToCreateObjectWithBadVersion1() throws IOException
    {
        Scene scene = new Scene();
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)-1); // Object version;
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        
        new DummyObject(new DataInputStream(targetStream), scene);        
    }
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testAttemptToCreateObjectWithBadVersion2() throws IOException
    {
        Scene scene = new Scene();
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)2); // Object version;
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        
        new DummyObject(new DataInputStream(targetStream), scene);        
    }
    
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testCreateObjectNoTexturable() throws IOException {
        Scene scene = new Scene();
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)1); // Object version;
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        DummyObject.canSetTexture = false;
        new DummyObject(new DataInputStream(targetStream), scene);          
    }

    
    @Test(expected = IOException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testCreateObjectTexturableNoInputData() throws IOException {
        Scene scene = new Scene();
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)1); // Object version;
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        DummyObject.canSetTexture = true;
        new DummyObject(new DataInputStream(targetStream), scene);          
    }
    
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testCreateObjectNoMaterialButLayeredTexture() throws IOException
    {
        Scene scene = new Scene();
        byte[] bytes = new byte[200];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)1); // Object version;
        wrap.putInt(-1);  // No material
        wrap.putInt(-1);  // Layered texture
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        DummyObject.canSetTexture = true;
        DummyObject dob = new DummyObject(new DataInputStream(targetStream), scene); 
        Assert.assertNotNull(dob);
        
        Assert.assertNull(dob.getMaterial());
        Assert.assertNotNull(dob.getTexture());
        Assert.assertTrue(dob.getTexture() instanceof LayeredTexture);
    }
    
    private static class DummyObject extends Object3D
    {
        public static boolean canSetTexture = true;
        
        public DummyObject(DataInputStream in, Scene theScene) throws IOException
        {
            super(in, theScene);
        }
        
        @Override
        public Object3D duplicate() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void copyObject(Object3D obj) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public BoundingBox getBounds() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setSize(double xsize, double ysize, double zsize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public WireframeMesh getWireframeMesh() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe getPoseKeyframe() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void applyPoseKeyframe(Keyframe k) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canSetTexture() {
            return canSetTexture;
        }
        
        
                
    }
}

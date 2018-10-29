/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package artofillusion;

import artofillusion.material.UniformMaterial;
import artofillusion.math.RGBColor;
import artofillusion.object.Object3D;
import artofillusion.test.util.StreamUtil;
import artofillusion.texture.Texture;
import artofillusion.texture.TextureMapping;
import artofillusion.texture.TextureSpec;
import artofillusion.texture.UniformMapping;
import artofillusion.texture.UniformTexture;
import buoy.widget.BFrame;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
public class SceneLoadTest {
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testLoadSceneBadVersion1() throws IOException
    {
        
        ByteBuffer wrap = ByteBuffer.allocate(2);
        wrap.putShort((short)-1); // Scene Version
        
        new Scene(StreamUtil.stream(wrap), true);
    }
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testLoadSceneBadVersion2() throws IOException
    {
        
        ByteBuffer wrap = ByteBuffer.allocate(2);
        wrap.putShort((short)5); // Scene Version
        
        new Scene(StreamUtil.stream(wrap), true);
    }
    
    @Test(expected = IOException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testReadSceneWithMissedImage() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)2); // Scene Version 2. No metadata expected to  set
        
        // Ambient color data
        colorToBuffer(new RGBColor(100, 200, 200), wrap);
        // Fog color data
        colorToBuffer(new RGBColor(50, 50, 50), wrap);
        // Fog
        wrap.put((byte)1); 
        // Fog Distance
        wrap.putDouble(1000);
        // show grid
        wrap.put((byte)1); 
        // snap to grid
        wrap.put((byte)1); 
        // grid spacing
        wrap.putDouble(10);
        // grid Subdivisions
        wrap.putInt(10);
        // FPS
        wrap.putInt(60);
        // Image maps count
        wrap.putInt(1);
        {
            String className = "dummy.dummy.MissedImageClass";
            wrap.putShort(Integer.valueOf(className.length()).shortValue());
            wrap.put(className.getBytes());
        }

        new Scene(StreamUtil.stream(wrap), true);
    }
    
    @Test
    public void testReadEmptySceneWithMissedMaterialAndTexture() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)2); // Scene Version 2. No metadata expected to  set
        
        // Ambient color data
        colorToBuffer(new RGBColor(100, 200, 200), wrap);
        // Fog color data
        colorToBuffer(new RGBColor(50, 50, 50), wrap);
        // Fog
        wrap.put((byte)1); 
        // Fog Distance
        wrap.putDouble(1000);
        // show grid
        wrap.put((byte)1); 
        // snap to grid
        wrap.put((byte)1); 
        // grid spacing
        wrap.putDouble(10);
        // grid Subdivisions
        wrap.putInt(10);
        // FPS
        wrap.putInt(60);
        // Image maps count
        wrap.putInt(0);
        // Materials count
        wrap.putInt(1);
        {
            String className = "dummy.dummy.UnknownMaterialClass";

            wrap.putShort(Integer.valueOf(className.length()).shortValue());
            wrap.put(className.getBytes());
            // Material data length
            wrap.putInt(0);
        }
        // Textures count
        wrap.putInt(1);
        {
            String className = "dummy.dummy.UnknownTextureClass";

            wrap.putShort(Integer.valueOf(className.length()).shortValue());
            wrap.put(className.getBytes());
            // Texture data length
            wrap.putInt(0);
        }
        // Objects count
        wrap.putInt(0);
        
        // Environment mode
        wrap.putShort((short)0); // Solid EM
        {
        System.out.println(wrap.position());
            colorToBuffer(new RGBColor(45, 45, 45), wrap);            
        }
        
        Scene scene = new Scene(StreamUtil.stream(wrap), true);
        
        
        Assert.assertEquals(1, scene.getNumTextures());
        Assert.assertTrue(scene.getTexture(0) instanceof UniformTexture);
        Assert.assertTrue(scene.getTexture(0).getName().equals("<unreadable>"));
        Assert.assertEquals(1, scene.getNumMaterials());
        Assert.assertTrue(scene.getMaterial(0) instanceof UniformMaterial);
        Assert.assertTrue(scene.getMaterial(0).getName().equals("<unreadable>"));
        Assert.assertFalse(scene.getLoadingErrors().isEmpty());
    }
    
    @Test
    public void testReadEmptySceneWithMissedMaterialAndBadTexture() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)2); // Scene Version 2. No metadata expected to  set
        
        // Ambient color data
        colorToBuffer(new RGBColor(100, 200, 200), wrap);
        // Fog color data
        colorToBuffer(new RGBColor(50, 50, 50), wrap);
        // Fog
        wrap.put((byte)1); 
        // Fog Distance
        wrap.putDouble(1000);
        // show grid
        wrap.put((byte)1); 
        // snap to grid
        wrap.put((byte)1); 
        // grid spacing
        wrap.putDouble(10);
        // grid Subdivisions
        wrap.putInt(10);
        // FPS
        wrap.putInt(60);
        // Image maps count
        wrap.putInt(0);
        // Materials count
        wrap.putInt(1);
        {
            String className = "dummy.dummy.UnknownMaterialClass";

            wrap.putShort(Integer.valueOf(className.length()).shortValue());
            wrap.put(className.getBytes());
            // Material data length
            wrap.putInt(0);
        }
        // Textures count
        wrap.putInt(1);
        {
            String className = DummyTextureNoConstructor.class.getTypeName();

            wrap.putShort(Integer.valueOf(className.length()).shortValue());
            wrap.put(className.getBytes());
            // Texture data length
            wrap.putInt(0);
        }
        // Objects count
        wrap.putInt(0);
        
        // Environment mode
        wrap.putShort((short)0); // Solid EM
        {
        System.out.println(wrap.position());
            colorToBuffer(new RGBColor(45, 45, 45), wrap);            
        }
        
        Scene scene = new Scene(StreamUtil.stream(wrap), true);
        
        
        Assert.assertEquals(1, scene.getNumTextures());
        Assert.assertTrue(scene.getTexture(0) instanceof UniformTexture);
        Assert.assertTrue(scene.getTexture(0).getName().equals("<unreadable>"));
        Assert.assertEquals(1, scene.getNumMaterials());
        Assert.assertTrue(scene.getMaterial(0) instanceof UniformMaterial);
        Assert.assertTrue(scene.getMaterial(0).getName().equals("<unreadable>"));
        Assert.assertFalse(scene.getLoadingErrors().isEmpty());
    }

    @Test
    public void testReadEmptySceneWithMissedMaterial() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)2); // Scene Version 2. No metadata expected to  set
        
        // Ambient color data
        colorToBuffer(new RGBColor(100, 200, 200), wrap);
        // Fog color data
        colorToBuffer(new RGBColor(50, 50, 50), wrap);
        // Fog
        wrap.put((byte)1); 
        // Fog Distance
        wrap.putDouble(1000);
        // show grid
        wrap.put((byte)1); 
        // snap to grid
        wrap.put((byte)1); 
        // grid spacing
        wrap.putDouble(10);
        // grid Subdivisions
        wrap.putInt(10);
        // FPS
        wrap.putInt(60);
        // Image maps count
        wrap.putInt(0);
        // Materials count
        wrap.putInt(1);
        {
            String className = "dummy.dummy.UnknownMaterialClass";

            wrap.putShort(Integer.valueOf(className.length()).shortValue());
            wrap.put(className.getBytes());
            // Material data length
            wrap.putInt(0);
        }
        // Textures count
        wrap.putInt(1);
        {
            String className = LoadableTexture.class.getTypeName();
            
            wrap.putShort(Integer.valueOf(className.length()).shortValue());
            wrap.put(className.getBytes());
            // Texture data length
            wrap.putInt(0);
        }
        // Objects count
        wrap.putInt(0);
        
        // Environment mode
        wrap.putShort((short)0); // Solid EM
        {
        System.out.println(wrap.position());
            colorToBuffer(new RGBColor(45, 45, 45), wrap);            
        }
        
        Scene scene = new Scene(StreamUtil.stream(wrap), true);
        
        
        Assert.assertEquals(1, scene.getNumTextures());
        Assert.assertTrue(scene.getTexture(0) instanceof LoadableTexture);
        
        Assert.assertEquals(1, scene.getNumMaterials());
        Assert.assertTrue(scene.getMaterial(0) instanceof UniformMaterial);
        Assert.assertTrue(scene.getMaterial(0).getName().equals("<unreadable>"));
        Assert.assertFalse(scene.getLoadingErrors().isEmpty());
    }
    
    //This test fails as no envirinment texture loaded. in general this is impossible situation
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testReadEmptySceneSettingsOnlyNoMeta() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)2); // Scene Version 2. No metadata expected to  set
        
        // Ambient color data
        colorToBuffer(new RGBColor(100, 200, 200), wrap);
        // Fog color data
        colorToBuffer(new RGBColor(50, 50, 50), wrap);
        // Fog
        wrap.put((byte)1); 
        // Fog Distance
        wrap.putDouble(1000);
        // show grid
        wrap.put((byte)1); 
        // snap to grid
        wrap.put((byte)1); 
        // grid spacing
        wrap.putDouble(10);
        // grid Subdivisions
        wrap.putInt(10);
        // FPS
        wrap.putInt(60);
        // Image maps count
        wrap.putInt(0);
        // Materials count
        wrap.putInt(0);
        // Textures count
        wrap.putInt(0);  
        // Objects count
        wrap.putInt(0);
        
        // Environment mode
        wrap.putInt(0); // Solid EM
        {
        
        }
        
        new Scene(StreamUtil.stream(wrap), true);
    }

    private static void colorToBuffer(RGBColor color, ByteBuffer buffer)
    {
        buffer.putFloat(color.getRed());
        buffer.putFloat(color.getGreen());
        buffer.putFloat(color.getBlue());
    }
    
    public static class LoadableTexture extends UniformTexture
    {
        public LoadableTexture(DataInputStream in, Scene theScene)
        {            
        }
    }
    
    public class DummyTextureNoConstructor extends Texture
    {
        public DummyTextureNoConstructor()
        {
            
        }
        
        @Override
        public boolean hasComponent(int component) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getAverageSpec(TextureSpec spec, double time, double[] param) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public TextureMapping getDefaultMapping(Object3D object) {
            return new UniformMapping(object, this);
        }

        @Override
        public Texture duplicate() {
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

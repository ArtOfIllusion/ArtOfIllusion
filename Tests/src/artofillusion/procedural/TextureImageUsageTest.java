/* Copyright (C) 2021 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.Scene;
import artofillusion.image.ImageMap;
import artofillusion.math.RGBColor;
import artofillusion.math.Vec2;
import artofillusion.texture.ProceduralTexture2D;
import artofillusion.texture.ProceduralTexture3D;
import java.awt.Image;
import java.awt.Point;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.Assert;

import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class TextureImageUsageTest {
    
    
    @Test
    public void testTexture2DNotUsesImage() {
        
        ProceduralTexture2D ptex = new ProceduralTexture2D();
        ImageModule imm = new ImageModule(new Point());
        ImageMap untied = new DummyImage();
        ptex.getProcedure().addModule(imm);
        Assert.assertFalse(ptex.usesImage(untied));
        
    }
    

    @Test
    public void testTexture2DNotUsesImage2() {
        
        ProceduralTexture2D ptex = new ProceduralTexture2D();

        ImageMap untied = new DummyImage();
        ptex.getProcedure().addModule(new ParameterModule(new Point()));
        Assert.assertFalse(ptex.usesImage(untied));
        
    }
    
    @Test
    public void testTexture2DUsesImage() {
        
        ProceduralTexture2D ptex = new ProceduralTexture2D();
        ImageModule imm = new ImageModule(new Point());
        ImageMap tied = new DummyImage();
        imm.setMap(tied);
        ptex.getProcedure().addModule(imm);
        Assert.assertTrue(ptex.usesImage(tied));
        
    }

    @Test
    public void testTexture2DUsesImage2() {
        
        ProceduralTexture2D ptex = new ProceduralTexture2D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        ImageModule imm = new ImageModule(new Point());
        ImageMap tied = new DummyImage();
        imm.setMap(tied);
        ptex.getProcedure().addModule(imm);
        Assert.assertTrue(ptex.usesImage(tied));
        
    }
    
    
    @Test
    public void testTexture3DNotUsesImage() {
        
        ProceduralTexture3D ptex = new ProceduralTexture3D();
        ImageModule imm = new ImageModule(new Point());
        ImageMap untied = new DummyImage();
        ptex.getProcedure().addModule(imm);
        Assert.assertFalse(ptex.usesImage(untied));
        
    }
    

    @Test
    public void testTexture3DNotUsesImage2() {
        
        ProceduralTexture3D ptex = new ProceduralTexture3D();

        ImageMap untied = new DummyImage();
        ptex.getProcedure().addModule(new ParameterModule(new Point()));
        Assert.assertFalse(ptex.usesImage(untied));
        
    }
    
    @Test
    public void testTexture3DUsesImage() {
        
        ProceduralTexture3D ptex = new ProceduralTexture3D();
        ImageModule imm = new ImageModule(new Point());
        ImageMap tied = new DummyImage();
        imm.setMap(tied);
        ptex.getProcedure().addModule(imm);
        Assert.assertTrue(ptex.usesImage(tied));
        
    }

    @Test
    public void testTexture3DUsesImage2() {
        
        ProceduralTexture3D ptex = new ProceduralTexture3D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        ImageModule imm = new ImageModule(new Point());
        ImageMap tied = new DummyImage();
        imm.setMap(tied);
        ptex.getProcedure().addModule(imm);
        Assert.assertTrue(ptex.usesImage(tied));
        
    }
    
    
    
    public static class DummyImage extends ImageMap {

        @Override
        public int getWidth() {
           return 100;
        }

        @Override
        public int getHeight() {
            return 100;
        }

        @Override
        public float getAspectRatio() {
            return 1.0f;
        }

        @Override
        public int getComponentCount() {
            return 1;
        }

        @Override
        public float getComponent(int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public float getAverageComponent(int component) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getColor(RGBColor theColor, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getGradient(Vec2 grad, int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Image getPreview() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Image getPreview(int size) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void writeToStream(DataOutputStream out, Scene scene) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

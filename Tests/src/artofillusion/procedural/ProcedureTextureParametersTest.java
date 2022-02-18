/* Copyright (C) 2021 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.TextureParameter;
import artofillusion.animation.ProceduralPositionTrack;
import artofillusion.animation.ProceduralRotationTrack;
import artofillusion.animation.distortion.CustomDistortionTrack;
import artofillusion.math.CoordinateSystem;
import artofillusion.object.Cube;
import artofillusion.object.ObjectInfo;
import artofillusion.texture.ProceduralTexture2D;
import artofillusion.texture.ProceduralTexture3D;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class ProcedureTextureParametersTest {
    
    @Test
    public void getEmptyProcedureTextureParametersForCustomDistortionTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        CustomDistortionTrack track = new CustomDistortionTrack(info);
        
        Field procedureField = CustomDistortionTrack.class.getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Method getParams = CustomDistortionTrack.class.getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
        
    }
    

    @Test
    public void getEmptyProcedureTextureParametersForProceduralTexture2D() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{

        ProceduralTexture2D ptex = new ProceduralTexture2D();
        
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
        
    }
    
    @Test
    public void getProcedureNoParametersForProceduralTexture2D() {
        ProceduralTexture2D ptex = new ProceduralTexture2D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
    }
    
    @Test
    public void getProcedureSingleParameterModuleForProceduralTexture2D() {
        ProceduralTexture2D ptex = new ProceduralTexture2D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        
        ParameterModule module = new ParameterModule(new Point());
        ptex.getProcedure().addModule(module);
   
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(1, parameter.length);
        Assert.assertEquals(0, module.index);
        

    }
    
   
    @Test
    public void getProcedureTwoParameterModulesModuleForProceduralTexture2D() {
        ProceduralTexture2D ptex = new ProceduralTexture2D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        
        ParameterModule one = new ParameterModule(new Point());
        ParameterModule two = new ParameterModule(new Point());
        ptex.getProcedure().addModule(one);
        ptex.getProcedure().addModule(two);
   
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(2, parameter.length);
        Assert.assertEquals(0, one.index);
        Assert.assertEquals(1, two.index);
        

    }
    
    @Test
    public void getProcedureTwoParameterAndOtherModulesModuleForProceduralTexture2D() {
        ProceduralTexture2D ptex = new ProceduralTexture2D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        
        ParameterModule one = new ParameterModule(new Point());
        ParameterModule two = new ParameterModule(new Point());
        ptex.getProcedure().addModule(one);
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        ptex.getProcedure().addModule(two);
   
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(2, parameter.length);
        Assert.assertEquals(0, one.index);
        Assert.assertEquals(1, two.index);
        

    }
    
    @Test
    public void getEmptyProcedureTextureParametersForProceduralTexture3D() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{

        ProceduralTexture3D ptex = new ProceduralTexture3D();
        
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
        
    }
    
    @Test
    public void getProcedureNoParametersForProceduralTexture3D() {
        ProceduralTexture3D ptex = new ProceduralTexture3D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
    }
    
    @Test
    public void getProcedureSingleParameterModuleForProceduralTexture3D() {
        ProceduralTexture3D ptex = new ProceduralTexture3D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        
        ParameterModule module = new ParameterModule(new Point());
        ptex.getProcedure().addModule(module);
   
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(1, parameter.length);
        Assert.assertEquals(0, module.index);
        

    }
    
    @Test
    public void getProcedureTwoParameterModulesModuleForProceduralTexture3D() {
        ProceduralTexture3D ptex = new ProceduralTexture3D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        
        ParameterModule one = new ParameterModule(new Point());
        ParameterModule two = new ParameterModule(new Point());
        ptex.getProcedure().addModule(one);
        ptex.getProcedure().addModule(two);
   
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(2, parameter.length);
        Assert.assertEquals(0, one.index);
        Assert.assertEquals(1, two.index);
        

    }

    @Test
    public void getProcedureTwoParameterAndOtherModulesModuleForProceduralTexture3D() {
        ProceduralTexture3D ptex = new ProceduralTexture3D();
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        
        
        ParameterModule one = new ParameterModule(new Point());
        ParameterModule two = new ParameterModule(new Point());
        ptex.getProcedure().addModule(one);
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        ptex.getProcedure().addModule(new ImageModule(new Point()));
        ptex.getProcedure().addModule(two);
   
        TextureParameter parameter[] = ptex.getParameters();
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(2, parameter.length);
        Assert.assertEquals(0, one.index);
        Assert.assertEquals(1, two.index);
        

    }
    
    @Test
    public void getProcedureNoParameterModulesTextureParametersForCustomDistortionTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        CustomDistortionTrack track = new CustomDistortionTrack(info);
        
        Field procedureField = CustomDistortionTrack.class.getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        procedure.addModule(new ImageModule(new Point()));
        
        
        Method getParams = CustomDistortionTrack.class.getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
        
    }
    
    @Test
    public void getProcedureSingleParameterModuleTextureParametersForCustomDistortionTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        CustomDistortionTrack track = new CustomDistortionTrack(info);
        
        Field procedureField = CustomDistortionTrack.class.getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        ParameterModule module = new ParameterModule(new Point());
        procedure.addModule(module);
        
        
        Method getParams = CustomDistortionTrack.class.getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(1, parameter.length);
        Assert.assertEquals(0, module.index);
        
        
    }
    
    @Test
    public void getProcedureTwoParameterModulesTextureParametersForCustomDistortionTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        CustomDistortionTrack track = new CustomDistortionTrack(info);
        
        Field procedureField = track.getClass().getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        ParameterModule one = new ParameterModule(new Point());
        ParameterModule two = new ParameterModule(new Point());
        
        procedure.addModule(new ImageModule(new Point()));
        procedure.addModule(one);
        procedure.addModule(new ImageModule(new Point()));
        procedure.addModule(two);
        
        
        Method getParams = track.getClass().getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(2, parameter.length);
        Assert.assertEquals(0, one.index);
        Assert.assertEquals(1, two.index);
        
        
    }
    
    
    @Test
    public void getProcedureNoParameterModulesTextureParametersForProceduralPositionTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        ProceduralPositionTrack track = new ProceduralPositionTrack(info);
        
        Field procedureField = ProceduralPositionTrack.class.getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        procedure.addModule(new ImageModule(new Point()));
        
        
        Method getParams = ProceduralPositionTrack.class.getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
        
    }
    
    @Test
    public void getProcedureSingleParameterModuleTextureParametersForProceduralPositionTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        ProceduralPositionTrack track = new ProceduralPositionTrack(info);
        
        Field procedureField = ProceduralPositionTrack.class.getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        ParameterModule module = new ParameterModule(new Point());
        procedure.addModule(module);
        
        
        Method getParams = ProceduralPositionTrack.class.getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(1, parameter.length);
        Assert.assertEquals(0, module.index);
        
        
    }
    
    @Test
    public void getProcedureTwoParameterModulesTextureParametersForProceduralPositionTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        ProceduralPositionTrack track = new ProceduralPositionTrack(info);
        
        Field procedureField = track.getClass().getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        ParameterModule one = new ParameterModule(new Point());
        ParameterModule two = new ParameterModule(new Point());
        
        procedure.addModule(new ImageModule(new Point()));
        procedure.addModule(one);
        procedure.addModule(new ImageModule(new Point()));
        procedure.addModule(two);
        
        
        Method getParams = track.getClass().getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(2, parameter.length);
        Assert.assertEquals(0, one.index);
        Assert.assertEquals(1, two.index);
        
        
    }
    
    @Test
    public void getProcedureNoParameterModulesTextureParametersForProceduralRotationTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        ProceduralRotationTrack track = new ProceduralRotationTrack(info);
        
        Field procedureField = ProceduralRotationTrack.class.getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        procedure.addModule(new ImageModule(new Point()));
        
        
        Method getParams = ProceduralRotationTrack.class.getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(0, parameter.length);
        
    }
    
    @Test
    public void getProcedureSingleParameterModuleTextureParametersForProceduralRotationTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        ProceduralRotationTrack track = new ProceduralRotationTrack(info);
        
        Field procedureField = ProceduralRotationTrack.class.getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        ParameterModule module = new ParameterModule(new Point());
        procedure.addModule(module);
        
        
        Method getParams = ProceduralRotationTrack.class.getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(1, parameter.length);
        Assert.assertEquals(0, module.index);
        
        
    }
    
    @Test
    public void getProcedureTwoParameterModulesTextureParametersForProceduralRotationTrack() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ObjectInfo info = new ObjectInfo(new Cube(1d, 1d, 1d), new CoordinateSystem(), "TestCube");
        ProceduralRotationTrack track = new ProceduralRotationTrack(info);
        
        Field procedureField = track.getClass().getDeclaredField("proc");
        procedureField.setAccessible(true);
        
        Procedure procedure = (Procedure)procedureField.get(track);
        ParameterModule one = new ParameterModule(new Point());
        ParameterModule two = new ParameterModule(new Point());
        
        procedure.addModule(new ImageModule(new Point()));
        procedure.addModule(one);
        procedure.addModule(new ImageModule(new Point()));
        procedure.addModule(two);
        
        
        Method getParams = track.getClass().getDeclaredMethod("getParameters");
        getParams.setAccessible(true);
        TextureParameter parameter[] = (TextureParameter[])getParams.invoke(track);
        
        Assert.assertNotNull(parameter);
        Assert.assertEquals(2, parameter.length);
        Assert.assertEquals(0, one.index);
        Assert.assertEquals(1, two.index);
        
        
    }
    
}

/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.Scene;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author maksim.khramov
 */
public class ProcedureReadStreamTest {

    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testReadProcedureBadVersion() throws IOException
    {        
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Procedure Version 1. Expected exception to be thrown
        
        new Procedure(new OutputModule[0]).readFromStream(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
        
    }


    @Test
    public void testReadEmptyProcedure() throws IOException
    {        
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)0); // Procedure Version 1. Expected exception to be thrown
        wrap.putInt(0); // No Modules
        wrap.putInt(0); // No Links
        new Procedure(new OutputModule[0]).readFromStream(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
        
    }
    
    @Test(expected = IOException.class)
    public void testReadProcedureWithBadModuleName() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)0); // Procedure Version 1. Expected exception to be thrown
        wrap.putInt(1); // One Module But bad Name
        
        String className = "module.module.BadModule";
        wrap.putShort(Integer.valueOf(className.length()).shortValue());
        wrap.put(className.getBytes());
        // Module's Point
        {
            wrap.putInt(123);
            wrap.putInt(456);
        }
        new Procedure(new OutputModule[0]).readFromStream(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
    }
    
    @Test(expected = IOException.class)
    public void testReadProcedureWithBadModuleConstructor() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)0); // Procedure Version 1. Expected exception to be thrown
        wrap.putInt(1); // One Module But bad Name
        
        String className = DummyModuleNoPointConstructor.class.getTypeName();
        
        wrap.putShort(Integer.valueOf(className.length()).shortValue());
        wrap.put(className.getBytes());
        // Module's Point
        {
            wrap.putInt(123);
            wrap.putInt(456);
        }
        Procedure proc = new Procedure(new OutputModule[0]);
        proc.readFromStream(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
    }
    
    @Test
    public void testReadProcedureWithSingleModule() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)0); // Procedure Version 1. Expected exception to be thrown
        wrap.putInt(1); // One Module But bad Name
        
        String className = DummyModule.class.getTypeName();
        
        wrap.putShort(Integer.valueOf(className.length()).shortValue());
        wrap.put(className.getBytes());
        // Module's Point
        {
            wrap.putInt(123);
            wrap.putInt(456);
        }
        Procedure proc = new Procedure(new OutputModule[0]);
        proc.readFromStream(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);
        
        Assert.assertEquals(1, proc.getModules().length);
        Module module = proc.getModules()[0];
        Assert.assertEquals("DummyModule", module.getName());
        
    }
    public static class DummyModuleNoPointConstructor extends Module
    {
        public DummyModuleNoPointConstructor()
        {
            super("NPC", new IOPort[0], new IOPort[0], new Point(0,0));
        }
    }
    
    public static class DummyModule extends Module
    {
        public DummyModule(Point modulePoint)
        {
            super("DummyModule", new IOPort[0], new IOPort[0], modulePoint);
        }

    }
}

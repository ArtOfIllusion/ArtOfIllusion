/* Copyright (C) 2017 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package artofillusion.procedural;

import artofillusion.math.RGBColor;
import java.awt.Point;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author MaksK
 */
public class ProcedureTest
{
  
  public ProcedureTest()
  {
  }
  
  @BeforeClass
  public static void setUpClass()
  {
  }
  
  @Before
  public void setUp()
  {
  }
  
  @After
  public void tearDown()
  {
  }

  @Test
  public void testCreateNewProcedure()
  {
    Procedure procedure = new Procedure(new OutputModule[] {});
    assertNotNull(procedure);
    assertNotNull(procedure.getOutputModules());
    assertEquals(0, procedure.getOutputModules().length);
    assertNotNull(procedure.getModules());
    assertEquals(0, procedure.getModules().length);
    
    assertNotNull(procedure.getLinks());
    assertEquals(0, procedure.getLinks().length);
  }
  
  @Test
  public void testGetNullOutputModuleIndexFromProcedure()
  {
    Procedure procedure = new Procedure(new OutputModule[] {});
    assertEquals(-1, procedure.getOutputIndex(null));
  }
  
  @Test
  public void testGetMissedOutputModuleIndexFromProcedure()
  {
    
    Procedure procedure = new Procedure(new OutputModule[] {});
    
    OutputModule missed = new OutputModule("TestOut", "Label", 0, new RGBColor(1, 1, 1), 0);
    assertEquals(-1, procedure.getOutputIndex(missed));
    
  }
 
  @Test
  public void testGetSingleOutputModuleIndexFromProcedure()
  {
    OutputModule exist = new OutputModule("TestOut", "Label", 0, new RGBColor(1, 1, 1), 0);
    Procedure procedure = new Procedure(new OutputModule[] {exist});
    
    
    assertEquals(0, procedure.getOutputIndex(exist));
    
  }

  @Test
  public void testGetSecondOutputModuleIndexFromProcedure()
  {
    OutputModule first = new OutputModule("TestOut1", "Label0", 0, new RGBColor(1, 1, 1), 0);
    OutputModule exist = new OutputModule("TestOut", "Label", 0, new RGBColor(1, 1, 1), 0);
    Procedure procedure = new Procedure(new OutputModule[] {first, exist});
    
    
    assertEquals(1, procedure.getOutputIndex(exist));
    
  }
  
  @Test
  public void testGetNullModuleFromProcedure()
  {
    Procedure procedure = new Procedure(new OutputModule[] {});
    assertEquals(-1, procedure.getModuleIndex(null));
  }
  
  
  @Test
  public void testGetMissedModuleFromProcedure()
  {
    Module mod = new Module("Test", new IOPort[] {}, new IOPort[] {}, new Point());
    Procedure procedure = new Procedure(new OutputModule[] {});
    assertEquals(-1, procedure.getModuleIndex(mod));
  }
  
  @Test
  public void testGetSingleModuleIndex()
  {
    Module mod = new Module("Test", new IOPort[] {}, new IOPort[] {}, new Point());
    Procedure procedure = new Procedure(new OutputModule[] {});
    procedure.addModule(mod);
    assertEquals(0, procedure.getModuleIndex(mod));    
  }

  @Test
  public void testGetDoubleModuleIndex()
  {
    Module mod1 = new Module("Test1", new IOPort[] {}, new IOPort[] {}, new Point());
    Module mod2 = new Module("Test2", new IOPort[] {}, new IOPort[] {}, new Point());
    Procedure procedure = new Procedure(new OutputModule[] {});
    procedure.addModule(mod1);
    procedure.addModule(mod2);
    assertEquals(0, procedure.getModuleIndex(mod1));
    assertEquals(1, procedure.getModuleIndex(mod2));    
  }
  
  @Test
  public void testDeleteFirstOfTwoModules()
  {
    Module mod1 = new Module("Test1", new IOPort[] {}, new IOPort[] {}, new Point());
    Module mod2 = new Module("Test2", new IOPort[] {}, new IOPort[] {}, new Point());
    Procedure procedure = new Procedure(new OutputModule[] {});
    procedure.addModule(mod1);
    procedure.addModule(mod2);

    procedure.deleteModule(0);
    assertEquals(0, procedure.getModuleIndex(mod2));
  }
  
  @Test
  public void testDeleteLastSingleModule()
  {
    Module mod = new Module("Test", new IOPort[] {}, new IOPort[] {}, new Point());
    Procedure procedure = new Procedure(new OutputModule[] {});
    procedure.addModule(mod);

    procedure.deleteModule(0);
  }

  
  @Test
  public void testDeleteMissedModule()
  {
    Module mod = new Module("Test", new IOPort[] {}, new IOPort[] {}, new Point());
    Procedure procedure = new Procedure(new OutputModule[] {});
    procedure.addModule(mod);

    procedure.deleteModule(10);
  }
  
  @Test
  public void testInitModuleFromPoint()
  {
    
    Module mod = new Module("Test", new IOPort[] {}, new IOPort[] {}, new Point()) {
      private PointInfo point;
      @Override
      public void init(PointInfo p)
      {
        super.init(p);
        bounds.x = (int)p.x;
        bounds.y = (int)p.y;
      }
      
    };
    Procedure procedure = new Procedure(new OutputModule[] {});
    procedure.addModule(mod);
    PointInfo pi = new PointInfo();
    pi.x = 100;
    pi.y = 100;
    procedure.initForPoint(pi);
    
    assertEquals(100, mod.getBounds().getX(), 0.000001);
    assertEquals(100, mod.getBounds().getY(), 0.000001);
    
  }
  
  @Test
  public void testAddLink()
  {
    OutputModule exist = new OutputModule("TestOut", "Label", 0, new RGBColor(1, 1, 1), 0);
    Procedure procedure = new Procedure(new OutputModule[] {exist});
    int linksCount = procedure.getLinks().length;
    IOPort from = new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.BOTTOM, new String[] {"Link From"});
    IOPort to = new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String[] {"Link To"});
    to.setModule(exist);
    
    Link link = new Link(from,to);
    procedure.addLink(link);
    
    assertEquals(++linksCount, procedure.getLinks().length);
  }
  
}

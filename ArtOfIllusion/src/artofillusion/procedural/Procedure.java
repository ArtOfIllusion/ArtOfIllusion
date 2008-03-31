/* Copyright (C) 2000-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.math.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;

/** This represents a procedure for calculating a set of values (typically, the parameters
    for a texture or material). */

public class Procedure
{
  OutputModule output[];
  Module module[];
  Link link[];
  
  public Procedure(OutputModule output[])
  {
    this.output = output;
    module = new Module [0];
    link = new Link [0];
  }
  
  /** Get the list of output modules. */
  
  public OutputModule [] getOutputModules()
  {
    return output;
  }
  
  /** Get the list of all other modules. */
  
  public Module [] getModules()
  {
    return module;
  }
  
  /** Get the index of a particular module. */
  
  public int getModuleIndex(Module mod)
  {
    for (int i = 0; i < module.length; i++)
      if (module[i] == mod)
        return i;
    return -1;
  }
  
  /** Get the index of a particular output module. */
  
  public int getOutputIndex(Module mod)
  {
    for (int i = 0; i < output.length; i++)
      if (output[i] == mod)
        return i;
    return -1;
  }

  /** Add a module to the procedure. */
  
  public void addModule(Module mod)
  {
    Module newmod[] = new Module [module.length+1];
    for (int i = 0; i < module.length; i++)
      newmod[i] = module[i];
    newmod[module.length] = mod;
    module = newmod;
  }
  
  /** Delete a module from the procedure.  Any links involving this module should be deleted
      *before* calling this method. */

  public void deleteModule(int which)
  {
    Module newmod[] = new Module [module.length-1];
    int i, j;
    for (i = 0, j = 0; i < module.length; i++)
      if (i != which)
        newmod[j++] = module[i];
    module = newmod;
  }

  /** Get the list of links between modules. */
  
  public Link [] getLinks()
  {
    return link;
  }
  
  /** Add a link to the procedure. */
  
  public void addLink(Link ln)
  {
    Link newlink[] = new Link [link.length+1];
    for (int i = 0; i < link.length; i++)
      newlink[i] = link[i];
    newlink[link.length] = ln;
    link = newlink;
    ln.to.getModule().setInput(ln.to, ln.from);
  }
  
  /** Delete a link from the procedure. */
  
  public void deleteLink(int which)
  {
    Link newlink[] = new Link [link.length-1];
    int i, j;

    if (link[which].to.getType() == IOPort.INPUT)
      link[which].to.getModule().setInput(link[which].to, null);
    else
      link[which].from.getModule().setInput(link[which].from, null);
    for (i = 0, j = 0; i < link.length; i++)
      if (i != which)
        newlink[j++] = link[i];
    link = newlink;
  }
  
  /** Check for feedback loops in this procedure. */
  
  public boolean checkFeedback()
  {
    for (int i = 0; i < output.length; i++)
      {
        for (int j = 0; j < output.length ; j++)
          output[j].checked = false;
        for (int j = 0; j < module.length ; j++)
          module[j].checked = false;
        if (output[i].checkFeedback())
          return true;
      }
    return false;
  }
  
  /** This routine is called before the procedure is evaluated.  The PointInfo object 
      describes the point for which it is to be evaluated. */
  
  public void initForPoint(PointInfo p)
  {
    for (int i = 0; i < module.length; i++)
      module[i].init(p);
  }
  
  /** This routine returns the value of the specified output module.  If that output does
      not have value type NUMBER, the results are undefined. */
  
  public double getOutputValue(int which)
  {
    return output[which].getAverageValue(0, 0.0);
  }
  
  /** This routine returns the gradient of the specified output module.  If that output does
      not have value type NUMBER, the results are undefined. */
  
  public void getOutputGradient(int which, Vec3 grad)
  {
    output[which].getValueGradient(0, grad, 0.0);
  }
  
  /** This routine returns the color of the specified output module.  If that output does
      not have value type COLOR, the results are undefined. */
  
  public void getOutputColor(int which, RGBColor color)
  {
    output[which].getColor(0, color, 0.0);
  }
  
  /** Make this procedure identical to another one.  The output modules must already
      be set up before calling this method. */
  
  public void copy(Procedure proc)
  {
    module = new Module [proc.module.length];
    for (int i = 0; i < module.length; i++)
      module[i] = proc.module[i].duplicate();
    link = new Link [proc.link.length];
    for (int i = 0; i < link.length; i++)
      {
        Module fromModule = proc.link[i].from.getModule();
        Module toModule = proc.link[i].to.getModule();
        int fromIndex = proc.getModuleIndex(fromModule);
        int toIndex = toModule instanceof OutputModule ? proc.getOutputIndex(toModule) : proc.getModuleIndex(toModule);
        IOPort from = module[fromIndex].getOutputPorts()[proc.module[fromIndex].getOutputIndex(proc.link[i].from)];
        IOPort to = toModule instanceof OutputModule ? 
                output[toIndex].getInputPorts()[proc.output[toIndex].getInputIndex(proc.link[i].to)] :
                module[toIndex].getInputPorts()[proc.module[toIndex].getInputIndex(proc.link[i].to)];
        link[i] = new Link(from, to);
        to.getModule().setInput(to, from);
      }
  }
  
  /** Write this procedure to an output stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(0);
    out.writeInt(module.length);
    for (int i = 0; i < module.length; i++)
      {
        out.writeUTF(module[i].getClass().getName());
        out.writeInt(module[i].getBounds().x);
        out.writeInt(module[i].getBounds().y);
        module[i].writeToStream(out, theScene);
      }
    out.writeInt(link.length);
    for (int i = 0; i < link.length; i++)
      {
        out.writeInt(getModuleIndex(link[i].from.getModule()));
        out.writeInt(link[i].from.getModule().getOutputIndex(link[i].from));
        if (link[i].to.getModule() instanceof OutputModule)
          out.writeInt(-getOutputIndex(link[i].to.getModule())-1);
        else
          {
            out.writeInt(getModuleIndex(link[i].to.getModule()));
            out.writeInt(link[i].to.getModule().getInputIndex(link[i].to));
          }
      }
  }
  
  /** Reconstruct this procedure from an input stream.  The output modules must already
      be set up before calling this method. */

  public void readFromStream(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    short version = in.readShort();
    
    if (version != 0)
      throw new InvalidObjectException("");
    for (int i = 0; i < output.length; i++)
      output[i].setInput(output[i].getInputPorts()[0], null);
    module = new Module [in.readInt()];
    try
      {
        for (int i = 0; i < module.length; i++)
          {
            String classname = in.readUTF();
            Point p = new Point(in.readInt(), in.readInt());
            Class cls = ArtOfIllusion.getClass(classname);
            Constructor con = cls.getConstructor(new Class [] {Point.class});
            module[i] = (Module) con.newInstance(new Object [] {p});
            module[i].readFromStream(in, theScene);
          }
      }
    catch (InvocationTargetException ex)
      {
        ex.getTargetException().printStackTrace();
        throw new IOException();
      }
    catch (Exception ex)
      {
        ex.printStackTrace();
        throw new IOException();
      }
    link = new Link [in.readInt()];
    for (int i = 0; i < link.length; i++)
      {
        IOPort to, from = module[in.readInt()].getOutputPorts()[in.readInt()];
        int j = in.readInt();
        if (j < 0)
          to = output[-j-1].getInputPorts()[0];
        else
          to = module[j].getInputPorts()[in.readInt()];
        link[i] = new Link(from, to);
        to.getModule().setInput(to, from);
      }
  }
}
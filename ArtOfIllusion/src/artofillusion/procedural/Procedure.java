/* Copyright (C) 2000-2004 by Peter Eastman
   Changes copyright (C) 2020 by Maksim Khramov

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
import java.util.ArrayList;
import java.util.List;

/** This represents a procedure for calculating a set of values (typically, the parameters
    for a texture or material). */
public class Procedure
{
  private final List<OutputModule> outputs;
  private final List<Module> modules = new ArrayList<>();
  private final List<Link> links = new ArrayList<>();

  public Procedure(OutputModule... output) {
    this.outputs = java.util.Arrays.asList(output);
  }
  
  /** Get the list of output modules. */
  
  public OutputModule[] getOutputModules()
  {
    return outputs.toArray(new OutputModule[0]);
  }
  
  /** Get the list of all other modules. */
  
  public Module[] getModules()
  {
    return modules.toArray(new Module[0]);
  }
  
  /** Get the index of a particular module. */
  
  public int getModuleIndex(Module mod)
  {
    return modules.indexOf(mod);
  }
  
  /** Get the index of a particular output module. */
  
  public int getOutputIndex(Module mod)
  {
    return outputs.indexOf(mod);
  }

  /** Add a module to the procedure. */
  
  public void addModule(Module mod)
  {
    modules.add(mod);
  }
  
  /** Delete a module from the procedure.  Any links involving this module should be deleted
      *before* calling this method. */

  public void deleteModule(int which)
  {
    modules.remove(which);
  }

  /** Get the list of links between modules. */
  
  public Link[] getLinks()
  {
    return links.toArray(new Link[0]);
  }
  
  /** Add a link to the procedure. */
  
  public void addLink(Link ln)
  {
    ln.to.getModule().setInput(ln.to, ln.from);
    links.add(ln);
  }
  
  /** Delete a link from the procedure. */
  
  public void deleteLink(int which)
  {
    Link target = links.get(which);

    if (target.to.getType() == IOPort.INPUT)
      target.to.getModule().setInput(target.to, null);
    else
      target.from.getModule().setInput(target.from, null);

    links.remove(target);
  }
  
  /** Check for feedback loops in this procedure. */
  
  public boolean checkFeedback()
  {
    for (OutputModule output: outputs)
    {
      outputs.forEach(module -> module.checked = false );
      modules.forEach(module -> module.checked = false );
      if (output.checkFeedback())
        return true;
    }
    return false;
  }
  
  /** This routine is called before the procedure is evaluated.  The PointInfo object 
      describes the point for which it is to be evaluated. */
  
  public void initForPoint(PointInfo p)
  {
    modules.forEach(module -> module.init(p));
  }
  
  /** This routine returns the value of the specified output module.  If that output does
      not have value type NUMBER, the results are undefined. */
  
  public double getOutputValue(int which)
  {
    return outputs.get(which).getAverageValue(0, 0.0);
  }
  
  /** This routine returns the gradient of the specified output module.  If that output does
      not have value type NUMBER, the results are undefined. */
  
  public void getOutputGradient(int which, Vec3 grad)
  {
    outputs.get(which).getValueGradient(0, grad, 0.0);
  }
  
  /** This routine returns the color of the specified output module.  If that output does
      not have value type COLOR, the results are undefined. */
  
  public void getOutputColor(int which, RGBColor color)
  {
    outputs.get(which).getColor(0, color, 0.0);
  }
  
  /** Make this procedure identical to another one.  The output modules must already
      be set up before calling this method. */
  
  public void copy(Procedure proc)
  {
    modules.clear();
    proc.modules.forEach(module -> { modules.add(module.duplicate()); });

    links.clear();
    proc.links.forEach(link -> {
      Module toModule = link.to.getModule();
      int fromIndex = proc.getModuleIndex(link.from.getModule());
      int toIndex = toModule instanceof OutputModule ? proc.getOutputIndex(toModule) : proc.getModuleIndex(toModule);
      IOPort from = modules.get(fromIndex).getOutputPorts()[proc.modules.get(fromIndex).getOutputIndex(link.from)];
      IOPort to = toModule instanceof OutputModule ?
              outputs.get(toIndex).getInputPorts()[proc.outputs.get(toIndex).getInputIndex(link.to)] :
              modules.get(toIndex).getInputPorts()[proc.modules.get(toIndex).getInputIndex(link.to)];
      links.add(new Link(from, to));
      to.getModule().setInput(to, from);
    });

  }
  
  /** Write this procedure to an output stream. */
  
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(0);
    out.writeInt(modules.size());
    for (Module module: modules)
    {
      out.writeUTF(module.getClass().getName());
      out.writeInt(module.getBounds().x);
      out.writeInt(module.getBounds().y);
      module.writeToStream(out, theScene);
    }

    out.writeInt(links.size());
    for (Link link: links)
    {
      out.writeInt(getModuleIndex(link.from.getModule()));
      out.writeInt(link.from.getModule().getOutputIndex(link.from));
      if (link.to.getModule() instanceof OutputModule)
        out.writeInt(-getOutputIndex(link.to.getModule())-1);
      else
        {
          out.writeInt(getModuleIndex(link.to.getModule()));
          out.writeInt(link.to.getModule().getInputIndex(link.to));
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

    outputs.forEach(output -> { output.setInput(output.getInputPorts()[0], null); });

    int counter = in.readInt();
    modules.clear();
    try
    {
      for (int i = 0; i < counter; i++)
      {
        String classname = in.readUTF();
        Point point = new Point(in.readInt(), in.readInt());
        Constructor con = ArtOfIllusion.getClass(classname).getConstructor(Point.class);
        Module module = (Module)con.newInstance(point);
        module.readFromStream(in, theScene);
        modules.add(module);
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

    counter = in.readInt();
    links.clear();
    for (int i = 0; i < counter; i++)
    {
      IOPort to, from = modules.get(in.readInt()).getOutputPorts()[in.readInt()];
      int j = in.readInt();
      if (j < 0)
        to = outputs.get(-j-1).getInputPorts()[0];
      else
        to = modules.get(j).getInputPorts()[in.readInt()];
      links.add(new Link(from, to));
      to.getModule().setInput(to, from);
    }
  }
}
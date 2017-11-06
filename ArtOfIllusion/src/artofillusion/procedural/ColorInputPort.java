/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.procedural;

/**
 *
 * @author MaksK
 */
public class ColorInputPort extends IOPort
{
  public ColorInputPort(String[] name)
  {
    super(IOPort.COLOR, IOPort.INPUT, IOPort.LEFT, name);
  }
}

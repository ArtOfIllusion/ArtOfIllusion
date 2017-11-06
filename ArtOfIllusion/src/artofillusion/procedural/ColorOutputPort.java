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
public class ColorOutputPort extends IOPort
{
  public ColorOutputPort(String[] name)
  {
    super(IOPort.COLOR, IOPort.OUTPUT, IOPort.RIGHT, name);
  }
}

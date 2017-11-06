/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.procedural;

import artofillusion.math.RGBColor;
import java.awt.Point;

/**
 *
 * @author MaksK
 */
public class RGBChannelsModule extends Module
{

  private final RGBColor color;
  
  public RGBChannelsModule(Point position)
  {
    super("RGB Channels", new IOPort [] {new IOPort(IOPort.COLOR, IOPort.INPUT, IOPort.LEFT, new String [] {"Color"})},
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Red", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Green", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Blue", "(0)"})}, position);
      color = new RGBColor(0, 0.5, 1);
  }

  @Override
  public double getAverageValue(int which, double blur)
  {
    RGBColor inputColor = new RGBColor();
    if(linkFrom[0] == null)
    {
       inputColor = color.duplicate();
    } else
    {
      linkFrom[0].getColor(0, inputColor, blur);
      System.out.println(inputColor);
    }
    if(which == 0) return inputColor.red;
    if(which == 1) return inputColor.green;
    return inputColor.blue;
  }
 
  
}

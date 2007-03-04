/* Light is an abstract class which represents a light source in a scene. */

/* Copyright (C) 1999-2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.math.*;
import java.io.*;

public abstract class Light extends Object3D
{
  RGBColor color;
  float intensity, decayRate;
  boolean ambient;

  public Light()
  {
    super();
  }

  public Light(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);
  }
  
  // Set the parameters for this light.

  public void setParameters(RGBColor theColor, float theIntensity, boolean isAmbient, float decay)
  {
    color = theColor;
    intensity = theIntensity;
    ambient = isAmbient;
    decayRate = decay;
  }
  
  public RGBColor getColor()
  {
    return color;
  }

  public float getIntensity()
  {
    return intensity;
  }
  
  // Get the attenuated light at a given distance from the light source.

  public void getLight(RGBColor light, float distance)
  {
    double d = distance*decayRate;
    
    light.copy(color);
    light.scale(intensity/(1.0f+d+d*d));
  }
  
  public boolean isAmbient()
  {
    return ambient;
  }
  
  public float getDecayRate()
  {
    return decayRate;
  }
}
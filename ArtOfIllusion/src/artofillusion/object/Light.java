/* Copyright (C) 1999-2007 by Peter Eastman

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

/** Light is an abstract class which represents a light source in a scene. */

public abstract class Light extends Object3D
{
  protected RGBColor color;
  protected float intensity, decayRate;
  protected int type = TYPE_NORMAL;

  /**
   * This value for the light type represents a normal light, one which only illuminates faces
   * pointed toward it and which casts shadows.
   */
  public static final int TYPE_NORMAL = 0;
  /**
   * This value for the light type represents a light which does not cast shadows.
   */
  public static final int TYPE_SHADOWLESS = 1;
  /**
   * This value for the light type represents a light which adds to the ambient light in the
   * region it affects.  This means that it does not cast shadows, and illuminates all surfaces
   * equally regardless of whether or not they face toward the light.
   */
  public static final int TYPE_AMBIENT = 2;

  public Light()
  {
    super();
  }

  public Light(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);
  }
  
  /**
   * Set the parameters for this light.
   */

  public void setParameters(RGBColor color, float intensity, int type, float decayRate)
  {
    this.color = color;
    this.intensity = intensity;
    this.type = type;
    this.decayRate = decayRate;
  }

  /**
   * Get the color of the light.
   */
  
  public RGBColor getColor()
  {
    return color.duplicate();
  }

  /**
   * Set the color of the light.
   */

  public void setColor(RGBColor color)
  {
    this.color = color.duplicate();
  }

  /**
   * Get the intensity of the light.
   */

  public float getIntensity()
  {
    return intensity;
  }

  /**
   * Set the intensity of the light.
   */

  public void setIntensity(float intensity)
  {
    this.intensity = intensity;
  }

  /**
   * Get the attenuated light at a given position relative to the light source.
   */

  public abstract void getLight(RGBColor light, Vec3 position);
  
  /**
   * Get the decay rate of the light.
   */

  public float getDecayRate()
  {
    return decayRate;
  }

  /**
   * Set the decay rate of the light.
   */

  public void setDecayRate(float rate)
  {
    decayRate = rate;
  }

  /**
   * Get the type of light this object represents.  This is one of the constants TYPE_NORMAL,
   * TYPE_SHADOWLESS, or TYPE_AMBIENT.
   */

  public int getType()
  {
    return type;
  }

  /**
   * Set the type of light this object represents.  This is one of the constants TYPE_NORMAL,
   * TYPE_SHADOWLESS, or TYPE_AMBIENT.
   */

  public void setType(int type)
  {
    this.type = type;
  }
}
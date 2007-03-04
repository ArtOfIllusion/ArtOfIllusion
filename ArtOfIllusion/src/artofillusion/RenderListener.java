/* Copyright (C) 2002,2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.image.ComplexImage;
import java.awt.Image;

/** A RenderListener is an object which asks a Render to generate an image, and is notified when it is completed. */

public interface RenderListener
{
  /** The renderer may call this method periodically during rendering, to notify the listener that more of the
      image is complete. */
  
  public void imageUpdated(Image image);
  
  /** The renderer may call this method periodically during rendering, to give the listener text descriptions
      of the current status of rendering. */
  
  public void statusChanged(String status);
  
  /** This method will be called when rendering is complete. */
  
  public void imageComplete(ComplexImage image);
  
  /** This method will be called if rendering is canceled. */
  
  public void renderingCanceled();
}
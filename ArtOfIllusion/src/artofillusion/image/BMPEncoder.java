/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


/** This class saves images in BMP format.

    @author Michael Butscher
*/


package artofillusion.image;

import artofillusion.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

public class BMPEncoder
{
  public static final int[] HEADER_PART1 = new int [] {
      0x42, 0x4d};

  public static final int[] HEADER_PART2 = new int [] {
      0x00, 0x00, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x28, 0x00,
      0x00, 0x00};

  public static final int[] HEADER_PART3 = new int [] {
      0x01,0x00,0x18,0x00,0x00,0x00,
      0x00,0x00,0x68,0x85,0x15,0x00,0x61,0x0f,0x00,0x00,0x61,0x0f,0x00,0x00,0x00,0x00,
      0x00,0x00,0x00,0x00,0x00,0x00};

  public static final int IMAGE_WIDTH = 0x0012;
  public static final int IMAGE_LENGTH = 0x0016;
  public static final int IMAGE_START = 0x0036;

  private Image im;
  int width, height;
  int lineSize;
  int linePad;
  private int imageDataSize;
  private byte[] description;
  private int data[];

  public BMPEncoder(Image im) throws InterruptedException
  {
    this.im = im;
    recordData();
    width = im.getWidth(null);

    if (width == -1)          // TODO(MB) Remove this hack.
    {
      try {Thread.sleep(600);}
      catch (InterruptedException ex)
      {}
      width = im.getWidth(null);
    }
    
    height = im.getHeight(null);
    
    lineSize = width*3;
    linePad = 4 - lineSize%4;
    if (linePad == 4)
      linePad = 0;

    lineSize += linePad;
    imageDataSize = lineSize*height;
  }

  protected static void writeIA(DataOutputStream out, int[] data)
      throws IOException
  {
    for (int i=0; i < data.length; ++i)
    {
      out.writeByte(data[i]);
    }
  }

  protected static int[] formatLittleEndian(int data)
  {
    int[] result=new int[4];

    for (int i=0; i<4; ++i)
    {
      result[i]=data & 0xff;
      data >>= 8;
    }

    return result;
  }

  /** Saves an image as a BMP file. The DataOutputStream is not closed. */
  public void writeImage(DataOutputStream out) throws IOException
  {
    // Write the image.

    writeHeader(out);
    writeImageData(out);
  }

  /** Grab the image data. */

  private void recordData() throws InterruptedException
  {
    PixelGrabber pg = new PixelGrabber(im, 0, 0, -1, -1, true);

    pg.grabPixels();
    data = (int []) pg.getPixels();
  }

  /** Writes the image file header. */

  private void writeHeader(DataOutputStream out) throws IOException
  {
    writeIA(out, HEADER_PART1);
    writeIA(out, formatLittleEndian(IMAGE_START+imageDataSize-40));
    writeIA(out, HEADER_PART2);
    writeIA(out, formatLittleEndian(width));
    writeIA(out, formatLittleEndian(height));
    writeIA(out, HEADER_PART3);
  }


  /** Writes the actual image data. */

  private void writeImageData(DataOutputStream out) throws IOException
  {
    int start, end;
    int padbytes[] = new int[linePad];

    for (int l = height-1; l >= 0; --l)
    {
      start=l*width;
      end=(l+1)*width;

      for (int i = start; i < end; i++)
      {
	out.writeByte(data[i]&0xFF);      // B
	out.writeByte((data[i]>>8)&0xFF);  // G
	out.writeByte((data[i]>>16)&0xFF);  // R
      }
      writeIA(out, padbytes);
    }
  }

  /** Creates a string describing the software. */

  private void makeDescriptionString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("Art of Illusion v"+ArtOfIllusion.getVersion()+"\n");
    sb.append("");
    description = new String(sb).getBytes();
    description[description.length-1] = 0;
  }
}

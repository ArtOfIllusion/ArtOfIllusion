/* This class saves images in TIFF format (uncompressed 32 bit ARGB).  Much of this
   code is adapted from the TiffEncoder class in the public domain JImage program. */

/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

public class TIFFEncoder
{
  static final int IMAGE_START = 768; 
  static final int HDR_SIZE = 8; 
  static final int MAP_SIZE = 768; // in 16-bit words 
  static final int SCALE_DATA_SIZE = 16; 
   
  public static final int NEW_SUBFILE_TYPE = 254; 
  public static final int IMAGE_WIDTH = 256; 
  public static final int IMAGE_LENGTH = 257; 
  public static final int BITS_PER_SAMPLE = 258; 
  public static final int COMPRESSION = 259; 
  public static final int PHOTO_INTERP = 262; 
  public static final int IMAGE_DESCRIPTION = 270; 
  public static final int STRIP_OFFSETS = 273; 
  public static final int SAMPLES_PER_PIXEL = 277; 
  public static final int ROWS_PER_STRIP = 278; 
  public static final int STRIP_BYTE_COUNT = 279; 
  public static final int X_RESOLUTION = 282; 
  public static final int Y_RESOLUTION = 283; 
  public static final int PLANAR_CONFIGURATION = 284; 
  public static final int RESOLUTION_UNIT = 296; 
  public static final int SOFTWARE = 305; 
  public static final int DATE_TIME = 306; 
  public static final int COLOR_MAP = 320; 
  public static final int EXTRA_SAMPLES = 338; 
  public static final int SAMPLE_FORMAT = 339; 
   
  // Constants 
  
  static final int UNSIGNED = 1; 
  static final int SIGNED = 2; 
  static final int FLOATING_POINT = 3; 
 
  // Field types
  
  static final int BYTE = 1; 
  static final int ASCII = 2; 
  static final int SHORT = 3; 
  static final int LONG = 4; 

  private Image im; 
  int width, height;
  private int bitsPerSample; 
  private int photoInterp; 
  private int samplesPerPixel; 
  private int nEntries; 
  private int ifdSize; 
  private int imageOffset = IMAGE_START; 
  private int imageSize; 
  private byte[] description;
  private int data[];
  private boolean transparent;
  
  public TIFFEncoder(Image im) throws InterruptedException
  {
    this.im = im;
    recordData();
    width = im.getWidth(null);
    height = im.getHeight(null);
    bitsPerSample = 8; 
    samplesPerPixel = transparent ? 4 : 3; 
    nEntries = 10; 
    if (transparent)
      nEntries++;
    int bytesPerPixel = transparent ? 4 : 3; 
    photoInterp = 2; 
    makeDescriptionString(); 
    ifdSize = 2 + nEntries*12 + 4; 
    imageSize = width*height*bytesPerPixel; 
  }
  
  /* Saves an image as a TIFF file. The DataOutputStream is not closed. */
  
  public void writeImage(DataOutputStream out) throws IOException
  { 
    // Write the image.

    writeHeader(out); 
    int nextIFD = 0; 
    writeIFD(out, imageOffset, nextIFD); 
    int bpsSize, scaleSize = 0, descriptionSize; 
    bpsSize = writeBitsPerPixel(out); 
    descriptionSize = writeDescription(out); 
    byte[] filler = new byte[IMAGE_START - (HDR_SIZE+ifdSize+bpsSize+descriptionSize+scaleSize)]; 
    out.write(filler); // force image to start at offset 768
    writeImageData(out); 
  } 
   
  /* Grab the image data, and check whether it is partially transparent. */
  
  private void recordData() throws InterruptedException
  {
    PixelGrabber pg = new PixelGrabber(im, 0, 0, -1, -1, true);
    
    pg.grabPixels();
    data = (int []) pg.getPixels();
    for (int i = 0; i < data.length; i++)
      if ((data[i]&0xFF000000) != 0xFF000000)
        {
          transparent = true;
          return;
        }
  }

  /* Writes the 8-byte image file header. */
  
  private void writeHeader(DataOutputStream out) throws IOException
  { 
    byte[] hdr = new byte[8]; 
    hdr[0] = 77; // "MM" (Motorola byte order)
    hdr[1] = 77; 
    hdr[2] = 0;  // 42 (magic number)
    hdr[3] = 42; 
    hdr[4] = 0;  // 8 (offset to first IFD)
    hdr[5] = 0; 
    hdr[6] = 0; 
    hdr[7] = 8; 
    out.write(hdr); 
  } 
   
  /* Writes one 12-byte IFD entry. */
  
  private void writeEntry(DataOutputStream out, int tag, int fieldType, int count, int value) throws IOException
  { 
    out.writeShort(tag); 
    out.writeShort(fieldType); 
    out.writeInt(count); 
    if (count == 1 && fieldType == BYTE) 
      value <<= 24; //left justify 8-bit values
    if (count == 1 && fieldType == SHORT) 
      value <<= 16; //left justify 16-bit values
    out.writeInt(value); // may be an offset
  } 
   
  /* Writes one IFD (Image File Directory). */
  
  private void writeIFD(DataOutputStream out, int imageOffset, int nextIFD) throws IOException
  {   
    int tagDataOffset = HDR_SIZE + ifdSize; 
    out.writeShort(nEntries); 
    writeEntry(out, NEW_SUBFILE_TYPE, LONG, 1, 0); 
    writeEntry(out, IMAGE_WIDTH,      SHORT, 1, width); 
    writeEntry(out, IMAGE_LENGTH,     SHORT, 1, height); 
    writeEntry(out, BITS_PER_SAMPLE,  SHORT, transparent ? 4 : 3, tagDataOffset); 
    tagDataOffset += transparent ? 8 : 6;
    writeEntry(out, PHOTO_INTERP,     SHORT, 1, photoInterp); 
    writeEntry(out, STRIP_OFFSETS,    LONG, 1, imageOffset); 
    writeEntry(out, SAMPLES_PER_PIXEL,SHORT, 1, samplesPerPixel); 
    writeEntry(out, ROWS_PER_STRIP,   SHORT, 1, height); 
    writeEntry(out, STRIP_BYTE_COUNT, LONG, 1, imageSize); 
    writeEntry(out, SOFTWARE, ASCII, description.length, tagDataOffset); 
    tagDataOffset += description.length; 
    if (transparent)
      writeEntry(out, EXTRA_SAMPLES, SHORT, 1, 1); 
    out.writeInt(nextIFD); 
  } 
   
  /* Writes the 6 bytes of data required by RGB BitsPerSample tag. */
  
  private int writeBitsPerPixel(DataOutputStream out) throws IOException
  { 
    out.writeShort(8);
    out.writeShort(8);
    out.writeShort(8);
    if (transparent)
      out.writeShort(8);
    return transparent ? 8 : 6;
  } 
 
  /* Writes the variable length ImageDescription string. */
  
  private int writeDescription(DataOutputStream out) throws IOException
  { 
    out.write(description, 0, description.length);
    return description.length;
  }
  
  /* Writes the actual image data. */
 
  private void writeImageData(DataOutputStream out) throws IOException
  {
    for (int i = 0; i < data.length; i++)
      {
        out.writeByte((data[i]>>16)&0xFF);
        out.writeByte((data[i]>>8)&0xFF);
        out.writeByte(data[i]&0xFF);
        if (transparent)
          out.writeByte((data[i]>>24)&0xFF);
      }
  }

  /* Creates a string describing the software. */

  private void makeDescriptionString()
  { 
    StringBuffer sb = new StringBuffer(100); 
    sb.append("Art of Illusion v"+ArtOfIllusion.getVersion()+"\n"); 
    sb.append(""); 
    description = new String(sb).getBytes(); 
    description[description.length-1] = 0;  
  } 
}
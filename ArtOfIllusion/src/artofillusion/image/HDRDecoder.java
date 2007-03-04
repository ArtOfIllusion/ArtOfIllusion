/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import java.io.*;
import java.util.*;

/** This class decodes .hdr image files, and creates HDRImage objects from them. */

public class HDRDecoder
{
  /** Create an HDRImage from a file in RADIANCE .hdr format. */
  
  public static HDRImage createImage(File file) throws IOException
  {
    // Make sure this is really an .hdr file.
    
    byte signature[] = "#?RADIANCE".getBytes("ISO-8859-1");
    byte fileSignature[] = new byte [signature.length];
    DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    in.mark(signature.length);
    in.read(fileSignature, 0, signature.length);
    for (int i = 0; i < signature.length; i++)
      if (signature[i] != fileSignature[i])
        {
          in.close();
          throw new IOException("Not an .hdr file");
        }
    in.reset();
    
    // Read the header of the file.
    
    boolean xyze = false;
    String s;
    while ((s = readLine(in)).length() > 0)
      {
        if (s.startsWith("FORMAT"))
          xyze = (s.indexOf("xyze") > -1);
      }
    
    // Parse the resolution string.
    
    String resolution = readLine(in);
    StringTokenizer st = new StringTokenizer(resolution);
    String res[] = new String [4];
    for (int i = 0; i < 4; i++)
      {
        if (!st.hasMoreTokens())
          {
            in.close();
            throw new IOException("Error parsing image file.");
          }
        res[i] = st.nextToken();
      }
    boolean flipY = (res[0].startsWith("+"));
    boolean flipX = (res[2].startsWith("-"));
    int yres, xres;
    try
      {
        yres = Integer.parseInt(res[1]);
        xres = Integer.parseInt(res[3]);
      }
    catch (NumberFormatException ex)
      {
        in.close();
        throw new IOException("Error parsing image file.");
      }
    
    // Read the pixel data.
    
    byte map[][] = new byte [4][xres*yres];
    byte buf[] = new byte [4];
    for (int row = 0; row < yres; row++)
      {
        int start, step;
        if (flipY)
          start = (yres-row-1)*xres;
        else
          start = row*xres;
        if (flipX)
          {
            step = -1;
            start += xres-1;
          }
        else
          step = 1;
        
        // Read the data for the next row.
        
        in.readFully(buf);
        if (buf[0] == 2 && buf[1] == 2 && buf[2] >= 0)
          {
            // Use the "new" RLE compression.
            
            for (int component = 0; component < 4; component++)
              {
                int col = 0, pos = start;
                while (col < xres)
                  {
                    int count = in.read();
                    if (count <= 128)
                      for (int i = 0; i < count; i++)
                        {
                          map[component][pos] = (byte) in.read();
                          pos += step;
                        }
                    else
                      {
                        byte repeat = (byte) in.read();
                        count -= 128;
                        for (int i = 0; i < count; i++)
                          {
                            map[component][pos] = repeat;
                            pos += step;
                          }
                      }
                    col += count;
                  }
              }
          }
        else
          {
            // Use the "old" RLE compression.
            
            int pos = start;
            for (int col = 0; col < xres; col++)
              {
                if (col > 0)
                  in.readFully(buf);
                if (buf[0] == 1 && buf[1] == 1 && buf[2] == 1)
                  {
                    int count = ((int) buf[3])&0xFF;
                    byte r = map[0][pos-step];
                    byte g = map[1][pos-step];
                    byte b = map[2][pos-step];
                    byte e = map[3][pos-step];
                    for (int i = 0; i < count; i++)
                      {
                        map[0][pos] = r;
                        map[1][pos] = g;
                        map[2][pos] = b;
                        map[3][pos] = e;
                        pos += step;
                      }
                    col += count-1;
                  }
                else
                  {
                    map[0][pos] = buf[0];
                    map[1][pos] = buf[1];
                    map[2][pos] = buf[2];
                    map[3][pos] = buf[3];
                    pos += step;
                  }
              }
          }
      }
    in.close();
    
    // If the data is stored as XYZE components, convert it to RGBE.
    
    if (xyze)
      {
        for (int i = 0; i < map[0].length; i++)
          {
            int x = map[0][i]&0xFF;
            int y = map[1][i]&0xFF;
            int z = map[2][i]&0xFF;
            map[0][i] = (byte) (3.24*x - 1.54*y - 0.50*z);
            map[1][i] = (byte) (-0.97*x + 1.88*y + 0.42*z);
            map[2][i] = (byte) (0.06*x - 0.20*y + 1.06*z);
          }
      }
    return new HDRImage(map[0], map[1], map[2], map[3], xres, yres);
  }
  
  /** Read a line of ASCII text from the input stream. */
  
  private static String readLine(InputStream in) throws IOException
  {
    StringBuffer buf = new StringBuffer();
    int c;
    while ((c = in.read()) > -1 && c != '\n')
      {
        buf.append((char) c);
      }
    return buf.toString();
  }
}
/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.Scene;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class CubeTest {
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void createCubeTestBadVersionLess() throws IOException {
        Scene scene = new Scene();
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)-1);
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        
        new Cube(new DataInputStream(targetStream), scene);
    }
    
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void createCubeTestBadVersionMore() throws IOException {
        Scene scene = new Scene();
        byte[] bytes = new byte[2];
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.putShort((short)2);
        
        InputStream targetStream = new ByteArrayInputStream(bytes);
        
        new Cube(new DataInputStream(targetStream), scene);
    }
    
    
}

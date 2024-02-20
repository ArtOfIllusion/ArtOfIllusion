/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.Scene;
import artofillusion.test.util.StreamUtil;
import java.io.IOException;
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
        ByteBuffer wrap = ByteBuffer.allocate(2);
        wrap.putShort((short)-1);

        
        new Cube(StreamUtil.stream(wrap), scene);
    }
    
    
    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void createCubeTestBadVersionMore() throws IOException {
        Scene scene = new Scene();
        ByteBuffer wrap = ByteBuffer.allocate(2);
        wrap.putShort((short)2);
        
        new Cube(StreamUtil.stream(wrap), scene);
    }
    
    
}

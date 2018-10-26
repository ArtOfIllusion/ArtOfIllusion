/*
 * Copyright 2018 Veeam Software.
 * 
 * Created by Maksim Khramov
 * Date: Oct 26, 2018.
 */
package artofillusion.test.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

/**
 *
 * @author maksim.khramov
 */
public class StreamUtil {
    public static DataInputStream stream(ByteBuffer wrap) {
        return new DataInputStream(new ByteArrayInputStream(wrap.array()));
    }
}

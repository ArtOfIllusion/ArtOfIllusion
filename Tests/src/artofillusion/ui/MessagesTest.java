/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import javax.swing.JOptionPane;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class MessagesTest {

    
    @Test
    public void testError() {
        Messages.error("Hello Errors");
    }
    
    @Test
    public void testWarning() {
        Messages.warning("This is warning");
    }
    
    @Test
    public void testInformation() {
      Messages.information("Information message");
    }
    
    @Test
    public void testPlain() {
      JOptionPane.showMessageDialog(null, "Plain message", "", JOptionPane.PLAIN_MESSAGE);
    }
}

/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author maksim.khramov
 */
public class TitleWindowNB extends JFrame implements PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(TitleWindowNB.class.getName());
    
    private static final long serialVersionUID = 1L;
    
    
    
    
    public TitleWindowNB() {
        super("Art Of Illusion");
    }

    @Override
    protected void frameInit() {
        
        super.frameInit();
        
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setUndecorated(true);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                super.windowActivated(e);
                logger.log(Level.INFO, "Window activated");
                
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                super.windowDeactivated(e);
                logger.log(Level.INFO, "Window deactivated");
                
            }
            
            
        });
        int num = new Random(System.currentTimeMillis()).nextInt(8);
        ImageIcon image = new ImageIcon(getClass().getResource("/artofillusion/titleImages/titleImage" + num + ".jpg"));
        String text = "<html><div align=\"center\">"
                + "Art of Illusion v" + ArtOfIllusion.getVersion()
                + "<br>Copyright 1999-2015 by Peter Eastman and others"
                + "<br>(See the README file for details.)"
                + "<br>This program may be freely distributed under"
                + "<br>the terms of the accompanying license.</div></html>";
        
        Color background = num == 4 ? new Color(204, 204, 255) : (num == 6 ? new Color(232, 255, 232) : Color.WHITE);
        
        JLabel label = new JLabel(text,image,JLabel.CENTER);
        
        label.setBorder(new EmptyBorder(0,0,5,0));
        label.setVerticalTextPosition(JLabel.BOTTOM);
        label.setHorizontalTextPosition(JLabel.CENTER);
        
        JPanel labelContainerPanel = new JPanel();
        labelContainerPanel.setLayout(new BorderLayout(0, 0));        
        labelContainerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        labelContainerPanel.add(label);
        labelContainerPanel.setBackground(background);
      
        JPanel root = new JPanel();
        root.setLayout(new BorderLayout(0, 0));
        root.setBackground(background);
        
        root.setBorder(BorderFactory.createLineBorder(Color.BLACK,1));
        root.add(labelContainerPanel);

        this.getContentPane().add(root);
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow", this);
        this.pack();
        this.setVisible(true);
        this.setLocationRelativeTo(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("activeWindow", this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        //logger.log(Level.INFO, "Property change event fired: " + event);
    }
    
    
    
    
    
}

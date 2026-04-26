/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion;

import artofillusion.math.CoordinateSystem;
import artofillusion.object.ObjectInfo;
import artofillusion.object.Sphere;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Locale;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.netbeans.jemmy.Bundle;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JMenuBarOperator;

/**
 *
 * @author MaksK
 */
public class SelectAllAndUndoTest
{
  private static final Bundle bundle = new Bundle();
  private JMenuBarOperator appMainMenu;
  private JFrameOperator appFrame;
  
  private LayoutWindow layout;
  
  
  public SelectAllAndUndoTest()
  {
  }
  
  @BeforeClass
  public static void setupClass() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, URISyntaxException, IOException
  {

    Locale.setDefault(Locale.ENGLISH);
    new ClassReference("artofillusion.ArtOfIllusion").startApplication();
    bundle.load(ArtOfIllusion.class.getClassLoader().getResourceAsStream("artofillusion.properties"));
    JemmyProperties.setCurrentOutput(TestOut.getNullOutput());
  }

  
  @Before
  public void setUp()
  {
    appFrame = new JFrameOperator("Untitled");
    appMainMenu = new JMenuBarOperator(appFrame);
    appMainMenu.closeSubmenus();
    
    
    layout = (LayoutWindow) ArtOfIllusion.getWindows()[0];
    layout.updateImage();
    layout.updateMenus();


  }
  
  @After
  public void done()
  {
    int scc = layout.getScene().getNumObjects();
    for (int i = 2; i < scc; i++)
    {
      layout.removeObject(2, null);
    }
    layout.updateImage();
    layout.updateMenus();
    
    try {
      Thread.sleep(1000);
    } catch(InterruptedException ie) {
      
    }
  }

  @Test
  public void selectAllAndUndo() 
  {
    for(int i = 0; i<10; i++) {
      ObjectInfo test = new ObjectInfo(new Sphere(1d, 1d, 1d), new CoordinateSystem(), "Test-" + i);
      layout.addObject(test, null);
    }
        
    layout.updateImage();
    layout.updateMenus();
    
    assertFalse(layout.isModified());
    
    appMainMenu.pushMenu("Edit|Select All");

    
    assertFalse(layout.isModified());
    
    appMainMenu.pushMenu("Edit|Undo");

    
    assertFalse(layout.isModified());    
    
  }
  
  
}

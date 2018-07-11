/*
 * Copyright 2018 Veeam Software.
 * 
 * Created by Maksim Khramov
 * Date: Jul 5, 2018.
 */
package artofillusion.ui;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class TreeListTest {
    
    @Test
    public void testGetNoneElements() {
        TreeList tl = new TreeList(null);

        TreeElement[] te = tl.getElements();
        Assert.assertNotNull(te);
        Assert.assertEquals(0, te.length); 
    }
    
    @Test
    public void testGetSingleElement() {
        TreeList tl = new TreeList(null);
        GenericTreeElement item = new GenericTreeElement("LAbel", null, null, tl, null);
        tl.addElement(item);
        TreeElement[] te = tl.getElements();
        Assert.assertNotNull(te);
        Assert.assertEquals(1, te.length);
    }
    
    @Test
    public void testGetMoreElements() {
        TreeList tl = new TreeList(null);
        GenericTreeElement item = new GenericTreeElement("Label1", null, null, tl, null);
        tl.addElement(item);
        tl.addElement(new GenericTreeElement("Label2", null, null, tl, null));
        
        TreeElement[] te = tl.getElements();
        Assert.assertNotNull(te);
        Assert.assertEquals(2, te.length);        
    }
    
    @Test
    public void testGetElementsRecursively() {
        TreeList tl = new TreeList(null);
        List<TreeElement> child = new ArrayList<>();
        child.add(new GenericTreeElement("Level2Label1", null, null, tl, null));
        child.add(new GenericTreeElement("Level2Label2", null, null, tl, null));
        GenericTreeElement item = new GenericTreeElement("Label1", null, null, tl, child);
        
        tl.addElement(item);
        tl.addElement(new GenericTreeElement("Label2", null, null, tl, null));
        
        TreeElement[] te = tl.getElements();
        Assert.assertNotNull(te);
        Assert.assertEquals(4, te.length);     
    }
    
    @Test
    public void testFindSingle() {
        String target = "Target";
        TreeList tl = new TreeList(null);
        tl.addElement(new GenericTreeElement("Label2", target, null, tl, null));
        Assert.assertNotNull(tl.findElement(target));
        Assert.assertEquals(target, tl.findElement(target).getObject());
    }
    
    
    @Test
    public void testFindMissingElementInTree() {
        TreeList tl = new TreeList(null);
        List<TreeElement> child = new ArrayList<>();
        child.add(new GenericTreeElement("Level2Label1", "1", null, tl, null));
        child.add(new GenericTreeElement("Level2Label2", "2", null, tl, null));
        GenericTreeElement item = new GenericTreeElement("Label1", "3", null, tl, child);
        
        tl.addElement(item);
        tl.addElement(new GenericTreeElement("Label2", "4", null, tl, null));
        
        Assert.assertNull(tl.findElement("Test"));
    }
    
    @Test
    public void testFindElementInTree() {
        String target = "Target";
        TreeList tl = new TreeList(null);
        List<TreeElement> child = new ArrayList<>();
        child.add(new GenericTreeElement("Level2Label1", "1", null, tl, null));
        child.add(new GenericTreeElement("Level2Label2", "2", null, tl, null));
        GenericTreeElement item = new GenericTreeElement("Label1", "3", null, tl, child);
        
        tl.addElement(item);
        tl.addElement(new GenericTreeElement("Label2", target, null, tl, null));
        
        Assert.assertNotNull(tl.findElement(target));
        Assert.assertEquals(target, tl.findElement(target).getObject());
    }    
}

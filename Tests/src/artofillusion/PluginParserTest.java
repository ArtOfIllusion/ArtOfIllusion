/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion;

import java.io.IOException;
import java.lang.reflect.Field;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class PluginParserTest
{
  private static Unmarshaller umt = null;
  
  @BeforeClass
  public static void setUpClass() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException
  {
      Field umf = PluginRegistry.class.getDeclaredField("um");
      umf.setAccessible(true);
      umt = (Unmarshaller)umf.get(null);

  }
  
  @Test(expected = javax.xml.bind.UnmarshalException.class)
  public void testBadFileContent() throws IOException, JAXBException {
    umt.unmarshal(PluginParserTest.class.getResource("bad.xml").openStream());
  }

  @Test
  public void testGoodExtension() throws IOException, JAXBException {
    PluginRegistry.Extension ext = (PluginRegistry.Extension)umt.unmarshal(PluginParserTest.class.getResource("good.xml").openStream());
    Assert.assertNull("Test", ext.name);
    Assert.assertNull("1.0", ext.version);
    Assert.assertTrue(ext.imports.isEmpty());
    Assert.assertTrue(ext.resources.isEmpty());
  }
  
  @Test
  public void testGoodExtensionNamed() throws IOException, JAXBException {
    PluginRegistry.Extension ext = (PluginRegistry.Extension)umt.unmarshal(PluginParserTest.class.getResource("goodname.xml").openStream());
    Assert.assertEquals("Test", ext.name);
    Assert.assertEquals("1.0", ext.version);
    Assert.assertTrue(ext.imports.isEmpty());
    Assert.assertTrue(ext.resources.isEmpty());
  }
  
  
  @Test
  public void testExtensionResource() throws IOException, JAXBException 
  {
    PluginRegistry.Extension ext = (PluginRegistry.Extension)umt.unmarshal(PluginParserTest.class.getResource("locale.xml").openStream());
    Assert.assertEquals("Test", ext.name);
    Assert.assertEquals("1.0", ext.version);
    
    Assert.assertTrue(ext.imports.isEmpty());
    Assert.assertFalse(ext.resources.isEmpty());
    
    
    for(Object resource: ext.resources)
      System.out.println("Resource:" + resource);
  }
  
  @Test
  public void testListImports() throws IOException, JAXBException {
    PluginRegistry.Extension ext = (PluginRegistry.Extension)umt.unmarshal(PluginParserTest.class.getResource("imports.xml").openStream());
    Assert.assertFalse(ext.imports.isEmpty());

  }
  
  @Test
  public void testListCategories() throws IOException, JAXBException {
    PluginRegistry.Extension ext = (PluginRegistry.Extension)umt.unmarshal(PluginParserTest.class.getResource("category.xml").openStream());
    Assert.assertFalse(ext.categories.isEmpty());
    
  }
  
}

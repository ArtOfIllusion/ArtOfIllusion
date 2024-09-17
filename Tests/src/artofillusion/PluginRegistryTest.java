/*
 *  Copyright 2022 by Maksim Khramov
 *  This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 2 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package artofillusion;

import artofillusion.PluginRegistry.PluginResource;
import artofillusion.test.mocks.DummyPlugin;
import artofillusion.test.mocks.DummyPluginAndTool;
import artofillusion.test.mocks.MethodHolderPlugin;
import artofillusion.test.mocks.MockPluginCategory;
import artofillusion.test.mocks.RegisteredCategory;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.junit.Assert;

import org.junit.Test;

/**
 *
 * @author MaksK
 */
public class PluginRegistryTest {


    @org.junit.BeforeClass
    public static void setupClass() {
        PluginRegistry.addCategory(Plugin.class);
        PluginRegistry.addCategory(ModellingTool.class);
    }

    @Test
    public void getCategories() {
      List<Class> categories =  PluginRegistry.getCategories();
      Assert.assertNotNull(categories);
      Assert.assertEquals(2, categories.size());
    }

    @Test
    public void testAddToSingleCategory() {

        PluginRegistry.registerPlugin(new DummyPlugin());

        List<Plugin> plugins = PluginRegistry.getPlugins(Plugin.class);
        Assert.assertNotNull(plugins);
        Assert.assertEquals(1, plugins.size());

        plugins.forEach(plugin -> System.out.println(plugin.getClass()) );
    }

    @Test
    public void testAddToDoubleCategory() {

        PluginRegistry.registerPlugin(new DummyPlugin());
        PluginRegistry.registerPlugin(new DummyPluginAndTool());

        List<Plugin> plugins = PluginRegistry.getPlugins(Plugin.class);
        plugins.forEach(plugin -> System.out.println(plugin.getClass()) );

        List<ModellingTool> tools = PluginRegistry.getPlugins(ModellingTool.class);
        tools.forEach(tool -> System.out.println(tool.getClass()) );
    }

    @Test
    public void testGetPluginsFromEmptyUnregisteredCategory() {
        List<MockPluginCategory> plugins = PluginRegistry.getPlugins(MockPluginCategory.class);
        Assert.assertNotNull(plugins);
        Assert.assertEquals(0, plugins.size());
    }

    @Test
    public void testGetPluginsFromEmptyKnownCategory() {
        PluginRegistry.addCategory(RegisteredCategory.class);
        List<RegisteredCategory> plugins = PluginRegistry.getPlugins(RegisteredCategory.class);
        Assert.assertNotNull(plugins);
        Assert.assertEquals(0, plugins.size());
    }

    @Test
    public void testGetEmptyPluginsListAndSort() {
        List<String> strings = PluginRegistry.getPlugins(String.class);
        Collections.sort(strings, Comparator.comparing(String::length));
    }

    @Test
    public void testGetResourceForUnknownType() {
        List<PluginResource> empty = PluginRegistry.getResources("mesh.Template");
        Assert.assertNotNull(empty);
        Assert.assertEquals(0, empty.size());
    }

    @Test
    public void testGetNullForUnknownResource() {
        PluginResource resource = PluginRegistry.getResource("Unknownn", "MissedId");
        Assert.assertNull(resource);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterExportMethod() {
        MethodHolderPlugin hp = new MethodHolderPlugin();
        PluginRegistry.registerExportedMethod(hp, "someMethodOne", "pluginMethod");
        PluginRegistry.registerExportedMethod(hp, "someMethodOne", "pluginMethod");
    }

    @Test
    public void testRegisterExportMethodAndCall() throws NoSuchMethodException, InvocationTargetException {
        MethodHolderPlugin hp = new MethodHolderPlugin();
        PluginRegistry.registerExportedMethod(hp, "someMethodTwo", "methodTwo");

        PluginRegistry.invokeExportedMethod("methodTwo");
    }

    @Test(expected = NoSuchMethodException.class)
    public void testInvokeMissingMethodId() throws NoSuchMethodException, InvocationTargetException {
        PluginRegistry.invokeExportedMethod("missedId");
    }

    @Test(expected = NoSuchMethodException.class)
    public void testInvokeWrongRegisteredMethod() throws NoSuchMethodException, InvocationTargetException {
        MethodHolderPlugin hp = new MethodHolderPlugin();
        PluginRegistry.registerExportedMethod(hp, "badMethod", "goodId");

        PluginRegistry.invokeExportedMethod("goodId");
    }

    @Test(expected = NoSuchMethodException.class)
    public void testInvokeMethodWithWrongParameter() throws NoSuchMethodException, InvocationTargetException {
        MethodHolderPlugin hp = new MethodHolderPlugin();
        PluginRegistry.registerExportedMethod(hp, "someMethowWithIntegerParameter", "someMethowWithIntegerParameter");
        PluginRegistry.invokeExportedMethod("someMethowWithIntegerParameter", "Hello Plugin");
    }

    @Test
    public void getAllExportMethods() {
      List<String> methodIds = PluginRegistry.getExportedMethodIds();
      Assert.assertNotNull(methodIds);
    }

    @Test
    public void registerResource() {
      PluginRegistry.registerResource("StringResource", "sid", null, "name", Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerResourceDoubleLocale() {
      PluginRegistry.registerResource("StringResourceDoubled", "sid", null, "name", Locale.ENGLISH);
      PluginRegistry.registerResource("StringResourceDoubled", "sid", null, "name", Locale.ENGLISH);
    }
}

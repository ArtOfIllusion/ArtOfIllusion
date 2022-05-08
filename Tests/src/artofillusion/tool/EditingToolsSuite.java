/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4Suite.java to edit this template
 */
package artofillusion.tool;

import artofillusion.tool.help.TestToolsHelp;
import artofillusion.tool.hint.TestToolsHints;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author MaksK
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({TestToolWichClicks.class, TestToolsHelp.class, TestToolsHints.class})
public class EditingToolsSuite {}

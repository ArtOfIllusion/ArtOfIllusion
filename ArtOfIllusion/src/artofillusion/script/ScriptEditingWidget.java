/* Copyright (C) 2023 by Lucas Stanek
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package artofillusion.script;

import java.awt.Color;
import javax.swing.text.JTextComponent;
import buoy.widget.Widget;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Shared code for setting up an editing widget for aoi scripts.
 *
 * This is a widget that can display and edit script text, in either
 * Groovy or Beanshell. It exists primarily to provide a unified
 * styling across the various script editors. It has a fixed preferred
 * size. Saving, loading, and evaluation of the scripts should be
 * handled by the calling code.
 */

public class ScriptEditingWidget extends Widget
{
  private RSyntaxTextArea textArea;

  public ScriptEditingWidget(String script)
  {
    textArea = new RSyntaxTextArea(script, 25, 100);
    try{
      Theme theme = Theme.load(ScriptEditingWidget.class
          .getResourceAsStream("/scriptEditorTheme.xml"));
      theme.apply(textArea);
    } catch (Exception e)
    {
      //shouldn't happen unless we are pointing at a non-existant file
      e.printStackTrace();
    }
    textArea.setAnimateBracketMatching(false);
    textArea.setTabSize(2);

    component = new RTextScrollPane(textArea);
  }

  //TODO: migrate to constant, rather than string
  public void setLanguage(String lang)
  {
    textArea.setSyntaxEditingStyle(lang.equalsIgnoreCase("groovy")
                                   ? SyntaxConstants.SYNTAX_STYLE_GROOVY
                                   : SyntaxConstants.SYNTAX_STYLE_JAVA);
  }

  public String getScriptText()
  {
    return textArea.getText();
  }

  public void setScriptText(String text)
  {
    textArea.setText(text);
  }

  public void requestFocus()
  {
    textArea.requestFocus();
  }

  /**
   * Pass through the underlying implementing text component.
   *
   * This is primarily useful for advanced manipulations of the script
   * text, such as substring analysis. User classes don't need to know
   * exactly what the implementing type is.
   */

  public JTextComponent textComponent()
  {
    return textArea;
  }


  /**
   * Set whether this widget should be editable.?
   *
   * Defaults to "true".
   * If "false", the widget acts as a read-only highlighted view of the
   * script.
   *
   * @param editable Whether the loaded script should be editable. 
   */

  public void setEditable(boolean editable)
  {
    textArea.setEditable(editable);
    textArea.setEnabled(editable);
    textArea.setBackground(editable? Color.WHITE : Color.LIGHT_GRAY);
  }
}

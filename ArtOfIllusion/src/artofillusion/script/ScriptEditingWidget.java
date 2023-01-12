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

import org.fife.ui.rsyntaxtextarea.*;

/**
 * Shared code for setting up an editing widget for aoi scripts.
 */

public class ScriptEditingWidget
{
  public static RSyntaxTextArea getScriptWidget(String script)
  {
    RSyntaxTextArea widget = new RSyntaxTextArea(script, 25, 100);
    try{
      Theme theme = Theme.load(ScriptEditingWidget.class
                               .getResourceAsStream("/scriptEditorTheme.xml"));
      theme.apply(widget);
    } catch (Exception e) //shouldn't happen unless we are pointing at a non-existant file
    {
      e.printStackTrace();
    }
    widget.setAnimateBracketMatching(false);
    widget.setTabSize(2);
    return widget;
  }
}

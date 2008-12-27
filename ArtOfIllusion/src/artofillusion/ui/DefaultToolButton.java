/* Copyright (C) 2007 by François Guillet

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.util.IconGenerator;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

import java.util.HashMap;

/**
 * This ToolButton is the classic button with one icon for standard representation
 * and another one for selected state.
 * 
 * @author Francois Guillet
 *
 */
public class DefaultToolButton extends ToolButton
{
    protected Image icon;
    protected Image selectedIcon;

    /*
    public DefaultToolButton(Object owner, String iconFileName, String selectedIconFilename) {
        this(owner, ThemeManager.getIcon(iconFileName), ThemeManager.getIcon(selectedIconFilename));
    }
    */

    /**
     * create a DefaultToolButton for the specified owner, and the icon
     * specified by the <i>image</i>, by applying the correct style to the
     * image to generate theme-consistent <i>normal</i> and <i>selected</i>
     * icons.
     *
     * This ctor is the preferred ctor, as it ensures the greatest consistency
     * of all icons with the theme.
     *
     *  @param owner the owning object for this button
     *
     *  @param image the image to use with the style to generate the normal and
     *                selected icons for this button.
     */
    public DefaultToolButton(Object owner, ImageIcon image)
    {
        super(owner);

        ThemeManager.ButtonStyle bstyle = ThemeManager.getButtonStyle(owner);
        if (bstyle != null) {
            try {
                icon = applyStyle(bstyle, "normal", owner, image);
                selectedIcon = applyStyle(bstyle, "selected", owner, image);
            } catch (Exception e) {
                System.out.println("Applying style: " + e);
            }
        }

        // if the style failed to apply, just use the icon as it is
        if (icon == null || icon.getWidth(null) <= 0)
            icon = image.getImage();

        // last resort: set icon to notFoundIcon
        if (icon == null || icon.getWidth(null) <= 0)
            icon = ThemeManager.getNotFoundIcon(owner).getImage();

        // selectedIcon defaults to icon with a red "wash"
        if (selectedIcon == null)  {
            selectedIcon = IconGenerator.copy(icon);
            IconGenerator.multiply((BufferedImage) selectedIcon, 1.0f, 1.0f, 0.5f, 0.5f);
        }

        width = icon.getWidth(null);
        height = icon.getHeight(null);
    }

    /**
     *  create a new DefaultToolButton using the specified icons for
     *  <i>normal</i> and <i>selected</i> icons.
     *
     *<b><em>No</em> style is applied - the icons are used as-is.
     *
     *  This is the least-preferred ctor, since it applies no consistency
     *  at all, and relies on the icon creator to have made the icons
     *  consistent. This normally works well for icons that are part of the
     *  theme, but usually works very poorly for icons associated with plugins.
     *
     *  @param icon the icon to use for normal button display
     *  @param selectedIcon the icon to use for selected display.
     */
    public DefaultToolButton(Object owner, ImageIcon icon, ImageIcon selectedIcon) {
        super(owner);
        this.icon = icon.getImage();
        this.selectedIcon = selectedIcon.getImage();
        height = icon.getIconHeight();
        width = icon.getIconWidth();
    }

    public void paint(Graphics2D g) {
        switch(state) {
        case NORMAL_STATE:
        case HIGHLIGHTED_STATE:
            g.drawImage(icon, position.x, position.y, null);
            break;
        case SELECTED_STATE:
            g.drawImage(selectedIcon, position.x, position.y, null);
            break;
        }
    }


  /**
   *  apply a style for the specified type and owner, to the specified
   *  icon url.
   *
   *  If the style defined no <code>type.icon</code> attribute, then
   *  a definition is generated using the <code>type.background</i> and
   *  <code>type.overlay</code> attributes. If no style definition can
   *  be foudn or generated, the icon from <i>image</i> is returned.
   *
   *  @param style the ButtonStyle to apply
   *  @param type the type of icon to create. Current values are
   *        <em>normal</em> and <em>selected</em>.
   *        In theory <em>highlighted</em> can also be specified, but
   *        currently no code recognises this type.
   *
   *  @param owner the owner of this style.
   *
   *  @param image the image to use with the style to generate the final
   *                icon.
   *
   *  @return the generated icon.
   */
  public Image applyStyle(ThemeManager.ButtonStyle style, String type, Object owner, ImageIcon image)
          throws Exception
  {
      // can default the macro from other attributes
      String macro = style.attributes.get(type + ".icon");
      if (macro == null || macro.length() == 0) {
          StringBuffer sb = new StringBuffer(64);

          String att = style.attributes.get(type + ".background");
          if (att == null) att = style.attributes.get("background");

          if (att != null) {
              sb.append(att);
              sb.append("; {icon};");

              att = (String) style.attributes.get(type + ".overlay");
              if (att == null) att = style.attributes.get("overlay");
              if (att != null) sb.append(att);

              macro = sb.toString();
          }
          else
              // no macro possible, just return the image
              return image.getImage();
      }

      // we're still here, so apply the style...

      // initialise the namespace
      HashMap<String, Object> namespace = new HashMap<String, Object>(style.attributes);

      if (image != null) {
          Image img = image.getImage();
          namespace.put("{icon}", img);
          namespace.put('{' + type + ".icon}", img);
      }

      namespace.put("{owner}", owner);

      // get the classloader from the selected theme
      ClassLoader loader = ThemeManager.getSelectedTheme().loader;

      // return the result of applying the style
      return IconGenerator.apply(macro, null, namespace, loader, style.width, style.height);
  }
}
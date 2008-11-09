/*  IconGenerator.java  */

package artofillusion.util;

/*
 * IconGenerator: provide editing and compositing features for icon images
 *
 * Copyright (C) 2008 Nik Trevallyn-Jones, Sydney Australia
 *
 * Author: Nik Trevallyn-Jones, nik777@users.sourceforge.net
 * $Id: Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See version 2 of the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * with this program. If not, version 2 of the license is available
 * from the GNU project, at http://www.gnu.org.
 */

import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;


/**
 *  Apply editing functions to icon image(s).
 *
 *  IconGenerator supports a number of basic image operations such as overlay,
 *  blend, add, multiply, etc, and can apply these to the pixels of one or more
 *  images.
 *
 *  IconGenerator implements a basic macro processor, and the ability to execute
 *  a macro in source (string) form, or to compile source and to execute the
 *  compiled version.
 *
 *  IconGenerator supports a default syntax for macros, but the symbols
 *  mapped to each operation can be overridden by the caller.
 *
 *<br>
 *  The editing macro consists of one of more <i>instructions</i>, each separated by a delimiter.
 *  Each instruction can perform a single <i>operation</i> which can be modified in various ways.
 *  Each operation and each of the modifiers are specified by an <i>operator</i>
 *  each of which is specified by some token in the macro "language".
 *  
 *  All other words are either the pathname of an image or an object in the execution namespace. 
 *  
 *  The operators and the tokens that specify them in the default language binding are:
 *<ul>
 *<li>COMPOSIT semicolon (;) - at the end of each <i>instruction</i>, the result of the instruction is
 *	composited over the current image.
 *<br><li>OVERLAY pipe (|) - the lhs is overlaid over an equal-sized rectangle of colour specified by rhs
 *<br><li>ADD plus (+) - the rhs (an RGB colour) is added, component by component, to each pixel of lhs
 *<br><li>SUBTRACT minus (-) - the rhs (an RGB colour) is subtracted, component by component, from each pixel of lhs
 *<br><li>MULTIPLY asterisk (*) - each pixel of lhs is multiplied, component by component, by rhs
 *	(an ARGB colour, or a list of scalar (floating point) values). If a component in RHS is not speified,
 *	then the corresponding component in LHS is left unmodified.
 *<br><li>ASSIGN equals (=) - rhs (an ARGB colour) is assigned, component by component, to each pixel of lhs.
 *	If any component of RHS is not specified, then the corresponding component in LHS is left unmodified.
 *<br><li>FEATHER tilde (~) - lhs has feathering applied to selected pixels using the value(s) in rhs to
 *	determine the type of feathering applied:
 *<br>A number or set of numbers causes the image to be feathered against itself - ie antialiased.
 *<br>A set of BOUNDS causes the image to be feathered at its edges.
 *<br><li>BOUNDS square brackets ([]) - sets or modifies the current instruction by altering the bounds (size)
 *	of the image (larger or smaller)
 *<br><li>POSITION at (@) - modifies the current instruction by altering the image position when it is composited.
 *<br><li>RESOLVE parens ({}) - specifies a name as being a logical name in the namespace.
 *	A name is resolved by searching the namespace for a matching string. Resolution is recursive,
 *	meaning that if the result of the namespace lookup is a resolvable string, then that string is itself
 *	looked up.
 *<br><li>CALL dot (.) - call a method on the specified object (lhs).
 *<br><li>COLOR hash (#) - specifies a color as a list of component values
 *<br>eg: #(1.5, 1, 1, 1)  or #(a=1.5,g=1,b=1)
 *<br><li>HEX (0x) or (0X) - specifies an RGB or ARGB 8-bit colour value as a single hex value
 *<br>egs: 0xff => alpha=1; 0x112233 => red=11, green=22, blue=33; 0xff112233 => alpha=ff, red=11, green=22, blue=33
 *</ul>
 *<br>
 *<pre>
 *Examples (using default language):
 *  "BackgroundIcon; {icon}"
 *  an image file called BackgroundIcon, overlaid with an image resolved from the namespace 
 *  with the name "{icon}".
 *    
 *  "SelectedBackgroundIcon; {icon} + 0x200000 @(x+1,y+1) [-1,-1]"
 *  an image file called SelectedBackgroundIcon overlaid with an image resolved from the namespace with the name "{icon}"
 *  which has had 0x200000 added to each pixel (red increased) and has been offset by x+1, y+1
 *  and has been reduced in size by 1 pixel on x and y axes.
 *  
 *  "0x121212 [32,32]; {icon}; @(z+3)"
 *  the colour 0x121212 sized to a 32x32 image overlaid with the {icon} image
 *  with the result of that offset on the z-axis by +3 (this results in a 3D "popped out" effect)
 *   
 *  "0x080808 [32,32]; {icon} * #(r=0.6,g=0.6,b=0.6) @(x+1,y+1); @(z-3)"
 *  the colour 0x080808 sized to a 32x32 image, overlaid with the {icon} image
 *  which has been multiplied by 0.6 on red, green and blue (made darker)
 *  and offset x+1,y+1 (down 1, right 1);
 *  the result of that is then positioned -3 on the Z-axis (which results in a 3D "pressed in" effect).
 *
 *  "{widget}.getBackground() [32,32]; {icon} * #(r=1.0,g=0.5,b=0.5)"
 *  The colour returned by calling getBackground() on the object in the
 *  namespace called "{widget}"; overlaid with the {icon} image which has been
 *  multiplied by 1.0 on red, and 0.5 on green and blue (made redder).
 *
 *  "BackgroundIcon; {icon} ~(0.5)"
 *  The image named BackgroundIcom overlaid with the {icon} image in the
 *  namespace which has had antialiasing applied using alpha=50% for dark AA
 *  pixels, and light AA pixels having a alpha calculated from the dark pixels.
 *  
 *  "BackgroundIcon; {icon} ~(0.5, 0.66)"
 *  The image named BackgroundIcom overlaid with the {icon} image in the
 *  namespace which has had antialiasing applied using alpha=50% for the
 *  darkest AA pixels, with successively lighter pixels being 66% as dark.
 *  
 *  "BackgroundIcon; {icon} ~[3,3]"
 *  The image named BackgroundIcon overlaid with the {icon} image in the
 *  namespace which has had it edges feathered to transparent over an area
 *  3 pixels wide on each side (x-axis) and 3 pixels wide on top and bottom
 *  (y-axis).
 *  
 *  "BackgroundIcon; {icon}; 0xa0804040 ~[-6,-6]"
 *  The image named BackgroundIcom overlaid with the {icon} image in the
 *  namespace which has then been overlaid with a region of the specified
 *  colour (translucent red) which has had its centre feathered to transparent
 *  from the 6th pixel from the edge into the centre.
 *  
 *  "BackgroundIcon; {icon}; 0xa0804040 [-4, -4, ~3]"
 *  The image named BackgroundIcom overlaid with the {icon} image in the
 *  namespace, overlaid with a a region of the specified colour
 *  (translucent red) which is 4 pixels smaller than the current image in both
 *  X and Y axes, and which has had the outer 3 pixels feathered to trasnparent.
 *  
 *</pre>
 */

public class IconGenerator
{
    public static final byte FEATHER_OUT_DIR	= 0x1;
    public static final byte FEATHER_IN_DIR	= 0x2;

    protected Instruction program = null;
    protected int imageWidth, imageHeight;
    protected String[] delims;

    // the meaning of the chars in delims[ARG_DELIMS]    
    protected static final byte ARG_LIST	= 0x0;
    protected static final byte ARG_RESOLVE	= 0x1;
    protected static final byte ARG_NUMBER	= 0x2;

    // the meaning of the chars in delims[OPS]
    protected static final byte OP_NONE		= 0x0;
    protected static final byte OP_COMPOSIT	= 0x1;
    protected static final byte OP_OVERLAY	= 0x2;
    protected static final byte OP_ADD		= 0x3;
    protected static final byte OP_SUBTRACT	= 0x4;
    protected static final byte OP_MULTIPLY	= 0x5;
    protected static final byte OP_ASSIGN	= 0x6;
    protected static final byte OP_SCALE	= 0x7;
    protected static final byte OP_FEATHER	= 0x8;
    protected static final byte OP_POSITION	= 0x9;
    protected static final byte OP_RESIZE	= 0xa;
    protected static final byte OP_CALL		= 0xb;
    protected static final byte OP_COLOR	= 0xc;

    // must be last assigned OP_ code
    protected static final byte OP_ARG		= 0xd;

    /*
     *  now the "arg" opcodes.
     *  These *cannot* be included in DELIMS because they represent chars that
     *  are *included* in an arg, rather than delimiting an arg.
     */
    protected static final byte OP_LIST		= OP_ARG+ARG_LIST;
    protected static final byte OP_RESOLVE	= OP_ARG+ARG_RESOLVE;
    protected static final byte OP_NUMBER	= OP_ARG+ARG_NUMBER;

    // the three separate strings of delims
    protected static final int OPS = 0;
    protected static final int ARG_DELIMS = 1;
    protected static final int OPEN_CLOSE = 2;
    
    protected static final String[] DEFAULT_DELIMS = { ";|+-*=%~@[.#", ",{0", "()[]{}" };

    protected static final String QUOTES = "\"'";
    protected static final String WHITESPACE = " \t\r\n";
    protected static final String DEFAULT_TARGET = "{}";

    protected static final float SCALE_DOWN = 1.0f/0xff;
    
    protected static final Class[] NULL_SIG = new Class[0];
    protected static final Object[] NULL_ARGS = new Object[0];
    
    private static final String dots =
	"................................................................................";
    
    /**
     * create a new IconGenerator for the specified macro using the <i>default</i> delims.
     * 
     * @param macro
     * @throws Exception
     */
    public IconGenerator(String macro)
    	throws Exception
    {
	delims = DEFAULT_DELIMS;
	compile(macro);
    }

    /**
     * create a new IconGenerator for the specified macro and delims.
     * 
     * @param macro
     * @param delims
     * @throws Exception
     */
    public IconGenerator(String macro, String[] delims)
	throws Exception
    {
	this.delims = delims;
	compile(macro);
    }

    /**
     * compile the specified macro using the <i>delims</i> defined for this IconGenerator.
     * 
     * @param macro
     * @throws Exception
     */
    public void compile(String macro)
    	throws Exception
    {
	int len = macro.length();
	program = new Instruction(imageWidth, imageHeight);
	program.compile(macro, 0, delims);

	Instruction instr = program;
	for (int pos = program.end+1; pos < len; pos = instr.end+1) {
	    instr = instr.compileNext(delims);
	}
    }

    /**
     * set the default size for this IconGenerator.
     * 
     * @param width
     * @param height
     */
    public void setSize(int width, int height)
    {
	imageWidth = width;
	imageHeight = height;
	
	if (program != null) program.setSize(width, height);
    }

    /**
     * execute a compiled IconGenerator.
     * 
     * @param namespace
     * @param loader
     * @return
     * @throws Exception if this IconGenerator is <i>not</i> compiled, or the compiled instructions
     * 		throw and exception.
     */
    public Image execute(Map<String, Object> namespace, ClassLoader loader)
	throws Exception
    {
	if (program == null) throw new Exception("IconGenerator has no compiled program");
	return program.execute(null, namespace, loader);
    }

    /**
     * apply (compile and execute) the specified macro.
     * 
     * @param macro the macro to apply
     * @param delims an array of 3 string, defining the operator and delimiter chars to use
     * @param namespace a Map containing the named objects that can be resolved 
     * @param loader the ClassLoader to use for loading external image files.
     * @param width the (initial) width of the icon
     * @param height the (initial) height of the icon
     * 
     * @return the resulting Image
     * @throws Exception
     */
    public static Image apply(String macro, String[] delims, Map<String, Object> namespace, ClassLoader loader, int width, int height)
	throws Exception
    {
	int len = macro.length();
	Instruction instr = new Instruction(width, height);
	BufferedImage result = null;
	
	for (int pos = 0; pos < len; pos = instr.end+1) {
	    instr.compile(macro, pos, delims);
	    result = instr.execute(result, namespace, loader);
	}

	return result;
    }

    /**
     * apply (compile and execute) the specified macro
     * @see #apply(String, String[], Map, ClassLoader, int, int)
     */
    public static Image apply(String macro, String[] delims, Map<String, Object> namespace, ClassLoader loader)
    	throws Exception
    { return apply(macro, delims, namespace, loader, -1, -1); }

    /**
     * apply (compile and execute) the specified macro. 
     * @see #apply(String, String[], Map, ClassLoader, int, int)
     */
    public static Image apply(String macro, Map<String, Object> namespace, ClassLoader loader)
            throws Exception
    { return apply(macro, DEFAULT_DELIMS, namespace, loader, -1, -1); }

    /**
     * copy an image.
     *
     *  The result will be the same as the original, in both content and size.
     *
     *  @see #copy(Image, int, int, float)
     */
    public static BufferedImage copy(Image orig)
    { return copy(orig, -1, -1, 0); }

    /**
     *  copy the original image to a new image of the specified size
     *  (may be larger or smaller). The content of the <i>orig</i> image is
     *  centered within the new image.
     *
     *  @param orig the image to copy <i>from</i>
     *  @param width the width of the new image
     *  @param height the height of the new image.
     *  @param scale the way to scale original when copying.
     *                scale == 0 means no scaling;
     *                scale &gt; 0 means scale original to new dims * scale
     *                scale &lt; 0 means scale original abs(scale) piselx smaller than new sims
     *
     *  @return a newly allocated image of the specified ize containing the
     *                content of <i>orig</i> centered within it.
     */    
    public static BufferedImage copy(Image orig, int width, int height, float scale)
    {
        int w = orig.getWidth(null);
        int h = orig.getHeight(null);

        if (width <= 0) width = w;
        else if (scale > 0) w = (int) (width * scale);
        else if (scale < 0) w = (int) (width + scale);

        if (height <= 0) height = h;
        else if (scale > 0) h = (int) (height * scale);
        else if (scale < 0) h = (int) (height + scale);

        int x = (width-w)/2;
        int y = (height-h)/2;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D graphics = (Graphics2D) result.getGraphics();

        //graphics.setClip(x, y, width, height);
        if (scale != 0)
            graphics.drawImage(orig, x, y, w, h, null);
        else
            graphics.drawImage(orig, x, y, null);

        graphics.dispose();

        return result;
    }

    /**
     * draw a 3d bevelled edge on a rectangular image
     * 
     * @param image the image to modify
     * @param depth the "size" and "direction" of the bevel, which results in
     *                the specified apparent "depth" or "height".
     *
     *  @see #bevel3D(BufferedImage, int, Rectangle)
     */
    public static void bevel3D(BufferedImage image, int depth)
    { bevel3D(image, depth, null); }

    /**
     *  draw a 3d bevelled edge on or in the image, with the edges being those
     *  of the specified clip rectangle.
     *
     *  @param image the image to modify
     *  @param depth the size and "direction" of the bevelled edge.
     *                a positive value creates a "popped-out" effect by making the
     *                top and left edges lighter and the bottom and right edges
     *                darker. A negative value creates a "pressed-in" effect by
     *                doing the opposite. The absolute value determines the apparent
     *                depth or height of the 3d effect by determining the width in
     *                pixels of the bevelled edge. So 1 creates an edge 1 pixel wide,
     *                and 3 creates an edge 3 pixels wide.
     */    
    public static void bevel3D(BufferedImage image, int depth, Rectangle clip)
    {
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (x < 0) x = 0;
        if (y < 0) y = 0;

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        float alpha = 0.65f;
        float feather = 0.85f;
        //float comp = 1.0f;
        //float darkComp;
        int max = (depth < 0 ? -depth : depth);

        Color light = Color.WHITE;
        Color dark = Color.BLACK;

        int w = width-1;
        int h = height-1;

        // loop for each strip of the bevel
        for (int i = 0; i < max; i++) {

            /*
             *  The dark lines are drawn first, then the light lines are drawn
             *  over them. This gives nice antialiasing at the corners where
             *  the lines overlap.
             *
             *  The intensity of each successive line is "feathered" by
             *  multiplying alpha by a value which is < 1.0.
             *
             *  In fact, to get a slightly "curved" look, the feather value
             *  is itself feathered at each cycle.
             *
             *  In addition, the dark lines are feathered more aggressively
             *  (twice per cycle compared to the light lines' once).
             */

            // set the alpha value for the dark lines (smaller than for light)
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SrcOver.getRule(), alpha*feather));

            //darkComp = 1.0f - comp*feather;

            // draw the dark sides first
            //graphics.setColor(new Color(darkComp, darkComp, darkComp));
            graphics.setColor(dark);

            if (depth > 0) {
                // draw right and bottom lines
                graphics.drawLine(x+i+w, y+i, x+i+w, y+i+h);
                graphics.drawLine(x+i, y+i+h, x+i+w-1, y+i+h);
            }
            else {
                // draw top and left lines
                graphics.drawLine(x+i, y+i, x+i+w, y+i);
                graphics.drawLine(x+i, y+i+1, x+i, y+i+h);
            }

            // set the alpha value for the light lines
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SrcOver.getRule(), alpha));

            // now draw light lines (so corners get antialiasing)
            //graphics.setColor(new Color(comp, comp, comp));
            graphics.setColor(light);

            if (depth > 0) {
                // draw top and left lines
                graphics.drawLine(x+i, y+i, x+i+w, y+i);
                graphics.drawLine(x+i, y+i+1, x+i, y+i+h);
            }
            else {
                // draw right and bottom lines
                graphics.drawLine(x+i+w, y+i, x+i+w, y+i+h);
                graphics.drawLine(x+i, y+i+h, x+i+w-1, y+i+h);
            }

            // adjust for next strip of bevel
            w -= 2;
            h -= 2;

            // feather the feather value, then use that to feather alpha
            feather *= 0.9f;
            alpha *= feather;
            //comp *= 0.95f;
        }

        graphics.dispose();
    }

    /**
     * add the color components to each pixel in the image.
     *
     * @see #add(BufferedImage, int, int, int, Rectangle)
     */
    public static void add(BufferedImage image, int red, int green, int blue)
    { add(image, red, green, blue, null); }

    /**
     * add the colour components to each pixel in a rectangle within the image.
     *
     *  @param image the image to modify
     *
     *  @param red the red component to add. 0 <= red <= 255
     *  @param green the green component to add. 0 <= green <= 255
     *  @param blue the blue component to add. 0 <= blue <= 255
     *
     *  @param clip the area within which the modification is applied.
     */
    public static void add(BufferedImage image, int red, int green, int blue, Rectangle clip)
    {
        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        int[] pix = new int[width];
        int p, a, r, g, b;
        int max = height+y;
        // add to pixels
        for (int i = y; i < max; i++) {
            pix = image.getRGB(x, i, width, 1, pix, 0, width);
            for (int j = 0; j < width; j++) {
                p = pix[j];
                r = Math.min(0xff, ((p >> 16) & 0xff) + red);
                g = Math.min(0xff, ((p >> 8) & 0xff) + green);
                b = Math.min(0xff, (p & 0xff) + blue);

                pix[j] = (p & 0xff000000) + (r << 16) + (g << 8) + b;
            }

            image.setRGB(x, i, width, 1, pix, 0, width);
        }
    }

    /**
     *  subtract the colour components from each pixel in image.
     *
     *  @see #subtract(BufferedImage, int, int, int, Rectangle)
     */
    public static void subtract(BufferedImage image, int red, int green, int blue)
    { subtract(image, red, green, blue, null); }

    /**
     *  subtract the colour components from each pixel in a rectangle within
     *  the image.
     *
     *  @param image the image to modify
     *
     *  @param red the red component to subtract. 0 <= red <= 255
     *  @param green the green component to subtract. 0 <= green <= 255
     *  @param blue the blue component to subtract. 0 <= blue <= 255
     *
     *  @param clip the area within which the modification is applied.
     */
    public static void subtract(BufferedImage image, int red, int green, int blue, Rectangle clip)
    {
        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        int[] pix = new int[width];
        int p, a, r, g, b;
        int max = height+y;
        for (int i = y; i < max; i++) {
            pix = image.getRGB(x, i, width, 1, pix, 0, width);
            for (int j = 0; j < width; j++) {
                p = pix[j];
                r = Math.max(0, ((p >> 16) & 0xff) - red);
                g = Math.max(0, ((p >> 8) & 0xff) - green);
                b = Math.max(0, (p & 0xff) - blue);

                pix[j] = (p & 0xff000000) + (r << 16) + (g << 8) + b;
            }

            image.setRGB(x, i, width, 1, pix, 0, width);
        }
    }

    /**
     * multiply the components of each pixel in the image
     *
     * @see #multiply(BufferedImage, float, float, float, float, Rectangle)
     */
    public static void multiply(BufferedImage image, float alpha, float red, float green, float blue)
    { multiply(image, alpha, red, green, blue, null); }

    /**
     * multiply the components of each pixel in a rectangle within the image.
     *
     *  @param image the image to modify
     *
     *  @param alpha the alpha component to multiply by. 0.0f <= alpha <= 1.0f
     *  @param red the red component to multiply by. 0.0f <= red <= 1.0f
     *  @param green the green component to multiply by. 0.0f <= green <= 1.0f
     *  @param blue the blue component to multiply by. 0.0f <= blue <= 1.0f
     *
     *  @param clip the area within which the modification is applied.
     */
    public static void multiply(BufferedImage image, float alpha, float red, float green, float blue, Rectangle clip)
    {
        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        int[] pix = new int[width];
        int p, a, r, g, b;
        int max = height+y;
        for (int i = y; i < max; i++) {
            pix = image.getRGB(x, i, width, 1, pix, 0, width);
            for (int j = 0; j < width; j++) {
                p = pix[j];
                a = (int) Math.min(0xff, alpha*((p >> 24) & 0xff));
                r = (int) Math.min(0xff, red*((p >> 16) & 0xff));
                g = (int) Math.min(0xff, green*((p >> 8) & 0xff));
                b = (int) Math.min(0xff, blue*(p & 0xff));

                pix[j] = (a << 24) + (r << 16) + (g << 8) + b;
            }

            image.setRGB(x, i, width, 1, pix, 0, width);
        }
    }

    /**
     *  assign the colour components to each pixel in the image.
     *
     *  @see #assign(BufferedImage, int, int, int, int, Rectangle)
     */
    public static void assign(BufferedImage image, int alpha, int red, int green, int blue)
    { assign(image, alpha, red, green, blue, null); }

    /**
     *  assign the colour components to each pixel in a rectangle within the
     *  image.
     *  If any component value is negative, then that component is not modified.
     *
     *  @param image the image to modify
     *
     *  @param alpha the alpha component to assign. 0 <= alpha <= 255
     *  @param red the red component to assign. 0 <= red <= 255
     *  @param green the green component to assign. 0 <= green <= 255
     *  @param blue the blue component to assign. 0 <= blue <= 255
     *
     *  @param clip the area within which the modification is applied.
     */
    public static void assign(BufferedImage image, int alpha, int red, int green, int blue, Rectangle clip)
    {
        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        int[] pix = new int[width];
        int p, a, r, g, b;
        int max = height+y;
        for (int i = y; i < max; i++) {
            pix = image.getRGB(x, i, width, 1, pix, 0, width);
            for (int j = 0; j < width; j++) {
                p = pix[j];
                a = (alpha >= 0 ? Math.min(0xff, Math.max(0, alpha)) : (p >> 24) & 0xff);
                r = (red >= 0 ? Math.min(0xff, Math.max(0, red)) : (p >> 16) & 0xff);
                g = (green >= 0 ? Math.min(0xff, Math.max(0, green)) : (p >> 8) & 0xff);
                b = (blue >= 0 ? Math.min(0xff, Math.max(0, blue)) : (p & 0xff));

                pix[j] = (a << 24) + (r << 16) + (g << 8) + b;
            }

            image.setRGB(x, i, width, 1, pix, 0, width);
        }
    }

    /**
     *  overlay the specified image over a background of the specified colour.
     *
     *  @see #overlay(BufferedImage, int, int, int, int, Rectangle)
     */
    public static void overlay(BufferedImage image, int alpha, int red, int green, int blue)
    { overlay(image, alpha, red, green, blue, null); }

    /**
     *  overlay the specified image over a rectangle of the specified colour.
     *
     *  @param image the image to modify
     *
     *  @param alpha the alpha component of the colour. 0 <= alpha <= 255
     *  @param red the red component of the colour. 0 <= red <= 255
     *  @param green the green component of the colour. 0 <= green <= 255
     *  @param blue the blue component of the colour. 0 <= blue <= 255
     *
     *  @param clip the area within which the modification is applied.
     */
    public static void overlay(BufferedImage image, int alpha, int red, int green, int blue, Rectangle clip)
    {
        System.out.println("overlay: a=" + alpha + "; r=" + red + "; g=" + green + "; b=" + blue);

        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        Graphics2D overlay = (Graphics2D) image.getGraphics();
        overlay.setComposite(AlphaComposite.DstOver);
        overlay.setColor(new Color(red, green, blue, alpha));
        overlay.fillRect(x, y, width, height);

        overlay.dispose();
    }

    /**
     */
    public static void overlay(BufferedImage image, Image overlay)
    { overlay(image, overlay, null); }

    /**
     * overlay one image over another.
     * 
     * @param image the image to modify
     * @param overlay the image to overlay onto <i>image</i>
     * @param clip the region of <i>image</i> to modify. If this is <i>null</i>,
     *                then <i>overlay</i> is centered on <i>image</i>.
     */
    public static void overlay(BufferedImage image, Image overlay, Rectangle clip)
    {
        int x=0, y=0;

        if (clip == null) {
            int width = image.getWidth();
            int height = image.getHeight();

            int w = overlay.getWidth(null);
            int h = overlay.getHeight(null);

            if (w != width || h != height) {
                x = (width-w)/2;
                y = (height-h)/2;

                clip = new Rectangle(x, y, Math.min(width, width-(2*x)), Math.min(height, height-(2*y)));
            }
        }
        else {
            x = clip.x;
            y = clip.y;
        }

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        if (clip != null) graphics.setClip(clip);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(overlay, x, y, null);

        graphics.dispose();
    }

    /**
     *  antialias the diagonal lines in image
     *
     *  @see #antialias(BufferedImage, float, float, Rectangle)
     */
    public static void antialias(BufferedImage image, float dark)
    { antialias(image, dark, -1, null); }

    /**
     *  antialias the diagonal lines a rectangular area of image.
     *
     *  @param image the image to antialias
     *  @param alpha the alpha value to use for the antialias strongest AA (antialias)
     *                pixels. 0.0 &lt; alpha &lt; 1.0
     *                The Strongest AA pixels are those with the most adjoining
     *                 opaque pixels (currently 4 is the maximum).
     *  @param attenuate the scaling factor to apply for increasingly weaker
     *                AA pixels. 0.0 &lt; attenuate. Lighter AA pixels are those that
     *                have fewer adjoining opaque pixels (currently 3 is the minimum).
     *
     *  @param clip the rectangular area to modify
     */
    public static void antialias(BufferedImage image, float alpha, float attenuate, Rectangle clip)
    {
        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        int[][] pix = new int[3][width];
        int[] tmp;

        int tl, tp, tr, lt, rt, bl, bm, br;

        if (alpha <= 0 || alpha >= 1) alpha = 0.5f;
        if (attenuate <= 0) attenuate = 0.66f;

        int dark = Math.max(0, Math.min(0xff, (int) (alpha * 255.0f))) << 24;
        int light = Math.max(0, Math.min(0xff, (int) (alpha * attenuate * 255.0f))) << 24;

        int i, j;
        int p, a, r, g, b;

        // get the first row
        pix[1] = image.getRGB(x, y, width, 1, pix[1], 0, width);

        int max = y+height;
        for (i = y; i < max; i++) {
            if (i < max-1)
                pix[2] = image.getRGB(x, i+1, width, 1, pix[2], 0, width);
            else
                pix[2] = null;

            for (j = x; j < width; j++) {
                tl = tp = tr = lt = rt = bl = bm = br = -1;

                p = pix[1][j];
                if (((p >> 24) & 0xff) > 0) continue;        // ignore non-transparent pixels

                // fill in details of all surrounding pixels
                if (pix[0] != null) {
                    if (j > 0) tl = (pix[0][j-1] >> 24) & 0xff;
                    tp = (pix[0][j] >> 24) & 0xff;
                    if (j < width-1) tr = (pix[0][j+1] >> 24) & 0xff;
                }

                if (j > 0) lt = (pix[1][j-1] >> 24) & 0xff;
                if (j < width-1) rt = (pix[1][j+1] >> 24) & 0xff;

                if (pix[2] != null) {
                    if (j > 0) bl = (pix[2][j-1] >> 24) & 0xff;
                    bm = (pix[2][j] >> 24) & 0xff;
                    if (j < width-1) br = (pix[2][j+1] >> 24) & 0xff;
                }

                // now look for antialiasing opportunities
                if (tp == 0xff && bm <= 0) {
                    if (lt == 0xff && tl == 0xff && rt <= 0 && br <= 0) {
                        a = (tr == 0xff || bl == 0xff ? dark : light);

                        r = ((pix[0][j-1] >> 16) & 0xff);
                        r += ((pix[0][j] >> 16) & 0xff);
                        r += ((pix[1][j-1] >> 16) & 0xff);
                        r /= 3;

                        g = ((pix[0][j-1] >> 8) & 0xff);
                        g += ((pix[0][j] >> 8) & 0xff);
                        g += ((pix[1][j-1] >> 8) & 0xff);
                        g /= 3;

                        b = (pix[0][j-1] & 0xff);
                        b += (pix[0][j] & 0xff);
                        b += (pix[1][j-1] & 0xff);
                        b /= 3;

                        pix[1][j] = a + (r << 16) + (g << 8) + b;
                    }
                    else if (rt == 0xff && tr == 0xff && lt <= 0 && bl <= 0) {
                        a = (tl == 0xff || br == 0xff ? dark : light);

                        r = ((pix[0][j] >> 16) & 0xff);
                        r += ((pix[0][j+1] >> 16) & 0xff);
                        r += ((pix[1][j+1] >> 16) & 0xff);
                        r /= 3;

                        g = ((pix[0][j] >> 8) & 0xff);
                        g += ((pix[0][j+1] >> 8) & 0xff);
                        g += ((pix[1][j+1] >> 8) & 0xff);
                        g /= 3;

                        b = (pix[0][j] & 0xff);
                        b += (pix[0][j+1] & 0xff);
                        b += (pix[1][j+1] & 0xff);
                        b /= 3;

                        pix[1][j] = a + (r << 16) + (g << 8) + b;
                    }
                }
                else if (tp <= 0 && bm == 0xff) {
                    if (lt == 0xff && bl == 0xff && rt <= 0 && tr <= 0) {
                        a = (tl == 0xff || br == 0xff ? dark : light);

                        r = ((pix[1][j-1] >> 16) & 0xff);
                        r += ((pix[2][j] >> 16) & 0xff);
                        r += ((pix[2][j-1] >> 16) & 0xff);
                        r /= 3;

                        g = ((pix[1][j-1] >> 8) & 0xff);
                        g += ((pix[2][j] >> 8) & 0xff);
                        g += ((pix[2][j-1] >> 8) & 0xff);
                        g /= 3;

                        b = (pix[1][j-1] & 0xff);
                        b += (pix[2][j] & 0xff);
                        b += (pix[2][j-1] & 0xff);
                        b /= 3;

                        pix[1][j] = a + (r << 16) + (g << 8) + b;
                    }
                    else if (rt == 0xff && br == 0xff && lt <= 0 && tl <= 0) {
                        a = (tr == 0xff || bl == 0xff ? dark : light);

                        r = ((pix[1][j+1] >> 16) & 0xff);
                        r += ((pix[2][j] >> 16) & 0xff);
                        r += ((pix[2][j+1] >> 16) & 0xff);
                        r /= 3;

                        g = ((pix[1][j+1] >> 8) & 0xff);
                        g += ((pix[2][j] >> 8) & 0xff);
                        g += ((pix[2][j+1] >> 8) & 0xff);
                        g /= 3;

                        b = (pix[1][j+1] & 0xff);
                        b += (pix[2][j] & 0xff);
                        b += (pix[2][j+1] & 0xff);
                        b /= 3;

                        pix[1][j] = a + (r << 16) + (g << 8) + b;
                    }
                }
            }

            image.setRGB(x, i, width, 1, pix[1], 0, width);

            tmp = pix[0];
            pix[0] = pix[1];
            pix[1] = pix[2];
            pix[2] = tmp;
        }
    }

    /**
     *  feather (fade to transparent) the image.
     *
     *  @see #feather(BufferedImage, int, int, byte, Rectangle)
     */
    public static void feather(BufferedImage image, int xsize, int ysize, byte dir)
    { feather(image, xsize, ysize, dir, null); }

    /**
     *  The XSIZE and YSIZE parameters determine how far from the edge
     *  of the image to start feathering, and the DIRection specifies whether
     *  to feather OUT (FEATHER_OUT_DIR) towards the edges, or
     *  IN (FEATHER_IN_DIR) towards the centre.
     *
     *  @param image the image to modify
     *  @param xsize start feathering at the nth pixel from the left and right edges.
     *  @param ysize start feathering at the nth pixel from the top and bottom edges.
     *  @param dir specifies whether to feather OUT towards the edges, or IN
     *                towards the centre.
     *                see {@link #FEATHER_OUT_DIR} and {@link #FEATHER_IN_DIR}
     *
     *  @param clip defines the edges for feathering. If this is non-null,
     *                and specifies values other than
     *                (0, 0, image.width, image.height), then the feathering is
     *                performed relative to the edges of this rectangle.
     *
     *                This is useful if the feathered image is going to be overlaid
     *                on some other image using an identical clip region.
     */    
    public static void feather(BufferedImage image, int xsize, int ysize, byte dir, Rectangle clip)
    {
        int x = (clip != null ? clip.x : 0);
        int y = (clip != null ? clip.y : 0);
        int width = (clip != null ? clip.width : -1);
        int height = (clip != null ? clip.height : -1);

        if (width <= 0) width = image.getWidth();
        if (height <= 0) height = image.getHeight();

        if (xsize >= width && ysize >= height) return;

        int[] pix1 = new int[width];
        int[] pix2 = new int[width];

        int i, j, p, pa, a, r, g, b;

        float transy = 1.0f;
        float transx;
        float atten = 0.8f;
        float alpha;

        int opaque = 0xff << 24;

        int cy = height/2;
        int cx = width/2;

        int firstx = xsize;
        int mitre = Math.min(xsize, (ysize > 0 ? ysize : xsize));

        /*
         * to avoid problems with images that already contain transparent pixels,
         * we only feather pixels *less* trasnparent. Pixels already *more* transparent
         * are left unchanged.
         */

        for (i = y; i <= cy; i++ ) {
            if (dir == FEATHER_OUT_DIR) {

                pix1 = image.getRGB(0, i, width, 1, pix1, 0, width);
                pix2 = image.getRGB(0, height-i-1, width, 1, pix2, 0, width);

                transx = atten;
                pa = ((int) ((1.0f - transx) * 0xff));
                a = pa << 24;

                // do the side pixels individually
                firstx = Math.min(i, mitre);
                for (j = x; j <= firstx; j++) {

                    pa = ((int) ((1.0f - transx) * 0xff));
                    a = pa << 24;

                    if (pa < ((pix1[j] >> 24) & 0xff))
                        pix1[j] = a + (pix1[j] & 0xffffff);

                    if (pa < ((pix1[width-j-1] >> 24) & 0xff))
                        pix1[width-j-1] = a + (pix1[width-j-1] & 0xffffff);

                    if (pa < ((pix2[j] >> 24) & 0xff))
                        pix2[j] = a + (pix2[j] & 0xffffff);

                    if (pa < ((pix2[width-j-1] >> 24) & 0xff))
                        pix2[width-j-1] = a + (pix2[width-j-1] & 0xffffff);

                    transx *= atten;
                }

                // now do the rest of the line
                if (i < ysize) {
                    //pa = (int) ((1.0f - transy) * 0xff);
                    //a = pa << 24;

                    for (j = firstx+1; j < width-firstx-1; j++) {
                        if (pa < ((pix1[j] >> 24) & 0xff))
                            pix1[j] = a + (pix1[j] & 0xffffff);

                        if (pa < ((pix2[j] >> 24) & 0xff))
                            pix2[j] = a + (pix2[j] & 0xffffff);
                    }
                }

                image.setRGB(0, i, width, 1, pix1, 0, width);
                image.setRGB(0, height-i-1, width, 1, pix2, 0, width);

                transy *= atten;
            }
            else if (dir == FEATHER_IN_DIR) {

                pix1 = image.getRGB(0, i, width, 1, pix1, 0, width);
                pix2 = image.getRGB(0, height-i-1, width, 1, pix2, 0, width);

                if (i < ysize) continue;

                /*
                // outside feather zone, no change
                if (i < y) {
                    for (j = 0; j < width; j++) {
                        pix1[j] = opaque + (pix1[j] & 0xffffff);
                        pix2[j] = opaque + (pix2[j] & 0xffffff);
                    }

                    image.setRGB(0, i, width, 1, pix1, 0, width);
                    image.setRGB(0, height-i-1, width, 1, pix2, 0, width);

                    continue;
                }
                */

                /*
                // first, the opaque side pixels
                for (j = 0; j < x; j++) {
                    pix1[j] = opaque + (pix1[j] & 0xffffff);
                    pix1[width-j-1] = opaque + (pix1[width-j-1] & 0xffffff);

                    pix2[j] = opaque + (pix2[j] & 0xffffff);
                    pix2[width-j-1] = opaque + (pix2[width-j-1] & 0xffffff);
                }
                */

                // then the feathered pixels
                alpha = 0.6f;
                pa = ((int) (alpha * 0xff));
                a = pa << 24;

                for (j = xsize; j <= cx; j++) {

                    // mitre the corner
                    if (j < i) {
                        alpha *= atten;

                        pa = ((int) (alpha * 0xff));
                        a = pa << 24;
                    }

                    if (pa < ((pix1[j] >> 24) & 0xff))
                        pix1[j] = a + (pix1[j] & 0xffffff);

                    if (pa < ((pix1[width-j-1] >> 24) & 0xff))
                        pix1[width-j-1] = a + (pix1[width-j-1] & 0xffffff);

                    if (pa < ((pix2[j] >> 24) & 0xff))
                        pix2[j] = a + (pix2[j] & 0xffffff);

                    if (pa < ((pix2[width-j-1] >> 24) & 0xff))
                        pix2[width-j-1] = a + (pix2[width-j-1] & 0xffffff);

                }

                image.setRGB(0, i, width, 1, pix1, 0, width);
                image.setRGB(0, height-i-1, width, 1, pix2, 0, width);
            }
        }
    }

    /**
     * nested class for the compiled instructions
     *  
     * @author nik
     *
     */
    protected static class Instruction
    {
        /** reference to the macro source  */
        String source;

        /** starting char in source of this instruction */
        int start = -1;

        /** ending char in source of this instruction */
        int end = 0;

        /**  definition of the "language" for this instruction  */
        String[] delims;

        /** opcode for this instruction */
        byte opcode;

        /** externally set size */
        int imageWidth, imageHeight;

        /** components of the colour argument */
        float alpha, red, green, blue;

        /** scale this result */
        float scale;

        /** name of target image/object of this instruction  */
        String target;

        /** name of method to call for this instruction  */
        ArrayList<String> method;

        /** antia-alias blend factors  */
        ArgList antialias = null;

        /** offset arglist  */
        ArgList offset = null;

        /** resize arglist  */
        ArgList resize = null;

        /** background colour  */
        Color background;

        /** next instruction in this program  */
        Instruction next;

        /**
         * create an instruction for an image of undefined size
         */
        public Instruction()
        {
            imageWidth = -1;
            imageHeight = -1;
        }

        /**
         * create a new empty Instruction
         */
        public Instruction(int width, int height)
        {
            imageWidth = width;
            imageHeight = height;
        }

        /**
         * set the image size on this ad all subsequent Instructions
         */
        public void setSize(int width, int height)
        {
            imageWidth = width;
            imageHeight = height;
            if (next != null) next.setSize(width, height);
        }

        /**
         * compile the next Instruction
         */
        public Instruction compileNext(String[] delims)
           throws Exception
        {
            if (next == null) next = new Instruction(imageWidth, imageHeight);
            next.compile(source, end, delims);

            return next;
        }

        /**
         * compile one instruction from the macro into this Instruction object.
         */
        public void compile(String macro, int pos, String[] delims)
           throws Exception
        {
            source = macro;
            this.delims = delims;

            start = pos;
            while (WHITESPACE.indexOf(macro.charAt(pos)) >= 0) pos++;

            alpha = 1.0f;
            red = green = blue = -1.0f;
            scale = 1.0f;
            opcode = OP_NONE;
            target = null;
            method = null;
            if (antialias != null) antialias.length = 0;
            if (resize != null) resize.length = 0;
            if (offset != null) offset.length = 0;
            background = null;

            next = null;

            Color color;

            int i, count, arglen, cut;
            int x, y, w, h;
            int valid = 0, validop;

            char c;
            String arg;
            byte opval;

            /*
             *  get the various lookup strings that define the language
             */

            this.delims = (delims != null ? (String[]) delims.clone() : DEFAULT_DELIMS);

            if (this.delims[OPS] == null)
                this.delims[OPS] = DEFAULT_DELIMS[OPS];
            if (this.delims[ARG_DELIMS] == null)
                this.delims[ARG_DELIMS] = DEFAULT_DELIMS[ARG_DELIMS];
            if (this.delims[OPEN_CLOSE] == null)
                this.delims[OPEN_CLOSE] = DEFAULT_DELIMS[OPEN_CLOSE];

            String ops = this.delims[OPS];
            String tokens = this.delims[ARG_DELIMS];
            String openclose = this.delims[OPEN_CLOSE];

            // collect all the close chars into a single string
            String close = ops.substring(OP_COMPOSIT-1, OP_COMPOSIT);
            for (i = 1; i < openclose.length(); i += 2)
                close += openclose.charAt(i);

            // the opcode-and-token lookup which is ops + tokens
            String optok = ops + tokens;

            int len = macro.length();

            while (pos >= 0 && (end = parseToken(macro, pos)) >= pos) {

                c = macro.charAt(pos);
                opval = (byte) (optok.indexOf(c)+1);

                // test for valid
                if (valid != 0) {
                    if (opval > 0) validop = 1 << opval;
                    else validop = 0;

                    if ((valid & validop) == 0)
                        error("Syntax error: invalid operand: " + c, pos);
                }

                // reset valid
                valid = 0;

                //System.out.println("compile: pos=" + pos + "; end= " + end + "; c=" + c + "; opval=" + opval);

                if (opval == OP_COMPOSIT) {
                    if (opcode == OP_NONE) opcode = opval;
                    end++;
                    break;
                }

                switch (opval) {

                case OP_ADD:
                case OP_SUBTRACT:
                case OP_MULTIPLY:
                case OP_OVERLAY:
                case OP_ASSIGN:
                    // set the valid operands
                    valid = (1 << OP_COLOR) + (1 << OP_NUMBER);

                    // default target is the current image
                    if (target == null || target.length() == 0) target = DEFAULT_TARGET;

                    // overwrite the opcode
                    opcode = opval;
                    break;

                case OP_SCALE:
                    valid = (1 << OP_NUMBER);

                    // default target is the current image
                    if (target == null || target.length() == 0) target = DEFAULT_TARGET;

                    // overwrite the opcode
                    opcode = opval;

                    if (pos < end) scale = Float.parseFloat(macro.substring(pos, end).trim())/100;
                    break;

                case OP_FEATHER:
                    //if (target == null || target.length() == 0) target = DEFAULT_TARGET;

                    // a list of values in parens
                    if (pos < len && macro.charAt(pos+1) == '(') {;
                        end = find(macro, pos+1, len, close);
                        antialias = parseArgs(antialias, 2, pos+2, end);
                    }
                    else {
                        if (opcode == OP_COLOR) opcode = OP_FEATHER;

                        // valid args are: a number or a color or resize
                        valid = (1 << OP_NUMBER) + (1 << OP_COLOR) + (1 << OP_RESIZE);
                    }
                    break;

                case OP_RESIZE:
                    if (pos == end) end = find(macro, end, len, close);
                    resize = parseArgs(resize, 4, pos+1, end);
                    break;

                case OP_POSITION:
                    if (pos == end) end = find(macro, end+1, len, close);
                    offset = parseArgs(offset, 3, pos+2, end);
                    break;

                case OP_COLOR:
                    // parse a colour by components
                    if (pos == end) end = find(macro, end+1, len, close);
                    ArgList comps = parseArgs(null, 4, pos+2, end);
                    if (comps.length > 0 && comps.value[0] != null)
                        alpha = Float.parseFloat(comps.value[0]);

                    if (comps.length > 1 && comps.value[1] != null)
                        red = Float.parseFloat(comps.value[1]);

                    if (comps.length > 2 && comps.value[2] != null)
                        green = Float.parseFloat(comps.value[2]);

                    if (comps.length > 3 && comps.value[3] != null)
                        blue = Float.parseFloat(comps.value[3]);

                    break;

                case OP_CALL:
                    // method call
                    if (pos == end) end = find(macro, end+1, len, ")");
                    if (end > pos) {
                        if (method == null) method = new ArrayList<String>(4);

                        method.add(macro.substring(pos+1, end+1).trim());
                    }
                    else
                        error("Invalid method syntax: closing paren ')' missing", pos);

                    //end = find(macro, end, len, close);
                    break;

                case OP_NUMBER:
                    // parse a number

                    arglen = macro.indexOf(ops.charAt(OP_COMPOSIT-1), pos);
                    if (arglen > 0 && arglen < end) end = arglen-1;
                    arg = macro.substring(pos, end).trim();

                    if (arg.startsWith("0x") || arg.startsWith("0X")) {
                        // parse HEX numbers as a form of RGB color
                        color = null;
                        arglen = arg.length();
                        if (arglen == 4)
                            alpha = SCALE_DOWN * Integer.decode(arg).intValue();
                        else if (arg.length() == 8)
                            color = Color.decode(arg);
                        else if (arglen == 10) {
                            alpha = SCALE_DOWN * Integer.decode(arg.substring(0, 4)).intValue();
                            color = Color.decode("0x" + arg.substring(4));
                        }

                        if (color != null) {
                            opval = OP_COLOR;
                            red = SCALE_DOWN * color.getRed();
                            green = SCALE_DOWN * color.getGreen();
                            blue = SCALE_DOWN * color.getBlue();
                        }
                    }
                    else {
                        // parse a floating point value
                        switch (opcode) {
                        case OP_SCALE:
                            scale = Float.parseFloat(arg)/100;
                            break;

                        case OP_FEATHER:
                            antialias = parseArgs(antialias, 1, pos, end);
                            break;

                        case OP_ADD:
                        case OP_SUBTRACT:
                            red = green = blue = Float.parseFloat(arg);
                            break;

                        case OP_MULTIPLY:
                            alpha = Float.parseFloat(arg);
                            break;

                        default:
                            error("Unexpected numeric value: " + arg, pos);
                            break;
                        }
                    }
                    break;

                case OP_RESOLVE:
                    target = macro.substring(pos, end+1).trim();
                    break;

                default:
                    if (end < len && macro.charAt(end) == '(') {
                        if (method == null) method = new ArrayList<String>(4);
                        method.add(macro.substring(pos, end-1).trim());
                    }
                    else
                        target = macro.substring(pos, end).trim();
                }

                // advance
                if (pos == end) end++;        // consume single-char token
                pos = parseToken(macro, end);

                // remember the first opcode
                if (opcode == OP_NONE && opval < OP_ARG) opcode = opval;

                // scale set values and default unset ones
                if (opval == OP_COLOR) {
                    switch (opcode) {
                    case OP_ADD:
                    case OP_SUBTRACT:
                        red = (red >= 0 ? red * 0xff : 0.0f);
                        green = (green >= 0 ? green * 0xff : 0.0f);
                        blue = (blue >= 0 ? blue * 0xff : 0.0f);
                        break;

                    case OP_MULTIPLY:
                        if (alpha < 0) alpha = 1.0f;
                        if (red < 0) red = 1.0f;
                        if (green < 0) green = 1.0f;
                        if (blue < 0) blue = 1.0f;
                        break;

                    case OP_ASSIGN:
                        if (alpha >= 0) alpha *= 0xff;
                        if (red >= 0) red *= 0xff;
                        if (green >= 0) green *= 0xff;
                        if (blue >= 0) blue *= 0xff;
                        break;

                    case OP_OVERLAY:
                        alpha = (alpha >= 0 ? alpha * 0xff : 1.0f);
                        red = (red >= 0 ? red * 0xff : 0.0f);
                        green = (green >= 0 ? green * 0xff : 0.0f);
                        blue = (blue >= 0 ? blue * 0xff : 0.0f);
                        //System.out.println("OP_OVERLAY: a=" + alpha + "; r=" + red + "; g=" + green + "; b=" + blue);

                        break;
                    }
                }
            }
        }

        /**
         * execute this instruction
         */
        public BufferedImage execute(BufferedImage lhs, Map<String, Object> namespace, ClassLoader loader)
           throws Exception
        {
            int x, y, z, w, h;
            int width, height;

            // positioning is centred
            boolean centreX = true, centreY = true;

            BufferedImage rhs = null;
            BufferedImage tmp;
            String ref;

            // calculate lhs dimensions
            if (lhs != null) {
                width = lhs.getWidth();
                height = lhs.getHeight();
            }
            else if (resize != null && opcode != OP_FEATHER) {
                width = resize.getInt(0);
                height = resize.getInt(1);
            }
            else if (imageWidth > 0 && imageHeight > 0) {
                width = imageWidth;
                height = imageHeight;
            }
            else {
                width = -1;
                height = -1;
            }

            // does target resolve to an image?
            if (target != null) {
                if (target.equals(DEFAULT_TARGET)) rhs = lhs;
                else rhs = getImage(target, width, height, namespace, loader);
            }

            // if LHS dimensions are still unknown, try defaulting to RHS
            if (width < 0 && height < 0) {
                if (rhs != null) {
                    width = rhs.getWidth();
                    height = rhs.getHeight();
                }
                else throw new Exception("IconGenerator: cannot determine initial image size. Source: " + source.substring(start, end));
            }

            // invoke any method calls
            if (opcode == OP_CALL) {
                //System.out.println("resolving: " + method);
                Object obj;
                if (target.equals(DEFAULT_TARGET))
                    obj = lhs;
                else
                    obj = resolve(target, method, namespace);

                //System.out.println("...to " + obj);
                if (obj != null) {
                    if (obj instanceof Color) background = (Color) obj;
                    else if (rhs == null && obj instanceof BufferedImage) rhs = (BufferedImage) obj;
                    else throw new Exception("Unusable result of call (" + obj.getClass().getName() + "): " + obj.toString());
                }
            }

            // create any implied image
            if (rhs == null) {

                Color color = null;
                if (background != null) color = background;
                else if (red >= 0 || green >= 0 || blue >=0) {

                    if (alpha < 0) alpha = 1.0f;
                    if (red < 0) red = 0.0f;
                    if (green < 0) green = 0.0f;
                    if (blue < 0) blue = 0.0f;

                    color = new Color(red, blue, green, alpha);
                }

                if (color != null) {
                    rhs = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
                    Graphics2D overlay = (Graphics2D) rhs.getGraphics();
                    overlay.setColor(color);
                    overlay.fillRect(0, 0, width, height);

                    overlay.dispose();
                }
            }

            Graphics2D graphics = (Graphics2D) (lhs != null ? lhs.getGraphics() : null);

            // centre on lhs to start with
            x = width/2;
            y = height/2;

            if (rhs != null) {
                w = rhs.getWidth();
                h = rhs.getHeight();
            }
            else {
                w = width;
                h = height;
            }

            // calculate offset of RHS
            if (offset != null) {
                if (offset.length > 0) {
                    x = offset.calc(x, 0);
                    if (offset.opcode[0] == OP_ASSIGN) centreX = false;
                }

                if (offset.length > 1) {
                    y = offset.calc(y, 1);
                    if (offset.opcode[1] == OP_ASSIGN) centreY = false;
                }
            }

            // calculate size of RHS
            if (resize != null && opcode != OP_FEATHER) {

                if (resize.length > 0) {
                    ref = resize.ref[0];
                    if (ref != null) {
                        tmp = getImage(ref, -1, -1, namespace, loader);
                        w = tmp.getWidth();
                    }

                    w = resize.calc(w, 0);
                }

                if (resize.length > 1) {
                    ref = resize.ref[1];
                    if (ref != null) {
                        tmp = getImage(ref, -1, -1, namespace, loader);
                        h = tmp.getHeight();
                    }

                    h = resize.calc(h, 1);
                }

                // calculate x and y coordinates
                if (centreX) x -= w/2;
                if (centreY) y -= h/2;

                // if we are overlaying, set the clipping area
                if (rhs != null && graphics != null)
                    graphics.setClip(x, y, w, h);
            }
            else {
                // calculate x and y coordinates
                if (centreX) x -= w/2;
                if (centreY) y -= h/2;
            }

            // apply a colour
            if (rhs != null && (alpha >= 0 || red >= 0 || green >= 0 || blue >= 0)) {

                Graphics2D overlay;

                int ired = (int) red;
                int igreen = (int) green;
                int iblue = (int) blue;

                int p, a, r, g, b;
                int pix[] = new int[w];
                int i, j;

                // if we are editing in place, create a clipping rectangle
                Rectangle clip = (rhs == lhs ? new Rectangle(x, y, w, h) : null);

                switch (opcode) {
                case OP_ADD:
                    // add
                    add(rhs, ired, igreen, iblue, clip);
                    break;

                case OP_SUBTRACT:
                    // subtract
                    subtract(rhs, ired, igreen, iblue, clip);
                    break;

                case OP_MULTIPLY:
                    // multiply
                    multiply(rhs, alpha, red, green, blue, clip);
                    break;

                case OP_ASSIGN:
                    // assign (set values)
                    assign(rhs, (int) alpha, ired, igreen, iblue, clip);
                    break;

                case OP_OVERLAY:
                    // overlay RHS onto colour
                    overlay(rhs, (int) alpha, ired, igreen, iblue, clip);
                    break;
                }
            }

            // apply feathering to rhs
            if (rhs != null && opcode == OP_FEATHER) {

                // antialias
                if (antialias != null && antialias.length > 0) {
                    float a = antialias.getFloat(0);
                    float atten;

                    if (antialias.length > 1) atten = antialias.getFloat(1);
                    else atten = -1;
                    antialias(rhs, a, atten, null);
                }


                /*
                // overlay
                else if (red >= 0 || green >= 0 || blue >= 0) {
                    overlay(rhs, (int) alpha, (int) red, (int) green, (int) blue);
                }
                */
                else if (resize != null && resize.length > 0) {
                    int lr = 0;
                    lr = resize.calc(lr, 0);
                    int tb = 0;
                    tb = resize.calc(tb, 1);

                    byte dir = (lr < 0 || tb < 0
                                ? FEATHER_IN_DIR : FEATHER_OUT_DIR);

                    if (lr < 0) lr = -lr;
                    if (tb < 0) tb = -tb;

                    feather(rhs, lr, tb, dir, null);
                }
            }

            // resize AND feather
            if (rhs != null && resize != null && resize.length > 2 && resize.opcode[2] == OP_FEATHER) {
                int lr = resize.getInt(2);
                int tb = (resize.length > 3 ? resize.getInt(3) : lr);

                feather(rhs, lr, tb, FEATHER_OUT_DIR, new Rectangle(0, 0, w, h));
            }

            // apply z-value to rhs
            if (offset != null && opcode != OP_POSITION && rhs != null) {
                z = offset.getInt(2);
                if (z != 0) bevel3D(rhs, z, new Rectangle(0, 0, w, h));
            }

            // apply rhs to lhs
            if (lhs == null) lhs = copy(rhs, width, height, 0); // assign

            else if (rhs != null && rhs != lhs) {
                graphics.setComposite(AlphaComposite.SrcOver);

                //if (scale == 0)
                    graphics.drawImage(rhs, x, y, null);

                    /*
                else {
                    if (scale > 0) {
                        w = (int) (width * scale);
                        h = (int) (height * scale);
                    }
                    else if (scale < 0) {
                        w = (int) (width + scale);
                        h = (int) (height + scale);
                    }

                    if (centreX) x = (width-w)/2;
                    if (centreY) y = (height-h)/2;

                    graphics.drawImage(rhs, x, y, w, h, null);
                }
                    */
            }

            // resize the result
            if (opcode == OP_RESIZE && rhs == null && lhs != null && (w != width || h != height)) {

                // scale with the resize?
                float factor = (resize.length > 2 && resize.opcode[2] == OP_SCALE ? resize.getFloat(2) : 0);

                lhs = copy(lhs, w, h, factor);
            }

            // apply z-value (3D effect) to result
            if (opcode == OP_POSITION && lhs != null && offset != null && offset.length > 2) {
                z = offset.getInt(2);
                if (z != 0) bevel3D(lhs, z);
            }

            if (graphics != null) graphics.dispose();

            // execute next instruction, or return result
            return (next != null ? next.execute(lhs, namespace, loader) : lhs);
        }

        protected void error(String message, int pos)
            throws Exception
        {
            int first = Math.max(0, pos-10);
            int last = Math.min(source.length(), pos+10);
            int max = dots.length()-1;
            String intro = "\nsource: ";
            message += intro + source.substring(first, last) + "\n" + dots.substring(0, Math.min(max, intro.length()+first)) + "^";

            throw new Exception(message);
        }

        protected ArgList parseArgs(ArgList vec, int maxArgs, int first, int last)
        {
            if (vec == null) vec = new ArgList();
            vec.parse(maxArgs, source, first, last);

            return vec;
        }

        protected BufferedImage getImage(String name, int width, int height, Map<String, Object> namespace, ClassLoader loader)
           throws IOException
        {
            Object obj = namespace.get(name);

            // resolve reference
            if (obj != null && obj.getClass() == String.class
                && delims[ARG_DELIMS].indexOf(name.charAt(0)) == ARG_RESOLVE) {
                name = obj.toString();
                obj = namespace.get(name);
            }

            // something there, but it's not an image
            if (obj != null && !(obj instanceof Image)) return null;

            int w = 0;
            int h = 0;

            // not found, go looking
            if (obj == null) {
                if (Character.isLetter(name.charAt(0))) {
                    URL url = loader.getResource(name);
                    if (url == null) url = loader.getResource(name + ".png");
                    if (url == null) url = loader.getResource(name + ".gif");

                    if (url != null) {
                        ImageIcon icon = new ImageIcon(url);
                        w = icon.getIconWidth();
                        h = icon.getIconHeight();

                        obj = icon.getImage();
                    }
                }

                //if (obj == null) return null;
                if (obj == null) throw new FileNotFoundException(name);
            }
            else {
                Image img = (Image) obj;
                w = img.getWidth(null);
                h = img.getHeight(null);
            }

            namespace.put(name, obj);

            // make a copy
            /*
            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics gr = tmp.getGraphics();
            gr.drawImage((Image) obj, 0, 0, null);
            gr.dispose();
            obj = tmp;

            return (BufferedImage) obj;
            */

            return copy((Image) obj, width, height, scale);
        }

        protected Object resolve(String target, List method, Map namespace)
           throws Exception
        {
            Object obj = namespace.get(target);

            // resolve reference
            if (obj != null && obj.getClass() == String.class
                && delims[ARG_DELIMS].indexOf(target.toString().charAt(0)) == ARG_RESOLVE) {
                target = obj.toString();
                obj = namespace.get(target);
            }

            if (obj == null) return obj;

            try {
                Method meth;
                String methName;
                int cut;
                for (int i = 0; i < method.size(); i++) {
                    methName = method.get(i).toString();
                    cut = methName.indexOf('(');
                    if (cut > 0) methName = methName.substring(0, cut);
                    meth = obj.getClass().getMethod(methName, NULL_SIG);
                    obj = meth.invoke(obj, NULL_ARGS);
                }
                return obj;
            } catch (Exception e) {
                throw new Exception("Could not resolve call: " + target + "." + method + "\n" + e);
            }
        }

        protected int parseToken(String macro, int pos)
        {
            int len = macro.length();
            if (pos < 0 || pos >= len) return -1;

            // skip trailing quotes
            if (QUOTES.indexOf(macro.charAt(pos)) >= 0) pos++;

            if (pos >= len) return -1;

            String ops = delims[OPS];
            String openclose = delims[OPEN_CLOSE];

            char c = macro.charAt(pos);

            //if (ops.indexOf(c) >= 0) return pos;
            switch (c) {

            //case '(':
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                // skip whitespace and nested parens
                //while (macro.charAt(++pos) == ' ');
                while (++pos < len && " \t\r\n".indexOf(macro.charAt(pos)) >= 0);

                // and dequote
                return (QUOTES.indexOf(macro.charAt(pos)) >= 0 ? pos + 1 : pos);

            case ')':
            case ']':
            case '}':
                // skip nested parens
                while (++pos < len && " )]}\t\r\n".indexOf(macro.charAt(pos)) >= 0);
                return (pos < len ? pos : -1);

            default:
                // quoted string?
                if (pos > 0 && QUOTES.indexOf(macro.charAt(pos - 1)) >= 0) {
                    return macro.indexOf(c, pos);                        // close quote
                }

                    // word token
                else {
                    while (pos < len && ops.indexOf(macro.charAt(pos)) < 0 && (openclose.indexOf(macro.charAt(pos)) % 2) != 1) pos++;
                    return (pos < len ? pos : len);
                }
            }
        }

        protected int skip(String macro, int start, int end, String skipchars)
        {
            while (start < end && skipchars.indexOf(macro.charAt(start)) >= 0) start++;
            return start;
        }

        protected int find(String macro, int start, int end, String findchars)
        {
            while (start < end && findchars.indexOf(macro.charAt(start)) < 0) start++;
            return start;
        }

        protected class ArgList
        {
            int length = 0;
            byte[] opcode = null;
            String[] ref = null;
            String[] value = null;

            /**
             *  parse arguments for an operator into this ArgList
             *
             *  @param maxArgs maximum args to be stored in this list
             *  @param macro the String to parse from
             *  @param start the position in macro to start parsing
             *  @param end the position in macro to end parsing
             *
             * TODO: parse ref values.
             */
            protected void parse(int maxArgs, String macro, int start, int end)
            {
                if (opcode == null || opcode.length < maxArgs) {
                    opcode = new byte[maxArgs];
                    ref = new String[maxArgs];
                    value = new String[maxArgs];
                }

                byte opval;
                char listDelim = delims[ARG_DELIMS].charAt(ARG_LIST);
                int pos1 = start, pos2, len;
                int index = 0, max=0;

                for (int i = 0; pos1 < end; i++) {

                    index = i;

                    pos2 = macro.indexOf(listDelim, pos1);
                    if (pos2 < 0 || pos2 > end) pos2 = end;

                    if (pos2-pos1 > 1 && "+-=".indexOf(macro.charAt(pos1+1)) >= 0) {

                        switch (macro.charAt(pos1)) {
                        case 'x':
                        case 'X':
                        case 'a':
                        case 'A':
                            index = 0;
                            pos1++;
                            break;

                        case 'y':
                        case 'Y':
                        case 'r':
                        case 'R':
                            index = 1;
                            pos1++;
                            break;

                        case 'z':
                        case 'Z':
                        case 'g':
                        case 'G':
                            index = 2;
                            pos1++;
                            break;

                        case 'b':
                        case 'B':
                            index = 3;
                            pos1++;
                            break;
                        }
                    }

                    if (pos2 > pos1) {
                        opval = (byte) (delims[OPS].indexOf(macro.charAt(pos1))+1);
                        switch (opval) {
                        case OP_SUBTRACT:
                        case OP_ADD:
                        case OP_ASSIGN:
                        case OP_FEATHER:
                        case OP_SCALE:
                            pos1++;
                            opcode[index] = opval;
                            break;

                        default:
                            opcode[index] = OP_ASSIGN;
                        }
                    }

                    if (pos2 > pos1) value[index] = macro.substring(pos1, pos2).trim();
                    else value[index] = macro.substring(pos1).trim();

                    length = Math.max(index+1, length);

                    // advance past delimiter
                    pos1 = pos2+1;
                }
            }

            protected int getInt(int index)
            { return calc(0, index); }

            protected int getFloat(int index)
            { return calc(0, index); }

            protected int calc(int lhs, int index)
            {
                byte op = opcode[(index < length ? index : 0)];
                String val = (index < length ? value[index] : null);
                float rhs = (val != null ? Float.parseFloat(val) : 0.0f);

                switch (op) {
                case OP_ADD:
                    return (int) (lhs + rhs);

                case OP_SUBTRACT:
                    return (int) (lhs - rhs);

                case OP_MULTIPLY:
                    return (int) (lhs * rhs);

                case OP_ASSIGN:
                case OP_FEATHER:
                    return (int) rhs;

                default:
                    return lhs;
                }
            }

            protected float calc(float lhs, int index)
            {
                byte op = opcode[(index < length ? index : 0)];
                String val = (index < length ? value[index] : null);
                float rhs = (val != null ? Float.parseFloat(val) : 0.0f);

                switch (op) {
                case OP_ADD:
                    return lhs + rhs;

                case OP_SUBTRACT:
                    return lhs - rhs;

                case OP_MULTIPLY:
                    return lhs * rhs;

                case OP_ASSIGN:
                case OP_FEATHER:
                    return rhs;

                default:
                    return lhs;
                }
            }
        }
    }
}

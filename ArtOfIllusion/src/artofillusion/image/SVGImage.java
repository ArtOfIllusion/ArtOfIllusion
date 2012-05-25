/* Copyright (C) 2011-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.*;
import artofillusion.math.*;
import com.kitfox.svg.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.util.*;

public class SVGImage extends ImageMap
{
  private final byte xml[];
  private SVGDiagram svg;
  private BufferedImage preview;
  private HashMap<TileKey, SoftReference<int[]>> tiles;
  private float average[];

  private static final float SCALE = 1.0f/255.0f;
  private static final int TILE_SIZE = 64;

  /**
   * Construct a SVGImage from a SVG file.
   */

  public SVGImage(File file) throws IOException, InterruptedException, SVGException
  {
    xml = ArtOfIllusion.loadFile(file).getBytes("UTF-8");
    initialize();
  }

  private void initialize() throws SVGException
  {
    SVGUniverse universe = new SVGUniverse();
    URI uri = universe.loadSVG(new InputStreamReader(new ByteArrayInputStream(xml)), "image");
    svg = universe.getDiagram(uri);
    svg.setIgnoringClipHeuristic(true);
    tiles = new HashMap<TileKey, SoftReference<int[]>>();

    // Create the preview image.

    float aspectRatio = svg.getWidth()/svg.getHeight();
    int previewWidth, previewHeight;
    if (aspectRatio >= 1)
    {
      previewWidth = PREVIEW_WIDTH;
      previewHeight = (int) (PREVIEW_WIDTH/aspectRatio);
    }
    else
    {
      previewWidth = (int) (PREVIEW_HEIGHT*aspectRatio);
      previewHeight = PREVIEW_HEIGHT;
    }
    preview = new BufferedImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = preview.createGraphics();
    g.setClip(0, 0, (int) svg.getWidth(), (int) svg.getHeight());
    g.setTransform(new AffineTransform(previewWidth/svg.getWidth(), 0, 0, previewHeight/svg.getHeight(), 0, 0));
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    svg.render(g);
    g.dispose();

    // Compute the average components based on the preview image.

    average = new float[4];
    for (int i = 0; i < previewWidth; i++)
      for (int j = 0; j < previewHeight; j++)
      {
        int argb = preview.getRGB(i, j);
        average[0] += argb&0xFF;
        average[1] += (argb>>8)&0xFF;
        average[2] += (argb>>16)&0xFF;
        average[3] += (argb>>24)&0xFF;
      }
    for (int i = 0; i < 4; i++)
      average[i] /= 255.0f*previewWidth*previewHeight;
    average[3] = 1-average[3];
  }

  private BufferedImage createImage(int x, int y, int scale) throws SVGException
  {
    BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setClip(0, 0, (int) svg.getWidth(), (int) svg.getHeight());
    g.setTransform(new AffineTransform(scale/svg.getWidth(), 0, 0, -scale/svg.getHeight(), -x, scale-y));
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    svg.render(g);
    g.dispose();
    return image;
  }

  private int[] getTile(int x, int y, int scale)
  {
    TileKey key = new TileKey();
    key.x = x;
    key.y = y;
    key.scale = scale;
    int tile[] = null;
    synchronized (this)
    {
      SoftReference<int[]> ref = tiles.get(key);
      if (ref != null)
        tile = ref.get();
      if (tile == null)
      {
        // Create the tile;

        try
        {
          BufferedImage image = createImage(x, y, scale);
          tile = ((DataBufferInt) ((BufferedImage) image).getRaster().getDataBuffer()).getData();
        }
        catch (SVGException ex)
        {
          ex.printStackTrace();
          tile = new int[TILE_SIZE*TILE_SIZE];
        }
        tiles.put(key.clone(), new SoftReference<int[]>(tile));
      }
    }
    return tile;
  }

  /** Get the width of the image. */

  public int getWidth()
  {
    return (int) svg.getWidth();
  }

  /** Get the height of the image. */

  public int getHeight()
  {
    return (int) svg.getHeight();
  }

  /** Get the number of components in the image. */

  public int getComponentCount()
  {
    return 4;
  }

  private void getPixels(int x, int y, int scale, boolean wrapx, boolean wrapy, int values[])
  {
    int tile1[], tile2[], tile3[], tile4[];
    int dx = x&(TILE_SIZE-1), dy = y&(TILE_SIZE-1);
    int basex = x-dx, basey = y-dy;
    int dx2 = (x+1)&(TILE_SIZE-1), dy2 = (y+1)&(TILE_SIZE-1);
    int basex2 = x+1-dx2, basey2 = y+1-dy2;
    if (x == scale-1)
    {
      if (wrapx)
      {
        dx2 = 0;
        basex2 = 0;
      }
      else
      {
        dx2 = dx;
        basex2 = basex;
      }
    }
    if (y == scale-1)
    {
      if (wrapy)
      {
        dy2 = 0;
        basey2 = 0;
      }
      else
      {
        dy2 = dy;
        basey2 = basey;
      }
    }
    tile1 = getTile(basex, basey, scale);
    if (basex == basex2 && basey == basey2)
      tile2 = tile3 = tile4 = tile1;
    else
    {
      tile2 = getTile(basex2, basey, scale);
      tile3 = getTile(basex, basey2, scale);
      tile4 = getTile(basex2, basey2, scale);
    }
    values[0] = tile1[dx+dy*TILE_SIZE];
    values[1] = tile2[dx2+dy*TILE_SIZE];
    values[2] = tile3[dx+dy2*TILE_SIZE];
    values[3] = tile4[dx2+dy2*TILE_SIZE];
  }

  /** Get the value of a single component at a particular location in the image.  The value
      is represented as a float between 0.0 and 1.0.  The components are:

      0: Red
      1: Green
      2: Blue
      3: Alpha

      The location is specified by x and y, which must lie between 0 and 1.  The value is
      averaged over a region of width (xsize, ysize).  wrapx and wrapy specify whether, for
      purposes of interpolation, the image should be treated as wrapping around so that
      opposite edges touch each other. */

  public float getComponent(int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    double size = (xsize > ysize ? xsize : ysize);
    if (size >= 1)
      return average[component];
    int shift = component*8;
    double invsize = 1/size;
    int scale = 2;
    while (scale < invsize)
      scale *= 2;
    int values[] = new int[4];
    float scaledx = (float) (x*scale);
    float scaledy = (float) (y*scale);
    getPixels((int) scaledx, (int) scaledy, scale, wrapx, wrapy, values);
    float scaleFract = 1-(float) (scale-invsize)/(0.5f*scale);
    float xfract = scaledx-(int) scaledx;
    float yfract = scaledy-(int) scaledy;
    getPixels((int) scaledx, (int) scaledy, scale, wrapx, wrapy, values);
    float result = scaleFract*SCALE*((1-xfract)*(1-yfract)*((values[0]>>shift)&0xFF) +
                   (xfract)*(1-yfract)*((values[1]>>shift)&0xFF) +
                   (1-xfract)*(yfract)*((values[2]>>shift)&0xFF) +
                   (xfract)*(yfract)*((values[3]>>shift)&0xFF));
    scaledx = (float) (0.5*x*scale);
    scaledy = (float) (0.5*y*scale);
    getPixels((int) scaledx, (int) scaledy, scale/2, wrapx, wrapy, values);
    scaleFract = 1-scaleFract;
    xfract = scaledx-(int) scaledx;
    yfract = scaledy-(int) scaledy;
    result += scaleFract*SCALE*((1-xfract)*(1-yfract)*((values[0]>>shift)&0xFF) +
                   (xfract)*(1-yfract)*((values[1]>>shift)&0xFF) +
                   (1-xfract)*(yfract)*((values[2]>>shift)&0xFF) +
                   (xfract)*(yfract)*((values[3]>>shift)&0xFF));
    if (component == 3)
      result = 1-result;
    return result;
  }

  /** Get the average value for a particular component, over the entire image. */

  public float getAverageComponent(int component)
  {
    return average[component];
  }

  /** Get the color at a particular location.  The location is specified by x and y,
      which must lie between 0 and 1.  The color is averaged over a region of width
      (xsize, ysize).  wrapx and wrapy specify whether, for purposes of interpolation, the
      image should be treated as wrapping around so that opposite edges touch each other. */

  public void getColor(RGBColor theColor, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    double size = (xsize > ysize ? xsize : ysize);
    if (size >= 1)
    {
      theColor.setRGB(average[0], average[1], average[2]);
      return;
    }
    double invsize = 1/size;
    int scale = 2;
    while (scale < invsize)
      scale *= 2;
    int values[] = new int[4];
    float scaledx = (float) (x*scale);
    float scaledy = (float) (y*scale);
    getPixels((int) scaledx, (int) scaledy, scale, wrapx, wrapy, values);
    float scaleFract = 1-(float) ((scale-invsize)/(0.5f*scale));
    float xfract = scaledx-(int) scaledx;
    float yfract = scaledy-(int) scaledy;
    getPixels((int) scaledx, (int) scaledy, scale, wrapx, wrapy, values);
    theColor.setRGB(scaleFract*SCALE*((1-xfract)*(1-yfract)*((values[0]>>16)&0xFF) +
                                      (xfract)*(1-yfract)*((values[1]>>16)&0xFF) +
                                      (1-xfract)*(yfract)*((values[2]>>16)&0xFF) +
                                      (xfract)*(yfract)*((values[3]>>16)&0xFF)),
                    scaleFract*SCALE*((1-xfract)*(1-yfract)*((values[0]>>8)&0xFF) +
                                      (xfract)*(1-yfract)*((values[1]>>8)&0xFF) +
                                      (1-xfract)*(yfract)*((values[2]>>8)&0xFF) +
                                      (xfract)*(yfract)*((values[3]>>8)&0xFF)),
                    scaleFract*SCALE*((1-xfract)*(1-yfract)*(values[0]&0xFF) +
                                      (xfract)*(1-yfract)*(values[1]&0xFF) +
                                      (1-xfract)*(yfract)*(values[2]&0xFF) +
                                      (xfract)*(yfract)*(values[3]&0xFF)));
    scaledx = (float) (0.5*x*scale);
    scaledy = (float) (0.5*y*scale);
    getPixels((int) scaledx, (int) scaledy, scale/2, wrapx, wrapy, values);
    scaleFract = 1-scaleFract;
    xfract = scaledx-(int) scaledx;
    yfract = scaledy-(int) scaledy;
    theColor.add(scaleFract*SCALE*((1-xfract)*(1-yfract)*((values[0]>>16)&0xFF) +
                                   (xfract)*(1-yfract)*((values[1]>>16)&0xFF) +
                                   (1-xfract)*(yfract)*((values[2]>>16)&0xFF) +
                                   (xfract)*(yfract)*((values[3]>>16)&0xFF)),
                 scaleFract*SCALE*((1-xfract)*(1-yfract)*((values[0]>>8)&0xFF) +
                                   (xfract)*(1-yfract)*((values[1]>>8)&0xFF) +
                                   (1-xfract)*(yfract)*((values[2]>>8)&0xFF) +
                                   (xfract)*(yfract)*((values[3]>>8)&0xFF)),
                 scaleFract*SCALE*((1-xfract)*(1-yfract)*(values[0]&0xFF) +
                                   (xfract)*(1-yfract)*(values[1]&0xFF) +
                                   (1-xfract)*(yfract)*(values[2]&0xFF) +
                                   (xfract)*(yfract)*(values[3]&0xFF)));
  }

  /** Get the gradient of a single component at a particular location in the image.
      The location is specified by x and y, which must lie between 0 and 1.  The value is
      averaged over a region of width (xsize, ysize) before the gradient is calculated.
      wrapx and wrapy specify whether, for purposes of interpolation, the image should be
      treated as wrapping around so that opposite edges touch each other. */

  public void getGradient(Vec2 grad, int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    double size = (xsize > ysize ? xsize : ysize);
    if (size >= 1)
    {
      grad.set(0.0, 0.0);
      return;
    }
    int shift = component*8;
    double invsize = 1/size;
    int scale = 2;
    while (scale < invsize)
      scale *= 2;
    int values[] = new int[4];
    float scaledx = (float) (x*scale);
    float scaledy = (float) (y*scale);
    getPixels((int) scaledx, (int) scaledy, scale, wrapx, wrapy, values);
    float scaleFract = 1-(float) (scale-invsize)/(0.5f*scale);
    float xfract = scaledx-(int) scaledx;
    float yfract = scaledy-(int) scaledy;
    getPixels((int) scaledx, (int) scaledy, scale, wrapx, wrapy, values);
    grad.set(scaleFract*scale*SCALE*((1-yfract)*((values[1]>>shift)&0xFF)-((values[0]>>shift)&0xFF) +
                                     (yfract)*((values[3]>>shift)&0xFF)-((values[2]>>shift)&0xFF)),
             scaleFract*scale*SCALE*((1-xfract)*((values[2]>>shift)&0xFF)-((values[0]>>shift)&0xFF) +
                                     (xfract)*((values[3]>>shift)&0xFF)-((values[1]>>shift)&0xFF)));
    scaledx = (float) (0.5*x*scale);
    scaledy = (float) (0.5*y*scale);
    getPixels((int) scaledx, (int) scaledy, scale/2, wrapx, wrapy, values);
    scaleFract = 1-scaleFract;
    xfract = scaledx-(int) scaledx;
    yfract = scaledy-(int) scaledy;
    grad.x += scaleFract*scale*SCALE*((1-yfract)*((values[1]>>shift)&0xFF)-((values[0]>>shift)&0xFF) +
                                      (yfract)*((values[3]>>shift)&0xFF)-((values[2]>>shift)&0xFF));
    grad.y += scaleFract*scale*SCALE*((1-xfract)*((values[2]>>shift)&0xFF)-((values[0]>>shift)&0xFF) +
                                      (xfract)*((values[3]>>shift)&0xFF)-((values[1]>>shift)&0xFF));
    if (component == 3)
    {
      grad.x = -grad.x;
      grad.y = -grad.y;
    }
  }

  /** Get a scaled down copy of the image, to use for previews.  This Image will be no larger
      (but may be smaller) than PREVIEW_WIDTH by PREVIEW_HEIGHT. */

  public Image getPreview()
  {
    return preview;
  }

  /** Reconstruct an image from its serialized representation. */

  public SVGImage(DataInputStream in) throws IOException, SVGException
  {
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("Illegal version for SVGImage");
    xml = new byte[in.readInt()];
    in.readFully(xml);
    initialize();
  }

  /** Serialize an image to an output stream. */

  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeShort(0);
    out.writeInt(xml.length);
    out.write(xml);
  }

  private static class TileKey implements Cloneable
  {
    public int x, y, scale;

    @Override
    public boolean equals(Object o)
    {
      if (!(o instanceof TileKey))
        return false;
      TileKey other = (TileKey) o;
      return (other.x == x && other.y == y && other.scale == scale);
    }

    @Override
    public int hashCode()
    {
      return x+(y<<10)+(scale<<20);
    }

    @Override
    public TileKey clone()
    {
      TileKey copy = new TileKey();
      copy.x = x;
      copy.y = y;
      copy.scale = scale;
      return copy;
    }
  }
}

/* Copyright (C) 1999-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import ch.randelshofer.media.quicktime.*;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import javax.imageio.*;
import javax.imageio.stream.*;

/** This class is used to save rendered images to disk. */

public class ImageSaver
{
  private int format, index;
  private String name, directory;
  private boolean ok, premultiply;
  private double quality;
  private BFrame parent;
  private QuickTimeWriter qt;

  public static final int FORMAT_JPEG = 0;
  public static final int FORMAT_TIFF = 1;
  public static final int FORMAT_PNG = 2;
  public static final int FORMAT_BMP = 3;
  public static final int FORMAT_HDR = 4;
  public static final int FORMAT_QUICKTIME = 5;
  
  private static final String FORMAT_NAME[] = new String [] {
    "JPEG", "TIFF", "PNG", "BMP", "HDR", "Quicktime"
  };
  private static final String FORMAT_EXTENSION[] = new String [] {
    "jpg", "tif", "png", "bmp", "hdr", "mov"
  };
  
  private static boolean premultiplyDefault = true;
  private static double qualityDefault = 90.0;
  private static int lastImageFormat = FORMAT_JPEG;
  private static int lastMovieFormat = FORMAT_QUICKTIME;
  
  /** Create an ImageSaver object which will be used for saving a single images.
      The constructor displays a dialog in which the user can select the name, location,
      and format.  The saveImage() method can then be used to save individual frames of the
      animation.  Use clickedOk() to determine whether the user clicked the OK or Cancel
      button. */

  public ImageSaver(BFrame parent)
  {
    init(parent, Integer.MIN_VALUE);
  }

  /** Create an ImageSaver object which will be used for saving an animation.
      The constructor displays a dialog in which the user can select the name, location,
      and format.  The saveImage() method can then be used to save individual frames of the
      animation, and lastMovieImage() should be called once all frames have been saved.
      Use clickedOk() to determine whether the user clicked the OK or Cancel button.
      @param parent             the parent window
      @param width              the image width, in pixels
      @param height             the image height, in pixels
      @param fps                the number of frames per second
      @param startFrameNumber   the default number for the first frame of the animation
  */

  public ImageSaver(BFrame parent, int width, int height, int fps, int startFrameNumber) throws IOException
  {
    init(parent, startFrameNumber);
    if (format == FORMAT_QUICKTIME && this.clickedOk())
    {
      qt = new QuickTimeWriter(new File(directory, name));
      qt.addVideoTrack(QuickTimeWriter.VideoFormat.JPG, fps, width, height);
      qt.setCompressionQuality(0, (float) quality*0.01f);
    }
  }

  /** Initialize the ImageSaver and display the dialog.
      @param parent             the parent window
      @param startFrameNumber   the default number for the first frame of an animation, or Integer.MIN_VALUE for a single image
  */

  private void init(BFrame parent, int startFrameNumber)
  {
    this.parent = parent;
    boolean animate = (startFrameNumber != Integer.MIN_VALUE);
    index = startFrameNumber;

    final BComboBox formatChoice = new BComboBox();
    for (int i = 0; i < FORMAT_NAME.length; i++)
      formatChoice.add(FORMAT_NAME[i]);
    formatChoice.setSelectedValue(FORMAT_NAME[animate ? lastMovieFormat : lastImageFormat]);
    BCheckBox premultBox = new BCheckBox("Premultiply Transparency", premultiplyDefault);
    final ValueSlider qualitySlider = new ValueSlider(0.0, 100.0, 100, qualityDefault);
    final OverlayContainer optionsPanel = new OverlayContainer();
    final RowContainer startFramePanel = new RowContainer();
    ValueField numberFromField = new ValueField(startFrameNumber, ValueField.INTEGER);
    formatChoice.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        Object value = formatChoice.getSelectedValue();
        if (FORMAT_NAME[FORMAT_JPEG].equals(value) || FORMAT_NAME[FORMAT_QUICKTIME].equals(value))
          optionsPanel.setVisibleChild(0);
        else if (FORMAT_NAME[FORMAT_TIFF].equals(value))
          optionsPanel.setVisibleChild(1);
        else
          optionsPanel.setVisibleChild(2);
        startFramePanel.setVisible(!FORMAT_NAME[FORMAT_QUICKTIME].equals(value));
      }
    });
    FormContainer mainPanel = new FormContainer(1, 3);
    mainPanel.add(formatChoice, 0, 0, new LayoutInfo());
    mainPanel.add(optionsPanel, 0, 1);
    if (animate)
      mainPanel.add(startFramePanel, 0, 2);
    RowContainer jpegRow = new RowContainer();
    jpegRow.add(Translate.label("Quality"));
    jpegRow.add(qualitySlider);
    optionsPanel.add(jpegRow, 0);
    optionsPanel.add(premultBox, 1);
    optionsPanel.add(new BLabel(), 2);
    premultBox.setEnabled(true);
    startFramePanel.add(Translate.label("numberFramesFrom"));
    startFramePanel.add(numberFromField);
    formatChoice.dispatchEvent(new ValueChangedEvent(formatChoice));
    PanelDialog dlg = new PanelDialog(parent, Translate.text("selectFileFormat"), mainPanel);
    if (dlg.clickedOk())
    {
      premultiply = premultiplyDefault = premultBox.getState();
      quality = qualityDefault = qualitySlider.getValue();
      Object formatValue = formatChoice.getSelectedValue();
      for (int i = 0; i < FORMAT_NAME.length; i++)
        if (FORMAT_NAME[i].equals(formatValue))
          format = i;
      if (animate)
        lastMovieFormat = format;
      else
        lastImageFormat = format;
      index = (int) numberFromField.getValue();
      BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("saveImage"));
      String filename = "Untitled."+FORMAT_EXTENSION[format];
      File file = (ArtOfIllusion.getCurrentDirectory() == null ? new File(filename) : new File(ArtOfIllusion.getCurrentDirectory(), filename));
      fc.setSelectedFile(file);
      ok = fc.showDialog(parent);
      if (ok)
      {
        file = fc.getSelectedFile();
        name = file.getName();
        directory = file.getParentFile().getAbsolutePath();
        if (file.isFile())
        {
          String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
          int choice = new BStandardDialog("", Translate.text("overwriteFile", file.getName()), BStandardDialog.QUESTION).showOptionDialog(parent, options, options[1]);
          if (choice == 1)
            ok = false;
        }
      }
    }
  }

  /** Determine whether the user canceled saving the image. */
  
  public boolean clickedOk()
  {
    return ok;
  }
  
  /** Save the next image to disk.  Returns false if an error occurs.*/
  
  public boolean saveImage(Image im) throws IOException
  {
    return saveImage(new ComplexImage(im));
  }
  
  /** Save the next image to disk.  Returns false if an error occurs.*/
  
  public boolean saveImage(ComplexImage img) throws IOException
  {
    String filename = name;
    if (qt != null)
    {
      BufferedImage buffer = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = buffer.createGraphics();
      g.drawImage(img.getImage(), 0, 0, parent.getComponent());
      g.dispose();
      qt.writeFrame(0, buffer, 1);
      return true;
    }
    if (index != Integer.MIN_VALUE)
    {
      // Insert the image number into the filename.
      
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMinimumIntegerDigits(4);
      nf.setGroupingUsed(false);
      int i = name.lastIndexOf('.');
      if (i == -1 || i == (name.length()-1))
        filename += nf.format(index++);
      else
        filename = name.substring(0, i)+nf.format(index++)+name.substring(i);
    }
    try
    {
      if (format == FORMAT_TIFF && premultiply)
        img = new ComplexImage(premultiplyTransparency(img.getImage()));
      return saveImage(img, new File(directory, filename), format, (int) quality);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      new BStandardDialog("", Translate.text("errorSavingFile", ex.getMessage() == null ? "" : ex.getMessage()), BStandardDialog.ERROR).showMessageDialog(parent);
    }
    return false;
  }
  
  /** Save an image to disk in the specified format.  Returns true if the image was
      successfully saved, false if an error occurred.  For JPEG, quality should be
      between 0 and 100.  For other formats, it is ignored. */
  
  public static boolean saveImage(Image im, File f, int format, int quality) throws IOException, InterruptedException
  {
    return saveImage(new ComplexImage(im), f, format, quality);
  }
  
  /** Save an image to disk in the specified format.  Returns true if the image was
      successfully saved, false if an error occurred.  For JPEG, quality should be
      between 0 and 100.  For other formats, it is ignored. */
  
  public static boolean saveImage(ComplexImage img, File f, int format, int quality) throws IOException, InterruptedException
  {
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
    Image im = img.getImage();

    if (format == FORMAT_JPEG)
    {
      im = premultiplyTransparency(im);
      writeJpegToStream(im, bos, quality);
      bos.close();
      return true;
    }
    if (format == FORMAT_TIFF)
    {
      DataOutputStream dos = new DataOutputStream(bos);
      TIFFEncoder tiff = new TIFFEncoder(im);
      tiff.writeImage(dos);
      dos.close();
      return true;
    }
    if (format == FORMAT_PNG)
    {
      ImageIO.write(getBufferedImage(im, true), "png", bos);
      bos.close();
      return true;
    }
    if (format == FORMAT_BMP)
    {
      im = premultiplyTransparency(im);
      DataOutputStream dos = new DataOutputStream(bos);
      BMPEncoder bmp = new BMPEncoder(im);
      bmp.writeImage(dos);
      dos.close();
      return true;
    }
    if (format == FORMAT_HDR)
    {
      HDREncoder.writeImage(img, bos);
      bos.close();
      return true;
    }
    bos.close();
    return false;
  }

  /**
   * Write an image to a stream in JPEG format.
   */

  private static void writeJpegToStream(Image im, OutputStream out, int quality) throws IOException
  {
    ImageWriter writer = (ImageWriter) ImageIO.getImageWritersBySuffix("jpeg").next();
    ImageOutputStream ios = ImageIO.createImageOutputStream(out);
    writer.setOutput(ios);
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(quality/100.0f);
    writer.write(null, new IIOImage(getBufferedImage(im, false), null, null), param);
  }

  /** This should be called after the last frame of an animation has been saved. */

  public void lastMovieImage() throws IOException
  {
    if (qt != null)
      qt.close();
  }
  
  /** Determine whether this image is partially transparent, and if so, create a new image
      in which the color components are premultiplied by the transparency. */
  
  private static Image premultiplyTransparency(Image im)
  {
    int i, data[];
    
    try
    {
      PixelGrabber pg = new PixelGrabber(im, 0, 0, -1, -1, true);
      pg.grabPixels();
      data = (int []) pg.getPixels();
    }
    catch (InterruptedException ex)
    {
      return im;
    }
    for (i = 0; i < data.length && (data[i] & 0xFF000000) == 0xFF000000; i++);
    if (i == data.length)
      return im;
    
    // The image is partially transparent.  Premultiply by the transparency.
    
    for (i = 0; i < data.length; i++)
    {
      int alpha = (data[i]>>24) & 0xFF;
      int red = (data[i]>>16) & 0xFF;
      int green = (data[i]>>8) & 0xFF;
      int blue = data[i] & 0xFF;
      red = (red*(alpha+1))>>8;
      green = (green*(alpha+1))>>8;
      blue = (blue*(alpha+1))>>8;
      data[i] = (alpha<<24) + (red<<16) + (green<<8) + blue;
    }
    
    // Create the new image.
    
    MemoryImageSource source = new MemoryImageSource(im.getWidth(null), im.getHeight(null), 
      data, 0, im.getWidth(null));
    return Toolkit.getDefaultToolkit().createImage(source);
  }
  
  /** Get a BufferedImage containing the image from a ComplexImage.  This is necessary
      for using the ImageIO classes, which only work on BufferedImages. */
  
  private static BufferedImage getBufferedImage(Image im, boolean hasAlpha)
  {
    if (im instanceof BufferedImage)
      return (BufferedImage) im;
    BufferedImage bi = new BufferedImage(im.getWidth(null), im.getHeight(null), hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    Graphics g = bi.getGraphics();
    g.drawImage(im, 0, 0, null);
    g.dispose();
    return bi;
  }
}

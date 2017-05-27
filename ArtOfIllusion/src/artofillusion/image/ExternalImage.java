/* Copyright (C) 2017 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image;

import artofillusion.*;
import artofillusion.math.*;
import buoy.widget.BStandardDialog;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Date;
import javax.imageio.*;

public class ExternalImage extends ImageMap
{
  private File externalFile;
  private String loadingError, imageType;
  private ImageMap imageMap;
  private int w, h;
  private String type, lastAbsolutePath, lastRelativePath;
  private File imageFile;
  private Image brokenImage;
  private boolean connected, newLink, nameAutomatic = true;

  /** Create an external image out of a image file */
  
  public ExternalImage(String path) throws InterruptedException
  {
    this(new File(path));
  }

  public ExternalImage(File file) throws InterruptedException
  {
    this(file, null);
  }

  /** Create an external image out of an image file */

  public ExternalImage(File file, Scene scene) throws InterruptedException
  {
    newLink = true;
    try
    {
      loadExternalImage(file);
    }
    catch(Exception e)
    {
      throw new InterruptedException();
    }
    imageFile = file;
    imageType = imageMap.getType();
    setDataCreated(file);
    lastAbsolutePath = file.getAbsolutePath();
	if (scene == null)
	  lastRelativePath = new String();
	else
	  lastRelativePath = findRelativePath(scene);
  }

  // To be developed to a Thememanager thing
  private Image loadIcon(String iconName)
  {  
    try 
    {
      return ImageIO.read(ExternalImage.class.getResource("/artofillusion/image/icons/" + iconName));
    }
    catch(IOException e)
    {
      System.out.println(e);
    }
    return null;
  }
  
  @Override
  public File getFile()
  {
    if (! connected)
      return null;
    return imageFile;
  }

  public String getPath()
  {
	if (connected)
	  return imageFile.getAbsolutePath();
	return lastAbsolutePath;
  }

  public String getType()
  {
    if (connected)
      return imageMap.getType();
	return (imageType);
  }

  @Override
  public int getWidth()
  {
    return imageMap.getWidth();
  }

  @Override
  public int getHeight()
  {
    return imageMap.getHeight();
  }

  @Override
  public float getAspectRatio()
  {
    return imageMap.getAspectRatio();
  }

  @Override
  public int getComponentCount()
  {
    return imageMap.getComponentCount();
  }

  @Override
  public float getComponent(int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    return imageMap.getComponent(component, wrapx, wrapy, x, y, xsize, ysize);
  }

  @Override
  public float getAverageComponent(int component)
  {
    return imageMap.getAverageComponent(component);
  }

  @Override
  public void getColor(RGBColor theColor, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    imageMap.getColor(theColor, wrapx, wrapy, x, y, xsize, ysize);
  }

  @Override
  public void getGradient(Vec2 grad, int component, boolean wrapx, boolean wrapy, double x, double y, double xsize, double ysize)
  {
    imageMap.getGradient(grad, component, wrapx, wrapy, x, y, xsize, ysize);
  }
  
  @Override
  public Image getPreview()
  {
    if (connected){
      return imageMap.getPreview();}
    else    
      return brokenImage.getScaledInstance(PREVIEW_SIZE_DEFAULT, PREVIEW_SIZE_DEFAULT, Image.SCALE_SMOOTH);
   }

  @Override
  public Image getPreview(int size)
  {
    if (connected)
        return imageMap.getPreview(size);
    else
        return brokenImage.getScaledInstance(size, size, Image.SCALE_SMOOTH);
  }
  
  @Override
  public Image getMapImage(int size)
  {
    return imageMap.getMapImage(size);
  }
  
  /** Check if the image name is updated automatically. */

  public boolean isNameAutomatic()
  {
    return nameAutomatic;
  }

  
  /** Set if the image name is updated automatically. */
  
  public void setNameAutomatic(boolean automatic)
  {
    nameAutomatic = automatic;
  }

  /** Load an image file to create, refresh or reconnect an external image */

  private void loadExternalImage(File file) throws Exception
  {
    try
    {
      imageMap = loadImage(file);
    }
    catch (Exception e)
    {
      connected = false;
      throw e;
    }
    w = imageMap.getWidth();
    h = imageMap.getHeight();
    imageFile = file;
	imageType = imageMap.getType(); // The image type may change behind the same filename.
    connected = true;
  }

  private void createTemporaryImage()
  {
    if (w <= 0 || h <= 0)
        w = h = 256;
    try
    {
      brokenImage = loadIcon("file_missing.png");
      imageMap = new MIPMappedImage(brokenImage.getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }
    catch (Exception e)
    {
        // This should never be needed if the icon image was in the compiled .jar
    }
  }

  public void refreshImage() // Refresh image can be called only if and imageMap has been lodatd to this ExternalImage before.
  {
    File file;
	
	System.out.println("@REFRESH " + imageFile);
	System.out.println("@REFRESH " + lastAbsolutePath);
	System.out.println("@REFRESH " + lastRelativePath);
	
    if (imageFile != null && imageFile.isFile()) // the 'imageFile' is null if the scene file was opened with a broken link.
	{
	  System.out.println("@REFRESH IFILE" + lastRelativePath);
      file = imageFile;
	}
    else
    {
	  if (lastRelativePath.isEmpty() || ! (new File(lastRelativePath)).isFile()){
	    System.out.println("@REFRESH ABSP" + lastRelativePath);
        file = new File(lastAbsolutePath); // lastRealtivePath may not exist or be up to date
	  }
	  else
	  {
	    System.out.println("@REFRESH RELP" + lastRelativePath);
        file = new File(lastRelativePath);
	  }
	}

    try
    {
	  System.out.println("@TRY " + file);
      loadExternalImage(file);
    }
    catch(Exception e)
    {
	  System.out.println("@CATCH " + e);
      createTemporaryImage();
    }
	//lastAbsolutePath = imageFile.getAbsolutePath();
	//lastRelativePath = new String(); // No scene available
    setDataEdited();
  }

  public void reconnectImage(File file, Scene scene) throws Exception
  {
    try
    {
      loadExternalImage(file);
    }
    catch(Exception e)
    {
      throw e;
    }
    String fileName = file.getName();
    imageName = fileName.substring(0, fileName.lastIndexOf('.'));
	lastAbsolutePath = file.getAbsolutePath();
	if (scene == null)
	  lastRelativePath = new String();
	else
      lastRelativePath = findRelativePath(scene);
  }
  
  public boolean isConnected()
  {
    // Connected is true if the last load from file was succesful
    return connected;
  }

  public ImageMap getImageMap()
  {
    return imageMap;
  }
  
  /** Create an external image from a saves aoi scene */
  
  public ExternalImage(DataInputStream in) throws IOException
  {
    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("Unrecognized version of ExternalImage");
    
    String absolutePath = in.readUTF();
    String relativePath = in.readUTF();
    lastAbsolutePath = absolutePath;
    lastRelativePath = relativePath; // in case the image file is not there bu the scene is saved
    File f = new File(absolutePath);
    imageFile = new File(relativePath);
    if (!imageFile.isFile() && f.isFile()) // if no imageFile @ relativePath
      imageFile = f;
    
    w = in.readInt();
    h = in.readInt();
    imageType     = in.readUTF();
    imageName     = in.readUTF();
	nameAutomatic = in.readBoolean();
    userCreated   = in.readUTF();
    dateCreated   = new Date(in.readLong());
    zoneCreated   = in.readUTF();
    userEdited    = in.readUTF();
    dateEdited    = new Date(in.readLong());
    zoneEdited    = in.readUTF();
    try
    {
      loadExternalImage(imageFile); // At least 'w' and 'h' need to be read before attempting to load the image.
    }
    catch(Exception e)
    {
        connected = false; // This is not use dvery much after all....
        createTemporaryImage();
    }
  }

  /** @deprecated <br> 
      Use writeToStream(DataOutputStream out, Scene scene) instead. <p>
      This will save the ExternalImage to the stream, but the relative path will 
      be replaced by absolute path as the path to the saved scene is missing */

  @Override
  @Deprecated
  public void writeToStream(DataOutputStream out) throws IOException
  {
    writeToStream(out, null);
  }
  
  /** Write to data stream. Scene is used to determine the relative path to the external file. */
  
  public void writeToStream(DataOutputStream out, Scene scene) throws IOException
  {
    out.writeShort(0); // ExternalImage was not exixting in AoI 3.0.3 and earlier
    if (connected)
    {
      out.writeUTF(imageFile.getAbsolutePath());
      if (scene == null) // If used by the other writeToStream method.
        out.writeUTF(imageFile.getAbsolutePath());
      else
		lastRelativePath = findRelativePath(scene);
        out.writeUTF(lastRelativePath);
    }
    else
    {
      out.writeUTF(lastAbsolutePath);
      out.writeUTF(lastRelativePath);
    }
    out.writeInt(w);
    out.writeInt(h);
    out.writeUTF(imageType);
    out.writeUTF(imageName);
	out.writeBoolean(nameAutomatic);
    out.writeUTF(userCreated);
    out.writeLong(dateCreated.getTime());
    out.writeUTF(zoneCreated);
    out.writeUTF(userEdited);
    out.writeLong(dateEdited.getTime());
    out.writeUTF(zoneEdited);
  }
  
  /** Find the relative path from the scene file containing this object to the external scene. */

  // This procedure is copied from ExternalObject almost as such
  private String findRelativePath(Scene scene)
  {
    String scenePath = null, imagePath = null;
    try
    {
      scenePath = new File(scene.getDirectory()).getCanonicalPath();
      imagePath = imageFile.getCanonicalPath();
    }
    catch (Exception e)
    {
      // We couldn't get the canonical name for one of the files.

      return "";
    }

    // Break each path into pieces, and find how much they share in common.

    String splitExpr = File.separator;
    if ("\\".equals(splitExpr))
      splitExpr = "\\\\";
    String scenePathParts[] = scenePath.split(splitExpr);
    String externalPathParts[] = imagePath.split(splitExpr);
    int numCommon;
    for (numCommon = 0; numCommon < scenePathParts.length && numCommon < externalPathParts.length && scenePathParts[numCommon].equals(externalPathParts[numCommon]); numCommon++);
    StringBuilder relPath = new StringBuilder();
    for (int i = numCommon; i < scenePathParts.length; i++)
      relPath.append("..").append(File.separator);
    for (int i = numCommon; i < externalPathParts.length; i++)
    {
      if (i > numCommon)
        relPath.append(File.separator);
      relPath.append(externalPathParts[i]);
    }
    return relPath.toString();
  }
}

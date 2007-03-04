/*  SafeFileOutputStream.java  */

package artofillusion;

/*
 * Copyright (C) 2004, Nik Trevallyn-Jones.
 *
 * SafeFileOutputStream: writes to a file safely
 *
 * Author: Nik Trevallyn-Jones, nik777@users.sourceforge.net
 * $Id: SafeFileOutputStream.java,v 1.2 2004/12/21 13:30:35 nik Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * with this program. If not, the license is available from the
 * GNU project, at http://www.gnu.org.
 */

/**
 *  A SafeFileOutputStream modifies a file safely, in a way that permits
 *  rollback of the changes.
 */

import java.io.File;
import java.io.FilterOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;

public class SafeFileOutputStream extends FilterOutputStream
{
    /**
     *  Stream creation modes
     */
    public static final int OVERWRITE = 0;
    public static final int CREATE = 1;
    public static final int APPEND = 2;

    /**
     *  optional modes
     */
    public static final int KEEP_BACKUP = 128;

    /**
     *  Ctor
     *  Create a safe output stream on the named path
     *
     *  @param path string pathname
     *  @param mode the logical OR of the desired mode values
     */
    public SafeFileOutputStream(String path, int mode)
        throws IOException
    {
        super(null);
        open(path, mode);
    }

    /**
     *  Ctor
     *  Create a safe output stream on the specified file
     *
     *  @param file - File to write
     *  @param mode - logical OR of the desired mode values
     */
    public SafeFileOutputStream(File file, int mode)
        throws IOException
    {
        super(null);
        open(file, mode);
    }

    /**
     *  open the specified path
     *
     *  @param path String pathname of the output file
     *  @param mode logical OR of the desired mode values
     */
    public void open(String path, int mode)
        throws IOException
    { open(new File(path), mode); }

    /**
     *  open the specified file
     *
     *  @param file File to write
     *  @param mode logical OR of the desired mode values
     */
    public void open(File file, int mode)
        throws IOException
    {
        // abort any current output
        if (out != null) abort();

        this.file = file;
        this.path = file.getPath();
        this.mode = mode;

        if ((mode & CREATE) > 0 && file.exists()) {
            throw new IOException("File exists: " + path);
        }

        out = new FileOutputStream(path + ".tmp");

        // if appending, then prepend the existing content
        if ((mode & APPEND) > 0 && file.exists()) {
            FileInputStream in = new FileInputStream(file);

            if (array == null) {
                array = new byte[10000];
            }

            int count = 0;
            do {
                count = in.read(array);
                
                if (count > 0) {
                    out.write(array, 0, count);
                }
            } while ( count >= 0);
        }
    }

    /**
     *  abort the stream
     *
     *  closes the underlying stream, frees all resources, and removes any
     *  temporary files.
     */
    public void abort()
        throws IOException
    {
        if (out != null) {
            out.close();
            out = null;
        
            File temp = new File(path + ".tmp");
        
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    /**
     *  close the underlying stream, and complete the commit
     */
    public void close()
        throws IOException
    {
        // close the output first.
        out.flush();
        out.close();
        out = null;

        File bak = null;

        // swap the files
        try {

            // remove any existing backup
            bak = new File(path + ".bak");
            if (bak.exists()) {
                if (!bak.delete())
                    throw new IOException("SafeFileOutputStream.close: " + 
                                          "could not delete backup file");
            }

            // backup file
            if (file.exists()) {
                if (!file.renameTo(bak))
                    throw new IOException("SafeFileOutputStream.close: " +
                                          "could not create backup file");
            }

            // rename temp file
            File temp = new File(path + ".tmp");
            if (!temp.renameTo(file))
                throw new IOException("SafeFileOutputStream.close: " +
                                      "could not make file live");
        }
        finally {
            if (bak != null && bak.exists() == true) {

                // recover from backup
                if (file.exists() == false) {
                    if (!bak.renameTo(file))
                        throw new IOException("SafeFileOutputStream.close: " +
                                              "Error recovering from backup." +
                                              "\nFailed to rename " +
                                              bak.getAbsolutePath() + " to " +
                                              file.getAbsolutePath());
                }

                // tidy up
                if ((mode & KEEP_BACKUP) == 0) {
                    if (!bak.delete())
                        throw new IOException("SafeFileOutputStream.close: " + 
                                              "could not delete backup file");
                }
            }
        }
    }

    /**
     *  write to the underlying stream
     */
    public void write(byte[] array)
        throws IOException
    { out.write(array, 0, array.length); }

    /**
     *  write to the underlying stream
     */
    public void write(byte[] array, int start, int length)
        throws IOException
    { out.write(array, start, length); }

    /**
     *  write to the underlying stream
     */
    public void write(int value)
        throws IOException
    { out.write(value); }

    /*
     *  private state
     */
    private String path = null;
    private File file = null;
    private int mode = -1;
    
    private byte[] array = null;
}


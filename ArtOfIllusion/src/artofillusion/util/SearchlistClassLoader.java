package artofillusion.util;

/*
 * SearchlistClassLoader: class loader which loads classes using a searchlist
 *
 * Copyright (C) 2006 Nik Trevallyn-Jones, Sydney Austraila.
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

import java.util.Vector;
import java.net.URL;
import java.net.URLClassLoader;

import java.io.*;

/**
 *  A class loader which loads classes using a searchlist of
 *  other classloaders.
 *
 *<br><b>Important</b>There is an big difference between loading a class
 *	through a URLClassLoader created internally from a specified URL, and
 *	loading it through an existing ClassLoader.
 *<br>A class loaded through a newly created URLClassloader may be a
 *	<i>duplicate</i> of that same class already loaded through a different
 *	classloader, whereas a class loaded through an existing classloader
 *	will be	shared by all users of that classloader.
 *<br>(Of course, if the existing classloader is itself a URLClassLoader, then
 *	that classloader may well create duplicates.)
 *
 *<br>SearchlistClassLoader therefore allows control over the oder in which
 *	classloaders are searched, through the {@link #setSearchMode(byte)}
 *	method.
 *
 *<br>The possible <i>searchmodes</i> are:
 *<ul>
 *   <li>SHARED
 *	<br>added classloaders are searched before added URLs, which causes
 *	an existing class instance to be used (and SHARED) in preference to
 *	loading a NON-SHARED duplicate.
 *
 *   <li>NONSHARED
 *	<br>added URLs are searched <i>before</i> added classloaders. This will
 *	create a NON-SHARED copy of a class (ie a duplicate) in preference to
 *	using a SHARED one from another classloader.
 *
 *   <li>ORDERED
 *	<br>added classloaders and URLs are searched in the order in which
 *	they were added to the searchlist.
 *</ul>
 *
 *<br>There is also a method which retrieves a class <i>without</i> searching
 *	any added classloaders. This effectively retrieves the <i>canonical</i>
 *	instance of the requested class (see {@link #loadLocalClass(String)}
 *	and {@link #getLocalResource(String)}).
 *
 *  <i>Implementation note:</i>.
 *<br>The structure of the internal ClassLoader methods is as per the
 *  instructions in {@link java.lang.ClassLoader}. While I don't think this is
 *  necessary any longer, it was quite easy to comply with the instructions.
 */
public class SearchlistClassLoader extends ClassLoader
{
    protected Vector shared, nonshared;
    protected ClassLoader recent;
    protected URLClassLoader urlParent;
    protected int searchMode = SHARED;

    /**  search mode enums  */
    public static final byte SHARED	= 0x1;
    public static final byte NONSHARED	= 0x2;
    public static final byte ORDERED	= 0x3;

    protected static final URL EMPTY_URL[] = new URL[0];

    /**
     *  create a SearchlistClassLoader.
     */
    public SearchlistClassLoader()
    {}

    /**
     *  create a SearchlistClassLoader.
     */
    public SearchlistClassLoader(ClassLoader parent)
    { super(parent); }

    /**
     *  create a SearchlistClassLoader.
     */
    public SearchlistClassLoader(URL url[])
    { urlParent = new URLClassLoader(url); }

    /**
     *  create a SearchlistClassLoader.
     */
    public SearchlistClassLoader(URL url[], ClassLoader parent)
    {
	super(parent);
	urlParent = new URLClassLoader(url);
    }

    /**
     *  set the search mode.
     *
     *  @param mode enum for the searchmode: SHARED, NONSHARED, ORDERED
     */
    public void setSearchMode(byte mode)
    {
	searchMode = mode;

	if (searchMode <= 0 || searchMode > ORDERED) {
	    System.out.println("SearchlistClassLoader.setSearchMode: " +
			       "Invalid search mode: " + mode +
			       "; defaulting to SHARED.");

	    searchMode = SHARED;
	}
    }

    /**
     *  add a classloader to the searchlist
     */
    public void add(ClassLoader loader)
    {
	if (shared == null) shared = new Vector(16);
	shared.add(loader);
    }

    /**
     *  add a URL to the searchlist.
     *
     *  Creates a new URLClassLoader and adds it to the searchlist.
     */
    public void add(URL url)
    {
	// if mode is ORDERED, keep all loaders in a single list
	if (searchMode == ORDERED) {
	    if (shared == null) shared = new Vector(16);
	    shared.add(new URLClassLoader(new URL[] { url }));
	}
	// otherwise keep URLloaders in a separate list
	else {
	    if (nonshared == null) nonshared = new Vector(16);
	    nonshared.add(new URLClassLoader(new URL[] { url }));
	}
    }

    /**
     *  return the array of URLs used locally by this class loader
     */
    public URL[] getURLs()
    { return (urlParent != null ? urlParent.getURLs() : EMPTY_URL); }

    /**
     *  return the list of URLs in the search list
     */
    public URL[] getSearchPath()
    {
	Vector list = new Vector(16);
	Vector search = (nonshared != null
			 ? nonshared
			 : searchMode == ORDERED ? shared : null);

	if (search == null || search.size() == 0) return EMPTY_URL;

	URL[] url;
	int i, j;
	for (i = 0; i < search.size(); i++) {
	    if (search.get(i) instanceof URLClassLoader) {
		url = ((URLClassLoader) search.get(i)).getURLs();
		for (j = 0; j < url.length; j++)
		    list.add(url[j]);
	    }
	}

	return (list.size() > 0 ? (URL[]) list.toArray(EMPTY_URL) : EMPTY_URL);
    }

    /**
     *  Return the local class instance for <i>name</i>.
     *
     *<br>This does <i>not</i> search the searchlist. Only classes loaded
     *  directly by this loader or its parent are returned.
     *
     *<br>This method can be used to retrieve the <i>canonical</i> instance
     *  of a class.
     *  If this method is called on a set of SearchlistClassLoaders, then
     *  the only classloader which will return the class is the one which
     *  originally loaded it (assuming no duplicates have been created yet).
     */
    public Class loadLocalClass(String name)
	throws ClassNotFoundException
    {
	ClassNotFoundException err = null;

	if (getParent() != null) {
	    try {
		return getParent().loadClass(name);
	    } catch (ClassNotFoundException e) {
		err = e;
	    }
	}

	if (urlParent != null) {
	    try {
		return urlParent.loadClass(name);
	    } catch (ClassNotFoundException e) {
		err = e;
	    }
	}

	throw (err != null ? err : new ClassNotFoundException(name));
    }

    /**
     *  Return the URL for the local resource specified by <i>name</i>.
     *
     *<br>This does <i>not</i> search the searchlist. Only resources loaded
     *  directly by this loader or its parent are returned.
     *
     *<br>This method can be used to retrieve the <i>canonical</i> URL for a
     *  resource.
     *  If this method is called on a set of SearchlistClassLoaders, then
     *  the only classloader which will return the resource is the one which
     *  originally loaded it (assuming no duplicates have been created yet).
     */
    public URL getLocalResource(String name)
    {
	URL result = null;

	if (getParent() != null) {
	    result = getParent().getResource(name);
	}

	if (result == null && urlParent != null) {
	    result = urlParent.getResource(name);
	}

	return result;
    }


    /**
     *  Return a Class object for the specified class name.
     *
     *  @overloads java.lang.ClassLoader#findClass(String)
     *<br>This method is called by inherited loadClass() methods whenever
     *  a class cannot be found by the parent classloader.
     */
    public Class findClass(String name)
	throws ClassNotFoundException
    {
	byte[] b = loadClassData(name);
	return defineClass(name, b, 0, b.length);
    }

    /**
     *  load the byte data for a class definition.
     *
     *  @param name the fully-qualified class name
     *  @return a byte[] containing the class bytecode.
     *
     *  @throws ClassNotFoundException if the class cannot be found.
     */
    protected byte[] loadClassData(String name)
	throws ClassNotFoundException
    {
	InputStream in;
	ByteArrayOutputStream barray;
	byte buff[];
	int len;

	try {
	    URL url = findResource(translate(name, ".", "/") + ".class");
	    if (url != null) {

		in = url.openStream();
		barray = new ByteArrayOutputStream(1024*16);
		buff = new byte[1024];

		do {
		    len = in.read(buff, 0, buff.length);
		    if (len > 0) barray.write(buff, 0, len);
		} while (len >= 0);

		if (barray.size() == 0)
		    throw new ClassNotFoundException("Unable to read data: " +
						     name);

		return barray.toByteArray();
	    }

	} catch (Exception e) {
	    throw new ClassNotFoundException(e.getMessage() +
					     " (" + name + ")");
	}

	throw new ClassNotFoundException(name);
    }

    /**
     *  find a resource using the searchlist.
     *
     *  @overloads ClassLoader#findResource(String)
     *
     *  <em>Note</em>Inherited behaviour of loadClass() and getResource() means
     *  that this method is <em>not</em> called if our parent classloader has
     *  already found the data.
     *
     *  @param path the fully-qualified name of the resource to retrieve
     *  @return the URL if the resource is found, and <i>null</i> otherwise.
     */
    public URL findResource(String path)
    {
	ClassLoader loader = null, recent = null;
	URL url = null;
	int j, first, second;

	// check our own urlloader first
	if (urlParent != null) url = urlParent.getResource(path);

	// check the most recent loader next
	if (url == null && recent != null) url = recent.getResource(path);

	// work out how many entries are in the first and second lists
	if (searchMode == NONSHARED) {
	    first = (nonshared != null ? nonshared.size() : 0);
	    second = first + (shared != null ? shared.size() : 0);
	}
	else {
	    first = (shared != null ? shared.size() : 0);
	    second = first + (nonshared != null ? nonshared.size() : 0);
	}

	// now use the search lists
	j = 0;
	while (url == null) {

	    loader = null;

	    // search the loaders in the appropriate order...
	    if (searchMode == NONSHARED) {
		if (j < first) loader = (ClassLoader) nonshared.get(j);
		else if (j < second)
		    loader = (ClassLoader) shared.get(j - first);
	    }
	    else {
		if (j < first) loader = (ClassLoader) shared.get(j);
		else if (j < second)
		    loader = (ClassLoader) nonshared.get(j - first);
	    }

	    j++;

	    if (loader == null) break;

	    // avoid checking recent loader twice
	    if (loader == recent) continue;

	    url = loader.getResource(path);
	}

	if (url != null) recent = loader;

	return url;
    }

    /**
     *  translate matching chars in a string.
     *
     *  @param str the String to translate
     *  @param match the list of chars to match, in a string.
     *  @param replace the list of corresponding chars to replace matched chars
     *		with.
     *
     *<pre>
     *  Eg: translate("the dog.", "o.", "i")
     *  returns "the dig", because 'o' is replaced with 'i', and '.' is
     *  replaced with nothing (ie deleted).
     *</pre>
     *
     *  @return the result as a string.
     */
    public static String translate(String str, String match, String replace)
    {
	StringBuffer b = new StringBuffer(str.length());

	int pos = 0;
	char c = 0;

	if (match == null) match = "";
	if (replace == null) replace = "";

	boolean copy = (match.length() != 0 &&
			match.length() >= replace.length());

	// loop over the input string
	int max = str.length();
	for (int x = 0; x < max; x++) {
	    c = str.charAt(x);
	    pos = match.indexOf(c);

	    // if found c in 'match'
	    if (pos >= 0) {
		// translate
		if (pos < replace.length()) b.append(replace.charAt(pos));
	    }

	    // copy
	    else if (copy || replace.indexOf(c) >= match.length()) b.append(c);

	    // otherwise, effectively, delete...
	}
	
	return b.toString();
    }
}
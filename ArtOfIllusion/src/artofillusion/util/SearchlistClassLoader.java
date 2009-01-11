package artofillusion.util;

/*
 * SearchlistClassLoader: class loader which loads classes using a searchlist
 *
 * Copyright (C) 2007-2009 Nik Trevallyn-Jones, Sydney Austraila.
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

/*
 * History:
 *   V1.2	NTJ, Jan 2009.
 *		- Fixed NullPointerException when searchlist was empty.
 *		- Added support for findLibrary()
 *
 *   V1.1	NTJ, Aug 2007.
 *		- Reworked the searchlist storage, and moved searchlist
 *		  traversal into a new method: getLoader(int, byte).
 *		  -- traversal should be somewhat faster; searchMode can be
 *		     changed even after ClassLoaders have been added;
 *		- Reworked findClass(name) so that all classes located
 *		  through shared loaders are associated with the shared
 *		  loader, and all classes located through non-shared loaders
 *		  are associated with the owning SearchlistClassLoader.
 *		- removed 'recent' loader code.
 *
 *  V1.0	NTJ, April 2007.
 *		- Initial coding, based on a RemoteClassLoader used in AOS.
 */

import java.util.ArrayList;
import java.util.Vector;
import java.util.Hashtable;
import java.net.URL;
import java.net.URLClassLoader;

import java.io.*;

/**
 *  A class loader which loads classes using a searchlist of
 *  other classloaders.
 *
 *<br>The classloaders in the searchlist are of two types: <b>shared</b> and
 *  <b>non-shared</b>. A shared classloader may be in use by other code, and
 *  so <i>no</i> duplicates should be made of the classes in the loaders.
 *<br>A non-shared classloader is private to this SearchlistClassLoader, and
 *  so there is no possibility that other code could be using them. To avoid
 *  problems of isolation, all classes loaded through non-shared loaders are
 *  defined as having been loaded by the SearchlistClassLoader itself. This
 *  ensures the JVM can find the correct loader when loading associated
 *  classes (including shared classes).
 *
 *<br>The SearchlistClassLoader API therefore makes a clear distinction
 *  between <b>shared</b> and <b>non-shared</b> classloaders.
 *<br>The {@link #add(ClassLoader)} method adds an <i>existing</i> classloader
 *  which means the added classloader is treated as being <i>shared</i>.
 *<br>The {@link #add(URL)} method adds a new internally created classloader
 *  which loads the content associated with the specified URL, which means the
 *  internally created classloader is <i>non-shared</i>.
 *<br>
 *
 *<br>SearchlistClassLoader therefore also allows control over the order in
 *  which classloaders are searched, through the {@link #setSearchMode(byte)}
 *  method.
 *
 *<br>The possible <i>searchmodes</i> are:
 *<ul>
 *   <li>SHARED
 *	<br>added classloaders are searched before added URLs, which causes
 *	an existing class instance to be used (and SHARED) in preference to
 *	loading (or creating) a NON-SHARED duplicate.
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
 *<p><i>Implementation notes:</i>.
 *
 *<br>Because each class object is associated with the classloader which
 *  defined it (see ClassLoader.defineClass(...)), SearchlistClassLoader
 *  must associate <i>itself</i> with <i>all</i> class objects it loads
 *  through <i>non-shared</i> loaders, and similarly <i>must not</i> associate
 *  itself with class objects loaded through shared loaders.
 *  (See {@link #findClass(String)}.)
 *
 *<br>The structure of the internal ClassLoader methods is as per the
 *  instructions in {@link java.lang.ClassLoader}. While I don't think this is
 *  necessary any longer, it was quite easy to comply with the instructions.
 */
public class SearchlistClassLoader extends ClassLoader
{
    protected Vector list, search;
    protected Hashtable cache;
    protected Loader content = null;
    protected byte searchMode = SHARED;
    protected int divide = 0;

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
    { content = new Loader(new URLClassLoader(url), false); }

    /**
     *  create a SearchlistClassLoader.
     */
    public SearchlistClassLoader(URL url[], ClassLoader parent)
    {
	super(parent);
	content = new Loader(new URLClassLoader(url), false);
    }

    /**
     *  set the search mode.
     *
     *  @param mode enum for the searchmode: SHARED, NONSHARED, ORDERED
     */
    public void setSearchMode(byte mode)
    {
	byte prev = searchMode;
	searchMode = mode;

	if (searchMode <= 0 || searchMode > ORDERED) {
	    System.out.println("SearchlistClassLoader.setSearchMode: " +
			       "Invalid search mode: " + mode +
			       "; defaulting to SHARED.");

	    searchMode = SHARED;
	}
    }

    /**
     *  add a (shared) classloader to the searchlist.
     *
     *  The loader is added to the list as a shared loader.
     *
     *  @param loader the ClassLoader to add to the searchlist.
     */
    public void add(ClassLoader loader)
    {
	Loader ldr = new Loader(loader, true);

	// store loaders in order in list
	if (list == null) list = new Vector(16);
	list.add(ldr);

	// store shared loaders in front of non-shared loaders in search.
	if (search == null) search = new Vector(16);
	if (search.size() > divide) search.add(divide, ldr);
	else search.add(ldr);

	divide++;
    }

    /**
     *  add a (non-shared) URL to the searchlist.
     *
     *  Creates a new URLClassLoader and adds it to the searchlist as a
     *  non-shared classloader.
     *
     *  @param url the URL to add to the searchlist.
     */
    public void add(URL url)
    {
	Loader ldr = new Loader(new URLClassLoader(new URL[] { url }), false);

	// store loaders in order in list
	if (list == null) list = new Vector(16);
	list.add(ldr);

	// store non-shared loaders after shared loaders in search
	if (search == null) search = new Vector(16);
	search.add(ldr);
    }

    /**
     *  return the array of URLs used locally by this class loader
     */
    public URL[] getURLs()
    {
	return (content != null
		? ((URLClassLoader)  content.loader).getURLs()
		: EMPTY_URL
		);
    }

    /**
     *  return the list of URLs in the search list
     */
    public URL[] getSearchPath()
    {
	Loader ldr;
	URL[] url;
	int j;
	ArrayList path = new ArrayList(8);

	for (int i = 0; (ldr = getLoader(i++, searchMode)) != null; i++) {
	    if (ldr.loader instanceof SearchlistClassLoader)
		url = ((SearchlistClassLoader) ldr.loader).getSearchPath();
	    else if (ldr.loader instanceof URLClassLoader)
		url = ((URLClassLoader) ldr.loader).getURLs();
	    else
		url = null;

	    if (url != null) {
		for (j = 0; j < url.length; j++)
		    path.add(url[j]);
	    }
	}

	return (path.size() > 0 ? (URL[]) path.toArray(EMPTY_URL) : EMPTY_URL);
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
     *
     *  @param name the fully-qualified name of the class
     *  @return the loaded class.
     *
     *  @throws ClassNotFoundException if the class is not found.
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

	if (content != null) {

	    // try the cache first
	    Class result = (cache != null ? (Class) cache.get(name) : null);
	    if (result != null) return result;

	    // try loading the class data
	    byte[] data = loadClassData(content.loader, name);

	    if (data != null) {

		// define the class
		result = defineClass(name, data, 0, data.length);

		if (result != null) {
		    //System.out.println("defined class: " + name);

		    // cache the result
		    if (cache == null) cache = new Hashtable(1024);
		    cache.put(name, result);

		    return result;
		}
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
     *
     *  @param name the fully-qualified name of the resource.
     *  @return the located URL, or <i>null</i>.
     */
    public URL getLocalResource(String name)
    {
	URL result = null;

	if (getParent() != null) {
	    result = getParent().getResource(name);
	}

	if (result == null && content != null) {
	    result = content.loader.getResource(name);
	}

	return result;
    }


    /**
     *  Return a Class object for the specified class name.
     *
     *  @overloads java.lang.ClassLoader#findClass(String)
     *
     *  Traverses the searchlist looking for a classloader which can return
     *	the specified class.
     *
     *<br>This method is called by inherited loadClass() methods whenever
     *  a class cannot be found in the parent classloader.
     *
     *<br>If the class is found using a <i>shared</i> loader, then it is
     *  returned directly. If the class is found using a <i>non-shared</i>
     *  loader, then the actual class object is defined by the containing
     *  SearchlistClassLoader, which causes Java to associate the new class
     *  object with the SearchlistClassLaoder, rather then the non-shared
     *  loader.
     *
     *  @param name the fully-qualified name of the class
     *  @return the loaded class object
     *
     *  @throws ClassNotFoundException if the class could not be loaded.
     */
    public Class findClass(String name)
	throws ClassNotFoundException
    {
	Loader ldr;
	Throwable err = null;
	Class result;
	byte[] data;

	for (int i = 0; (ldr = getLoader(i, searchMode)) != null; i++) {
	    try {
		// for shared loaders - just try getting the class
		if (ldr.shared)
		    return ldr.loader.loadClass(name);

		// for non-shared loaders, we have to define the class manually
		else {
		    // check the cache first
		    result = (cache != null ? (Class) cache.get(name) : null);
		    if (result != null) return result;

		    // try loading the class
		    data = loadClassData(ldr.loader, name);
		    if (data != null) {

			// data loaded, define the class
			result = defineClass(name, data, 0, data.length);

			if (result != null) {
			    //System.out.println("defined class: " + name);

			    // cache the result
			    if (cache == null) cache = new Hashtable(1024);
			    cache.put(name, result);

			    return result;
			}
		    }
		}
	    }
	    catch (Throwable t) {
		err = t;
	    }
	}

	throw (err != null
		   ? new ClassNotFoundException(name, err)
		   : new ClassNotFoundException(name)
		   );
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
	System.out.println("findResource: looking in " + this + " for " +
			   path);

	URL url = null;
	Loader ldr;
	for (int i = 0; (ldr = getLoader(i, searchMode)) != null; i++) {

	    url = ldr.loader.getResource(path);

	    if (url != null) {
		System.out.println("found " + path + " in loader: " +
				   ldr.loader);

		break;
	    }
	}

	return url;
    }

    /**
     *  return the pathname to the specified native library.
     *
     *  If the library is not found on the searchpath, then <i>null</i>
     *  is returned, indicating to the Java machine that it should search
     *  java.library.path.
     *
     *  @param libname - the String name of the library to find
     *  @return the full path to the found library file, or <i>null</i>.
     */
    public String findLibrary(String libname)
    {
	String fileName = System.mapLibraryName(libname);
	System.out.println("findLibrary: looking in " + this + " for " +
			   libname + " as " + fileName);

	int i, j;
	URL[] url;
	File dir, file;
	Loader ldr;
	for (i = 0; (ldr = getLoader(i++, searchMode)) != null; i++) {
	    if (ldr.loader instanceof SearchlistClassLoader)
		url = ((SearchlistClassLoader) ldr.loader).getSearchPath();
	    else if (ldr.loader instanceof URLClassLoader)
		url = ((URLClassLoader) ldr.loader).getURLs();
	    else
		url = null;

	    if (url != null) {
		for (j = 0; j < url.length; j++) {
		    if (!url[j].getProtocol().equalsIgnoreCase("file"))
			continue;
		    
		    try {
			dir = new File(url[j].toURI()).getParentFile();
			file = new File(dir, fileName);

			if (file.exists()) {
			    System.out.println("found: " +
					       file.getAbsolutePath());

			    return file.getAbsolutePath();
			}
		    } catch (Exception e) {
			System.out.println("Ignoring url: " + url[j] + ": "
					   + e);
		    }
		}
	    }
	}

	// nothing found, use java.library.path
	return null;
    }

    /**
     *  return the correct classloader for the specified position in the
     *  search.
     *
     *  @param index the position (step) in the search process
     *  @param mode the search mode to use
     *
     *  @return The corresponding Loader or <i>null</i>
     */
    protected Loader getLoader(int index, byte mode)
    {
	// content is always the first loader searched
	if (content != null) {
	    if (index == 0) return content;
	    else index--;
	}

	if (index < 0 || list == null || index >= list.size()) return null;

	Loader result;

	switch (mode) {
	case SHARED:
	    // return shared loaders before non-shared loaders
	    result = (Loader) search.get(index);
	    break;

	case NONSHARED:
	    // return non-shared loaders before shared loaders
	    {
		int pos = index + divide;
		result = (Loader) (pos < search.size()
					? search.get(pos)
					: search.get(pos-divide)
					);
	    }
	    break;

	default:
	    // return loaders in the order in which they were added
	    result = (Loader) list.get(index);
	}

	return result;
    }

    /**
     *  load the byte data for a class definition.
     *
     *  @param name the fully-qualified class name
     *  @return a byte[] containing the class bytecode or <i>null</i>
     */
    protected byte[] loadClassData(ClassLoader cl, String name)
    {
	ByteArrayOutputStream barray;
	byte buff[];
	int len;

	InputStream in = cl.getResourceAsStream(translate(name, ".", "/")
						+ ".class");

	if (in == null) return null;

	try {
	    
	    barray = new ByteArrayOutputStream(1024*16);
	    buff = new byte[1024];

	    do {
		len = in.read(buff, 0, buff.length);
		if (len > 0) barray.write(buff, 0, len);
	    } while (len >= 0);

	    return (barray.size() > 0 ? barray.toByteArray() : null);

	} catch (Exception e) {
	    return null;
	}
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

    /**
     *  internal class to store the state of each searchable ClassLoader.
     *
     *  The containing SearchlistClassLoader needs to know information about
     *  each loader in the list.
     */
    protected static class Loader
    {
	ClassLoader loader = null;		// the actual classloader
	boolean shared = false;			// shared flag

	Loader(ClassLoader loader, boolean shared)
	{
	    this.loader = loader;
	    this.shared = shared;
	}
    }
}
/*
 *  Copyright 2004 Francois Guillet
 *  This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 2 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */
package artofillusion.spmanager;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import java.net.*;


/**
 *  Description of the Class
 *
 *@author     François Guillet
 *@created    15 mars 2004
 */
public class SPMObjectInfo
{
	/**
	 *  Author name
	 */
	public String author = "?";
	/**
	 *  Script name
	 */
	public String name = "";
	/**
	 *  Release date
	 */
	public String date = "00/00/0000";
	/**
	 *  Version
	 */
	public String version = "0.0";
	/**
	 *  Length of the script file
	 */
	public long length;
	/**
	 *  Beta version, -1 means it's not a beta
	 */
	public int beta = -1;

	/** restriction for info */
	public int restriction = SPMParameters.ENABLE;

	/**  number of active references to this object */
	public int refcount = 0;

	/** string of flags */
	public String flags;

	/** invalid flag */
	public boolean invalid = false;
	
	private String description = null;
	private String comments = null;

	private HashMap externals;
	private Vector changeLog;
	private Vector details;

	protected HashMap exports;
	public HashMap actions;

	/**
	 *  Script file name
	 */
	public String fileName = "**Uninitialised**";
	static String textBoxString = "";
	/**
	 *  URL of the script
	 */
	public URL httpFile;

	/**
	 *  Associated files, if any (fileset)
	 */
	public String[] files;
	/**
	 *  Same with URLs
	 */
	public String[] httpFiles;
	/**
	 *  local destination to copy to
	 */
	public ArrayList destination;
	/**
	 *  sizes of the fileset files
	 */
	public long[] fileSizes;
	/**
	 *  xml description of the script, root node being <script>
	 */
	public Document xmlDescription;

	/**  flag to indicate that this plugin/script has been loaded */
	public boolean loaded = false;

	char separatorChar;
	boolean selected = false;
	boolean deletable = true;
	private boolean remote;
	private boolean filesetRecord;

	/**
	 *  Constructor for the SPMObjectInfo object
	 */
	public SPMObjectInfo()
	{}


	/**
	 *  Constructor for the SPMObjectInfo object
	 *
	 *@param  fn  Filename
	 */
	public SPMObjectInfo( String fn )
	{
		fileName = fn;
		separatorChar = File.separatorChar;
		getName();
		remote = false;
		if ( fn.endsWith( ".bsh" ) )
		{
			loadXmlInfoFromScript();
		}
		else if ( fn.endsWith( ".jar" ) )
		{
			loadXmlInfoFromJarFile();
		}
		File f = new File( fn );
		length = f.length();
		//length = 0;

	}


	/**
	 *  Constructor for the SPMObjectInfo object
	 *
	 *@param  hf  URL location of the file
	 */
	public SPMObjectInfo( URL hf )
	{
		this.httpFile = hf;
		remote = true;
		fileName = httpFile.toString();
		fileName = fileName.replaceAll( "%20", " " );
		separatorChar = '/';
		getName();
		String s = hf.toString();
		if ( s.endsWith( ".bsh" ) )
		{
			loadXmlInfoFromRemoteScript();
		}
		else if ( s.endsWith( ".jar" ) )
		{
			loadXmlInfoFromRemoteJarFile();
		}
	}

	/**
	 *  Constructor for the SPMObjectInfo object
	 *
	 *@param  n  Node for the <script> tag of an xml document
	 *@param  hf  URL location of the file
	 *@param  length  File length, in bytes
	 */
	public SPMObjectInfo( Node n, URL hf, long length )
	{
		this.httpFile = hf;
		remote = true;
		fileName = httpFile.toString();
		fileName = fileName.replaceAll( "%20", " " );
		separatorChar = '/';
		getName();
		readInfoFromDocumentNode( n );
		this.length = length;
	}


	/**
	 *  Gets the name attribute of the SPMObjectInfo object
	 *
	 *@return    The name value
	 */
	public String getName()
	{
		if ( name.equals( "" ) )
		{
			int cut = fileName.lastIndexOf( separatorChar );
			if (cut >= 0 && cut < fileName.length()-1)
				name = fileName.substring(cut+1);
			else
				name = fileName;

			cut = name.lastIndexOf('.');
			if (cut >= 0) name = name.substring(0, cut);
		}

		return name;
	}


	/**
	 *  Gets the fullName attribute of the SPMObjectInfo object
	 *
	 *@return    The fullName value
	 */
	public String getFullName()
	{
		String betaString = "";
		if ( beta > -1 )
		{
			betaString = "b" + beta;
		}
		String addFiles = "";
		long kbsize;
		if ( files != null )
		{
			addFiles = " (" + SPMTranslate.text( "additionalFiles" ) + " ";
			for ( int i = 0; i < files.length; ++i )
			{
				kbsize = Math.round( fileSizes[i] / 1000 );
				if ( kbsize < 1 )
				{
					kbsize = 1;
				}
				addFiles = addFiles + files[i] + " " + kbsize + "kb";
				if ( i != files.length - 1 )
				{
					addFiles = addFiles + ",";
				}
				else
				{
					addFiles = addFiles + ")";
				}
			}
		}
		//return getName()+" by "+author+", v"+version+betaString+" "+date+" , Size: "+length+"b";
		kbsize = Math.round( length / 1000 );
		if ( kbsize < 1 )
		{
			kbsize = 1;
		}
		return ( SPMTranslate.text( "fullname", new Object[]{
				getName(), author, version + betaString, date,
				String.valueOf( kbsize )} ) + addFiles );
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public String toString()
	{
		return getName();
	}


	/**
	 *  Gets the xmlHeaderAsString attribute of the SPMObjectInfo object
	 *
	 *@param  reader  Description of the Parameter
	 *@return         The xmlHeaderAsString value
	 */
	private String getXmlHeaderAsString( BufferedReader reader )
	{
		//Vector c = new Vector();
		char c1 = ' ';
		char c2 = ' ';
		//String s = "";
		StringBuffer sb = new StringBuffer(1024);
		int status = 0;
		try
		{
			c1 = (char) reader.read();
			c2 = (char) reader.read();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		while ( ( ( c1 != '/' ) || ( c2 != '*' ) ) && ( status != -1 ) )
		{
			c1 = c2;
			try
			{
				status = reader.read();
				c2 = (char) status;
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		while ( c1 != '<'  )
		{
			try
			{
				status = reader.read();
				if ( status == -1 )
				{
					return null;
				}
				c1 = (char) status;
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		//s = s + c1;
		sb.append(c1);

		while ( ( ( c1 != '*' ) || ( c2 != '/' ) ) && ( status != -1 ) )
		{
			c1 = c2;
			try
			{
				status = reader.read();
				c2 = (char) status;
				sb.append(c2);
				//s = s + c2;
				//c.add( new Byte( (byte) c2 ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		if ( sb.length() > 2 )
		{
			//s = s.substring( 0, s.length() - 3 );
			//return s;
			return sb.substring(0, sb.length() - 3);
		}
		else
		{
			return null;
		}
	}


	/**
	 *  Description of the Method
	 */
	private void loadXmlInfoFromScript()
	{
		BufferedReader fileReader = null;
		String s = null;
		try
		{
			fileReader = new BufferedReader( new FileReader( fileName ) );
			s = getXmlHeaderAsString( fileReader );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
			return;
		}
		try
		{
			fileReader.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}

		if ( s == null )
		{
			return;
		}
		try
		{
			byte[] xmlByteArray = s.getBytes();
			BufferedInputStream xmlStream = new BufferedInputStream( new ByteArrayInputStream( xmlByteArray ) );
			xmlDescription = SPManagerUtils.builder.parse( xmlStream );
			readInfoFromXmlHeader( xmlDescription );
			xmlStream.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}


	/**
	 *  Description of the Method
	 */
	private void loadXmlInfoFromJarFile()
	{
		/*
		 * NTJ: AOI 2.5. Default XML file name changed to 'extensions.xml'
		 * For compatibility we try the new name first, then the old...
		 */
		//String fn = getName() + ".xml";

		/*
		 *  byte[] xmlByteArray = SPManagerUtils.getJarFileContent( fileName, fn );
		 *  if ( xmlByteArray != null )
		 *  {
		 *  try
		 *  {
		 *  ByteArrayInputStream xmlStream = new ByteArrayInputStream( xmlByteArray );
		 *  BufferedReader xmlReader = new BufferedReader( new InputStreamReader( xmlStream ) );
		 *  XmlParser parser = new XmlParser( xmlReader );
		 *  try
		 *  {
		 *  traverse( parser, "" );
		 *  }
		 *  catch ( Exception exc )
		 *  {
		 *  System.out.println( "File " + fileName + " does not have a valid XML optional header." );
		 *  }
		 *  xmlReader.close();
		 *  xmlStream.close();
		 *  }
		 *  catch ( Exception e )
		 *  {
		 *  e.printStackTrace();
		 *  }
		 *  }
		 */

		String fn = "extensions.xml";

		try
		{
			InputStream is;
			URL url = new URL( "jar:file:" + fileName + "!/" + fn );
			//JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
			//Object obj = jarConnection.getContent();
			//if ( obj instanceof InputStream )
			//{

			// try new name first
			try {
				is = url.openStream();
			} catch (Exception e) {
				is = null;
			}

			// ok... try old name...
			if (is == null) {
				fn = getName() + ".xml";
				url = new URL("jar:file:" + fileName + "!/" + fn);
				is = url.openStream();
			}

			try {
				//InputStreamReader r;
				//BufferedReader xmlReader = new BufferedReader( new InputStreamReader( (InputStream) obj ) );
				//BufferedInputStream xmlStream = new BufferedInputStream( (InputStream) obj );
				BufferedInputStream xmlStream = new BufferedInputStream(is);
				xmlDescription = SPManagerUtils.builder.parse( xmlStream );
				readInfoFromXmlHeader( xmlDescription );
				xmlStream.close();
				//r.close();
				//( (InputStream) obj ).close();
			}
			catch ( Throwable t ) {
				System.out.println("Reading: " + url);
				t.printStackTrace();
			}
			//}
		}
		catch ( Exception e )
		{
			if ( !( e instanceof IOException ) )
				e.printStackTrace();
		}
	}


	/**
	 *  Description of the Method
	 */
	private void loadXmlInfoFromRemoteScript()
	{
		BufferedReader in = null;
		String s = null;
		try
		{
			HttpURLConnection.setFollowRedirects( false );
			HttpURLConnection connection = (HttpURLConnection) httpFile.openConnection();
			String header = connection.getHeaderField( 0 );
			//System.out.println( header );
			//System.out.println( "---Start of headers---" );
			int i = 1;
			while ( ( header = connection.getHeaderField( i ) ) != null )
			{
				String key = connection.getHeaderFieldKey( i );
				if ( key != null )
				{
					if ( key.equals( "Content-Length" ) )
					{
						length = Long.parseLong( header );
					}
				}
				//System.out.println( ( ( key == null ) ? "" : key + ": " ) + header );
				i++;
			}
			//System.out.println( "---End of headers---" );
			in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
			s = getXmlHeaderAsString( in );
			in.close();

		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}

		if ( s == null )
		{
			return;
		}
		try
		{
			byte[] xmlByteArray = s.getBytes();
			BufferedInputStream xmlStream = new BufferedInputStream( new ByteArrayInputStream( xmlByteArray ) );

			/*BufferedReader xmlReader = new BufferedReader( new InputStreamReader( xmlStream ) );
            XmlParser parser = new XmlParser( xmlReader );
            try
            {
                traverse( parser, "" );
            }
            catch ( Exception exc )
            {
                System.out.println( "File " + fileName + " does not have a valid XML optional header." );
            }*/
			xmlDescription = SPManagerUtils.builder.parse( xmlStream );
			readInfoFromXmlHeader( xmlDescription );

		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}


	/**
	 *  Description of the Method
	 */
	private void loadXmlInfoFromRemoteJarFile()
	{
		//BufferedReader in = null;
		String s = null;
		try
		{
			HttpURLConnection.setFollowRedirects( false );
			HttpURLConnection connection = (HttpURLConnection) httpFile.openConnection();
			String header = connection.getHeaderField( 0 );
			//System.out.println( header );
			//System.out.println( "---Start of headers---" );
			int i = 1;
			while ( ( header = connection.getHeaderField( i ) ) != null )
			{
				String key = connection.getHeaderFieldKey( i );
				if ( key != null )
				{
					if ( key.equals( "Content-Length" ) )
					{
						length = Long.parseLong( header );
					}
				}
				//System.out.println( ( ( key == null ) ? "" : key + ": " ) + header );
				i++;
			}
			//System.out.println( "---End of headers---" );
			//connection.disconnect();

			/*
			 *  NTJ: for AOI 2.5: XML filename is changed to 'extensions.xml'.
			 *  For compatibility, we check the new name first, then the old.
			 */
			InputStream is = null;

			s = httpFile.toString();
			s = s.substring(0, s.lastIndexOf('/')+1) + "extensions.xml";

			try {
				is = new URL( s ).openStream();
			} catch (Exception e) {
				is = null;
			}

			if (is == null) {
				s = httpFile.toString();
				//s = s.substring( 0, s.length() - 4 ) + ".xml";
				s = s.substring( 0, s.lastIndexOf('.') ) + ".xml";
				//System.out.println("SPMObjectInfo getting " + s);
				is = new URL( s ).openStream();
			}

			//XmlParser parser = new XmlParser( in );
			//traverse( parser, "" );
			//byte[] xmlByteArray = s.getBytes();

			BufferedInputStream xmlStream = new BufferedInputStream(is);

			xmlDescription = SPManagerUtils.builder.parse( xmlStream );
			readInfoFromXmlHeader( xmlDescription );
			//if (in != null) in.close();
			if (is != null) is.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}


	/**
	 *  Gets the totalLength attribute of the SPMObjectInfo object
	 *
	 *@return    The totalLength value
	 */
	public long getTotalLength()
	{
		if ( files == null )
		{
			return length;
		}
		else
		{
			long l = length;
			for ( int i = 0; i < files.length; ++i )
			{
				l += fileSizes[i];
			}
			return l;
		}
	}


	/**
	 *  Gets the addFileName attribute of the SPMObjectInfo object
	 *
	 *@param  index  Description of the Parameter
	 *@return        The addFileName value
	 */
	public String getAddFileName( int index )
	{
		int i = fileName.lastIndexOf( separatorChar );
		String name = fileName.substring( 0, i + 1 ) + files[index];
		return name;
	}


	/**
	 *  Gets the addFileURL attribute of the SPMObjectInfo object
	 *
	 *@param  index  Description of the Parameter
	 *@return        The addFileURL value
	 */
	public URL getAddFileURL( int index )
	{
		int i = fileName.lastIndexOf( separatorChar );
		String name = fileName.substring( 0, i + 1 ) + httpFiles[index];
		URL url = null;
		try
		{
			//System.out.println( "addURL " + name );
			url = new URL( name );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		return url;
	}


	/**
	 */
	public long getRemoteFileSize(String url)
	{
		//System.out.println("fileSize: " + url.toString());

		try
		{
			URL addFile = new URL( url );
			HttpURLConnection.setFollowRedirects( false );
			HttpURLConnection connection =
				(HttpURLConnection) addFile.openConnection();

			String key = connection.getHeaderField(0);
			int i=1;
			//System.out.println( header );
			//System.out.println( "---Start of headers---" );

			while ((key = connection.getHeaderFieldKey(i)) != null )
			{
				//System.out.println("Header=" + key);
				if (key.equals( "Content-Length")) {
					//System.out.println("Content-Length=" +
					//	       connection.getHeaderField(i));
					return Long.parseLong(connection.getHeaderField(i));
				}
				//System.out.println( ( ( key == null ) ? "" : key + ": " ) + header );
				i++;
			}
			//System.out.println("headers parsed: " + i);
			//System.out.println( "---End of headers---" );
			//connection.disconnect();

		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 *  Gets the addFileSize attribute of the SPMObjectInfo object
	 *
	 *@param  addFileName  Description of the Parameter
	 *@return              The addFileSize value
	 */
	private long getRemoteAddFileSize( String addFileName )
	{
		int i = fileName.lastIndexOf( separatorChar );
		long fileSize = 0;
		String name = fileName.substring( 0, i + 1 );

		return getRemoteFileSize( name + addFileName );
	}


	/**
	 *  Gets the AddFileSize attribute of the SPMObjectInfo object
	 *
	 *@param  addFileName  Description of the Parameter
	 *@return              The remoteAddFileSize value
	 */
	private long getAddFileSize( String addFileName )
	{
		int i = fileName.lastIndexOf( separatorChar );
		long fileSize = 0;
		String name = fileName.substring( 0, i + 1 ) + addFileName;
		File file = new File( name );
		fileSize = file.length();
		if ( fileSize < 1 )
		{
			fileSize = 1;
		}
		return fileSize;
	}


	private void readInfoFromXmlHeader(Document doc)
	{
		readInfoFromDocumentNode( doc.getDocumentElement() );

	}

	private void readInfoFromDocumentNode(Node script)
	{
		int i, j, filtType;
		String val, filtName, filtVal;
		Iterator iter;
		Map.Entry entry;
		SPMParameters params = SPManagerFrame.getParameters();

		if (changeLog == null) {
			changeLog = new Vector(16);
			details = new Vector(16);
			externals = new HashMap(16);
			destination = new ArrayList(16);
			actions = new HashMap(16);
			exports = new HashMap(32);
		}
		else {
			changeLog.clear();
			details.clear();
			externals.clear();
			destination.clear();
			actions.clear();
			exports.clear();
		}

		flags = "";

		// check attributes first
		name = SPManagerUtils.getAttribute(script, "name");
		version = SPManagerUtils.getAttribute(script, "version");

		// nested tags (old syntax) are overridden by attributes (new syntax)
		if (name == null)
			name = SPManagerUtils.getNodeValue( script, "name", "", 0 );

		if (version == null)
			version = SPManagerUtils.getNodeValue( script, "version", "", 0 );

		author = SPManagerUtils.getNodeValue( script, "author", "", 0 );
		date = SPManagerUtils.getNodeValue( script, "date", "", 0 );

		String b = SPManagerUtils.getNodeValue( script, "beta", "", 0 );
		if ( ! "".equals(b) )
		{
			try
			{
				// NTJ: changed to avoid exceptions for non-digits
				//beta = Integer.parseInt( b );
				beta = SPManagerUtils.parseInt(b, 0, -1);
			}
			catch ( Exception e )
			{
				beta = -1;
			}
		}
		else beta = -1;

		// NTJ: filter on beta
		if (beta > 0) {
			if (params != null) restriction = params.getFilter("beta");

			if (flags.length() > 0) flags += "\n";
			flags += "beta";
		}

		NodeList nl = script.getChildNodes();

		Node node, subnode;
		NodeList sl;
		String extName, extType, extAssoc, extAction;

		// NTJ: infer depedencies from other tags
		for (i = 0;
		(node = SPManagerUtils.getNodeFromNodeList(nl, "import", i)) != null;
		i++) {

			extName = SPManagerUtils.getAttribute(node, "name");

			if (extName != null && !externals.containsKey(extName)) {
				externals.put(extName, extName + ":plugin= required");
			}
		}

		// NTJ: get explicit dependencies
		for (i = 0;
		(node = SPManagerUtils.getNodeFromNodeList(nl, "external", i))
		!= null;
		i++) {

			extName = SPManagerUtils.getAttribute(node, "name");

			// NTJ: explicit <external> can override implicit <import>
			//if (!externals.containsKey(extName)) {
			//System.out.println("SPManager: ext=" + extName);

			extType = SPManagerUtils.getAttribute(node, "type");
			extAssoc = SPManagerUtils.getAttribute(node, "association");
			externals.put(extName, extName + ":"+extType+"= " + extAssoc);

			extAction = SPManagerUtils.getAttribute(node, "action");
			//System.out.println("SPManager: action=" + extAction);
			if (extAction != null && extAction.length() > 0) {
				actions.put(extName, extAction);
			}
			//}
		}


		String plugClass, methName, methId, methHelp, exportList="";

		// get details of plugin classes
		for (i = 0;
		(node = SPManagerUtils.getNodeFromNodeList(nl, "plugin", i))
		!= null;
		i++) {

			plugClass = SPManagerUtils.getAttribute(node, "class");

			//System.out.println("plugin: " + plugClass);

			sl = node.getChildNodes();
			for (j = 0;
			(subnode =SPManagerUtils.getNodeFromNodeList(sl, "export", j))
			!= null;
			j++) {

				methName = SPManagerUtils.getAttribute(subnode, "method");
				if (methName == null || methName.length() == 0) continue;

				methId = SPManagerUtils.getAttribute(subnode, "id");
				exports.put(methId, plugClass + "." + methName);

				if (subnode.getChildNodes() != null
						&& subnode.getChildNodes().item(0) != null) {
					methHelp = subnode.getChildNodes().item(0).getNodeValue();
					if (exportList.length() > 0)
						exportList += "========================\n";
					exportList += methId + "\n" + methHelp + "\n";
				}

				//System.out.println("export: " + methName + ": " + methId +
				//	   "\n" + methHelp);
			}
		}

		val = SPManagerUtils.getNodeValue(script, "description", "none", 0);
		if (val != null) setDescription(val);

		val = SPManagerUtils.getNodeValue(script, "comments", "", 0);
		if (val != null) setComments(val);

		// create the display lists
		String extList = "";
		iter = externals.entrySet().iterator();
		while (iter.hasNext()) {
			extList += (extList.length() > 0 ? "\n" : "") +
			((Map.Entry) iter.next()).getValue().toString();
		}

		setLog(SPMTranslate.text("flags"), flags, 1);
		setLog(SPMTranslate.text("otherFiles"), extList, 2);
		setLog(SPMTranslate.text("exports"), exportList, 3);
		setLog(SPMTranslate.text("history"), "none", 4);

		// check assertions and set restriction accordlingly
		node = SPManagerUtils.getNodeFromNodeList(nl, "assert", 0);
		if (node != null) {
			NamedNodeMap nm = node.getAttributes();
			for (i = 0; i < nm.getLength(); i++) {
				node = nm.item(i);

				filtName = node.getNodeName();
				filtVal = node.getNodeValue();

				if (flags.length() > 0) flags += "\n";
				flags += filtName + ':' + filtVal;

				val = System.getProperty(filtName);
				if (val == null || val.length() == 0) {
					System.out.println("SPMObjectInfo: could not resolve " +
							"<assert> value: " + fileName);
					continue;
				}

				if (!test(val, filtVal) && restriction<SPMParameters.DISABLE) {
					restriction = SPMParameters.DISABLE;

					flags += " **FAILED**";
					invalid = true;
				}
			}
		}

		// NTJ: process filters
		//nl = script.getChildNodes();
		node = SPManagerUtils.getNodeFromNodeList(nl, "filter", 0);

		if (params != null && node != null) {
			NamedNodeMap nm = node.getAttributes();
			for (i = 0; i < nm.getLength(); i++) {
				node = nm.item(i);
				filtName = node.getNodeName();

				filtVal = params.getFilterString(filtName);
				filtType = SPMParameters.getFilterType(filtVal);

				if (filtType == SPMParameters.DEFAULT) {
					filtType = params.getFilterType(node.getNodeValue());
				}

				if (filtType > restriction) restriction = filtType;

				// add any new filter to params
				if (filtVal == null && filtType != SPMParameters.DEFAULT) {
					params.addFilter(filtName, SPMParameters.DEFAULT);
				}

				if (flags.length() > 0) flags += "\n";
				//flags += filtName + '=' + node.getNodeValue();
				flags += filtName;
			}
		}

		if (flags.length() > 0) setLog(SPMTranslate.text("flags"), flags, 1);

		// NTJ: set changeLog from the history nodes
		String history = "";
		//nl = script.getChildNodes();
		node = SPManagerUtils.getNodeFromNodeList( nl, "history", 0 );
		if (node != null) {

			// iterate the child log nodes
			NodeList hl = node.getChildNodes();
			for (i = 0;
			(node = SPManagerUtils.getNodeFromNodeList(hl, "log", i)) != null;
			i++
			) {

				String name = "v ";
				String str = SPManagerUtils.getAttribute(node, "version");
				name += (str != null ? str : "??");

				str = SPManagerUtils.getAttribute(node, "date");
				if (str != null) name += " " + str;

				str = SPManagerUtils.getAttribute(node, "author");
				if (str != null) name += " " + "; " + str;

				str = node.getChildNodes().item(0).getNodeValue();

				/*
		if (str != null)
		    setLog(name, str, i+5);
		else 
		    setLog(name, SPMTranslate.text("noDetail"), i+5);
				 */

				history += (history.length() > 0 ? "\n" : "") + name + str;
			}

			// update the history
			if (history.length() > 0)
				setLog( SPMTranslate.text("history"), history, 4);
		}

		//nl = script.getChildNodes();
		Node fileSet = SPManagerUtils.getNodeFromNodeList( nl, "fileset", 0 );
		String todir;
		String src;
		if (fileSet != null)
		{
			NodeList filesList = fileSet.getChildNodes();
			Vector fileNames = new Vector();
			for (i = 0; i < filesList.getLength(); ++i )
			{
				if( ! "file".equals( filesList.item(i).getNodeName() ) )
					continue;

				// NTJ: get attributes
				todir = SPManagerUtils.getAttribute(filesList.item(i),"todir");
				src = SPManagerUtils.getAttribute(filesList.item(i),"src");
				NodeList tmp = filesList.item(i).getChildNodes();
				if (tmp.getLength() > 0) {
					fileNames.add( (src != null && src.length() > 0 ? src : tmp.item(0).getNodeValue()) );

					destination.add( (todir != null ? todir+separatorChar : "") + tmp.item(0).getNodeValue());

					System.out.println("file: " + tmp.item(0).getNodeValue());
				}
			}
			if (fileNames.size() > 0)
			{
				files = new String[ fileNames.size() ];
				for (i = 0; i < files.length; ++i )
					files[i] = (String) fileNames.elementAt(i);
				httpFiles = new String[files.length];
				fileSizes = new long[files.length];
				for (i = 0; i < files.length; ++i )
				{
					files[i] = files[i].trim();
					httpFiles[i] = files[i].replaceAll( " ", "%20" );
					if ( remote )
					{
						fileSizes[i] = getRemoteAddFileSize( httpFiles[i] );
					}
					else
					{
						fileSizes[i] = getAddFileSize( files[i] );
					}
				}
			}
		}
	}

	/**
	 *  Sets the selected attribute of the SPMObjectInfo object
	 *
	 *@param  sel  The new selected value
	 */
	public void setSelected( boolean sel )
	{
		if (refcount <= 0 && restriction < SPMParameters.DISABLE)
			selected = sel;

		//System.out.println("SPMObjectInfo: restr=" + restriction + "; sel=" +
		//	   selected);
	}


	/**
	 *  Gets the selected attribute of the SPMObjectInfo object
	 *
	 *@return    The selected value
	 */
	public boolean isSelected()
	{
		return (selected == true || refcount > 0);
	}


	/**
	 *  Gets the major attribute of the SPMObjectInfo object
	 *
	 *@return    The major value
	 */
	public int getMajor()
	{
		int index = version.indexOf( '.' );
		//System.out.println(version+" "+index);
		if ( index > 0 ) {
			try {
				//return Integer.parseInt( version.substring( 0, index ) );
				//System.out.println("major(" + version + ")= " +
				// SPManagerUtils.parseInt(version, 0, index));

				return SPManagerUtils.parseInt(version, 0, index);
			} catch (Exception e) {
				return 0;
			}
		}
		else {
			return 0;
		}
	}


	/**
	 *  Gets the minor attribute of the SPMObjectInfo object
	 *
	 *@return    The minor value
	 */
	public int getMinor()
	{
		//int index = version.indexOf( '.' );
		int index = version.indexOf( '.' ) + 1;

		//System.out.println(version+" "+index);
		if ( index < version.length() ) {
			try {
				//String substring = version.substring( index + 1, version.length() );
				//if ( substring.length() < 2 )
				if (version.length() - index < 2) {
					// System.out.println("minor(" + version + ")= " +
					// SPManagerUtils.parseInt(version+"0", index, -1));

					//substring = substring + "0";
					return SPManagerUtils.parseInt(version, index, -1)*10;
				}
				//return Integer.parseInt( substring );
				//  System.out.println("minor(" + version + ")= " +
				//       SPManagerUtils.parseInt(version, index, -1));

				return SPManagerUtils.parseInt(version, index, -1);
			} catch (Exception e) {
				return 0;
			}
		}
		else {
			return 0;
		}
	}

	/**
	 *  Gets the beta attribute of the SPMObjectInfo object
	 *
	 *@return    The beta value
	 */
	public boolean isBeta()
	{
		return ( ( beta == -1 ) ? false : true );
	}


	/**
	 *  Gets the beta attribute of the SPMObjectInfo object
	 *
	 *@return    The beta value
	 */
	public int getBeta()
	{
		return beta;
	}


	/**
	 *  Gets the description attribute of the SPMObjectInfo object
	 *
	 *@return    The description value
	 */
	public String getDescription()
	{
		return description;
	}


	/**
	 *  Gets the comments attribute of the SPMObjectInfo object
	 *
	 *@return    The comments value
	 */
	public String getComments()
	{
		return comments;
	}

	/**
	 *  get the list of external dependencies.
	 */
	public Collection getExternals()
	{ return (externals != null ? externals.values() : null); }

	/**
	 *  get the change log
	 */
	public Vector getChangeLog()
	{
		return changeLog;
	}

	/**
	 *  Gets the details vector
	 *
	 *@return    The details vector
	 */
	public Vector getDetails()
	{
		return details;
	}

	/**
	 *  Sets the description attribute of the SPMObjectInfo object
	 *
	 *@param  text  The new description value
	 */
	public void setDescription( String text )
	{
		description = text.replace( '\n', ' ' );
		description = description.replaceAll( "   ", "\n\n" );
		description = description.replaceAll( "  ", "\n" );
		description = description.trim();

		String desc = new String(description);
		if (comments != null && comments.length() > 0)
			desc += "\n\n" + comments;

		setLog(SPMTranslate.text("description"), desc, 0);
	}


	/**
	 *  Sets the comment of the SPMObjectInfo object
	 *
	 *@param  text  The new comment value
	 */
	public void setComments( String text )
	{
		comments = text.replace( '\n', ' ' );
		comments = comments.replaceAll( "   ", "\n\n" );
		comments = comments.replaceAll( "  ", "\n" );
		comments = comments.trim();

		String desc = new String(comments);
		if (description != null && description.length() > 0)
			desc = description + "\n\n" + comments;

		setLog(SPMTranslate.text("description"), desc, 0);
	}

	/**
	 *  Sets the specified log entry
	 *
	 *@param  name  The log entry name
	 *@param  text  The log entry value
	 *@param  index the index of the entry to set.
	 */
	public void setLog( String name, String text, int index )
	{
		if (index >= changeLog.size())
			changeLog.add(name);
		else changeLog.set(index, name);

		if (text.startsWith("\n")) text = text.substring(1);
		if (index >= details.size())
			details.add(text);
		else details.set(index, text);
	}

	/**
	 *  compare two string and return the boolean result.
	 *
	 *  Either <i>lhs</i> or <i>rhs</i> may include a boolean operator,
	 *  otherwise <i>equals</i> is assumed. If <i>both</i> lhs and rhs
	 *  contain a boolean operator, then the one on rhs is used.
	 *
	 *  @param lhs the left-hand-side of the test. May contain a trailing
	 *     boolean operator.
	 *<br>Eg: "Fred", "Fred=", "100&gt;=", "Fred+="
	 *
	 *  @param rhs the right-hand-side of the test. May contain a leading
	 *	   boolean operator, or a trailing range indicator.
	 *<br>Eg: "Fred", "!=Fred", "&lt;100", "100+", "99-"
	 */
	public static boolean test(String lhs, String rhs)
	{
		String oper = "=";
		int cut;

		System.out.println("test: lhs:" + lhs + "; rhs:" + rhs
				+ "; rhs[0]:" + rhs.charAt(0));

		// does lhs have an operator?
		cut = lhs.indexOf('<');
		if (cut < 0) cut = lhs.indexOf('>');
		if (cut < 0) cut = lhs.indexOf('!');
		if (cut < 0) cut = lhs.indexOf('=');
		if (cut < 0) cut = lhs.indexOf('+');
		if (cut < 0) cut = lhs.indexOf('-');
		
		if (cut > 0) {
			if (cut < lhs.length()-1 && lhs.charAt(cut+1) == '=')
			    oper = lhs.substring(cut, cut+2);
			else
			    oper = lhs.substring(cut, cut+1);

			lhs = lhs.substring(0, cut);
		}

		// does rhs have an operator?
		cut = rhs.indexOf('<');
		if (cut < 0) cut = rhs.indexOf('>');
		if (cut < 0) cut = rhs.indexOf('!');
		if (cut < 0) cut = rhs.indexOf('=');
		if (cut < 0) cut = rhs.indexOf('+');
		if (cut < 0) cut = rhs.indexOf('-');
		
		if (cut >= 0) {
		    System.out.println("cut=" + cut + "; length=" + rhs.length());
		    
			if (cut < rhs.length()-1 && rhs.charAt(cut+1) == '=')
				oper = rhs.substring(cut, cut+2);
			else
				oper = rhs.substring(cut, cut+1);
			
			System.out.println("oper=" + oper);
			
			// RHS operator may be leading or trailing
			rhs = (cut >= rhs.length()-2 ? rhs.substring(0, cut) : rhs.substring(cut+oper.length()));
		}

		int comp = 0;

		System.out.println("test: lhs:" + lhs + "; rhs:" + rhs
				+ "; oper:" + oper);

		if (oper == null || oper.length() == 0) oper = "=";
		
		// try numeric comparison first
		try {
			double lval, rval;
			int pos=0;

			// compare multi-component numbers (eg, version numbers)
			if (lhs.lastIndexOf('.') > lhs.indexOf('.')
					|| rhs.lastIndexOf('.') > rhs.indexOf('.')) {

				lval = SPManagerUtils.parseVersion(lhs);
				rval = SPManagerUtils.parseVersion(rhs);

				// scale both to the same number of digits
				int delta = String.valueOf(lval).length() -
					String.valueOf(rval).length();
				
				if (delta > 0) rval *= Math.pow(10, delta);
				else if (delta < 0) lval *= Math.pow(10, delta);
			}
			else {
				lval = SPManagerUtils.parseDouble(lhs);
				rval = SPManagerUtils.parseDouble(rhs);
			}

			System.out.println("test: lval:" + lval + "; rval:" + rval);

			comp = (lval < rval ? -1 : lval > rval ? 1 : 0);

		} catch (Exception e) {
			// not numeric, compare strings (ignoring case)
			comp = lhs.compareToIgnoreCase(rhs);
		}

		switch (oper.charAt(0)) {
		case '>':
			return (oper.length() > 1 ? comp >= 0 : comp > 0);

		case '<':
			return (oper.length() > 1 ? comp <= 0 : comp < 0);

		case '+':
		    	return (comp >= 0);
		    	
		case '-':
		    	return (comp <= 0);
		    	
		case '!':
			return (comp != 0);

		case '=':
		default:
			return (comp == 0);
		}
	}
}


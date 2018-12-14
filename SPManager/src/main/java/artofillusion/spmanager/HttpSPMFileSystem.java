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

import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;

/**
 *  Description of the Class
 *
 *@author     Francois Guillet
 *@created    July, 01 2004
 */
public class HttpSPMFileSystem extends SPMFileSystem
{
    boolean unknownHost;
    private URL repository;
    private HttpStatusDialog statusDialog;
    private boolean isDownloading;
    private Vector callbacks;
    private Document pluginsDoc, objectsDoc, startupDoc, toolsDoc;
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    

    /**
     *  Constructor for the HttpSPMFileSystem object
     *
     *@param  rep  Description of the Parameter
     */
    public HttpSPMFileSystem( URL rep )
    {
        super();
        unknownHost = false;
        repository = rep;
        pluginsDoc = null;
        objectsDoc = null;
        startupDoc = null;
        toolsDoc = null;
    }


    /**
     *  Sets the repository attribute of the HttpSPMFileSystem object
     *
     *@param  rep  The new repository value
     */
    public void setRepository( URL rep )
    {
        pluginsInfo = new Vector();
        toolInfo = new Vector();
        objectInfo = new Vector();
        startupInfo = new Vector();
        initialized = false;
        unknownHost = false;
        repository = rep;
        pluginsDoc = null;
        objectsDoc = null;
        startupDoc = null;
        toolsDoc = null;
        //SPManagerPlugin.getFrame().getParameters().initHttp();
    }


    /**
     *  Gets the remoteInfo attribute of the HttpSPMFileSystem object
     *
     *@param  cb  Callback to call when done
     */
    @Override
    public void getRemoteInfo( Runnable cb )
    {
        if ( initialized )
            cb.run();
        else {
            super.initialize();
            unknownHost = false;
            if ( isDownloading )
                callbacks.add( cb );
            else {
                callbacks = new Vector();
                callbacks.add( cb );
                isDownloading = true;
                statusDialog = new HttpStatusDialog();
                (
                        new Thread()
                        {
                            @Override
                            public void run()
                            {
                                scanPlugins();
                                if ( !unknownHost )
                                    scanToolScripts();
                                if ( !unknownHost )
                                    scanObjectScripts();
                                if ( !unknownHost )
                                    scanStartupScripts();
                                isDownloading = false;
                                initialized = true;
                                for ( int i = 0; i < callbacks.size(); ++i )
                                    ( (Runnable) callbacks.elementAt( i ) ).run();
                                statusDialog.dispose();
                                statusDialog = null;
                            }
                        } ).start();
            }
        }
    }


    /**
     *  Init stuff
     */
    @Override
    public void initialize()
    {
        super.initialize();
        statusDialog = null;
        scanPlugins();
        if ( !unknownHost )
            scanToolScripts();
        if ( !unknownHost )
            scanObjectScripts();
        if ( !unknownHost )
            scanStartupScripts();
        initialized = true;
    }


    /**
     *  Scans plugins
     */
    private void scanPlugins()
    {
        if (SPManagerFrame.getParameters().getUseCache() ) 
        {
            //String s = repository.toString().replaceAll("/AoIRepository/", "");
            String s = repository.toString();
            s = s.substring(0, s.lastIndexOf('/'));
            s = s + "/cgi-bin/scripts.cgi?Plugins%20" + SPManagerPlugin.AOI_VERSION;
            SPManagerFrame.getInstance().setRemoteStatusText(SPMTranslate.text("scanningPluginsFrom", s), 5000);
        }            
        else
        {
            SPManagerFrame.getInstance().setRemoteStatusText(SPMTranslate.text("scanningPluginsFrom", repository.toString()), 5000);
        }
        if ( statusDialog != null )
            statusDialog.setText( SPMTranslate.text( "scanningPlugins" ) );
        pluginsInfo = new ArrayList<>();
        if ( SPManagerFrame.getParameters().getUseCache() )
        {
                scanFiles( "Plugins", pluginsInfo );
        }
        else
        {
            try
            {
                URL pluginsURL = new URL( repository, "Plugins/" );
                scanFiles( pluginsURL, pluginsInfo, ".jar" );

            }
            catch ( MalformedURLException e )
            {
                e.printStackTrace();
            }
        }
    }


    /**
     *  Scans tools scripts
     */
    private void scanToolScripts()
    {
        if ( SPManagerFrame.getParameters().getUseCache() ) {
            String s = repository.toString();
            s = s.substring(0, s.lastIndexOf('/'));
            s = s + "/cgi-bin/scripts.cgi?Scripts/Tools%20" + SPManagerPlugin.AOI_VERSION;
            SPManagerFrame.getInstance().setRemoteStatusText(SPMTranslate.text("scanningToolScriptsFrom", s), 5000);
        } else {
            SPManagerFrame.getInstance().setRemoteStatusText(SPMTranslate.text("scanningToolScriptsFrom", repository.toString()), 5000);
        }
            
        if ( statusDialog != null )
            statusDialog.setText( SPMTranslate.text( "scanningToolScripts" ) );
        toolInfo = new Vector();
        if ( SPManagerFrame.getParameters().getUseCache() )
        {
           scanFiles( "Scripts/Tools", toolInfo );
        }
        else
        {
             try
             {
                 URL toolScriptURL = new URL( repository, "Scripts/Tools/" );
                 scanFiles( toolScriptURL, toolInfo, ".bsh" );

             }
             catch ( MalformedURLException e )
             {
                 e.printStackTrace();
             }
         }
    }


    /**
     *  Scans objects scripts
     */
    private void scanObjectScripts()
    {
        if ( SPManagerFrame.getParameters().getUseCache() ) {
            String s = repository.toString();
            s = s.substring(0, s.lastIndexOf('/'));
            s = s + "/cgi-bin/scripts.cgi?Scripts/Objects%20" + SPManagerPlugin.AOI_VERSION;
            SPManagerFrame.getInstance().setRemoteStatusText( SPMTranslate.text( "scanningObjectScriptsFrom", s ), 5000 );
        } else {
            SPManagerFrame.getInstance().setRemoteStatusText( SPMTranslate.text( "scanningObjectScriptsFrom", repository.toString() ), 5000 );
        }
            
        if ( statusDialog != null )
            statusDialog.setText( SPMTranslate.text( "scanningObjectScripts" ) );
        objectInfo = new Vector();
        if ( SPManagerFrame.getParameters().getUseCache() )
        {
           scanFiles( "Scripts/Objects", objectInfo );
        }
        else
        {
            try
            {
                URL objectScriptURL = new URL( repository, "Scripts/Objects/" );
                scanFiles( objectScriptURL, objectInfo, ".bsh" );

            }
            catch ( MalformedURLException e )
            {
                e.printStackTrace();
            }
        }
    }


    /**
     *  Scans startup scripts
     */
    private void scanStartupScripts()
    {
        if ( SPManagerFrame.getParameters().getUseCache() ) {
            //String s = repository.toString().replaceAll("/AoIRepository/", "");
            String s = repository.toString();
            s = s.substring(0, s.lastIndexOf('/'));
            s = s + "/cgi-bin/scripts.cgi?Scripts/Startup%20" + SPManagerPlugin.AOI_VERSION;
            SPManagerFrame.getInstance().setRemoteStatusText( SPMTranslate.text( "scanningStartupScriptsFrom", s ), 5000 );
        }                    
        else
        {
            SPManagerFrame.getInstance().setRemoteStatusText( SPMTranslate.text( "scanningStartupScriptsFrom", repository.toString() ), 5000 );
        }
        if ( statusDialog != null )
            statusDialog.setText( SPMTranslate.text( "scanningStartupScripts" ) );
        startupInfo = new Vector();
        if ( SPManagerFrame.getParameters().getUseCache() )
        {
           scanFiles( "Scripts/Startup", startupInfo );
        }
        else
        {
            try
            {
                URL startupScriptURL = new URL( repository, "Scripts/Startup/" );
                scanFiles( startupScriptURL, startupInfo, ".bsh" );

            }
            catch ( MalformedURLException e )
            {
                e.printStackTrace();
            }
        }
    }


    /**
     *  Scans file on a general basis
     *
     *@param  from    URL to scan files from
     *@param  addTo   Which vector to add info to
     *@param  suffix  Scanned files suffix
     */
    private void scanFiles( URL from, List<SPMObjectInfo> addTo, String suffix )
    {
        SPMObjectInfo info;
        boolean eligible;

        Vector v = null;

        try
        {
            Object obj = from.getContent();
            if ( obj instanceof InputStream )
            {
                v = htmlFindFilesVersioning( (InputStream) ( obj ), from );
            }
            ( (InputStream) obj ).close();
        }
        catch ( IOException e )
        {
            if ( e instanceof UnknownHostException )
            {
                JOptionPane.showMessageDialog( null, from.toString() + ": " + SPMTranslate.text( "unknownHost" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
                unknownHost = true;
            }
            else if ( e instanceof FileNotFoundException )
            {
                JOptionPane.showMessageDialog( null, from.toString() + ": " + SPMTranslate.text( "fileNotFound" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
                unknownHost = true;
            }
            else
                e.printStackTrace();
        }
        if ( v != null ) {
            // sort the list
            String[] sarray = (String[]) v.toArray(EMPTY_STRING_ARRAY);
            Arrays.sort(sarray);
            for ( int i = 0; i < sarray.length; i++ )
            {
                //String s = (String) v.elementAt( i );
        	String s = sarray[i];
                System.out.println(s);
                if ( s.endsWith( suffix ) )
                {
                    //check if file candidate for update or install
                    eligible = true;

                    String name = s.substring( 0, s.length() - 4 );
                    if ( suffix.equals( ".jar" ) )
                    {
                        //look for xml file

			/*
			 *  NTJ: for AOI 2.5: The XML file name has changed to
			 *  'extensions.xml'. For compatibility, the old name
			 *  is checked if the new name does not exist.
			 */

                        eligible = true;
                        String sxml;
			sxml = s.substring(0, s.lastIndexOf('/')) +
			    "extensions.xml";

                        URL xmlURL = null;
			try {
			    xmlURL = new URL(from, sxml);
			} catch (MalformedURLException e) {
			    e.printStackTrace();
			}

                        try
                        {
                            HttpURLConnection.setFollowRedirects( false );
                            HttpURLConnection conn =
				(HttpURLConnection) xmlURL.openConnection();

			    if (conn.getResponseCode()
				!= HttpURLConnection.HTTP_OK) {
				
				eligible = false;
			    }

			    else {
				sxml = s.substring(0, s.lastIndexOf('.')) +
				    ".xml";
				xmlURL = new URL(from, sxml);

				conn =
				    (HttpURLConnection)xmlURL.openConnection();

				if (conn.getResponseCode()
				    != HttpURLConnection.HTTP_OK) {

				    eligible = false;
				}
			    }

			    if (eligible) {
				InputStreamReader in = new
				    InputStreamReader(conn.getInputStream());

				in.close();
			    }
			}
                        catch ( IOException e )
                        {
                            eligible = false;
                        }
		    }

                    info = null;
                    if ( eligible )
                    {
			System.out.println("adding: " + s);
                        try
                        {
                            info = new SPMObjectInfo( new URL( from, s ) );
                        }
                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                    if ( info != null )
                        addTo.add( info );
                }
            }
        }
    }

    /**
     *  Scans file using server cgi
     *
     *@param  dir     directory to fetch scripts from
     *@param  addTo   Which vector to add info to
     */
    private void scanFiles( String dir, List<SPMObjectInfo> addTo )
    {

        URL cgiUrl = null;
        try
        {
            //String s = repository.toString().replaceAll("/AoIRepository/", "");
            String s = repository.toString();
	    String err = "";
	    s = s.substring(0, s.lastIndexOf('/'));
            cgiUrl = new URL( s + "/cgi-bin/scripts.cgi?" + dir + "%20" + SPManagerPlugin.AOI_VERSION );
            //cgiUrl = new URL( s + "/cgi-bin/scripts.cgi?-z%20" + dir + "%20" + SPManagerPlugin.AOI_VERSION );
            //cgiUrl = new URL( s + "/cgi-bin/RepoServer");
            //cgiUrl = new URL( s + "/cgi-bin/RepoServer?HTTP_X_AOI_Dir=" + dir + "&HTTP_X_AOI_Version="
    	    //	+ SPManagerPlugin.AOI_VERSION);
            
            String content = null;
            boolean received = false;
            int attempts = 0;
            System.out.println( cgiUrl );
            while (!received && attempts++ < 5 )
            {
		HttpURLConnection conn =
		    (HttpURLConnection) cgiUrl.openConnection();

		conn.setRequestProperty("Accept-Encoding", "deflate, gzip");
		conn.setRequestProperty("X-AOI-Version", SPManagerPlugin.AOI_VERSION);
		conn.setRequestProperty("X-AOI-Dir", dir);
		
		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
		    err = conn.getResponseMessage();
		    Thread.sleep(250);
		    continue;
		}

                //InputStream is= cgiUrl.openStream();
		InputStream is= conn.getInputStream();
                is = new BufferedInputStream( is );
                //Document doc = SPManagerUtils.builder.parse( bis );

                System.out.println("Content-Encoding: " + conn.getHeaderField("Content-Encoding"));
                
                if (conn.getHeaderField("Content-Encoding").equalsIgnoreCase("deflate"))
                	is = new InflaterInputStream(is, new Inflater(true));

                else if (conn.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip"))
                	is = new GZIPInputStream(is);

                /*
                byte[] prolog = new byte[40];
                int chunk = is.read(prolog, 0, 40);
                System.out.println("first " + chunk + " bytes >>" + new String(prolog, "UTF-8") + "<<");
                */
                
                InputSource input = null;
                try { input = new InputSource(new InputStreamReader(is, "UTF-8")); }
                catch (Exception e) { e.printStackTrace(); }
                
                System.out.println("Encoding: " + input.getEncoding());
                
                Document doc = SPManagerUtils.builder.parse( input );
                NodeList tst = doc.getElementsByTagName( "scriptcollection" );
                if (tst.getLength() > 0)
                {
                    received = true;
                    NodeList nl = doc.getElementsByTagName( "scriptreference" );
                    Node script;
                    String location = "";
                    long length = 0;
                    for (int i = 0; i < nl.getLength(); i++)
                    {
                        Node n = nl.item(i);
                        NodeList nnl = n.getChildNodes();
                        script = null;
                        location = null;
                        for (int j = 0; j < nnl.getLength(); j++)
                        {
                            if ( "scriptlocation".equals( nnl.item(j).getNodeName() ) )
                            {
                                location = repository.toString() + "/" + dir +"/" + nnl.item(j).getChildNodes().item(0).getNodeValue();
                                //System.out.println( "location found" );
                            }
                            else if ( "scriptlength".equals( nnl.item(j).getNodeName() ) )
                            {
                                length = Long.parseLong( nnl.item(j).getChildNodes().item(0).getNodeValue() );
                                //System.out.println( "length found" );
                            }
			    else if ("extension".equals(nnl.item(j).getNodeName()))
				script = nnl.item(j);
                            else if ("script".equals( nnl.item(j).getNodeName() ))
                                script = nnl.item(j);
                        }
                        //System.out.println( location + " / " + script + " / " + length);
                        if ( script != null && location != null )
                        {
                            addTo.add( new SPMObjectInfo( script, new URL(location) , length) );
                        }
                    }

                }
                is.close();
            }
            if (!received)
            {
                JOptionPane.showMessageDialog( null, cgiUrl.toString() + ": " + SPMTranslate.text( "scriptServerFailed" ), SPMTranslate.text( "error" ) + " " +
					       err,
					       JOptionPane.ERROR_MESSAGE );
                return;
            }


        }
        catch ( Exception e )
        {
            if ( e instanceof UnknownHostException )
            {
                JOptionPane.showMessageDialog( null, cgiUrl.toString() + ": " + SPMTranslate.text( "unknownHost" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
                unknownHost = true;
            }
            else if ( e instanceof FileNotFoundException )
            {
                JOptionPane.showMessageDialog( null, cgiUrl.toString() + ": " + SPMTranslate.text( "fileNotFound" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
                unknownHost = true;
            }
            else {
                JOptionPane.showMessageDialog( null, cgiUrl.toString() + ": " + SPMTranslate.text( "httpError" ), SPMTranslate.text( "error" ) + ": " + e.getMessage(), JOptionPane.ERROR_MESSAGE );
                e.printStackTrace();
	    }
        }
        
        // sort the result
        SPMObjectInfo left, right;
        int i, j;
        for (i = addTo.size()-1; i > 0; i--) {
            j = i;
            right = (SPMObjectInfo) addTo.get(i);

            while (j > 0) {
        	left = (SPMObjectInfo) addTo.get(j-1);
        	if (right.getName().compareTo(left.getName()) >= 0) break;
        	j--;
            }

            // relocate
            if (j < i) {
        	addTo.remove(i);
        	addTo.add(j, right);
            }
        }
    }

    /**
     *  Description of the Method
     *
     *@param  fileName          Description of the Parameter
     *@param  from              Description of the Parameter
     *@param  status            Description of the Parameter
     *@param  downloadedLength  Description of the Parameter
     *@param  lengthToDownload  Description of the Parameter
     *@return                   Description of the Return Value
     */
    public static long downloadRemoteTextFile( URL from, String fileName, long size, StatusDialog status, long totalDownload, long downloadedLength, ArrayList errors )
    {
	//if (fileName.endsWith(".upd")) return 0;

        BufferedReader in = null;
        BufferedWriter file = null;
        long initialValue = downloadedLength;
        //System.out.println( from + ": downloadedLength :" + downloadedLength + " " + lengthToDownload );
        try
        {
	    HttpURLConnection conn = (HttpURLConnection) from.openConnection();
	    
	    conn.setRequestProperty("Cache-Control", "no-cache");
	    conn.setRequestProperty("Accept-Encoding", "deflate, gzip");

	    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
		new BStandardDialog("SPManager", new String[] {
					SPMTranslate.text("httpError"),
					conn.getResponseMessage() +
					" (" + conn.getResponseCode() + ")",
				    }, BStandardDialog.ERROR)
		    .showMessageDialog(SPManagerFrame.getInstance());

		return 0;
	    }

	    InputStream is = conn.getInputStream();
	    
	    System.out.println("Content-Encoding: " + conn.getHeaderField("Content-Encoding"));
	    
	    if (conn.getHeaderField("Content-Encoding").equalsIgnoreCase("deflate"))
	    	is = new InflaterInputStream(is, new Inflater(true));

	    if (conn.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip"))
	    	is = new GZIPInputStream(is);

            //in = new BufferedReader( new InputStreamReader( from.openStream() ) );
	    in = new BufferedReader( new InputStreamReader( is ) );
                
	    file = new BufferedWriter( new FileWriter(fileName) );

            double a;
            double b = totalDownload;
            int value;
            int newValue;
            value = status.getBarValue();

            int i = in.read();
            while ( i != -1 )
            {
                file.write( i );
                i = in.read();
                if ( status != null )
                {
                    ++downloadedLength;
                    a = downloadedLength;
                    newValue = (int) Math.round( ( a * 100.0 ) / b );
                    if ( newValue > value )
                    {
                        status.setBarValue( newValue );
                        status.setProgressText( newValue + "%" );
                        value = newValue;
                    }
                }
            }

	    file.flush();
	    file.close();

            //System.out.println( "downloadedLength :" + downloadedLength + " " + ( downloadedLength - initialValue ) );

	    // check we got the expected data
	    long received = downloadedLength - initialValue;
	    if (received != size)
		throw new IOException("SPManager: file incomplete." +
				      " Only received " + received +
				      " bytes of " + size);

        }
        catch ( Exception e)
        {
	    /*
            e.printStackTrace();
            JOptionPane.showMessageDialog( null, from.toString() + ": " + SPMTranslate.text( "fileNotFound" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
	    */
	    errors.add(SPMTranslate.text("error") + "(" + fileName + ")" + e);
        }
	/*
        catch ( IOException e )
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog( null, from.toString() + ": " + SPMTranslate.text( "ioError" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
        }
	*/
        finally
        {
            try
            {
                if ( in != null )
                    in.close();
                if ( file != null )
                    file.close();
            }
            catch ( IOException e )
            {
                //e.printStackTrace();
		System.out.println("SPManager: error closing " + fileName +
				   ": " + e);
		//errors.add("error closing " + fileName);
            }

	    //if (update.exists()) update.delete();
        }
        return downloadedLength - initialValue;
    }


    /**
     *  Description of the Method
     *
     *@param  from              Description of the Parameter
     *@param  fileName          Description of the Parameter
     *@param  status            Description of the Parameter
     *@param  lengthToDownload  Description of the Parameter
     *@param  downloadedLength  Description of the Parameter
     *@return                   Description of the Return Value
     */
    public static long downloadRemoteBinaryFile( URL from, String fileName, long size, StatusDialog status, long totalDownload, long downloadedLength, ArrayList errors )
    {
	System.out.println("download: size=" + size +
			   "; total=" + totalDownload +
			   "; downloaded=" + downloadedLength);

	//if (fileName.endsWith(".upd")) return 0;

	File update = new File(fileName);

	Thread thread = Thread.currentThread();

        BufferedInputStream in = null;
        BufferedOutputStream file = null;
        long initialValue = downloadedLength;
        try
        {
	    HttpURLConnection conn = (HttpURLConnection) from.openConnection();

	    conn.setRequestProperty("Cache-Control", "no-cache");
	    
	    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
		new BStandardDialog("SPManager", new String[] {
					SPMTranslate.text("httpError"),
					conn.getResponseMessage() +
					" (" + conn.getResponseCode() + ")",
				    }, BStandardDialog.ERROR)
		    .showMessageDialog(SPManagerFrame.getInstance());

		return 0;
	    }

            //in = new BufferedInputStream( from.openStream() );
	    in = new BufferedInputStream( conn.getInputStream() );

            file = new BufferedOutputStream(new FileOutputStream(update));

            double a;
            double b = totalDownload;
            int value;
            int newValue;
            value = status.getBarValue();
	    String mod="", newMod;

	    if (b <= 0) status.setIdle(true);

            int result = 0;
            while ( ( result = in.read() ) != -1 )
            {
		if (thread.interrupted()) {
		    thread.interrupt();

		    if (!update.delete()) {
			RandomAccessFile raf =
			    new RandomAccessFile(update, "rw");

			raf.setLength(0);
			raf.close();
		    }

		    throw new InterruptedException("download cancelled: " +
						   fileName);
		}

                file.write( (byte) result );
                ++downloadedLength;
                a = downloadedLength;

		if (b > a) {
		    newValue = (int) Math.round( a * 100.0 / b );
		    if ( newValue > value ) {
			status.setBarValue( newValue );
			status.setProgressText( newValue + "%" );
			value = newValue;
		    }
		}
		else {
		    newMod = (a > 1000000
			      ? ((int) (a/100000.0)) + " MB"
			      : a > 1000
			      ? ((int) (a/1000.0)) + " kB"
			      : a + " bytes");

		    if (!newMod.equals(mod)) status.setProgressText(newMod);
		}
	    }

	    file.flush();
	    file.close();

	    // check we got the expected data
	    if (size > 0) {
		long received = downloadedLength - initialValue;
		if (received != size)
		    throw new IOException("SPManager: file incomplete." +
					  " Only received " + received +
					  " bytes of " + size);
	    }

	    // test validity of zipfiles	    
	    if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
		new ZipFile(update);
		System.out.println("SPManager: ZipFile ok");
	    }

        }
        catch ( Exception e)
        {
	    errors.add(SPMTranslate.text("error") + "(" + fileName + ")" + e);

	    /*	    
            e.printStackTrace();

	    JOptionPane.showMessageDialog( null, from.toString() + ": " + SPMTranslate.text( "errMsg" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
	    */
        }
	/*
        catch ( IOException e )
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog( null, from.toString() + ": " + SPMTranslate.text( "ioError" ), SPMTranslate.text( "error" ), JOptionPane.ERROR_MESSAGE );
        }
	*/
        finally
        {
            try
            {
                if ( in != null )
                    in.close();
                if ( file != null )
                    file.close();
            }
            catch ( IOException e )
            {
		System.out.println("SPManager: error closing " + fileName +
				   ": " + e);

                //e.printStackTrace();
		//errors.add("Error closing " + fileName);
            }

	    //if (update.exists()) update.delete();
        }
        return downloadedLength - initialValue;
    }

    /**
     *  Description of the Method
     *
     *@param  is  Description of the Parameter
     *@return     Description of the Return Value
     */
    private Vector htmlFindFiles( InputStream is )
    {
        Vector v = new Vector();

        HtmlParserCallback callback = new HtmlParserCallback( v );
//        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( is ) );
		BufferedReader bufferedReader = null;
        try {bufferedReader = new BufferedReader( new InputStreamReader( is, "UTF-8" ) ); }
        catch (Exception e) { e.printStackTrace(); return v; }
        try
        {
            new ParserDelegator().parse( bufferedReader, callback, false );
            is.close();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return v;
    }


    /**
     *  Description of the Method
     *
     *@param  is    Description of the Parameter
     *@param  from  Description of the Parameter
     *@return       Description of the Return Value
     */
    private Vector htmlFindFilesVersioning( InputStream is, URL from )
    {
        Vector v = new Vector();

        HtmlVersioningParserCallback callback = new HtmlVersioningParserCallback( v, from );
        //BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( is ) );
        BufferedReader bufferedReader = null;
        try { bufferedReader = new BufferedReader( new InputStreamReader( is, "UTF-8" ) ); }
        catch (Exception e) { e.printStackTrace(); return v; }
        try
        {
            new ParserDelegator().parse( bufferedReader, callback, false );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return v;
    }


    /**
     *  Description of the Class
     *
     *@author     pims
     *@created    1 juillet 2004
     */
    private class HtmlParserCallback extends HTMLEditorKit.ParserCallback
    {
        private Vector v;


        /**
         *  Constructor for the HtmlParserCallback object
         *
         *@param  v  Description of the Parameter
         */
        public HtmlParserCallback( Vector v )
        {
            this.v = v;
        }


        /**
         *  Description of the Method
         *
         *@param  data  Description of the Parameter
         *@param  pos   Description of the Parameter
         */
        @Override
        public void handleText( char[] data, int pos )
        {
            System.out.println( "handleText " + new String( data ) + " " + pos );
        }


        /**
         *  Description of the Method
         *
         *@param  t    Description of the Parameter
         *@param  a    Description of the Parameter
         *@param  pos  Description of the Parameter
         */
        @Override
        public void handleStartTag( HTML.Tag t, MutableAttributeSet a, int pos )
        {
            System.out.println( "StartTag :" + t + ":" + a + ":" + pos );
            if ( t == HTML.Tag.A )
            {
                String s = (String) a.getAttribute( HTML.Attribute.HREF );
                v.add( s );
            }

        }


        /**
         *  Description of the Method
         *
         *@param  t    Description of the Parameter
         *@param  a    Description of the Parameter
         *@param  pos  Description of the Parameter
         */
        public void handleEndTag( HTML.Tag t, MutableAttributeSet a, int pos )
        {
            System.out.println( "EndTag :" + t + ":" + a + ":" + pos );
        }
    }



    /**
     *  Description of the Class
     *
     *@author     pims
     *@created    1 juillet 2004
     */
    private class HtmlVersioningParserCallback extends HTMLEditorKit.ParserCallback
    {
        private Vector v;
        private URL from;


        /**
         *  Constructor for the HtmlParserCallback object
         *
         *@param  v     Description of the Parameter
         *@param  from  Description of the Parameter
         */
        public HtmlVersioningParserCallback( Vector v, URL from )
        {
            this.v = v;
            this.from = from;
        }


        /**
         *  Description of the Method
         *
         *@param  data  Description of the Parameter
         *@param  pos   Description of the Parameter
         */
        @Override
        public void handleText( char[] data, int pos )
        {
            System.out.println( "handleText " + new String( data ) + " " + pos );
        }


        /**
         *  Description of the Method
         *
         *@param  AoIversion  Description of the Parameter
         *@param  versions    Description of the Parameter
         *@return             Description of the Return Value
         */
        private String findCorrectVersion( String AoIversion, String[] versions )
        {
            int maj;
            int min;
            //System.out.println( "AoI " + AoIversion );
            //for ( int i = 0; i < versions.length; ++i )
            //    System.out.println( versions[i] );
            String[] tmps = AoIversion.split( "[^0-9]" );
            maj = Integer.parseInt( tmps[0] );
            min = Integer.parseInt( tmps[1] );
            String result = null;
            while ( result == null && ( maj >= 0 ) )
            {
                for ( int i = 0; i < versions.length; ++i )
                {
                    String[] versionsplit = versions[i].split( " " );
                    tmps = versionsplit[0].split( "[^0-9]" );
                    if ( ( maj == Integer.parseInt( tmps[0] ) ) && ( min == Integer.parseInt( tmps[1] ) ) )
                        result = versionsplit[2];
                }
                if ( result == null )
                {
                    --min;
                    if ( min < 0 )
                    {
                        min = 9;
                        --maj;
                    }
                }
            }
            if ( result == null )
            {
                //System.out.println( "no script for version" );
                return "";
            }
            if ( result.equals( "-" ) )
            {
                //System.out.println( "-" );
                return "";
            }
            //System.out.println( "result : " + result );
            return result;
        }


        /**
         *  Description of the Method
         *
         *@param  t    Description of the Parameter
         *@param  a    Description of the Parameter
         *@param  pos  Description of the Parameter
         */
        @Override
        public void handleStartTag( HTML.Tag t, MutableAttributeSet a, int pos )
        {
            System.out.println( "StartTag :" + t + ":" + a + ":" + pos );
            if ( t == HTML.Tag.A )
            {
                String s = (String) a.getAttribute( HTML.Attribute.HREF );
                if ( s.endsWith( "/" ) )
                {
                    //directory : get the location of content text file
                    String[] ss = s.split( "/" );
                    s = ss[ss.length - 1];
                    String txt = s + "/" + s + ".txt";
                    try
                    {
                        URL fileURL = new URL( from, txt );
                        System.out.println( "fileURL " + fileURL );
                        HttpURLConnection.setFollowRedirects( false );
                        HttpURLConnection connection = (HttpURLConnection) fileURL.openConnection();
                        String header = connection.getHeaderField( 0 );
                        InputStreamReader in = new InputStreamReader( connection.getInputStream() );
                        //read the contents of the contents file
                        int status = 0;
                        String content = "";
                        while ( status != -1 )
                        {
                            try
                            {
                                status = in.read();
                                if ( status != -1 )
                                    content += (char) status;
                            }
                            catch ( IOException e )
                            {
                                e.printStackTrace();
                            }
                        }
                        in.close();
                        //Get the file names
                        String[] versions = content.split( "\n" );
                        //send to findCorrectVersion
                        String name = findCorrectVersion( SPManagerPlugin.AOI_VERSION, versions );
                        if ( !name.equals( "" ) )
                            v.add( s + "/" + name );

                    }
                    catch ( IOException e )
                    {
                        return;
                    }
                }
            }

        }


        /**
         *  Description of the Method
         *
         *@param  t    Description of the Parameter
         *@param  a    Description of the Parameter
         *@param  pos  Description of the Parameter
         */
        public void handleEndTag( HTML.Tag t, MutableAttributeSet a, int pos )
        {
            System.out.println( "EndTag :" + t + ":" + a + ":" + pos );
        }
    }


    /**
     *  Description of the Class
     *
     *@author     pims
     *@created    1 juillet 2004
     */
    private class HttpStatusDialog extends BDialog
    {
        private BLabel label;
        private BProgressBar progressBar;


        /**
         *  Constructor for the StatusDialog object
         */
        public HttpStatusDialog()
        {
            super( SPManagerPlugin.getFrame(), SPMTranslate.text( "remoteStatus" ), false );
            ColumnContainer cc = new ColumnContainer();
            LayoutInfo layout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.NONE, new Insets( 10, 10, 10, 10 ), new Dimension( 0, 0 ) );
            cc.add( label = SPMTranslate.bLabel( "status" ), layout );
            label.setText( SPMTranslate.text( "scanningPlugins" ) );
            //cc.add( SPMTranslate.bLabel( "connectedTo", new String[]{repository.toString()} ) );
            layout = new LayoutInfo( LayoutInfo.CENTER, LayoutInfo.HORIZONTAL, new Insets( 10, 10, 10, 10 ), new Dimension( 0, 0 ) );
            cc.add( progressBar = new BProgressBar(), layout );
            progressBar.setIndeterminate( true );
            setContent( cc );
            pack();
            centerAndSizeWindow();
            setVisible( true );
            layoutChildren();
            addEventLink( WindowClosingEvent.class, this, "doClose" );
        }



        /**
         *  Sets the text attribute of the StatusDialog object
         *
         *@param  text  The new text value
         */
        public void setText( String text )
        {
            label.setText( text );
            layoutChildren();
        }


        /**
         *  Description of the Method
         */
        private void centerAndSizeWindow()
        {
	    UIUtilities.centerDialog(this, (WindowWidget) getParent());

	    /*
            Dimension d1 = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension d2 = getComponent().getSize();
            int x;
            int y;

            d2.width = new Long( Math.round( d2.width * 1.3 ) ).intValue();
            //System.out.println( d1 );
            x = ( d1.width - d2.width ) / 2;
            y = ( d1.height - d2.height ) / 2;
            if ( x < 0 )
                x = 0;
            if ( y < 0 )
                y = 0;
            setBounds( new Rectangle( x, y, d2.width, d2.height + 2 ) );
	    */
        }


        /**
         *  Description of the Method
         */
        private void doClose()
        {
            setVisible( false );
        }
    }
}


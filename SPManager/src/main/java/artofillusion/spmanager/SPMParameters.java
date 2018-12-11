
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
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import javax.swing.*;
import buoy.widget.*;
import buoy.event.*;

import artofillusion.ui.*;

/**
 *  Description of the Class
 *
 *@author     François Guillet
 *@created    20 mars 2004
 */
public class SPMParameters
{
    private static Vector repositories;
    private static int current;
    private static String repoList;
    private static HashMap filters;
    private static boolean useProxy;
    private static String proxyHost;
    private static String proxyPort;
    private static String username;
    private static String password;
    private static boolean changed;
    private StringEncrypter se;
    private URL repListURL;
    private boolean useCache;

    /**  enum for filter values - in order of increasing restriction  */
    public static final int DEFAULT	= 0;
    public static final int ENABLE	= 1;
    public static final int MARK	= 2;
    public static final int CONFIRM	= 3;
    public static final int DISABLE	= 4;
    public static final int HIDE	= 5;
    public static final int LAST_FILTER = 6;
    public static final int FILTER_MODULO = LAST_FILTER;

    public static final String FILTER_NAMES[] = {
	SPMTranslate.text("filtDefault"), SPMTranslate.text("filtEnable"),
	SPMTranslate.text("filtMark"), SPMTranslate.text("filtConfirm"),
	SPMTranslate.text("filtDisable"), SPMTranslate.text("filtHide"),
	"default", "enable", "mark", "confirm", "disable", "hide"
    };

    /**
     *  Constructor for the SPMParameters object
     */
    public SPMParameters()
    {
        repositories = new Vector();

        repositories.add( "http://aoisp.sourceforge.net/AoIRepository/" );
        //hack
        //repositories.add( "http://localhost/AoIRepository/" );
        // NTJ: testing

	filters = new HashMap();
	filters.put("beta", "mark");
	filters.put("earlyAccess", "confirm");
	filters.put("experimental", "hide");

        proxyHost = "";
        proxyPort = "";
        username = "";
        password = "";
        current = 0;
        useProxy = false;
        useCache = true;
        se = null;
        loadPropertiesFile();
        initHttp();
        //getRepositoriesList( false );
    }


    /**
     *  Description of the Method
     */
    private void loadPropertiesFile()
    {
        File f = new File( System.getProperty( "user.home" ), ".spmanagerprefs" );
        if ( !f.exists() )
        {
	    savePropertiesFile();
            return;
        }
        try
        {
            InputStream in = new BufferedInputStream( new FileInputStream( f ) );
            Properties props = new Properties();
            props.load( in );
            parseProperties( props );
            in.close();
        }
        catch ( IOException ex )
        {
            ex.printStackTrace();
        }

    }


    /**
     *  Gets the repositories list
     *
     *@param  forceUpdate  Description of the Parameter
     */
    public void getRepositoriesList( boolean forceUpdate )
    {
        if ( forceUpdate )
        {
            (
                new Thread()
                {
                    public void run()
                    {
                        getThreadedRepositoriesList( true );
                    }
                } ).start();
        }
        else
        {
            (
                new Thread()
                {
                    public void run()
                    {
                        getThreadedRepositoriesList( false );
                    }
                } ).start();
        }
    }


    /**
     *  Gets the threadedRepositoriesList attribute of the SPMParameters object
     *
     *@param  forceUpdate  Description of the Parameter
     */
    private void getThreadedRepositoriesList( boolean forceUpdate )
    {
	final BDialog dlg = new BDialog(SPManagerFrame.getInstance(),
					SPMTranslate.text("remoteStatus"),
					true);

	dlg.setEnabled(true);

	(new Thread() {
		public void run()
		{
		    try {
			Thread.sleep(500);
			if (dlg.isEnabled()) {
			    dlg.setContent(new BLabel(SPMTranslate.text("waiting")));

			    dlg.pack();
			    UIUtilities.centerWindow(dlg);
			    if (dlg.isEnabled()) dlg.setVisible(true);
			}
		    } catch (Exception e) {}
		}
	    }).start();

        boolean updated = false;
        //hack
        /*if (true)
        {
            SwingUtilities.invokeLater(
                    new Thread()
                    {
                        public void run()
                        {
                            SPManagerFrame.getInstance().updatePanes();
                        }
                    } );
            return;
        } */

        repListURL = null;
        //try to get a new repositories definition file
        try
        {
            repListURL = new URL( "http://aoisp.sourceforge.net/SPRepositories.txt" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        SPManagerFrame.getInstance().setRemoteStatusText( SPMTranslate.text( "fetchingRepositoriesList" ) + " " + repListURL, -1 );
        try
        {
	    HttpURLConnection conn =
		(HttpURLConnection) repListURL.openConnection();

	    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
		new BStandardDialog("SPManager", new String[] {
					SPMTranslate.text("noRepoList"),
					conn.getResponseMessage() +
					" (" + conn.getResponseCode() + ")",
				    }, BStandardDialog.ERROR)
		    .showMessageDialog(SPManagerFrame.getInstance());

		//conn.disconnect();
		return;
	    }

            //LineNumberReader rd = new LineNumberReader(new InputStreamReader(repListURL.openStream()));

	    LineNumberReader rd = new LineNumberReader(new InputStreamReader(conn.getInputStream()));

            String repoName;
	    boolean modified = true;
	    String currentString = (String) repositories.elementAt( current );

	    System.out.println("current repo (" + current + "): " +
			       currentString);

	    int previous = current;
	    current = 0;
	    Vector newRepositories = new Vector();
	    while (true) {
		repoName = rd.readLine();
		if (repoName == null || repoName.length() == 0) break;
		
		if (repoName.startsWith("<DOC")) {
		    System.out.println("Error retrieving repositories list: ");
		    SPManagerFrame.getInstance().setRemoteStatusText( SPMTranslate.text( "noRepoList" ), -1 );
		    

		    current = previous;
		    return;
		}

		repoName = repoName.trim();
		if (repoName.endsWith("/"))
		    repoName = repoName.substring(0, repoName.length()-1);

		System.out.println("repoName: " + repoName + "<<");

		newRepositories.addElement( repoName );
		if ( repoName.equals( currentString ) ) {
		    current = rd.getLineNumber() - 1;

		    System.out.println("new current=" + current);
		    modified = false;
		}
		//System.out.println( i + " " + (String) newRepositories.elementAt( i ) );
	    }

	    repositories = newRepositories;
            rd.close();

            if ( modified )
            {
                SwingUtilities.invokeLater(
					   //new Thread()
					   new Runnable()
                    {
                        public void run()
                        {
                            SPManagerFrame.getInstance().updatePanes();
                        }
                    } );
                updated = true;
            }
        }
        catch ( Exception e )
        {
            if ( !( ( e instanceof UnknownHostException ) || ( e instanceof SocketException ) ) )
                e.printStackTrace();
            SPManagerFrame.getInstance().setRemoteStatusText( SPMTranslate.text( "unknownRepositoriesHost", new String[]{repListURL.toString()} ), -1 );
            //live on saved repositories.
            //System.out.println( "Remote repository list : Not connected. Try later" );
        }
        finally
        {
	    // close and displose of the dialog
	    dlg.setEnabled(false);
	    dlg.setVisible(false);
	    dlg.dispose();

            SPManagerFrame.getInstance().setRemoteStatusTextDuration( 3000 );
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            if ( ( !updated ) && forceUpdate )
            {
                SwingUtilities.invokeLater(
					   //new Thread()
					   new Runnable()
                    {
                        public void run()
                        {
                            SPManagerFrame.getInstance().updatePanes();
                        }
                    } );
            }

        }
    }


    /**
     *  Description of the Method
     */
    private void savePropertiesFile()
    {

        File f = new File( System.getProperty( "user.home" ), ".spmanagerprefs" );
        try
        {
            OutputStream out = new BufferedOutputStream( new FileOutputStream( f ) );
            Properties props = newProperties();
            props.store( out, "Scripts & Plugins Manager Preferences File" );
            out.close();
        }
        catch ( IOException ex )
        {
            ex.printStackTrace();
        }

    }


    /**
     *  Description of the Method
     *
     *@param  p  Description of the Parameter
     */
    private void parseProperties( Properties p )
    {
        int i = 0;
        String s = null;

        repositories.clear();
        current = 0;

        while ( i != -1 )
        {
            s = p.getProperty( "URL_" + i );
            if ( s == null )
            {
                i = -1;
            }
            else
            {
		s.trim();
		if (s.endsWith("/")) s = s.substring(0, s.length()-1);

                repositories.add( s );
                ++i;
            }
        }

        s = p.getProperty( "default", "0" );
        try
        {
            current = Integer.parseInt( s );
            if ( current > repositories.size() )
            {
                current = 0;
            }
        }
        catch ( Exception e )
        {
            current = 0;
            System.out.println( "SPManager : Wrong default URL index in properties file." );
        }

        //hack
        //repositories.clear();
        //current = 0;
        //repositories.add( "http://localhost/AoIRepository/" );

	Iterator iter = p.entrySet().iterator();
	Map.Entry entry;
	while (iter.hasNext()) {
	    entry = (Map.Entry) iter.next();
	    s = (String) entry.getKey();
	    if (s.startsWith("FILTER_")) {
		filters.put(s.substring("FILTER_".length()),
			   (String) entry.getValue());
	    }
	}

	// initialise an empty filter set
	if (filters.size() == 0) {
	    filters.put("beta", "mark");
	    filters.put("earlyAccess", "confirm");
	    filters.put("experimental", "hide");
	}

        proxyHost = p.getProperty( "proxyHost", "" );
        proxyPort = p.getProperty( "proxyPort", "" );
        username = p.getProperty( "username", "" );
        password = p.getProperty( "password", "" );
        if ( !password.equals( "" ) )
        {
            if ( se == null )
                se = new StringEncrypter( "SPMan8ger" );
            password = se.decrypt( password );
        }
        s = p.getProperty( "useProxy", "false" );
        try
        {
            useProxy = Boolean.valueOf( s ).booleanValue();
        }
        catch ( Exception e )
        {
            useProxy = false;
            System.out.println( "SPManager : Invalid use of proxy setting in properties file: useProxy=" + s);
        }
        useCache = Boolean.valueOf( p.getProperty( "usecache", "true" )).booleanValue();

    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    private Properties newProperties()
    {
        Properties p = new Properties();

        for ( int i = 0; i < repositories.size(); ++i )
        {
            p.setProperty( "URL_" + i, (String) repositories.elementAt( i ) );
        }
        p.setProperty( "default", String.valueOf( current ) );

	Iterator iter = filters.entrySet().iterator();
	Map.Entry entry;
	while (iter.hasNext()) {
	    entry = (Map.Entry) iter.next();
	    p.setProperty("FILTER_" + entry.getKey(),
			  (String) entry.getValue());
	}

        p.setProperty( "proxyHost", proxyHost );
        p.setProperty( "proxyPort", proxyPort );
        p.setProperty( "username", username );
        String pwd = "";
        if ( !password.equals( "" ) )
        {
            if ( se == null )
                se = new StringEncrypter( "SPMan8ger" );
            pwd = se.encrypt( password );
        }
        p.setProperty( "password", pwd );
        p.setProperty( "useProxy", String.valueOf( useProxy ) );
        p.setProperty( "usecache", String.valueOf( useCache ));
        return p;
    }


    /**
     *  Sets the uRLs attribute of the SPMParameters object
     *
     *@param  urls           The new uRLs value
     *@param  selectedIndex  The new uRLs value
     */
    public void setURLs( String[] urls, int selectedIndex )
    {
        repositories.clear();
        for ( int i = 0; i < urls.length; ++i )
        {
            repositories.add( urls[i] );
        }
        current = selectedIndex;
        savePropertiesFile();
    }


    /**
     *  Gets the repositories attribute of the SPMParameters object
     *
     *@return    The repositories value
     */
    public String[] getRepositories()
    {
        String[] s = new String[repositories.size()];
        for ( int i = 0; i < repositories.size(); ++i )
        {
            s[i] = new String( (String) repositories.elementAt( i ) );
        }
        return s;
    }


    /**
     *  Gets the currentRepository attribute of the SPMParameters object
     *
     *@return    The currentRepository value
     */
    public URL getCurrentRepository()
    {
        URL url = null;
        try
        {
	    // NTJ: DEBUG!!
	    //if (true) return new URL("http://localhost/AoIRepository/");

            url = new URL( (String) repositories.elementAt( current ) );
        }
        catch ( MalformedURLException e )
        {
            e.printStackTrace();
        }
        return url;
    }


    /**
     *  Sets the currentRepository attribute of the SPMParameters object
     *
     *@param  c  The new currentRepository value
     */
    public void setCurrentRepository( int c )
    {
        current = c;
    }


    /**
     *  Gets the currentRepositoryIndex attribute of the SPMParameters object
     *
     *@return    The currentRepositoryIndex value
     */
    public int getCurrentRepositoryIndex()
    {
	if (current < 0) getRepositoriesList( false );
        return current;
    }

    /**
     *  return the current filter map
     */
    public HashMap getFilters()
    { return filters; }

    /**
     *  get a filter value
     */
    public int getFilter(String name)
    { return getFilterType((String) filters.get(name)); }

    public String getFilterString(String name)
    { return (String) filters.get(name); }

    /**
     *  return the filter type corresponding to this filter value
     */
    public static int getFilterType(String val)
    {
	if (val == null || val.length() == 0) return DEFAULT;

	for (int i = 0; i < FILTER_NAMES.length; i++)
	    if (val.equals(FILTER_NAMES[i])) return i % FILTER_MODULO;

	return DEFAULT;
    }

    public static String getFilterType(int type)
    {
	if (type < 0) return FILTER_NAMES[0];
	return FILTER_NAMES[type % FILTER_MODULO];
    }


    /**
     *  add a filter to the list
     */
    public void addFilter(String name, String value)
    { filters.put(name, value); }

    public void addFilter(String name, int type)
    { filters.put(name, getFilterType(type)); }
    
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public boolean useProxy()
    {
        return useProxy;
    }


    /**
     *  Gets the proxyHost attribute of the SPMParameters object
     *
     *@return    The proxyHost value
     */
    public String getProxyHost()
    {
        return proxyHost;
    }


    /**
     *  Gets the proxyPort attribute of the SPMParameters object
     *
     *@return    The proxyPort value
     */
    public String getProxyPort()
    {
        return proxyPort;
    }


    /**
     *  Gets the username attribute of the SPMParameters object
     *
     *@return    The username value
     */
    public String getUsername()
    {
        return username;
    }


    /**
     *  Gets the password attribute of the SPMParameters object
     *
     *@return    The password value
     */
    public String getPassword()
    {
        return password;
    }

    /**
     *  Returns true if cached headers file is to be used
     *
     *@return    The password value
     */
    public boolean getUseCache()
    {
        return useCache;
    }

    /**
     *  Sets if cached headers files are to be used
     *
     *@param useCache True if cached info is to be used
     */
    public void setUseCache( boolean useCache )
    {
        this.useCache = useCache;
    }


    /**
     *  Sets the proxyParameters attribute of the SPMParameters object
     *
     *@param  up   The new proxyParameters value
     *@param  ph   The new proxyParameters value
     *@param  pp   The new proxyParameters value
     *@param  usr  The new proxyParameters value
     *@param  pwd  The new proxyParameters value
     */
    public void setProxyParameters( boolean up, String ph, String pp, String usr, String pwd )
    {
        useProxy = up;
        proxyHost = ph;
        proxyPort = pp;
        username = usr;
        password = pwd;
        initHttp();
        savePropertiesFile();
    }


    /**
     *  Sets the changed attribute of the SPMParameters object
     *
     *@param  ch  The new changed value
     */
    public void setChanged( boolean ch )
    {
        changed = ch;
	if (changed) savePropertiesFile();
    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public boolean hasChanged()
    {
        return changed;
    }


    /**
     *  Description of the Method
     */
    public void initHttp()
    {
        if ( !useProxy )
        {
            //System.getProperties().remove("proxySet");
            //System.getProperties().remove("proxyHost");
            //System.getProperties().remove("proxyPort");
            System.getProperties().remove( "http.proxyHost" );
            System.getProperties().remove( "http.proxyPort" );
            //System.getProperties().remove("http.nonProxyHosts");
            Authenticator.setDefault( null );
        }
        else
        {
            // set proxy host
            System.setProperty( "http.proxyHost", proxyHost );
            // set proxy port
            System.setProperty( "http.proxyPort", proxyPort );

            // set proxy authentication
            if ( username == null || username.length() == 0 )
            {
                Authenticator.setDefault( new FirewallAuthenticator( null ) );
            }
            else
            {
                PasswordAuthentication pw = new PasswordAuthentication(
                        username, password.toCharArray()
                         );
                Authenticator.setDefault( new FirewallAuthenticator( pw ) );
            }
        }
    }


    //copied from jEdit
    /**
     *  Description of the Class
     *
     *@author     Francois Guillet
     *@created    March, 20 2004
     */
    static class FirewallAuthenticator extends Authenticator
    {
        PasswordAuthentication pw;

        /**
         *  Constructor for the FirewallAuthenticator object
         *
         *@param  pw  Description of the Parameter
         */
        public FirewallAuthenticator( PasswordAuthentication pw )
        {
            this.pw = pw;
        }


        /**
         *  Gets the passwordAuthentication attribute of the
         *  FirewallAuthenticator object
         *
         *@return    The passwordAuthentication value
         */
        protected PasswordAuthentication getPasswordAuthentication()
        {
            // if we have no stored credentials, prompt the user now
            if (pw == null) {
        	BTextField nameField = new BTextField();
        	BPasswordField pwField = new BPasswordField();
        	ComponentsDialog dlg = new ComponentsDialog(SPManagerFrame.getInstance(), "SPManager:Authentication",
        		new Widget[] { nameField, pwField },
        		new String[] { Translate.text("SPManager:name"), Translate.text("SPManager.password") });
        	
        	if (dlg.clickedOk()) {
        	    pw = new PasswordAuthentication(nameField.getText(), pwField.getText().toCharArray());
        	}
            }
            
            return pw;
        }
    }
}


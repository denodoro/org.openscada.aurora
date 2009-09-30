package org.openscada.ca.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openscada.ca.Configuration;
import org.openscada.ca.ConfigurationAdministrator;
import org.openscada.ca.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class ListServlet extends HttpServlet
{

    /**
     * 
     */
    private static final long serialVersionUID = -4627580623796526837L;

    private final ServiceTracker adminTracker;

    public ListServlet ( final BundleContext context )
    {
        this.adminTracker = new ServiceTracker ( context, ConfigurationAdministrator.class.getName (), null );
        this.adminTracker.open ( true );
    }

    @Override
    public void destroy ()
    {
        this.adminTracker.close ();
        super.destroy ();
    }

    protected ConfigurationAdministrator getAdmin ()
    {
        final Object o = this.adminTracker.getService ();
        if ( o instanceof ConfigurationAdministrator )
        {
            return (ConfigurationAdministrator)o;
        }
        return null;
    }

    @Override
    protected void doGet ( final HttpServletRequest req, final HttpServletResponse resp ) throws ServletException, IOException
    {
        handleRequest ( req, resp );
    }

    @Override
    protected void doPost ( final HttpServletRequest req, final HttpServletResponse resp ) throws ServletException, IOException
    {
        handleRequest ( req, resp );
    }

    private void handleRequest ( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException
    {
        final ConfigurationAdministrator admin = getAdmin ();
        if ( admin == null )
        {
            resp.sendError ( HttpServletResponse.SC_SERVICE_UNAVAILABLE, "The configuration administrator could not be found" );
            return;
        }

        startPage ( resp, "List Factories" );
        try
        {
            if ( req.getParameter ( "cmd_create" ) != null )
            {
                handleCreate ( admin, req, resp );
            }
            if ( req.getParameter ( "cmd_purge" ) != null )
            {
                handlePurge ( admin, req, resp );
            }
            if ( req.getParameter ( "cmd_delete" ) != null )
            {
                handleDelete ( admin, req, resp );
            }
            if ( req.getParameter ( "cmd_update" ) != null )
            {
                handleUpdate ( admin, req, resp );
            }

            showFactories ( resp, admin );
        }
        finally
        {
            stopPage ( resp );
        }
    }

    private void handleUpdate ( final ConfigurationAdministrator admin, final HttpServletRequest req, final HttpServletResponse resp ) throws IOException
    {
        final String factoryId = req.getParameter ( "factoryId" );
        final String configurationId = req.getParameter ( "configurationId" );
        final String data = req.getParameter ( "data" );

        if ( factoryId == null || configurationId == null || data == null )
        {
            return;
        }

        final Map<String, String> properties = parseData ( data );

        final Factory f = admin.getFactory ( factoryId );
        if ( f == null )
        {
            resp.getOutputStream ().print ( "Failed to load factory" );
            return;
        }

        final Configuration c = f.getConfiguration ( configurationId );
        if ( c == null )
        {
            resp.getOutputStream ().print ( "Failed to load configuration" );
            return;
        }

        c.update ( properties );
    }

    private void handleDelete ( final ConfigurationAdministrator admin, final HttpServletRequest req, final HttpServletResponse resp ) throws IOException
    {
        final String factoryId = req.getParameter ( "factoryId" );
        final String configurationId = req.getParameter ( "configurationId" );

        final Factory f = admin.getFactory ( factoryId );
        if ( f == null )
        {
            resp.getOutputStream ().print ( "Failed to load factory" );
            return;
        }

        final Configuration c = f.getConfiguration ( configurationId );
        if ( c == null )
        {
            resp.getOutputStream ().print ( "Failed to load configuration" );
            return;
        }

        c.delete ();
    }

    private void handlePurge ( final ConfigurationAdministrator admin, final HttpServletRequest req, final HttpServletResponse resp )
    {
        final String id = req.getParameter ( "id" );
        final Factory f = admin.getFactory ( id );
        if ( f != null )
        {
            f.purge ();
        }
        else
        {

        }
    }

    private void handleCreate ( final ConfigurationAdministrator admin, final HttpServletRequest req, final HttpServletResponse resp ) throws IOException
    {
        final String id = req.getParameter ( "id" );
        final String data = req.getParameter ( "data" );
        final String factoryId = req.getParameter ( "factoryId" );

        if ( id == null || data == null || id.length () <= 0 || data.length () <= 0 )
        {
            return;

        }
        final ServletOutputStream stream = resp.getOutputStream ();

        stream.print ( "<div class='log'>" );
        stream.print ( String.format ( "<div class='%s'>%s</div>", "info", "Creating configuration " + id + " => " + data ) );

        final Map<String, String> properties = parseData ( data );

        admin.createConfiguration ( factoryId, id, properties );

        stream.print ( "</div>" );
    }

    private Map<String, String> parseData ( final String data ) throws IOException
    {
        final Properties p = parseProperties ( data );
        final Map<String, String> properties = new HashMap<String, String> ();
        for ( final Entry<Object, Object> entry : p.entrySet () )
        {
            properties.put ( entry.getKey ().toString (), entry.getValue ().toString () );
        }
        return properties;
    }

    private Properties parseProperties ( final String data ) throws IOException
    {
        final Reader r = new StringReader ( data );
        final Properties p = new Properties ();
        p.load ( new InputStream () {

            @Override
            public int read () throws IOException
            {
                return r.read ();
            }
        } );
        return p;
    }

    private void stopPage ( final HttpServletResponse resp ) throws IOException
    {
        resp.getOutputStream ().print ( "</body></html>" );
    }

    private void startPage ( final HttpServletResponse resp, final String title ) throws IOException
    {
        resp.setCharacterEncoding ( "UTF-8" );
        resp.setContentType ( "text/html; charset=UTF-8" );

        final ServletOutputStream stream = resp.getOutputStream ();
        stream.print ( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" );
        stream.print ( "<html>" );
        stream.print ( "<head>" );
        stream.print ( "<title>" + title + "</title>" );
        stream.print ( "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">" );
        stream.print ( "<link type=\"text/css\" media=\"screen,print\" rel=\"stylesheet\" href=\"/ca/resources/style.css\"></link>" );
        stream.print ( "</head><body>" );
    }

    private void showFactories ( final HttpServletResponse resp, final ConfigurationAdministrator admin ) throws IOException
    {
        final ServletOutputStream stream = resp.getOutputStream ();
        stream.print ( "<table class='factories'>" );
        stream.print ( "<tr><th>ID</th><th>Description</th><th>#</th><th>State</th></tr>" );

        for ( final Factory factory : admin.listKnownFactories () )
        {
            stream.print ( "<tr>" );
            stream.print ( String.format ( "<td>%s</td>", factory.getId () ) );
            stream.print ( String.format ( "<td>%s</td>", factory.getDescription () ) );
            stream.print ( String.format ( "<td>%s</td>", factory.getConfigurations ().length ) );
            stream.print ( String.format ( "<td>%s</td>", factory.getState () ) );

            stream.print ( "<td><form method='POST'><input type='submit' value='Purge' name='cmd_purge' /><input type='hidden' value='" + factory.getId () + "' name='id' /></form></td>" );

            stream.print ( "</tr>" );

            if ( factory.getConfigurations ().length > 0 )
            {
                stream.print ( "<tr><td></td><td colspan='4'>" );
                showConfigurations ( resp, admin, factory.getId (), factory.getConfigurations () );
                stream.print ( "</td></tr>" );
            }
        }
        stream.print ( "</table>" );

        stream.print ( "<form method='GET'><input type='submit' value='Refresh' /></form>" );
        stream.print ( "<form method='POST'>" );
        stream.print ( "<label for='create_factoryId'>Factory Id:</label><input id='create_factory_id' type='text' value='' name='factoryId' /><br />" );
        stream.print ( "<label for='create_id'>Id:</label><input id='create_id' type='text' value='' name='id' /><br />" );
        stream.print ( "<textarea name='data' cols='60' rows='5'></textarea><br />" );
        stream.print ( "<input type='submit' value='Create' name='cmd_create' /><br />" );
        stream.print ( "</form>" );

    }

    private void showConfigurations ( final HttpServletResponse resp, final ConfigurationAdministrator admin, final String factoryId, final Configuration[] configurations ) throws IOException
    {
        final ServletOutputStream stream = resp.getOutputStream ();
        stream.print ( "<table class='configuration' width='100%'>" );
        stream.print ( "<tr><th>ID</th><th>State</th><th>Error</th><th>Data</th></tr>" );

        for ( final Configuration cfg : configurations )
        {
            stream.print ( "<tr>" );
            stream.print ( String.format ( "<td>%s</td>", cfg.getId () ) );
            stream.print ( String.format ( "<td>%s</td>", cfg.getState () ) );
            if ( cfg.getErrorInformation () != null )
            {
                stream.print ( String.format ( "<td>%s</td>", cfg.getErrorInformation ().getMessage () ) );
            }
            else
            {
                stream.print ( String.format ( "<td></td>" ) );
            }
            stream.print ( "<td><div style='float:right;'>" );

            showData ( stream, cfg.getData (), factoryId, cfg.getId () );

            //stream.print ( "</td><td>" );

            stream.print ( "<form method='post'><input type='Submit' value='Delete' name='cmd_delete' /><input type='hidden' name='factoryId' value='" + factoryId + "' /><input type='hidden' name='configurationId' value=" + cfg.getId () + " /></form>" );
            stream.print ( "</div></td>" );

            stream.print ( "</tr>" );
        }

        stream.print ( "</table>" );
    }

    private void showData ( final ServletOutputStream stream, final Map<String, String> data, final String factoryId, final String configurationId ) throws IOException
    {

        final Properties p = new Properties ();
        if ( data != null )
        {
            p.putAll ( data );
        }

        final StringWriter sw = new StringWriter ();

        p.store ( new OutputStream () {

            @Override
            public void write ( final int b ) throws IOException
            {
                sw.write ( b );
            }
        }, null );

        stream.print ( "<form method='post'>" );
        stream.print ( "<textarea name='data' rows='5' cols='60'>" );

        stream.print ( sw.getBuffer ().toString () );

        stream.print ( "</textarea>" );

        stream.print ( String.format ( "<input type='hidden' name='factoryId' value='%s' />", factoryId ) );
        stream.print ( String.format ( "<input type='hidden' name='configurationId' value='%s' />", configurationId ) );
        stream.print ( "<input type='submit' value='Update' name='cmd_update' /></form>" );
    }
}
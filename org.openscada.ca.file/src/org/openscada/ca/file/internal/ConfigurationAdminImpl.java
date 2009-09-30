package org.openscada.ca.file.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class ConfigurationAdminImpl extends AbstractConfigurationAdminImpl
{
    private final static class DataFilenameFilter implements FilenameFilter
    {
        public boolean accept ( final File dir, final String name )
        {
            if ( ".meta".equals ( name ) )
            {
                return false;
            }
            return true;
        }
    }

    private static final String META_FILE = ".meta";

    private final static Logger logger = Logger.getLogger ( ConfigurationAdminImpl.class );

    private static final String STORE = "openscadaConfigStore";

    private final BundleContext context;

    private final File root;

    public ConfigurationAdminImpl ( final BundleContext context ) throws InvalidSyntaxException
    {
        super ( context );
        this.context = context;
        this.root = initRoot ();
    }

    private File initRoot ()
    {
        File file = this.context.getDataFile ( STORE );
        if ( file != null )
        {
            if ( !file.exists () )
            {
                logger.info ( "Storage root does not exist: " + file.getName () );
                file.mkdir ();
            }
            if ( file.isDirectory () )
            {
                return file;
            }
            else
            {
                logger.warn ( "File exists but is not a directory: " + file.getName () );
            }
        }
        else
        {
            logger.warn ( "No file system support" );
        }
        return null;
    }

    @Override
    protected void performInitialLoad ()
    {
        if ( this.root == null )
        {
            logger.warn ( "No root found" );
            return;
        }

        for ( String pathName : this.root.list () )
        {
            File path = new File ( this.root, pathName );
            if ( path.isDirectory () )
            {
                logger.debug ( "Checking for path: " + path.getName () );
                String factoryId = detectFactory ( path );
                if ( factoryId != null )
                {
                    logger.debug ( String.format ( "Path %s is a possible factory (%s). Adding...", path.getName (), factoryId ) );
                    addFactory ( factoryId );
                }
            }
        }
    }

    private String detectFactory ( final File path )
    {
        File meta = new File ( path, META_FILE );
        Properties p = new Properties ();
        FileInputStream stream = null;
        try
        {
            stream = new FileInputStream ( meta );
            p.load ( stream );
        }
        catch ( Exception e )
        {
            return null;
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close ();
                }
                catch ( IOException e )
                {
                    logger.warn ( "Failed to close stream", e );
                }
            }
        }
        return p.getProperty ( "id" );
    }

    @Override
    protected void performLoadFactory ( final FactoryImpl factory )
    {
        if ( this.root == null )
        {
            logger.warn ( "No root found" );
            return;
        }

        File path = getFactoryPath ( factory );
        load ( path, factory );
    }

    private void createStore ( final File factoryRoot, final FactoryImpl factory )
    {
        if ( !factoryRoot.mkdir () )
        {
            logger.warn ( "Failed to create store: " + factoryRoot );
            return;
        }
        File meta = new File ( factoryRoot, META_FILE );
        Properties p = new Properties ();
        p.put ( "id", factory.getId () );
        FileOutputStream stream = null;
        try
        {
            stream = new FileOutputStream ( meta );

            p.store ( stream, "" );
        }
        catch ( Exception e )
        {
            logger.warn ( "Failed to initialize store: " + factoryRoot );
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close ();
                }
                catch ( IOException e )
                {
                    logger.warn ( "Failed to close stream", e );
                }
            }
        }
    }

    private void load ( final File configurationRoot, final FactoryImpl factory )
    {
        logger.info ( "Loading from: " + configurationRoot.getName () );

        Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>> ();

        for ( File file : configurationRoot.listFiles ( new DataFilenameFilter () ) )
        {
            logger.info ( "Loading file: " + file.getName () );
            Map<String, String> cfg = loadConfiguration ( file );
            if ( cfg != null )
            {
                String id = cfg.get ( "id" );
                if ( id != null )
                {
                    configurations.put ( id, cfg );
                }
            }
        }

        factory.setConfigurations ( configurations );
    }

    private Map<String, String> loadConfiguration ( final File file )
    {
        try
        {
            Properties p = new Properties ();

            FileInputStream stream = new FileInputStream ( file );
            try
            {
                p.load ( stream );
            }
            finally
            {
                stream.close ();
            }

            Map<String, String> result = new HashMap<String, String> ();
            for ( Entry<Object, Object> entry : p.entrySet () )
            {
                result.put ( entry.getKey ().toString (), entry.getValue ().toString () );
            }
            return result;
        }
        catch ( Throwable e )
        {
            logger.warn ( "Failed to load" );
            return null;
        }
    }

    private String getPath ( final FactoryImpl factory )
    {
        String path = factory.getId ();
        path = encode ( path );
        return path;
    }

    private String encode ( String path )
    {
        path = path.replace ( "_", "__" );
        path = path.replaceAll ( "[^a-zA-Z0-9_-]", "_" );
        return path;
    }

    protected void performPurge ( final FactoryImpl factoryImpl )
    {
        if ( this.root == null )
        {
            logger.warn ( "Unable to store : no root" );
            return;
        }

        File path = new File ( this.root, getPath ( factoryImpl ) );
        logger.info ( "Deleting factory: " + factoryImpl.getId () );

        File meta = new File ( path, META_FILE );
        meta.delete ();
        path.delete ();
    }

    protected void performStoreConfiguration ( final ConfigurationImpl configurationImpl, final Map<String, String> properties ) throws FileNotFoundException, IOException
    {
        if ( this.root == null )
        {
            logger.warn ( "Unable to store : no root" );
            return;
        }

        File path = getFactoryPath ( configurationImpl.getFactory () );

        String id = configurationImpl.getId ();
        File file = new File ( path, encode ( id ) );

        logger.info ( String.format ( "Storing %s to %s", configurationImpl.getId (), file ) );

        Properties p = new Properties ();
        p.putAll ( properties );
        p.put ( "id", id );

        FileOutputStream stream = new FileOutputStream ( file );
        try
        {
            p.store ( stream, "" );
        }
        finally
        {
            stream.close ();
        }
    }

    private File getFactoryPath ( final FactoryImpl factoryImpl )
    {
        File path = new File ( this.root, getPath ( factoryImpl ) );
        if ( !path.exists () )
        {
            logger.info ( String.format ( "Store for factory (%s) does not exist", factoryImpl.getId () ) );
            createStore ( path, factoryImpl );
        }
        return path;
    }

    @Override
    protected void performDeleteConfiguration ( final ConfigurationImpl configurationImpl )
    {
        File path = getFactoryPath ( configurationImpl.getFactory () );

        String id = configurationImpl.getId ();
        File file = new File ( path, encode ( id ) );

        logger.info ( "Deleting " + id );

        if ( !file.delete () )
        {
            logger.info ( "Failed to delete: " + file );
        }

    }
}
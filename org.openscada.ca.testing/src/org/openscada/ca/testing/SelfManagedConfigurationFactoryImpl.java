package org.openscada.ca.testing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openscada.ca.Configuration;
import org.openscada.ca.ConfigurationData;
import org.openscada.ca.SelfManagedConfigurationFactory;
import org.openscada.ca.StorageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfManagedConfigurationFactoryImpl implements SelfManagedConfigurationFactory
{

    private final static Logger logger = LoggerFactory.getLogger ( SelfManagedConfigurationFactoryImpl.class );

    private ExecutorService executor;

    private final Map<String, ConfigurationImpl> configurations = new HashMap<String, ConfigurationImpl> ();

    private final String factoryId;

    private final Set<StorageListener> listeners = new HashSet<StorageListener> ();

    public SelfManagedConfigurationFactoryImpl ( final String factoryId )
    {
        this.factoryId = factoryId;

        for ( int i = 0; i < 5; i++ )
        {
            // sample init
            final Map<String, String> data = new HashMap<String, String> ();
            data.put ( "foo", "bar" );
            data.put ( "a", "" + i );
            this.configurations.put ( "test" + i, new ConfigurationImpl ( "test" + i, factoryId, data ) );
        }
    }

    public synchronized void start ()
    {
        this.executor = Executors.newSingleThreadExecutor ();
    }

    public synchronized void stop ()
    {
        this.executor.shutdown ();
        this.executor = null;
    }

    public synchronized void addConfigurationListener ( final StorageListener listener )
    {
        if ( !this.listeners.add ( listener ) )
        {
            return;
        }

        // notify the initial in the same thread
        listener.configurationUpdate ( this.configurations.values ().toArray ( new Configuration[0] ), null );
    }

    public synchronized void delete ( final String configurationId )
    {
        logger.info ( "Deleting: {}", configurationId );

        if ( this.configurations.remove ( configurationId ) != null )
        {
            notifyListeners ( null, new String[] { configurationId } );
        }
    }

    /**
     * Notify all listeners in a seperate thread
     * @param addedOrChanged the added or changed configs
     * @param deleted the deleted configs
     */
    private void notifyListeners ( final Configuration[] addedOrChanged, final String[] deleted )
    {
        final Set<StorageListener> listeners = new HashSet<StorageListener> ( this.listeners );

        this.executor.execute ( new Runnable () {

            public void run ()
            {
                for ( final StorageListener listener : listeners )
                {
                    listener.configurationUpdate ( addedOrChanged, deleted );
                }
            }
        } );
    }

    public synchronized void removeConfigurationListener ( final StorageListener listener )
    {
        this.listeners.remove ( listener );
    }

    public synchronized void update ( final ConfigurationData configuration ) throws Exception
    {
        logger.info ( "Updating: {} -> {}", new Object[] { configuration.getId (), configuration.getData () } );
        ConfigurationImpl cfg = this.configurations.get ( configuration.getId () );
        if ( cfg != null )
        {
            cfg.setData ( configuration.getData () );
        }
        else
        {
            cfg = new ConfigurationImpl ( configuration.getId (), this.factoryId, configuration.getData () );
        }

        notifyListeners ( new Configuration[] { cfg }, null );
    }
}
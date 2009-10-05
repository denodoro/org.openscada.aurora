package org.openscada.ca.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openscada.ca.Configuration;
import org.openscada.ca.ConfigurationAdministrator;
import org.openscada.ca.ConfigurationAlreadyExistsException;
import org.openscada.ca.ConfigurationEvent;
import org.openscada.ca.ConfigurationFactory;
import org.openscada.ca.ConfigurationState;
import org.openscada.ca.FactoryEvent;
import org.openscada.ca.FactoryNotFoundException;
import org.openscada.ca.FactoryState;
import org.openscada.ca.SelfManagedConfigurationFactory;
import org.openscada.utils.concurrent.AbstractFuture;
import org.openscada.utils.concurrent.InstantErrorFuture;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfigurationAdministrator implements ConfigurationAdministrator
{

    private final static Logger logger = LoggerFactory.getLogger ( AbstractConfigurationAdministrator.class );

    private final BundleContext context;

    protected final ExecutorService executor;

    private final ListenerTracker listenerTracker;

    private final Map<String, FactoryImpl> factories = new HashMap<String, FactoryImpl> ();

    private final Map<ServiceReference, ConfigurationFactory> services = new HashMap<ServiceReference, ConfigurationFactory> ();

    private final Map<ServiceReference, SelfManagedConfigurationFactory> selfServices = new HashMap<ServiceReference, SelfManagedConfigurationFactory> ();

    private final ServiceTracker serviceListener;

    private final ServiceTracker selfServiceListener;

    public AbstractConfigurationAdministrator ( final BundleContext context )
    {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor ( new NamedThreadFactory ( "Configuration Administrator" ) );

        this.listenerTracker = new ListenerTracker ( context );
        this.serviceListener = new ServiceTracker ( context, ConfigurationFactory.class.getName (), new ServiceTrackerCustomizer () {

            public void removedService ( final ServiceReference reference, final Object service )
            {
                AbstractConfigurationAdministrator.this.removedService ( reference, service );
            }

            public void modifiedService ( final ServiceReference reference, final Object service )
            {
            }

            public Object addingService ( final ServiceReference reference )
            {
                return AbstractConfigurationAdministrator.this.addingService ( reference );
            }
        } );
        this.selfServiceListener = new ServiceTracker ( context, SelfManagedConfigurationFactory.class.getName (), new ServiceTrackerCustomizer () {

            public void removedService ( final ServiceReference reference, final Object service )
            {
                AbstractConfigurationAdministrator.this.removedSelfService ( reference, service );
            }

            public void modifiedService ( final ServiceReference reference, final Object service )
            {
            }

            public Object addingService ( final ServiceReference reference )
            {
                return AbstractConfigurationAdministrator.this.addingSelfService ( reference );
            }
        } );
    }

    public synchronized void start ()
    {
        this.listenerTracker.open ();
        this.serviceListener.open ();
        this.selfServiceListener.open ();
    }

    public synchronized void stop ()
    {
        this.serviceListener.close ();
        this.selfServiceListener.close ();
        this.listenerTracker.close ();
    }

    protected synchronized void addStoredFactory ( final String factoryId, final Storage storage, final ConfigurationImpl[] configurations )
    {
        logger.info ( "Adding stored factory: {} ({})", new Object[] { factoryId, configurations.length } );

        FactoryImpl factory = getFactory ( factoryId );
        if ( factory == null )
        {
            factory = new FactoryImpl ( factoryId, storage );
            setFactoryState ( factory, FactoryState.LOADED );
            this.factories.put ( factoryId, factory );
            // FIXME: announce new factory
        }
        factory.setConfigurations ( configurations );

        final ConfigurationFactory factoryService = factory.getService ();
        if ( factoryService != null )
        {
            scheduleBind ( configurations, factoryService, factory );
        }
    }

    private synchronized void setConfigurationStatus ( final ConfigurationImpl configuration, final ConfigurationState configurationState, final Throwable error )
    {
        configuration.setState ( configurationState, error );
        this.listenerTracker.fireEvent ( new ConfigurationEvent ( ConfigurationEvent.Type.STATE, configuration, configurationState, error ) );
    }

    protected synchronized void setFactoryState ( final FactoryImpl factory, final FactoryState state )
    {
        factory.setState ( state );
        this.listenerTracker.fireEvent ( new FactoryEvent ( FactoryEvent.Type.STATE, factory, state ) );
    }

    private synchronized void scheduleBind ( final ConfigurationImpl[] configurations, final ConfigurationFactory factoryService, final FactoryImpl factory )
    {
        setFactoryState ( factory, FactoryState.BINDING );

        // set all to "APPLYING" now
        for ( final ConfigurationImpl cfg : configurations )
        {
            setConfigurationStatus ( cfg, ConfigurationState.APPLYING, null );
        }

        this.executor.execute ( new Runnable () {

            public void run ()
            {
                for ( final ConfigurationImpl cfg : configurations )
                {
                    applyConfiguration ( null, factoryService, factory, cfg );
                }
                setFactoryState ( factory, FactoryState.BOUND );
            }
        } );
    }

    protected synchronized void addFactoryService ( final String factoryId, final ConfigurationFactory service, final String description )
    {
        FactoryImpl factory = getFactory ( factoryId );
        if ( factory == null )
        {
            Storage storage;
            try
            {
                storage = performCreateStorage ( factoryId );
            }
            catch ( final Exception e )
            {
                // FIXME: better handling
                return;
            }

            factory = new FactoryImpl ( factoryId, storage );
            this.factories.put ( factoryId, factory );

            // FIXME: announce new factory
            setFactoryState ( factory, FactoryState.BINDING );
        }

        if ( factory.getService () == null )
        {
            factory.setDescription ( description );
            factory.setService ( service );

            scheduleBind ( factory.getConfigurations (), service, factory );
        }
    }

    protected synchronized void removeFactoryService ( final String factoryId, final ConfigurationFactory service )
    {
        final FactoryImpl factory = getFactory ( factoryId );
        if ( factory == null )
        {
            return;
        }

        for ( final ConfigurationImpl configuration : factory.getConfigurations () )
        {
            configuration.setState ( ConfigurationState.AVAILABLE, null );
        }

        if ( factory.getService () == service )
        {
            // remove service
            factory.setService ( null );
            setFactoryState ( factory, FactoryState.LOADED );
        }
    }

    private synchronized void addFactorySelfService ( final String factoryId, final SelfManagedConfigurationFactory factoryService, final String description )
    {
        // TODO Auto-generated method stub
    }

    private synchronized void removeSelfFactoryService ( final String property, final SelfManagedConfigurationFactory factoryService )
    {
        // TODO Auto-generated method stub
    }

    protected abstract Storage performCreateStorage ( String factoryId ) throws Exception;

    /**
     * Request a change of the configuration
     * @param factoryId
     * @param configurationId
     * @param properties
     * @param future the future to notify after completion or <code>null</code> if no notification is needed
     */
    protected synchronized void changeConfiguration ( final String factoryId, final String configurationId, final Map<String, String> properties, final ConfigurationFuture future )
    {
        logger.info ( "Request to change configuration: {}/{} -> {}", new Object[] { factoryId, configurationId, properties } );

        final FactoryImpl factory = getFactory ( factoryId );

        if ( factory == null )
        {
            logger.warn ( "Factory not found: {}", new Object[] { factoryId } );
            if ( future != null )
            {
                future.setError ( new FactoryNotFoundException ( factoryId ) );
            }
            return;
        }

        ConfigurationImpl configuration = factory.getConfiguration ( configurationId );
        if ( configuration != null )
        {
            // update data
            configuration.setData ( properties );

            if ( properties == null )
            {
                // delete
                factory.removeConfigration ( configurationId );
            }
        }
        else
        {
            if ( properties != null )
            {
                configuration = new ConfigurationImpl ( configurationId, factoryId, properties );
                factory.addConfiguration ( configuration );
            }
        }

        final ConfigurationFactory factoryService = factory.getService ();

        // now apply the configuration if the factory has a service attached
        if ( factoryService != null && configuration != null )
        {
            final ConfigurationImpl applyConfiguration = configuration;
            setConfigurationStatus ( configuration, ConfigurationState.APPLYING, null );

            this.executor.execute ( new Runnable () {

                public void run ()
                {
                    AbstractConfigurationAdministrator.this.applyConfiguration ( future, factoryService, factory, applyConfiguration );
                }
            } );
        }
        else
        {
            future.setResult ( configuration );
        }
    }

    /**
     * Apply the configuration to the assigned service
     * <p>
     * This method can block for some while
     * </p>
     * @param future
     * @param factory 
     * @param factoryService 
     * @param configuration
     */
    protected void applyConfiguration ( final ConfigurationFuture future, final ConfigurationFactory factoryService, final FactoryImpl factory, final ConfigurationImpl configuration )
    {
        logger.info ( "Apply configuration: {}/{} -> {}", new Object[] { factory.getId (), configuration.getId (), configuration.getData () } );

        try
        {
            final Map<String, String> properties = configuration.getData ();
            if ( properties != null )
            {
                factoryService.update ( configuration.getId (), properties );
            }
            else
            {
                factoryService.delete ( configuration.getId () );
                // FIXME: notify remove                
            }
            synchronized ( this )
            {
                setConfigurationStatus ( configuration, ConfigurationState.APPLIED, null );
            }

            logger.info ( "Applied configuration: {}/{} -> {}", new Object[] { factory.getId (), configuration.getId (), configuration.getData () } );
        }
        catch ( final Throwable e )
        {
            logger.info ( "Apply failed configuration: {}/{} -> {}", new Object[] { factory.getId (), configuration.getId (), configuration.getData () }, e );
            synchronized ( this )
            {
                setConfigurationStatus ( configuration, ConfigurationState.ERROR, e );
            }
        }

        // if the apply operation fails the configuration is still "ok"
        if ( future != null )
        {
            future.setResult ( configuration );
        }
    }

    public synchronized Future<Configuration> createConfiguration ( final String factoryId, final String configurationId, final Map<String, String> properties )
    {
        final FactoryImpl factory = getFactory ( factoryId );
        if ( factory == null )
        {
            return new InstantErrorFuture<Configuration> ( new FactoryNotFoundException ( factoryId ).fillInStackTrace () );
        }

        if ( factory.getConfiguration ( configurationId ) != null )
        {
            // this is not a create operation
            return new InstantErrorFuture<Configuration> ( new ConfigurationAlreadyExistsException ( factoryId, configurationId ).fillInStackTrace () );
        }

        return factory.getStorage ().store ( configurationId, properties );

        // return invokeStore ( factoryId, configurationId, properties );
    }

    public synchronized Future<Configuration> updateConfiguration ( final String factoryId, final String configurationId, final Map<String, String> properties )
    {
        final FactoryImpl factory = getFactory ( factoryId );
        if ( factory == null )
        {
            return new InstantErrorFuture<Configuration> ( new FactoryNotFoundException ( factoryId ).fillInStackTrace () );
        }

        if ( factory.getConfiguration ( configurationId ) == null )
        {
            // this is not a create operation
            return new InstantErrorFuture<Configuration> ( new ConfigurationNotFoundException ( factoryId, configurationId ).fillInStackTrace () );
        }

        return factory.getStorage ().store ( configurationId, properties );
        // return invokeStore ( factoryId, configurationId, properties );
    }

    public synchronized Future<Configuration> deleteConfiguration ( final String factoryId, final String configurationId )
    {
        final FactoryImpl factory = getFactory ( factoryId );
        if ( factory == null )
        {
            return new InstantErrorFuture<Configuration> ( new FactoryNotFoundException ( factoryId ).fillInStackTrace () );
        }

        if ( factory.getConfiguration ( configurationId ) == null )
        {
            // this is not a create operation
            return new InstantErrorFuture<Configuration> ( new ConfigurationNotFoundException ( factoryId, configurationId ).fillInStackTrace () );
        }

        return factory.getStorage ().delete ( configurationId );
        // return invokeDelete ( factoryId, configurationId );
    }

    public final class ConfigurationFuture extends AbstractFuture<Configuration>
    {
        @Override
        public void setError ( final Throwable error )
        {
            super.setError ( error );
        }

        @Override
        public void setResult ( final Configuration result )
        {
            super.setResult ( result );
        }
    }

    /* readers */

    public synchronized Configuration getConfiguration ( final String factoryId, final String configurationId )
    {
        final FactoryImpl factory = getFactory ( factoryId );
        return factory.getConfiguration ( configurationId );
    }

    public synchronized Configuration[] getConfigurations ( final String factoryId )
    {
        final FactoryImpl factory = getFactory ( factoryId );
        return factory.getConfigurations ();
    }

    public synchronized FactoryImpl getFactory ( final String factoryId )
    {
        return this.factories.get ( factoryId );
    }

    public synchronized FactoryImpl[] getKnownFactories ()
    {
        return this.factories.values ().toArray ( new FactoryImpl[0] );
    }

    protected Object addingService ( final ServiceReference reference )
    {
        final String factoryId = checkAndGetFactoryId ( reference );
        final String description = getDescription ( reference );

        Object service = null;
        try
        {
            service = this.context.getService ( reference );
            final ConfigurationFactory factory = (ConfigurationFactory)service;

            addFactoryService ( factoryId, factory, description );

            this.services.put ( reference, factory );

            return factory;
        }
        catch ( final ClassCastException e )
        {
            if ( service != null )
            {
                this.context.ungetService ( reference );
            }
            return null;
        }
    }

    protected synchronized void removedService ( final ServiceReference reference, final Object service )
    {
        final ConfigurationFactory factoryService = this.services.remove ( reference );
        if ( factoryService != null )
        {
            this.context.ungetService ( reference );
            removeFactoryService ( (String)reference.getProperty ( FACTORY_ID ), factoryService );
        }
    }

    /**
     * Get the description from a service reference
     * @param reference the reference to use
     * @return the description or <code>null</code> if none exists
     */
    private String getDescription ( final ServiceReference reference )
    {
        String description;
        if ( reference.getProperty ( Constants.SERVICE_DESCRIPTION ) instanceof String )
        {
            description = (String)reference.getProperty ( Constants.SERVICE_DESCRIPTION );
        }
        else
        {
            description = null;
        }
        return description;
    }

    protected Object addingSelfService ( final ServiceReference reference )
    {
        final String factoryId = checkAndGetFactoryId ( reference );

        final String description = getDescription ( reference );

        Object service = null;
        try
        {
            service = this.context.getService ( reference );
            final SelfManagedConfigurationFactory factory = (SelfManagedConfigurationFactory)service;

            addFactorySelfService ( factoryId, factory, description );

            this.selfServices.put ( reference, factory );

            return factory;
        }
        catch ( final ClassCastException e )
        {
            if ( service != null )
            {
                this.context.ungetService ( reference );
            }
            return null;
        }
    }

    protected void removedSelfService ( final ServiceReference reference, final Object service )
    {
        final SelfManagedConfigurationFactory factoryService = this.selfServices.remove ( reference );
        if ( factoryService != null )
        {
            this.context.ungetService ( reference );
            removeSelfFactoryService ( (String)reference.getProperty ( FACTORY_ID ), factoryService );
        }
    }

    private String checkAndGetFactoryId ( final ServiceReference reference )
    {
        if ( ! ( reference.getProperty ( FACTORY_ID ) instanceof String ) )
        {
            logger.warn ( "Found new service {} but it is missing 'factoryId' in its properties", reference );
            return null;
        }

        final String factoryId = (String)reference.getProperty ( FACTORY_ID );
        return factoryId;
    }

}
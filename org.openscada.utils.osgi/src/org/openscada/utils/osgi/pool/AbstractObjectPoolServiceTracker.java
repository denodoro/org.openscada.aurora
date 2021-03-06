/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2011 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * OpenSCADA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * OpenSCADA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details
 * (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with OpenSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
 */

package org.openscada.utils.osgi.pool;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.openscada.utils.osgi.pool.ObjectPoolTracker.ObjectPoolServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractObjectPoolServiceTracker
{
    private final static Logger logger = LoggerFactory.getLogger ( AbstractObjectPoolServiceTracker.class );

    private final ObjectPoolTracker poolTracker;

    private final ObjectPoolServiceListener poolListener;

    protected final String serviceId;

    private final Map<ObjectPool, PoolHandler> poolMap = new HashMap<ObjectPool, PoolHandler> ( 1 );

    protected class PoolHandler implements ObjectPoolListener
    {
        private final ObjectPool pool;

        private final String serviceId;

        private final Map<Object, Dictionary<?, ?>> services = new HashMap<Object, Dictionary<?, ?>> ( 1 );

        public PoolHandler ( final ObjectPool pool, final String serviceId )
        {
            this.pool = pool;
            this.serviceId = serviceId;

            synchronized ( this )
            {
                this.pool.addListener ( this.serviceId, this );
            }
        }

        public synchronized void dispose ()
        {
            this.pool.removeListener ( this.serviceId, this );

            for ( final Map.Entry<Object, Dictionary<?, ?>> entry : this.services.entrySet () )
            {
                fireServiceRemoved ( entry.getKey (), entry.getValue () );
            }
            this.services.clear ();
        }

        @Override
        public synchronized void serviceAdded ( final Object service, final Dictionary<?, ?> properties )
        {
            this.services.put ( service, properties );
            fireServiceAdded ( service, properties );
        }

        private void fireServiceAdded ( final Object service, final Dictionary<?, ?> properties )
        {
            logger.debug ( "Service added to pool: {} -> {}", new Object[] { this.serviceId, service } );
            handleServiceAdded ( service, properties );
        }

        @Override
        public synchronized void serviceModified ( final Object service, final Dictionary<?, ?> properties )
        {
            this.services.put ( service, properties );
            fireServiceModified ( service, properties );
        }

        private void fireServiceModified ( final Object service, final Dictionary<?, ?> properties )
        {
            handleServiceModified ( service, properties );
        }

        @Override
        public synchronized void serviceRemoved ( final Object service, final Dictionary<?, ?> properties )
        {
            final Dictionary<?, ?> oldProperties = this.services.remove ( service );
            if ( oldProperties != null )
            {
                fireServiceRemoved ( service, properties );
            }
        }

        private void fireServiceRemoved ( final Object service, final Dictionary<?, ?> properties )
        {
            handleServiceRemoved ( service, properties );
        }
    }

    public AbstractObjectPoolServiceTracker ( final ObjectPoolTracker poolTracker, final String serviceId )
    {
        this.serviceId = serviceId;
        this.poolTracker = poolTracker;

        this.poolListener = new ObjectPoolServiceListener () {

            @Override
            public void poolRemoved ( final ObjectPool objectPool )
            {
                AbstractObjectPoolServiceTracker.this.handlePoolRemove ( objectPool );
            }

            @Override
            public void poolModified ( final ObjectPool objectPool, final int newPriority )
            {
                AbstractObjectPoolServiceTracker.this.handlePoolModified ( objectPool, newPriority );
            }

            @Override
            public void poolAdded ( final ObjectPool objectPool, final int priority )
            {
                AbstractObjectPoolServiceTracker.this.handlePoolAdd ( objectPool, priority );
            }
        };
    }

    protected abstract void handleServiceAdded ( final Object service, final Dictionary<?, ?> properties );

    protected abstract void handleServiceModified ( final Object service, final Dictionary<?, ?> properties );

    protected abstract void handleServiceRemoved ( final Object service, final Dictionary<?, ?> properties );

    protected synchronized void handlePoolAdd ( final ObjectPool objectPool, final int priority )
    {
        logger.debug ( "Pool added: {}/{}", new Object[] { objectPool, priority } );
        this.poolMap.put ( objectPool, new PoolHandler ( objectPool, this.serviceId ) );
    }

    protected synchronized void handlePoolModified ( final ObjectPool objectPool, final int newPriority )
    {
        // we don't care
    }

    protected synchronized void handlePoolRemove ( final ObjectPool objectPool )
    {
        logger.debug ( "Pool removed: {}", objectPool );

        final PoolHandler handler = this.poolMap.get ( objectPool );
        if ( handler != null )
        {
            handler.dispose ();
        }
    }

    public void open ()
    {
        this.poolTracker.addListener ( this.poolListener );
    }

    public void close ()
    {
        this.poolTracker.removeListener ( this.poolListener );
    }
}

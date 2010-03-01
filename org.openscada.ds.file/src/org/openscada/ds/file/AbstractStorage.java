/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2009-2010 inavare GmbH (http://inavare.com)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.openscada.ds.file;

import java.util.concurrent.Executor;

import org.openscada.ds.DataListener;
import org.openscada.ds.DataNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public abstract class AbstractStorage
{
    protected final Executor executor;

    private final Multimap<String, DataListener> listeners = HashMultimap.create ();

    public AbstractStorage ( final Executor executor )
    {
        this.executor = executor;
    }

    public abstract DataNode getNode ( final String nodeId );

    public synchronized void dispose ()
    {
        for ( final DataListener listener : this.listeners.values () )
        {
            this.executor.execute ( new Runnable () {

                public void run ()
                {
                    listener.nodeChanged ( null );
                }
            } );
        }
        this.listeners.clear ();
    }

    public synchronized void attachListener ( final String nodeId, final DataListener listener )
    {
        if ( this.listeners.put ( nodeId, listener ) )
        {
            final DataNode node = getNode ( nodeId );
            this.executor.execute ( new Runnable () {

                public void run ()
                {
                    listener.nodeChanged ( node );
                }
            } );
        }
    }

    public synchronized void detachListener ( final String nodeId, final DataListener listener )
    {
        this.listeners.remove ( nodeId, listener );
    }

}
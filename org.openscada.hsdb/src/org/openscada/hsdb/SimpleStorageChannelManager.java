package org.openscada.hsdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.openscada.hsdb.datatypes.DoubleValue;
import org.openscada.hsdb.datatypes.LongValue;

/**
 * This StorageChannel implementation provides methods for managing StorageChannel objects.
 * This can be useful when complex storage channel structures have to be created and handled.
 * @author Ludwig Straub
 */
public abstract class SimpleStorageChannelManager implements StorageChannelManager
{
    /** List of currently registers storage channels. */
    private final List<ExtendedStorageChannel> storageChannels;

    /**
     * Standard constructor.
     */
    public SimpleStorageChannelManager ()
    {
        storageChannels = new LinkedList<ExtendedStorageChannel> ();
    }

    /**
     * @see org.openscada.hsdb.StorageChannelManager#registerStorageChannel
     */
    public synchronized void registerStorageChannel ( final ExtendedStorageChannel storageChannel )
    {
        storageChannels.add ( storageChannel );
    }

    /**
     * @see org.openscada.hsdb.StorageChannelManager#unregisterStorageChannel
     */
    public synchronized void unregisterStorageChannel ( final ExtendedStorageChannel storageChannel )
    {
        storageChannels.remove ( storageChannel );
    }

    /**
     * @see org.openscada.hsdb.StorageChannel#updateLong
     */
    public synchronized void updateLong ( final LongValue longValue ) throws Exception
    {
        for ( StorageChannel storageChannel : storageChannels )
        {
            storageChannel.updateLong ( longValue );
        }
    }

    /**
     * @see org.openscada.hsdb.StorageChannel#updateLongs
     */
    public synchronized void updateLongs ( final LongValue[] longValues ) throws Exception
    {
        for ( StorageChannel storageChannel : storageChannels )
        {
            storageChannel.updateLongs ( longValues );
        }
    }

    /**
     * @see org.openscada.hsdb.StorageChannel#getLongValues
     */
    public synchronized LongValue[] getLongValues ( final long startTime, final long endTime ) throws Exception
    {
        // optimization if exactly one storage channel is currently managed
        if ( storageChannels.size () == 1 )
        {
            return storageChannels.get ( 0 ).getLongValues ( startTime, endTime );
        }

        // default method logic
        List<LongValue> longValues = new ArrayList<LongValue> ();
        for ( StorageChannel storageChannel : storageChannels )
        {
            longValues.addAll ( Arrays.asList ( storageChannel.getLongValues ( startTime, endTime ) ) );
        }
        return longValues.toArray ( EMPTY_LONGVALUE_ARRAY );
    }

    /**
     * @see org.openscada.hsdb.ExtendedStorageChannel#updateDouble
     */
    public synchronized void updateDouble ( final DoubleValue doubleValue ) throws Exception
    {
        for ( ExtendedStorageChannel storageChannel : storageChannels )
        {
            storageChannel.updateDouble ( doubleValue );
        }
    }

    /**
     * @see org.openscada.hsdb.ExtendedStorageChannel#updateDoubles
     */
    public synchronized void updateDoubles ( final DoubleValue[] doubleValues ) throws Exception
    {
        for ( ExtendedStorageChannel storageChannel : storageChannels )
        {
            storageChannel.updateDoubles ( doubleValues );
        }
    }

    /**
     * @see org.openscada.hsdb.ExtendedStorageChannel#getDoubleValues
     */
    public synchronized DoubleValue[] getDoubleValues ( final long startTime, final long endTime ) throws Exception
    {
        // optimization if exactly one storage channel is currently managed
        if ( storageChannels.size () == 1 )
        {
            return storageChannels.get ( 0 ).getDoubleValues ( startTime, endTime );
        }

        // default method logic
        List<DoubleValue> doubleValues = new ArrayList<DoubleValue> ();
        for ( ExtendedStorageChannel storageChannel : storageChannels )
        {
            doubleValues.addAll ( Arrays.asList ( storageChannel.getDoubleValues ( startTime, endTime ) ) );
        }
        return doubleValues.toArray ( EMPTY_DOUBLEVALUE_ARRAY );
    }

    /**
     * @see org.openscada.hsdb.ExtendedStorageChannel#cleanupRelicts
     */
    public synchronized void cleanupRelicts () throws Exception
    {
        for ( StorageChannel storageChannel : storageChannels )
        {
            storageChannel.cleanupRelicts ();
        }
    }
}

package org.openscada.hsdb.backend;

import org.openscada.hsdb.StorageChannel;
import org.openscada.hsdb.StorageChannelMetaData;

/**
 * This interface provides methods for storage channel backend implementations.
 * @author Ludwig Straub
 */
public interface BackEnd extends StorageChannel
{
    /**
     * This method resets any previously existing data and prepares the storage channel for new data.
     * This method has to be called if a new backend storage channel has to be created but not if an existing one should be used.
     * After this method is executed, the initialize method has to be called before methods of the StorageChannel interface can be used.
     * @param storageChannelMetaData metadata describing the backend of the storage channel
     * @throws Exception in case of any problem
     */
    public abstract void create ( final StorageChannelMetaData storageChannelMetaData ) throws Exception;

    /**
     * This method initializes the backend storage channel.
     * It has to be assured that the method is called before any methods of the StorageChannel interface all triggered.
     * @param storageChannelMetaData metadata describing the backend of the storage channel
     * @throws Exception in case of any problem
     */
    public abstract void initialize ( final StorageChannelMetaData storageChannelMetaData ) throws Exception;

    /**
     * This method returns whether the time span that is defined via the passed start time and end time is identical to the data that is received via the method getMetaData or not.
     * The result specifier further, whether multiple calls to the method getMetaData will always return the same result or not.
     * The information of this flag can be used for optimization while handling storage channel backend objects.
     * @return true, if time span does not change while processing data, otherwise false
     */
    public abstract boolean isTimeSpanConstant ();

    /**
     * This method performs cleanup operations at the end of the storage channel backend's lifecycle.
     * @throws Exception in case of any problem
     */
    public abstract void deinitialize () throws Exception;

    /**
     * This method removes all data that was previously stored or retrieved by the backend storage channel.
     * The initialize method must have been executed before this method can be called.
     * @throws Exception in case of any problem
     */
    public abstract void delete () throws Exception;
}

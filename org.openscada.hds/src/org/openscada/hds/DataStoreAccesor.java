/*
 * This file is part of the openSCADA project
 * Copyright (C) 2006-2011 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * openSCADA is free software: you can redistribute it and/or modify
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
 * version 3 along with openSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
 */

package org.openscada.hds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openscada.utils.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreAccesor extends AbstractValueSource
{
    private final static Logger logger = LoggerFactory.getLogger ( DataStoreAccesor.class );

    private final File basePath;

    private final long time;

    private final TimeUnit unit;

    private final Quantizer quantizer;

    private final int count;

    private final DataFilePool pool;

    private final Set<AccessorWrapper> wrappers = new HashSet<AccessorWrapper> ();

    private final Lock lock = new ReentrantLock ();

    private final Condition wrapperCondition = this.lock.newCondition ();

    private boolean disposed;

    private class AccessorWrapper implements DataFileAccessor
    {
        private final DataFileAccessor accessor;

        public AccessorWrapper ( final DataFileAccessor accessor )
        {
            this.accessor = accessor;
            take ( this );
        }

        @Override
        public void insertValue ( final double value, final Date date, final boolean error, final boolean manual, final boolean heartbeat ) throws IOException
        {
            this.accessor.insertValue ( value, date, error, manual, heartbeat );
        }

        @Override
        public boolean visit ( final ValueVisitor visitor ) throws Exception
        {
            return this.accessor.visit ( visitor );
        }

        @Override
        public boolean visitFirstValue ( final ValueVisitor visitor ) throws Exception
        {
            return this.accessor.visitFirstValue ( visitor );
        }

        @Override
        public void forwardCorrect ( final double value, final Date date, final boolean error, final boolean manual ) throws Exception
        {
            this.accessor.forwardCorrect ( value, date, error, manual );
        }

        @Override
        public void delete ()
        {
            this.accessor.delete ();
            dispose ();
        }

        @Override
        public Date getStart ()
        {
            return this.accessor.getStart ();
        }

        @Override
        public Date getEnd ()
        {
            return this.accessor.getEnd ();
        }

        @Override
        public void dispose ()
        {
            try
            {
                this.accessor.dispose ();
            }
            finally
            {
                giveBack ( this );
            }
        }
    }

    public DataStoreAccesor ( final File basePath, final DataFilePool pool ) throws Exception
    {
        this.basePath = basePath;
        this.pool = pool;

        if ( !basePath.isDirectory () )
        {
            throw new IllegalStateException ( String.format ( "'%s' is not a directory", basePath ) );
        }

        final Properties p = new Properties ();
        p.loadFromXML ( new FileInputStream ( new File ( basePath, "settings.xml" ) ) );

        this.time = Long.parseLong ( p.getProperty ( "time" ) );
        this.unit = TimeUnit.valueOf ( p.getProperty ( "unit" ) );
        this.count = Integer.parseInt ( p.getProperty ( "count" ) );
        this.quantizer = new Quantizer ( this.time, this.unit, this.count );
    }

    /**
     * Get the number of slices configured
     * @return the number of slices configured
     */
    public int getCount ()
    {
        return this.count;
    }

    /**
     * Get the time slice in milliseconds
     * @return the time slice in millseconds
     */
    public long getTimeSlice ()
    {
        return TimeUnit.MILLISECONDS.convert ( this.time, this.unit );
    }

    protected void take ( final AccessorWrapper wrapper )
    {
        this.lock.lock ();
        try
        {
            this.wrappers.add ( wrapper );
        }
        finally
        {
            this.lock.unlock ();
        }
    }

    protected void giveBack ( final AccessorWrapper wrapper )
    {
        logger.debug ( "Giving back resource: {}", wrapper );
        this.lock.lock ();
        try
        {
            this.wrappers.remove ( wrapper );
            this.wrapperCondition.signalAll ();
        }
        finally
        {
            this.lock.unlock ();
        }
    }

    protected void insertValue ( final double value, final Date date, final boolean error, final boolean manual, final boolean heartbeat ) throws Exception
    {
        {
            logger.debug ( "Inserting value - value: {}, timestamp: {}, error: {}, manual: {}", new Object[] { value, date, error, manual } );
            final DataFileAccessor file = createOrGetFile ( date, true );
            if ( file != null )
            {
                file.insertValue ( value, date, error, manual, heartbeat );
                file.dispose ();
            }
            else
            {
                throw new IllegalStateException ( String.format ( "Unable to get file for %tc", date ) );
            }
        }

        /*
         * The following forward cleans data, assuming that the provided timestamp overrides all errors
         * between then and now. 
         */

        if ( !heartbeat && !Double.isNaN ( value ) )
        {
            logger.debug ( "Starting forward correction" );

            final Date now = new Date ();
            Date current = this.quantizer.getValidStart ( date );
            while ( current != null && current.before ( now ) )
            {
                final Date next = this.quantizer.getNext ( current );

                logger.debug ( "Forward correcting - {} -> {}", current, next );
                final DataFileAccessor file = createOrGetFile ( date, true );

                if ( file != null )
                {
                    try
                    {
                        file.forwardCorrect ( value, date, error, manual );
                    }
                    finally
                    {
                        file.dispose ();
                    }
                }
                current = next;
            }

            logger.debug ( "Finished forward correcting" );
        }
    }

    public void insertValue ( final double value, final Date date, final boolean error, final boolean manual ) throws Exception
    {
        insertValue ( value, date, error, manual, false );
    }

    /**
     * Insert a marker that the data input mechanism was alive at the provided time
     * @param date the date of the marker
     * @throws Exception thrown if anything goes wrong
     */
    public void insertHeartbeat ( final Date date ) throws Exception
    {
        insertValue ( Double.NaN, date, false, false, true );
    }

    /**
     * Visit values
     * @param visitor the visitor
     * @param start the start range
     * @param end the end range
     * @return <code>true</code> if more should be read, <code>false</code> otherwise
     */
    @Override
    public boolean visit ( final ValueVisitor visitor, final Date start, final Date end )
    {
        Date current = this.quantizer.getStart ( start );

        // read backwards till first entry

        boolean firstRead = false;
        do
        {
            final Date startTimestamp = this.quantizer.getPrevious ( current );
            final Date endTimestamp = current;

            logger.debug ( "Visiting - start: {}, end: {}", startTimestamp, endTimestamp );

            DataFileAccessor file = null;

            try
            {
                try
                {
                    file = createOrGetFile ( startTimestamp, false );
                }
                catch ( final Exception e )
                {
                    logger.warn ( String.format ( "Failed to access file for timestamp %tc -> %tc", startTimestamp, endTimestamp ), e );
                }

                if ( file == null )
                {
                    logger.info ( "Did not receive file. Skipping..." );
                }
                else
                {
                    try
                    {
                        logger.debug ( "Visiting file" );
                        firstRead = file.visitFirstValue ( visitor );
                    }
                    catch ( final Exception e )
                    {
                        logger.warn ( "Failed to visit file", e );
                    }
                }
            }
            finally
            {
                if ( file != null )
                {
                    file.dispose ();
                }
            }

            current = startTimestamp;

        } while ( !firstRead && this.quantizer.getValidStart ( current ) != null );

        // now read forward

        current = this.quantizer.getStart ( start );

        do
        {
            final Date next = this.quantizer.getNext ( current );
            logger.debug ( "Visiting - start: {}, end: {}", current, next );

            DataFileAccessor file = null;
            try
            {
                try
                {
                    file = createOrGetFile ( current, false );
                }
                catch ( final Exception e )
                {
                    logger.warn ( String.format ( "Failed to access file for timestamp %tc -> %tc", current, next ), e );
                }

                if ( file == null )
                {
                    logger.info ( "Did not receive file. Marking invalid!" );
                    if ( !visitor.value ( Double.NaN, current, true, false ) )
                    {
                        logger.debug ( "Visitor requested stop" );
                        return false;
                    }
                }
                else
                {
                    try
                    {
                        logger.debug ( "Visiting file" );
                        if ( !file.visit ( visitor ) )
                        {
                            logger.debug ( "Visitor requested stop" );
                            return false;
                        }
                    }
                    catch ( final Exception e )
                    {
                        logger.warn ( "Failed to visit file", e );
                        if ( !visitor.value ( Double.NaN, current, true, false ) )
                        {
                            logger.debug ( "Visitor requested stop" );
                            return false;
                        }
                    }
                }
            }
            finally
            {
                if ( file != null )
                {
                    file.dispose ();
                }
            }

            current = next;
        } while ( current.before ( end ) );

        return true;
    }

    /**
     * Delete data files that are out of range
     */
    public void purge ()
    {
        logger.info ( "Purging {}", this.basePath );

        for ( final File file : this.basePath.listFiles () )
        {
            logger.debug ( "Checking file: {}", file );

            if ( !file.isFile () )
            {
                logger.debug ( "{} is not a file. Ignoring.", file );
                continue;
            }

            try
            {
                final DataFileAccessor accessor = this.pool.getAccessor ( file );
                if ( accessor == null )
                {
                    logger.warn ( "No accessor. Ignoring file: {}", file );
                    continue;
                }

                // DELETE
                if ( accessor.getEnd ().before ( this.quantizer.getEndOfPeriod ( null ) ) )
                {
                    accessor.delete ();
                }
                else
                {
                    accessor.dispose ();
                }

            }
            catch ( final Exception e )
            {
                logger.warn ( String.format ( "Failed to check file: %s", file ), e );
            }
        }
    }

    /**
     * Get a file from the pool or create it if necessary and requested
     * @param date the date for which to file should be valid for
     * @param create a flag that indicated whether to create a missing file or return <code>null</code> instead
     * @return either the file found, the file created or <code>null</code> if the file could not be found but
     * the create flag was <code>false</code>.
     * @throws Exception if anything goes wrong
     */
    private AccessorWrapper createOrGetFile ( final Date date, final boolean create ) throws Exception
    {
        if ( this.disposed )
        {
            throw new IllegalStateException ( "Data store is disposed" );
        }

        final Date start = this.quantizer.getValidStart ( date );

        if ( start == null )
        {
            logger.info ( "{} is outside of the valid time period", date );
            return null;
        }

        final File file = new File ( this.basePath, String.format ( "%08x.hds", start.getTime () ) );

        final Date end = this.quantizer.getEnd ( date );

        final Pair<DataFileAccessor, Boolean> poolResult = this.pool.getAccessor ( file, start, end, create );
        if ( poolResult == null )
        {
            logger.info ( "Pool returned no file" );
            return null;
        }

        if ( poolResult.second )
        {
            logger.debug ( "Pool file was created. Notify change" );
            notifyChange ( start, end );
        }

        final AccessorWrapper result = new AccessorWrapper ( poolResult.first );

        final Date secondStart = this.quantizer.getValidStart ( date );
        if ( secondStart == null )
        {
            logger.info ( "Resource timeout out while we waited for it" );
            result.dispose ();
            return null;
        }
        else
        {
            return result;
        }
    }

    public static DataStoreAccesor create ( final File basePath, final long time, final TimeUnit unit, final int count, final DataFilePool pool ) throws Exception
    {
        if ( basePath.exists () )
        {
            throw new IllegalArgumentException ( String.format ( "'%s' must not exists", basePath ) );
        }

        if ( !basePath.getParentFile ().isDirectory () )
        {
            throw new IllegalArgumentException ( String.format ( "Parent directory '%s' must exists and must be a directory", basePath.getParentFile () ) );
        }

        if ( !basePath.mkdir () )
        {
            throw new IllegalArgumentException ( String.format ( "Unable to create directory %s", basePath ) );
        }

        final Properties p = new Properties ();
        p.put ( "time", "" + time );
        p.put ( "unit", unit.name () );
        p.put ( "count", "" + count );
        p.put ( "version", "1" );

        try
        {
            p.storeToXML ( new FileOutputStream ( new File ( basePath, "settings.xml" ) ), "openSCADA HDS Settings" );
        }
        catch ( final Exception e )
        {
            // try to delete directory
            basePath.delete ();
            throw new IllegalStateException ( e );
        }

        return new DataStoreAccesor ( basePath, pool );
    }

    public void dispose ()
    {
        this.lock.lock ();
        try
        {
            this.disposed = true;
            logger.info ( "Waiting for all resources to be given back" );
            while ( !this.wrappers.isEmpty () )
            {
                try
                {
                    this.wrapperCondition.await ();
                }
                catch ( final InterruptedException e )
                {
                    logger.warn ( "Failed to wait for shutdown", e );
                    return;
                }
            }
            logger.info ( "Everbody home" );
        }
        finally
        {
            this.lock.unlock ();
        }
    }

}

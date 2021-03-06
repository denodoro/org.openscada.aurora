/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
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

package org.openscada.hsdb.backend.internal;

import java.util.Comparator;

import org.openscada.hsdb.StorageChannelMetaData;
import org.openscada.hsdb.backend.BackEnd;

/**
 * Comparator that is used to sort storage channel meta data by time span.
 * @author Ludwig Straub
 */
public class InverseTimeOrderComparator implements Comparator<BackEnd>
{
    /**
     * @see java.util.Comparator#compare
     */
    public int compare ( final BackEnd o1, final BackEnd o2 )
    {
        if ( o1 == null )
        {
            return 1;
        }
        if ( o2 == null )
        {
            return -1;
        }
        StorageChannelMetaData m1 = null;
        try
        {
            m1 = o1.getMetaData ();
        }
        catch ( final Exception e )
        {
            return 1;
        }
        StorageChannelMetaData m2 = null;
        try
        {
            m2 = o2.getMetaData ();
        }
        catch ( final Exception e )
        {
            return -1;
        }
        if ( m1 == null )
        {
            return 1;
        }
        if ( m2 == null )
        {
            return -1;
        }
        final long endTime1 = m1.getEndTime ();
        final long endTime2 = m2.getEndTime ();
        if ( endTime1 < endTime2 )
        {
            return 1;
        }
        if ( endTime1 > endTime2 )
        {
            return -1;
        }
        final long startTime1 = m1.getStartTime ();
        final long startTime2 = m2.getStartTime ();
        if ( startTime1 < startTime2 )
        {
            return 1;
        }
        if ( startTime1 > startTime2 )
        {
            return -1;
        }
        return 0;
    }
}

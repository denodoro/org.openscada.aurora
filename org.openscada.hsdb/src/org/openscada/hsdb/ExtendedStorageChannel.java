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

package org.openscada.hsdb;

import org.openscada.hsdb.datatypes.DoubleValue;

/**
 * This interface extends the storage channel interface with mmethods for additional datatypes.
 * @author Ludwig Straub
 */
public interface ExtendedStorageChannel extends StorageChannel
{
    /** Empty array of double values that is used when transforming a list to an array. */
    public final static DoubleValue[] EMPTY_DOUBLEVALUE_ARRAY = new DoubleValue[0];

    /**
     * This method updates the passed double value.
     * If a value with the same time stamp already exists, the previous value will be replaced.
     * The implementation decides whether the data is processed or not.
     * See the documentation of the different implementations for more details.
     * @param doubleValue value that has to be updated
     * @throws Exception in case of read/write problems or file corruption
     */
    public abstract void updateDouble ( DoubleValue doubleValue ) throws Exception;

    /**
     * This method updates the passed double values.
     * If a value with the same time stamp already exists, the previous value will be replaced.
     * The implementation decides whether the data is processed or not.
     * See the documentation of the different implementations for more details.
     * @param doubleValues values that have to be updated
     * @throws Exception in case of read/write problems or file corruption
     */
    public abstract void updateDoubles ( final DoubleValue[] doubleValues ) throws Exception;

    /**
     * This method retrieves all double values that match the specified time span and returns them as array sorted by time.
     * @param startTime start of the time span for which the values have to be retrieved
     * @param endTime end of the time span for which the values have to be retrieved
     * @return double values that match the specified time span
     * @throws Exception in case of read/write problems or file corruption
     */
    public abstract DoubleValue[] getDoubleValues ( long startTime, long endTime ) throws Exception;
}

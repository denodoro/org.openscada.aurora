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

package org.openscada.utils.osgi.jdbc.data;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class SingleColumnRowMapper<T> implements RowMapper<T>
{
    private final Class<T> clazz;

    public SingleColumnRowMapper ( final Class<T> clazz )
    {
        this.clazz = clazz;
    }

    @Override
    public void validate ( final ResultSet resultSet ) throws RowMapperValidationException, SQLException
    {
        final ResultSetMetaData md = resultSet.getMetaData ();
        if ( md.getColumnCount () != 1 )
        {
            throw new RowMapperValidationException ( "Column count must be exactly one" );
        }
    }

    @Override
    public T mapRow ( final ResultSet resultSet ) throws RowMapperMappingException, SQLException
    {
        final Object result = resultSet.getObject ( 1 );

        if ( this.clazz.isAssignableFrom ( result.getClass () ) )
        {
            return this.clazz.cast ( result );
        }
        else
        {
            throw new RowMapperMappingException ( String.format ( "Failed to map from data type %s to %s", result.getClass (), this.clazz ) );
        }
    }
}
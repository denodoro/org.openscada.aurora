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

package org.openscada.hsdb.calculation;

import org.openscada.hsdb.datatypes.DataType;
import org.openscada.hsdb.datatypes.DoubleValue;
import org.openscada.hsdb.datatypes.LongValue;

/**
 * This class implements the CalculationLogicProvider interface for the calculation of maximum values.
 * @author Ludwig Straub
 */
public class MaximumCalculationLogicProvider extends CalculationLogicProviderBase
{
    /**
     * Constructor.
     * @param inputDataType data type of the input values
     * @param outputDataType data type of the output values
     * @param parameters parameters further specifying the behaviour
     */
    public MaximumCalculationLogicProvider ( final DataType inputDataType, final DataType outputDataType, final long[] parameters )
    {
        super ( inputDataType, outputDataType, parameters );
    }

    /**
     * @see org.openscada.hsdb.calculation.CalculationLogicProvider#getPassThroughValues
     */
    public boolean getPassThroughValues ()
    {
        return false;
    }

    /**
     * @see org.openscada.hsdb.calculation.CalculationLogicProviderBase#calculateLong
     */
    protected long calculateLong ( final LongValue[] values )
    {
        long maxValue = Long.MIN_VALUE;
        for ( final LongValue value : values )
        {
            if ( value.getQualityIndicator () > 0 )
            {
                maxValue = Math.max ( maxValue, value.getValue () );
            }
        }
        return maxValue;
    }

    /**
     * @see org.openscada.hsdb.calculation.CalculationLogicProviderBase#calculateLong
     */
    protected long calculateLong ( final DoubleValue[] values )
    {
        double maxValue = Double.NEGATIVE_INFINITY;
        for ( final DoubleValue value : values )
        {
            if ( value.getQualityIndicator () > 0 )
            {
                maxValue = Math.max ( maxValue, value.getValue () );
            }
        }
        return Math.round ( maxValue );
    }

    /**
     * @see org.openscada.hsdb.calculation.CalculationLogicProviderBase#calculateDouble
     */
    protected double calculateDouble ( final LongValue[] values )
    {
        double maxValue = Long.MIN_VALUE;
        for ( final LongValue value : values )
        {
            if ( value.getQualityIndicator () > 0 )
            {
                maxValue = Math.max ( maxValue, value.getValue () );
            }
        }
        return maxValue;
    }

    /**
     * @see org.openscada.hsdb.calculation.CalculationLogicProviderBase#calculateDouble
     */
    protected double calculateDouble ( final DoubleValue[] values )
    {
        double maxValue = Double.NEGATIVE_INFINITY;
        for ( final DoubleValue value : values )
        {
            if ( value.getQualityIndicator () > 0 )
            {
                maxValue = Math.max ( maxValue, value.getValue () );
            }
        }
        return maxValue;
    }
}

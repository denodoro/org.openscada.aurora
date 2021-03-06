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

/**
 * Enumeration of all supported calculation methods.
 * @author Ludwig Straub
 */
public enum CalculationMethod
{
    /** The calculation method is unknown. */
    UNKNOWN,

    /** No calculation is performed. All values that are processed by a storage channel that is marked as NATIVE will remain unchanged. */
    NATIVE,

    /** An average calculation is performed on the values that are processed. */
    AVERAGE,

    /** A minimum value calculation is performed on the values that are processed. */
    MINIMUM,

    /** A maximum value calculation is performed on the values that are processed. */
    MAXIMUM;

    /**
     * This method transforms the passed calculation method value to a corresponding short string representation.
     * This method is inverse to the method convertShortStringToCalculationMethod.
     * @param calculationMethod calculation method value that has to be transformed
     * @return short string representation of the passed calculation method
     */
    public static String convertCalculationMethodToShortString ( final CalculationMethod calculationMethod )
    {
        switch ( calculationMethod )
        {
        case NATIVE:
        {
            return "NAT";
        }
        case AVERAGE:
        {
            return "AVG";
        }
        case MINIMUM:
        {
            return "MIN";
        }
        case MAXIMUM:
        {
            return "MAX";
        }
        default:
        {
            return "UNK";
        }
        }
    }

    /**
     * This method transforms the passed short string representation of a calculation method value to the corresponding calculation method value.
     * This method is inverse to the method convertCalculationMethodToShortString.
     * @param calculationMethod calculation method value that has to be transformed
     * @return transformed calculation method value
     */
    public static CalculationMethod convertShortStringToCalculationMethod ( final String calculationMethod )
    {
        if ( "NAT".equals ( calculationMethod ) )
        {
            return NATIVE;
        }
        if ( "AVG".equals ( calculationMethod ) )
        {
            return AVERAGE;
        }
        if ( "MIN".equals ( calculationMethod ) )
        {
            return MINIMUM;
        }
        if ( "MAX".equals ( calculationMethod ) )
        {
            return MAXIMUM;
        }
        return UNKNOWN;
    }

    /**
     * This method transforms the passed calculation method value to a corresponding string representation.
     * This method is inverse to the method convertStringToCalculationMethod.
     * @param calculationMethod calculation method value that has to be transformed
     * @return string representation of the passed calculation method
     */
    public static String convertCalculationMethodToString ( final CalculationMethod calculationMethod )
    {
        return calculationMethod == null ? UNKNOWN.toString () : calculationMethod.toString ();
    }

    /**
     * This method transforms the passed string representation of a calculation method value to the corresponding calculation method value.
     * This method is inverse to the method convertCalculationMethodToString.
     * @param calculationMethod calculation method value that has to be transformed
     * @return transformed calculation method value
     */
    public static CalculationMethod convertStringToCalculationMethod ( final String calculationMethod )
    {
        return calculationMethod == null ? UNKNOWN : Enum.valueOf ( CalculationMethod.class, calculationMethod );
    }

    /**
     * This method transforms the passed calculation method value to a corresponding long representation.
     * This method is inverse to the method convertLongToCalculationMethod.
     * @param calculationMethod calculation method value that has to be transformed
     * @return long representation of the passed calculation method
     */
    public static long convertCalculationMethodToLong ( final CalculationMethod calculationMethod )
    {
        switch ( calculationMethod )
        {
        case NATIVE:
        {
            return 0L;
        }
        case AVERAGE:
        {
            return 1L;
        }
        case MINIMUM:
        {
            return 2L;
        }
        case MAXIMUM:
        {
            return 3L;
        }
        default:
        {
            return -1L;
        }
        }
    }

    /**
     * This method transforms the passed long representation of a calculation method value to the corresponding calculation method value.
     * This method is inverse to the method convertCalculationMethodToLong.
     * @param calculationMethod calculation method value that has to be transformed
     * @return transformed calculation method value
     */
    public static CalculationMethod convertLongToCalculationMethod ( final long calculationMethod )
    {
        switch ( (int)calculationMethod )
        {
        case 0:
        {
            return NATIVE;
        }
        case 1:
        {
            return AVERAGE;
        }
        case 2:
        {
            return MINIMUM;
        }
        case 3:
        {
            return MAXIMUM;
        }
        default:
        {
            return UNKNOWN;
        }
        }
    }
}

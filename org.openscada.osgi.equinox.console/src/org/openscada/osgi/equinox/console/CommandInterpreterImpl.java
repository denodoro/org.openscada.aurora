/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2008-2009 inavare GmbH (http://inavare.com)
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

package org.openscada.osgi.equinox.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Queue;

import org.apache.mina.core.session.IoSession;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.osgi.framework.Bundle;

public class CommandInterpreterImpl implements CommandInterpreter
{

    private final static String NL = System.getProperty ( "line.separator", "\n" );

    private final IoSession session;

    private final Queue<String> args;

    public CommandInterpreterImpl ( final IoSession session, final Queue<String> args )
    {
        this.session = session;
        this.args = args;
    }

    public Object execute ( final String cmd )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String nextArgument ()
    {
        if ( this.args.isEmpty () )
        {
            return null;
        }
        return this.args.remove ();
    }

    public void print ( final Object o )
    {
        this.session.write ( "" + o );
    }

    public void printBundleResource ( final Bundle bundle, final String resource )
    {
        final URL url = bundle.getResource ( resource );
        try
        {
            this.session.write ( url.openStream () );
        }
        catch ( final IOException e )
        {
        }
    }

    @SuppressWarnings ( "unchecked" )
    public void printDictionary ( final Dictionary dic, final String title )
    {
        println ( title );
        final Enumeration<?> e = dic.keys ();
        while ( e.hasMoreElements () )
        {
            final Object key = e.nextElement ();
            final Object value = dic.get ( key );
            println ( String.format ( "\t%s => %s", key, value ) );
        }
    }

    public void printStackTrace ( final Throwable t )
    {
        final StringWriter sw = new StringWriter ();
        final PrintWriter pw = new PrintWriter ( sw );

        t.printStackTrace ( pw );

        try
        {
            sw.close ();
        }
        catch ( final IOException e )
        {
            this.session.write ( sw.getBuffer ().toString () );
        }
    }

    public void println ()
    {
        this.session.write ( NL );
    }

    public void println ( final Object o )
    {
        this.session.write ( "" + o + NL );
    }

}
package org.ops4j.pax.construct.inherit;

/*
 * Copyright 2007 Stuart McCulloch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

public class PluginXml
{
    private final File m_file;
    private Xpp3Dom m_xml;

    public PluginXml( File pluginFile )
        throws XmlPullParserException, IOException
    {
        m_file = pluginFile;

        XmlPullParser parser = new MXParser();
        parser.setInput( new FileReader( m_file ) );
        m_xml = Xpp3DomBuilder.build( parser, false );
    }

    public Xpp3Dom[] getMojos()
    {
        return m_xml.getChild( "mojos" ).getChildren( "mojo" );
    }

    public Xpp3Dom findMojo( String goal )
    {
        Xpp3Dom[] mojos = getMojos();
        for( int i = 0; i < mojos.length; i++ )
        {
            if( goal.equals( mojos[i].getChild( "goal" ).getValue() ) )
            {
                return mojos[i];
            }
        }

        return null;
    }

    public static void mergeMojo( Xpp3Dom mojo, Xpp3Dom superMojo )
    {
        Xpp3Dom tempMojo = new Xpp3Dom( superMojo );

        removeDuplicates( mojo, tempMojo, "parameters", "name/", true );
        removeDuplicates( mojo, tempMojo, "configuration", null, false );
        removeDuplicates( mojo, tempMojo, "requirements", "field-name/", true );

        setAppendMode( mojo.getChild( "parameters" ) );
        setAppendMode( mojo.getChild( "configuration" ) );
        setAppendMode( mojo.getChild( "requirements" ) );

        Xpp3Dom.mergeXpp3Dom( mojo, tempMojo );
        Xpp3Dom goal = mojo.getChild( "goal" );

        goal.setValue( goal.getValue().replaceAll( "\\w+:(?:\\w+=)?(\\w+)", "$1" ) );
    }

    static void removeDuplicates( Xpp3Dom mojo, Xpp3Dom superMojo, String listName, String idPath, boolean verbose )
    {
        Xpp3Dom superList = superMojo.getChild( listName );
        Xpp3Dom list = mojo.getChild( listName );

        if( null == superList || null == list )
        {
            return;
        }

        for( int s = 0; s < superList.getChildCount(); s++ )
        {
            Xpp3Dom superNode = getIdNode( superList.getChild( s ), idPath );
            if( hasMatchingNode( list, idPath, verbose, superNode ) )
            {
                superList.removeChild( s-- );
            }
        }
    }

    static boolean hasMatchingNode( Xpp3Dom list, String idPath, boolean verbose, Xpp3Dom superNode )
    {
        for( int n = 0; n < list.getChildCount(); n++ )
        {
            Xpp3Dom node = getIdNode( list.getChild( n ), idPath );

            String lhs;
            String rhs;

            if( null != idPath && idPath.endsWith( "/" ) )
            {
                lhs = superNode.getValue();
                rhs = node.getValue();
            }
            else
            {
                lhs = superNode.getName();
                rhs = node.getName();
            }

            if( lhs.equals( rhs ) )
            {
                if( verbose )
                {
                    System.out.println( "[WARN] overriding field " + lhs );
                }

                return true;
            }
        }

        return false;
    }

    static Xpp3Dom getIdNode( Xpp3Dom node, String idPath )
    {
        Xpp3Dom idNode = node;

        if( null != idPath )
        {
            String[] idSegments = idPath.split( "/" );
            for( int i = 0; i < idSegments.length; i++ )
            {
                idNode = node.getChild( idSegments[i] );
            }
        }

        return idNode;
    }

    static void setAppendMode( Xpp3Dom node )
    {
        if( null != node )
        {
            node.setAttribute( Xpp3Dom.CHILDREN_COMBINATION_MODE_ATTRIBUTE, Xpp3Dom.CHILDREN_COMBINATION_APPEND );
        }
    }

    public void write()
        throws IOException
    {
        FileWriter writer = new FileWriter( m_file );

        XmlSerializer serializer = new PluginSerializer();

        serializer.setOutput( writer );
        serializer.startDocument( writer.getEncoding(), null );
        m_xml.writeToSerializer( null, serializer );
        serializer.endDocument();
    }
}

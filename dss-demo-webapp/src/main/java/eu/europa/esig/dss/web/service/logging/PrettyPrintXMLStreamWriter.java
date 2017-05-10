/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package eu.europa.esig.dss.web.service.logging;

import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Vychází z CXF {@link org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter}u. Pokud má
 * {@link #shrinkLongCharacters} nastaveno na <code>true</code>, omezuje výpis příliš dlouhé
 * sekvence znaků (characters event) - nahradí ji tečkou.
 */
public class PrettyPrintXMLStreamWriter implements XMLStreamWriter {

    static final int DEFAULT_INDENT_LEVEL = 2;

    private XMLStreamWriter baseWriter;

    private int curIndent;

    private int indentAmount = DEFAULT_INDENT_LEVEL;

    private Stack<CurrentElement> elems = new Stack<CurrentElement>();

    /** Zkrátit příliš dlouhé sekvence znaků? */
    private boolean shrinkLongCharacters;

    public PrettyPrintXMLStreamWriter( XMLStreamWriter writer, int indentAmount ) {
        this( writer, indentAmount, 0 );
    }

    public PrettyPrintXMLStreamWriter( XMLStreamWriter writer, int indentAmount, int initialLevel ) {
        baseWriter = writer;
        curIndent = initialLevel;
        this.indentAmount = indentAmount;
    }

    public void writeSpaces() throws XMLStreamException {
        for ( int i = 0; i < curIndent; i++ ) {
            baseWriter.writeCharacters( " " );
        }
    }

    public void indentWithSpaces() throws XMLStreamException {
        writeSpaces();
        indent();
    }

    public void indent() {
        curIndent += indentAmount;
    }

    public void unindent() {
        curIndent -= indentAmount;
    }

    @Override
    public void close() throws XMLStreamException {
        baseWriter.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        baseWriter.flush();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return baseWriter.getNamespaceContext();
    }

    @Override
    public String getPrefix( String uri ) throws XMLStreamException {
        return baseWriter.getPrefix( uri );
    }

    @Override
    public Object getProperty( String name ) throws IllegalArgumentException {
        return baseWriter.getProperty( name );
    }

    @Override
    public void setDefaultNamespace( String uri ) throws XMLStreamException {
        baseWriter.setDefaultNamespace( uri );
    }

    @Override
    public void setNamespaceContext( NamespaceContext context ) throws XMLStreamException {
        baseWriter.setNamespaceContext( context );
    }

    @Override
    public void setPrefix( String prefix, String uri ) throws XMLStreamException {
        baseWriter.setPrefix( prefix, uri );
    }

    @Override
    public void writeAttribute( String localName, String value ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeAttribute( localName, value );
    }

    @Override
    public void writeAttribute( String namespaceURI, String localName, String value ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeAttribute( namespaceURI, localName, value );
    }

    @Override
    public void writeAttribute( String prefix, String namespaceURI, String localName, String value ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeAttribute( prefix, namespaceURI, localName, value );
    }

    @Override
    public void writeCData( String data ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeCData( data );
    }

    @Override
    public void writeCharacters( char[] text, int start, int len ) throws XMLStreamException {
        if ( !shrinkLongCharacters ) {
            baseWriter.writeCharacters( text, start, len );
        }
        else {
            String substring = String.valueOf( text, start, len );
            appendToBuffer( substring );
        }
    }

    @Override
    public void writeCharacters( String text ) throws XMLStreamException {
        if ( !shrinkLongCharacters )
            baseWriter.writeCharacters( text );
        else
            appendToBuffer( text );
    }

    /**
     * Buffer zapisovaných znaků (characters). Pokud je zapnuté zkracování dlouhých dat (
     * {@link #shrinkLongCharacters} je <code>true</code>), tak se zde po dobu streamování dat
     * kumuluje text.
     */
    private final StringBuilder buffer = new StringBuilder();

    /**
     * Maximální počet znaků, které se při zapnutém zkracování dlouhých dat ještě vypíšou do logu.
     */
    private static final int MAX_CHARACTERS_COUNT = 442;

    /** Kolik bylo zapsáno celkem znaků (characters) v aktuálním elementu. */
    private int totalCharactersWritten;

    /** Přidá do bufferu text. Pokud už je buffer plný, nic do něj nepřidá. */
    private void appendToBuffer( String text ) {
        if ( buffer.length() <= MAX_CHARACTERS_COUNT )
            buffer.append( text );
        totalCharactersWritten += text.length();
    }

    @Override
    public void writeComment( String data ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeComment( data );
    }

    @Override
    public void writeDefaultNamespace( String namespaceURI ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeDefaultNamespace( namespaceURI );
    }

    @Override
    public void writeDTD( String dtd ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeDTD( dtd );
    }

    @Override
    public void writeEmptyElement( String localName ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeEmptyElement( localName );
    }

    @Override
    public void writeEmptyElement( String namespaceURI, String localName ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeEmptyElement( localName, namespaceURI );
    }

    @Override
    public void writeEmptyElement( String prefix, String localName, String namespaceURI ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeEmptyElement( prefix, localName, namespaceURI );
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        flushBuffer();
        baseWriter.writeEndDocument();
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        flushBuffer();
        CurrentElement elem = elems.pop();
        unindent();
        if ( elem.hasChildElements() ) {
            baseWriter.writeCharacters( "\n" );
            writeSpaces();
        }
        baseWriter.writeEndElement();
        if ( elems.empty() ) {
            baseWriter.writeCharacters( "\n" );
        }
    }

    @Override
    public void writeEntityRef( String name ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeEntityRef( name );
    }

    @Override
    public void writeNamespace( String prefix, String namespaceURI ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeNamespace( prefix, namespaceURI );
    }

    @Override
    public void writeProcessingInstruction( String target ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeProcessingInstruction( target );
    }

    @Override
    public void writeProcessingInstruction( String target, String data ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeProcessingInstruction( target, data );
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        flushBuffer();
        baseWriter.writeStartDocument();
    }

    @Override
    public void writeStartDocument( String version ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeStartDocument( version );
    }

    @Override
    public void writeStartDocument( String encoding, String version ) throws XMLStreamException {
        flushBuffer();
        baseWriter.writeStartDocument( encoding, version );
    }

    @Override
    public void writeStartElement( String localName ) throws XMLStreamException {
        flushBuffer();
        writeStartElement( null, localName, null );
    }

    @Override
    public void writeStartElement( String namespaceURI, String localName ) throws XMLStreamException {
        flushBuffer();
        writeStartElement( null, localName, namespaceURI );
    }

    @Override
    public void writeStartElement( String prefix, String localName, String namespaceURI ) throws XMLStreamException {
        flushBuffer();
        QName currElemName = new QName( namespaceURI, localName );
        if ( elems.empty() ) {
            indentWithSpaces();
        }
        else {
            baseWriter.writeCharacters( "" );
            baseWriter.writeCharacters( "\n" );
            indentWithSpaces();
            CurrentElement elem = elems.peek();
            elem.setChildElements( true );
        }
        if ( prefix == null && namespaceURI == null ) {
            baseWriter.writeStartElement( localName );
        }
        else if ( prefix == null ) {
            baseWriter.writeStartElement( namespaceURI, localName );
        }
        else {
            baseWriter.writeStartElement( prefix, localName, namespaceURI );
        }
        elems.push( new CurrentElement( currElemName ) );
    }

    class CurrentElement {
        private QName name;

        private boolean hasChildElements;

        CurrentElement( QName qname ) {
            name = qname;
        }

        public QName getQName() {
            return name;
        }

        public boolean hasChildElements() {
            return hasChildElements;
        }

        public void setChildElements( boolean childElements ) {
            hasChildElements = childElements;
        }
    }

    /**
     * Vyprázdní {@link #buffer} a zapíše obsah (případně zkrácený obsah) do {@link #baseWriter}u,
     * pokud je to potřeba.
     */
    private void flushBuffer() throws XMLStreamException {
        if ( !shrinkLongCharacters )
            // bez zkracovani se buffer nepouziva
            return;

        if ( totalCharactersWritten > 0 ) {
            if ( totalCharactersWritten > MAX_CHARACTERS_COUNT )
                baseWriter.writeCharacters( "..." );
            else
                baseWriter.writeCharacters( buffer.toString() );

            buffer.setLength( 0 );
            totalCharactersWritten = 0;
        }
    }

    // setters

    public void setShrinkLongCharacters( boolean shrinkLongCharacters ) {
        this.shrinkLongCharacters = shrinkLongCharacters;
    }

}

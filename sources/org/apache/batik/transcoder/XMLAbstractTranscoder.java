/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.transcoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import org.apache.batik.dom.util.DocumentFactory;
import org.apache.batik.dom.util.SAXDocumentFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * This class may be the base class of all transcoders which take an
 * XML document as input and which need to build a DOM tree. In order
 * to take advantage of this class, you have to specify the following
 * transcoding hints:
 *
 * <ul>
 * <li><tt>KEY_XML_PARSER_CLASSNAME</tt>: the XML parser to use
 *
 * <li><tt>KEY_DOM_IMPLEMENTATION</tt>: the DOM Implementation to use
 *
 * <li><tt>KEY_DOCUMENT_ELEMENT_NAMESPACE_URI</tt>: the namespace URI of the
 * document to create
 *
 * <li><tt>KEY_DOCUMENT_ELEMENT</tt>: the qualified name of the document type
 * to create
 * </ul>
 *
 * @author <a href="mailto:Thierry.Kormann@sophia.inria.fr">Thierry Kormann</a>
 * @version $Id$
 */
public abstract class XMLAbstractTranscoder extends AbstractTranscoder {

    /**
     * Constructs a new <tt>XMLAbstractTranscoder</tt>.
     */
    protected XMLAbstractTranscoder() {}

    /**
     * Transcodes the specified XML input in the specified output. All
     * <tt>TranscoderException</tt> exceptions not catched previously
     * are tagged as fatal errors (ie. call the <tt>fatalError</tt>
     * method of the <tt>ErrorHandler</tt>).
     *
     * @param input the XML input to transcode
     * @param output the ouput where to transcode
     * @exception TranscoderException if an error occured while transcoding
     */
    public void transcode(TranscoderInput input, TranscoderOutput output)
            throws TranscoderException {

        Document document = null;
        if (input.getDocument() != null) {
            document = input.getDocument();
        } else {
            String parserClassname =
                (String)hints.get(KEY_XML_PARSER_CLASSNAME);
            String namespaceURI =
                (String)hints.get(KEY_DOCUMENT_ELEMENT_NAMESPACE_URI);
            String documentElement =
                (String)hints.get(KEY_DOCUMENT_ELEMENT);
            DOMImplementation domImpl =
                (DOMImplementation)hints.get(KEY_DOM_IMPLEMENTATION);

            if (parserClassname == null) {
                handler.fatalError(new TranscoderException(
                   "Unspecified transcoding hints: KEY_XML_PARSER_CLASSNAME"));
                return;
            }
            if (domImpl == null) {
                handler.fatalError(new TranscoderException(
                    "Unspecified transcoding hints: KEY_DOM_IMPLEMENTATION"));
                return;
            }
            if (namespaceURI == null) {
                handler.fatalError(new TranscoderException(
                "Unspecified transcoding hints: KEY_DOCUMENT_ELEMENT_NAMESPACE_URI"));
                return;
            }
            if (documentElement == null) {
                handler.fatalError(new TranscoderException(
                    "Unspecified transcoding hints: KEY_DOCUMENT_ELEMENT"));
                return;
            }
            // parse the XML document
            DocumentFactory f = createDocumentFactory(domImpl, parserClassname);
            try {
                if (input.getInputStream() != null) {
                    document = f.createDocument(namespaceURI,
                                                documentElement,
                                                input.getURI(),
                                                input.getInputStream());
                } else if (input.getReader() != null) {
                    document = f.createDocument(namespaceURI,
                                                documentElement,
                                                input.getURI(),
                                                input.getReader());
                } else if (input.getURI() != null) {
                    document = f.createDocument(namespaceURI,
                                                documentElement,
                                                input.getURI());
                }
            } catch (DOMException ex) {
                handler.fatalError(new TranscoderException(ex));
            } catch (IOException ex) {
                handler.fatalError(new TranscoderException(ex));
            }
        }
        // call the dedicated transcode method
        if (document != null) {
            try {
                transcode(document, output);
            } catch(TranscoderException ex) {
                // at this time, all TranscoderExceptions are fatal errors
                handler.fatalError(ex);
                return;
            }
        }
    }

    /**
     * Creates the <tt>DocumentFactory</tt> used to create the DOM
     * tree. Override this method if you have to use another
     * implementation of the <tt>DocumentFactory</tt> (ie. for SVG,
     * you have to use the <tt>SAXSVGDocumentFactory</tt>).
     *
     * @param domImpl the DOM Implementation to use
     * @param parserClassname the XML parser classname
     */
    protected DocumentFactory createDocumentFactory(DOMImplementation domImpl,
                                                    String parserClassname) {
        return new SAXDocumentFactory(domImpl, parserClassname);
    }

    /**
     * Transcodes the specified Document in the specified output.
     * @param document the document to transcode
     * @param output the ouput where to transcode
     * @exception TranscoderException if an error occured while transcoding
     */
    protected abstract void transcode(Document document,
                                      TranscoderOutput output)
            throws TranscoderException;

    // --------------------------------------------------------------------
    // Keys definition
    // --------------------------------------------------------------------

    /**
     * XML parser classname key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_XML_PARSER_CLASSNAME</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">String</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">null</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">Yes</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">Specify the XML parser classname to use.</TD></TR>
     * </TABLE>
     */
    public static final TranscodingHints.Key KEY_XML_PARSER_CLASSNAME
        = new StringKey(0);

    /**
     * Document element key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_DOCUMENT_ELEMENT</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">String</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">null</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">Yes</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">Specify the qualified name of the document
     * type to be created.</TD></TR>
     * </TABLE>
     */
    public static final TranscodingHints.Key KEY_DOCUMENT_ELEMENT
        = new StringKey(2);

    /**
     * Document element namespace URI key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_DOCUMENT_ELEMENT_NAMESPACE_URI</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">String</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">null</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">Yes</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     *
     * <TD VALIGN="TOP">Specify the namespace URI of the document
     * element.</TD></TR>
     * </TABLE>
     */
    public static final TranscodingHints.Key KEY_DOCUMENT_ELEMENT_NAMESPACE_URI
        = new StringKey(3);

    /**
     * DOM Implementation key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_DOM_IMPLEMENTATION</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">String</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">null</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">Yes</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     *
     * <TD VALIGN="TOP">Specify the DOM Implementation to use.</TD></TR>
     * </TABLE>
     */
    public static final TranscodingHints.Key KEY_DOM_IMPLEMENTATION
        = new DOMImplKey(0);

    /**
     * A transcoding Key represented as a string.
     */
    private static class StringKey extends TranscodingHints.Key {
        public StringKey(int privatekey) {
            super(privatekey);
        }
        public boolean isCompatibleValue(Object v) {
            return (v instanceof String);
        }
    }

    /**
     * A transcoding Key represented as a DOMImplementation.
     */
    private static class DOMImplKey extends TranscodingHints.Key {
        public DOMImplKey(int privatekey) {
            super(privatekey);
        }
        public boolean isCompatibleValue(Object v) {
            return (v instanceof DOMImplementation);
        }
    }
}


/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.transcoder.print;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.batik.dom.svg.DefaultSVGContext;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.batik.dom.util.DocumentFactory;

import org.apache.batik.gvt.GraphicsNodeRenderContext;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.TextPainter;

import org.apache.batik.gvt.filter.GraphicsNodeRableFactory;
import org.apache.batik.gvt.filter.ConcreteGraphicsNodeRableFactory;
import org.apache.batik.gvt.renderer.StrokingTextPainter;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.ViewBox;

import org.apache.batik.gvt.event.EventDispatcher;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.XMLAbstractTranscoder;

import org.apache.batik.util.SVGConstants;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGAElement;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

/**
 * This class is a <tt>Transcoder</tt> that prints SVG images.
 * This class works as follows: any-time the transcode method
 * is invoked, the corresponding input is cached and nothing
 * else happens. <br />
 * However, the <tt>PrintTranscoder</tt> is also a Printable. If used
 * in a print operation, it will print each of the input
 * it cached, one input per page.
 * <br />
 * The <tt>PrintTranscoder</tt> uses several different hints that
 * guide its printing:<br />
 * <ul>
 *   <li><tt>KEY_LANGUAGE, KEY_USER_STYLESHEET_URI, KEY_PIXEL_TO_MM,
 *       KEY_XML_PARSER_CLASSNAME</tt> can be used to set the defaults for
 *       the various SVG properties.</li>
 *   <li><tt>KEY_PAGE_WIDTH, KEY_PAGE_HEIGHT, KEY_MARGIN_TOP, KEY_MARGIN_BOTTOM,
 *       KEY_MARGIN_LEFT, KEY_MARGIN_RIGHT</tt> and <tt>KEY_PAGE_ORIENTATION</tt>
 *       can be used to specify the printing page characteristics.</li>
 *   <li><tt>KEY_WIDTH, KEY_HEIGHT</tt> can be used to specify how to scale the
 *       SVG image</li>
 *   <li><tt>KEY_SCALE_TO_PAGE</tt> can be used to specify whether or not the
 *       SVG image should be scaled uniformly to fit into the printed page or
 *       if it should just be centered into the printed page.</li>
 * </ul>
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class PrintTranscoder extends XMLAbstractTranscoder
    implements Printable{
    /**
     * Set of inputs this transcoder has been requested to
     * transcode so far
     */
    private Vector inputs = new Vector();

    /**
     * Currently printing set of pages. This vector is
     * created as a clone of inputs when the first page is printed.
     */
    private Vector printedInputs = null;

    /**
     * Current GVT Tree, i.e., the GVT tree representing the page
     * being printed currently.
     */
    private GraphicsNode root;

    /**
     * Index of the page corresponding to root
     */
    private int curIndex = -1;

    /**
     * Current area of interest.
     */
    private Rectangle2D curAOI;

    /**
     * Transform needed to render the current area of interest
     */
    private AffineTransform curTxf;

    /**
     * GraphicsNodeRenderContext used by this transcoder to render
     * the GVT trees.
     */
    private GraphicsNodeRenderContext nodeRenderContext;

    /**
     * UserAgent. Configuration comes from hints
     */
    private UserAgent userAgent = new PrintTranscoderUserAgent();

    /**
     * Creates a <tt>DocumentFactory</tt> that is used to create an SVG DOM
     * tree. The specified DOM Implementation is ignored and the Batik
     * SVG DOM Implementation is automatically used.
     *
     * @param domImpl the DOM Implementation (not used)
     * @param parserClassname the XML parser classname
     */
    protected DocumentFactory createDocumentFactory(DOMImplementation domImpl,
                                                    String parserClassname) {
        return new SAXSVGDocumentFactory(parserClassname);
    }

    /**
     * Constructs a new transcoder that prints images.
     */
    public PrintTranscoder() {
        hints.put(KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
                  SVGConstants.SVG_NAMESPACE_URI);
        hints.put(KEY_DOCUMENT_ELEMENT,
                  SVGConstants.SVG_SVG_TAG);
        hints.put(KEY_DOM_IMPLEMENTATION,
                  SVGDOMImplementation.getDOMImplementation());
    }

    public void transcode(TranscoderInput in,
                           TranscoderOutput out){
        if(in != null){
            inputs.addElement(in);
        }
    }

    /**
     * Convenience method
     */
    public void print() throws PrinterException{
        //
        // Now, request the transcoder to actually perform the
        // printing job.
        //
        PrinterJob printerJob =
            PrinterJob.getPrinterJob();

        PageFormat pageFormat =
            printerJob.defaultPage();

        //
        // Set the page parameters from the hints
        //
        Paper paper = pageFormat.getPaper();

        Float pageWidth = (Float)hints.get(KEY_PAGE_WIDTH);
        Float pageHeight = (Float)hints.get(KEY_PAGE_HEIGHT);
        if(pageWidth != null){
            paper.setSize(pageWidth.floatValue(),
                          paper.getHeight());
        }
        if(pageHeight != null){
            paper.setSize(paper.getWidth(),
                          pageHeight.floatValue());
        }

        float x=0, y=0, width=(float)paper.getWidth(), height=(float)paper.getHeight();

        Float leftMargin = (Float)hints.get(KEY_MARGIN_LEFT);
        Float topMargin = (Float)hints.get(KEY_MARGIN_TOP);
        Float rightMargin = (Float)hints.get(KEY_MARGIN_RIGHT);
        Float bottomMargin = (Float)hints.get(KEY_MARGIN_BOTTOM);

        if(leftMargin != null){
            x = leftMargin.floatValue();
            width -= leftMargin.floatValue();
        }
        if(topMargin != null){
            y = topMargin.floatValue();
            height -= topMargin.floatValue();
        }
        if(rightMargin != null){
            width -= rightMargin.floatValue();
        }
        if(bottomMargin != null){
            height -= bottomMargin.floatValue();
        }

        paper.setImageableArea(x, y, width, height);

        String pageOrientation = (String)hints.get(KEY_PAGE_ORIENTATION);
        if(VALUE_PAGE_ORIENTATION_PORTRAIT.equalsIgnoreCase(pageOrientation)){
            pageFormat.setOrientation(pageFormat.PORTRAIT);
        }
        else if(VALUE_PAGE_ORIENTATION_LANDSCAPE.equalsIgnoreCase(pageOrientation)){
            pageFormat.setOrientation(pageFormat.LANDSCAPE);
        }
        else if(VALUE_PAGE_ORIENTATION_REVERSE_LANDSCAPE.equalsIgnoreCase(pageOrientation)){
            pageFormat.setOrientation(pageFormat.REVERSE_LANDSCAPE);
        }

        pageFormat.setPaper(paper);
        pageFormat = printerJob.validatePage(pageFormat);

        // Print on the default printer: there is no way in
        // the Java API to set the printer by program (without
        // a GUI).
        printerJob.setPrintable(this, pageFormat);
        printerJob.print();

    }

    /**
     * Printable implementation
     */
    public int print(Graphics _g, PageFormat pageFormat, int pageIndex){
        //
        // On the first page, take a snapshot of the vector of
        // TranscodeInputs.
        //
        if(pageIndex == 0){
            printedInputs = (Vector)inputs.clone();
        }

        //
        // If we have already printed each page, return
        //
        if(pageIndex >= printedInputs.size()){
            curIndex = -1;
            System.out.println("Done");
            return NO_SUCH_PAGE;
        }

        //
        // Load a new document now if we are printing a new page
        //
        if(curIndex != pageIndex){
            // The following call will invoke this class' transcode
            // method which takes a document as an input. That method
            // builds the GVT root tree.{
            try{
                super.transcode((TranscoderInput)printedInputs.elementAt(pageIndex),
                                null);
                curIndex = pageIndex;
            }catch(TranscoderException e){
                drawError(_g, e);
                return PAGE_EXISTS;
            }
        }

        // Cast to Graphics2D to access Java 2D features
        Graphics2D g = (Graphics2D)_g;
        g.addRenderingHints(nodeRenderContext.getRenderingHints());


        //
        // Compute transform so that the SVG document fits on one page
        //
        AffineTransform t = g.getTransform();
        Shape clip = g.getClip();

        Rectangle2D bounds = curAOI;

        double scaleX = pageFormat.getImageableWidth() / bounds.getWidth();
        double scaleY = pageFormat.getImageableHeight() / bounds.getHeight();
        double scale = scaleX < scaleY ? scaleX : scaleY;

        // Check hint to know if scaling is really needed
        Boolean scaleToPage = (Boolean)hints.get(KEY_SCALE_TO_PAGE);
        if(scaleToPage != null && !scaleToPage.booleanValue()){
            scale = 1;
        }

        double xMargin = (pageFormat.getImageableWidth() - bounds.getWidth()*scale)/2;
        double yMargin = (pageFormat.getImageableHeight() - bounds.getHeight()*scale)/2;
        g.translate(pageFormat.getImageableX() + xMargin,
                    pageFormat.getImageableY() + yMargin);
        g.scale(scale, scale);


        //
        // Append transform to selected area
        //
        g.transform(curTxf);

        g.clip(curAOI);

        //
        // Delegate rendering to painter
        //
        try{
            root.paint(g, nodeRenderContext);
        }catch(Exception e){
            g.setTransform(t);
            g.setClip(clip);
            drawError(_g, e);
        }

        //
        // Restore transform and clip
        //
        g.setTransform(t);
        g.setClip(clip);

        // g.setPaint(Color.black);
        // g.drawString(uris[pageIndex], 30, 30);


        //
        // Return status indicated that we did paint a page
        //
        return PAGE_EXISTS;
    }

    /**
     * Prints an error on the output page
     */
    private void drawError(Graphics g, Exception e){
        // Do nothing now.
    }

    /**
     * Transcodes the specified Document as an image in the specified output.
     * @param document the document to transcode
     * @param output the ouput where to transcode
     * @exception TranscoderException if an error occured while transcoding
     */
    protected void transcode(Document document, TranscoderOutput output)
            throws TranscoderException {
        // We can only handle SVG documents.
        if (!(document instanceof SVGOMDocument)) {
            // <!> TO BE FIXED WHEN WE DO ERROR HANDLING PROPERLY
            throw new TranscoderException("");
        }

        SVGDocument svgDoc = (SVGDocument)document;
        SVGSVGElement root = svgDoc.getRootElement();

        //
        // Initialize the SVG document with the appropriate context
        //
        String parserClassname = (String)hints.get(KEY_XML_PARSER_CLASSNAME);
        DefaultSVGContext svgCtx = new DefaultSVGContext();
        svgCtx.setPixelToMM(userAgent.getPixelToMM());
        ((SVGOMDocument)document).setSVGContext(svgCtx);

        //
        // Get the 'width' and 'height' attributes of the SVG document
        //
        float docWidth = (int)root.getWidth().getBaseVal().getValue();
        float docHeight = (int)root.getHeight().getBaseVal().getValue();

        //
        // Compute the image's width and height according the hints
        //
        float imgWidth = -1;
        if (hints.containsKey(KEY_WIDTH)) {
            imgWidth = ((Float)hints.get(KEY_WIDTH)).floatValue();
        }
        float imgHeight = -1;
        if (hints.containsKey(KEY_HEIGHT)) {
            imgHeight = ((Float)hints.get(KEY_HEIGHT)).floatValue();
        }
        float width, height;
        if (imgWidth > 0 && imgHeight > 0) {
            width = imgWidth;
            height = imgHeight;
        } else if (imgHeight > 0) {
            width = (docWidth * imgHeight) / docHeight;
            height = imgHeight;
        } else if (imgWidth > 0) {
            width = imgWidth;
            height = (docHeight * imgWidth) / docWidth;
        } else {
            width = docWidth;
            height = docHeight;
        }

        //
        // Compute the preserveAspectRatio matrix
        //
        AffineTransform Px =
            ViewBox.getPreserveAspectRatioTransform(root, width, height);
        if (Px.isIdentity() && (width != docWidth || height != docHeight)) {
            // The document has no viewBox, we need to resize it by hand.
            // we want to keep the document size ratio
            float d = Math.max(docWidth, docHeight);
            float dd = Math.max(width, height);
            float scale = dd/d;
            Px = AffineTransform.getScaleInstance(scale, scale);
        }

        //
        // Take the AOI into account if any
        //
        if (hints.containsKey(KEY_AOI)) {
            Rectangle2D aoi = (Rectangle2D)hints.get(KEY_AOI);
            // transform the AOI into the image's coordinate system
            aoi = Px.createTransformedShape(aoi).getBounds2D();
            AffineTransform Mx = new AffineTransform();
            double sx = width / aoi.getWidth();
            double sy = height / aoi.getHeight();
            double tx = -aoi.getX();
            double ty = -aoi.getY();
            Mx.translate(tx, ty);
            // take the AOI transformation matrix into account
            // we apply first the preserveAspectRatio matrix
            Px.preConcatenate(Mx);

            curAOI = aoi;
        }
        else{
            curAOI = new Rectangle2D.Float(0, 0, width, height);
        }

        curTxf = Px;

        //
        // Build the GVT tree
        //
        GVTBuilder builder = new GVTBuilder();
        GraphicsNodeRenderContext rc = getRenderContext();
        BridgeContext ctx = new BridgeContext(userAgent, rc);
        this.root = builder.build(ctx, svgDoc);

    }

    /**
     * Builds a GraphicsNodeRenderContext
     */
    public GraphicsNodeRenderContext getRenderContext() {
        if (nodeRenderContext == null) {
            RenderingHints hints = new RenderingHints(null);
            hints.put(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);

            hints.put(RenderingHints.KEY_INTERPOLATION,
                  RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            FontRenderContext fontRenderContext =
                new FontRenderContext(new AffineTransform(), true, true);

            TextPainter textPainter = new StrokingTextPainter();

            GraphicsNodeRableFactory gnrFactory =
                new ConcreteGraphicsNodeRableFactory();

            nodeRenderContext =
                new GraphicsNodeRenderContext(new AffineTransform(),
                                          null,
                                          hints,
                                          fontRenderContext,
                                          textPainter,
                                          gnrFactory);
            }

        return nodeRenderContext;
    }

    /**
     * A user agent implementation for <tt>PrintTranscoder</tt>.
     */
    class PrintTranscoderUserAgent implements UserAgent {

        /**
         * Returns the default size of this user agent (400x400).
         */
        public Dimension2D getViewportSize() {
            return new Dimension(400, 400);
        }

        /**
         * Displays the specified error message using the <tt>ErrorHandler</tt>.
         */
        public void displayError(String message) {
            try {
                handler.error(new TranscoderException(message));
            } catch (TranscoderException ex) {
                throw new RuntimeException();
            }
        }

        /**
         * Displays the specified error using the <tt>ErrorHandler</tt>.
         */
        public void displayError(Exception e) {
            try {
                handler.error(new TranscoderException(e));
            } catch (TranscoderException ex) {
                throw new RuntimeException();
            }
        }

        /**
         * Displays the specified message using the <tt>ErrorHandler</tt>.
         */
        public void displayMessage(String message) {
            try {
                handler.warning(new TranscoderException(message));
            } catch (TranscoderException ex) {
                throw new RuntimeException();
            }
        }

        /**
         * Returns the pixel to millimeter conversion factor specified in the
         * <tt>TranscodingHints</tt> or 0.3528 if any.
         */
        public float getPixelToMM() {
            if (hints.containsKey(KEY_PIXEL_TO_MM)) {
                return ((Float)hints.get(KEY_PIXEL_TO_MM)).floatValue();
            } else {
                // return 0.3528f; // 72 dpi
                return 0.26458333333333333333333333333333f; // 96dpi
            }
        }

        /**
         * Returns the user language specified in the
         * <tt>TranscodingHints</tt> or "en" (english) if any.
         */
        public String getLanguages() {
            if (hints.containsKey(KEY_LANGUAGE)) {
                return (String)hints.get(KEY_LANGUAGE);
            } else {
                return "en";
            }
        }

        /**
         * Returns the user stylesheet specified in the
         * <tt>TranscodingHints</tt> or null if any.
         */
        public String getUserStyleSheetURI() {
            return (String)hints.get(KEY_USER_STYLESHEET_URI);
        }

        /**
         * Returns the XML parser to use from the TranscodingHints.
         */
        public String getXMLParserClassName() {
            return (String)hints.get(KEY_XML_PARSER_CLASSNAME);
        }

        /**
         * Unsupported operation.
         */
        public EventDispatcher getEventDispatcher() {
            return null;
        }

        /**
         * Unsupported operation.
         */
        public void openLink(SVGAElement elt) { }

        /**
         * Unsupported operation.
         */
        public void setSVGCursor(Cursor cursor) { }

        /**
         * Unsupported operation.
         */
        public void runThread(Thread t) { }

        /**
         * Unsupported operation.
         */
        public AffineTransform getTransform() {
            return null;
        }

        /**
         * Unsupported operation.
         */
        public Point getClientAreaLocationOnScreen() {
            return new Point();
        }
    }

    /**
     * The pageWidth key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_PAGE_WIDTH</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Length</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">None</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">The width of the print page</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_PAGE_WIDTH
        = new LengthKey(21);
    public static final String KEY_PAGE_WIDTH_STR = "pageWidth";

    /**
     * The pageHeight key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_PAGE_HEIGHT</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Length</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">None</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">The height of the print page</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_PAGE_HEIGHT
        = new LengthKey(22);
    public static final String KEY_PAGE_HEIGHT_STR = "pageHeight";

    /**
     * The marginTop key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_MARGIN_TOP</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Length</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">None</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">The print page top margin</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_MARGIN_TOP
        = new LengthKey(23);
    public static final String KEY_MARGIN_TOP_STR = "marginTop";


    /**
     * The marginRight key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_MARGIN_RIGHT</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Length</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">None</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">The print page right margin</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_MARGIN_RIGHT
        = new LengthKey(24);
    public static final String KEY_MARGIN_RIGHT_STR = "marginRight";

    /**
     * The marginBottom key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_MARGIN_BOTTOM</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Length</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">None</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">The print page bottom margin</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_MARGIN_BOTTOM
        = new LengthKey(25);
    public static final String KEY_MARGIN_BOTTOM_STR = "marginBottom";

    /**
     * The marginLeft key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_MARGIN_LEFT</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Length</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">None</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">The print page left margin</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_MARGIN_LEFT
        = new LengthKey(26);
    public static final String KEY_MARGIN_LEFT_STR = "marginLeft";

    /**
     * The pageOrientation key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_PAGE_ORIENTATION</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">String</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">VALUE_PAGE_ORIENTATION_PORTRAIT</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">The print page's orientation</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_PAGE_ORIENTATION
        = new StringKey(27);
    public static final String KEY_PAGE_ORIENTATION_STR         = "pageOrientation";
    public static final String VALUE_PAGE_ORIENTATION_PORTRAIT  = "portrait";
    public static final String VALUE_PAGE_ORIENTATION_LANDSCAPE = "landscape";
    public static final String VALUE_PAGE_ORIENTATION_REVERSE_LANDSCAPE = "reverseLandscape";

    /**
     * The scaleToPage key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_SCALE_TO_PAGE</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Boolean</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">true</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">No</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">Specifies whether or not the SVG images are scaled to
     *                  fit into the printed page</TD></TR>
     * </TABLE> */
    public static final TranscodingHints.Key KEY_SCALE_TO_PAGE
        = new BooleanKey(28);
    public static final String KEY_SCALE_TO_PAGE_STR         = "scaleToPage";

    public static final String USAGE = "java org.apache.batik.transcoder.print.PrintTranscoder <svgFileToPrint>";

    public static void main(String args[]) throws Exception{
        if(args.length < 1){
            System.err.println(USAGE);
            System.exit(0);
        }

        //
        // Builds a PrintTranscoder
        //
        PrintTranscoder transcoder = new PrintTranscoder();

        //
        // Set the hints, from the command line arguments
        //

        // Language
        setTranscoderFloatHint(transcoder,
                               KEY_LANGUAGE_STR,
                               KEY_LANGUAGE);

        // User stylesheet
        setTranscoderFloatHint(transcoder,
                               KEY_USER_STYLESHEET_URI_STR,
                               KEY_USER_STYLESHEET_URI);

        // XML parser
        setTranscoderStringHint(transcoder,
                                 KEY_XML_PARSER_CLASSNAME_STR,
                                 KEY_XML_PARSER_CLASSNAME);

        // Scale to page
        setTranscoderBooleanHint(transcoder,
                                 KEY_SCALE_TO_PAGE_STR,
                                 KEY_SCALE_TO_PAGE);

        // AOI
        setTranscoderRectangleHint(transcoder,
                                   KEY_AOI_STR,
                                   KEY_AOI);


        // Image size
        setTranscoderFloatHint(transcoder,
                               KEY_WIDTH_STR,
                               KEY_WIDTH);
        setTranscoderFloatHint(transcoder,
                               KEY_HEIGHT_STR,
                               KEY_HEIGHT);

        // Pixel to millimeter
        setTranscoderFloatHint(transcoder,
                               KEY_PIXEL_TO_MM_STR,
                               KEY_PIXEL_TO_MM);

        // Page orientation
        setTranscoderStringHint(transcoder,
                                KEY_PAGE_ORIENTATION_STR,
                                KEY_PAGE_ORIENTATION);

        // Page size
        setTranscoderFloatHint(transcoder,
                               KEY_PAGE_WIDTH_STR,
                               KEY_PAGE_WIDTH);
        setTranscoderFloatHint(transcoder,
                               KEY_PAGE_HEIGHT_STR,
                               KEY_PAGE_HEIGHT);

        // Margins
        setTranscoderFloatHint(transcoder,
                               KEY_MARGIN_TOP_STR,
                               KEY_MARGIN_TOP);
        setTranscoderFloatHint(transcoder,
                               KEY_MARGIN_RIGHT_STR,
                               KEY_MARGIN_RIGHT);
        setTranscoderFloatHint(transcoder,
                               KEY_MARGIN_BOTTOM_STR,
                               KEY_MARGIN_BOTTOM);
        setTranscoderFloatHint(transcoder,
                               KEY_MARGIN_LEFT_STR,
                               KEY_MARGIN_LEFT);


        //
        // First, request the transcoder to transcode
        // each of the input files
        //
        for(int i=0; i<args.length; i++){
            transcoder.transcode(new TranscoderInput(new File(args[i]).toURL().toString()),
                                 null);
        }

        //
        // Now, print...
        //
        transcoder.print();
    }

    public static void setTranscoderFloatHint(Transcoder transcoder,
                                              String property,
                                              TranscodingHints.Key key){
        String str = System.getProperty(property);
        if(str != null){
            try{
                Float value = new Float(Float.parseFloat(str));
                transcoder.addTranscodingHint(key, value);
            }catch(NumberFormatException e){
                handleValueError(property, str);
            }
        }
    }

    public static void setTranscoderRectangleHint(Transcoder transcoder,
                                                  String property,
                                                  TranscodingHints.Key key){
        String str = System.getProperty(property);
        if(str != null){
            StringTokenizer st = new StringTokenizer(str, " ,");
            if(st.countTokens() != 4){
                handleValueError(property, str);
            }

            try{
                String x = st.nextToken();
                String y = st.nextToken();
                String width = st.nextToken();
                String height = st.nextToken();
                Rectangle2D r = new Rectangle2D.Float(Float.parseFloat(x),
                                                      Float.parseFloat(y),
                                                      Float.parseFloat(width),
                                                      Float.parseFloat(height));
                transcoder.addTranscodingHint(key, r);
            }catch(NumberFormatException e){
                handleValueError(property, str);
            }
        }
    }

    public static void setTranscoderBooleanHint(Transcoder transcoder,
                                                String property,
                                                TranscodingHints.Key key){
        String str = System.getProperty(property);
        if(str != null){
            Boolean value = new Boolean("true".equalsIgnoreCase(str));
            transcoder.addTranscodingHint(key, value);
        }
    }

    public static void setTranscoderStringHint(Transcoder transcoder,
                                              String property,
                                              TranscodingHints.Key key){
        String str = System.getProperty(property);
        if(str != null){
            transcoder.addTranscodingHint(key, str);
        }
    }

    public static void handleValueError(String property,
                                        String value){
        System.err.println("Invalid " + property + " value : " + value);
        System.exit(1);
    }
}

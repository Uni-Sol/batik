/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.svggen;

import java.awt.geom.*;
import java.awt.*;
import java.util.Map;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class that converts an custom Paint object into
 * a set of SVG properties and definitions.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 * @see                org.apache.batik.svggen.SVGPaint
 */
public class SVGCustomPaint extends AbstractSVGConverter{
    public static final String ERROR_EXTENSION_HANDLER_NULL = "extensionHandler should not be null";

    /**
     * Paint conversion is handed to the extensionHandler.
     * This class keeps track of already converted Paints
     */
    private ExtensionHandler extensionHandler;

    /**
     * @param domFactory for use by SVGCustomPaint to build Elements
     */
    public SVGCustomPaint(Document domFactory,
                          ExtensionHandler extensionHandler){
        super(domFactory);

        if(extensionHandler == null)
            throw new IllegalArgumentException(ERROR_EXTENSION_HANDLER_NULL);

        this.extensionHandler = extensionHandler;
    }

    /**
     * Converts part or all of the input GraphicContext into
     * a set of attribute/value pairs and related definitions
     *
     * @param gc GraphicContext to be converted
     * @return descriptor of the attributes required to represent
     *         some or all of the GraphicContext state, along
     *         with the related definitions
     * @see org.apache.batik.svggen.SVGDescriptor
     */
    public SVGDescriptor toSVG(GraphicContext gc){
        return toSVG(gc.getPaint());
    }

    /**
     * @param paint the Paint object to convert to SVG
     * @return a description of the SVG paint and opacity corresponding
     *         to the Paint. The definiton of the paint is put in the
     *         linearGradientDefsMap
     */
    public SVGPaintDescriptor toSVG(Paint paint){
        SVGPaintDescriptor paintDesc = (SVGPaintDescriptor)descMap.get(paint);

        if(paintDesc == null){
            // First time this paint is used. Request handler
            // to do the convertion
            paintDesc = extensionHandler.handlePaint(paint, domFactory);

            if(paintDesc != null){
                Element def = paintDesc.getDef();
                if(def != null) defSet.add(def);
                descMap.put(paint, paintDesc);
            }
        }

        return paintDesc;
    }
}
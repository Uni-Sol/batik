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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class that converts a Rectangle2D or RoundRectangle2D
 * object into an SVG element.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class SVGRectangle extends SVGGraphicObjectConverter{
    /**
     * @param domFactory used to build Elements
     */
    public SVGRectangle(Document domFactory){
        super(domFactory);
    }

    /**
     * @param rect rectangle object to convert to SVG
     */
    public Element toSVG(Rectangle2D rect){
        return toSVG((RectangularShape)rect);
    }


    /**
     * @param rect rectangle object to convert to SVG
     */
    public Element toSVG(RoundRectangle2D rect){
        Element svgRect = toSVG((RectangularShape)rect);
        svgRect.setAttributeNS(null, SVG_RX_ATTRIBUTE, doubleString(rect.getArcWidth()/2));
        svgRect.setAttributeNS(null, SVG_RY_ATTRIBUTE, doubleString(rect.getArcHeight()/2));
        return svgRect;
    }


    /**
     * @param rect rectangle object to convert to SVG
     */
    private Element toSVG(RectangularShape rect){
        Element svgRect = domFactory.createElementNS(SVG_NAMESPACE_URI, TAG_RECT);
        svgRect.setAttributeNS(null, SVG_X_ATTRIBUTE, doubleString(rect.getX()));
        svgRect.setAttributeNS(null, SVG_Y_ATTRIBUTE, doubleString(rect.getY()));
        svgRect.setAttributeNS(null, SVG_WIDTH_ATTRIBUTE, doubleString(rect.getWidth()));
        svgRect.setAttributeNS(null, SVG_HEIGHT_ATTRIBUTE, doubleString(rect.getHeight()));

        return svgRect;
    }

    /**
     * Unit testing
     */
    public static void main(String args[]) throws Exception{
        Document domFactory = TestUtil.getDocumentPrototype();
        SVGRectangle converter = new SVGRectangle(domFactory);

        Element rects[] = {
            converter.toSVG(new Rectangle(10, 20, 30, 40)),
            converter.toSVG(new Rectangle2D.Double(100., 200., 300., 400.)),
            converter.toSVG(new Rectangle2D.Float(1000f, 2000f, 3000f, 4000f)),
            converter.toSVG(new RoundRectangle2D.Double(15., 16., 17., 18., 30., 20.)),
            converter.toSVG(new RoundRectangle2D.Float(35f, 45f, 55f, 65f, 25f, 45f))
        };

        Element group = domFactory.createElementNS(SVG_NAMESPACE_URI, SVG_G_TAG);
        for(int i=0; i<rects.length; i++)
            group.appendChild(rects[i]);

        TestUtil.trace(group, System.out);
    }
}
/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.svggen;

import java.awt.geom.*;
import java.awt.Shape;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class that converts an Ellipse2D object into
 * a corresponding SVG element, i.e., a circle or an ellipse.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class SVGEllipse extends SVGGraphicObjectConverter{
    /**
     * @param domFactory used to build Elements
     */
    public SVGEllipse(Document domFactory){
        super(domFactory);
    }

    /**
     * @param ellipse the Ellipse2D object to be converted
     */
    public Element toSVG(Ellipse2D ellipse){
        if(ellipse.getWidth() == ellipse.getHeight())
            return toSVGCircle(ellipse);
        else
            return toSVGEllipse(ellipse);
    }

    /**
     * @param ellipse the Ellipse2D object to be converted to a circle
     */
    private Element toSVGCircle(Ellipse2D ellipse){
        Element svgCircle = domFactory.createElementNS(SVG_NAMESPACE_URI, SVG_CIRCLE_TAG);
        svgCircle.setAttributeNS(null, SVG_CX_ATTRIBUTE,
                               doubleString(ellipse.getX() + ellipse.getWidth()/2));
        svgCircle.setAttributeNS(null, SVG_CY_ATTRIBUTE,
                               doubleString(ellipse.getY() + ellipse.getHeight()/2));
        svgCircle.setAttributeNS(null, SVG_R_ATTRIBUTE, doubleString(ellipse.getWidth()/2));
        return svgCircle;
    }

    /**
     * @param ellipse the Ellipse2D object to be converted to an ellipse
     */
    private Element toSVGEllipse(Ellipse2D ellipse){
        Element svgCircle = domFactory.createElementNS(SVG_NAMESPACE_URI, SVG_ELLIPSE_TAG);
        svgCircle.setAttributeNS(null, SVG_CX_ATTRIBUTE,
                               doubleString(ellipse.getX() + ellipse.getWidth()/2));
        svgCircle.setAttributeNS(null, SVG_CY_ATTRIBUTE,
                               doubleString(ellipse.getY() + ellipse.getHeight()/2));
        svgCircle.setAttributeNS(null, SVG_RX_ATTRIBUTE,
                               doubleString(ellipse.getWidth()/2));
        svgCircle.setAttributeNS(null, SVG_RY_ATTRIBUTE,
                               doubleString(ellipse.getHeight()/2));
        return svgCircle;
    }

    /**
     * Unit testing
     */
    public static void main(String args[]) throws Exception {
        Ellipse2D ellipses [] = { new Ellipse2D.Float(0, 0, 100, 100),
                                  new Ellipse2D.Double(40, 40, 240, 240),
                                  new Ellipse2D.Float(0, 0, 100, 200),
                                  new Ellipse2D.Float(40, 100, 240, 200) };

        Document domFactory = TestUtil.getDocumentPrototype();
        SVGEllipse converter = new SVGEllipse(domFactory);
        Element group = domFactory.createElementNS(SVG_NAMESPACE_URI, SVG_G_TAG);
        for(int i=0; i<ellipses.length; i++)
            group.appendChild(converter.toSVG(ellipses[i]));

        TestUtil.trace(group, System.out);
    }
}
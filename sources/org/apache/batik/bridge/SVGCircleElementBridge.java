/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.IllegalAttributeValueException;
import org.apache.batik.bridge.MissingAttributeException;
import org.apache.batik.gvt.ShapeNode;
import org.apache.batik.bridge.resources.Messages;
import org.apache.batik.util.UnitProcessor;

import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.svg.SVGElement;

/**
 * A factory for the &lt;circle> SVG element.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class SVGCircleElementBridge extends SVGShapeElementBridge {

    /**
     * Returns a <tt>Ellipse2D.Float</tt>.
     */
    protected void buildShape(BridgeContext ctx,
                              SVGElement svgElement,
                              ShapeNode node,
                              CSSStyleDeclaration decl,
                              UnitProcessor.Context uctx) {

        // parse the cx attribute, (default is 0)
        String s = svgElement.getAttributeNS(null, SVG_CX_ATTRIBUTE);
        float cx = 0;
        if (s.length() != 0) {
            cx = SVGUtilities.svgToUserSpace(svgElement,
                                             SVG_CX_ATTRIBUTE, s,
                                             uctx,
                                             UnitProcessor.HORIZONTAL_LENGTH);
        }

        // parse the cy attribute, (default is 0)
        s = svgElement.getAttributeNS(null, SVG_CY_ATTRIBUTE);
        float cy = 0;
        if (s.length() != 0) {
            cy = SVGUtilities.svgToUserSpace(svgElement,
                                             SVG_CY_ATTRIBUTE, s,
                                             uctx,
                                             UnitProcessor.VERTICAL_LENGTH);
        }

        // parse the r attribute, (required and must be positive)
        s = svgElement.getAttributeNS(null, SVG_R_ATTRIBUTE);
        float r;
        if (s.length() == 0) {
            throw new MissingAttributeException(
                Messages.formatMessage("circle.r.required", null));
        } else {
            r = SVGUtilities.svgToUserSpace(svgElement,
                                            SVG_R_ATTRIBUTE, s,
                                            uctx,
                                            UnitProcessor.OTHER_LENGTH);
            if (r < 0) {
                throw new IllegalAttributeValueException(
                    Messages.formatMessage("circle.r.negative", null));
            }
        }

        float x = cx - r;
        float y = cy - r;
        float w = r * 2;
        node.setShape(new Ellipse2D.Float(x, y, w, w));
    }
}
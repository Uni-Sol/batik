/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.renderable.FilterAlphaRable;
import org.apache.batik.ext.awt.image.renderable.FloodRable8Bit;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.filter.BackgroundRable8Bit;
import org.apache.batik.util.SVGConstants;

import org.w3c.dom.Element;
import org.w3c.dom.css.CSSStyleDeclaration;

/**
 * The base bridge class for SVG filter primitives.
 *
 * @author <a href="mailto:tkormann@apache.org">Thierry Kormann</a>
 * @version $Id$
 */
public abstract class SVGAbstractFilterPrimitiveElementBridge
    implements FilterPrimitiveBridge, SVGConstants, ErrorConstants {

    /**
     * Constructs a new bridge for a filter primitive element.
     */
    protected SVGAbstractFilterPrimitiveElementBridge() {}

    /**
     * Returns the input source of the specified filter primitive
     * element defined by its 'in' attribute.
     *
     * @param filterElement the filter primitive element
     * @param filteredElement the element on which the filter is referenced
     * @param filteredNode the graphics node on which the filter is applied
     * @param inputFilter the default input filter
     * @param filterMap the map that containes the named filter primitives
     * @param ctx the bridge context
     */
    protected static Filter getIn(Element filterElement,
                                  Element filteredElement,
                                  GraphicsNode filteredNode,
                                  Filter inputFilter,
                                  Map filterMap,
                                  BridgeContext ctx) {

        String s = filterElement.getAttributeNS(null, SVG_IN_ATTRIBUTE);
        if (s.length() == 0) {
            return inputFilter;
        } else {
            return getFilterSource(filterElement,
                                   s,
                                   filteredElement,
                                   filteredNode,
                                   filterMap,
                                   ctx);
        }
    }

    /**
     * Returns the input source of the specified filter primitive
     * element defined by its 'in2' attribute. The 'in2' attribute is assumed
     * to be required if the subclasses ask for it.
     *
     * @param filterElement the filter primitive element
     * @param filteredElement the element on which the filter is referenced
     * @param filteredNode the graphics node on which the filter is applied
     * @param inputFilter the default input filter
     * @param filterMap the map that containes the named filter primitives
     * @param ctx the bridge context
     */
    protected static Filter getIn2(Element filterElement,
                                   Element filteredElement,
                                   GraphicsNode filteredNode,
                                   Filter inputFilter,
                                   Map filterMap,
                                   BridgeContext ctx) {

        String s = filterElement.getAttributeNS(null, SVG_IN2_ATTRIBUTE);
        if (s.length() == 0) {
            throw new BridgeException(filterElement, ERR_ATTRIBUTE_MISSING,
                                      new Object [] {SVG_IN2_ATTRIBUTE});
        }
        return getFilterSource(filterElement,
                               s,
                               filteredElement,
                               filteredNode,
                               filterMap,
                               ctx);
    }

    /**
     * Updates the filterMap according to the specified parameters.
     *
     * @param filterElement the filter primitive element
     * @param filter the filter that is part of the filter chain
     * @param filterMap the filter map to update
     */
    protected static void updateFilterMap(Element filterElement,
                                          Filter filter,
                                          Map filterMap) {

        String s = filterElement.getAttributeNS(null, SVG_RESULT_ATTRIBUTE);
        if ((s.length() != 0) && (s.trim().length() != 0)) {
            filterMap.put(s, filter);
        }
    }

    /**
     * Returns the filter source according to the specified parameters.
     *
     * @param filterElement the filter element
     * @param s the input of the filter primitive
     * @param filteredElement the filtered element
     * @param filteredNode the filtered graphics node
     * @param filterMap the filter map that contains named filter primitives
     * @param ctx the bridge context
     */
    static Filter getFilterSource(Element filterElement,
                                  String s,
                                  Element filteredElement,
                                  GraphicsNode filteredNode,
                                  Map filterMap,
                                  BridgeContext ctx) {

        int length = s.length();
        Filter source = null;
        switch (length) {
        case 13:
            if (SVG_SOURCE_GRAPHIC_VALUE.equals(s)) {
                // SourceGraphic
                source = (Filter)filterMap.get(SVG_SOURCE_GRAPHIC_VALUE);
            }
            break;
        case 11:
            if (s.charAt(1) == SVG_SOURCE_ALPHA_VALUE.charAt(1)) {
                if (SVG_SOURCE_ALPHA_VALUE.equals(s)) {
                    // SourceAlpha
                    source = (Filter)filterMap.get(SVG_SOURCE_GRAPHIC_VALUE);
                    source = new FilterAlphaRable(source);
                }
            } else if (SVG_STROKE_PAINT_VALUE.equals(s)) {
                    // StrokePaint
                    CSSStyleDeclaration cssDecl
                        = CSSUtilities.getComputedStyle(filteredElement);
                    Paint paint = PaintServer.convertStrokePaint
                        (filteredElement,filteredNode, ctx);
                    // <!> FIXME: Should we create a transparent flood ???
                    source = new FloodRable8Bit(INFINITE_FILTER_REGION, paint);
            }
            break;
        case 15:
            if (s.charAt(10) == SVG_BACKGROUND_IMAGE_VALUE.charAt(10)) {
                if (SVG_BACKGROUND_IMAGE_VALUE.equals(s)) {
                    // BackgroundImage
                    source = new BackgroundRable8Bit
                        (filteredNode, ctx.getGraphicsNodeRenderContext());
                }
            } else if (SVG_BACKGROUND_ALPHA_VALUE.equals(s)) {
                // BackgroundAlpha
                source = new BackgroundRable8Bit
                    (filteredNode, ctx.getGraphicsNodeRenderContext());
                source = new FilterAlphaRable(source);
            }
            break;
        case 9:
            if (SVG_FILL_PAINT_VALUE.equals(s)) {
                // FillPaint
                CSSStyleDeclaration cssDecl
                    = CSSUtilities.getComputedStyle(filteredElement);
                Paint paint = PaintServer.convertFillPaint
                    (filteredElement,filteredNode, ctx);
                if (paint == null) {
                    paint = new Color(0, 0, 0, 0); // create a transparent flood
                }
                source = new FloodRable8Bit(INFINITE_FILTER_REGION, paint);
            }
            break;
        }
        if (source == null) {
            // <identifier>
            source = (Filter)filterMap.get(s);
        }
        return source;
    }

    /**
     * This is a bit of a hack but we set the flood bounds to
     * -floatmax/2 -> floatmax/2 (should cover the area ok).
     */
    static final Rectangle2D INFINITE_FILTER_REGION
        = new Rectangle2D.Float(-Float.MAX_VALUE/2,
                                -Float.MAX_VALUE/2,
                                Float.MAX_VALUE,
                                Float.MAX_VALUE);



    /**
     * Converts on the specified filter primitive element, the specified
     * attribute that represents an integer and with the specified
     * default value.
     *
     * @param filterElement the filter primitive element
     * @param attrName the name of the attribute
     * @param defaultValue the default value of the attribute
     */
    protected static int convertInteger(Element filterElement,
                                        String attrName,
                                        int defaultValue) {
        String s = filterElement.getAttributeNS(null, attrName);
        if (s.length() == 0) {
            return defaultValue;
        } else {
            try {
                return SVGUtilities.convertSVGInteger(s);
            } catch (NumberFormatException ex) {
                throw new BridgeException
                    (filterElement, ERR_ATTRIBUTE_VALUE_MALFORMED,
                     new Object[] {attrName, s});
            }
        }
    }

    /**
     * Converts on the specified filter primitive element, the specified
     * attribute that represents a float and with the specified
     * default value.
     *
     * @param filterElement the filter primitive element
     * @param attrName the name of the attribute
     * @param defaultValue the default value of the attribute
     */
    protected static float convertNumber(Element filterElement,
                                         String attrName,
                                         float defaultValue) {

        String s = filterElement.getAttributeNS(null, attrName);
        if (s.length() == 0) {
            return defaultValue;
        } else {
            try {
                return SVGUtilities.convertSVGNumber(s);
            } catch (NumberFormatException ex) {
                throw new BridgeException
                    (filterElement, ERR_ATTRIBUTE_VALUE_MALFORMED,
                     new Object[] {attrName, s, ex});
            }
        }
    }

    /**
     * Performs an update according to the specified event.
     *
     * @param evt the event describing the update to perform
     */
    public void update(BridgeMutationEvent evt) {
        throw new Error("Not implemented");
    }
}
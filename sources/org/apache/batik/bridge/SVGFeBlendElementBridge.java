/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Vector;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.BridgeMutationEvent;
import org.apache.batik.bridge.FilterPrimitiveBridge;
import org.apache.batik.bridge.IllegalAttributeValueException;
import org.apache.batik.bridge.MissingAttributeException;

import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.GraphicsNodeRenderContext;
import org.apache.batik.ext.awt.image.renderable.CompositeRable;
import org.apache.batik.ext.awt.image.renderable.CompositeRule;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.renderable.PadMode;

import org.apache.batik.bridge.resources.Messages;
import org.apache.batik.ext.awt.image.renderable.CompositeRable8Bit;
import org.apache.batik.ext.awt.image.renderable.PadRable8Bit;

import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.UnitProcessor;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSStyleDeclaration;

/**
 * This class bridges an SVG <tt>feBlend</tt> element with
 * a concrete <tt>Filter</tt> filter implementation
 *
 * @author <a href="mailto:Thomas.DeWeeese@Kodak.com">Thomas DeWeese</a>
 * @author <a href="mailto:Thierry.Kormann@sophia.inria.fr">Thierry Kormann</a>
 * @version $Id$
 */
public class SVGFeBlendElementBridge implements FilterPrimitiveBridge,
                                                SVGConstants {


    /**
     * Returns the <tt>Filter</tt> that implements the filter
     * operation modeled by the input DOM element
     *
     * @param filteredNode the node to which the filter will be attached.
     * @param bridgeContext the context to use.
     * @param filterElement DOM element that represents a filter abstraction
     * @param in the <tt>Filter</tt> that represents the current
     *        filter input if the filter chain.
     * @param filterRegion the filter area defined for the filter chained
     *        the new node will be part of.
     * @param filterMap a map where the mediator can map a name to the
     *        <tt>Filter</tt> it creates. Other <tt>FilterBridge</tt>s
     *        can then access a filter node from the filterMap if they
     *        know its name.
     */
    public Filter create(GraphicsNode filteredNode,
                         BridgeContext bridgeContext,
                         Element filterElement,
                         Element filteredElement,
                         Filter in,
                         Rectangle2D filterRegion,
                         Map filterMap){

         GraphicsNodeRenderContext rc = 
                         bridgeContext.getGraphicsNodeRenderContext();

        // Extract Blend operation
        CompositeRule rule = getRule(filterElement);

        // Extract sources
        String in1Attr = filterElement.getAttributeNS(null, SVG_IN_ATTRIBUTE);
        Filter in1;
        in1 = CSSUtilities.getFilterSource(filteredNode,
                                           in1Attr,
                                           bridgeContext,
                                           filteredElement,
                                           in,
                                           filterMap);

        String in2Attr = filterElement.getAttributeNS(null, SVG_IN2_ATTRIBUTE);
        if (in2Attr.length() == 0) {
            throw new MissingAttributeException(
                Messages.formatMessage("feBlend.in2.required", null));
        }
        Filter in2;
        in2 = CSSUtilities.getFilterSource(filteredNode,
                                           in2Attr,
                                           bridgeContext,
                                           filteredElement,
                                           in,
                                           filterMap);

        if ((in1 == null) || (in2 == null)) {
            return null;
        }

        //
        // The default region is the union of the input sources
        // regions unless 'in' is 'SourceGraphic' in which case the
        // default region is the filterChain's region
        //
        Filter sourceGraphics = (Filter)filterMap.get(VALUE_SOURCE_GRAPHIC);

        Rectangle2D defaultRegion = in1.getBounds2D();
        defaultRegion.add(in2.getBounds2D());

        if(in1 == sourceGraphics) {
            defaultRegion = filterRegion;
        }

        CSSStyleDeclaration cssDecl
            = bridgeContext.getViewCSS().getComputedStyle(filterElement, null);

        UnitProcessor.Context uctx
            = new DefaultUnitProcessorContext(bridgeContext, cssDecl);

        Rectangle2D blendArea
            = SVGUtilities.convertFilterPrimitiveRegion(filterElement,
                                                        filteredElement,
                                                        defaultRegion,
                                                        filterRegion,
                                                        filteredNode,
                                                        rc,
                                                        uctx);

        // Now, do the blend.
        Filter filter = null;
        Vector srcs = new Vector(2);
        srcs.add(in2);
        srcs.add(in1);
        filter = new CompositeRable8Bit(srcs, rule, true);

        filter = new PadRable8Bit(filter, blendArea, PadMode.ZERO_PAD);

        // Get result attribute and update map
        String result = filterElement.getAttributeNS(null, ATTR_RESULT);
        if((result != null) && (result.trim().length() > 0)){
            filterMap.put(result, filter);
        }

        return filter;
    }

    private static CompositeRule getRule(Element filterElement) {
        String ruleStr = filterElement.getAttributeNS(null, SVG_MODE_ATTRIBUTE);
        CompositeRule rule;

        if (ruleStr.length() == 0) {
            rule = CompositeRule.OVER; // default value

        } else if (SVG_NORMAL_VALUE.equals(ruleStr)) {
            rule = CompositeRule.OVER;  // Same rule

        } else if (SVG_MULTIPLY_VALUE.equals(ruleStr)) {
            rule = CompositeRule.MULTIPLY;

        }  else if (SVG_SCREEN_VALUE.equals(ruleStr)) {
            rule = CompositeRule.SCREEN;

        } else if (SVG_DARKEN_VALUE.equals(ruleStr)) {
            rule = CompositeRule.DARKEN;

        } else if (SVG_LIGHTEN_VALUE.equals(ruleStr)) {
            rule = CompositeRule.LIGHTEN;

        } else {
            throw new IllegalAttributeValueException(
                Messages.formatMessage("feBlen.mode.invalid",
                                       new Object[] { ruleStr }));
        }
        return rule;
    }

    /**
     * Update the <tt>Filter</tt> object to reflect the current
     * configuration in the <tt>Element</tt> that models the filter.
     */
    public void update(BridgeMutationEvent evt) {
        // <!> FIXME : TODO
    }

}
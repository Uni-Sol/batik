/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.refimpl.bridge;

import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Vector;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.BridgeMutationEvent;
import org.apache.batik.bridge.FilterBridge;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.filter.CompositeRable;
import org.apache.batik.gvt.filter.CompositeRule;
import org.apache.batik.gvt.filter.Filter;
import org.apache.batik.gvt.filter.FilterRegion;
import org.apache.batik.gvt.filter.PadMode;
import org.apache.batik.refimpl.gvt.filter.ConcreteCompositeRable;
import org.apache.batik.refimpl.gvt.filter.ConcretePadRable;
import org.apache.batik.refimpl.gvt.filter.FilterSourceRegion;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.SVGUtilities;
import org.apache.batik.util.UnitProcessor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSStyleDeclaration;

/**
 * This class bridges an SVG <tt>feMerge</tt> element with
 * a concrete <tt>Filter</tt> filter implementation
 *
 * @author <a href="mailto:Thomas.DeWeeese@Kodak.com">Thomas DeWeese</a>
 * @version $Id$
 */
public class SVGFeMergeElementBridge implements FilterBridge,
                                                SVGConstants{

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
                         FilterRegion filterRegion,
                         Map filterMap){

        // Extract sources, they are defined in the filterElement's
        // children.
        NodeList children = filterElement.getChildNodes();
        int nChildren = children.getLength();
        Filter [] srcs = new Filter[nChildren];

        int count = 0;
        for (int i=0; i<nChildren; i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element elt = (Element)child;
            if (!elt.getNodeName().equals(TAG_FE_MERGE_NODE))
                continue;

            String inAttr = elt.getAttributeNS(null, ATTR_IN);
            Filter tmp;
            tmp = CSSUtilities.getFilterSource(filteredNode, inAttr, 
                                               bridgeContext, 
                                               elt, in, filterMap);
            if (tmp == null) continue;
            srcs[count++] = in = tmp;
        }

        if (count == 0)
            return null;

        if (count != nChildren) {
            Filter [] tmp = new Filter[count];
            System.arraycopy(srcs, 0, tmp, 0, count);
            srcs=tmp;
        }

        FilterRegion defaultRegion = new FilterSourceRegion(srcs);
        
          // Get unit. Comes from parent node.
        Node parentNode = filterElement.getParentNode();
        String units = VALUE_USER_SPACE_ON_USE;
        if((parentNode != null)
           && (parentNode.getNodeType() == parentNode.ELEMENT_NODE)) {
            units = ((Element)parentNode).
                getAttributeNS(null, ATTR_PRIMITIVE_UNITS);
        }
        
          //
          // Now, extraact filter region
          //
        CSSStyleDeclaration cssDecl
            = bridgeContext.getViewCSS().getComputedStyle(filterElement,
                                                          null);
        
        UnitProcessor.Context uctx
            = new DefaultUnitProcessorContext(bridgeContext,
                                              cssDecl);
        
        final FilterRegion compositeArea
            = SVGUtilities.convertFilterPrimitiveRegion(filterElement,
                                                        filteredElement,
                                                        defaultRegion,
                                                        units,
                                                        filteredNode,
                                                        uctx);
        
          // Now, do the Merge.
        Vector srcsVec = new Vector(count);
        for (int i=0; i<count; i++)
            srcsVec.add(srcs[i]);

        Filter filter = null;
        filter = new ConcreteCompositeRable(srcsVec, CompositeRule.OVER);
        
        filter = new ConcretePadRable(filter,
                                      compositeArea.getRegion(),
                                      PadMode.ZERO_PAD) {
                public Rectangle2D getBounds2D(){
                    setPadRect(compositeArea.getRegion());
                    return super.getBounds2D();
                }
                
                public java.awt.image.RenderedImage createRendering
                    (java.awt.image.renderable.RenderContext rc){
                    setPadRect(compositeArea.getRegion());
                    return super.createRendering(rc);
                }
            };
        
        
          // Get result attribute and update map
        String result = filterElement.getAttributeNS(null, ATTR_RESULT);
        if((result != null) && (result.trim().length() > 0)){
              // The filter will be added to the filter map. Before
              // we do that, append the filter region crop
            filterMap.put(result, filter);
        }

        return filter;
    }

    /**
     * Update the <tt>Filter</tt> object to reflect the current
     * configuration in the <tt>Element</tt> that models the filter.
     */
    public void update(BridgeMutationEvent evt) {
        // <!> FIXME : TODO
    }

}
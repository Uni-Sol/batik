/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.refimpl.gvt.filter;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GraphicsNodeRenderContext;
import org.apache.batik.gvt.filter.Filter;
import org.apache.batik.gvt.filter.GraphicsNodeRable;
import org.apache.batik.gvt.filter.GraphicsNodeRableFactory;
import org.apache.batik.gvt.filter.CompositeRule;

/**
 * This implementation of RenderableImage will render its input
 * GraphicsNode into a BufferedImage upon invokation of one of its
 * createRendering methods.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class ConcreteBackgroundRable 
    extends    AbstractRable {

    /**
     * GraphicsNode this image can render
     */
    private GraphicsNode node;

    /**
     * Returns the <tt>GraphicsNode</tt> rendered by this image
     */
    public GraphicsNode getGraphicsNode(){
        return node;
    }

    /**
     * Sets the <tt>GraphicsNode</tt> this image should render
     */
    public void setGraphicsNode(GraphicsNode node){
        if(node == null){
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * @param node The GraphicsNode this image should represent
     */
    public ConcreteBackgroundRable(GraphicsNode node){
        if(node == null)
            throw new IllegalArgumentException();

        this.node = node;
    }


    // This is a utilitiy method that unions the bounds of
    // cgn upto child (if child is null it does all children).
    // It unions them with init if provided.
    static Rectangle2D addBounds(CompositeGraphicsNode cgn,
                                 GraphicsNode child,
                                 Rectangle2D  init) {
        List children = cgn.getChildren();
        Iterator i = children.iterator();
        Rectangle2D r2d = null;
        while (i.hasNext()) {
            GraphicsNode gn = (GraphicsNode)i.next();
            if (gn == child) 
                break;

            Rectangle2D cr2d = gn.getBounds();
            AffineTransform at = gn.getTransform();
            cr2d = at.createTransformedShape(cr2d).getBounds2D();
            if (r2d == null) r2d = cr2d;
            r2d.add(cr2d);
        }

        if (r2d == null) {
            if (init == null)
                return CompositeGraphicsNode.VIEWPORT;
            else
                return init;
        } else if (init ==null)
            return r2d;
        
        init.add(r2d);
        return init;
    }


    static Rectangle2D getViewportBounds(GraphicsNode gn,
                                         GraphicsNode child) {
        // See if background is enabled.
        Rectangle2D r2d = null;
        if (gn instanceof CompositeGraphicsNode) {
            CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
            r2d = cgn.getBackgroundEnable();
        }

        if (r2d == null)
            // No background enable so check our parent's value.
            r2d = getViewportBounds(gn.getParent(), gn);


        // No background for any ancester (error) return null
        if (r2d == null)
            return null;

        // Background enabled is set, but it has no fixed bounds set.
        if (r2d == CompositeGraphicsNode.VIEWPORT) {
            // If we don't have a child then just use our bounds.
            if (child == null)
                return gn.getPrimitiveBounds();

            // gn must be composite so add all it's children's bounds
            // up to child.
            CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
            return addBounds(cgn, child, null);
        }

        try {
            // We have a partial bound from parent, so map it to gn's
            // coordinate system...
            AffineTransform at = gn.getTransform();
            at = at.createInverse();
            r2d = at.createTransformedShape(r2d).getBounds2D();
        } catch (NoninvertibleTransformException nte) {
            // Degenerate case return null;
            r2d = null;
        }
        
        if (child != null) {
            // Add our childrens bounds to it...
            CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
            r2d = addBounds(cgn, child, r2d);
        } else
            r2d.add(gn.getPrimitiveBounds());

        return r2d;
    }

    // This does the leg work for getBounds().
    // It traverses the tree figuring out the bounds of the
    // background image.
    static Rectangle2D getBoundsRecursive(GraphicsNode gn, 
                                          GraphicsNode child) {
        
        Rectangle2D r2d = null;
        if (gn == null)
            return null;

        if (gn instanceof CompositeGraphicsNode) {
            CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
            // See if background is enabled.
            r2d = cgn.getBackgroundEnable();
        }

        // background has definite bounds so return them.
        if (r2d != null) 
            return  r2d;

        // No background enable so check our parent's value.
        r2d = getBoundsRecursive(gn.getParent(), gn);

        // No background for any ancester (error) return null
        if (r2d == null)
            return null;

        // Our parent has background but no bounds (and we must
        // have been the first child so build the new bounds...
        if (r2d == CompositeGraphicsNode.VIEWPORT) 
            return r2d;

        try {
            // background has a definite bound so map it to gn's
            // coordinate system...
            AffineTransform at = gn.getTransform();
            at = at.createInverse();
            r2d = at.createTransformedShape(r2d).getBounds2D();
        } catch (NoninvertibleTransformException nte) {
            // Degenerate case return null;
            r2d = null;
        }
            
        return r2d;
    }

    /**
     * Returns the bounds of this Rable in the user coordinate system.
     */
    public Rectangle2D getBounds2D() {
        Rectangle2D r2d = getBoundsRecursive(node, null);

        if (r2d == CompositeGraphicsNode.VIEWPORT)
            r2d = getViewportBounds(node, null);

        return r2d;
    }

    /**
     * Returns a filter that represents the background image
     * for <tt>child</tt>.  
     */
    public Filter getBackground(GraphicsNode gn, 
                                GraphicsNode child,
                                GraphicsNodeRenderContext rc) {
        Rectangle2D r2d = null;
        if (gn instanceof CompositeGraphicsNode) {
            CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
            r2d = cgn.getBackgroundEnable();
        }

        Vector srcs = new Vector();
        if (r2d == null) {
            Filter f = getBackground(gn.getParent(), gn, rc);
            if (f == null)
                return null;
            srcs.add(f);
        }

        if (gn instanceof CompositeGraphicsNode) {
            CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
            List children = cgn.getChildren();
            Iterator i = children.iterator();
            while (i.hasNext()) {
                GraphicsNode childGN = (GraphicsNode)i.next();
                if (childGN == child)
                    break;
                GraphicsNodeRable gnr;
                GraphicsNodeRableFactory gnrf;
                gnrf = rc.getGraphicsNodeRableFactory();
                gnr  = gnrf.createGraphicsNodeRable(childGN);
                gnr.setUsePrimitivePaint(false);
                srcs.add(gnr);
            }
        }
        Filter ret = null;
        if (srcs.size() > 1)
            ret = new ConcreteCompositeRable(srcs, CompositeRule.OVER);
        if (srcs.size() > 0)
            ret = (Filter)srcs.get(0);

        return ret;
    }

    /**
     * Returns true if successive renderings (that is, calls to
     * createRendering() or createScaledRendering()) with the same arguments
     * may produce different results.  This method may be used to
     * determine whether an existing rendering may be cached and
     * reused.  It is always safe to return true.
     */
    public boolean isDynamic(){
        return false;
    }

    /**
     * Creates a RenderedImage that represented a rendering of this image
     * using a given RenderContext.  This is the most general way to obtain a
     * rendering of a RenderableImage.
     *
     * <p> The created RenderedImage may have a property identified
     * by the String HINTS_OBSERVED to indicate which RenderingHints
     * (from the RenderContext) were used to create the image.
     * In addition any RenderedImages
     * that are obtained via the getSources() method on the created
     * RenderedImage may have such a property.
     *
     * @param renderContext the RenderContext to use to produce the rendering.
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createRendering(RenderContext renderContext){

        GraphicsNodeRenderContext gnrc;
        gnrc = GraphicsNodeRenderContext.
            getGraphicsNodeRenderContext(renderContext);

        Filter f = getBackground(node, null, gnrc);
        
        // f = new ConcreteClipRable(f, getBounds2D());
        
        RenderedImage ri = f.createRendering(renderContext);
        return ri;
    }
}
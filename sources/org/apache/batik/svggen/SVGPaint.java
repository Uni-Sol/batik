/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.svggen;

import java.awt.Paint;
import java.awt.GradientPaint;
import java.awt.Color;
import java.awt.TexturePaint;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;

import java.util.Map;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class that converts a Paint object into an
 * SVG pattern element
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 * @see              org.apache.batik.svggen.SVGLinearGradient
 * @see              org.apache.batik.svggen.SVGTexturePaint
 */
public class SVGPaint implements SVGConverter{
    /**
     * All GradientPaint convertions are handed to svgLinearGradient
     */
    private SVGLinearGradient svgLinearGradient;

    /**
     * All TexturePaint convertions are handed to svgTextureGradient
     */
    private SVGTexturePaint svgTexturePaint;

    /**
     * All Color convertions are handed to svgColor
     */
    private SVGColor svgColor;

    /**
     * All custom Paint convetions are handed to svgCustomPaint
     */
    private SVGCustomPaint svgCustomPaint;

    /**
     * Used to generate DOM elements
     */
    private Document domFactory;

    /**
     * @param domFactory used to build Elements
     * @param imageHandler used to populate image Elements (for TexturePaints)
     * @param extensionHandler used to handle custom Paints
     */
    public SVGPaint(Document domFactory,
                    ImageHandler imageHandler,
                    ExtensionHandler extensionHandler){

        this.svgLinearGradient = new SVGLinearGradient(domFactory);
        this.svgTexturePaint = new SVGTexturePaint(domFactory, imageHandler);
        this.svgCustomPaint = new SVGCustomPaint(domFactory, extensionHandler);
        this.svgColor = new SVGColor();
        this.domFactory = domFactory;
    }

    /**
     * @param new extension handler this object should use
     */
    void setExtensionHandler(ExtensionHandler extensionHandler){
        this.svgCustomPaint = new SVGCustomPaint(domFactory, extensionHandler);
    }

    /**
     * @return Set of Elements defining the Paints this
     *         converter has processed since it was created
     */
    public Set getDefinitionSet(){
        Set paintDefs = new HashSet(svgLinearGradient.getDefinitionSet());
        paintDefs.addAll(svgTexturePaint.getDefinitionSet());
        paintDefs.addAll(svgCustomPaint.getDefinitionSet());
        paintDefs.addAll(svgColor.getDefinitionSet());
        return paintDefs;
    }

    public SVGTexturePaint getTexturePaintConverter(){
        return svgTexturePaint;
    }

    public SVGLinearGradient getGradientPaintConverter(){
        return svgLinearGradient;
    }

    public SVGCustomPaint getCustomPaintConverter(){
        return svgCustomPaint;
    }

    public SVGColor getColorConverter(){
        return svgColor;
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
     * @param paint Paint to be converted to SVG
     * @return a descriptor of the corresponding SVG paint
     */
    public SVGPaintDescriptor toSVG(Paint paint){
        SVGPaintDescriptor paintDesc = null;

        if(paint instanceof Color)
            paintDesc = svgColor.toSVG((Color)paint);
        else if(paint instanceof GradientPaint)
            paintDesc = svgLinearGradient.toSVG((GradientPaint)paint);
        else if(paint instanceof TexturePaint)
            paintDesc = svgTexturePaint.toSVG((TexturePaint)paint);
        else
            paintDesc = svgCustomPaint.toSVG(paint);

        return paintDesc;
    }
}
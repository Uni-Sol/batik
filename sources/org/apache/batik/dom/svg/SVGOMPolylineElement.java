/*

   Copyright 2000-2001,2003,2006  The Apache Software Foundation 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.dom.svg;

import org.apache.batik.dom.AbstractDocument;
import org.apache.batik.anim.values.AnimatableValue;
import org.apache.batik.util.SVGTypes;

import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGPointList;
import org.w3c.dom.svg.SVGPolylineElement;

/**
 * This class implements {@link SVGPolylineElement}.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class SVGOMPolylineElement
    extends    SVGGraphicsElement
    implements SVGPolylineElement {

    /**
     * Creates a new SVGOMPolylineElement object.
     */
    protected SVGOMPolylineElement() {
    }

    /**
     * Creates a new SVGOMPolylineElement object.
     * @param prefix The namespace prefix.
     * @param owner The owner document.
     */
    public SVGOMPolylineElement(String prefix, AbstractDocument owner) {
        super(prefix, owner);
    }

    /**
     * <b>DOM</b>: Implements {@link Node#getLocalName()}.
     */
    public String getLocalName() {
        return SVG_POLYLINE_TAG;
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.svg.SVGAnimatedPoints#getPoints()}.
     */
    public SVGPointList getPoints() {
        return SVGAnimatedPointsSupport.getPoints(this);
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.svg.SVGAnimatedPoints#getAnimatedPoints()}.
     */
    public SVGPointList getAnimatedPoints() {
        return SVGAnimatedPointsSupport.getAnimatedPoints(this);
    }

    /**
     * Returns a new uninitialized instance of this object's class.
     */
    protected Node newNode() {
        return new SVGOMPolylineElement();
    }

    // ExtendedTraitAccess ///////////////////////////////////////////////////

    /**
     * Returns whether the given XML attribute is animatable.
     */
    public boolean isAttributeAnimatable(String ns, String ln) {
        if (ns == null) {
            if (ln.equals(SVG_POINTS_ATTRIBUTE)) {
                return true;
            }
        }
        return super.isAttributeAnimatable(ns, ln);
    }

    /**
     * Returns the type of the given attribute.
     */
    public int getAttributeType(String ns, String ln) {
        if (ns == null) {
            if (ln.equals(SVG_POINTS_ATTRIBUTE)) {
                return SVGTypes.TYPE_POINTS_VALUE;
            }
        }
        return super.getAttributeType(ns, ln);
    }

    // AnimationTarget ///////////////////////////////////////////////////////

    /**
     * Updates an attribute value in this target.
     */
    public void updateAttributeValue(String ns, String ln,
                                     AnimatableValue val) {
        if (ns == null) {
            if (ln.equals(SVG_POINTS_ATTRIBUTE)) {
                SVGOMAnimatedPoints p =
                    SVGAnimatedPointsSupport.getSVGOMAnimatedPoints(this);
                updatePointsAttributeValue(p, val);
                return;
            }
        }
        super.updateAttributeValue(ns, ln, val);
    }

    /**
     * Returns the underlying value of an animatable XML attribute.
     */
    public AnimatableValue getUnderlyingValue(String ns, String ln) {
        if (ns == null) {
            if (ln.equals(SVG_POINTS_ATTRIBUTE)) {
                SVGOMAnimatedPoints p =
                    SVGAnimatedPointsSupport.getSVGOMAnimatedPoints(this);
                return getBaseValue(p);
            }
        }
        return super.getUnderlyingValue(ns, ln);
    }
}

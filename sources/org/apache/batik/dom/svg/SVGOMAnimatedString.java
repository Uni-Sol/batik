/*

   Copyright 2000-2003  The Apache Software Foundation 

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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.svg.SVGAnimatedString;

/**
 * This class implements the {@link SVGAnimatedString} interface.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class SVGOMAnimatedString extends AbstractSVGAnimatedValue
                                 implements SVGAnimatedString {

    /**
     * The current animated value.
     */
    protected String animVal;

    /**
     * Creates a new SVGOMAnimatedString.
     * @param elt The associated element.
     * @param ns The attribute's namespace URI.
     * @param ln The attribute's local name.
     */
    public SVGOMAnimatedString(AbstractElement elt,
                               String ns,
                               String ln) {
        super(elt, ns, ln);
    }

    /**
     * <b>DOM</b>: Implements {@link SVGAnimatedString#getBaseVal()}.
     */
    public String getBaseVal() {
        return element.getAttributeNS(namespaceURI, localName);
    }

    /**
     * <b>DOM</b>: Implements {@link SVGAnimatedString#setBaseVal(String)}.
     */
    public void setBaseVal(String baseVal) throws DOMException {
        element.setAttributeNS(namespaceURI, localName, baseVal);
    }

    /**
     * <b>DOM</b>: Implements {@link SVGAnimatedString#getAnimVal()}.
     */
    public String getAnimVal() {
        if (hasAnimVal) {
            return animVal;
        }
        return element.getAttributeNS(namespaceURI, localName);
    }

    /**
     * Sets the animated value.
     */
    public void setAnimatedValue(String s) {
        hasAnimVal = true;
        animVal = s;
        fireAnimatedAttributeListeners();
    }

    /**
     * Removes the animated value.
     */
    public void resetAnimatedValue() {
        hasAnimVal = false;
        fireAnimatedAttributeListeners();
    }

    /**
     * Called when an Attr node has been added.
     */
    public void attrAdded(Attr node, String newv) {
        fireBaseAttributeListeners();
        if (!hasAnimVal) {
            fireAnimatedAttributeListeners();
        }
    }

    /**
     * Called when an Attr node has been modified.
     */
    public void attrModified(Attr node, String oldv, String newv) {
        fireBaseAttributeListeners();
        if (!hasAnimVal) {
            fireAnimatedAttributeListeners();
        }
    }

    /**
     * Called when an Attr node has been removed.
     */
    public void attrRemoved(Attr node, String oldv) {
        fireBaseAttributeListeners();
        if (!hasAnimVal) {
            fireAnimatedAttributeListeners();
        }
    }
}

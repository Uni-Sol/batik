/*

   Copyright 2003  The Apache Software Foundation 

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

import org.w3c.dom.svg.SVGNumber;

/**
 * Implementation of {@link SVGNumber}.
 *
 * @author <a href="mailto:tonny@kiyut.com">Tonny Kohar</a>
 * @version $Id$
 */
public abstract class AbstractSVGNumber implements SVGNumber {

    /**
     * The number.
     */
    protected float value;
    
    /**
     * <b>DOM</b>: Implements {@link SVGNumber#getValue()}.
     */
    public float getValue() {
        return value;
    }

    /**
     * <b>DOM</b>: Implements {@link SVGNumber#setValue(float)}.
     */
    public void setValue(float f) {
        value = f;
    }
}

/*

   Copyright 2005  The Apache Software Foundation 

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
package org.apache.batik.dom.events;

/**
 * A custom event object.
 *
 * @author <a href="mailto:cam%40mcc%2eid%2eau">Cameron McCormack</a>
 * @version $Id$
 */
public class DOMCustomEvent extends DOMEvent {

    /**
     * The custom details associated with this event.
     */
    protected Object details;

    /**
     * Returns the custom details of this event.
     */
    public Object getDetails() {
        return details;
    }

    /**
     * Initializes this custom event.
     */
    public void initCustomEventNS(String namespaceURIArg,
                                  String typeArg,
                                  boolean canBubbleArg,
                                  boolean cancelableArg,
                                  Object detailsArg) {
        initEventNS(namespaceURIArg, typeArg, canBubbleArg, cancelableArg);
        details = detailsArg;
    }
}

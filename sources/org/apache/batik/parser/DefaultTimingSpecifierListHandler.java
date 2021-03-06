/*

   Copyright 2006  The Apache Software Foundation 

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
package org.apache.batik.parser;

/**
 * An adapter class for {@link TimingSpecifierListHandler}.
 *
 * @author <a href="mailto:cam%40mcc%2eid%2eau">Cameron McCormack</a>
 * @version $Id$
 */
public class DefaultTimingSpecifierListHandler
        extends DefaultTimingSpecifierHandler
        implements TimingSpecifierListHandler {

    /**
     * The default handler.
     */
    public final static TimingSpecifierListHandler INSTANCE
        = new DefaultTimingSpecifierListHandler();

    protected DefaultTimingSpecifierListHandler() {
    }

    /**
     * Invoked when parsing of a timing specifier list commences.
     */
    public void startTimingSpecifierList() {
    }

    /**
     * Invoked when parsing of a timing specifier list ends.
     */
    public void endTimingSpecifierList() {
    }
}

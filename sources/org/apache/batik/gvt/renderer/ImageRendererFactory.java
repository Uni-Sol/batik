/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.gvt.renderer;

import org.apache.batik.gvt.GraphicsNodeRenderContext;

/**
 * Interface for a factory of ImageRenderers
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public interface ImageRendererFactory extends RendererFactory{
    /**
     * Creates a new renderer.
     */
    ImageRenderer createImageRenderer();
}
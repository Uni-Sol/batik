/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.css.svg;

import org.apache.batik.css.CSSOMReadOnlyStyleDeclaration;
import org.apache.batik.css.CSSOMReadOnlyValue;
import org.apache.batik.css.value.AbstractValueFactory;
import org.apache.batik.css.value.ImmutableValue;
import org.apache.batik.css.value.RelativeValueResolver;
import org.w3c.dom.Element;
import org.w3c.dom.css.ViewCSS;

/**
 * This class provides a relative value resolver for the
 * 'writing-mode' CSS property.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class WritingModeResolver implements RelativeValueResolver {
    /**
     * The auto CSS value.
     */
    public final static CSSOMReadOnlyValue LR_TB =
        new CSSOMReadOnlyValue(WritingModeFactory.LR_TB_VALUE);

    /**
     * Whether the handled property is inherited or not.
     */
    public boolean isInheritedProperty() {
	return true;
    }

    /**
     * Returns the name of the handled property.
     */
    public String getPropertyName() {
	return "writing-mode";
    }

    /**
     * Returns the default value for the handled property.
     */
    public CSSOMReadOnlyValue getDefaultValue() {
	return LR_TB;
    }
    
    /**
     * Resolves the given value if relative, and puts it in the given table.
     * @param element The element to which this value applies.
     * @param pseudoElement The pseudo element if one.
     * @param view The view CSS of the current document.
     * @param styleDeclaration The computed style declaration.
     * @param value The cascaded value.
     * @param priority The priority of the cascaded value.
     * @param origin The origin of the cascaded value.
     */
    public void resolveValue(Element element,
			     String pseudoElement,
			     ViewCSS view,
			     CSSOMReadOnlyStyleDeclaration styleDeclaration,
			     CSSOMReadOnlyValue value,
			     String priority,
			     int origin) {
        // Nothing to do.
    }
}
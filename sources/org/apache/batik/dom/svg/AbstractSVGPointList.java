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

import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PointsHandler;
import org.apache.batik.parser.PointsParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.svg.SVGException;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGPointList;

/**
 * Abstract implementation of {@link SVGPointList}.
 *
 * @author <a href="mailto:nicolas.socheleau@bitflash.com">Nicolas Socheleau</a>
 * @version $Id$
 */
public abstract class AbstractSVGPointList
    extends AbstractSVGList
    implements SVGPointList {

    /**
     * Separator for a point list.
     */
    public final static String SVG_POINT_LIST_SEPARATOR
        = " ";

    /**
     * Return the separator between points in the list.
     */
    protected String getItemSeparator() {
        return SVG_POINT_LIST_SEPARATOR;
    }

    /**
     * Create an SVGException when the checkItemType fails.
     * @return SVGException
     */
    protected abstract SVGException createSVGException(short type,
                                                       String key,
                                                       Object[] args);

    /**
     * Creates a new SVGPointList.
     */
    protected AbstractSVGPointList() {
    }

    /**
     * <b>DOM</b>: Implements {@link SVGPointList#initialize(int)}.
     */
    public SVGPoint initialize(SVGPoint newItem)
            throws DOMException, SVGException {
        return (SVGPoint) initializeImpl(newItem);
    }

    /**
     * <b>DOM</b>: Implements {@link SVGPointList#getItem(int)}.
     */
    public SVGPoint getItem(int index) throws DOMException {
        return (SVGPoint) getItemImpl(index);
    }

    /**
     * <b>DOM</b>: Implements {@link
     * SVGPointList#insertItemBefore(SVGPoint,int)}.
     */
    public SVGPoint insertItemBefore(SVGPoint newItem, int index)
            throws DOMException, SVGException {
        return (SVGPoint) insertItemBeforeImpl(newItem,index);
    }

    /**
     * <b>DOM</b>: Implements {@link
     * SVGPointList#replaceItem(SVGPoint,int)}.
     */
    public SVGPoint replaceItem(SVGPoint newItem, int index)
            throws DOMException, SVGException {
        return (SVGPoint) replaceItemImpl(newItem,index);
    }

    /**
     * <b>DOM</b>: Implements {@link SVGPointList#removeItem(int)}.
     */
    public SVGPoint removeItem(int index) throws DOMException {
        return (SVGPoint) removeItemImpl(index);
    }

    /**
     * <b>DOM</b>: Implements {@link SVGPointList#appendItem(SVGPoint)}.
     */
    public SVGPoint appendItem(SVGPoint newItem)
            throws DOMException, SVGException {
        return (SVGPoint) appendItemImpl(newItem);
    }

    /**
     * Creates a new {@link SVGItem} object from the given {@link SVGpoint}.
     */
    protected SVGItem createSVGItem(Object newItem) {
        SVGPoint point = (SVGPoint) newItem;
        return new SVGPointItem(point.getX(), point.getY());
    }
    
    /**
     * Parse the 'points' attribute.
     *
     * @param value 'points' attribute value
     * @param handler list handler
     */
    protected void doParse(String value, ListHandler handler)
            throws ParseException {
        PointsParser pointsParser = new PointsParser();
        PointsListBuilder builder = new PointsListBuilder(handler);
        pointsParser.setPointsHandler(builder);
        pointsParser.parse(value);
    }

    /**
     * Asserts that the given item is an {@link SVGPoint}.
     */
    protected void checkItemType(Object newItem) throws SVGException {
        if (!(newItem instanceof SVGPoint)) {
            createSVGException(SVGException.SVG_WRONG_TYPE_ERR,
                               "expected SVGPoint",
                               null);
        }
    }

    /**
     * Representation of the item SVGPoint.
     */
    protected class SVGPointItem extends AbstractSVGItem implements SVGPoint {

        /**
         * The x value.
         */
        protected float x;

        /**
         * The y value.
         */
        protected float y;

        /**
         * Creates a new SVGPointItem.
         */
        public SVGPointItem(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Return a String representation of this SVGPoint.
         */
        protected String getStringValue() {
            StringBuffer value = new StringBuffer();
            value.append(x);
            value.append(',');
            value.append(y);
            return value.toString();
        }

        /**
         * <b>DOM</b>: Implements {@link SVGPoint#getX()}.
         */
        public float getX() {
            return x;
        }

        /**
         * <b>DOM</b>: Implements {@link SVGPoint#getY()}.
         */
        public float getY() {
            return y;
        }

        /**
         * <b>DOM</b>: Implements {@link SVGPoint#setX(float)}.
         */
        public void setX(float x) {
            this.x = x;
            resetAttribute();
        }

        /**
         * <b>DOM</b>: Implements {@link SVGPoint#setY(float)}.
         */
        public void setY(float y) {
            this.y = y;
            resetAttribute();
        }

        /**
         * <b>DOM</b>: Implements {@link SVGPoint#matrixTransform(SVGMatrix)}.
         */
        public SVGPoint matrixTransform(SVGMatrix matrix) {
            return SVGOMPoint.matrixTransform(this, matrix);
        }
    }

    /**
     * Helper class to interface the {@link PointsParser} and the
     * {@link ListHandler}.
     */
    protected class PointsListBuilder implements PointsHandler {

        /**
         * The ListHandler pass newly created {@link SVGPointItem} objects to.
         */
        protected ListHandler listHandler;
        
        /**
         * Creates a new PointsListBuilder.
         */
        public PointsListBuilder(ListHandler listHandler) {
            this.listHandler = listHandler;
        }

        public void startPoints() throws ParseException {
            listHandler.startList();
        }

        /**
         * Creates a new {@link SVGPointItem} and passes it to the list handler.
         */
        public void point(float x, float y) throws ParseException {
            listHandler.item(new SVGPointItem(x, y));
        }

        public void endPoints() throws ParseException {
            listHandler.endList();
        }
    }
}

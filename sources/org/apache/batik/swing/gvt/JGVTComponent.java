/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.swing.gvt;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import java.awt.image.BufferedImage;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import org.apache.batik.gvt.GraphicsNode;

import org.apache.batik.gvt.event.AWTEventDispatcher;

import org.apache.batik.gvt.renderer.DynamicRendererFactory;
import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.gvt.renderer.ImageRendererFactory;

/**
 * This class represents a component which can display a GVT tree.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class JGVTComponent extends JComponent {
    
    /**
     * The listener.
     */
    protected Listener listener;

    /**
     * The GVT tree renderer.
     */
    protected GVTTreeRenderer gvtTreeRenderer;

    /**
     * The GVT tree root.
     */
    protected GraphicsNode gvtRoot;

    /**
     * The renderer factory.
     */
    protected ImageRendererFactory rendererFactory = new DynamicRendererFactory();

    /**
     * The current renderer.
     */
    protected ImageRenderer renderer;

    /**
     * The GVT tree renderer listeners.
     */
    protected List gvtTreeRendererListeners =
        Collections.synchronizedList(new LinkedList());

    /**
     * Whether a render was requested.
     */
    protected boolean needRender;

    /**
     * Whether to allow progressive paint.
     */
    protected boolean progressivePaint;

    /**
     * The progressive paint thread.
     */
    protected Thread progressivePaintThread;

    /**
     * The image to paint.
     */
    protected BufferedImage image;

    /**
     * The initial rendering transform.
     */
    protected AffineTransform initialTransform;

    /**
     * The transform used for rendering.
     */
    protected AffineTransform renderingTransform;

    /**
     * The transform used for painting.
     */
    protected AffineTransform paintingTransform;

    /**
     * The interactor list.
     */
    protected List interactors = new LinkedList();

    /**
     * The current interactor.
     */
    protected Interactor interactor;

    /**
     * The overlays.
     */
    protected List overlays = new LinkedList();

    /**
     * The event dispatcher.
     */
    protected AWTEventDispatcher eventDispatcher;

    /**
     * The text selection manager.
     */
    protected TextSelectionManager textSelectionManager;

    /**
     * Whether the GVT tree should be reactive to mouse and key events.
     */
    protected boolean eventsEnabled;

    /**
     * Whether the text should be selectable if eventEnabled is false,
     * this flag is ignored.
     */
    protected boolean selectableText;

    /**
     * Creates a new JGVTComponent.
     */
    public JGVTComponent() {
        this(false, false);
    }

    /**
     * Creates a new JGVTComponent.
     * @param eventEnabled Whether the GVT tree should be reactive
     *        to mouse and key events.
     * @param selectableText Whether the text should be selectable.
     *        if eventEnabled is false, this flag is ignored.
     */
    public JGVTComponent(boolean eventsEnabled, boolean selectableText) {
        setDoubleBuffered(false);
        setBackground(Color.white);

        this.eventsEnabled = eventsEnabled;
        this.selectableText = selectableText;

        listener = createListener();

        addKeyListener(listener);
        addMouseListener(listener);
        addMouseMotionListener(listener);

        addGVTTreeRendererListener(listener);

        addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    updateRenderingTransform();
                    if (gvtTreeRenderer != null) {
                        needRender = true;
                        gvtTreeRenderer.interrupt();
                    } else {
                        image = null;
                        renderGVTTree();
                    }
                }
            });

    }

    /**
     * Returns the interactor list.
     */
    public List getInteractors() {
        return interactors;
    }

    /**
     * Returns the overlay list.
     */
    public List getOverlays() {
        return overlays;
    }

    /**
     * Returns the off-screen image, if any.
     */
    public BufferedImage getOffScreen() {
        return image;
    }

    /**
     * Stops the processing of the current tree.
     */
    public void stopProcessing() {
        if (gvtTreeRenderer != null) {
            gvtTreeRenderer.interrupt();
            if (progressivePaintThread != null) {
                progressivePaintThread.interrupt();
                progressivePaintThread = null;
            }
        }
    }

    /**
     * Sets the GVT tree to display.
     */
    public void setGraphicsNode(GraphicsNode gn) {
        setGraphicsNode(gn, true);
    }

    /**
     * Sets the GVT tree to display.
     */
    protected void setGraphicsNode(GraphicsNode gn, boolean createDispatcher) {
        gvtRoot = gn;
        if (gn != null && createDispatcher) {
            initializeEventHandling();
        }
        if (eventDispatcher != null) {
            eventDispatcher.setRootNode(gn);
        }
        computeRenderingTransform();
    }

    /**
     * Initializes the event handling classes.
     */
    protected void initializeEventHandling() {
        if (eventsEnabled) {
            eventDispatcher =
                new AWTEventDispatcher(rendererFactory.getRenderContext());
            if (selectableText) {
                textSelectionManager =
                    new TextSelectionManager(this, rendererFactory.getRenderContext(),
                                             eventDispatcher);
            }
        }
    }

    /**
     * Whether to enable the progressive paint.
     */
    public void setProgressivePaint(boolean b) {
        if (progressivePaint != b) {
            progressivePaint = b;
            if (progressivePaintThread != null) {
                progressivePaintThread.interrupt();
                progressivePaintThread = null;
            }
        }
    }

    /**
     * Paints this component.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D)g;

        Dimension d = getSize();
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setPaint(getBackground());
        g2d.fillRect(0, 0, d.width, d.height);

        if (image != null) {
            if (paintingTransform != null) {
                g2d.transform(paintingTransform);
            }
            g2d.drawRenderedImage(image, null);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_OFF);
            Iterator it = overlays.iterator();
            while (it.hasNext()) {
                ((Overlay)it.next()).paint(g);
            }
        }
    }

    /**
     * Sets the painting transform. A null transform is the same as
     * an identity transform.
     * The next repaint will use the given transform.
     */
    public void setPaintingTransform(AffineTransform at) {
        paintingTransform = at;
        repaint();
    }

    /**
     * Returns the current painting transform.
     */
    public AffineTransform getPaintingTransform() {
        return paintingTransform;
    }

    /**
     * Sets the rendering transform.
     * Calling this method causes a rendering to be performed.
     */
    public void setRenderingTransform(AffineTransform at) {
        renderingTransform = at;
        if (eventDispatcher != null) {
            try {
                eventDispatcher.setBaseTransform(renderingTransform.createInverse());
            } catch (NoninvertibleTransformException e) {
                handleException(e);
            }
        }
        paintingTransform = null;
        if (gvtTreeRenderer != null) {
            needRender = true;
            gvtTreeRenderer.interrupt();
        } else {
            image = null;
            renderGVTTree();
        }
    }

    /**
     * Returns the current rendering transform.
     */
    public AffineTransform getRenderingTransform() {
        return renderingTransform;
    }

    /**
     * Sets whether this component should use double buffering to render
     * SVG documents.
     */
    public void setDoubleBufferedRendering(boolean b) {
        // !!! TODO
    }

    /**
     * Adds a GVTTreeRendererListener to this component.
     */
    public void addGVTTreeRendererListener(GVTTreeRendererListener l) {
        gvtTreeRendererListeners.add(l);
    }

    /**
     * Removes a GVTTreeRendererListener from this component.
     */
    public void removeGVTTreeRendererListener(GVTTreeRendererListener l) {
        gvtTreeRendererListeners.remove(l);
    }

    /**
     * Renders the GVT tree. Used for the initial rendering and resize only.
     */
    protected void renderGVTTree() {
        Dimension d = getSize();
        if (gvtRoot == null || d.width <= 0 || d.height <= 0) {
            return;
        }

        // Renderer setup.
        if (renderer == null) {
            renderer = rendererFactory.createImageRenderer();
            renderer.setTree(gvtRoot);
        }
        renderer.setTransform(renderingTransform);

        // Area of interest computation.
        AffineTransform inv;
        try {
            inv = renderingTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new InternalError(e.getMessage());
        }
        Shape s = inv.createTransformedShape(new Rectangle(0, 0, d.width, d.height));

        // Rendering thread setup.
        gvtTreeRenderer = new GVTTreeRenderer(renderer, s, d.width, d.height);
        gvtTreeRenderer.setPriority(Thread.MIN_PRIORITY);

        Iterator it = gvtTreeRendererListeners.iterator();
        while (it.hasNext()) {
            gvtTreeRenderer.addGVTTreeRendererListener
                ((GVTTreeRendererListener)it.next());
        }
        
        // Disable the dispatch during the rendering
        // to avoid concurrent access to the GVT tree.
        if (eventDispatcher != null) {
            eventDispatcher.setRootNode(null);
        }
        gvtTreeRenderer.start();
    }

    /**
     * Computes the initial value of the transform used for rendering.
     */
    protected void computeRenderingTransform() {
        initialTransform = new AffineTransform();
        setRenderingTransform(initialTransform);
    }

    /**
     * Updates the value of the transform used for rendering.
     */
    protected void updateRenderingTransform() {
        // Do nothing
    }

    /**
     * Handles an exception.
     */
    protected void handleException(Exception e) {
        // Do nothing.
    }

    /**
     * Releases the references to the rendering resources,
     */
    protected void releaseRenderingReferences() {
        eventDispatcher = null;
        if (textSelectionManager != null) {
            overlays.remove(textSelectionManager.getSelectionOverlay());
            textSelectionManager = null;
        }
        renderer = null;
        gvtRoot = null;
    }

    /**
     * Creates an instance of Listener.
     */
    protected Listener createListener() {
        return new Listener();
    }

    /**
     * To hide the listener methods.
     */
    protected class Listener
        implements GVTTreeRendererListener,
                   KeyListener,
                   MouseListener,
                   MouseMotionListener {

        /**
         * Creates a new Listener.
         */
        protected Listener() {
        }

        // GVTTreeRendererListener /////////////////////////////////////////////

        /**
         * Called when a rendering is in its preparing phase.
         */
        public void gvtRenderingPrepare(GVTTreeRendererEvent e) {
            // Do nothing
        }

        /**
         * Called when a rendering started.
         * The data of the event is initialized to the old document.
         */
        public void gvtRenderingStarted(GVTTreeRendererEvent e) {
            if (progressivePaint /* && !doubleBuffering */) {
                image = e.getImage();
                progressivePaintThread = new Thread() {
                    public void run() {
                        try {
                            while (!isInterrupted()) {
                                repaint();
                                sleep(100);
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                };
                progressivePaintThread.setPriority(Thread.MIN_PRIORITY + 1);
                progressivePaintThread.start();
            }
        }
        
        /**
         * Called when a rendering was completed.
         */
        public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
            if (progressivePaintThread != null) {
                progressivePaintThread.interrupt();
                progressivePaintThread = null;
            }
            gvtTreeRenderer = null;
            image = e.getImage();
            if (needRender) {
                image = null;
                renderGVTTree();
                needRender = false;
            }
            if (eventDispatcher != null) {
                eventDispatcher.setRootNode(gvtRoot);
            }
            repaint();
        }
        
        /**
         * Called when a rendering was cancelled.
         */
        public void gvtRenderingCancelled(GVTTreeRendererEvent e) {
            if (progressivePaintThread != null) {
                progressivePaintThread.interrupt();
                progressivePaintThread = null;
            }
            gvtTreeRenderer = null;
            image = null;
            if (needRender) {
                renderGVTTree();
                needRender = false;
            }
            repaint();
        }
        
        /**
         * Called when a rendering failed.
         */
        public void gvtRenderingFailed(GVTTreeRendererEvent e) {
            if (progressivePaintThread != null) {
                progressivePaintThread.interrupt();
                progressivePaintThread = null;
            }
            gvtTreeRenderer = null;
            image = null;
            if (needRender) {
                renderGVTTree();
                needRender = false;
            }
            repaint();
        }

        // KeyListener //////////////////////////////////////////////////////////

        /**
         * Invoked when a key has been typed.
         * This event occurs when a key press is followed by a key release.
         */
        public void keyTyped(KeyEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.keyTyped(e);
                deselectInteractor();
            }
        }
        
        /**
         * Invoked when a key has been pressed.
         */
        public void keyPressed(KeyEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.keyPressed(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.keyReleased(e);
            }
        }

        /**
         * Invoked when a key has been released.
         */
        public void keyReleased(KeyEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.keyReleased(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.keyReleased(e);
            }
        }

        // MouseListener ///////////////////////////////////////////////////////
        
        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void mouseClicked(MouseEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.mouseClicked(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.mouseClicked(e);
            }
        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         */
        public void mousePressed(MouseEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.mousePressed(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.mousePressed(e);
            }
        }

        /**
         * Invoked when a mouse button has been released on a component.
         */
        public void mouseReleased(MouseEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.mouseReleased(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.mouseReleased(e);
            }
        }

        /**
         * Invoked when the mouse enters a component.
         */
        public void mouseEntered(MouseEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.mouseEntered(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.mouseEntered(e);
            }
        }

        /**
         * Invoked when the mouse exits a component.
         */
        public void mouseExited(MouseEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.mouseExited(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.mouseExited(e);
            }
        }

        // MouseMotionListener /////////////////////////////////////////////////

        /**
         * Invoked when a mouse button is pressed on a component and then 
         * dragged.  Mouse drag events will continue to be delivered to
         * the component where the first originated until the mouse button is
         * released (regardless of whether the mouse position is within the
         * bounds of the component).
         */
        public void mouseDragged(MouseEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.mouseDragged(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.mouseDragged(e);
            }
        }

        /**
         * Invoked when the mouse button has been moved on a component
         * (with no buttons no down).
         */
        public void mouseMoved(MouseEvent e) {
            selectInteractor(e);
            if (interactor != null) {
                interactor.mouseMoved(e);
                deselectInteractor();
            } else if (eventDispatcher != null) {
                eventDispatcher.mouseMoved(e);
            }
        }

        /**
         * Selects an interactor, given an input event.
         */
        protected void selectInteractor(InputEvent ie) {
            if (interactor == null) {
                Iterator it = interactors.iterator();
                while (it.hasNext()) {
                    Interactor i = (Interactor)it.next();
                    if (i.startInteraction(ie)) {
                        interactor = i;
                        break;
                    }
                }
            }
        }

        /**
         * Deselects an interactor, if the interaction has finished.
         */
        protected void deselectInteractor() {
            if (interactor.endInteraction()) {
                interactor = null;
            }
        }
    }
}
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.util;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

/**
 * A simple non-editable tree browser with multiple columns 
 * for display and navigation of a tree structure. This type of
 * browser is ideal for showing deeply (or infinitely) nested 
 * trees/graphs. The most famous example of its use is Mac OS X 
 * Finder column view.
 * 
 * <p>
 * Uses the same TreeModel as JTree for its navigation model.
 * </p>
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class MultiColumnBrowser extends JPanel {
    private static Logger logObj = Logger.getLogger(MultiColumnBrowser.class);

    public static final int DEFAULT_MIN_COLUMNS_COUNT = 3;

    protected int minColumns;
    protected ListCellRenderer renderer;
    protected TreeModel model;
    protected TreePath selectionPath;
    protected Dimension preferredColumnSize;

    private int offset;
    private List columns;
    private ListSelectionListener browserSelector = new PanelController();

    public MultiColumnBrowser() {
        this(DEFAULT_MIN_COLUMNS_COUNT);
    }

    public MultiColumnBrowser(int minColumns) {
        this(minColumns, CellRenderers.listRendererWithIcons());
    }

    public MultiColumnBrowser(int minColumns, ListCellRenderer renderer) {
        if (minColumns < DEFAULT_MIN_COLUMNS_COUNT) {
            throw new IllegalArgumentException(
                "Expected "
                    + DEFAULT_MIN_COLUMNS_COUNT
                    + " or more columns, got: "
                    + minColumns);
        }

        this.minColumns = minColumns;
        this.renderer = renderer;

        initView();
    }

    private void initView() {
        columns = new ArrayList(minColumns);
        expandView(minColumns);
    }

    private void expandView(int addColumns) {
        if (addColumns == 0) {
            return;
        }

        if (addColumns < 0) {
            // TODO: implement "contractView" if the columns number has shrunk
            return;
        }

        setLayout(new GridLayout(1, columns.size() + addColumns, 3, 3));

        for (int i = 0; i < addColumns; i++) {
            appendColumn();
        }

        refreshPreferredSize();
        refreshView();
    }

    private void refreshView() {
        Container parent = getParent();
        if (parent != null) {

        }

        revalidate();
    }

    private void refreshPreferredSize() {
        if (preferredColumnSize != null) {
            int w = getColumnsCount() * (preferredColumnSize.width + 3) + 3;
            int h = preferredColumnSize.height + 6;
            setPreferredSize(new Dimension(w, h));
        }
    }

    private void scrollToColumn(int column) {
        if (getParent() instanceof JViewport) {

            JViewport viewport = (JViewport) getParent();

            // find a rectangle in the middle of the browser
            // and scroll it...
            double x = getWidth() * column / ((double) getMinColumns());
            double y = getHeight() / 2;

            if (preferredColumnSize != null) {
                x -= preferredColumnSize.width / 2;
                if (x < 0) {
                    x = 0;
                }
            }

            Rectangle rectangle = new Rectangle((int) x, (int) y, 1, 1);

            // Scroll the area into view.
            viewport.scrollRectToVisible(rectangle);
        }
    }

    private BrowserPanel appendColumn() {
        BrowserPanel panel = new BrowserPanel();
        panel.addListSelectionListener(browserSelector);

        if (renderer != null) {
            panel.setCellRenderer(renderer);
        }

        columns.add(panel);
        JScrollPane scroller =
            new JScrollPane(
                panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // note - it is important to set prefrred size on scroller,
        // not on the component itself... otherwise resizing
        // will be very ugly...
        if (preferredColumnSize != null) {
            scroller.setPreferredSize(preferredColumnSize);
        }
        add(scroller);
        return panel;
    }

    // builds initial view
    private void initFromModel() {
        Object root = model.getRoot();
        selectionPath = new TreePath(root);

        if (!model.isLeaf(model.getRoot())) {
            BrowserPanel firstPanel = (BrowserPanel) columns.get(0);
            firstPanel.setRootNode(root);
        }
    }

    // rebuilds view for the new path selection
    private void updateFromModel(Object selectedNode, int panelIndex) {

        // build path to selected node
        // TreePath oldPath = this.selectionPath;
        this.selectionPath = rebuildPath(selectionPath, selectedNode);
        //   int lastRootIndex = selectionPath.getPathCount();

        // figure how to display the new path
        // selectedNode becomes the root of columns[panelIndex + 1]

        if (!model.isLeaf(selectedNode)) {

            // expand columns as needed
            expandView(panelIndex + 2 - columns.size());

            BrowserPanel lastPanel = (BrowserPanel) columns.get(panelIndex + 1);
            lastPanel.setRootNode(selectedNode);
            logObj.debug(
                "column: " + (panelIndex + 1) + ", updated with: " + selectedNode);
            scrollToColumn(panelIndex + 1);
        }

    }

    // builds a TreePath to the new node 
    // that is known to be a peer or a child of one 
    // of the path components
    private TreePath rebuildPath(TreePath path, Object node) {
        if (path == null) {
            return null;
        }

        if (model.getIndexOfChild(path.getLastPathComponent(), node) >= 0) {
            return path.pathByAddingChild(node);
        }
        else {
            // recursive call
            return rebuildPath(path.getParentPath(), node);
        }
    }

    /**
     * Returns current selection path or null if no selection is made.
     */
    public TreePath getSelectionPath() {
        return selectionPath;
    }

    public int getMinColumns() {
        return minColumns;
    }

    public void setMinColumns(int minColumns) {
        this.minColumns = minColumns;
    }

    public Dimension getPreferredColumnSize() {
        return preferredColumnSize;
    }

    public void setPreferredColumnSize(Dimension preferredColumnSize) {
        this.preferredColumnSize = preferredColumnSize;
        refreshPreferredSize();
    }

    public ListCellRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(ListCellRenderer renderer) {
        this.renderer = renderer;
    }

    public void setModel(TreeModel model) {
        this.model = model;

        // display first column
        initFromModel();
    }

    public TreeModel getModel() {
        return model;
    }

    public int getColumnsCount() {
        return columns.size();
    }

    // List adapter for the TreeModel node, showing the branch
    // containing node children
    final class ColumnListModel extends AbstractListModel {
        Object treeNode;
        int children;

        void setTreeNode(Object treeNode) {
            int oldChildren = children;
            this.treeNode = treeNode;
            this.children = (treeNode != null) ? model.getChildCount(treeNode) : 0;

            // must fire an event to refresh the view
            super.fireContentsChanged(
                MultiColumnBrowser.this,
                0,
                Math.max(oldChildren, children));
        }

        public Object getElementAt(int index) {
            return model.getChild(treeNode, index);
        }

        public int getSize() {
            return children;
        }
    }

    final class BrowserPanel extends JList {
        BrowserPanel() {
            BrowserPanel.this.setModel(new ColumnListModel());
        }

        void setRootNode(Object node) {
            ((ColumnListModel) BrowserPanel.this.getModel()).setTreeNode(node);
        }

        Object getTreeNode() {
            return ((ColumnListModel) BrowserPanel.this.getModel()).treeNode;
        }
    }

    final class PanelController implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            // ignore "adjusting"
            if (!e.getValueIsAdjusting()) {
                BrowserPanel panel = (BrowserPanel) e.getSource();
                int index = e.getFirstIndex();
                Object selectedNode =
                    (index >= 0)
                        ? panel.getModel().getElementAt(index)
                        : panel.getTreeNode();

                updateFromModel(selectedNode, columns.indexOf(panel));
            }
        }
    };
}

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

import java.awt.GridLayout;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

    public static final int MIN_COLUMNS_COUNT = 3;

    protected TreeModel model;
    protected TreePath selectionPath;
    private int offset;
    private BrowserPanel[] columns;

    public MultiColumnBrowser() {
        this(MIN_COLUMNS_COUNT);
    }

    public MultiColumnBrowser(int columnsCount) {
        this(MIN_COLUMNS_COUNT, CellRenderers.listRendererWithIcons());
    }

    public MultiColumnBrowser(int columnsCount, ListCellRenderer cellRenderer) {
        if (columnsCount < MIN_COLUMNS_COUNT) {
            throw new IllegalArgumentException(
                "Expected "
                    + MIN_COLUMNS_COUNT
                    + " or more columns, got: "
                    + columnsCount);
        }

        initView(columnsCount, cellRenderer);
        initController();
    }

    private void initView(int columnsCount, ListCellRenderer cellRenderer) {
        setLayout(new GridLayout(1, columnsCount, 3, 3));
        columns = new BrowserPanel[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            columns[i] = new BrowserPanel();
            if (cellRenderer != null) {
                columns[i].setCellRenderer(cellRenderer);
            }

            add(new JScrollPane(columns[i]));
        }
    }

    private void initController() {
        ListSelectionListener browserSelector = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    // ignore "adjusting"
                    updateFromModel((BrowserPanel) e.getSource(), e.getFirstIndex());
                }
            }
        };

        for (int i = 0; i < columns.length; i++) {
            columns[i].addListSelectionListener(browserSelector);
        }
    }

    private void updateFromModel(BrowserPanel panel, int selectionIndex) {
        // clear browsers following currently selected browser
        int i = columns.length - 1;
        for (; columns[i] != panel && i >= 0; i--) {
            logObj.debug("cleaning column: " + i);
            columns[i].setRootNode(null);
        }

        // create new selection path
        Object selectedNode = panel.getModel().getElementAt(selectionIndex);
        selectionPath = rebuildPath(selectionPath, selectedNode);
        logObj.debug("new path: " + selectionPath);

        i++;
        columns[i].setRootNode(selectionPath.getPathComponent(offset + i));
        logObj.debug(
            "column: "
                + i
                + ", updated with: "
                + selectionPath.getPathComponent(offset + i));
    }

    private void updateFromModel() {
        int size = selectionPath.getPathCount();
        int len = Math.min(size - offset, getColumnsCount());

        if (len == 0) {
            return;
        }

        for (int i = 0; i < len; i++) {
            columns[i].setRootNode(selectionPath.getPathComponent(offset + i));
            logObj.debug(
                "column: "
                    + i
                    + ", updated with: "
                    + selectionPath.getPathComponent(offset + i));
        }
    }

    // builds a path to the new node 
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

    public void setModel(TreeModel model) {
        this.model = model;

        // display first column
        selectionPath = new TreePath(model.getRoot());
        updateFromModel();
    }

    public TreeModel getModel() {
        return model;
    }

    public int getColumnsCount() {
        return columns.length;
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
            getParent().invalidate();
            getParent().validate();
        }
    }
}

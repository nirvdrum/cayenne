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
package org.objectstyle.cayenne.modeler.dialog.query;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.modeler.util.EntityTreeModel;
import org.objectstyle.cayenne.modeler.util.MultiColumnBrowser;
import org.objectstyle.cayenne.modeler.util.UIUtil;
import org.scopemvc.core.Selector;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STable;
import org.scopemvc.view.swing.STableModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel for configuring SelectQuery ordering.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class SelectQueryOrderingPanel extends SPanel {
    private static Logger logObj = Logger.getLogger(SelectQueryOrderingPanel.class);

    private static final Dimension BROWSER_CELL_DIM = new Dimension(150, 100);
    private static final Dimension TABLE_DIM = new Dimension(460, 120);

    protected MultiColumnBrowser browser;
    protected STable orderingsTable;

    public SelectQueryOrderingPanel() {
        initView();
        initController();
    }

    private void initView() {
        // create widgets
        SButton addButton =
            new SButton(new SAction(SelectQueryController.ADD_ORDERING_CONTROL));
        addButton.setEnabled(true);

        SButton removeButton =
            new SButton(new SAction(SelectQueryController.REMOVE_ORDERING_CONTROL));
        removeButton.setEnabled(true);

        browser = new MultiColumnBrowser();
        browser.setPreferredColumnSize(BROWSER_CELL_DIM);
        browser.setDefaultRenderer();

        orderingsTable = new OrderingsTable();
        STableModel orderingsTableModel = new STableModel(orderingsTable);
        orderingsTableModel.setSelector(SelectQueryModel.ORDERINGS_SELECTOR);
        orderingsTableModel.setColumnNames(
            new String[] { "Path", "Ascending", "Ignore Case" });
        orderingsTableModel.setColumnSelectors(
            new Selector[] {
                OrderingModel.PATH_SELECTOR,
                OrderingModel.ASCENDING_SELECTOR,
                OrderingModel.CASE_SELECTOR });

        orderingsTable.setModel(orderingsTableModel);
        orderingsTable.setSelectionSelector(SelectQueryModel.SELECTED_ORDERING_SELECTOR);

        // assemble
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder =
            new PanelBuilder(
                new FormLayout(
                    "fill:min(400dlu;pref), 3dlu, fill:min(100dlu;pref)",
                    "top:p:grow, 3dlu, fill:100dlu"));

        // orderings table must grow as the dialog is resized
        builder.add(new JScrollPane(orderingsTable), cc.xy(1, 1, "d, fill"));
        builder.add(removeButton, cc.xywh(3, 1, 1, 1));
        builder.add(
            new JScrollPane(
                browser,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
            cc.xywh(1, 3, 1, 1));

        // while browser must fill the whole are, button must stay on top
        builder.add(addButton, cc.xy(3, 3, "d, top"));
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        // update model when a tree selection happens
        browser.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                Object[] path = e.getPath() != null ? e.getPath().getPath() : null;
                ((SelectQueryModel) getShownModel()).setNavigationPath(path);
            }
        });

        // scroll to selected row whenever a selection even occurs
        orderingsTable
            .getSelectionModel()
            .addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    UIUtil.scrollToSelectedRow(orderingsTable);
                }
            }
        });
    }

    public void setBoundModel(Object model) {
        super.setBoundModel(model);

        Object shownModel = getShownModel();

        // init tree model of the browser
        if (shownModel instanceof SelectQueryModel) {
            Object root = ((SelectQueryModel) shownModel).getRoot();
            if (root instanceof Entity) {
                EntityTreeModel treeModel = new EntityTreeModel((Entity) root);
                browser.setModel(treeModel);
            }
        }

        // init column sizes
        orderingsTable.getColumnModel().getColumn(0).setPreferredWidth(250);
    }

    final class OrderingsTable extends STable {
        OrderingsTable() {
            setRowHeight(25);
            setRowMargin(3);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return TABLE_DIM;
        }
    }
}

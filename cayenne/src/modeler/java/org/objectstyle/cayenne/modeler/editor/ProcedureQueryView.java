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
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.modeler.event.QueryDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.modeler.util.TextFieldAdapter;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class ProcedureQueryView extends JPanel {

    protected EventController mediator;
    protected TextFieldAdapter name;
    protected JComboBox queryRoot;

    public ProcedureQueryView(EventController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = new TextFieldAdapter(CayenneWidgetFactory.createTextField()) {

            protected void initModel(String text) {
                setQueryName(text);
            }
        };

        queryRoot = CayenneWidgetFactory.createComboBox();
        queryRoot.setRenderer(CellRenderers.listRendererWithIcons());

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:max(170dlu;pref)",
                "p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("ProcedureQuery Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name.getTextField(), cc.xy(3, 3));
        builder.addLabel("Query Root:", cc.xy(1, 5));
        builder.add(queryRoot, cc.xy(3, 5));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {

        queryRoot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Query query = mediator.getCurrentQuery();
                if (query != null) {
                    query.setRoot(queryRoot.getModel().getSelectedItem());
                    mediator.fireQueryEvent(new QueryEvent(this, query));
                }
            }
        });

        mediator.addQueryDisplayListener(new QueryDisplayListener() {

            public void currentQueryChanged(QueryDisplayEvent e) {
                initFromModel();
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    void initFromModel() {
        Query query = mediator.getCurrentQuery();

        if (!(query instanceof ProcedureQuery)) {
            setVisible(false);
            return;
        }

        ProcedureQuery procedureQuery = (ProcedureQuery) query;

        name.setText(procedureQuery.getName());

        // init root choices and title label..

        // - ProcedureQuery supports Procedure roots

        // TODO: now we only allow roots from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.

        DataMap map = mediator.getCurrentDataMap();
        Object[] roots = map.getProcedures().toArray();

        if (roots.length > 1) {
            Arrays.sort(roots, Comparators.getDataMapChildrenComparator());
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel(roots);
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);

        setVisible(true);
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String string) {
        string = (string == null) ? "" : string.trim();
        if (string.length() == 0) {
            throw new ValidationException("Enter name for ProcedureQuery");
        }
        
        DataMap map = mediator.getCurrentDataMap();
        Query query = mediator.getCurrentQuery();

        Query matchingQuery = map.getQuery(string);

        if (matchingQuery == null) {
            // completely new name, set new name for entity
            QueryEvent e = new QueryEvent(this, query, query.getName());
            MapUtil.setQueryName(map, query, string);
            mediator.fireQueryEvent(e);
        }
        else if (matchingQuery != query) {
            // there is a query with the same name
            throw new ValidationException("There is another query named '"
                    + string
                    + "'. Use a different name.");
        }
    }
}
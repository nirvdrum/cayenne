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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.dialog.query.SelectQueryController;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.modeler.event.QueryDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.query.Query;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Query editor panel.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class QueryDetailView extends JPanel implements QueryDisplayListener {
    static final Logger logObj = Logger.getLogger(QueryDetailView.class);
    
    protected EventController eventController;
    protected JTextField name;
    protected JComboBox queryRoot;
    protected JButton editButton;

    public QueryDetailView(EventController eventController) {
        this.eventController = eventController;

        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = CayenneWidgetFactory.createTextField();
        queryRoot = CayenneWidgetFactory.createComboBox();
        queryRoot.setRenderer(CellRenderers.listRendererWithIcons());
        editButton = CayenneWidgetFactory.createButton("Edit Query");

        // assemble
        this.setLayout(new BorderLayout());
        FormLayout layout =
            new FormLayout("right:max(50dlu;pref), 3dlu, fill:max(170dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("Query Configuration");
        builder.append("Query Name:", name);

        // for now only allow ObjEntities in queries
        // in the future this will be expanded...
        builder.append("Query Root:", queryRoot);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(editButton);

        this.add(builder.getPanel(), BorderLayout.CENTER);
        this.add(buttons, BorderLayout.SOUTH);
    }

    private void initController() {
        eventController.addQueryDisplayListener(this);

        name.setInputVerifier(new InputVerifier() {
            public boolean verify(JComponent input) {
                String text = name.getText();
                if (text != null) {
                    text = text.trim();
                }

                DataMap map = eventController.getCurrentDataMap();
                Query query = eventController.getCurrentQuery();

                Query matchingQuery = map.getQuery(text);

                if (matchingQuery == null) {
                    // completely new name, set new name for entity
                    QueryEvent e = new QueryEvent(this, query, query.getName());
                    MapUtil.setQueryName(map, query, text);
                    eventController.fireQueryEvent(e);
                    return true;
                }
                else if (matchingQuery == query) {
                    // no name changes, just return
                    return true;
                }
                else {
                    // there is an entity with the same name
                    return false;
                }
            }
        });

        queryRoot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Query query = eventController.getCurrentQuery();
                if (query != null) {
                    query.setRoot(queryRoot.getModel().getSelectedItem());
                    eventController.fireQueryEvent(new QueryEvent(this, query));
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Query query = eventController.getCurrentQuery();
                if (query != null) {
                    try {
                    new SelectQueryController(eventController, query).startup();
                    }
                    catch(Exception ex) {
                        logObj.warn("EEEEE", ex);
                    }
                }
            }
        });
    }

    /**
     * Updates the view from the current model state.
     * Invoked when a currently displayed query is changed.
     */
    private void initFromModel(Query query) {
        // init query name
        name.setText(query.getName());

        // init root choices

        // TODO: in the future we will allow all kinds of "roots"
        // for now just allow ObjEntity

        DataMap map = eventController.getCurrentDataMap();
        
        // TODO: now we only allow ObjEntities from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.
        Object[] entities = map.getObjEntities().toArray();

        if (entities.length > 1) {
            Arrays.sort(entities, Comparators.getDataMapChildrenComparator());
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel(entities);
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);
    }

    /**
     * Displays newly selected query.
     */
    public void currentQueryChanged(QueryDisplayEvent e) {
        Query query = e.getQuery();
        if (query == null) {
            return;
        }

        initFromModel(query);
    }
}

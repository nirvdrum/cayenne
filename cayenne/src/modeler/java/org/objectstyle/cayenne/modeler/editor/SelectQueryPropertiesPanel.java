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
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.TextFieldAdapter;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel that supports editing the properties of a GenericSelectQuery.
 * 
 * @author Andrei Adamchik
 */
public class SelectQueryPropertiesPanel extends JPanel {

    private static final Logger logObj = Logger
            .getLogger(SelectQueryPropertiesPanel.class);

    private static final Integer ZERO = new Integer(0);

    private static final String NO_CACHE_LABEL = "No Result Caching";
    private static final String LOCAL_CACHE_LABEL = "DataContext Cache";
    private static final String SHARED_CACHE_LABEL = "Shared Cache";

    private static final Object[] CACHE_POLICIES = new Object[] {
            GenericSelectQuery.NO_CACHE, GenericSelectQuery.LOCAL_CACHE,
            GenericSelectQuery.SHARED_CACHE
    };

    private static final Map cachePolicyLabels = new TreeMap();

    static {
        cachePolicyLabels.put(GenericSelectQuery.NO_CACHE, NO_CACHE_LABEL);
        cachePolicyLabels.put(GenericSelectQuery.LOCAL_CACHE, LOCAL_CACHE_LABEL);
        cachePolicyLabels.put(GenericSelectQuery.SHARED_CACHE, SHARED_CACHE_LABEL);
    }

    protected EventController mediator;

    protected JCheckBox dataRows;
    protected JCheckBox refreshesResults;
    protected TextFieldAdapter fetchLimit;
    protected TextFieldAdapter pageSize;
    protected JComboBox cachePolicy;

    public SelectQueryPropertiesPanel(EventController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    private void initView() {
        // create widgets

        dataRows = new JCheckBox();
        refreshesResults = new JCheckBox();

        fetchLimit = new TextFieldAdapter(7) {

            protected void initModel(String text) {
                setFetchLimit(text);
            }
        };

        pageSize = new TextFieldAdapter(7) {

            protected void initModel(String text) {
                setPageSize(text);
            }
        };

        cachePolicy = CayenneWidgetFactory.createComboBox();
        cachePolicy.setRenderer(new CachePolicyRenderer());

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, left:max(50dlu;pref) fill:max(120dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("", cc.xywh(1, 1, 4, 1));

        builder.addLabel("Result Caching:", cc.xy(1, 3));
        builder.add(cachePolicy, cc.xywh(3, 3, 2, 1));
        builder.addLabel("Fetch Data Rows:", cc.xy(1, 7));
        builder.add(dataRows, cc.xy(3, 7));
        builder.addLabel("Refresh Objects:", cc.xy(1, 9));
        builder.add(refreshesResults, cc.xy(3, 9));
        builder.addLabel("Fetch Limit, Rows:", cc.xy(1, 11));
        builder.add(fetchLimit.getTextField(), cc.xy(3, 11));
        builder.addLabel("Page Size:", cc.xy(1, 13));
        builder.add(pageSize.getTextField(), cc.xy(3, 13));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);

        // store children
    }

    private void initController() {
        dataRows.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Boolean b = dataRows.isSelected() ? Boolean.TRUE : Boolean.FALSE;
                setQueryProperty("fetchingDataRows", b);
            }
        });

        refreshesResults.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Boolean b = refreshesResults.isSelected() ? Boolean.TRUE : Boolean.FALSE;
                setQueryProperty("refreshingObjects", b);
            }
        });

        cachePolicy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Object policy = cachePolicy.getModel().getSelectedItem();
                setQueryProperty("cachePolicy", policy);
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    public void initFromModel(GenericSelectQuery query) {
        DefaultComboBoxModel cacheModel = new DefaultComboBoxModel(CACHE_POLICIES);
        cacheModel.setSelectedItem(query.getCachePolicy());
        cachePolicy.setModel(cacheModel);

        dataRows.setSelected(query.isFetchingDataRows());
        refreshesResults.setSelected(query.isRefreshingObjects());

        fetchLimit.setText(String.valueOf(query.getFetchLimit()));
        pageSize.setText(String.valueOf(query.getPageSize()));
    }

    void setQueryProperty(String property, Object value) {
        Query query = getQuery();
        if (query != null) {
            try {
                PropertyUtils.setProperty(query, property, value);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
            catch (Exception ex) {
                logObj.warn("Error setting property: " + property, ex);
            }
        }
    }

    void setFetchLimit(String string) {
        string = (string == null) ? "" : string.trim();

        if (string.length() == 0) {
            setQueryProperty("fetchLimit", ZERO);
        }
        else {
            try {
                setQueryProperty("fetchLimit", new Integer(string));
            }
            catch (NumberFormatException nfex) {
                throw new ValidationException("Fetch limit must be an integer: " + string);
            }
        }
    }

    void setPageSize(String string) {
        string = (string == null) ? "" : string.trim();

        if (string.length() == 0) {
            setQueryProperty("pageSize", ZERO);
        }
        else {
            try {
                setQueryProperty("pageSize", new Integer(string));
            }
            catch (NumberFormatException nfex) {
                throw new ValidationException("Page size must be an integer: " + string);
            }
        }
    }

    Query getQuery() {
        return mediator.getCurrentQuery();
    }

    final class CachePolicyRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(
                JList list,
                Object object,
                int arg2,
                boolean arg3,
                boolean arg4) {

            object = cachePolicyLabels.get(object);
            if (object == null) {
                object = NO_CACHE_LABEL;
            }

            return super.getListCellRendererComponent(list, object, arg2, arg3, arg4);
        }
    }

    public void setEnabled(boolean flag) {
        super.setEnabled(flag);

        // propagate to children

        Container mainPanel = (Container) getComponent(0);
        Component[] children = mainPanel.getComponents();
        for (int i = 0; i < children.length; i++) {
            children[i].setEnabled(flag);
        }
    }
}
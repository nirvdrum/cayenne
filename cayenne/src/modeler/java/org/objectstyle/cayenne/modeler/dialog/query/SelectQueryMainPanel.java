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
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JList;

import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.scopemvc.view.swing.SCheckBox;
import org.scopemvc.view.swing.SComboBox;
import org.scopemvc.view.swing.SLabel;
import org.scopemvc.view.swing.SListCellRenderer;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STextField;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Main panel of SelectQuery edit dialog.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class SelectQueryMainPanel extends SPanel {

    private static final String NO_CACHE_LABEL = "No Result Caching";
    private static final String LOCAL_CACHE_LABEL = "DataContext Cache";
    private static final String SHARED_CACHE_LABEL = "Shared Cache";

    private static final Map cachePolicyLabels = new HashMap();

    static {
        cachePolicyLabels.put(GenericSelectQuery.NO_CACHE, NO_CACHE_LABEL);
        cachePolicyLabels.put(GenericSelectQuery.LOCAL_CACHE, LOCAL_CACHE_LABEL);
        cachePolicyLabels.put(GenericSelectQuery.SHARED_CACHE, SHARED_CACHE_LABEL);
    }

    protected SLabel rootLabel;

    public SelectQueryMainPanel() {
        initView();
    }

    protected void initView() {
        // create widgets
        STextField name = new STextField(40);
        name.setSelector(QueryModel.NAME_SELECTOR);

        STextField qualifier = new STextField(40);
        qualifier.setSelector(SelectQueryModel.QUALIFIER_SELECTOR);

        SCheckBox distinct = new SCheckBox();
        distinct.setSelector(SelectQueryModel.DISTINCT_SELECTOR);

        SCheckBox dataRows = new SCheckBox();
        dataRows.setSelector(SelectQueryModel.FETCHING_DATA_ROWS_SELECTOR);

        SCheckBox refreshesResults = new SCheckBox();
        refreshesResults.setSelector(SelectQueryModel.REFRESHING_OBJECTS_SELECTOR);

        STextField fetchLimit = new STextField(7);
        fetchLimit.setSelector(SelectQueryModel.FETCH_LIMIT_SELECTOR);

        STextField pageSize = new STextField(7);
        pageSize.setSelector(SelectQueryModel.PAGE_SIZE_SELECTOR);

        SComboBox cachePolicy = new SComboBox();
        cachePolicy.setSelectionSelector(SelectQueryModel.CACHE_POLICY_SELECTOR);
        cachePolicy.setSelector(SelectQueryModel.CACHE_POLICIES_SELECTOR);
        cachePolicy.setRenderer(new CachePolicyRenderer());

        rootLabel = new SLabel();
        rootLabel.setSelector(QueryModel.ROOT_NAME_SELECTOR);

        // assemble
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, left:max(50dlu;pref) fill:max(120dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, "
                        + "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("SelectQuery Settings", cc.xywh(1, 1, 4, 1));
        builder.addLabel("Query Root:", cc.xy(1, 3));
        builder.add(rootLabel, cc.xy(3, 3));
        builder.addLabel("Query Name:", cc.xy(1, 5));
        builder.add(name, cc.xywh(3, 5, 2, 1));
        builder.addLabel("Qualifier:", cc.xy(1, 7));
        builder.add(qualifier, cc.xywh(3, 7, 2, 1));

        builder.addSeparator("", cc.xywh(1, 9, 4, 1));
        builder.addLabel("Result Caching:", cc.xy(1, 11));
        builder.add(cachePolicy, cc.xywh(3, 11, 2, 1));
        builder.addLabel("Distinct:", cc.xy(1, 13));
        builder.add(distinct, cc.xy(3, 13));
        builder.addLabel("Fetch Data Rows:", cc.xy(1, 15));
        builder.add(dataRows, cc.xy(3, 15));
        builder.addLabel("Refresh Objects:", cc.xy(1, 17));
        builder.add(refreshesResults, cc.xy(3, 17));
        builder.addLabel("Fetch Limit, Rows:", cc.xy(1, 19));
        builder.add(fetchLimit, cc.xy(3, 19));
        builder.addLabel("Page Size:", cc.xy(1, 21));
        builder.add(pageSize, cc.xy(3, 21));

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void setBoundModel(Object model) {
        super.setBoundModel(model);

        // init root icon
        if (model instanceof QueryModel) {
            Object root = ((QueryModel) model).getRoot();
            Icon icon = CellRenderers.iconForObject(root);
            rootLabel.setIcon(icon);
        }
    }

    final class CachePolicyRenderer extends SListCellRenderer {

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
}
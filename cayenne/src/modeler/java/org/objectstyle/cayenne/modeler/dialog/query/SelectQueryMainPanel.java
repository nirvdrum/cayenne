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

import javax.swing.Icon;

import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.ScopeWidgetFactory;
import org.scopemvc.view.swing.SCheckBox;
import org.scopemvc.view.swing.SLabel;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Main panel of SelectQuery edit dialog.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class SelectQueryMainPanel extends SPanel {

    protected SLabel rootLabel;

    public SelectQueryMainPanel() {
        initView();
    }

    protected void initView() {
        // create widgets
        STextField name = ScopeWidgetFactory.createTextField(40);
        name.setSelector(QueryModel.NAME_SELECTOR);

        STextField qualifier = ScopeWidgetFactory.createTextField(40);
        qualifier.setSelector(SelectQueryModel.QUALIFIER_SELECTOR);

        SCheckBox distinct = new SCheckBox();
        distinct.setSelector(SelectQueryModel.DISTINCT_SELECTOR);

        SCheckBox dataRows = new SCheckBox();
        dataRows.setSelector(SelectQueryModel.FETCHING_DATA_ROWS_SELECTOR);

        SCheckBox refreshesResults = new SCheckBox();
        refreshesResults.setSelector(SelectQueryModel.REFRESHING_OBJECTS_SELECTOR);

        STextField fetchLimit = ScopeWidgetFactory.createTextField(7);
        fetchLimit.setSelector(SelectQueryModel.FETCH_LIMIT_SELECTOR);

        STextField pageSize = ScopeWidgetFactory.createTextField(7);
        pageSize.setSelector(SelectQueryModel.PAGE_SIZE_SELECTOR);

        rootLabel = new SLabel();
        rootLabel.setSelector(QueryModel.ROOT_NAME_SELECTOR);

        // assemble
        setLayout(new BorderLayout());

        FormLayout layout =
            new FormLayout("right:max(50dlu;pref), 3dlu, left:max(200dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.appendSeparator("SelectQuery Settings");
        builder.append("Query Root:", rootLabel);
        builder.append("Query Name:", name);
        builder.append("Qualifier:", qualifier);
        builder.appendSeparator();
        builder.append("Distinct:", distinct);
        builder.append("Fetch Data Rows:", dataRows);
        builder.append("Refresh Objects:", refreshesResults);
        builder.append("Fetch Limit, Rows:", fetchLimit);
        builder.append("Page Size:", pageSize);

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
}

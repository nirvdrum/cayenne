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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.DbAdapterInfo;
import org.objectstyle.cayenne.modeler.util.TextAreaAdapter;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.util.Util;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel for configuring SQL scripts of a SQL template.
 * 
 * @author Andrei Adamchik
 */
public class SQLTemplateScriptsTab extends JPanel {

    private static final String DEFAULT_LABEL = "Default";

    protected EventController mediator;

    protected JComboBox adapters;

    protected JList scripts;
    protected TextAreaAdapter script;

    protected JButton addScript;
    protected JButton removeScript;

    public SQLTemplateScriptsTab(EventController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    protected void initView() {
        // create widgets
        addScript = new JButton("Add Adapter Template");
        removeScript = new JButton("Remove Adapter Template");

        scripts = new JList();
        scripts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scripts.setCellRenderer(DbAdapterInfo.getListRenderer());

        script = new TextAreaAdapter(15, 30) {

            protected void initModel(String text) {
                setSQL(text);
            }

            protected void initModel(DocumentEvent e) {
                setSQL(e);
            }
        };

        adapters = CayenneWidgetFactory.createComboBox();
        adapters.setModel(new DefaultComboBoxModel(DbAdapterInfo.getStandardAdapters()));
        adapters.setRenderer(DbAdapterInfo.getListRenderer());

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:100dlu, 3dlu, fill:pref:grow, 3dlu, fill:100dlu",
                "3dlu, p, 3dlu, p, 10dlu, top:100dlu:grow"));

        // orderings table must grow as the panel is resized
        builder.add(new JScrollPane(
                scripts,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xywh(1, 2, 1, 5));
        builder.add(new JScrollPane(script.getTextArea()), cc.xywh(3, 2, 1, 5));
        builder.add(adapters, cc.xy(5, 2, "d, top"));
        builder.add(addScript, cc.xy(5, 4, "d, top"));
        builder.add(removeScript, cc.xy(5, 6, "d, top"));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initController() {

        // scroll to selected row whenever a selection even occurs
        scripts.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    displayScript();
                }
            }
        });

        addScript.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addAdapterScript();
            }
        });

        removeScript.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeAdapterScript();
            }
        });
    }

    void initFromModel() {
        Query query = mediator.getCurrentQuery();

        if (!(query instanceof SQLTemplate)) {
            setVisible(false);
            return;
        }

        initScriptsFromModel();

        // select default script
        scripts.setSelectedIndex(0);
        script.getTextArea().setEnabled(true);

        setVisible(true);
    }

    SQLTemplate getQuery() {
        return (SQLTemplate) mediator.getCurrentQuery();
    }

    void initScriptsFromModel() {
        SQLTemplate template = getQuery();

        List keys = new ArrayList(template.getTemplateKeys());
        Collections.sort(keys);
        keys.add(0, DEFAULT_LABEL);

        scripts.setModel(new DefaultComboBoxModel(keys.toArray()));
    }

    /**
     * Adds an empty SQL script for selected DbAdapter.
     */
    void addAdapterScript() {
        String key = (String) adapters.getSelectedItem();
        if (key == null) {
            return;
        }

        SQLTemplate query = getQuery();
        if (!query.getTemplateKeys().contains(key)) {
            query.setTemplate(key, "");
            initScriptsFromModel();
            mediator.fireQueryEvent(new QueryEvent(this, query));
        }

        scripts.setSelectedValue(key, true);
    }

    /**
     * Removes existing SQL script for selected DbAdapter.
     */
    void removeAdapterScript() {
        String key = (String) scripts.getSelectedValue();
        if (key == null || key.equals(DEFAULT_LABEL)) {
            return;
        }

        getQuery().removeTemplate(key);
        initScriptsFromModel();
        mediator.fireQueryEvent(new QueryEvent(this, getQuery()));
    }

    /**
     * Shows selected script in the editor.
     */
    void displayScript() {
        SQLTemplate query = getQuery();
        if (query == null) {
            disableEditor();
            return;
        }

        String key = (String) scripts.getSelectedValue();
        if (key == null) {
            disableEditor();
            return;
        }

        enableEditor();
        if (key.equals(DEFAULT_LABEL)) {
            script.setText(query.getDefaultTemplate());
        }
        else {
            script.setText(query.getCustomTemplate(key));
        }
    }

    void disableEditor() {
        script.setText(null);
        script.getTextArea().setEnabled(false);
        script.getTextArea().setEditable(false);
        script.getTextArea().setBackground(getBackground());
    }

    void enableEditor() {
        script.getTextArea().setEnabled(true);
        script.getTextArea().setEditable(true);
        script.getTextArea().setBackground(Color.WHITE);
    }

    void setSQL(DocumentEvent e) {
        Document doc = e.getDocument();

        try {
            setSQL(doc.getText(0, doc.getLength()));
        }
        catch (BadLocationException e1) {
            e1.printStackTrace();
        }

    }

    /**
     * Sets the value of SQL template for the currently selected script.
     */
    void setSQL(String text) {
        SQLTemplate query = getQuery();
        if (query == null) {
            return;
        }

        String key = (String) scripts.getSelectedValue();
        if (key == null) {
            return;
        }

        if (text != null) {
            text = text.trim();
            if (text.length() == 0) {
                text = null;
            }
        }

        // Compare the value before modifying the query - text area
        // will call "verify" even if no changes have occured....
        if (key.equals(DEFAULT_LABEL)) {
            if (!Util.nullSafeEquals(text, query.getDefaultTemplate())) {
                query.setDefaultTemplate(text);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
        }
        else {
            if (!Util.nullSafeEquals(text, query.getTemplate(key))) {
                query.setTemplate(key, text);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
        }

    }
}
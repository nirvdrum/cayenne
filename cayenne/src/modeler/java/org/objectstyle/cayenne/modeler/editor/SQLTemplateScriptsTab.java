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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLTemplate;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel for configuring SQL scripts of a SQL template.
 * 
 * @author Andrei Adamchik
 */
public class SQLTemplateScriptsTab extends JPanel {

    protected EventController mediator;

    protected JComboBox adapters;

    protected JList scripts;
    protected JTextArea script;

    protected JButton addScript;
    protected JButton removeScript;

    public SQLTemplateScriptsTab(EventController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    protected void initView() {
        // create widgets
        addScript = new JButton("Add");
        removeScript = new JButton("Remove");
        scripts = new JList();
        script = new JTextArea();
        adapters = new JComboBox();

        // assemble

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:100dlu, 3dlu, fill:pref:grow, 3dlu, fill:100dlu",
                "3dlu, p, 3dlu, p, 10dlu, top:100dlu:grow"));

        // orderings table must grow as the panel is resized
        builder.add(scripts, cc.xywh(1, 2, 1, 5));
        builder.add(new JScrollPane(script), cc.xywh(3, 2, 1, 5));
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
    }

    protected void initFromModel() {
        Query query = mediator.getCurrentQuery();

        if (!(query instanceof SQLTemplate)) {
            setVisible(false);
            return;
        }

        SQLTemplate template = (SQLTemplate) query;
        Object[] keys = template.getTemplateKeys().toArray();

        setVisible(true);
    }

    SQLTemplate getQuery() {
        return (SQLTemplate) mediator.getCurrentQuery();
    }

    void displayScript() {
        SQLTemplate query = getQuery();
        if (query == null) {
            return;
        }

    }
}
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.editor.dbentity;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.ProjectController;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class PKDBGeneratorPanel extends PKGeneratorPanel {

    protected JPanel checkboxes;

    public PKDBGeneratorPanel(ProjectController mediator) {
        super(mediator);

        initView();
    }

    private void initView() {

        checkboxes = new JPanel();

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "right:70dlu, 3dlu, fill:200dlu",
                "top:p"));
        builder.setDefaultDialogBorder();

        builder.addLabel("Auto Incremented:", cc.xy(1, 1));
        builder.add(checkboxes, cc.xy(3, 1));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void setDbEntity(DbEntity entity) {
        if (isVisible()) {
            onInit(entity);
        }
    }

    public void onInit(DbEntity entity) {
        resetStrategy(entity, true, false);

        checkboxes.removeAll();

        Collection pkAttributes = entity.getPrimaryKey();

        // by default check the only numeric PK
        if (pkAttributes.size() == 1) {
            DbAttribute pk = (DbAttribute) pkAttributes.iterator().next();
            if (TypesMapping.isNumeric(pk.getType()) && !pk.isGenerated()) {
                pk.setGenerated(true);
                mediator.fireDbEntityEvent(new EntityEvent(this, entity));
            }
        }

        if (pkAttributes.isEmpty()) {
            checkboxes.add(new JLabel("<Entity has no PK columns>"));
        }
        else {
            checkboxes.setLayout(new GridLayout(pkAttributes.size(), 1));

            Iterator it = pkAttributes.iterator();
            while (it.hasNext()) {
                final DbAttribute a = (DbAttribute) it.next();

                String type = TypesMapping.getSqlNameByType(a.getType());

                String name = a.getName() + " (" + (type != null ? type : "?") + ")";
                final JCheckBox c = new JCheckBox(name, a.isGenerated());
                c.addChangeListener(new ChangeListener() {

                    public void stateChanged(ChangeEvent e) {
                        if (a.isGenerated() != c.isSelected()) {
                            a.setGenerated(c.isSelected());
                            mediator.fireDbEntityEvent(new EntityEvent(this, a
                                    .getEntity()));
                        }
                    }
                });

                checkboxes.add(c);
            }
        }

        // revalidate as children layout has changed...
        revalidate();
    }
}

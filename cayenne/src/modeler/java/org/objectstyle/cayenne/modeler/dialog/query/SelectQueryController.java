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

import java.awt.Component;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.ModelChangeListener;
import org.scopemvc.core.Selector;

/**
 * @author Andrei Adamchik
 */
public class SelectQueryController extends BasicController {
    private static Logger logObj = Logger.getLogger(SelectQueryController.class);

    public static final String CANCEL_CONTROL =
        "cayenne.modeler.selectQuery.cancel.button";

    public static final String SAVE_CONTROL = "cayenne.modeler.selectQuery.save.button";

    public static final String ADD_ORDERING_CONTROL =
        "cayenne.modeler.selectQuery.addOrdering.button";

    public static final String REMOVE_ORDERING_CONTROL =
        "cayenne.modeler.selectQuery.removeOrdering.button";

    protected EventController mediator;
    protected boolean modified;
    protected boolean renamed;
    protected ModelChangeListener eventHelper;

    public SelectQueryController(EventController mediator, Query query) {
        this.mediator = mediator;
        this.eventHelper = new EventHelper();

        SelectQueryModel model = new SelectQueryModel(query);
        setModel(model);
        model.addModelChangeListener(eventHelper);
    }

    /**
     * Creates and runs the classpath dialog.
     */
    public void startup() {
        setView(new SelectQueryDialog());
        showView();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(SAVE_CONTROL)) {
            saveQuery();
        }
        else if (control.matchesID(ADD_ORDERING_CONTROL)) {
            addOrdering();
        }
        else if (control.matchesID(REMOVE_ORDERING_CONTROL)) {
            removeOrdering();
        }
    }

    protected void addOrdering() {
        SelectQueryModel model = (SelectQueryModel) getModel();
        OrderingModel newOrdering = model.createOrderingFromNavigationPath();

        if (newOrdering == null) {
            return;
        }

        // check that there are no orderings for 
        // the same path..
        Iterator it = model.getOrderings().iterator();
        while (it.hasNext()) {
            OrderingModel ordering = (OrderingModel) it.next();
            if (Util.nullSafeEquals(ordering.getPath(), newOrdering.getPath())) {
                return;
            }
        }

        model.getOrderings().add(newOrdering);
        modified = true;
    }

    protected void removeOrdering() {
        SelectQueryModel model = (SelectQueryModel) getModel();
        OrderingModel removeOrdering = model.getSelectedOrdering();

        if (removeOrdering != null) {
            model.getOrderings().remove(removeOrdering);
            modified = true;
        }
    }

    protected void saveQuery() {
        SelectQueryModel model = (SelectQueryModel) getModel();

        if (renamed) {
            Query query = model.getQuery();
            DataMap map = mediator.getCurrentDataMap();
            Query matchingQuery = map.getQuery(model.getName());

            if (matchingQuery == null) {
                // completely new name, set new name for entity
                QueryEvent e = new QueryEvent(this, query, query.getName());
                MapUtil.setQueryName(map, query, model.getName());
                mediator.fireQueryEvent(e);
            }
            else if (matchingQuery != query) {
                // there is a query with the same name
                JOptionPane.showMessageDialog(
                    (Component) getView(),
                    "Duplicate query name: " + model.getName(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            renamed = false;
        }

        if (modified) {
            model.updateQuery();
            mediator.fireQueryEvent(new QueryEvent(getView(), model.getQuery()));
            modified = false;
        }

        shutdown();
    }

    final class EventHelper implements ModelChangeListener {
        public void modelChanged(ModelChangeEvent event) {
            // name change requires special handling
            Selector selector = event.getSelector();
            if (selector.startsWith(QueryModel.NAME_SELECTOR)) {
                renamed = true;
            }
            else {
                modified = true;
            }
        }
    }
}

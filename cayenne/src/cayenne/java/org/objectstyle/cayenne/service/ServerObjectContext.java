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
package org.objectstyle.cayenne.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * A wrapper around regular DataContext that works as a client ObjectContext peer on the
 * server side.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: DataContext should be made compatible with ObjectContext interface at some point.
// Get rid of this class whwn this happens.
public class ServerObjectContext implements ObjectContext {

    protected DataContext dataContext;

    public ServerObjectContext(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    /**
     * Commits all changes to the underlying DataContext.
     */
    public void commitChanges() {
        dataContext.commitChanges();
    }

    public void commitChangesInContext(ObjectContext context) {

    }

    public List performQuery(String queryName, Map parameters, boolean refresh) {

        List results = dataContext.performQuery(queryName, parameters, refresh);

        if (results.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        // if DataRows, return as is
        Object first = results.get(0);
        if (!(first instanceof DataObject)) {
            // TODO: filter out server-side properties
            return results;
        }

        // translate to client objects

        EntityResolver resolver = dataContext.getEntityResolver();
        ObjEntity entity = resolver.lookupObjEntity((DataObject) first);

        try {
            List clientObjects = ClientServerUtils.toClientObjects(entity, results);
            System.out.println("Client objects: " + clientObjects);
            return clientObjects;
        }
        catch (Exception ex) {
            Throwable th = Util.unwindException(ex);
            throw new CayenneRuntimeException("Error instantiating client objects - "
                    + th.getClass().getName()
                    + ": "
                    + ex.getMessage(), ex);
        }
    }

    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        return dataContext.performNonSelectingQuery(queryName, parameters);
    }

    public int[] performNonSelectingQuery(Query query) {
        return dataContext.performNonSelectingQuery(query);
    }

    public List performQuery(GenericSelectQuery query) {
        return dataContext.performQuery(query);
    }

    public void objectWillRead(Persistent dataObject, String property) {
        // hmm... what would this be on the server...
    }

    public void objectWillWrite(
            Persistent dataObject,
            String property,
            Object oldValue,
            Object newValue) {
        // hmm... what would this be on the server...
    }

    public Collection deletedObjects() {
        return dataContext.deletedObjects();
    }

    public Collection modifiedObjects() {
        return dataContext.modifiedObjects();
    }

    public Collection newObjects() {
        return dataContext.newObjects();
    }
}
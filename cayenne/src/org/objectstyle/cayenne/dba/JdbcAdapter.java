package org.objectstyle.cayenne.dba;
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

import java.util.Iterator;
import java.util.logging.Logger;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationSorter;
import org.objectstyle.cayenne.map.*;

/** 
 * A generic DbAdapter implementation. 
 * Can be used as a default adapter or as
 * a superclass of a concrete adapter implementation.  
 *
 * @author Andrei Adamchik
 */
public class JdbcAdapter implements DbAdapter {
    static Logger logObj = Logger.getLogger(JdbcAdapter.class.getName());

    protected PkGenerator pkGenerator;
    protected TypesHandler typesHandler;

    public JdbcAdapter() {
        // create Pk generator
        pkGenerator = createPkGenerator();
        typesHandler = TypesHandler.getHandler(this.getClass());
    }

    /** 
     * Creates and returns a primary key generator. This factory
     * method should be overriden by JdbcAdapter subclasses to
     * provide custom implementations of PKGenerator. 
     */
    protected PkGenerator createPkGenerator() {
        return new JdbcPkGenerator();
    }
    
    /** Returns primary key generator associated with this DbAdapter. */
    public PkGenerator getPkGenerator() {
        return pkGenerator;
    }
    

    /** Returns true. */
    public boolean supportsFkConstraints() {
        return true;
    }

    /** Returns a query string to drop a table corresponding
      * to <code>ent</code> DbEntity. */
    public String dropTable(DbEntity ent) {
        return "DROP TABLE " + ent.getName();
    }

    /** Returns a SQL string that can be used to create
      * a foreign key constraint for the relationship. */
    public String createFkConstraint(DbRelationship rel) {
        StringBuffer buf = new StringBuffer();
        StringBuffer refBuf = new StringBuffer();

        buf.append("ALTER TABLE ").append(rel.getSourceEntity().getName()).append(
            " ADD FOREIGN KEY (");

        Iterator jit = rel.getJoins().iterator();
        boolean first = true;
        while (jit.hasNext()) {
            DbAttributePair join = (DbAttributePair) jit.next();
            if (!first) {
                buf.append(", ");
                refBuf.append(", ");
            }
            else
                first = false;

            buf.append(join.getSource().getName());
            refBuf.append(join.getTarget().getName());
        }

        buf
            .append(") REFERENCES ")
            .append(rel.getTargetEntity().getName())
            .append(" (")
            .append(refBuf)
            .append(')');
        return buf.toString();
    }

    public String[] externalTypesForJdbcType(int type) {
        return typesHandler.externalTypesForJdbcType(type);
    }

    /** Returns null - by default no operation sorter is used. */
    public OperationSorter getOpSorter(DataNode node) {
        return null;
    }
}
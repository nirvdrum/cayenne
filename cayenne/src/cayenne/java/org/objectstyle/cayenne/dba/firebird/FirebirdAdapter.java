/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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
package org.objectstyle.cayenne.dba.firebird;

import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.trans.TrimmingQualifierTranslator;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;

/**
 * DbAdapter implementation for <a href="http://www.firebirdsql.org">Firebird RDBMS</a>.
 * Sample <a target="_top" href="../../../../../../../developerguide/unit-tests.html">connection
 * settings</a> to use with Firebird are shown below:
 *
 * <pre>
 * test-firebird.cayenne.adapter = org.objectstyle.cayenne.dba.firebird.FirebirdAdapter
 * test-firebird.jdbc.username = sysdba
 * test-firebird.jdbc.password = masterkey
 * test-firebird.jdbc.url = jdbc:firebirdsql:[host[/port]/]<database>
 * test-firebird.jdbc.driver = org.firebirdsql.jdbc.FBDriver
 * </pre>
 *
 * @author Heiko Wenzel
 */
public class FirebirdAdapter extends JdbcAdapter {   
	
	public FirebirdAdapter() {
		super();
	}

    public String createTable(DbEntity ent) {
        if (ent instanceof DerivedDbEntity) {
            throw new CayenneRuntimeException(
            "Can't create table for derived DbEntity '" + ent.getName() + "'.");
        }
        
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(ent.getFullyQualifiedName()).append(" (");
        
        // columns
        Iterator it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            }
            else {
                buf.append(", ");
            }
            
            DbAttribute at = (DbAttribute) it.next();
            
            // attribute may not be fully valid, do a simple check
            if (at.getType() == TypesMapping.NOT_DEFINED) {
                throw new CayenneRuntimeException(
                "Undefined type for attribute '"
                + ent.getFullyQualifiedName()
                + "."
                + at.getName()
                + "'.");
            }
            
            String[] types = externalTypesForJdbcType(at.getType());
            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException(
                "Undefined type for attribute '"
                + ent.getFullyQualifiedName()
                + "."
                + at.getName()
                + "': "
                + at.getType());
            }
            
            String type = types[0];
            buf.append(at.getName()).append(' ').append(type);
            
            // append size and precision (if applicable)
            if (TypesMapping.supportsLength(at.getType())) {
                int len = at.getMaxLength();
                int prec = TypesMapping.isDecimal(at.getType()) ? at.getPrecision() : -1;
                
                if (type.compareTo("double precision") == 0)  // double precision returns with len = 15
                    len = 0;
                
                // sanity check
                if (prec > len) {
                    prec = -1;
                }
                
                if (len > 0) {
                    buf.append('(').append(len);
                    
                    if (prec >= 0) {
                        buf.append(", ").append(prec);
                    }
                    buf.append(')');
                }
            }
            
            if (at.isMandatory()) {
                buf.append(" NOT NULL");
            }
            
        }
        
        // primary key clause
        Iterator pkit = ent.getPrimaryKey().iterator();
        if (pkit.hasNext()) {
            if (first)
                first = false;
            else
                buf.append(", ");
            
            buf.append("PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    buf.append(", ");
                
                DbAttribute at = (DbAttribute) pkit.next();
                buf.append(at.getName());
            }
            buf.append(')');
        }
        buf.append(");");  // added ; and so I can execute the DDL Script at once
        return buf.toString();
    }
    
    /**
     * Returns a SQL string that can be used to create
     * a foreign key constraint for the relationship.
     */
    public String createFkConstraint(DbRelationship rel) {
        // added ; and so I can execute the DDL Script at once
        return super.createFkConstraint(rel) + ";";
    }
    
    public String dropTable(DbEntity ent) {
        // added ; and so I can execute the DDL Script at once
        return super.dropTable(ent) + ";";
    }
    
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        map.registerType(new CharType(true, false));
    }
    
    /**
     * @see JdbcAdapter#createPkGenerator()
     */
    protected PkGenerator createPkGenerator() {
        return new FirebirdPkGenerator();
    } 
    
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new TrimmingQualifierTranslator(queryAssembler, "");
    }
}

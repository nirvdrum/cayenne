package org.objectstyle.cayenne.map;
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


import java.util.*;


/** Metadata for the relational database table. */
public class DbEntity  extends Entity {
    // The catalog of the database table.
    private String catalog;

    // Database table schema
    private String schema;


    public DbEntity() {}

    public DbEntity(String name) {
        setName(name);
    }


    /** Get schema of this table.
     *  @return table's schema, null if not set.*/
    public String getSchema() {
        return schema;
    }


    /** Set the schema of this table. */
    public void setSchema(String schema) {
        this.schema = schema;
    }


    /** Get the catalog of this table.
     *  @return catalog, or null if not set or applicable.*/
    public String getCatalog() {
        return catalog;
    }


    /** Set the catalog for this table.*/
    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }



    /** Returns a list of DbAttributes representing the key of the given database table. */
    public List getPrimaryKey() {
        ArrayList list = new ArrayList();
        Iterator it = this.getAttributeList().iterator();
        while(it.hasNext()) {
            DbAttribute dba = (DbAttribute)it.next();
            if(dba.isPrimaryKey())
                list.add(dba);
        }
        return list;
    }
        
    public String toString() {
        StringBuffer sb = new StringBuffer("DbEntity:");
        sb.append("\nTable name: ").append(name);

        // 1. print attributes
        Iterator attIt = attributes.values().iterator();
        while(attIt.hasNext()) {
            DbAttribute dbAttribute = (DbAttribute)attIt.next();
            String name = dbAttribute.getName();
            int type = dbAttribute.getType();
            sb.append("\n   Column name: ").append(name);
            if(dbAttribute.isPrimaryKey())
                sb.append(" (pk)");

            sb.append("\n   Column type: ").append(type);
            sb.append("\n------------------");
        }

        // 2. print relationships
        Iterator relIt = getRelationshipList().iterator();
        while(relIt.hasNext()) {
            DbRelationship dbRel = (DbRelationship)relIt.next();
            sb.append("\n   Rel. to: ").append(dbRel.getTargetEntity().getName());
            sb.append("\n------------------");
        }

        return sb.toString();
    }
}

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

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationSorter;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;


/** Defines API needed to handle differences between
  * various databases accessed via JDBC. Implementing classed are 
  * intended to be pluggable database specific adapters.
  *
  * @author Andrei Adamchik 
  */
public interface DbAdapter {

    /** Returns true if a target database supports FK constraints. */
    public boolean supportsFkConstraints();

    /** Returns a SQL string that can be used to drop
      * a database table corresponding to <code>ent</code> parameter. */
    public String dropTable(DbEntity ent);


    /** Returns a SQL string that can be used to create
      * a foreign key constraint for the relationship. */
    public String createFkConstraint(DbRelationship rel);


    /** Creates necessary database objects to do primary key generation. */
    public void createAutoPkSupport(DataNode dataNode) throws Exception;


    /** Performs necessary database operations to do primary key generation
     *  for a particular DbEntity. This  may require a prior call to 
     *  <code>"createAutoPkSupport"<code> method.
     *
     *  @param dataNode node that provides connection layer for PkGenerator.
     *  @param dbEntity DbEntity that needs an auto PK support
     */
    public void createAutoPkSupportForDbEntity(DataNode dataNode, DbEntity dbEntity) throws Exception;


    /** Generates unique and non-repeating primary key for specified dbEntity. */
    public Object generatePkForDbEntity(DataNode dataNode, DbEntity dbEntity) throws Exception;

    /** Returns an array of RDBMS types that can be used with JDBC <code>type</code>.
      * Valid types are defined in java.sql.Types. */
    public String[] externalTypesForJdbcType(int type);


    /** Returns an operation sorter or null if no sorting
      * is required. Operation sorter is needed for databases
      * (like Sybase) that do not have deferred constraint checking
      * and need appropriate operation ordering within transactions. */
    public OperationSorter getOpSorter(DataNode node);
}
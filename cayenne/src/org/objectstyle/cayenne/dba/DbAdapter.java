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

package org.objectstyle.cayenne.dba;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.access.trans.QualifierTranslatorFactory;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.Query;

/** 
 * Defines API needed to handle differences between various 
 * databases accessed via JDBC. Implementing classed are 
 * intended to be pluggable database-specific adapters. 
 * DbAdapter-based architecture is introduced to solve the 
 * following problems:
 * 
 * <ul>
 * <li>Make Cayenne code independent from SQL syntax differences
 * between different RDBMS.
 * <li>Allow for vendor-specific tuning of JDBC access.
 * </ul>
 *
 * @author Andrei Adamchik 
 */
public interface DbAdapter {

    public static final String JDBC = "org.objectstyle.cayenne.dba.JdbcAdapter";
    public static final String MYSQL =
        "org.objectstyle.cayenne.dba.mysql.MySQLAdapter";
    public static final String ORACLE =
        "org.objectstyle.cayenne.dba.oracle.OracleAdapter";
    public static final String SYBASE =
        "org.objectstyle.cayenne.dba.sybase.SybaseAdapter";
    public static final String POSTGRES =
        "org.objectstyle.cayenne.dba.postgres.PostgresAdapter";

    /** Returns true if a target database supports FK constraints. */
    public boolean supportsFkConstraints();

    /** Returns a SQL string that can be used to drop
      * a database table corresponding to <code>ent</code> parameter. */
    public String dropTable(DbEntity ent);

    /**
     *  Returns a SQL string that can be used to create
     * a foreign key constraint for the relationship. 
     */
    public String createFkConstraint(DbRelationship rel);

    /** 
     * Returns an array of RDBMS types that can be used with JDBC <code>type</code>.
     * Valid types are defined in java.sql.Types. 
     */
    public String[] externalTypesForJdbcType(int type);
    
    /** 
     * Returns a map of type converters (ExtendedType objects)
     * that can be used to translate between simple Java types 
     * and JDBC formats.
     */
    public ExtendedTypeMap getTypeConverter();

    /** 
     * Returns an operation sorter or null if no sorting
     * is required. Operation sorter is needed for databases
     * (like Sybase) that do not have deferred constraint checking
     * and need appropriate operation ordering within transactions. 
     */
    public OperationSorter getOpSorter(DataNode node);

    /** 
     * Returns primary key generator associated with this DbAdapter. 
     */
    public PkGenerator getPkGenerator();


    /** 
     * Creates and returns a QueryTranslator appropriate for the 
     * specified <code>query</code> parameter. Sets translator
     * "query" and "adapter" property.
     * 
     * <p>This factory method allows subclasses to specify their
     * own translators that implement vendor-specific optimizations.
     * </p>
     */
    public QueryTranslator getQueryTranslator(Query query) throws Exception;
    
    public QualifierTranslatorFactory getQualifierFactory();
    
    /**
     * Creates and returns a DbAttribute based on supplied parameters 
     * (usually obtained from database meta data).
     * 
     * @param name database column name
     * @param type JDBC column type
     * @param size database column size
     * @param precision database column precision (ignored if less than zero)
     * @param allowNulls database column nullable parameter
     */ 
    public DbAttribute buildAttribute(String name, int type, int size, int precision, boolean allowNulls);
}
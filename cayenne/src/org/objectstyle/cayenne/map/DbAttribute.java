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

package org.objectstyle.cayenne.map;

import org.objectstyle.cayenne.dba.TypesMapping;

/** 
 * Contains information about a database column.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbAttribute extends Attribute {
	/** 
	 * The type of the column. 
	 */
	protected int type = TypesMapping.NOT_DEFINED;

	/**
	 * If <code>true</code>, column corresponding to 
	 * this attribute does not allows nulls.
	 */
	protected boolean mandatory;

	/** 
	 * If <code>true</code>, this attribute is 
	 * a part of primary key.
	 */
	protected boolean primaryKey;

	// The length of CHAR or VARCHAr or max num of digits for DECIMAL.
	protected int maxLength = -1;

	// The number of digits after period for DECIMAL.
	protected int precision = -1;

	public DbAttribute() {
	}
	
	public DbAttribute(String name) {
		setName(name);
	}

	public DbAttribute(String name, int type, DbEntity entity) {
		setName(name);
		setType(type);
		setEntity(entity);
	}

	public String getAliasedName(String alias) {
		return (alias != null) ? alias + '.' + name : name;
	}

	/** 
	 * Returns the SQL type of the column.
	 * 
	 * @see java.sql.Types
	 */
	public int getType() {
		return type;
	}

	/** 
	 * Sets the SQL type for the column.
	 * 
	 * @see java.sql.Types
	 */
	public void setType(int type) {
		this.type = type;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	/** Returns the length of database column described by this attribute. */
	public int getMaxLength() {
		return maxLength;
	}

	/** Sets the length of character or binary type or max num of digits for DECIMAL.*/
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	/** Returns the number of digits after period for DECIMAL.*/
	public int getPrecision() {
		return precision;
	}

	/** Sets the number of digits after period for DECIMAL.*/
	public void setPrecision(int precision) {
		this.precision = precision;
	}

	/** Appends string representation of attribute to a provided buffer.
	 *  This is a variation of "toString" method. It may be more
	 *  efficient in some cases. For example, when printing all
	 *  attributes of a single entity together. */
	public StringBuffer toStringBuffer(StringBuffer buf) {
		buf.append("   Column name: " + name + "\n");
		buf.append("   Column type: " + type + "\n");
		return buf;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("DbAttribute\n");
		return this.toStringBuffer(buf).toString();
	}

}

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
package org.objectstyle.cayenne.gui.datamap;

import java.util.ArrayList;

import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.map.*;

/**
 * @author Andrei Adamchik
 */
public class DerivedAttributeParamsTableModel extends DbAttributeTableModel {
	private static final int DB_ATTRIBUTE_NAME = 0;
	private static final int DB_ATTRIBUTE_TYPE = 1;

	protected DerivedDbAttribute derived;

	/**
	 * Constructor for DerivedAttributeParamsTableModel.
	 */
	public DerivedAttributeParamsTableModel(
		DerivedDbAttribute derived,
		Mediator mediator,
		Object eventSource) {

		super(
			((DerivedDbEntity) derived.getEntity()).getParentEntity(),
			mediator,
			eventSource,
			new ArrayList(derived.getParams()));
		this.derived = derived;
	}

    /**
     * Returns <code>null</code> to disable ordering.
     */
	public String getOrderingKey() {
		return null;
	}
	
    public DbEntity getParentEntity() {
    	return ((DerivedDbEntity) derived.getEntity()).getParentEntity();
    }
    
	/**
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return 2;
	}

	public String getColumnName(int col) {
		switch(col) {
			case DB_ATTRIBUTE_NAME: return "Name";
			case DB_ATTRIBUTE_TYPE: return "Type";
			default: return "";
		}
	}
	
	public Object getValueAt(int row, int column) {
		DbAttribute attr = getAttribute(row);

		if (attr == null) {
			return "";
		}

		switch (column) {
			case DB_ATTRIBUTE_NAME :
				return getAttributeName(attr);
			case DB_ATTRIBUTE_TYPE :
				return getAttributeType(attr);
			default :
				return "";
		}
	}

	public void setValueAt(Object newVal, int row, int col) {
		if (col == nameColumnInd()) {
			replaceParameter(row, (String)newVal);
		}
	}

	/** Replaces parameter at index with the new attribute. */
	protected void replaceParameter(int ind, String attrName) {
		if (attrName != null) {
			objectList.set(ind, getParentEntity().getAttribute(attrName));
			fireTableDataChanged();
		}
	}

	public boolean isCellEditable(int row, int col) {
		return col == DB_ATTRIBUTE_NAME;
	}
}

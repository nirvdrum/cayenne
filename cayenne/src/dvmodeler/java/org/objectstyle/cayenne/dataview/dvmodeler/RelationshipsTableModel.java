/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group
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

package org.objectstyle.cayenne.dataview.dvmodeler;

import java.util.*;
import javax.swing.table.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class RelationshipsTableModel extends AbstractTableModel {
  private List objRelationships = new ArrayList();
  private String[] columnNames= {"Name",
                                 "Source",
                                 "Target",
                                 "To Many"};


  public RelationshipsTableModel() {
  }

  /*Setting another relationships in model*/
  public void setObjRelationships(List relationships){
    objRelationships = relationships;
    fireTableStructureChanged();
  }

  /*Returns the number of rows in the model. */
  public int getRowCount(){
    return objRelationships.size();
  }

  /*Returns the number of columns in the model. */
  public int getColumnCount(){
      return columnNames.length;
  }

  /*Returns a default name for the column */
  public String getColumnName(int col) {
    return columnNames[col];
  }

  /*Returns Object.class regardless of columnIndex.*/
  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  /*Returns the value for the cell at column index and row index. */
  public Object getValueAt(int row, int column){
    ObjRelationship relationship = (ObjRelationship)objRelationships.get(row);
    switch (column){
      case 0:
        return relationship.getName();
      case 1:
        return relationship.getSourceObjEntity();
      case 2:
        return relationship.getTargetObjEntity();
      case 3:
        return Boolean.valueOf(relationship.isToMany());

      default: return null;
    }

  }
}

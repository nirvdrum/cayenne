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
package org.objectstyle.cayenne.modeler.datamap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.modeler.CayenneDialog;
import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.util.RelationshipWrapper;

/** 
 * Used to select the DbRelationship for ObjRelationship mapping. 
 * Allows selecting the relationship, canceling, edit the relationship and
 * create new relationship. It is needed for the
 * cases when there is more than one DbRelationship between start and
 * end entities, like in the case when the DbRelationship starts and
 * ends in the same DbEntity.
 * 
 * <p>The choice is returned in getChoice() method. If choice is SELECT or EDIT,
 * the selected DbRelationship may be retrieved by getDbRelationship(),
 * which will return the list with one DbRelationship. List is used for the
 * future expansion, when one ObjRelaitonship will be mapped for 
 * multiple DbRelaitonship's.</p>
 *  
 * <p>Existing mapping for this ObjRelationship is pre-selected in the combo box.
 * Combo box contains the DbRelaitonship-s between the start and  end DbEntity's</p>
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ChooseDbRelationshipDialog extends CayenneDialog
implements ActionListener {
	public static final int SELECT 	= 0;
	public static final int CANCEL 	= 1;
	public static final int NEW		= 2;
	public static final int EDIT	= 3;

	private DataMap map;
	private DbEntity start;
	private DbEntity end;
	private List dbRels;
	private List relList = new ArrayList();
	
	JComboBox relSelect	= new JComboBox();
	JButton select		= new JButton("Select");
	JButton cancel		= new JButton("Cancel");
	JButton create		= new JButton("New");
	JButton edit		= new JButton("Edit");
	private int choice  = CANCEL;

	//Looks for a direct relationship from start to end.
	//Then looks at the destination of each direct relationship from start, and follows any 
	//direct relationships from *that* entity (using recursion).  seenEntities is always updated
	// to ensure that loops do not occur
	private List findRelationshipPath(DbEntity start, DbEntity end, Set seenEntities, String indent) {
		// Find matching relationship in the start DbEntity
		List result=new ArrayList();
		//Ensure we never come "back" to this entity
		seenEntities.add(start);
		
		java.util.List list = start.getRelationshipList();
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			DbRelationship db_rel = (DbRelationship)iter.next();
			if (db_rel.getTargetEntity() == end) {
				List aList=new ArrayList();
				aList.add(db_rel);
				result.add(aList);
			} else {
				//Not a direct relationship... recurse, but don't come back to this entity
				if(!seenEntities.contains(db_rel.getTargetEntity())) {
					List deeperRels=this.findRelationshipPath((DbEntity)db_rel.getTargetEntity(), end, seenEntities, indent+"  ");
				
					//deeperRels will be a list of relationship paths that make it from the targetEntity to the end
					//Create a *new* list with the current relationship at the head of each of these lists
					Iterator deeperIt=deeperRels.iterator();
					while(deeperIt.hasNext()) {
						List deeperRelList=(List)deeperIt.next();
						List aList=new ArrayList();
						aList.add(db_rel); //Start of with db_rel...
						aList.addAll(deeperRelList); //..add the rest..
						result.add(aList); //  ... and pop it into the result
					}
				}
			}
		}
		return result;
	}
	
	private void populateRelationshipList(DbEntity startEntity, DbEntity endEntity) {
		relList.addAll(this.findRelationshipPath(startEntity, endEntity, new HashSet(), ""));
	}
	
	public ChooseDbRelationshipDialog(DataMap temp_map, java.util.List db_rel_list
				, DbEntity temp_start, DbEntity temp_end, boolean to_many)
	{
		super(Editor.getFrame(), "Select DbRelationship", true);
		map = temp_map;
		start = temp_start;
		end = temp_end;
		
		this.populateRelationshipList(temp_start, temp_end);
		
		// If DbRelationship does not exist, create it.
		if (null != db_rel_list && db_rel_list.size() > 0) {
			dbRels = new ArrayList(db_rel_list); //Copy
		}
		
		init();
		
		this.pack();
        this.centerWindow();

		select.addActionListener(this);
		cancel.addActionListener(this);
		create.addActionListener(this);
		edit.addActionListener(this);
	}
	
	private boolean relListsSame(List relList1, List relList2) {
		if(relList1.size()!=relList2.size()) {
			return false;
		}
		int i;
		for(i=0; i<relList1.size(); i++) {
			if(!relList1.get(i).equals(relList2.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	
	/** Sets up the graphical components. */
	private void init() {
		getContentPane().setLayout(new BorderLayout());	
		
		relSelect.setBackground(Color.WHITE);		
		
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		RelationshipWrapper sel_item = new RelationshipWrapper((Relationship)null);
		model.addElement(sel_item);
		Iterator iter = relList.iterator();
		while (iter.hasNext()) {
			List db_rels= (List) iter.next();
			RelationshipWrapper wrap = new RelationshipWrapper(db_rels);
			model.addElement(wrap);
			if (dbRels != null &&  relListsSame(db_rels, dbRels)) {	
				sel_item = wrap;
			}
		}		
		model.setSelectedItem(sel_item);
		relSelect.setModel(model);
		
		
		JPanel buttons = PanelFactory.createButtonPanel(new JButton[] {select, cancel, create, edit});		
		
		Component[] left = new Component[] {
			new JLabel("Relationships: "), new JLabel()
		};

		Component[] right = new Component[] {
			relSelect, buttons
		};
		
		JPanel panel = PanelFactory.createForm(left, right, 5, 5, 5, 5);
		getContentPane().add(panel, BorderLayout.CENTER);
	}
	
	
	public java.util.List getDbRelationshipList() {
		if (getChoice() != SELECT && getChoice() != EDIT)
			return null;
		List list = new ArrayList();
		if (dbRels != null)
			list.addAll(dbRels);
		return list;
	}


	public int getChoice() {
		return choice;
	}
		
			
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == this.select) {
			processSelect();
		} else if (src == this.cancel) {
			processCancel();
		} else if (src == this.edit) {
			processEdit();
		} else if (src == this.create) {
			processNew();
		}
	}

	private void processSelect() {
		RelationshipWrapper wrap;
		wrap = (RelationshipWrapper)relSelect.getSelectedItem();
		if (null != wrap && wrap.getRelationshipList() != null)
			dbRels = wrap.getRelationshipList();
		else
			dbRels = null;
	
		choice = SELECT;
		hide();
	}

	private void processEdit() {
		RelationshipWrapper wrap;
		wrap = (RelationshipWrapper)relSelect.getSelectedItem();
		if (null == wrap || wrap.getRelationshipList() == null) {
			JOptionPane.showMessageDialog(Editor.getFrame(),
											"Select the relationship");
			return;
		}
		dbRels = wrap.getRelationshipList();
		choice = EDIT;
		hide();
	}
	
	private void processCancel() {
		dbRels = null;
		choice = CANCEL;
		hide();
	}
	
	private void processNew() {
		dbRels = null;
		choice = NEW;
		hide();
	}
	

}
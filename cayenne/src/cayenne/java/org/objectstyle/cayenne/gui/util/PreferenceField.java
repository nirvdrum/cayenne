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

package org.objectstyle.cayenne.gui.util;

import java.util.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.objectstyle.cayenne.project.CayennePreferences;

public class PreferenceField extends JComboBox {
	static Logger logObj = Logger.getLogger(PreferenceField.class.getName());

	private String key;

	/** 
	 * Values to put in pref field in addition to preference values.
	 * These values are not going to be stored to preference file.
	 */
	private List initValues;

	/** 
	 * Creates PreferenceField that will set preferences 
	 * only on explicit call to <code>storePreferences()</code>.
	 */
	public PreferenceField(String key) {
		this(key, new Vector());
	}

	public PreferenceField(String key, List initValues) {
		this.key = key;
		setEditable(true);
		setSelectedIndex(-1);
		CayennePreferences pref = CayennePreferences.getPreferences();
		this.initValues = initValues;
		Vector v = new Vector(initValues);

		// If has key, append preference values to init values
		if (pref.containsKey(key)) {
			String[] options = pref.getStringArray(key);
			if (options != null) {
				v.addAll(Arrays.asList(options));
			}
		}

		// If any values (init or pref), put them in the model
		if (v.size() > 0) {
			setModel(new DefaultComboBoxModel(v));
		}
		setSelectedItem(null);
	}

	/** 
	 * Returns the text of the selected item or empty string 
	 * if nothing is selected.
	 */
	public String getText() {
		ComboBoxEditor editor = getEditor();
		if (null == editor) {
			return "";
		}
		return editor.getItem().toString();
	}

	/** Adds string to the drop down and selects it. */
	public void setText(String text) {
		getEditor().setItem(text);
	}
	
	/** 
	 * Returns elements of the combo as string array. Doesn't check 
	 * if currently selected item is in the drop down. 
	 */
	public Object[] getItems() {
		DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
		int size = model.getSize();
		Object[] arr = new Object[size];
		for (int i = 0; i < size; i++) {
			arr[i] = model.getElementAt(i);
		}
		return arr;
	}
	

	/** 
	 * Saves the drop down items and newly added item to preferences.
	 */
	public void storePreferences() {
		DefaultComboBoxModel model = (DefaultComboBoxModel)getModel();
		
		// add new item to ComboBox if it is not there already
		String item = getText();
		if (item != null) {
			item = item.trim();
			if (item.length() > 0 && model.getIndexOf(item) < 0) {
				model.addElement(item);
			}
		}

        Object[] items = getItems();
	    Arrays.sort(items);
	    
		CayennePreferences pref = CayennePreferences.getPreferences();
		pref.remove(key);
		
		int size = items.length;
		for(int i = 0; i < size; i++) {			
			// Skip initial items (not from preferences)
			if (initValues.contains(items[i])) {
				continue;
			}

			pref.addProperty(key, items[i]);
		}
		
		model = new DefaultComboBoxModel(items);
		model.setSelectedItem(item);
	}

	public Document getDocument() {
		return ((JTextComponent) getEditor().getEditorComponent())
			.getDocument();
	}
}
package org.objectstyle.cayenne.gui.util;
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

import java.awt.Component;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.objectstyle.util.Preferences;

public class PreferenceField extends JComboBox
{
    static Logger logObj = Logger.getLogger(PreferenceField.class.getName());

	private String key;
	/** Values to put in pref field in addition to preference values.
	 *  These values are not going to be stored to preference file.*/
	private List initValues;
	
	/** Sets preference only on explicit call to storePreferences().*/
	public PreferenceField(String temp_key) {
		this(temp_key, false);
	}

	/** Allows storing preferences on focus lost.
	  * @param temp_key Key under which preferences is retrieved or stored
	  * @param set_on_focus If true, stores preferences each time focus is lost.
	  */
	public PreferenceField(String temp_key, boolean set_on_focus) {
		this(temp_key, set_on_focus, new Vector());
	}
	
	public PreferenceField(String temp_key, boolean set_on_focus
						 , List init_values)
	{
		key = temp_key;
		setEditable(true);
		setSelectedIndex(-1);
		Preferences pref = Preferences.getPreferences();
		initValues = init_values;
		Vector v = new Vector(init_values);
		// If has key, append preference values to init values
		if (pref.containsKey(key)) {
			String[] options = pref.getStringArray(key);
			if (options != null ) {
				v.addAll(Arrays.asList(options));
			}
		}
		// If any values (init or pref), put them in the model
		if (v.size() > 0) {
			DefaultComboBoxModel model = new DefaultComboBoxModel(v);
			setModel(model);
		}
		setSelectedItem(null);
	}
	
	/** Return the text of the selected item or "" if nothing is selected.*/
	public String getText() {
		ComboBoxEditor editor = getEditor();
		if (null == editor) {
			return "";
		}
		return editor.getItem().toString();
	}

	/** Gets elements of the combo as string array. 
	  * Doesn't check if currently selected item is in the drop down. */
	private Vector getItemsVector() {
		DefaultComboBoxModel model = (DefaultComboBoxModel)getModel();
		int size = model.getSize();
		Vector arr = new Vector();
		for (int i = 0; i < size; i++) {
			arr.add(model.getElementAt(i));
		}
		return arr;
	}

	/** Ads string to the drop down and selects it. */
	public void setText(String text) {
		ComboBoxEditor editor = getEditor();
		editor.setItem(text);
	}	
	
	/** Saves the drop down items and newly added item to preferences.*/
	public void storePreferences() {
		Vector items = getItemsVector();
		String item = getText();
		if (item != null) {
			item = item.trim();
			if (item.length() > 0 && !items.contains(item))
				items.add(item);
		}
		Collections.sort(items);
		Preferences pref = Preferences.getPreferences();
		pref.remove(key);
		Iterator iter = items.iterator();
		
	    StringBuffer buf;
	    buf = new StringBuffer("PreferenceField::storePreferences()"
								+", key "+ key + ":\n");
	    while (iter.hasNext()) {
		    String str = (String)iter.next();
		    // Skip initial items (not from preferences)
		    if (initValues.contains(str))
		    	continue;
		    pref.addProperty(key, str);
		    buf.append(str + "\n");
	    }// End while
	    logObj.finer(buf.toString());
		DefaultComboBoxModel model = new DefaultComboBoxModel(items);
		model.setSelectedItem(item);
		setModel(model);
	}
	
	public Document getDocument() {
		Component comp = getEditor().getEditorComponent();
		if (!(comp instanceof JTextComponent)) {
			throw new ClassCastException();
		}
		JTextComponent text_comp = (JTextComponent)comp;
		return text_comp.getDocument();
	}
}
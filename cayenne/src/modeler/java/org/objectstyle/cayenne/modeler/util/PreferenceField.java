/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.modeler.util;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.util.Util;

public class PreferenceField extends JComboBox {
    private String key;

    /** 
     * Values to put in pref field in addition to preference values.
     * These values are not going to be stored to preference file.
     */
    private Collection initValues;

    /** 
     * Creates PreferenceField that will set preferences 
     * only on explicit call to <code>storePreferences()</code>.
     */
    public PreferenceField(String key) {
        this(key, Collections.EMPTY_LIST);
    }

    public PreferenceField(String key, Collection initValues) {
        this.key = key;

        setRenderer(new Renderer(40));
        setEditable(true);
        setSelectedIndex(-1);
        ModelerPreferences pref = ModelerPreferences.getPreferences();
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
        DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();

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

        ModelerPreferences pref = ModelerPreferences.getPreferences();
        pref.remove(key);

        int size = items.length;
        for (int i = 0; i < size; i++) {
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

    class Renderer extends DefaultListCellRenderer {
    	protected int size;
    	
    	public Renderer(int size) {
    		this.size = size;
    	}
    	
        /**
         * Will trim the value to fit defined size.
         */
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

            if (size > 0 && !(value instanceof Icon) && value != null) {
                value = Util.prettyTrim(value.toString(), size);
            }

            return super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus);
        }

    }
}

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
package org.objectstyle.cayenne.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.FileSystemViewDecorator;
import org.objectstyle.cayenne.map.DataMap;

/** 
 * Detail view of the DataNode and DataSourceInfo
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DataMapDetailView
	extends JPanel
	implements DocumentListener, ActionListener, DataMapDisplayListener, ItemListener {

	static Logger logObj = Logger.getLogger(DataMapDetailView.class.getName());

	Mediator mediator;

	JLabel nameLabel;
	JTextField name;
	String oldName;

	JLabel locationLabel;
	JTextField location;
	JButton fileBtn;
	protected JPanel fileChooser;
	protected JPanel depMapsPanel;

	protected HashMap mapLookup = new HashMap();

	/** Cludge to prevent marking map as dirty during initial load. */
	private boolean ignoreChange;

	public DataMapDetailView(Mediator mediator) {
		super();
		this.mediator = mediator;
		mediator.addDataMapDisplayListener(this);
		// Create and layout components
		init();
		// Add listeners
		location.getDocument().addDocumentListener(this);
		name.getDocument().addDocumentListener(this);
		fileBtn.addActionListener(this);
	}

	private void init() {
		BorderLayout layout = new BorderLayout();
		this.setLayout(layout);
		nameLabel = new JLabel("Data map name: ");
		name = new JTextField(20);
		locationLabel = new JLabel("Location: ");
		location = new JTextField(25);
		location.setEditable(false);
		fileBtn = new JButton("...");

		fileChooser = this.formatFileChooser(location, fileBtn);

		Component[] leftComp = new Component[2];
		leftComp[0] = nameLabel;
		leftComp[1] = locationLabel;

		Component[] rightComp = new Component[2];
		rightComp[0] = name;
		rightComp[1] = fileChooser;

		JPanel temp = PanelFactory.createForm(leftComp, rightComp, 5, 5, 5, 5);
		add(temp, BorderLayout.NORTH);
	}

	private JPanel formatFileChooser(JTextField fld, JButton btn) {
		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout());
		panel.add(fld, BorderLayout.CENTER);
		panel.add(btn, BorderLayout.EAST);

		return panel;
	}

	public void insertUpdate(DocumentEvent e) {
		textFieldChanged(e);
	}
	public void changedUpdate(DocumentEvent e) {
		textFieldChanged(e);
	}
	public void removeUpdate(DocumentEvent e) {
		textFieldChanged(e);
	}

	private void textFieldChanged(DocumentEvent e) {
		if (ignoreChange) {
			return;
		}

		DataMap map = mediator.getCurrentDataMap();
		DataMapEvent event;
		if (e.getDocument() == name.getDocument()) {
			String new_name = name.getText();
			// If name hasn't changed, do nothing
			if (oldName != null && new_name.equals(oldName))
				return;
			map.setName(new_name);
			event = new DataMapEvent(this, map, oldName);
			mediator.fireDataMapEvent(event);
			oldName = new_name;
		} else if (e.getDocument() == location.getDocument()) {
			if (map.getLocation().equals(location.getText()))
				return;
			map.setLocation(location.getText());
			event = new DataMapEvent(this, map);
			mediator.fireDataMapEvent(event);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == fileBtn) {
			selectMapLocation();
		}
	}

	private void selectMapLocation() {
		DataMap map = mediator.getCurrentDataMap();
		try {
			// Get the project file name (always cayenne.xml)
			File file = null;
			String proj_dir_str = mediator.getConfig().getProjDir();
			File proj_dir = null;
			if (proj_dir_str != null)
				proj_dir = new File(proj_dir_str);
			JFileChooser fc;
			FileSystemViewDecorator file_view;
			file_view = new FileSystemViewDecorator(proj_dir);
			fc = new JFileChooser(file_view);
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
			fc.setDialogTitle("Data Map location");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (null != proj_dir)
				fc.setCurrentDirectory(proj_dir);
			int ret_code = fc.showSaveDialog(this);
			if (ret_code != JFileChooser.APPROVE_OPTION)
				return;
			file = fc.getSelectedFile();

			// Create new file
			if (!file.exists())
				file.createNewFile();
			// Determine and set new data map location
			String new_file_location = file.getAbsolutePath();
			String relative_location;
			// If it is set, use path striped of proj dir and following separator
			// If proj dir not set, use absolute location.
			if (proj_dir_str == null)
				relative_location = new_file_location;
			else
				relative_location =
					new_file_location.substring(proj_dir_str.length() + 1);
			map.setLocation(relative_location);
			location.setText(relative_location);
			// Map location changed
			mediator.fireDataMapEvent(new DataMapEvent(this, map));

		} catch (Exception e) {
			logObj.warn("Error setting map location.", e);
		}
	}

	public void currentDataMapChanged(DataMapDisplayEvent e) {
		DataMap map = e.getDataMap();
		if (null == map) {
			return;
		}

		oldName = map.getName();
		ignoreChange = true;
		name.setText(oldName);
		location.setText(map.getLocation());
		ignoreChange = false;

		if (depMapsPanel != null) {
			remove(depMapsPanel);
			depMapsPanel = null;
		}

		mapLookup.clear();

		// add a list of dependencies
		java.util.List maps = mediator.getCurrentDataDomain().getMapList();

		if (maps.size() < 2) {
			return;
		}

		Component[] leftComp = new Component[maps.size() - 1];
		Component[] rightComp = new Component[maps.size() - 1];

		Iterator it = maps.iterator();
		int i = 0;
		while (it.hasNext()) {
			DataMap nextMap = (DataMap) it.next();
			if (nextMap != map) {
				JCheckBox check = new JCheckBox();
				JLabel label = new JLabel(nextMap.getName());

				check.addItemListener(this);
				if (nextMap.isDependentOn(map)) {
					check.setEnabled(false);
					label.setEnabled(false);
				}

				if (map.isDependentOn(nextMap)) {
					check.setSelected(true);
				}

				mapLookup.put(check, nextMap);
				leftComp[i] = label;
				rightComp[i] = check;
				i++;
			}
		}

		depMapsPanel = PanelFactory.createForm(leftComp, rightComp, 5, 5, 5, 5);
		depMapsPanel.setBorder(
			BorderFactory.createTitledBorder("Depends on DataMaps"));
		add(depMapsPanel, BorderLayout.CENTER);
		validate();
	}

	/**
	 * @see java.awt.event.ItemListener#itemStateChanged(ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		JCheckBox src = (JCheckBox) e.getSource();
		DataMap map = (DataMap) mapLookup.get(src);

		if (map != null) {
			DataMap curMap = mediator.getCurrentDataMap();
			if (e.getStateChange() == ItemEvent.SELECTED) {
				curMap.addDependency(map);
			} else if (e.getStateChange() == ItemEvent.DESELECTED) {
				curMap.removeDependency(map);
			}

			mediator.fireDataMapEvent(new DataMapEvent(this, curMap));
		}
	}
}

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

package org.objectstyle.cayenne.modeler;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.objectstyle.cayenne.modeler.action.CayenneAction;
import org.objectstyle.cayenne.modeler.util.ModelerStrings;


/** 
 * Displays the information about the licnese of Cayenne
 * and about CayenneModeler.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class AboutDialog extends CayenneDialog implements ActionListener {
	private static final int WIDTH = 523;
	private static final int HEIGHT = 450;
	private static String licenseString;

	private boolean view_info = true;

	JLabel image;
	JButton ok = new JButton("Ok");
	JButton view = new JButton("View License");

	JPanel centerPanel = new JPanel();
	CardLayout cardLayout = new CardLayout();
	JPanel infoPanel = new JPanel();
	JTextArea licenceText = new JTextArea();

	public AboutDialog(Editor frame) {
		super(frame, "About CayenneModeler", true);
		init();

		ok.addActionListener(this);
		view.addActionListener(this);

		setSize(WIDTH, HEIGHT);
		setResizable(false);
		this.centerWindow();
		this.getContentPane().doLayout();
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	/** 
	 * Reads Cayenne license from cayenne.jar file and returns it as a string.
	 */
	public static String getLicenseString() {
		if (licenseString == null) {
			BufferedReader in = null;
			try {
				InputStream licenseIn =
					AboutDialog.class.getClassLoader().getResourceAsStream(
						"META-INF/LICENSE");

				if (licenseIn != null) {
					in = new BufferedReader(new InputStreamReader(licenseIn));
					String line = null;
					StringBuffer buf = new StringBuffer();

					while ((line = in.readLine()) != null) {
						// strip comments
						if (line.startsWith("/*") || line.startsWith(" */")) {
							continue;
						}

						if (line.startsWith(" *")) {
							line = line.substring(2);
						}

						buf.append(line).append('\n');
					}

					licenseString = buf.toString();
				}

			} catch (IOException ioex) {
				// ignoring
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ioex) {
						// ignoring
					}
				}
			}

			// if license is not initialized for whatever reason,
			// send them to the website
			if (licenseString == null) {
				licenseString =
					"Latest Cayenne license can be found at http://objectstyle.org/cayenne/";
			}
		}

		return licenseString;
	}

	/**
	 * Builds CayenneModeler info string */
	public String getInfoString() {
		String infoString = ModelerStrings.getString("cayenne.modeler.about.info");
		String version = ModelerStrings.getString("cayenne.version");
		String versionStr = (version != null) ? "Version: " + version : "";

		String buildDate = ModelerStrings.getString("cayenne.build.date");
		String buildDateStr =
			(buildDate != null) ? " (" + buildDate + ")": "";
		return infoString
			+ "<font size='-2' face='Arial,Helvetica'>"
			+ versionStr
			+ buildDateStr
			+ "</font>";
	}

	/** Set up the graphical components. */
	private void init() {
		GridBagLayout layout = new GridBagLayout();
		getContentPane().setLayout(layout);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 3;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 100;
		c.weighty = 100;
		c.ipadx = 7;

		Border border = BorderFactory.createEtchedBorder();

		JPanel image_pane = new JPanel(new FlowLayout(FlowLayout.CENTER));

		ClassLoader cl = AboutDialog.class.getClassLoader();
		URL url = cl.getResource(CayenneAction.RESOURCE_PATH + "logo.jpg");
		ImageIcon logo_icon = new ImageIcon(url);
		image = new JLabel(logo_icon);
		image.setBorder(border);
		image_pane.add(image, BorderLayout.CENTER);
		layout.setConstraints(image_pane, c);
		getContentPane().add(image_pane);

		JEditorPane infoArea = new JEditorPane("text/html", getInfoString());
		infoArea.setBackground(getContentPane().getBackground());
		infoArea.setEditable(false);
		// popup hyperlinks
		infoArea.addHyperlinkListener(this);

		infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		infoPanel.setBorder(border);
		infoPanel.add(infoArea);

		licenceText.setText(getLicenseString());
		licenceText.setEditable(false);
		licenceText.setLineWrap(true);
		licenceText.setWrapStyleWord(true);
		JScrollPane temp = new JScrollPane(licenceText);
		temp.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		centerPanel.setLayout(cardLayout);
		centerPanel.add(infoPanel, "Info");
		centerPanel.add(temp, "License");
		c.gridy = 3;
		c.gridheight = 5;
		layout.setConstraints(centerPanel, c);
		getContentPane().add(centerPanel);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(ok);
		buttonPanel.add(view);
		c.gridy = 8;
		c.gridheight = 1;
		layout.setConstraints(buttonPanel, c);
		getContentPane().add(buttonPanel);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == ok) {
			setVisible(false);
			dispose();
		} else {
			if (view_info) {
				view.setText("View Info");
				cardLayout.show(centerPanel, "License");
				view_info = false;
			} else {
				view.setText("View License");
				cardLayout.show(centerPanel, "Info");
				view_info = true;
			}
		}
	}
}
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;

import org.objectstyle.cayenne.gui.util.GUIUtil;

/**
 * Displays unexpected CayenneModeler exceptions.
 * 
 * @author Andrei Adamchik
 */
public class ErrorDebugDialog extends CayenneDialog implements ActionListener {
	protected JButton close;
	protected JTextArea exText = new JTextArea();
	protected Throwable throwable;

	/**
	 * Constructor for ErrorDebugDialog.
	 * @param owner
	 * @throws HeadlessException
	 */
	public ErrorDebugDialog(Editor owner, Throwable throwable)
		throws HeadlessException {
		super(owner, "CayenneModeler Error", true);
		init();

		setThrowable(throwable);
	}

	protected void init() {
		setResizable(false);

		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());

		// info area
		JEditorPane infoText =
			new JEditorPane(
				"text/html",
				"<b><font face='Arial,Helvetica' size='+1' color='red'>CayenneModeler Error</font></b><br>"
					+ "<font face='Arial,Helvetica' size='-1'>Please copy the message below and "
					+ "report this error by going to <br>"
					+ "<a href='http://sourceforge.net/tracker/?func=add&group_id=48132&atid=452068'>"
					+ "http://sourceforge.net/tracker/?func=add&group_id=48132&atid=452068</a></font>");
		infoText.setBackground(pane.getBackground());
		infoText.setEditable(false);
		// popup hyperlinks
		infoText.addHyperlinkListener(this);

		JPanel infoPanel = new JPanel();
		infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		infoPanel.add(infoText);
		pane.add(infoPanel, BorderLayout.NORTH);

		// exception area
		exText.setEditable(false);
		exText.setLineWrap(true);
		exText.setWrapStyleWord(true);
		exText.setRows(16);
		exText.setColumns(40);
		JScrollPane exScroll =
			new JScrollPane(
				exText,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel exPanel = new JPanel();
		exPanel.setLayout(new BorderLayout());
		exPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		exPanel.add(exScroll, BorderLayout.CENTER);
		pane.add(exPanel, BorderLayout.CENTER);

		// buttons
		close = new JButton("Close");
		close.addActionListener(this);
		pane.add(
			GUIUtil.createButtonPanel(new JButton[] { close }),
			BorderLayout.SOUTH);

		// prepare to display
		this.pack();
		GUIUtil.centerWindow(this);
	}

	protected void setThrowable(Throwable throwable) {
		this.throwable = throwable;

		String text = null;
		if (throwable != null) {
			StringWriter str = new StringWriter();
			PrintWriter out = new PrintWriter(str);

			// first add extra diagnostics
			String version = getParentEditor().getProperty("cayenne.version");
			version = (version != null) ? version : "(unknown)";

			String buildDate =
				getParentEditor().getProperty("cayenne.build.date");
			buildDate = (buildDate != null) ? buildDate : "(unknown)";

			out.println("CayenneModeler Info");
			out.println("Version: " + version);
			out.println("Build Date: " + buildDate);
			out.println("Exception: ");
			out.println("=================================");
			throwable.printStackTrace(out);

			try {
				out.close();
				str.close();
			} catch (IOException ioex) {
				// this should never happen
			}
			text = str.getBuffer().toString();
		}

		exText.setText(text);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == close) {
			this.dispose();
		}
	}
}

package org.objectstyle.cayenne.gui;
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
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/** Used to select schema of the database. */
public class AboutDialog extends JDialog
implements ActionListener
{
	private static final int WIDTH  = 523;
	private static final int HEIGHT = 450;
	
	private boolean view_info = true;
	
	JLabel image;
	JButton ok		= new JButton("Ok");
	JButton view	= new JButton("View License");
	
	JPanel centerPanel = new JPanel();
	CardLayout cardLayout = new CardLayout();
    JPanel infoPanel = new JPanel();
	JTextArea licenceText = new JTextArea();
	
	String licence =  "The ObjectStyle Group Software License, Version 1.0 \n"
 + "\n"
 + " Copyright (c) 2002 The ObjectStyle Group \n"
 + " and individual authors of the software.  \nAll rights reserved."
 + "\n"
 + " Redistribution and use in source and binary forms, with or without"
 + " modification, are permitted provided that the following conditions"
 + " are met\n"
 + "\n"
 + " 1. Redistributions of source code must retain the above copyright"
 + " notice, this list of conditions and the following disclaimer. \n"
 + "\n"
 + " 2. Redistributions in binary form must reproduce the above copyright"
 + " notice, this list of conditions and the following disclaimer in"
 + " the documentation and/or other materials provided with the"
 + " distribution.\n"
 + "\n"
 + " 3. The end-user documentation included with the redistribution, if"
 + " any, must include the following acknowlegement:  \n"
 + "      \"This product includes software developed by the \n"
 + "        ObjectStyle Group (http://objectstyle.org/)\".\n"
 + "    Alternately, this acknowlegement may appear in the software itself,\n"
 + "    if and wherever such third-party acknowlegements normally appear.\n"
 + "\n"
 + " 4. The names \"ObjectStyle Group\" and \"Cayenne\""
 + " must not be used to endorse or promote products derived"
 + " from this software without prior written permission. For written"
 + " permission, please contact andrus@objectstyle.org.\n"
 + "\n"
 + " 5. Products derived from this software may not be called \"ObjectStyle\""
 + " nor may \"ObjectStyle\" appear in their names without prior written"
 + " permission of the ObjectStyle Group.\n"
 + "\n"
 + "THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED"
 + " WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES"
 + " OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE"
 + " DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR"
 + " ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,"
 + " SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT"
 + " LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF"
 + " USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND"
 + " ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,"
 + " OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT"
 + " OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF"
 + " SUCH DAMAGE.\n"
 + "\n"
 + "This software consists of voluntary contributions made by many"
 + " individuals on behalf of the ObjectStyle Group. For more"
 + " information on the ObjectStyle Group, please see"
 + " <http://objectstyle.org/>.";

	String info = 
   		"Copyright (c) 2002 The ObjectStyle Group" 
 		+ " (http://www.objectstyle.org)\n"
		 + "and individual authors of the software."
 		+ " All rights reserved.\n\n"
 		+ "This software is distributed free of charge  under\n"
 		+" the terms of The ObjectStyle Group license.\n"
 		+ "Click \"View License\" for more details.\n\n"
 		+ "Version 0.7 (alpha).";


	public AboutDialog(JFrame frame)
	{
		super(frame, "About Cayenne", true);
		System.out.println("In AboutDialog");
		init();
		
		ok.addActionListener(this);
		view.addActionListener(this);
		
		setSize(WIDTH, HEIGHT);
		Point point = frame.getLocationOnScreen();
		int width = frame.getWidth();
		int x = (width - WIDTH)/2;
		int height = frame.getHeight();
		int y = (height - HEIGHT)/2;
		
		point.setLocation(point.x + x, point.y + y);
		this.setLocation(point);
		this.getContentPane().doLayout();
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);		
	}// End AboutDialog
	
	/** Set up the graphical components. */
	private void init() {
		GridBagLayout layout = new GridBagLayout();
		getContentPane().setLayout(layout);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx	= 0;
		c.gridy	= 0;
		c.gridwidth = 1;
		c.gridheight = 3;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 100;
		c.weighty = 100;
		c.ipadx = 7;
		
        Border border = BorderFactory.createEtchedBorder();

		JPanel image_pane = new JPanel(new FlowLayout(FlowLayout.CENTER));
    	String path = "org/objectstyle/gui/";    	
    	ClassLoader cl = AboutDialog.class.getClassLoader();
    	URL url = cl.getResource(path + "images/logo.jpg");
        ImageIcon logo_icon = new ImageIcon(url);
        image = new JLabel(logo_icon);
        image.setBorder(border);
        image_pane.add(image, BorderLayout.CENTER);
        layout.setConstraints(image_pane, c);
        getContentPane().add(image_pane);
  
  		
        infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JTextArea infoArea = new JTextArea(info);
        infoPanel.setBorder(border);
		infoArea.setEditable(false);
		infoArea.setBackground(infoPanel.getBackground());
        infoPanel.add(infoArea);

		licenceText.setText(licence);
		licenceText.setEditable(false);
		licenceText.setLineWrap(true);
		licenceText.setWrapStyleWord(true);
		JScrollPane temp = new JScrollPane(licenceText);
		temp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		centerPanel.setLayout(cardLayout);
   		centerPanel.add(infoPanel, "Info");
		centerPanel.add(temp, "Licence");
		c.gridy	= 3;
		c.gridheight = 5;
        layout.setConstraints(centerPanel, c);
        getContentPane().add(centerPanel);
  		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(ok);
		buttonPanel.add(view);
		c.gridy	= 8;
		c.gridheight = 1;
        layout.setConstraints(buttonPanel, c);
        getContentPane().add(buttonPanel);
	}// End init()
	

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == ok) {
			setVisible(false);
			dispose();
		} else {
			if (view_info) {
				view.setText("View Info");
				cardLayout.show(centerPanel, "Licence");
				view_info = false;
			} else {
				view.setText("View Licence");
				cardLayout.show(centerPanel, "Info");
				view_info = true;
			}
		}
	}
}
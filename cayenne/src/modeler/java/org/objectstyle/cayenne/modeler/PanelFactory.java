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

import java.awt.*;

import javax.swing.*;

/** 
 * Utility methods for laying out components on the panels.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class PanelFactory {

	/** 
	 * Creates a borderless button that can be used
	 * as a clickable label.
	 */
	public static JButton createLabelButton(String text) {
		JButton but = new JButton(text);
		but.setBorderPainted(false);
		but.setHorizontalAlignment(SwingConstants.LEFT);
		but.setFocusPainted(false);
		but.setMargin(new Insets(0, 0, 0, 0));
		but.setBorder(null);
		return but;
	}

	/** 
	 * Creates and returns a panel with right-centered buttons.
	 */
	public static JPanel createButtonPanel(JButton[] buttons) {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 7));
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		for (int i = 0; i < buttons.length; i++) {
			panel.add(buttons[i]);
		}

		return panel;
	}

	public static JPanel createJDK13Form(
		Component[] leftComponents,
		Component[] rightComponents,
		int xSpacing,
		int ySpacing,
		int xPad,
		int yPad) {

		JPanel panel = new JPanel();

		panel.setBorder(
			BorderFactory.createEmptyBorder(
				xSpacing,
				ySpacing,
				xSpacing,
				ySpacing));
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);

		GridBagConstraints cstr = new GridBagConstraints();
		cstr.ipadx = xPad;
		cstr.ipady = yPad;
		cstr.fill = GridBagConstraints.HORIZONTAL;
		cstr.insets = new Insets(1, 1, 1, 1);

		int numRows = Math.max(leftComponents.length, rightComponents.length);
		for (int i = 0; i < numRows; i++) {
			cstr.gridwidth = 1;
			cstr.anchor = GridBagConstraints.EAST;
			gbl.setConstraints(leftComponents[i], cstr);
			panel.add(leftComponents[i]);

			cstr.gridwidth = GridBagConstraints.REMAINDER;
			cstr.anchor = GridBagConstraints.WEST;
			gbl.setConstraints(rightComponents[i], cstr);
			panel.add(rightComponents[i]);
		}

		return panel;
	}

	/** 
	 * Create panel with aligned labels on the right and fields on the left.
	 * Panel uses SpringLayout.
	 */
	public static JPanel createForm(
		Component[] leftComponents,
		Component[] rightComponents,
		int initialX,
		int initialY,
		int xPad,
		int yPad) {
		SpringLayout layout = new SpringLayout();
		int numRows = Math.max(leftComponents.length, rightComponents.length);

		// The constant springs we'll use to enforce spacing.
		Spring xSpring = Spring.constant(initialX);
		Spring ySpring = Spring.constant(initialY);
		Spring xPadSpring = Spring.constant(xPad);
		Spring yPadSpring = Spring.constant(yPad);
		Spring negXPadSpring = Spring.constant(-xPad);

		// Create the container and add the components to it.
		JPanel parent = new JPanel(layout);
		for (int i = 0; i < numRows; i++) {
			parent.add(leftComponents[i]);
			parent.add(rightComponents[i]);
		}

		// maxEastSpring will contain the highest min/pref/max values
		// for the right edges of the components in the first column
		// (i.e. the largest X coordinate in a first-column component).
		// We use layout.getConstraint instead of layout.getConstraints
		// (layout.getConstraints(comp).getConstraint("East"))
		// because we need a proxy -- not the current Spring.
		// Otherwise, it won't take the revised X position into account
		// for the initial layout.
		Spring maxEastSpring = layout.getConstraint("East", leftComponents[0]);
		for (int row = 1; row < numRows; row++) {
			maxEastSpring =
				Spring.max(
					maxEastSpring,
					layout.getConstraint("East", leftComponents[row]));
		}

		// Lay out each pair. The left column's x is constrained based on
		// the passed x location. The y for each component in the left column
		// is the max of the previous pair's height. In the right column, x is
		// constrained by the max width of the left column (maxEastSpring),
		// y is constrained as in the left column, and the width is
		// constrained to be the x location minus the width of the
		// parent container. This last constraint makes the right column fill
		// all extra horizontal space.
		SpringLayout.Constraints lastConsL = null;
		SpringLayout.Constraints lastConsR = null;
		Spring parentWidth = layout.getConstraint("East", parent);
		Spring rWidth = null;
		Spring maxHeightSpring = null;
		Spring rX = Spring.sum(maxEastSpring, xPadSpring); //right col location
		Spring negRX = Spring.minus(rX); //negative of rX

		for (int row = 0; row < numRows; row++) {
			SpringLayout.Constraints consL =
				layout.getConstraints(leftComponents[row]);
			SpringLayout.Constraints consR =
				layout.getConstraints(rightComponents[row]);

			consL.setX(xSpring);
			consR.setX(rX);

			if (row == 0)
				rWidth = consR.getWidth();
			else
				rWidth = Spring.max(rWidth, consR.getWidth());
			consR.setWidth(
				Spring.sum(Spring.sum(parentWidth, negRX), negXPadSpring));
			Spring height = Spring.max(consL.getHeight(), consR.getHeight());
			if (row == 0) {
				consL.setY(ySpring);
				consR.setY(ySpring);
				maxHeightSpring = Spring.sum(ySpring, height);
			} else { // row > 0
				Spring y =
					Spring.sum(
						Spring.max(
							lastConsL.getConstraint("South"),
							lastConsR.getConstraint("South")),
						yPadSpring);

				consL.setY(y);
				consR.setY(y);
				maxHeightSpring =
					Spring.sum(yPadSpring, Spring.sum(maxHeightSpring, height));
			}
			Dimension dimL = leftComponents[row].getPreferredSize();
			Dimension dimR = rightComponents[row].getPreferredSize();
			int dim_height = Math.max(dimL.height, dimR.height);
			consL.setHeight(Spring.constant(dim_height));
			consR.setHeight(Spring.constant(dim_height));
			lastConsL = consL;
			lastConsR = consR;
		} // end of for loop

		// Wire up the east/south of the container so that the its preferred
		// size is valid.  The east spring is the distance to the right
		// column (rX) + the right component's width (rWidth) + the final
		// padding (xPadSpring).
		// The south side is maxHeightSpring + the final padding (yPadSpring).

		SpringLayout.Constraints consParent = layout.getConstraints(parent);

		consParent.setConstraint(
			"East",
			Spring.sum(rX, Spring.sum(rWidth, xPadSpring)));
		consParent.setConstraint(
			"South",
			Spring.sum(maxHeightSpring, yPadSpring));

		return parent;
	}

	/** 
	 * Creates panel with table within scroll panel and buttons in the bottom.
	 * Also sets the resizing and selection policies of the table to
	 * AUTO_RESIZE_OFF and SINGLE_SELECTION respectively.
	 */
	public static JPanel createTablePanel(JTable table, JButton[] buttons) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(5, 5));

		// Create table with two columns and no rows.
		table.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// Panel to add space between table and EAST/WEST borders
		panel.add(new JScrollPane(table), BorderLayout.CENTER);

		// Add Add and Remove buttons
		if (buttons != null) {
			panel.add(createButtonPanel(buttons), BorderLayout.SOUTH);
		}
		return panel;
	}

	/** Creates panel with table within scroll panel and buttons in the bottom.
	  * Also sets the resizing and selection policies of the table to
	  * AUTO_RESIZE_OFF and SINGLE_SELECTION respectively.*/
	public static JPanel createTablePanel(
		JTable table,
		JComponent[] components,
		JButton[] buttons) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(5, 5));

		JPanel temp_panel = new JPanel(new BorderLayout());

		// Create table with two columns and no rows.
		table.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane scroll_pane = new JScrollPane(table);
		temp_panel.add(scroll_pane, BorderLayout.CENTER);

		for (int i = 0; i < components.length; i++) {
			JPanel temp = new JPanel(new BorderLayout());
			temp.add(temp_panel, BorderLayout.CENTER);
			temp.add(components[i], BorderLayout.SOUTH);
			temp_panel = temp;
		}

		panel.add(temp_panel, BorderLayout.CENTER);

		if (buttons != null) {
			panel.add(createButtonPanel(buttons), BorderLayout.SOUTH);
		}
		return panel;
	}

}

package org.objectstyle.cayenne.modeler.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import org.objectstyle.cayenne.modeler.Editor;

public class YesNoToAllDialog extends JDialog implements ActionListener
{
	public static final int UNDEFINED 	= -1;
	public static final int YES 		= 0;
	public static final int YES_TO_ALL 	= 1;
	public static final int NO  		= 2;
	public static final int NO_TO_ALL 	= 3;
	public static final int CANCEL 		= 4;
	
	private static final int WIDTH  = 380;
	private static final int HEIGHT = 150;

	JButton yes = new JButton("Yes");
	JButton yesToAll = new JButton("Yes to all");
	JButton no = new JButton("No");
	JButton noToAll = new JButton("No to all");
	JButton cancel = new JButton("Stop");
	
	private int status = CANCEL;

	public YesNoToAllDialog(String title, String msg) {
		super(Editor.getFrame(), title != null ? title : "Cayenne Db Import", true);
		
		getContentPane().setLayout(new BorderLayout());
		
        JTextArea infoArea = new JTextArea(msg);
        Border border = BorderFactory.createBevelBorder(3);
        infoArea.setBorder(border);
		infoArea.setEditable(false);
		infoArea.setLineWrap(true);
		infoArea.setBackground(getContentPane().getBackground());
        getContentPane().add(infoArea, BorderLayout.CENTER);
        
        JPanel temp = new JPanel(new FlowLayout(FlowLayout.CENTER));
        temp.add(yes);
        temp.add(yesToAll);
        temp.add(no);
        temp.add(noToAll);
        temp.add(cancel);
        getContentPane().add(temp, BorderLayout.SOUTH);
        
        yes.addActionListener(this);
        yesToAll.addActionListener(this);
        no.addActionListener(this);
        noToAll.addActionListener(this);
        cancel.addActionListener(this);
        
        Point point;

		if (Editor.getFrame() != null) {
			point = Editor.getFrame().getLocationOnScreen();
			int width = Editor.getFrame().getWidth();
			int x = (width - WIDTH)/2;
			int height = Editor.getFrame().getHeight();
			int y = (height - HEIGHT)/2;
			point.setLocation(point.x + x, point.y + y);
		} else {        
        	point = new Point(250, 250);
        }
		setLocation(point);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.setSize(WIDTH, HEIGHT);
        setVisible(true);
	}
	
	public int getStatus() {
		return status;
	}
	
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == yes) {
			status = YES;
		} else if (src == yesToAll) {
			status = YES_TO_ALL;
		} else if (src == no) {
			status = NO;
		} else if (src == noToAll) {
			status = NO_TO_ALL;
		} else if (src == cancel) {
			status = CANCEL;
		}
		setVisible(false);
	}
}
	
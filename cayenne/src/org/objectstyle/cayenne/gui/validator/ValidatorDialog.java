package org.objectstyle.cayenne.gui.validator;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.Editor;


public class ValidatorDialog extends JDialog
implements ListSelectionListener, ActionListener
{
	Mediator mediator;
	private JFrame frame;
	private Vector errMsg;
	
	JList messages;
	JButton closeBtn;
	
	
	/** 
	  * @param err_msg list or ErrorMsg messages.
	  * @param ERROR or WARNING Determines message at the top. */
	public ValidatorDialog(JFrame temp_frame, Mediator temp_mediator
						, Vector err_msg, int severity)
	{
		super(temp_frame
			, severity==ErrorMsg.ERROR 
				? "There are errors - cannot continue"
			 	: "Finished. There are warnings"
			 , false);
		frame = temp_frame;
		mediator = temp_mediator;
		errMsg = err_msg;
		init();
		
		messages.addListSelectionListener(this);
		closeBtn.addActionListener(this);
		
		setSize(380, 150);
		this.setVisible(true);		
		Point point = Editor.getFrame().getLocationOnScreen();
		this.setLocation(point);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}
	
	private void init()
	{
		getContentPane().setLayout(new BorderLayout());
		messages = new JList(errMsg.toArray());
		messages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION );
		getContentPane().add(messages, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		closeBtn = new JButton("Close");
		panel.add(closeBtn);
		getContentPane().add(panel, BorderLayout.SOUTH);
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		ErrorMsg obj = (ErrorMsg)messages.getSelectedValue();
		obj.displayField(mediator, frame);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.setVisible(false);
		this.dispose();
	}
}
package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 * @author Andrei Adamchik
 */
public class PreferenceDialogView extends JDialog {

    protected JSplitPane split;
    protected JList list;
    protected CardLayout detailLayout;
    protected Container detailPanel;
    protected JButton cancelButton;
    protected JButton saveButton;

    public PreferenceDialogView() {
        this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.list = new JList();
        this.detailLayout = new CardLayout();
        this.detailPanel = new JPanel(detailLayout);
        this.saveButton = new JButton("Save");
        this.cancelButton = new JButton("Cancel");

        // assemble

        Container leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(new JScrollPane(list));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        Container rightContainer = new JPanel(new BorderLayout());
        rightContainer.add(detailPanel, BorderLayout.CENTER);
        rightContainer.add(buttons, BorderLayout.SOUTH);

        split.setLeftComponent(leftContainer);
        split.setRightComponent(rightContainer);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(split, BorderLayout.CENTER);
        setTitle("Edit Preferences");
    }

    public JList getList() {
        return list;
    }

    public JSplitPane getSplit() {
        return split;
    }

    public Container getDetailPanel() {
        return detailPanel;
    }

    public CardLayout getDetailLayout() {
        return detailLayout;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }
}
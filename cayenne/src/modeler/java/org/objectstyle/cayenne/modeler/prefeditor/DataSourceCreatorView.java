package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class DataSourceCreatorView extends JDialog {

    protected JTextField dataSourceName;
    protected JComboBox adapters;
    protected JButton okButton;
    protected JButton cancelButton;

    public DataSourceCreatorView() {
        this.dataSourceName = new JTextField();
        this.adapters = new JComboBox();
        this.okButton = new JButton("Create");
        this.cancelButton = new JButton("Cancel");

        // assemble
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:max(50dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Name:", dataSourceName);
        builder.append("Adapter:", adapters);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setTitle("Create New Local DataSource");
    }

    public JComboBox getAdapters() {
        return adapters;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public JTextField getDataSourceName() {
        return dataSourceName;
    }
}
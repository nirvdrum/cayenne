package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class ClasspathPreferencesView extends JPanel {

    protected JButton addJarButton;
    protected JButton addDirButton;
    protected JButton removeEntryButton;
    protected JTable table;

    public ClasspathPreferencesView() {

        // create widgets
        addJarButton = new JButton("Add Jar/Zip");
        addDirButton = new JButton("Add Class Folder");
        removeEntryButton = new JButton("Remove");

        table = new JTable();
        table.setRowMargin(3);

        // assemble

        FormLayout layout = new FormLayout("fill:min(150dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append(addJarButton);
        builder.append(addDirButton);
        builder.append(removeEntryButton);

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(builder.getPanel(), BorderLayout.EAST);
    }

    public JButton getAddDirButton() {
        return addDirButton;
    }

    public JButton getAddJarButton() {
        return addJarButton;
    }

    public JButton getRemoveEntryButton() {
        return removeEntryButton;
    }

    public JTable getTable() {
        return table;
    }
}